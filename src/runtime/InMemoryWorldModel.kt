package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.ObservationResult
import parker.core.interfaces.WorldBelief
import parker.core.interfaces.WorldModel
import parker.core.interfaces.WorldModelSource
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
 *
 * ## World Model Source (Sprint 11 Unit 8)
 *
 * Also implements [WorldModelSource] directly -- a second, narrower
 * interface over this exact same instance and the exact same owned state,
 * not a second store (`docs/architecture/WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md`
 * Section 2.3), mirroring precisely how this class's own sibling
 * `InMemoryMemoryStore` implements `MemoryStore` and `MemorySource`
 * together. [recall] is a direct, zero-logic delegate to [query] -- no new
 * map, no new lock, no new field, and no duplicated filtering logic: the
 * one authoritative subject-matching, confidence-filtering, and
 * staleness-exclusion behaviour [query] already implements is the only
 * such behaviour this class has, and [recall] simply calls it.
 */
class InMemoryWorldModel(
    private val updatePolicy: WorldModelUpdatePolicy = DefaultWorldModelUpdatePolicy(),
) : WorldModel, WorldModelSource {

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
     * [WorldQuery.subjectMatch] against [WorldBelief.subject] --
     * skipped entirely when [WorldQuery.subjectMatch] is `null`, per
     * `docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md`
     * -- narrowed by [WorldQuery.minimumConfidence] if supplied,
     * excluding any belief [WorldModelUpdatePolicy.isStillCurrent]
     * judges stale, and truncated to [WorldQuery.maximumResults]. No
     * ranking or scoring formula is applied -- results are returned in
     * whatever order the underlying map iterates, per
     * `WORLD_MODEL_CONTRACT_DESIGN.md` §4's own "what it must not carry"
     * rule; a caller must not depend on any particular ordering beyond
     * the filters and bound stated here. A `null` `subjectMatch` does
     * not introduce ranking, scoring, inference, or topic extraction --
     * it removes one filter condition, exactly as a `null`
     * [WorldQuery.minimumConfidence] already does for the confidence
     * condition.
     */
    override suspend fun query(query: WorldQuery): List<WorldBelief> = mutex.withLock {
        beliefs.values
            .filter { belief ->
                (query.subjectMatch == null || belief.subject.contains(query.subjectMatch, ignoreCase = true)) &&
                    (query.minimumConfidence == null || belief.confidence >= query.minimumConfidence) &&
                    updatePolicy.isStillCurrent(belief)
            }
            .take(query.maximumResults)
    }

    /**
     * [WorldModelSource]'s own single operation -- a direct, zero-logic
     * delegate to [query]. Named `recall`, not `query`, so a caller
     * holding only a [WorldModelSource] reference is never confused for
     * one holding a full [WorldModel] reference, even though the
     * underlying behaviour is identical
     * (`WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md` Section 2.1).
     */
    override suspend fun recall(query: WorldQuery): List<WorldBelief> = query(query)
}
