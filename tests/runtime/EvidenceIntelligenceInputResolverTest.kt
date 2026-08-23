package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.Assertion
import parker.core.interfaces.AssertionId
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.ChronologicalLookupQuery
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentLookupQuery
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.EntityLookupQuery
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.MetadataLookupQuery
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceLookupQuery
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.RelationshipTraversalQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Evidence Intelligence, Implementation Unit 2 ("Governed Input
 * Resolution"). Behavioural tests for [EvidenceIntelligenceInputResolver],
 * proving it resolves exclusively through [EvidenceCustodian.retrieve] and
 * [MemoryRetrieval], preserves per-input identity for every outcome
 * (including the Memory Core side, which [MemoryRetrieval] itself does
 * not self-identify), rejects a request naming nothing to analyse before
 * any boundary is called, and performs no reasoning, orchestration,
 * acceptance, candidate production, runtime composition, or Permission
 * Engine invocation of its own.
 */
class EvidenceIntelligenceInputResolverTest {

    private val principalId = PrincipalId("owner-1")
    private val fixedInstant: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private fun minimalRequest(
        evidenceArtifactIds: List<EvidenceArtifactId> = emptyList(),
        memoryCoreReferences: List<RelationshipEndpoint> = emptyList(),
    ) = EvidenceAnalysisRequest(
        analysisKind = "comparison",
        requestingPrincipalId = principalId,
        evidenceArtifactIds = evidenceArtifactIds,
        memoryCoreReferences = memoryCoreReferences,
    )

    // ================= Rejecting a request with nothing to analyse =================

    @Test
    fun `resolve rejects a request naming no evidence artefact and no Memory Core reference, before calling either boundary`() = runTest {
        val evidenceCustodian = FakeEvidenceCustodian { _, _ -> throw AssertionError("must not be called") }
        val memoryRetrieval = FakeMemoryRetrieval()
        val resolver = EvidenceIntelligenceInputResolver(evidenceCustodian, memoryRetrieval)

        assertFailsWith<IllegalArgumentException> { resolver.resolve(minimalRequest()) }
    }

    // ================= Evidence artefact resolution =================

    @Test
    fun `resolve calls EvidenceCustodian retrieve once per evidenceArtifactId, in order, with the requesting principal`() = runTest {
        val artifactA = EvidenceArtifactId("artifact-a")
        val artifactB = EvidenceArtifactId("artifact-b")
        val calls = mutableListOf<Pair<PrincipalId, EvidenceArtifactId>>()
        val evidenceCustodian = FakeEvidenceCustodian { principal, id ->
            calls += principal to id
            EvidenceRetrievalResult.Found(id, byteArrayOf(1))
        }
        val resolver = EvidenceIntelligenceInputResolver(evidenceCustodian, FakeMemoryRetrieval())

        resolver.resolve(minimalRequest(evidenceArtifactIds = listOf(artifactA, artifactB)))

        assertEquals(listOf(principalId to artifactA, principalId to artifactB), calls)
    }

    @Test
    fun `a multi-input resolution preserves Found, NotFound, and Rejected, each self-identified by its own EvidenceArtifactId`() = runTest {
        val artifactFound = EvidenceArtifactId("artifact-found")
        val artifactNotFound = EvidenceArtifactId("artifact-not-found")
        val artifactRejected = EvidenceArtifactId("artifact-rejected")
        val evidenceCustodian = FakeEvidenceCustodian { _, id ->
            when (id) {
                artifactFound -> EvidenceRetrievalResult.Found(id, byteArrayOf(9, 9))
                artifactNotFound -> EvidenceRetrievalResult.NotFound(id)
                artifactRejected -> EvidenceRetrievalResult.Rejected(id, "Permission Engine did not authorise evidence retrieval")
                else -> throw AssertionError("unexpected id: $id")
            }
        }
        val resolver = EvidenceIntelligenceInputResolver(evidenceCustodian, FakeMemoryRetrieval())

        val (evidenceResults, _) = resolver.resolve(
            minimalRequest(evidenceArtifactIds = listOf(artifactFound, artifactNotFound, artifactRejected)),
        )

        assertEquals(3, evidenceResults.size)
        val found = assertIs<EvidenceRetrievalResult.Found>(evidenceResults[0])
        assertEquals(artifactFound, found.evidenceArtifactId)
        val notFound = assertIs<EvidenceRetrievalResult.NotFound>(evidenceResults[1])
        assertEquals(artifactNotFound, notFound.evidenceArtifactId)
        val rejected = assertIs<EvidenceRetrievalResult.Rejected>(evidenceResults[2])
        assertEquals(artifactRejected, rejected.evidenceArtifactId)
        assertTrue(rejected.reason.isNotBlank())
    }

    @Test
    fun `resolve never calls EvidenceCustodian accept`() = runTest {
        // FakeEvidenceCustodian.accept always throws; reaching this test's own assertion
        // without that exception proves accept was never called.
        val evidenceCustodian = FakeEvidenceCustodian { _, id -> EvidenceRetrievalResult.NotFound(id) }
        val resolver = EvidenceIntelligenceInputResolver(evidenceCustodian, FakeMemoryRetrieval())

        val (evidenceResults, _) = resolver.resolve(minimalRequest(evidenceArtifactIds = listOf(EvidenceArtifactId("artifact-1"))))

        assertEquals(1, evidenceResults.size)
    }

    // ================= Memory Core reference resolution =================

    @Test
    fun `resolve dispatches an entity-kind reference to getEntity and wraps the result as MemoryCoreRecord OfEntity`() = runTest {
        val entity = Entity(
            entityId = EntityId("entity-1"),
            entityType = "person",
            primaryLabel = "Alex",
            provenanceId = ProvenanceId("provenance-1"),
            createdAt = fixedInstant,
        )
        val memoryRetrieval = FakeMemoryRetrieval(onGetEntity = { _, id -> if (id == entity.entityId) entity else null })
        val resolver = EvidenceIntelligenceInputResolver(FakeEvidenceCustodian { _, id -> EvidenceRetrievalResult.NotFound(id) }, memoryRetrieval)
        val endpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1")

        val (_, memoryCoreResults) = resolver.resolve(minimalRequest(memoryCoreReferences = listOf(endpoint)))

        assertEquals(1, memoryCoreResults.size)
        assertEquals(endpoint, memoryCoreResults[0].first)
        val record = assertIs<MemoryCoreRecord.OfEntity>(memoryCoreResults[0].second)
        assertEquals(entity, record.entity)
    }

    @Test
    fun `resolve dispatches a document-kind reference to getDocument and wraps the result as MemoryCoreRecord OfDocument`() = runTest {
        val document = Document(
            documentId = DocumentId("document-1"),
            documentType = "pdf",
            locationReference = "evidence-artifact-1",
            provenanceId = ProvenanceId("provenance-1"),
            registeredAt = fixedInstant,
        )
        val memoryRetrieval = FakeMemoryRetrieval(onGetDocument = { _, id -> if (id == document.documentId) document else null })
        val resolver = EvidenceIntelligenceInputResolver(FakeEvidenceCustodian { _, id -> EvidenceRetrievalResult.NotFound(id) }, memoryRetrieval)
        val endpoint = RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, "document-1")

        val (_, memoryCoreResults) = resolver.resolve(minimalRequest(memoryCoreReferences = listOf(endpoint)))

        val record = assertIs<MemoryCoreRecord.OfDocument>(memoryCoreResults.single().second)
        assertEquals(document, record.document)
    }

    @Test
    fun `resolve dispatches an assertion-kind reference to getAssertion and wraps the result as MemoryCoreRecord OfAssertion`() = runTest {
        val assertion = Assertion(
            assertionId = AssertionId("assertion-1"),
            statement = "the document was signed on 2026-01-01",
            provenanceId = ProvenanceId("provenance-1"),
        )
        val memoryRetrieval = FakeMemoryRetrieval(onGetAssertion = { _, id -> if (id == assertion.assertionId) assertion else null })
        val resolver = EvidenceIntelligenceInputResolver(FakeEvidenceCustodian { _, id -> EvidenceRetrievalResult.NotFound(id) }, memoryRetrieval)
        val endpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1")

        val (_, memoryCoreResults) = resolver.resolve(minimalRequest(memoryCoreReferences = listOf(endpoint)))

        val record = assertIs<MemoryCoreRecord.OfAssertion>(memoryCoreResults.single().second)
        assertEquals(assertion, record.assertion)
    }

    @Test
    fun `resolve dispatches a relationship-kind reference to getRelationship and wraps the result as MemoryCoreRecord OfRelationship`() = runTest {
        val relationship = Relationship(
            relationshipId = RelationshipId("relationship-1"),
            relationshipType = Relationship.CONTRADICTS,
            fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1"),
            toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-2"),
            directional = false,
            provenanceId = ProvenanceId("provenance-1"),
            createdAt = fixedInstant,
        )
        val memoryRetrieval = FakeMemoryRetrieval(onGetRelationship = { _, id -> if (id == relationship.relationshipId) relationship else null })
        val resolver = EvidenceIntelligenceInputResolver(FakeEvidenceCustodian { _, id -> EvidenceRetrievalResult.NotFound(id) }, memoryRetrieval)
        val endpoint = RelationshipEndpoint(RelationshipEndpoint.RELATIONSHIP, "relationship-1")

        val (_, memoryCoreResults) = resolver.resolve(minimalRequest(memoryCoreReferences = listOf(endpoint)))

        val record = assertIs<MemoryCoreRecord.OfRelationship>(memoryCoreResults.single().second)
        assertEquals(relationship, record.relationship)
    }

    @Test
    fun `a Memory Core reference nothing resolves under is paired with its own endpoint and a null record, never inferred from list position`() = runTest {
        val foundEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1")
        val missingEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-missing")
        val entity = Entity(
            entityId = EntityId("entity-1"),
            entityType = "person",
            primaryLabel = "Alex",
            provenanceId = ProvenanceId("provenance-1"),
            createdAt = fixedInstant,
        )
        val memoryRetrieval = FakeMemoryRetrieval(onGetEntity = { _, id -> if (id == entity.entityId) entity else null })
        val resolver = EvidenceIntelligenceInputResolver(FakeEvidenceCustodian { _, id -> EvidenceRetrievalResult.NotFound(id) }, memoryRetrieval)

        val (_, memoryCoreResults) = resolver.resolve(
            minimalRequest(memoryCoreReferences = listOf(missingEndpoint, foundEndpoint)),
        )

        assertEquals(2, memoryCoreResults.size)
        assertEquals(missingEndpoint, memoryCoreResults[0].first)
        assertNull(memoryCoreResults[0].second, "the missing reference must resolve to null, self-identified by its own endpoint")
        assertEquals(foundEndpoint, memoryCoreResults[1].first)
        assertIs<MemoryCoreRecord.OfEntity>(memoryCoreResults[1].second)
    }

    @Test
    fun `a Memory Core reference of a kind Memory Core does not own fails explicitly, never as an ordinary not-found`() = runTest {
        // A completely unconfigured FakeMemoryRetrieval throws UnsupportedOperationException from
        // every one of its ten methods; observing IllegalArgumentException instead proves none of
        // them -- direct lookup or query -- was ever called for an unsupported recordKind.
        val memoryRetrieval = FakeMemoryRetrieval()
        val resolver = EvidenceIntelligenceInputResolver(FakeEvidenceCustodian { _, id -> EvidenceRetrievalResult.NotFound(id) }, memoryRetrieval)
        val endpoint = RelationshipEndpoint(RelationshipEndpoint.CONVERSATION_TURN, "turn-1")

        val exception = assertFailsWith<IllegalArgumentException> {
            resolver.resolve(minimalRequest(memoryCoreReferences = listOf(endpoint)))
        }

        assertTrue(exception.message!!.contains(RelationshipEndpoint.CONVERSATION_TURN))
        assertTrue(exception.message!!.contains("turn-1"))
    }

    @Test
    fun `an unsupported Memory Core reference kind is never silently represented as a null, not-found result`() = runTest {
        val memoryRetrieval = FakeMemoryRetrieval()
        val resolver = EvidenceIntelligenceInputResolver(FakeEvidenceCustodian { _, id -> EvidenceRetrievalResult.NotFound(id) }, memoryRetrieval)
        val endpoint = RelationshipEndpoint(RelationshipEndpoint.KNOWLEDGE_RECORD, "knowledge-1")

        // If this resolved to null instead of throwing, the line below would run and this
        // assertion would fail -- demonstrating the two outcomes are not conflated.
        assertFailsWith<IllegalArgumentException> {
            val (_, memoryCoreResults) = resolver.resolve(minimalRequest(memoryCoreReferences = listOf(endpoint)))
            assertNull(memoryCoreResults.singleOrNull()?.second, "should never reach this point")
        }
    }

    @Test
    fun `resolve never calls any of MemoryRetrieval's six query-based methods`() = runTest {
        // FakeMemoryRetrieval throws UnsupportedOperationException from all six query methods;
        // reaching this test's own assertions without that exception proves none was called.
        val memoryRetrieval = FakeMemoryRetrieval(onGetEntity = { _, _ -> null })
        val resolver = EvidenceIntelligenceInputResolver(FakeEvidenceCustodian { _, id -> EvidenceRetrievalResult.NotFound(id) }, memoryRetrieval)

        resolver.resolve(
            minimalRequest(memoryCoreReferences = listOf(RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1"))),
        )
    }

    // ================= Fakes =================

    /** Configurable [EvidenceCustodian.retrieve]; [accept] throws if reached -- Unit 2 never accepts. */
    private class FakeEvidenceCustodian(
        private val onRetrieve: (PrincipalId, EvidenceArtifactId) -> EvidenceRetrievalResult,
    ) : EvidenceCustodian {

        override suspend fun accept(
            requestingPrincipalId: PrincipalId,
            candidate: CandidateEvidenceArtifact,
        ): EvidenceAcceptanceResult = throw UnsupportedOperationException("Unit 2 must never call EvidenceCustodian.accept")

        override suspend fun retrieve(
            requestingPrincipalId: PrincipalId,
            evidenceArtifactId: EvidenceArtifactId,
        ): EvidenceRetrievalResult = onRetrieve(requestingPrincipalId, evidenceArtifactId)

        override suspend fun retrieveManifest(
            requestingPrincipalId: PrincipalId,
            evidenceArtifactId: EvidenceArtifactId,
        ): EvidenceManifestRetrievalResult = throw UnsupportedOperationException("not used by EvidenceIntelligenceInputResolver")
    }

    /**
     * Configurable direct-lookup methods only. The six query-based methods
     * throw [UnsupportedOperationException] -- Unit 2's own governing
     * instruction ("no code path resolves an evidence reference by any
     * means other than the two named boundaries") forbids ever reaching
     * them.
     */
    private class FakeMemoryRetrieval(
        private val onGetEntity: (PrincipalId, EntityId) -> Entity? = { _, _ -> throw UnsupportedOperationException("not configured") },
        private val onGetDocument: (PrincipalId, DocumentId) -> Document? = { _, _ -> throw UnsupportedOperationException("not configured") },
        private val onGetAssertion: (PrincipalId, AssertionId) -> Assertion? = { _, _ -> throw UnsupportedOperationException("not configured") },
        private val onGetRelationship: (PrincipalId, RelationshipId) -> Relationship? = { _, _ -> throw UnsupportedOperationException("not configured") },
    ) : MemoryRetrieval {
        override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? =
            onGetEntity(requestingPrincipalId, entityId)

        override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document? =
            onGetDocument(requestingPrincipalId, documentId)

        override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion? =
            onGetAssertion(requestingPrincipalId, assertionId)

        override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? =
            onGetRelationship(requestingPrincipalId, relationshipId)

        override suspend fun findEntities(query: EntityLookupQuery): List<Entity> =
            throw UnsupportedOperationException("Unit 2 must never call findEntities")

        override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> =
            throw UnsupportedOperationException("Unit 2 must never call findDocuments")

        override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> =
            throw UnsupportedOperationException("Unit 2 must never call traverseRelationships")

        override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("Unit 2 must never call findByTimeRange")

        override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("Unit 2 must never call findByMetadata")

        override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("Unit 2 must never call findByProvenance")
    }
}
