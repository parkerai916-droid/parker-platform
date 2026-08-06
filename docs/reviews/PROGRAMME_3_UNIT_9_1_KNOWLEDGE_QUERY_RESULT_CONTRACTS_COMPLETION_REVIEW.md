# Programme 3 — Unit 9.1: Knowledge Query / Result Contracts — Completion Review

## Status

**Implementation completion review, including a self-performed Independent Constitutional Review.** Unit 9.1 only is implemented, exactly as `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` §4's own Unit 9.1 entry specifies. Units 9.2 through 9.6 are not begun. No governance document was modified. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD at start:** `0e1dc58c037042b96802ead7336fd3b305fe50f3` (`0e1dc58`)
- **Branch:** `main`
- **Working tree at start:** clean.

---

## Governance Read Before Implementation

Read fresh, in full, before writing any code: `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (Adopted); `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (Adopted); `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` (§4, Unit 9.1's own objective, dependencies, repository impact, and verification requirements). Source inspected directly: `src/interfaces/KnowledgeStore.kt` in full (1230 lines, before this Unit's own addition) — every existing "Unit N" KDoc block, `KnowledgeSubmission`/`KnowledgeSubmissionDisposition`, and legacy `KnowledgeQuery`/`KnowledgeStore`; `tests/contracts/KnowledgeSubmissionScopeTest.kt` and `tests/contracts/KnowledgeLifecycleEventTest.kt` in full, as the style and structural-testing precedent this Unit's own tests mirror.

---

## What Was Implemented

**`src/interfaces/KnowledgeStore.kt`, extended additively** (exactly the file the Implementation Plan's own Unit 9.1 entry names), adding five declarations after `KnowledgeSubmission`, nothing existing altered:

- **`KnowledgeRetrievalQuery`** — `relevance: String`, `correlationId: String`, `maximumResults: Int`, each validated at construction (non-blank, non-blank, `>= 1`). A new, V2-tier request type, distinct from legacy `KnowledgeQuery` (which returns the incompatible legacy `KnowledgeRecord` shape and cannot answer with V2 `KnowledgeItem` content).
- **`KnowledgeResultEntry`** — pairs one `KnowledgeItem` with a non-nullable `stale: Boolean` staleness disclosure. Declares the shape; computes nothing.
- **`KnowledgeRetrievalResult`** — wraps an ordered `List<KnowledgeResultEntry>`. An empty list is a valid, successful result.
- **`KnowledgeRetrievalDisposition`** — sealed interface, exactly two variants: `Retrieved(result: KnowledgeRetrievalResult)` and `NotAuthorised(reason: String)` (non-blank, validated).
- **`KnowledgeRetrieval`** — the interface itself: `suspend fun retrieve(requestingPrincipalId: PrincipalId, query: KnowledgeRetrievalQuery): KnowledgeRetrievalDisposition`. No implementing class exists — declared as a pure contract, mirroring `KnowledgeSubmission`'s own existence before `DefaultKnowledgeSubmission` was built.

**`tests/contracts/KnowledgeRetrievalContractsTest.kt`, new file** — 18 tests: construction-time validation for `KnowledgeRetrievalQuery` (5 tests); a structural, reflection-based test proving `KnowledgeRetrievalQuery` declares exactly its three authorised properties, and a second proving no ranking/semantic/permission-assertion property exists (2 tests); pure data-shape tests for `KnowledgeResultEntry` and `KnowledgeRetrievalResult`, including a reflection-based test proving `stale` is non-nullable (4 tests); closure, exhaustiveness, validation, and empty-versus-denied distinctness tests for `KnowledgeRetrievalDisposition` (4 tests); reflection-based structural tests for `KnowledgeRetrieval`'s own shape and its exclusion of storage/submission/lifecycle/evaluation-policy operations (3 tests).

---

## Design Decisions and Reasoning

Three genuine design decisions were required beyond what the Contract Design fixes at the properties level, each reasoned here rather than applied silently:

1. **`KnowledgeRetrievalDisposition` declares two variants, not five.** The Unit 9 Contract Design §9's Error Model names five distinguishable outcomes, but only two — permission denial and a successful (possibly empty) retrieval — require a sealed-type variant. "Invalid query" is prevented by `KnowledgeRetrievalQuery`'s own construction-time validation (a malformed query cannot exist to be passed to `retrieve` at all), mirroring `DefaultKnowledgeSubmission`'s own identical "structural admissibility... a compile-time guarantee" reasoning for its own Evaluation-Order step 2. "Unavailable data" and "implementation failure" are genuine infrastructure faults, expressed as thrown exceptions rather than returned values, mirroring `InMemoryMemoryCore`'s and `DefaultKnowledgeSubmission`'s own shared "no `try`/`catch`, faults propagate unchanged" discipline. This is not a narrowing of the Contract Design's own requirement — all five outcomes remain independently, unambiguously distinguishable to a caller (three by construction/exception, two by sealed variant) — it is the same pattern this codebase already applies consistently everywhere a similar five-shaped-into-fewer situation arises.
2. **`KnowledgeResultEntry` bundles a full `KnowledgeItem`, not a `KnowledgeReference`.** Contract Design §4 authorises bundling either. `KnowledgeReference`'s own KDoc explicitly reserves "any summary or task-scoped display content" as "a retrieval-time projection belonging to Unit 9's own Knowledge Result contract" — since the Contract Design does not itself require such a projection, and inventing one now would exceed this Unit's own authorised scope, `KnowledgeItem` (already fully specified, already carrying its own evidential-state and provenance reference) is the minimal, non-inventive choice. Whether a future unit ever projects down to `KnowledgeReference` for some retrieval shape remains open, undecided by this Unit.
3. **`maximumResults` is not a ranking instruction.** Reused directly from legacy `KnowledgeQuery`'s own already-accepted field, and from `DefaultReasoningContextAssembler`'s own `MEMORY_QUERY_MAXIMUM_RESULTS`/`WORLD_QUERY_MAXIMUM_RESULTS` precedent (both explicitly documented there as "not architecturally significant"). A volume bound is not a preference among matches — it says nothing about which matches are returned when more exist than the bound allows, a question Ordering (Contract Design §8), not the Query type, governs. The structural test suite directly confirms no ranking/scoring/weighting property exists on `KnowledgeRetrievalQuery`.

---

## Verification Performed

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 1m 1s
5 actionable tasks: 5 executed
```

- **New test class:** `parker.core.interfaces.KnowledgeRetrievalContractsTest` — 18 tests, 0 failures, 0 errors, 0 skipped.
- **Full repository suite:** 1679 tests, 0 failures, 0 errors, 5 skipped (pre-existing skips, unrelated to this Unit).
- No existing test file was modified; no existing test's outcome changed.

---

## Independent Constitutional Review

Audited as if written by another reviewer, against the adopted Contract Design, the adopted Clarification, and the actual compiled code:

- **Does this Unit implement a retrieval engine?** No — `KnowledgeRetrieval` has no implementing class anywhere in `src/`; it is declared and nothing more, mirroring `KnowledgeSubmission`'s own identical pre-implementation existence.
- **Does this Unit implement permission enforcement?** No — `retrieve`'s own `requestingPrincipalId` parameter declares the contract shape only; no `PermissionEngine` reference, call, or dependency exists anywhere in this Unit's additions.
- **Does this Unit implement ranking?** No — confirmed both by design reasoning (above) and by a dedicated structural test asserting no ranking-shaped property exists on `KnowledgeRetrievalQuery`.
- **Does this Unit implement staleness?** No — `KnowledgeResultEntry.stale` is a declared field with no detection logic anywhere; nothing computes its value, mirroring `KnowledgeItem.status`'s own identical "field before mechanism" precedent from Unit 4.
- **Does this Unit touch Memory Core?** No — none of the five new declarations holds a `MemoryRetrieval`, `MemoryCore`, or any Memory-Core-adjacent dependency; all five are plain data holders or a bare, dependency-free interface.
- **Does this Unit perform runtime composition?** No — `src/composition/ParkerRuntime.kt` is untouched; confirmed by `git diff --stat` showing only `src/interfaces/KnowledgeStore.kt` and the new test file changed.
- **Does this Unit touch Reasoning Context?** No — no reference to `ReasoningContext`, `ReasoningContextAssembler`, or any reasoning-provider type appears anywhere in this Unit's additions.
- **Does this Unit modify any existing governance document?** No — confirmed by `git status`; only the two files listed below changed.
- **Does the two-variant disposition design narrow the Contract Design's own five-outcome requirement?** No — reasoned in Design Decision 1, above; all five outcomes remain independently distinguishable to a caller, expressed across construction validation, thrown exceptions, and the sealed type, exactly mirroring this codebase's own established pattern for identically-shaped situations elsewhere.

**No genuine defect found requiring correction.** No correction was therefore made, and no re-verification beyond the test run already reported above was required.

---

## Files Created

- `tests/contracts/KnowledgeRetrievalContractsTest.kt`
- `docs/reviews/PROGRAMME_3_UNIT_9_1_KNOWLEDGE_QUERY_RESULT_CONTRACTS_COMPLETION_REVIEW.md` (this document)

## Files Modified

- `src/interfaces/KnowledgeStore.kt` (additive only — 177 lines added, nothing existing altered, confirmed by `git diff --stat`)

## Test Results

- `KnowledgeRetrievalContractsTest`: **18/18 passed**
- Full repository suite: **1679 tests, 0 failures, 0 errors, 5 pre-existing skips**

## Constitutional Verdict

```
UNIT 9.1 — COMPLETE. NO DEFECT FOUND.
```

## Recommendation

Unit 9.1 satisfies its own Completion Gate (Implementation Plan §7): it compiles, every existing test still passes unmodified, every new test named for it passes, and no later unit was required to reach this state. Unit 9.2 (Deterministic Retrieval Engine) may now begin, its only dependency — this Unit — being complete. Units 9.3 and 9.4 may proceed once 9.2 lands, per the Implementation Plan's own ordering. Unit 9.5 remains blocked behind its own governance precondition, unaffected by this Unit's completion.

---

## Final Git Status

```
$ git status --short
 M src/interfaces/KnowledgeStore.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_1_KNOWLEDGE_QUERY_RESULT_CONTRACTS_COMPLETION_REVIEW.md
?? tests/contracts/KnowledgeRetrievalContractsTest.kt
```

Nothing staged, committed, or pushed.
