# Memory Core Durability — Unit 5: Identifier Restoration — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the governing documents re-read fresh, and against the actual, current file contents — not against the Completion Review's own summary of them. This document does not amend `src/runtime/InMemoryMemoryCore.kt`, `src/runtime/MemoryCoreRecovery.kt`, either test file, the Completion Review, or any governance document.

---

## 1. Baseline Confirmation

`HEAD` is `10c51db672e9a6885d49a671833813e7f46fca26`, unchanged since this task began. `git diff --stat` confirms `src/runtime/InMemoryMemoryCore.kt` carries 111 insertions and **zero** deletions. `src/runtime/MemoryCoreRecovery.kt` carries 39 insertions and 1 deletion — checked directly: the one deleted line is a KDoc sentence extended into a longer one, not a behavioural change. No other file beyond the four reported (plus this Unit's own two review documents) is touched.

---

## 2. Re-Verification of the Completion Review's Two Self-Reported Corrections

1. **The "Section 5" → "Unit 5" citation fix.** Checked directly against the current text: the KDoc now reads "own Unit 5 section selects deriving each counter..." Checked against the Implementation Plan's own actual text: the "avoiding a second, redundant source of truth that could itself drift out of sync with the records it counts" sentence appears verbatim in the Plan's own "### Unit 5 — Identifier Restoration" section, not in "## 5. Design Resolution." **Correctly applied.**
2. **The added `DELETED`-record test.** Checked directly: `a DELETED record's own identifier still counts toward the restored maximum -- its identifier is never reused` exists in the current `InMemoryMemoryCoreTest.kt`, restores an entity, transitions it to `DELETED`, calls `restoreIdentifierCounters`, and asserts the next minted identifier is one past the deleted record's own — genuinely exercising the property named, not merely asserting it in a comment.

Both corrections are genuine and correctly applied.

---

## 3. Full, Independent Quotation Audit

| Quoted fragment | Cited source | Verified |
| --- | --- | --- |
| "Per-record-kind identifier counters must be restored to (highest persisted identifier of that kind) + 1 — never reset to 1..." (referenced, not directly quoted in the new code) | Contract Design §6 | Confirmed present at that section, exact wording, via direct re-read. |
| "A Memory Core durability implementation **SHALL** restore each per-kind identifier counter to (highest persisted identifier of that kind) + 1 on recovery, and **SHALL NOT** permit identifier reuse across a restart." | Scope Lock §18 | Exact match, confirmed by direct re-read of Scope Lock §18. |
| "avoiding a second, redundant source of truth that could itself drift out of sync with the records it counts" | Implementation Plan, Unit 5 | Verified in Section 2, above. |
| `"kind-N"` | Implementation Plan, Unit 5 (test-files bullet) | Exact match, including the source's own combined backtick-and-quote-mark formatting. |

**No defect found.** Every quotation in the new code is accurate and correctly attributed.

---

## 4. Challenge — Does Restoration Correctly Handle Every Named Case, Not Merely the Common One?

Tested independently against each case the governing task names, by tracing the code directly rather than trusting the test suite alone:

- **No records** — `nextSequenceFor` with an empty collection never enters its `forEach`, `maxSuffix` stays `0`, returns `0 + 1 = 1`. Matches `InMemoryMemoryCore`'s own existing, unmodified starting value exactly — no special case was needed or added.
- **Sparse identifiers** — the function has no gap-filling logic anywhere; it only ever tracks a running `maxSuffix`. Confirmed by test and by the absence of any code path that could infer or synthesise a missing intermediate value.
- **Out-of-order arrival** — `identifierValues.forEach` visits every element regardless of collection iteration order (a `Set` from `Map.keys`, order not guaranteed or relied upon); `if (suffix > maxSuffix) maxSuffix = suffix` is order-independent by construction — the true maximum is found regardless of which element is visited first.
- **Duplicate identical entries** — `nextSequenceFor` reads `Store.keys`, a `Set`; a key can appear at most once by definition. Since `restoreEntity` and its four counterparts already collapse an identical duplicate to zero net change on the store (Unit 4's own already-reviewed idempotence), the key set duplicate replay produces is indistinguishable from the key set a single restoration would have produced. No separate deduplication logic was needed in this Unit, and none was added.
- **Malformed identifiers** — two independent `require` checks (prefix match with a non-empty remainder; a parseable, positive `Long` suffix), both `IllegalArgumentException`, both propagated by `MemoryCoreRecovery` as `IdentifierCounterRestorationFailed` rather than swallowed.
- **Identifier values approaching numeric limits** — `Math.addExact(maxSuffix, 1L)` is JDK-standard overflow-checked addition, catching exactly the one arithmetic operation in this function capable of silent wraparound; confirmed by a dedicated test using `Long.MAX_VALUE` directly, not an approximation.

**Sound**, all six cases traced to specific, working code, not merely to test assertions.

---

## 5. Challenge — Is the Placement Decision (`InMemoryMemoryCore`, Not `MemoryCoreRecovery` or a Dedicated Helper) Actually Correct?

Re-derived independently, not accepted from the Completion Review's own reasoning: `MemoryCoreRecovery` has no access to any store's own key set except through `MemoryRetrieval`'s own public, paginated query methods (`findEntities`, and so on), each requiring a `maximumResults` bound and none guaranteed to return literally every stored identifier in one call regardless of volume — using them to enumerate "every identifier of this kind" would be fragile, indirect, and a misuse of a retrieval mode never designed for exhaustive enumeration. A dedicated external helper would need the same access and would gain nothing by living outside `InMemoryMemoryCore` except an extra layer of indirection around private state it still could not reach without a new accessor of some kind. Placing both the read (`*Store.keys`) and the write (`next*Sequence = ...`) inside the one class that owns both, under its own existing `mutex`, is the only placement that requires introducing zero new coupling. **Sound.**

---

## 6. Challenge — Does This Unit Correctly Refuse to Advance Counters on a Failed Recovery, Including the Specific Failure Mode This Unit Itself Introduces?

Tested two distinct failure directions, not merely the general case already covered by Unit 4: (a) a failure during entry replay itself (already covered, Unit 4's own reviewed behaviour, `restoreIdentifierCounters` never reached at all); (b) a failure *within counter restoration itself*, after every entry has already replayed successfully — checked directly via `a malformed identifier discovered only during counter restoration fails the entire recovery`, which constructs a scenario where every `restore*` call individually succeeds (a malformed-format identifier is still merely non-blank, so passes `Entity`'s own construction-time check and `restoreEntity`'s own referential-integrity check) and only `restoreIdentifierCounters` itself detects the problem. Confirmed this still fails the *entire* `recover()` call, not merely the counter-restoration step in isolation — `recover()`'s own two-branch design (replay loop, then counter restoration, both inside the same function, no early return between them that a caller could observe) makes a "records restored but counters silently wrong" outcome structurally unreachable. **Sound** — this is the more interesting and more thoroughly tested failure mode, not merely the one Unit 4 had already covered.

---

## 7. Challenge — Was Any Later Unit's Work (Concurrency, Runtime Composition) Leaked Into This One?

Checked against the task's own explicit exclusion list: no durable write-through decorator, runtime composition, `ParkerRuntime` wiring, Docker change, new identifier format, new public API, new persistence mechanism, or counter-persisted-as-a-durable-record exists anywhere in the diff — confirmed by direct reading of both changed production files and by `git status` showing no file under `src/composition/`, `Dockerfile`, or `docker-compose.yml`. `DurableMemoryCoreEntry.kt` (Unit 1) is untouched — no new field or entry kind was added to carry a counter value durably, consistent with the derivation-not-persistence mechanism this Unit's own governing authority already selected. No new concurrency primitive was introduced — `restoreIdentifierCounters` uses the class's own pre-existing `mutex`, not a new lock, new dispatcher, or new coordination mechanism Unit 7 would otherwise have had to introduce first. **Sound.**

---

## 8. Challenge — Do the Tests Verify Governed Behaviour, or Merely Freeze Today's Implementation Convenience?

Reviewed the 17-test addition specifically for over-specification. Every test asserts an outcome traceable to a named governing requirement (identifier reuse, independence, sparse/out-of-order correctness, malformed-input rejection, overflow rejection, `DELETED`-record retention, no-advance-on-failure) or to this task's own explicit required-test list. None asserts an incidental implementation detail (an internal field's exact numeric value independent of behaviour, a specific exception message string, or timing). The `Long.MAX_VALUE` overflow test in particular is worth noting as testing genuine constitutional behaviour (Contract Design's own "never fabricated, defaulted, or inferred" principle, applied to arithmetic overflow specifically) rather than a convenience — a implementation that let `+1` wrap silently would still "work" in every other test, making this the one test that exists specifically to catch a defect no other test could. **Sound.**

---

## Findings

No required correction was found. Both of the Completion Review's own self-reported corrections are independently re-verified as genuine (Section 2). A full, independent quotation audit found no further discrepancy (Section 3). Every named behavioural case, the placement decision, the failure-propagation path this Unit itself introduces, and the absence of later-unit leakage were each independently re-derived and confirmed sound, not merely accepted (Sections 4–7). Test quality was reviewed specifically for over-specification and found sound (Section 8).

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction.

---

## Recommended Next Step

No further correction is required for Unit 5. Per this task's own explicit instruction, no Defect Confirmation Review is necessary, since no required defect was found. Per this task's own explicit stop point, work halts here: Unit 6 is not begun; nothing is staged, committed, or pushed.

---

## Final Git Status at Time of This Review

```
$ git status --short
 M src/runtime/InMemoryMemoryCore.kt
 M src/runtime/MemoryCoreRecovery.kt
 M tests/runtime/InMemoryMemoryCoreTest.kt
 M tests/runtime/MemoryCoreRecoveryTest.kt
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_5_IDENTIFIER_RESTORATION_COMPLETION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_5_IDENTIFIER_RESTORATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
