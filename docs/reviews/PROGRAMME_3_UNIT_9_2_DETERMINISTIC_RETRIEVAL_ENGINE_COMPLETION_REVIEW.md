# Programme 3 — Unit 9.2: Deterministic Retrieval Engine — Completion Review

## Status

**Implementation completion review, including a self-performed Independent Constitutional Review.** Unit 9.2 only is implemented, exactly as `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` §4's own Unit 9.2 entry specifies. Units 9.3 through 9.6 are not begun. No governance document was modified. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD at start:** `d9725f4bbfd7f2304dee90b4cfcdf4db9e053c11` (`d9725f4`)
- **Branch:** `main`
- **Working tree at start:** clean.

---

## Governance Read Before Implementation

Read fresh, in full, before writing any code: `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` (§4, Unit 9.2's own objective, dependencies, repository impact, verification requirements); `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (Adopted, §2, §3, §7, §8); `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (Adopted); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`; `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` (§7 determinism NFR). Source read in full: `src/interfaces/KnowledgeStore.kt`'s own Unit 9.1 addition (post-acceptance); `src/runtime/KnowledgeItemPersistence.kt`; `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`; `src/runtime/DefaultKnowledgeSubmission.kt`; `tests/runtime/KnowledgeItemPersistenceTest.kt`.

---

## What Was Implemented

**`src/runtime/DefaultKnowledgeRetrieval.kt`, new file** (exactly the location the Implementation Plan names) — `internal class DefaultKnowledgeRetrieval(private val persistence: KnowledgeItemPersistence) : KnowledgeRetrieval`, the sole implementation of the Unit 9.1 interface. One method: `retrieve(requestingPrincipalId, query)` — reads every stored item via `persistence.findAll()`, filters by a case-insensitive substring match of `query.relevance` against each item's own most recent history event `basis`, bounds the matched set to `query.maximumResults`, and wraps each surviving item in a `KnowledgeResultEntry` with `stale = true`. Always returns `KnowledgeRetrievalDisposition.Retrieved` — never `NotAuthorised`, since permission enforcement is explicitly Unit 9.5's own, later responsibility.

**`src/runtime/KnowledgeItemPersistence.kt`, modified** — added `findAll(): List<KnowledgeItem>` to the interface and `InMemoryKnowledgeItemPersistence`, returning stored items in insertion order. This is the one change to existing code this Unit required: without it, structural query execution against "Knowledge Memory's own held state" (Contract Design §2) was impossible, since the existing seam exposed only a by-identifier lookup. Purely internal (never in `src/interfaces/`); `store` and `find`'s own existing behaviour is untouched.

**`tests/runtime/DefaultKnowledgeRetrievalTest.kt`, new file** — 15 tests: matching (contains, case-insensitive, exclusion, most-recent-history-entry precedence over an earlier entry); empty-result handling; `maximumResults` bounding; insertion-order preservation; determinism (identical repeated calls); the `stale = true` placeholder disclosure; permission-blindness (never returns `NotAuthorised`; identical results for different principals); and four structural tests (exactly one constructor dependency; no `MemoryRetrieval`/`MemoryCore`/`PermissionEngine`-shaped property; exactly one public method; no ranking/scoring/semantic-shaped declaration).

**`tests/runtime/KnowledgeItemPersistenceTest.kt`, modified** — added 3 tests for `findAll()` (empty case, insertion-order preservation, non-interference with `store`/`find`).

**`tests/runtime/DefaultKnowledgeSubmissionTest.kt`, modified** — `FakeKnowledgeItemPersistence` (a private test double) required a `findAll()` override to remain a valid implementation of the now-three-method `KnowledgeItemPersistence` interface; added, delegating to the same real `InMemoryKnowledgeItemPersistence` its other two methods already delegate to. No test behaviour changed.

---

## Design Decisions and Reasoning

Three decisions were required beyond what the Contract Design fixes at the properties level, each reasoned here rather than applied silently:

1. **Structural matching target: the most recent history entry's `basis`.** `KnowledgeItem` (V2) carries no free-text payload field of its own — by design, Knowledge Memory never copies Memory Core content. The only genuine free text on the type is each lifecycle event's own disclosed `basis`. Matching against the *most recent* entry's `basis` (case-insensitive substring, mirroring legacy `KnowledgeQuery.relevance`'s own already-proven convention) reflects why the item's current classification is what it is — the most load-bearing text available — without inventing a new field or duplicating Memory Core content. Verified directly by a dedicated test that an earlier history entry's own basis does not cause a match once superseded by a later one.
2. **`KnowledgeItemPersistence.findAll()`, a new method on an existing internal seam.** Required because the existing seam exposed only by-identifier lookup, and structural query execution is impossible without enumeration. This is not a governance modification — the seam is `internal`, never part of `src/interfaces/`, and neither `store` nor `find`'s own contract changed. Returns items in insertion order, which is the one deterministic ordering guarantee this Unit relies on rather than computes.
3. **`stale = true` for every entry, unconditionally.** `KnowledgeResultEntry.stale` is non-nullable — some value must be supplied now, though genuine staleness detection is Unit 9.3's own, later responsibility, and computing it accurately would require exactly the Memory Core query this Unit is structurally forbidden from making. `true` was chosen over `false`: asserting freshness with no evidence would be the fabrication the Contract Design's own governing principle forbids, while disclosing potential staleness is the fail-closed direction consistent with this repository's own established uncertainty-discipline (Chapter 10's "uncertainty... defaults to inaction," applied here to disclosure). This is a known, disclosed, inherited tension from Unit 9.1's own accepted non-nullable `Boolean` shape (identified in that Unit's own Independent Constitutional Review, Finding 1) — this Unit does not resolve it, only makes the least dishonest choice available within it.

---

## Verification Performed

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 48s
5 actionable tasks: 5 executed
```

- **`DefaultKnowledgeRetrievalTest`:** 15/15 passed.
- **`KnowledgeItemPersistenceTest`:** 8/8 passed (5 pre-existing + 3 new).
- **`DefaultKnowledgeSubmissionTest`:** unaffected, still passing (the `FakeKnowledgeItemPersistence` fix was required for compilation, not behaviour).
- **Full repository suite:** 1697 tests, 0 failures, 0 errors, 5 pre-existing skips (up from 1679 at Unit 9.1's own baseline; +18 new tests, 0 regressions).

---

## Independent Constitutional Review

Audited as if written by another reviewer, against the adopted Contract Design, the adopted Clarification, the Implementation Plan's own Unit 9.2 entry, and the actual compiled code:

- **Does this Unit implement Unit 9.3, 9.4, 9.5, or 9.6?** No — no staleness computation (a disclosed, constant placeholder only), no retirement/supersession default-inclusion policy (every matched item is returned regardless of `status`, unfiltered by lifecycle state), no permission evaluation (`retrieve` never consults `requestingPrincipalId`, confirmed by a dedicated test that different principals receive identical results), no `ParkerRuntime.kt` change (confirmed by `git diff --stat`).
- **Does this Unit preserve Memory Core separation?** Yes — `DefaultKnowledgeRetrieval` holds exactly one dependency (`KnowledgeItemPersistence`); no `MemoryRetrieval`/`MemoryCore` reference exists anywhere in the new file, confirmed both by direct source inspection and a reflection-based structural test.
- **Does this Unit preserve provider neutrality?** Yes — no reference to any reasoning provider or model exists anywhere in the diff.
- **Does this Unit preserve deterministic behaviour?** Yes — `filter`/`take` both preserve input order (Kotlin's own documented guarantee), `findAll()` returns a fixed insertion order, and a dedicated test proves three repeated calls with the same query against unchanged state produce byte-identical results.
- **Does this Unit introduce a ranking heuristic or semantic search?** No — matching is a boolean, non-scored substring test; bounding is truncation, not weighting; confirmed by a dedicated structural test asserting no ranking/scoring/semantic-shaped declaration exists on the class.
- **Is the `findAll()` addition to `KnowledgeItemPersistence` a governance modification requiring documentation elsewhere?** No — it is an internal, non-`src/interfaces/` seam; no governance document names or constrains this seam's own method set, and the Unit 8 Clarification's own §10 explicitly reserves storage-mechanism latitude to implementation. Disclosed extensively in the seam's own KDoc regardless, on this repository's own "disclose, don't hide" convention.
- **Is the `stale = true` placeholder itself a defect?** Considered carefully. It is not a computation, and does not claim to be one — the KDoc states this explicitly, states the reasoning for choosing `true` over `false`, and names Unit 9.3 as the owner of the real mechanism. This is the same category of disclosed, narrower, non-fabricating baseline `DefaultKnowledgeCandidateEvaluator`'s own KDoc already establishes precedent for (a disclosed, reasoned narrowing, not a silent gap).
- **Does this Unit modify any governance document?** No — confirmed by `git status`; only source and test files changed.
- **Do the tests enforce governance rather than manufacture it, and do they avoid vacuous or naive substring-over-source-text checks?** Yes — every reflection-based structural test operates on compiled declarations (`declaredMemberProperties`, `declaredFunctions`, `primaryConstructor`), mirroring `KnowledgeSubmissionScopeTest.kt`'s own already-accepted technique; every behavioural test asserts a specific, governance-traceable property, not merely that code runs.

**One limitation is disclosed here rather than concealed, mirroring the same limitation Unit 9.1's own Independent Constitutional Review already found and accepted.** No test in this Unit exercises "unavailable data" or "implementation failure" distinguishability, because `InMemoryKnowledgeItemPersistence`'s own in-memory read has no plausible failure mode to exercise — this remains genuinely untestable until a fallible storage technology is introduced, which is explicitly out of this Unit's own scope (Contract Design §11).

**No genuine defect found requiring correction.** No correction was therefore made, and no Defect Confirmation Review is required.

---

## Files Created

- `src/runtime/DefaultKnowledgeRetrieval.kt`
- `tests/runtime/DefaultKnowledgeRetrievalTest.kt`
- `docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_COMPLETION_REVIEW.md` (this document)

## Files Modified

- `src/runtime/KnowledgeItemPersistence.kt` (additive — `findAll()` added to interface and implementation; `store`/`find` untouched)
- `tests/runtime/KnowledgeItemPersistenceTest.kt` (additive — 3 new tests for `findAll()`)
- `tests/runtime/DefaultKnowledgeSubmissionTest.kt` (minimal — `FakeKnowledgeItemPersistence` given a `findAll()` override, required for compilation; no test behaviour changed)

## Test Results

- `DefaultKnowledgeRetrievalTest`: **15/15 passed**
- `KnowledgeItemPersistenceTest`: **8/8 passed**
- Full repository suite: **1697 tests, 0 failures, 0 errors, 5 pre-existing skips**

## Constitutional Verdict

```
UNIT 9.2 — COMPLETE. NO DEFECT FOUND.
```

## Recommendation

Unit 9.2 satisfies its own Completion Gate (Implementation Plan §7): it compiles, every existing test still passes, every new test named for it passes, and no later unit was required to reach this state. Unit 9.4 (Retirement and Supersession Retrieval-Shape Decision) may now begin, its only dependency — this Unit — being complete. Unit 9.3 (Staleness Disclosure Mechanism) may also begin in parallel, per the Implementation Plan's own ordering. Unit 9.5 remains blocked behind its own governance precondition, unaffected by this Unit's completion.

---

## Final Git Status

```
$ git status --short
 M src/runtime/KnowledgeItemPersistence.kt
 M tests/runtime/DefaultKnowledgeSubmissionTest.kt
 M tests/runtime/KnowledgeItemPersistenceTest.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_COMPLETION_REVIEW.md
?? src/runtime/DefaultKnowledgeRetrieval.kt
?? tests/runtime/DefaultKnowledgeRetrievalTest.kt
```

Nothing staged, committed, or pushed.
