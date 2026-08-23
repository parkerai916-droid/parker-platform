package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceAnalysisResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceIntelligence
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.RelationshipEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Evidence Intelligence, Implementation Unit 5 ("The Evidence Intelligence
 * Operation"). Behavioural tests for [DefaultEvidenceIntelligence],
 * demonstrating -- not merely asserting -- that it assembles Units 2-4
 * exactly as `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
 * §8 Unit 5 and
 * `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_5_PLANNING_AND_BOUNDARY_REVIEW.md`
 * require: orchestration and sequencing (resolve, then optionally reason,
 * then convert), partial completion, the documented, permanent
 * `ReasoningContext(emptyList())` limitation, and dependency boundaries
 * (no acceptance interface, no Permission Engine, no runtime composition,
 * no new public type).
 */
class DefaultEvidenceIntelligenceTest {

    private val principalId = PrincipalId("owner-1")

    private fun request(
        evidenceArtifactIds: List<EvidenceArtifactId> = emptyList(),
        memoryCoreReferences: List<RelationshipEndpoint> = emptyList(),
    ) = EvidenceAnalysisRequest(
        analysisKind = "comparison",
        requestingPrincipalId = principalId,
        evidenceArtifactIds = evidenceArtifactIds,
        memoryCoreReferences = memoryCoreReferences,
    )

    private fun resolverOf(evidenceCustodian: EvidenceCustodian) =
        EvidenceIntelligenceInputResolver(evidenceCustodian, FakeMemoryRetrievalForUnit5())

    // ================= Orchestration and sequencing =================

    @Test
    fun `analyse resolves inputs before ever invoking a Reasoning Provider`() = runTest {
        val callOrder = mutableListOf<String>()
        val artifactId = EvidenceArtifactId("artifact-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id ->
            callOrder += "resolve"
            EvidenceRetrievalResult.Found(id, byteArrayOf(1))
        }
        val reasoningProvider = FakeReasoningProvider {
            callOrder += "reason"
            ReasoningProviderResponse.NoAction
        }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(artifactId)))

        assertEquals(listOf("resolve", "reason"), callOrder)
    }

    @Test
    fun `analyse rejects a request naming nothing to analyse before any resolution or reasoning occurs`() = runTest {
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, _ -> throw AssertionError("must not be called") }
        val reasoningProvider = FakeReasoningProvider { throw AssertionError("must not be called") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        assertFailsWith<IllegalArgumentException> { evidenceIntelligence.analyse(request()) }
    }

    @Test
    fun `a Reply becomes exactly one TransientOutput labelled model-generated, citing every resolved reference`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val endpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val memoryRetrieval = FakeMemoryRetrievalForUnit5(onGetEntity = { _, _ -> parker.core.interfaces.Entity(
            entityId = parker.core.interfaces.EntityId("entity-1"),
            entityType = "person",
            primaryLabel = "Alex",
            provenanceId = parker.core.interfaces.ProvenanceId("provenance-1"),
            createdAt = java.time.Instant.parse("2026-01-01T00:00:00Z"),
        ) })
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("the two invoices agree") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            EvidenceIntelligenceInputResolver(evidenceCustodian, memoryRetrieval),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        val results = evidenceIntelligence.analyse(
            request(evidenceArtifactIds = listOf(artifactId), memoryCoreReferences = listOf(endpoint)),
        )

        val output = assertIs<EvidenceAnalysisResult.TransientOutput>(results.single())
        assertEquals("[${AnalyticalOutputDiscipline.MODEL_GENERATED}] the two invoices agree", output.text)
        assertEquals(listOf(artifactId), output.evidenceArtifactReferences)
        assertEquals(listOf(endpoint), output.memoryCoreReferences)
    }

    @Test
    fun `a NoAction response produces an empty result list, never a fabricated TransientOutput`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        val results = evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(artifactId)))

        assertEquals(emptyList(), results)
    }

    @Test
    fun `a Goal response is treated as an implementation-level anomaly, never silently coerced into a result`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Goal("investigate discrepancy") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        assertFailsWith<UnsupportedOperationException> {
            evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(artifactId)))
        }
    }

    // ================= Reasoning Provider optionality =================

    @Test
    fun `with no Reasoning Provider configured, analyse never invokes reasoning and returns an empty list`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            reasoningCoordinator = null,
        )

        val results = evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(artifactId)))

        assertEquals(emptyList(), results)
    }

    // ================= Partial completion =================

    @Test
    fun `with some inputs resolved and others missing, only the resolved subset is cited -- never the missing ones`() = runTest {
        val foundArtifact = EvidenceArtifactId("artifact-found")
        val missingArtifact = EvidenceArtifactId("artifact-missing")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id ->
            if (id == foundArtifact) EvidenceRetrievalResult.Found(id, byteArrayOf(1)) else EvidenceRetrievalResult.NotFound(id)
        }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("partial finding") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        val results = evidenceIntelligence.analyse(
            request(evidenceArtifactIds = listOf(foundArtifact, missingArtifact)),
        )

        val output = assertIs<EvidenceAnalysisResult.TransientOutput>(results.single())
        assertEquals(listOf(foundArtifact), output.evidenceArtifactReferences)
        assertTrue(missingArtifact !in output.evidenceArtifactReferences)
    }

    @Test
    fun `when every referenced input fails to resolve, analyse returns an empty list and never invokes reasoning`() = runTest {
        val missingArtifact = EvidenceArtifactId("artifact-missing")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.NotFound(id) }
        val reasoningProvider = FakeReasoningProvider { throw AssertionError("must not be called -- nothing resolved to reason about") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        val results = evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(missingArtifact)))

        assertEquals(emptyList(), results)
    }

    @Test
    fun `a missing Memory Core reference is never cited, while a resolved one alongside it still is`() = runTest {
        val foundEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-found")
        val missingEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-missing")
        val entity = parker.core.interfaces.Entity(
            entityId = parker.core.interfaces.EntityId("entity-found"),
            entityType = "person",
            primaryLabel = "Alex",
            provenanceId = parker.core.interfaces.ProvenanceId("provenance-1"),
            createdAt = java.time.Instant.parse("2026-01-01T00:00:00Z"),
        )
        val memoryRetrieval = FakeMemoryRetrievalForUnit5(onGetEntity = { _, id -> if (id.value == "entity-found") entity else null })
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.NotFound(id) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("finding") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            EvidenceIntelligenceInputResolver(evidenceCustodian, memoryRetrieval),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        val results = evidenceIntelligence.analyse(
            request(memoryCoreReferences = listOf(foundEndpoint, missingEndpoint)),
        )

        val output = assertIs<EvidenceAnalysisResult.TransientOutput>(results.single())
        assertEquals(listOf(foundEndpoint), output.memoryCoreReferences)
    }

    // ================= The documented ReasoningContext limitation =================

    @Test
    fun `analyse always supplies an empty ReasoningContext to the Reasoning Provider invocation, never one derived from the request`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val nestedContext = ReasoningContext(listOf("must never reach the Reasoning Provider"))
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        evidenceIntelligence.analyse(
            EvidenceAnalysisRequest(
                analysisKind = "comparison",
                requestingPrincipalId = principalId,
                evidenceArtifactIds = listOf(artifactId),
                reasoningContext = nestedContext,
            ),
        )

        assertEquals(ReasoningContext(emptyList()), reasoningProvider.lastRequest?.reasoningContext)
    }

    // ================= Dependency boundaries =================

    @Test
    fun `the constructor accepts exactly EvidenceIntelligenceInputResolver and a nullable EvidenceIntelligenceReasoningCoordinator`() {
        val constructor = DefaultEvidenceIntelligence::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("EvidenceIntelligenceInputResolver", "EvidenceIntelligenceReasoningCoordinator"), parameterTypes)
    }

    @Test
    fun `DefaultEvidenceIntelligence declares no field of any acceptance-interface or PermissionEngine type`() {
        val forbiddenTypeNames = setOf(
            "EvidenceCustodian",
            "MemoryCore",
            "PermissionEngine",
            "ExecutionPipeline",
            "PlannerRuntime",
        )
        val fieldTypeNames = DefaultEvidenceIntelligence::class.java.declaredFields.map { it.type.simpleName }.toSet()

        assertTrue(
            fieldTypeNames.none { it in forbiddenTypeNames },
            "declared fields were: $fieldTypeNames",
        )
    }

    @Test
    fun `DefaultEvidenceIntelligence implements EvidenceIntelligence and no other declared interface`() {
        val implementedInterfaces = DefaultEvidenceIntelligence::class.java.interfaces.map { it.simpleName }

        assertEquals(listOf("EvidenceIntelligence"), implementedInterfaces)
    }

    @Test
    fun `a Reasoning Provider fault propagates unchanged, never caught or converted into a fabricated result`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val reasoningProvider = FakeReasoningProvider { throw IllegalStateException("model unreachable") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        assertFailsWith<IllegalStateException> {
            evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(artifactId)))
        }
    }

    // ================= Fakes =================

    private class FakeEvidenceCustodianForUnit5(
        private val onRetrieve: (PrincipalId, EvidenceArtifactId) -> EvidenceRetrievalResult,
    ) : EvidenceCustodian {
        override suspend fun accept(
            requestingPrincipalId: PrincipalId,
            candidate: CandidateEvidenceArtifact,
        ): EvidenceAcceptanceResult = throw UnsupportedOperationException("Unit 5 must never call EvidenceCustodian.accept")

        override suspend fun retrieve(
            requestingPrincipalId: PrincipalId,
            evidenceArtifactId: EvidenceArtifactId,
        ): EvidenceRetrievalResult = onRetrieve(requestingPrincipalId, evidenceArtifactId)

        override suspend fun retrieveManifest(
            requestingPrincipalId: PrincipalId,
            evidenceArtifactId: EvidenceArtifactId,
        ): EvidenceManifestRetrievalResult = throw UnsupportedOperationException("Unit 5 must never call EvidenceCustodian.retrieveManifest")
    }

    private class FakeMemoryRetrievalForUnit5(
        private val onGetEntity: (PrincipalId, parker.core.interfaces.EntityId) -> parker.core.interfaces.Entity? =
            { _, _ -> null },
    ) : parker.core.interfaces.MemoryRetrieval {
        override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: parker.core.interfaces.EntityId) =
            onGetEntity(requestingPrincipalId, entityId)

        override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: parker.core.interfaces.DocumentId) = null

        override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: parker.core.interfaces.AssertionId) = null

        override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: parker.core.interfaces.RelationshipId) = null

        override suspend fun findEntities(query: parker.core.interfaces.EntityLookupQuery): List<parker.core.interfaces.Entity> =
            throw UnsupportedOperationException("Unit 5 must never call findEntities")

        override suspend fun findDocuments(query: parker.core.interfaces.DocumentLookupQuery): List<parker.core.interfaces.Document> =
            throw UnsupportedOperationException("Unit 5 must never call findDocuments")

        override suspend fun traverseRelationships(query: parker.core.interfaces.RelationshipTraversalQuery): List<parker.core.interfaces.Relationship> =
            throw UnsupportedOperationException("Unit 5 must never call traverseRelationships")

        override suspend fun findByTimeRange(query: parker.core.interfaces.ChronologicalLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("Unit 5 must never call findByTimeRange")

        override suspend fun findByMetadata(query: parker.core.interfaces.MetadataLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("Unit 5 must never call findByMetadata")

        override suspend fun findByProvenance(query: parker.core.interfaces.ProvenanceLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("Unit 5 must never call findByProvenance")
    }
}
