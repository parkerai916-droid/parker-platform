package parker.core.runtime

import java.lang.reflect.Modifier
import java.time.Instant
import kotlin.reflect.full.declaredFunctions
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceSourceManifest
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.IdentityService
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionExplanation
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.ResourceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * AG-1D -- R0 Governed Runtime Projections
 * (`docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 7 item 2,
 * Section 20). Unit-level coverage of [AgentGatewayEvidenceProjection] --
 * the thin "AsAgent" read layer this unit adds -- exercised against a
 * hand-built environment carrying the exact same rule shape AG-1C already
 * committed (a coarse `(READ, DOCUMENT)` approval for Owner's own
 * `evidence.retrieve`/`evidence.retrieve-manifest`, plus AG-1C's own
 * exact-verb DENIED guards and Purpose-scoped `APPROVED` overrides for the
 * two Agent Gateway verbs).
 *
 * Composition-level proof that the *real*, composed `ParkerRuntime` wires
 * this class correctly, with the real (`CREATED`) Hermes principal, lives
 * in `ParkerRuntimeAgentGatewayR0ProjectionCompositionTest`. This file's
 * own job is the class's *own* logic: exact request shape, structural
 * principal binding, and behaviour once a principal is genuinely
 * authorised -- which requires a synthetic, test-only ACTIVE Hermes,
 * since AG-1D must not activate the real one.
 */
class AgentGatewayEvidenceProjectionTest {

    private val hermesPrincipalId = PrincipalId("agent.hermes-ingestion-operator")
    private val agentGatewayPurpose = AuthorizationPurposeId("agent-gateway.hermes-ingestion")
    private val ownerEvidenceRetrieveResourceId = DefaultEvidenceCustodian.EVIDENCE_RETRIEVAL_RESOURCE_ID
    private val ownerEvidenceManifestResourceId = DefaultEvidenceCustodian.EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID
    private val gatewayRetrieveResourceId = AgentGatewayEvidenceProjection.AGENT_GATEWAY_EVIDENCE_RETRIEVAL_RESOURCE_ID
    private val gatewayManifestResourceId = AgentGatewayEvidenceProjection.AGENT_GATEWAY_EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID
    private val gatewayRetrieveAction = AgentGatewayEvidenceProjection.AGENT_GATEWAY_EVIDENCE_RETRIEVE_ACTION_NAME
    private val gatewayManifestAction = AgentGatewayEvidenceProjection.AGENT_GATEWAY_EVIDENCE_RETRIEVE_MANIFEST_ACTION_NAME
    private val gatewaySubmitResourceId = AgentGatewayEvidenceProjection.AGENT_GATEWAY_EVIDENCE_SUBMIT_RESOURCE_ID
    private val gatewaySubmitAction = AgentGatewayEvidenceProjection.AGENT_GATEWAY_EVIDENCE_SUBMIT_ACTION_NAME

    /** Records every [ExecutionRequest] this test's engine was asked to evaluate, then delegates to [delegate]. */
    private class RecordingPermissionEngine(private val delegate: PermissionEngine) : PermissionEngine {
        val requests = mutableListOf<ExecutionRequest>()
        override suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
            requests.add(request)
            return delegate.evaluate(request)
        }
        override suspend fun explain(decisionId: parker.core.interfaces.DecisionId): PermissionExplanation = delegate.explain(decisionId)
    }

    private class Environment(
        val identityService: InMemoryIdentityService,
        val engine: RecordingPermissionEngine,
        val evidenceCustodian: DefaultEvidenceCustodian,
        val artifactStorage: InMemoryEvidenceArtifactStorage,
        val manifestStorage: InMemoryEvidenceSourceManifestStorage,
        val projection: AgentGatewayEvidenceProjection,
    )

    /**
     * Builds the minimum environment reproducing production's own rule shape for these two
     * verbs: the pre-existing coarse `(READ, DOCUMENT)` approval (needed for
     * [EvidenceCustodian.retrieve]/[EvidenceCustodian.retrieveManifest]'s own separate internal
     * check, unmodified by this unit) plus AG-1C's own exact-verb DENIED guards and
     * Purpose-scoped `APPROVED` overrides for the two Agent Gateway verbs.
     */
    private suspend fun buildEnvironment(): Environment {
        val identityService = InMemoryIdentityService()
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val ownerPrincipalId = PrincipalId("user.owner-agent-gateway-projection-test")
        identityService.register(
            Principal(ownerPrincipalId, PrincipalType.USER, "Owner", null, PrincipalStatus.CREATED, now, now),
        )
        identityService.updateStatus(ownerPrincipalId, PrincipalStatus.ACTIVE)

        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(DefaultEvidenceCustodian.RETRIEVE_ACTION_NAME, setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT))),
        )
        vocabulary.register(
            ActionVocabularyEntry(DefaultEvidenceCustodian.RETRIEVE_MANIFEST_ACTION_NAME, setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT))),
        )
        vocabulary.register(
            ActionVocabularyEntry(gatewayRetrieveAction, setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT))),
        )
        vocabulary.register(
            ActionVocabularyEntry(gatewayManifestAction, setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT))),
        )
        vocabulary.register(
            ActionVocabularyEntry(DefaultEvidenceCustodian.ACCEPT_ACTION_NAME, setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT))),
        )
        vocabulary.register(
            ActionVocabularyEntry(gatewaySubmitAction, setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT))),
        )

        val resourceRegistry = InMemoryResourceRegistry()
        listOf(
            ownerEvidenceRetrieveResourceId, ownerEvidenceManifestResourceId, gatewayRetrieveResourceId,
            gatewayManifestResourceId, DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID, gatewaySubmitResourceId,
        ).forEach { id ->
            resourceRegistry.register(
                parker.core.interfaces.Resource(
                    resourceId = id,
                    resourceType = ResourceType.DOCUMENT,
                    displayName = id.value,
                    ownerPrincipalId = ownerPrincipalId,
                    sensitivity = parker.core.interfaces.ResourceSensitivity.PUBLIC,
                    lifecycleState = parker.core.interfaces.ResourceLifecycleState.REGISTERED,
                    createdAt = now,
                    updatedAt = now,
                    source = "test",
                ),
            )
        }

        val authorizationPurposeRegistry = InMemoryAuthorizationPurposeRegistry()
        authorizationPurposeRegistry.register(agentGatewayPurpose)

        val rules = listOf(
            PermissionPolicyRule(PermissionAction.READ, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC),
            PermissionPolicyRule(PermissionAction.WRITE, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC),
            PermissionPolicyRule(PermissionAction.READ, ResourceType.DOCUMENT, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC, proposedAction = gatewayRetrieveAction),
            PermissionPolicyRule(PermissionAction.READ, ResourceType.DOCUMENT, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC, proposedAction = gatewayManifestAction),
            PermissionPolicyRule(PermissionAction.WRITE, ResourceType.DOCUMENT, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC, proposedAction = gatewaySubmitAction),
            PermissionPolicyRule(
                PermissionAction.READ, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC,
                authorizationPurpose = agentGatewayPurpose, proposedAction = gatewayRetrieveAction,
            ),
            PermissionPolicyRule(
                PermissionAction.READ, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC,
                authorizationPurpose = agentGatewayPurpose, proposedAction = gatewayManifestAction,
            ),
            PermissionPolicyRule(
                PermissionAction.WRITE, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC,
                authorizationPurpose = agentGatewayPurpose, proposedAction = gatewaySubmitAction,
            ),
        )
        val policy = DefaultPermissionPolicy(ActionMapper(vocabulary), resourceRegistry, rules, authorizationPurposeRegistry)
        val engine = RecordingPermissionEngine(DefaultPermissionEngine(identityService, policy))
        val artifactStorage = InMemoryEvidenceArtifactStorage()
        val manifestStorage = InMemoryEvidenceSourceManifestStorage()
        val evidenceCustodian = DefaultEvidenceCustodian(artifactStorage, engine, manifestStorage)
        val projection = AgentGatewayEvidenceProjection(hermesPrincipalId, agentGatewayPurpose, engine, evidenceCustodian)

        return Environment(identityService, engine, evidenceCustodian, artifactStorage, manifestStorage, projection)
    }

    private suspend fun Environment.registerHermes(status: PrincipalStatus) {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        identityService.register(
            Principal(hermesPrincipalId, PrincipalType.EXTERNAL_AGENT, "Hermes (test)", PrincipalId("user.owner-agent-gateway-projection-test"), PrincipalStatus.CREATED, now, now),
        )
        if (status == PrincipalStatus.ACTIVE) {
            identityService.updateStatus(hermesPrincipalId, PrincipalStatus.ACTIVE)
        }
    }

    // ================= B/C/D. Exact principal / purpose / action / resource used =================

    @Test
    fun `retrieveEvidence constructs a request naming exactly Hermes, the gateway purpose, the gateway retrieve verb, and its own resource`() = runTest {
        val env = buildEnvironment()
        env.registerHermes(PrincipalStatus.CREATED) // stays CREATED -- request shape is checked regardless of outcome

        env.projection.retrieveEvidence(EvidenceArtifactId("evidence-1"))

        val request = env.engine.requests.single()
        assertEquals(hermesPrincipalId, request.principalId)
        assertEquals(agentGatewayPurpose, request.authorizationPurpose)
        assertEquals(listOf(gatewayRetrieveAction), request.proposedActions)
        assertEquals(listOf(gatewayRetrieveResourceId), request.targetResources)
    }

    @Test
    fun `retrieveEvidenceManifest constructs a request naming exactly Hermes, the gateway purpose, the gateway manifest verb, and its own resource`() = runTest {
        val env = buildEnvironment()
        env.registerHermes(PrincipalStatus.CREATED)

        env.projection.retrieveEvidenceManifest(EvidenceArtifactId("evidence-1"))

        val request = env.engine.requests.single()
        assertEquals(hermesPrincipalId, request.principalId)
        assertEquals(agentGatewayPurpose, request.authorizationPurpose)
        assertEquals(listOf(gatewayManifestAction), request.proposedActions)
        assertEquals(listOf(gatewayManifestResourceId), request.targetResources)
    }

    @Test
    fun `the gateway proposedAction and resource ids match AG-1C's own registered vocabulary exactly`() {
        assertEquals("agent-gateway.evidence.retrieve", gatewayRetrieveAction)
        assertEquals("agent-gateway.evidence.retrieve-manifest", gatewayManifestAction)
        assertEquals(parker.core.interfaces.ResourceId("agent-gateway-evidence-retrieval"), gatewayRetrieveResourceId)
        assertEquals(parker.core.interfaces.ResourceId("agent-gateway-evidence-manifest-retrieval"), gatewayManifestResourceId)
    }

    // ================= A/G/H/I/J. No arbitrary principal parameter exists on this class =================

    @Test
    fun `retrieveEvidence and retrieveEvidenceManifest accept only an EvidenceArtifactId -- no PrincipalId, PrincipalType, or AuthorizationPurposeId parameter of any kind`() {
        val functions = AgentGatewayEvidenceProjection::class.declaredFunctions.filter { it.name == "retrieveEvidence" || it.name == "retrieveEvidenceManifest" }
        assertEquals(2, functions.size)
        functions.forEach { function ->
            val valueParameterTypes = function.parameters
                .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
                .map { it.type.classifier }
            assertEquals(listOf(EvidenceArtifactId::class), valueParameterTypes, "unexpected parameter(s) on ${function.name}: $valueParameterTypes")
        }
    }

    @Test
    fun `AgentGatewayEvidenceProjection declares no public constructor parameter settable after construction -- hermesPrincipalId and purpose are fixed for the object's lifetime`() {
        val fields = AgentGatewayEvidenceProjection::class.java.declaredFields.filter { it.name in setOf("hermesPrincipalId", "agentGatewayPurpose") }
        assertEquals(2, fields.size)
        fields.forEach { field ->
            assertTrue(Modifier.isFinal(field.modifiers), "${field.name} must be immutable (val), never reassignable after construction")
        }
    }

    // ================= K. Unknown target evidence id fails closed (NotFound, not Found) =================

    @Test
    fun `an unknown evidenceArtifactId resolves NotFound for an authorised, ACTIVE Hermes, never Found and never an exception`() = runTest {
        val env = buildEnvironment()
        env.registerHermes(PrincipalStatus.ACTIVE)

        val retrieveResult = env.projection.retrieveEvidence(EvidenceArtifactId("never-accepted"))
        val manifestResult = env.projection.retrieveEvidenceManifest(EvidenceArtifactId("never-accepted"))

        assertIs<AgentGatewayEvidenceRetrievalResult.NotFound>(retrieveResult)
        assertIs<AgentGatewayEvidenceManifestResult.NotFound>(manifestResult)
    }

    // ================= F. Synthetic ACTIVE Hermes reaches the projection when authorised =================

    @Test
    fun `a synthetic ACTIVE Hermes, on the same runtime path and policy, successfully retrieves previously-seeded evidence`() = runTest {
        val env = buildEnvironment()
        env.registerHermes(PrincipalStatus.ACTIVE)
        val id = EvidenceArtifactId("evidence-1")
        val content = "hello agent gateway".toByteArray()
        env.artifactStorage.write(id, content)

        val result = env.projection.retrieveEvidence(id)

        assertIs<AgentGatewayEvidenceRetrievalResult.Found>(result)
        assertEquals(id, result.evidenceArtifactId)
        assertEquals(content.size, result.byteLength)
    }

    @Test
    fun `a synthetic ACTIVE Hermes, on the same runtime path and policy, successfully retrieves a previously-seeded manifest`() = runTest {
        val env = buildEnvironment()
        env.registerHermes(PrincipalStatus.ACTIVE)
        val id = EvidenceArtifactId("evidence-1")
        val sha256 = "a".repeat(64)
        env.manifestStorage.write(EvidenceSourceManifest(id, sha256, 42L, "application/pdf", "source.pdf"))

        val result = env.projection.retrieveEvidenceManifest(id)

        assertIs<AgentGatewayEvidenceManifestResult.Found>(result)
        assertEquals(id, result.manifest.evidenceArtifactId)
        assertEquals(sha256, result.manifest.sha256)
        assertEquals(42L, result.manifest.byteLength)
        assertEquals("application/pdf", result.manifest.receivedMediaType)
        assertEquals("source.pdf", result.manifest.originalFileName)
    }

    // ================= E-equivalent (unit level). CREATED Hermes is DENIED here too =================

    @Test
    fun `a CREATED (not yet ACTIVE) Hermes is DENIED for both operations`() = runTest {
        val env = buildEnvironment()
        env.registerHermes(PrincipalStatus.CREATED)
        val id = EvidenceArtifactId("evidence-1")
        env.artifactStorage.write(id, "content".toByteArray())
        env.manifestStorage.write(EvidenceSourceManifest(id, "b".repeat(64), 7L))

        val retrieveResult = env.projection.retrieveEvidence(id)
        val manifestResult = env.projection.retrieveEvidenceManifest(id)

        assertIs<AgentGatewayEvidenceRetrievalResult.Denied>(retrieveResult)
        assertEquals(PermissionDecisionOutcome.DENIED, retrieveResult.decision)
        assertIs<AgentGatewayEvidenceManifestResult.Denied>(manifestResult)
        assertEquals(PermissionDecisionOutcome.DENIED, manifestResult.decision)
    }

    // ================= L. Narrow DTO only =================

    @Test
    fun `AgentGatewayEvidenceRetrievalResult Found carries only evidenceArtifactId and byteLength -- never raw content`() {
        val fields = AgentGatewayEvidenceRetrievalResult.Found::class.java.declaredFields.filterNot { it.isSynthetic }
        assertEquals(setOf("evidenceArtifactId", "byteLength"), fields.map { it.name }.toSet())
    }

    @Test
    fun `AgentGatewayEvidenceManifestProjection carries only opaque, already-narrow manifest fields`() {
        val fields = AgentGatewayEvidenceManifestProjection::class.java.declaredFields.filterNot { it.isSynthetic }
        assertEquals(
            setOf("evidenceArtifactId", "sha256", "byteLength", "receivedMediaType", "originalFileName"),
            fields.map { it.name }.toSet(),
        )
    }

    // ================= M/N/O/P/Q. Only the exact governed surface exists -- no other mutation =================

    @Test
    fun `AgentGatewayEvidenceProjection declares exactly four public methods -- the two R0 reads, AG-1F's governed submission, and AG-1G's governed acquisition request, nothing else`() {
        val publicFunctionNames = AgentGatewayEvidenceProjection::class.declaredFunctions
            .filter { it.visibility == kotlin.reflect.KVisibility.PUBLIC }
            .map { it.name }
            .toSet()
        // In particular: no transcription, HFR, case-assignment, or deletion method exists on
        // this class at all -- it is structurally impossible for this projection to trigger any
        // of them. requestAcquisition (AG-1G) delegates the entire acquisition decision to the
        // existing, unmodified GovernedAcquisitionOwnerWorkflow -- this class itself still
        // performs no routing, provider, or egress logic of its own.
        //
        // Revision history: AG-1G added requestAcquisition (three -> four).
        assertEquals(setOf("retrieveEvidence", "retrieveEvidenceManifest", "submitSource", "requestAcquisition"), publicFunctionNames)
    }

    // ================= AG-1F. Candidate-source submission =================

    @Test
    fun `submitSource constructs a request naming exactly Hermes, the gateway purpose, the submit verb, and its own resource`() = runTest {
        val env = buildEnvironment()
        env.registerHermes(PrincipalStatus.CREATED) // request shape is checked regardless of outcome

        env.projection.submitSource(parker.core.interfaces.CandidateEvidenceArtifact("content".toByteArray()), null)

        val request = env.engine.requests.single()
        assertEquals(hermesPrincipalId, request.principalId)
        assertEquals(agentGatewayPurpose, request.authorizationPurpose)
        assertEquals(listOf(gatewaySubmitAction), request.proposedActions)
        assertEquals(listOf(gatewaySubmitResourceId), request.targetResources)
    }

    @Test
    fun `submitSource accepts only a CandidateEvidenceArtifact and an optional advisory hash -- no PrincipalId, PrincipalType, or AuthorizationPurposeId parameter of any kind`() {
        val function = AgentGatewayEvidenceProjection::class.declaredFunctions.single { it.name == "submitSource" }
        val valueParameterTypes = function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .map { it.type.classifier }
        assertEquals(listOf(parker.core.interfaces.CandidateEvidenceArtifact::class, String::class), valueParameterTypes)
    }

    @Test
    fun `a CREATED (not yet ACTIVE) Hermes is DENIED for submission too`() = runTest {
        val env = buildEnvironment()
        env.registerHermes(PrincipalStatus.CREATED)

        val result = env.projection.submitSource(parker.core.interfaces.CandidateEvidenceArtifact("content".toByteArray()), null)

        assertIs<AgentGatewaySourceSubmissionResult.Denied>(result)
        assertEquals(PermissionDecisionOutcome.DENIED, result.decision)
    }

    @Test
    fun `a synthetic ACTIVE Hermes successfully registers a new source, then the identical bytes resolve AlreadyRegistered`() = runTest {
        val env = buildEnvironment()
        env.registerHermes(PrincipalStatus.ACTIVE)
        val content = "hello agent gateway submission".toByteArray()

        val first = env.projection.submitSource(parker.core.interfaces.CandidateEvidenceArtifact(content, "text/plain", "hello.txt"), null)
        val second = env.projection.submitSource(parker.core.interfaces.CandidateEvidenceArtifact(content, "text/plain", "hello.txt"), null)

        val registered = assertIs<AgentGatewaySourceSubmissionResult.Registered>(first)
        val alreadyRegistered = assertIs<AgentGatewaySourceSubmissionResult.AlreadyRegistered>(second)
        assertEquals(registered.projection.evidenceArtifactId, alreadyRegistered.projection.evidenceArtifactId)
        assertEquals("text/plain", registered.projection.receivedMediaType)
        assertEquals("hello.txt", registered.projection.originalFileName)
    }

    @Test
    fun `a synthetic ACTIVE Hermes submitting a mismatched advisory hash gets HashMismatch and nothing is registered`() = runTest {
        val env = buildEnvironment()
        env.registerHermes(PrincipalStatus.ACTIVE)
        val content = "content for mismatch test".toByteArray()

        val result = env.projection.submitSource(
            parker.core.interfaces.CandidateEvidenceArtifact(content),
            "0".repeat(64),
        )

        assertIs<AgentGatewaySourceSubmissionResult.HashMismatch>(result)
    }
}
