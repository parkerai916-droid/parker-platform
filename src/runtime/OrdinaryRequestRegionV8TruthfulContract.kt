package parker.core.runtime

import java.security.MessageDigest

const val ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID = "ordinary-external-request-region-transcription-v8"
const val REQUEST_REGION_V8_PROFILE_ID = "request-region-fidelity-acquisition-v4"
const val REQUEST_REGION_V8_SCHEMA_ID = "request-region-transcription-schema-v4"
const val REQUEST_REGION_V8_WIRE_VERSION = 8
const val REQUEST_REGION_V8_PROCESSING_PROFILE = "external-transcription.deterministic-complete-set-request-region-v4"
const val REQUEST_REGION_V8_ADAPTER_VERSION = "7.0.0"
const val REQUEST_REGION_V8_PARSER_VERSION = "3.0.0"
const val REQUEST_REGION_V8_INSTRUCTION = "Transcribe only text visibly present inside each supplied Parker request-region crop. Bind one result only to its request_region_id and page_number. Preserve exact visible Unicode, spelling, punctuation, capitalization, numbers, dates, identifiers, line breaks, paragraph breaks, repeated spaces, tabs, and indentation. Report bounded uncertainty instead of guessing. Never summarize, interpret, rewrite, correct, normalize, infer, complete, split text among constituent source regions, create identifiers, decide document source order, or generate character-level visual observations. Return only the strict structured schema."

val REQUEST_REGION_V8_INSTRUCTION_SHA256 = requestRegionV8Sha256(REQUEST_REGION_V8_INSTRUCTION.toByteArray())
val REQUEST_REGION_V8_SCHEMA_SOURCE: String = requestRegionV8Schema()
val REQUEST_REGION_V8_SCHEMA_SHA256 = requestRegionV8Sha256(REQUEST_REGION_V8_SCHEMA_SOURCE.toByteArray())

enum class RequestRegionV8CapabilityLifecycle { ACCEPTANCE_PENDING, ACCEPTED }

data class OrdinaryRequestRegionV8Capability(
    val capabilityId:String=ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,
    val lifecycle:RequestRegionV8CapabilityLifecycle=RequestRegionV8CapabilityLifecycle.ACCEPTANCE_PENDING,
) {
    init { require(capabilityId==ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID);require(lifecycle==RequestRegionV8CapabilityLifecycle.ACCEPTANCE_PENDING) }
    fun digest()=requestRegionV8Sha256(listOf(capabilityId,REQUEST_REGION_V8_PROFILE_ID,REQUEST_REGION_V8_SCHEMA_ID,
        REQUEST_REGION_V8_WIRE_VERSION.toString(),REQUEST_REGION_V8_SCHEMA_SHA256,REQUEST_REGION_V8_PROCESSING_PROFILE,
        REQUEST_REGION_SHAPING_ID,REQUEST_REGION_ADAPTER_ID,REQUEST_REGION_V8_ADAPTER_VERSION,REQUEST_REGION_PARSER_ID,
        REQUEST_REGION_V8_PARSER_VERSION,REQUEST_REGION_V8_INSTRUCTION_SHA256,REQUEST_REGION_PROVIDER,REQUEST_REGION_MODEL,
        REQUEST_REGION_MAXIMUM.toString(),REQUEST_REGION_BODY_MAXIMUM_BYTES.toString(),"false","provider-observations-absent-v1")
        .joinToString("\u0000").toByteArray())
}

/** Offline feasibility projection only. It never upgrades historical v7 evidence or repairs anchors. */
object RequestRegionV8FeasibilityProjector {
    fun projectHistoricalV7(wire:Map<String,Any?>):Map<String,Any?> {
        require(wire["transcription_profile_id"]==REQUEST_REGION_V7_PROFILE_ID)
        require(wire["schema_id"]==REQUEST_REGION_V7_SCHEMA_ID)
        require((wire["schema_version"] as Number).toInt()==REQUEST_REGION_V7_WIRE_VERSION)
        @Suppress("UNCHECKED_CAST") val provenance=(wire["provider_provenance"] as Map<String,Any?>).toMutableMap()
        provenance["adapter_version"]=REQUEST_REGION_V8_ADAPTER_VERSION;provenance["parser_version"]=REQUEST_REGION_V8_PARSER_VERSION
        @Suppress("UNCHECKED_CAST") val blocks=(wire["blocks"] as List<Map<String,Any?>>).map { block ->
            linkedMapOf<String,Any?>("request_region_id" to block["request_region_id"],"page_number" to block["page_number"],
                "literal_text" to block["literal_text"],"status" to block["status"],"uncertainties" to block["uncertainties"],
                "warnings" to block["warnings"],"provider_returned_ordinal" to block["provider_returned_ordinal"])
        }
        return linkedMapOf("correlation_id" to wire["correlation_id"],"transcription_profile_id" to REQUEST_REGION_V8_PROFILE_ID,
            "schema_id" to REQUEST_REGION_V8_SCHEMA_ID,"schema_version" to REQUEST_REGION_V8_WIRE_VERSION,
            "provider_provenance" to provenance,"blocks" to blocks)
    }
}

private fun requestRegionV8Schema():String {
    @Suppress("UNCHECKED_CAST") fun mutable(v:Any?):Any?=when(v){is Map<*,*>->v.entries.associateTo(linkedMapOf()){it.key as String to mutable(it.value)};is List<*>->v.map(::mutable).toMutableList();else->v}
    @Suppress("UNCHECKED_CAST") val root=mutable(RegionJson.parse(REQUEST_REGION_V7_SCHEMA_SOURCE)) as MutableMap<String,Any?>
    @Suppress("UNCHECKED_CAST") val properties=root["properties"] as MutableMap<String,Any?>
    properties["transcription_profile_id"]=linkedMapOf("const" to REQUEST_REGION_V8_PROFILE_ID,"type" to "string")
    properties["schema_id"]=linkedMapOf("const" to REQUEST_REGION_V8_SCHEMA_ID,"type" to "string")
    properties["schema_version"]=linkedMapOf("const" to REQUEST_REGION_V8_WIRE_VERSION,"type" to "integer")
    @Suppress("UNCHECKED_CAST") val blockItems=(properties["blocks"] as MutableMap<String,Any?>)["items"] as MutableMap<String,Any?>
    @Suppress("UNCHECKED_CAST") val blockProperties=blockItems["properties"] as MutableMap<String,Any?>
    blockProperties.remove("visual_observations")
    @Suppress("UNCHECKED_CAST") val required=blockItems["required"] as MutableList<String>;required.remove("visual_observations")
    @Suppress("UNCHECKED_CAST") val provenance=((properties["provider_provenance"] as MutableMap<String,Any?>)["properties"] as MutableMap<String,Any?>)
    provenance["adapter_version"]=linkedMapOf("const" to REQUEST_REGION_V8_ADAPTER_VERSION,"type" to "string")
    provenance["parser_version"]=linkedMapOf("const" to REQUEST_REGION_V8_PARSER_VERSION,"type" to "string")
    return RegionJson.encode(root)
}

private fun requestRegionV8Sha256(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}
