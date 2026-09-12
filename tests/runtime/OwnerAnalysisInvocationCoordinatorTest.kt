package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import parker.core.interfaces.*

class OwnerAnalysisInvocationCoordinatorTest {
    private val evidence = EvidenceArtifactId("evidence-a")
    private val generation = DerivativeGenerationId("generation-a")

    @Test
    fun `preferred evidence is retrieved then sent to Hermes with exact scope`() = runTest {
        var invoked = false
        val packageValue = AnalysisRetrievalPackage(
            AnalysisRequestId.new(), "What date?", AnalysisType.ISSUE_ANALYSIS,
            AnalysisEvidenceScope(listOf(evidence), mapOf(evidence.value to generation)), emptyList(),
        )
        val outcome = coordinator(
            { PreferredDerivativeResolution.Preferred(evidence, candidate(), "only candidate") },
            { request -> AnalysisRequestResult.Accepted(packageValue.copy(requestId = request.requestId)) },
            object : HermesAnalysisInvoker {
                override suspend fun invoke(question: String, analysisType: AnalysisType, governedPackage: AnalysisRetrievalPackage): HermesAnalysisInvocation {
                    invoked = true
                    assertEquals(listOf(evidence), governedPackage.scope.evidenceArtifactIds)
                    assertEquals(generation, governedPackage.scope.derivativeGenerationIds[evidence.value])
                    return HermesAnalysisInvocation(
                        """{"answer":"supported","findings":[],"contraryEvidence":[],"uncertainties":[],"evidenceGaps":[],"conclusion":"supported"}""",
                        "session-1",
                    )
                }
            },
        ).invoke(OwnerAnalysisInvocationRequest("What date?", listOf(evidence)))

        assertIs<OwnerAnalysisInvocationOutcome.Completed>(outcome)
        assertEquals("""{"answer":"supported","findings":[],"contraryEvidence":[],"uncertainties":[],"evidenceGaps":[],"conclusion":"supported"}""", outcome.analysisText)
        assertEquals("parker-analysis-agent", outcome.profile)
        assertEquals(true, invoked)
    }

    @Test
    fun `ambiguity prevents governed retrieval and Hermes invocation`() = runTest {
        var called = false
        val result = coordinator(
            { PreferredDerivativeResolution.Ambiguous(evidence, listOf(candidate()), "tie") },
            { called = true; error("must not retrieve") },
            object : HermesAnalysisInvoker {
                override suspend fun invoke(question: String, analysisType: AnalysisType, governedPackage: AnalysisRetrievalPackage): HermesAnalysisInvocation = error("must not reason")
            },
        ).invoke(OwnerAnalysisInvocationRequest("Question", listOf(evidence)))
        assertIs<OwnerAnalysisInvocationOutcome.DerivativeAmbiguous>(result)
        assertEquals(false, called)
    }

    @Test
    fun `bounded prior context is passed as context and current follow-up remains distinct`() = runTest {
        var submittedQuestion = ""
        val packageValue = AnalysisRetrievalPackage(
            AnalysisRequestId.new(), "follow-up", AnalysisType.ISSUE_ANALYSIS,
            AnalysisEvidenceScope(listOf(evidence), mapOf(evidence.value to generation)), emptyList(),
        )
        val result = coordinator(
            { PreferredDerivativeResolution.Preferred(evidence, candidate(), "only candidate") },
            { request -> AnalysisRequestResult.Accepted(packageValue.copy(requestId = request.requestId)) },
            object : HermesAnalysisInvoker {
                override suspend fun invoke(question: String, analysisType: AnalysisType, governedPackage: AnalysisRetrievalPackage): HermesAnalysisInvocation {
                    submittedQuestion = question
                    return HermesAnalysisInvocation("""{"answer":"follow-up","findings":[],"contraryEvidence":[],"uncertainties":[],"evidenceGaps":[],"conclusion":"follow-up"}""", null)
                }
            },
        ).invoke(OwnerAnalysisInvocationRequest("What contradicts that?", listOf(evidence)), "Previous user question: What is established?\nPrevious Parker answer: The agreement is dated 15 July 2026.")
        assertIs<OwnerAnalysisInvocationOutcome.Completed>(result)
        assertTrue(submittedQuestion.contains("context only, not evidence"))
        assertTrue(submittedQuestion.contains("What is established?"))
        assertTrue(submittedQuestion.contains("Current follow-up question: What contradicts that?"))
    }

    private fun candidate() = DerivativeCandidateSummary(
        generation, evidence, "PDF structure",
        DerivativeProducerIdentity("test", "1", "test"), Instant.EPOCH,
        DerivativeOperationalOutcome.USABLE, DerivativeCompletenessState.ACCOUNTED_FOR,
        emptyList(), listOf(DerivativeTransformation.STRUCTURAL_PARSING), true,
    )

    private fun coordinator(
        resolve: suspend (EvidenceArtifactId) -> PreferredDerivativeResolution,
        submit: suspend (AnalysisRequest) -> AnalysisRequestResult,
        invoker: HermesAnalysisInvoker,
    ) = OwnerAnalysisInvocationCoordinator(resolve, submit, invoker)
}
