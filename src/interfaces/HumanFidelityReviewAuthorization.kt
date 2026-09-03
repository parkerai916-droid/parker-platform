package parker.core.interfaces

/** The sole Sequence A purpose for recording immutable human-fidelity review facts. */
val HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE: AuthorizationPurposeId =
    AuthorizationPurposeId("document-ingestion.human-fidelity-review-recording")

/**
 * One explicitly owner-granted, exact-target authority scope. This is request authority only:
 * it is neither a stored review nor an audit fact and carries no correction/provider authority.
 */
data class HumanFidelityReviewRecordingAuthorityScope(
    val principalId: PrincipalId,
    val authorizationPurpose: AuthorizationPurposeId?,
    val target: HumanFidelityReviewTarget,
)

/**
 * The target proposed for later A4 recording is carried separately so the authority evaluator
 * must prove exact equality before any prepare, publish, or audit mutation can be attempted.
 */
data class HumanFidelityReviewRecordingPermissionRequest(
    val authority: HumanFidelityReviewRecordingAuthorityScope,
    val proposedReviewTarget: HumanFidelityReviewTarget,
)

enum class HumanFidelityReviewRecordingDenialReason {
    WRONG_PRINCIPAL,
    MISSING_OR_WRONG_PURPOSE,
    PURPOSE_NOT_ACTIVE,
    TARGET_MISMATCH,
    PERMISSION_POLICY_DENIED,
}

sealed interface HumanFidelityReviewRecordingPermissionResult {
    data object Authorized : HumanFidelityReviewRecordingPermissionResult
    data class Denied(val reason: HumanFidelityReviewRecordingDenialReason) :
        HumanFidelityReviewRecordingPermissionResult
}

/** A3 authority boundary only; implementations must perform no review or audit mutation. */
fun interface HumanFidelityReviewRecordingPermissionEvaluator {
    suspend fun evaluate(request: HumanFidelityReviewRecordingPermissionRequest):
        HumanFidelityReviewRecordingPermissionResult
}
