package parker.core.runtime

import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.PlanCandidateGenerator
import parker.core.interfaces.PlannerRuntime
import parker.core.interfaces.PlanningRequest
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.PlanningSessionResult
import parker.core.interfaces.ReasoningProviderResponse

/**
 * The result of [GoalPlanningHandoffCoordinator.initiatePlanning], per
 * `docs/architecture/PLAN_CANDIDATE_TO_PLANNER_INTEGRATION_CONTRACT_DESIGN.md`
 * and `docs/implementation/PLAN_CANDIDATE_TO_PLANNER_INTEGRATION_SCOPE_LOCK.md`
 * Section 4. Sealed, not a plain data class, and `class`, not
 * `interface` -- matching this repository's own exclusive existing
 * convention for outcome types (`GatedOutcome`, `ParkerRuntimeOutcome`,
 * `PlanningSessionResult`, `ReasoningProviderResponse`, and every other
 * sealed outcome type in `src/`; none is a `sealed interface`).
 *
 * **Revised, Plan Candidate to PlannerRuntime Integration.** The prior
 * `Deferred` variant (and its own `PlanningDeferralReason`) is removed in
 * full: this coordinator now genuinely calls `PlannerRuntime.plan()`, so
 * "planner invocation is deferred" is no longer an honest description of
 * what happens here. Sealed with exactly one variant, [Planned] --
 * mirroring `ConversationOutcome.ReplyDelivered`'s own precedent of an
 * outer wrapper name describing "the pipeline ran to a real terminal
 * value" without implying guaranteed success: [Planned] may carry any of
 * `PlanningSessionResult`'s three variants, including `Failed`.
 */
sealed class GoalPlanningHandoffOutcome {

    /**
     * A Planning Session was genuinely run to completion via
     * [PlannerRuntime.plan]. [planningSessionResult] is that call's own
     * return value, carried completely unchanged -- never inspected,
     * unwrapped, or remapped by this coordinator. This is the *only*
     * variant [GoalPlanningHandoffOutcome] declares: no separate wrapper
     * exists for `Completed`, `Rejected`, `Failed`, no-viable-candidate,
     * deferred, or waiting-for-input -- `PlanningSessionResult` itself
     * already distinguishes those cases.
     */
    data class Planned(
        val planningSessionResult: PlanningSessionResult,
    ) : GoalPlanningHandoffOutcome()
}

/**
 * Sequences a [ReasoningProviderResponse.Goal] toward
 * [PlannerRuntime], per
 * `docs/architecture/PLAN_CANDIDATE_TO_PLANNER_INTEGRATION_GOVERNANCE_REVIEW.md`,
 * `docs/architecture/PLAN_CANDIDATE_TO_PLANNER_INTEGRATION_CONTRACT_DESIGN.md`,
 * and
 * `docs/implementation/PLAN_CANDIDATE_TO_PLANNER_INTEGRATION_SCOPE_LOCK.md`.
 *
 * **This coordinator now genuinely calls `PlannerRuntime.plan()`, via
 * [planCandidateGenerator] and [plannerRuntime].** It holds no reference
 * to `IdentityService`, `EventBus`, `TaskProposalIntake`, or
 * `TaskManagerRuntime` -- the absence of any constructor parameter beyond
 * the three declared below is itself the structural guarantee that this
 * class cannot reach any of them directly, the same pattern every other
 * coordinator in this codebase already uses its own dependency list to
 * prove (`ConversationTurnReasoningCoordinator`,
 * `ConversationReplyCoordinator`, `ResponseComposer`).
 *
 * Concrete, not interface-backed, matching
 * `ConversationTurnReasoningCoordinator`'s own precedent. Introduces no
 * new public contract type beyond the revised [GoalPlanningHandoffOutcome]
 * above.
 *
 * @param planningSessionIdFactory Mints a fresh `PlanningSessionId` value
 *   per [initiatePlanning] call. **No default is supplied here** -- both
 *   production wiring (`ParkerRuntime.kt`, supplying
 *   `{ java.util.UUID.randomUUID().toString() }`) and every test must
 *   supply this explicitly. The returned value must be newly minted per
 *   call, non-blank, opaque, production-unique, and not derived from
 *   `correlationId`, message text, or `Goal.text`.
 * @param planCandidateGenerator Called exactly once per [initiatePlanning]
 *   call, with the same [PlanningRequest] instance [plannerRuntime] later
 *   receives. Its returned candidate list is passed to [plannerRuntime]
 *   unchanged and in the same order -- including an empty list, which is
 *   passed through, never short-circuited or reinterpreted here.
 * @param plannerRuntime Called exactly once per [initiatePlanning] call,
 *   after [planCandidateGenerator]. Its returned [PlanningSessionResult]
 *   is wrapped in [GoalPlanningHandoffOutcome.Planned] and returned
 *   unchanged -- this coordinator never inspects which variant it is.
 */
class GoalPlanningHandoffCoordinator(
    private val planningSessionIdFactory: () -> String,
    private val planCandidateGenerator: PlanCandidateGenerator,
    private val plannerRuntime: PlannerRuntime,
) {

    /**
     * Constructs a [PlanningRequest] from [originalMessage] and [goal],
     * generates candidates via [planCandidateGenerator], runs them
     * through [plannerRuntime], and returns the result wrapped in
     * [GoalPlanningHandoffOutcome.Planned].
     *
     * **Field-by-field `PlanningRequest` construction, unchanged from
     * this coordinator's prior revision -- no other derivation or
     * inference is permitted:**
     * - `planningSessionId`: [planningSessionIdFactory]'s result, wrapped.
     * - `initiatingPrincipalId`: [originalMessage]`.senderPrincipalId`,
     *   unchanged.
     * - `correlationId`: [originalMessage]`.correlationId.value`,
     *   unchanged (one unwrap only).
     * - `goal`: [goal]`.text`, unchanged.
     * - `source`/`priority`: the fields' own existing defaults
     *   (`RequestOrigin.TEXT`/`RequestPriority.NORMAL`).
     *
     * **The same `PlanningRequest` instance is passed unchanged to both
     * [planCandidateGenerator] and [plannerRuntime].** The candidate list
     * [planCandidateGenerator] returns is passed to [plannerRuntime]
     * unchanged and in the same order, including when it is empty --
     * [plannerRuntime] remains the sole owner of no-viable-candidate
     * handling.
     *
     * **No `try`/`catch` in this method.** An exception thrown by
     * [planCandidateGenerator] propagates unchanged, and
     * [plannerRuntime] is never called in that case. An exception thrown
     * by [plannerRuntime] propagates unchanged. A returned
     * `PlanningSessionResult.Failed` is a normal, non-exceptional result
     * -- wrapped in [GoalPlanningHandoffOutcome.Planned] exactly like
     * `Completed`/`Rejected`, never treated as a fault by this method.
     * Any exception reaches this method's own caller
     * ([ConversationReplyCoordinator.submitAndDeliver]), which also has
     * no `try`/`catch`, ultimately reaching
     * `parker.composition.ParkerRuntime.submitOwnerMessage`'s own
     * existing outer boundary and surfacing as
     * `parker.composition.ParkerRuntimeOutcome.Failed(PipelineStage.UNKNOWN, e)`.
     * No new `PipelineStage`, exception type, or `ParkerRuntimeOutcome`
     * variant is introduced for this failure path.
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

        val candidates = planCandidateGenerator.generate(planningRequest)
        val result = plannerRuntime.plan(planningRequest, candidates)

        return GoalPlanningHandoffOutcome.Planned(result)
    }
}
