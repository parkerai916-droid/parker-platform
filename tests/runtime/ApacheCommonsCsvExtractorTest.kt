package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CsvStructuralExtractionOutcome
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.DerivativeTransformation

class ApacheCommonsCsvExtractorTest {
    @Test
    fun `fixture 06 preserves every literal cell and declared structure`() = runTest {
        val bytes = Files.readAllBytes(FIXTURE)
        val outcome = assertIs<CsvStructuralExtractionOutcome.Extracted>(ApacheCommonsCsvExtractor().extract(bytes))
        val result = outcome.result
        assertEquals(listOf("id", "name", "reference", "amount", "note"), result.headers)
        assertEquals(EXPECTED_ROWS, result.rows)
        assertEquals(',', result.delimiter)
        assertEquals('"', result.quoteCharacter)
        assertEquals("CRLF", result.lineEnding)
        assertEquals(DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, result.completenessState)
        assertEquals(ApacheCommonsCsvExtractor.PRODUCER_IDENTITY, result.producerIdentity)
        assertEquals(
            listOf(DerivativeTransformation.CHARACTER_DECODING, DerivativeTransformation.STRUCTURAL_PARSING, DerivativeTransformation.FIELD_UNQUOTING),
            result.transformationHistory,
        )
        assertEquals(ApacheCommonsCsvExtractor.LIMITATION_WARNINGS, result.warnings)
    }

    @Test
    fun `malformed CSV is rejected without repair`() = runTest {
        val outcome = ApacheCommonsCsvExtractor().extract("a,b\r\n\"unterminated,b".toByteArray())
        assertIs<CsvStructuralExtractionOutcome.Malformed>(outcome)
    }

    @Test
    fun `duplicate headers are rejected explicitly`() = runTest {
        assertIs<CsvStructuralExtractionOutcome.Malformed>(
            ApacheCommonsCsvExtractor().extract("id,id\r\n1,2\r\n".toByteArray()),
        )
    }

    @Test
    fun `mixed line endings are disclosed rather than mislabeled`() = runTest {
        val result = assertIs<CsvStructuralExtractionOutcome.Extracted>(
            ApacheCommonsCsvExtractor().extract("a,b\r\n1,2\n".toByteArray()),
        ).result
        assertEquals("MIXED", result.lineEnding)
    }

    @Test
    fun `UTF-8 BOM is preserved as literal header content`() = runTest {
        val result = assertIs<CsvStructuralExtractionOutcome.Extracted>(
            ApacheCommonsCsvExtractor().extract("\uFEFFa,b\r\n1,2\r\n".toByteArray()),
        ).result
        assertEquals("\uFEFFa", result.headers.first())
    }

    companion object {
        val FIXTURE: Path = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/06-structured.csv")
        val EXPECTED_ROWS = listOf(
            listOf("001", "Alpha, Limited", "00001234", "1234.50", "Exact, quoted value"),
            listOf("002", "Māori Test", "ABC-007", "0.00", ""),
            listOf("003", "Bravo", "01-02-03", "42.10", "Line with  repeated spaces"),
            listOf("004", "Fixture Marker", "PARKER-FIXTURE-2026-006", "7.05", "He said \"literal\"."),
        )
    }
}
