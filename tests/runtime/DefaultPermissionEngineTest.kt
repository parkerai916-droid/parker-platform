package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.IdentityService
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
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
 * Proves Sprint 2, Unit A1 (`docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md`):
 * [DefaultPermissionEngine] resolves identity first and enforces
 * Principal status before [DefaultPermissionPolicy] is ever consulted,
 * closing `IMPLEMENTATION_GAPS.md` #40. Uses [InMemoryIdentityService]
 * directly -- the smallest existing identity infrastructure -- rather
 * than inventing a new test double, mirroring
 * `tests/runtime/InMemoryIdentityServiceTest.kt`'s own
 * principal-construction pattern. Does not modify, extend, or duplicate
 * `tests/runtime/FakePermissionEngine.kt`, which remains
 * `DefaultExecutionPipelineTest`'s own fixture.
 *
 * **Sprint 2, Unit A2 update:** the Unit A1 placeholder
 * `decisionFor` lambda no longer exists -- [DefaultPermissionEngine] now
 * takes a real [DefaultPermissionPolicy]. Tests that previously proved
 * "the supplied decision function runs/does not run" now prove the
 * equivalent against the real policy: whether [DefaultPermissionPolicy]
 * is consulted at all (observed via [RecordingResourceRegistry], since
 * [DefaultPermissionPolicy.evaluate] always resolves target Resources as
 * its first step) and, for an Active Principal, that the policy's own
 * matching rule -- not a hardcoded value -- determines the returned
 * decision's outcome/level/action. `DefaultPermissionPolicy`'s own
 * dedicated test coverage lives in `DefaultPermissionPolicyTest.kt`; this
 * file's job is only the identity-gate boundary in front of it.
 */
class DefaultPermissionEngineTest {

    /** Records call order across the identity service and the policy layer. */
    private class RecordingIdentityService(
        private val delegate: IdentityService,
        private val calls: MutableList<String>,
    ) : IdentityService by delegate {
        override suspend fun resolve(principalId: PrincipalId): Principal? {
            calls.add("identity.resolve")
            return delegate.resolve(principalId)
        }
    }

    /**
     * Records call order/occurrence for [DefaultPermissionPolicy]'s own
     * first read (`resourceRegistry.resolve`), used here only as an
     * observable proxy for "was the policy consulted at all" -- this test
     * file does not otherwise reach into [DefaultPermissionPolicy]'s
     * internals.
     */
    private class RecordingResourceRegistry(
        private val delegate: ResourceRegistry,
        private val calls: MutableList<String>,
    ) : ResourceRegistry by delegate {
        override suspend fun resolve(resourceId: ResourceId): Resource? {
            calls.add("policy.resourceResolve")
            return delegate.resolve(resourceId)
        }
    }

    private fun newPrincipal(id: String) = Principal(
        principalId = PrincipalId(id),
        principalType = PrincipalType.USER,
        displayName = "Test Principal",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    /**
     * Registers [id] and advances it to [status], one legal edge at a time,
     * per `PrincipalLifecycleTransitions`'s strict linear chain -- there is
     * no direct jump from `Created` to e.g. `Revoked`.
     */
    private suspend fun registerAt(
        identityService: InMemoryIdentityService,
        id: String,
        status: PrincipalStatus,
    ): PrincipalId {
        val principalId = identityService.register(newPrincipal(id))
        val chain = listOf(
            PrincipalStatus.CREATED,
            PrincipalStatus.ACTIVE,
            PrincipalStatus.SUSPENDED,
            PrincipalStatus.REVOKED,
            PrincipalStatus.ARCHIVED,
        )
        val targetIndex = chain.indexOf(status)
        for (i in 1..targetIndex) {
            identityService.updateStatus(principalId, chain[i])
        }
        return principalId
    }

    private fun request(principalId: String = "user-1") = ExecutionRequest(
        requestId = RequestId("req-1"),
        principalId = PrincipalId(principalId),
        origin = RequestOrigin.TEXT,
        intent = "test intent",
        targetResources = listOf(ResourceId("res-1")),
        proposedActions = listOf("do something"),
        priority = RequestPriority.NORMAL,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = "corr-1",
    )

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

    /**
     * Builds a [DefaultPermissionPolicy] wired against a fresh, minimal
     * [ActionMapper]/[ResourceRegistry] pair, registering "do something"
     * (this file's [request]'s own `proposedActions` entry) against
     * [ResourceType.CALENDAR] and [calendarResource] under `res-1` (this
     * file's own [request]'s own `targetResources` entry), so that
     * `DefaultPermissionPolicy.evaluate` resolves a real (action,
     * resourceType) pair for every test in this file that reaches it.
     * [rules] supplies whatever policy outcome each test needs; an empty
     * list is a legitimate "no rule matches" policy, used by the
     * Suspended/Revoked/Archived/unresolvable tests below specifically
     * because those tests supply an *approving* rule instead, to prove the
     * identity gate -- not an empty policy -- is what produces DENIED.
     */
    private suspend fun buildPolicy(
        resourceRegistry: ResourceRegistry? = null,
        rules: List<PermissionPolicyRule> = emptyList(),
    ): DefaultPermissionPolicy {
        // The default ResourceRegistry is built here, as an ordinary statement in the
        // function body, rather than as a parameter-default expression: a suspend call
        // (register) inside a parameter-default value is not supported by the Kotlin
        // coroutines compiler ("Unsupported suspend function calls in a coroutine
        // context"). This is a test-harness-only concern; it does not reflect anything
        // about DefaultPermissionPolicy's own behaviour.
        val effectiveResourceRegistry = resourceRegistry ?: InMemoryResourceRegistry().also {
            it.register(calendarResource())
        }
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = "do something",
                mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.CALENDAR)),
            ),
        )
        return DefaultPermissionPolicy(ActionMapper(vocabulary), effectiveResourceRegistry, rules)
    }

    private val approveReadCalendarRule = PermissionPolicyRule(
        action = PermissionAction.READ,
        resourceType = ResourceType.CALENDAR,
        outcome = PermissionDecisionOutcome.APPROVED,
        level = PermissionLevel.AUTOMATIC,
    )

    // --- 1. Identity resolution happens before the policy is ever consulted ---

    @Test
    fun `evaluate resolves principalId via IdentityService before consulting the policy`() = runTest {
        val calls = mutableListOf<String>()
        val innerIdentity = InMemoryIdentityService()
        registerAt(innerIdentity, "user-1", PrincipalStatus.ACTIVE)
        val identityService = RecordingIdentityService(innerIdentity, calls)

        val innerResources = InMemoryResourceRegistry()
        innerResources.register(calendarResource())
        val resources = RecordingResourceRegistry(innerResources, calls)
        val policy = buildPolicy(resourceRegistry = resources, rules = listOf(approveReadCalendarRule))

        val engine = DefaultPermissionEngine(identityService, policy)
        engine.evaluate(request())

        assertEquals(listOf("identity.resolve", "policy.resourceResolve"), calls)
    }

    // --- 2/3/4. Suspended / Revoked / Archived are always DENIED ---

    @Test
    fun `Suspended Principal always produces DENIED even if the policy would approve`() = runTest {
        val identityService = InMemoryIdentityService()
        registerAt(identityService, "user-1", PrincipalStatus.SUSPENDED)
        val engine = DefaultPermissionEngine(identityService, buildPolicy(rules = listOf(approveReadCalendarRule)))

        val decision = engine.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    @Test
    fun `Revoked Principal always produces DENIED even if the policy would approve`() = runTest {
        val identityService = InMemoryIdentityService()
        registerAt(identityService, "user-1", PrincipalStatus.REVOKED)
        val engine = DefaultPermissionEngine(identityService, buildPolicy(rules = listOf(approveReadCalendarRule)))

        val decision = engine.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    @Test
    fun `Archived Principal always produces DENIED even if the policy would approve`() = runTest {
        val identityService = InMemoryIdentityService()
        registerAt(identityService, "user-1", PrincipalStatus.ARCHIVED)
        val engine = DefaultPermissionEngine(identityService, buildPolicy(rules = listOf(approveReadCalendarRule)))

        val decision = engine.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    // --- 5. Unresolvable principalId produces DENIED ---

    @Test
    fun `unresolvable principalId produces DENIED`() = runTest {
        val identityService = InMemoryIdentityService()
        // No principal registered at all for "user-1".
        val engine = DefaultPermissionEngine(identityService, buildPolicy(rules = listOf(approveReadCalendarRule)))

        val decision = engine.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    // --- 6/7. Active Principal reaches the policy, and its matching rule decides the outcome ---

    @Test
    fun `Active Principal reaches policy evaluation`() = runTest {
        val calls = mutableListOf<String>()
        val identityService = InMemoryIdentityService()
        registerAt(identityService, "user-1", PrincipalStatus.ACTIVE)
        val innerResources = InMemoryResourceRegistry()
        innerResources.register(calendarResource())
        val resources = RecordingResourceRegistry(innerResources, calls)
        val policy = buildPolicy(resourceRegistry = resources, rules = listOf(approveReadCalendarRule))
        val engine = DefaultPermissionEngine(identityService, policy)

        engine.evaluate(request())

        assertTrue(calls.contains("policy.resourceResolve"), "policy should be consulted for an Active Principal")
    }

    @Test
    fun `Active Principal's decision matches the policy's own rule, not a hardcoded value`() = runTest {
        val identityService = InMemoryIdentityService()
        registerAt(identityService, "user-1", PrincipalStatus.ACTIVE)
        val rule = PermissionPolicyRule(
            action = PermissionAction.READ,
            resourceType = ResourceType.CALENDAR,
            outcome = PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION,
            level = PermissionLevel.HIGH_ASSURANCE,
        )
        val engine = DefaultPermissionEngine(identityService, buildPolicy(rules = listOf(rule)))

        val decision = engine.evaluate(request())

        assertEquals(PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION, decision.decision)
        assertEquals(PermissionLevel.HIGH_ASSURANCE, decision.level)
        assertEquals(PermissionAction.READ, decision.action)
    }

    // --- 8. Denied non-active/unresolvable cases never consult the policy ---

    @Test
    fun `the policy is not consulted for Suspended, Revoked, Archived, or unresolvable Principals`() = runTest {
        val statuses = listOf(PrincipalStatus.SUSPENDED, PrincipalStatus.REVOKED, PrincipalStatus.ARCHIVED)
        for (status in statuses) {
            val calls = mutableListOf<String>()
            val identityService = InMemoryIdentityService()
            registerAt(identityService, "user-1", status)
            val innerResources = InMemoryResourceRegistry()
            innerResources.register(calendarResource())
            val resources = RecordingResourceRegistry(innerResources, calls)
            val policy = buildPolicy(resourceRegistry = resources, rules = listOf(approveReadCalendarRule))
            val engine = DefaultPermissionEngine(identityService, policy)

            engine.evaluate(request())

            assertFalse(calls.contains("policy.resourceResolve"), "policy should not be consulted for status $status")
        }

        // Unresolvable principalId case.
        val calls = mutableListOf<String>()
        val identityServiceNoPrincipal = InMemoryIdentityService()
        val innerResources = InMemoryResourceRegistry()
        innerResources.register(calendarResource())
        val resources = RecordingResourceRegistry(innerResources, calls)
        val policy = buildPolicy(resourceRegistry = resources, rules = listOf(approveReadCalendarRule))
        val engineForUnresolved = DefaultPermissionEngine(identityServiceNoPrincipal, policy)

        engineForUnresolved.evaluate(request())

        assertFalse(calls.contains("policy.resourceResolve"), "policy should not be consulted for an unresolvable principalId")
    }

    // --- 9. explain(decisionId) preserves the PermissionEngine contract ---

    @Test
    fun `explain returns a PermissionExplanation compatible with the existing PermissionEngine contract`() = runTest {
        val identityService = InMemoryIdentityService()
        val engine = DefaultPermissionEngine(identityService, buildPolicy())

        val explanation = engine.explain(DecisionId("dec-1"))

        assertEquals(DecisionId("dec-1"), explanation.decisionId)
        assertTrue(explanation.reason.isNotBlank())
    }

    // --- Interpretive decision, recorded here per this class's own KDoc: Created is DENIED ---

    @Test
    fun `a Created (not yet Active) Principal is DENIED, without the policy being consulted`() = runTest {
        val calls = mutableListOf<String>()
        val identityService = InMemoryIdentityService()
        identityService.register(newPrincipal("user-1")) // stays at CREATED, never activated
        val innerResources = InMemoryResourceRegistry()
        innerResources.register(calendarResource())
        val resources = RecordingResourceRegistry(innerResources, calls)
        val policy = buildPolicy(resourceRegistry = resources, rules = listOf(approveReadCalendarRule))
        val engine = DefaultPermissionEngine(identityService, policy)

        val decision = engine.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
        assertFalse(calls.contains("policy.resourceResolve"))
    }
}
