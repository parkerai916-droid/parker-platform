package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CommunicationIntake
import parker.core.interfaces.CommunicationIntakeDisposition
import parker.core.interfaces.Conversation
import parker.core.interfaces.ConversationDisposition
import parker.core.interfaces.ConversationEngine
import parker.core.interfaces.ConversationId
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

/**
 * Sprint 7, Unit C2 (Communication-to-Conversation Wiring) acceptance
 * test, per
 * `docs/implementation/COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md`
 * Section 6, covering [CommunicationConversationCoordinator] in
 * isolation. [CommunicationIntake] is reused through
 * [FakeCommunicationIntake]; [ConversationTurnReasoningCoordinator] is
 * real and unchanged, wired to a pass-through fake `ConversationEngine`
 * and [FakeReasoningProvider], so neither
 * [InMemoryCommunicationIntake]'s nor [InMemoryConversationEngine]'s own
 * internal logic is exercised here.
 */
class CommunicationConversationCoordinatorTest {

    private fun message(
        text: String = "hello",
        correlationId: String = "corr-1",
        senderPrincipalId: String = "user-1",
    ) = InboundOwnerMessage(
        channelId = ModuleId("channel.local-text"),
        senderPrincipalId = PrincipalId(senderPrincipalId),
        text = text,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId(correlationId),
    )

    /**
     * A `ConversationEngine` fake that wraps whatever [InboundOwnerMessage]
     * it is given into a `Turn` unchanged -- mirrors
     * [InMemoryConversationEngine]'s own message pass-through behaviour,
     * without exercising its identity-resolution logic, so tests here stay
     * isolated to [CommunicationConversationCoordinator]'s own behaviour.
     */
    private fun passThroughConversationEngine() = object : ConversationEngine {
        override suspend fun submitTurn(message: InboundOwnerMessage): ConversationDisposition {
            val conversationId = ConversationId("conv-1")
            val turnId = TurnId("turn-1")
            return ConversationDisposition(
                conversation = Conversation(
                    conversationId = conversationId,
                    ownerPrincipalId = message.senderPrincipalId,
                    channelId = message.channelId,
                    turnIds = listOf(turnId),
                ),
                turn = Turn(
                    turnId = turnId,
                    conversationId = conversationId,
                    message = message,
                    receivedAt = Instant.parse("2026-01-01T00:00:00Z"),
                ),
                isNewConversation = true,
            )
        }
    }

    // ================= Accepted path =================

    @Test
    fun `an accepted message reaches reasoning and returns Produced wrapping a Goal unchanged`() = runTest {
        val communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Accepted(msg.correlationId, msg) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Goal("book a flight") }
        val downstream = ConversationTurnReasoningCoordinator(passThroughConversationEngine(), reasoningProvider)
        val coordinator = CommunicationConversationCoordinator(communicationIntake, downstream)
        val context = ReasoningContext(listOf("prior context"))

        val outcome = coordinator.submitAndReason(message(), context)

        val produced = assertIs<GatedOutcome.Produced<ReasoningProviderResponse>>(outcome)
        val goal = assertIs<ReasoningProviderResponse.Goal>(produced.value)
        assertEquals("book a flight", goal.text)
        assertEquals(1, communicationIntake.submitInboundMessageCallCount)
        assertEquals(1, reasoningProvider.reasonCallCount)
        assertEquals(context, reasoningProvider.lastRequest?.reasoningContext)
    }

    @Test
    fun `an accepted message returns Produced wrapping a Reply unchanged`() = runTest {
        val communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Accepted(msg.correlationId, msg) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("sure, on it") }
        val downstream = ConversationTurnReasoningCoordinator(passThroughConversationEngine(), reasoningProvider)
        val coordinator = CommunicationConversationCoordinator(communicationIntake, downstream)

        val outcome = coordinator.submitAndReason(message(), ReasoningContext(emptyList()))

        val produced = assertIs<GatedOutcome.Produced<ReasoningProviderResponse>>(outcome)
        val reply = assertIs<ReasoningProviderResponse.Reply>(produced.value)
        assertEquals("sure, on it", reply.text)
        assertEquals(1, reasoningProvider.reasonCallCount)
    }

    @Test
    fun `an accepted message returns Produced wrapping NoAction unchanged`() = runTest {
        val communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Accepted(msg.correlationId, msg) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val downstream = ConversationTurnReasoningCoordinator(passThroughConversationEngine(), reasoningProvider)
        val coordinator = CommunicationConversationCoordinator(communicationIntake, downstream)

        val outcome = coordinator.submitAndReason(message(), ReasoningContext(emptyList()))

        val produced = assertIs<GatedOutcome.Produced<ReasoningProviderResponse>>(outcome)
        assertIs<ReasoningProviderResponse.NoAction>(produced.value)
        assertEquals(1, reasoningProvider.reasonCallCount)
    }

    // ================= Rejected path =================

    @Test
    fun `a rejected message returns NotAccepted with the rejection reason, and never reaches reasoning`() = runTest {
        val communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Rejected(msg.correlationId, "channel not enabled") }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val downstream = ConversationTurnReasoningCoordinator(passThroughConversationEngine(), reasoningProvider)
        val coordinator = CommunicationConversationCoordinator(communicationIntake, downstream)

        val outcome = coordinator.submitAndReason(message(), ReasoningContext(emptyList()))

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertEquals("channel not enabled", notAccepted.reason)
        assertEquals(1, communicationIntake.submitInboundMessageCallCount)
        assertEquals(0, reasoningProvider.reasonCallCount)
    }

    // ================= Message pass-through invariant (Section 4b) =================

    @Test
    fun `the Turn built downstream wraps disposition-message, not the coordinator's own input parameter`() = runTest {
        val originalMessage = message(text = "original text", correlationId = "corr-original")
        val acceptedMessage = message(text = "accepted text", correlationId = "corr-accepted")
        val communicationIntake = FakeCommunicationIntake { CommunicationIntakeDisposition.Accepted(acceptedMessage.correlationId, acceptedMessage) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val downstream = ConversationTurnReasoningCoordinator(passThroughConversationEngine(), reasoningProvider)
        val coordinator = CommunicationConversationCoordinator(communicationIntake, downstream)

        coordinator.submitAndReason(originalMessage, ReasoningContext(emptyList()))

        val turnMessage = reasoningProvider.lastRequest?.turn?.message
        assertEquals(acceptedMessage, turnMessage)
        assertNotEquals(originalMessage, turnMessage)
    }

    // ================= Exactly-once invocation, unconditional across variants =================

    @Test
    fun `submitAndReason calls submitInboundMessage exactly once and reason exactly once for a single accepted call`() = runTest {
        val communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Accepted(msg.correlationId, msg) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val downstream = ConversationTurnReasoningCoordinator(passThroughConversationEngine(), reasoningProvider)
        val coordinator = CommunicationConversationCoordinator(communicationIntake, downstream)

        coordinator.submitAndReason(message(), ReasoningContext(emptyList()))

        assertEquals(1, communicationIntake.submitInboundMessageCallCount)
        assertEquals(1, reasoningProvider.reasonCallCount)
    }

    // ================= Exception propagation, not recovery (Section 4c) =================

    @Test
    fun `an exception thrown by CommunicationIntake propagates unchanged, and reasoning is never reached`() = runTest {
        val communicationIntake = FakeCommunicationIntake { throw IllegalStateException("communication boom") }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val downstream = ConversationTurnReasoningCoordinator(passThroughConversationEngine(), reasoningProvider)
        val coordinator = CommunicationConversationCoordinator(communicationIntake, downstream)

        assertFailsWith<IllegalStateException> {
            coordinator.submitAndReason(message(), ReasoningContext(emptyList()))
        }
        assertEquals(0, reasoningProvider.reasonCallCount)
    }

    @Test
    fun `an exception thrown by ReasoningProvider propagates unchanged`() = runTest {
        val communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Accepted(msg.correlationId, msg) }
        val reasoningProvider = FakeReasoningProvider { throw IllegalStateException("reasoning boom") }
        val downstream = ConversationTurnReasoningCoordinator(passThroughConversationEngine(), reasoningProvider)
        val coordinator = CommunicationConversationCoordinator(communicationIntake, downstream)

        assertFailsWith<IllegalStateException> {
            coordinator.submitAndReason(message(), ReasoningContext(emptyList()))
        }
    }

    // ================= Structural: no prohibited dependency slot =================

    @Test
    fun `the coordinator's constructor accepts exactly two dependencies -- CommunicationIntake and ConversationTurnReasoningCoordinator`() {
        val constructor = CommunicationConversationCoordinator::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("CommunicationIntake", "ConversationTurnReasoningCoordinator"), parameterTypes)
    }

    // ================= Statelessness (Section 4a) =================

    @Test
    fun `the coordinator declares no field beyond its two constructor-injected dependencies`() {
        val fieldNames = CommunicationConversationCoordinator::class.java.declaredFields.map { it.name }.toSet()

        assertEquals(setOf("communicationIntake", "conversationTurnReasoningCoordinator"), fieldNames)
    }

    @Test
    fun `two independent invocations for different owners never observably interact`() = runTest {
        val communicationIntake = FakeCommunicationIntake { msg -> CommunicationIntakeDisposition.Accepted(msg.correlationId, msg) }
        val reasoningProvider = FakeReasoningProvider { request ->
            if (request.turn.message.text == "first") {
                ReasoningProviderResponse.Reply("first reply")
            } else {
                ReasoningProviderResponse.Reply("second reply")
            }
        }
        val downstream = ConversationTurnReasoningCoordinator(passThroughConversationEngine(), reasoningProvider)
        val coordinator = CommunicationConversationCoordinator(communicationIntake, downstream)

        val firstOutcome = coordinator.submitAndReason(
            message(text = "first", correlationId = "corr-1", senderPrincipalId = "user-1"),
            ReasoningContext(emptyList()),
        )
        val secondOutcome = coordinator.submitAndReason(
            message(text = "second", correlationId = "corr-2", senderPrincipalId = "user-2"),
            ReasoningContext(emptyList()),
        )

        val firstReply = assertIs<ReasoningProviderResponse.Reply>(
            assertIs<GatedOutcome.Produced<ReasoningProviderResponse>>(firstOutcome).value,
        )
        val secondReply = assertIs<ReasoningProviderResponse.Reply>(
            assertIs<GatedOutcome.Produced<ReasoningProviderResponse>>(secondOutcome).value,
        )
        assertEquals("first reply", firstReply.text)
        assertEquals("second reply", secondReply.text)
        assertEquals(2, communicationIntake.submitInboundMessageCallCount)
        assertEquals(2, reasoningProvider.reasonCallCount)
    }

    // ================= GatedOutcome construction (shared, generic type) =================

    @Test
    fun `GatedOutcome-Produced holds its value unchanged`() {
        val produced = GatedOutcome.Produced("some value")

        assertEquals("some value", produced.value)
    }

    @Test
    fun `GatedOutcome-NotAccepted rejects a blank reason`() {
        assertFailsWith<IllegalArgumentException> {
            GatedOutcome.NotAccepted("   ")
        }
    }
}
