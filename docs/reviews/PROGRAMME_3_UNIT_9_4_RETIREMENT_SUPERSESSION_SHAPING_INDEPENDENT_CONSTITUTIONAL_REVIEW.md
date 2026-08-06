# Programme 3 — Unit 9.4: Retirement and Supersession Shaping — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review.** Performed as if by another reviewer, against the governing documents re-read fresh for this task, not against the Completion Review's own summary of them. No production code or test was modified during this review. Unit 9.5 and Unit 9.6 are not begun. Nothing is staged, committed, or pushed.

---

## Repository Baseline Confirmed

- **Branch:** `main`
- **HEAD:** `a231947445f8347c55256ac8c6000466b0fd62bf` (unchanged since Unit 9.4 implementation began)
- **Working tree:** exactly the expected Unit 9.4 changes —
  ```
   M src/interfaces/KnowledgeStore.kt
   M src/runtime/DefaultKnowledgeRetrieval.kt
   M tests/contracts/KnowledgeRetrievalContractsTest.kt
   M tests/runtime/DefaultKnowledgeRetrievalTest.kt
  ?? docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_COMPLETION_REVIEW.md
  ```
  No discrepancy from the expected set.
- **Staged changes:** none.

---

## Required Reading, Re-Read Fresh For This Review

`docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md`; `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (§1, §4, §6, §11 read word-for-word against every quotation the implementation makes of them); `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` §10, §12; `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §3, §6; `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §7, §8; `src/interfaces/KnowledgeStore.kt` (`KnowledgeItem`, `KnowledgeItemStatus`, the three lifecycle event variants, the widened `KnowledgeRetrievalQuery`); `src/runtime/DefaultKnowledgeRetrieval.kt` (the full current file, including the new "Lifecycle shaping" section); `tests/runtime/DefaultKnowledgeRetrievalTest.kt`; `tests/contracts/KnowledgeRetrievalContractsTest.kt`. This review does not rely on the Completion Review's own prose as a substitute for reading the underlying governance directly — every quotation the implementation makes was independently checked, word-for-word, against its cited source.

---

## Review Findings

### 1. Whether retirement policy came from governance rather than implementation preference

The chosen default (excluded, opt-in via `includeRetired`) is traced to two governing texts genuinely in tension — Contract Design §1's "task-scoped request for relevant, already-promoted knowledge" (naturally current knowledge) against Contract Design V2 §3's "retirement never implies deletion... remain retained and retrievable" (forecloses permanent exclusion) — and to Contract Design §6's own explicit naming of "included only under an explicit caller criterion" as one of exactly three lawful outcomes it reserved for this Unit to choose among. This is not implementation preference dressed as governance: all three candidate defaults were genuinely available, the reasoning for the one chosen is disclosed in full in the source KDoc itself (not only in the Completion Review), and the choice does not exceed the latitude Contract Design §6 and §11 explicitly grant. **Sound.**

### 2. Whether superseded items remain retrievable

Confirmed directly: `retrieve` forwards each matched `KnowledgeItem` unchanged, including its full `history`, in every code path. No method truncates, filters, re-projects, or drops any `KnowledgeLifecycleEvent`. Verified behaviourally by `a superseded classification remains retrievable as part of the same item's own history, never as a separate item`. **Sound.**

### 3. Whether multi-hop chains are preserved

Confirmed directly by `a multi-hop supersession chain of four classifications remains transitively retrievable in full, never truncated to the latest`, which asserts the entire four-entry chain, in order, is present on the returned entry. No code path bounds, samples, or windows `history`. **Sound.** (Noted, not a defect: `matches` — Unit 9.2's own fixed, unmodified decision — matches only against the *most recent* history entry's own `basis`, so an item is discoverable only via its current text, never independently via an older hop's own text. This is a pre-existing, already-disclosed Unit 9.2 boundary, outside Unit 9.4's own scope to revisit, and does not affect whether an already-matched item's chain is preserved once returned.)

### 4. Whether restoration semantics are correct

Contract Design §6 fixes that restoration "returns its status to active" and that "a restored item is retrievable exactly as any other active item once restored." `isRetrievable` filters on `KnowledgeItem.status` alone, never on whether `history` contains a `KnowledgeRetirement` — verified directly in `isRetrievable`'s own body (a two-line function, read in full) and by the behavioural test constructing a genuine promoted → retired → restored item and confirming it is included by default with its full three-entry history intact. **Sound**, and the KDoc's own disclosed reasoning for *why* status-only filtering is correct (rather than history-contains-retirement, which would "incorrectly re-exclude every restored item forever") is substantively accurate, not merely asserted.

### 5. Whether any "latest" or "current" selection was invented

Confirmed by direct reading of `retrieve`, `matches`, `isRetrievable`, and `disclosureFor` — none computes, selects, or returns anything resembling a "latest" or "most current" projection of `history`; the entire `KnowledgeItem`, including every historical entry, is what a caller receives. Verified behaviourally by `no latest-only selection occurs -- the full item, not a projection of only its current classification, is returned`, which asserts full structural equality between the stored item and the returned one. **Sound.**

### 6. Whether the query/result contract was widened lawfully

`KnowledgeRetrievalQuery.includeRetired: Boolean = false` is the only widening. It is additive (no existing field removed, renamed, or repurposed), defaulted (no existing three-argument construction site breaks), and traces directly to Contract Design §6's own named "explicit caller criterion" outcome — not a newly invented authority. `KnowledgeResultEntry` was correctly left unwidened: the reasoning that a retired item's own `status` field and an item's own unchanged `history` already carry everything a caller needs was checked against Contract Design §6's own "Superseded" and "Retired" paragraphs directly and holds. **Sound**, subject to Required Correction 1, below (a citation-accuracy defect in the reasoning's own supporting quotation, not in the widening's substance or lawfulness).

### 7. Whether lifecycle state was confused with permission

Checked directly: `isRetrievable` never reads `requestingPrincipalId`, never calls anything permission-shaped, and returns a plain `Boolean`. The KDoc's own dedicated paragraph ("`includeRetired` is a structural criterion, never a permission signal") correctly identifies and quotes Contract Design §6's own "lifecycle status is never a substitute for, or determinant of, a permission decision" and applies it, accurately, to the new field by direct analogy to `KnowledgeItem.status`'s own identical treatment. The structural test confirming `includeRetired` is a plain, non-nullable `Boolean` (not a permission-shaped type) and the behavioural test confirming it grants no permission outcome both exist. **Sound.**

### 8. Whether Unit 9.5 or 9.6 work leaked in

Confirmed independently, not merely by re-reading the Completion Review's own claim: `git diff --stat -- src/composition/ParkerRuntime.kt` produces no output (file untouched); `git diff --name-only` lists exactly four files, none under `src/composition/`; no `PermissionEngine` reference, resource/action string, denial-filtering branch, caching field, or indexing structure exists anywhere in the diff (checked by direct reading of both changed source files in full, not by keyword search alone). **Sound.**

---

## Required Correction

### Correction 1 — a fabricated quotation, appearing in three files

**The defect.** Both `src/interfaces/KnowledgeStore.kt` and `src/runtime/DefaultKnowledgeRetrieval.kt`, plus this Unit's own Completion Review, ground part of the planning determination in a quotation attributed to Unit 9 Contract Design §1: *"the single public path through which anything outside Knowledge Memory may observe promoted knowledge."*

Checked word-for-word against the actual Contract Design §1 text: *"It is the **sole** public path through which anything outside Knowledge Memory may observe promoted knowledge."* The word actually used is **"sole,"** never "single." "Single public path" is a different phrase — one that happens to appear verbatim elsewhere in the same Contract Design (§4's "the retrieval interface — the single public path through which **a Knowledge Query is answered**"), describing a different guarantee (that there is exactly one retrieval *operation*, not that Retrieval is the sole *observation point* into Knowledge Memory). The implementation's own quotation blends the wording of one sentence with the citation of another, producing text inside quotation marks that matches neither source verbatim.

**Why this is a genuine defect, not pedantry.** This repository's own established discipline (seen throughout Units 9.1–9.3's own accepted KDoc) treats governing text inside quotation marks as a verbatim, checkable citation load-bearing for a constitutional conclusion — not paraphrase. Here, the misquoted sentence is doing real argumentative work: it is one of exactly two governing texts the planning determination holds "in tension" to justify rejecting a permanent, non-overridable exclusion of retired items. A reader who checks the citation, as this review did, finds it does not say what the quotation marks claim. The underlying substantive point survives unchanged once corrected — the actual §1 sentence ("sole public path... may observe promoted knowledge") supports the same conclusion the misquote was used for — so this does not overturn the determination itself; it corrects a citation-accuracy defect in three files' own supporting text.

**Where it appears**, confirmed by direct search:
- `src/interfaces/KnowledgeStore.kt`, `KnowledgeRetrievalQuery`'s own KDoc.
- `src/runtime/DefaultKnowledgeRetrieval.kt`, the "Lifecycle shaping (Unit 9.4)" KDoc section.
- `docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_COMPLETION_REVIEW.md`, the planning determination's own item 1.

**Required correction:** replace "the single public path" with the accurate quotation, "the sole public path," in all three locations, changing nothing else about the surrounding reasoning, which remains correct once the citation is accurate.

No other required correction was found.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

One required correction — a quotation-accuracy defect, not a substantive or architectural one. The retired-item default policy, the supersession and multi-hop reasoning, the restoration treatment, the "no latest-only selection" guarantee, the lawfulness and minimality of the contract widening, and the Unit boundary are all confirmed sound and require no change.

---

## Recommended Next Step

Apply Correction 1 only (a three-file, three-word-per-site text substitution); do not re-open any other section of the implementation. A narrow Defect Confirmation Review follows, confirming the correction was applied precisely and that no regression was introduced, without repeating this full review.

---

## Final Git Status at Time of This Review

```
$ git status --short
 M src/interfaces/KnowledgeStore.kt
 M src/runtime/DefaultKnowledgeRetrieval.kt
 M tests/contracts/KnowledgeRetrievalContractsTest.kt
 M tests/runtime/DefaultKnowledgeRetrievalTest.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_COMPLETION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
