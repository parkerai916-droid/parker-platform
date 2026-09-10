package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.HermesProcessingDecisionRegistry
import parker.core.interfaces.HermesProcessingHumanDecision

/**
 * Hermes Exception Decision Backend, Task 4. A mutex-guarded in-memory store, mirroring
 * [InMemoryHermesProcessingResultRegistry]'s own locking convention.
 *
 * **Durability, explicitly scoped down for this unit** (same disclosure
 * [InMemoryHermesProcessingResultRegistry] already makes for its own, structurally identical,
 * Task 2 unit): a process restart loses every recorded [HermesProcessingHumanDecision]. This is a
 * larger disclosed gap than Task 2's own: an Owner decision is a genuine, deliberate human act,
 * not a safely-retriable machine submission, so a lost decision after restart is not
 * self-healing the way a lost, idempotent Hermes retry is. Durable persistence is deferred to a
 * later task (this unit's own R0 scope explicitly excludes it -- see the Task 4 return report's
 * own "Risks / Deviations" section) and may be added later behind this same
 * [HermesProcessingDecisionRegistry] contract without changing it.
 */
class InMemoryHermesProcessingDecisionRegistry : HermesProcessingDecisionRegistry {

    private val mutex = Mutex()
    private val decisions = mutableMapOf<Key, MutableList<HermesProcessingHumanDecision>>()

    override suspend fun record(decision: HermesProcessingHumanDecision): HermesProcessingHumanDecision = mutex.withLock {
        val key = Key(decision.batchId, decision.sourceSha256)
        decisions.getOrPut(key) { mutableListOf() }.add(decision)
        decision
    }

    override suspend fun latest(batchId: String, sourceSha256: String): HermesProcessingHumanDecision? = mutex.withLock {
        decisions[Key(batchId, sourceSha256)]?.lastOrNull()
    }

    override suspend fun history(batchId: String, sourceSha256: String): List<HermesProcessingHumanDecision> = mutex.withLock {
        decisions[Key(batchId, sourceSha256)]?.toList() ?: emptyList()
    }

    private data class Key(val batchId: String, val sourceSha256: String)
}
