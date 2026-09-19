package parker.core.runtime

import java.time.Instant
import parker.core.interfaces.CaseGovernanceAudit
import parker.core.interfaces.CaseGovernanceAuditEventType
import parker.core.interfaces.CaseGovernanceAuditRecord
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseLifecycleStatus
import parker.core.interfaces.CaseStorage
import parker.core.interfaces.PrincipalId

internal class CaseLifecycleCoordinator(
    private val caseStorage: CaseStorage,
    private val audit: CaseGovernanceAudit,
    private val actor: PrincipalId,
    private val clock: () -> Instant = Instant::now,
) {
    suspend fun archive(caseId: CaseId): CaseLifecycleOutcome = transition(caseId, CaseLifecycleStatus.ARCHIVED, CaseGovernanceAuditEventType.CASE_ARCHIVED)

    suspend fun restore(caseId: CaseId): CaseLifecycleOutcome = transition(caseId, CaseLifecycleStatus.ACTIVE, CaseGovernanceAuditEventType.CASE_RESTORED)

    private suspend fun transition(caseId: CaseId, target: CaseLifecycleStatus, event: CaseGovernanceAuditEventType): CaseLifecycleOutcome {
        val current = caseStorage.read(caseId) ?: return CaseLifecycleOutcome.UnknownCase
        if (current.lifecycleStatus == target) {
            return if (target == CaseLifecycleStatus.ARCHIVED) CaseLifecycleOutcome.AlreadyArchived(current) else CaseLifecycleOutcome.AlreadyActive(current)
        }
        return try {
            val updated = caseStorage.updateLifecycle(caseId, target) ?: return CaseLifecycleOutcome.UnknownCase
            audit.record(CaseGovernanceAuditRecord(event, caseId, actorPrincipalId = actor, recordedAt = clock()))
            CaseLifecycleOutcome.Changed(updated)
        } catch (e: Exception) {
            CaseLifecycleOutcome.Failed(e.message ?: "case lifecycle update failed")
        }
    }
}

sealed interface CaseLifecycleOutcome {
    data class Changed(val case: parker.core.interfaces.CaseRecord) : CaseLifecycleOutcome
    data class AlreadyArchived(val case: parker.core.interfaces.CaseRecord) : CaseLifecycleOutcome
    data class AlreadyActive(val case: parker.core.interfaces.CaseRecord) : CaseLifecycleOutcome
    data object UnknownCase : CaseLifecycleOutcome
    data class Failed(val reason: String) : CaseLifecycleOutcome
}
