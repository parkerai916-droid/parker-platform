package parker.core.runtime

import parker.core.interfaces.PlanCandidate
import parker.core.interfaces.PlanCandidateId
import parker.core.interfaces.PlanDecision
import parker.core.interfaces.PlanDecisionResult
import parker.core.interfaces.PlanRejection
import parker.core.interfaces.PlanRejectionReason

/**
 * Sprint 3, Track D, Unit D2 (Alignment Pass). The concrete, deterministic
 * [PlanDecision] this Unit supplies -- per
 * `docs/architecture/PLANNER_RUNTIME_PROGRESSION_DESIGN.md` Section 11,
 * `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` §5, and this
 * Unit's own instructions: "Maintain deterministic behaviour. No
 * randomness. No heuristic scoring. No hidden prioritisation. Selection
 * behaviour must be completely explainable and repeatable."
 *
 * ## The rule, stated in full
 *
 * Candidates are evaluated once, in the exact order supplied to [decide]
 * ("generation order"). For each candidate, in order, the *first* of the
 * following that applies determines its outcome:
 *
 * 1. If its [PlanCandidate.planCandidateId] was already seen earlier in
 *    this same call (a duplicate), it is rejected:
 *    [PlanRejectionReason.DUPLICATE_CANDIDATE_ID].
 * 2. Otherwise, if its [PlanCandidate.goal] is blank, it is rejected:
 *    [PlanRejectionReason.BLANK_GOAL].
 * 3. Otherwise, if its [PlanCandidate.goal] does not equal the Planning
 *    Session's own Goal (the [decide] `goal` parameter), it is rejected:
 *    [PlanRejectionReason.GOAL_MISMATCH].
 * 4. Otherwise, if a winner has already been chosen earlier in this same
 *    call, it is rejected: [PlanRejectionReason.NOT_SELECTED] (an earlier,
 *    valid candidate already won).
 * 5. Otherwise, it is the winner.
 *
 * This is a plain total ordering by (validity, generation order) --
 * deliberately **not** ranked by [parker.core.interfaces.RiskEstimate] or
 * any other weighted/scored criterion, even though
 * `PlannerRuntimeSpecification.md` Section 4 names Risk as a Plan
 * Candidate characteristic: introducing a risk-based ranking would itself
 * be a scoring function, which this Unit's own governing design document
 * and instructions explicitly rule out. "Evaluation" here means
 * structural validity checking (non-blank, matching, non-duplicate) only
 * -- never a weighing of otherwise-valid candidates against each other.
 *
 * Every rejection carries exactly one of [PlanRejectionReason]'s four
 * closed values, plus a non-blank `detail` naming the specifics -- never
 * a numeric score -- so that [PlanRejection] alone is sufficient to
 * explain and reproduce the outcome, per this Unit's "completely
 * explainable and repeatable" requirement. Calling [decide] twice with
 * the same arguments always produces an identical [PlanDecisionResult] --
 * no clock read, no random value, and no external state is consulted.
 *
 * **`suspend`, per `PLANNER_RUNTIME_CONTRACT_DESIGN.md` §5.** This body
 * never actually suspends -- the signature is `suspend` only because
 * [PlanDecision] itself is (to leave room for a future model-backed
 * implementation without a breaking interface change), not because this
 * deterministic implementation needs it. No blocking call is introduced
 * to compensate; there was never a blocking call here to begin with.
 */
class DefaultPlanDecision : PlanDecision {

    override suspend fun decide(goal: String, candidates: List<PlanCandidate>): PlanDecisionResult {
        val rejections = mutableListOf<PlanRejection>()
        val seenIds = mutableSetOf<PlanCandidateId>()
        var winner: PlanCandidate? = null

        for (candidate in candidates) {
            val rejection = when {
                !seenIds.add(candidate.planCandidateId) ->
                    PlanRejectionReason.DUPLICATE_CANDIDATE_ID to
                        ("duplicate planCandidateId '${candidate.planCandidateId.value}': a candidate with " +
                            "this identifier was already evaluated earlier in generation order")
                candidate.goal.isBlank() ->
                    PlanRejectionReason.BLANK_GOAL to "PlanCandidate.goal must not be blank"
                candidate.goal != goal ->
                    PlanRejectionReason.GOAL_MISMATCH to
                        ("PlanCandidate.goal ('${candidate.goal}') does not match this Planning Session's " +
                            "own Goal ('$goal')")
                winner != null ->
                    PlanRejectionReason.NOT_SELECTED to
                        ("an earlier, valid candidate ('${winner.planCandidateId.value}') was already " +
                            "chosen in generation order")
                else -> null
            }

            if (rejection != null) {
                val (reason, detail) = rejection
                rejections += PlanRejection(candidate.planCandidateId, reason, detail)
            } else {
                winner = candidate
            }
        }

        val finalWinner = winner
        return if (finalWinner != null) {
            PlanDecisionResult.Selected(finalWinner, rejections)
        } else {
            PlanDecisionResult.NoViableCandidate(rejections)
        }
    }
}
