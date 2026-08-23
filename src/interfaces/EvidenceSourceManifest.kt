package parker.core.interfaces

/**
 * Document Ingestion, Authoritative Source Manifest Foundation
 * Implementation. Governed in full by
 * `docs/architecture/DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
 * ("the Scope Lock"), which this file implements exactly -- no
 * responsibility, exclusion, or field beyond what that document already
 * fixed. See the Scope Lock's own Sections 4-14 for the full rationale;
 * this file's KDoc restates only what is specific to the types
 * themselves.
 *
 * ## Custody-integrity metadata, not a second `EvidenceArtifact`
 *
 * [EvidenceSourceManifest] is Evidence-Custodian-owned custody-integrity
 * metadata about already-custodied source bytes (Scope Lock Section 20)
 * -- it is never itself accepted, retrieved, or deleted as an
 * `EvidenceArtifact`, never a derivative, never a Memory Core record, and
 * never routing authority. Two fields ([sha256], [byteLength]) are
 * mandatory, deterministic facts established once, from the exact
 * candidate bytes, during governed admission (Scope Lock Section 7-8);
 * two ([receivedMediaType], [originalFileName]) are optional, externally
 * declared facts that may legitimately be absent and are never
 * inferred, computed, or fabricated (Scope Lock Sections 9-11, 13).
 *
 * ## Explicitly not part of this type (Scope Lock Section 6)
 *
 * No storage reference, no derivative identity, no Memory identity, no
 * Knowledge identity, no parser/OCR output, and no review state -- each
 * belongs to a different, already-governed domain this type does not
 * reach into.
 */
data class EvidenceSourceManifest(
    val evidenceArtifactId: EvidenceArtifactId,
    val sha256: String,
    val byteLength: Long,
    val receivedMediaType: String? = null,
    val originalFileName: String? = null,
) {
    init {
        require(sha256.matches(SHA256_PATTERN)) {
            "EvidenceSourceManifest.sha256 must be 64 lowercase hexadecimal characters"
        }
        require(byteLength >= 0) { "EvidenceSourceManifest.byteLength must not be negative" }
        require(receivedMediaType == null || receivedMediaType.isNotBlank()) {
            "EvidenceSourceManifest.receivedMediaType must not be blank if present -- omit it (null) instead " +
                "to express a genuinely undeclared media type"
        }
        require(originalFileName == null || originalFileName.isNotBlank()) {
            "EvidenceSourceManifest.originalFileName must not be blank if present -- omit it (null) instead " +
                "to express a genuinely undeclared filename"
        }
    }

    private companion object {
        val SHA256_PATTERN = Regex("^[0-9a-f]{64}$")
    }
}

/**
 * What [EvidenceCustodian.retrieveManifest] returns -- mirroring
 * [EvidenceRetrievalResult]'s own three-outcome shape exactly (Scope
 * Lock Section 14: "read-only, exactly mirroring `retrieve`'s own
 * ... discipline"). A denied Permission Engine decision and a genuinely
 * absent manifest are both ordinary, expected, non-exceptional outcomes,
 * never conflated with each other or with a thrown fault.
 */
sealed class EvidenceManifestRetrievalResult {

    /** Retrieval was authorised and an authoritative manifest exists for the requested identity. */
    data class Found(val manifest: EvidenceSourceManifest) : EvidenceManifestRetrievalResult()

    /**
     * Retrieval was authorised, but no authoritative manifest exists under
     * [evidenceArtifactId] -- either because admission has not yet
     * established one (a pre-foundation legacy artefact, Scope Lock
     * Section 13) or because nothing was ever custodied under this
     * identity. This type does not distinguish the two; both are an
     * honest "not presently manifest-complete" fact, never fabricated
     * into an apparent manifest.
     */
    data class NotFound(val evidenceArtifactId: EvidenceArtifactId) : EvidenceManifestRetrievalResult()

    /** Retrieval was not authorised. Carries the identifier requested, mirroring [EvidenceRetrievalResult.Rejected]. */
    data class Rejected(val evidenceArtifactId: EvidenceArtifactId, val reason: String) : EvidenceManifestRetrievalResult() {
        init {
            require(reason.isNotBlank()) { "EvidenceManifestRetrievalResult.Rejected.reason must not be blank" }
        }
    }
}

/**
 * Typed failures for [EvidenceSourceManifestStorage] -- mirroring
 * [EvidenceArtifactStorageException]'s and
 * [DerivativeGenerationStorageException]'s own established shape and
 * naming exactly, so behaviour does not silently diverge between this
 * repository's storage-exception families.
 */
sealed class EvidenceSourceManifestStorageException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {

    /** A write was attempted for an identity that already has a manifest -- refused, never overwritten (immutability, Scope Lock Section 9). */
    class DuplicateIdentifier(val evidenceArtifactId: EvidenceArtifactId) : EvidenceSourceManifestStorageException(
        "Evidence source manifest storage already holds a manifest for identifier " +
            "'${evidenceArtifactId.value}' -- duplicate writes are refused, never merged or overwritten",
    )

    /** [evidenceArtifactId]'s value cannot safely be used by this implementation, mirroring [EvidenceArtifactIdentifierSafety]'s own rule. */
    class UnsafeIdentifier(val evidenceArtifactId: EvidenceArtifactId) : EvidenceSourceManifestStorageException(
        "Evidence artifact identifier '${evidenceArtifactId.value}' cannot be used by this manifest storage implementation",
    )

    /** The supplied storage root failed validation at construction time. */
    class InvalidStorageRoot(val path: String, val reason: String) :
        EvidenceSourceManifestStorageException("Evidence source manifest storage root '$path' is invalid: $reason")

    /** An underlying I/O failure occurred while writing, reading, or deleting a manifest. [cause] is always the original exception. */
    class StorageIOFailure(message: String, cause: Throwable) : EvidenceSourceManifestStorageException(message, cause)

    /**
     * A stored manifest could not be decoded, or its own recorded identity
     * did not match the identity it was requested under -- never silently
     * repaired into apparent validity (Scope Lock's own adversarial-review
     * "corrupt manifest records" determination).
     */
    class CorruptRecord(val evidenceArtifactId: EvidenceArtifactId, message: String, cause: Throwable? = null) :
        EvidenceSourceManifestStorageException("Evidence source manifest for '${evidenceArtifactId.value}' is corrupt: $message", cause)
}

/**
 * The narrow, read-only-to-callers persistence primitive for
 * [EvidenceSourceManifest], subordinate to Evidence Custodian authority
 * (Scope Lock Section 14, option B). [write] is exposed here because
 * this interface is the storage primitive Evidence Custodian's own
 * governed `accept` boundary calls internally -- it is deliberately not
 * exposed as part of [EvidenceCustodian] itself, exactly as
 * `EvidenceArtifactStorage.write` is a lower-level primitive
 * `DefaultEvidenceCustodian.accept` calls, never a caller-facing
 * operation in its own right.
 *
 * No listing, search, "latest," update, or query capability exists on
 * this interface (Scope Lock Section 14) -- a manifest is retrieved only
 * by its own artefact's identity, exactly like the artefact's own bytes.
 */
interface EvidenceSourceManifestStorage {

    /**
     * Durably persists [manifest], exactly once, under
     * [EvidenceSourceManifest.evidenceArtifactId]. Throws
     * [EvidenceSourceManifestStorageException.DuplicateIdentifier] if a
     * manifest already exists for that identity -- never overwrites,
     * never merges (immutability, Scope Lock Section 9).
     */
    suspend fun write(manifest: EvidenceSourceManifest)

    /** Reads back the manifest stored for [evidenceArtifactId], or `null` if none has been established. */
    suspend fun read(evidenceArtifactId: EvidenceArtifactId): EvidenceSourceManifest?

    /**
     * Physically removes the manifest stored for [evidenceArtifactId], if
     * any -- no tombstone of any kind (Scope Lock Section 19, mirroring
     * Evidence Custodian Phase 7 Boundary Clarification Determination 2
     * exactly). Deliberately ungated, exactly like
     * `EvidenceArtifactStorage.delete` -- gating happens one level up, in
     * whatever governed deletion authority calls this method. Returns
     * `true` if a manifest existed and was removed; `false` if nothing was
     * stored under [evidenceArtifactId] -- deleting an absent manifest is
     * an ordinary, non-exceptional outcome, never an error.
     */
    suspend fun delete(evidenceArtifactId: EvidenceArtifactId): Boolean
}
