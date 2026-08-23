package parker.integration

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionRequest
import parker.core.runtime.DoclingOcrProviderAdapter
import parker.core.runtime.DoclingOcrProviderAdapterConfiguration
import parker.core.runtime.ProcessBuilderDoclingSubprocessInvoker

/**
 * OCR Mechanism, Docling Concrete Adapter -- this Unit's own required
 * "genuine live integration test" (mirroring
 * `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s own established shape
 * exactly, Section 19/§23 of both the Bridge Contract and the Adapter Plan):
 * runs the real, production `DoclingOcrProviderAdapter` against a real,
 * provisioned Docling installation and a real `tools/docling-ocr-bridge.py`
 * subprocess, over the immutable document-ingestion bake-off fixtures.
 *
 * Lives under `tests/integration`, this repository's own pre-existing
 * detached `liveModelEvaluation` source set (`build.gradle.kts`) --
 * deliberately NOT part of the ordinary `test`/`check`/`build` lifecycle.
 * Gated by [LIVE_PROPERTY]: with the property absent, every test below is
 * skipped via `assumeTrue`, not failed.
 *
 * Run explicitly, from a machine with Docling provisioned into an isolated
 * Python environment, via:
 * ```
 * ./gradlew doclingOcrProviderAdapterLiveAcceptance \
 *   -DDOCLING_TEST_PYTHON=/home/steve/docling-venv/bin/python3
 * ```
 * (task registered in `build.gradle.kts`).
 *
 * Configuration (Python executable, bridge script path, model cache
 * directory) is read from environment variables, mirroring
 * `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s own identical discipline
 * -- never a fabricated, machine-specific fallback, except the bridge
 * script's own repository-relative path (this file's own default, since
 * that path is not deployment-specific -- it is this repository's own
 * committed file).
 */
class DoclingOcrProviderAdapterLiveAcceptanceTest {

    companion object {
        private const val LIVE_PROPERTY = "parker.ocr.docling.live.enabled"
    }

    private fun resolvedPythonExecutablePath(): String? =
        System.getenv("DOCLING_TEST_PYTHON")?.takeIf { it.isNotBlank() }

    private fun resolvedBridgeScriptPath(): String =
        System.getenv("DOCLING_TEST_BRIDGE_SCRIPT")?.takeIf { it.isNotBlank() }
            ?: Path.of("tools", "docling-ocr-bridge.py").toAbsolutePath().toString()

    /**
     * Explicit, diagnosable skip -- never a fabricated path -- when this
     * machine has no provisioned Docling environment. A real, lightweight
     * subprocess probe (`python -c "import docling"`), not merely a file-
     * existence check, since the genuinely load-bearing prerequisite is
     * "Docling is importable in this interpreter," not "some python3
     * executable exists somewhere."
     */
    private fun assumeLiveDoclingPrerequisitesProvisioned() {
        val pythonExecutablePath = resolvedPythonExecutablePath()
        assumeTrue(
            pythonExecutablePath != null,
            "Live Docling prerequisites are not provisioned on this machine -- missing: " +
                "DOCLING_TEST_PYTHON (the Python executable inside a provisioned Docling virtual " +
                "environment). Docling is never auto-discovered, guessed, or downloaded; provision it and " +
                "set this environment variable explicitly to run this live acceptance instrument.",
        )
        val probe = try {
            ProcessBuilder(listOf(pythonExecutablePath!!, "-c", "import docling")).start().apply { waitFor() }.exitValue() == 0
        } catch (_: Exception) {
            false
        }
        assumeTrue(probe, "DOCLING_TEST_PYTHON=$pythonExecutablePath cannot import the docling package -- skipping")
    }

    private fun liveConfiguration(): DoclingOcrProviderAdapterConfiguration {
        val pythonExecutablePath = resolvedPythonExecutablePath()!!
        val bridgeScriptPath = resolvedBridgeScriptPath()
        val modelCacheDir = System.getenv("DOCLING_TEST_MODEL_CACHE_DIR")?.takeIf { it.isNotBlank() }
        return DoclingOcrProviderAdapterConfiguration(
            pythonExecutablePath = pythonExecutablePath,
            bridgeScriptPath = bridgeScriptPath,
            modelCacheDir = modelCacheDir,
            timeoutMillis = 120_000,
        )
    }

    private fun fixtureBytes(name: String): ByteArray =
        Files.readAllBytes(Path.of("tests", "fixtures", "document-ingestion-bakeoff", "fixtures", name))

    @Test
    fun `live Docling subprocess recognises the accepted scanned PDF fixture`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))
        val originalBytes = fixtureBytes("03-scanned.pdf")
        val originalCopy = originalBytes.copyOf()

        val outcome = adapter.recognise(
            OcrRecognitionRequest(
                sourceEvidenceId = EvidenceArtifactId("live-acceptance-scanned-pdf"),
                content = originalBytes,
                mediaType = "application/pdf",
            ),
        )

        assertTrue(outcome is OcrRecognitionOutcome.Recognised, "expected Recognised, got: $outcome")
        val result = (outcome as OcrRecognitionOutcome.Recognised).result
        println("live Docling PDF recognition: ${result.recognisedText}")
        assertTrue(result.recognisedText.contains("SCANNED SYNTHETIC EVIDENCE"), "expected the fixture's own known text, got: ${result.recognisedText}")
        assertTrue(result.recognisedText.contains("Māori"), "expected Unicode (Māori macron) text to survive the full JSON round-trip intact")
        assertEquals("docling", result.identity.mechanismIdentity)
        assertTrue(result.identity.mechanismVersion != null, "a real Docling installation must report a genuine mechanismVersion")
        assertTrue(result.confidence != null && result.confidence!! in 0.0..1.0, "expected a genuine document-level confidence scalar")
        assertTrue(originalBytes.contentEquals(originalCopy), "source bytes must remain byte-identical after a real invocation")
    }

    @Test
    fun `live Docling subprocess recognises the accepted PNG fixture`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))

        val outcome = adapter.recognise(
            OcrRecognitionRequest(
                sourceEvidenceId = EvidenceArtifactId("live-acceptance-png"),
                content = fixtureBytes("07-text-image.png"),
                mediaType = "image/png",
            ),
        )

        assertTrue(outcome is OcrRecognitionOutcome.Recognised, "expected Recognised, got: $outcome")
        val result = (outcome as OcrRecognitionOutcome.Recognised).result
        println("live Docling PNG recognition: ${result.recognisedText}")
        assertTrue(result.recognisedText.contains("PARKER TEXT IMAGE"), "expected the fixture's own known text, got: ${result.recognisedText}")
    }

    @Test
    fun `live Docling subprocess fails closed, never a fabricated success, when the model cache is genuinely empty`() = runTest {
        // Mirrors QmdRelevanceMechanismLiveAcceptanceTest.kt's own "a missing local embedding
        // model fails closed" test exactly, one tier further along the OCR chain: an empty,
        // fresh temp directory as modelCacheDir, HF_HUB_OFFLINE already set by the adapter
        // itself -- Docling's own layout-model resolution must fail closed rather than
        // triggering an on-demand network download.
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val emptyCacheDir = Files.createTempDirectory("parker-docling-live-empty-model-cache-")
        try {
            val configuration = liveConfiguration().copy(modelCacheDir = emptyCacheDir.toAbsolutePath().toString())
            val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))

            val outcome = adapter.recognise(
                OcrRecognitionRequest(
                    sourceEvidenceId = EvidenceArtifactId("live-acceptance-empty-cache"),
                    content = fixtureBytes("07-text-image.png"),
                    mediaType = "image/png",
                ),
            )

            assertTrue(
                outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure,
                "an empty modelCacheDir must fail closed as ProcessingOrDependencyFailure, never a fabricated success -- got: $outcome",
            )
        } finally {
            emptyCacheDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `live Docling subprocess maps a corrupt source to UnsupportedOrInaccessibleInput`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))

        val outcome = adapter.recognise(
            OcrRecognitionRequest(
                sourceEvidenceId = EvidenceArtifactId("live-acceptance-corrupt"),
                content = "not a real pdf".toByteArray(Charsets.UTF_8),
                mediaType = "application/pdf",
            ),
        )

        assertTrue(outcome is OcrRecognitionOutcome.UnsupportedOrInaccessibleInput, "expected UnsupportedOrInaccessibleInput, got: $outcome")
    }
}
