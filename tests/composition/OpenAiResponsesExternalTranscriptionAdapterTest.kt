package parker.core.runtime

import java.io.IOException
import java.net.ConnectException
import java.net.URI
import java.net.UnknownHostException
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpTimeoutException
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CompletionException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import parker.composition.*
import parker.core.interfaces.*

class OpenAiResponsesExternalTranscriptionAdapterTest {
    private val sentinel = "API_KEY_SENTINEL"
    private val credential = OpenAiApiCredential.fromEnvironment(sentinel)!!
    private val evidenceId = EvidenceArtifactId("evidence-unit-h")
    private val digest = sha256("source".toByteArray())

    private class FakeTransport(private val action: (OpenAiResponsesTransportRequest) -> OpenAiResponsesTransportResponse) : OpenAiResponsesTransport {
        var calls = 0
        lateinit var request: OpenAiResponsesTransportRequest
        override suspend fun execute(request: OpenAiResponsesTransportRequest): OpenAiResponsesTransportResponse {
            calls++; this.request = request; return action(request)
        }
    }

    @Test
    fun `PDF request is exact stateless Responses shape with strict schema and one call`() = runTest {
        val transport = FakeTransport { OpenAiResponsesTransportResponse(200, successEnvelope().toByteArray()) }
        val outcome = adapter(transport).transcribe(request("application/pdf", "pdf bytes".toByteArray()))

        assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcome)
        assertEquals(1, transport.calls)
        assertEquals(URI("https://api.openai.com/v1/responses"), transport.request.endpoint)
        val body = transport.request.body
        assertTrue(body.contains("\"store\":false"))
        assertTrue(body.contains("\"stream\":false"))
        assertTrue(body.contains("\"model\":\"synthetic-reviewed-model\""))
        assertTrue(body.contains("\"type\":\"input_file\""))
        assertTrue(body.contains("\"filename\":\"source.pdf\""))
        assertTrue(body.contains("\"file_data\":\"data:application/pdf;base64,cGRmIGJ5dGVz\""))
        assertFalse(body.contains("\"file_data\":\"cGRmIGJ5dGVz\""))
        assertTrue(body.contains("\"type\":\"json_schema\""))
        assertTrue(body.contains("\"strict\":true"))
        assertTrue(body.contains("Do not paraphrase", ignoreCase = true))
        assertTrue(body.contains("Omission or qualification is preferable to invention", ignoreCase = true))
        listOf("\"tools\"", "web_search", "file_search", "mcp", "code_interpreter", "previous_response_id", "conversation").forEach {
            assertTrue(!body.contains(it, ignoreCase = true), "request unexpectedly contains $it")
        }
    }

    @Test
    fun `literal v2 instruction and schema have one canonical byte identity`() = runTest {
        val instructionBytes = LITERAL_V2_INSTRUCTION.toByteArray(Charsets.UTF_8)
        assertEquals(1146, instructionBytes.size)
        assertFalse(instructionBytes.take(3) == listOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()))
        assertFalse(LITERAL_V2_INSTRUCTION.endsWith("\n"))
        assertEquals(LITERAL_V2_INSTRUCTION_SHA256, sha256Hex(instructionBytes))
        assertEquals("c721e63b29e56f9242ee24dd8f13ddcab5d4468d3d17e9e3b9b1d66a68cb2000", LITERAL_V2_INSTRUCTION_SHA256)
        listOf(
            "Do not paraphrase", "summarize", "rewrite for clarity", "infer missing text",
            "Do not insert likely names, dates, amounts, facts, legal propositions", "Omission or qualification is preferable to invention",
        ).forEach { assertTrue(LITERAL_V2_INSTRUCTION.contains(it, ignoreCase = true), "missing frozen rule: $it") }

        assertEquals(canonicalizeStructuredSchema("{\"b\":2,\"a\":1}"), canonicalizeStructuredSchema(" { \"a\" : 1, \"b\" : 2 } "))
        assertNotEquals(canonicalizeStructuredSchema("{\"a\":[1,2]}"), canonicalizeStructuredSchema("{\"a\":[2,1]}"))
        assertEquals("{\"a\":\"line\\nquote\\\"slash\\\\\"}", canonicalizeStructuredSchema("{\"a\":\"line\\nquote\\\"slash\\\\\"}"))
        assertEquals(LITERAL_V2_SCHEMA_SHA256, sha256Hex(LITERAL_V2_SCHEMA_CANONICAL.toByteArray(Charsets.UTF_8)))
        assertEquals("3fe8a26be40a06f047b493094d06c52e1df056162583b8e0b81564f55de265b2", LITERAL_V2_SCHEMA_SHA256)

        val transport = FakeTransport { OpenAiResponsesTransportResponse(200, successEnvelope().toByteArray()) }
        adapter(transport).transcribe(request())
        assertTrue(transport.request.body.contains("\"instructions\":\"${escape(LITERAL_V2_INSTRUCTION)}\""))
        assertTrue(transport.request.body.contains("\"schema\":$LITERAL_V2_SCHEMA_CANONICAL"))
    }

    @Test
    fun `provider rejection fingerprint exposes bounded fields only and never message or body`() = runTest {
        val secretMessage = "SOURCE_SECRET_SENTINEL API_KEY_SENTINEL TRANSCRIPT_SECRET_SENTINEL"
        val raw = """{"error":{"message":"$secretMessage","type":"invalid_request_error","param":"input[0].content[1].file_data","code":"invalid_value"}}"""
        var observed: OpenAiProviderErrorFingerprint? = null
        val transport = FakeTransport { OpenAiResponsesTransportResponse(400, raw.toByteArray()) }
        val outcome = OpenAiResponsesExternalTranscriptionAdapter(
            ready(), credential, transport,
            providerRejectionObserver = { observed = it },
        ).transcribe(request())

        assertEquals("PROVIDER_REJECTED_REQUEST", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        val fingerprint = requireNotNull(observed)
        assertEquals(400, fingerprint.httpStatus)
        assertEquals("invalid_request_error", fingerprint.providerErrorType)
        assertEquals("invalid_value", fingerprint.providerErrorCode)
        assertEquals("input[0].content[1].file_data", fingerprint.providerErrorParam)
        assertEquals("PROVIDER_REJECTED_REQUEST", fingerprint.category)
        assertEquals(
            "HTTP_STATUS=400 PROVIDER_ERROR_TYPE=invalid_request_error PROVIDER_ERROR_CODE=invalid_value " +
                "PROVIDER_ERROR_PARAM=input[0].content[1].file_data CATEGORY=PROVIDER_REJECTED_REQUEST",
            fingerprint.render(),
        )
        assertFalse(fingerprint.toString().contains(secretMessage))
        assertFalse(fingerprint.render().contains(secretMessage))
        assertFalse(outcome.toString().contains(secretMessage))
        assertEquals(1, transport.calls)
    }

    @Test
    fun `adapter construction is network silent until explicit transcription`() {
        val transport = FakeTransport { error("provider transport must not run during construction") }
        OpenAiResponsesExternalTranscriptionAdapter(ready(), credential, transport)
        assertEquals(0, transport.calls)
    }

    @Test
    fun `image request uses an inline data URL and unsupported media is rejected before transport`() = runTest {
        val transport = FakeTransport { OpenAiResponsesTransportResponse(200, successEnvelope().toByteArray()) }
        adapter(transport).transcribe(request("image/png", byteArrayOf(1, 2, 3)))
        assertTrue(transport.request.body.contains("data:image/png;base64,AQID"))

        assertFailsWith<IllegalArgumentException> { request("text/plain", byteArrayOf(1)) }
    }

    @Test
    fun `bearer header exists only at JDK transport boundary and request diagnostics redact body and secret`() {
        val transportRequest = OpenAiResponsesTransportRequest(
            URI("https://api.openai.com/v1/responses"), 1000, "sensitive-body", 1000, credential,
        )
        val request = JdkOpenAiResponsesTransport().buildHttpRequest(transportRequest)

        assertEquals("Bearer $sentinel", request.headers().firstValue("Authorization").orElseThrow())
        assertTrue(!transportRequest.toString().contains(sentinel))
        assertTrue(!transportRequest.toString().contains("sensitive-body"))
    }

    @Test
    fun `request construction failure is staged and classified without network or secret leakage`() = runTest {
        val messageSentinel = "API_KEY_SENTINEL/SOURCE_SECRET_SENTINEL"
        val transport = JdkOpenAiResponsesTransport()
        val invalid = OpenAiResponsesTransportRequest(
            URI("https://api.openai.com/v1/responses"), 0, messageSentinel, 1000, credential,
        )

        val thrown = assertFailsWith<OpenAiRequestConstructionException> { transport.execute(invalid) }
        val fingerprint = fingerprintOpenAiTransportFailure(thrown)
        assertEquals("OpenAiRequestConstructionException", fingerprint.topLevelThrowable)
        assertEquals("IllegalArgumentException", fingerprint.firstNonWrapperCause)
        assertEquals("REQUEST_BUILD", fingerprint.stage)
        assertEquals("PROVIDER_REQUEST_CONFIGURATION_FAILURE", fingerprint.category)
        assertTrue(messageSentinel !in fingerprint.toString())
        assertTrue(messageSentinel !in fingerprint.render())
        assertTrue(sentinel !in fingerprint.render())
    }

    @Test
    fun `success captures response ID exact model and NotExposed snapshot without secret`() = runTest {
        val transport = FakeTransport { OpenAiResponsesTransportResponse(200, successEnvelope().toByteArray()) }
        val candidate = assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(
            adapter(transport).transcribe(request()),
        ).candidate

        assertEquals("resp_unit_h_1", candidate.providerProvenance.providerCorrelationIdentifier)
        assertEquals("returned-model-snapshot-name", candidate.providerProvenance.providerReportedModelIdentifier)
        assertIs<OcrModelSnapshot.NotExposed>(candidate.providerProvenance.modelSnapshot)
        assertEquals(evidenceId, candidate.processingProvenance.sourceEvidenceArtifactId)
        assertTrue(!candidate.toString().contains(sentinel))
    }

    @Test
    fun `missing response ID model malformed envelope and schema-invalid content fail safely`() = runTest {
        val responses = listOf(
            successEnvelope().replace("\"id\":\"resp_unit_h_1\",", ""),
            successEnvelope().replace("\"model\":\"returned-model-snapshot-name\",", ""),
            "not-json",
            successEnvelope(structured = "{\"requested_pages\":[1]}")
        )
        responses.forEach { raw ->
            val outcome = adapter(FakeTransport { OpenAiResponsesTransportResponse(200, raw.toByteArray()) }).transcribe(request())
            assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
            assertTrue(!outcome.toString().contains(raw))
        }
    }

    @Test
    fun `documented completed raw Responses envelope parses assistant output text`() = runTest {
        val raw = """{"id":"resp_unit_h_1","object":"response","created_at":1750000000,"status":"completed","error":null,"incomplete_details":null,"instructions":null,"max_output_tokens":null,"model":"returned-model-snapshot-name","output":[{"id":"msg_unit_h_1","type":"message","status":"completed","content":[{"type":"output_text","annotations":[],"logprobs":[],"text":"${escape(structuredResult())}"}],"role":"assistant"}],"parallel_tool_calls":true,"previous_response_id":null,"store":false,"temperature":1.0,"text":{"format":{"type":"json_schema"}},"tool_choice":"auto","tools":[],"top_p":1.0,"truncation":"disabled","usage":{"input_tokens":1,"output_tokens":1,"total_tokens":2},"user":null,"metadata":{}}"""
        val outcome = adapter(FakeTransport { OpenAiResponsesTransportResponse(200, raw.toByteArray()) }).transcribe(request())

        assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(outcome)
    }

    @Test
    fun `response failures expose bounded shape and stage without sentinel content`() = runTest {
        val contentSentinel = "SOURCE_SECRET_SENTINEL_API_KEY_SENTINEL_TRANSCRIPT_SECRET_SENTINEL"
        val structured = structuredResult().replace("literal text", contentSentinel)
        val cases = listOf(
            "not-json" to "ENVELOPE_JSON",
            """{"status":"completed","model":"returned-model-snapshot-name","output":[]}""" to "RESPONSE_ID",
            """{"id":"resp_unit_h_1","status":"completed","output":[]}""" to "MODEL",
            """{"id":"resp_unit_h_1","model":"returned-model-snapshot-name","status":"completed","output":[]}""" to "OUTPUT_TEXT",
            successEnvelope(structured = "not-json-$contentSentinel") to "STRUCTURED_PAYLOAD",
            successEnvelope(structured = structured.replace("\"requested_pages\":[1]", "\"requested_pages\":[1.5]")) to "PAGE_ACCOUNTING",
            successEnvelope(structured = structured.replace("\"outcome\":\"TRANSCRIBED\"", "\"outcome\":\"UNKNOWN\"")) to "PAGES",
        )
        cases.forEach { (raw, expectedStage) ->
            var observed: OpenAiResponseFailureFingerprint? = null
            val outcome = OpenAiResponsesExternalTranscriptionAdapter(
                ready(), credential, FakeTransport { OpenAiResponsesTransportResponse(200, raw.toByteArray()) },
                responseFailureObserver = { observed = it },
            ).transcribe(request())
            assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
            val rendered = requireNotNull(observed).render()
            assertEquals(expectedStage, observed?.parkerParseStage)
            assertFalse(rendered.contains(contentSentinel))
            assertFalse(rendered.contains(raw))
            assertFalse(outcome.toString().contains(contentSentinel))
        }
    }

    @Test
    fun `response shape distinguishes refusal incomplete wrong content and empty output safely`() = runTest {
        val sentinel = "PROVIDER_MESSAGE_SECRET_SENTINEL"
        val cases = listOf(
            """{"id":"resp_refusal","model":"returned-model","status":"completed","output":[{"type":"message","content":[{"type":"refusal","refusal":"$sentinel"}]}]}""" to Triple(true, false, "OUTPUT_TEXT"),
            """{"id":"resp_incomplete","model":"returned-model","status":"incomplete","incomplete_details":{"reason":"max_output_tokens"},"output":[]}""" to Triple(false, true, "OUTPUT_TEXT"),
            """{"id":"resp_wrong","model":"returned-model","status":"completed","output":[{"type":"function_call","arguments":"$sentinel"}]}""" to Triple(false, false, "OUTPUT_TEXT"),
            """{"id":"resp_empty","model":"returned-model","status":"completed","output":[]}""" to Triple(false, false, "OUTPUT_TEXT"),
        )
        cases.forEach { (raw, expected) ->
            var observed: OpenAiResponseFailureFingerprint? = null
            OpenAiResponsesExternalTranscriptionAdapter(
                ready(), credential, FakeTransport { OpenAiResponsesTransportResponse(200, raw.toByteArray()) },
                responseFailureObserver = { observed = it },
            ).transcribe(request())
            val fingerprint = requireNotNull(observed)
            assertEquals(expected.first, fingerprint.refusalPresent)
            assertEquals(expected.second, fingerprint.incompleteReasonPresent)
            assertEquals(expected.third, fingerprint.parkerParseStage)
            assertFalse(fingerprint.render().contains(sentinel))
            assertFalse(fingerprint.toString().contains(sentinel))
        }
    }

    @Test
    fun `status redirects oversized timeout and network failures map safely with no retry`() = runTest {
        mapOf(
            301 to "PROVIDER_REDIRECT_REJECTED", 401 to "PROVIDER_AUTHENTICATION_FAILURE",
            403 to "PROVIDER_AUTHENTICATION_FAILURE", 429 to "PROVIDER_RATE_LIMITED",
            500 to "PROVIDER_UNAVAILABLE", 400 to "PROVIDER_REJECTED_REQUEST",
        ).forEach { (status, expected) ->
            val transport = FakeTransport { OpenAiResponsesTransportResponse(status, "provider secret body".toByteArray()) }
            val result = assertIs<ExternalTranscriptionMechanismOutcome.Failure>(adapter(transport).transcribe(request()))
            assertEquals(expected, result.reason); assertEquals(1, transport.calls); assertTrue(!result.toString().contains("provider secret body"))
        }
        mapOf<Exception, String>(
            HttpConnectTimeoutException("connect timeout with $sentinel") to "PROVIDER_CONNECT_TIMEOUT",
            HttpTimeoutException("request timeout with $sentinel") to "PROVIDER_REQUEST_TIMEOUT",
            SSLHandshakeException("handshake with $sentinel") to "PROVIDER_TLS_FAILURE",
            SSLException("TLS with $sentinel") to "PROVIDER_TLS_FAILURE",
            UnknownHostException("DNS with $sentinel") to "PROVIDER_DNS_FAILURE",
            ConnectException("connect with $sentinel") to "PROVIDER_CONNECT_FAILURE",
            InterruptedException("interrupted with $sentinel") to "PROVIDER_INTERRUPTED",
            IOException("I/O with $sentinel") to "PROVIDER_IO_FAILURE",
            IllegalStateException("publisher with $sentinel") to "PROVIDER_TRANSPORT_FAILURE",
            CompletionException(SSLHandshakeException("wrapped handshake with $sentinel")) to "PROVIDER_TLS_FAILURE",
        ).forEach { (error, expected) -> assertFailureFromThrow(error, expected) }
        val oversized = FakeTransport { OpenAiResponsesTransportResponse(200, ByteArray(1025)) }
        assertEquals("RESPONSE_TOO_LARGE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(adapter(oversized, outputLimit = 1024).transcribe(request())).reason)
    }

    @Test
    fun `safe fingerprint exposes class names and category only, never sentinel-bearing messages`() = runTest {
        val messageSentinel = "SOURCE_SECRET_SENTINEL/API_KEY_SENTINEL/TRANSCRIPT_SECRET_SENTINEL"
        val wrapped = CompletionException(IllegalArgumentException(messageSentinel))
        val fingerprint = fingerprintOpenAiTransportFailure(wrapped)

        assertEquals("CompletionException", fingerprint.topLevelThrowable)
        assertEquals("IllegalArgumentException", fingerprint.firstNonWrapperCause)
        assertEquals("CLIENT_SEND_OR_RESPONSE_READ", fingerprint.stage)
        assertEquals("PROVIDER_TRANSPORT_FAILURE", fingerprint.category)
        assertEquals(
            "TRANSPORT_THROWABLE=CompletionException ROOT_CAUSE=IllegalArgumentException TRANSPORT_STAGE=CLIENT_SEND_OR_RESPONSE_READ CATEGORY=PROVIDER_TRANSPORT_FAILURE",
            fingerprint.render(),
        )
        assertTrue(messageSentinel !in fingerprint.toString())
        assertTrue(messageSentinel !in fingerprint.render())

        var observed: OpenAiTransportFailureFingerprint? = null
        val transport = FakeTransport { throw wrapped }
        val outcome = OpenAiResponsesExternalTranscriptionAdapter(
            ready(), credential, transport,
            transportFailureObserver = { observed = it },
        ).transcribe(request())
        assertEquals("PROVIDER_TRANSPORT_FAILURE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
        assertEquals(fingerprint, observed)
        assertEquals(1, transport.calls)
        assertTrue(messageSentinel !in outcome.toString())
    }

    @Test
    fun `cancellation propagates and never retries`() = runTest {
        val transport = object : OpenAiResponsesTransport {
            var calls = 0
            override suspend fun execute(request: OpenAiResponsesTransportRequest): OpenAiResponsesTransportResponse {
                calls++; throw CancellationException("cancel $sentinel")
            }
        }
        val thrown = assertFailsWith<CancellationException> { adapter(transport).transcribe(request()) }
        assertEquals(1, transport.calls)
        assertEquals("OpenAI transcription cancelled", thrown.message)
        assertTrue(!thrown.message.orEmpty().contains(sentinel))
    }

    @Test
    fun `source and base64 expansion bounds reject before transport`() = runTest {
        val transport = FakeTransport { error("must not call") }
        val lowSourceProfile = ready(pdfLimit = 2)
        val tooLarge = OpenAiResponsesExternalTranscriptionAdapter(lowSourceProfile, credential, transport).transcribe(request(bytes = byteArrayOf(1, 2, 3)))
        assertEquals("INPUT_TOO_LARGE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(tooLarge).reason)
        val encoded = OpenAiResponsesExternalTranscriptionAdapter(ready(), credential, transport, maximumEncodedRequestBytes = 65_536)
            .transcribe(request(bytes = byteArrayOf(1)))
        assertEquals("ENCODED_INPUT_TOO_LARGE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(encoded).reason)
        assertEquals(0, transport.calls)
    }

    @Test
    fun `adapter dependencies remain isolated and production composition stays readiness gated`() {
        val types = OpenAiResponsesExternalTranscriptionAdapter::class.java.declaredFields.map { it.type.name }
        listOf("EvidenceCustodian", "PermissionEngine", "Memory", "Knowledge", "Analysis", "OwnerUi", "Derivative", "Docling", "Registry")
            .forEach { forbidden -> types.forEach { assertTrue(!it.contains(forbidden), "$it contains $forbidden") } }
        val runtimeSource = java.io.File("src/composition/ParkerRuntime.kt").readText()
        assertTrue(runtimeSource.contains("DisabledExternalTranscriptionMechanism"))
        assertTrue(runtimeSource.contains("OpenAiResponsesExternalTranscriptionAdapter("))
    }

    private suspend fun assertFailureFromThrow(error: Exception, expected: String) {
        val transport = object : OpenAiResponsesTransport {
            var calls = 0
            override suspend fun execute(request: OpenAiResponsesTransportRequest): OpenAiResponsesTransportResponse { calls++; throw error }
        }
        val result = assertIs<ExternalTranscriptionMechanismOutcome.Failure>(adapter(transport).transcribe(request()))
        assertEquals(expected, result.reason); assertEquals(1, transport.calls); assertTrue(!result.toString().contains(sentinel))
    }

    private fun adapter(transport: OpenAiResponsesTransport, outputLimit: Long = 4096) =
        OpenAiResponsesExternalTranscriptionAdapter(ready(outputLimit = outputLimit), credential, transport)

    private fun request(media: String = "application/pdf", bytes: ByteArray = "source".toByteArray()): ExternalTranscriptionRequest {
        val sourceDigest = sha256(bytes)
        val representation = OcrProcessingRepresentation(
            bytes,
            OcrProcessingProvenance(
                evidenceId, sourceDigest, media, bytes.size.toLong(), null, null,
                media, bytes.size.toLong(), sourceDigest, true,
                OcrProcessingRepresentationFactory.PROCESSING_PROFILE_IDENTITY, Instant.EPOCH,
            ),
        )
        return ExternalTranscriptionRequest(representation, 200)
    }

    private fun sha256(bytes: ByteArray) = OcrSha256Digest(
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) },
    )

    private fun ready(pdfLimit: Long = 1024 * 1024, outputLimit: Long = 4096) = OpenAiExternalTranscriptionReadiness.Ready(
        OpenAiExternalTranscriptionProviderProfile(
            "2", "OpenAI", "/v1/responses", false, "synthetic-reviewed-model", "RECORD_PRESENT_OR_NOT_EXPOSED",
            pdfLimit, 1024 * 1024, outputLimit, 30_000, "https://api.openai.com", "reviewed retention", "reviewed training",
            "not enabled", "reviewed account", "reviewed controls", "BEARER_API_CREDENTIAL", "reviewed logs", "reviewed region",
            LocalDate.parse("2026-08-01"), "owner-review", LocalDate.parse("2026-09-01"), listOf("reference"), listOf("terms change"),
            transcriptionProfileId = LITERAL_V2_PROFILE_ID,
            instructionSha256 = LITERAL_V2_INSTRUCTION_SHA256,
            structuredSchemaSha256 = LITERAL_V2_SCHEMA_SHA256,
            processingProfileIdentity = BYTE_EXACT_PROCESSING_PROFILE_ID,
            acceptanceState = ExternalTranscriptionAcceptanceState.CONFIGURATION_READY,
        ), OpenAiExternalTranscriptionEffectiveLimits(pdfLimit, 1024 * 1024, outputLimit, 30_000),
    )

    private fun successEnvelope(structured: String = structuredResult()): String =
        "{\"id\":\"resp_unit_h_1\",\"model\":\"returned-model-snapshot-name\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"${escape(structured)}\"}]}]}"

    private fun structuredResult() = """{"requested_pages":[1],"returned_pages":[1],"pages":[{"page_number":1,"text":"literal text","outcome":"TRANSCRIBED","reason_classification":null,"reason_detail":null,"warnings":[],"uncertainty_spans":[]}],"warnings":[]}"""
    private fun escape(value: String) = buildString { value.forEach { if (it == '\\' || it == '"') append('\\'); append(it) } }
}
