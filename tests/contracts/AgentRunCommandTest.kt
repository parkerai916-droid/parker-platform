package parker.core.interfaces

import kotlin.reflect.full.functions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Sprint 1, Unit 2 acceptance test.
 *
 * Provenance: docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md, Section 6,
 * Unit 2 ("Agent Run Request contract"), following the same "well-typed, no
 * behaviour yet" framing as Unit 1's TaskProposalTest.
 *
 * The contract types under test (AgentRunId, AgentRunCommandType,
 * AgentRunCommand, AgentRunCommandResult, AgentRunCommandChannel) were
 * already added to src/contracts/AgentRunCommand.kt during Sprint 1 blocker
 * closure -- see docs/implementation/SPRINT_1_BLOCKER_CLOSURE.md,
 * TaskManagerRuntimeSpecification.md Section 16 ("Agent Run Command
 * Channel"), and AgentRuntimeSpecification.md Section 5. This file adds the
 * missing test coverage; it does not add new src/ behaviour.
 * AgentRunCommandChannel itself has no implementation yet (that is later
 * Sprint 1 work, Unit 7 -- Minimal Agent Runtime), so only the interface's
 * shape is checked here, not any runtime behaviour.
 */
class AgentRunCommandTest {

    private fun startCommand(
        goalDescription: String = "Draft a reply to the unread email",
        correlationId: String = "corr-2",
    ) = AgentRunCommand(
        commandType = AgentRunCommandType.START,
        taskId = TaskId("task-1"),
        requestingPrincipalId = PrincipalId("user-1"),
        goalDescription = goalDescription,
        correlationId = correlationId,
    )

    private fun cancelCommand(
        cancellationReason: String? = "Task Manager Task was cancelled by its owner",
    ) = AgentRunCommand(
        commandType = AgentRunCommandType.CANCEL,
        taskId = TaskId("task-1"),
        agentRunId = AgentRunId("run-1"),
        requestingPrincipalId = PrincipalId("user-1"),
        goalDescription = "Cancel in-flight reply drafting",
        correlationId = "corr-3",
        cancellationReason = cancellationReason,
    )

    // --- AgentRunId ---

    @Test
    fun `AgentRunId rejects a blank value and equal values are equal`() {
        assertFailsWith<IllegalArgumentException> { AgentRunId("") }
        assertFailsWith<IllegalArgumentException> { AgentRunId("   ") }
        assertEquals(AgentRunId("run-1"), AgentRunId("run-1"))
        assertNotEquals(AgentRunId("run-1"), AgentRunId("run-2"))
    }

    // --- AgentRunCommand: START ---

    @Test
    fun `a START command can be constructed with only its required fields, and carries no agentRunId`() {
        val c = startCommand()

        assertEquals(AgentRunCommandType.START, c.commandType)
        assertEquals(TaskId("task-1"), c.taskId)
        assertEquals(null, c.agentRunId)
        assertEquals(PrincipalId("user-1"), c.requestingPrincipalId)
        assertEquals(emptySet(), c.targetAgentCapability)
        assertEquals(emptyList(), c.contextReferences)
        assertEquals(null, c.permissionScopeReference)
        assertEquals(emptyList(), c.resourceReferences)
        assertEquals(null, c.cancellationReason)
    }

    @Test
    fun `START must not carry an existing agentRunId`() {
        assertFailsWith<IllegalArgumentException> {
            AgentRunCommand(
                commandType = AgentRunCommandType.START,
                taskId = TaskId("task-1"),
                agentRunId = AgentRunId("run-1"),
                requestingPrincipalId = PrincipalId("user-1"),
                goalDescription = "Draft a reply",
                correlationId = "corr-2",
            )
        }
    }

    @Test
    fun `a START command may declare a target capability, resource references, and a permission scope reference`() {
        val c = startCommand().copy(
            targetAgentCapability = setOf(PermissionAction.READ, PermissionAction.SEND_EXTERNAL),
            resourceReferences = listOf(ResourceId("resource-1")),
            permissionScopeReference = "scope-ref-1",
        )

        assertEquals(setOf(PermissionAction.READ, PermissionAction.SEND_EXTERNAL), c.targetAgentCapability)
        assertEquals(listOf(ResourceId("resource-1")), c.resourceReferences)
        assertEquals("scope-ref-1", c.permissionScopeReference)
    }

    // --- AgentRunCommand: SUSPEND / RESUME require an existing agentRunId ---

    @Test
    fun `SUSPEND and RESUME require an existing agentRunId`() {
        assertFailsWith<IllegalArgumentException> {
            AgentRunCommand(
                commandType = AgentRunCommandType.SUSPEND,
                taskId = TaskId("task-1"),
                requestingPrincipalId = PrincipalId("user-1"),
                goalDescription = "Suspend pending user input",
                correlationId = "corr-4",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            AgentRunCommand(
                commandType = AgentRunCommandType.RESUME,
                taskId = TaskId("task-1"),
                requestingPrincipalId = PrincipalId("user-1"),
                goalDescription = "Resume after user input",
                correlationId = "corr-5",
            )
        }
    }

    @Test
    fun `a SUSPEND command with an existing agentRunId is well-typed`() {
        val c = AgentRunCommand(
            commandType = AgentRunCommandType.SUSPEND,
            taskId = TaskId("task-1"),
            agentRunId = AgentRunId("run-1"),
            requestingPrincipalId = PrincipalId("user-1"),
            goalDescription = "Suspend pending user input",
            correlationId = "corr-4",
        )

        assertEquals(AgentRunCommandType.SUSPEND, c.commandType)
        assertEquals(AgentRunId("run-1"), c.agentRunId)
    }

    // --- AgentRunCommand: CANCEL requires a non-blank cancellationReason ---

    @Test
    fun `CANCEL requires an existing agentRunId and a non-blank cancellationReason`() {
        val c = cancelCommand()

        assertEquals(AgentRunCommandType.CANCEL, c.commandType)
        assertEquals(AgentRunId("run-1"), c.agentRunId)
        assertEquals("Task Manager Task was cancelled by its owner", c.cancellationReason)

        assertFailsWith<IllegalArgumentException> { cancelCommand(cancellationReason = null) }
        assertFailsWith<IllegalArgumentException> { cancelCommand(cancellationReason = "") }
        assertFailsWith<IllegalArgumentException> { cancelCommand(cancellationReason = "   ") }
    }

    @Test
    fun `cancellationReason is ignored for non-CANCEL command types`() {
        val c = startCommand()

        assertEquals(null, c.cancellationReason)
    }

    // --- AgentRunCommand: shared field validation ---

    @Test
    fun `a blank goalDescription is rejected`() {
        assertFailsWith<IllegalArgumentException> { startCommand(goalDescription = "") }
        assertFailsWith<IllegalArgumentException> { startCommand(goalDescription = "   ") }
    }

    @Test
    fun `a blank correlationId is rejected`() {
        assertFailsWith<IllegalArgumentException> { startCommand(correlationId = "") }
    }

    @Test
    fun `two commands with identical fields are equal`() {
        assertEquals(startCommand(), startCommand())
    }

    // --- AgentRunCommandResult ---

    @Test
    fun `Accepted names the resulting agentRunId and the command type it accepted`() {
        val result: AgentRunCommandResult =
            AgentRunCommandResult.Accepted(AgentRunId("run-1"), AgentRunCommandType.START)

        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunId("run-1"), accepted.agentRunId)
        assertEquals(AgentRunCommandType.START, accepted.commandType)
    }

    @Test
    fun `Rejected requires a non-blank reason`() {
        val result = AgentRunCommandResult.Rejected(AgentRunCommandType.CANCEL, "no such Agent Run is active")

        assertEquals("no such Agent Run is active", result.reason)
        assertFailsWith<IllegalArgumentException> {
            AgentRunCommandResult.Rejected(AgentRunCommandType.CANCEL, "")
        }
    }

    // --- AgentRunCommandChannel (interface shape only -- no implementation exists yet) ---

    @Test
    fun `AgentRunCommandChannel declares submit as a suspend function returning AgentRunCommandResult`() {
        val submit = AgentRunCommandChannel::class.functions.single { it.name == "submit" }

        assertTrue(submit.isSuspend)
        assertEquals(AgentRunCommandResult::class, submit.returnType.classifier)
    }
}
