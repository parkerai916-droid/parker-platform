package parker.composition

import parker.core.interfaces.EventBus
import parker.core.interfaces.EventType
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Subscription

/**
 * Sprint 10, Unit 4 (Production Composition Root). Logs "Execution
 * authorised" / "Execution denied" and related Trust/execution lifecycle
 * events by subscribing to [EventBus] -- the one sanctioned, already-
 * existing integration mechanism this repository uses for exactly this
 * purpose elsewhere (`InMemoryTaskManagerRuntime` subscribes to
 * `agent.completed`/`agent.failed` the same way, per
 * `IMPLEMENTATION_GAPS.md` #42). This is deliberately **not** a decorator
 * around `ExecutionPipeline` or `PermissionEngine`: `DefaultExecutionPipeline`
 * already publishes every event named below as part of its own,
 * already-approved, unmodified behaviour
 * (`src/runtime/DefaultExecutionPipeline.kt`'s own `publishLifecycleEvent`
 * call sites) -- subscribing to observe them is strictly additive, touches
 * no frozen file, and requires no new decorator type for a component this
 * Unit has no need to wrap for any other reason.
 *
 * **Why only these seven event types, and not a wildcard subscription.**
 * [EventBus.subscribe] takes exactly one [EventType] per call (no
 * wildcard/prefix subscription exists on the interface) -- this class
 * subscribes to each Trust/execution-lifecycle event type
 * `DefaultExecutionPipeline` is known to publish, individually, rather
 * than inventing a subscription mechanism the interface does not offer.
 *
 * **Log-line mapping, and why "Reply delivered" is inferred, not
 * independently observed.** `execution.completed` fires once
 * `LocalTextChannelDeliverTool.execute` has already run successfully
 * (`DefaultExecutionPipeline.executeResolvedTool`) -- since this
 * composition root's only production `ExecutionPipeline` caller is
 * `ResponseDelivery.deliver`, and this Unit's Local Text Channel module
 * exposes exactly one Tool (`deliver`), an observed `execution.completed`
 * event on this runtime's own [eventBus] instance is, in practice, always
 * the deliver Tool's own successful run -- logged here as "Reply
 * delivered." This is a structural inference specific to this composition
 * root's own fixed wiring, not a general claim `ExecutionPipeline` itself
 * makes or could make for an arbitrary caller.
 *
 * Every log line below carries `correlationId` and, where applicable,
 * `eventType` only -- [ParkerEvent.payload] is never logged wholesale,
 * since a future Tool's payload is not this Unit's to assume is
 * non-sensitive.
 */
class RuntimeEventLogger(
    private val eventBus: EventBus,
    private val logger: ParkerLogger,
    private val subscriberPrincipalId: PrincipalId,
) {

    private companion object {
        val PERMISSION_GRANTED = EventType("permission.granted")
        val PERMISSION_DENIED = EventType("permission.denied")
        val PERMISSION_REQUESTED = EventType("permission.requested")
        val EXECUTION_REQUEST_RECEIVED = EventType("execution.request_received")
        val EXECUTION_STARTED = EventType("execution.started")
        val EXECUTION_COMPLETED = EventType("execution.completed")
        val EXECUTION_FAILED = EventType("execution.failed")
    }

    private var subscriptions: List<Subscription> = emptyList()

    /** Idempotent within one [RuntimeEventLogger] instance's own lifetime -- calling this twice replaces, not duplicates, the subscription set. */
    fun start() {
        subscriptions = listOf(
            eventBus.subscribe(EXECUTION_REQUEST_RECEIVED, subscriberPrincipalId) { event ->
                logger.debug(loggedEvent("execution request received", event))
            },
            eventBus.subscribe(PERMISSION_REQUESTED, subscriberPrincipalId) { event ->
                logger.debug(loggedEvent("permission requested", event))
            },
            eventBus.subscribe(PERMISSION_GRANTED, subscriberPrincipalId) { event ->
                logger.info(loggedEvent("Execution authorised", event))
            },
            eventBus.subscribe(PERMISSION_DENIED, subscriberPrincipalId) { event ->
                logger.warn(loggedEvent("Execution denied", event))
            },
            eventBus.subscribe(EXECUTION_STARTED, subscriberPrincipalId) { event ->
                logger.debug(loggedEvent("execution started", event))
            },
            eventBus.subscribe(EXECUTION_COMPLETED, subscriberPrincipalId) { event ->
                logger.info(loggedEvent("Reply delivered", event))
            },
            eventBus.subscribe(EXECUTION_FAILED, subscriberPrincipalId) { event ->
                logger.warn(loggedEvent("Execution failed", event))
            },
        )
    }

    /** Cancels every subscription this instance holds, part of [ParkerRuntime.shutdown]'s own graceful-shutdown sequence. Safe to call even if [start] was never called. */
    suspend fun stop() {
        subscriptions.forEach { it.cancel() }
        subscriptions = emptyList()
    }

    private fun loggedEvent(label: String, event: ParkerEvent): String =
        "$label (eventType=${event.eventType.value}, correlationId=${event.correlationId})"
}
