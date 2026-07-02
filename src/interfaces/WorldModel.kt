package parker.core.interfaces

interface WorldModel {
    suspend fun observe(observation: WorldObservation): ObservationResult
    suspend fun current(resourceId: ResourceId): WorldState?
    suspend fun query(query: WorldQuery): List<WorldState>
}
