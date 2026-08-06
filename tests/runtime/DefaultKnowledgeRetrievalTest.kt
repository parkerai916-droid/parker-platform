package parker.core.runtime

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgeLifecycleEvent
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeRestoration
import parker.core.interfaces.KnowledgeRetirement
import parker.core.interfaces.KnowledgeRetrievalDisposition
import parker.core.interfaces.KnowledgeRetrievalQuery
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.StalenessDisclosure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Programme 3, Knowledge Memory, Implementation Units 9.2 (Deterministic
 * Retrieval Engine), 9.3 (Staleness Disclosure), and 9.4 (Retirement and
 * Supersession Retrieval-Shape Decision). Behavioural and structural
 * tests for [DefaultKnowledgeRetrieval] -- see
 * `docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_COMPLETION_REVIEW.md`,
 * `docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_COMPLETION_REVIEW.md`,
 * and `docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_COMPLETION_REVIEW.md`
 * for the design decisions this suite verifies. This suite does not
 * exercise permission enforcement or runtime composition -- neither is
 * implemented by any of these three Units, and neither is exercised here.
 */
class DefaultKnowledgeRetrievalTest {

    private val principal = PrincipalId("owner-1")

    /** A fixed instant every "fresh, just classified" fixture is anchored to. */
    private val now = Instant.parse("2026-06-15T12:00:00Z")
    private val fixedClock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    private fun item(
        knowledgeId: KnowledgeId,
        basis: String,
        history: List<KnowledgeLifecycleEvent>? = null,
        occurredAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        status: KnowledgeItemStatus = KnowledgeItemStatus.ACTIVE,
    ): KnowledgeItem {
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(
            parker.core.interfaces.AssertionId("assertion-${knowledgeId.value}"),
        )
        val defaultHistory = listOf(
            KnowledgePromotion(
                knowledgeId = knowledgeId,
                evidenceReference = evidenceReference,
                resultingState = EvidentialState.UNKNOWN,
                occurredAt = occurredAt,
                basis = basis,
            ),
        )
        return KnowledgeItem(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            provenanceReference = ProvenanceReference(ProvenanceId("prov-${knowledgeId.value}")),
            evidentialState = EvidentialState.UNKNOWN,
            status = status,
            history = history ?: defaultHistory,
        )
    }

    private fun query(relevance: String, maximumResults: Int = 10, includeRetired: Boolean = false) = KnowledgeRetrievalQuery(
        relevance = relevance,
        correlationId = "corr-1",
        maximumResults = maximumResults,
        includeRetired = includeRetired,
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

    // --- Lifecycle shaping (Unit 9.4): active-item behaviour ---

    @Test
    fun `an ACTIVE item is matched, ordered, and bounded exactly as before -- Unit 9-4 changes nothing about active-item behaviour`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", status = KnowledgeItemStatus.ACTIVE))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(1, retrieved.result.entries.size)
        assertEquals(KnowledgeItemStatus.ACTIVE, retrieved.result.entries.single().item.status)
    }

    // --- Lifecycle shaping (Unit 9.4): retired-item default behaviour ---

    @Test
    fun `a RETIRED item is excluded from an ordinary query by default -- Unit 9-4's own considered default`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", status = KnowledgeItemStatus.RETIRED))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            emptyList<Any>(),
            retrieved.result.entries,
            "a well-formed query that legitimately matches nothing (because the only match is retired and " +
                "includeRetired was not set) must still return a valid, empty Retrieved result, never an error",
        )
    }

    @Test
    fun `a mixed batch of ACTIVE and RETIRED items returns only the ACTIVE ones by default`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("active"), basis = "grocery active", status = KnowledgeItemStatus.ACTIVE))
        persistence.store(item(KnowledgeId("retired"), basis = "grocery retired", status = KnowledgeItemStatus.RETIRED))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(listOf(KnowledgeId("active")), retrieved.result.entries.map { it.item.knowledgeId })
    }

    // --- Lifecycle shaping (Unit 9.4): explicit retired-item request behaviour ---

    @Test
    fun `includeRetired = true admits a RETIRED item, disclosing its retired status honestly, never as though it were active`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", status = KnowledgeItemStatus.RETIRED))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery", includeRetired = true))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(1, retrieved.result.entries.size)
        assertEquals(
            KnowledgeItemStatus.RETIRED,
            retrieved.result.entries.single().item.status,
            "an included retired item must still disclose RETIRED honestly, never presented as ACTIVE",
        )
    }

    @Test
    fun `includeRetired = true admits both ACTIVE and RETIRED items in the same result`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("active"), basis = "grocery active", status = KnowledgeItemStatus.ACTIVE))
        persistence.store(item(KnowledgeId("retired"), basis = "grocery retired", status = KnowledgeItemStatus.RETIRED))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery", includeRetired = true))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            listOf(KnowledgeId("active"), KnowledgeId("retired")),
            retrieved.result.entries.map { it.item.knowledgeId },
        )
    }

    @Test
    fun `includeRetired = true does not itself grant, imply, or bypass any permission decision -- Unit 9-5's own future responsibility, untouched`() = runTest {
        // A structural, behavioural companion to the contract-tier "includeRetired is a structural
        // criterion, never a permission-shaped field" test -- confirms includeRetired only widens
        // which already-matched items are considered, never returns NotAuthorised or any permission
        // outcome of its own, since no permission evaluation exists anywhere in this Unit yet.
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", status = KnowledgeItemStatus.RETIRED))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery", includeRetired = true))

        assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
    }

    // --- Lifecycle shaping (Unit 9.4): restored-item behaviour ---

    @Test
    fun `a restored item -- promoted, retired, then restored -- is included by default, exactly as any other ACTIVE item`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val promotion = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
            basis = "grocery list",
        )
        val retirement = KnowledgeRetirement(
            knowledgeId = knowledgeId,
            occurredAt = Instant.parse("2026-02-01T00:00:00Z"),
            basis = "no longer needed",
        )
        val restoration = KnowledgeRestoration(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            occurredAt = Instant.parse("2026-03-01T00:00:00Z"),
            basis = "support re-established",
        )
        val restoredItem = KnowledgeItem(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            provenanceReference = ProvenanceReference(ProvenanceId("prov-k1")),
            evidentialState = EvidentialState.UNKNOWN,
            status = KnowledgeItemStatus.ACTIVE,
            history = listOf(promotion, retirement, restoration),
        )
        persistence.store(restoredItem)
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        // Matching uses the most recent history entry's own basis (Unit 9.2's own fixed decision,
        // untouched here), so the query targets the restoration's own disclosed basis text.
        val disposition = retrieval.retrieve(principal, query(relevance = "re-established"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            1,
            retrieved.result.entries.size,
            "a restored item's own status is ACTIVE -- isRetrievable admits it without includeRetired, " +
                "and without any special-case code recognising the earlier retirement in its history",
        )
        assertEquals(KnowledgeItemStatus.ACTIVE, retrieved.result.entries.single().item.status)
        assertEquals(
            3,
            retrieved.result.entries.single().item.history.size,
            "the full promoted -> retired -> restored sequence remains visible, never silently collapsed",
        )
    }

    // --- Lifecycle shaping (Unit 9.4): revised-item behaviour ---

    @Test
    fun `a revised, still-ACTIVE item is included by default, matched against its own most recent classification`() = runTest {
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
            basis = "grocery shopping list, revised quantities",
        )
        persistence.store(
            item(knowledgeId, basis = "unused", history = listOf(originalPromotion, revision), status = KnowledgeItemStatus.ACTIVE),
        )
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "revised quantities"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(1, retrieved.result.entries.size)
        assertEquals(2, retrieved.result.entries.single().item.history.size, "the revision is appended, never overwriting the original promotion")
    }

    // --- Lifecycle shaping (Unit 9.4): superseded-item retrievability and multi-hop chains ---

    @Test
    fun `a superseded classification remains retrievable as part of the same item's own history, never as a separate item`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val originalPromotion = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
            basis = "grocery list, superseded original",
        )
        val supersedingPromotion = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-02-01T00:00:00Z"),
            basis = "grocery list, current classification",
        )
        persistence.store(
            item(knowledgeId, basis = "unused", history = listOf(originalPromotion, supersedingPromotion)),
        )
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        val history = retrieved.result.entries.single().item.history
        assertEquals(
            listOf(originalPromotion, supersedingPromotion),
            history,
            "the superseded entry remains part of the same item's own history, in order, never dropped",
        )
    }

    @Test
    fun `current versus superseded is distinguished by position in history, not by a separate field or a separate item`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val superseded = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
            basis = "grocery list, superseded",
        )
        val current = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-02-01T00:00:00Z"),
            basis = "grocery list, current",
        )
        persistence.store(item(knowledgeId, basis = "unused", history = listOf(superseded, current)))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        val history = retrieved.result.entries.single().item.history
        assertEquals(current, history.last(), "the most recent history entry is the item's own current classification")
        assertEquals(superseded, history.first(), "the earlier entry remains present, distinguishable only by its own position")
    }

    @Test
    fun `a multi-hop supersession chain of four classifications remains transitively retrievable in full, never truncated to the latest`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val chain = (1..4).map { hop ->
            KnowledgePromotion(
                knowledgeId = knowledgeId,
                evidenceReference = evidenceReference,
                resultingState = EvidentialState.UNKNOWN,
                occurredAt = Instant.parse("2026-0$hop-01T00:00:00Z"),
                basis = "grocery list, hop $hop",
            )
        }
        persistence.store(item(knowledgeId, basis = "unused", history = chain))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(chain, retrieved.result.entries.single().item.history, "every hop of the chain, in order, must remain reachable")
    }

    @Test
    fun `no latest-only selection occurs -- the full item, not a projection of only its current classification, is returned`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val chain = (1..3).map { hop ->
            KnowledgePromotion(
                knowledgeId = knowledgeId,
                evidenceReference = evidenceReference,
                resultingState = EvidentialState.UNKNOWN,
                occurredAt = Instant.parse("2026-0$hop-01T00:00:00Z"),
                basis = "grocery list, hop $hop",
            )
        }
        val storedItem = item(knowledgeId, basis = "unused", history = chain)
        persistence.store(storedItem)
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(storedItem, retrieved.result.entries.single().item, "the entire stored item, unprojected, is returned")
    }

    // --- Lifecycle shaping (Unit 9.4): deterministic ordering and bounding ---

    @Test
    fun `deterministic ordering is preserved across a mixed batch of ACTIVE and RETIRED items when includeRetired = true`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k3"), basis = "grocery third", status = KnowledgeItemStatus.RETIRED))
        persistence.store(item(KnowledgeId("k1"), basis = "grocery first", status = KnowledgeItemStatus.ACTIVE))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery second", status = KnowledgeItemStatus.RETIRED))
        val retrieval = DefaultKnowledgeRetrieval(persistence)
        val theQuery = query(relevance = "grocery", includeRetired = true)

        val first = retrieval.retrieve(principal, theQuery)
        val second = retrieval.retrieve(principal, theQuery)

        val firstOrder = assertIs<KnowledgeRetrievalDisposition.Retrieved>(first).result.entries.map { it.item.knowledgeId }
        assertEquals(listOf(KnowledgeId("k3"), KnowledgeId("k1"), KnowledgeId("k2")), firstOrder)
        assertEquals(first, second, "the same query against unchanged state, including lifecycle shaping, is fully repeatable")
    }

    @Test
    fun `maximumResults bounds the result after lifecycle shaping has already excluded non-matching items`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery one", status = KnowledgeItemStatus.ACTIVE))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery two", status = KnowledgeItemStatus.RETIRED))
        persistence.store(item(KnowledgeId("k3"), basis = "grocery three", status = KnowledgeItemStatus.ACTIVE))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery", maximumResults = 1, includeRetired = true))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            listOf(KnowledgeId("k1")),
            retrieved.result.entries.map { it.item.knowledgeId },
            "bounding applies to the already-lifecycle-shaped set, in the same insertion order",
        )
    }

    // --- Lifecycle shaping (Unit 9.4): staleness disclosure preserved for shaped results ---

    @Test
    fun `a RETIRED item admitted via includeRetired still carries a mandatory staleness disclosure`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(
            item(
                KnowledgeId("k1"),
                basis = "grocery list",
                occurredAt = now.minus(Duration.ofDays(1)),
                status = KnowledgeItemStatus.RETIRED,
            ),
        )
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery", includeRetired = true))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(StalenessDisclosure.INDETERMINATE, retrieved.result.entries.single().staleness)
    }

    // --- Lifecycle shaping (Unit 9.4): no mutation of stored items ---

    @Test
    fun `retrieval never mutates the stored KnowledgeItem, regardless of lifecycle shaping applied`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val retiredItem = item(knowledgeId, basis = "grocery list", status = KnowledgeItemStatus.RETIRED)
        persistence.store(retiredItem)
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        retrieval.retrieve(principal, query(relevance = "grocery", includeRetired = true))
        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertEquals(retiredItem, persistence.find(knowledgeId), "the stored item must be byte-for-byte unchanged after any number of retrieve calls")
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
    fun `the same query against unchanged state and a fixed clock returns an identical result across repeated calls`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list one", occurredAt = now))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery list two", occurredAt = now))
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)
        val theQuery = query(relevance = "grocery")

        val first = retrieval.retrieve(principal, theQuery)
        val second = retrieval.retrieve(principal, theQuery)
        val third = retrieval.retrieve(principal, theQuery)

        assertEquals(first, second)
        assertEquals(second, third)
    }

    // --- Staleness disclosure (Unit 9.3, widened per the Independent Constitutional Review) ---

    @Test
    fun `an item classified well within the possibly-stale threshold discloses INDETERMINATE, never a freshness claim`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val recentlyClassifiedAt = now.minus(Duration.ofDays(1))
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", occurredAt = recentlyClassifiedAt))
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(StalenessDisclosure.INDETERMINATE, retrieved.result.entries.single().staleness)
    }

    @Test
    fun `an item classified well beyond the possibly-stale threshold discloses POSSIBLY_STALE, never CONFIRMED_STALE`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val longAgoClassifiedAt = now.minus(Duration.ofDays(90))
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", occurredAt = longAgoClassifiedAt))
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(StalenessDisclosure.POSSIBLY_STALE, retrieved.result.entries.single().staleness)
    }

    @Test
    fun `an item classified exactly at the possibly-stale threshold is not yet disclosed as POSSIBLY_STALE`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val exactlyAtThreshold = now.minus(Duration.ofDays(30))
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", occurredAt = exactlyAtThreshold))
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            StalenessDisclosure.INDETERMINATE,
            retrieved.result.entries.single().staleness,
            "elapsed time exactly equal to the threshold must not itself be treated as exceeding it",
        )
    }

    @Test
    fun `an item classified one instant beyond the possibly-stale threshold is disclosed as POSSIBLY_STALE`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val justOverThreshold = now.minus(Duration.ofDays(30)).minusSeconds(1)
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", occurredAt = justOverThreshold))
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(StalenessDisclosure.POSSIBLY_STALE, retrieved.result.entries.single().staleness)
    }

    @Test
    fun `a mixed batch discloses each item's own staleness disclosure independently`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("fresh"), basis = "grocery fresh", occurredAt = now.minus(Duration.ofDays(1))))
        persistence.store(item(KnowledgeId("stale"), basis = "grocery stale", occurredAt = now.minus(Duration.ofDays(90))))
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        val disclosureByItem = retrieved.result.entries.associate { it.item.knowledgeId to it.staleness }
        assertEquals(
            mapOf(KnowledgeId("fresh") to StalenessDisclosure.INDETERMINATE, KnowledgeId("stale") to StalenessDisclosure.POSSIBLY_STALE),
            disclosureByItem,
        )
    }

    @Test
    fun `staleness is computed from the most recent KnowledgePromotion's occurredAt, not an earlier one`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val originalPromotion = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = now.minus(Duration.ofDays(90)),
            basis = "grocery",
        )
        val recentRevision = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = now.minus(Duration.ofDays(1)),
            basis = "grocery revised",
        )
        persistence.store(item(knowledgeId, basis = "unused", history = listOf(originalPromotion, recentRevision)))
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            StalenessDisclosure.INDETERMINATE,
            retrieved.result.entries.single().staleness,
            "the recent revision's own occurredAt, not the 90-day-old original promotion's, must govern staleness",
        )
    }

    @Test
    fun `a KnowledgeRetirement following a promotion does not govern the staleness reference timestamp`() = runTest {
        // Finding 4 of the Independent Constitutional Review: a retirement or restoration is "a
        // status change, never an evidential classification" (Unit 7's own established precedent),
        // so it must not reset the staleness clock even though it is the latest history entry.
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val longAgoPromotion = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = now.minus(Duration.ofDays(90)),
            basis = "grocery",
        )
        val recentRetirement = KnowledgeRetirement(
            knowledgeId = knowledgeId,
            occurredAt = now.minus(Duration.ofDays(1)),
            basis = "grocery no longer needed",
        )
        persistence.store(item(knowledgeId, basis = "unused", history = listOf(longAgoPromotion, recentRetirement)))
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            StalenessDisclosure.POSSIBLY_STALE,
            retrieved.result.entries.single().staleness,
            "the 90-day-old promotion, not the recent retirement, must govern the staleness reference timestamp",
        )
    }

    @Test
    fun `a KnowledgeRestoration following a retirement does not govern the staleness reference timestamp either`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val longAgoPromotion = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = now.minus(Duration.ofDays(90)),
            basis = "grocery",
        )
        val oldRetirement = KnowledgeRetirement(
            knowledgeId = knowledgeId,
            occurredAt = now.minus(Duration.ofDays(60)),
            basis = "no longer needed",
        )
        val recentRestoration = KnowledgeRestoration(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            occurredAt = now.minus(Duration.ofDays(1)),
            basis = "grocery support re-established",
        )
        persistence.store(
            item(knowledgeId, basis = "unused", history = listOf(longAgoPromotion, oldRetirement, recentRestoration)),
        )
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            StalenessDisclosure.POSSIBLY_STALE,
            retrieved.result.entries.single().staleness,
            "the 90-day-old promotion, not the recent restoration, must govern the staleness reference timestamp",
        )
    }

    @Test
    fun `an item with no KnowledgePromotion history entry discloses INDETERMINATE, never POSSIBLY_STALE`() = runTest {
        // Structurally impossible via this Unit's own precedent construction path (every promoted
        // KnowledgeItem carries a KnowledgePromotion as history's first entry), but this class has
        // no reference timestamp to measure elapsed time against if it ever occurred, and therefore
        // no basis for POSSIBLY_STALE -- INDETERMINATE, never a fabricated claim in either direction.
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val onlyRetirement = KnowledgeRetirement(
            knowledgeId = knowledgeId,
            occurredAt = now.minus(Duration.ofDays(1)),
            basis = "no longer needed",
        )
        val itemWithNoPromotion = KnowledgeItem(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            provenanceReference = ProvenanceReference(ProvenanceId("prov-k1")),
            evidentialState = EvidentialState.UNKNOWN,
            status = KnowledgeItemStatus.RETIRED,
            history = listOf(onlyRetirement),
        )
        persistence.store(itemWithNoPromotion)
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery", includeRetired = true))

        // "unused" basis note does not apply here -- matching still requires the retirement's own
        // basis to contain the relevance text, so query against its own basis text instead. This
        // item is RETIRED, so includeRetired = true is required for either call to see it at all
        // (Unit 9.4's own default-exclude policy) -- orthogonal to the INDETERMINATE case under test.
        val secondDisposition = retrieval.retrieve(principal, query(relevance = "no longer needed", includeRetired = true))

        assertEquals(emptyList<Any>(), assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition).result.entries)
        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(secondDisposition)
        assertEquals(StalenessDisclosure.INDETERMINATE, retrieved.result.entries.single().staleness)
    }

    @Test
    fun `staleness disclosure does not affect which items match or their order`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery fresh", occurredAt = now.minus(Duration.ofDays(1))))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery stale", occurredAt = now.minus(Duration.ofDays(90))))
        val retrieval = DefaultKnowledgeRetrieval(persistence, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            listOf(KnowledgeId("k1"), KnowledgeId("k2")),
            retrieved.result.entries.map { it.item.knowledgeId },
            "staleness must not reorder, drop, or duplicate matched entries",
        )
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
    fun `DefaultKnowledgeRetrieval holds exactly two constructor dependencies -- KnowledgeItemPersistence and a Clock`() {
        val constructor = requireNotNull(DefaultKnowledgeRetrieval::class.primaryConstructor)
        val parameterTypes = constructor.parameters.map { it.type.classifier }

        assertEquals(listOf(KnowledgeItemPersistence::class, Clock::class), parameterTypes)
    }

    @Test
    fun `the clock parameter defaults, so an existing single-argument construction site remains valid`() = runTest {
        // Unit 9.3 added `clock` with a default value specifically so this construction shape --
        // already used by every pre-existing Unit 9.2 call site -- continues to compile and behave,
        // using the real system clock implicitly, exactly as before this Unit's own addition.
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list"))
        val retrieval = DefaultKnowledgeRetrieval(persistence)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
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
