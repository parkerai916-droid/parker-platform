package parker.core.runtime

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.EvidenceSourceSubmissionResult
import parker.core.interfaces.EntityId
import parker.core.interfaces.ExactGovernedIdentity
import parker.core.interfaces.GroundedProposition
import parker.core.interfaces.GroundedPropositionClassification
import parker.core.interfaces.GroundedReply
import parker.core.interfaces.GroundedReviewReason
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContextEntry
import parker.core.interfaces.ReasoningProvider
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.StructuredClaimSupport

class StrictEvidenceReasoningInvocationTest {

    private val artifactId = EvidenceArtifactId("strict-artifact")
    private val principal = PrincipalId("strict-principal")
    private val request = EvidenceAnalysisRequest("strict-analysis", principal, listOf(artifactId))

    @Test
    fun `complete path supplies only resolved typed context and strict instructions`() = runTest {
        var captured: ReasoningProviderRequest? = null
        val provider = provider { providerRequest ->
            captured = providerRequest
            encodedReply(providerRequest) { entry ->
                GroundedProposition(
                    "EXACT_IDENTITY:EVIDENCE_ARTIFACT_ID:${artifactId.value}",
                    GroundedPropositionClassification.SUPPORTED_FACT,
                    listOf(entry),
                    structuredSupports = listOf(StructuredClaimSupport.ExactIdentity(entry, ExactGovernedIdentity.EVIDENCE_ARTIFACT_ID, artifactId.value)),
                )
            }
        }

        val result = invocation(provider).invoke(request)
        val success = assertIs<StrictEvidenceReasoningResult.Success>(result, result.toString())
        val providerContext = captured!!.reasoningContext

        assertEquals(1, providerContext.suppliedGovernedEntries.size)
        assertEquals(artifactId, (providerContext.suppliedGovernedEntries.single() as ReasoningContextEntry.GovernedEvidence).evidenceArtifactId)
        assertTrue(providerContext.entries.first().contains("Use only information contained in the supplied Parker governed context"))
        assertTrue(providerContext.entries.first().contains("Do not use general model knowledge"))
        assertEquals(GroundedPropositionClassification.SUPPORTED_FACT, success.groundedReply.propositions.single().classification)
        assertTrue(success.groundingReport.validation.valid)
    }

    @Test
    fun `inference remains inference through parsing validation and report`() = runTest {
        val result = invocation(provider { requestForProvider ->
            encodedReply(requestForProvider) { entry ->
                GroundedProposition("the source may imply more", GroundedPropositionClassification.INFERENCE, basisReferences = listOf(entry))
            }
        }).invoke(request)

        val success = assertIs<StrictEvidenceReasoningResult.Success>(result, result.toString())
        assertEquals(GroundedPropositionClassification.INFERENCE, success.groundedReply.propositions.single().classification)
        assertEquals(1, success.groundingReport.propositions.single().inferenceMappings.size)
    }

    @Test
    fun `not established and human conflict remain explicit execution outcomes`() = runTest {
        val notEstablished = invocation(provider { requestForProvider ->
            encodedReply(requestForProvider) {
                GroundedProposition("payment is not established", GroundedPropositionClassification.NOT_ESTABLISHED)
            }
        }).invoke(request)
        assertIs<StrictEvidenceReasoningResult.Success>(notEstablished, notEstablished.toString())

        val review = invocation(provider { requestForProvider ->
            encodedReply(requestForProvider) { entry ->
                GroundedProposition(
                    "dates conflict",
                    GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED,
                    supportReferences = listOf(entry),
                    reviewReason = GroundedReviewReason.CONFLICT,
                )
            }
        }).invoke(request)
        val reviewResult = assertIs<StrictEvidenceReasoningResult.HumanReviewRequired>(review, review.toString())
        assertEquals(GroundedReviewReason.CONFLICT, reviewResult.groundedReply.propositions.single().reviewReason)
        assertEquals(GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED, reviewResult.groundingReport.propositions.single().classification)
    }

    @Test
    fun `malformed wire and unsupported plain prose fail without conversational fallback`() = runTest {
        val malformed = invocation(provider { "the source is true" }).invoke(request)
        val malformedFailure = assertIs<StrictEvidenceReasoningResult.Failed>(malformed)
        assertEquals(StrictEvidenceReasoningFailure.MALFORMED_GROUNDED_REPLY, malformedFailure.failure)

        val providerFailure = invocation(object : ReasoningProvider {
            override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse = error("provider failed")
        }).invoke(request)
        assertEquals(StrictEvidenceReasoningFailure.PROVIDER_FAILURE, assertIs<StrictEvidenceReasoningResult.Failed>(providerFailure).failure)
    }

    @Test
    fun `out of range fabricated support cannot produce grounded success`() = runTest {
        val result = invocation(provider {
            "GROUNDED_REPLY_R0\nP\tSUPPORTED_FACT\tY2xhaW0=\t999\t-\t-"
        }).invoke(request)

        assertEquals(StrictEvidenceReasoningFailure.MALFORMED_GROUNDED_REPLY, assertIs<StrictEvidenceReasoningResult.Failed>(result).failure)
    }

    private fun provider(response: (ReasoningProviderRequest) -> String) = object : ReasoningProvider {
        override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse =
            ReasoningProviderResponse.Reply(response(request))
    }

    private fun encodedReply(
        request: ReasoningProviderRequest,
        proposition: (ReasoningContextEntry) -> GroundedProposition,
    ): String {
        val entry = request.reasoningContext.suppliedGovernedEntries.single()
        val reply = GroundedReply.fromContext(request.reasoningContext, listOf(proposition(entry)))
        return GroundedReplyParser().encode(reply, request.reasoningContext)
    }

    private fun invocation(provider: ReasoningProvider) = StrictEvidenceReasoningInvocation(
        EvidenceIntelligenceInputResolver(
            evidenceCustodian = object : EvidenceCustodian {
                override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult =
                    error("strict invocation must not accept")

                override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult =
                    EvidenceRetrievalResult.Found(evidenceArtifactId, "strict source".toByteArray())

                override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult =
                    error("strict invocation must not retrieve manifests")

                override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?): EvidenceSourceSubmissionResult =
                    error("strict invocation must not submit source")
            },
            memoryRetrieval = object : MemoryRetrieval {
                override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId) = null
                override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: parker.core.interfaces.DocumentId) = null
                override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: parker.core.interfaces.AssertionId) = null
                override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: parker.core.interfaces.RelationshipId) = null
                override suspend fun findEntities(query: parker.core.interfaces.EntityLookupQuery): List<parker.core.interfaces.Entity> = error("not used")
                override suspend fun findDocuments(query: parker.core.interfaces.DocumentLookupQuery): List<parker.core.interfaces.Document> = error("not used")
                override suspend fun traverseRelationships(query: parker.core.interfaces.RelationshipTraversalQuery): List<parker.core.interfaces.Relationship> = error("not used")
                override suspend fun findByTimeRange(query: parker.core.interfaces.ChronologicalLookupQuery): List<MemoryCoreRecord> = error("not used")
                override suspend fun findByMetadata(query: parker.core.interfaces.MetadataLookupQuery): List<MemoryCoreRecord> = error("not used")
                override suspend fun findByProvenance(query: parker.core.interfaces.ProvenanceLookupQuery): List<MemoryCoreRecord> = error("not used")
            },
        ),
        provider,
    )
}
