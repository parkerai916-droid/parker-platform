package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.RiskEstimate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Sprint 1, Unit 5 acceptance test
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6, Unit 5;
 * restated in §7's Test Plan, "Planner produces Task Proposal"): "given the
 * fixed Goal, produces exactly one well-formed TaskProposal, and the
 * harness transitions
 * CREATED -> CONTEXT_GATHERING -> ANALYSING -> PROPOSING -> SUBMITTED."
 *
 * Scope note: this file proves Unit 5 (the harness itself: its fixed
 * lifecycle path, and the single Plan Candidate / Task Proposal it
 * produces). It does not construct a Task Manager Runtime, call
 * `TaskProposalIntake.submitProposal`, or exercise `WAITING_FOR_INPUT`,
 * `COMPLETED`, `REJECTED`, `CANCELLED`, or `FAILED` -- those are out of
 * this unit's scope per `DeterministicPlannerHarness.kt`'s own top-level
 * KDoc, and Unit 6 onward.
 *
 * Sprint 1, Unit 9: `run()` is now `suspend` (it publishes `planner.*`
 * events via [EventBus]), so every test that calls it runs inside
 * `runTest {}`; each constructs its own [InMemoryEventBus], mirroring the
 * per-test-fresh-dependency convention already used throughout
 * `tests/runtime/`. This file does not itself assert on published events
 * -- that is `RuntimeLifecycleEventPublicationTest.kt`'s job; here, the
 * bus is present only so the harness has somewhere to publish to.
 */
class DeterministicPlannerHarnessTest {

    private val planningSessionId = PlanningSessionId("session-1")
    private val principalId = PrincipalId("user-1")
    private val goal = "read today's calendar"
    private val correlationId = "corr-1"

    // --- initial state, before run() ---

    @Test
    fun `a fresh harness starts at CREATED with no candidates or proposals`() {
        val harness = DeterministicPlannerHarness(InMemoryEventBus())

        assertEquals(listOf(PlanningSessionLifecycleState.CREATED), harness.stateHistory)
        assertEquals(PlanningSessionLifecycleState.CREATED, harness.currentState)
        assertTrue(harness.planCandidates.isEmpty())
        assertTrue(harness.taskProposals.isEmpty())
    }

    // --- the fixed lifecycle path ---

    @Test
    fun `run transitions exactly CREATED to CONTEXT_GATHERING to ANALYSING to PROPOSING to SUBMITTED`() = runTest {
        val harness = DeterministicPlannerHarness(InMemoryEventBus())

        harness.run(planningSessionId, principalId, goal, correlationId)

        assertEquals(
            listOf(
                PlanningSessionLifecycleState.CREATED,
                PlanningSessionLifecycleState.CONTEXT_GATHERING,
                PlanningSessionLifecycleState.ANALYSING,
                PlanningSessionLifecycleState.PROPOSING,
                PlanningSessionLifecycleState.SUBMITTED,
            ),
            harness.stateHistory,
        )
        assertEquals(PlanningSessionLifecycleState.SUBMITTED, harness.currentState)
    }

    @Test
    fun `run may not be called a second time on the same instance`() = runTest {
        val harness = DeterministicPlannerHarness(InMemoryEventBus())
        harness.run(planningSessionId, principalId, goal, correlationId)

        assertFailsWith<IllegalStateException> {
            harness.run(planningSessionId, principalId, goal, correlationId)
        }
    }

    // --- exactly one Plan Candidate ---

    @Test
    fun `run produces exactly one well-formed PlanCandidate for the fixed Goal`() = runTest {
        val harness = DeterministicPlannerHarness(InMemoryEventBus())

        harness.run(planningSessionId, principalId, goal, correlationId)

        assertEquals(1, harness.planCandidates.size)
        val candidate = harness.planCandidates.single()
        assertEquals(goal, candidate.goal)
        assertTrue(candidate.planCandidateId.isNotBlank())
    }

    // --- exactly one well-formed Task Proposal ---

    @Test
    fun `run produces exactly one well-formed TaskProposal`() = runTest {
        val harness = DeterministicPlannerHarness(InMemoryEventBus())

        harness.run(
            planningSessionId = planningSessionId,
            initiatingPrincipalId = principalId,
            goal = goal,
            correlationId = correlationId,
            source = RequestOrigin.VOICE,
            priority = RequestPriority.HIGH,
        )

        assertEquals(1, harness.taskProposals.size)
        val proposal = harness.taskProposals.single()

        assertEquals(planningSessionId, proposal.planningSessionId)
        assertEquals(principalId, proposal.initiatingPrincipalId)
        assertEquals(principalId, proposal.proposedOwnerPrincipalId)
        assertEquals(goal, proposal.goal)
        assertEquals(RequestOrigin.VOICE, proposal.source)
        assertEquals(RequestPriority.HIGH, proposal.priority)
        assertEquals(correlationId, proposal.correlationId)
        assertEquals(RiskEstimate.LOW, proposal.riskEstimate)
        assertTrue(proposal.rationale.isNotBlank())
    }

    @Test
    fun `run uses sensible defaults for source and priority when not supplied`() = runTest {
        val harness = DeterministicPlannerHarness(InMemoryEventBus())

        harness.run(planningSessionId, principalId, goal, correlationId)

        val proposal = harness.taskProposals.single()
        assertEquals(RequestOrigin.TEXT, proposal.source)
        assertEquals(RequestPriority.NORMAL, proposal.priority)
    }

    // --- determinism ---

    @Test
    fun `two harnesses given identical arguments produce identical PlanCandidate and TaskProposal`() = runTest {
        val first = DeterministicPlannerHarness(InMemoryEventBus())
        val second = DeterministicPlannerHarness(InMemoryEventBus())

        first.run(planningSessionId, principalId, goal, correlationId)
        second.run(planningSessionId, principalId, goal, correlationId)

        assertEquals(first.planCandidates, second.planCandidates)
        assertEquals(first.taskProposals, second.taskProposals)
    }

    // --- the transition object itself ---

    @Test
    fun `PlanningSessionLifecycleTransitions rejects an edge outside the fixed 5-state path`() {
        assertFailsWith<IllegalArgumentException> {
            PlanningSessionLifecycleTransitions.requireValidTransition(
                PlanningSessionLifecycleState.CREATED,
                PlanningSessionLifecycleState.SUBMITTED,
            )
        }
    }

    @Test
    fun `SUBMITTED is terminal for this fixed harness`() {
        assertTrue(
            PlanningSessionLifecycleState.entries.none {
                PlanningSessionLifecycleTransitions.isValidTransition(PlanningSessionLifecycleState.SUBMITTED, it)
            },
        )
    }
}
