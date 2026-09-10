package parker.core.interfaces

import java.time.Instant

/**
 * Hermes Exception Decision Backend, Task 4. The complete, closed vocabulary of Owner/Steve
 * decisions against one pre-ingestion [HermesProcessingResult] exception. Deliberately distinct
 * from `SteveReviewQueueStatus`/`DerivativeReviewState`/`HumanFidelityReviewState` -- those govern
 * the later, post-ingestion Parker fidelity/review lifecycle stage; this enum governs only the
 * earlier, pre-ingestion "does this Hermes processing result proceed to governed ingestion at
 * all" decision (see [HermesProcessingResult]'s own KDoc for that lifecycle boundary, which this
 * file's types sit on the same side of).
 */
enum class HermesProcessingHumanDecisionType {
    /** Steve confirms the currently stored [HermesProcessingResult] is acceptable to proceed. */
    ACCEPT,

    /** Steve supplies a bounded correction sufficient to resolve one identified processing issue. */
    CORRECT,

    /** Steve marks the source as requiring Hermes processing again. Records intent only -- never invokes Hermes. */
    REPROCESS,

    /** Steve explicitly blocks the source from governed ingestion through the sanctioned Hermes path. */
    REJECT,
}

/**
 * A bounded correction to exactly one already-recorded [HermesProcessingIssue] within a stored
 * [HermesProcessingResult]. [issueIndex] is a zero-based index into that result's own
 * [HermesProcessingResult.issues] list -- deliberately not a fabricated, independently-addressed
 * issue identifier (Task 4's own R0 scope: "no existing stable issue identifier exists; use a
 * bounded index/reference within the stored `HermesProcessingResult` rather than inventing a
 * giant addressing scheme"). Never carries or implies a change to source bytes, batch identity, or
 * source identity -- it corrects Hermes's own recorded *interpretation* of already-fixed content,
 * nothing else.
 */
data class HermesProcessingCorrection(
    val issueIndex: Int,
    val correctedInterpretation: String,
    val reason: String,
) {
    init {
        require(issueIndex >= 0) { "HermesProcessingCorrection.issueIndex must be zero-based and non-negative" }
        require(correctedInterpretation.isNotBlank() && correctedInterpretation.length <= MAX_HERMES_DECISION_TEXT_CHARACTERS) {
            "HermesProcessingCorrection.correctedInterpretation must contain 1..$MAX_HERMES_DECISION_TEXT_CHARACTERS characters"
        }
        require(reason.isNotBlank() && reason.length <= MAX_HERMES_DECISION_TEXT_CHARACTERS) {
            "HermesProcessingCorrection.reason must contain 1..$MAX_HERMES_DECISION_TEXT_CHARACTERS characters"
        }
    }
}

/**
 * Hermes Exception Decision Backend, Task 4. One immutable, auditable Owner decision fact against
 * one pre-ingestion Hermes exception, keyed by the same (`batchId`, `sourceSha256`) pair
 * [HermesProcessingResult] itself is keyed by. Never mutates the [HermesProcessingResult] it
 * decides against -- machine processing status and human decision are always two separate,
 * independently-retained facts (see `HermesProcessingDecisionRegistry`'s own KDoc for the
 * append-only history this immutability requires in storage).
 *
 * [decidedBy] reuses Parker's own existing [PrincipalId] -- never a free-form username string --
 * and is always the Owner principal in production (structurally enforced by
 * `ParkerRuntime.recordHermesProcessingDecisionAsOwner`'s own "no caller-supplied principal"
 * shape, mirroring `ParkerRuntime.deleteEvidenceAsOwner`'s established precedent).
 */
data class HermesProcessingHumanDecision(
    val batchId: String,
    val sourceSha256: String,
    val decision: HermesProcessingHumanDecisionType,
    val decidedBy: PrincipalId,
    val decidedAt: Instant,
    val reason: String? = null,
    val correction: HermesProcessingCorrection? = null,
) {
    init {
        require(batchId.isNotBlank()) { "HermesProcessingHumanDecision.batchId must not be blank" }
        require(sourceSha256.matches(HERMES_DECISION_SHA256_PATTERN)) {
            "HermesProcessingHumanDecision.sourceSha256 must be 64 lowercase hexadecimal characters"
        }
        require(reason == null || (reason.isNotBlank() && reason.length <= MAX_HERMES_DECISION_TEXT_CHARACTERS)) {
            "HermesProcessingHumanDecision.reason must be absent or contain 1..$MAX_HERMES_DECISION_TEXT_CHARACTERS characters"
        }
        require((decision == HermesProcessingHumanDecisionType.CORRECT) == (correction != null)) {
            "HermesProcessingHumanDecision.correction must be supplied if and only if decision is CORRECT"
        }
    }
}

private const val MAX_HERMES_DECISION_TEXT_CHARACTERS = 4_096
private val HERMES_DECISION_SHA256_PATTERN = Regex("^[0-9a-f]{64}$")
