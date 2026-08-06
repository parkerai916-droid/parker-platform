# Programme 3 Unit 9.2 — Deterministic Retrieval Engine — Defect Confirmation Review

## Status

**Defect confirmation review only — not a new Independent Constitutional Review.** This review does not re-examine the architecture, the matching/ordering design, or any constitutional conclusion the Independent Constitutional Review already reached. It confirms only whether the three required corrections were correctly implemented, and that no regression was introduced. No behaviour, interface, signature, ordering, or implementation logic was changed during remediation. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD:** `d9725f4bbfd7f2304dee90b4cfcdf4db9e053c11` (`d9725f4`)
- **Branch:** `main`
- **Staged changes:** none.

---

## Defects Reviewed

The Independent Constitutional Review's three required corrections:

1. Add KDoc to `DefaultKnowledgeRetrieval` disclosing that active and retired items are currently both returned unconditionally, that this is the absence of a decision, and that it must not be read as Unit 9.4's own adopted policy.
2. Add a test constructing a `RETIRED` item and asserting the current provisional behaviour, documented as a regression guard, not a policy statement.
3. Extend the `stale = true` KDoc to state explicitly that the placeholder is never observable by a real caller before Unit 9.3, because Unit 9.6 is strictly last in the Implementation Plan's own ordering.

---

## Verification

All three corrections re-read directly from the current files and confirmed:

1. **Lifecycle-status disclosure** (`src/runtime/DefaultKnowledgeRetrieval.kt`, new "## Lifecycle status -- currently unfiltered, an absence of policy, not a policy" section). States: "`retrieve` applies no filtering by `KnowledgeItem.status` of any kind... This is not a considered 'include retired items by default' policy decision. It is the simple absence of any lifecycle-aware filtering logic in this Unit at all." Names Contract Design §6 and Unit 9.4 as the actual owner of that decision, and states the current behaviour "must not be relied upon as stable, and remains subject to change, in either direction."
2. **Retired-item test** (`tests/runtime/DefaultKnowledgeRetrievalTest.kt`, new test `a RETIRED item is currently returned identically to an ACTIVE one -- this protects today's implementation from accidental change and does not establish Unit 9-4 policy`). Its own inline comment states explicitly: "This test exists only to guard today's unconditional-inclusion behaviour against silent, accidental change in either direction; it must not be read as approving, requiring, or predicting whatever default Unit 9.4 eventually adopts." Constructs a genuine `KnowledgeItemStatus.RETIRED` item and asserts it is matched and returned, with its retired status preserved on the returned entry.
3. **Staleness-placeholder safety disclosure** (`src/runtime/DefaultKnowledgeRetrieval.kt`, appended to the existing "## Staleness" section). States: "No class in this codebase constructs `DefaultKnowledgeRetrieval` outside this Unit's own tests... the Unit 9 Implementation Plan's own ordering fixes Unit 9.6 as strictly last, gated on Units 9.1 through 9.5 all being complete -- including Unit 9.3."

**All three corrections are correctly and precisely implemented, satisfying every element the Independent Constitutional Review's own required-correction text named.**

---

## Regression Check

Confirmed by direct re-inspection and by re-running the affected tests:

- **Behaviour** — unchanged: `matches`, `retrieve`'s own matching/filtering/bounding logic, and `findAll`'s own insertion-order return are byte-identical to the version the Independent Constitutional Review examined; only KDoc was added to `DefaultKnowledgeRetrieval.kt`.
- **Interfaces and signatures** — unchanged: `KnowledgeRetrieval.retrieve`'s signature, `KnowledgeItemPersistence`'s three methods, and every Unit 9.1 type are untouched by this remediation pass.
- **Ordering and implementation boundaries** — unchanged: no new dependency, no new class, no change to which Unit owns which responsibility.
- **Existing tests** — unchanged in meaning: no assertion in any pre-existing test was altered; only one new test was added.
- **Test results:**
  ```
  $ ./gradlew test --tests "*DefaultKnowledgeRetrievalTest*" --tests "*KnowledgeItemPersistenceTest*" --tests "*DefaultKnowledgeSubmissionTest*"
  BUILD SUCCESSFUL
  ```
  `DefaultKnowledgeRetrievalTest`: **16/16 passed** (15 prior + 1 new). `KnowledgeItemPersistenceTest` and `DefaultKnowledgeSubmissionTest`: unaffected, still passing.
  ```
  $ ./gradlew clean test
  BUILD SUCCESSFUL in 47s
  ```
  Full repository suite: **1698 tests, 0 failures, 0 errors, 5 pre-existing skips** (up from 1697 before this remediation; +1 new test, 0 regressions).

**No regression found.** All changes are additive KDoc plus one additive test.

---

## Confirmations

- All three required corrections are present, verified against the exact text of each.
- No architectural decision changed — `findAll()`'s own layering, the matching target, and the ordering mechanism are all untouched.
- No constitutional conclusion changed — the Independent Constitutional Review's own findings on Memory Core separation, provider neutrality, determinism, and the absence of ranking/permission/runtime-composition/Reasoning-Context work all remain as stated.
- No implementation behaviour changed — confirmed by direct diff inspection and by the identical pass/fail outcome of every pre-existing test.
- No new governance document was created.
- The Completion Review was not modified.

---

## Recommended Next Step

Unit 9.2 may now be treated as fully accepted. Unit 9.4 (Retirement and Supersession Retrieval-Shape Decision) may begin, per the Implementation Plan's own ordering — its own real decision will resolve the lifecycle-status question this remediation pass disclosed rather than answered. Unit 9.3 (Staleness Disclosure Mechanism) may also begin in parallel. Unit 9.5 remains blocked behind its own governance precondition, unaffected by this remediation.

---

## Final Git Status

```
$ git status --short
 M src/runtime/KnowledgeItemPersistence.kt
 M tests/runtime/DefaultKnowledgeSubmissionTest.kt
 M tests/runtime/KnowledgeItemPersistenceTest.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_COMPLETION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? src/runtime/DefaultKnowledgeRetrieval.kt
?? tests/runtime/DefaultKnowledgeRetrievalTest.kt
```

Nothing staged, committed, or pushed.
