package parker.core.runtime

import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.zip.ZipInputStream
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.openxml4j.util.ZipSecureFile
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBrType
import parker.core.interfaces.*

class ApachePoiXwpfExtractor : DocxStructuralExtractor {
    override suspend fun extract(sourceBytes: ByteArray): DocxStructuralExtractionOutcome {
        if (sourceBytes.size > MAX_SOURCE_BYTES) return malformed("DOCX source exceeds the $MAX_SOURCE_BYTES-byte adapter limit")
        return try {
            val zipParts = preflight(sourceBytes)
            OPCPackage.open(ByteArrayInputStream(sourceBytes)).use { pkg ->
                XWPFDocument(pkg).use { document -> extract(document, pkg, zipParts) }
            }
        } catch (e: Exception) {
            malformed("DOCX/OOXML parsing failed: ${e.message ?: e::class.simpleName}")
        }
    }

    private fun extract(document: XWPFDocument, pkg: OPCPackage, zipParts: Map<String, Long>): DocxStructuralExtractionOutcome {
        val paragraphs = document.paragraphs.mapIndexed { index, p -> paragraph(index, p) }
        val tables = document.tables.mapIndexed { index, table ->
            DocxTable(index, table.styleID, table.rows.mapIndexed { rowIndex, row ->
                DocxTableRow(rowIndex, row.tableCells.mapIndexed { cellIndex, cell -> DocxTableCell(cellIndex, cell.text) })
            })
        }
        val headers = document.headerList.mapIndexed { index, header ->
            DocxHeaderFooter("HEADER", index, document.getRelationId(header), header.paragraphs.mapIndexed(::paragraph))
        }
        val footers = document.footerList.mapIndexed { index, footer ->
            DocxHeaderFooter("FOOTER", index, document.getRelationId(footer), footer.paragraphs.mapIndexed(::paragraph))
        }
        val runCount = paragraphs.sumOf { it.runs.size.toLong() } + headers.sumOf { h -> h.paragraphs.sumOf { it.runs.size.toLong() } } + footers.sumOf { f -> f.paragraphs.sumOf { it.runs.size.toLong() } }
        val cellCount = tables.sumOf { t -> t.rows.sumOf { it.cells.size.toLong() } }
        if (paragraphs.size.toLong() > MAX_PARAGRAPHS || runCount > MAX_RUNS || tables.size.toLong() > MAX_TABLES || cellCount > MAX_TABLE_CELLS) {
            return malformed("DOCX structure exceeds adapter collection limits")
        }
        val core = document.properties.coreProperties
        val extended = document.properties.extendedProperties
        val created = core.created
        val contentTypes = pkg.parts.associate { it.partName.name.removePrefix("/") to it.contentType }
        val parts = zipParts.entries.sortedBy { it.key }.map { (name, size) ->
            OoxmlPartInventoryEntry(name, contentTypes[name], size)
        }
        val warnings = listOf(
            "Apache POI XWPF exposes OOXML structure, not rendered pagination or page coordinates",
            "Core-property timestamps are exposed as parsed instants; original lexical XML remains only in the inventoried source package part",
            "Style-inherited list numbering is preserved as a style reference when direct numId/ilvl values are absent; rendered values are not inferred",
            "Nested-table and merged-cell visual semantics are not reconstructed beyond XWPF-exposed top-level ordered cell text",
            "Completeness is limited to inventoried parts and XWPF-exposed structures; no universal OOXML completeness is claimed",
        )
        val relationshipTypes = (pkg.relationships.map { it.relationshipType } +
            pkg.parts.filterNot { it.partName.name.endsWith(".rels") }.flatMap { part -> part.relationships.map { it.relationshipType } }).sorted()
        return DocxStructuralExtractionOutcome.Extracted(DocxStructuralResult(
            paragraphs, tables, headers, footers,
            DocxMetadata(core.title, core.creator, core.subject, created?.toInstant(), extended.application, extended.appVersion),
            parts,
            relationshipTypes.size,
            relationshipTypes,
            parts.filter { it.name.startsWith("word/media/") }.map { it.name },
            PRODUCER_IDENTITY, TRANSFORMATIONS, DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, warnings,
        ))
    }

    private fun paragraph(index: Int, p: org.apache.poi.xwpf.usermodel.XWPFParagraph): DocxParagraph = DocxParagraph(
        index, p.text, p.style, p.numID?.toString(), p.numIlvl?.toInt(),
        p.runs.mapIndexed { runIndex, run -> DocxRun(runIndex, run.text(), run.isBold, run.isItalic) },
        p.runs.sumOf { run -> run.ctr.brList.count { it.type == STBrType.PAGE } },
    )

    private fun preflight(bytes: ByteArray): Map<String, Long> {
        var parts = 0; var total = 0L
        val inventory = linkedMapOf<String, Long>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            val buffer = ByteArray(8192)
            while (true) {
                val entry = zip.nextEntry ?: break
                parts++
                if (parts > MAX_PARTS) throw IllegalArgumentException("OOXML part count exceeds $MAX_PARTS")
                var partSize = 0L
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    partSize += count; total += count
                    if (partSize > MAX_PART_BYTES) throw IllegalArgumentException("OOXML part '${entry.name}' exceeds $MAX_PART_BYTES inflated bytes")
                    if (total > MAX_TOTAL_INFLATED_BYTES) throw IllegalArgumentException("OOXML package exceeds $MAX_TOTAL_INFLATED_BYTES total inflated bytes")
                }
                inventory[entry.name] = partSize
            }
        }
        if (parts == 0) throw IllegalArgumentException("DOCX input is not a readable ZIP package")
        return inventory
    }

    private fun malformed(reason: String) = DocxStructuralExtractionOutcome.Malformed(reason)

    companion object {
        const val PRODUCT_IDENTITY = "Apache POI"
        const val PRODUCT_VERSION = "5.5.1"
        const val ADAPTER_IDENTITY = "parker.apache-poi-xwpf"
        const val ADAPTER_VERSION = "1"
        const val CONFIGURATION_IDENTITY = "xwpf-read-only-ordered-structure-bounded-v1"
        const val MAX_SOURCE_BYTES = 32 * 1024 * 1024
        const val MAX_PARTS = 2000
        const val MAX_PART_BYTES = 32 * 1024 * 1024
        const val MAX_TOTAL_INFLATED_BYTES = 128 * 1024 * 1024
        const val MAX_PARAGRAPHS = 100000L
        const val MAX_RUNS = 500000L
        const val MAX_TABLES = 10000L
        const val MAX_TABLE_CELLS = 500000L
        val PRODUCER_IDENTITY = DerivativeProducerIdentity(PRODUCT_IDENTITY, PRODUCT_VERSION, CONFIGURATION_IDENTITY, ADAPTER_IDENTITY, ADAPTER_VERSION)
        val TRANSFORMATIONS = listOf(DerivativeTransformation.DECOMPRESSION, DerivativeTransformation.CHARACTER_DECODING, DerivativeTransformation.STRUCTURAL_PARSING, DerivativeTransformation.METADATA_INTERPRETATION)
        val POI_MIN_INFLATE_RATIO: Double get() = ZipSecureFile.getMinInflateRatio()
    }
}
