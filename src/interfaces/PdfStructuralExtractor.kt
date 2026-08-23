package parker.core.interfaces

data class PdfMetadataValue(val name: String, val value: String, val representation: String)

data class PdfStructuralResult(
    val documentText: String,
    val pageCount: Int?,
    val pageTextAssociationAvailable: Boolean,
    val metadata: List<PdfMetadataValue>,
    val embeddedResources: List<EmbeddedResourceObservation>,
    val producerIdentity: DerivativeProducerIdentity,
    val transformationHistory: List<DerivativeTransformation>,
    val completenessState: DerivativeCompletenessState,
    val warnings: List<String>,
)

sealed class PdfStructuralExtractionOutcome {
    data class Extracted(val result: PdfStructuralResult) : PdfStructuralExtractionOutcome()
    data class RequiresTierB(val pageCount: Int?, val reason: String) : PdfStructuralExtractionOutcome()
    data class Unsupported(val reason: String) : PdfStructuralExtractionOutcome()
    data class Malformed(val reason: String) : PdfStructuralExtractionOutcome()
}

fun interface PdfStructuralExtractor {
    suspend fun extract(sourceBytes: ByteArray): PdfStructuralExtractionOutcome
}
