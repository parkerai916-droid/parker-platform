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
)

class DerivativeGenerationDiscoveryProjection(
    private val generations: DerivativeGenerationDiscovery,
    private val contents: DerivativeContentStorage,
) {
    suspend fun discover(evidenceArtifactId: EvidenceArtifactId): List<DerivativeCandidateSummary> =
        generations.findGenerationsForEvidence(evidenceArtifactId)
            .filter { it.rootSourceEvidenceArtifactId == evidenceArtifactId }
            .map { record ->
                val available = try { contents.retrieve(record.derivativeGenerationId) != null }
                catch (_: DerivativeContentStorageException) { false }
                DerivativeCandidateSummary(record.derivativeGenerationId, record.rootSourceEvidenceArtifactId,
                    record.derivativeKind, record.producerIdentity, record.generatedAt, record.operationalOutcome,
                    record.completenessState, record.warnings, record.transformationHistory, available)
            }
}
