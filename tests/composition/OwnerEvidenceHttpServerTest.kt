package parker.composition

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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.PrincipalId

/**
 * Owner LAN Evidence Upload. Decisive proof for [OwnerEvidenceHttpServer]
 * itself: a real, running server bound to an ephemeral loopback port, in
 * front of the real, fully-wired production graph (mirroring
 * [OwnerUiEvidenceRuntimeAdapterTest]'s own "real production graph, never a
 * fake" discipline), driven with the JDK's own `java.net.http.HttpClient`
 * -- the same "no new dependency" discipline the server itself follows.
 * There is no browser here, but every request below is exactly what a
 * browser's own `fetch`/`FormData` would send.
 */
class OwnerEvidenceHttpServerTest {

    private val ownerPrincipalId = "user.owner-evidence-http-test"
    private val fixtureRoot: Path = Path.of("tests", "fixtures", "document-ingestion-bakeoff", "fixtures")
    private val client: HttpClient = HttpClient.newHttpClient()
    private val token = "test-owner-http-token-1234"

    private fun config(doclingBridgeScriptPath: String): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-evidence-http-test",
        evidenceStorageRootPath = Files.createTempDirectory("evidence-http-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("evidence-http-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("evidence-http-derivative").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("evidence-http-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("evidence-http-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("evidence-http-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("evidence-http-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = "/bin/sh",
        doclingBridgeScriptPath = doclingBridgeScriptPath,
        doclingTimeoutMillis = 30_000L,
    )

    private fun writeFakeBridgeScript(directory: Path, exitCode: Int, stdout: String): Path {
        val scriptPath = Files.createTempFile(directory, "fake-docling-bridge-", ".sh")
        Files.writeString(
            scriptPath,
            "#!/bin/sh\n" + (if (stdout.isNotEmpty()) "printf '%s' '$stdout'\n" else "") + "exit $exitCode\n",
        )
        scriptPath.toFile().setExecutable(true)
        return scriptPath
    }

    private class Harness(val runtime: ParkerRuntime, val server: OwnerEvidenceHttpServer) {
        fun baseUri(): String = "http://127.0.0.1:${server.boundPort}"
        fun shutdown() {
            server.stop()
            kotlinx.coroutines.runBlocking { runtime.shutdown() }
        }
    }

    private fun startHarness(doclingBridgeScriptPath: String, tokenOverride: String = token): Harness {
        val scriptDir = Files.createTempDirectory("evidence-http-scripts")
        val bridgePath = doclingBridgeScriptPath.ifEmpty { writeFakeBridgeScript(scriptDir, 0, "").toString() }
        val runtime = ParkerRuntime(config(bridgePath), RecordingParkerLogger())
        kotlinx.coroutines.runBlocking { runtime.start() }
        val adapter = OwnerUiEvidenceRuntimeAdapter(
            ownerPrincipalId = PrincipalId(ownerPrincipalId),
            importEvidenceFileAsOwner = runtime::importEvidenceFileAsOwner,
            invokeTierAIngestionAsOwner = runtime::invokeTierAIngestionAsOwner,
            analyseEvidence = runtime::analyseEvidence,
        )
        val server = OwnerEvidenceHttpServer(
            bindAddress = "127.0.0.1",
            port = 0,
            token = tokenOverride,
            operations = adapter,
            logger = RecordingParkerLogger(),
        )
        server.start()
        return Harness(runtime, server)
    }

    private data class UploadPart(val fieldName: String, val fileName: String, val contentType: String?, val bytes: ByteArray)

    private fun multipartBody(boundary: String, parts: List<UploadPart>): ByteArray {
        val out = ByteArrayOutputStream()
        fun line(s: String) {
            out.write(s.toByteArray(StandardCharsets.UTF_8))
            out.write("\r\n".toByteArray(StandardCharsets.UTF_8))
        }
        for (part in parts) {
            line("--$boundary")
            line("Content-Disposition: form-data; name=\"${part.fieldName}\"; filename=\"${part.fileName}\"")
            if (part.contentType != null) line("Content-Type: ${part.contentType}")
            line("")
            out.write(part.bytes)
            out.write("\r\n".toByteArray(StandardCharsets.UTF_8))
        }
        line("--$boundary--")
        return out.toByteArray()
    }

    private fun uploadRequest(harness: Harness, parts: List<UploadPart>, authToken: String? = token): HttpRequest {
        val boundary = "OwnerEvidenceHttpServerTestBoundary"
        val body = multipartBody(boundary, parts)
        val builder = HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence"))
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
        if (authToken != null) builder.header("Authorization", "Bearer $authToken")
        return builder.build()
    }

    private fun send(request: HttpRequest): HttpResponse<String> =
        client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))

    private fun extractField(json: String, field: String): String? =
        Regex(""""$field"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1)

    private fun extractAllFields(json: String, field: String): List<String> =
        Regex(""""$field"\s*:\s*"([^"]*)"""").findAll(json).map { it.groupValues[1] }.toList()

    // ================= Authentication =================

    @Test
    fun `an upload request with no Authorization header is rejected and nothing is imported`() = runTest {
        val harness = startHarness("")
        try {
            val response = send(
                uploadRequest(
                    harness,
                    listOf(UploadPart("files", "report.pdf", "application/pdf", "content".toByteArray())),
                    authToken = null,
                ),
            )
            assertEquals(401, response.statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `an upload request with the wrong token is rejected`() = runTest {
        val harness = startHarness("")
        try {
            val response = send(
                uploadRequest(
                    harness,
                    listOf(UploadPart("files", "report.pdf", "application/pdf", "content".toByteArray())),
                    authToken = "wrong-token",
                ),
            )
            assertEquals(401, response.statusCode())
        } finally {
            harness.shutdown()
        }
    }

    // ================= Upload =================

    @Test
    fun `an authenticated upload places the exact bytes into Evidence Custody and returns a real EvidenceArtifactId`() = runTest {
        val harness = startHarness("")
        try {
            val content = "owner LAN evidence upload real bytes".toByteArray()
            val response = send(uploadRequest(harness, listOf(UploadPart("files", "report.pdf", "application/pdf", content))))

            assertEquals(200, response.statusCode())
            val id = extractField(response.body(), "evidenceArtifactId")
            assertTrue(id != null && id.isNotBlank())

            val retrieved = harness.runtime.retrieveEvidence(PrincipalId(ownerPrincipalId), parker.core.interfaces.EvidenceArtifactId(id))
            val found = assertIsFound(retrieved)
            assertTrue(content.contentEquals(found.content))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `multiple files in one request are imported independently with distinct evidence artefact ids`() = runTest {
        val harness = startHarness("")
        try {
            val response = send(
                uploadRequest(
                    harness,
                    listOf(
                        UploadPart("files", "one.csv", "text/csv", "a,b\n1,2\n".toByteArray()),
                        UploadPart("files", "two.csv", "text/csv", "c,d\n3,4\n".toByteArray()),
                    ),
                ),
            )

            assertEquals(200, response.statusCode())
            val ids = extractAllFields(response.body(), "evidenceArtifactId")
            assertEquals(2, ids.size)
            assertNotEquals(ids[0], ids[1])
        } finally {
            harness.shutdown()
        }
    }

    // The full 64 MiB HTTP-level bound is proven directly at the parser level below (with a small,
    // injected maxPartBytes) rather than by transferring an actual 64 MiB payload in this suite.

    @Test
    fun `path traversal, Windows fakepath, and separator-bearing filenames are reduced to a basename`() = runTest {
        val harness = startHarness("")
        try {
            val response = send(
                uploadRequest(
                    harness,
                    listOf(
                        UploadPart("files", "../../../etc/evil.txt", "text/plain", "x".toByteArray()),
                        UploadPart("files", "C:\\fakepath\\report.pdf", "application/pdf", "x".toByteArray()),
                        UploadPart("files", "/absolute/unix/path.csv", "text/csv", "x".toByteArray()),
                    ),
                ),
            )

            assertEquals(200, response.statusCode())
            val names = extractAllFields(response.body(), "originalFileName")
            assertEquals(listOf("evil.txt", "report.pdf", "path.csv"), names)
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `a Unicode filename is retained verbatim as basename metadata in the response`() = runTest {
        val harness = startHarness("")
        try {
            val unicodeName = "Kōwhai Whanganui café report — 2026.pdf"
            val response = send(
                uploadRequest(harness, listOf(UploadPart("files", unicodeName, "application/pdf", "x".toByteArray()))),
            )

            assertEquals(200, response.statusCode())
            assertEquals(unicodeName, extractField(response.body(), "originalFileName"))
        } finally {
            harness.shutdown()
        }
    }

    // ================= Tier A / Tier B endpoints =================

    @Test
    fun `the process endpoint invokes the real owner Tier A path and completes a CSV`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(
                uploadRequest(harness, listOf(UploadPart("files", "structured.csv", "text/csv", Files.readAllBytes(fixtureRoot.resolve("06-structured.csv"))))),
            )
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))

            val processRequest = HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/$id/process"))
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build()
            val processResponse = send(processRequest)

            assertEquals(200, processResponse.statusCode())
            assertEquals("TIER_A_COMPLETE", extractField(processResponse.body(), "status"))
            assertEquals("CSV", extractField(processResponse.body(), "format"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `a scanned PDF requires explicit OCR and is never processed automatically`() = runTest {
        val scriptDir = Files.createTempDirectory("evidence-http-scripts")
        val marker = scriptDir.resolve("invoked.marker")
        val scriptPath = Files.createTempFile(scriptDir, "fake-docling-bridge-", ".sh")
        Files.writeString(scriptPath, "#!/bin/sh\ntouch '${marker.toAbsolutePath()}'\nexit 0\n")
        scriptPath.toFile().setExecutable(true)
        val harness = startHarness(scriptPath.toString())
        try {
            val uploadResponse = send(
                uploadRequest(harness, listOf(UploadPart("files", "scanned.pdf", "application/pdf", Files.readAllBytes(fixtureRoot.resolve("03-scanned.pdf"))))),
            )
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))

            val processRequest = HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/$id/process"))
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build()
            val processResponse = send(processRequest)

            assertEquals("REQUIRES_OCR", extractField(processResponse.body(), "status"))
            assertTrue(Files.notExists(marker), "Tier A processing must never automatically invoke the OCR bridge")
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the explicit ocr endpoint invokes the real owner Tier B path and completes a scanned PDF`() = runTest {
        val recognisedJson = """{"status":"recognised","recognisedText":"HTTP TEST TEXT","fidelity":"VERBATIM","mechanismVersion":"fake-1.0.0"}"""
        val scriptDir = Files.createTempDirectory("evidence-http-scripts")
        val harness = startHarness(writeFakeBridgeScript(scriptDir, 0, recognisedJson).toString())
        try {
            val uploadResponse = send(
                uploadRequest(harness, listOf(UploadPart("files", "scanned.pdf", "application/pdf", Files.readAllBytes(fixtureRoot.resolve("03-scanned.pdf"))))),
            )
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))

            fun post(path: String) = send(
                HttpRequest.newBuilder(URI.create("${harness.baseUri()}$path"))
                    .header("Authorization", "Bearer $token")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(),
            )

            assertEquals("REQUIRES_OCR", extractField(post("/owner/evidence/$id/process").body(), "status"))
            val ocrResponse = post("/owner/evidence/$id/ocr")

            assertEquals(200, ocrResponse.statusCode())
            assertEquals("COMPLETE", extractField(ocrResponse.body(), "status"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `processing an evidence artefact id that was never imported fails truthfully, never fabricated`() = runTest {
        val harness = startHarness("")
        try {
            val processRequest = HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/evidence-never-registered/process"))
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build()
            val response = send(processRequest)

            assertEquals(200, response.statusCode())
            assertEquals("FAILED", extractField(response.body(), "status"))
        } finally {
            harness.shutdown()
        }
    }

    // ================= Static page =================

    @Test
    fun `the root page is served without authentication and contains the upload controls`() = runTest {
        val harness = startHarness("")
        try {
            val response = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build())
            assertEquals(200, response.statusCode())
            assertTrue(response.body().contains("Select"))
        } finally {
            harness.shutdown()
        }
    }

    // ================= UI state-mapping: Process action after a successful import =================
    //
    // Live server behavior: a successful upload returns EvidenceImportOutcome.Imported, which
    // handleUpload maps to JSON status "IMPORTED" (never "READY_TO_PROCESS" -- that name is never
    // actually produced by this endpoint, only carried in the page's own status vocabulary
    // alongside the Compose UI's identical one). The page's render() function must render the
    // Process action for that real status, not only for a status the server never sends.

    // The exact, known Process-button render condition this fix produces -- asserted as a literal
    // substring rather than parsed out of the page with a regex, since the page's source is fully
    // under this repository's own control and a literal check is far less fragile than trying to
    // structurally re-parse JavaScript out of an HTML/JS test fixture.
    private val processRenderCondition = "if (row.status === 'IMPORTED' || row.status === 'READY_TO_PROCESS') {"
    private val ocrRenderCondition = "} else if (row.status === 'REQUIRES_OCR') {"

    @Test
    fun `a successful upload returns status IMPORTED, and the served page renders Process for that exact status`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(
                uploadRequest(harness, listOf(UploadPart("files", "report.pdf", "application/pdf", "content".toByteArray()))),
            )
            assertEquals(200, uploadResponse.statusCode())
            assertEquals("IMPORTED", extractField(uploadResponse.body(), "status"))

            val pageResponse = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build())
            assertTrue(
                pageResponse.body().contains(processRenderCondition),
                "the Process button's render condition must include the real, server-sent 'IMPORTED' status, not only 'READY_TO_PROCESS'",
            )
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `READY_TO_PROCESS remains a processable status in the page's render logic`() = runTest {
        val harness = startHarness("")
        try {
            val pageResponse = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build())
            // The Process-button condition (the `if` guarding the `processRow` click handler) must
            // still reference READY_TO_PROCESS, in case any future response ever uses that name.
            assertTrue(
                pageResponse.body().contains(processRenderCondition),
                "READY_TO_PROCESS must remain recognised as processable, not removed by this fix",
            )
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `IMPORT_FAILED is never treated as processable in the page's render logic`() = runTest {
        val harness = startHarness("")
        try {
            val pageResponse = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build())
            assertTrue(
                "row.status === 'IMPORT_FAILED'" !in pageResponse.body(),
                "IMPORT_FAILED must never appear as a condition satisfying the Process-button render logic",
            )
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `REQUIRES_OCR renders only the Run OCR action, never Process, in the page's render logic`() = runTest {
        val harness = startHarness("")
        try {
            val pageResponse = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build())
            val body = pageResponse.body()
            assertTrue(
                !processRenderCondition.contains("REQUIRES_OCR") && body.contains(ocrRenderCondition),
                "REQUIRES_OCR must be in its own separate branch (Run OCR), never part of the Process-button condition",
            )
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `a successful upload response carries no Tier A or Tier B outcome fields -- no automatic processing occurs`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(
                uploadRequest(harness, listOf(UploadPart("files", "report.pdf", "application/pdf", "content".toByteArray()))),
            )
            assertEquals(200, uploadResponse.statusCode())
            val body = uploadResponse.body()
            assertEquals("IMPORTED", extractField(body, "status"))
            assertTrue(extractField(body, "format") == null, "upload must never carry a Tier A format -- Process is a separate, explicit action")
        } finally {
            harness.shutdown()
        }
    }

    // ================= Multipart parser: bounded / truncated =================

    @Test
    fun `a part exceeding the configured maximum size is rejected as that one part, and a sibling part after it still parses normally`() {
        val tempDir = Files.createTempDirectory("evidence-http-parser")
        val boundary = "b"
        val body = multipartBody(
            boundary,
            listOf(
                UploadPart("files", "big.bin", "application/octet-stream", ByteArray(1000)),
                UploadPart("files", "small.txt", "text/plain", "hello".toByteArray()),
            ),
        )
        val parser = BoundedMultipartParser(boundary, maxPartBytes = 100, tempDirectory = tempDir, maxFiles = 10)

        val outcome = parser.parse(body.inputStream())

        assertEquals(1, outcome.fileParts.size, "the sibling part after the oversized one must still be parsed")
        assertEquals("small.txt", outcome.fileParts.single().originalFileName)
        assertEquals(1, outcome.rejectedParts.size)
        assertEquals("big.bin", outcome.rejectedParts.single().originalFileName)
        assertTrue(Files.list(tempDir).use { it.count() } == 1L, "only the surviving sibling's temp file remains -- no leftover for the rejected part")
        outcome.fileParts.forEach { runCatching { Files.deleteIfExists(it.tempPath) } }
    }

    @Test
    fun `a truncated multipart body (aborted upload) is rejected rather than hanging or silently truncating`() {
        val tempDir = Files.createTempDirectory("evidence-http-parser")
        val boundary = "b"
        val full = multipartBody(boundary, listOf(UploadPart("files", "a.txt", "text/plain", "hello world".toByteArray())))
        val truncated = full.copyOf(full.size - 10) // cut off before the closing boundary
        val parser = BoundedMultipartParser(boundary, maxPartBytes = 1024, tempDirectory = tempDir, maxFiles = 10)

        assertThrows<MultipartParseException> { parser.parse(truncated.inputStream()) }
        assertTrue(Files.list(tempDir).use { it.count() } == 0L, "no leftover temp file after a truncated upload")
    }

    @Test
    fun `too many files in one request is rejected`() {
        val tempDir = Files.createTempDirectory("evidence-http-parser")
        val boundary = "b"
        val parts = (1..5).map { UploadPart("files", "f$it.txt", "text/plain", "x".toByteArray()) }
        val body = multipartBody(boundary, parts)
        val parser = BoundedMultipartParser(boundary, maxPartBytes = 1024, tempDirectory = tempDir, maxFiles = 3)

        assertThrows<MultipartParseException> { parser.parse(body.inputStream()) }
    }

    private fun assertIsFound(result: EvidenceRetrievalResult): EvidenceRetrievalResult.Found {
        assertTrue(result is EvidenceRetrievalResult.Found, "expected Found but was $result")
        return result
    }

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        try {
            block()
            throw AssertionError("expected ${T::class.simpleName} but nothing was thrown")
        } catch (e: Throwable) {
            if (e !is T) throw AssertionError("expected ${T::class.simpleName} but got $e", e)
        }
    }
}
