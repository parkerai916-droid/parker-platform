package parker.core.interfaces

/**
 * Evidence Custodian, Implementation Plan Phase 2 ("Immutable storage
 * behaviour"), Unit 1: the storage primitive later phases will build
 * governed acceptance and retrieval on top of. Governed by
 * `docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md` (the Contract
 * Design), `docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md` (the
 * Scope Lock), and `docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`
 * (the Implementation Plan) -- none of which this Unit reopens, narrows,
 * or reinterprets.
 *
 * This is deliberately **not** an `EvidenceCustodian` interface, and
 * [read] is deliberately **not** the governed, Permission-Engine-gated
 * retrieval surface Contract Design Section 4/6.6 describes. This Unit is
 * a narrower, lower-level primitive: write content under an identifier
 * exactly once, and allow that content to be read back for this Unit's
 * own internal verification only. Naming it separately from
 * `EvidenceCustodian` is deliberate, not an oversight -- constructing this
 * type must never be mistaken for "evidence has been accepted into
 * custody," since no acceptance concept exists anywhere in this file.
 *
 * ## What this Unit deliberately does not implement
 *
 * No `EvidenceCustodian` interface; no `EvidenceArtifact` or
 * `CandidateEvidenceArtifact` record; no acceptance concept of any kind
 * (no timestamp, no governed "this was accepted" claim); no Permission
 * Engine gating; no Memory Core integration; no provenance creation; no
 * hashing; no OCR or document analysis; no deletion; no runtime
 * composition. [read] exists solely so this Unit's own tests can verify
 * what [write] actually stored -- it is not exposed as, and must not be
 * treated as, a caller-facing retrieval API. That remains Implementation
 * Plan Section 4, Phase 4's own, later responsibility.
 *
 * ## What this Unit does establish
 *
 * - **No overwrite.** [write] fails outright, via
 *   [EvidenceArtifactStorageException.DuplicateIdentifier], if content
 *   already exists for the supplied identifier -- never overwrites, never
 *   merges, never silently succeeds a second time.
 * - **Exact-byte preservation.** Content written is content read back,
 *   unchanged, with no transformation, re-encoding, or compression.
 * - **No identifier minting.** This interface accepts an already-constructed
 *   [EvidenceArtifactId]; it never generates, reuses, or reassigns one --
 *   minting belongs to a later Unit.
 *
 * ## What this Unit does not yet guarantee
 *
 * Object- and file-level immutability under Parker's own code paths only
 * -- not the full guarantee Article IX (as amended) ultimately describes,
 * which also depends on acceptance-gating (a later Unit) and the eventual
 * absence of any other write path anywhere in the system. Each
 * implementation's own KDoc states its specific, further limitations
 * (crash behaviour, cross-process concurrency, OS/administrator-level
 * limits) rather than this interface overclaiming a guarantee no single
 * implementation can back up on its own.
 */
interface EvidenceArtifactStorage {

    /**
     * Writes [content] under [evidenceArtifactId], exactly once.
     *
     * Throws [EvidenceArtifactStorageException.DuplicateIdentifier] if
     * content already exists for this identifier. Throws
     * [EvidenceArtifactStorageException.UnsafeIdentifier] if
     * [evidenceArtifactId]'s value cannot safely be used by this
     * implementation. Throws [EvidenceArtifactStorageException.StorageIOFailure]
     * for an underlying I/O failure. A failed write of any kind leaves no
     * content readable under [evidenceArtifactId] -- a failure is never
     * partial.
     */
    suspend fun write(evidenceArtifactId: EvidenceArtifactId, content: ByteArray)

    /**
     * Reads back exactly what [write] stored for [evidenceArtifactId], or
     * `null` if nothing has been written for it yet. Exists for this
     * Unit's own internal verification only -- **not** the governed,
     * gated retrieval API a future Unit will add. Each call returns a
     * freshly allocated [ByteArray]; no cached or shared reference is
     * ever returned, so a caller mutating a previously returned array can
     * never affect what a later call returns.
     *
     * Throws [EvidenceArtifactStorageException.UnsafeIdentifier] under the
     * same condition [write] would -- an identifier that could never have
     * been safely written returns a clear error, not a misleading `null`.
     */
    suspend fun read(evidenceArtifactId: EvidenceArtifactId): ByteArray?
}

/**
 * Typed failures for [EvidenceArtifactStorage]. Sealed and thrown -- not
 * returned as a sealed result type -- mirroring this repository's own
 * established convention for caller-misuse/precondition failures
 * ([IdentityService], [ModuleRegistry], [ToolRegistry], and
 * `parker.composition.ParkerRuntimeException` all throw rather than
 * return a result type for exactly this class of failure).
 */
sealed class EvidenceArtifactStorageException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {

    /**
     * A write was attempted for an identifier that already has content
     * stored. The one behaviour this entire Unit exists to prove never
     * yields -- refused, not merged, not overwritten, not silently
     * accepted a second time.
     */
    class DuplicateIdentifier(val evidenceArtifactId: EvidenceArtifactId) : EvidenceArtifactStorageException(
        "Evidence artifact storage already holds content for identifier " +
            "'${evidenceArtifactId.value}' -- duplicate writes are refused, never merged or overwritten",
    )

    /**
     * [evidenceArtifactId]'s value cannot safely be used by this
     * implementation (for example, a filesystem-backed implementation
     * refusing a value that could be read as a path-traversal segment).
     * [EvidenceArtifactId] itself carries only a blank-value check
     * (Contract Design/Scope Lock's own already-frozen shape, unmodified
     * by this Unit) -- this exception is how an implementation defends
     * itself independently, rather than silently sanitising or truncating
     * an unsafe value into something else.
     */
    class UnsafeIdentifier(val evidenceArtifactId: EvidenceArtifactId, val reason: String) :
        EvidenceArtifactStorageException(
            "Evidence artifact identifier '${evidenceArtifactId.value}' cannot be used by this storage " +
                "implementation: $reason",
        )

    /** The supplied storage root failed validation at construction time. */
    class InvalidStorageRoot(val path: String, val reason: String) :
        EvidenceArtifactStorageException("Evidence artifact storage root '$path' is invalid: $reason")

    /**
     * An underlying I/O failure occurred while writing or reading
     * content. [cause] is always the original exception, never discarded.
     */
    class StorageIOFailure(message: String, cause: Throwable) : EvidenceArtifactStorageException(message, cause)
}

/**
 * The identifier-safety rule every [EvidenceArtifactStorage] implementation
 * in this Unit applies, shared here so behaviour does not silently diverge
 * between implementations (an identifier safe for one implementation must
 * be safe, or unsafe, identically for another). This is an input-validation
 * boundary this Unit's own storage layer enforces independently, never a
 * change to [EvidenceArtifactId]'s own already-frozen constructor -- which
 * continues to accept any non-blank value; the narrower rule below governs
 * only what this Unit's *storage* boundary will accept.
 *
 * ## Rule 1 -- canonical (lowercase-only) form
 *
 * A storage identifier must match `^[a-z0-9_-]+$`: lowercase ASCII letters,
 * digits, hyphen, and underscore only -- no uppercase, no `.`, no path
 * separator of either platform's kind, no whitespace, no null character.
 * An identifier containing an uppercase character is **rejected outright,
 * never silently lowercased or otherwise normalised** -- this is a
 * deliberate choice, not an oversight: silently normalising
 * `"Artifact-1"` to `"artifact-1"` would let two distinct
 * [EvidenceArtifactId] values (case-sensitive per that type's own equality)
 * collide on a case-insensitive filesystem (default Windows NTFS, default
 * macOS APFS) without either caller ever being told. Rejecting is the only
 * choice that keeps identity mapping exact and never surprises a caller by
 * quietly renaming what they asked to store.
 *
 * ## Rule 2 -- no Windows reserved device name
 *
 * A storage identifier must not be a Windows reserved device name --
 * `con`, `prn`, `aux`, `nul`, or `com1`-`com9`/`lpt1`-`lpt9` (exactly one
 * trailing digit `1`-`9`; `com10`/`lpt10` are ordinary, valid identifiers,
 * not reserved). This check compares against the identifier's own
 * already-lowercase form (Rule 1 above guarantees no uppercase reaches
 * this point) -- it is documented here as **case-insensitive platform
 * protection**, not merely "a lowercase check that happens to catch the
 * lowercase spelling": the two rules compose so that no case variant of a
 * reserved name (`CON`, `Con`, `con`) can ever reach storage, even though
 * only the canonical lowercase form is ever compared directly. These names
 * are special to the Windows filesystem regardless of extension
 * (`con.evidence` is still special) -- excluding them protects portability
 * of a storage layout this Unit deliberately keeps platform-independent.
 */
internal object EvidenceArtifactIdentifierSafety {

    private val SAFE_IDENTIFIER_PATTERN = Regex("^[a-z0-9_-]+$")
    private val RESERVED_BASE_NAMES = setOf("con", "prn", "aux", "nul")
    private val RESERVED_NUMBERED_PREFIXES = setOf("com", "lpt")

    /**
     * Throws [EvidenceArtifactStorageException.UnsafeIdentifier] unless
     * [evidenceArtifactId]'s value satisfies both Rule 1 and Rule 2 above.
     * Every [EvidenceArtifactStorage] implementation in this Unit calls
     * this before deriving any storage location from an identifier --
     * including the in-memory implementation, which does not itself
     * construct a path, so that switching implementations never changes
     * which identifiers are accepted.
     */
    fun requireSafe(evidenceArtifactId: EvidenceArtifactId) {
        val value = evidenceArtifactId.value
        if (!SAFE_IDENTIFIER_PATTERN.matches(value)) {
            throw EvidenceArtifactStorageException.UnsafeIdentifier(
                evidenceArtifactId,
                "identifier value must match ${SAFE_IDENTIFIER_PATTERN.pattern} (lowercase letters, digits, " +
                    "hyphen, underscore only -- uppercase is rejected, never normalised)",
            )
        }
        if (isReservedDeviceName(value)) {
            throw EvidenceArtifactStorageException.UnsafeIdentifier(
                evidenceArtifactId,
                "identifier value '$value' matches a Windows reserved device name and is rejected on every " +
                    "platform for portability",
            )
        }
    }

    /**
     * `value` is guaranteed already lowercase and charset-safe by the time
     * this is called ([requireSafe] checks Rule 1 first) -- so a direct
     * comparison against the lowercase reserved-name forms below is
     * sufficient to catch every case variant, per this object's own KDoc.
     */
    private fun isReservedDeviceName(value: String): Boolean {
        if (value in RESERVED_BASE_NAMES) {
            return true
        }
        val trailingDigit = value.lastOrNull()
        val prefix = if (value.isNotEmpty()) value.dropLast(1) else value
        return prefix in RESERVED_NUMBERED_PREFIXES && trailingDigit != null && trailingDigit in '1'..'9'
    }
}
