# Memory Core Durability — Unit 6: Durable Memory Core Decorator — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the governing documents re-read fresh, and against the actual, current file contents — not against the Completion Review's own summary of them. This document does not amend `src/runtime/DurableMemoryCore.kt`, `src/runtime/InMemoryMemoryCore.kt`, `tests/runtime/DurableMemoryCoreTest.kt`, the Completion Review, or any governance document.

---

## 1. Baseline Confirmation

`HEAD` is `9351b05b84ceaad189eaf49b44c0fc5483b76fb9`, unchanged since this task began. `git diff --stat` confirms `src/runtime/InMemoryMemoryCore.kt` carries 139 insertions and **zero** deletions. Two new files exist (`DurableMemoryCore.kt`, `DurableMemoryCoreTest.kt`) plus this Unit's own two review documents. No other file is touched.

---

## 2. Re-Verification of the Completion Review's Boundary Review Resolution — the Most Important Question This Unit Raises

**Test:** is "durable-append-first" genuinely mandated by governance, or could "in-memory-first" have been an equally lawful reading the Completion Review dismissed too quickly?

Re-read Contract Design §11 fresh, independently: it enumerates exactly four failure moments — before durable commit; **"failure after durable commit but before the in-memory update completes"**; recovery failure; storage unavailability. The second is the *only* moment involving an ordering between durable commit and in-memory effect, and it is stated in one direction only. Checked further, independently: §11 also fixes an absolute, mechanism-independent promise — "a caller must never be told a write failed if it was durably committed, or told it succeeded if it was not" — which is *only* satisfiable without a rollback mechanism (explicitly forbidden by this task's own "No rollback" constraint) if nothing observable happens in memory before the durable commit succeeds. Under "in-memory-first," a caller could receive a thrown exception (a truthful "not told it succeeded") while a *different* caller, reading concurrently, could observe the phantom record before the failure is discovered — not a violation of the letter of the promise (which binds what the *original caller* is told) but a real, if narrow, breach of its evident spirit. Durable-append-first has no equivalent gap: nothing is observable in memory until *after* durability is confirmed. **The Completion Review's determination is independently confirmed correct, and reinforced by an argument (the "no rollback" constraint's own implicit dependence on nothing-observable-before-append-succeeds) the Completion Review itself does not make explicit.**

---

## 3. Challenge — Does the `prepare*`/`restore*` Split Genuinely Avoid Duplicating Logic, or Does It Quietly Reimplement `create*`?

Compared `prepareEntity` line-by-line against `createEntity`: identical order of operations (`requireExistingProvenance` first, mint second, construct third), identical field mapping from `CandidateEntity` to `Entity`, identical `Instant.now()`/`MemoryCoreRecordStatus.ACTIVE` defaulting — the *only* difference is the final line (`entityStore[entityId] = entity; entity` in `createEntity`, versus simply returning the constructed `Entity` in `prepareEntity`, with storage deferred to `restoreEntity`). This is not an independent reimplementation risking drift from `createEntity`'s own behaviour — it is the same construction logic, factored so the storage step can be deferred. The commit step reuses `restoreEntity` verbatim, unmodified since Unit 4. **No duplicate logic; the split is genuine, minimal, and the two write paths (`create*` directly, or `prepare*`+`restore*` via the decorator) cannot silently diverge in what a resulting record looks like.**

---

## 4. Challenge — Does the `transitionStatus` Pre-Validation Genuinely Reuse the Existing Check, or Does It Introduce a Second Rule Set?

Checked directly: `DurableMemoryCore.transitionStatus` calls `MemoryCoreLifecycleTransitions.requireValidTransition(priorStatus, targetStatus)` — the identical function, from the identical `internal object`, that `InMemoryMemoryCore.transitionStatus` itself calls internally. Not a copy, not a re-derived table, not a parallel `when` block enumerating the same transitions a second time. A future change to the transition table (should one ever be governed) would automatically apply to both the decorator's own pre-check and the real mutation, with no possibility of the two drifting apart. **Sound — "no duplicate lifecycle logic" is honoured literally, not merely in spirit.**

---

## 5. Challenge — Was the Disclosed Race Window in `transitionStatus` Actually Necessary to Accept, or Could It Have Been Closed Without Building Unit 7 Early?

Pressed this specifically, since accepting a known gap is only justified if closing it genuinely requires work this task excludes. The gap: between `DurableMemoryCore`'s own read-current-status step and its own subsequent append, a *different* concurrent caller could transition the same record, making the first caller's `priorStatus` stale by the time its append lands. Closing this within `InMemoryMemoryCore`'s own single-record `mutex` is not possible, because the gap spans *two separate operations* (a read on `InMemoryMemoryCore`, then an append on a *different* object, `durabilityLog`) with no shared lock covering both — exactly the "single class's own internal write-serialisation" (Unit 3's own `FileSystemMemoryCoreDurabilityLog` mutex) versus "a lock spanning two different classes' own operations as one atomic unit" (Unit 7's own explicitly-scoped "outer, cross-call serialisation guard") distinction the Implementation Plan itself already draws. Closing it *would* require exactly the mechanism Unit 7 owns. **The deferral is correctly justified, not a convenience.**

---

## 6. Challenge — Does Any Read Operation Actually Touch the Durability Log, Contrary to What the KDoc Claims?

Checked every one of the ten `MemoryRetrieval` overrides individually, by direct reading, not by trusting the KDoc's own claim: `getEntity`, `getDocument`, `getAssertion`, `getRelationship`, `findEntities`, `findDocuments`, `traverseRelationships`, `findByTimeRange`, `findByMetadata`, `findByProvenance` — every one is a single-line direct delegation to the corresponding `inMemory.*` call, with no other statement in the method body. `durabilityLog` is referenced nowhere in any of the ten. Independently confirmed by the dedicated test asserting the fake log's own `appended.size` is unchanged after a sequence of reads. **Sound.**

---

## 7. Challenge — Does the Decorator Genuinely Preserve `InMemoryMemoryCore`'s Own Existing Failure Messages, or Does It Introduce a Behavioural Difference a Caller Could Notice?

Tested specifically, since this task's own instruction requires "no altered `MemoryCore` semantics." Checked `describeReference`'s own output against `InMemoryMemoryCore.transitionStatus`'s own four inline `NoSuchElementException` messages, field by field: both produce `"Entity '<value>' does not exist"`, `"Document '<value>' does not exist"`, and so on, character-for-character identical. Checked the impossible-transition case: `DurableMemoryCore` calls `MemoryCoreLifecycleTransitions.requireValidTransition` directly, which throws the exact same `IllegalArgumentException` with the exact same message (`"Illegal Memory Core lifecycle transition: $from -> $to"`) `InMemoryMemoryCore.transitionStatus`'s own internal call to the same function would produce. **A caller cannot distinguish, from either the returned value or the thrown exception's own type and message, whether they are talking to `DurableMemoryCore` or `InMemoryMemoryCore` directly** — the strongest form of the "no altered semantics" requirement, verified rather than merely claimed.

---

## 8. Challenge — Is the "Burned Identifier" Design Actually Safe, or Does It Risk Identifier Exhaustion or Confusion?

Re-derived independently: identifiers are `Long`-backed, and Unit 5's own `restoreIdentifierCounters` already tolerates and correctly handles arbitrarily sparse sequences — a gap from a failed append is indistinguishable, to every downstream mechanism, from a gap that already existed for any other reason (a `DELETED` record, or any future cause). No mechanism anywhere in this Programme's own governed design attaches meaning to *contiguity* — only to *uniqueness* and *monotonic non-reuse*, both of which the burned-identifier design preserves exactly. Checked the specific test added during the Completion Review's own self-review (`an identifier already minted by a failed create is never reused...`) and confirmed it exercises exactly this property, not merely a proxy for it. **Sound.**

---

## 9. Challenge — Full, Independent Quotation Audit

| Quoted fragment | Cited source | Verified |
| --- | --- | --- |
| "failure after durable commit but before the in-memory update completes" | Contract Design §11 | Exact match, re-verified independently in Section 2, above. |
| "a caller must never be told a write failed if it was durably committed, or told it succeeded if it was not" | Contract Design §11 | Exact match, confirmed by direct re-read. |
| "one interface, one implementing class" | `InMemoryMemoryCore.kt`'s own KDoc, describing `InMemoryKnowledgeStore` | Exact match, correctly attributed after the Completion Review's own disclosed self-correction. |

No further quoted fragment appears in either new production file. **No defect found.**

---

## 10. Challenge — Did Any Later Unit's Work Leak In?

Checked against the task's own explicit exclusion list, item by item: `MemoryCore`/`MemoryRetrieval` interfaces — unchanged, confirmed via `git status`. Recovery algorithm — `MemoryCoreRecovery.recover` is called, never modified; `git diff` shows no change to `MemoryCoreRecovery.kt`. Codec — untouched. Durability log — `MemoryCoreDurabilityLog`'s own interface and `FileSystemMemoryCoreDurabilityLog`'s own implementation are both untouched; `DurableMemoryCore` only calls the interface's own two already-fixed methods. Identifier restoration logic — `restoreIdentifierCounters` and `nextSequenceFor` are both untouched; `DurableMemoryCore` never calls either (correctly — counter restoration is `MemoryCoreRecovery`'s own job, already invoked once, inside `recover`, before `DurableMemoryCore.create` ever wraps the result). Composition into `ParkerRuntime` — no reference to `ParkerRuntime` anywhere. Backup, snapshots, compaction, pruning, version migration — none referenced. **Sound.**

---

## Findings

No required correction was found. The central design tension this Unit exists to resolve (append-first ordering versus the existing `create*` operations' own atomic mint-and-store shape) is independently re-derived as correctly resolved, with an additional supporting argument (Section 2) the Completion Review itself did not make. Both `prepare*`/`restore*` reuse and the `transitionStatus` pre-validation reuse are confirmed to be genuine reuse, not disguised duplication. The disclosed concurrency gap is confirmed genuinely unclosable without Implementation Unit 7's own scope. Read-path purity, exact failure-message preservation, and burned-identifier safety are each independently verified, not merely accepted. No later unit's work leaked in.

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction.

---

## Recommended Next Step

No further correction is required for Unit 6. Per this task's own explicit instruction, no Defect Confirmation Review is necessary, since no required defect was found. Per this task's own explicit stop point, work halts here: Unit 7 is not begun; nothing is staged, committed, or pushed.

---

## Final Git Status at Time of This Review

```
$ git status --short
 M src/runtime/InMemoryMemoryCore.kt
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_6_DECORATOR_COMPLETION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_6_DECORATOR_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? src/runtime/DurableMemoryCore.kt
?? tests/runtime/DurableMemoryCoreTest.kt
```

Nothing staged, committed, or pushed.
