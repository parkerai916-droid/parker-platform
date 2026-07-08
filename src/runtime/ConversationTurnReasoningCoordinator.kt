package parker.core.runtime

import parker.core.interfaces.ConversationEngine
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProvider
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse

/**
 * Sequences [ConversationEngine.submitTurn] followed by [ReasoningProvider.reason]
 * for Sprint 7, Stage 3 Implementation Unit, per
 * `docs/implementation/CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md`.
 *
 * **Required Implementation Decision 2** (accepted at Scope Lock): this
 * coordinator is intentionally concrete and **not interface-backed**. It
 * introduces no new public contract type -- no new data field, no new
 * domain concept -- and is therefore ordinary Stage 3 implementation-level
 * wiring between two already-approved contracts, not a new architectural
 * boundary. If a future caller needs to depend on this coordinated
 * behaviour abstractly, that will require a later Contract Design pass or
 * an explicit additive public interface decision -- not silent promotion
 * of this class.
 *
 * **Unit stop condition.** This coordinator stops after obtaining a
 * [ReasoningProviderResponse] and returns it unchanged. It does not invoke
 * `PlannerRuntime`, construct a `PlanningRequest`, invoke Response
 * Delivery, construct an `OutboundParkerResponse`, write to Memory, write
 * to the World Model, or invoke `ExecutionPipeline`.
 *
 * @param conversationEngine Used to bind the inbound message to a Turn.
 * @param reasoningProvider Used to reason about the resulting Turn.
 *   The absence of any other constructor parameter is itself the
 *   structural guarantee that this coordinator cannot reach
 *   `PlannerRuntime`, `ExecutionPipeline`, `MemoryStore`, or `WorldModel`.
 */
class ConversationTurnReasoningCoordinator(
    private val conversationEngine: ConversationEngine,
    private val reasoningProvider: ReasoningProvider,
) {
    suspend fun submitTurnAndReason(
        message: InboundOwnerMessage,
        reasoningContext: ReasoningContext,
    ): ReasoningProviderResponse {
        val disposition = conversationEngine.submitTurn(message)

        val request = ReasoningProviderRequest(
            turn = disposition.turn,
            reasoningContext = reasoningContext,
        )

        return reasoningProvider.reason(request)
    }
}
