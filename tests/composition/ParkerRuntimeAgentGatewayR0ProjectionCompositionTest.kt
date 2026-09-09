package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import java.time.Instant
import kotlin.reflect.full.declaredFunctions
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.IdentityService
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.runtime.AgentGatewayEvidenceManifestResult
import parker.core.runtime.AgentGatewayEvidenceProjection
import parker.core.runtime.AgentGatewayEvidenceRetrievalResult
import parker.core.runtime.EvidenceRegistrationOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import parker.core.runtime.BulkIngestionAuthorisation
import parker.core.runtime.CaseCreationOutcome

/**
 * AG-1D -- R0 Governed Runtime Projections
 * (`docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 7 item 2,
 * Section 20). Proves the unit's own acceptance criteria against the real,
 * composed [ParkerRuntime] -- real [DefaultPermissionPolicy], real
 * [DefaultEvidenceCustodian], real (`CREATED`) Hermes principal:
 *
 * A. `retrieveEvidenceAsAgent`/`retrieveEvidenceManifestAsAgent` accept no
 *    caller-supplied principal/purpose/action/resource -- structurally,
 *    mirroring [ParkerRuntimeEvidenceCustodianIntegrationTest]'s own
 *    established `deleteEvidenceAsOwner` reflection precedent exactly.
 * B/C/D. The composed [AgentGatewayEvidenceProjection] is fixed, at
 *    construction, to Hermes's own `PrincipalId` and the Agent Gateway's
 *    own `AuthorizationPurposeId` -- proven here by inspecting the
 *    composed instance itself, not a copy.
 * E. The real, composed Hermes principal remains `CREATED`; both AG-1D
 *    entry points therefore resolve `Denied`.
 * N. Owner's existing `retrieveEvidence`/`retrieveEvidenceManifest`-backed
 *    reads are unaffected.
 * O. `EvidenceCustodian`'s own accept/retrieve/retrieveManifest behaviour
 *    is unaffected -- proven via the real `submitEvidence` -> owner
 *    `retrieveEvidence` round trip this suite exercises.
 *
 * Unit-level coverage of [AgentGatewayEvidenceProjection]'s own logic
 * (exact request shape, a synthetic ACTIVE Hermes reaching the read,
 * unknown-id fail-closed behaviour, narrow-DTO shape, no-mutation-method
 * shape) lives in `AgentGatewayEvidenceProjectionTest`
 * (`tests/runtime/`) -- this file does not re-prove it.
 */
class ParkerRuntimeAgentGatewayR0ProjectionCompositionTest {

    private val ownerPrincipalId = "user.owner-agent-gateway-projection-test"
    private val hermesPrincipalId = PrincipalId("agent.hermes-ingestion-operator")
    private val agentGatewayPurpose = AuthorizationPurposeId("agent-gateway.hermes-ingestion")

    private fun config(caseClassificationConfigured: Boolean = false, agentGatewayHermesActive: Boolean = false): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted by these tests
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-agent-gateway-projection-test",
        evidenceStorageRootPath = Files.createTempDirectory("agent-gateway-projection-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("agent-gateway-projection-evidence-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("agent-gateway-projection-derivative-generation").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("agent-gateway-projection-derivative-generation-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("agent-gateway-projection-saved-analysis").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("agent-gateway-projection-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("agent-gateway-projection-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("agent-gateway-projection-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("agent-gateway-projection-knowledge-items").resolve("items.log").toString(),
        caseStorageRootPath = if (caseClassificationConfigured) Files.createTempDirectory("agent-gateway-projection-cases").toString() else null,
        caseAssignmentStorageRootPath = if (caseClassificationConfigured) Files.createTempDirectory("agent-gateway-projection-case-assignments").toString() else null,
        caseGovernanceAuditLogPath = if (caseClassificationConfigured) Files.createTempDirectory("agent-gateway-projection-case-audit").resolve("audit.log").toString() else null,
        agentGatewayHermesActive = agentGatewayHermesActive,
    )

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "agent-gateway-projection-test-source",
        sourceType = "test",
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    private fun composedEngine(runtime: ParkerRuntime): PermissionEngine = runtime.privateField("permissionEngine")
    private fun composedIdentityService(runtime: ParkerRuntime): IdentityService = composedEngine(runtime).privateField("identityService")
    private fun composedProjection(runtime: ParkerRuntime): AgentGatewayEvidenceProjection = runtime.privateField("agentGatewayEvidenceProjection")

    private suspend fun submitAsOwner(runtime: ParkerRuntime, content: ByteArray): EvidenceArtifactId {
        val outcome = runtime.submitEvidence(
            requestingPrincipalId = PrincipalId(ownerPrincipalId),
            candidateEvidenceArtifact = CandidateEvidenceArtifact(content),
            candidateProvenance = candidateProvenance(),
            documentType = "agent-gateway-projection-test-document",
        )
        return assertIs<EvidenceRegistrationOutcome.Registered>(outcome).acceptedEvidenceArtifact.evidenceArtifactId
    }

    // ================= A. No caller-supplied principal/purpose/action/resource parameter =================

    @Test
    fun `retrieveEvidenceAsAgent declares exactly one parameter -- the target EvidenceArtifactId -- no principal, purpose, action, or resource parameter`() {
        val function = ParkerRuntime::class.declaredFunctions.single { it.name == "retrieveEvidenceAsAgent" }
        val valueParameterTypes = function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .map { it.type.classifier }

        assertEquals(listOf(EvidenceArtifactId::class), valueParameterTypes)
    }

    @Test
    fun `retrieveEvidenceManifestAsAgent declares exactly one parameter -- the target EvidenceArtifactId -- no principal, purpose, action, or resource parameter`() {
        val function = ParkerRuntime::class.declaredFunctions.single { it.name == "retrieveEvidenceManifestAsAgent" }
        val valueParameterTypes = function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .map { it.type.classifier }

        assertEquals(listOf(EvidenceArtifactId::class), valueParameterTypes)
    }

    @Test
    fun `submitSourceAsAgent declares only a CandidateEvidenceArtifact and an advisory hash -- no principal, purpose, action, or resource parameter (AG-1F)`() {
        val function = ParkerRuntime::class.declaredFunctions.single { it.name == "submitSourceAsAgent" }
        val valueParameterTypes = function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .map { it.type.classifier }

        assertEquals(listOf(CandidateEvidenceArtifact::class, String::class, String::class), valueParameterTypes)
    }

    // ================= B/C. Composed projection is fixed to Hermes's principal and the gateway purpose =================

    @Test
    fun `the composed AgentGatewayEvidenceProjection is fixed to Hermes's own PrincipalId and the Agent Gateway's own AuthorizationPurposeId`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val projection = composedProjection(runtime)
        // `PrincipalId`/`AuthorizationPurposeId` are @JvmInline value classes -- a non-nullable
        // value-class-typed field is stored unboxed (as the raw String) at the JVM level, so
        // reflection reads the underlying String and this test re-wraps it, rather than casting
        // the raw field value directly to the value-class type (which throws ClassCastException).
        val boundPrincipalId = PrincipalId(projection.privateField<String>("hermesPrincipalId"))
        val boundPurpose = AuthorizationPurposeId(projection.privateField<String>("agentGatewayPurpose"))

        assertEquals(hermesPrincipalId, boundPrincipalId)
        assertEquals(agentGatewayPurpose, boundPurpose)

        runtime.shutdown()
    }

    @Test
    fun `configured projection shares the runtime bulk coordinator and accepts a valid Owner-authorised batch submission`() = runTest {
        val runtime = ParkerRuntime(config(caseClassificationConfigured = true, agentGatewayHermesActive = true), RecordingParkerLogger())
        runtime.start()

        val case = assertIs<CaseCreationOutcome.Created>(runtime.createCaseAsOwner("batch case")).case
        val authorisation = assertIs<BulkIngestionAuthorisation.Authorised>(runtime.authoriseBulkIngestionAsOwner(case.caseId))
        val projection = composedProjection(runtime)
        val projectionCoordinator = projection.privateField<Any>("bulkIngestionBindingCoordinator")
        val runtimeCoordinator = runtime.privateField<Any>("bulkIngestionBindingCoordinator")
        assertSame(runtimeCoordinator, projectionCoordinator)

        val submitted = runtime.submitSourceAsAgent(
            CandidateEvidenceArtifact("valid batch submission".toByteArray()),
            null,
            authorisation.binding.batchId,
        )
        assertIs<parker.core.runtime.AgentGatewaySourceSubmissionResult.Registered>(submitted)

        runtime.shutdown()
    }

    @Test
    fun `the composed AgentGatewayEvidenceProjection shares the same PermissionEngine and EvidenceCustodian instances the rest of the runtime uses -- no second evaluator, no duplicated custodian`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val projection = composedProjection(runtime)
        val boundEngine = projection.privateField<PermissionEngine>("permissionEngine")
        val boundCustodian = projection.privateField<EvidenceCustodian>("evidenceCustodian")
        val runtimeEngine: PermissionEngine = runtime.privateField("permissionEngine")
        val runtimeCustodian: EvidenceCustodian = runtime.privateField("evidenceCustodian")

        assertEquals(runtimeEngine, boundEngine)
        assertEquals(runtimeCustodian, boundCustodian)

        runtime.shutdown()
    }

    // ================= E. Real, composed (CREATED) Hermes is DENIED =================

    @Test
    fun `Hermes remains CREATED in the composed runtime, and both AG-1D entry points resolve Denied`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val hermes = composedIdentityService(runtime).resolve(hermesPrincipalId)
        assertNotNull(hermes)
        assertEquals(PrincipalStatus.CREATED, hermes.status)

        val evidenceArtifactId = submitAsOwner(runtime, "agent gateway r0 projection test content".toByteArray())

        val retrieveResult = runtime.retrieveEvidenceAsAgent(evidenceArtifactId)
        val manifestResult = runtime.retrieveEvidenceManifestAsAgent(evidenceArtifactId)

        val deniedRetrieve = assertIs<AgentGatewayEvidenceRetrievalResult.Denied>(retrieveResult)
        assertEquals(PermissionDecisionOutcome.DENIED, deniedRetrieve.decision)
        val deniedManifest = assertIs<AgentGatewayEvidenceManifestResult.Denied>(manifestResult)
        assertEquals(PermissionDecisionOutcome.DENIED, deniedManifest.decision)

        runtime.shutdown()
    }

    @Test
    fun `submitSourceAsAgent also resolves Denied while Hermes remains CREATED -- no source is registered (AG-1F)`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val result = runtime.submitSourceAsAgent(CandidateEvidenceArtifact("agent gateway r1 submission test content".toByteArray()), null)

        val denied = assertIs<parker.core.runtime.AgentGatewaySourceSubmissionResult.Denied>(result)
        assertEquals(PermissionDecisionOutcome.DENIED, denied.decision)

        runtime.shutdown()
    }

    @Test
    fun `AG-1D denial holds even for an evidenceArtifactId that genuinely exists and Owner can read`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val evidenceArtifactId = submitAsOwner(runtime, "exists for owner, denied for Hermes".toByteArray())

        // Control: Owner's own existing read still succeeds against the identical id (item N).
        val ownerResult = runtime.retrieveEvidence(PrincipalId(ownerPrincipalId), evidenceArtifactId)
        assertIs<EvidenceRetrievalResult.Found>(ownerResult)

        // The AG-1D path denies Hermes regardless -- not because the id is unknown.
        val agentResult = runtime.retrieveEvidenceAsAgent(evidenceArtifactId)
        assertIs<AgentGatewayEvidenceRetrievalResult.Denied>(agentResult)

        runtime.shutdown()
    }

    // ================= N/O. Existing Owner read and EvidenceCustodian behaviour unchanged =================

    @Test
    fun `submitEvidence then Owner retrieveEvidence and retrieveEvidence-manifest-backed reads still round-trip exactly, unaffected by AG-1D`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val content = "owner round trip unaffected by AG-1D".toByteArray()
        val evidenceArtifactId = submitAsOwner(runtime, content)

        val retrieveResult = assertIs<EvidenceRetrievalResult.Found>(runtime.retrieveEvidence(PrincipalId(ownerPrincipalId), evidenceArtifactId))
        assertEquals(evidenceArtifactId, retrieveResult.evidenceArtifactId)
        assertEquals(content.toList(), retrieveResult.content.toList())

        runtime.shutdown()
    }
}
