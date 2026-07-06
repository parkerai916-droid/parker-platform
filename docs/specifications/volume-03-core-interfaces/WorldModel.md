# WorldModel Interface

## Status
Version: 0.8 (Sprint 4 Volume 3 Specification Alignment pass). Updated
to match `docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md` (Sprint 4,
Track B, Unit B2, accepted) and the verified implementation in
`src/interfaces/WorldModel.kt` (Sprint 4, Track B, Unit B3). The prior
revision (0.6-alpha3, stamped during the v0.7 Architecture Completion
Phase -- see `docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md` §3.6)
described an interface shape Unit B2's Contract Design has since
superseded; this revision replaces that shape rather than layering a
correction alongside it, per `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`
Category A1's own instruction not to preserve an older interface shape
merely because it appeared in a prior specification. This document
remains a summary of already-approved, already-implemented behaviour --
`WORLD_MODEL_CONTRACT_DESIGN.md` remains the authoritative, field-level
source; this document does not reinterpret or extend it.

## Purpose

The WorldModel maintains Parker's current understanding of reality:
what Parker currently believes is true, not a history of what it has
ever believed (Chapter 16, World Model).

## Responsibilities

- Store transient state
- Track confidence
- Expire stale observations
- Resolve current state
- Publish state change events (deferred -- see "Known Scope
  Reductions," below)

## Required Operations

```kotlin
interface WorldModel {
    suspend fun observe(observation: WorldObservation): ObservationResult
    suspend fun current(subject: String): WorldBelief?
    suspend fun query(query: WorldQuery): List<WorldBelief>
}
```

Supporting types: `WorldBelief` (the current, live belief held for one
subject -- replaces the former `WorldState` name, matching the
vocabulary Chapter 16 and `reasoning-context.md` already use; carries
no history and no prior-belief reference, since the World Model is
never historical storage), `WorldObservation` (what a source submits
via `observe` -- a subject, a required confidence, a source, a value
required unless retracting, and optional provenance), `ObservationResult`
(the sealed `Accepted` / `Invalidated` / `Rejected` outcome `observe`
returns), `WorldModelUpdatePolicy` (the internal decision seam that
determines every `ObservationResult` and separately judges whether an
already-held belief remains current -- see "Policy Is Internal,"
below), and `WorldQuery` (a required, non-blank subject-match string, a
required positive `maximumResults` bound, and an optional
minimum-confidence filter). Full field-level shape for each is defined
in `WORLD_MODEL_CONTRACT_DESIGN.md`, not repeated here.

**Naming and scoping note.** `current`'s parameter is a plain,
non-blank subject `String`, not a `ResourceId` as the prior revision of
this document specified. Not every Information Category the World
Model can hold a belief about maps onto a registered Resource
(`WORLD_MODEL_CONTRACT_DESIGN.md`, Resource Identity resolution), so the
World Model no longer requires Resource registration for every subject
it tracks a belief about.

## Policy Is Internal

`WorldModelUpdatePolicy` decides whether a submitted `WorldObservation`
is accepted, invalidates an existing belief, or is rejected, and
separately judges whether an already-held belief remains current. It is
consulted internally by whatever `WorldModel` implementation handles
`observe`/`current`/`query`, and is never invoked directly by an
external caller (`WORLD_MODEL_CONTRACT_DESIGN.md` §5). External callers
submit an observation and learn the outcome through `observe` alone;
they never see the policy's reasoning before the decision is final, and
never override, appeal, or bypass it.

There is also no separate `WorldModelRuntime` interface. `WorldModel`
remains the sole public interface, mirroring `MemoryStore`'s identical
"one interface, no wrapper" determination.

## Normative Requirements

- World state MUST be timestamped -- every `WorldBelief.timestamp` is
  assigned at Update time by whatever constructs the belief internally,
  never supplied by an external caller.
- World state MUST have confidence -- `confidence` is a required,
  non-optional field on both `WorldObservation` and `WorldBelief`.
- Stale state MUST expire or degrade -- `WorldModelUpdatePolicy.isStillCurrent`
  is consulted lazily by `current` and `query`, excluding any belief
  judged stale from what either operation returns; there is no
  autonomous background expiry sweep.
- World Model MUST NOT become Memory -- the World Model holds only
  present, replaceable belief; it has no promotion mechanism, no
  durable retention concept, and no history of its own for any subject.

## Known Scope Reductions (carried over from implementation, not
introduced here)

- `InMemoryWorldModel` does not yet publish state-change events, though
  "Publish state change events" remains a named Responsibility above --
  a disclosed, open gap (`IMPLEMENTATION_GAPS.md` #47), not a silent
  omission. `docs/adr/ADR-023-context-provider-event-publication.md`
  settles the architectural shape a future closing unit must follow; it
  does not implement it, and the gap remains open.

## Related

- Chapter 16 – World Model
- `docs/architecture/WORLD_MODEL_RUNTIME_ARCHITECTURE.md`
- `docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md` (authoritative
  field-level source for every type named above)
- `docs/adr/ADR-023-context-provider-event-publication.md`
- `docs/reviews/SPRINT_4_TRACK_B_UNIT_B3_POST_IMPLEMENTATION_REVIEW.md`
