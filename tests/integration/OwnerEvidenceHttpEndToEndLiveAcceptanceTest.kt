package parker.integration

import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.composition.ConsoleParkerLogger
import parker.composition.LogLevel
import parker.composition.OwnerEvidenceHttpServer
import parker.composition.OwnerUiEvidenceRuntimeAdapter
import parker.composition.ParkerRuntime
import parker.composition.ParkerRuntimeConfig
import parker.core.interfaces.PrincipalId

/**
 * Owner LAN Evidence Upload -- Phase 13's own decisive real, LAN-style
 * acceptance instrument: the real [OwnerEvidenceHttpServer], bound to a
 * real loopback TCP port and driven with the JDK's own
 * `java.net.http.HttpClient` -- standing in for the Windows-laptop-side
 * browser `fetch`/`FormData` behaviour this Unit's own governing task
 * describes -- in front of the real, fully-wired production graph and a
 * real, provisioned Docling installation (never a fake bridge; mirrors
 * [OwnerEvidenceUiEndToEndLiveAcceptanceTest]'s own identical live gate and
 * seven-fixture shape, over HTTP transport instead of direct in-process
 * calls). Proves the whole owner path end to end: upload -> import ->
 * explicit Tier A -> (for the two OCR-eligible fixtures) explicit,
 * owner-triggered Tier B via real Docling -> Complete.
 */
class OwnerEvidenceHttpEndToEndLiveAcceptanceTest {

    companion object {
        private const val LIVE_PROPERTY = "parker.ocr.docling.live.enabled"
    }

    private val ownerPrincipalId = "user.owner-evidence-http-e2e-live"
    private val fixtureRoot: Path = Path.of("tests", "fixtures", "document-ingestion-bakeoff", "fixtures")
    private val client: HttpClient = HttpClient.newHttpClient()
    private val token = "live-acceptance-owner-http-token"

    private fun resolvedPythonExecutablePath(): String? =
        System.getenv("DOCLING_TEST_PYTHON")?.takeIf { it.isNotBlank() }

    private fun resolvedBridgeScriptPath(): String =
        System.getenv("DOCLING_TEST_BRIDGE_SCRIPT")?.takeIf { it.isNotBlank() }
            ?: Path.of("tools", "docling-ocr-bridge.py").toAbsolutePath().toString()

    private fun assumeLiveDoclingPrerequisitesProvisioned() {
        val pythonExecutablePath = resolvedPythonExecutablePath()
        assumeTrue(
            pythonExecutablePath != null,
            "Live Docling prerequisites are not provisioned on this machine -- missing: DOCLING_TEST_PYTHON.",
        )
        val probe = try {
            ProcessBuilder(listOf(pythonExecutablePath!!, "-c", "import docling")).start().apply { waitFor() }.exitValue() == 0
        } catch (_: Exception) {
            false
        }
        assumeTrue(probe, "DOCLING_TEST_PYTHON=$pythonExecutablePath cannot import the docling package -- skipping")
    }

    private fun config(): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- reasoning is not under test here
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-evidence-http-e2e-live",
        evidenceStorageRootPath = Files.createTempDirectory("evidence-http-e2e-live-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("evidence-http-e2e-live-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("evidence-http-e2e-live-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("evidence-http-e2e-live-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("evidence-http-e2e-live-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("evidence-http-e2e-live-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("evidence-http-e2e-live-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("evidence-http-e2e-live-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = resolvedPythonExecutablePath() ?: "python3",
        doclingBridgeScriptPath = resolvedBridgeScriptPath(),
        doclingModelCacheDir = System.getenv("DOCLING_TEST_MODEL_CACHE_DIR")?.takeIf { it.isNotBlank() },
        doclingTimeoutMillis = 120_000L,
        logLevel = LogLevel.ERROR,
    )

    private data class UploadPart(val fileName: String, val contentType: String, val bytes: ByteArray)

    private fun multipartBody(boundary: String, parts: List<UploadPart>): ByteArray {
        val out = ByteArrayOutputStream()
        fun line(s: String) {
            out.write(s.toByteArray(StandardCharsets.UTF_8))
            out.write("\r\n".toByteArray(StandardCharsets.UTF_8))
        }
        for (part in parts) {
            line("--$boundary")
            line("Content-Disposition: form-data; name=\"files\"; filename=\"${part.fileName}\"")
            line("Content-Type: ${part.contentType}")
            line("")
            out.write(part.bytes)
            out.write("\r\n".toByteArray(StandardCharsets.UTF_8))
        }
        line("--$boundary--")
        return out.toByteArray()
    }

    private fun extractAllFields(json: String, field: String): List<String> =
        Regex(""""$field"\s*:\s*"([^"]*)"""").findAll(json).map { it.groupValues[1] }.toList()

    private fun extractField(json: String, field: String): String? = extractAllFields(json, field).firstOrNull()

    private fun send(request: HttpRequest): HttpResponse<String> =
        client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))

    @Test
    fun `real HTTP upload, Tier A, and explicit Tier B over real Docling process all seven governed fixtures truthfully`() {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live Docling property absent; no subprocess invoked")
        assumeLiveDoclingPrerequisitesProvisioned()

        val runtime = ParkerRuntime(config(), ConsoleParkerLogger(component = "evidence-http-e2e-live", minLevel = LogLevel.ERROR))
        runBlocking { runtime.start() }
        val adapter = OwnerUiEvidenceRuntimeAdapter(
            ownerPrincipalId = PrincipalId(ownerPrincipalId),
            importEvidenceFileAsOwner = runtime::importEvidenceFileAsOwner,
            invokeTierAIngestionAsOwner = runtime::invokeTierAIngestionAsOwner,
            analyseEvidence = runtime::analyseEvidence,
            retrieveTierAExtractedContentAsOwner = runtime::retrieveTierAExtractedContentAsOwner,
            invokeTierBOcrDurableGenerationAsOwner = runtime::invokeTierBOcrDurableGenerationAsOwner,
            retrieveTierBOcrContentAsOwner = runtime::retrieveTierBOcrContentAsOwner,
            analyseDocumentsAsOwner = runtime::analyseDocumentsAsOwner,
            saveAnalysisAsOwner = runtime::saveAnalysisAsOwner,
            retrieveSavedAnalysisAsOwner = runtime::retrieveSavedAnalysisAsOwner,
            listSavedAnalysesAsOwner = runtime::listSavedAnalysesAsOwner,
        )
        val server = OwnerEvidenceHttpServer(
            bindAddress = "127.0.0.1",
            port = 0,
            token = token,
            operations = adapter,
            logger = ConsoleParkerLogger(component = "evidence-http-e2e-live-server", minLevel = LogLevel.ERROR),
        )
        server.start()
        val baseUri = "http://127.0.0.1:${server.boundPort}"

        try {
            data class Case(val file: String, val mediaType: String, val expectFormat: String?, val requiresOcr: Boolean)
            val cases = listOf(
                Case("01-searchable-simple.pdf", "application/pdf", "PDF", false),
                Case("02-multicolumn-complex.pdf", "application/pdf", "PDF", false),
                Case("03-scanned.pdf", "application/pdf", null, true),
                Case("04-structured.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "DOCX", false),
                Case("05-email-with-attachment.eml", "message/rfc822", "EML", false),
                Case("06-structured.csv", "text/csv", "CSV", false),
                Case("07-text-image.png", "image/png", null, true),
            )

            // Step 1: real LAN-style upload -- one HTTP request carrying all seven files, exactly
            // as the browser page's own "Select Files" -> "Upload" action would send them.
            val boundary = "OwnerEvidenceHttpE2eLiveBoundary"
            val body = multipartBody(
                boundary,
                cases.map { UploadPart(it.file, it.mediaType, Files.readAllBytes(fixtureRoot.resolve(it.file))) },
            )
            val uploadResponse = send(
                HttpRequest.newBuilder(URI.create("$baseUri/owner/evidence"))
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "multipart/form-data; boundary=$boundary")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build(),
            )
            assertEquals(200, uploadResponse.statusCode())
            val ids = extractAllFields(uploadResponse.body(), "evidenceArtifactId")
            assertEquals(7, ids.size, "all seven governed fixtures must import independently, with real EvidenceArtifactIds")
            val idByFile = cases.map { it.file }.zip(ids).toMap()

            fun post(path: String): HttpResponse<String> = send(
                HttpRequest.newBuilder(URI.create("$baseUri$path"))
                    .header("Authorization", "Bearer $token")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(),
            )

            // Step 2: explicit Process for every uploaded file.
            val afterTierA = cases.associate { case ->
                val response = post("/owner/evidence/${idByFile.getValue(case.file)}/process")
                assertEquals(200, response.statusCode())
                case.file to response.body()
            }
            cases.filterNot { it.requiresOcr }.forEach { case ->
                assertEquals("TIER_A_COMPLETE", extractField(afterTierA.getValue(case.file), "status"), "unexpected Tier A status for ${case.file}")
                assertEquals(case.expectFormat, extractField(afterTierA.getValue(case.file), "format"), "unexpected Tier A format for ${case.file}")
            }
            cases.filter { it.requiresOcr }.forEach { case ->
                assertEquals("REQUIRES_OCR", extractField(afterTierA.getValue(case.file), "status"), "unexpected Tier A status for ${case.file}")
            }

            // Step 3: explicit Run OCR for the two RequiresTierB fixtures only -- a real Docling
            // subprocess invocation, over real HTTP transport, never invoked automatically.
            val ocrStart = System.nanoTime()
            val ocrResponses = cases.filter { it.requiresOcr }.associate { case ->
                case.file to post("/owner/evidence/${idByFile.getValue(case.file)}/ocr")
            }
            val ocrElapsedMillis = (System.nanoTime() - ocrStart) / 1_000_000
            println("Owner HTTP end-to-end live acceptance: real OCR leg elapsed=${ocrElapsedMillis}ms")
            assertTrue(ocrElapsedMillis > 1000, "real Docling recognition over two real fixtures, via real HTTP, is not near-instantaneous")

            ocrResponses.forEach { (file, response) ->
                assertEquals(200, response.statusCode())
                assertEquals("COMPLETE", extractField(response.body(), "status"), "unexpected OCR status for $file")
            }
        } finally {
            server.stop()
            runBlocking { runtime.shutdown() }
        }
    }
}
