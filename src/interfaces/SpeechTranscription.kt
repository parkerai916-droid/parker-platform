package parker.core.interfaces

interface SpeechTranscriber {
    suspend fun transcribe(audio: ByteArray, mediaType: String): SpeechTranscriptionOutcome
}

sealed interface SpeechTranscriptionOutcome {
    data class Completed(val transcript: String) : SpeechTranscriptionOutcome {
        init { require(transcript.isNotBlank() && transcript.length <= 8_000) }
    }
    data object BackendUnavailable : SpeechTranscriptionOutcome
    data object UnsupportedAudio : SpeechTranscriptionOutcome
    data object Timeout : SpeechTranscriptionOutcome
    data class Failed(val reason: String) : SpeechTranscriptionOutcome {
        init { require(reason.isNotBlank() && reason.length <= 256) }
    }
}
