# Programme 3 — Unit 9.5: Permission Enforcement Implementation — Completion Review

## Status

**Implementation completion review.** Unit 9.5 only is implemented, exactly as the adopted `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` ("the Clarification") fixes, on top of Units 9.1–9.4's own already-accepted behaviour, unchanged. Unit 9.6 (Runtime Composition) is not begun. No governance document was modified. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD at start:** `8b929c223ba11af5df66da061373239002b12269`
- **Branch:** `main`
- **Working tree at start:** three untracked governance files (the adopted Clarification and its two review documents), no other change. Units 9.1 through 9.4 accepted, committed, and pushed.

---

## Governance Read Before Implementation

Read fresh, in full: `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` (Adopted, all 17 sections); `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (§4, §5, §9); `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (§7, §8); `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` (Unit 9.5 entry). Source read in full: `src/runtime/DefaultKnowledgeRetrieval.kt` (post-Unit-9.4); `src/runtime/DefaultKnowledgeSubmission.kt` (the closest self-gating precedent the Clarification names); `src/interfaces/PermissionEngine.kt`; `src/contracts/ExecutionRequest.kt`; `src/contracts/Permission.kt`; `tests/runtime/FakePermissionEngine.kt`; `tests/runtime/DefaultKnowledgeSubmissionTest.kt` (the test-fixture pattern this Unit's own tests mirror).

---

## What Was Implemented

**`src/runtime/DefaultKnowledgeRetrieval.kt`.** `retrieve` now performs, in order:

1. **Act-level gate.** Evaluates `permissionEngine` exactly once, using a fixed `ExecutionRequest` (`KNOWLEDGE_RETRIEVAL_RESOURCE_ID` / `RETRIEVE_ACTION_NAME`, `intent = ACT_LEVEL_INTENT`, `correlationId = query.correlationId`), **before** `persistence.findAll()` is ever called. On any outcome other than `APPROVED`/`APPROVED_WITH_CONFIRMATION`, returns `KnowledgeRetrievalDisposition.NotAuthorised` with a reason naming the requesting principal — no persistence read, no matching, no lifecycle shaping, no staleness computation occurs on this path.
2. **Matching and lifecycle shaping**, unchanged: `persistence.findAll().filter { matches(item, query.relevance) && isRetrievable(item, query) }` — byte-identical to Unit 9.2/9.4's own logic.
3. **Item-level gate.** For each item surviving step 2, in the same order, evaluates `permissionEngine` once more, using the same fixed resource/action pair, `intent` naming the specific item (`itemLevelIntent`), and the same `query.correlationId`. An item not `APPROVED`/`APPROVED_WITH_CONFIRMATION` is silently excluded — never surfaced as a distinguishable denial.
4. **Bounding**, applied to the permission-approved set: `.take(query.maximumResults)`, unchanged Unit 9.2 bounding semantics, now operating on a possibly-smaller, already-visible set.
5. **Staleness disclosure**, unchanged Unit 9.3 logic (`disclosureFor`), computed only for the final, bounded, approved set.
6. Returns `KnowledgeRetrievalDisposition.Retrieved`.

New constructor parameter: `permissionEngine: PermissionEngine`, mandatory, positioned between `persistence` and the still-defaulted `clock` — no default value, since a default-approving fallback would be self-authorisation, which no governing document permits. New companion constants: `KNOWLEDGE_RETRIEVAL_RESOURCE_ID = ResourceId("knowledge-memory-retrieval")`, `RETRIEVE_ACTION_NAME = "knowledge.retrieve"`, `ACT_LEVEL_INTENT`. New private members: `isAuthorised(decision)`, `itemLevelIntent(item)`, `buildExecutionRequest(...)`.

Matching (`matches`), lifecycle shaping (`isRetrievable`), and staleness disclosure (`disclosureFor`) are byte-identical to the pre-Unit-9.5 implementation — none was touched.

**`tests/runtime/DefaultKnowledgeRetrievalTest.kt`.** Every pre-existing construction site updated to supply a `PermissionEngine` (an `approvingEngine()` fake by default, so every Unit 9.2/9.3/9.4 behavioural test continues to exercise exactly the behaviour it always did, now passing through an always-approving gate). A new "Permission Enforcement (Unit 9.5)" section adds 22 new tests (see Tests Added, below). Three pre-existing structural tests were updated to reflect the new, larger, correct shape: the constructor-arity test (two → three dependencies); the "no MemoryRetrieval/MemoryCore/PermissionEngine dependency" test (PermissionEngine removed from the forbidden list — it is now a required, correct dependency); the obsolete "retrieve never returns NotAuthorised" test was removed (its own premise is now false) and its coverage is superseded by the new act-level-denial tests.

---

## Design Decisions and Reasoning

1. **Two-tier gate, implemented exactly as the Clarification fixes.** No design latitude was exercised here — the Clarification (§6.2, §8) already fixed the act-level/item-level split, the evaluation order, and the bounding-after-filtering placement. This Unit translates that fixed mechanism into Kotlin without introducing any policy of its own.
2. **`intent` as the per-item distinguishing signal.** The Clarification fixes that resource and action must stay identical across both granularities (§7) but says nothing about `ExecutionRequest.intent`, a free-text field this codebase already uses descriptively (`DefaultEvidenceCustodian`'s own "Accept evidence artifact..."/"Retrieve evidence artifact..." convention). `itemLevelIntent` embeds the item's own `KnowledgeId` so a future, genuinely item-aware policy — and this Unit's own tests — can distinguish which item is under evaluation without inventing a second resource or action identifier, which the Clarification's own Section 7 and Section 11 (Explicit Non-Expansions) both forbid.
3. **`requestId` freshly minted per evaluation; `correlationId` propagated unchanged.** Exactly as the Clarification §9 fixes: `correlationId` always equals `query.correlationId`, identical across every evaluation for one query; `requestId` is a fresh UUID per evaluation, addressing that one specific evaluation, mirroring `DefaultKnowledgeSubmission.buildExecutionRequest`'s own per-call `requestId` minting.
4. **A sequential `for` loop for item-level evaluation, not `.filter`.** `PermissionEngine.evaluate` is `suspend`; Kotlin's stdlib `List.filter` does not accept a suspend predicate. A sequential loop, building an ordered `MutableList`, preserves exactly the same relative order `.filter` would have (Contract Design §8's own ordering guarantee), evaluates each candidate exactly once, and requires no new dependency (no `kotlinx.coroutines.flow` import) to achieve it.

---

## Verification Performed

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 46s
5 actionable tasks: 5 executed
```

- **`DefaultKnowledgeRetrievalTest`:** 63/63 passed.
- **Full repository suite:** 1749 tests, 0 failures, 0 errors, 5 pre-existing skips (up from 1727 at the Unit 9.4 accepted baseline; +22 new tests, 0 regressions).

---

## Tests Added (22, all in `tests/runtime/DefaultKnowledgeRetrievalTest.kt`)

- **Authorised retrieval** — an authorised query returns `Retrieved`; `retrieve` requires an actual Permission Engine call.
- **Act-level denial** — `DENIED` returns `NotAuthorised` and never reads persistence; `DEFERRED` treated identically; `NotAuthorised.reason` is non-blank and names the requesting principal; an act-level denial performs no matching/shaping/staleness computation.
- **Mixed authorised/unauthorised items** — an item-level `DENIED` silently excludes that item; an item-level `DEFERRED` is silently excluded too.
- **Complete filtering** — when every item is denied at item level, the result is an ordinary empty `Retrieved`, never `NotAuthorised`.
- **Deterministic ordering after filtering** — insertion order preserved minus the denied item, repeatable across calls.
- **Retirement + permission filtering composed** — a `RETIRED` item excluded by lifecycle shaping never reaches the item-level gate at all; an explicitly `includeRetired`-admitted `RETIRED` item is still subject to it.
- **Superseded history preservation** — a full multi-hop history remains intact on an item passing both gates.
- **Correlation identifier propagation** — `query.correlationId` propagated unchanged into every evaluation, act-level and item-level; propagated even on act-level denial.
- **Resource/action correctness** — every request, at both granularities, names the fixed resource and action; the two granularities differ only by `intent`.
- **Principal propagation** — `requestingPrincipalId` propagated unchanged into every evaluation.
- **Exact evaluation count / no double evaluation** — `1 + N` on approval; `1` on denial; no `requestId` is ever reused across evaluations.
- **Boundary conditions** — empty persistence evaluates only the act-level gate; `maximumResults` bounds the permission-approved set, not the pre-filter candidate set (a denied item does not consume a bounding slot).
- **Structural** — three constructor dependencies (was two); `PermissionEngine` correctly a required, not forbidden, dependency; the two-argument construction site (`persistence`, `permissionEngine`) remains valid with `clock` still defaulted.

---

## Unit Boundary Confirmation

- **No Unit 9.6 work** — confirmed via `git diff --stat -- src/composition/ParkerRuntime.kt`: no output, file untouched. No class outside this Unit's own tests constructs `DefaultKnowledgeRetrieval`.
- **No Reasoning Context integration** — no such reference anywhere in the diff.
- **No Memory Core dependency beyond the already-governed permission interface** — confirmed by the (renamed, still-passing) structural test; `PermissionEngine` is the only new dependency, and it is Trust-Framework-owned, not Memory-Core-owned.
- **No ranking, semantic retrieval, fuzzy matching, scoring, or provider-specific behaviour** — `matches` is untouched (still an unweighted, case-insensitive substring check); the existing "no ranking/scoring/weighting method" structural test still passes unmodified.
- **Retirement/supersession (Unit 9.4) and staleness (Unit 9.3) behaviour preserved** — `isRetrievable` and `disclosureFor` are byte-identical to the pre-Unit-9.5 file; every pre-existing Unit 9.3/9.4 behavioural test still passes, now additionally exercised through an always-approving gate.
- **Deterministic ordering preserved after filtering** — confirmed both by direct code inspection (the item-level loop iterates in `persistence.findAll()`'s own order, never reordering) and by the new dedicated test.

---

## Files Modified

- `src/runtime/DefaultKnowledgeRetrieval.kt` (two-tier permission gate added; new `PermissionEngine` constructor dependency; matching, lifecycle shaping, and staleness disclosure unchanged)
- `tests/runtime/DefaultKnowledgeRetrievalTest.kt` (every construction site updated; 22 new tests added; 3 structural tests updated for the new, correct shape; 1 obsolete test removed)

## Files Created

- `docs/reviews/PROGRAMME_3_UNIT_9_5_PERMISSION_ENFORCEMENT_IMPLEMENTATION_COMPLETION_REVIEW.md` (this document)

## Test Results

- `DefaultKnowledgeRetrievalTest`: **63/63 passed**
- Full repository suite: **1749 tests, 0 failures, 0 errors, 5 pre-existing skips**

## Constitutional Verdict

```
UNIT 9.5 -- COMPLETE.
```

## Recommendation

Unit 9.5 satisfies its own Completion Gate (Implementation Plan §7): it compiles, every existing test still passes, every new test named for it passes, and no later unit was required to reach this state. An Independent Constitutional Review follows this document. Unit 9.6 (Runtime Composition) may begin only once every one of Units 9.1–9.5 is complete — that gate is now satisfied on the implementation side, pending this Unit's own Independent Constitutional Review.

---

## Final Git Status

```
$ git status --short
 M src/runtime/DefaultKnowledgeRetrieval.kt
 M tests/runtime/DefaultKnowledgeRetrievalTest.kt
?? docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md
?? docs/reviews/PROGRAMME_3_UNIT_9_5_PERMISSION_ENFORCEMENT_IMPLEMENTATION_COMPLETION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
