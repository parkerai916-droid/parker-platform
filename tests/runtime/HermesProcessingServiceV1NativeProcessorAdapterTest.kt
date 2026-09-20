package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.CsvStructuralExtractionOutcome
import parker.core.interfaces.CsvStructuralExtractor
import parker.core.interfaces.CsvStructuralResult
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.DerivativeProducerIdentity
import parker.core.interfaces.EmlStructuralExtractionOutcome
import parker.core.interfaces.EmlStructuralExtractor
import parker.core.interfaces.EmlStructuralResult
import parker.core.interfaces.HermesProcessingServiceV1Request
import parker.core.interfaces.HermesProcessingServiceV1Source
import parker.core.interfaces.HermesStructuredRepresentation
import parker.core.interfaces.HermesV1BatchId
import parker.core.interfaces.HermesV1FailureDetailCode
import parker.core.interfaces.HermesV1JobId
import parker.core.interfaces.HermesV1MediaType
import parker.core.interfaces.HermesV1OccurrenceId
import parker.core.interfaces.HermesV1OriginalFilename
import parker.core.interfaces.HermesV1ProcessingMethod
import parker.core.interfaces.HermesV1ProcessingPrincipal
import parker.core.interfaces.HermesV1ProtocolVersion
import parker.core.interfaces.HermesV1RequestId
import parker.core.interfaces.HermesV1Sha256
import parker.core.interfaces.HermesV1SourceReference
import parker.core.interfaces.HermesV1SourceSize
import parker.core.interfaces.PdfStructuralExtractionOutcome
import parker.core.interfaces.PdfStructuralExtractor
import parker.core.interfaces.PdfStructuralResult
import parker.core.interfaces.StructuredDocumentExtractionOutcome
import parker.core.interfaces.StructuredDocumentExtractor
import parker.core.interfaces.StructuredDocumentKind
import parker.core.interfaces.StructuredDocumentRepresentation

class HermesProcessingServiceV1NativeProcessorAdapterTest {
    @TempDir
    lateinit var temp: Path

    @Test
    fun `approved native and structured formats dispatch through existing extractor seams`() = runBlocking {
        val adapter = adapter()
        val cases = listOf(
            HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION to source("hello", "text/plain", "note.txt"),
            HermesV1ProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION to source("PK-docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "note.docx", zipEntry = "word/document.xml"),
            HermesV1ProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION to source("a,b\n1,2\n", "text/csv", "table.csv"),
            HermesV1ProcessingMethod.STRUCTURED_SPREADSHEET_EXTRACTION to source("PK-xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "table.xlsx", zipEntry = "xl/workbook.xml"),
            HermesV1ProcessingMethod.STRUCTURED_EMAIL_EXTRACTION to source("From: sender@example.test\nSubject: hello\n\nBody", "message/rfc822", "message.eml"),
            HermesV1ProcessingMethod.STRUCTURED_EMAIL_EXTRACTION to source(OLE, "application/vnd.ms-outlook", "message.msg"),
            HermesV1ProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION to source("{\\rtf1 hello}", "application/rtf", "note.rtf"),
            HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION to source("%PDF-native", "application/pdf", "document.pdf"),
        )
        cases.forEach { (method, verified) ->
            val raw = adapter.process(HermesV1ProcessingPrincipal.PARKER_PROCESSING, verified)
            assertTrue(raw is HermesV1NativeProcessingOutcome.Produced, "failed method=$method filename=${verified.request.source.originalFilename.value} result=$raw")
            val outcome = raw as HermesV1NativeProcessingOutcome.Produced
            assertEquals("PASS", outcome.response.establishedResult.status.name)
            assertEquals(method, outcome.response.representations.single().method)
            assertTrue(outcome.response.provenance.operations.single().configurationDigest != null)
            assertTrue(outcome.response.failure == null)
        }
    }

    @Test
    fun `media type and magic bytes control dispatch rather than filename extension`() = runBlocking {
        val adapter = adapter()
        val renamed = source("plain text", "text/plain", "looks-like.pdf")
        assertIs<HermesV1NativeProcessingOutcome.Produced>(adapter.process(HermesV1ProcessingPrincipal.PARKER_PROCESSING, renamed))

        val mismatch = source("not a pdf", "application/pdf", "document.pdf")
        val rejected = assertIs<HermesV1NativeProcessingOutcome.Rejected>(adapter.process(HermesV1ProcessingPrincipal.PARKER_PROCESSING, mismatch))
        assertEquals(HermesV1FailureDetailCode.METHOD_SOURCE_MISMATCH, rejected.response.failure?.detailCode)
    }

    @Test
    fun `ocr transcription images audio and scanned pdf remain unavailable`() = runBlocking {
        val adapter = adapter()
        listOf(
            source("image", "image/jpeg", "photo.jpg"),
            source("image", "image/png", "photo.png"),
            source("image", "image/tiff", "photo.tiff"),
            source("audio", "audio/wav", "audio.wav"),
            source("audio", "audio/mp4", "audio.m4a"),
        ).forEach { source ->
            val rejected = assertIs<HermesV1NativeProcessingOutcome.Rejected>(adapter.process(HermesV1ProcessingPrincipal.PARKER_PROCESSING, source))
            assertEquals(HermesV1FailureDetailCode.METHOD_SOURCE_MISMATCH, rejected.response.failure?.detailCode)
        }
        assertFailsWith<IllegalArgumentException> { HermesV1ProcessingMethod.fromWireValue("OCR") }
        assertFailsWith<IllegalArgumentException> { HermesV1ProcessingMethod.fromWireValue("TRANSCRIPTION") }
        val scannedPdf = source("%PDF-scanned", "application/pdf", "scan.pdf")
        val rejected = assertIs<HermesV1NativeProcessingOutcome.Rejected>(adapter(processorReturnsTierB = true).process(HermesV1ProcessingPrincipal.PARKER_PROCESSING, scannedPdf))
        assertEquals(HermesV1FailureDetailCode.UNSUPPORTED_MEDIA_TYPE, rejected.response.failure?.detailCode)
    }

    @Test
    fun `only verified source is accepted and no arbitrary path API exists`() {
        assertTrue(HermesProcessingServiceV1NativeProcessorAdapter::class.java.methods.none { it.parameterTypes.any { type -> type == Path::class.java } })
        assertTrue(HermesProcessingServiceV1NativeProcessorAdapter::class.java.methods.none { it.name.contains("ocr", ignoreCase = true) || it.name.contains("transcrib", ignoreCase = true) })
    }

    @Test
    fun `text and structured limits fail closed without truncation`() = runBlocking {
        val largeText = source("x", "text/plain", "large.txt")
        val textAdapter = adapter(text = "x".repeat(4_000_001))
        val textRejected = assertIs<HermesV1NativeProcessingOutcome.Rejected>(textAdapter.process(HermesV1ProcessingPrincipal.PARKER_PROCESSING, largeText))
        assertEquals(HermesV1FailureDetailCode.TEXT_TOO_LARGE, textRejected.response.failure?.detailCode)

        val largeCell = "x".repeat(8 * 1024 * 1024 + 1)
        val spreadsheet = StructuredDocumentRepresentation(
            StructuredDocumentKind.XLSX, sha256(XLSX), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", null, "summary",
            spreadsheetSheets = listOf(parker.core.interfaces.StructuredSpreadsheetSheet("Sheet1", listOf(parker.core.interfaces.StructuredSpreadsheetCell("Sheet1", "A1", largeCell, largeCell)))),
            parserIdentity = "test", parserVersion = "1", transformations = emptyList(), completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
        )
        val structuredRejected = assertIs<HermesV1NativeProcessingOutcome.Rejected>(adapter(structured = spreadsheet).process(HermesV1ProcessingPrincipal.PARKER_PROCESSING, source("ignored", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "large.xlsx", zipEntry = "xl/workbook.xml")))
        assertEquals(HermesV1FailureDetailCode.STRUCTURED_RESULT_TOO_LARGE, structuredRejected.response.failure?.detailCode)
    }

    @Test
    fun `no confidence or Parker admission state is fabricated`() = runBlocking {
        val response = assertIs<HermesV1NativeProcessingOutcome.Produced>(adapter().process(HermesV1ProcessingPrincipal.PARKER_PROCESSING, source("hello", "text/plain", "note.txt"))).response
        assertEquals("PASS", response.establishedResult.status.name)
        assertTrue(response.establishedResult.proposedEvidenceArtifactId == null)
        assertTrue(response.establishedResult.reviewConfidenceThreshold == null)
        assertTrue(response.provenance.processor.name.isNotBlank())
        assertTrue(response.provenance.processor.version.isNotBlank())
    }

    private fun adapter(
        text: String = "native text",
        structured: StructuredDocumentRepresentation? = null,
        processorReturnsTierB: Boolean = false,
    ) = HermesProcessingServiceV1NativeProcessorAdapter(
        structuredExtractor = StubStructured(structured ?: document(text)),
        csvExtractor = StubCsv(),
        pdfExtractor = StubPdf(processorReturnsTierB),
        emlExtractor = StubEml(),
    )

    private fun source(text: String, media: String, filename: String, zipEntry: String? = null): HermesV1VerifiedSource {
        val bytes = if (zipEntry == null) text.toByteArray() else zip(zipEntry)
        return source(bytes, media, filename)
    }

    private fun source(bytes: ByteArray, media: String, filename: String): HermesV1VerifiedSource {
        val path = Files.createTempFile(temp, "verified-", ".bin")
        Files.write(path, bytes)
        val digest = sha256(bytes)
        val request = HermesProcessingServiceV1Request(
            HermesV1ProtocolVersion.CURRENT, HermesV1RequestId("request-${filename.hashCode()}"), HermesV1JobId("job-1"), HermesV1OccurrenceId("occurrence-1"), HermesV1BatchId("batch-1"),
            HermesProcessingServiceV1Source(HermesV1SourceReference("source-${filename.hashCode()}"), HermesV1Sha256(digest), HermesV1SourceSize(bytes.size.toLong()), HermesV1OriginalFilename(filename), HermesV1MediaType(media)),
            listOf(methodFor(media)),
        )
        return HermesV1VerifiedSource(request, request.source.sourceSha256, request.source.sourceSha256, bytes.size.toLong(), HermesV1SourceHandle(path))
    }

    private fun methodFor(media: String) = when {
        media == "text/plain" || media == "application/pdf" -> HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION
        media == "text/csv" || media == "application/rtf" || media == "text/rtf" || media == "application/msword" || media.contains("wordprocessing") -> HermesV1ProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION
        media.contains("spreadsheet") || media == "application/vnd.ms-excel" -> HermesV1ProcessingMethod.STRUCTURED_SPREADSHEET_EXTRACTION
        media == "message/rfc822" || media.contains("outlook") -> HermesV1ProcessingMethod.STRUCTURED_EMAIL_EXTRACTION
        else -> HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION
    }

    private fun document(text: String) = StructuredDocumentRepresentation(StructuredDocumentKind.TXT, sha256(text.toByteArray()), "text/plain", "UTF-8", text, parserIdentity = "test-native", parserVersion = "1", transformations = emptyList(), completenessState = DerivativeCompletenessState.ACCOUNTED_FOR)
    private fun zip(entry: String): ByteArray = ByteArrayOutputStream().also { output -> ZipOutputStream(output).use { zip -> zip.putNextEntry(ZipEntry(entry)); zip.write(byteArrayOf(1)); zip.closeEntry() } }.toByteArray()
    private fun sha256(bytes: ByteArray) = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private class StubStructured(private val result: StructuredDocumentRepresentation) : StructuredDocumentExtractor {
        override suspend fun extract(sourceBytes: ByteArray, sourceSha256: String, mediaType: String, fileName: String?): StructuredDocumentExtractionOutcome {
            val spreadsheet = if (mediaType.contains("spreadsheet") && result.spreadsheetSheets.isEmpty()) {
                listOf(parker.core.interfaces.StructuredSpreadsheetSheet("Sheet1", listOf(parker.core.interfaces.StructuredSpreadsheetCell("Sheet1", "A1", "value", "value"))))
            } else result.spreadsheetSheets
            return StructuredDocumentExtractionOutcome.Extracted(result.copy(sourceSha256 = sourceSha256, originalMediaType = mediaType, spreadsheetSheets = spreadsheet))
        }
    }
    private class StubCsv : CsvStructuralExtractor {
        override suspend fun extract(sourceBytes: ByteArray) = CsvStructuralExtractionOutcome.Extracted(CsvStructuralResult(listOf("header"), listOf(listOf("value")), ',', '"', "LF", PRODUCER, emptyList(), DerivativeCompletenessState.ACCOUNTED_FOR, emptyList()))
    }
    private class StubPdf(private val tierB: Boolean) : PdfStructuralExtractor {
        override suspend fun extract(sourceBytes: ByteArray) = if (tierB) PdfStructuralExtractionOutcome.RequiresTierB(null, "OCR required") else PdfStructuralExtractionOutcome.Extracted(PdfStructuralResult("pdf text", 1, true, emptyList(), emptyList(), PRODUCER, emptyList(), DerivativeCompletenessState.ACCOUNTED_FOR, emptyList()))
    }
    private class StubEml : EmlStructuralExtractor {
        override suspend fun extract(sourceBytes: ByteArray) = EmlStructuralExtractionOutcome.Extracted(EmlStructuralResult(emptyList(), "from", "to", null, null, null, "subject", null, null, null, emptyList(), emptyList(), emptyList(), PRODUCER, emptyList(), DerivativeCompletenessState.ACCOUNTED_FOR, emptyList()))
    }

    private companion object {
        val PRODUCER = DerivativeProducerIdentity("test-processor", "1", "test")
        val OLE = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte())
        val XLSX = zipBytes("xl/workbook.xml")
        fun zipBytes(entry: String): ByteArray = ByteArrayOutputStream().also { output -> ZipOutputStream(output).use { zip -> zip.putNextEntry(ZipEntry(entry)); zip.write(byteArrayOf(1)); zip.closeEntry() } }.toByteArray()
    }
}
