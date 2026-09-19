package parker.core.interfaces

import java.time.Instant

/** Read-only discovery seam for the current legacy assignment files. */
interface CurrentCaseAssignmentSource {
    suspend fun listCurrentAssignmentIds(): List<EvidenceArtifactId>
}

enum class CaseEvidenceAssociationMigrationStatus {
    NOT_STARTED,
    RUNNING,
    COMPLETE,
    INCOMPLETE,
}

data class LegacyAssignmentSnapshot(
    val evidenceArtifactId: EvidenceArtifactId,
    val caseId: CaseId?,
    val assignedAt: Instant,
)

data class CaseEvidenceAssociationMigrationFailure(
    val evidenceArtifactId: EvidenceArtifactId?,
    val reason: String,
)

data class CaseEvidenceAssociationMigrationState(
    val schemaVersion: Int,
    val status: CaseEvidenceAssociationMigrationStatus,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val processedAssignments: List<LegacyAssignmentSnapshot>,
    val failures: List<CaseEvidenceAssociationMigrationFailure>,
)

data class CaseEvidenceAssociationMigrationReadiness(
    val status: CaseEvidenceAssociationMigrationStatus,
    val ready: Boolean,
    val reasons: List<String>,
)

interface CaseEvidenceAssociationMigrationStateStorage {
    suspend fun read(): CaseEvidenceAssociationMigrationState
    suspend fun write(state: CaseEvidenceAssociationMigrationState)
}

fun interface CaseEvidenceAssociationMigrationReadinessProvider {
    suspend fun readiness(): CaseEvidenceAssociationMigrationReadiness
}
