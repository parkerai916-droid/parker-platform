package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*

class FileSystemHumanFidelityReviewStorageTest {
    @TempDir lateinit var directory: Path
    private val clock = Clock.fixed(Instant.parse("2026-09-03T02:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `prepare publish exact readback and restart preserve the R6 fixture`() = runTest {
        val (store, audit, _, _) = stores()
        val record = HumanFidelityReviewFixture.review()
        assertEquals(HumanFidelityReviewPreparationResult.Prepared, store.prepare(record))
        assertNull(store.retrieve(record.reviewId))
        assertEquals(listOf(HumanFidelityGovernanceAuditEventType.REVIEW_PREPARED), audit.listForReview(record.reviewId).map { it.eventType })
        assertEquals(HumanFidelityReviewPublicationResult.Published, store.publishPrepared(record.reviewId))

        val restartedAudit = FileSystemHumanFidelityGovernanceAudit(directory.resolve("audit"))
        val restarted = FileSystemHumanFidelityReviewStorage(directory.resolve("reviews"), restartedAudit, clock)
        val read = assertNotNull(restarted.retrieve(record.reviewId))
        assertContentEquals(HumanFidelityReviewRecordCodec.encode(record), HumanFidelityReviewRecordCodec.encode(read))
        assertEquals(2, read.discrepancyOccurrences.size)
        assertEquals(1, read.systematicPatterns.size)
        assertEquals(listOf(1, 5), read.discrepancyOccurrences.map { it.location.pageNumber })
        assertEquals(listOf("Kellee", "Kellee"), read.discrepancyOccurrences.map { it.location.originalProviderSubstring })
        assertTrue(read.discrepancyOccurrences.all {
            (it.sourceResolution as HumanSourceResolution.ResolvedAgainstSource).assertedSourceValue == "Kellec" &&
                it.severity == FidelityDiscrepancySeverity.MATERIAL && it.causeAssessment.state == FidelityCauseState.UNKNOWN
        })
        assertEquals(HumanFidelityReviewFixture.HIGH_FIDELITY, read.descriptiveFidelity)
    }

    @Test
    fun `exact duplicates are deterministic while same identity different bytes fail closed`() = runTest {
        val (store, audit, _, _) = stores()
        val record = HumanFidelityReviewFixture.review()
        store.prepare(record); store.publishPrepared(record.reviewId)
        assertEquals(HumanFidelityReviewPreparationResult.AlreadyPublished, store.prepare(record))
        assertEquals(HumanFidelityReviewPublicationResult.AlreadyPublished, store.publishPrepared(record.reviewId))
        assertEquals(1, audit.listForReview(record.reviewId).count {
            it.eventType == HumanFidelityGovernanceAuditEventType.REVIEW_DUPLICATE_CONFIRMED
        })
        assertFailsWith<HumanFidelityReviewStorageException.ConflictingIdentifier> {
            store.prepare(HumanFidelityReviewFixture.review(reason = "Different exact discrepancy reason"))
        }
    }

    @Test
    fun `exact target query is identity ordered and conveys no timestamp authority`() = runTest {
        val (store, _, _, _) = stores()
        val later = HumanFidelityReviewFixture.review(reviewedAt = Instant.parse("2026-09-04T00:00:00Z"))
        val earlier = HumanFidelityReviewFixture.review(reviewedAt = Instant.parse("2026-09-02T00:00:00Z"))
        store.prepare(later); store.publishPrepared(later.reviewId)
        store.prepare(earlier); store.publishPrepared(earlier.reviewId)
        val records = store.listForExactTarget(HumanFidelityReviewFixture.target)
        assertEquals(records.map { it.reviewId.value }.sorted(), records.map { it.reviewId.value })
        assertEquals(setOf(later.reviewId, earlier.reviewId), records.map { it.reviewId }.toSet())
        assertTrue(store.listForExactTarget(HumanFidelityReviewFixture.target.copy(sourceSha256 = OcrSha256Digest("f".repeat(64)))).isEmpty())
        assertNull(store.retrieve(HumanFidelityReviewId("review-${"f".repeat(64)}")))
        assertFalse(HumanFidelityReviewStorage::class.members.any { it.name.contains("latest", ignoreCase = true) })
    }

    @Test
    fun `orphan preparation remains non-canonical across restart`() = runTest {
        val (store, _, reviewRoot, auditRoot) = stores()
        val record = HumanFidelityReviewFixture.review()
        store.prepare(record)
        assertTrue(Files.exists(reviewRoot.resolve(".prepared/${record.reviewId.value}.human-fidelity-review-v1")))
        val restarted = FileSystemHumanFidelityReviewStorage(reviewRoot, FileSystemHumanFidelityGovernanceAudit(auditRoot), clock)
        assertNull(restarted.retrieve(record.reviewId))
        assertTrue(restarted.listForExactTarget(record.target).isEmpty())
    }

    @Test
    fun `canonical corruption truncation and unsupported version fail closed without overwrite`() = runTest {
        val cases = listOf("corrupt", "truncated", "version")
        for (case in cases) {
            val caseRoot = Files.createDirectories(directory.resolve(case))
            val reviewRoot = Files.createDirectories(caseRoot.resolve("reviews"))
            val auditRoot = Files.createDirectories(caseRoot.resolve("audit"))
            val store = FileSystemHumanFidelityReviewStorage(reviewRoot, FileSystemHumanFidelityGovernanceAudit(auditRoot), clock)
            val record = HumanFidelityReviewFixture.review()
            store.prepare(record); store.publishPrepared(record.reviewId)
            val path = reviewRoot.resolve("${record.reviewId.value}.human-fidelity-review-v1")
            val original = Files.readAllBytes(path)
            when (case) {
                "corrupt" -> Files.write(path, original.copyOf().also { it[20] = (it[20].toInt() xor 1).toByte() })
                "truncated" -> Files.write(path, original.copyOf(original.size - 3))
                else -> Files.write(path, withVersion(original, 99))
            }
            val corrupted = Files.readAllBytes(path)
            if (case == "version") {
                assertFailsWith<HumanFidelityReviewStorageException.UnsupportedRepresentationVersion> { store.retrieve(record.reviewId) }
            } else {
                assertFailsWith<HumanFidelityReviewStorageException.CorruptRecord> { store.retrieve(record.reviewId) }
            }
            assertFails { store.prepare(record) }
            assertContentEquals(corrupted, Files.readAllBytes(path))
        }
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

    @Test
    fun `audit ordering follows durable state and failed publication never emits PUBLISHED`() = runTest {
        val reviewRoot = Files.createDirectories(directory.resolve("ordered-reviews"))
        val auditRoot = Files.createDirectories(directory.resolve("ordered-audit"))
        val delegate = FileSystemHumanFidelityGovernanceAudit(auditRoot)
        val observing = ObservingAudit(delegate, reviewRoot)
        val store = FileSystemHumanFidelityReviewStorage(reviewRoot, observing, clock)
        val record = HumanFidelityReviewFixture.review()
        store.prepare(record); store.publishPrepared(record.reviewId)
        assertEquals(listOf("PREPARED_AFTER_STAGE", "PUBLISHED_AFTER_CANONICAL"), observing.observations)

        val failRoot = Files.createDirectories(directory.resolve("failed-reviews"))
        val failAuditRoot = Files.createDirectories(directory.resolve("failed-audit"))
        val durableAudit = FileSystemHumanFidelityGovernanceAudit(failAuditRoot)
        val failing = FailingPublishedAudit(durableAudit)
        val failedStore = FileSystemHumanFidelityReviewStorage(failRoot, failing, clock)
        failedStore.prepare(record)
        assertFailsWith<HumanFidelityReviewStorageException.PersistenceFailure> { failedStore.publishPrepared(record.reviewId) }
        assertFalse(durableAudit.listForReview(record.reviewId).any { it.eventType == HumanFidelityGovernanceAuditEventType.REVIEW_PUBLISHED })
        val failClosed = FileSystemHumanFidelityReviewStorage(failRoot, durableAudit, clock)
        assertFailsWith<HumanFidelityReviewStorageException.IncompleteAudit> { failClosed.retrieve(record.reviewId) }
        assertEquals(HumanFidelityReviewPublicationResult.AlreadyPublished, failClosed.publishPrepared(record.reviewId))
        assertNotNull(failClosed.retrieve(record.reviewId))
    }

    @Test
    fun `storage contract has no update delete latest or effective-state operation`() {
        val operations = HumanFidelityReviewStorage::class.members.filter { it.isAbstract }.map { it.name }.toSet()
        assertEquals(setOf("prepare", "publishPrepared", "retrieve", "listForExactTarget"), operations)
    }

    private fun stores(): StoreFixture {
        val reviewRoot = Files.createDirectories(directory.resolve("reviews"))
        val auditRoot = Files.createDirectories(directory.resolve("audit"))
        val audit = FileSystemHumanFidelityGovernanceAudit(auditRoot)
        return StoreFixture(FileSystemHumanFidelityReviewStorage(reviewRoot, audit, clock), audit, reviewRoot, auditRoot)
    }

    private data class StoreFixture(
        val store: FileSystemHumanFidelityReviewStorage,
        val audit: FileSystemHumanFidelityGovernanceAudit,
        val reviewRoot: Path,
        val auditRoot: Path,
    )

    private class ObservingAudit(
        private val delegate: HumanFidelityGovernanceAudit,
        private val reviewRoot: Path,
    ) : HumanFidelityGovernanceAudit by delegate {
        val observations = mutableListOf<String>()
        override suspend fun append(record: HumanFidelityGovernanceAuditRecord): HumanFidelityGovernanceAuditAppendResult {
            when (record.eventType) {
                HumanFidelityGovernanceAuditEventType.REVIEW_PREPARED -> {
                    assertTrue(Files.exists(reviewRoot.resolve(".prepared/${record.reviewId.value}.human-fidelity-review-v1")))
                    observations += "PREPARED_AFTER_STAGE"
                }
                HumanFidelityGovernanceAuditEventType.REVIEW_PUBLISHED -> {
                    assertTrue(Files.exists(reviewRoot.resolve("${record.reviewId.value}.human-fidelity-review-v1")))
                    observations += "PUBLISHED_AFTER_CANONICAL"
                }
                HumanFidelityGovernanceAuditEventType.REVIEW_DUPLICATE_CONFIRMED -> Unit
            }
            return delegate.append(record)
        }
    }

    private class FailingPublishedAudit(private val delegate: HumanFidelityGovernanceAudit) : HumanFidelityGovernanceAudit by delegate {
        override suspend fun append(record: HumanFidelityGovernanceAuditRecord): HumanFidelityGovernanceAuditAppendResult {
            if (record.eventType == HumanFidelityGovernanceAuditEventType.REVIEW_PUBLISHED) {
                throw HumanFidelityGovernanceAuditException.PersistenceFailure("injected", java.io.IOException("audit unavailable"))
            }
            return delegate.append(record)
        }
    }
}
