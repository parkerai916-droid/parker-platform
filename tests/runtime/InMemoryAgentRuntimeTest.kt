package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AgentRunCommand
import parker.core.interfaces.AgentRunCommandResult
import parker.core.interfaces.AgentRunCommandType
import parker.core.interfaces.AgentRunId
import parker.core.interfaces.AgentRunStatus
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.interfaces.TaskId
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolLifecycleState
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sprint 1, Unit 7 acceptance test
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6, Unit 7; §7's
 * "Agent Runtime creates Agent Run" / "Agent Runtime submits Execution
 * Request" rows): "given the `AgentRunCommand`, the Agent Instance reaches
 * `INITIALISED` only after resolving a `PrincipalId` via the Identity
 * Service, then `READY`, then `RUNNING`... constructs exactly one
 * `ExecutionRequest` with `origin = RequestOrigin.AGENT`... reaches
 * `COMPLETED` once a matching `ExecutionResult` is returned."
 *
 * Wiring mirrors `DefaultExecutionPipelineTest.kt`'s existing
 * `buildPipeline` pattern exactly -- this file does not invent a new way
 * to assemble a real `DefaultExecutionPipeline`.
 *
 * Scope note: this file proves Unit 7 (identity resolution, the fixed
 * `CREATED -> INITIALISED -> READY -> RUNNING -> {COMPLETED, FAILED}`
 * path, and one constructed+submitted `ExecutionRequest`). It does not
 * exercise `WAITING_FOR_PERMISSION`, `WAITING_FOR_INPUT`, or `SUSPENDED`
 * -- see `AgentRunLifecycleTransitionsTest.kt` for the full 10-state
 * lifecycle's own coverage, independent of this runtime -- and it does
 * not implement or test `SUSPEND`/`RESUME`/`CANCEL` beyond confirming
 * they are rejected as out of this unit's scope.
 */
class InMemoryAgentRuntimeTest {

    private val calendarResourceId = ResourceId("res.calendar.1")
    private val toolResourceId = ResourceId("res.tool.calendar-reader")

    private suspend fun buildRuntime(
        decisionFor: (ExecutionRequest) -> PermissionDecision,
    ): Triple<InMemoryAgentRuntime, InMemoryIdentityService, FakePermissionEngine> {
        val resources = InMemoryResourceRegistry()
        resources.register(
            Resource(
                resourceId = calendarResourceId,
                resourceType = ResourceType.CALENDAR,
                displayName = "Household Calendar",
                ownerPrincipalId = PrincipalId("user-1"),
                sensitivity = ResourceSensitivity.HOUSEHOLD,
                lifecycleState = ResourceLifecycleState.AVAILABLE,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                source = "test",
            ),
        )
        resources.register(
            Resource(
                resourceId = toolResourceId,
                resourceType = ResourceType.TOOL,
                displayName = "Calendar Reader Tool Resource",
                ownerPrincipalId = PrincipalId("system"),
                sensitivity = ResourceSensitivity.PUBLIC,
                lifecycleState = ResourceLifecycleState.AVAILABLE,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                source = "test",
            ),
        )

        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = "read today's calendar",
                mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.CALENDAR)),
            ),
        )
        val actionMapper = ActionMapper(vocabulary)

        val tools = InMemoryToolRegistry(resources)
        tools.register(
            ToolDescriptor(
                toolId = "tool.calendar.read",
                displayName = "Calendar Reader",
                description = "Reads calendar entries",
                supportedActions = setOf(PermissionAction.READ),
                supportedResourceTypes = setOf(ResourceType.CALENDAR),
            ),
            toolResourceId,
        )
        tools.setLifecycleState("tool.calendar.read", "0.1.0", ToolLifecycleState.ENABLED)

        val eventBus = InMemoryEventBus()
        val permissionEngine = FakePermissionEngine(decisionFor)
        val pipeline = DefaultExecutionPipeline(resources, actionMapper, permissionEngine, tools, eventBus)

        val identity = InMemoryIdentityService()
        val runtime = InMemoryAgentRuntime(identity, pipeline)
        return Triple(runtime, identity, permissionEngine)
    }

    private fun approvedDecision(action: PermissionAction = PermissionAction.READ) = PermissionDecision(
        decisionId = DecisionId("dec-1"),
        principalId = PrincipalId("agent-for-task-1"),
        resourceId = calendarResourceId,
        action = action,
        decision = PermissionDecisionOutcome.APPROVED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun owner(id: String = "user-1") = Principal(
        principalId = PrincipalId(id),
        principalType = PrincipalType.USER,
        displayName = "Test Owner",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun agentIdentity(
        taskId: String = "task-1",
        type: PrincipalType = PrincipalType.INTERNAL_AGENT,
        owner: PrincipalId? = PrincipalId("user-1"),
    ) = Principal(
        principalId = PrincipalId("agent-for-$taskId"),
        principalType = type,
        displayName = "Test Agent",
        owner = owner,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun startCommand(
        taskId: String = "task-1",
        goalDescription: String = "read today's calendar",
        resourceReferences: List<ResourceId> = listOf(calendarResourceId),
        correlationId: String = "corr-1",
    ) = AgentRunCommand(
        commandType = AgentRunCommandType.START,
        taskId = TaskId(taskId),
        requestingPrincipalId = PrincipalId("user-1"),
        goalDescription = goalDescription,
        resourceReferences = resourceReferences,
        correlationId = correlationId,
    )

    // --- happy path ---

    @Test
    fun `START resolves identity, reaches RUNNING, and COMPLETED once a SUCCESS ExecutionResult is returned`() = runTest {
        val (runtime, identity, permissionEngine) = buildRuntime { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())

        val result = runtime.submit(startCommand())

        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunCommandType.START, accepted.commandType)
        assertEquals(AgentRunId("run-for-task-1"), accepted.agentRunId)

        val run = runtime.getAgentRun(accepted.agentRunId)
        assertNotNull(run)
        assertEquals(AgentRunStatus.COMPLETED, run.status)
        assertEquals(PrincipalId("agent-for-task-1"), run.agentIdentityPrincipalId)
        assertEquals(TaskId("task-1"), run.taskId)
        assertEquals(1, permissionEngine.evaluateCallCount)
    }

    @Test
    fun `the one Agent Step constructs an ExecutionRequest with origin AGENT`() = runTest {
        var capturedOrigin: RequestOrigin? = null
        val (runtime, identity, _) = buildRuntime { request ->
            capturedOrigin = request.origin
            approvedDecision()
        }
        identity.register(owner())
        identity.register(agentIdentity())

        runtime.submit(startCommand())

        assertEquals(RequestOrigin.AGENT, capturedOrigin)
    }

    // --- unresolvable / invalid identity ---

    @Test
    fun `an unresolvable Agent Identity is Rejected and the run stays stuck at CREATED`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(owner()) // owner registered, but the Agent Identity itself is not

        val result = runtime.submit(startCommand())

        val rejected = assertIs<AgentRunCommandResult.Rejected>(result)
        assertEquals(AgentRunCommandType.START, rejected.commandType)
        assertTrue(rejected.reason.isNotBlank())

        val run = runtime.getAgentRun(AgentRunId("run-for-task-1"))
        assertNotNull(run)
        assertEquals(AgentRunStatus.CREATED, run.status)
    }

    @Test
    fun `a resolvable Principal that is not an INTERNAL_AGENT is Rejected`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity(type = PrincipalType.USER))

        val result = runtime.submit(startCommand())

        assertIs<AgentRunCommandResult.Rejected>(result)
        assertEquals(AgentRunStatus.CREATED, runtime.getAgentRun(AgentRunId("run-for-task-1"))?.status)
    }

    @Test
    fun `a resolvable SYSTEM Principal (also not INTERNAL_AGENT) is Rejected`() = runTest {
        // Distinct from the USER case above: SYSTEM, like USER, needs no owner to register
        // (InMemoryIdentityService.register), so this also proves the type check independently
        // of the owner check below.
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(agentIdentity(type = PrincipalType.SYSTEM, owner = null))

        val result = runtime.submit(startCommand())

        assertIs<AgentRunCommandResult.Rejected>(result)
        assertEquals(AgentRunStatus.CREATED, runtime.getAgentRun(AgentRunId("run-for-task-1"))?.status)
    }

    // Note: an INTERNAL_AGENT Principal with a null owner cannot be constructed through
    // InMemoryIdentityService.register() at all -- it already enforces AgentRuntimeSpecification.md
    // Section 7's "non-null owner" rule at registration time (see InMemoryIdentityService.kt).
    // InMemoryAgentRuntime's own `identity.owner == null` check is therefore defence-in-depth against
    // a state the current IdentityService implementation already refuses to produce, not a
    // reachable-today scenario this test suite can independently exercise without bypassing
    // IdentityService's own public API.

    // --- non-SUCCESS ExecutionResult ---

    @Test
    fun `a DENIED ExecutionResult still returns Accepted but the Agent Run ends at FAILED`() = runTest {
        val (runtime, identity, _) = buildRuntime {
            approvedDecision().copy(decision = PermissionDecisionOutcome.DENIED)
        }
        identity.register(owner())
        identity.register(agentIdentity())

        val result = runtime.submit(startCommand())

        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunStatus.FAILED, runtime.getAgentRun(accepted.agentRunId)?.status)
    }

    @Test
    fun `an unresolvable target resource also ends the Agent Run at FAILED`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())

        val result = runtime.submit(startCommand(resourceReferences = listOf(ResourceId("nonexistent"))))

        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunStatus.FAILED, runtime.getAgentRun(accepted.agentRunId)?.status)
    }

    // --- SUSPEND / RESUME / CANCEL out of scope ---

    @Test
    fun `SUSPEND is Rejected as not implemented by this Sprint 1 runtime`() = runTest {
        val (runtime, _, _) = buildRuntime { approvedDecision() }

        val result = runtime.submit(
            AgentRunCommand(
                commandType = AgentRunCommandType.SUSPEND,
                taskId = TaskId("task-1"),
                agentRunId = AgentRunId("run-for-task-1"),
                requestingPrincipalId = PrincipalId("user-1"),
                goalDescription = "pause",
                correlationId = "corr-1",
            ),
        )

        val rejected = assertIs<AgentRunCommandResult.Rejected>(result)
        assertEquals(AgentRunCommandType.SUSPEND, rejected.commandType)
        assertTrue(rejected.reason.contains("not implemented"))
    }

    @Test
    fun `RESUME and CANCEL are also Rejected as not implemented`() = runTest {
        val (runtime, _, _) = buildRuntime { approvedDecision() }

        val resumeResult = runtime.submit(
            AgentRunCommand(
                commandType = AgentRunCommandType.RESUME,
                taskId = TaskId("task-1"),
                agentRunId = AgentRunId("run-for-task-1"),
                requestingPrincipalId = PrincipalId("user-1"),
                goalDescription = "resume",
                correlationId = "corr-1",
            ),
        )
        val cancelResult = runtime.submit(
            AgentRunCommand(
                commandType = AgentRunCommandType.CANCEL,
                taskId = TaskId("task-1"),
                agentRunId = AgentRunId("run-for-task-1"),
                requestingPrincipalId = PrincipalId("user-1"),
                goalDescription = "cancel",
                correlationId = "corr-1",
                cancellationReason = "test cancellation",
            ),
        )

        assertIs<AgentRunCommandResult.Rejected>(resumeResult)
        assertIs<AgentRunCommandResult.Rejected>(cancelResult)
    }

    // --- unknown lookup ---

    @Test
    fun `getAgentRun returns null for an unknown agentRunId, not an exception`() = runTest {
        val (runtime, _, _) = buildRuntime { approvedDecision() }
        assertNull(runtime.getAgentRun(AgentRunId("run-for-nonexistent")))
    }

    // --- duplicate START ---

    @Test
    fun `resubmitting START for the same taskId is rejected as caller misuse`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())

        runtime.submit(startCommand())

        assertFailsWith<IllegalStateException> {
            runtime.submit(startCommand())
        }
    }

    // --- isolation between independent commands ---

    @Test
    fun `two independent START commands produce two independent, isolated Agent Runs`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(owner("user-1"))
        identity.register(owner("user-2"))
        identity.register(agentIdentity(taskId = "task-1", owner = PrincipalId("user-1")))
        identity.register(agentIdentity(taskId = "task-2", owner = PrincipalId("user-2")))

        val first = assertIs<AgentRunCommandResult.Accepted>(runtime.submit(startCommand(taskId = "task-1", correlationId = "corr-1")))
        val second = assertIs<AgentRunCommandResult.Accepted>(runtime.submit(startCommand(taskId = "task-2", correlationId = "corr-2")))

        assertTrue(first.agentRunId != second.agentRunId)
        assertEquals(2, runtime.listAgentRuns().size)
        assertEquals(TaskId("task-1"), runtime.getAgentRun(first.agentRunId)?.taskId)
        assertEquals(TaskId("task-2"), runtime.getAgentRun(second.agentRunId)?.taskId)
    }
}
