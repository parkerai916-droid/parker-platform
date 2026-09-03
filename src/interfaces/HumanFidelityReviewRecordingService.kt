package parker.core.interfaces

data class GovernedHumanFidelityReviewRecordingRequest(
    val review: HumanFidelityReviewRecord,
    val authority: HumanFidelityReviewRecordingAuthorityScope,
)

enum class GovernedHumanFidelityReviewRecordingFailureReason {
    AUTHORITY_EVALUATION_FAILED,
    STORAGE_OPERATION_FAILED,
    CANONICAL_READBACK_MISSING,
    CANONICAL_READBACK_MISMATCH,
}

sealed interface GovernedHumanFidelityReviewRecordingResult {
    data class Recorded(val reviewId: HumanFidelityReviewId) : GovernedHumanFidelityReviewRecordingResult
    data class AlreadyRecorded(val reviewId: HumanFidelityReviewId) : GovernedHumanFidelityReviewRecordingResult
    data class AuthorizationDenied(val reason: HumanFidelityReviewRecordingDenialReason) :
        GovernedHumanFidelityReviewRecordingResult
    data class Failure(val reason: GovernedHumanFidelityReviewRecordingFailureReason) :
        GovernedHumanFidelityReviewRecordingResult
}

fun interface GovernedHumanFidelityReviewRecordingService {
    suspend fun record(request: GovernedHumanFidelityReviewRecordingRequest):
        GovernedHumanFidelityReviewRecordingResult
}
