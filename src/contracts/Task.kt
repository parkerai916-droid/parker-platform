package parker.core.interfaces

/**
 * The platform's canonical unit of tracked work
 * (`TaskManagerRuntimeSpecification.md` Section 4, "Task Manager Task";
 * Chapter 37; `Task-Schema.md`; ADR-012). First Kotlin representation of
 * this concept -- `src/contracts/TaskProposal.kt` already named [TaskId]
 * because a [TaskProposal] must reference it, but deliberately left the
 * full Task type for Sprint 1 coding work
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` Unit 6), not
 * introduced there.
 *
 * Deliberately minimal: only the fields Unit 6's own scope needs --
 * accepting a [TaskProposal] and producing a `Created -> Queued` Task.
 * Section 4's other Core Concepts (Task
 * Dependency, Task Constraint, Task Result, Task Context Reference, Agent
 * Run Reference, Execution Reference) are real, specified concepts this
 * file does not add fields for -- introducing them now, with no unit that
 * yet reads or writes them, would be speculative. Field-by-field
 * provenance:
 *
 * - [taskId]: **(schema: `taskId`).**
 * - [ownerPrincipalId]: **(schema: `ownerPrincipalId`).** Resolved through
 *   the Identity Service by whatever creates a [Task] -- never a
 *   Task-Manager-local store (Section 8).
 * - [assigneePrincipalId]: **(proposed).** Section 4's Task Assignee.
 *   Nullable: "A Task MAY have no Assignee (unassigned, e.g. freshly
 *   `CREATED`)."
 * - [status]: **(schema: `status`).** See [TaskStatus].
 * - [source]: **(proposed, aligned to `RequestOrigin`).** Section 4's Task
 *   Source, reusing the same enum `TaskProposal.source` already reuses,
 *   for the same reason.
 * - [goal]: Section 4's Task Goal, free text, mirroring
 *   `TaskProposal.goal`'s identical treatment.
 * - [priority]: **(proposed, aligned to `RequestPriority`).** Section 4's
 *   Task Priority.
 * - [correlationId]: not itself a named Section 4 field, but required for
 *   consistency with every other correlation-bearing type in this
 *   repository, and needed so a `task.created` event (Section 10,
 *   published by `InMemoryTaskManagerRuntime` as of Sprint 1, Unit 9) can
 *   share it with this Task's originating [TaskProposal] and Planning
 *   Session.
 * - [originatingTaskProposalId]: not a Section 4 field at all -- this
 *   file's own addition, so a [Task] can be traced back to the
 *   [TaskProposal] that produced it (Section 15:
 *   "exactly one \[Task Manager Task\]... per Section 5's existing
 *   lifecycle (enters Created)"). Recorded here rather than silently
 *   discarded once accepted.
 */
data class Task(
    val taskId: TaskId,
    val ownerPrincipalId: PrincipalId,
    val assigneePrincipalId: PrincipalId? = null,
    val status: TaskStatus,
    val source: RequestOrigin,
    val goal: String,
    val priority: RequestPriority,
    val correlationId: String,
    val originatingTaskProposalId: TaskProposalId,
) {
    init {
        require(goal.isNotBlank()) { "Task.goal must not be blank" }
        require(correlationId.isNotBlank()) { "Task.correlationId must not be blank" }
    }
}
