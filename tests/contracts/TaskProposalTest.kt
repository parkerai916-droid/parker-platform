package parker.core.interfaces

import kotlin.reflect.full.functions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Sprint 1, Unit 1 acceptance test.
 *
 * Provenance: docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md, Section 6,
 * Unit 1 ("Task Proposal intake contract"): "A unit test constructs a
 * TaskProposal and a TaskProposalDisposition.Accepted and confirms both are
 * well-typed; no behaviour yet."
 *
 * The contract types under test (TaskProposal, TaskProposalDisposition,
 * ProposalDependency, TaskProposalIntake) were already added to
 * src/contracts/TaskProposal.kt during Sprint 1 blocker closure -- see
 * docs/implementation/SPRINT_1_BLOCKER_CLOSURE.md and
 * TaskManagerRuntimeSpecification.md Section 15 ("Task Proposal Intake").
 * This file adds the missing test coverage; it does not add new src/
 * behaviour. TaskProposalIntake itself has no implementation yet (that is
 * later Sprint 1 work, per the Vertical Slice Plan's Unit 6 -- Minimal Task
 * Manager Runtime), so only the interface's shape is checked here, not any
 * runtime behaviour.
 */
class TaskProposalTest {

    private fun proposal(
        proposedAssigneePrincipalId: PrincipalId? = null,
        goal: String = "Summarise today's unread email",
        correlationId: String = "corr-1",
    ) = TaskProposal(
        taskProposalId = TaskProposalId("proposal-1"),
        planningSessionId = PlanningSessionId("session-1"),
        initiatingPrincipalId = PrincipalId("user-1"),
        proposedOwnerPrincipalId = PrincipalId("user-1"),
        proposedAssigneePrincipalId = proposedAssigneePrincipalId,
        goal = goal,
        source = RequestOrigin.SCHEDULED_TASK,
        priority = RequestPriority.NORMAL,
        correlationId = correlationId,
    )

    // --- TaskProposal ---

    @Test
    fun `a Task Proposal can be constructed with only its required fields`() {
        val p = proposal()

        assertEquals(TaskProposalId("proposal-1"), p.taskProposalId)
        assertEquals(PlanningSessionId("session-1"), p.planningSessionId)
        assertEquals(PrincipalId("user-1"), p.initiatingPrincipalId)
        assertEquals(PrincipalId("user-1"), p.proposedOwnerPrincipalId)
        assertEquals(null, p.proposedAssigneePrincipalId)
        assertEquals(RequestOrigin.SCHEDULED_TASK, p.source)
        assertEquals(RequestPriority.NORMAL, p.priority)
        assertEquals(emptyList(), p.constraints)
        assertEquals(emptyList(), p.dependencies)
        assertEquals(emptySet(), p.requiredCapabilities)
        assertEquals(emptySet(), p.anticipatedPermissionActions)
        assertEquals(emptyList(), p.contextReferences)
        assertEquals(null, p.riskEstimate)
    }

    @Test
    fun `a Task Proposal may declare an assignee, capabilities, a dependency, and a risk estimate`() {
        val p = proposal(proposedAssigneePrincipalId = PrincipalId("agent-1")).copy(
            requiredCapabilities = setOf(PermissionAction.READ, PermissionAction.EXECUTE),
            anticipatedPermissionActions = setOf(PermissionAction.EXECUTE),
            riskEstimate = RiskEstimate.LOW,
            dependencies = listOf(ProposalDependency.OnExistingTask(TaskId("task-0"))),
        )

        assertEquals(PrincipalId("agent-1"), p.proposedAssigneePrincipalId)
        assertEquals(setOf(PermissionAction.READ, PermissionAction.EXECUTE), p.requiredCapabilities)
        assertEquals(RiskEstimate.LOW, p.riskEstimate)
        assertEquals(1, p.dependencies.size)
        assertIs<ProposalDependency.OnExistingTask>(p.dependencies.single())
    }

    @Test
    fun `two Task Proposals with identical fields are equal`() {
        assertEquals(proposal(), proposal())
    }

    @Test
    fun `Task Proposals with different goals are not equal`() {
        assertNotEquals(proposal(goal = "Goal A"), proposal(goal = "Goal B"))
    }

    @Test
    fun `a blank goal is rejected`() {
        assertFailsWith<IllegalArgumentException> { proposal(goal = "") }
        assertFailsWith<IllegalArgumentException> { proposal(goal = "   ") }
    }

    @Test
    fun `a blank correlationId is rejected`() {
        assertFailsWith<IllegalArgumentException> { proposal(correlationId = "") }
    }

    // --- Identifiers (PlanningSessionId, TaskProposalId, TaskId) ---

    @Test
    fun `PlanningSessionId, TaskProposalId, and TaskId reject blank values`() {
        assertFailsWith<IllegalArgumentException> { PlanningSessionId("") }
        assertFailsWith<IllegalArgumentException> { TaskProposalId("  ") }
        assertFailsWith<IllegalArgumentException> { TaskId("") }
    }

    @Test
    fun `identifiers with equal values are equal, and with different values are not`() {
        assertEquals(PlanningSessionId("s-1"), PlanningSessionId("s-1"))
        assertEquals(TaskProposalId("p-1"), TaskProposalId("p-1"))
        assertEquals(TaskId("t-1"), TaskId("t-1"))
        assertNotEquals(TaskId("t-1"), TaskId("t-2"))
    }

    // --- ProposalDependency ---

    @Test
    fun `a dependency may reference an already-existing Task`() {
        val dependency = ProposalDependency.OnExistingTask(TaskId("task-0"))

        assertEquals(TaskId("task-0"), dependency.taskId)
    }

    @Test
    fun `a dependency may reference a sibling Task Proposal not yet resolved`() {
        val dependency = ProposalDependency.OnProposal(TaskProposalId("proposal-0"))

        assertEquals(TaskProposalId("proposal-0"), dependency.taskProposalId)
    }

    // --- TaskProposalDisposition ---

    @Test
    fun `Accepted names exactly one resulting Task`() {
        val disposition: TaskProposalDisposition =
            TaskProposalDisposition.Accepted(TaskProposalId("proposal-1"), TaskId("task-1"))

        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)
        assertEquals(TaskProposalId("proposal-1"), accepted.taskProposalId)
        assertEquals(TaskId("task-1"), accepted.taskId)
    }

    @Test
    fun `Deferred requires a non-blank reason`() {
        val disposition = TaskProposalDisposition.Deferred(
            TaskProposalId("proposal-1"),
            "waiting on a revised constraint",
        )

        assertEquals("waiting on a revised constraint", disposition.reason)
        assertFailsWith<IllegalArgumentException> {
            TaskProposalDisposition.Deferred(TaskProposalId("proposal-1"), "")
        }
    }

    @Test
    fun `Rejected requires a non-blank reason`() {
        val disposition = TaskProposalDisposition.Rejected(
            TaskProposalId("proposal-1"),
            "duplicate of an existing Task",
        )

        assertEquals("duplicate of an existing Task", disposition.reason)
        assertFailsWith<IllegalArgumentException> {
            TaskProposalDisposition.Rejected(TaskProposalId("proposal-1"), "   ")
        }
    }

    @Test
    fun `Split requires at least two resulting Tasks`() {
        val disposition = TaskProposalDisposition.Split(
            TaskProposalId("proposal-1"),
            listOf(TaskId("task-1"), TaskId("task-2")),
        )

        assertEquals(2, disposition.taskIds.size)
        assertFailsWith<IllegalArgumentException> {
            TaskProposalDisposition.Split(TaskProposalId("proposal-1"), listOf(TaskId("task-1")))
        }
    }

    @Test
    fun `Merged requires at least one sibling proposal it was merged with`() {
        val disposition = TaskProposalDisposition.Merged(
            TaskProposalId("proposal-1"),
            TaskId("task-1"),
            listOf(TaskProposalId("proposal-2")),
        )

        assertEquals(listOf(TaskProposalId("proposal-2")), disposition.mergedWithProposalIds)
        assertFailsWith<IllegalArgumentException> {
            TaskProposalDisposition.Merged(TaskProposalId("proposal-1"), TaskId("task-1"), emptyList())
        }
    }

    @Test
    fun `every disposition names the proposal it resolves, via the shared sealed base`() {
        val dispositions: List<TaskProposalDisposition> = listOf(
            TaskProposalDisposition.Accepted(TaskProposalId("p"), TaskId("t")),
            TaskProposalDisposition.Deferred(TaskProposalId("p"), "reason"),
            TaskProposalDisposition.Rejected(TaskProposalId("p"), "reason"),
            TaskProposalDisposition.Split(TaskProposalId("p"), listOf(TaskId("t1"), TaskId("t2"))),
            TaskProposalDisposition.Merged(TaskProposalId("p"), TaskId("t"), listOf(TaskProposalId("p2"))),
        )

        assertTrue(dispositions.all { it.taskProposalId == TaskProposalId("p") })
    }

    // --- TaskProposalIntake (interface shape only -- no implementation exists yet) ---

    @Test
    fun `TaskProposalIntake declares submitProposal as a suspend function returning TaskProposalDisposition`() {
        val submitProposal = TaskProposalIntake::class.functions.single { it.name == "submitProposal" }

        assertTrue(submitProposal.isSuspend)
        assertEquals(TaskProposalDisposition::class, submitProposal.returnType.classifier)
    }
}
