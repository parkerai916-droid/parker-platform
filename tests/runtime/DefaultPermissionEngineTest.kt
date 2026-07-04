package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.IdentityService
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Proves Sprint 2, Unit A1 (`docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md`):
 * [DefaultPermissionEngine] resolves identity first and enforces
 * Principal status before any caller-supplied decision logic runs,
 * closing `IMPLEMENTATION_GAPS.md` #40. Uses [InMemoryIdentityService]
 * directly -- the smallest existing identity infrastructure -- rather
 * than inventing a new test double, mirroring
 * `tests/runtime/InMemoryIdentityServiceTest.kt`'s own
 * principal-construction pattern. Does not modify, extend, or duplicate
 * `tests/runtime/FakePermissionEngine.kt`, which remains
 * `DefaultExecutionPipelineTest`'s own fixture.
 */
class DefaultPermissionEngineTest {

    /** Records call order across the identity service and the supplied decision function. */
    private class RecordingIdentityService(
        private val delegate: IdentityService,
        private val calls: MutableList<String>,
    ) : IdentityService by delegate {
        override suspend fun resolve(principalId: PrincipalId): Principal? {
            calls.add("identity.resolve")
            return delegate.resolve(principalId)
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

    private fun suppliedDecision(request: ExecutionRequest, outcome: PermissionDecisionOutcome) = PermissionDecision(
        decisionId = DecisionId("dec-supplied-1"),
        principalId = request.principalId,
        resourceId = request.targetResources.first(),
        action = PermissionAction.READ,
        decision = outcome,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
    )

    // --- 1. Identity resolution happens before the supplied decision function ---

    @Test
    fun `evaluate resolves principalId via IdentityService before the supplied decision function runs`() = runTest {
        val calls = mutableListOf<String>()
        val inner = InMemoryIdentityService()
        registerAt(inner, "user-1", PrincipalStatus.ACTIVE)
        val recording = RecordingIdentityService(inner, calls)

        val engine = DefaultPermissionEngine(recording) { req ->
            calls.add("decisionFor")
            suppliedDecision(req, PermissionDecisionOutcome.APPROVED)
        }

        engine.evaluate(request())

        assertEquals(listOf("identity.resolve", "decisionFor"), calls)
    }

    // --- 2/3/4. Suspended / Revoked / Archived are always DENIED ---

    @Test
    fun `Suspended Principal always produces DENIED even if the supplied decision function would approve`() = runTest {
        val identityService = InMemoryIdentityService()
        registerAt(identityService, "user-1", PrincipalStatus.SUSPENDED)
        val engine = DefaultPermissionEngine(identityService) { req -> suppliedDecision(req, PermissionDecisionOutcome.APPROVED) }

        val decision = engine.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    @Test
    fun `Revoked Principal always produces DENIED even if the supplied decision function would approve`() = runTest {
        val identityService = InMemoryIdentityService()
        registerAt(identityService, "user-1", PrincipalStatus.REVOKED)
        val engine = DefaultPermissionEngine(identityService) { req -> suppliedDecision(req, PermissionDecisionOutcome.APPROVED) }

        val decision = engine.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    @Test
    fun `Archived Principal always produces DENIED even if the supplied decision function would approve`() = runTest {
        val identityService = InMemoryIdentityService()
        registerAt(identityService, "user-1", PrincipalStatus.ARCHIVED)
        val engine = DefaultPermissionEngine(identityService) { req -> suppliedDecision(req, PermissionDecisionOutcome.APPROVED) }

        val decision = engine.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    // --- 5. Unresolvable principalId produces DENIED ---

    @Test
    fun `unresolvable principalId produces DENIED`() = runTest {
        val identityService = InMemoryIdentityService()
        // No principal registered at all for "user-1".
        val engine = DefaultPermissionEngine(identityService) { req -> suppliedDecision(req, PermissionDecisionOutcome.APPROVED) }

        val decision = engine.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
    }

    // --- 6/7. Active Principal reaches the supplied decision function, unchanged ---

    @Test
    fun `Active Principal reaches the supplied decision function`() = runTest {
        val identityService = InMemoryIdentityService()
        registerAt(identityService, "user-1", PrincipalStatus.ACTIVE)
        var decisionFunctionCalled = false
        val engine = DefaultPermissionEngine(identityService) { req ->
            decisionFunctionCalled = true
            suppliedDecision(req, PermissionDecisionOutcome.APPROVED)
        }

        engine.evaluate(request())

        assertTrue(decisionFunctionCalled)
    }

    @Test
    fun `Active Principal returns the supplied decision unchanged`() = runTest {
        val identityService = InMemoryIdentityService()
        registerAt(identityService, "user-1", PrincipalStatus.ACTIVE)
        var suppliedInstance: PermissionDecision? = null
        val engine = DefaultPermissionEngine(identityService) { req ->
            suppliedDecision(req, PermissionDecisionOutcome.DEFERRED).also { suppliedInstance = it }
        }

        val result = engine.evaluate(request())

        // Reference equality, not just structural equality: evaluate() must return exactly
        // the object the supplied decision function produced, not a copy or a reconstruction.
        assertSame(suppliedInstance, result)
        assertEquals(PermissionDecisionOutcome.DEFERRED, result.decision)
    }

    // --- 8. Denied non-active/unresolvable cases never call the supplied decision function ---

    @Test
    fun `the supplied decision function is not called for Suspended, Revoked, Archived, or unresolvable Principals`() = runTest {
        val statuses = listOf(PrincipalStatus.SUSPENDED, PrincipalStatus.REVOKED, PrincipalStatus.ARCHIVED)
        for (status in statuses) {
            val identityService = InMemoryIdentityService()
            registerAt(identityService, "user-1", status)
            var decisionFunctionCalled = false
            val engine = DefaultPermissionEngine(identityService) { req ->
                decisionFunctionCalled = true
                suppliedDecision(req, PermissionDecisionOutcome.APPROVED)
            }

            engine.evaluate(request())

            assertFalse(decisionFunctionCalled, "decision function should not be called for status $status")
        }

        // Unresolvable principalId case.
        val identityServiceNoPrincipal = InMemoryIdentityService()
        var unresolvedDecisionCalled = false
        val engineForUnresolved = DefaultPermissionEngine(identityServiceNoPrincipal) { req ->
            unresolvedDecisionCalled = true
            suppliedDecision(req, PermissionDecisionOutcome.APPROVED)
        }
        engineForUnresolved.evaluate(request())
        assertFalse(unresolvedDecisionCalled, "decision function should not be called for an unresolvable principalId")
    }

    // --- 9. explain(decisionId) preserves the PermissionEngine contract ---

    @Test
    fun `explain returns a PermissionExplanation compatible with the existing PermissionEngine contract`() = runTest {
        val identityService = InMemoryIdentityService()
        val engine = DefaultPermissionEngine(identityService) { req -> suppliedDecision(req, PermissionDecisionOutcome.APPROVED) }

        val explanation = engine.explain(DecisionId("dec-1"))

        assertEquals(DecisionId("dec-1"), explanation.decisionId)
        assertTrue(explanation.reason.isNotBlank())
    }

    // --- Interpretive decision, recorded here per this class's own KDoc: Created is DENIED ---

    @Test
    fun `a Created (not yet Active) Principal is DENIED, not delegated to the supplied decision function`() = runTest {
        val identityService = InMemoryIdentityService()
        identityService.register(newPrincipal("user-1")) // stays at CREATED, never activated
        var decisionFunctionCalled = false
        val engine = DefaultPermissionEngine(identityService) { req ->
            decisionFunctionCalled = true
            suppliedDecision(req, PermissionDecisionOutcome.APPROVED)
        }

        val decision = engine.evaluate(request())

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
        assertFalse(decisionFunctionCalled)
    }
}
