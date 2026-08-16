package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.time.Instant
import java.util.Base64
import parker.core.interfaces.AssertionId
import parker.core.interfaces.DocumentId
import parker.core.interfaces.EntityId
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgeLifecycleEvent
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeRestoration
import parker.core.interfaces.KnowledgeRetirement
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.RelationshipId

internal object DurableKnowledgeItemEntryCodec {
    private const val MAX_COLLECTION_SIZE = 1_000_000

    fun encode(entry: DurableKnowledgeItemEntry): String {
        val payload = encodeItem(entry.item)
        return listOf(entry.schemaVersion, entry.sequence, entry.previousEntryHash, entry.currentEntryHash, payload)
            .joinToString("\t")
    }

    fun decode(line: String, lineNumber: Int): DurableKnowledgeItemEntry = try {
        val fields = line.split('\t')
        if (fields.size != 5) corrupt("line $lineNumber has ${fields.size} fields; expected 5")
        val version = fields[0].toIntOrNull() ?: corrupt("line $lineNumber has an invalid schema version")
        if (version != DurableKnowledgeItemEntry.CURRENT_SCHEMA_VERSION) corrupt("line $lineNumber has unsupported schema version $version")
        val sequence = fields[1].toLongOrNull()?.takeIf { it > 0 } ?: corrupt("line $lineNumber has an invalid sequence")
        val previousHash = requireHash(fields[2], lineNumber, "previous")
        val currentHash = requireHash(fields[3], lineNumber, "current")
        val expected = DurableKnowledgeItemEntry.calculateHash(version, sequence, previousHash, fields[4])
        if (currentHash != expected) corrupt("line $lineNumber has an invalid current-entry hash")
        DurableKnowledgeItemEntry(version, sequence, decodeItem(fields[4]), previousHash, currentHash)
    } catch (e: KnowledgeItemDurabilityException.CorruptLog) {
        throw e
    } catch (e: Exception) {
        throw KnowledgeItemDurabilityException.CorruptLog("line $lineNumber cannot be decoded", e)
    }

    internal fun encodeItem(item: KnowledgeItem): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeUTF(item.knowledgeId.value)
            writeReference(out, item.evidenceReference)
            out.writeUTF(item.provenanceReference.provenanceId.value)
            out.writeUTF(item.evidentialState.name)
            out.writeUTF(item.status.name)
            out.writeInt(item.history.size)
            item.history.forEach { event ->
                when (event) {
                    is KnowledgePromotion -> {
                        out.writeByte(1); writeEventCommon(out, event); writeReference(out, event.evidenceReference)
                        out.writeUTF(event.resultingState.name)
                    }
                    is KnowledgeRetirement -> { out.writeByte(2); writeEventCommon(out, event) }
                    is KnowledgeRestoration -> { out.writeByte(3); writeEventCommon(out, event); writeReference(out, event.evidenceReference) }
                }
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    internal fun decodeItem(encoded: String): KnowledgeItem {
        val raw = try { Base64.getDecoder().decode(encoded) } catch (e: IllegalArgumentException) { corrupt("payload is not valid Base64", e) }
        val input = ByteArrayInputStream(raw)
        val item = DataInputStream(input).use { data ->
            val id = KnowledgeId(data.readUTF())
            val reference = readReference(data)
            val provenance = ProvenanceReference(ProvenanceId(data.readUTF()))
            val state = enumValue<EvidentialState>(data.readUTF(), "evidential state")
            val status = enumValue<KnowledgeItemStatus>(data.readUTF(), "status")
            val count = data.readInt()
            if (count < 0 || count > MAX_COLLECTION_SIZE) corrupt("invalid lifecycle event count $count")
            val history = List(count) {
                val kind = data.readUnsignedByte()
                val eventId = KnowledgeId(data.readUTF())
                val instant = parseInstant(data.readUTF())
                val basis = data.readUTF()
                when (kind) {
                    1 -> KnowledgePromotion(eventId, readReference(data), enumValue(data.readUTF(), "resulting state"), instant, basis)
                    2 -> KnowledgeRetirement(eventId, instant, basis)
                    3 -> KnowledgeRestoration(eventId, readReference(data), instant, basis)
                    else -> corrupt("unknown lifecycle event kind $kind")
                }
            }
            if (input.available() != 0) corrupt("payload contains trailing bytes")
            KnowledgeItem(id, reference, provenance, state, status, history)
        }
        return item
    }

    private fun writeEventCommon(out: DataOutputStream, event: KnowledgeLifecycleEvent) {
        out.writeUTF(event.knowledgeId.value); out.writeUTF(event.occurredAt.toString()); out.writeUTF(event.basis)
    }

    private fun writeReference(out: DataOutputStream, reference: MemoryCoreRecordReference) = when (reference) {
        is MemoryCoreRecordReference.ToEntity -> { out.writeByte(1); out.writeUTF(reference.entityId.value) }
        is MemoryCoreRecordReference.ToDocument -> { out.writeByte(2); out.writeUTF(reference.documentId.value) }
        is MemoryCoreRecordReference.ToAssertion -> { out.writeByte(3); out.writeUTF(reference.assertionId.value) }
        is MemoryCoreRecordReference.ToRelationship -> { out.writeByte(4); out.writeUTF(reference.relationshipId.value) }
    }

    private fun readReference(input: DataInputStream): MemoryCoreRecordReference = when (val kind = input.readUnsignedByte()) {
        1 -> MemoryCoreRecordReference.ToEntity(EntityId(input.readUTF()))
        2 -> MemoryCoreRecordReference.ToDocument(DocumentId(input.readUTF()))
        3 -> MemoryCoreRecordReference.ToAssertion(AssertionId(input.readUTF()))
        4 -> MemoryCoreRecordReference.ToRelationship(RelationshipId(input.readUTF()))
        else -> corrupt("unknown evidence-reference kind $kind")
    }

    private fun parseInstant(value: String): Instant = try { Instant.parse(value) } catch (e: Exception) { corrupt("invalid lifecycle timestamp '$value'", e) }
    private inline fun <reified T : Enum<T>> enumValue(value: String, label: String): T = try { enumValueOf<T>(value) }
        catch (e: IllegalArgumentException) { corrupt("invalid $label '$value'", e) }
    private fun requireHash(value: String, line: Int, label: String): String {
        if (!value.matches(Regex("[0-9a-f]{64}"))) corrupt("line $line has an invalid $label-entry hash")
        return value
    }
    private fun corrupt(reason: String, cause: Throwable? = null): Nothing = throw KnowledgeItemDurabilityException.CorruptLog(reason, cause)
}
