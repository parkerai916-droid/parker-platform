package parker.core.runtime

import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingHumanDecision
import parker.core.interfaces.HermesProcessingHumanDecisionType
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingStatus

/**
 * Hermes Exception Decision Backend, Task 4. The complete, closed outcome of combining one stored
 * [HermesProcessingResult]'s own machine status with the latest [HermesProcessingHumanDecision]
 * (if any) for the same (`batchId`, `sourceSha256`) key. Consulted by
 * [AgentGatewayEvidenceProjection.submitGovernedIngestion] (Task 3's own governed-ingestion path,
 * extended, never replaced) immediately after Task 3's own existing hash-verified stored-result
 * lookup -- this type invents no dedup, registration, permission, or hash logic of its own.
 */
sealed class HermesEffectiveGateDecision {

    /** Governed ingestion may proceed: machine PASS, or a human ACCEPT/CORRECT resolving REVIEW_REQUIRED. */
    data object Allow : HermesEffectiveGateDecision()

    /** A human REJECT is on record for this exact key -- always blocks, even over a machine PASS. Highest precedence. */
    data object HumanRejected : HermesEffectiveGateDecision()

    /** A human REPROCESS is on record for this exact key -- always blocks, even over a machine PASS. */
    data object ReprocessRequired : HermesEffectiveGateDecision()

    /** Machine REVIEW_REQUIRED with no (or not yet a resolving) human decision on record. */
    data object HeldForReview : HermesEffectiveGateDecision()

    /** Machine FAILED with no human decision able to resolve it in this R0 scope. */
    data class ProcessingFailed(val failure: HermesProcessingFailure) : HermesEffectiveGateDecision()

    /** A human decision is on record that this R0 vocabulary does not permit for the current machine status (for example, ACCEPT against FAILED). */
    data object InvalidHumanDecision : HermesEffectiveGateDecision()
}

/**
 * Hermes Exception Decision Backend, Task 4. A small, pure coordinator/helper -- never a general
 * policy engine (Task 4's own R0 scope). Precedence, exactly:
 *
 * ```text
 * human REJECT      -> BLOCK (HumanRejected), regardless of machine status
 * human REPROCESS   -> BLOCK (ReprocessRequired), regardless of machine status
 * machine PASS       -> ALLOW (human ACCEPT/CORRECT on top of an already-PASS result changes nothing)
 * machine REVIEW_REQUIRED + human ACCEPT or CORRECT -> ALLOW
 * machine REVIEW_REQUIRED + no decision              -> BLOCK (HeldForReview)
 * machine FAILED + no decision                        -> BLOCK (ProcessingFailed)
 * machine FAILED + human CORRECT                       -> BLOCK (ProcessingFailed) -- see below
 * machine FAILED + human ACCEPT                         -> InvalidHumanDecision (defensive; the
 *     sanctioned decision-recording path, HermesProcessingDecisionCoordinator, already refuses to
 *     record ACCEPT against a FAILED result at all, so this branch is unreachable through that
 *     path and exists only so a decision constructed by any other means still fails closed)
 * ```
 *
 * ## Why FAILED + CORRECT never allows in R0
 *
 * Every [parker.core.interfaces.HermesProcessingFailureKind] in the current closed vocabulary
 * (`UNSUPPORTED_FILE_FORMAT`, `ENCRYPTED_SOURCE`, `NO_READABLE_CONTENT`, `PROCESSOR_FAILURE`,
 * `CORRUPT_SOURCE`, `PROCESSING_TIMEOUT`, `REQUIRED_PROCESSOR_UNAVAILABLE`) is a whole-source,
 * byte/processing-level failure, never a per-issue content-interpretation uncertainty -- exactly
 * the category Task 4's own instructions name as never bypassable by a correction that does not
 * itself resolve the underlying processing problem. A [parker.core.interfaces.HermesProcessingCorrection]
 * can only ever restate Hermes's own *interpretation* of an existing
 * [parker.core.interfaces.HermesProcessingIssue] -- it cannot decrypt a source, repair corrupt
 * bytes, or supply a missing processor -- so no correction in this R0 vocabulary can truthfully
 * resolve a FAILED result. REPROCESS remains the correct, permitted path for every FAILED source.
 */
object HermesProcessingEffectiveGate {

    fun evaluate(result: HermesProcessingResult, latestDecision: HermesProcessingHumanDecision?): HermesEffectiveGateDecision {
        when (latestDecision?.decision) {
            HermesProcessingHumanDecisionType.REJECT -> return HermesEffectiveGateDecision.HumanRejected
            HermesProcessingHumanDecisionType.REPROCESS -> return HermesEffectiveGateDecision.ReprocessRequired
            else -> Unit
        }
        return when (result.status) {
            HermesProcessingStatus.PASS -> HermesEffectiveGateDecision.Allow
            HermesProcessingStatus.REVIEW_REQUIRED -> when (latestDecision?.decision) {
                HermesProcessingHumanDecisionType.ACCEPT, HermesProcessingHumanDecisionType.CORRECT -> HermesEffectiveGateDecision.Allow
                else -> HermesEffectiveGateDecision.HeldForReview
            }
            HermesProcessingStatus.FAILED -> when (latestDecision?.decision) {
                HermesProcessingHumanDecisionType.ACCEPT -> HermesEffectiveGateDecision.InvalidHumanDecision
                else -> HermesEffectiveGateDecision.ProcessingFailed(
                    result.failure ?: HermesProcessingFailure(parker.core.interfaces.HermesProcessingFailureKind.PROCESSOR_FAILURE),
                )
            }
        }
    }
}
