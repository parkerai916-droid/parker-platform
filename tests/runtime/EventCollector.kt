package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.EventBus
import parker.core.interfaces.EventType
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Subscription

/**
 * Sprint 1, Unit 10 (renumbered from the Vertical Slice Plan's original
 * Unit 9, "Event assertion harness" -- Unit 9 was inserted ahead of it to
 * actually publish the events this class collects; see
 * `docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6):
 * "Subscribe to every `EventType` Sprint 1 emits (`planner.*`, `task.*`,
 * `agent.*`, `execution.*`, `permission.*`) and collect them for post-hoc
 * causal-sequence assertion... this harness subscribes to each concrete
 * `EventType` it expects individually, not a wildcard."
 *
 * Test-only fixture. Deliberately does NOT live in `src/runtime` --
 * nothing in the specifications requires a production auditing/collection
 * component; this exists solely so a test can reconstruct what a run
 * actually published, mirroring `FakePermissionEngine.kt`/`MockTool.kt`/
 * `DeterministicPlannerHarness.kt`'s identical "test fixture, not
 * `src/runtime`" precedent.
 *
 * ## Scope: what this does and does not do
 *
 * **Observes only.** [EventCollector] calls [EventBus.subscribe] and
 * nothing else on [eventBus] -- it never calls [EventBus.publish]. It
 * holds no reference to, and never calls, the Planner, Task Manager,
 * Agent Runtime, Execution Pipeline, Tool Registry, or Identity Service;
 * its only relationship to any of them is indirect, through whichever
 * [EventBus] instance a test also wires them to.
 *
 * **Subscribes to a fixed, concrete list of `EventType`s, not a
 * wildcard.** [SPRINT_1_EVENT_TYPES] is a snapshot of exactly what Units
 * 5-9's runtime code publishes today (`docs/implementation/
 * SPRINT_1_VERTICAL_SLICE_PLAN.md` Unit 9's own event tables) -- not a
 * general `EventType` catalogue, and not a resolution of `EventType.md`'s
 * own open "should wildcard subscription exist" question, which this unit
 * deliberately works around rather than answers. `permission.deferred` is
 * intentionally absent: `DefaultExecutionPipeline`'s `DEFERRED` branch
 * never calls its own `publishLifecycleEvent`, so no such event exists to
 * subscribe to.
 *
 * **Fabricates nothing.** Every [ParkerEvent] in [collectedEvents] is
 * exactly the instance a producer passed to `EventBus.publish` -- this
 * class never constructs, mutates, or synthesises a [ParkerEvent] of its
 * own.
 *
 * **Insertion order is delivery order**, which [InMemoryEventBus]'s own
 * KDoc documents as synchronous/sequential per `publish` call -- a
 * property of that concrete implementation, not a general [EventBus]
 * contract guarantee this class asserts for every possible implementation.
 *
 * `subscriberPrincipalId` defaults to [COLLECTOR_PRINCIPAL_ID], the same
 * kind of hardcoded Sprint 1 placeholder as Unit 9's publisher Principals
 * -- [EventBus.subscribe] does not authenticate or authorise the
 * subscriber identity it is given (documented existing gap), so this
 * placeholder carries no real trust meaning.
 */
class EventCollector(
    private val eventBus: EventBus,
    subscriberPrincipalId: PrincipalId = COLLECTOR_PRINCIPAL_ID,
    eventTypes: Set<EventType> = SPRINT_1_EVENT_TYPES,
) {
    companion object {
        /** Sprint 1 placeholder -- see this class's own KDoc. */
        val COLLECTOR_PRINCIPAL_ID = PrincipalId("system.event-collector")

        /**
         * Sprint 1 snapshot, not a general EventType catalogue -- see this
         * class's own KDoc. Exactly the concrete EventTypes Units 5-9
         * publish today; must be updated by hand if a later unit adds more
         * publish calls.
         */
        val SPRINT_1_EVENT_TYPES: Set<EventType> = setOf(
            // planner.* (DeterministicPlannerHarness, Unit 9)
            EventType("planner.session_created"),
            EventType("planner.context_requested"),
            EventType("planner.analysis_started"),
            EventType("planner.proposal_created"),
            EventType("planner.proposal_submitted"),
            // task.* (InMemoryTaskManagerRuntime, Unit 9)
            EventType("task.created"),
            EventType("task.ready"),
            // agent.* (InMemoryAgentRuntime, Unit 9)
            EventType("agent.created"),
            EventType("agent.initialised"),
            EventType("agent.ready"),
            EventType("agent.started"),
            EventType("agent.step_completed"),
            EventType("agent.completed"),
            EventType("agent.failed"),
            // execution.*/permission.* (DefaultExecutionPipeline, pre-existing Phase 2 code)
            EventType("execution.request_received"),
            EventType("execution.started"),
            EventType("execution.completed"),
            EventType("execution.failed"),
            EventType("permission.requested"),
            EventType("permission.granted"),
            EventType("permission.denied"),
        )
    }

    private val mutex = Mutex()
    private val events = mutableListOf<ParkerEvent>()

    private val subscriptions: List<Subscription> = eventTypes.map { eventType ->
        eventBus.subscribe(eventType, subscriberPrincipalId) { event ->
            mutex.withLock { events += event }
        }
    }

    /** Every [ParkerEvent] delivered to this collector so far, in delivery order. */
    suspend fun collectedEvents(): List<ParkerEvent> = mutex.withLock { events.toList() }

    /** The subset of [collectedEvents] sharing [correlationId], in delivery order. */
    suspend fun eventsFor(correlationId: String): List<ParkerEvent> =
        mutex.withLock { events.filter { it.correlationId == correlationId } }

    /** Cancels every subscription this collector holds. Idempotent, per [Subscription.cancel]'s own contract. */
    suspend fun stop() {
        subscriptions.forEach { it.cancel() }
    }
}
