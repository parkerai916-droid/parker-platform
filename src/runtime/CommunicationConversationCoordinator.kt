package parker.core.runtime

import parker.core.interfaces.CommunicationIntake
import parker.core.interfaces.CommunicationIntakeDisposition
import parker.core.interfaces.ConversationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse

/**
 * Sequences [CommunicationIntake.submitInboundMessage] and
 * [ConversationTurnReasoningCoordinator.submitTurnAndReason] (Sprint 7,
 * Unit C2 — Communication-to-Conversation Wiring), per
 * `docs/implementation/COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md`.
 *
 * ```
 * Owner Message
 *     v
 * CommunicationIntake.submitInboundMessage(...)
 *     v
 * ConversationTurnReasoningCoordinator.submitTurnAndReason(...)
 *     v
 * stop
 * ```
 *
 * If [CommunicationIntake] rejects the message, this coordinator stops
 * immediately and returns [GatedOutcome.NotAccepted] carrying the
 * rejection reason unchanged — [ConversationTurnReasoningCoordinator],
 * `ConversationEngine`, and `ReasoningProvider` are never reached. If it
 * accepts the message, this coordinator delegates to the existing
 * [ConversationTurnReasoningCoordinator] unchanged and returns whatever
 * [ReasoningProviderResponse] results, wrapped in [GatedOutcome.Produced].
 * **This is this Unit's stop condition:** nothing routes a `Goal` onward
 * to Planner Runtime, nothing routes a `Reply` onward to Response
 * Delivery, and nothing else happens (Plan Section 1).
 *
 * **Decision 2 (non-interface-backed).** This class is intentionally not
 * interface-backed, for the identical reason
 * [ConversationTurnReasoningCoordinator] itself is not: it sequences two
 * already-shaped components and introduces no new field, no new domain
 * concept beyond [GatedOutcome], and no state of its own. If a future
 * caller needs to depend on this coordinated behaviour abstractly, that
 * need must be met by a later Contract Design pass or an explicit,
 * disclosed additive-interface decision — not by silently promoting this
 * class into a public contract.
 *
 * **Revised Sprint 11 Unit 5 (Conversation Continuity Implementation):**
 * [submitAndReason] gains one additive, pass-through [ConversationId]
 * parameter, forwarded unchanged into
 * [ConversationTurnReasoningCoordinator.submitTurnAndReason] --
 * `docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` Section
 * 5's own propagation path. This class does not inspect continuity
 * policy, does not generate or resolve any identifier, and does not
 * mutate the one it is given -- it only threads it through.
 *
 * **Decision 3 (whole reuse, not re-derivation).** This class depends on
 * [ConversationTurnReasoningCoordinator] as a single, opaque dependency,
 * unchanged — never directly on `ConversationEngine` or
 * `ReasoningProvider`. It does not re-implement the `submitTurn` ->
 * `reason` sequence.
 *
 * **Statelessness invariant (Plan Section 4a).** This class holds
 * exactly its two constructor-injected dependencies as its only fields.
 * No `var`, no mutable collection, no cache of any prior
 * `Conversation`/`Turn`/`InboundOwnerMessage`/`ReasoningProviderResponse`,
 * and no `Mutex`. Each call to [submitAndReason] is fully independent of
 * every other call.
 *
 * **Message pass-through invariant (Plan Section 4b).** This class never
 * mutates or reinterprets an accepted [InboundOwnerMessage] -- it
 * sequences only. The message passed downstream is
 * `disposition.message` (the message [CommunicationIntake] itself
 * returned as accepted), never a separately-held reference to this
 * method's own [message] parameter, never a `.copy()`, and this class
 * never reads [InboundOwnerMessage.text] to make any decision of its
 * own.
 *
 * **Exception propagation invariant (Plan Section 4c).** This class must
 * not recover from, translate, retry, or suppress an exception thrown by
 * either dependency. It contains no `try`/`catch` of any kind. Such
 * failures propagate unchanged to the caller and are outside this
 * class's responsibility -- never converted into a [GatedOutcome.NotAccepted].
 *
 * **Exactly-once invocation invariant.** [submitAndReason] calls
 * [CommunicationIntake.submitInboundMessage] exactly once. On
 * acceptance, it calls
 * [ConversationTurnReasoningCoordinator.submitTurnAndReason] exactly
 * once. This holds unconditionally, regardless of which
 * [ReasoningProviderResponse] variant results -- `NoAction`, `Goal`, and
 * `Reply` are all treated identically for invocation-count purposes. No
 * retry, loop, or batching behaviour exists anywhere in this class.
 *
 * **Dependencies (structural, not merely asserted).** The only two
 * dependencies below are the entirety of this class's reach. There is no
 * slot for `PlannerRuntime`, `ExecutionPipeline`, `PermissionEngine`,
 * `ToolRegistry`, `MemoryStore`, `WorldModel`, `ModuleRegistry`, or
 * `IdentityService` to even construct this class with one.
 *
 * @param communicationIntake Used exactly once per call, to accept or
 *   reject an inbound message.
 * @param conversationTurnReasoningCoordinator Used exactly once per call,
 *   on acceptance only, reused whole and unchanged.
 */
class CommunicationConversationCoordinator(
    private val communicationIntake: CommunicationIntake,
    private val conversationTurnReasoningCoordinator: ConversationTurnReasoningCoordinator,
) {
    suspend fun submitAndReason(
        message: InboundOwnerMessage,
        reasoningContext: ReasoningContext,
        conversationId: ConversationId,
    ): GatedOutcome<ReasoningProviderResponse> {
        val disposition = communicationIntake.submitInboundMessage(message)

        return when (disposition) {
            is CommunicationIntakeDisposition.Accepted -> GatedOutcome.Produced(
                conversationTurnReasoningCoordinator.submitTurnAndReason(disposition.message, reasoningContext, conversationId),
            )
            is CommunicationIntakeDisposition.Rejected -> GatedOutcome.NotAccepted(disposition.reason)
        }
    }
}
