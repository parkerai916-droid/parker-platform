package parker.core.runtime

import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.ExternalTranscriptionMechanism
import parker.core.interfaces.ExternalTranscriptionMechanismOutcome
import parker.core.interfaces.ExternalTranscriptionOwnerInvocationOutcome
import parker.core.interfaces.ExternalTranscriptionRequest
import parker.core.interfaces.ExternalTranscriptionExecutionBinding
import parker.core.interfaces.OcrProcessingRepresentationOutcome
import parker.core.interfaces.OcrStructuredValidationOutcome
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import java.util.UUID

interface ExternalTranscriptionInvocationObserver {
    fun sourceRetrieved() = Unit
    fun representationBuilt() = Unit
    fun requestPrepared() = Unit
    fun generationAdmitted() = Unit
    companion object { val NONE = object : ExternalTranscriptionInvocationObserver {} }
}

/**
 * Custody verification, one external invocation, and pure validation -- gated by the exact
 * caller-supplied [PrincipalId] passed to [invoke], never a principal fixed at construction.
 *
 * ## Principal-parameterised, not principal-fixed (Parker Agent Gateway AG-1G correction)
 *
 * This class previously held a constructor-fixed `ownerPrincipalId`, used unconditionally for its
 * own internal permission check, source resolution, and durable admission/provenance -- meaning
 * every caller, regardless of who actually initiated the request, was silently attributed to
 * whichever principal this instance was constructed with. That was safe as long as the only
 * production callers were genuinely Owner-only entry points (`ParkerRuntime.invokeExternalTranscriptionAsOwner`,
 * `FidelityFirstAcceptanceCoordinator`), but AG-1G's own Hermes-scoped governed-acquisition path
 * (`ExternalTranscriptionAcquisitionExecutor`) reuses this exact same, shared coordinator instance
 * -- and a constructor-fixed Owner principal there would silently misattribute a Hermes-initiated
 * acquisition to Owner at the one stage (internal permission check, source resolution, durable
 * admission) this class itself controls. [invoke] now takes [requestingPrincipalId] as an explicit
 * per-call parameter instead -- every existing Owner-only call site now passes
 * `PrincipalId(config.ownerPrincipalId)` (or its own already-held owner-scoped field) explicitly,
 * preserving identical behaviour; AG-1G's own Hermes-scoped call site passes Hermes's principal.
 * No second coordinator, no second pipeline -- the identical class, parameterised correctly.
 */
class ExternalTranscriptionOwnerInvocationCoordinator(
    private val permissionEngine: PermissionEngine,
    private val evidenceCustodian: EvidenceCustodian,
    private val externalMechanism: ExternalTranscriptionMechanism,
    private val validator: OcrStructuredResultValidator,
    private val durableAdmission: ValidatedExternalTranscriptionAdmission,
    private val representationFactory: OcrProcessingRepresentationFactory = OcrProcessingRepresentationFactory(),
    private val correlationFactory: () -> String = { UUID.randomUUID().toString() },
    private val invocationObserver: ExternalTranscriptionInvocationObserver = ExternalTranscriptionInvocationObserver.NONE,
    private val executionBinding: ExternalTranscriptionExecutionBinding? = null,
) {
    private val sourceResolver = AuthoritativeAcquisitionSourceResolver(evidenceCustodian)

    suspend fun invoke(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): ExternalTranscriptionOwnerInvocationOutcome {
        val decision = permissionEngine.evaluate(
            ExternalTranscriptionInvocationGate.buildExecutionRequest(requestingPrincipalId, evidenceArtifactId),
        )
        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) return ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised

        val trusted = when (val resolution = sourceResolver.resolveSourceThenManifest(requestingPrincipalId, evidenceArtifactId)) {
            is AuthoritativeAcquisitionResolution.Verified -> resolution.input
            AuthoritativeAcquisitionResolution.ManifestNotFound -> return ExternalTranscriptionOwnerInvocationOutcome.ManifestNotFound(evidenceArtifactId)
            is AuthoritativeAcquisitionResolution.ManifestRejected,
            AuthoritativeAcquisitionResolution.ManifestIdentityMismatch,
            -> return ExternalTranscriptionOwnerInvocationOutcome.ManifestRejected(evidenceArtifactId)
            AuthoritativeAcquisitionResolution.SourceNotFound -> return ExternalTranscriptionOwnerInvocationOutcome.SourceNotFound(evidenceArtifactId)
            is AuthoritativeAcquisitionResolution.SourceRejected -> return ExternalTranscriptionOwnerInvocationOutcome.SourceRetrievalRejected(evidenceArtifactId)
            is AuthoritativeAcquisitionResolution.ByteLengthMismatch -> return ExternalTranscriptionOwnerInvocationOutcome.ByteLengthMismatch(evidenceArtifactId)
            is AuthoritativeAcquisitionResolution.DigestMismatch -> return ExternalTranscriptionOwnerInvocationOutcome.DigestMismatch(evidenceArtifactId)
        }
        val mediaType = trusted.mediaType
        if (trusted.byteLength <= 0 || trusted.byteLength > ExternalTranscriptionRequest.MAX_SOURCE_BYTES ||
            mediaType == null || (
                mediaType != "application/pdf" && mediaType != "text/csv" &&
                    !mediaType.startsWith("image/", ignoreCase = true)
                )
        ) return ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds(evidenceArtifactId)
        invocationObserver.sourceRetrieved()

        val representation = when (val outcome = representationFactory.create(
            authoritativeSource = trusted,
        )) {
            is OcrProcessingRepresentationOutcome.Created -> outcome.representation
            else -> return ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds(evidenceArtifactId)
        }
        val request = ExternalTranscriptionRequest(
            representation = representation,
            maximumPageCount = ExternalTranscriptionRequest.MAX_PAGE_COUNT,
            expectedPageCount = if (mediaType.startsWith("image/", ignoreCase = true)) 1 else null,
            executionBinding = executionBinding,
        )
        invocationObserver.representationBuilt()
        invocationObserver.requestPrepared()
        val candidate = when (val mechanismOutcome = externalMechanism.transcribe(request)) {
            is ExternalTranscriptionMechanismOutcome.Candidate -> mechanismOutcome.candidate
            is ExternalTranscriptionMechanismOutcome.Failure ->
                return ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure(mechanismOutcome.reason)
        }
        val provenance = candidate.processingProvenance
        if (provenance.sourceEvidenceArtifactId != evidenceArtifactId ||
            provenance.sourceManifestSha256.value != trusted.sha256 ||
            provenance.sourceMediaType != mediaType ||
            provenance.sourceByteLength != trusted.byteLength ||
            !provenance.byteExactCopy || provenance.representationMediaType != mediaType ||
            provenance.representationByteLength != trusted.byteLength ||
            provenance.representationSha256.value != trusted.sha256
        ) return ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected("Candidate provenance contradicts the verified source representation")

        return when (val validated = validator.validate(candidate)) {
            is OcrStructuredValidationOutcome.Validated -> when (val admission = durableAdmission.admit(
                evidenceArtifactId, validated, requestingPrincipalId, correlationFactory(),
            )) {
                is OcrDerivativeGenerationCoordinationOutcome.Admitted -> {
                    invocationObserver.generationAdmitted()
                    ExternalTranscriptionOwnerInvocationOutcome.Admitted(evidenceArtifactId, admission.record, admission.extracted)
                }
                is OcrDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed -> ExternalTranscriptionOwnerInvocationOutcome.ReconciliationRequired(evidenceArtifactId, admission.record, admission.extracted, admission.reason)
                is OcrDerivativeGenerationCoordinationOutcome.MandatoryProvenanceUnavailable -> ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed(admission.reason)
                is OcrDerivativeGenerationCoordinationOutcome.PreparationFailed -> ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed(admission.reason)
                is OcrDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed -> ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed(admission.reason)
                is OcrDerivativeGenerationCoordinationOutcome.PublicationFailed -> ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed(admission.reason)
            }
            is OcrStructuredValidationOutcome.Rejected ->
                ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected(validated.outcome.reason)
        }
    }
}
