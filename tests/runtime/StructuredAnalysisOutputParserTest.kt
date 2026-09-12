package parker.core.runtime

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.*

class StructuredAnalysisOutputParserTest {
    private val evidence = EvidenceArtifactId("evidence-a")
    private val generation = DerivativeGenerationId("generation-a")
    private val hash = "a".repeat(64)

    @Test
    fun `valid document reference is accepted and enriched from governed package`() = runTest {
        val result = parser().parse(envelope(reference()), packageValue())
        assertEquals("The date is established.", result.answer)
        assertEquals(AnalysisReferencePrecision.DOCUMENT, result.findings.single().supportReferences.single().precision)
        assertEquals("fixture.pdf", result.findings.single().supportReferences.single().originalFilename)
        assertEquals(hash, result.findings.single().supportReferences.single().sourceSha256)
    }

    @Test
    fun `page precision is rejected when governed payload has no page association`() = runTest {
        assertFailsWith<InvalidAnalysisReferenceException> {
            parser().parse(envelope(reference("PAGE", page = 1)), packageValue())
        }
    }

    @Test
    fun `invented evidence and mismatched derivative are rejected`() = runTest {
        assertFailsWith<InvalidAnalysisReferenceException> {
            parser().parse(envelope(reference(evidenceId = "evidence-other")), packageValue())
        }
        assertFailsWith<InvalidAnalysisReferenceException> {
            parser().parse(envelope(reference(derivativeId = "generation-other")), packageValue())
        }
    }

    @Test
    fun `evidence gap is explicit and has no fabricated citation`() = runTest {
        val result = parser().parse(
            """{"answer":"Not established.","findings":[],"contraryEvidence":[],"uncertainties":[],"evidenceGaps":[{"text":"The requested event is not established.","relatedEvidenceArtifactIds":["evidence-a"]}],"conclusion":"Not established."}""",
            packageValue(),
        )
        assertEquals(1, result.evidenceGaps.size)
        assertEquals(emptyList(), result.findings)
    }

    @Test
    fun `malformed JSON is an explicit structured output failure`() = runTest {
        assertFailsWith<StructuredAnalysisOutputException> { parser().parse("plain prose", packageValue()) }
    }

    private fun parser() = StructuredAnalysisOutputParser()

    private fun reference(
        precision: String = "DOCUMENT",
        page: Int? = null,
        evidenceId: String = evidence.value,
        derivativeId: String = generation.value,
    ) = """{"evidenceArtifactId":"$evidenceId","derivativeGenerationId":"$derivativeId","precision":"$precision","authority":"MACHINE_DERIVED","correctionId":null,"correctionScope":null,"pageNumber":${page ?: "null"},"regionId":null}"""

    private fun envelope(reference: String) = """{"answer":"The date is established.","findings":[{"text":"The date is established.","supportReferences":[$reference]}],"contraryEvidence":[],"uncertainties":[],"evidenceGaps":[],"conclusion":"The date is established."}"""

    private fun packageValue(): AnalysisRetrievalPackage {
        val record = DerivativeGenerationRecord(
            generation, evidence, listOf(DerivativeParentReference.RootEvidenceArtifact(evidence)), "PDF structure",
            TierADerivativePayloadFixtures.PRODUCER, listOf(DerivativeTransformation.STRUCTURAL_PARSING), Instant.EPOCH,
            DerivativeContentIdentity.NoCanonicalSerialization, DerivativeCompletenessState.ACCOUNTED_FOR,
            DerivativeOperationalOutcome.USABLE,
        )
        return AnalysisRetrievalPackage(
            AnalysisRequestId.new(), "What date?", AnalysisType.ISSUE_ANALYSIS,
            AnalysisEvidenceScope(listOf(evidence), mapOf(evidence.value to generation)),
            listOf(AnalysisRetrievedEvidence(
                evidence,
                AgentGatewayEvidenceManifestProjection(evidence, hash, 10, "application/pdf", "fixture.pdf"),
                AnalysisGovernedContent(generation, record, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf())),
            )),
        )
    }
}
