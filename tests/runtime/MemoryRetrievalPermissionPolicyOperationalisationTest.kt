package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceRegistry
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Gap #54 Memory Retrieval Operationalisation, Unit 1 policy-mechanism verification. */
class MemoryRetrievalPermissionPolicyOperationalisationTest {

    private val candidatePurpose = AuthorizationPurposeId("test.candidate-evaluation")

    private fun request(
        proposedAction: String,
        targetResources: List<ResourceId> = emptyList(),
        purpose: AuthorizationPurposeId? = null,
    ) = ExecutionRequest(
        requestId = RequestId("req-gap54-unit1-${proposedAction.replace('.', '-')}-${purpose?.value ?: "absent"}"),
        principalId = PrincipalId("user-gap54-unit1"),
        origin = RequestOrigin.TEXT,
        intent = "test only",
        targetResources = targetResources,
        proposedActions = listOf(proposedAction),
        priority = RequestPriority.NORMAL,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = "corr-gap54-unit1",
        authorizationPurpose = purpose,
    )

    private suspend fun vocabulary(vararg entries: Pair<String, ActionResourceMapping>): InMemoryActionVocabulary =
        InMemoryActionVocabulary().also { vocabulary ->
            entries.forEach { (verb, mapping) ->
                vocabulary.register(ActionVocabularyEntry(verb, setOf(mapping)))
            }
        }

    private fun rule(
        resourceType: ResourceType,
        outcome: PermissionDecisionOutcome,
        purpose: AuthorizationPurposeId? = null,
        verb: String? = null,
    ) = PermissionPolicyRule(
        action = PermissionAction.READ,
        resourceType = resourceType,
        outcome = outcome,
        level = PermissionLevel.AUTOMATIC,
        authorizationPurpose = purpose,
        proposedAction = verb,
    )

    private class RecordingResourceRegistry : ResourceRegistry {
        var resolveCalls = 0

        override suspend fun register(resource: Resource): ResourceId = error("register must not be called")
        override suspend fun resolve(resourceId: ResourceId): Resource? {
            resolveCalls += 1
            return null
        }
        override suspend fun update(resource: Resource): Resource = error("update must not be called")
        override suspend fun listByOwner(owner: PrincipalId): List<Resource> = emptyList()
    }

    @Test
    fun `memory retrieve derives READ MEMORY through only the governed targetless configuration`() = runTest {
        val registry = RecordingResourceRegistry()
        val policy = DefaultPermissionPolicy(
            ActionMapper(vocabulary("memory.retrieve" to ActionResourceMapping(PermissionAction.READ, ResourceType.MEMORY))),
            registry,
            listOf(rule(ResourceType.MEMORY, PermissionDecisionOutcome.APPROVED, verb = "memory.retrieve")),
            targetlessResourceTypesByProposedAction = mapOf("memory.retrieve" to setOf(ResourceType.MEMORY)),
        )

        val decision = policy.evaluate(request("memory.retrieve"))

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        assertEquals(PermissionAction.READ, decision.action)
        assertEquals(0, registry.resolveCalls, "targetless derivation must not resolve or fabricate a Resource")
    }

    @Test
    fun `memory retrieve document derives READ DOCUMENT`() = runTest {
        val policy = DefaultPermissionPolicy(
            ActionMapper(vocabulary("memory.retrieve_document" to ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT))),
            RecordingResourceRegistry(),
            listOf(rule(ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, verb = "memory.retrieve_document")),
            targetlessResourceTypesByProposedAction = mapOf("memory.retrieve_document" to setOf(ResourceType.DOCUMENT)),
        )

        val decision = policy.evaluate(request("memory.retrieve_document"))

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        assertEquals(PermissionAction.READ, decision.action)
    }

    @Test
    fun `unknown or unconfigured targetless verbs derive nothing and deny`() = runTest {
        val mapping = ActionResourceMapping(PermissionAction.READ, ResourceType.MEMORY)
        val policy = DefaultPermissionPolicy(
            ActionMapper(vocabulary("memory.retrieve" to mapping, "memory.unknown" to mapping)),
            RecordingResourceRegistry(),
            listOf(rule(ResourceType.MEMORY, PermissionDecisionOutcome.APPROVED)),
            targetlessResourceTypesByProposedAction = mapOf("memory.retrieve" to setOf(ResourceType.MEMORY)),
        )

        assertEquals(PermissionDecisionOutcome.DENIED, policy.evaluate(request("memory.unknown")).decision)
        assertEquals(PermissionDecisionOutcome.DENIED, policy.evaluate(request("not.registered")).decision)
    }

    @Test
    fun `the targetless configuration rejects verbs and mappings outside the frozen closed set`() = runTest {
        val mapper = ActionMapper(InMemoryActionVocabulary())
        val registry = RecordingResourceRegistry()

        assertFailsWith<IllegalArgumentException> {
            DefaultPermissionPolicy(
                mapper,
                registry,
                emptyList(),
                targetlessResourceTypesByProposedAction = mapOf("memory.unknown" to setOf(ResourceType.MEMORY)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DefaultPermissionPolicy(
                mapper,
                registry,
                emptyList(),
                targetlessResourceTypesByProposedAction = mapOf("memory.retrieve" to setOf(ResourceType.DOCUMENT)),
            )
        }
    }

    @Test
    fun `ordinary target resource resolution remains unchanged and does not use targetless configuration`() = runTest {
        val resourceId = ResourceId("calendar-unit1")
        val resourceRegistry = InMemoryResourceRegistry().also {
            it.register(
                Resource(
                    resourceId = resourceId,
                    resourceType = ResourceType.CALENDAR,
                    displayName = "Calendar",
                    ownerPrincipalId = PrincipalId("user-gap54-unit1"),
                    sensitivity = ResourceSensitivity.HOUSEHOLD,
                    lifecycleState = ResourceLifecycleState.AVAILABLE,
                    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                    updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                    source = "test",
                ),
            )
        }
        val policy = DefaultPermissionPolicy(
            ActionMapper(vocabulary("calendar.read" to ActionResourceMapping(PermissionAction.READ, ResourceType.CALENDAR))),
            resourceRegistry,
            listOf(rule(ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED)),
            targetlessResourceTypesByProposedAction = mapOf("memory.retrieve" to setOf(ResourceType.MEMORY)),
        )

        assertEquals(
            PermissionDecisionOutcome.APPROVED,
            policy.evaluate(request("calendar.read", targetResources = listOf(resourceId))).decision,
        )
    }

    @Test
    fun `derivation and test-local action registration without a policy rule grant no authority`() = runTest {
        val policy = DefaultPermissionPolicy(
            ActionMapper(vocabulary("memory.retrieve" to ActionResourceMapping(PermissionAction.READ, ResourceType.MEMORY))),
            RecordingResourceRegistry(),
            emptyList(),
            targetlessResourceTypesByProposedAction = mapOf("memory.retrieve" to setOf(ResourceType.MEMORY)),
        )

        assertEquals(PermissionDecisionOutcome.DENIED, policy.evaluate(request("memory.retrieve")).decision)
    }

    @Test
    fun `a verb-specific rule matches only its exact resolved verb`() = runTest {
        val mapping = ActionResourceMapping(PermissionAction.READ, ResourceType.MEMORY)
        val policy = DefaultPermissionPolicy(
            ActionMapper(vocabulary("memory.retrieve" to mapping, "knowledge.retrieve" to mapping)),
            RecordingResourceRegistry(),
            listOf(rule(ResourceType.MEMORY, PermissionDecisionOutcome.APPROVED, verb = "memory.retrieve")),
            targetlessResourceTypesByProposedAction = mapOf("memory.retrieve" to setOf(ResourceType.MEMORY)),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, policy.evaluate(request("memory.retrieve")).decision)
        assertEquals(PermissionDecisionOutcome.DENIED, policy.evaluate(request("knowledge.retrieve")).decision)
    }

    @Test
    fun `verb-specific rule takes precedence over a coarse rule independent of ordering`() = runTest {
        val mapper = ActionMapper(vocabulary("memory.retrieve" to ActionResourceMapping(PermissionAction.READ, ResourceType.MEMORY)))
        val coarseApprove = rule(ResourceType.MEMORY, PermissionDecisionOutcome.APPROVED)
        val verbDeny = rule(ResourceType.MEMORY, PermissionDecisionOutcome.DENIED, verb = "memory.retrieve")

        listOf(listOf(coarseApprove, verbDeny), listOf(verbDeny, coarseApprove)).forEach { rules ->
            val policy = DefaultPermissionPolicy(
                mapper,
                RecordingResourceRegistry(),
                rules,
                targetlessResourceTypesByProposedAction = mapOf("memory.retrieve" to setOf(ResourceType.MEMORY)),
            )
            assertEquals(PermissionDecisionOutcome.DENIED, policy.evaluate(request("memory.retrieve")).decision)
        }
    }

    @Test
    fun `Authorization Purpose and verb specificity interact deterministically`() = runTest {
        val purposeRegistry = InMemoryAuthorizationPurposeRegistry().also { it.register(candidatePurpose) }
        val mapper = ActionMapper(vocabulary("memory.retrieve" to ActionResourceMapping(PermissionAction.READ, ResourceType.MEMORY)))
        val rules = listOf(
            rule(ResourceType.MEMORY, PermissionDecisionOutcome.APPROVED),
            rule(ResourceType.MEMORY, PermissionDecisionOutcome.DENIED, verb = "memory.retrieve"),
            rule(ResourceType.MEMORY, PermissionDecisionOutcome.APPROVED, purpose = candidatePurpose),
            rule(
                ResourceType.MEMORY,
                PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION,
                purpose = candidatePurpose,
                verb = "memory.retrieve",
            ),
        )

        listOf(rules, rules.reversed()).forEach { orderedRules ->
            val policy = DefaultPermissionPolicy(
                mapper,
                RecordingResourceRegistry(),
                orderedRules,
                authorizationPurposeRegistry = purposeRegistry,
                targetlessResourceTypesByProposedAction = mapOf("memory.retrieve" to setOf(ResourceType.MEMORY)),
            )
            assertEquals(
                PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION,
                policy.evaluate(request("memory.retrieve", purpose = candidatePurpose)).decision,
            )
            assertEquals(PermissionDecisionOutcome.DENIED, policy.evaluate(request("memory.retrieve")).decision)
        }
    }

    @Test
    fun `equally specific conflicting rules deny instead of using list order`() = runTest {
        val mapper = ActionMapper(vocabulary("memory.retrieve" to ActionResourceMapping(PermissionAction.READ, ResourceType.MEMORY)))
        val approve = rule(ResourceType.MEMORY, PermissionDecisionOutcome.APPROVED, verb = "memory.retrieve")
        val deny = rule(ResourceType.MEMORY, PermissionDecisionOutcome.DENIED, verb = "memory.retrieve")

        listOf(listOf(approve, deny), listOf(deny, approve)).forEach { rules ->
            val policy = DefaultPermissionPolicy(
                mapper,
                RecordingResourceRegistry(),
                rules,
                targetlessResourceTypesByProposedAction = mapOf("memory.retrieve" to setOf(ResourceType.MEMORY)),
            )
            assertEquals(PermissionDecisionOutcome.DENIED, policy.evaluate(request("memory.retrieve")).decision)
        }
    }

    @Test
    fun `incomparable purpose-only and verb-only maximal rules are ambiguous and deny`() = runTest {
        val purposeRegistry = InMemoryAuthorizationPurposeRegistry().also { it.register(candidatePurpose) }
        val policy = DefaultPermissionPolicy(
            ActionMapper(vocabulary("memory.retrieve" to ActionResourceMapping(PermissionAction.READ, ResourceType.MEMORY))),
            RecordingResourceRegistry(),
            listOf(
                rule(ResourceType.MEMORY, PermissionDecisionOutcome.APPROVED, purpose = candidatePurpose),
                rule(ResourceType.MEMORY, PermissionDecisionOutcome.APPROVED, verb = "memory.retrieve"),
            ),
            authorizationPurposeRegistry = purposeRegistry,
            targetlessResourceTypesByProposedAction = mapOf("memory.retrieve" to setOf(ResourceType.MEMORY)),
        )

        assertEquals(
            PermissionDecisionOutcome.DENIED,
            policy.evaluate(request("memory.retrieve", purpose = candidatePurpose)).decision,
        )
    }
}
