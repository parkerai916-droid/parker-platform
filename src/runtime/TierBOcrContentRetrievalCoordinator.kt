package parker.core.runtime

import parker.core.interfaces.DerivativeContentStorage
import parker.core.interfaces.DerivativeContentStorageException
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationStorage
import parker.core.interfaces.DerivativeTransformation
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrDerivativeExtractedResult
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierBOcrContentRetrievalOutcome

/**
 * Document Ingestion — Tier B Durable OCR Derivative Content Retrieval
 * Boundary. Governed by
 * `docs/architecture/DOCUMENT_INGESTION_TIER_B_DURABLE_OCR_DERIVATIVE_CONTENT_SCOPE_LOCK.md`
 * §24/§28. Mirrors [TierAContentRetrievalCoordinator]'s own established
 * shape exactly -- **a separate class, never a modification or
 * repurposing of [TierAContentRetrievalCoordinator]** (§28: "the existing
 * `TierAContentRetrievalCoordinator` is not modified or repurposed") --
 * with one addition §28 itself requires: verifying the resolved record's
 * own kind discriminator (`DerivativeTransformation.OCR` in its
 * `transformationHistory`) before ever attempting to decode its content
 * as the Tier B representation shape.
 *
 * Retrieval is by already-known [EvidenceArtifactId] + [DerivativeGenerationId]
 * only -- no enumeration/browse of any kind, and this class holds no
 * [OcrMechanism]/[EvidenceIntelligenceOcrCoordinator] dependency of any
 * kind (Tier B scope lock §35's own "structural, not merely behavioural,
 * non-regeneration guarantee" -- retrieval cannot rerun OCR because it
 * has no path to OCR at all, not merely because it chooses not to call
 * one).
 */
class TierBOcrContentRetrievalCoordinator(
    private val generationStorage: DerivativeGenerationStorage,
    private val contentStorage: DerivativeContentStorage,
) {
    suspend fun retrieve(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): TierBOcrContentRetrievalOutcome {
        val record = generationStorage.retrieve(derivativeGenerationId)
            ?: return TierBOcrContentRetrievalOutcome.UnknownGeneration(derivativeGenerationId)

        if (record.rootSourceEvidenceArtifactId != evidenceArtifactId) {
            return TierBOcrContentRetrievalOutcome.SourceMismatch(evidenceArtifactId, derivativeGenerationId)
        }

        if (DerivativeTransformation.OCR !in record.transformationHistory) {
            return TierBOcrContentRetrievalOutcome.WrongDerivativeKind(derivativeGenerationId)
        }

        val entry = try {
            contentStorage.retrieve(derivativeGenerationId)
        } catch (e: DerivativeContentStorageException.CorruptContent) {
            return TierBOcrContentRetrievalOutcome.ContentCorrupt(derivativeGenerationId, e.message ?: "corrupt")
        } catch (e: DerivativeContentStorageException.UnsupportedRepresentationVersion) {
            return TierBOcrContentRetrievalOutcome.UnsupportedRepresentationVersion(derivativeGenerationId, e.version)
        }
        entry ?: return TierBOcrContentRetrievalOutcome.ContentMissing(derivativeGenerationId)

        val extracted: OcrDerivativeExtractedResult = when (val payload = entry.payload) {
            is TierADerivativePayload.Ocr -> payload.value
            else -> return TierBOcrContentRetrievalOutcome.WrongDerivativeKind(derivativeGenerationId)
        }

        return TierBOcrContentRetrievalOutcome.Retrieved(record, extracted)
    }
}
