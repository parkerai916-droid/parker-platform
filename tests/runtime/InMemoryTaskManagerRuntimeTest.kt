package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AgentRunCommandType
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.TaskId
import parker.core.interfaces.TaskProposal
import parker.core.interfaces.TaskProposalDisposition
import parker.core.interfaces.TaskProposalId
import parker.core.interfaces.TaskStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sprint 1, Unit 6 acceptance test
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6, Unit 6; §7's
 * "Task Manager accepts proposal and creates Task" / "Task Manager
 * requests Agent Run" rows): "submitting a well-formed `TaskProposal`
 * results in exactly one Task in `Queued` state and exactly one
 * `AgentRunRequest` constructed" (realised as `AgentRunCommand`, per
 * `AgentRunCommand.kt`'s own note that it closes the "Agent Run Request
 * has no named, shaped object" gap), "with `ownerPrincipalId` resolved
 * through the Identity Service (not a Task-Manager-local store)."
 *
 * Scope note: this file proves Unit 6 (accept-only intake for a
 * resolvable owner, `Created -> Queued`, one constructed
 * `AgentRunCommand`). It does not call `AgentRunCommandChannel.submit`
 * (no implementation exists -- Unit 7), and it does not exercise any Task
 * Status beyond `Created`/`Queued` -- see `TaskLifecycleTransitionsTest.kt`
 * for the full 9-state lifecycle's own coverage, independent of this
 * runtime.
 */
class InMemoryTaskManagerRuntimeTest {

    private fun principal(id: String = "user-1") = Principal(
        principalId = PrincipalId(id),
        principalType = PrincipalType.USER,
        displayName = "Test Principal",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun proposal(
        taskProposalId: String = "proposal-1",
        ownerPrincipalId: String = "user-1",
        goal: String = "read today's calendar",
        correlationId: String = "corr-1",
    ) = TaskProposal(
        taskProposalId = TaskProposalId(taskProposalId),
        planningSessionId = PlanningSessionId("session-1"),
        initiatingPrincipalId = PrincipalId(ownerPrincipalId),
        proposedOwnerPrincipalId = PrincipalId(ownerPrincipalId),
        goal = goal,
        source = RequestOrigin.TEXT,
        priority = RequestPriority.NORMAL,
        correlationId = correlationId,
    )

    // --- accept path ---

    @Test
    fun `submitting a well-formed proposal with a resolvable owner results in exactly one Task in Queued state`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity)

        val disposition = runtime.submitProposal(proposal())

        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)
        val task = runtime.getTask(accepted.taskId)
        assertNotNull(task)
        assertEquals(TaskStatus.QUEUED, task.status)
        assertEquals(PrincipalId("user-1"), task.ownerPrincipalId)
        assertEquals("read today's calendar", task.goal)
        assertEquals("corr-1", task.correlationId)
        assertEquals(TaskProposalId("proposal-1"), task.originatingTaskProposalId)

        assertEquals(listOf(task), runtime.listTasks())
    }

    @Test
    fun `ownerPrincipalId is resolved through the Identity Service, not trusted as-is`() = runTest {
        val identity = InMemoryIdentityService()
        val registered = principal("user-1")
        identity.register(registered)
        val runtime = InMemoryTaskManagerRuntime(identity)

        val disposition = runtime.submitProposal(proposal(ownerPrincipalId = "user-1"))

        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)
        val task = runtime.getTask(accepted.taskId)
        assertEquals(registered.principalId, task?.ownerPrincipalId)
    }

    // --- Agent Run Command construction ---

    @Test
    fun `accepting a proposal constructs exactly one AgentRunCommand referencing the created Task`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity)

        val disposition = runtime.submitProposal(proposal())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)

        val commands = runtime.agentRunCommandsFor(accepted.taskId)
        assertEquals(1, commands.size)
        val command = commands.single()
        assertEquals(AgentRunCommandType.START, command.commandType)
        assertEquals(accepted.taskId, command.taskId)
        assertEquals(null, command.agentRunId)
        assertEquals("read today's calendar", command.goalDescription)
        assertEquals("corr-1", command.correlationId)
        assertEquals(PrincipalId("user-1"), command.requestingPrincipalId)
    }

    @Test
    fun `requiredCapabilities on the proposal carry forward to targetAgentCapability on the command`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity)

        val withCapabilities = proposal().copy(requiredCapabilities = setOf(PermissionAction.READ))
        val disposition = runtime.submitProposal(withCapabilities)
        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)

        val command = runtime.agentRunCommandsFor(accepted.taskId).single()
        assertEquals(setOf(PermissionAction.READ), command.targetAgentCapability)
    }

    // --- unresolvable owner ---

    @Test
    fun `an unresolvable owner is Rejected, and no Task or AgentRunCommand is created`() = runTest {
        val identity = InMemoryIdentityService() // no Principal registered
        val runtime = InMemoryTaskManagerRuntime(identity)

        val disposition = runtime.submitProposal(proposal(ownerPrincipalId = "ghost-user"))

        val rejected = assertIs<TaskProposalDisposition.Rejected>(disposition)
        assertEquals(TaskProposalId("proposal-1"), rejected.taskProposalId)
        assertTrue(rejected.reason.isNotBlank())
        assertTrue(runtime.listTasks().isEmpty())
    }

    // --- unknown Task lookup ---

    @Test
    fun `getTask returns null for an unknown taskId, not an exception`() = runTest {
        val runtime = InMemoryTaskManagerRuntime(InMemoryIdentityService())

        assertNull(runtime.getTask(TaskId("task-for-nonexistent")))
        assertTrue(runtime.agentRunCommandsFor(TaskId("task-for-nonexistent")).isEmpty())
    }

    // --- duplicate submission ---

    @Test
    fun `resubmitting the same taskProposalId is rejected as caller misuse`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity)

        runtime.submitProposal(proposal())

        assertFailsWith<IllegalStateException> {
            runtime.submitProposal(proposal())
        }
    }

    // --- isolation between independent proposals (no regression / no cross-contamination) ---

    @Test
    fun `two independent proposals produce two independent Tasks and command lists`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal("user-1"))
        identity.register(principal("user-2"))
        val runtime = InMemoryTaskManagerRuntime(identity)

        val first = assertIs<TaskProposalDisposition.Accepted>(
            runtime.submitProposal(proposal(taskProposalId = "proposal-1", ownerPrincipalId = "user-1", correlationId = "corr-1")),
        )
        val second = assertIs<TaskProposalDisposition.Accepted>(
            runtime.submitProposal(proposal(taskProposalId = "proposal-2", ownerPrincipalId = "user-2", correlationId = "corr-2")),
        )

        assertTrue(first.taskId != second.taskId)
        assertEquals(2, runtime.listTasks().size)
        assertEquals(PrincipalId("user-1"), runtime.getTask(first.taskId)?.ownerPrincipalId)
        assertEquals(PrincipalId("user-2"), runtime.getTask(second.taskId)?.ownerPrincipalId)
        assertEquals(1, runtime.agentRunCommandsFor(first.taskId).size)
        assertEquals(1, runtime.agentRunCommandsFor(second.taskId).size)
    }
}
