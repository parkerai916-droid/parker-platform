package parker.core.runtime

import java.io.IOException
import java.io.UncheckedIOException
import java.math.BigDecimal
import java.net.URI
import java.net.ConnectException
import java.net.UnknownHostException
import java.net.http.HttpClient
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import javax.net.ssl.SSLException
import kotlinx.coroutines.suspendCancellableCoroutine
import parker.composition.OpenAiApiCredential
import parker.composition.OpenAiExternalTranscriptionReadiness
import parker.composition.BYTE_EXACT_PROCESSING_PROFILE_ID
import parker.composition.DIRECT_AUTHORITATIVE_PROCESSING_PROFILE_ID
import parker.composition.FIDELITY_FIRST_TRANSCRIPTION_PROFILE_ID
import parker.core.interfaces.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val LITERAL_V2_PROFILE_ID = "openai-literal-page-transcription-v2"
internal const val LITERAL_V2_INSTRUCTION = "Perform literal transcription only. Reproduce only text visibly present in the submitted source. Preserve source spelling, punctuation, capitalization, page association, and visible reading order exactly as seen. Do not paraphrase, summarize, rewrite for clarity, correct grammar, normalize wording, infer missing text, or complete fragments. Do not insert likely names, dates, amounts, facts, legal propositions, or contextual completions. Do not use general knowledge, perform legal interpretation or analysis, or resolve factual conflicts. Where handwritten text is visibly distinct, preserve it as a separate line or block in visible reading order; do not merge it into or rewrite printed text. If any text is unreadable or uncertain, disclose that using the structured uncertainty, illegibility, qualification, or page-outcome fields and qualify or omit the text instead of completing it. Omission or qualification is preferable to invention. If page orientation is awkward, read the visible page as supplied and do not invent text to compensate. Output must correspond only to the submitted source. Return only the strict structured schema."
internal const val LITERAL_V2_INSTRUCTION_SHA256 = "c721e63b29e56f9242ee24dd8f13ddcab5d4468d3d17e9e3b9b1d66a68cb2000"
internal val LITERAL_V2_SCHEMA_SOURCE = """{"type":"object","additionalProperties":false,"required":["requested_pages","returned_pages","pages","warnings"],"properties":{"requested_pages":{"type":"array","items":{"type":"integer","minimum":1}},"returned_pages":{"type":"array","items":{"type":"integer","minimum":1}},"warnings":{"type":"array","items":{"type":"string"}},"pages":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["page_number","text","outcome","reason_classification","reason_detail","warnings","uncertainty_spans"],"properties":{"page_number":{"type":"integer","minimum":1},"text":{"type":["string","null"]},"outcome":{"type":"string","enum":["TRANSCRIBED","TRANSCRIBED_WITH_QUALIFICATIONS","ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT","FAILED","NOT_RETURNED"]},"reason_classification":{"type":["string","null"]},"reason_detail":{"type":["string","null"]},"warnings":{"type":"array","items":{"type":"string"}},"uncertainty_spans":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["start","end","kind","disclosure"],"properties":{"start":{"type":"integer","minimum":0},"end":{"type":"integer","minimum":1},"kind":{"type":"string","enum":["UNCERTAIN","ILLEGIBLE"]},"disclosure":{"type":"string"}}}}}}}}}"""
internal val LITERAL_V2_SCHEMA_CANONICAL = canonicalizeStructuredSchema(LITERAL_V2_SCHEMA_SOURCE)
internal val LITERAL_V2_SCHEMA_SHA256 = sha256Hex(LITERAL_V2_SCHEMA_CANONICAL.toByteArray(StandardCharsets.UTF_8))

internal const val FIDELITY_FIRST_INSTRUCTION = "Transcribe only content visibly present in the directly submitted authoritative source. Preserve exact spelling, punctuation, capitalization, page boundaries, block ordering, handwriting distinctions, tables, and layout-significant reading order. Never summarize, paraphrase, correct, normalize, infer, reconstruct, or complete uncertain content. Record every uncertainty in the structured uncertainty fields; omission with an explicit disclosure is preferable to invention. Return every represented page exactly once and every visible text block in deterministic reading order. Echo the supplied profile, request, and attempt identifiers exactly. Return only the strict structured schema."
internal val FIDELITY_FIRST_SCHEMA_SOURCE = """{"type":"object","additionalProperties":false,"required":["profile_id","request_id","attempt_id","document_outcome","completeness_state","requested_pages","returned_pages","pages","warnings"],"properties":{"profile_id":{"type":"string"},"request_id":{"type":"string"},"attempt_id":{"type":"string"},"document_outcome":{"type":"string","enum":["TRANSCRIBED","TRANSCRIBED_WITH_QUALIFICATIONS","FAILED"]},"completeness_state":{"type":"string","enum":["COMPLETE","INCOMPLETE","UNDETERMINED"]},"requested_pages":{"type":"array","items":{"type":"integer","minimum":1}},"returned_pages":{"type":"array","items":{"type":"integer","minimum":1}},"warnings":{"type":"array","items":{"type":"string"}},"pages":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["page_number","outcome","reason_classification","reason_detail","blocks","warnings","uncertainties"],"properties":{"page_number":{"type":"integer","minimum":1},"outcome":{"type":"string","enum":["TRANSCRIBED","TRANSCRIBED_WITH_QUALIFICATIONS","ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT","FAILED","NOT_RETURNED"]},"reason_classification":{"type":["string","null"]},"reason_detail":{"type":["string","null"]},"blocks":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["block_order","kind","text"],"properties":{"block_order":{"type":"integer","minimum":1},"kind":{"type":"string","enum":["PRINTED_TEXT","HANDWRITING","TABLE","HEADER","FOOTER","OTHER"]},"text":{"type":"string"}}}},"warnings":{"type":"array","items":{"type":"string"}},"uncertainties":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["category","block_order","observed_text","disclosure"],"properties":{"category":{"type":"string","enum":["ILLEGIBLE_TEXT","UNCERTAIN_CHARACTER","UNCERTAIN_WORD","UNCERTAIN_PHRASE","UNCERTAIN_ORDERING_LAYOUT","PARTIAL_VISIBILITY","HANDWRITING"]},"block_order":{"type":["integer","null"],"minimum":1},"observed_text":{"type":["string","null"]},"disclosure":{"type":"string"}}}}}}}}}"""
internal val FIDELITY_FIRST_SCHEMA_CANONICAL = canonicalizeStructuredSchema(FIDELITY_FIRST_SCHEMA_SOURCE)
internal const val FIDELITY_FIRST_INSTRUCTION_SHA256 = "38e4b87e3a429dac8ed5de91e5e2c94ad3d10cd739db186d41d09ed940b11b88"
internal const val FIDELITY_FIRST_SCHEMA_SHA256 = "7b46bdd6ce615592bb4e7cfee84f5ec5f6fde546d678e13bdad0555f829a3313"

internal data class OpenAiResponsesTransportRequest(
    val endpoint: URI,
    val timeoutMillis: Long,
    val body: String,
    val maximumResponseBytes: Long,
    val credential: OpenAiApiCredential,
    val lifecycleObserver: OpenAiTransportLifecycleObserver = OpenAiTransportLifecycleObserver.NONE,
) {
    override fun toString() = "OpenAiResponsesTransportRequest(endpoint=$endpoint, timeoutMillis=$timeoutMillis, body=<redacted>, maximumResponseBytes=$maximumResponseBytes, credential=$credential)"
}

internal data class OpenAiResponsesTransportResponse(val statusCode: Int, val body: ByteArray)

interface OpenAiTransportLifecycleObserver {
    fun providerAttemptStarting()
    fun providerResponseReceived()
    companion object { val NONE = object : OpenAiTransportLifecycleObserver {
        override fun providerAttemptStarting() = Unit
        override fun providerResponseReceived() = Unit
    } }
}

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
            request.lifecycleObserver.providerAttemptStarting()
            val future = client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
            future.whenComplete { response, error ->
                if (error != null) {
                    continuation.resumeWithException((error as? CompletionException)?.cause ?: error)
                } else {
                    try {
                        response.body().use { stream ->
                            val bytes = stream.readNBytes(Math.addExact(request.maximumResponseBytes, 1).toInt())
                            if (bytes.size.toLong() > request.maximumResponseBytes) throw OpenAiResponseTooLargeException()
                            request.lifecycleObserver.providerResponseReceived()
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
    private val responseFailureObserver: (OpenAiResponseFailureFingerprint) -> Unit = {},
    private val transportLifecycleObserver: OpenAiTransportLifecycleObserver = OpenAiTransportLifecycleObserver.NONE,
) : ExternalTranscriptionMechanism {
    private val profile = readiness.profile
    private val limits = readiness.effectiveLimits
    private val endpoint = URI.create(profile.allowedNetworkDestination).resolve(profile.apiProductPath)

    init {
        require(endpoint == URI.create("https://api.openai.com/v1/responses")) { "OpenAI Responses endpoint is not approved" }
        require(!profile.store) { "OpenAI adapter requires store=false" }
        require(profile.transcriptionProfileId in setOf(LITERAL_V2_PROFILE_ID, FIDELITY_FIRST_TRANSCRIPTION_PROFILE_ID))
        require(profile.instructionSha256 == instructionSha256) { "OpenAI instruction digest does not match the governed profile" }
        require(profile.structuredSchemaSha256 == schemaSha256) { "OpenAI schema digest does not match the governed profile" }
        require(profile.processingProfileIdentity == if (fidelityFirst) DIRECT_AUTHORITATIVE_PROCESSING_PROFILE_ID else BYTE_EXACT_PROCESSING_PROFILE_ID)
        if (fidelityFirst) {
            require(profile.modelSelectionRule == "gpt-5.6-sol")
            require(profile.reasoningEffort == "none" && profile.pdfDetail == "high" && profile.imageDetail == "original")
        }
    }

    override suspend fun transcribe(request: ExternalTranscriptionRequest): ExternalTranscriptionMechanismOutcome {
        val sourceLimit = if (request.mediaType == "application/pdf") limits.maximumPdfBytes else limits.maximumImageBytes
        if (request.content.size.toLong() > sourceLimit) return failure("INPUT_TOO_LARGE")
        val encodedLength = 4L * ((request.content.size.toLong() + 2L) / 3L)
        if (encodedLength + REQUEST_OVERHEAD_ALLOWANCE > maximumEncodedRequestBytes) return failure("ENCODED_INPUT_TOO_LARGE")

        val body = buildRequestBody(request)
        val response = try {
            transport.execute(OpenAiResponsesTransportRequest(endpoint, limits.timeoutMillis, body, limits.maximumOutputBytes, credential, transportLifecycleObserver))
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
        } catch (e: Exception) {
            runCatching {
                responseFailureObserver(fingerprintOpenAiResponseFailure(response, e))
            }
            failure("MALFORMED_PROVIDER_RESPONSE")
        }
    }

    private fun buildRequestBody(request: ExternalTranscriptionRequest): String {
        val base64 = Base64.getEncoder().encodeToString(request.content)
        val binding = request.executionBinding
        if (fidelityFirst) require(binding != null && binding.profileId == profile.transcriptionProfileId)
        val mediaPart = if (request.mediaType == "application/pdf") {
            "{\"type\":\"input_file\",\"filename\":\"source.pdf\",\"detail\":\"${jsonEscape(profile.pdfDetail)}\",\"file_data\":\"data:application/pdf;base64,$base64\"}"
        } else {
            "{\"type\":\"input_image\",\"detail\":\"${jsonEscape(profile.imageDetail)}\",\"image_url\":\"data:${jsonEscape(request.mediaType)};base64,$base64\"}"
        }
        val identityText = if (binding == null) "Maximum page count: ${request.maximumPageCount}." else
            "Profile ID: ${binding.profileId}. Request ID: ${binding.requestId}. Attempt ID: ${binding.attemptId}. Maximum page count: ${request.maximumPageCount}. Expected page count: ${request.expectedPageCount?.toString() ?: "not independently established"}."
        return "{" +
            "\"model\":\"${jsonEscape(profile.modelSelectionRule)}\"," +
            "\"store\":false,\"stream\":false," +
            (if (fidelityFirst) "\"reasoning\":{\"effort\":\"none\"}," else "") +
            "\"instructions\":\"${jsonEscape(instruction)}\"," +
            "\"input\":[{\"role\":\"user\",\"content\":[" +
            "{\"type\":\"input_text\",\"text\":\"Transcribe this source under the developer instruction. ${jsonEscape(identityText)}\"}," +
            mediaPart + "]}]," +
            "\"text\":{\"format\":{\"type\":\"json_schema\",\"name\":\"parker_page_transcription\",\"strict\":true,\"schema\":" + schemaCanonical + "}}}"
    }

    private fun parseResponse(request: ExternalTranscriptionRequest, raw: String): OcrStructuredTranscriptionCandidate {
        val envelope = responseParseStage("ENVELOPE_JSON") { Json.parse(raw).objectValue() }
        val responseId = responseParseStage("RESPONSE_ID") {
            envelope.requiredString("id").also { require(ID_PATTERN.matches(it)) }
        }
        val model = responseParseStage("MODEL") {
            envelope.requiredString("model").also { require(it.isNotBlank() && !it.equals("unknown", true)) }
        }
        val outputText = responseParseStage("OUTPUT_TEXT") {
            envelope.requiredArray("output").flatMap { it.objectValue().requiredArray("content") }
                .map { it.objectValue() }.single { it.requiredString("type") == "output_text" }.requiredString("text")
        }
        val result = responseParseStage("STRUCTURED_PAYLOAD") { Json.parse(outputText).objectValue() }
        if (fidelityFirst) return parseFidelityFirst(request, responseId, model, result)
        val requested = responseParseStage("PAGE_ACCOUNTING") {
            OcrPageScope(result.requiredIntArray("requested_pages"))
        }
        val returned = responseParseStage("PAGE_ACCOUNTING") {
            OcrPageScope(result.requiredIntArray("returned_pages"))
        }
        responseParseStage("PAGE_ACCOUNTING") { require(requested.pageNumbers.size <= request.maximumPageCount) }
        val pages = responseParseStage("PAGES") { result.requiredArray("pages").map { pageValue ->
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
        } }
        return responseParseStage("CANDIDATE") { OcrStructuredTranscriptionCandidate(
            requested, requested, returned, pages, TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
            OcrRecognitionIdentity("openai-responses", TRANSCRIPTION_PROFILE_ID, ADAPTER_VERSION),
            OcrProviderProvenance(
                "OpenAI", "openai-responses-adapter", ADAPTER_VERSION, TRANSCRIPTION_PROFILE_ID,
                model, OcrModelSnapshot.NotExposed, responseId,
                OcrTranscriptionConfiguration.DigestedConfiguration(
                    TRANSCRIPTION_PROFILE_ID,
                    OcrSha256Digest(TRANSCRIPTION_INSTRUCTION_SHA256),
                    OcrSha256Digest(STRUCTURED_SCHEMA_SHA256),
                ),
            ),
            request.processingProvenance, Instant.now(), result.requiredStringArray("warnings"),
        ) }
    }

    private fun parseFidelityFirst(
        request: ExternalTranscriptionRequest,
        responseId: String,
        model: String,
        result: Map<String, Json>,
    ): OcrStructuredTranscriptionCandidate {
        val binding = requireNotNull(request.executionBinding)
        responseParseStage("EXECUTION_BINDING") {
            require(result.requiredString("profile_id") == binding.profileId)
            require(result.requiredString("request_id") == binding.requestId)
            require(result.requiredString("attempt_id") == binding.attemptId)
        }
        responseParseStage("DOCUMENT_OUTCOME") {
            require(result.requiredString("document_outcome") in setOf("TRANSCRIBED", "TRANSCRIBED_WITH_QUALIFICATIONS", "FAILED"))
            require(result.requiredString("completeness_state") in setOf("COMPLETE", "INCOMPLETE", "UNDETERMINED"))
        }
        val requestedNumbers = result.requiredIntArray("requested_pages")
        val returnedNumbers = result.requiredIntArray("returned_pages")
        responseParseStage("PAGE_ACCOUNTING") {
            require(requestedNumbers == requestedNumbers.sorted() && requestedNumbers.distinct() == requestedNumbers)
            require(returnedNumbers == returnedNumbers.sorted() && returnedNumbers.distinct() == returnedNumbers)
            require(requestedNumbers.size <= request.maximumPageCount)
            request.expectedPageCount?.let { require(requestedNumbers == (1..it).toList()) }
        }
        val pages = responseParseStage("PAGES") { result.requiredArray("pages").map { value ->
            val page = value.objectValue()
            val pageNumber = page.requiredInt("page_number")
            val blocks = page.requiredArray("blocks").map { it.objectValue() }
            val orders = blocks.map { it.requiredInt("block_order") }
            require(orders == (1..orders.size).toList())
            val texts = blocks.map { it.requiredStringAllowEmpty("text") }
            val starts = mutableListOf<Int>()
            var offset = 0
            texts.forEachIndexed { index, text -> starts += offset; offset += text.length + if (index < texts.lastIndex) 1 else 0 }
            val joined = texts.joinToString("\n").takeIf { it.isNotBlank() }
            val spans = page.requiredArray("uncertainties").map { uncertaintyValue ->
                val uncertainty = uncertaintyValue.objectValue()
                val category = uncertainty.requiredString("category")
                val blockOrder = uncertainty.nullableInt("block_order")
                val observed = uncertainty.nullableString("observed_text")
                val start = blockOrder?.let { starts.getOrNull(it - 1) } ?: 0
                val blockText = blockOrder?.let { texts.getOrNull(it - 1) }.orEmpty()
                val relative = observed?.takeIf { it.isNotEmpty() }?.let { blockText.indexOf(it).takeIf { index -> index >= 0 } } ?: 0
                val absoluteStart = (start + relative).coerceAtMost((joined?.length ?: 1) - 1)
                val length = observed?.length?.takeIf { it > 0 } ?: blockText.length.takeIf { it > 0 } ?: 1
                OcrUncertaintySpan(pageNumber, absoluteStart, (absoluteStart + length).coerceAtMost(joined?.length ?: 1).coerceAtLeast(absoluteStart + 1),
                    if (category == "ILLEGIBLE_TEXT") OcrUncertaintyKind.ILLEGIBLE else OcrUncertaintyKind.UNCERTAIN,
                    "$category: ${uncertainty.requiredString("disclosure")}")
            }
            val classification = page.nullableString("reason_classification")
            OcrStructuredPageCandidate(pageNumber, joined, OcrPageOutcomeKind.valueOf(page.requiredString("outcome")),
                classification?.let { OcrPageOutcomeReason(it, page.nullableString("reason_detail")) },
                page.requiredStringArray("warnings"), spans)
        } }
        responseParseStage("PAGE_ACCOUNTING") {
            val pageNumbers = pages.map { it.pageNumber }
            require(pageNumbers == pageNumbers.sorted() && pageNumbers.distinct() == pageNumbers)
            require(pageNumbers == returnedNumbers)
        }
        return responseParseStage("CANDIDATE") { OcrStructuredTranscriptionCandidate(
            OcrPageScope(requestedNumbers), OcrPageScope(requestedNumbers), OcrPageScope(returnedNumbers), pages,
            TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
            OcrRecognitionIdentity("openai-responses", profile.transcriptionProfileId, adapterVersion),
            OcrProviderProvenance("OpenAI", "openai-responses-adapter", adapterVersion, profile.transcriptionProfileId,
                model, OcrModelSnapshot.NotExposed, responseId,
                OcrTranscriptionConfiguration.DigestedConfiguration(profile.transcriptionProfileId,
                    OcrSha256Digest(instructionSha256), OcrSha256Digest(schemaSha256))),
            request.processingProvenance, Instant.now(), result.requiredStringArray("warnings"),
        ) }
    }

    private fun failure(code: String) = ExternalTranscriptionMechanismOutcome.Failure(code)

    private companion object {
        const val ADAPTER_VERSION = "1.1.0"
        const val TRANSCRIPTION_PROFILE_ID = LITERAL_V2_PROFILE_ID
        const val REQUEST_OVERHEAD_ALLOWANCE = 64L * 1024L
        const val MAXIMUM_ENCODED_REQUEST_BYTES = 96L * 1024L * 1024L
        val ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,1024}$")
        const val TRANSCRIPTION_INSTRUCTION = LITERAL_V2_INSTRUCTION
        const val TRANSCRIPTION_INSTRUCTION_SHA256 = LITERAL_V2_INSTRUCTION_SHA256
        val STRUCTURED_SCHEMA_CANONICAL = LITERAL_V2_SCHEMA_CANONICAL
        val STRUCTURED_SCHEMA_SHA256 = LITERAL_V2_SCHEMA_SHA256
    }

    private val fidelityFirst get() = profile.transcriptionProfileId == FIDELITY_FIRST_TRANSCRIPTION_PROFILE_ID
    private val instruction get() = if (fidelityFirst) FIDELITY_FIRST_INSTRUCTION else TRANSCRIPTION_INSTRUCTION
    private val instructionSha256 get() = if (fidelityFirst) FIDELITY_FIRST_INSTRUCTION_SHA256 else TRANSCRIPTION_INSTRUCTION_SHA256
    private val schemaCanonical get() = if (fidelityFirst) FIDELITY_FIRST_SCHEMA_CANONICAL else STRUCTURED_SCHEMA_CANONICAL
    private val schemaSha256 get() = if (fidelityFirst) FIDELITY_FIRST_SCHEMA_SHA256 else STRUCTURED_SCHEMA_SHA256
    private val adapterVersion get() = if (fidelityFirst) "2.0.0" else ADAPTER_VERSION
}

internal class OpenAiResponseParseException(
    val stage: String,
    cause: Exception,
) : RuntimeException(null, cause, false, false)

private inline fun <T> responseParseStage(stage: String, block: () -> T): T = try {
    block()
} catch (e: OpenAiResponseParseException) {
    throw e
} catch (e: Exception) {
    throw OpenAiResponseParseException(stage, e)
}

/** Detached-acceptance-only structural projection of a 2xx response; values never include content. */
internal data class OpenAiResponseFailureFingerprint(
    val httpStatus: Int,
    val responseJsonParseable: Boolean,
    val responseIdPresent: Boolean,
    val modelPresent: Boolean,
    val status: String,
    val outputItemCount: Int,
    val outputItemTypes: String,
    val messageContentCount: Int,
    val contentTypes: String,
    val outputTextPresent: Boolean,
    val refusalPresent: Boolean,
    val incompleteReasonPresent: Boolean,
    val structuredPayloadPresent: Boolean,
    val parkerParseStage: String,
    val category: String = "MALFORMED_PROVIDER_RESPONSE",
) {
    fun render(): String = listOf(
        "HTTP_STATUS=$httpStatus",
        "RESPONSE_JSON_PARSEABLE=$responseJsonParseable",
        "RESPONSE_ID_PRESENT=$responseIdPresent",
        "MODEL_PRESENT=$modelPresent",
        "STATUS=$status",
        "OUTPUT_ITEM_COUNT=$outputItemCount",
        "OUTPUT_ITEM_TYPES=$outputItemTypes",
        "MESSAGE_CONTENT_COUNT=$messageContentCount",
        "CONTENT_TYPES=$contentTypes",
        "OUTPUT_TEXT_PRESENT=$outputTextPresent",
        "REFUSAL_PRESENT=$refusalPresent",
        "INCOMPLETE_REASON_PRESENT=$incompleteReasonPresent",
        "STRUCTURED_PAYLOAD_PRESENT=$structuredPayloadPresent",
        "PARKER_PARSE_STAGE=$parkerParseStage",
        "CATEGORY=$category",
    ).joinToString(" ")
}

internal fun fingerprintOpenAiResponseFailure(
    response: OpenAiResponsesTransportResponse,
    error: Exception,
): OpenAiResponseFailureFingerprint {
    val envelope = runCatching { Json.parse(response.body.toString(Charsets.UTF_8)) as? Json.Obj }.getOrNull()
    val fields = envelope?.fields
    val output = (fields?.get("output") as? Json.Arr)?.values.orEmpty()
    val outputObjects = output.mapNotNull { it as? Json.Obj }
    val messageObjects = outputObjects.filter { (it.fields["type"] as? Json.Str)?.value == "message" }
    val content = messageObjects.flatMap { ((it.fields["content"] as? Json.Arr)?.values).orEmpty() }
    val contentObjects = content.mapNotNull { it as? Json.Obj }
    val outputTexts = contentObjects.filter { (it.fields["type"] as? Json.Str)?.value == "output_text" }
    val structuredPayloadPresent = outputTexts.any { item ->
        val text = (item.fields["text"] as? Json.Str)?.value ?: return@any false
        runCatching { Json.parse(text) is Json.Obj }.getOrDefault(false)
    }
    return OpenAiResponseFailureFingerprint(
        httpStatus = response.statusCode,
        responseJsonParseable = envelope != null,
        responseIdPresent = (fields?.get("id") as? Json.Str)?.value?.isNotBlank() == true,
        modelPresent = (fields?.get("model") as? Json.Str)?.value?.isNotBlank() == true,
        status = boundedResponseStatus((fields?.get("status") as? Json.Str)?.value),
        outputItemCount = output.size.coerceAtMost(100),
        outputItemTypes = boundedTypeSet(outputObjects.map { (it.fields["type"] as? Json.Str)?.value }),
        messageContentCount = content.size.coerceAtMost(100),
        contentTypes = boundedTypeSet(contentObjects.map { (it.fields["type"] as? Json.Str)?.value }),
        outputTextPresent = outputTexts.isNotEmpty(),
        refusalPresent = contentObjects.any { (it.fields["type"] as? Json.Str)?.value == "refusal" },
        incompleteReasonPresent = (fields?.get("incomplete_details") as? Json.Obj)?.fields?.get("reason") is Json.Str,
        structuredPayloadPresent = structuredPayloadPresent,
        parkerParseStage = (error as? OpenAiResponseParseException)?.stage ?: "UNKNOWN",
    )
}

private fun boundedResponseStatus(value: String?): String = when (value) {
    "completed" -> "COMPLETED"
    "failed" -> "FAILED"
    "incomplete" -> "INCOMPLETE"
    "in_progress" -> "IN_PROGRESS"
    "queued" -> "QUEUED"
    null -> "MISSING"
    else -> "OTHER"
}

private fun boundedTypeSet(values: List<String?>): String {
    val documented = setOf(
        "message", "output_text", "refusal", "reasoning", "function_call", "web_search_call",
        "file_search_call", "computer_call", "code_interpreter_call", "image_generation_call",
        "local_shell_call", "mcp_call", "mcp_list_tools", "mcp_approval_request", "custom_tool_call",
    )
    val bounded = values.map { value -> value?.takeIf { it in documented } ?: "OTHER" }.toSortedSet()
    return bounded.takeIf { it.isNotEmpty() }?.joinToString(",") ?: "NONE"
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

internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** RFC 8785 canonical form for Parker's governed schema subset (objects, arrays, strings, booleans, null, and integers). */
internal fun canonicalizeStructuredSchema(source: String): String = canonicalJson(Json.parse(source))

private fun canonicalJson(value: Json): String = when (value) {
    is Json.Obj -> value.fields.keys.sorted().joinToString(prefix = "{", postfix = "}") { key ->
        "\"${canonicalJsonEscape(key)}\":" + canonicalJson(value.fields.getValue(key))
    }
    is Json.Arr -> value.values.joinToString(prefix = "[", postfix = "]") { canonicalJson(it) }
    is Json.Str -> "\"${canonicalJsonEscape(value.value)}\""
    is Json.Num -> value.value.stripTrailingZeros().let {
        require(it.scale() <= 0) { "governed transcription schema numbers must be integers" }
        if (it.signum() == 0) "0" else it.toPlainString()
    }
    Json.Null -> "null"
    is Json.Bool -> value.value.toString()
}

private fun canonicalJsonEscape(value: String): String = buildString(value.length) {
    value.forEach { c -> when (c) {
        '\b' -> append("\\b")
        '\t' -> append("\\t")
        '\n' -> append("\\n")
        '\u000C' -> append("\\f")
        '\r' -> append("\\r")
        '"' -> append("\\\"")
        '\\' -> append("\\\\")
        else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
    } }
}

private sealed interface Json {
    data class Obj(val fields: Map<String, Json>) : Json
    data class Arr(val values: List<Json>) : Json
    data class Str(val value: String) : Json
    data class Num(val value: BigDecimal) : Json
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
    private fun number(): Json.Num {
        val start = index
        if (source[index] == '-') index++
        require(index < source.length)
        if (source[index] == '0') {
            index++
        } else {
            require(source[index] in '1'..'9')
            while (index < source.length && source[index].isDigit()) index++
        }
        if (index < source.length && source[index] == '.') {
            index++
            val fractionStart = index
            while (index < source.length && source[index].isDigit()) index++
            require(index > fractionStart)
        }
        if (index < source.length && (source[index] == 'e' || source[index] == 'E')) {
            index++
            if (index < source.length && (source[index] == '+' || source[index] == '-')) index++
            val exponentStart = index
            while (index < source.length && source[index].isDigit()) index++
            require(index > exponentStart)
        }
        return Json.Num(source.substring(start, index).toBigDecimal())
    }
    private fun <T: Json> literal(text: String, value: T): T { require(source.startsWith(text, index)); index += text.length; return value }
    private fun take(c: Char): Boolean { space(); if (index < source.length && source[index] == c) { index++; return true }; return false }
    private fun space() { while (index < source.length && source[index].isWhitespace()) index++ }
}

private fun Json.objectValue() = (this as Json.Obj).fields
private fun Map<String, Json>.requiredString(name: String) = (getValue(name) as Json.Str).value.also { require(it.isNotBlank()) }
private fun Map<String, Json>.requiredStringAllowEmpty(name: String) = (getValue(name) as Json.Str).value
private fun Map<String, Json>.nullableString(name: String) = when (val value = getValue(name)) { Json.Null -> null; is Json.Str -> value.value; else -> error("not string") }
private fun Map<String, Json>.nullableInt(name: String) = when (val value = getValue(name)) { Json.Null -> null; is Json.Num -> value.value.intValueExact(); else -> error("not integer") }
private fun Map<String, Json>.requiredArray(name: String) = (getValue(name) as Json.Arr).values
private fun Map<String, Json>.requiredInt(name: String) = (getValue(name) as Json.Num).value.intValueExact()
private fun Map<String, Json>.requiredIntArray(name: String) = requiredArray(name).map { (it as Json.Num).value.intValueExact() }
private fun Map<String, Json>.requiredStringArray(name: String) = requiredArray(name).map { (it as Json.Str).value }
