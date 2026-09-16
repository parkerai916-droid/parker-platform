package parker.core.interfaces

/** Common governed representation for deterministic text, legacy/OOXML Office, RTF, and Outlook sources. */
enum class StructuredDocumentKind { TXT, DOC, DOCX, XLS, XLSX, MSG, RTF }

data class StructuredTextLine(val lineNumber: Int, val text: String, val startOffset: Int, val endOffset: Int)
data class StructuredWordBlock(val order: Int, val text: String, val kind: String = "PARAGRAPH")
data class StructuredSpreadsheetCell(
    val sheetName: String,
    val coordinate: String,
    val rawValue: String?,
    val displayedValue: String?,
    val formula: String? = null,
    val mergedRange: String? = null,
)
data class StructuredSpreadsheetSheet(val name: String, val cells: List<StructuredSpreadsheetCell>)
data class StructuredEmailAttachment(
    val filename: String?,
    val mediaType: String?,
    val sha256: String?,
    val childRelationship: String = "CANDIDATE_SEPARATE_SOURCE",
)

data class StructuredDocumentRepresentation(
    val kind: StructuredDocumentKind,
    val sourceSha256: String,
    val originalMediaType: String,
    val encoding: String?,
    val text: String,
    val lines: List<StructuredTextLine> = emptyList(),
    val wordBlocks: List<StructuredWordBlock> = emptyList(),
    val spreadsheetSheets: List<StructuredSpreadsheetSheet> = emptyList(),
    val sender: String? = null,
    val recipients: List<String> = emptyList(),
    val cc: List<String> = emptyList(),
    val bcc: List<String> = emptyList(),
    val subject: String? = null,
    val timestamp: String? = null,
    val bodyFormat: String? = null,
    val attachments: List<StructuredEmailAttachment> = emptyList(),
    val parserIdentity: String,
    val parserVersion: String,
    val transformations: List<DerivativeTransformation>,
    val completenessState: DerivativeCompletenessState,
    val warnings: List<String> = emptyList(),
    /** Parker's native governed-derivative extraction method, not Hermes preflight's method list. */
    val extractionMethod: String? = null,
) {
    init {
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(originalMediaType.isNotBlank() && parserIdentity.isNotBlank() && parserVersion.isNotBlank())
        require(text.toByteArray(Charsets.UTF_8).size <= 20 * 1024 * 1024)
        require(lines.all { it.startOffset >= 0 && it.endOffset >= it.startOffset && it.endOffset <= text.length })
        require(spreadsheetSheets.flatMap { it.cells }.all { it.sheetName.isNotBlank() && it.coordinate.isNotBlank() })
        require(warnings.none { it.isBlank() })
    }
}
