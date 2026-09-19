package parker.core.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.CaseGovernanceAudit
import parker.core.interfaces.CaseGovernanceAuditEventType
import parker.core.interfaces.CaseGovernanceAuditRecord
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseStorage
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceOccurrence
import parker.core.interfaces.EvidenceOccurrenceId
import parker.core.interfaces.PrincipalId

/** One Owner-authorised, immutable batch-to-case binding and its submitted opaque artifact ids. */
internal data class BulkIngestionBinding(
    val batchId: String,
    val caseId: CaseId,
    val evidence: Set<EvidenceArtifactId>,
    val externalTranscriptionAuthorised: Boolean = false,
)

internal sealed interface BulkIngestionAuthorisation {
    data class Authorised(val binding: BulkIngestionBinding) : BulkIngestionAuthorisation
    data object UnknownCase : BulkIngestionAuthorisation
    data class Failure(val reason: String) : BulkIngestionAuthorisation
}

internal sealed interface BulkIngestionAssignment {
    data class Assigned(val caseId: CaseId) : BulkIngestionAssignment
    data object UnknownBatch : BulkIngestionAssignment
    data object EvidenceNotSubmittedUnderBatch : BulkIngestionAssignment
    data object UnknownCase : BulkIngestionAssignment
    data object UnknownEvidence : BulkIngestionAssignment
    data class MigrationNotReady(val reasons: List<String>) : BulkIngestionAssignment
    data class Rejected(val reason: String) : BulkIngestionAssignment
    data class Failure(val reason: String) : BulkIngestionAssignment
}

/** Read-only handoff projection; deliberately omits CaseId. */
data class ReadyBulkIngestionBatch(val batchId: String, val caseName: String)

/**
 * The only subordinate case-binding seam. The case id is written once by an Owner call and is
 * never accepted by the Hermes-facing methods. Evidence membership is durable and is recorded
 * only after Parker's Evidence Custodian returns the submitted artifact identity.
 */
internal class BulkIngestionBindingCoordinator(
    private val storageRoot: Path,
    private val caseStorage: CaseStorage,
    private val caseEvidenceAssociationCoordinator: CaseEvidenceAssociationCoordinator,
    private val audit: CaseGovernanceAudit,
    private val ownerPrincipalId: PrincipalId,
    private val clock: () -> java.time.Instant = java.time.Instant::now,
) {
    private val mutex = Mutex()

    init {
        Files.createDirectories(storageRoot)
        require(Files.isDirectory(storageRoot) && Files.isWritable(storageRoot)) { "bulk-ingestion binding storage is unavailable" }
    }

    suspend fun authoriseAsOwner(caseId: CaseId): BulkIngestionAuthorisation = mutex.withLock {
        if (caseStorage.read(caseId) == null) return@withLock BulkIngestionAuthorisation.UnknownCase
        val batchId = "bulk-${UUID.randomUUID()}"
        val binding = BulkIngestionBinding(batchId, caseId, emptySet(), false)
        return@withLock try {
            write(binding)
            audit.record(CaseGovernanceAuditRecord(CaseGovernanceAuditEventType.INGESTION_BATCH_AUTHORISED, caseId, actorPrincipalId = ownerPrincipalId, recordedAt = clock()))
            BulkIngestionAuthorisation.Authorised(binding)
        } catch (e: Exception) {
            BulkIngestionAuthorisation.Failure(e.message ?: "batch authorisation failed")
        }
    }

    suspend fun recordSubmission(batchId: String, evidenceArtifactId: EvidenceArtifactId): Boolean = mutex.withLock {
        val existing = read(batchId) ?: return@withLock false
        if (evidenceArtifactId in existing.evidence) return@withLock true
        write(existing.copy(evidence = existing.evidence + evidenceArtifactId))
        true
    }

    suspend fun isAuthorised(batchId: String): Boolean = mutex.withLock { read(batchId) != null }

    /** Exact read-only membership check used by source-bound derivative intake. */
    suspend fun containsEvidence(batchId: String, evidenceArtifactId: EvidenceArtifactId): Boolean = mutex.withLock {
        evidenceArtifactId in (read(batchId)?.evidence ?: emptySet())
    }

    suspend fun externalTranscriptionIsAuthorised(batchId: String): Boolean = mutex.withLock {
        // Retained only as a compatibility query; batch metadata is never policy authority.
        false
    }

    /** Proves the exact authorised batch/evidence pair before deriving the existing grant. */
    suspend fun deriveExternalTranscriptionAuthorization(
        batchId: String,
        evidenceArtifactId: EvidenceArtifactId,
        derive: suspend (
            suspend (ExternalTranscriptionOwnerAuthorization) -> Unit,
            suspend (ExternalTranscriptionOwnerAuthorization) -> Unit,
        ) -> Boolean,
    ): Boolean = mutex.withLock {
        val binding = read(batchId) ?: return@withLock false
        if (evidenceArtifactId !in binding.evidence) return@withLock false
        derive({ _ ->
            audit.record(CaseGovernanceAuditRecord(
                CaseGovernanceAuditEventType.INGESTION_BATCH_EXTERNAL_TRANSCRIPTION_DERIVATION_PREPARED,
                binding.caseId,
                evidenceArtifactId = evidenceArtifactId,
                actorPrincipalId = ownerPrincipalId,
                recordedAt = clock(),
                batchId = batchId,
                externalTranscriptionAuthorised = true,
                authorizationPurpose = ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE.value,
                policyVersion = STANDING_EXTERNAL_TRANSCRIPTION_POLICY_VERSION,
            ))
        }, { _ ->
            audit.record(CaseGovernanceAuditRecord(
                CaseGovernanceAuditEventType.INGESTION_BATCH_EXTERNAL_TRANSCRIPTION_DERIVED,
                binding.caseId,
                evidenceArtifactId = evidenceArtifactId,
                actorPrincipalId = ownerPrincipalId,
                recordedAt = clock(),
                batchId = batchId,
                externalTranscriptionAuthorised = true,
                authorizationPurpose = ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE.value,
                policyVersion = STANDING_EXTERNAL_TRANSCRIPTION_POLICY_VERSION,
            ))
        })
    }

    /**
     * Hermes Exception Decision Backend, Task 4. Read-only case display name for one exact batch,
     * for Owner review usability only -- mirrors [listReady]'s own identical
     * `read(batchId)`/`caseStorage.read` lookup, never a new case-resolution mechanism. `null` for
     * an unknown batch or a batch whose bound case no longer exists. Never exposed to Hermes.
     */
    suspend fun caseNameForBatch(batchId: String): String? = mutex.withLock {
        val binding = runCatching { read(batchId) }.getOrNull() ?: return@withLock null
        caseStorage.read(binding.caseId)?.caseName
    }

    /** Read-only exact-evidence lookup for downstream projections. */
    suspend fun batchesForEvidence(evidenceArtifactId: EvidenceArtifactId): List<String> = mutex.withLock {
        Files.list(storageRoot).use { paths ->
            paths.filter { it.fileName.toString().endsWith(".binding") }
                .map { it.fileName.toString().removeSuffix(".binding") }
                .sorted()
                .filter { batchId -> runCatching { evidenceArtifactId in (read(batchId)?.evidence ?: emptySet()) }.getOrDefault(false) }
                .toList()
        }
    }

    /** Bounded read-only projection for Hermes. Reads the existing durable bindings only. */
    suspend fun listReady(limit: Int = 32): List<ReadyBulkIngestionBatch> = mutex.withLock {
        val result = mutableListOf<ReadyBulkIngestionBatch>()
        Files.list(storageRoot).use { paths ->
            val iterator = paths.filter { it.fileName.toString().endsWith(".binding") }.sorted().iterator()
            while (iterator.hasNext() && result.size < limit) {
                val path = iterator.next()
                val batchId = path.fileName.toString().removeSuffix(".binding")
                val binding = runCatching { read(batchId) }.getOrNull() ?: continue
                val case = caseStorage.read(binding.caseId) ?: continue
                result += ReadyBulkIngestionBatch(batchId, case.caseName)
            }
        }
        result
    }

    suspend fun assignFromHermes(batchId: String, evidenceArtifactId: EvidenceArtifactId): BulkIngestionAssignment = mutex.withLock {
        val binding = read(batchId) ?: return@withLock BulkIngestionAssignment.UnknownBatch
        if (evidenceArtifactId !in binding.evidence) return@withLock BulkIngestionAssignment.EvidenceNotSubmittedUnderBatch
        return@withLock when (val outcome = caseEvidenceAssociationCoordinator.createOrGet(
            ownerPrincipalId,
            binding.caseId,
            evidenceArtifactId,
        )) {
            is CaseEvidenceAssociationCoordinatorOutcome.Created,
            is CaseEvidenceAssociationCoordinatorOutcome.AlreadyPresent -> BulkIngestionAssignment.Assigned(binding.caseId)
            CaseEvidenceAssociationCoordinatorOutcome.UnknownCase -> BulkIngestionAssignment.UnknownCase
            CaseEvidenceAssociationCoordinatorOutcome.UnknownEvidence -> BulkIngestionAssignment.UnknownEvidence
            is CaseEvidenceAssociationCoordinatorOutcome.MigrationNotReady -> BulkIngestionAssignment.MigrationNotReady(outcome.reasons)
            is CaseEvidenceAssociationCoordinatorOutcome.Failure -> BulkIngestionAssignment.Failure(outcome.reason)
        }
    }

    /**
     * Owner-authorized prep handoff seam. The case is always derived from the durable batch;
     * caller-supplied provenance cannot select or override case authority.
     */
    suspend fun registerPrepOccurrence(
        batchId: String,
        evidenceArtifactId: EvidenceArtifactId,
        sourceSha256: String,
        prepJobId: String,
        prepOccurrenceId: String,
        relativePath: String,
        archiveParentOccurrenceId: String?,
        archiveMemberPath: String?,
    ): BulkIngestionOccurrenceRegistration = mutex.withLock {
        val binding = read(batchId) ?: return@withLock BulkIngestionOccurrenceRegistration.UnknownBatch
        if (evidenceArtifactId !in binding.evidence) {
            return@withLock BulkIngestionOccurrenceRegistration.EvidenceNotSubmittedUnderBatch
        }
        val association = try {
            caseEvidenceAssociationCoordinator.findAssociation(binding.caseId, evidenceArtifactId)
        } catch (e: Exception) {
            return@withLock BulkIngestionOccurrenceRegistration.Failure(e.message ?: "association lookup failed")
        } ?: return@withLock BulkIngestionOccurrenceRegistration.Rejected("CASE_EVIDENCE_ASSOCIATION_NOT_FOUND")
        val occurrence = try {
            EvidenceOccurrence(
                occurrenceId = deterministicPrepOccurrenceId(prepJobId, prepOccurrenceId),
                associationId = association.associationId,
                evidenceArtifactId = evidenceArtifactId,
                caseId = binding.caseId,
                sourceSha256 = sourceSha256,
                prepJobId = prepJobId,
                prepOccurrenceId = prepOccurrenceId,
                relativePath = relativePath,
                archiveParentOccurrenceId = archiveParentOccurrenceId,
                archiveMemberPath = archiveMemberPath,
                createdAt = clock(),
            )
        } catch (e: IllegalArgumentException) {
            return@withLock BulkIngestionOccurrenceRegistration.Rejected(e.message ?: "invalid occurrence provenance")
        }
        return@withLock when (val result = caseEvidenceAssociationCoordinator.registerOccurrence(ownerPrincipalId, occurrence)) {
            is EvidenceOccurrenceCoordinatorOutcome.Created -> BulkIngestionOccurrenceRegistration.Created(
                result.occurrence.associationId.value, result.occurrence.occurrenceId.value,
            )
            is EvidenceOccurrenceCoordinatorOutcome.AlreadyPresent -> BulkIngestionOccurrenceRegistration.AlreadyPresent(
                result.occurrence.associationId.value, result.occurrence.occurrenceId.value,
            )
            EvidenceOccurrenceCoordinatorOutcome.UnknownCase -> BulkIngestionOccurrenceRegistration.Rejected("UNKNOWN_CASE")
            EvidenceOccurrenceCoordinatorOutcome.UnknownEvidence -> BulkIngestionOccurrenceRegistration.Rejected("UNKNOWN_EVIDENCE")
            EvidenceOccurrenceCoordinatorOutcome.AssociationNotFound -> BulkIngestionOccurrenceRegistration.Rejected("CASE_EVIDENCE_ASSOCIATION_NOT_FOUND")
            EvidenceOccurrenceCoordinatorOutcome.AssociationMismatch -> BulkIngestionOccurrenceRegistration.Rejected("CASE_EVIDENCE_ASSOCIATION_MISMATCH")
            EvidenceOccurrenceCoordinatorOutcome.SourceSha256Mismatch -> BulkIngestionOccurrenceRegistration.Rejected("SOURCE_SHA256_MISMATCH")
            EvidenceOccurrenceCoordinatorOutcome.InvalidProvenance -> BulkIngestionOccurrenceRegistration.Rejected("INVALID_PROVENANCE")
            is EvidenceOccurrenceCoordinatorOutcome.MigrationNotReady -> BulkIngestionOccurrenceRegistration.Rejected("MIGRATION_NOT_READY:${result.reasons.joinToString("|")}")
            is EvidenceOccurrenceCoordinatorOutcome.Failure -> BulkIngestionOccurrenceRegistration.Failure(result.reason)
        }
    }

    private fun safeBatchId(batchId: String) = require(Regex("^bulk-[a-f0-9-]+$").matches(batchId)) { "invalid batch id" }
    private fun path(batchId: String): Path { safeBatchId(batchId); return storageRoot.resolve("$batchId.binding") }
    private fun write(binding: BulkIngestionBinding) {
        val body = buildString {
            append("case=").append(enc(binding.caseId.value)).append('\n')
            append("evidence=").append(binding.evidence.joinToString(",") { enc(it.value) }).append('\n')
            append("externalTranscriptionAuthorised=").append(binding.externalTranscriptionAuthorised).append('\n')
        }.toByteArray(StandardCharsets.UTF_8)
        val target = path(binding.batchId)
        val tmp = Files.createTempFile(storageRoot, ".binding-", ".tmp")
        Files.write(tmp, body)
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun read(batchId: String): BulkIngestionBinding? {
        val target = path(batchId)
        if (!Files.exists(target)) return null
        val lines = Files.readAllLines(target)
        if (lines.any { it.count { character -> character == '=' } != 1 }) return null
        val fields = lines.associate { line -> line.substringBefore('=') to line.substringAfter('=') }
        if (fields.keys.any { it !in setOf("case", "evidence", "externalTranscriptionAuthorised") }) return null
        val case = fields["case"]?.let { CaseId(dec(it)) } ?: return null
        val evidence = fields["evidence"].orEmpty().split(',').filter { it.isNotEmpty() }.map { EvidenceArtifactId(dec(it)) }.toSet()
        val external = fields["externalTranscriptionAuthorised"]?.let { value ->
            when (value) { "true" -> true; "false" -> false; else -> return null }
        } ?: false
        return BulkIngestionBinding(batchId, case, evidence, external)
    }

    private fun enc(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun dec(value: String) = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}

internal sealed interface BulkIngestionOccurrenceRegistration {
    data class Created(val associationId: String, val occurrenceId: String) : BulkIngestionOccurrenceRegistration
    data class AlreadyPresent(val associationId: String, val occurrenceId: String) : BulkIngestionOccurrenceRegistration
    data object UnknownBatch : BulkIngestionOccurrenceRegistration
    data object EvidenceNotSubmittedUnderBatch : BulkIngestionOccurrenceRegistration
    data class Rejected(val reason: String) : BulkIngestionOccurrenceRegistration
    data class Failure(val reason: String) : BulkIngestionOccurrenceRegistration
}

internal data class BulkIngestionOccurrenceRequest(
    val sourceSha256: String,
    val prepJobId: String,
    val prepOccurrenceId: String,
    val relativePath: String,
    val archiveParentOccurrenceId: String?,
    val archiveMemberPath: String?,
)
