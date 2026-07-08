package parker.core.runtime

import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceRegistry

/**
 * Test-only fake, mirroring [FakeCommunicationIntake]/[FakePermissionEngine]'s
 * lambda-based fake precedent. Exists so [ResponseDeliveryTest] can prove
 * [ResponseDelivery]'s own Resource-location logic (Contract Design
 * Decision 2, Section 5) -- including its zero-match, many-match, and
 * dependency-exception-propagation paths -- independently of
 * [InMemoryResourceRegistry]'s own already-tested storage logic, which
 * [InMemoryResourceRegistryTest] already covers on its own.
 *
 * Only [listByOwner] is exercised by [ResponseDelivery] (Plan Section 4);
 * [register]/[resolve]/[update] are never called by it, so they throw if
 * reached -- a structural guard against this fake silently masking an
 * unexpected dependency on a method [ResponseDelivery] must not call.
 */
class FakeResourceRegistry(
    private val resourcesFor: (PrincipalId) -> List<Resource>,
) : ResourceRegistry {

    var listByOwnerCallCount: Int = 0
        private set

    override suspend fun register(resource: Resource): ResourceId {
        throw UnsupportedOperationException("FakeResourceRegistry.register must not be called by ResponseDelivery")
    }

    override suspend fun resolve(resourceId: ResourceId): Resource? {
        throw UnsupportedOperationException("FakeResourceRegistry.resolve must not be called by ResponseDelivery")
    }

    override suspend fun update(resource: Resource): Resource {
        throw UnsupportedOperationException("FakeResourceRegistry.update must not be called by ResponseDelivery")
    }

    override suspend fun listByOwner(owner: PrincipalId): List<Resource> {
        listByOwnerCallCount++
        return resourcesFor(owner)
    }
}
