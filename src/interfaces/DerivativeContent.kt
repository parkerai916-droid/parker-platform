package parker.core.interfaces

/**
 * Document Ingestion — Derivative Content Persistence and Retrieval.
 * Governed by `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`.
 * A durable, subordinate storage representation of the structured content
 * a specific, already-admitted Tier A [TierADocumentRoutingResult.Admitted]
 * generation produced -- never source evidence, never a replacement
 * `EvidenceArtifact`, never Memory, Knowledge, QMD, or RKS content, never
 * canonical (Scope Lock §5/§6). Exactly one entry per [DerivativeGenerationId]
 * (Scope Lock §4 cardinality freeze).
 */
data class DerivativeContentEntry(
    val derivativeGenerationId: DerivativeGenerationId,
    val rootSourceEvidenceArtifactId: EvidenceArtifactId,
    val payload: TierADerivativePayload,
)

sealed class DerivativeContentStorageException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class DuplicateIdentifier(val derivativeGenerationId: DerivativeGenerationId) :
        DerivativeContentStorageException("Derivative content '${derivativeGenerationId.value}' already exists")
    class UnsafeIdentifier(val derivativeGenerationId: DerivativeGenerationId) :
        DerivativeContentStorageException("Derivative generation identifier '${derivativeGenerationId.value}' is unsafe for storage")
    class InvalidStorageRoot(path: String, reason: String) :
        DerivativeContentStorageException("Derivative content storage root '$path' is invalid: $reason")
    class PersistenceFailure(message: String, cause: Throwable) : DerivativeContentStorageException(message, cause)

    /** The storage-representation integrity digest (Scope Lock §6) did not match on read -- proves storage-byte corruption only, never a claim about source or evidential integrity. */
    class CorruptContent(val derivativeGenerationId: DerivativeGenerationId, message: String, cause: Throwable? = null) :
        DerivativeContentStorageException("Derivative content '${derivativeGenerationId.value}' is corrupt: $message", cause)

    /** A stored entry's own representationVersion (Scope Lock §8) is not one this codec understands -- fails closed, never a best-effort guess. */
    class UnsupportedRepresentationVersion(val derivativeGenerationId: DerivativeGenerationId, val version: Int) :
        DerivativeContentStorageException("Derivative content '${derivativeGenerationId.value}' uses unsupported representation version $version")
}

/**
 * `prepare`/`publishPrepared`/`retrieve` mirrors [DerivativeGenerationStorage]'s
 * own established shape exactly -- write-once, durable, keyed by
 * [DerivativeGenerationId], never an arbitrary filesystem path from any
 * caller. Scope Lock §9's own frozen ordering requires every caller to
 * complete this store's own `prepare`/`publishPrepared` sequence *before*
 * the corresponding [DerivativeGenerationRecord] is ever prepared -- a
 * generation must never be reported admitted while its required content
 * is absent.
 */
interface DerivativeContentStorage {
    suspend fun prepare(entry: DerivativeContentEntry)
    suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId)
    suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId): DerivativeContentEntry?
}
