package parker.core.runtime

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.*

class FileSystemHumanFidelityGovernanceAudit(storageRoot: Path) : HumanFidelityGovernanceAudit {
    private val root = storageRoot.toAbsolutePath().normalize()
    private val temporary = root.resolve(".tmp")
    private val mutex = Mutex()

    init {
        if (!Files.exists(root) || !Files.isDirectory(root) || !Files.isWritable(root)) {
            throw HumanFidelityGovernanceAuditException.InvalidStorageRoot(root.toString(), "root must exist as a writable directory")
        }
        try {
            Files.createDirectories(temporary); restrictDirectory(root); restrictDirectory(temporary)
        } catch (e: IOException) {
            throw HumanFidelityGovernanceAuditException.InvalidStorageRoot(root.toString(), "could not initialize audit directories")
        }
    }

    override suspend fun append(record: HumanFidelityGovernanceAuditRecord): HumanFidelityGovernanceAuditAppendResult = mutex.withLock {
        val encoded = HumanFidelityGovernanceAuditCodec.encode(record)
        val target = target(record.eventId)
        if (Files.exists(target)) {
            val existing = decode(record.eventId, target)
            if (!HumanFidelityGovernanceAuditCodec.encode(existing).contentEquals(encoded)) {
                throw HumanFidelityGovernanceAuditException.ConflictingIdentifier(record.eventId)
            }
            return@withLock HumanFidelityGovernanceAuditAppendResult.AlreadyPresent
        }
        val temp = try { Files.createTempFile(temporary, "fidelity-audit-", ".tmp") }
        catch (e: IOException) { throw HumanFidelityGovernanceAuditException.PersistenceFailure("Failed to create audit temporary file", e) }
        try {
            writeDurably(temp, encoded); restrictFile(temp)
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE) }
            catch (e: FileAlreadyExistsException) {
                val existing = decode(record.eventId, target)
                if (!HumanFidelityGovernanceAuditCodec.encode(existing).contentEquals(encoded)) {
                    throw HumanFidelityGovernanceAuditException.ConflictingIdentifier(record.eventId)
                }
                return@withLock HumanFidelityGovernanceAuditAppendResult.AlreadyPresent
            }
            restrictFile(target)
            HumanFidelityGovernanceAuditAppendResult.Appended
        } catch (e: HumanFidelityGovernanceAuditException) { throw e }
        catch (e: Exception) { throw HumanFidelityGovernanceAuditException.PersistenceFailure("Failed to append human fidelity audit event", e) }
        finally { Files.deleteIfExists(temp) }
    }

    override suspend fun retrieve(eventId: HumanFidelityGovernanceAuditEventId): HumanFidelityGovernanceAuditRecord? = mutex.withLock {
        val target = target(eventId)
        if (!Files.exists(target)) null else decode(eventId, target)
    }

    override suspend fun listForReview(reviewId: HumanFidelityReviewId): List<HumanFidelityGovernanceAuditRecord> = mutex.withLock {
        try {
            Files.list(root).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(EXTENSION) }.toList()
            }.map { path ->
                val eventId = HumanFidelityGovernanceAuditEventId(path.fileName.toString().removeSuffix(EXTENSION))
                decode(eventId, path)
            }.filter { it.reviewId == reviewId }.sortedBy { it.eventId.value }
        } catch (e: HumanFidelityGovernanceAuditException) { throw e }
        catch (e: Exception) { throw HumanFidelityGovernanceAuditException.PersistenceFailure("Failed to list human fidelity audit events", e) }
    }

    private fun decode(eventId: HumanFidelityGovernanceAuditEventId, path: Path): HumanFidelityGovernanceAuditRecord = try {
        val size = Files.size(path); require(size <= HumanFidelityGovernanceAuditCodec.MAX_RECORD_BYTES) { "record exceeds bounded size" }
        HumanFidelityGovernanceAuditCodec.decode(Files.readAllBytes(path)).also { require(it.eventId == eventId) }
    } catch (e: UnsupportedHumanFidelityAuditRepresentationVersionException) {
        throw HumanFidelityGovernanceAuditException.UnsupportedRepresentationVersion(eventId, e.version)
    } catch (e: HumanFidelityGovernanceAuditException) { throw e }
    catch (e: Exception) { throw HumanFidelityGovernanceAuditException.CorruptRecord(eventId, "record could not be decoded", e) }

    private fun target(id: HumanFidelityGovernanceAuditEventId) = root.resolve("${id.value}$EXTENSION")

    private companion object { const val EXTENSION = ".human-fidelity-audit-v1" }
}

internal fun writeDurably(path: Path, bytes: ByteArray) {
    FileChannel.open(path, StandardOpenOption.WRITE).use { channel ->
        val buffer = ByteBuffer.wrap(bytes)
        while (buffer.hasRemaining()) channel.write(buffer)
        channel.force(true)
    }
}

internal fun restrictFile(path: Path) {
    try { Files.setPosixFilePermissions(path, setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)) }
    catch (_: UnsupportedOperationException) { /* Non-POSIX test filesystem: established create-once semantics still apply. */ }
}

internal fun restrictDirectory(path: Path) {
    try {
        Files.setPosixFilePermissions(path, setOf(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
        ))
    } catch (_: UnsupportedOperationException) { /* Non-POSIX test filesystem. */ }
}
