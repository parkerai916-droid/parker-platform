package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateMemory
import parker.core.interfaces.MemoryCategory
import parker.core.interfaces.MemoryId
import parker.core.interfaces.MemoryPromotionDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 4, Track A, Unit A3. Unit tests of [DefaultMemoryPromotionPolicy]'s
 * own two-rule evaluation, stated in full in that class's KDoc: explicit
 * request promotes unconditionally; otherwise confidence at or above the
 * threshold promotes; otherwise the candidate is rejected with a
 * plain-language reason. Deliberately does not test repetition,
 * (non-explicit) user importance, goal relevance, or frequency -- this
 * policy does not implement them, per its own KDoc and
 * `docs/architecture/IMPLEMENTATION_GAPS.md`.
 */
class DefaultMemoryPromotionPolicyTest {

    private val policy = DefaultMemoryPromotionPolicy()
    private val memoryId = MemoryId("memory-1")

    private fun candidate(
        confidence: Double? = null,
        explicitlyRequested: Boolean = false,
        category: MemoryCategory = MemoryCategory.SEMANTIC,
    ) = CandidateMemory(
        knowledgePayload = "the user prefers window seats",
        proposedCategory = category,
        sourceSubsystem = "test-harness",
        correlationId = "corr-1",
        confidence = confidence,
        explicitlyRequested = explicitlyRequested,
    )

    // --- explicit-request promotion factor ---

    @Test
    fun `an explicitly requested candidate is promoted even with no confidence figure at all`() = runTest {
        val result = policy.evaluate(candidate(confidence = null, explicitlyRequested = true), memoryId)

        val promote = assertIs<MemoryPromotionDecision.Promote>(result)
        assertEquals(memoryId, promote.memoryId)
    }

    @Test
    fun `an explicitly requested candidate is promoted even with confidence far below the threshold`() = runTest {
        val result = policy.evaluate(candidate(confidence = 0.01, explicitlyRequested = true), memoryId)

        assertIs<MemoryPromotionDecision.Promote>(result)
    }

    // --- confidence-based promotion factor ---

    @Test
    fun `a non-explicit candidate at or above the confidence threshold is promoted`() = runTest {
        val result = policy.evaluate(
            candidate(confidence = DefaultMemoryPromotionPolicy.DEFAULT_CONFIDENCE_THRESHOLD, explicitlyRequested = false),
            memoryId,
        )

        assertIs<MemoryPromotionDecision.Promote>(result)
    }

    @Test
    fun `a non-explicit candidate comfortably above the confidence threshold is promoted`() = runTest {
        val result = policy.evaluate(candidate(confidence = 0.95, explicitlyRequested = false), memoryId)

        assertIs<MemoryPromotionDecision.Promote>(result)
    }

    @Test
    fun `the confirmed category on Promote is the candidate's own proposed category`() = runTest {
        val result = policy.evaluate(
            candidate(explicitlyRequested = true, category = MemoryCategory.EPISODIC),
            memoryId,
        )

        val promote = assertIs<MemoryPromotionDecision.Promote>(result)
        assertEquals(MemoryCategory.EPISODIC, promote.category)
    }

    // --- rejection ---

    @Test
    fun `a non-explicit candidate just below the confidence threshold is rejected`() = runTest {
        val result = policy.evaluate(
            candidate(confidence = DefaultMemoryPromotionPolicy.DEFAULT_CONFIDENCE_THRESHOLD - 0.01, explicitlyRequested = false),
            memoryId,
        )

        val reject = assertIs<MemoryPromotionDecision.Reject>(result)
        assertEquals(memoryId, reject.memoryId)
        assertTrue(reject.reason.isNotBlank())
    }

    @Test
    fun `a non-explicit candidate with no confidence figure at all is rejected`() = runTest {
        val result = policy.evaluate(candidate(confidence = null, explicitlyRequested = false), memoryId)

        val reject = assertIs<MemoryPromotionDecision.Reject>(result)
        assertTrue(reject.reason.contains("absent", ignoreCase = true))
    }

    @Test
    fun `rejection reason is never a bare numeric score`() = runTest {
        val result = policy.evaluate(candidate(confidence = 0.1, explicitlyRequested = false), memoryId)

        val reject = assertIs<MemoryPromotionDecision.Reject>(result)
        assertTrue(reject.reason.toDoubleOrNull() == null, "reason '${reject.reason}' must not be a bare numeric score")
    }

    // --- determinism ---

    @Test
    fun `evaluating the same candidate twice yields an identical decision`() = runTest {
        val input = candidate(confidence = 0.4, explicitlyRequested = false)

        val first = policy.evaluate(input, memoryId)
        val second = policy.evaluate(input, memoryId)

        assertEquals(first, second)
    }

    @Test
    fun `a custom confidence threshold is honoured`() = runTest {
        val lenientPolicy = DefaultMemoryPromotionPolicy(confidenceThreshold = 0.2)

        val result = lenientPolicy.evaluate(candidate(confidence = 0.25, explicitlyRequested = false), memoryId)

        assertIs<MemoryPromotionDecision.Promote>(result)
    }
}
