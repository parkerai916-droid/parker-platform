package parker.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.*
import java.time.Instant

class DerivativeGenerationDiscoveryProjectionTest {
    private val evidenceA = EvidenceArtifactId("evidence-a")
    private val evidenceB = EvidenceArtifactId("evidence-b")

    private fun record(id: String, root: EvidenceArtifactId, kind: String) = DerivativeGenerationRecord(
        DerivativeGenerationId(id), root,
        listOf(DerivativeParentReference.RootEvidenceArtifact(root)), kind,
        DerivativeProducerIdentity("test-producer", "1", "test-config"),
        listOf(DerivativeTransformation.STRUCTURAL_PARSING), Instant.EPOCH,
        DerivativeContentIdentity.NoCanonicalSerialization,
        DerivativeCompletenessState.ACCOUNTED_FOR, DerivativeOperationalOutcome.USABLE,
    )

    private fun candidate(record: DerivativeGenerationRecord, available: Boolean = true, completeness: DerivativeCompletenessState = record.completenessState, equivalenceKey: String? = null) =
        DerivativeCandidateSummary(record.derivativeGenerationId, record.rootSourceEvidenceArtifactId, record.derivativeKind,
            record.producerIdentity, record.generatedAt, record.operationalOutcome, completeness, record.warnings,
            record.transformationHistory, available, equivalenceKey = equivalenceKey)

    private fun resolver() = PreferredDerivativeResolver(
        DerivativeGenerationDiscoveryProjection(DerivativeGenerationDiscovery { emptyList() },
            object : DerivativeContentStorage {
                override suspend fun prepare(entry: DerivativeContentEntry) = Unit
                override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) = Unit
                override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId) = null
            }),
    )

    @Test
    fun `resolver prefers the only complete general document candidate`() {
        val pdf = record("generation-pdf", evidenceA, "PDF structure")
        val result = resolver().resolve(evidenceA, listOf(candidate(pdf)))
        assertIs<PreferredDerivativeResolution.Preferred>(result)
        assertEquals(pdf.derivativeGenerationId, result.derivative.derivativeGenerationId)
    }

    @Test
    fun `resolver accepts the emitted searchable PDF literal text candidate`() {
        val pdf = record("generation-searchable-pdf", evidenceA, "Searchable PDF literal text")
        val result = resolver().resolve(evidenceA, listOf(candidate(pdf)))
        assertIs<PreferredDerivativeResolution.Preferred>(result)
        assertEquals(pdf.derivativeGenerationId, result.derivative.derivativeGenerationId)
    }

    @Test
    fun `resolver accepts governed structured representations for practical formats`() {
        for (kind in listOf("DOC structured representation", "DOCX structured representation", "XLS structured representation", "XLSX structured representation", "MSG structured representation", "RTF structured representation", "TXT structured representation")) {
            val generation = record("generation-${kind.hashCode()}", evidenceA, kind)
            assertIs<PreferredDerivativeResolution.Preferred>(resolver().resolve(evidenceA, listOf(candidate(generation))))
        }
    }

    @Test
    fun `resolver prefers complete over qualified candidate of the same kind`() {
        val complete = record("generation-complete", evidenceA, "OCR recognised text")
        val partial = record("generation-partial", evidenceA, "OCR recognised text")
        val result = resolver().resolve(evidenceA, listOf(candidate(complete), candidate(partial, completeness = DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS)))
        assertIs<PreferredDerivativeResolution.Preferred>(result)
        assertEquals(complete.derivativeGenerationId, result.derivative.derivativeGenerationId)
    }

    @Test
    fun `resolver collapses equivalent retries and chooses earliest generation independent of order`() {
        val first = record("generation-first", evidenceA, "XLSX structured representation")
            .copy(generatedAt = Instant.parse("2026-01-01T00:00:00Z"))
        val second = record("generation-second", evidenceA, "XLSX structured representation")
            .copy(generatedAt = Instant.parse("2026-01-02T00:00:00Z"))
        val result = resolver().resolve(evidenceA, listOf(candidate(second, equivalenceKey = "same"), candidate(first, equivalenceKey = "same")))
        assertIs<PreferredDerivativeResolution.Preferred>(result)
        assertEquals(first.derivativeGenerationId, result.derivative.derivativeGenerationId)
    }

    @Test
    fun `same flattened text with a different structural equivalence key remains ambiguous`() {
        val first = record("generation-structure-a", evidenceA, "XLSX structured representation")
        val second = record("generation-structure-b", evidenceA, "XLSX structured representation")
        assertIs<PreferredDerivativeResolution.Ambiguous>(resolver().resolve(evidenceA, listOf(
            candidate(first, equivalenceKey = "sheet-Payments-cell-B4"),
            candidate(second, equivalenceKey = "sheet-Payments-cell-C4"),
        )))
    }

    @Test
    fun `authoritative external OCR wins even when text matches local preliminary OCR`() {
        val local = record("generation-local", evidenceA, "OCR recognised text")
        val external = record("generation-external", evidenceA, "OCR recognised text")
        val localCandidate = candidate(local, equivalenceKey = "local-text")
        val externalCandidate = candidate(external, equivalenceKey = "external-text").copy(authority = OcrAuthorityClassification.EXTERNAL_AUTHORITATIVE)
        val result = resolver().resolve(evidenceA, listOf(localCandidate, externalCandidate))
        assertIs<PreferredDerivativeResolution.Preferred>(result)
        assertEquals(external.derivativeGenerationId, result.derivative.derivativeGenerationId)
    }

    @Test
    fun `authoritative external OCR outranks local preliminary OCR across producer kind labels`() {
        val local = record("generation-local", evidenceA, "OCR recognised text")
        val external = record("generation-external", evidenceA, "External transcription recognised text")
        val result = resolver().resolve(evidenceA, listOf(
            candidate(local, equivalenceKey = "local"),
            candidate(external, equivalenceKey = "external").copy(authority = OcrAuthorityClassification.EXTERNAL_AUTHORITATIVE),
        ))
        assertIs<PreferredDerivativeResolution.Preferred>(result)
        assertEquals(external.derivativeGenerationId, result.derivative.derivativeGenerationId)
    }

    @Test
    fun `local OCR remains selectable when no authoritative external OCR exists`() {
        val local = record("generation-local", evidenceA, "OCR recognised text")
        val result = resolver().resolve(evidenceA, listOf(candidate(local)))
        assertIs<PreferredDerivativeResolution.Preferred>(result)
        assertEquals(local.derivativeGenerationId, result.derivative.derivativeGenerationId)
    }

    @Test
    fun `resolver reports ambiguity across equally valid representation kinds`() {
        val pdf = record("generation-pdf", evidenceA, "PDF structure")
        val ocr = record("generation-ocr", evidenceA, "OCR recognised text")
        assertIs<PreferredDerivativeResolution.Ambiguous>(resolver().resolve(evidenceA, listOf(candidate(pdf), candidate(ocr))))
    }

    @Test
    fun `resolver excludes specialized and unavailable candidates`() {
        val region = record("generation-region", evidenceA, "Region transcription")
        val missing = record("generation-missing", evidenceA, "PDF structure")
        val result = resolver().resolve(evidenceA, listOf(candidate(region), candidate(missing, available = false)))
        assertIs<PreferredDerivativeResolution.NoUsableDerivative>(result)
    }

    @Test
    fun `projection returns every exact-evidence derivative kind and content availability`() = runTest {
        val pdf = record("generation-pdf", evidenceA, "PDF structure")
        val ocr = record("generation-ocr", evidenceA, "OCR recognised text")
        val region = record("generation-region", evidenceA, "Region transcription")
        val foreign = record("generation-foreign", evidenceB, "OCR recognised text")
        val projection = DerivativeGenerationDiscoveryProjection(
            DerivativeGenerationDiscovery { listOf(pdf, ocr, region, foreign) },
            object : DerivativeContentStorage {
                override suspend fun prepare(entry: DerivativeContentEntry) = Unit
                override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) = Unit
                override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId) =
                    if (derivativeGenerationId == pdf.derivativeGenerationId) null else DerivativeContentEntry(
                        derivativeGenerationId, evidenceA, TierADerivativePayload.Ocr(
                        OcrDerivativeExtractedResult(
                            recognisedText = "text",
                            fidelity = TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
                            outcomeKind = OcrDerivativeOutcomeKind.RECOGNISED,
                            degradationReason = null,
                            warnings = emptyList(), segments = emptyList(),
                            producerIdentity = DerivativeProducerIdentity("test", "1", "config"),
                            transformationHistory = listOf(DerivativeTransformation.OCR),
                            completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
                        )
                        ),
                    )
            },
        )

        val result = projection.discover(evidenceA)
        assertEquals(listOf("generation-pdf", "generation-ocr", "generation-region"), result.map { it.derivativeGenerationId.value })
        assertEquals(setOf("PDF structure", "OCR recognised text", "Region transcription"), result.map { it.derivativeKind }.toSet())
        assertFalse(result.first { it.derivativeGenerationId == pdf.derivativeGenerationId }.contentAvailable)
        assertTrue(result.first { it.derivativeGenerationId == ocr.derivativeGenerationId }.contentAvailable)
        assertTrue(result.none { it.rootSourceEvidenceArtifactId == evidenceB })
    }

    @Test
    fun `projection gives equivalent durable payloads the same key across generation ids`() = runTest {
        val first = record("generation-equivalent-first", evidenceA, "OCR recognised text")
        val second = record("generation-equivalent-second", evidenceA, "OCR recognised text")
            .copy(generatedAt = Instant.parse("2026-01-02T00:00:00Z"))
        val payload = TierADerivativePayload.Ocr(
            OcrDerivativeExtractedResult(
                recognisedText = "same text",
                fidelity = TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
                outcomeKind = OcrDerivativeOutcomeKind.RECOGNISED,
                degradationReason = null,
                warnings = emptyList(), segments = emptyList(),
                producerIdentity = DerivativeProducerIdentity("test", "1", "config"),
                transformationHistory = listOf(DerivativeTransformation.OCR),
                completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
            ),
        )
        val projection = DerivativeGenerationDiscoveryProjection(
            DerivativeGenerationDiscovery { listOf(first, second) },
            object : DerivativeContentStorage {
                override suspend fun prepare(entry: DerivativeContentEntry) = Unit
                override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) = Unit
                override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId) =
                    DerivativeContentEntry(derivativeGenerationId, evidenceA, payload)
            },
        )
        val result = projection.discover(evidenceA)
        assertEquals(2, result.size)
        assertTrue(result.all { it.equivalenceKey != null })
        assertEquals(result[0].equivalenceKey, result[1].equivalenceKey)
    }

    @Test
    fun `projection preserves zero candidate result and reports corrupt content unavailable`() = runTest {
        val generation = record("generation-corrupt", evidenceA, "OCR recognised text")
        val projection = DerivativeGenerationDiscoveryProjection(
            DerivativeGenerationDiscovery { requested -> if (requested == evidenceA) listOf(generation) else emptyList() },
            object : DerivativeContentStorage {
                override suspend fun prepare(entry: DerivativeContentEntry) = Unit
                override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) = Unit
                override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId): DerivativeContentEntry =
                    throw DerivativeContentStorageException.CorruptContent(derivativeGenerationId, "test corruption")
            },
        )
        assertEquals(1, projection.discover(evidenceA).size)
        assertFalse(projection.discover(evidenceA).single().contentAvailable)
        assertTrue(projection.discover(evidenceB).isEmpty())
    }
}
