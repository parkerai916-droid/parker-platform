package parker.core.interfaces

/**
 * Document Ingestion, Owner-Facing Tier A Runtime Invocation Boundary.
 * The terminal outcome of one explicit owner-authorized Tier A ingestion
 * invocation, governed by
 * `docs/architecture/DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
 * Section 21's own fixed chain: owner authorization -> manifest
 * retrieval -> byte retrieval -> integrity verification -> Tier A
 * router. Every variant below corresponds to exactly one point that
 * chain can stop at, before the existing, unchanged
 * [TierADocumentIngestionRouter] is ever reached -- once
 * [TierADocumentIngestionRouter.ingest] is actually called, its own
 * result is carried unchanged in [Routed], never re-derived or
 * collapsed, so every distinction the router itself already makes
 * (`Admitted`/`RequiresTierB`/`Unsupported`/`ExtractionFailed`/
 * `SourceIntegrityFailed`/`AdmissionFailed`/`ReconciliationRequired`)
 * remains fully visible to the caller.
 *
 * A denied Permission Engine decision and a genuinely absent manifest or
 * source are kept distinct at each stage (mirroring
 * [EvidenceManifestRetrievalResult]'s and [EvidenceRetrievalResult]'s
 * own existing Rejected/NotFound distinction) -- never conflated with
 * each other, and never reported as an [TierADocumentRoutingResult]
 * variant, since the router itself was never reached on any of these
 * paths.
 */
sealed class TierAOwnerInvocationOutcome {

    /**
     * Manifest retrieval and source-byte retrieval both succeeded, the
     * retrieved bytes verified against the manifest's own authoritative
     * byte length and SHA-256, and [TierADocumentIngestionRouter.ingest]
     * was called exactly once. [result] is that call's own, unmodified
     * return value.
     */
    data class Routed(val result: TierADocumentRoutingResult) : TierAOwnerInvocationOutcome()

    /** [EvidenceCustodian.retrieveManifest] returned [EvidenceManifestRetrievalResult.Rejected]. No byte retrieval or router call occurred. */
    data class ManifestRetrievalRejected(val evidenceArtifactId: EvidenceArtifactId, val reason: String) : TierAOwnerInvocationOutcome()

    /** [EvidenceCustodian.retrieveManifest] returned [EvidenceManifestRetrievalResult.NotFound]. No byte retrieval or router call occurred. */
    data class ManifestNotFound(val evidenceArtifactId: EvidenceArtifactId) : TierAOwnerInvocationOutcome()

    /** [EvidenceCustodian.retrieve] returned [EvidenceRetrievalResult.Rejected]. No router call occurred. */
    data class SourceRetrievalRejected(val evidenceArtifactId: EvidenceArtifactId, val reason: String) : TierAOwnerInvocationOutcome()

    /** [EvidenceCustodian.retrieve] returned [EvidenceRetrievalResult.NotFound]. No router call occurred. */
    data class SourceNotFound(val evidenceArtifactId: EvidenceArtifactId) : TierAOwnerInvocationOutcome()

    /**
     * The retrieved source bytes' own length does not equal the
     * authoritative manifest's [EvidenceSourceManifest.byteLength]. Fails
     * closed before any digest computation or router call -- the cheap,
     * early-exit defense the manifest scope lock's own digest-authority
     * design (Section 8) anticipates.
     */
    data class ByteLengthMismatch(val evidenceArtifactId: EvidenceArtifactId, val expectedByteLength: Long, val actualByteLength: Long) :
        TierAOwnerInvocationOutcome()

    /**
     * The retrieved source bytes' own computed SHA-256 does not equal the
     * authoritative manifest's [EvidenceSourceManifest.sha256] -- the
     * load-bearing check the manifest scope lock exists to make possible.
     * [expectedSha256] is always the manifest's own persisted value,
     * never one computed from these same retrieved bytes. Fails closed
     * before any router call.
     */
    data class DigestMismatch(val evidenceArtifactId: EvidenceArtifactId, val expectedSha256: String, val actualSha256: String) :
        TierAOwnerInvocationOutcome()
}
