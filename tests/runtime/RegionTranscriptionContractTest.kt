package parker.core.runtime

import kotlin.test.*
import org.junit.jupiter.api.Test
import parker.core.interfaces.*

class RegionTranscriptionContractTest {
    private val validator = RegionTranscriptionValidator()

    @Test fun `request construction cryptographically binds source regions and defensive images`() {
        val request = request(target(ONE, 2))
        assertEquals(REGION_TRANSCRIPTION_SCHEMA_SHA256, request.schemaSha256)
        assertEquals(64, request.schemaSha256.length)
        val bytes = request.targets.single().regionImage.encodedBytes(); bytes[0] = 0
        assertContentEquals(byteArrayOf(1, 2, 3), request.targets.single().regionImage.encodedBytes())
        assertFailsWith<IllegalArgumentException> { request(target(ONE, 2).copy(cropDigest = digest('f'))) }
    }

    @Test fun `valid literal uncertain illegible no-text table and handwriting fixtures validate`() {
        assertValid(request(target(ONE, 1)), response(block(ONE, 1, "Exact “text”\n  12—34", "TRANSCRIBED", 1)))
        assertValid(request(target(ONE, 1)), response(block(ONE, 1, "A𝄞B", "PARTIALLY_TRANSCRIBED", 1,
            uncertainties = listOf(uncertainty(1, 2, "𝄞", "AMBIGUOUS_CHARACTER")))))
        assertValid(request(target(ONE, 1)), response(block(ONE, 1, null, "ILLEGIBLE", 1)))
        assertValid(request(target(ONE, 1)), response(block(ONE, 1, null, "NO_VISIBLE_TEXT", 1)))
        assertValid(request(target(ONE, 1, SourceRegionStructuralClass.TABLE_LIKE)), response(block(ONE, 1, "A | B\n1 | 2", "TRANSCRIBED", 1,
            observations = listOf(observation("TABLE_CELL_TEXT", 0, 1)))))
        assertValid(request(target(ONE, 1)), response(block(ONE, 1, "Jo?n", "PARTIALLY_TRANSCRIBED", 1,
            uncertainties = listOf(uncertainty(2, 3, "?", "HANDWRITING_UNCERTAIN")))))
    }

    @Test fun `exact accounting rejects missing duplicate unknown wrong-page and additional fields`() {
        val req = request(target(ONE, 2), target(TWO, 3))
        assertRejected(RegionTranscriptionRejection.MISSING_REGION, req, response(block(ONE, 2, "x", "TRANSCRIBED", 1)))
        assertRejected(RegionTranscriptionRejection.DUPLICATE_REGION, req, response(block(ONE, 2, "x", "TRANSCRIBED", 1), block(ONE, 2, "y", "TRANSCRIBED", 2)))
        assertRejected(RegionTranscriptionRejection.UNKNOWN_REGION, req, response(block(ONE, 2, "x", "TRANSCRIBED", 1), block(THREE, 3, "y", "TRANSCRIBED", 2)))
        assertRejected(RegionTranscriptionRejection.PAGE_MISMATCH, req, response(block(ONE, 9, "x", "TRANSCRIBED", 1), block(TWO, 3, "y", "TRANSCRIBED", 2)))
        val extra = response(block(ONE, 2, "x", "TRANSCRIBED", 1), block(TWO, 3, "y", "TRANSCRIBED", 2)).toMutableMap(); extra["invented"] = "field"
        assertRejected(RegionTranscriptionRejection.ADDITIONAL_FIELD, req, extra)
    }

    @Test fun `invalid spans unicode status text ordinals and excessive output fail closed`() {
        val req = request(target(ONE, 1))
        assertRejected(RegionTranscriptionRejection.INVALID_UNCERTAINTY, req, response(block(ONE, 1, "abc", "PARTIALLY_TRANSCRIBED", 1, listOf(uncertainty(2, 4, "c", "CLIPPED")))))
        assertRejected(RegionTranscriptionRejection.INVALID_UNICODE, req, response(block(ONE, 1, "\uD800", "TRANSCRIBED", 1)))
        assertRejected(RegionTranscriptionRejection.INVALID_STATUS_TEXT, req, response(block(ONE, 1, "invented", "NO_VISIBLE_TEXT", 1)))
        assertRejected(RegionTranscriptionRejection.INVALID_ORDINAL, req, response(block(ONE, 1, "x", "TRANSCRIBED", 2)))
        assertRejected(RegionTranscriptionRejection.INVALID_STATUS, req, response(block(ONE, 1, "x", "GUESSED", 1)))
        assertRejected(RegionTranscriptionRejection.EXCESSIVE_TEXT, req, response(block(ONE, 1, "x".repeat(100_001), "TRANSCRIBED", 1)))
        val excessive = response(*List(33) { block((it + 1).toString(16).padStart(64, '0'), 1, "x", "TRANSCRIBED", it + 1) }.toTypedArray())
        assertRejected(RegionTranscriptionRejection.EXCESSIVE_BLOCK_COUNT, req, excessive)
    }

    @Test fun `provider block reversal preserves Parker identities and never becomes source order`() {
        val req = request(target(ONE, 2), target(TWO, 3))
        val valid = assertIs<RegionTranscriptionValidationOutcome.Valid>(validator.validate(req,
            response(block(TWO, 3, "second source region", "TRANSCRIBED", 1), block(ONE, 2, "first source region", "TRANSCRIBED", 2))))
        assertEquals(listOf(TWO, ONE), valid.result.blocksInProviderOrder.map { it.sourceRegionId.value })
        assertEquals(setOf(ONE, TWO), valid.result.blocksInProviderOrder.map { it.sourceRegionId.value }.toSet())
    }

    @Test fun `A1 page 2 proposition and page 3 authorization remain bound when provider order reverses`() {
        val page2Proposition = "5dfb6c252dd668e7ae9dc1be95c8243c4505916e825ca143a0b02e543a9ab668"
        val page3Authorization = "e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff"
        val req = request(target(page2Proposition, 2), target(page3Authorization, 3))
        val valid = assertIs<RegionTranscriptionValidationOutcome.Valid>(validator.validate(req, response(
            block(page3Authorization, 3, "SYNTHETIC_AUTHORIZATION_TOKEN", "TRANSCRIBED", 1),
            block(page2Proposition, 2, "SYNTHETIC_PROPOSITION_TOKEN", "TRANSCRIBED", 2))))
        assertEquals(page2Proposition, valid.result.blocksInProviderOrder.single { it.literalText!!.contains("PROPOSITION") }.sourceRegionId.value)
        assertEquals(page3Authorization, valid.result.blocksInProviderOrder.single { it.literalText!!.contains("AUTHORIZATION") }.sourceRegionId.value)
    }

    @Test fun `schema identity digest and serialization source are deterministic and strict`() {
        val digests = (1..10).map { RegionTranscriptionImage.sha256(REGION_TRANSCRIPTION_SCHEMA_SOURCE.toByteArray()) }
        assertEquals(setOf(REGION_TRANSCRIPTION_SCHEMA_SHA256), digests.toSet())
        assertTrue(REGION_TRANSCRIPTION_SCHEMA_SOURCE.contains("\"additionalProperties\":false"))
        assertTrue(REGION_TRANSCRIPTION_SCHEMA_SOURCE.contains("\"maxItems\":32"))
    }

    private fun target(id: String, page: Int, cls: SourceRegionStructuralClass = SourceRegionStructuralClass.TEXT_LIKE): RegionTranscriptionTarget {
        val bounds = PixelCropBounds(10, 20, 110, 80); val bytes = byteArrayOf(1, 2, 3); val crop = digest('c')
        val image = RegionTranscriptionImage(pageId(), bounds, crop, "image/png", RegionTranscriptionImage.sha256(bytes), bytes)
        return RegionTranscriptionTarget(EvidenceArtifactId("evidence-r63"), "a".repeat(64), pageId(), page, PagePixelDimensions(1000, 1400), SourceRegionId(id), bounds, crop, cls, "pixel-whitespace-source-regions-v1", 1, image)
    }
    private fun request(vararg targets: RegionTranscriptionTarget) = RegionTranscriptionRequest("request-r63", REGION_TRANSCRIPTION_PROFILE_ID, REGION_TRANSCRIPTION_SCHEMA_ID,
        REGION_TRANSCRIPTION_WIRE_VERSION, REGION_TRANSCRIPTION_SCHEMA_SHA256, REGION_TRANSCRIPTION_PROCESSING_PROFILE, REGION_LITERAL_TRANSCRIPTION_INSTRUCTION, targets.toList())
    private fun response(vararg blocks: Map<String, Any?>) = mapOf<String, Any?>(
        "correlation_id" to "request-r63", "transcription_profile_id" to REGION_TRANSCRIPTION_PROFILE_ID, "schema_id" to REGION_TRANSCRIPTION_SCHEMA_ID,
        "schema_version" to REGION_TRANSCRIPTION_WIRE_VERSION, "provider_provenance" to mapOf("provider" to "synthetic", "requested_model" to "fixture",
            "provider_reported_model" to null, "provider_response_id" to "response-fixture", "adapter_id" to "synthetic-adapter", "adapter_version" to "1",
            "parser_id" to "region-schema-parser", "parser_version" to "1"), "blocks" to blocks.toList())
    private fun block(id: String, page: Int, text: String?, status: String, ordinal: Int,
        uncertainties: List<Map<String, Any?>> = emptyList(), observations: List<Map<String, Any?>> = emptyList()) = mapOf<String, Any?>(
        "source_region_id" to id, "page_number" to page, "literal_text" to text, "status" to status, "uncertainties" to uncertainties,
        "warnings" to emptyList<String>(), "provider_returned_ordinal" to ordinal, "visual_observations" to observations)
    private fun uncertainty(start: Int, end: Int, substring: String, category: String) = mapOf<String, Any?>("start_code_point" to start,
        "end_code_point_exclusive" to end, "exact_substring" to substring, "category" to category, "alternatives" to emptyList<String>(), "provider_confidence" to null)
    private fun observation(kind: String, start: Int?, end: Int?) = mapOf<String, Any?>("kind" to kind, "start_code_point" to start, "end_code_point_exclusive" to end)
    private fun assertValid(request: RegionTranscriptionRequest, wire: Map<String, Any?>) { assertIs<RegionTranscriptionValidationOutcome.Valid>(validator.validate(request, wire)) }
    private fun assertRejected(reason: RegionTranscriptionRejection, request: RegionTranscriptionRequest, wire: Map<String, Any?>) {
        assertEquals(reason, assertIs<RegionTranscriptionValidationOutcome.Rejected>(validator.validate(request, wire)).reason)
    }
    private fun pageId() = PageRepresentationId("b".repeat(64)); private fun digest(c: Char) = CanonicalPixelDigest(c.toString().repeat(64))
    companion object {
        val ONE = "1f" + "0".repeat(62); val TWO = "2f" + "0".repeat(62); val THREE = "3f" + "0".repeat(62)
    }
}
