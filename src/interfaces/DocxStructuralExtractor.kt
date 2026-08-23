package parker.core.interfaces

import java.time.Instant

data class DocxRun(val order: Int, val text: String, val bold: Boolean, val italic: Boolean)
data class DocxParagraph(
    val order: Int,
    val text: String,
    val styleId: String?,
    val numberingId: String?,
    val numberingLevel: Int?,
    val runs: List<DocxRun>,
    val hardPageBreakCount: Int,
)
data class DocxTableCell(val order: Int, val text: String)
data class DocxTableRow(val order: Int, val cells: List<DocxTableCell>)
data class DocxTable(val order: Int, val styleId: String?, val rows: List<DocxTableRow>)
data class DocxHeaderFooter(val kind: String, val order: Int, val relationshipId: String?, val paragraphs: List<DocxParagraph>)
data class DocxMetadata(
    val title: String?, val author: String?, val subject: String?,
    val parsedCreated: Instant?,
    val application: String?, val applicationVersion: String?,
)
data class OoxmlPartInventoryEntry(val name: String, val contentType: String?, val uncompressedBytes: Long)

data class DocxStructuralResult(
    val paragraphs: List<DocxParagraph>,
    val tables: List<DocxTable>,
    val headers: List<DocxHeaderFooter>,
    val footers: List<DocxHeaderFooter>,
    val metadata: DocxMetadata,
    val parts: List<OoxmlPartInventoryEntry>,
    val relationshipCount: Int,
    val relationshipTypes: List<String>,
    val mediaPartNames: List<String>,
    val producerIdentity: DerivativeProducerIdentity,
    val transformationHistory: List<DerivativeTransformation>,
    val completenessState: DerivativeCompletenessState,
    val warnings: List<String>,
)

sealed class DocxStructuralExtractionOutcome {
    data class Extracted(val result: DocxStructuralResult) : DocxStructuralExtractionOutcome()
    data class Malformed(val reason: String) : DocxStructuralExtractionOutcome() {
        init { require(reason.isNotBlank()) }
    }
}

fun interface DocxStructuralExtractor {
    suspend fun extract(sourceBytes: ByteArray): DocxStructuralExtractionOutcome
}
