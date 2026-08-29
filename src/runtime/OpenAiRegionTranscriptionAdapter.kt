package parker.core.runtime

import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpTimeoutException
import java.security.MessageDigest
import java.util.Base64
import parker.composition.OpenAiApiCredential
import parker.composition.ExternalTranscriptionAcceptanceState
import parker.core.interfaces.*

const val OPENAI_REGION_ADAPTER_ID = "openai-responses-region-transcription-adapter"
const val OPENAI_REGION_ADAPTER_VERSION = "3.0.0"
const val OPENAI_REGION_PARSER_ID = "openai-region-structured-response-parser"
const val OPENAI_REGION_PARSER_VERSION = "1.0.0"
const val OPENAI_REGION_PROFILE_ID = "openai-region-anchored-transcription-v1"
const val OPENAI_REGION_MODEL = "gpt-5.6-sol"
const val OPENAI_REGION_IMAGE_DETAIL = "original"

const val OPENAI_REGION_INSTRUCTION = "Transcribe only text visibly present in each supplied Parker target region. Preserve exact visible Unicode, spelling, punctuation, capitalization, numbers, dates, identifiers, line breaks, paragraph breaks, repeated spaces, tabs, indentation, and significant emphasis. Bind every block only to its supplied source_region_id and page_number. Report bounded uncertainty instead of guessing. Use NO_VISIBLE_TEXT for no visible text and ILLEGIBLE for unreadable content. Never summarize, interpret, reason, rewrite, correct, normalize, infer, complete, regroup, move text between regions, create region IDs, transcribe surrounding page context, or decide source order. Provider-returned ordinal is forensic metadata only. Return only the strict structured schema."
val OPENAI_REGION_INSTRUCTION_SHA256 = regionSha256(OPENAI_REGION_INSTRUCTION.toByteArray(Charsets.UTF_8))
val OPENAI_REGION_WIRE_SCHEMA_SOURCE = REGION_TRANSCRIPTION_SCHEMA_SOURCE
val OPENAI_REGION_WIRE_SCHEMA_SHA256 = regionSha256(OPENAI_REGION_WIRE_SCHEMA_SOURCE.toByteArray(Charsets.UTF_8))

data class OpenAiRegionTranscriptionProfile(
    val profileId: String = OPENAI_REGION_PROFILE_ID,
    val provider: String = "OpenAI",
    val model: String = OPENAI_REGION_MODEL,
    val providerNeutralProfileId: String = REGION_TRANSCRIPTION_PROFILE_ID,
    val schemaId: String = REGION_TRANSCRIPTION_SCHEMA_ID,
    val wireVersion: Int = REGION_TRANSCRIPTION_WIRE_VERSION,
    val schemaSha256: String = REGION_TRANSCRIPTION_SCHEMA_SHA256,
    val instructionSha256: String = OPENAI_REGION_INSTRUCTION_SHA256,
    val processingProfile: String = REGION_TRANSCRIPTION_PROCESSING_PROFILE,
    val adapterId: String = OPENAI_REGION_ADAPTER_ID,
    val adapterVersion: String = OPENAI_REGION_ADAPTER_VERSION,
    val reasoning: String = "none",
    val store: Boolean = false,
    val imageDetail: String = OPENAI_REGION_IMAGE_DETAIL,
    val lifecycle: ExternalTranscriptionAcceptanceState = ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING,
) {
    init {
        require(profileId == OPENAI_REGION_PROFILE_ID && provider == "OpenAI" && model == OPENAI_REGION_MODEL)
        require(providerNeutralProfileId == REGION_TRANSCRIPTION_PROFILE_ID && schemaId == REGION_TRANSCRIPTION_SCHEMA_ID)
        require(wireVersion == REGION_TRANSCRIPTION_WIRE_VERSION && schemaSha256 == REGION_TRANSCRIPTION_SCHEMA_SHA256)
        require(instructionSha256 == OPENAI_REGION_INSTRUCTION_SHA256 && processingProfile == REGION_TRANSCRIPTION_PROCESSING_PROFILE)
        require(adapterId == OPENAI_REGION_ADAPTER_ID && adapterVersion == OPENAI_REGION_ADAPTER_VERSION)
        require(reasoning == "none" && !store && imageDetail == "original")
        require(lifecycle == ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING)
    }
}

data class OpenAiRegionRawResponseState(
    val responseId: String,
    val providerReportedModel: String,
    val responseSha256: String,
    val structuredSegmentSha256: String,
    val exactStructuredResponse: Map<String, Any?>,
    private val boundedResponseBody: ByteArray,
) {
    fun responseBody(): ByteArray = boundedResponseBody.copyOf()
}

sealed interface OpenAiRegionAdapterOutcome {
    data class Success(val result: RegionTranscriptionResult, val rawState: OpenAiRegionRawResponseState) : OpenAiRegionAdapterOutcome
    data class Failure(val code: String) : OpenAiRegionAdapterOutcome
}

class OpenAiRegionTranscriptionAdapter internal constructor(
    private val credential: OpenAiApiCredential,
    private val transport: OpenAiResponsesTransport,
    private val profile: OpenAiRegionTranscriptionProfile = OpenAiRegionTranscriptionProfile(),
    private val validator: RegionTranscriptionValidator = RegionTranscriptionValidator(),
    private val timeoutMillis: Long = 300_000,
    private val maximumResponseBytes: Long = 20L * 1024L * 1024L,
    private val providerStateStore: FileSystemRegionProviderStateStore? = null,
) : RegionExternalTranscriptionMechanism {
    init { require(timeoutMillis > 0 && maximumResponseBytes in 1..20L * 1024L * 1024L) }

    override suspend fun transcribe(request: RegionTranscriptionRequest): RegionExternalTranscriptionOutcome = when (val outcome = transcribeWithRawState(request)) {
        is OpenAiRegionAdapterOutcome.Success -> RegionExternalTranscriptionOutcome.Candidate(outcome.rawState.exactStructuredResponse)
        is OpenAiRegionAdapterOutcome.Failure -> RegionExternalTranscriptionOutcome.Failure(outcome.code)
    }

    suspend fun transcribeWithRawState(request: RegionTranscriptionRequest): OpenAiRegionAdapterOutcome {
        if (!matchesProfile(request)) return failure("PROFILE_MISMATCH")
        val response = try {
            transport.execute(OpenAiResponsesTransportRequest(ENDPOINT, timeoutMillis, buildRequestBody(request), maximumResponseBytes, credential))
        } catch (_: HttpTimeoutException) {
            return failure("PROVIDER_TIMEOUT")
        } catch (_: Exception) {
            return failure("PROVIDER_TRANSPORT_FAILURE")
        }
        val receipt = providerStateStore?.persistReceived(request, response.statusCode, null, response.body)
        val outcome = if (response.statusCode !in 200..299) failure(httpFailure(response.statusCode)) else parseResponse(request, response.body)
        receipt?.let { saved -> providerStateStore?.recordAssessment(saved, (outcome as? OpenAiRegionAdapterOutcome.Failure)?.code ?: "SUCCESS", structuredForPersistence(response.body)) }
        return outcome
    }

    private fun structuredForPersistence(bytes: ByteArray): Map<String, Any?>? {
        return try {
            val envelope = RegionJson.parse(bytes.toString(Charsets.UTF_8)) as? Map<*, *> ?: return null
            val text = outputText(envelope) ?: return null
            @Suppress("UNCHECKED_CAST") (RegionJson.parse(text) as? Map<String, Any?>)
        } catch (_: Exception) { null }
    }

    internal fun buildRequestBody(request: RegionTranscriptionRequest): String {
        require(matchesProfile(request))
        val content = mutableListOf<Map<String, Any?>>()
        content += mapOf("type" to "input_text", "text" to OPENAI_REGION_INSTRUCTION + "\nrequest_correlation_id=" + request.correlationId)
        request.targets.forEach { target ->
            content += mapOf("type" to "input_text", "text" to manifest(target, false))
            content += mapOf("type" to "input_image", "image_url" to dataUrl(target.regionImage), "detail" to profile.imageDetail)
            target.pageContextImage?.let {
                content += mapOf("type" to "input_text", "text" to manifest(target, true))
                content += mapOf("type" to "input_image", "image_url" to dataUrl(it), "detail" to profile.imageDetail)
            }
        }
        val body = linkedMapOf<String, Any?>(
            "model" to profile.model,
            "store" to false,
            "stream" to false,
            "reasoning" to mapOf("effort" to "none"),
            "input" to listOf(mapOf("role" to "user", "content" to content)),
            "text" to mapOf("format" to mapOf("type" to "json_schema", "name" to REGION_TRANSCRIPTION_SCHEMA_ID,
                "strict" to true, "schema" to RegionJson.parse(OPENAI_REGION_WIRE_SCHEMA_SOURCE))),
        )
        return RegionJson.encode(body)
    }

    private fun parseResponse(request: RegionTranscriptionRequest, bytes: ByteArray): OpenAiRegionAdapterOutcome {
        val rawSha = regionSha256(bytes)
        val envelope = try { RegionJson.parse(bytes.toString(Charsets.UTF_8)) as? Map<*, *> ?: return failure("MALFORMED_PROVIDER_RESPONSE") }
        catch (_: Exception) { return failure("MALFORMED_PROVIDER_RESPONSE") }
        if (containsRefusal(envelope)) return failure("PROVIDER_REFUSAL")
        val responseId = envelope["id"] as? String ?: return failure("MALFORMED_PROVIDER_RESPONSE")
        val model = envelope["model"] as? String ?: return failure("MALFORMED_PROVIDER_RESPONSE")
        if (model != profile.model) return failure("PROVIDER_MODEL_MISMATCH")
        val structuredText = outputText(envelope) ?: return failure("MISSING_STRUCTURED_OUTPUT")
        if (structuredText.toByteArray().size > maximumResponseBytes) return failure("EXCESSIVE_PROVIDER_OUTPUT")
        val structured = try {
            @Suppress("UNCHECKED_CAST")
            RegionJson.parse(structuredText) as? Map<String, Any?> ?: return failure("SCHEMA_INVALID_RESPONSE")
        } catch (_: Exception) { return failure("SCHEMA_INVALID_RESPONSE") }
        return when (val validated = validator.validate(request, structured)) {
            is RegionTranscriptionValidationOutcome.Valid -> {
                val p = validated.result.providerProvenance
                if (p.provider != "OpenAI" || p.requestedModel != profile.model || p.providerReportedModel != model ||
                    p.providerResponseId != responseId || p.adapterId != OPENAI_REGION_ADAPTER_ID || p.adapterVersion != OPENAI_REGION_ADAPTER_VERSION ||
                    p.parserId != OPENAI_REGION_PARSER_ID || p.parserVersion != OPENAI_REGION_PARSER_VERSION
                ) return failure("INVALID_PROVIDER_PROVENANCE")
                OpenAiRegionAdapterOutcome.Success(validated.result,
                    OpenAiRegionRawResponseState(responseId, model, rawSha, regionSha256(structuredText.toByteArray()), structured, bytes.copyOf()))
            }
            is RegionTranscriptionValidationOutcome.Rejected -> failure("VALIDATION_${validated.reason.name}")
        }
    }

    private fun matchesProfile(request: RegionTranscriptionRequest) = request.transcriptionProfileId == profile.providerNeutralProfileId &&
        request.schemaId == profile.schemaId && request.schemaVersion == profile.wireVersion && request.schemaSha256 == profile.schemaSha256 &&
        request.processingProfile == profile.processingProfile && request.literalInstruction == REGION_LITERAL_TRANSCRIPTION_INSTRUCTION
    private fun manifest(target: RegionTranscriptionTarget, context: Boolean) = if (context) {
        "PAGE_CONTEXT_ONLY source_region_id=${target.sourceRegionId.value} page_number=${target.pageNumber} target_bounds=${target.bounds.left},${target.bounds.top},${target.bounds.rightExclusive},${target.bounds.bottomExclusive}; do not transcribe outside target"
    } else {
        "TARGET source_region_id=${target.sourceRegionId.value} page_number=${target.pageNumber} bounds=${target.bounds.left},${target.bounds.top},${target.bounds.rightExclusive},${target.bounds.bottomExclusive} crop_digest=${target.cropDigest.value}"
    }
    private fun dataUrl(image: RegionTranscriptionImage) = "data:${image.encodedMediaType};base64," + Base64.getEncoder().encodeToString(image.encodedBytes())
    private fun outputText(envelope: Map<*, *>): String? = (envelope["output"] as? List<*>)?.asSequence()?.mapNotNull { it as? Map<*, *> }
        ?.filter { it["type"] == "message" }?.flatMap { ((it["content"] as? List<*>) ?: emptyList<Any?>()).asSequence() }
        ?.mapNotNull { it as? Map<*, *> }?.firstOrNull { it["type"] == "output_text" }?.get("text") as? String
    private fun containsRefusal(envelope: Map<*, *>) = (envelope["output"] as? List<*>)?.flatMap { ((it as? Map<*, *>)?.get("content") as? List<*>) ?: emptyList<Any?>() }
        ?.mapNotNull { it as? Map<*, *> }?.any { it["type"] == "refusal" } == true
    private fun httpFailure(status: Int) = when (status) { HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> "PROVIDER_AUTHENTICATION_FAILURE"; 429 -> "PROVIDER_RATE_LIMITED"; in 300..399 -> "PROVIDER_REDIRECT_REJECTED"; in 500..599 -> "PROVIDER_UNAVAILABLE"; else -> "PROVIDER_REJECTED_REQUEST" }
    private fun failure(code: String) = OpenAiRegionAdapterOutcome.Failure(code)
    companion object { val ENDPOINT: URI = URI("https://api.openai.com/v1/responses") }
}

internal object RegionJson {
    fun parse(text: String): Any? = Reader(text).parse()
    fun encode(value: Any?): String = when (value) {
        null -> "null"; is String -> "\"${escape(value)}\""; is Boolean, is Int, is Long -> value.toString()
        is BigDecimal -> value.stripTrailingZeros().toPlainString()
        is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { (k, v) -> encode(k as String) + ":" + encode(v) }
        is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { encode(it) }
        else -> error("unsupported JSON value")
    }
    private fun escape(value: String) = buildString { value.forEach { c -> when (c) { '\\' -> append("\\\\"); '"' -> append("\\\""); '\b' -> append("\\b"); '\t' -> append("\\t"); '\n' -> append("\\n"); '\u000C' -> append("\\f"); '\r' -> append("\\r"); else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c) } } }
    private class Reader(private val source: String) {
        private var i = 0
        fun parse(): Any? { val v = value(); ws(); require(i == source.length); return v }
        private fun value(): Any? { ws(); require(i < source.length); return when (source[i]) { '{' -> obj(); '[' -> arr(); '"' -> str(); 't' -> literal("true", true); 'f' -> literal("false", false); 'n' -> literal("null", null); else -> num() } }
        private fun obj(): Map<String, Any?> { i++; val m = linkedMapOf<String, Any?>(); ws(); if (take('}')) return m; do { val k = str(); require(k !in m); ws(); require(take(':')); m[k] = value(); ws() } while (take(',')); require(take('}')); return m }
        private fun arr(): List<Any?> { i++; val a = mutableListOf<Any?>(); ws(); if (take(']')) return a; do { a += value(); ws() } while (take(',')); require(take(']')); return a }
        private fun str(): String { require(take('"')); val b = StringBuilder(); while (i < source.length) { val c = source[i++]; if (c == '"') return b.toString(); if (c != '\\') { require(c >= ' '); b.append(c); continue }; require(i < source.length); when (val e = source[i++]) { '"','\\','/' -> b.append(e); 'b' -> b.append('\b'); 'f' -> b.append('\u000C'); 'n' -> b.append('\n'); 'r' -> b.append('\r'); 't' -> b.append('\t'); 'u' -> { require(i + 4 <= source.length); b.append(source.substring(i, i + 4).toInt(16).toChar()); i += 4 }; else -> error("escape") } }; error("string") }
        private fun num(): BigDecimal { val s = i; if (source[i] == '-') i++; while (i < source.length && (source[i].isDigit() || source[i] in ".eE+-")) i++; return source.substring(s, i).toBigDecimal() }
        private fun <T> literal(s: String, v: T): T { require(source.startsWith(s, i)); i += s.length; return v }
        private fun take(c: Char): Boolean { ws(); if (i < source.length && source[i] == c) { i++; return true }; return false }
        private fun ws() { while (i < source.length && source[i].isWhitespace()) i++ }
    }
}

internal fun regionSha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
