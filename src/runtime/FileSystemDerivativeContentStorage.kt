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
import parker.core.interfaces.DerivativeContentEntry
import parker.core.interfaces.DerivativeContentStorage
import parker.core.interfaces.DerivativeContentStorageException
import parker.core.interfaces.DerivativeGenerationId

/**
 * Document Ingestion — Derivative Content Persistence and Retrieval.
 * Governed by `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`
 * §4/§9/§10. Mirrors [FileSystemDerivativeGenerationStorage]'s own
 * established shape exactly (temp-file -> `.prepared` staging -> atomic
 * rename, [DerivativeGenerationId]-only identifiers, the same
 * safe-identifier discipline) — a second, independent store, never a
 * variant of the Record store itself, and never sharing its storage root.
 */
class FileSystemDerivativeContentStorage(storageRoot: Path) : DerivativeContentStorage {
    private val storageRoot = storageRoot.toAbsolutePath().normalize()
    private val tempDirectory: Path
    private val preparedDirectory: Path
    private val mutex = Mutex()

    init {
        if (!Files.exists(this.storageRoot)) throw DerivativeContentStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root does not exist")
        if (!Files.isDirectory(this.storageRoot)) throw DerivativeContentStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not a directory")
        if (!Files.isWritable(this.storageRoot)) throw DerivativeContentStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not writable")
        tempDirectory = this.storageRoot.resolve(".tmp")
        preparedDirectory = this.storageRoot.resolve(".prepared")
        try {
            Files.createDirectories(tempDirectory)
            Files.createDirectories(preparedDirectory)
        } catch (e: IOException) {
            throw DerivativeContentStorageException.InvalidStorageRoot(storageRoot.toString(), "could not create temporary directory: ${e.message}")
        }
    }

    override suspend fun prepare(entry: DerivativeContentEntry) {
        requireSafe(entry.derivativeGenerationId)
        val target = target(entry.derivativeGenerationId)
        val preparedTarget = preparedTarget(entry.derivativeGenerationId)
        val encoded = try {
            DerivativeContentCodec.encode(entry)
        } catch (e: IllegalArgumentException) {
            throw DerivativeContentStorageException.PersistenceFailure("Derivative content for '${entry.derivativeGenerationId.value}' exceeds the storage bound", e)
        }
        mutex.withLock {
            if (Files.exists(target) || Files.exists(preparedTarget)) {
                throw DerivativeContentStorageException.DuplicateIdentifier(entry.derivativeGenerationId)
            }
            val temporary = try {
                Files.createTempFile(tempDirectory, "derivative-content-", ".tmp")
            } catch (e: IOException) {
                throw DerivativeContentStorageException.PersistenceFailure("Failed to create derivative content temporary file", e)
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
                    throw DerivativeContentStorageException.DuplicateIdentifier(entry.derivativeGenerationId)
                }
            } catch (e: DerivativeContentStorageException) {
                throw e
            } catch (e: IOException) {
                throw DerivativeContentStorageException.PersistenceFailure("Failed to persist derivative content '${entry.derivativeGenerationId.value}'", e)
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
    }

    override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) {
        requireSafe(derivativeGenerationId)
        val prepared = preparedTarget(derivativeGenerationId)
        val target = target(derivativeGenerationId)
        mutex.withLock {
            if (Files.exists(target)) throw DerivativeContentStorageException.DuplicateIdentifier(derivativeGenerationId)
            if (!Files.exists(prepared)) {
                throw DerivativeContentStorageException.PersistenceFailure(
                    "No prepared derivative content exists for '${derivativeGenerationId.value}'",
                    IOException("prepared content not found"),
                )
            }
            val preparedEntry = try {
                val size = Files.size(prepared)
                if (size > DerivativeContentCodec.MAX_ENTRY_BYTES) error("prepared content exceeds the ${DerivativeContentCodec.MAX_ENTRY_BYTES}-byte limit")
                DerivativeContentCodec.decode(Files.readAllBytes(prepared))
            } catch (e: Exception) {
                throw DerivativeContentStorageException.CorruptContent(
                    derivativeGenerationId,
                    e.message ?: "prepared content could not be decoded",
                    e,
                )
            }
            if (preparedEntry.derivativeGenerationId != derivativeGenerationId) {
                throw DerivativeContentStorageException.CorruptContent(
                    derivativeGenerationId,
                    "prepared identity does not match publication identity",
                )
            }
            try {
                Files.move(prepared, target, StandardCopyOption.ATOMIC_MOVE)
            } catch (e: FileAlreadyExistsException) {
                throw DerivativeContentStorageException.DuplicateIdentifier(derivativeGenerationId)
            } catch (e: IOException) {
                throw DerivativeContentStorageException.PersistenceFailure(
                    "Failed to publish prepared derivative content '${derivativeGenerationId.value}'",
                    e,
                )
            }
        }
    }

    override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId): DerivativeContentEntry? {
        requireSafe(derivativeGenerationId)
        val target = target(derivativeGenerationId)
        return mutex.withLock {
            if (!Files.exists(target)) return@withLock null
            val content = try {
                val size = Files.size(target)
                if (size > DerivativeContentCodec.MAX_ENTRY_BYTES) {
                    throw DerivativeContentStorageException.CorruptContent(
                        derivativeGenerationId,
                        "content size $size exceeds the ${DerivativeContentCodec.MAX_ENTRY_BYTES}-byte limit",
                    )
                }
                Files.readAllBytes(target)
            } catch (e: IOException) {
                throw DerivativeContentStorageException.PersistenceFailure("Failed to read derivative content '${derivativeGenerationId.value}'", e)
            }
            val entry = try {
                DerivativeContentCodec.decode(content)
            } catch (e: DerivativeContentCodec.UnsupportedRepresentationVersionException) {
                throw DerivativeContentStorageException.UnsupportedRepresentationVersion(derivativeGenerationId, e.version)
            } catch (e: Exception) {
                throw DerivativeContentStorageException.CorruptContent(derivativeGenerationId, e.message ?: "content could not be decoded", e)
            }
            if (entry.derivativeGenerationId != derivativeGenerationId) {
                throw DerivativeContentStorageException.CorruptContent(derivativeGenerationId, "stored identity does not match file identity")
            }
            entry
        }
    }

    private fun target(id: DerivativeGenerationId): Path = storageRoot.resolve("${id.value}.content")
    private fun preparedTarget(id: DerivativeGenerationId): Path = preparedDirectory.resolve("${id.value}.content")

    private fun requireSafe(id: DerivativeGenerationId) {
        if (!SAFE_IDENTIFIER.matches(id.value) || id.value in RESERVED_NAMES || isReservedNumberedName(id.value)) {
            throw DerivativeContentStorageException.UnsafeIdentifier(id)
        }
    }

    private fun isReservedNumberedName(value: String): Boolean =
        value.length == 4 && value.take(3) in setOf("com", "lpt") && value.last() in '1'..'9'

    private companion object {
        val SAFE_IDENTIFIER = Regex("^[a-z0-9_-]+$")
        val RESERVED_NAMES = setOf("con", "prn", "aux", "nul")
    }
}
