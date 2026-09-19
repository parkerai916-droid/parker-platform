package parker.core.runtime

import parker.core.interfaces.CaseEvidenceAssociation
import parker.core.interfaces.CaseEvidenceAssociationCreationOutcome
import parker.core.interfaces.CaseEvidenceAssociationMigrationReadinessProvider
import parker.core.interfaces.CaseEvidenceAssociationStorage
import parker.core.interfaces.CaseGovernanceAudit
import parker.core.interfaces.CaseGovernanceAuditEventType
import parker.core.interfaces.CaseGovernanceAuditQuery
import parker.core.interfaces.CaseGovernanceAuditReader
import parker.core.interfaces.CaseGovernanceAuditRecord
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseStorage
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceOccurrence
import parker.core.interfaces.EvidenceOccurrenceRegistrationOutcome
import parker.core.interfaces.EvidenceOccurrenceStorage
import parker.core.interfaces.EvidenceSourceManifestStorage
import parker.core.interfaces.PrincipalId
import java.time.Instant

/**
 * Unit 4's governed runtime seam for multi-case membership and source occurrences.
 * It deliberately does not grant permission, mutate legacy assignment state, or expose retrieval.
 */
internal class CaseEvidenceAssociationCoordinator(
    private val caseStorage: CaseStorage,
    private val manifestStorage: EvidenceSourceManifestStorage,
    private val associationStorage: CaseEvidenceAssociationStorage,
    private val occurrenceStorage: EvidenceOccurrenceStorage,
    private val migrationReadiness: CaseEvidenceAssociationMigrationReadinessProvider,
    private val audit: CaseGovernanceAudit,
    private val auditReader: CaseGovernanceAuditReader,
    private val clock: () -> Instant = Instant::now,
) {
    suspend fun createOrGetAssociation(
        actorPrincipalId: PrincipalId,
        caseId: CaseId,
        evidenceArtifactId: EvidenceArtifactId,
    ): CaseEvidenceAssociationCoordinatorOutcome {
        val readiness = readinessOrFailure() ?: return CaseEvidenceAssociationCoordinatorOutcome.MigrationNotReady(
            listOf("MIGRATION_READINESS_FAILURE"),
        )
        if (!readiness.ready) return CaseEvidenceAssociationCoordinatorOutcome.MigrationNotReady(readiness.reasons)
        try {
            if (caseStorage.read(caseId) == null) return CaseEvidenceAssociationCoordinatorOutcome.UnknownCase
            if (manifestStorage.read(evidenceArtifactId) == null) return CaseEvidenceAssociationCoordinatorOutcome.UnknownEvidence
            val result = associationStorage.createOrGet(caseId, evidenceArtifactId, clock())
            val association = when (result) {
                is CaseEvidenceAssociationCreationOutcome.Created -> result.association
                is CaseEvidenceAssociationCreationOutcome.AlreadyPresent -> result.association
            }
            try {
                ensureAssociationCreatedAudit(actorPrincipalId, association)
            } catch (e: Exception) {
                return CaseEvidenceAssociationCoordinatorOutcome.Failure(CoordinatorFailureKind.AUDIT, e.message ?: "association audit failed")
            }
            return when (result) {
                is CaseEvidenceAssociationCreationOutcome.Created -> CaseEvidenceAssociationCoordinatorOutcome.Created(association)
                is CaseEvidenceAssociationCreationOutcome.AlreadyPresent -> CaseEvidenceAssociationCoordinatorOutcome.AlreadyPresent(association)
            }
        } catch (e: CaseEvidenceAssociationStorageException.CorruptRecord) {
            return CaseEvidenceAssociationCoordinatorOutcome.Failure(CoordinatorFailureKind.CORRUPT_ASSOCIATION, e.message ?: "association record is corrupt")
        } catch (e: CaseEvidenceAssociationStorageException) {
            return CaseEvidenceAssociationCoordinatorOutcome.Failure(CoordinatorFailureKind.ASSOCIATION_STORAGE, e.message ?: "association storage failed")
        } catch (e: Exception) {
            return CaseEvidenceAssociationCoordinatorOutcome.Failure(CoordinatorFailureKind.STORAGE, e.message ?: "association operation failed")
        }
    }

    /** Short alias matching the storage operation while keeping the coordinator's governance seam explicit. */
    suspend fun createOrGet(
        actorPrincipalId: PrincipalId,
        caseId: CaseId,
        evidenceArtifactId: EvidenceArtifactId,
    ): CaseEvidenceAssociationCoordinatorOutcome = createOrGetAssociation(actorPrincipalId, caseId, evidenceArtifactId)

    /** Read-only lookup used by the batch-bound occurrence handoff; no association is created here. */
    suspend fun findAssociation(caseId: CaseId, evidenceArtifactId: EvidenceArtifactId): CaseEvidenceAssociation? =
        associationStorage.find(caseId, evidenceArtifactId)

    suspend fun registerOccurrence(
        actorPrincipalId: PrincipalId,
        occurrence: EvidenceOccurrence,
    ): EvidenceOccurrenceCoordinatorOutcome {
        val readiness = readinessOrFailure() ?: return EvidenceOccurrenceCoordinatorOutcome.MigrationNotReady(listOf("MIGRATION_READINESS_FAILURE"))
        if (!readiness.ready) return EvidenceOccurrenceCoordinatorOutcome.MigrationNotReady(readiness.reasons)
        try {
            if (caseStorage.read(occurrence.caseId) == null) return EvidenceOccurrenceCoordinatorOutcome.UnknownCase
            val manifest = manifestStorage.read(occurrence.evidenceArtifactId)
                ?: return EvidenceOccurrenceCoordinatorOutcome.UnknownEvidence
            if (occurrence.sourceSha256 != manifest.sha256) return EvidenceOccurrenceCoordinatorOutcome.SourceSha256Mismatch

            val association = associationStorage.find(occurrence.caseId, occurrence.evidenceArtifactId)
                ?: return EvidenceOccurrenceCoordinatorOutcome.AssociationNotFound
            if (association.associationId != occurrence.associationId ||
                association.caseId != occurrence.caseId ||
                association.evidenceArtifactId != occurrence.evidenceArtifactId
            ) {
                return EvidenceOccurrenceCoordinatorOutcome.AssociationMismatch
            }
            if (occurrence.prepJobId != null &&
                (occurrence.prepOccurrenceId.isNullOrBlank() || occurrence.relativePath.isNullOrBlank())
            ) {
                return EvidenceOccurrenceCoordinatorOutcome.InvalidProvenance
            }
            if ((occurrence.archiveParentOccurrenceId == null) != (occurrence.archiveMemberPath == null)) {
                return EvidenceOccurrenceCoordinatorOutcome.InvalidProvenance
            }

            val result = occurrenceStorage.createOrGet(occurrence)
            try {
                ensureOccurrenceAddedAudit(actorPrincipalId, association, occurrence)
            } catch (e: Exception) {
                return EvidenceOccurrenceCoordinatorOutcome.Failure(CoordinatorFailureKind.AUDIT, e.message ?: "occurrence audit failed")
            }
            return when (result) {
                is EvidenceOccurrenceRegistrationOutcome.Created -> EvidenceOccurrenceCoordinatorOutcome.Created(result.occurrence)
                is EvidenceOccurrenceRegistrationOutcome.AlreadyPresent -> EvidenceOccurrenceCoordinatorOutcome.AlreadyPresent(result.occurrence)
            }
        } catch (e: CaseEvidenceAssociationStorageException.CorruptRecord) {
            return EvidenceOccurrenceCoordinatorOutcome.Failure(CoordinatorFailureKind.CORRUPT_ASSOCIATION, e.message ?: "association record is corrupt")
        } catch (e: EvidenceOccurrenceStorageException.CorruptRecord) {
            return EvidenceOccurrenceCoordinatorOutcome.Failure(CoordinatorFailureKind.CORRUPT_OCCURRENCE, e.message ?: "occurrence record is corrupt")
        } catch (e: EvidenceOccurrenceStorageException) {
            return EvidenceOccurrenceCoordinatorOutcome.Failure(CoordinatorFailureKind.OCCURRENCE_STORAGE, e.message ?: "occurrence storage failed")
        } catch (e: Exception) {
            return EvidenceOccurrenceCoordinatorOutcome.Failure(CoordinatorFailureKind.STORAGE, e.message ?: "occurrence operation failed")
        }
    }

    private suspend fun readinessOrFailure() = try {
        migrationReadiness.readiness()
    } catch (_: Exception) {
        null
    }

    private suspend fun ensureAssociationCreatedAudit(actor: PrincipalId, association: CaseEvidenceAssociation) {
        val query = CaseGovernanceAuditQuery(
            eventType = CaseGovernanceAuditEventType.CASE_EVIDENCE_ASSOCIATION_CREATED,
            caseId = association.caseId,
            evidenceArtifactId = association.evidenceArtifactId,
            caseEvidenceAssociationId = association.associationId,
        )
        if (!auditReader.has(query)) {
            audit.record(
                CaseGovernanceAuditRecord(
                    eventType = CaseGovernanceAuditEventType.CASE_EVIDENCE_ASSOCIATION_CREATED,
                    caseId = association.caseId,
                    evidenceArtifactId = association.evidenceArtifactId,
                    actorPrincipalId = actor,
                    recordedAt = clock(),
                    caseEvidenceAssociationId = association.associationId,
                ),
            )
        }
    }

    private suspend fun ensureOccurrenceAddedAudit(
        actor: PrincipalId,
        association: CaseEvidenceAssociation,
        occurrence: EvidenceOccurrence,
    ) {
        val query = CaseGovernanceAuditQuery(
            eventType = CaseGovernanceAuditEventType.EVIDENCE_OCCURRENCE_ADDED,
            caseId = association.caseId,
            evidenceArtifactId = association.evidenceArtifactId,
            caseEvidenceAssociationId = association.associationId,
            evidenceOccurrenceId = occurrence.occurrenceId,
        )
        if (!auditReader.has(query)) {
            audit.record(
                CaseGovernanceAuditRecord(
                    eventType = CaseGovernanceAuditEventType.EVIDENCE_OCCURRENCE_ADDED,
                    caseId = association.caseId,
                    evidenceArtifactId = association.evidenceArtifactId,
                    actorPrincipalId = actor,
                    recordedAt = clock(),
                    caseEvidenceAssociationId = association.associationId,
                    evidenceOccurrenceId = occurrence.occurrenceId,
                ),
            )
        }
    }
}

enum class CoordinatorFailureKind {
    CORRUPT_ASSOCIATION,
    CORRUPT_OCCURRENCE,
    ASSOCIATION_STORAGE,
    OCCURRENCE_STORAGE,
    AUDIT,
    STORAGE,
}

sealed interface CaseEvidenceAssociationCoordinatorOutcome {
    data class Created(val association: CaseEvidenceAssociation) : CaseEvidenceAssociationCoordinatorOutcome
    data class AlreadyPresent(val association: CaseEvidenceAssociation) : CaseEvidenceAssociationCoordinatorOutcome
    data object UnknownCase : CaseEvidenceAssociationCoordinatorOutcome
    data object UnknownEvidence : CaseEvidenceAssociationCoordinatorOutcome
    data class MigrationNotReady(val reasons: List<String>) : CaseEvidenceAssociationCoordinatorOutcome
    data class Failure(val kind: CoordinatorFailureKind, val reason: String) : CaseEvidenceAssociationCoordinatorOutcome
}

sealed interface EvidenceOccurrenceCoordinatorOutcome {
    data class Created(val occurrence: EvidenceOccurrence) : EvidenceOccurrenceCoordinatorOutcome
    data class AlreadyPresent(val occurrence: EvidenceOccurrence) : EvidenceOccurrenceCoordinatorOutcome
    data object UnknownCase : EvidenceOccurrenceCoordinatorOutcome
    data object UnknownEvidence : EvidenceOccurrenceCoordinatorOutcome
    data object AssociationNotFound : EvidenceOccurrenceCoordinatorOutcome
    data object AssociationMismatch : EvidenceOccurrenceCoordinatorOutcome
    data object SourceSha256Mismatch : EvidenceOccurrenceCoordinatorOutcome
    data object InvalidProvenance : EvidenceOccurrenceCoordinatorOutcome
    data class MigrationNotReady(val reasons: List<String>) : EvidenceOccurrenceCoordinatorOutcome
    data class Failure(val kind: CoordinatorFailureKind, val reason: String) : EvidenceOccurrenceCoordinatorOutcome
}
