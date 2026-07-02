package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EventType
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PublishResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Proves the behaviours EventBus.md and its four supporting-type
 * documents require: publish/subscribe, cancellation, PublishResult
 * variants, authentication hooks, and trust-sensitive signature
 * requirements.
 */
class InMemoryEventBusTest {

    private fun event(
        eventType: String = "resource.updated",
        signature: String? = null,
        publisher: String = "user-1",
    ) = ParkerEvent(
        eventId = "evt-1",
        publisherPrincipalId = PrincipalId(publisher),
        eventType = EventType(eventType),
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = "corr-1",
        payload = mapOf("key" to "value"),
        signature = signature,
    )

    @Test
    fun `EventType rejects a blank or non-namespaced value`() {
        assertFailsWith<IllegalArgumentException> { EventType("") }
        assertFailsWith<IllegalArgumentException> { EventType("noDotHere") }
        EventType("resource.updated") // does not throw
        EventType("plugin:irrigation.cycle_started") // does not throw
    }

    @Test
    fun `publishing with no subscribers is Delivered with zero count, not an error`() = runTest {
        val bus = InMemoryEventBus()
        val result = bus.publish(event())
        assertEquals(PublishResult.Delivered(0), result)
    }

    @Test
    fun `a subscriber receives an event published on its exact eventType`() = runTest {
        val bus = InMemoryEventBus()
        var received: ParkerEvent? = null
        bus.subscribe(EventType("resource.updated"), PrincipalId("subscriber-1")) { received = it }

        val result = bus.publish(event())
        assertEquals(PublishResult.Delivered(1), result)
        assertEquals("evt-1", received?.eventId)
    }

    @Test
    fun `a subscriber does not receive events of a different eventType`() = runTest {
        val bus = InMemoryEventBus()
        var receivedCount = 0
        bus.subscribe(EventType("resource.updated"), PrincipalId("subscriber-1")) { receivedCount++ }

        bus.publish(event(eventType = "session.expired"))
        assertEquals(0, receivedCount)
    }

    @Test
    fun `multiple subscribers to the same eventType all receive the event`() = runTest {
        val bus = InMemoryEventBus()
        var countA = 0
        var countB = 0
        bus.subscribe(EventType("resource.updated"), PrincipalId("subscriber-a")) { countA++ }
        bus.subscribe(EventType("resource.updated"), PrincipalId("subscriber-b")) { countB++ }

        val result = bus.publish(event())
        assertEquals(PublishResult.Delivered(2), result)
        assertEquals(1, countA)
        assertEquals(1, countB)
    }

    @Test
    fun `a throwing handler is isolated -- other subscribers still receive, publish still returns`() = runTest {
        val bus = InMemoryEventBus()
        var goodHandlerRan = false
        bus.subscribe(EventType("resource.updated"), PrincipalId("subscriber-bad")) { throw RuntimeException("boom") }
        bus.subscribe(EventType("resource.updated"), PrincipalId("subscriber-good")) { goodHandlerRan = true }

        val result = bus.publish(event())
        assertTrue(goodHandlerRan)
        val partial = assertIs<PublishResult.PartialFailure>(result)
        assertEquals(1, partial.deliveredCount)
        assertEquals(1, partial.failedSubscriptionIds.size)
    }

    @Test
    fun `cancelling a subscription stops further delivery`() = runTest {
        val bus = InMemoryEventBus()
        var count = 0
        val subscription = bus.subscribe(EventType("resource.updated"), PrincipalId("subscriber-1")) { count++ }

        bus.publish(event())
        subscription.cancel()
        bus.publish(event())

        assertEquals(1, count)
        assertEquals(false, subscription.active)
    }

    @Test
    fun `cancelling an already-cancelled subscription is idempotent, not an error`() = runTest {
        val bus = InMemoryEventBus()
        val subscription = bus.subscribe(EventType("resource.updated"), PrincipalId("subscriber-1")) { }

        subscription.cancel()
        subscription.cancel() // must not throw
        assertEquals(false, subscription.active)
    }

    @Test
    fun `publish is rejected when the publisher Principal is not in good standing`() = runTest {
        val bus = InMemoryEventBus(authenticator = PrincipalAuthenticator { false })
        val result = bus.publish(event())
        assertIs<PublishResult.Rejected>(result)
    }

    @Test
    fun `trust-sensitive event types require a non-blank signature`() = runTest {
        val bus = InMemoryEventBus()

        val unsigned = bus.publish(event(eventType = "execution.completed", signature = null))
        assertIs<PublishResult.Rejected>(unsigned)

        val signed = bus.publish(event(eventType = "execution.completed", signature = "sig-abc"))
        assertEquals(PublishResult.Delivered(0), signed)
    }

    @Test
    fun `non-trust-sensitive event types do not require a signature`() = runTest {
        val bus = InMemoryEventBus()
        val result = bus.publish(event(eventType = "resource.updated", signature = null))
        assertEquals(PublishResult.Delivered(0), result)
    }
}
