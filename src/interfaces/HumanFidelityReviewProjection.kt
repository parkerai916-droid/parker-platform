package parker.core.interfaces

/**
 * A narrow content-eligibility context, not an Authorization Purpose. Raw provider retrieval is
 * deliberately absent: it remains governed by its existing retrieval authority and is never
 * denied merely because source-confirmed use is denied.
 */
enum class HumanFidelityEligibilityUse {
    SOURCE_CONFIRMED_WHOLE_GENERATION,
}

data class EffectiveHumanFidelityReviewSummary(
    val purpose: HumanFidelityEligibilityUse,
    val projection: EffectiveHumanFidelityReviewProjection,
    val materialDiscrepancyCount: Int,
    val systematicPatternCount: Int,
    val unresolvedConflict: Boolean,
) {
    init {
        require(materialDiscrepancyCount >= 0)
        require(systematicPatternCount >= 0)
        require(unresolvedConflict == (projection.effectiveState == HumanFidelityReviewState.HUMAN_REVIEW_CONFLICT))
    }
}

sealed interface EffectiveHumanFidelityReviewProjectionOutcome {
    data class Projected(val summary: EffectiveHumanFidelityReviewSummary) :
        EffectiveHumanFidelityReviewProjectionOutcome

    data class FailedClosed(
        val target: HumanFidelityReviewTarget,
        val purpose: HumanFidelityEligibilityUse,
        val eligibility: SourceConfirmedEligibility = SourceConfirmedEligibility(
            SourceConfirmedEligibilityState.DENIED,
            SourceConfirmedDenialReason.MALFORMED_OR_UNSUPPORTED_STATE,
        ),
    ) : EffectiveHumanFidelityReviewProjectionOutcome {
        init {
            require(eligibility == SourceConfirmedEligibility(
                SourceConfirmedEligibilityState.DENIED,
                SourceConfirmedDenialReason.MALFORMED_OR_UNSUPPORTED_STATE,
            ))
        }
    }
}

fun interface EffectiveHumanFidelityReviewProjector {
    suspend fun project(
        target: HumanFidelityReviewTarget,
        purpose: HumanFidelityEligibilityUse,
    ): EffectiveHumanFidelityReviewProjectionOutcome
}
