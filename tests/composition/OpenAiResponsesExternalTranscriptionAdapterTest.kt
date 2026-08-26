package parker.core.runtime

import java.io.IOException
import java.net.URI
import java.net.http.HttpTimeoutException
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import parker.composition.*
import parker.core.interfaces.*

class OpenAiResponsesExternalTranscriptionAdapterTest {
    private val sentinel = "unit-h-fake-secret-sentinel"
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
        assertTrue(body.contains("\"type\":\"json_schema\""))
        assertTrue(body.contains("\"strict\":true"))
        assertTrue(body.contains("do not guess", ignoreCase = true))
        assertTrue(body.contains("Do not summarize", ignoreCase = true))
        listOf("\"tools\"", "web_search", "file_search", "mcp", "code_interpreter", "previous_response_id", "conversation").forEach {
            assertTrue(!body.contains(it, ignoreCase = true), "request unexpectedly contains $it")
        }
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
        assertFailureFromThrow(HttpTimeoutException("timeout with $sentinel"), "PROVIDER_TIMEOUT")
        assertFailureFromThrow(IOException("dns with $sentinel"), "PROVIDER_NETWORK_FAILURE")
        val oversized = FakeTransport { OpenAiResponsesTransportResponse(200, ByteArray(1025)) }
        assertEquals("RESPONSE_TOO_LARGE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(adapter(oversized, outputLimit = 1024).transcribe(request())).reason)
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
    fun `adapter dependencies remain isolated and production composition remains disabled`() {
        val types = OpenAiResponsesExternalTranscriptionAdapter::class.java.declaredFields.map { it.type.name }
        listOf("EvidenceCustodian", "PermissionEngine", "Memory", "Knowledge", "Analysis", "OwnerUi", "Derivative", "Docling", "Registry")
            .forEach { forbidden -> types.forEach { assertTrue(!it.contains(forbidden), "$it contains $forbidden") } }
        val runtimeSource = java.io.File("src/composition/ParkerRuntime.kt").readText()
        assertTrue(runtimeSource.contains("DisabledExternalTranscriptionMechanism"))
        assertTrue(!runtimeSource.contains("OpenAiResponsesExternalTranscriptionAdapter"))
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
            "1", "OpenAI", "/v1/responses", false, "synthetic-reviewed-model", "RECORD_PRESENT_OR_NOT_EXPOSED",
            pdfLimit, 1024 * 1024, outputLimit, 30_000, "https://api.openai.com", "reviewed retention", "reviewed training",
            "not enabled", "reviewed account", "reviewed controls", "BEARER_API_CREDENTIAL", "reviewed logs", "reviewed region",
            LocalDate.parse("2026-08-01"), "owner-review", LocalDate.parse("2026-09-01"), listOf("reference"), listOf("terms change"),
        ), OpenAiExternalTranscriptionEffectiveLimits(pdfLimit, 1024 * 1024, outputLimit, 30_000),
    )

    private fun successEnvelope(structured: String = structuredResult()): String =
        "{\"id\":\"resp_unit_h_1\",\"model\":\"returned-model-snapshot-name\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"${escape(structured)}\"}]}]}"

    private fun structuredResult() = """{"requested_pages":[1],"returned_pages":[1],"pages":[{"page_number":1,"text":"literal text","outcome":"TRANSCRIBED","reason_classification":null,"reason_detail":null,"warnings":[],"uncertainty_spans":[]}],"warnings":[]}"""
    private fun escape(value: String) = buildString { value.forEach { if (it == '\\' || it == '"') append('\\'); append(it) } }
}
