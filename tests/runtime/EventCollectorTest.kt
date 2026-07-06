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
import parker.core.interfaces.TaskProposalDisposition
import parker.core.interfaces.TaskProposalId
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolLifecycleState
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 1, Unit 10 acceptance test
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6, renumbered
 * Unit 10; §7's "EventBus records lifecycle events" row): proves
 * [EventCollector] subscribes to every Sprint 1 `EventType`, collects in
 * delivery order, preserves `correlationId`/`publisherPrincipalId`
 * verbatim, fabricates nothing, ignores anything outside its fixed
 * subscription list, and does not change any producer's own behaviour by
 * being attached.
 *
 * Scope note: this file does not re-prove Units 5-9's own behaviour
 * (lifecycle transitions, disposition/result values, which events each
 * producer publishes and why) -- that is
 * `DeterministicPlannerHarnessTest.kt`, `InMemoryTaskManagerRuntimeTest.kt`,
 * `InMemoryAgentRuntimeTest.kt`, and
 * `RuntimeLifecycleEventPublicationTest.kt`'s own, already-covered
 * territory. It asserts only on [EventCollector]'s own behaviour as a
 * subscriber.
 */
class EventCollectorTest {

    /**
     * Test-only, local to this file (mirrors
     * `RuntimeLifecycleEventPublicationTest.kt`'s identical fixture,
     * duplicated rather than shared, per this codebase's existing
     * per-file-local-helper convention): records every [ParkerEvent]
     * actually published, regardless of type, as an independent ground
     * truth to compare [EventCollector]'s own collection against.
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

    // ================= planner.* =================

    @Test
    fun `collects every planner event published, in delivery order`() = runTest {
        val bus = InMemoryEventBus()
        val collector = EventCollector(bus)
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
            collector.collectedEvents().map { it.eventType.value },
        )
    }

    @Test
    fun `planner events are collected under their documented placeholder publisherPrincipalId`() = runTest {
        val bus = InMemoryEventBus()
        val collector = EventCollector(bus)
        val harness = DeterministicPlannerHarness(bus)

        harness.run(PlanningSessionId("session-1"), PrincipalId("user-1"), "read today's calendar", "corr-1")

        assertTrue(collector.collectedEvents().all { it.publisherPrincipalId == PrincipalId("system.planner-runtime") })
    }

    // ================= task.* =================

    private fun principal(id: String = "user-1") = Principal(
        principalId = PrincipalId(id),
        principalType = PrincipalType.USER,
        displayName = "Test Principal",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun taskProposal(correlationId: String = "corr-1") = TaskProposal(
        taskProposalId = TaskProposalId("proposal-1"),
        planningSessionId = PlanningSessionId("session-1"),
        initiatingPrincipalId = PrincipalId("user-1"),
        proposedOwnerPrincipalId = PrincipalId("user-1"),
        goal = "read today's calendar",
        source = RequestOrigin.TEXT,
        priority = RequestPriority.NORMAL,
        correlationId = correlationId,
    )

    @Test
    fun `collects every task event published, in delivery order, under the Task Manager's placeholder publisher`() = runTest {
        val bus = InMemoryEventBus()
        val collector = EventCollector(bus)
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity, bus)

        val disposition = runtime.submitProposal(taskProposal())

        assertIs<TaskProposalDisposition.Accepted>(disposition) // no regression: runtime's own return value is unaffected by the collector
        assertEquals(listOf("task.created", "task.ready"), collector.collectedEvents().map { it.eventType.value })
        assertTrue(collector.collectedEvents().all { it.publisherPrincipalId == PrincipalId("system.task-manager-runtime") })
    }

    // ================= agent.* + execution.*/permission.* on one shared bus =================

    private val calendarResourceId = ResourceId("res.calendar.1")
    private val toolResourceId = ResourceId("res.tool.calendar-reader")

    private suspend fun buildAgentRuntime(bus: EventBus): InMemoryAgentRuntime {
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

        val permissionEngine = FakePermissionEngine {
            PermissionDecision(
                decisionId = DecisionId("dec-1"),
                principalId = PrincipalId("agent-for-task-1"),
                resourceId = calendarResourceId,
                action = PermissionAction.READ,
                decision = PermissionDecisionOutcome.APPROVED,
                level = PermissionLevel.AUTOMATIC,
                timestamp = Instant.parse("2026-01-01T00:00:00Z"),
            )
        }
        // Sprint 1, Unit 11A: bind a MockTool so this suite's event-collection assertions
        // are unaffected by DefaultExecutionPipeline now actually invoking the bound Tool.
        val toolInvocationBinding = InMemoryToolInvocationBinding()
        toolInvocationBinding.bind(toolDescriptor, MockTool(toolDescriptor))
        val pipeline = DefaultExecutionPipeline(resources, actionMapper, permissionEngine, tools, bus, toolInvocationBinding)

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

        return InMemoryAgentRuntime(identity, pipeline, bus, SingleStepAgentStepSource(), DEFAULT_AGENT_POLICY)
    }

    private fun startCommand(correlationId: String = "corr-1") = AgentRunCommand(
        commandType = AgentRunCommandType.START,
        taskId = TaskId("task-1"),
        requestingPrincipalId = PrincipalId("user-1"),
        goalDescription = "read today's calendar",
        resourceReferences = listOf(calendarResourceId),
        correlationId = correlationId,
    )

    @Test
    fun `collects agent and execution and permission events together, in causal order, from one shared bus`() = runTest {
        val bus = InMemoryEventBus()
        val collector = EventCollector(bus)
        val runtime = buildAgentRuntime(bus)

        val result = runtime.submit(startCommand())

        assertIs<AgentRunCommandResult.Accepted>(result) // no regression: runtime's own return value is unaffected by the collector
        val types = collector.collectedEvents().map { it.eventType.value }
        assertEquals(
            listOf(
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
            types,
        )
        assertTrue(collector.collectedEvents().all { it.correlationId == "corr-1" })
    }

    @Test
    fun `publisherPrincipalId differs correctly by domain -- agent events under the Agent Identity, not the Task Manager placeholder`() = runTest {
        val bus = InMemoryEventBus()
        val collector = EventCollector(bus)
        val runtime = buildAgentRuntime(bus)

        runtime.submit(startCommand())

        val agentEvents = collector.collectedEvents().filter { it.eventType.value.startsWith("agent.") }
        assertTrue(agentEvents.isNotEmpty())
        assertTrue(agentEvents.all { it.publisherPrincipalId == PrincipalId("agent-for-task-1") })
    }

    // ================= correlationId continuity =================

    @Test
    fun `eventsFor isolates one run's events from a concurrent, independently-correlated run on the same bus`() = runTest {
        val bus = InMemoryEventBus()
        val collector = EventCollector(bus)
        DeterministicPlannerHarness(bus).run(PlanningSessionId("session-1"), PrincipalId("user-1"), "goal one", "corr-1")
        DeterministicPlannerHarness(bus).run(PlanningSessionId("session-2"), PrincipalId("user-2"), "goal two", "corr-2")

        val corr1Events = collector.eventsFor("corr-1")
        val corr2Events = collector.eventsFor("corr-2")

        assertEquals(5, corr1Events.size)
        assertEquals(5, corr2Events.size)
        assertTrue(corr1Events.all { it.correlationId == "corr-1" })
        assertTrue(corr2Events.all { it.correlationId == "corr-2" })
        assertEquals(10, collector.collectedEvents().size)
    }

    // ================= no wildcard subscription =================

    @Test
    fun `an EventType outside the Sprint 1 list is never collected -- this is not a wildcard subscriber`() = runTest {
        val bus = InMemoryEventBus()
        val collector = EventCollector(bus)

        bus.publish(
            ParkerEvent(
                eventId = "evt-unrelated",
                publisherPrincipalId = PrincipalId("user-1"),
                eventType = EventType("resource.updated"),
                timestamp = Instant.parse("2026-01-01T00:00:00Z"),
                correlationId = "corr-unrelated",
                payload = emptyMap(),
            ),
        )

        assertTrue(collector.collectedEvents().isEmpty())
    }

    // ================= no fabrication =================

    @Test
    fun `every collected event is exactly the instance a producer published -- no fabrication, no drops, no reordering`() = runTest {
        val recordingBus = RecordingEventBus()
        val collector = EventCollector(recordingBus)
        val harness = DeterministicPlannerHarness(recordingBus)

        harness.run(PlanningSessionId("session-1"), PrincipalId("user-1"), "read today's calendar", "corr-1")

        // DeterministicPlannerHarness only ever publishes planner.* events, all of which are in
        // EventCollector.SPRINT_1_EVENT_TYPES, so the collector's list and the raw published log
        // must be identical, not merely equal in size.
        assertEquals(recordingBus.published, collector.collectedEvents())
    }

    // ================= empty state / stop() =================

    @Test
    fun `a fresh collector has collected nothing before any event is published`() = runTest {
        val collector = EventCollector(InMemoryEventBus())
        assertTrue(collector.collectedEvents().isEmpty())
    }

    @Test
    fun `after stop(), further published events are no longer collected`() = runTest {
        val bus = InMemoryEventBus()
        val collector = EventCollector(bus)
        DeterministicPlannerHarness(bus).run(PlanningSessionId("session-1"), PrincipalId("user-1"), "goal one", "corr-1")
        val countBeforeStop = collector.collectedEvents().size

        collector.stop()
        DeterministicPlannerHarness(bus).run(PlanningSessionId("session-2"), PrincipalId("user-2"), "goal two", "corr-2")

        assertEquals(countBeforeStop, collector.collectedEvents().size)
        assertEquals(5, countBeforeStop)
    }

    @Test
    fun `stop() is idempotent, mirroring Subscription cancel()'s own contract`() = runTest {
        val collector = EventCollector(InMemoryEventBus())
        collector.stop()
        collector.stop() // must not throw
    }
}
