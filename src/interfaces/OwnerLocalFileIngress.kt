package parker.core.interfaces

/**
 * Document Ingestion, Owner-Authorized Local File Ingress. The terminal
 * outcome of one explicit, owner-authorized local file import invocation,
 * governed by
 * `docs/architecture/DOCUMENT_INGESTION_OWNER_AUTHORIZED_LOCAL_FILE_INGRESS_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 2's fixed chain: owner authorization ->
 * permission evaluation -> absolute-path validation -> symlink rejection
 * -> regular-file validation -> size-bound check -> bounded exact-byte
 * read -> [CandidateEvidenceArtifact] construction -> the existing,
 * unchanged [EvidenceCustodian.accept]. Every variant below corresponds to
 * exactly one point that chain can stop at (Scope Lock Section 15) --
 * never collapsed into a single vague failure, and never carrying the
 * owner-supplied local path itself (Scope Lock Section 18: the path is
 * invocation-local only and must never become durable, path-bearing
 * result data).
 */
sealed class OwnerLocalFileIngressOutcome {

    /**
     * Permission evaluation, path validation, size check, and the byte
     * read all succeeded, and [EvidenceCustodian.accept] accepted the
     * resulting candidate. [acceptedEvidenceArtifact] is that call's own,
     * unmodified accepted result.
     */
    data class Accepted(val acceptedEvidenceArtifact: AcceptedEvidenceArtifact) : OwnerLocalFileIngressOutcome()

    /**
     * The Permission Engine did not authorize this invocation's own,
     * distinct local-file-read proposed action (Scope Lock Section 3).
     * Evaluated strictly before any filesystem access of any kind -- no
     * path existence, type, or size fact was observed on this path.
     */
    data class AuthorizationRejected(val reason: String) : OwnerLocalFileIngressOutcome()

    /**
     * The supplied path was not syntactically valid or was not absolute
     * (Scope Lock Section 4: absolute paths only; relative paths are
     * rejected, fail-closed, distinctly from a nonexistent path). No
     * filesystem access occurred on this path.
     */
    object InvalidPath : OwnerLocalFileIngressOutcome()

    /** No filesystem object exists at the designated, already-validated-absolute path. */
    object PathNotFound : OwnerLocalFileIngressOutcome()

    /**
     * A symbolic link was found at some component of the resolved path --
     * the final designated object, an ancestor directory, or both (Scope
     * Lock Section 5: "no component of the resolved path may traverse a
     * symlink"). Never silently followed.
     */
    object SymlinkProhibited : OwnerLocalFileIngressOutcome()

    /**
     * The designated, non-symlink path exists but is not a regular file
     * -- a directory, a FIFO, a socket, a device file, or any other
     * non-regular filesystem object (Scope Lock Section 2/Section 8).
     */
    object NotARegularFile : OwnerLocalFileIngressOutcome()

    /**
     * The source exceeds the governed 64 MiB ingress bound (Scope Lock
     * Section 7) -- either observed at the pre-read attribute check, or
     * detected mid-read because the source grew past the bound between
     * validation and read completion. [observedByteLength] is whichever
     * of those two facts was actually observed; never a truncated byte
     * count.
     */
    data class SourceTooLarge(val observedByteLength: Long) : OwnerLocalFileIngressOutcome()

    /**
     * The source passed every prior check but the read itself failed --
     * a genuine I/O fault, or an observable inconsistency between the
     * byte length seen at validation and the byte length actually read
     * (Scope Lock Section 9's own "detected... modification... fails
     * closed" rule). [reason] is a plain-language, path-free explanation
     * (Scope Lock Section 18/Section 20: full paths are never persisted
     * or leaked through failure text).
     */
    data class SourceReadFailure(val reason: String) : OwnerLocalFileIngressOutcome()

    /**
     * [EvidenceCustodian.accept]'s own, unmodified rejection -- the
     * existing acceptance-time Permission Engine evaluation denied the
     * constructed candidate. [reason] is exactly
     * [EvidenceAcceptanceResult.Rejected.reason], unmodified.
     */
    data class EvidenceCustodianRejected(val reason: String) : OwnerLocalFileIngressOutcome()
}
