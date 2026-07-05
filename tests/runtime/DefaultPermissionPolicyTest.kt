package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves Sprint 2, Unit A2
 * (`docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md`):
 * [DefaultPermissionPolicy] implements
 * `docs/specifications/volume-03-core-interfaces/PermissionPolicy.md`
 * against real [ActionMapper]/[ResourceRegistry] dependencies, closing
 * `IMPLEMENTATION_GAPS.md` #25. This file tests [DefaultPermissionPolicy]
 * directly, not through [DefaultPermissionEngine] -- the identity-gate
 * boundary in front of it is `DefaultPermissionEngineTest.kt`'s own
 * responsibility, unchanged by this unit.
 */
class DefaultPermissionPolicyTest {

    /** Records every call made against the wrapped [ResourceRegistry], to prove no side effects. */
    private class RecordingResourceRegistry(
        private val delegate: ResourceRegistry,
        private val calls: MutableList<String>,
    ) : ResourceRegistry by delegate {
        override suspend fun register(resource: Resource): ResourceId {
            calls.add("register")
            return delegate.register(resource)
        }
        override suspend fun resolve(resourceId: ResourceId): Resource? {
            calls.add("resolve")
            return delegate.resolve(resourceId)
        }
        override suspend fun update(resource: Resource): Resource {
            calls.add("update")
            return delegate.update(resource)
        }
        override suspend fun listByOwner(owner: PrincipalId): List<Resource> {
            calls.add("listByOwner")
            return delegate.listByOwner(owner)
        }
    }

    private fun calendarResource(id: String = "res-1") = Resource(
        resourceId = ResourceId(id),
        resourceType = ResourceType.CALENDAR,
        displayName = "Test Calendar",
        ownerPrincipalId = PrincipalId("user-1"),
        sensitivity = ResourceSensitivity.HOUSEHOLD,
        lifecycleState = ResourceLifecycleState.AVAILABLE,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        source = "test",
    )

    private fun request(
        proposedActions: List<String> = listOf("do something"),
        targetResources: List<ResourceId> = listOf(ResourceId("res-1")),
    ) = ExecutionRequest(
        requestId = RequestId("req-1"),
        principalId = PrincipalId("user-1"),
        origin = RequestOrigin.TEXT,
        intent = "test intent",
        targetResources = targetResources,
        proposedActions = proposedActions,
        priority = RequestPriority.NORMAL,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = "corr-1",
    )

    /** A vocabulary with a single "do something" -> (READ, CALENDAR) entry, and no others. */
    private suspend fun readCalendarVocabulary(): InMemoryActionVocabulary {
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = "do something",
                mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.CALENDAR)),
            ),
        )
        return vocabulary
    }

    private suspend fun registryWithCalendarResource(): InMemoryResourceRegistry {
        val registry = InMemoryResourceRegistry()
        registry.register(calendarResource())
        return registry
    }

    // --- 1. A matching allow rule is applied ---

    @Test
    fun `a matching allow rule produces its configured APPROVED outcome`() = runTest {
        val rule = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        val policy = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), registryWithCalendarResource(), listOf(rule))

        val decision = policy.evaluate(request())

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        assertEquals(PermissionLevel.AUTOMATIC, decision.level)
        assertEquals(PermissionAction.READ, decision.action)
    }

    // --- 2. A matching deny rule is applied ---

    @Test
    fun `a matching deny rule produces its configured DENIED outcome`() = runTest {
        val rule = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.ADMINISTRATIVE)
        val policy = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), registryWithCalendarResource(), listOf(rule))

        val decision = policy.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
        assertEquals(PermissionLevel.ADMINISTRATIVE, decision.level)
    }

    // --- 3. Unknown action (not present in the vocabulary at all) is DENIED ---

    @Test
    fun `an unknown action not present in the vocabulary produces DENIED`() = runTest {
        val approveEverything = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        val policy = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), registryWithCalendarResource(), listOf(approveEverything))

        val decision = policy.evaluate(request(proposedActions = listOf("an action nobody registered")))

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    // --- 4. Unknown resource (target Resource not registered) produces DENIED ---

    @Test
    fun `an unresolvable target Resource produces DENIED`() = runTest {
        val approveEverything = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        // No Resource registered at all for "res-unregistered".
        val policy = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), InMemoryResourceRegistry(), listOf(approveEverything))

        val decision = policy.evaluate(request(targetResources = listOf(ResourceId("res-unregistered"))))

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    // --- 5. Unknown permission (action/resource resolve, but no rule addresses that pair) produces DENIED ---

    @Test
    fun `a resolved action-resource pair with no addressing rule produces DENIED (Unknown Permission)`() = runTest {
        // A rule exists, but for a different (action, resourceType) pair -- not the one this request resolves to.
        val unrelatedRule = PermissionPolicyRule(PermissionAction.WRITE, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        val policy = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), registryWithCalendarResource(), listOf(unrelatedRule))

        val decision = policy.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    // --- 6. No matching rule at all (empty rule list) produces DENIED ---

    @Test
    fun `an empty rule list produces DENIED for every request`() = runTest {
        val policy = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), registryWithCalendarResource(), rules = emptyList())

        val decision = policy.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    // --- 7. A rule configured for APPROVED_WITH_CONFIRMATION is honoured ---

    @Test
    fun `a matching rule configured for APPROVED_WITH_CONFIRMATION is honoured`() = runTest {
        val rule = PermissionPolicyRule(
            PermissionAction.READ,
            ResourceType.CALENDAR,
            PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION,
            PermissionLevel.CONFIRMATION_REQUIRED,
        )
        val policy = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), registryWithCalendarResource(), listOf(rule))

        val decision = policy.evaluate(request())

        assertEquals(PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION, decision.decision)
        assertEquals(PermissionLevel.CONFIRMATION_REQUIRED, decision.level)
    }

    // --- 8. Repeated evaluation of the same request is deterministic ---

    @Test
    fun `evaluating the same request twice produces the same decisionId, action, decision, and level`() = runTest {
        val rule = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        val policy = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), registryWithCalendarResource(), listOf(rule))
        val req = request()

        val first = policy.evaluate(req)
        val second = policy.evaluate(req)

        assertEquals(first.decisionId, second.decisionId)
        assertEquals(first.action, second.action)
        assertEquals(first.decision, second.decision)
        assertEquals(first.level, second.level)
    }

    // --- 9. Policy evaluation has no side effects ---

    @Test
    fun `evaluate never registers or modifies Resources -- it only reads`() = runTest {
        val calls = mutableListOf<String>()
        val inner = registryWithCalendarResource()
        val recording = RecordingResourceRegistry(inner, calls)
        val rule = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        val policy = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), recording, listOf(rule))

        policy.evaluate(request())

        assertTrue(calls.contains("resolve"), "evaluate should read via resolve()")
        assertFalse(calls.contains("register"), "evaluate must never register a Resource")
        assertFalse(calls.contains("update"), "evaluate must never update a Resource")
        assertFalse(calls.contains("listByOwner"), "evaluate must never enumerate Resources by owner")
    }
}
