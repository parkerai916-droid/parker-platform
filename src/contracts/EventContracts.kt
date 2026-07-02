package parker.core.interfaces

import java.time.Instant

/**
 * EventBus supporting types (`docs/specifications/volume-03-core-interfaces/
 * {EventType,EventHandler,Subscription,PublishResult}.md`), closing
 * consistency review §2.2. `ParkerEvent` is the Kotlin mapping of
 * `docs/schemas/Event.schema.json`, not previously implemented (deferred
 * per `Event-Schema.md` to "the phase that implements EventBus" -- this
 * one).
 */

/**
 * EventType.md: an open, namespaced string identifier, not a closed enum.
 * Namespacing convention: `<domain>.<event>` for core events (e.g.
 * `execution.completed`), `plugin:<pluginId>.<event>` for plugin-supplied
 * events. Validation here is deliberately loose (non-blank, must contain
 * a `.`) -- EventType.md gives a convention and examples, not a strict
 * grammar, so this does not invent a stricter one than specified.
 */
@JvmInline
value class EventType(val value: String) {
    init {
        require(value.isNotBlank()) { "EventType must not be blank" }
        require('.' in value) {
            "EventType '$value' must be namespaced as <domain>.<event> or plugin:<pluginId>.<event>"
        }
    }
}

/**
 * Kotlin mapping of `docs/schemas/Event.schema.json` ("ParkerEvent" in
 * that schema's title). Field names and requiredness mirror the schema
 * exactly: eventId, publisherPrincipalId, eventType, timestamp,
 * correlationId, payload are required; signature and metadata are
 * nullable/defaulted, matching the schema's optional properties.
 */
data class ParkerEvent(
    val eventId: String,
    val publisherPrincipalId: PrincipalId,
    val eventType: EventType,
    val timestamp: Instant,
    val correlationId: String,
    val payload: Map<String, String>,
    val signature: String? = null,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(eventId.isNotBlank()) { "ParkerEvent.eventId must not be blank" }
        require(correlationId.isNotBlank()) { "ParkerEvent.correlationId must not be blank" }
    }
}

/**
 * EventHandler.md: a single-method functional contract. Fire-and-forget
 * per EventBus.md "Delivery Guarantees" -- the return value cannot affect
 * delivery, and a throw MUST be caught and isolated by the bus (see
 * [InMemoryEventBus]), not propagated to the publisher.
 */
fun interface EventHandler {
    suspend fun handle(event: ParkerEvent)
}

/**
 * Subscription.md: a live, cancellable subscription. `cancel()` MUST be
 * idempotent (mirrors [CancellationResult]'s established pattern).
 */
interface Subscription {
    val subscriptionId: String
    val eventType: EventType
    val subscriberPrincipalId: PrincipalId
    val active: Boolean
    suspend fun cancel()
}

/**
 * PublishResult.md: the sealed outcome of [EventBus.publish], following
 * the same success/failure-with-reason pattern as [ValidationResult].
 */
sealed class PublishResult {
    data class Delivered(val deliveredCount: Int) : PublishResult()
    data class Rejected(val reason: String) : PublishResult()
    data class PartialFailure(val deliveredCount: Int, val failedSubscriptionIds: List<String>) : PublishResult()
}
