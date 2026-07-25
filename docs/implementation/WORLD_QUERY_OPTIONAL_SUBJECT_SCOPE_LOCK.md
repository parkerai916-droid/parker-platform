# WorldQuery Optional Subject — Scope Lock

## Status

**Implemented, within this Scope Lock's own Included list exactly —
nothing in the Excluded list below was implemented.** Steven's own
go-ahead ("The Scope Lock is frozen. Proceed with implementation only
within the approved scope.") authorised the work now recorded in
`docs/implementation/IMPLEMENTATION_HISTORY.md`, "WorldQuery Optional
Subject — Contract Revision Implementation." Build/test verification
could not be completed in this session's sandbox (Gradle did not execute
meaningfully); Steven's own local `.\gradlew.bat test` run remains the
authoritative verification, per PES-001 Stage 7 — this Scope Lock's work
is implemented but **not yet accepted** pending that run's own reported
result.

**Sprint 11, narrowly-scoped contract-revision Scope Lock.** Companion to
`docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_GOVERNANCE_REVIEW.md`
(compatibility and risk assessment) and
`docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md`
(the accepted field-level shape).

---

## Included

- The `WorldQuery.subjectMatch` nullability revision: `String` →
  `String? = null`, exactly as shaped in
  `WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md` Section 2.
- Null-means-unfiltered semantics in `InMemoryWorldModel.query`'s own
  filter predicate, exactly as shaped in
  `WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md` Section 3.
- Validation preserving the existing non-blank requirement for any
  supplied, non-null `subjectMatch` value (`require(subjectMatch == null || subjectMatch.isNotBlank())`).
- Focused contract tests (construction-time validation, mirroring
  `WorldModelContractsTest.kt`'s own existing style) and regression tests
  (confirming every existing, currently-passing `InMemoryWorldModelTest.kt`
  test continues to pass unchanged, plus new tests for the unfiltered-read
  case: a `null` `subjectMatch` returns every currently-non-stale belief,
  subject to `minimumConfidence` and `maximumResults`; a blank, non-null
  `subjectMatch` is still rejected).
- Documentation updates required by the revision:
  `docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md` Section 4 (amended
  to reflect the new optional field, per
  `WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md` Section 5),
  `IMPLEMENTATION_HISTORY.md`, and `IMPLEMENTATION_GAPS.md` where this
  revision materially changes the documented state.

## Excluded

- **`WorldModelSource` implementation.** Not created, touched, or
  designed further by this revision. `src/interfaces/WorldModelSource.kt`
  remains unwritten; this Scope Lock governs `WorldQuery`/`InMemoryWorldModel`
  only.
- **Assembler integration.** No change to
  `DefaultReasoningContextAssembler` — no new constructor dependency, no
  query construction, no rendering. Unit 8's own still-open
  query-construction question (how the Assembler would use a nullable
  `subjectMatch`, once `WorldModelSource` exists) is not resolved or
  implemented here.
- **`ParkerRuntime` wiring.** No change. `InMemoryWorldModel` remains
  unconstructed in the production composition root after this revision,
  exactly as before it.
- **Changes to `maximumResults`.** Remains mandatory, positive, unchanged
  in type, default, or validation.
- **Ordering guarantees.** No ordering guarantee is added to
  `WorldModel.query`. Its existing "no guarantee, caller must not depend
  on any particular order" behaviour is preserved exactly.
- **Ranking.** No ranking algorithm, scoring function, or relevance
  weighting is introduced anywhere in `InMemoryWorldModel.query`.
- **Scoring.** Restated for emphasis alongside "Ranking," above — no
  scoring concept of any kind is introduced.
- **Semantic search.** The existing case-insensitive substring match
  against `belief.subject` is the only matching behaviour this revision
  touches, and it touches it only to make invoking it conditional on
  `subjectMatch` being non-null — the match algorithm itself is
  unchanged.
- **Embeddings.** No vectorisation, embedding, or index of any kind is
  introduced.
- **Classification.** No mapping from arbitrary input to a structured
  subject is introduced anywhere by this revision.
- **Topic inference.** Restated for emphasis alongside "Classification,"
  above — `null` means "no filter," never "infer a subject," and no code
  path in this revision does anything resembling inference.
- **Contradiction resolution.** `WorldModelUpdatePolicy`'s own,
  already-implemented, Observation-acceptance-time contradiction handling
  is entirely unaffected — this revision touches only the read path
  (`query`), never `observe`.
- **Confidence changes.** `minimumConfidence`'s own field, default,
  validation, and filter behaviour are unchanged.
- **`WorldModel.current(subject)` changes.** Confirmed by
  `WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md` Section 1: not
  required for consistency, and therefore out of scope. `current`'s own
  `subject: String` parameter remains mandatory, non-nullable, and
  non-blank, exactly as today.
- **Unrelated refactoring.** No change to `WorldBelief`, `WorldObservation`,
  `ObservationResult`, `WorldModelUpdatePolicy`, `DefaultWorldModelUpdatePolicy`,
  or any file not directly named in the Included list above.

---

## Governing Principle

**This revision widens one field's type and relaxes one validation rule
on an already-approved contract. It introduces no new capability
anywhere it is not itself the subject of this Scope Lock's own Included
list.** Every existing caller and test remains valid and unmodified in
behaviour, per `WORLD_QUERY_OPTIONAL_SUBJECT_GOVERNANCE_REVIEW.md`'s own
Compatibility section. The revision's only observable new behaviour (an
unfiltered `query` result) is reachable exclusively by a caller that does
not yet exist anywhere in this repository — this revision does not create
that caller.

---

## Acceptance of This Scope Lock

This Scope Lock is binding once accepted. Implementation authorised
against it must satisfy the Included list exactly, must not implement
anything in the Excluded list, and must treat any discovered need to
exceed either list as grounds to pause and request a Scope Lock revision.
**Acceptance of this Scope Lock is not, by itself, authorisation to
implement** — per this task's own explicit instruction, a further,
separate go-ahead is required before any Kotlin is written.

---

## Relationship to Sprint 11 Unit 8

Implementing this revision would resolve the specific incompatibility
`WORLD_MODEL_SOURCE_QUERY_CONSTRUCTION_DECISION.md` identified (a
mandatory, non-nullable `subjectMatch` with no unfiltered-read path), and
would make the Contract Design's own originally preferred resolution
adoptable: `subjectMatch = null`, caller supplies `maximumResults`, World
Model owns selection and (non-guaranteed) ordering, Assembler preserves
returned order. **It would not, by itself, complete Unit 8.**
`WorldModelSource`, the Assembler's own fifth constructor dependency, and
`ParkerRuntime`'s own wiring step all remain separate, still-unauthorised
implementation work, explicitly excluded from this Scope Lock's own
Included list above.
