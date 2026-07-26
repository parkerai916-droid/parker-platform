package parker.core.runtime

import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.PlanningRequest
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.ReasoningProviderResponse

/**
 * The authoritative reason a [GoalPlanningHandoffCoordinator] defers
 * planning initiation, per
 * `docs/implementation/REASONING_TO_PLANNING_HANDOFF_SCOPE_LOCK.md`
 * Section 2.1. A single member today, deliberately -- not a placeholder
 * forgotten mid-design. This is the machine-readable source of truth for
 * why a [GoalPlanningHandoffOutcome.Deferred] exists; [GoalPlanningHandoffOutcome.Deferred.detail]
 * is supplementary only and must never be treated as authoritative in its
 * place. Adding a second member (e.g. once real submission to
 * `PlannerRuntime` exists) is itself a future, separately-governed
 * contract revision -- not decided or performed here.
 */
enum class PlanningDeferralReason {
    CANDIDATE_GENERATION_UNAVAILABLE,
}

/**
 * The result of [GoalPlanningHandoffCoordinator.initiatePlanning], per
 * `docs/architecture/REASONING_TO_PLANNING_HANDOFF_CONTRACT_DESIGN.md`
 * Section 5.2. Sealed, not a plain data class, and `class`, not
 * `interface` -- matching this repository's own exclusive existing
 * convention for outcome types (`GatedOutcome`, `ParkerRuntimeOutcome`,
 * `PlanningSessionResult`, `ReasoningProviderResponse`, and every other
 * sealed outcome type in `src/`; none is a `sealed interface`).
 *
 * **Deliberately named without any variant implying planner invocation
 * ever occurred.** The Contract Design's own Revision 2 exists because
 * an earlier name (`PlanningInitiationOutcome`) implied initiation that
 * never happens in this Unit's own code. Every variant below must
 * preserve that discipline.
 *
 * Sealed with exactly one variant today: this signals, at the type
 * level, that [Deferred] is the *only* outcome this coordinator can
 * honestly produce while no legitimate `PlanCandidate` source exists. A
 * future `Submitted`-shaped variant, added only once a real
 * `PlannerRuntime.plan()` call exists somewhere in this coordinator's own
 * body, is a distinct, separately-governed addition -- not a silent
 * extension of this type performed here.
 */
sealed class GoalPlanningHandoffOutcome {

    /**
     * Planner invocation is deferred. [planningRequest] is a fully and
     * correctly constructed value -- ready for a future `PlannerRuntime.plan()`
     * call -- but is not submitted anywhere by this coordinator.
     *
     * **Not a rejection, not a failure, not a completed planning
     * initiation.** It is neither [GatedOutcome.NotAccepted] (reserved for
     * genuine rejections elsewhere in this pipeline -- a malformed
     * message, an invalid `Reply`) nor a thrown exception (see
     * [GoalPlanningHandoffCoordinator.initiatePlanning]'s own KDoc for
     * the one real fault surface this coordinator has) nor any
     * `PlanningSessionResult` variant (that type belongs exclusively to
     * `PlannerRuntime.plan`'s own real return value, never fabricated
     * here).
     *
     * @param planningRequest The fully-constructed request, carried
     *   unchanged, so a caller (logging, a future `EventBus` payload, a
     *   future real submission path) has the already-correct value
     *   available without reconstructing it.
     * @param reason The **authoritative** reason. Exactly one value
     *   exists today ([PlanningDeferralReason.CANDIDATE_GENERATION_UNAVAILABLE]).
     * @param detail A human-readable, non-blank, fixed explanation.
     *   **Supplementary only, never machine-authoritative** -- mirrors
     *   the existing `PlanRejection(reason, detail)` shape already in
     *   this codebase (`src/contracts/PlanDecision.kt`). No caller may
     *   treat this string's content as the source of truth for what
     *   happened; [reason] alone is authoritative.
     */
    data class Deferred(
        val planningRequest: PlanningRequest,
        val reason: PlanningDeferralReason,
        val detail: String,
    ) : GoalPlanningHandoffOutcome() {
        init {
            require(detail.isNotBlank()) { "GoalPlanningHandoffOutcome.Deferred.detail must not be blank" }
        }
    }
}

/**
 * Fixed, non-blank, non-caller-supplied wording for every
 * [GoalPlanningHandoffOutcome.Deferred.detail] this coordinator produces.
 * A private, top-level, file-scoped constant -- deliberately not a
 * companion-object member, so [GoalPlanningHandoffCoordinator]'s own
 * `declaredFields` carries nothing beyond its one constructor-injected
 * dependency (a companion object would add a synthetic `Companion` field
 * to the outer class, which the Scope Lock's own structural
 * "no field beyond its one constructor-injected dependency" test must
 * not have to account for).
 */
private const val DEFERRAL_DETAIL = "Planning initiation for this Goal is deferred: PlanCandidate generation " +
    "is not yet implemented in this repository. PlannerRuntime.plan() was not invoked."

/**
 * Sequences a [ReasoningProviderResponse.Goal] toward
 * [parker.core.interfaces.PlannerRuntime], per
 * `docs/architecture/REASONING_TO_PLANNING_HANDOFF_GOVERNANCE_REVIEW.md`
 * (commit `4490e36`),
 * `docs/architecture/REASONING_TO_PLANNING_HANDOFF_CONTRACT_DESIGN.md`
 * (Revision 2), and
 * `docs/implementation/REASONING_TO_PLANNING_HANDOFF_SCOPE_LOCK.md`.
 *
 * **This coordinator never calls `PlannerRuntime.plan()`.** It holds no
 * reference to `PlannerRuntime`, to any `PlanCandidate`-producing
 * component, or to `IdentityService`, `EventBus`, or `TaskManagerRuntime`.
 * The absence of any constructor parameter beyond [planningSessionIdFactory]
 * is itself the structural guarantee that this class cannot reach any of
 * them -- the same pattern every other coordinator in this codebase
 * already uses its own dependency list to prove
 * (`ConversationTurnReasoningCoordinator`, `ConversationReplyCoordinator`,
 * `ResponseComposer`). "Invocation responsibility" for `PlannerRuntime` is
 * discharged architecturally by this coordinator's own position in the
 * call chain, not operationally by this coordinator's own current code
 * (Contract Design Section 1): a future, separately-governed Unit --
 * which must also supply legitimate `PlanCandidate` generation and
 * production composition wiring -- is the one that adds a `PlannerRuntime`
 * dependency here and replaces [initiatePlanning]'s body with a real
 * `plan()` call.
 *
 * **Required Implementation Decision, mirroring
 * `ConversationTurnReasoningCoordinator`'s own precedent.** Concrete, not
 * interface-backed. Introduces no new public contract type beyond
 * [GoalPlanningHandoffOutcome]/[PlanningDeferralReason] above, and is
 * ordinary Stage 3 implementation-level wiring between two
 * already-approved contracts -- not a new architectural boundary
 * requiring an interface seam.
 *
 * @param planningSessionIdFactory Mints a fresh `PlanningSessionId` value
 *   per [initiatePlanning] call. **No default is supplied here** -- per
 *   Scope Lock Section 2.1, both production wiring (`ParkerRuntime.kt`,
 *   supplying `{ java.util.UUID.randomUUID().toString() }`) and every
 *   test must supply this explicitly. The returned value must be
 *   newly minted per call, non-blank, opaque, production-unique, and not
 *   derived from `correlationId`, message text, or `Goal.text` -- the
 *   last of these is a documented obligation on whatever function is
 *   supplied, not something this coordinator's own code can verify at
 *   runtime, since it has no way to inspect whether a returned string was
 *   derived from its own inputs.
 */
class GoalPlanningHandoffCoordinator(
    private val planningSessionIdFactory: () -> String,
) {

    /**
     * Constructs a [PlanningRequest] from [originalMessage] and [goal],
     * and returns [GoalPlanningHandoffOutcome.Deferred] reporting that
     * planner invocation does not occur. `PlannerRuntime.plan()` is never
     * called by this method, under any input.
     *
     * **Field-by-field construction (Contract Design Section 4, Scope
     * Lock Section 2.1) -- no other derivation or inference is
     * permitted:**
     * - `planningSessionId`: [planningSessionIdFactory]'s result, wrapped.
     * - `initiatingPrincipalId`: [originalMessage]`.senderPrincipalId`,
     *   unchanged.
     * - `correlationId`: [originalMessage]`.correlationId.value`,
     *   unchanged (one unwrap only).
     * - `goal`: [goal]`.text`, unchanged.
     * - `source`: the field's own existing default, `RequestOrigin.TEXT`
     *   -- not derived from anything, since `InboundOwnerMessage` carries
     *   no field from which a `RequestOrigin` could be derived, and the
     *   only production channel capable of producing an
     *   `InboundOwnerMessage` that reaches this coordinator today is the
     *   Local Text Channel.
     * - `priority`: the field's own existing default,
     *   `RequestPriority.NORMAL` -- identical reasoning; no field
     *   anywhere upstream carries a priority signal to derive from.
     *
     * **Does not throw under a well-formed, non-throwing
     * [planningSessionIdFactory], provable by construction:**
     * `PlanningRequest.goal` and `PlanningRequest.correlationId` both
     * require non-blank, but are supplied from [goal]`.text` and
     * [originalMessage]`.correlationId.value`, both already guaranteed
     * non-blank by their own types' `init` blocks; [GoalPlanningHandoffOutcome.Deferred.detail]
     * is a fixed, non-blank literal, never caller-supplied.
     *
     * **The one real fault surface this method has:** if
     * [planningSessionIdFactory] returns blank, `PlanningSessionId`'s own
     * existing `init` check throws `IllegalArgumentException` --  not a
     * new check this method adds. If [planningSessionIdFactory] itself
     * throws, that exception propagates unchanged. Neither case is
     * caught here; no `try`/`catch` exists in this method, matching every
     * other coordinator's own established discipline in this codebase. An
     * exception from either case propagates to this method's own caller
     * ([ConversationReplyCoordinator.submitAndDeliver]), which also has
     * no `try`/`catch`, ultimately reaching
     * `parker.composition.ParkerRuntime.submitOwnerMessage`'s own
     * existing outer boundary and surfacing as
     * `parker.composition.ParkerRuntimeOutcome.Failed(PipelineStage.UNKNOWN, e)`
     * -- the same, already-governed machinery this runtime already uses
     * for every other untagged fault. No new `PipelineStage`, exception
     * type, or `ParkerRuntimeOutcome` variant is introduced for this
     * failure path.
     *
     * `suspend`, even though nothing this method's own body does
     * currently suspends ([planningSessionIdFactory] is a plain,
     * non-suspend `() -> String`) -- retained for the identical
     * forward-compatibility reason already established as precedent by
     * `PlanDecision.decide` (`src/contracts/PlanDecision.kt`): the day a
     * future Unit adds a real `PlannerRuntime` dependency (a suspend
     * interface) and replaces this method's body with an actual `plan()`
     * call, this signature does not need to change a second time, and
     * neither does its only caller.
     */
    suspend fun initiatePlanning(
        originalMessage: InboundOwnerMessage,
        goal: ReasoningProviderResponse.Goal,
    ): GoalPlanningHandoffOutcome {
        val planningRequest = PlanningRequest(
            planningSessionId = PlanningSessionId(planningSessionIdFactory()),
            initiatingPrincipalId = originalMessage.senderPrincipalId,
            goal = goal.text,
            correlationId = originalMessage.correlationId.value,
        )
        return GoalPlanningHandoffOutcome.Deferred(
            planningRequest = planningRequest,
            reason = PlanningDeferralReason.CANDIDATE_GENERATION_UNAVAILABLE,
            detail = DEFERRAL_DETAIL,
        )
    }
}
