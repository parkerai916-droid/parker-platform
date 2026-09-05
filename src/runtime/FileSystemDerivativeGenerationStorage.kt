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
import parker.core.interfaces.DerivativeTransformation
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrDerivativeGenerationDiscovery

class FileSystemDerivativeGenerationStorage(storageRoot: Path) : DerivativeGenerationStorage, OcrDerivativeGenerationDiscovery {
    private val storageRoot = storageRoot.toAbsolutePath().normalize()
    private val tempDirectory: Path
    private val preparedDirectory: Path
    private val mutex = Mutex()

    init {
        if (!Files.exists(this.storageRoot)) throw DerivativeGenerationStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root does not exist")
        if (!Files.isDirectory(this.storageRoot)) throw DerivativeGenerationStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not a directory")
        if (!Files.isWritable(this.storageRoot)) throw DerivativeGenerationStorageException.InvalidStorageRoot(storageRoot.toString(), "storage root is not writable")
        tempDirectory = this.storageRoot.resolve(".tmp")
        preparedDirectory = this.storageRoot.resolve(".prepared")
        try {
            Files.createDirectories(tempDirectory)
            Files.createDirectories(preparedDirectory)
        } catch (e: IOException) {
            throw DerivativeGenerationStorageException.InvalidStorageRoot(storageRoot.toString(), "could not create temporary directory: ${e.message}")
        }
    }

    override suspend fun prepare(record: DerivativeGenerationRecord) {
        requireSafe(record.derivativeGenerationId)
        val target = target(record.derivativeGenerationId)
        val preparedTarget = preparedTarget(record.derivativeGenerationId)
        val encoded = DerivativeGenerationRecordCodec.encode(record)
        mutex.withLock {
            if (Files.exists(target) || Files.exists(preparedTarget)) {
                throw DerivativeGenerationStorageException.DuplicateIdentifier(record.derivativeGenerationId)
            }
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
                    Files.move(temporary, preparedTarget, StandardCopyOption.ATOMIC_MOVE)
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

    override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) {
        requireSafe(derivativeGenerationId)
        val prepared = preparedTarget(derivativeGenerationId)
        val target = target(derivativeGenerationId)
        mutex.withLock {
            if (Files.exists(target)) throw DerivativeGenerationStorageException.DuplicateIdentifier(derivativeGenerationId)
            if (!Files.exists(prepared)) {
                throw DerivativeGenerationStorageException.PersistenceFailure(
                    "No prepared derivative generation exists for '${derivativeGenerationId.value}'",
                    IOException("prepared record not found"),
                )
            }
            val preparedRecord = try {
                val size = Files.size(prepared)
                if (size > MAX_RECORD_BYTES) error("prepared record exceeds the $MAX_RECORD_BYTES-byte limit")
                DerivativeGenerationRecordCodec.decode(Files.readAllBytes(prepared))
            } catch (e: Exception) {
                throw DerivativeGenerationStorageException.CorruptRecord(
                    derivativeGenerationId,
                    e.message ?: "prepared record could not be decoded",
                    e,
                )
            }
            if (preparedRecord.derivativeGenerationId != derivativeGenerationId) {
                throw DerivativeGenerationStorageException.CorruptRecord(
                    derivativeGenerationId,
                    "prepared identity does not match publication identity",
                )
            }
            try {
                Files.move(prepared, target, StandardCopyOption.ATOMIC_MOVE)
            } catch (e: FileAlreadyExistsException) {
                throw DerivativeGenerationStorageException.DuplicateIdentifier(derivativeGenerationId)
            } catch (e: IOException) {
                throw DerivativeGenerationStorageException.PersistenceFailure(
                    "Failed to publish prepared derivative generation '${derivativeGenerationId.value}'",
                    e,
                )
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

    /**
     * UI-INGESTION-8B: `DOCUMENT_INGESTION_TIER_B_OCR_EXACT_EVIDENCE_DERIVATIVE_GENERATION_DISCOVERY_SCOPE_LOCK_AMENDMENT.md`'s
     * one authorised capability -- given an already-known [evidenceArtifactId], scans this
     * storage's own flat, published-record directory (never `.tmp`/`.prepared`, never a
     * client-supplied path) and returns every record rooted at exactly that artifact whose
     * `transformationHistory` contains [DerivativeTransformation.OCR]. This is a bounded,
     * paired-identity-filtered read, never a general `enumerate()`/`listAll()`: nothing about this
     * method lets a caller retrieve a record for any artifact other than the one it already
     * supplied, and no unfiltered record or path ever escapes this method.
     *
     * A record for another evidence artifact that fails to decode, exceeds the size limit, or is
     * otherwise unreadable is simply excluded from the result (fail-closed by omission) rather than
     * aborting discovery for the artifact actually requested -- unlike [retrieve], which is asked
     * for one specific, already-identified record and so throws on corruption instead. Ordering is
     * deterministic: [DerivativeGenerationRecord.generatedAt] descending, tie-broken by
     * [DerivativeGenerationId.value] descending -- never an inferred "current" or "authoritative"
     * generation; every admitted generation remains independently present in the result.
     */
    override suspend fun findOcrGenerationsForEvidence(evidenceArtifactId: EvidenceArtifactId): List<DerivativeGenerationRecord> {
        val matches = mutex.withLock {
            Files.list(storageRoot).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".derivative") }
                    .toList()
            }
        }.mapNotNull { path ->
            try {
                val size = Files.size(path)
                if (size > MAX_RECORD_BYTES) return@mapNotNull null
                DerivativeGenerationRecordCodec.decode(Files.readAllBytes(path))
            } catch (_: Exception) {
                null
            }
        }
        return matches
            .filter { it.rootSourceEvidenceArtifactId == evidenceArtifactId && DerivativeTransformation.OCR in it.transformationHistory }
            .sortedWith(compareByDescending<DerivativeGenerationRecord> { it.generatedAt }.thenByDescending { it.derivativeGenerationId.value })
    }

    private fun target(id: DerivativeGenerationId): Path = storageRoot.resolve("${id.value}.derivative")
    private fun preparedTarget(id: DerivativeGenerationId): Path = preparedDirectory.resolve("${id.value}.derivative")

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
