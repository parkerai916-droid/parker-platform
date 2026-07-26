package parker.core.runtime

import parker.core.interfaces.ConversationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse

/**
 * Sequences [CommunicationConversationCoordinator], [GoalPlanningHandoffCoordinator],
 * and [ReplyDeliveryCoordinator], per
 * `docs/implementation/CONVERSATION_REPLY_COORDINATOR_SCOPE_LOCK.md`
 * (Stage 5 Scope Lock), the Plan it freezes,
 * `docs/implementation/CONVERSATION_REPLY_COORDINATOR_IMPLEMENTATION_PLAN.md`,
 * and, as of the Reasoning-to-Planning Handoff,
 * `docs/implementation/REASONING_TO_PLANNING_HANDOFF_SCOPE_LOCK.md`.
 *
 * **Orchestration only** -- never reasons, plans, authorises, formats,
 * constructs or mutates an [parker.core.interfaces.OutboundParkerResponse],
 * delivers, invokes `ExecutionPipeline`/`PermissionEngine`/`PlannerRuntime`
 * directly, selects or configures a model provider, or creates a
 * production composition root. Holds exactly the three dependencies
 * below and nothing else -- there is nothing reachable through which
 * this class could do any of those things. Communication intake remains
 * upstream, inside [CommunicationConversationCoordinator]'s own
 * `CommunicationIntake` dependency; conversation reasoning remains
 * inside [CommunicationConversationCoordinator] itself; reply
 * composition and delivery remain inside [ReplyDeliveryCoordinator]
 * itself; `PlanningRequest` construction and planning-deferral reporting
 * remain inside [GoalPlanningHandoffCoordinator] itself. This class only
 * sequences the three.
 *
 * **Revised Sprint 11 Unit 5 (Conversation Continuity Implementation):**
 * [submitAndDeliver] gains one additive, pass-through [ConversationId]
 * parameter, forwarded unchanged into
 * [CommunicationConversationCoordinator.submitAndReason] --
 * `docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` Section
 * 5's own propagation path. This class does not inspect continuity
 * policy, does not generate or resolve any identifier, and does not
 * mutate the one it is given.
 *
 * **Revised, Reasoning-to-Planning Handoff.** [submitAndDeliver] now
 * inspects the [ReasoningProviderResponse] `communicationConversationCoordinator.submitAndReason`
 * produces, before ever calling [replyDeliveryCoordinator] -- the exact
 * interception point
 * `docs/architecture/REASONING_TO_PLANNING_HANDOFF_GOVERNANCE_REVIEW.md`
 * Section 5.5 identified: this class already held the raw response, one
 * level before `ResponseComposer`. A [ReasoningProviderResponse.Goal] is
 * routed to [goalPlanningHandoffCoordinator] and never reaches
 * [replyDeliveryCoordinator] or `ResponseComposer` at all. A `Reply` or
 * `NoAction` follows the exact existing path, unchanged --
 * [replyDeliveryCoordinator]`.composeAndDeliver` is called exactly as it
 * was before this revision, and `ResponseComposer`/[ReplyDeliveryCoordinator]
 * are not modified by this revision in any way.
 *
 * **Return type revised from `GatedOutcome<ExecutionResult>` to
 * [ConversationOutcome]** (Contract Design Section 7.1, Scope Lock
 * Section 2.1) -- reusing `GatedOutcome.NotAccepted` for a deferred
 * `Goal` would misrepresent it as an ordinary rejection. `ConversationOutcome`
 * is flattened, not nested: [ReplyDeliveryCoordinator.composeAndDeliver]'s
 * own `GatedOutcome<ExecutionResult>` result is unwrapped one level here,
 * so a downstream rejection on the `Reply`/`NoAction` path (`ResponseComposer`
 * declining, or `ResponseDelivery` failing) collapses into
 * [ConversationOutcome.NotAccepted] exactly as it already did under the
 * prior return type -- no behavioural change on that path, only a
 * renamed wrapper.
 *
 * **Message forwarding (Scope Lock Section 13), disclosed, not
 * redesigned here.** [CommunicationConversationCoordinator.submitAndReason]'s
 * own return type does not expose `CommunicationIntake`'s own
 * accepted-disposition message back to its caller -- only the reasoning
 * outcome. This class therefore forwards its own [message] parameter,
 * unchanged, into both [ReplyDeliveryCoordinator.composeAndDeliver]'s
 * `originalMessage` argument and [GoalPlanningHandoffCoordinator.initiatePlanning]'s
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
 *   [ReasoningProviderResponse].
 * @param replyDeliveryCoordinator Used exactly once per call, only when
 *   the admitted response is `Reply` or `NoAction`, to compose and
 *   deliver.
 * @param goalPlanningHandoffCoordinator Used exactly once per call, only
 *   when the admitted response is [ReasoningProviderResponse.Goal], to
 *   construct a `PlanningRequest` and report planning deferral. This is
 *   the only new dependency this class accepts as of the
 *   Reasoning-to-Planning Handoff -- the absence of any other
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
    private val goalPlanningHandoffCoordinator: GoalPlanningHandoffCoordinator,
) {

    /**
     * Given an [InboundOwnerMessage] and an already-assembled
     * [ReasoningContext] (this class does not assemble one): submits and
     * reasons via [communicationConversationCoordinator], then branches
     * on the resulting [ReasoningProviderResponse]:
     * - Not accepted upstream: [ConversationOutcome.NotAccepted],
     *   unchanged, [replyDeliveryCoordinator] and
     *   [goalPlanningHandoffCoordinator] both never called.
     * - [ReasoningProviderResponse.Goal]: routed to
     *   [goalPlanningHandoffCoordinator]`.initiatePlanning`, wrapped in
     *   [ConversationOutcome.PlanningDeferred]. [replyDeliveryCoordinator]
     *   is never called on this branch.
     * - `Reply`/`NoAction`: routed to [replyDeliveryCoordinator]`.composeAndDeliver`,
     *   exactly as before this revision, unwrapped into
     *   [ConversationOutcome.ReplyDelivered] or [ConversationOutcome.NotAccepted].
     *   [goalPlanningHandoffCoordinator] is never called on this branch.
     *
     * This class never resolves identity, never constructs or mutates an
     * [parker.core.interfaces.OutboundParkerResponse], never constructs
     * or mutates a `PlanningRequest` itself, never retries, and never
     * recovers from an exception any dependency throws -- such an
     * exception propagates to this method's own caller unchanged.
     */
    suspend fun submitAndDeliver(
        message: InboundOwnerMessage,
        reasoningContext: ReasoningContext,
        conversationId: ConversationId,
    ): ConversationOutcome {
        val reasoned = communicationConversationCoordinator.submitAndReason(message, reasoningContext, conversationId)
        return when (reasoned) {
            is GatedOutcome.NotAccepted -> ConversationOutcome.NotAccepted(reasoned.reason)
            is GatedOutcome.Produced -> when (val response = reasoned.value) {
                is ReasoningProviderResponse.Goal ->
                    ConversationOutcome.PlanningDeferred(goalPlanningHandoffCoordinator.initiatePlanning(message, response))
                is ReasoningProviderResponse.Reply -> deliverReply(message, response)
                ReasoningProviderResponse.NoAction -> deliverReply(message, response)
            }
        }
    }

    /**
     * The unchanged `Reply`/`NoAction` path: [replyDeliveryCoordinator]`.composeAndDeliver`
     * called exactly as it was before the Reasoning-to-Planning Handoff,
     * its own `GatedOutcome<ExecutionResult>` result unwrapped one level
     * into [ConversationOutcome] (Contract Design Section 7.1) -- no
     * nested [GatedOutcome] wrapping.
     */
    private suspend fun deliverReply(message: InboundOwnerMessage, response: ReasoningProviderResponse): ConversationOutcome =
        when (val delivered = replyDeliveryCoordinator.composeAndDeliver(message, response)) {
            is GatedOutcome.NotAccepted -> ConversationOutcome.NotAccepted(delivered.reason)
            is GatedOutcome.Produced -> ConversationOutcome.ReplyDelivered(delivered.value)
        }
}
