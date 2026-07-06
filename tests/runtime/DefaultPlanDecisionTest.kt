package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.PlanCandidate
import parker.core.interfaces.PlanCandidateId
import parker.core.interfaces.PlanDecisionResult
import parker.core.interfaces.PlanRejectionReason
import parker.core.interfaces.RiskEstimate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 3, Track D, Unit D2 (Alignment Pass). Unit tests of
 * [DefaultPlanDecision]'s own rule, stated in full in that class's KDoc:
 * evaluate candidates in generation order; the first valid, non-duplicate
 * candidate wins; every other candidate (invalid or simply not chosen) is
 * a [parker.core.interfaces.PlanRejection] with an exact
 * [PlanRejectionReason] plus a non-blank `detail`. Wrapped in [runTest]
 * throughout because [DefaultPlanDecision.decide] is now `suspend`
 * (`docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` §5) -- no
 * blocking call was introduced to avoid this; the project's existing
 * coroutine-test convention is used instead, exactly as every other
 * suspend-function test in this codebase already does.
 */
class DefaultPlanDecisionTest {

    private val decision = DefaultPlanDecision()
    private val goal = "read today's calendar"

    private fun candidate(
        id: String,
        goal: String = this.goal,
        riskEstimate: RiskEstimate? = null,
    ) = PlanCandidate(
        planCandidateId = PlanCandidateId(id),
        goal = goal,
        riskEstimate = riskEstimate,
    )

    // --- zero candidates ---

    @Test
    fun `zero candidates yields NoViableCandidate with no rejections`() = runTest {
        val result = decision.decide(goal, emptyList())

        val noViable = assertIs<PlanDecisionResult.NoViableCandidate>(result)
        assertTrue(noViable.rejections.isEmpty())
    }

    // --- one candidate ---

    @Test
    fun `one valid candidate is selected with no rejections`() = runTest {
        val only = candidate("cand-1")

        val result = decision.decide(goal, listOf(only))

        val selected = assertIs<PlanDecisionResult.Selected>(result)
        assertEquals(only, selected.winner)
        assertTrue(selected.rejections.isEmpty())
    }

    @Test
    fun `one invalid candidate (blank goal) yields NoViableCandidate with exactly one rejection`() = runTest {
        val invalid = candidate("cand-1", goal = "")

        val result = decision.decide(goal, listOf(invalid))

        val noViable = assertIs<PlanDecisionResult.NoViableCandidate>(result)
        assertEquals(1, noViable.rejections.size)
        assertEquals(PlanCandidateId("cand-1"), noViable.rejections.single().planCandidateId)
        assertEquals(PlanRejectionReason.BLANK_GOAL, noViable.rejections.single().reason)
    }

    // --- multiple candidates / deterministic selection ---

    @Test
    fun `multiple valid candidates -- the first in generation order is selected, the rest are rejected as not-selected`() = runTest {
        val first = candidate("cand-1")
        val second = candidate("cand-2")
        val third = candidate("cand-3")

        val result = decision.decide(goal, listOf(first, second, third))

        val selected = assertIs<PlanDecisionResult.Selected>(result)
        assertEquals(first, selected.winner)
        assertEquals(
            listOf(PlanCandidateId("cand-2"), PlanCandidateId("cand-3")),
            selected.rejections.map { it.planCandidateId },
        )
        assertTrue(selected.rejections.all { it.reason == PlanRejectionReason.NOT_SELECTED })
    }

    @Test
    fun `selection is deterministic and repeatable -- calling decide twice with the same input yields the same result`() = runTest {
        val candidates = listOf(candidate("cand-1"), candidate("cand-2"))

        val first = decision.decide(goal, candidates)
        val second = decision.decide(goal, candidates)

        assertEquals(first, second)
    }

    @Test
    fun `candidate order determines the winner -- reordering the same candidates changes who wins`() = runTest {
        val a = candidate("cand-a")
        val b = candidate("cand-b")

        val aFirst = assertIs<PlanDecisionResult.Selected>(decision.decide(goal, listOf(a, b)))
        val bFirst = assertIs<PlanDecisionResult.Selected>(decision.decide(goal, listOf(b, a)))

        assertEquals(a, aFirst.winner)
        assertEquals(b, bFirst.winner)
    }

    @Test
    fun `RiskEstimate does not influence selection -- a HIGH-risk earlier candidate still wins over a LOW-risk later one`() = runTest {
        val highRiskFirst = candidate("cand-1", riskEstimate = RiskEstimate.HIGH)
        val lowRiskSecond = candidate("cand-2", riskEstimate = RiskEstimate.LOW)

        val result = assertIs<PlanDecisionResult.Selected>(decision.decide(goal, listOf(highRiskFirst, lowRiskSecond)))

        assertEquals(highRiskFirst, result.winner)
    }

    // --- invalid candidate rejection ---

    @Test
    fun `a candidate whose goal does not match the session Goal is rejected, not selected`() = runTest {
        val mismatched = candidate("cand-1", goal = "a different goal entirely")
        val valid = candidate("cand-2")

        val result = assertIs<PlanDecisionResult.Selected>(decision.decide(goal, listOf(mismatched, valid)))

        assertEquals(valid, result.winner)
        assertEquals(1, result.rejections.size)
        assertEquals(PlanCandidateId("cand-1"), result.rejections.single().planCandidateId)
        assertEquals(PlanRejectionReason.GOAL_MISMATCH, result.rejections.single().reason)
    }

    @Test
    fun `all candidates invalid yields NoViableCandidate with one rejection per candidate`() = runTest {
        val blank = candidate("cand-1", goal = "")
        val mismatched = candidate("cand-2", goal = "wrong goal")

        val result = assertIs<PlanDecisionResult.NoViableCandidate>(decision.decide(goal, listOf(blank, mismatched)))

        assertEquals(2, result.rejections.size)
        assertEquals(
            listOf(PlanCandidateId("cand-1"), PlanCandidateId("cand-2")),
            result.rejections.map { it.planCandidateId },
        )
        assertEquals(PlanRejectionReason.BLANK_GOAL, result.rejections[0].reason)
        assertEquals(PlanRejectionReason.GOAL_MISMATCH, result.rejections[1].reason)
    }

    // --- duplicate candidate handling ---

    @Test
    fun `a duplicate planCandidateId is rejected as a duplicate, and the first occurrence still wins`() = runTest {
        val original = candidate("cand-1")
        val duplicate = candidate("cand-1") // same id, would otherwise also be valid

        val result = assertIs<PlanDecisionResult.Selected>(decision.decide(goal, listOf(original, duplicate)))

        assertEquals(original, result.winner)
        assertEquals(1, result.rejections.size)
        assertEquals(PlanCandidateId("cand-1"), result.rejections.single().planCandidateId)
        assertEquals(PlanRejectionReason.DUPLICATE_CANDIDATE_ID, result.rejections.single().reason)
    }

    @Test
    fun `a duplicate id is rejected as duplicate even when the first occurrence was itself invalid`() = runTest {
        val invalidFirst = candidate("cand-1", goal = "")
        val wouldBeValidDuplicate = candidate("cand-1")

        val result = assertIs<PlanDecisionResult.NoViableCandidate>(decision.decide(goal, listOf(invalidFirst, wouldBeValidDuplicate)))

        assertEquals(2, result.rejections.size)
        assertEquals(PlanRejectionReason.BLANK_GOAL, result.rejections[0].reason)
        assertEquals(PlanRejectionReason.DUPLICATE_CANDIDATE_ID, result.rejections[1].reason)
    }

    @Test
    fun `three candidates sharing one id -- only the first is evaluated normally, the other two are duplicates`() = runTest {
        val first = candidate("cand-1")
        val secondDuplicate = candidate("cand-1")
        val thirdDuplicate = candidate("cand-1")

        val result = assertIs<PlanDecisionResult.Selected>(
            decision.decide(goal, listOf(first, secondDuplicate, thirdDuplicate)),
        )

        assertEquals(first, result.winner)
        assertEquals(2, result.rejections.size)
        assertTrue(result.rejections.all { it.reason == PlanRejectionReason.DUPLICATE_CANDIDATE_ID })
    }

    // --- boundary conditions ---

    @Test
    fun `an invalid candidate followed by a valid one -- the valid one wins`() = runTest {
        val invalid = candidate("cand-1", goal = "")
        val valid = candidate("cand-2")

        val result = assertIs<PlanDecisionResult.Selected>(decision.decide(goal, listOf(invalid, valid)))

        assertEquals(valid, result.winner)
        assertEquals(1, result.rejections.size)
    }

    @Test
    fun `rejection reasons are exact enum values, and detail is never blank or a bare numeric score`() = runTest {
        val blank = candidate("cand-1", goal = "")
        val mismatched = candidate("cand-2", goal = "wrong")
        val duplicateOriginal = candidate("cand-3")
        val duplicate = candidate("cand-3")
        val notSelected = candidate("cand-4")

        val result = assertIs<PlanDecisionResult.Selected>(
            decision.decide(goal, listOf(blank, mismatched, duplicateOriginal, duplicate, notSelected)),
        )

        assertEquals(duplicateOriginal, result.winner)
        assertEquals(4, result.rejections.size)
        assertEquals(
            listOf(
                PlanRejectionReason.BLANK_GOAL,
                PlanRejectionReason.GOAL_MISMATCH,
                PlanRejectionReason.DUPLICATE_CANDIDATE_ID,
                PlanRejectionReason.NOT_SELECTED,
            ),
            result.rejections.map { it.reason },
        )
        for (rejection in result.rejections) {
            assertTrue(rejection.detail.isNotBlank())
            assertTrue(rejection.detail.toDoubleOrNull() == null, "detail '${rejection.detail}' must not be a bare numeric score")
        }
    }
}
