package parker.core.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import parker.core.interfaces.KnowledgeItem

internal data class DurableKnowledgeItemEntry(
    val schemaVersion: Int,
    val sequence: Long,
    val item: KnowledgeItem,
    val previousEntryHash: String,
    val currentEntryHash: String,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        val GENESIS_HASH = "0".repeat(64)

        fun create(sequence: Long, item: KnowledgeItem, previousEntryHash: String): DurableKnowledgeItemEntry {
            val payload = DurableKnowledgeItemEntryCodec.encodeItem(item)
            return DurableKnowledgeItemEntry(
                CURRENT_SCHEMA_VERSION, sequence, item, previousEntryHash,
                calculateHash(CURRENT_SCHEMA_VERSION, sequence, previousEntryHash, payload),
            )
        }

        internal fun calculateHash(version: Int, sequence: Long, previousHash: String, payload: String): String {
            val canonical = "$version\n$sequence\n$previousHash\n$payload"
            return MessageDigest.getInstance("SHA-256")
                .digest(canonical.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
    }
}

internal sealed class KnowledgeItemDurabilityException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class InvalidStorage(path: String, reason: String, cause: Throwable? = null) :
        KnowledgeItemDurabilityException("Knowledge Item durability path '$path' is invalid: $reason", cause)
    class StorageFailure(message: String, cause: Throwable) : KnowledgeItemDurabilityException(message, cause)
    class CorruptLog(reason: String, cause: Throwable? = null) :
        KnowledgeItemDurabilityException("Knowledge Item durability log is corrupt: $reason", cause)
    class ConflictingDuplicate(id: String) :
        KnowledgeItemDurabilityException("Knowledge Item '$id' conflicts with an already stored item")
}
