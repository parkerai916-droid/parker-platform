# Programme 3 Unit 9.3 — Staleness Disclosure — Defect Confirmation Review

## Status

**Defect confirmation review only — not a new Independent Constitutional Review.** This review does not re-examine the architecture, the matching/ordering design, or any constitutional conclusion the genuine Independent Constitutional Review already reached. It confirms only whether that review's two Required Corrections were correctly implemented, including the constitutional design determination Required Correction 1 itself demanded, and that no regression was introduced. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD:** `883cb6b1bffab7870b887dafbc2c445062631b47` (`883cb6b1`)
- **Branch:** `main`
- **Staged changes:** none.

---

## Defects Reviewed

The genuine Independent Constitutional Review's (`docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`) two Required Corrections:

1. Determine, explicitly and on the record, whether `KnowledgeResultEntry.stale` should be widened from `Boolean` to a representation capable of expressing a genuine indeterminate state, exercising the authority Unit 9.1's own KDoc already grants — reasoning through the review's own Section 3 and Section 4 findings rather than deferring the question again — and implement the widening if the determination concludes it is warranted.
2. Rewrite the "Staleness" KDoc section to state plainly, without softening, that age is not authorised by any governing document as a staleness signal, that the governed condition is evidence-status change (quoted exactly), and that the false-negative direction (recently-classified items whose evidence may have already changed) is the more serious of the proxy's two failure modes, not a symmetric limitation.

Findings 4 (retirement/restoration timestamp selection) and 5 (missing indeterminate test) were named as recommended clarifications the review's own text permitted addressing "alongside Correction 1 rather than separately" — both were, in fact, addressed alongside it, and are verified below alongside the two Required Corrections proper.

---

## Verification

### Required Correction 1 — the widening determination

Re-read directly from `docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_COMPLETION_REVIEW.md`, section "The Widening Determination": the determination is made explicitly and on the record — **"widening is constitutionally required"** — reasoned through five numbered points grounded in Contract Design V2 §3's own confirmed-not-inferred definition, the false-negative-is-the-governing-problem framing, the limits of `DefaultKnowledgeCandidateEvaluator`'s "disclosed narrower baseline" precedent, the explicit non-equivalence to `DefaultWorldModelUpdatePolicy`'s own, genuinely different age-based model, and Unit 9.1's own pre-authorisation of exactly this widening as a "disclosed contract amendment." This is not deferral — no future determination is left open, and no reader is left to independently rediscover the review's own analysis.

**Implementation, confirmed present:**

- `src/interfaces/KnowledgeStore.kt`: `KnowledgeResultEntry.stale: Boolean` is replaced by `KnowledgeResultEntry.staleness: StalenessDisclosure`. `StalenessDisclosure` is a four-value enum: `CONFIRMED_CURRENT` and `CONFIRMED_STALE` (reserved, never assigned by any mechanism built as of Unit 9.3), `POSSIBLY_STALE` and `INDETERMINATE` (the two values Unit 9.3's own mechanism actually assigns). The field's own KDoc, and the enum's own KDoc, are both present and explain the four-value design and who may assign each value.
- `src/runtime/DefaultKnowledgeRetrieval.kt`: `isStale(item): Boolean` is replaced by `disclosureFor(item): StalenessDisclosure`, called from `retrieve` as `KnowledgeResultEntry(item = item, staleness = disclosureFor(item))`. `disclosureFor` never assigns `CONFIRMED_CURRENT` or `CONFIRMED_STALE` — confirmed by direct reading of the method body, which returns only `POSSIBLY_STALE` or `INDETERMINATE`.
- `tests/runtime/DefaultKnowledgeRetrievalTest.kt` and `tests/contracts/KnowledgeRetrievalContractsTest.kt`: every construction site and assertion updated to the new field name and enum values; confirmed by `grep -rn "\.stale\b\|stale = " src/ tests/` returning no matches anywhere in the repository.

**Required Correction 1 is correctly and fully implemented.**

### Required Correction 2 — the "Staleness" KDoc rewrite

Re-read directly from `src/runtime/DefaultKnowledgeRetrieval.kt`'s own "## Staleness" section:

- **States plainly that age is not authorised as a staleness signal:** "**No governing document authorises age, by itself, as a staleness signal.**"
- **Quotes the governed condition exactly:** the section opens by quoting Contract Design V2 §3 (Amendment 7) verbatim — "Where the underlying evidence's status changes afterward (for example, becomes disputed) before Knowledge Memory has re-evaluated and produced a new classification, the Knowledge Item is stale, and this must never be silently concealed" — and states directly: "**Staleness, as governed, is an evidence-status-change condition. It is never an elapsed-time condition.**"
- **States the false-negative direction is the more serious failure mode, not symmetric:** "**The false-negative direction is the more serious of this proxy's two failure modes, not a symmetric limitation.**" followed by a full explanation of why a freshly-classified item's honest `INDETERMINATE` disclosure resolves the concealment risk a `Boolean false` would have carried, and why an old item's `POSSIBLY_STALE` is "the comparatively minor failure mode."

**Required Correction 2 is correctly and fully implemented, satisfying every element the review's own required-correction text named.**

### Finding 4 — retirement/restoration reference-timestamp selection

Re-read directly from `disclosureFor`: the reference timestamp is now selected via `item.history.filterIsInstance<KnowledgePromotion>().lastOrNull()?.occurredAt`, excluding `KnowledgeRetirement` and `KnowledgeRestoration` entries. Two new tests confirm this behaviourally: `a KnowledgeRetirement following a promotion does not govern the staleness reference timestamp` and `a KnowledgeRestoration following a retirement does not govern the staleness reference timestamp either`, both passing.

### Finding 5 — missing indeterminate test

Confirmed present and strengthened well beyond a single test: every "fresh item" test now asserts `INDETERMINATE` explicitly (rather than the old `assertFalse`), and a dedicated test — `an item with no KnowledgePromotion history entry discloses INDETERMINATE, never POSSIBLY_STALE` — covers the no-reference-timestamp edge case explicitly.

---

## Regression Check

Confirmed by direct re-inspection and by re-running the affected tests:

- **Matching, ordering, bounding** — unchanged: `matches`, `retrieve`'s own filtering/bounding logic, and `findAll`'s own insertion-order return are unaffected; only the staleness computation and its return type changed.
- **Memory Core separation** — unchanged: no `MemoryRetrieval`/`MemoryCore` reference exists anywhere in `DefaultKnowledgeRetrieval.kt`; the existing structural test proving this still passes unmodified.
- **Permission, ranking, runtime-composition boundaries** — unchanged: no `PermissionEngine` reference, no ranking/scoring method, `ParkerRuntime.kt` untouched (confirmed no diff against it).
- **Existing tests not concerned with staleness** — unchanged in meaning: no assertion outside the staleness-disclosure block was altered.
- **Test results:**
  ```
  $ ./gradlew test --tests "*DefaultKnowledgeRetrievalTest*" --tests "*KnowledgeRetrievalContractsTest*"
  BUILD SUCCESSFUL
  ```
  `DefaultKnowledgeRetrievalTest`: **26/26 passed** (23 prior + 3 net new). `KnowledgeRetrievalContractsTest`: **19/19 passed** (18 prior + 1 net new), including the new `StalenessDisclosure` closure test.
  ```
  $ ./gradlew clean test
  BUILD SUCCESSFUL in 46s
  ```
  Full repository suite: **1709 tests, 0 failures, 0 errors, 5 pre-existing skips** (up from 1705 before this remediation; +4 net new tests, 0 regressions).

**No regression found.**

---

## Confirmations

- Both Required Corrections are present, verified against the exact text of each.
- Both recommended clarifications (Finding 4, Finding 5) were addressed alongside Required Correction 1, exactly as the review's own text permitted.
- No architectural decision changed — Memory Core separation, the age-based mechanism's own "checked at query time" direction, and the injected-`Clock` determinism exemption are all untouched; only the disclosed value's own shape widened.
- No constitutional conclusion from the genuine Independent Constitutional Review's other nine review areas was revisited or altered by this remediation.
- No new governance document was created; only two existing review documents (`..._COMPLETE_REVIEW.md`, this document) and four source/test files changed.
- The Independent Constitutional Review document itself was not modified.

---

## Recommended Next Step

Unit 9.3 may now be treated as fully accepted, with `KnowledgeResultEntry.staleness: StalenessDisclosure` as its adopted public shape. Unit 9.4 (Retirement and Supersession Retrieval-Shape Decision) may begin, per the Implementation Plan's own ordering — its dependency on Unit 9.2 remains satisfied, and this widening does not touch lifecycle-status filtering. Unit 9.6 (Runtime Composition) remains strictly last and unaffected. Unit 9.5 remains blocked behind its own governance precondition.

---

## Final Git Status

```
$ git status --short
 M src/interfaces/KnowledgeStore.kt
 M src/runtime/DefaultKnowledgeRetrieval.kt
 M tests/contracts/KnowledgeRetrievalContractsTest.kt
 M tests/runtime/DefaultKnowledgeRetrievalTest.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_COMPLETION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
