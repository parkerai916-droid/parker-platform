package parker.core.runtime

import parker.core.interfaces.DerivativeContentStorage
import parker.core.interfaces.DerivativeContentStorageException
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationStorage
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.TierAContentRetrievalOutcome

/**
 * Document Ingestion, Owner-Facing Tier A Derivative Content Retrieval
 * Boundary. Governed by
 * `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`
 * §11/§12. Sequences [DerivativeGenerationStorage] (record lookup) and
 * [DerivativeContentStorage] (content lookup) -- two independently
 * governed stores -- mirroring [TierAOwnerInvocationCoordinator]'s own
 * "sequencing across two separate constitutional domains" shape exactly.
 *
 * Retrieval is by already-known [EvidenceArtifactId] + [DerivativeGenerationId]
 * only -- this class performs no enumeration/browse of any kind (Scope
 * Lock §11), and never accepts a filesystem path from any caller.
 */
class TierAContentRetrievalCoordinator(
    private val generationStorage: DerivativeGenerationStorage,
    private val contentStorage: DerivativeContentStorage,
) {
    suspend fun retrieve(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): TierAContentRetrievalOutcome {
        val record = generationStorage.retrieve(derivativeGenerationId)
            ?: return TierAContentRetrievalOutcome.UnknownGeneration(derivativeGenerationId)

        if (record.rootSourceEvidenceArtifactId != evidenceArtifactId) {
            return TierAContentRetrievalOutcome.SourceMismatch(evidenceArtifactId, derivativeGenerationId)
        }

        val entry = try {
            contentStorage.retrieve(derivativeGenerationId)
        } catch (e: DerivativeContentStorageException.CorruptContent) {
            return TierAContentRetrievalOutcome.ContentCorrupt(derivativeGenerationId, e.message ?: "corrupt")
        } catch (e: DerivativeContentStorageException.UnsupportedRepresentationVersion) {
            return TierAContentRetrievalOutcome.UnsupportedRepresentationVersion(derivativeGenerationId, e.version)
        }
        entry ?: return TierAContentRetrievalOutcome.ContentMissing(derivativeGenerationId)

        return TierAContentRetrievalOutcome.Retrieved(record, entry.payload)
    }
}
