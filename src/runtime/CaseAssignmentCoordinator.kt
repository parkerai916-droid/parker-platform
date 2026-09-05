package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.CaseAssignmentRecord
import parker.core.interfaces.CaseAssignmentStorage
import parker.core.interfaces.CaseGovernanceAudit
import parker.core.interfaces.CaseGovernanceAuditEventType
import parker.core.interfaces.CaseGovernanceAuditRecord
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseRecord
import parker.core.interfaces.CaseStorage
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.PrincipalId

/**
 * CASE-1 — Owner Case / Matter Classification for Evidence. The one governance seam between the
 * narrow [CaseStorage]/[CaseAssignmentStorage] primitives and the owner-facing surface: mints
 * [CaseId] (never derived from the owner-supplied case name), validates both halves of an
 * assignment against real governed facts before ever writing one (Section "Assignment must
 * validate" of this unit's own instruction: the [EvidenceArtifactId] must already exist in
 * governed evidence custody -- reusing [EvidenceCustodian.retrieve]'s own existing, unmodified
 * authorization boundary, never a new one -- and the [CaseId] must already exist), and records
 * every governed fact ([CaseGovernanceAuditRecord]) this unit's own audit vocabulary requires.
 *
 * Deliberately owns no evidence enumeration of any kind -- [assign] and [currentAssignment] each
 * take one already-known [EvidenceArtifactId] the caller already possesses (from the existing
 * Owner Evidence UI's own listing), exactly mirroring every other owner-only coordinator in this
 * codebase's own "known identity in, never a query surface" discipline.
 */
internal class CaseAssignmentCoordinator(
    private val caseStorage: CaseStorage,
    private val assignmentStorage: CaseAssignmentStorage,
    private val audit: CaseGovernanceAudit,
    private val evidenceCustodian: EvidenceCustodian,
    private val ownerPrincipalId: PrincipalId,
    private val clock: () -> Instant = Instant::now,
    private val caseIdFactory: () -> CaseId = { CaseId("case-${UUID.randomUUID()}") },
) {
    suspend fun createCase(caseName: String): CaseCreationOutcome {
        val trimmed = caseName.trim()
        if (trimmed.isBlank()) {
            return CaseCreationOutcome.InvalidCaseName("Case name must not be blank")
        }
        if (trimmed.length > CaseRecord.MAX_CASE_NAME_LENGTH) {
            return CaseCreationOutcome.InvalidCaseName("Case name must not exceed ${CaseRecord.MAX_CASE_NAME_LENGTH} characters")
        }
        val case = try {
            CaseRecord(caseIdFactory(), trimmed, clock())
        } catch (e: IllegalArgumentException) {
            return CaseCreationOutcome.InvalidCaseName(e.message ?: "invalid case name")
        }
        return try {
            caseStorage.create(case)
            audit.record(
                CaseGovernanceAuditRecord(
                    eventType = CaseGovernanceAuditEventType.CASE_CREATED,
                    caseId = case.caseId,
                    actorPrincipalId = ownerPrincipalId,
                    recordedAt = clock(),
                ),
            )
            CaseCreationOutcome.Created(case)
        } catch (e: Exception) {
            CaseCreationOutcome.Failure(e.message ?: "case could not be created")
        }
    }

    suspend fun listCases(): List<CaseRecord> = caseStorage.list()

    suspend fun currentAssignment(evidenceArtifactId: EvidenceArtifactId): CaseId? =
        assignmentStorage.readAssignment(evidenceArtifactId)?.caseId

    /**
     * Assigns or reassigns [evidenceArtifactId] to [caseId] (`null` deliberately returns it to
     * Unassigned). Fails closed with [CaseAssignmentOutcome.UnknownEvidence] /
     * [CaseAssignmentOutcome.UnknownCase] before ever touching [assignmentStorage] if either half
     * does not already exist. A request that would not change the current assignment is a
     * harmless, unaudited no-op -- "do not silently overwrite... without recording the change"
     * governs an actual change, not reselecting the same value.
     */
    suspend fun assign(evidenceArtifactId: EvidenceArtifactId, caseId: CaseId?): CaseAssignmentOutcome {
        if (caseId != null && caseStorage.read(caseId) == null) {
            return CaseAssignmentOutcome.UnknownCase
        }
        val evidenceExists = when (evidenceCustodian.retrieve(ownerPrincipalId, evidenceArtifactId)) {
            is EvidenceRetrievalResult.Found -> true
            is EvidenceRetrievalResult.NotFound, is EvidenceRetrievalResult.Rejected -> false
        }
        if (!evidenceExists) {
            return CaseAssignmentOutcome.UnknownEvidence
        }

        val previous = assignmentStorage.readAssignment(evidenceArtifactId)?.caseId
        if (previous == caseId) {
            return CaseAssignmentOutcome.NoChange
        }

        val now = clock()
        val record = CaseAssignmentRecord(evidenceArtifactId, caseId, now)
        return try {
            assignmentStorage.writeAssignment(record)
            audit.record(
                CaseGovernanceAuditRecord(
                    eventType = if (previous == null) CaseGovernanceAuditEventType.EVIDENCE_ASSIGNED else CaseGovernanceAuditEventType.EVIDENCE_REASSIGNED,
                    caseId = caseId,
                    previousCaseId = previous,
                    evidenceArtifactId = evidenceArtifactId,
                    actorPrincipalId = ownerPrincipalId,
                    recordedAt = now,
                ),
            )
            if (previous == null) CaseAssignmentOutcome.Assigned(record) else CaseAssignmentOutcome.Reassigned(record, previous)
        } catch (e: Exception) {
            CaseAssignmentOutcome.Failure(e.message ?: "assignment could not be recorded")
        }
    }
}

sealed interface CaseCreationOutcome {
    data class Created(val case: CaseRecord) : CaseCreationOutcome
    data class InvalidCaseName(val reason: String) : CaseCreationOutcome
    data class Failure(val reason: String) : CaseCreationOutcome
}

sealed interface CaseAssignmentOutcome {
    data class Assigned(val record: CaseAssignmentRecord) : CaseAssignmentOutcome
    data class Reassigned(val record: CaseAssignmentRecord, val previousCaseId: CaseId) : CaseAssignmentOutcome
    data object NoChange : CaseAssignmentOutcome
    data object UnknownEvidence : CaseAssignmentOutcome
    data object UnknownCase : CaseAssignmentOutcome
    data class Failure(val reason: String) : CaseAssignmentOutcome
}
