package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingResultRecordOutcome
import parker.core.interfaces.HermesProcessingResultRegistry

/**
 * Hermes Processing Result Intake, Task 2. A mutex-guarded in-memory store, mirroring
 * [InMemoryDerivativeReviewRegistry]'s own locking convention.
 *
 * **Durability, explicitly scoped down for this unit** (same disclosure
 * [InMemoryDerivativeReviewRegistry] already makes for its own, structurally identical, first
 * unit): a process restart loses every recorded [HermesProcessingResult]. This is a materially
 * smaller failure mode than the deletion audit's own irreversible-action correctness requirement
 * -- a lost record is recoverable by Hermes simply retrying the same submission, which this
 * class's own idempotency guarantee (see [record]) makes safe to do -- which is why this first
 * unit does not require a durable, file-backed implementation the way, for example,
 * `FileSystemCaseAssignmentStorage`'s own durable state does. Durable persistence may be added
 * later behind this same [HermesProcessingResultRegistry] contract without changing it.
 */
class InMemoryHermesProcessingResultRegistry : HermesProcessingResultRegistry {

    private val mutex = Mutex()
    private val records = mutableMapOf<Key, HermesProcessingResult>()

    override suspend fun record(result: HermesProcessingResult): HermesProcessingResultRecordOutcome = mutex.withLock {
        val key = Key(result.batchId, result.sourceSha256)
        when (val existing = records[key]) {
            null -> {
                records[key] = result
                HermesProcessingResultRecordOutcome.Recorded(result)
            }
            result -> HermesProcessingResultRecordOutcome.AlreadyRecorded(existing)
            else -> HermesProcessingResultRecordOutcome.Conflict(existing, result)
        }
    }

    override suspend fun find(batchId: String, sourceSha256: String): HermesProcessingResult? = mutex.withLock {
        records[Key(batchId, sourceSha256)]
    }

    override suspend fun listForBatch(batchId: String): List<HermesProcessingResult> = mutex.withLock {
        records.entries.filter { it.key.batchId == batchId }.map { it.value }
    }

    private data class Key(val batchId: String, val sourceSha256: String)
}
