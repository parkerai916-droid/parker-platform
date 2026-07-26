package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ConversationDisposition
import parker.core.interfaces.ConversationId
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Sprint 7, Stage 3 acceptance test, revised Sprint 11 Unit 5
 * (Conversation Continuity Implementation): [ConversationTurnReasoningCoordinator.submitTurnAndReason]
 * gains one additive, pass-through [ConversationId] parameter, forwarded
 * unchanged into [ConversationEngine.submitTurn]
 * (`docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` Section
 * 5). Covers [ConversationTurnReasoningCoordinator] in isolation, using
 * [FakeCommunicationIntake]-style fakes for both of its dependencies so
 * neither [InMemoryConversationEngine]'s nor any reasoning provider's own
 * internal logic is exercised here.
 */
class ConversationTurnReasoningCoordinatorTest {

    private val fixedConversationId = parker.core.interfaces.ConversationId("conv-1")

    private fun message(correlationId: String = "corr-1") = InboundOwnerMessage(
        channelId = ModuleId("channel.local-text"),
        senderPrincipalId = PrincipalId("user-1"),
        text = "hello",
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId(correlationId),
    )

    /**
     * A minimal [parker.core.interfaces.ConversationEngine] fake returning a
     * fixed disposition for any Turn submission, and recording the exact
     * [ConversationId] it was called with -- so tests here can assert the
     * coordinator forwards the value it is given, never a different one.
     */
    private fun conversationEngineReturning(disposition: ConversationDisposition) =
        object : parker.core.interfaces.ConversationEngine {
            var lastSubmitTurnConversationId: ConversationId? = null
                private set

            override suspend fun resolveConversationId(message: InboundOwnerMessage): ConversationId =
                throw UnsupportedOperationException(
                    "resolveConversationId must never be called by ConversationTurnReasoningCoordinator -- " +
                        "it consumes an already-resolved identity, it never resolves one itself",
                )

            override suspend fun submitTurn(message: InboundOwnerMessage, conversationId: ConversationId): ConversationDisposition {
                lastSubmitTurnConversationId = conversationId
                return disposition
            }
        }

    private fun disposition(message: InboundOwnerMessage) = ConversationDisposition(
        conversation = parker.core.interfaces.Conversation(
            conversationId = fixedConversationId,
            ownerPrincipalId = message.senderPrincipalId,
            channelId = message.channelId,
            turnIds = listOf(parker.core.interfaces.TurnId("turn-1")),
        ),
        turn = parker.core.interfaces.Turn(
            turnId = parker.core.interfaces.TurnId("turn-1"),
            conversationId = fixedConversationId,
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

        val response = coordinator.submitTurnAndReason(inbound, context, fixedConversationId)

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

        val response = coordinator.submitTurnAndReason(inbound, ReasoningContext(emptyList()), fixedConversationId)

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

        val response = coordinator.submitTurnAndReason(inbound, ReasoningContext(emptyList()), fixedConversationId)

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

        coordinator.submitTurnAndReason(inbound, ReasoningContext(emptyList()), fixedConversationId)

        assertEquals(expectedDisposition.turn.turnId, reasoningProvider.lastRequest?.turn?.turnId)
        assertEquals(expectedDisposition.turn.conversationId, reasoningProvider.lastRequest?.turn?.conversationId)
    }

    // --- Sprint 11 Unit 5: the exact supplied ConversationId reaches submitTurn, never re-resolved ---

    @Test
    fun `the ConversationId supplied to submitTurnAndReason is forwarded unchanged to ConversationEngine_submitTurn, and resolveConversationId is never called`() = runTest {
        val inbound = message()
        val conversationEngine = conversationEngineReturning(disposition(inbound))
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val coordinator = ConversationTurnReasoningCoordinator(conversationEngine, reasoningProvider)

        coordinator.submitTurnAndReason(inbound, ReasoningContext(emptyList()), fixedConversationId)

        assertEquals(fixedConversationId, conversationEngine.lastSubmitTurnConversationId)
        // resolveConversationId throws UnsupportedOperationException if ever called -- reaching
        // this assertion at all already proves it was not.
    }

    // --- Sprint 11 Unit 5, Guarantee 4: submission rejection stops the pipeline ---

    @Test
    fun `when ConversationEngine_submitTurn rejects the supplied ConversationId, reasoning is never reached`() = runTest {
        val inbound = message()
        val rejectingConversationEngine = object : parker.core.interfaces.ConversationEngine {
            override suspend fun resolveConversationId(message: InboundOwnerMessage): ConversationId =
                throw UnsupportedOperationException("not exercised by this test")

            override suspend fun submitTurn(message: InboundOwnerMessage, conversationId: ConversationId): ConversationDisposition =
                throw IllegalArgumentException("simulated rejection of an unknown or stale ConversationId")
        }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val coordinator = ConversationTurnReasoningCoordinator(rejectingConversationEngine, reasoningProvider)

        assertFailsWith<IllegalArgumentException> {
            coordinator.submitTurnAndReason(inbound, ReasoningContext(emptyList()), fixedConversationId)
        }

        assertEquals(0, reasoningProvider.reasonCallCount)
        // No Turn is retained anywhere observable: submitTurn threw before returning a
        // ConversationDisposition, so no Turn object was ever constructed by this call.
    }

    // --- structural: no prohibited dependency slot exists ---

    @Test
    fun `the coordinator's constructor accepts exactly two dependencies -- ConversationEngine and ReasoningProvider`() {
        val constructor = ConversationTurnReasoningCoordinator::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("ConversationEngine", "ReasoningProvider"), parameterTypes)
    }
}
