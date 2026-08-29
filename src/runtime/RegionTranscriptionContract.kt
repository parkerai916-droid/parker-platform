package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.*

const val REGION_LITERAL_TRANSCRIPTION_INSTRUCTION = "Transcribe only text visibly present inside each specified Parker source region. Preserve visible characters, spelling, punctuation, capitalization, line breaks, paragraph breaks, repeated spaces, indentation, symbols, numbers, dates, identifiers, and visibly significant emphasis. Do not summarize, interpret, reason, rewrite, correct, normalize, infer, complete, regroup, or move text between regions. Use only the supplied Parker region identifiers. If text is uncertain or illegible, report bounded uncertainty instead of guessing. If no text is visible, return NO_VISIBLE_TEXT and no caption or description. Full-page context, when supplied, is context only: return no surrounding text. Return only the strict structured response."

val REGION_TRANSCRIPTION_SCHEMA_SOURCE_V4 = """{"additionalProperties":false,"properties":{"blocks":{"items":{"additionalProperties":false,"properties":{"literal_text":{"maxLength":100000,"type":["string","null"]},"page_number":{"maximum":10000,"minimum":1,"type":"integer"},"provider_returned_ordinal":{"maximum":32,"minimum":1,"type":"integer"},"source_region_id":{"pattern":"^[0-9a-f]{64}$","type":"string"},"status":{"enum":["TRANSCRIBED","PARTIALLY_TRANSCRIBED","ILLEGIBLE","NO_VISIBLE_TEXT","UNSUPPORTED_VISUAL_CONTENT"],"type":"string"},"uncertainties":{"items":{"additionalProperties":false,"properties":{"alternatives":{"items":{"maxLength":256,"type":"string"},"maxItems":8,"type":"array"},"category":{"enum":["ILLEGIBLE","AMBIGUOUS_CHARACTER","AMBIGUOUS_WORD","PARTIALLY_OCCLUDED","LOW_CONTRAST","HANDWRITING_UNCERTAIN","CLIPPED","OTHER_VISUAL_UNCERTAINTY"],"type":"string"},"end_code_point_exclusive":{"minimum":1,"type":"integer"},"exact_substring":{"maxLength":4096,"type":"string"},"provider_confidence":{"maxLength":64,"type":["string","null"]},"start_code_point":{"minimum":0,"type":"integer"}},"required":["start_code_point","end_code_point_exclusive","exact_substring","category","alternatives","provider_confidence"],"type":"object"},"maxItems":200,"type":"array"},"visual_observations":{"items":{"additionalProperties":false,"properties":{"end_code_point_exclusive":{"minimum":1,"type":["integer","null"]},"kind":{"enum":["LINE_BREAK","PARAGRAPH_BREAK","LIST_MARKER","TABLE_CELL_TEXT","BOLD","ITALIC","UNDERLINE","ALL_CAPS","ENLARGED_TEXT"],"type":"string"},"start_code_point":{"minimum":0,"type":["integer","null"]}},"required":["kind","start_code_point","end_code_point_exclusive"],"type":"object"},"maxItems":200,"type":"array"},"warnings":{"items":{"maxLength":1024,"minLength":1,"type":"string"},"maxItems":50,"type":"array"}},"required":["source_region_id","page_number","literal_text","status","uncertainties","warnings","provider_returned_ordinal","visual_observations"],"type":"object"},"maxItems":32,"minItems":1,"type":"array"},"correlation_id":{"maxLength":120,"minLength":1,"pattern":"^[A-Za-z0-9_-]+$","type":"string"},"provider_provenance":{"additionalProperties":false,"properties":{"adapter_id":{"maxLength":128,"minLength":1,"type":"string"},"adapter_version":{"maxLength":64,"minLength":1,"type":"string"},"parser_id":{"maxLength":128,"minLength":1,"type":"string"},"parser_version":{"maxLength":64,"minLength":1,"type":"string"},"provider":{"maxLength":128,"minLength":1,"type":"string"},"provider_reported_model":{"maxLength":256,"type":["string","null"]},"provider_response_id":{"maxLength":256,"type":["string","null"]},"requested_model":{"maxLength":256,"minLength":1,"type":"string"}},"required":["provider","requested_model","provider_reported_model","provider_response_id","adapter_id","adapter_version","parser_id","parser_version"],"type":"object"},"schema_id":{"const":"region-anchored-transcription-schema-v1","type":"string"},"schema_version":{"const":4,"type":"integer"},"transcription_profile_id":{"const":"region-anchored-fidelity-acquisition-v1","type":"string"}},"required":["correlation_id","transcription_profile_id","schema_id","schema_version","provider_provenance","blocks"],"type":"object"}"""
val REGION_TRANSCRIPTION_SCHEMA_SHA256_V4: String = sha256(REGION_TRANSCRIPTION_SCHEMA_SOURCE_V4.toByteArray(Charsets.UTF_8))
val REGION_TRANSCRIPTION_SCHEMA_SOURCE: String = REGION_TRANSCRIPTION_SCHEMA_SOURCE_V4.replace(
    "\"end_code_point_exclusive\":{\"minimum\":1,\"type\":[\"integer\",\"null\"]},\"kind\"",
    "\"end_code_point_exclusive\":{\"minimum\":0,\"type\":[\"integer\",\"null\"]},\"kind\"",
).replace(
    "\"schema_version\":{\"const\":4,\"type\":\"integer\"}",
    "\"schema_version\":{\"const\":5,\"type\":\"integer\"}",
).also { require(it != REGION_TRANSCRIPTION_SCHEMA_SOURCE_V4) }
val REGION_TRANSCRIPTION_SCHEMA_SHA256: String = sha256(REGION_TRANSCRIPTION_SCHEMA_SOURCE.toByteArray(Charsets.UTF_8))

enum class RegionTranscriptionRejection {
    MALFORMED_SCHEMA, ADDITIONAL_FIELD, CORRELATION_MISMATCH, PROFILE_OR_SCHEMA_MISMATCH,
    UNKNOWN_REGION, MISSING_REGION, DUPLICATE_REGION, PAGE_MISMATCH, INVALID_STATUS,
    INVALID_UNCERTAINTY, INVALID_UNICODE, INVALID_STATUS_TEXT, INVALID_ORDINAL,
    INVALID_VISUAL_OBSERVATION, EXCESSIVE_TEXT, EXCESSIVE_BLOCK_COUNT, INVALID_PROVENANCE,
}

sealed interface RegionTranscriptionValidationOutcome {
    data class Valid(val result: RegionTranscriptionResult) : RegionTranscriptionValidationOutcome
    data class Rejected(val reason: RegionTranscriptionRejection) : RegionTranscriptionValidationOutcome
}

enum class RegionVisualObservationSemantics {
    WIRE_BOUND, V4_NON_EMPTY_RANGES, V5_EXPLICIT_ANCHORS,
}

class RegionTranscriptionValidator(
    private val observationSemantics: RegionVisualObservationSemantics = RegionVisualObservationSemantics.WIRE_BOUND,
) {
    fun validate(request: RegionTranscriptionRequest, wire: Map<String, Any?>): RegionTranscriptionValidationOutcome = try {
        exactKeys(wire, setOf("correlation_id", "transcription_profile_id", "schema_id", "schema_version", "provider_provenance", "blocks"))
        if (wire.string("correlation_id") != request.correlationId) reject(RegionTranscriptionRejection.CORRELATION_MISMATCH)
        if (wire.string("transcription_profile_id") != request.transcriptionProfileId || wire.string("schema_id") != request.schemaId || wire.int("schema_version") != request.schemaVersion) reject(RegionTranscriptionRejection.PROFILE_OR_SCHEMA_MISMATCH)
        val provenance = parseProvenance(wire.map("provider_provenance"))
        val rawBlocks = wire.list("blocks")
        if (rawBlocks.size !in 1..RegionTranscriptionRequest.MAX_REGIONS_PER_REQUEST) reject(RegionTranscriptionRejection.EXCESSIVE_BLOCK_COUNT)
        val requested = request.targets.associateBy { it.sourceRegionId }
        val blocks = rawBlocks.map { parseBlock(it as? Map<*, *> ?: malformed(), requested, request.schemaVersion) }
        val ids = blocks.map { it.sourceRegionId }
        if (ids.distinct().size != ids.size) reject(RegionTranscriptionRejection.DUPLICATE_REGION)
        if (ids.any { it !in requested }) reject(RegionTranscriptionRejection.UNKNOWN_REGION)
        if (ids.toSet() != requested.keys) reject(RegionTranscriptionRejection.MISSING_REGION)
        val ordinals = blocks.map { it.providerReturnedOrdinal }
        if (ordinals.distinct().size != ordinals.size || ordinals.toSet() != (1..blocks.size).toSet()) reject(RegionTranscriptionRejection.INVALID_ORDINAL)
        RegionTranscriptionValidationOutcome.Valid(RegionTranscriptionResult(request.correlationId, request.transcriptionProfileId, request.schemaId, request.schemaVersion, provenance, blocks))
    } catch (e: Rejection) {
        RegionTranscriptionValidationOutcome.Rejected(e.reason)
    } catch (_: Exception) {
        RegionTranscriptionValidationOutcome.Rejected(RegionTranscriptionRejection.MALFORMED_SCHEMA)
    }

    private fun parseProvenance(value: Map<String, Any?>): RegionTranscriptionProviderProvenance {
        exactKeys(value, setOf("provider", "requested_model", "provider_reported_model", "provider_response_id", "adapter_id", "adapter_version", "parser_id", "parser_version"))
        fun required(name: String, max: Int) = value.string(name).also { if (it.isBlank() || it.length > max) reject(RegionTranscriptionRejection.INVALID_PROVENANCE) }
        fun optional(name: String, max: Int) = value.nullableString(name)?.also { if (it.length > max) reject(RegionTranscriptionRejection.INVALID_PROVENANCE) }
        return RegionTranscriptionProviderProvenance(required("provider", 128), required("requested_model", 256), optional("provider_reported_model", 256), optional("provider_response_id", 256), required("adapter_id", 128), required("adapter_version", 64), required("parser_id", 128), required("parser_version", 64))
    }

    private fun parseBlock(raw: Map<*, *>, requested: Map<SourceRegionId, RegionTranscriptionTarget>, wireVersion: Int): RegionTranscriptionBlock {
        val value = raw.entries.associate { (k, v) -> (k as? String ?: malformed()) to v }
        exactKeys(value, setOf("source_region_id", "page_number", "literal_text", "status", "uncertainties", "warnings", "provider_returned_ordinal", "visual_observations"))
        val id = runCatching { SourceRegionId(value.string("source_region_id")) }.getOrElse { reject(RegionTranscriptionRejection.UNKNOWN_REGION) }
        val target = requested[id]
        val page = value.int("page_number")
        if (target != null && target.pageNumber != page) reject(RegionTranscriptionRejection.PAGE_MISMATCH)
        val text = value.nullableString("literal_text")
        if (text != null && text.codePointCount(0, text.length) > MAX_TEXT_CODE_POINTS) reject(RegionTranscriptionRejection.EXCESSIVE_TEXT)
        if (text != null && !wellFormedUnicode(text)) reject(RegionTranscriptionRejection.INVALID_UNICODE)
        val status = runCatching { RegionTranscriptionStatus.valueOf(value.string("status")) }.getOrElse { reject(RegionTranscriptionRejection.INVALID_STATUS) }
        val uncertainties = value.list("uncertainties").also { if (it.size > 200) reject(RegionTranscriptionRejection.INVALID_UNCERTAINTY) }.map { parseUncertainty(it as? Map<*, *> ?: malformed(), text.orEmpty()) }
        val warnings = value.list("warnings").also { if (it.size > 50) malformed() }.map { (it as? String ?: malformed()).also { warning -> if (warning.isBlank() || warning.length > 1024) malformed() } }
        val observations = value.list("visual_observations").also { if (it.size > 200) malformed() }.map { parseObservation(it as? Map<*, *> ?: malformed(), text.orEmpty(), wireVersion) }
        when (status) {
            RegionTranscriptionStatus.TRANSCRIBED -> if (text.isNullOrEmpty() || uncertainties.isNotEmpty()) reject(RegionTranscriptionRejection.INVALID_STATUS_TEXT)
            RegionTranscriptionStatus.PARTIALLY_TRANSCRIBED -> if (text.isNullOrEmpty() || uncertainties.isEmpty()) reject(RegionTranscriptionRejection.INVALID_STATUS_TEXT)
            RegionTranscriptionStatus.ILLEGIBLE -> if (!text.isNullOrEmpty()) reject(RegionTranscriptionRejection.INVALID_STATUS_TEXT)
            RegionTranscriptionStatus.NO_VISIBLE_TEXT, RegionTranscriptionStatus.UNSUPPORTED_VISUAL_CONTENT -> if (!text.isNullOrEmpty() || uncertainties.isNotEmpty()) reject(RegionTranscriptionRejection.INVALID_STATUS_TEXT)
        }
        return RegionTranscriptionBlock(id, page, text, status, uncertainties, warnings, value.int("provider_returned_ordinal"), observations)
    }

    private fun parseUncertainty(raw: Map<*, *>, text: String): RegionTranscriptionUncertainty {
        val value = raw.entries.associate { (k, v) -> (k as? String ?: malformed()) to v }
        exactKeys(value, setOf("start_code_point", "end_code_point_exclusive", "exact_substring", "category", "alternatives", "provider_confidence"))
        val start = value.int("start_code_point"); val end = value.int("end_code_point_exclusive")
        val count = text.codePointCount(0, text.length)
        if (start < 0 || end <= start || end > count) reject(RegionTranscriptionRejection.INVALID_UNCERTAINTY)
        val substring = value.string("exact_substring")
        val actual = text.substring(text.offsetByCodePoints(0, start), text.offsetByCodePoints(0, end))
        if (substring != actual || !wellFormedUnicode(substring)) reject(RegionTranscriptionRejection.INVALID_UNCERTAINTY)
        val category = runCatching { RegionTranscriptionUncertaintyCategory.valueOf(value.string("category")) }.getOrElse { reject(RegionTranscriptionRejection.INVALID_UNCERTAINTY) }
        val alternatives = value.list("alternatives").also { if (it.size > 8) reject(RegionTranscriptionRejection.INVALID_UNCERTAINTY) }.map { (it as? String ?: malformed()).also { a -> if (a.length > 256 || !wellFormedUnicode(a)) reject(RegionTranscriptionRejection.INVALID_UNCERTAINTY) } }
        val confidence = value.nullableString("provider_confidence")?.also { if (it.length > 64) reject(RegionTranscriptionRejection.INVALID_UNCERTAINTY) }
        return RegionTranscriptionUncertainty(start, end, substring, category, alternatives, confidence)
    }

    private fun parseObservation(raw: Map<*, *>, text: String, wireVersion: Int): RegionVisualObservation {
        val value = raw.entries.associate { (k, v) -> (k as? String ?: malformed()) to v }
        exactKeys(value, setOf("kind", "start_code_point", "end_code_point_exclusive"))
        val kind = runCatching { RegionVisualObservationKind.valueOf(value.string("kind")) }
            .getOrElse { reject(RegionTranscriptionRejection.INVALID_VISUAL_OBSERVATION) }
        val start = value.nullableInt("start_code_point")
        val end = value.nullableInt("end_code_point_exclusive")
        val pointKind = kind == RegionVisualObservationKind.LINE_BREAK
        val explicitAnchors = when (observationSemantics) {
            RegionVisualObservationSemantics.WIRE_BOUND -> wireVersion >= REGION_TRANSCRIPTION_WIRE_VERSION
            RegionVisualObservationSemantics.V4_NON_EMPTY_RANGES -> false
            RegionVisualObservationSemantics.V5_EXPLICIT_ANCHORS -> true
        }
        if (!explicitAnchors && start != null && end != null && start == end) {
            malformed()
        }
        val anchor = runCatching {
            RegionVisualObservationAnchor.resolve(
                start, end, text.codePointCount(0, text.length), pointsAllowed = explicitAnchors,
            )
        }.getOrElse { reject(RegionTranscriptionRejection.INVALID_VISUAL_OBSERVATION) }
        if (explicitAnchors) {
            if (anchor is RegionVisualObservationAnchor.Point && !pointKind) {
                reject(RegionTranscriptionRejection.INVALID_VISUAL_OBSERVATION)
            }
            if (anchor is RegionVisualObservationAnchor.Range && pointKind) {
                reject(RegionTranscriptionRejection.INVALID_VISUAL_OBSERVATION)
            }
        }
        return RegionVisualObservation(kind, start, end)
    }

    private fun exactKeys(value: Map<String, Any?>, expected: Set<String>) { if (value.keys != expected) reject(RegionTranscriptionRejection.ADDITIONAL_FIELD) }
    private fun wellFormedUnicode(value: String): Boolean { var i = 0; while (i < value.length) { val c = value[i]; when { c.isHighSurrogate() -> { if (i + 1 >= value.length || !value[i + 1].isLowSurrogate()) return false; i += 2 }; c.isLowSurrogate() -> return false; else -> i++ } }; return true }
    private fun Map<String, Any?>.string(name: String) = this[name] as? String ?: malformed()
    private fun Map<String, Any?>.nullableString(name: String) = when (val v = this[name]) { null -> null; is String -> v; else -> malformed() }
    private fun Map<String, Any?>.int(name: String) = (this[name] as? Number)?.let { n -> n.toLong().takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE && n.toDouble() == it.toDouble() }?.toInt() } ?: malformed()
    private fun Map<String, Any?>.nullableInt(name: String) = if (this[name] == null) null else int(name)
    @Suppress("UNCHECKED_CAST") private fun Map<String, Any?>.map(name: String) = this[name] as? Map<String, Any?> ?: malformed()
    @Suppress("UNCHECKED_CAST") private fun Map<String, Any?>.list(name: String) = this[name] as? List<Any?> ?: malformed()
    private fun malformed(): Nothing = reject(RegionTranscriptionRejection.MALFORMED_SCHEMA)
    private fun reject(reason: RegionTranscriptionRejection): Nothing = throw Rejection(reason)
    private class Rejection(val reason: RegionTranscriptionRejection) : RuntimeException(null, null, false, false)
    companion object { const val MAX_TEXT_CODE_POINTS = 100_000 }
}

private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
