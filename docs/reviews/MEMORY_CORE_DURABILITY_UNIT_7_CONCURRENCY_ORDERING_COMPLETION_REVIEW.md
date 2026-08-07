# Memory Core Durability — Unit 7: Concurrency and Ordering — Completion Review

## Status

**Implementation complete for Unit 7 only.** Unit 8 (Runtime Composition) and every later durability unit are not begun. Nothing is staged, committed, or pushed.

---

## Repository Baseline — A Discrepancy Found and Disclosed

**The task's own stated baseline ("HEAD: current clean main," "Units 1–6 complete, reviewed, committed, and pushed") does not match the actual repository state.** `git status --short` at the start of this task showed the working tree carrying Unit 6's own changes still uncommitted — `src/runtime/InMemoryMemoryCore.kt` modified, `src/runtime/DurableMemoryCore.kt` and `tests/runtime/DurableMemoryCoreTest.kt` untracked, plus Unit 6's own two review documents. `HEAD` was `9351b05b84ceaad189eaf49b44c0fc5483b76fb9` (the Unit 5 commit), not a commit reflecting Unit 6. This is consistent with Unit 6's own governing task, which explicitly instructed "Stop before... staging, committing, or pushing" — nothing was committed because nothing was supposed to be. This task's own baseline description appears to describe the intended end-state rather than the actual one. Per this session's own established discipline of verifying claims against primary evidence rather than accepting a stated baseline at face value, this Unit proceeds using the actual, current working tree (Unit 6's own code, already independently reviewed and `ACCEPTED`) as its starting point — not by committing anything itself, since nothing in this task's own instructions authorises that.

---

## Remaining-Plan Reconciliation

Read fresh: `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md` in full (Units 7 through 10, and its own Dependency Graph, §7); `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` §10, §11; `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md` §12; `src/composition/ParkerRuntime.kt`'s own current Memory Core composition (lines ~615–803).

**What units remain after the completed decorator.** The Plan's own Unit 7 (Concurrency and Ordering), Unit 8 (Runtime Composition), Unit 9 (Container Durability), and Unit 10 (Verification) all remain. Session Unit 6 corresponds to the Plan's own Unit 3 "write path, read delegation" output; the Plan's own Unit 7 text states directly that its own outer serialisation guard was meant to be "added in the same pass as Unit 3's own write-path logic — listed as a separate Unit here only because the Scope Lock's own Section 12 names it as a distinct required property to verify, not because it is separable production code." Session Unit 6 did not include it — both Unit 6's own Completion Review and Independent Constitutional Review explicitly disclosed this as a deferred gap, naming Unit 7 as where it would close.

**Whether concurrency/write-ordering hardening is still outstanding.** Confirmed yes, at the start of this task — the outer `DurableMemoryCore`-level guard did not yet exist.

**Whether runtime composition is authorised immediately or depends on another unit first.** It depends on Unit 7. The Plan's own Dependency Graph (§7) states row-for-row: `"8 | 1–7 | Runtime composition..."`. Unit 8's own "Dependencies" field states directly: "Units 1–7, complete and independently tested." Unit 7 was not complete at the start of this task. **Runtime composition was therefore not lawful yet — confirmed before any Kotlin was written, per this task's own explicit "if the next lawful unit is not runtime composition, stop and implement only the actual next unit" instruction.**

**Whether Docker/named-volume wiring is a separate later unit.** Yes — Unit 9, whose own "Dependencies" field names only "Unit 8."

**Whether any verification-only unit remains after composition.** Yes — Unit 10, dependent on "Units 1–9, complete," test-only, no new production code.

**Whether the runtime currently constructs raw `InMemoryMemoryCore`, `DurableMemoryCore`, or both.** Only raw `InMemoryMemoryCore` — confirmed directly: `src/composition/ParkerRuntime.kt` line 634, `val inMemoryMemoryCore = InMemoryMemoryCore()`. `DurableMemoryCore` is referenced nowhere in `ParkerRuntime.kt`. This directly explains the task's own observed symptom (no knowledge retention across a container restart).

**Exactly which live consumers currently receive raw Memory Core references.** `evidenceRegistrationCoordinator` (via `EvidenceRegistrationCoordinator`'s own `memoryCore` constructor parameter, line 638) and the Programme 4 Unit 8 coordinator (`evidenceIntelligenceAcceptanceCoordinator`, line 803) both receive the raw `memoryCore` (= `inMemoryMemoryCore`, typed `MemoryCore`) reference directly, each gating its own writes internally. `permissionFilteredMemoryRetrieval` (line 765) wraps `inMemoryMemoryCore` directly as the one shared `MemoryRetrieval`, consumed by `knowledgeCandidateEvaluator` and `evidenceIntelligenceInputResolver`.

**Whether replacing the raw Memory Core in `ParkerRuntime` would cause double permission gating anywhere.** Not with the composition pattern already in place — the existing inline comments explain both self-gating callers already gate their own `MemoryCore` writes internally, and wrapping either in `PermissionGatedMemoryCore` today would double-gate an already-gated write. This determination is preserved, not re-decided, by this Unit — Unit 8's own future work is what will need to confirm it still holds once `DurableMemoryCore` replaces the raw instance.

**Whether `PermissionGatedMemoryCore` and `PermissionFilteredMemoryRetrieval` boundaries remain intact.** Yes, both — `PermissionGatedMemoryCore` exists in the repository (`src/composition/PermissionGatedMemoryCore.kt`) but is not composed into the current graph; `PermissionFilteredMemoryRetrieval` is actively composed, wrapping `inMemoryMemoryCore`. This Unit touches neither.

**Whether any existing consumer must remain on the raw delegate for constitutional reasons.** Not a question this Unit needs to resolve — composition itself is out of scope here. Noted for Unit 8's own future determination.

---

## Governance Sufficiency

**Sufficient — no gap.** Unit 7's own requirement, mechanism latitude ("`Mutex` is this Unit's own chosen means... freely replaceable... without a governance revision"), production/test file assignment, dependencies, non-responsibilities, and completion criteria are all already fixed in the Implementation Plan's own text, tracing directly to Contract Design §10 and Scope Lock §12. No ambiguity required a stop.

---

## Next Lawful Unit Identified

**Unit 7 — Concurrency and Ordering.** Not runtime composition, per the reconciliation above. Implemented in this task; runtime composition (Unit 8) remains for a future, separate task.

---

## Files Created

None.

## Files Modified

- `src/runtime/DurableMemoryCore.kt` — extended (not yet committed, so no meaningful "insertions/deletions" diff against `HEAD` exists for this file specifically; the change is additive relative to Unit 6's own already-reviewed content: one new `private val writeMutex = Mutex()` field, and every one of the six existing write-operation bodies wrapped in `writeMutex.withLock { ... }`, with no other line altered).
- `tests/runtime/DurableMemoryCoreTest.kt` — extended, three new tests appended; no existing test altered.

`src/runtime/InMemoryMemoryCore.kt` (Unit 6's own already-reviewed 139-insertion, 0-deletion change) remains untouched by this Unit — confirmed by `git diff --stat` showing no further change to it.

No other file is touched. `ParkerRuntime.kt`, `ParkerRuntimeConfig.kt`, `Dockerfile`, `docker-compose.yml`, `MemoryCore.kt`, `MemoryCoreRecovery.kt`, `DurableMemoryCoreEntryCodec.kt`, and `MemoryCoreDurabilityLog.kt` are all untouched.

---

## Runtime Composition Changes

**None — out of scope for this Unit**, per the reconciliation above.

---

## Concurrency Design

`private val writeMutex = Mutex()` on `DurableMemoryCore`, distinct from and outer to `InMemoryMemoryCore`'s own internal `Mutex` and `FileSystemMemoryCoreDurabilityLog`'s own internal write guard. Every one of the six write operations (five creates, `transitionStatus`) now holds `writeMutex` for its *entire* body — prepare/validate through durable append through in-memory commit — guaranteeing durable append order and in-memory commit order can never disagree between two concurrent callers. Read operations deliberately do not acquire `writeMutex`: a reader observing the narrow "durably committed, not yet in-memory-visible" window is the same "acceptable" window Contract Design §11 already names, not a condition this guard exists to close.

**A direct, disclosed consequence:** this guard also closes the one race window `DurableMemoryCore.transitionStatus`'s own KDoc had left open since Unit 6 — a concurrent transition invalidating another's own pre-validated `priorStatus` between its read and its append is no longer possible, since no second write of any kind can begin until the first's entire read-validate-append-commit sequence completes.

No change to `InMemoryMemoryCore`'s own internal `Mutex`. No cross-process concurrency introduced or claimed (Contract Design §10's own disclosed limitation is inherited unchanged).

---

## Tests Added

`tests/runtime/DurableMemoryCoreTest.kt` (+3): a 12-way concurrent-write ordering test using a deliberately *reversed* artificial delay (later-minted identifiers given shorter delays, actively incentivising reordering absent the guard) — proving observed append order is strictly increasing, matching mint order exactly; a 20-way concurrent-write test confirming no write is lost or duplicated under contention; a mixed-kind concurrency test (a lifecycle transition racing a creation on the same instance) confirming both durably commit with none lost.

---

## Targeted Test Results

```
$ ./gradlew test --tests "*DurableMemoryCoreTest*"
BUILD SUCCESSFUL
```
`DurableMemoryCoreTest`: `tests="25" skipped="0" failures="0" errors="0"` (up from 22). Re-run three times total (one incremental, two full `--rerun-tasks` rebuilds) with identical results each time — `runTest`'s own virtual-time scheduler makes this deterministic, not merely observed-to-be-stable, satisfying the Plan's own "passes reliably (no flaking)" completion criterion structurally, not by chance.

---

## Full Repository Verification

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 47s
```

Aggregated across every test-result XML: `tests="1898" skipped="5" failures="0" errors="0"`.

1898 total, up from the pre-Unit-7 baseline of 1895 by exactly the 3 tests this Unit adds. Zero failures, zero errors, the same 5 pre-existing skips as before — zero regression in any other subsystem.

---

## Self-Review, Performed Before This Document Was Presented as Complete

**A genuine test defect was found and corrected during this self-review, before any Independent Constitutional Review began.** The original interleaving-order test supplied a custom `appendBehavior` to `FakeMemoryCoreDurabilityLog` that only introduced a delay and never recorded the entry — `FakeMemoryCoreDurabilityLog`'s own KDoc states directly that a configured `appendBehavior` replaces its default recording entirely ("distinct from what a configured `appendBehavior` may have done instead"). The test failed with an empty observed list, correctly exposing this as a test-authoring defect, not a production one. Corrected: the test now records observed append order into its own local list from within the custom behaviour. Re-run and confirmed passing, including across two additional full-rebuild repetitions.

**Every quotation was checked word-for-word against its cited source.** The Implementation Plan's own Unit 7 quotations ("added in the same pass as Unit 3's own write-path logic...," the `Mutex`-guard description) and the Dependency Graph row were each independently re-verified against the Plan's own current text, not merely recalled from an earlier read in this session.

- **No architectural decision was invented** — the mechanism (`Mutex`), its placement (outer to both existing locks), and its scope (write operations only, not reads) all trace directly to the Plan's own Unit 7 text and to Contract Design §11's own already-established "acceptable window" reasoning.
- **`InMemoryMemoryCore`'s own internal `Mutex` was not touched** — confirmed by `git diff --stat` showing zero further change to that file in this Unit.
- **No cross-process concurrency was introduced or claimed.**
- **Runtime composition, Docker, and every other later unit's own responsibility were correctly left untouched** — confirmed by `git status` showing no file under `src/composition/`, `Dockerfile`, or `docker-compose.yml` touched.

---

## Final Git Status

```
$ git status --short
 M src/runtime/InMemoryMemoryCore.kt
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_6_DECORATOR_COMPLETION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_6_DECORATOR_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_7_CONCURRENCY_ORDERING_COMPLETION_REVIEW.md
?? src/runtime/DurableMemoryCore.kt
?? tests/runtime/DurableMemoryCoreTest.kt
```

Nothing staged, committed, or pushed.

---

## Recommended Next Step

An Independent Constitutional Review of this Unit follows, mirroring this session's own unbroken pattern. Once accepted, Unit 8 (Runtime Composition) becomes the next lawful step — a separate, future task, since this task's own stop point ends here.
