package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ReasoningSubject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Evidence Intelligence, Implementation Unit 3 ("Reasoning Provider
 * Orchestration"). Behavioural tests for
 * [EvidenceIntelligenceReasoningCoordinator], proving it orchestrates
 * [parker.core.interfaces.ReasoningProvider] exactly as
 * `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` §8
 * Unit 3 requires, independently of any real `ReasoningProvider`
 * implementation.
 */
class EvidenceIntelligenceReasoningCoordinatorTest {

    private fun analysisRequest(
        analysisKind: String = "comparison",
        reasoningContext: ReasoningContext? = null,
    ) = EvidenceAnalysisRequest(
        analysisKind = analysisKind,
        requestingPrincipalId = PrincipalId("user-1"),
        reasoningContext = reasoningContext,
    )

    @Test
    fun `reason constructs a ReasoningProviderRequest whose subject is OfEvidenceAnalysisRequest wrapping the exact supplied request`() = runTest {
        val request = analysisRequest()
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val coordinator = EvidenceIntelligenceReasoningCoordinator(reasoningProvider)

        coordinator.reason(request, ReasoningContext(emptyList()))

        val subject = reasoningProvider.lastRequest?.subject
        val ofEvidenceAnalysisRequest = assertIs<ReasoningSubject.OfEvidenceAnalysisRequest>(subject)
        assertEquals(request, ofEvidenceAnalysisRequest.request)
    }

    @Test
    fun `reason supplies the top-level reasoningContext parameter as ReasoningProviderRequest reasoningContext, never EvidenceAnalysisRequest's own field`() = runTest {
        val nestedContext = ReasoningContext(listOf("nested, never consulted"))
        val request = analysisRequest(reasoningContext = nestedContext)
        val topLevelContext = ReasoningContext(listOf("top-level, the sole context supplied"))
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val coordinator = EvidenceIntelligenceReasoningCoordinator(reasoningProvider)

        coordinator.reason(request, topLevelContext)

        assertEquals(topLevelContext, reasoningProvider.lastRequest?.reasoningContext)
    }

    @Test
    fun `reason calls ReasoningProvider reason exactly once`() = runTest {
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val coordinator = EvidenceIntelligenceReasoningCoordinator(reasoningProvider)

        coordinator.reason(analysisRequest(), ReasoningContext(emptyList()))

        assertEquals(1, reasoningProvider.reasonCallCount)
    }

    // --- pass-through: Goal, Reply, NoAction, unchanged ---

    @Test
    fun `reason returns a Goal response unchanged`() = runTest {
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Goal("investigate discrepancy") }
        val coordinator = EvidenceIntelligenceReasoningCoordinator(reasoningProvider)

        val response = coordinator.reason(analysisRequest(), ReasoningContext(emptyList()))

        val goal = assertIs<ReasoningProviderResponse.Goal>(response)
        assertEquals("investigate discrepancy", goal.text)
    }

    @Test
    fun `reason returns a Reply response unchanged`() = runTest {
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("the documents agree") }
        val coordinator = EvidenceIntelligenceReasoningCoordinator(reasoningProvider)

        val response = coordinator.reason(analysisRequest(), ReasoningContext(emptyList()))

        val reply = assertIs<ReasoningProviderResponse.Reply>(response)
        assertEquals("the documents agree", reply.text)
    }

    @Test
    fun `reason returns a NoAction response unchanged`() = runTest {
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val coordinator = EvidenceIntelligenceReasoningCoordinator(reasoningProvider)

        val response = coordinator.reason(analysisRequest(), ReasoningContext(emptyList()))

        assertIs<ReasoningProviderResponse.NoAction>(response)
    }

    // --- faults propagate uncaught: pure, stateless callee ---

    @Test
    fun `an exception thrown by ReasoningProvider reason propagates unchanged`() = runTest {
        val reasoningProvider = FakeReasoningProvider { throw IllegalStateException("model unreachable") }
        val coordinator = EvidenceIntelligenceReasoningCoordinator(reasoningProvider)

        assertFailsWith<IllegalStateException> {
            coordinator.reason(analysisRequest(), ReasoningContext(emptyList()))
        }
    }

    // --- structural: no prohibited dependency slot exists ---

    @Test
    fun `the coordinator's constructor accepts exactly one dependency -- ReasoningProvider`() {
        val constructor = EvidenceIntelligenceReasoningCoordinator::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("ReasoningProvider"), parameterTypes)
    }

    @Test
    fun `EvidenceIntelligenceReasoningCoordinator does not itself implement ReasoningProvider`() {
        val implementedInterfaces = EvidenceIntelligenceReasoningCoordinator::class.java.interfaces.map { it.simpleName }

        assertEquals(emptyList(), implementedInterfaces)
    }
}
