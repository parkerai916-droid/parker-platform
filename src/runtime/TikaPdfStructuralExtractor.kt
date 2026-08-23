package parker.core.runtime

import parker.core.interfaces.*

class TikaPdfStructuralExtractor(
    private val evidenceExtractor: EvidenceExtractor = TikaEvidenceExtractor(),
) : PdfStructuralExtractor {
    override suspend fun extract(sourceBytes: ByteArray): PdfStructuralExtractionOutcome {
        if (sourceBytes.size > MAX_SOURCE_BYTES) {
            return PdfStructuralExtractionOutcome.Malformed("PDF source exceeds the $MAX_SOURCE_BYTES-byte Tier A adapter limit")
        }
        return when (val outcome = evidenceExtractor.extract(sourceBytes.copyOf())) {
            is ExtractionOutcome.RequiresOcr -> PdfStructuralExtractionOutcome.RequiresTierB(
                outcome.pageCount, "No searchable text layer was found; Tier B OCR is required and was not invoked",
            )
            is ExtractionOutcome.Malformed -> PdfStructuralExtractionOutcome.Malformed(outcome.reason)
            is ExtractionOutcome.Unsupported -> PdfStructuralExtractionOutcome.Unsupported(outcome.reason)
            is ExtractionOutcome.Extracted -> {
                val result = outcome.result
                val pageCount = result.documentMetadata["pageCount"]?.toIntOrNull()
                if (pageCount != null && pageCount > MAX_PAGE_COUNT) {
                    return PdfStructuralExtractionOutcome.Malformed("PDF page count exceeds the $MAX_PAGE_COUNT-page Tier A adapter limit")
                }
                if (result.extractedText.length > TikaEvidenceExtractor.MAX_EXTRACTED_TEXT_CHARACTERS) {
                    return PdfStructuralExtractionOutcome.Malformed("PDF extracted text exceeds the bounded Tier A adapter limit")
                }
                if (result.documentMetadata.size > MAX_METADATA_COUNT || result.documentMetadata.any { (key, value) ->
                        key.length > MAX_METADATA_CHARACTERS || value.length > MAX_METADATA_CHARACTERS
                    }) {
                    return PdfStructuralExtractionOutcome.Malformed("PDF parser metadata exceeds the bounded Tier A adapter limit")
                }
                PdfStructuralExtractionOutcome.Extracted(PdfStructuralResult(
                    documentText = result.extractedText,
                    pageCount = pageCount,
                    pageTextAssociationAvailable = false,
                    metadata = result.documentMetadata.entries.sortedBy { it.key }.map {
                        PdfMetadataValue(it.key, it.value, "TIKA_PARSER_EXPOSED")
                    },
                    embeddedResources = result.embeddedResources,
                    producerIdentity = PRODUCER_IDENTITY,
                    transformationHistory = TRANSFORMATIONS,
                    completenessState = DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS,
                    warnings = LIMITATION_WARNINGS,
                ))
            }
        }
    }

    companion object {
        const val MAX_SOURCE_BYTES = 32 * 1024 * 1024
        const val MAX_PAGE_COUNT = 10000
        const val MAX_METADATA_COUNT = 100
        const val MAX_METADATA_CHARACTERS = 64 * 1024
        const val PRODUCT_IDENTITY = "Apache Tika"
        const val PRODUCT_VERSION = "3.3.1"
        const val ADAPTER_IDENTITY = "parker.tika-searchable-pdf"
        const val ADAPTER_VERSION = "1"
        const val CONFIGURATION_IDENTITY = "pdfparser-searchable-text-whole-document-bounded-no-ocr-v1"
        val PRODUCER_IDENTITY = DerivativeProducerIdentity(PRODUCT_IDENTITY, PRODUCT_VERSION, CONFIGURATION_IDENTITY, ADAPTER_IDENTITY, ADAPTER_VERSION)
        val TRANSFORMATIONS = listOf(DerivativeTransformation.CHARACTER_DECODING, DerivativeTransformation.STRUCTURAL_PARSING, DerivativeTransformation.METADATA_INTERPRETATION)
        val LIMITATION_WARNINGS = listOf(
            "The existing Tika PDF path exposes whole-document text but not page-associated text or coordinates",
            "Reading order is parser-observed text order; no column, table, or layout reconstruction is claimed",
            "Metadata values are Tika-exposed representations and are not claimed as original lexical PDF object values",
            "Parker bounds source and SAX text/metadata output, but Tika/PDFBox may allocate bounded-source parser structures internally before Parker observes them",
        )
    }
}
