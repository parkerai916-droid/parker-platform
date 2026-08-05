package parker.core.runtime

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.core.interfaces.ExtractionOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Evidence Processing (Searchable PDF), Implementation Unit 2 ("Apache
 * Tika Adapter"). The real [TikaEvidenceExtractor] against a test corpus,
 * per the Implementation Plan's own Unit 2 test list.
 *
 * One small synthetic searchable PDF is committed to the repository
 * (`tests/fixtures/synthetic-searchable.pdf`) for deterministic CI. The
 * real-world corpus (Uber-style receipt, WelTec/ERA document, one scanned/
 * image-only PDF) lives in a gitignored local fixtures directory
 * (`tests/fixtures/local/`) and is never committed -- each such test
 * **skips gracefully** via [assumeTrue] when its file is absent, so CI
 * stays green while a local run with the real files present exercises
 * them fully (Implementation Plan Unit 2; Unit 7).
 *
 * The "unsupported" and one "truncated bytes" case are each exercised with
 * inline, synthetic byte content rather than an external fixture, so this
 * suite's CI coverage of those two branches does not depend on any
 * gitignored file being present locally.
 */
class TikaEvidenceExtractorTest {

    private val localFixturesDir = File("tests/fixtures/local")
    private fun localFixture(name: String) = File(localFixturesDir, name)

    private fun syntheticSearchablePdfBytes(): ByteArray {
        val file = File("tests/fixtures/synthetic-searchable.pdf")
        check(file.exists()) { "Committed fixture tests/fixtures/synthetic-searchable.pdf is missing" }
        return file.readBytes()
    }

    @Test
    fun `the committed synthetic searchable PDF extracts successfully with non-blank text and a correctly populated extraction identity`() = runTest {
        val extractor = TikaEvidenceExtractor()

        val outcome = extractor.extract(syntheticSearchablePdfBytes())

        val extracted = assertIs<ExtractionOutcome.Extracted>(outcome)
        assertTrue(extracted.result.extractedText.isNotBlank())
        assertTrue(
            extracted.result.extractedText.contains("Hello, Parker"),
            "expected the fixture's own known embedded text; got: ${extracted.result.extractedText}",
        )
        assertEquals("application/pdf", extracted.result.detectedMediaType)
        assertEquals(TikaEvidenceExtractor.EXTRACTOR_NAME, extracted.result.extractorName)
        assertEquals(TikaEvidenceExtractor.EXTRACTOR_VERSION, extracted.result.extractorVersion)
        assertEquals(TikaEvidenceExtractor.CONFIGURATION_PROFILE, extracted.result.extractionIdentity.configurationProfile)
        assertEquals(TikaEvidenceExtractor.NORMALISATION_PROFILE, extracted.result.extractionIdentity.normalisationProfile)
        assertTrue(extracted.result.extractionIdentity.parserIdentity.isNotBlank())
        assertFalse(extracted.result.ocrUsed)
    }

    @Test
    fun `running extraction twice against the same synthetic fixture produces byte-identical text and an identical extraction identity`() = runTest {
        val extractor = TikaEvidenceExtractor()
        val bytes = syntheticSearchablePdfBytes()

        val first = assertIs<ExtractionOutcome.Extracted>(extractor.extract(bytes))
        val second = assertIs<ExtractionOutcome.Extracted>(extractor.extract(bytes))

        assertEquals(first.result.extractedText, second.result.extractedText)
        assertEquals(first.result.extractionIdentity, second.result.extractionIdentity)
        assertEquals(first.result.detectedMediaType, second.result.detectedMediaType)
    }

    @Test
    fun `a non-PDF file is classified unsupported, carrying the detected media type and a reason`() = runTest {
        val extractor = TikaEvidenceExtractor()
        val plainTextBytes = "This is a plain text file, not a PDF at all.".toByteArray(Charsets.UTF_8)

        val outcome = extractor.extract(plainTextBytes)

        val unsupported = assertIs<ExtractionOutcome.Unsupported>(outcome)
        assertTrue(unsupported.reason.isNotBlank())
        assertTrue(unsupported.detectedMediaType != "application/pdf")
    }

    @Test
    fun `bytes with a PDF header but no valid structure never produce a false successful extraction`() = runTest {
        val extractor = TikaEvidenceExtractor()
        // A genuine PDF magic-number header followed immediately by truncated garbage -- detected
        // as application/pdf by magic-number sniffing, but unparseable as a real PDF document
        // structure. Which exact terminal outcome PDFBox's own recovery parsing produces for
        // genuinely truncated bytes (Malformed, or an empty parse classified RequiresOcr) is not
        // asserted specifically here -- only that it is never a false, emptily "successful"
        // Extracted result. The dedicated local real-malformed-file test below asserts the
        // specific Malformed/Unsupported classification against a genuine real-world corrupt file.
        val truncatedPdfBytes = "%PDF-1.4\nJUNKDATA-NOT-A-REAL-PDF-BODY-0123456789".toByteArray(Charsets.ISO_8859_1)

        val outcome = extractor.extract(truncatedPdfBytes)

        assertTrue(
            outcome !is ExtractionOutcome.Extracted,
            "truncated PDF-header bytes must never produce a successful extraction -- got $outcome",
        )
    }

    // ================= Real-world corpus (gitignored, skips gracefully if absent) =================

    @Test
    fun `a real searchable Uber-style receipt PDF extracts successfully`() = runTest {
        val file = localFixture("uber-receipt.pdf")
        assumeTrue(file.exists(), "local fixture tests/fixtures/local/uber-receipt.pdf not present -- skipping")

        val outcome = TikaEvidenceExtractor().extract(file.readBytes())

        val extracted = assertIs<ExtractionOutcome.Extracted>(outcome)
        assertTrue(extracted.result.extractedText.isNotBlank())
    }

    @Test
    fun `a real searchable WelTec-ERA PDF extracts successfully`() = runTest {
        val file = localFixture("weltec-era.pdf")
        assumeTrue(file.exists(), "local fixture tests/fixtures/local/weltec-era.pdf not present -- skipping")

        val outcome = TikaEvidenceExtractor().extract(file.readBytes())

        val extracted = assertIs<ExtractionOutcome.Extracted>(outcome)
        assertTrue(extracted.result.extractedText.isNotBlank())
    }

    @Test
    fun `a real scanned image-only PDF is classified requires-OCR`() = runTest {
        val file = localFixture("scanned-image-only.pdf")
        assumeTrue(file.exists(), "local fixture tests/fixtures/local/scanned-image-only.pdf not present -- skipping")

        val outcome = TikaEvidenceExtractor().extract(file.readBytes())

        assertIs<ExtractionOutcome.RequiresOcr>(outcome)
    }

    @Test
    fun `a real malformed PDF is classified malformed`() = runTest {
        val file = localFixture("malformed.pdf")
        assumeTrue(file.exists(), "local fixture tests/fixtures/local/malformed.pdf not present -- skipping")

        val outcome = TikaEvidenceExtractor().extract(file.readBytes())

        assertTrue(
            outcome is ExtractionOutcome.Malformed || outcome is ExtractionOutcome.Unsupported,
            "expected malformed or unsupported for a genuinely corrupt real-world file -- got $outcome",
        )
    }

    @Test
    fun `a real unsupported non-PDF file is classified unsupported`() = runTest {
        val file = localFixture("unsupported-format.docx")
        assumeTrue(file.exists(), "local fixture tests/fixtures/local/unsupported-format.docx not present -- skipping")

        val outcome = TikaEvidenceExtractor().extract(file.readBytes())

        assertIs<ExtractionOutcome.Unsupported>(outcome)
    }
}
