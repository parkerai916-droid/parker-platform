package parker.core.interfaces

/**
 * Transition validator for [PrincipalStatus] (defined in `Principal.kt`),
 * following the same pattern as `ExecutionLifecycleTransitions` and
 * `ToolLifecycleTransitions`: a fixed adjacency map, `isValidTransition`,
 * and `requireValidTransition`.
 *
 * Edges match `docs/diagrams/principal-lifecycle-state-machine.mmd`
 * exactly -- a literal linear chain, no branching:
 *
 *   Created -> Active -> Suspended -> Revoked -> Archived
 *
 * There is deliberately no `Suspended -> Active` reactivation edge and no
 * `Active -> Revoked` direct edge: `Principal.md` and the diagram do not
 * specify either, and `IMPLEMENTATION_GAPS.md` #5 explicitly defers
 * inventing branches for this lifecycle. A practical consequence: a
 * Principal cannot be revoked without first passing through Suspended.
 * Whether that is the intended real-world rule is recorded as an open
 * question in `IMPLEMENTATION_GAPS.md`, not decided here.
 */
object PrincipalLifecycleTransitions {

    private val allowed: Map<PrincipalStatus, Set<PrincipalStatus>> = mapOf(
        PrincipalStatus.CREATED to setOf(PrincipalStatus.ACTIVE),
        PrincipalStatus.ACTIVE to setOf(PrincipalStatus.SUSPENDED),
        PrincipalStatus.SUSPENDED to setOf(PrincipalStatus.REVOKED),
        PrincipalStatus.REVOKED to setOf(PrincipalStatus.ARCHIVED),
        PrincipalStatus.ARCHIVED to emptySet(),
    )

    fun isTerminal(status: PrincipalStatus): Boolean = allowed.getValue(status).isEmpty()

    fun isValidTransition(from: PrincipalStatus, to: PrincipalStatus): Boolean =
        to in allowed.getValue(from)

    /** Throws [IllegalArgumentException] if `from -> to` is not a permitted edge in the diagram. */
    fun requireValidTransition(from: PrincipalStatus, to: PrincipalStatus) {
        require(isValidTransition(from, to)) {
            "Illegal Principal lifecycle transition: $from -> $to"
        }
    }
}
