package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.HermesProcessingCorrection
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
import kotlin.test.assertTrue

/** Hermes Exception Decision Backend, Task 4. Pending-queue filtering logic in [HermesProcessingReviewProjection]. */
class HermesProcessingReviewProjectionTest {

    private val decidedBy = PrincipalId("owner-${"1".repeat(64)}")
    private val at: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val oneIssue = listOf(HermesProcessingIssue(HermesProcessingIssueKind.MISSING_CONTENT, "content missing"))

    private fun result(sha256: String, batchId: String, status: HermesProcessingStatus, failure: HermesProcessingFailure? = null) =
        HermesProcessingResult(sha256, batchId, status, setOf(HermesProcessingMethod.OCR), issues = if (status == HermesProcessingStatus.REVIEW_REQUIRED) oneIssue else emptyList(), failure = failure)

    private fun decision(batchId: String, sha256: String, type: HermesProcessingHumanDecisionType, correction: HermesProcessingCorrection? = null) =
        HermesProcessingHumanDecision(batchId, sha256, type, decidedBy, at, correction = correction)

    @Test
    fun `PASS never appears, REVIEW_REQUIRED and FAILED with no decision always appear`() = runTest {
        val results = InMemoryHermesProcessingResultRegistry()
        results.record(result("a".repeat(64), "bulk-x", HermesProcessingStatus.PASS))
        results.record(result("b".repeat(64), "bulk-x", HermesProcessingStatus.REVIEW_REQUIRED))
        results.record(result("c".repeat(64), "bulk-x", HermesProcessingStatus.FAILED, HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE)))
        val projection = HermesProcessingReviewProjection(results, InMemoryHermesProcessingDecisionRegistry())

        val items = projection.listPendingForOwner()

        assertEquals(setOf("b".repeat(64), "c".repeat(64)), items.map { it.sourceSha256 }.toSet())
    }

    @Test
    fun `REVIEW_REQUIRED resolved by ACCEPT or REJECT leaves the pending queue`() = runTest {
        val results = InMemoryHermesProcessingResultRegistry()
        val decisions = InMemoryHermesProcessingDecisionRegistry()
        results.record(result("a".repeat(64), "bulk-x", HermesProcessingStatus.REVIEW_REQUIRED))
        results.record(result("b".repeat(64), "bulk-x", HermesProcessingStatus.REVIEW_REQUIRED))
        decisions.record(decision("bulk-x", "a".repeat(64), HermesProcessingHumanDecisionType.ACCEPT))
        decisions.record(decision("bulk-x", "b".repeat(64), HermesProcessingHumanDecisionType.REJECT))
        val projection = HermesProcessingReviewProjection(results, decisions)

        assertEquals(emptyList(), projection.listPendingForOwner())
    }

    @Test
    fun `REVIEW_REQUIRED resolved by a valid CORRECT leaves the pending queue`() = runTest {
        val results = InMemoryHermesProcessingResultRegistry()
        val decisions = InMemoryHermesProcessingDecisionRegistry()
        results.record(result("a".repeat(64), "bulk-x", HermesProcessingStatus.REVIEW_REQUIRED))
        decisions.record(decision("bulk-x", "a".repeat(64), HermesProcessingHumanDecisionType.CORRECT, HermesProcessingCorrection(0, "corrected", "reason")))
        val projection = HermesProcessingReviewProjection(results, decisions)

        assertEquals(emptyList(), projection.listPendingForOwner())
    }

    @Test
    fun `REPROCESS remains visible for both REVIEW_REQUIRED and FAILED`() = runTest {
        val results = InMemoryHermesProcessingResultRegistry()
        val decisions = InMemoryHermesProcessingDecisionRegistry()
        results.record(result("a".repeat(64), "bulk-x", HermesProcessingStatus.REVIEW_REQUIRED))
        results.record(result("b".repeat(64), "bulk-x", HermesProcessingStatus.FAILED, HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE)))
        decisions.record(decision("bulk-x", "a".repeat(64), HermesProcessingHumanDecisionType.REPROCESS))
        decisions.record(decision("bulk-x", "b".repeat(64), HermesProcessingHumanDecisionType.REPROCESS))
        val projection = HermesProcessingReviewProjection(results, decisions)

        assertEquals(setOf("a".repeat(64), "b".repeat(64)), projection.listPendingForOwner().map { it.sourceSha256 }.toSet())
    }

    @Test
    fun `FAILED corrected by a CORRECT decision remains visible -- the correction did not resolve it`() = runTest {
        val results = InMemoryHermesProcessingResultRegistry()
        val decisions = InMemoryHermesProcessingDecisionRegistry()
        val failedWithIssue = HermesProcessingResult(
            "a".repeat(64), "bulk-x", HermesProcessingStatus.FAILED, setOf(HermesProcessingMethod.OCR),
            issues = oneIssue, failure = HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE),
        )
        results.record(failedWithIssue)
        decisions.record(decision("bulk-x", "a".repeat(64), HermesProcessingHumanDecisionType.CORRECT, HermesProcessingCorrection(0, "corrected", "reason")))
        val projection = HermesProcessingReviewProjection(results, decisions)

        assertEquals(listOf("a".repeat(64)), projection.listPendingForOwner().map { it.sourceSha256 })
    }

    @Test
    fun `no cross-batch leakage in the pending list`() = runTest {
        val results = InMemoryHermesProcessingResultRegistry()
        results.record(result("a".repeat(64), "bulk-a", HermesProcessingStatus.REVIEW_REQUIRED))
        results.record(result("a".repeat(64), "bulk-b", HermesProcessingStatus.REVIEW_REQUIRED))
        val projection = HermesProcessingReviewProjection(results, InMemoryHermesProcessingDecisionRegistry())

        val items = projection.listPendingForOwner()

        assertEquals(2, items.size)
        assertEquals(setOf("bulk-a", "bulk-b"), items.map { it.batchId }.toSet())
    }

    @Test
    fun `case display name is resolved per batch via the supplied lookup`() = runTest {
        val results = InMemoryHermesProcessingResultRegistry()
        results.record(result("a".repeat(64), "bulk-a", HermesProcessingStatus.REVIEW_REQUIRED))
        val projection = HermesProcessingReviewProjection(results, InMemoryHermesProcessingDecisionRegistry()) { batchId ->
            if (batchId == "bulk-a") "Uber ERA 2026" else null
        }

        val items = projection.listPendingForOwner()

        assertEquals("Uber ERA 2026", items.single().caseDisplayName)
    }

    @Test
    fun `each pending item carries the latest decision when one exists`() = runTest {
        val results = InMemoryHermesProcessingResultRegistry()
        val decisions = InMemoryHermesProcessingDecisionRegistry()
        results.record(result("a".repeat(64), "bulk-x", HermesProcessingStatus.FAILED, HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE)))
        val recorded = decision("bulk-x", "a".repeat(64), HermesProcessingHumanDecisionType.REPROCESS)
        decisions.record(recorded)
        val projection = HermesProcessingReviewProjection(results, decisions)

        assertEquals(recorded, projection.listPendingForOwner().single().latestDecision)
    }
}
