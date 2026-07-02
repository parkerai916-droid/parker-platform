package parker.core.interfaces

interface MemoryStore {
    suspend fun addCandidate(candidate: CandidateMemory): MemoryId
    suspend fun promote(memoryId: MemoryId): Memory
    suspend fun retrieve(query: MemoryQuery): List<Memory>
    suspend fun forget(memoryId: MemoryId): ForgetResult
}
