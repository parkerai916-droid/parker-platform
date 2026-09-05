package parker.core.runtime

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseIdentifierSafety
import parker.core.interfaces.CaseRecord
import parker.core.interfaces.CaseStorage
import parker.core.interfaces.CaseStorageException

/**
 * CASE-1. The production [CaseStorage] implementation, backed by the local filesystem --
 * mirroring [FileSystemEvidenceSourceManifestStorage]'s own write-once, temp-file-then-atomic-move,
 * no-overwrite discipline exactly. One immutable file per case; [create] throws
 * [CaseStorageException.DuplicateIdentifier] if a record already exists under the identity.
 *
 * [list] scans [storageRoot] directly -- CASE-1's own case count is bounded by however many the
 * owner has actually created (an organisational/usability count, never an evidence-scale one), so a
 * directory scan on every call is the smallest correct implementation; no separate index is kept.
 */
class FileSystemCaseStorage(storageRoot: Path) : CaseStorage {

    private val storageRoot: Path = storageRoot.toAbsolutePath().normalize()
    private val tempDirectory: Path
    private val mutex = Mutex()

    init {
        if (!Files.exists(this.storageRoot)) {
            throw CaseStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root does not exist")
        }
        if (!Files.isDirectory(this.storageRoot)) {
            throw CaseStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not a directory")
        }
        if (!Files.isWritable(this.storageRoot)) {
            throw CaseStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not writable")
        }
        tempDirectory = this.storageRoot.resolve(TEMP_DIRECTORY_NAME)
        try {
            Files.createDirectories(tempDirectory)
        } catch (e: IOException) {
            throw CaseStorageException.InvalidStorageRoot(
                storageRoot.toString(),
                "could not create temporary directory '$TEMP_DIRECTORY_NAME': ${e.message}",
            )
        }
    }

    override suspend fun create(case: CaseRecord) {
        requireSafe(case.caseId)
        val target = target(case.caseId)
        val encoded = CaseRecordCodec.encode(case)

        mutex.withLock {
            if (Files.exists(target)) {
                throw CaseStorageException.DuplicateIdentifier(case.caseId)
            }
            val temporary = try {
                Files.createTempFile(tempDirectory, TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX)
            } catch (e: IOException) {
                throw CaseStorageException.StorageIOFailure(
                    "Failed to create a temporary file for case identifier '${case.caseId.value}'",
                    e,
                )
            }
            try {
                FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                    val buffer = ByteBuffer.wrap(encoded)
                    while (buffer.hasRemaining()) channel.write(buffer)
                    channel.force(true)
                }
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
                } catch (e: FileAlreadyExistsException) {
                    throw CaseStorageException.DuplicateIdentifier(case.caseId)
                }
            } catch (e: CaseStorageException) {
                throw e
            } catch (e: IOException) {
                throw CaseStorageException.StorageIOFailure(
                    "Failed to persist case record for identifier '${case.caseId.value}'",
                    e,
                )
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
    }

    override suspend fun read(caseId: CaseId): CaseRecord? {
        requireSafe(caseId)
        val target = target(caseId)

        return mutex.withLock { readFile(caseId, target) }
    }

    override suspend fun list(): List<CaseRecord> = mutex.withLock {
        Files.list(storageRoot).use { paths ->
            paths.filter { it.isRegularCaseFile() }
                .map { path ->
                    val caseId = CaseId(path.fileName.toString().removeSuffix(EXTENSION))
                    readFile(caseId, path) ?: throw CaseStorageException.CorruptRecord(caseId, "record disappeared during listing")
                }
                .toList()
        }
    }

    private fun readFile(caseId: CaseId, target: Path): CaseRecord? {
        if (!Files.exists(target)) return null
        val content = try {
            val size = Files.size(target)
            if (size > MAX_RECORD_BYTES) {
                throw CaseStorageException.CorruptRecord(caseId, "case record size $size exceeds the $MAX_RECORD_BYTES-byte limit")
            }
            Files.readAllBytes(target)
        } catch (e: IOException) {
            throw CaseStorageException.StorageIOFailure("Failed to read case record for identifier '${caseId.value}'", e)
        }
        val case = try {
            CaseRecordCodec.decode(content)
        } catch (e: Exception) {
            throw CaseStorageException.CorruptRecord(caseId, e.message ?: "case record could not be decoded", e)
        }
        if (case.caseId != caseId) {
            throw CaseStorageException.CorruptRecord(caseId, "stored identity does not match file identity")
        }
        return case
    }

    private fun Path.isRegularCaseFile(): Boolean =
        Files.isRegularFile(this) && fileName.toString().endsWith(EXTENSION)

    private fun target(id: CaseId): Path = storageRoot.resolve("${id.value}$EXTENSION")

    private fun requireSafe(id: CaseId) {
        try {
            CaseIdentifierSafety.requireSafe(id)
        } catch (e: Exception) {
            throw CaseStorageException.UnsafeIdentifier(id)
        }
    }

    private companion object {
        const val MAX_RECORD_BYTES = 1L * 1024L * 1024L
        const val TEMP_DIRECTORY_NAME = ".tmp"
        const val TEMP_FILE_PREFIX = "case-"
        const val TEMP_FILE_SUFFIX = ".tmp"
        const val EXTENSION = ".case"
    }
}
