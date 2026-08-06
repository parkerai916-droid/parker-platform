# Programme 3 — Unit 9.5: Permission Enforcement Implementation — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the adopted governance re-read fresh, and against the actual, current file contents — not against the Completion Review's own summary of them. This document does not amend `src/runtime/DefaultKnowledgeRetrieval.kt`, `tests/runtime/DefaultKnowledgeRetrievalTest.kt`, the Clarification, the Unit 9 Contract Design, the Unit 9 Scope Lock Clarification, or any other governance document. It identifies conflict, or its absence, and states a determination.

---

## 1. Baseline Confirmation

`HEAD` is `8b929c223ba11af5df66da061373239002b12269`, unchanged since implementation began. The working tree carries exactly the expected set: two modified files (`src/runtime/DefaultKnowledgeRetrieval.kt`, `tests/runtime/DefaultKnowledgeRetrievalTest.kt`) and four untracked governance/review files (the adopted Clarification, its two reviews, and this Unit's own Completion Review). No other file is touched.

---

## 2. Scope and Method

This review reads the adopted Clarification (`docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md`) in full, fresh, and checks the current `src/runtime/DefaultKnowledgeRetrieval.kt` against every one of its own frozen requirements directly — by reading the actual code, not the Completion Review's account of it. It additionally diffs the current file against the pre-Unit-9.5 version (`git show HEAD:src/runtime/DefaultKnowledgeRetrieval.kt`) section-by-section, to confirm every KDoc section either survived unchanged, was deliberately and correctly superseded, or was deliberately and correctly extended — following the same discipline the immediately preceding Unit 9.4 review applied when it caught a citation defect by checking claims against source rather than accepting them. The test suite (`tests/runtime/DefaultKnowledgeRetrievalTest.kt`) is read in full and each of the required coverage categories is checked for a genuinely corresponding, correctly-asserting test.

---

## 3. Act-Level Gate — Location, Timing, and Denial Disposition

**Test:** does the act-level gate evaluate exactly once, before any persistence read, and correctly return `NotAuthorised`?

Checked directly against the code: `retrieve` calls `permissionEngine.evaluate` once, unconditionally, as its first action; `persistence.findAll()` appears only after the `if (!isAuthorised(actLevelDecision)) return NotAuthorised(...)` branch. No code path reaches `persistence.findAll()` without first passing this check. `isAuthorised` correctly treats only `APPROVED`/`APPROVED_WITH_CONFIRMATION` as authorising, matching Chapter 10 §4's four outcomes and the Clarification §8 step 3's fail-closed treatment of `DEFERRED` exactly. The `NotAuthorised` reason string names the requesting principal and the decision outcome, mirroring `DefaultKnowledgeSubmission`'s own accepted reason-string convention. **Sound.**

---

## 4. Item-Level Gate — Granularity, Order, and Silent Filtering

**Test:** does the item-level gate evaluate once per candidate surviving matching and lifecycle shaping, in order, before bounding, filtering silently rather than surfacing a distinguishable denial?

Checked directly: the `for (item in candidates)` loop iterates over the full, unbounded `candidates` list (matching + lifecycle shaping applied, `.take()` not yet applied), in the order `persistence.findAll()` returned it — the same order Unit 9.2's own ordering guarantee already fixes. Denied or deferred items are simply never added to `approved`; no sentinel, no exception, no distinguishable per-item outcome exists anywhere in this path. Bounding (`.take(query.maximumResults)`) is applied to `approved`, after the loop — exactly the order Clarification §8 step 6 fixes, for exactly the reason it discloses (a caller who receives fewer than `maximumResults` entries can trust this reflects genuinely fewer visible items). **Sound.**

---

## 5. Resource, Action, Intent, Correlation, and Principal

**Test:** does every `ExecutionRequest`, at both granularities, use the single fixed resource/action pair, the caller's own `correlationId`, and the caller's own principal — varying only by `intent`?

Checked directly: `buildExecutionRequest` is the sole construction path for both granularities; `targetResources`/`proposedActions` are always `listOf(KNOWLEDGE_RETRIEVAL_RESOURCE_ID)`/`listOf(RETRIEVE_ACTION_NAME)`, both `private`/`internal`-visible companion constants, never varied by call site. `correlationId` is always the caller-supplied parameter, traced to `query.correlationId` at both call sites in `retrieve` — never a freshly minted value. `principalId` is always `requestingPrincipalId`, the caller's own parameter — never a second identity source. Only `intent` varies: `ACT_LEVEL_INTENT` (a `const val`, truly fixed) for the act-level gate; `itemLevelIntent(item)` (embedding the item's own `KnowledgeId`) for the item-level gate. This is squarely within the Clarification's own authorised latitude: Section 7 fixes resource and action, says nothing restricting `intent`, and Chapter 10 §3 itself names "whatever circumstantial context bears on whether the action should be authorised" as a legitimate proposal component. Embedding the item's identity in `intent` is not merely permitted but *necessary* for the item-level gate to be capable of differentiating between items at all in a real policy — without it, every item-level `ExecutionRequest` for a given query would be byte-identical, and no policy could ever approve one item while denying another, defeating the entire reason the Clarification required a two-tier gate in the first place (Clarification §6.2's own per-item disclosure reasoning). **Sound**, and the design choice is more than merely permitted — it is required for the mechanism to function as governed at all.

---

## 6. Evaluation Count and No Double Evaluation

**Test:** does the implementation produce exactly the `1` / `1 + N` count the Clarification fixes, with no item ever evaluated twice?

Checked directly: on act-level denial, the function returns before any further `permissionEngine.evaluate` call — exactly `1`. On approval, the loop evaluates exactly once per element of `candidates`, a list with no duplicates (backed by `InMemoryKnowledgeItemPersistence`'s own `Map<KnowledgeId, KnowledgeItem>`, confirmed in `KnowledgeItemPersistence.kt`) — exactly `1 + N`, matching the Clarification §8's own disclosed "Verification consequence" precisely. Confirmed additionally by the new `exactly 1 + N evaluations...`, `exactly 1 evaluation...`, and `no item is evaluated more than once` tests, each independently verified to pass. **Sound.**

---

## 7. Unit Boundary

**Test:** does the implementation stay within Unit 9.5's own authorised scope — no runtime composition, no Reasoning Context integration, no new Memory Core dependency, no ranking?

Checked directly: `git diff --stat -- src/composition/ParkerRuntime.kt` produces no output. `git diff --name-only` lists exactly two files. No `MemoryRetrieval`/`MemoryCore` import exists anywhere in the current file (confirmed by direct reading of the full import list). `matches` is byte-identical to its pre-Unit-9.5 form — no scoring, weighting, or semantic comparison was introduced. **Sound.**

---

## 8. Regression Check — Matching, Lifecycle Shaping, Ordering, and Staleness Preserved

**Test:** are Unit 9.2's matching, Unit 9.4's lifecycle shaping, and Unit 9.3's staleness disclosure genuinely unchanged in substance, not merely claimed unchanged?

Checked by direct diff: `matches` and `isRetrievable` are character-for-character identical to the pre-Unit-9.5 version. `disclosureFor` is character-for-character identical; it is now called on a possibly-smaller set (post-permission-filtering), which is the *correct* consequence of Clarification §8 step 8 ("Compute staleness disclosure only for the final, bounded, permission-approved set"), not a defect. Every pre-existing Unit 9.2/9.3/9.4 behavioural test still exists, unmodified in its own assertions, now constructed with an always-approving fake engine so it continues to exercise exactly the behaviour it always did. **Sound.**

---

## 9. Test Coverage Against the Task's Own Required List

Checked one-by-one against the eleven categories the governing task named: authorised retrieval (present); act-level denial (present, plus `DEFERRED` and the reason-content test); mixed authorised/unauthorised items (present); complete filtering of all items (present, and correctly distinguished from `NotAuthorised`); deterministic ordering after filtering (present); retirement filtering combined with permission filtering (present, in both directions — excluded-before-reaching-the-gate, and included-but-still-gated); superseded history preservation (present); correlation identifier propagation (present, including under denial); resource/action correctness (present); boundary conditions and regression cases (present — empty persistence, bounding-after-filtering, structural dependency-count tests). **All eleven categories are genuinely, not merely nominally, covered** — each corresponding test was read and confirmed to assert something that would actually fail if the governed behaviour it names were broken, not a tautology.

---

## Findings

### Finding 1 (Required Correction) — a substantive, previously-accepted KDoc section silently deleted with no replacement

**The defect.** Comparing the current file's own section headings against the pre-Unit-9.5 version (`git show HEAD:src/runtime/DefaultKnowledgeRetrieval.kt`) shows the entire **"## Determinism, disclosed precisely"** section — present, substantive, and already independently reviewed and accepted as part of Unit 9.4 — no longer exists anywhere in the file, with no replacement section and no substitute content covering the same ground. That section's own content is not merely restated more briefly elsewhere: the current "Ordering" section gained one added clause ("...and an unchanged permission policy therefore returns an identical result...") but this does not carry the deleted section's own reasoning — the explicit citation of `isRetrievable` as "a pure function of `KnowledgeItem.status` and `KnowledgeRetrievalQuery.includeRetired` alone," the Scope Lock §8 concurrent-revision-ordering analogy, and the fully-explained "one disclosed exception" treatment of `disclosureFor`'s own wall-clock dependency (with its own citation to `DefaultKnowledgeCandidateEvaluator`'s identical precedent) are all gone, not summarised.

This repository's own established discipline throughout this entire Programme — visible in every KDoc section this file itself still contains — is that a determinism guarantee, once disclosed and accepted, is not silently narrowed or removed without a stated reason; removing it without any replacement or explanation is itself a form of the "silent implementation-time drift" this Programme's own governing discipline has repeatedly, explicitly forbidden elsewhere (for example, Unit 9.1's own KDoc: widening or narrowing an already-disclosed guarantee "requires its own explicit, disclosed... amendment when it happens, never a silent... drift introduced without one"). Unit 9.5 also introduces a genuinely new determinism-relevant fact this section never addressed even implicitly: the class now depends on `permissionEngine.evaluate` returning stable decisions across repeated calls with identical `ExecutionRequest` content for its own overall determinism claim to hold at all — a dependency worth disclosing exactly as candidly as the pre-existing staleness/wall-clock exception was.

**A second, smaller instance of the same defect.** The paragraph beginning "**[KnowledgeItemStatus] alone is sufficient to represent every retrieval-shaping decision this Unit makes**" — also previously accepted, substantive Unit 9.4 content explaining why no additional lifecycle classification was needed — is also gone with no replacement.

**Why this is a genuine defect, not a style preference.** Both paragraphs were substantive, previously-reviewed, constitutionally load-bearing disclosures (one about a binding non-functional requirement — determinism — the Contract Design and Scope Lock both fix as an explicit, tested guarantee; the other about why no new lifecycle state was invented, directly relevant to the Implementation Plan's own cross-cutting "no new lifecycle state or event kind" boundary). Their disappearance was not disclosed, explained, or even mentioned anywhere in the Completion Review, which claims elsewhere that "matching, lifecycle shaping, and staleness disclosure are byte-identical to the pre-Unit-9.5 implementation" — true of the *code*, but the claim of continuity does not extend to the *documentation* the way a reader would reasonably expect, since two of the very sections a reader would consult to verify that continuity are themselves missing.

**Required correction:** restore the "Determinism, disclosed precisely" section, updated to also disclose the new permission-evaluation dependency described above, and restore the "`KnowledgeItemStatus` alone is sufficient" paragraph, both in their appropriate positions in the file's own KDoc.

No other required correction was found. Every behavioural, structural, and test-coverage claim checked in Sections 3–9 above was independently verified sound.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

One required correction — a documentation-completeness defect (silent deletion of previously-accepted, substantive KDoc content, with no replacement or disclosure), not a behavioural, architectural, or test-coverage one. The act-level gate, the item-level gate, resource/action/intent/correlation/principal handling, evaluation count, the Unit boundary, and the full regression/coverage picture are all confirmed sound and require no change.

---

## Recommended Next Step

Restore the two deleted KDoc passages (updated for Unit 9.5's own new determinism-relevant fact), touching no Kotlin logic and no test. A narrow Defect Confirmation Review follows, confirming the restoration is complete and accurate and that no regression was introduced, without repeating this full review.

---

## Final Git Status at Time of This Review

```
$ git status --short
 M src/runtime/DefaultKnowledgeRetrieval.kt
 M tests/runtime/DefaultKnowledgeRetrievalTest.kt
?? docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md
?? docs/reviews/PROGRAMME_3_UNIT_9_5_PERMISSION_ENFORCEMENT_IMPLEMENTATION_COMPLETION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_5_PERMISSION_ENFORCEMENT_IMPLEMENTATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
