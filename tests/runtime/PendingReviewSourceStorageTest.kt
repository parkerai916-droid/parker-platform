package parker.core.runtime

import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.PendingReviewSource
import parker.core.interfaces.PendingReviewSourceStoreResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PendingReviewSourceStorageTest {
    private val batch = "bulk-pending-review"

    private fun source(bytes: ByteArray = "pending source".toByteArray()): PendingReviewSource {
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return PendingReviewSource(batch, sha, bytes.size.toLong(), "application/pdf", "review.pdf", Instant.parse("2026-09-12T00:00:00Z"), bytes)
    }

    @Test
    fun `filesystem custody is hash verified immutable idempotent and restart durable`() = runTest {
        val root = Files.createTempDirectory("pending-review-source-")
        val first = FileSystemPendingReviewSourceStorage(root)
        val value = source()
        assertIs<PendingReviewSourceStoreResult.Stored>(first.store(value))
        assertIs<PendingReviewSourceStoreResult.AlreadyStored>(first.store(value))

        val recreated = FileSystemPendingReviewSourceStorage(root)
        val restored = recreated.find(batch, value.sourceSha256)
        requireNotNull(restored)
        assertContentEquals(value.bytes, restored.bytes)
        assertEquals(value.sourceSha256, restored.sourceSha256)
    }

    @Test
    fun `in-memory custody uses exact batch and hash lookup`() = runTest {
        val storage = InMemoryPendingReviewSourceStorage()
        val value = source()
        storage.store(value)
        assertNull(storage.find("bulk-other", value.sourceSha256))
        assertNull(storage.find(batch, "b".repeat(64)))
        assertContentEquals(value.bytes, storage.find(batch, value.sourceSha256)!!.bytes)
    }
}
