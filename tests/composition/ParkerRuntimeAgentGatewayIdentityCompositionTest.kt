package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.IdentityService
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.runtime.ActionMapper
import parker.core.runtime.DefaultPermissionEngine
import parker.core.runtime.DefaultPermissionPolicy
import parker.core.runtime.InMemoryActionVocabulary
import parker.core.runtime.InMemoryAuthorizationPurposeRegistry
import parker.core.runtime.InMemoryIdentityService
import parker.core.runtime.InMemoryResourceRegistry
import parker.core.runtime.PermissionPolicyRule
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * AG-1B -- External Agent Identity Foundation
 * (`docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 10,
 * Section 20). Proves the unit's own acceptance criteria against the real,
 * composed [ParkerRuntime]:
 *
 * A. The Hermes principal exists in the composed runtime.
 * B. Hermes is distinct from Owner and from [PrincipalType.INTERNAL_AGENT].
 * C. Hermes has the new external-agent [PrincipalType], correct owner
 *    lineage, and status CREATED.
 * D. The Agent Gateway [AuthorizationPurposeId] is registered.
 * E. Hermes has zero authority while CREATED -- including against a
 *    synthetic, permissive policy that would otherwise approve.
 * F/G. Existing Owner and INTERNAL_AGENT-shaped behaviour is unchanged.
 * H. No existing permission policy is broadened by this unit (no rule
 *    referencing the new purpose or Hermes's principal exists).
 *
 * Reflection is used only where no public seam exists to observe internal
 * composition state, mirroring [ParkerRuntimeAuthorizationPurposeCompositionTest]'s
 * own established `privateField` precedent.
 */
class ParkerRuntimeAgentGatewayIdentityCompositionTest {

    private fun config(
        localTextChannelModuleId: String = "channel.local-text-agent-gateway-identity-test",
        agentGatewayHermesActive: Boolean = false,
        agentGatewayHttpConfigured: Boolean = false,
    ) = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted by these tests
        modelName = "test-model",
        ownerPrincipalId = "user.owner-agent-gateway-identity-test",
        localTextChannelModuleId = localTextChannelModuleId,
        evidenceStorageRootPath = Files.createTempDirectory("agent-gateway-identity-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("agent-gateway-identity-evidence-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("agent-gateway-identity-derivative-generation").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("agent-gateway-identity-derivative-generation-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("agent-gateway-identity-saved-analysis").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("agent-gateway-identity-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("agent-gateway-identity-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("agent-gateway-identity-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("agent-gateway-identity-knowledge-items").resolve("items.log").toString(),
        agentGatewayHttpBindAddress = if (agentGatewayHttpConfigured) "127.0.0.1" else null,
        agentGatewayHttpPort = if (agentGatewayHttpConfigured) 0 else null,
        agentGatewayHttpToken = if (agentGatewayHttpConfigured) "test-hermes-bearer-token" else null,
        agentGatewayAnalysisHttpToken = if (agentGatewayHttpConfigured) "test-analysis-bearer-token" else null,
        agentGatewayAccessAuditLogPath = if (agentGatewayHttpConfigured) {
            Files.createTempDirectory("agent-gateway-identity-access-audit").resolve("audit.log").toString()
        } else null,
        agentGatewayHermesActive = agentGatewayHermesActive,
        agentGatewayAnalysisActive = agentGatewayHermesActive,
    )

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    private fun composedEngine(runtime: ParkerRuntime): PermissionEngine = runtime.privateField("permissionEngine")
    private fun composedPolicy(runtime: ParkerRuntime): DefaultPermissionPolicy = composedEngine(runtime).privateField("policy")
    private fun composedIdentityService(runtime: ParkerRuntime): IdentityService = composedEngine(runtime).privateField("identityService")

    private val hermesPrincipalId = PrincipalId("agent.hermes-ingestion-operator")
    private val analysisPrincipalId = PrincipalId("agent.hermes-analysis-operator")
    private val agentGatewayPurpose = AuthorizationPurposeId("agent-gateway.hermes-ingestion")

    @Test
    fun `Analysis Agent principal is distinct and independently activated`() = runTest {
        val runtime = ParkerRuntime(config(agentGatewayHermesActive = true), RecordingParkerLogger())
        runtime.start()

        val identityService = composedIdentityService(runtime)
        val analysis = identityService.resolve(analysisPrincipalId)

        assertTrue(analysis != null)
        assertEquals(PrincipalType.EXTERNAL_AGENT, analysis!!.principalType)
        assertEquals(PrincipalStatus.ACTIVE, analysis.status)
        assertNotEquals(hermesPrincipalId, analysis.principalId)
        assertNotEquals(analysisPrincipalId, PrincipalId("user.owner-agent-gateway-identity-test"))

        runtime.shutdown()
    }

    // ================= A/C. Hermes exists, correctly shaped, CREATED =================

    @Test
    fun `Hermes principal is provisioned in the composed runtime as an external agent, owned by the configured owner, at status CREATED`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val hermes = composedIdentityService(runtime).resolve(hermesPrincipalId)

        assertTrue(hermes != null, "Hermes principal must be registered in the composed runtime")
        assertEquals(PrincipalType.EXTERNAL_AGENT, hermes!!.principalType)
        assertEquals(PrincipalId("user.owner-agent-gateway-identity-test"), hermes.owner)
        assertEquals(PrincipalStatus.CREATED, hermes.status)

        runtime.shutdown()
    }

    // ================= B. Distinct from Owner and INTERNAL_AGENT =================

    @Test
    fun `Hermes is a distinct principal from the Owner`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val identityService = composedIdentityService(runtime)
        val hermes = identityService.resolve(hermesPrincipalId)!!
        val owner = identityService.resolve(PrincipalId("user.owner-agent-gateway-identity-test"))!!

        assertNotEquals(owner.principalId, hermes.principalId)
        assertEquals(PrincipalType.USER, owner.principalType)
        assertEquals(PrincipalStatus.ACTIVE, owner.status)

        runtime.shutdown()
    }

    @Test
    fun `Hermes's PrincipalType is new and distinct from INTERNAL_AGENT`() {
        assertNotEquals(PrincipalType.INTERNAL_AGENT, PrincipalType.EXTERNAL_AGENT)
    }

    // ================= D. Agent Gateway AuthorizationPurposeId is registered =================

    @Test
    fun `the Agent Gateway AuthorizationPurposeId is registered and active`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registry = composedPolicy(runtime).privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")

        assertTrue(registry.isActive(agentGatewayPurpose))

        runtime.shutdown()
    }

    // ================= E. Zero authority while CREATED =================

    @Test
    fun `CREATED Hermes is DENIED even against a synthetic permissive policy that would otherwise approve`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        // Reuse the real, composed IdentityService -- proving this against the actual
        // production-provisioned Hermes principal, not a freshly-constructed test double.
        val identityService = composedIdentityService(runtime)
        val hermes = identityService.resolve(hermesPrincipalId)!!
        assertEquals(PrincipalStatus.CREATED, hermes.status)

        val resourceRegistry = InMemoryResourceRegistry()
        val testResource = Resource(
            resourceId = ResourceId("agent-gateway-identity-test-resource"),
            resourceType = ResourceType.CALENDAR,
            displayName = "Test Resource",
            ownerPrincipalId = hermesPrincipalId,
            sensitivity = ResourceSensitivity.HOUSEHOLD,
            lifecycleState = ResourceLifecycleState.AVAILABLE,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            source = "test",
        )
        resourceRegistry.register(testResource)

        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = "agent gateway identity test action",
                mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.CALENDAR)),
            ),
        )
        // A maximally permissive rule: approves the exact (action, resourceType) pair the
        // request below uses, with no AuthorizationPurpose restriction at all.
        val permissiveRule = PermissionPolicyRule(
            action = PermissionAction.READ,
            resourceType = ResourceType.CALENDAR,
            outcome = PermissionDecisionOutcome.APPROVED,
            level = PermissionLevel.AUTOMATIC,
        )
        val permissivePolicy = DefaultPermissionPolicy(ActionMapper(vocabulary), resourceRegistry, listOf(permissiveRule))
        val engine = DefaultPermissionEngine(identityService, permissivePolicy)

        val request = ExecutionRequest(
            requestId = RequestId("req-agent-gateway-identity-test"),
            principalId = hermesPrincipalId,
            origin = RequestOrigin.AGENT,
            intent = "test intent",
            targetResources = listOf(testResource.resourceId),
            proposedActions = listOf("agent gateway identity test action"),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = "corr-agent-gateway-identity-test",
        )

        val decision = engine.evaluate(request)

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)

        // Control: the identical request, for a freshly-activated principal, is APPROVED by
        // the same permissive policy -- proving the denial above is specifically Hermes's
        // CREATED status, not a defect in the test's own policy/resource wiring.
        val activePrincipalId = PrincipalId("agent-gateway-identity-test-active-control")
        identityService.register(
            Principal(
                principalId = activePrincipalId,
                principalType = PrincipalType.USER,
                displayName = "Active Control Principal",
                owner = null,
                status = PrincipalStatus.CREATED,
                createdAt = Instant.now(),
                lastSeenAt = Instant.now(),
            ),
        )
        identityService.updateStatus(activePrincipalId, PrincipalStatus.ACTIVE)
        val controlDecision = engine.evaluate(request.copy(principalId = activePrincipalId))
        assertEquals(PermissionDecisionOutcome.APPROVED, controlDecision.decision)

        runtime.shutdown()
    }

    // ================= H. No existing policy is broadened =================

    @Test
    fun `any PermissionPolicyRule referencing the new Agent Gateway purpose is exact-verb-scoped, never a coarse grant`() = runTest {
        // At AG-1B's own commit, zero such rules existed at all -- this unit added only the
        // Principal and the AuthorizationPurposeId (Section 20: "This grants nothing in
        // AG-1B"). AG-1C (a later, separately-governed unit, Section 20) is explicitly
        // authorised to add "exact, gateway-purpose-scoped PermissionPolicyRule entries" --
        // see ParkerRuntimeAgentGatewayR0VocabularyCompositionTest for that unit's own
        // dedicated coverage of exactly what it added. What AG-1B's own acceptance criterion H
        // still guarantees, and what this test now proves instead of an absolute "zero rules
        // forever" claim that a later, authorised unit is expected to supersede: every rule
        // naming this purpose is scoped to an exact proposedAction verb phrase -- never a
        // coarse grant across the purpose alone -- so the purpose's mere existence still
        // grants nothing beyond the narrow, named verbs a later unit explicitly adds.
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val rules = composedPolicy(runtime).privateField<List<PermissionPolicyRule>>("rules")
        val gatewayScopedRules = rules.filter { it.authorizationPurpose == agentGatewayPurpose }

        assertTrue(gatewayScopedRules.all { it.proposedAction != null }, "every Agent Gateway purpose rule must be scoped to an exact verb phrase, never a coarse grant")

        runtime.shutdown()
    }

    // ================= F/G. Existing Owner/INTERNAL_AGENT behaviour is unchanged =================

    @Test
    fun `an existing production owner action still resolves APPROVED after AG-1B composition`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val policy = composedPolicy(runtime)
        val decision = policy.evaluate(
            ExecutionRequest(
                requestId = RequestId("req-agent-gateway-identity-regression"),
                principalId = PrincipalId("user.owner-agent-gateway-identity-test"),
                origin = RequestOrigin.TEXT,
                intent = "regression check",
                targetResources = listOf(parker.core.runtime.DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID),
                proposedActions = listOf(parker.core.runtime.DefaultEvidenceCustodian.ACCEPT_ACTION_NAME),
                priority = RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "corr-agent-gateway-identity-regression",
            ),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)

        runtime.shutdown()
    }

    @Test
    fun `INTERNAL_AGENT remains a distinct, unaffected PrincipalType`() {
        val internalAgent = Principal(
            principalId = PrincipalId("agent-gateway-identity-test-internal-agent"),
            principalType = PrincipalType.INTERNAL_AGENT,
            displayName = "Internal Agent",
            owner = PrincipalId("user.owner-agent-gateway-identity-test"),
            status = PrincipalStatus.ACTIVE,
            createdAt = Instant.now(),
            lastSeenAt = Instant.now(),
        )

        assertEquals(PrincipalType.INTERNAL_AGENT, internalAgent.principalType)
        assertNotEquals(PrincipalType.EXTERNAL_AGENT, internalAgent.principalType)
    }

    @Test
    fun `Hermes has a non-null owner -- correct owner lineage, not a root identity`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val hermes = composedIdentityService(runtime).resolve(hermesPrincipalId)!!

        assertEquals(PrincipalId("user.owner-agent-gateway-identity-test"), hermes.owner)

        runtime.shutdown()
    }

    // ================= Production Hermes activation (PARKER_AGENT_GATEWAY_HERMES_ACTIVE) =================
    //
    // Deployment Tests B/C/D/E: the composed runtime's own real startup sequence, exercised
    // directly through ParkerRuntimeConfig.agentGatewayHermesActive (never through
    // docker-compose/env-var parsing, which ParkerRuntimeConfigLoaderTest already covers
    // separately). The composed runtime constructed by [ParkerRuntime.start] always uses the real
    // `InMemoryIdentityService.updateStatus` -- there is no second, alternate activation path.

    @Test
    fun `B - default deployment (activation absent-or-false) leaves Hermes CREATED even with the Gateway HTTP surface fully configured`() = runTest {
        val runtime = ParkerRuntime(config(agentGatewayHermesActive = false, agentGatewayHttpConfigured = true), RecordingParkerLogger())
        runtime.start()

        val hermes = composedIdentityService(runtime).resolve(hermesPrincipalId)

        assertTrue(hermes != null)
        assertEquals(PrincipalStatus.CREATED, hermes!!.status)

        runtime.shutdown()
    }

    @Test
    fun `C - explicit activation transitions exactly Hermes to ACTIVE, reachable through the real composed IdentityService`() = runTest {
        val runtime = ParkerRuntime(config(agentGatewayHermesActive = true, agentGatewayHttpConfigured = true), RecordingParkerLogger())
        runtime.start()

        val identityService = composedIdentityService(runtime)
        val hermes = identityService.resolve(hermesPrincipalId)

        assertTrue(hermes != null)
        assertEquals(PrincipalStatus.ACTIVE, hermes!!.status)

        runtime.shutdown()
    }

    @Test
    fun `explicit activation affects only Hermes -- Owner's own status is unaffected either way`() = runTest {
        val inactiveRuntime = ParkerRuntime(config(agentGatewayHermesActive = false), RecordingParkerLogger())
        inactiveRuntime.start()
        val ownerWithoutActivation = composedIdentityService(inactiveRuntime).resolve(PrincipalId("user.owner-agent-gateway-identity-test"))
        inactiveRuntime.shutdown()

        val activeRuntime = ParkerRuntime(config(agentGatewayHermesActive = true, agentGatewayHttpConfigured = true), RecordingParkerLogger())
        activeRuntime.start()
        val ownerWithActivation = composedIdentityService(activeRuntime).resolve(PrincipalId("user.owner-agent-gateway-identity-test"))
        activeRuntime.shutdown()

        assertTrue(ownerWithoutActivation != null && ownerWithActivation != null)
        assertEquals(ownerWithoutActivation!!.status, ownerWithActivation!!.status)
        assertEquals(PrincipalStatus.ACTIVE, ownerWithActivation.status, "Owner's own activation is unrelated to, and unaffected by, this flag")
    }

    @Test
    fun `D - a second, independent startup with activation true registers Hermes CREATED then explicitly transitions it ACTIVE again -- freshly re-derived, never a persisted one-time state`() = runTest {
        val cfg = config(agentGatewayHermesActive = true, agentGatewayHttpConfigured = true)

        val first = ParkerRuntime(cfg, RecordingParkerLogger())
        first.start()
        assertEquals(PrincipalStatus.ACTIVE, composedIdentityService(first).resolve(hermesPrincipalId)!!.status)
        first.shutdown()

        // A second, wholly separate ParkerRuntime/IdentityService instance, simulating a fresh
        // process restart against the identical activation setting -- proves the transition is
        // performed again on this startup, not merely inherited from state that happened to
        // already exist (InMemoryIdentityService itself holds no state across instances at all).
        val second = ParkerRuntime(cfg, RecordingParkerLogger())
        second.start()
        assertEquals(PrincipalStatus.ACTIVE, composedIdentityService(second).resolve(hermesPrincipalId)!!.status)
        second.shutdown()
    }

    @Test
    fun `E - a second, independent startup with activation false (or absent) leaves Hermes CREATED again -- restart never inherits a prior run's activation`() = runTest {
        val cfg = config(agentGatewayHermesActive = false, agentGatewayHttpConfigured = true)

        val first = ParkerRuntime(cfg, RecordingParkerLogger())
        first.start()
        assertEquals(PrincipalStatus.CREATED, composedIdentityService(first).resolve(hermesPrincipalId)!!.status)
        first.shutdown()

        val second = ParkerRuntime(cfg, RecordingParkerLogger())
        second.start()
        assertEquals(PrincipalStatus.CREATED, composedIdentityService(second).resolve(hermesPrincipalId)!!.status)
        second.shutdown()
    }
}
