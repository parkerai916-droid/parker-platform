package parker.core.runtime

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionRequest

/**
 * OCR Mechanism -- Docling Concrete Adapter Implementation Unit. Real
 * [ProcessBuilderDoclingSubprocessInvoker] coverage -- mirrors
 * `ProcessBuilderQmdSubprocessInvokerTest.kt`'s own established style:
 * every test here either proves a genuine process-launch attempt occurs
 * (never a shell), or exercises a pure, separately-extracted function
 * directly, so no test in this file depends on Python or Docling being
 * installed.
 */
class ProcessBuilderDoclingSubprocessInvokerTest {

    private fun configuration(
        pythonExecutablePath: String,
        bridgeScriptPath: String = "tools/docling-ocr-bridge.py",
    ): DoclingOcrProviderAdapterConfiguration = DoclingOcrProviderAdapterConfiguration(
        pythonExecutablePath = pythonExecutablePath,
        bridgeScriptPath = bridgeScriptPath,
    )

    // ---- 12. Command is argument-vector based, never a shell string ----

    @Test
    fun `a nonexistent python executable surfaces as a genuine process-start failure -- proving a real launch attempt occurred, never a shell`() {
        val invoker = ProcessBuilderDoclingSubprocessInvoker(
            configuration(pythonExecutablePath = "/nonexistent/python-does-not-exist-9f3a"),
        )

        val result = invoker.invoke("{}", timeoutMillis = 1_000)

        assertEquals(-1, result.exitCode)
        assertFalse(result.timedOut)
        assertTrue("failed to start" in result.stderr)
        assertTrue("/nonexistent/python-does-not-exist-9f3a" in result.stderr)
    }

    // Adversarial: a single string containing shell metacharacters must be treated as one,
    // literal (nonexistent) executable name by ProcessBuilder(List<String>) -- never split,
    // interpreted, or executed as a shell command. If a shell were ever used here, this would
    // be a command-injection vulnerability; because ProcessBuilder(List<String>) is used, the
    // entire string is looked up as a single program name and fails identically to any other
    // nonexistent path.
    @Test
    fun `a python executable path containing shell metacharacters is never interpreted by a shell`() {
        val dangerousPath = "python3; touch /tmp/parker-docling-test-injection-marker-should-never-exist"
        val markerFile = java.io.File("/tmp/parker-docling-test-injection-marker-should-never-exist")
        markerFile.delete()

        val invoker = ProcessBuilderDoclingSubprocessInvoker(configuration(pythonExecutablePath = dangerousPath))
        val result = invoker.invoke("{}", timeoutMillis = 1_000)

        assertEquals(-1, result.exitCode)
        assertFalse(markerFile.exists(), "no shell must ever have interpreted the ';' as a command separator")
        markerFile.delete()
    }

    // ---- 13. Offline flags/config passed ----

    @Test
    fun `applyDoclingOfflineEnvironment sets exactly the two offline-mode flags, nothing else`() {
        val environment = mutableMapOf("PATH" to "/usr/bin", "EVIDENCE_DERIVED_VALUE" to "should-be-untouched")

        applyDoclingOfflineEnvironment(environment)

        assertEquals("1", environment["HF_HUB_OFFLINE"])
        assertEquals("1", environment["TRANSFORMERS_OFFLINE"])
        assertEquals("/usr/bin", environment["PATH"], "pre-existing environment entries must be left alone")
        assertEquals("should-be-untouched", environment["EVIDENCE_DERIVED_VALUE"], "this function must never remove or alter unrelated entries")
    }

    @Test
    fun `applyDoclingOfflineEnvironment is idempotent -- calling it twice does not change the result`() {
        val environment = mutableMapOf<String, String>()
        applyDoclingOfflineEnvironment(environment)
        applyDoclingOfflineEnvironment(environment)

        assertEquals(setOf("HF_HUB_OFFLINE", "TRANSFORMERS_OFFLINE"), environment.keys)
    }

    // ---- 8, 9. stdout/stderr cap -- BoundedStreamDrain, tested directly, no subprocess ----

    @Test
    fun `BoundedStreamDrain captures the full stream when under the cap`() {
        val drain = BoundedStreamDrain(capBytes = 1_000)
        drain.drain(ByteArrayInputStream("line one\nline two\n".toByteArray(Charsets.UTF_8)))

        assertFalse(drain.capExceeded)
        assertEquals("line one\nline two\n", drain.text())
    }

    @Test
    fun `BoundedStreamDrain stops appending once the cap is exceeded but still drains to completion`() {
        val lines = (1..1_000).joinToString("\n") { "x".repeat(50) }
        val drain = BoundedStreamDrain(capBytes = 200)

        drain.drain(ByteArrayInputStream(lines.toByteArray(Charsets.UTF_8)))

        assertTrue(drain.capExceeded)
        assertTrue(drain.text().toByteArray(Charsets.UTF_8).size <= 250, "buffered text must not grow materially beyond the configured cap")
    }

    @Test
    fun `BoundedStreamDrain with an empty stream never sets capExceeded`() {
        val drain = BoundedStreamDrain(capBytes = 10)
        drain.drain(ByteArrayInputStream(ByteArray(0)))

        assertFalse(drain.capExceeded)
        assertEquals("", drain.text())
    }

    // ---- Real Kotlin<->Python protocol compatibility, against the real script ----
    //
    // Unlike every other test in this file, these two genuinely invoke
    // tools/docling-ocr-bridge.py as a real subprocess -- no fake invoker, no
    // Docling installation required. Both exercise a request-file shape that is
    // deterministic without Docling (a nonexistent modelCacheDir, per that
    // script's own contract, is rejected during its pre-flight check, before any
    // `import docling` is ever attempted). Skipped gracefully, never failed, on
    // an environment lacking python3 or the script itself -- mirroring this
    // codebase's own established "skip, don't fail, on a missing optional
    // external prerequisite" discipline.

    private fun realBridgeScriptPath(): String {
        val file = java.io.File("tools/docling-ocr-bridge.py")
        return file.absolutePath
    }

    private fun assumePythonAndBridgeScriptAvailable(): String {
        val scriptFile = java.io.File(realBridgeScriptPath())
        assumeTrue(scriptFile.exists(), "tools/docling-ocr-bridge.py not found at ${scriptFile.absolutePath} -- skipping real protocol-compatibility test")
        val pythonProbe = try {
            ProcessBuilder(listOf("python3", "--version")).start().apply { waitFor() }.exitValue() == 0
        } catch (_: Exception) {
            false
        }
        assumeTrue(pythonProbe, "no python3 executable found on PATH -- skipping real protocol-compatibility test")
        return scriptFile.absolutePath
    }

    @Test
    fun `real bridge script -- a nonexistent modelCacheDir is rejected with exit code 2, matching the adopted exit-code contract`() {
        val scriptPath = assumePythonAndBridgeScriptAvailable()
        val invoker = ProcessBuilderDoclingSubprocessInvoker(
            configuration(pythonExecutablePath = "python3", bridgeScriptPath = scriptPath),
        )
        val requestJson = """{"protocolVersion":"1","sourceFilePath":"/tmp/does-not-matter.pdf","mediaType":"application/pdf","configurationProfile":"docling-bridge-v1","modelCacheDir":"/nonexistent/model-cache-9f3a"}"""

        val result = invoker.invoke(requestJson, timeoutMillis = 15_000)

        assertEquals(2, result.exitCode, "the real script's own exit code for a missing modelCacheDir must match EXIT_CODE_MISSING_ASSETS exactly -- stderr: ${result.stderr}")
        assertEquals("", result.stdout, "no stdout output is permitted on a non-zero exit")
        assertFalse(result.timedOut)
    }

    @Test
    fun `real bridge script through the real adapter -- a nonexistent modelCacheDir maps end-to-end to ProcessingOrDependencyFailure`() = runTest {
        val scriptPath = assumePythonAndBridgeScriptAvailable()
        val configuration = configuration(pythonExecutablePath = "python3", bridgeScriptPath = scriptPath)
            .copy(modelCacheDir = "/nonexistent/model-cache-9f3a")
        val adapter = DoclingOcrProviderAdapter(configuration, ProcessBuilderDoclingSubprocessInvoker(configuration))
        val request = OcrRecognitionRequest(
            sourceEvidenceId = EvidenceArtifactId("evidence-1"),
            content = byteArrayOf(1, 2, 3),
            mediaType = "application/pdf",
        )

        val outcome = adapter.recognise(request)

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure, "expected ProcessingOrDependencyFailure, got: $outcome")
    }
}
