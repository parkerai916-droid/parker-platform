package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.LinkedHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import parker.core.interfaces.FramingException
import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingFailureKind
import parker.core.interfaces.HermesProcessingMethod
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingServiceV1Framing
import parker.core.interfaces.HermesProcessingServiceV1Limits
import parker.core.interfaces.HermesProcessingServiceV1Request
import parker.core.interfaces.HermesProcessingServiceV1Response
import parker.core.interfaces.HermesProcessingServiceV1ResponseSerializer
import parker.core.interfaces.HermesStructuredRepresentation
import parker.core.interfaces.HermesV1BatchId
import parker.core.interfaces.HermesV1Failure
import parker.core.interfaces.HermesV1FailureCategory
import parker.core.interfaces.HermesV1FailureDetailCode
import parker.core.interfaces.HermesV1Issue
import parker.core.interfaces.HermesV1IssueCode
import parker.core.interfaces.HermesV1JobId
import parker.core.interfaces.HermesV1MediaType
import parker.core.interfaces.HermesV1MethodProvenance
import parker.core.interfaces.HermesV1OriginalFilename
import parker.core.interfaces.HermesV1OccurrenceId
import parker.core.interfaces.HermesV1ProcessingMethod
import parker.core.interfaces.HermesV1ProcessorIdentity
import parker.core.interfaces.HermesV1Provenance
import parker.core.interfaces.HermesV1RepresentationDescriptor
import parker.core.interfaces.HermesV1RepresentationId
import parker.core.interfaces.HermesV1RepresentationType
import parker.core.interfaces.HermesV1RequestId
import parker.core.interfaces.HermesV1Sha256
import parker.core.interfaces.HermesV1SourceReference
import parker.core.interfaces.HermesV1SourceSize
import parker.core.interfaces.HermesV1StatusRegistry
import parker.core.interfaces.HermesV1ProtocolVersion

/** Parker-owned, already-governed source context. [sourcePath] never crosses the wire. */
data class HermesV1PreparedSource(
    val requestId: HermesV1RequestId,
    val jobId: HermesV1JobId,
    val occurrenceId: HermesV1OccurrenceId,
    val batchId: HermesV1BatchId,
    val sourceReference: HermesV1SourceReference,
    val sourcePath: Path,
    val sourceSha256: HermesV1Sha256,
    val sizeBytes: HermesV1SourceSize,
    val originalFilename: HermesV1OriginalFilename,
    val mediaType: HermesV1MediaType,
    val requestedMethods: List<HermesV1ProcessingMethod>,
)

sealed interface HermesV1ClientOutcome {
    data class Accepted(val response: HermesProcessingServiceV1Response, val canonicalResponseBytes: ByteArray) : HermesV1ClientOutcome
    data class Rejected(val failure: HermesV1Failure) : HermesV1ClientOutcome
}

sealed interface HermesV1TransportOutcome {
    data class Response(val framedBytes: ByteArray) : HermesV1TransportOutcome
    data class Failed(val failure: HermesV1Failure) : HermesV1TransportOutcome
}

/** Testable transport seam. Implementations receive a verified source stream only. */
fun interface HermesV1RequestTransport {
    fun invoke(request: HermesProcessingServiceV1Request, source: InputStream): HermesV1TransportOutcome
}

/** Parker-side Unit 8 client; it performs no admission or Parker state mutation. */
class HermesProcessingServiceV1Client(
    private val transport: HermesV1RequestTransport,
) {
    fun process(prepared: HermesV1PreparedSource): HermesV1ClientOutcome {
        val request = try { verifyAndBuildRequest(prepared) } catch (failure: ClientValidationFailure) {
            return HermesV1ClientOutcome.Rejected(failure.failure)
        }
        val transportResult = try {
            Files.newInputStream(prepared.sourcePath).use { source -> transport.invoke(request, source) }
        } catch (_: IOException) {
            HermesV1TransportOutcome.Failed(clientFailure(HermesV1FailureDetailCode.SOURCE_STREAM_FAILED, true, "Parker source could not be opened"))
        } catch (_: RuntimeException) {
            HermesV1TransportOutcome.Failed(clientFailure(HermesV1FailureDetailCode.TRANSPORT_DISCONNECTED, true, "Parker Hermes transport failed"))
        }
        return when (transportResult) {
            is HermesV1TransportOutcome.Failed -> HermesV1ClientOutcome.Rejected(transportResult.failure)
            is HermesV1TransportOutcome.Response -> decodeResponse(transportResult.framedBytes, request)
        }
    }

    private fun verifyAndBuildRequest(prepared: HermesV1PreparedSource): HermesProcessingServiceV1Request {
        if (prepared.requestedMethods.isEmpty() || prepared.requestedMethods.size > HermesProcessingServiceV1Limits.MAX_METHODS) {
            throw ClientValidationFailure(clientFailure(HermesV1FailureDetailCode.UNSUPPORTED_METHOD, false, "no supported processing method was selected"))
        }
        if (prepared.requestedMethods.distinct().size != prepared.requestedMethods.size) {
            throw ClientValidationFailure(clientFailure(HermesV1FailureDetailCode.INVALID_FIELD, false, "processing methods must be unique"))
        }
        val allowedMethods = HermesV1ClientMethodSelection.forMediaType(prepared.mediaType.value)
            ?: throw ClientValidationFailure(clientFailure(HermesV1FailureDetailCode.UNSUPPORTED_MEDIA_TYPE, false, "media type is not enabled for Hermes v1 client processing"))
        if (prepared.requestedMethods.any { it !in allowedMethods }) {
            throw ClientValidationFailure(clientFailure(HermesV1FailureDetailCode.METHOD_SOURCE_MISMATCH, false, "requested method is incompatible with the governed media type"))
        }
        val actualSize: Long
        val actualDigest: HermesV1Sha256
        try {
            actualSize = Files.size(prepared.sourcePath)
            val digest = MessageDigest.getInstance("SHA-256")
            DigestInputStream(Files.newInputStream(prepared.sourcePath), digest).use { input ->
                val buffer = ByteArray(HermesProcessingServiceV1Framing.SOURCE_COPY_BUFFER_BYTES)
                while (input.read(buffer) != -1) { /* bounded pre-send verification */ }
            }
            actualDigest = HermesV1Sha256(digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) })
        } catch (_: IOException) {
            throw ClientValidationFailure(clientFailure(HermesV1FailureDetailCode.SOURCE_STREAM_FAILED, true, "Parker source could not be verified"))
        }
        if (actualSize != prepared.sizeBytes.value) {
            throw ClientValidationFailure(clientFailure(HermesV1FailureDetailCode.SOURCE_SIZE_MISMATCH, false, "Parker source size changed before send"))
        }
        if (actualDigest != prepared.sourceSha256) {
            throw ClientValidationFailure(clientFailure(HermesV1FailureDetailCode.SOURCE_HASH_MISMATCH, false, "Parker source hash changed before send"))
        }
        return HermesProcessingServiceV1Request(
            protocolVersion = HermesV1ProtocolVersion.CURRENT,
            requestId = prepared.requestId,
            jobId = prepared.jobId,
            occurrenceId = prepared.occurrenceId,
            batchId = prepared.batchId,
            source = parker.core.interfaces.HermesProcessingServiceV1Source(
                prepared.sourceReference, prepared.sourceSha256, prepared.sizeBytes, prepared.originalFilename, prepared.mediaType,
            ),
            requestedMethods = prepared.requestedMethods,
        )
    }

    private fun decodeResponse(frame: ByteArray, request: HermesProcessingServiceV1Request): HermesV1ClientOutcome {
        return try {
            val body = HermesV1ResponseDecoder.decodeFrame(frame)
            val response = HermesV1ResponseDecoder.decodeResponse(body, request)
            HermesV1ClientOutcome.Accepted(response, body)
        } catch (failure: FramingException) {
            HermesV1ClientOutcome.Rejected(failure.failure)
        } catch (_: RuntimeException) {
            HermesV1ClientOutcome.Rejected(clientFailure(HermesV1FailureDetailCode.MALFORMED_JSON, false, "Hermes response failed v1 validation"))
        }
    }
}

/** Closed Parker-side routing vocabulary; unsupported formats never become native requests. */
object HermesV1ClientMethodSelection {
    fun forMediaType(mediaType: String): Set<HermesV1ProcessingMethod>? = when (mediaType) {
        "application/pdf", "text/plain" -> setOf(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION)
        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/rtf", "text/rtf", "application/x-rtf", "text/csv" -> setOf(HermesV1ProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION)
        "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> setOf(HermesV1ProcessingMethod.STRUCTURED_SPREADSHEET_EXTRACTION)
        "message/rfc822", "application/vnd.ms-outlook", "application/x-ole-storage" -> setOf(HermesV1ProcessingMethod.STRUCTURED_EMAIL_EXTRACTION)
        else -> null
    }
}

/** Secure production transport seam; the remote forced command is fixed by deployment. */
class HermesV1SshRequestTransport(
    private val keyPath: Path,
    private val knownHostsPath: Path,
    private val host: String = "192.168.178.45",
    private val user: String = "steve",
    private val timeoutMillis: Long = 300_000L,
    private val processFactory: (List<String>) -> Process = { ProcessBuilder(it).start() },
) : HermesV1RequestTransport {
    override fun invoke(request: HermesProcessingServiceV1Request, source: InputStream): HermesV1TransportOutcome {
        val command = listOf(
            "ssh", "-T", "-i", keyPath.toString(),
            "-o", "IdentitiesOnly=yes", "-o", "BatchMode=yes",
            "-o", "StrictHostKeyChecking=yes", "-o", "UserKnownHostsFile=${knownHostsPath}",
            "$user@$host",
        )
        val process = try { processFactory(command) } catch (_: Exception) {
            return HermesV1TransportOutcome.Failed(clientFailure(HermesV1FailureDetailCode.TRANSPORT_DISCONNECTED, true, "Hermes SSH could not be launched"))
        }
        val pool = Executors.newFixedThreadPool(2)
        return try {
            val stdout = pool.submit<ByteArray> { readBounded(process.inputStream, HermesProcessingServiceV1Limits.MAX_INLINE_RESPONSE_BYTES + 4) }
            val stderr = pool.submit<ByteArray> { readBounded(process.errorStream, MAX_STDERR_BYTES) }
            process.outputStream.use { output -> HermesProcessingServiceV1Framing.writeRequestFrame(output, request, source) }
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                HermesV1TransportOutcome.Failed(clientFailure(HermesV1FailureDetailCode.PROCESSING_DEADLINE_EXCEEDED, true, "Hermes SSH processing timed out"))
            } else {
                val out = stdout.get(2, TimeUnit.SECONDS)
                stderr.get(2, TimeUnit.SECONDS)
                if (process.exitValue() != 0) HermesV1TransportOutcome.Failed(clientFailure(HermesV1FailureDetailCode.TRANSPORT_DISCONNECTED, true, "Hermes SSH forced command failed"))
                else HermesV1TransportOutcome.Response(out)
            }
        } catch (_: FramingException) {
            HermesV1TransportOutcome.Failed(clientFailure(HermesV1FailureDetailCode.SOURCE_SIZE_MISMATCH, false, "source changed while framing request"))
        } catch (_: Exception) {
            HermesV1TransportOutcome.Failed(clientFailure(HermesV1FailureDetailCode.TRANSPORT_DISCONNECTED, true, "Hermes SSH transport failed"))
        } finally {
            pool.shutdownNow()
            if (process.isAlive) process.destroyForcibly()
        }
    }

    private fun readBounded(input: InputStream, limit: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) return output.toByteArray()
            total += count
            if (total > limit) throw IOException("bounded transport output exceeded")
            output.write(buffer, 0, count)
        }
    }

    companion object { const val MAX_STDERR_BYTES: Long = 64L * 1024L }
}

private class ClientValidationFailure(val failure: HermesV1Failure) : IllegalArgumentException()

private fun clientFailure(code: HermesV1FailureDetailCode, retryable: Boolean, detail: String): HermesV1Failure =
    HermesV1Failure(code.category, code, retryable, detail)

private object HermesV1ResponseDecoder {
    fun decodeFrame(frame: ByteArray): ByteArray {
        if (frame.size < 4) throw framingFailure(HermesV1FailureDetailCode.SHORT_METADATA_LENGTH_READ, "response length prefix is incomplete")
        val length = ByteBuffer.wrap(frame, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xffffffffL
        if (length > HermesProcessingServiceV1Limits.MAX_INLINE_RESPONSE_BYTES) throw framingFailure(HermesV1FailureDetailCode.RESPONSE_TOO_LARGE, "response exceeds the inline v1 limit")
        if (frame.size.toLong() != length + 4L) throw framingFailure(HermesV1FailureDetailCode.TRAILING_DATA, "response length does not match the received bytes")
        return try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(frame, 4, length.toInt())).toString().toByteArray(StandardCharsets.UTF_8)
        } catch (_: CharacterCodingException) {
            throw framingFailure(HermesV1FailureDetailCode.INVALID_UTF8, "response is not valid UTF-8")
        }
    }

    fun decodeResponse(body: ByteArray, request: HermesProcessingServiceV1Request): HermesProcessingServiceV1Response {
        val root = V1JsonParser(String(body, StandardCharsets.UTF_8)).parse().obj()
        root.rejectUnknown(setOf("protocolVersion", "requestId", "jobId", "occurrenceId", "batchId", "sourceSha256", "status", "methods", "representations", "issues", "failure", "provenance"))
        val protocol = HermesV1ProtocolVersion(root.string("protocolVersion"))
        val requestId = HermesV1RequestId(root.string("requestId"))
        val jobId = HermesV1JobId(root.string("jobId"))
        val occurrenceId = HermesV1OccurrenceId(root.string("occurrenceId"))
        val batchId = HermesV1BatchId(root.string("batchId"))
        val sha = HermesV1Sha256(root.string("sourceSha256"))
        if (requestId != request.requestId || jobId != request.jobId || occurrenceId != request.occurrenceId || batchId != request.batchId || sha != request.source.sourceSha256) {
            throw framingFailure(HermesV1FailureDetailCode.INVALID_FIELD, "Hermes response identity does not match the request")
        }
        val status = HermesV1StatusRegistry.fromWireValue(root.string("status"))
        val methods = root.array("methods").map { HermesV1ProcessingMethod.fromWireValue(it.str()) }
        val representations = root.array("representations").map { representation(it.obj()) }
        if (representations.size > HermesProcessingServiceV1Limits.MAX_REPRESENTATIONS) throw framingFailure(HermesV1FailureDetailCode.REPRESENTATION_COUNT_EXCEEDED, "too many response representations")
        val issues = root.array("issues").map { issue(it.obj()) }
        if (issues.size > HermesProcessingServiceV1Limits.MAX_ISSUES) throw framingFailure(HermesV1FailureDetailCode.ISSUE_COUNT_EXCEEDED, "too many response issues")
        val failure = root.optional("failure")?.let { if (it is V1JsonNull) null else failure(it.obj()) }
        val provenance = provenance(root.obj("provenance"), sha)
        val structured = representations.firstNotNullOfOrNull { it.content }
        val established = HermesProcessingResult(
            sourceSha256 = sha.value,
            batchId = batchId.value,
            status = status,
            methods = methods.map { it.establishedMethod }.toSet(),
            issues = issues.map { HermesProcessingIssueMapper.map(it) },
            failure = failure?.let { HermesProcessingFailure(HermesProcessingFailureKind.PROCESSOR_FAILURE, it.detail) },
            structuredRepresentation = structured,
        )
        val response = HermesProcessingServiceV1Response(protocol, requestId, jobId, occurrenceId, batchId, sha, established, representations, issues, failure, provenance)
        if (!body.contentEquals(HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(response))) {
            throw framingFailure(HermesV1FailureDetailCode.MALFORMED_JSON, "Hermes response is not canonical v1 JSON")
        }
        return response
    }

    private fun representation(value: V1JsonObject): HermesV1RepresentationDescriptor {
        value.rejectUnknown(setOf("representationId", "type", "method", "content"))
        val type = HermesV1RepresentationType.fromWireValue(value.string("type"))
        val method = HermesV1ProcessingMethod.fromWireValue(value.string("method"))
        val content = value.optional("content")?.let { if (it is V1JsonNull) null else content(it.obj(), type) }
        return HermesV1RepresentationDescriptor(HermesV1RepresentationId(value.string("representationId")), type, method, content)
    }

    private fun content(value: V1JsonObject, type: HermesV1RepresentationType): HermesStructuredRepresentation {
        return when (type) {
            HermesV1RepresentationType.TEXT -> {
                value.rejectUnknown(setOf("text")); HermesStructuredRepresentation.Text(value.string("text"))
            }
            HermesV1RepresentationType.STRUCTURED_DOCUMENT -> {
                if (value.fields.containsKey("sheets")) spreadsheet(value) else email(value)
            }
        }
    }

    private fun spreadsheet(value: V1JsonObject): HermesStructuredRepresentation.Spreadsheet {
        value.rejectUnknown(setOf("workbook", "sheets"))
        val sheets = value.array("sheets").map { sheetValue ->
            val sheet = sheetValue.obj(); sheet.rejectUnknown(setOf("name", "cells"))
            HermesStructuredRepresentation.Spreadsheet.Sheet(sheet.string("name"), sheet.array("cells").map { cellValue ->
                val cell = cellValue.obj(); cell.rejectUnknown(setOf("coordinate", "value", "formula", "displayedValue"))
                HermesStructuredRepresentation.Spreadsheet.Cell(cell.string("coordinate"), cell.optionalString("value"), cell.optionalString("formula"), cell.optionalString("displayedValue"))
            })
        }
        return HermesStructuredRepresentation.Spreadsheet(value.optionalString("workbook"), sheets)
    }

    private fun email(value: V1JsonObject): HermesStructuredRepresentation.Email {
        value.rejectUnknown(setOf("from", "to", "cc", "bcc", "subject", "date", "body", "messageFormat", "attachments"))
        val attachments = value.array("attachments").map { item ->
            val attachment = item.obj(); attachment.rejectUnknown(setOf("filename", "contentType"))
            HermesStructuredRepresentation.Email.Attachment(attachment.optionalString("filename"), attachment.optionalString("contentType"))
        }
        return HermesStructuredRepresentation.Email(value.optionalString("from"), value.optionalString("to"), value.optionalString("cc"), value.optionalString("bcc"), value.optionalString("subject"), value.optionalString("date"), value.string("body"), value.optionalString("messageFormat"), attachments)
    }

    private fun issue(value: V1JsonObject): HermesV1Issue {
        value.rejectUnknown(setOf("code", "explanation"))
        return HermesV1Issue(HermesV1IssueCode.fromWireValue(value.string("code")), value.string("explanation"))
    }

    private fun failure(value: V1JsonObject): HermesV1Failure {
        value.rejectUnknown(setOf("category", "detailCode", "retryable", "detail"))
        val category = HermesV1FailureCategory.fromWireValue(value.string("category"))
        val detail = HermesV1FailureDetailCode.fromWireValue(value.string("detailCode"))
        if (detail.category != category) throw framingFailure(HermesV1FailureDetailCode.INVALID_FIELD, "failure category and detail code disagree")
        return HermesV1Failure(category, detail, value.bool("retryable"), value.string("detail"))
    }

    private fun provenance(value: V1JsonObject, sourceSha: HermesV1Sha256): HermesV1Provenance {
        value.rejectUnknown(setOf("sourceSha256", "processor", "processorVersion", "operations", "provenanceDigest"))
        if (HermesV1Sha256(value.string("sourceSha256")) != sourceSha) throw framingFailure(HermesV1FailureDetailCode.INVALID_FIELD, "provenance source hash disagrees")
        val operations = value.array("operations").map { item ->
            val operation = item.obj(); operation.rejectUnknown(setOf("method", "startedAt", "completedAt", "configurationDigest"))
            HermesV1MethodProvenance(HermesV1ProcessingMethod.fromWireValue(operation.string("method")), Instant.parse(operation.string("startedAt")), Instant.parse(operation.string("completedAt")), operation.optionalString("configurationDigest")?.let(::HermesV1Sha256))
        }
        return HermesV1Provenance(HermesV1Sha256(value.string("sourceSha256")), HermesV1ProcessorIdentity(value.string("processor"), value.string("processorVersion")), operations, value.optionalString("provenanceDigest")?.let(::HermesV1Sha256))
    }
}

private object HermesProcessingIssueMapper {
    fun map(issue: HermesV1Issue): parker.core.interfaces.HermesProcessingIssue =
        parker.core.interfaces.HermesProcessingIssue(
            when (issue.code) {
                HermesV1IssueCode.UNSUPPORTED_CONTENT -> parker.core.interfaces.HermesProcessingIssueKind.UNSUPPORTED_CONTENT
                HermesV1IssueCode.MISSING_CONTENT -> parker.core.interfaces.HermesProcessingIssueKind.MISSING_CONTENT
                HermesV1IssueCode.MALFORMED_STRUCTURE -> parker.core.interfaces.HermesProcessingIssueKind.PROCESSING_EXCEPTION
                HermesV1IssueCode.STRUCTURE_AMBIGUITY -> parker.core.interfaces.HermesProcessingIssueKind.TABLE_STRUCTURE_AMBIGUITY
                HermesV1IssueCode.CONFLICTING_EXTRACTION -> parker.core.interfaces.HermesProcessingIssueKind.CONFLICTING_EXTRACTION_OUTPUTS
                HermesV1IssueCode.OUTPUT_LIMIT_EXCEEDED -> parker.core.interfaces.HermesProcessingIssueKind.PROCESSING_EXCEPTION
                HermesV1IssueCode.PROCESSING_QUALIFICATION -> parker.core.interfaces.HermesProcessingIssueKind.PROCESSING_EXCEPTION
            }, issue.explanation,
        )
}

private fun framingFailure(code: HermesV1FailureDetailCode, detail: String): FramingException =
    FramingException(HermesV1Failure(code.category, code, retryable = code.category == HermesV1FailureCategory.TRANSPORT, detail))

private sealed interface V1JsonValue {
    fun obj(): V1JsonObject = error("JSON object required")
    fun str(): String = error("JSON string required")
}
private class V1JsonObject(val fields: LinkedHashMap<String, V1JsonValue>) : V1JsonValue {
    fun required(name: String): V1JsonValue = fields[name] ?: error("missing response field: $name")
    fun string(name: String) = required(name).str()
    fun bool(name: String) = when (required(name)) { V1JsonBoolean -> true; V1JsonFalse -> false; else -> error("boolean required: $name") }
    fun array(name: String) = required(name).let { it as? V1JsonArray ?: error("array required: $name") }.values
    fun obj(name: String) = required(name).obj()
    fun optional(name: String) = fields[name]
    fun optionalString(name: String) = optional(name)?.let { if (it is V1JsonNull) null else it.str() }
    fun rejectUnknown(allowed: Set<String>) { fields.keys.firstOrNull { it !in allowed }?.let { error("unknown response field: $it") } }
    override fun obj() = this
}
private data class V1JsonString(val value: String) : V1JsonValue { override fun str() = value }
private data class V1JsonArray(val values: List<V1JsonValue>) : V1JsonValue
private data object V1JsonBoolean : V1JsonValue
private data object V1JsonFalse : V1JsonValue
private data object V1JsonNull : V1JsonValue

private class V1JsonParser(private val text: String) {
    private var index = 0
    fun parse(): V1JsonValue { val result = value(); whitespace(); if (index != text.length) error("trailing JSON") ; return result }
    private fun value(): V1JsonValue { whitespace(); return when (text.getOrNull(index)) {
        '{' -> objectValue(); '[' -> arrayValue(); '"' -> V1JsonString(stringValue()); 't' -> literal("true", V1JsonBoolean); 'f' -> literal("false", V1JsonFalse); 'n' -> literal("null", V1JsonNull); else -> error("unsupported JSON value")
    } }
    private fun literal(expected: String, value: V1JsonValue): V1JsonValue { if (!text.startsWith(expected, index)) error("invalid JSON literal"); index += expected.length; return value }
    private fun objectValue(): V1JsonObject { index++; val fields = LinkedHashMap<String, V1JsonValue>(); whitespace(); if (consume('}')) return V1JsonObject(fields); while (true) { whitespace(); if (text.getOrNull(index) != '"') error("object key required"); val key = stringValue(); if (fields.containsKey(key)) error("duplicate JSON field"); whitespace(); expect(':'); fields[key] = value(); whitespace(); if (consume('}')) return V1JsonObject(fields); expect(',') } }
    private fun arrayValue(): V1JsonArray { index++; val values = mutableListOf<V1JsonValue>(); whitespace(); if (consume(']')) return V1JsonArray(values); while (true) { values += value(); whitespace(); if (consume(']')) return V1JsonArray(values); expect(',') } }
    private fun stringValue(): String { expect('"'); val result = StringBuilder(); while (index < text.length) { val c = text[index++]; when (c) { '"' -> return result.toString(); '\\' -> { if (index >= text.length) error("invalid escape"); val escaped = text[index++]; result.append(when (escaped) { '"' -> '"'; '\\' -> '\\'; '/' -> '/'; 'b' -> '\b'; 'f' -> '\u000C'; 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; else -> error("unsupported escape") }) }; else -> { if (c.code < 0x20) error("control character in string"); result.append(c) } } }; error("unterminated string") }
    private fun whitespace() { while (text.getOrNull(index)?.isWhitespace() == true) index++ }
    private fun consume(c: Char): Boolean = if (text.getOrNull(index) == c) { index++; true } else false
    private fun expect(c: Char) { if (!consume(c)) error("expected $c") }
}
