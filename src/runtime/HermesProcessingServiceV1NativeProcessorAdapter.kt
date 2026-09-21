package parker.core.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.temporal.ChronoUnit
import java.time.Instant
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import parker.core.interfaces.CsvStructuralExtractionOutcome
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingFailureKind
import parker.core.interfaces.HermesProcessingIssue
import parker.core.interfaces.HermesProcessingIssueKind
import parker.core.interfaces.HermesProcessingMethod
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingServiceV1Framing
import parker.core.interfaces.HermesProcessingServiceV1Limits
import parker.core.interfaces.HermesProcessingServiceV1Request
import parker.core.interfaces.HermesProcessingServiceV1Response
import parker.core.interfaces.HermesProcessingServiceV1ResponseSerializer
import parker.core.interfaces.HermesStructuredRepresentation
import parker.core.interfaces.HermesV1AuthorizationResult
import parker.core.interfaces.HermesV1Failure
import parker.core.interfaces.HermesV1FailureCategory
import parker.core.interfaces.HermesV1FailureDetailCode
import parker.core.interfaces.HermesV1Issue
import parker.core.interfaces.HermesV1MethodProvenance
import parker.core.interfaces.HermesV1ProcessingMethod
import parker.core.interfaces.HermesV1ProcessingPrincipal
import parker.core.interfaces.HermesV1ProcessorIdentity
import parker.core.interfaces.HermesV1Provenance
import parker.core.interfaces.HermesV1RepresentationDescriptor
import parker.core.interfaces.HermesV1RepresentationId
import parker.core.interfaces.HermesV1RepresentationType
import parker.core.interfaces.HermesV1Sha256
import parker.core.runtime.HermesV1SourceReceiptOutcome.Verified

/** Result of the native/structured adapter; it never performs Parker admission. */
sealed interface HermesV1NativeProcessingOutcome {
    data class Produced(val response: HermesProcessingServiceV1Response) : HermesV1NativeProcessingOutcome
    data class Rejected(val response: HermesProcessingServiceV1Response) : HermesV1NativeProcessingOutcome
}

/**
 * Unit 6 adapter. Its public processing entry accepts only a Unit 4 verified
 * source, so raw arbitrary paths cannot enter the normal processor path.
 */
class HermesProcessingServiceV1NativeProcessorAdapter(
    private val authorizer: parker.core.interfaces.HermesV1CapabilityAuthorizer = parker.core.interfaces.HermesV1CapabilityAuthorizer(
        parker.core.interfaces.HermesV1CapabilityPolicy.initial(),
    ),
    private val pdfExtractor: parker.core.interfaces.PdfStructuralExtractor = TikaPdfStructuralExtractor(),
    private val structuredExtractor: parker.core.interfaces.StructuredDocumentExtractor = ApacheStructuredDocumentExtractor(),
    private val csvExtractor: parker.core.interfaces.CsvStructuralExtractor = ApacheCommonsCsvExtractor(),
    private val emlExtractor: parker.core.interfaces.EmlStructuralExtractor = ApacheJamesMime4jExtractor(),
) {
    suspend fun process(
        principal: HermesV1ProcessingPrincipal,
        verified: HermesV1VerifiedSource,
    ): HermesV1NativeProcessingOutcome {
        val request = verified.request
        val authorization = authorizer.authorize(principal, request)
        if (authorization is HermesV1AuthorizationResult.Denied) {
            return HermesV1NativeProcessingOutcome.Rejected(failureResponse(request, authorization.failure))
        }
        val methods = request.requestedMethods
        if (methods.isEmpty()) return HermesV1NativeProcessingOutcome.Rejected(
            failureResponse(request, failure(HermesV1FailureDetailCode.UNSUPPORTED_METHOD, false)),
        )

        val sourceBytes = try {
            verified.handle.open().use { it.readBytes() }
        } catch (_: Exception) {
            return HermesV1NativeProcessingOutcome.Rejected(
                failureResponse(request, failure(HermesV1FailureDetailCode.SOURCE_STREAM_FAILED, true)),
            )
        }
        if (sourceBytes.size.toLong() != request.source.sizeBytes.value) {
            return HermesV1NativeProcessingOutcome.Rejected(
                failureResponse(request, failure(HermesV1FailureDetailCode.SOURCE_SIZE_MISMATCH, false)),
            )
        }

        val startedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val representations = mutableListOf<HermesV1RepresentationDescriptor>()
        val issues = mutableListOf<HermesV1Issue>()
        var content: HermesStructuredRepresentation? = null
        var processor = HermesV1ProcessorIdentity("hermes-native-processor-adapter", "1")
        for (method in methods) {
            val result = try {
                withTimeout(HermesProcessingServiceV1Limits.PROCESSING_TIMEOUT_SECONDS * 1_000L) {
                    dispatch(method, request, sourceBytes)
                }
            } catch (_: TimeoutCancellationException) {
                return HermesV1NativeProcessingOutcome.Rejected(failureResponse(request, failure(HermesV1FailureDetailCode.PROCESSOR_TIMEOUT, true), methods, startedAt))
            } catch (limit: LimitFailure) {
                return HermesV1NativeProcessingOutcome.Rejected(failureResponse(request, failure(limit.code, false), methods, startedAt))
            } catch (_: UnsupportedOperationException) {
                return HermesV1NativeProcessingOutcome.Rejected(failureResponse(request, failure(HermesV1FailureDetailCode.UNSUPPORTED_METHOD, false)))
            } catch (_: Exception) {
                return HermesV1NativeProcessingOutcome.Rejected(failureResponse(request, failure(HermesV1FailureDetailCode.MALFORMED_PROCESSOR_RESULT, false)))
            }
            when (result) {
                is DispatchResult.Failure -> return HermesV1NativeProcessingOutcome.Rejected(
                    failureResponse(request, result.failure, methods, startedAt),
                )
                is DispatchResult.Success -> {
                    val mapped = try {
                        mapRepresentation(result, request)
                    } catch (limit: LimitFailure) {
                        return HermesV1NativeProcessingOutcome.Rejected(failureResponse(request, failure(limit.code, false), methods, startedAt))
                    }
                    if (mapped.content != null) content = mapped.content
                    representations += HermesV1RepresentationDescriptor(
                        HermesV1RepresentationId("representation-${representations.size + 1}"),
                        mapped.type,
                        method,
                        mapped.content,
                    )
                    issues += mapped.issues
                    processor = mapped.processor
                }
            }
        }
        if (representations.size > HermesProcessingServiceV1Limits.MAX_REPRESENTATIONS) {
            return HermesV1NativeProcessingOutcome.Rejected(failureResponse(request, failure(HermesV1FailureDetailCode.REPRESENTATION_COUNT_EXCEEDED, false), methods, startedAt))
        }
        if (issues.size > HermesProcessingServiceV1Limits.MAX_ISSUES) {
            return HermesV1NativeProcessingOutcome.Rejected(failureResponse(request, failure(HermesV1FailureDetailCode.ISSUE_COUNT_EXCEEDED, false), methods, startedAt))
        }
        val completedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val provenance = HermesV1Provenance(
            sourceSha256 = request.source.sourceSha256,
            processor = processor,
            operations = methods.map { method -> HermesV1MethodProvenance(method, startedAt, completedAt, configurationDigest(method, request.source.mediaType.value)) },
        )
        val established = HermesProcessingResult(
            sourceSha256 = request.source.sourceSha256.value,
            batchId = request.batchId.value,
            status = parker.core.interfaces.HermesProcessingStatus.PASS,
            methods = methods.map { it.establishedMethod }.toSet(),
            issues = emptyList(),
            structuredRepresentation = content,
        )
        val response = HermesProcessingServiceV1Response(
            protocolVersion = request.protocolVersion,
            requestId = request.requestId,
            jobId = request.jobId,
            occurrenceId = request.occurrenceId,
            batchId = request.batchId,
            sourceSha256 = request.source.sourceSha256,
            establishedResult = established,
            representations = representations,
            issues = issues,
            failure = null,
            provenance = provenance,
        )
        HermesProcessingServiceV1Framing.encodeResponseFrame(response)
        return HermesV1NativeProcessingOutcome.Produced(response)
    }

    private suspend fun dispatch(method: HermesV1ProcessingMethod, request: HermesProcessingServiceV1Request, bytes: ByteArray): DispatchResult {
        val media = request.source.mediaType.value
        if (!compatible(method, media, bytes)) return DispatchResult.Failure(failure(HermesV1FailureDetailCode.METHOD_SOURCE_MISMATCH, false))
        val sha = request.source.sourceSha256.value
        return when (method) {
            HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION -> when (media) {
                "text/plain" -> structured(request, structuredExtractor.extract(bytes, sha, media, request.source.originalFilename.value), method)
                "application/pdf" -> pdf(pdfExtractor.extract(bytes), method)
                else -> DispatchResult.Failure(failure(HermesV1FailureDetailCode.METHOD_SOURCE_MISMATCH, false))
            }
            HermesV1ProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION -> when (media) {
                "text/csv" -> csv(csvExtractor.extract(bytes), method)
                else -> structured(request, structuredExtractor.extract(bytes, sha, media, request.source.originalFilename.value), method)
            }
            HermesV1ProcessingMethod.STRUCTURED_SPREADSHEET_EXTRACTION -> structured(request, structuredExtractor.extract(bytes, sha, media, request.source.originalFilename.value), method)
            HermesV1ProcessingMethod.STRUCTURED_EMAIL_EXTRACTION -> when (media) {
                "message/rfc822" -> eml(emlExtractor.extract(bytes), method)
                else -> structured(request, structuredExtractor.extract(bytes, sha, media, request.source.originalFilename.value), method)
            }
        }
    }

    private fun compatible(method: HermesV1ProcessingMethod, media: String, bytes: ByteArray): Boolean = when (method) {
        HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION -> media == "text/plain" || (media == "application/pdf" && bytes.startsWithAscii("%PDF-"))
        HermesV1ProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION -> media == "text/csv" || media in setOf("application/rtf", "text/rtf", "application/x-rtf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document") && if (media == "application/vnd.openxmlformats-officedocument.wordprocessingml.document") bytes.isZipPackage("word/document.xml") else if (media == "application/msword") bytes.startsWithOle() else true
        HermesV1ProcessingMethod.STRUCTURED_SPREADSHEET_EXTRACTION -> (media == "application/vnd.ms-excel" && bytes.startsWithOle()) || (media == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" && bytes.isZipPackage("xl/workbook.xml"))
        HermesV1ProcessingMethod.STRUCTURED_EMAIL_EXTRACTION -> media == "message/rfc822" || (media in setOf("application/vnd.ms-outlook", "application/x-ole-storage") && bytes.startsWithOle())
    }

    private fun pdf(outcome: parker.core.interfaces.PdfStructuralExtractionOutcome, method: HermesV1ProcessingMethod): DispatchResult = when (outcome) {
        is parker.core.interfaces.PdfStructuralExtractionOutcome.Extracted -> DispatchResult.Success(
            HermesStructuredRepresentation.Text(outcome.result.documentText), outcome.result.producerIdentity.pluginIdentity, outcome.result.producerIdentity.pluginVersion, HermesV1RepresentationType.TEXT, emptyList(), method,
        )
        is parker.core.interfaces.PdfStructuralExtractionOutcome.RequiresTierB -> DispatchResult.Failure(failure(HermesV1FailureDetailCode.OCR_REQUIRED, false))
        is parker.core.interfaces.PdfStructuralExtractionOutcome.Unsupported -> DispatchResult.Failure(failure(HermesV1FailureDetailCode.UNSUPPORTED_MEDIA_TYPE, false))
        is parker.core.interfaces.PdfStructuralExtractionOutcome.Malformed -> DispatchResult.Failure(failure(HermesV1FailureDetailCode.MALFORMED_PROCESSOR_RESULT, false))
    }

    private fun structured(request: HermesProcessingServiceV1Request, outcome: parker.core.interfaces.StructuredDocumentExtractionOutcome, method: HermesV1ProcessingMethod): DispatchResult = when (outcome) {
        is parker.core.interfaces.StructuredDocumentExtractionOutcome.Extracted -> {
            val r = outcome.result
            val content = when (method) {
                HermesV1ProcessingMethod.STRUCTURED_SPREADSHEET_EXTRACTION -> spreadsheet(r.spreadsheetSheets)
                HermesV1ProcessingMethod.STRUCTURED_EMAIL_EXTRACTION -> email(r)
                else -> HermesStructuredRepresentation.Text(r.text)
            }
            DispatchResult.Success(content, r.parserIdentity, r.parserVersion, if (content is HermesStructuredRepresentation.Text) HermesV1RepresentationType.TEXT else HermesV1RepresentationType.STRUCTURED_DOCUMENT, issuesFrom(r.warnings), method)
        }
        is parker.core.interfaces.StructuredDocumentExtractionOutcome.CapabilityUnavailable -> DispatchResult.Failure(failure(HermesV1FailureDetailCode.PROCESSOR_UNAVAILABLE, false))
        is parker.core.interfaces.StructuredDocumentExtractionOutcome.Malformed -> DispatchResult.Failure(failure(HermesV1FailureDetailCode.MALFORMED_PROCESSOR_RESULT, false))
    }

    private fun csv(outcome: CsvStructuralExtractionOutcome, method: HermesV1ProcessingMethod): DispatchResult = when (outcome) {
        is CsvStructuralExtractionOutcome.Extracted -> {
            val r = outcome.result
            val cells = r.rows.flatMapIndexed { row, values -> values.mapIndexed { column, value -> HermesStructuredRepresentation.Spreadsheet.Cell("${columnName(column)}${row + 2}", value, null, value) } }
            val headers = r.headers.mapIndexed { column, value -> HermesStructuredRepresentation.Spreadsheet.Cell("${columnName(column)}1", value, null, value) }
            DispatchResult.Success(HermesStructuredRepresentation.Spreadsheet("CSV", listOf(HermesStructuredRepresentation.Spreadsheet.Sheet("CSV", headers + cells))), r.producerIdentity.pluginIdentity, r.producerIdentity.pluginVersion, HermesV1RepresentationType.STRUCTURED_DOCUMENT, issuesFrom(r.warnings), method)
        }
        is CsvStructuralExtractionOutcome.Malformed -> DispatchResult.Failure(failure(HermesV1FailureDetailCode.MALFORMED_PROCESSOR_RESULT, false))
    }

    private fun eml(outcome: parker.core.interfaces.EmlStructuralExtractionOutcome, method: HermesV1ProcessingMethod): DispatchResult = when (outcome) {
        is parker.core.interfaces.EmlStructuralExtractionOutcome.Extracted -> {
            val r = outcome.result
            DispatchResult.Success(HermesStructuredRepresentation.Email(r.from, r.to, r.cc, null, r.subject, r.rawDate, r.bodyAlternatives.joinToString("\n") { it.decodedText }, r.contentType, r.attachmentCandidates.map { HermesStructuredRepresentation.Email.Attachment(it.filename, it.declaredMimeType) }), r.producerIdentity.pluginIdentity, r.producerIdentity.pluginVersion, HermesV1RepresentationType.STRUCTURED_DOCUMENT, issuesFrom(r.warnings), method)
        }
        is parker.core.interfaces.EmlStructuralExtractionOutcome.Malformed -> DispatchResult.Failure(failure(HermesV1FailureDetailCode.MALFORMED_PROCESSOR_RESULT, false))
    }

    private fun mapRepresentation(result: DispatchResult.Success, request: HermesProcessingServiceV1Request): MappedRepresentation {
        val content = result.content
        val textSize = if (content is HermesStructuredRepresentation.Text) content.text.length else 0
        if (textSize > HermesProcessingServiceV1Limits.MAX_EXTRACTED_TEXT_CHARACTERS) throw LimitFailure(HermesV1FailureDetailCode.TEXT_TOO_LARGE)
        if (serializedSize(content) > HermesProcessingServiceV1Limits.MAX_STRUCTURED_REPRESENTATION_BYTES) throw LimitFailure(HermesV1FailureDetailCode.STRUCTURED_RESULT_TOO_LARGE)
        return MappedRepresentation(content, result.type, result.issues, HermesV1ProcessorIdentity(result.processorName, result.processorVersion))
    }

    private fun failureResponse(request: HermesProcessingServiceV1Request, failure: HermesV1Failure, methods: List<HermesV1ProcessingMethod> = request.requestedMethods, startedAt: Instant = Instant.now()): HermesProcessingServiceV1Response {
        val completed = maxOf(startedAt, Instant.now().truncatedTo(ChronoUnit.MILLIS))
        val safeMethods = methods.ifEmpty { listOf(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION) }
        val oldFailureKind = when (failure.detailCode) {
            HermesV1FailureDetailCode.UNSUPPORTED_MEDIA_TYPE, HermesV1FailureDetailCode.METHOD_SOURCE_MISMATCH, HermesV1FailureDetailCode.UNSUPPORTED_METHOD -> HermesProcessingFailureKind.UNSUPPORTED_FILE_FORMAT
            HermesV1FailureDetailCode.PROCESSOR_TIMEOUT -> HermesProcessingFailureKind.PROCESSING_TIMEOUT
            HermesV1FailureDetailCode.PROCESSOR_UNAVAILABLE -> HermesProcessingFailureKind.REQUIRED_PROCESSOR_UNAVAILABLE
            HermesV1FailureDetailCode.MALFORMED_PROCESSOR_RESULT -> HermesProcessingFailureKind.CORRUPT_SOURCE
            else -> HermesProcessingFailureKind.PROCESSOR_FAILURE
        }
        val established = HermesProcessingResult(request.source.sourceSha256.value, request.batchId.value, parker.core.interfaces.HermesProcessingStatus.FAILED, safeMethods.map { it.establishedMethod }.toSet(), failure = HermesProcessingFailure(oldFailureKind, failure.detail))
        return HermesProcessingServiceV1Response(request.protocolVersion, request.requestId, request.jobId, request.occurrenceId, request.batchId, request.source.sourceSha256, established, emptyList(), emptyList(), failure, HermesV1Provenance(request.source.sourceSha256, HermesV1ProcessorIdentity("hermes-native-processor-adapter", "1"), safeMethods.map { HermesV1MethodProvenance(it, startedAt, completed) }))
    }

    private fun failure(code: HermesV1FailureDetailCode, retryable: Boolean) = HermesV1Failure(code.category, code, retryable, "Hermes native processing failed")
    private fun issuesFrom(warnings: List<String>) = warnings.take(HermesProcessingServiceV1Limits.MAX_ISSUES).map { HermesV1Issue(parker.core.interfaces.HermesV1IssueCode.PROCESSING_QUALIFICATION, it.take(HermesProcessingServiceV1Limits.MAX_FAILURE_DETAIL_CHARACTERS)) }
    private fun configurationDigest(method: HermesV1ProcessingMethod, media: String) = HermesProcessingServiceV1ResponseSerializer.configurationDigest(
        mapOf("mediaType" to media, "method" to method.wireValue),
    )

    private fun spreadsheet(sheets: List<parker.core.interfaces.StructuredSpreadsheetSheet>) = HermesStructuredRepresentation.Spreadsheet("workbook", sheets.map { sheet -> HermesStructuredRepresentation.Spreadsheet.Sheet(sheet.name, sheet.cells.map { HermesStructuredRepresentation.Spreadsheet.Cell(it.coordinate, it.rawValue, it.formula, it.displayedValue) }) })
    private fun email(r: parker.core.interfaces.StructuredDocumentRepresentation) = HermesStructuredRepresentation.Email(r.sender, r.recipients.joinToString(","), r.cc.joinToString(","), r.bcc.joinToString(","), r.subject, r.timestamp, r.text, r.bodyFormat, r.attachments.map { HermesStructuredRepresentation.Email.Attachment(it.filename, it.mediaType) })

    private fun serializedSize(value: HermesStructuredRepresentation): Long = when (value) {
        is HermesStructuredRepresentation.Text -> jsonString(value.text).toByteArray(StandardCharsets.UTF_8).size.toLong()
        is HermesStructuredRepresentation.Spreadsheet -> value.sheets.sumOf { sheet -> jsonString(sheet.name).toByteArray(StandardCharsets.UTF_8).size.toLong() + sheet.cells.sumOf { cell -> jsonString(cell.coordinate).toByteArray(StandardCharsets.UTF_8).size.toLong() + (cell.value?.let { jsonString(it).toByteArray(StandardCharsets.UTF_8).size } ?: 0) + (cell.formula?.let { jsonString(it).toByteArray(StandardCharsets.UTF_8).size } ?: 0) + (cell.displayedValue?.let { jsonString(it).toByteArray(StandardCharsets.UTF_8).size } ?: 0) } }
        is HermesStructuredRepresentation.Email -> listOf(value.from, value.to, value.cc, value.bcc, value.subject, value.date, value.body, value.messageFormat).filterNotNull().sumOf { jsonString(it).toByteArray(StandardCharsets.UTF_8).size.toLong() } + value.attachments.size * 64L
    }

    private fun jsonString(value: String): String = buildString { append('"'); value.forEach { c -> when (c) { '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t"); else -> append(c) } }; append('"') }
    private fun columnName(index: Int): String { var value = index + 1; val result = StringBuilder(); while (value > 0) { val rem = (value - 1) % 26; result.append(('A'.code + rem).toChar()); value = (value - 1) / 26 }; return result.reverse().toString() }
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private fun ByteArray.startsWithAscii(value: String) = size >= value.length && value.toByteArray().contentEquals(copyOfRange(0, value.length))
    private fun ByteArray.startsWithOle() = size >= 8 && copyOfRange(0, 8).contentEquals(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte()))
    private fun ByteArray.isZipPackage(requiredEntry: String): Boolean = try { java.util.zip.ZipInputStream(inputStream()).use { stream -> generateSequence { stream.nextEntry }.any { it.name == requiredEntry } } } catch (_: Exception) { false }

    private data class MappedRepresentation(val content: HermesStructuredRepresentation, val type: HermesV1RepresentationType, val issues: List<HermesV1Issue>, val processor: HermesV1ProcessorIdentity)
    private sealed interface DispatchResult {
        data class Success(val content: HermesStructuredRepresentation, val processorName: String, val processorVersion: String, val type: HermesV1RepresentationType, val issues: List<HermesV1Issue>, val method: HermesV1ProcessingMethod) : DispatchResult
        data class Failure(val failure: HermesV1Failure) : DispatchResult
    }
    private class LimitFailure(val code: HermesV1FailureDetailCode) : RuntimeException()
}
