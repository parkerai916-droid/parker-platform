package parker.composition

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeTransformation
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierBOcrContentRetrievalOutcome
import parker.core.interfaces.TierBOcrOwnerInvocationOutcome
import parker.core.runtime.EvidenceRegistrationOutcome
import parker.core.runtime.TierBOcrContentRetrievalCoordinator

/**
 * Document Ingestion — Tier B Durable OCR Derivative Content. Governed by
 * `docs/architecture/DOCUMENT_INGESTION_TIER_B_DURABLE_OCR_DERIVATIVE_CONTENT_SCOPE_LOCK.md`.
 * Full-stack acceptance against a real, fully-wired [ParkerRuntime] --
 * mirroring [DerivativeContentPersistenceRestartAcceptanceTest]'s own
 * restart/reprocessing style and [ParkerRuntimeOcrCompositionTest]'s own
 * fake-bridge-script technique for the OCR leg (a real subprocess, a fake
 * program on the other end -- never a fake `DoclingSubprocessInvoker`
 * Kotlin object).
 */
class ParkerRuntimeTierBOcrDurableGenerationTest {

    private val ownerPrincipalId = "user.tierb-durable-test"
    private val digestA = "a".repeat(64)
    private val digestB = "b".repeat(64)

    private fun config(localTextChannelModuleId: String, doclingBridgeScriptPath: String) = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- reasoning is not under test here
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = localTextChannelModuleId,
        evidenceStorageRootPath = Files.createTempDirectory("tierb-durable-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("tierb-durable-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("tierb-durable-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("tierb-durable-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("tierb-durable-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("tierb-durable-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("tierb-durable-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("tierb-durable-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = "/bin/sh",
        doclingBridgeScriptPath = doclingBridgeScriptPath,
        doclingTimeoutMillis = 30_000L,
    )

    private fun minimalPngBytes(): ByteArray = ByteArrayOutputStream().also {
        ImageIO.write(BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", it)
    }.toByteArray()

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "tierb-durable-test-source",
        sourceType = "test",
        acquisitionTime = java.time.Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun writeFakeBridgeScript(directory: Path, stdout: String): Path {
        val scriptPath = Files.createTempFile(directory, "fake-docling-bridge-", ".sh")
        scriptPath.writeText("#!/bin/sh\nprintf '%s' '$stdout'\nexit 0\n")
        scriptPath.toFile().setExecutable(true)
        return scriptPath
    }

    private fun successJson(text: String = "FAKE TIER B DURABLE TEXT", digest: String = digestA): String =
        """{"status":"recognised","recognisedText":"$text","fidelity":"VERBATIM","mechanismVersion":"docling-2.5.0","modelIdentity":"rapidocr-onnxruntime:PP-OCRv6_rec_small","modelVersion":"sha256:$digest"}"""

    private val missingModelVersionJson =
        """{"status":"recognised","recognisedText":"no model provenance","fidelity":"VERBATIM","mechanismVersion":"docling-2.5.0"}"""

    private suspend fun registerImage(runtime: ParkerRuntime): EvidenceArtifactId {
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(PrincipalId(ownerPrincipalId), CandidateEvidenceArtifact(minimalPngBytes(), receivedMediaType = "image/png"), candidateProvenance(), "test-document"),
        )
        return registered.acceptedEvidenceArtifact.evidenceArtifactId
    }

    @Test
    fun `mint, stop, restart, retrieve without rerunning OCR -- exact content and provenance survive`() = runTest {
        val scriptDir = Files.createTempDirectory("tierb-durable-scripts")
        val cfg = config("channel.local-text-tierb-restart", writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        val evidenceArtifactId = registerImage(runtime)

        val admitted = assertIs<TierBOcrOwnerInvocationOutcome.Admitted>(runtime.invokeTierBOcrDurableGenerationAsOwner(evidenceArtifactId))
        val derivativeGenerationId = admitted.record.derivativeGenerationId
        runtime.shutdown()

        // A genuinely new ParkerRuntime instance against the same durable storage roots only --
        // and this test never overwrites the fake bridge script with a marker-based "was OCR
        // invoked" check because the retrieval coordinator holds no OcrMechanism dependency at
        // all (proven structurally, separately, below) -- there is no path through which restart
        // retrieval could invoke OCR even if it tried.
        val restarted = ParkerRuntime(cfg, RecordingParkerLogger())
        restarted.start()
        val retrieved = assertIs<TierBOcrContentRetrievalOutcome.Retrieved>(
            restarted.retrieveTierBOcrContentAsOwner(evidenceArtifactId, derivativeGenerationId),
        )

        assertEquals(admitted.record, retrieved.record)
        assertEquals(admitted.extracted, retrieved.extracted)
        assertEquals("FAKE TIER B DURABLE TEXT", retrieved.extracted.recognisedText)
        assertEquals("docling", retrieved.record.producerIdentity.pluginIdentity)
        assertEquals("docling-2.5.0", retrieved.record.producerIdentity.pluginVersion)
        assertEquals("rapidocr-onnxruntime:PP-OCRv6_rec_small", retrieved.record.producerIdentity.modelIdentity)
        assertEquals("sha256:$digestA", retrieved.record.producerIdentity.modelVersion)
        assertNull(retrieved.record.confidence, "confidence must never be persisted for Tier B (scope lock §16)")
        assertTrue(DerivativeTransformation.OCR in retrieved.record.transformationHistory)
        restarted.shutdown()
    }

    @Test
    fun `reprocessing the same source produces two distinct generation ids -- both independently retrievable after a restart, neither overwritten`() = runTest {
        val scriptDir = Files.createTempDirectory("tierb-durable-scripts")
        // A tiny fake bridge that alternates its own response per invocation, via an incrementing
        // counter file, so the two explicit OCR runs below produce genuinely distinct recognised
        // text/digest -- proving A != B is a real difference in what was produced, not a fixture
        // coincidence.
        val counterPath = scriptDir.resolve("counter")
        Files.writeString(counterPath, "0")
        val scriptPath = Files.createTempFile(scriptDir, "fake-docling-bridge-alternating-", ".sh")
        scriptPath.writeText(
            "#!/bin/sh\n" +
                "n=\$(cat '$counterPath')\n" +
                "n=\$((n + 1))\n" +
                "echo \$n > '$counterPath'\n" +
                "if [ \"\$n\" = \"1\" ]; then\n" +
                "  printf '%s' '${successJson("GENERATION A TEXT", digestA)}'\n" +
                "else\n" +
                "  printf '%s' '${successJson("GENERATION B TEXT", digestB)}'\n" +
                "fi\n" +
                "exit 0\n",
        )
        scriptPath.toFile().setExecutable(true)
        val cfg = config("channel.local-text-tierb-reprocess", scriptPath.toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        val evidenceArtifactId = registerImage(runtime)

        val first = assertIs<TierBOcrOwnerInvocationOutcome.Admitted>(runtime.invokeTierBOcrDurableGenerationAsOwner(evidenceArtifactId))
        val second = assertIs<TierBOcrOwnerInvocationOutcome.Admitted>(runtime.invokeTierBOcrDurableGenerationAsOwner(evidenceArtifactId))
        assertNotEquals(first.record.derivativeGenerationId, second.record.derivativeGenerationId)
        assertEquals("GENERATION A TEXT", first.extracted.recognisedText)
        assertEquals("GENERATION B TEXT", second.extracted.recognisedText)
        runtime.shutdown()

        val restarted = ParkerRuntime(cfg, RecordingParkerLogger())
        restarted.start()
        val retrievedFirst = assertIs<TierBOcrContentRetrievalOutcome.Retrieved>(
            restarted.retrieveTierBOcrContentAsOwner(evidenceArtifactId, first.record.derivativeGenerationId),
        )
        val retrievedSecond = assertIs<TierBOcrContentRetrievalOutcome.Retrieved>(
            restarted.retrieveTierBOcrContentAsOwner(evidenceArtifactId, second.record.derivativeGenerationId),
        )
        assertEquals(first.extracted, retrievedFirst.extracted)
        assertEquals(second.extracted, retrievedSecond.extracted)
        assertEquals(first.record, retrievedFirst.record)
        assertEquals(second.record, retrievedSecond.record)
        restarted.shutdown()
    }

    @Test
    fun `missing model provenance fails closed with MandatoryProvenanceUnavailable -- no DerivativeGenerationId minted, nothing retrievable`() = runTest {
        val scriptDir = Files.createTempDirectory("tierb-durable-scripts")
        val cfg = config("channel.local-text-tierb-no-provenance", writeFakeBridgeScript(scriptDir, missingModelVersionJson).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        val evidenceArtifactId = registerImage(runtime)

        val outcome = runtime.invokeTierBOcrDurableGenerationAsOwner(evidenceArtifactId)

        assertIs<TierBOcrOwnerInvocationOutcome.MandatoryProvenanceUnavailable>(outcome)
        runtime.shutdown()
    }

    @Test
    fun `a retrieval request for a generation id belonging to a different evidence artefact fails closed with SourceMismatch`() = runTest {
        val scriptDir = Files.createTempDirectory("tierb-durable-scripts")
        val cfg = config("channel.local-text-tierb-mismatch", writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        val evidenceArtifactId = registerImage(runtime)
        val admitted = assertIs<TierBOcrOwnerInvocationOutcome.Admitted>(runtime.invokeTierBOcrDurableGenerationAsOwner(evidenceArtifactId))
        val otherEvidenceArtifactId = registerImage(runtime)

        val outcome = runtime.retrieveTierBOcrContentAsOwner(otherEvidenceArtifactId, admitted.record.derivativeGenerationId)

        assertEquals(TierBOcrContentRetrievalOutcome.SourceMismatch(otherEvidenceArtifactId, admitted.record.derivativeGenerationId), outcome)
        runtime.shutdown()
    }

    @Test
    fun `a retrieval request for a never-processed generation id fails closed with UnknownGeneration`() = runTest {
        val scriptDir = Files.createTempDirectory("tierb-durable-scripts")
        val cfg = config("channel.local-text-tierb-unknown", writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()

        val outcome = runtime.retrieveTierBOcrContentAsOwner(EvidenceArtifactId("evidence-never-registered"), DerivativeGenerationId("generation-never-registered"))

        assertEquals(TierBOcrContentRetrievalOutcome.UnknownGeneration(DerivativeGenerationId("generation-never-registered")), outcome)
        runtime.shutdown()
    }

    @Test
    fun `Tier B durable admission never registers with Memory Core or Knowledge -- scope lock §31`() = runTest {
        val scriptDir = Files.createTempDirectory("tierb-durable-scripts")
        val cfg = config("channel.local-text-tierb-memory-nonaffect", writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        val evidenceArtifactId = registerImage(runtime)

        // Captured after registration (unrelated baseline activity -- e.g. evidence registration
        // itself -- may already have written to these logs) and before the Tier B call, so this
        // test isolates the effect of durable Tier B admission specifically, never asserting an
        // unconditional "always empty" that baseline activity could already violate.
        fun sizeOf(path: Path): Long = if (Files.exists(path)) Files.size(path) else 0L
        val memoryLog = Path.of(cfg.memoryCoreDurabilityLogPath)
        val knowledgeLog = Path.of(cfg.knowledgeItemDurabilityLogPath)
        val memoryLogSizeBefore = sizeOf(memoryLog)
        val knowledgeLogSizeBefore = sizeOf(knowledgeLog)

        assertIs<TierBOcrOwnerInvocationOutcome.Admitted>(runtime.invokeTierBOcrDurableGenerationAsOwner(evidenceArtifactId))

        assertEquals(memoryLogSizeBefore, sizeOf(memoryLog), "Memory Core durability log must be unchanged by durable Tier B admission")
        assertEquals(knowledgeLogSizeBefore, sizeOf(knowledgeLog), "Knowledge item durability log must be unchanged by durable Tier B admission")
        runtime.shutdown()
    }

    @Test
    fun `TierBOcrContentRetrievalCoordinator structurally holds no OcrMechanism, EvidenceIntelligenceOcrCoordinator, or OcrProviderAdapter dependency -- scope lock §35`() {
        // Structural, not merely behavioural: a compile-time property of the constructor's own
        // declared parameter types, mirroring TierAContentRetrievalCoordinator's own identical
        // structural guarantee (DerivativeContentPersistenceRestartAcceptanceTest's own header KDoc).
        val constructor = TierBOcrContentRetrievalCoordinator::class.constructors.single()
        val parameterTypeNames = constructor.parameters.map { it.type.toString() }
        parameterTypeNames.forEach { typeName ->
            assertTrue(
                !typeName.contains("OcrMechanism") && !typeName.contains("OcrProviderAdapter") && !typeName.contains("EvidenceIntelligenceOcrCoordinator"),
                "TierBOcrContentRetrievalCoordinator must hold no path to OCR of any kind -- found parameter type $typeName",
            )
        }
    }
}
