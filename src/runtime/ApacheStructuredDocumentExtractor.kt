package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.Closeable
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import parker.core.interfaces.*

/** Bounded, deterministic structured extraction for non-PDF Tier A sources. */
class ApacheStructuredDocumentExtractor : StructuredDocumentExtractor {
    override suspend fun extract(sourceBytes: ByteArray, sourceSha256: String, mediaType: String, fileName: String?): StructuredDocumentExtractionOutcome {
        if (sourceBytes.size > MAX_SOURCE_BYTES) return StructuredDocumentExtractionOutcome.Malformed("structured source exceeds $MAX_SOURCE_BYTES bytes")
        return try {
            when (mediaType) {
                "text/plain" -> text(sourceBytes, sourceSha256, mediaType, StructuredDocumentKind.TXT)
                "application/rtf", "text/rtf" -> rtf(sourceBytes, sourceSha256, mediaType)
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel" -> workbook(sourceBytes, sourceSha256, mediaType)
                "application/msword" -> legacyWord(sourceBytes, sourceSha256, mediaType)
                "application/vnd.ms-outlook", "application/x-ole-storage" -> outlook(sourceBytes, sourceSha256, mediaType)
                else -> StructuredDocumentExtractionOutcome.CapabilityUnavailable("no structured extractor for $mediaType")
            }
        } catch (e: Exception) {
            StructuredDocumentExtractionOutcome.Malformed("structured parsing failed: ${e.message ?: e::class.simpleName}")
        }
    }

    private fun text(bytes: ByteArray, sha: String, media: String, kind: StructuredDocumentKind): StructuredDocumentExtractionOutcome {
        val decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
        val value = try { decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString() } catch (e: Exception) {
            return StructuredDocumentExtractionOutcome.Malformed("text source is not valid UTF-8: ${e.message}")
        }
        val lines = lineOffsets(value)
        return StructuredDocumentExtractionOutcome.Extracted(StructuredDocumentRepresentation(kind, sha, media, "UTF-8", value, lines = lines,
            parserIdentity = "parker-utf8-text", parserVersion = "1", transformations = listOf(DerivativeTransformation.CHARACTER_DECODING), extractionMethod = "DIRECT_TEXT_EXTRACTION",
            completenessState = DerivativeCompletenessState.ACCOUNTED_FOR))
    }

    private fun rtf(bytes: ByteArray, sha: String, media: String): StructuredDocumentExtractionOutcome {
        val source = try { bytes.toString(StandardCharsets.US_ASCII) } catch (e: Exception) { return StructuredDocumentExtractionOutcome.Malformed("RTF is not ASCII control text") }
        if (!source.startsWith("{\\rtf")) return StructuredDocumentExtractionOutcome.Malformed("RTF header is missing")
        val value = source.replace(Regex("\\\\'[0-9a-fA-F]{2}"), "")
            .replace(Regex("\\\\[a-zA-Z]+-?\\d*\\s?"), "")
            .replace("{", "").replace("}", "").replace("\\", "").trim()
        if (value.isBlank()) return StructuredDocumentExtractionOutcome.Malformed("RTF contains no readable text")
        return StructuredDocumentExtractionOutcome.Extracted(StructuredDocumentRepresentation(StructuredDocumentKind.RTF, sha, media, "ASCII", value,
            lines = lineOffsets(value), parserIdentity = "parker-bounded-rtf", parserVersion = "1", extractionMethod = "STRUCTURED_DOCUMENT_EXTRACTION",
            transformations = listOf(DerivativeTransformation.CHARACTER_DECODING, DerivativeTransformation.STRUCTURAL_PARSING),
            completenessState = DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS,
            warnings = listOf("RTF text and control structure are preserved only where safely exposed; layout and pagination are not claimed")))
    }

    private fun workbook(bytes: ByteArray, sha: String, media: String): StructuredDocumentExtractionOutcome {
        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val formatter = DataFormatter()
            val sheets = workbook.map { sheet ->
                val cells = mutableListOf<StructuredSpreadsheetCell>()
                for (row in sheet) for (cell in row) {
                    val formula = if (cell.cellType == CellType.FORMULA) cell.cellFormula else null
                    val raw = when (cell.cellType) {
                        CellType.STRING -> cell.stringCellValue
                        CellType.NUMERIC -> cell.numericCellValue.toString()
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        CellType.FORMULA -> cell.cellFormula
                        else -> null
                    }
                    cells += StructuredSpreadsheetCell(sheet.sheetName, cell.address.formatAsString(), raw, formatter.formatCellValue(cell), formula)
                }
                StructuredSpreadsheetSheet(sheet.sheetName, cells)
            }
            return StructuredDocumentExtractionOutcome.Extracted(StructuredDocumentRepresentation(
                if (media == "application/vnd.ms-excel") StructuredDocumentKind.XLS else StructuredDocumentKind.XLSX,
                sha, media, null, sheets.flatMap { it.cells }.joinToString("\n") { "${it.sheetName}!${it.coordinate}: ${it.displayedValue}" }, spreadsheetSheets = sheets,
                parserIdentity = "apache-poi", parserVersion = "5.5.1", transformations = listOf(DerivativeTransformation.STRUCTURAL_PARSING), extractionMethod = "STRUCTURED_SPREADSHEET_EXTRACTION",
                completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
            ))
        }
    }

    private fun legacyWord(bytes: ByteArray, sha: String, media: String): StructuredDocumentExtractionOutcome {
        val value = reflectText("org.apache.poi.hwpf.HWPFDocument", bytes, "getRange", "text")
            ?: return StructuredDocumentExtractionOutcome.CapabilityUnavailable("Apache POI HWPF is unavailable")
        if (value.isBlank()) return StructuredDocumentExtractionOutcome.Malformed("DOC contains no readable text")
        val blocks = value.split(Regex("[\\r\\n]+")).filter { it.isNotBlank() }.mapIndexed { index, text -> StructuredWordBlock(index, text) }
        return StructuredDocumentExtractionOutcome.Extracted(StructuredDocumentRepresentation(StructuredDocumentKind.DOC, sha, media, null, value,
            lines = lineOffsets(value), wordBlocks = blocks, parserIdentity = "apache-poi-hwpf", parserVersion = "5.5.1", transformations = listOf(DerivativeTransformation.STRUCTURAL_PARSING), extractionMethod = "STRUCTURED_DOCUMENT_EXTRACTION",
            completenessState = DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS,
            warnings = listOf("Legacy DOC pagination and visual layout are not claimed")))
    }

    private fun outlook(bytes: ByteArray, sha: String, media: String): StructuredDocumentExtractionOutcome {
        val clazz = runCatching { Class.forName("org.apache.poi.hsmf.MAPIMessage") }.getOrElse {
            return StructuredDocumentExtractionOutcome.CapabilityUnavailable("Apache POI HSMF is unavailable")
        }
        val message = runCatching { clazz.getConstructor(java.io.InputStream::class.java).newInstance(ByteArrayInputStream(bytes)) }.getOrElse {
            return StructuredDocumentExtractionOutcome.Malformed("MSG container is corrupt or encrypted: ${it.message}")
        }
        fun call(name: String): String? = runCatching { clazz.getMethod(name).invoke(message)?.toString() }.getOrNull()?.takeIf { it.isNotBlank() }
        val body = call("getTextBody") ?: return StructuredDocumentExtractionOutcome.Malformed("MSG has no readable text body")
        val sender = call("getDisplayFrom")
        val recipients = call("getDisplayTo")?.split(Regex("[;,]"))?.map(String::trim)?.filter(String::isNotBlank)
        val cc = call("getDisplayCC")?.split(Regex("[;,]"))?.map(String::trim)?.filter(String::isNotBlank)
        val bcc = call("getDisplayBCC")?.split(Regex("[;,]"))?.map(String::trim)?.filter(String::isNotBlank)
        val recipientGroups = recipientGroups(clazz, message)
        val attachments = runCatching {
            val chunks = clazz.getMethod("getAttachmentFiles").invoke(message) as? Array<*> ?: emptyArray<Any>()
            chunks.map { chunk ->
                val name = runCatching { chunk!!.javaClass.getMethod("getAttachLongFileName").invoke(chunk)?.toString() ?: chunk.javaClass.getMethod("getAttachFileName").invoke(chunk)?.toString() }.getOrNull()
                val mime = runCatching { val value = chunk!!.javaClass.getMethod("getAttachMimeTag").invoke(chunk) ?: return@runCatching null; value.javaClass.getMethod("getValue").invoke(value)?.toString() }.getOrNull()
                val bytes = runCatching { val value = chunk!!.javaClass.getMethod("getAttachData").invoke(chunk) ?: return@runCatching null; value.javaClass.getMethod("getValue").invoke(value) as? ByteArray }.getOrNull()
                StructuredEmailAttachment(name, mime, bytes?.let { MessageDigest.getInstance("SHA-256").digest(it).joinToString("") { b -> "%02x".format(b) } })
            }
        }.getOrDefault(emptyList())
        (message as? Closeable)?.close()
        return StructuredDocumentExtractionOutcome.Extracted(StructuredDocumentRepresentation(StructuredDocumentKind.MSG, sha, media, null, body,
            lines = lineOffsets(body), sender = sender,
            recipients = recipients ?: recipientGroups.first,
            cc = cc ?: recipientGroups.second,
            bcc = bcc ?: recipientGroups.third,
            subject = call("getSubject"), timestamp = call("getMessageDate"), bodyFormat = "text/plain", attachments = attachments,
            parserIdentity = "apache-poi-hsmf", parserVersion = "5.5.1", transformations = listOf(DerivativeTransformation.STRUCTURAL_PARSING), extractionMethod = "STRUCTURED_EMAIL_EXTRACTION",
            completenessState = DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS,
            warnings = listOf("MSG attachment enumeration remains separate from parent body text")))
    }

    private fun recipientGroups(clazz: Class<*>, message: Any): Triple<List<String>, List<String>, List<String>> {
        val to = mutableListOf<String>(); val cc = mutableListOf<String>(); val bcc = mutableListOf<String>()
        val chunks = runCatching { clazz.getMethod("getRecipientDetailsChunks").invoke(message) as? Array<*> ?: emptyArray<Any>() }.getOrDefault(emptyArray())
        chunks.forEach { chunk ->
            val address = runCatching { chunk!!.javaClass.getMethod("getRecipientEmailAddress").invoke(chunk)?.toString() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@forEach
            val type = runCatching {
                val properties = chunk!!.javaClass.getMethod("getProperties").invoke(chunk) as? Map<*, *> ?: return@runCatching null
                val entry = properties.entries.firstOrNull { it.key.toString().contains("RECIPIENT_TYPE") } ?: return@runCatching null
                val values = entry.value as? List<*> ?: return@runCatching null
                (values.firstOrNull()?.javaClass?.getMethod("getValue")?.invoke(values.first()) as? Number)?.toInt()
            }.getOrNull()
            when (type) { 2 -> cc += address; 3 -> bcc += address; else -> to += address }
        }
        return Triple(to, cc, bcc)
    }

    private fun reflectText(className: String, bytes: ByteArray, containerMethod: String, textMethod: String): String? {
        val clazz = Class.forName(className)
        val value = clazz.getConstructor(java.io.InputStream::class.java).newInstance(ByteArrayInputStream(bytes))
        return try { val range = clazz.getMethod(containerMethod).invoke(value); range.javaClass.getMethod(textMethod).invoke(range) as? String } finally { (value as? Closeable)?.close() }
    }

    private fun lineOffsets(value: String): List<StructuredTextLine> {
        if (value.isEmpty()) return emptyList()
        val result = mutableListOf<StructuredTextLine>(); var start = 0; var line = 1
        value.split("\n", limit = Int.MAX_VALUE).forEach { part -> val end = start + part.length; result += StructuredTextLine(line++, part.removeSuffix("\r"), start, end); start = end + 1 }
        return result
    }

    companion object { private const val MAX_SOURCE_BYTES = 64 * 1024 * 1024 }
}
