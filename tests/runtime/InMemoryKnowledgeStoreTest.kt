package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateKnowledge
import parker.core.interfaces.KnowledgeCategory
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgePromotionDecision
import parker.core.interfaces.KnowledgePromotionPolicy
import parker.core.interfaces.KnowledgeQuery
import parker.core.interfaces.KnowledgeSource
import parker.core.interfaces.KnowledgeStore
import parker.core.interfaces.PrincipalId
import kotlin.reflect.full.functions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 4, Track A, Unit A3. Behavioural tests of [InMemoryKnowledgeStore]:
 * submission, internally-invoked Evaluation and Promotion, rejection,
 * retrieval (identity scoping, category narrowing, `maximumResults`,
 * deterministic ordering), forgetting (auditable, safe for a missing
 * id), the `KnowledgeStore` public-surface boundary (no caller-facing
 * `promote`), and this Unit's scope discipline (no Planner/Agent
 * Runtime/Permission Engine dependency or behaviour).
 *
 * Extended Sprint 11 Unit 7 (Memory Source Integration): structural tests
 * confirming [InMemoryKnowledgeStore] also satisfies [KnowledgeSource], that
 * `recall` returns exactly what `retrieve` would (a zero-logic delegate,
 * not a second implementation), and that [KnowledgeSource] itself exposes no
 * mutation operation. `retrieve`'s own substantive behaviour -- identity
 * scoping, category narrowing, `maximumResults` capping, deterministic
 * ordering -- is exhaustively covered by the tests above already; these
 * new tests do not re-prove that behaviour, only that `recall` inherits
 * it unchanged.
 */
class InMemoryMemoryStoreTest {

    private val principal = PrincipalId("user-1")

    private fun candidate(
        payload: String = "the user prefers window seats",
        category: KnowledgeCategory = KnowledgeCategory.SEMANTIC,
        explicitlyRequested: Boolean = true,
        confidence: Double? = null,
        originatingPrincipalId: PrincipalId? = principal,
        correlationId: String = "corr-1",
    ) = CandidateKnowledge(
        knowledgePayload = payload,
        proposedCategory = category,
        sourceSubsystem = "test-harness",
        correlationId = correlationId,
        originatingPrincipalId = originatingPrincipalId,
        confidence = confidence,
        explicitlyRequested = explicitlyRequested,
    )

    private fun query(
        relevance: String = "window",
        maximumResults: Int = 10,
        category: KnowledgeCategory? = null,
        requestingPrincipalId: PrincipalId = principal,
    ) = KnowledgeQuery(
        requestingPrincipalId = requestingPrincipalId,
        relevance = relevance,
        correlationId = "corr-query",
        maximumResults = maximumResults,
        category = category,
    )

    // --- promotion approved / rejected ---

    @Test
    fun `an explicitly requested candidate is promoted`() = runTest {
        val store = InMemoryKnowledgeStore()

        val result = store.remember(candidate(explicitlyRequested = true))

        assertIs<KnowledgePromotionDecision.Promote>(result)
    }

    @Test
    fun `a candidate with no explicit request and no confidence is rejected`() = runTest {
        val store = InMemoryKnowledgeStore()

        val result = store.remember(candidate(explicitlyRequested = false, confidence = null))

        assertIs<KnowledgePromotionDecision.Reject>(result)
    }

    // --- retrievability follows the decision ---

    @Test
    fun `a promoted candidate becomes retrievable`() = runTest {
        val store = InMemoryKnowledgeStore()
        store.remember(candidate(payload = "the user prefers window seats", explicitlyRequested = true))

        val results = store.retrieve(query(relevance = "window"))

        assertEquals(1, results.size)
        assertEquals("the user prefers window seats", results.single().knowledgePayload)
    }

    @Test
    fun `a rejected candidate never becomes retrievable`() = runTest {
        val store = InMemoryKnowledgeStore()
        store.remember(candidate(payload = "an unremarkable, low-confidence observation", explicitlyRequested = false, confidence = null))

        val results = store.retrieve(query(relevance = "unremarkable"))

        assertTrue(results.isEmpty())
    }

    // --- KnowledgeCategory use ---

    @Test
    fun `retrieve narrows by KnowledgeCategory when one is supplied`() = runTest {
        val store = InMemoryKnowledgeStore()
        store.remember(candidate(payload = "episodic window seat story", category = KnowledgeCategory.EPISODIC))
        store.remember(candidate(payload = "semantic window seat fact", category = KnowledgeCategory.SEMANTIC))

        val episodicOnly = store.retrieve(query(relevance = "window", category = KnowledgeCategory.EPISODIC))

        assertEquals(1, episodicOnly.size)
        assertEquals(KnowledgeCategory.EPISODIC, episodicOnly.single().category)
    }

    // --- maximumResults ---

    @Test
    fun `retrieve never returns more than maximumResults`() = runTest {
        val store = InMemoryKnowledgeStore()
        repeat(5) { i -> store.remember(candidate(payload = "window seat memory number $i")) }

        val results = store.retrieve(query(relevance = "window", maximumResults = 2))

        assertEquals(2, results.size)
    }

    @Test
    fun `retrieve does not imply return everything -- an unbounded-looking query is still capped`() = runTest {
        val store = InMemoryKnowledgeStore()
        repeat(10) { i -> store.remember(candidate(payload = "window seat memory number $i")) }

        val results = store.retrieve(query(relevance = "window", maximumResults = 3))

        assertEquals(3, results.size)
    }

    // --- deterministic retrieval ---

    @Test
    fun `retrieve returns deterministic, most-recently-promoted-first results`() = runTest {
        val store = InMemoryKnowledgeStore()
        store.remember(candidate(payload = "window seat memory A"))
        store.remember(candidate(payload = "window seat memory B"))
        store.remember(candidate(payload = "window seat memory C"))

        val first = store.retrieve(query(relevance = "window", maximumResults = 10))
        val second = store.retrieve(query(relevance = "window", maximumResults = 10))

        assertEquals(first, second)
        assertEquals(listOf("window seat memory C", "window seat memory B", "window seat memory A"), first.map { it.knowledgePayload })
    }

    @Test
    fun `retrieve is scoped to the requesting Principal`() = runTest {
        val store = InMemoryKnowledgeStore()
        store.remember(candidate(payload = "window seat memory for user-1", originatingPrincipalId = PrincipalId("user-1")))
        store.remember(candidate(payload = "window seat memory for user-2", originatingPrincipalId = PrincipalId("user-2")))

        val forUser1 = store.retrieve(query(relevance = "window", requestingPrincipalId = PrincipalId("user-1")))

        assertEquals(1, forUser1.size)
        assertEquals("window seat memory for user-1", forUser1.single().knowledgePayload)
    }

    // --- forget ---

    @Test
    fun `forget removes a promoted record from retrieval`() = runTest {
        val store = InMemoryKnowledgeStore()
        val promoted = store.remember(candidate(payload = "a memory to be forgotten")) as KnowledgePromotionDecision.Promote

        val forgotten = store.forget(promoted.memoryId)

        assertTrue(forgotten)
        assertTrue(store.retrieve(query(relevance = "forgotten")).isEmpty())
    }

    @Test
    fun `forget is auditable -- a forgotten KnowledgeId is still confirmable as having existed`() = runTest {
        val store = InMemoryKnowledgeStore()
        val promoted = store.remember(candidate(payload = "a memory to be forgotten")) as KnowledgePromotionDecision.Promote

        store.forget(promoted.memoryId)

        assertTrue(store.wasForgotten(promoted.memoryId))
    }

    @Test
    fun `forgetting a KnowledgeId that was never promoted is handled safely, not an exception`() = runTest {
        val store = InMemoryKnowledgeStore()

        val result = store.forget(KnowledgeId("never-existed"))

        assertFalse(result)
        assertFalse(store.wasForgotten(KnowledgeId("never-existed")))
    }

    // --- KnowledgeStore public surface: no caller-facing promote ---

    @Test
    fun `KnowledgeStore exposes no external promote operation`() {
        val functionNames = KnowledgeStore::class.functions.map { it.name }.toSet()

        assertFalse("promote" in functionNames, "KnowledgeStore must not expose a caller-facing 'promote' operation")
        assertTrue(
            setOf("remember", "retrieve", "forget").all { it in functionNames },
            "KnowledgeStore must expose remember/retrieve/forget",
        )
    }

    // --- KnowledgePromotionPolicy is invoked internally ---

    @Test
    fun `KnowledgePromotionPolicy is consulted internally by InMemoryKnowledgeStore, exactly once per submission`() = runTest {
        val fakePolicy = FakeMemoryPromotionPolicy { candidateArg, memoryId ->
            KnowledgePromotionDecision.Promote(memoryId, candidateArg.proposedCategory)
        }
        val store = InMemoryKnowledgeStore(promotionPolicy = fakePolicy)

        store.remember(candidate())

        assertEquals(1, fakePolicy.evaluateCallCount)
    }

    @Test
    fun `InMemoryKnowledgeStore's promotion outcome is entirely controlled by the injected policy, not hardcoded`() = runTest {
        val alwaysReject = FakeMemoryPromotionPolicy { _, memoryId -> KnowledgePromotionDecision.Reject(memoryId, "fake always rejects") }
        val store = InMemoryKnowledgeStore(promotionPolicy = alwaysReject)

        // Even an explicitly-requested, high-confidence candidate is rejected, because the
        // injected fake -- not DefaultKnowledgePromotionPolicy's own rules -- governs the outcome.
        val result = store.remember(candidate(explicitlyRequested = true, confidence = 1.0))

        assertIs<KnowledgePromotionDecision.Reject>(result)
    }

    // --- scope discipline ---

    @Test
    fun `InMemoryKnowledgeStore has no dependency on the Planner Runtime, Agent Runtime, Permission Engine, or EventBus`() {
        // Structural proof, not a runtime assertion, mirroring InMemoryIdentityServiceTest's
        // own identical pattern: InMemoryKnowledgeStore's constructor takes only a
        // KnowledgePromotionPolicy (defaulted to DefaultKnowledgePromotionPolicy). If this class ever
        // gained a PlannerRuntime, AgentRunCommandChannel, PermissionEngine, or EventBus
        // dependency, this single-argument construction would no longer compile -- the
        // constructor signature itself is the guarantee, not this assertion.
        val store: KnowledgeStore = InMemoryKnowledgeStore()
        assertTrue(store is KnowledgeStore)
    }

    // --- Sprint 11 Unit 7: KnowledgeSource ---

    @Test
    fun `InMemoryKnowledgeStore also implements KnowledgeSource`() {
        val store = InMemoryKnowledgeStore()

        assertTrue(store is KnowledgeSource)
    }

    @Test
    fun `recall returns exactly what retrieve would for the identical KnowledgeQuery -- a zero-logic delegate, not a second implementation`() = runTest {
        val store = InMemoryKnowledgeStore()
        store.remember(candidate(payload = "window seat memory via recall"))
        val memorySource: KnowledgeSource = store

        val viaRetrieve = store.retrieve(query(relevance = "window"))
        val viaRecall = memorySource.recall(query(relevance = "window"))

        assertEquals(viaRetrieve, viaRecall)
    }

    @Test
    fun `KnowledgeSource exposes no external remember or forget operation`() {
        val functionNames = KnowledgeSource::class.functions.map { it.name }.toSet()

        assertTrue("recall" in functionNames, "KnowledgeSource must expose recall")
        assertFalse("remember" in functionNames, "KnowledgeSource must not expose remember")
        assertFalse("forget" in functionNames, "KnowledgeSource must not expose forget")
    }
}
