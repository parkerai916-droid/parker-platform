package parker.core.runtime

import parker.core.interfaces.ExecutionResult

/**
 * [ConversationReplyCoordinator.submitAndDeliver]'s own return type, per
 * `docs/architecture/REASONING_TO_PLANNING_HANDOFF_CONTRACT_DESIGN.md`
 * Section 7.1 and
 * `docs/implementation/REASONING_TO_PLANNING_HANDOFF_SCOPE_LOCK.md`
 * Section 2.1. Replaces `GatedOutcome<ExecutionResult>` as that method's
 * return type -- reusing [GatedOutcome.NotAccepted] for a deferred
 * `Goal` would misrepresent it as an ordinary rejection, which the
 * Reasoning-to-Planning Handoff's own Mandatory Constraint expressly
 * forbids.
 *
 * A small, generic-shaped, purely additive sealed type, following
 * [GatedOutcome]'s own explicit precedent of being "a generic
 * implementation-level utility, not a domain contract" -- not a Volume
 * 1-3 governed contract.
 *
 * **Flattened, exactly three variants -- no nested [GatedOutcome]
 * wrapping is permitted (Scope Lock Section 2.1).** A downstream
 * rejection on the `Reply`/`NoAction` path (`ResponseComposer` declining,
 * or `ResponseDelivery` failing) collapses into [NotAccepted] exactly as
 * it already does today under the pre-existing
 * `GatedOutcome<ExecutionResult>` shape -- no behavioural change on that
 * path, only a renamed wrapper.
 */
sealed class ConversationOutcome {

    /** A `Reply`/`NoAction` Turn composed and delivered successfully; [executionResult] is `ResponseDelivery`'s own, unchanged. */
    data class ReplyDelivered(val executionResult: ExecutionResult) : ConversationOutcome()

    /**
     * A `Goal` Turn was intercepted before `ResponseComposer`
     * (Governance Review Section 5.5) and handed to
     * [GoalPlanningHandoffCoordinator.initiatePlanning]; [outcome] is its
     * own, unchanged result. **Never converted into a rejection, a
     * delivery success, an ordinary planning failure, or a silent
     * discard** -- it remains observable, unchanged, all the way to
     * `parker.composition.ParkerRuntime.submitOwnerMessage`'s own caller
     * via `parker.composition.ParkerRuntimeOutcome.PlanningDeferred`.
     */
    data class PlanningDeferred(val outcome: GoalPlanningHandoffOutcome) : ConversationOutcome()

    /** An upstream admission gate rejected the message, or a downstream `Reply`/`NoAction` step declined -- [reason] is the rejecting component's own, unchanged. */
    data class NotAccepted(val reason: String) : ConversationOutcome()
}
