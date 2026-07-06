package parker.core.interfaces

/**
 * Sprint 3, Track D, Unit D2 (Alignment Pass). Pure contract additions,
 * aligned to `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md`
 * (Unit D1A) -- the authoritative, field-level architecture for every
 * Planner Runtime public contract, superseding the exploratory shapes
 * this file originally carried under
 * `docs/architecture/PLANNER_RUNTIME_PROGRESSION_DESIGN.md` (Unit D1)
 * alone. Unit D1 authorised these concepts by name only ("a
 * `PlanCandidate` type," "a concrete Plan Decision mechanism"); Unit D1A
 * is what actually settles every field, every signature, and the
 * `PlannerRuntime` interface below -- this file introduces no
 * architecture beyond what Unit D1A already settles.
 *
 * Candidate *generation* (deciding what candidates to propose for a Goal)
 * remains explicitly out of scope: `PlannerRuntimeSpecification.md`
 * Section 3's Non-Goals and this Unit's own instructions rule out
 * "Planner reasoning" and "LLM integration." [PlanCandidate] values are
 * supplied to a [PlanDecision] by whatever upstream mechanism produced
 * them (a test fixture today; a real reasoning component, of whatever
 * kind, later -- AD-010, Model Independence); this file only shapes the
 * candidate and decides among however many are supplied, mirroring
 * [AgentStepSource]'s own "decision provider, not an authority" precedent
 * from Track C.
 */

/**
 * A Plan Candidate's own identifier
 * (`PlannerRuntimeSpecification.md` Section 4, Section 10). Distinct from
 * [TaskProposalId]: a candidate that is never selected never becomes a
 * Task Proposal at all. Unchanged by Unit D1A/the Alignment Pass --
 * `PLANNER_RUNTIME_CONTRACT_DESIGN.md` §1 confirms this exactly matches
 * the approved contract.
 */
@JvmInline
value class PlanCandidateId(val value: String) {
    init {
        require(value.isNotBlank()) { "PlanCandidateId must not be blank" }
    }
}

/**
 * One internally-generated candidate decomposition of a Goal
 * (`PlannerRuntimeSpecification.md` Section 4, "Plan Candidate"),
 * promoted from `DeterministicPlannerHarness.kt`'s test-only, single-field
 * `PlanCandidate` (`planCandidateId`, `goal`) to the real schema a
 * multi-candidate [PlanDecision] needs. Field shape approved by
 * `PLANNER_RUNTIME_CONTRACT_DESIGN.md` §2, in two explicit tiers:
 *
 * - **Required** (needed for [PlanDecision]'s own evaluation):
 *   [planCandidateId], [goal].
 * - **Optional carry-forward** (needed only so a selected candidate can
 *   become a well-formed [TaskProposal] without a separate re-entry step
 *   -- each is an identical name/type/default reuse of a [TaskProposal]
 *   field, not new schema): [rationale], [riskEstimate],
 *   [requiredCapabilities], [anticipatedPermissionActions], [constraints],
 *   [dependencies], [contextReferences], [resourceReferences],
 *   [expectedOutputs].
 *
 * A [PlanDecision] rejects any candidate whose [goal] does not match the
 * Planning Session's own Goal ([DefaultPlanDecision]) -- candidates are
 * expected to be decompositions of the same Goal they are being compared
 * for.
 *
 * **Deliberately no `init` validity check on [goal] (unlike
 * [TaskProposal.goal]).** A [PlanCandidate] is *pre-evaluation* input --
 * unlike a [TaskProposal], which is already-decided output expected to be
 * well-formed by construction, a [PlanCandidate] may legitimately be
 * malformed (blank goal, a goal that does not match the session's own
 * Goal). Recognising and rejecting that is [PlanDecision]'s own job, not
 * something this type enforces at construction time -- enforcing it here
 * would make invalid candidates impossible to construct, and therefore
 * impossible to test the rejection path against.
 */
data class PlanCandidate(
    val planCandidateId: PlanCandidateId,
    val goal: String,
    val rationale: String = "",
    val riskEstimate: RiskEstimate? = null,
    val requiredCapabilities: Set<PermissionAction> = emptySet(),
    val anticipatedPermissionActions: Set<PermissionAction> = emptySet(),
    val constraints: List<String> = emptyList(),
    val dependencies: List<ProposalDependency> = emptyList(),
    val contextReferences: List<String> = emptyList(),
    val resourceReferences: List<ResourceId> = emptyList(),
    val expectedOutputs: String = "",
)

/**
 * The closed set of reasons a [PlanDecision] may decline a [PlanCandidate]
 * (`PLANNER_RUNTIME_CONTRACT_DESIGN.md` §3). Exhaustive over
 * [DefaultPlanDecision]'s own four evaluation rules, in the order they are
 * checked:
 *
 * - [DUPLICATE_CANDIDATE_ID]: a candidate with this [PlanCandidateId] was
 *   already evaluated earlier in generation order.
 * - [BLANK_GOAL]: [PlanCandidate.goal] is blank.
 * - [GOAL_MISMATCH]: [PlanCandidate.goal] does not equal the Planning
 *   Session's own Goal.
 * - [NOT_SELECTED]: the candidate was itself structurally valid, but an
 *   earlier candidate in generation order was already chosen.
 *
 * Replaces the exploratory Unit D2 attempt's free-text-only
 * `reason: String`, which required tests to substring-match human prose
 * to check *why* a candidate was rejected -- a closed enum is
 * machine-checkable, matching this Unit's "completely explainable and
 * repeatable" requirement more precisely than prose alone.
 */
enum class PlanRejectionReason {
    DUPLICATE_CANDIDATE_ID,
    BLANK_GOAL,
    GOAL_MISMATCH,
    NOT_SELECTED,
}

/**
 * A declined [PlanCandidate] (`PlannerRuntimeSpecification.md` Section 4,
 * "Plan Rejection" -- "a Plan Candidate that is not selected is a Plan
 * Rejection... not an error"). [reason] is always exactly one of
 * [PlanRejectionReason]'s four values -- never a numeric score, since
 * this Unit's scope explicitly excludes "heuristic scoring" and "hidden
 * prioritisation" -- and [detail] is always a non-blank, human-readable
 * explanation of the specific offending value (e.g. which
 * [PlanCandidateId] this one duplicated). Aligned to
 * `PLANNER_RUNTIME_CONTRACT_DESIGN.md` §3.
 */
data class PlanRejection(
    val planCandidateId: PlanCandidateId,
    val reason: PlanRejectionReason,
    val detail: String,
) {
    init {
        require(detail.isNotBlank()) { "PlanRejection.detail must not be blank" }
    }
}

/**
 * The outcome of a [PlanDecision] (`PlannerRuntimeSpecification.md`
 * Section 4, "Plan Decision"). Exactly two variants, mirroring
 * `AgentStepDecision`'s own "sealed, exhaustive, no silent third case"
 * precedent:
 *
 * - [Selected]: at least one supplied [PlanCandidate] was valid; [winner]
 *   is the one this mechanism deterministically chose, and [rejections]
 *   names every other supplied candidate -- including any that were
 *   themselves structurally valid but simply not chosen -- as a
 *   [PlanRejection].
 * - [NoViableCandidate]: no supplied [PlanCandidate] was valid. This
 *   includes the zero-candidates case, where [rejections] is empty
 *   because there was nothing to reject.
 *   `PlannerRuntimeSpecification.md` Section 12's "Impossible proposal"
 *   governs what a caller does with this: the Planning Session
 *   transitions to `FAILED`.
 *
 * Unchanged in shape by Unit D1A/the Alignment Pass, beyond `rejections`
 * now carrying the corrected [PlanRejection] (`PLANNER_RUNTIME_CONTRACT_DESIGN.md` §4).
 */
sealed class PlanDecisionResult {
    data class Selected(
        val winner: PlanCandidate,
        val rejections: List<PlanRejection>,
    ) : PlanDecisionResult()

    data class NoViableCandidate(
        val rejections: List<PlanRejection>,
    ) : PlanDecisionResult()
}

/**
 * The seam by which a Planning Session chooses among its Plan Candidates
 * (`PlannerRuntimeSpecification.md` Section 4, "Plan Candidate" -- "a
 * Planning Session MAY generate one or many Plan Candidates before
 * selecting among them (Plan Decision, below)"). Mirrors
 * [AgentStepSource]'s own "decision provider, not an authority" precedent
 * exactly: a [PlanDecision] only ever chooses among, or rejects,
 * candidates it is given -- it grants no permission and creates no Task,
 * and its output still passes through
 * [TaskProposalIntake.submitProposal] exactly as any other [TaskProposal]
 * does (AD-005, AD-002).
 *
 * **`suspend`, per `PLANNER_RUNTIME_CONTRACT_DESIGN.md` §5 (Alignment
 * Pass correction).** The exploratory Unit D2 attempt declared this
 * non-`suspend`, reasoning that a deterministic decision needs no
 * suspension -- true for [DefaultPlanDecision] specifically, but the
 * wrong basis for the *interface* signature. `PlannerRuntimeSpecification.md`
 * Section 14 and AD-010 (Model Independence) anticipate a future
 * model-backed or human-in-the-loop [PlanDecision] implementation, which
 * would need to suspend (an external call, a UI wait). Declaring `decide`
 * `suspend` now, even though [DefaultPlanDecision]'s own body never
 * actually suspends, avoids a breaking interface change later -- exactly
 * the same reasoning [AgentStepSource] was already given in Track C.
 */
interface PlanDecision {
    suspend fun decide(goal: String, candidates: List<PlanCandidate>): PlanDecisionResult
}

/**
 * The input that starts one Planning Session's progression through
 * [PlannerRuntime] (`PlannerRuntimeSpecification.md` Section 4, "Planning
 * Request" -- "a Goal, an initiating Principal, and (optionally) declared
 * Constraints").
 *
 * **No `candidates` field, per `PLANNER_RUNTIME_CONTRACT_DESIGN.md` §6
 * (Alignment Pass correction).** The exploratory Unit D2 attempt embedded
 * `candidates: List<PlanCandidate>` directly on this type -- a defect,
 * not a simplification worth keeping: Section 4 never places candidates
 * on a Planning Request, and a Planning Request logically precedes
 * `CONTEXT_GATHERING`/`ANALYSING`, before any Plan Candidate is generated,
 * by whatever mechanism. Candidates are generated by planning, not
 * supplied by callers, and are instead passed as a separate argument to
 * [PlannerRuntime.plan].
 */
data class PlanningRequest(
    val planningSessionId: PlanningSessionId,
    val initiatingPrincipalId: PrincipalId,
    val goal: String,
    val correlationId: String,
    val source: RequestOrigin = RequestOrigin.TEXT,
    val priority: RequestPriority = RequestPriority.NORMAL,
) {
    init {
        require(goal.isNotBlank()) { "PlanningRequest.goal must not be blank" }
        require(correlationId.isNotBlank()) { "PlanningRequest.correlationId must not be blank" }
    }
}

/**
 * The terminal outcome of one [PlannerRuntime.plan] call, mirroring
 * `PlannerRuntimeSpecification.md` Section 5's three terminal states this
 * Unit's progression actually reaches (`COMPLETED`, `REJECTED`, `FAILED`
 * -- see [PlannerSessionLifecycleTransitions] for the full, documented
 * subset this Unit models). Unchanged in shape by Unit D1A/the Alignment
 * Pass, beyond `rejections` now carrying the corrected [PlanRejection]
 * (`PLANNER_RUNTIME_CONTRACT_DESIGN.md` §7).
 */
sealed class PlanningSessionResult {

    /** `SUBMITTED -> COMPLETED`: the Task Manager Runtime's disposition was not an outright rejection. */
    data class Completed(
        val planningSessionId: PlanningSessionId,
        val taskProposalId: TaskProposalId,
        val disposition: TaskProposalDisposition,
        val rejections: List<PlanRejection>,
    ) : PlanningSessionResult()

    /** `SUBMITTED -> REJECTED`: the Task Manager Runtime declined the submitted Task Proposal outright. */
    data class Rejected(
        val planningSessionId: PlanningSessionId,
        val taskProposalId: TaskProposalId,
        val disposition: TaskProposalDisposition.Rejected,
        val rejections: List<PlanRejection>,
    ) : PlanningSessionResult()

    /**
     * Either the initiating Principal never resolved (in which case no
     * Planning Session record was ever created -- see
     * [InMemoryPlannerRuntime.plan]'s own KDoc) or `ANALYSING -> FAILED`
     * (no viable Plan Candidate, `PlannerRuntimeSpecification.md` Section
     * 12's "Impossible proposal"). [reason] remains free text
     * (`PLANNER_RUNTIME_CONTRACT_DESIGN.md` §7 considered, and declined,
     * the same enum-plus-detail treatment [PlanRejection] received: a
     * Planning Session's own terminal failure is a one-off summary with
     * only two origins, already distinguishable by which branch of code
     * returns [Failed] at all, unlike a per-candidate rejection evaluated
     * automatically many times against a fixed rule set).
     */
    data class Failed(
        val planningSessionId: PlanningSessionId,
        val reason: String,
        val rejections: List<PlanRejection>,
    ) : PlanningSessionResult()
}

/**
 * The Planner Runtime's public interface
 * (`PLANNER_RUNTIME_CONTRACT_DESIGN.md` §10 -- new in the Alignment Pass).
 * Closes the architectural traceability finding that the exploratory Unit
 * D2 attempt's `InMemoryPlannerRuntime` implemented no pre-existing
 * interface at all, unlike every other `InMemory*` runtime in this
 * codebase (`InMemoryTaskManagerRuntime : TaskProposalIntake`,
 * `InMemoryAgentRuntime : AgentRunCommandChannel`,
 * `InMemoryIdentityService : IdentityService`) -- each of which implements
 * an interface named in an earlier phase, before the concrete class
 * existed. This interface is that missing counterpart for the Planner
 * Runtime, named directly (rather than a "Channel"/"Intake"-style
 * functional name) since its public contract is genuinely just "run a
 * Planning Session" -- one operation.
 *
 * [candidates] is a separate parameter, not a field of [PlanningRequest]
 * (see that type's own KDoc) -- candidates are generated by planning, not
 * supplied by callers alongside the request that starts it.
 */
interface PlannerRuntime {
    suspend fun plan(request: PlanningRequest, candidates: List<PlanCandidate>): PlanningSessionResult
}
