# Memory Core Durability — Unit 4: Replay and Startup Recovery — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the governing documents re-read fresh, and against the actual, current file contents — not against the Completion Review's own summary of them. This document does not amend `src/runtime/InMemoryMemoryCore.kt`, `src/runtime/MemoryCoreRecovery.kt`, either test file, the Completion Review, or any governance document. Per this task's own explicit instruction, this review specifically challenges each of the nine named questions before reaching a verdict.

---

## 1. Baseline Confirmation

`HEAD` is `31d27341288093181fa1fb4cb641ce968075768c`, unchanged since this task began. `git diff --stat` confirms `src/runtime/InMemoryMemoryCore.kt` carries 137 insertions and **zero** deletions; `tests/runtime/InMemoryMemoryCoreTest.kt` carries 144 insertions and zero deletions. Two new files exist (`MemoryCoreRecovery.kt`, `MemoryCoreRecoveryTest.kt`) plus this Unit's own Completion Review. No other file is touched.

---

## 2. Challenge — Does Restoration Preserve Original Identity Rather Than Recreating Records?

Checked directly against the current text of all five `restore*` functions: none constructs an `EntityId`/`DocumentId`/`AssertionId`/`RelationshipId`/`ProvenanceId` from a sequence counter — each stores the identifier already present on the passed-in domain object, unchanged (`provenanceStore[provenance.provenanceId] = provenance`, and identically for the other four). Confirmed independently by test evidence, not merely by reading intent: `MemoryCoreRecoveryTest`'s own `recovery never mints a replacement identifier for any restored record` asserts the recovered record's own identifier equals the literal string `"entity-99"` supplied to the fixture, not a freshly-minted `"entity-1"` a `create*`-based implementation would have produced. **Sound.**

---

## 3. Challenge — Does Any Internal Restore Function Weaken the Public Memory Core Boundary?

Checked three ways, not one: (a) direct reading confirms all five `restore*` functions are declared `internal`; (b) a dedicated reflection-based test (`restore functions on InMemoryMemoryCore are internal, never public API surface`) asserts this structurally, not merely by inspection; (c) `git status --short` and `git diff --stat` confirm `src/interfaces/MemoryCore.kt` is absent from every changed-file list, and `MemoryCoreInterfacesTest.kt`'s own existing, unmodified "exactly five candidate-to-record operations plus `transitionStatus`" test ran unchanged as part of the reported full-suite pass — a widened `MemoryCore` interface would have failed that pre-existing regression guard directly. Checked further: does `MemoryCoreRecovery` itself, though a separate class, leak a *behavioural* boundary weakening even without a *type-level* one — for example, by giving any caller a way to bypass `requireExistingProvenance`/`requireResolvableEndpoint`? No: every `restore*` function re-runs those exact checks; `MemoryCoreRecovery` never accesses `InMemoryMemoryCore`'s private stores directly. **Sound.**

---

## 4. Challenge — Does Replay Ordering Follow Governance?

Re-derived independently against the Contract Design's own §6 ("Provenance records must be available before any Entity, Document, Assertion, or Relationship record that references one") and §10 (total write order, needed for `findByTimeRange`'s own cross-kind tiebreak): `MemoryCoreRecovery.recover` replays `durabilityLog.readAll()`'s own result via `entries.forEachIndexed`, in exact list order, with no reordering, sorting, or grouping step anywhere in the function. Since `FileSystemMemoryCoreDurabilityLog.readAll()` (Unit 3, unmodified) already guarantees exact append order, and since a `Relationship` or dependent record could only ever have been originally created after its own `Provenance` already existed (enforced by the original `create*` call's own `requireExistingProvenance` check at write time), strict replay order mechanically reproduces the required dependency order without a separate pass. Verified by test, not merely reasoned: `provenance, entities, and a relationship connecting them recover correctly when durable order already satisfies dependency order` exercises exactly this. **Sound** — but see Section 9, below, for one residual ordering question this review pressed further and found not fully closed by testing.

---

## 5. Challenge — Can Corruption Ever Become a Silent Empty Store?

Tested directly, not merely reasoned: `MemoryCoreRecovery.recover` constructs `InMemoryMemoryCore()` once, then calls `applyEntry` for every entry; the first failing `applyEntry` call throws, and the function has no `catch` clause around the `forEachIndexed` loop capable of swallowing that exception and falling through to `return memoryCore` regardless — checked line-by-line, no such path exists. A missing or genuinely empty log is the **only** case producing an empty result, and it does so by returning normally (zero entries, zero iterations, the freshly-constructed empty instance returned as-is) — structurally indistinguishable, in code, from "corruption produced an empty store" only if corruption could somehow cause `readAll()` itself to return an empty list rather than throw. Checked directly against `FileSystemMemoryCoreDurabilityLog.readAll()` (Unit 3): it decodes every line via `DurableMemoryCoreEntryCodec.decode` and throws on the first failure — it has no code path returning a truncated or partial list. **Sound.**

---

## 6. Challenge — Can Partial State Escape After Failed Recovery?

Tested directly: `recover throws before returning anything on a failing sequence` constructs a log where the second of two entries fails, and asserts the call throws — but a stronger, structural argument than any single test can provide is worth stating plainly, since this is exactly the kind of guarantee a test can suggest but not prove exhaustively: `recover`'s own Kotlin signature is `suspend fun recover(...): InMemoryMemoryCore` (non-nullable, non-Result-wrapped) — the *only* way a caller can obtain a value from this function is via its one `return` statement, which is lexically the last line of the function, after the entire `forEachIndexed` loop has completed without throwing. There is no assignment of the in-progress `memoryCore` to any external, escaping reference (no field, no callback, no shared mutable state) anywhere in the function. This is a structural guarantee, not merely a tested one. **Sound.**

---

## 7. Challenge — Is Lifecycle History Replayed Rather Than Collapsed?

This is the correct question to press hardest, since a naive implementation could easily "collapse" a chain of transitions to just the final status without genuinely replaying each step. Checked directly: `multiple transitions on one record are replayed in order, reaching the correct final status -- not collapsed to one step` exercises a two-step chain (`ACTIVE → DISPUTED → ARCHIVED`) via two separate `StatusTransitioned` entries, each independently validated by `applyTransition`'s own three-case logic — each call genuinely invokes `InMemoryMemoryCore.transitionStatus` (or is genuinely, individually recognised as an idempotent skip), never a single computed "jump straight to final status" shortcut. Checked further, independently of the test: does `applyTransition`'s own logic ever have a code path that could skip an *intermediate* transition while still reaching the correct final state through some other means? No — each `StatusTransitioned` entry is processed as its own, separate call to `applyEntry`, in durable order; there is no batching or coalescing of multiple transitions anywhere in `MemoryCoreRecovery`. **Sound.**

---

## 8. Challenge — Was Identifier Restoration Implemented Prematurely?

Checked directly by search, not by trusting the Completion Review's own claim: `grep -n "nextProvenanceSequence\|nextEntitySequence\|nextDocumentSequence\|nextAssertionSequence\|nextRelationshipSequence" src/runtime/InMemoryMemoryCore.kt src/runtime/MemoryCoreRecovery.kt` returns matches only in the five field declarations and the five *original* `create*` methods (lines 146–234, all pre-existing, unmodified) — no match anywhere in the new restore-function bodies (lines ~362 onward) or in `MemoryCoreRecovery.kt`. **Sound** — counter restoration is genuinely absent, not merely undocumented.

---

## 9. Challenge — Did Unit 5 or Later Work Leak Into This Unit?

Checked against this task's own explicit exclusion list, one item at a time: identifier counter max-plus-one restoration — absent (Section 8, above). Durable write-through decoration — absent; no class in either new file implements `MemoryCore` or `MemoryRetrieval`, confirmed by direct reading (`MemoryCoreRecovery` is a plain `object`, not a decorator). Acknowledged-write atomicity across Memory Core and the log — absent; nothing in this Unit performs a durable `append` at all, only `readAll`. Runtime composition / `ParkerRuntime` changes — absent, confirmed by `git status` showing no file under `src/composition/` touched. Permission Engine changes — absent; `recoveryPrincipal` is a `PrincipalId` value passed for auditability only, never a `PermissionEngine` reference, and `getEntity`/`getDocument`/etc. already ignore this parameter entirely (Errata 004's own established behaviour, unmodified). Docker, Knowledge Memory, backup/replication, compaction, checkpointing, indexing, SQLite, cross-process locking, recovery UI — none referenced anywhere in either new file.

**One finding, not blocking, worth surfacing rather than silently accepting.** This review pressed the "duplicate creation entry after a transition" edge case specifically, since it sits at the exact seam between this Unit's own idempotence logic and Unit 3's (Plan-numbered) not-yet-built write-through decorator's own future retry semantics. Scenario: could a *creation* entry (e.g. `EntityCreated`, carrying the record's own *initial* status) ever be durably re-committed and appear in the log *after* a `StatusTransitioned` entry for the same record has already been replayed — which would make the stored (already-transitioned) record's own status differ from the incoming duplicate's initial status, causing `restoreEntity`'s own `check(existing == entity)` to fail and misreport a genuine idempotent duplicate as `ConflictingDuplicateIdentity`? Traced through: this cannot happen under the *only* write-path model this Unit's own design assumes — one caller call attempts to durably commit exactly once (with any retry occurring *within* that single attempt, before the call returns), so a second, legitimate caller-initiated transition could never be interleaved between two retried copies of the *same* original creation call. This is a sound assumption *today*, but it is an assumption about a write path (Plan-numbered Unit 3, the decorator) that does not exist in this repository yet, and this Unit has no way to verify it structurally — only to note it. **Not a defect in what this Unit built**, and not something this Unit's own governing task asked it to solve (durable write-through decoration is explicitly excluded here) — but worth carrying forward explicitly as a design assumption the future decorator unit must uphold, not silently rediscover.

---

## 10. Challenge — Do Tests Freeze Implementation Convenience Instead of Constitutional Behaviour?

Reviewed the full 31-test addition specifically for over-specification. Every test asserting a *specific* `MemoryCoreRecoveryException` subtype (`PriorStatusMismatch`, `ImpossibleTransition`, `MissingTransitionTarget`, `ConflictingDuplicateIdentity`, `RestorationFailed`, `DurabilityLogUnreadable`) is testing that **governance-distinguished failure modes remain genuinely distinguished from one another** — the Contract Design's own §7 explicitly requires treating "malformed... corruption" and "interrupted... discardable" as different cases, and this task's own required-test list names each of these as its own, separate category — asserting the specific subtype is testing that distinction is real, not an arbitrary implementation convenience like an exact internal field value or wall-clock timing. No test was found asserting anything not traceable to a named governing requirement or this task's own explicit required-test list. **Sound.**

---

## Findings

### Finding 1 (Disclosed, Non-Blocking) — A write-path assumption this Unit depends on but cannot itself verify

See Section 9, above. Not a defect in this Unit's own implementation; a design assumption to carry forward explicitly to whichever future unit builds the write-through decorator, so it is upheld deliberately rather than rediscovered under pressure.

No required correction was found. Every one of the nine challenge points this task's own governing instruction named was tested independently against the current code, not accepted from the Completion Review's own account.

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction. One disclosed, non-blocking finding, already fully accounted for and outside this Unit's own authorised scope to resolve.

---

## Recommended Next Step

No further correction is required for Unit 4. Per this task's own explicit instruction, no Defect Confirmation Review is necessary, since no required defect was found. Per this task's own explicit stop point, work halts here: identifier-counter restoration, durable write decoration, runtime composition, and Docker work are not begun; nothing is staged, committed, or pushed.

---

## Final Git Status at Time of This Review

```
$ git status --short
 M src/runtime/InMemoryMemoryCore.kt
 M tests/runtime/InMemoryMemoryCoreTest.kt
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_4_REPLAY_STARTUP_RECOVERY_COMPLETION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_4_REPLAY_STARTUP_RECOVERY_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? src/runtime/MemoryCoreRecovery.kt
?? tests/runtime/MemoryCoreRecoveryTest.kt
```

Nothing staged, committed, or pushed.
