package parker.core.runtime

import java.io.IOException
import java.io.UncheckedIOException
import java.net.URI
import java.net.ConnectException
import java.net.UnknownHostException
import java.net.http.HttpClient
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import javax.net.ssl.SSLException
import kotlinx.coroutines.suspendCancellableCoroutine
import parker.composition.OpenAiApiCredential
import parker.composition.OpenAiExternalTranscriptionReadiness
import parker.core.interfaces.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class OpenAiResponsesTransportRequest(
    val endpoint: URI,
    val timeoutMillis: Long,
    val body: String,
    val maximumResponseBytes: Long,
    val credential: OpenAiApiCredential,
) {
    override fun toString() = "OpenAiResponsesTransportRequest(endpoint=$endpoint, timeoutMillis=$timeoutMillis, body=<redacted>, maximumResponseBytes=$maximumResponseBytes, credential=$credential)"
}

internal data class OpenAiResponsesTransportResponse(val statusCode: Int, val body: ByteArray)

internal fun interface OpenAiResponsesTransport {
    suspend fun execute(request: OpenAiResponsesTransportRequest): OpenAiResponsesTransportResponse
}

/** JDK transport boundary: the only place that constructs the bearer header. */
internal class JdkOpenAiResponsesTransport(
    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(30))
        .build(),
) : OpenAiResponsesTransport {
    override suspend fun execute(request: OpenAiResponsesTransportRequest): OpenAiResponsesTransportResponse {
        val httpRequest = try {
            buildHttpRequest(request)
        } catch (e: IllegalArgumentException) {
            throw OpenAiRequestConstructionException(e)
        }
        return suspendCancellableCoroutine { continuation ->
            val future = client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
            future.whenComplete { response, error ->
                if (error != null) {
                    continuation.resumeWithException((error as? CompletionException)?.cause ?: error)
                } else {
                    try {
                        response.body().use { stream ->
                            val bytes = stream.readNBytes(Math.addExact(request.maximumResponseBytes, 1).toInt())
                            if (bytes.size.toLong() > request.maximumResponseBytes) throw OpenAiResponseTooLargeException()
                            continuation.resume(OpenAiResponsesTransportResponse(response.statusCode(), bytes))
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
            continuation.invokeOnCancellation { future.cancel(true) }
        }
    }

    internal fun buildHttpRequest(request: OpenAiResponsesTransportRequest): HttpRequest =
        request.credential.useValue { value ->
            HttpRequest.newBuilder(request.endpoint)
                .timeout(Duration.ofMillis(request.timeoutMillis))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $value")
                .POST(HttpRequest.BodyPublishers.ofString(request.body))
                .build()
        }
}

internal class OpenAiResponseTooLargeException : IOException("OpenAI response exceeded the configured bound")
internal class OpenAiRequestConstructionException(cause: IllegalArgumentException) :
    RuntimeException(null, cause, false, false)

/** Concrete, non-composed OpenAI Responses API adapter behind Unit E's provider-neutral seam. */
class OpenAiResponsesExternalTranscriptionAdapter internal constructor(
    readiness: OpenAiExternalTranscriptionReadiness.Ready,
    private val credential: OpenAiApiCredential,
    private val transport: OpenAiResponsesTransport,
    private val maximumEncodedRequestBytes: Long = MAXIMUM_ENCODED_REQUEST_BYTES,
    private val transportFailureObserver: (OpenAiTransportFailureFingerprint) -> Unit = {},
    private val providerRejectionObserver: (OpenAiProviderErrorFingerprint) -> Unit = {},
) : ExternalTranscriptionMechanism {
    private val profile = readiness.profile
    private val limits = readiness.effectiveLimits
    private val endpoint = URI.create(profile.allowedNetworkDestination).resolve(profile.apiProductPath)

    init {
        require(endpoint == URI.create("https://api.openai.com/v1/responses")) { "OpenAI Responses endpoint is not approved" }
        require(!profile.store) { "OpenAI adapter requires store=false" }
    }

    override suspend fun transcribe(request: ExternalTranscriptionRequest): ExternalTranscriptionMechanismOutcome {
        val sourceLimit = if (request.mediaType == "application/pdf") limits.maximumPdfBytes else limits.maximumImageBytes
        if (request.content.size.toLong() > sourceLimit) return failure("INPUT_TOO_LARGE")
        val encodedLength = 4L * ((request.content.size.toLong() + 2L) / 3L)
        if (encodedLength + REQUEST_OVERHEAD_ALLOWANCE > maximumEncodedRequestBytes) return failure("ENCODED_INPUT_TOO_LARGE")

        val body = buildRequestBody(request)
        val response = try {
            transport.execute(OpenAiResponsesTransportRequest(endpoint, limits.timeoutMillis, body, limits.maximumOutputBytes, credential))
        } catch (_: OpenAiResponseTooLargeException) {
            return failure("RESPONSE_TOO_LARGE")
        } catch (_: kotlinx.coroutines.CancellationException) {
            throw kotlinx.coroutines.CancellationException("OpenAI transcription cancelled")
        } catch (e: Exception) {
            val fingerprint = fingerprintOpenAiTransportFailure(e)
            runCatching { transportFailureObserver(fingerprint) }
            return failure(fingerprint.category)
        }
        if (response.body.size.toLong() > limits.maximumOutputBytes) return failure("RESPONSE_TOO_LARGE")
        val rejectionCategory = when (response.statusCode) {
            in 200..299 -> null
            in 300..399 -> "PROVIDER_REDIRECT_REJECTED"
            401, 403 -> "PROVIDER_AUTHENTICATION_FAILURE"
            429 -> "PROVIDER_RATE_LIMITED"
            in 500..599 -> "PROVIDER_UNAVAILABLE"
            else -> "PROVIDER_REJECTED_REQUEST"
        }
        if (rejectionCategory != null) {
            runCatching { providerRejectionObserver(fingerprintOpenAiProviderRejection(response, rejectionCategory)) }
            return failure(rejectionCategory)
        }
        return try {
            ExternalTranscriptionMechanismOutcome.Candidate(parseResponse(request, response.body.toString(Charsets.UTF_8)))
        } catch (_: Exception) {
            failure("MALFORMED_PROVIDER_RESPONSE")
        }
    }

    private fun buildRequestBody(request: ExternalTranscriptionRequest): String {
        val base64 = Base64.getEncoder().encodeToString(request.content)
        val mediaPart = if (request.mediaType == "application/pdf") {
            "{\"type\":\"input_file\",\"filename\":\"source.pdf\",\"file_data\":\"data:application/pdf;base64,$base64\"}"
        } else {
            "{\"type\":\"input_image\",\"detail\":\"high\",\"image_url\":\"data:${jsonEscape(request.mediaType)};base64,$base64\"}"
        }
        return "{" +
            "\"model\":\"${jsonEscape(profile.modelSelectionRule)}\"," +
            "\"store\":false,\"stream\":false," +
            "\"instructions\":\"${jsonEscape(TRANSCRIPTION_INSTRUCTION)}\"," +
            "\"input\":[{\"role\":\"user\",\"content\":[" +
            "{\"type\":\"input_text\",\"text\":\"Transcribe this source under the developer instruction. Maximum page count: ${request.maximumPageCount}.\"}," +
            mediaPart + "]}]," +
            "\"text\":{\"format\":{\"type\":\"json_schema\",\"name\":\"parker_page_transcription\",\"strict\":true,\"schema\":" + STRUCTURED_SCHEMA + "}}}"
    }

    private fun parseResponse(request: ExternalTranscriptionRequest, raw: String): OcrStructuredTranscriptionCandidate {
        val envelope = Json.parse(raw).objectValue()
        val responseId = envelope.requiredString("id").also { require(ID_PATTERN.matches(it)) }
        val model = envelope.requiredString("model").also { require(it.isNotBlank() && !it.equals("unknown", true)) }
        val outputText = envelope.requiredArray("output").flatMap { it.objectValue().requiredArray("content") }
            .map { it.objectValue() }.single { it.requiredString("type") == "output_text" }.requiredString("text")
        val result = Json.parse(outputText).objectValue()
        val requested = OcrPageScope(result.requiredIntArray("requested_pages"))
        val returned = OcrPageScope(result.requiredIntArray("returned_pages"))
        require(requested.pageNumbers.size <= request.maximumPageCount)
        val pages = result.requiredArray("pages").map { pageValue ->
            val page = pageValue.objectValue()
            val pageNumber = page.requiredInt("page_number")
            val text = page.nullableString("text")
            val outcome = OcrPageOutcomeKind.valueOf(page.requiredString("outcome"))
            val classification = page.nullableString("reason_classification")
            val reason = classification?.let { OcrPageOutcomeReason(it, page.nullableString("reason_detail")) }
            val warnings = page.requiredStringArray("warnings")
            val spans = page.requiredArray("uncertainty_spans").map { spanValue ->
                val span = spanValue.objectValue()
                OcrUncertaintySpan(
                    pageNumber, span.requiredInt("start"), span.requiredInt("end"),
                    OcrUncertaintyKind.valueOf(span.requiredString("kind")), span.requiredString("disclosure"),
                )
            }
            OcrStructuredPageCandidate(pageNumber, text, outcome, reason, warnings, spans)
        }
        return OcrStructuredTranscriptionCandidate(
            requested, requested, returned, pages, TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
            OcrRecognitionIdentity("openai-responses", TRANSCRIPTION_PROFILE_ID, ADAPTER_VERSION),
            OcrProviderProvenance("OpenAI", "openai-responses-adapter", ADAPTER_VERSION, TRANSCRIPTION_PROFILE_ID, model, OcrModelSnapshot.NotExposed, responseId),
            request.processingProvenance, Instant.now(), result.requiredStringArray("warnings"),
        )
    }

    private fun failure(code: String) = ExternalTranscriptionMechanismOutcome.Failure(code)

    private companion object {
        const val ADAPTER_VERSION = "1.0.0"
        const val TRANSCRIPTION_PROFILE_ID = "openai-faithful-page-transcription-v1"
        const val REQUEST_OVERHEAD_ALLOWANCE = 64L * 1024L
        const val MAXIMUM_ENCODED_REQUEST_BYTES = 96L * 1024L * 1024L
        val ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,1024}$")
        const val TRANSCRIPTION_INSTRUCTION = "Faithfully transcribe readable source text with page association. Mark uncertainty and illegible content; do not guess. Do not summarize, interpret, translate, perform legal analysis, correct substantive wording, resolve factual conflicts, browse, retrieve external information, or use tools. Return only the strict structured schema."
        val STRUCTURED_SCHEMA = """{"type":"object","additionalProperties":false,"required":["requested_pages","returned_pages","pages","warnings"],"properties":{"requested_pages":{"type":"array","items":{"type":"integer","minimum":1}},"returned_pages":{"type":"array","items":{"type":"integer","minimum":1}},"warnings":{"type":"array","items":{"type":"string"}},"pages":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["page_number","text","outcome","reason_classification","reason_detail","warnings","uncertainty_spans"],"properties":{"page_number":{"type":"integer","minimum":1},"text":{"type":["string","null"]},"outcome":{"type":"string","enum":["TRANSCRIBED","TRANSCRIBED_WITH_QUALIFICATIONS","ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT","FAILED","NOT_RETURNED"]},"reason_classification":{"type":["string","null"]},"reason_detail":{"type":["string","null"]},"warnings":{"type":"array","items":{"type":"string"}},"uncertainty_spans":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["start","end","kind","disclosure"],"properties":{"start":{"type":"integer","minimum":0},"end":{"type":"integer","minimum":1},"kind":{"type":"string","enum":["UNCERTAIN","ILLEGIBLE"]},"disclosure":{"type":"string"}}}}}}}}}"""
    }
}

/** Acceptance-only safe projection of an HTTP error envelope; raw provider text never escapes. */
internal data class OpenAiProviderErrorFingerprint(
    val httpStatus: Int,
    val providerErrorType: String,
    val providerErrorCode: String,
    val providerErrorParam: String,
    val category: String,
) {
    fun render(): String =
        "HTTP_STATUS=$httpStatus PROVIDER_ERROR_TYPE=$providerErrorType PROVIDER_ERROR_CODE=$providerErrorCode " +
            "PROVIDER_ERROR_PARAM=$providerErrorParam CATEGORY=$category"
}

internal fun fingerprintOpenAiProviderRejection(
    response: OpenAiResponsesTransportResponse,
    category: String,
): OpenAiProviderErrorFingerprint {
    val error = runCatching { (Json.parse(response.body.toString(Charsets.UTF_8)) as? Json.Obj)?.fields?.get("error") as? Json.Obj }
        .getOrNull()
    fun field(name: String): String = ((error?.fields?.get(name) as? Json.Str)?.value)
        ?.takeIf { value -> value.length in 1..128 && value.all { it.isLetterOrDigit() || it in "_-.[]" } }
        ?: "NOT_EXPOSED"
    return OpenAiProviderErrorFingerprint(response.statusCode, field("type"), field("code"), field("param"), category)
}

/**
 * Content-safe transport diagnostics. Only a frozen category crosses the adapter boundary: never
 * an exception message, URI, header, request/response body, credential, or stack trace. The full
 * bounded cause chain is inspected because JDK HttpClient commonly wraps the useful network cause
 * in CompletionException/IOException layers.
 */
internal data class OpenAiTransportFailureFingerprint(
    val topLevelThrowable: String,
    val firstNonWrapperCause: String,
    val stage: String,
    val category: String,
) {
    fun render(): String =
        "TRANSPORT_THROWABLE=$topLevelThrowable ROOT_CAUSE=$firstNonWrapperCause TRANSPORT_STAGE=$stage CATEGORY=$category"
}

internal fun fingerprintOpenAiTransportFailure(error: Throwable): OpenAiTransportFailureFingerprint {
    val causes = boundedCauseChain(error)
    val firstNonWrapper = causes.firstOrNull {
        it !is CompletionException && it !is ExecutionException && it !is UncheckedIOException &&
            it !is OpenAiRequestConstructionException
    } ?: causes.last()
    return OpenAiTransportFailureFingerprint(
        topLevelThrowable = safeThrowableClassName(error),
        firstNonWrapperCause = safeThrowableClassName(firstNonWrapper),
        stage = if (causes.any { it is OpenAiRequestConstructionException }) "REQUEST_BUILD" else "CLIENT_SEND_OR_RESPONSE_READ",
        category = classifyOpenAiTransportFailure(causes),
    )
}

internal fun classifyOpenAiTransportFailure(error: Throwable): String =
    classifyOpenAiTransportFailure(boundedCauseChain(error))

private fun boundedCauseChain(error: Throwable): List<Throwable> {
    val causes = mutableListOf<Throwable>()
    val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Throwable, Boolean>())
    var current: Throwable? = error
    while (current != null && causes.size < 16 && seen.add(current)) {
        causes += current
        current = current.cause
    }
    return causes
}

private fun classifyOpenAiTransportFailure(causes: List<Throwable>): String = when {
        causes.any { it is OpenAiRequestConstructionException } -> "PROVIDER_REQUEST_CONFIGURATION_FAILURE"
        causes.any { it is HttpConnectTimeoutException } -> "PROVIDER_CONNECT_TIMEOUT"
        causes.any { it is HttpTimeoutException } -> "PROVIDER_REQUEST_TIMEOUT"
        causes.any { it is SSLException } -> "PROVIDER_TLS_FAILURE"
        causes.any { it is UnknownHostException } -> "PROVIDER_DNS_FAILURE"
        causes.any { it is ConnectException } -> "PROVIDER_CONNECT_FAILURE"
        causes.any { it is InterruptedException } -> "PROVIDER_INTERRUPTED"
        causes.any { it is IOException } -> "PROVIDER_IO_FAILURE"
        else -> "PROVIDER_TRANSPORT_FAILURE"
}

private fun safeThrowableClassName(error: Throwable): String =
    error::class.simpleName.orEmpty().takeIf { it.length in 1..128 && it.all { c -> c.isLetterOrDigit() || c == '_' || c == '$' } }
        ?: "UnknownThrowable"

private fun jsonEscape(value: String): String = buildString(value.length) {
    value.forEach { c -> when (c) { '\\' -> append("\\\\"); '"' -> append("\\\""); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t"); else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c) } }
}

private sealed interface Json {
    data class Obj(val fields: Map<String, Json>) : Json
    data class Arr(val values: List<Json>) : Json
    data class Str(val value: String) : Json
    data class Num(val value: Long) : Json
    data object Null : Json
    data class Bool(val value: Boolean) : Json
    companion object { fun parse(text: String): Json = Reader(text).parse() }
}

private class Reader(private val source: String) {
    private var index = 0
    fun parse(): Json { val value = value(); space(); require(index == source.length); return value }
    private fun value(): Json { space(); require(index < source.length); return when (source[index]) {
        '{' -> obj(); '[' -> array(); '"' -> Json.Str(string()); 'n' -> literal("null", Json.Null)
        't' -> literal("true", Json.Bool(true)); 'f' -> literal("false", Json.Bool(false)); else -> number()
    } }
    private fun obj(): Json.Obj { index++; space(); val map = linkedMapOf<String, Json>(); if (take('}')) return Json.Obj(map); do { space(); val key = string(); space(); require(take(':')); require(key !in map); map[key] = value(); space() } while (take(',')); require(take('}')); return Json.Obj(map) }
    private fun array(): Json.Arr { index++; space(); val list = mutableListOf<Json>(); if (take(']')) return Json.Arr(list); do { list += value(); space() } while (take(',')); require(take(']')); return Json.Arr(list) }
    private fun string(): String { require(take('"')); val out = StringBuilder(); while (index < source.length) { val c = source[index++]; if (c == '"') return out.toString(); if (c != '\\') { require(c >= ' '); out.append(c); continue }; require(index < source.length); when (val e = source[index++]) { '"','\\','/' -> out.append(e); 'b' -> out.append('\b'); 'f' -> out.append('\u000C'); 'n' -> out.append('\n'); 'r' -> out.append('\r'); 't' -> out.append('\t'); 'u' -> { require(index + 4 <= source.length); out.append(source.substring(index, index + 4).toInt(16).toChar()); index += 4 }; else -> error("invalid escape") } }; error("unterminated string") }
    private fun number(): Json.Num { val start = index; if (source[index] == '-') index++; while (index < source.length && source[index].isDigit()) index++; require(index > start); return Json.Num(source.substring(start, index).toLong()) }
    private fun <T: Json> literal(text: String, value: T): T { require(source.startsWith(text, index)); index += text.length; return value }
    private fun take(c: Char): Boolean { space(); if (index < source.length && source[index] == c) { index++; return true }; return false }
    private fun space() { while (index < source.length && source[index].isWhitespace()) index++ }
}

private fun Json.objectValue() = (this as Json.Obj).fields
private fun Map<String, Json>.requiredString(name: String) = (getValue(name) as Json.Str).value.also { require(it.isNotBlank()) }
private fun Map<String, Json>.nullableString(name: String) = when (val value = getValue(name)) { Json.Null -> null; is Json.Str -> value.value; else -> error("not string") }
private fun Map<String, Json>.requiredArray(name: String) = (getValue(name) as Json.Arr).values
private fun Map<String, Json>.requiredInt(name: String) = Math.toIntExact((getValue(name) as Json.Num).value)
private fun Map<String, Json>.requiredIntArray(name: String) = requiredArray(name).map { Math.toIntExact((it as Json.Num).value) }
private fun Map<String, Json>.requiredStringArray(name: String) = requiredArray(name).map { (it as Json.Str).value }
