package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AgentRunCommand
import parker.core.interfaces.AgentRunCommandResult
import parker.core.interfaces.AgentRunCommandType
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EventBus
import parker.core.interfaces.EventHandler
import parker.core.interfaces.EventType
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.PublishResult
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.interfaces.Subscription
import parker.core.interfaces.TaskId
import parker.core.interfaces.TaskProposal
import parker.core.interfaces.TaskProposalId
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolLifecycleState
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 1, Unit 9 (Runtime Lifecycle Event Publication) acceptance test.
 *
 * Not the Vertical Slice Plan's original Unit 9 ("Event assertion
 * harness" / `EventCollector.kt`, now renumbered to Unit 10) -- this file
 * proves the *inserted* unit: that [DeterministicPlannerHarness],
 * [InMemoryTaskManagerRuntime], and [InMemoryAgentRuntime] now publish the
 * `planner.*`/`task.*`/`agent.*` events each already-implemented lifecycle
 * transition is specified to produce (`PlannerRuntimeSpecification.md`
 * §11, `TaskManagerRuntimeSpecification.md` §10,
 * `AgentRuntimeSpecification.md` §9), not that a subscriber can collect
 * them for causal-sequence assertion across all three -- that remains
 * Unit 10's job. This file uses a local recording [EventBus] instead of a
 * real `EventCollector` for exactly that reason: it is a test tool for
 * this unit's own assertions, not the deliverable Unit 10 will add.
 *
 * Scope note: this file does not re-prove Units 5-7's own state-machine
 * behaviour (lifecycle transitions, disposition/result values,
 * determinism, duplicate-submission handling, etc.) -- that is
 * `DeterministicPlannerHarnessTest.kt`, `InMemoryTaskManagerRuntimeTest.kt`,
 * and `InMemoryAgentRuntimeTest.kt`'s own, already-covered territory. It
 * asserts only on the events this unit adds: which `EventType`s are
 * published, in what order, with what `correlationId`/payload, and that
 * no event fires for a transition that did not really happen.
 */
class RuntimeLifecycleEventPublicationTest {

    /**
     * Test-only: records every [ParkerEvent] passed to [publish], in
     * order, while still delegating to a real backing [EventBus] so
     * `subscribe`/trust-sensitive-signature/authenticator behaviour is
     * exercised unchanged -- mirrors this codebase's existing
     * wrap-and-record fixture style (`FakePermissionEngine`'s
     * `evaluateCallCount`), applied to [EventBus] instead.
     */
    private class RecordingEventBus(private val delegate: EventBus = InMemoryEventBus()) : EventBus {
        val published = mutableListOf<ParkerEvent>()

        override suspend fun publish(event: ParkerEvent): PublishResult {
            published += event
            return delegate.publish(event)
        }

        override fun subscribe(eventType: EventType, subscriberPrincipalId: PrincipalId, handler: EventHandler): Subscription =
            delegate.subscribe(eventType, subscriberPrincipalId, handler)
    }

    // ================= DeterministicPlannerHarness (planner.*) =================

    @Test
    fun `DeterministicPlannerHarness publishes exactly the 5 planner events, in order, sharing run()'s correlationId`() = runTest {
        val bus = RecordingEventBus()
        val harness = DeterministicPlannerHarness(bus)

        harness.run(PlanningSessionId("session-1"), PrincipalId("user-1"), "read today's calendar", "corr-1")

        assertEquals(
            listOf(
                "planner.session_created",
                "planner.context_requested",
                "planner.analysis_started",
                "planner.proposal_created",
                "planner.proposal_submitted",
            ),
            bus.published.map { it.eventType.value },
        )
        assertTrue(bus.published.all { it.correlationId == "corr-1" })
    }

    @Test
    fun `planner_session_created payload carries the initiating Principal and goal`() = runTest {
        val bus = RecordingEventBus()
        val harness = DeterministicPlannerHarness(bus)

        harness.run(PlanningSessionId("session-1"), PrincipalId("user-1"), "read today's calendar", "corr-1")

        val created = bus.published.first { it.eventType.value == "planner.session_created" }
        assertEquals("user-1", created.payload["initiatingPrincipalId"])
        assertEquals("read today's calendar", created.payload["goal"])
    }

    @Test
    fun `planner_proposal_submitted payload carries the constructed TaskProposal's id`() = runTest {
        val bus = RecordingEventBus()
        val harness = DeterministicPlannerHarness(bus)

        harness.run(PlanningSessionId("session-1"), PrincipalId("user-1"), "read today's calendar", "corr-1")

        val submitted = bus.published.first { it.eventType.value == "planner.proposal_submitted" }
        assertEquals(harness.taskProposals.single().taskProposalId.value, submitted.payload["taskProposalId"])
    }

    @Test
    fun `two independent harness runs publish independent, non-interleaved event sequences`() = runTest {
        val bus = RecordingEventBus()
        val first = DeterministicPlannerHarness(bus)
        val second = DeterministicPlannerHarness(bus)

        first.run(PlanningSessionId("session-1"), PrincipalId("user-1"), "goal one", "corr-1")
        second.run(PlanningSessionId("session-2"), PrincipalId("user-2"), "goal two", "corr-2")

        assertEquals(10, bus.published.size)
        assertEquals(5, bus.published.count { it.correlationId == "corr-1" })
        assertEquals(5, bus.published.count { it.correlationId == "corr-2" })
    }

    // ================= InMemoryTaskManagerRuntime (task.*) =================

    private fun principal(id: String = "user-1") = Principal(
        principalId = PrincipalId(id),
        principalType = PrincipalType.USER,
        displayName = "Test Principal",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun taskProposal(
        taskProposalId: String = "proposal-1",
        ownerPrincipalId: String = "user-1",
        correlationId: String = "corr-1",
    ) = TaskProposal(
        taskProposalId = TaskProposalId(taskProposalId),
        planningSessionId = PlanningSessionId("session-1"),
        initiatingPrincipalId = PrincipalId(ownerPrincipalId),
        proposedOwnerPrincipalId = PrincipalId(ownerPrincipalId),
        goal = "read today's calendar",
        source = RequestOrigin.TEXT,
        priority = RequestPriority.NORMAL,
        correlationId = correlationId,
    )

    @Test
    fun `InMemoryTaskManagerRuntime publishes task_created then task_ready, in order, for an accepted proposal`() = runTest {
        val bus = RecordingEventBus()
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity, bus)

        runtime.submitProposal(taskProposal())

        assertEquals(listOf("task.created", "task.ready"), bus.published.map { it.eventType.value })
        assertTrue(bus.published.all { it.correlationId == "corr-1" })
    }

    @Test
    fun `task_created payload carries the resolved ownerPrincipalId and Task Source`() = runTest {
        val bus = RecordingEventBus()
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity, bus)

        runtime.submitProposal(taskProposal())

        val created = bus.published.first { it.eventType.value == "task.created" }
        assertEquals("user-1", created.payload["ownerPrincipalId"])
        assertEquals(RequestOrigin.TEXT.name, created.payload["source"])
    }

    @Test
    fun `an unresolvable owner publishes no task events -- no Task record was ever created`() = runTest {
        val bus = RecordingEventBus()
        val runtime = InMemoryTaskManagerRuntime(InMemoryIdentityService(), bus)

        runtime.submitProposal(taskProposal(ownerPrincipalId = "ghost-user"))

        assertTrue(bus.published.isEmpty())
    }

    @Test
    fun `two independent proposals publish independent task event sequences (no regression from isolation)`() = runTest {
        val bus = RecordingEventBus()
        val identity = InMemoryIdentityService()
        identity.register(principal("user-1"))
        identity.register(principal("user-2"))
        val runtime = InMemoryTaskManagerRuntime(identity, bus)

        runtime.submitProposal(taskProposal(taskProposalId = "proposal-1", ownerPrincipalId = "user-1", correlationId = "corr-1"))
        runtime.submitProposal(taskProposal(taskProposalId = "proposal-2", ownerPrincipalId = "user-2", correlationId = "corr-2"))

        assertEquals(4, bus.published.size)
        assertEquals(2, bus.published.count { it.correlationId == "corr-1" })
        assertEquals(2, bus.published.count { it.correlationId == "corr-2" })
    }

    // ================= InMemoryAgentRuntime (agent.*) =================

    private val calendarResourceId = ResourceId("res.calendar.1")
    private val toolResourceId = ResourceId("res.tool.calendar-reader")

    private suspend fun buildAgentRuntime(
        decisionFor: (ExecutionRequest) -> PermissionDecision,
    ): Pair<InMemoryAgentRuntime, RecordingEventBus> {
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

        val bus = RecordingEventBus()
        val permissionEngine = FakePermissionEngine(decisionFor)
        val pipeline = DefaultExecutionPipeline(resources, actionMapper, permissionEngine, tools, bus)

        val identity = InMemoryIdentityService()
        identity.register(
            Principal(
                principalId = PrincipalId("user-1"),
                principalType = PrincipalType.USER,
                displayName = "Test Owner",
                owner = null,
                status = PrincipalStatus.CREATED,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        identity.register(
            Principal(
                principalId = PrincipalId("agent-for-task-1"),
                principalType = PrincipalType.INTERNAL_AGENT,
                displayName = "Test Agent",
                owner = PrincipalId("user-1"),
                status = PrincipalStatus.CREATED,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        return InMemoryAgentRuntime(identity, pipeline, bus) to bus
    }

    private fun approvedDecision() = PermissionDecision(
        decisionId = DecisionId("dec-1"),
        principalId = PrincipalId("agent-for-task-1"),
        resourceId = calendarResourceId,
        action = PermissionAction.READ,
        decision = PermissionDecisionOutcome.APPROVED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun startCommand(correlationId: String = "corr-1") = AgentRunCommand(
        commandType = AgentRunCommandType.START,
        taskId = TaskId("task-1"),
        requestingPrincipalId = PrincipalId("user-1"),
        goalDescription = "read today's calendar",
        resourceReferences = listOf(calendarResourceId),
        correlationId = correlationId,
    )

    @Test
    fun `InMemoryAgentRuntime publishes the 6 agent events in order and ends at agent_completed on SUCCESS`() = runTest {
        val (runtime, bus) = buildAgentRuntime { approvedDecision() }

        val result = runtime.submit(startCommand())

        assertIs<AgentRunCommandResult.Accepted>(result)
        assertEquals(
            listOf(
                "agent.created",
                "agent.initialised",
                "agent.ready",
                "agent.started",
                "agent.step_completed",
                "agent.completed",
            ),
            bus.published.filter { it.eventType.value.startsWith("agent.") }.map { it.eventType.value },
        )
    }

    @Test
    fun `agent events share run() commands correlationId and are published under the Agent Identity`() = runTest {
        val (runtime, bus) = buildAgentRuntime { approvedDecision() }

        runtime.submit(startCommand(correlationId = "corr-xyz"))

        val agentEvents = bus.published.filter { it.eventType.value.startsWith("agent.") }
        assertTrue(agentEvents.isNotEmpty())
        assertTrue(agentEvents.all { it.correlationId == "corr-xyz" })
        assertTrue(agentEvents.all { it.publisherPrincipalId == PrincipalId("agent-for-task-1") })
    }

    @Test
    fun `a DENIED ExecutionResult still publishes agent_step_completed then agent_failed, never agent_completed`() = runTest {
        val (runtime, bus) = buildAgentRuntime { approvedDecision().copy(decision = PermissionDecisionOutcome.DENIED) }

        runtime.submit(startCommand())

        val agentEventTypes = bus.published.filter { it.eventType.value.startsWith("agent.") }.map { it.eventType.value }
        assertEquals(
            listOf("agent.created", "agent.initialised", "agent.ready", "agent.started", "agent.step_completed", "agent.failed"),
            agentEventTypes,
        )
        assertTrue("agent.completed" !in agentEventTypes)
    }

    @Test
    fun `agent_created still publishes on the unresolvable Agent Identity Rejected path, with nothing after it`() = runTest {
        val bus = RecordingEventBus()
        val resources = InMemoryResourceRegistry()
        val vocabulary = InMemoryActionVocabulary()
        val actionMapper = ActionMapper(vocabulary)
        val tools = InMemoryToolRegistry(resources)
        val pipeline = DefaultExecutionPipeline(resources, actionMapper, FakePermissionEngine { approvedDecision() }, tools, bus)
        val identity = InMemoryIdentityService() // Agent Identity deliberately not registered
        val runtime = InMemoryAgentRuntime(identity, pipeline, bus)

        val result = runtime.submit(startCommand())

        assertIs<AgentRunCommandResult.Rejected>(result)
        assertEquals(listOf("agent.created"), bus.published.filter { it.eventType.value.startsWith("agent.") }.map { it.eventType.value })
    }

    @Test
    fun `agent events and the ExecutionPipeline's own execution and permission events share one causal correlationId on the same bus`() = runTest {
        val (runtime, bus) = buildAgentRuntime { approvedDecision() }

        runtime.submit(startCommand(correlationId = "corr-shared"))

        val allTypes = bus.published.map { it.eventType.value }
        assertTrue("execution.request_received" in allTypes)
        assertTrue("permission.granted" in allTypes)
        assertTrue("execution.completed" in allTypes)
        assertTrue("agent.completed" in allTypes)
        // Not every execution.*/permission.* event carries corr-shared (DefaultExecutionPipeline
        // stamps ExecutionRequest.correlationId, which InMemoryAgentRuntime already sets from
        // command.correlationId -- Unit 7's own existing behaviour, unchanged by this unit).
        assertTrue(bus.published.filter { it.eventType.value.startsWith("execution.") }.all { it.correlationId == "corr-shared" })
        assertTrue(bus.published.filter { it.eventType.value.startsWith("agent.") }.all { it.correlationId == "corr-shared" })
    }
}
