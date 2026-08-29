package parker.core.runtime

import java.net.URI
import java.net.http.HttpTimeoutException
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import org.junit.jupiter.api.Test
import parker.composition.OpenAiApiCredential
import parker.composition.ExternalTranscriptionAcceptanceState
import parker.core.interfaces.*

class OpenAiRegionTranscriptionAdapterTest {
    private val secret = "R64_API_KEY_SENTINEL"
    private val credential = OpenAiApiCredential.fromEnvironment(secret)!!

    private class FakeTransport(private val action: (OpenAiResponsesTransportRequest) -> OpenAiResponsesTransportResponse) : OpenAiResponsesTransport {
        var calls = 0; lateinit var request: OpenAiResponsesTransportRequest
        override suspend fun execute(request: OpenAiResponsesTransportRequest): OpenAiResponsesTransportResponse { calls++; this.request = request; return action(request) }
    }

    @Test fun `profile and frozen instruction schema identities are exact`() {
        val profile = OpenAiRegionTranscriptionProfile()
        assertEquals(OPENAI_REGION_ADAPTER_ID, profile.adapterId); assertEquals("4.0.0", profile.adapterVersion)
        assertEquals("gpt-5.6-sol", profile.model); assertEquals("none", profile.reasoning); assertFalse(profile.store)
        assertEquals("original", profile.imageDetail); assertEquals(ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING, profile.lifecycle)
        assertEquals(REGION_TRANSCRIPTION_SCHEMA_SHA256, OPENAI_REGION_WIRE_SCHEMA_SHA256)
        assertEquals(OPENAI_REGION_INSTRUCTION_SHA256, regionSha256(OPENAI_REGION_INSTRUCTION.toByteArray()))
        assertNotEquals("38e4b87e3a429dac8ed5de91e5e2c94ad3d10cd739db186d41d09ed940b11b88", OPENAI_REGION_INSTRUCTION_SHA256)
    }

    @Test fun `exact request uses Responses PNG original detail strict schema and no native text`() = runTest {
        val transport = FakeTransport { OpenAiResponsesTransportResponse(200, envelope(validWire()).toByteArray()) }
        val req = request(target(ONE, 2))
        val outcome = adapter(transport).transcribeWithRawState(req)
        assertIs<OpenAiRegionAdapterOutcome.Success>(outcome); assertEquals(1, transport.calls)
        assertEquals(URI("https://api.openai.com/v1/responses"), transport.request.endpoint)
        val body = transport.request.body
        listOf("\"model\":\"gpt-5.6-sol\"", "\"store\":false", "\"effort\":\"none\"", "\"strict\":true",
            "\"detail\":\"original\"", "data:image/png;base64,AQID", ONE, "page_number=2", "crop_digest=" + "c".repeat(64)).forEach { assertTrue(body.contains(it), "missing $it") }
        assertTrue(body.contains(OPENAI_REGION_INSTRUCTION)); assertTrue(body.contains(REGION_TRANSCRIPTION_SCHEMA_ID))
        listOf("native text", "pdf text", "previous transcription", secret, "Authorization").forEach { assertFalse(body.contains(it, ignoreCase = true)) }
    }

    @Test fun `optional page context is explicit and target-limited`() = runTest {
        val t = target(ONE, 2, context = true); val transport = FakeTransport { OpenAiResponsesTransportResponse(200, envelope(validWire()).toByteArray()) }
        assertIs<OpenAiRegionAdapterOutcome.Success>(adapter(transport).transcribeWithRawState(request(t)))
        assertEquals(2, Regex("data:image/png;base64,").findAll(transport.request.body).count())
        assertTrue(transport.request.body.contains("PAGE_CONTEXT_ONLY")); assertTrue(transport.request.body.contains("do not transcribe outside target"))
    }

    @Test fun `valid response preserves literal Unicode uncertainty provenance raw bytes and digest`() = runTest {
        val wire = validWire(text = "A𝄞B\n  12—34", status = "PARTIALLY_TRANSCRIBED", uncertainty = true)
        val raw = envelope(wire)
        val outcome = assertIs<OpenAiRegionAdapterOutcome.Success>(adapter(FakeTransport { OpenAiResponsesTransportResponse(200, raw.toByteArray()) }).transcribeWithRawState(request(target(ONE, 2))))
        val block = outcome.result.blocksInProviderOrder.single()
        assertEquals("A𝄞B\n  12—34", block.literalText); assertEquals("𝄞", block.uncertainties.single().exactSubstring)
        assertEquals("OpenAI", outcome.result.providerProvenance.provider); assertEquals("resp-r64", outcome.rawState.responseId)
        assertEquals(OPENAI_REGION_MODEL, outcome.rawState.providerReportedModel); assertContentEquals(raw.toByteArray(), outcome.rawState.responseBody())
        assertEquals(regionSha256(raw.toByteArray()), outcome.rawState.responseSha256)
    }

    @Test fun `reversed A1 provider order remains exact forensic order`() = runTest {
        val req = request(target(PAGE2, 2), target(PAGE3, 3))
        val blocks = listOf(block(PAGE3, 3, "AUTHORIZATION_TOKEN", 1), block(PAGE2, 2, "PROPOSITION_TOKEN", 2))
        val outcome = assertIs<OpenAiRegionAdapterOutcome.Success>(adapter(FakeTransport { OpenAiResponsesTransportResponse(200, envelope(wire(blocks)).toByteArray()) }).transcribeWithRawState(req))
        assertEquals(listOf(PAGE3, PAGE2), outcome.result.blocksInProviderOrder.map { it.sourceRegionId.value })
        assertTrue(adapter(FakeTransport { error("unused") }).buildRequestBody(req).contains(PAGE2))
        assertTrue(adapter(FakeTransport { error("unused") }).buildRequestBody(req).contains(PAGE3))
    }

    @Test fun `HTTP timeout transport malformed refusal missing model and excess fail closed without secrets`() = runTest {
        suspend fun failure(transport: FakeTransport, max: Long = 20L * 1024 * 1024) = assertIs<OpenAiRegionAdapterOutcome.Failure>(adapter(transport, max).transcribeWithRawState(request(target(ONE, 2))))
        assertEquals("PROVIDER_AUTHENTICATION_FAILURE", failure(FakeTransport { OpenAiResponsesTransportResponse(401, "secret body".toByteArray()) }).code)
        assertEquals("PROVIDER_TIMEOUT", failure(FakeTransport { throw HttpTimeoutException("timeout $secret") }).code)
        assertEquals("PROVIDER_TRANSPORT_FAILURE", failure(FakeTransport { throw IllegalStateException(secret) }).code)
        assertEquals("MALFORMED_PROVIDER_RESPONSE", failure(FakeTransport { OpenAiResponsesTransportResponse(200, "not-json".toByteArray()) }).code)
        assertEquals("PROVIDER_REFUSAL", failure(FakeTransport { OpenAiResponsesTransportResponse(200, refusal().toByteArray()) }).code)
        assertEquals("PROVIDER_MODEL_MISMATCH", failure(FakeTransport { OpenAiResponsesTransportResponse(200, envelope(validWire(), model = "wrong-model").toByteArray()) }).code)
        assertEquals("MISSING_STRUCTURED_OUTPUT", failure(FakeTransport { OpenAiResponsesTransportResponse(200, "{\"id\":\"x\",\"model\":\"gpt-5.6-sol\",\"output\":[]}".toByteArray()) }).code)
        assertEquals("EXCESSIVE_PROVIDER_OUTPUT", failure(FakeTransport { OpenAiResponsesTransportResponse(200, envelope(validWire(text = "x".repeat(1000))).toByteArray()) }, 500).code)
        listOf(failure(FakeTransport { OpenAiResponsesTransportResponse(500, secret.toByteArray()) }), failure(FakeTransport { throw IllegalStateException(secret) })).forEach { assertFalse(it.toString().contains(secret)) }
    }

    @Test fun `schema validator failures propagate without adapter repair`() = runTest {
        suspend fun rejected(w: Map<String, Any?>): String = assertIs<OpenAiRegionAdapterOutcome.Failure>(adapter(FakeTransport { OpenAiResponsesTransportResponse(200, envelope(w).toByteArray()) }).transcribeWithRawState(request(target(ONE, 2)))).code
        val cases = linkedMapOf(
            "VALIDATION_UNKNOWN_REGION" to validWire(id = TWO),
            "VALIDATION_DUPLICATE_REGION" to wire(listOf(block(ONE, 2, "x", 1), block(ONE, 2, "y", 2))),
            "VALIDATION_PAGE_MISMATCH" to validWire(page = 9),
            "VALIDATION_INVALID_UNCERTAINTY" to validWire(text = "abc", status = "PARTIALLY_TRANSCRIBED", uncertainty = true, invalidSpan = true),
            "VALIDATION_INVALID_STATUS_TEXT" to validWire(text = "invented", status = "NO_VISIBLE_TEXT"),
        )
        cases.forEach { (expected, value) -> assertEquals(expected, rejected(value)) }
        val twoRegionRequest = request(target(ONE, 2), target(TWO, 3))
        val missing = assertIs<OpenAiRegionAdapterOutcome.Failure>(adapter(FakeTransport { OpenAiResponsesTransportResponse(200, envelope(validWire()).toByteArray()) }).transcribeWithRawState(twoRegionRequest))
        assertEquals("VALIDATION_MISSING_REGION", missing.code)
        val extra = validWire().toMutableMap(); extra["unexpected"] = true
        assertEquals("VALIDATION_ADDITIONAL_FIELD", rejected(extra))
        val malformedStructured = envelopeText("{truncated")
        val failure = assertIs<OpenAiRegionAdapterOutcome.Failure>(adapter(FakeTransport { OpenAiResponsesTransportResponse(200, malformedStructured.toByteArray()) }).transcribeWithRawState(request(target(ONE, 2))))
        assertEquals("SCHEMA_INVALID_RESPONSE", failure.code)
    }

    private fun adapter(transport: FakeTransport, max: Long = 20L * 1024 * 1024) = OpenAiRegionTranscriptionAdapter(credential, transport, maximumResponseBytes = max)
    private fun request(vararg targets: RegionTranscriptionTarget) = RegionTranscriptionRequest("r64-correlation", REGION_TRANSCRIPTION_PROFILE_ID, REGION_TRANSCRIPTION_SCHEMA_ID,
        REGION_TRANSCRIPTION_WIRE_VERSION, REGION_TRANSCRIPTION_SCHEMA_SHA256, REGION_TRANSCRIPTION_PROCESSING_PROFILE, REGION_LITERAL_TRANSCRIPTION_INSTRUCTION, targets.toList())
    private fun target(id: String, page: Int, context: Boolean = false): RegionTranscriptionTarget {
        val bounds = PixelCropBounds(10, 20, 110, 80); val crop = CanonicalPixelDigest("c".repeat(64)); val bytes = byteArrayOf(1, 2, 3)
        val region = RegionTranscriptionImage(pageId(), bounds, crop, "image/png", RegionTranscriptionImage.sha256(bytes), bytes)
        val pageImage = if (context) RegionTranscriptionImage(pageId(), PixelCropBounds(0, 0, 1000, 1400), CanonicalPixelDigest("d".repeat(64)), "image/png", RegionTranscriptionImage.sha256(bytes), bytes) else null
        return RegionTranscriptionTarget(EvidenceArtifactId("evidence-r64"), "a".repeat(64), pageId(), page, PagePixelDimensions(1000, 1400), SourceRegionId(id), bounds, crop,
            SourceRegionStructuralClass.TEXT_LIKE, "pixel-whitespace-source-regions-v1", 1, region, pageImage)
    }
    private fun validWire(id: String = ONE, page: Int = 2, text: String? = "literal", status: String = "TRANSCRIBED", uncertainty: Boolean = false, invalidSpan: Boolean = false) =
        wire(listOf(block(id, page, text, 1, status, uncertainty, invalidSpan)))
    private fun wire(blocks: List<Map<String, Any?>>) = linkedMapOf<String, Any?>("correlation_id" to "r64-correlation", "transcription_profile_id" to REGION_TRANSCRIPTION_PROFILE_ID,
        "schema_id" to REGION_TRANSCRIPTION_SCHEMA_ID, "schema_version" to REGION_TRANSCRIPTION_WIRE_VERSION,
        "provider_provenance" to mapOf("provider" to "OpenAI", "requested_model" to OPENAI_REGION_MODEL, "provider_reported_model" to OPENAI_REGION_MODEL,
            "provider_response_id" to "resp-r64", "adapter_id" to OPENAI_REGION_ADAPTER_ID, "adapter_version" to OPENAI_REGION_ADAPTER_VERSION,
            "parser_id" to OPENAI_REGION_PARSER_ID, "parser_version" to OPENAI_REGION_PARSER_VERSION), "blocks" to blocks)
    private fun block(id: String, page: Int, text: String?, ordinal: Int, status: String = "TRANSCRIBED", uncertainty: Boolean = false, invalidSpan: Boolean = false): Map<String, Any?> {
        val spans = if (uncertainty) listOf(mapOf<String, Any?>("start_code_point" to 1, "end_code_point_exclusive" to if (invalidSpan) 99 else 2,
            "exact_substring" to if (text?.contains("𝄞") == true) "𝄞" else "b", "category" to "AMBIGUOUS_CHARACTER", "alternatives" to emptyList<String>(), "provider_confidence" to null)) else emptyList()
        return mapOf("source_region_id" to id, "page_number" to page, "literal_text" to text, "status" to status, "uncertainties" to spans,
            "warnings" to emptyList<String>(), "provider_returned_ordinal" to ordinal, "visual_observations" to emptyList<Any>())
    }
    private fun envelope(wire: Map<String, Any?>, model: String = OPENAI_REGION_MODEL) = envelopeText(RegionJson.encode(wire), model)
    private fun envelopeText(text: String, model: String = OPENAI_REGION_MODEL) = RegionJson.encode(mapOf("id" to "resp-r64", "model" to model, "status" to "completed",
        "output" to listOf(mapOf("type" to "message", "content" to listOf(mapOf("type" to "output_text", "text" to text))))))
    private fun refusal() = RegionJson.encode(mapOf("id" to "resp-r64", "model" to OPENAI_REGION_MODEL, "output" to listOf(mapOf("type" to "message", "content" to listOf(mapOf("type" to "refusal", "refusal" to secret))))))
    private fun pageId() = PageRepresentationId("b".repeat(64))
    companion object {
        val ONE = "1f" + "0".repeat(62); val TWO = "2f" + "0".repeat(62)
        const val PAGE2 = "5dfb6c252dd668e7ae9dc1be95c8243c4505916e825ca143a0b02e543a9ab668"
        const val PAGE3 = "e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff"
    }
}
