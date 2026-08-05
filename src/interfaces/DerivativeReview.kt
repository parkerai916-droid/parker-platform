package parker.core.interfaces

import java.time.Instant

/**
 * Evidence Processing (Searchable PDF), Implementation Unit 3 ("Human
 * Review Registry"). Governed in full by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
 * ("the Boundary Clarification") Section 6; by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 7; and by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 3. Mirrors [OwnerEvidenceDeletionAuthority]'s
 * own "narrow, purpose-built port; no dependency of its own" shape exactly.
 *
 * ## A new, narrow, standalone contract
 *
 * Not a Memory Core record kind, not an Evidence Custodian operation, not a
 * general workflow engine (Boundary Clarification Section 6). This file
 * holds no structural dependency on [EvidenceCustodian] or [MemoryCore],
 * and is held by neither in return.
 *
 * ## Gating rule (disclosed here, enforced by a future Runtime Integration
 * unit, not this one)
 *
 * No consumer -- Evidence Intelligence once resumed, or any other
 * downstream capability -- may treat a derivative's extracted text as
 * human-verified unless [DerivativeReviewRegistry.currentReviewState] for
 * that derivative's own [EvidenceArtifactId] returns exactly [DerivativeReviewState.APPROVED].
 * Absent that check, or for any other state, the text must be treated as
 * unverified machine-extracted output only.
 */

/**
 * Exactly four values. No fifth value, and no caller-defined open
 * classification -- a closed, exhaustive enumeration, deliberately unlike
 * Memory Core's own open `entityType`/`documentType` strings, because a
 * downstream gate must be able to test for exactly [APPROVED] without
 * needing to know every string a future caller might invent (Boundary
 * Clarification Section 6).
 */
enum class DerivativeReviewState {
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    NEEDS_CORRECTION,
}

/**
 * One flat review-lifecycle fact. [reviewingPrincipalId] is `null` for the
 * initial, system-recorded [DerivativeReviewState.PENDING_REVIEW] -- no
 * human reviewer performed that transition. [note] is a reviewer's own
 * free-text reason for [DerivativeReviewState.REJECTED] or
 * [DerivativeReviewState.NEEDS_CORRECTION], optional even then.
 */
data class DerivativeReviewRecord(
    val evidenceArtifactId: EvidenceArtifactId,
    val documentId: DocumentId,
    val state: DerivativeReviewState,
    val recordedAt: Instant,
    val reviewingPrincipalId: PrincipalId? = null,
    val note: String? = null,
)

/**
 * Exactly two operations. Recording a new review state validates the
 * attempted transition against a fixed graph (enforced by whatever
 * implements this interface, never by [DerivativeReviewState] itself,
 * mirroring [MemoryCoreLifecycleTransitions]'s own identical discipline)
 * and throws [IllegalArgumentException] on an invalid attempt, mirroring
 * [MemoryCoreLifecycleTransitions.requireValidTransition]'s own
 * already-established, directly-on-point precedent -- never a new outcome
 * type or exception hierarchy (Implementation Plan Unit 3). Recording
 * itself returns nothing on success.
 *
 * Reading the current state for an identifier with no review record at all
 * returns an explicit `null` -- a distinct, and itself reportable,
 * condition from one genuinely at [DerivativeReviewState.PENDING_REVIEW]
 * (Boundary Clarification Section 6).
 */
interface DerivativeReviewRegistry {
    suspend fun recordReviewState(record: DerivativeReviewRecord)

    suspend fun currentReviewState(evidenceArtifactId: EvidenceArtifactId): DerivativeReviewState?
}
