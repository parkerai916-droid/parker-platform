package parker.core.runtime

import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap
import parker.core.interfaces.EventBus
import parker.core.interfaces.EventHandler
import parker.core.interfaces.EventType
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PublishResult
import parker.core.interfaces.Subscription

/**
 * A caller supplies whether a publisher Principal is currently in good
 * standing (EventBus.md "Authentication": events from an unresolvable, or
 * Revoked/Archived, Principal MUST be rejected). No IdentityService
 * exists yet to answer this for real (deferred per
 * `docs/architecture/IdentityService.md`) -- this is the seam where that
 * integration plugs in later. [AllowAllPrincipalAuthenticator] is a
 * stand-in, not a real trust decision, and is documented as such in
 * IMPLEMENTATION_GAPS.md.
 */
fun interface PrincipalAuthenticator {
    suspend fun isInGoodStanding(principalId: PrincipalId): Boolean
}

/** Stand-in authenticator: treats every syntactically valid [PrincipalId] as active. Not a real trust decision -- see [PrincipalAuthenticator]. */
object AllowAllPrincipalAuthenticator : PrincipalAuthenticator {
    override suspend fun isInGoodStanding(principalId: PrincipalId): Boolean = true
}

/**
 * In-memory implementation of [EventBus], per `EventBus.md` and its four
 * supporting-type documents. Implements:
 *
 * - Authentication (publisher good-standing check; presence check for the
 *   `signature` field on trust-sensitive `permission.*`/`execution.*`
 *   event types -- NOT cryptographic signature verification, which is not
 *   specified anywhere and would be inventing a crypto scheme; recorded
 *   as a gap).
 * - Ordering: FIFO per publisher, per event type, per subscriber (achieved
 *   here trivially, since delivery is synchronous/sequential per publish
 *   call and subscriptions are stored in insertion order).
 * - Delivery guarantees: at-most-once, best-effort, no replay to
 *   subscriptions created after publish.
 * - Failure handling: a throwing [EventHandler] is caught and isolated,
 *   never propagated to the publisher, never blocks delivery to other
 *   subscribers.
 * - Cancellation: idempotent `Subscription.cancel()`.
 *
 * NOT implemented (recorded in IMPLEMENTATION_GAPS.md): cascading
 * cancellation of a Principal's subscriptions on Revoke/Archive (requires
 * IdentityService lifecycle events, which don't exist yet), and real
 * cryptographic signature verification.
 */
class InMemoryEventBus(
    private val authenticator: PrincipalAuthenticator = AllowAllPrincipalAuthenticator,
) : EventBus {

    private companion object {
        val TRUST_SENSITIVE_PREFIXES = listOf("permission.", "execution.")
    }

    private class SubscriptionImpl(
        override val subscriptionId: String,
        override val eventType: EventType,
        override val subscriberPrincipalId: PrincipalId,
        private val handler: EventHandler,
        private val onCancel: (SubscriptionImpl) -> Unit,
    ) : Subscription {
        @Volatile
        override var active: Boolean = true
            private set

        suspend fun deliver(event: ParkerEvent) = handler.handle(event)

        override suspend fun cancel() {
            if (!active) return // idempotent: already cancelled is a no-op, not an error
            active = false
            onCancel(this)
        }
    }

    // eventType -> subscriptions, in registration order (supports FIFO-per-subscriber delivery).
    private val subscriptionsByType = ConcurrentHashMap<EventType, CopyOnWriteArrayList<SubscriptionImpl>>()

    override suspend fun publish(event: ParkerEvent): PublishResult {
        if (!authenticator.isInGoodStanding(event.publisherPrincipalId)) {
            return PublishResult.Rejected(
                "publisher Principal '${event.publisherPrincipalId.value}' is not in good standing",
            )
        }

        val isTrustSensitive = TRUST_SENSITIVE_PREFIXES.any { event.eventType.value.startsWith(it) }
        if (isTrustSensitive && event.signature.isNullOrBlank()) {
            return PublishResult.Rejected(
                "event type '${event.eventType.value}' is trust-sensitive and requires a non-blank signature",
            )
        }

        // Snapshot under the list's own copy-on-write semantics: safe to iterate without a bus-wide lock.
        val targets = subscriptionsByType[event.eventType]?.filter { it.active } ?: emptyList()
        if (targets.isEmpty()) {
            return PublishResult.Delivered(deliveredCount = 0)
        }

        val failedSubscriptionIds = mutableListOf<String>()
        var deliveredCount = 0
        for (subscription in targets) {
            try {
                subscription.deliver(event)
                deliveredCount++
            } catch (e: Exception) {
                // EventHandler.md: a throwing handler MUST be isolated, never propagated, never block other subscribers.
                failedSubscriptionIds += subscription.subscriptionId
            }
        }

        return if (failedSubscriptionIds.isEmpty()) {
            PublishResult.Delivered(deliveredCount)
        } else {
            PublishResult.PartialFailure(deliveredCount, failedSubscriptionIds)
        }
    }

    override fun subscribe(eventType: EventType, handler: EventHandler): Subscription {
        val list = subscriptionsByType.computeIfAbsent(eventType) { CopyOnWriteArrayList() }
        val subscription = SubscriptionImpl(
            subscriptionId = UUID.randomUUID().toString(),
            eventType = eventType,
            // No IdentityService to resolve "the calling Principal" from context yet; subscriber identity
            // is not asserted by this bus -- callers requiring subscriber-Principal-scoped bookkeeping
            // (cascading cancellation on Revoke) must supply it themselves. Recorded in IMPLEMENTATION_GAPS.md.
            subscriberPrincipalId = PrincipalId("system.event-bus.unresolved-subscriber"),
            handler = handler,
            onCancel = { list.remove(it) },
        )
        list.add(subscription)
        return subscription
    }
}
