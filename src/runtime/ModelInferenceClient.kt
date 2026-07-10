package parker.core.runtime

import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Model-Backed ReasoningProvider (Sprint 9). See
 * `docs/implementation/reviews/ModelReasoningProvider_Implementation_Review.md`
 * ("the Review") Section 4/6 and
 * `docs/implementation/MODEL_REASONING_PROVIDER_IMPLEMENTATION_PLAN.md`
 * ("the Plan") Section 6 Decisions D/E/J.
 *
 * The entire model-call seam. [ModelReasoningProvider] never knows or
 * cares what is behind it (Review Section 4).
 */
fun interface ModelInferenceClient {
    suspend fun infer(prompt: String): String
}

/**
 * Concrete, production, local-first [ModelInferenceClient]. Owns no
 * model process, no model file, no model weights (Review Section 10) --
 * it only calls an already-running, already-loaded inference endpoint.
 *
 * `endpointUrl`/`modelName` are required, with no default (Review Section
 * 6): a caller must state its own actual local endpoint explicitly.
 * `requestBodyFormatter`/`responseBodyParser` default to Ollama's
 * `/api/generate` convention ([defaultOllamaRequestBody],
 * [defaultOllamaResponseBody]) but are fully overridable for any other
 * local server's request/response shape, without modifying this class
 * (Review Section 6).
 *
 * Holds no mutable state -- the one [HttpClient] instance is itself
 * immutable/thread-safe, so concurrent [infer] calls do not interfere
 * with each other structurally (Plan Decision D). This class's own live
 * HTTP path is not exercised by the automated test suite (Review Risk 1).
 */
class LocalHttpModelInferenceClient(
    private val endpointUrl: String,
    private val modelName: String,
    private val requestBodyFormatter: (prompt: String, modelName: String) -> String = ::defaultOllamaRequestBody,
    private val responseBodyParser: (rawResponseBody: String) -> String = ::defaultOllamaResponseBody,
) : ModelInferenceClient {

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    override suspend fun infer(prompt: String): String {
        val requestBody = requestBodyFormatter(prompt, modelName)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(endpointUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = suspendCancellableCoroutine<HttpResponse<String>> { continuation ->
            val future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            future.whenComplete { result, error ->
                if (error != null) {
                    continuation.resumeWithException(error)
                } else {
                    continuation.resume(result)
                }
            }
            continuation.invokeOnCancellation { future.cancel(true) }
        }

        return responseBodyParser(response.body())
    }
}

/**
 * Minimal, hand-rolled JSON string escaping -- exactly the five
 * sequences required for well-formed JSON string content this Unit
 * produces (Plan Decision J). Not a general-purpose JSON escaper.
 */
private fun jsonEscape(value: String): String {
    val builder = StringBuilder(value.length)
    for (character in value) {
        when (character) {
            '\\' -> builder.append("\\\\")
            '"' -> builder.append("\\\"")
            '\n' -> builder.append("\\n")
            '\r' -> builder.append("\\r")
            '\t' -> builder.append("\\t")
            else -> builder.append(character)
        }
    }
    return builder.toString()
}

/**
 * Named, top-level, overridable default request formatter shaped like
 * Ollama's `/api/generate` convention (Review Section 6). Contains no
 * assumption specific to [LocalHttpModelInferenceClient]'s own class
 * body -- supplied alongside it, not hard-coded into it.
 */
fun defaultOllamaRequestBody(prompt: String, modelName: String): String {
    return "{\"model\":\"${jsonEscape(modelName)}\",\"prompt\":\"${jsonEscape(prompt)}\",\"stream\":false}"
}

/**
 * Named, top-level, overridable default response parser shaped like
 * Ollama's `/api/generate` convention (Review Section 6): extracts the
 * value of the top-level `"response"` string field via a direct scan for
 * `"response":"` followed by characters up to the next unescaped `"`,
 * un-escaping the same five sequences [jsonEscape] escapes -- not a
 * general-purpose JSON parser (Plan Decision J).
 *
 * @throws IllegalArgumentException if the `"response"` field is not
 *   found, or its string value is unterminated.
 */
fun defaultOllamaResponseBody(rawResponseBody: String): String {
    val key = "\"response\":\""
    val startIndex = rawResponseBody.indexOf(key)
    require(startIndex >= 0) {
        "defaultOllamaResponseBody: no \"response\" field found in: $rawResponseBody"
    }

    val builder = StringBuilder()
    var index = startIndex + key.length
    while (index < rawResponseBody.length) {
        val character = rawResponseBody[index]
        if (character == '\\' && index + 1 < rawResponseBody.length) {
            when (rawResponseBody[index + 1]) {
                '\\' -> builder.append('\\')
                '"' -> builder.append('"')
                'n' -> builder.append('\n')
                'r' -> builder.append('\r')
                't' -> builder.append('\t')
                else -> builder.append(rawResponseBody[index + 1])
            }
            index += 2
        } else if (character == '"') {
            return builder.toString()
        } else {
            builder.append(character)
            index += 1
        }
    }
    throw IllegalArgumentException(
        "defaultOllamaResponseBody: unterminated \"response\" string in: $rawResponseBody",
    )
}
