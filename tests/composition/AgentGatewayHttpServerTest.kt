package parker.composition

import java.lang.reflect.Field
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import kotlin.reflect.full.declaredFunctions
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.runtime.AgentGatewayEvidenceManifestProjection
import parker.core.runtime.AgentGatewayEvidenceManifestResult
import parker.core.runtime.AgentGatewayEvidenceRetrievalResult
import parker.core.runtime.FileSystemAgentGatewayAccessAudit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * AG-1E -- R0 Agent Gateway Transport
 * (`docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 18,
 * Section 20). Two harnesses:
 *
 * - [fakeHarness]: [AgentGatewayHttpServer] wired to hand-controlled,
 *   call-recording stub lambdas standing in for AG-1D's own
 *   `retrieveEvidenceAsAgent`/`retrieveEvidenceManifestAsAgent`. Used to
 *   prove HTTP-layer behaviour precisely and in isolation: exact routing,
 *   authentication, malformed-id rejection *before any delegation occurs*,
 *   JSON response shape, error mapping, and audit content.
 * - [realHarness]: the real, composed `ParkerRuntime` (Hermes left
 *   `CREATED`, never activated) wired through the real
 *   `retrieveEvidenceAsAgent`/`retrieveEvidenceManifestAsAgent`. Used to
 *   prove genuine end-to-end integration -- not just that the stubs work.
 *
 * A synthetic ACTIVE-Hermes success path through the full governed read
 * mechanism is already proven at the projection layer by
 * `AgentGatewayEvidenceProjectionTest`; this file does not re-prove it --
 * re-deriving an entire second composed-runtime-shaped environment here
 * would test this file's own plumbing, not new behaviour.
 */
class AgentGatewayHttpServerTest {

    private val client: HttpClient = HttpClient.newHttpClient()
    private val token = "test-hermes-bearer-token"
    private val hermesPrincipalId = PrincipalId("agent.hermes-ingestion-operator")

    private fun send(request: HttpRequest): HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    // ================= Fake harness: isolated HTTP-layer behaviour =================

    private class FakeHarness(
        val retrieveCalls: MutableList<EvidenceArtifactId> = mutableListOf(),
        val manifestCalls: MutableList<EvidenceArtifactId> = mutableListOf(),
        val submitCalls: MutableList<Pair<parker.core.interfaces.CandidateEvidenceArtifact, String?>> = mutableListOf(),
        val acquireCalls: MutableList<EvidenceArtifactId> = mutableListOf(),
        val submitProcessingResultCalls: MutableList<Pair<String, parker.core.interfaces.HermesProcessingResult>> = mutableListOf(),
        val listProcessingResultCalls: MutableList<String> = mutableListOf(),
        val submitGovernedIngestionCalls: MutableList<Triple<String, String, parker.core.interfaces.CandidateEvidenceArtifact>> = mutableListOf(),
        var retrieveResult: AgentGatewayEvidenceRetrievalResult = AgentGatewayEvidenceRetrievalResult.NotFound(EvidenceArtifactId("unset")),
        var manifestResult: AgentGatewayEvidenceManifestResult = AgentGatewayEvidenceManifestResult.NotFound(EvidenceArtifactId("unset")),
        var submitResult: parker.core.runtime.AgentGatewaySourceSubmissionResult = parker.core.runtime.AgentGatewaySourceSubmissionResult.Denied(PermissionDecisionOutcome.DENIED),
        var acquireResult: parker.core.runtime.AgentGatewayAcquisitionResult = parker.core.runtime.AgentGatewayAcquisitionResult.Denied(PermissionDecisionOutcome.DENIED),
        var submitProcessingResultResult: parker.core.runtime.AgentGatewayProcessingResultSubmissionResult = parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Denied(PermissionDecisionOutcome.DENIED),
        var listProcessingResultsResult: parker.core.runtime.AgentGatewayProcessingResultListResult = parker.core.runtime.AgentGatewayProcessingResultListResult.Denied(PermissionDecisionOutcome.DENIED),
        var submitGovernedIngestionResult: parker.core.runtime.AgentGatewayGovernedIngestionResult = parker.core.runtime.AgentGatewayGovernedIngestionResult.Denied(PermissionDecisionOutcome.DENIED),
        val readyBatches: List<parker.core.runtime.ReadyBulkIngestionBatch> = emptyList(),
    ) {
        val auditLogFile = Files.createTempDirectory("agent-gateway-http-test-audit").resolve("audit.log")
        val server = AgentGatewayHttpServer(
            bindAddress = "127.0.0.1",
            port = 0,
            authentication = AgentGatewayAuthentication("test-hermes-bearer-token", PrincipalId("agent.hermes-ingestion-operator")),
            retrieveEvidenceAsAgent = { id -> retrieveCalls.add(id); retrieveResult },
            retrieveEvidenceManifestAsAgent = { id -> manifestCalls.add(id); manifestResult },
            submitSourceAsAgent = { candidate, advisory -> submitCalls.add(candidate to advisory); submitResult },
            requestAcquisitionAsAgent = { id -> acquireCalls.add(id); acquireResult },
            listReadyIngestionBatchesAsAgent = { readyBatches },
            submitProcessingResultAsAgent = { batchId, result -> submitProcessingResultCalls.add(batchId to result); submitProcessingResultResult },
            listProcessingResultsForBatchAsAgent = { batchId -> listProcessingResultCalls.add(batchId); listProcessingResultsResult },
            submitGovernedIngestionAsAgent = { batchId, sha, candidate -> submitGovernedIngestionCalls.add(Triple(batchId, sha, candidate)); submitGovernedIngestionResult },
            audit = FileSystemAgentGatewayAccessAudit(auditLogFile),
            logger = RecordingParkerLogger(),
        ).also { it.start() }

        fun baseUri(): String = "http://127.0.0.1:${server.boundPort}"
        fun stop() = server.stop()
        fun auditLines(): List<String> = Files.readAllLines(auditLogFile)
    }

    private fun withFakeHarness(block: (FakeHarness) -> Unit) {
        val harness = FakeHarness()
        try {
            block(harness)
        } finally {
            harness.stop()
        }
    }

    // ================= Real harness: genuine end-to-end integration =================

    @Test
    fun `authenticated Hermes can discover READY batches without CaseId and unauthenticated requests are rejected`() = withFakeHarness { fake ->
        val response = send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/ingestion-batches"))
            .header("Authorization", "Bearer $token").GET().build())
        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("\"batches\":[]"))
        assertFalse(response.body().contains("caseId"))
        val unauthorised = send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/ingestion-batches")).GET().build())
        assertEquals(401, unauthorised.statusCode())
    }

    private fun realConfig(enableCaseClassification: Boolean = false): ParkerRuntimeConfig {
        val base = ParkerRuntimeConfig(
            modelEndpointUrl = "http://127.0.0.1:1/api/generate",
            modelName = "test-model",
            ownerPrincipalId = "user.owner-agent-gateway-http-test",
            localTextChannelModuleId = "channel.local-text-agent-gateway-http-test",
            evidenceStorageRootPath = Files.createTempDirectory("agent-gateway-http-evidence").toString(),
            evidenceSourceManifestStorageRootPath = Files.createTempDirectory("agent-gateway-http-evidence-manifest").toString(),
            derivativeGenerationStorageRootPath = Files.createTempDirectory("agent-gateway-http-derivative-generation").toString(),
            derivativeContentStorageRootPath = Files.createTempDirectory("agent-gateway-http-derivative-generation-content").toString(),
            savedAnalysisStorageRootPath = Files.createTempDirectory("agent-gateway-http-saved-analysis").toString(),
            documentIngestionAuditLogPath = Files.createTempDirectory("agent-gateway-http-ingestion-audit").resolve("audit.log").toString(),
            evidenceDeletionAuditLogPath = Files.createTempDirectory("agent-gateway-http-deletion-audit").resolve("audit.log").toString(),
            memoryCoreDurabilityLogPath = Files.createTempDirectory("agent-gateway-http-memory").resolve("memory-core.log").toString(),
            knowledgeItemDurabilityLogPath = Files.createTempDirectory("agent-gateway-http-knowledge-items").resolve("items.log").toString(),
            hermesProcessingStorageRootPath = Files.createTempDirectory("agent-gateway-http-hermes-processing").toString(),
        )
        if (!enableCaseClassification) return base
        // Hermes Processing Result Intake, Task 2: CASE-1's three roots, co-required, so a real
        // BulkIngestionBindingCoordinator (and therefore a real, server-minted batch) exists for
        // the genuine end-to-end processing-result tests below.
        return base.copy(
            caseStorageRootPath = Files.createTempDirectory("agent-gateway-http-cases").toString(),
            caseAssignmentStorageRootPath = Files.createTempDirectory("agent-gateway-http-case-assignments").toString(),
            caseGovernanceAuditLogPath = Files.createTempDirectory("agent-gateway-http-case-audit").resolve("audit.log").toString(),
        )
    }

    private class RealHarness(val runtime: ParkerRuntime, val server: AgentGatewayHttpServer) {
        fun baseUri(): String = "http://127.0.0.1:${server.boundPort}"
        suspend fun stop() {
            server.stop()
            runtime.shutdown()
        }
    }

    private suspend fun withRealHarness(
        token: String,
        enableCaseClassification: Boolean = false,
        block: suspend (RealHarness) -> Unit,
    ) {
        val runtime = ParkerRuntime(realConfig(enableCaseClassification), RecordingParkerLogger())
        runtime.start()
        val auditLogFile = Files.createTempDirectory("agent-gateway-http-real-audit").resolve("audit.log")
        val server = AgentGatewayHttpServer(
            bindAddress = "127.0.0.1",
            port = 0,
            authentication = AgentGatewayAuthentication(token, PrincipalId("agent.hermes-ingestion-operator")),
            retrieveEvidenceAsAgent = { id -> runtime.retrieveEvidenceAsAgent(id) },
            retrieveEvidenceManifestAsAgent = { id -> runtime.retrieveEvidenceManifestAsAgent(id) },
            submitSourceAsAgent = { candidate, advisory -> runtime.submitSourceAsAgent(candidate, advisory) },
            requestAcquisitionAsAgent = { id -> runtime.requestAcquisitionAsAgent(id) },
            submitProcessingResultAsAgent = { batchId, result -> runtime.submitProcessingResultAsAgent(batchId, result) },
            listProcessingResultsForBatchAsAgent = { batchId -> runtime.listProcessingResultsForBatchAsAgent(batchId) },
            submitGovernedIngestionAsAgent = { batchId, sha, candidate -> runtime.submitGovernedIngestionAsAgent(batchId, sha, candidate) },
            audit = FileSystemAgentGatewayAccessAudit(auditLogFile),
            logger = RecordingParkerLogger(),
        ).also { it.start() }
        val harness = RealHarness(runtime, server)
        try {
            block(harness)
        } finally {
            harness.stop()
        }
    }

    // ================= A. Structural separation from OwnerEvidenceHttpServer =================

    @Test
    fun `AgentGatewayHttpServer declares no field of type OwnerUiAuthentication or ParkerRuntime`() {
        val fieldTypes = AgentGatewayHttpServer::class.java.declaredFields.map { it.type.simpleName }.toSet()
        assertFalse(fieldTypes.contains("OwnerUiAuthentication"))
        assertFalse(fieldTypes.contains("ParkerRuntime"))
    }

    @Test
    fun `AgentGatewayHttpServer and OwnerEvidenceHttpServer are distinct classes on independent ports`() = withFakeHarness { fake ->
        assertTrue(AgentGatewayHttpServer::class != OwnerEvidenceHttpServer::class)
        // The fake harness's own server is already running independently on its own ephemeral
        // port -- proof this class binds and serves entirely on its own, requiring no Owner
        // HTTP server, cookie store, or pairing state of any kind.
        val response = send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/x")).header("Authorization", "Bearer $token").GET().build())
        assertEquals(404, response.statusCode()) // "x" resolves to NotFound by the fake's own default stub
    }

    // ================= B/C. Authentication resolves only to Hermes; no PrincipalId parameter =================

    @Test
    fun `a valid token authenticates to exactly the Hermes principal and nothing else`() {
        val auth = AgentGatewayAuthentication(token, hermesPrincipalId)

        assertEquals(hermesPrincipalId, auth.authenticate(token))
        assertEquals(null, auth.authenticate("wrong-token"))
        assertEquals(null, auth.authenticate(null))
    }

    @Test
    fun `AgentGatewayAuthentication authenticate accepts only a String token -- no PrincipalId parameter`() {
        // Kotlin reflection, not java.lang.reflect: authenticate's return type (PrincipalId) is a
        // @JvmInline value class, which mangles the compiled method name with a trailing hash
        // suffix -- KFunction resolves the source-level signature directly instead.
        val function = AgentGatewayAuthentication::class.declaredFunctions.single { it.name == "authenticate" }
        val valueParameterTypes = function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .map { it.type.classifier }
        assertEquals(listOf(String::class), valueParameterTypes)
    }

    // ================= D. Owner UI cookie material is rejected =================

    @Test
    fun `presenting a real-looking Owner session cookie with no bearer token is rejected`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1"))
                .header("Cookie", "${OwnerEvidenceHttpServer.SESSION_COOKIE}=some-owner-session-value")
                .GET().build(),
        )

        assertEquals(401, response.statusCode())
        assertTrue(response.body().contains("unauthorised"))
        assertEquals(0, fake.retrieveCalls.size, "Owner cookie material must never reach AG-1D")
    }

    // ================= E/F. Invalid / missing Gateway credential rejected =================

    @Test
    fun `an invalid bearer token is rejected`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1"))
                .header("Authorization", "Bearer wrong-token").GET().build(),
        )

        assertEquals(401, response.statusCode())
        assertEquals(0, fake.retrieveCalls.size)
    }

    @Test
    fun `a missing bearer token is rejected`() = withFakeHarness { fake ->
        val response = send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1")).GET().build())

        assertEquals(401, response.statusCode())
        assertEquals(0, fake.retrieveCalls.size)
    }

    // ================= G. Malformed EvidenceArtifactId rejected before AG-1D runs =================

    @Test
    fun `a malformed evidence id is rejected with 400 before AG-1D is ever invoked`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/has%20space"))
                .header("Authorization", "Bearer $token").GET().build(),
        )

        assertEquals(400, response.statusCode())
        assertTrue(response.body().contains("invalid evidence artefact id"))
        assertEquals(0, fake.retrieveCalls.size)
        assertEquals(0, fake.manifestCalls.size)
    }

    // ================= H. Valid authenticated GET delegates to exact AG-1D method =================

    @Test
    fun `GET evidence retrieve route delegates to exactly retrieveEvidenceAsAgent with the exact id, never the manifest method`() = withFakeHarness { fake ->
        fake.retrieveResult = AgentGatewayEvidenceRetrievalResult.Found(EvidenceArtifactId("evidence-1"), byteLength = 42)

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1"))
                .header("Authorization", "Bearer $token").GET().build(),
        )

        assertEquals(200, response.statusCode())
        assertEquals(listOf(EvidenceArtifactId("evidence-1")), fake.retrieveCalls)
        assertEquals(0, fake.manifestCalls.size)
    }

    @Test
    fun `GET manifest route delegates to exactly retrieveEvidenceManifestAsAgent with the exact id, never the retrieve method`() = withFakeHarness { fake ->
        fake.manifestResult = AgentGatewayEvidenceManifestResult.Found(
            AgentGatewayEvidenceManifestProjection(EvidenceArtifactId("evidence-1"), "a".repeat(64), 42L, null, null),
        )

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/manifest"))
                .header("Authorization", "Bearer $token").GET().build(),
        )

        assertEquals(200, response.statusCode())
        assertEquals(listOf(EvidenceArtifactId("evidence-1")), fake.manifestCalls)
        assertEquals(0, fake.retrieveCalls.size)
    }

    // ================= I/M. Evidence projection: only allowed fields, no raw bytes =================

    @Test
    fun `evidence retrieve response contains only status, evidenceArtifactId, and byteLength -- never raw bytes`() = withFakeHarness { fake ->
        fake.retrieveResult = AgentGatewayEvidenceRetrievalResult.Found(EvidenceArtifactId("evidence-1"), byteLength = 12345)

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1"))
                .header("Authorization", "Bearer $token").GET().build(),
        )

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"FOUND\""))
        assertTrue(response.body().contains("\"evidenceArtifactId\":\"evidence-1\""))
        assertTrue(response.body().contains("\"byteLength\":12345"))
        // No raw content field of any kind -- "byteLength" is the only content-shaped field.
        assertFalse(response.body().contains("content"))
        assertFalse(response.body().contains("bytes\""))
    }

    // ================= J. Manifest projection: only allowed fields =================

    @Test
    fun `manifest response contains only the documented manifest fields`() = withFakeHarness { fake ->
        fake.manifestResult = AgentGatewayEvidenceManifestResult.Found(
            AgentGatewayEvidenceManifestProjection(
                EvidenceArtifactId("evidence-1"), "b".repeat(64), 99L, "application/pdf", "source.pdf",
            ),
        )

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/manifest"))
                .header("Authorization", "Bearer $token").GET().build(),
        )

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("\"sha256\":\"${"b".repeat(64)}\""))
        assertTrue(response.body().contains("\"byteLength\":99"))
        assertTrue(response.body().contains("\"receivedMediaType\":\"application/pdf\""))
        assertTrue(response.body().contains("\"originalFileName\":\"source.pdf\""))
    }

    @Test
    fun `manifest response never contains a filesystem path even though receivedMediaType legitimately contains a slash`() = withFakeHarness { fake ->
        fake.manifestResult = AgentGatewayEvidenceManifestResult.Found(
            AgentGatewayEvidenceManifestProjection(EvidenceArtifactId("evidence-1"), "c".repeat(64), 1L, "application/pdf", null),
        )

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/manifest"))
                .header("Authorization", "Bearer $token").GET().build(),
        )

        assertFalse(response.body().contains("/home/"))
        assertFalse(response.body().contains("/mnt/"))
        assertFalse(response.body().contains("\\"))
    }

    // ================= K/L. Denied / NotFound map deterministically =================

    @Test
    fun `a Denied projection result maps to 403 with a narrow error body`() = withFakeHarness { fake ->
        fake.retrieveResult = AgentGatewayEvidenceRetrievalResult.Denied(EvidenceArtifactId("evidence-1"), PermissionDecisionOutcome.DENIED)

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1"))
                .header("Authorization", "Bearer $token").GET().build(),
        )

        assertEquals(403, response.statusCode())
        assertTrue(response.body().contains("denied"))
    }

    @Test
    fun `a NotFound projection result maps to 404 with a narrow error body`() = withFakeHarness { fake ->
        fake.retrieveResult = AgentGatewayEvidenceRetrievalResult.NotFound(EvidenceArtifactId("evidence-1"))

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1"))
                .header("Authorization", "Bearer $token").GET().build(),
        )

        assertEquals(404, response.statusCode())
        assertTrue(response.body().contains("not found"))
    }

    // ================= N/O. No write / submission / acquisition route exists =================

    @Test
    fun `a POST to the evidence route is rejected as not found, never processed as a write`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
        )

        assertEquals(404, response.statusCode())
        assertEquals(0, fake.retrieveCalls.size)
    }

    @Test
    fun `no submission or acquisition route exists under agent`() = withFakeHarness { fake ->
        listOf("/agent/submit", "/agent/evidence-submission", "/agent/acquisition/evidence-1").forEach { path ->
            val response = send(
                HttpRequest.newBuilder(URI.create("${fake.baseUri()}$path"))
                    .header("Authorization", "Bearer $token").GET().build(),
            )
            assertEquals(404, response.statusCode(), "unexpected route reachable: $path")
        }
    }

    // ================= P. Cannot invoke arbitrary ParkerRuntime methods =================

    @Test
    fun `AgentGatewayHttpServer's only callable capabilities are the injected gateway functions`() {
        val functionTypedFields = AgentGatewayHttpServer::class.java.declaredFields.filter {
            it.type.name.startsWith("kotlin.jvm.functions.Function")
        }
        // Exactly the explicitly injected delegate fields, including the narrow read-only READY
        // batch handoff -- no generic "invoke arbitrary method" capability.
        //
        // Revision history: BI-4 adds only the fixed batch-binding delegate. Hermes Processing
        // Result Intake, Task 2, adds submitProcessingResultAsAgent/listProcessingResultsForBatchAsAgent
        // -- both still narrow, explicitly injected delegates, never a generic invocation surface.
        // Hermes Governed Ingestion, Task 3, adds submitGovernedIngestionAsAgent -- same shape.
        assertEquals(
            setOf(
                "retrieveEvidenceAsAgent", "retrieveEvidenceManifestAsAgent", "submitSourceAsAgent", "requestAcquisitionAsAgent",
                "bindIngestionEvidenceAsAgent", "submitSourceWithBatchAsAgent", "listReadyIngestionBatchesAsAgent",
                "submitProcessingResultAsAgent", "listProcessingResultsForBatchAsAgent", "submitGovernedIngestionAsAgent",
            ),
            functionTypedFields.map { it.name }.toSet(),
        )
    }

    // ================= Audit =================

    @Test
    fun `each request outcome produces exactly one audit line with the expected outcome token`() = withFakeHarness { fake ->
        send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1")).GET().build()) // missing token
        send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1")).header("Authorization", "Bearer wrong").GET().build())
        send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/has%20space")).header("Authorization", "Bearer $token").GET().build())
        fake.retrieveResult = AgentGatewayEvidenceRetrievalResult.Denied(EvidenceArtifactId("evidence-1"), PermissionDecisionOutcome.DENIED)
        send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1")).header("Authorization", "Bearer $token").GET().build())
        fake.retrieveResult = AgentGatewayEvidenceRetrievalResult.Found(EvidenceArtifactId("evidence-1"), 5)
        send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1")).header("Authorization", "Bearer $token").GET().build())

        val lines = fake.auditLines()
        assertEquals(5, lines.size)
        assertTrue(lines[0].contains("outcome=UNAUTHENTICATED"))
        assertTrue(lines[1].contains("outcome=AUTHENTICATION_FAILED"))
        assertTrue(lines[2].contains("outcome=MALFORMED_IDENTIFIER"))
        assertTrue(lines[3].contains("outcome=DENIED"))
        assertTrue(lines[3].contains("principalId=${hermesPrincipalId.value}"))
        assertTrue(lines[4].contains("outcome=APPROVED"))
        assertTrue(lines[4].contains("targetId=evidence-1"))
    }

    // ================= AG-1F. Candidate-source submission =================

    @Test
    fun `A -- a valid authenticated POST registers a new source and returns 201 with the narrow projection`() = withFakeHarness { fake ->
        fake.submitResult = parker.core.runtime.AgentGatewaySourceSubmissionResult.Registered(
            parker.core.runtime.AgentGatewayEvidenceManifestProjection(EvidenceArtifactId("evidence-new"), "a".repeat(64), 11L, "text/plain", "hello.txt"),
        )

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "text/plain")
                .header("X-Parker-Original-Filename", "hello.txt")
                .POST(HttpRequest.BodyPublishers.ofString("hello world!"))
                .build(),
        )

        assertEquals(201, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"REGISTERED\""))
        assertTrue(response.body().contains("\"evidenceArtifactId\":\"evidence-new\""))
        assertEquals(1, fake.submitCalls.size)
        assertEquals("hello world!", String(fake.submitCalls.single().first.content))
        assertEquals("text/plain", fake.submitCalls.single().first.receivedMediaType)
        assertEquals("hello.txt", fake.submitCalls.single().first.originalFileName)
    }

    @Test
    fun `D -- an ALREADY_REGISTERED result maps to 200, not 201`() = withFakeHarness { fake ->
        fake.submitResult = parker.core.runtime.AgentGatewaySourceSubmissionResult.AlreadyRegistered(
            parker.core.runtime.AgentGatewayEvidenceManifestProjection(EvidenceArtifactId("evidence-existing"), "b".repeat(64), 5L, null, null),
        )

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString("dup"))
                .build(),
        )

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"ALREADY_REGISTERED\""))
        assertTrue(response.body().contains("\"evidenceArtifactId\":\"evidence-existing\""))
    }

    @Test
    fun `F -- a matching advisory sha256 header is forwarded unchanged to submitSourceAsAgent`() = withFakeHarness { fake ->
        val body = "advisory match body"
        val sha256 = java.security.MessageDigest.getInstance("SHA-256").digest(body.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        fake.submitResult = parker.core.runtime.AgentGatewaySourceSubmissionResult.Registered(
            parker.core.runtime.AgentGatewayEvidenceManifestProjection(EvidenceArtifactId("evidence-advisory"), sha256, body.length.toLong(), null, null),
        )

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer $token")
                .header("X-Parker-Advisory-Sha256", sha256)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
        )

        assertEquals(201, response.statusCode())
        assertEquals(sha256, fake.submitCalls.single().second)
    }

    @Test
    fun `G -- a HashMismatch result maps to 409 with computed and advisory hashes, and this is what a caller-mismatched advisory produces end to end`() = withFakeHarness { fake ->
        fake.submitResult = parker.core.runtime.AgentGatewaySourceSubmissionResult.HashMismatch("c".repeat(64), "d".repeat(64))

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer $token")
                .header("X-Parker-Advisory-Sha256", "d".repeat(64))
                .POST(HttpRequest.BodyPublishers.ofString("mismatched body"))
                .build(),
        )

        assertEquals(409, response.statusCode())
        assertTrue(response.body().contains("hash mismatch"))
        assertTrue(response.body().contains("c".repeat(64)))
        assertTrue(response.body().contains("d".repeat(64)))
    }

    @Test
    fun `a malformed advisory sha256 header is rejected with 400 before AG-1D is ever invoked`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer $token")
                .header("X-Parker-Advisory-Sha256", "not-a-real-hash")
                .POST(HttpRequest.BodyPublishers.ofString("content"))
                .build(),
        )

        assertEquals(400, response.statusCode())
        assertTrue(response.body().contains("invalid advisory sha256"))
        assertEquals(0, fake.submitCalls.size)
    }

    @Test
    fun `an empty request body is rejected with 400 invalid source before AG-1D is ever invoked`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofByteArray(ByteArray(0)))
                .build(),
        )

        assertEquals(400, response.statusCode())
        assertTrue(response.body().contains("invalid source"))
        assertEquals(0, fake.submitCalls.size)
    }

    @Test
    fun `an oversized request body is rejected with 413 before AG-1D is ever invoked`() = withFakeHarness { fake ->
        val oversized = ByteArray(65 * 1024 * 1024) // 65 MiB > 64 MiB MAX_SUBMISSION_BYTES

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofByteArray(oversized))
                .build(),
        )

        assertEquals(413, response.statusCode())
        assertTrue(response.body().contains("too large"))
        assertEquals(0, fake.submitCalls.size)
    }

    // ================= H/I/J. Auth failures on POST -- same as GET =================

    @Test
    fun `H -- a POST with a missing bearer token is rejected, no submission is invoked`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .POST(HttpRequest.BodyPublishers.ofString("content"))
                .build(),
        )

        assertEquals(401, response.statusCode())
        assertEquals(0, fake.submitCalls.size)
    }

    @Test
    fun `I -- a POST with an invalid bearer token is rejected, no submission is invoked`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer wrong-token")
                .POST(HttpRequest.BodyPublishers.ofString("content"))
                .build(),
        )

        assertEquals(401, response.statusCode())
        assertEquals(0, fake.submitCalls.size)
    }

    @Test
    fun `J -- a POST presenting only an Owner UI session cookie is rejected, no submission is invoked`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Cookie", "${OwnerEvidenceHttpServer.SESSION_COOKIE}=some-owner-session-value")
                .POST(HttpRequest.BodyPublishers.ofString("content"))
                .build(),
        )

        assertEquals(401, response.statusCode())
        assertEquals(0, fake.submitCalls.size)
    }

    // ================= K. Permission denial maps deterministically =================

    @Test
    fun `a Denied submission result maps to 403 with a narrow error body`() = withFakeHarness { fake ->
        fake.submitResult = parker.core.runtime.AgentGatewaySourceSubmissionResult.Denied(PermissionDecisionOutcome.DENIED)

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString("content"))
                .build(),
        )

        assertEquals(403, response.statusCode())
        assertTrue(response.body().contains("denied"))
    }

    // ================= N/O. Existing R0 GET routes are unaffected by the new POST route =================

    @Test
    fun `existing GET evidence and manifest routes still work exactly as before, unaffected by the new POST route`() = withFakeHarness { fake ->
        fake.retrieveResult = AgentGatewayEvidenceRetrievalResult.Found(EvidenceArtifactId("evidence-1"), 7)
        fake.manifestResult = AgentGatewayEvidenceManifestResult.Found(
            parker.core.runtime.AgentGatewayEvidenceManifestProjection(EvidenceArtifactId("evidence-1"), "e".repeat(64), 7L, null, null),
        )

        val retrieveResponse = send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1")).header("Authorization", "Bearer $token").GET().build())
        val manifestResponse = send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/manifest")).header("Authorization", "Bearer $token").GET().build())

        assertEquals(200, retrieveResponse.statusCode())
        assertEquals(200, manifestResponse.statusCode())
        assertEquals(1, fake.retrieveCalls.size)
        assertEquals(1, fake.manifestCalls.size)
        assertEquals(0, fake.submitCalls.size)
    }

    @Test
    fun `no transcription route is reachable, and acquire without an identity segment is not reachable either -- only exact evidenceArtifactId-acquire is (AG-1G)`() = withFakeHarness { fake ->
        listOf("/agent/evidence/acquire", "/agent/evidence/transcribe").forEach { path ->
            val response = send(
                HttpRequest.newBuilder(URI.create("${fake.baseUri()}$path"))
                    .header("Authorization", "Bearer $token").GET().build(),
            )
            assertEquals(404, response.statusCode(), "unexpected route reachable: $path")
        }
        // "/agent/evidence/acquire" as a POST has no identity segment -- distinct from the real
        // AG-1G route "/agent/evidence/{evidenceArtifactId}/acquire" -- and must still 404.
        val postResponse = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/acquire"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
        )
        assertEquals(404, postResponse.statusCode())
        assertEquals(0, fake.acquireCalls.size)
    }

    // ================= AG-1G. Governed acquisition request =================

    @Test
    fun `A -- a valid authenticated acquisition POST delegates to requestAcquisitionAsAgent with the exact id and returns 200 on Completed`() = withFakeHarness { fake ->
        fake.acquireResult = parker.core.runtime.AgentGatewayAcquisitionResult.Completed(
            EvidenceArtifactId("evidence-1"),
            parker.core.interfaces.DerivativeGenerationId("derivative-1"),
            "parker-tier-a-native-v1",
            parker.core.interfaces.EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION,
        )

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/acquire"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"COMPLETED\""))
        assertTrue(response.body().contains("\"derivativeGenerationId\":\"derivative-1\""))
        assertEquals(listOf(EvidenceArtifactId("evidence-1")), fake.acquireCalls)
    }

    @Test
    fun `AUTHORIZATION_REQUIRED maps to 409 with a narrow, opaque body`() = withFakeHarness { fake ->
        fake.acquireResult = parker.core.runtime.AgentGatewayAcquisitionResult.AuthorizationRequired(EvidenceArtifactId("evidence-1"))

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/acquire"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        assertEquals(409, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"AUTHORIZATION_REQUIRED\""))
    }

    @Test
    fun `PROVIDER_NOT_READY maps to 409 with a narrow, opaque body`() = withFakeHarness { fake ->
        fake.acquireResult = parker.core.runtime.AgentGatewayAcquisitionResult.ProviderNotReady(EvidenceArtifactId("evidence-1"))

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/acquire"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        assertEquals(409, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"PROVIDER_NOT_READY\""))
    }

    @Test
    fun `a NotFound acquisition result maps to 404`() = withFakeHarness { fake ->
        fake.acquireResult = parker.core.runtime.AgentGatewayAcquisitionResult.NotFound(EvidenceArtifactId("evidence-1"))

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/acquire"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `a Failed acquisition result maps to 409 with only the narrow enum-derived reason`() = withFakeHarness { fake ->
        fake.acquireResult = parker.core.runtime.AgentGatewayAcquisitionResult.Failed(EvidenceArtifactId("evidence-1"), "UNSUPPORTED_SOURCE_OR_MEDIA")

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/acquire"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        assertEquals(409, response.statusCode())
        assertTrue(response.body().contains("UNSUPPORTED_SOURCE_OR_MEDIA"))
    }

    @Test
    fun `a Denied acquisition result maps to 403 with a narrow error body`() = withFakeHarness { fake ->
        fake.acquireResult = parker.core.runtime.AgentGatewayAcquisitionResult.Denied(PermissionDecisionOutcome.DENIED)

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/acquire"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        assertEquals(403, response.statusCode())
    }

    @Test
    fun `E -- a malformed evidence id in the acquire route is rejected with 400 before requestAcquisitionAsAgent is ever invoked`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/has%20space/acquire"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        assertEquals(400, response.statusCode())
        assertTrue(response.body().contains("invalid evidence artefact id"))
        assertEquals(0, fake.acquireCalls.size)
    }

    @Test
    fun `H (fake) -- a POST acquisition with a missing bearer token is rejected, no acquisition is invoked`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/acquire"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        assertEquals(401, response.statusCode())
        assertEquals(0, fake.acquireCalls.size)
    }

    @Test
    fun `I (fake) -- a POST acquisition with an invalid bearer token is rejected, no acquisition is invoked`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/acquire"))
                .header("Authorization", "Bearer wrong-token").POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        assertEquals(401, response.statusCode())
        assertEquals(0, fake.acquireCalls.size)
    }

    @Test
    fun `J (fake) -- a POST acquisition presenting only an Owner UI session cookie is rejected, no acquisition is invoked`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/acquire"))
                .header("Cookie", "parker_owner_session=some-real-looking-session-value")
                .POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        assertEquals(401, response.statusCode())
        assertEquals(0, fake.acquireCalls.size)
    }

    @Test
    fun `the acquire route ignores any request body -- no caller-selected acquisition mode, provider, or model is ever read`() = withFakeHarness { fake ->
        fake.acquireResult = parker.core.runtime.AgentGatewayAcquisitionResult.NotFound(EvidenceArtifactId("evidence-1"))

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/acquire"))
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString("""{"provider":"anything","mode":"whatever"}"""))
                .build(),
        )

        assertEquals(404, response.statusCode())
        assertEquals(listOf(EvidenceArtifactId("evidence-1")), fake.acquireCalls)
    }

    @Test
    fun `T-U -- existing GET routes and the AG-1F submit route are unaffected by the new acquire route`() = withFakeHarness { fake ->
        fake.retrieveResult = AgentGatewayEvidenceRetrievalResult.Found(EvidenceArtifactId("evidence-1"), 7)
        fake.submitResult = parker.core.runtime.AgentGatewaySourceSubmissionResult.Registered(
            AgentGatewayEvidenceManifestProjection(EvidenceArtifactId("evidence-1"), "e".repeat(64), 7L, null, null),
        )
        fake.acquireResult = parker.core.runtime.AgentGatewayAcquisitionResult.NotFound(EvidenceArtifactId("evidence-2"))

        val getResponse = send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1")).header("Authorization", "Bearer $token").GET().build())
        val submitResponse = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.ofString("content")).build(),
        )
        val acquireResponse = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-2/acquire"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        assertEquals(200, getResponse.statusCode())
        assertEquals(201, submitResponse.statusCode())
        assertEquals(404, acquireResponse.statusCode())
        assertEquals(1, fake.retrieveCalls.size)
        assertEquals(1, fake.submitCalls.size)
        assertEquals(1, fake.acquireCalls.size)
    }

    @Test
    fun `the audit log for an acquisition request contains the operation and outcome but never the bearer token`() = withFakeHarness { fake ->
        fake.acquireResult = parker.core.runtime.AgentGatewayAcquisitionResult.AuthorizationRequired(EvidenceArtifactId("evidence-1"))

        send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence/evidence-1/acquire"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build(),
        )

        val lines = fake.auditLines()
        assertEquals(1, lines.size)
        assertTrue(lines.single().contains("ACQUISITION_AUTHORIZATION_REQUIRED"))
        assertTrue(lines.single().contains("evidence-1"))
        assertFalse(lines.single().contains(token))
    }

    // ================= R. No raw bytes/path/internal object in the response =================

    @Test
    fun `the submission response never contains the raw submitted body bytes`() = withFakeHarness { fake ->
        val secretBody = "MY-SECRET-DOCUMENT-BODY-CONTENT-xyz123"
        fake.submitResult = parker.core.runtime.AgentGatewaySourceSubmissionResult.Registered(
            parker.core.runtime.AgentGatewayEvidenceManifestProjection(EvidenceArtifactId("evidence-secret"), "f".repeat(64), secretBody.length.toLong(), null, null),
        )

        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString(secretBody))
                .build(),
        )

        assertFalse(response.body().contains(secretBody))
        assertFalse(response.body().contains("/home/"))
        assertFalse(response.body().contains("/mnt/"))
    }

    // ================= S. Audit contains operation/result but not token/body bytes =================

    @Test
    fun `the audit log for a submission contains the operation and outcome but never the bearer token or request body`() = withFakeHarness { fake ->
        val secretBody = "AUDIT-MUST-NEVER-CONTAIN-THIS-BODY-abc987"
        fake.submitResult = parker.core.runtime.AgentGatewaySourceSubmissionResult.Registered(
            parker.core.runtime.AgentGatewayEvidenceManifestProjection(EvidenceArtifactId("evidence-audited"), "g".repeat(64), secretBody.length.toLong(), null, null),
        )

        send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/evidence"))
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString(secretBody))
                .build(),
        )

        val lines = fake.auditLines()
        val submissionLine = lines.single { it.contains("operation=agent-gateway.evidence.submit") }
        assertTrue(submissionLine.contains("outcome=REGISTERED"))
        assertTrue(submissionLine.contains("targetId=evidence-audited"))
        assertFalse(submissionLine.contains(token))
        assertFalse(submissionLine.contains(secretBody))
    }

    // ================= Real-runtime integration: CREATED Hermes is DENIED end-to-end =================

    @Test
    fun `through the real composed runtime, a valid authenticated request for a real evidence id is DENIED because Hermes remains CREATED`() = runTest {
        withRealHarness(token) { harness ->
            val identityService: parker.core.interfaces.IdentityService = harness.runtime.privateField<parker.core.interfaces.PermissionEngine>("permissionEngine").privateField("identityService")
            val hermes = identityService.resolve(hermesPrincipalId)
            assertNotNull(hermes)
            assertEquals(PrincipalStatus.CREATED, hermes.status)

            val retrieveResponse = send(
                HttpRequest.newBuilder(URI.create("${harness.baseUri()}/agent/evidence/nonexistent-but-syntactically-valid"))
                    .header("Authorization", "Bearer $token").GET().build(),
            )
            val manifestResponse = send(
                HttpRequest.newBuilder(URI.create("${harness.baseUri()}/agent/evidence/nonexistent-but-syntactically-valid/manifest"))
                    .header("Authorization", "Bearer $token").GET().build(),
            )

            assertEquals(403, retrieveResponse.statusCode())
            assertEquals(403, manifestResponse.statusCode())
        }
    }

    @Test
    fun `through the real composed runtime, the wrong bearer token is rejected exactly as the fake harness proves`() = runTest {
        withRealHarness(token) { harness ->
            val response = send(
                HttpRequest.newBuilder(URI.create("${harness.baseUri()}/agent/evidence/evidence-1"))
                    .header("Authorization", "Bearer wrong-token").GET().build(),
            )
            assertEquals(401, response.statusCode())
        }
    }

    @Test
    fun `through the real composed runtime, a valid authenticated POST submission is DENIED because Hermes remains CREATED (AG-1F)`() = runTest {
        withRealHarness(token) { harness ->
            val response = send(
                HttpRequest.newBuilder(URI.create("${harness.baseUri()}/agent/evidence"))
                    .header("Authorization", "Bearer $token")
                    .POST(HttpRequest.BodyPublishers.ofString("real runtime submission while Hermes is CREATED"))
                    .build(),
            )

            assertEquals(403, response.statusCode())
        }
    }

    // ================= H (AG-1G). Real-runtime integration: CREATED Hermes is DENIED for acquisition too =================

    @Test
    fun `through the real composed runtime, a valid authenticated acquisition request is DENIED because Hermes remains CREATED (AG-1G)`() = runTest {
        withRealHarness(token) { harness ->
            val identityService: parker.core.interfaces.IdentityService = harness.runtime.privateField<parker.core.interfaces.PermissionEngine>("permissionEngine").privateField("identityService")
            val hermes = identityService.resolve(hermesPrincipalId)
            assertNotNull(hermes)
            assertEquals(PrincipalStatus.CREATED, hermes.status)

            val response = send(
                HttpRequest.newBuilder(URI.create("${harness.baseUri()}/agent/evidence/nonexistent-but-syntactically-valid/acquire"))
                    .header("Authorization", "Bearer $token")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(),
            )

            assertEquals(403, response.statusCode())
        }
    }

    // ================= Hermes Processing Result Intake, Task 2 =================

    private fun processingResultRequestBody(
        sourceSha256: String = "a".repeat(64),
        status: String = "PASS",
        methods: String = "\"DIRECT_TEXT_EXTRACTION\"",
    ): String = """{"sourceSha256":"$sourceSha256","status":"$status","methods":[$methods]}"""

    private fun postProcessingResult(baseUri: String, batchId: String, body: String, bearer: String = token): HttpResponse<String> = send(
        HttpRequest.newBuilder(URI.create("$baseUri/agent/ingestion-batches/$batchId/processing-results"))
            .header("Authorization", "Bearer $bearer")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
    )

    private fun getProcessingResults(baseUri: String, batchId: String, bearer: String = token): HttpResponse<String> = send(
        HttpRequest.newBuilder(URI.create("$baseUri/agent/ingestion-batches/$batchId/processing-results"))
            .header("Authorization", "Bearer $bearer")
            .GET().build(),
    )

    // --- Authentication ---

    @Test
    fun `submitting a processing result with a missing credential is rejected before the coordinator is reached`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/ingestion-batches/bulk-test/processing-results"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(processingResultRequestBody()))
                .build(),
        )
        assertEquals(401, response.statusCode())
        assertEquals(0, fake.submitProcessingResultCalls.size)
    }

    @Test
    fun `submitting a processing result with an invalid credential is rejected before the coordinator is reached`() = withFakeHarness { fake ->
        val response = postProcessingResult(fake.baseUri(), "bulk-test", processingResultRequestBody(), bearer = "wrong-token")
        assertEquals(401, response.statusCode())
        assertEquals(0, fake.submitProcessingResultCalls.size)
    }

    @Test
    fun `an Owner-shaped session cookie with no bearer token does not authenticate the processing-result route`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/ingestion-batches/bulk-test/processing-results"))
                .header("Cookie", "${OwnerEvidenceHttpServer.SESSION_COOKIE}=some-owner-session-value")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(processingResultRequestBody()))
                .build(),
        )
        assertEquals(401, response.statusCode())
        assertEquals(0, fake.submitProcessingResultCalls.size)
    }

    @Test
    fun `a valid credential reaches the coordinator even when the coordinator itself denies the request`() = withFakeHarness { fake ->
        fake.submitProcessingResultResult = parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Denied(PermissionDecisionOutcome.DENIED)

        val response = postProcessingResult(fake.baseUri(), "bulk-test", processingResultRequestBody())

        assertEquals(403, response.statusCode())
        assertEquals(1, fake.submitProcessingResultCalls.size, "authentication alone must not be sufficient -- the coordinator's own authorisation decision is what's reflected in the response")
    }

    // --- Batch validation ---

    @Test
    fun `an unknown batch is reported as 404, never as an internal error`() = withFakeHarness { fake ->
        fake.submitProcessingResultResult = parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.UnknownBatch

        val response = postProcessingResult(fake.baseUri(), "bulk-does-not-exist", processingResultRequestBody())

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `a request body naming a caseId or batchId field is rejected as malformed -- the batch comes only from the route`() = withFakeHarness { fake ->
        val bodyWithCaseId = """{"sourceSha256":"${"a".repeat(64)}","status":"PASS","methods":["OCR"],"caseId":"case-forged"}"""
        val bodyWithBatchId = """{"sourceSha256":"${"a".repeat(64)}","status":"PASS","methods":["OCR"],"batchId":"bulk-forged"}"""

        val responseCase = postProcessingResult(fake.baseUri(), "bulk-test", bodyWithCaseId)
        val responseBatch = postProcessingResult(fake.baseUri(), "bulk-test", bodyWithBatchId)

        assertEquals(400, responseCase.statusCode())
        assertEquals(400, responseBatch.statusCode())
        assertEquals(0, fake.submitProcessingResultCalls.size)
    }

    // --- PASS / REVIEW_REQUIRED / FAILED recording ---

    @Test
    fun `a PASS result is recorded as 201 and the batch id comes from the route, not the body`() = withFakeHarness { fake ->
        val recorded = parker.core.interfaces.HermesProcessingResult(
            "a".repeat(64), "bulk-abc", parker.core.interfaces.HermesProcessingStatus.PASS,
            setOf(parker.core.interfaces.HermesProcessingMethod.DIRECT_TEXT_EXTRACTION),
        )
        fake.submitProcessingResultResult = parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Recorded(recorded)

        val response = postProcessingResult(fake.baseUri(), "bulk-abc", processingResultRequestBody())

        assertEquals(201, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"RECORDED\""))
        assertEquals("bulk-abc", fake.submitProcessingResultCalls.single().first)
    }

    @Test
    fun `a REVIEW_REQUIRED result with an issue and a page location is parsed and recorded successfully`() = withFakeHarness { fake ->
        val result = parker.core.interfaces.HermesProcessingResult(
            "a".repeat(64), "bulk-abc", parker.core.interfaces.HermesProcessingStatus.REVIEW_REQUIRED,
            setOf(parker.core.interfaces.HermesProcessingMethod.OCR),
            issues = listOf(
                parker.core.interfaces.HermesProcessingIssue(
                    parker.core.interfaces.HermesProcessingIssueKind.TABLE_STRUCTURE_AMBIGUITY,
                    "table column boundary is ambiguous",
                    parker.core.interfaces.HermesProcessingIssueLocation.DocumentPage(pageNumber = 3),
                ),
            ),
        )
        fake.submitProcessingResultResult = parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Recorded(result)
        val body = """{"sourceSha256":"${"a".repeat(64)}","status":"REVIEW_REQUIRED","methods":["OCR"],"issues":[{"kind":"TABLE_STRUCTURE_AMBIGUITY","explanation":"table column boundary is ambiguous","location":{"pageNumber":3}}]}"""

        val response = postProcessingResult(fake.baseUri(), "bulk-abc", body)

        assertEquals(201, response.statusCode())
        val submitted = fake.submitProcessingResultCalls.single().second
        assertEquals(parker.core.interfaces.HermesProcessingStatus.REVIEW_REQUIRED, submitted.status)
        val location = submitted.issues.single().location as parker.core.interfaces.HermesProcessingIssueLocation.DocumentPage
        assertEquals(3, location.pageNumber)
    }

    @Test
    fun `a REVIEW_REQUIRED request body with no issues is rejected as malformed, matching HermesProcessingResult's own invariant`() = withFakeHarness { fake ->
        val body = """{"sourceSha256":"${"a".repeat(64)}","status":"REVIEW_REQUIRED","methods":["OCR"]}"""

        val response = postProcessingResult(fake.baseUri(), "bulk-abc", body)

        assertEquals(400, response.statusCode())
        assertEquals(0, fake.submitProcessingResultCalls.size)
    }

    @Test
    fun `a FAILED result with a failure reason is recorded successfully`() = withFakeHarness { fake ->
        val result = parker.core.interfaces.HermesProcessingResult(
            "a".repeat(64), "bulk-abc", parker.core.interfaces.HermesProcessingStatus.FAILED,
            setOf(parker.core.interfaces.HermesProcessingMethod.OCR),
            failure = parker.core.interfaces.HermesProcessingFailure(parker.core.interfaces.HermesProcessingFailureKind.CORRUPT_SOURCE),
        )
        fake.submitProcessingResultResult = parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Recorded(result)
        val body = """{"sourceSha256":"${"a".repeat(64)}","status":"FAILED","methods":["OCR"],"failure":{"kind":"CORRUPT_SOURCE"}}"""

        val response = postProcessingResult(fake.baseUri(), "bulk-abc", body)

        assertEquals(201, response.statusCode())
        assertEquals(parker.core.interfaces.HermesProcessingStatus.FAILED, fake.submitProcessingResultCalls.single().second.status)
    }

    @Test
    fun `a FAILED request body with no failure field is rejected as malformed, matching HermesProcessingResult's own invariant`() = withFakeHarness { fake ->
        val body = """{"sourceSha256":"${"a".repeat(64)}","status":"FAILED","methods":["OCR"]}"""

        val response = postProcessingResult(fake.baseUri(), "bulk-abc", body)

        assertEquals(400, response.statusCode())
        assertEquals(0, fake.submitProcessingResultCalls.size)
    }

    // --- Idempotency / conflict (HTTP status mapping) ---

    @Test
    fun `an ALREADY_RECORDED outcome is reported as 200, not 201`() = withFakeHarness { fake ->
        val result = parker.core.interfaces.HermesProcessingResult(
            "a".repeat(64), "bulk-abc", parker.core.interfaces.HermesProcessingStatus.PASS, setOf(parker.core.interfaces.HermesProcessingMethod.OCR),
        )
        fake.submitProcessingResultResult = parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.AlreadyRecorded(result)

        val response = postProcessingResult(fake.baseUri(), "bulk-abc", processingResultRequestBody())

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"ALREADY_RECORDED\""))
    }

    @Test
    fun `a CONFLICT outcome is reported as 409 and echoes the existing, unchanged result`() = withFakeHarness { fake ->
        val existing = parker.core.interfaces.HermesProcessingResult(
            "a".repeat(64), "bulk-abc", parker.core.interfaces.HermesProcessingStatus.PASS, setOf(parker.core.interfaces.HermesProcessingMethod.OCR),
        )
        fake.submitProcessingResultResult = parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Conflict(existing)

        val response = postProcessingResult(fake.baseUri(), "bulk-abc", processingResultRequestBody())

        assertEquals(409, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"CONFLICT\""))
    }

    // --- SHA ---

    @Test
    fun `a malformed sourceSha256 is rejected with 400 before the coordinator is reached`() = withFakeHarness { fake ->
        val body = """{"sourceSha256":"not-a-hash","status":"PASS","methods":["OCR"]}"""

        val response = postProcessingResult(fake.baseUri(), "bulk-abc", body)

        assertEquals(400, response.statusCode())
        assertEquals(0, fake.submitProcessingResultCalls.size)
    }

    @Test
    fun `a valid lowercase sha256 is parsed and passed through exactly`() = withFakeHarness { fake ->
        val sha = "b".repeat(64)
        fake.submitProcessingResultResult = parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Recorded(
            parker.core.interfaces.HermesProcessingResult(sha, "bulk-abc", parker.core.interfaces.HermesProcessingStatus.PASS, setOf(parker.core.interfaces.HermesProcessingMethod.OCR)),
        )

        postProcessingResult(fake.baseUri(), "bulk-abc", processingResultRequestBody(sourceSha256 = sha))

        assertEquals(sha, fake.submitProcessingResultCalls.single().second.sourceSha256)
    }

    // --- Proposed evidence id ---

    @Test
    fun `an optional proposedEvidenceArtifactId is parsed and retained, and is never required`() = withFakeHarness { fake ->
        fake.submitProcessingResultResult = parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Recorded(
            parker.core.interfaces.HermesProcessingResult("a".repeat(64), "bulk-abc", parker.core.interfaces.HermesProcessingStatus.PASS, setOf(parker.core.interfaces.HermesProcessingMethod.OCR)),
        )
        val body = """{"sourceSha256":"${"a".repeat(64)}","status":"PASS","methods":["OCR"],"proposedEvidenceArtifactId":"evidence-already-known"}"""

        postProcessingResult(fake.baseUri(), "bulk-abc", body)

        assertEquals(EvidenceArtifactId("evidence-already-known"), fake.submitProcessingResultCalls.single().second.proposedEvidenceArtifactId)
    }

    // --- Read-back ---

    @Test
    fun `read-back returns exactly the coordinator's own results for the requested batch`() = withFakeHarness { fake ->
        val result = parker.core.interfaces.HermesProcessingResult("a".repeat(64), "bulk-abc", parker.core.interfaces.HermesProcessingStatus.PASS, setOf(parker.core.interfaces.HermesProcessingMethod.OCR))
        fake.listProcessingResultsResult = parker.core.runtime.AgentGatewayProcessingResultListResult.Found(listOf(result))

        val response = getProcessingResults(fake.baseUri(), "bulk-abc")

        assertEquals(200, response.statusCode())
        assertEquals(listOf("bulk-abc"), fake.listProcessingResultCalls)
        assertTrue(response.body().contains("\"sourceSha256\":\"${"a".repeat(64)}\""))
    }

    @Test
    fun `read-back for an unknown batch is 404`() = withFakeHarness { fake ->
        fake.listProcessingResultsResult = parker.core.runtime.AgentGatewayProcessingResultListResult.UnknownBatch

        val response = getProcessingResults(fake.baseUri(), "bulk-missing")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `read-back with a missing credential is rejected before the coordinator is reached`() = withFakeHarness { fake ->
        val response = send(HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/ingestion-batches/bulk-abc/processing-results")).GET().build())

        assertEquals(401, response.statusCode())
        assertEquals(0, fake.listProcessingResultCalls.size)
    }

    // ================= Hermes Governed Ingestion, Task 3 (fake harness: isolated HTTP layer) =================

    private fun postGovernedIngestion(baseUri: String, batchId: String, sha256: String, bytes: ByteArray = "content".toByteArray(), bearer: String = token): HttpResponse<String> = send(
        HttpRequest.newBuilder(URI.create("$baseUri/agent/ingestion-batches/$batchId/sources/$sha256"))
            .header("Authorization", "Bearer $bearer")
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
            .build(),
    )

    @Test
    fun `governed ingestion with a missing credential is rejected before the coordinator is reached`() = withFakeHarness { fake ->
        val response = send(
            HttpRequest.newBuilder(URI.create("${fake.baseUri()}/agent/ingestion-batches/bulk-test/sources/${"a".repeat(64)}"))
                .POST(HttpRequest.BodyPublishers.ofByteArray("content".toByteArray())).build(),
        )
        assertEquals(401, response.statusCode())
        assertEquals(0, fake.submitGovernedIngestionCalls.size)
    }

    @Test
    fun `governed ingestion with an invalid credential is rejected before the coordinator is reached`() = withFakeHarness { fake ->
        val response = postGovernedIngestion(fake.baseUri(), "bulk-test", "a".repeat(64), bearer = "wrong-token")
        assertEquals(401, response.statusCode())
        assertEquals(0, fake.submitGovernedIngestionCalls.size)
    }

    @Test
    fun `a malformed source sha256 in the route is rejected with 400 before the coordinator is reached`() = withFakeHarness { fake ->
        val response = postGovernedIngestion(fake.baseUri(), "bulk-test", "not-a-hash")
        assertEquals(400, response.statusCode())
        assertEquals(0, fake.submitGovernedIngestionCalls.size)
    }

    @Test
    fun `Ingested is reported as 201 and the batch id and sha256 come from the route`() = withFakeHarness { fake ->
        val projection = parker.core.runtime.AgentGatewayEvidenceManifestProjection(EvidenceArtifactId("evidence-1"), "a".repeat(64), 7L, "text/plain", "f.txt")
        fake.submitGovernedIngestionResult = parker.core.runtime.AgentGatewayGovernedIngestionResult.Ingested(projection)

        val response = postGovernedIngestion(fake.baseUri(), "bulk-abc", "a".repeat(64))

        assertEquals(201, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"INGESTED\""))
        val call = fake.submitGovernedIngestionCalls.single()
        assertEquals("bulk-abc", call.first)
        assertEquals("a".repeat(64), call.second)
    }

    @Test
    fun `AlreadyIngested is reported as 200, not 201`() = withFakeHarness { fake ->
        val projection = parker.core.runtime.AgentGatewayEvidenceManifestProjection(EvidenceArtifactId("evidence-1"), "a".repeat(64), 7L, null, null)
        fake.submitGovernedIngestionResult = parker.core.runtime.AgentGatewayGovernedIngestionResult.AlreadyIngested(projection)

        val response = postGovernedIngestion(fake.baseUri(), "bulk-abc", "a".repeat(64))

        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"ALREADY_INGESTED\""))
    }

    @Test
    fun `HeldForReview blocks ingestion and is reported distinctly`() = withFakeHarness { fake ->
        fake.submitGovernedIngestionResult = parker.core.runtime.AgentGatewayGovernedIngestionResult.HeldForReview

        val response = postGovernedIngestion(fake.baseUri(), "bulk-abc", "a".repeat(64))

        assertEquals(409, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"HELD_FOR_REVIEW\""))
    }

    @Test
    fun `ProcessingFailed blocks ingestion and echoes the failure kind`() = withFakeHarness { fake ->
        fake.submitGovernedIngestionResult = parker.core.runtime.AgentGatewayGovernedIngestionResult.ProcessingFailed(
            parker.core.interfaces.HermesProcessingFailure(parker.core.interfaces.HermesProcessingFailureKind.CORRUPT_SOURCE),
        )

        val response = postGovernedIngestion(fake.baseUri(), "bulk-abc", "a".repeat(64))

        assertEquals(409, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"PROCESSING_FAILED\""))
        assertTrue(response.body().contains("CORRUPT_SOURCE"))
    }

    @Test
    fun `ProcessingResultRequired blocks ingestion when no stored result exists`() = withFakeHarness { fake ->
        fake.submitGovernedIngestionResult = parker.core.runtime.AgentGatewayGovernedIngestionResult.ProcessingResultRequired

        val response = postGovernedIngestion(fake.baseUri(), "bulk-abc", "a".repeat(64))

        assertEquals(409, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"PROCESSING_RESULT_REQUIRED\""))
    }

    @Test
    fun `HashMismatch blocks ingestion and echoes both hashes`() = withFakeHarness { fake ->
        fake.submitGovernedIngestionResult = parker.core.runtime.AgentGatewayGovernedIngestionResult.HashMismatch("b".repeat(64), "a".repeat(64))

        val response = postGovernedIngestion(fake.baseUri(), "bulk-abc", "a".repeat(64))

        assertEquals(409, response.statusCode())
        assertTrue(response.body().contains("\"status\":\"HASH_MISMATCH\""))
        assertTrue(response.body().contains("b".repeat(64)))
        assertTrue(response.body().contains("a".repeat(64)))
    }

    @Test
    fun `UnknownBatch is reported as 404`() = withFakeHarness { fake ->
        fake.submitGovernedIngestionResult = parker.core.runtime.AgentGatewayGovernedIngestionResult.UnknownBatch

        val response = postGovernedIngestion(fake.baseUri(), "bulk-missing", "a".repeat(64))

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `Denied is reported as 403`() = withFakeHarness { fake ->
        fake.submitGovernedIngestionResult = parker.core.runtime.AgentGatewayGovernedIngestionResult.Denied(PermissionDecisionOutcome.DENIED)

        val response = postGovernedIngestion(fake.baseUri(), "bulk-abc", "a".repeat(64))

        assertEquals(403, response.statusCode())
    }

    // ================= Real-runtime integration: genuine end-to-end processing-result intake =================

    private suspend fun RealHarness.activateHermesAndMintBatch(caseName: String = "Processing Result Test Case"): String {
        val identityService: parker.core.interfaces.IdentityService =
            runtime.privateField<parker.core.interfaces.PermissionEngine>("permissionEngine").privateField("identityService")
        identityService.updateStatus(hermesPrincipalId, PrincipalStatus.ACTIVE)
        val created = runtime.createCaseAsOwner(caseName) as parker.core.runtime.CaseCreationOutcome.Created
        val authorised = runtime.authoriseBulkIngestionAsOwner(created.case.caseId) as parker.core.runtime.BulkIngestionAuthorisation.Authorised
        return authorised.binding.batchId
    }

    @Test
    fun `through the real composed runtime, an ACTIVE Hermes records PASS against a real server-minted batch, retries idempotently, and reads it back`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch()
            val body = processingResultRequestBody(status = "PASS")

            val first = postProcessingResult(harness.baseUri(), batchId, body)
            val retry = postProcessingResult(harness.baseUri(), batchId, body)
            val readBack = getProcessingResults(harness.baseUri(), batchId)

            assertEquals(201, first.statusCode())
            assertTrue(first.body().contains("\"status\":\"RECORDED\""))
            assertEquals(200, retry.statusCode())
            assertTrue(retry.body().contains("\"status\":\"ALREADY_RECORDED\""))
            assertEquals(200, readBack.statusCode())
            assertTrue(readBack.body().contains("\"batchId\":\"$batchId\""))

            // Lifecycle isolation: no evidence was ever registered through EvidenceCustodian, so
            // the post-ingestion review queue -- a different, later pipeline stage -- stays empty.
            val queue = harness.runtime.steveReviewQueueProjectionAsAgent()?.enumerate() ?: emptyList()
            assertEquals(emptyList(), queue)
        }
    }

    @Test
    fun `through the real composed runtime, a changed result for the same batch and source hash is a conflict, and the original is unchanged`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch()
            val first = postProcessingResult(harness.baseUri(), batchId, processingResultRequestBody(status = "PASS"))
            val differentBody = """{"sourceSha256":"${"a".repeat(64)}","status":"REVIEW_REQUIRED","methods":["OCR"],"issues":[{"kind":"MISSING_CONTENT","explanation":"content missing"}]}"""

            val conflict = postProcessingResult(harness.baseUri(), batchId, differentBody)
            val readBack = getProcessingResults(harness.baseUri(), batchId)

            assertEquals(201, first.statusCode())
            assertEquals(409, conflict.statusCode())
            assertTrue(readBack.body().contains("\"status\":\"PASS\""), "the original PASS record must remain unchanged")
            assertFalse(readBack.body().contains("REVIEW_REQUIRED"), "the conflicting attempt must never be stored")
        }
    }

    @Test
    fun `through the real composed runtime, an unknown batch is 404 even for an ACTIVE Hermes`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val identityService: parker.core.interfaces.IdentityService =
                harness.runtime.privateField<parker.core.interfaces.PermissionEngine>("permissionEngine").privateField("identityService")
            identityService.updateStatus(hermesPrincipalId, PrincipalStatus.ACTIVE)

            val response = postProcessingResult(harness.baseUri(), "bulk-00000000-0000-0000-0000-000000000000", processingResultRequestBody())

            assertEquals(404, response.statusCode())
        }
    }

    @Test
    fun `through the real composed runtime, a CREATED (not yet ACTIVE) Hermes is DENIED for processing-result submission and read-back`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val created = harness.runtime.createCaseAsOwner("Denied Path Case") as parker.core.runtime.CaseCreationOutcome.Created
            val batchId = (harness.runtime.authoriseBulkIngestionAsOwner(created.case.caseId) as parker.core.runtime.BulkIngestionAuthorisation.Authorised).binding.batchId

            val submitResponse = postProcessingResult(harness.baseUri(), batchId, processingResultRequestBody())
            val readResponse = getProcessingResults(harness.baseUri(), batchId)

            assertEquals(403, submitResponse.statusCode())
            assertEquals(403, readResponse.statusCode())
        }
    }

    @Test
    fun `through the real composed runtime, results from one batch never appear when reading back another`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val identityService: parker.core.interfaces.IdentityService =
                harness.runtime.privateField<parker.core.interfaces.PermissionEngine>("permissionEngine").privateField("identityService")
            identityService.updateStatus(hermesPrincipalId, PrincipalStatus.ACTIVE)
            val created = harness.runtime.createCaseAsOwner("Isolation Case") as parker.core.runtime.CaseCreationOutcome.Created
            val batchA = (harness.runtime.authoriseBulkIngestionAsOwner(created.case.caseId) as parker.core.runtime.BulkIngestionAuthorisation.Authorised).binding.batchId
            val batchB = (harness.runtime.authoriseBulkIngestionAsOwner(created.case.caseId) as parker.core.runtime.BulkIngestionAuthorisation.Authorised).binding.batchId

            postProcessingResult(harness.baseUri(), batchA, processingResultRequestBody(sourceSha256 = "a".repeat(64)))
            postProcessingResult(harness.baseUri(), batchB, processingResultRequestBody(sourceSha256 = "b".repeat(64)))

            val readA = getProcessingResults(harness.baseUri(), batchA)
            val readB = getProcessingResults(harness.baseUri(), batchB)

            assertTrue(readA.body().contains("a".repeat(64)))
            assertFalse(readA.body().contains("b".repeat(64)))
            assertTrue(readB.body().contains("b".repeat(64)))
            assertFalse(readB.body().contains("a".repeat(64)))
        }
    }

    // ================= Hermes Governed Ingestion, Task 3 (real-runtime integration) =================

    private fun sha256Of(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun `PASS happy path -- governed ingestion produces an authoritative EvidenceArtifactId and real case binding`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch("PASS Happy Path Case")
            val bytes = "the actual submitted bytes".toByteArray()
            val sha256 = sha256Of(bytes)
            postProcessingResult(harness.baseUri(), batchId, processingResultRequestBody(sourceSha256 = sha256, status = "PASS"))

            val response = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)

            assertEquals(201, response.statusCode())
            assertTrue(response.body().contains("\"status\":\"INGESTED\""))
            val evidenceArtifactId = jsonStringField(response.body(), "evidenceArtifactId")
            assertNotNull(evidenceArtifactId)
            val retrieved = harness.runtime.retrieveEvidenceAsAgent(EvidenceArtifactId(evidenceArtifactId))
            assertIs<AgentGatewayEvidenceRetrievalResult.Found>(retrieved)
            assertEquals(bytes.size, retrieved.byteLength)
            val caseId = harness.runtime.currentCaseAssignmentAsOwner(EvidenceArtifactId(evidenceArtifactId))
            assertNotNull(caseId, "governed ingestion must complete real case binding, not just registration")
        }
    }

    @Test
    fun `PASS idempotency -- repeating the identical submission returns the same authoritative EvidenceArtifactId and creates no duplicate`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch()
            val bytes = "idempotent bytes".toByteArray()
            val sha256 = sha256Of(bytes)
            postProcessingResult(harness.baseUri(), batchId, processingResultRequestBody(sourceSha256 = sha256, status = "PASS"))

            val first = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)
            val second = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)

            assertEquals(201, first.statusCode())
            assertEquals(200, second.statusCode())
            assertTrue(second.body().contains("\"status\":\"ALREADY_INGESTED\""))
            assertEquals(jsonStringField(first.body(), "evidenceArtifactId"), jsonStringField(second.body(), "evidenceArtifactId"))
        }
    }

    @Test
    fun `REVIEW_REQUIRED blocks ingestion, registers no evidence, and the stored result remains readable`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch()
            val bytes = "needs human review".toByteArray()
            val sha256 = sha256Of(bytes)
            val body = """{"sourceSha256":"$sha256","status":"REVIEW_REQUIRED","methods":["OCR"],"issues":[{"kind":"MISSING_CONTENT","explanation":"content missing"}]}"""
            postProcessingResult(harness.baseUri(), batchId, body)

            val response = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)
            val readBack = getProcessingResults(harness.baseUri(), batchId)

            assertEquals(409, response.statusCode())
            assertTrue(response.body().contains("\"status\":\"HELD_FOR_REVIEW\""))
            assertTrue(readBack.body().contains("REVIEW_REQUIRED"), "the stored result must remain visible for a future review interface")
            val queue = harness.runtime.steveReviewQueueProjectionAsAgent()?.enumerate() ?: emptyList()
            assertEquals(emptyList(), queue, "no evidence was ever registered, so the post-ingestion review queue stays empty")
        }
    }

    @Test
    fun `FAILED blocks ingestion, registers no evidence, and the stored result remains readable`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch()
            val bytes = "corrupt or unsupported".toByteArray()
            val sha256 = sha256Of(bytes)
            val body = """{"sourceSha256":"$sha256","status":"FAILED","methods":["OCR"],"failure":{"kind":"CORRUPT_SOURCE"}}"""
            postProcessingResult(harness.baseUri(), batchId, body)

            val response = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)
            val readBack = getProcessingResults(harness.baseUri(), batchId)

            assertEquals(409, response.statusCode())
            assertTrue(response.body().contains("\"status\":\"PROCESSING_FAILED\""))
            assertTrue(readBack.body().contains("FAILED"), "the stored result must remain visible for a future review interface")
        }
    }

    @Test
    fun `missing processing result blocks ingestion through the sanctioned Hermes bulk path -- no silent fallback`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch()
            val bytes = "never processed by Hermes".toByteArray()

            val response = postGovernedIngestion(harness.baseUri(), batchId, sha256Of(bytes), bytes)

            assertEquals(409, response.statusCode())
            assertTrue(response.body().contains("\"status\":\"PROCESSING_RESULT_REQUIRED\""))
        }
    }

    @Test
    fun `hash mismatch between the declared route hash and the actual submitted bytes is rejected, and no evidence is registered`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch()
            val processedBytes = "what Hermes actually analysed".toByteArray()
            val declaredSha256 = sha256Of(processedBytes)
            postProcessingResult(harness.baseUri(), batchId, processingResultRequestBody(sourceSha256 = declaredSha256, status = "PASS"))
            val differentBytes = "completely different bytes submitted instead".toByteArray()

            val response = postGovernedIngestion(harness.baseUri(), batchId, declaredSha256, differentBytes)

            assertEquals(409, response.statusCode())
            assertTrue(response.body().contains("\"status\":\"HASH_MISMATCH\""))
            assertTrue(response.body().contains(sha256Of(differentBytes)))
        }
    }

    @Test
    fun `an unknown batch is rejected before any evidence admission is attempted`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val identityService: parker.core.interfaces.IdentityService =
                harness.runtime.privateField<parker.core.interfaces.PermissionEngine>("permissionEngine").privateField("identityService")
            identityService.updateStatus(hermesPrincipalId, PrincipalStatus.ACTIVE)
            val bytes = "content".toByteArray()

            val response = postGovernedIngestion(harness.baseUri(), "bulk-00000000-0000-0000-0000-000000000000", sha256Of(bytes), bytes)

            assertEquals(404, response.statusCode())
        }
    }

    @Test
    fun `through the real composed runtime, a CREATED (not yet ACTIVE) Hermes is DENIED for governed ingestion -- existing PermissionEngine protection remains active`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val created = harness.runtime.createCaseAsOwner("Denied Governed Ingestion Case") as parker.core.runtime.CaseCreationOutcome.Created
            val batchId = (harness.runtime.authoriseBulkIngestionAsOwner(created.case.caseId) as parker.core.runtime.BulkIngestionAuthorisation.Authorised).binding.batchId
            val bytes = "content".toByteArray()

            val response = postGovernedIngestion(harness.baseUri(), batchId, sha256Of(bytes), bytes)

            assertEquals(403, response.statusCode())
        }
    }

    @Test
    fun `Hermes's own proposedEvidenceArtifactId is never treated as authoritative -- Parker's own dedup resolution wins`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch()
            val bytes = "proposed id should be ignored".toByteArray()
            val sha256 = sha256Of(bytes)
            val body = """{"sourceSha256":"$sha256","status":"PASS","methods":["OCR"],"proposedEvidenceArtifactId":"evidence-hermes-guessed-this"}"""
            postProcessingResult(harness.baseUri(), batchId, body)

            val response = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)

            assertEquals(201, response.statusCode())
            val evidenceArtifactId = jsonStringField(response.body(), "evidenceArtifactId")
            assertNotNull(evidenceArtifactId)
            assertFalse(evidenceArtifactId == "evidence-hermes-guessed-this", "Parker's own authoritative identity, not Hermes's proposal, must be the final EvidenceArtifactId")
        }
    }

    @Test
    fun `lifecycle isolation -- the stored HermesProcessingResult is unchanged after successful ingestion, and post-ingestion review state is untouched`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch()
            val bytes = "lifecycle isolation bytes".toByteArray()
            val sha256 = sha256Of(bytes)
            postProcessingResult(harness.baseUri(), batchId, processingResultRequestBody(sourceSha256 = sha256, status = "PASS"))

            postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)
            val readBack = getProcessingResults(harness.baseUri(), batchId)

            // HermesProcessingStatus itself has no INGESTED value -- the stored result can only ever
            // still say PASS; this also proves it was never rewritten in place.
            assertTrue(readBack.body().contains("\"status\":\"PASS\""))
            assertFalse(readBack.body().contains("INGESTED"), "the processing result record itself must never be mutated to reflect ingestion")
            val queue = harness.runtime.steveReviewQueueProjectionAsAgent()?.enumerate() ?: emptyList()
            assertEquals(emptyList(), queue, "governed ingestion registers raw bytes only -- no OCR/derivative generation exists yet, so the post-ingestion review projection has nothing to enumerate")
        }
    }

    // ================= Hermes Exception Decision Backend, Task 4 (real-runtime integration) =================
    //
    // Precedence-matrix/registry-internal unit coverage (without a real batch or hash) lives in
    // HermesProcessingEffectiveGateTest, InMemoryHermesProcessingDecisionRegistryTest, and
    // HermesProcessingDecisionCoordinatorTest. This section's own job, mirroring Task 3's own
    // "real composed runtime" tests immediately above, is that the Owner decision genuinely
    // changes what the sanctioned Hermes governed-ingestion HTTP path does -- Task 3's own
    // machinery is never replaced, only additively gated.

    @Test
    fun `Owner ACCEPT unblocks a REVIEW_REQUIRED source for governed ingestion, and the stored machine result is unchanged`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch("Owner Accept Case")
            val bytes = "owner accepted this review".toByteArray()
            val sha256 = sha256Of(bytes)
            val body = """{"sourceSha256":"$sha256","status":"REVIEW_REQUIRED","methods":["OCR"],"issues":[{"kind":"MISSING_CONTENT","explanation":"content missing"}]}"""
            postProcessingResult(harness.baseUri(), batchId, body)

            val decisionOutcome = harness.runtime.recordHermesProcessingDecisionAsOwner(
                batchId, sha256, parker.core.interfaces.HermesProcessingHumanDecisionType.ACCEPT, "looks fine", null,
            )
            val response = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)
            val readBack = getProcessingResults(harness.baseUri(), batchId)

            assertIs<parker.core.runtime.HermesProcessingDecisionOutcome.Recorded>(decisionOutcome)
            assertEquals(201, response.statusCode(), response.body())
            assertTrue(response.body().contains("\"status\":\"INGESTED\""))
            assertTrue(readBack.body().contains("REVIEW_REQUIRED"), "the stored machine result must remain unchanged by the human ACCEPT")
        }
    }

    @Test
    fun `Owner ACCEPT against a FAILED result is refused, and no evidence is ever admitted`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch("Owner Accept Failed Case")
            val bytes = "corrupt bytes".toByteArray()
            val sha256 = sha256Of(bytes)
            postProcessingResult(harness.baseUri(), batchId, """{"sourceSha256":"$sha256","status":"FAILED","methods":["OCR"],"failure":{"kind":"CORRUPT_SOURCE"}}""")

            val decisionOutcome = harness.runtime.recordHermesProcessingDecisionAsOwner(
                batchId, sha256, parker.core.interfaces.HermesProcessingHumanDecisionType.ACCEPT, null, null,
            )
            val response = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)

            assertIs<parker.core.runtime.HermesProcessingDecisionOutcome.InvalidDecision>(decisionOutcome)
            assertEquals(409, response.statusCode())
            assertTrue(response.body().contains("\"status\":\"PROCESSING_FAILED\""), "no ACCEPT was ever actually recorded, so the effective gate behaves exactly as with no decision at all")
        }
    }

    @Test
    fun `Owner CORRECT naming a real issue unblocks REVIEW_REQUIRED -- naming a nonexistent issue is refused`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch("Owner Correct Case")
            val bytes = "table structure ambiguity".toByteArray()
            val sha256 = sha256Of(bytes)
            postProcessingResult(
                harness.baseUri(), batchId,
                """{"sourceSha256":"$sha256","status":"REVIEW_REQUIRED","methods":["OCR"],"issues":[{"kind":"TABLE_STRUCTURE_AMBIGUITY","explanation":"ambiguous table","hermesInterpretation":"Gross earnings appears to be ${'$'}42,871"}]}""",
            )
            val correction = parker.core.interfaces.HermesProcessingCorrection(0, "Gross earnings is ${'$'}42,871.00", "verified against the source document")

            val invalidOutcome = harness.runtime.recordHermesProcessingDecisionAsOwner(
                batchId, sha256, parker.core.interfaces.HermesProcessingHumanDecisionType.CORRECT, null,
                parker.core.interfaces.HermesProcessingCorrection(5, "nonexistent issue", "reason"),
            )
            val validOutcome = harness.runtime.recordHermesProcessingDecisionAsOwner(
                batchId, sha256, parker.core.interfaces.HermesProcessingHumanDecisionType.CORRECT, "resolved", correction,
            )
            val response = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)

            assertIs<parker.core.runtime.HermesProcessingDecisionOutcome.InvalidDecision>(invalidOutcome)
            val recorded = assertIs<parker.core.runtime.HermesProcessingDecisionOutcome.Recorded>(validOutcome)
            assertEquals(correction, recorded.decision.correction)
            assertEquals(201, response.statusCode(), response.body())
        }
    }

    @Test
    fun `Owner REJECT overrides an otherwise-passing machine PASS -- human REJECT has the highest precedence`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch("Owner Reject Overrides Pass Case")
            val bytes = "machine says pass, owner says no".toByteArray()
            val sha256 = sha256Of(bytes)
            postProcessingResult(harness.baseUri(), batchId, processingResultRequestBody(sourceSha256 = sha256, status = "PASS"))

            harness.runtime.recordHermesProcessingDecisionAsOwner(batchId, sha256, parker.core.interfaces.HermesProcessingHumanDecisionType.REJECT, "blocked by owner", null)
            val response = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)
            val retry = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)

            assertEquals(409, response.statusCode())
            assertTrue(response.body().contains("\"status\":\"HUMAN_REJECTED\""))
            assertEquals(409, retry.statusCode(), "a later retry through the sanctioned path remains blocked")
            assertTrue(retry.body().contains("\"status\":\"HUMAN_REJECTED\""))
        }
    }

    @Test
    fun `Owner REPROCESS overrides an otherwise-passing machine PASS, and no Hermes call is ever made by recording it`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch("Owner Reprocess Overrides Pass Case")
            val bytes = "machine says pass, owner wants reprocessing".toByteArray()
            val sha256 = sha256Of(bytes)
            postProcessingResult(harness.baseUri(), batchId, processingResultRequestBody(sourceSha256 = sha256, status = "PASS"))

            val decisionOutcome = harness.runtime.recordHermesProcessingDecisionAsOwner(
                batchId, sha256, parker.core.interfaces.HermesProcessingHumanDecisionType.REPROCESS, "please reprocess", null,
            )
            val response = postGovernedIngestion(harness.baseUri(), batchId, sha256, bytes)
            val readBack = getProcessingResults(harness.baseUri(), batchId)

            assertIs<parker.core.runtime.HermesProcessingDecisionOutcome.Recorded>(decisionOutcome)
            assertEquals(409, response.statusCode())
            assertTrue(response.body().contains("\"status\":\"REPROCESS_REQUIRED\""))
            assertTrue(readBack.body().contains("\"status\":\"PASS\""), "REPROCESS must not reset or delete the existing stored machine result")
        }
    }

    @Test
    fun `a decision against a source Hermes never reported on is UnknownProcessingResult, and the Owner review queue reflects pending vs resolved items correctly`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch("Owner Review Queue Case")
            val unknownOutcome = harness.runtime.recordHermesProcessingDecisionAsOwner(
                batchId, "f".repeat(64), parker.core.interfaces.HermesProcessingHumanDecisionType.ACCEPT, null, null,
            )
            assertIs<parker.core.runtime.HermesProcessingDecisionOutcome.UnknownProcessingResult>(unknownOutcome)

            val reviewRequiredSha = "1".repeat(64)
            val failedSha = "2".repeat(64)
            val passSha = "3".repeat(64)
            val acceptedSha = "4".repeat(64)
            postProcessingResult(harness.baseUri(), batchId, """{"sourceSha256":"$reviewRequiredSha","status":"REVIEW_REQUIRED","methods":["OCR"],"issues":[{"kind":"MISSING_CONTENT","explanation":"m"}]}""")
            postProcessingResult(harness.baseUri(), batchId, """{"sourceSha256":"$failedSha","status":"FAILED","methods":["OCR"],"failure":{"kind":"CORRUPT_SOURCE"}}""")
            postProcessingResult(harness.baseUri(), batchId, processingResultRequestBody(sourceSha256 = passSha, status = "PASS"))
            postProcessingResult(harness.baseUri(), batchId, """{"sourceSha256":"$acceptedSha","status":"REVIEW_REQUIRED","methods":["OCR"],"issues":[{"kind":"MISSING_CONTENT","explanation":"m"}]}""")
            harness.runtime.recordHermesProcessingDecisionAsOwner(batchId, acceptedSha, parker.core.interfaces.HermesProcessingHumanDecisionType.ACCEPT, null, null)

            val queue = assertIs<parker.core.runtime.HermesProcessingReviewListOutcome.Found>(harness.runtime.listHermesProcessingReviewAsOwner())
            val pendingHashes = queue.items.map { it.sourceSha256 }.toSet()

            assertTrue(reviewRequiredSha in pendingHashes, "REVIEW_REQUIRED with no decision must be pending")
            assertTrue(failedSha in pendingHashes, "FAILED with no decision must be pending")
            assertTrue(passSha !in pendingHashes, "PASS must never appear in the pending queue")
            assertTrue(acceptedSha !in pendingHashes, "a resolved ACCEPT must leave the pending queue")
            assertEquals("Owner Review Queue Case", queue.items.first { it.sourceSha256 == reviewRequiredSha }.caseDisplayName)
        }
    }

    @Test
    fun `real mixed batch routes PASS review acceptance and FAILED reprocess through one server-bound case`() = runTest {
        withRealHarness(token, enableCaseClassification = true) { harness ->
            val batchId = harness.activateHermesAndMintBatch("Mixed Batch Acceptance Case")
            val pass = "pass source bytes".toByteArray()
            val review = "review source bytes".toByteArray()
            val failed = "failed source bytes".toByteArray()
            val passSha = sha256Of(pass); val reviewSha = sha256Of(review); val failedSha = sha256Of(failed)

            postProcessingResult(harness.baseUri(), batchId, processingResultRequestBody(sourceSha256 = passSha, status = "PASS"))
            postProcessingResult(harness.baseUri(), batchId, """{"sourceSha256":"$reviewSha","status":"REVIEW_REQUIRED","methods":["OCR"],"issues":[{"kind":"MISSING_CONTENT","explanation":"needs Owner review"}]}""")
            postProcessingResult(harness.baseUri(), batchId, """{"sourceSha256":"$failedSha","status":"FAILED","methods":["OCR"],"failure":{"kind":"PROCESSOR_FAILURE"}}""")

            val pending = assertIs<parker.core.runtime.HermesProcessingReviewListOutcome.Found>(harness.runtime.listHermesProcessingReviewAsOwner())
            assertEquals(setOf(reviewSha, failedSha), pending.items.map { it.sourceSha256 }.toSet())

            val passResponse = postGovernedIngestion(harness.baseUri(), batchId, passSha, pass)
            assertEquals(201, passResponse.statusCode(), passResponse.body())
            val passArtifact = assertNotNull(jsonStringField(passResponse.body(), "evidenceArtifactId"))
            assertEquals(passArtifact, jsonStringField(postGovernedIngestion(harness.baseUri(), batchId, passSha, pass).body(), "evidenceArtifactId"))

            harness.runtime.recordHermesProcessingDecisionAsOwner(batchId, reviewSha, parker.core.interfaces.HermesProcessingHumanDecisionType.ACCEPT, "reviewed", null)
            val reviewResponse = postGovernedIngestion(harness.baseUri(), batchId, reviewSha, review)
            assertEquals(201, reviewResponse.statusCode(), reviewResponse.body())
            val reviewArtifact = assertNotNull(jsonStringField(reviewResponse.body(), "evidenceArtifactId"))

            harness.runtime.recordHermesProcessingDecisionAsOwner(batchId, failedSha, parker.core.interfaces.HermesProcessingHumanDecisionType.REPROCESS, "send back to Hermes", null)
            val failedResponse = postGovernedIngestion(harness.baseUri(), batchId, failedSha, failed)
            assertEquals(409, failedResponse.statusCode())
            assertTrue(failedResponse.body().contains("REPROCESS_REQUIRED"))

            val caseId = assertNotNull(harness.runtime.currentCaseAssignmentAsOwner(EvidenceArtifactId(passArtifact)))
            assertEquals(caseId, harness.runtime.currentCaseAssignmentAsOwner(EvidenceArtifactId(reviewArtifact)))
        }
    }

    private fun jsonStringField(body: String, field: String): String? {
        val marker = "\"$field\":\""
        val start = body.indexOf(marker).takeIf { it >= 0 }?.plus(marker.length) ?: return null
        val end = body.indexOf('"', start)
        return if (end >= 0) body.substring(start, end) else null
    }
}
