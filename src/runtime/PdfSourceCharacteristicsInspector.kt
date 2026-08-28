package parker.core.runtime

import java.io.IOException
import org.apache.pdfbox.Loader
import org.apache.pdfbox.contentstream.operator.Operator
import org.apache.pdfbox.pdfparser.PDFStreamParser
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import parker.core.interfaces.AcquisitionCharacteristicState
import parker.core.interfaces.AcquisitionPageCount

/**
 * Read-only inspection of bounded PDF structural facts. This does not decode, return, or persist
 * document text and cannot create an acquisition derivative.
 */
internal class PdfSourceCharacteristicsInspector {
    fun inspect(authoritativeSource: AuthoritativeAcquisitionInput): PdfSourceCharacteristicsInspection {
        if (authoritativeSource.mediaType?.lowercase() != PDF_MEDIA_TYPE) {
            return PdfSourceCharacteristicsInspection.Unsupported
        }
        return try {
            Loader.loadPDF(authoritativeSource.bytes()).use { document ->
                if (document.isEncrypted) return PdfSourceCharacteristicsInspection.Indeterminate("PDF_ENCRYPTED")
                val pageCount = document.numberOfPages
                if (pageCount <= 0) return PdfSourceCharacteristicsInspection.Indeterminate("PDF_HAS_NO_PAGES")
                val textBearingPages = document.pages.count { page ->
                    PDFStreamParser(page).parse().filterIsInstance<Operator>().any { it.name in TEXT_SHOWING_OPERATORS }
                }
                val imageOnlyPages = pageCount - textBearingPages
                PdfSourceCharacteristicsInspection.Established(
                    pageCount = AcquisitionPageCount.Known(pageCount),
                    nativeSearchableText = state(textBearingPages > 0),
                    imageOnlyOrScanned = state(textBearingPages == 0),
                    mixedTextAndImage = state(textBearingPages > 0 && imageOnlyPages > 0),
                    mechanismIdentity = MECHANISM_IDENTITY,
                    sourceSha256 = authoritativeSource.sha256,
                )
            }
        } catch (_: InvalidPasswordException) {
            PdfSourceCharacteristicsInspection.Indeterminate("PDF_ENCRYPTED_OR_PASSWORD_REQUIRED")
        } catch (_: IOException) {
            PdfSourceCharacteristicsInspection.Indeterminate("PDF_PARSE_FAILED")
        } catch (_: RuntimeException) {
            PdfSourceCharacteristicsInspection.Indeterminate("PDF_PARSE_FAILED")
        }
    }

    private fun state(value: Boolean) =
        if (value) AcquisitionCharacteristicState.PRESENT else AcquisitionCharacteristicState.ABSENT

    companion object {
        const val MECHANISM_IDENTITY = "parker.pdf-structural-characteristics.v1"
        private const val PDF_MEDIA_TYPE = "application/pdf"
        private val TEXT_SHOWING_OPERATORS = setOf("Tj", "TJ", "'", "\"")
    }
}

internal sealed interface PdfSourceCharacteristicsInspection {
    data class Established(
        val pageCount: AcquisitionPageCount.Known,
        val nativeSearchableText: AcquisitionCharacteristicState,
        val imageOnlyOrScanned: AcquisitionCharacteristicState,
        val mixedTextAndImage: AcquisitionCharacteristicState,
        val mechanismIdentity: String,
        val sourceSha256: String,
    ) : PdfSourceCharacteristicsInspection
    data class Indeterminate(val reason: String) : PdfSourceCharacteristicsInspection
    data object Unsupported : PdfSourceCharacteristicsInspection
}
