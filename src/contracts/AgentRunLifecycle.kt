package parker.core.interfaces

/**
 * Agent Run lifecycle state machine, transcribed exactly from
 * `AgentRuntimeSpecification.md` Section 5's own diagram. Mirrors
 * [ExecutionLifecycleState]/[ExecutionLifecycleTransitions] and
 * [TaskStatus]/[TaskLifecycleTransitions]'s existing shape exactly.
 *
 *   Created -> {Initialised, Cancelled}
 *   Initialised -> {Ready, Cancelled, Failed}
 *   Ready -> {Running, Cancelled, Failed}
 *   Running -> {WaitingForPermission, WaitingForInput, Suspended, Completed, Failed, Cancelled}
 *   WaitingForPermission -> {Running, Suspended, Failed, Cancelled}
 *   WaitingForInput -> {Running, Suspended, Cancelled}
 *   Suspended -> {Running, Cancelled, Failed}
 *
 * Per Section 5's "Relationship to the Task Manager Task Lifecycle": these
 * ten states belong exclusively to the Agent Run lifecycle. Several names
 * (`CREATED`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`) coincide with
 * [TaskStatus]'s own values -- "a naming coincidence between two
 * deliberately separate state machines, not a shared one." This file does
 * not reuse [TaskStatus] or [TaskLifecycleTransitions] for that reason.
 *
 * Note there is deliberately no `CREATED -> FAILED` edge (unlike
 * [TaskStatus], which does have one): per Section 7, "an Agent Instance
 * cannot proceed past `CREATED`" if its identity is unresolvable -- it
 * stays at `CREATED` rather than moving to a terminal state. This is not
 * an omission; it is the diagram exactly as specified.
 *
 * Added by Sprint 1 Unit 7
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md`), the first unit
 * to need a Kotlin representation of the Agent Run lifecycle.
 * [InMemoryAgentRuntime] only ever drives
 * `CREATED -> INITIALISED -> READY -> RUNNING -> {COMPLETED, FAILED}`; the
 * remaining states and edges (`WAITING_FOR_PERMISSION`,
 * `WAITING_FOR_INPUT`, `SUSPENDED`, and cancellation from every
 * non-terminal state) are transcribed in full here, not invented, because
 * this is a production contract, not a test-only fixture.
 */
enum class AgentRunStatus {
    CREATED,
    INITIALISED,
    READY,
    RUNNING,
    WAITING_FOR_PERMISSION,
    WAITING_FOR_INPUT,
    SUSPENDED,
    COMPLETED,
    FAILED,
    CANCELLED,
}

object AgentRunLifecycleTransitions {

    private val allowed: Map<AgentRunStatus, Set<AgentRunStatus>> = mapOf(
        AgentRunStatus.CREATED to setOf(
            AgentRunStatus.INITIALISED,
            AgentRunStatus.CANCELLED,
        ),
        AgentRunStatus.INITIALISED to setOf(
            AgentRunStatus.READY,
            AgentRunStatus.CANCELLED,
            AgentRunStatus.FAILED,
        ),
        AgentRunStatus.READY to setOf(
            AgentRunStatus.RUNNING,
            AgentRunStatus.CANCELLED,
            AgentRunStatus.FAILED,
        ),
        AgentRunStatus.RUNNING to setOf(
            AgentRunStatus.WAITING_FOR_PERMISSION,
            AgentRunStatus.WAITING_FOR_INPUT,
            AgentRunStatus.SUSPENDED,
            AgentRunStatus.COMPLETED,
            AgentRunStatus.FAILED,
            AgentRunStatus.CANCELLED,
        ),
        AgentRunStatus.WAITING_FOR_PERMISSION to setOf(
            AgentRunStatus.RUNNING,
            AgentRunStatus.SUSPENDED,
            AgentRunStatus.FAILED,
            AgentRunStatus.CANCELLED,
        ),
        AgentRunStatus.WAITING_FOR_INPUT to setOf(
            AgentRunStatus.RUNNING,
            AgentRunStatus.SUSPENDED,
            AgentRunStatus.CANCELLED,
        ),
        AgentRunStatus.SUSPENDED to setOf(
            AgentRunStatus.RUNNING,
            AgentRunStatus.CANCELLED,
            AgentRunStatus.FAILED,
        ),
        AgentRunStatus.COMPLETED to emptySet(),
        AgentRunStatus.FAILED to emptySet(),
        AgentRunStatus.CANCELLED to emptySet(),
    )

    fun isTerminal(status: AgentRunStatus): Boolean = allowed.getValue(status).isEmpty()

    fun isValidTransition(from: AgentRunStatus, to: AgentRunStatus): Boolean = to in allowed.getValue(from)

    /** Throws [IllegalArgumentException] if `from -> to` is not a permitted edge in the diagram. */
    fun requireValidTransition(from: AgentRunStatus, to: AgentRunStatus) {
        require(isValidTransition(from, to)) {
            "Illegal Agent Run lifecycle transition: $from -> $to"
        }
    }
}
