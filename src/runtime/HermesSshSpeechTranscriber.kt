package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Files
import java.util.Base64
import java.util.concurrent.TimeUnit
import parker.core.interfaces.SpeechTranscriber
import parker.core.interfaces.SpeechTranscriptionOutcome

/** Fixed, bounded Parker→Hermes STT transport; caller data never supplies SSH commands or profile names. */
class HermesSshSpeechTranscriber(
    private val host: String = "192.168.178.45",
    private val user: String = "steve",
    private val keyPath: Path = Path.of("/home/steve/.ssh/parker_hermes_stt_ed25519"),
    private val knownHostsPath: Path = Path.of("/home/steve/.ssh/parker_hermes_analysis_known_hosts"),
    private val timeoutMillis: Long = 120_000L,
    private val processFactory: (List<String>) -> Process = { ProcessBuilder(it).start() },
) : SpeechTranscriber {
    override suspend fun transcribe(audio: ByteArray, mediaType: String): SpeechTranscriptionOutcome {
        if (audio.isEmpty()) return SpeechTranscriptionOutcome.Failed("INVALID_AUDIO")
        if (audio.size > MAX_AUDIO_BYTES) return SpeechTranscriptionOutcome.Failed("AUDIO_TOO_LARGE")
        if (mediaType !in SUPPORTED_MEDIA_TYPES) return SpeechTranscriptionOutcome.UnsupportedAudio
        val request = "{\"mediaType\":\"${escape(mediaType)}\",\"audioBase64\":\"${Base64.getEncoder().encodeToString(audio)}\"}"
        val process = try { processFactory(listOf("ssh", "-T", "-i", keyPath.toString(), "-o", "BatchMode=yes", "-o", "IdentitiesOnly=yes", "-o", "StrictHostKeyChecking=yes", "-o", "UserKnownHostsFile=${knownHostsPath}", "$user@$host")) }
        catch (_: Exception) { return SpeechTranscriptionOutcome.BackendUnavailable }
        return try {
            process.outputStream.use { it.write(request.toByteArray(StandardCharsets.UTF_8)) }
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                process.destroy(); if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly()
                return SpeechTranscriptionOutcome.Timeout
            }
            val output = readBounded(process.inputStream)
            if (process.exitValue() != 0) SpeechTranscriptionOutcome.Failed("TRANSCRIPTION_FAILED") else parse(output)
        } catch (_: Exception) { SpeechTranscriptionOutcome.Failed("TRANSCRIPTION_FAILED") }
        finally { if (process.isAlive) process.destroyForcibly() }
    }

    private fun parse(bytes: ByteArray): SpeechTranscriptionOutcome {
        val json = String(bytes, StandardCharsets.UTF_8).trim()
        val status = jsonString(json, "status") ?: return SpeechTranscriptionOutcome.Failed("INVALID_REMOTE_RESPONSE")
        return when (status) {
            "COMPLETED" -> jsonString(json, "transcript")?.let(SpeechTranscriptionOutcome::Completed) ?: SpeechTranscriptionOutcome.Failed("INVALID_REMOTE_RESPONSE")
            "AUDIO_TOO_LARGE" -> SpeechTranscriptionOutcome.Failed("AUDIO_TOO_LARGE")
            "UNSUPPORTED_AUDIO" -> SpeechTranscriptionOutcome.UnsupportedAudio
            "TRANSCRIPTION_TIMEOUT" -> SpeechTranscriptionOutcome.Timeout
            else -> SpeechTranscriptionOutcome.Failed("TRANSCRIPTION_FAILED")
        }
    }

    private fun jsonString(json: String, key: String): String? {
        val match = Regex("\\\"${Regex.escape(key)}\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").find(json) ?: return null
        return match.groupValues[1].replace("\\\\\"", "\\\"").replace("\\\\n", "\n").replace("\\\\\\\\", "\\")
    }
    private fun readBounded(input: java.io.InputStream): ByteArray {
        val out = ByteArrayOutputStream(); val buffer = ByteArray(4096); var total = 0L
        while (true) { val n = input.read(buffer); if (n < 0) break; total += n; if (total > MAX_OUTPUT_BYTES) throw IllegalStateException(); out.write(buffer, 0, n) }
        return out.toByteArray()
    }
    private fun escape(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")

    companion object {
        const val MAX_AUDIO_BYTES: Int = 25 * 1024 * 1024
        const val MAX_OUTPUT_BYTES: Long = 16L * 1024L
        val SUPPORTED_MEDIA_TYPES = setOf("audio/aac", "audio/flac", "audio/m4a", "audio/mp3", "audio/mp4", "audio/mpeg", "audio/ogg", "audio/wav", "audio/webm", "audio/x-m4a", "audio/x-wav", "video/webm")
    }
}
