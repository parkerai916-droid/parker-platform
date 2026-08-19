package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.time.Instant
import java.util.Base64
import parker.core.interfaces.AssertionId
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeRestoration
import parker.core.interfaces.KnowledgeRetirement
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DurableKnowledgeItemEntryCodecTest {
    private val genesis = DurableKnowledgeItemEntry.GENESIS_HASH
    private val time = Instant.parse("2026-01-01T00:00:00Z")

    private fun line(
        payload: String,
        version: Int = 1,
        sequence: Long = 1,
        previous: String = genesis,
        current: String = DurableKnowledgeItemEntry.calculateHash(version, sequence, previous, payload),
    ) = "$version\t$sequence\t$previous\t$current\t$payload"

    private fun payload(
        id: String = "knowledge-1",
        itemState: String = EvidentialState.UNKNOWN.name,
        status: String = KnowledgeItemStatus.ACTIVE.name,
        referenceTag: Int = 3,
        historyCount: Int = 0,
        eventTag: Int = 1,
        eventId: String = id,
        timestamp: String = time.toString(),
        eventState: String = EvidentialState.UNKNOWN.name,
        trailingByte: Boolean = false,
    ): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeUTF(id)
            out.writeByte(referenceTag); out.writeUTF("assertion-1")
            out.writeUTF("provenance-1")
            out.writeUTF(itemState)
            out.writeUTF(status)
            out.writeInt(historyCount)
            if (historyCount > 0) {
                out.writeByte(eventTag)
                out.writeUTF(eventId)
                out.writeUTF(timestamp)
                out.writeUTF("basis")
                if (eventTag == 1) {
                    out.writeByte(3); out.writeUTF("assertion-event")
                    out.writeUTF(eventState)
                } else if (eventTag == 3) {
                    out.writeByte(3); out.writeUTF("assertion-event")
                }
            }
            if (trailingByte) out.writeByte(99)
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    private fun corrupt(encodedLine: String) = assertFailsWith<KnowledgeItemDurabilityException.CorruptLog> {
        DurableKnowledgeItemEntryCodec.decode(encodedLine, 1)
    }

    @Test fun `round trip preserves empty history and status independently of history`() {
        val value = KnowledgeItem(
            KnowledgeId("empty-history"),
            MemoryCoreRecordReference.ToAssertion(AssertionId("assertion-empty")),
            ProvenanceReference(ProvenanceId("provenance-empty")),
            EvidentialState.INDETERMINATE,
            KnowledgeItemStatus.RETIRED,
            emptyList(),
        )
        val entry = DurableKnowledgeItemEntry.create(1, value, genesis)
        assertEquals(entry, DurableKnowledgeItemEntryCodec.decode(DurableKnowledgeItemEntryCodec.encode(entry), 1))
    }

    @Test fun `round trip preserves unusual lifecycle mixtures timestamps and exact event order`() {
        val id = KnowledgeId("unusual-history")
        val reference = MemoryCoreRecordReference.ToAssertion(AssertionId("assertion-unusual"))
        val history = listOf(
            KnowledgeRestoration(id, reference, time.plusSeconds(20), "restoration first is structurally permitted"),
            KnowledgeRetirement(id, time.plusSeconds(10), "non-monotonic timestamp is structurally permitted"),
            KnowledgeRetirement(id, time.plusSeconds(30), "repeated retirement is structurally permitted"),
            KnowledgePromotion(id, reference, EvidentialState.UNKNOWN, time, "promotion last is structurally permitted"),
        )
        val value = KnowledgeItem(id, reference, ProvenanceReference(ProvenanceId("provenance-unusual")), EvidentialState.VERIFIED_EVIDENCE, KnowledgeItemStatus.ACTIVE, history)
        val decoded = DurableKnowledgeItemEntryCodec.decode(DurableKnowledgeItemEntryCodec.encode(DurableKnowledgeItemEntry.create(1, value, genesis)), 1)
        assertEquals(value, decoded.item)
        assertEquals(history, decoded.item.history)
    }

    @Test fun `unsupported schema version fails closed`() { corrupt(line(payload(), version = 2)) }
    @Test fun `zero and negative sequence values fail closed`() {
        corrupt(line(payload(), sequence = 0)); corrupt(line(payload(), sequence = -1))
    }
    @Test fun `current hash mismatch fails closed`() { corrupt(line(payload(), current = "f".repeat(64))) }
    @Test fun `malformed previous and current hash syntax fails closed`() {
        corrupt(line(payload(), previous = "not-a-hash", current = "0".repeat(64)))
        corrupt(line(payload(), current = "xyz"))
    }
    @Test fun `invalid Base64 fails closed`() { corrupt(line("%%%not-base64%%%")) }
    @Test fun `invalid identifier fails closed`() { corrupt(line(payload(id = "   "))) }
    @Test fun `invalid timestamp fails closed`() { corrupt(line(payload(historyCount = 1, timestamp = "not-an-instant"))) }
    @Test fun `invalid item and event enum values fail closed`() {
        corrupt(line(payload(itemState = "NOT_A_STATE")))
        corrupt(line(payload(status = "NOT_A_STATUS")))
        corrupt(line(payload(historyCount = 1, eventState = "NOT_A_STATE")))
    }
    @Test fun `invalid reference and lifecycle event tags fail closed`() {
        corrupt(line(payload(referenceTag = 99)))
        corrupt(line(payload(historyCount = 1, eventTag = 99)))
    }
    @Test fun `trailing unread payload data fails closed`() { corrupt(line(payload(trailingByte = true))) }
}
