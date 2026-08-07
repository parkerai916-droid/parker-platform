# Memory Core Durability — Unit 7: Concurrency and Ordering — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the governing documents re-read fresh, and against the actual, current file contents — not against the Completion Review's own summary of them. This document does not amend `src/runtime/DurableMemoryCore.kt`, `tests/runtime/DurableMemoryCoreTest.kt`, the Completion Review, or any governance document.

---

## 1. Baseline and Reconciliation Re-Verification

Independently re-confirmed, not accepted from the Completion Review's own account: `git status --short` at review time shows `src/runtime/InMemoryMemoryCore.kt` modified (Unit 6's own already-reviewed, unchanged-since content), `DurableMemoryCore.kt` and its test untracked, `HEAD` at `9351b05...` (the Unit 5 commit) — the task's own "clean main, Units 1–6 committed" baseline claim is indeed inaccurate, exactly as the Completion Review discloses. Independently re-read the Implementation Plan's own Dependency Graph (§7): row 8 reads `"1–7"` under Prerequisites for Runtime Composition, in the actual current document text, not merely as recalled. **The determination that Unit 7, not Unit 8, was the next lawful step is independently confirmed correct** — this is the single most consequential finding this task turned on, and it withstands independent re-derivation.

---

## 2. Challenge — Does `writeMutex` Actually Cover Every Write Operation, or Was One Overlooked?

Checked directly, method by method, against the current file: `createProvenance`, `createEntity`, `registerDocument`, `createAssertion`, `createRelationship`, and `transitionStatus` — all six now read `writeMutex.withLock { ... }` as their entire body, with no statement outside the lock in any of them. No seventh write path exists on `MemoryCore` to have been missed. **Sound.**

---

## 3. Challenge — Is the Interleaving-Order Test Actually Meaningful, or Would It Have Passed Even Without the New Guard?

This is the question worth pressing hardest, since a test that cannot fail proves nothing. Traced through what would happen *without* `writeMutex`, by direct reasoning against `InMemoryMemoryCore.prepareEntity`'s own scope: absent the outer guard, each `createEntity` call's own `prepareEntity` step acquires and releases `InMemoryMemoryCore`'s own internal `Mutex` independently of the subsequent `append` — meaning two concurrent calls could each mint (acquire/release the inner lock) in quick succession, then race on their own, differently-delayed `append` calls. Under the test's own deliberately reversed delay (a later-minted identifier's append delayed *less*), an unsynchronised implementation would very likely have produced a non-monotonic observed order (e.g., a higher-numbered identifier's entry appearing before a lower-numbered one's). **The test is genuinely capable of failing against a plausible unsynchronised implementation, not merely against a contrived one — it is not a tautology.**

---

## 4. Challenge — Is the "Mixed-Kind Concurrency" Test as Strong as the Completion Review's Own Framing Suggests?

Independently assessed, and found weaker than the Completion Review's own listing might imply, though not incorrect: the mixed-kind test asserts only that both a concurrent transition and a concurrent creation *durably commit* (append count grows by two; the final status is correct) — it does not assert *which* of the two entries appears first in the log, so it would very likely pass even without `writeMutex`, since `InMemoryMemoryCore`'s own internal `Mutex` and `MemoryCoreDurabilityLog`'s own internal guard already independently guarantee each individual operation completes atomically on its own terms. This is a legitimate, useful *regression/liveness* test (proving the new lock does not cause a lost write or a deadlock across different operation kinds) but is not, by itself, ordering-discriminating evidence the way the first new test is. **Not a defect** — the Completion Review's own "Tests Added" section describes it accurately enough ("confirming both durably commit with none lost," not claiming it proves ordering) — but this review notes the distinction explicitly, since a future reader skimming only the test's own name could otherwise overstate what it demonstrates.

---

## 5. Challenge — Does `writeMutex` Introduce Any Deadlock Risk With `InMemoryMemoryCore`'s Own Internal `Mutex`?

Traced the lock-acquisition order across every code path in the current file: every write operation acquires `writeMutex` first, then (via `prepare*`/`restore*`/`transitionStatus`/`getX` calls inside `currentStatusOf`) `InMemoryMemoryCore`'s own internal `Mutex` second — always in this order, never reversed, anywhere in this file. Read operations acquire only `InMemoryMemoryCore`'s own internal `Mutex` directly, never `writeMutex`. Since no code path ever holds `InMemoryMemoryCore`'s own lock while attempting to acquire `writeMutex`, no circular-wait condition exists. **Sound — no deadlock risk.**

---

## 6. Challenge — Does `writeMutex.withLock` Correctly Release the Lock on a Thrown Exception (Referential-Integrity Failure, Append Failure, Invalid Transition)?

Confirmed by direct reasoning about `kotlinx.coroutines.sync.Mutex.withLock`'s own documented contract (a `try`/`finally`-equivalent implementation, releasing the lock on any exit path including an exception) and, independently, by the fact that Unit 6's own pre-existing tests for referential-integrity failure, append failure, and invalid-transition rejection all continue to pass unchanged after `writeMutex` was added around their own code paths (confirmed in the full regression run, 1898 tests, zero failures) — a leaked lock would have caused every *subsequent* test in the same suite to hang or fail, which did not happen. **Sound.**

---

## 7. Challenge — Was Anything Beyond Unit 7's Own Scope Implemented?

Checked directly: no reference to `ParkerRuntime`, `ParkerRuntimeConfig`, `Docker`, or any Unit 8/9/10-owned file exists anywhere in the diff; `git status` confirms no such file touched. `InMemoryMemoryCore`'s own internal `Mutex` is unmodified (confirmed by `git diff --stat` showing zero further change to `InMemoryMemoryCore.kt` beyond Unit 6's own already-reviewed content). No cross-process concurrency mechanism was introduced. **Sound.**

---

## 8. Full, Independent Quotation Audit

| Quoted fragment | Cited source | Verified |
| --- | --- | --- |
| "a `DurableMemoryCore`-level `Mutex` guarding the append-then-delegate sequence as a whole... so that two concurrent callers can never observe one call's durable commit interleaved with a different call's in-memory update" | Implementation Plan, Unit 7 | Ellipsis-bridged fragments are exact, contiguous substrings of one continuous source sentence, in original order, with the omitted middle material (a "distinct from... internal guard" clause) genuinely non-load-bearing to the claim retained. Legitimate selective quotation, consistent with this session's own established standard for this pattern. |
| "acceptable" | Contract Design §11 | A single word, correctly used as this document's own established term for the one disclosed failure window, not a fabricated multi-word claim. |
| "distinct from what a configured `appendBehavior` may have done instead" | `FakeMemoryCoreDurabilityLog.kt`'s own KDoc | Exact match, confirmed by direct re-read, correctly cited as the root cause of the self-corrected test defect. |

**No defect found.**

---

## Findings

No required correction was found. The central reconciliation finding (Unit 7, not Unit 8, is the next lawful step) is independently re-derived and confirmed. Lock coverage, deadlock safety, and exception-safety are each independently verified. One test (the mixed-kind concurrency test) is confirmed weaker than a casual reading might suggest, but is accurately described by the Completion Review's own text and is not itself a defect — noted for clarity, not required to be corrected.

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction.

---

## Recommended Next Step

No further correction is required for Unit 7. Per this task's own explicit instruction, no Defect Confirmation Review is necessary, since no required defect was found. Per this task's own explicit stop point, work halts here: Unit 8 (Runtime Composition) is not begun; nothing is staged, committed, or pushed.

---

## Final Git Status at Time of This Review

```
$ git status --short
 M src/runtime/InMemoryMemoryCore.kt
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_6_DECORATOR_COMPLETION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_6_DECORATOR_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_7_CONCURRENCY_ORDERING_COMPLETION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_7_CONCURRENCY_ORDERING_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? src/runtime/DurableMemoryCore.kt
?? tests/runtime/DurableMemoryCoreTest.kt
```

Nothing staged, committed, or pushed.
