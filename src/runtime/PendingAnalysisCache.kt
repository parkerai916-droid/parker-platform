package parker.core.runtime

import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.OwnerDocumentAnalysisResult
import parker.core.interfaces.PendingAnalysisId

/**
 * Reviewed Analysis Result — Explicit Owner Save. A small, bounded,
 * TTL-expiring, in-memory-only cache of already-completed
 * [OwnerDocumentAnalysisResult]s awaiting an explicit owner Save decision.
 * This is the entire anti-forgery mechanism: the browser never resubmits
 * analysis text to Save it, so nothing this coordinator persists can ever
 * be client-fabricated -- it is always exactly the object Parker already
 * produced and registered here. Deliberately never durable -- if Parker
 * restarts before the owner saves, the pending entry is gone, which is
 * correct: the owner never asked for durability, only the analysis itself
 * did not yet warrant it.
 *
 * Not a distributed/session framework -- one process-local map, one mutex,
 * opportunistic expiry on each operation, no background thread.
 *
 * ## One-shot / concurrency-safe Save semantics
 *
 * [claim] marks an entry `IN_FLIGHT` and returns its result; a concurrent
 * second [claim] on the same id fails fast ([ClaimOutcome.AlreadyInFlight])
 * rather than allowing two Save attempts to race for the same reviewed
 * result. [finalize]
 * removes the entry permanently -- called only after durable publication
 * genuinely succeeds. [release] returns an `IN_FLIGHT` entry to `AVAILABLE`
 * -- called when persistence fails, so a legitimate retry with the same
 * [PendingAnalysisId] remains possible without ever risking two saved
 * records for one reviewed result.
 */
class PendingAnalysisCache(
    private val ttl: Duration = DEFAULT_TTL,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val now: () -> Instant = Instant::now,
    private val idFactory: () -> PendingAnalysisId = { PendingAnalysisId(UUID.randomUUID().toString()) },
) {
    private enum class State { AVAILABLE, IN_FLIGHT }
    private class Entry(val result: OwnerDocumentAnalysisResult, val expiresAt: Instant, var state: State)

    private val mutex = Mutex()
    private val entries = LinkedHashMap<PendingAnalysisId, Entry>()

    /** Registers a newly completed analysis, returning the fresh [PendingAnalysisId] it may later be saved under. Always succeeds -- evicts the single oldest entry first if already at [maxEntries]. */
    suspend fun register(result: OwnerDocumentAnalysisResult): PendingAnalysisId = mutex.withLock {
        evictExpiredLocked()
        if (entries.size >= maxEntries) {
            entries.keys.firstOrNull()?.let { entries.remove(it) }
        }
        val id = idFactory()
        entries[id] = Entry(result, now().plus(ttl), State.AVAILABLE)
        id
    }

    /** The truthful result of one [claim] attempt -- distinguishes "never existed / expired / already consumed" from "a concurrent Save is already in progress for this exact id," since a caller needs to report those two cases differently. */
    sealed class ClaimOutcome {
        data class Claimed(val result: OwnerDocumentAnalysisResult) : ClaimOutcome()
        data object UnknownOrExpired : ClaimOutcome()
        data object AlreadyInFlight : ClaimOutcome()
    }

    /** Atomically claims an `AVAILABLE` entry for an in-progress Save. */
    suspend fun claim(id: PendingAnalysisId): ClaimOutcome = mutex.withLock {
        evictExpiredLocked()
        val entry = entries[id] ?: return@withLock ClaimOutcome.UnknownOrExpired
        if (entry.state == State.IN_FLIGHT) return@withLock ClaimOutcome.AlreadyInFlight
        entry.state = State.IN_FLIGHT
        ClaimOutcome.Claimed(entry.result)
    }

    /** Permanently removes a successfully, durably saved entry -- the one-shot guarantee: a later [claim] on the same id returns `null`. */
    suspend fun finalize(id: PendingAnalysisId) {
        mutex.withLock { entries.remove(id) }
    }

    /** Returns an `IN_FLIGHT` entry to `AVAILABLE` after a failed Save attempt, so a legitimate retry with the same id remains possible. */
    suspend fun release(id: PendingAnalysisId) {
        mutex.withLock { entries[id]?.let { if (it.state == State.IN_FLIGHT) it.state = State.AVAILABLE } }
    }

    private fun evictExpiredLocked() {
        val nowValue = now()
        entries.entries.removeAll { it.value.expiresAt.isBefore(nowValue) }
    }

    companion object {
        /** Long enough for an owner to read a completed analysis and decide whether to save it; short enough to bound memory for a capability that is deliberately never meant to be durable on its own. */
        val DEFAULT_TTL: Duration = Duration.ofMinutes(15)

        /** A modest, frozen bound on simultaneously pending (unsaved) analyses -- not an enormous theoretical one, matching `DocumentAnalysisCoordinator.MAX_SELECTIONS`'s own scale. */
        const val DEFAULT_MAX_ENTRIES: Int = 20
    }
}
