package parker.core.interfaces

data class CsvStructuralResult(
    val headers: List<String>,
    val rows: List<List<String>>,
    val delimiter: Char,
    val quoteCharacter: Char,
    val lineEnding: String,
    val producerIdentity: DerivativeProducerIdentity,
    val transformationHistory: List<DerivativeTransformation>,
    val completenessState: DerivativeCompletenessState,
    val warnings: List<String>,
) {
    init {
        require(headers.isNotEmpty()) { "CsvStructuralResult.headers must not be empty" }
        require(rows.all { it.size == headers.size }) { "CsvStructuralResult rows must match the header width" }
    }
}

sealed class CsvStructuralExtractionOutcome {
    data class Extracted(val result: CsvStructuralResult) : CsvStructuralExtractionOutcome()
    data class Malformed(val reason: String) : CsvStructuralExtractionOutcome() {
        init { require(reason.isNotBlank()) { "CsvStructuralExtractionOutcome.Malformed.reason must not be blank" } }
    }
}

fun interface CsvStructuralExtractor {
    suspend fun extract(sourceBytes: ByteArray): CsvStructuralExtractionOutcome
}
