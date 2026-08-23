package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.KnowledgeCandidate
import parker.core.interfaces.KnowledgeCandidateEvaluation
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceRegistry
import parker.core.interfaces.ResourceType
import parker.core.runtime.ActionMapper
import parker.core.runtime.AuthorizationPurposeRegistry
import parker.core.runtime.DefaultKnowledgeCandidateEvaluator
import parker.core.runtime.DefaultPermissionPolicy
import parker.core.runtime.DurableMemoryCore
import parker.core.runtime.EvidenceIntelligenceInputResolver
import parker.core.runtime.InMemoryActionVocabulary
import parker.core.runtime.InMemoryAuthorizationPurposeRegistry
import parker.core.runtime.PermissionPolicyRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Gap #54 Memory Retrieval Operationalisation Units 2-4 composition and authorization verification. */
class ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest {

    private val candidatePurpose = AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
    private val evidencePurpose = AuthorizationPurposeId("evidence-intelligence.input-resolution")
    private val reasoningContextPurpose = AuthorizationPurposeId("knowledge-memory.reasoning-context-retrieval")
    private val owner = PrincipalId("user.owner-gap54-unit2-composition")

    private fun config() = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate",
        modelName = "test-model",
        ownerPrincipalId = owner.value,
        localTextChannelModuleId = "channel.local-text-gap54-unit2-composition",
        evidenceStorageRootPath = Files.createTempDirectory("gap54-unit2-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("gap54-unit2-evidence-manifest").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("gap54-unit2-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("gap54-unit2-memory").resolve("memory.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("knowledge-items-test").resolve("items.log").toString(),
    )

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    private fun engine(runtime: ParkerRuntime): PermissionEngine = runtime.privateField("permissionEngine")
    private fun policy(runtime: ParkerRuntime): DefaultPermissionPolicy = engine(runtime).privateField("policy")

    private fun request(verb: String, purpose: AuthorizationPurposeId? = null, suffix: String = "request") = ExecutionRequest(
        requestId = RequestId("req-gap54-unit2-$suffix"),
        principalId = owner,
        origin = RequestOrigin.TEXT,
        intent = "Unit 2 fail-closed verification",
        targetResources = emptyList(),
        proposedActions = listOf(verb),
        priority = RequestPriority.NORMAL,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = "corr-gap54-unit2-$suffix",
        authorizationPurpose = purpose,
    )

    @Test
    fun `production registers exactly both Memory retrieval actions and closed derivations`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val policy = policy(runtime)
        val mapper = policy.privateField<ActionMapper>("actionMapper")
        val vocabulary = mapper.privateField<InMemoryActionVocabulary>("vocabulary")
        val derivations = policy.privateField<Map<String, Set<ResourceType>>>("targetlessResourceTypesByProposedAction")

        assertEquals(
            setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.MEMORY)),
            vocabulary.lookup(PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME)?.mappings,
        )
        assertEquals(
            setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT)),
            vocabulary.lookup(PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME)?.mappings,
        )
        assertEquals(
            mapOf(
                PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME to setOf(ResourceType.MEMORY),
                PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME to setOf(ResourceType.DOCUMENT),
            ),
            derivations,
        )

        runtime.shutdown()
    }

    @Test
    fun `production registry contains exactly all three accepted active real purposes`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registry = policy(runtime).privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")
        val entries = registry.privateField<Map<*, *>>("entries")

        assertEquals(setOf(candidatePurpose, evidencePurpose, reasoningContextPurpose), entries.keys)
        assertTrue(registry.isActive(candidatePurpose))
        assertTrue(registry.isActive(evidencePurpose))
        assertTrue(registry.isActive(reasoningContextPurpose))

        runtime.shutdown()
    }

    @Test
    fun `production contains exactly two unchanged guards, two candidate-only approval rules, and one reasoning-context memory_retrieve approval`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val rules = policy(runtime).privateField<List<PermissionPolicyRule>>("rules")
        val memoryVerbRules = rules.filter {
            it.proposedAction == PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME ||
                it.proposedAction == PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME
        }

        assertEquals(5, memoryVerbRules.size)
        assertEquals(
            setOf(
                PermissionPolicyRule(
                    PermissionAction.READ,
                    ResourceType.MEMORY,
                    PermissionDecisionOutcome.DENIED,
                    parker.core.interfaces.PermissionLevel.AUTOMATIC,
                    proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
                ),
                PermissionPolicyRule(
                    PermissionAction.READ,
                    ResourceType.DOCUMENT,
                    PermissionDecisionOutcome.DENIED,
                    parker.core.interfaces.PermissionLevel.AUTOMATIC,
                    proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
                ),
            ),
            memoryVerbRules.filter { it.outcome == PermissionDecisionOutcome.DENIED }.toSet(),
        )
        assertEquals(
            setOf(
                PermissionPolicyRule(
                    PermissionAction.READ,
                    ResourceType.MEMORY,
                    PermissionDecisionOutcome.APPROVED,
                    PermissionLevel.AUTOMATIC,
                    candidatePurpose,
                    PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
                ),
                PermissionPolicyRule(
                    PermissionAction.READ,
                    ResourceType.DOCUMENT,
                    PermissionDecisionOutcome.APPROVED,
                    PermissionLevel.AUTOMATIC,
                    candidatePurpose,
                    PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
                ),
                PermissionPolicyRule(
                    PermissionAction.READ,
                    ResourceType.MEMORY,
                    PermissionDecisionOutcome.APPROVED,
                    PermissionLevel.AUTOMATIC,
                    reasoningContextPurpose,
                    PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
                ),
            ),
            memoryVerbRules.filter { it.outcome == PermissionDecisionOutcome.APPROVED }.toSet(),
        )
        assertTrue(rules.none { it.authorizationPurpose == evidencePurpose && it.decisionIsApproved() })

        runtime.shutdown()
    }

    @Test
    fun `candidate rules outrank guards while guards outrank coarse approvals independent of rule order`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val productionPolicy = policy(runtime)
        val rules = productionPolicy.privateField<List<PermissionPolicyRule>>("rules")
        val mapper = productionPolicy.privateField<ActionMapper>("actionMapper")
        val resourceRegistry = productionPolicy.privateField<ResourceRegistry>("resourceRegistry")
        val purposeRegistry = productionPolicy.privateField<AuthorizationPurposeRegistry>("authorizationPurposeRegistry")
        val derivations = productionPolicy.privateField<Map<String, Set<ResourceType>>>("targetlessResourceTypesByProposedAction")

        assertTrue(rules.any { it.action == PermissionAction.READ && it.resourceType == ResourceType.MEMORY && it.proposedAction == null && it.decisionIsApproved() })
        assertTrue(rules.any { it.action == PermissionAction.READ && it.resourceType == ResourceType.DOCUMENT && it.proposedAction == null && it.decisionIsApproved() })

        val reversedPolicy = DefaultPermissionPolicy(
            actionMapper = mapper,
            resourceRegistry = resourceRegistry,
            rules = rules.reversed(),
            authorizationPurposeRegistry = purposeRegistry,
            targetlessResourceTypesByProposedAction = derivations,
        )

        listOf(productionPolicy, reversedPolicy).forEachIndexed { index, evaluatedPolicy ->
            assertEquals(
                PermissionDecisionOutcome.APPROVED,
                evaluatedPolicy.evaluate(request(PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME, candidatePurpose, "candidate-memory-$index")).decision,
            )
            assertEquals(
                PermissionDecisionOutcome.APPROVED,
                evaluatedPolicy.evaluate(request(PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME, candidatePurpose, "candidate-document-$index")).decision,
            )
            assertEquals(
                PermissionDecisionOutcome.DENIED,
                evaluatedPolicy.evaluate(request(PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME, suffix = "memory-$index")).decision,
            )
            assertEquals(
                PermissionDecisionOutcome.DENIED,
                evaluatedPolicy.evaluate(request(PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME, suffix = "document-$index")).decision,
            )
        }

        runtime.shutdown()
    }

    @Test
    fun `exact authorization matrix approves only candidate purpose and denies every other purpose state`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val engine = engine(runtime)
        val registry = policy(runtime).privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")
        val unregistered = AuthorizationPurposeId("test.unit2-unregistered")
        val retired = AuthorizationPurposeId("test.unit2-retired")
        registry.register(retired)
        registry.retire(retired)

        val purposes = listOf(null, candidatePurpose, evidencePurpose, unregistered, retired)
        val verbs = listOf(
            PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
            PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
        )
        verbs.forEach { verb ->
            purposes.forEachIndexed { index, purpose ->
                val expected = if (purpose == candidatePurpose) PermissionDecisionOutcome.APPROVED else PermissionDecisionOutcome.DENIED
                assertEquals(
                    expected,
                    engine.evaluate(request(verb, purpose, "${verb.replace('.', '-')}-$index")).decision,
                    "verb=$verb purpose=$purpose",
                )
            }
        }

        runtime.shutdown()
    }

    @Test
    fun `candidate authority rejects wrong verb and conflicting duplicate authority denies by ambiguity`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val productionPolicy = policy(runtime)
        assertEquals(
            PermissionDecisionOutcome.DENIED,
            productionPolicy.evaluate(request("memory.retrieve_unapproved", candidatePurpose, "wrong-verb")).decision,
        )

        val rules = productionPolicy.privateField<List<PermissionPolicyRule>>("rules")
        val conflicting = rules + PermissionPolicyRule(
            action = PermissionAction.READ,
            resourceType = ResourceType.MEMORY,
            outcome = PermissionDecisionOutcome.DENIED,
            level = PermissionLevel.AUTOMATIC,
            authorizationPurpose = candidatePurpose,
            proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
        )
        val ambiguousPolicy = DefaultPermissionPolicy(
            actionMapper = productionPolicy.privateField("actionMapper"),
            resourceRegistry = productionPolicy.privateField("resourceRegistry"),
            rules = conflicting,
            authorizationPurposeRegistry = productionPolicy.privateField("authorizationPurposeRegistry"),
            targetlessResourceTypesByProposedAction = productionPolicy.privateField("targetlessResourceTypesByProposedAction"),
        )
        assertEquals(
            PermissionDecisionOutcome.DENIED,
            ambiguousPolicy.evaluate(request(PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME, candidatePurpose, "ambiguous")).decision,
        )

        runtime.shutdown()
    }

    @Test
    fun `real candidate evaluator resolves genuine Memory Core evidence through its shared purpose-bound decorator`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val rawMemoryCore = acceptanceCoordinator.privateField<DurableMemoryCore>("memoryCore")
        val provenance = rawMemoryCore.createProvenance(
            owner,
            CandidateProvenance(
                sourceIdentifier = "unit-4-real-candidate",
                sourceType = "test-harness",
                acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                contentNature = ContentNature.ORIGINAL,
            ),
        )
        val assertion = rawMemoryCore.createAssertion(
            owner,
            CandidateAssertion(
                statement = "Unit 4 candidate evidence is resolvable",
                provenanceId = provenance.provenanceId,
                confidence = 1.0,
            ),
        )
        val contradictingAssertion = rawMemoryCore.createAssertion(
            owner,
            CandidateAssertion(
                statement = "Unit 4 candidate evidence is contradicted",
                provenanceId = provenance.provenanceId,
                confidence = 1.0,
            ),
        )
        rawMemoryCore.createRelationship(
            owner,
            CandidateRelationship(
                relationshipType = Relationship.CONTRADICTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, assertion.assertionId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, contradictingAssertion.assertionId.value),
                directional = false,
                provenanceId = provenance.provenanceId,
            ),
        )
        val submission = acceptanceCoordinator.privateField<Any>("knowledgeSubmission")
        val evaluator = submission.privateField<DefaultKnowledgeCandidateEvaluator>("evaluator")

        val promoted = assertIs<KnowledgeCandidateEvaluation.Promote>(
            evaluator.evaluate(
                owner,
                KnowledgeCandidate(
                    evidenceReference = MemoryCoreRecordReference.ToAssertion(assertion.assertionId),
                    explicitlyRequested = true,
                ),
            ),
        )
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, promoted.item.evidentialState)
        assertIs<KnowledgeCandidateEvaluation.Reject>(
            evaluator.evaluate(
                PrincipalId("user.unregistered-accountable-principal"),
                KnowledgeCandidate(
                    evidenceReference = MemoryCoreRecordReference.ToAssertion(assertion.assertionId),
                    explicitlyRequested = true,
                ),
            ),
        )

        runtime.shutdown()
    }

    @Test
    fun `both consumers receive distinct exact-purpose views over one shared parent decorator`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val evidenceIntelligence = runtime.privateField<Any>("evidenceIntelligence")
        val resolver = evidenceIntelligence.privateField<EvidenceIntelligenceInputResolver>("inputResolver")
        val resolverRetrieval = resolver.privateField<MemoryRetrieval>("memoryRetrieval")
        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val submission = acceptanceCoordinator.privateField<Any>("knowledgeSubmission")
        val evaluator = submission.privateField<DefaultKnowledgeCandidateEvaluator>("evaluator")
        val evaluatorRetrieval = evaluator.privateField<MemoryRetrieval>("memoryRetrieval")

        assertNotSame(resolverRetrieval, evaluatorRetrieval)
        val resolverParent = resolverRetrieval.privateField<PermissionFilteredMemoryRetrieval>("parent")
        val evaluatorParent = evaluatorRetrieval.privateField<PermissionFilteredMemoryRetrieval>("parent")
        assertSame(resolverParent, evaluatorParent)
        assertEquals(evidencePurpose.value, resolverRetrieval.privateField<String>("authorizationPurpose"))
        assertEquals(candidatePurpose.value, evaluatorRetrieval.privateField<String>("authorizationPurpose"))
        assertFalse(DefaultKnowledgeCandidateEvaluator::class.java.declaredFields.any { it.name.contains("authorizationPurpose", true) })
        assertFalse(EvidenceIntelligenceInputResolver::class.java.declaredFields.any { it.name.contains("authorizationPurpose", true) })
        assertFalse(PermissionFilteredMemoryRetrieval::class.java.declaredFields.any { it.name.contains("authorizationPurpose", true) })

        runtime.shutdown()
    }

    private fun PermissionPolicyRule.decisionIsApproved(): Boolean =
        outcome == PermissionDecisionOutcome.APPROVED || outcome == PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
}
