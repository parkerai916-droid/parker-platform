package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Sprint 4, Track A, Unit A3. Construction-time validation tests for the
 * field-level Memory contracts `docs/architecture/MEMORY_CONTRACT_DESIGN.md`
 * approved: [KnowledgeId], [CandidateKnowledge], [KnowledgeRecord], [KnowledgeQuery],
 * and [KnowledgePromotionDecision]. Behavioural tests of [KnowledgeStore] and
 * [KnowledgePromotionPolicy] live in `tests/runtime/InMemoryMemoryStoreTest.kt`
 * and `tests/runtime/DefaultMemoryPromotionPolicyTest.kt` instead --
 * this file is pure data-shape validation, mirroring `IdentifiersTest.kt`'s
 * own scope.
 */
class MemoryContractsTest {

    // --- KnowledgeId ---

    @Test
    fun `KnowledgeId with equal values are equal`() {
        assertEquals(KnowledgeId("memory-1"), KnowledgeId("memory-1"))
    }

    @Test
    fun `a blank KnowledgeId is rejected at construction`() {
        assertFailsWith<IllegalArgumentException> { KnowledgeId("") }
        assertFailsWith<IllegalArgumentException> { KnowledgeId("   ") }
    }

    // --- CandidateKnowledge ---

    private fun candidate(
        knowledgePayload: String = "the user prefers window seats",
        confidence: Double? = null,
        correlationId: String = "corr-1",
        sourceSubsystem: String = "test-harness",
    ) = CandidateKnowledge(
        knowledgePayload = knowledgePayload,
        proposedCategory = KnowledgeCategory.SEMANTIC,
        sourceSubsystem = sourceSubsystem,
        correlationId = correlationId,
        confidence = confidence,
    )

    @Test
    fun `a CandidateKnowledge with a blank knowledgePayload is rejected`() {
        assertFailsWith<IllegalArgumentException> { candidate(knowledgePayload = "") }
    }

    @Test
    fun `a CandidateKnowledge with a blank sourceSubsystem is rejected`() {
        assertFailsWith<IllegalArgumentException> { candidate(sourceSubsystem = "") }
    }

    @Test
    fun `a CandidateKnowledge with a blank correlationId is rejected`() {
        assertFailsWith<IllegalArgumentException> { candidate(correlationId = "") }
    }

    @Test
    fun `a CandidateKnowledge confidence outside 0-0 to 1-0 is rejected`() {
        assertFailsWith<IllegalArgumentException> { candidate(confidence = 1.5) }
        assertFailsWith<IllegalArgumentException> { candidate(confidence = -0.1) }
    }

    @Test
    fun `a CandidateKnowledge with a valid confidence at each boundary is accepted`() {
        candidate(confidence = 0.0)
        candidate(confidence = 1.0)
    }

    @Test
    fun `a CandidateKnowledge carries every KnowledgeCategory value without error`() {
        KnowledgeCategory.entries.forEach { category ->
            CandidateKnowledge(
                knowledgePayload = "payload",
                proposedCategory = category,
                sourceSubsystem = "test-harness",
                correlationId = "corr-1",
            )
        }
    }

    // --- KnowledgeRecord ---

    private fun record(
        knowledgePayload: String = "the user prefers window seats",
        sourceSubsystem: String = "test-harness",
        correlationId: String = "corr-1",
        confidence: Double? = null,
    ) = KnowledgeRecord(
        memoryId = KnowledgeId("memory-1"),
        category = KnowledgeCategory.SEMANTIC,
        sourceSubsystem = sourceSubsystem,
        correlationId = correlationId,
        promotedAt = Instant.parse("2026-01-01T00:00:00Z"),
        knowledgePayload = knowledgePayload,
        confidence = confidence,
    )

    @Test
    fun `a KnowledgeRecord with a blank knowledgePayload is rejected`() {
        assertFailsWith<IllegalArgumentException> { record(knowledgePayload = "") }
    }

    @Test
    fun `a KnowledgeRecord with a blank sourceSubsystem is rejected`() {
        assertFailsWith<IllegalArgumentException> { record(sourceSubsystem = "") }
    }

    @Test
    fun `a KnowledgeRecord with a blank correlationId is rejected`() {
        assertFailsWith<IllegalArgumentException> { record(correlationId = "") }
    }

    @Test
    fun `a KnowledgeRecord confidence outside 0-0 to 1-0 is rejected`() {
        assertFailsWith<IllegalArgumentException> { record(confidence = 2.0) }
    }

    @Test
    fun `a valid KnowledgeRecord carries every KnowledgeCategory value without error`() {
        KnowledgeCategory.entries.forEach { category ->
            KnowledgeRecord(
                memoryId = KnowledgeId("memory-1"),
                category = category,
                sourceSubsystem = "test-harness",
                correlationId = "corr-1",
                promotedAt = Instant.parse("2026-01-01T00:00:00Z"),
                knowledgePayload = "payload",
            )
        }
    }

    // --- KnowledgeQuery ---

    private fun query(
        relevance: String = "window seats",
        correlationId: String = "corr-1",
        maximumResults: Int = 10,
    ) = KnowledgeQuery(
        requestingPrincipalId = PrincipalId("user-1"),
        relevance = relevance,
        correlationId = correlationId,
        maximumResults = maximumResults,
    )

    @Test
    fun `a KnowledgeQuery with blank relevance is rejected`() {
        assertFailsWith<IllegalArgumentException> { query(relevance = "") }
    }

    @Test
    fun `a KnowledgeQuery with a blank correlationId is rejected`() {
        assertFailsWith<IllegalArgumentException> { query(correlationId = "") }
    }

    @Test
    fun `a KnowledgeQuery with a non-positive maximumResults is rejected`() {
        assertFailsWith<IllegalArgumentException> { query(maximumResults = 0) }
        assertFailsWith<IllegalArgumentException> { query(maximumResults = -1) }
    }

    @Test
    fun `a KnowledgeQuery with maximumResults of exactly 1 is accepted`() {
        query(maximumResults = 1)
    }

    // --- KnowledgePromotionDecision ---

    @Test
    fun `KnowledgePromotionDecision Reject with a blank reason is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            KnowledgePromotionDecision.Reject(KnowledgeId("memory-1"), "")
        }
    }

    @Test
    fun `KnowledgePromotionDecision Promote and Reject both expose the same memoryId field`() {
        val id = KnowledgeId("memory-1")
        val promote: KnowledgePromotionDecision = KnowledgePromotionDecision.Promote(id, KnowledgeCategory.EPISODIC)
        val reject: KnowledgePromotionDecision = KnowledgePromotionDecision.Reject(id, "not viable")

        assertEquals(id, promote.memoryId)
        assertEquals(id, reject.memoryId)
    }
}
