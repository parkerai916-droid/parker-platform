package parker.core.runtime

import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.DerivativeOperationalOutcome
import parker.core.interfaces.OcrAuthorityClassification

/**
 * The single application-level continuation after governed source admission and case binding.
 * It deliberately accepts an already-admitted evidence identity: source custody, hashing, and
 * case membership remain owned by their existing coordinators.
 *
 * Tier A native admission is already a durable governed representation. OCR-required material
 * must continue through governed acquisition, where the production catalogue can select only an
 * authorised external OCR capability (local OCR remains preliminary/diagnostic).
 */
internal class PostAdmissionProcessingCoordinator(
    private val invokeTierA: suspend (EvidenceArtifactId) -> TierAOwnerInvocationOutcome,
    private val executeGovernedAcquisition: suspend (EvidenceArtifactId) -> AgentGatewayAcquisitionResult,
    private val findExistingAuthoritativeDerivative: suspend (EvidenceArtifactId) -> parker.core.interfaces.DerivativeGenerationId? = { null },
) {
    suspend fun process(evidenceArtifactId: EvidenceArtifactId): PostAdmissionProcessingOutcome {
        findExistingAuthoritativeDerivative(evidenceArtifactId)?.let {
            return when (val acquisition = executeGovernedAcquisition(evidenceArtifactId)) {
                is AgentGatewayAcquisitionResult.Completed -> PostAdmissionProcessingOutcome.AnalysisReady(
                    evidenceArtifactId, acquisition.derivativeGenerationId, acquisition.capabilityId,
                )
                is AgentGatewayAcquisitionResult.AuthorizationRequired -> PostAdmissionProcessingOutcome.RequiresOcr(
                    "Authorised external OCR is required before this evidence can become analysis-ready",
                )
                is AgentGatewayAcquisitionResult.ProviderNotReady -> PostAdmissionProcessingOutcome.CapabilityUnavailable(
                    "The authoritative OCR capability is not ready",
                )
                is AgentGatewayAcquisitionResult.ConfigurationNotAccepted -> PostAdmissionProcessingOutcome.CapabilityUnavailable(
                    "The authoritative OCR provider configuration is not accepted",
                )
                is AgentGatewayAcquisitionResult.CredentialUnavailable -> PostAdmissionProcessingOutcome.CapabilityUnavailable(
                    "The authoritative OCR provider credential is unavailable",
                )
                is AgentGatewayAcquisitionResult.NotFound -> PostAdmissionProcessingOutcome.Failed("SOURCE_NOT_FOUND", "Admitted evidence was not available for governed acquisition")
                is AgentGatewayAcquisitionResult.Denied -> PostAdmissionProcessingOutcome.CapabilityUnavailable("Governed acquisition was not authorised")
                is AgentGatewayAcquisitionResult.Failed -> PostAdmissionProcessingOutcome.CapabilityUnavailable(acquisition.reason)
            }
        }
        return when (val tierA = invokeTierA(evidenceArtifactId)) {
            is TierAOwnerInvocationOutcome.Routed -> when (val result = tierA.result) {
                is TierADocumentRoutingResult.Admitted -> continueWithGovernedAcquisition(evidenceArtifactId)
                is TierADocumentRoutingResult.RequiresTierB -> continueWithGovernedAcquisition(evidenceArtifactId)
                is TierADocumentRoutingResult.Unsupported -> PostAdmissionProcessingOutcome.Failed("UNSUPPORTED", result.reason)
                is TierADocumentRoutingResult.ExtractionFailed -> PostAdmissionProcessingOutcome.Failed("EXTRACTION", result.reason)
                is TierADocumentRoutingResult.SourceIntegrityFailed -> PostAdmissionProcessingOutcome.Failed("INTEGRITY", result.reason)
                is TierADocumentRoutingResult.AdmissionFailed -> PostAdmissionProcessingOutcome.Failed(result.stage, result.reason)
                is TierADocumentRoutingResult.ReconciliationRequired -> PostAdmissionProcessingOutcome.ReviewRequired(result.reason)
            }
            is TierAOwnerInvocationOutcome.ManifestRetrievalRejected,
            is TierAOwnerInvocationOutcome.ManifestNotFound,
            is TierAOwnerInvocationOutcome.SourceRetrievalRejected,
            is TierAOwnerInvocationOutcome.SourceNotFound,
            is TierAOwnerInvocationOutcome.ByteLengthMismatch,
            is TierAOwnerInvocationOutcome.DigestMismatch,
            -> PostAdmissionProcessingOutcome.Failed("SOURCE_VALIDATION", "Admitted source could not be validated for processing")
        }
    }

    private suspend fun continueWithGovernedAcquisition(id: EvidenceArtifactId): PostAdmissionProcessingOutcome =
        when (val acquisition = executeGovernedAcquisition(id)) {
            is AgentGatewayAcquisitionResult.Completed -> PostAdmissionProcessingOutcome.AnalysisReady(
                id, acquisition.derivativeGenerationId, acquisition.capabilityId,
            )
            is AgentGatewayAcquisitionResult.AuthorizationRequired -> PostAdmissionProcessingOutcome.RequiresOcr(
                "Authorised external OCR is required before this evidence can become analysis-ready",
            )
            is AgentGatewayAcquisitionResult.ProviderNotReady -> PostAdmissionProcessingOutcome.CapabilityUnavailable(
                "The authoritative external OCR capability is not ready",
            )
            is AgentGatewayAcquisitionResult.ConfigurationNotAccepted -> PostAdmissionProcessingOutcome.CapabilityUnavailable(
                "The authoritative external OCR provider configuration is not accepted",
            )
            is AgentGatewayAcquisitionResult.CredentialUnavailable -> PostAdmissionProcessingOutcome.CapabilityUnavailable(
                "The authoritative external OCR provider credential is unavailable",
            )
            is AgentGatewayAcquisitionResult.NotFound -> PostAdmissionProcessingOutcome.Failed("SOURCE_NOT_FOUND", "Admitted evidence was not available for governed acquisition")
            is AgentGatewayAcquisitionResult.Denied -> PostAdmissionProcessingOutcome.CapabilityUnavailable("Governed acquisition was not authorised")
            is AgentGatewayAcquisitionResult.Failed -> PostAdmissionProcessingOutcome.CapabilityUnavailable(acquisition.reason)
        }
}

internal sealed interface PostAdmissionProcessingOutcome {
    data class AnalysisReady(val evidenceArtifactId: EvidenceArtifactId, val derivativeGenerationId: parker.core.interfaces.DerivativeGenerationId, val capabilityId: String) : PostAdmissionProcessingOutcome
    data class RequiresOcr(val reason: String) : PostAdmissionProcessingOutcome
    data class CapabilityUnavailable(val reason: String) : PostAdmissionProcessingOutcome
    data class ReviewRequired(val reason: String) : PostAdmissionProcessingOutcome
    data class Failed(val stage: String, val reason: String) : PostAdmissionProcessingOutcome
}
