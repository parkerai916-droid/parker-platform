package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.AuthorizationPurposeRegistrationOutcome
import parker.core.interfaces.AuthorizationPurposeRetirementOutcome
import parker.core.interfaces.AuthorizationPurposeStatus
import parker.core.interfaces.ExecutionRequest
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
import kotlin.test.assertTrue

/**
 * Authorization Purpose Implementation Plan, Unit 6 ("End-to-End
 * Verification"). Proves the one path no prior Unit's own tests combined:
 * [AuthorizationPurposeId] -> [AuthorizationPurposeRegistry] ->
 * [ExecutionRequest.authorizationPurpose] -> [DefaultPermissionPolicy] ->
 * [DefaultPermissionEngine] -> `PermissionDecision`. Every prior Unit's own
 * test either called [DefaultPermissionPolicy.evaluate] directly, bypassing
 * [DefaultPermissionEngine] entirely (`DefaultPermissionPolicyTest.kt`,
 * `AuthorizationPurposeRegistryTest.kt`), or exercised
 * [DefaultPermissionEngine] with no Authorization-Purpose-bearing request at
 * all (the pre-existing `DefaultPermissionEngineTest.kt`). This file closes
 * that gap using a single, clearly-synthetic, test-tier-only Authorization
 * Purpose value per test (`test.`-prefixed, never registered in
 * `ParkerRuntime.kt`'s own production set).
 *
 * Constructs the **real** [DefaultPermissionEngine]/[DefaultPermissionPolicy]
 * class pair directly with test-supplied dependencies -- the identical
 * classes `ParkerRuntime.kt` composes, exercised with test data, mirroring
 * `DefaultPermissionEngineTest.kt`'s own already-established,
 * already-independently-reviewed construction pattern
 * ([registerAt]/[newPrincipal], reproduced here) -- never a second,
 * competing authorization mechanism. A test-supplied rule list is required
 * specifically because precedence testing (registered-and-eligible values
 * governing over a coarser rule) needs a purpose-aware
 * [PermissionPolicyRule] to exist, which the real, composed runtime's own
 * fixed, production rule list cannot supply without an unauthorised
 * production change (Unit 6's own "no production Kotlin unless a genuine
 * Units 1-5 defect is found" rule). Composed-runtime-specific properties
 * (single-instance structure, regression against real production actions,
 * the Gap #54 non-widening proof) are covered separately, in
 * `ParkerRuntimeAuthorizationPurposeCompositionTest.kt`.
 */
class AuthorizationPurposeEndToEndVerificationTest {

    private fun newPrincipal(id: String) = Principal(
        principalId = PrincipalId(id),
        principalType = PrincipalType.USER,
        displayName = "Test Principal",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private suspend fun registerAt(identityService: InMemoryIdentityService, id: String, status: PrincipalStatus): PrincipalId {
        val principalId = identityService.register(newPrincipal(id))
        val chain = listOf(PrincipalStatus.CREATED, PrincipalStatus.ACTIVE, PrincipalStatus.SUSPENDED, PrincipalStatus.REVOKED, PrincipalStatus.ARCHIVED)
        val targetIndex = chain.indexOf(status)
        for (i in 1..targetIndex) identityService.updateStatus(principalId, chain[i])
        return principalId
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

    private fun request(authorizationPurpose: AuthorizationPurposeId? = null, principalId: String = "user-1") = ExecutionRequest(
        requestId = RequestId("req-1"),
        principalId = PrincipalId(principalId),
        origin = RequestOrigin.TEXT,
        intent = "test intent",
        targetResources = listOf(ResourceId("res-1")),
        proposedActions = listOf("do something"),
        priority = RequestPriority.NORMAL,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = "corr-1",
        authorizationPurpose = authorizationPurpose,
    )

    /** Real [DefaultPermissionEngine], wired against a real, Active-Principal [InMemoryIdentityService]. */
    private suspend fun engine(
        rules: List<PermissionPolicyRule>,
        authorizationPurposeRegistry: AuthorizationPurposeRegistry? = null,
        resourceRegistry: ResourceRegistry? = null,
    ): DefaultPermissionEngine {
        val identityService = InMemoryIdentityService()
        registerAt(identityService, "user-1", PrincipalStatus.ACTIVE)
        val effectiveResourceRegistry = resourceRegistry ?: InMemoryResourceRegistry().also { it.register(calendarResource()) }
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = "do something",
                mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.CALENDAR)),
            ),
        )
        val policy = DefaultPermissionPolicy(ActionMapper(vocabulary), effectiveResourceRegistry, rules, authorizationPurposeRegistry)
        return DefaultPermissionEngine(identityService, policy)
    }

    private val coarseApprove = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC)
    private val coarseDeny = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC)

    // === 1. Registered and active purpose: both ALLOW and DENY, proving the rule genuinely controls ===

    @Test
    fun `a purpose-aware ALLOW rule governs, through the full engine, when the declared purpose is registered and active`() = runTest {
        val purpose = AuthorizationPurposeId("test.unit-6-registered-active-allow")
        val purposeAware = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC, purpose)
        val registry = InMemoryAuthorizationPurposeRegistry()
        registry.register(purpose)
        val e = engine(rules = listOf(coarseDeny, purposeAware), authorizationPurposeRegistry = registry)

        val decision = e.evaluate(request(authorizationPurpose = purpose))

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
    }

    @Test
    fun `a purpose-aware DENY rule governs, through the full engine, when the declared purpose is registered and active`() = runTest {
        val purpose = AuthorizationPurposeId("test.unit-6-registered-active-deny")
        val purposeAware = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.ADMINISTRATIVE, purpose)
        val registry = InMemoryAuthorizationPurposeRegistry()
        registry.register(purpose)
        val e = engine(rules = listOf(coarseApprove, purposeAware), authorizationPurposeRegistry = registry)

        val decision = e.evaluate(request(authorizationPurpose = purpose))

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
        assertEquals(PermissionLevel.ADMINISTRATIVE, decision.level)
    }

    // === 2. Purpose-aware precedence, both directions, through the full engine, order-independent ===

    @Test
    fun `precedence -- coarse ALLOW plus specific DENY yields DENY through the full engine`() = runTest {
        val purpose = AuthorizationPurposeId("test.unit-6-precedence-direction-a")
        val purposeAware = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC, purpose)
        val registry = InMemoryAuthorizationPurposeRegistry()
        registry.register(purpose)
        val e = engine(rules = listOf(coarseApprove, purposeAware), authorizationPurposeRegistry = registry)

        val decision = e.evaluate(request(authorizationPurpose = purpose))

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    @Test
    fun `precedence -- coarse DENY plus specific ALLOW yields ALLOW through the full engine`() = runTest {
        val purpose = AuthorizationPurposeId("test.unit-6-precedence-direction-b")
        val purposeAware = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC, purpose)
        val registry = InMemoryAuthorizationPurposeRegistry()
        registry.register(purpose)
        val e = engine(rules = listOf(coarseDeny, purposeAware), authorizationPurposeRegistry = registry)

        val decision = e.evaluate(request(authorizationPurpose = purpose))

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
    }

    @Test
    fun `precedence is independent of rule-list order, through the full engine`() = runTest {
        val purpose = AuthorizationPurposeId("test.unit-6-precedence-order-independence")
        val purposeAware = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.ADMINISTRATIVE, purpose)
        val registry = InMemoryAuthorizationPurposeRegistry()
        registry.register(purpose)

        val coarseFirst = engine(rules = listOf(coarseApprove, purposeAware), authorizationPurposeRegistry = registry)
        val purposeFirst = engine(rules = listOf(purposeAware, coarseApprove), authorizationPurposeRegistry = registry)

        assertEquals(PermissionDecisionOutcome.DENIED, coarseFirst.evaluate(request(authorizationPurpose = purpose)).decision)
        assertEquals(PermissionDecisionOutcome.DENIED, purposeFirst.evaluate(request(authorizationPurpose = purpose)).decision)
    }

    // === 3. Absent purpose retains pre-existing behaviour, even in the presence of a purpose-aware rule ===

    @Test
    fun `an absent authorizationPurpose retains coarse-rule behaviour through the full engine, even when a purpose-aware rule exists for the same pair`() = runTest {
        val purpose = AuthorizationPurposeId("test.unit-6-absent-purpose-regression")
        val purposeAware = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC, purpose)
        val registry = InMemoryAuthorizationPurposeRegistry()
        registry.register(purpose)
        val e = engine(rules = listOf(coarseApprove, purposeAware), authorizationPurposeRegistry = registry)

        val decision = e.evaluate(request(authorizationPurpose = null))

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
    }

    // === 4. Unregistered purpose cannot gain purpose-specific authority ===

    @Test
    fun `an unregistered authorizationPurpose cannot gain purpose-specific authority through the full engine`() = runTest {
        val purpose = AuthorizationPurposeId("test.unit-6-never-registered")
        val purposeAware = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.ADMINISTRATIVE, purpose)
        val registry = InMemoryAuthorizationPurposeRegistry() // purpose never registered
        val e = engine(rules = listOf(coarseApprove, purposeAware), authorizationPurposeRegistry = registry)

        val decision = e.evaluate(request(authorizationPurpose = purpose))

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision, "an unregistered purpose must fall back to the coarse rule, never the purpose-aware one")
    }

    // === 5. Retired purpose cannot participate as active authority; historical presence remains intact ===

    @Test
    fun `a retired authorizationPurpose cannot participate as active authority through the full engine, though it remains lookup-visible`() = runTest {
        val purpose = AuthorizationPurposeId("test.unit-6-retired-purpose")
        val purposeAware = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.ADMINISTRATIVE, purpose)
        val registry = InMemoryAuthorizationPurposeRegistry()
        registry.register(purpose)
        registry.retire(purpose)
        val e = engine(rules = listOf(coarseApprove, purposeAware), authorizationPurposeRegistry = registry)

        val decision = e.evaluate(request(authorizationPurpose = purpose))

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision, "a retired purpose must fall back to the coarse rule, never the purpose-aware one")
        val entry = registry.lookup(purpose)
        assertEquals(AuthorizationPurposeStatus.RETIRED, entry?.status, "retirement must not delete -- historical vocabulary presence must remain intact")
    }

    // === 6. Registry properties relevant to permission evaluation, on the instance actually wired into a real engine ===

    @Test
    fun `registry conflict behaviour on the instance wired into a real engine -- conflicting registration rejected, no silent reactivation`() = runTest {
        val purpose = AuthorizationPurposeId("test.unit-6-registry-conflict-behaviour")
        val registry = InMemoryAuthorizationPurposeRegistry()
        val firstRegistration = registry.register(purpose)
        assertTrue(firstRegistration is AuthorizationPurposeRegistrationOutcome.Registered)

        registry.retire(purpose)
        val reRegistrationAfterRetirement = registry.register(purpose)
        assertTrue(reRegistrationAfterRetirement is AuthorizationPurposeRegistrationOutcome.Rejected, "a retired value must never be silently reactivated by re-registration")
        assertEquals(AuthorizationPurposeStatus.RETIRED, registry.lookup(purpose)?.status, "the rejected re-registration attempt must not have flipped status back to ACTIVE")

        val secondRetirement = registry.retire(purpose)
        assertTrue(secondRetirement is AuthorizationPurposeRetirementOutcome.AlreadyRetired)

        // The same instance, wired into a real DefaultPermissionEngine, must reflect this history:
        // a retired-then-rejected-re-registration id still cannot govern a purpose-aware rule.
        val purposeAware = PermissionPolicyRule(PermissionAction.READ, ResourceType.CALENDAR, PermissionDecisionOutcome.DENIED, PermissionLevel.ADMINISTRATIVE, purpose)
        val e = engine(rules = listOf(coarseApprove, purposeAware), authorizationPurposeRegistry = registry)
        assertEquals(PermissionDecisionOutcome.APPROVED, e.evaluate(request(authorizationPurpose = purpose)).decision)
    }
}
