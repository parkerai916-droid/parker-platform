package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*

class FileSystemHumanFidelityGovernanceAuditTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `append duplicate and restart readback are create-once and exact`() = runTest {
        val record = HumanFidelityReviewFixture.auditRecord()
        val audit = FileSystemHumanFidelityGovernanceAudit(directory)
        assertEquals(HumanFidelityGovernanceAuditAppendResult.Appended, audit.append(record))
        assertEquals(HumanFidelityGovernanceAuditAppendResult.AlreadyPresent, audit.append(record))
        assertEquals(record, audit.retrieve(record.eventId))
        assertEquals(listOf(record), FileSystemHumanFidelityGovernanceAudit(directory).listForReview(record.reviewId))
    }

    @Test
    fun `same event identity with different canonical content is rejected`() = runTest {
        val record = HumanFidelityReviewFixture.auditRecord()
        val audit = FileSystemHumanFidelityGovernanceAudit(directory)
        audit.append(record)
        val conflict = record.copy(recordedAt = Instant.parse("2026-09-03T03:00:00Z"))
        assertEquals(record.eventId, conflict.eventId)
        assertFailsWith<HumanFidelityGovernanceAuditException.ConflictingIdentifier> { audit.append(conflict) }
    }

    @Test
    fun `audit preserves exact target payload and factual event fields`() = runTest {
        val record = HumanFidelityReviewFixture.auditRecord(detail = "durable preparation completed")
        val audit = FileSystemHumanFidelityGovernanceAudit(directory)
        audit.append(record)
        val read = assertNotNull(audit.retrieve(record.eventId))
        assertEquals(HumanFidelityReviewFixture.target, read.target)
        assertEquals(HumanFidelityReviewFixture.reviewer, read.actorPrincipalId)
        assertEquals(HumanFidelityReviewRecordCodec.payloadSha256(HumanFidelityReviewFixture.review()), read.reviewPayloadSha256)
        assertEquals("durable preparation completed", read.factualDetail)
    }

    @Test
    fun `corrupt truncated and unsupported audit entries fail closed`() = runTest {
        for (case in listOf("corrupt", "truncated", "version")) {
            val root = Files.createDirectories(directory.resolve(case))
            val audit = FileSystemHumanFidelityGovernanceAudit(root)
            val record = HumanFidelityReviewFixture.auditRecord()
            audit.append(record)
            val path = root.resolve("${record.eventId.value}.human-fidelity-audit-v1")
            val bytes = Files.readAllBytes(path)
            when (case) {
                "corrupt" -> Files.write(path, bytes.copyOf().also { it[15] = (it[15].toInt() xor 1).toByte() })
                "truncated" -> Files.write(path, bytes.copyOf(bytes.size - 1))
                else -> Files.write(path, withVersion(bytes, 42))
            }
            if (case == "version") {
                assertFailsWith<HumanFidelityGovernanceAuditException.UnsupportedRepresentationVersion> { audit.retrieve(record.eventId) }
            } else {
                assertFailsWith<HumanFidelityGovernanceAuditException.CorruptRecord> { audit.retrieve(record.eventId) }
            }
        }
    }

    @Test
    fun `audit contract is append and read only`() {
        val operations = HumanFidelityGovernanceAudit::class.members.filter { it.isAbstract }.map { it.name }.toSet()
        assertEquals(setOf("append", "retrieve", "listForReview"), operations)
    }

    private fun withVersion(envelope: ByteArray, version: Int): ByteArray = envelope.copyOf().also {
        val buffer = ByteBuffer.wrap(it)
        buffer.putInt(4, version)
        val magic = buffer.getInt(0)
        val payloadSize = buffer.getInt(8)
        val payload = it.copyOfRange(12, 12 + payloadSize)
        val bound = ByteBuffer.allocate(8 + payloadSize).putInt(magic).putInt(version).put(payload).array()
        MessageDigest.getInstance("SHA-256").digest(bound).copyInto(it, 12 + payloadSize)
    }
}
