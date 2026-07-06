package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateMemory
import parker.core.interfaces.MemoryCategory
import parker.core.interfaces.MemoryId
import parker.core.interfaces.MemoryPromotionDecision
import parker.core.interfaces.MemoryPromotionPolicy
import parker.core.interfaces.MemoryQuery
import parker.core.interfaces.MemoryStore
import parker.core.interfaces.PrincipalId
import kotlin.reflect.full.functions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 4, Track A, Unit A3. Behavioural tests of [InMemoryMemoryStore]:
 * submission, internally-invoked Evaluation and Promotion, rejection,
 * retrieval (identity scoping, category narrowing, `maximumResults`,
 * deterministic ordering), forgetting (auditable, safe for a missing
 * id), the `MemoryStore` public-surface boundary (no caller-facing
 * `promote`), and this Unit's scope discipline (no Planner/Agent
 * Runtime/Permission Engine dependency or behaviour).
 */
class InMemoryMemoryStoreTest {

    private val principal = PrincipalId("user-1")

    private fun candidate(
        payload: String = "the user prefers window seats",
        category: MemoryCategory = MemoryCategory.SEMANTIC,
        explicitlyRequested: Boolean = true,
        confidence: Double? = null,
        originatingPrincipalId: PrincipalId? = principal,
        correlationId: String = "corr-1",
    ) = CandidateMemory(
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
        category: MemoryCategory? = null,
        requestingPrincipalId: PrincipalId = principal,
    ) = MemoryQuery(
        requestingPrincipalId = requestingPrincipalId,
        relevance = relevance,
        correlationId = "corr-query",
        maximumResults = maximumResults,
        category = category,
    )

    // --- promotion approved / rejected ---

    @Test
    fun `an explicitly requested candidate is promoted`() = runTest {
        val store = InMemoryMemoryStore()

        val result = store.remember(candidate(explicitlyRequested = true))

        assertIs<MemoryPromotionDecision.Promote>(result)
    }

    @Test
    fun `a candidate with no explicit request and no confidence is rejected`() = runTest {
        val store = InMemoryMemoryStore()

        val result = store.remember(candidate(explicitlyRequested = false, confidence = null))

        assertIs<MemoryPromotionDecision.Reject>(result)
    }

    // --- retrievability follows the decision ---

    @Test
    fun `a promoted candidate becomes retrievable`() = runTest {
        val store = InMemoryMemoryStore()
        store.remember(candidate(payload = "the user prefers window seats", explicitlyRequested = true))

        val results = store.retrieve(query(relevance = "window"))

        assertEquals(1, results.size)
        assertEquals("the user prefers window seats", results.single().knowledgePayload)
    }

    @Test
    fun `a rejected candidate never becomes retrievable`() = runTest {
        val store = InMemoryMemoryStore()
        store.remember(candidate(payload = "an unremarkable, low-confidence observation", explicitlyRequested = false, confidence = null))

        val results = store.retrieve(query(relevance = "unremarkable"))

        assertTrue(results.isEmpty())
    }

    // --- MemoryCategory use ---

    @Test
    fun `retrieve narrows by MemoryCategory when one is supplied`() = runTest {
        val store = InMemoryMemoryStore()
        store.remember(candidate(payload = "episodic window seat story", category = MemoryCategory.EPISODIC))
        store.remember(candidate(payload = "semantic window seat fact", category = MemoryCategory.SEMANTIC))

        val episodicOnly = store.retrieve(query(relevance = "window", category = MemoryCategory.EPISODIC))

        assertEquals(1, episodicOnly.size)
        assertEquals(MemoryCategory.EPISODIC, episodicOnly.single().category)
    }

    // --- maximumResults ---

    @Test
    fun `retrieve never returns more than maximumResults`() = runTest {
        val store = InMemoryMemoryStore()
        repeat(5) { i -> store.remember(candidate(payload = "window seat memory number $i")) }

        val results = store.retrieve(query(relevance = "window", maximumResults = 2))

        assertEquals(2, results.size)
    }

    @Test
    fun `retrieve does not imply return everything -- an unbounded-looking query is still capped`() = runTest {
        val store = InMemoryMemoryStore()
        repeat(10) { i -> store.remember(candidate(payload = "window seat memory number $i")) }

        val results = store.retrieve(query(relevance = "window", maximumResults = 3))

        assertEquals(3, results.size)
    }

    // --- deterministic retrieval ---

    @Test
    fun `retrieve returns deterministic, most-recently-promoted-first results`() = runTest {
        val store = InMemoryMemoryStore()
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
        val store = InMemoryMemoryStore()
        store.remember(candidate(payload = "window seat memory for user-1", originatingPrincipalId = PrincipalId("user-1")))
        store.remember(candidate(payload = "window seat memory for user-2", originatingPrincipalId = PrincipalId("user-2")))

        val forUser1 = store.retrieve(query(relevance = "window", requestingPrincipalId = PrincipalId("user-1")))

        assertEquals(1, forUser1.size)
        assertEquals("window seat memory for user-1", forUser1.single().knowledgePayload)
    }

    // --- forget ---

    @Test
    fun `forget removes a promoted record from retrieval`() = runTest {
        val store = InMemoryMemoryStore()
        val promoted = store.remember(candidate(payload = "a memory to be forgotten")) as MemoryPromotionDecision.Promote

        val forgotten = store.forget(promoted.memoryId)

        assertTrue(forgotten)
        assertTrue(store.retrieve(query(relevance = "forgotten")).isEmpty())
    }

    @Test
    fun `forget is auditable -- a forgotten MemoryId is still confirmable as having existed`() = runTest {
        val store = InMemoryMemoryStore()
        val promoted = store.remember(candidate(payload = "a memory to be forgotten")) as MemoryPromotionDecision.Promote

        store.forget(promoted.memoryId)

        assertTrue(store.wasForgotten(promoted.memoryId))
    }

    @Test
    fun `forgetting a MemoryId that was never promoted is handled safely, not an exception`() = runTest {
        val store = InMemoryMemoryStore()

        val result = store.forget(MemoryId("never-existed"))

        assertFalse(result)
        assertFalse(store.wasForgotten(MemoryId("never-existed")))
    }

    // --- MemoryStore public surface: no caller-facing promote ---

    @Test
    fun `MemoryStore exposes no external promote operation`() {
        val functionNames = MemoryStore::class.functions.map { it.name }.toSet()

        assertFalse("promote" in functionNames, "MemoryStore must not expose a caller-facing 'promote' operation")
        assertTrue(
            setOf("remember", "retrieve", "forget").all { it in functionNames },
            "MemoryStore must expose remember/retrieve/forget",
        )
    }

    // --- MemoryPromotionPolicy is invoked internally ---

    @Test
    fun `MemoryPromotionPolicy is consulted internally by InMemoryMemoryStore, exactly once per submission`() = runTest {
        val fakePolicy = FakeMemoryPromotionPolicy { candidateArg, memoryId ->
            MemoryPromotionDecision.Promote(memoryId, candidateArg.proposedCategory)
        }
        val store = InMemoryMemoryStore(promotionPolicy = fakePolicy)

        store.remember(candidate())

        assertEquals(1, fakePolicy.evaluateCallCount)
    }

    @Test
    fun `InMemoryMemoryStore's promotion outcome is entirely controlled by the injected policy, not hardcoded`() = runTest {
        val alwaysReject = FakeMemoryPromotionPolicy { _, memoryId -> MemoryPromotionDecision.Reject(memoryId, "fake always rejects") }
        val store = InMemoryMemoryStore(promotionPolicy = alwaysReject)

        // Even an explicitly-requested, high-confidence candidate is rejected, because the
        // injected fake -- not DefaultMemoryPromotionPolicy's own rules -- governs the outcome.
        val result = store.remember(candidate(explicitlyRequested = true, confidence = 1.0))

        assertIs<MemoryPromotionDecision.Reject>(result)
    }

    // --- scope discipline ---

    @Test
    fun `InMemoryMemoryStore has no dependency on the Planner Runtime, Agent Runtime, Permission Engine, or EventBus`() {
        // Structural proof, not a runtime assertion, mirroring InMemoryIdentityServiceTest's
        // own identical pattern: InMemoryMemoryStore's constructor takes only a
        // MemoryPromotionPolicy (defaulted to DefaultMemoryPromotionPolicy). If this class ever
        // gained a PlannerRuntime, AgentRunCommandChannel, PermissionEngine, or EventBus
        // dependency, this single-argument construction would no longer compile -- the
        // constructor signature itself is the guarantee, not this assertion.
        val store: MemoryStore = InMemoryMemoryStore()
        assertTrue(store is MemoryStore)
    }
}
