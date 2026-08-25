package parker.core.interfaces

/**
 * Document Ingestion — Tier B Durable OCR Derivative Content. Governed by
 * `docs/architecture/DOCUMENT_INGESTION_TIER_B_DURABLE_OCR_DERIVATIVE_CONTENT_SCOPE_LOCK.md`
 * ("the Tier B scope lock") and
 * `docs/architecture/TIER_B_OCR_DURABLE_REPRESENTATION_BOUNDS_DECISION.md`
 * ("the bounds decision"). Extends Document Ingestion's existing, unified
 * `DerivativeGenerationId`/`DerivativeGenerationRecord`/`DerivativeContentStorage`
 * architecture (Tier B scope lock §5) with a new content kind -- never a
 * parallel store, never a redesign of Tier A.
 */

/** Tier B scope lock §13: exactly two admissible outcome kinds; a degraded result must never become indistinguishable from a clean one merely because it was persisted. */
enum class OcrDerivativeOutcomeKind {
    RECOGNISED,
    PARTIAL_OR_DEGRADED,
}

/**
 * The durable Tier B OCR content payload (Tier B scope lock §12's
 * permitted-fields list). [producerIdentity]/[transformationHistory]/
 * [completenessState] are duplicated here as well as on the enclosing
 * [DerivativeGenerationRecord] -- mirroring Tier A's own precedent
 * ([PdfStructuralResult] etc. already carry [producerIdentity] as part of
 * their own content payload, not only via the Record built from it).
 * [degradationReason] is non-null if, and only if, [outcomeKind] is
 * [OcrDerivativeOutcomeKind.PARTIAL_OR_DEGRADED] (Tier B scope lock §13).
 * Unit B's optional page/processing/provider facts are in-memory contract
 * locations only. The unchanged version-1 durable codec neither writes nor
 * reconstructs them; durable representation support belongs to the later,
 * separately authorized version-2 admission unit.
 */
data class OcrDerivativeExtractedResult(
    val recognisedText: String,
    val fidelity: TranscriptionFidelity,
    val outcomeKind: OcrDerivativeOutcomeKind,
    val degradationReason: String?,
    val warnings: List<String>,
    val segments: List<OcrRecognitionSegment>,
    val producerIdentity: DerivativeProducerIdentity,
    val transformationHistory: List<DerivativeTransformation>,
    val completenessState: DerivativeCompletenessState,
    val pageAccounting: OcrPageAccounting? = null,
    val processingProvenance: OcrProcessingProvenance? = null,
    val providerProvenance: OcrProviderProvenance? = null,
) {
    init {
        require(recognisedText.isNotBlank()) { "OcrDerivativeExtractedResult.recognisedText must not be blank" }
        require((outcomeKind == OcrDerivativeOutcomeKind.PARTIAL_OR_DEGRADED) == (degradationReason != null)) {
            "OcrDerivativeExtractedResult.degradationReason must be present if, and only if, outcomeKind is PARTIAL_OR_DEGRADED"
        }
        require(degradationReason == null || degradationReason.isNotBlank()) {
            "OcrDerivativeExtractedResult.degradationReason must not be blank when present"
        }
    }
}

/**
 * The truthful result of one structurally owner-only, Permission-Engine-authorised
 * Tier B durable OCR generation attempt (Tier B scope lock §9/§11/§19).
 * Mirrors [TierAOwnerInvocationOutcome]'s own established shape, with the
 * two additional, Tier-B-specific fail-closed outcomes ([NotAuthorised],
 * [MandatoryProvenanceUnavailable]) named failure combination 1 and 2 of
 * §19 require.
 */
sealed class TierBOcrOwnerInvocationOutcome {

    /** Every step of §19's write/publication ordering succeeded; [record] and its subordinate content are both durably admitted. */
    data class Admitted(val record: DerivativeGenerationRecord, val extracted: OcrDerivativeExtractedResult) : TierBOcrOwnerInvocationOutcome()

    /** §19 named failure combination 1: the Permission Engine did not approve this invocation. No OCR execution occurred; no durable side effect of any kind. */
    data class NotAuthorised(val reason: String) : TierBOcrOwnerInvocationOutcome()

    /** [EvidenceCustodian.retrieveManifest] returned [EvidenceManifestRetrievalResult.NotFound] (surfaced via [EvidenceIntelligenceOcrCoordinator]). */
    data class ManifestNotFound(val evidenceArtifactId: EvidenceArtifactId) : TierBOcrOwnerInvocationOutcome()

    /** [EvidenceCustodian.retrieveManifest] returned [EvidenceManifestRetrievalResult.Rejected], or [EvidenceCustodian.retrieve] returned [EvidenceRetrievalResult.Rejected]. */
    data class SourceRetrievalRejected(val evidenceArtifactId: EvidenceArtifactId, val reason: String) : TierBOcrOwnerInvocationOutcome()

    /** [EvidenceCustodian.retrieve] returned [EvidenceRetrievalResult.NotFound]. */
    data class SourceNotFound(val evidenceArtifactId: EvidenceArtifactId) : TierBOcrOwnerInvocationOutcome()

    /** The retrieved source bytes' own length did not match the authoritative manifest. */
    data class ByteLengthMismatch(val evidenceArtifactId: EvidenceArtifactId) : TierBOcrOwnerInvocationOutcome()

    /** The retrieved source bytes' own SHA-256 did not match the authoritative manifest. */
    data class DigestMismatch(val evidenceArtifactId: EvidenceArtifactId) : TierBOcrOwnerInvocationOutcome()

    /** The manifest's own media type is not one [EvidenceIntelligenceOcrCoordinator] treats as OCR-eligible. */
    data class NotOcrEligible(val evidenceArtifactId: EvidenceArtifactId) : TierBOcrOwnerInvocationOutcome()

    /** [OcrMechanism.recognise] returned a non-admissible outcome (Tier B scope lock §13: anything other than `Recognised`/`PartialOrDegradedOutput`). Never durable. */
    data class OcrNotAdmissible(val outcomeKind: String, val reason: String) : TierBOcrOwnerInvocationOutcome()

    /** §19 named failure combination 2: a Record-mandatory provenance field could not be truthfully populated (Tier B scope lock §11). No `DerivativeGenerationId` minted; no durable state of any kind. */
    data class MandatoryProvenanceUnavailable(val reason: String) : TierBOcrOwnerInvocationOutcome()

    /** §19 named failure combination 3: OCR succeeded and provenance was truthful, but content or record preparation failed. No `DerivativeGenerationId` is ever disclosed as durable. */
    data class PreparationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : TierBOcrOwnerInvocationOutcome()

    /** §19 named failure combination 7 (`ADMISSION_AUTHORISED`): the record remains staged, never published. */
    data class AuthorisationAuditFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : TierBOcrOwnerInvocationOutcome()

    /** §19 named failure combination 5: content published, record publish failed. Requires reconciliation. */
    data class PublicationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : TierBOcrOwnerInvocationOutcome()

    /** §19 named failure combination 7 (`ADMITTED`): the record and content are genuinely admitted, but the audit trail's own final entry is missing. Reconciliation-required, never silently hidden or presented as unqualified success. */
    data class AdmittedAuditFailed(val record: DerivativeGenerationRecord, val extracted: OcrDerivativeExtractedResult, val reason: String) : TierBOcrOwnerInvocationOutcome()
}

/**
 * The truthful result of one Tier B durable OCR content retrieval by
 * known `EvidenceArtifactId` + `DerivativeGenerationId` (Tier B scope lock
 * §24/§28). Mirrors [TierAContentRetrievalOutcome]'s own established
 * shape, with [WrongDerivativeKind] added for §28's own kind-discrimination
 * requirement -- never reachable through [TierAContentRetrievalCoordinator],
 * which is not modified or repurposed by this document.
 */
sealed class TierBOcrContentRetrievalOutcome {
    data class Retrieved(val record: DerivativeGenerationRecord, val extracted: OcrDerivativeExtractedResult) : TierBOcrContentRetrievalOutcome()
    data class UnknownGeneration(val derivativeGenerationId: DerivativeGenerationId) : TierBOcrContentRetrievalOutcome()
    data class SourceMismatch(val evidenceArtifactId: EvidenceArtifactId, val derivativeGenerationId: DerivativeGenerationId) : TierBOcrContentRetrievalOutcome()

    /** The resolved record exists but does not carry `DerivativeTransformation.OCR` in its own transformation history -- a Tier A (or other) generation retrieved through the Tier B-specific path. Never mis-decoded. */
    data class WrongDerivativeKind(val derivativeGenerationId: DerivativeGenerationId) : TierBOcrContentRetrievalOutcome()
    data class ContentMissing(val derivativeGenerationId: DerivativeGenerationId) : TierBOcrContentRetrievalOutcome()
    data class ContentCorrupt(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : TierBOcrContentRetrievalOutcome()
    data class UnsupportedRepresentationVersion(val derivativeGenerationId: DerivativeGenerationId, val version: Int) : TierBOcrContentRetrievalOutcome()
}
