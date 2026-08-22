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
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeGenerationStorage
import parker.core.interfaces.DerivativeGenerationStorageException

class FileSystemDerivativeGenerationStorage(storageRoot: Path) : DerivativeGenerationStorage {
    private val storageRoot = storageRoot.toAbsolutePath().normalize()
    private val tempDirectory: Path
    private val mutex = Mutex()

    init {
        if (!Files.exists(this.storageRoot)) throw DerivativeGenerationStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root does not exist")
        if (!Files.isDirectory(this.storageRoot)) throw DerivativeGenerationStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not a directory")
        if (!Files.isWritable(this.storageRoot)) throw DerivativeGenerationStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not writable")
        tempDirectory = this.storageRoot.resolve(".tmp")
        try {
            Files.createDirectories(tempDirectory)
        } catch (e: IOException) {
            throw DerivativeGenerationStorageException.InvalidStorageRoot(storageRoot.toString(), "could not create temporary directory: ${e.message}")
        }
    }

    override suspend fun admit(record: DerivativeGenerationRecord) {
        requireSafe(record.derivativeGenerationId)
        val target = target(record.derivativeGenerationId)
        val encoded = DerivativeGenerationRecordCodec.encode(record)
        mutex.withLock {
            if (Files.exists(target)) throw DerivativeGenerationStorageException.DuplicateIdentifier(record.derivativeGenerationId)
            val temporary = try {
                Files.createTempFile(tempDirectory, "derivative-generation-", ".tmp")
            } catch (e: IOException) {
                throw DerivativeGenerationStorageException.PersistenceFailure("Failed to create derivative generation temporary file", e)
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
                    throw DerivativeGenerationStorageException.DuplicateIdentifier(record.derivativeGenerationId)
                }
            } catch (e: DerivativeGenerationStorageException) {
                throw e
            } catch (e: IOException) {
                throw DerivativeGenerationStorageException.PersistenceFailure("Failed to persist derivative generation '${record.derivativeGenerationId.value}'", e)
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
    }

    override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId): DerivativeGenerationRecord? {
        requireSafe(derivativeGenerationId)
        val target = target(derivativeGenerationId)
        return mutex.withLock {
            if (!Files.exists(target)) return@withLock null
            val content = try {
                val size = Files.size(target)
                if (size > MAX_RECORD_BYTES) {
                    throw DerivativeGenerationStorageException.CorruptRecord(
                        derivativeGenerationId,
                        "record size $size exceeds the $MAX_RECORD_BYTES-byte limit",
                    )
                }
                Files.readAllBytes(target)
            } catch (e: IOException) {
                throw DerivativeGenerationStorageException.PersistenceFailure("Failed to read derivative generation '${derivativeGenerationId.value}'", e)
            }
            val record = try {
                DerivativeGenerationRecordCodec.decode(content)
            } catch (e: Exception) {
                throw DerivativeGenerationStorageException.CorruptRecord(derivativeGenerationId, e.message ?: "record could not be decoded", e)
            }
            if (record.derivativeGenerationId != derivativeGenerationId) {
                throw DerivativeGenerationStorageException.CorruptRecord(derivativeGenerationId, "stored identity does not match file identity")
            }
            record
        }
    }

    private fun target(id: DerivativeGenerationId): Path = storageRoot.resolve("${id.value}.derivative")

    private fun requireSafe(id: DerivativeGenerationId) {
        if (!SAFE_IDENTIFIER.matches(id.value) || id.value in RESERVED_NAMES || isReservedNumberedName(id.value)) {
            throw DerivativeGenerationStorageException.UnsafeIdentifier(id)
        }
    }

    private fun isReservedNumberedName(value: String): Boolean =
        value.length == 4 && value.take(3) in setOf("com", "lpt") && value.last() in '1'..'9'

    private companion object {
        const val MAX_RECORD_BYTES = 32L * 1024L * 1024L
        val SAFE_IDENTIFIER = Regex("^[a-z0-9_-]+$")
        val RESERVED_NAMES = setOf("con", "prn", "aux", "nul")
    }
}
