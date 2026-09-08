package parker.core.interfaces

import java.time.Instant

/**
 * The narrow shared response boundary for external transcription: exactly the two lawful
 * concrete shapes, added to without touching either's existing fields
 * (FIDELITY_PRESERVING_EVIDENCE_ACQUISITION_SCOPE_LOCK.md §6.1). [OcrStructuredTranscriptionCandidate]
 * remains page-shaped and completely unchanged; [EmlStructuredTranscriptionCandidate] is
 * non-paginated and carries no page/pixel/OCR-specific field.
 */
sealed interface ExternalTranscriptionResultCandidate

/**
 * One MIME entity's verification outcome. This is a structured receipt over a section Parker
 * already knows deterministically -- never a second, independently-authored copy of its text.
 */
data class EmlVerifiedSectionOutcome(
    val mimeEntityId: String,
    val outcome: OcrPageOutcomeKind,
    val reason: OcrPageOutcomeReason? = null,
    val warnings: List<String> = emptyList(),
)

enum class EmlMessageOutcomeKind { TRANSCRIBED, TRANSCRIBED_WITH_QUALIFICATIONS, FAILED }

/**
 * Non-paginated structured receipt over an already-deterministic [parker.core.runtime.EmlDerivedRepresentation].
 * The provider is never asked to reproduce or rewrite the email text Parker already knows --
 * [sections] carries verification outcomes keyed by MIME entity id, never returned full text.
 */
data class EmlStructuredTranscriptionCandidate(
    val profileId: String,
    val requestId: String,
    val attemptId: String,
    val sourceEvidenceArtifactId: EvidenceArtifactId,
    val submittedRepresentationSha256: OcrSha256Digest,
    val processingProfileIdentity: String,
    val messageOutcome: EmlMessageOutcomeKind,
    val completenessState: DerivativeCompletenessState,
    val sections: List<EmlVerifiedSectionOutcome>,
    val recognitionIdentity: OcrRecognitionIdentity,
    val providerProvenance: OcrProviderProvenance,
    val recognisedAt: Instant,
    val warnings: List<String> = emptyList(),
) : ExternalTranscriptionResultCandidate

sealed interface EmlStructuredValidationOutcome {
    data class Validated(
        val messageOutcome: EmlMessageOutcomeKind,
        val completenessState: DerivativeCompletenessState,
        val verifiedSectionIds: List<String>,
        val warnings: List<String>,
    ) : EmlStructuredValidationOutcome
    data class Rejected(val reason: String) : EmlStructuredValidationOutcome
}

/** The EML analogue of [OcrDerivativeExtractedResult] -- what is durably admitted after validation. Never the email text again; identifiers and outcomes only. */
data class EmlExternalVerificationReceipt(
    val sourceEvidenceArtifactId: EvidenceArtifactId,
    val submittedRepresentationSha256: OcrSha256Digest,
    val representationGenerationProfileIdentity: String,
    val messageOutcome: EmlMessageOutcomeKind,
    val completenessState: DerivativeCompletenessState,
    val verifiedSectionIds: List<String>,
    val warnings: List<String>,
    val producerIdentity: DerivativeProducerIdentity,
    val providerProvenance: OcrProviderProvenance,
    val recognisedAt: Instant,
)

fun interface EmlValidatedExternalVerificationAdmission {
    suspend fun admitEmlExternalVerification(
        evidenceArtifactId: EvidenceArtifactId,
        structuralResult: EmlStructuralResult,
        receipt: EmlExternalVerificationReceipt,
        requestingPrincipalId: PrincipalId,
        correlationValue: String,
    ): EmlExternalVerificationAdmissionOutcome
}

sealed class EmlExternalVerificationAdmissionOutcome {
    data class Admitted(val record: DerivativeGenerationRecord, val receipt: EmlExternalVerificationReceipt) : EmlExternalVerificationAdmissionOutcome()
    data class MandatoryProvenanceUnavailable(val reason: String) : EmlExternalVerificationAdmissionOutcome()
    data class PreparationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : EmlExternalVerificationAdmissionOutcome()
    data class AuthorisationAuditFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : EmlExternalVerificationAdmissionOutcome()
    data class PublicationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : EmlExternalVerificationAdmissionOutcome()
    data class AdmittedAuditFailed(val record: DerivativeGenerationRecord, val receipt: EmlExternalVerificationReceipt, val reason: String) : EmlExternalVerificationAdmissionOutcome()
}
