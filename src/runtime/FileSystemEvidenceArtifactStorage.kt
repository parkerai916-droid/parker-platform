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
import parker.core.interfaces.EvidenceArtifactStorage
import parker.core.interfaces.EvidenceArtifactStorageException

/**
 * Evidence Custodian, Implementation Plan Phase 2, Unit 1: the production
 * [EvidenceArtifactStorage] implementation, backed by the local
 * filesystem -- the storage mechanism selected for this Unit after
 * comparing it against embedded-database and in-memory-only alternatives.
 * Local filesystem was chosen because every guarantee this Unit needs
 * (write-once enforcement, exact-byte preservation, atomic creation) is
 * already provided natively by the operating system, and this repository
 * currently has exactly one runtime dependency (`kotlinx-coroutines-core`)
 * -- adding an embedded database would trade a dependency for a guarantee
 * the filesystem already gives for free. No database, object store, cloud
 * storage, or networking is used anywhere in this file.
 *
 * ## Write strategy: temporary file, then atomic move -- never a direct
 * write to the final path
 *
 * A naive implementation opening the final path directly with
 * create-new/exclusive-create semantics looks correct until a crash
 * mid-write is considered: the create-new check would already have
 * succeeded, so the final path would exist but hold incomplete content,
 * and a retry would then incorrectly report "duplicate identifier"
 * against corrupted data. This implementation avoids that failure mode:
 * content is written and [FileChannel.force]d to a uniquely-named
 * temporary file inside [storageRoot]'s own `.tmp` subdirectory first,
 * and only moved into its final, identifier-derived path via
 * [Files.move] with [StandardCopyOption.ATOMIC_MOVE] once the write is
 * confirmed complete. A crash during the write phase can therefore only
 * ever orphan a temporary file -- it can never corrupt content already
 * readable under a real identifier.
 *
 * The temporary directory is created inside [storageRoot] itself
 * (never in a platform temp directory), guaranteeing the temporary file
 * and its final destination share one filesystem, which [Files.move]'s
 * own [StandardCopyOption.ATOMIC_MOVE] option requires in order to behave
 * atomically at all.
 *
 * ## What this establishes (see [EvidenceArtifactStorage]'s own KDoc for
 * the full statement)
 *
 * No overwrite (OS-level exclusive-create/atomic-move semantics, not an
 * application-level check-then-write race); exact-byte preservation
 * (bytes are read and written raw, with no transformation); no identifier
 * minting (an already-constructed [EvidenceArtifactId] is required).
 *
 * ## What this does not yet guarantee
 *
 * - **The move step's "fail if target exists" behaviour is not proven
 *   atomic by the JDK on every platform.** [Files.move] without
 *   `REPLACE_EXISTING` throws [FileAlreadyExistsException] if the target
 *   already exists, but the JDK does not document this check-and-move as
 *   atomic against a *simultaneous* mover the way POSIX `O_EXCL` file
 *   creation is. This implementation narrows the exposure with an
 *   existence check under [mutex] before attempting the move, and treats
 *   the move's own [FileAlreadyExistsException] as the authoritative
 *   guard -- but a theoretical race remains, and this is disclosed rather
 *   than claimed closed. In this Unit's own threat model the risk is low:
 *   identifiers are never caller-supplied and are only ever minted once,
 *   by a future governed acceptance path, never by two callers racing to
 *   use the same value.
 * - **No cross-process concurrency guarantee.** [mutex] is an in-process
 *   guard only; two separate Parker processes pointed at the same
 *   [storageRoot] are not coordinated by this implementation.
 * - **No protection against OS- or administrator-level access.** A
 *   process with sufficient OS privilege can still read, modify, or
 *   delete any file this implementation wrote. This implementation
 *   constrains what *Parker's own code* can do to a stored artefact; it
 *   cannot and does not constrain the underlying operating system's most
 *   privileged users.
 * - **No integrity verification.** No hash is computed or stored --
 *   correctly excluded, since nothing in the governing Contract Design
 *   strictly requires one at this layer.
 *
 * @param storageRoot An already-existing, writable directory, supplied by
 *   the caller -- never guessed, never defaulted. Validated once, at
 *   construction: [init] throws [EvidenceArtifactStorageException.InvalidStorageRoot]
 *   if [storageRoot] does not exist, is not a directory, is not writable,
 *   or if this implementation's own `.tmp` subdirectory cannot be created
 *   under it.
 */
class FileSystemEvidenceArtifactStorage(storageRoot: Path) : EvidenceArtifactStorage {

    private val storageRoot: Path = storageRoot.toAbsolutePath().normalize()
    private val tempDirectory: Path
    private val mutex = Mutex()

    init {
        if (!Files.exists(this.storageRoot)) {
            throw EvidenceArtifactStorageException.InvalidStorageRoot(
                storageRoot.toString(),
                "storage root does not exist",
            )
        }
        if (!Files.isDirectory(this.storageRoot)) {
            throw EvidenceArtifactStorageException.InvalidStorageRoot(
                storageRoot.toString(),
                "storage root is not a directory",
            )
        }
        if (!Files.isWritable(this.storageRoot)) {
            throw EvidenceArtifactStorageException.InvalidStorageRoot(
                storageRoot.toString(),
                "storage root is not writable",
            )
        }
        tempDirectory = this.storageRoot.resolve(TEMP_DIRECTORY_NAME)
        try {
            Files.createDirectories(tempDirectory)
        } catch (e: IOException) {
            throw EvidenceArtifactStorageException.InvalidStorageRoot(
                storageRoot.toString(),
                "could not create temporary-file directory '$TEMP_DIRECTORY_NAME': ${e.message}",
            )
        }
    }

    override suspend fun write(evidenceArtifactId: EvidenceArtifactId, content: ByteArray) {
        EvidenceArtifactIdentifierSafety.requireSafe(evidenceArtifactId)
        val targetPath = resolveTargetPath(evidenceArtifactId)

        mutex.withLock {
            if (Files.exists(targetPath)) {
                throw EvidenceArtifactStorageException.DuplicateIdentifier(evidenceArtifactId)
            }

            val tempFile = try {
                Files.createTempFile(tempDirectory, TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX)
            } catch (e: IOException) {
                throw EvidenceArtifactStorageException.StorageIOFailure(
                    "Failed to create a temporary file for evidence artifact identifier " +
                        "'${evidenceArtifactId.value}'",
                    e,
                )
            }

            try {
                writeAndForce(tempFile, content)
                try {
                    Files.move(tempFile, targetPath, StandardCopyOption.ATOMIC_MOVE)
                } catch (e: FileAlreadyExistsException) {
                    throw EvidenceArtifactStorageException.DuplicateIdentifier(evidenceArtifactId)
                }
            } catch (e: EvidenceArtifactStorageException) {
                throw e
            } catch (e: IOException) {
                throw EvidenceArtifactStorageException.StorageIOFailure(
                    "Failed to write evidence artifact content for identifier '${evidenceArtifactId.value}'",
                    e,
                )
            } finally {
                // No-op once the move above has succeeded (the file no longer exists at this
                // path); cleans up an orphaned temp file for every failure path above.
                Files.deleteIfExists(tempFile)
            }
        }
    }

    override suspend fun read(evidenceArtifactId: EvidenceArtifactId): ByteArray? {
        EvidenceArtifactIdentifierSafety.requireSafe(evidenceArtifactId)
        val targetPath = resolveTargetPath(evidenceArtifactId)

        return mutex.withLock {
            if (!Files.exists(targetPath)) {
                null
            } else {
                try {
                    Files.readAllBytes(targetPath)
                } catch (e: IOException) {
                    throw EvidenceArtifactStorageException.StorageIOFailure(
                        "Failed to read evidence artifact content for identifier '${evidenceArtifactId.value}'",
                        e,
                    )
                }
            }
        }
    }

    /**
     * Writes [content] to [tempFile] and forces it to durable storage
     * ([FileChannel.force]) before returning -- the concrete step that
     * makes the "temporary file" half of this Unit's crash-protection
     * strategy real rather than nominal: a crash after this call returns
     * but before the subsequent [Files.move] leaves a complete, valid
     * temporary file behind, never a partially-written one.
     */
    private fun writeAndForce(tempFile: Path, content: ByteArray) {
        FileChannel.open(tempFile, StandardOpenOption.WRITE).use { channel ->
            val buffer = ByteBuffer.wrap(content)
            // FileChannel.write's general contract permits a partial write per call (it is not
            // guaranteed to drain the whole buffer in one invocation) -- loop until every byte
            // is actually written, rather than assuming a single call suffices.
            while (buffer.hasRemaining()) {
                channel.write(buffer)
            }
            channel.force(true)
        }
    }

    private fun resolveTargetPath(evidenceArtifactId: EvidenceArtifactId): Path =
        storageRoot.resolve("${evidenceArtifactId.value}$FILE_EXTENSION")

    companion object {
        private const val TEMP_DIRECTORY_NAME = ".tmp"
        private const val TEMP_FILE_PREFIX = "evidence-artifact-"
        private const val TEMP_FILE_SUFFIX = ".tmp"
        private const val FILE_EXTENSION = ".evidence"
    }
}
