package parker.core.interfaces

import java.time.Instant

/**
 * CASE-1 — Owner Case / Matter Classification for Evidence. An opaque,
 * server-minted, stable identity for one owner-defined case/matter --
 * mirroring [EvidenceArtifactId]'s own "blank rejected, nothing else
 * asserted" shape exactly. Deliberately never derived from
 * [CaseRecord.caseName]: a case may be renamed in a future unit without
 * ever needing to reissue this identity, and two cases may coincidentally
 * share a name without colliding.
 */
@JvmInline
value class CaseId(val value: String) {
    init {
        require(value.isNotBlank()) { "CaseId must not be blank" }
    }
}

/**
 * CASE-1. The minimum durable fact set for one case/matter: a stable
 * [caseId], an owner-facing [caseName] label, and [createdAt]. No status,
 * no lifecycle, no legal-case semantics, no members, no notes -- CASE-1 is
 * exactly "stable human-readable case identity," nothing else (see this
 * unit's own scope boundary list).
 */
data class CaseRecord(
    val caseId: CaseId,
    val caseName: String,
    val createdAt: Instant,
) {
    init {
        require(caseName.isNotBlank()) { "CaseRecord.caseName must not be blank" }
        require(caseName.length <= MAX_CASE_NAME_LENGTH) {
            "CaseRecord.caseName must not exceed $MAX_CASE_NAME_LENGTH characters"
        }
    }

    companion object {
        const val MAX_CASE_NAME_LENGTH = 200
    }
}

/** Typed failures for [CaseStorage], mirroring [EvidenceSourceManifestStorageException]'s own established shape and naming. */
sealed class CaseStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    /** A create was attempted for a [CaseId] that already has a record -- refused, never overwritten (cases are immutable once created). */
    class DuplicateIdentifier(val caseId: CaseId) : CaseStorageException(
        "Case storage already holds a record for identifier '${caseId.value}' -- duplicate writes are refused, never merged or overwritten",
    )

    /** [caseId]'s value cannot safely be used by this implementation. */
    class UnsafeIdentifier(val caseId: CaseId) : CaseStorageException(
        "Case identifier '${caseId.value}' cannot be used by this case storage implementation",
    )

    /** The supplied storage root failed validation at construction time. */
    class InvalidStorageRoot(val path: String, val reason: String) :
        CaseStorageException("Case storage root '$path' is invalid: $reason")

    /** An underlying I/O failure occurred while writing, reading, or listing case records. [cause] is always the original exception. */
    class StorageIOFailure(message: String, cause: Throwable) : CaseStorageException(message, cause)

    /** A stored case record could not be decoded, or its own recorded identity did not match the identity it was requested under. */
    class CorruptRecord(val caseId: CaseId, message: String, cause: Throwable? = null) :
        CaseStorageException("Case record for '${caseId.value}' is corrupt: $message", cause)
}

/**
 * CASE-1. The narrow persistence primitive for [CaseRecord]. Deliberately
 * carries no update, rename, delete, status, or archival capability --
 * none of those are in CASE-1's own scope. [list] exists only to populate
 * the Owner UI's case filter/selector with the small, bounded set of
 * defined cases -- it is not a general query capability and this
 * interface never enumerates evidence.
 */
interface CaseStorage {
    /** Durably persists [case], exactly once, under [CaseRecord.caseId]. Throws [CaseStorageException.DuplicateIdentifier] if one already exists. */
    suspend fun create(case: CaseRecord)

    /** Reads back the case record stored for [caseId], or `null` if none has been created. */
    suspend fun read(caseId: CaseId): CaseRecord?

    /** Every defined case, in no particular guaranteed order -- the caller sorts/presents as needed. Bounded by however many cases the owner has actually created. */
    suspend fun list(): List<CaseRecord>
}

/**
 * CASE-1. The current, single, primary case assignment for one
 * [EvidenceArtifactId] -- [caseId] is `null` to represent the explicit
 * "Unassigned" state (evidence never assigned, or deliberately returned to
 * Unassigned). This is mutable "current state," not an append-only
 * history: [CaseAssignmentStorage.writeAssignment] always overwrites the
 * prior record for the same [evidenceArtifactId] -- the history of how it
 * got there lives only in [CaseGovernanceAudit], never duplicated here.
 */
data class CaseAssignmentRecord(
    val evidenceArtifactId: EvidenceArtifactId,
    val caseId: CaseId?,
    val assignedAt: Instant,
)

/** Typed failures for [CaseAssignmentStorage], mirroring [CaseStorageException]'s own shape. */
sealed class CaseAssignmentStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class UnsafeIdentifier(val evidenceArtifactId: EvidenceArtifactId) : CaseAssignmentStorageException(
        "Evidence artifact identifier '${evidenceArtifactId.value}' cannot be used by this case assignment storage implementation",
    )

    class InvalidStorageRoot(val path: String, val reason: String) :
        CaseAssignmentStorageException("Case assignment storage root '$path' is invalid: $reason")

    class StorageIOFailure(message: String, cause: Throwable) : CaseAssignmentStorageException(message, cause)

    class CorruptRecord(val evidenceArtifactId: EvidenceArtifactId, message: String, cause: Throwable? = null) :
        CaseAssignmentStorageException("Case assignment for '${evidenceArtifactId.value}' is corrupt: $message", cause)
}

/**
 * CASE-1. The narrow persistence primitive for the current
 * evidence-to-case assignment. Deliberately offers no "evidence IDs for
 * case X" or "unassigned evidence IDs" index and no generic listing of
 * any kind -- CASE-1's own storage-narrowness requirement: evidence
 * enumeration remains the existing Owner Evidence UI/domain's own
 * responsibility; this store answers exactly one question, "what is this
 * one exact evidence artifact's current case assignment," and nothing
 * else. A caller needing "which evidence belongs to case X" joins this
 * store's per-evidence answer against the evidence listing it already
 * has, at the scale CASE-1's own bounded evidence library requires.
 */
interface CaseAssignmentStorage {
    /** The current assignment for [evidenceArtifactId], or `null` if it has never been touched (Unassigned by default, no record needed for that state). */
    suspend fun readAssignment(evidenceArtifactId: EvidenceArtifactId): CaseAssignmentRecord?

    /** Durably overwrites the current assignment for [CaseAssignmentRecord.evidenceArtifactId] with [record] -- the one governed mutation primitive this store exposes. */
    suspend fun writeAssignment(record: CaseAssignmentRecord)
}

/** CASE-1's own closed audit-event vocabulary -- exactly the three facts this unit's own governance requires, nothing broader. */
enum class CaseGovernanceAuditEventType {
    CASE_CREATED,
    EVIDENCE_ASSIGNED,
    EVIDENCE_REASSIGNED,
}

/**
 * CASE-1. One durable audit fact. [caseId] is the new/created case for
 * every event type; [previousCaseId] is populated only for
 * [CaseGovernanceAuditEventType.EVIDENCE_REASSIGNED] (the assignment that
 * existed immediately before this change); [evidenceArtifactId] is `null`
 * only for [CaseGovernanceAuditEventType.CASE_CREATED]. Never carries
 * evidence content, case notes, or anything beyond these identity/actor/
 * timestamp facts.
 */
data class CaseGovernanceAuditRecord(
    val eventType: CaseGovernanceAuditEventType,
    val caseId: CaseId?,
    val previousCaseId: CaseId? = null,
    val evidenceArtifactId: EvidenceArtifactId? = null,
    val actorPrincipalId: PrincipalId,
    val recordedAt: Instant,
) {
    init {
        require((eventType == CaseGovernanceAuditEventType.CASE_CREATED) == (evidenceArtifactId == null)) {
            "CaseGovernanceAuditRecord.evidenceArtifactId must be present for every event type except CASE_CREATED"
        }
        require(eventType == CaseGovernanceAuditEventType.EVIDENCE_REASSIGNED || previousCaseId == null) {
            "CaseGovernanceAuditRecord.previousCaseId is meaningful only for EVIDENCE_REASSIGNED"
        }
        require(eventType != CaseGovernanceAuditEventType.EVIDENCE_REASSIGNED || previousCaseId != null) {
            "CaseGovernanceAuditRecord.previousCaseId must be present for EVIDENCE_REASSIGNED -- a reassignment always has a prior case"
        }
        require(eventType != CaseGovernanceAuditEventType.CASE_CREATED || caseId != null) {
            "CaseGovernanceAuditRecord.caseId must be present for CASE_CREATED"
        }
        require(eventType != CaseGovernanceAuditEventType.EVIDENCE_ASSIGNED || caseId != null) {
            "CaseGovernanceAuditRecord.caseId must be present for EVIDENCE_ASSIGNED -- a first assignment is always to a real case, never to Unassigned"
        }
    }
}

sealed class CaseGovernanceAuditException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class PersistenceFailure(message: String, cause: Throwable) : CaseGovernanceAuditException(message, cause)
}

/** CASE-1's narrow, append-only audit port -- mirroring [DocumentIngestionAudit]'s own single-method write-only shape exactly; no read-back capability exists because none is required. */
fun interface CaseGovernanceAudit {
    suspend fun record(record: CaseGovernanceAuditRecord)
}

/**
 * CASE-1. Mirrors [EvidenceArtifactIdentifierSafety]'s own rule exactly -- [CaseId] is always
 * server-minted (never owner-typed, per this unit's own "the owner must not type CaseId manually"
 * requirement) so it always already satisfies this pattern by construction; this check exists as
 * the same defensive storage-boundary discipline every identifier-keyed store in this codebase
 * applies before ever deriving a filesystem path from a caller-supplied value.
 */
internal object CaseIdentifierSafety {
    private val SAFE_IDENTIFIER_PATTERN = Regex("^[a-z0-9_-]+$")

    fun requireSafe(caseId: CaseId) {
        if (!SAFE_IDENTIFIER_PATTERN.matches(caseId.value)) {
            throw CaseStorageException.UnsafeIdentifier(caseId)
        }
    }
}
