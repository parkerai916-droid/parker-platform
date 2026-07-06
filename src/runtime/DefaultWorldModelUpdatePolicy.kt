package parker.core.runtime

import java.time.Duration
import java.time.Instant
import parker.core.interfaces.ObservationResult
import parker.core.interfaces.WorldBelief
import parker.core.interfaces.WorldModelUpdatePolicy
import parker.core.interfaces.WorldObservation

/**
 * Sprint 4, Track B, Unit B3. The concrete, deterministic
 * [WorldModelUpdatePolicy] this Unit supplies, mirroring
 * `DefaultMemoryPromotionPolicy`'s own role for Memory and
 * `DefaultPlanDecision`'s own role for the Planner Runtime.
 *
 * ## Structural validity is not this class's concern
 *
 * Blank subject, blank value (outside a retraction), and out-of-range
 * confidence are already rejected at [WorldObservation]'s own
 * construction time (a thrown `IllegalArgumentException`), exactly as
 * `CandidateMemory` rejects the same class of problem before a
 * `MemoryPromotionPolicy` is ever consulted. By the time an Observation
 * reaches [evaluate], it is already known to be well-formed; this
 * policy's own decisions concern currency, never malformation.
 *
 * ## The four rules this class implements
 *
 * 1. A retracting Observation ([WorldObservation.retracts]) invalidates
 *    the current belief for its subject if one exists
 *    ([ObservationResult.Invalidated]), and is rejected if none exists
 *    ([ObservationResult.Rejected]) -- retracting something that was
 *    never believed is not a meaningful act.
 * 2. Absent any existing belief for the subject, the first valid
 *    Observation is always accepted.
 * 3. Where a belief already exists, a new Observation is accepted only
 *    if its confidence is equal to or higher than the existing belief's.
 *    A weaker, contradictory Observation is rejected, not silently
 *    discarded -- the caller learns why.
 * 4. [isStillCurrent] is a separate, read-time check: a belief is still
 *    current if it was accepted within [staleAfter] of now, and stale
 *    otherwise. There is no background sweep -- staleness is discovered
 *    only when [InMemoryWorldModel] consults this method during
 *    `current`/`query`, never on a timer.
 *
 * ## Why "newer" reduces to a confidence comparison alone
 *
 * [InMemoryWorldModel] assigns [WorldBelief.timestamp] at the moment of
 * acceptance, so every Observation this policy evaluates against an
 * existing belief is already chronologically newer than that belief by
 * construction -- there is no separate recency comparison to make.
 * Confidence is the only remaining question, per rule 3.
 *
 * ## Who constructs the accepted `WorldBelief`
 *
 * This class constructs the [WorldBelief] carried by an
 * [ObservationResult.Accepted], including its authoritative [Instant.now]
 * timestamp. `WORLD_MODEL_CONTRACT_DESIGN.md` §5 states
 * [WorldModelUpdatePolicy] "does not itself construct a `WorldBelief`...
 * that remains `WorldModel`'s job" -- this is read here as a boundary
 * against an *external caller or another subsystem* constructing one,
 * not as a prohibition on which class, inside the World Model's own,
 * entirely internal implementation, performs the allocation.
 * [WorldModelUpdatePolicy] is never reachable from outside
 * [InMemoryWorldModel]'s boundary (`WORLD_MODEL_CONTRACT_DESIGN.md` §5's
 * own "never invoked directly by one" rule), so belief construction
 * happening here still occurs entirely within the World Model's
 * boundary, never outside it. This interpretation is disclosed
 * explicitly, not applied silently -- see
 * `docs/reviews/SPRINT_4_TRACK_B_UNIT_B3_POST_IMPLEMENTATION_REVIEW.md`.
 *
 * ## Deliberately not implemented
 *
 * Any confidence-weighted blending, sensor fusion, or probabilistic
 * reasoning across multiple corroborating Observations. A future
 * implementation could combine several weaker Observations into a
 * stronger derived belief; this Unit's own instructions explicitly
 * forbid inventing that now, and it is not needed for a first, minimal,
 * deterministic policy.
 */
class DefaultWorldModelUpdatePolicy(
    private val staleAfter: Duration = DEFAULT_STALE_AFTER,
) : WorldModelUpdatePolicy {

    override suspend fun evaluate(observation: WorldObservation, existing: WorldBelief?): ObservationResult {
        if (observation.retracts) {
            return if (existing != null) {
                ObservationResult.Invalidated(observation.subject)
            } else {
                ObservationResult.Rejected(
                    observation.subject,
                    "retraction requested, but no current belief exists for this subject",
                )
            }
        }

        val value = requireNotNull(observation.value) {
            "non-retracting WorldObservation must carry a value -- already enforced at construction"
        }

        if (existing == null) {
            return ObservationResult.Accepted(observation.subject, buildBelief(observation, value))
        }

        return if (observation.confidence >= existing.confidence) {
            ObservationResult.Accepted(observation.subject, buildBelief(observation, value))
        } else {
            ObservationResult.Rejected(
                observation.subject,
                "confidence (${observation.confidence}) is lower than the existing belief's " +
                    "confidence (${existing.confidence}) for this subject",
            )
        }
    }

    override suspend fun isStillCurrent(belief: WorldBelief): Boolean =
        Duration.between(belief.timestamp, Instant.now()) <= staleAfter

    private fun buildBelief(observation: WorldObservation, value: String): WorldBelief = WorldBelief(
        subject = observation.subject,
        value = value,
        confidence = observation.confidence,
        timestamp = Instant.now(),
        source = observation.source,
        derivedFrom = observation.derivedFrom,
    )

    companion object {
        val DEFAULT_STALE_AFTER: Duration = Duration.ofMinutes(15)
    }
}
