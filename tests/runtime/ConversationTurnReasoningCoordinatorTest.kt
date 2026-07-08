package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ConversationDisposition
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Sprint 7, Stage 3 Implementation Unit acceptance test
 * (`docs/implementation/CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` §6),
 * covering [ConversationTurnReasoningCoordinator] in isolation, using
 * [FakeCommunicationIntake]-style fakes for both of its dependencies so
 * neither [InMemoryConversationEngine]'s nor any reasoning provider's own
 * internal logic is exercised here.
 */
class ConversationTurnReasoningCoordinatorTest {

    private fun message(correlationId: String = "corr-1") = InboundOwnerMessage(
        channelId = ModuleId("channel.local-text"),
        senderPrincipalId = PrincipalId("user-1"),
        text = "hello",
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId(correlationId),
    )

    /** A minimal, fixed [parker.core.interfaces.ConversationEngine] fake returning a fixed disposition for any message. */
    private fun conversationEngineReturning(disposition: ConversationDisposition) =
        object : parker.core.interfaces.ConversationEngine {
            override suspend fun submitTurn(message: InboundOwnerMessage): ConversationDisposition = disposition
        }

    private fun disposition(message: InboundOwnerMessage) = ConversationDisposition(
        conversation = parker.core.interfaces.Conversation(
            conversationId = parker.core.interfaces.ConversationId("conv-1"),
            ownerPrincipalId = message.senderPrincipalId,
            channelId = message.channelId,
            turnIds = listOf(parker.core.interfaces.TurnId("turn-1")),
        ),
        turn = parker.core.interfaces.Turn(
            turnId = parker.core.interfaces.TurnId("turn-1"),
            conversationId = parker.core.interfaces.ConversationId("conv-1"),
            message = message,
            receivedAt = Instant.parse("2026-01-01T00:00:00Z"),
        ),
        isNewConversation = true,
    )

    // --- pass-through: Goal ---

    @Test
    fun `submitTurnAndReason calls submitTurn then reason, returning a Goal response unchanged`() = runTest {
        val inbound = message()
        val expectedDisposition = disposition(inbound)
        val conversationEngine = conversationEngineReturning(expectedDisposition)
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Goal("book a flight") }
        val coordinator = ConversationTurnReasoningCoordinator(conversationEngine, reasoningProvider)
        val context = ReasoningContext(entries = listOf("prior context"))

        val response = coordinator.submitTurnAndReason(inbound, context)

        val goal = assertIs<ReasoningProviderResponse.Goal>(response)
        assertEquals("book a flight", goal.text)
        assertEquals(1, reasoningProvider.reasonCallCount)
        assertEquals(expectedDisposition.turn, reasoningProvider.lastRequest?.turn)
        assertEquals(context, reasoningProvider.lastRequest?.reasoningContext)
    }

    // --- pass-through: Reply ---

    @Test
    fun `submitTurnAndReason returns a Reply response unchanged`() = runTest {
        val inbound = message()
        val conversationEngine = conversationEngineReturning(disposition(inbound))
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("sure, on it") }
        val coordinator = ConversationTurnReasoningCoordinator(conversationEngine, reasoningProvider)

        val response = coordinator.submitTurnAndReason(inbound, ReasoningContext(emptyList()))

        val reply = assertIs<ReasoningProviderResponse.Reply>(response)
        assertEquals("sure, on it", reply.text)
    }

    // --- pass-through: NoAction ---

    @Test
    fun `submitTurnAndReason returns a NoAction response unchanged`() = runTest {
        val inbound = message()
        val conversationEngine = conversationEngineReturning(disposition(inbound))
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val coordinator = ConversationTurnReasoningCoordinator(conversationEngine, reasoningProvider)

        val response = coordinator.submitTurnAndReason(inbound, ReasoningContext(emptyList()))

        assertIs<ReasoningProviderResponse.NoAction>(response)
    }

    // --- sequencing: submitTurn's resulting Turn is what reason is called with ---

    @Test
    fun `the ReasoningProviderRequest passed to reason carries the Turn produced by submitTurn, not the raw message`() = runTest {
        val inbound = message(correlationId = "corr-specific")
        val expectedDisposition = disposition(inbound)
        val conversationEngine = conversationEngineReturning(expectedDisposition)
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val coordinator = ConversationTurnReasoningCoordinator(conversationEngine, reasoningProvider)

        coordinator.submitTurnAndReason(inbound, ReasoningContext(emptyList()))

        assertEquals(expectedDisposition.turn.turnId, reasoningProvider.lastRequest?.turn?.turnId)
        assertEquals(expectedDisposition.turn.conversationId, reasoningProvider.lastRequest?.turn?.conversationId)
    }

    // --- structural: no prohibited dependency slot exists ---

    @Test
    fun `the coordinator's constructor accepts exactly two dependencies -- ConversationEngine and ReasoningProvider`() {
        val constructor = ConversationTurnReasoningCoordinator::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("ConversationEngine", "ReasoningProvider"), parameterTypes)
    }
}
