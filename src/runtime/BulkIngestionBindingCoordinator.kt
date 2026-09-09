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
import parker.core.interfaces.PrincipalId

/** One Owner-authorised, immutable batch-to-case binding and its submitted opaque artifact ids. */
internal data class BulkIngestionBinding(val batchId: String, val caseId: CaseId, val evidence: Set<EvidenceArtifactId>)

internal sealed interface BulkIngestionAuthorisation {
    data class Authorised(val binding: BulkIngestionBinding) : BulkIngestionAuthorisation
    data object UnknownCase : BulkIngestionAuthorisation
    data class Failure(val reason: String) : BulkIngestionAuthorisation
}

internal sealed interface BulkIngestionAssignment {
    data class Assigned(val caseId: CaseId) : BulkIngestionAssignment
    data object UnknownBatch : BulkIngestionAssignment
    data object EvidenceNotSubmittedUnderBatch : BulkIngestionAssignment
    data class Rejected(val reason: String) : BulkIngestionAssignment
    data class Failure(val reason: String) : BulkIngestionAssignment
}

/**
 * The only subordinate case-binding seam. The case id is written once by an Owner call and is
 * never accepted by the Hermes-facing methods. Evidence membership is durable and is recorded
 * only after Parker's Evidence Custodian returns the submitted artifact identity.
 */
internal class BulkIngestionBindingCoordinator(
    private val storageRoot: Path,
    private val caseStorage: CaseStorage,
    private val caseAssignmentCoordinator: CaseAssignmentCoordinator,
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
        val binding = BulkIngestionBinding(batchId, caseId, emptySet())
        return@withLock try {
            write(binding)
            audit.record(CaseGovernanceAuditRecord(CaseGovernanceAuditEventType.INGESTION_BATCH_AUTHORISED, caseId, actorPrincipalId = ownerPrincipalId, recordedAt = clock()))
            BulkIngestionAuthorisation.Authorised(binding)
        } catch (e: Exception) { BulkIngestionAuthorisation.Failure(e.message ?: "batch authorisation failed") }
    }

    suspend fun recordSubmission(batchId: String, evidenceArtifactId: EvidenceArtifactId): Boolean = mutex.withLock {
        val existing = read(batchId) ?: return@withLock false
        if (evidenceArtifactId in existing.evidence) return@withLock true
        write(existing.copy(evidence = existing.evidence + evidenceArtifactId))
        true
    }

    suspend fun isAuthorised(batchId: String): Boolean = mutex.withLock { read(batchId) != null }

    suspend fun assignFromHermes(batchId: String, evidenceArtifactId: EvidenceArtifactId): BulkIngestionAssignment = mutex.withLock {
        val binding = read(batchId) ?: return@withLock BulkIngestionAssignment.UnknownBatch
        if (evidenceArtifactId !in binding.evidence) return@withLock BulkIngestionAssignment.EvidenceNotSubmittedUnderBatch
        return@withLock when (val outcome = caseAssignmentCoordinator.assign(evidenceArtifactId, binding.caseId)) {
            is CaseAssignmentOutcome.Assigned, is CaseAssignmentOutcome.NoChange -> BulkIngestionAssignment.Assigned(binding.caseId)
            is CaseAssignmentOutcome.Reassigned -> BulkIngestionAssignment.Rejected("evidence is already assigned to another case")
            is CaseAssignmentOutcome.UnknownCase, is CaseAssignmentOutcome.UnknownEvidence -> BulkIngestionAssignment.Rejected("Parker rejected the governed assignment")
            is CaseAssignmentOutcome.Failure -> BulkIngestionAssignment.Failure(outcome.reason)
        }
    }

    private fun safeBatchId(batchId: String) = require(Regex("^bulk-[a-f0-9-]+$").matches(batchId)) { "invalid batch id" }
    private fun path(batchId: String): Path { safeBatchId(batchId); return storageRoot.resolve("$batchId.binding") }

    private fun write(binding: BulkIngestionBinding) {
        val body = buildString {
            append("case=").append(enc(binding.caseId.value)).append('\n')
            append("evidence=").append(binding.evidence.joinToString(",") { enc(it.value) }).append('\n')
        }.toByteArray(StandardCharsets.UTF_8)
        val target = path(binding.batchId)
        val tmp = Files.createTempFile(storageRoot, ".binding-", ".tmp")
        Files.write(tmp, body)
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun read(batchId: String): BulkIngestionBinding? {
        val target = path(batchId)
        if (!Files.exists(target)) return null
        val fields = Files.readAllLines(target).associate { it.substringBefore('=') to it.substringAfter('=') }
        val case = fields["case"]?.let { CaseId(dec(it)) } ?: return null
        val evidence = fields["evidence"].orEmpty().split(',').filter { it.isNotEmpty() }.map { EvidenceArtifactId(dec(it)) }.toSet()
        return BulkIngestionBinding(batchId, case, evidence)
    }

    private fun enc(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun dec(value: String) = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}
