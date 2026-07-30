package parker.core.runtime

import java.time.Instant
import parker.core.interfaces.AssertionId
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeRestoration
import parker.core.interfaces.KnowledgeRetirement
import parker.core.interfaces.KnowledgeRetirementEvaluation
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.3. Unit tests of
 * [DefaultKnowledgeRetirementEvaluator]'s own retirement-qualification
 * logic, stated in full in that class's KDoc and in
 * `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §8,
 * §15. Kept entirely separate from [DefaultKnowledgeRevisionEvaluatorTest]
 * -- revision (Unit 7.2) and retirement (Unit 7.3) are independent,
 * separately governed seams.
 *
 * Unlike [DefaultKnowledgeRevisionEvaluatorTest], no [InMemoryMemoryCore]
 * or any other [parker.core.interfaces.MemoryRetrieval] implementation is
 * constructed anywhere in this file -- [DefaultKnowledgeRetirementEvaluator]
 * takes no such dependency, by design (see its own KDoc), and these tests
 * exist in part to demonstrate that no such dependency is needed to
 * exercise its behaviour fully.
 */
class DefaultKnowledgeRetirementEvaluatorTest {

    private val knowledgeId = KnowledgeId("item-1")
    private val evidenceReference = MemoryCoreRecordReference.ToAssertion(AssertionId("assertion-1"))
    private val provenanceReference = ProvenanceReference(ProvenanceId("provenance-1"))
    private val evaluator = DefaultKnowledgeRetirementEvaluator()

    private val initialPromotion = KnowledgePromotion(
        knowledgeId = knowledgeId,
        evidenceReference = evidenceReference,
        resultingState = EvidentialState.UNKNOWN,
        occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
        basis = "initial promotion, fixed for test setup",
    )

    private fun activeItem(
        evidentialState: EvidentialState = EvidentialState.UNKNOWN,
        history: List<parker.core.interfaces.KnowledgeLifecycleEvent> = listOf(initialPromotion),
    ): KnowledgeItem = KnowledgeItem(
        knowledgeId = knowledgeId,
        evidenceReference = evidenceReference,
        provenanceReference = provenanceReference,
        evidentialState = evidentialState,
        status = KnowledgeItemStatus.ACTIVE,
        history = history,
    )

    private val retirementOccurredAt: Instant = Instant.parse("2026-06-01T00:00:00Z")
    private val retirementBasis = "re-evaluation concluded the underlying evidence no longer sufficiently supports this item"

    // --- Successful retirement ---

    @Test
    fun `an ACTIVE item is retired`() {
        // This test also stands as the demonstration, per Independent Static Review
        // Finding 2, that no MemoryRetrieval or other Memory Core dependency is
        // required to evaluate retirement: no InMemoryMemoryCore or MemoryRetrieval
        // implementation is constructed anywhere in this file, and
        // DefaultKnowledgeRetirementEvaluator() itself takes no constructor
        // arguments (see class declaration) -- a fact this or any other single test
        // body cannot itself assert, since it is a structural property of the
        // production class's signature and this file's own imports, not a runtime
        // outcome. The previous, separate test asserting only this same call and
        // result added no independent verification and has been folded in here.
        val item = activeItem()

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt)

        assertIs<KnowledgeRetirementEvaluation.Applied>(result)
        assertEquals(KnowledgeItemStatus.RETIRED, result.item.status)
    }

    @Test
    fun `exactly one retirement event is appended`() {
        val item = activeItem()

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertEquals(item.history.size + 1, result.item.history.size)
        assertIs<KnowledgeRetirement>(result.item.history.last())
    }

    @Test
    fun `existing history is preserved unchanged`() {
        val item = activeItem()

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertEquals(item.history, result.item.history.dropLast(1))
    }

    @Test
    fun `the returned Applied result surfaces the same retirement event appended to history`() {
        val item = activeItem()

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertSame(result.retirement, result.item.history.last())
    }

    @Test
    fun `KnowledgeId is preserved`() {
        val item = activeItem()

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertEquals(knowledgeId, result.item.knowledgeId)
        assertEquals(knowledgeId, result.retirement.knowledgeId)
    }

    @Test
    fun `evidenceReference is preserved`() {
        val item = activeItem()

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertEquals(evidenceReference, result.item.evidenceReference)
    }

    @Test
    fun `provenanceReference is preserved`() {
        val item = activeItem()

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertEquals(provenanceReference, result.item.provenanceReference)
    }

    @Test
    fun `EvidentialState is preserved, including COMPETING_EXPLANATIONS`() {
        val item = activeItem(evidentialState = EvidentialState.COMPETING_EXPLANATIONS)

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, result.item.evidentialState)
    }

    @Test
    fun `caller-supplied occurredAt is preserved on the retirement event`() {
        val item = activeItem()

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertEquals(retirementOccurredAt, result.retirement.occurredAt)
    }

    @Test
    fun `caller-supplied basis is preserved on the retirement event`() {
        val item = activeItem()

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertEquals(retirementBasis, result.retirement.basis)
    }

    // --- Rejection: not permitted from current status ---

    @Test
    fun `an already-RETIRED item is rejected, not silently no-op'd`() {
        val retiredItem = activeItem().copy(status = KnowledgeItemStatus.RETIRED)

        val result = evaluator.evaluate(retiredItem, retirementBasis, retirementOccurredAt)

        assertIs<KnowledgeRetirementEvaluation.NotPermittedFromCurrentStatus>(result)
        // Strengthened per Independent Static Review Finding 1: assert the disclosed
        // basis actually names the item's real current status, not merely that it is
        // non-blank -- the evaluator interpolates item.status into its own message,
        // so this is a real, checkable property, not an incidental implementation detail.
        assertTrue(result.basis.contains("RETIRED"))
        assertTrue(result.basis.contains("not ACTIVE"))
    }

    @Test
    fun `repeated retirement of an already-retired item is rejected every time, never treated as a new permitted event`() {
        val retiredItem = activeItem().copy(status = KnowledgeItemStatus.RETIRED)

        val firstAttempt = evaluator.evaluate(retiredItem, retirementBasis, retirementOccurredAt)
        val secondAttempt = evaluator.evaluate(retiredItem, retirementBasis, retirementOccurredAt)

        assertIs<KnowledgeRetirementEvaluation.NotPermittedFromCurrentStatus>(firstAttempt)
        assertIs<KnowledgeRetirementEvaluation.NotPermittedFromCurrentStatus>(secondAttempt)
        // Strengthened per Independent Static Review Finding 1: same content check as
        // the single-attempt test above, confirmed stable across repeated attempts.
        assertTrue(firstAttempt.basis.contains("RETIRED"))
        assertTrue(secondAttempt.basis.contains("RETIRED"))
    }

    @Test
    fun `retiring the evaluator's own real Applied output a second time is rejected, not just a synthetically-constructed RETIRED item`() {
        // Strengthened per Independent Static Review Finding 3: rather than only
        // testing rejection against a copy()-constructed RETIRED item, this chains
        // through an actual first evaluate() call's own Applied.item output before
        // attempting a second retirement against it -- the realistic sequence the
        // finding's own name implies.
        val item = activeItem()

        val firstEvaluation = evaluator.evaluate(item, retirementBasis, retirementOccurredAt)
        assertIs<KnowledgeRetirementEvaluation.Applied>(firstEvaluation)

        val secondEvaluation = evaluator.evaluate(firstEvaluation.item, retirementBasis, retirementOccurredAt)

        assertIs<KnowledgeRetirementEvaluation.NotPermittedFromCurrentStatus>(secondEvaluation)
        assertTrue(secondEvaluation.basis.contains("RETIRED"))
        // The first evaluation's own result is untouched by the second attempt.
        assertEquals(KnowledgeItemStatus.RETIRED, firstEvaluation.item.status)
        assertEquals(1, firstEvaluation.item.history.count { it is KnowledgeRetirement })
    }

    @Test
    fun `rejecting an already-retired item appends no event and leaves history untouched`() {
        val retiredItem = activeItem().copy(status = KnowledgeItemStatus.RETIRED)

        val result = evaluator.evaluate(retiredItem, retirementBasis, retirementOccurredAt)

        assertIs<KnowledgeRetirementEvaluation.NotPermittedFromCurrentStatus>(result)
        assertEquals(1, retiredItem.history.size)
    }

    // --- Determinism ---

    @Test
    fun `evaluation is deterministic for identical inputs`() {
        val item = activeItem()

        val first = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied
        val second = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertEquals(first.item, second.item)
        assertEquals(first.retirement, second.retirement)
    }

    // --- No Memory Core dependency ---
    //
    // See `an ACTIVE item is retired`, above -- folded there per Independent Static
    // Review Finding 2, since a separate test body asserting the identical call and
    // result added no independent verification.

    // --- No persistence ---

    @Test
    fun `evaluation performs no persistence -- the caller's own prior KnowledgeItem reference is unaffected`() {
        val item = activeItem()

        evaluator.evaluate(item, retirementBasis, retirementOccurredAt)

        assertEquals(KnowledgeItemStatus.ACTIVE, item.status)
        assertEquals(1, item.history.size)
    }

    // --- Retirement distinct from revision ---

    @Test
    fun `retirement never appends a KnowledgePromotion and never changes evidenceReference or evidentialState`() {
        val item = activeItem(evidentialState = EvidentialState.UNKNOWN)

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertIs<KnowledgeRetirement>(result.item.history.last())
        assertEquals(item.evidenceReference, result.item.evidenceReference)
        assertEquals(item.evidentialState, result.item.evidentialState)
    }

    // --- Retirement distinct from restoration ---

    @Test
    fun `retirement never constructs a KnowledgeRestoration and never transitions RETIRED to ACTIVE`() {
        val item = activeItem()

        val result = evaluator.evaluate(item, retirementBasis, retirementOccurredAt) as KnowledgeRetirementEvaluation.Applied

        assertTrue(result.item.history.none { it is KnowledgeRestoration })
        assertEquals(KnowledgeItemStatus.RETIRED, result.item.status)
    }
}
