package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Sprint 4, Track A, Unit A3. Construction-time validation tests for the
 * field-level Memory contracts `docs/architecture/MEMORY_CONTRACT_DESIGN.md`
 * approved: [MemoryId], [CandidateMemory], [MemoryRecord], [MemoryQuery],
 * and [MemoryPromotionDecision]. Behavioural tests of [MemoryStore] and
 * [MemoryPromotionPolicy] live in `tests/runtime/InMemoryMemoryStoreTest.kt`
 * and `tests/runtime/DefaultMemoryPromotionPolicyTest.kt` instead --
 * this file is pure data-shape validation, mirroring `IdentifiersTest.kt`'s
 * own scope.
 */
class MemoryContractsTest {

    // --- MemoryId ---

    @Test
    fun `MemoryId with equal values are equal`() {
        assertEquals(MemoryId("memory-1"), MemoryId("memory-1"))
    }

    @Test
    fun `a blank MemoryId is rejected at construction`() {
        assertFailsWith<IllegalArgumentException> { MemoryId("") }
        assertFailsWith<IllegalArgumentException> { MemoryId("   ") }
    }

    // --- CandidateMemory ---

    private fun candidate(
        knowledgePayload: String = "the user prefers window seats",
        confidence: Double? = null,
        correlationId: String = "corr-1",
        sourceSubsystem: String = "test-harness",
    ) = CandidateMemory(
        knowledgePayload = knowledgePayload,
        proposedCategory = MemoryCategory.SEMANTIC,
        sourceSubsystem = sourceSubsystem,
        correlationId = correlationId,
        confidence = confidence,
    )

    @Test
    fun `a CandidateMemory with a blank knowledgePayload is rejected`() {
        assertFailsWith<IllegalArgumentException> { candidate(knowledgePayload = "") }
    }

    @Test
    fun `a CandidateMemory with a blank sourceSubsystem is rejected`() {
        assertFailsWith<IllegalArgumentException> { candidate(sourceSubsystem = "") }
    }

    @Test
    fun `a CandidateMemory with a blank correlationId is rejected`() {
        assertFailsWith<IllegalArgumentException> { candidate(correlationId = "") }
    }

    @Test
    fun `a CandidateMemory confidence outside 0-0 to 1-0 is rejected`() {
        assertFailsWith<IllegalArgumentException> { candidate(confidence = 1.5) }
        assertFailsWith<IllegalArgumentException> { candidate(confidence = -0.1) }
    }

    @Test
    fun `a CandidateMemory with a valid confidence at each boundary is accepted`() {
        candidate(confidence = 0.0)
        candidate(confidence = 1.0)
    }

    @Test
    fun `a CandidateMemory carries every MemoryCategory value without error`() {
        MemoryCategory.entries.forEach { category ->
            CandidateMemory(
                knowledgePayload = "payload",
                proposedCategory = category,
                sourceSubsystem = "test-harness",
                correlationId = "corr-1",
            )
        }
    }

    // --- MemoryRecord ---

    private fun record(
        knowledgePayload: String = "the user prefers window seats",
        sourceSubsystem: String = "test-harness",
        correlationId: String = "corr-1",
        confidence: Double? = null,
    ) = MemoryRecord(
        memoryId = MemoryId("memory-1"),
        category = MemoryCategory.SEMANTIC,
        sourceSubsystem = sourceSubsystem,
        correlationId = correlationId,
        promotedAt = Instant.parse("2026-01-01T00:00:00Z"),
        knowledgePayload = knowledgePayload,
        confidence = confidence,
    )

    @Test
    fun `a MemoryRecord with a blank knowledgePayload is rejected`() {
        assertFailsWith<IllegalArgumentException> { record(knowledgePayload = "") }
    }

    @Test
    fun `a MemoryRecord with a blank sourceSubsystem is rejected`() {
        assertFailsWith<IllegalArgumentException> { record(sourceSubsystem = "") }
    }

    @Test
    fun `a MemoryRecord with a blank correlationId is rejected`() {
        assertFailsWith<IllegalArgumentException> { record(correlationId = "") }
    }

    @Test
    fun `a MemoryRecord confidence outside 0-0 to 1-0 is rejected`() {
        assertFailsWith<IllegalArgumentException> { record(confidence = 2.0) }
    }

    @Test
    fun `a valid MemoryRecord carries every MemoryCategory value without error`() {
        MemoryCategory.entries.forEach { category ->
            MemoryRecord(
                memoryId = MemoryId("memory-1"),
                category = category,
                sourceSubsystem = "test-harness",
                correlationId = "corr-1",
                promotedAt = Instant.parse("2026-01-01T00:00:00Z"),
                knowledgePayload = "payload",
            )
        }
    }

    // --- MemoryQuery ---

    private fun query(
        relevance: String = "window seats",
        correlationId: String = "corr-1",
        maximumResults: Int = 10,
    ) = MemoryQuery(
        requestingPrincipalId = PrincipalId("user-1"),
        relevance = relevance,
        correlationId = correlationId,
        maximumResults = maximumResults,
    )

    @Test
    fun `a MemoryQuery with blank relevance is rejected`() {
        assertFailsWith<IllegalArgumentException> { query(relevance = "") }
    }

    @Test
    fun `a MemoryQuery with a blank correlationId is rejected`() {
        assertFailsWith<IllegalArgumentException> { query(correlationId = "") }
    }

    @Test
    fun `a MemoryQuery with a non-positive maximumResults is rejected`() {
        assertFailsWith<IllegalArgumentException> { query(maximumResults = 0) }
        assertFailsWith<IllegalArgumentException> { query(maximumResults = -1) }
    }

    @Test
    fun `a MemoryQuery with maximumResults of exactly 1 is accepted`() {
        query(maximumResults = 1)
    }

    // --- MemoryPromotionDecision ---

    @Test
    fun `MemoryPromotionDecision Reject with a blank reason is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            MemoryPromotionDecision.Reject(MemoryId("memory-1"), "")
        }
    }

    @Test
    fun `MemoryPromotionDecision Promote and Reject both expose the same memoryId field`() {
        val id = MemoryId("memory-1")
        val promote: MemoryPromotionDecision = MemoryPromotionDecision.Promote(id, MemoryCategory.EPISODIC)
        val reject: MemoryPromotionDecision = MemoryPromotionDecision.Reject(id, "not viable")

        assertEquals(id, promote.memoryId)
        assertEquals(id, reject.memoryId)
    }
}
