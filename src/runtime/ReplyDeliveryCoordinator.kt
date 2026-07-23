package parker.core.runtime

import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ReasoningProviderResponse

/**
 * Sprint 10, Unit 2. Sequences [ResponseComposer] and [ResponseDelivery],
 * per `docs/implementation/REPLY_DELIVERY_COORDINATOR_SCOPE_LOCK.md`
 * (Stage 5 Scope Lock) and the Plan it freezes,
 * `docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`.
 *
 * **Orchestration only** -- never reasons, plans, authorises, constructs
 * an [parker.core.interfaces.OutboundParkerResponse], mutates a composed
 * response, or invokes `ExecutionPipeline`/`PermissionEngine` directly
 * (Scope Lock Section 9). Holds exactly the two dependencies below and
 * nothing else -- there is nothing reachable through which this class
 * could do any of those things. Composition (`ResponseComposer`),
 * delivery (`ResponseDelivery`), and execution (`ExecutionPipeline`)
 * each remain exactly one other component's job (Scope Lock Section 9).
 *
 * **Sequencing (Scope Lock Section 7/Section 8).** [composeAndDeliver]
 * calls [ResponseComposer.compose] exactly once, first. If the result is
 * [GatedOutcome.NotAccepted], it is returned unchanged --
 * [ResponseDelivery.deliver] is never called on that branch. If the
 * result is [GatedOutcome.Produced], [ResponseDelivery.deliver] is
 * called exactly once with the composed value, and its own result is
 * returned unchanged, whatever it is (`Produced` or `NotAccepted`) -- no
 * additional wrapping, exploiting [GatedOutcome]'s declared covariance so
 * the `NotAccepted` branch type-checks without conversion.
 *
 * @param responseComposer Composes a [ReasoningProviderResponse] into an
 *   [parker.core.interfaces.OutboundParkerResponse] (or a structural
 *   rejection). Called exactly once, first, on every invocation.
 * @param responseDelivery Delivers an already-composed
 *   [parker.core.interfaces.OutboundParkerResponse]. Called exactly
 *   once, only when composition succeeds. This is the only other
 *   dependency this class accepts -- the absence of any other
 *   constructor parameter is itself the structural guarantee that this
 *   class cannot reach `IdentityService`, `ExecutionPipeline`,
 *   `PermissionEngine`, `ReasoningProvider`, `ResourceRegistry`,
 *   `ToolRegistry`, `PlannerRuntime`, `MemoryStore`, or `WorldModel`,
 *   at any depth, except transitively through these two dependencies'
 *   own already-approved internals.
 */
class ReplyDeliveryCoordinator(
    private val responseComposer: ResponseComposer,
    private val responseDelivery: ResponseDelivery,
) {

    /**
     * Given the [InboundOwnerMessage] that began a Turn, and the
     * [ReasoningProviderResponse] reasoning about that Turn produced:
     * composes via [responseComposer], then delivers via
     * [responseDelivery] only on the admitted branch. See class KDoc for
     * the full sequencing rule. This coordinator never resolves
     * identity, never constructs or mutates an
     * [parker.core.interfaces.OutboundParkerResponse], never retries,
     * and never recovers from an exception either dependency throws --
     * such an exception propagates to this method's own caller
     * unchanged.
     */
    suspend fun composeAndDeliver(
        originalMessage: InboundOwnerMessage,
        reasoningResponse: ReasoningProviderResponse,
    ): GatedOutcome<ExecutionResult> {
        val composed = responseComposer.compose(originalMessage, reasoningResponse)
        return when (composed) {
            is GatedOutcome.NotAccepted -> composed
            is GatedOutcome.Produced -> responseDelivery.deliver(composed.value)
        }
    }
}
