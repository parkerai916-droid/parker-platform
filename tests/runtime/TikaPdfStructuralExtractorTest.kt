package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import parker.core.interfaces.*

class TikaPdfStructuralExtractorTest {
    @Test fun `fixture 01 preserves searchable literal controls without OCR or normalization`() = runTest {
        val bytes = Files.readAllBytes(FIXTURE_01); val original = bytes.copyOf()
        assertEquals(75717, bytes.size); assertEquals(HASH_01, sha256(bytes))
        val result = extracted(bytes)
        CONTROLS_01.forEach { assertTrue(it in result.documentText, "missing literal control: $it") }
        assertTrue("Whitespace control: Alpha     Beta  Gamma" in result.documentText)
        assertTrue("PAGE-2-ONLY: KŌWHAI-SECOND-PAGE-882" in result.documentText)
        assertEquals(2, result.pageCount)
        assertFalse(result.pageTextAssociationAvailable)
        assertTrue(result.embeddedResources.isEmpty())
        assertEquals(ApacheTikaIdentity, result.producerIdentity)
        assertFalse(DerivativeTransformation.OCR in result.transformationHistory)
        assertContentEquals(original, bytes)
    }

    @Test fun `fixture 02 retains all text controls and observed order without fabricated table`() = runTest {
        val result = extracted(Files.readAllBytes(FIXTURE_02))
        CONTROLS_02.forEach { assertTrue(it in result.documentText, "missing literal control: $it") }
        assertEquals(1, result.pageCount)
        assertTrue(result.documentText.indexOf("L1 Alpha one") < result.documentText.indexOf("TABLE-END"))
        assertTrue(result.warnings.any { "no column, table, or layout reconstruction" in it })
        assertEquals(DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, result.completenessState)
    }

    @Test fun `fixture 03 requires Tier B and never returns textual extraction`() = runTest {
        val bytes = Files.readAllBytes(FIXTURE_03); val original = bytes.copyOf()
        val outcome = assertIs<PdfStructuralExtractionOutcome.RequiresTierB>(TikaPdfStructuralExtractor().extract(bytes))
        assertEquals(1, outcome.pageCount); assertTrue("OCR" in outcome.reason)
        assertContentEquals(original, bytes)
    }

    @Test fun `random truncated and oversized inputs cannot produce extraction`() = runTest {
        assertTrue(TikaPdfStructuralExtractor().extract("not pdf".toByteArray()) !is PdfStructuralExtractionOutcome.Extracted)
        assertTrue(TikaPdfStructuralExtractor().extract("%PDF-1.4\ntruncated".toByteArray()) !is PdfStructuralExtractionOutcome.Extracted)
        assertIs<PdfStructuralExtractionOutcome.Malformed>(TikaPdfStructuralExtractor().extract(ByteArray(TikaPdfStructuralExtractor.MAX_SOURCE_BYTES + 1)))
    }

    @Test fun `one meaningful searchable character is not misclassified as raster only`() = runTest {
        val bytes = searchablePdf("X")
        val result = extracted(bytes)
        assertTrue("X" in result.documentText)
    }

    @Test fun `password protected PDF cannot become false successful Tier A extraction`() = runTest {
        val document = PDDocument(); document.addPage(PDPage())
        document.protect(StandardProtectionPolicy("owner-secret", "user-secret", AccessPermission()).apply { encryptionKeyLength = 128 })
        val output = java.io.ByteArrayOutputStream(); document.use { it.save(output) }
        assertTrue(TikaPdfStructuralExtractor().extract(output.toByteArray()) !is PdfStructuralExtractionOutcome.Extracted)
    }

    private fun searchablePdf(text: String): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        PDDocument().use { document ->
            val page = PDPage(); document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                stream.beginText(); stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                stream.newLineAtOffset(72f, 720f); stream.showText(text); stream.endText()
            }
            document.save(output)
        }
        return output.toByteArray()
    }

    private suspend fun extracted(bytes: ByteArray): PdfStructuralResult {
        val outcome = TikaPdfStructuralExtractor().extract(bytes)
        return assertIs<PdfStructuralExtractionOutcome.Extracted>(outcome, outcome.toString()).result
    }
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        val FIXTURE_01 = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/01-searchable-simple.pdf")
        val FIXTURE_02 = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/02-multicolumn-complex.pdf")
        val FIXTURE_03 = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/03-scanned.pdf")
        const val HASH_01 = "7320a9921a3af2b89c56360fd7eea041ed4d2a2146bc4bd540363232e7b8a3db"
        val ApacheTikaIdentity = TikaPdfStructuralExtractor.PRODUCER_IDENTITY
        val CONTROLS_01 = listOf("PARKER-FIXTURE-2026-001", "The quick bronze fox costs NZ$1,234.50 — GST included.", "Case-ID: PF-007/26", "Account: 00104567", "Māori", "café", "naïve", "€42.00", "\"quoted text\"", "'single quotes'", "& < > / \\ ( ) [ ] { }")
        val CONTROLS_02 = listOf("PARKER-FIXTURE-2026-002", "L1 Alpha one", "L2 Alpha two", "L3 Alpha three", "R1 Bravo one", "R2 Bravo two", "R3 Bravo three", "Alpha", "2", "\$12.50", "Bravo", "10", "\$1,004.07", "Charlie", "001", "\$0.09", "TABLE-END")
    }
}
