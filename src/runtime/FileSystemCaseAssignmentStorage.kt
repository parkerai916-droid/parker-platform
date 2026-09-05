package parker.core.runtime

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.CaseAssignmentRecord
import parker.core.interfaces.CaseAssignmentStorage
import parker.core.interfaces.CaseAssignmentStorageException
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceArtifactIdentifierSafety

/**
 * CASE-1. The production [CaseAssignmentStorage] implementation. Unlike [FileSystemCaseStorage]
 * (write-once, immutable), this is a mutable "current pointer" store: [writeAssignment] always
 * durably replaces whatever was previously stored for the same [EvidenceArtifactId] -- reassignment
 * is the entire point of this store, so there is no duplicate-identifier refusal here. Durability
 * still uses the same temp-file-then-atomic-move discipline as every other store in this codebase;
 * [StandardCopyOption.REPLACE_EXISTING] is the only difference from [FileSystemCaseStorage]'s move.
 *
 * Reuses [EvidenceArtifactIdentifierSafety] directly -- this store's own key is the same
 * [EvidenceArtifactId] type evidence itself is keyed by, so the identical safety rule applies
 * without needing a second, duplicated implementation.
 */
class FileSystemCaseAssignmentStorage(storageRoot: Path) : CaseAssignmentStorage {

    private val storageRoot: Path = storageRoot.toAbsolutePath().normalize()
    private val tempDirectory: Path
    private val mutex = Mutex()

    init {
        if (!Files.exists(this.storageRoot)) {
            throw CaseAssignmentStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root does not exist")
        }
        if (!Files.isDirectory(this.storageRoot)) {
            throw CaseAssignmentStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not a directory")
        }
        if (!Files.isWritable(this.storageRoot)) {
            throw CaseAssignmentStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not writable")
        }
        tempDirectory = this.storageRoot.resolve(TEMP_DIRECTORY_NAME)
        try {
            Files.createDirectories(tempDirectory)
        } catch (e: IOException) {
            throw CaseAssignmentStorageException.InvalidStorageRoot(
                storageRoot.toString(),
                "could not create temporary directory '$TEMP_DIRECTORY_NAME': ${e.message}",
            )
        }
    }

    override suspend fun readAssignment(evidenceArtifactId: EvidenceArtifactId): CaseAssignmentRecord? {
        requireSafe(evidenceArtifactId)
        val target = target(evidenceArtifactId)

        return mutex.withLock {
            if (!Files.exists(target)) return@withLock null
            val content = try {
                val size = Files.size(target)
                if (size > MAX_RECORD_BYTES) {
                    throw CaseAssignmentStorageException.CorruptRecord(
                        evidenceArtifactId,
                        "assignment record size $size exceeds the $MAX_RECORD_BYTES-byte limit",
                    )
                }
                Files.readAllBytes(target)
            } catch (e: IOException) {
                throw CaseAssignmentStorageException.StorageIOFailure(
                    "Failed to read case assignment for identifier '${evidenceArtifactId.value}'",
                    e,
                )
            }
            val record = try {
                CaseAssignmentRecordCodec.decode(content)
            } catch (e: Exception) {
                throw CaseAssignmentStorageException.CorruptRecord(
                    evidenceArtifactId,
                    e.message ?: "assignment record could not be decoded",
                    e,
                )
            }
            if (record.evidenceArtifactId != evidenceArtifactId) {
                throw CaseAssignmentStorageException.CorruptRecord(evidenceArtifactId, "stored identity does not match file identity")
            }
            record
        }
    }

    override suspend fun writeAssignment(record: CaseAssignmentRecord) {
        requireSafe(record.evidenceArtifactId)
        val target = target(record.evidenceArtifactId)
        val encoded = CaseAssignmentRecordCodec.encode(record)

        mutex.withLock {
            val temporary = try {
                Files.createTempFile(tempDirectory, TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX)
            } catch (e: IOException) {
                throw CaseAssignmentStorageException.StorageIOFailure(
                    "Failed to create a temporary file for case assignment identifier '${record.evidenceArtifactId.value}'",
                    e,
                )
            }
            try {
                FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                    val buffer = ByteBuffer.wrap(encoded)
                    while (buffer.hasRemaining()) channel.write(buffer)
                    channel.force(true)
                }
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: IOException) {
                throw CaseAssignmentStorageException.StorageIOFailure(
                    "Failed to persist case assignment for identifier '${record.evidenceArtifactId.value}'",
                    e,
                )
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
    }

    private fun target(id: EvidenceArtifactId): Path = storageRoot.resolve("${id.value}.assignment")

    private fun requireSafe(id: EvidenceArtifactId) {
        try {
            EvidenceArtifactIdentifierSafety.requireSafe(id)
        } catch (e: Exception) {
            throw CaseAssignmentStorageException.UnsafeIdentifier(id)
        }
    }

    private companion object {
        const val MAX_RECORD_BYTES = 1L * 1024L * 1024L
        const val TEMP_DIRECTORY_NAME = ".tmp"
        const val TEMP_FILE_PREFIX = "case-assignment-"
        const val TEMP_FILE_SUFFIX = ".tmp"
    }
}
