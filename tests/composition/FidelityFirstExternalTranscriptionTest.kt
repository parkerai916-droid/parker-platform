package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import parker.composition.*
import parker.core.interfaces.*

class FidelityFirstExternalTranscriptionTest {
    private class FakeTransport(private val response: String) : OpenAiResponsesTransport {
        var calls = 0
        lateinit var request: OpenAiResponsesTransportRequest
        override suspend fun execute(request: OpenAiResponsesTransportRequest): OpenAiResponsesTransportResponse {
            calls++; this.request = request
            return OpenAiResponsesTransportResponse(200, response.toByteArray())
        }
    }

    @Test fun `request is direct stateless gpt-5-6-sol with governed detail and identity`() = runTest {
        val transport = FakeTransport(envelope(payload()))
        val result = adapter(transport).transcribe(request("application/pdf"))
        assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(result)
        assertEquals(1, transport.calls)
        with(transport.request.body) {
            assertContains(this, "\"model\":\"gpt-5.6-sol\"")
            assertContains(this, "\"store\":false")
            assertContains(this, "\"reasoning\":{\"effort\":\"none\"}")
            assertContains(this, "\"type\":\"input_file\"")
            assertContains(this, "\"detail\":\"high\"")
            assertContains(this, "data:application/pdf;base64,")
            assertContains(this, "request-1")
            assertContains(this, "attempt-1")
            listOf("web_search", "file_search", "code_interpreter", "previous_response_id").forEach { assertFalse(contains(it)) }
        }
    }

    @Test fun `image uses original detail and ordered blocks map without losing uncertainty`() = runTest {
        val transport = FakeTransport(envelope(payload()))
        val candidate = assertIs<ExternalTranscriptionMechanismOutcome.Candidate>(
            adapter(transport).transcribe(request("image/png")),
        ).candidate
        assertContains(transport.request.body, "\"detail\":\"original\"")
        assertEquals("Alpha\nBeta?", candidate.pages.single().text)
        assertEquals("UNCERTAIN_CHARACTER: final glyph unclear", candidate.pages.single().uncertaintySpans.single().disclosure)
        assertEquals("2.0.0", candidate.providerProvenance.adapterVersion)
        assertEquals(FIDELITY_FIRST_TRANSCRIPTION_PROFILE_ID, candidate.providerProvenance.transcriptionConfigurationProfile)
    }

    @Test fun `identity mismatch duplicate reordered and missing pages fail closed without retry`() = runTest {
        listOf(
            payload().replace("\"attempt_id\":\"attempt-1\"", "\"attempt_id\":\"other\""),
            payload().replace("\"returned_pages\":[1]", "\"returned_pages\":[1,1]"),
            payload().replace("\"block_order\":2", "\"block_order\":3"),
            payload().replace("\"returned_pages\":[1]", "\"returned_pages\":[2]"),
        ).forEach { malformed ->
            val transport = FakeTransport(envelope(malformed))
            val outcome = adapter(transport).transcribe(request())
            assertEquals("MALFORMED_PROVIDER_RESPONSE", assertIs<ExternalTranscriptionMechanismOutcome.Failure>(outcome).reason)
            assertEquals(1, transport.calls)
        }
    }

    @Test fun `frozen instruction and schema identities are deterministic`() {
        assertEquals(FIDELITY_FIRST_INSTRUCTION_SHA256, sha256Hex(FIDELITY_FIRST_INSTRUCTION.toByteArray()))
        assertEquals(FIDELITY_FIRST_SCHEMA_SHA256, sha256Hex(FIDELITY_FIRST_SCHEMA_CANONICAL.toByteArray()))
        assertContains(FIDELITY_FIRST_SCHEMA_CANONICAL, "block_order")
        assertContains(FIDELITY_FIRST_SCHEMA_CANONICAL, "UNCERTAIN_ORDERING_LAYOUT")
    }

    private fun adapter(transport: OpenAiResponsesTransport) = OpenAiResponsesExternalTranscriptionAdapter(
        OpenAiExternalTranscriptionReadiness.Ready(profile(), OpenAiExternalTranscriptionEffectiveLimits(1_000_000, 1_000_000, 1_000_000, 30_000)),
        OpenAiApiCredential.fromEnvironment("synthetic-secret")!!, transport,
    )
    private fun profile() = OpenAiExternalTranscriptionProviderProfile(
        "3", "OpenAI", "/v1/responses", false, "gpt-5.6-sol", "RECORD_PRESENT_OR_NOT_EXPOSED",
        1_000_000, 1_000_000, 1_000_000, 30_000, "https://api.openai.com", "reviewed", "reviewed", "not enabled",
        "reviewed", "reviewed", "BEARER_API_CREDENTIAL", "reviewed", "reviewed", LocalDate.parse("2026-08-28"),
        "owner", LocalDate.parse("2026-09-28"), listOf("reference"), listOf("change"),
        FIDELITY_FIRST_TRANSCRIPTION_PROFILE_ID, FIDELITY_FIRST_INSTRUCTION_SHA256, FIDELITY_FIRST_SCHEMA_SHA256,
        DIRECT_AUTHORITATIVE_PROCESSING_PROFILE_ID, ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING,
        "none", "high", "original",
    )
    private fun request(media: String = "application/pdf"): ExternalTranscriptionRequest {
        val bytes = "synthetic-source".toByteArray()
        val digest = OcrSha256Digest(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })
        val representation = OcrProcessingRepresentation(bytes, OcrProcessingProvenance(
            EvidenceArtifactId("synthetic-evidence"), digest, media, bytes.size.toLong(), null, null,
            media, bytes.size.toLong(), digest, true, DIRECT_AUTHORITATIVE_PROCESSING_PROFILE_ID, Instant.EPOCH,
        ))
        return ExternalTranscriptionRequest(representation, 200, 1,
            ExternalTranscriptionExecutionBinding("request-1", "attempt-1", FIDELITY_FIRST_TRANSCRIPTION_PROFILE_ID))
    }
    private fun payload() = """{"profile_id":"openai-fidelity-first-transcription-v1","request_id":"request-1","attempt_id":"attempt-1","document_outcome":"TRANSCRIBED_WITH_QUALIFICATIONS","completeness_state":"COMPLETE","requested_pages":[1],"returned_pages":[1],"pages":[{"page_number":1,"outcome":"TRANSCRIBED_WITH_QUALIFICATIONS","reason_classification":null,"reason_detail":null,"blocks":[{"block_order":1,"kind":"PRINTED_TEXT","text":"Alpha"},{"block_order":2,"kind":"HANDWRITING","text":"Beta?"}],"warnings":[],"uncertainties":[{"category":"UNCERTAIN_CHARACTER","block_order":2,"observed_text":"?","disclosure":"final glyph unclear"}]}],"warnings":[]}"""
    private fun envelope(payload: String) = """{"id":"resp_synthetic_1","model":"gpt-5.6-sol","output":[{"type":"message","content":[{"type":"output_text","text":"${payload.replace("\\", "\\\\").replace("\"", "\\\"")}"}]}]}"""
}
