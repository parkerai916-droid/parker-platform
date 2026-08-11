package parker.ui

/** Authorship is structural so presentation status can never become Parker speech. */
sealed interface OwnerTranscriptEntry {
    val text: String

    data class Owner(override val text: String) : OwnerTranscriptEntry
    data class Parker(override val text: String) : OwnerTranscriptEntry
    data class System(override val text: String) : OwnerTranscriptEntry
}

enum class OwnerUiStatus {
    READY,
    PROCESSING,
    STOPPED,
    ERROR,
}

data class OwnerUiState(
    val transcript: List<OwnerTranscriptEntry> = emptyList(),
    val status: OwnerUiStatus = OwnerUiStatus.READY,
    val submissionActive: Boolean = false,
)

enum class OwnerSubmissionAttempt {
    STARTED,
    BLANK_REJECTED,
    ALREADY_PROCESSING,
    UNAVAILABLE,
}
