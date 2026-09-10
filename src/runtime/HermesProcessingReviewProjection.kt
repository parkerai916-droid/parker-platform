package parker.core.runtime

import parker.core.interfaces.HermesProcessingDecisionRegistry
import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingHumanDecision
import parker.core.interfaces.HermesProcessingHumanDecisionType
import parker.core.interfaces.HermesProcessingIssue
import parker.core.interfaces.HermesProcessingMethod
import parker.core.interfaces.HermesProcessingResultRegistry
import parker.core.interfaces.HermesProcessingStatus

/**
 * Hermes Exception Decision Backend, Task 4. One row of the Owner review queue -- enough for a
 * future Steve review screen (Task 4's own R0 target: `Case / Document / Page / Issue / Hermes
 * interpretation / [ ACCEPT ] [ CORRECT ] [ REPROCESS ] [ REJECT ]`), never more. Deliberately
 * omits [parker.core.interfaces.CaseId] itself -- only [caseDisplayName], mirroring
 * [ReadyBulkIngestionBatch]'s own identical "case name only, never CaseId" precedent for a
 * Hermes-facing projection; this one is Owner-facing, so the precedent is followed by choice, not
 * by boundary necessity, but is followed anyway since nothing here needs the raw [parker.core.interfaces.CaseId].
 */
data class HermesProcessingReviewItem(
    val batchId: String,
    val sourceSha256: String,
    val status: HermesProcessingStatus,
    val methods: Set<HermesProcessingMethod>,
    val issues: List<HermesProcessingIssue>,
    val failure: HermesProcessingFailure?,
    val latestDecision: HermesProcessingHumanDecision?,
    val caseDisplayName: String?,
)

/**
 * Hermes Exception Decision Backend, Task 4. A pure, read-only composition of the existing
 * [HermesProcessingResultRegistry] (the machine fact) and [HermesProcessingDecisionRegistry] (the
 * human fact) -- mirroring [SteveReviewQueueProjection]'s own established "compose several
 * existing registries into one read projection, invent no new authority" shape. Performs no
 * permission check of its own (that lives in [HermesProcessingDecisionCoordinator], the one
 * production caller); this class is unconditionally read-only and side-effect-free.
 */
class HermesProcessingReviewProjection(
    private val processingResultRegistry: HermesProcessingResultRegistry,
    private val decisionRegistry: HermesProcessingDecisionRegistry,
    private val caseDisplayNameForBatch: suspend (String) -> String? = { null },
) {

    /**
     * Every pre-ingestion exception still requiring Steve's attention, across every batch. Never
     * returns a `PASS` result -- those never require Owner attention. Excludes a REVIEW_REQUIRED
     * result already resolved by a human ACCEPT/CORRECT and any result already blocked by a human
     * REJECT (Task 4's own "resolved items normally leave the pending queue" rule); a REPROCESS
     * decision, and a FAILED result corrected but not thereby resolved (see
     * [HermesProcessingEffectiveGate]'s own KDoc for why FAILED + CORRECT never resolves in this
     * R0 scope), both remain visible -- both still need Steve's eyes.
     */
    suspend fun listPendingForOwner(): List<HermesProcessingReviewItem> =
        processingResultRegistry.listAll().mapNotNull { result ->
            val latestDecision = decisionRegistry.latest(result.batchId, result.sourceSha256)
            if (!isPending(result.status, latestDecision?.decision)) return@mapNotNull null
            HermesProcessingReviewItem(
                batchId = result.batchId,
                sourceSha256 = result.sourceSha256,
                status = result.status,
                methods = result.methods,
                issues = result.issues,
                failure = result.failure,
                latestDecision = latestDecision,
                caseDisplayName = caseDisplayNameForBatch(result.batchId),
            )
        }

    private fun isPending(status: HermesProcessingStatus, decision: HermesProcessingHumanDecisionType?): Boolean {
        if (status == HermesProcessingStatus.PASS) return false
        return when (decision) {
            null, HermesProcessingHumanDecisionType.REPROCESS -> true
            HermesProcessingHumanDecisionType.ACCEPT -> false
            HermesProcessingHumanDecisionType.REJECT -> false
            // REVIEW_REQUIRED + CORRECT resolves (the effective gate allows); FAILED + CORRECT
            // does not (the effective gate still blocks) -- so FAILED stays visible, REVIEW_REQUIRED does not.
            HermesProcessingHumanDecisionType.CORRECT -> status == HermesProcessingStatus.FAILED
        }
    }
}
