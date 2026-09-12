package parker.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ExactGovernedIdentity
import parker.core.interfaces.ExactStructuredClaimField
import parker.core.interfaces.GroundedProposition
import parker.core.interfaces.GroundedPropositionClassification
import parker.core.interfaces.GroundedReply
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry
import parker.core.interfaces.StructuredClaimSupport

class ExactStructuredClaimSupportValidatorTest {

    private val bytes = "an exact governed value".toByteArray()
    private val id = EvidenceArtifactId("exact-support-artifact")
    private val entry = ReasoningContextEntry.GovernedEvidence(
        text = "exact governed value",
        evidenceArtifactId = id,
        derivativeGenerationId = parker.core.interfaces.DerivativeGenerationId("generation-exact"),
        sourceSha256 = sha256(bytes),
        pageNumber = 3,
        sourceRegionId = parker.core.interfaces.SourceRegionId("a".repeat(64)),
    )
    private val context = ReasoningContext.fromTypedEntries(listOf(entry))
    private val found = listOf(EvidenceRetrievalResult.Found(id, bytes))

    @Test
    fun `exact identity page region and source hash values pass`() {
        val supports = listOf(
            StructuredClaimSupport.ExactIdentity(entry, ExactGovernedIdentity.EVIDENCE_ARTIFACT_ID, id.value),
            StructuredClaimSupport.ExactIdentity(entry, ExactGovernedIdentity.DERIVATIVE_GENERATION_ID, "generation-exact"),
            StructuredClaimSupport.ExactIdentity(entry, ExactGovernedIdentity.SOURCE_REGION_ID, "a".repeat(64)),
            StructuredClaimSupport.ExactStructuredValue(entry, ExactStructuredClaimField.PAGE_NUMBER, "3"),
            StructuredClaimSupport.ExactStructuredValue(entry, ExactStructuredClaimField.SOURCE_SHA256, sha256(bytes)),
        )
        val results = supports.map { support ->
            val proposition = proposition(support)
            ExactStructuredClaimSupportValidator.validate(
                GroundedReply.fromContext(context, listOf(proposition)), context, found, emptyList(),
            )
        }

        results.forEach { assertIs<ExactStructuredClaimSupportValidationOutcome.ExactlySupported>(it) }
    }

    @Test
    fun `exact source value passes and mismatched value fails`() {
        val exact = StructuredClaimSupport.ExactSourceValue(entry, "an exact governed value")
        val exactResult = ExactStructuredClaimSupportValidator.validate(
            GroundedReply.fromContext(context, listOf(proposition(exact))), context, found, emptyList(),
        )
        assertIs<ExactStructuredClaimSupportValidationOutcome.ExactlySupported>(exactResult)

        val mismatch = StructuredClaimSupport.ExactSourceValue(entry, "different value")
        val mismatchResult = ExactStructuredClaimSupportValidator.validate(
            GroundedReply.fromContext(context, listOf(proposition(mismatch))), context, found, emptyList(),
        )
        assertEquals(
            ExactStructuredClaimSupportFailure.SOURCE_VALUE_MISMATCH,
            assertIs<ExactStructuredClaimSupportValidationOutcome.Invalid>(mismatchResult).reason,
        )
    }

    @Test
    fun `arbitrary supported prose routes to human review without reclassification`() {
        val reply = GroundedReply.fromContext(
            context,
            listOf(GroundedProposition("the source proves a conclusion", GroundedPropositionClassification.SUPPORTED_FACT, listOf(entry))),
        )
        val outcome = ExactStructuredClaimSupportValidator.validate(reply, context, found, emptyList())

        assertEquals(
            ExactStructuredClaimSupportFailure.MISSING_EXPLICIT_STRUCTURED_SUPPORT,
            assertIs<ExactStructuredClaimSupportValidationOutcome.HumanReviewRequired>(outcome).reason,
        )
        assertEquals(GroundedPropositionClassification.SUPPORTED_FACT, reply.propositions.single().classification)
    }

    @Test
    fun `foreign invocation and inconsistent structural provenance fail`() {
        val identity = StructuredClaimSupport.ExactIdentity(entry, ExactGovernedIdentity.EVIDENCE_ARTIFACT_ID, id.value)
        val reply = GroundedReply.fromContext(context, listOf(proposition(identity)))
        assertIs<ExactStructuredClaimSupportValidationOutcome.Invalid>(
            ExactStructuredClaimSupportValidator.validate(
                reply, context, listOf(EvidenceRetrievalResult.Found(EvidenceArtifactId("foreign"), bytes)), emptyList(),
            ),
        )

        val mismatchedEntry = entry.copy(sourceSha256 = "0".repeat(64))
        val mismatchedContext = ReasoningContext.fromTypedEntries(listOf(mismatchedEntry))
        val mismatchedSupport = StructuredClaimSupport.ExactIdentity(mismatchedEntry, ExactGovernedIdentity.EVIDENCE_ARTIFACT_ID, id.value)
        val structural = GroundedReplyValidator.validate(
            GroundedReply.fromContext(mismatchedContext, listOf(proposition(mismatchedSupport))),
            mismatchedContext,
            found,
            emptyList(),
        )
        assertEquals(GroundedReplyValidationFailure.INCONSISTENT_EVIDENCE_PROVENANCE, assertIs<GroundedReplyValidationOutcome.Invalid>(structural).reason)
    }

    private fun proposition(support: StructuredClaimSupport) = GroundedProposition(
        support.claimText,
        GroundedPropositionClassification.SUPPORTED_FACT,
        supportReferences = listOf(support.contextEntry),
        structuredSupports = listOf(support),
    )

    private companion object {
        fun sha256(value: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256")
            .digest(value).joinToString("") { "%02x".format(it) }
    }
}
