# Memory Core Durability — Unit 5: Identifier Restoration — Completion Review

## Status

**Implementation complete for Unit 5 only.** Unit 6 and every later durability unit are not begun. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD (before this Unit began):** `10c51db672e9a6885d49a671833813e7f46fca26`
- **Branch:** `main`
- **Working tree (before this Unit began):** clean. Units 1–4 confirmed complete, reviewed, committed, and pushed at this HEAD.

---

## Governance Sufficiency Determination

Read fresh: `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`; `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md`; `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`; `src/runtime/InMemoryMemoryCore.kt`; `src/runtime/MemoryCoreRecovery.kt`.

Governance is **sufficient**, with no gap. The rule is fixed identically in three separate places, not merely implied: Contract Design §6 ("Per-record-kind identifier counters must be restored to (highest persisted identifier of that kind) + 1 — never reset to 1, and never left at a value that could collide with, or leave a gap ambiguous against, an already-persisted identifier"); Scope Lock §8 and, as a binding `SHALL`, Scope Lock §18 ("A Memory Core durability implementation **SHALL** restore each per-kind identifier counter to (highest persisted identifier of that kind) + 1 on recovery, and **SHALL NOT** permit identifier reuse across a restart"); and the Implementation Plan's own Unit 5 section, which additionally selects the *mechanism* — derivation from the already-restored records themselves, not separate persistence of a counter value as its own durable fact — "avoiding a second, redundant source of truth that could itself drift out of sync with the records it counts." No ambiguity was found requiring a stop.

---

## Planning Reconciliation

Session "Unit 5" matches the Implementation Plan's own Unit 5 directly — purpose, governing authority, inputs, outputs, production files, non-responsibilities, and completion criteria all align point-for-point, unlike Units 2–4's own numbering divergences. Unit 5 is confirmed the next lawful implementation step: its only dependency (Unit 4, replay) is complete, reviewed, and committed, and no later unit's own work (Unit 6 lifecycle recovery, Unit 7 concurrency, Unit 8 runtime composition) is required first.

---

## Files Created

None. This Unit is implemented entirely as an extension of two already-existing files from Units 4/1.

## Files Modified

- `src/runtime/InMemoryMemoryCore.kt` — **additive only**, confirmed by `git diff`: 111 insertions, **0 deletions**. One new `internal suspend fun restoreIdentifierCounters()` plus one private helper (`nextSequenceFor`).
- `src/runtime/MemoryCoreRecovery.kt` — 39 insertions, 1 deletion (a single KDoc sentence extended, not a behavioural change). `recover()` now calls `restoreIdentifierCounters()` as its last step before returning; one new `MemoryCoreRecoveryException.IdentifierCounterRestorationFailed` subtype.
- `tests/runtime/InMemoryMemoryCoreTest.kt` — additive only, 200 insertions, 0 deletions.
- `tests/runtime/MemoryCoreRecoveryTest.kt` — additive only, 79 insertions, 0 deletions.

No other file is touched. `src/interfaces/MemoryCore.kt`, `ParkerRuntime.kt`, `Dockerfile`, `docker-compose.yml`, and every Unit 1–4 file's own existing behaviour are untouched.

---

## Identifier Restoration Design

**Every identifier sequence that exists:** five, one per creatable record kind — `nextProvenanceSequence`, `nextEntitySequence`, `nextDocumentSequence`, `nextAssertionSequence`, `nextRelationshipSequence` — confirmed via direct reading, unchanged since their original declaration.

**Independence:** each is restored from only its own store's own key set (`provenanceStore.keys`, `entityStore.keys`, and so on) — restoring one kind's counter has no way to read or affect another kind's.

**Safe parsing:** each identifier's own string `value` is checked against its kind's own fixed prefix (`"provenance-"`, `"entity-"`, etc.); the remainder is parsed as a `Long` and required to be positive. Neither check is silently skipped or defaulted — both are `require()` (`IllegalArgumentException`) on failure.

**Behaviour in every named case:**
- No records of a kind exist → that kind's counter resumes at `1`, the same value a genuinely fresh store already starts at (computed uniformly as `0 + 1`, not special-cased).
- Sparse identifiers (e.g. `entity-1`, `entity-5`, `entity-3` present, `entity-2`/`4` absent) → resumes at `6`; the gap is never filled.
- Out-of-order restoration (highest-numbered identifier restored first) → the true numeric maximum is still found, since every key is inspected, not merely the most recently seen one.
- Duplicate identical entries → already collapsed to one store key by Unit 4's own idempotent `restore*` behaviour before this function ever runs; counted once, never inflated.
- Malformed identifiers (wrong prefix, non-numeric or non-positive suffix) → `IllegalArgumentException`, fail closed, never silently skipped or defaulted.
- Identifier values approaching `Long`'s own upper bound → `Math.addExact` detects the overflow `+1` would otherwise cause silently (wrapping to a negative counter) and fails with `IllegalStateException` instead.

**Where restoration belongs:** a single new `internal` function on `InMemoryMemoryCore` itself (not a dedicated separate helper class, and not computed inside `MemoryCoreRecovery`) — the only place with direct, authoritative access to both the store keys (source of truth) and the private counter fields being restored.

**Counters may not be restored before replay succeeds; restoration occurs only after successful recovery:** `MemoryCoreRecovery.recover()` calls `restoreIdentifierCounters()` exactly once, as the last statement before `return`, after the entire entry-replay loop has completed without throwing. If any entry fails to replay, the function propagates that failure immediately and never reaches the counter-restoration call — the partially-populated instance is discarded, unreachable, and its counters (whatever they happened to be) are never observed by any caller.

**No public API changes required, and none made:** `restoreIdentifierCounters()` is `internal`, confirmed by a dedicated reflection-based test; `src/interfaces/MemoryCore.kt` is untouched.

---

## Required Behaviour, Confirmed Against the Implementation

- **Identifier reuse is impossible after recovery** — every newly minted identifier is provably greater than every restored one (tested directly).
- **Each identifier sequence restores independently** — tested directly with mixed restored/untouched kinds in one scenario.
- **Failed recovery never advances counters** — structurally guaranteed (counter restoration is unreachable on any failing path) and tested directly across two separate `recover()` calls.
- **Malformed identifiers fail closed** — `IllegalArgumentException`/`IllegalStateException`, never silently ignored or defaulted.
- **Lifecycle transitions never affect identifier counters** — a `StatusTransitioned` entry only ever mutates an existing record's own `status` field via `.copy()`; it adds no store key, so counter derivation (which reads only store keys) is structurally blind to it. Tested directly.
- **Duplicate replay never inflates counters** — derivation reads the store's own final key set, not a running tally kept during replay, so an idempotently-collapsed duplicate is counted exactly once. Tested directly.
- **Successful replay resumes normal identifier allocation** — tested via full `recover()` → `create*` round trips, both for a non-empty and a genuinely empty log.

---

## Tests Added

`tests/runtime/InMemoryMemoryCoreTest.kt` (+13): empty store; single restored identifier; sparse identifiers; out-of-order restoration; independent per-kind counters; duplicate-collapse non-inflation; malformed prefix rejection; non-numeric-suffix rejection; `Long.MAX_VALUE` overflow rejection; lifecycle-transition non-interference; no-collision-after-restoration (five sequential creates checked against three restored identifiers); a `DELETED` record's own identifier still counting toward the restored maximum (added during self-review, below); internal-visibility structural check.

`tests/runtime/MemoryCoreRecoveryTest.kt` (+4): full `recover()` → `create*` round trip after restoring one record; empty-log `recover()` → `create*` round trip; a malformed identifier surfacing only during the counter-restoration step fails the *entire* recovery with the dedicated new exception type; a failed `recover()` attempt has no influence on a later, separate, successful attempt's own counters.

**17 tests added in total.**

---

## Targeted Test Results

```
$ ./gradlew test --tests "*InMemoryMemoryCoreTest*"
BUILD SUCCESSFUL
```
`InMemoryMemoryCoreTest`: `tests="60" skipped="0" failures="0" errors="0"` (up from 47).

```
$ ./gradlew test --tests "*MemoryCoreRecoveryTest*"
BUILD SUCCESSFUL
```
`MemoryCoreRecoveryTest`: `tests="28" skipped="0" failures="0" errors="0"` (up from 24).

---

## Full Repository Verification

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 54s
```

Aggregated across every test-result XML: `tests="1873" skipped="5" failures="0" errors="0"`.

1873 total, up from the pre-Unit-5 baseline of 1856 by exactly the 17 tests this Unit adds. Zero failures, zero errors, the same 5 pre-existing skips as before — zero regression in any other subsystem.

---

## Self-Review, Performed Before This Document Was Presented as Complete

**Every quotation was checked word-for-word against its cited source.** One mis-citation was found and corrected before finalising: the new `restoreIdentifierCounters` KDoc attributed the "avoiding a second, redundant source of truth" quotation to the Implementation Plan's own "Section 5" (a *different* section — "Design Resolution — Identifier Restoration Without Public-Contract Reuse," about a different topic) — the quotation actually appears in the Plan's own "Unit 5" section. Corrected to cite "Unit 5" directly. The Scope Lock §18 `SHALL`/`SHALL NOT` quotation and the `"kind-N"` formatting were independently re-verified as exact matches against their sources.

**A genuine test-coverage gap was found and closed during this same self-review, before any Independent Constitutional Review began.** `restoreIdentifierCounters` derives each counter from a store's own current key set with no status filtering — correctly, since a `DELETED` record is never physically removed from its store (Version 1 Scope Lock: an identifier is "never reassigned, reused, or recycled — including for a `DELETED` record"). This behaviour was correct by construction but had no dedicated test proving it — a regression that silently added status-based filtering to the counter-derivation logic could have shipped without any test catching it. Added: `a DELETED record's own identifier still counts toward the restored maximum -- its identifier is never reused`, re-run and confirmed passing.

- **No architectural decision was invented** — every behaviour traces directly to Contract Design §6, Scope Lock §8/§18, or the Implementation Plan's own Unit 5 text.
- **The additive-only constraint on `InMemoryMemoryCore.kt` is confirmed literally**: `git diff` shows 111 insertions, 0 deletions.
- **No new public API exists** — confirmed by dedicated reflection test and by `src/interfaces/MemoryCore.kt`'s continued absence from every changed-file list.
- **No counter persistence as a separate durable record was implemented** — confirmed by direct reading: `DurableMemoryCoreEntry.kt` is untouched, and no new entry kind or field was added anywhere.
- **No Unit 6 (lifecycle recovery, already substantively exercised as a side effect of Unit 4's own transition-replay logic) work was newly implemented here** — the lifecycle-transition-non-interference test added by this Unit tests only that transitions do not affect *counters*, not lifecycle-replay correctness itself, which remains Unit 4's own, already-reviewed responsibility.

---

## Final Git Status

```
$ git status --short
 M src/runtime/InMemoryMemoryCore.kt
 M src/runtime/MemoryCoreRecovery.kt
 M tests/runtime/InMemoryMemoryCoreTest.kt
 M tests/runtime/MemoryCoreRecoveryTest.kt
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_5_IDENTIFIER_RESTORATION_COMPLETION_REVIEW.md
```

Nothing staged, committed, or pushed.

---

## Recommended Next Step

An Independent Constitutional Review of this Unit follows, mirroring this session's own unbroken pattern.
