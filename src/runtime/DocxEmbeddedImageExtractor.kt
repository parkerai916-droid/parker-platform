package parker.core.runtime

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xwpf.usermodel.IBodyElement
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableCell

/** The bounded, document-order image inventory used by the image-only DOCX fallback. */
internal data class DocxEmbeddedImage(
    val ordinal: Int,
    val partName: String,
    val mediaType: String,
    val sha256: String,
    val relationshipId: String?,
    val bytes: ByteArray,
)

internal sealed interface DocxEmbeddedImageInspection {
    data class Ready(val readableNativeText: Boolean, val images: List<DocxEmbeddedImage>, val unsupportedMediaCount: Int) : DocxEmbeddedImageInspection
    data class Malformed(val reason: String) : DocxEmbeddedImageInspection
}

internal object DocxEmbeddedImageExtractor {
    fun inspect(sourceBytes: ByteArray): DocxEmbeddedImageInspection = try {
        OPCPackage.open(ByteArrayInputStream(sourceBytes)).use { packageFile ->
            XWPFDocument(packageFile).use { document ->
                val images = mutableListOf<DocxEmbeddedImage>()
                lateinit var collectTable: (XWPFTable) -> Unit
                fun collectParagraph(paragraph: XWPFParagraph) {
                    paragraph.runs.forEach { run ->
                        run.embeddedPictures.forEach { picture ->
                            val data = picture.pictureData
                            val mediaType = data.packagePart.contentType.lowercase()
                            val supported = mediaType == "image/jpeg" || mediaType == "image/png"
                            if (supported) {
                                val bytes = data.data
                                images += DocxEmbeddedImage(
                                    ordinal = images.size + 1,
                                    partName = data.packagePart.partName.name.removePrefix("/"),
                                    mediaType = mediaType,
                                    sha256 = sha256(bytes),
                                    relationshipId = picture.ctPicture.blipFill.blip.embed,
                                    bytes = bytes.copyOf(),
                                )
                            }
                        }
                    }
                }
                fun collectCell(cell: XWPFTableCell) {
                    cell.paragraphs.forEach { collectParagraph(it) }
                    cell.tables.forEach { collectTable(it) }
                }
                collectTable = { table: XWPFTable ->
                    table.rows.forEach { row -> row.tableCells.forEach { collectCell(it) } }
                }
                document.bodyElements.forEach { element: IBodyElement ->
                    when (element) {
                        is XWPFParagraph -> collectParagraph(element)
                        is XWPFTable -> collectTable(element)
                    }
                }
                val readable = document.paragraphs.any { it.text.trim().isNotEmpty() } ||
                    document.tables.any { table -> table.rows.any { row -> row.tableCells.any { it.text.trim().isNotEmpty() } } } ||
                    document.headerList.any { header -> header.paragraphs.any { it.text.trim().isNotEmpty() } } ||
                    document.footerList.any { footer -> footer.paragraphs.any { it.text.trim().isNotEmpty() } }
                val supportedPartNames = images.map { it.partName }.toSet()
                val unsupported = document.allEmbeddedParts.count { part ->
                    part.partName.name.startsWith("/word/media/") &&
                        part.partName.name.removePrefix("/") !in supportedPartNames
                }
                DocxEmbeddedImageInspection.Ready(readable, images, unsupported)
            }
        }
    } catch (e: Exception) {
        DocxEmbeddedImageInspection.Malformed("DOCX embedded-image inspection failed: ${e.message ?: e::class.simpleName}")
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
