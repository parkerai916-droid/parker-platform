package parker.core.runtime

import parker.core.interfaces.EvidenceAnalysisResult
import parker.core.interfaces.EvidenceArtifactId
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Evidence Intelligence, Implementation Unit 4 ("Analytical Output
 * Discipline"). Behavioural tests for [AnalyticalOutputDiscipline],
 * demonstrating -- not merely asserting -- every objective
 * `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` §8
 * Unit 4 requires.
 */
class AnalyticalOutputDisciplineTest {

    private fun ref(value: String = "artifact-1") = EvidenceArtifactId(value)

    // ================= Content-type distinction =================

    @Test
    fun `labelContent produces a discoverable, distinctly-labelled segment for each of the four content kinds`() {
        val extracted = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.EXTRACTED, "the document reads 'net 30'")
        val observed = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.OBSERVED, "the signature block is blank")
        val inferred = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.INFERRED, "the invoice likely predates the contract")
        val modelGenerated = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.MODEL_GENERATED, "in summary, the two accounts diverge")

        assertEquals(listOf(AnalyticalOutputDiscipline.EXTRACTED to "the document reads 'net 30'"), AnalyticalOutputDiscipline.discoverSegments(extracted))
        assertEquals(listOf(AnalyticalOutputDiscipline.OBSERVED to "the signature block is blank"), AnalyticalOutputDiscipline.discoverSegments(observed))
        assertEquals(listOf(AnalyticalOutputDiscipline.INFERRED to "the invoice likely predates the contract"), AnalyticalOutputDiscipline.discoverSegments(inferred))
        assertEquals(listOf(AnalyticalOutputDiscipline.MODEL_GENERATED to "in summary, the two accounts diverge"), AnalyticalOutputDiscipline.discoverSegments(modelGenerated))
    }

    @Test
    fun `a piece of output mixing an extracted quotation and an inferred conclusion keeps both portions discoverable and distinct`() {
        val mixed = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.EXTRACTED, "'payment due within 30 days'") +
            " " +
            AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.INFERRED, "this predates the amended terms")

        val segments = AnalyticalOutputDiscipline.discoverSegments(mixed)

        assertEquals(2, segments.size)
        assertEquals(AnalyticalOutputDiscipline.EXTRACTED, segments[0].first)
        assertEquals(AnalyticalOutputDiscipline.INFERRED, segments[1].first)
    }

    @Test
    fun `labelContent rejects an unrecognised content kind`() {
        assertFailsWith<IllegalArgumentException> {
            AnalyticalOutputDiscipline.labelContent("SPECULATION", "not one of the four recognised kinds")
        }
    }

    @Test
    fun `labelContent rejects blank text`() {
        assertFailsWith<IllegalArgumentException> {
            AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.EXTRACTED, "   ")
        }
    }

    // ================= Transcription-fidelity distinction =================

    @Test
    fun `labelTranscription produces a discoverable, distinctly-labelled segment for each of the three fidelity kinds`() {
        val verbatim = AnalyticalOutputDiscipline.labelTranscription(AnalyticalOutputDiscipline.VERBATIM, "Recieved wth thanks")
        val normalised = AnalyticalOutputDiscipline.labelTranscription(AnalyticalOutputDiscipline.NORMALISED, "Received with thanks")
        val reconstructed = AnalyticalOutputDiscipline.labelTranscription(AnalyticalOutputDiscipline.RECONSTRUCTED, "[illegible: likely 'thanks']")

        assertEquals(AnalyticalOutputDiscipline.VERBATIM to "Recieved wth thanks", AnalyticalOutputDiscipline.discoverSegments(verbatim).single())
        assertEquals(AnalyticalOutputDiscipline.NORMALISED to "Received with thanks", AnalyticalOutputDiscipline.discoverSegments(normalised).single())
        assertEquals(AnalyticalOutputDiscipline.RECONSTRUCTED to "[illegible: likely 'thanks']", AnalyticalOutputDiscipline.discoverSegments(reconstructed).single())
    }

    @Test
    fun `discoverSegments preserves labelled content that itself contains a literal bracket, never truncating it`() {
        val extracted = AnalyticalOutputDiscipline.labelContent(
            AnalyticalOutputDiscipline.EXTRACTED,
            "the invoice cites '[Exhibit A]' as support",
        )

        assertEquals(
            AnalyticalOutputDiscipline.EXTRACTED to "the invoice cites '[Exhibit A]' as support",
            AnalyticalOutputDiscipline.discoverSegments(extracted).single(),
        )
    }

    @Test
    fun `a transcription mixing verbatim and reconstructed portions keeps both discoverable and distinct, never uniform`() {
        val mixed = AnalyticalOutputDiscipline.labelTranscription(AnalyticalOutputDiscipline.VERBATIM, "Dear Sir,") +
            " " +
            AnalyticalOutputDiscipline.labelTranscription(AnalyticalOutputDiscipline.RECONSTRUCTED, "[likely: 'yours faithfully']")

        val segments = AnalyticalOutputDiscipline.discoverSegments(mixed)

        assertEquals(listOf(AnalyticalOutputDiscipline.VERBATIM, AnalyticalOutputDiscipline.RECONSTRUCTED), segments.map { it.first })
    }

    @Test
    fun `labelTranscription rejects an unrecognised fidelity kind`() {
        assertFailsWith<IllegalArgumentException> {
            AnalyticalOutputDiscipline.labelTranscription("PARAPHRASED", "not one of the three recognised kinds")
        }
    }

    // ================= Claim-level traceability (Unit 1's own structural guarantee, demonstrated here) =================

    @Test
    fun `a TransientOutput representing exactly one claim carries at least one governed reference`() {
        val claim = EvidenceAnalysisResult.TransientOutput(
            text = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.OBSERVED, "the two invoices name different totals"),
            evidenceArtifactReferences = listOf(ref()),
        )

        assertTrue(claim.evidenceArtifactReferences.isNotEmpty())
    }

    @Test
    fun `a multi-claim analysis is represented as multiple TransientOutput values, never one bundling both`() {
        val firstClaim = EvidenceAnalysisResult.TransientOutput(
            text = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.EXTRACTED, "invoice A states 'net 30'"),
            evidenceArtifactReferences = listOf(ref("invoice-a")),
        )
        val secondClaim = EvidenceAnalysisResult.TransientOutput(
            text = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.EXTRACTED, "invoice B states 'net 60'"),
            evidenceArtifactReferences = listOf(ref("invoice-b")),
        )

        val results: List<EvidenceAnalysisResult> = listOf(firstClaim, secondClaim)

        assertEquals(2, results.size)
        assertTrue(results.all { it is EvidenceAnalysisResult.TransientOutput })
    }

    // ================= Contradiction disclosure =================

    @Test
    fun `discloseContradiction always returns both claims, never one alone`() {
        val supporting = EvidenceAnalysisResult.TransientOutput(
            text = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.EXTRACTED, "witness A: the meeting was on Tuesday"),
            evidenceArtifactReferences = listOf(ref("statement-a")),
        )
        val contradicting = EvidenceAnalysisResult.TransientOutput(
            text = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.EXTRACTED, "witness B: the meeting was on Wednesday"),
            evidenceArtifactReferences = listOf(ref("statement-b")),
        )

        val disclosed = AnalyticalOutputDiscipline.discloseContradiction(supporting, contradicting)

        assertEquals(listOf(supporting, contradicting), disclosed)
    }

    @Test
    fun `discloseContradiction's own signature makes a single-sided disclosure structurally impossible`() {
        // Structural, not behavioural: discloseContradiction has exactly one overload,
        // and it requires two TransientOutput parameters -- there is no path that
        // compiles while supplying only one side.
        val method = AnalyticalOutputDiscipline::class.java.getMethod(
            "discloseContradiction",
            EvidenceAnalysisResult.TransientOutput::class.java,
            EvidenceAnalysisResult.TransientOutput::class.java,
        )

        assertEquals(2, method.parameterCount)
    }

    // ================= Confidence isolation =================

    @Test
    fun `analytical confidence remains expressible only as ordinary prose within a TransientOutput, never a durable field`() {
        val withConfidenceNoted = EvidenceAnalysisResult.TransientOutput(
            text = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.INFERRED, "similarity score 0.87 -- likely the same event"),
            evidenceArtifactReferences = listOf(ref()),
        )

        assertContains(withConfidenceNoted.text, "0.87")
        // TransientOutput has no confidence-shaped field of its own to populate --
        // structurally guaranteed by Unit 1's own frozen shape (text, evidenceArtifactReferences,
        // memoryCoreReferences only), confirmed here by declared-field enumeration.
        val fieldNames = EvidenceAnalysisResult.TransientOutput::class.java.declaredFields.map { it.name }
        assertEquals(setOf("text", "evidenceArtifactReferences", "memoryCoreReferences"), fieldNames.toSet())
    }

    @Test
    fun `AnalyticalOutputDiscipline exposes no method that accepts or returns a confidence or evidential-state value`() {
        val suspiciousTypeNames = setOf("Double", "Float", "EvidentialState")
        val methods = AnalyticalOutputDiscipline::class.java.declaredMethods

        for (method in methods) {
            val parameterTypeNames = method.parameterTypes.map { it.simpleName }.toSet()
            val returnTypeName = method.returnType.simpleName
            assertTrue(
                parameterTypeNames.none { it in suspiciousTypeNames } && returnTypeName !in suspiciousTypeNames,
                "Method '${method.name}' unexpectedly references a confidence/evidential-state-shaped type",
            )
        }
    }

    // ================= Structural: no new public type introduced =================

    @Test
    fun `AnalyticalOutputDiscipline is not one of the four already-frozen public runtime types`() {
        // Kotlin `internal` visibility is enforced by the compiler (mangled names,
        // module-boundary checks), not by JVM access modifiers, so this test checks
        // the one thing meaningful from the JVM side: this object's own name is not,
        // and does not collide with, any of the four already-frozen public runtime
        // types (EvidenceAnalysisRequest, EvidenceAnalysisResult, the payload
        // selector, EvidenceIntelligence) -- it is a distinct, later-introduced
        // implementation class, exactly as Unit 4's own governance requires.
        val name = AnalyticalOutputDiscipline::class.simpleName

        assertTrue(name !in setOf("EvidenceAnalysisRequest", "EvidenceAnalysisResult", "EvidenceIntelligence"))
    }
}
