package parker.core.interfaces

import java.time.Instant

/**
 * Evidence Custodian, Implementation Plan Phase 7 ("Deletion workflow").
 * Governed by `docs/architecture/EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`
 * ("the Boundary Clarification"), Sections 2, 4, and 6-7. A narrow,
 * purpose-built durability port -- not `AuditService`
 * (`src/interfaces/AuditService.kt`), which has zero implementations
 * anywhere in this repository and whose first implementation this
 * Programme is not authorised to write (Boundary Clarification Section
 * 2). This port exists solely to discharge Evidence Custodian's own
 * frozen "audited" obligation (Scope Lock Section 7) for exactly one
 * operation -- owner-authorised deletion -- and for nothing else.
 *
 * ## Exactly one record type, one stage field, two values
 *
 * [EvidenceDeletionAuditRecord] is the only record type this port ever
 * produces. [EvidenceDeletionAuditStage] carries exactly two values,
 * [EvidenceDeletionAuditStage.AUTHORISED] and
 * [EvidenceDeletionAuditStage.COMPLETED] -- no `FAILED`, `REQUESTED`,
 * `NOT_FOUND`, or any other value is introduced (Boundary Clarification
 * Section 4's own explicit instruction). The same record type is
 * durably written twice per successful deletion, distinguished only by
 * [EvidenceDeletionAuditRecord.stage] -- never as two different types.
 *
 * ## Ordering this port's own caller ([parker.core.runtime.DefaultOwnerEvidenceDeletionAuthority])
 * must honour
 *
 * A record with `stage = AUTHORISED` must be durably confirmed written
 * *before* [EvidenceArtifactStorage.delete] is ever called -- this is
 * what closes the crash-window failure mode the Boundary Clarification
 * (Section 6, Determination 5) requires closed: a successful deletion
 * must never exist without durable evidence, unconditionally, that it
 * was authorised. A record with `stage = COMPLETED` is written only
 * after physical deletion has already succeeded, and only a
 * successfully durable `COMPLETED` write allows
 * [OwnerEvidenceDeletionAuthority.deleteAsOwner] to return
 * [EvidenceDeletionResult.Deleted]. This port itself enforces neither
 * ordering rule -- it is a pure write primitive -- the ordering is
 * [parker.core.runtime.DefaultOwnerEvidenceDeletionAuthority]'s own
 * responsibility, exactly as [EvidenceArtifactStorage] is a pure
 * write/read primitive and [parker.core.runtime.DefaultEvidenceCustodian]
 * alone enforces permission-gating order.
 *
 * ## No query capability
 *
 * [EvidenceDeletionAudit] declares no read/query operation -- Boundary
 * Clarification Section 2 explicitly excludes one. Durability, not
 * retrievability by this Programme's own runtime, is this port's entire
 * purpose; a human reviewer inspects the durable record directly.
 */
enum class EvidenceDeletionAuditStage {
    AUTHORISED,
    COMPLETED,
}

/**
 * One durable fact about one deletion attempt, at one of exactly two
 * points in its lifecycle (see [EvidenceDeletionAuditStage]).
 * [deletionRequestId] is minted once, by
 * [parker.core.runtime.DefaultOwnerEvidenceDeletionAuthority], per
 * [OwnerEvidenceDeletionAuthority.deleteAsOwner] call, and correlates
 * the (at most two) records one attempt produces -- it is a correlation
 * value only, never a domain identity type, and carries no meaning
 * beyond linking these records to one another.
 */
data class EvidenceDeletionAuditRecord(
    val deletionRequestId: String,
    val evidenceArtifactId: EvidenceArtifactId,
    val requestingPrincipalId: PrincipalId,
    val recordedAt: Instant,
    val stage: EvidenceDeletionAuditStage,
) {
    init {
        require(deletionRequestId.isNotBlank()) { "EvidenceDeletionAuditRecord.deletionRequestId must not be blank" }
    }
}

/**
 * Typed failure for [EvidenceDeletionAudit.record] -- thrown, not
 * returned as a result type, mirroring [EvidenceArtifactStorageException]'s
 * own established convention for a genuine, unexpected fault at this
 * layer (as opposed to an ordinary, expected outcome).
 */
sealed class EvidenceDeletionAuditException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {

    /** A durable write genuinely failed -- disk I/O, permissions, or an equivalent fault. */
    class PersistenceFailure(message: String, cause: Throwable) : EvidenceDeletionAuditException(message, cause)
}

/**
 * The one operation this port exposes. Durably persists [record] before
 * returning; throws [EvidenceDeletionAuditException.PersistenceFailure]
 * if it cannot. Never overwrites or removes a previously written record
 * -- append-only, exactly as Scope Lock Section 7's "durable,
 * append-only audit record" requires.
 */
interface EvidenceDeletionAudit {
    suspend fun record(record: EvidenceDeletionAuditRecord)
}
