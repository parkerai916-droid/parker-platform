package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.CaseEvidenceAssociation
import parker.core.interfaces.CaseEvidenceAssociationCreationOutcome
import parker.core.interfaces.CaseEvidenceAssociationId
import parker.core.interfaces.CaseEvidenceAssociationStorage
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseIdentifierSafety
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceArtifactIdentifierSafety

/** Durable, write-once-by-key storage for multi-case evidence associations. */
class FileSystemCaseEvidenceAssociationStorage(storageRoot: Path) : CaseEvidenceAssociationStorage {
    private val storageRoot = storageRoot.toAbsolutePath().normalize()
    private val temporaryDirectory: Path
    private val mutex = Mutex()

    init {
        validateRoot(storageRoot)
        temporaryDirectory = this.storageRoot.resolve(TEMPORARY_DIRECTORY)
        try {
            Files.createDirectories(temporaryDirectory)
        } catch (e: IOException) {
            throw CaseEvidenceAssociationStorageException.InvalidStorageRoot(
                storageRoot.toString(), "could not create temporary directory: ${e.message}", e,
            )
        }
    }

    override suspend fun createOrGet(
        caseId: CaseId,
        evidenceArtifactId: EvidenceArtifactId,
        associatedAt: Instant,
    ): CaseEvidenceAssociationCreationOutcome {
        requireSafe(caseId, evidenceArtifactId)
        val association = CaseEvidenceAssociation(
            associationId = deterministicAssociationId(caseId, evidenceArtifactId),
            evidenceArtifactId = evidenceArtifactId,
            caseId = caseId,
            associatedAt = associatedAt,
        )
        return mutex.withLock {
            withCrossProcessLock(association.associationId) {
                val target = target(association.associationId)
                val existing = readIfPresent(target, association.associationId)
                if (existing != null) {
                    verifyTuple(existing, caseId, evidenceArtifactId)
                    CaseEvidenceAssociationCreationOutcome.AlreadyPresent(existing)
                } else {
                    if (persistIfAbsent(target, AssociationRecordCodec.encode(association))) {
                        CaseEvidenceAssociationCreationOutcome.Created(association)
                    } else {
                        val raced = readRequired(target, association.associationId)
                        verifyTuple(raced, caseId, evidenceArtifactId)
                        CaseEvidenceAssociationCreationOutcome.AlreadyPresent(raced)
                    }
                }
            }
        }
    }

    override suspend fun find(caseId: CaseId, evidenceArtifactId: EvidenceArtifactId): CaseEvidenceAssociation? {
        requireSafe(caseId, evidenceArtifactId)
        val id = deterministicAssociationId(caseId, evidenceArtifactId)
        return mutex.withLock { readIfPresent(target(id), id) }
    }

    override suspend fun listForCase(caseId: CaseId): List<CaseEvidenceAssociation> {
        CaseIdentifierSafety.requireSafe(caseId)
        return listAll().filter { it.caseId == caseId }.sortedBy { it.associationId.value }
    }

    override suspend fun listForEvidence(evidenceArtifactId: EvidenceArtifactId): List<CaseEvidenceAssociation> {
        EvidenceArtifactIdentifierSafety.requireSafe(evidenceArtifactId)
        return listAll().filter { it.evidenceArtifactId == evidenceArtifactId }.sortedBy { it.associationId.value }
    }

    private suspend fun listAll(): List<CaseEvidenceAssociation> = mutex.withLock {
        try {
            Files.list(storageRoot).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(FILE_SUFFIX) }
                    .sorted()
                    .map { path ->
                        val id = CaseEvidenceAssociationId(path.fileName.toString().removeSuffix(FILE_SUFFIX))
                        readRequired(path, id)
                    }.toList()
            }
        } catch (e: CaseEvidenceAssociationStorageException) {
            throw e
        } catch (e: Exception) {
            throw CaseEvidenceAssociationStorageException.StorageIOFailure("failed to list associations", e)
        }
    }

    private fun readIfPresent(path: Path, id: CaseEvidenceAssociationId): CaseEvidenceAssociation? {
        if (!Files.exists(path)) return null
        return readRequired(path, id)
    }

    private fun readRequired(path: Path, id: CaseEvidenceAssociationId): CaseEvidenceAssociation {
        val bytes = try {
            val size = Files.size(path)
            if (size > MAX_RECORD_BYTES) throw CorruptAssociation(id, "record exceeds size limit")
            Files.readAllBytes(path)
        } catch (e: CaseEvidenceAssociationStorageException) {
            throw e
        } catch (e: IOException) {
            throw CaseEvidenceAssociationStorageException.StorageIOFailure("failed to read association '${id.value}'", e)
        }
        val record = try { AssociationRecordCodec.decode(bytes) } catch (e: Exception) {
            throw CorruptAssociation(id, e.message ?: "record could not be decoded", e)
        }
        if (record.associationId != id || deterministicAssociationId(record.caseId, record.evidenceArtifactId) != id) {
            throw CorruptAssociation(id, "stored association identity does not match its canonical tuple")
        }
        return record
    }

    private fun persistIfAbsent(target: Path, bytes: ByteArray): Boolean {
        val temporary = try {
            Files.createTempFile(temporaryDirectory, "association-", ".tmp")
        } catch (e: IOException) {
            throw CaseEvidenceAssociationStorageException.PersistenceFailure("failed to create association temporary file", e)
        }
        try {
            FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                val buffer = ByteBuffer.wrap(bytes)
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }
            return try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
                true
            } catch (_: FileAlreadyExistsException) {
                false
            }
        } catch (e: CaseEvidenceAssociationStorageException) {
            throw e
        } catch (e: IOException) {
            throw CaseEvidenceAssociationStorageException.PersistenceFailure("failed to persist association", e)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun target(id: CaseEvidenceAssociationId): Path = storageRoot.resolve("${id.value}$FILE_SUFFIX")

    private suspend fun <T> withCrossProcessLock(id: CaseEvidenceAssociationId, action: () -> T): T =
        PROCESS_MUTEX.withLock {
            val lockPath = temporaryDirectory.resolve("${id.value}.lock")
            try {
                FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                    channel.lock().use { action() }
                }
            } catch (e: CaseEvidenceAssociationStorageException) {
                throw e
            } catch (e: IOException) {
                throw CaseEvidenceAssociationStorageException.PersistenceFailure(
                    "failed to acquire association lock for '${id.value}'", e,
                )
            }
        }

    private fun requireSafe(caseId: CaseId, evidenceArtifactId: EvidenceArtifactId) {
        try {
            CaseIdentifierSafety.requireSafe(caseId)
            EvidenceArtifactIdentifierSafety.requireSafe(evidenceArtifactId)
        } catch (e: RuntimeException) {
            throw CaseEvidenceAssociationStorageException.UnsafeIdentifier(caseId, evidenceArtifactId, e)
        }
    }

    private fun verifyTuple(record: CaseEvidenceAssociation, caseId: CaseId, evidenceArtifactId: EvidenceArtifactId) {
        if (record.caseId != caseId || record.evidenceArtifactId != evidenceArtifactId) {
            throw CorruptAssociation(record.associationId, "stored case/artifact tuple does not match requested tuple")
        }
    }

    private companion object {
        const val FILE_SUFFIX = ".association-v1"
        const val TEMPORARY_DIRECTORY = ".tmp"
        const val MAX_RECORD_BYTES = 64L * 1024L
        val PROCESS_MUTEX = Mutex()

        fun validateRoot(root: Path) {
            if (!Files.exists(root)) throw CaseEvidenceAssociationStorageException.InvalidStorageRoot(root.toString(), "does not exist")
            if (!Files.isDirectory(root)) throw CaseEvidenceAssociationStorageException.InvalidStorageRoot(root.toString(), "is not a directory")
            if (!Files.isWritable(root)) throw CaseEvidenceAssociationStorageException.InvalidStorageRoot(root.toString(), "is not writable")
        }
    }
}

/** Durable, write-once-by-occurrence-key storage for governed source occurrences. */
class FileSystemEvidenceOccurrenceStorage(storageRoot: Path) : parker.core.interfaces.EvidenceOccurrenceStorage {
    private val storageRoot = storageRoot.toAbsolutePath().normalize()
    private val temporaryDirectory: Path
    private val mutex = Mutex()

    init {
        if (!Files.exists(storageRoot) || !Files.isDirectory(storageRoot) || !Files.isWritable(storageRoot)) {
            throw EvidenceOccurrenceStorageException.InvalidStorageRoot(storageRoot.toString(), "root must be an existing writable directory")
        }
        temporaryDirectory = storageRoot.resolve(".tmp")
        try { Files.createDirectories(temporaryDirectory) } catch (e: IOException) {
            throw EvidenceOccurrenceStorageException.InvalidStorageRoot(storageRoot.toString(), "could not create temporary directory", e)
        }
    }

    override suspend fun createOrGet(occurrence: parker.core.interfaces.EvidenceOccurrence): parker.core.interfaces.EvidenceOccurrenceRegistrationOutcome {
        requireSafe(occurrence)
        val target = target(occurrence.occurrenceId)
        return mutex.withLock {
            withCrossProcessLock(occurrence.occurrenceId) {
                val existing = readIfPresent(target, occurrence.occurrenceId)
                if (existing != null) {
                    verifyIdentity(existing, occurrence)
                    parker.core.interfaces.EvidenceOccurrenceRegistrationOutcome.AlreadyPresent(existing)
                } else if (persistIfAbsent(target, OccurrenceRecordCodec.encode(occurrence))) {
                    parker.core.interfaces.EvidenceOccurrenceRegistrationOutcome.Created(occurrence)
                } else {
                    val raced = readRequired(target, occurrence.occurrenceId)
                    verifyIdentity(raced, occurrence)
                    parker.core.interfaces.EvidenceOccurrenceRegistrationOutcome.AlreadyPresent(raced)
                }
            }
        }
    }

    override suspend fun findByPrepOccurrence(prepJobId: String, prepOccurrenceId: String): parker.core.interfaces.EvidenceOccurrence? {
        requirePrepKey(prepJobId, prepOccurrenceId)
        val id = deterministicPrepOccurrenceId(prepJobId, prepOccurrenceId)
        return mutex.withLock { readIfPresent(target(id), id) }
    }

    override suspend fun listForAssociation(associationId: parker.core.interfaces.CaseEvidenceAssociationId): List<parker.core.interfaces.EvidenceOccurrence> {
        requireSafeId(associationId.value)
        return mutex.withLock {
            try {
                Files.list(storageRoot).use { paths ->
                    paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(FILE_SUFFIX) }
                        .sorted()
                        .map { path ->
                            val id = parker.core.interfaces.EvidenceOccurrenceId(path.fileName.toString().removeSuffix(FILE_SUFFIX))
                            readRequired(path, id)
                        }.toList()
                }
            } catch (e: EvidenceOccurrenceStorageException) { throw e }
            catch (e: Exception) { throw EvidenceOccurrenceStorageException.StorageIOFailure("failed to list occurrences", e) }
        }.filter { it.associationId == associationId }.sortedBy { it.occurrenceId.value }
    }

    private fun readIfPresent(path: Path, id: parker.core.interfaces.EvidenceOccurrenceId): parker.core.interfaces.EvidenceOccurrence? =
        if (Files.exists(path)) readRequired(path, id) else null

    private fun readRequired(path: Path, id: parker.core.interfaces.EvidenceOccurrenceId): parker.core.interfaces.EvidenceOccurrence {
        val bytes = try {
            val size = Files.size(path)
            if (size > MAX_RECORD_BYTES) throw CorruptOccurrence(id, "record exceeds size limit")
            Files.readAllBytes(path)
        } catch (e: EvidenceOccurrenceStorageException) { throw e }
        catch (e: IOException) { throw EvidenceOccurrenceStorageException.StorageIOFailure("failed to read occurrence '${id.value}'", e) }
        val record = try { OccurrenceRecordCodec.decode(bytes) } catch (e: Exception) {
            throw CorruptOccurrence(id, e.message ?: "record could not be decoded", e)
        }
        if (record.occurrenceId != id) throw CorruptOccurrence(id, "stored occurrence identity does not match filename")
        validateStoredIdentity(record, id)
        return record
    }

    private fun persistIfAbsent(target: Path, bytes: ByteArray): Boolean {
        val temporary = try { Files.createTempFile(temporaryDirectory, "occurrence-", ".tmp") }
        catch (e: IOException) { throw EvidenceOccurrenceStorageException.PersistenceFailure("failed to create occurrence temporary file", e) }
        try {
            FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                val buffer = ByteBuffer.wrap(bytes)
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }
            return try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE); true }
            catch (_: FileAlreadyExistsException) { false }
        } catch (e: EvidenceOccurrenceStorageException) { throw e }
        catch (e: IOException) { throw EvidenceOccurrenceStorageException.PersistenceFailure("failed to persist occurrence", e) }
        finally { Files.deleteIfExists(temporary) }
    }

    private fun target(id: parker.core.interfaces.EvidenceOccurrenceId): Path {
        requireSafeId(id.value)
        return storageRoot.resolve("${id.value}$FILE_SUFFIX")
    }

    private suspend fun <T> withCrossProcessLock(id: parker.core.interfaces.EvidenceOccurrenceId, action: () -> T): T =
        PROCESS_MUTEX.withLock {
            val lockPath = temporaryDirectory.resolve("${id.value}.lock")
            try {
                FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                    channel.lock().use { action() }
                }
            } catch (e: EvidenceOccurrenceStorageException) {
                throw e
            } catch (e: IOException) {
                throw EvidenceOccurrenceStorageException.PersistenceFailure(
                    "failed to acquire occurrence lock for '${id.value}'", e,
                )
            }
        }

    private fun requireSafe(occurrence: parker.core.interfaces.EvidenceOccurrence) {
        requireSafeId(occurrence.occurrenceId.value)
        try {
            parker.core.interfaces.CaseIdentifierSafety.requireSafe(occurrence.caseId)
            parker.core.interfaces.EvidenceArtifactIdentifierSafety.requireSafe(occurrence.evidenceArtifactId)
        } catch (e: RuntimeException) {
            throw EvidenceOccurrenceStorageException.UnsafeIdentifier(occurrence.occurrenceId, e)
        }
        if (occurrence.prepJobId != null) {
            val expected = deterministicPrepOccurrenceId(occurrence.prepJobId, occurrence.prepOccurrenceId!!)
            if (expected != occurrence.occurrenceId) throw EvidenceOccurrenceStorageException.CorruptRecord(occurrence.occurrenceId, "prep occurrence ID is not deterministic for its prep key")
        }
    }

    private fun validateStoredIdentity(record: parker.core.interfaces.EvidenceOccurrence, id: parker.core.interfaces.EvidenceOccurrenceId) {
        requireSafe(record)
        if (record.occurrenceId != id) throw EvidenceOccurrenceStorageException.CorruptRecord(id, "stored identity mismatch")
    }

    private fun verifyIdentity(existing: parker.core.interfaces.EvidenceOccurrence, requested: parker.core.interfaces.EvidenceOccurrence) {
        // createdAt is recording metadata, not part of the prep idempotency key.
        // All source/content/provenance fields must nevertheless remain stable.
        if (existing != requested.copy(createdAt = existing.createdAt)) {
            throw EvidenceOccurrenceStorageException.CorruptRecord(requested.occurrenceId, "existing occurrence identity contains different provenance or content")
        }
    }

    private companion object {
        const val FILE_SUFFIX = ".occurrence-v1"
        const val MAX_RECORD_BYTES = 256L * 1024L
        val SAFE_IDENTIFIER = Regex("^[a-z0-9_-]+$")
        val PROCESS_MUTEX = Mutex()

        fun requireSafeId(value: String) {
            if (!SAFE_IDENTIFIER.matches(value)) throw IllegalArgumentException("unsafe occurrence identifier")
        }

        fun requirePrepKey(job: String, occurrence: String) {
            require(job.isNotBlank() && occurrence.isNotBlank()) { "prep occurrence key must not be blank" }
        }
    }
}

sealed class CaseEvidenceAssociationStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class InvalidStorageRoot(path: String, reason: String, cause: Throwable? = null) : CaseEvidenceAssociationStorageException("association storage root '$path' is invalid: $reason", cause)
    class UnsafeIdentifier(caseId: CaseId, evidenceArtifactId: EvidenceArtifactId, cause: Throwable? = null) : CaseEvidenceAssociationStorageException("unsafe association identifiers '${caseId.value}'/'${evidenceArtifactId.value}'", cause)
    class PersistenceFailure(message: String, cause: Throwable) : CaseEvidenceAssociationStorageException(message, cause)
    class StorageIOFailure(message: String, cause: Throwable) : CaseEvidenceAssociationStorageException(message, cause)
    class CorruptRecord(id: CaseEvidenceAssociationId, detail: String, cause: Throwable? = null) : CaseEvidenceAssociationStorageException("association '${id.value}' is corrupt: $detail", cause)
}

sealed class EvidenceOccurrenceStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class InvalidStorageRoot(path: String, reason: String, cause: Throwable? = null) : EvidenceOccurrenceStorageException("occurrence storage root '$path' is invalid: $reason", cause)
    class UnsafeIdentifier(id: parker.core.interfaces.EvidenceOccurrenceId, cause: Throwable? = null) : EvidenceOccurrenceStorageException("unsafe occurrence identifier '${id.value}'", cause)
    class PersistenceFailure(message: String, cause: Throwable) : EvidenceOccurrenceStorageException(message, cause)
    class StorageIOFailure(message: String, cause: Throwable) : EvidenceOccurrenceStorageException(message, cause)
    class CorruptRecord(id: parker.core.interfaces.EvidenceOccurrenceId, detail: String, cause: Throwable? = null) : EvidenceOccurrenceStorageException("occurrence '${id.value}' is corrupt: $detail", cause)
}

internal fun deterministicAssociationId(caseId: CaseId, evidenceArtifactId: EvidenceArtifactId): CaseEvidenceAssociationId =
    CaseEvidenceAssociationId("case-evidence-association-" + sha256Canonical("case-evidence-association-v1", caseId.value, evidenceArtifactId.value))

internal fun deterministicPrepOccurrenceId(prepJobId: String, prepOccurrenceId: String): parker.core.interfaces.EvidenceOccurrenceId =
    parker.core.interfaces.EvidenceOccurrenceId("evidence-occurrence-" + sha256Canonical("evidence-occurrence-prep-v1", prepJobId, prepOccurrenceId))

private fun sha256Canonical(schema: String, vararg fields: String): String {
    val bytes = ByteArrayOutputStream().use { output ->
        DataOutputStream(output).use { data ->
            data.writeUTF(schema)
            fields.forEach { field ->
                val encoded = field.toByteArray(StandardCharsets.UTF_8)
                data.writeInt(encoded.size)
                data.write(encoded)
            }
        }
        output.toByteArray()
    }
    return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

private object AssociationRecordCodec {
    private const val MAGIC = 0x43454153 // CEAS
    private const val VERSION = 1
    fun encode(record: CaseEvidenceAssociation): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(MAGIC); out.writeInt(VERSION)
            out.writeString(record.associationId.value); out.writeString(record.evidenceArtifactId.value)
            out.writeString(record.caseId.value); out.writeString(record.associatedAt.toString())
        }; bytes.toByteArray()
    }
    fun decode(bytes: ByteArray): CaseEvidenceAssociation = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == MAGIC) { "invalid association record magic" }
        require(input.readInt() == VERSION) { "unsupported association record version" }
        val id = CaseEvidenceAssociationId(input.readString()); val artifact = EvidenceArtifactId(input.readString())
        val caseId = CaseId(input.readString()); val at = Instant.parse(input.readString())
        require(input.available() == 0) { "unexpected trailing bytes" }
        CaseEvidenceAssociation(id, artifact, caseId, at)
    }
}

private object OccurrenceRecordCodec {
    private const val MAGIC = 0x454F4343 // EOCC
    private const val VERSION = 1
    fun encode(record: parker.core.interfaces.EvidenceOccurrence): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(MAGIC); out.writeInt(VERSION)
            out.writeString(record.occurrenceId.value); out.writeString(record.associationId.value)
            out.writeString(record.evidenceArtifactId.value); out.writeString(record.caseId.value)
            out.writeString(record.sourceSha256); out.writeNullableString(record.prepJobId)
            out.writeNullableString(record.prepOccurrenceId); out.writeNullableString(record.relativePath)
            out.writeNullableString(record.archiveParentOccurrenceId); out.writeNullableString(record.archiveMemberPath)
            out.writeString(record.createdAt.toString())
        }; bytes.toByteArray()
    }
    fun decode(bytes: ByteArray): parker.core.interfaces.EvidenceOccurrence = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == MAGIC) { "invalid occurrence record magic" }
        require(input.readInt() == VERSION) { "unsupported occurrence record version" }
        val id = parker.core.interfaces.EvidenceOccurrenceId(input.readString())
        val association = CaseEvidenceAssociationId(input.readString()); val artifact = EvidenceArtifactId(input.readString())
        val caseId = CaseId(input.readString()); val sha = input.readString()
        val prepJob = input.readNullableString(); val prepOccurrence = input.readNullableString(); val relative = input.readNullableString()
        val parent = input.readNullableString()
        val member = input.readNullableString(); val created = Instant.parse(input.readString())
        require(input.available() == 0) { "unexpected trailing bytes" }
        return parker.core.interfaces.EvidenceOccurrence(id, association, artifact, caseId, sha, prepJob, prepOccurrence, relative, parent, member, created)
    }
}

private fun DataOutputStream.writeString(value: String) {
    val encoded = value.toByteArray(StandardCharsets.UTF_8)
    require(encoded.size <= 64 * 1024) { "string exceeds codec limit" }
    writeInt(encoded.size); write(encoded)
}

private fun DataOutputStream.writeNullableString(value: String?) { writeBoolean(value != null); value?.let(::writeString) }

private fun DataInputStream.readString(): String {
    val size = readInt(); require(size in 0..(64 * 1024)) { "invalid string length" }
    val encoded = ByteArray(size).also(::readFully)
    return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(encoded)).toString()
}

private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null

private fun CorruptAssociation(id: CaseEvidenceAssociationId, detail: String, cause: Throwable? = null): CaseEvidenceAssociationStorageException =
    CaseEvidenceAssociationStorageException.CorruptRecord(id, detail, cause)

private fun CorruptOccurrence(id: parker.core.interfaces.EvidenceOccurrenceId, detail: String, cause: Throwable? = null): EvidenceOccurrenceStorageException =
    EvidenceOccurrenceStorageException.CorruptRecord(id, detail, cause)
