package parker.core.interfaces

/**
 * Parker Agent Gateway, AG-1F (R1 Candidate-Source Submission,
 * `docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 8, Section
 * 16, Section 20). Section 8's own "hard prerequisite, not optional":
 * `DefaultEvidenceCustodian.accept` mints a fresh, random `EvidenceArtifactId`
 * on every call, regardless of content -- submitting identical bytes twice
 * through the existing, unmodified path creates two distinct identities.
 * Section 8 requires this be closed by "a separate, narrow,
 * `EvidenceCustodian`-owned addition -- a source-identity-by-hash lookup --
 * decided and authored as its own governance unit, not invented inside the
 * Gateway." This file is that addition: a narrowly governed subordinate
 * mechanism within the same evidence authority boundary as
 * [EvidenceCustodian] itself, never a second, Gateway-side evidence
 * authority.
 *
 * ## Correction: durable create-or-get, not separate lookup/record
 * (crash-safe idempotency review)
 *
 * The original shape of this interface (`lookup` then, on a miss, a
 * separate `record` call) left a real crash/concurrency window open: a
 * process could durably accept evidence under identity `X` and then die, be
 * pre-empted, or otherwise never reach the `record` call, leaving `X`
 * durably registered with no hash mapping at all. A second submission of
 * the identical bytes would then see `lookup` return `null` and mint a
 * second, distinct identity `Y` -- violating [EvidenceCustodian.submitSource]'s
 * own hard idempotency requirement. [createOrGet] closes this by making the
 * reservation itself the single atomic, durable step -- the same
 * check-then-act sequence collapsed into one filesystem-level atomic
 * primitive, exactly the discipline [EvidenceSourceSubmissionResult]'s own
 * governing document already required of the *rest* of this mechanism.
 *
 * ## Template: [FileSystemExternalTranscriptionAuthorizationStore.createOrGet]
 * (see `src/runtime/ExternalTranscriptionOwnerAuthorization.kt`)
 *
 * Section 16 names this exact class's `createOrGet` as "the one existing
 * idempotent primitive in the relevant subsystem... this is the template
 * any future source-identity-by-hash addition should follow: same-bytes,
 * same-scope resubmission returns the existing identity; a scope mismatch
 * fails closed rather than silently picking one." [EvidenceSourceIdentityIndex]
 * follows that same one-fact-per-key, atomic-create-or-read shape, keyed
 * directly by the authoritative SHA-256 hex string itself (already a safe,
 * fixed-length, collision-resistant identifier -- no additional digest of
 * the key is needed, unlike that template's own digest-of-`evidenceArtifactId`
 * step, since a `UUID`-shaped `EvidenceArtifactId` is not itself
 * guaranteed filesystem-safe the way a lowercase-hex SHA-256 already is).
 *
 * ## What this reservation is, and is not, the source of truth for
 *
 * This index is a durable *source-identity reservation / idempotency*
 * mechanism only. It never becomes, and must never be treated as,
 * canonical registered evidence -- the canonical fact of "this evidence
 * artefact exists and was accepted" remains exactly what it always was:
 * the Evidence Custodian's own completed, durable source bytes
 * ([EvidenceArtifactStorage]) plus its [EvidenceSourceManifest]. A
 * reservation naming [EvidenceArtifactId] `X` with no completed canonical
 * bytes/manifest under `X` is an *incomplete* reservation, not a
 * registration -- [DefaultEvidenceCustodian.submitSource] is responsible
 * for completing (or discovering already-complete) canonical state before
 * ever reporting success, never this index.
 */
interface EvidenceSourceIdentityIndex {

    /**
     * Atomically claims [sha256] for [proposedEvidenceArtifactId], or reports the identity a
     * prior caller already claimed it for. Exactly one caller, across any number of concurrent or
     * sequential callers (in-process or cross-process, for the durable filesystem-backed
     * implementation), ever receives [SourceIdentityReservation.Created] for a given [sha256] --
     * every other caller, no matter how many, receives [SourceIdentityReservation.Existing] naming
     * that same winning identity. This single atomic step is the entire cross-instance
     * serialisation point for source-identity idempotency -- no caller-side mutex, lock, or
     * check-then-act sequence is required for correctness, only for reducing redundant work.
     *
     * Never mints, infers, or overwrites anything beyond the one atomic claim itself -- whether
     * canonical evidence (bytes, manifest) actually exists yet under the returned identity is
     * entirely the caller's own responsibility to establish and verify.
     */
    suspend fun createOrGet(sha256: String, proposedEvidenceArtifactId: EvidenceArtifactId): SourceIdentityReservation
}

/** What [EvidenceSourceIdentityIndex.createOrGet] returns. */
sealed class SourceIdentityReservation {

    /**
     * No prior reservation existed for the requested SHA-256 -- this call's own
     * [proposedEvidenceArtifactId] argument is now durably, atomically the one and only identity
     * reserved for it.
     */
    data class Created(val evidenceArtifactId: EvidenceArtifactId) : SourceIdentityReservation()

    /**
     * A reservation already existed for the requested SHA-256, naming [evidenceArtifactId] --
     * the caller's own proposed identity, if it supplied one, was never used.
     */
    data class Existing(val evidenceArtifactId: EvidenceArtifactId) : SourceIdentityReservation()
}

/**
 * What [EvidenceCustodian.submitSource] returns -- an explicit,
 * non-exceptional outcome for every governed possibility, mirroring
 * [EvidenceAcceptanceResult]'s own established shape.
 */
sealed class EvidenceSourceSubmissionResult {

    /**
     * No prior source shared this authoritative SHA-256, and canonical registration (source bytes
     * plus manifest) did not already exist under the reserved identity before this call -- this
     * call is the one that durably completed it, whether by winning the identity reservation
     * outright or by completing a reservation an earlier, interrupted attempt had already made
     * (crash-safe recovery, never a second, distinct identity).
     */
    data class Registered(
        val evidenceArtifactId: EvidenceArtifactId,
        val manifest: EvidenceSourceManifest,
    ) : EvidenceSourceSubmissionResult()

    /**
     * A source with this exact authoritative SHA-256 was already registered under
     * [evidenceArtifactId] -- no new identity was minted, no bytes were
     * rewritten, and [manifest] is the pre-existing, unmodified manifest
     * (Section 16's "same-bytes, same-scope resubmission returns the
     * existing identity").
     */
    data class AlreadyRegistered(
        val evidenceArtifactId: EvidenceArtifactId,
        val manifest: EvidenceSourceManifest,
    ) : EvidenceSourceSubmissionResult()

    /**
     * The caller's own advisory SHA-256 disagreed with Parker's own
     * authoritative, computed SHA-256. Fails closed -- nothing is
     * registered, and no prior registration (if any exists under the
     * *authoritative* hash) is disclosed or altered by this outcome.
     */
    data class HashMismatch(val computedSha256: String, val advisorySha256: String) : EvidenceSourceSubmissionResult()

    /** Acceptance was not authorised. Mirrors [EvidenceAcceptanceResult.Rejected] exactly. */
    data class Rejected(val reason: String) : EvidenceSourceSubmissionResult() {
        init {
            require(reason.isNotBlank()) { "EvidenceSourceSubmissionResult.Rejected.reason must not be blank" }
        }
    }

    /**
     * Crash-safe idempotency correction. The source-identity reservation for [computedSha256]
     * names [evidenceArtifactId], but the canonical Evidence Custodian state already stored under
     * that exact identity (bytes and/or manifest) does not match what this submission would
     * produce. Fails closed -- nothing already stored under [evidenceArtifactId] is overwritten,
     * and no second identity is minted for [computedSha256]. This represents an internal
     * consistency fault (corrupted or tampered storage; two genuinely different byte sequences
     * sharing one SHA-256) -- never an ordinary outcome of resubmitting identical bytes, and never
     * silently repaired.
     */
    data class Conflict(
        val evidenceArtifactId: EvidenceArtifactId,
        val computedSha256: String,
        val reason: String,
    ) : EvidenceSourceSubmissionResult() {
        init {
            require(reason.isNotBlank()) { "EvidenceSourceSubmissionResult.Conflict.reason must not be blank" }
        }
    }
}
