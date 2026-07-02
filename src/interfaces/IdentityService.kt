package parker.core.interfaces

/**
 * Identity Service (`docs/architecture/IdentityService.md`). Promotes that
 * document's proposed interface shape to a real Volume 3 interface and
 * concrete implementation, per this explicitly-declared implementation
 * phase (closes IMPLEMENTATION_GAPS.md #1's "implementation still
 * deferred" status; ADR-022's "must not be added until runtime
 * architecture reaches the implementation phase" condition is now
 * satisfied for this interface specifically).
 *
 * Per IdentityService.md, this interface answers "who is this?" only --
 * never "are they allowed?" (Chapter 41: "Identity Is Not Permission").
 * It does not evaluate policy, does not implement authentication
 * providers, and is deliberately NOT wired into `PermissionEngine.evaluate`
 * in this phase -- recorded as a follow-up, not done silently here.
 */
interface IdentityService {

    /**
     * Registers [principal] at lifecycle state CREATED (IdentityService.md
     * "Interfaces (Proposed)"). See `InMemoryIdentityService.register`'s
     * KDoc for the exact validation rules (unique principalId, CREATED
     * status, and an already-established owning context for any
     * PrincipalType other than USER or SYSTEM).
     */
    suspend fun register(principal: Principal): PrincipalId

    /**
     * The primary read path. Returns null for an unknown [principalId] --
     * never throws for a not-found lookup, mirroring
     * [ResourceRegistry.resolve]'s established pattern.
     */
    suspend fun resolve(principalId: PrincipalId): Principal?

    /**
     * The only sanctioned way to move a Principal through
     * [PrincipalStatus]. Enforces [PrincipalLifecycleTransitions]. Throws
     * if [principalId] is not registered, or if the current status ->
     * [status] is not a valid edge.
     */
    suspend fun updateStatus(principalId: PrincipalId, status: PrincipalStatus): Principal

    /**
     * Updates `lastSeenAt` to the current time. Does not itself change
     * `status` -- this is metadata only, never a side-channel for
     * lifecycle changes (IdentityService.md "touch"). Throws if
     * [principalId] is not registered.
     */
    suspend fun touch(principalId: PrincipalId): Principal

    /** Supports delegation/ownership queries, mirroring [ResourceRegistry.listByOwner]. */
    suspend fun listByOwner(owner: PrincipalId): List<Principal>
}
