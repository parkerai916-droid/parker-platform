package parker.core.runtime

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.ExecutionRequest
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
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.RelevanceCandidate
import parker.core.interfaces.RelevanceCandidateToken
import parker.core.interfaces.RelevanceMechanism
import parker.core.interfaces.RelevanceRequest
import parker.core.interfaces.RelevanceResult
import parker.core.interfaces.StalenessDisclosure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Programme 3, Knowledge Memory, Implementation Units 9.2 (Deterministic
 * Retrieval Engine), 9.3 (Staleness Disclosure), 9.4 (Retirement and
 * Supersession Retrieval-Shape Decision), 9.5 (Permission Enforcement
 * Wiring), 9.7.2 (Fallback Trigger and Closed Candidate Set), and 9.7.4
 * (Integrity Validation, Canonical Token Re-resolution, and Fresh
 * Pre-disclosure Re-verification). Behavioural and structural tests for
 * [DefaultKnowledgeRetrieval] -- see
 * `docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_COMPLETION_REVIEW.md`,
 * `docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_COMPLETION_REVIEW.md`,
 * `docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_COMPLETION_REVIEW.md`,
 * `docs/reviews/PROGRAMME_3_UNIT_9_5_PERMISSION_ENFORCEMENT_IMPLEMENTATION_COMPLETION_REVIEW.md`,
 * and `docs/governance/PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * §8's own Unit 9.7.2 and Unit 9.7.4 entries, for the design decisions this
 * suite verifies. This suite does not exercise runtime composition -- Unit
 * 9.7.5 is not implemented, and not exercised here.
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

    /** An engine approving (or, per [outcome], not approving) every evaluation, regardless of granularity. */
    private fun approvingEngine(outcome: PermissionDecisionOutcome = PermissionDecisionOutcome.APPROVED) =
        FakePermissionEngine { request -> decision(request, outcome) }

    /**
     * An engine approving the act-level gate unconditionally, and deciding every item-level gate
     * per [itemOutcome] -- the shape most Unit 9.5 tests need. Distinguishes the two granularities
     * by comparing [ExecutionRequest.intent] against [DefaultKnowledgeRetrieval.ACT_LEVEL_INTENT],
     * never by call order alone.
     */
    private fun actLevelApprovingEngine(itemOutcome: PermissionDecisionOutcome) =
        FakePermissionEngine { request ->
            val outcome = if (request.intent == DefaultKnowledgeRetrieval.ACT_LEVEL_INTENT) {
                PermissionDecisionOutcome.APPROVED
            } else {
                itemOutcome
            }
            decision(request, outcome)
        }

    private fun decision(request: ExecutionRequest, outcome: PermissionDecisionOutcome) = PermissionDecision(
        decisionId = DecisionId("decision-1"),
        principalId = request.principalId,
        resourceId = request.targetResources.first(),
        action = PermissionAction.READ,
        decision = outcome,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.now(),
    )

    /**
     * Programme 3, Unit 9.7.4. The default [RelevanceMechanism] dependency
     * for every test in this suite whose own fixture must never reach the
     * fallback branch's mechanism invocation at all -- because structural
     * matching found a result, the lifecycle-eligible or closed candidate
     * set is empty, the act-level gate denied, or a persistence failure
     * propagated first. Throwing here, rather than quietly returning an
     * empty result, turns an accidental widening of when Unit 9.7.4's own
     * invocation actually occurs into an immediate, loud test failure
     * instead of a silent pass.
     */
    private val neverInvokedRelevanceMechanism = RelevanceMechanism { _ ->
        error(
            "RelevanceMechanism.rank must not be invoked by this test's own fixture -- structural " +
                "matching found a result, the lifecycle-eligible or closed candidate set was empty, " +
                "the act-level gate denied, or a failure occurred before the fallback branch's own " +
                "mechanism invocation point",
        )
    }

    /**
     * Programme 3, Unit 9.7.4. A [RelevanceMechanism] fake that always
     * returns a genuine, successful, empty [RelevanceResult], regardless of
     * what it is asked to rank. Used only by the two pre-existing Unit
     * 9.7.2 fixtures (below) whose own fallback branch does reach a
     * non-empty closed candidate set -- and therefore does now invoke the
     * mechanism, since Unit 9.7.4 landed -- but whose own assertions
     * predate Unit 9.7.4 and were written to expect an empty `entries`
     * result regardless of what a later unit's mechanism invocation
     * eventually did with that candidate set.
     */
    private val emptyResultRelevanceMechanism = RelevanceMechanism { _ -> RelevanceResult(rankedTokens = emptyList()) }

    // --- Matching ---

    @Test
    fun `an item whose most recent basis contains the relevance text is matched`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery shopping list preferences"))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(1, retrieved.result.entries.size)
        assertEquals(KnowledgeId("k1"), retrieved.result.entries[0].item.knowledgeId)
    }

    @Test
    fun `matching is case-insensitive`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "Grocery Shopping List"))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(1, retrieved.result.entries.size)
    }

    @Test
    fun `an item whose basis does not contain the relevance text is excluded`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated household task"))
        // Windows Verification Failure (Task D, bounded correction): this test's own fixture
        // legitimately reaches Unit 9.7.4's fallback branch now -- the item is lifecycle-eligible
        // and permission-approved (closedCandidateSet non-empty) while structurally unmatched
        // (structurallyMatched empty), which is exactly the lawful trigger condition. Supplying
        // neverInvokedRelevanceMechanism here was a Task B categorization defect: it made the
        // mechanism throw instead of representing the neutral "found nothing relevant" semantic
        // outcome this test's own emptyList() assertion already expects. emptyResultRelevanceMechanism
        // is the correct fixture -- the test's own assertion is unchanged, only the fake supplied.
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), emptyResultRelevanceMechanism)

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
        // Windows Verification Failure (Task D, bounded correction): the first retrieve() call below
        // (relevance = "grocery") does not structurally match the most recent history entry's own
        // basis ("updated household budget notes"), so it legitimately reaches the fallback branch
        // with a non-empty closed candidate set -- the identical Task B categorization defect as the
        // sibling test above. The second call (relevance = "budget") does structurally match and
        // takes the structural branch regardless, so a single shared emptyResultRelevanceMechanism
        // across both calls in this test is safe and correct.
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), emptyResultRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(listOf(KnowledgeId("active")), retrieved.result.entries.map { it.item.knowledgeId })
    }

    // --- Lifecycle shaping (Unit 9.4): explicit retired-item request behaviour ---

    @Test
    fun `includeRetired = true admits a RETIRED item, disclosing its retired status honestly, never as though it were active`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", status = KnowledgeItemStatus.RETIRED))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery", includeRetired = true))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            listOf(KnowledgeId("active"), KnowledgeId("retired")),
            retrieved.result.entries.map { it.item.knowledgeId },
        )
    }

    @Test
    fun `includeRetired = true does not itself grant, imply, or bypass the item-level permission gate`() = runTest {
        // A structural, behavioural companion to the contract-tier "includeRetired is a structural
        // criterion, never a permission-shaped field" test -- confirms includeRetired only widens
        // which already-matched items are considered for permission evaluation, never bypasses it.
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", status = KnowledgeItemStatus.RETIRED))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)
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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism, fixedClock)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

        retrieval.retrieve(principal, query(relevance = "grocery", includeRetired = true))
        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertEquals(retiredItem, persistence.find(knowledgeId), "the stored item must be byte-for-byte unchanged after any number of retrieve calls")
    }

    // --- Empty result ---

    @Test
    fun `an empty persistence returns Retrieved with an empty result, never an error or denial`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism, fixedClock)
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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(StalenessDisclosure.INDETERMINATE, retrieved.result.entries.single().staleness)
    }

    @Test
    fun `an item classified well beyond the possibly-stale threshold discloses POSSIBLY_STALE, never CONFIRMED_STALE`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val longAgoClassifiedAt = now.minus(Duration.ofDays(90))
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", occurredAt = longAgoClassifiedAt))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(StalenessDisclosure.POSSIBLY_STALE, retrieved.result.entries.single().staleness)
    }

    @Test
    fun `an item classified exactly at the possibly-stale threshold is not yet disclosed as POSSIBLY_STALE`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val exactlyAtThreshold = now.minus(Duration.ofDays(30))
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", occurredAt = exactlyAtThreshold))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism, fixedClock)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(StalenessDisclosure.POSSIBLY_STALE, retrieved.result.entries.single().staleness)
    }

    @Test
    fun `a mixed batch discloses each item's own staleness disclosure independently`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("fresh"), basis = "grocery fresh", occurredAt = now.minus(Duration.ofDays(1))))
        persistence.store(item(KnowledgeId("stale"), basis = "grocery stale", occurredAt = now.minus(Duration.ofDays(90))))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism, fixedClock)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism, fixedClock)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism, fixedClock)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism, fixedClock)

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
        // Windows Verification Failure (Task D, bounded correction): the first retrieve() call below
        // (relevance = "grocery", includeRetired = true) does not structurally match the retirement's
        // own basis ("no longer needed"), while the item is lifecycle-eligible (RETIRED + includeRetired)
        // and permission-approved -- the identical Task B categorization defect as the two "Matching"
        // section tests, above. The second call (relevance = "no longer needed") does structurally
        // match and takes the structural branch regardless, so a single shared
        // emptyResultRelevanceMechanism across both calls is safe and correct.
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), emptyResultRelevanceMechanism, fixedClock)

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
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism, fixedClock)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            listOf(KnowledgeId("k1"), KnowledgeId("k2")),
            retrieved.result.entries.map { it.item.knowledgeId },
            "staleness must not reorder, drop, or duplicate matched entries",
        )
    }

    @Test
    fun `different requesting principals receive identical results for the same query, given a principal-agnostic policy`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list"))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)
        val theQuery = query(relevance = "grocery")

        val first = retrieval.retrieve(PrincipalId("owner-a"), theQuery)
        val second = retrieval.retrieve(PrincipalId("owner-b"), theQuery)

        assertEquals(first, second)
    }

    // ================================================================
    // Permission Enforcement (Unit 9.5)
    // ================================================================

    // --- Authorised retrieval ---

    @Test
    fun `an authorised query returns Retrieved with the matched item`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list"))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(1, retrieved.result.entries.size)
    }

    @Test
    fun `retrieve requires an actual Permission Engine call -- it is not bypassed`() = runTest {
        val engine = approvingEngine()
        val persistence = InMemoryKnowledgeItemPersistence()
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertTrue(engine.evaluateCallCount >= 1)
    }

    // --- Act-level denial ---

    @Test
    fun `a DENIED act-level decision returns NotAuthorised and never reads persistence`() = runTest {
        val persistence = CountingKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list"))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(PermissionDecisionOutcome.DENIED), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        assertIs<KnowledgeRetrievalDisposition.NotAuthorised>(disposition)
        assertEquals(0, persistence.findAllCallCount, "a denied act-level decision must never read persistence")
    }

    @Test
    fun `a DEFERRED act-level decision is NotAuthorised, treated the same as denial`() = runTest {
        val persistence = CountingKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list"))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(PermissionDecisionOutcome.DEFERRED), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        assertIs<KnowledgeRetrievalDisposition.NotAuthorised>(disposition)
        assertEquals(0, persistence.findAllCallCount)
    }

    @Test
    fun `a NotAuthorised result carries a non-blank reason naming the requesting principal`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(PermissionDecisionOutcome.DENIED), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val notAuthorised = assertIs<KnowledgeRetrievalDisposition.NotAuthorised>(disposition)
        assertTrue(notAuthorised.reason.isNotBlank())
        assertTrue(notAuthorised.reason.contains(principal.value), "reason should name the requesting principal")
    }

    @Test
    fun `an act-level denial performs no matching, lifecycle shaping, or staleness computation`() = runTest {
        val persistence = CountingKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list", occurredAt = now.minus(Duration.ofDays(90))))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(PermissionDecisionOutcome.DENIED), neverInvokedRelevanceMechanism, fixedClock)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertEquals(0, persistence.findAllCallCount, "no persistence read occurs, so no matching, shaping, or staleness computation can occur either")
    }

    // --- Mixed authorised/unauthorised items (item-level gate) ---

    @Test
    fun `an item-level DENIED decision silently excludes that item, never surfaced as a distinguishable denial`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val approvedId = KnowledgeId("approved")
        val deniedId = KnowledgeId("denied")
        persistence.store(item(approvedId, basis = "grocery approved"))
        persistence.store(item(deniedId, basis = "grocery denied"))
        val engine = FakePermissionEngine { request ->
            val outcome = if (request.intent.contains(deniedId.value)) {
                PermissionDecisionOutcome.DENIED
            } else {
                PermissionDecisionOutcome.APPROVED
            }
            decision(request, outcome)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(listOf(approvedId), retrieved.result.entries.map { it.item.knowledgeId })
    }

    @Test
    fun `an item-level DEFERRED decision is also silently excluded, mirroring DENIED treatment`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list"))
        val retrieval = DefaultKnowledgeRetrieval(persistence, actLevelApprovingEngine(PermissionDecisionOutcome.DEFERRED), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(emptyList<Any>(), retrieved.result.entries)
    }

    // --- Complete filtering of all items ---

    @Test
    fun `when every item is denied at item level, the result is an ordinary empty Retrieved, never NotAuthorised`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery one"))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery two"))
        val retrieval = DefaultKnowledgeRetrieval(persistence, actLevelApprovingEngine(PermissionDecisionOutcome.DENIED), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            emptyList<Any>(),
            retrieved.result.entries,
            "complete item-level filtering must never be conflated with act-level NotAuthorised",
        )
    }

    // --- Deterministic ordering after filtering ---

    @Test
    fun `deterministic ordering is preserved after item-level permission filtering`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery one"))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery two"))
        persistence.store(item(KnowledgeId("k3"), basis = "grocery three"))
        val deniedId = KnowledgeId("k2")
        val engine = FakePermissionEngine { request ->
            val outcome = if (request.intent.contains(deniedId.value)) PermissionDecisionOutcome.DENIED else PermissionDecisionOutcome.APPROVED
            decision(request, outcome)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)
        val theQuery = query(relevance = "grocery")

        val first = retrieval.retrieve(principal, theQuery)
        val second = retrieval.retrieve(principal, theQuery)

        val order = assertIs<KnowledgeRetrievalDisposition.Retrieved>(first).result.entries.map { it.item.knowledgeId }
        assertEquals(listOf(KnowledgeId("k1"), KnowledgeId("k3")), order, "insertion order preserved, minus the denied item")
        assertEquals(first, second, "repeated calls against unchanged state and policy are fully repeatable")
    }

    // --- Retirement filtering combined with permission filtering ---

    @Test
    fun `retirement filtering and permission filtering compose -- a RETIRED item is excluded before ever reaching the item-level gate`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("active"), basis = "grocery active", status = KnowledgeItemStatus.ACTIVE))
        persistence.store(item(KnowledgeId("retired"), basis = "grocery retired", status = KnowledgeItemStatus.RETIRED))
        val evaluatedIntents = mutableListOf<String>()
        val engine = FakePermissionEngine { request ->
            evaluatedIntents.add(request.intent)
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(listOf(KnowledgeId("active")), retrieved.result.entries.map { it.item.knowledgeId })
        assertFalse(
            evaluatedIntents.any { it.contains("retired") },
            "a RETIRED item excluded by lifecycle shaping must never reach the item-level permission gate at all",
        )
        assertEquals(2, evaluatedIntents.size, "1 act-level + 1 item-level (the ACTIVE item only)")
    }

    @Test
    fun `an explicitly included RETIRED item (includeRetired = true) is still subject to the item-level permission gate`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val retiredId = KnowledgeId("retired")
        persistence.store(item(retiredId, basis = "grocery retired", status = KnowledgeItemStatus.RETIRED))
        val engine = FakePermissionEngine { request ->
            val outcome = if (request.intent.contains(retiredId.value)) PermissionDecisionOutcome.DENIED else PermissionDecisionOutcome.APPROVED
            decision(request, outcome)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery", includeRetired = true))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            emptyList<Any>(),
            retrieved.result.entries,
            "includeRetired grants no permission of its own -- the item-level gate still applies",
        )
    }

    // --- Superseded history preservation under permission approval ---

    @Test
    fun `superseded history remains fully intact on an item that passes both permission gates`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val knowledgeId = KnowledgeId("k1")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val superseded = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
            basis = "grocery superseded",
        )
        val current = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-02-01T00:00:00Z"),
            basis = "grocery current",
        )
        persistence.store(item(knowledgeId, basis = "unused", history = listOf(superseded, current)))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(listOf(superseded, current), retrieved.result.entries.single().item.history)
    }

    // --- Correlation identifier propagation ---

    @Test
    fun `query correlationId is propagated unchanged into every ExecutionRequest, act-level and item-level alike`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery one"))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery two"))
        val capturedCorrelationIds = mutableListOf<String>()
        val engine = FakePermissionEngine { request ->
            capturedCorrelationIds.add(request.correlationId)
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)
        val theQuery = KnowledgeRetrievalQuery(relevance = "grocery", correlationId = "distinctive-correlation-id", maximumResults = 10)

        retrieval.retrieve(principal, theQuery)

        assertEquals(3, capturedCorrelationIds.size, "1 act-level + 2 item-level evaluations")
        assertTrue(
            capturedCorrelationIds.all { it == "distinctive-correlation-id" },
            "every evaluation must carry the caller's own correlationId unchanged, never a freshly minted one",
        )
    }

    @Test
    fun `an act-level denial still evaluates with the caller-supplied correlationId, never an ambient or freshly minted one`() = runTest {
        var capturedCorrelationId: String? = null
        val engine = FakePermissionEngine { request ->
            capturedCorrelationId = request.correlationId
            decision(request, PermissionDecisionOutcome.DENIED)
        }
        val persistence = InMemoryKnowledgeItemPersistence()
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)
        val theQuery = KnowledgeRetrievalQuery(relevance = "grocery", correlationId = "act-level-corr-id", maximumResults = 10)

        retrieval.retrieve(principal, theQuery)

        assertEquals("act-level-corr-id", capturedCorrelationId)
    }

    // --- Resource/action correctness ---

    @Test
    fun `every ExecutionRequest, act-level and item-level, names the fixed KNOWLEDGE_RETRIEVAL_RESOURCE_ID and RETRIEVE_ACTION_NAME`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery one"))
        val capturedRequests = mutableListOf<ExecutionRequest>()
        val engine = FakePermissionEngine { request ->
            capturedRequests.add(request)
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertEquals(2, capturedRequests.size, "1 act-level + 1 item-level")
        capturedRequests.forEach { request ->
            assertEquals(listOf(DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID), request.targetResources)
            assertEquals(listOf(DefaultKnowledgeRetrieval.RETRIEVE_ACTION_NAME), request.proposedActions)
        }
    }

    @Test
    fun `the act-level and item-level requests are distinguishable only by intent, never by resource or action`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery one"))
        val capturedIntents = mutableListOf<String>()
        val engine = FakePermissionEngine { request ->
            capturedIntents.add(request.intent)
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertEquals(DefaultKnowledgeRetrieval.ACT_LEVEL_INTENT, capturedIntents[0])
        assertTrue(capturedIntents[1].contains("k1"), "the item-level intent names the specific item under evaluation")
    }

    // --- Principal propagation ---

    @Test
    fun `requestingPrincipalId is propagated unchanged into every ExecutionRequest, act-level and item-level alike`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery one"))
        val capturedPrincipals = mutableListOf<PrincipalId>()
        val engine = FakePermissionEngine { request ->
            capturedPrincipals.add(request.principalId)
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertTrue(capturedPrincipals.isNotEmpty())
        assertTrue(capturedPrincipals.all { it == principal })
    }

    // --- Exact evaluation count / no double evaluation ---

    @Test
    fun `exactly 1 + N evaluations occur when the act-level gate approves, for N items surviving matching and lifecycle shaping`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery one"))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery two"))
        persistence.store(item(KnowledgeId("k3"), basis = "unrelated household task"))
        val engine = approvingEngine()
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertEquals(3, engine.evaluateCallCount, "1 act-level + 2 items surviving matching -- k3 never matched, never evaluated")
    }

    @Test
    fun `exactly 1 evaluation occurs when the act-level gate denies, regardless of how many items would otherwise match`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery one"))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery two"))
        val engine = approvingEngine(PermissionDecisionOutcome.DENIED)
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertEquals(1, engine.evaluateCallCount)
    }

    @Test
    fun `no item is evaluated more than once`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery one"))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery two"))
        val requestIds = mutableListOf<String>()
        val engine = FakePermissionEngine { request ->
            requestIds.add(request.requestId.value)
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertEquals(requestIds.size, requestIds.toSet().size, "every evaluation must use its own, distinct requestId -- no evaluation is repeated")
    }

    // --- Boundary conditions ---

    @Test
    fun `an authorised query against empty persistence evaluates only the act-level gate and returns an empty Retrieved`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val engine = approvingEngine()
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "anything"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(emptyList<Any>(), retrieved.result.entries)
        assertEquals(1, engine.evaluateCallCount, "only the act-level gate is evaluated when nothing matches")
    }

    @Test
    fun `maximumResults bounds the permission-approved set, not the pre-filter candidate set`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery one"))
        persistence.store(item(KnowledgeId("k2"), basis = "grocery two"))
        persistence.store(item(KnowledgeId("k3"), basis = "grocery three"))
        val deniedId = KnowledgeId("k1")
        val engine = FakePermissionEngine { request ->
            val outcome = if (request.intent.contains(deniedId.value)) PermissionDecisionOutcome.DENIED else PermissionDecisionOutcome.APPROVED
            decision(request, outcome)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery", maximumResults = 1))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            listOf(KnowledgeId("k2")),
            retrieved.result.entries.map { it.item.knowledgeId },
            "k1 was denied, so bounding to 1 must yield k2 (the next approved item in order), not an empty result",
        )
    }

    // --- Structural: three constructor dependencies, no Memory Core dependency ---

    @Test
    fun `DefaultKnowledgeRetrieval holds exactly four constructor dependencies -- KnowledgeItemPersistence, PermissionEngine, RelevanceMechanism, and a Clock`() {
        // Unit 9.7.4's own disclosed, deliberate widening of Unit 9.7.2's
        // own "exactly three" structural boundary. RelevanceMechanism.rank
        // is structurally impossible to invoke from retrieve() without a
        // constructor-level reference to one -- this codebase has no
        // service-locator or ambient-dependency pattern anywhere else --
        // so this Unit's own governing task's Objective item 1 ("invoke
        // the already-complete RelevanceMechanism ... on the lawful Unit
        // 9.7.2 fallback branch") requires this widening. The real QMD
        // instance is still not constructed anywhere in this class; that
        // remains Unit 9.7.5's own, later, separately authorised
        // composition-wiring responsibility (see the sibling test, below,
        // and this Unit's own delivery notes for the full disclosed
        // reasoning behind this deliberate exception to the Unit 9.7
        // Implementation Plan's own "Unit 9.7.2's own tests remain passing
        // unmodified" completion criterion).
        val constructor = requireNotNull(DefaultKnowledgeRetrieval::class.primaryConstructor)
        val parameterTypes = constructor.parameters.map { it.type.classifier }

        assertEquals(
            listOf(KnowledgeItemPersistence::class, PermissionEngine::class, RelevanceMechanism::class, Clock::class),
            parameterTypes,
        )
    }

    @Test
    fun `the clock parameter defaults, so a three-argument (persistence, permissionEngine, relevanceMechanism) construction site remains valid`() = runTest {
        // Unit 9.3 added `clock` with a default value; Unit 9.5 added a mandatory `permissionEngine`
        // between `persistence` and `clock`; Unit 9.7.4 adds a mandatory `relevanceMechanism` between
        // `permissionEngine` and `clock` -- a three-argument call now uses the real system clock
        // implicitly, exactly as a two-argument call did before this Unit's own addition. There is,
        // deliberately, no default `PermissionEngine` or `RelevanceMechanism` -- a default-approving
        // permission fallback would be self-authorisation, and a default relevance mechanism would be
        // silent, undisclosed configuration inference; neither is permitted by any governing document.
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "grocery list"))
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
    }

    @Test
    fun `no MemoryRetrieval or MemoryCore dependency exists anywhere on DefaultKnowledgeRetrieval`() {
        val declaredProperties = DefaultKnowledgeRetrieval::class.declaredMemberProperties.map { it.name.lowercase() }
        listOf("memoryretrieval", "memorycore").forEach { forbidden ->
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
    fun `no scoring, weighting, embedding, or similarity computation exists anywhere on DefaultKnowledgeRetrieval`() {
        // Windows Verification Failure (Task D, bounded correction): this test was originally named
        // "no ranking, scoring, or weighting method exists anywhere on DefaultKnowledgeRetrieval" and
        // forbade the substrings "rank", "score", "weight", "embedding", "vector", "semantic", and
        // "similarity" anywhere in this class's own declared functions/properties. Unit 9.7.4's own
        // lawful `resolveSemanticResult` private method (which orchestrates -- never computes -- the
        // mechanism-ranked result) trips the "semantic" substring alone; no other declared member
        // trips any other forbidden substring, confirmed by direct inspection.
        //
        // Per this Unit's own governing task (Phase 3): the original test conflated two different
        // rules. Rule A -- the permanent invariant -- is that DefaultKnowledgeRetrieval must never
        // itself calculate a relevance score, weight, embedding, vector, or similarity value; it may
        // only ever receive already-ranked opaque tokens through the injected RelevanceMechanism
        // contract. Rule B -- now obsolete -- was the pre-Unit-9.7 rule that this class could never
        // participate in ranked semantic-result handling at all, including by orchestration. Unit
        // 9.7.4 lawfully requires exactly the latter (invoking RelevanceMechanism.rank(), receiving
        // and preserving its ranked token order), so a blanket "rank"/"semantic" substring ban is now
        // an obsolete phase-specific assertion, not a permanent one.
        //
        // This test is narrowed, not deleted: "score", "weight", "embedding", "vector", and
        // "similarity" remain absolutely forbidden (Parker-owned computation of any of these would be
        // exactly the violation adopted governance prohibits). The word "rank" and the substring
        // "semantic" are removed from this blanket ban -- the permanent invariant they were meant to
        // protect is instead proven precisely by the companion test, below, which confirms
        // DefaultKnowledgeRetrieval never declares its own function literally named "rank" (as
        // opposed to invoking the injected RelevanceMechanism's own `rank()`).
        val declaredNames = (DefaultKnowledgeRetrieval::class.declaredFunctions.map { it.name } +
            DefaultKnowledgeRetrieval::class.declaredMemberProperties.map { it.name }).map { it.lowercase() }
        listOf("score", "weight", "embedding", "vector", "similarity").forEach { forbidden ->
            assertFalse(
                declaredNames.any { it.contains(forbidden) },
                "DefaultKnowledgeRetrieval must not declare '$forbidden' -- found: $declaredNames",
            )
        }
    }

    @Test
    fun `DefaultKnowledgeRetrieval never declares its own function literally named rank -- semantic ordering is received only through the RelevanceMechanism contract`() {
        // The successor, permanent-invariant-preserving half of the structural test above: proves
        // DefaultKnowledgeRetrieval contains no ranking algorithm of its own. This class does invoke
        // `relevanceMechanism.rank(...)` (an external call on the injected RelevanceMechanism, not a
        // function this class declares), and its own private `resolveSemanticResult` preserves --
        // never recomputes -- the mechanism's own returned token order. Neither is a self-declared
        // function named exactly "rank"; this assertion targets exactly that narrower, permanent fact.
        val declaredFunctionNames = DefaultKnowledgeRetrieval::class.declaredFunctions.map { it.name }
        assertFalse(
            declaredFunctionNames.any { it.equals("rank", ignoreCase = true) },
            "DefaultKnowledgeRetrieval must never declare its own function literally named 'rank' -- " +
                "semantic ordering must be received only through the RelevanceMechanism contract, never " +
                "computed internally -- found: $declaredFunctionNames",
        )
    }

    // --- Unit 9.7.2: fallback trigger ---

    @Test
    fun `one or more structural matches prevent fallback, regardless of the permission outcome on those matches`() = runTest {
        // Frozen Boundary #2 / adopted Proposal §16: "Permission denial is
        // not a fallback trigger." A DENIED item-level outcome on the sole
        // structural match must still short-circuit before the wider
        // eligible set is ever permission-gated -- proven here by the exact
        // evaluated intents, not merely by the final (empty) result, so
        // this test cannot be satisfied merely by fallback happening to
        // also produce nothing.
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("matched"), basis = "grocery list"))
        persistence.store(item(KnowledgeId("eligible-but-unmatched"), basis = "unrelated household task"))
        val evaluatedIntents = mutableListOf<String>()
        // Defect fix (Windows Verification Failure, bounded correction): the
        // previous version of this fake denied EVERY evaluation, including
        // the act-level gate -- so retrieve() returned NotAuthorised before
        // ever reaching the structural/fallback branching logic this test
        // exists to prove, and assertIs<Retrieved> failed for a reason
        // wholly unrelated to fallback triggering. The act-level gate must
        // approve here (mirroring the existing actLevelApprovingEngine
        // helper's own established two-tier distinction, above); only the
        // item-level evaluation of the sole structural match is denied.
        val engine = FakePermissionEngine { request ->
            evaluatedIntents.add(request.intent)
            val outcome = if (request.intent == DefaultKnowledgeRetrieval.ACT_LEVEL_INTENT) {
                PermissionDecisionOutcome.APPROVED
            } else {
                PermissionDecisionOutcome.DENIED
            }
            decision(request, outcome)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            emptyList<Any>(),
            retrieved.result.entries,
            "the sole structural match was denied at item level -- this must not be conflated with a genuine zero-structural-match fallback trigger",
        )
        assertEquals(2, evaluatedIntents.size, "1 act-level + 1 item-level, for the structurally matched item only")
        assertFalse(
            evaluatedIntents.any { it.contains("eligible-but-unmatched") },
            "an item that was lifecycle-eligible but never structurally matched must never be permission-evaluated while at least one structural match exists",
        )
    }

    @Test
    fun `a thrown exception during persistence read never reaches the fallback branch`() = runTest {
        // Frozen Boundary #2 / adopted Proposal §16: a structural-matching
        // failure is never treated as a legitimate zero-match outcome.
        val persistence = ThrowingKnowledgeItemPersistence()
        val evaluatedIntents = mutableListOf<String>()
        val engine = FakePermissionEngine { request ->
            evaluatedIntents.add(request.intent)
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        assertFailsWith<IllegalStateException> {
            retrieval.retrieve(principal, query(relevance = "grocery"))
        }
        assertEquals(
            listOf(DefaultKnowledgeRetrieval.ACT_LEVEL_INTENT),
            evaluatedIntents,
            "only the act-level gate may run before a persistence-read failure propagates -- no item-level evaluation, fallback or otherwise, may occur",
        )
    }

    // --- Unit 9.7.2: closed candidate set ---

    @Test
    fun `the fallback branch permission-gates the full lifecycle-eligible set, and only that set, when structural matching finds nothing`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated one"))
        persistence.store(item(KnowledgeId("k2"), basis = "unrelated two"))
        persistence.store(item(KnowledgeId("retired"), basis = "unrelated retired", status = KnowledgeItemStatus.RETIRED))
        val evaluatedIntents = mutableListOf<String>()
        val engine = FakePermissionEngine { request ->
            evaluatedIntents.add(request.intent)
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, emptyResultRelevanceMechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertEquals(
            3,
            evaluatedIntents.size,
            "1 act-level + 2 item-level -- the two lifecycle-eligible items, even though structural matching found zero relevant candidates",
        )
        assertTrue(evaluatedIntents.any { it.contains("k1") })
        assertTrue(evaluatedIntents.any { it.contains("k2") })
        assertFalse(
            evaluatedIntents.any { it.contains("retired") },
            "a RETIRED, lifecycle-ineligible item must never enter even the fallback candidate-set permission gate",
        )
    }

    @Test
    fun `an empty lifecycle-eligible set never triggers fallback candidate-set permission gating`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val engine = approvingEngine()
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, neverInvokedRelevanceMechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(emptyList<Any>(), retrieved.result.entries)
        assertEquals(1, engine.evaluateCallCount, "no lifecycle-eligible items exist at all -- only the act-level gate is ever evaluated")
    }

    @Test
    fun `a denied item within the fallback-eligible set does not prevent fallback candidate-set construction from completing`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val deniedId = KnowledgeId("denied")
        val approvedId = KnowledgeId("approved")
        persistence.store(item(deniedId, basis = "unrelated denied"))
        persistence.store(item(approvedId, basis = "unrelated approved"))
        val engine = FakePermissionEngine { request ->
            val outcome = if (request.intent.contains(deniedId.value)) PermissionDecisionOutcome.DENIED else PermissionDecisionOutcome.APPROVED
            decision(request, outcome)
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, emptyResultRelevanceMechanism)

        // Unit 9.7.2 does not yet disclose any fallback result -- entries
        // remain empty regardless of how many candidates survive fallback
        // permission gating (disclosure is Unit 9.7.4's own, later
        // responsibility). What this test proves is narrower and already
        // fully in this Unit's own scope: fallback candidate construction
        // completes without error over a permission-mixed eligible set,
        // reusing permissionApprove's own already-tested filtering logic
        // rather than assuming every fallback batch is uniformly approved.
        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(emptyList<Any>(), retrieved.result.entries)
    }

    // --- Unit 9.7.2: opaque token / minimum content minting (private, reflection-verified) ---

    @Test
    fun `mintFallbackCandidates mints exactly one candidate per supplied item, content equal to its own most recent basis`() {
        val retrieval = DefaultKnowledgeRetrieval(InMemoryKnowledgeItemPersistence(), approvingEngine(), neverInvokedRelevanceMechanism)
        val items = listOf(
            item(KnowledgeId("k1"), basis = "grocery list one"),
            item(KnowledgeId("k2"), basis = "grocery list two"),
        )

        val candidates = mintFallbackCandidatesForTest(retrieval, items)

        assertEquals(2, candidates.size)
        assertEquals(listOf("grocery list one", "grocery list two"), candidates.map { it.content })
    }

    @Test
    fun `mintFallbackCandidates tokens never equal or contain the item's own KnowledgeId`() {
        val retrieval = DefaultKnowledgeRetrieval(InMemoryKnowledgeItemPersistence(), approvingEngine(), neverInvokedRelevanceMechanism)
        val items = listOf(item(KnowledgeId("secret-knowledge-id-42"), basis = "grocery list"))

        val candidates = mintFallbackCandidatesForTest(retrieval, items)

        val tokenValue = candidates.single().token.value
        assertFalse(tokenValue.contains("secret-knowledge-id-42"), "token must not encode or contain the canonical KnowledgeId")
        assertTrue(tokenValue != "secret-knowledge-id-42")
    }

    @Test
    fun `mintFallbackCandidates mints a fresh, distinct token on every separate call -- no stable identity across retrieval preparations`() {
        val retrieval = DefaultKnowledgeRetrieval(InMemoryKnowledgeItemPersistence(), approvingEngine(), neverInvokedRelevanceMechanism)
        val items = listOf(item(KnowledgeId("k1"), basis = "grocery list"))

        val first = mintFallbackCandidatesForTest(retrieval, items).single().token.value
        val second = mintFallbackCandidatesForTest(retrieval, items).single().token.value

        assertTrue(first != second, "two separate fallback candidate-set constructions for the same item must not reuse the same token")
    }

    @Test
    fun `mintFallbackCandidates produces distinct tokens for distinct candidates in the same call`() {
        val retrieval = DefaultKnowledgeRetrieval(InMemoryKnowledgeItemPersistence(), approvingEngine(), neverInvokedRelevanceMechanism)
        val items = listOf(item(KnowledgeId("k1"), basis = "one"), item(KnowledgeId("k2"), basis = "two"))

        val tokens = mintFallbackCandidatesForTest(retrieval, items).map { it.token.value }

        assertEquals(tokens.size, tokens.toSet().size, "every candidate minted in the same request must receive its own distinct token")
    }

    @Test
    fun `mintFallbackCandidates over an empty closed candidate set mints nothing`() {
        val retrieval = DefaultKnowledgeRetrieval(InMemoryKnowledgeItemPersistence(), approvingEngine(), neverInvokedRelevanceMechanism)

        val candidates = mintFallbackCandidatesForTest(retrieval, emptyList())

        assertEquals(emptyList<RelevanceCandidate>(), candidates)
    }

    // --- Unit 9.7.2: no persistent relevance state, no mechanism invocation ---

    @Test
    fun `no class-level field holds fallback candidate, token, or token-map state between retrieve calls`() {
        val declaredProperties = DefaultKnowledgeRetrieval::class.declaredMemberProperties.map { it.name.lowercase() }
        listOf("token", "candidate", "fallback").forEach { forbidden ->
            assertFalse(
                declaredProperties.any { it.contains(forbidden) },
                "DefaultKnowledgeRetrieval must not declare a class-level property resembling '$forbidden' -- found: $declaredProperties",
            )
        }
    }

    @Test
    fun `DefaultKnowledgeRetrieval now declares exactly one RelevanceMechanism-typed constructor dependency -- Unit 9-7-4's own disclosed boundary change`() {
        // This test originally asserted the opposite (Boundary Review item
        // 6, Unit 9.7.2's own governing task: "this Unit must not invoke
        // RelevanceMechanism.rank()"). That boundary was correct and
        // enforced for Unit 9.7.2's own narrower scope. Unit 9.7.4's own
        // governing task requires DefaultKnowledgeRetrieval to actually
        // invoke RelevanceMechanism.rank() on the lawful fallback branch --
        // structurally impossible without a constructor-level reference,
        // since this codebase has no service-locator or ambient-dependency
        // pattern anywhere else (mirroring how `permissionEngine` and
        // `persistence` are reached). This assertion is flipped, not
        // deleted, so the boundary this test protects remains continuously
        // enforced at exactly one RelevanceMechanism-typed dependency --
        // never zero (Unit 9.7.2's own now-superseded boundary) and never
        // more than one (no second, redundant, or differently-scoped
        // mechanism reference). QmdRelevanceMechanism itself remains
        // completely untouched by this Unit; only this class's own
        // constructor shape changed.
        val constructor = requireNotNull(DefaultKnowledgeRetrieval::class.primaryConstructor)
        val parameterTypeNames = constructor.parameters.map { it.type.toString().lowercase() }
        assertEquals(
            1,
            parameterTypeNames.count { it.contains("relevancemechanism") },
            "found: $parameterTypeNames",
        )
    }

    // ================================================================
    // Unit 9.7.4: Integrity Validation, Canonical Token Re-resolution,
    // and Fresh Pre-disclosure Re-verification
    // ================================================================

    // --- A. Mechanism invocation ---

    @Test
    fun `RelevanceMechanism-rank is invoked exactly once when structural matching finds nothing and the closed candidate set is non-empty`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated basis text"))
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertEquals(1, mechanism.rankCallCount)
    }

    @Test
    fun `RelevanceMechanism-rank is never invoked when one or more structural matches exist`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("matched"), basis = "grocery list"))
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(0, mechanism.rankCallCount)
    }

    @Test
    fun `RelevanceMechanism-rank is never invoked when the closed candidate set is empty`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated basis text"))
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        // Every item-level evaluation is denied -- the closed candidate set is empty even though the
        // lifecycle-eligible set is not.
        val retrieval = DefaultKnowledgeRetrieval(persistence, actLevelApprovingEngine(PermissionDecisionOutcome.DENIED), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(emptyList<Any>(), retrieved.result.entries)
        assertEquals(0, mechanism.rankCallCount)
    }

    @Test
    fun `a persistence-read failure propagates before RelevanceMechanism could ever be invoked`() = runTest {
        val mechanism = FakeRelevanceMechanism { error("must not be invoked -- structural matching itself threw first") }
        val retrieval = DefaultKnowledgeRetrieval(ThrowingKnowledgeItemPersistence(), approvingEngine(), mechanism)

        assertFailsWith<IllegalStateException> {
            retrieval.retrieve(principal, query(relevance = "grocery"))
        }
        assertEquals(0, mechanism.rankCallCount)
    }

    // --- B. Integrity validation ---

    @Test
    fun `an unknown token returned by the mechanism fails the whole retrieval closed`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated basis text"))
        val mechanism = FakeRelevanceMechanism { _ ->
            RelevanceResult(rankedTokens = listOf(RelevanceCandidateToken("unknown-token-never-minted")))
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val error = assertFailsWith<IllegalStateException> {
            retrieval.retrieve(principal, query(relevance = "grocery"))
        }
        assertTrue(error.message!!.contains("not a member of this request's own closed candidate set"))
    }

    @Test
    fun `a duplicate token returned by the mechanism fails the whole retrieval closed, never silently de-duplicated`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated one"))
        persistence.store(item(KnowledgeId("k2"), basis = "unrelated two"))
        val mechanism = FakeRelevanceMechanism { request ->
            val token = request.candidates.first().token
            RelevanceResult(rankedTokens = listOf(token, token))
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val error = assertFailsWith<IllegalStateException> {
            retrieval.retrieve(principal, query(relevance = "grocery"))
        }
        assertTrue(error.message!!.contains("duplicate token"))
    }

    @Test
    fun `more tokens returned than were supplied fails the whole retrieval closed`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated basis text"))
        val mechanism = FakeRelevanceMechanism { request ->
            val token = request.candidates.single().token
            RelevanceResult(rankedTokens = listOf(token, RelevanceCandidateToken("a-second-token-never-supplied")))
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val error = assertFailsWith<IllegalStateException> {
            retrieval.retrieve(principal, query(relevance = "grocery"))
        }
        assertTrue(error.message!!.contains("more tokens"))
    }

    @Test
    fun `a token minted for a different, earlier retrieve call is rejected as unknown to this request`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated basis text"))
        var capturedStaleToken: RelevanceCandidateToken? = null
        val capturingMechanism = FakeRelevanceMechanism { request ->
            capturedStaleToken = request.candidates.single().token
            RelevanceResult(rankedTokens = request.candidates.map { it.token })
        }
        val firstRetrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), capturingMechanism)
        firstRetrieval.retrieve(principal, query(relevance = "grocery"))
        val staleToken = requireNotNull(capturedStaleToken)

        val replayingMechanism = FakeRelevanceMechanism { _ -> RelevanceResult(rankedTokens = listOf(staleToken)) }
        val secondRetrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), replayingMechanism)

        val error = assertFailsWith<IllegalStateException> {
            secondRetrieval.retrieve(principal, query(relevance = "grocery"))
        }
        assertTrue(error.message!!.contains("not a member of this request's own closed candidate set"))
    }

    @Test
    fun `an integrity fault raises an exception -- it is never converted into a successful empty Retrieved result`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated basis text"))
        val mechanism = FakeRelevanceMechanism { _ ->
            RelevanceResult(rankedTokens = listOf(RelevanceCandidateToken("unknown-token")))
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        // The defect this proves against: silently catching the integrity fault and returning
        // Retrieved(KnowledgeRetrievalResult(emptyList())) instead of propagating it -- indistinguishable,
        // at the disposition type level, from a genuine "found nothing relevant" outcome.
        assertFailsWith<IllegalStateException> {
            retrieval.retrieve(principal, query(relevance = "grocery"))
        }
    }

    // --- C. Canonical re-resolution ---

    @Test
    fun `each returned token resolves to its own item via the request-local map, never a different candidate's item`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated one"))
        persistence.store(item(KnowledgeId("k2"), basis = "unrelated two"))
        val mechanism = FakeRelevanceMechanism { request ->
            // QMD-ranked order deliberately reversed relative to supplied order.
            RelevanceResult(rankedTokens = request.candidates.map { it.token }.reversed())
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            listOf(KnowledgeId("k2"), KnowledgeId("k1")),
            retrieved.result.entries.map { it.item.knowledgeId },
            "each token must resolve back to its own originally-minted item, in the mechanism's own ranked order",
        )
    }

    @Test
    fun `Stage C performs a fresh persistence-find call for every mechanism-surfaced token, after the mechanism result returns`() = runTest {
        val knowledgeId = KnowledgeId("k1")
        val storedItem = item(knowledgeId, basis = "unrelated basis text")
        val persistence = ScriptedFindKnowledgeItemPersistence { id -> if (id == knowledgeId) storedItem else null }
        persistence.store(storedItem)
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        assertEquals(1, persistence.findCallCount, "the surviving candidate's own KnowledgeId must be freshly looked up exactly once")
        assertEquals(listOf(knowledgeId), persistence.findCalledWith)
    }

    @Test
    fun `a KnowledgeItem removed from persistence between Pre-computation and Pre-disclosure is excluded, never disclosed`() = runTest {
        val knowledgeId = KnowledgeId("k1")
        val storedItem = item(knowledgeId, basis = "unrelated basis text")
        val persistence = ScriptedFindKnowledgeItemPersistence { _ -> null }
        persistence.store(storedItem)
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            emptyList<Any>(),
            retrieved.result.entries,
            "a candidate that no longer resolves via a fresh find must be excluded, never substituted for",
        )
    }

    @Test
    fun `the disclosed entry reflects the fresh find result, never the earlier Pre-computation snapshot`() = runTest {
        val knowledgeId = KnowledgeId("k1")
        val assertionEvidence = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val staleSnapshot = item(knowledgeId, basis = "unrelated basis text")
        val freshItem = item(
            knowledgeId,
            basis = "unused",
            history = listOf(
                KnowledgePromotion(
                    knowledgeId = knowledgeId,
                    evidenceReference = assertionEvidence,
                    resultingState = EvidentialState.UNKNOWN,
                    occurredAt = Instant.parse("2026-05-01T00:00:00Z"),
                    basis = "unrelated basis text",
                ),
                KnowledgePromotion(
                    knowledgeId = knowledgeId,
                    evidenceReference = assertionEvidence,
                    resultingState = EvidentialState.UNKNOWN,
                    occurredAt = Instant.parse("2026-06-01T00:00:00Z"),
                    basis = "a freshly revised classification, written after Pre-computation captured its own snapshot",
                ),
            ),
        )
        val persistence = ScriptedFindKnowledgeItemPersistence { id -> if (id == knowledgeId) freshItem else null }
        persistence.store(staleSnapshot)
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            freshItem,
            retrieved.result.entries.single().item,
            "the final entry must be built from the fresh find result, never the Pre-computation snapshot",
        )
    }

    // --- D. Fresh permission re-verification ---

    @Test
    fun `Stage C's permission check is a genuinely fresh, second evaluation -- not a reuse of the Pre-computation decision`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated basis text"))
        val evaluatedIntents = mutableListOf<String>()
        val engine = FakePermissionEngine { request ->
            evaluatedIntents.add(request.intent)
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, mechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        val itemLevelEvaluations = evaluatedIntents.count { it.contains("k1") }
        assertEquals(
            2,
            itemLevelEvaluations,
            "the sole eligible item must be item-level evaluated twice -- once for Unit 9.7.2's own " +
                "Pre-computation closed-candidate-set gate, and once more, freshly, for Unit 9.7.4's " +
                "own Pre-disclosure re-verification",
        )
    }

    @Test
    fun `a candidate approved during Pre-computation but denied before disclosure is excluded, never disclosed`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated basis text"))
        var itemLevelCallCount = 0
        val engine = FakePermissionEngine { request ->
            if (request.intent == DefaultKnowledgeRetrieval.ACT_LEVEL_INTENT) {
                decision(request, PermissionDecisionOutcome.APPROVED)
            } else {
                itemLevelCallCount++
                // Approved the first (Pre-computation) time, denied the second (Pre-disclosure) time.
                val outcome = if (itemLevelCallCount == 1) PermissionDecisionOutcome.APPROVED else PermissionDecisionOutcome.DENIED
                decision(request, outcome)
            }
        }
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(emptyList<Any>(), retrieved.result.entries)
    }

    @Test
    fun `the Pre-disclosure permission check reuses the identical item-level intent, resource, and action -- no new permission pathway`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated basis text"))
        val capturedRequests = mutableListOf<ExecutionRequest>()
        val engine = FakePermissionEngine { request ->
            capturedRequests.add(request)
            decision(request, PermissionDecisionOutcome.APPROVED)
        }
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, engine, mechanism)

        retrieval.retrieve(principal, query(relevance = "grocery"))

        // 1 act-level + 1 Pre-computation item-level + 1 Pre-disclosure item-level.
        assertEquals(3, capturedRequests.size)
        val itemLevelRequests = capturedRequests.drop(1)
        itemLevelRequests.forEach { request ->
            assertEquals(listOf(DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID), request.targetResources)
            assertEquals(listOf(DefaultKnowledgeRetrieval.RETRIEVE_ACTION_NAME), request.proposedActions)
            assertTrue(request.intent.contains("k1"))
        }
        assertEquals(
            itemLevelRequests[0].intent,
            itemLevelRequests[1].intent,
            "both item-level evaluations must name the identical intent text",
        )
    }

    // --- E. Lifecycle / currentness re-verification ---

    @Test
    fun `a candidate that becomes lifecycle-ineligible between Pre-computation and Pre-disclosure is excluded`() = runTest {
        val knowledgeId = KnowledgeId("k1")
        val activeSnapshot = item(knowledgeId, basis = "unrelated basis text", status = KnowledgeItemStatus.ACTIVE)
        val retiredNow = item(knowledgeId, basis = "unrelated basis text", status = KnowledgeItemStatus.RETIRED)
        val persistence = ScriptedFindKnowledgeItemPersistence { id -> if (id == knowledgeId) retiredNow else null }
        persistence.store(activeSnapshot)
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(emptyList<Any>(), retrieved.result.entries)
    }

    @Test
    fun `an unchanged, still-eligible candidate survives the full Stage B-plus-C pipeline and is disclosed`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val storedItem = item(KnowledgeId("k1"), basis = "unrelated basis text")
        persistence.store(storedItem)
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(listOf(storedItem), retrieved.result.entries.map { it.item })
        assertEquals(1, mechanism.rankCallCount)
    }

    // --- F. Ordering and bounding ---

    @Test
    fun `surviving candidates are disclosed in the mechanism's own ranked order, not persistence insertion order`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated one"))
        persistence.store(item(KnowledgeId("k2"), basis = "unrelated two"))
        persistence.store(item(KnowledgeId("k3"), basis = "unrelated three"))
        val mechanism = FakeRelevanceMechanism { request ->
            fun tokenFor(basisSuffix: String) = request.candidates.first { it.content == "unrelated $basisSuffix" }.token
            RelevanceResult(rankedTokens = listOf(tokenFor("three"), tokenFor("one"), tokenFor("two")))
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            listOf(KnowledgeId("k3"), KnowledgeId("k1"), KnowledgeId("k2")),
            retrieved.result.entries.map { it.item.knowledgeId },
        )
    }

    @Test
    fun `maximumResults bounds the surviving, re-verified semantic candidates at the same governed stage as the structural path`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), basis = "unrelated one"))
        persistence.store(item(KnowledgeId("k2"), basis = "unrelated two"))
        persistence.store(item(KnowledgeId("k3"), basis = "unrelated three"))
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery", maximumResults = 2))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(2, retrieved.result.entries.size)
        assertEquals(listOf(KnowledgeId("k1"), KnowledgeId("k2")), retrieved.result.entries.map { it.item.knowledgeId })
    }

    @Test
    fun `a candidate excluded by fresh re-verification does not reorder or replace the remaining survivors' own content`() = runTest {
        val excludedId = KnowledgeId("excluded")
        val survivorAId = KnowledgeId("survivor-a")
        val survivorBId = KnowledgeId("survivor-b")
        val excludedItem = item(excludedId, basis = "unrelated excluded")
        val survivorA = item(survivorAId, basis = "unrelated survivor a")
        val survivorB = item(survivorBId, basis = "unrelated survivor b")
        val persistence = ScriptedFindKnowledgeItemPersistence { id ->
            when (id) {
                excludedId -> null // removed between Pre-computation and Pre-disclosure
                survivorAId -> survivorA
                survivorBId -> survivorB
                else -> null
            }
        }
        persistence.store(excludedItem)
        persistence.store(survivorA)
        persistence.store(survivorB)
        val mechanism = FakeRelevanceMechanism { request ->
            fun tokenFor(basisSuffix: String) = request.candidates.first { it.content == "unrelated $basisSuffix" }.token
            // QMD ranks the soon-to-be-excluded candidate first.
            RelevanceResult(rankedTokens = listOf(tokenFor("excluded"), tokenFor("survivor a"), tokenFor("survivor b")))
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            listOf(survivorAId, survivorBId),
            retrieved.result.entries.map { it.item.knowledgeId },
            "the excluded candidate's own removal must not reorder or substitute the remaining survivors",
        )
    }

    // --- G. Content and authority boundaries ---

    @Test
    fun `the disclosed entry carries the full, unprojected fresh KnowledgeItem, exactly as the structural path already does`() = runTest {
        val knowledgeId = KnowledgeId("k1")
        val assertionEvidence = MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-k1"))
        val chain = (1..3).map { hop ->
            KnowledgePromotion(
                knowledgeId = knowledgeId,
                evidenceReference = assertionEvidence,
                resultingState = EvidentialState.UNKNOWN,
                occurredAt = Instant.parse("2026-0$hop-01T00:00:00Z"),
                basis = "unrelated hop $hop",
            )
        }
        val freshItem = item(knowledgeId, basis = "unused", history = chain)
        val persistence = ScriptedFindKnowledgeItemPersistence { id -> if (id == knowledgeId) freshItem else null }
        persistence.store(item(knowledgeId, basis = "unused", history = chain))
        val mechanism = FakeRelevanceMechanism { request -> RelevanceResult(rankedTokens = request.candidates.map { it.token }) }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(freshItem, retrieved.result.entries.single().item)
        assertEquals(3, retrieved.result.entries.single().item.history.size, "the full history chain must remain intact, never truncated")
    }

    @Test
    fun `RelevanceCandidate content sent to the mechanism never becomes the final disclosed content`() = runTest {
        val knowledgeId = KnowledgeId("k1")
        val preComputationBasis = "unrelated pre-computation basis"
        val freshBasis = "a materially different fresh basis, written after Pre-computation captured its own snapshot"
        val staleSnapshot = item(knowledgeId, basis = preComputationBasis)
        val freshItem = item(knowledgeId, basis = freshBasis)
        var capturedCandidateContent: String? = null
        val persistence = ScriptedFindKnowledgeItemPersistence { id -> if (id == knowledgeId) freshItem else null }
        persistence.store(staleSnapshot)
        val mechanism = FakeRelevanceMechanism { request ->
            capturedCandidateContent = request.candidates.single().content
            RelevanceResult(rankedTokens = request.candidates.map { it.token })
        }
        val retrieval = DefaultKnowledgeRetrieval(persistence, approvingEngine(), mechanism)

        val disposition = retrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            preComputationBasis,
            capturedCandidateContent,
            "the mechanism must still see the Pre-computation content it was actually asked to rank",
        )
        assertEquals(
            freshBasis,
            retrieved.result.entries.single().item.history.last().basis,
            "the disclosed entry's own content must reflect the fresh find, never the RelevanceCandidate.content the mechanism was sent",
        )
    }

    // --- H. Regressions ---
    // Categories A-G's own tests, above, exercise Unit 9.7.4 exclusively through the pre-existing,
    // now four-argument DefaultKnowledgeRetrieval constructor and the unchanged retrieve() entry
    // point. No pre-existing Unit 9.2/9.3/9.4/9.5/9.7.2 test assertion was altered anywhere in this
    // file -- every one of those call sites received only a mechanical, behaviour-preserving
    // relevanceMechanism argument (neverInvokedRelevanceMechanism for every fixture that must not
    // reach mechanism invocation at all; emptyResultRelevanceMechanism for every fixture whose own
    // fallback branch does reach a non-empty closed candidate set). The two exceptions -- the former
    // "exactly three constructor dependencies" and "declares no RelevanceMechanism-typed constructor
    // dependency" structural tests -- are deliberately and visibly modified, not silently
    // preserved-but-defeated, exactly as this Unit's own delivery notes disclose.
    //
    // Windows Verification Failure (Task D, bounded correction): the original delivery's own
    // categorization of this split (Task B) was incomplete -- it identified only the two Unit 9.7.2
    // "closed candidate set" tests as reaching the fallback branch with a non-empty closed candidate
    // set, missing three further pre-existing tests (in the "Matching" and "Staleness disclosure"
    // sections, above) whose own fixture data also structurally satisfies that same condition
    // (structurallyMatched.isEmpty() && closedCandidateSet.isNotEmpty()) wherever a stored item's
    // most recent basis does not happen to contain the query's own relevance text. A full,
    // systematic re-audit of every DefaultKnowledgeRetrieval(...) call site in this file (not merely
    // the ones Windows reported failing) confirmed exactly five such call sites in total -- the two
    // originally identified, plus the three corrected here, each carrying its own disclosed
    // "Windows Verification Failure (Task D...)" comment at its call site -- and confirmed no other
    // call site in this file exhibits the same pattern. The production retrieve()/resolveSemanticResult
    // logic was not touched by this correction: every fix here is scoped entirely to which pre-built
    // fake RelevanceMechanism a given test's own fixture supplies.
}

/**
 * Test-only double whose [findAll] always throws, so tests can prove a
 * genuine structural-matching/persistence-read failure propagates as a
 * thrown exception rather than silently falling through to Unit 9.7.2's
 * own fallback branch as though it were a legitimate zero-match outcome
 * (adopted Proposal §16: "A structural retrieval failure or exception is
 * not an empty result").
 */
private class ThrowingKnowledgeItemPersistence : KnowledgeItemPersistence {
    override suspend fun store(item: KnowledgeItem): KnowledgeItem = error("not used by this test")
    override suspend fun find(knowledgeId: KnowledgeId): KnowledgeItem? = error("not used by this test")
    override suspend fun findAll(): List<KnowledgeItem> = error("simulated persistence-read failure")
}

/**
 * Reflection helper for Unit 9.7.2's own private `mintFallbackCandidates`.
 * Deliberately reflective rather than promoting the method to `internal`
 * or `public`: [DefaultKnowledgeRetrieval] keeps every one of its helper
 * methods `private` (mirroring [matches], [isRetrievable], and
 * [disclosureFor] already), and this suite already establishes the
 * precedent of using `kotlin.reflect` to verify a private member's
 * behaviour without widening the class's own visible surface (see the
 * existing structural tests, above, which reflect on
 * [DefaultKnowledgeRetrieval]'s declared functions/properties/constructor).
 * `mintFallbackCandidates` is deliberately a plain, non-`suspend` function,
 * so an ordinary [kotlin.reflect.KFunction.call] -- no `Continuation`
 * plumbing -- is sufficient here. Its return type
 * (`Pair<List<RelevanceCandidate>, Map<RelevanceCandidateToken, KnowledgeItem>>`)
 * is composed entirely of already-public types, so only the first element
 * of the pair needs extracting; the token-to-item map itself is exercised
 * indirectly, by confirming every returned candidate's own token is fresh
 * and content-correct.
 */
@Suppress("UNCHECKED_CAST")
private fun mintFallbackCandidatesForTest(
    retrieval: DefaultKnowledgeRetrieval,
    items: List<KnowledgeItem>,
): List<RelevanceCandidate> {
    val function = DefaultKnowledgeRetrieval::class.declaredFunctions.single { it.name == "mintFallbackCandidates" }
    function.isAccessible = true
    val (candidates, _) = function.call(retrieval, items) as Pair<List<RelevanceCandidate>, Map<RelevanceCandidateToken, KnowledgeItem>>
    return candidates
}

/**
 * Test-only wrapper counting [findAll] invocations, so tests can prove an
 * act-level permission denial never reads persistence at all (the adopted
 * Unit 9 Permission Enforcement Mechanism Clarification §8 step 2's own
 * "must never read `KnowledgeItemPersistence` before it completes"
 * requirement). Delegates to a real [InMemoryKnowledgeItemPersistence] so
 * tests that only care about call counting still get real storage
 * semantics, mirroring [DefaultKnowledgeSubmissionTest]'s own
 * `FakeKnowledgeItemPersistence` shape exactly.
 */
private class CountingKnowledgeItemPersistence : KnowledgeItemPersistence {

    private val delegate = InMemoryKnowledgeItemPersistence()

    var findAllCallCount: Int = 0
        private set

    override suspend fun store(item: KnowledgeItem): KnowledgeItem = delegate.store(item)

    override suspend fun find(knowledgeId: KnowledgeId): KnowledgeItem? = delegate.find(knowledgeId)

    override suspend fun findAll(): List<KnowledgeItem> {
        findAllCallCount++
        return delegate.findAll()
    }
}

/**
 * Programme 3, Unit 9.7.4. A [KnowledgeItemPersistence] double whose
 * [find] result is fully scripted, independent of what [findAll] already
 * returned for the same [KnowledgeItemPersistence]. This is the only way,
 * from outside [DefaultKnowledgeRetrieval]'s own private state, to prove
 * that Stage C's fresh canonical re-resolution genuinely re-reads
 * [persistence] a second time after the mechanism returns, rather than
 * reusing the Pre-computation [KnowledgeItem] snapshot [findAll] already
 * produced -- [findAll] itself still delegates to a real
 * [InMemoryKnowledgeItemPersistence], so [store]d items remain genuinely
 * lifecycle-eligible and structurally unmatched exactly as any other test
 * fixture's own items are.
 */
private class ScriptedFindKnowledgeItemPersistence(
    private val findResultFor: (KnowledgeId) -> KnowledgeItem?,
) : KnowledgeItemPersistence {

    private val delegate = InMemoryKnowledgeItemPersistence()

    var findCallCount: Int = 0
        private set
    val findCalledWith = mutableListOf<KnowledgeId>()

    override suspend fun store(item: KnowledgeItem): KnowledgeItem = delegate.store(item)

    override suspend fun find(knowledgeId: KnowledgeId): KnowledgeItem? {
        findCallCount++
        findCalledWith.add(knowledgeId)
        return findResultFor(knowledgeId)
    }

    override suspend fun findAll(): List<KnowledgeItem> = delegate.findAll()
}

/**
 * Programme 3, Unit 9.7.4. A scriptable [RelevanceMechanism] fake --
 * mirroring [FakePermissionEngine]'s own established shape exactly -- so
 * this suite's own Unit 9.7.4 tests can supply arbitrary [RelevanceResult]
 * values, including ones a real mechanism would never produce (to prove
 * this class's own integrity validation), without ever depending on
 * [QmdRelevanceMechanism] or a live QMD subprocess. [resultFor] may also
 * simply throw, to prove a genuine mechanism fault propagates unchanged.
 */
private class FakeRelevanceMechanism(
    private val resultFor: (RelevanceRequest) -> RelevanceResult,
) : RelevanceMechanism {

    var rankCallCount: Int = 0
        private set
    val capturedRequests = mutableListOf<RelevanceRequest>()

    override suspend fun rank(request: RelevanceRequest): RelevanceResult {
        rankCallCount++
        capturedRequests.add(request)
        return resultFor(request)
    }
}
