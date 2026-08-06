# Programme 3 Unit 9.2 — Deterministic Retrieval Engine — Independent Constitutional Review

## Status

**Independent constitutional review only.** No production code or test was modified during this review. Unit 9.3 or any later Unit 9 work was not begun. Nothing is staged, committed, or pushed. This review does not rely on `docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_COMPLETION_REVIEW.md`'s own account — every claim below was independently re-verified against the actual code, the actual test file, and primary governance text, read fresh.

---

## Repository Baseline

- **HEAD:** `d9725f4bbfd7f2304dee90b4cfcdf4db9e053c11` (`d9725f4`)
- **Branch:** `main`
- **Working tree, confirmed before this review began:**
  ```
   M src/runtime/KnowledgeItemPersistence.kt
   M tests/runtime/DefaultKnowledgeSubmissionTest.kt
   M tests/runtime/KnowledgeItemPersistenceTest.kt
  ?? docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_COMPLETION_REVIEW.md
  ?? src/runtime/DefaultKnowledgeRetrieval.kt
  ?? tests/runtime/DefaultKnowledgeRetrievalTest.kt
  ```
  Exactly as expected. No discrepancy.
- **Staged changes:** none.

---

## Authorities Reviewed

Read fresh for this review: `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` (§3, §4 Unit 9.1/9.2/9.4/9.6 entries, §5 ordering); `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (Adopted — §2, §3, §5, §6, §7, §8, §10, §11); `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (Adopted); `src/runtime/DefaultKnowledgeRetrieval.kt` (full, current); `src/runtime/KnowledgeItemPersistence.kt` (full, current); `src/interfaces/KnowledgeStore.kt`'s own Unit 9.1 addition (current, post-acceptance); `tests/runtime/DefaultKnowledgeRetrievalTest.kt` (full); `docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_COMPLETION_REVIEW.md` (full, treated as a claim to test, not a source of truth).

---

## 1. Is `KnowledgeItemPersistence.findAll()` the Minimum Constitutional Extension, or Does It Leak Responsibility?

**Confirmed: minimal, and correctly layered — not a leak.**

Contract Design §10 (Runtime Responsibilities) fixes explicitly: "**Retrieval owns:** query execution against Knowledge Memory's own held state; structural matching and deterministic ordering." This assigns *matching* to the retrieval engine itself, not to the persistence seam. `findAll()` returns the raw, unfiltered stored set and performs no matching, filtering, or ordering computation of its own (`items.values.toList()`, nothing more) — the alternative design this review considered and rejected (pushing a matcher function *into* `KnowledgeItemPersistence`, e.g. `find(predicate: (KnowledgeItem) -> Boolean)`) would have been the actual leak, relocating "structural matching" into the persistence layer Contract Design §10 assigns elsewhere. `findAll()` as built keeps responsibility correctly separated.

Neither the adopted Contract Design nor the adopted Clarification names or constrains `KnowledgeItemPersistence` anywhere — it is a Unit-8-era internal seam, invisible to Unit 9's own governance chain, confirmed by direct search of both documents. The Unit 8 Clarification §10 (persistence boundary) discusses only the write-side boundary; nothing in it addresses or forecloses read/enumeration capability. No governance document is reopened, contradicted, or bypassed by this addition.

Return type (`List<KnowledgeItem>`) matches the existing precedent every `MemoryRetrieval` query method already uses; no new collection abstraction is introduced. No defect found.

---

## 2. Is `stale = true` an Authorised Conservative Placeholder, or Does It Prejudge Unit 9.3?

**Authorised in substance; the reasoning is sound but incompletely disclosed.**

The choice does not constrain Unit 9.3's own future design space — `stale` remains `Boolean` (Unit 9.1's own frozen type), and nothing about a hardcoded runtime value prevents a future, genuine detection mechanism from computing either `true` or `false` per item once it exists. The reasoning for `true` over `false` (asserting freshness without evidence would be the fabrication the Contract Design's own governing principle forbids; disclosing potential staleness is the fail-closed direction) is sound and consistent with this repository's own established uncertainty discipline. Throwing instead of returning a placeholder was correctly rejected: Unit 9.4 depends on Unit 9.2 and needs a working `retrieve()` to test its own default-inclusion logic against — a non-functional Unit 9.2 would block it.

**One disclosure gap.** The KDoc and Completion Review both justify the placeholder's *honesty* but do not state the placeholder's *practical safety*: every result from this class is unconditionally flagged stale, which would be a materially misleading signal to any real caller — but the Implementation Plan's own ordering (Unit 9.6, Runtime Composition, is strictly last, gated on Units 9.1–9.5 all being complete) means this class is never reachable by a real caller before Unit 9.3 replaces the placeholder. This safety argument is not stated anywhere; a reader auditing this class in isolation has no way to know the placeholder is inert by construction rather than merely "intended to be temporary."

---

## 3. Is Deterministic Ordering Fully Governed and Reproducible?

**Confirmed, rigorously — not merely empirically true in this test run.**

Three guarantees compose the ordering claim, each independently verified as a documented Kotlin stdlib property, not incidental behaviour: `mutableMapOf()` is documented to return a `LinkedHashMap` specifically (insertion-ordered iteration, not an unspecified `Map`); `List.filter` is documented to preserve the input's own relative order; `List.take` is documented to return a prefix in that same order. `findAll()`'s own snapshot is taken under the same `Mutex` that guards `store`, so a concurrent write cannot produce a torn or partially-visible read. The determinism test (three repeated calls, same query, unchanged state, byte-identical results) exercises this correctly under `runTest`'s own sequential dispatcher. No defect found.

---

## 4. Has Ranking, Semantic Retrieval, Permission Evaluation, Lifecycle Shaping, Runtime Composition, or Reasoning Context Work Begun?

Ranking and semantic retrieval: no — matching is a single boolean substring test, never scored or weighted; confirmed both by direct source inspection and a dedicated structural test. Permission evaluation: no — `requestingPrincipalId` is accepted but never read inside `retrieve` or `matches`; confirmed directly and by a dedicated test showing two different principals receive identical results. Runtime composition: no — `ParkerRuntime.kt` is untouched, confirmed by `git status`. Reasoning Context: no — no reference exists anywhere in the diff.

**Lifecycle shaping: a genuine, substantive finding — not fully clean.** `matches()` and `retrieve()` never inspect `KnowledgeItem.status` at all. This means a `RETIRED` item, if its basis text matches, is returned on identical footing to an `ACTIVE` item — every currently stored item is included regardless of lifecycle status. The Unit 9 Contract Design §6 explicitly reserves this exact question — "whether a retired item is included in an ordinary Knowledge Query's own result set by default... is not fixed by this document" — to a later retrieval-shape decision (the Implementation Plan's own Unit 9.4). By including every item unconditionally, Unit 9.2 has not merely "not yet decided" this question — it has produced an *observable default* (include unconditionally) indistinguishable from a decision, without Unit 9.4 having made it. This is not the same defect as deliberately implementing retirement-aware filtering (which would clearly overreach into Unit 9.4's own territory) — it is the quieter, easier-to-miss version of the same overreach: a default arising from omission rather than design, but a default nonetheless. Neither the code's own KDoc nor the Completion Review discloses that this current behaviour is provisional and must not be relied upon as Unit 9.4's own eventual answer.

---

## 5. Do the New Tests Verify Governance, or Manufacture It?

Most of the suite verifies genuine, governance-traceable properties: the structural tests (constructor shape, absent Memory-Core/PermissionEngine-shaped dependencies, single public operation, absent ranking-shaped declarations) each trace to a specific Contract Design clause and use compiled-declaration reflection, not naive source-text search — mirroring `KnowledgeSubmissionScopeTest.kt`'s own already-accepted technique. The matching, bounding, ordering, and determinism tests each verify a specific, Contract-Design-traceable guarantee with a real assertion, not a tautology.

**One test manufactures rather than verifies governance, precisely and narrowly.** "every returned entry discloses stale as true" pins down a *disclosed implementation choice* (Finding 2, above), not a governance *mandate* — no adopted document requires `true` specifically; only that some value is always present. This is defensible as a regression guard against an accidental future change to the placeholder, but it should not be read, and is not currently framed, as verifying a constitutional requirement — only Unit 9.1's own "always present, never optional" requirement is actually mandated; the specific value `true` is this Unit's own choice, not governance's.

**One coverage gap, directly tied to Finding 4.** No test constructs a `RETIRED` item and observes what `retrieve()` currently does with it. Given that behaviour is a real, if unintentional, default this review has just identified, its absence from the test suite means the current behaviour is neither verified as intentional nor guarded against silent, accidental change in either direction.

---

## Findings

| # | Severity | Finding |
| --- | --- | --- |
| 1 | Moderate | Retired items are returned unconditionally, on equal footing with active items — a de facto "include by default" outcome for a question Contract Design §6 explicitly reserves to Unit 9.4, arising from omission rather than a considered decision, and not disclosed as provisional. |
| 2 | Moderate | The `stale = true` placeholder's practical safety (never reachable by a real caller before Unit 9.3, because Unit 9.6 is strictly last in the Implementation Plan's own ordering) is not stated anywhere the code or Completion Review discloses its reasoning. |
| 3 | Minor | The "stale as true" test verifies a disclosed implementation choice, not a governance mandate, and should not be characterised as testing a constitutional requirement. |
| 4 | Minor | No test exercises retrieval behaviour against a `RETIRED` item, leaving Finding 1's own behaviour both undisclosed and unguarded against accidental change. |
| 5 | None (confirmed sound) | `findAll()`'s minimality and correct layering, deterministic-ordering rigour, and the absence of ranking, semantic retrieval, permission evaluation, runtime composition, and Reasoning Context work are each independently verified without qualification. |

---

## Required Corrections

Three corrections are required before full acceptance, all additive (documentation and one test) — no correction to `retrieve`'s or `matches`' own matching, ordering, or bounding logic is required:

1. **Add a KDoc disclosure to `DefaultKnowledgeRetrieval`** stating explicitly that this class currently applies no lifecycle-status filtering — retired and active items are both returned unconditionally — that this is the absence of a decision, not a considered default, and that Unit 9.4 owns the actual default-inclusion policy this behaviour must not be mistaken for.
2. **Add a test constructing a `RETIRED` `KnowledgeItem`** and asserting the current (disclosed, provisional) behaviour — that it is returned when its basis matches, exactly as an active item would be — so the behaviour Finding 1 identifies is visible and guarded against silent, accidental change in either direction before Unit 9.4 makes the real decision.
3. **Add a sentence to the `stale = true` KDoc section** stating explicitly that this placeholder is never reachable by a real caller before Unit 9.3 replaces it, because Unit 9.6 (Runtime Composition) is strictly last in the Implementation Plan's own ordering, gated on Units 9.1–9.5 all being complete.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

The engine's own substantive correctness — minimal and correctly layered persistence extension, rigorously reproducible determinism, and clean absence of ranking, semantic retrieval, permission evaluation, runtime composition, and Reasoning Context work — was independently verified and found sound. What blocks full acceptance is narrower: one genuine, unintentional retrieval-shape default (unconditional inclusion of retired items) that was produced by omission rather than decision and is not disclosed as provisional, together with one under-disclosed safety argument for the staleness placeholder. Both are additive documentation and test corrections, not a defect in the engine's own architecture or a re-litigation of any decision this review confirmed sound.

---

## Recommended Next Step

Apply the three required corrections directly, then request a narrow defect-confirmation review — not a full re-review — verifying only that the corrections were made and that nothing else changed, mirroring this repository's own established "narrow correction pass, then defect-confirmation review" pattern already used repeatedly this session. Only after that confirmation should Unit 9.2 be treated as fully accepted, and only then should Unit 9.3 or Unit 9.4 begin.

---

## Git Confirmations

- No production code or test was modified during this review.
- Unit 9.3 and no later Unit 9 work was begun.
- Nothing was staged during this review.
- Nothing was committed during this review.
- Nothing was pushed during this review.

## Final Git Status

```
$ git status --short
 M src/runtime/KnowledgeItemPersistence.kt
 M tests/runtime/DefaultKnowledgeSubmissionTest.kt
 M tests/runtime/KnowledgeItemPersistenceTest.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_COMPLETION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_2_DETERMINISTIC_RETRIEVAL_ENGINE_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? src/runtime/DefaultKnowledgeRetrieval.kt
?? tests/runtime/DefaultKnowledgeRetrievalTest.kt
```
