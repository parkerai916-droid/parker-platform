package parker.core.interfaces

/**
 * Retains [HermesProcessingResult] records for the batch/source-hash keys they were submitted
 * under, on the same, explicit pre-ingestion side of the "RAW SOURCE -> HERMES PROCESSING ->
 * HermesProcessingResult" boundary [HermesProcessingResult]'s own KDoc establishes. Never mixed
 * into [EvidenceCustodian], [DerivativeReviewRegistry], or any post-ingestion review registry --
 * recording a result here confers no governed-evidence fact and mutates no post-ingestion review
 * state of any kind.
 *
 * Keyed by (batchId, sourceSha256): a batch may carry many sources, and Hermes may retry a
 * submission for the same source within the same batch any number of times (network
 * uncertainty). [record] is therefore idempotent for a byte-identical retry and fails closed --
 * never silently overwrites -- for a changed result under the same key. Reprocessing a source
 * with a genuinely different result is a separate, future, explicit lifecycle (not implemented
 * here); this registry only ever tells a caller the two results disagree.
 */
interface HermesProcessingResultRegistry {

    /**
     * Records [result] under its own (`result.batchId`, `result.sourceSha256`) key.
     *
     * - No prior record exists for that key: durably stores [result] and returns
     *   [HermesProcessingResultRecordOutcome.Recorded].
     * - An identical ([HermesProcessingResult.equals]) record already exists for that key: stores
     *   nothing new and returns [HermesProcessingResultRecordOutcome.AlreadyRecorded] -- the
     *   ordinary outcome of a Hermes retry.
     * - A different record already exists for that key: stores nothing, does not alter the
     *   existing record, and returns [HermesProcessingResultRecordOutcome.Conflict].
     */
    suspend fun record(result: HermesProcessingResult): HermesProcessingResultRecordOutcome

    /** The record stored for ([batchId], [sourceSha256]), or `null` if none exists. */
    suspend fun find(batchId: String, sourceSha256: String): HermesProcessingResult?

    /** Every record stored for [batchId], in no particular guaranteed order. Never crosses batches. */
    suspend fun listForBatch(batchId: String): List<HermesProcessingResult>

    /**
     * Hermes Exception Decision Backend, Task 4. Every record stored across every batch, in no
     * particular guaranteed order -- the narrow read this task's own cross-batch Owner review
     * queue needs (`GET /owner/hermes-processing/review`) that [listForBatch] cannot provide.
     * Never leaks any fact this registry does not already hold (no case identity, no provenance).
     */
    suspend fun listAll(): List<HermesProcessingResult>
}

/** What [HermesProcessingResultRegistry.record] returns. */
sealed class HermesProcessingResultRecordOutcome {

    /** No prior record existed for this key; [result] is now the one durably retained under it. */
    data class Recorded(val result: HermesProcessingResult) : HermesProcessingResultRecordOutcome()

    /** A byte-for-byte identical record was already retained under this key -- an ordinary retry, not a duplicate. */
    data class AlreadyRecorded(val result: HermesProcessingResult) : HermesProcessingResultRecordOutcome()

    /** A different record was already retained under this key. [existing] is unchanged; [attempted] was not stored. */
    data class Conflict(val existing: HermesProcessingResult, val attempted: HermesProcessingResult) : HermesProcessingResultRecordOutcome()
}
