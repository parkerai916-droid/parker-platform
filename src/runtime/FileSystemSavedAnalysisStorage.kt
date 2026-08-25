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
import parker.core.interfaces.SavedAnalysisId
import parker.core.interfaces.SavedAnalysisRecord
import parker.core.interfaces.SavedAnalysisStorage
import parker.core.interfaces.SavedAnalysisStorageException

/**
 * Reviewed Analysis Result — Explicit Owner Save. Mirrors
 * [FileSystemDerivativeGenerationStorage]'s own exact filesystem-storage
 * discipline: identifier-safe filenames, a `.tmp`/`.prepared` staging pair,
 * atomic moves, write-once (never overwrites), bounded reads, and honest
 * corrupt/unsupported-version fail-closed handling -- a wholly separate
 * storage root from Evidence/Derivative Generation/Derivative Content/
 * Memory/Knowledge, never nested inside or sharing a filename namespace
 * with any of them.
 */
class FileSystemSavedAnalysisStorage(storageRoot: Path) : SavedAnalysisStorage {
    private val storageRoot = storageRoot.toAbsolutePath().normalize()
    private val tempDirectory: Path
    private val preparedDirectory: Path
    private val mutex = Mutex()

    init {
        if (!Files.exists(this.storageRoot)) throw SavedAnalysisStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root does not exist")
        if (!Files.isDirectory(this.storageRoot)) throw SavedAnalysisStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not a directory")
        if (!Files.isWritable(this.storageRoot)) throw SavedAnalysisStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not writable")
        tempDirectory = this.storageRoot.resolve(".tmp")
        preparedDirectory = this.storageRoot.resolve(".prepared")
        try {
            Files.createDirectories(tempDirectory)
            Files.createDirectories(preparedDirectory)
        } catch (e: IOException) {
            throw SavedAnalysisStorageException.InvalidStorageRoot(storageRoot.toString(), "could not create temporary directory: ${e.message}")
        }
    }

    override suspend fun prepare(record: SavedAnalysisRecord) {
        requireSafe(record.savedAnalysisId)
        val target = target(record.savedAnalysisId)
        val preparedTarget = preparedTarget(record.savedAnalysisId)
        val encoded = SavedAnalysisRecordCodec.encode(record)
        mutex.withLock {
            if (Files.exists(target) || Files.exists(preparedTarget)) {
                throw SavedAnalysisStorageException.DuplicateIdentifier(record.savedAnalysisId)
            }
            val temporary = try {
                Files.createTempFile(tempDirectory, "saved-analysis-", ".tmp")
            } catch (e: IOException) {
                throw SavedAnalysisStorageException.PersistenceFailure("Failed to create saved analysis temporary file", e)
            }
            try {
                FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                    val buffer = ByteBuffer.wrap(encoded)
                    while (buffer.hasRemaining()) channel.write(buffer)
                    channel.force(true)
                }
                try {
                    Files.move(temporary, preparedTarget, StandardCopyOption.ATOMIC_MOVE)
                } catch (e: FileAlreadyExistsException) {
                    throw SavedAnalysisStorageException.DuplicateIdentifier(record.savedAnalysisId)
                }
            } catch (e: SavedAnalysisStorageException) {
                throw e
            } catch (e: IOException) {
                throw SavedAnalysisStorageException.PersistenceFailure("Failed to persist saved analysis '${record.savedAnalysisId.value}'", e)
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
    }

    override suspend fun publishPrepared(savedAnalysisId: SavedAnalysisId) {
        requireSafe(savedAnalysisId)
        val prepared = preparedTarget(savedAnalysisId)
        val target = target(savedAnalysisId)
        mutex.withLock {
            if (Files.exists(target)) throw SavedAnalysisStorageException.DuplicateIdentifier(savedAnalysisId)
            if (!Files.exists(prepared)) {
                throw SavedAnalysisStorageException.PersistenceFailure(
                    "No prepared saved analysis exists for '${savedAnalysisId.value}'",
                    IOException("prepared record not found"),
                )
            }
            val preparedRecord = try {
                val size = Files.size(prepared)
                if (size > MAX_RECORD_BYTES) error("prepared record exceeds the $MAX_RECORD_BYTES-byte limit")
                SavedAnalysisRecordCodec.decode(Files.readAllBytes(prepared))
            } catch (e: UnsupportedSavedAnalysisRepresentationVersionException) {
                throw SavedAnalysisStorageException.UnsupportedRepresentationVersion(savedAnalysisId, e.version)
            } catch (e: Exception) {
                throw SavedAnalysisStorageException.CorruptRecord(
                    savedAnalysisId,
                    e.message ?: "prepared record could not be decoded",
                    e,
                )
            }
            if (preparedRecord.savedAnalysisId != savedAnalysisId) {
                throw SavedAnalysisStorageException.CorruptRecord(
                    savedAnalysisId,
                    "prepared identity does not match publication identity",
                )
            }
            try {
                Files.move(prepared, target, StandardCopyOption.ATOMIC_MOVE)
            } catch (e: FileAlreadyExistsException) {
                throw SavedAnalysisStorageException.DuplicateIdentifier(savedAnalysisId)
            } catch (e: IOException) {
                throw SavedAnalysisStorageException.PersistenceFailure(
                    "Failed to publish prepared saved analysis '${savedAnalysisId.value}'",
                    e,
                )
            }
        }
    }

    override suspend fun retrieve(savedAnalysisId: SavedAnalysisId): SavedAnalysisRecord? {
        requireSafe(savedAnalysisId)
        val target = target(savedAnalysisId)
        return mutex.withLock {
            if (!Files.exists(target)) return@withLock null
            val content = try {
                val size = Files.size(target)
                if (size > MAX_RECORD_BYTES) {
                    throw SavedAnalysisStorageException.CorruptRecord(
                        savedAnalysisId,
                        "record size $size exceeds the $MAX_RECORD_BYTES-byte limit",
                    )
                }
                Files.readAllBytes(target)
            } catch (e: IOException) {
                throw SavedAnalysisStorageException.PersistenceFailure("Failed to read saved analysis '${savedAnalysisId.value}'", e)
            }
            val record = try {
                SavedAnalysisRecordCodec.decode(content)
            } catch (e: UnsupportedSavedAnalysisRepresentationVersionException) {
                throw SavedAnalysisStorageException.UnsupportedRepresentationVersion(savedAnalysisId, e.version)
            } catch (e: Exception) {
                throw SavedAnalysisStorageException.CorruptRecord(savedAnalysisId, e.message ?: "record could not be decoded", e)
            }
            if (record.savedAnalysisId != savedAnalysisId) {
                throw SavedAnalysisStorageException.CorruptRecord(savedAnalysisId, "stored identity does not match file identity")
            }
            record
        }
    }

    override suspend fun listRecentIds(maxCount: Int): List<SavedAnalysisId> {
        require(maxCount > 0) { "maxCount must be positive" }
        return mutex.withLock {
            val entries = try {
                Files.list(storageRoot).use { stream ->
                    stream
                        .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(EXTENSION) }
                        .toList()
                }
            } catch (e: IOException) {
                throw SavedAnalysisStorageException.PersistenceFailure("Failed to list saved analysis storage root", e)
            }
            entries
                .sortedByDescending { runCatching { Files.getLastModifiedTime(it) }.getOrNull() }
                .take(maxCount)
                .map { path -> SavedAnalysisId(path.fileName.toString().removeSuffix(EXTENSION)) }
        }
    }

    private fun target(id: SavedAnalysisId): Path = storageRoot.resolve("${id.value}$EXTENSION")
    private fun preparedTarget(id: SavedAnalysisId): Path = preparedDirectory.resolve("${id.value}$EXTENSION")

    private fun requireSafe(id: SavedAnalysisId) {
        if (!SAFE_IDENTIFIER.matches(id.value) || id.value in RESERVED_NAMES || isReservedNumberedName(id.value)) {
            throw SavedAnalysisStorageException.UnsafeIdentifier(id)
        }
    }

    private fun isReservedNumberedName(value: String): Boolean =
        value.length == 4 && value.take(3) in setOf("com", "lpt") && value.last() in '1'..'9'

    private companion object {
        const val MAX_RECORD_BYTES = 32L * 1024L * 1024L
        const val EXTENSION = ".saved-analysis"
        val SAFE_IDENTIFIER = Regex("^[a-z0-9_-]+$")
        val RESERVED_NAMES = setOf("con", "prn", "aux", "nul")
    }
}
