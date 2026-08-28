package parker.composition

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.TierAContentRetrievalOutcome
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome

/**
 * Document Ingestion — Derivative Content Persistence and Retrieval.
 * Governed by `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`
 * §14/§15/§16. The mandatory restart acceptance instrument: a real
 * [ParkerRuntime], processed once, stopped, and started again as a genuinely
 * new instance against the *same* durable storage roots -- retrieval after
 * restart never calls `processTierA`/`invokeTierAIngestionAsOwner` again, so
 * a match proves the content came from durable storage, not re-extraction
 * (also structurally true by construction:
 * [parker.core.runtime.TierAContentRetrievalCoordinator] depends on exactly
 * [parker.core.interfaces.DerivativeGenerationStorage] and
 * [parker.core.interfaces.DerivativeContentStorage] -- it holds no
 * extractor of any kind and cannot re-extract even in principle).
 */
class DerivativeContentPersistenceRestartAcceptanceTest {

    private val ownerPrincipalId = "user.derivative-content-restart-test"
    private val fixtureRoot: Path = Path.of("tests", "fixtures", "document-ingestion-bakeoff", "fixtures")

    private fun config(localTextChannelModuleId: String) = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- reasoning is not under test here
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = localTextChannelModuleId,
        evidenceStorageRootPath = Files.createTempDirectory("restart-acceptance-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("restart-acceptance-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("restart-acceptance-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("restart-acceptance-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("restart-acceptance-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("restart-acceptance-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("restart-acceptance-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("restart-acceptance-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = syntheticBridgeShellExecutable(),
        doclingBridgeScriptPath = Files.createTempFile("restart-acceptance-unused-bridge", ".sh").also {
            Files.writeString(it, "#!/bin/sh\nexit 0\n")
            it.toFile().setExecutable(true)
        }.toString(),
    )

    private suspend fun importAndProcess(
        runtime: ParkerRuntime,
        fileName: String,
        mediaType: String,
    ): Pair<EvidenceArtifactId, TierADocumentRoutingResult.Admitted> {
        val imported = assertIs<OwnerLocalFileIngressOutcome.Accepted>(
            runtime.importEvidenceFileAsOwner(fixtureRoot.resolve(fileName).toAbsolutePath().toString(), mediaType),
        )
        val id = imported.acceptedEvidenceArtifact.evidenceArtifactId
        val admitted = assertIs<TierADocumentRoutingResult.Admitted>(
            assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(id)).result,
        )
        return id to admitted
    }

    @Test
    fun `PDF -- import, Process once, stop, restart, retrieve without re-processing -- exact documentText and provenance survive`() = runTest {
        val cfg = config("channel.local-text-restart-pdf")
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        val (evidenceArtifactId, admitted) = importAndProcess(runtime, "01-searchable-simple.pdf", "application/pdf")
        val derivativeGenerationId = admitted.record.derivativeGenerationId
        val originalPdf = (admitted.payload as TierADerivativePayload.Pdf).value
        runtime.shutdown()

        // A genuinely new ParkerRuntime instance -- never the same object, never any in-memory
        // reference to what was just extracted -- against the same durable storage roots only.
        val restarted = ParkerRuntime(cfg, RecordingParkerLogger())
        restarted.start()
        val retrieved = assertIs<TierAContentRetrievalOutcome.Retrieved>(
            restarted.retrieveTierAExtractedContentAsOwner(evidenceArtifactId, derivativeGenerationId),
        )
        val retrievedPdf = (retrieved.payload as TierADerivativePayload.Pdf).value

        assertEquals(originalPdf, retrievedPdf, "every governed PdfStructuralResult field must survive a restart exactly")
        assertEquals(admitted.record, retrieved.record)
        restarted.shutdown()
    }

    @Test
    fun `CSV -- full rows survive a restart exactly, never the browser's 500-row preview truncation`() = runTest {
        val cfg = config("channel.local-text-restart-csv")
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        val (evidenceArtifactId, admitted) = importAndProcess(runtime, "06-structured.csv", "text/csv")
        val derivativeGenerationId = admitted.record.derivativeGenerationId
        val originalCsv = (admitted.payload as TierADerivativePayload.Csv).value
        runtime.shutdown()

        val restarted = ParkerRuntime(cfg, RecordingParkerLogger())
        restarted.start()
        val retrieved = assertIs<TierAContentRetrievalOutcome.Retrieved>(
            restarted.retrieveTierAExtractedContentAsOwner(evidenceArtifactId, derivativeGenerationId),
        )
        val retrievedCsv = (retrieved.payload as TierADerivativePayload.Csv).value

        assertEquals(originalCsv, retrievedCsv, "every governed CsvStructuralResult field, including full rows, must survive a restart exactly")
        restarted.shutdown()
    }

    @Test
    fun `DOCX -- paragraphs, tables, headers, footers, and metadata survive a restart exactly`() = runTest {
        val cfg = config("channel.local-text-restart-docx")
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        val (evidenceArtifactId, admitted) = importAndProcess(
            runtime, "04-structured.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        )
        val derivativeGenerationId = admitted.record.derivativeGenerationId
        val originalDocx = (admitted.payload as TierADerivativePayload.Docx).value
        runtime.shutdown()

        val restarted = ParkerRuntime(cfg, RecordingParkerLogger())
        restarted.start()
        val retrieved = assertIs<TierAContentRetrievalOutcome.Retrieved>(
            restarted.retrieveTierAExtractedContentAsOwner(evidenceArtifactId, derivativeGenerationId),
        )
        val retrievedDocx = (retrieved.payload as TierADerivativePayload.Docx).value

        assertEquals(originalDocx, retrievedDocx, "every governed DocxStructuralResult field must survive a restart exactly")
        restarted.shutdown()
    }

    @Test
    fun `EML -- headers, mime entities, decoded body text, and attachment metadata survive a restart exactly -- raw and decoded bytes are never persisted`() = runTest {
        val cfg = config("channel.local-text-restart-eml")
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        val (evidenceArtifactId, admitted) = importAndProcess(runtime, "05-email-with-attachment.eml", "message/rfc822")
        val derivativeGenerationId = admitted.record.derivativeGenerationId
        val originalEml = (admitted.payload as TierADerivativePayload.Eml)
        runtime.shutdown()

        val restarted = ParkerRuntime(cfg, RecordingParkerLogger())
        restarted.start()
        val retrieved = assertIs<TierAContentRetrievalOutcome.Retrieved>(
            restarted.retrieveTierAExtractedContentAsOwner(evidenceArtifactId, derivativeGenerationId),
        )
        val retrievedEml = (retrieved.payload as TierADerivativePayload.Eml)

        // Kotlin data class equals() compares ByteArray fields by reference, not content, so the
        // governed subset below is asserted field-by-field rather than via a single top-level
        // assertEquals -- see DerivativeContentCodecTest's own identical, deliberate choice.
        assertEquals(originalEml.childSourceCandidateCount, retrievedEml.childSourceCandidateCount)
        assertEquals(
            originalEml.value.headers.map { Triple(it.name, it.value, it.rawRepresentation) },
            retrievedEml.value.headers.map { Triple(it.name, it.value, it.rawRepresentation) },
        )
        assertEquals(originalEml.value.from, retrievedEml.value.from)
        assertEquals(originalEml.value.subject, retrievedEml.value.subject)
        assertEquals(originalEml.value.mimeEntities, retrievedEml.value.mimeEntities)
        assertEquals(
            originalEml.value.bodyAlternatives.map { it.mimeEntityId to it.decodedText },
            retrievedEml.value.bodyAlternatives.map { it.mimeEntityId to it.decodedText },
            "decoded body text (never raw bytes, Scope Lock §7) must survive a restart exactly",
        )
        assertEquals(
            originalEml.value.attachmentCandidates.map { it.filename to it.byteLength to it.sha256 },
            retrievedEml.value.attachmentCandidates.map { it.filename to it.byteLength to it.sha256 },
            "attachment metadata (never decoded attachment bytes, Scope Lock §7) must survive a restart exactly",
        )
        assertEquals(originalEml.value.producerIdentity, retrievedEml.value.producerIdentity)
        assertEquals(originalEml.value.completenessState, retrievedEml.value.completenessState)
        assertEquals(originalEml.value.warnings, retrievedEml.value.warnings)
        restarted.shutdown()
    }

    @Test
    fun `reprocessing the same source produces two distinct generation ids, and both remain independently retrievable after a restart, neither overwritten`() = runTest {
        val cfg = config("channel.local-text-restart-reprocess")
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        val imported = assertIs<OwnerLocalFileIngressOutcome.Accepted>(
            runtime.importEvidenceFileAsOwner(fixtureRoot.resolve("06-structured.csv").toAbsolutePath().toString(), "text/csv"),
        )
        val evidenceArtifactId = imported.acceptedEvidenceArtifact.evidenceArtifactId
        val first = assertIs<TierADocumentRoutingResult.Admitted>(
            assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result,
        )
        val second = assertIs<TierADocumentRoutingResult.Admitted>(
            assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result,
        )
        assertNotEquals(first.record.derivativeGenerationId, second.record.derivativeGenerationId)
        runtime.shutdown()

        val restarted = ParkerRuntime(cfg, RecordingParkerLogger())
        restarted.start()
        val retrievedFirst = assertIs<TierAContentRetrievalOutcome.Retrieved>(
            restarted.retrieveTierAExtractedContentAsOwner(evidenceArtifactId, first.record.derivativeGenerationId),
        )
        val retrievedSecond = assertIs<TierAContentRetrievalOutcome.Retrieved>(
            restarted.retrieveTierAExtractedContentAsOwner(evidenceArtifactId, second.record.derivativeGenerationId),
        )
        assertEquals(first.payload, retrievedFirst.payload)
        assertEquals(second.payload, retrievedSecond.payload)
        assertEquals(first.record, retrievedFirst.record)
        assertEquals(second.record, retrievedSecond.record)
        restarted.shutdown()
    }

    @Test
    fun `a retrieval request for a generation id that belongs to a different evidence artefact fails closed with SourceMismatch, never cross-source content`() = runTest {
        val cfg = config("channel.local-text-restart-mismatch")
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        val (_, admitted) = importAndProcess(runtime, "06-structured.csv", "text/csv")
        val otherImported = assertIs<OwnerLocalFileIngressOutcome.Accepted>(
            runtime.importEvidenceFileAsOwner(fixtureRoot.resolve("01-searchable-simple.pdf").toAbsolutePath().toString(), "application/pdf"),
        )

        val outcome = runtime.retrieveTierAExtractedContentAsOwner(
            otherImported.acceptedEvidenceArtifact.evidenceArtifactId,
            admitted.record.derivativeGenerationId,
        )

        assertEquals(
            TierAContentRetrievalOutcome.SourceMismatch(otherImported.acceptedEvidenceArtifact.evidenceArtifactId, admitted.record.derivativeGenerationId),
            outcome,
        )
        runtime.shutdown()
    }

    @Test
    fun `a retrieval request for a never-processed generation id fails closed with UnknownGeneration`() = runTest {
        val cfg = config("channel.local-text-restart-unknown")
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()

        val outcome = runtime.retrieveTierAExtractedContentAsOwner(
            EvidenceArtifactId("evidence-never-registered"),
            DerivativeGenerationId("generation-never-registered"),
        )

        assertEquals(TierAContentRetrievalOutcome.UnknownGeneration(DerivativeGenerationId("generation-never-registered")), outcome)
        runtime.shutdown()
    }
}
