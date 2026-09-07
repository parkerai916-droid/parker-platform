package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredFunctions
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceAcquisitionCapability
import parker.core.interfaces.AcquisitionAvailability
import parker.core.interfaces.AcquisitionAvailabilityReason
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ExternalTranscriptionMechanism
import parker.core.interfaces.ExternalTranscriptionMechanismOutcome
import parker.core.interfaces.ExternalTranscriptionRequest
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Parker Agent Gateway, AG-1G (R2 Governed-Acquisition Request,
 * `docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 9, Section 20). Unit/integration
 * coverage of [AgentGatewayEvidenceProjection.requestAcquisition] against a hand-built environment
 * carrying the same rule shape [AgentGatewayEvidenceProjectionTest] already uses for AG-1D/AG-1F,
 * extended with a *real*, Hermes-principal-scoped [GovernedAcquisitionOwnerWorkflow] wired to the
 * *real* [DeterministicEvidenceAcquisitionRouter]/[GovernedAcquisitionExecutionCoordinator]/
 * [TierANativeAcquisitionExecutor]/[TierAOwnerInvocationCoordinator]/real Tier A extractors --
 * never a fake acquisition pipeline -- so these tests genuinely exercise the same, unmodified
 * governed acquisition machinery Owner UI's own route already uses, not a second implementation.
 *
 * Composition-level proof that the *real*, composed `ParkerRuntime` wires this correctly (Hermes
 * left `CREATED`, real permission rules, real HTTP transport) lives in
 * `AgentGatewayHttpServerTest`'s real-harness section and
 * `ParkerRuntimeAgentGatewayR0VocabularyCompositionTest`. This file's own job is
 * [requestAcquisition]'s own logic once a synthetic ACTIVE Hermes reaches it.
 */
class AgentGatewayAcquisitionRequestTest {

    private val hermesPrincipalId = PrincipalId("agent.hermes-ingestion-operator")
    private val agentGatewayPurpose = AuthorizationPurposeId("agent-gateway.hermes-ingestion")
    private val ownerPrincipalId = PrincipalId("user.owner-agent-gateway-acquisition-test")
    private val gatewayAcquireResourceId = AgentGatewayEvidenceProjection.AGENT_GATEWAY_EVIDENCE_ACQUIRE_RESOURCE_ID
    private val gatewayAcquireAction = AgentGatewayEvidenceProjection.AGENT_GATEWAY_EVIDENCE_ACQUIRE_ACTION_NAME

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
        val projection: AgentGatewayEvidenceProjection,
    )

    /**
     * Builds the minimum environment reproducing production's own rule shape for the acquire verb
     * (the pre-existing coarse `(WRITE, DOCUMENT)` approval, plus AG-1G's own exact-verb DENIED
     * guard and Purpose-scoped `APPROVED` override), wired to a *real* Hermes-scoped
     * [GovernedAcquisitionOwnerWorkflow] over [registry]/[externalEgressAuthorised]. When
     * [registry] is `null`, [AgentGatewayEvidenceProjection] is built with no acquisition workflow
     * at all (the "not configured" defensive case).
     */
    private suspend fun buildEnvironment(
        registry: GovernedAcquisitionCapabilityRegistry? = null,
        externalEgressAuthorised: suspend (EvidenceArtifactId) -> Boolean = { false },
        externalExecutorFactory: ((RecordingPermissionEngine, DefaultEvidenceCustodian) -> BoundAcquisitionCapabilityExecutor)? = null,
        tierAAuditLogFile: java.nio.file.Path = Files.createTempDirectory("ag-1g-ingestion-audit").resolve("audit.log"),
    ): Environment {
        val identityService = InMemoryIdentityService()
        val now = Instant.parse("2026-01-01T00:00:00Z")
        identityService.register(Principal(ownerPrincipalId, PrincipalType.USER, "Owner", null, PrincipalStatus.CREATED, now, now))
        identityService.updateStatus(ownerPrincipalId, PrincipalStatus.ACTIVE)

        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(ActionVocabularyEntry(DefaultEvidenceCustodian.ACCEPT_ACTION_NAME, setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT))))
        vocabulary.register(ActionVocabularyEntry(DefaultEvidenceCustodian.RETRIEVE_ACTION_NAME, setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT))))
        vocabulary.register(ActionVocabularyEntry(DefaultEvidenceCustodian.RETRIEVE_MANIFEST_ACTION_NAME, setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT))))
        vocabulary.register(ActionVocabularyEntry(gatewayAcquireAction, setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT))))

        val resourceRegistry = InMemoryResourceRegistry()
        listOf(DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID, DefaultEvidenceCustodian.EVIDENCE_RETRIEVAL_RESOURCE_ID, DefaultEvidenceCustodian.EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID, gatewayAcquireResourceId).forEach { id ->
            resourceRegistry.register(
                parker.core.interfaces.Resource(
                    resourceId = id, resourceType = ResourceType.DOCUMENT, displayName = id.value,
                    ownerPrincipalId = ownerPrincipalId, sensitivity = parker.core.interfaces.ResourceSensitivity.PUBLIC,
                    lifecycleState = parker.core.interfaces.ResourceLifecycleState.REGISTERED, createdAt = now, updatedAt = now,
                    source = "test",
                ),
            )
        }

        val authorizationPurposeRegistry = InMemoryAuthorizationPurposeRegistry()
        authorizationPurposeRegistry.register(agentGatewayPurpose)

        val rules = listOf(
            PermissionPolicyRule(PermissionAction.WRITE, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC),
            PermissionPolicyRule(PermissionAction.READ, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC),
            PermissionPolicyRule(PermissionAction.WRITE, ResourceType.DOCUMENT, PermissionDecisionOutcome.DENIED, PermissionLevel.AUTOMATIC, proposedAction = gatewayAcquireAction),
            PermissionPolicyRule(
                PermissionAction.WRITE, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC,
                authorizationPurpose = agentGatewayPurpose, proposedAction = gatewayAcquireAction,
            ),
        )
        val policy = DefaultPermissionPolicy(ActionMapper(vocabulary), resourceRegistry, rules, authorizationPurposeRegistry)
        val engine = RecordingPermissionEngine(DefaultPermissionEngine(identityService, policy))
        val evidenceCustodian = DefaultEvidenceCustodian(InMemoryEvidenceArtifactStorage(), engine, InMemoryEvidenceSourceManifestStorage())

        val workflow = registry?.let {
            val router = DeterministicEvidenceAcquisitionRouter()
            val tierARouter = TierADocumentIngestionComposition.create(
                FileSystemDerivativeGenerationStorage(Files.createTempDirectory("ag-1g-derivative-generation")),
                FileSystemDocumentIngestionAudit(tierAAuditLogFile),
            )
            val tierACoordinator = TierAOwnerInvocationCoordinator(evidenceCustodian, tierARouter)
            val executionCoordinator = GovernedAcquisitionExecutionCoordinator(
                it, router, evidenceCustodian,
                listOfNotNull(
                    TierANativeAcquisitionExecutor(
                        AcquisitionExecutorBinding(ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID, parker.core.interfaces.EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION, null),
                        tierACoordinator,
                    ),
                    externalExecutorFactory?.invoke(engine, evidenceCustodian),
                ),
            )
            GovernedAcquisitionOwnerWorkflow(hermesPrincipalId, evidenceCustodian, it, router, executionCoordinator, externalEgressAuthorised)
        }

        val projection = AgentGatewayEvidenceProjection(hermesPrincipalId, agentGatewayPurpose, engine, evidenceCustodian, workflow)
        return Environment(identityService, engine, evidenceCustodian, projection)
    }

    private suspend fun Environment.registerHermes(status: PrincipalStatus) {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        identityService.register(Principal(hermesPrincipalId, PrincipalType.EXTERNAL_AGENT, "Hermes (test)", ownerPrincipalId, PrincipalStatus.CREATED, now, now))
        if (status == PrincipalStatus.ACTIVE) identityService.updateStatus(hermesPrincipalId, PrincipalStatus.ACTIVE)
    }

    private suspend fun Environment.accept(content: ByteArray, mediaType: String, fileName: String): EvidenceArtifactId {
        val accepted = assertIs<EvidenceAcceptanceResult.Accepted>(
            evidenceCustodian.accept(ownerPrincipalId, CandidateEvidenceArtifact(content, mediaType, fileName)),
        )
        return accepted.acceptedEvidenceArtifact.evidenceArtifactId
    }

    private fun registryWithout(externalCapability: EvidenceAcquisitionCapability?) = ProductionAcquisitionCapabilityCatalogue.create(
        externalCapabilityProjection = externalCapability,
        localOcrAvailability = AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.DISABLED),
    )

    private fun availableExternalCapability() = ProductionAcquisitionCapabilityCatalogue.fidelityFirstExternalCapability().let { pending ->
        EvidenceAcquisitionCapability(
            pending.capabilityId, pending.mechanism, pending.supportedMediaTypes, pending.supportedSourceForms,
            pending.fidelity, pending.supportedRepresentations, pending.egress, pending.providerConfiguration,
            AcquisitionAvailability.Available, pending.limits, pending.fidelitySuitabilityByMediaType,
        )
    }

    // ================= F/G. Structural: no caller-supplied principal/purpose/action/resource =================

    @Test
    fun `requestAcquisition takes exactly one value parameter -- an EvidenceArtifactId -- and returns AgentGatewayAcquisitionResult`() {
        val function = AgentGatewayEvidenceProjection::class.declaredFunctions.single { it.name == "requestAcquisition" }
        val valueParameters = function.parameters.filter { it.kind == KParameter.Kind.VALUE }

        assertEquals(1, valueParameters.size, "requestAcquisition must accept no caller-supplied principal, purpose, action, resource, mode, or provider -- only the target identity")
        assertEquals(EvidenceArtifactId::class, valueParameters.single().type.classifier)
        assertEquals(AgentGatewayAcquisitionResult::class, function.returnType.classifier)
        assertTrue(function.isSuspend)
    }

    @Test
    fun `requestAcquisition constructs a request naming exactly Hermes, the gateway purpose, the acquire verb, and its own resource`() = runTest {
        val env = buildEnvironment()
        env.registerHermes(PrincipalStatus.CREATED) // request shape is checked regardless of outcome

        env.projection.requestAcquisition(EvidenceArtifactId("evidence-1"))

        val request = env.engine.requests.single()
        assertEquals(hermesPrincipalId, request.principalId)
        assertEquals(agentGatewayPurpose, request.authorizationPurpose)
        assertEquals(listOf(gatewayAcquireAction), request.proposedActions)
        assertEquals(listOf(gatewayAcquireResourceId), request.targetResources)
    }

    // ================= H. CREATED Hermes is Denied, workflow never reached =================

    @Test
    fun `a CREATED Hermes principal is Denied before any acquisition workflow is ever consulted`() = runTest {
        // No registry/workflow at all -- if the permission gate did not run first, this would
        // surface as Failed("ACQUISITION_NOT_CONFIGURED") instead of Denied, proving the gate
        // genuinely runs before the workflow is ever touched.
        val env = buildEnvironment(registry = null)
        env.registerHermes(PrincipalStatus.CREATED)

        val result = env.projection.requestAcquisition(EvidenceArtifactId("evidence-1"))

        val denied = assertIs<AgentGatewayAcquisitionResult.Denied>(result)
        assertEquals(PermissionDecisionOutcome.DENIED, denied.decision)
    }

    @Test
    fun `an ACTIVE Hermes principal with no acquisition workflow configured fails closed, distinct from Denied`() = runTest {
        val env = buildEnvironment(registry = null)
        env.registerHermes(PrincipalStatus.ACTIVE)

        val result = env.projection.requestAcquisition(EvidenceArtifactId("evidence-1"))

        assertIs<AgentGatewayAcquisitionResult.Failed>(result)
    }

    // ================= I/J/K. ACTIVE Hermes reaches the SAME real governed acquisition path =================

    @Test
    fun `NotFound -- an ACTIVE Hermes requesting acquisition for an unregistered identity gets NotFound, never Denied or an exception`() = runTest {
        val env = buildEnvironment(registry = registryWithout(null))
        env.registerHermes(PrincipalStatus.ACTIVE)

        val result = env.projection.requestAcquisition(EvidenceArtifactId("never-registered"))

        assertIs<AgentGatewayAcquisitionResult.NotFound>(result)
    }

    @Test
    fun `Failed -- an unsupported media type is routed and rejected by the real router, never silently accepted`() = runTest {
        val env = buildEnvironment(registry = registryWithout(null))
        env.registerHermes(PrincipalStatus.ACTIVE)
        val id = env.accept("arbitrary bytes".toByteArray(), "application/zip", "archive.zip")

        val result = env.projection.requestAcquisition(id)

        val failed = assertIs<AgentGatewayAcquisitionResult.Failed>(result)
        assertTrue(failed.reason.isNotBlank())
    }

    @Test
    fun `ProviderNotReady -- disablement is the entire remaining explanation, never silently completed via a disabled capability`() = runTest {
        // A registry naming only the (disabled) Local OCR capability -- no native, no external --
        // so the real router's own aggregate no-selection reason set contains exactly
        // CAPABILITY_DISABLED_OR_NOT_READY, unmixed with any unsupported-media/fidelity reason
        // from a second, independently-ineligible capability. This is the one unambiguous shape
        // this mapping reports as ProviderNotReady rather than the more generic Failed.
        val registry = GovernedAcquisitionCapabilityRegistry(
            listOf(ProductionAcquisitionCapabilityCatalogue.localOcrCapability(AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.DISABLED))),
        )
        val env = buildEnvironment(registry = registry)
        env.registerHermes(PrincipalStatus.ACTIVE)
        val id = env.accept("fake png bytes".toByteArray(), "image/png", "scan.png")

        val result = env.projection.requestAcquisition(id)

        assertIs<AgentGatewayAcquisitionResult.ProviderNotReady>(result)
    }

    @Test
    fun `M-N -- AUTHORIZATION_REQUIRED for a source that needs external egress, and Hermes has no parameter through which to grant it itself`() = runTest {
        // externalEgressAuthorised defaults to { false } -- there is no parameter on
        // requestAcquisition, or anywhere else Hermes can reach, through which this could be
        // flipped to true. Only a separate, owner-authorised action can ever change it.
        val env = buildEnvironment(registry = registryWithout(availableExternalCapability()))
        env.registerHermes(PrincipalStatus.ACTIVE)
        val id = env.accept("fake png bytes".toByteArray(), "image/png", "scan.png")

        val result = env.projection.requestAcquisition(id)

        assertIs<AgentGatewayAcquisitionResult.AuthorizationRequired>(result)
    }

    @Test
    fun `Completed -- a real CSV fixture is admitted end to end through the real, unmodified Tier A native extraction path`() = runTest {
        val env = buildEnvironment(registry = registryWithout(null))
        env.registerHermes(PrincipalStatus.ACTIVE)
        val bytes = Files.readAllBytes(java.nio.file.Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/06-structured.csv"))
        val id = env.accept(bytes, "text/csv", "06-structured.csv")

        val result = env.projection.requestAcquisition(id)

        val completed = assertIs<AgentGatewayAcquisitionResult.Completed>(result)
        assertEquals(id, completed.evidenceArtifactId)
        assertEquals(ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID, completed.capabilityId)
        assertEquals(parker.core.interfaces.EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION, completed.mechanism)
        assertTrue(completed.derivativeGenerationId.value.isNotBlank())
        // No raw bytes, path, or internal object anywhere in the result's own toString().
        assertFalse(completed.toString().contains(String(bytes)))
    }

    // ================= AG-1G FINAL SECURITY REVIEW -- end-to-end principal attribution =================
    //
    // CONFIRMED FINDING (reported, not fixed here -- see the owner-review report): source reading
    // shows ExternalTranscriptionAcquisitionExecutor.execute calls
    // `coordinator.invoke(request.authoritativeSource.evidenceArtifactId)` -- it never passes
    // `request.principalId` anywhere. ExternalTranscriptionOwnerInvocationCoordinator itself holds
    // a *constructor-fixed* `ownerPrincipalId`, used for its own internal permission check
    // (`ExternalTranscriptionInvocationGate.buildExecutionRequest(ownerPrincipalId, ...)`), its own
    // source resolution, and the durable admission/provenance write
    // (`durableAdmission.admit(..., ownerPrincipalId, ...)`). In production this constructor-fixed
    // principal is `PrincipalId(config.ownerPrincipalId)` -- the real Owner -- and the exact same
    // coordinator instance is shared, by reference, with the Hermes-scoped
    // GovernedAcquisitionOwnerWorkflow AG-1G composed. The tests below prove this precisely: Tier A
    // native acquisition genuinely threads the caller's own principal (Hermes, when Hermes-scoped)
    // all the way to its own audit record; external transcription does not -- it silently and
    // unconditionally attributes to Owner, regardless of who triggered it or what principal
    // GovernedAcquisitionExecutionCoordinator was called with.

    @Test
    fun `1 -- Tier A native acquisition triggered by ACTIVE Hermes is genuinely Hermes-attributed all the way to the durable audit record`() = runTest {
        val auditLogFile = Files.createTempDirectory("ag-1g-attribution-audit").resolve("audit.log")
        val env = buildEnvironment(registry = registryWithout(null), tierAAuditLogFile = auditLogFile)
        env.registerHermes(PrincipalStatus.ACTIVE)
        val bytes = Files.readAllBytes(java.nio.file.Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/06-structured.csv"))
        val id = env.accept(bytes, "text/csv", "06-structured.csv")

        assertIs<AgentGatewayAcquisitionResult.Completed>(env.projection.requestAcquisition(id))

        // FileSystemDocumentIngestionAudit is a write-only, append-only log with no read API
        // (by design, matching this codebase's own frozen "no listing/search" audit convention)
        // -- decode its own plain tab-separated, base64url-encoded line format directly, exactly
        // as FileSystemDocumentIngestionAudit.record itself writes it, rather than adding a new
        // capability to that frozen class.
        val lines = Files.readAllLines(auditLogFile)
        assertTrue(lines.isNotEmpty(), "Tier A must have written at least one audit line")
        lines.forEach { line ->
            val requestingPrincipalField = line.split('\t').first { it.startsWith("requestingPrincipalId=") }
            val encoded = requestingPrincipalField.removePrefix("requestingPrincipalId=")
            val decoded = String(java.util.Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
            assertEquals(hermesPrincipalId.value, decoded, "Tier A's own durable audit record must attribute this acquisition to Hermes, never Owner")
        }
    }

    @Test
    fun `2 -- CORRECTED -- external transcription's own internal permission check and durable admission now run as Hermes, never Owner, for a Hermes-triggered acquisition`() = runTest {
        val externalCapability = availableExternalCapability()
        val registry = GovernedAcquisitionCapabilityRegistry(listOf(externalCapability))
        var admissionPrincipal: PrincipalId? = null

        val env = buildEnvironment(
            registry = registry,
            externalEgressAuthorised = { true }, // a controlled fixture representing already-granted Owner egress authorisation
            externalExecutorFactory = { engine, custodian ->
                val externalCoordinator = ExternalTranscriptionOwnerInvocationCoordinator(
                    permissionEngine = engine,
                    evidenceCustodian = custodian,
                    externalMechanism = object : ExternalTranscriptionMechanism {
                        override suspend fun transcribe(request: ExternalTranscriptionRequest): ExternalTranscriptionMechanismOutcome =
                            error("provider mechanism must not be reached in this scenario -- the internal permission check denies first")
                    },
                    validator = OcrStructuredResultValidator(),
                    durableAdmission = ValidatedExternalTranscriptionAdmission { _, _, principal, _ -> admissionPrincipal = principal; error("durable admission must not be reached in this scenario") },
                )
                ExternalTranscriptionAcquisitionExecutor(
                    AcquisitionExecutorBinding(externalCapability.capabilityId, parker.core.interfaces.EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION, externalCapability.providerConfiguration?.configurationIdentity),
                    externalCoordinator,
                )
            },
        )
        env.registerHermes(PrincipalStatus.ACTIVE)
        val id = env.accept("fake png bytes representing an already-egress-authorised scan".toByteArray(), "image/png", "scan.png")
        env.engine.requests.clear()

        env.projection.requestAcquisition(id)

        val outerRequest = env.engine.requests.first { it.proposedActions == listOf(gatewayAcquireAction) }
        assertEquals(hermesPrincipalId, outerRequest.principalId, "the outer Agent-Gateway-specific gate correctly used Hermes")

        val innerExternalTranscriptionRequests = env.engine.requests.filter { it.proposedActions == listOf(ExternalTranscriptionInvocationGate.ACTION_NAME) }
        assertTrue(innerExternalTranscriptionRequests.isNotEmpty(), "external transcription's own internal permission gate must have been reached")
        innerExternalTranscriptionRequests.forEach { request ->
            // THE FIX: this inner gate is now evaluated as Hermes, the actual initiating
            // principal -- never silently substituted with Owner.
            assertEquals(hermesPrincipalId, request.principalId)
            assertNotEquals(ownerPrincipalId, request.principalId)
        }
        // This minimal test environment registers no rule for the internal external-transcription
        // verb at all, so the check above is DENIED and admission is never reached -- confirmed by
        // admissionPrincipal remaining unset, proving the "must not be reached" mechanism/admission
        // stubs above were never silently bypassed by this test's own setup.
        assertEquals(null, admissionPrincipal)
    }

    @Test
    fun `3 -- missing external egress authorisation returns AuthorizationRequired before any external provider invocation`() = runTest {
        var mechanismInvokedCount = 0
        val externalCapability = availableExternalCapability()
        val registry = GovernedAcquisitionCapabilityRegistry(listOf(externalCapability))
        // A real external executor IS wired -- proving the provider is never reached is only
        // meaningful if the executor genuinely exists and could have been invoked.
        val env = buildEnvironment(
            registry = registry,
            externalEgressAuthorised = { false },
            externalExecutorFactory = { engine, custodian ->
                val externalCoordinator = ExternalTranscriptionOwnerInvocationCoordinator(
                    permissionEngine = engine,
                    evidenceCustodian = custodian,
                    externalMechanism = object : ExternalTranscriptionMechanism {
                        override suspend fun transcribe(request: ExternalTranscriptionRequest): ExternalTranscriptionMechanismOutcome {
                            mechanismInvokedCount++
                            return ExternalTranscriptionMechanismOutcome.Failure("must not be reached")
                        }
                    },
                    validator = OcrStructuredResultValidator(),
                    durableAdmission = ValidatedExternalTranscriptionAdmission { _, _, _, _ -> error("must not be reached") },
                )
                ExternalTranscriptionAcquisitionExecutor(
                    AcquisitionExecutorBinding(externalCapability.capabilityId, parker.core.interfaces.EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION, externalCapability.providerConfiguration?.configurationIdentity),
                    externalCoordinator,
                )
            },
        )
        env.registerHermes(PrincipalStatus.ACTIVE)
        val id = env.accept("fake png bytes".toByteArray(), "image/png", "scan.png")

        val result = env.projection.requestAcquisition(id)

        assertIs<AgentGatewayAcquisitionResult.AuthorizationRequired>(result)
        assertEquals(0, mechanismInvokedCount, "the provider mechanism must never be invoked while egress authorisation is absent")
    }

    @Test
    fun `4 -- no Gateway path can reach the Owner-only external transcription authorisation writer`() {
        val forbidden = setOf("ExternalTranscriptionOwnerAuthorizationCoordinator")
        val projectionFieldTypes = AgentGatewayEvidenceProjection::class.java.declaredFields.map { it.type.simpleName }.toSet()
        val httpServerFieldTypes = parker.composition.AgentGatewayHttpServer::class.java.declaredFields.map { it.type.simpleName }.toSet()

        assertTrue(forbidden.intersect(projectionFieldTypes).isEmpty(), "forbidden types present on AgentGatewayEvidenceProjection: ${forbidden.intersect(projectionFieldTypes)}")
        assertTrue(forbidden.intersect(httpServerFieldTypes).isEmpty(), "forbidden types present on AgentGatewayHttpServer: ${forbidden.intersect(httpServerFieldTypes)}")
    }

    // ================= Structural: no direct provider/OCR/HFR/case/deletion reference =================

    @Test
    fun `AgentGatewayEvidenceProjection declares no field referencing case assignment, HFR write, deletion authority, or a provider client`() {
        val forbidden = setOf(
            "CaseAssignmentCoordinator", "FileSystemCaseAssignmentStorage", "FileSystemCaseGovernanceAudit",
            "DefaultGovernedHumanFidelityReviewRecordingService", "DefaultGovernedHumanCorrectionService",
            "TierBOcrHumanFidelityReviewCoordinator",
            "DefaultOwnerEvidenceDeletionAuthority", "OwnerEvidenceDeletionAuthority",
            "ModelInferenceClient", "OpenAiRegionTranscriptionAdapter",
        )
        val fieldTypeNames = AgentGatewayEvidenceProjection::class.java.declaredFields.map { it.type.simpleName }.toSet()

        assertTrue(forbidden.intersect(fieldTypeNames).isEmpty(), "forbidden types present: ${forbidden.intersect(fieldTypeNames)}")
    }
}
