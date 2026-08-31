package parker.core.runtime

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.imageio.ImageIO
import parker.core.interfaces.*

const val ORDINARY_REQUEST_REGION_CAPABILITY_ID = "ordinary-external-request-region-transcription-v6"
const val REQUEST_REGION_PROFILE_ID = "request-region-anchored-fidelity-acquisition-v2"
const val REQUEST_REGION_SCHEMA_ID = "request-region-anchored-transcription-schema-v2"
const val REQUEST_REGION_WIRE_VERSION = 6
const val REQUEST_REGION_PROCESSING_PROFILE = "external-transcription.deterministic-complete-set-request-region-v2"
const val REQUEST_REGION_SHAPING_ID = "complete-set-request-region-shaping-v1"
const val REQUEST_REGION_ADAPTER_ID = "openai-responses-request-region-transcription-adapter"
const val REQUEST_REGION_ADAPTER_VERSION = "5.0.0"
const val REQUEST_REGION_PARSER_ID = "openai-request-region-structured-response-parser"
const val REQUEST_REGION_PARSER_VERSION = "1.0.0"
const val REQUEST_REGION_PROVIDER = "OpenAI"
const val REQUEST_REGION_MODEL = "gpt-5.6-sol"
const val REQUEST_REGION_MAXIMUM = 32
const val REQUEST_REGION_BODY_MAXIMUM_BYTES = 16_777_216

const val REQUEST_REGION_INSTRUCTION = "Transcribe only text visibly present inside each supplied Parker request-region crop. A request region is a transport representation of one or more ordered Parker source regions; bind one result only to its request_region_id and page_number. Preserve exact visible Unicode, spelling, punctuation, capitalization, numbers, dates, identifiers, line breaks, paragraph breaks, repeated spaces, tabs, indentation, and significant emphasis. Report bounded uncertainty instead of guessing. Never summarize, interpret, reason, rewrite, correct, normalize, infer, complete, split text among constituent source regions, create identifiers, or decide document source order. Provider-returned ordinal is forensic metadata only. Return only the strict structured schema."

val REQUEST_REGION_SCHEMA_SOURCE = """{"additionalProperties":false,"properties":{"blocks":{"items":{"additionalProperties":false,"properties":{"literal_text":{"maxLength":100000,"type":["string","null"]},"page_number":{"maximum":10000,"minimum":1,"type":"integer"},"provider_returned_ordinal":{"maximum":32,"minimum":1,"type":"integer"},"request_region_id":{"pattern":"^[0-9a-f]{64}$","type":"string"},"status":{"enum":["TRANSCRIBED","PARTIALLY_TRANSCRIBED","ILLEGIBLE","NO_VISIBLE_TEXT","UNSUPPORTED_VISUAL_CONTENT"],"type":"string"},"uncertainties":{"items":{"additionalProperties":false,"properties":{"alternatives":{"items":{"maxLength":256,"type":"string"},"maxItems":8,"type":"array"},"category":{"enum":["ILLEGIBLE","AMBIGUOUS_CHARACTER","AMBIGUOUS_WORD","PARTIALLY_OCCLUDED","LOW_CONTRAST","HANDWRITING_UNCERTAIN","CLIPPED","OTHER_VISUAL_UNCERTAINTY"],"type":"string"},"end_code_point_exclusive":{"minimum":1,"type":"integer"},"exact_substring":{"maxLength":4096,"type":"string"},"provider_confidence":{"maxLength":64,"type":["string","null"]},"start_code_point":{"minimum":0,"type":"integer"}},"required":["start_code_point","end_code_point_exclusive","exact_substring","category","alternatives","provider_confidence"],"type":"object"},"maxItems":200,"type":"array"},"visual_observations":{"items":{"additionalProperties":false,"properties":{"end_code_point_exclusive":{"minimum":0,"type":["integer","null"]},"kind":{"enum":["LINE_BREAK","PARAGRAPH_BREAK","LIST_MARKER","TABLE_CELL_TEXT","BOLD","ITALIC","UNDERLINE","ALL_CAPS","ENLARGED_TEXT"],"type":"string"},"start_code_point":{"minimum":0,"type":["integer","null"]}},"required":["kind","start_code_point","end_code_point_exclusive"],"type":"object"},"maxItems":200,"type":"array"},"warnings":{"items":{"maxLength":1024,"minLength":1,"type":"string"},"maxItems":50,"type":"array"}},"required":["request_region_id","page_number","literal_text","status","uncertainties","warnings","provider_returned_ordinal","visual_observations"],"type":"object"},"maxItems":32,"minItems":1,"type":"array"},"correlation_id":{"maxLength":120,"minLength":1,"pattern":"^[A-Za-z0-9_-]+$","type":"string"},"provider_provenance":{"additionalProperties":false,"properties":{"adapter_id":{"const":"openai-responses-request-region-transcription-adapter","type":"string"},"adapter_version":{"const":"5.0.0","type":"string"},"parser_id":{"const":"openai-request-region-structured-response-parser","type":"string"},"parser_version":{"const":"1.0.0","type":"string"},"provider":{"const":"OpenAI","type":"string"},"provider_reported_model":{"maxLength":256,"type":["string","null"]},"provider_response_id":{"maxLength":256,"type":["string","null"]},"requested_model":{"const":"gpt-5.6-sol","type":"string"}},"required":["provider","requested_model","provider_reported_model","provider_response_id","adapter_id","adapter_version","parser_id","parser_version"],"type":"object"},"schema_id":{"const":"request-region-anchored-transcription-schema-v2","type":"string"},"schema_version":{"const":6,"type":"integer"},"transcription_profile_id":{"const":"request-region-anchored-fidelity-acquisition-v2","type":"string"}},"required":["correlation_id","transcription_profile_id","schema_id","schema_version","provider_provenance","blocks"],"type":"object"}"""
val REQUEST_REGION_SCHEMA_SHA256 = requestRegionSha256(REQUEST_REGION_SCHEMA_SOURCE.toByteArray())
val REQUEST_REGION_INSTRUCTION_SHA256 = requestRegionSha256(REQUEST_REGION_INSTRUCTION.toByteArray())

enum class RequestRegionCapabilityLifecycle { ACCEPTANCE_PENDING, ACCEPTED }
data class OrdinaryRequestRegionCapability(
    val capabilityId: String = ORDINARY_REQUEST_REGION_CAPABILITY_ID,
    val lifecycle: RequestRegionCapabilityLifecycle = RequestRegionCapabilityLifecycle.ACCEPTANCE_PENDING,
    val maximumRequestRegions: Int = REQUEST_REGION_MAXIMUM,
    val maximumBodyBytes: Int = REQUEST_REGION_BODY_MAXIMUM_BYTES,
    val batching: Boolean = false,
) {
    init { require(capabilityId == ORDINARY_REQUEST_REGION_CAPABILITY_ID && maximumRequestRegions == 32 && maximumBodyBytes == 16_777_216 && !batching) }
    fun digest() = requestRegionSha256(listOf(capabilityId, REQUEST_REGION_PROFILE_ID, REQUEST_REGION_SCHEMA_ID,
        REQUEST_REGION_WIRE_VERSION.toString(), REQUEST_REGION_SCHEMA_SHA256, REQUEST_REGION_PROCESSING_PROFILE,
        REQUEST_REGION_SHAPING_ID, REQUEST_REGION_ADAPTER_ID, REQUEST_REGION_ADAPTER_VERSION,
        REQUEST_REGION_INSTRUCTION_SHA256, REQUEST_REGION_PROVIDER, REQUEST_REGION_MODEL,
        maximumRequestRegions.toString(), maximumBodyBytes.toString(), batching.toString())
        .joinToString("\u0000").toByteArray())
}

@JvmInline value class RequestRegionId(val value: String) { init { require(value.matches(Regex("^[0-9a-f]{64}$"))) } }

data class RequestRegion(
    val id: RequestRegionId,
    val sourceEvidenceArtifactId: EvidenceArtifactId,
    val sourceSha256: String,
    val pageRepresentationId: PageRepresentationId,
    val pageNumber: Int,
    val pageDimensions: PagePixelDimensions,
    val bounds: PixelCropBounds,
    val cropDigest: CanonicalPixelDigest,
    val structuralClass: SourceRegionStructuralClass,
    val derivationProfileId: String,
    val derivationProfileVersion: Int,
    val constituentSourceRegions: List<SourceRegion>,
    val image: RegionTranscriptionImage,
) {
    init {
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")) && pageNumber > 0)
        require(constituentSourceRegions.isNotEmpty())
        require(constituentSourceRegions.map { it.id }.distinct().size == constituentSourceRegions.size)
        require(constituentSourceRegions.all { it.provenance.pageRepresentationId == pageRepresentationId && it.provenance.pageNumber == pageNumber })
        require(bounds == unionRequestRegionBounds(constituentSourceRegions.map { it.bounds }))
        require(bounds.rightExclusive <= pageDimensions.width && bounds.bottomExclusive <= pageDimensions.height)
        require(image.pageRepresentationId == pageRepresentationId && image.bounds == bounds && image.cropDigest == cropDigest)
    }
    val constituentIds: List<SourceRegionId> get() = constituentSourceRegions.map { it.id }
}

sealed interface RequestRegionShapingOutcome {
    data class Shaped(val regions: List<RequestRegion>, val sourceOrder: List<SourceRegionId>) : RequestRegionShapingOutcome
    data class Unsupported(val disposition: OrdinaryRegionDisposition, val detail: String) : RequestRegionShapingOutcome
}

class DeterministicCompleteSetRequestRegionShaper(
    private val renderer: DeterministicSourcePageRenderer = DeterministicSourcePageRenderer(),
) {
    fun shape(pages: List<AuthoritativePageRepresentation>, graphs: List<SourceRegionOrderGraph>): RequestRegionShapingOutcome {
        if (graphs.count { it.regions.isNotEmpty() } > REQUEST_REGION_MAXIMUM) return unsupported("nonempty page count exceeds 32")
        if (pages.map { it.id }.toSet() != graphs.map { it.pageRepresentationId }.toSet()) return unsupported("page/graph set mismatch")
        graphs.firstOrNull { it.ambiguityState == SourceRegionAmbiguityState.HUMAN_ORDER_REQUIRED }?.let {
            return RequestRegionShapingOutcome.Unsupported(OrdinaryRegionDisposition.SOURCE_ORDER_REVIEW_REQUIRED, it.reason ?: "human source-order review required")
        }
        graphs.firstOrNull { it.ambiguityState != SourceRegionAmbiguityState.UNAMBIGUOUS }?.let {
            return RequestRegionShapingOutcome.Unsupported(OrdinaryRegionDisposition.SOURCE_ORDER_NOT_SUPPORTED, it.reason ?: "source order not supported")
        }
        val orderedAll = RegionSourceOrderReconstructor().order(graphs).getOrElse { return unsupported("source-order graph is cyclic or inconsistent") }
        if (orderedAll.isEmpty()) return RequestRegionShapingOutcome.Unsupported(OrdinaryRegionDisposition.NO_TRANSCRIBABLE_REGIONS, "Parker derived no transcribable regions")
        val nonempty = graphs.filter { it.regions.isNotEmpty() }.sortedWith(compareBy({ pageNumber(it) }, { it.pageRepresentationId.value }))
        if (nonempty.size > REQUEST_REGION_MAXIMUM) return unsupported("nonempty page count exceeds 32")
        val orderedByPage = nonempty.associateWith { graph ->
            val byId = graph.regions.associateBy { it.id }
            RegionSourceOrderReconstructor().order(listOf(graph)).getOrElse { return unsupported("source-order graph is cyclic or inconsistent") }.map(byId::getValue)
        }
        val quotas = if (orderedAll.size <= REQUEST_REGION_MAXIMUM) nonempty.associateWith { orderedByPage.getValue(it).size }.toMutableMap()
        else allocateQuotas(nonempty, orderedByPage)
        val requestRegions = nonempty.flatMap { graph ->
            val page = pages.single { it.id == graph.pageRepresentationId }
            partition(orderedByPage.getValue(graph), quotas.getValue(graph)).map { members -> requestRegion(page, members) }
        }
        return CompleteRequestRegionSetValidator().validate(graphs, requestRegions).fold(
            onSuccess = { RequestRegionShapingOutcome.Shaped(requestRegions, orderedAll) },
            onFailure = { unsupported(it.message ?: "complete request-region invariant failed") },
        )
    }

    private fun allocateQuotas(pages: List<SourceRegionOrderGraph>, ordered: Map<SourceRegionOrderGraph, List<SourceRegion>>): MutableMap<SourceRegionOrderGraph, Int> {
        val quotas = pages.associateWith { 1 }.toMutableMap(); var remaining = REQUEST_REGION_MAXIMUM - pages.size
        while (remaining > 0) {
            val candidate = pages.filter { quotas.getValue(it) < ordered.getValue(it).size }.sortedWith(
                compareByDescending<SourceRegionOrderGraph> { ordered.getValue(it).size.toDouble() / quotas.getValue(it) }
                    .thenBy { pageNumber(it) }.thenBy { it.pageRepresentationId.value },
            ).firstOrNull() ?: break
            quotas[candidate] = quotas.getValue(candidate) + 1; remaining--
        }
        return quotas
    }

    private fun partition(source: List<SourceRegion>, count: Int) = (0 until count).map { index ->
        source.subList(index * source.size / count, (index + 1) * source.size / count).also { check(it.isNotEmpty()) }
    }

    private fun requestRegion(page: AuthoritativePageRepresentation, members: List<SourceRegion>): RequestRegion {
        val bounds = unionRequestRegionBounds(members.map { it.bounds }); val crop = renderer.crop(page, bounds)
        val encoded = requestRegionPng(crop.dimensions, crop.canonicalPixels())
        val klass = members.map { it.structuralClass }.distinct().singleOrNull() ?: SourceRegionStructuralClass.MIXED
        val id = requestRegionIdentity(page, members, bounds, crop.canonicalPixelDigest)
        return RequestRegion(id, members.first().provenance.sourceEvidenceArtifactId, members.first().provenance.sourceSha256,
            page.id, page.provenance.pageNumber, page.provenance.pixelDimensions, bounds, crop.canonicalPixelDigest, klass,
            members.first().provenance.derivationProfileId, members.first().provenance.derivationProfileVersion, members,
            RegionTranscriptionImage(page.id, bounds, crop.canonicalPixelDigest, "image/png", RegionTranscriptionImage.sha256(encoded), encoded))
    }

    private fun unsupported(detail: String) = RequestRegionShapingOutcome.Unsupported(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED, detail)
    private fun pageNumber(graph: SourceRegionOrderGraph) = graph.regions.firstOrNull()?.provenance?.pageNumber ?: Int.MAX_VALUE
}

class CompleteRequestRegionSetValidator {
    fun validate(graphs: List<SourceRegionOrderGraph>, requestRegions: List<RequestRegion>): Result<Unit> = runCatching {
        require(requestRegions.size in 1..REQUEST_REGION_MAXIMUM) { "request-region count outside 1..32" }
        require(requestRegions.map { it.id }.distinct().size == requestRegions.size) { "duplicate request-region identity" }
        val sourceById = graphs.flatMap { it.regions }.associateBy { it.id }
        require(sourceById.size == graphs.sumOf { it.regions.size }) { "duplicate source-region identity" }
        val flattened = requestRegions.flatMap { request ->
            require(request.constituentSourceRegions.all { sourceById[it.id] == it }) { "unknown or altered constituent" }
            require(request.constituentSourceRegions.all { it.provenance.pageRepresentationId == request.pageRepresentationId }) { "wrong-page or cross-page constituent" }
            require(request.bounds == unionRequestRegionBounds(request.constituentSourceRegions.map { it.bounds })) { "inconsistent request bounds" }
            val graph = graphs.single { it.pageRepresentationId == request.pageRepresentationId }
            val order = RegionSourceOrderReconstructor().order(listOf(graph)).getOrThrow(); val indexes = request.constituentIds.map(order::indexOf)
            require(indexes.all { it >= 0 } && indexes.zipWithNext().all { (a, b) -> b == a + 1 }) { "constituents are not source-adjacent and ordered" }
            request.constituentIds
        }
        require(flattened.size == flattened.distinct().size) { "duplicate constituent" }
        require(flattened.toSet() == sourceById.keys) { "missing or unknown constituent" }
        val expected = RegionSourceOrderReconstructor().order(graphs).getOrThrow()
        require(flattened == expected) { "request order does not preserve Parker source order" }
    }
}

data class RequestRegionTranscriptionRequest(
    val correlationId: String,
    val regions: List<RequestRegion>,
    val transcriptionProfileId: String = REQUEST_REGION_PROFILE_ID,
    val schemaId: String = REQUEST_REGION_SCHEMA_ID,
    val schemaVersion: Int = REQUEST_REGION_WIRE_VERSION,
    val schemaSha256: String = REQUEST_REGION_SCHEMA_SHA256,
    val processingProfile: String = REQUEST_REGION_PROCESSING_PROFILE,
    val literalInstruction: String = REQUEST_REGION_INSTRUCTION,
) {
    init {
        require(correlationId.matches(Regex("^[A-Za-z0-9_-]{1,120}$")))
        require(regions.size in 1..REQUEST_REGION_MAXIMUM && regions.map { it.id }.distinct().size == regions.size)
        require(transcriptionProfileId == REQUEST_REGION_PROFILE_ID && schemaId == REQUEST_REGION_SCHEMA_ID && schemaVersion == 6)
        require(schemaSha256 == REQUEST_REGION_SCHEMA_SHA256 && processingProfile == REQUEST_REGION_PROCESSING_PROFILE && literalInstruction == REQUEST_REGION_INSTRUCTION)
    }
}

data class RequestRegionTranscriptionBlock(
    val requestRegionId: RequestRegionId, val pageNumber: Int, val literalText: String?,
    val status: RegionTranscriptionStatus, val uncertainties: List<RegionTranscriptionUncertainty>,
    val warnings: List<String>, val providerReturnedOrdinal: Int, val visualObservations: List<RegionVisualObservation>,
)
data class RequestRegionTranscriptionResult(
    val correlationId: String, val providerProvenance: RegionTranscriptionProviderProvenance,
    val blocksInProviderOrder: List<RequestRegionTranscriptionBlock>,
)

enum class RequestRegionValidationRejection { MALFORMED, MISMATCH, UNKNOWN, MISSING, DUPLICATE, PAGE_MISMATCH, INVALID_ORDINAL, INVALID_PROVENANCE }
sealed interface RequestRegionValidationOutcome {
    data class Valid(val result: RequestRegionTranscriptionResult) : RequestRegionValidationOutcome
    data class Rejected(val reason: RequestRegionValidationRejection) : RequestRegionValidationOutcome
}

class RequestRegionTranscriptionValidator {
    fun validate(request: RequestRegionTranscriptionRequest, wire: Map<String, Any?>): RequestRegionValidationOutcome = try {
        exact(wire, setOf("correlation_id","transcription_profile_id","schema_id","schema_version","provider_provenance","blocks"))
        if (wire.string("correlation_id") != request.correlationId || wire.string("transcription_profile_id") != REQUEST_REGION_PROFILE_ID ||
            wire.string("schema_id") != REQUEST_REGION_SCHEMA_ID || wire.int("schema_version") != 6) reject(RequestRegionValidationRejection.MISMATCH)
        val provenance = provenance(wire.map("provider_provenance"))
        val requested = request.regions.associateBy { it.id }; val raw = wire.list("blocks")
        if (raw.size !in 1..32) reject(RequestRegionValidationRejection.MALFORMED)
        val blocks = raw.map { block(it as? Map<*, *> ?: reject(RequestRegionValidationRejection.MALFORMED), requested) }
        val ids = blocks.map { it.requestRegionId }
        if (ids.distinct().size != ids.size) reject(RequestRegionValidationRejection.DUPLICATE)
        if (ids.any { it !in requested }) reject(RequestRegionValidationRejection.UNKNOWN)
        if (ids.toSet() != requested.keys) reject(RequestRegionValidationRejection.MISSING)
        val ordinals = blocks.map { it.providerReturnedOrdinal }
        if (ordinals.distinct().size != ordinals.size || ordinals.toSet() != (1..blocks.size).toSet()) reject(RequestRegionValidationRejection.INVALID_ORDINAL)
        RequestRegionValidationOutcome.Valid(RequestRegionTranscriptionResult(request.correlationId, provenance, blocks))
    } catch (e: RequestRegionRejection) { RequestRegionValidationOutcome.Rejected(e.reason) }
      catch (_: Exception) { RequestRegionValidationOutcome.Rejected(RequestRegionValidationRejection.MALFORMED) }

    private fun block(raw: Map<*, *>, requested: Map<RequestRegionId, RequestRegion>): RequestRegionTranscriptionBlock {
        val v = raw.entries.associate { (k, value) -> (k as? String ?: reject(RequestRegionValidationRejection.MALFORMED)) to value }
        exact(v, setOf("request_region_id","page_number","literal_text","status","uncertainties","warnings","provider_returned_ordinal","visual_observations"))
        val id = runCatching { RequestRegionId(v.string("request_region_id")) }.getOrElse { reject(RequestRegionValidationRejection.UNKNOWN) }
        val page = v.int("page_number"); if (requested[id]?.pageNumber?.let { it != page } == true) reject(RequestRegionValidationRejection.PAGE_MISMATCH)
        val text = v.nullableString("literal_text"); val status = runCatching { RegionTranscriptionStatus.valueOf(v.string("status")) }.getOrElse { reject(RequestRegionValidationRejection.MALFORMED) }
        val uncertainties = v.list("uncertainties").map { parseRequestRegionUncertainty(it as? Map<*, *> ?: reject(RequestRegionValidationRejection.MALFORMED), text.orEmpty()) }
        val warnings = v.list("warnings").map { it as? String ?: reject(RequestRegionValidationRejection.MALFORMED) }
        val observations = v.list("visual_observations").map { parseRequestRegionObservation(it as? Map<*, *> ?: reject(RequestRegionValidationRejection.MALFORMED), text.orEmpty()) }
        when (status) {
            RegionTranscriptionStatus.TRANSCRIBED -> if (text.isNullOrEmpty() || uncertainties.isNotEmpty()) reject(RequestRegionValidationRejection.MALFORMED)
            RegionTranscriptionStatus.PARTIALLY_TRANSCRIBED -> if (text.isNullOrEmpty() || uncertainties.isEmpty()) reject(RequestRegionValidationRejection.MALFORMED)
            RegionTranscriptionStatus.ILLEGIBLE -> if (!text.isNullOrEmpty()) reject(RequestRegionValidationRejection.MALFORMED)
            else -> if (!text.isNullOrEmpty() || uncertainties.isNotEmpty()) reject(RequestRegionValidationRejection.MALFORMED)
        }
        return RequestRegionTranscriptionBlock(id, page, text, status, uncertainties, warnings, v.int("provider_returned_ordinal"), observations)
    }

    private fun provenance(v: Map<String, Any?>): RegionTranscriptionProviderProvenance {
        exact(v, setOf("provider","requested_model","provider_reported_model","provider_response_id","adapter_id","adapter_version","parser_id","parser_version"))
        val p = RegionTranscriptionProviderProvenance(v.string("provider"), v.string("requested_model"), v.nullableString("provider_reported_model"),
            v.nullableString("provider_response_id"), v.string("adapter_id"), v.string("adapter_version"), v.string("parser_id"), v.string("parser_version"))
        if (p.provider != REQUEST_REGION_PROVIDER || p.requestedModel != REQUEST_REGION_MODEL || p.adapterId != REQUEST_REGION_ADAPTER_ID ||
            p.adapterVersion != REQUEST_REGION_ADAPTER_VERSION || p.parserId != REQUEST_REGION_PARSER_ID || p.parserVersion != REQUEST_REGION_PARSER_VERSION) reject(RequestRegionValidationRejection.INVALID_PROVENANCE)
        return p
    }
    private fun exact(v: Map<String, Any?>, keys: Set<String>) { if (v.keys != keys) reject(RequestRegionValidationRejection.MALFORMED) }
    private fun Map<String, Any?>.string(k: String) = this[k] as? String ?: reject(RequestRegionValidationRejection.MALFORMED)
    private fun Map<String, Any?>.nullableString(k: String) = when(val x=this[k]) { null -> null; is String -> x; else -> reject(RequestRegionValidationRejection.MALFORMED) }
    private fun Map<String, Any?>.int(k: String) = (this[k] as? Number)?.toInt() ?: reject(RequestRegionValidationRejection.MALFORMED)
    @Suppress("UNCHECKED_CAST") private fun Map<String, Any?>.map(k:String)=this[k] as? Map<String,Any?> ?: reject(RequestRegionValidationRejection.MALFORMED)
    @Suppress("UNCHECKED_CAST") private fun Map<String, Any?>.list(k:String)=this[k] as? List<Any?> ?: reject(RequestRegionValidationRejection.MALFORMED)
    private fun reject(reason: RequestRegionValidationRejection): Nothing = throw RequestRegionRejection(reason)
    private class RequestRegionRejection(val reason: RequestRegionValidationRejection):RuntimeException(null,null,false,false)
}

class RequestRegionSourceOrderReconstructor {
    fun reconstruct(request: RequestRegionTranscriptionRequest, result: RequestRegionTranscriptionResult): Result<List<RequestRegionTranscriptionBlock>> = runCatching {
        val blocks = result.blocksInProviderOrder.associateBy { it.requestRegionId }
        require(blocks.size == result.blocksInProviderOrder.size && blocks.keys == request.regions.map { it.id }.toSet())
        request.regions.map { blocks.getValue(it.id) }
    }
}

class OpenAiRequestRegionCodec {
    fun buildRequestBody(request: RequestRegionTranscriptionRequest): String {
        val content = mutableListOf<Map<String, Any?>>(mapOf("type" to "input_text", "text" to REQUEST_REGION_INSTRUCTION + "\nrequest_correlation_id=" + request.correlationId))
        request.regions.forEach { region ->
            content += mapOf("type" to "input_text", "text" to manifest(region))
            content += mapOf("type" to "input_image", "image_url" to "data:image/png;base64," + Base64.getEncoder().encodeToString(region.image.encodedBytes()), "detail" to "original")
        }
        return RegionJson.encode(linkedMapOf("model" to REQUEST_REGION_MODEL, "store" to false, "stream" to false,
            "reasoning" to mapOf("effort" to "none"), "input" to listOf(mapOf("role" to "user", "content" to content)),
            "text" to mapOf("format" to mapOf("type" to "json_schema", "name" to REQUEST_REGION_SCHEMA_ID, "strict" to true, "schema" to RegionJson.parse(REQUEST_REGION_SCHEMA_SOURCE)))))
    }
    fun requestDigest(request: RequestRegionTranscriptionRequest) = requestRegionSha256(canonicalBinding(request))
    fun canonicalBinding(request: RequestRegionTranscriptionRequest): ByteArray {
        val targets = request.regions.map { r -> linkedMapOf<String, Any?>(
            "request_region_id" to r.id.value, "evidence_artifact_id" to r.sourceEvidenceArtifactId.value,
            "source_sha256" to r.sourceSha256, "page_representation_id" to r.pageRepresentationId.value,
            "page_number" to r.pageNumber,
            "bounds" to listOf(r.bounds.left, r.bounds.top, r.bounds.rightExclusive, r.bounds.bottomExclusive),
            "crop_digest" to r.cropDigest.value, "image_sha256" to r.image.encodedSha256,
            "constituent_source_region_ids" to r.constituentIds.map { it.value },
        ) }
        return RegionJson.encode(linkedMapOf<String, Any?>(
            "correlation_id" to request.correlationId, "profile" to request.transcriptionProfileId,
            "schema_id" to request.schemaId, "schema_version" to request.schemaVersion,
            "schema_sha256" to request.schemaSha256, "processing_profile" to request.processingProfile,
            "instruction_sha256" to REQUEST_REGION_INSTRUCTION_SHA256, "provider" to REQUEST_REGION_PROVIDER,
            "model" to REQUEST_REGION_MODEL, "adapter_id" to REQUEST_REGION_ADAPTER_ID,
            "adapter_version" to REQUEST_REGION_ADAPTER_VERSION, "reasoning" to "none", "store" to false,
            "targets" to targets,
        )).toByteArray()
    }
    private fun manifest(r: RequestRegion) = "REQUEST_REGION request_region_id=${r.id.value} page_number=${r.pageNumber} bounds=${r.bounds.left},${r.bounds.top},${r.bounds.rightExclusive},${r.bounds.bottomExclusive} crop_digest=${r.cropDigest.value} constituent_source_region_ids=${r.constituentIds.joinToString(",") { it.value }}"
}

data class PreparedRequestRegionAcquisition(
    val request: RequestRegionTranscriptionRequest, val pages: List<AuthoritativePageRepresentation>,
    val graphs: List<SourceRegionOrderGraph>, val bodyBytes: Int, val requestDigest: String,
)
sealed interface RequestRegionPreparationOutcome {
    data class Prepared(val value: PreparedRequestRegionAcquisition):RequestRegionPreparationOutcome
    data class Blocked(val disposition: OrdinaryRegionDisposition,val detail:String):RequestRegionPreparationOutcome
}

class OrdinaryRequestRegionPreparer(
    private val renderer: DeterministicSourcePageRenderer = DeterministicSourcePageRenderer(),
    private val deriver: SourceRegionDeriver = DeterministicSourceRegionDeriver(),
    private val shaper: DeterministicCompleteSetRequestRegionShaper = DeterministicCompleteSetRequestRegionShaper(renderer),
    private val codec: OpenAiRequestRegionCodec = OpenAiRequestRegionCodec(),
    private val maximumBodyBytes: Int = REQUEST_REGION_BODY_MAXIMUM_BYTES,
) {
    init { require(maximumBodyBytes > 0 && maximumBodyBytes <= REQUEST_REGION_BODY_MAXIMUM_BYTES) }
    internal fun prepare(source: AuthoritativeAcquisitionInput, attemptId: String): RequestRegionPreparationOutcome {
        if (source.mediaType != "application/pdf") return blocked(OrdinaryRegionDisposition.UNSUPPORTED_MEDIA,"request-region v6 accepts application/pdf only")
        val profile=PageRenderProfile("authoritative-page-region-raster-v1",1,300);val bytes=source.bytes()
        val first=(renderer.render(SourcePageRenderRequest(source.evidenceArtifactId,source.sha256,"application/pdf",bytes,1,profile)) as? SourcePageRepresentationOutcome.Created)?.representation
            ?:return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,"PDF rendering failed or exceeded governed source/page bounds")
        val pages=mutableListOf(first);for(number in 2..first.provenance.declaredPageCount){val page=(renderer.render(SourcePageRenderRequest(source.evidenceArtifactId,source.sha256,"application/pdf",bytes,number,profile)) as? SourcePageRepresentationOutcome.Created)?.representation
            ?:return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,"PDF rendering failed or exceeded governed source/page bounds");pages+=page}
        val graphs=pages.map{page->when(val d=deriver.derive(page)){is SourceRegionDerivationOutcome.Derived->d.graph;SourceRegionDerivationOutcome.ExcessiveRegions->return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,"region derivation exceeded its bound");else->return blocked(OrdinaryRegionDisposition.REVIEW_REQUIRED,"deterministic region derivation failed")}}
        val shaped=when(val outcome=shaper.shape(pages,graphs)){is RequestRegionShapingOutcome.Shaped->outcome;is RequestRegionShapingOutcome.Unsupported->return blocked(outcome.disposition,outcome.detail)}
        val request=RequestRegionTranscriptionRequest(attemptId,shaped.regions);val body=codec.buildRequestBody(request).toByteArray(StandardCharsets.UTF_8).size
        if(body>maximumBodyBytes)return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,"exact UTF-8 request body exceeds $maximumBodyBytes bytes")
        return RequestRegionPreparationOutcome.Prepared(PreparedRequestRegionAcquisition(request,pages,graphs,body,codec.requestDigest(request)))
    }
    private fun blocked(d:OrdinaryRegionDisposition,s:String)=RequestRegionPreparationOutcome.Blocked(d,s)
}

data class RequestRegionDerivativeBlock(
    val requestRegionId: String, val pageNumber: Int, val constituentSourceRegionIds: List<String>,
    val literalText: String?, val status: RegionTranscriptionStatus, val uncertainties: List<RegionTranscriptionUncertainty>,
    val warnings: List<String>, val visualObservations: List<RegionVisualObservation>,
)
data class RequestRegionTranscriptionDerivative(
    val capabilityId: String, val evidenceArtifactId: String, val sourceSha256: String,
    val requestDigest: String, val processingProfile: String, val blocksInParkerOrder: List<RequestRegionDerivativeBlock>,
)
class RequestRegionDerivativeBinder {
    fun bind(request: RequestRegionTranscriptionRequest, result: RequestRegionTranscriptionResult): Result<RequestRegionTranscriptionDerivative> = runCatching {
        val ordered=RequestRegionSourceOrderReconstructor().reconstruct(request,result).getOrThrow();val byId=request.regions.associateBy{it.id}
        val blocks=ordered.map{b->val r=byId.getValue(b.requestRegionId);RequestRegionDerivativeBlock(r.id.value,r.pageNumber,r.constituentIds.map{it.value},b.literalText,b.status,b.uncertainties,b.warnings,b.visualObservations)}
        RequestRegionTranscriptionDerivative(ORDINARY_REQUEST_REGION_CAPABILITY_ID,request.regions.first().sourceEvidenceArtifactId.value,
            request.regions.first().sourceSha256,OpenAiRequestRegionCodec().requestDigest(request),REQUEST_REGION_PROCESSING_PROFILE,blocks)
    }
}

fun requestRegionProviderStateBinding(request: RequestRegionTranscriptionRequest): Map<String,Any?> = linkedMapOf(
    "format" to "region-provider-state-request-binding-v2", "capability_id" to ORDINARY_REQUEST_REGION_CAPABILITY_ID,
    "request_digest" to OpenAiRequestRegionCodec().requestDigest(request), "adapter_id" to REQUEST_REGION_ADAPTER_ID,
    "adapter_version" to REQUEST_REGION_ADAPTER_VERSION, "request_region_ids" to request.regions.map{it.id.value},
    "constituent_memberships" to request.regions.map{it.constituentIds.map(SourceRegionId::value)},
)

internal fun requestRegionIdentity(page:AuthoritativePageRepresentation,members:List<SourceRegion>,bounds:PixelCropBounds,digest:CanonicalPixelDigest):RequestRegionId{
    val p=members.first().provenance;val fields=buildList{add("parker.complete-region-set-group.identity.v1");add(REQUEST_REGION_SHAPING_ID);add(page.id.value);add(p.pageNumber.toString());add(p.derivationProfileId);add(p.derivationProfileVersion.toString());add(bounds.left.toString());add(bounds.top.toString());add(bounds.rightExclusive.toString());add(bounds.bottomExclusive.toString());add(digest.value);add(members.size.toString());members.forEach{add(it.id.value)}}
    val md=MessageDigest.getInstance("SHA-256");fields.forEach{v->val b=v.toByteArray();md.update(ByteBuffer.allocate(4).putInt(b.size).array());md.update(b)};return RequestRegionId(md.digest().joinToString(""){"%02x".format(it.toInt()and 255)})
}
internal fun unionRequestRegionBounds(b:List<PixelCropBounds>)=PixelCropBounds(b.minOf{it.left},b.minOf{it.top},b.maxOf{it.rightExclusive},b.maxOf{it.bottomExclusive})
internal fun requestRegionPng(d:PagePixelDimensions,p:ByteArray):ByteArray{val image=BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_RGB);var i=0;for(y in 0 until d.height)for(x in 0 until d.width)image.setRGB(x,y,((p[i++].toInt()and 255)shl 16)or((p[i++].toInt()and 255)shl 8)or(p[i++].toInt()and 255));return ByteArrayOutputStream().use{out->check(ImageIO.write(image,"png",out));out.toByteArray()}}
internal fun requestRegionSha256(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}

private fun parseRequestRegionUncertainty(raw:Map<*,*>,text:String):RegionTranscriptionUncertainty{val v=raw.entries.associate{(k,x)->(k as? String?:error("key"))to x};require(v.keys==setOf("start_code_point","end_code_point_exclusive","exact_substring","category","alternatives","provider_confidence"));val s=(v["start_code_point"]as Number).toInt();val e=(v["end_code_point_exclusive"]as Number).toInt();require(s>=0&&e>s&&e<=text.codePointCount(0,text.length));val exact=v["exact_substring"]as String;require(exact==text.substring(text.offsetByCodePoints(0,s),text.offsetByCodePoints(0,e)));return RegionTranscriptionUncertainty(s,e,exact,RegionTranscriptionUncertaintyCategory.valueOf(v["category"]as String),(v["alternatives"]as List<*>).map{it as String},v["provider_confidence"]as? String)}
private fun parseRequestRegionObservation(raw:Map<*,*>,text:String):RegionVisualObservation{val v=raw.entries.associate{(k,x)->(k as? String?:error("key"))to x};require(v.keys==setOf("kind","start_code_point","end_code_point_exclusive"));val kind=RegionVisualObservationKind.valueOf(v["kind"]as String);val s=(v["start_code_point"]as? Number)?.toInt();val e=(v["end_code_point_exclusive"]as? Number)?.toInt();val anchor=RegionVisualObservationAnchor.resolve(s,e,text.codePointCount(0,text.length),true);require((anchor is RegionVisualObservationAnchor.Point)==(kind==RegionVisualObservationKind.LINE_BREAK)||anchor is RegionVisualObservationAnchor.Unanchored);return RegionVisualObservation(kind,s,e)}
