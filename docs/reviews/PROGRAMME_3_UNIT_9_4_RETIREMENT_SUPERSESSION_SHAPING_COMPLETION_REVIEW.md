# Programme 3 — Unit 9.4: Retirement and Supersession Shaping — Completion Review

## Status

**Implementation completion review.** Unit 9.4 only is implemented, exactly as `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` §4's own Unit 9.4 entry specifies. Unit 9.5 (Permission Enforcement Wiring) and Unit 9.6 (Runtime Composition) are not begun. No governance document was modified. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD at start:** `a231947445f8347c55256ac8c6000466b0fd62bf`
- **Branch:** `main`
- **Working tree at start:** clean. Units 9.1, 9.2, and 9.3 accepted, committed, and pushed.

---

## Governance Read Before Implementation

Read fresh, in full where relevant: `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md`; `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (§1–§12, Context, Final Recommendation); `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (full); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` (§3, §6 exactly); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` (§5, §6, §7). Source read in full: `src/interfaces/KnowledgeStore.kt` (`KnowledgeItem`, `KnowledgeItemStatus`, the three `KnowledgeLifecycleEvent` variants, `KnowledgeRetrievalQuery`, `KnowledgeResultEntry`); `src/runtime/DefaultKnowledgeRetrieval.kt` (post-Unit-9.3-acceptance); `src/runtime/KnowledgeItemPersistence.kt`; `tests/runtime/DefaultKnowledgeRetrievalTest.kt`; `tests/contracts/KnowledgeRetrievalContractsTest.kt`.

---

## Required Planning Determination

Stated explicitly, from the governing documents, before any code was changed:

1. **Whether retired items are included by default, excluded by default, included only by explicit request, or governed by another rule.** **Excluded by default; included only when the caller sets `KnowledgeRetrievalQuery.includeRetired = true`.** Unit 9 Contract Design §6 names three lawful outcomes and reserves the choice to this Unit. Two governing texts, in tension, are both satisfied only by this combination: Contract Design §1 defines an ordinary query as a request for "relevant, **already-promoted** knowledge" — naturally current knowledge, since a retired item is, by Contract Design V2 §3's own definition, "no longer current"; but Contract Design V2 §3's own "retirement never implies deletion... remain retained and retrievable" guarantee forecloses a *permanent*, non-overridable exclusion, since Knowledge Retrieval is "the sole public path through which anything outside Knowledge Memory may observe promoted knowledge" (Contract Design §1) — a retired item unreachable through it, ever, would be retrievable in name only. No other existing rule resolves this question; it is squarely this Unit's own to decide.
2. **Whether superseded items remain retrievable.** **Yes, unconditionally, with no filtering decision required at all.** Contract Design V2 §3 fixes that supersession "is re-evaluated against the superseding evidence, exactly as an ordinary revision" — it never forks a `KnowledgeItem` into a separate current/superseded pair. A single item's own `history` already holds every classification, including every hop of an arbitrarily long chain, and `DefaultKnowledgeRetrieval` already forwards each matched item's `history` unchanged. Nothing about supersession is status-gated, so nothing about it needed new filtering logic.
3. **How current and historical items in a multi-hop chain are distinguished.** By position in the same item's own `history`, exactly as Contract Design §6's "Superseded" paragraph fixes: "the item's current classification is the most recent entry in its own single, non-forking history, and any earlier, superseded entry remains part of that same history." No separate field, marker, or item identity distinguishes them.
4. **Whether restoration returns an item to ordinary current-item visibility.** **Yes, automatically, with no special-case code.** Contract Design §6 fixes that "restoration... returns its status to active" and "a restored item is retrievable exactly as any other active item once restored." Filtering on `KnowledgeItem.status` alone (never on "history contains a retirement") gives this for free — a restored item's status is `ACTIVE`, so it is admitted exactly as any other active item.
5. **Whether retrieval selects a "latest" item automatically or discloses relationships and lets callers decide.** **Discloses relationships only; no "latest" selection exists.** Contract Design §6 states plainly that "nothing here selects a 'latest only' retrieval policy." `retrieve` forwards each matched item's full, unprojected `history`; no method computes, selects, or summarises a "latest" entry.
6. **Whether `KnowledgeItemStatus` alone is sufficient, or whether supersession needs an additional derived classification.** **`KnowledgeItemStatus` alone is sufficient.** The one genuine status-shaped retrieval-shaping decision is retired-item default inclusion, already fully expressed by the existing two-value status model. Supersession is not a status in the constitutional model this class is bound to (Contract Design V2 §3) — inventing a parallel classification for it would violate the Implementation Plan's own "no new lifecycle state or event kind" boundary (§3) for no disclosed benefit, since position-in-history already, honestly, distinguishes current from superseded.
7. **Whether the existing `KnowledgeRetrievalQuery`/`KnowledgeResultEntry` contracts are sufficient.** **`KnowledgeResultEntry` is sufficient, unchanged.** A retired item, once included, already discloses its own status honestly via the unchanged `KnowledgeItem.status` field every entry already carries; the current-versus-superseded distinction is already fully expressed by the unchanged `KnowledgeItem.history` every entry already carries. **`KnowledgeRetrievalQuery` is not sufficient** — it has no way to express "include retired items," and determination 1 requires exactly that capability. The required widening is identified and implemented below.

---

## Contract Widening

**`src/interfaces/KnowledgeStore.kt`, `KnowledgeRetrievalQuery` widened additively.** One new field: `includeRetired: Boolean = false`. This is the smallest necessary widening — a single boolean, defaulted so no existing three-argument construction site fails to compile, expressing exactly the one missing capability determination 7 identified. It is not a new type, not a new interface, and not a new outcome shape.

**Why this widening is authorised, not invented.** Unit 9 Contract Design §6 itself names "included only under an explicit caller criterion" as one of exactly three lawful outcomes for the retired-item default question — this document does not invent a fourth option; it implements the third one the adopted Contract Design already disclosed. Contract Design §4's own "nothing else" limit on `KnowledgeRetrievalQuery` (no ranking instruction, no semantic hint, no permission assertion) is respected: `includeRetired` is a structural criterion about which of an already-matched caller's own items to include, not a ranking, semantic, or permission-assertion field.

**Contract disclosure distinguished from filtering policy.** The widening only adds the *capability* for a caller to express a request; it fixes no policy of its own about when a caller should set it, and it grants no permission. `KnowledgeRetrievalQuery.includeRetired = true` is evaluated identically by whatever mechanism Unit 9.5 eventually wires, exactly as every other matched item is — Contract Design §6's own "lifecycle status is never a substitute for, or determinant of, a permission decision" applies to this field exactly as it already applies to `KnowledgeItem.status` itself.

**`KnowledgeResultEntry` was not widened.** No new field was needed, and none was added — see determination 7, above.

---

## What Was Implemented

**`src/interfaces/KnowledgeStore.kt`.** `KnowledgeRetrievalQuery` gains `includeRetired: Boolean = false`, with KDoc explaining the widening's constitutional basis, its default, and its structural (never permission) character.

**`src/runtime/DefaultKnowledgeRetrieval.kt`.** `retrieve` gains a second filter predicate, `isRetrievable(item, query)`, applied in the same `.filter` step as the existing `matches` predicate: `item.status == KnowledgeItemStatus.ACTIVE || query.includeRetired`. No other method changed. The "Lifecycle status -- currently unfiltered" KDoc section (Units 9.2/9.3's own disclosed absence-of-policy) is replaced by a new "Lifecycle shaping (Unit 9.4)" section stating the planning determination above, in full, in the file itself — including why filtering on `status` alone (never on history-contains-retirement) is correct for restoration, why supersession required no new logic, why no "latest only" selection exists, and why `includeRetired` is structural, not permission-shaped. The "Determinism" section is extended to state that `isRetrievable` is a pure function of `KnowledgeItem.status` and `KnowledgeRetrievalQuery.includeRetired`, applied by the same deterministic `.filter` step as matching, so lifecycle shaping carries the same determinism guarantee matching and ordering already had.

**`tests/contracts/KnowledgeRetrievalContractsTest.kt`.** Three new tests: `includeRetired` defaults to `false`; `includeRetired` is preserved when explicitly set `true`; `includeRetired` is a non-nullable `Boolean`. The existing "exactly three properties" structural test is updated to expect the new fourth property.

**`tests/runtime/DefaultKnowledgeRetrievalTest.kt`.** The old "a RETIRED item is currently returned identically to an ACTIVE one" test (which explicitly documented the pre-9.4 absence-of-policy and disclaimed establishing any policy) is replaced by a "Lifecycle shaping (Unit 9.4)" section of sixteen new tests, covering: active-item behaviour unchanged; retired-item default exclusion (single item, and mixed batch); explicit `includeRetired = true` inclusion (single item, mixed batch, and confirmation it grants no permission outcome); restored-item visibility (promoted → retired → restored, included by default, full history preserved); revised-item behaviour; superseded-item retrievability; current-versus-superseded distinguished by history position; a four-hop supersession chain remaining fully, transitively retrievable; confirmation no "latest only" projection occurs (the entire stored item is returned, byte-for-byte); deterministic ordering across a mixed-status batch with `includeRetired = true`, across repeated calls; bounding applied after lifecycle shaping; staleness disclosure preserved for an admitted retired item; and no mutation of the stored item across repeated retrieve calls. Two existing tests (`item()` helper, `query()` helper) were widened with new, defaulted parameters (`status`, `includeRetired`) so every pre-existing call site continues to compile and behave unchanged.

---

## Verification Performed

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 46s
5 actionable tasks: 5 executed
```

- **`DefaultKnowledgeRetrievalTest`:** 41/41 passed.
- **`KnowledgeRetrievalContractsTest`:** 22/22 passed.
- **Full repository suite:** 1727 tests, 0 failures, 0 errors, 5 pre-existing skips (up from 1709 at the Unit 9.3 accepted baseline; +18 net new tests, 0 regressions).

---

## Unit Boundary Confirmation

Confirmed directly, before and after implementation:

- **No `PermissionEngine` dependency** — the existing structural test (`no MemoryRetrieval, MemoryCore, or PermissionEngine dependency exists anywhere on DefaultKnowledgeRetrieval`) still passes unmodified; `includeRetired`'s own structural test confirms it is a plain `Boolean`, not a permission type.
- **No resource/action mapping, no denial filtering** — `isRetrievable` returns a `Boolean` computed from `KnowledgeItem.status` and `KnowledgeRetrievalQuery.includeRetired` alone; no `PermissionEngine.evaluate` call, no resource identifier, no action string exists anywhere in the diff.
- **No `ParkerRuntime.kt` modification** — confirmed via `git diff --stat -- src/composition/ParkerRuntime.kt`: no output, file untouched.
- **No Reasoning Context integration, no production composition, no provider selection** — confirmed by `git diff --name-only`: exactly four files changed, none under `src/composition/` or Reasoning Context's own package.
- **No caching or indexing** — `isRetrievable` and `matches` are both pure, uncached predicates evaluated fresh on every call; no new field stores a derived or memoised value.

---

## Files Modified

- `src/interfaces/KnowledgeStore.kt` (`KnowledgeRetrievalQuery` widened with `includeRetired: Boolean = false`)
- `src/runtime/DefaultKnowledgeRetrieval.kt` (`isRetrievable` added; lifecycle-shaping KDoc added; determinism KDoc extended)
- `tests/contracts/KnowledgeRetrievalContractsTest.kt` (three new tests; one structural test updated for the new field)
- `tests/runtime/DefaultKnowledgeRetrievalTest.kt` (one obsolete test replaced; sixteen new tests added; two helpers widened with defaulted parameters)

## Files Created

- `docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_COMPLETION_REVIEW.md` (this document)

## Test Results

- `DefaultKnowledgeRetrievalTest`: **41/41 passed**
- `KnowledgeRetrievalContractsTest`: **22/22 passed**
- Full repository suite: **1727 tests, 0 failures, 0 errors, 5 pre-existing skips**

## Constitutional Verdict

```
UNIT 9.4 -- COMPLETE.
```

## Recommendation

Unit 9.4 satisfies its own Completion Gate (Implementation Plan §7): it compiles, every existing test still passes, every new test named for it passes, and no later unit was required to reach this state. An Independent Constitutional Review follows this document. Unit 9.5 (Permission Enforcement Wiring) remains blocked behind its own governance precondition (the not-yet-drafted enforcement-mechanism Clarification), unaffected by this Unit. Unit 9.6 (Runtime Composition) remains strictly last, gated on Units 9.1 through 9.5 all being complete.

---

## Final Git Status

```
$ git status --short
 M src/interfaces/KnowledgeStore.kt
 M src/runtime/DefaultKnowledgeRetrieval.kt
 M tests/contracts/KnowledgeRetrievalContractsTest.kt
 M tests/runtime/DefaultKnowledgeRetrievalTest.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_COMPLETION_REVIEW.md
```

Nothing staged, committed, or pushed.
