package parker.core.interfaces

enum class TierADocumentFormat { CSV, EML, DOCX, PDF }

data class TierADocumentSourceContext(
    val evidenceArtifactId: EvidenceArtifactId,
    val content: ByteArray,
    val expectedSha256: String,
    val receivedMediaType: String?,
    val originalFileName: String?,
    val requestingPrincipalId: PrincipalId,
    val correlationValue: String,
) {
    init {
        require(expectedSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(correlationValue.isNotBlank())
    }
}

data class TierAMediaFacts(
    val receivedMediaType: String?,
    val mechanicallyDetectedMediaType: String?,
    val originalFileName: String?,
    val disagreement: Boolean,
)

sealed class TierADerivativePayload {
    data class Csv(val value: CsvStructuralResult) : TierADerivativePayload()
    data class Eml(val value: EmlStructuralResult, val childSourceCandidateCount: Int) : TierADerivativePayload()
    data class Docx(val value: DocxStructuralResult) : TierADerivativePayload()
    data class Pdf(val value: PdfStructuralResult) : TierADerivativePayload()

    /**
     * Tier B durable OCR content -- reuses this same sealed payload type
     * (Document Ingestion — Tier B Durable OCR Derivative Content Scope
     * Lock §5: "DerivativeContentStorage/DerivativeContentEntry shape --
     * Yes, extended with a new content kind"), never a parallel store.
     * [DerivativeGenerationRecord.derivativeKind]/`transformationHistory`
     * remain the actual Tier A/Tier B discriminator (scope lock §28) --
     * this variant's own name is a storage-shape label only.
     */
    data class Ocr(val value: OcrDerivativeExtractedResult) : TierADerivativePayload()

    /** Ordinary external region-v5 transcription. This is deliberately distinct from OCR. */
    data class RegionTranscription(val value: OrdinaryRegionTranscriptionDerivative) : TierADerivativePayload()
}

sealed class TierADocumentRoutingResult {
    abstract val mediaFacts: TierAMediaFacts
    data class Admitted(val format: TierADocumentFormat, val record: DerivativeGenerationRecord,
        val payload: TierADerivativePayload, override val mediaFacts: TierAMediaFacts) : TierADocumentRoutingResult()
    data class RequiresTierB(val reason: String, override val mediaFacts: TierAMediaFacts) : TierADocumentRoutingResult()
    data class Unsupported(val reason: String, override val mediaFacts: TierAMediaFacts) : TierADocumentRoutingResult()
    data class ExtractionFailed(val format: TierADocumentFormat, val reason: String,
        override val mediaFacts: TierAMediaFacts) : TierADocumentRoutingResult()
    data class SourceIntegrityFailed(val reason: String, override val mediaFacts: TierAMediaFacts) : TierADocumentRoutingResult()
    data class AdmissionFailed(val format: TierADocumentFormat, val stage: String,
        val derivativeGenerationId: DerivativeGenerationId, val reason: String,
        override val mediaFacts: TierAMediaFacts) : TierADocumentRoutingResult()
    data class ReconciliationRequired(val format: TierADocumentFormat, val record: DerivativeGenerationRecord,
        val payload: TierADerivativePayload, val reason: String,
        override val mediaFacts: TierAMediaFacts) : TierADocumentRoutingResult()
}

fun interface TierADocumentIngestionRouter {
    suspend fun ingest(source: TierADocumentSourceContext): TierADocumentRoutingResult
}
