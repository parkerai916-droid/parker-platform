# Programme 3 Unit 9.4 — Retirement and Supersession Shaping — Defect Confirmation Review

## Status

**Defect confirmation review only — not a new Independent Constitutional Review.** This review does not re-examine the retired-item default policy, the supersession/multi-hop reasoning, the restoration treatment, the contract widening's lawfulness, or the Unit boundary — the genuine Independent Constitutional Review already confirmed each of those sound. It confirms only that the one required correction was applied precisely, and that no regression was introduced. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD:** `a231947445f8347c55256ac8c6000466b0fd62bf`
- **Branch:** `main`
- **Staged changes:** none.

---

## Defect Reviewed

The Independent Constitutional Review's one Required Correction: a fabricated quotation — "the single public path through which anything outside Knowledge Memory may observe promoted knowledge," attributed to Unit 9 Contract Design §1 — where the actual text reads "the **sole** public path," appearing in `src/interfaces/KnowledgeStore.kt`, `src/runtime/DefaultKnowledgeRetrieval.kt`, and this Unit's own Completion Review.

---

## Verification

Re-checked directly, by re-reading each of the three named locations and the actual Contract Design §1 text side by side:

1. **`src/interfaces/KnowledgeStore.kt`** — `KnowledgeRetrievalQuery`'s own KDoc now reads "Retrieval is 'the sole public path through which anything outside...'". Matches Contract Design §1 exactly.
2. **`src/runtime/DefaultKnowledgeRetrieval.kt`** — the "Lifecycle shaping (Unit 9.4)" KDoc section now reads "since this class is 'the sole public path through which anything outside Knowledge Memory may observe promoted knowledge'". Matches exactly.
3. **`docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_COMPLETION_REVIEW.md`** — the planning determination's own item 1 now reads "since Knowledge Retrieval is 'the sole public path through which anything outside Knowledge Memory may observe promoted knowledge'". Matches exactly.

Confirmed by direct search that no remaining occurrence of the fabricated "single public path" phrasing exists anywhere in `src/`, `tests/`, or this Unit's own Completion Review — the only remaining occurrences are inside the Independent Constitutional Review's own text, which documents the defect historically and must not itself be edited.

**Nothing else was changed.** The correction is a three-word substitution ("single" → "sole") at each of three sites; the surrounding reasoning, the widening's own shape, `isRetrievable`'s own logic, and every test are untouched.

**Correction confirmed correctly and completely applied.**

---

## Regression Check

- **Behaviour** — unchanged: `isRetrievable`, `matches`, `disclosureFor`, and `retrieve`'s own filtering/bounding/mapping logic are byte-identical to the version the Independent Constitutional Review examined; only KDoc text and one prose sentence in a review document changed.
- **Interfaces and signatures** — unchanged: `KnowledgeRetrievalQuery`'s own field set, `KnowledgeResultEntry`, and `KnowledgeRetrieval.retrieve`'s signature are untouched by this remediation pass.
- **Existing tests** — unchanged in meaning: no assertion in any test was altered; no test was added or removed by this remediation.
- **Test results:**
  ```
  $ ./gradlew test --tests "*DefaultKnowledgeRetrievalTest*" --tests "*KnowledgeRetrievalContractsTest*"
  BUILD SUCCESSFUL
  ```
  `DefaultKnowledgeRetrievalTest`: **41/41 passed**. `KnowledgeRetrievalContractsTest`: **22/22 passed**. Unchanged from the pre-correction run, as expected for a documentation-only fix.
  ```
  $ ./gradlew clean test
  BUILD SUCCESSFUL in 46s
  ```
  Full repository suite: **1727 tests, 0 failures, 0 errors, 5 pre-existing skips** — identical to the pre-correction count, confirming zero regression.

**No regression found.**

---

## Confirmations

- The one required correction is present, verified against the exact text of each of the three affected locations.
- No architectural decision changed — the retired-item default policy, the status-only filtering rationale for restoration, the unwidened `KnowledgeResultEntry`, and the `includeRetired` field's own shape are all untouched.
- No constitutional conclusion from the Independent Constitutional Review's other seven review areas was revisited or altered.
- No new governance document was created; only the same three files the defect was found in were touched, plus this document.
- The Independent Constitutional Review document itself was not modified.

---

## Recommended Next Step

Unit 9.4 may now be treated as fully accepted, with `KnowledgeRetrievalQuery.includeRetired: Boolean = false` as its adopted widening and the disclosed, status-only lifecycle-shaping policy in `DefaultKnowledgeRetrieval` as its adopted default. Unit 9.5 (Permission Enforcement Wiring) remains blocked behind its own governance precondition (the not-yet-drafted enforcement-mechanism Clarification), unaffected by this Unit. Unit 9.6 (Runtime Composition) remains strictly last, depending on Units 9.1 through 9.5 all being complete.

---

## Final Git Status

```
$ git status --short
 M src/interfaces/KnowledgeStore.kt
 M src/runtime/DefaultKnowledgeRetrieval.kt
 M tests/contracts/KnowledgeRetrievalContractsTest.kt
 M tests/runtime/DefaultKnowledgeRetrievalTest.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_COMPLETION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
