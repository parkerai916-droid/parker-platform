package parker.core.interfaces

import java.time.Instant

/**
 * One provider-neutral page candidate before Parker reconciliation. Schema-valid construction
 * does not establish transcription accuracy, source fidelity, completeness, or truth.
 */
data class OcrStructuredPageCandidate(
    val pageNumber: Int,
    val text: String?,
    val outcome: OcrPageOutcomeKind,
    val reason: OcrPageOutcomeReason? = null,
    val warnings: List<String> = emptyList(),
    val uncertaintySpans: List<OcrUncertaintySpan> = emptyList(),
) {
    init {
        require(pageNumber >= 1) { "OcrStructuredPageCandidate.pageNumber must be one-based and positive" }
        require(text == null || text.isNotBlank()) { "OcrStructuredPageCandidate.text must be absent or non-blank" }
        require(warnings.size <= MAX_STRUCTURED_PAGE_WARNINGS) {
            "OcrStructuredPageCandidate.warnings must contain at most $MAX_STRUCTURED_PAGE_WARNINGS entries"
        }
        require(warnings.all { it.isNotBlank() && it.length <= MAX_STRUCTURED_TEXT_CHARACTERS }) {
            "OcrStructuredPageCandidate warnings must each contain 1..$MAX_STRUCTURED_TEXT_CHARACTERS characters"
        }
    }
}

/**
 * Provider-neutral structured transcription candidate. [declaredReturnedPageScope] is an
 * assertion to reconcile, never trusted as coverage proof. No durable generation identity,
 * transport detail, provider-specific vocabulary, or authority is carried here.
 */
data class OcrStructuredTranscriptionCandidate(
    val requestedPageScope: OcrPageScope,
    val submittedPageScope: OcrPageScope,
    val declaredReturnedPageScope: OcrPageScope,
    val pages: List<OcrStructuredPageCandidate>,
    val fidelity: TranscriptionFidelity,
    val recognitionIdentity: OcrRecognitionIdentity,
    val providerProvenance: OcrProviderProvenance,
    val processingProvenance: OcrProcessingProvenance,
    val recognisedAt: Instant,
    val warnings: List<String> = emptyList(),
) {
    init {
        require(warnings.size <= MAX_STRUCTURED_PAGE_WARNINGS) {
            "OcrStructuredTranscriptionCandidate.warnings must contain at most $MAX_STRUCTURED_PAGE_WARNINGS entries"
        }
        require(warnings.all { it.isNotBlank() && it.length <= MAX_STRUCTURED_TEXT_CHARACTERS }) {
            "OcrStructuredTranscriptionCandidate warnings must each contain 1..$MAX_STRUCTURED_TEXT_CHARACTERS characters"
        }
    }
}

/** Pure-validation result. Rejection reuses the existing OCR validation-rejection taxonomy. */
sealed interface OcrStructuredValidationOutcome {
    data class Validated(
        val outcome: OcrRecognitionOutcome,
        val completenessState: DerivativeCompletenessState,
        val pageAccounting: OcrPageAccounting,
    ) : OcrStructuredValidationOutcome

    data class Rejected(val outcome: OcrRecognitionOutcome.ValidationRejection) : OcrStructuredValidationOutcome
}

private const val MAX_STRUCTURED_PAGE_WARNINGS = 200
private const val MAX_STRUCTURED_TEXT_CHARACTERS = 4_096
