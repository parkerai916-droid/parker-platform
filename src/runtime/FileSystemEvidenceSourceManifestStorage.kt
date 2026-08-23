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
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceArtifactIdentifierSafety
import parker.core.interfaces.EvidenceSourceManifest
import parker.core.interfaces.EvidenceSourceManifestStorage
import parker.core.interfaces.EvidenceSourceManifestStorageException

/**
 * Document Ingestion, Authoritative Source Manifest Foundation
 * Implementation. The production [EvidenceSourceManifestStorage]
 * implementation, backed by the local filesystem -- mirroring
 * [FileSystemDerivativeGenerationStorage]'s own write-once,
 * temp-file-then-atomic-move, no-overwrite discipline exactly (itself
 * mirroring [FileSystemEvidenceArtifactStorage]'s own established
 * pattern), and reusing [EvidenceArtifactIdentifierSafety] directly --
 * the manifest's own key is the same [EvidenceArtifactId] type the
 * source bytes themselves are keyed by, so the identical safety rule
 * applies without needing a second, duplicated implementation.
 *
 * One immutable file per identity. A manifest, once written, is never
 * overwritten -- [write] throws
 * [EvidenceSourceManifestStorageException.DuplicateIdentifier] if
 * content already exists under the identity, mirroring
 * [EvidenceSourceManifest]'s own immutability rule (Scope Lock Section
 * 9). [delete] physically removes the file with no tombstone, mirroring
 * [EvidenceArtifactStorage.delete]'s own "deliberately not gated"
 * discipline -- gating remains one level up, in whatever governed
 * deletion authority calls it.
 *
 * ## What this does not guarantee (disclosed, not claimed closed)
 *
 * - **No cross-process concurrency guarantee.** [mutex] is an in-process
 *   guard only -- two separate Parker processes pointed at the same
 *   [storageRoot] are not coordinated by this implementation, exactly as
 *   [FileSystemEvidenceArtifactStorage] and
 *   [FileSystemDerivativeGenerationStorage] already disclose for the
 *   identical reason.
 * - **No protection against OS- or administrator-level access**, for the
 *   same reason [FileSystemEvidenceArtifactStorage] discloses this
 *   limitation.
 */
class FileSystemEvidenceSourceManifestStorage(storageRoot: Path) : EvidenceSourceManifestStorage {

    private val storageRoot: Path = storageRoot.toAbsolutePath().normalize()
    private val tempDirectory: Path
    private val mutex = Mutex()

    init {
        if (!Files.exists(this.storageRoot)) {
            throw EvidenceSourceManifestStorageException.InvalidStorageRoot(
                storageRoot.toString(),
                "storage root does not exist",
            )
        }
        if (!Files.isDirectory(this.storageRoot)) {
            throw EvidenceSourceManifestStorageException.InvalidStorageRoot(
                storageRoot.toString(),
                "storage root is not a directory",
            )
        }
        if (!Files.isWritable(this.storageRoot)) {
            throw EvidenceSourceManifestStorageException.InvalidStorageRoot(
                storageRoot.toString(),
                "storage root is not writable",
            )
        }
        tempDirectory = this.storageRoot.resolve(TEMP_DIRECTORY_NAME)
        try {
            Files.createDirectories(tempDirectory)
        } catch (e: IOException) {
            throw EvidenceSourceManifestStorageException.InvalidStorageRoot(
                storageRoot.toString(),
                "could not create temporary directory '$TEMP_DIRECTORY_NAME': ${e.message}",
            )
        }
    }

    override suspend fun write(manifest: EvidenceSourceManifest) {
        requireSafe(manifest.evidenceArtifactId)
        val target = target(manifest.evidenceArtifactId)
        val encoded = EvidenceSourceManifestCodec.encode(manifest)

        mutex.withLock {
            if (Files.exists(target)) {
                throw EvidenceSourceManifestStorageException.DuplicateIdentifier(manifest.evidenceArtifactId)
            }
            val temporary = try {
                Files.createTempFile(tempDirectory, TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX)
            } catch (e: IOException) {
                throw EvidenceSourceManifestStorageException.StorageIOFailure(
                    "Failed to create a temporary file for evidence source manifest identifier " +
                        "'${manifest.evidenceArtifactId.value}'",
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
                    throw EvidenceSourceManifestStorageException.DuplicateIdentifier(manifest.evidenceArtifactId)
                }
            } catch (e: EvidenceSourceManifestStorageException) {
                throw e
            } catch (e: IOException) {
                throw EvidenceSourceManifestStorageException.StorageIOFailure(
                    "Failed to persist evidence source manifest for identifier '${manifest.evidenceArtifactId.value}'",
                    e,
                )
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
    }

    override suspend fun read(evidenceArtifactId: EvidenceArtifactId): EvidenceSourceManifest? {
        requireSafe(evidenceArtifactId)
        val target = target(evidenceArtifactId)

        return mutex.withLock {
            if (!Files.exists(target)) return@withLock null
            val content = try {
                val size = Files.size(target)
                if (size > MAX_RECORD_BYTES) {
                    throw EvidenceSourceManifestStorageException.CorruptRecord(
                        evidenceArtifactId,
                        "manifest size $size exceeds the $MAX_RECORD_BYTES-byte limit",
                    )
                }
                Files.readAllBytes(target)
            } catch (e: IOException) {
                throw EvidenceSourceManifestStorageException.StorageIOFailure(
                    "Failed to read evidence source manifest for identifier '${evidenceArtifactId.value}'",
                    e,
                )
            }
            val manifest = try {
                EvidenceSourceManifestCodec.decode(content)
            } catch (e: Exception) {
                throw EvidenceSourceManifestStorageException.CorruptRecord(
                    evidenceArtifactId,
                    e.message ?: "manifest could not be decoded",
                    e,
                )
            }
            if (manifest.evidenceArtifactId != evidenceArtifactId) {
                throw EvidenceSourceManifestStorageException.CorruptRecord(
                    evidenceArtifactId,
                    "stored identity does not match file identity",
                )
            }
            manifest
        }
    }

    override suspend fun delete(evidenceArtifactId: EvidenceArtifactId): Boolean {
        requireSafe(evidenceArtifactId)
        val target = target(evidenceArtifactId)

        return mutex.withLock {
            try {
                Files.deleteIfExists(target)
            } catch (e: IOException) {
                throw EvidenceSourceManifestStorageException.StorageIOFailure(
                    "Failed to delete evidence source manifest for identifier '${evidenceArtifactId.value}'",
                    e,
                )
            }
        }
    }

    private fun target(id: EvidenceArtifactId): Path = storageRoot.resolve("${id.value}.manifest")

    private fun requireSafe(id: EvidenceArtifactId) {
        try {
            EvidenceArtifactIdentifierSafety.requireSafe(id)
        } catch (e: Exception) {
            throw EvidenceSourceManifestStorageException.UnsafeIdentifier(id)
        }
    }

    private companion object {
        const val MAX_RECORD_BYTES = 1L * 1024L * 1024L
        const val TEMP_DIRECTORY_NAME = ".tmp"
        const val TEMP_FILE_PREFIX = "evidence-source-manifest-"
        const val TEMP_FILE_SUFFIX = ".tmp"
    }
}
