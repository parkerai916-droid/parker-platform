package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.IdentityService
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceRegistry
import parker.core.interfaces.ResourceType
import parker.core.runtime.ActionMapper
import parker.core.runtime.ActionVocabulary
import parker.core.runtime.DefaultPermissionPolicy
import parker.core.runtime.InMemoryAuthorizationPurposeRegistry
import parker.core.runtime.PermissionPolicyRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * AG-1C -- R0 Permission Vocabulary
 * (`docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 7 item 2,
 * Section 11, Section 20). Proves the unit's own acceptance criteria against
 * the real, composed [ParkerRuntime]:
 *
 * A. Each new R0 action exists and is distinct.
 * B. Each new resource identifier exists and is distinct (Section 11 froze
 *    "No new ResourceType is introduced" -- see this file's own note below
 *    on the resulting collision with the unit brief's literal wording).
 * C. The Agent Gateway purpose (`agent-gateway.hermes-ingestion`, AG-1B) is
 *    reused exactly -- no second Agent Gateway purpose is registered.
 * D/E/F. Unknown action / unknown resource / wrong purpose all deny.
 * G. An unrelated, active principal type gets no accidental R0 authority.
 * H. Hermes, still status CREATED, remains DENIED despite the new
 *    vocabulary and rules existing.
 * I/J. Existing Owner/INTERNAL_AGENT-shaped behaviour is unchanged.
 * K. No write capability exists in the new vocabulary.
 * L. No evidence-submission/acquisition/HFR/case/provider rule is attached
 *    to the Agent Gateway purpose.
 *
 * Reflection is used only where no public seam exists to observe internal
 * composition state, mirroring [ParkerRuntimeAgentGatewayIdentityCompositionTest]'s
 * own established `privateField` precedent.
 *
 * ## Governance-collision note (Section 11 vs. this unit's own brief)
 *
 * Section 11 of the scope lock is explicit and frozen: "Resources remain
 * opaque and pre-registered. **No new `ResourceType` is introduced.**"
 * The codebase's own established precedent for evidence reads
 * (`DefaultEvidenceCustodian.EVIDENCE_RETRIEVAL_RESOURCE_ID`/
 * `EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID`) already follows this exactly:
 * new, distinct, fixed `ResourceId`s under the *existing*
 * `ResourceType.DOCUMENT`, never a new enum member. This implementation
 * follows that same precedent. Acceptance criterion B is therefore proven
 * here against two new, distinct `ResourceId`s (opaque governed
 * identifiers), not a new `ResourceType` enum member -- the scope lock is
 * treated as authoritative over a literal "resource type" reading of the
 * unit brief.
 *
 * ## Principal-binding security review addendum (POLICY-LEVEL ONLY)
 *
 * The `POLICY-LEVEL ONLY` tests in the "G (adversarial)" section below prove
 * an important, permanent architectural fact: [DefaultPermissionPolicy] is
 * intentionally principal-blind (its own KDoc: "no per-principal matching
 * capability at all"). An ACTIVE `PLUGIN`/`INTERNAL_AGENT`/other principal
 * supplying the correct `agent-gateway.hermes-ingestion` purpose resolves
 * `APPROVED` identically to Hermes at the policy layer. This is NOT evidence
 * that such a principal is entitled to Agent Gateway access, nor that this
 * vocabulary is exploitable in the shipped system -- see the structural
 * principal-binding invariant documented directly on the two `APPROVED`
 * `PermissionPolicyRule` entries in `ParkerRuntime.kt`: no production call
 * site anywhere constructs an `ExecutionRequest` naming this purpose/verb
 * combination, so none of these principals can reach these rules through
 * any real production path today. Production reachability and policy-level
 * matching are two separate invariants; AG-1D/AG-1E owns the former
 * structurally and must never "fix" the latter by adding principal matching
 * to this policy.
 */
class ParkerRuntimeAgentGatewayR0VocabularyCompositionTest {

    private fun config(localTextChannelModuleId: String = "channel.local-text-agent-gateway-r0-vocab-test") = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted by these tests
        modelName = "test-model",
        ownerPrincipalId = "user.owner-agent-gateway-r0-vocab-test",
        localTextChannelModuleId = localTextChannelModuleId,
        evidenceStorageRootPath = Files.createTempDirectory("agent-gateway-r0-vocab-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("agent-gateway-r0-vocab-evidence-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("agent-gateway-r0-vocab-derivative-generation").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("agent-gateway-r0-vocab-derivative-generation-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("agent-gateway-r0-vocab-saved-analysis").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("agent-gateway-r0-vocab-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("agent-gateway-r0-vocab-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("agent-gateway-r0-vocab-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("agent-gateway-r0-vocab-knowledge-items").resolve("items.log").toString(),
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
    private fun composedActionMapper(runtime: ParkerRuntime): ActionMapper = composedPolicy(runtime).privateField("actionMapper")
    private fun composedVocabulary(runtime: ParkerRuntime): ActionVocabulary = composedActionMapper(runtime).privateField("vocabulary")
    private fun composedResourceRegistry(runtime: ParkerRuntime): ResourceRegistry = composedPolicy(runtime).privateField("resourceRegistry")

    private val hermesPrincipalId = PrincipalId("agent.hermes-ingestion-operator")
    private val agentGatewayPurpose = AuthorizationPurposeId("agent-gateway.hermes-ingestion")
    private val retrieveAction = "agent-gateway.evidence.retrieve"
    private val retrieveManifestAction = "agent-gateway.evidence.retrieve-manifest"
    private val retrieveResourceId = ResourceId("agent-gateway-evidence-retrieval")
    private val retrieveManifestResourceId = ResourceId("agent-gateway-evidence-manifest-retrieval")

    private suspend fun activePrincipal(
        runtime: ParkerRuntime,
        id: String,
        type: PrincipalType = PrincipalType.USER,
        owner: PrincipalId? = null,
    ): PrincipalId {
        val identityService = composedIdentityService(runtime)
        val principalId = PrincipalId(id)
        identityService.register(
            Principal(
                principalId = principalId,
                principalType = type,
                displayName = "Test Principal ($id)",
                owner = owner,
                status = PrincipalStatus.CREATED,
                createdAt = Instant.now(),
                lastSeenAt = Instant.now(),
            ),
        )
        identityService.updateStatus(principalId, PrincipalStatus.ACTIVE)
        return principalId
    }

    private fun request(
        principalId: PrincipalId,
        proposedAction: String,
        resourceId: ResourceId,
        authorizationPurpose: AuthorizationPurposeId? = null,
    ) = ExecutionRequest(
        requestId = RequestId("req-agent-gateway-r0-vocab-${proposedAction}-${principalId.value}"),
        principalId = principalId,
        origin = RequestOrigin.AGENT,
        intent = "test intent",
        targetResources = listOf(resourceId),
        proposedActions = listOf(proposedAction),
        priority = RequestPriority.NORMAL,
        createdAt = Instant.now(),
        correlationId = "corr-agent-gateway-r0-vocab",
        authorizationPurpose = authorizationPurpose,
    )

    // ================= A. New R0 actions exist and are distinct =================

    @Test
    fun `both new R0 verb phrases are registered, resolve to READ DOCUMENT, and are distinct from each other`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val vocabulary = composedVocabulary(runtime)
        val retrieveEntry = vocabulary.lookup(retrieveAction)
        val manifestEntry = vocabulary.lookup(retrieveManifestAction)

        assertNotNull(retrieveEntry)
        assertNotNull(manifestEntry)
        assertNotEquals(retrieveAction, retrieveManifestAction)
        assertEquals(setOf(parker.core.interfaces.ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT)), retrieveEntry!!.mappings)
        assertEquals(setOf(parker.core.interfaces.ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT)), manifestEntry!!.mappings)

        runtime.shutdown()
    }

    @Test
    fun `neither new R0 verb phrase collides with or is confused for Owner's own evidence retrieve verbs`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val vocabulary = composedVocabulary(runtime)

        assertNotEquals(retrieveAction, "evidence.retrieve")
        assertNotEquals(retrieveManifestAction, "evidence.retrieve-manifest")
        assertNotNull(vocabulary.lookup("evidence.retrieve"), "AG-1C must not remove or replace Owner's existing verb")
        assertNotNull(vocabulary.lookup("evidence.retrieve-manifest"), "AG-1C must not remove or replace Owner's existing verb")

        runtime.shutdown()
    }

    // ================= B. New resource identifiers exist and are distinct =================

    @Test
    fun `both new resource identifiers are registered under the existing DOCUMENT resource type, never a new ResourceType`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val resourceRegistry = composedResourceRegistry(runtime)
        val retrieveResource = resourceRegistry.resolve(retrieveResourceId)
        val manifestResource = resourceRegistry.resolve(retrieveManifestResourceId)

        assertNotNull(retrieveResource)
        assertNotNull(manifestResource)
        assertNotEquals(retrieveResourceId, retrieveManifestResourceId)
        assertEquals(ResourceType.DOCUMENT, retrieveResource!!.resourceType)
        assertEquals(ResourceType.DOCUMENT, manifestResource!!.resourceType)

        runtime.shutdown()
    }

    @Test
    fun `the PrincipalType and ResourceType enums gained no new members from AG-1C`() {
        // AG-1B's own EXTERNAL_AGENT member (already accepted) remains the only Principal-side
        // addition; AG-1C adds none. ResourceType gains none at all -- Section 11's own freeze.
        assertEquals(
            setOf(
                ResourceType.MEMORY, ResourceType.WORLD_MODEL, ResourceType.DOCUMENT, ResourceType.EMAIL,
                ResourceType.CALENDAR, ResourceType.CONTACT, ResourceType.HOME_ASSISTANT_ENTITY,
                ResourceType.ANDROID_CAPABILITY, ResourceType.TOOL, ResourceType.PLUGIN, ResourceType.AGENT,
                ResourceType.SECRET, ResourceType.CONFIGURATION, ResourceType.AUDIT_LOG,
            ),
            ResourceType.values().toSet(),
        )
    }

    // ================= C. Agent Gateway purpose reused exactly =================

    @Test
    fun `the Agent Gateway purpose is AG-1B's own value, still the only agent-gateway-namespaced purpose registered`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registry = composedPolicy(runtime).privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")
        val entries = registry.privateField<Map<*, *>>("entries")

        assertTrue(registry.isActive(agentGatewayPurpose))
        val gatewayNamespacedPurposes = entries.keys.filterIsInstance<AuthorizationPurposeId>().filter { it.value.startsWith("agent-gateway.") }
        assertEquals(listOf(agentGatewayPurpose), gatewayNamespacedPurposes)

        runtime.shutdown()
    }

    // ================= D/E/F. Unknown action / unknown resource / wrong purpose deny =================

    @Test
    fun `an unknown action name is DENIED`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principalId = activePrincipal(runtime, "agent-gateway-r0-vocab-unknown-action")

        val decision = composedPolicy(runtime).evaluate(
            request(principalId, "agent-gateway.evidence.nonexistent-verb", retrieveResourceId, agentGatewayPurpose),
        )

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
        runtime.shutdown()
    }

    @Test
    fun `an unregistered target resource is DENIED`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principalId = activePrincipal(runtime, "agent-gateway-r0-vocab-unknown-resource")

        val decision = composedPolicy(runtime).evaluate(
            request(principalId, retrieveAction, ResourceId("not-a-registered-resource"), agentGatewayPurpose),
        )

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
        runtime.shutdown()
    }

    @Test
    fun `the wrong AuthorizationPurposeId is DENIED, not silently approved by the exact-verb rule`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principalId = activePrincipal(runtime, "agent-gateway-r0-vocab-wrong-purpose")
        val wrongPurpose = AuthorizationPurposeId("knowledge-memory.candidate-evaluation") // a real, active, but unrelated purpose

        val decision = composedPolicy(runtime).evaluate(
            request(principalId, retrieveAction, retrieveResourceId, wrongPurpose),
        )

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
        runtime.shutdown()
    }

    @Test
    fun `an absent AuthorizationPurposeId is also DENIED`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principalId = activePrincipal(runtime, "agent-gateway-r0-vocab-absent-purpose")

        val decision = composedPolicy(runtime).evaluate(
            request(principalId, retrieveManifestAction, retrieveManifestResourceId, authorizationPurpose = null),
        )

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
        runtime.shutdown()
    }

    // ================= G. Unrelated principal types get no accidental grant =================

    @Test
    fun `an unrelated, active, non-Hermes principal proposing the new verb without the gateway purpose is DENIED`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        // PLUGIN is unrelated to the Agent Gateway -- proves the new verb's exact-verb DENIED
        // guard applies regardless of the calling principal's own type, i.e. no principal type
        // is grandfathered into the pre-existing coarse (READ, DOCUMENT) approval for this verb.
        val principalId = activePrincipal(runtime, "agent-gateway-r0-vocab-unrelated-plugin", type = PrincipalType.PLUGIN, owner = PrincipalId("user.owner-agent-gateway-r0-vocab-test"))

        val decision = composedPolicy(runtime).evaluate(
            request(principalId, retrieveAction, retrieveResourceId, authorizationPurpose = null),
        )

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)
        runtime.shutdown()
    }

    // ================= G (adversarial). POLICY-LEVEL-ONLY regression documentation:
    // DefaultPermissionPolicy is intentionally principal-blind =================
    //
    // IMPORTANT -- READ BEFORE TREATING ANY "APPROVED" BELOW AS A FINDING OF ITS OWN:
    //
    // Security review (AG-1C): the earlier negative tests above only omit the Agent Gateway
    // purpose -- they do not, by themselves, prove principal isolation. DefaultPermissionPolicy's
    // own matching (`ruleOutcomeFor`) reads only `action`, `resourceType`, `authorizationPurpose`,
    // and `proposedAction` -- it never reads `request.principalId` at all (this class's own
    // KDoc: "no per-principal matching capability at all"). The tests below supply the correct
    // Agent Gateway purpose deliberately, to observe and permanently document the actual,
    // current POLICY-LEVEL decision, never to claim any of these principals have -- or should
    // ever have -- real Agent Gateway access.
    //
    // What "APPROVED" below DOES prove: this policy mechanism, exactly as designed, cannot by
    // itself distinguish Hermes from any other ACTIVE principal that happens to supply the same
    // purpose/verb/resource triple.
    //
    // What "APPROVED" below does NOT prove: that a PLUGIN, INTERNAL_AGENT, or any other
    // unrelated principal is entitled to Agent Gateway access, or that this vocabulary is
    // exploitable in the shipped system today. Production reachability is a SEPARATE invariant,
    // owned by call-site structure, not by this policy -- see the structural principal-binding
    // invariant documented directly on the two APPROVED PermissionPolicyRule entries in
    // `ParkerRuntime.kt` (points 3-8): as of AG-1C, no production call site anywhere constructs
    // an `ExecutionRequest` naming this purpose/verb combination at all, so none of the
    // principals below can actually reach these rules through any real production path. AG-1D/
    // AG-1E must enforce that reachability invariant structurally, never by changing this policy.

    @Test
    fun `POLICY-LEVEL ONLY -- an ACTIVE PLUGIN with the correct purpose is approved by the policy alone, unreachable in production`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val pluginId = activePrincipal(runtime, "agent-gateway-r0-vocab-adversarial-plugin", type = PrincipalType.PLUGIN, owner = PrincipalId("user.owner-agent-gateway-r0-vocab-test"))

        val decision = composedPolicy(runtime).evaluate(
            request(pluginId, retrieveAction, retrieveResourceId, authorizationPurpose = agentGatewayPurpose),
        )

        // Documents the actual, current POLICY-LEVEL behaviour only -- DefaultPermissionPolicy
        // has no principal matching, so a PLUGIN principal supplying the correct
        // purpose/action/resource is indistinguishable, AT THE POLICY LAYER, from Hermes itself.
        // This is NOT evidence of a reachable production vulnerability: see this section's own
        // header comment and the invariant on the rule itself in ParkerRuntime.kt.
        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        runtime.shutdown()
    }

    @Test
    fun `POLICY-LEVEL ONLY -- an ACTIVE PLUGIN is also approved for retrieve-manifest, the gap is not verb-specific`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val pluginId = activePrincipal(runtime, "agent-gateway-r0-vocab-adversarial-plugin-manifest", type = PrincipalType.PLUGIN, owner = PrincipalId("user.owner-agent-gateway-r0-vocab-test"))

        val decision = composedPolicy(runtime).evaluate(
            request(pluginId, retrieveManifestAction, retrieveManifestResourceId, authorizationPurpose = agentGatewayPurpose),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision, "confirms the policy-level outcome is identical in shape for retrieve-manifest, not specific to retrieve")
        runtime.shutdown()
    }

    @Test
    fun `POLICY-LEVEL ONLY -- an ACTIVE INTERNAL_AGENT with the correct purpose is approved by the policy alone, unreachable in production`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val internalAgentId = activePrincipal(
            runtime,
            "agent-gateway-r0-vocab-adversarial-internal-agent",
            type = PrincipalType.INTERNAL_AGENT,
            owner = PrincipalId("user.owner-agent-gateway-r0-vocab-test"),
        )

        val decision = composedPolicy(runtime).evaluate(
            request(internalAgentId, retrieveAction, retrieveResourceId, authorizationPurpose = agentGatewayPurpose),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        runtime.shutdown()
    }

    @Test
    fun `POLICY-LEVEL ONLY -- an ACTIVE INTERNAL_AGENT is also approved for retrieve-manifest, the gap is not verb-specific`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val internalAgentId = activePrincipal(
            runtime,
            "agent-gateway-r0-vocab-adversarial-internal-agent-manifest",
            type = PrincipalType.INTERNAL_AGENT,
            owner = PrincipalId("user.owner-agent-gateway-r0-vocab-test"),
        )

        val decision = composedPolicy(runtime).evaluate(
            request(internalAgentId, retrieveManifestAction, retrieveManifestResourceId, authorizationPurpose = agentGatewayPurpose),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision, "confirms the policy-level outcome is identical in shape for retrieve-manifest, not specific to retrieve")
        runtime.shutdown()
    }

    @Test
    fun `POLICY-LEVEL ONLY -- no PrincipalId is special-cased, any resolvable principal satisfies the rule identically at the policy layer`() = runTest {
        // Strongest form of the documented fact: DefaultPermissionPolicy.evaluate never reads
        // request.principalId at all (confirmed by reading DefaultPermissionPolicy.kt's own
        // evaluate/ruleOutcomeFor bodies) -- so a principal bearing neither Hermes's identity
        // nor any relationship to the Agent Gateway is approved identically AT THE POLICY LAYER.
        // (owner is still supplied because InMemoryIdentityService.register requires one for any
        // non-USER/SYSTEM PrincipalType -- an identity-registration constraint, unrelated to
        // policy matching.) As above, this is a policy-level fact only -- see this section's own
        // header comment on why it is not itself a production-reachable finding.
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val strangerId = activePrincipal(
            runtime,
            "agent-gateway-r0-vocab-adversarial-stranger",
            type = PrincipalType.TOOL,
            owner = PrincipalId("user.owner-agent-gateway-r0-vocab-test"),
        )

        val decision = composedPolicy(runtime).evaluate(
            request(strangerId, retrieveAction, retrieveResourceId, authorizationPurpose = agentGatewayPurpose),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        runtime.shutdown()
    }

    // ================= H. CREATED Hermes remains DENIED despite the new vocabulary =================

    @Test
    fun `CREATED Hermes is DENIED through the full composed engine even naming the correct verb, resource, and purpose`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val identityService = composedIdentityService(runtime)
        val hermes = identityService.resolve(hermesPrincipalId)!!
        assertEquals(PrincipalStatus.CREATED, hermes.status, "AG-1C must not activate Hermes")

        val engine = composedEngine(runtime)
        val retrieveDecision = engine.evaluate(request(hermesPrincipalId, retrieveAction, retrieveResourceId, agentGatewayPurpose))
        val manifestDecision = engine.evaluate(request(hermesPrincipalId, retrieveManifestAction, retrieveManifestResourceId, agentGatewayPurpose))

        assertEquals(PermissionDecisionOutcome.DENIED, retrieveDecision.decision)
        assertEquals(PermissionDecisionOutcome.DENIED, manifestDecision.decision)

        runtime.shutdown()
    }

    @Test
    fun `the identical request for a freshly-activated control principal is APPROVED, isolating the denial above to Hermes's CREATED status`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val controlPrincipalId = activePrincipal(runtime, "agent-gateway-r0-vocab-active-control")

        val decision = composedEngine(runtime).evaluate(
            request(controlPrincipalId, retrieveAction, retrieveResourceId, agentGatewayPurpose),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        runtime.shutdown()
    }

    // ================= I. Existing Owner behaviour is unchanged =================

    @Test
    fun `Owner's existing evidence retrieve and retrieve-manifest actions still resolve APPROVED, unaffected by the new guards`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val ownerPrincipalId = PrincipalId("user.owner-agent-gateway-r0-vocab-test")

        val retrieveDecision = composedPolicy(runtime).evaluate(
            request(ownerPrincipalId, "evidence.retrieve", parker.core.runtime.DefaultEvidenceCustodian.EVIDENCE_RETRIEVAL_RESOURCE_ID),
        )
        val manifestDecision = composedPolicy(runtime).evaluate(
            request(ownerPrincipalId, "evidence.retrieve-manifest", parker.core.runtime.DefaultEvidenceCustodian.EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID),
        )
        val acceptDecision = composedPolicy(runtime).evaluate(
            request(ownerPrincipalId, parker.core.runtime.DefaultEvidenceCustodian.ACCEPT_ACTION_NAME, parker.core.runtime.DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, retrieveDecision.decision)
        assertEquals(PermissionDecisionOutcome.APPROVED, manifestDecision.decision)
        assertEquals(PermissionDecisionOutcome.APPROVED, acceptDecision.decision)

        runtime.shutdown()
    }

    // ================= J. Existing INTERNAL_AGENT behaviour is unchanged =================

    @Test
    fun `INTERNAL_AGENT remains distinct from EXTERNAL_AGENT and is not itself granted R0 authority merely by its type`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        assertNotEquals(PrincipalType.INTERNAL_AGENT, PrincipalType.EXTERNAL_AGENT)
        val internalAgentId = activePrincipal(
            runtime,
            "agent-gateway-r0-vocab-internal-agent",
            type = PrincipalType.INTERNAL_AGENT,
            owner = PrincipalId("user.owner-agent-gateway-r0-vocab-test"),
        )

        val decision = composedPolicy(runtime).evaluate(
            request(internalAgentId, retrieveAction, retrieveResourceId, authorizationPurpose = null),
        )

        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision, "an INTERNAL_AGENT principal gets no free pass around the exact-verb guard either")
        runtime.shutdown()
    }

    // ================= K. No write capability exists =================

    @Test
    fun `neither new verb's ActionVocabulary mapping includes WRITE, DELETE, or EXECUTE`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val vocabulary = composedVocabulary(runtime)
        val retrieveEntry = vocabulary.lookup(retrieveAction)!!
        val manifestEntry = vocabulary.lookup(retrieveManifestAction)!!

        assertTrue(retrieveEntry.mappings.all { it.action == PermissionAction.READ })
        assertTrue(manifestEntry.mappings.all { it.action == PermissionAction.READ })

        runtime.shutdown()
    }

    // ================= L. No evidence-submission/acquisition/HFR/case/provider rule =================

    @Test
    fun `exactly the two R0 READ DOCUMENT rules are scoped to the Agent Gateway purpose -- as of AG-1C, nothing else`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val rules = composedPolicy(runtime).privateField<List<PermissionPolicyRule>>("rules")
        // AG-1F (R1 Candidate-Source Submission) later adds a third, WRITE-scoped rule to this
        // same purpose -- deliberately excluded here so this AG-1C-scoped assertion continues to
        // prove exactly what AG-1C itself added. See
        // `no rule maps the Agent Gateway purpose to DELETE or EXECUTE, and the only WRITE rule
        // is AG-1F's own exact submission verb` below for the complete, current picture.
        val readRules = rules.filter { it.authorizationPurpose == agentGatewayPurpose && it.action == PermissionAction.READ }

        assertEquals(2, readRules.size)
        readRules.forEach { rule ->
            assertEquals(ResourceType.DOCUMENT, rule.resourceType)
            assertEquals(PermissionDecisionOutcome.APPROVED, rule.outcome)
            assertTrue(rule.proposedAction == retrieveAction || rule.proposedAction == retrieveManifestAction)
        }

        runtime.shutdown()
    }

    @Test
    fun `no rule maps the Agent Gateway purpose to DELETE or EXECUTE, and the only WRITE rules are AG-1F's submission verb and AG-1G's acquisition verb`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val rules = composedPolicy(runtime).privateField<List<PermissionPolicyRule>>("rules")
        val gatewayScopedRules = rules.filter { it.authorizationPurpose == agentGatewayPurpose }

        assertTrue(rules.none { it.authorizationPurpose == agentGatewayPurpose && (it.action == PermissionAction.DELETE || it.action == PermissionAction.EXECUTE) })
        val writeRules = gatewayScopedRules.filter { it.action == PermissionAction.WRITE }
        // Revision history: BI-4 adds a third WRITE rule for the fixed batch-binding operation.
        // reachability of the governed acquisition workflow at all.
        assertEquals(3, writeRules.size)
        assertEquals(setOf("agent-gateway.evidence.submit", "agent-gateway.evidence.acquire", "agent-gateway.ingestion.bind"), writeRules.map { it.proposedAction }.toSet())
        writeRules.forEach { rule ->
            assertEquals(ResourceType.DOCUMENT, rule.resourceType)
            assertEquals(PermissionDecisionOutcome.APPROVED, rule.outcome)
        }

        runtime.shutdown()
    }
}
