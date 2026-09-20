package parker.core.interfaces

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder

/** The single deterministic JSON representation used for v1 response bodies. */
object HermesProcessingServiceV1ResponseSerializer {
    fun canonicalJson(response: HermesProcessingServiceV1Response): String = buildString {
        append('{')
        quotedField("protocolVersion", response.protocolVersion.value)
        quotedField("requestId", response.requestId.value)
        quotedField("jobId", response.jobId.value)
        quotedField("occurrenceId", response.occurrenceId.value)
        quotedField("batchId", response.batchId.value)
        quotedField("sourceSha256", response.sourceSha256.value)
        quotedField("status", response.establishedResult.status.name)
        append(",\"methods\":[")
        response.establishedResult.methods.map { established ->
            HermesV1ProcessingMethod.entries.firstOrNull { it.establishedMethod == established }
                ?: throw serializerFailure(HermesV1FailureDetailCode.UNSUPPORTED_METHOD, "response contains an unavailable method")
        }.sortedBy { it.wireValue }.forEachIndexed { index, method ->
            if (index > 0) append(',')
            quoted(method.wireValue)
        }
        append(']')
        append(",\"representations\":[")
        response.representations.sortedBy { it.representationId.value }.forEachIndexed { index, representation ->
            if (index > 0) append(',')
            append('{')
            quotedField("representationId", representation.representationId.value)
            quotedField("type", representation.type.wireValue)
            quotedField("method", representation.method.wireValue)
            append(",\"content\":")
            appendContent(representation.content ?: if (index == 0) response.establishedResult.structuredRepresentation else null)
            append('}')
        }
        append(']')
        append(",\"issues\":[")
        response.issues.forEachIndexed { index, issue ->
            if (index > 0) append(',')
            append('{')
            quotedField("code", issue.code.wireValue)
            quotedField("explanation", issue.explanation)
            append('}')
        }
        append(']')
        append(",\"failure\":")
        response.failure?.let {
            append('{')
            quotedField("category", it.category.wireValue)
            quotedField("detailCode", it.detailCode.wireValue)
            append(",\"retryable\":").append(it.retryable)
            quotedField("detail", it.detail)
            append('}')
        } ?: append("null")
        append(",\"provenance\":{")
        quotedField("sourceSha256", response.provenance.sourceSha256.value)
        quotedField("processor", response.provenance.processor.name)
        quotedField("processorVersion", response.provenance.processor.version)
        append(",\"operations\":[")
        response.provenance.operations.forEachIndexed { index, operation ->
            if (index > 0) append(',')
            append('{')
            quotedField("method", operation.method.wireValue)
            quotedField("startedAt", timestamp(operation.startedAt))
            quotedField("completedAt", timestamp(operation.completedAt))
            operation.configurationDigest?.let { quotedField("configurationDigest", it.value) }
            append('}')
        }
        append(']')
        response.provenance.provenanceDigest?.let { quotedField("provenanceDigest", it.value) }
        append("}}")
    }.also { json ->
        if (json.toByteArray(StandardCharsets.UTF_8).size.toLong() > HermesProcessingServiceV1Limits.MAX_INLINE_RESPONSE_BYTES) {
            throw serializerFailure(HermesV1FailureDetailCode.RESPONSE_TOO_LARGE, "response exceeds the inline v1 limit")
        }
    }

    fun canonicalJsonUtf8(response: HermesProcessingServiceV1Response): ByteArray = try {
        canonicalJson(response).toByteArray(StandardCharsets.UTF_8)
    } catch (error: FramingException) {
        if (error.failure.category != HermesV1FailureCategory.RESOURCE_LIMIT) throw error
        canonicalJson(resourceLimitResponse(response, error.failure)).toByteArray(StandardCharsets.UTF_8)
    }

    /** Canonical JSON configuration scope for provenance digests; timestamps are excluded. */
    fun configurationDigest(fields: Map<String, String>): HermesV1Sha256 {
        val json = buildString {
            append('{')
            fields.toSortedMap().entries.forEachIndexed { index, (key, value) ->
                if (index > 0) append(',')
                quoted(key)
                append(':')
                quoted(value)
            }
            append('}')
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(json.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return HermesV1Sha256(digest)
    }

    private fun StringBuilder.appendContent(content: HermesStructuredRepresentation?) {
        val contentStart = length
        when (content) {
            null -> append("null")
            is HermesStructuredRepresentation.Text -> {
                if (content.text.length > HermesProcessingServiceV1Limits.MAX_EXTRACTED_TEXT_CHARACTERS) {
                    throw serializerFailure(HermesV1FailureDetailCode.TEXT_TOO_LARGE, "text representation exceeds the v1 limit")
                }
                append('{'); quotedField("text", content.text); append('}')
            }
            is HermesStructuredRepresentation.Spreadsheet -> {
                append('{'); nullableField("workbook", content.workbook)
                append(",\"sheets\":[")
                content.sheets.forEachIndexed { sheetIndex, sheet ->
                    if (sheetIndex > 0) append(',')
                    append('{'); quotedField("name", sheet.name); append(",\"cells\":[")
                    sheet.cells.forEachIndexed { cellIndex, cell ->
                        if (cellIndex > 0) append(',')
                        append('{'); quotedField("coordinate", cell.coordinate)
                        nullableField("value", cell.value); nullableField("formula", cell.formula); nullableField("displayedValue", cell.displayedValue)
                        append('}')
                    }
                    append("]}")
                }
                append("]}")
            }
            is HermesStructuredRepresentation.Email -> {
                append('{')
                nullableField("from", content.from); nullableField("to", content.to); nullableField("cc", content.cc); nullableField("bcc", content.bcc)
                nullableField("subject", content.subject); nullableField("date", content.date); quotedField("body", content.body); nullableField("messageFormat", content.messageFormat)
                append(",\"attachments\":[")
                content.attachments.forEachIndexed { index, attachment ->
                    if (index > 0) append(',')
                    append('{'); nullableField("filename", attachment.filename); nullableField("contentType", attachment.contentType); append('}')
                }
                append("]}")
            }
        }
        if (substring(contentStart).toByteArray(StandardCharsets.UTF_8).size.toLong() > HermesProcessingServiceV1Limits.MAX_STRUCTURED_REPRESENTATION_BYTES) {
            throw serializerFailure(HermesV1FailureDetailCode.STRUCTURED_RESULT_TOO_LARGE, "structured representation exceeds the v1 limit")
        }
    }

    private fun StringBuilder.quotedField(name: String, value: String) {
        if (isNotEmpty() && last() != '{') append(',')
        quoted(name); append(':'); quoted(value)
    }

    private fun StringBuilder.nullableField(name: String, value: String?) {
        if (isNotEmpty() && last() != '{') append(',')
        quoted(name); append(':')
        if (value == null) append("null") else quoted(value)
    }

    private fun StringBuilder.quoted(value: String) {
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

    private fun timestamp(value: Instant): String {
        if (value.nano % 1_000_000 != 0) throw serializerFailure(HermesV1FailureDetailCode.INVALID_FIELD, "provenance timestamps must have millisecond precision")
        return DateTimeFormatterBuilder().appendPattern("uuuu-MM-dd'T'HH:mm:ss.SSSX").toFormatter().withZone(ZoneOffset.UTC).format(value)
    }

    /**
     * Replaces an unrepresentable result with a bounded terminal failure. This
     * path has no result content and is therefore not recursive on size.
     */
    private fun resourceLimitResponse(
        response: HermesProcessingServiceV1Response,
        resourceFailure: HermesV1Failure,
    ): HermesProcessingServiceV1Response {
        val establishedFailure = HermesProcessingFailure(
            HermesProcessingFailureKind.PROCESSOR_FAILURE,
            resourceFailure.detail,
        )
        val established = response.establishedResult.copy(
            status = HermesProcessingStatus.FAILED,
            issues = emptyList(),
            failure = establishedFailure,
            structuredRepresentation = null,
        )
        return response.copy(
            establishedResult = established,
            representations = emptyList(),
            issues = emptyList(),
            failure = resourceFailure,
            provenance = response.provenance.copy(
                operations = response.provenance.operations.take(1),
                provenanceDigest = null,
            ),
        )
    }

    private fun serializerFailure(code: HermesV1FailureDetailCode, detail: String): FramingException =
        FramingException(HermesV1Failure(code.category, code, retryable = false, detail))
}
