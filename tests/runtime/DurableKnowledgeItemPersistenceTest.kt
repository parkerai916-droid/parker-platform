package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AssertionId
import parker.core.interfaces.DocumentId
import parker.core.interfaces.EntityId
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
import parker.core.interfaces.RelationshipId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DurableKnowledgeItemPersistenceTest {
    private val time = Instant.parse("2026-01-01T00:00:00Z")

    private fun item(idValue: String = "knowledge-1", reference: MemoryCoreRecordReference = MemoryCoreRecordReference.ToAssertion(AssertionId("assertion-1"))): KnowledgeItem {
        val id = KnowledgeId(idValue)
        return KnowledgeItem(
            id, reference, ProvenanceReference(ProvenanceId("provenance-$idValue")),
            EvidentialState.CORROBORATED_EVIDENCE, KnowledgeItemStatus.ACTIVE,
            listOf(KnowledgePromotion(id, reference, EvidentialState.CORROBORATED_EVIDENCE, time, "promoted")),
        )
    }

    private class ReplayLog(private val entries: List<DurableKnowledgeItemEntry>) : KnowledgeItemDurabilityLog {
        override suspend fun append(entry: DurableKnowledgeItemEntry) = Unit
        override suspend fun readAll() = entries
    }

    @Test fun `empty log starts empty and store survives restart in insertion order`() = runTest {
        val path = Files.createTempDirectory("knowledge-durable").resolve("items.log")
        val first = item("first")
        val second = item("second", MemoryCoreRecordReference.ToDocument(DocumentId("document-2")))
        val persistence = DurableKnowledgeItemPersistence.create(FileSystemKnowledgeItemDurabilityLog(path))
        assertEquals(emptyList(), persistence.findAll())
        persistence.store(first); persistence.store(second)
        assertEquals(first, persistence.find(first.knowledgeId))
        val recovered = DurableKnowledgeItemPersistence.create(FileSystemKnowledgeItemDurabilityLog(path))
        assertEquals(listOf(first, second), recovered.findAll())
    }

    @Test fun `codec round trips every evidence reference and lifecycle event variant`() {
        val references = listOf(
            MemoryCoreRecordReference.ToEntity(EntityId("entity")),
            MemoryCoreRecordReference.ToDocument(DocumentId("document")),
            MemoryCoreRecordReference.ToAssertion(AssertionId("assertion")),
            MemoryCoreRecordReference.ToRelationship(RelationshipId("relationship")),
        )
        references.forEachIndexed { index, reference ->
            val id = KnowledgeId("knowledge-$index")
            val history = listOf(
                KnowledgePromotion(id, reference, EvidentialState.UNKNOWN, time, "initial"),
                KnowledgeRetirement(id, time.plusSeconds(1), "retired"),
                KnowledgeRestoration(id, reference, time.plusSeconds(2), "restored"),
                KnowledgePromotion(id, reference, EvidentialState.VERIFIED_EVIDENCE, time.plusSeconds(3), "revised"),
            )
            val value = KnowledgeItem(id, reference, ProvenanceReference(ProvenanceId("provenance-$index")), EvidentialState.VERIFIED_EVIDENCE, KnowledgeItemStatus.ACTIVE, history)
            val entry = DurableKnowledgeItemEntry.create(1, value, DurableKnowledgeItemEntry.GENESIS_HASH)
            assertEquals(entry, DurableKnowledgeItemEntryCodec.decode(DurableKnowledgeItemEntryCodec.encode(entry), 1))
        }
    }

    @Test fun `identical duplicate is idempotent and conflict fails closed`() = runTest {
        val path = Files.createTempDirectory("knowledge-duplicates").resolve("items.log")
        val persistence = DurableKnowledgeItemPersistence.create(FileSystemKnowledgeItemDurabilityLog(path))
        val value = item()
        persistence.store(value); persistence.store(value)
        assertEquals(1, persistence.findAll().size)
        val conflict = value.copy(evidentialState = EvidentialState.UNKNOWN)
        assertFailsWith<KnowledgeItemDurabilityException.ConflictingDuplicate> { persistence.store(conflict) }
        assertEquals(value, persistence.find(value.knowledgeId))
    }

    @Test fun `truncated final record and broken chain fail closed`() = runTest {
        val dir = Files.createTempDirectory("knowledge-corrupt")
        val truncated = dir.resolve("truncated.log")
        Files.writeString(truncated, "1\t")
        assertFailsWith<KnowledgeItemDurabilityException.CorruptLog> {
            DurableKnowledgeItemPersistence.create(FileSystemKnowledgeItemDurabilityLog(truncated))
        }
        val chained = dir.resolve("chain.log")
        val first = DurableKnowledgeItemEntry.create(1, item("one"), DurableKnowledgeItemEntry.GENESIS_HASH)
        val second = DurableKnowledgeItemEntry.create(2, item("two"), "f".repeat(64))
        Files.writeString(chained, DurableKnowledgeItemEntryCodec.encode(first) + "\n" + DurableKnowledgeItemEntryCodec.encode(second) + "\n")
        assertFailsWith<KnowledgeItemDurabilityException.CorruptLog> {
            DurableKnowledgeItemPersistence.create(FileSystemKnowledgeItemDurabilityLog(chained))
        }
    }

    @Test fun `append failure leaves item invisible`() = runTest {
        val failing = object : KnowledgeItemDurabilityLog {
            override suspend fun append(entry: DurableKnowledgeItemEntry) { throw IllegalStateException("disk failed") }
            override suspend fun readAll() = emptyList<DurableKnowledgeItemEntry>()
        }
        val persistence = DurableKnowledgeItemPersistence.create(failing)
        val value = item()
        assertFailsWith<IllegalStateException> { persistence.store(value) }
        assertNull(persistence.find(value.knowledgeId))
    }

    @Test fun `concurrent stores recover without interleaving`() = runTest {
        val path = Files.createTempDirectory("knowledge-concurrent").resolve("items.log")
        val persistence = DurableKnowledgeItemPersistence.create(FileSystemKnowledgeItemDurabilityLog(path))
        (1..32).map { async { persistence.store(item("item-$it")) } }.awaitAll()
        val recovered = DurableKnowledgeItemPersistence.create(FileSystemKnowledgeItemDurabilityLog(path))
        assertEquals(32, recovered.findAll().size)
    }

    @Test fun `sequence gap repeated sequence and reordered sequence fail closed during recovery`() = runTest {
        val first = DurableKnowledgeItemEntry.create(1, item("first"), DurableKnowledgeItemEntry.GENESIS_HASH)
        val gap = DurableKnowledgeItemEntry.create(3, item("gap"), first.currentEntryHash)
        assertFailsWith<KnowledgeItemDurabilityException.CorruptLog> {
            DurableKnowledgeItemPersistence.create(ReplayLog(listOf(first, gap)))
        }
        val repeated = DurableKnowledgeItemEntry.create(1, item("repeated"), first.currentEntryHash)
        assertFailsWith<KnowledgeItemDurabilityException.CorruptLog> {
            DurableKnowledgeItemPersistence.create(ReplayLog(listOf(first, repeated)))
        }
        val sequenceTwo = DurableKnowledgeItemEntry.create(2, item("second"), first.currentEntryHash)
        assertFailsWith<KnowledgeItemDurabilityException.CorruptLog> {
            DurableKnowledgeItemPersistence.create(ReplayLog(listOf(sequenceTwo, first)))
        }
    }

    @Test fun `previous hash mismatch fails closed during recovery`() = runTest {
        val first = DurableKnowledgeItemEntry.create(1, item("first"), DurableKnowledgeItemEntry.GENESIS_HASH)
        val second = DurableKnowledgeItemEntry.create(2, item("second"), "f".repeat(64))
        assertFailsWith<KnowledgeItemDurabilityException.CorruptLog> {
            DurableKnowledgeItemPersistence.create(ReplayLog(listOf(first, second)))
        }
    }

    @Test fun `identical duplicate durable record replays idempotently at original position`() = runTest {
        val firstItem = item("first")
        val secondItem = item("second")
        val first = DurableKnowledgeItemEntry.create(1, firstItem, DurableKnowledgeItemEntry.GENESIS_HASH)
        val second = DurableKnowledgeItemEntry.create(2, secondItem, first.currentEntryHash)
        val replay = DurableKnowledgeItemEntry.create(3, firstItem, second.currentEntryHash)
        val recovered = DurableKnowledgeItemPersistence.create(ReplayLog(listOf(first, second, replay)))
        assertEquals(listOf(firstItem, secondItem), recovered.findAll())
    }

    @Test fun `conflicting duplicate durable record fails closed during recovery`() = runTest {
        val firstItem = item("same-id")
        val first = DurableKnowledgeItemEntry.create(1, firstItem, DurableKnowledgeItemEntry.GENESIS_HASH)
        val conflict = DurableKnowledgeItemEntry.create(
            2,
            firstItem.copy(evidentialState = EvidentialState.INDETERMINATE),
            first.currentEntryHash,
        )
        assertFailsWith<KnowledgeItemDurabilityException.ConflictingDuplicate> {
            DurableKnowledgeItemPersistence.create(ReplayLog(listOf(first, conflict)))
        }
    }
}
