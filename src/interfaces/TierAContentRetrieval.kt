package parker.core.interfaces

/**
 * Document Ingestion, Owner-Facing Tier A Derivative Content Retrieval
 * Boundary. The terminal outcome of one explicit owner-authorized
 * retrieval of already-persisted Tier A derivative content, governed by
 * `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`
 * §11's own fixed chain: record lookup -> source-identity match ->
 * content lookup. Retrieval is by already-known [EvidenceArtifactId] +
 * [DerivativeGenerationId] only (Scope Lock §11) -- never a general
 * enumeration/browse capability, never an arbitrary filesystem path.
 */
sealed class TierAContentRetrievalOutcome {

    /** Both the [DerivativeGenerationRecord] and its content were found, and the record's own root matched the supplied [EvidenceArtifactId]. */
    data class Retrieved(
        val record: DerivativeGenerationRecord,
        val payload: TierADerivativePayload,
        val humanFidelityProjection: EffectiveHumanFidelityReviewProjectionOutcome? = null,
        val humanCorrectedRepresentation: HumanCorrectedRepresentationPresentation? = null,
    ) : TierAContentRetrievalOutcome()

    /** No [DerivativeGenerationRecord] exists for the supplied [DerivativeGenerationId]. */
    data class UnknownGeneration(val derivativeGenerationId: DerivativeGenerationId) : TierAContentRetrievalOutcome()

    /** The named generation's own [DerivativeGenerationRecord.rootSourceEvidenceArtifactId] does not equal the supplied [EvidenceArtifactId] -- fails closed rather than returning content under the wrong source identity (Scope Lock §11). */
    data class SourceMismatch(val evidenceArtifactId: EvidenceArtifactId, val derivativeGenerationId: DerivativeGenerationId) : TierAContentRetrievalOutcome()

    /** The [DerivativeGenerationRecord] exists but no corresponding content entry was ever published -- the one reachable orphan/reconciliation-required state Scope Lock §9/§13 names. Never silently treated as valid. */
    data class ContentMissing(val derivativeGenerationId: DerivativeGenerationId) : TierAContentRetrievalOutcome()

    /** The persisted content's own storage-integrity digest did not match on read (Scope Lock §6/§14) -- never a partial or best-effort return of possibly-damaged data. */
    data class ContentCorrupt(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : TierAContentRetrievalOutcome()

    /** The persisted content's own representationVersion is not one this Parker build understands (Scope Lock §8) -- fails closed, never a guess. */
    data class UnsupportedRepresentationVersion(val derivativeGenerationId: DerivativeGenerationId, val version: Int) : TierAContentRetrievalOutcome()
}
