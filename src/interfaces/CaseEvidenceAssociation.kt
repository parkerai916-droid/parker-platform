package parker.core.interfaces

import java.time.Instant

/**
 * Multi-case evidence association identity. This is deliberately an opaque
 * identity: deterministic issuance and durable uniqueness belong to the later
 * association storage/runtime units.
 */
@JvmInline
value class CaseEvidenceAssociationId(val value: String) {
    init {
        require(value.isNotBlank()) { "CaseEvidenceAssociationId must not be blank" }
    }
}

/**
 * One governed relationship between canonical evidence content and one case.
 *
 * The uniqueness contract is `(caseId, evidenceArtifactId)`, not the generated
 * association identifier. One canonical artifact may therefore have many
 * associations, while one case/artifact pair has exactly one association.
 * Creating the same pair again is represented by the storage outcome
 * [CaseEvidenceAssociationCreationOutcome.AlreadyPresent].
 */
data class CaseEvidenceAssociation(
    val associationId: CaseEvidenceAssociationId,
    val evidenceArtifactId: EvidenceArtifactId,
    val caseId: CaseId,
    val associatedAt: Instant,
)

/** Normal, non-exceptional result of idempotent association creation. */
sealed interface CaseEvidenceAssociationCreationOutcome {
    data class Created(val association: CaseEvidenceAssociation) : CaseEvidenceAssociationCreationOutcome
    data class AlreadyPresent(val association: CaseEvidenceAssociation) : CaseEvidenceAssociationCreationOutcome
}

/**
 * Opaque identity for one source occurrence. Occurrence identity is never
 * derived from a filename; for Parker Ingestion Prep, the later storage unit
 * uses `(prepJobId, prepOccurrenceId)` as the idempotency key.
 */
@JvmInline
value class EvidenceOccurrenceId(val value: String) {
    init {
        require(value.isNotBlank()) { "EvidenceOccurrenceId must not be blank" }
    }
}

/**
 * Case-scoped source/provenance context for canonical evidence content.
 *
 * An association may have multiple occurrences. Prep fields are nullable only
 * because a non-prep acquisition may not have a Parker prep job identity. For
 * prep-origin occurrences, job ID, occurrence ID, and relative path are all
 * required and the pair `(prepJobId, prepOccurrenceId)` is the later storage
 * unit's idempotency identity. Archive fields are supplied together when the
 * occurrence came from an archive member.
 *
 * This record contains no evidence bytes and grants no permission. It records
 * provenance only; canonical content remains identified by
 * [evidenceArtifactId] and [sourceSha256]. The [associationId], [caseId], and
 * [evidenceArtifactId] values must refer to the same governed association;
 * runtime validation belongs to later units.
 */
data class EvidenceOccurrence(
    val occurrenceId: EvidenceOccurrenceId,
    val associationId: CaseEvidenceAssociationId,
    val evidenceArtifactId: EvidenceArtifactId,
    val caseId: CaseId,
    val sourceSha256: String,
    val prepJobId: String? = null,
    val prepOccurrenceId: String? = null,
    val relativePath: String? = null,
    val archiveParentOccurrenceId: String? = null,
    val archiveMemberPath: String? = null,
    val createdAt: Instant,
) {
    init {
        require(sourceSha256.matches(SHA256_PATTERN)) {
            "EvidenceOccurrence.sourceSha256 must be exactly 64 lowercase hexadecimal characters"
        }
        require((prepJobId == null) == (prepOccurrenceId == null)) {
            "EvidenceOccurrence prepJobId and prepOccurrenceId must be supplied together"
        }
        require(prepJobId == null || prepJobId.isNotBlank()) {
            "EvidenceOccurrence.prepJobId must not be blank when present"
        }
        require(prepOccurrenceId == null || prepOccurrenceId.isNotBlank()) {
            "EvidenceOccurrence.prepOccurrenceId must not be blank when present"
        }
        require(prepJobId == null || !relativePath.isNullOrBlank()) {
            "EvidenceOccurrence.relativePath is required for prep-origin occurrences"
        }
        require(relativePath == null || relativePath.isNotBlank()) {
            "EvidenceOccurrence.relativePath must not be blank when present"
        }
        require((archiveParentOccurrenceId == null) == (archiveMemberPath == null)) {
            "EvidenceOccurrence archive parent and member path must be supplied together"
        }
        require(archiveParentOccurrenceId == null || archiveParentOccurrenceId.isNotBlank()) {
            "EvidenceOccurrence.archiveParentOccurrenceId must not be blank when present"
        }
        require(archiveMemberPath == null || archiveMemberPath.isNotBlank()) {
            "EvidenceOccurrence.archiveMemberPath must not be blank when present"
        }
    }

    private companion object {
        val SHA256_PATTERN = Regex("^[0-9a-f]{64}$")
    }
}

/** Normal, non-exceptional result of idempotent occurrence registration. */
sealed interface EvidenceOccurrenceRegistrationOutcome {
    data class Created(val occurrence: EvidenceOccurrence) : EvidenceOccurrenceRegistrationOutcome
    data class AlreadyPresent(val occurrence: EvidenceOccurrence) : EvidenceOccurrenceRegistrationOutcome
}

/** Contract for durable, case-scoped artifact associations; no persistence is supplied here. */
interface CaseEvidenceAssociationStorage {
    suspend fun createOrGet(
        caseId: CaseId,
        evidenceArtifactId: EvidenceArtifactId,
        associatedAt: Instant,
    ): CaseEvidenceAssociationCreationOutcome

    suspend fun find(caseId: CaseId, evidenceArtifactId: EvidenceArtifactId): CaseEvidenceAssociation?

    suspend fun listForCase(caseId: CaseId): List<CaseEvidenceAssociation>

    suspend fun listForEvidence(evidenceArtifactId: EvidenceArtifactId): List<CaseEvidenceAssociation>
}

/** Contract for durable case-scoped source occurrences; no persistence is supplied here. */
interface EvidenceOccurrenceStorage {
    suspend fun createOrGet(occurrence: EvidenceOccurrence): EvidenceOccurrenceRegistrationOutcome

    suspend fun findByPrepOccurrence(prepJobId: String, prepOccurrenceId: String): EvidenceOccurrence?

    suspend fun listForAssociation(associationId: CaseEvidenceAssociationId): List<EvidenceOccurrence>
}
