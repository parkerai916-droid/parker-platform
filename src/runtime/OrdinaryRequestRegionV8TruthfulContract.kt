package parker.core.runtime

import java.security.MessageDigest
import java.util.Base64
import parker.core.interfaces.*

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

enum class RequestRegionV8UncertaintyCategory { ILLEGIBLE, AMBIGUOUS, PARTIALLY_OCCLUDED, LOW_CONTRAST, HANDWRITING_UNCERTAIN, CLIPPED, OTHER_VISUAL_UNCERTAINTY }
data class RequestRegionV8Uncertainty(val category:RequestRegionV8UncertaintyCategory,val description:String?,val alternatives:List<String>,val providerConfidence:String?)
data class RequestRegionV8Request(val correlationId:String,val regions:List<RequestRegion>){init{require(correlationId.matches(Regex("^[A-Za-z0-9_-]{1,120}$")));require(regions.size in 1..REQUEST_REGION_MAXIMUM);require(regions.map{it.id}.distinct().size==regions.size)}}
data class RequestRegionV8Block(val requestRegionId:RequestRegionId,val pageNumber:Int,val literalText:String?,val status:RegionTranscriptionStatus,val uncertainties:List<RequestRegionV8Uncertainty>,val warnings:List<String>,val providerReturnedOrdinal:Int)
data class RequestRegionV8Result(val blocksInProviderOrder:List<RequestRegionV8Block>,val providerProvenance:RegionTranscriptionProviderProvenance)
sealed interface RequestRegionV8ValidationOutcome{data class Valid(val result:RequestRegionV8Result):RequestRegionV8ValidationOutcome;data class Rejected(val reason:String):RequestRegionV8ValidationOutcome}

class OpenAiRequestRegionV8Codec {
    fun buildRequestBody(request:RequestRegionV8Request):String {
        val content=mutableListOf<Map<String,Any?>>(mapOf("type" to "input_text","text" to REQUEST_REGION_V8_INSTRUCTION+"\nrequest_correlation_id="+request.correlationId))
        request.regions.forEach{r->content+=mapOf("type" to "input_text","text" to manifest(r));content+=mapOf("type" to "input_image","image_url" to "data:image/png;base64,"+Base64.getEncoder().encodeToString(r.image.encodedBytes()),"detail" to "original")}
        val body=RegionJson.encode(linkedMapOf("model" to REQUEST_REGION_MODEL,"store" to false,"stream" to false,"reasoning" to mapOf("effort" to "none"),"input" to listOf(mapOf("role" to "user","content" to content)),"text" to mapOf("format" to mapOf("type" to "json_schema","name" to REQUEST_REGION_V8_SCHEMA_ID,"strict" to true,"schema" to RegionJson.parse(REQUEST_REGION_V8_SCHEMA_SOURCE)))))
        require(body.toByteArray().size<=REQUEST_REGION_BODY_MAXIMUM_BYTES);return body
    }
    fun requestDigest(request:RequestRegionV8Request)=requestRegionV8Sha256(canonicalBinding(request))
    fun canonicalBinding(request:RequestRegionV8Request):ByteArray=RegionJson.encode(linkedMapOf<String,Any?>("correlation_id" to request.correlationId,"profile" to REQUEST_REGION_V8_PROFILE_ID,"schema_id" to REQUEST_REGION_V8_SCHEMA_ID,"schema_version" to 8,"schema_sha256" to REQUEST_REGION_V8_SCHEMA_SHA256,"processing_profile" to REQUEST_REGION_V8_PROCESSING_PROFILE,"instruction_sha256" to REQUEST_REGION_V8_INSTRUCTION_SHA256,"provider" to REQUEST_REGION_PROVIDER,"model" to REQUEST_REGION_MODEL,"adapter_id" to REQUEST_REGION_ADAPTER_ID,"adapter_version" to REQUEST_REGION_V8_ADAPTER_VERSION,"parser_id" to REQUEST_REGION_PARSER_ID,"parser_version" to REQUEST_REGION_V8_PARSER_VERSION,"reasoning" to "none","store" to false,"targets" to request.regions.map{r->linkedMapOf("request_region_id" to r.id.value,"evidence_artifact_id" to r.sourceEvidenceArtifactId.value,"source_sha256" to r.sourceSha256,"page_representation_id" to r.pageRepresentationId.value,"page_number" to r.pageNumber,"bounds" to listOf(r.bounds.left,r.bounds.top,r.bounds.rightExclusive,r.bounds.bottomExclusive),"crop_digest" to r.cropDigest.value,"image_sha256" to r.image.encodedSha256,"constituent_source_region_ids" to r.constituentIds.map{it.value})})).toByteArray()
    private fun manifest(r:RequestRegion)="REQUEST_REGION request_region_id=${r.id.value} page_number=${r.pageNumber} bounds=${r.bounds.left},${r.bounds.top},${r.bounds.rightExclusive},${r.bounds.bottomExclusive} crop_digest=${r.cropDigest.value}"
}

class RequestRegionV8StructuredValidator {
    fun parseExact(request:RequestRegionV8Request,json:String)=try{ @Suppress("UNCHECKED_CAST") val wire=RegionJson.parse(json) as Map<String,Any?>;validate(request,wire)}catch(e:Exception){RequestRegionV8ValidationOutcome.Rejected(e.message?:"MALFORMED")}
    fun validate(request:RequestRegionV8Request,wire:Map<String,Any?>):RequestRegionV8ValidationOutcome=try{
        require(wire.keys==setOf("correlation_id","transcription_profile_id","schema_id","schema_version","provider_provenance","blocks"));require(wire["correlation_id"]==request.correlationId);require(wire["transcription_profile_id"]==REQUEST_REGION_V8_PROFILE_ID);require(wire["schema_id"]==REQUEST_REGION_V8_SCHEMA_ID);require((wire["schema_version"] as Number).toInt()==8)
        @Suppress("UNCHECKED_CAST") val p=wire["provider_provenance"] as Map<String,Any?>;require(p.keys==setOf("provider","requested_model","provider_reported_model","provider_response_id","adapter_id","adapter_version","parser_id","parser_version"));require(p["provider"]==REQUEST_REGION_PROVIDER&&p["requested_model"]==REQUEST_REGION_MODEL&&p["adapter_id"]==REQUEST_REGION_ADAPTER_ID&&p["adapter_version"]==REQUEST_REGION_V8_ADAPTER_VERSION&&p["parser_id"]==REQUEST_REGION_PARSER_ID&&p["parser_version"]==REQUEST_REGION_V8_PARSER_VERSION)
        val provenance=RegionTranscriptionProviderProvenance(p["provider"] as String,p["requested_model"] as String,p["provider_reported_model"] as? String,p["provider_response_id"] as? String,p["adapter_id"] as String,p["adapter_version"] as String,p["parser_id"] as String,p["parser_version"] as String)
        @Suppress("UNCHECKED_CAST") val raw=wire["blocks"] as List<Map<String,Any?>>;require(raw.size==request.regions.size);val targets=request.regions.associateBy{it.id}
        val blocks=raw.map{b->require(b.keys==setOf("request_region_id","page_number","literal_text","status","uncertainties","warnings","provider_returned_ordinal"));val id=RequestRegionId(b["request_region_id"] as String);val target=targets.getValue(id);val page=(b["page_number"] as Number).toInt();require(page==target.pageNumber);val text=b["literal_text"] as? String;require(text==null||text.codePointCount(0,text.length)<=100000)
            @Suppress("UNCHECKED_CAST") val uncertainties=(b["uncertainties"] as List<Map<String,Any?>>).map{u->require(u.keys==setOf("category","description","alternatives","provider_confidence"));RequestRegionV8Uncertainty(RequestRegionV8UncertaintyCategory.valueOf(u["category"] as String),u["description"] as? String,(u["alternatives"] as List<*>).map{it as String},u["provider_confidence"] as? String)}
            @Suppress("UNCHECKED_CAST") val warnings=(b["warnings"] as List<*>).map{it as String};RequestRegionV8Block(id,page,text,RegionTranscriptionStatus.valueOf(b["status"] as String),uncertainties,warnings,(b["provider_returned_ordinal"] as Number).toInt())}
        require(blocks.map{it.requestRegionId}.toSet()==targets.keys&&blocks.map{it.requestRegionId}.distinct().size==blocks.size);RequestRegionV8ValidationOutcome.Valid(RequestRegionV8Result(blocks,provenance))
    }catch(e:Exception){RequestRegionV8ValidationOutcome.Rejected(e.message?:"MALFORMED")}
}

data class RequestRegionV8DerivativeBlock(val requestRegionId:String,val pageNumber:Int,val constituentSourceRegionIds:List<String>,val literalText:String?,val status:String,val uncertainties:List<RequestRegionV8Uncertainty>,val warnings:List<String>)
data class RequestRegionV8Derivative(val capabilityId:String,val evidenceArtifactId:String,val sourceSha256:String,val requestDigest:String,val blocksInParkerOrder:List<RequestRegionV8DerivativeBlock>,val canonicalDigest:String)
class RequestRegionV8DerivativeBinder {
    fun bind(request:RequestRegionV8Request,result:RequestRegionV8Result):Result<RequestRegionV8Derivative> = runCatching {
        val byId=result.blocksInProviderOrder.associateBy{it.requestRegionId}
        require(byId.keys==request.regions.map{it.id}.toSet())
        val blocks=request.regions.map { r ->
            val b=byId.getValue(r.id)
            RequestRegionV8DerivativeBlock(r.id.value,r.pageNumber,r.constituentIds.map{it.value},b.literalText,b.status.name,b.uncertainties,b.warnings)
        }
        val digest=OpenAiRequestRegionV8Codec().requestDigest(request)
        val fields=linkedMapOf<String,Any?>(
            "capability" to ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,"request_digest" to digest,
            "evidence" to request.regions.first().sourceEvidenceArtifactId.value,"source_sha256" to request.regions.first().sourceSha256,
            "blocks" to blocks.map { b -> linkedMapOf("request_region_id" to b.requestRegionId,"page_number" to b.pageNumber,
                "constituent_source_region_ids" to b.constituentSourceRegionIds,"literal_text" to b.literalText,"status" to b.status,
                "uncertainties" to b.uncertainties.map{u->listOf(u.category.name,u.description,u.alternatives,u.providerConfidence)},"warnings" to b.warnings) })
        RequestRegionV8Derivative(ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,request.regions.first().sourceEvidenceArtifactId.value,
            request.regions.first().sourceSha256,digest,blocks,requestRegionV8Sha256(RegionJson.encode(fields).toByteArray()))
    }
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
            require((block["uncertainties"] as List<*>).isEmpty()) { "historical span uncertainty cannot be projected into v8" }
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
    blockProperties["uncertainties"]=linkedMapOf("type" to "array","maxItems" to 200,"items" to linkedMapOf("type" to "object","additionalProperties" to false,"properties" to linkedMapOf(
        "category" to linkedMapOf("type" to "string","enum" to RequestRegionV8UncertaintyCategory.entries.map{it.name}),
        "description" to linkedMapOf("type" to listOf("string","null"),"maxLength" to 1024),
        "alternatives" to linkedMapOf("type" to "array","maxItems" to 8,"items" to linkedMapOf("type" to "string","maxLength" to 256)),
        "provider_confidence" to linkedMapOf("type" to listOf("string","null"),"maxLength" to 64)),"required" to listOf("category","description","alternatives","provider_confidence")))
    @Suppress("UNCHECKED_CAST") val required=blockItems["required"] as MutableList<String>;required.remove("visual_observations")
    @Suppress("UNCHECKED_CAST") val provenance=((properties["provider_provenance"] as MutableMap<String,Any?>)["properties"] as MutableMap<String,Any?>)
    provenance["adapter_version"]=linkedMapOf("const" to REQUEST_REGION_V8_ADAPTER_VERSION,"type" to "string")
    provenance["parser_version"]=linkedMapOf("const" to REQUEST_REGION_V8_PARSER_VERSION,"type" to "string")
    return RegionJson.encode(root)
}

private fun requestRegionV8Sha256(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}
