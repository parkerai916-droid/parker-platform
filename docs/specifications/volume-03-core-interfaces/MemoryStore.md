# MemoryStore Interface

## Status
Version: 0.8 (Sprint 4 Volume 3 Specification Alignment pass). Updated
to match `docs/architecture/MEMORY_CONTRACT_DESIGN.md` (Sprint 4, Track
A, Unit A2, accepted) and the verified implementation in
`src/interfaces/MemoryStore.kt` (Sprint 4, Track A, Unit A3). The prior
revision (0.6-alpha3, stamped during the v0.7 Architecture Completion
Phase -- see `docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md` §3.6)
described an interface shape Unit A2's Contract Design has since
superseded; this revision replaces that shape rather than layering a
correction alongside it, per `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`
Category A1's own instruction not to preserve an older interface shape
merely because it appeared in a prior specification. This document
remains a summary of already-approved, already-implemented behaviour --
`MEMORY_CONTRACT_DESIGN.md` remains the authoritative, field-level
source; this document does not reinterpret or extend it.

## Purpose

The MemoryStore manages Parker's durable long-term knowledge -- memory
that is earned through evaluation, not asserted by whichever subsystem
submits it (Chapter 17, Memory Architecture; `reasoning-context.md`).

## Responsibilities

- Store memories
- Retrieve memories
- Evaluate and promote candidate memories internally, in one submission
  step (see "Promotion Is Internal," below)
- Archive or forget memories
- Preserve source attribution

## Required Operations

```kotlin
interface MemoryStore {
    suspend fun remember(candidate: CandidateMemory): MemoryPromotionDecision
    suspend fun retrieve(query: MemoryQuery): List<MemoryRecord>
    suspend fun forget(memoryId: MemoryId): Boolean
}
```

Supporting types: `CandidateMemory` (what a subsystem submits for
consideration -- a knowledge payload, a proposed category, source and
correlation attribution, and optional confidence/explicit-request
signals), `MemoryRecord` (the durable, promoted representation
`retrieve` returns -- replaces the former bare `Memory` name, since
"Memory" already names the subsystem itself throughout this
architecture, and reusing it as a concrete type name invited exactly
the ambiguity a reader would otherwise have to resolve from context
every time it appeared), `MemoryCategory` (the closed, five-value
classification enum: `EPISODIC`, `SEMANTIC`, `PROCEDURAL`,
`USER_PREFERENCES`, `RELATIONSHIPS`), `MemoryPromotionDecision` (the
sealed `Promote`/`Reject` outcome `remember` itself returns),
`MemoryPromotionPolicy` (the internal decision seam that determines
every `MemoryPromotionDecision` -- see "Promotion Is Internal," below),
and `MemoryQuery` (a requesting Principal, a relevance string, a
correlation ID, a required positive `maximumResults` bound, and an
optional category filter). Full field-level shape for each is defined
in `MEMORY_CONTRACT_DESIGN.md`, not repeated here.

## Promotion Is Internal

`remember` is Memory's one submission-and-outcome operation: an
external caller submits a `CandidateMemory` and learns, in the same
call, whether it was promoted or rejected. There is no separate,
caller-facing `promote` operation, and `MemoryPromotionPolicy` is never
invoked directly by an external caller -- it is consulted internally by
whatever `MemoryStore` implementation handles `remember`
(`MEMORY_CONTRACT_DESIGN.md` §9's architectural decision: "External
callers never invoke promotion. Memory owns evaluation and promotion
internally, end to end"). This replaces the prior revision's separate
`addCandidate`/`promote` pair, which implied a caller could submit a
candidate and, independently, later decide to promote it itself --
something no accepted architecture has ever authorised.

There is also no separate `MemoryRuntime` interface. `MemoryStore`
remains Memory's one public interface, mirroring `IdentityService`'s
and `ToolRegistry`'s identical "one interface, no wrapper" precedent
(`MEMORY_CONTRACT_DESIGN.md` §9).

## Normative Requirements

- Memory promotion MUST respect policy -- `MemoryPromotionPolicy`
  determines every promotion; no caller decides its own outcome, and no
  caller can bypass it by calling anything other than `remember`.
- Sensitive memories MUST require appropriate permission --
  `CandidateMemory.sensitive` and `MemoryRecord.sensitive` carry this
  flag forward for the Permission Engine to evaluate later; Memory
  itself never evaluates a disclosure decision.
- Memories MUST preserve provenance -- `sourceSubsystem`,
  `correlationId`, and (where present) `originatingPrincipalId` are
  carried from `CandidateMemory` onto the resulting `MemoryRecord`
  unchanged.
- Forgetting MUST be auditable -- `forget` returns `true` only if a
  record existed and was removed, `false` if the `MemoryId` named
  nothing; a forgotten `MemoryId` is never recycled or reassigned, so
  an audit trail can still name it after the record itself is gone.

## Related

- Chapter 17 – Memory Architecture
- Chapter 33 – Memory Consolidation
- `docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md`
- `docs/architecture/MEMORY_CONTRACT_DESIGN.md` (authoritative
  field-level source for every type named above)
- `docs/reviews/SPRINT_4_TRACK_A_UNIT_A3_POST_IMPLEMENTATION_REVIEW.md`
