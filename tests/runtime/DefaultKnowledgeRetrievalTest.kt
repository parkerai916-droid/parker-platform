package parker.core.runtime

import java.time.Instant
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeRetrievalDisposition
import parker.core.interfaces.KnowledgeRetrievalQuery
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Programme 3, Knowledge Memory, Implementation Unit 9.2 (Deterministic
 * Retrieval Engine). Behavioural and structural tests for
 * [DefaultKnowledgeRetrieval] -- see `docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_COMPLETION_REVIEW.md`
 * for the design decisions this suite verifies. This suite does not
 * exercise staleness detection, retirement/supersession default policy,
 * permission enforcement, or runtime composition -- none of that is
 * implemented by this Unit, and none of it is exercised here.
 */
class DefaultKnowledgeRetrievalTest {

    private val principal = PrincipalId("owner-1")

    private fun item(
        knowledgeId: KnowledgeId,
        basis: String,
        history: List<KnowledgePromotion>? = null,
    ): KnowledgeItem {
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(
            parker.core.interfaces.AssertionId("assertion-${knowledgeId.value}"),
        )
        val defaultHistory = listOf(
            KnowledgePromotion(
                knowledgeId = knowledgeId,
                evidenceReference = evidenceReference,
                resultingState = EvidentialState.UNKNOWN,
                occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
                basis = basis,
            ),
        )
        return KnowledgeItem(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            provenanceReference = ProvenanceReference(ProvenanceId("prov-${knowledgeId.value}")),
            evidentialState = EvidentialState.UNKNOWN,
            status = KnowledgeItemStatus.ACTIVE,
            history = history ?: defaultHistory,
        )
    }

    private fun query(relevance: String, maximumResults: Int = 10) = KnowledgeRetrievalQuery(
        relevance = relevance,
        correlationId = "corr-1",
        maximumResults = maximumResults,
    )

    // --- Matching ---

    @Test
    fun `an item whose most recent basis contains the relevance text is matched`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery shopping list preferences"))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(1, retrieved.result.entries.size)
        assertEquals(KnowledgeId("k1"), retrieved.result.entries[0].item.knowledgeId)
    }

    @Test
    fun `matching is case-insensitive`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "Grocery Shopping List"))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(1, retrieved.result.entries.size)
    }

    @Test
    fun `an item whose basis does not contain the relevance text is excluded`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated household task"))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(emptyList<Any>(), retrieved.result.entries)
    }

    @Test
    fun `matching uses the most recent history entry's basis, not an earlier one`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val originalPromotion = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
            basis = "grocery shopping list",
        )
        val revision = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-01-02T00:00:00Z"),
            basis = "updated household budget notes",
        )
        persistence.store(item(knowledgeId, basis = "unused", history = listOf(originalPromotion, revision)))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val groceryResult = retrieval.retrieve(principal, query(relevance = "grocery"))
        val budgetResult = retrieval.retrieve(principal, query(relevance = "budget"))

        assertEquals(emptyList<Any>(), assertIs<KnowledgeRetrievalDisposition.Retrieved>(groceryResult).result.entries)
        assertEquals(1, assertIs<KnowledgeRetrievalDisposition.Retrieved>(budgetResult).result.entries.size)
    }

    // --- Lifecycle status: currently unfiltered ---

    @Test
    fun `a RETIRED item is currently returned identically to an ACTIVE one -- this protects today's implementation from accidental change and does not establish Unit 9-4 policy`() = runTest {
        // This test documents Unit 9.2's own current, provisional behaviour: no lifecycle-status
        // filtering exists yet, so a RETIRED item matches, orders, and bounds exactly like an
        // ACTIVE one. This is the absence of a decision, not a considered "include retired items
        // by default" policy -- the Unit 9 Contract Design §6 reserves that actual decision to a
        // later Unit 9.4. This test exists only to guard today's unconditional-inclusion behaviour
        // against silent, accidental change in either direction; it must not be read as approving,
        // requiring, or predicting whatever default Unit 9.4 eventually adopts.
        val persistence = InMemoryKnowledgeItemPersistence()
        val retiredId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val promotion = KnowledgePromotion(
            knowledgeId = retiredId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
            basis = "grocery shopping list",
        )
        val retiredItem = KnowledgeItem(
            knowledgeId = retiredId,
            evidenceReference = evidenceReference,
            provenanceReference = ProvenanceReference(ProvenanceId("prov-k1")),
            evidentialState = EvidentialState.UNKNOWN,
            status = KnowledgeItemStatus.RETIRED,
            history = listOf(promotion),
        )
        persistence.store(retiredItem)
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(1, retrieved.result.entries.size)
        assertEquals(KnowledgeItemStatus.RETIRED, retrieved.result.entries[0].item.status)
    }

    // --- Empty result ---

    @Test
    fun `an empty persistence returns Retrieved with an empty result, never an error or denial`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "anything"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(emptyList<Any>(), retrieved.result.entries)
    }

    // --- Bounding ---

    @Test
    fun `maximumResults bounds the matched set without altering which items match`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list one"))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery list two"))
        persistence.store(item(KnowledgeId("k3"), basis = "grocery list three"))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery", maximumResults = 2))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(2, retrieved.result.entries.size)
        assertEquals(listOf(KnowledgeId("k1"), KnowledgeId("k2")), retrieved.result.entries.map { it.item.knowledgeId })
    }

    // --- Ordering / determinism ---

    @Test
    fun `matched entries preserve insertion order`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k3"), basis = "grocery third"))
        persistence.store(item(KnowledgeId("k1"), basis = "grocery first"))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery second"))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            listOf(KnowledgeId("k3"), KnowledgeId("k1"), KnowledgeId("k2")),
            retrieved.result.entries.map { it.item.knowledgeId },
        )
    }

    @Test
    fun `the same query against unchanged state returns an identical result across repeated calls`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list one"))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery list two"))
        val retrieval = DefaultKnowledgeRetrieval(persistence)
        val theQuery = query(relevance = "grocery")

        val first = retrieval.retrieve(principal, theQuery)
        val second = retrieval.retrieve(principal, theQuery)
        val third = retrieval.retrieve(principal, theQuery)

        assertEquals(first, second)
        assertEquals(second, third)
    }

    // --- Staleness placeholder ---

    @Test
    fun `every returned entry discloses stale as true -- a conservative placeholder, not a computation`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list"))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertTrue(retrieved.result.entries.all { it.stale })
    }

    // --- Permission: accepted, never consulted ---

    @Test
    fun `retrieve never returns NotAuthorised -- permission enforcement is not implemented by this Unit`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list"))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
    }

    @Test
    fun `different requesting principals receive identical results for the same query`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list"))
        val retrieval = DefaultKnowledgeRetrieval(persistence)
        val theQuery = query(relevance = "grocery")

        val first = retrieval.retrieve(PrincipalId("owner-a"), theQuery)
        val second = retrieval.retrieve(PrincipalId("owner-b"), theQuery)

        assertEquals(first, second)
    }

    // --- Structural: no Memory Core dependency, no ranking, no PermissionEngine dependency ---

    @Test
    fun `DefaultKnowledgeRetrieval holds exactly one constructor dependency -- KnowledgeItemPersistence`() {
        val constructor = requireNotNull(DefaultKnowledgeRetrieval::class.primaryConstructor)
        val parameterTypes = constructor.parameters.map { it.type.classifier }

        assertEquals(listOf(KnowledgeItemPersistence::class), parameterTypes)
    }

    @Test
    fun `no MemoryRetrieval, MemoryCore, or PermissionEngine dependency exists anywhere on DefaultKnowledgeRetrieval`() {
        val declaredProperties = DefaultKnowledgeRetrieval::class.declaredMemberProperties.map { it.name.lowercase() }
        listOf("memoryretrieval", "memorycore", "permissionengine").forEach { forbidden ->
            assertFalse(
                declaredProperties.any { it.contains(forbidden) },
                "DefaultKnowledgeRetrieval must not declare a property resembling '$forbidden' -- found: $declaredProperties",
            )
        }
    }

    @Test
    fun `DefaultKnowledgeRetrieval declares exactly one public domain operation -- retrieve`() {
        val publicFunctions = DefaultKnowledgeRetrieval::class.declaredFunctions
            .filter { it.visibility == KVisibility.PUBLIC }
            .map { it.name }
            .toSet()

        assertEquals(setOf("retrieve"), publicFunctions, "found: $publicFunctions")
    }

    @Test
    fun `no ranking, scoring, or weighting method exists anywhere on DefaultKnowledgeRetrieval`() {
        val declaredNames = (DefaultKnowledgeRetrieval::class.declaredFunctions.map { it.name } +
            DefaultKnowledgeRetrieval::class.declaredMemberProperties.map { it.name }).map { it.lowercase() }
        listOf("rank", "score", "weight", "embedding", "vector", "semantic", "similarity").forEach { forbidden ->
            assertFalse(
                declaredNames.any { it.contains(forbidden) },
                "DefaultKnowledgeRetrieval must not declare '$forbidden' -- found: $declaredNames",
            )
        }
    }
}
