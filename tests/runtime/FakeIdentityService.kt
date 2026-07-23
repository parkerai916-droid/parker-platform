package parker.core.runtime

import parker.core.interfaces.IdentityService
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus

/**
 * Test-only fake, mirroring [FakeCommunicationIntake]/[FakePermissionEngine]/
 * [FakeResourceRegistry]/[FakeExecutionPipeline]'s lambda-based fake
 * precedent. Exists so [ResponseComposerTest] can prove
 * [ResponseComposer]'s own per-branch identity-resolution behaviour --
 * exactly one [resolve] call for `Reply`, zero for `Goal`/`NoAction` --
 * per `docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md` Section 7,
 * independently of any real [IdentityService] implementation.
 *
 * Only [resolve] is exercised by [ResponseComposer] (Scope Lock
 * Section 6); [register]/[updateStatus]/[touch]/[listByOwner] are never
 * called by it, so they throw if reached -- a structural guard against
 * this fake silently masking an unexpected dependency on a method
 * [ResponseComposer] must not call.
 */
class FakeIdentityService(
    private val principalFor: (PrincipalId) -> Principal?,
) : IdentityService {

    var resolveCallCount: Int = 0
        private set

    override suspend fun resolve(principalId: PrincipalId): Principal? {
        resolveCallCount++
        return principalFor(principalId)
    }

    override suspend fun register(principal: Principal): PrincipalId {
        throw UnsupportedOperationException("FakeIdentityService.register must not be called by ResponseComposer")
    }

    override suspend fun updateStatus(principalId: PrincipalId, status: PrincipalStatus): Principal {
        throw UnsupportedOperationException("FakeIdentityService.updateStatus must not be called by ResponseComposer")
    }

    override suspend fun touch(principalId: PrincipalId): Principal {
        throw UnsupportedOperationException("FakeIdentityService.touch must not be called by ResponseComposer")
    }

    override suspend fun listByOwner(owner: PrincipalId): List<Principal> {
        throw UnsupportedOperationException("FakeIdentityService.listByOwner must not be called by ResponseComposer")
    }
}
