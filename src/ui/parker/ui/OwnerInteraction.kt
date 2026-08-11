package parker.ui

/** Whether the narrow owner-conversation interaction is accepting submissions. */
enum class OwnerInteractionAvailability {
    AVAILABLE,
    STOPPED,
}

/**
 * Text delivered through the owner-reply seam. Constructing this value does
 * not make text Parker-authored; only an [OwnerInteraction] implementation's
 * invocation of the supplied reply receiver does that at this boundary.
 */
data class OwnerReply(val text: String) {
    init {
        require(text.isNotBlank()) { "OwnerReply.text must not be blank" }
    }
}

/**
 * The presentation-safe result of one owner submission. Reply text is
 * deliberately absent: it travels only through [OwnerInteraction.submit]'s
 * separate reply receiver.
 */
sealed interface OwnerSubmissionDisposition {
    data class Delivered(val executionStatus: String) : OwnerSubmissionDisposition
    data class NotAccepted(val reason: String) : OwnerSubmissionDisposition
    data class Failed(val stage: String, val safeMessage: String) : OwnerSubmissionDisposition
    data class Planned(val resultCategory: String) : OwnerSubmissionDisposition
    data class Unavailable(val reason: String) : OwnerSubmissionDisposition
}

/**
 * The future owner UI's complete capability boundary. It is intentionally
 * limited to owner text submission, separately delivered reply text, and
 * basic availability. It is not a generic command or runtime facade.
 */
interface OwnerInteraction {
    val availability: OwnerInteractionAvailability

    suspend fun submit(
        ownerText: String,
        onReply: suspend (OwnerReply) -> Unit,
    ): OwnerSubmissionDisposition
}
