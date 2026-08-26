package parker.core.runtime

import java.time.Instant
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*

class ExternalTranscriptionDurableAdmissionTest {
    @TempDir lateinit var directory: Path
    private val evidenceId = EvidenceArtifactId("evidence-v2")
    private val principal = PrincipalId("owner-v2")

    @Test
    fun `v2 codec round trips all fidelity page accounting provenance and model snapshot forms`() {
        TranscriptionFidelity.entries.forEach { fidelity ->
            listOf<OcrModelSnapshot>(OcrModelSnapshot.NotExposed, OcrModelSnapshot.Present("snapshot-2026-08-26")).forEach { snapshot ->
                val extracted = extracted(validation(fidelity, snapshot))
                val entry = DerivativeContentEntry(DerivativeGenerationId("generation-${fidelity.name.lowercase()}-${snapshot::class.simpleName!!.lowercase()}"), evidenceId, TierADerivativePayload.Ocr(extracted))
                val encoded = DerivativeContentCodec.encode(entry)
                val decoded = DerivativeContentCodec.decode(encoded)
                assertEquals(entry, decoded)
                assertEquals(2, representationVersion(encoded, entry))
            }
        }
    }

    @Test
    fun `malformed truncated v2 fails closed`() {
        val entry = DerivativeContentEntry(DerivativeGenerationId("generation-malformed-v2"), evidenceId, TierADerivativePayload.Ocr(extracted(validation())))
        val encoded = DerivativeContentCodec.encode(entry)
        val truncatedBody = encoded.copyOf(encoded.size - 33)
        val malformed = truncatedBody + java.security.MessageDigest.getInstance("SHA-256").digest(truncatedBody)
        assertFails { DerivativeContentCodec.decode(malformed) }
    }

    @Test
    fun `validated admission mints fresh generations preserves first and exact retrieval restores v2`() = runTest {
        val generations = MemoryGenerationStorage()
        val contents = MemoryContentStorage()
        val ids = ArrayDeque(listOf("generation-v2-one", "generation-v2-two"))
        val coordinator = coordinator(generations, contents) { DerivativeGenerationId(ids.removeFirst()) }

        val first = assertIs<OcrDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.admit(evidenceId, validation(), principal, "correlation-one"))
        val second = assertIs<OcrDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.admit(evidenceId, validation(), principal, "correlation-two"))

        assertNotEquals(first.record.derivativeGenerationId, second.record.derivativeGenerationId)
        assertEquals(first.record, generations.retrieve(first.record.derivativeGenerationId))
        val restarted = TierBOcrContentRetrievalCoordinator(generations, contents)
        val restored = assertIs<TierBOcrContentRetrievalOutcome.Retrieved>(restarted.retrieve(evidenceId, first.record.derivativeGenerationId))
        assertEquals(first.extracted, restored.extracted)
        assertEquals(OcrPageOutcomeKind.NOT_RETURNED, restored.extracted.pageAccounting!!.pageOutcomes.last().outcome)
        assertIs<OcrModelSnapshot.NotExposed>(restored.extracted.providerProvenance!!.modelSnapshot)
    }

    @Test
    fun `v2 derivative survives reconstructed filesystem storage and retrieval coordinator`() = runTest {
        val generationRoot = directory.resolve("generations").also(Files::createDirectory)
        val contentRoot = directory.resolve("content").also(Files::createDirectory)
        val firstGenerations = FileSystemDerivativeGenerationStorage(generationRoot)
        val firstContents = FileSystemDerivativeContentStorage(contentRoot)
        val admitted = assertIs<OcrDerivativeGenerationCoordinationOutcome.Admitted>(
            DerivativeGenerationCoordinator(CsvStructuralExtractor { error("unused") }, firstGenerations, DocumentIngestionAudit { },
                { DerivativeGenerationId("generation-filesystem-v2") }, { Instant.EPOCH }, contentStorage = firstContents)
                .admit(evidenceId, validation(), principal, "restart-correlation"),
        )

        val restarted = TierBOcrContentRetrievalCoordinator(
            FileSystemDerivativeGenerationStorage(generationRoot), FileSystemDerivativeContentStorage(contentRoot),
        )
        val restored = assertIs<TierBOcrContentRetrievalOutcome.Retrieved>(restarted.retrieve(evidenceId, admitted.record.derivativeGenerationId))
        assertEquals(admitted.record, restored.record)
        assertEquals(admitted.extracted, restored.extracted)
    }

    @Test
    fun `missing provenance and invalid page accounting fail before id minting or storage`() = runTest {
        val generations = MemoryGenerationStorage(); val contents = MemoryContentStorage(); var ids = 0
        val coordinator = coordinator(generations, contents) { ids++; DerivativeGenerationId("must-not-mint-$ids") }
        val valid = validation()
        val result = (valid.outcome as OcrRecognitionOutcome.Recognised).result
        val missing = valid.copy(outcome = OcrRecognitionOutcome.Recognised(result.copy(providerProvenance = null)))
        val duplicateAccounting = valid.pageAccounting.copy(pageOutcomes = valid.pageAccounting.pageOutcomes + valid.pageAccounting.pageOutcomes.first())
        val invalid = valid.copy(outcome = OcrRecognitionOutcome.Recognised(result.copy(pageAccounting = duplicateAccounting)), pageAccounting = duplicateAccounting)

        assertIs<OcrDerivativeGenerationCoordinationOutcome.MandatoryProvenanceUnavailable>(coordinator.admit(evidenceId, missing, principal, "c1"))
        assertIs<OcrDerivativeGenerationCoordinationOutcome.MandatoryProvenanceUnavailable>(coordinator.admit(evidenceId, invalid, principal, "c2"))
        assertEquals(0, ids); assertTrue(generations.records.isEmpty()); assertTrue(contents.entries.isEmpty())
    }

    @Test
    fun `content publish failure cannot advertise a durable generation`() = runTest {
        val generations = MemoryGenerationStorage(); val contents = MemoryContentStorage(failPublish = true)
        val outcome = coordinator(generations, contents) { DerivativeGenerationId("generation-failed-content") }
            .admit(evidenceId, validation(), principal, "correlation")
        assertIs<OcrDerivativeGenerationCoordinationOutcome.PreparationFailed>(outcome)
        assertTrue(generations.records.isEmpty())
    }

    private fun coordinator(generations: MemoryGenerationStorage, contents: MemoryContentStorage, ids: () -> DerivativeGenerationId) =
        DerivativeGenerationCoordinator(CsvStructuralExtractor { error("unused") }, generations, DocumentIngestionAudit { }, ids, { Instant.EPOCH }, contentStorage = contents)

    private fun validation(fidelity: TranscriptionFidelity = TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION, snapshot: OcrModelSnapshot = OcrModelSnapshot.NotExposed): OcrStructuredValidationOutcome.Validated {
        val requested = OcrPageScope((1..5).toList()); val submitted = OcrPageScope((1..5).toList()); val returned = OcrPageScope(listOf(1, 2, 3))
        val overlap = listOf(OcrUncertaintySpan(2, 0, 2, OcrUncertaintyKind.UNCERTAIN, "uncertain"), OcrUncertaintySpan(2, 1, 3, OcrUncertaintyKind.ILLEGIBLE, "overlap retained"))
        val outcomes = listOf(
            OcrPageOutcome(1, OcrPageOutcomeKind.TRANSCRIBED),
            OcrPageOutcome(2, OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS, OcrPageOutcomeReason("UNCERTAIN_TEXT"), listOf("qualified"), overlap),
            OcrPageOutcome(3, OcrPageOutcomeKind.ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT, OcrPageOutcomeReason("ILLEGIBLE"), listOf("illegible")),
            OcrPageOutcome(4, OcrPageOutcomeKind.FAILED, OcrPageOutcomeReason("PROVIDER_PAGE_FAILURE")),
            OcrPageOutcome(5, OcrPageOutcomeKind.NOT_RETURNED, OcrPageOutcomeReason("VALIDATOR_NOT_RETURNED")),
        )
        val accounting = OcrPageAccounting(requested, submitted, returned, outcomes)
        val processing = OcrProcessingProvenance(evidenceId, OcrSha256Digest("a".repeat(64)), "application/pdf", 100, requested, submitted, "application/pdf", 100, OcrSha256Digest("a".repeat(64)), true, "external-transcription.direct-byte-exact-v1", Instant.EPOCH)
        val provider = OcrProviderProvenance("provider", "adapter", "1.0.0", "transcription-v1", "model-exact", snapshot, "response-123")
        val result = OcrRecognitionResult("page one\npage two", fidelity, OcrRecognitionIdentity("external", "transcription-v1", "1.0.0"), recognisedAt = Instant.EPOCH,
            warnings = listOf("overall"), segments = listOf(OcrRecognitionSegment("page one", fidelity, 1), OcrRecognitionSegment("page two", fidelity, 2)),
            pageAccounting = accounting, processingProvenance = processing, providerProvenance = provider)
        return OcrStructuredValidationOutcome.Validated(OcrRecognitionOutcome.Recognised(result), DerivativeCompletenessState.KNOWN_INCOMPLETE, accounting)
    }

    private fun extracted(validation: OcrStructuredValidationOutcome.Validated): OcrDerivativeExtractedResult {
        val result = (validation.outcome as OcrRecognitionOutcome.Recognised).result
        val provider = result.providerProvenance!!
        val producer = DerivativeProducerIdentity("external", "1.0.0", "transcription-v1", provider.adapterIdentity, provider.adapterVersion,
            provider.providerReportedModelIdentifier, (provider.modelSnapshot as? OcrModelSnapshot.Present)?.value)
        return OcrDerivativeExtractedResult(result.recognisedText, result.fidelity, OcrDerivativeOutcomeKind.RECOGNISED, null, result.warnings, result.segments, producer,
            listOf(DerivativeTransformation.OCR, DerivativeTransformation.MODEL_INFERENCE), validation.completenessState, validation.pageAccounting, result.processingProvenance, provider, result.recognisedAt)
    }

    private fun representationVersion(bytes: ByteArray, entry: DerivativeContentEntry): Int {
        val offset = 4 + 4 + 4 + entry.derivativeGenerationId.value.toByteArray().size + 4 + entry.rootSourceEvidenceArtifactId.value.toByteArray().size + 1
        return java.nio.ByteBuffer.wrap(bytes, offset, 4).int
    }

    private class MemoryGenerationStorage : DerivativeGenerationStorage {
        val records = linkedMapOf<DerivativeGenerationId, DerivativeGenerationRecord>(); private val prepared = linkedMapOf<DerivativeGenerationId, DerivativeGenerationRecord>()
        override suspend fun prepare(record: DerivativeGenerationRecord) { prepared[record.derivativeGenerationId] = record }
        override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) { records[derivativeGenerationId] = prepared.remove(derivativeGenerationId)!! }
        override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId) = records[derivativeGenerationId]
    }
    private class MemoryContentStorage(private val failPublish: Boolean = false) : DerivativeContentStorage {
        val entries = linkedMapOf<DerivativeGenerationId, DerivativeContentEntry>(); private val prepared = linkedMapOf<DerivativeGenerationId, ByteArray>()
        override suspend fun prepare(entry: DerivativeContentEntry) { prepared[entry.derivativeGenerationId] = DerivativeContentCodec.encode(entry) }
        override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) { if (failPublish) throw DerivativeContentStorageException.PersistenceFailure("injected", java.io.IOException("injected")); entries[derivativeGenerationId] = DerivativeContentCodec.decode(prepared.remove(derivativeGenerationId)!!) }
        override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId) = entries[derivativeGenerationId]
    }
}
