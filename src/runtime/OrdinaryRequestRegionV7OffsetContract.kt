package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.RegionVisualObservation
import parker.core.interfaces.RegionVisualObservationAnchor
import parker.core.interfaces.RegionVisualObservationKind

const val ORDINARY_REQUEST_REGION_V7_CAPABILITY_ID = "ordinary-external-request-region-transcription-v7"
const val REQUEST_REGION_V7_PROFILE_ID = "request-region-anchored-fidelity-acquisition-v3"
const val REQUEST_REGION_V7_SCHEMA_ID = "request-region-anchored-transcription-schema-v3"
const val REQUEST_REGION_V7_WIRE_VERSION = 7
const val REQUEST_REGION_V7_PROCESSING_PROFILE = "external-transcription.deterministic-complete-set-request-region-v3"
const val REQUEST_REGION_V7_ADAPTER_VERSION = "6.0.0"
const val REQUEST_REGION_V7_PARSER_VERSION = "2.0.0"

const val REQUEST_REGION_V7_INSTRUCTION = "Transcribe only text visibly present inside each supplied Parker request-region crop. A request region is a transport representation of one or more ordered Parker source regions; bind one result only to its request_region_id and page_number. Preserve exact visible Unicode, spelling, punctuation, capitalization, numbers, dates, identifiers, line breaks, paragraph breaks, repeated spaces, tabs, indentation, and significant emphasis. Report bounded uncertainty instead of guessing. Never summarize, interpret, reason, rewrite, correct, normalize, infer, complete, split text among constituent source regions, create identifiers, or decide document source order. Provider-returned ordinal is forensic metadata only. For every visual_observation, measure start_code_point and end_code_point_exclusive over the exact literal_text value returned in the same block. Index from 0 and use half-open [start,end) positions that count Unicode scalar values/code points, not UTF-8 bytes, UTF-16 code units, grapheme clusters, rendered glyphs, or any hidden or normalized text. Every position must be between 0 and the exact literal_text Unicode code-point length inclusive. LINE_BREAK uses a zero-width point [n,n) at the exact code-point boundary. Every other anchored observation uses a non-empty range [start,end). Use null/null only for an unanchored observation; never mix a null endpoint with a non-null endpoint. Return only the strict structured schema."

val REQUEST_REGION_V7_INSTRUCTION_SHA256 = requestRegionV7Sha256(REQUEST_REGION_V7_INSTRUCTION.toByteArray())
val REQUEST_REGION_V7_SCHEMA_SOURCE: String = requestRegionV7Schema()
val REQUEST_REGION_V7_SCHEMA_SHA256 = requestRegionV7Sha256(REQUEST_REGION_V7_SCHEMA_SOURCE.toByteArray())

enum class RequestRegionV7CapabilityLifecycle { ACCEPTANCE_PENDING, ACCEPTED }

data class OrdinaryRequestRegionV7Capability(
    val capabilityId: String = ORDINARY_REQUEST_REGION_V7_CAPABILITY_ID,
    val lifecycle: RequestRegionV7CapabilityLifecycle = RequestRegionV7CapabilityLifecycle.ACCEPTANCE_PENDING,
    val maximumRequestRegions: Int = REQUEST_REGION_MAXIMUM,
    val maximumBodyBytes: Int = REQUEST_REGION_BODY_MAXIMUM_BYTES,
    val batching: Boolean = false,
) {
    init {
        require(capabilityId == ORDINARY_REQUEST_REGION_V7_CAPABILITY_ID)
        require(lifecycle == RequestRegionV7CapabilityLifecycle.ACCEPTANCE_PENDING)
        require(maximumRequestRegions == 32 && maximumBodyBytes == 16_777_216 && !batching)
    }

    fun digest() = requestRegionV7Sha256(
        listOf(
            capabilityId, REQUEST_REGION_V7_PROFILE_ID, REQUEST_REGION_V7_SCHEMA_ID,
            REQUEST_REGION_V7_WIRE_VERSION.toString(), REQUEST_REGION_V7_SCHEMA_SHA256,
            REQUEST_REGION_V7_PROCESSING_PROFILE, REQUEST_REGION_SHAPING_ID,
            REQUEST_REGION_ADAPTER_ID, REQUEST_REGION_V7_ADAPTER_VERSION,
            REQUEST_REGION_V7_INSTRUCTION_SHA256, REQUEST_REGION_PROVIDER, REQUEST_REGION_MODEL,
            maximumRequestRegions.toString(), maximumBodyBytes.toString(), batching.toString(),
            "unicode-scalar-code-point-half-open-v1",
        ).joinToString("\u0000").toByteArray(),
    )
}

/** Strict v7 coordinate contract. It never clamps, drops, shifts, normalizes, or guesses offsets. */
object RequestRegionV7ObservationContract {
    const val COORDINATE_SYSTEM = "Unicode scalar-value/code-point offsets over exact literal_text; zero-based half-open [start,end)"

    fun validate(
        literalText: String,
        kind: RegionVisualObservationKind,
        startCodePoint: Int?,
        endCodePointExclusive: Int?,
    ): Result<RegionVisualObservation> = runCatching {
        val length = literalText.codePointCount(0, literalText.length)
        val anchor = RegionVisualObservationAnchor.resolve(startCodePoint, endCodePointExclusive, length, pointsAllowed = true)
        when (anchor) {
            RegionVisualObservationAnchor.Unanchored -> Unit
            is RegionVisualObservationAnchor.Point -> require(kind == RegionVisualObservationKind.LINE_BREAK) {
                "only LINE_BREAK may use a point anchor"
            }
            is RegionVisualObservationAnchor.Range -> require(kind != RegionVisualObservationKind.LINE_BREAK) {
                "LINE_BREAK must use a point anchor"
            }
        }
        RegionVisualObservation(kind, startCodePoint, endCodePointExclusive)
    }
}

data class RequestRegionV7StructuredBlock(
    val requestRegionId: RequestRegionId,
    val pageNumber: Int,
    val literalText: String?,
    val visualObservations: List<RegionVisualObservation>,
)

sealed interface RequestRegionV7ValidationOutcome {
    data class Valid(val blocksInProviderOrder: List<RequestRegionV7StructuredBlock>) : RequestRegionV7ValidationOutcome
    data class Rejected(val reason: String) : RequestRegionV7ValidationOutcome
}

/** Acceptance-only v7 structured-state validator; no production route composes this type. */
class RequestRegionV7StructuredValidator {
    fun validate(request: RequestRegionTranscriptionRequest, wire: Map<String, Any?>): RequestRegionV7ValidationOutcome = try {
        require(wire.keys == setOf("correlation_id", "transcription_profile_id", "schema_id", "schema_version", "provider_provenance", "blocks"))
        require(wire["correlation_id"] == request.correlationId)
        require(wire["transcription_profile_id"] == REQUEST_REGION_V7_PROFILE_ID)
        require(wire["schema_id"] == REQUEST_REGION_V7_SCHEMA_ID)
        require((wire["schema_version"] as Number).toInt() == REQUEST_REGION_V7_WIRE_VERSION)
        @Suppress("UNCHECKED_CAST") val provenance = wire["provider_provenance"] as Map<String, Any?>
        require(provenance["provider"] == REQUEST_REGION_PROVIDER && provenance["requested_model"] == REQUEST_REGION_MODEL)
        require(provenance["adapter_id"] == REQUEST_REGION_ADAPTER_ID && provenance["adapter_version"] == REQUEST_REGION_V7_ADAPTER_VERSION)
        require(provenance["parser_id"] == REQUEST_REGION_PARSER_ID && provenance["parser_version"] == REQUEST_REGION_V7_PARSER_VERSION)
        @Suppress("UNCHECKED_CAST") val rawBlocks = wire["blocks"] as List<Map<String, Any?>>
        require(rawBlocks.size == request.regions.size && rawBlocks.size in 1..REQUEST_REGION_MAXIMUM)
        val requested = request.regions.associateBy { it.id }
        val parsed = rawBlocks.map { raw ->
            require(raw.keys == setOf("request_region_id", "page_number", "literal_text", "status", "uncertainties", "warnings", "provider_returned_ordinal", "visual_observations"))
            val id = RequestRegionId(raw["request_region_id"] as String); val target = requested.getValue(id)
            val page = (raw["page_number"] as Number).toInt(); require(page == target.pageNumber)
            val text = raw["literal_text"] as? String
            @Suppress("UNCHECKED_CAST") val observations = (raw["visual_observations"] as List<Map<String, Any?>>).map { observation ->
                require(observation.keys == setOf("kind", "start_code_point", "end_code_point_exclusive"))
                RequestRegionV7ObservationContract.validate(
                    text.orEmpty(), RegionVisualObservationKind.valueOf(observation["kind"] as String),
                    (observation["start_code_point"] as? Number)?.toInt(),
                    (observation["end_code_point_exclusive"] as? Number)?.toInt(),
                ).getOrThrow()
            }
            RequestRegionV7StructuredBlock(id, page, text, observations)
        }
        require(parsed.map { it.requestRegionId }.toSet() == requested.keys)
        val legacy = wire.toMutableMap().also { top ->
            top["transcription_profile_id"] = REQUEST_REGION_PROFILE_ID
            top["schema_id"] = REQUEST_REGION_SCHEMA_ID
            top["schema_version"] = REQUEST_REGION_WIRE_VERSION
            @Suppress("UNCHECKED_CAST")
            val legacyProvenance = (provenance as Map<String, Any?>).toMutableMap()
            legacyProvenance["adapter_version"] = REQUEST_REGION_ADAPTER_VERSION
            legacyProvenance["parser_version"] = REQUEST_REGION_PARSER_VERSION
            top["provider_provenance"] = legacyProvenance
        }
        require(RequestRegionTranscriptionValidator().validate(request, legacy) is RequestRegionValidationOutcome.Valid) {
            "existing complete structural validation rejected response"
        }
        RequestRegionV7ValidationOutcome.Valid(parsed)
    } catch (e: Exception) {
        RequestRegionV7ValidationOutcome.Rejected(e.message ?: e::class.simpleName ?: "MALFORMED")
    }

    fun parseExact(request: RequestRegionTranscriptionRequest, structuredJson: String): RequestRegionV7ValidationOutcome {
        @Suppress("UNCHECKED_CAST") val wire = RegionJson.parse(structuredJson) as? Map<String, Any?>
            ?: return RequestRegionV7ValidationOutcome.Rejected("MALFORMED")
        return validate(request, wire)
    }
}

@Suppress("UNCHECKED_CAST")
private fun requestRegionV7Schema(): String {
    fun mutable(value: Any?): Any? = when (value) {
        is Map<*, *> -> value.entries.associateTo(linkedMapOf()) { it.key as String to mutable(it.value) }
        is List<*> -> value.map(::mutable).toMutableList()
        else -> value
    }
    val root = mutable(RegionJson.parse(REQUEST_REGION_SCHEMA_SOURCE)) as MutableMap<String, Any?>
    val properties = root.getValue("properties") as MutableMap<String, Any?>
    properties["transcription_profile_id"] = linkedMapOf("const" to REQUEST_REGION_V7_PROFILE_ID, "type" to "string")
    properties["schema_id"] = linkedMapOf("const" to REQUEST_REGION_V7_SCHEMA_ID, "type" to "string")
    properties["schema_version"] = linkedMapOf("const" to REQUEST_REGION_V7_WIRE_VERSION, "type" to "integer")
    val block = (((properties.getValue("blocks") as MutableMap<String, Any?>).getValue("items") as MutableMap<String, Any?>).getValue("properties") as MutableMap<String, Any?> )
    (block.getValue("literal_text") as MutableMap<String, Any?>)["description"] = "Exact returned transcription string; visual-observation coordinates index this value without normalization."
    val observations = (((block.getValue("visual_observations") as MutableMap<String, Any?>).getValue("items") as MutableMap<String, Any?>).getValue("properties") as MutableMap<String, Any?> )
    (observations.getValue("start_code_point") as MutableMap<String, Any?>)["description"] = "Zero-based Unicode scalar-value/code-point start over exact literal_text; null only with a null end for an unanchored observation."
    (observations.getValue("end_code_point_exclusive") as MutableMap<String, Any?>)["description"] = "Exclusive Unicode scalar-value/code-point end over exact literal_text; must not exceed its code-point length. LINE_BREAK uses end equal to start; other anchored kinds require end greater than start."
    val provenance = ((properties.getValue("provider_provenance") as MutableMap<String, Any?>).getValue("properties") as MutableMap<String, Any?> )
    provenance["adapter_version"] = linkedMapOf("const" to REQUEST_REGION_V7_ADAPTER_VERSION, "type" to "string")
    provenance["parser_version"] = linkedMapOf("const" to REQUEST_REGION_V7_PARSER_VERSION, "type" to "string")
    return RegionJson.encode(root)
}

private fun requestRegionV7Sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
