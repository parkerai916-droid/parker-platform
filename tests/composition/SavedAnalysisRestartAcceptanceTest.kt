package parker.composition

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.DocumentAnalysisOutcome
import parker.core.interfaces.EvidenceGenerationSelection
import parker.core.interfaces.OwnerDocumentAnalysisRequest
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.RetrieveSavedAnalysisOutcome
import parker.core.interfaces.SaveAnalysisOutcome
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome

/**
 * Reviewed Analysis Result — Explicit Owner Save. The mandatory restart
 * acceptance instrument at the [ParkerRuntime] level, mirroring
 * [DerivativeContentPersistenceRestartAcceptanceTest]'s own established
 * "genuinely new instance, same durable storage roots, never rerun the
 * expensive step" shape: a real analysis is produced once (against a real,
 * stubbed local model endpoint), explicitly saved, the runtime is stopped,
 * and a wholly new [ParkerRuntime] instance retrieves it -- proving the
 * saved record is truly durable, not merely held in the first process's own
 * memory, and that retrieval never re-invokes the model.
 */
class SavedAnalysisRestartAcceptanceTest {

    private val ownerPrincipalId = "user.saved-analysis-restart-test"
    private val fixtureRoot: Path = Path.of("tests", "fixtures", "document-ingestion-bakeoff", "fixtures")

    private fun config(localTextChannelModuleId: String, modelEndpointUrl: String) = ParkerRuntimeConfig(
        modelEndpointUrl = modelEndpointUrl,
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = localTextChannelModuleId,
        evidenceStorageRootPath = Files.createTempDirectory("saved-analysis-restart-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("saved-analysis-restart-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("saved-analysis-restart-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("saved-analysis-restart-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-restart-saved-analysis").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("saved-analysis-restart-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("saved-analysis-restart-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("saved-analysis-restart-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("saved-analysis-restart-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = syntheticBridgeShellExecutable(),
        doclingBridgeScriptPath = Files.createTempFile("saved-analysis-restart-unused-bridge", ".sh").also {
            Files.writeString(it, "#!/bin/sh\nexit 0\n")
            it.toFile().setExecutable(true)
        }.toString(),
    )

    @Test
    fun `analyse, explicitly Save, restart, retrieve by saved id -- exact instruction, analysis text, analysedAt, and evidence references survive, retrieval invokes the model zero times`() = runBlocking<Unit> {
        StubModelServer.start("The document appears to be a synthetic fixture.").use { stub ->
            val cfg = config("channel.local-text-saved-analysis-restart", stub.endpointUrl)
            val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
            runtime.start()

            val imported = assertIs<OwnerLocalFileIngressOutcome.Accepted>(
                runtime.importEvidenceFileAsOwner(fixtureRoot.resolve("01-searchable-simple.pdf").toAbsolutePath().toString(), "application/pdf"),
            )
            val evidenceArtifactId = imported.acceptedEvidenceArtifact.evidenceArtifactId
            val admitted = assertIs<TierADocumentRoutingResult.Admitted>(
                assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result,
            )
            val derivativeGenerationId = admitted.record.derivativeGenerationId

            val invocation = runtime.analyseDocumentsAsOwner(
                OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidenceArtifactId, derivativeGenerationId)), "Summarise this document"),
            )
            val completed = assertIs<DocumentAnalysisOutcome.Completed>(invocation.outcome)
            val pendingAnalysisId = requireNotNull(invocation.pendingAnalysisId)

            val saveOutcome = assertIs<SaveAnalysisOutcome.Saved>(runtime.saveAnalysisAsOwner(pendingAnalysisId))
            val savedAnalysisId = saveOutcome.savedAnalysisId

            runtime.shutdown()

            // A genuinely new ParkerRuntime instance -- never the same object, never any in-memory
            // reference to what was just analysed -- against the same durable storage roots only.
            // Deliberately still points at the stub -- if retrieval ever invoked the model, the
            // stub's own receivedRequestBodies count below would prove it.
            val restarted = ParkerRuntime(cfg, RecordingParkerLogger())
            restarted.start()

            val requestCountBeforeRetrieval = stub.receivedRequestBodies.size
            val retrieved = assertIs<RetrieveSavedAnalysisOutcome.Retrieved>(restarted.retrieveSavedAnalysisAsOwner(savedAnalysisId))

            assertEquals(completed.result.instruction, retrieved.record.instruction)
            assertEquals(completed.result.analysisText, retrieved.record.analysisText)
            assertEquals(completed.result.analysedAt, retrieved.record.analysedAt)
            assertEquals(completed.result.mechanismIdentity, retrieved.record.mechanismIdentity)
            assertEquals(completed.result.mechanismVersion, retrieved.record.mechanismVersion)
            assertEquals(1, retrieved.record.evidenceReferences.size)
            assertEquals(evidenceArtifactId, retrieved.record.evidenceReferences.single().evidenceArtifactId)
            assertEquals(derivativeGenerationId, retrieved.record.evidenceReferences.single().derivativeGenerationId)
            assertEquals(requestCountBeforeRetrieval, stub.receivedRequestBodies.size, "retrieval must invoke the model zero times")

            restarted.shutdown()
        }
    }

    @Test
    fun `an unsaved pending analysis does not survive a restart -- a fresh runtime's own pending cache starts empty`() = runBlocking<Unit> {
        StubModelServer.start("transient, never saved").use { stub ->
            val cfg = config("channel.local-text-saved-analysis-restart-unsaved", stub.endpointUrl)
            val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
            runtime.start()

            val imported = assertIs<OwnerLocalFileIngressOutcome.Accepted>(
                runtime.importEvidenceFileAsOwner(fixtureRoot.resolve("01-searchable-simple.pdf").toAbsolutePath().toString(), "application/pdf"),
            )
            val evidenceArtifactId = imported.acceptedEvidenceArtifact.evidenceArtifactId
            val admitted = assertIs<TierADocumentRoutingResult.Admitted>(
                assertIs<TierAOwnerInvocationOutcome.Routed>(runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)).result,
            )

            val invocation = runtime.analyseDocumentsAsOwner(
                OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidenceArtifactId, admitted.record.derivativeGenerationId)), "Summarise"),
            )
            assertIs<DocumentAnalysisOutcome.Completed>(invocation.outcome)
            val pendingAnalysisId = requireNotNull(invocation.pendingAnalysisId)
            // Deliberately never saved.
            runtime.shutdown()

            val restarted = ParkerRuntime(cfg, RecordingParkerLogger())
            restarted.start()

            val saveAttempt = restarted.saveAnalysisAsOwner(pendingAnalysisId)

            assertEquals(SaveAnalysisOutcome.UnknownOrExpiredPendingAnalysis, saveAttempt)
            restarted.shutdown()
        }
    }
}
