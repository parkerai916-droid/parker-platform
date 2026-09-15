package parker.core.interfaces

data class PdfMetadataValue(val name: String, val value: String, val representation: String)

data class PageTextRegion(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    init { require(left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()); require(right >= left && bottom >= top) }
}

/** Text associated with one source page. Offsets are UTF-16 offsets in documentText. */
data class PageTextSegment(
    val pageNumber: Int,
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val sectionHeading: String? = null,
    val region: PageTextRegion? = null,
    val extractionMethod: String,
    val confidence: Double? = null,
    val sourceSha256: String,
    val derivativeGenerationId: DerivativeGenerationId? = null,
) {
    init {
        require(pageNumber > 0); require(startOffset >= 0 && endOffset >= startOffset)
        require(sourceSha256.matches(Regex("[0-9a-f]{64}")))
        require(confidence == null || confidence in 0.0..1.0)
    }
}

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
    val pageTextSegments: List<PageTextSegment> = emptyList(),
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
