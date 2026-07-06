package parker.core.interfaces

/**
 * Sprint 3, Track D, Unit D2. The subset of
 * `PlannerRuntimeSpecification.md` Section 5's ten-state Planning Session
 * lifecycle that [InMemoryPlannerRuntime] actually drives in production --
 * eight of the ten specified states, transcribed exactly (no invented
 * edges) from the diagram there, following the same "documented subset,
 * not a claim the other states don't exist" precedent
 * `DeterministicPlannerHarness.kt`'s own `PlanningSessionLifecycleState`
 * already established for its five-state test-only subset.
 *
 *   CREATED -> CONTEXT_GATHERING
 *   CONTEXT_GATHERING -> ANALYSING
 *   ANALYSING -> {PROPOSING, FAILED}
 *   PROPOSING -> SUBMITTED
 *   SUBMITTED -> {COMPLETED, REJECTED}
 *
 * **Not reached, and not modelled here:**
 * - `WAITING_FOR_INPUT` -- would require Planner Runtime reasoning about
 *   insufficient context; explicitly out of this Unit's scope ("Planner
 *   reasoning").
 * - `CANCELLED` -- no cancellation command channel exists for the Planner
 *   Runtime; adding one is a "Planner concurrency redesign"/command-channel
 *   concern this Unit's own governing design document
 *   (`docs/architecture/PLANNER_RUNTIME_PROGRESSION_DESIGN.md` Section 11)
 *   explicitly excludes.
 *
 * Both remain real, specified states; this is a coverage gap, not a
 * specification defect, exactly as that design document's own Section
 * 5/Section 7 review already found for `DeterministicPlannerHarness.kt`.
 *
 * **A Planning Session whose initiating Principal does not resolve is
 * deliberately NOT modelled as `CREATED -> FAILED`.**
 * `PlannerRuntimeSpecification.md` Section 5's own diagram has no such
 * edge -- `CREATED`'s only specified edges are `CONTEXT_GATHERING` and
 * `CANCELLED`. [InMemoryPlannerRuntime.plan] resolves identity *before*
 * a session record is created at all, mirroring
 * `InMemoryTaskManagerRuntime`'s own unresolvable-owner precedent
 * exactly ("no Task record is ever created there, so there is no `taskId`
 * to correlate an event against"): an unresolvable initiating Principal
 * means this Planning Session never validly entered `CREATED`, so there
 * is nothing to transition out of it.
 *
 * Named `PlannerSessionStatus`/`PlannerSessionLifecycleTransitions`
 * (rather than reusing `DeterministicPlannerHarness.kt`'s own
 * `PlanningSessionLifecycleState`/`PlanningSessionLifecycleTransitions`
 * names) so production code under `src/` never depends on, or risks a
 * same-package naming collision with, that test-only fixture's
 * identically-scoped but narrower five-state names.
 */
enum class PlannerSessionStatus {
    CREATED,
    CONTEXT_GATHERING,
    ANALYSING,
    PROPOSING,
    SUBMITTED,
    COMPLETED,
    REJECTED,
    FAILED,
}

/**
 * The edges [InMemoryPlannerRuntime] actually drives -- see
 * [PlannerSessionStatus]'s own KDoc for exactly which subset of
 * `PlannerRuntimeSpecification.md` Section 5's diagram this is, and why.
 * Mirrors [TaskLifecycleTransitions]'s/[ExecutionLifecycleTransitions]'s
 * identical map-of-allowed-next-states shape.
 */
object PlannerSessionLifecycleTransitions {

    private val allowed: Map<PlannerSessionStatus, Set<PlannerSessionStatus>> = mapOf(
        PlannerSessionStatus.CREATED to setOf(
            PlannerSessionStatus.CONTEXT_GATHERING,
        ),
        PlannerSessionStatus.CONTEXT_GATHERING to setOf(
            PlannerSessionStatus.ANALYSING,
        ),
        PlannerSessionStatus.ANALYSING to setOf(
            PlannerSessionStatus.PROPOSING,
            PlannerSessionStatus.FAILED,
        ),
        PlannerSessionStatus.PROPOSING to setOf(
            PlannerSessionStatus.SUBMITTED,
        ),
        PlannerSessionStatus.SUBMITTED to setOf(
            PlannerSessionStatus.COMPLETED,
            PlannerSessionStatus.REJECTED,
        ),
        PlannerSessionStatus.COMPLETED to emptySet(),
        PlannerSessionStatus.REJECTED to emptySet(),
        PlannerSessionStatus.FAILED to emptySet(),
    )

    fun isValidTransition(from: PlannerSessionStatus, to: PlannerSessionStatus): Boolean =
        to in allowed.getValue(from)

    /** Throws [IllegalArgumentException] if `from -> to` is not one of the edges this Unit models. */
    fun requireValidTransition(from: PlannerSessionStatus, to: PlannerSessionStatus) {
        require(isValidTransition(from, to)) {
            "Illegal Planner Session lifecycle transition for this Unit's modelled subset: $from -> $to"
        }
    }
}
