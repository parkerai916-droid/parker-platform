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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.CaseEvidenceAssociation
import parker.core.interfaces.CaseEvidenceAssociationCreationOutcome
import parker.core.interfaces.CaseEvidenceAssociationMigrationFailure
import parker.core.interfaces.CaseEvidenceAssociationMigrationReadiness
import parker.core.interfaces.CaseEvidenceAssociationMigrationState
import parker.core.interfaces.CaseEvidenceAssociationMigrationStateStorage
import parker.core.interfaces.CaseEvidenceAssociationMigrationStatus
import parker.core.interfaces.CaseEvidenceAssociationMigrationReadinessProvider
import parker.core.interfaces.CaseEvidenceAssociationStorage
import parker.core.interfaces.CaseGovernanceAudit
import parker.core.interfaces.CaseGovernanceAuditEventType
import parker.core.interfaces.CaseGovernanceAuditQuery
import parker.core.interfaces.CaseGovernanceAuditReader
import parker.core.interfaces.CaseGovernanceAuditRecord
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseStorage
import parker.core.interfaces.CurrentCaseAssignmentSource
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceSourceManifestStorage
import parker.core.interfaces.EvidenceArtifactIdentifierSafety
import parker.core.interfaces.LegacyAssignmentSnapshot
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.CaseAssignmentStorage
import parker.core.interfaces.CaseIdentifierSafety

private const val MIGRATION_SCHEMA_VERSION = 1

/** Durable versioned state for the one-time legacy assignment migration. */
class FileSystemCaseEvidenceAssociationMigrationStateStorage(root: Path) : CaseEvidenceAssociationMigrationStateStorage {
    private val root = root.toAbsolutePath().normalize()
    private val temporaryDirectory: Path
    private val stateFile: Path
    private val lockFile: Path
    private val mutex = Mutex()

    init {
        if (!Files.exists(root) || !Files.isDirectory(root) || !Files.isWritable(root)) {
            throw CaseEvidenceAssociationMigrationStorageException.InvalidStorageRoot(root.toString(), "root must be an existing writable directory")
        }
        temporaryDirectory = root.resolve(".tmp")
        stateFile = root.resolve("migration-state-v1.bin")
        lockFile = root.resolve("migration.lock")
        try { Files.createDirectories(temporaryDirectory) } catch (e: IOException) {
            throw CaseEvidenceAssociationMigrationStorageException.InvalidStorageRoot(root.toString(), "could not create temporary directory", e)
        }
    }

    override suspend fun read(): CaseEvidenceAssociationMigrationState = mutex.withLock {
        if (!Files.exists(stateFile)) return@withLock emptyState()
        val bytes = try {
            if (Files.size(stateFile) > MAX_STATE_BYTES) throw CorruptMigrationState("state exceeds size limit")
            Files.readAllBytes(stateFile)
        } catch (e: CaseEvidenceAssociationMigrationStorageException) { throw e }
        catch (e: IOException) { throw CaseEvidenceAssociationMigrationStorageException.StorageIOFailure("failed to read migration state", e) }
        try { MigrationStateCodec.decode(bytes).also(::validateState) } catch (e: Exception) {
            throw CorruptMigrationState(e.message ?: "state could not be decoded", e)
        }
    }

    override suspend fun write(state: CaseEvidenceAssociationMigrationState) {
        mutex.withLock {
            require(state.schemaVersion == MIGRATION_SCHEMA_VERSION) { "unsupported migration schema version" }
            validateState(state)
            val bytes = MigrationStateCodec.encode(state)
            val temporary = try { Files.createTempFile(temporaryDirectory, "migration-state-", ".tmp") }
            catch (e: IOException) { throw CaseEvidenceAssociationMigrationStorageException.PersistenceFailure("failed to create migration state temporary file", e) }
            try {
                FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                    val buffer = ByteBuffer.wrap(bytes)
                    while (buffer.hasRemaining()) channel.write(buffer)
                    channel.force(true)
                }
                Files.move(temporary, stateFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: IOException) {
                throw CaseEvidenceAssociationMigrationStorageException.PersistenceFailure("failed to persist migration state", e)
            } finally { Files.deleteIfExists(temporary) }
        }
    }

    suspend fun <T> withExclusive(action: suspend () -> T): T = PROCESS_MUTEX.withLock {
        try {
            FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                channel.lock().use { action() }
            }
        } catch (e: CaseEvidenceAssociationMigrationStorageException) { throw e }
        catch (e: IOException) {
            throw CaseEvidenceAssociationMigrationStorageException.PersistenceFailure("failed to acquire migration lock", e)
        }
    }

    private fun emptyState() = CaseEvidenceAssociationMigrationState(
        schemaVersion = MIGRATION_SCHEMA_VERSION,
        status = CaseEvidenceAssociationMigrationStatus.NOT_STARTED,
        startedAt = null,
        completedAt = null,
        processedAssignments = emptyList(),
        failures = emptyList(),
    )

    private fun validateState(state: CaseEvidenceAssociationMigrationState) {
        require(state.schemaVersion == MIGRATION_SCHEMA_VERSION) { "unsupported migration schema version" }
        require(state.processedAssignments.map { it.evidenceArtifactId }.distinct().size == state.processedAssignments.size) {
            "migration state contains duplicate processed assignment identities"
        }
        state.processedAssignments.forEach {
            EvidenceArtifactIdentifierSafety.requireSafe(it.evidenceArtifactId)
            it.caseId?.let(CaseIdentifierSafety::requireSafe)
        }
        state.failures.forEach { it.evidenceArtifactId?.let(EvidenceArtifactIdentifierSafety::requireSafe) }
        when (state.status) {
            CaseEvidenceAssociationMigrationStatus.NOT_STARTED -> require(state.startedAt == null && state.completedAt == null && state.processedAssignments.isEmpty() && state.failures.isEmpty()) { "invalid NOT_STARTED migration state" }
            CaseEvidenceAssociationMigrationStatus.RUNNING -> require(state.startedAt != null && state.completedAt == null) { "invalid RUNNING migration state" }
            CaseEvidenceAssociationMigrationStatus.COMPLETE -> require(state.startedAt != null && state.completedAt != null && state.failures.isEmpty()) { "invalid COMPLETE migration state" }
            CaseEvidenceAssociationMigrationStatus.INCOMPLETE -> require(state.startedAt != null && state.completedAt == null) { "invalid INCOMPLETE migration state" }
        }
    }

    private companion object {
        const val MAX_STATE_BYTES = 16L * 1024L * 1024L
        val PROCESS_MUTEX = Mutex()
    }
}

/** Migrates only the current legacy assignment pointer for each durable assignment file. */
class CaseEvidenceAssociationMigrationRunner(
    private val assignmentStorage: CaseAssignmentStorage,
    private val assignmentSource: CurrentCaseAssignmentSource,
    private val caseStorage: CaseStorage,
    private val manifestStorage: EvidenceSourceManifestStorage,
    private val associationStorage: CaseEvidenceAssociationStorage,
    private val stateStorage: FileSystemCaseEvidenceAssociationMigrationStateStorage,
    private val audit: CaseGovernanceAudit,
    private val auditReader: CaseGovernanceAuditReader,
    private val systemPrincipalId: PrincipalId = PrincipalId("case-association-migration"),
    private val clock: () -> Instant = Instant::now,
) : CaseEvidenceAssociationMigrationReadinessProvider {
    suspend fun run(): CaseEvidenceAssociationMigrationState = stateStorage.withExclusive {
        val previous = stateStorage.read()
        val current = loadCurrentAssignments()
        if (previous.status == CaseEvidenceAssociationMigrationStatus.COMPLETE) {
            if (current.failures.isNotEmpty() || current.snapshots != previous.processedAssignments) {
                return@withExclusive invalidate(previous, "LEGACY_ASSIGNMENT_DRIFT")
            }
            return@withExclusive previous
        }

        val startedAt = previous.startedAt ?: clock()
        var state = CaseEvidenceAssociationMigrationState(
            schemaVersion = MIGRATION_SCHEMA_VERSION,
            status = CaseEvidenceAssociationMigrationStatus.RUNNING,
            startedAt = startedAt,
            completedAt = null,
            processedAssignments = emptyList(),
            failures = emptyList(),
        )
        stateStorage.write(state)

        val processed = linkedMapOf<EvidenceArtifactId, LegacyAssignmentSnapshot>()
        val failures = mutableListOf<CaseEvidenceAssociationMigrationFailure>()
        for (evidenceArtifactId in current.ids) {
            val assignment = try { assignmentStorage.readAssignment(evidenceArtifactId) }
            catch (e: Exception) {
                failures += failure(evidenceArtifactId, "LEGACY_ASSIGNMENT_UNREADABLE: ${safeMessage(e)}")
                checkpoint(startedAt, processed, failures).also { state = it }
                stateStorage.write(state)
                continue
            }
            if (assignment == null) {
                failures += failure(evidenceArtifactId, "LEGACY_ASSIGNMENT_DISAPPEARED")
                checkpoint(startedAt, processed, failures).also { state = it }
                stateStorage.write(state)
                continue
            }
            val snapshot = LegacyAssignmentSnapshot(assignment.evidenceArtifactId, assignment.caseId, assignment.assignedAt)
            try {
                if (manifestStorage.read(evidenceArtifactId) == null) {
                    throw IllegalStateException("evidence artifact does not exist")
                }
                val caseId = assignment.caseId
                if (caseId != null) {
                    if (caseStorage.read(caseId) == null) throw IllegalStateException("case does not exist")
                    val result = associationStorage.createOrGet(caseId, evidenceArtifactId, assignment.assignedAt)
                    val association = when (result) {
                        is CaseEvidenceAssociationCreationOutcome.Created -> result.association
                        is CaseEvidenceAssociationCreationOutcome.AlreadyPresent -> result.association
                    }
                    ensureMigrationAudit(association)
                }
                processed[evidenceArtifactId] = snapshot
                failures.removeAll { it.evidenceArtifactId == evidenceArtifactId }
            } catch (e: Exception) {
                failures += failure(evidenceArtifactId, "MIGRATION_VALIDATION_FAILED: ${safeMessage(e)}")
            }
            state = checkpoint(startedAt, processed, failures)
            stateStorage.write(state)
        }

        val finalCurrent = loadCurrentAssignments()
        if (finalCurrent.failures.isNotEmpty() || finalCurrent.snapshots != processed.values.toList()) {
            failures += failure(null, "LEGACY_ASSIGNMENT_DRIFT_DURING_MIGRATION")
        }
        val finalState = if (failures.isEmpty()) {
            CaseEvidenceAssociationMigrationState(MIGRATION_SCHEMA_VERSION, CaseEvidenceAssociationMigrationStatus.COMPLETE, startedAt, clock(), processed.values.toList(), emptyList())
        } else {
            CaseEvidenceAssociationMigrationState(MIGRATION_SCHEMA_VERSION, CaseEvidenceAssociationMigrationStatus.INCOMPLETE, startedAt, null, processed.values.toList(), failures.distinct())
        }
        stateStorage.write(finalState)
        finalState
    }

    override suspend fun readiness(): CaseEvidenceAssociationMigrationReadiness = stateStorage.withExclusive {
        val state = stateStorage.read()
        if (state.status != CaseEvidenceAssociationMigrationStatus.COMPLETE) {
            return@withExclusive CaseEvidenceAssociationMigrationReadiness(state.status, false, state.failures.map { it.reason }.ifEmpty { listOf("MIGRATION_NOT_COMPLETE") })
        }
        val current = loadCurrentAssignments()
        if (current.failures.isNotEmpty() || current.snapshots != state.processedAssignments) {
            val invalidated = invalidate(state, "LEGACY_ASSIGNMENT_DRIFT")
            return@withExclusive CaseEvidenceAssociationMigrationReadiness(invalidated.status, false, invalidated.failures.map { it.reason })
        }
        CaseEvidenceAssociationMigrationReadiness(state.status, true, emptyList())
    }

    private suspend fun ensureMigrationAudit(association: CaseEvidenceAssociation) {
        val query = CaseGovernanceAuditQuery(
            eventType = CaseGovernanceAuditEventType.CASE_EVIDENCE_ASSOCIATION_MIGRATED,
            caseId = association.caseId,
            evidenceArtifactId = association.evidenceArtifactId,
            caseEvidenceAssociationId = association.associationId,
        )
        if (!auditReader.has(query)) {
            audit.record(
                CaseGovernanceAuditRecord(
                    eventType = CaseGovernanceAuditEventType.CASE_EVIDENCE_ASSOCIATION_MIGRATED,
                    caseId = association.caseId,
                    evidenceArtifactId = association.evidenceArtifactId,
                    actorPrincipalId = systemPrincipalId,
                    recordedAt = clock(),
                    caseEvidenceAssociationId = association.associationId,
                ),
            )
        }
    }

    private suspend fun loadCurrentAssignments(): CurrentAssignments {
        val ids = try { assignmentSource.listCurrentAssignmentIds() }
        catch (e: Exception) { return CurrentAssignments(emptyList(), emptyList(), listOf(failure(null, "ASSIGNMENT_DISCOVERY_FAILED: ${safeMessage(e)}"))) }
        val snapshots = mutableListOf<LegacyAssignmentSnapshot>()
        val failures = mutableListOf<CaseEvidenceAssociationMigrationFailure>()
        for (id in ids.sortedBy { it.value }) {
            try {
                val assignment = assignmentStorage.readAssignment(id)
                    ?: throw IllegalStateException("assignment record disappeared")
                snapshots += LegacyAssignmentSnapshot(assignment.evidenceArtifactId, assignment.caseId, assignment.assignedAt)
            } catch (e: Exception) {
                failures += failure(id, "LEGACY_ASSIGNMENT_UNREADABLE: ${safeMessage(e)}")
            }
        }
        return CurrentAssignments(ids.sortedBy { it.value }, snapshots, failures)
    }

    private fun checkpoint(
        startedAt: Instant,
        processed: Map<EvidenceArtifactId, LegacyAssignmentSnapshot>,
        failures: List<CaseEvidenceAssociationMigrationFailure>,
    ) = CaseEvidenceAssociationMigrationState(
        MIGRATION_SCHEMA_VERSION,
        CaseEvidenceAssociationMigrationStatus.RUNNING,
        startedAt,
        null,
        processed.values.toList(),
        failures.distinct(),
    )

    private suspend fun invalidate(previous: CaseEvidenceAssociationMigrationState, reason: String): CaseEvidenceAssociationMigrationState {
        val state = previous.copy(status = CaseEvidenceAssociationMigrationStatus.INCOMPLETE, completedAt = null, failures = listOf(failure(null, reason)))
        stateStorage.write(state)
        return state
    }

    private fun failure(id: EvidenceArtifactId?, reason: String) = CaseEvidenceAssociationMigrationFailure(id, reason)
    private fun safeMessage(e: Exception) = e.message?.take(500) ?: e::class.simpleName.orEmpty()

    private data class CurrentAssignments(
        val ids: List<EvidenceArtifactId>,
        val snapshots: List<LegacyAssignmentSnapshot>,
        val failures: List<CaseEvidenceAssociationMigrationFailure>,
    )
}

/** Read-only readiness check for consumers that must not hold evidence-byte storage capability. */
class FileSystemCaseEvidenceAssociationMigrationReadinessProvider(
    private val assignmentStorage: CaseAssignmentStorage,
    private val assignmentSource: CurrentCaseAssignmentSource,
    private val stateStorage: CaseEvidenceAssociationMigrationStateStorage,
) : CaseEvidenceAssociationMigrationReadinessProvider {
    override suspend fun readiness(): CaseEvidenceAssociationMigrationReadiness {
        val state = stateStorage.read()
        if (state.status != CaseEvidenceAssociationMigrationStatus.COMPLETE) {
            return CaseEvidenceAssociationMigrationReadiness(
                state.status,
                false,
                state.failures.map { it.reason }.ifEmpty { listOf("MIGRATION_NOT_COMPLETE") },
            )
        }
        val snapshots = try {
            assignmentSource.listCurrentAssignmentIds().sortedBy { it.value }.map { id ->
                val assignment = assignmentStorage.readAssignment(id)
                    ?: return CaseEvidenceAssociationMigrationReadiness(
                        CaseEvidenceAssociationMigrationStatus.INCOMPLETE,
                        false,
                        listOf("LEGACY_ASSIGNMENT_DISAPPEARED:${id.value}"),
                    )
                LegacyAssignmentSnapshot(assignment.evidenceArtifactId, assignment.caseId, assignment.assignedAt)
            }
        } catch (_: Exception) {
            return CaseEvidenceAssociationMigrationReadiness(
                CaseEvidenceAssociationMigrationStatus.INCOMPLETE,
                false,
                listOf("LEGACY_ASSIGNMENT_UNREADABLE"),
            )
        }
        return if (snapshots == state.processedAssignments) {
            CaseEvidenceAssociationMigrationReadiness(CaseEvidenceAssociationMigrationStatus.COMPLETE, true, emptyList())
        } else {
            CaseEvidenceAssociationMigrationReadiness(
                CaseEvidenceAssociationMigrationStatus.INCOMPLETE,
                false,
                listOf("LEGACY_ASSIGNMENT_DRIFT"),
            )
        }
    }
}

sealed class CaseEvidenceAssociationMigrationStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class InvalidStorageRoot(path: String, reason: String, cause: Throwable? = null) : CaseEvidenceAssociationMigrationStorageException("migration state root '$path' is invalid: $reason", cause)
    class PersistenceFailure(message: String, cause: Throwable) : CaseEvidenceAssociationMigrationStorageException(message, cause)
    class StorageIOFailure(message: String, cause: Throwable) : CaseEvidenceAssociationMigrationStorageException(message, cause)
    class CorruptRecord(detail: String, cause: Throwable? = null) : CaseEvidenceAssociationMigrationStorageException("migration state is corrupt: $detail", cause)
}

private fun CorruptMigrationState(detail: String, cause: Throwable? = null): CaseEvidenceAssociationMigrationStorageException =
    CaseEvidenceAssociationMigrationStorageException.CorruptRecord(detail, cause)

private object MigrationStateCodec {
    private const val MAGIC = 0x43454D53 // CEMS
    private const val VERSION = 1
    private const val MAX_STRING_BYTES = 64 * 1024

    fun encode(state: CaseEvidenceAssociationMigrationState): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(MAGIC); out.writeInt(VERSION); out.writeInt(state.schemaVersion); out.writeUTF(state.status.name)
            out.writeNullableInstant(state.startedAt); out.writeNullableInstant(state.completedAt)
            out.writeInt(state.processedAssignments.size)
            state.processedAssignments.forEach { snapshot ->
                out.writeString(snapshot.evidenceArtifactId.value); out.writeNullableString(snapshot.caseId?.value); out.writeString(snapshot.assignedAt.toString())
            }
            out.writeInt(state.failures.size)
            state.failures.forEach { failure -> out.writeNullableString(failure.evidenceArtifactId?.value); out.writeString(failure.reason) }
        }; bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): CaseEvidenceAssociationMigrationState = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == MAGIC) { "invalid migration state magic" }
        require(input.readInt() == VERSION) { "unsupported migration state version" }
        val schema = input.readInt(); require(schema == MIGRATION_SCHEMA_VERSION) { "unsupported migration schema version" }
        val status = enumValueOf<CaseEvidenceAssociationMigrationStatus>(input.readUTF())
        val started = input.readNullableInstant(); val completed = input.readNullableInstant()
        val processedCount = input.readInt(); require(processedCount in 0..MAX_ITEMS) { "invalid processed assignment count" }
        val processed = (0 until processedCount).map {
            LegacyAssignmentSnapshot(EvidenceArtifactId(input.readString()), input.readNullableString()?.let(::CaseId), Instant.parse(input.readString()))
        }
        val failureCount = input.readInt(); require(failureCount in 0..MAX_ITEMS) { "invalid failure count" }
        val failures = (0 until failureCount).map { CaseEvidenceAssociationMigrationFailure(input.readNullableString()?.let(::EvidenceArtifactId), input.readString()) }
        require(input.available() == 0) { "unexpected trailing migration state bytes" }
        CaseEvidenceAssociationMigrationState(schema, status, started, completed, processed, failures)
    }

    private const val MAX_ITEMS = 1_000_000
}

private fun DataOutputStream.writeString(value: String) {
    val bytes = value.toByteArray(StandardCharsets.UTF_8); require(bytes.size <= 64 * 1024) { "string exceeds migration codec limit" }
    writeInt(bytes.size); write(bytes)
}
private fun DataOutputStream.writeNullableString(value: String?) { writeBoolean(value != null); value?.let(::writeString) }
private fun DataOutputStream.writeNullableInstant(value: Instant?) { writeNullableString(value?.toString()) }
private fun DataInputStream.readString(): String {
    val size = readInt(); require(size in 0..(64 * 1024)) { "invalid migration string length" }
    val bytes = ByteArray(size).also(::readFully)
    return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
}
private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null
private fun DataInputStream.readNullableInstant(): Instant? = readNullableString()?.let(Instant::parse)
