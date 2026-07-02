package parker.core.interfaces

/**
 * Tool Registry runtime lifecycle (`docs/architecture/tool-registry.md`
 * "Runtime Lifecycle"): Registered -> Enabled -> Disabled -> Deprecated ->
 * Removed, transcribed exactly from that document's stateDiagram-v2 (also
 * mirrored in `docs/diagrams/tool-lifecycle-state-machine.mmd` -- see
 * IMPLEMENTATION_GAPS.md for why that diagram file does not exist yet:
 * this phase adds the Kotlin validator directly from the architecture
 * doc's diagram rather than waiting on a standalone .mmd file that says
 * the same thing).
 */
enum class ToolLifecycleState {
    REGISTERED,
    ENABLED,
    DISABLED,
    DEPRECATED,
    REMOVED,
}

/**
 * Transition validator for [ToolLifecycleState], following the same
 * pattern as [ExecutionLifecycleTransitions]: a fixed adjacency map, an
 * `isValidTransition` check, and a `requireValidTransition` that throws.
 * Edges match tool-registry.md's diagram exactly:
 *
 *   Registered -> Enabled
 *   Enabled -> Disabled, Deprecated
 *   Disabled -> Enabled, Removed
 *   Deprecated -> Removed
 *   Removed -> (terminal)
 */
object ToolLifecycleTransitions {

    private val allowed: Map<ToolLifecycleState, Set<ToolLifecycleState>> = mapOf(
        ToolLifecycleState.REGISTERED to setOf(ToolLifecycleState.ENABLED),
        ToolLifecycleState.ENABLED to setOf(ToolLifecycleState.DISABLED, ToolLifecycleState.DEPRECATED),
        ToolLifecycleState.DISABLED to setOf(ToolLifecycleState.ENABLED, ToolLifecycleState.REMOVED),
        ToolLifecycleState.DEPRECATED to setOf(ToolLifecycleState.REMOVED),
        ToolLifecycleState.REMOVED to emptySet(),
    )

    fun isTerminal(state: ToolLifecycleState): Boolean = allowed.getValue(state).isEmpty()

    fun isValidTransition(from: ToolLifecycleState, to: ToolLifecycleState): Boolean =
        to in allowed.getValue(from)

    /** Throws [IllegalArgumentException] if `from -> to` is not a permitted edge in tool-registry.md's diagram. */
    fun requireValidTransition(from: ToolLifecycleState, to: ToolLifecycleState) {
        require(isValidTransition(from, to)) {
            "Illegal Tool lifecycle transition: $from -> $to"
        }
    }
}
