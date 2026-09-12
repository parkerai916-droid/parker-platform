package parker.composition

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationTest
import parker.core.interfaces.DerivativeParentReference
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierADerivativePayloadFixtures
import parker.core.interfaces.AnalysisRequest
import parker.core.interfaces.AnalysisType
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PrincipalId
import parker.core.runtime.AgentGatewayEvidenceManifestResult
import parker.core.runtime.AgentGatewayEvidenceManifestProjection
import parker.core.runtime.AgentGatewayEvidenceRetrievalResult
import parker.core.runtime.AnalysisRequestResult
import parker.core.runtime.AnalysisRetrievalPackage
import parker.core.runtime.AnalysisRetrievedEvidence
import parker.core.runtime.FileSystemAgentGatewayAccessAudit

/** GA-3 identity, capability and credential separation at the actual Agent Gateway boundary. */
class AgentGatewayAnalysisIsolationTest {
    private val ingestionToken = "fixture-ingestion-token"
    private val analysisToken = "fixture-analysis-token"
    private val ingestionPrincipal = PrincipalId("agent.hermes-ingestion-operator")
    private val analysisPrincipal = PrincipalId("agent.hermes-analysis-operator")
    private val client = HttpClient.newHttpClient()

    @Test
    fun `credentials resolve to distinct principals and unknown credentials fail`() {
        val authentication = AgentGatewayAuthentication(
            listOf(
                AgentGatewayCredentialBinding(ingestionToken, ingestionPrincipal),
                AgentGatewayCredentialBinding(analysisToken, analysisPrincipal),
            ),
        )

        assertEquals(ingestionPrincipal, authentication.authenticate(ingestionToken))
        assertEquals(analysisPrincipal, authentication.authenticate(analysisToken))
        assertNull(authentication.authenticate("unknown"))
        assertEquals(null, authentication.authenticate(null))
    }

    @Test
    fun `analysis credential can retrieve through the narrow route but cannot submit evidence`() {
        val audit = Files.createTempDirectory("analysis-agent-gateway-audit").resolve("audit.log")
        val retrieved = mutableListOf<EvidenceArtifactId>()
        val server = AgentGatewayHttpServer(
            bindAddress = "127.0.0.1",
            port = 0,
            authentication = AgentGatewayAuthentication(
                listOf(
                    AgentGatewayCredentialBinding(ingestionToken, ingestionPrincipal),
                    AgentGatewayCredentialBinding(analysisToken, analysisPrincipal),
                ),
            ),
            ingestionPrincipalId = ingestionPrincipal,
            analysisPrincipalId = analysisPrincipal,
            retrieveEvidenceAsAgent = { AgentGatewayEvidenceRetrievalResult.Denied(it, PermissionDecisionOutcome.DENIED) },
            retrieveEvidenceManifestAsAgent = { AgentGatewayEvidenceManifestResult.Denied(it, PermissionDecisionOutcome.DENIED) },
            retrieveEvidenceAsAnalysisAgent = {
                retrieved += it
                AgentGatewayEvidenceRetrievalResult.Found(it, 17)
            },
            retrieveEvidenceManifestAsAnalysisAgent = { AgentGatewayEvidenceManifestResult.NotFound(it) },
            submitSourceAsAgent = { _, _ -> error("analysis request must not reach ingestion delegate") },
            requestAcquisitionAsAgent = { error("analysis request must not reach acquisition delegate") },
            audit = FileSystemAgentGatewayAccessAudit(audit),
            logger = RecordingParkerLogger(),
        ).also { it.start() }

        try {
            val base = URI.create("http://127.0.0.1:${server.boundPort}")
            val read = client.send(
                HttpRequest.newBuilder(base.resolve("/agent/evidence/evidence-1"))
                    .header("Authorization", "Bearer $analysisToken")
                    .GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(200, read.statusCode())
            assertTrue(read.body().contains("\"status\":\"FOUND\""))
            assertEquals(listOf(EvidenceArtifactId("evidence-1")), retrieved)

            val write = client.send(
                HttpRequest.newBuilder(base.resolve("/agent/evidence"))
                    .header("Authorization", "Bearer $analysisToken")
                    .POST(HttpRequest.BodyPublishers.ofString("not-governed"))
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(403, write.statusCode())
        } finally {
            server.stop()
        }
    }

    @Test
    fun `analysis request is text-only, scope-bounded, server-identified, and ingestion identity is rejected`() {
        val audit = Files.createTempDirectory("analysis-request-gateway-audit").resolve("audit.log")
        var received: AnalysisRequest? = null
        val server = AgentGatewayHttpServer(
            bindAddress = "127.0.0.1",
            port = 0,
            authentication = AgentGatewayAuthentication(
                listOf(
                    AgentGatewayCredentialBinding(ingestionToken, ingestionPrincipal),
                    AgentGatewayCredentialBinding(analysisToken, analysisPrincipal),
                ),
            ),
            ingestionPrincipalId = ingestionPrincipal,
            analysisPrincipalId = analysisPrincipal,
            retrieveEvidenceAsAgent = { AgentGatewayEvidenceRetrievalResult.Denied(it, PermissionDecisionOutcome.DENIED) },
            retrieveEvidenceManifestAsAgent = { AgentGatewayEvidenceManifestResult.Denied(it, PermissionDecisionOutcome.DENIED) },
            submitSourceAsAgent = { _, _ -> error("analysis request must not reach ingestion delegate") },
            requestAcquisitionAsAgent = { error("analysis request must not reach acquisition delegate") },
            submitAnalysisRequestAsAgent = { request ->
                received = request
                val id = request.scope.evidenceArtifactIds.single()
                val generationId = request.scope.derivativeGenerationIds.getValue(id.value)
                val record = DerivativeGenerationTest.record(generationId.value).copy(
                    rootSourceEvidenceArtifactId = id,
                    parents = listOf(DerivativeParentReference.RootEvidenceArtifact(id)),
                )
                AnalysisRequestResult.Accepted(
                    AnalysisRetrievalPackage(
                        request.requestId,
                        request.question,
                        request.analysisType,
                        request.scope,
                        listOf(
                            AnalysisRetrievedEvidence(
                                id,
                                AgentGatewayEvidenceManifestProjection(id, "a".repeat(64), 4, "text/plain", "controlled.txt"),
                                parker.core.runtime.AnalysisGovernedContent(
                                    generationId,
                                    record,
                                    TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()),
                                ),
                            ),
                        ),
                    ),
                )
            },
            audit = FileSystemAgentGatewayAccessAudit(audit),
            logger = RecordingParkerLogger(),
        ).also { it.start() }

        try {
            val base = URI.create("http://127.0.0.1:${server.boundPort}")
            val body = """{"question":"Identify the controlled test issue","analysisType":"ISSUE_ANALYSIS","scope":{"evidenceArtifactIds":["evidence-1"],"derivativeGenerationIds":{"evidence-1":"generation-http"}}}"""
            val response = client.send(
                HttpRequest.newBuilder(base.resolve("/agent/analysis"))
                    .header("Authorization", "Bearer $analysisToken")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(200, response.statusCode())
            assertTrue(response.body().contains("\"status\":\"ACCEPTED\""))
            assertTrue(response.body().contains("\"analysisType\":\"ISSUE_ANALYSIS\""))
            assertTrue(received!!.requestId.value.startsWith("analysis-"))
            assertEquals("Identify the controlled test issue", received!!.question)
            assertEquals(AnalysisType.ISSUE_ANALYSIS, received!!.analysisType)
            assertEquals(listOf(EvidenceArtifactId("evidence-1")), received!!.scope.evidenceArtifactIds)
            assertTrue(response.body().contains("\"governedContent\""))
            assertTrue(response.body().contains("\"headers\""))
            assertTrue(!response.body().contains("/tmp/"))

            val ingestionResponse = client.send(
                HttpRequest.newBuilder(base.resolve("/agent/analysis"))
                    .header("Authorization", "Bearer $ingestionToken")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(403, ingestionResponse.statusCode())

            val malformed = client.send(
                HttpRequest.newBuilder(base.resolve("/agent/analysis"))
                    .header("Authorization", "Bearer $analysisToken")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"question\":\"\",\"analysisType\":\"ISSUE_ANALYSIS\",\"scope\":{\"evidenceArtifactIds\":[\"evidence-1\"]}}"))
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(400, malformed.statusCode())
        } finally {
            server.stop()
        }
    }
}
