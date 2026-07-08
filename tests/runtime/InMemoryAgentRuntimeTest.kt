package parker.core.runtime

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AgentPolicy
import parker.core.interfaces.AgentRunCommand
import parker.core.interfaces.AgentRunCommandResult
import parker.core.interfaces.AgentRunCommandType
import parker.core.interfaces.AgentRunId
import parker.core.interfaces.AgentRunStatus
import parker.core.interfaces.AgentStepDecision
import parker.core.interfaces.AgentStepSource
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EventType
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
import parker.core.interfaces.ToolResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sprint 1, Unit 7 origin
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6, Unit 7),
 * rewritten in full by **Sprint 3, Track C, Unit C2** to prove the
 * multi-step Agent Run model specified by
 * `docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md`. Wiring still mirrors
 * `DefaultExecutionPipelineTest.kt`'s existing `buildPipeline` pattern.
 *
 * ## What changed from Unit 7's test suite
 *
 * [InMemoryAgentRuntime]'s constructor now requires an [AgentStepSource]
 * and an [AgentPolicy]. Every existing test's `buildRuntime` call site is
 * updated for this; [singleStepThenCompleteSource] reproduces Unit 7's
 * exact one-step-then-done behaviour as an explicit, visible
 * [AgentStepSource] configuration, so the pre-existing happy-path
 * assertions (`COMPLETED` after one step, `evaluateCallCount == 1`, etc.)
 * continue to hold unchanged.
 *
 * The three Unit 7 tests asserting `SUSPEND`/`RESUME`/`CANCEL` were
 * "Rejected as not implemented" are removed -- they are no longer true,
 * since this unit implements all three -- and replaced by the dedicated
 * `SUSPEND` / `RESUME` / `CANCEL` sections below, mirroring exactly how
 * Sprint 2's Unit B2 superseded an earlier now-incorrect test with an
 * explanatory note rather than a silent deletion.
 *
 * New coverage added by this unit: multi-step success: `Complete` honoured
 * only after >=1 successful step; `Complete` before any successful step
 * treated as `Fail`; `DENIED` vs `DEFERRED` now distinct outcomes;
 * `AgentPolicy.maxAgentSteps` ending a run at `SUSPENDED`, never `FAILED`;
 * an explicit `SUSPEND` honoured at the next step boundary, not mid-step;
 * `RESUME` continuing with the next step number, not resetting to 1;
 * `CANCEL` accepted from several non-terminal states and rejected from a
 * terminal one; and, via [ControllableTool] (a genuinely pausable `Tool`
 * `MockTool` cannot be), two concurrency proofs: an unrelated Agent Run's
 * command is not blocked while another Agent Run's step is executing
 * (design document §8's locking model), and `CANCEL` received mid-step
 * applies immediately without waiting for that step to finish.
 */
class InMemoryAgentRuntimeTest {

    private val calendarResourceId = ResourceId("res.calendar.1")
    private val toolResourceId = ResourceId("res.tool.calendar-reader")

    // Distinct resources/tools used only by the concurrency tests further down, so a slow
    // (ControllableTool-backed) Agent Run and a fast (MockTool-backed) Agent Run can coexist in
    // one runtime instance without contending over the same Tool.
    private val slowCalendarResourceId = ResourceId("res.calendar.slow")
    private val fastEmailResourceId = ResourceId("res.email.fast")
    private val slowToolResourceId = ResourceId("res.tool.slow")
    private val fastToolResourceId = ResourceId("res.tool.fast")

    /**
     * Reproduces Unit 7's exact one-step-then-done behaviour as an explicit
     * [AgentStepSource]: `Propose(context.goal, context.resourceReferences)`
     * on step 1, `Complete` on step 2 (reached only after step 1 succeeds)
     * -- the default used by every test below that does not itself care
     * about multi-step behaviour.
     *
     * Bug fixed: this previously took `proposedAction`/`targetResources`
     * as its own defaulted parameters (`"read today's calendar"` /
     * `listOf(calendarResourceId)`) instead of reading them from
     * [AgentStepContext] -- since nothing ever called it with non-default
     * arguments, every test using the default [buildRuntime] wiring
     * silently proposed the fixed, always-resolvable calendar action/
     * resource regardless of what `AgentRunCommand.goalDescription`/
     * `resourceReferences` a given test actually supplied. This is what let
     * `an unresolvable target resource also ends the Agent Run at FAILED`
     * pass identity/READY/RUNNING as normal but then silently substitute a
     * *resolvable* resource for the deliberately-unresolvable one the test
     * supplies, so `DefaultExecutionPipeline` never hit its resource-
     * resolution-failure branch and the run reached `COMPLETED` instead of
     * `FAILED`. Reading from `context` instead (exactly like the top-level
     * [SingleStepAgentStepSource] fixture already does) makes the proposed
     * action/target genuinely track whatever the test's own `AgentRunCommand`
     * specifies, restoring that test's original intent without changing
     * `InMemoryAgentRuntime`, `DefaultExecutionPipeline`, or any other
     * production code.
     */
    private fun singleStepThenCompleteSource() = FakeAgentStepSource { context ->
        if (context.stepNumber == 1) {
            AgentStepDecision.Propose(context.goal, context.resourceReferences)
        } else {
            AgentStepDecision.Complete
        }
    }

    /**
     * Agent Run Reference Exposure (`docs/implementation/AGENT_RUN_REFERENCE_EXPOSURE_IMPLEMENTATION_PLAN.md`
     * Section 5): now returns [RuntimeFixture], not `Triple`, so a test can
     * also reach the [InMemoryEventBus] this helper already builds
     * internally (previously discarded after being passed to the
     * constructors below) -- needed to subscribe before triggering an
     * Agent Run and assert on published event payloads. Every existing
     * `val (runtime, identity, permissionEngine) = buildRuntime(...)` call
     * site is unaffected: Kotlin destructuring is positional and does not
     * require consuming every available `componentN()`.
     */
    private data class RuntimeFixture(
        val runtime: InMemoryAgentRuntime,
        val identity: InMemoryIdentityService,
        val permissionEngine: FakePermissionEngine,
        val eventBus: InMemoryEventBus,
    )

    private suspend fun buildRuntime(
        agentStepSource: AgentStepSource = singleStepThenCompleteSource(),
        agentPolicy: AgentPolicy = AgentPolicy(maxAgentSteps = 10),
        decisionFor: (ExecutionRequest) -> PermissionDecision,
    ): RuntimeFixture {
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
        val toolDescriptor = ToolDescriptor(
            toolId = "tool.calendar.read",
            displayName = "Calendar Reader",
            description = "Reads calendar entries",
            supportedActions = setOf(PermissionAction.READ),
            supportedResourceTypes = setOf(ResourceType.CALENDAR),
        )
        tools.register(toolDescriptor, toolResourceId)
        tools.setLifecycleState("tool.calendar.read", "0.1.0", ToolLifecycleState.ENABLED)

        val eventBus = InMemoryEventBus()
        val permissionEngine = FakePermissionEngine(decisionFor)
        val toolInvocationBinding = InMemoryToolInvocationBinding()
        toolInvocationBinding.bind(toolDescriptor, MockTool(toolDescriptor))
        val pipeline = DefaultExecutionPipeline(resources, actionMapper, permissionEngine, tools, eventBus, toolInvocationBinding)

        val identity = InMemoryIdentityService()
        val runtime = InMemoryAgentRuntime(identity, pipeline, eventBus, agentStepSource, agentPolicy)
        return RuntimeFixture(runtime, identity, permissionEngine, eventBus)
    }

    /** Fixture shared by the two concurrency tests: a slow (ControllableTool-backed) Agent Run path and a fast (MockTool-backed) one, in one runtime. */
    private class ConcurrencyFixture(
        val runtime: InMemoryAgentRuntime,
        val identity: InMemoryIdentityService,
        val controllableTool: ControllableTool,
    )

    private suspend fun buildConcurrencyFixture(agentStepSource: AgentStepSource): ConcurrencyFixture {
        val resources = InMemoryResourceRegistry()
        resources.register(
            Resource(
                resourceId = slowCalendarResourceId,
                resourceType = ResourceType.CALENDAR,
                displayName = "Slow Calendar",
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
                resourceId = fastEmailResourceId,
                resourceType = ResourceType.EMAIL,
                displayName = "Fast Email",
                ownerPrincipalId = PrincipalId("user-2"),
                sensitivity = ResourceSensitivity.HOUSEHOLD,
                lifecycleState = ResourceLifecycleState.AVAILABLE,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                source = "test",
            ),
        )
        resources.register(
            Resource(
                resourceId = slowToolResourceId,
                resourceType = ResourceType.TOOL,
                displayName = "Slow Tool Resource",
                ownerPrincipalId = PrincipalId("system"),
                sensitivity = ResourceSensitivity.PUBLIC,
                lifecycleState = ResourceLifecycleState.AVAILABLE,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                source = "test",
            ),
        )
        resources.register(
            Resource(
                resourceId = fastToolResourceId,
                resourceType = ResourceType.TOOL,
                displayName = "Fast Tool Resource",
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
                verbPhrase = "slow action",
                mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.CALENDAR)),
            ),
        )
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = "fast action",
                mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.EMAIL)),
            ),
        )
        val actionMapper = ActionMapper(vocabulary)

        val tools = InMemoryToolRegistry(resources)
        val slowDescriptor = ToolDescriptor(
            toolId = "tool.slow",
            displayName = "Slow Tool",
            description = "Blocks in execute() until a test releases it",
            supportedActions = setOf(PermissionAction.READ),
            supportedResourceTypes = setOf(ResourceType.CALENDAR),
        )
        val fastDescriptor = ToolDescriptor(
            toolId = "tool.fast",
            displayName = "Fast Tool",
            description = "Returns immediately",
            supportedActions = setOf(PermissionAction.READ),
            supportedResourceTypes = setOf(ResourceType.EMAIL),
        )
        tools.register(slowDescriptor, slowToolResourceId)
        tools.register(fastDescriptor, fastToolResourceId)
        tools.setLifecycleState("tool.slow", "0.1.0", ToolLifecycleState.ENABLED)
        tools.setLifecycleState("tool.fast", "0.1.0", ToolLifecycleState.ENABLED)

        val eventBus = InMemoryEventBus()
        val permissionEngine = FakePermissionEngine { request ->
            PermissionDecision(
                decisionId = DecisionId("dec-${request.requestId.value}"),
                principalId = request.principalId,
                resourceId = request.targetResources.first(),
                action = PermissionAction.READ,
                decision = PermissionDecisionOutcome.APPROVED,
                level = PermissionLevel.AUTOMATIC,
                timestamp = Instant.parse("2026-01-01T00:00:00Z"),
            )
        }
        val controllableTool = ControllableTool(slowDescriptor)
        val fastTool = MockTool(fastDescriptor)
        val toolInvocationBinding = InMemoryToolInvocationBinding()
        toolInvocationBinding.bind(slowDescriptor, controllableTool)
        toolInvocationBinding.bind(fastDescriptor, fastTool)
        val pipeline = DefaultExecutionPipeline(resources, actionMapper, permissionEngine, tools, eventBus, toolInvocationBinding)

        val identity = InMemoryIdentityService()
        val runtime = InMemoryAgentRuntime(identity, pipeline, eventBus, agentStepSource, AgentPolicy(maxAgentSteps = 10))
        return ConcurrencyFixture(runtime, identity, controllableTool)
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

    private fun suspendCommand(taskId: String = "task-1", agentRunId: AgentRunId = AgentRunId("run-for-$taskId")) = AgentRunCommand(
        commandType = AgentRunCommandType.SUSPEND,
        taskId = TaskId(taskId),
        agentRunId = agentRunId,
        requestingPrincipalId = PrincipalId("user-1"),
        goalDescription = "pause",
        correlationId = "corr-1",
    )

    private fun resumeCommand(taskId: String = "task-1", agentRunId: AgentRunId = AgentRunId("run-for-$taskId")) = AgentRunCommand(
        commandType = AgentRunCommandType.RESUME,
        taskId = TaskId(taskId),
        agentRunId = agentRunId,
        requestingPrincipalId = PrincipalId("user-1"),
        goalDescription = "resume",
        correlationId = "corr-1",
    )

    private fun cancelCommand(
        taskId: String = "task-1",
        agentRunId: AgentRunId = AgentRunId("run-for-$taskId"),
        reason: String = "test cancellation",
    ) = AgentRunCommand(
        commandType = AgentRunCommandType.CANCEL,
        taskId = TaskId(taskId),
        agentRunId = agentRunId,
        requestingPrincipalId = PrincipalId("user-1"),
        goalDescription = "cancel",
        correlationId = "corr-1",
        cancellationReason = reason,
    )

    // --- happy path (Unit 7, adapted for the AgentStepSource/AgentPolicy constructor params) ---

    @Test
    fun `START resolves identity, reaches RUNNING, and COMPLETED once Complete is decided after a successful step`() = runTest {
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

    // --- Agent Run Reference Exposure (docs/implementation/AGENT_RUN_REFERENCE_EXPOSURE_IMPLEMENTATION_PLAN.md) ---

    @Test
    fun `agent-completed's published payload carries the real, returned AgentRunId`() = runTest {
        val fixture = buildRuntime { approvedDecision() }
        fixture.identity.register(owner())
        fixture.identity.register(agentIdentity())
        var completedPayload: Map<String, String>? = null
        fixture.eventBus.subscribe(EventType("agent.completed"), PrincipalId("test-subscriber")) { event ->
            completedPayload = event.payload
        }

        val result = fixture.runtime.submit(startCommand())
        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)

        assertEquals(accepted.agentRunId.value, completedPayload?.get("agentRunId"))
    }

    @Test
    fun `agent-created's published payload also carries agentRunId, proving exposure is uniform, not agent-completed alone`() = runTest {
        val fixture = buildRuntime { approvedDecision() }
        fixture.identity.register(owner())
        fixture.identity.register(agentIdentity())
        var createdPayload: Map<String, String>? = null
        fixture.eventBus.subscribe(EventType("agent.created"), PrincipalId("test-subscriber")) { event ->
            createdPayload = event.payload
        }

        val result = fixture.runtime.submit(startCommand())
        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)

        // agent.created fires before the Agent Run reaches any terminal state -- proving
        // agentRunId is exposed uniformly via the one shared publish(run, eventType) helper,
        // not special-cased for agent.completed alone.
        assertEquals(accepted.agentRunId.value, createdPayload?.get("agentRunId"))
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
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(agentIdentity(type = PrincipalType.SYSTEM, owner = null))

        val result = runtime.submit(startCommand())

        assertIs<AgentRunCommandResult.Rejected>(result)
        assertEquals(AgentRunStatus.CREATED, runtime.getAgentRun(AgentRunId("run-for-task-1"))?.status)
    }

    // --- non-SUCCESS ExecutionResult: DENIED vs DEFERRED (Unit C2 makes these distinct) ---

    @Test
    fun `a DENIED ExecutionResult still returns Accepted but the Agent Run ends at FAILED`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision().copy(decision = PermissionDecisionOutcome.DENIED) }
        identity.register(owner())
        identity.register(agentIdentity())

        val result = runtime.submit(startCommand())

        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunStatus.FAILED, runtime.getAgentRun(accepted.agentRunId)?.status)
    }

    @Test
    fun `a DEFERRED ExecutionResult ends the current step at SUSPENDED, distinct from DENIED`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision().copy(decision = PermissionDecisionOutcome.DEFERRED) }
        identity.register(owner())
        identity.register(agentIdentity())

        val result = runtime.submit(startCommand())

        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunStatus.SUSPENDED, runtime.getAgentRun(accepted.agentRunId)?.status)
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

    // --- multi-step (new in Unit C2) ---

    @Test
    fun `a multi-step Agent Run submits one ExecutionRequest per Propose and reaches COMPLETED after Complete`() = runTest {
        val stepNumbersSeen = mutableListOf<Int>()
        val source = FakeAgentStepSource { context ->
            stepNumbersSeen += context.stepNumber
            when (context.stepNumber) {
                1, 2 -> AgentStepDecision.Propose("read today's calendar", listOf(calendarResourceId))
                else -> AgentStepDecision.Complete
            }
        }
        val (runtime, identity, permissionEngine) = buildRuntime(agentStepSource = source) { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())

        val result = runtime.submit(startCommand())

        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunStatus.COMPLETED, runtime.getAgentRun(accepted.agentRunId)?.status)
        assertEquals(listOf(1, 2, 3), stepNumbersSeen)
        assertEquals(2, permissionEngine.evaluateCallCount)
    }

    @Test
    fun `a Complete decision before any Agent Step has succeeded ends the Agent Run at FAILED, not COMPLETED`() = runTest {
        val source = FakeAgentStepSource { AgentStepDecision.Complete }
        val (runtime, identity, permissionEngine) = buildRuntime(agentStepSource = source) { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())

        val result = runtime.submit(startCommand())

        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunStatus.FAILED, runtime.getAgentRun(accepted.agentRunId)?.status)
        assertEquals(0, permissionEngine.evaluateCallCount)
    }

    @Test
    fun `a Fail decision ends the Agent Run at FAILED without ever submitting an ExecutionRequest`() = runTest {
        val source = FakeAgentStepSource { AgentStepDecision.Fail("cannot proceed") }
        val (runtime, identity, permissionEngine) = buildRuntime(agentStepSource = source) { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())

        val result = runtime.submit(startCommand())

        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunStatus.FAILED, runtime.getAgentRun(accepted.agentRunId)?.status)
        assertEquals(0, permissionEngine.evaluateCallCount)
    }

    @Test
    fun `reaching AgentPolicy maxAgentSteps after a successful step ends the Agent Run at SUSPENDED, not FAILED`() = runTest {
        val source = FakeAgentStepSource { AgentStepDecision.Propose("read today's calendar", listOf(calendarResourceId)) }
        val (runtime, identity, permissionEngine) = buildRuntime(
            agentStepSource = source,
            agentPolicy = AgentPolicy(maxAgentSteps = 1),
        ) { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())

        val result = runtime.submit(startCommand())

        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunStatus.SUSPENDED, runtime.getAgentRun(accepted.agentRunId)?.status)
        assertEquals(1, permissionEngine.evaluateCallCount) // only the one permitted step was ever attempted
        assertEquals(1, source.nextStepCallCount) // the second consultation never happened -- the bound is
        // checked before consulting AgentStepSource again, not after
    }

    // --- SUSPEND ---

    @Test
    fun `an explicit SUSPEND is honoured at the next step boundary, not mid-step`() = runTest {
        // FakeAgentStepSource's decisionFor is a plain, non-suspend lambda type -- calling a
        // suspend function such as InMemoryAgentRuntime.submit from inside it does not compile
        // ("Suspension functions can be called only within coroutine body"). ControllableAgentStepSource
        // is used instead: its nextStep is a genuine suspend override, and it pauses after entering
        // step 1 so this test can submit SUSPEND from a concurrently launched coroutine while the
        // Agent Run is still genuinely RUNNING -- step 1 has been consulted but has not yet even
        // proposed an action, let alone reached WAITING_FOR_PERMISSION. If SUSPEND applied
        // immediately instead of at the boundary, step 1 itself would never complete; if it were
        // lost, step 2 (which this source would refuse, if ever reached) would run instead.
        val source = ControllableAgentStepSource()
        val (runtime, identity, permissionEngine) = buildRuntime(agentStepSource = source) { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())

        var result: AgentRunCommandResult? = null
        val job = launch { result = runtime.submit(startCommand()) }

        source.enteredStep1.await()
        val suspendResult = runtime.submit(suspendCommand())
        assertIs<AgentRunCommandResult.Accepted>(suspendResult)

        source.releaseStep1(AgentStepDecision.Propose("read today's calendar", listOf(calendarResourceId)))
        job.join()

        val accepted = assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunStatus.SUSPENDED, runtime.getAgentRun(accepted.agentRunId)?.status)
        assertEquals(1, permissionEngine.evaluateCallCount)
        assertEquals(1, source.nextStepCallCount)
    }

    @Test
    fun `SUSPEND is rejected when the Agent Run is not RUNNING`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())
        runtime.submit(startCommand()) // reaches COMPLETED synchronously

        val result = runtime.submit(suspendCommand())

        assertIs<AgentRunCommandResult.Rejected>(result)
    }

    @Test
    fun `SUSPEND for an unknown agentRunId is rejected`() = runTest {
        val (runtime, _, _) = buildRuntime { approvedDecision() }

        val result = runtime.submit(suspendCommand(agentRunId = AgentRunId("run-for-nonexistent")))

        assertIs<AgentRunCommandResult.Rejected>(result)
    }

    // --- RESUME ---

    @Test
    fun `RESUME transitions SUSPENDED back to RUNNING and continues with the next step number, not resetting to 1`() = runTest {
        // Same reason as the SUSPEND-at-a-step-boundary test above: FakeAgentStepSource's
        // decisionFor is a plain, non-suspend lambda, so calling InMemoryAgentRuntime.submit (a
        // suspend function) from inside it does not compile. ControllableAgentStepSource is used
        // instead so SUSPEND can be submitted, from a concurrently launched coroutine, while step 1
        // is genuinely still RUNNING and has not yet proposed an action.
        val source = ControllableAgentStepSource()
        val (runtime, identity, permissionEngine) = buildRuntime(agentStepSource = source) { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())

        var startResult: AgentRunCommandResult? = null
        val startJob = launch { startResult = runtime.submit(startCommand()) }

        source.enteredStep1.await()
        assertIs<AgentRunCommandResult.Accepted>(runtime.submit(suspendCommand()))
        source.releaseStep1(AgentStepDecision.Propose("read today's calendar", listOf(calendarResourceId)))
        startJob.join()

        val started = assertIs<AgentRunCommandResult.Accepted>(startResult)
        assertEquals(AgentRunStatus.SUSPENDED, runtime.getAgentRun(started.agentRunId)?.status)
        assertEquals(listOf(1), source.stepNumbersSeen)

        val resumeResult = runtime.submit(resumeCommand())

        val resumed = assertIs<AgentRunCommandResult.Accepted>(resumeResult)
        assertEquals(AgentRunStatus.COMPLETED, runtime.getAgentRun(resumed.agentRunId)?.status)
        assertEquals(listOf(1, 2), source.stepNumbersSeen)
        assertEquals(1, permissionEngine.evaluateCallCount) // only step 1 ever submitted an ExecutionRequest
    }

    @Test
    fun `RESUME is rejected when the Agent Run is not SUSPENDED`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())
        runtime.submit(startCommand()) // reaches COMPLETED synchronously

        val result = runtime.submit(resumeCommand())

        assertIs<AgentRunCommandResult.Rejected>(result)
    }

    // --- CANCEL ---

    @Test
    fun `CANCEL from CREATED (stuck due to unresolvable identity) is accepted and ends CANCELLED`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(owner()) // Agent Identity itself is not registered -> stuck at CREATED
        assertIs<AgentRunCommandResult.Rejected>(runtime.submit(startCommand()))
        assertEquals(AgentRunStatus.CREATED, runtime.getAgentRun(AgentRunId("run-for-task-1"))?.status)

        val result = runtime.submit(cancelCommand())

        assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunStatus.CANCELLED, runtime.getAgentRun(AgentRunId("run-for-task-1"))?.status)
    }

    @Test
    fun `CANCEL from SUSPENDED is accepted and ends CANCELLED`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision().copy(decision = PermissionDecisionOutcome.DEFERRED) }
        identity.register(owner())
        identity.register(agentIdentity())
        val started = assertIs<AgentRunCommandResult.Accepted>(runtime.submit(startCommand()))
        assertEquals(AgentRunStatus.SUSPENDED, runtime.getAgentRun(started.agentRunId)?.status)

        val result = runtime.submit(cancelCommand())

        assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(AgentRunStatus.CANCELLED, runtime.getAgentRun(started.agentRunId)?.status)
    }

    @Test
    fun `CANCEL from a terminal state (COMPLETED) is rejected`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())
        val started = assertIs<AgentRunCommandResult.Accepted>(runtime.submit(startCommand()))
        assertEquals(AgentRunStatus.COMPLETED, runtime.getAgentRun(started.agentRunId)?.status)

        val result = runtime.submit(cancelCommand(reason = "too late"))

        val rejected = assertIs<AgentRunCommandResult.Rejected>(result)
        assertTrue(rejected.reason.isNotBlank())
        assertEquals(AgentRunStatus.COMPLETED, runtime.getAgentRun(started.agentRunId)?.status) // unchanged
    }

    @Test
    fun `CANCEL for an unknown agentRunId is rejected`() = runTest {
        val (runtime, _, _) = buildRuntime { approvedDecision() }

        val result = runtime.submit(cancelCommand(agentRunId = AgentRunId("run-for-nonexistent")))

        assertIs<AgentRunCommandResult.Rejected>(result)
    }

    // --- concurrency: the Section 8 locking model ---

    @Test
    fun `a slow Agent Step in one Agent Run does not block a command for a different, independent Agent Run`() = runTest {
        val source = FakeAgentStepSource { context ->
            when {
                context.taskId == TaskId("task-slow") && context.stepNumber == 1 ->
                    AgentStepDecision.Propose("slow action", listOf(slowCalendarResourceId))
                context.taskId == TaskId("task-fast") && context.stepNumber == 1 ->
                    AgentStepDecision.Propose("fast action", listOf(fastEmailResourceId))
                else -> AgentStepDecision.Complete
            }
        }
        val fixture = buildConcurrencyFixture(source)
        fixture.identity.register(owner("user-1"))
        fixture.identity.register(owner("user-2"))
        fixture.identity.register(agentIdentity(taskId = "task-slow", owner = PrincipalId("user-1")))
        fixture.identity.register(agentIdentity(taskId = "task-fast", owner = PrincipalId("user-2")))

        val slowJob = launch {
            fixture.runtime.submit(
                startCommand(
                    taskId = "task-slow",
                    goalDescription = "slow action",
                    resourceReferences = listOf(slowCalendarResourceId),
                    correlationId = "corr-slow",
                ),
            )
        }

        fixture.controllableTool.executeStarted.await()

        // While the slow Agent Run's step is still executing -- blocked inside the Tool, with the
        // Agent Runtime's own Mutex deliberately NOT held across that call (design document
        // Section 8) -- a completely unrelated Agent Run's START must still be able to proceed and
        // finish. If the Mutex were (incorrectly) held for the whole slow submit() call, this next
        // line would hang until the test timed out.
        val fastResult = fixture.runtime.submit(
            startCommand(
                taskId = "task-fast",
                goalDescription = "fast action",
                resourceReferences = listOf(fastEmailResourceId),
                correlationId = "corr-fast",
            ),
        )
        val fastAccepted = assertIs<AgentRunCommandResult.Accepted>(fastResult)
        assertEquals(AgentRunStatus.COMPLETED, fixture.runtime.getAgentRun(fastAccepted.agentRunId)?.status)

        fixture.controllableTool.complete(ToolResult(toolId = "tool.slow", success = true))
        slowJob.join()

        assertEquals(AgentRunStatus.COMPLETED, fixture.runtime.getAgentRun(AgentRunId("run-for-task-slow"))?.status)
    }

    @Test
    fun `CANCEL received while a step is executing applies immediately and the next step never starts`() = runTest {
        val source = FakeAgentStepSource { AgentStepDecision.Propose("slow action", listOf(slowCalendarResourceId)) }
        val fixture = buildConcurrencyFixture(source)
        fixture.identity.register(owner("user-1"))
        fixture.identity.register(agentIdentity(taskId = "task-slow", owner = PrincipalId("user-1")))

        val job = launch {
            fixture.runtime.submit(
                startCommand(
                    taskId = "task-slow",
                    goalDescription = "slow action",
                    resourceReferences = listOf(slowCalendarResourceId),
                    correlationId = "corr-slow",
                ),
            )
        }

        fixture.controllableTool.executeStarted.await()

        val cancelResult = fixture.runtime.submit(cancelCommand(taskId = "task-slow", reason = "user changed their mind"))
        assertIs<AgentRunCommandResult.Accepted>(cancelResult)
        assertEquals(AgentRunStatus.CANCELLED, fixture.runtime.getAgentRun(AgentRunId("run-for-task-slow"))?.status)

        // The step that was already in flight eventually finishes with SUCCESS. That must have no
        // further effect: the Agent Run stays CANCELLED, and no second Agent Step is ever attempted.
        fixture.controllableTool.complete(ToolResult(toolId = "tool.slow", success = true))
        job.join()

        assertEquals(AgentRunStatus.CANCELLED, fixture.runtime.getAgentRun(AgentRunId("run-for-task-slow"))?.status)
        assertEquals(1, source.nextStepCallCount)
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

        val exception = assertFailsWith<IllegalStateException> {
            runtime.submit(startCommand())
        }
        assertTrue(exception.message?.contains("has already been processed") == true)
    }

    // --- Pre-Module Readiness Unit 2 (gap #48): one Agent Run per Task is a deliberate constraint ---

    @Test
    fun `the one-Agent-Run-per-Task cap is a deliberate, documented decision, not an accidental limitation`() = runTest {
        val (runtime, identity, _) = buildRuntime { approvedDecision() }
        identity.register(owner())
        identity.register(agentIdentity())

        runtime.submit(startCommand())

        val exception = assertFailsWith<IllegalStateException> {
            runtime.submit(startCommand())
        }
        assertTrue(exception.message?.contains("deliberate, documented constraint") == true)
        assertTrue(exception.message?.contains("IMPLEMENTATION_GAPS.md #48") == true)
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
