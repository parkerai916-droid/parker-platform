package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AgentRunCommand
import parker.core.interfaces.AgentRunCommandChannel
import parker.core.interfaces.AgentRunCommandResult
import parker.core.interfaces.AgentRunExecutionTrigger
import parker.core.interfaces.AgentRunId
import parker.core.interfaces.AgentRunStatus
import parker.core.interfaces.CancellationResult
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionPipeline
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.ExecutionStatus
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.RequestId
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.interfaces.TaskProposalDisposition
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolLifecycleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A transparent pass-through [ExecutionPipeline] that records the exact
 * [ExecutionRequest]/[ExecutionResult] a real, wrapped [ExecutionPipeline]
 * (here, a real [DefaultExecutionPipeline]) was actually called with and
 * actually returned. Exists solely so this test can assert on the real
 * `ExecutionRequest.targetResources` [InMemoryAgentRuntime] constructs
 * internally -- that object is otherwise never exposed to a caller -- and
 * on the real `ExecutionResult` [DefaultExecutionPipeline] actually
 * returns, without modifying [InMemoryAgentRuntime], [DefaultExecutionPipeline],
 * or the [ExecutionPipeline] interface itself. Mirrors [FakePermissionEngine]'s
 * own precedent exactly: a test double implementing an existing,
 * unmodified interface, not a change to any approved-frozen production
 * file. Delegates every call unchanged; fabricates nothing.
 */
private class RecordingExecutionPipeline(private val delegate: ExecutionPipeline) : ExecutionPipeline {
    var lastRequest: ExecutionRequest? = null
        private set
    var lastResult: ExecutionResult? = null
        private set

    override suspend fun submit(request: ExecutionRequest): ExecutionResult {
        lastRequest = request
        val result = delegate.submit(request)
        lastResult = result
        return result
    }

    override suspend fun cancel(requestId: RequestId): CancellationResult = delegate.cancel(requestId)
    override suspend fun status(requestId: RequestId): ExecutionStatus = delegate.status(requestId)
}

/**
 * Sprint 1, Unit 11B: the honest, full vertical-slice end-to-end proof
 * deferred out of Unit 11A. Exercises every real component in the chain --
 * no hand-built `ExecutionRequest`, and no `.copy(resourceReferences =
 * ...)` patch anywhere -- proving that a [ResourceId] a caller supplies to
 * [DeterministicPlannerHarness.run] flows, structurally, through
 * [parker.core.interfaces.TaskProposal] -> [InMemoryTaskManagerRuntime] ->
 * [parker.core.interfaces.AgentRunCommand] -> [InMemoryAgentRuntime] ->
 * [parker.core.interfaces.ExecutionRequest] -> [DefaultExecutionPipeline]
 * -> [InMemoryToolInvocationBinding] -> a bound [MockTool], reaching a real
 * `SUCCESS`, with [EventCollector] observing the full causal sequence
 * under one shared `correlationId`.
 *
 * ## Scope: what this does and does not do
 *
 * Proves only Unit 11B's own addition (resource reference propagation),
 * using every component prior units already built and already tested
 * individually in isolation -- this file adds no new production behaviour
 * of its own beyond what `TaskProposal.kt` and
 * `InMemoryTaskManagerRuntime.kt` already gained for Unit 11B. It performs
 * no resource discovery (the target [ResourceId] is supplied directly to
 * [DeterministicPlannerHarness.run], exactly as a real caller already
 * would have to -- this harness has no [parker.core.interfaces.ResourceRegistry]
 * dependency and does not gain one here), and introduces no scheduling,
 * persistence, retry, or orchestration logic.
 *
 * Neither the Task Manager Task's derived `taskId` nor the Agent Run's
 * derived Agent Identity `PrincipalId` are predicted ahead of time by
 * string-formatting a value class -- both are read back from the real
 * objects each real component actually produced ([TaskProposalDisposition.Accepted.taskId],
 * [parker.core.interfaces.AgentRunCommand.taskId]), then used to register
 * exactly the Agent Identity [InMemoryAgentRuntime.start] will itself look
 * up, mirroring how a real caller would have to learn that identity from
 * the chain's own output rather than assume its shape.
 *
 * ## Event ordering (requirement 3)
 *
 * The asserted sequence below is the exact, already-independently-verified
 * order (`tests/runtime/EventCollectorTest.kt`'s own
 * "agent.* + execution.* / permission.* on one shared bus" coverage asserts
 * this identical interleaving already) -- not a preferred or guessed
 * order. Two placements are worth calling out because a different, equally
 * plausible-looking order exists and was deliberately not chosen:
 *
 * - `execution.request_received` precedes `permission.requested`/
 *   `permission.granted`, not the reverse -- `DefaultExecutionPipeline.submit`
 *   publishes it as its very first statement, before any validation or
 *   Permission Engine call, so that a request which is later rejected for
 *   *any* reason (unresolvable resource, unmapped action, DENIED) still has
 *   an audit record that it was received at all. Moving it after
 *   `permission.granted` would silently drop that audit event on every
 *   failure/denial path.
 * - `execution.completed` precedes `agent.step_completed`, not the reverse
 *   -- `execution.completed` is published inside the synchronous
 *   `DefaultExecutionPipeline.submit` call itself (Unit 11A's
 *   `executeResolvedTool`), which must return before
 *   `InMemoryAgentRuntime.start` can know the step's outcome at all;
 *   `agent.step_completed` is only published after that call returns. The
 *   reverse order is not achievable without `InMemoryAgentRuntime`
 *   reporting a step as "completed" before it knows whether the step
 *   succeeded, which would be dishonest.
 *
 * Neither ordering was changed to produce this result -- both are exactly
 * what the already-approved, unmodified Unit 9/Unit 11A code already does.
 */
/**
 * Corrective compilation pass (Controlled Agent Run Submission): [InMemoryTaskManagerRuntime]
 * and [InMemoryAgentRuntime] both gained new required constructor parameters this milestone.
 * This fake supplies the narrowest possible [AgentRunCommandChannel] for the
 * [InMemoryTaskManagerRuntime] instance below -- deliberately NOT the real [InMemoryAgentRuntime]
 * instance the test already builds and drives manually, so the pre-existing manual
 * `agentRuntime.submit(command)` call later in this test is completely unaffected (no double
 * submission, no duplicate-START collision). Mirrors
 * `InMemoryTaskManagerRuntimeTest.kt`'s own `FakeAgentRunCommandChannel` default-Rejected
 * convention, for the same reason: minimising behavioural impact on a file this milestone does
 * not otherwise touch.
 */
private class VerticalSliceRejectingAgentRunCommandChannel : AgentRunCommandChannel {
    override suspend fun submit(command: AgentRunCommand): AgentRunCommandResult =
        AgentRunCommandResult.Rejected(command.commandType, "test fixture -- no Agent Run started")
}

/**
 * Two-Phase Acceptance/Execution Amendment
 * (`docs/implementation/CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md`,
 * "Amendment -- Two-Phase Agent Run Operation," A.5): [InMemoryTaskManagerRuntime]'s new fourth
 * constructor parameter. [VerticalSliceRejectingAgentRunCommandChannel] always returns
 * `Rejected` for `taskManagerRuntime`'s own submission, so `execute()` is structurally never
 * called for it -- this fixture throws if it ever is. The test's manually-driven `agentRuntime`
 * instance (below) is entirely separate and calls `execute()` directly, not through this.
 */
private class VerticalSliceThrowingAgentRunExecutionTrigger : AgentRunExecutionTrigger {
    override suspend fun execute(agentRunId: AgentRunId) {
        error("execute() must never be called when AgentRunCommandChannel.submit returned Rejected")
    }
}

class VerticalSliceEndToEndTest {

    private val planningSessionId = PlanningSessionId("e2e-session")
    private val userPrincipalId = PrincipalId("user-1")
    private val goal = "read calendar"
    private val correlationId = "corr-e2e-1"
    private val calendarResourceId = ResourceId("res.calendar.1")
    private val toolResourceId = ResourceId("res.tool.calendar-reader")
    private val runInitiationResourceId = ResourceId("resource-agent-runtime-boundary")
    private val runInitiationVerbPhrase = "start agent run"

    @Test
    fun `a caller-supplied ResourceId flows structurally from the Planner to a SUCCESS ExecutionResult, with no manual patching`() = runTest {
        // --- shared infrastructure, wired exactly like DefaultExecutionPipelineTest's own fixture ---

        val eventBus = InMemoryEventBus()

        val resources = InMemoryResourceRegistry()
        resources.register(
            Resource(
                resourceId = calendarResourceId,
                resourceType = ResourceType.CALENDAR,
                displayName = "Household Calendar",
                ownerPrincipalId = userPrincipalId,
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
        // Controlled Agent Run Submission: the real production shape (a registered AGENT
        // Resource representing the Agent Runtime Execution Boundary), even though the
        // dedicated run-initiation FakePermissionEngine below does not itself consult the
        // registry -- registered for consistency with the composition root's real semantics.
        resources.register(
            Resource(
                resourceId = runInitiationResourceId,
                resourceType = ResourceType.AGENT,
                displayName = "Agent Runtime Execution Boundary",
                ownerPrincipalId = PrincipalId("system.parker"),
                sensitivity = ResourceSensitivity.PUBLIC,
                lifecycleState = ResourceLifecycleState.REGISTERED,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                source = "test",
            ),
        )

        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = goal,
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

        val toolInvocationBinding = InMemoryToolInvocationBinding()
        val mockTool = MockTool(toolDescriptor)
        toolInvocationBinding.bind(toolDescriptor, mockTool)

        // Mirrors request.principalId rather than a pre-guessed Agent Identity string --
        // the decision does not need to predict any derived identifier to be valid.
        val permissionEngine = FakePermissionEngine { request ->
            PermissionDecision(
                decisionId = DecisionId("dec-e2e-1"),
                principalId = request.principalId,
                resourceId = calendarResourceId,
                action = PermissionAction.READ,
                decision = PermissionDecisionOutcome.APPROVED,
                level = PermissionLevel.AUTOMATIC,
                timestamp = Instant.parse("2026-01-01T00:00:00Z"),
            )
        }

        // Controlled Agent Run Submission: a second, independent FakePermissionEngine for the
        // new run-initiation check, isolated from `permissionEngine` above so its own
        // `evaluateCallCount == 1` assertion (per-action gating only) is unaffected. Always
        // approves -- this test's manual `agentRuntime.submit(command)` call further below must
        // still reach Accepted exactly as it did before this milestone.
        val runInitiationPermissionEngine = FakePermissionEngine { request ->
            PermissionDecision(
                decisionId = DecisionId("dec-e2e-run-initiation"),
                principalId = request.principalId,
                resourceId = runInitiationResourceId,
                action = PermissionAction.EXECUTE,
                decision = PermissionDecisionOutcome.APPROVED,
                level = PermissionLevel.AUTOMATIC,
                timestamp = Instant.parse("2026-01-01T00:00:00Z"),
            )
        }

        val realPipeline = DefaultExecutionPipeline(resources, actionMapper, permissionEngine, tools, eventBus, toolInvocationBinding)
        // Records the real ExecutionRequest/ExecutionResult InMemoryAgentRuntime and
        // DefaultExecutionPipeline exchange internally, so this test can assert on them --
        // see RecordingExecutionPipeline's own KDoc. Delegates every call unchanged; this
        // does not alter DefaultExecutionPipeline's behaviour in any way.
        val pipeline = RecordingExecutionPipeline(realPipeline)

        val identity = InMemoryIdentityService()
        identity.register(
            Principal(
                principalId = userPrincipalId,
                principalType = PrincipalType.USER,
                displayName = "Test Owner",
                owner = null,
                status = PrincipalStatus.CREATED,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val taskManagerRuntime = InMemoryTaskManagerRuntime(
            identity,
            eventBus,
            VerticalSliceRejectingAgentRunCommandChannel(),
            VerticalSliceThrowingAgentRunExecutionTrigger(),
        )
        val agentRuntime = InMemoryAgentRuntime(
            identity,
            pipeline,
            eventBus,
            SingleStepAgentStepSource(),
            DEFAULT_AGENT_POLICY,
            runInitiationPermissionEngine,
            runInitiationResourceId,
            runInitiationVerbPhrase,
        )
        val plannerHarness = DeterministicPlannerHarness(eventBus)
        val collector = EventCollector(eventBus)

        // --- run the real chain, start to finish, reading each real identifier back rather
        // than predicting it ---

        plannerHarness.run(
            planningSessionId = planningSessionId,
            initiatingPrincipalId = userPrincipalId,
            goal = goal,
            correlationId = correlationId,
            targetResourceReferences = listOf(calendarResourceId),
        )
        val proposal = plannerHarness.taskProposals.single()
        // Object equality on the ResourceId value object itself, extracted from the real
        // TaskProposal the harness produced -- not a string/formatted-identifier comparison,
        // and not a list-equality shortcut that could pass without the same instance-equal
        // ResourceId surviving the hop.
        val resourceIdInProposal = proposal.resourceReferences.single()
        assertEquals(calendarResourceId, resourceIdInProposal)

        val disposition = taskManagerRuntime.submitProposal(proposal)
        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)

        val command = taskManagerRuntime.agentRunCommandsFor(accepted.taskId).single()
        val resourceIdInCommand = command.resourceReferences.single()
        assertEquals(calendarResourceId, resourceIdInCommand)
        assertEquals(resourceIdInProposal, resourceIdInCommand)
        assertEquals(correlationId, command.correlationId)

        // Register exactly the Agent Identity InMemoryAgentRuntime.start() will itself derive
        // and look up (PrincipalId("agent-for-${command.taskId.value}")) -- read from the real
        // AgentRunCommand's own taskId, not guessed ahead of time.
        val agentIdentityPrincipalId = PrincipalId("agent-for-${command.taskId.value}")
        identity.register(
            Principal(
                principalId = agentIdentityPrincipalId,
                principalType = PrincipalType.INTERNAL_AGENT,
                displayName = "Test Agent",
                owner = userPrincipalId,
                status = PrincipalStatus.CREATED,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val commandResult = agentRuntime.submit(command)
        val commandAccepted = assertIs<AgentRunCommandResult.Accepted>(commandResult)
        // Two-Phase Acceptance/Execution Amendment (Scope Lock Amendment, A.5): submit() alone
        // now only reaches READY; execute() drives the rest of this chain -- the real
        // ExecutionRequest/ExecutionResult capture, the Tool invocation, and the COMPLETED
        // outcome this test asserts below all require it.
        agentRuntime.execute(commandAccepted.agentRunId)

        // --- requirement 1: the same ResourceId is carried unchanged through every real
        // stage's own object, not reconstructed or guessed. Task.kt (src/contracts/Task.kt)
        // does NOT store a resourceReferences field -- it is documented as deliberately
        // minimal, explicitly excluding Section 4 concepts (including Resource references)
        // that no unit yet reads or writes (see Task.kt's own class KDoc). So the chain this
        // asserts is exactly the one that exists: TaskProposal.resourceReferences ->
        // AgentRunCommand.resourceReferences -> ExecutionRequest.targetResources.
        // `resourceIdInProposal`/`resourceIdInCommand` above already proved the first two
        // links via direct ResourceId object equality; this proves the third the same way,
        // using the real ExecutionRequest InMemoryAgentRuntime actually built (captured by
        // RecordingExecutionPipeline), not a hand-built, reconstructed, or string-compared
        // one. ---

        val capturedRequest = pipeline.lastRequest
        assertNotNull(capturedRequest)
        val resourceIdInExecutionRequest = capturedRequest.targetResources.single()
        assertEquals(calendarResourceId, resourceIdInExecutionRequest)
        assertEquals(resourceIdInCommand, resourceIdInExecutionRequest)
        assertEquals(correlationId, capturedRequest.correlationId)

        // --- requirement 2: the Tool that ran is provably the one bound via
        // ToolInvocationBinding -- validated exactly once, executed exactly once, and the
        // ExecutionResult carries that Tool's own real output, not a fabricated stand-in
        // (the pre-Unit-11A bug this pipeline used to have: a fixed "resolvedVersion" value
        // fabricated the instant a descriptor resolved, without ever calling a Tool). ---

        assertEquals(1, mockTool.validateCallCount)
        assertEquals(1, mockTool.executeCallCount)
        val capturedResult = pipeline.lastResult
        assertNotNull(capturedResult)
        assertEquals(ExecutionResultStatus.SUCCESS, capturedResult.status)
        val toolResult = capturedResult.toolResults.single()
        assertEquals("tool.calendar.read", toolResult.toolId)
        assertTrue(toolResult.success)
        // MockTool's own default resultFor (tests/runtime/MockTool.kt) produces
        // output = mapOf("intent" to request.intent) -- asserting this exact shape (not a
        // "resolvedVersion" placeholder) proves the ExecutionResult really came from
        // executing the bound MockTool, not from a fallback/fabricated success path.
        assertEquals(mapOf("intent" to capturedRequest.intent), toolResult.output)

        // --- the honest outcome one level up: the Agent Run itself reached COMPLETED ---

        val agentRun = agentRuntime.getAgentRun(commandAccepted.agentRunId)
        assertNotNull(agentRun)
        assertEquals(AgentRunStatus.COMPLETED, agentRun.status)
        assertEquals(agentIdentityPrincipalId, agentRun.agentIdentityPrincipalId)
        assertEquals(1, permissionEngine.evaluateCallCount)

        // --- EventCollector observes the full causal sequence, all sharing one correlationId ---

        val observedEventTypes = collector.eventsFor(correlationId).map { it.eventType.value }
        assertEquals(
            listOf(
                "planner.session_created",
                "planner.context_requested",
                "planner.analysis_started",
                "planner.proposal_created",
                "planner.proposal_submitted",
                "task.created",
                "task.ready",
                "task.agent_run_rejected",
                "agent.created",
                "agent.initialised",
                "agent.ready",
                "agent.started",
                "execution.request_received",
                "permission.requested",
                "permission.granted",
                "execution.started",
                "execution.completed",
                "agent.step_completed",
                "agent.completed",
            ),
            observedEventTypes,
        )
        assertTrue(collector.collectedEvents().isNotEmpty())
        assertTrue(collector.collectedEvents().all { it.correlationId == correlationId })
    }
}
