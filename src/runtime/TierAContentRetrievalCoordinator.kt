package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.DerivativeContentStorage
import parker.core.interfaces.DerivativeContentStorageException
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationStorage
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.TierAContentRetrievalOutcome
import parker.core.interfaces.EffectiveHumanFidelityReviewProjector
import parker.core.interfaces.HumanFidelityEligibilityUse
import parker.core.interfaces.HumanFidelityReviewTarget
import parker.core.interfaces.OcrSha256Digest
import parker.core.interfaces.TierADerivativePayload

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
    private val humanFidelityProjector: EffectiveHumanFidelityReviewProjector? = null,
    private val humanCorrectedRetrieval: HumanCorrectedRepresentationRetrievalService? = null,
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

        var exactTarget: HumanFidelityReviewTarget? = null
        val projection = when (val payload = entry.payload) {
            is TierADerivativePayload.RegionTranscription -> payload.value.preparationIdentity?.let { preparationIdentity ->
                val target = HumanFidelityReviewTarget(
                    evidenceArtifactId = record.rootSourceEvidenceArtifactId,
                    sourceSha256 = OcrSha256Digest(payload.value.sourceSha256),
                    preparationIdentity = OcrSha256Digest(preparationIdentity),
                    derivativeGenerationId = record.derivativeGenerationId,
                    derivativeGenerationSha256 = OcrSha256Digest(sha256(DerivativeGenerationRecordCodec.encode(record))),
                    derivativeContentSha256 = OcrSha256Digest(sha256(DerivativeContentCodec.encode(entry))),
                )
                exactTarget = target
                humanFidelityProjector?.project(target, HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION)
            }
            else -> null
        }
        // Corrected-representation presentation is subordinate metadata. A corrupt/ambiguous
        // corrected store must never leak an eligibility allowance, but it must not suppress an
        // independently authorised, intact raw provider representation either.
        val corrected = exactTarget?.let { target ->
            runCatching { humanCorrectedRetrieval?.retrieveForExactTarget(target) }.getOrNull()
        }
        return TierAContentRetrievalOutcome.Retrieved(record, entry.payload, projection, corrected)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
