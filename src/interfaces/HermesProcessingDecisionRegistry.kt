package parker.core.interfaces

/**
 * Hermes Exception Decision Backend, Task 4. Retains [HermesProcessingHumanDecision] records for
 * the (`batchId`, `sourceSha256`) keys they were recorded under -- the pre-ingestion, Owner-facing
 * counterpart to [HermesProcessingResultRegistry] (which retains the *machine* fact this registry's
 * own records decide against). Never mixed into [HermesProcessingResultRegistry], any post-ingestion
 * review registry, or any case/evidence/provenance store: recording a decision here mutates no
 * other registry's state, and no other registry's state is ever read or written by this one.
 *
 * **Append-only.** Unlike [HermesProcessingResultRegistry.record] (which fails closed on a
 * conflicting re-submission for the same key, since a changed Hermes result under an unchanged key
 * would be a genuine inconsistency), [record] here always appends a new entry to that key's
 * history -- a second, later Owner decision against the same source is an ordinary, expected event
 * (Steve reconsidering, or reacting to a later reprocessing round), never a conflict to reject.
 * Nothing already recorded is ever edited or removed; superseding an earlier decision means
 * recording a new one, never rewriting history.
 */
interface HermesProcessingDecisionRegistry {

    /** Durably appends [decision] to the history for its own (`batchId`, `sourceSha256`) key and returns it unchanged. */
    suspend fun record(decision: HermesProcessingHumanDecision): HermesProcessingHumanDecision

    /** The most recently recorded decision for (`batchId`, `sourceSha256`), or `null` if none exists. */
    suspend fun latest(batchId: String, sourceSha256: String): HermesProcessingHumanDecision?

    /** Every decision recorded for (`batchId`, `sourceSha256`), oldest first. Never crosses keys. */
    suspend fun history(batchId: String, sourceSha256: String): List<HermesProcessingHumanDecision>
}
