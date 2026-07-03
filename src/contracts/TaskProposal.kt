package parker.core.interfaces

/**
 * Sprint 1 contract closing Blocker 1 and Blocker 2 of
 * docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md (see
 * docs/implementation/SPRINT_1_BLOCKER_CLOSURE.md for the full closure
 * record). This file is a **pure contract addition**: data classes, a
 * sealed result type, value-class identifiers, and one small interface.
 * It adds no runtime behaviour and modifies no existing file.
 *
 * Grounded directly in already-existing prose, not invented here:
 * - Field shapes mirror `PlannerRuntimeSpecification.md` Section 10
 *   ("Proposal Model") field-by-field, using the same "(maps to
 *   Task-Schema.md)" / "(maps to Task Manager Runtime Specification,
 *   proposed)" / "(Planner-only, proposed)" distinctions that document
 *   already draws.
 * - The five disposition outcomes ([TaskProposalDisposition]'s five
 *   subclasses) are named, verbatim, by
 *   `TaskManagerRuntimeSpecification.md` Section 6 ("The Task Manager
 *   Runtime may reject, defer, split, merge, or accept a Task Proposal.
 *   All five dispositions are the Task Manager Runtime's own
 *   prerogative.") and restated identically from the Planner's own side
 *   in `PlannerRuntimeSpecification.md` Section 6.
 *
 * Per AD-005 (Planner Never Creates Tasks) and AD-004 (Task Manager Owns
 * Canonical Tasks): a [TaskProposal] is not, and must never be treated
 * as, a Task Manager Task. It becomes a canonical Task Manager Task
 * (identified by [TaskId]) only via [TaskProposalDisposition.Accepted],
 * [TaskProposalDisposition.Split], or [TaskProposalDisposition.Merged] --
 * a decision this file names the shape of, but does not itself make or
 * implement. [TaskProposalIntake] declares the operation signature only;
 * no implementation of it is added by this change.
 */

/**
 * A Planning Session's identifier
 * (`PlannerRuntimeSpecification.md` Section 4, Section 5). First Kotlin
 * representation of this concept -- the Planner Runtime itself has no
 * other Kotlin yet (it remains specification-only per
 * `IMPLEMENTATION_ORDER.md`); this type exists here only because
 * [TaskProposal] must reference the Planning Session that produced it.
 */
@JvmInline
value class PlanningSessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "PlanningSessionId must not be blank" }
    }
}

/**
 * A Task Proposal's own identifier
 * (`PlannerRuntimeSpecification.md` Section 4, Section 10). Distinct from
 * [TaskId]: a Task Proposal is a recommendation, never itself a Task
 * Manager Task (AD-005), so it carries its own identifier space rather
 * than borrowing the Task's.
 */
@JvmInline
value class TaskProposalId(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskProposalId must not be blank" }
    }
}

/**
 * A Task Manager Task's identifier -- `Task-Schema.md`'s `taskId` field
 * and `TaskManagerRuntimeSpecification.md` Section 4's "Task ID". First
 * Kotlin representation of this concept: the Task Manager Runtime itself
 * has no other Kotlin yet (`IMPLEMENTATION_ORDER.md` Section 3 lists it
 * as a "corrected draft" specification, not yet promoted to an
 * implementation phase). This type exists here only because
 * [TaskProposalDisposition] must be able to name which Task Manager
 * Task(s) a disposition resulted in. A full Task Manager Task Kotlin
 * type (fields, lifecycle transitions) remains Sprint 1 coding work, not
 * introduced by this change.
 */
@JvmInline
value class TaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskId must not be blank" }
    }
}

/**
 * A Task Proposal's Dependency (`PlannerRuntimeSpecification.md` Section
 * 4, Section 10), which may point at an already-existing Task Manager
 * Task, or at another Task Proposal in the same Planning Session that
 * does not yet exist as a Task. Modelled as a sealed type rather than a
 * single reference field precisely because the specification itself
 * distinguishes these two cases and leaves how the Task Manager Runtime
 * should resolve the second one (ordering, atomicity) as an open
 * question -- this type does not resolve that question, only names the
 * two cases distinctly so a resolution can be built later without a
 * breaking change to this shape.
 */
sealed class ProposalDependency {
    data class OnExistingTask(val taskId: TaskId) : ProposalDependency()
    data class OnProposal(val taskProposalId: TaskProposalId) : ProposalDependency()
}

/**
 * The Planner Runtime's output (`PlannerRuntimeSpecification.md` Section
 * 4, Section 10): a structured, not-yet-authoritative recommendation for
 * a Task Manager Task. Field-by-field provenance:
 *
 * - [taskProposalId], [planningSessionId]: this proposal's own identity
 *   and the Planning Session that produced it.
 * - [initiatingPrincipalId]: the Planning Session's initiating Principal
 *   (Section 8) -- resolved through the Identity Service like any other
 *   `PrincipalId` in this repository, never a Planner-local identity.
 * - [proposedOwnerPrincipalId], [proposedAssigneePrincipalId]: Section
 *   10's "Proposed owner" / "Proposed assignee" -- recommendations only;
 *   Section 6 states plainly the Task Manager Runtime "is not bound to
 *   accept this recommendation as given."
 * - [goal]: the Goal this proposal is meant to progress (Section 4;
 *   already defined by `AgentRuntimeSpecification.md` Section 4 and
 *   reused, not redefined, here). Represented as free text, mirroring
 *   `ExecutionRequest.intent`'s existing treatment -- no Goal Kotlin type
 *   exists anywhere in this repository yet, and inventing one is outside
 *   this change's scope.
 * - [source]: Section 10 proposes this reuse the existing [RequestOrigin]
 *   enum, for the same reason `TaskManagerRuntimeSpecification.md`
 *   Section 4 already proposes Task Source do so.
 * - [priority]: Section 10 proposes this reuse the existing
 *   [RequestPriority] enum, symmetrically.
 * - [constraints]: Section 4's Constraint has no formal schema anywhere
 *   in this repository -- represented here as free-text descriptions,
 *   deliberately unstructured pending a future Constraint schema
 *   decision this change does not make.
 * - [dependencies]: Section 4/Section 10's Dependency set; see
 *   [ProposalDependency].
 * - [requiredCapabilities]: Section 10's "Required capabilities" --  a
 *   planning-time hint referencing the kinds of Agent Capability
 *   (`AgentRuntimeSpecification.md` Section 4) or Tool capability an
 *   eventual Agent Run might need, represented using the same
 *   [PermissionAction] vocabulary `ToolDescriptor.supportedActions`
 *   already uses for an equivalent purpose -- not a grant, per Section 7.
 * - [expectedOutputs]: Section 10's free-text description of what a
 *   successful outcome should produce.
 * - [anticipatedPermissionActions]: Section 10's "Permission
 *   requirements" -- the Planner Runtime's own anticipated
 *   [PermissionAction] implications. Section 8 is explicit this is
 *   "advisory labelling... not a `PermissionDecision`" -- this field
 *   carries no authority and must never be read as one (AD-007, AD-015).
 * - [contextReferences]: Section 4's Context Reference -- opaque
 *   identifiers into Planning Context, resolved only by the Planner
 *   Runtime that produced them (Section 9), never copied inline.
 * - [rationale]: Section 10's free-text explanation of why this
 *   proposal's Plan Candidate was selected.
 * - [riskEstimate]: Section 10 proposes this reuse the existing
 *   [RiskEstimate] enum, nullable since Section 10 does not require it.
 * - [correlationId]: not itself a named Section 10 field, but required
 *   for consistency with every other `ParkerEvent`-adjacent object in
 *   this repository (`ExecutionRequest.correlationId`,
 *   `ParkerEvent.correlationId`) -- carries the causal-sequence
 *   correlation a `planner.proposal_submitted` /
 *   `task.created` event pair must share to be reconstructable
 *   (`PlannerRuntimeSpecification.md` Section 11; `EventBus.md`
 *   "Ordering").
 *
 * Deliberately omitted: Section 10's optional "Confidence" field. Not
 * required by the Sprint 1 blocker-closure task's own field list, and
 * omitting an optional, explicitly-not-mandated field is not a departure
 * from the specification -- Section 10 itself states no downstream
 * component is specified to require it.
 */
data class TaskProposal(
    val taskProposalId: TaskProposalId,
    val planningSessionId: PlanningSessionId,
    val initiatingPrincipalId: PrincipalId,
    val proposedOwnerPrincipalId: PrincipalId,
    val proposedAssigneePrincipalId: PrincipalId? = null,
    val goal: String,
    val source: RequestOrigin,
    val priority: RequestPriority,
    val constraints: List<String> = emptyList(),
    val dependencies: List<ProposalDependency> = emptyList(),
    val requiredCapabilities: Set<PermissionAction> = emptySet(),
    val expectedOutputs: String = "",
    val anticipatedPermissionActions: Set<PermissionAction> = emptySet(),
    val contextReferences: List<String> = emptyList(),
    val rationale: String = "",
    val riskEstimate: RiskEstimate? = null,
    val correlationId: String,
) {
    init {
        require(goal.isNotBlank()) { "TaskProposal.goal must not be blank" }
        require(correlationId.isNotBlank()) { "TaskProposal.correlationId must not be blank" }
    }
}

/**
 * The Task Manager Runtime's response to a submitted [TaskProposal]
 * (`TaskManagerRuntimeSpecification.md` Section 6; `PlannerRuntimeSpecification.md`
 * Section 6). All five subclasses are **terminal for the proposal**: none
 * of them can be revised in place, only superseded by a new
 * [TaskProposal] submission, consistent with AD-016 (Terminal Lifecycle
 * States Are Final) applied to the proposal's own disposition rather than
 * to a runtime lifecycle state machine.
 *
 * - [Accepted]: exactly one Task Manager Task is created from the
 *   proposal. A Task Manager Task now exists; the Task Manager Runtime
 *   MAY later involve the Agent Runtime for it (via
 *   [AgentRunCommand]/[AgentRunCommandType.START]), but does not have to.
 *   No further Planner input is needed for this disposition itself.
 *   Required event implication: `task.created` (Task Manager Runtime
 *   Specification Section 10) and `planner.session_completed`
 *   (Planner Runtime Specification Section 11) both fire, sharing this
 *   proposal's [TaskProposal.correlationId].
 * - [Deferred]: no Task Manager Task is created yet. The proposal is not
 *   rejected -- the Task Manager Runtime may reconsider it later (e.g.
 *   once a [ProposalDependency] resolves), but this disposition itself is
 *   still terminal for *this* submission: reconsideration means a new
 *   submission, not a mutation of this result, mirroring the "no
 *   resurrection from a terminal state" pattern this repository applies
 *   everywhere else. Further Planner input MAY be needed if the deferral
 *   reason names something only the Planner Runtime can supply (e.g. a
 *   revised Constraint) -- this type does not decide that, it only
 *   carries the reason. Agent Runtime is not involved for a Deferred
 *   proposal. Required event implication: no `task.created`; this
 *   repository's existing event tables have no dedicated
 *   `planner.session_deferred` event, and this file does not add one --
 *   see docs/implementation/SPRINT_1_BLOCKER_CLOSURE.md's Open Questions.
 * - [Rejected]: no Task Manager Task is created, and none ever will be
 *   for this proposal. No further Planner input is relevant to this
 *   specific proposal (a new Planning Session may of course produce a
 *   new one). Agent Runtime is never involved. Required event
 *   implication: `planner.session_failed` does not apply (that is an
 *   internal Planner failure, per `PlannerRuntimeSpecification.md`
 *   Section 5's `FAILED`/`REJECTED` distinction, itself an application of
 *   AD-015); this is the external-decision case, and the Planning
 *   Session transitions `SUBMITTED --> REJECTED`. No dedicated Task Event
 *   fires, since no Task exists to attach one to.
 * - [Split]: two or more Task Manager Tasks are created from the one
 *   proposal (`PlannerRuntimeSpecification.md` Section 6: "the Task
 *   Manager Runtime may realise zero, one, or several Task Manager Tasks
 *   from a single Task Proposal"). No further Planner input is needed for
 *   the split itself. Agent Runtime MAY later be involved for any subset
 *   of the resulting Tasks independently. Required event implication: one
 *   `task.created` per resulting Task, all sharing this proposal's
 *   `correlationId`, plus `planner.session_completed`.
 * - [Merged]: exactly one Task Manager Task is created, combining this
 *   proposal with one or more sibling proposals from the same (or a
 *   related) Planning Session. No further Planner input is needed. Agent
 *   Runtime MAY later be involved for the merged Task. Required event
 *   implication: one `task.created` (not one per merged proposal), plus
 *   one `planner.session_completed` per merged proposal's own Planning
 *   Session (Section 6 does not resolve whether merges can span more than
 *   one Planning Session -- if they do, each session's own completion
 *   event still fires independently).
 *
 * None of these five outcomes grants execution authority by itself
 * (AD-002, AD-007): creating a Task Manager Task is an orchestration
 * decision, never a substitute for a later, separate Permission Engine
 * evaluation of any `ExecutionRequest` the resulting Task eventually
 * causes.
 */
sealed class TaskProposalDisposition {
    abstract val taskProposalId: TaskProposalId

    data class Accepted(
        override val taskProposalId: TaskProposalId,
        val taskId: TaskId,
    ) : TaskProposalDisposition()

    data class Deferred(
        override val taskProposalId: TaskProposalId,
        val reason: String,
    ) : TaskProposalDisposition() {
        init {
            require(reason.isNotBlank()) { "TaskProposalDisposition.Deferred.reason must not be blank" }
        }
    }

    data class Rejected(
        override val taskProposalId: TaskProposalId,
        val reason: String,
    ) : TaskProposalDisposition() {
        init {
            require(reason.isNotBlank()) { "TaskProposalDisposition.Rejected.reason must not be blank" }
        }
    }

    data class Split(
        override val taskProposalId: TaskProposalId,
        val taskIds: List<TaskId>,
    ) : TaskProposalDisposition() {
        init {
            require(taskIds.size >= 2) { "TaskProposalDisposition.Split must produce at least two Task Manager Tasks" }
        }
    }

    data class Merged(
        override val taskProposalId: TaskProposalId,
        val taskId: TaskId,
        val mergedWithProposalIds: List<TaskProposalId>,
    ) : TaskProposalDisposition() {
        init {
            require(mergedWithProposalIds.isNotEmpty()) {
                "TaskProposalDisposition.Merged must name at least one sibling proposal it was merged with"
            }
        }
    }
}

/**
 * The intake operation named, but not shaped, by
 * `TaskManagerRuntimeSpecification.md` (no Section 7-level definition
 * existed prior to this change) and depended on, but not defined, by
 * `PlannerRuntimeSpecification.md` Section 6 ("This document depends on
 * that disposition existing... but does not invent the mechanism itself").
 *
 * This interface declares the operation's signature only. **No
 * implementation of this interface exists in this repository as of this
 * change** -- providing one (an in-memory Task Manager Runtime that
 * actually creates Task Manager Tasks) is Sprint 1 coding work, per
 * `docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` Unit 6, not this
 * contract-preparation change.
 */
interface TaskProposalIntake {
    suspend fun submitProposal(proposal: TaskProposal): TaskProposalDisposition
}
