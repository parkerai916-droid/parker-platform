package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EventType
import parker.core.interfaces.PlanCandidate
import parker.core.interfaces.PlanCandidateId
import parker.core.interfaces.PlannerRuntime
import parker.core.interfaces.PlannerSessionLifecycleTransitions
import parker.core.interfaces.PlannerSessionStatus
import parker.core.interfaces.PlanningRequest
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.PlanningSessionResult
import parker.core.interfaces.PlanRejectionReason
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.TaskId
import parker.core.interfaces.TaskProposalDisposition
import parker.core.interfaces.TaskProposalId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sprint 3, Track D, Unit D2 (Alignment Pass) acceptance test
 * (`docs/architecture/PLANNER_RUNTIME_PROGRESSION_DESIGN.md` Section 11,
 * Testing requirements): "zero candidates; one candidate; multiple
 * candidates; deterministic selection; invalid candidate rejection;
 * duplicate candidate handling; event publication; lifecycle transitions;
 * boundary conditions." [DefaultPlanDecisionTest] covers the pure
 * selection algorithm in isolation; this file covers
 * [InMemoryPlannerRuntime]'s own progression (identity resolution, Plan
 * Decision consultation, Task Proposal construction, submission, event
 * publication, and lifecycle bookkeeping) end to end, using the real
 * [InMemoryIdentityService], [InMemoryEventBus], and
 * [InMemoryTaskManagerRuntime] wherever their actual (accept-only)
 * behaviour is sufficient, and [FakeTaskProposalIntake] only where a
 * disposition [InMemoryTaskManagerRuntime] cannot currently produce
 * (`REJECTED` for a resolvable owner; `Deferred`/`Split`/`Merged`) is
 * needed to exercise [InMemoryPlannerRuntime]'s own classification logic.
 *
 * Aligned to `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md`
 * (Unit D1A): [PlanningRequest] no longer carries `candidates` (now a
 * separate argument to [PlannerRuntime.plan]), and every assertion against
 * a rejection's reason now checks the exact [PlanRejectionReason] value
 * rather than substring-matching prose.
 */
class InMemoryPlannerRuntimeTest {

    private val goal = "read today's calendar"

    private fun principal(id: String = "user-1") = Principal(
        principalId = PrincipalId(id),
        principalType = PrincipalType.USER,
        displayName = "Test Principal",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun candidate(id: String, goal: String = this.goal) = PlanCandidate(
        planCandidateId = PlanCandidateId(id),
        goal = goal,
    )

    private fun request(
        planningSessionId: String = "session-1",
        initiatingPrincipalId: String = "user-1",
        goal: String = this.goal,
        correlationId: String = "corr-1",
    ) = PlanningRequest(
        planningSessionId = PlanningSessionId(planningSessionId),
        initiatingPrincipalId = PrincipalId(initiatingPrincipalId),
        goal = goal,
        correlationId = correlationId,
    )

    /** The single-candidate set most tests need; a few build their own list to exercise multiple/zero candidates. */
    private fun oneCandidate(goal: String = this.goal) = listOf(candidate("cand-1", goal))

    private val plannerEventTypes = setOf(
        EventType("planner.session_created"),
        EventType("planner.context_requested"),
        EventType("planner.analysis_started"),
        EventType("planner.candidate_generated"),
        EventType("planner.candidate_rejected"),
        EventType("planner.proposal_created"),
        EventType("planner.proposal_submitted"),
        EventType("planner.session_completed"),
        EventType("planner.session_failed"),
    )

    // --- one candidate: full happy path against the real Task Manager Runtime ---

    @Test
    fun `one valid candidate results in a Completed Planning Session and exactly one Task`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)

        val result = planner.plan(request(), oneCandidate())

        val completed = assertIs<PlanningSessionResult.Completed>(result)
        assertEquals(TaskProposalId("session-1-proposal-1"), completed.taskProposalId)
        assertIs<TaskProposalDisposition.Accepted>(completed.disposition)
        assertTrue(completed.rejections.isEmpty())
        assertEquals(1, taskManager.listTasks().size)
        assertEquals(goal, taskManager.listTasks().single().goal)
        assertEquals(PlannerSessionStatus.COMPLETED, planner.getSessionStatus(PlanningSessionId("session-1")))
    }

    // --- zero candidates ---

    @Test
    fun `zero candidates results in a Failed Planning Session, and no Task is created`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)

        val result = planner.plan(request(), emptyList())

        val failed = assertIs<PlanningSessionResult.Failed>(result)
        assertTrue(failed.rejections.isEmpty())
        assertTrue(failed.reason.contains("no Plan Candidates", ignoreCase = true))
        assertTrue(taskManager.listTasks().isEmpty())
        assertEquals(PlannerSessionStatus.FAILED, planner.getSessionStatus(PlanningSessionId("session-1")))
    }

    // --- multiple candidates / deterministic selection, end to end ---

    @Test
    fun `multiple candidates -- the first valid one in generation order is submitted, the rest are recorded as rejections`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)

        val result = planner.plan(
            request(),
            listOf(candidate("cand-1"), candidate("cand-2"), candidate("cand-3")),
        )

        val completed = assertIs<PlanningSessionResult.Completed>(result)
        assertEquals(2, completed.rejections.size)
        assertEquals(
            listOf(PlanCandidateId("cand-2"), PlanCandidateId("cand-3")),
            completed.rejections.map { it.planCandidateId },
        )
        assertEquals(1, taskManager.listTasks().size)
    }

    @Test
    fun `re-running the same PlanningRequest candidates twice selects the same winner both times`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal("user-1"))
        identity.register(principal("user-2"))
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)
        val candidates = listOf(candidate("cand-1"), candidate("cand-2"))

        val first = assertIs<PlanningSessionResult.Completed>(
            planner.plan(request(planningSessionId = "session-1", initiatingPrincipalId = "user-1", correlationId = "corr-1"), candidates),
        )
        val second = assertIs<PlanningSessionResult.Completed>(
            planner.plan(request(planningSessionId = "session-2", initiatingPrincipalId = "user-2", correlationId = "corr-2"), candidates),
        )

        assertEquals(first.rejections.map { it.planCandidateId }, second.rejections.map { it.planCandidateId })
    }

    // --- invalid candidate rejection, end to end ---

    @Test
    fun `a candidate whose goal does not match the session Goal is rejected, and the valid one still succeeds`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)

        val result = planner.plan(
            request(),
            listOf(candidate("cand-1", goal = "an unrelated goal"), candidate("cand-2")),
        )

        val completed = assertIs<PlanningSessionResult.Completed>(result)
        assertEquals(1, completed.rejections.size)
        assertEquals(PlanCandidateId("cand-1"), completed.rejections.single().planCandidateId)
        assertEquals(PlanRejectionReason.GOAL_MISMATCH, completed.rejections.single().reason)
        assertEquals(goal, taskManager.listTasks().single().goal)
    }

    // --- duplicate candidate handling, end to end ---

    @Test
    fun `duplicate candidate ids -- the first occurrence is submitted, the duplicate is rejected`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)

        val result = planner.plan(
            request(),
            listOf(candidate("cand-1"), candidate("cand-1")),
        )

        val completed = assertIs<PlanningSessionResult.Completed>(result)
        assertEquals(1, completed.rejections.size)
        assertEquals(PlanRejectionReason.DUPLICATE_CANDIDATE_ID, completed.rejections.single().reason)
    }

    // --- event publication ---

    @Test
    fun `a successful single-candidate Planning Session publishes the expected planner event sequence`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)
        val collector = EventCollector(eventBus, eventTypes = plannerEventTypes)

        planner.plan(request(), oneCandidate())

        assertEquals(
            listOf(
                "planner.session_created",
                "planner.context_requested",
                "planner.analysis_started",
                "planner.candidate_generated",
                "planner.proposal_created",
                "planner.proposal_submitted",
                "planner.session_completed",
            ),
            collector.eventsFor("corr-1").map { it.eventType.value },
        )
    }

    @Test
    fun `a zero-candidate Planning Session publishes analysis_started then session_failed, never a proposal event`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)
        val collector = EventCollector(eventBus, eventTypes = plannerEventTypes)

        planner.plan(request(), emptyList())

        val types = collector.eventsFor("corr-1").map { it.eventType.value }
        assertEquals(
            listOf(
                "planner.session_created",
                "planner.context_requested",
                "planner.analysis_started",
                "planner.session_failed",
            ),
            types,
        )
        assertTrue("planner.proposal_created" !in types)
        assertTrue("planner.proposal_submitted" !in types)
    }

    @Test
    fun `a rejected candidate publishes a planner-candidate-rejected event carrying its id, reason, and detail`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)
        val collector = EventCollector(eventBus, eventTypes = plannerEventTypes)

        planner.plan(request(), listOf(candidate("cand-1"), candidate("cand-2")))

        val rejectedEvent = collector.eventsFor("corr-1").single { it.eventType.value == "planner.candidate_rejected" }
        assertEquals("cand-2", rejectedEvent.payload["planCandidateId"])
        assertEquals(PlanRejectionReason.NOT_SELECTED.name, rejectedEvent.payload["reason"])
        assertTrue(rejectedEvent.payload["detail"]?.isNotBlank() == true)
    }

    // --- lifecycle transitions ---

    @Test
    fun `PlannerSessionLifecycleTransitions models exactly the edges this runtime drives, no more`() {
        assertTrue(PlannerSessionLifecycleTransitions.isValidTransition(PlannerSessionStatus.CREATED, PlannerSessionStatus.CONTEXT_GATHERING))
        assertTrue(PlannerSessionLifecycleTransitions.isValidTransition(PlannerSessionStatus.CONTEXT_GATHERING, PlannerSessionStatus.ANALYSING))
        assertTrue(PlannerSessionLifecycleTransitions.isValidTransition(PlannerSessionStatus.ANALYSING, PlannerSessionStatus.PROPOSING))
        assertTrue(PlannerSessionLifecycleTransitions.isValidTransition(PlannerSessionStatus.ANALYSING, PlannerSessionStatus.FAILED))
        assertTrue(PlannerSessionLifecycleTransitions.isValidTransition(PlannerSessionStatus.PROPOSING, PlannerSessionStatus.SUBMITTED))
        assertTrue(PlannerSessionLifecycleTransitions.isValidTransition(PlannerSessionStatus.SUBMITTED, PlannerSessionStatus.COMPLETED))
        assertTrue(PlannerSessionLifecycleTransitions.isValidTransition(PlannerSessionStatus.SUBMITTED, PlannerSessionStatus.REJECTED))

        // No CREATED -> FAILED edge: PlannerRuntimeSpecification.md Section 5 has none (see
        // PlannerSessionStatus's own KDoc) -- an unresolvable initiating Principal is handled
        // as a pre-CREATED guard, never a lifecycle transition.
        assertTrue(!PlannerSessionLifecycleTransitions.isValidTransition(PlannerSessionStatus.CREATED, PlannerSessionStatus.FAILED))
        // No direct CREATED -> ANALYSING/PROPOSING/SUBMITTED shortcut.
        assertTrue(!PlannerSessionLifecycleTransitions.isValidTransition(PlannerSessionStatus.CREATED, PlannerSessionStatus.ANALYSING))
    }

    @Test
    fun `getSessionStatus reflects the terminal status after plan returns`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)

        assertNull(planner.getSessionStatus(PlanningSessionId("session-1")))

        planner.plan(request(), oneCandidate())

        assertEquals(PlannerSessionStatus.COMPLETED, planner.getSessionStatus(PlanningSessionId("session-1")))
    }

    // --- boundary conditions ---

    @Test
    fun `an unresolvable initiating Principal results in a Failed Planning Session with no session record`() = runTest {
        val identity = InMemoryIdentityService() // no Principal registered
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)

        val result = planner.plan(request(initiatingPrincipalId = "ghost-user"), oneCandidate())

        val failed = assertIs<PlanningSessionResult.Failed>(result)
        assertTrue(failed.reason.contains("does not resolve", ignoreCase = true))
        assertNull(planner.getSessionStatus(PlanningSessionId("session-1")))
        assertTrue(taskManager.listTasks().isEmpty())
    }

    @Test
    fun `an unresolvable initiating Principal publishes no planner events at all`() = runTest {
        val identity = InMemoryIdentityService()
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)
        val collector = EventCollector(eventBus, eventTypes = plannerEventTypes)

        planner.plan(request(initiatingPrincipalId = "ghost-user"), oneCandidate())

        assertTrue(collector.eventsFor("corr-1").isEmpty())
    }

    @Test
    fun `resubmitting the same planningSessionId is rejected as caller misuse`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner = InMemoryPlannerRuntime(identity, eventBus, taskManager)

        planner.plan(request(), oneCandidate())

        assertFailsWith<IllegalStateException> {
            planner.plan(request(), oneCandidate())
        }
    }

    @Test
    fun `a Task Manager rejection results in a Rejected Planning Session, using a fake disposition`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val fakeIntake = FakeTaskProposalIntake { proposal ->
            TaskProposalDisposition.Rejected(proposal.taskProposalId, "business-reason rejection, test fixture")
        }
        val planner = InMemoryPlannerRuntime(identity, eventBus, fakeIntake)

        val result = planner.plan(request(), oneCandidate())

        val rejected = assertIs<PlanningSessionResult.Rejected>(result)
        assertEquals(TaskProposalId("session-1-proposal-1"), rejected.taskProposalId)
        assertEquals(PlannerSessionStatus.REJECTED, planner.getSessionStatus(PlanningSessionId("session-1")))
        assertEquals(1, fakeIntake.submitCallCount)
    }

    @Test
    fun `a Deferred disposition still results in a Completed Planning Session, per Section 5's own COMPLETED rule`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val fakeIntake = FakeTaskProposalIntake { proposal ->
            TaskProposalDisposition.Deferred(proposal.taskProposalId, "deferred pending a dependency, test fixture")
        }
        val planner = InMemoryPlannerRuntime(identity, eventBus, fakeIntake)

        val result = planner.plan(request(), oneCandidate())

        val completed = assertIs<PlanningSessionResult.Completed>(result)
        assertIs<TaskProposalDisposition.Deferred>(completed.disposition)
        assertEquals(PlannerSessionStatus.COMPLETED, planner.getSessionStatus(PlanningSessionId("session-1")))
    }

    @Test
    fun `the constructed TaskProposal carries the winning candidate's goal, and exactly one proposal is submitted`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val fakeIntake = FakeTaskProposalIntake { proposal ->
            TaskProposalDisposition.Accepted(proposal.taskProposalId, TaskId("task-1"))
        }
        val planner = InMemoryPlannerRuntime(identity, eventBus, fakeIntake)

        planner.plan(request(), listOf(candidate("cand-1"), candidate("cand-2")))

        assertEquals(1, fakeIntake.submitCallCount)
        val submitted = fakeIntake.lastProposal
        assertEquals(goal, submitted?.goal)
        assertEquals(TaskProposalId("session-1-proposal-1"), submitted?.taskProposalId)
    }

    // --- PlannerRuntime interface conformance (Alignment Pass, PLANNER_RUNTIME_CONTRACT_DESIGN.md §10/§11) ---

    @Test
    fun `InMemoryPlannerRuntime is usable entirely through the PlannerRuntime interface`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val taskManager = InMemoryTaskManagerRuntime(identity, eventBus)
        val planner: PlannerRuntime = InMemoryPlannerRuntime(identity, eventBus, taskManager)

        val result = planner.plan(request(), oneCandidate())

        assertIs<PlanningSessionResult.Completed>(result)
    }
}
