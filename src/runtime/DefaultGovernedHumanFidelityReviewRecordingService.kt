package parker.core.runtime

import parker.core.interfaces.*

/**
 * Minimum A4-MIN application boundary. Authority is the only dependency invoked before storage;
 * A2 remains solely responsible for prepare/publication and truthful audit ordering.
 */
class DefaultGovernedHumanFidelityReviewRecordingService(
    private val permissionEvaluator: HumanFidelityReviewRecordingPermissionEvaluator,
    private val storage: HumanFidelityReviewStorage,
) : GovernedHumanFidelityReviewRecordingService {

    override suspend fun record(
        request: GovernedHumanFidelityReviewRecordingRequest,
    ): GovernedHumanFidelityReviewRecordingResult {
        if (request.authority.target != request.review.target) {
            return GovernedHumanFidelityReviewRecordingResult.AuthorizationDenied(
                HumanFidelityReviewRecordingDenialReason.TARGET_MISMATCH,
            )
        }
        val permission = try {
            permissionEvaluator.evaluate(
                HumanFidelityReviewRecordingPermissionRequest(request.authority, request.review.target),
            )
        } catch (_: Exception) {
            return failure(GovernedHumanFidelityReviewRecordingFailureReason.AUTHORITY_EVALUATION_FAILED)
        }
        if (permission is HumanFidelityReviewRecordingPermissionResult.Denied) {
            return GovernedHumanFidelityReviewRecordingResult.AuthorizationDenied(permission.reason)
        }

        val preparation = try {
            storage.prepare(request.review)
        } catch (_: Exception) {
            return failure(GovernedHumanFidelityReviewRecordingFailureReason.STORAGE_OPERATION_FAILED)
        }
        if (preparation != HumanFidelityReviewPreparationResult.AlreadyPublished) {
            try {
                storage.publishPrepared(request.review.reviewId)
            } catch (_: Exception) {
                return failure(GovernedHumanFidelityReviewRecordingFailureReason.STORAGE_OPERATION_FAILED)
            }
        }

        val canonical = try {
            storage.retrieve(request.review.reviewId)
        } catch (_: Exception) {
            return failure(GovernedHumanFidelityReviewRecordingFailureReason.STORAGE_OPERATION_FAILED)
        } ?: return failure(GovernedHumanFidelityReviewRecordingFailureReason.CANONICAL_READBACK_MISSING)

        val exact = try {
            HumanFidelityReviewRecordCodec.encode(canonical)
                .contentEquals(HumanFidelityReviewRecordCodec.encode(request.review))
        } catch (_: Exception) {
            false
        }
        if (!exact) return failure(GovernedHumanFidelityReviewRecordingFailureReason.CANONICAL_READBACK_MISMATCH)

        return if (preparation == HumanFidelityReviewPreparationResult.AlreadyPublished) {
            GovernedHumanFidelityReviewRecordingResult.AlreadyRecorded(request.review.reviewId)
        } else {
            GovernedHumanFidelityReviewRecordingResult.Recorded(request.review.reviewId)
        }
    }

    private fun failure(reason: GovernedHumanFidelityReviewRecordingFailureReason) =
        GovernedHumanFidelityReviewRecordingResult.Failure(reason)
}
