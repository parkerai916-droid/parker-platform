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
        authorizationPurpose: AuthorizationPurposeId? = null,
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
        authorizationPurpose = authorizationPurpose,
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

    // === Authorization Purpose (Authorization Purpose Implementation Plan, Unit 4) ===

    private suspend fun registryWithActive(id: AuthorizationPurposeId): InMemoryAuthorizationPurposeRegistry {
        val registry = InMemoryAuthorizationPurposeRegistry()
        registry.register(id)
        return registry
    }

    private suspend fun registryWithRetired(id: AuthorizationPurposeId): InMemoryAuthorizationPurposeRegistry {
        val registry = InMemoryAuthorizationPurposeRegistry()
        registry.register(id)
        registry.retire(id)
        return registry
    }

    // --- 10. Regression: a request that never populates authorizationPurpose is unaffected
    //         by the presence of a purpose-aware rule sharing its (action, resourceType) pair ---

    @Test
    fun `a request with no authorizationPurpose matches only the coarse rule, even when a purpose-aware rule exists for the same pair`() = runTest {
        val purpose = AuthorizationPurposeId("household.routine-maintenance")
        val coarse = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        val purposeAware = PermissionPolicyRule(
            PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC,
            authorizationPurpose = purpose,
        )
        val policy = DefaultPermissionPolicy(
            ActionMapper(readCalendarVocabulary()),
            registryWithCalendarResource(),
            listOf(coarse, purposeAware),
            registryWithActive(purpose),
        )

        val decision = policy.evaluate(request())

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
    }

    // --- 11. Regression: omitting the AuthorizationPurposeRegistry constructor argument entirely
    //         (the shape ParkerRuntime.kt's own unmodified composition still uses) is safe even
    //         when a request declares a purpose and a purpose-aware rule exists ---

    @Test
    fun `omitting the AuthorizationPurposeRegistry constructor argument falls back to the coarse rule`() = runTest {
        val purpose = AuthorizationPurposeId("household.routine-maintenance")
        val coarse = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        val purposeAware = PermissionPolicyRule(
            PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC,
            authorizationPurpose = purpose,
        )
        // 3-arg construction -- no AuthorizationPurposeRegistry supplied at all.
        val policy = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), registryWithCalendarResource(), listOf(coarse, purposeAware))

        val decision = policy.evaluate(request(authorizationPurpose = purpose))

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
    }

    // --- 12. A purpose-aware rule takes precedence over a coarser rule when the request's
    //         declared purpose is registered and active, even though it is MORE restrictive
    //         than the coarse rule -- proving genuine precedence, not "most restrictive wins" ---

    @Test
    fun `a more restrictive purpose-aware rule overrides a more permissive coarse rule for a matching, active purpose`() = runTest {
        val purpose = AuthorizationPurposeId("household.routine-maintenance")
        val coarse = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        val purposeAware = PermissionPolicyRule(
            PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.ADMINISTRATIVE,
            authorizationPurpose = purpose,
        )
        val policy = DefaultPermissionPolicy(
            ActionMapper(readCalendarVocabulary()),
            registryWithCalendarResource(),
            listOf(coarse, purposeAware),
            registryWithActive(purpose),
        )

        val decision = policy.evaluate(request(authorizationPurpose = purpose))

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
        assertEquals(PermissionLevel.ADMINISTRATIVE, decision.level)
    }

    // --- 13. Same as above, in the opposite restrictiveness direction: a LESS restrictive
    //         purpose-aware rule also governs over a MORE restrictive coarse rule -- confirming
    //         this is dimension-based precedence, not restrictiveness-based selection ---

    @Test
    fun `a less restrictive purpose-aware rule also overrides a more restrictive coarse rule for a matching, active purpose`() = runTest {
        val purpose = AuthorizationPurposeId("household.routine-maintenance")
        val coarse = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC)
        val purposeAware = PermissionPolicyRule(
            PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC,
            authorizationPurpose = purpose,
        )
        val policy = DefaultPermissionPolicy(
            ActionMapper(readCalendarVocabulary()),
            registryWithCalendarResource(),
            listOf(coarse, purposeAware),
            registryWithActive(purpose),
        )

        val decision = policy.evaluate(request(authorizationPurpose = purpose))

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
    }

    // --- 14. Fail-closed: an unregistered purpose value cannot satisfy a purpose-aware rule,
    //         and falls back to the coarse rule exactly as an absent purpose would ---

    @Test
    fun `an unregistered authorizationPurpose falls back to the coarse rule, never the purpose-aware one`() = runTest {
        val purpose = AuthorizationPurposeId("household.never-registered")
        val coarse = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        val purposeAware = PermissionPolicyRule(
            PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC,
            authorizationPurpose = purpose,
        )
        val policy = DefaultPermissionPolicy(
            ActionMapper(readCalendarVocabulary()),
            registryWithCalendarResource(),
            listOf(coarse, purposeAware),
            InMemoryAuthorizationPurposeRegistry(), // purpose never registered
        )

        val decision = policy.evaluate(request(authorizationPurpose = purpose))

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
    }

    // --- 15. Fail-closed: a retired purpose value is treated identically to an unregistered one ---

    @Test
    fun `a retired authorizationPurpose falls back to the coarse rule, never the purpose-aware one`() = runTest {
        val purpose = AuthorizationPurposeId("household.retired-purpose")
        val coarse = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        val purposeAware = PermissionPolicyRule(
            PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC,
            authorizationPurpose = purpose,
        )
        val policy = DefaultPermissionPolicy(
            ActionMapper(readCalendarVocabulary()),
            registryWithCalendarResource(),
            listOf(coarse, purposeAware),
            registryWithRetired(purpose),
        )

        val decision = policy.evaluate(request(authorizationPurpose = purpose))

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
    }

    // --- 16. Fail-closed, no coarse fallback available: an absent/unregistered purpose with
    //         only a purpose-aware rule in the table produces DENIED via the pre-existing
    //         "no rule matches" default -- not a new, separate deny mechanism ---

    @Test
    fun `with no coarse rule available, an unmatched authorizationPurpose produces DENIED via the existing no-rule-matches default`() = runTest {
        val purpose = AuthorizationPurposeId("household.routine-maintenance")
        val purposeAware = PermissionPolicyRule(
            PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC,
            authorizationPurpose = purpose,
        )
        val policy = DefaultPermissionPolicy(
            ActionMapper(readCalendarVocabulary()),
            registryWithCalendarResource(),
            listOf(purposeAware),
            registryWithActive(purpose),
        )

        // No authorizationPurpose declared -- the only rule in the table requires one.
        val decision = policy.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    // --- 17. Precedence safety, stated directly: a coarse rule must never silently resolve
    //         a request whose declared, active purpose matches a more specific rule -- tested
    //         against BOTH possible resolution orders in the rule list, to rule out an
    //         implementation that merely happens to prefer list order ---

    @Test
    fun `a coarse rule never resolves a request whose active purpose matches a more specific rule, regardless of rule list order`() = runTest {
        val purpose = AuthorizationPurposeId("household.routine-maintenance")
        val coarse = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
        val purposeAware = PermissionPolicyRule(
            PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.ADMINISTRATIVE,
            authorizationPurpose = purpose,
        )
        val registry = registryWithActive(purpose)

        val coarseFirst = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), registryWithCalendarResource(), listOf(coarse, purposeAware), registry)
        val purposeFirst = DefaultPermissionPolicy(ActionMapper(readCalendarVocabulary()), registryWithCalendarResource(), listOf(purposeAware, coarse), registry)

        val req = request(authorizationPurpose = purpose)
        assertEquals(PermissionDecisionOutcome.DENIED, coarseFirst.evaluate(req).decision)
        assertEquals(PermissionDecisionOutcome.DENIED, purposeFirst.evaluate(req).decision)
    }
}
