package parker.core.runtime

import java.io.IOException
import java.io.StringReader
import java.io.UncheckedIOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.DuplicateHeaderMode
import parker.core.interfaces.CsvStructuralExtractionOutcome
import parker.core.interfaces.CsvStructuralExtractor
import parker.core.interfaces.CsvStructuralResult
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.DerivativeProducerIdentity
import parker.core.interfaces.DerivativeTransformation

class ApacheCommonsCsvExtractor : CsvStructuralExtractor {
    override suspend fun extract(sourceBytes: ByteArray): CsvStructuralExtractionOutcome {
        val text = try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(sourceBytes))
                .toString()
        } catch (e: Exception) {
            return CsvStructuralExtractionOutcome.Malformed("CSV source is not valid UTF-8: ${e.message}")
        }
        val lineEnding = lineEnding(text)
        return try {
            val format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(',')
                    .setQuote('"')
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreSurroundingSpaces(false)
                    .setTrim(false)
                    .setAllowMissingColumnNames(false)
                    .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
                    .setIgnoreEmptyLines(false)
                    .get()
            CSVParser.builder().setReader(StringReader(text)).setFormat(format).get().use { parser ->
                val headers = parser.headerNames.toList()
                val rows = parser.records.map { record -> record.toList() }
                if (headers.isEmpty() || rows.any { it.size != headers.size }) {
                    CsvStructuralExtractionOutcome.Malformed("CSV record width does not match the header width")
                } else {
                    CsvStructuralExtractionOutcome.Extracted(
                        CsvStructuralResult(
                            headers = headers,
                            rows = rows,
                            delimiter = ',',
                            quoteCharacter = '"',
                            lineEnding = lineEnding,
                            producerIdentity = PRODUCER_IDENTITY,
                            transformationHistory = TRANSFORMATIONS,
                            completenessState = DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS,
                            warnings = LIMITATION_WARNINGS,
                        ),
                    )
                }
            }
        } catch (e: IOException) {
            CsvStructuralExtractionOutcome.Malformed("CSV parsing failed: ${e.message}")
        } catch (e: UncheckedIOException) {
            CsvStructuralExtractionOutcome.Malformed("CSV parsing failed: ${e.cause?.message ?: e.message}")
        } catch (e: IllegalArgumentException) {
            CsvStructuralExtractionOutcome.Malformed("CSV parsing failed: ${e.message}")
        }
    }

    companion object {
        const val PRODUCT_IDENTITY = "Apache Commons CSV"
        const val PRODUCT_VERSION = "1.14.1"
        const val ADAPTER_IDENTITY = "parker.apache-commons-csv"
        const val ADAPTER_VERSION = "1"
        const val CONFIGURATION_IDENTITY = "rfc4180-compatible-utf8-header-crlf-preserve-literals-v1"
        val PRODUCER_IDENTITY = DerivativeProducerIdentity(
            pluginIdentity = PRODUCT_IDENTITY,
            pluginVersion = PRODUCT_VERSION,
            configurationIdentity = CONFIGURATION_IDENTITY,
            adapterIdentity = ADAPTER_IDENTITY,
            adapterVersion = ADAPTER_VERSION,
        )
        val TRANSFORMATIONS = listOf(
            DerivativeTransformation.CHARACTER_DECODING,
            DerivativeTransformation.STRUCTURAL_PARSING,
            DerivativeTransformation.FIELD_UNQUOTING,
        )
        val LIMITATION_WARNINGS = listOf(
            "Apache Commons CSV does not expose original field quoting or per-field source byte offsets",
            "UTF-8 byte-order marks are preserved as literal content and are not treated as CSV syntax",
        )
    }

    private fun lineEnding(text: String): String {
        val withoutCrLf = text.replace("\r\n", "")
        val forms = buildSet {
            if ("\r\n" in text) add("CRLF")
            if ('\n' in withoutCrLf) add("LF")
            if ('\r' in withoutCrLf) add("CR")
        }
        return when (forms.size) {
            0 -> "NONE"
            1 -> forms.single()
            else -> "MIXED"
        }
    }
}
