package parker.composition

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CommunicationIntakeDisposition
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.EventType
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import parker.core.interfaces.ConversationId
import parker.core.runtime.FakeCommunicationIntake
import parker.core.runtime.FakeReasoningProvider
import parker.core.runtime.InMemoryEventBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Sprint 10, Unit 4 acceptance test for the three logging seams
 * `ParkerRuntime` wires: [LoggingCommunicationIntake], [LoggingReasoningProvider],
 * [RuntimeEventLogger]. Reuses `tests/runtime`'s own existing
 * `FakeCommunicationIntake`/`FakeReasoningProvider` fixtures rather than
 * introducing new ones for a delegation-only decorator, per this
 * repository's own established "no new fake where an existing one already
 * fits" discipline.
 */
class LoggingDecoratorsTest {

    private val fixedTimestamp: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private fun message(text: String = "hello") = InboundOwnerMessage(
        channelId = ModuleId("channel.test"),
        senderPrincipalId = PrincipalId("user-1"),
        text = text,
        timestamp = fixedTimestamp,
        correlationId = CorrelationId("corr-1"),
    )

    // ================= LoggingCommunicationIntake =================

    @Test
    fun `LoggingCommunicationIntake delegates and returns the delegate's disposition unchanged, logging Conversation accepted on Accepted`() = runTest {
        val theMessage = message()
        val delegate = FakeCommunicationIntake {
            CommunicationIntakeDisposition.Accepted(correlationId = it.correlationId, message = it)
        }
        val logger = RecordingParkerLogger()
        val decorated = LoggingCommunicationIntake(delegate, logger)

        val disposition = decorated.submitInboundMessage(theMessage)

        assertTrue(disposition is CommunicationIntakeDisposition.Accepted)
        assertEquals(1, delegate.submitInboundMessageCallCount)
        assertTrue(logger.hasMessageContaining("Conversation accepted"))
        assertTrue(logger.hasMessageContaining("corr-1"))
    }

    @Test
    fun `LoggingCommunicationIntake logs Conversation rejected on Rejected, without altering the reason`() = runTest {
        val theMessage = message()
        val delegate = FakeCommunicationIntake {
            CommunicationIntakeDisposition.Rejected(correlationId = it.correlationId, reason = "channel disabled")
        }
        val logger = RecordingParkerLogger()
        val decorated = LoggingCommunicationIntake(delegate, logger)

        val disposition = decorated.submitInboundMessage(theMessage) as CommunicationIntakeDisposition.Rejected

        assertEquals("channel disabled", disposition.reason)
        assertTrue(logger.hasMessageContaining("Conversation rejected"))
        assertTrue(logger.hasMessageContaining("channel disabled"))
    }

    @Test
    fun `LoggingCommunicationIntake never logs InboundOwnerMessage-text`() = runTest {
        val theMessage = message(text = "a very secret owner message")
        val delegate = FakeCommunicationIntake {
            CommunicationIntakeDisposition.Accepted(correlationId = it.correlationId, message = it)
        }
        val logger = RecordingParkerLogger()
        val decorated = LoggingCommunicationIntake(delegate, logger)

        decorated.submitInboundMessage(theMessage)

        assertFalse(logger.hasMessageContaining("a very secret owner message"))
    }

    // ================= LoggingReasoningProvider =================

    private fun turnRequest(): ReasoningProviderRequest = ReasoningProviderRequest(
        turn = Turn(
            turnId = TurnId("turn-1"),
            conversationId = ConversationId("conv-1"),
            message = message(),
            receivedAt = fixedTimestamp,
        ),
        reasoningContext = ReasoningContext(emptyList()),
    )

    @Test
    fun `LoggingReasoningProvider delegates and returns the response unchanged, logging outcome=Reply`() = runTest {
        val delegate = FakeReasoningProvider { ReasoningProviderResponse.Reply("hi there") }
        val logger = RecordingParkerLogger()
        val decorated = LoggingReasoningProvider(delegate, logger)

        val response = decorated.reason(turnRequest())

        assertEquals(ReasoningProviderResponse.Reply("hi there"), response)
        assertEquals(1, delegate.reasonCallCount)
        assertTrue(logger.hasMessageContaining("Reasoning completed"))
        assertTrue(logger.hasMessageContaining("outcome=Reply"))
        assertFalse(logger.hasMessageContaining("hi there"))
    }

    @Test
    fun `LoggingReasoningProvider logs outcome=Goal and outcome=NoAction for the other two variants`() = runTest {
        val logger = RecordingParkerLogger()

        LoggingReasoningProvider(FakeReasoningProvider { ReasoningProviderResponse.Goal("do something") }, logger)
            .reason(turnRequest())
        LoggingReasoningProvider(FakeReasoningProvider { ReasoningProviderResponse.NoAction }, logger)
            .reason(turnRequest())

        assertTrue(logger.hasMessageContaining("outcome=Goal"))
        assertTrue(logger.hasMessageContaining("outcome=NoAction"))
    }

    @Test
    fun `LoggingReasoningProvider propagates an exception unchanged and does not log it as if it were a normal completion`() = runTest {
        val delegate = FakeReasoningProvider { throw IllegalStateException("model boom") }
        val logger = RecordingParkerLogger()
        val decorated = LoggingReasoningProvider(delegate, logger)

        assertFailsWith<IllegalStateException> { decorated.reason(turnRequest()) }
        assertFalse(logger.hasMessageContaining("Reasoning completed"))
    }

    // ================= RuntimeEventLogger =================

    @Test
    fun `RuntimeEventLogger logs Execution authorised on permission-granted and Execution denied on permission-denied`() = runTest {
        val eventBus = InMemoryEventBus()
        val logger = RecordingParkerLogger()
        val runtimeEventLogger = RuntimeEventLogger(eventBus, logger, PrincipalId("system.parker"))
        runtimeEventLogger.start()

        eventBus.publish(
            ParkerEvent(
                eventId = "evt-1",
                publisherPrincipalId = PrincipalId("system.response-composer"),
                eventType = EventType("permission.granted"),
                timestamp = fixedTimestamp,
                correlationId = "corr-1",
                payload = emptyMap(),
                signature = "test-signature",
            ),
        )
        eventBus.publish(
            ParkerEvent(
                eventId = "evt-2",
                publisherPrincipalId = PrincipalId("system.response-composer"),
                eventType = EventType("permission.denied"),
                timestamp = fixedTimestamp,
                correlationId = "corr-2",
                payload = emptyMap(),
                signature = "test-signature",
            ),
        )

        assertTrue(logger.hasMessageContaining("Execution authorised"))
        assertTrue(logger.hasMessageContaining("corr-1"))
        assertTrue(logger.hasMessageContaining("Execution denied"))
        assertTrue(logger.hasMessageContaining("corr-2"))
    }

    @Test
    fun `RuntimeEventLogger logs Reply delivered on execution-completed`() = runTest {
        val eventBus = InMemoryEventBus()
        val logger = RecordingParkerLogger()
        val runtimeEventLogger = RuntimeEventLogger(eventBus, logger, PrincipalId("system.parker"))
        runtimeEventLogger.start()

        eventBus.publish(
            ParkerEvent(
                eventId = "evt-3",
                publisherPrincipalId = PrincipalId("system.response-composer"),
                eventType = EventType("execution.completed"),
                timestamp = fixedTimestamp,
                correlationId = "corr-3",
                payload = emptyMap(),
                signature = "test-signature",
            ),
        )

        assertTrue(logger.hasMessageContaining("Reply delivered"))
    }

    @Test
    fun `RuntimeEventLogger-stop cancels every subscription, so a subsequent event is no longer logged`() = runTest {
        val eventBus = InMemoryEventBus()
        val logger = RecordingParkerLogger()
        val runtimeEventLogger = RuntimeEventLogger(eventBus, logger, PrincipalId("system.parker"))
        runtimeEventLogger.start()
        runtimeEventLogger.stop()

        eventBus.publish(
            ParkerEvent(
                eventId = "evt-4",
                publisherPrincipalId = PrincipalId("system.response-composer"),
                eventType = EventType("permission.granted"),
                timestamp = fixedTimestamp,
                correlationId = "corr-4",
                payload = emptyMap(),
                signature = "test-signature",
            ),
        )

        assertFalse(logger.hasMessageContaining("Execution authorised"))
    }
}
