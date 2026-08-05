package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.DerivativeReviewRecord
import parker.core.interfaces.DerivativeReviewRegistry
import parker.core.interfaces.DerivativeReviewState
import parker.core.interfaces.EvidenceArtifactId

/**
 * Evidence Processing (Searchable PDF), Implementation Unit 3 ("Human
 * Review Registry"). Governed in full by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
 * ("the Boundary Clarification") Section 6, "as corrected: terminal
 * needs-correction"; by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 7; and by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 3.
 *
 * An append-only, mutex-guarded in-memory store, mirroring
 * [InMemoryMemoryCore]'s own locking convention. Recording a new state
 * looks up the most recently recorded state for the target identifier
 * (absent if none), validates the transition via
 * [DerivativeReviewTransitions.requireValidTransition], and only then
 * appends the new record. Nothing already appended is ever mutated or
 * removed. Reading the current state returns the most recently appended
 * record's own state for that identifier, or `null` if none exists.
 *
 * **Durability, explicitly scoped down for this first unit** (Boundary
 * Clarification Section 6): a process restart loses in-flight review
 * state, recoverable by re-review -- a materially smaller failure mode
 * than the deletion audit's own irreversible-action correctness
 * requirement, which is why that capability required a durable,
 * file-backed implementation and this one does not. Durability may be
 * added later without changing this contract.
 */
class InMemoryDerivativeReviewRegistry : DerivativeReviewRegistry {

    private val mutex = Mutex()
    private val history = mutableMapOf<EvidenceArtifactId, MutableList<DerivativeReviewRecord>>()

    override suspend fun recordReviewState(record: DerivativeReviewRecord) {
        mutex.withLock {
            val existing = history.getOrPut(record.evidenceArtifactId) { mutableListOf() }
            val currentState = existing.lastOrNull()?.state
            DerivativeReviewTransitions.requireValidTransition(currentState, record.state)
            existing.add(record)
        }
    }

    override suspend fun currentReviewState(evidenceArtifactId: EvidenceArtifactId): DerivativeReviewState? =
        mutex.withLock {
            history[evidenceArtifactId]?.lastOrNull()?.state
        }
}

/**
 * The fixed transition graph [InMemoryDerivativeReviewRegistry] enforces --
 * never enforced by [DerivativeReviewState] itself, mirroring
 * [MemoryCoreLifecycleTransitions]'s own identical discipline exactly
 * (`src/runtime/InMemoryMemoryCore.kt`). `null` (no prior record for an
 * identifier) may only move to [DerivativeReviewState.PENDING_REVIEW].
 * [DerivativeReviewState.PENDING_REVIEW] may move to [DerivativeReviewState.APPROVED],
 * [DerivativeReviewState.REJECTED], or [DerivativeReviewState.NEEDS_CORRECTION].
 * Every other pair -- including any transition away from [DerivativeReviewState.APPROVED],
 * [DerivativeReviewState.REJECTED], or [DerivativeReviewState.NEEDS_CORRECTION]
 * (all three terminal for their own identifier) -- is rejected. A
 * "correction" is always a new coordinator run producing a new, separately
 * identified derivative starting its own `PENDING_REVIEW`, never a
 * transition back on the same identifier (Boundary Clarification Section
 * 6, as corrected).
 */
internal object DerivativeReviewTransitions {

    private val allowed: Map<DerivativeReviewState?, Set<DerivativeReviewState>> = mapOf(
        null to setOf(DerivativeReviewState.PENDING_REVIEW),
        DerivativeReviewState.PENDING_REVIEW to setOf(
            DerivativeReviewState.APPROVED,
            DerivativeReviewState.REJECTED,
            DerivativeReviewState.NEEDS_CORRECTION,
        ),
        DerivativeReviewState.APPROVED to emptySet(),
        DerivativeReviewState.REJECTED to emptySet(),
        DerivativeReviewState.NEEDS_CORRECTION to emptySet(),
    )

    fun isValidTransition(from: DerivativeReviewState?, to: DerivativeReviewState): Boolean =
        to in allowed.getValue(from)

    fun requireValidTransition(from: DerivativeReviewState?, to: DerivativeReviewState) {
        require(isValidTransition(from, to)) {
            "Illegal derivative review transition: $from -> $to"
        }
    }
}
