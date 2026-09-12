package parker.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.GroundedProposition
import parker.core.interfaces.GroundedPropositionClassification
import parker.core.interfaces.GroundedReply
import parker.core.interfaces.GroundedReviewReason
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry
import parker.core.interfaces.ExactGovernedIdentity
import parker.core.interfaces.StructuredClaimSupport

class GroundingReportTest {

    private val bytes = "report source".toByteArray()
    private val evidenceId = EvidenceArtifactId("artifact-report")
    private val evidence = ReasoningContextEntry.GovernedEvidence(
        text = "report source",
        evidenceArtifactId = evidenceId,
        sourceSha256 = sha256(bytes),
    )
    private val context = ReasoningContext.fromTypedEntries(
        listOf(ReasoningContextEntry.PlainText("ordinary context"), evidence),
    )
    private val request = EvidenceAnalysisRequest(
        analysisKind = "grounded-report-test",
        requestingPrincipalId = PrincipalId("principal-report"),
        evidenceArtifactIds = listOf(evidenceId, EvidenceArtifactId("artifact-unresolved")),
    )

    @Test
    fun `report distinguishes requested resolved and supplied context`() {
        val report = project(GroundedProposition(
            "the source is available",
            GroundedPropositionClassification.SUPPORTED_FACT,
            supportReferences = listOf(evidence),
        ))

        assertEquals(request.evidenceArtifactIds, report.requestedEvidence)
        assertEquals(listOf(evidenceId), report.resolvedEvidence)
        assertEquals(listOf(evidence), report.suppliedContext.map { it.entry })
        assertEquals(listOf("ordinary context"), report.plainTextContext.map { it.entry.text })
        assertEquals(request, report.request)
    }

    @Test
    fun `supported fact maps to exact supplied evidence and exposes provenance`() {
        val report = project(GroundedProposition(
            "the source is available",
            GroundedPropositionClassification.SUPPORTED_FACT,
            supportReferences = listOf(evidence),
        ))
        val proposition = report.propositions.single()
        val mapping = proposition.supportMappings.single()

        assertEquals(GroundedPropositionClassification.SUPPORTED_FACT, proposition.classification)
        assertEquals(evidence, mapping.reference)
        assertEquals(evidence, mapping.suppliedContextEntry?.entry)
        assertEquals(GroundingReportProvenanceStatus.AVAILABLE, mapping.suppliedContextEntry?.provenance?.status)
        assertEquals(evidenceId, mapping.suppliedContextEntry?.provenance?.evidenceArtifactId)
        assertEquals(sha256(bytes), mapping.suppliedContextEntry?.provenance?.sourceSha256)
    }

    @Test
    fun `inference and not established remain distinct in report`() {
        val inference = GroundedProposition(
            "the source may imply a later event",
            GroundedPropositionClassification.INFERENCE,
            basisReferences = listOf(evidence),
        )
        val notEstablished = GroundedProposition(
            "payment is not established",
            GroundedPropositionClassification.NOT_ESTABLISHED,
        )
        val report = project(inference, notEstablished)

        assertEquals(GroundedPropositionClassification.INFERENCE, report.propositions[0].classification)
        assertEquals(evidence, report.propositions[0].inferenceMappings.single().reference)
        assertTrue(report.propositions[0].supportMappings.isEmpty())
        assertEquals(GroundedPropositionClassification.NOT_ESTABLISHED, report.propositions[1].classification)
        assertTrue(report.propositions[1].supportMappings.isEmpty())
        assertTrue(report.propositions[1].inferenceMappings.isEmpty())
    }

    @Test
    fun `human conflict is reported distinctly`() {
        val report = project(GroundedProposition(
            "the dates conflict",
            GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED,
            supportReferences = listOf(evidence),
            reviewReason = GroundedReviewReason.CONFLICT,
        ))

        assertEquals(GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED, report.propositions.single().classification)
        assertEquals(GroundedReviewReason.CONFLICT, report.propositions.single().reviewReason)
    }

    @Test
    fun `invalid validation remains visible and inconsistent provenance is explicit`() {
        val mismatched = evidence.copy(sourceSha256 = "0".repeat(64))
        val invalidContext = ReasoningContext.fromTypedEntries(listOf(mismatched))
        val invalidReply = GroundedReply.fromContext(
            invalidContext,
            listOf(GroundedProposition(
                "mismatched source",
                GroundedPropositionClassification.SUPPORTED_FACT,
                supportReferences = listOf(mismatched),
            )),
        )
        val report = GroundingReportProjection.project(
            request,
            invalidReply,
            invalidContext,
            listOf(EvidenceRetrievalResult.Found(evidenceId, bytes)),
            emptyList(),
        )

        assertFalse(report.validation.valid)
        assertEquals(GroundedReplyValidationFailure.INCONSISTENT_EVIDENCE_PROVENANCE, report.validation.failure)
        assertEquals(GroundingReportProvenanceStatus.INCONSISTENT, report.suppliedContext.single().provenance.status)
    }

    @Test
    fun `report cannot show provider-added context`() {
        val report = project(GroundedProposition(
            "supported",
            GroundedPropositionClassification.SUPPORTED_FACT,
            supportReferences = listOf(evidence),
        ))

        assertEquals(context.suppliedGovernedEntries, report.suppliedContext.map { it.entry })
        assertEquals(evidence, report.propositions.single().supportMappings.single().suppliedContextEntry?.entry)
    }

    @Test
    fun `report exposes exact semantic validation outcome`() {
        val support = StructuredClaimSupport.ExactIdentity(
            evidence,
            ExactGovernedIdentity.EVIDENCE_ARTIFACT_ID,
            evidenceId.value,
        )
        val reply = GroundedReply.fromContext(
            context,
            listOf(GroundedProposition(
                support.claimText,
                GroundedPropositionClassification.SUPPORTED_FACT,
                supportReferences = listOf(evidence),
                structuredSupports = listOf(support),
            )),
        )
        val semantic = ExactStructuredClaimSupportValidator.validate(
            reply,
            context,
            listOf(EvidenceRetrievalResult.Found(evidenceId, bytes)),
            emptyList(),
        )
        val report = GroundingReportProjection.project(
            request,
            reply,
            context,
            listOf(EvidenceRetrievalResult.Found(evidenceId, bytes)),
            emptyList(),
            semantic,
        )

        assertEquals(ExactStructuredClaimSupportStatus.EXACTLY_SUPPORTED, report.semanticValidation.status)
        assertEquals("EXACT_IDENTITY", report.propositions.single().semanticSupport.supportClass)
        assertEquals(evidenceId.value, report.propositions.single().semanticSupport.checkedValue)
    }

    private fun project(vararg propositions: GroundedProposition): GroundingReport =
        GroundingReportProjection.project(
            request,
            GroundedReply.fromContext(context, propositions.toList()),
            context,
            listOf(EvidenceRetrievalResult.Found(evidenceId, bytes)),
            emptyList(),
        )

    private companion object {
        fun sha256(value: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256")
            .digest(value).joinToString("") { "%02x".format(it) }
    }
}
