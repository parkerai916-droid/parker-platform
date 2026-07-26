package parker.core.interfaces

/**
 * The seam by which one or more [PlanCandidate]s are produced for a
 * [PlanningRequest], per
 * `docs/architecture/CANDIDATE_GENERATION_CONTRACT_DESIGN.md` and
 * `docs/implementation/CANDIDATE_GENERATION_SCOPE_LOCK.md`. Mirrors
 * [PlanDecision]'s and [parker.core.interfaces.ReasoningProvider]'s own
 * "single `suspend` operation, model-independent, pure callee" shape
 * exactly -- this is architecturally the same kind of seam, not a
 * sequencing coordinator and not a stateful runtime.
 *
 * **The sole legitimate input is the complete [PlanningRequest] --
 * nothing else** (Contract Design Section 1, Scope Lock Section 2.2).
 * `Turn`, `InboundOwnerMessage`, `ReasoningContext`, Conversation History,
 * Memory Source, World Model Source, runtime metadata, `IdentityService`,
 * authenticated Principal objects, Permission Engine state, and Tool
 * Registry state must never be received by an implementation of this
 * interface -- `initiatingPrincipalId`, `correlationId`, the Goal text,
 * `source`, and `priority` are available only through [PlanningRequest]'s
 * own already-existing fields. No Planning Context is designed or implied
 * by this interface; a future, independently-owned Planning Context
 * assembler remains entirely deferred (Contract Design Section 9).
 *
 * **An implementation never calls [PlannerRuntime.plan], holds no
 * reference to [PlannerRuntime], and never authorises, executes,
 * schedules, or creates a Task.** It proposes only -- [PlanCandidate]s
 * this interface produces are consumed by [PlanDecision], never submitted
 * anywhere by this interface's own contract.
 *
 * **Output.** [generate] returns a plain `List<PlanCandidate>` -- no
 * sealed wrapper or result type, since no decision happens here, only
 * production (Contract Design Section 4). Zero, one, or many candidates
 * are all valid, non-exceptional results (Scope Lock Section 2.3,
 * Section 2.5): an empty list means only that no candidate was produced
 * for this [PlanningRequest] -- it is never a structured signal for an
 * ambiguous Goal, insufficient information, an impossible request, an
 * unsafe request, or conflicting Goals, none of which this contract
 * represents. **Output list order must be stable and deterministic for
 * identical inputs and identical generator configuration** -- required
 * because [DefaultPlanDecision] may consume list order as its own
 * tie-break rule ("first valid, non-duplicate candidate in generation
 * order"); an implementation must not derive its output order from an
 * unordered collection, or from incidental `Map`/`Set` iteration order.
 *
 * **Failure contract.** An implementation may throw only for a genuine
 * fault (a real timeout, crash, or malformed upstream output) -- never
 * merely because it could not decompose the Goal, which is an empty list,
 * not a fault, mirroring [ReasoningProviderResponse.NoAction]'s own
 * documented discipline in reverse.
 *
 * **`suspend`, for forward-compatibility only**, identical reasoning to
 * [PlanDecision.decide] and [ReasoningProvider.reason]: nothing in a
 * first, deterministic implementation needs to suspend, but a future
 * model-backed, rule-based, or human-in-the-loop generator would need to
 * -- declaring this now avoids a breaking interface change later.
 */
interface PlanCandidateGenerator {
    suspend fun generate(
        request: PlanningRequest,
    ): List<PlanCandidate>
}
