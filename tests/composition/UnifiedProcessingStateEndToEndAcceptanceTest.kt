package parker.composition

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.core.interfaces.CaseId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.runtime.BulkIngestionAuthorisation
import parker.core.runtime.EvidenceProcessingState
import parker.core.runtime.FileSystemEvidenceProcessingStateStore

/**
 * Dedicated unified-state harness. It uses the real Parker composition graph and both real HTTP
 * transports, while every store is created under a temporary directory. The external provider
 * leg is deliberately not faked: the provider-specific test remains opt-in and is skipped when
 * the repository's isolated live-acceptance prerequisites are not provisioned.
 */
class UnifiedProcessingStateEndToEndAcceptanceTest {
    private val client = HttpClient.newHttpClient()
    private val fixtureRoot = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures")
    private val ownerToken = "unified-state-owner-test-token"
    private val agentToken = "unified-state-agent-test-token"

    private data class Harness(val runtime: ParkerRuntime, val owner: OwnerEvidenceHttpServer, val agent: AgentGatewayHttpServer, val config: ParkerRuntimeConfig, val caseId: CaseId, val ownerSession: String)

    private data class FixtureResult(val evidenceId: EvidenceArtifactId, val state: EvidenceProcessingState, val sourceSha256: String, val batchId: String)

    private fun config(): ParkerRuntimeConfig {
        fun dir(name: String) = Files.createTempDirectory("unified-state-$name").toString()
        val environment = System.getenv()
        return ParkerRuntimeConfig(
            modelEndpointUrl = "http://127.0.0.1:1/api/generate",
            modelName = "isolated-test-model",
            ownerPrincipalId = "owner-unified-state-acceptance",
            evidenceStorageRootPath = dir("evidence"),
            evidenceDeletionAuditLogPath = Path.of(dir("evidence-audit"), "audit.log").toString(),
            evidenceSourceManifestStorageRootPath = dir("manifests"),
            derivativeGenerationStorageRootPath = dir("generations"),
            derivativeContentStorageRootPath = dir("content"),
            savedAnalysisStorageRootPath = dir("saved-analysis"),
            documentIngestionAuditLogPath = Path.of(dir("ingestion-audit"), "audit.log").toString(),
            memoryCoreDurabilityLogPath = Path.of(dir("memory"), "memory.log").toString(),
            knowledgeItemDurabilityLogPath = Path.of(dir("knowledge"), "items.log").toString(),
            caseStorageRootPath = dir("cases"),
            caseAssignmentStorageRootPath = dir("assignments"),
            caseGovernanceAuditLogPath = Path.of(dir("case-audit"), "audit.log").toString(),
            hermesProcessingStorageRootPath = dir("hermes-processing"),
            agentGatewayHermesActive = true,
            productionLocalOcrEligible = false,
            // Reuse the same process-environment injection as the detached live acceptance.
            // The focused test supplies the host-mounted profile path at invocation time; no
            // credential or provider configuration is copied into this source file.
            openAiExternalTranscriptionEnabled = environment["PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED"] == "true",
            openAiExternalTranscriptionProviderProfilePath = environment["PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_PROVIDER_PROFILE_PATH"],
            openAiApiCredential = parker.composition.OpenAiApiCredential.fromEnvironment(environment["PARKER_OPENAI_API_KEY"]),
            fidelityFirstAttemptStorageRootPath = dir("external-attempts"),
        )
    }

    private fun startHarness(): Harness {
        val config = config()
        val runtime = ParkerRuntime(
            config,
            ConsoleParkerLogger("unified-state-acceptance", LogLevel.ERROR),
            buildIdentity = { System.getenv("PARKER_BUILD_COMMIT") },
        )
        runBlocking { runtime.start() }
        val caseId = runBlocking {
            assertIs<parker.core.runtime.CaseCreationOutcome.Created>(runtime.createCaseAsOwner("Unified State Acceptance")).case.caseId
        }
        val ownerAuth = OwnerUiAuthentication(Files.createTempDirectory("unified-state-owner-auth"), parker.core.interfaces.PrincipalId("owner-${"2".repeat(64)}"))
        val pairing = requireNotNull(ownerAuth.pair(ownerAuth.initiatePairing()))
        val owner = OwnerEvidenceHttpServer(
            "127.0.0.1", 0,
            ownerAuth,
            buildOwnerHttpAdapter(runtime, config),
            ConsoleParkerLogger("unified-state-owner-http", LogLevel.ERROR),
        ).also { it.start() }
        val agent = AgentGatewayHttpServer(
            "127.0.0.1", 0,
            AgentGatewayAuthentication(agentToken, parker.core.interfaces.PrincipalId("agent.hermes-ingestion-operator")),
            retrieveEvidenceAsAgent = runtime::retrieveEvidenceAsAgent,
            retrieveEvidenceManifestAsAgent = runtime::retrieveEvidenceManifestAsAgent,
            submitSourceAsAgent = runtime::submitSourceAsAgent,
            requestAcquisitionAsAgent = runtime::requestAcquisitionAsAgent,
            bindIngestionEvidenceAsAgent = runtime::bindIngestionEvidenceAsAgent,
            submitSourceWithBatchAsAgent = { candidate, advisory, batch -> runtime.submitSourceAsAgent(candidate, advisory, batch) },
            listReadyIngestionBatchesAsAgent = runtime::listReadyBulkIngestionBatchesAsAgent,
            submitProcessingResultAsAgent = runtime::submitProcessingResultAsAgent,
            submitOcrRepresentationAsAgent = runtime::submitOcrRepresentationAsAgent,
            submitPendingReviewSourceAsAgent = runtime::submitPendingReviewSourceAsAgent,
            listProcessingResultsForBatchAsAgent = runtime::listProcessingResultsForBatchAsAgent,
            submitGovernedIngestionAsAgent = runtime::submitGovernedIngestionAsAgent,
            audit = parker.core.runtime.FileSystemAgentGatewayAccessAudit(Path.of(config.evidenceStorageRootPath, "agent-audit.log")),
            logger = ConsoleParkerLogger("unified-state-agent-http", LogLevel.ERROR),
        ).also { it.start() }
        return Harness(runtime, owner, agent, config, caseId, pairing.sessionId)
    }

    private fun request(base: String, token: String, method: String, body: ByteArray? = null, contentType: String? = null, cookie: String? = null): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI.create(base)).header("Authorization", "Bearer $token")
        contentType?.let { builder.header("Content-Type", it) }
        cookie?.let { builder.header("Cookie", "ParkerOwnerSession=$it") }
        val publisher = body?.let { HttpRequest.BodyPublishers.ofByteArray(it) } ?: HttpRequest.BodyPublishers.noBody()
        builder.method(method, publisher)
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun processingResultJson(sha: String) = """{"sourceSha256":"$sha","status":"PASS","methods":["DIRECT_TEXT_EXTRACTION"],"processingCompleteness":"COMPLETE","processingWarnings":[],"issues":[]}""".toByteArray()

    private fun runFixture(h: Harness, fileName: String, mediaType: String): FixtureResult =
        runFixture(h, fileName, Files.readAllBytes(fixtureRoot.resolve(fileName)), mediaType)

    @Test
    fun `real external OCR completes the unified state transition for scanned PDF`() {
        val environment = System.getenv()
        val problems = parker.core.runtime.OpenAiLiveAcceptanceBridge.preflightProblems(
            environment,
            System.getProperty("parker.externalTranscription.live.enabled") == "true" ||
                environment["PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED"] == "true",
            Path.of(""),
        )
        assumeTrue(problems.isEmpty(), "live provider preflight unavailable: ${problems.joinToString(",")}")

        val h = startHarness()
        try {
            val source = Files.readAllBytes(fixtureRoot.resolve("03-scanned.pdf"))
            val before = runFixture(h, "03-scanned.pdf", source, "application/pdf")
            assertTrue(before.state in setOf(EvidenceProcessingState.REQUIRES_OCR, EvidenceProcessingState.CAPABILITY_UNAVAILABLE))

            val external = runBlocking { h.runtime.invokeExternalTranscriptionAsOwner(before.evidenceId) }
            check(external is parker.core.interfaces.ExternalTranscriptionOwnerInvocationOutcome.Admitted) {
                "external OCR outcome was not admitted: $external"
            }
            val admitted = external
            assertTrue(admitted.record.derivativeGenerationId.value.isNotBlank())
            assertTrue(admitted.extracted.recognisedText.isNotBlank())
            val processingProvenance = requireNotNull(admitted.extracted.processingProvenance)
            assertEquals(before.sourceSha256, processingProvenance.sourceManifestSha256.value)
            assertEquals(before.sourceSha256, processingProvenance.representationSha256.value)
            val providerProvenance = requireNotNull(admitted.extracted.providerProvenance)
            assertTrue(providerProvenance.providerReportedModelIdentifier.isNotBlank())
            assertTrue(providerProvenance.providerCorrelationIdentifier.isNotBlank())

            val agentBase = "http://127.0.0.1:${h.agent.boundPort}"
            val completion = request(
                "$agentBase/agent/ingestion-batches/${before.batchId}/sources/${before.sourceSha256}",
                agentToken, "POST", source, "application/pdf",
            )
            assertEquals(201, completion.statusCode(), completion.body())
            assertTrue(completion.body().contains("\"status\":\"ANALYSIS_READY\""), completion.body())

            val generations = runBlocking { h.runtime.listDerivativeGenerationsAsOwner(before.evidenceId) }
            val externalSummary = generations.single { it.derivativeGenerationId == admitted.record.derivativeGenerationId }
            assertEquals(parker.core.interfaces.OcrAuthorityClassification.EXTERNAL_AUTHORITATIVE, externalSummary.authority)
            val selected = runBlocking { h.runtime.evaluateGovernedAcquisitionAsOwner(before.evidenceId) }
            assertTrue(externalSummary.contentAvailable)
            assertTrue(selected.toString().contains("Selected"), selected.toString())
            assertEquals(EvidenceProcessingState.ANALYSIS_READY, runBlocking {
                FileSystemEvidenceProcessingStateStore(Path.of(h.config.evidenceStorageRootPath, "processing-state")).find(before.evidenceId)!!.state
            })
            assertEquals(EvidenceProcessingState.ANALYSIS_READY, EvidenceProcessingState.valueOf(
                Regex("\"processingState\":\"([^\"]+)\"").find(
                    request("http://127.0.0.1:${h.owner.boundPort}/owner/evidence", ownerToken, "GET", cookie = h.ownerSession).body(),
                )!!.groupValues[1],
            ))
            assertEquals(EvidenceProcessingState.ANALYSIS_READY, EvidenceProcessingState.valueOf(
                Regex("\"processingState\":\"([^\"]+)\"").find(completion.body())!!.groupValues[1],
            ))
            val content = runBlocking { h.runtime.retrieveTierBOcrContentAsOwner(before.evidenceId, admitted.record.derivativeGenerationId) }
            assertTrue(content.toString().contains("Retrieved"), content.toString())

            h.agent.stop(); h.owner.stop(); runBlocking { h.runtime.shutdown() }
            val restarted = ParkerRuntime(
                h.config,
                ConsoleParkerLogger("unified-state-acceptance-restart", LogLevel.ERROR),
                buildIdentity = { System.getenv("PARKER_BUILD_COMMIT") },
            )
            runBlocking { restarted.start() }
            try {
                assertEquals(EvidenceProcessingState.ANALYSIS_READY, runBlocking { restarted.processingStateAsOwner(before.evidenceId) }?.let(EvidenceProcessingState::valueOf))
                assertTrue(runBlocking { restarted.evaluateGovernedAcquisitionAsOwner(before.evidenceId) }.toString().contains("Selected"))
                assertTrue(runBlocking { restarted.retrieveTierBOcrContentAsOwner(before.evidenceId, admitted.record.derivativeGenerationId) }.toString().contains("Retrieved"))
            } finally {
                runBlocking { restarted.shutdown() }
            }
        } finally {
            runCatching { h.agent.stop() }; runCatching { h.owner.stop() }
            runBlocking { runCatching { h.runtime.shutdown() } }
        }
    }

    private fun runFixture(h: Harness, fileName: String, bytes: ByteArray, mediaType: String): FixtureResult {
        val sha = sha256(bytes)
        val case = runBlocking { assertIs<BulkIngestionAuthorisation.Authorised>(h.runtime.authoriseBulkIngestionAsOwner(h.caseId)) }
        val agentBase = "http://127.0.0.1:${h.agent.boundPort}"
        val result = request("$agentBase/agent/ingestion-batches/${case.binding.batchId}/processing-results", agentToken, "POST", processingResultJson(sha), "application/json")
        assertEquals(201, result.statusCode(), result.body())
        val admitted = request("$agentBase/agent/ingestion-batches/${case.binding.batchId}/sources/$sha", agentToken, "POST", bytes, mediaType)
        assertTrue(admitted.statusCode() in 200..202, admitted.body())
        val evidenceId = Regex("\"evidenceArtifactId\":\"([^\"]+)\"").find(admitted.body())!!.groupValues[1]
        val owner = request("http://127.0.0.1:${h.owner.boundPort}/owner/evidence", ownerToken, "GET", cookie = h.ownerSession)
        assertEquals(200, owner.statusCode())
        val ownerEntry = Regex("\\{[^{}]*\\\"evidenceArtifactId\\\":\\\"$evidenceId\\\"[^{}]*\\}").find(owner.body())!!.value
        val ownerState = Regex("\"processingState\":\"([^\"]+)\"").find(ownerEntry)!!.groupValues[1]
        val persisted = runBlocking { FileSystemEvidenceProcessingStateStore(Path.of(h.config.evidenceStorageRootPath, "processing-state")).find(parker.core.interfaces.EvidenceArtifactId(evidenceId)) }
        assertEquals(ownerState, persisted!!.state.name, "Owner/persisted divergence for $fileName: ${owner.body()}")
        val dualState = Regex("\"processingState\":\"([^\"]+)\"").find(admitted.body())!!.groupValues[1]
        assertEquals(ownerState, dualState)
        val evaluation = runBlocking { h.runtime.evaluateGovernedAcquisitionAsOwner(EvidenceArtifactId(evidenceId)) }
        if (persisted.state == EvidenceProcessingState.ANALYSIS_READY) {
            assertTrue(evaluation.toString().contains("Selected"), "ANALYSIS_READY without governed selection for $fileName: $evaluation")
        } else {
            assertTrue(!evaluation.toString().contains("Selected"), "non-ready state unexpectedly selected for $fileName: $evaluation")
        }
        return FixtureResult(EvidenceArtifactId(evidenceId), persisted.state, sha, case.binding.batchId)
    }

    private fun jpegFixture(): ByteArray {
        val image = BufferedImage(1000, 280, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.color = Color.BLACK
        graphics.font = graphics.font.deriveFont(42f)
        graphics.drawString("PARKER JPEG ACCEPTANCE", 40, 90)
        graphics.drawString("Jane Doe / Acme Corporation", 40, 165)
        graphics.drawString("Product Manager", 40, 235)
        graphics.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(image, "jpg", it) }.toByteArray()
    }

    /** Generates a genuine WebP only in the isolated test temp directory. */
    private fun webpFixtureOrNull(): ByteArray? {
        val output = Files.createTempFile("unified-state-webp-", ".webp")
        val script = """
            from PIL import Image, ImageDraw
            image = Image.new('RGB', (1000, 280), 'white')
            draw = ImageDraw.Draw(image)
            draw.text((40, 60), 'PARKER WEBP ACCEPTANCE', fill='black')
            draw.text((40, 130), 'Jane Doe / Acme Corporation', fill='black')
            draw.text((40, 200), 'Product Manager', fill='black')
            image.save(r'''${output.toAbsolutePath()}''', 'WEBP')
        """.trimIndent()
        val python = listOf(
            System.getenv("PARKER_TEST_PYTHON"),
            "/home/steve/docling-venv/bin/python",
            "python3",
        ).filterNotNull().firstOrNull { candidate ->
            candidate == "python3" || Files.isExecutable(Path.of(candidate))
        }
        val process = runCatching {
            ProcessBuilder(python ?: return null, "-c", script).redirectErrorStream(true).start().also { it.waitFor() }
        }.getOrNull()
        if (process == null || process.exitValue() != 0 || !Files.isRegularFile(output) || Files.size(output) == 0L) {
            output.deleteIfExists()
            return null
        }
        return Files.readAllBytes(output).also { output.deleteIfExists() }
    }

    @Test
    fun `real Owner and Agent Gateway projections agree for native and OCR-required fixtures`() {
        val h = startHarness()
        try {
            val native = runFixture(h, "01-searchable-simple.pdf", "application/pdf")
            assertEquals(EvidenceProcessingState.ANALYSIS_READY, native.state)
            val scanned = runFixture(h, "03-scanned.pdf", "application/pdf")
            assertTrue(scanned.state in setOf(EvidenceProcessingState.REQUIRES_OCR, EvidenceProcessingState.CAPABILITY_UNAVAILABLE))
            val png = runFixture(h, "07-text-image.png", "image/png")
            assertTrue(png.state in setOf(EvidenceProcessingState.REQUIRES_OCR, EvidenceProcessingState.CAPABILITY_UNAVAILABLE))
            val jpeg = runFixture(h, "generated-jane-doe.jpg", jpegFixture(), "image/jpeg")
            assertTrue(jpeg.state in setOf(EvidenceProcessingState.REQUIRES_OCR, EvidenceProcessingState.CAPABILITY_UNAVAILABLE))
            val webp = webpFixtureOrNull()
            assumeTrue(webp != null, "Python Pillow WebP encoder is not available in this environment")
            val webpResult = runFixture(h, "generated-jane-doe.webp", webp!!, "image/webp")
            assertTrue(webpResult.state in setOf(EvidenceProcessingState.REQUIRES_OCR, EvidenceProcessingState.CAPABILITY_UNAVAILABLE))
        } finally {
            h.agent.stop(); h.owner.stop(); runBlocking { h.runtime.shutdown() }
        }
    }

    @Test
    fun `source-only and local preliminary paths cannot become analysis ready`() {
        val h = startHarness()
        try {
            val source = Files.readAllBytes(fixtureRoot.resolve("01-searchable-simple.pdf"))
            val sourcePath = Files.createTempFile("unified-state-source-only-", ".pdf")
            Files.write(sourcePath, source)
            val imported = runBlocking { h.runtime.importEvidenceFileAsOwner(sourcePath.toString(), "application/pdf") }
            val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(imported)
            val evidenceId = accepted.acceptedEvidenceArtifact.evidenceArtifactId
            val listed = runBlocking { h.runtime.listRegisteredEvidenceAsOwner() }.single { it.evidenceArtifactId == evidenceId }
            assertTrue(listed.processingState != EvidenceProcessingState.ANALYSIS_READY)
            assertTrue(listed.processingState in setOf(EvidenceProcessingState.REGISTERED, EvidenceProcessingState.PROCESSING))
            assertEquals(null, runBlocking { FileSystemEvidenceProcessingStateStore(Path.of(h.config.evidenceStorageRootPath, "processing-state")).find(evidenceId) })
            sourcePath.deleteIfExists()
        } finally {
            h.agent.stop(); h.owner.stop(); runBlocking { h.runtime.shutdown() }
        }
    }

    @Test
    fun `legacy durable native representation is reconciled only when governed selection succeeds`() {
        val h = startHarness()
        try {
            val result = runFixture(h, "01-searchable-simple.pdf", "application/pdf")
            assertEquals(EvidenceProcessingState.ANALYSIS_READY, result.state)
            // The legacy fixture is intentionally singular. Remove any additional same-kind
            // records emitted by the ordinary acceptance submission so this test exercises
            // read-side reconciliation, not duplicate-candidate ambiguity.
            val generations = runBlocking { h.runtime.listDerivativeGenerationsAsOwner(result.evidenceId) }
            generations.drop(1).forEach { duplicate ->
                Files.deleteIfExists(Path.of(h.config.derivativeGenerationStorageRootPath, "${duplicate.derivativeGenerationId.value}.derivative"))
                Files.deleteIfExists(Path.of(h.config.derivativeContentStorageRootPath, "${duplicate.derivativeGenerationId.value}.content"))
            }
            val statePath = Path.of(h.config.evidenceStorageRootPath, "processing-state", "${result.evidenceId.value}.state")
            statePath.deleteIfExists()
            val owner = runBlocking { h.runtime.listRegisteredEvidenceAsOwner().single { it.evidenceArtifactId == result.evidenceId } }
            assertEquals(
                EvidenceProcessingState.ANALYSIS_READY,
                owner.processingState,
                "legacy reconciliation failed: owner=$owner evaluation=${runBlocking { h.runtime.evaluateGovernedAcquisitionAsOwner(result.evidenceId) }} derivatives=${runBlocking { h.runtime.listDerivativeGenerationsAsOwner(result.evidenceId) }}",
            )
            val restored = runBlocking { FileSystemEvidenceProcessingStateStore(Path.of(h.config.evidenceStorageRootPath, "processing-state")).find(result.evidenceId) }
            assertEquals(EvidenceProcessingState.ANALYSIS_READY, restored!!.state)
            assertEquals(result.evidenceId, restored.evidenceArtifactId)
        } finally {
            h.agent.stop(); h.owner.stop(); runBlocking { h.runtime.shutdown() }
        }
    }

    @Test
    fun `real isolated state survives a runtime restart`() {
        val h = startHarness()
        try {
            val native = runFixture(h, "01-searchable-simple.pdf", "application/pdf")
            val image = runFixture(h, "07-text-image.png", "image/png")
            val before = listOf(native, image).associate { it.evidenceId to it.state }
            h.agent.stop(); h.owner.stop(); runBlocking { h.runtime.shutdown() }
            val restarted = ParkerRuntime(h.config, ConsoleParkerLogger("unified-state-acceptance-restart", LogLevel.ERROR))
            runBlocking { restarted.start() }
            try {
                for ((evidenceId, expected) in before) {
                    assertEquals(expected, runBlocking { restarted.processingStateAsOwner(evidenceId) }?.let(EvidenceProcessingState::valueOf))
                    val evaluation = runBlocking { restarted.evaluateGovernedAcquisitionAsOwner(evidenceId) }
                    if (expected == EvidenceProcessingState.ANALYSIS_READY) assertTrue(evaluation.toString().contains("Selected"))
                }
            } finally {
                runBlocking { restarted.shutdown() }
            }
        } finally {
            // The original runtime is already stopped in the normal path; this is idempotent.
            runCatching { h.agent.stop() }; runCatching { h.owner.stop() }; runBlocking { runCatching { h.runtime.shutdown() } }
        }
    }
}
