package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import parker.core.interfaces.*

/** Task 19: hostile-provider acceptance tests for the complete strict path. */
class StrictEvidenceAdversarialAcceptanceTest {
    private val principal = PrincipalId("task19-principal")
    private val artifact = EvidenceArtifactId("task19-artifact")
    private val evidenceRequest = EvidenceAnalysisRequest("task19-evidence", principal, listOf(artifact))

    @Test
    fun `fabricated and foreign evidence references fail before grounded success`() = runTest {
        val fabricated = invokeEvidence { request -> "GROUNDED_REPLY_R0\nP\tSUPPORTED_FACT\tY2xhaW0=\t999\t-\t-\t-" }
        assertFailed(fabricated, StrictEvidenceReasoningFailure.MALFORMED_GROUNDED_REPLY)

        val foreign = invokeEvidenceWithProvider(object : ReasoningProvider {
            override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse =
                ReasoningProviderResponse.Reply("GROUNDED_REPLY_R0\nP\tSUPPORTED_FACT\tY2xhaW0=\t1\t-\t-\t-")
        }, foreignEvidence = true)
        assertFailed(foreign, StrictEvidenceReasoningFailure.INPUT_RESOLUTION_INCONSISTENT)
    }

    @Test
    fun `fabricated memory reference and unsupported plain reference fail closed`() = runTest {
        val result = invokeMemory { request -> "GROUNDED_REPLY_R0\nP\tINFERENCE\tYmFzaXM=\t-\t999\t-\t-" }
        assertFailed(result, StrictEvidenceReasoningFailure.MALFORMED_GROUNDED_REPLY)
    }

    @Test
    fun `valid citation without deterministic support requires review`() = runTest {
        val result = invokeEvidence { request ->
            encoded(request, GroundedProposition("arbitrary factual claim", GroundedPropositionClassification.SUPPORTED_FACT, supportReferences = listOf(evidence(request))))
        }
        val review = assertIs<StrictEvidenceReasoningResult.HumanReviewRequired>(result)
        assertEquals(ExactStructuredClaimSupportFailure.MISSING_EXPLICIT_STRUCTURED_SUPPORT, review.groundingReport.semanticValidation.failure)
        assertEquals(GroundedPropositionClassification.SUPPORTED_FACT, review.groundedReply.propositions.single().classification)
    }

    @Test
    fun `inference promoted to fact is not accepted while valid inference remains inference`() = runTest {
        val promoted = invokeEvidence { request ->
            encoded(request, GroundedProposition("the source implies a later event", GroundedPropositionClassification.SUPPORTED_FACT, supportReferences = listOf(evidence(request))))
        }
        assertIs<StrictEvidenceReasoningResult.HumanReviewRequired>(promoted)

        val valid = invokeEvidence { request ->
            encoded(request, GroundedProposition("the source may imply a later event", GroundedPropositionClassification.INFERENCE, basisReferences = listOf(evidence(request))))
        }
        assertEquals(GroundedPropositionClassification.INFERENCE, assertIs<StrictEvidenceReasoningResult.Success>(valid).groundedReply.propositions.single().classification)
    }

    @Test
    fun `general knowledge and absence cannot be promoted to supported fact`() = runTest {
        listOf("a generally known fact", "therefore X did not happen").forEach { claim ->
            val result = invokeEvidence { request ->
                encoded(request, GroundedProposition(claim, GroundedPropositionClassification.SUPPORTED_FACT, supportReferences = listOf(evidence(request))))
            }
            assertIs<StrictEvidenceReasoningResult.HumanReviewRequired>(result)
        }
    }

    @Test
    fun `known contradiction is supplied and one-sided fact is rejected`() = runTest {
        val result = invokeConflict { request ->
            val side = request.reasoningContext.suppliedGovernedEntries
                .filterIsInstance<ReasoningContextEntry.GovernedMemoryCore>().single()
            encoded(request, GroundedProposition("one-sided fact", GroundedPropositionClassification.SUPPORTED_FACT, supportReferences = listOf(side)))
        }
        val failed = assertFailed(result, StrictEvidenceReasoningFailure.KNOWN_CONFLICT_ENFORCEMENT_FAILURE)
        assertEquals(1, failed.groundingReport!!.knownConflicts.size)
    }

    @Test
    fun `known contradiction disclosed as human review is valid`() = runTest {
        val result = invokeConflict { request ->
            val conflict = request.reasoningContext.suppliedGovernedEntries.filterIsInstance<ReasoningContextEntry.GovernedConflict>().single()
            encoded(request, GroundedProposition("the sources conflict", GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED, supportReferences = listOf(conflict), reviewReason = GroundedReviewReason.CONFLICT))
        }
        val review = assertIs<StrictEvidenceReasoningResult.HumanReviewRequired>(result)
        assertTrue(review.groundingReport.knownConflicts.single().disclosedByReply)
    }

    @Test
    fun `exact support does not override known contradiction`() = runTest {
        val result = invokeConflict { request ->
            val side = request.reasoningContext.suppliedGovernedEntries.filterIsInstance<ReasoningContextEntry.GovernedMemoryCore>().single()
            encoded(request, GroundedProposition("exact side", GroundedPropositionClassification.SUPPORTED_FACT, supportReferences = listOf(side)))
        }
        assertFailed(result, StrictEvidenceReasoningFailure.KNOWN_CONFLICT_ENFORCEMENT_FAILURE)
    }

    @Test
    fun `superseded contradiction is not enforced as unresolved`() = runTest {
        val result = invokeConflict(status = MemoryCoreRecordStatus.SUPERSEDED) { request ->
            val side = request.reasoningContext.suppliedGovernedEntries.filterIsInstance<ReasoningContextEntry.GovernedMemoryCore>().single()
            encoded(request, GroundedProposition("resolved side", GroundedPropositionClassification.SUPPORTED_FACT, supportReferences = listOf(side)))
        }
        assertIs<StrictEvidenceReasoningResult.HumanReviewRequired>(result)
        // The relationship is resolved, but the claim still lacks an exact
        // structured support assertion; no conflict failure is reported.
        assertEquals(ExactStructuredClaimSupportFailure.MISSING_EXPLICIT_STRUCTURED_SUPPORT, (result as StrictEvidenceReasoningResult.HumanReviewRequired).groundingReport.semanticValidation.failure)
        assertEquals(KnownConflictResolutionState.AUTHORITATIVE_RESOLVED, result.groundingReport.knownConflicts.single().resolutionState)
    }

    @Test
    fun `tampered exact source value and structured value fail closed`() = runTest {
        val wrongSource = invokeEvidence { request ->
            val entry = evidence(request)
            val support = StructuredClaimSupport.ExactSourceValue(entry, "tampered")
            encoded(request, GroundedProposition(support.claimText, GroundedPropositionClassification.SUPPORTED_FACT, supportReferences = listOf(entry), structuredSupports = listOf(support)))
        }
        assertFailed(wrongSource, StrictEvidenceReasoningFailure.INVALID_GROUNDING)

        val wrongHash = invokeEvidence { request ->
            val entry = evidence(request)
            val support = StructuredClaimSupport.ExactStructuredValue(entry, ExactStructuredClaimField.SOURCE_SHA256, "0".repeat(64))
            encoded(request, GroundedProposition(support.claimText, GroundedPropositionClassification.SUPPORTED_FACT, supportReferences = listOf(entry), structuredSupports = listOf(support)))
        }
        assertFailed(wrongHash, StrictEvidenceReasoningFailure.INVALID_GROUNDING)
    }

    @Test
    fun `malformed wire prose and provider failures never use conversational fallback`() = runTest {
        val malformed = invokeEvidence { "not grounded wire" }
        assertFailed(malformed, StrictEvidenceReasoningFailure.MALFORMED_GROUNDED_REPLY)

        val providerFailure = invokeEvidenceWithProvider(object : ReasoningProvider {
            override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse = error("hostile provider failure")
        })
        assertFailed(providerFailure, StrictEvidenceReasoningFailure.PROVIDER_FAILURE)
    }

    @Test
    fun `malformed classifications indexes truncation and structured encoding fail closed`() = runTest {
        val malformed = listOf(
            "GROUNDED_REPLY_R0\nP\tUNKNOWN\tY2xhaW0=\t-\t-\t-\t-",
            "GROUNDED_REPLY_R0\nP\tSUPPORTED_FACT\tY2xhaW0=\t0\t-\t-\t-",
            "GROUNDED_REPLY_R0\nP\tSUPPORTED_FACT\tY2xhaW0=\t1\t-\t-",
            "GROUNDED_REPLY_R0\nP\tSUPPORTED_FACT\tY2xhaW0=\t1\t-\t-\tBOGUS,1,-,dGFtcGVyZWQ=",
        )
        malformed.forEach { raw ->
            assertFailed(invokeEvidence { raw }, StrictEvidenceReasoningFailure.MALFORMED_GROUNDED_REPLY)
        }
    }

    @Test
    fun `human review is an execution outcome and report remains accurate`() = runTest {
        val result = invokeEvidence { request ->
            encoded(request, GroundedProposition("needs a person", GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED, reviewReason = GroundedReviewReason.UNRESOLVED_AMBIGUITY))
        }
        val review = assertIs<StrictEvidenceReasoningResult.HumanReviewRequired>(result)
        assertEquals(evidenceRequest.evidenceArtifactIds, review.groundingReport.requestedEvidence)
        assertEquals(listOf(artifact), review.groundingReport.resolvedEvidence)
        assertEquals(GroundedReviewReason.UNRESOLVED_AMBIGUITY, review.groundingReport.propositions.single().reviewReason)
    }

    @Test
    fun `zero unauthorized writes occur during strict adversarial execution`() = runTest {
        var evidenceWrites = 0
        var acceptanceWrites = 0
        val custodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult { acceptanceWrites++; error("write") }
            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult = EvidenceRetrievalResult.Found(evidenceArtifactId, SOURCE.toByteArray())
            override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult = error("not used")
            override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?): EvidenceSourceSubmissionResult { evidenceWrites++; error("write") }
        }
        val result = StrictEvidenceReasoningInvocation(
            EvidenceIntelligenceInputResolver(custodian, emptyMemoryRetrieval()),
            object : ReasoningProvider {
                override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse = ReasoningProviderResponse.Reply("free prose")
            },
        ).invoke(evidenceRequest)
        assertIs<StrictEvidenceReasoningResult.Failed>(result)
        assertEquals(0, evidenceWrites)
        assertEquals(0, acceptanceWrites)
    }

    private suspend fun invokeEvidence(response: (ReasoningProviderRequest) -> String): StrictEvidenceReasoningResult =
        invokeEvidenceWithProvider(object : ReasoningProvider {
            override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse = ReasoningProviderResponse.Reply(response(request))
        })

    private suspend fun invokeEvidenceWithProvider(provider: ReasoningProvider, foreignEvidence: Boolean = false): StrictEvidenceReasoningResult =
        StrictEvidenceReasoningInvocation(EvidenceIntelligenceInputResolver(evidenceCustodian(foreignEvidence), emptyMemoryRetrieval()), provider).invoke(evidenceRequest)

    private suspend fun invokeMemory(response: (ReasoningProviderRequest) -> String): StrictEvidenceReasoningResult =
        invokeConflict(response = response)

    private suspend fun invokeConflict(
        status: MemoryCoreRecordStatus = MemoryCoreRecordStatus.ACTIVE,
        response: (ReasoningProviderRequest) -> String,
    ): StrictEvidenceReasoningResult {
        val left = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "task19-left")
        val right = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "task19-right")
        val request = EvidenceAnalysisRequest("task19-memory", principal, memoryCoreReferences = listOf(left))
        val relationship = Relationship(RelationshipId("task19-conflict"), Relationship.CONTRADICTS, left, right, false, ProvenanceId("task19-provenance"), Instant.parse("2024-01-01T00:00:00Z"), status)
        val result = StrictEvidenceReasoningInvocation(
            EvidenceIntelligenceInputResolver(evidenceCustodian(), memoryRetrieval(relationship, left)),
            object : ReasoningProvider {
                override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse = ReasoningProviderResponse.Reply(response(request))
            },
        ).invoke(request)
        return result
    }

    private fun encoded(request: ReasoningProviderRequest, proposition: GroundedProposition): String =
        GroundedReplyParser().encode(GroundedReply.fromContext(request.reasoningContext, listOf(proposition)), request.reasoningContext)

    private fun evidence(request: ReasoningProviderRequest) = request.reasoningContext.suppliedGovernedEntries.filterIsInstance<ReasoningContextEntry.GovernedEvidence>().single()

    private fun assertFailed(result: StrictEvidenceReasoningResult, failure: StrictEvidenceReasoningFailure): StrictEvidenceReasoningResult.Failed {
        val failed = assertIs<StrictEvidenceReasoningResult.Failed>(result)
        assertEquals(failure, failed.failure)
        return failed
    }

    private fun evidenceCustodian(foreign: Boolean = false) = object : EvidenceCustodian {
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult = error("not used")
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult = EvidenceRetrievalResult.Found(if (foreign) EvidenceArtifactId("task19-foreign") else evidenceArtifactId, SOURCE.toByteArray())
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult = error("not used")
        override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?): EvidenceSourceSubmissionResult = error("not used")
    }

    private fun emptyMemoryRetrieval() = memoryRetrieval(null, null)

    private fun memoryRetrieval(conflict: Relationship?, resolved: RelationshipEndpoint?) = object : MemoryRetrieval {
        override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? = null
        override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document? = null
        override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion? =
            if (resolved?.recordId == assertionId.value) Assertion(assertionId, "assertion ${assertionId.value}", ProvenanceId("prov-${assertionId.value}")) else null
        override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? = null
        override suspend fun findEntities(query: EntityLookupQuery): List<Entity> = emptyList()
        override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> = emptyList()
        override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> = if (conflict != null && query.startingEndpoint == resolved) listOf(conflict) else emptyList()
        override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> = emptyList()
        override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> = emptyList()
        override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> = emptyList()
    }

    private companion object { const val SOURCE = "task19 source" }
}
