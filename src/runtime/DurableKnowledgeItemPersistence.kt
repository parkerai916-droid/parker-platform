package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem

/**
 * Durable storage preserves every [KnowledgeItem] accepted by the existing domain
 * constructors exactly as supplied. It does not approve, infer, or govern lifecycle
 * transitions: empty histories, unusual event mixtures, event order, timestamps, and
 * status/history combinations remain the authoritative Knowledge contract's concern.
 */
internal class DurableKnowledgeItemPersistence private constructor(
    private val durabilityLog: KnowledgeItemDurabilityLog,
    recovered: LinkedHashMap<KnowledgeId, KnowledgeItem>,
    private var lastSequence: Long,
    private var lastHash: String,
) : KnowledgeItemPersistence {
    private val mutex = Mutex()
    private val items = recovered

    companion object {
        suspend fun create(durabilityLog: KnowledgeItemDurabilityLog): DurableKnowledgeItemPersistence {
            val recovered = linkedMapOf<KnowledgeId, KnowledgeItem>()
            var expectedSequence = 1L
            var expectedPreviousHash = DurableKnowledgeItemEntry.GENESIS_HASH
            durabilityLog.readAll().forEach { entry ->
                if (entry.sequence != expectedSequence) corrupt("expected sequence $expectedSequence but found ${entry.sequence}")
                if (entry.previousEntryHash != expectedPreviousHash) corrupt("broken hash chain at sequence ${entry.sequence}")
                val existing = recovered[entry.item.knowledgeId]
                if (existing == null) recovered[entry.item.knowledgeId] = entry.item
                else if (existing != entry.item) throw KnowledgeItemDurabilityException.ConflictingDuplicate(entry.item.knowledgeId.value)
                expectedSequence++
                expectedPreviousHash = entry.currentEntryHash
            }
            return DurableKnowledgeItemPersistence(durabilityLog, recovered, expectedSequence - 1, expectedPreviousHash)
        }

        private fun corrupt(reason: String): Nothing = throw KnowledgeItemDurabilityException.CorruptLog(reason)
    }

    override suspend fun store(item: KnowledgeItem): KnowledgeItem = mutex.withLock {
        items[item.knowledgeId]?.let { existing ->
            if (existing == item) return@withLock item
            throw KnowledgeItemDurabilityException.ConflictingDuplicate(item.knowledgeId.value)
        }
        val entry = DurableKnowledgeItemEntry.create(lastSequence + 1, item, lastHash)
        durabilityLog.append(entry)
        items[item.knowledgeId] = item
        lastSequence = entry.sequence
        lastHash = entry.currentEntryHash
        item
    }

    override suspend fun find(knowledgeId: KnowledgeId): KnowledgeItem? = mutex.withLock { items[knowledgeId] }
    override suspend fun findAll(): List<KnowledgeItem> = mutex.withLock { items.values.toList() }
}
