package parker.composition

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierBOcrOwnerInvocationOutcome
import parker.core.runtime.EvidenceRegistrationOutcome

/**
 * UI-INGESTION-8D: [buildOwnerHttpAdapter] is the exact construction
 * [main] uses to wire [OwnerEvidenceHttpServer]'s `operations:` seam --
 * the real, deployed production entry point. UI-INGESTION-8C deployed a
 * candidate where the newly accepted exact-evidence OCR derivative
 * discovery capability was wired into
 * [createOwnerUiRuntimeSession] (`OwnerUiRuntimeComposition.kt`, the
 * desktop-launcher composition root) but *not* here, so the deployed
 * adapter silently fell back to [OwnerEvidenceOperations]'s own safe
 * `{ emptyList() }` default -- discovered only by live production
 * verification, rolled back as a result. These tests exercise
 * [buildOwnerHttpAdapter] directly against a real, fully-wired
 * [ParkerRuntime], proving the real production wiring itself works, not
 * merely that [ParkerRuntime.discoverOcrDerivativeGenerationsAsOwner] does
 * (already covered elsewhere) or that some other adapter instance does.
 */
class BuildOwnerHttpAdapterCompositionTest {

    private val ownerPrincipalId = "user.build-owner-http-adapter-test"
    private val digestA = "a".repeat(64)

    private fun config(doclingBridgeScriptPath: String) = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-build-owner-http-adapter-test",
        evidenceStorageRootPath = Files.createTempDirectory("build-owner-http-adapter-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("build-owner-http-adapter-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("build-owner-http-adapter-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("build-owner-http-adapter-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("build-owner-http-adapter-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("build-owner-http-adapter-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("build-owner-http-adapter-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("build-owner-http-adapter-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = syntheticBridgeShellExecutable(),
        doclingBridgeScriptPath = doclingBridgeScriptPath,
        doclingTimeoutMillis = 30_000L,
    )

    private fun minimalPngBytes(): ByteArray = ByteArrayOutputStream().also {
        ImageIO.write(BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", it)
    }.toByteArray()

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "build-owner-http-adapter-test-source",
        sourceType = "test",
        acquisitionTime = java.time.Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun writeFakeBridgeScript(directory: Path, stdout: String): Path {
        val scriptPath = Files.createTempFile(directory, "fake-docling-bridge-", ".sh")
        Files.writeString(scriptPath, "#!/bin/sh\nprintf '%s' '$stdout'\nexit 0\n")
        scriptPath.toFile().setExecutable(true)
        return scriptPath
    }

    private fun successJson(): String =
        """{"status":"recognised","recognisedText":"BUILD OWNER HTTP ADAPTER TEXT","fidelity":"UNVERIFIED_LITERAL_TRANSCRIPTION","mechanismVersion":"docling-2.5.0","modelIdentity":"rapidocr-onnxruntime:PP-OCRv6_rec_small","modelVersion":"sha256:$digestA"}"""

    private suspend fun registerImage(runtime: ParkerRuntime): EvidenceArtifactId {
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(PrincipalId(ownerPrincipalId), CandidateEvidenceArtifact(minimalPngBytes(), receivedMediaType = "image/png"), candidateProvenance(), "test-document"),
        )
        return registered.acceptedEvidenceArtifact.evidenceArtifactId
    }

    @Test
    fun `Main-kt's real adapter construction discovers a real admitted OCR derivative generation -- it does not fall back to the safe empty-list default`() = runTest {
        val scriptDir = Files.createTempDirectory("build-owner-http-adapter-scripts")
        val cfg = config(writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        try {
            val evidenceArtifactId = registerImage(runtime)
            val admitted = assertIs<TierBOcrOwnerInvocationOutcome.Admitted>(runtime.invokeTierBOcrDurableGenerationAsOwner(evidenceArtifactId))

            val adapter = buildOwnerHttpAdapter(runtime, cfg)
            val discovered = adapter.discoverOcrDerivativeGenerations(evidenceArtifactId)

            assertEquals(1, discovered.size, "the real production adapter must not silently return the empty-list default")
            assertEquals(admitted.record.derivativeGenerationId.value, discovered.single().derivativeGenerationId)
            assertEquals(evidenceArtifactId.value, discovered.single().evidenceArtifactId)
        } finally {
            runtime.shutdown()
        }
    }

    @Test
    fun `Main-kt's real adapter construction correctly returns an empty list for evidence with no admitted OCR derivative`() = runTest {
        val scriptDir = Files.createTempDirectory("build-owner-http-adapter-scripts-empty")
        val cfg = config(writeFakeBridgeScript(scriptDir, successJson()).toString())
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        try {
            val evidenceArtifactId = registerImage(runtime)
            val adapter = buildOwnerHttpAdapter(runtime, cfg)

            assertEquals(emptyList(), adapter.discoverOcrDerivativeGenerations(evidenceArtifactId))
        } finally {
            runtime.shutdown()
        }
    }

    @Test
    fun `dual-composition parity -- both Main-kt and OwnerUiRuntimeComposition-kt supply the same real discovery dependency`() {
        // UI-INGESTION-8C's root cause was these two independent, deliberately-separate
        // OwnerUiEvidenceRuntimeAdapter(...) constructions (see
        // OwnerUiRuntimeCompositionTest's own "preserves capability and isolation boundaries"
        // test, which requires this file's construction to remain a distinct, independently
        // auditable source-of-truth from the desktop launcher's -- unifying them would weaken that
        // guarantee) silently drifting apart: one received discoverOcrDerivativeGenerationsAsOwner,
        // the other did not. This does not unify the two constructions -- it only proves both now
        // independently supply the same real dependency, exactly like every other capability both
        // already share (importEvidenceFileAsOwner, invokeTierAIngestionAsOwner, etc).
        val mainSource = Path.of("src/composition/Main.kt").toFile().readText()
        val compositionSource = Path.of("src/composition/OwnerUiRuntimeComposition.kt").toFile().readText()

        assertTrue(
            mainSource.contains("discoverOcrDerivativeGenerationsAsOwner = runtime::discoverOcrDerivativeGenerationsAsOwner"),
            "Main.kt (the real production entry point) must wire discoverOcrDerivativeGenerationsAsOwner",
        )
        assertTrue(
            compositionSource.contains("discoverOcrDerivativeGenerationsAsOwner = runtime::discoverOcrDerivativeGenerationsAsOwner"),
            "OwnerUiRuntimeComposition.kt must also wire discoverOcrDerivativeGenerationsAsOwner",
        )
    }
}
