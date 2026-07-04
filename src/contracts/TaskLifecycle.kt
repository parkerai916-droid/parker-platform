package parker.core.interfaces

/**
 * Task Manager Task lifecycle state machine, transcribed exactly from
 * `docs/diagrams/task-lifecycle-state-machine.mmd` (itself derived from
 * `Task.schema.json`'s `status` enum), per
 * `TaskManagerRuntimeSpecification.md` Section 5's own instruction: "This
 * section reproduces docs/diagrams/task-lifecycle-state-machine.mmd
 * exactly... no invented branching." Mirrors [ExecutionLifecycleState] /
 * [ExecutionLifecycleTransitions]'s existing shape exactly
 * (`src/contracts/ExecutionLifecycle.kt`).
 *
 *   Created -> {Queued, Cancelled, Superseded}
 *   Queued -> {Running, Cancelled, Expired, Superseded}
 *   Running -> {Paused, Completed, Failed, Cancelled}
 *   Paused -> {Running, Cancelled, Expired, Superseded}
 *
 * `Superseded` is not reachable from `Running` -- a deliberate,
 * already-specified design choice (`Task-Schema.md`; restated in
 * `TaskManagerRuntimeSpecification.md` Section 5's "Invalid transitions"),
 * not an omission introduced here. `Failed` is reachable only from
 * `Running`, per the same section.
 *
 * This is added by Sprint 1 Unit 6
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md`), the first unit
 * to need a Kotlin representation of the Task Manager Task lifecycle --
 * `src/contracts/TaskProposal.kt`'s own doc comment noted "a full Task
 * Manager Task Kotlin type (fields, lifecycle transitions) remains Sprint
 * 1 coding work." Unit 6 itself only ever drives `CREATED -> QUEUED`; the
 * remaining states and edges are transcribed in full here (not invented)
 * because this is a production contract, not a test-only fixture, and a
 * partial lifecycle would misrepresent the already-specified state
 * machine for whatever later unit or component drives the rest of it.
 */
enum class TaskStatus {
    CREATED,
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED,
    SUPERSEDED,
}

object TaskLifecycleTransitions {

    private val allowed: Map<TaskStatus, Set<TaskStatus>> = mapOf(
        TaskStatus.CREATED to setOf(
            TaskStatus.QUEUED,
            TaskStatus.CANCELLED,
            TaskStatus.SUPERSEDED,
        ),
        TaskStatus.QUEUED to setOf(
            TaskStatus.RUNNING,
            TaskStatus.CANCELLED,
            TaskStatus.EXPIRED,
            TaskStatus.SUPERSEDED,
        ),
        TaskStatus.RUNNING to setOf(
            TaskStatus.PAUSED,
            TaskStatus.COMPLETED,
            TaskStatus.FAILED,
            TaskStatus.CANCELLED,
        ),
        TaskStatus.PAUSED to setOf(
            TaskStatus.RUNNING,
            TaskStatus.CANCELLED,
            TaskStatus.EXPIRED,
            TaskStatus.SUPERSEDED,
        ),
        TaskStatus.COMPLETED to emptySet(),
        TaskStatus.FAILED to emptySet(),
        TaskStatus.CANCELLED to emptySet(),
        TaskStatus.EXPIRED to emptySet(),
        TaskStatus.SUPERSEDED to emptySet(),
    )

    fun isTerminal(status: TaskStatus): Boolean = allowed.getValue(status).isEmpty()

    fun isValidTransition(from: TaskStatus, to: TaskStatus): Boolean = to in allowed.getValue(from)

    /** Throws [IllegalArgumentException] if `from -> to` is not a permitted edge in the diagram. */
    fun requireValidTransition(from: TaskStatus, to: TaskStatus) {
        require(isValidTransition(from, to)) {
            "Illegal Task Manager Task lifecycle transition: $from -> $to"
        }
    }
}
