package parker.core.interfaces

import java.time.Instant
import java.security.MessageDigest

/** Source bytes held before governed evidence admission while an Owner decision is pending. */
data class PendingReviewSource(
    val batchId: String,
    val sourceSha256: String,
    val byteLength: Long,
    val mediaType: String?,
    val originalDisplayName: String?,
    val createdAt: Instant,
    val bytes: ByteArray,
) {
    init {
        require(batchId.matches(Regex("^bulk-[a-z0-9-]{1,200}$"))) { "invalid pending-review batchId" }
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$"))) { "invalid pending-review sourceSha256" }
        require(bytes.isNotEmpty() && byteLength == bytes.size.toLong()) { "pending-review byte length mismatch" }
        require(byteLength <= 64L * 1024L * 1024L) { "pending-review source exceeds the source-size limit" }
        require(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } == sourceSha256) {
            "pending-review sourceSha256 does not match received bytes"
        }
        require(mediaType == null || mediaType.isNotBlank())
        require(originalDisplayName == null || originalDisplayName.isNotBlank())
    }

    fun copyBytes(): ByteArray = bytes.copyOf()
}

sealed class PendingReviewSourceStoreResult {
    data class Stored(val source: PendingReviewSource) : PendingReviewSourceStoreResult()
    data class AlreadyStored(val source: PendingReviewSource) : PendingReviewSourceStoreResult()
    data class Conflict(val source: PendingReviewSource) : PendingReviewSourceStoreResult()
}

/** Narrow custody primitive; it is not Evidence Custodian storage and never creates EvidenceArtifactId. */
interface PendingReviewSourceStorage {
    suspend fun store(source: PendingReviewSource): PendingReviewSourceStoreResult
    suspend fun find(batchId: String, sourceSha256: String): PendingReviewSource?
}
