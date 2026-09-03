package parker.core.runtime

import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.*

class FileSystemHumanFidelityReviewStorage(
    storageRoot: Path,
    private val audit: HumanFidelityGovernanceAudit,
    private val clock: Clock = Clock.systemUTC(),
) : HumanFidelityReviewStorage {
    private val root = storageRoot.toAbsolutePath().normalize()
    private val temporary = root.resolve(".tmp")
    private val prepared = root.resolve(".prepared")
    private val mutex = Mutex()

    init {
        if (!Files.exists(root) || !Files.isDirectory(root) || !Files.isWritable(root)) {
            throw HumanFidelityReviewStorageException.InvalidStorageRoot(root.toString(), "root must exist as a writable directory")
        }
        try {
            Files.createDirectories(temporary); Files.createDirectories(prepared)
            restrictDirectory(root); restrictDirectory(temporary); restrictDirectory(prepared)
        } catch (e: IOException) {
            throw HumanFidelityReviewStorageException.InvalidStorageRoot(root.toString(), "could not initialize storage directories")
        }
    }

    override suspend fun prepare(record: HumanFidelityReviewRecord): HumanFidelityReviewPreparationResult = mutex.withLock {
        requireSafe(record.reviewId)
        val encoded = HumanFidelityReviewRecordCodec.encode(record)
        val target = target(record.reviewId)
        val staged = staged(record.reviewId)
        if (Files.exists(target)) {
            requireExact(record.reviewId, target, encoded)
            ensureAudit(record, HumanFidelityGovernanceAuditEventType.REVIEW_PUBLISHED, HumanFidelityGovernanceAuditOutcome.SUCCEEDED)
            ensureAudit(record, HumanFidelityGovernanceAuditEventType.REVIEW_DUPLICATE_CONFIRMED, HumanFidelityGovernanceAuditOutcome.EXACT_DUPLICATE)
            return@withLock HumanFidelityReviewPreparationResult.AlreadyPublished
        }
        if (Files.exists(staged)) {
            requireExact(record.reviewId, staged, encoded)
            ensureAudit(record, HumanFidelityGovernanceAuditEventType.REVIEW_PREPARED, HumanFidelityGovernanceAuditOutcome.SUCCEEDED)
            ensureAudit(record, HumanFidelityGovernanceAuditEventType.REVIEW_DUPLICATE_CONFIRMED, HumanFidelityGovernanceAuditOutcome.EXACT_DUPLICATE)
            return@withLock HumanFidelityReviewPreparationResult.AlreadyPrepared
        }
        val temp = try { Files.createTempFile(temporary, "human-fidelity-review-", ".tmp") }
        catch (e: IOException) { throw HumanFidelityReviewStorageException.PersistenceFailure("Failed to create review temporary file", e) }
        try {
            writeDurably(temp, encoded); restrictFile(temp)
            try { Files.move(temp, staged, StandardCopyOption.ATOMIC_MOVE) }
            catch (e: FileAlreadyExistsException) { throw HumanFidelityReviewStorageException.ConflictingIdentifier(record.reviewId) }
            restrictFile(staged)
            ensureAudit(record, HumanFidelityGovernanceAuditEventType.REVIEW_PREPARED, HumanFidelityGovernanceAuditOutcome.SUCCEEDED)
            HumanFidelityReviewPreparationResult.Prepared
        } catch (e: HumanFidelityReviewStorageException) { throw e }
        catch (e: HumanFidelityGovernanceAuditException) {
            throw HumanFidelityReviewStorageException.PersistenceFailure("Review was prepared but its PREPARED audit fact failed", e)
        } catch (e: Exception) { throw HumanFidelityReviewStorageException.PersistenceFailure("Failed to prepare human fidelity review", e) }
        finally { Files.deleteIfExists(temp) }
    }

    override suspend fun publishPrepared(reviewId: HumanFidelityReviewId): HumanFidelityReviewPublicationResult = mutex.withLock {
        requireSafe(reviewId)
        val target = target(reviewId)
        val staged = staged(reviewId)
        if (Files.exists(target)) {
            val record = decode(reviewId, target)
            ensureAudit(record, HumanFidelityGovernanceAuditEventType.REVIEW_PUBLISHED, HumanFidelityGovernanceAuditOutcome.SUCCEEDED)
            ensureAudit(record, HumanFidelityGovernanceAuditEventType.REVIEW_DUPLICATE_CONFIRMED, HumanFidelityGovernanceAuditOutcome.EXACT_DUPLICATE)
            return@withLock HumanFidelityReviewPublicationResult.AlreadyPublished
        }
        if (!Files.exists(staged)) throw HumanFidelityReviewStorageException.MissingPreparedRecord(reviewId)
        val record = decode(reviewId, staged)
        requireAudit(record, HumanFidelityGovernanceAuditEventType.REVIEW_PREPARED, HumanFidelityGovernanceAuditOutcome.SUCCEEDED)
        try {
            Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE); restrictFile(target)
        } catch (e: FileAlreadyExistsException) {
            throw HumanFidelityReviewStorageException.ConflictingIdentifier(reviewId)
        } catch (e: IOException) {
            throw HumanFidelityReviewStorageException.PersistenceFailure("Failed to publish prepared review '${reviewId.value}'", e)
        }
        try {
            ensureAudit(record, HumanFidelityGovernanceAuditEventType.REVIEW_PUBLISHED, HumanFidelityGovernanceAuditOutcome.SUCCEEDED)
        } catch (e: HumanFidelityGovernanceAuditException) {
            throw HumanFidelityReviewStorageException.PersistenceFailure(
                "Review was published but its PUBLISHED audit fact failed; readback remains fail-closed until deterministic retry", e,
            )
        }
        HumanFidelityReviewPublicationResult.Published
    }

    override suspend fun retrieve(reviewId: HumanFidelityReviewId): HumanFidelityReviewRecord? = mutex.withLock {
        requireSafe(reviewId)
        val path = target(reviewId)
        if (!Files.exists(path)) return@withLock null
        val record = decode(reviewId, path)
        requireAudit(record, HumanFidelityGovernanceAuditEventType.REVIEW_PUBLISHED, HumanFidelityGovernanceAuditOutcome.SUCCEEDED)
        record
    }

    override suspend fun listForExactTarget(target: HumanFidelityReviewTarget): List<HumanFidelityReviewRecord> = mutex.withLock {
        try {
            Files.list(root).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(EXTENSION) }.toList()
            }.map { path ->
                val reviewId = HumanFidelityReviewId(path.fileName.toString().removeSuffix(EXTENSION))
                val record = decode(reviewId, path)
                requireAudit(record, HumanFidelityGovernanceAuditEventType.REVIEW_PUBLISHED, HumanFidelityGovernanceAuditOutcome.SUCCEEDED)
                record
            }.filter { it.target == target }.sortedBy { it.reviewId.value }
        } catch (e: HumanFidelityReviewStorageException) { throw e }
        catch (e: Exception) { throw HumanFidelityReviewStorageException.PersistenceFailure("Failed to list exact-target reviews", e) }
    }

    private fun requireExact(reviewId: HumanFidelityReviewId, path: Path, expected: ByteArray) {
        val existing = decode(reviewId, path)
        if (!HumanFidelityReviewRecordCodec.encode(existing).contentEquals(expected)) {
            throw HumanFidelityReviewStorageException.ConflictingIdentifier(reviewId)
        }
    }

    private fun decode(reviewId: HumanFidelityReviewId, path: Path): HumanFidelityReviewRecord = try {
        val size = Files.size(path); require(size <= HumanFidelityReviewRecordCodec.MAX_RECORD_BYTES) { "record exceeds bounded size" }
        HumanFidelityReviewRecordCodec.decode(Files.readAllBytes(path)).also { require(it.reviewId == reviewId) }
    } catch (e: UnsupportedHumanFidelityReviewRepresentationVersionException) {
        throw HumanFidelityReviewStorageException.UnsupportedRepresentationVersion(reviewId, e.version)
    } catch (e: HumanFidelityReviewStorageException) { throw e }
    catch (e: Exception) { throw HumanFidelityReviewStorageException.CorruptRecord(reviewId, "record could not be decoded", e) }

    private suspend fun ensureAudit(
        record: HumanFidelityReviewRecord,
        eventType: HumanFidelityGovernanceAuditEventType,
        outcome: HumanFidelityGovernanceAuditOutcome,
    ) {
        val payloadDigest = HumanFidelityReviewRecordCodec.payloadSha256(record)
        val existing = audit.listForReview(record.reviewId).filter { it.eventType == eventType && it.outcome == outcome }
        if (existing.isNotEmpty()) {
            require(existing.size == 1 && existing.single().target == record.target &&
                existing.single().reviewPayloadSha256 == payloadDigest && existing.single().actorPrincipalId == record.reviewerPrincipalId) {
                "Conflicting human fidelity audit facts"
            }
            return
        }
        val eventId = HumanFidelityGovernanceAuditIdentity.derive(
            eventType, record.reviewerPrincipalId, record.reviewId, record.target, payloadDigest, outcome,
        )
        audit.append(HumanFidelityGovernanceAuditRecord(
            eventId, eventType, clock.instant(), record.reviewerPrincipalId, record.reviewId,
            record.target, payloadDigest, outcome,
        ))
    }

    private suspend fun requireAudit(
        record: HumanFidelityReviewRecord,
        eventType: HumanFidelityGovernanceAuditEventType,
        outcome: HumanFidelityGovernanceAuditOutcome,
    ) {
        val payloadDigest = HumanFidelityReviewRecordCodec.payloadSha256(record)
        val exact = audit.listForReview(record.reviewId).filter { it.eventType == eventType && it.outcome == outcome &&
            it.target == record.target && it.reviewPayloadSha256 == payloadDigest && it.actorPrincipalId == record.reviewerPrincipalId }
        if (exact.size != 1) throw HumanFidelityReviewStorageException.IncompleteAudit(record.reviewId, eventType)
    }

    private fun target(id: HumanFidelityReviewId) = root.resolve("${id.value}$EXTENSION")
    private fun staged(id: HumanFidelityReviewId) = prepared.resolve("${id.value}$EXTENSION")
    private fun requireSafe(id: HumanFidelityReviewId) {
        if (!SAFE.matches(id.value)) throw HumanFidelityReviewStorageException.UnsafeIdentifier(id)
    }

    private companion object {
        const val EXTENSION = ".human-fidelity-review-v1"
        val SAFE = Regex("^review-[0-9a-f]{64}$")
    }
}
