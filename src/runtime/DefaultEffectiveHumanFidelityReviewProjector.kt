package parker.core.runtime

import parker.core.interfaces.*

/** Read-only, exact-target projection. It has no derivative writer, provider, audit, or clock. */
class DefaultEffectiveHumanFidelityReviewProjector(
    private val storage: HumanFidelityReviewStorage,
) : EffectiveHumanFidelityReviewProjector {

    override suspend fun project(
        target: HumanFidelityReviewTarget,
        purpose: HumanFidelityEligibilityUse,
    ): EffectiveHumanFidelityReviewProjectionOutcome {
        val records = try {
            storage.listForExactTarget(target)
        } catch (_: Exception) {
            return failed(target, purpose)
        }
        if (records.any { it.target != target } || records.map { it.reviewId }.distinct().size != records.size) {
            return failed(target, purpose)
        }
        if (records.isEmpty()) return projected(target, purpose, null)
        if (records.size == 1) {
            val only = records.single()
            if (only.supersession != null || only.adjudication != null) return failed(target, purpose)
            return projected(target, purpose, only)
        }

        val byId = records.associateBy { it.reviewId }
        if (records.any { record ->
                record.supersession?.let { it.target != target || it.predecessorReviewId !in byId } == true ||
                    record.adjudication?.let { adjudication ->
                        adjudication.target != target ||
                            adjudication.conflictingReviewIds.any { it !in byId } ||
                            adjudication.selectedReviewId !in byId
                    } == true
            } || hasSupersessionCycle(records, byId)) {
            return conflict(target, purpose, records)
        }

        val adjudications = records.filter { it.adjudication != null }
        val selected = when {
            adjudications.isEmpty() -> {
                val predecessorIds = records.mapNotNull { it.supersession?.predecessorReviewId }.toSet()
                records.filter { it.reviewId !in predecessorIds }.singleOrNull()
            }
            adjudications.size == 1 -> {
                val adjudicator = adjudications.single()
                val adjudication = requireNotNull(adjudicator.adjudication)
                val completeConflictSet = records.map { it.reviewId }.toSet() - adjudicator.reviewId
                if (adjudication.conflictingReviewIds == completeConflictSet) {
                    byId.getValue(adjudication.selectedReviewId)
                } else null
            }
            else -> null
        }
        return if (selected == null) conflict(target, purpose, records) else projected(target, purpose, selected)
    }

    private fun hasSupersessionCycle(
        records: List<HumanFidelityReviewRecord>,
        byId: Map<HumanFidelityReviewId, HumanFidelityReviewRecord>,
    ): Boolean = records.any { start ->
        val visited = mutableSetOf<HumanFidelityReviewId>()
        var current: HumanFidelityReviewRecord? = start
        while (current?.supersession != null) {
            if (!visited.add(current.reviewId)) return@any true
            current = byId[current.supersession!!.predecessorReviewId]
        }
        false
    }

    private fun projected(
        target: HumanFidelityReviewTarget,
        purpose: HumanFidelityEligibilityUse,
        record: HumanFidelityReviewRecord?,
    ): EffectiveHumanFidelityReviewProjectionOutcome.Projected {
        if (record == null) {
            return outcome(
                purpose,
                EffectiveHumanFidelityReviewProjection(
                    target, HumanFidelityReviewState.UNREVIEWED, null, emptySet(), emptySet(),
                    denied(SourceConfirmedDenialReason.UNREVIEWED),
                ),
                0,
                0,
            )
        }
        val material = record.discrepancyOccurrences.count { it.severity == FidelityDiscrepancySeverity.MATERIAL }
        val eligibility = when {
            record.coverage.kind != HumanFidelityCoverageKind.FULL_GENERATION ->
                denied(SourceConfirmedDenialReason.PARTIAL_COVERAGE)
            record.reviewState == HumanFidelityReviewState.HUMAN_REVIEWED_PASS && record.discrepancyOccurrences.isEmpty() ->
                SourceConfirmedEligibility(SourceConfirmedEligibilityState.ALLOWED)
            material > 0 -> denied(SourceConfirmedDenialReason.MATERIAL_DISCREPANCY)
            else -> denied(SourceConfirmedDenialReason.MALFORMED_OR_UNSUPPORTED_STATE)
        }
        return outcome(
            purpose,
            EffectiveHumanFidelityReviewProjection(
                target,
                record.reviewState,
                record.coverage,
                setOf(record.reviewId),
                record.discrepancyOccurrences.map { it.discrepancyId }.toSet(),
                eligibility,
            ),
            material,
            record.systematicPatterns.size,
        )
    }

    private fun conflict(
        target: HumanFidelityReviewTarget,
        purpose: HumanFidelityEligibilityUse,
        records: List<HumanFidelityReviewRecord>,
    ) = outcome(
        purpose,
        EffectiveHumanFidelityReviewProjection(
            target,
            HumanFidelityReviewState.HUMAN_REVIEW_CONFLICT,
            null,
            records.map { it.reviewId }.toSet(),
            records.flatMap { it.discrepancyOccurrences }.map { it.discrepancyId }.toSet(),
            denied(SourceConfirmedDenialReason.UNRESOLVED_CONFLICT),
        ),
        records.sumOf { record -> record.discrepancyOccurrences.count { it.severity == FidelityDiscrepancySeverity.MATERIAL } },
        records.sumOf { it.systematicPatterns.size },
    )

    private fun outcome(
        purpose: HumanFidelityEligibilityUse,
        projection: EffectiveHumanFidelityReviewProjection,
        material: Int,
        patterns: Int,
    ) = EffectiveHumanFidelityReviewProjectionOutcome.Projected(
        EffectiveHumanFidelityReviewSummary(
            purpose,
            projection,
            material,
            patterns,
            projection.effectiveState == HumanFidelityReviewState.HUMAN_REVIEW_CONFLICT,
        ),
    )

    private fun failed(target: HumanFidelityReviewTarget, purpose: HumanFidelityEligibilityUse) =
        EffectiveHumanFidelityReviewProjectionOutcome.FailedClosed(target, purpose)

    private fun denied(reason: SourceConfirmedDenialReason) =
        SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED, reason)
}
