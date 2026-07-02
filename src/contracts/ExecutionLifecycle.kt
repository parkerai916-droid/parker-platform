package parker.core.interfaces

/**
 * ExecutionRequest lifecycle state machine, transcribed exactly from
 * docs/diagrams/execution-state-machine.mmd:
 *
 *   Created -> Validated -> PermissionPending -> {Approved, Denied, Deferred}
 *   Approved -> Queued -> Executing -> {Completed, Failed}
 *   Queued -> Cancelled
 *   Created -> Expired
 *
 * This is the one lifecycle in Volume 1 with an actual diagram behind it
 * (unlike Principal's or Resource's, which are prose-only linear chains --
 * see IMPLEMENTATION_GAPS.md for why those two don't get a validator here).
 */
enum class ExecutionLifecycleState {
    CREATED,
    VALIDATED,
    PERMISSION_PENDING,
    APPROVED,
    DENIED,
    DEFERRED,
    QUEUED,
    EXECUTING,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED,
}

object ExecutionLifecycleTransitions {

    private val allowed: Map<ExecutionLifecycleState, Set<ExecutionLifecycleState>> = mapOf(
        ExecutionLifecycleState.CREATED to setOf(
            ExecutionLifecycleState.VALIDATED,
            ExecutionLifecycleState.EXPIRED,
        ),
        ExecutionLifecycleState.VALIDATED to setOf(
            ExecutionLifecycleState.PERMISSION_PENDING,
        ),
        ExecutionLifecycleState.PERMISSION_PENDING to setOf(
            ExecutionLifecycleState.APPROVED,
            ExecutionLifecycleState.DENIED,
            ExecutionLifecycleState.DEFERRED,
        ),
        ExecutionLifecycleState.APPROVED to setOf(
            ExecutionLifecycleState.QUEUED,
        ),
        ExecutionLifecycleState.QUEUED to setOf(
            ExecutionLifecycleState.EXECUTING,
            ExecutionLifecycleState.CANCELLED,
        ),
        ExecutionLifecycleState.EXECUTING to setOf(
            ExecutionLifecycleState.COMPLETED,
            ExecutionLifecycleState.FAILED,
        ),
        ExecutionLifecycleState.COMPLETED to emptySet(),
        ExecutionLifecycleState.FAILED to emptySet(),
        ExecutionLifecycleState.DENIED to emptySet(),
        ExecutionLifecycleState.DEFERRED to emptySet(),
        ExecutionLifecycleState.CANCELLED to emptySet(),
        ExecutionLifecycleState.EXPIRED to emptySet(),
    )

    fun isTerminal(state: ExecutionLifecycleState): Boolean = allowed.getValue(state).isEmpty()

    fun isValidTransition(from: ExecutionLifecycleState, to: ExecutionLifecycleState): Boolean =
        to in allowed.getValue(from)

    /** Throws [IllegalArgumentException] if `from -> to` is not a permitted edge in the diagram. */
    fun requireValidTransition(from: ExecutionLifecycleState, to: ExecutionLifecycleState) {
        require(isValidTransition(from, to)) {
            "Illegal ExecutionRequest lifecycle transition: $from -> $to"
        }
    }
}
