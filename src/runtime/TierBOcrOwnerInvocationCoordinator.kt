package parker.core.runtime

import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.OcrDerivativeOutcomeKind
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierBOcrOwnerInvocationOutcome

/**
 * Document Ingestion — Tier B Durable OCR Derivative Content. Sequences
 * [PermissionEngine] (invocation authorisation), [EvidenceCustodian]
 * (source-byte retrieval), [EvidenceIntelligenceOcrCoordinator] (the
 * existing, unmodified manifest-verified OCR path), and
 * [DerivativeGenerationCoordinator] (admission) -- mirroring
 * [TierAOwnerInvocationCoordinator]'s own "sequencing across separate
 * constitutional domains" shape, extended with the Permission Engine
 * evaluation Tier B scope lock §9 requires and Tier A's own boundary does
 * not (§9's own contrast: Tier A's document-ingestion machinery carries
 * no applicable Permission Engine gate; Evidence-Intelligence-adjacent
 * machinery, which this operation runs OCR through, does).
 *
 * ## Sequence (Tier B scope lock §9/§19)
 *
 * 1. [PermissionEngine.evaluate] against
 *    [EvidenceIntelligenceInvocationGate.buildExecutionRequest] -- the
 *    **same**, already-registered `(EXECUTE, DOCUMENT)` proposal class
 *    `ParkerRuntime.analyseEvidence` itself already evaluates. Anything
 *    other than `APPROVED`/`APPROVED_WITH_CONFIRMATION` returns
 *    [TierBOcrOwnerInvocationOutcome.NotAuthorised] immediately -- no
 *    source retrieval, no OCR invocation, no durable side effect of any
 *    kind.
 * 2. [EvidenceCustodian.retrieve] -- the manifest-verified integrity
 *    sequence itself (byte-length/SHA-256 against the authoritative
 *    manifest) is performed by [EvidenceIntelligenceOcrCoordinator]
 *    below, not duplicated here, mirroring
 *    [EvidenceIntelligenceOcrCoordinator]'s own KDoc: "input resolution
 *    has already happened once... this coordinator does not retrieve
 *    bytes a second time."
 * 3. [EvidenceIntelligenceOcrCoordinator.recognise] exactly once, no
 *    retry. Only [OcrRecognitionOutcome.Recognised]/[OcrRecognitionOutcome.PartialOrDegradedOutput]
 *    proceed to step 4 (Tier B scope lock §13); every other outcome
 *    returns a distinct, honest, non-durable
 *    [TierBOcrOwnerInvocationOutcome.OcrNotAdmissible].
 * 4. [DerivativeGenerationCoordinator.ingestOcr] -- the mandatory-provenance
 *    gate, `DerivativeGenerationId` minting, and the full
 *    prepare/publish/audit ordering (Tier B scope lock §19).
 *
 * No `try`/`catch` appears anywhere in [invoke] beyond what step 3/4's
 * own dependencies already perform internally -- a genuine fault
 * propagates unchanged, mirroring every other coordinator in this
 * repository's own "faults are never swallowed" discipline.
 */
internal class TierBOcrOwnerInvocationCoordinator(
    private val evidenceCustodian: EvidenceCustodian,
    private val permissionEngine: PermissionEngine,
    private val evidenceIntelligenceOcrCoordinator: EvidenceIntelligenceOcrCoordinator,
    private val derivativeGenerationCoordinator: DerivativeGenerationCoordinator,
) {
    suspend fun invoke(
        ownerPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
        correlationValue: String,
    ): TierBOcrOwnerInvocationOutcome {
        val decision = permissionEngine.evaluate(EvidenceIntelligenceInvocationGate.buildExecutionRequest(ownerPrincipalId))
        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) {
            return TierBOcrOwnerInvocationOutcome.NotAuthorised(
                "Permission Engine did not authorise Tier B OCR durable-generation invocation for principal " +
                    "'${ownerPrincipalId.value}' (decision=${decision.decision})",
            )
        }

        val content = when (val retrievalResult = evidenceCustodian.retrieve(ownerPrincipalId, evidenceArtifactId)) {
            is EvidenceRetrievalResult.Found -> retrievalResult.content
            is EvidenceRetrievalResult.NotFound -> return TierBOcrOwnerInvocationOutcome.SourceNotFound(evidenceArtifactId)
            is EvidenceRetrievalResult.Rejected -> return TierBOcrOwnerInvocationOutcome.SourceRetrievalRejected(evidenceArtifactId, retrievalResult.reason)
        }

        val ocrOutcome = when (val coordinatorOutcome = evidenceIntelligenceOcrCoordinator.recognise(ownerPrincipalId, evidenceArtifactId, content)) {
            is OcrCoordinatorOutcome.ManifestNotFound -> return TierBOcrOwnerInvocationOutcome.ManifestNotFound(evidenceArtifactId)
            is OcrCoordinatorOutcome.ManifestRejected -> return TierBOcrOwnerInvocationOutcome.SourceRetrievalRejected(evidenceArtifactId, coordinatorOutcome.reason)
            is OcrCoordinatorOutcome.ByteLengthMismatch -> return TierBOcrOwnerInvocationOutcome.ByteLengthMismatch(evidenceArtifactId)
            is OcrCoordinatorOutcome.DigestMismatch -> return TierBOcrOwnerInvocationOutcome.DigestMismatch(evidenceArtifactId)
            is OcrCoordinatorOutcome.NotOcrEligible -> return TierBOcrOwnerInvocationOutcome.NotOcrEligible(evidenceArtifactId)
            is OcrCoordinatorOutcome.Recognised -> coordinatorOutcome.outcome
        }

        val result = when (ocrOutcome) {
            is OcrRecognitionOutcome.Recognised -> Triple(ocrOutcome.result, OcrDerivativeOutcomeKind.RECOGNISED, null as String?)
            is OcrRecognitionOutcome.PartialOrDegradedOutput -> Triple(ocrOutcome.partialResult, OcrDerivativeOutcomeKind.PARTIAL_OR_DEGRADED, ocrOutcome.reason)
            else -> return TierBOcrOwnerInvocationOutcome.OcrNotAdmissible(ocrOutcome::class.simpleName ?: "Unknown", ocrNotAdmissibleReason(ocrOutcome))
        }
        val (ocrResult, outcomeKind, degradationReason) = result

        return when (
            val admission = derivativeGenerationCoordinator.ingestOcr(
                evidenceArtifactId, ocrResult, outcomeKind, degradationReason, ownerPrincipalId, correlationValue,
            )
        ) {
            is OcrDerivativeGenerationCoordinationOutcome.Admitted ->
                TierBOcrOwnerInvocationOutcome.Admitted(admission.record, admission.extracted)
            is OcrDerivativeGenerationCoordinationOutcome.MandatoryProvenanceUnavailable ->
                TierBOcrOwnerInvocationOutcome.MandatoryProvenanceUnavailable(admission.reason)
            is OcrDerivativeGenerationCoordinationOutcome.PreparationFailed ->
                TierBOcrOwnerInvocationOutcome.PreparationFailed(admission.derivativeGenerationId, admission.reason)
            is OcrDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed ->
                TierBOcrOwnerInvocationOutcome.AuthorisationAuditFailed(admission.derivativeGenerationId, admission.reason)
            is OcrDerivativeGenerationCoordinationOutcome.PublicationFailed ->
                TierBOcrOwnerInvocationOutcome.PublicationFailed(admission.derivativeGenerationId, admission.reason)
            is OcrDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed ->
                TierBOcrOwnerInvocationOutcome.AdmittedAuditFailed(admission.record, admission.extracted, admission.reason)
        }
    }

    /** A safe, non-blank diagnostic for a non-admissible [OcrRecognitionOutcome] variant -- each already carries its own honest `reason`. */
    private fun ocrNotAdmissibleReason(outcome: OcrRecognitionOutcome): String = when (outcome) {
        is OcrRecognitionOutcome.Failed -> outcome.reason
        is OcrRecognitionOutcome.NotAuthorised -> outcome.reason
        is OcrRecognitionOutcome.UnsupportedOrInaccessibleInput -> outcome.reason
        is OcrRecognitionOutcome.NoRecognisableContent -> outcome.reason
        is OcrRecognitionOutcome.ValidationRejection -> outcome.reason
        is OcrRecognitionOutcome.ProcessingOrDependencyFailure -> outcome.reason
        is OcrRecognitionOutcome.GenuineImplementationFault -> outcome.reason
        is OcrRecognitionOutcome.Recognised, is OcrRecognitionOutcome.PartialOrDegradedOutput ->
            error("ocrNotAdmissibleReason must never be called for an admissible OCR outcome")
    }
}
