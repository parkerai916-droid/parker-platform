package parker.core.runtime

import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.composition.OpenAiApiCredential
import parker.core.interfaces.*

/** Offline-only R6.9A replay; the exact branch requires an isolated copied fixture path. */
class R69AConsumedResponseForensicReplayTest {
    private class FixtureTransport(private val response: OpenAiResponsesTransportResponse) : OpenAiResponsesTransport {
        var calls = 0
        override suspend fun execute(request: OpenAiResponsesTransportRequest) = response.also { calls += 1 }
    }

    @Test fun captured_raw_response_reproduces_seventy_zero_length_observations_and_malformed_validation() = runTest {
        val fixture = System.getenv("R69A_PROVIDER_STATE_FIXTURE")?.let(Path::of)
        assumeTrue(fixture != null && Files.isRegularFile(fixture), "isolated R6.9A fixture not supplied")
        val assessment = fixture!!.resolveSibling("assessment.json")
        assumeTrue(Files.isRegularFile(assessment), "isolated R6.9A assessment not supplied")
        val record = obj(obj(RegionJson.parse(Files.readString(fixture))).getValue("record"))
        val raw = Base64.getDecoder().decode(text(record, "raw_base64"))
        assertEquals("500863d65c7f9ca69a66b2ffef3ef8a42b7033903cf1b5a5bd774d9f0decd87f", regionSha256(raw))
        val request = requestFromBinding(obj(record.getValue("request_binding")))
        val transport = FixtureTransport(OpenAiResponsesTransportResponse(integer(record, "http_status"), raw))
        val outcome = OpenAiRegionTranscriptionAdapter(
            OpenAiApiCredential.fromEnvironment("R69A_OFFLINE_SENTINEL")!!, transport,
        ).transcribeWithRawState(request)
        assertEquals(1, transport.calls)
        assertEquals("VALIDATION_MALFORMED_SCHEMA", assertIs<OpenAiRegionAdapterOutcome.Failure>(outcome).code)

        val fromRaw = structuredFromRaw(raw)
        val persisted = obj(obj(obj(RegionJson.parse(Files.readString(assessment))).getValue("assessment"))
            .getValue("exact_structured_state"))
        assertEquals(fromRaw, persisted)
        assertEquals(70, zeroLengthCount(fromRaw))
        assertEquals(24, array(fromRaw, "blocks").size)
        assertIs<RegionTranscriptionValidationOutcome.Valid>(
            RegionTranscriptionValidator().validate(request, removeZeroLength(fromRaw, all = true)))
        assertIs<RegionTranscriptionValidationOutcome.Valid>(
            RegionTranscriptionValidator().validate(request, removeZeroLength(fromRaw, all = false)))
        assertIs<RegionTranscriptionValidationOutcome.Valid>(
            RegionTranscriptionValidator().validate(request, unanchorZeroLength(fromRaw)))
    }

    @Test fun point_range_is_rejected_while_non_empty_and_unanchored_observations_validate() {
        val request = syntheticRequest()
        fun validate(start: Int?, end: Int?) = RegionTranscriptionValidator().validate(request, syntheticWire(start, end))
        assertEquals(RegionTranscriptionRejection.MALFORMED_SCHEMA,
            assertIs<RegionTranscriptionValidationOutcome.Rejected>(validate(1, 1)).reason)
        assertIs<RegionTranscriptionValidationOutcome.Valid>(validate(1, 2))
        assertIs<RegionTranscriptionValidationOutcome.Valid>(validate(null, null))
    }

    private fun requestFromBinding(binding: Map<String, Any?>): RegionTranscriptionRequest {
        val bytes = byteArrayOf(1, 2, 3)
        val targets = array(binding, "targets").map(::obj).map { target ->
            val pageId = PageRepresentationId(text(target, "page_representation_id"))
            val bounds = PixelCropBounds(0, 0, 1, 1)
            val crop = CanonicalPixelDigest(text(target, "crop_digest"))
            val image = RegionTranscriptionImage(pageId, bounds, crop, "image/png", RegionTranscriptionImage.sha256(bytes), bytes)
            RegionTranscriptionTarget(EvidenceArtifactId(text(target, "evidence_artifact_id")), text(target, "source_sha256"),
                pageId, integer(target, "page_number"), PagePixelDimensions(1, 1), SourceRegionId(text(target, "source_region_id")),
                bounds, crop, SourceRegionStructuralClass.TEXT_LIKE, "r69a-offline-replay", 1, image)
        }
        return RegionTranscriptionRequest(text(binding, "correlation_id"), text(binding, "transcription_profile"),
            text(binding, "schema_id"), integer(binding, "schema_version"), text(binding, "schema_sha256"),
            text(binding, "processing_profile"), REGION_LITERAL_TRANSCRIPTION_INSTRUCTION, targets)
    }

    private fun structuredFromRaw(raw: ByteArray): Map<String, Any?> {
        val envelope = obj(RegionJson.parse(raw.toString(Charsets.UTF_8)))
        val output = array(envelope, "output").map(::obj).single { it["type"] == "message" }
        val segment = array(output, "content").map(::obj).single { it["type"] == "output_text" }
        return obj(RegionJson.parse(text(segment, "text")))
    }

    private fun zeroLengthCount(wire: Map<String, Any?>) = array(wire, "blocks").map(::obj).sumOf { block ->
        array(block, "visual_observations").map(::obj).count {
            nullableInteger(it, "start_code_point") == nullableInteger(it, "end_code_point_exclusive")
        }
    }

    private fun removeZeroLength(wire: Map<String, Any?>, all: Boolean) = wire.toMutableMap().also { top ->
        top["blocks"] = array(wire, "blocks").map(::obj).map { block -> block.toMutableMap().also { copy ->
            copy["visual_observations"] = if (all) emptyList<Any?>() else array(block, "visual_observations").map(::obj).filterNot {
                nullableInteger(it, "start_code_point") == nullableInteger(it, "end_code_point_exclusive")
            }
        }}
    }

    private fun unanchorZeroLength(wire: Map<String, Any?>) = wire.toMutableMap().also { top ->
        top["blocks"] = array(wire, "blocks").map(::obj).map { block -> block.toMutableMap().also { copy ->
            copy["visual_observations"] = array(block, "visual_observations").map(::obj).map { observation ->
                observation.toMutableMap().also {
                    if (nullableInteger(it, "start_code_point") == nullableInteger(it, "end_code_point_exclusive")) {
                        it["start_code_point"] = null
                        it["end_code_point_exclusive"] = null
                    }
                }
            }
        }}
    }

    private fun syntheticRequest(): RegionTranscriptionRequest {
        val bytes = byteArrayOf(1); val pageId = PageRepresentationId("b".repeat(64))
        val bounds = PixelCropBounds(0, 0, 1, 1); val crop = CanonicalPixelDigest("c".repeat(64))
        val image = RegionTranscriptionImage(pageId, bounds, crop, "image/png", RegionTranscriptionImage.sha256(bytes), bytes)
        val target = RegionTranscriptionTarget(EvidenceArtifactId("evidence-r69a"), "a".repeat(64), pageId, 1,
            PagePixelDimensions(1, 1), SourceRegionId("d".repeat(64)), bounds, crop,
            SourceRegionStructuralClass.TEXT_LIKE, "r69a", 1, image)
        return RegionTranscriptionRequest("r69a", REGION_TRANSCRIPTION_PROFILE_ID, REGION_TRANSCRIPTION_SCHEMA_ID,
            REGION_TRANSCRIPTION_WIRE_VERSION, REGION_TRANSCRIPTION_SCHEMA_SHA256,
            REGION_TRANSCRIPTION_PROCESSING_PROFILE, REGION_LITERAL_TRANSCRIPTION_INSTRUCTION, listOf(target))
    }

    private fun syntheticWire(start: Int?, end: Int?) = mapOf<String, Any?>(
        "correlation_id" to "r69a", "transcription_profile_id" to REGION_TRANSCRIPTION_PROFILE_ID,
        "schema_id" to REGION_TRANSCRIPTION_SCHEMA_ID, "schema_version" to REGION_TRANSCRIPTION_WIRE_VERSION,
        "provider_provenance" to mapOf("provider" to "OpenAI", "requested_model" to OPENAI_REGION_MODEL,
            "provider_reported_model" to OPENAI_REGION_MODEL, "provider_response_id" to "resp-r69a",
            "adapter_id" to OPENAI_REGION_ADAPTER_ID, "adapter_version" to OPENAI_REGION_ADAPTER_VERSION,
            "parser_id" to OPENAI_REGION_PARSER_ID, "parser_version" to OPENAI_REGION_PARSER_VERSION),
        "blocks" to listOf(mapOf("source_region_id" to "d".repeat(64), "page_number" to 1, "literal_text" to "ab",
            "status" to "TRANSCRIBED", "uncertainties" to emptyList<Any?>(), "warnings" to emptyList<String>(),
            "provider_returned_ordinal" to 1, "visual_observations" to listOf(mapOf("kind" to "LINE_BREAK",
                "start_code_point" to start, "end_code_point_exclusive" to end)))))

    @Suppress("UNCHECKED_CAST") private fun obj(value: Any?) = value as Map<String, Any?>
    @Suppress("UNCHECKED_CAST") private fun array(value: Map<String, Any?>, key: String) = value.getValue(key) as List<Any?>
    private fun text(value: Map<String, Any?>, key: String) = value.getValue(key) as String
    private fun integer(value: Map<String, Any?>, key: String) = (value.getValue(key) as BigDecimal).intValueExact()
    private fun nullableInteger(value: Map<String, Any?>, key: String) = (value[key] as? BigDecimal)?.intValueExact()
}
