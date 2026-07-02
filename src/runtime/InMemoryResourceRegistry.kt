package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceRegistry

/**
 * Minimal in-memory implementation of the already-specified
 * `ResourceRegistry` interface (Volume 3). Not itself one of this phase's
 * three requested systems (Tool Registry, Action Mapping, EventBus) --
 * added as a small, necessary supporting dependency because
 * `docs/architecture/tool-registry.md`'s registration invariant ("every
 * registered Tool MUST also have a corresponding Resource Registry
 * entry") cannot be enforced against an interface with no working
 * implementation. `IMPLEMENTATION_ORDER.md` already places Resource
 * Registry in an earlier phase than Tool Registry, so this is catching up
 * a prerequisite, not scope creep into a later phase.
 *
 * No Kotlin behaviour is invented beyond what `ResourceRegistry.kt`
 * already specifies: register/resolve/update/listByOwner, guarded by a
 * [Mutex] since nothing in this codebase yet specifies a concurrency
 * model and a registry that will be read from multiple coroutines
 * concurrently needs at least this much.
 */
class InMemoryResourceRegistry : ResourceRegistry {

    private val mutex = Mutex()
    private val resources = mutableMapOf<ResourceId, Resource>()

    override suspend fun register(resource: Resource): ResourceId = mutex.withLock {
        require(resource.resourceId !in resources) {
            "Resource ${resource.resourceId.value} is already registered; use update() to change an existing Resource"
        }
        resources[resource.resourceId] = resource
        resource.resourceId
    }

    override suspend fun resolve(resourceId: ResourceId): Resource? = mutex.withLock {
        resources[resourceId]
    }

    override suspend fun update(resource: Resource): Resource = mutex.withLock {
        require(resource.resourceId in resources) {
            "Cannot update unregistered Resource ${resource.resourceId.value}; use register() first"
        }
        resources[resource.resourceId] = resource
        resource
    }

    override suspend fun listByOwner(owner: PrincipalId): List<Resource> = mutex.withLock {
        resources.values.filter { it.ownerPrincipalId == owner }
    }
}
