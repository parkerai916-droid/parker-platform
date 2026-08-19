package parker.core.runtime

import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AssertionId
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FileSystemKnowledgeItemDurabilityLogTest {
    private fun entry() = DurableKnowledgeItemEntry.create(
        1,
        KnowledgeItem(
            KnowledgeId("knowledge-log-test"),
            MemoryCoreRecordReference.ToAssertion(AssertionId("assertion-log-test")),
            ProvenanceReference(ProvenanceId("provenance-log-test")),
            EvidentialState.UNKNOWN,
        ),
        DurableKnowledgeItemEntry.GENESIS_HASH,
    )

    @Test fun `malformed outer UTF-8 fails closed`() = runTest {
        val path = Files.createTempDirectory("knowledge-malformed-utf8").resolve("items.log")
        Files.write(path, byteArrayOf(0xC3.toByte(), 0x28, '\n'.code.toByte()))
        assertFailsWith<KnowledgeItemDurabilityException.CorruptLog> {
            FileSystemKnowledgeItemDurabilityLog(path).readAll()
        }
    }

    @Test fun `truncating a previously valid record fails closed`() = runTest {
        val path = Files.createTempDirectory("knowledge-truncated-valid").resolve("items.log")
        val valid = (DurableKnowledgeItemEntryCodec.encode(entry()) + "\n").toByteArray()
        Files.write(path, valid.copyOf(valid.size - 7))
        assertFailsWith<KnowledgeItemDurabilityException.CorruptLog> {
            FileSystemKnowledgeItemDurabilityLog(path).readAll()
        }
    }
}
