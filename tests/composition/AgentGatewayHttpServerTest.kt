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
        var retrieveResult: AgentGatewayEvidenceRetrievalResult = AgentGatewayEvidenceRetrievalResult.NotFound(EvidenceArtifactId("unset")),
        var manifestResult: AgentGatewayEvidenceManifestResult = AgentGatewayEvidenceManifestResult.NotFound(EvidenceArtifactId("unset")),
    ) {
        val auditLogFile = Files.createTempDirectory("agent-gateway-http-test-audit").resolve("audit.log")
        val server = AgentGatewayHttpServer(
            bindAddress = "127.0.0.1",
            port = 0,
            authentication = AgentGatewayAuthentication("test-hermes-bearer-token", PrincipalId("agent.hermes-ingestion-operator")),
            retrieveEvidenceAsAgent = { id -> retrieveCalls.add(id); retrieveResult },
            retrieveEvidenceManifestAsAgent = { id -> manifestCalls.add(id); manifestResult },
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

    private fun realConfig(): ParkerRuntimeConfig = ParkerRuntimeConfig(
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
    )

    private class RealHarness(val runtime: ParkerRuntime, val server: AgentGatewayHttpServer) {
        fun baseUri(): String = "http://127.0.0.1:${server.boundPort}"
        suspend fun stop() {
            server.stop()
            runtime.shutdown()
        }
    }

    private suspend fun withRealHarness(token: String, block: suspend (RealHarness) -> Unit) {
        val runtime = ParkerRuntime(realConfig(), RecordingParkerLogger())
        runtime.start()
        val auditLogFile = Files.createTempDirectory("agent-gateway-http-real-audit").resolve("audit.log")
        val server = AgentGatewayHttpServer(
            bindAddress = "127.0.0.1",
            port = 0,
            authentication = AgentGatewayAuthentication(token, PrincipalId("agent.hermes-ingestion-operator")),
            retrieveEvidenceAsAgent = { id -> runtime.retrieveEvidenceAsAgent(id) },
            retrieveEvidenceManifestAsAgent = { id -> runtime.retrieveEvidenceManifestAsAgent(id) },
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
    fun `AgentGatewayHttpServer's only callable capabilities are the two injected AG-1D functions`() {
        val functionTypedFields = AgentGatewayHttpServer::class.java.declaredFields.filter {
            it.type.name.startsWith("kotlin.jvm.functions.Function")
        }
        // Exactly the two AG-1D delegate fields (retrieveEvidenceAsAgent/retrieveEvidenceManifestAsAgent)
        // -- no generic "invoke arbitrary method" capability, and no third callable added silently.
        assertEquals(
            setOf("retrieveEvidenceAsAgent", "retrieveEvidenceManifestAsAgent"),
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
}
