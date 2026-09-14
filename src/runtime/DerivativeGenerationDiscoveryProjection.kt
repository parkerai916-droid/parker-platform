package parker.core.runtime

import parker.core.interfaces.*

data class DerivativeCandidateSummary(
    val derivativeGenerationId: DerivativeGenerationId,
    val rootSourceEvidenceArtifactId: EvidenceArtifactId,
    val derivativeKind: String,
    val producerIdentity: DerivativeProducerIdentity,
    val generatedAt: java.time.Instant,
    val operationalOutcome: DerivativeOperationalOutcome,
    val completenessState: DerivativeCompletenessState,
    val warnings: List<String>,
    val transformationHistory: List<DerivativeTransformation>,
    val contentAvailable: Boolean,
    val authority: OcrAuthorityClassification = OcrAuthorityClassification.LOCAL_PRELIMINARY,
)

class DerivativeGenerationDiscoveryProjection(
    private val generations: DerivativeGenerationDiscovery,
    private val contents: DerivativeContentStorage,
) {
    suspend fun discover(evidenceArtifactId: EvidenceArtifactId): List<DerivativeCandidateSummary> =
        generations.findGenerationsForEvidence(evidenceArtifactId)
            .filter { it.rootSourceEvidenceArtifactId == evidenceArtifactId }
            .map { record ->
                val content = try { contents.retrieve(record.derivativeGenerationId) }
                catch (_: DerivativeContentStorageException) { null }
                val available = content != null
                val ocr = (content?.payload as? TierADerivativePayload.Ocr)?.value
                val authority = ocr?.authority ?: if (ocr?.providerProvenance != null) {
                    OcrAuthorityClassification.EXTERNAL_AUTHORITATIVE
                } else OcrAuthorityClassification.LOCAL_PRELIMINARY
                DerivativeCandidateSummary(record.derivativeGenerationId, record.rootSourceEvidenceArtifactId,
                    record.derivativeKind, record.producerIdentity, record.generatedAt, record.operationalOutcome,
                    record.completenessState, record.warnings, record.transformationHistory, available, authority)
            }
}
