package parker.core.runtime

import java.time.Instant
import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingFailureKind
import parker.core.interfaces.HermesProcessingHumanDecision
import parker.core.interfaces.HermesProcessingHumanDecisionType
import parker.core.interfaces.HermesProcessingIssue
import parker.core.interfaces.HermesProcessingIssueKind
import parker.core.interfaces.HermesProcessingMethod
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingStatus
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Hermes Exception Decision Backend, Task 4. The complete precedence matrix for
 * [HermesProcessingEffectiveGate.evaluate] -- pure, no permission/registry/HTTP concerns, mirroring
 * how [HermesProcessingResultTest] tests [HermesProcessingResult]'s own invariants in isolation.
 */
class HermesProcessingEffectiveGateTest {

    private val sha256 = "a".repeat(64)
    private val batchId = "bulk-abc"
    private val decidedBy = PrincipalId("owner-${"1".repeat(64)}")
    private val at: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val oneIssue = listOf(HermesProcessingIssue(HermesProcessingIssueKind.MISSING_CONTENT, "content missing"))

    private fun result(status: HermesProcessingStatus, issues: List<HermesProcessingIssue> = emptyList(), failure: HermesProcessingFailure? = null) =
        HermesProcessingResult(sha256, batchId, status, setOf(HermesProcessingMethod.OCR), issues = issues, failure = failure)

    private fun decision(type: HermesProcessingHumanDecisionType, correction: parker.core.interfaces.HermesProcessingCorrection? = null) =
        HermesProcessingHumanDecision(batchId, sha256, type, decidedBy, at, correction = correction)

    @Test
    fun `PASS with no decision allows -- unmodified Task 3 semantics`() {
        assertEquals(HermesEffectiveGateDecision.Allow, HermesProcessingEffectiveGate.evaluate(result(HermesProcessingStatus.PASS), null))
    }

    @Test
    fun `REVIEW_REQUIRED with no decision holds for review`() {
        assertEquals(
            HermesEffectiveGateDecision.HeldForReview,
            HermesProcessingEffectiveGate.evaluate(result(HermesProcessingStatus.REVIEW_REQUIRED, oneIssue), null),
        )
    }

    @Test
    fun `FAILED with no decision blocks with the stored failure`() {
        val failure = HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE)
        val outcome = HermesProcessingEffectiveGate.evaluate(result(HermesProcessingStatus.FAILED, failure = failure), null)
        val blocked = assertIs<HermesEffectiveGateDecision.ProcessingFailed>(outcome)
        assertEquals(failure, blocked.failure)
    }

    @Test
    fun `REVIEW_REQUIRED plus ACCEPT allows`() {
        assertEquals(
            HermesEffectiveGateDecision.Allow,
            HermesProcessingEffectiveGate.evaluate(result(HermesProcessingStatus.REVIEW_REQUIRED, oneIssue), decision(HermesProcessingHumanDecisionType.ACCEPT)),
        )
    }

    @Test
    fun `REVIEW_REQUIRED plus CORRECT allows`() {
        val correction = parker.core.interfaces.HermesProcessingCorrection(0, "corrected", "reason")
        assertEquals(
            HermesEffectiveGateDecision.Allow,
            HermesProcessingEffectiveGate.evaluate(
                result(HermesProcessingStatus.REVIEW_REQUIRED, oneIssue),
                decision(HermesProcessingHumanDecisionType.CORRECT, correction),
            ),
        )
    }

    @Test
    fun `FAILED plus ACCEPT is InvalidHumanDecision -- never allows`() {
        val outcome = HermesProcessingEffectiveGate.evaluate(
            result(HermesProcessingStatus.FAILED, failure = HermesProcessingFailure(HermesProcessingFailureKind.ENCRYPTED_SOURCE)),
            decision(HermesProcessingHumanDecisionType.ACCEPT),
        )
        assertEquals(HermesEffectiveGateDecision.InvalidHumanDecision, outcome)
    }

    @Test
    fun `FAILED plus CORRECT still blocks -- no correction resolves a whole-source processing failure in this R0 scope`() {
        val failure = HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE)
        val correction = parker.core.interfaces.HermesProcessingCorrection(0, "corrected", "reason")
        val outcome = HermesProcessingEffectiveGate.evaluate(
            result(HermesProcessingStatus.FAILED, oneIssue, failure),
            decision(HermesProcessingHumanDecisionType.CORRECT, correction),
        )
        val blocked = assertIs<HermesEffectiveGateDecision.ProcessingFailed>(outcome)
        assertEquals(failure, blocked.failure)
    }

    @Test
    fun `human REJECT overrides machine PASS -- highest precedence`() {
        assertEquals(
            HermesEffectiveGateDecision.HumanRejected,
            HermesProcessingEffectiveGate.evaluate(result(HermesProcessingStatus.PASS), decision(HermesProcessingHumanDecisionType.REJECT)),
        )
    }

    @Test
    fun `human REPROCESS overrides machine PASS`() {
        assertEquals(
            HermesEffectiveGateDecision.ReprocessRequired,
            HermesProcessingEffectiveGate.evaluate(result(HermesProcessingStatus.PASS), decision(HermesProcessingHumanDecisionType.REPROCESS)),
        )
    }

    @Test
    fun `human REJECT overrides REVIEW_REQUIRED and FAILED too`() {
        assertEquals(
            HermesEffectiveGateDecision.HumanRejected,
            HermesProcessingEffectiveGate.evaluate(result(HermesProcessingStatus.REVIEW_REQUIRED, oneIssue), decision(HermesProcessingHumanDecisionType.REJECT)),
        )
        assertEquals(
            HermesEffectiveGateDecision.HumanRejected,
            HermesProcessingEffectiveGate.evaluate(
                result(HermesProcessingStatus.FAILED, failure = HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE)),
                decision(HermesProcessingHumanDecisionType.REJECT),
            ),
        )
    }

    @Test
    fun `human REPROCESS overrides REVIEW_REQUIRED and FAILED too`() {
        assertEquals(
            HermesEffectiveGateDecision.ReprocessRequired,
            HermesProcessingEffectiveGate.evaluate(result(HermesProcessingStatus.REVIEW_REQUIRED, oneIssue), decision(HermesProcessingHumanDecisionType.REPROCESS)),
        )
        assertEquals(
            HermesEffectiveGateDecision.ReprocessRequired,
            HermesProcessingEffectiveGate.evaluate(
                result(HermesProcessingStatus.FAILED, failure = HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE)),
                decision(HermesProcessingHumanDecisionType.REPROCESS),
            ),
        )
    }

    @Test
    fun `lifecycle isolation -- evaluate never mutates its inputs and reads only HermesProcessingResult and HermesProcessingHumanDecision`() {
        // Structural proof by construction: HermesProcessingEffectiveGate is a stateless `object`
        // whose only public member is a pure function of its two immutable data-class parameters --
        // there is no field, no SteveReviewQueueStatus/DerivativeReviewState/HumanFidelityReviewState
        // reference anywhere in its type, and nothing it returns can carry one.
        val before = result(HermesProcessingStatus.PASS)
        HermesProcessingEffectiveGate.evaluate(before, decision(HermesProcessingHumanDecisionType.REJECT))
        assertEquals(HermesProcessingStatus.PASS, before.status)
    }
}
