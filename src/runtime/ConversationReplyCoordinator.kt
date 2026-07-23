package parker.core.runtime

import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ReasoningContext

/**
 * Sequences [CommunicationConversationCoordinator] and
 * [ReplyDeliveryCoordinator], per
 * `docs/implementation/CONVERSATION_REPLY_COORDINATOR_SCOPE_LOCK.md`
 * (Stage 5 Scope Lock) and the Plan it freezes,
 * `docs/implementation/CONVERSATION_REPLY_COORDINATOR_IMPLEMENTATION_PLAN.md`.
 *
 * **Orchestration only** -- never reasons, plans, authorises, formats,
 * constructs or mutates an [parker.core.interfaces.OutboundParkerResponse],
 * delivers, invokes `ExecutionPipeline`/`PermissionEngine` directly,
 * selects or configures a model provider, or creates a production
 * composition root (Scope Lock Section 15). Holds exactly the two
 * dependencies below and nothing else -- there is nothing reachable
 * through which this class could do any of those things. Communication
 * intake remains upstream, inside
 * [CommunicationConversationCoordinator]'s own `CommunicationIntake`
 * dependency; conversation reasoning remains inside
 * [CommunicationConversationCoordinator] itself; reply composition and
 * delivery remain inside [ReplyDeliveryCoordinator] itself. This class
 * only sequences the two.
 *
 * **Sequencing (Scope Lock Section 10/Section 11).** [submitAndDeliver]
 * calls [CommunicationConversationCoordinator.submitAndReason] exactly
 * once, first. If the result is [GatedOutcome.NotAccepted], it is
 * returned unchanged -- [ReplyDeliveryCoordinator.composeAndDeliver] is
 * never called on that branch. If the result is [GatedOutcome.Produced],
 * [ReplyDeliveryCoordinator.composeAndDeliver] is called exactly once,
 * and its own result is returned unchanged (`Produced` or `NotAccepted`)
 * -- no additional wrapping, exploiting [GatedOutcome]'s declared
 * covariance so the `NotAccepted` branch type-checks without conversion.
 *
 * **Message forwarding (Scope Lock Section 13), disclosed, not
 * redesigned here.** [CommunicationConversationCoordinator.submitAndReason]'s
 * own return type does not expose `CommunicationIntake`'s own
 * accepted-disposition message back to its caller -- only the reasoning
 * outcome. This class therefore forwards its own [message] parameter,
 * unchanged, into [ReplyDeliveryCoordinator.composeAndDeliver]'s
 * `originalMessage` argument; it does not attempt to recover or
 * reconstruct `CommunicationIntake`'s own accepted message.
 * `CommunicationConversationCoordinator`'s own return type is not
 * redesigned by this class. The current, real `CommunicationIntake`
 * implementation (`InMemoryCommunicationIntake`) returns the identical
 * message reference it receives, so this has no observable effect
 * today.
 *
 * @param communicationConversationCoordinator Used exactly once per
 *   call, first, to obtain either a structural rejection or a
 *   [parker.core.interfaces.ReasoningProviderResponse].
 * @param replyDeliveryCoordinator Used exactly once per call, only on
 *   the admitted branch, to compose and deliver. This is the only other
 *   dependency this class accepts -- the absence of any other
 *   constructor parameter is itself the structural guarantee that this
 *   class cannot reach `CommunicationIntake`,
 *   `ConversationTurnReasoningCoordinator`, `ConversationEngine`,
 *   `ReasoningProvider`, `ResponseComposer`, `ResponseDelivery`,
 *   `IdentityService`, `ExecutionPipeline`, `PermissionEngine`,
 *   `PlannerRuntime`, `ModelReasoningProvider`,
 *   `LocalHttpModelInferenceClient`, `MemoryStore`, or `WorldModel`,
 *   directly.
 */
class ConversationReplyCoordinator(
    private val communicationConversationCoordinator: CommunicationConversationCoordinator,
    private val replyDeliveryCoordinator: ReplyDeliveryCoordinator,
) {

    /**
     * Given an [InboundOwnerMessage] and an already-assembled
     * [ReasoningContext] (this class does not assemble one -- Scope
     * Lock Section 15): submits and reasons via
     * [communicationConversationCoordinator], then -- only on
     * acceptance and successful composition -- delivers via
     * [replyDeliveryCoordinator]. See class KDoc for the full sequencing
     * rule. This class never resolves identity, never constructs or
     * mutates an [parker.core.interfaces.OutboundParkerResponse], never
     * retries, and never recovers from an exception either dependency
     * throws -- such an exception propagates to this method's own
     * caller unchanged.
     */
    suspend fun submitAndDeliver(
        message: InboundOwnerMessage,
        reasoningContext: ReasoningContext,
    ): GatedOutcome<ExecutionResult> {
        val reasoned = communicationConversationCoordinator.submitAndReason(message, reasoningContext)
        return when (reasoned) {
            is GatedOutcome.NotAccepted -> reasoned
            is GatedOutcome.Produced -> replyDeliveryCoordinator.composeAndDeliver(message, reasoned.value)
        }
    }
}
