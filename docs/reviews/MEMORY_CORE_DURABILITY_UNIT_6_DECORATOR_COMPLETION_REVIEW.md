# Memory Core Durability — Unit 6: Durable Memory Core Decorator — Completion Review

## Status

**Implementation complete for Unit 6 only.** Unit 7 and every later durability unit are not begun. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD (before this Unit began):** `9351b05b84ceaad189eaf49b44c0fc5483b76fb9`
- **Branch:** `main`
- **Working tree (before this Unit began):** clean. Units 1–5 confirmed complete, reviewed, committed, and pushed at this HEAD.

---

## Planning Review

Read fresh: `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` (in particular §4, §6, §11, §12); `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md` (§7, §13, §14); `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md` in full; `src/runtime/InMemoryMemoryCore.kt`; `src/runtime/MemoryCoreRecovery.kt`; `src/runtime/MemoryCoreDurabilityLog.kt`, `FileSystemMemoryCoreDurabilityLog.kt`, `DurableMemoryCoreEntry.kt`.

**Session "Unit 6" is, structurally, the Implementation Plan's own deferred Unit 3 output.** The Plan's own "Unit 3 — Append and Atomicity Behaviour" section already specifies exactly this deliverable: "The write-path half of `DurableMemoryCore`... for each of the six `MemoryCore` operations, `DurableMemoryCore` first constructs and durably appends the corresponding `DurableMemoryCoreEntry`... and only then invokes the corresponding `InMemoryMemoryCore` operation... Every `MemoryRetrieval` method on `DurableMemoryCore` delegates directly, unchanged, to the wrapped `InMemoryMemoryCore` instance." Session Unit 3 instead built the Plan's own deferred Unit 2 output (the concrete filesystem log). This session's own Units 4 and 5 then built recovery and identifier restoration standalone, each disclosing that the decorator they would eventually attach to did not yet exist. Unit 6 is that decorator — the natural, lawful next step once its three dependencies (`InMemoryMemoryCore`, `MemoryCoreDurabilityLog`, `MemoryCoreRecovery`) were all already complete.

---

## Boundary Review — A Genuine, Load-Bearing Ordering Conflict, Resolved From Governing Text, Not Guessed

**The conflict.** This task's own literal write-path instruction ("1. Create the appropriate `DurableMemoryCoreEntry`. 2. Append it. 3. Only after a successful append invoke the existing in-memory implementation") cannot be satisfied by the existing `create*` operations alone: each of the five mints its own identifier and stores the resulting record as one atomic, inseparable step, so there is no existing way to obtain a complete, durably-appendable record *without already having stored it in memory*.

**Resolution, from governing text, not invented.** The Contract Design's own §11 settles this directly: it names exactly one acceptable failure window — "failure after durable commit but before the in-memory update completes" — and never once contemplates the reverse (an in-memory update completing before the durable commit). This confirms durable-append-first is the only sanctioned model, not merely one option among several. The resolution: five small, additive `prepare*` functions on `InMemoryMemoryCore` (mint and validate, but do not store), paired with the *existing* `restore*` functions (Unit 4) for the actual commit step, which already do exactly "insert an already-complete, already-identified record" with no minting of their own. A `prepare*` call whose subsequent append then fails leaves its freshly-minted identifier permanently unused — a disclosed, already-tolerated gap, not a new risk (sparse identifier sequences are already a normal, correctly-handled outcome per Unit 5's own design).

**A second, narrower instance of the same conflict, in `transitionStatus`.** Naively appending a `StatusTransitioned` entry before confirming the transition is valid risks durably committing a fact the real `transitionStatus` call then rejects — corrupting the log in a way that would fail *all future recovery*, not merely the one failed call. Resolved by having the decorator call `MemoryCoreLifecycleTransitions.requireValidTransition` — the exact same closed-table check `InMemoryMemoryCore.transitionStatus` itself already uses, not a second implementation of the rules — *before* ever touching the durability log. A narrower race window (a transition invalidated by a concurrent one between this pre-check and the append) remains, disclosed and explicitly deferred to Unit 7 (Concurrency and Ordering), which this task's own instruction already excludes from this Unit's scope.

**No governance amendment was required.** Both resolutions are derived directly from already-accepted Contract Design text and from reusing existing, already-governed mechanisms (Unit 4's `restore*`, the existing `MemoryCoreLifecycleTransitions`) — no new policy was invented.

---

## Files Created

- `src/runtime/DurableMemoryCore.kt` — the decorator itself.
- `tests/runtime/DurableMemoryCoreTest.kt`
- `docs/reviews/MEMORY_CORE_DURABILITY_UNIT_6_DECORATOR_COMPLETION_REVIEW.md` (this document)

## Files Modified

- `src/runtime/InMemoryMemoryCore.kt` — **additive only**, confirmed by `git diff`: 139 insertions, **0 deletions**. Five new `internal suspend fun prepare*` functions, one per creatable record kind.

No other file is touched. `src/interfaces/MemoryCore.kt`, `MemoryCoreRecovery.kt`'s own recovery algorithm, `DurableMemoryCoreEntryCodec.kt`, `MemoryCoreDurabilityLog.kt`'s own interface, `InMemoryMemoryCore.restoreIdentifierCounters`, `ParkerRuntime.kt`, `Dockerfile`, and `docker-compose.yml` are all untouched.

---

## Recovery Mechanism / Decorator Design

`internal class DurableMemoryCore private constructor(inMemory: InMemoryMemoryCore, durabilityLog: MemoryCoreDurabilityLog) : MemoryCore, MemoryRetrieval`:

- **Construction.** No public constructor. `DurableMemoryCore.create(durabilityLog)` is the only entry point: calls `MemoryCoreRecovery.recover` exactly once and wraps the result. A recovery failure propagates `MemoryCoreRecoveryException` unwrapped — no instance is ever produced on failure, no partial runtime.
- **Writes (five creation operations).** `prepare*` (mint + validate, no store) → durable append → `restore*` (commit, Unit 4's own unmodified function). An append failure propagates immediately; nothing is ever rolled back, because nothing was ever stored before the append succeeded.
- **`transitionStatus`.** Read current status → `MemoryCoreLifecycleTransitions.requireValidTransition` (reused, not duplicated) → durable append → the existing, unmodified `InMemoryMemoryCore.transitionStatus`.
- **Reads (ten `MemoryRetrieval` methods).** Pure delegation to `inMemory`, confirmed both by direct reading (no `durabilityLog` reference anywhere in a read method) and by a dedicated behavioural test.
- **Recovery discipline.** `MemoryCoreRecovery.recover` is called exactly once, inside `create`, and nowhere else in this file — no lazy replay, no background repair, no automatic rebuilding.

---

## Tests Added

`tests/runtime/DurableMemoryCoreTest.kt` — 22 tests: successful recovery during construction (empty and non-empty logs); recovery failure propagation with no instance produced; successful writes for provenance and entity, confirmed both by the resulting read and by direct inspection of the fake log's own `appended` list; a referential-integrity failure (decorator regression against `InMemoryMemoryCore`'s own behaviour) appending nothing; append failure preventing mutation, for a first-ever write, for a write following an already-successful one, and (added during self-review, below) confirming a burned identifier from a failed create is never reissued by a later, successful one; read delegation, confirmed by both correctness and by the durability log's own append count staying unchanged across several reads; lifecycle persistence (a transition durably appended before being applied, with `priorStatus`/`targetStatus` both preserved on the entry); an impossible transition and a missing-target transition both appending nothing; deterministic behaviour (two independently-recovered instances from identical log content agree exactly); identifier continuity after recovery; decorator transparency (used through both interface types; relationship referential-integrity regression); one full real-filesystem construct→write→restart→read→continue-minting cycle; internal visibility; private-constructor structural check; prohibited-dependency structural check on `create`'s own signature.

**22 tests added in total.**

---

## Targeted Test Results

```
$ ./gradlew test --tests "*DurableMemoryCoreTest*"
BUILD SUCCESSFUL
```
`DurableMemoryCoreTest`: `tests="21" skipped="0" failures="0" errors="0"`.

```
$ ./gradlew test --tests "*InMemoryMemoryCoreTest*" --tests "*MemoryCoreRecoveryTest*"
BUILD SUCCESSFUL
```
Both suites pass unchanged (60 and 28 tests respectively), confirming the additive `prepare*` extension introduced zero regression.

---

## Full Repository Verification

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 48s
```

Aggregated across every test-result XML: `tests="1895" skipped="5" failures="0" errors="0"`.

1895 total, up from the pre-Unit-6 baseline of 1873 by exactly the 22 tests this Unit adds. Zero failures, zero errors, the same 5 pre-existing skips as before — zero regression in any other subsystem.

---

## Self-Review, Performed Before This Document Was Presented as Complete

**Every quotation was checked word-for-word against its cited source.** One mis-citation was found and corrected before finalising: the new `DurableMemoryCore` KDoc originally attributed the phrase "one class, two interfaces" to `InMemoryMemoryCore`'s own KDoc, in quotation marks — `InMemoryMemoryCore`'s actual text reads "one interface, one implementing class," describing `InMemoryKnowledgeStore`'s own precedent, not `InMemoryMemoryCore` itself. Corrected to describe the shape in the new file's own words and to quote `InMemoryMemoryCore`'s own actual phrase accurately, correctly attributed to the precedent it actually describes. The Contract Design §11 quotation ("failure after durable commit but before the in-memory update completes") was independently re-verified as an exact match.

**A genuine test-coverage gap was found and closed during this same self-review, before any Independent Constitutional Review began.** The original "append failure prevents mutation" tests proved a failed create leaves nothing stored, but did not prove the identifier it had already minted is genuinely never reused afterward — the specific property the "prepare, then append, then commit" design exists to guarantee safely. Added: `an identifier already minted by a failed create is never reused by a later, successful one -- it becomes a permanent gap`, re-run and confirmed passing.

- **No architectural decision was invented** beyond the two disclosed resolutions in the Boundary Review, above, both derived directly from already-accepted Contract Design text.
- **The additive-only constraint on `InMemoryMemoryCore.kt` is confirmed literally**: `git diff` shows 139 insertions, 0 deletions.
- **No public API changes exist anywhere** — `MemoryCore`, `MemoryRetrieval`, `MemoryCoreDurabilityLog`, the codec, and the recovery algorithm are all untouched; `DurableMemoryCore` itself is `internal` with a `private` constructor, confirmed by dedicated structural tests.
- **No `Unit 7` (concurrency), `Unit 8` (composition into `ParkerRuntime`), or later work leaked in** — no outer cross-call serialisation guard was added (the one disclosed, narrow race window in `transitionStatus` is explicitly named as Unit 7's own responsibility); no reference to `ParkerRuntime`, `Docker`, backup, snapshots, compaction, pruning, or version migration exists anywhere in the diff.
- **Preserve every existing public Memory Core behaviour** — confirmed both by the decorator's own regression tests (identical referential-integrity and lifecycle-transition rejection behaviour to `InMemoryMemoryCore` directly) and by the full, unmodified `InMemoryMemoryCoreTest`/`MemoryCoreRecoveryTest` suites continuing to pass unchanged.

---

## Final Git Status

```
$ git status --short
 M src/runtime/InMemoryMemoryCore.kt
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_6_DECORATOR_COMPLETION_REVIEW.md
?? src/runtime/DurableMemoryCore.kt
?? tests/runtime/DurableMemoryCoreTest.kt
```

Nothing staged, committed, or pushed.

---

## Recommended Next Step

An Independent Constitutional Review of this Unit follows, mirroring this session's own unbroken pattern, with particular scrutiny on the two disclosed ordering resolutions in the Boundary Review, above.
