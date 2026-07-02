package parker.core.interfaces

interface ResourceRegistry {
    suspend fun register(resource: Resource): ResourceId
    suspend fun resolve(resourceId: ResourceId): Resource?
    suspend fun update(resource: Resource): Resource
    suspend fun listByOwner(owner: PrincipalId): List<Resource>
}
