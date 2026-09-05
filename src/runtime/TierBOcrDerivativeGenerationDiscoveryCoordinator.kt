package parker.core.runtime

import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeTransformation
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrDerivativeGenerationDiscovery

/**
 * Document Ingestion — Tier B OCR Exact-Evidence Derivative Generation Discovery. Governed by
 * `docs/architecture/DOCUMENT_INGESTION_TIER_B_OCR_EXACT_EVIDENCE_DERIVATIVE_GENERATION_DISCOVERY_SCOPE_LOCK_AMENDMENT.md`.
 *
 * Given one already-known [EvidenceArtifactId], returns `0..N` admitted Tier B OCR derivative
 * generations rooted at exactly that artifact -- never a general enumeration, never a lookup by
 * any fact other than that one supplied identity. This is the discovery half of the read path;
 * content retrieval remains exclusively [TierBOcrContentRetrievalCoordinator]'s responsibility,
 * unmodified and un-duplicated here.
 *
 * Every candidate the underlying [OcrDerivativeGenerationDiscovery] returns is independently
 * re-validated against its own [DerivativeGenerationRecord.rootSourceEvidenceArtifactId] and
 * `transformationHistory` before inclusion -- the amendment's own paired-identity discipline. The
 * concrete storage implementation is expected to have already filtered correctly; this class does
 * not trust that and re-checks regardless, so a future differently-implemented
 * [OcrDerivativeGenerationDiscovery] (e.g. one backed by a separate index) can never leak a
 * generation for one evidence artifact into another's discovery result merely by returning an
 * unfiltered or stale candidate list.
 */
class TierBOcrDerivativeGenerationDiscoveryCoordinator(
    private val discovery: OcrDerivativeGenerationDiscovery,
) {
    suspend fun discover(evidenceArtifactId: EvidenceArtifactId): List<DerivativeGenerationRecord> =
        discovery.findOcrGenerationsForEvidence(evidenceArtifactId)
            .filter { it.rootSourceEvidenceArtifactId == evidenceArtifactId && DerivativeTransformation.OCR in it.transformationHistory }
            .sortedWith(compareByDescending<DerivativeGenerationRecord> { it.generatedAt }.thenByDescending { it.derivativeGenerationId.value })
}
