package parker.core.runtime

import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.ExternalTranscriptionMechanism
import parker.core.interfaces.ExternalTranscriptionMechanismOutcome
import parker.core.interfaces.ExternalTranscriptionOwnerInvocationOutcome
import parker.core.interfaces.ExternalTranscriptionRequest
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

/** Owner-bound authorization, custody verification, one external invocation, and pure validation. */
class ExternalTranscriptionOwnerInvocationCoordinator(
    private val ownerPrincipalId: PrincipalId,
    private val permissionEngine: PermissionEngine,
    private val evidenceCustodian: EvidenceCustodian,
    private val externalMechanism: ExternalTranscriptionMechanism,
    private val validator: OcrStructuredResultValidator,
    private val durableAdmission: ValidatedExternalTranscriptionAdmission,
    private val representationFactory: OcrProcessingRepresentationFactory = OcrProcessingRepresentationFactory(),
    private val correlationFactory: () -> String = { UUID.randomUUID().toString() },
    private val invocationObserver: ExternalTranscriptionInvocationObserver = ExternalTranscriptionInvocationObserver.NONE,
) {
    private val sourceResolver = AuthoritativeAcquisitionSourceResolver(evidenceCustodian)

    suspend fun invoke(evidenceArtifactId: EvidenceArtifactId): ExternalTranscriptionOwnerInvocationOutcome {
        val decision = permissionEngine.evaluate(
            ExternalTranscriptionInvocationGate.buildExecutionRequest(ownerPrincipalId, evidenceArtifactId),
        )
        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) return ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised

        val trusted = when (val resolution = sourceResolver.resolveSourceThenManifest(ownerPrincipalId, evidenceArtifactId)) {
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
            mediaType == null || (mediaType != "application/pdf" && !mediaType.startsWith("image/", ignoreCase = true))
        ) return ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds(evidenceArtifactId)
        invocationObserver.sourceRetrieved()

        val representation = when (val outcome = representationFactory.create(
            authoritativeSource = trusted,
        )) {
            is OcrProcessingRepresentationOutcome.Created -> outcome.representation
            else -> return ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds(evidenceArtifactId)
        }
        val request = ExternalTranscriptionRequest(
            processingRepresentation = representation,
            maximumPageCount = ExternalTranscriptionRequest.MAX_PAGE_COUNT,
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
                evidenceArtifactId, validated, ownerPrincipalId, correlationFactory(),
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
