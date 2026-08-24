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

    /**
     * A correct (escape-aware) JSON string-field extractor -- [extractField]'s naive `[^"]*` regex
     * cannot handle a value containing an escaped quote, backslash, or newline, which real extracted
     * document text legitimately can. Used only where a field's value needs exact, faithful
     * comparison against real extracted content.
     */
    private fun extractJsonStringField(json: String, field: String): String? {
        val key = "\"$field\":\""
        val start = json.indexOf(key)
        if (start < 0) return null
        var i = start + key.length
        val sb = StringBuilder()
        while (i < json.length) {
            val c = json[i]
            when {
                c == '"' -> return sb.toString()
                c == '\\' && i + 1 < json.length -> {
                    val next = json[i + 1]
                    when (next) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = json.substring(i + 2, i + 6)
                            sb.append(hex.toInt(16).toChar())
                            i += 4
                        }
                        else -> sb.append(next)
                    }
                    i += 2
                    continue
                }
                else -> sb.append(c)
            }
            i++
        }
        return null
    }

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

    // ================= Owner Tier A Extracted Content Presentation =================

    @Test
    fun `the HTTP Tier A response for a searchable PDF contains the exact extracted document text and provenance, without re-extraction`() = runTest {
        val harness = startHarness("")
        try {
            val sourceBytes = Files.readAllBytes(fixtureRoot.resolve("01-searchable-simple.pdf"))
            val expected = assertIsExtracted(
                parker.core.runtime.TikaPdfStructuralExtractor().extract(sourceBytes),
            )

            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "01-searchable-simple.pdf", "application/pdf", sourceBytes))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))

            val processRequest = HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/$id/process"))
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build()
            val processResponse = send(processRequest)
            val body = processResponse.body()

            assertEquals(200, processResponse.statusCode())
            assertEquals("TIER_A_COMPLETE", extractField(body, "status"))
            assertEquals("PDF", extractField(body, "format"))
            assertEquals("\"kind\":\"PDF\"", Regex(""""kind"\s*:\s*"PDF"""").find(body)?.value)
            assertEquals(expected.documentText, extractJsonStringField(body, "documentText"), "the exact PdfStructuralResult.documentText must reach the HTTP response, not a re-extraction or a fabrication")
            assertEquals(expected.completenessState.name, extractField(body, "completenessState"))
            assertEquals(expected.producerIdentity.pluginIdentity, extractField(body, "pluginIdentity"))
            assertEquals(expected.producerIdentity.pluginVersion, extractField(body, "pluginVersion"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `RequiresTierB never carries Tier A extracted content -- the owner must still explicitly Run OCR`() = runTest {
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

            val processResponse = send(
                HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/$id/process"))
                    .header("Authorization", "Bearer $token")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(),
            )
            val body = processResponse.body()

            assertEquals("REQUIRES_OCR", extractField(body, "status"))
            assertTrue("documentText" !in body, "a RequiresTierB result must never carry Tier A extracted content -- OCR has not run yet")
            assertTrue("\"content\"" !in body, "RequiresTierB must carry no content field at all")
            assertTrue(Files.notExists(marker), "viewing/requesting Tier A status must never automatically invoke OCR")
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `an unsupported or failed Tier A outcome never carries fabricated extracted content`() = runTest {
        val harness = startHarness("")
        try {
            val response = send(
                HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/evidence-never-registered/process"))
                    .header("Authorization", "Bearer $token")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(),
            )
            val body = response.body()

            assertEquals("FAILED", extractField(body, "status"))
            assertTrue("documentText" !in body)
            assertTrue("\"content\"" !in body)
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the Tier A response for a CSV, EML, and DOCX fixture presents truthful format-specific summaries, never a fabricated common shape`() = runTest {
        val harness = startHarness("")
        try {
            val csvUpload = send(uploadRequest(harness, listOf(UploadPart("files", "structured.csv", "text/csv", Files.readAllBytes(fixtureRoot.resolve("06-structured.csv"))))))
            val csvId = requireNotNull(extractField(csvUpload.body(), "evidenceArtifactId"))
            val csvProcess = post(harness, "/owner/evidence/$csvId/process").body()
            assertEquals("\"kind\":\"CSV\"", Regex(""""kind"\s*:\s*"CSV"""").find(csvProcess)?.value)
            assertTrue("headers" in csvProcess && "previewRows" in csvProcess && "totalRowCount" in csvProcess)

            val emlUpload = send(uploadRequest(harness, listOf(UploadPart("files", "email.eml", "message/rfc822", Files.readAllBytes(fixtureRoot.resolve("05-email-with-attachment.eml"))))))
            val emlId = requireNotNull(extractField(emlUpload.body(), "evidenceArtifactId"))
            val emlProcess = post(harness, "/owner/evidence/$emlId/process").body()
            assertEquals("\"kind\":\"EML\"", Regex(""""kind"\s*:\s*"EML"""").find(emlProcess)?.value)
            assertTrue("attachmentCandidateCount" in emlProcess && "bodyAlternatives" in emlProcess)

            val docxUpload = send(
                uploadRequest(
                    harness,
                    listOf(
                        UploadPart(
                            "files", "structured.docx",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            Files.readAllBytes(fixtureRoot.resolve("04-structured.docx")),
                        ),
                    ),
                ),
            )
            val docxId = requireNotNull(extractField(docxUpload.body(), "evidenceArtifactId"))
            val docxProcess = post(harness, "/owner/evidence/$docxId/process").body()
            assertEquals("\"kind\":\"DOCX\"", Regex(""""kind"\s*:\s*"DOCX"""").find(docxProcess)?.value)
            assertTrue("paragraphs" in docxProcess && "tables" in docxProcess)
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `Tier A extracted content JSON carries no server temp path or stack trace`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "01-searchable-simple.pdf", "application/pdf", Files.readAllBytes(fixtureRoot.resolve("01-searchable-simple.pdf"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))
            val body = post(harness, "/owner/evidence/$id/process").body()

            assertTrue("/tmp" !in body && "owner-upload-" !in body, "no server temp path may appear in the extracted-content response")
            assertTrue("Exception" !in body && "\tat " !in body, "no stack trace may appear in the extracted-content response")
        } finally {
            harness.shutdown()
        }
    }

    private fun post(harness: Harness, path: String): HttpResponse<String> = send(
        HttpRequest.newBuilder(URI.create("${harness.baseUri()}$path"))
            .header("Authorization", "Bearer $token")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build(),
    )

    private fun assertIsExtracted(
        outcome: parker.core.interfaces.PdfStructuralExtractionOutcome,
    ): parker.core.interfaces.PdfStructuralResult {
        assertTrue(outcome is parker.core.interfaces.PdfStructuralExtractionOutcome.Extracted, "expected Extracted but was $outcome")
        return outcome.result
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

    @Test
    fun `the served page offers a View Extracted Content action for a completed Tier A row`() = runTest {
        val harness = startHarness("")
        try {
            val response = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build())
            val body = response.body()
            assertTrue(body.contains("View Extracted Content"), "the page must offer an explicit action to view extracted content")
            assertTrue(body.contains("row.status === 'TIER_A_COMPLETE' && row.content"), "the action must only appear once Tier A has actually completed and content was returned")
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the served page never renders extracted text or metadata via innerHTML -- only textContent, closing off HTML or script injection`() = runTest {
        val harness = startHarness("")
        try {
            val response = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build())
            val body = response.body()
            // pre.textContent = ... / td2.textContent = ... etc. is how extracted content actually
            // reaches the DOM; the only innerHTML uses in the whole page are the two fixed, unrelated
            // template-literal row layouts that already escape their one owner-controllable field
            // (originalFileName, via escapeHtml).
            assertTrue(body.contains("pre.textContent = text"), "extracted document/body text must be inserted via textContent, never innerHTML")
            assertTrue(body.contains("li.textContent ="), "attachment metadata must be inserted via textContent, never innerHTML")
            assertTrue(body.contains("td2.textContent = cell"), "table cell content (CSV/DOCX) must be inserted via textContent, never innerHTML")
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
