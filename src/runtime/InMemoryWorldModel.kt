package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.ObservationResult
import parker.core.interfaces.WorldBelief
import parker.core.interfaces.WorldModel
import parker.core.interfaces.WorldModelUpdatePolicy
import parker.core.interfaces.WorldObservation
import parker.core.interfaces.WorldQuery

/**
 * Sprint 4, Track B, Unit B3. The first in-memory implementation of
 * [WorldModel] (`docs/architecture/WORLD_MODEL_RUNTIME_ARCHITECTURE.md`,
 * `docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md`). Implements
 * [WorldModel] directly -- no separate `WorldModelRuntime` interface
 * exists to implement instead, per `WORLD_MODEL_CONTRACT_DESIGN.md` §6's
 * own determination -- mirroring [InMemoryMemoryStore]/
 * [InMemoryIdentityService]'s identical "one interface, one implementing
 * class" precedent.
 *
 * ## Boundary this class enforces
 *
 * [observe] performs Validation, evaluation (via the injected
 * [WorldModelUpdatePolicy]), and Update/Invalidation in one call. An
 * external caller submits a [WorldObservation] and learns the outcome;
 * [WorldModelUpdatePolicy] is never reachable from outside this class,
 * exactly as `WORLD_MODEL_CONTRACT_DESIGN.md` §5 requires.
 *
 * ## Concurrency
 *
 * All three operations acquire [mutex] before touching [beliefs].
 * Concurrent Observations for the same subject are resolved entirely
 * inside this class, serialised by [mutex] -- callers never coordinate
 * updates themselves (`WORLD_MODEL_CONTRACT_DESIGN.md`, Concurrency
 * section).
 *
 * ## What this class does not do
 *
 * Per this Unit's explicit scope, this class does not implement:
 * storage engines, databases, graph technology, embeddings, retrieval
 * algorithms beyond the minimal, deterministic one described on [query],
 * networking, persistence, Android APIs, LLM prompts, any dependency on
 * Memory, the Planner Runtime, the Agent Runtime, or the Permission
 * Engine, and no `EventBus` publication (see
 * `docs/reviews/SPRINT_4_TRACK_B_UNIT_B3_POST_IMPLEMENTATION_REVIEW.md`
 * for why this is a disclosed, open gap rather than a silent omission).
 * Its constructor takes only a [WorldModelUpdatePolicy] (defaulted to
 * [DefaultWorldModelUpdatePolicy]).
 *
 * There is no autonomous background expiry sweep: a stale [WorldBelief]
 * is simply never removed from [beliefs] proactively -- it is only ever
 * excluded, lazily, the next time [current] or [query] consults
 * [WorldModelUpdatePolicy.isStillCurrent]. This is deliberate, not an
 * oversight: `WorldBelief` retains no history
 * (`WORLD_MODEL_CONTRACT_DESIGN.md` §1), so a stale entry sitting
 * unread is harmless, and it is replaced the moment a fresh Observation
 * for the same subject is accepted.
 */
class InMemoryWorldModel(
    private val updatePolicy: WorldModelUpdatePolicy = DefaultWorldModelUpdatePolicy(),
) : WorldModel {

    private val mutex = Mutex()
    private val beliefs = mutableMapOf<String, WorldBelief>()

    override suspend fun observe(observation: WorldObservation): ObservationResult = mutex.withLock {
        val existing = beliefs[observation.subject]
        when (val result = updatePolicy.evaluate(observation, existing)) {
            is ObservationResult.Accepted -> {
                beliefs[observation.subject] = result.belief
                result
            }

            is ObservationResult.Invalidated -> {
                beliefs.remove(observation.subject)
                result
            }

            is ObservationResult.Rejected -> result
        }
    }

    override suspend fun current(subject: String): WorldBelief? {
        require(subject.isNotBlank()) { "current(subject) requires a non-blank subject" }
        return mutex.withLock {
            val belief = beliefs[subject] ?: return@withLock null
            if (updatePolicy.isStillCurrent(belief)) belief else null
        }
    }

    /**
     * The minimal, deterministic matching this Unit is scoped to
     * implement: a case-insensitive substring match of
     * [WorldQuery.subjectMatch] against [WorldBelief.subject], narrowed
     * by [WorldQuery.minimumConfidence] if supplied, excluding any
     * belief [WorldModelUpdatePolicy.isStillCurrent] judges stale, and
     * truncated to [WorldQuery.maximumResults]. No ranking or scoring
     * formula is applied -- results are returned in whatever order the
     * underlying map iterates, per `WORLD_MODEL_CONTRACT_DESIGN.md` §4's
     * own "what it must not carry" rule; a caller must not depend on any
     * particular ordering beyond the filters and bound stated here.
     */
    override suspend fun query(query: WorldQuery): List<WorldBelief> = mutex.withLock {
        beliefs.values
            .filter { belief ->
                belief.subject.contains(query.subjectMatch, ignoreCase = true) &&
                    (query.minimumConfidence == null || belief.confidence >= query.minimumConfidence) &&
                    updatePolicy.isStillCurrent(belief)
            }
            .take(query.maximumResults)
    }
}
