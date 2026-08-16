package parker.core.runtime

internal interface KnowledgeItemDurabilityLog {
    suspend fun append(entry: DurableKnowledgeItemEntry)
    suspend fun readAll(): List<DurableKnowledgeItemEntry>
}
