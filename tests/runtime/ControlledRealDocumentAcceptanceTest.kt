package parker.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import parker.composition.ParkerRuntime
import parker.composition.ParkerRuntimeConfig
import parker.composition.RecordingParkerLogger
import parker.core.interfaces.*
import parker.core.runtime.CaseAssignmentOutcome
import parker.core.runtime.CaseCreationOutcome
import parker.core.runtime.EvidenceIntelligenceInputResolver
import parker.core.runtime.GroundedReplyParser
import parker.core.runtime.StrictEvidenceReasoningInvocation
import parker.core.runtime.StrictEvidenceReasoningFailure
import parker.core.runtime.StrictEvidenceReasoningResult

/** Controlled real-document acceptance; never substitutes a fixture when the source is absent. */
class ControlledRealDocumentAcceptanceTest {
    private val source = Path.of("/home/steve/Deed of Representation Michael Kellec.pdf")
    private val owner = PrincipalId("user.task20-real-document")

    @Test
    fun `real deed survives governed acceptance and retrieval`() = runTest {
        assumeTrue(Files.isRegularFile(source), "preferred real document is unavailable")
        val bytes = Files.readAllBytes(source)
        val sourceHash = sha256(bytes)
        assertEquals("5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e", sourceHash)
        println("TASK20 source=${source.toAbsolutePath()} bytes=${bytes.size} sha256=$sourceHash")

        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        try {
            val created = assertIs<CaseCreationOutcome.Created>(runtime.createCaseAsOwner("Task 20 controlled real document"))
            val imported = assertIs<OwnerLocalFileIngressOutcome.Accepted>(runtime.importEvidenceFileAsOwner(source.toString(), "application/pdf"))
            val evidenceId = imported.acceptedEvidenceArtifact.evidenceArtifactId
            assertIs<CaseAssignmentOutcome.Assigned>(runtime.assignEvidenceToCaseAsOwner(evidenceId, created.case.caseId))
            assertEquals(created.case.caseId, runtime.currentCaseAssignmentAsOwner(evidenceId))
            val processing = runtime.invokeTierAIngestionAsOwner(evidenceId)
            val retrieved = assertIs<EvidenceRetrievalResult.Found>(runtime.retrieveEvidence(owner, evidenceId))
            assertTrue(bytes.contentEquals(retrieved.content))
            println("TASK20 caseId=${created.case.caseId.value} evidenceArtifactId=${evidenceId.value} processing=${processing::class.simpleName} retrieval=FOUND")

            val strict: suspend ((ReasoningProviderRequest) -> GroundedProposition) -> StrictEvidenceReasoningResult = { proposition ->
                StrictEvidenceReasoningInvocation(
                    EvidenceIntelligenceInputResolver(runtimeCustodian(runtime), emptyMemoryRetrieval()),
                    provider { request ->
                        GroundedReplyParser().encode(
                            GroundedReply.fromContext(request.reasoningContext, listOf(proposition(request))),
                            request.reasoningContext,
                        )
                    },
                ).invoke(EvidenceAnalysisRequest("task20-strict", owner, listOf(evidenceId)))
            }
            val supported = strict { request ->
                val entry = request.reasoningContext.suppliedGovernedEntries.filterIsInstance<ReasoningContextEntry.GovernedEvidence>().single()
                val support = StructuredClaimSupport.ExactIdentity(entry, ExactGovernedIdentity.EVIDENCE_ARTIFACT_ID, evidenceId.value)
                GroundedProposition(support.claimText, GroundedPropositionClassification.SUPPORTED_FACT, supportReferences = listOf(entry), structuredSupports = listOf(support))
            }
            assertIs<StrictEvidenceReasoningResult.Success>(supported)
            val inference = strict { request ->
                GroundedProposition("the document may relate to a later matter", GroundedPropositionClassification.INFERENCE, basisReferences = listOf(request.reasoningContext.suppliedGovernedEntries.single()))
            }
            assertEquals(GroundedPropositionClassification.INFERENCE, assertIs<StrictEvidenceReasoningResult.Success>(inference).groundedReply.propositions.single().classification)
            assertIs<StrictEvidenceReasoningResult.Success>(strict { GroundedProposition("a particular payment is established", GroundedPropositionClassification.NOT_ESTABLISHED) })
            val unsupported = strict { request ->
                GroundedProposition("the source proves an unstated legal conclusion", GroundedPropositionClassification.SUPPORTED_FACT, supportReferences = listOf(request.reasoningContext.suppliedGovernedEntries.single()))
            }
            assertIs<StrictEvidenceReasoningResult.HumanReviewRequired>(unsupported)
            println("TASK20 strict=SUPPORTED_FACT,INFERENCE,NOT_ESTABLISHED,HUMAN_REVIEW_REQUIRED")

            val publicResult = runtime.reasonStrictlyFromEvidenceAsAgent(
                owner,
                EvidenceAnalysisRequest("task20-public-strict", owner, listOf(evidenceId)),
            )
            assertEquals(
                StrictEvidenceReasoningFailure.PROVIDER_FAILURE,
                assertIs<StrictEvidenceReasoningResult.Failed>(publicResult).failure,
            )
            assertIs<StrictEvidenceReasoningResult.Failed>(
                runtime.reasonStrictlyFromEvidenceAsAgent(
                    PrincipalId("wrong-principal"),
                    EvidenceAnalysisRequest("task20-wrong-scope", owner, listOf(evidenceId)),
                ),
            )
        } finally {
            runtime.shutdown()
        }
    }

    private fun config(): ParkerRuntimeConfig {
        fun dir(prefix: String) = Files.createTempDirectory(prefix).toString()
        return ParkerRuntimeConfig(
            modelEndpointUrl = "http://127.0.0.1:1/api/generate",
            modelName = "task20-no-model",
            ownerPrincipalId = owner.value,
            localTextChannelModuleId = "channel.task20-real-document",
            evidenceStorageRootPath = dir("task20-evidence-"),
            evidenceSourceManifestStorageRootPath = dir("task20-manifest-"),
            derivativeGenerationStorageRootPath = dir("task20-generation-"),
            derivativeContentStorageRootPath = dir("task20-content-"),
            savedAnalysisStorageRootPath = dir("task20-analysis-"),
            documentIngestionAuditLogPath = Path.of(dir("task20-ingestion-audit-"), "audit.log").toString(),
            evidenceDeletionAuditLogPath = Path.of(dir("task20-deletion-audit-"), "audit.log").toString(),
            memoryCoreDurabilityLogPath = Path.of(dir("task20-memory-"), "memory.log").toString(),
            knowledgeItemDurabilityLogPath = Path.of(dir("task20-knowledge-"), "knowledge.log").toString(),
            caseStorageRootPath = dir("task20-case-"),
            caseAssignmentStorageRootPath = dir("task20-assignment-"),
            caseGovernanceAuditLogPath = Path.of(dir("task20-case-audit-"), "audit.log").toString(),
        )
    }

    private fun provider(response: (ReasoningProviderRequest) -> String) = object : ReasoningProvider {
        override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse = ReasoningProviderResponse.Reply(response(request))
    }

    private fun runtimeCustodian(runtime: ParkerRuntime) = object : EvidenceCustodian {
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult = error("strict acceptance must not write")
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult = runtime.retrieveEvidence(requestingPrincipalId, evidenceArtifactId)
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult = error("not used")
        override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?): EvidenceSourceSubmissionResult = error("strict source submission must not write")
    }

    private fun emptyMemoryRetrieval() = object : MemoryRetrieval {
        override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? = null
        override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document? = null
        override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion? = null
        override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? = null
        override suspend fun findEntities(query: EntityLookupQuery): List<Entity> = emptyList()
        override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> = emptyList()
        override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> = emptyList()
        override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> = emptyList()
        override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> = emptyList()
        override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> = emptyList()
    }

    private companion object {
        fun sha256(bytes: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
