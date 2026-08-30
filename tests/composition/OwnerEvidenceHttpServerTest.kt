package parker.composition

import java.io.ByteArrayOutputStream
import java.lang.reflect.Field
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.PrincipalId
import parker.core.runtime.DocumentAnalysisCoordinator
import parker.core.runtime.FidelityFirstAcceptanceOutcome
import parker.core.runtime.ORDINARY_REGION_CAPABILITY_ID
import parker.core.runtime.OrdinaryRegionCapabilityPromotionOutcome
import parker.core.runtime.OrdinaryRegionCapabilityPromotionRequest
import parker.core.interfaces.*
import parker.ui.EnhancedTranscriptionReadiness
import java.time.Instant

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

    private fun config(doclingBridgeScriptPath: String, modelEndpointUrl: String = "http://127.0.0.1:1/api/generate"): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = modelEndpointUrl, // deliberately unreachable by default
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-evidence-http-test",
        evidenceStorageRootPath = Files.createTempDirectory("evidence-http-evidence").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("evidence-http-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("evidence-http-derivative").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("evidence-http-derivative-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("evidence-http-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("evidence-http-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("evidence-http-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("evidence-http-knowledge").resolve("items.log").toString(),
        doclingPythonExecutablePath = syntheticBridgeShellExecutable(),
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

    private class Harness(
        val runtime: ParkerRuntime,
        val server: OwnerEvidenceHttpServer,
        val runtimeLogger: RecordingParkerLogger,
        val serverLogger: RecordingParkerLogger,
    ) {
        fun baseUri(): String = "http://127.0.0.1:${server.boundPort}"
        fun shutdown() {
            server.stop()
            kotlinx.coroutines.runBlocking { runtime.shutdown() }
        }
    }

    private fun startHarness(
        doclingBridgeScriptPath: String,
        tokenOverride: String = token,
        modelEndpointUrl: String = "http://127.0.0.1:1/api/generate",
        externalReadiness: () -> EnhancedTranscriptionReadiness = { EnhancedTranscriptionReadiness.Disabled },
        invokeExternal: suspend (EvidenceArtifactId) -> ExternalTranscriptionOwnerInvocationOutcome = { ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed("disabled") },
        invokeAcceptance: suspend (String) -> FidelityFirstAcceptanceOutcome = { FidelityFirstAcceptanceOutcome.Blocked("disabled") },
        invokePromotion: (OrdinaryRegionCapabilityPromotionRequest) -> OrdinaryRegionCapabilityPromotionOutcome = {
            OrdinaryRegionCapabilityPromotionOutcome.Blocked("disabled")
        },
    ): Harness {
        val scriptDir = Files.createTempDirectory("evidence-http-scripts")
        val bridgePath = doclingBridgeScriptPath.ifEmpty { writeFakeBridgeScript(scriptDir, 0, "").toString() }
        val runtimeLogger = RecordingParkerLogger()
        val serverLogger = RecordingParkerLogger()
        val runtime = ParkerRuntime(config(bridgePath, modelEndpointUrl), runtimeLogger)
        kotlinx.coroutines.runBlocking { runtime.start() }
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
            externalReadiness = externalReadiness,
            invokeExternalTranscriptionAsOwner = invokeExternal,
            governedDecisionAsOwner = { projectGovernedDecision(runtime.evaluateGovernedAcquisitionAsOwner(it)) },
            executeGovernedAsOwner = { id, expected -> projectGovernedExecution(runtime.executeGovernedAcquisitionAsOwner(id, expected)) },
        )
        val server = OwnerEvidenceHttpServer(
            bindAddress = "127.0.0.1",
            port = 0,
            token = tokenOverride,
            operations = adapter,
            logger = serverLogger,
            invokeFidelityFirstAcceptance = invokeAcceptance,
            createOrdinaryRegionCapabilityAcceptance = invokePromotion,
        )
        server.start()
        return Harness(runtime, server, runtimeLogger, serverLogger)
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

    @Test
    fun `capability promotion endpoint is authenticated bounded and coordinator only`() {
        var calls = 0
        var request: OrdinaryRegionCapabilityPromotionRequest? = null
        val harness = startHarness("", invokePromotion = {
            calls++; request = it; OrdinaryRegionCapabilityPromotionOutcome.Blocked("SYNTHETIC_GOVERNED_BLOCK")
        })
        try {
            val uri = URI.create(harness.baseUri() + "/owner/admin/region-capability-acceptance")
            val body = "{\"capabilityId\":\"$ORDINARY_REGION_CAPABILITY_ID\",\"promotingBuildCommit\":\"${"a".repeat(40)}\"}"
            assertEquals(401, send(HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.ofString(body)).build()).statusCode())
            assertEquals(0, calls)
            val response = send(HttpRequest.newBuilder(uri).header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build())
            assertEquals(409, response.statusCode()); assertEquals(1, calls)
            assertEquals(ORDINARY_REGION_CAPABILITY_ID, request?.capabilityId)
            assertTrue(response.body().contains("SYNTHETIC_GOVERNED_BLOCK"))
            val arbitrary = send(HttpRequest.newBuilder(uri).header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString("{\"capabilityId\":\"$ORDINARY_REGION_CAPABILITY_ID\",\"promotingBuildCommit\":\"${"a".repeat(40)}\",\"evidence\":[\"${"f".repeat(64)}\"]}" )).build())
            assertEquals(400, arbitrary.statusCode()); assertEquals(1, calls)
        } finally { harness.shutdown() }
    }

    @Test
    fun `acceptance endpoint is owner authenticated exact-authority only and accepts no source overrides`() {
        var calls = 0
        var authority: String? = null
        val harness = startHarness("", invokeAcceptance = {
            calls++; authority = it
            FidelityFirstAcceptanceOutcome.Blocked("SYNTHETIC_PREFLIGHT_BLOCK")
        })
        try {
            val uri = URI.create(harness.baseUri() + "/owner/evidence/acceptance-executions/authority-synthetic")
            val unauthorised = send(HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).build())
            assertEquals(401, unauthorised.statusCode()); assertEquals(0, calls)
            val authorised = send(HttpRequest.newBuilder(uri).header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString("{\"evidenceArtifactId\":\"caller-override\",\"model\":\"caller-override\"}")).build())
            assertEquals(409, authorised.statusCode()); assertEquals(1, calls); assertEquals("authority-synthetic", authority)
            assertTrue(authorised.body().contains("SYNTHETIC_PREFLIGHT_BLOCK"))
        } finally { harness.shutdown() }
    }

    @Test
    fun `enhanced transcription is readiness gated explicit exact-id and safely presented`() {
        var calls = 0
        var invokedId: EvidenceArtifactId? = null
        val sentinel = "unit-k-secret-sentinel"
        val harness = startHarness(
            "",
            externalReadiness = { EnhancedTranscriptionReadiness.Ready },
            invokeExternal = { id -> calls++; invokedId = id; externalAdmitted(id) },
        )
        try {
            val root = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build())
            assertTrue(root.body().contains("Run local OCR")); assertTrue(root.body().contains("Run enhanced transcription"))
            assertTrue(root.body().contains("selectedForAnalysis: false")); assertEquals(0, calls)

            val readiness = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/owner/evidence/transcription-readiness")).header("Authorization", "Bearer $token").GET().build())
            assertTrue(readiness.body().contains("READY")); assertEquals(0, calls)

            val evidenceId = EvidenceArtifactId("evidence-route-exact")
            val response = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/owner/evidence/${evidenceId.value}/transcribe-external?evidenceArtifactId=replacement"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.ofString("{\"evidenceArtifactId\":\"replacement\",\"secret\":\"$sentinel\"}")).build())
            assertEquals(200, response.statusCode()); assertEquals(1, calls); assertEquals(evidenceId, invokedId)
            assertTrue(response.body().contains("generation-external-unit-k")); assertTrue(response.body().contains("Machine transcription — unverified"))
            assertTrue(root.body().contains("Fluent machine transcription may contain plausible text that is inconsistent with the source."))
            assertTrue(response.body().contains("profile")); assertTrue(response.body().contains("UNREVIEWED"))
            assertTrue(response.body().contains("Not separately exposed")); assertTrue(response.body().contains("KNOWN_INCOMPLETE"))
            assertFalse(response.body().contains(sentinel))

            val malformed = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/owner/evidence/bad%20id/transcribe-external"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build())
            assertEquals(400, malformed.statusCode()); assertEquals(1, calls)
            val get = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/owner/evidence/${evidenceId.value}/transcribe-external"))
                .header("Authorization", "Bearer $token").GET().build())
            assertEquals(404, get.statusCode()); assertEquals(1, calls)
        } finally { harness.shutdown() }
    }

    @Test
    fun `analysis request carries exact unverified acknowledgement and UI never preselects it`() {
        val page = startHarness("")
        try {
            val root = send(HttpRequest.newBuilder(URI.create(page.baseUri() + "/")).GET().build()).body()
            assertTrue(root.contains("acknowledgesUnverifiedExternalTranscription"))
            assertTrue(root.contains("I acknowledge this exact unverified machine transcription"))
            assertTrue(root.contains("selectedForAnalysis: false"))
            assertFalse(root.contains("externalResultRow: true, selectedForAnalysis: true"))
        } finally { page.shutdown() }
    }

    @Test
    fun `disabled enhanced transcription never invokes external operation`() {
        var calls = 0
        val harness = startHarness("", externalReadiness = { EnhancedTranscriptionReadiness.Disabled }, invokeExternal = { calls++; error("must not run") })
        try {
            val response = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/owner/evidence/evidence-one/transcribe-external"))
                .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build())
            assertEquals(409, response.statusCode()); assertTrue(response.body().contains("DISABLED")); assertEquals(0, calls)
        } finally { harness.shutdown() }
    }

    @Test
    fun `profile-not-ready and missing-credential readiness are transparent and non-executable`() {
        listOf(
            EnhancedTranscriptionReadiness.ProfileNotReady("Provider profile is invalid or stale.") to "PROFILE_NOT_READY",
            EnhancedTranscriptionReadiness.MissingCredential to "MISSING_CREDENTIAL",
        ).forEach { (readiness, expected) ->
            var calls = 0
            val harness = startHarness("", externalReadiness = { readiness }, invokeExternal = { calls++; error("must not run") })
            try {
                val status = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/owner/evidence/transcription-readiness"))
                    .header("Authorization", "Bearer $token").GET().build())
                assertTrue(status.body().contains(expected))
                val action = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/owner/evidence/evidence-one/transcribe-external"))
                    .header("Authorization", "Bearer $token").POST(HttpRequest.BodyPublishers.noBody()).build())
                assertEquals(409, action.statusCode()); assertEquals(0, calls)
            } finally { harness.shutdown() }
        }
    }

    @Test
    fun `governed acquisition GET is authenticated zero-execution and POST is exact and stale protected`() = runTest {
        val harness = startHarness("")
        try {
            val upload = send(uploadRequest(harness, listOf(UploadPart("files", "synthetic.csv", "text/csv", "name,value\na,1\n".toByteArray()))))
            val id = requireNotNull(extractField(upload.body(), "evidenceArtifactId"))
            val unauthorised = send(HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/$id/acquisition")).GET().build())
            assertEquals(401, unauthorised.statusCode())
            val decision = send(HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/$id/acquisition"))
                .header("Authorization", "Bearer $token").GET().build())
            assertEquals(200, decision.statusCode()); assertEquals("SELECTED", extractField(decision.body(), "status"))
            assertTrue(decision.body().contains("Native text extraction")); assertTrue(decision.body().contains("externalEgressRequired\":false"))
            val stale = send(HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/$id/acquire"))
                .header("Authorization", "Bearer $token").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"expectedCapabilityId\":\"wrong-capability\"}")).build())
            assertEquals(409, stale.statusCode()); assertEquals("STALE_DECISION", extractField(stale.body(), "status"))
            val execute = send(HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/$id/acquire"))
                .header("Authorization", "Bearer $token").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"expectedCapabilityId\":\"parker-tier-a-native-v1\"}")).build())
            assertEquals(200, execute.statusCode()); assertEquals("COMPLETED", extractField(execute.body(), "status"))
            assertTrue(execute.body().contains("derivativeGenerationId")); assertTrue(execute.body().contains("UNREVIEWED"))
        } finally { harness.shutdown() }
    }

    @Test
    fun `owner page presents governed primary action warnings and labels compatibility controls`() {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains("Acquire machine-readable representation"))
            assertTrue(body.contains("Legacy/manual specialist operation"))
            assertTrue(body.contains("bd.disabled = true"))
            assertTrue(body.contains("external.disabled = true"))
            assertTrue(body.contains("Machine transcription — unverified"))
            assertTrue(body.contains("Fluent machine transcription may contain plausible text that is inconsistent with the source."))
            listOf("best evidence", "preferred evidence", "high-quality evidence").forEach { assertFalse(body.contains(it, true)) }
        } finally { harness.shutdown() }
    }

    private fun externalAdmitted(id: EvidenceArtifactId): ExternalTranscriptionOwnerInvocationOutcome {
        val scope = OcrPageScope(listOf(1, 2))
        val accounting = OcrPageAccounting(scope, scope, OcrPageScope(listOf(1)), listOf(
            OcrPageOutcome(1, OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS, OcrPageOutcomeReason("UNCERTAIN_TEXT"), listOf("qualified"), listOf(OcrUncertaintySpan(1, 0, 1, OcrUncertaintyKind.UNCERTAIN, "uncertain"))),
            OcrPageOutcome(2, OcrPageOutcomeKind.NOT_RETURNED, OcrPageOutcomeReason("VALIDATOR_NOT_RETURNED")),
        ))
        val processing = OcrProcessingProvenance(id, OcrSha256Digest("a".repeat(64)), "application/pdf", 10, scope, scope, "application/pdf", 10, OcrSha256Digest("a".repeat(64)), true, "direct-v1", Instant.EPOCH)
        val provider = OcrProviderProvenance("OpenAI", "adapter", "1.0.0", "profile", "returned-model", OcrModelSnapshot.NotExposed, "response-id")
        val producer = DerivativeProducerIdentity("external", "1.0.0", "profile", "adapter", "1.0.0", "returned-model", null)
        val extracted = OcrDerivativeExtractedResult("literal", TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION, OcrDerivativeOutcomeKind.PARTIAL_OR_DEGRADED,
            "page 2 not returned", listOf("qualified"), listOf(OcrRecognitionSegment("literal", TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION, 1)), producer,
            listOf(DerivativeTransformation.OCR, DerivativeTransformation.MODEL_INFERENCE), DerivativeCompletenessState.KNOWN_INCOMPLETE, accounting, processing, provider, Instant.EPOCH)
        val record = DerivativeGenerationRecord(DerivativeGenerationId("generation-external-unit-k"), id, listOf(DerivativeParentReference.RootEvidenceArtifact(id)),
            "External transcription recognised text", producer, extracted.transformationHistory, Instant.EPOCH, DerivativeContentIdentity.NoCanonicalSerialization,
            extracted.completenessState, DerivativeOperationalOutcome.USABLE)
        return ExternalTranscriptionOwnerInvocationOutcome.Admitted(id, record, extracted)
    }

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
        val recognisedJson = """{"status":"recognised","recognisedText":"HTTP TEST TEXT","fidelity":"UNVERIFIED_LITERAL_TRANSCRIPTION","mechanismVersion":"fake-1.0.0"}"""
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

            assertEquals(200, ocrResponse.statusCode(), ocrResponse.body())
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

    private fun get(harness: Harness, path: String, authToken: String? = token): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI.create("${harness.baseUri()}$path")).GET()
        if (authToken != null) builder.header("Authorization", "Bearer $authToken")
        return send(builder.build())
    }

    private fun postJson(harness: Harness, path: String, body: String, authToken: String? = token): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI.create("${harness.baseUri()}$path"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        if (authToken != null) builder.header("Authorization", "Bearer $authToken")
        return send(builder.build())
    }

    /** Mirrors `ParkerRuntimeOcrCompositionTest`'s own identical reflection helper. */
    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    // ================= Document Ingestion -- Derivative Content Persistence and Retrieval =================

    @Test
    fun `the durable content endpoint returns the persisted content by evidence and generation identity, matching the Process response`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "01-searchable-simple.pdf", "application/pdf", Files.readAllBytes(fixtureRoot.resolve("01-searchable-simple.pdf"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))
            val processBody = post(harness, "/owner/evidence/$id/process").body()
            val derivativeGenerationId = requireNotNull(extractField(processBody, "derivativeGenerationId"))
            val originalText = requireNotNull(extractJsonStringField(processBody, "documentText"))

            val response = get(harness, "/owner/evidence/$id/content/$derivativeGenerationId")

            assertEquals(200, response.statusCode())
            assertEquals("RETRIEVED", extractField(response.body(), "status"))
            assertEquals("PDF", Regex(""""kind"\s*:\s*"([A-Z]+)"""").find(response.body())?.groupValues?.get(1))
            assertEquals(originalText, extractJsonStringField(response.body(), "documentText"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the durable content endpoint requires authentication -- no token, no content`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "structured.csv", "text/csv", Files.readAllBytes(fixtureRoot.resolve("06-structured.csv"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))
            val processBody = post(harness, "/owner/evidence/$id/process").body()
            val derivativeGenerationId = requireNotNull(extractField(processBody, "derivativeGenerationId"))

            val response = get(harness, "/owner/evidence/$id/content/$derivativeGenerationId", authToken = null)

            assertEquals(401, response.statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `an unknown derivative generation id returns UNKNOWN_GENERATION, never fabricated content`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "structured.csv", "text/csv", Files.readAllBytes(fixtureRoot.resolve("06-structured.csv"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))

            val response = get(harness, "/owner/evidence/$id/content/generation-never-registered")

            assertEquals(200, response.statusCode())
            assertEquals("UNKNOWN_GENERATION", extractField(response.body(), "status"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `a derivative generation id retrieved against the wrong evidence artefact returns SOURCE_MISMATCH, never the content`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "structured.csv", "text/csv", Files.readAllBytes(fixtureRoot.resolve("06-structured.csv"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))
            val processBody = post(harness, "/owner/evidence/$id/process").body()
            val derivativeGenerationId = requireNotNull(extractField(processBody, "derivativeGenerationId"))

            val otherUpload = send(uploadRequest(harness, listOf(UploadPart("files", "other.csv", "text/csv", "x,y\n1,2\n".toByteArray()))))
            val otherId = requireNotNull(extractField(otherUpload.body(), "evidenceArtifactId"))

            val response = get(harness, "/owner/evidence/$otherId/content/$derivativeGenerationId")

            assertEquals(200, response.statusCode())
            assertEquals("SOURCE_MISMATCH", extractField(response.body(), "status"))
            assertTrue("headers" !in response.body(), "a source-mismatched request must never carry the other evidence artefact's content")
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `a malformed evidence artefact or generation id in the content path is rejected with 400, never reaching the operations layer`() = runTest {
        val harness = startHarness("")
        try {
            assertEquals(400, get(harness, "/owner/evidence/%20/content/generation-1").statusCode())
            assertEquals(400, get(harness, "/owner/evidence/evidence-1/content/%20").statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `a path-traversal-shaped or reserved-device-name-shaped generation id is rejected with 400, never an internal error`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "structured.csv", "text/csv", Files.readAllBytes(fixtureRoot.resolve("06-structured.csv"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))

            assertEquals(400, get(harness, "/owner/evidence/$id/content/..").statusCode())
            assertEquals(400, get(harness, "/owner/evidence/$id/content/con").statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the durable content response carries no server temp path or stack trace`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "01-searchable-simple.pdf", "application/pdf", Files.readAllBytes(fixtureRoot.resolve("01-searchable-simple.pdf"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))
            val processBody = post(harness, "/owner/evidence/$id/process").body()
            val derivativeGenerationId = requireNotNull(extractField(processBody, "derivativeGenerationId"))

            val body = get(harness, "/owner/evidence/$id/content/$derivativeGenerationId").body()

            assertTrue("/tmp" !in body, "no server temp path may appear in the durable content response")
            assertTrue("Exception" !in body && "\tat " !in body, "no stack trace may appear in the durable content response")
        } finally {
            harness.shutdown()
        }
    }

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
            assertTrue(
                body.contains("row.status === 'TIER_A_COMPLETE' && row.derivativeGenerationId"),
                "the action must only appear once Tier A has actually completed and a durable derivativeGenerationId was returned",
            )
            assertTrue(body.contains("/content/"), "the action must fetch persisted content from the durable retrieval endpoint")
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

    // ================= Remember Owner Token On This Device =================
    //
    // A browser convenience only -- never sent to or read from the server. Every assertion here
    // inspects the served page's own static HTML/JS source (never a real production token), since
    // localStorage itself is a browser-side concern this JDK HttpClient-based test suite has no
    // DOM to exercise directly.

    @Test
    fun `the served page offers an explicit Remember token checkbox and a trusted-device warning`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains("id=\"rememberToken\""), "an explicit Remember-token checkbox must be present")
            assertTrue(body.contains("Remember token on this device"))
            assertTrue(body.contains("Use only on a trusted device."), "the trusted-device warning must be present near the checkbox")
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the served page uses localStorage with a Parker-specific storage key, never cookies or the URL`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains("TOKEN_STORAGE_KEY = 'parker.ownerHttpToken'"), "storage key must be Parker-specific and narrowly named")
            assertTrue(body.contains("localStorage.setItem(TOKEN_STORAGE_KEY"))
            assertTrue(body.contains("localStorage.getItem(TOKEN_STORAGE_KEY"))
            assertTrue("document.cookie" !in body, "the token must never be persisted via cookies")
            assertTrue("sessionStorage" !in body, "the token must never be persisted via sessionStorage")
            assertTrue("indexedDB" !in body && "IndexedDB" !in body, "the token must never be persisted via IndexedDB")
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `a remembered token is restored into the field and the checkbox starts checked`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains("function restoreRememberedToken"))
            assertTrue(body.contains("document.getElementById('token').value = remembered"))
            assertTrue(body.contains("document.getElementById('rememberToken').checked = true"))
            assertTrue(body.contains("restoreRememberedToken();"), "restoration must actually run on page load, not merely be defined")
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `unchecking Remember removes the persisted token without clearing the input field`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            val handler = requireNotNull(
                Regex("""function onRememberToggled\(\) \{.*?\n\}""", RegexOption.DOT_MATCHES_ALL).find(body),
            ) { "expected an onRememberToggled handler" }.value
            assertTrue(handler.contains("localStorage.removeItem(TOKEN_STORAGE_KEY)"))
            assertTrue(
                "getElementById('token').value = ''" !in handler && "getElementById(\"token\").value = ''" !in handler,
                "unchecking Remember must not clear the currently entered token from the input field",
            )
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `editing the token while Remember is checked updates the persisted value`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains("function onTokenFieldInput"))
            assertTrue(body.contains("addEventListener('input', onTokenFieldInput)"))
            val handler = requireNotNull(
                Regex("""function onTokenFieldInput\(\) \{.*?\n\}""", RegexOption.DOT_MATCHES_ALL).find(body),
            ) { "expected an onTokenFieldInput handler" }.value
            assertTrue(handler.contains("localStorage.setItem(TOKEN_STORAGE_KEY"), "editing the token while Remember is checked must update the persisted value, enabling rotation")
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the Authorization header still reads from the token input field, and remembering never bypasses authentication`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(
                body.contains("'Authorization': 'Bearer ' + document.getElementById('token').value"),
                "authHeaders() must be unchanged: still Bearer <the current token field value>",
            )

            // Remembering is a browser-side convenience only -- the server side of authentication is
            // untouched by this unit; a real request with no/wrong token is still rejected.
            val unauthorised = send(HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence")).POST(HttpRequest.BodyPublishers.noBody()).build())
            assertEquals(401, unauthorised.statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the server never embeds a real token into the page and never places it in a URL or query parameter`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            // The server-configured owner token (`token` in startHarness) must not appear anywhere in
            // the served page -- the page only ever reads whatever the owner has typed/restored
            // client-side, never a value the server itself knows or injects.
            assertTrue(token !in body, "the real server-side owner token must never be embedded in server-generated HTML")
            assertTrue("?token=" !in body && "&token=" !in body, "the token must never be placed in a URL query parameter")
            assertTrue(body.contains("id=\"token\" placeholder="), "the token field must remain owner-entered, not server-populated")
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the token field is type password, and existing Upload, Process, Run OCR, and View Extracted Content actions remain structurally intact`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains("<input type=\"password\" id=\"token\""), "the token field must be type=password")
            assertTrue(body.contains("id=\"uploadButton\""))
            assertTrue(body.contains("processRow(index)"))
            assertTrue(body.contains("ocrRow(index)"))
            assertTrue(body.contains("View Extracted Content"))
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
    private val durableOcrRenderCondition = "if (row.status === 'REQUIRES_OCR' || row.status === 'COMPLETE') {"
    private val tierAAnalysisEligibility = "row.status === 'TIER_A_COMPLETE' && row.derivativeGenerationId"
    private val tierBAnalysisEligibility = "row.status === 'TIER_B_DURABLE_COMPLETE' && row.ocrDerivativeGenerationId"

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
    fun `REQUIRES_OCR shows the explicit durable OCR action`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains(durableOcrRenderCondition))
            assertTrue(body.contains("bd.textContent = 'Run local OCR (Durable)'"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `transient COMPLETE still shows durable OCR but does not satisfy analysis eligibility`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains(durableOcrRenderCondition), "COMPLETE must retain the explicit durable OCR action")
            assertTrue("row.status === 'COMPLETE' && row.ocrDerivativeGenerationId" !in body)
            assertTrue("row.status === 'COMPLETE' && row.derivativeGenerationId" !in body)
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `durable Tier B with a real generation id is the sole Tier B analysis eligibility path`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains(tierBAnalysisEligibility))
            assertTrue(body.contains("derivativeGenerationId: row.ocrDerivativeGenerationId"))
            assertTrue(body.contains("acknowledgesUnverifiedExternalTranscription: !!row.acknowledgesUnverifiedExternalTranscription"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `Tier A analysis eligibility remains unchanged`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains(tierAAnalysisEligibility))
            assertTrue(body.contains("selections.push({ evidenceArtifactId: row.evidenceArtifactId, derivativeGenerationId: row.derivativeGenerationId })"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `Unit L each analysable row submits its own evidence and generation pair without evidence-level inference`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains("rows.forEach(row => {"))
            assertTrue(body.contains("if (!row.selectedForAnalysis) return;"))
            assertTrue(body.contains("rows.push({"), "a later durable generation must remain a separate visible row")
            assertTrue(body.contains("row.selectedForAnalysis = cb.checked;"), "each checkbox must update only its own row")
            assertTrue(body.contains("selections.push({ evidenceArtifactId: row.evidenceArtifactId, derivativeGenerationId: row.derivativeGenerationId })"))
            assertTrue(body.contains("derivativeGenerationId: row.ocrDerivativeGenerationId"))
            assertFalse(body.contains("latestForEvidence"))
            assertFalse(body.contains("preferredGeneration"))
            assertFalse(body.contains("newestOcr"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `Unit L analyse HTTP accepts the exact pair shape and malformed identifiers fail closed`() = runTest {
        val harness = startHarness("")
        try {
            val exact = postJson(
                harness,
                "/owner/analyse",
                """{"selections":[{"evidenceArtifactId":"evidence-exact","derivativeGenerationId":"generation-exact"}],"instruction":"Analyse"}""",
            )
            assertEquals(200, exact.statusCode())
            assertEquals("FAILED", extractField(exact.body(), "status"))

            assertEquals(
                400,
                postJson(harness, "/owner/analyse", """{"selections":[{"evidenceArtifactId":"","derivativeGenerationId":"generation"}],"instruction":"Analyse"}""").statusCode(),
            )
            assertEquals(
                400,
                postJson(harness, "/owner/analyse", """{"selections":[{"evidenceArtifactId":"evidence","derivativeGenerationId":""}],"instruction":"Analyse"}""").statusCode(),
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

    // ================= Document Ingestion — Tier B Durable OCR Derivative Content =================

    @Test
    fun `the explicit durable ocr endpoint mints a durable generation, and the durable ocr-content endpoint retrieves it back, matching exactly`() = runTest {
        val recognisedJson = """{"status":"recognised","recognisedText":"HTTP DURABLE OCR TEXT","fidelity":"UNVERIFIED_LITERAL_TRANSCRIPTION","mechanismVersion":"docling-2.5.0","modelIdentity":"rapidocr-onnxruntime:PP-OCRv6_rec_small","modelVersion":"sha256:${"a".repeat(64)}"}"""
        val scriptDir = Files.createTempDirectory("evidence-http-scripts")
        val harness = startHarness(writeFakeBridgeScript(scriptDir, 0, recognisedJson).toString())
        try {
            val uploadResponse = send(
                uploadRequest(harness, listOf(UploadPart("files", "scanned.pdf", "application/pdf", Files.readAllBytes(fixtureRoot.resolve("03-scanned.pdf"))))),
            )
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))
            assertEquals("REQUIRES_OCR", extractField(post(harness, "/owner/evidence/$id/process").body(), "status"))

            val durableResponse = post(harness, "/owner/evidence/$id/ocr-durable")
            assertEquals(200, durableResponse.statusCode())
            assertEquals("TIER_B_DURABLE_COMPLETE", extractField(durableResponse.body(), "status"), durableResponse.body())
            val derivativeGenerationId = requireNotNull(extractField(durableResponse.body(), "derivativeGenerationId"))
            assertEquals("HTTP DURABLE OCR TEXT", extractJsonStringField(durableResponse.body(), "recognisedText"))

            val retrieveResponse = get(harness, "/owner/evidence/$id/ocr-content/$derivativeGenerationId")
            assertEquals(200, retrieveResponse.statusCode())
            assertEquals("RETRIEVED", extractField(retrieveResponse.body(), "status"))
            assertEquals("HTTP DURABLE OCR TEXT", extractJsonStringField(retrieveResponse.body(), "recognisedText"))
            assertEquals("rapidocr-onnxruntime:PP-OCRv6_rec_small", extractField(retrieveResponse.body(), "modelIdentity"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the durable ocr endpoint requires authentication -- no token, no durable generation`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "scanned.pdf", "application/pdf", Files.readAllBytes(fixtureRoot.resolve("03-scanned.pdf"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))

            val response = send(
                HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/$id/ocr-durable"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(),
            )

            assertEquals(401, response.statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the durable ocr endpoint rejects the wrong token`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "scanned.pdf", "application/pdf", Files.readAllBytes(fixtureRoot.resolve("03-scanned.pdf"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))

            val response = send(
                HttpRequest.newBuilder(URI.create("${harness.baseUri()}/owner/evidence/$id/ocr-durable"))
                    .header("Authorization", "Bearer wrong-token")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(),
            )

            assertEquals(401, response.statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the durable ocr-content endpoint requires authentication -- no token, no content`() = runTest {
        val harness = startHarness("")
        try {
            val response = get(harness, "/owner/evidence/evidence-1/ocr-content/generation-1", authToken = null)
            assertEquals(401, response.statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `an unknown derivative generation id on the ocr-content endpoint returns UNKNOWN_GENERATION, never fabricated content`() = runTest {
        val harness = startHarness("")
        try {
            val response = get(harness, "/owner/evidence/evidence-1/ocr-content/generation-never-registered")
            assertEquals(200, response.statusCode())
            assertEquals("UNKNOWN_GENERATION", extractField(response.body(), "status"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `a Tier A generation retrieved through the ocr-content endpoint returns WRONG_DERIVATIVE_KIND, never a mis-decoded result`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "structured.csv", "text/csv", Files.readAllBytes(fixtureRoot.resolve("06-structured.csv"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))
            val processBody = post(harness, "/owner/evidence/$id/process").body()
            val derivativeGenerationId = requireNotNull(extractField(processBody, "derivativeGenerationId"))

            val response = get(harness, "/owner/evidence/$id/ocr-content/$derivativeGenerationId")

            assertEquals(200, response.statusCode())
            assertEquals("WRONG_DERIVATIVE_KIND", extractField(response.body(), "status"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `a path-traversal-shaped or reserved-device-name-shaped generation id on the ocr-content endpoint is rejected with 400, never an internal error`() = runTest {
        val harness = startHarness("")
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "structured.csv", "text/csv", Files.readAllBytes(fixtureRoot.resolve("06-structured.csv"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))

            assertEquals(400, get(harness, "/owner/evidence/$id/ocr-content/..").statusCode())
            assertEquals(400, get(harness, "/owner/evidence/$id/ocr-content/con").statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `the durable ocr response carries no server temp path, model path, or stack trace`() = runTest {
        val recognisedJson = """{"status":"recognised","recognisedText":"NO LEAKAGE TEXT","fidelity":"UNVERIFIED_LITERAL_TRANSCRIPTION","mechanismVersion":"docling-2.5.0","modelIdentity":"rapidocr-onnxruntime:PP-OCRv6_rec_small","modelVersion":"sha256:${"a".repeat(64)}"}"""
        val scriptDir = Files.createTempDirectory("evidence-http-scripts")
        val harness = startHarness(writeFakeBridgeScript(scriptDir, 0, recognisedJson).toString())
        try {
            val uploadResponse = send(uploadRequest(harness, listOf(UploadPart("files", "scanned.pdf", "application/pdf", Files.readAllBytes(fixtureRoot.resolve("03-scanned.pdf"))))))
            val id = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))
            post(harness, "/owner/evidence/$id/process")

            val body = post(harness, "/owner/evidence/$id/ocr-durable").body()

            assertTrue("/tmp" !in body, "response must never carry a server temp path: $body")
            assertTrue(".onnx" !in body, "response must never carry a model artifact filename: $body")
            assertTrue("Exception" !in body, "response must never carry a raw exception name: $body")
            assertTrue("\tat " !in body, "response must never carry a stack trace: $body")
        } finally {
            harness.shutdown()
        }
    }

    // ================= Minimum Production Document Pipeline -- Local Reasoning Implementation =================

    @Test
    fun `M an analyse request with no Authorization header is rejected with 401`() = runTest {
        val harness = startHarness("")
        try {
            val response = postJson(
                harness, "/owner/analyse",
                """{"selections":[{"evidenceArtifactId":"e","derivativeGenerationId":"g"}],"instruction":"Summarise"}""",
                authToken = null,
            )
            assertEquals(401, response.statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `M an analyse request with the wrong token is rejected with 401`() = runTest {
        val harness = startHarness("")
        try {
            val response = postJson(
                harness, "/owner/analyse",
                """{"selections":[{"evidenceArtifactId":"e","derivativeGenerationId":"g"}],"instruction":"Summarise"}""",
                authToken = "wrong-token",
            )
            assertEquals(401, response.statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `N a malformed or invalid analyse request body returns a clean 400, never an internal error`() = runTest {
        val harness = startHarness("")
        try {
            assertEquals(400, postJson(harness, "/owner/analyse", "not json at all").statusCode())
            assertEquals(400, postJson(harness, "/owner/analyse", "{}").statusCode())
            assertEquals(400, postJson(harness, "/owner/analyse", """{"selections":[],"instruction":"x"}""").statusCode())
            assertEquals(400, postJson(harness, "/owner/analyse", """{"selections":[{"evidenceArtifactId":"e"}],"instruction":"x"}""").statusCode())
            assertEquals(400, postJson(harness, "/owner/analyse", """{"selections":[{"evidenceArtifactId":"e","derivativeGenerationId":"g"}]}""").statusCode())
            assertEquals(400, postJson(harness, "/owner/analyse", """{"selections":[{"evidenceArtifactId":"e","derivativeGenerationId":"g"}],"instruction":""}""").statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `a GET request to the analyse route is rejected, never treated as a valid invocation`() = runTest {
        val harness = startHarness("")
        try {
            assertEquals(404, get(harness, "/owner/analyse").statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `I too many selections on the analyse route fails closed cleanly, never a 500`() = runTest {
        val harness = startHarness("")
        try {
            val selections = (1..(DocumentAnalysisCoordinator.MAX_SELECTIONS + 1))
                .joinToString(",") { """{"evidenceArtifactId":"e-$it","derivativeGenerationId":"g-$it"}""" }
            val response = postJson(harness, "/owner/analyse", """{"selections":[$selections],"instruction":"Summarise"}""")

            assertEquals(200, response.statusCode())
            assertEquals("FAILED", extractField(response.body(), "status"))
            assertTrue("Too many documents" in (extractField(response.body(), "message") ?: ""))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `end-to-end -- a real Tier A document, selected and analysed, returns the exact evidence references supplied, and neither evidence, prompt, nor model response content ever appears in a log`() = runTest {
        StubModelServer.start("The document appears to be a simple searchable PDF.").use { stub ->
            val harness = startHarness("", modelEndpointUrl = stub.endpointUrl)
            try {
                val uploadResponse = send(
                    uploadRequest(
                        harness,
                        listOf(UploadPart("files", "01-searchable-simple.pdf", "application/pdf", Files.readAllBytes(fixtureRoot.resolve("01-searchable-simple.pdf")))),
                    ),
                )
                val evidenceArtifactId = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))
                val processBody = post(harness, "/owner/evidence/$evidenceArtifactId/process").body()
                val derivativeGenerationId = requireNotNull(extractField(processBody, "derivativeGenerationId"))
                val originalText = requireNotNull(extractJsonStringField(processBody, "documentText"))
                val instruction = "What kind of document is this?"

                val analyseBody = """{"selections":[{"evidenceArtifactId":"$evidenceArtifactId","derivativeGenerationId":"$derivativeGenerationId"}],"instruction":"$instruction"}"""
                val response = postJson(harness, "/owner/analyse", analyseBody)

                assertEquals(200, response.statusCode())
                assertEquals("COMPLETED", extractField(response.body(), "status"))
                assertEquals("The document appears to be a simple searchable PDF.", extractJsonStringField(response.body(), "analysisText"))
                assertEquals(evidenceArtifactId, extractField(response.body(), "evidenceArtifactId"))
                assertEquals(derivativeGenerationId, extractField(response.body(), "derivativeGenerationId"))

                // W -- no re-extraction/OCR occurred: the exact same documentText the earlier
                // /process response already returned is what reached the (stubbed) model verbatim,
                // never re-derived from the source a second time. Escaped TWICE: once by
                // DefaultDocumentAnalysisPromptBuilder's own JSON framing (correction pass §4 --
                // evidence content is embedded as a JSON string value inside the prompt), then
                // again by LocalHttpModelInferenceClient's own defaultOllamaRequestBody/jsonEscape,
                // since the stub records the raw outbound Ollama JSON request body (the prompt
                // itself is that request's own JSON string field value).
                fun jsonEscapeOnce(s: String) = s
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                val doublyEscapedOriginalText = jsonEscapeOnce(jsonEscapeOnce(originalText))
                assertTrue(doublyEscapedOriginalText in stub.receivedRequestBodies.single())

                // P, Q, R -- evidence content, the owner's own prompt/instruction content, and the
                // model's own response never appear in either logger's own recorded messages.
                val allLogMessages = (harness.runtimeLogger.messages() + harness.serverLogger.messages()).joinToString("\n")
                assertFalse(originalText in allLogMessages, "evidence content must never appear in a log line")
                assertFalse(instruction in allLogMessages, "the owner's own instruction/prompt content must never appear in a log line")
                assertFalse("The document appears to be a simple searchable PDF." in allLogMessages, "the model's own response must never appear in a log line")
            } finally {
                harness.shutdown()
            }
        }
    }

    @Test
    fun `S the served page selects and renders analysis results via textContent only, never innerHTML, for owner- or model-controlled content`() = runTest {
        val harness = startHarness("")
        try {
            val response = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build())
            val body = response.body()
            assertTrue(body.contains("id=\"analysisInstruction\""), "the page must offer an instruction input for document analysis")
            assertTrue(body.contains("id=\"analyseButton\""), "the page must offer one explicit Analyse action")
            assertTrue(body.contains("id=\"analysisResults\""), "the page must offer a results area")
            assertTrue(body.contains("appendExtractedText(panel, 'Analysis:'"), "the model's own analysis text must be inserted via the existing textContent-only appendExtractedText helper")
            assertTrue(
                body.contains("li.textContent = analysisEvidenceReferenceText(ref)"),
                "evidence references must be inserted via textContent, never innerHTML",
            )
        } finally {
            harness.shutdown()
        }
    }

    // ================= Correction pass §1: bound /owner/analyse request body =================

    private class CountingInfiniteInputStream : java.io.InputStream() {
        var bytesRequested: Long = 0
            private set

        override fun read(): Int {
            bytesRequested += 1
            return 'x'.code
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            bytesRequested += len
            b.fill('x'.code.toByte(), off, off + len)
            return len
        }
    }

    @Test
    fun `A a request body at or below the permitted limit is processed normally, never rejected for size`() = runTest {
        val harness = startHarness("")
        try {
            // Well under MAX_ANALYSE_REQUEST_BODY_BYTES (32 KiB) and well under the coordinator's
            // own MAX_INSTRUCTION_CHARACTERS (4,000) -- this body must reach real JSON parsing and
            // coordinator logic, not be rejected for size.
            val instruction = "x".repeat(3_000)
            val body = """{"selections":[{"evidenceArtifactId":"e","derivativeGenerationId":"g"}],"instruction":"$instruction"}"""
            assertTrue(body.toByteArray(StandardCharsets.UTF_8).size < OwnerEvidenceHttpServer.MAX_ANALYSE_REQUEST_BODY_BYTES)

            val response = postJson(harness, "/owner/analyse", body)

            // Reaching a real "FAILED" (unknown generation) outcome -- not a 400/"request body too
            // large" -- proves this body was actually parsed and passed to the coordinator, not
            // rejected for size.
            assertEquals(200, response.statusCode())
            assertEquals("FAILED", extractField(response.body(), "status"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `B a request body above the permitted limit is rejected with a clean 400, never processed`() = runTest {
        val harness = startHarness("")
        try {
            val oversizedInstruction = "x".repeat(64 * 1024)
            val body = """{"selections":[{"evidenceArtifactId":"e","derivativeGenerationId":"g"}],"instruction":"$oversizedInstruction"}"""
            assertTrue(body.toByteArray(StandardCharsets.UTF_8).size > OwnerEvidenceHttpServer.MAX_ANALYSE_REQUEST_BODY_BYTES)

            val response = postJson(harness, "/owner/analyse", body)

            assertEquals(400, response.statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `C overflow is detected by a bounded read -- readBounded never requests substantially more than the limit from the stream, never buffers an unbounded body first`() {
        val limit = 1_000L
        val source = CountingInfiniteInputStream()

        assertFailsWithRequestBodyTooLarge { readBounded(source, limit) }

        // At most one 8 KiB chunk's worth beyond the limit is ever requested from the stream --
        // proving overflow is detected while reading, never by first calling an unbounded
        // readBytes() and inspecting the resulting size afterward.
        assertTrue(
            source.bytesRequested <= limit + 8192,
            "readBounded must never request substantially more than the limit from the stream; requested ${source.bytesRequested}",
        )
    }

    private fun assertFailsWithRequestBodyTooLarge(block: () -> Unit) {
        try {
            block()
            throw AssertionError("expected RequestBodyTooLargeException, but no exception was thrown")
        } catch (e: RequestBodyTooLargeException) {
            // expected
        }
    }

    @Test
    fun `D no model invocation occurs after an oversized request body is rejected`() = runTest {
        StubModelServer.start("must never be called").use { stub ->
            val harness = startHarness("", modelEndpointUrl = stub.endpointUrl)
            try {
                val oversizedInstruction = "x".repeat(64 * 1024)
                val body = """{"selections":[{"evidenceArtifactId":"e","derivativeGenerationId":"g"}],"instruction":"$oversizedInstruction"}"""

                val response = postJson(harness, "/owner/analyse", body)

                assertEquals(400, response.statusCode())
                assertTrue(stub.receivedRequestBodies.isEmpty(), "no request must ever reach the model endpoint after an oversized body is rejected")
            } finally {
                harness.shutdown()
            }
        }
    }

    // ================= Correction pass §5: stricten the purpose-built JSON parser =================

    @Test
    fun `A duplicate instruction key is rejected with a clean 400`() = runTest {
        val harness = startHarness("")
        try {
            val body = """{"selections":[{"evidenceArtifactId":"e","derivativeGenerationId":"g"}],"instruction":"first","instruction":"second"}"""
            assertEquals(400, postJson(harness, "/owner/analyse", body).statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `B duplicate selections key is rejected with a clean 400`() = runTest {
        val harness = startHarness("")
        try {
            val body = """{"selections":[{"evidenceArtifactId":"e","derivativeGenerationId":"g"}],"selections":[],"instruction":"x"}"""
            assertEquals(400, postJson(harness, "/owner/analyse", body).statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `C a duplicate nested selection field is rejected with a clean 400`() = runTest {
        val harness = startHarness("")
        try {
            val body = """{"selections":[{"evidenceArtifactId":"e1","evidenceArtifactId":"e2","derivativeGenerationId":"g"}],"instruction":"x"}"""
            assertEquals(400, postJson(harness, "/owner/analyse", body).statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `D a valid JSON object followed by trailing garbage is rejected with a clean 400`() = runTest {
        val harness = startHarness("")
        try {
            val body = """{"selections":[{"evidenceArtifactId":"e","derivativeGenerationId":"g"}],"instruction":"x"} garbage"""
            assertEquals(400, postJson(harness, "/owner/analyse", body).statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `E a valid JSON object followed only by trailing whitespace remains valid`() = runTest {
        val harness = startHarness("")
        try {
            val body = "{\"selections\":[{\"evidenceArtifactId\":\"e\",\"derivativeGenerationId\":\"g\"}],\"instruction\":\"x\"}\n  \t\n"
            val response = postJson(harness, "/owner/analyse", body)
            assertEquals(200, response.statusCode())
            assertEquals("FAILED", extractField(response.body(), "status"))
        } finally {
            harness.shutdown()
        }
    }

    // ================= Final correction pass §3: JSON parser nesting-depth limit =================

    @Test
    fun `A ordinary valid analysis JSON succeeds`() = runTest {
        val harness = startHarness("")
        try {
            val body = """{"selections":[{"evidenceArtifactId":"e","derivativeGenerationId":"g"}],"instruction":"Summarise"}"""
            val response = postJson(harness, "/owner/analyse", body)
            assertEquals(200, response.statusCode())
            assertEquals("FAILED", extractField(response.body(), "status"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `B legitimate nested selection structures (object containing an array of objects) succeed`() = runTest {
        val harness = startHarness("")
        try {
            // The real request schema's own maximum legitimate depth: root object -> "selections"
            // array -> selection object (three levels) -- well within MAX_JSON_NESTING_DEPTH.
            val body = """{"selections":[{"evidenceArtifactId":"e1","derivativeGenerationId":"g1"},{"evidenceArtifactId":"e2","derivativeGenerationId":"g2"}],"instruction":"Summarise"}"""
            val response = postJson(harness, "/owner/analyse", body)
            assertEquals(200, response.statusCode())
            assertEquals("FAILED", extractField(response.body(), "status"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `C deeply nested unknown content is rejected`() = runTest {
        val harness = startHarness("")
        try {
            // Well beyond MAX_JSON_NESTING_DEPTH (10) and beyond the real schema's own maximum
            // legitimate depth (3) -- an arbitrary, pathologically nested array structure.
            val nestingLevels = 30
            val body = "{\"selections\":" + "[".repeat(nestingLevels) + "\"x\"" + "]".repeat(nestingLevels) + ",\"instruction\":\"x\"}"

            val response = postJson(harness, "/owner/analyse", body)

            // D: rejection maps to a clean HTTP 400.
            assertEquals(400, response.statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `E no analysis or model invocation occurs after a deeply nested JSON body is rejected`() = runTest {
        StubModelServer.start("must never be called").use { stub ->
            val harness = startHarness("", modelEndpointUrl = stub.endpointUrl)
            try {
                val nestingLevels = 30
                val body = "{\"selections\":" + "[".repeat(nestingLevels) + "\"x\"" + "]".repeat(nestingLevels) + ",\"instruction\":\"x\"}"

                val response = postJson(harness, "/owner/analyse", body)

                assertEquals(400, response.statusCode())
                assertTrue(stub.receivedRequestBodies.isEmpty(), "no request must ever reach the model endpoint after a deeply nested JSON body is rejected")
                // The response body itself never exposes parser internals (position counters, stack
                // traces) -- only the generic malformed-request shape every other 400 on this route uses.
                assertFalse("StackOverflow" in response.body())
                assertFalse("Exception" in response.body())
            } finally {
                harness.shutdown()
            }
        }
    }

    // ================= Correction pass §6 / Final correction pass §4: composition direct-dependency check =================

    /**
     * Final correction pass §4: this test proves exactly three things, no more --
     * 1. the actual, currently-running [ParkerRuntime] contains a `documentAnalysisCoordinator`
     *    field, of type [parker.core.runtime.DocumentAnalysisCoordinator];
     * 2. that coordinator's own DIRECTLY DECLARED collaborator fields (its constructor
     *    parameters) contain no OCR/extraction/Memory/Knowledge/QMD/RKS/external-provider
     *    coordinator TYPE;
     * 3. this is a direct-dependency check against the CURRENT production composition only.
     *
     * It is **not** a transitive proof over every collaborator those fields themselves hold (a
     * `PermissionEngine`/`TierAContentRetrievalCoordinator`/etc. instance's own further internals
     * are not inspected here), and it is **not** a permanent architectural guarantee -- a future
     * edit to this class or to how [ParkerRuntime] composes it could change what it holds. It does
     * **not** prove "OCR can never be invoked" in any absolute sense; it proves "the current
     * coordinator has no direct OCR/extraction collaborator." The stronger no-regeneration
     * conclusion this repository relies on rests on the *combination* of this test, coordinator
     * source inspection (`DocumentAnalysisCoordinator.kt`'s own reuse of the already-governed,
     * non-regenerating Tier A/Tier B content-retrieval coordinators), behavioural tests
     * (`DocumentAnalysisCoordinatorTest`), and synthetic live acceptance -- never this test alone.
     */
    @Test
    fun `composition -- the real ParkerRuntime's documentAnalysisCoordinator has no direct OCR-Memory-Knowledge-QMD-RKS collaborator field`() = runTest {
        val harness = startHarness("")
        try {
            val coordinator = harness.runtime.privateField<Any>("documentAnalysisCoordinator")
            assertEquals("DocumentAnalysisCoordinator", coordinator::class.simpleName)

            val fieldTypeNames = coordinator::class.java.declaredFields.map { it.type.simpleName }.toSet()
            val forbidden = listOf(
                "MemoryCore", "KnowledgeRetrieval", "KnowledgeSubmission", "RelevanceMechanism", "QmdRelevanceMechanism",
                "OcrMechanism", "EvidenceIntelligenceOcrCoordinator", "DerivativeGenerationCoordinator",
                "TierADocumentIngestionRouter", "ReasoningProvider", "EvidenceIntelligence",
            )
            forbidden.forEach { forbiddenType ->
                assertTrue(
                    fieldTypeNames.none { it.contains(forbiddenType) },
                    "the real, currently-composed DocumentAnalysisCoordinator's own directly declared fields must contain no $forbiddenType type reference -- found: $fieldTypeNames",
                )
            }
        } finally {
            harness.shutdown()
        }
    }

    // ================= Reviewed Analysis Result -- Explicit Owner Save =================

    @Test
    fun `E DocumentAnalysisCoordinator's own directly declared fields hold no SavedAnalysisStorage or PendingAnalysisCache reference -- analysis alone never saves anything`() = runTest {
        val harness = startHarness("")
        try {
            val coordinator = harness.runtime.privateField<Any>("documentAnalysisCoordinator")
            val fieldTypeNames = coordinator::class.java.declaredFields.map { it.type.simpleName }.toSet()
            assertTrue(fieldTypeNames.none { it.contains("SavedAnalysisStorage") || it.contains("PendingAnalysisCache") })
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `R a save-analysis request with no Authorization header is rejected with 401`() = runTest {
        val harness = startHarness("")
        try {
            assertEquals(401, postJson(harness, "/owner/saved-analyses", """{"pendingAnalysisId":"x"}""", authToken = null).statusCode())
            assertEquals(401, get(harness, "/owner/saved-analyses", authToken = null).statusCode())
            assertEquals(401, get(harness, "/owner/saved-analyses/x", authToken = null).statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `S a save-analysis request with the wrong token is rejected with 401`() = runTest {
        val harness = startHarness("")
        try {
            assertEquals(401, postJson(harness, "/owner/saved-analyses", """{"pendingAnalysisId":"x"}""", authToken = "wrong-token").statusCode())
            assertEquals(401, get(harness, "/owner/saved-analyses", authToken = "wrong-token").statusCode())
            assertEquals(401, get(harness, "/owner/saved-analyses/x", authToken = "wrong-token").statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `L a path-traversal-shaped or reserved-device-name-shaped saved analysis id is rejected with a clean 400, never an internal error`() = runTest {
        val harness = startHarness("")
        try {
            assertEquals(400, get(harness, "/owner/saved-analyses/..").statusCode())
            assertEquals(400, get(harness, "/owner/saved-analyses/con").statusCode())
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `retrieving an unknown saved analysis id returns a clean UNKNOWN_SAVED_ANALYSIS status, never 500`() = runTest {
        val harness = startHarness("")
        try {
            val response = get(harness, "/owner/saved-analyses/never-saved-analysis-id")
            assertEquals(200, response.statusCode())
            assertEquals("UNKNOWN_SAVED_ANALYSIS", extractField(response.body(), "status"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    // The route's own request parser only ever reads "pendingAnalysisId" -- an "analysisText"/
    // "instruction" field the client adds is never read, let alone trusted or persisted. Because
    // the pending id below was never issued by a real /owner/analyse call, this must fail
    // regardless of what other fields accompany it.
    fun `C a save request naming an unknown pending id is rejected even with forged content fields present`() = runTest {
        val harness = startHarness("")
        try {
            val body = """{"pendingAnalysisId":"forged-pending-id","analysisText":"FORGED CONTENT","instruction":"forged instruction"}"""
            val response = postJson(harness, "/owner/saved-analyses", body)

            assertEquals(200, response.statusCode())
            assertEquals("FAILED", extractField(response.body(), "status"))
            assertEquals(emptyList<String>(), extractAllFields(get(harness, "/owner/saved-analyses").body(), "savedAnalysisId"))
        } finally {
            harness.shutdown()
        }
    }

    @Test
    fun `Q the served page renders retrieved saved analyses via textContent only, never innerHTML`() = runTest {
        val harness = startHarness("")
        try {
            val body = send(HttpRequest.newBuilder(URI.create(harness.baseUri() + "/")).GET().build()).body()
            assertTrue(body.contains("id=\"savedAnalysisRows\""), "the page must offer a saved-analyses listing area")
            assertTrue(body.contains("id=\"savedAnalysisDetail\""), "the page must offer a saved-analysis detail area")
            assertTrue(body.contains("saveButton.onclick = saveCurrentAnalysis"), "the page must offer an explicit Save Analysis action")
            assertTrue(body.contains("instructionTd.textContent = entry.instructionPreview"), "listing instruction previews must be inserted via textContent, never innerHTML")
            assertTrue(body.contains("appendExtractedText(panel, 'Analysis:', result.result.analysisText)"), "a retrieved saved analysis's own text must be inserted via the existing textContent-only appendExtractedText helper")
        } finally {
            harness.shutdown()
        }
    }

    @Test
    // Full end-to-end proof over real HTTP: upload, process, analyse, explicitly Save, list, and
    // retrieve a saved analysis -- exact instruction/references/evidence survive, and no
    // evidence/instruction/prompt/model-response content ever appears in a log.
    fun `end-to-end -- upload, process, analyse, Save, list, and retrieve a saved analysis over real HTTP`() = runTest {
        StubModelServer.start("The Case-ID is PF-007/26.").use { stub ->
            val harness = startHarness("", modelEndpointUrl = stub.endpointUrl)
            try {
                val uploadResponse = send(
                    uploadRequest(
                        harness,
                        listOf(UploadPart("files", "01-searchable-simple.pdf", "application/pdf", Files.readAllBytes(fixtureRoot.resolve("01-searchable-simple.pdf")))),
                    ),
                )
                val evidenceArtifactId = requireNotNull(extractField(uploadResponse.body(), "evidenceArtifactId"))
                val processBody = post(harness, "/owner/evidence/$evidenceArtifactId/process").body()
                val derivativeGenerationId = requireNotNull(extractField(processBody, "derivativeGenerationId"))
                val originalText = requireNotNull(extractJsonStringField(processBody, "documentText"))
                val instruction = "What is the Case-ID?"

                val analyseBody = """{"selections":[{"evidenceArtifactId":"$evidenceArtifactId","derivativeGenerationId":"$derivativeGenerationId"}],"instruction":"$instruction"}"""
                val analyseResponse = postJson(harness, "/owner/analyse", analyseBody)
                assertEquals("COMPLETED", extractField(analyseResponse.body(), "status"))
                val pendingAnalysisId = requireNotNull(extractField(analyseResponse.body(), "pendingAnalysisId"))

                // No saved analyses exist yet.
                assertEquals(emptyList<String>(), extractAllFields(get(harness, "/owner/saved-analyses").body(), "savedAnalysisId"))

                val saveResponse = postJson(harness, "/owner/saved-analyses", """{"pendingAnalysisId":"$pendingAnalysisId"}""")
                assertEquals(200, saveResponse.statusCode())
                assertEquals("SAVED", extractField(saveResponse.body(), "status"))
                val savedAnalysisId = requireNotNull(extractField(saveResponse.body(), "savedAnalysisId"))

                // A second Save attempt on the same (now-consumed) pending id fails cleanly.
                val secondSaveResponse = postJson(harness, "/owner/saved-analyses", """{"pendingAnalysisId":"$pendingAnalysisId"}""")
                assertEquals("FAILED", extractField(secondSaveResponse.body(), "status"))

                // N, O: bounded listing, metadata only -- never the full analysis text.
                val listResponse = get(harness, "/owner/saved-analyses")
                assertEquals(200, listResponse.statusCode())
                assertEquals(listOf(savedAnalysisId), extractAllFields(listResponse.body(), "savedAnalysisId"))
                assertFalse("Case-ID is PF-007/26" in listResponse.body(), "a listing entry must never carry the full analysis text")

                // P: retrieve-by-id exact equality.
                val retrieveResponse = get(harness, "/owner/saved-analyses/$savedAnalysisId")
                assertEquals(200, retrieveResponse.statusCode())
                assertEquals("RETRIEVED", extractField(retrieveResponse.body(), "status"))
                assertEquals(instruction, extractJsonStringField(retrieveResponse.body(), "instruction"))
                assertEquals("The Case-ID is PF-007/26.", extractJsonStringField(retrieveResponse.body(), "analysisText"))
                assertEquals(evidenceArtifactId, extractField(retrieveResponse.body(), "evidenceArtifactId"))
                assertEquals(derivativeGenerationId, extractField(retrieveResponse.body(), "derivativeGenerationId"))

                // U, W: no new derivative generation and no OCR/extraction occurred from analysing or saving.
                assertFalse("OCR" in harness.runtimeLogger.messages().joinToString(" "))

                // X: evidence content, the owner's own instruction, and the model's own response never
                // appear in either logger's own recorded messages, across the entire analyse+save+
                // retrieve+list sequence.
                val allLogMessages = (harness.runtimeLogger.messages() + harness.serverLogger.messages()).joinToString("\n")
                assertFalse(originalText in allLogMessages, "evidence content must never appear in a log line")
                assertFalse(instruction in allLogMessages, "the owner's own instruction content must never appear in a log line")
                assertFalse("The Case-ID is PF-007/26." in allLogMessages, "the model's own response must never appear in a log line")
            } finally {
                harness.shutdown()
            }
        }
    }
}
