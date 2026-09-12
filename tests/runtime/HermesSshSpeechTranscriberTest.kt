package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.SpeechTranscriptionOutcome

class HermesSshSpeechTranscriberTest {
    @Test
    fun `STT SSH command preserves stdin and does not use n`() = runTest {
        lateinit var process: FakeProcess
        val result = HermesSshSpeechTranscriber(
            processFactory = { arguments ->
                process = FakeProcess(arguments, "{\"status\":\"COMPLETED\",\"transcript\":\"hello\"}")
                process
            },
        ).transcribe(byteArrayOf(1, 2, 3), "audio/webm")

        assertIs<SpeechTranscriptionOutcome.Completed>(result)
        assertFalse(process.arguments.contains("-n"))
        assertTrue(process.stdin.toString(StandardCharsets.UTF_8).contains("audioBase64"))
        assertTrue(process.stdin.toString(StandardCharsets.UTF_8).contains("AQID"))
    }

    @Test
    fun `malformed remote output fails closed`() = runTest {
        val result = HermesSshSpeechTranscriber(
            processFactory = { FakeProcess(emptyList(), "not-json") },
        ).transcribe(byteArrayOf(1), "audio/webm")

        assertIs<SpeechTranscriptionOutcome.Failed>(result)
        assertTrue(result.reason == "INVALID_REMOTE_RESPONSE")
    }

    @Test
    fun `remote timeout remains bounded and destroys process`() = runTest {
        lateinit var process: FakeProcess
        val result = HermesSshSpeechTranscriber(
            timeoutMillis = 1,
            processFactory = { arguments ->
                process = FakeProcess(arguments, "", waits = false)
                process
            },
        ).transcribe(byteArrayOf(1), "audio/webm")

        assertIs<SpeechTranscriptionOutcome.Timeout>(result)
        assertTrue(process.destroyed)
    }

    private class FakeProcess(
        val arguments: List<String>,
        output: String,
        private val waits: Boolean = true,
    ) : Process() {
        val stdin = ByteArrayOutputStream()
        var destroyed = false

        private val stdout = ByteArrayInputStream(output.toByteArray(StandardCharsets.UTF_8))

        override fun getOutputStream(): OutputStream = stdin
        override fun getInputStream(): InputStream = stdout
        override fun getErrorStream(): InputStream = ByteArrayInputStream(ByteArray(0))
        override fun waitFor(): Int = 0
        override fun waitFor(timeout: Long, unit: java.util.concurrent.TimeUnit): Boolean = waits
        override fun exitValue(): Int = 0
        override fun destroy() { destroyed = true }
        override fun destroyForcibly(): Process { destroyed = true; return this }
        override fun isAlive(): Boolean = !destroyed && !waits
    }
}
