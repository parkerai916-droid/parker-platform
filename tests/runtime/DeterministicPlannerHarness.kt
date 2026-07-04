package parker.core.runtime

import java.time.Instant
import parker.core.interfaces.EventBus
import parker.core.interfaces.EventType
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId
import parker.core.interfaces.RiskEstimate
import parker.core.interfaces.TaskProposal
import parker.core.interfaces.TaskProposalId

/**
 * Sprint 1, Unit 5 (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md`
 * §6): "A fixed, non-LLM function producing exactly one Plan Candidate and
 * one Task Proposal for one fixed Goal, exercising the real Planning
 * Session lifecycle states."
 *
 * Test-only fixture. Deliberately does NOT live in `src/runtime` -- the
 * Planner Runtime itself remains specification-only
 * (`docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`,
 * "Status": not yet promoted to an implementation phase per
 * `IMPLEMENTATION_ORDER.md`). This mirrors `FakePermissionEngine.kt` and
 * `MockTool.kt`'s identical "test fixture, not `src/runtime`" precedent:
 * this harness exists only to give Sprint 1's vertical slice a real
 * `TaskProposal` instance to submit, not to implement the Planner Runtime
 * Specification's general Plan Decision mechanism (AD-010).
 *
 * ## Scope: what this does and does not model
 *
 * [PlanningSessionLifecycleState] intentionally names only the 5 states
 * this fixed harness actually visits --
 * `CREATED, CONTEXT_GATHERING, ANALYSING, PROPOSING, SUBMITTED` -- the
 * single path `PlannerRuntimeSpecification.md` §5 names as "the only path
 * into a submitted Task Proposal." The other 5 states that section's
 * lifecycle diagram specifies --
 * `WAITING_FOR_INPUT, COMPLETED, REJECTED, CANCELLED, FAILED` -- are real,
 * specified states this harness does not model, because a fixed,
 * deterministic, always-succeeds harness has no branch that would ever
 * reach them (no missing context, no rejected Plan Candidate, no
 * cancellation, no Task Manager disposition to react to). This is a
 * documented subset, not a claim that those 5 states don't exist.
 *
 * This harness also does not: generate or compare more than one Plan
 * Candidate (Plan Decision, §4, is out of scope -- there is nothing to
 * decide among when only one candidate is ever produced); resolve real
 * Planning Context (§9); or call
 * `TaskProposalIntake.submitProposal` (`src/contracts/TaskProposal.kt`) --
 * reaching `SUBMITTED` here means "a well-formed `TaskProposal` has been
 * constructed and is ready to submit," not that it has been submitted to a
 * real Task Manager Runtime, which has no implementation yet
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` Unit 6).
 */
enum class PlanningSessionLifecycleState {
    CREATED,
    CONTEXT_GATHERING,
    ANALYSING,
    PROPOSING,
    SUBMITTED,
}

/**
 * The 4 edges this fixed harness exercises, transcribed from the subset of
 * `PlannerRuntimeSpecification.md` §5's lifecycle diagram this unit
 * models (see [PlanningSessionLifecycleState]'s KDoc for what is
 * deliberately omitted). Mirrors `ExecutionLifecycleTransitions`'s
 * existing map-of-allowed-next-states shape
 * (`src/contracts/ExecutionLifecycle.kt`).
 */
object PlanningSessionLifecycleTransitions {

    private val allowed: Map<PlanningSessionLifecycleState, Set<PlanningSessionLifecycleState>> = mapOf(
        PlanningSessionLifecycleState.CREATED to setOf(
            PlanningSessionLifecycleState.CONTEXT_GATHERING,
        ),
        PlanningSessionLifecycleState.CONTEXT_GATHERING to setOf(
            PlanningSessionLifecycleState.ANALYSING,
        ),
        PlanningSessionLifecycleState.ANALYSING to setOf(
            PlanningSessionLifecycleState.PROPOSING,
        ),
        PlanningSessionLifecycleState.PROPOSING to setOf(
            PlanningSessionLifecycleState.SUBMITTED,
        ),
        PlanningSessionLifecycleState.SUBMITTED to emptySet(),
    )

    fun isValidTransition(from: PlanningSessionLifecycleState, to: PlanningSessionLifecycleState): Boolean =
        to in allowed.getValue(from)

    /** Throws [IllegalArgumentException] if `from -> to` is not one of the 4 edges this harness models. */
    fun requireValidTransition(from: PlanningSessionLifecycleState, to: PlanningSessionLifecycleState) {
        require(isValidTransition(from, to)) {
            "Illegal Planning Session lifecycle transition for this fixed harness: $from -> $to"
        }
    }
}

/**
 * One internally-generated candidate decomposition of a Goal
 * (`PlannerRuntimeSpecification.md` §4, "Plan Candidate"). No formal
 * schema exists anywhere in this repository for Plan Candidate -- unlike
 * [TaskProposal], which Sprint 1's contract-closure addendum already
 * shaped (Unit 1). This is deliberately the smallest possible shape: an
 * identifier and the Goal it decomposes, sufficient for this fixed
 * harness's single-candidate path and no more. Test-only, local to this
 * file; not proposed as a `src/contracts` addition.
 */
data class PlanCandidate(
    val planCandidateId: String,
    val goal: String,
) {
    init {
        require(planCandidateId.isNotBlank()) { "PlanCandidate.planCandidateId must not be blank" }
        require(goal.isNotBlank()) { "PlanCandidate.goal must not be blank" }
    }
}

/**
 * Runs one fixed, deterministic Planning Session: given a Goal and an
 * initiating Principal, produces exactly one [PlanCandidate] and exactly
 * one [TaskProposal], stepping through
 * `CREATED -> CONTEXT_GATHERING -> ANALYSING -> PROPOSING -> SUBMITTED`
 * (see this file's top-level KDoc for what is out of scope). A fresh
 * instance models exactly one Planning Session; [run] may be called only
 * once per instance.
 *
 * Deterministic by construction, per AD-010: given the same arguments to
 * [run], the produced [PlanCandidate] and [TaskProposal] are identical
 * every time -- no randomness, no clock reads beyond what the caller
 * supplies, no external state.
 *
 * ## Sprint 1, Unit 9 (Runtime Lifecycle Event Publication)
 *
 * [run] publishes one `planner.*` [ParkerEvent] per real transition this
 * harness models (`PlannerRuntimeSpecification.md` §11): `session_created`,
 * `context_requested`, `analysis_started`, `proposal_created`,
 * `proposal_submitted`. The other 7 `planner.*` events that section names
 * are not published -- either informational-only milestones this fixed
 * harness has no branch for (`context_received`, `candidate_generated`,
 * `candidate_rejected`, `permission_flagged`), or real transitions into
 * states this harness never reaches (`input_required`, `session_completed`,
 * `session_failed`, `session_cancelled`), consistent with this file's own
 * top-level "Scope" KDoc.
 *
 * `publisherPrincipalId` is [PLANNER_RUNTIME_PRINCIPAL_ID], a hardcoded
 * Sprint 1 placeholder for "the identity the Planner Runtime itself
 * operates under" (§11) -- no real Planner Runtime Identity registration
 * or allocation scheme is specified anywhere yet, mirroring the same
 * documented-placeholder treatment [InMemoryTaskManagerRuntime]'s `TaskId`
 * derivation and [InMemoryAgentRuntime]'s `AgentRunId`/agent `PrincipalId`
 * derivation already receive.
 *
 * `correlationId` on every event is [run]'s own caller-supplied
 * `correlationId` parameter, not `planningSessionId` (which §11 names as
 * the general convention). Sprint 1's own existing contracts already
 * thread one shared `correlationId` string end-to-end
 * (`TaskProposal.correlationId` -> `AgentRunCommand.correlationId` ->
 * `ExecutionRequest.correlationId`, each copied forward unchanged by
 * Units 6-7's own code); using that same shared value here, instead of
 * switching to a different per-domain key mid-slice, is what makes "every
 * event under the same Planning Session/Task/Agent Run/Execution Request
 * shares a resolvable correlationId chain" (Vertical Slice Plan §7) true
 * by construction.
 */
class DeterministicPlannerHarness(
    private val eventBus: EventBus,
) {

    private companion object {
        /** Sprint 1 placeholder -- see this class's own KDoc, "Unit 9" section. */
        val PLANNER_RUNTIME_PRINCIPAL_ID = PrincipalId("system.planner-runtime")
    }

    private val visitedStates = mutableListOf(PlanningSessionLifecycleState.CREATED)
    private var candidate: PlanCandidate? = null
    private var proposal: TaskProposal? = null

    /** The exact sequence of states visited so far, starting from [PlanningSessionLifecycleState.CREATED]. */
    val stateHistory: List<PlanningSessionLifecycleState> get() = visitedStates.toList()

    /** The current (most recently reached) lifecycle state. */
    val currentState: PlanningSessionLifecycleState get() = visitedStates.last()

    /** Exactly one [PlanCandidate] once [run] has completed; empty before that. */
    val planCandidates: List<PlanCandidate> get() = listOfNotNull(candidate)

    /** Exactly one [TaskProposal] once [run] has completed; empty before that. */
    val taskProposals: List<TaskProposal> get() = listOfNotNull(proposal)

    /**
     * Executes the fixed Planning Session for [goal], initiated by
     * [initiatingPrincipalId], recommending [initiatingPrincipalId] itself
     * as proposed owner (this fixed harness never recommends a different
     * owner or assignee -- that would require Plan Decision logic this
     * unit does not implement). [source] and [priority] mirror
     * `TaskProposal.source`/`priority`'s own existing reuse of
     * [RequestOrigin]/[RequestPriority] (`PlannerRuntimeSpecification.md`
     * §10) -- supplied by the caller, not fixed here, since which channel
     * a Planning Request arrived on is upstream information this harness
     * does not invent.
     *
     * Throws [IllegalStateException] if called more than once on the same
     * instance.
     *
     * [targetResourceReferences] (Sprint 1, Unit 11B): caller-supplied
     * [ResourceId]s to carry onto the produced [TaskProposal]'s own
     * [TaskProposal.resourceReferences] -- exactly like [goal] and
     * [correlationId], this harness does not discover or resolve these
     * itself (no [parker.core.interfaces.ResourceRegistry] dependency is
     * added here); it only carries forward what its caller already
     * knows. Defaults to `emptyList()` so every existing call site is
     * unaffected.
     */
    suspend fun run(
        planningSessionId: PlanningSessionId,
        initiatingPrincipalId: PrincipalId,
        goal: String,
        correlationId: String,
        source: RequestOrigin = RequestOrigin.TEXT,
        priority: RequestPriority = RequestPriority.NORMAL,
        targetResourceReferences: List<ResourceId> = emptyList(),
    ) {
        check(currentState == PlanningSessionLifecycleState.CREATED) {
            "DeterministicPlannerHarness.run() may only be called once, from CREATED; current state is $currentState"
        }

        // Enters CREATED (planner.session_created) -- Unit 9: the record this fixed
        // harness represents is considered "created" at the top of run(), not at
        // object construction; see this class's own "Unit 9" KDoc for why.
        publish(
            eventType = "planner.session_created",
            correlationId = correlationId,
            payload = mapOf("initiatingPrincipalId" to initiatingPrincipalId.value, "goal" to goal),
        )

        // CREATED -> CONTEXT_GATHERING: fixed, deterministic Planning Context --
        // Section 9's context categories are not resolved here; this harness's
        // fixed happy path needs none of them to proceed.
        advanceTo(PlanningSessionLifecycleState.CONTEXT_GATHERING)
        publish(eventType = "planner.context_requested", correlationId = correlationId)

        // CONTEXT_GATHERING -> ANALYSING: generate the sole Plan Candidate.
        advanceTo(PlanningSessionLifecycleState.ANALYSING)
        publish(eventType = "planner.analysis_started", correlationId = correlationId)
        val generatedCandidate = PlanCandidate(
            planCandidateId = "$planningSessionId-candidate-1",
            goal = goal,
        )
        candidate = generatedCandidate

        // ANALYSING -> PROPOSING: construct the Task Proposal from the (only)
        // candidate. No Plan Decision comparison occurs -- there is nothing to
        // decide among.
        advanceTo(PlanningSessionLifecycleState.PROPOSING)
        publish(
            eventType = "planner.proposal_created",
            correlationId = correlationId,
            payload = mapOf("planCandidateId" to generatedCandidate.planCandidateId),
        )
        proposal = TaskProposal(
            taskProposalId = TaskProposalId("$planningSessionId-proposal-1"),
            planningSessionId = planningSessionId,
            initiatingPrincipalId = initiatingPrincipalId,
            proposedOwnerPrincipalId = initiatingPrincipalId,
            goal = goal,
            source = source,
            priority = priority,
            resourceReferences = targetResourceReferences,
            rationale = "Selected the sole Plan Candidate generated for this fixed Sprint 1 " +
                "Planning Session (${generatedCandidate.planCandidateId}); no alternative " +
                "candidates were generated or rejected.",
            riskEstimate = RiskEstimate.LOW,
            correlationId = correlationId,
        )

        // PROPOSING -> SUBMITTED: the Task Proposal is well-formed and ready to
        // submit. This harness does not call TaskProposalIntake.submitProposal
        // (Unit 6, not yet implemented) -- see this file's top-level KDoc.
        advanceTo(PlanningSessionLifecycleState.SUBMITTED)
        publish(
            eventType = "planner.proposal_submitted",
            correlationId = correlationId,
            payload = mapOf("taskProposalId" to requireNotNull(proposal).taskProposalId.value),
        )
    }

    private fun advanceTo(next: PlanningSessionLifecycleState) {
        val current = currentState
        PlanningSessionLifecycleTransitions.requireValidTransition(current, next)
        visitedStates += next
    }

    /** Sprint 1, Unit 9: publishes one `planner.*` [ParkerEvent] -- see this class's own "Unit 9" KDoc. */
    private suspend fun publish(eventType: String, correlationId: String, payload: Map<String, String> = emptyMap()) {
        eventBus.publish(
            ParkerEvent(
                eventId = "evt-$correlationId-$eventType",
                publisherPrincipalId = PLANNER_RUNTIME_PRINCIPAL_ID,
                eventType = EventType(eventType),
                timestamp = Instant.now(),
                correlationId = correlationId,
                payload = payload,
            ),
        )
    }
}
