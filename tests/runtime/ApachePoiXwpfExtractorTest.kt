package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.apache.poi.xwpf.usermodel.BreakType
import org.apache.poi.xwpf.usermodel.XWPFDocument
import parker.core.interfaces.*

class ApachePoiXwpfExtractorTest {
    @Test fun `fixture 04 preserves literal paragraphs adjacent runs and formatting without rewriting source`() = runTest {
        val bytes = Files.readAllBytes(FIXTURE); val original = bytes.copyOf()
        assertEquals(38561, bytes.size); assertEquals(SOURCE_HASH, sha256(bytes))
        val result = extracted(bytes)
        assertContentEquals(original, bytes); assertEquals(SOURCE_HASH, sha256(bytes))
        val fidelity = result.paragraphs.single { "bold evidence" in it.text }
        assertEquals("This paragraph contains bold evidence, italic evidence, Unicode Māori, café, €42.00, and identifier 0004981.", fidelity.text)
        assertEquals(fidelity.text, fidelity.runs.joinToString("") { it.text })
        assertTrue(fidelity.runs.single { it.text == "bold evidence" }.bold)
        assertTrue(fidelity.runs.single { it.text == "italic evidence" }.italic)
        assertTrue(result.paragraphs.any { it.text == "PARKER-FIXTURE-2026-004" })
        assertTrue(result.paragraphs.any { it.text == "DOCX-PAGE-2-ONLY: STRUCTURE-7721" })
    }

    @Test fun `fixture lists tables page break headers footers and metadata remain structural`() = runTest {
        val result = extracted(Files.readAllBytes(FIXTURE))
        assertEquals("Title", result.paragraphs.first().styleId)
        assertEquals(3, result.paragraphs.count { it.styleId == "ListNumber" })
        assertEquals(3, result.paragraphs.count { it.styleId == "ListBullet" })
        assertTrue(result.paragraphs.filter { it.styleId?.startsWith("List") == true }.all { it.numberingId == null })
        assertEquals(EXPECTED_TABLE, result.tables.single().rows.map { row -> row.cells.map { it.text } })
        assertEquals(1, result.paragraphs.sumOf { it.hardPageBreakCount })
        assertEquals("Parker Structured DOCX Fixture — PARKER-FIXTURE-2026-004", result.headers.single().paragraphs.single().text)
        assertEquals("Synthetic evaluation evidence | Footer control DOCX-FOOTER-004", result.footers.single().paragraphs.single().text)
        assertEquals("rId9", result.headers.single().relationshipId)
        assertEquals("rId10", result.footers.single().relationshipId)
        assertEquals("Parker Structured Synthetic Fixture", result.metadata.title)
        assertEquals("Parker Synthetic Fixture Generator", result.metadata.author)
        assertEquals("Synthetic document-ingestion evaluation corpus", result.metadata.subject)
        assertEquals(19, result.parts.size)
        assertTrue(result.mediaPartNames.isEmpty())
        assertTrue(result.parts.none { it.name.contains("comments") || it.name.contains("footnotes") || it.name.contains("endnotes") || it.name.contains("embeddings/") })
        assertTrue(result.parts.any { it.name == "word/numbering.xml" })
        assertTrue(result.parts.any { it.name == "[Content_Types].xml" })
        assertTrue(result.relationshipCount > 0)
        assertTrue(result.relationshipTypes.none { it.endsWith("/hyperlink") })
        assertEquals(DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, result.completenessState)
    }

    @Test fun `non zip and truncated packages are malformed`() = runTest {
        assertIs<DocxStructuralExtractionOutcome.Malformed>(ApachePoiXwpfExtractor().extract("not a zip".toByteArray()))
        val bytes = Files.readAllBytes(FIXTURE)
        assertIs<DocxStructuralExtractionOutcome.Malformed>(ApachePoiXwpfExtractor().extract(bytes.copyOf(bytes.size / 2)))
    }

    @Test fun `oversized source fails explicitly before POI parsing`() = runTest {
        val outcome = assertIs<DocxStructuralExtractionOutcome.Malformed>(ApachePoiXwpfExtractor().extract(ByteArray(ApachePoiXwpfExtractor.MAX_SOURCE_BYTES + 1)))
        assertTrue("adapter limit" in outcome.reason)
    }

    @Test fun `POI zip bomb protection remains enabled`() {
        assertTrue(ApachePoiXwpfExtractor.POI_MIN_INFLATE_RATIO > 0.0)
    }

    @Test fun `multi-segment run tabs and line breaks remain text while only page breaks count`() = runTest {
        val output = ByteArrayOutputStream()
        XWPFDocument().use { document ->
            val paragraph = document.createParagraph()
            paragraph.createRun().apply { setText("alpha"); addTab(); setText("beta"); addBreak(); setText("gamma") }
            paragraph.createRun().apply { addBreak(BreakType.PAGE) }
            document.write(output)
        }
        val result = extracted(output.toByteArray())
        assertEquals("alpha\tbeta\ngamma\n", result.paragraphs.single().text)
        assertEquals("alpha\tbeta\ngamma", result.paragraphs.single().runs.first().text)
        assertEquals(1, result.paragraphs.single().hardPageBreakCount)
    }

    @Test fun `valid ZIP with malformed OOXML package cannot produce extraction`() = runTest {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml")); zip.write("<Types>".toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/document.xml")); zip.write("<not-wordprocessingml>".toByteArray()); zip.closeEntry()
        }
        assertIs<DocxStructuralExtractionOutcome.Malformed>(ApachePoiXwpfExtractor().extract(output.toByteArray()))
    }

    private suspend fun extracted(bytes: ByteArray): DocxStructuralResult {
        val outcome = ApachePoiXwpfExtractor().extract(bytes)
        return assertIs<DocxStructuralExtractionOutcome.Extracted>(outcome, (outcome as? DocxStructuralExtractionOutcome.Malformed)?.reason).result
    }
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        val FIXTURE = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/04-structured.docx")
        const val SOURCE_HASH = "9cd12baf98a26a8a0edf8d7caeb5a0f821f36d39dbe2e24e16eddbab40e5d49d"
        val EXPECTED_TABLE = listOf(
            listOf("Field", "Value", "Control"), listOf("Reference", "001245", "DOCX-CELL-004"),
            listOf("Location", "Te Whanganui-a-Tara", "Unicode"), listOf("Amount", "NZ$3,210.09", "Decimal"),
        )
    }
}
