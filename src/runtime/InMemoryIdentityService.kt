package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.IdentityService
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalLifecycleTransitions
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType

/**
 * In-memory implementation of [IdentityService], per
 * `docs/architecture/IdentityService.md`. Implements exactly what that
 * document specifies for this phase:
 *
 * - Registration: unique principalId, a CREATED-state entry, and an
 *   already-established owning context for any PrincipalType other than
 *   USER/SYSTEM ("Interfaces (Proposed)" / "Trust Relationships").
 * - Resolution: never throws for a not-found lookup.
 * - Lifecycle: [PrincipalLifecycleTransitions] is the sole enforcement
 *   point for status changes (IdentityService.md "Lifecycle").
 * - lastSeenAt tracking via [touch], which cannot itself change status --
 *   structurally unable to bypass lifecycle rules.
 * - Ownership queries via [listByOwner].
 *
 * NOT implemented (recorded in IMPLEMENTATION_GAPS.md, not invented
 * around): cascading revocation of owned Principals, publishing
 * `identity.*` events to the Event Bus, real authentication providers,
 * and PermissionEngine integration -- all explicitly out of this phase's
 * scope.
 */
class InMemoryIdentityService : IdentityService {

    private val mutex = Mutex()
    private val principals = mutableMapOf<PrincipalId, Principal>()

    /**
     * Validates, per IdentityService.md:
     * - [principal.principalId] must not already be registered (duplicate
     *   registration fails deterministically, every time, via the same
     *   exception).
     * - [principal.status] must be CREATED -- "register creates a new
     *   Principal at lifecycle state Created" is read as a requirement on
     *   the input, not a silent overwrite of whatever status was passed.
     * - If [principal.principalType] is anything other than USER or
     *   SYSTEM, [principal.owner] must be non-null and must already
     *   resolve to a registered Principal ("requires an already-
     *   established owning context"). USER and SYSTEM Principals may
     *   register with a null owner (root identities) -- this is not
     *   itself forbidden for other types, only *required* for them.
     */
    override suspend fun register(principal: Principal): PrincipalId = mutex.withLock {
        require(principal.principalId !in principals) {
            "Principal '${principal.principalId.value}' is already registered"
        }
        require(principal.status == PrincipalStatus.CREATED) {
            "A newly registered Principal must have status CREATED, was ${principal.status}"
        }
        if (principal.principalType != PrincipalType.USER && principal.principalType != PrincipalType.SYSTEM) {
            val owner = principal.owner
                ?: throw IllegalArgumentException(
                    "Principal '${principal.principalId.value}' of type ${principal.principalType} " +
                        "requires an owner (IdentityService.md: an already-established owning " +
                        "context is required for any PrincipalType other than USER or SYSTEM)",
                )
            require(owner in principals) {
                "Principal '${principal.principalId.value}''s owner '${owner.value}' is not a registered Principal"
            }
        }

        principals[principal.principalId] = principal
        principal.principalId
    }

    override suspend fun resolve(principalId: PrincipalId): Principal? = mutex.withLock {
        principals[principalId]
    }

    override suspend fun updateStatus(principalId: PrincipalId, status: PrincipalStatus): Principal = mutex.withLock {
        val current = principals[principalId]
            ?: throw NoSuchElementException("No registered Principal for principalId '${principalId.value}'")
        PrincipalLifecycleTransitions.requireValidTransition(current.status, status)
        val updated = current.copy(status = status)
        principals[principalId] = updated
        updated
    }

    override suspend fun touch(principalId: PrincipalId): Principal = mutex.withLock {
        val current = principals[principalId]
            ?: throw NoSuchElementException("No registered Principal for principalId '${principalId.value}'")
        val updated = current.copy(lastSeenAt = Instant.now())
        principals[principalId] = updated
        updated
    }

    override suspend fun listByOwner(owner: PrincipalId): List<Principal> = mutex.withLock {
        principals.values.filter { it.owner == owner }
    }
}
