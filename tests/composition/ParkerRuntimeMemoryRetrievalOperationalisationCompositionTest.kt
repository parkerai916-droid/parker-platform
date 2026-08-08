package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceRegistry
import parker.core.interfaces.ResourceType
import parker.core.runtime.ActionMapper
import parker.core.runtime.AuthorizationPurposeRegistry
import parker.core.runtime.DefaultKnowledgeCandidateEvaluator
import parker.core.runtime.DefaultPermissionPolicy
import parker.core.runtime.EvidenceIntelligenceInputResolver
import parker.core.runtime.InMemoryActionVocabulary
import parker.core.runtime.InMemoryAuthorizationPurposeRegistry
import parker.core.runtime.PermissionPolicyRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Gap #54 Memory Retrieval Operationalisation Unit 2 composition and fail-closed verification. */
class ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest {

    private val candidatePurpose = AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
    private val evidencePurpose = AuthorizationPurposeId("evidence-intelligence.input-resolution")
    private val owner = PrincipalId("user.owner-gap54-unit2-composition")

    private fun config() = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate",
        modelName = "test-model",
        ownerPrincipalId = owner.value,
        localTextChannelModuleId = "channel.local-text-gap54-unit2-composition",
        evidenceStorageRootPath = Files.createTempDirectory("gap54-unit2-evidence").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("gap54-unit2-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("gap54-unit2-memory").resolve("memory.log").toString(),
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
    fun `production registry contains exactly both accepted active real purposes`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registry = policy(runtime).privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")
        val entries = registry.privateField<Map<*, *>>("entries")

        assertEquals(setOf(candidatePurpose, evidencePurpose), entries.keys)
        assertTrue(registry.isActive(candidatePurpose))
        assertTrue(registry.isActive(evidencePurpose))

        runtime.shutdown()
    }

    @Test
    fun `production contains exactly both verb-specific DENIED guards and no purpose-specific rule`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val rules = policy(runtime).privateField<List<PermissionPolicyRule>>("rules")
        val memoryVerbRules = rules.filter {
            it.proposedAction == PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME ||
                it.proposedAction == PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME
        }

        assertEquals(2, memoryVerbRules.size)
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
            memoryVerbRules.toSet(),
        )
        assertTrue(rules.none { it.authorizationPurpose != null }, "Unit 4 purpose-specific policy has not begun")

        runtime.shutdown()
    }

    @Test
    fun `both guards outrank existing coarse approvals in production and reversed rule order`() = runTest {
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
    fun `absent active unregistered and retired purposes all remain denied for both verbs`() = runTest {
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
                assertEquals(
                    PermissionDecisionOutcome.DENIED,
                    engine.evaluate(request(verb, purpose, "${verb.replace('.', '-')}-$index")).decision,
                    "verb=$verb purpose=$purpose",
                )
            }
        }

        runtime.shutdown()
    }

    @Test
    fun `both consumers still share the unbound decorator and declare no purpose field`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val evidenceIntelligence = runtime.privateField<Any>("evidenceIntelligence")
        val resolver = evidenceIntelligence.privateField<EvidenceIntelligenceInputResolver>("inputResolver")
        val resolverRetrieval = resolver.privateField<MemoryRetrieval>("memoryRetrieval")
        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val submission = acceptanceCoordinator.privateField<Any>("knowledgeSubmission")
        val evaluator = submission.privateField<DefaultKnowledgeCandidateEvaluator>("evaluator")
        val evaluatorRetrieval = evaluator.privateField<MemoryRetrieval>("memoryRetrieval")

        assertTrue(resolverRetrieval is PermissionFilteredMemoryRetrieval)
        assertSame(resolverRetrieval, evaluatorRetrieval)
        assertFalse(DefaultKnowledgeCandidateEvaluator::class.java.declaredFields.any { it.name.contains("authorizationPurpose", true) })
        assertFalse(EvidenceIntelligenceInputResolver::class.java.declaredFields.any { it.name.contains("authorizationPurpose", true) })
        assertFalse(PermissionFilteredMemoryRetrieval::class.java.declaredFields.any { it.name.contains("authorizationPurpose", true) })

        runtime.shutdown()
    }

    private fun PermissionPolicyRule.decisionIsApproved(): Boolean =
        outcome == PermissionDecisionOutcome.APPROVED || outcome == PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
}
