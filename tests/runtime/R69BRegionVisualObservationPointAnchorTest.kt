package parker.core.runtime

import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.test.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.core.interfaces.*

/** FA.9.4P-A1E-R6.9B: network-free point-anchor semantics and immutable captured-state replay. */
class R69BRegionVisualObservationPointAnchorTest {
    private val validator = RegionTranscriptionValidator()

    @Test fun three_anchor_forms_and_invalid_bounds_are_governed() {
        assertValid(observation("LINE_BREAK", null, null))
        assertValid(observation("LINE_BREAK", 0, 0))
        assertValid(observation("LINE_BREAK", 1, 1))
        assertValid(observation("LINE_BREAK", 2, 2))
        assertValid(observation("BOLD", 0, 2))
        assertRejected(observation("LINE_BREAK", null, 1))
        assertRejected(observation("LINE_BREAK", 1, null))
        assertRejected(observation("LINE_BREAK", 2, 1))
        assertRejected(observation("LINE_BREAK", -1, -1))
        assertRejected(observation("LINE_BREAK", -1, 0))
        assertRejected(observation("LINE_BREAK", 3, 3))
        assertRejected(observation("BOLD", 0, 3))
    }

    @Test fun observation_kind_semantics_are_not_broadened() {
        assertRejected(observation("PARAGRAPH_BREAK", 1, 1))
        assertRejected(observation("LINE_BREAK", 0, 1))
        assertValid(observation("PARAGRAPH_BREAK", 0, 1))
        assertRejected(observation("BOLD", 1, 1))
        assertValid(observation("BOLD", null, null))
        assertValid(observation("TABLE_CELL_TEXT", 0, 2))
    }

    @Test fun uncertainty_spans_remain_non_empty() {
        val uncertainty = mapOf<String, Any?>(
            "start_code_point" to 1, "end_code_point_exclusive" to 1, "exact_substring" to "",
            "category" to "AMBIGUOUS_CHARACTER", "alternatives" to emptyList<String>(),
            "provider_confidence" to null,
        )
        val rejected = validator.validate(request(), wire(
            status = "PARTIALLY_TRANSCRIBED", uncertainties = listOf(uncertainty)))
        assertEquals(RegionTranscriptionRejection.INVALID_UNCERTAINTY,
            assertIs<RegionTranscriptionValidationOutcome.Rejected>(rejected).reason)
    }

    @Test fun historical_v4_semantics_are_preserved_and_superseding_mode_is_explicit() {
        val historical = request(REGION_TRANSCRIPTION_WIRE_VERSION_V4, REGION_TRANSCRIPTION_SCHEMA_SHA256_V4)
        val point = wire(observation = observation("LINE_BREAK", 1, 1),
            version = REGION_TRANSCRIPTION_WIRE_VERSION_V4)
        assertEquals(RegionTranscriptionRejection.MALFORMED_SCHEMA,
            assertIs<RegionTranscriptionValidationOutcome.Rejected>(
                RegionTranscriptionValidator().validate(historical, point)).reason)
        assertIs<RegionTranscriptionValidationOutcome.Valid>(
            RegionTranscriptionValidator(RegionVisualObservationSemantics.V5_EXPLICIT_ANCHORS)
                .validate(historical, point))
    }

    @Test fun schema_instruction_and_profile_identities_are_distinct_and_deterministic() {
        assertEquals(4, REGION_TRANSCRIPTION_WIRE_VERSION_V4)
        assertEquals(5, REGION_TRANSCRIPTION_WIRE_VERSION)
        assertEquals("672a626bd8a6183ff636a4617d017706897fe658e0036f3693c51d5c0d8bfad1",
            REGION_TRANSCRIPTION_SCHEMA_SHA256_V4)
        assertNotEquals(REGION_TRANSCRIPTION_SCHEMA_SHA256_V4, REGION_TRANSCRIPTION_SCHEMA_SHA256)
        assertEquals(REGION_TRANSCRIPTION_SCHEMA_SHA256,
            regionSha256(REGION_TRANSCRIPTION_SCHEMA_SOURCE.toByteArray()))
        assertEquals(OPENAI_REGION_INSTRUCTION_SHA256,
            regionSha256(OPENAI_REGION_INSTRUCTION.toByteArray()))
        assertNotEquals(OPENAI_REGION_INSTRUCTION_SHA256_V4, OPENAI_REGION_INSTRUCTION_SHA256)
        assertEquals("3.0.0", OPENAI_REGION_ADAPTER_VERSION_V4)
        assertEquals("4.0.0", OPENAI_REGION_ADAPTER_VERSION)
        assertEquals("openai-region-anchored-transcription-v1", OPENAI_REGION_PROFILE_ID_V4)
        assertEquals("openai-region-anchored-transcription-v2", OPENAI_REGION_PROFILE_ID)
        assertTrue(OPENAI_REGION_INSTRUCTION.contains("zero-width point anchor [n,n)"))
        assertTrue(REGION_TRANSCRIPTION_SCHEMA_SOURCE.contains("\"schema_version\":{\"const\":5"))
        assertTrue(REGION_TRANSCRIPTION_SCHEMA_SOURCE.contains(
            "\"end_code_point_exclusive\":{\"minimum\":0,\"type\":[\"integer\",\"null\"]}"))
    }

    @Test fun exact_consumed_state_validates_offline_without_mutation() {
        val fixture = System.getenv("R69B_PROVIDER_STATE_FIXTURE")?.let(Path::of)
        assumeTrue(fixture != null && Files.isRegularFile(fixture), "isolated R6.9B fixture not supplied")
        val assessmentPath = fixture!!.resolveSibling("assessment.json")
        assumeTrue(Files.isRegularFile(assessmentPath), "isolated R6.9B assessment not supplied")
        val record = obj(obj(RegionJson.parse(Files.readString(fixture))).getValue("record"))
        val raw = Base64.getDecoder().decode(text(record, "raw_base64"))
        assertEquals("500863d65c7f9ca69a66b2ffef3ef8a42b7033903cf1b5a5bd774d9f0decd87f",
            regionSha256(raw))
        val assessment = obj(obj(RegionJson.parse(Files.readString(assessmentPath))).getValue("assessment"))
        val structured = obj(assessment.getValue("exact_structured_state"))
        val canonicalBefore = RegionJson.encode(structured)
        val historicalRequest = requestFromBinding(obj(record.getValue("request_binding")))

        val outcome = RegionTranscriptionValidator(RegionVisualObservationSemantics.V5_EXPLICIT_ANCHORS)
            .validate(historicalRequest, structured)
        val valid = assertIs<RegionTranscriptionValidationOutcome.Valid>(outcome, "outcome=$outcome")
        assertEquals(24, valid.result.blocksInProviderOrder.size)
        assertEquals(24, valid.result.blocksInProviderOrder.map { it.sourceRegionId }.distinct().size)
        assertEquals(24, valid.result.blocksInProviderOrder.count {
            it.status == RegionTranscriptionStatus.TRANSCRIBED && !it.literalText.isNullOrEmpty()
        })
        val observations = valid.result.blocksInProviderOrder.flatMap { it.visualObservations }
        val lineBreaks = observations.filter { it.kind == RegionVisualObservationKind.LINE_BREAK }
        assertEquals(70, lineBreaks.size)
        assertTrue(lineBreaks.all {
            it.kind == RegionVisualObservationKind.LINE_BREAK &&
                it.startCodePoint == it.endCodePointExclusive
        })
        assertEquals(canonicalBefore, RegionJson.encode(structured))
        assertEquals("7031179aa4267fdc12a50a429eef184e4ecfb2efb3ae993b6a5527ecf9f4c476",
            assessment.getValue("structured_sha256"))
    }

    private fun assertValid(observation: Map<String, Any?>) {
        assertIs<RegionTranscriptionValidationOutcome.Valid>(validator.validate(request(), wire(observation)))
    }
    private fun assertRejected(observation: Map<String, Any?>) {
        assertEquals(RegionTranscriptionRejection.INVALID_VISUAL_OBSERVATION,
            assertIs<RegionTranscriptionValidationOutcome.Rejected>(
                validator.validate(request(), wire(observation))).reason)
    }

    private fun request(version: Int = REGION_TRANSCRIPTION_WIRE_VERSION,
        digest: String = REGION_TRANSCRIPTION_SCHEMA_SHA256): RegionTranscriptionRequest {
        val bytes = byteArrayOf(1); val page = PageRepresentationId("b".repeat(64))
        val bounds = PixelCropBounds(0, 0, 1, 1); val crop = CanonicalPixelDigest("c".repeat(64))
        val image = RegionTranscriptionImage(page, bounds, crop, "image/png",
            RegionTranscriptionImage.sha256(bytes), bytes)
        val target = RegionTranscriptionTarget(EvidenceArtifactId("evidence-r69b"), "a".repeat(64), page, 1,
            PagePixelDimensions(1, 1), SourceRegionId("d".repeat(64)), bounds, crop,
            SourceRegionStructuralClass.TEXT_LIKE, "r69b", 1, image)
        return RegionTranscriptionRequest("r69b", REGION_TRANSCRIPTION_PROFILE_ID, REGION_TRANSCRIPTION_SCHEMA_ID,
            version, digest, REGION_TRANSCRIPTION_PROCESSING_PROFILE, REGION_LITERAL_TRANSCRIPTION_INSTRUCTION,
            listOf(target))
    }

    private fun wire(observation: Map<String, Any?> = observation("LINE_BREAK", null, null),
        version: Int = REGION_TRANSCRIPTION_WIRE_VERSION, status: String = "TRANSCRIBED",
        uncertainties: List<Map<String, Any?>> = emptyList()) = mapOf<String, Any?>(
        "correlation_id" to "r69b", "transcription_profile_id" to REGION_TRANSCRIPTION_PROFILE_ID,
        "schema_id" to REGION_TRANSCRIPTION_SCHEMA_ID, "schema_version" to version,
        "provider_provenance" to mapOf("provider" to "OpenAI", "requested_model" to OPENAI_REGION_MODEL,
            "provider_reported_model" to OPENAI_REGION_MODEL, "provider_response_id" to "resp-r69b",
            "adapter_id" to OPENAI_REGION_ADAPTER_ID, "adapter_version" to OPENAI_REGION_ADAPTER_VERSION,
            "parser_id" to OPENAI_REGION_PARSER_ID, "parser_version" to OPENAI_REGION_PARSER_VERSION),
        "blocks" to listOf(mapOf("source_region_id" to "d".repeat(64), "page_number" to 1,
            "literal_text" to "ab", "status" to status, "uncertainties" to uncertainties,
            "warnings" to emptyList<String>(), "provider_returned_ordinal" to 1,
            "visual_observations" to listOf(observation))))

    private fun observation(kind: String, start: Int?, end: Int?) = mapOf<String, Any?>(
        "kind" to kind, "start_code_point" to start, "end_code_point_exclusive" to end)

    private fun requestFromBinding(binding: Map<String, Any?>): RegionTranscriptionRequest {
        val bytes = byteArrayOf(1, 2, 3)
        val targets = array(binding, "targets").map(::obj).map { target ->
            val page = PageRepresentationId(text(target, "page_representation_id"))
            val bounds = PixelCropBounds(0, 0, 1, 1); val crop = CanonicalPixelDigest(text(target, "crop_digest"))
            val image = RegionTranscriptionImage(page, bounds, crop, "image/png",
                RegionTranscriptionImage.sha256(bytes), bytes)
            RegionTranscriptionTarget(EvidenceArtifactId(text(target, "evidence_artifact_id")),
                text(target, "source_sha256"), page, integer(target, "page_number"), PagePixelDimensions(1, 1),
                SourceRegionId(text(target, "source_region_id")), bounds, crop,
                SourceRegionStructuralClass.TEXT_LIKE, "r69b-offline", 1, image)
        }
        return RegionTranscriptionRequest(text(binding, "correlation_id"), text(binding, "transcription_profile"),
            text(binding, "schema_id"), integer(binding, "schema_version"), text(binding, "schema_sha256"),
            text(binding, "processing_profile"), REGION_LITERAL_TRANSCRIPTION_INSTRUCTION, targets)
    }

    @Suppress("UNCHECKED_CAST") private fun obj(value: Any?) = value as Map<String, Any?>
    @Suppress("UNCHECKED_CAST") private fun array(value: Map<String, Any?>, key: String) =
        value.getValue(key) as List<Any?>
    private fun text(value: Map<String, Any?>, key: String) = value.getValue(key) as String
    private fun integer(value: Map<String, Any?>, key: String) =
        (value.getValue(key) as BigDecimal).intValueExact()
}
