package parker.core.interfaces

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Pure wire codec for the v1 forced-command boundary.
 *
 * A frame is a four-byte unsigned big-endian metadata length, the exact UTF-8
 * metadata bytes, and exactly source.sizeBytes raw source bytes.  The source
 * is copied in bounded chunks; this class never hashes, interprets, or stores
 * the source stream as a whole.
 */
object HermesProcessingServiceV1Framing {
    const val METADATA_LENGTH_BYTES: Int = 4
    const val SOURCE_COPY_BUFFER_BYTES: Int = 16 * 1024

    fun encodeMetadata(request: HermesProcessingServiceV1Request): ByteArray =
        HermesV1CanonicalJson.request(request).toByteArray(StandardCharsets.UTF_8).also {
            if (it.size.toLong() > HermesProcessingServiceV1Limits.MAX_METADATA_ENVELOPE_BYTES) {
                throw framingFailure(HermesV1FailureDetailCode.ENVELOPE_TOO_LARGE, "metadata envelope exceeds the v1 limit")
            }
        }

    /** Reads metadata, copies exactly source.sizeBytes to [sourceSink], then rejects trailing data. */
    fun readRequestFrame(
        input: InputStream,
        sourceSink: OutputStream,
        onSourceChunk: (ByteArray, Int, Int) -> Unit = { _, _, _ -> },
    ): HermesProcessingServiceV1Request {
        val lengthBytes = readExactly(input, METADATA_LENGTH_BYTES, HermesV1FailureDetailCode.SHORT_METADATA_LENGTH_READ)
        val metadataLength = ByteBuffer.wrap(lengthBytes).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xffffffffL
        if (metadataLength == 0L) {
            throw framingFailure(HermesV1FailureDetailCode.MALFORMED_FRAME, "metadata length must be positive")
        }
        if (metadataLength > HermesProcessingServiceV1Limits.MAX_METADATA_ENVELOPE_BYTES) {
            throw framingFailure(HermesV1FailureDetailCode.ENVELOPE_TOO_LARGE, "metadata envelope exceeds the v1 limit")
        }

        val metadata = readExactly(input, metadataLength.toInt(), HermesV1FailureDetailCode.SHORT_METADATA_READ)
        val request = decodeMetadata(metadata)
        copyExactSource(input, sourceSink, request.source.sizeBytes.value, onSourceChunk)
        if (input.read() != -1) {
            throw framingFailure(HermesV1FailureDetailCode.TRAILING_DATA, "bytes remain after the declared source")
        }
        return request
    }

    /** Writes a complete request frame and verifies that the source stream has the declared size. */
    fun writeRequestFrame(
        output: OutputStream,
        request: HermesProcessingServiceV1Request,
        source: InputStream,
    ) {
        val metadata = encodeMetadata(request)
        output.write(ByteBuffer.allocate(METADATA_LENGTH_BYTES).order(ByteOrder.BIG_ENDIAN).putInt(metadata.size).array())
        output.write(metadata)
        copyExactSource(source, output, request.source.sizeBytes.value)
        if (source.read() != -1) {
            throw framingFailure(HermesV1FailureDetailCode.SOURCE_SIZE_MISMATCH, "source stream exceeds the declared size")
        }
    }

    fun decodeMetadata(metadata: ByteArray): HermesProcessingServiceV1Request {
        if (metadata.isEmpty() || metadata.size.toLong() > HermesProcessingServiceV1Limits.MAX_METADATA_ENVELOPE_BYTES) {
            throw framingFailure(HermesV1FailureDetailCode.ENVELOPE_TOO_LARGE, "metadata envelope is outside the v1 bounds")
        }
        val text = decodeUtf8(metadata)
        val value = try {
            StrictJsonParser(text).parse()
        } catch (error: FramingException) {
            throw error
        } catch (_: RuntimeException) {
            throw framingFailure(HermesV1FailureDetailCode.MALFORMED_JSON, "metadata is not valid JSON")
        }
        val root = try {
            value.asObjectOrFail("metadata must be a JSON object")
        } catch (error: FramingException) {
            throw error
        } catch (_: RuntimeException) {
            throw framingFailure(HermesV1FailureDetailCode.INVALID_FIELD, "metadata must be a JSON object")
        }
        val allowed = setOf("protocolVersion", "requestId", "jobId", "occurrenceId", "batchId", "source", "requestedMethods")
        rejectUnknown(root, allowed)
        val request = try {
            val protocolVersion = string(root, "protocolVersion")
            HermesProcessingServiceV1Request(
                protocolVersion = try { HermesV1ProtocolVersion(protocolVersion) } catch (_: IllegalArgumentException) {
                    throw framingFailure(HermesV1FailureDetailCode.UNSUPPORTED_PROTOCOL_VERSION, "unsupported protocol version")
                },
                requestId = HermesV1RequestId(string(root, "requestId")),
                jobId = HermesV1JobId(string(root, "jobId")),
                occurrenceId = HermesV1OccurrenceId(string(root, "occurrenceId")),
                batchId = HermesV1BatchId(string(root, "batchId")),
                source = decodeSource(root.requiredObject("source")),
                requestedMethods = root.optionalArray("requestedMethods")?.map { item ->
                    try {
                        HermesV1ProcessingMethod.fromWireValue(item.asStringOrFail("requestedMethods entries must be strings"))
                    } catch (_: IllegalArgumentException) {
                        throw framingFailure(HermesV1FailureDetailCode.UNSUPPORTED_METHOD, "metadata names an unavailable processing method")
                    }
                } ?: emptyList(),
            )
        } catch (error: FramingException) {
            throw error
        } catch (_: IllegalArgumentException) {
            throw framingFailure(HermesV1FailureDetailCode.INVALID_FIELD, "metadata contains an invalid field")
        }
        return request
    }

    /** Deterministically frames a response JSON body with the same length prefix. */
    fun frameResponseBody(canonicalJsonUtf8: ByteArray): ByteArray {
        if (canonicalJsonUtf8.size.toLong() > HermesProcessingServiceV1Limits.MAX_INLINE_RESPONSE_BYTES) {
            throw framingFailure(HermesV1FailureDetailCode.RESPONSE_TOO_LARGE, "response exceeds the inline v1 limit")
        }
        val responseText = decodeUtf8(canonicalJsonUtf8)
        try {
            StrictJsonParser(responseText).parse().asObjectOrFail("response must be a JSON object")
        } catch (error: FramingException) {
            throw error
        } catch (_: RuntimeException) {
            throw framingFailure(HermesV1FailureDetailCode.MALFORMED_JSON, "response is not valid JSON")
        }
        val prefix = ByteBuffer.allocate(METADATA_LENGTH_BYTES).order(ByteOrder.BIG_ENDIAN).putInt(canonicalJsonUtf8.size).array()
        return prefix + canonicalJsonUtf8
    }

    fun encodeResponseFrame(response: HermesProcessingServiceV1Response): ByteArray =
        frameResponseBody(HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(response))

    private fun decodeSource(source: JsonObject): HermesProcessingServiceV1Source {
        rejectUnknown(source, setOf("reference", "sha256", "sizeBytes", "originalFilename", "mediaType"))
        return try {
            val sourceSize = source.requiredNumber("sizeBytes")
            if (sourceSize < 0L) throw framingFailure(HermesV1FailureDetailCode.INVALID_SOURCE_LENGTH, "source size must not be negative")
            if (sourceSize > HermesProcessingServiceV1Limits.MAX_SOURCE_BYTES) {
                throw framingFailure(HermesV1FailureDetailCode.SOURCE_TOO_LARGE, "source size exceeds the v1 limit")
            }
            HermesProcessingServiceV1Source(
                reference = HermesV1SourceReference(string(source, "reference")),
                sourceSha256 = HermesV1Sha256(string(source, "sha256")),
                sizeBytes = HermesV1SourceSize(sourceSize),
                originalFilename = HermesV1OriginalFilename(string(source, "originalFilename")),
                mediaType = HermesV1MediaType(string(source, "mediaType")),
            )
        } catch (error: FramingException) {
            throw error
        } catch (_: IllegalArgumentException) {
            throw framingFailure(HermesV1FailureDetailCode.INVALID_FIELD, "source contains an invalid field")
        }
    }

    private fun copyExactSource(
        input: InputStream,
        output: OutputStream,
        size: Long,
        onSourceChunk: (ByteArray, Int, Int) -> Unit = { _, _, _ -> },
    ) {
        if (size < 0L || size > HermesProcessingServiceV1Limits.MAX_SOURCE_BYTES) {
            throw framingFailure(HermesV1FailureDetailCode.INVALID_SOURCE_LENGTH, "source length is outside the v1 bounds")
        }
        val buffer = ByteArray(SOURCE_COPY_BUFFER_BYTES)
        var remaining = size
        while (remaining > 0L) {
            val wanted = minOf(remaining, buffer.size.toLong()).toInt()
            val read = input.read(buffer, 0, wanted)
            if (read < 0) throw framingFailure(HermesV1FailureDetailCode.SHORT_SOURCE_READ, "source ended before its declared length")
            if (read == 0) continue
            output.write(buffer, 0, read)
            onSourceChunk(buffer, 0, read)
            remaining -= read.toLong()
        }
    }

    private fun readExactly(input: InputStream, size: Int, code: HermesV1FailureDetailCode): ByteArray {
        val result = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = input.read(result, offset, size - offset)
            if (read < 0) throw framingFailure(code, "frame ended before the declared length")
            if (read == 0) continue
            offset += read
        }
        return result
    }

    private fun decodeUtf8(bytes: ByteArray): String = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        throw framingFailure(HermesV1FailureDetailCode.INVALID_UTF8, "metadata is not valid UTF-8")
    }

    private fun string(objectValue: JsonObject, name: String): String =
        objectValue.required(name).asStringOrFail("$name must be a string")

    private fun rejectUnknown(objectValue: JsonObject, allowed: Set<String>) {
        objectValue.keys.firstOrNull { it !in allowed }?.let {
            throw framingFailure(HermesV1FailureDetailCode.UNKNOWN_FIELD, "unknown metadata field: $it")
        }
    }

}

private fun framingFailure(code: HermesV1FailureDetailCode, detail: String): FramingException =
    FramingException(HermesV1Failure(code.category, code, retryable = code.category == HermesV1FailureCategory.TRANSPORT, detail))

class FramingException(val failure: HermesV1Failure) : IllegalArgumentException(
    "${failure.category.wireValue}/${failure.detailCode.wireValue}: ${failure.detail}",
)

private object HermesV1CanonicalJson {
    fun request(request: HermesProcessingServiceV1Request): String = buildString {
        append('{')
        appendQuoted("protocolVersion")
        append(':')
        appendQuoted(request.protocolVersion.value)
        field("requestId", request.requestId.value)
        field("jobId", request.jobId.value)
        field("occurrenceId", request.occurrenceId.value)
        field("batchId", request.batchId.value)
        append(",\"source\":{")
        appendQuoted("reference")
        append(':')
        appendQuoted(request.source.reference.value)
        field("sha256", request.source.sourceSha256.value)
        append(",\"sizeBytes\":").append(request.source.sizeBytes.value)
        field("originalFilename", request.source.originalFilename.value)
        field("mediaType", request.source.mediaType.value)
        append('}')
        if (request.requestedMethods.isNotEmpty()) {
            append(",\"requestedMethods\":[")
            request.requestedMethods.sortedBy { it.wireValue }.forEachIndexed { index, method ->
                if (index > 0) append(',')
                appendQuoted(method.wireValue)
            }
            append(']')
        }
        append('}')
    }

    fun response(response: HermesProcessingServiceV1Response): String =
        HermesProcessingServiceV1ResponseSerializer.canonicalJson(response)

    private fun StringBuilder.field(name: String, value: String) {
        append(',')
        appendQuoted(name)
        append(':')
        appendQuoted(value)
    }

    private fun StringBuilder.appendQuoted(value: String) {
        append('"')
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                in '\u0000'..'\u001F' -> append("\\u").append(character.code.toString(16).padStart(4, '0'))
                else -> append(character)
            }
        }
        append('"')
    }
}

private sealed interface JsonValue {
    fun asStringOrFail(message: String): String = throw JsonSyntaxException(message)
    fun asObjectOrFail(message: String): JsonObject = throw JsonSyntaxException(message)
}

private class JsonObject(val fields: LinkedHashMap<String, JsonValue>) : JsonValue {
    val keys: Set<String> get() = fields.keys
    fun required(name: String): JsonValue = fields[name] ?: throw JsonSyntaxException("missing required field: $name")
    fun requiredObject(name: String): JsonObject = required(name).asObjectOrFail("$name must be an object")
    fun requiredNumber(name: String): Long = (required(name) as? JsonNumber)?.value
        ?: throw JsonSyntaxException("$name must be an integer")
    fun optionalArray(name: String): List<JsonValue>? = fields[name]?.let { (it as? JsonArray)?.values ?: throw JsonSyntaxException("$name must be an array") }
    override fun asObjectOrFail(message: String): JsonObject = this
}

private data class JsonString(val value: String) : JsonValue {
    override fun asStringOrFail(message: String): String = value
}

private data class JsonNumber(val value: Long) : JsonValue
private data class JsonArray(val values: List<JsonValue>) : JsonValue
private data object JsonBoolean : JsonValue
private data object JsonNull : JsonValue
private class JsonSyntaxException(message: String) : IllegalArgumentException(message)

private class StrictJsonParser(private val text: String) {
    private var index = 0
    private var depth = 0

    fun parse(): JsonValue {
        val value = value()
        whitespace()
        if (index != text.length) throw JsonSyntaxException("trailing JSON data")
        return value
    }

    private fun value(): JsonValue {
        whitespace()
        if (++depth > 64) throw JsonSyntaxException("JSON nesting exceeds the bound")
        val result = when {
            consume('{') -> objectValue()
            consume('[') -> arrayValue()
            consume('"') -> JsonString(stringValue())
            text.startsWith("true", index) -> { index += 4; JsonBoolean }
            text.startsWith("false", index) -> { index += 5; JsonBoolean }
            text.startsWith("null", index) -> { index += 4; JsonNull }
            peek() == '-' || peek()?.isDigit() == true -> number()
            else -> throw JsonSyntaxException("invalid JSON value")
        }
        depth--
        return result
    }

    private fun objectValue(): JsonObject {
        val fields = LinkedHashMap<String, JsonValue>()
        whitespace()
        if (consume('}')) return JsonObject(fields)
        while (true) {
            whitespace()
            if (!consume('"')) throw JsonSyntaxException("object key must be a string")
            val key = stringValue()
            if (fields.containsKey(key)) throw framingFailure(HermesV1FailureDetailCode.DUPLICATE_FIELD, "duplicate JSON field: $key")
            whitespace()
            if (!consume(':')) throw JsonSyntaxException("object key must be followed by colon")
            fields[key] = value()
            whitespace()
            if (consume('}')) return JsonObject(fields)
            if (!consume(',')) throw JsonSyntaxException("object entries must be comma separated")
        }
    }

    private fun arrayValue(): JsonArray {
        val values = ArrayList<JsonValue>()
        whitespace()
        if (consume(']')) return JsonArray(values)
        while (true) {
            if (values.size >= HermesProcessingServiceV1Limits.MAX_METHODS) throw JsonSyntaxException("array exceeds v1 bound")
            values += value()
            whitespace()
            if (consume(']')) return JsonArray(values)
            if (!consume(',')) throw JsonSyntaxException("array entries must be comma separated")
        }
    }

    private fun number(): JsonNumber {
        val start = index
        if (consume('-')) { /* parsed below */ }
        if (consume('0')) {
            if (peek()?.isDigit() == true) throw JsonSyntaxException("leading zero in number")
        } else {
            if (peek()?.isDigit() != true) throw JsonSyntaxException("invalid number")
            while (peek()?.isDigit() == true) index++
        }
        val token = text.substring(start, index)
        return JsonNumber(token.toLongOrNull() ?: throw JsonSyntaxException("number out of range"))
    }

    private fun stringValue(): String {
        val result = StringBuilder()
        while (index < text.length) {
            when (val character = text[index++]) {
                '"' -> return result.toString()
                '\\' -> {
                    if (index >= text.length) throw JsonSyntaxException("unterminated escape")
                    when (val escaped = text[index++]) {
                        '"', '\\', '/' -> result.append(escaped)
                        'b' -> result.append('\b')
                        'f' -> result.append('\u000C')
                        'n' -> result.append('\n')
                        'r' -> result.append('\r')
                        't' -> result.append('\t')
                        'u' -> result.append(unicodeEscape())
                        else -> throw JsonSyntaxException("invalid string escape")
                    }
                }
                in '\u0000'..'\u001F' -> throw JsonSyntaxException("control character in string")
                else -> result.append(character)
            }
        }
        throw JsonSyntaxException("unterminated string")
    }

    private fun unicodeEscape(): Char {
        if (index + 4 > text.length) throw JsonSyntaxException("short unicode escape")
        val digits = text.substring(index, index + 4)
        index += 4
        return digits.toIntOrNull(16)?.toChar() ?: throw JsonSyntaxException("invalid unicode escape")
    }

    private fun whitespace() { while (peek()?.let { it == ' ' || it == '\n' || it == '\r' || it == '\t' } == true) index++ }
    private fun consume(expected: Char): Boolean { whitespace(); return if (peek() == expected) { index++; true } else false }
    private fun peek(): Char? = text.getOrNull(index)
}

private fun JsonValue.asStringOrFail(message: String): String = (this as? JsonString)?.value ?: throw JsonSyntaxException(message)
private fun JsonValue.asObjectOrFail(message: String): JsonObject = (this as? JsonObject) ?: throw JsonSyntaxException(message)
