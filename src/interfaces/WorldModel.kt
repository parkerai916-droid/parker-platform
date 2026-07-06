package parker.core.interfaces

import java.time.Instant

/*
 * Sprint 4, Track B, Unit B3. This file replaces the original,
 * operation-naming-only stub in place, per
 * `docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md` (Unit B2, accepted).
 * Three corrections from the original stub, each authorised by that
 * document rather than invented here:
 *
 * 1. `WorldState` is renamed `WorldBelief` (`WORLD_MODEL_CONTRACT_DESIGN.md`,
 *    Resolve Outstanding Design Questions, "WorldState vs. WorldBelief
 *    naming"), matching the vocabulary `16-world-model.md` and
 *    `reasoning-context.md` already use.
 * 2. `current`'s parameter changes from `resourceId: ResourceId` to a
 *    plain, non-blank subject `String` (`WORLD_MODEL_CONTRACT_DESIGN.md`
 *    §1/§2/§6, "Resource identity" resolution) -- not every Information
 *    Category (`WORLD_MODEL_RUNTIME_ARCHITECTURE.md` §4) maps onto a
 *    registered Resource, so the World Model no longer requires Resource
 *    registration for every subject it can hold a belief about.
 * 3. `WorldObservation`, `ObservationResult`, and `WorldQuery` -- named
 *    but entirely unshaped in the original stub -- receive their
 *    approved field-level shape, and `WorldModelUpdatePolicy` is
 *    introduced as the approved internal decision seam
 *    (`WORLD_MODEL_CONTRACT_DESIGN.md` §5).
 *
 * The excluded candidates `WorldModelUpdateDecision` and
 * `WorldModelRuntime` are not implemented anywhere in this file, per
 * `WORLD_MODEL_CONTRACT_DESIGN.md`'s Contract Minimalism Review. No
 * belief-category enumeration is implemented either, per that same
 * document's §7.
 */

/**
 * The current, live representation of what the World Model believes
 * about one [subject] (`WORLD_MODEL_CONTRACT_DESIGN.md` §1). Constructed
 * only by [WorldModel]'s implementation (via an injected
 * [WorldModelUpdatePolicy]) at Update time -- never constructed directly
 * by an external caller.
 *
 * Carries no history and no prior-belief reference: the World Model is
 * never historical storage (`WORLD_MODEL_RUNTIME_ARCHITECTURE.md`,
 * Architectural Principles). Carries no expiry field either -- staleness
 * is computed lazily, from [timestamp], by [WorldModelUpdatePolicy.isStillCurrent].
 */
data class WorldBelief(
    val subject: String,
    val value: String,
    val confidence: Double,
    val timestamp: Instant,
    val source: String,
    val derivedFrom: List<String> = emptyList(),
) {
    init {
        require(subject.isNotBlank()) { "WorldBelief.subject must not be blank" }
        require(value.isNotBlank()) { "WorldBelief.value must not be blank" }
        require(confidence in 0.0..1.0) {
            "WorldBelief.confidence must be between 0.0 and 1.0, was $confidence"
        }
        require(source.isNotBlank()) { "WorldBelief.source must not be blank" }
    }
}

/**
 * What any of `WORLD_MODEL_RUNTIME_ARCHITECTURE.md` §6's seven sources
 * (Sensors, Plugins, Agents, Runtime, User -- Planner and Memory
 * deliberately excluded, per that same table) submits via
 * [WorldModel.observe].
 *
 * [confidence] is required, not optional, unlike `CandidateMemory`'s own
 * confidence field -- `WorldModel.md`'s Normative Requirement ("World
 * state MUST have confidence") is unconditional.
 *
 * [value] is optional only because a retracting Observation
 * ([retracts] = `true`) asserts nothing new; every non-retracting
 * Observation must carry a non-blank [value] (enforced below).
 * [sourceTimestamp], if supplied, is retained only for provenance -- it
 * is never the authoritative timestamp a resulting [WorldBelief] carries
 * (`WORLD_MODEL_CONTRACT_DESIGN.md` §2, Timestamp Ownership).
 * [derivedFrom]'s presence, not a formal category field, is what marks a
 * belief as derived rather than directly sensed
 * (`WORLD_MODEL_RUNTIME_ARCHITECTURE.md` §4, Derived Beliefs).
 */
data class WorldObservation(
    val subject: String,
    val confidence: Double,
    val source: String,
    val value: String? = null,
    val sourceTimestamp: Instant? = null,
    val derivedFrom: List<String> = emptyList(),
    val retracts: Boolean = false,
) {
    init {
        require(subject.isNotBlank()) { "WorldObservation.subject must not be blank" }
        require(confidence in 0.0..1.0) {
            "WorldObservation.confidence must be between 0.0 and 1.0, was $confidence"
        }
        require(source.isNotBlank()) { "WorldObservation.source must not be blank" }
        require(retracts || !value.isNullOrBlank()) {
            "WorldObservation.value must not be blank unless this Observation is a retraction"
        }
    }
}

/**
 * The outcome of one [WorldModel.observe] call
 * (`WORLD_MODEL_CONTRACT_DESIGN.md` §3). A sealed outcome with exactly
 * three variants -- no fourth "Superseded"/"Replaced" variant exists,
 * because [WorldBelief] retains no reference to what it replaced, so
 * "established" and "superseded" look identical to a caller: a fresh
 * [WorldBelief] for the subject in question. No variant exists for
 * Expiry either -- Expiry is not the outcome of any specific `observe`
 * call; see [WorldModelUpdatePolicy.isStillCurrent].
 */
sealed class ObservationResult {
    abstract val subject: String

    /** The Observation now represents current belief for [subject]. */
    data class Accepted(override val subject: String, val belief: WorldBelief) : ObservationResult()

    /**
     * A prior belief existed for [subject] and has now been cleared; no
     * belief currently stands. Distinct from [Accepted] because it
     * asserts nothing new.
     */
    data class Invalidated(override val subject: String) : ObservationResult()

    /**
     * The Observation was not applied at all; whatever belief existed
     * for [subject] beforehand (if any) is unchanged. [reason] is free
     * text, not a closed enum, for the same reason
     * `MemoryPromotionDecision.Reject.reason` is
     * (`WORLD_MODEL_CONTRACT_DESIGN.md` §3): a weighing decision (a
     * confidence comparison against an existing, competing belief, or a
     * retraction with nothing to retract), not a fixed set of
     * mechanically-checkable structural rules.
     */
    data class Rejected(override val subject: String, val reason: String) : ObservationResult() {
        init {
            require(reason.isNotBlank()) { "ObservationResult.Rejected.reason must not be blank" }
        }
    }
}

/**
 * The seam by which the World Model decides whether a validated
 * [WorldObservation] becomes, updates, or invalidates current belief
 * (`WORLD_MODEL_CONTRACT_DESIGN.md` §5). Internal to the World Model's
 * implementation: never invoked directly by an external caller, and
 * never surfaced as a caller-facing operation on [WorldModel].
 */
interface WorldModelUpdatePolicy {

    /**
     * Given a [WorldObservation] and any [existing] [WorldBelief] for
     * the same subject (`null` if none), produce an [ObservationResult].
     */
    suspend fun evaluate(observation: WorldObservation, existing: WorldBelief?): ObservationResult

    /**
     * Given a currently-held [belief], determine whether it remains
     * current or has become stale enough to be excluded from
     * `current`/`query` results. Consulted only at read time -- there is
     * no autonomous, self-triggered expiry sweep
     * (`WORLD_MODEL_RUNTIME_ARCHITECTURE.md` §11).
     */
    suspend fun isStillCurrent(belief: WorldBelief): Boolean
}

/**
 * Describes what a caller is asking [WorldModel.query] to return
 * (`WORLD_MODEL_CONTRACT_DESIGN.md` §4). Deliberately carries no
 * requesting-Principal field and no correlation identifier -- neither
 * has an identified concrete need for the World Model, unlike Memory's
 * own `MemoryQuery` (see that section for the reasoning).
 */
data class WorldQuery(
    val subjectMatch: String,
    val maximumResults: Int,
    val minimumConfidence: Double? = null,
) {
    init {
        require(subjectMatch.isNotBlank()) { "WorldQuery.subjectMatch must not be blank" }
        require(maximumResults >= 1) {
            "WorldQuery.maximumResults must be at least 1, was $maximumResults"
        }
        require(minimumConfidence == null || minimumConfidence in 0.0..1.0) {
            "WorldQuery.minimumConfidence must be between 0.0 and 1.0 if present, was $minimumConfidence"
        }
    }
}

/**
 * The single public contract through which every other Runtime
 * Foundation component reaches the World Model
 * (`WORLD_MODEL_CONTRACT_DESIGN.md` §6). No separate `WorldModelRuntime`
 * interface exists -- this remains the World Model's one public
 * interface, mirroring `MemoryStore`'s identical "one interface, no
 * wrapper" determination.
 */
interface WorldModel {
    suspend fun observe(observation: WorldObservation): ObservationResult
    suspend fun current(subject: String): WorldBelief?
    suspend fun query(query: WorldQuery): List<WorldBelief>
}
