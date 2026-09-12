package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import parker.core.interfaces.*

class KnownConflictProjectionTest {
    private val principal = PrincipalId("conflict-principal")
    private val left = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "left-assertion")
    private val right = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "right-assertion")
    private val request = EvidenceAnalysisRequest(
        analysisKind = "known-conflict",
        requestingPrincipalId = principal,
        memoryCoreReferences = listOf(left),
    )

    @Test
    fun `existing contradicts relationship is projected and supplied to context`() = runTest {
        val relationship = relationship(Relationship.CONTRADICTS)
        val outcome = KnownConflictProjector.project(
            request,
            listOf(left to MemoryCoreRecord.OfAssertion(assertion(left.recordId))),
            retrieval(relationship),
        )
        val conflict = assertIs<KnownConflictProjectionOutcome.Projected>(outcome).conflicts.single()
        assertEquals(KnownConflictType.CONTRADICTS, conflict.conflictType)
        assertEquals(KnownConflictResolutionState.UNRESOLVED, conflict.resolutionState)
        assertEquals(listOf(left, right), conflict.participants)

        val context = ReasoningContext.fromTypedEntries(listOf(
            ReasoningContextEntry.GovernedMemoryCore("left", left),
        ))
        val conflictEntry = KnownConflictProjector.contextEntries(context, listOf(conflict)).last()
        assertIs<ReasoningContextEntry.GovernedConflict>(conflictEntry)
        assertEquals(conflict.conflictId, conflictEntry.conflictId)
        assertEquals(listOf(left, right), conflictEntry.participants)
    }

    @Test
    fun `supported fact citing one side of unresolved conflict is rejected`() = runTest {
        val relationship = relationship(Relationship.DISPUTES)
        val conflict = assertIs<KnownConflictProjectionOutcome.Projected>(
            KnownConflictProjector.project(
                request,
                listOf(left to MemoryCoreRecord.OfAssertion(assertion(left.recordId))),
                retrieval(relationship),
            ),
        ).conflicts
        val context = ReasoningContext.fromTypedEntries(listOf(
            ReasoningContextEntry.GovernedMemoryCore("left", left),
            ReasoningContextEntry.GovernedConflict("conflict", relationship.relationshipId.value, KnownConflictType.DISPUTES, KnownConflictResolutionState.UNRESOLVED, listOf(left, right), relationship.provenanceId),
        ))
        val reply = GroundedReply.fromContext(context, listOf(
            GroundedProposition("one side", GroundedPropositionClassification.SUPPORTED_FACT, supportReferences = listOf(context.typedEntries[0])),
        ))
        assertEquals(
            KnownConflictEnforcementFailure.UNRESOLVED_MATERIAL_CONFLICT,
            assertIs<KnownConflictEnforcementOutcome.Invalid>(KnownConflictEnforcer.enforce(reply, context, conflict)).reason,
        )
    }

    @Test
    fun `superseded conflict relationship is distinguishable as resolved`() = runTest {
        val conflict = relationship(Relationship.CONTRADICTS).copy(status = MemoryCoreRecordStatus.SUPERSEDED)
        val outcome = KnownConflictProjector.project(
            request,
            listOf(left to MemoryCoreRecord.OfAssertion(assertion(left.recordId))),
            retrieval(conflict),
        )
        assertEquals(
            KnownConflictResolutionState.AUTHORITATIVE_RESOLVED,
            assertIs<KnownConflictProjectionOutcome.Projected>(outcome).conflicts.single().resolutionState,
        )
    }

    @Test
    fun `strict invocation supplies known conflict and preserves review disclosure`() = runTest {
        val relationship = relationship(Relationship.CONTRADICTS)
        val memory = retrieval(relationship)
        val provider = object : ReasoningProvider {
            override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse {
                val conflict = request.reasoningContext.suppliedGovernedEntries
                    .filterIsInstance<ReasoningContextEntry.GovernedConflict>().single()
                val reply = GroundedReply.fromContext(request.reasoningContext, listOf(
                    GroundedProposition(
                        "known conflict",
                        GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED,
                        supportReferences = listOf(conflict),
                        reviewReason = GroundedReviewReason.CONFLICT,
                    ),
                ))
                return ReasoningProviderResponse.Reply(GroundedReplyParser().encode(reply, request.reasoningContext))
            }
        }
        val result = StrictEvidenceReasoningInvocation(
            EvidenceIntelligenceInputResolver(emptyCustodian(), memory),
            provider,
        ).invoke(request)

        val review = assertIs<StrictEvidenceReasoningResult.HumanReviewRequired>(result)
        assertEquals(1, review.groundingReport.knownConflicts.size)
        assertEquals(KnownConflictType.CONTRADICTS, review.groundingReport.knownConflicts.single().conflictType)
        assertEquals(true, review.groundingReport.knownConflicts.single().disclosedByReply)
    }

    private fun relationship(type: String) = Relationship(
        relationshipId = RelationshipId("relationship-conflict"),
        relationshipType = type,
        fromEndpoint = left,
        toEndpoint = right,
        directional = false,
        provenanceId = ProvenanceId("provenance-conflict"),
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
    )

    private fun assertion(id: String) = Assertion(AssertionId(id), "assertion $id", ProvenanceId("provenance-$id"))

    private fun retrieval(relationship: Relationship) = object : MemoryRetrieval {
        override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? = null
        override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document? = null
        override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion? = assertion(assertionId.value)
        override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? = null
        override suspend fun findEntities(query: EntityLookupQuery): List<Entity> = emptyList()
        override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> = emptyList()
        override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> =
            if (query.startingEndpoint == left) listOf(relationship) else emptyList()
        override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> = emptyList()
        override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> = emptyList()
        override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> = emptyList()
    }

    private fun emptyCustodian() = object : EvidenceCustodian {
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult = error("not used")
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult = error("not used")
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult = error("not used")
        override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?): EvidenceSourceSubmissionResult = error("not used")
    }
}
