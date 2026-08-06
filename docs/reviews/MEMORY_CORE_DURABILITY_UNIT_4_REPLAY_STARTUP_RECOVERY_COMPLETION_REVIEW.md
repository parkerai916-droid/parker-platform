# Memory Core Durability — Unit 4: Replay and Startup Recovery — Completion Review

## Status

**Implementation complete for Unit 4 only.** Identifier-counter restoration, durable write decoration, runtime composition, Docker work, and every later durability unit are not begun. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD (before this Unit began):** `31d27341288093181fa1fb4cb641ce968075768c`
- **Branch:** `main`
- **Working tree (before this Unit began):** clean. Units 1–3 confirmed complete at this HEAD.

---

## Planning and Numbering Reconciliation

**What Units 1–3 actually delivered**, verified against current source (not memory): Unit 1 — the pure `DurableMemoryCoreEntry` data model. Unit 2 — the `MemoryCoreDurabilityLog` interface only, no production implementation. Unit 3 — `FileSystemMemoryCoreDurabilityLog` and `DurableMemoryCoreEntryCodec`, which is what the Implementation Plan's own text labels **Unit 2's** output, built under the session's "Unit 3" label because Unit 2 deferred it.

**Does session "Unit 4" match the Plan's own Unit 4, or is it a relabelling?** It matches directly in purpose, governing authority, and non-responsibilities. The one structural gap: the Plan's own Unit 4 describes this capability as `DurableMemoryCore.recover()`, a method on a `MemoryCore`/`MemoryRetrieval`-implementing decorator that does not exist in this repository (confirmed: `grep -rn "^internal class DurableMemoryCore\b|^class DurableMemoryCore\b" src/` returns no match) — this session's own prior Unit built the Plan's deferred Unit 2 output instead of the Plan's own Unit 3 (the decorator), and this session's current task explicitly excludes building the decorator here too. Resolution, disclosed in the production code's own KDoc: recovery is built as a **standalone internal component** (`MemoryCoreRecovery`), not a method on a not-yet-built decorator. Nothing planned is duplicated by this — Units 1–3 cover format, interface, and append only; none performs replay or restoration.

**A direct tension in this task's own instructions, resolved via `AskUserQuestion` before any code was written:** the top-level Constraints section states "Do not modify... InMemoryMemoryCore," while the Pre-implementation design determination explicitly asks whether this Unit "may add internal restore pathways to InMemoryMemoryCore," and the Required implementation section describes such restore functions in detail. The user confirmed: "Do not modify" prohibits behavioural or public API changes only; additive, internal-only restoration functions authorised by the Implementation Plan's own Section 5 are permitted, provided existing public methods, signatures, semantics, and externally observable behaviour remain unchanged. Confirmed by direct diff inspection (below): the change to `InMemoryMemoryCore.kt` is 137 insertions, 0 deletions — not one existing line was altered.

**Governance sufficiency — one genuine gap found and resolved conservatively, not by inventing a heuristic:** the Implementation Plan's own Unit 4 text speculates that a decode failure on a durability log's own *last* line can safely be treated as a discardable, merely-interrupted write, distinguishable from genuine corruption by file position alone. Examined rigorously: this is a heuristic, not a proof — corruption from a cause unrelated to an in-progress append could equally affect only the last line, and neither the Contract Design nor the Scope Lock fixes a concrete mechanism for telling the two cases apart from the artifact itself, only the conceptual distinction between them. Per this task's own explicit instruction ("stop and report that limitation rather than inventing a heuristic"), **no such leniency is implemented.** Recovery treats any decode failure, at any position including the last line, as a full recovery failure — the strictly safer, fail-closed reading, independently required by both governing documents regardless ("no silent empty-store fallback"). This narrows Unit 4 by exactly one optional leniency; it does not block the Unit, and is verified directly by a dedicated test (`an incomplete or malformed final line fails recovery just like any other malformed line`).

No other governance gap was found. Full design determination (identity-preserving restoration, replay order, referential integrity, idempotence, schema handling, incomplete-terminal-data, identifier-counter deferral, success/failure signalling) is documented in this session's own prior planning-determination response and reflected directly in the implementation below.

---

## Governance Sufficiency Determination

Sufficient, on the basis above. No governance amendment or clarification was required to proceed with Unit 4 as scoped; the one identified gap (incomplete-terminal-data distinguishability) is resolved by adopting the strictly safer behaviour already required elsewhere in governance, not by requesting new authority.

---

## Files Created

- `src/runtime/MemoryCoreRecovery.kt` — the recovery orchestration object and its exception hierarchy.
- `tests/runtime/MemoryCoreRecoveryTest.kt`
- `docs/reviews/MEMORY_CORE_DURABILITY_UNIT_4_REPLAY_STARTUP_RECOVERY_COMPLETION_REVIEW.md` (this document)

## Files Modified

- `src/runtime/InMemoryMemoryCore.kt` — additive only (137 insertions, 0 deletions): five new `internal suspend fun restore*` functions.
- `tests/runtime/InMemoryMemoryCoreTest.kt` — additive only (144 insertions, 0 deletions): seven new tests for the restore* functions.

No other file is touched. `MemoryCore.kt`, `ParkerRuntime.kt`, `Dockerfile`, `docker-compose.yml`, and every Unit 1–3 file are untouched.

---

## Recovery Mechanism Implemented

`internal object MemoryCoreRecovery` with `suspend fun recover(durabilityLog: MemoryCoreDurabilityLog): InMemoryMemoryCore`:

1. Reads every entry via `durabilityLog.readAll()`, in exact durable (file) order — which alone already guarantees provenance-before-dependents, since the original write path could only ever have produced that order.
2. For each of the five creation-entry kinds, calls the corresponding new `restore*` function.
3. For each `StatusTransitioned` entry, reads the target record's current status first, then decides among exactly three cases before ever calling the existing public `transitionStatus`: current status already equals the entry's own target (idempotent duplicate — skipped, `transitionStatus` never called again, since the closed transition table has no `X → X` self-loop and calling it a second time would otherwise throw); current status equals the entry's own claimed prior status (the normal case — validity is checked directly for a clear failure message, then applied via the unmodified public method); neither (a genuine prior-status mismatch — rejected).
4. Returns a fully reconstructed `InMemoryMemoryCore` on success, or throws before returning anything on any failure.

---

## Internal Restoration Pathways Added

Five `internal suspend fun` additions to `InMemoryMemoryCore.kt`: `restoreProvenance`, `restoreEntity`, `restoreDocument`, `restoreAssertion`, `restoreRelationship`. Each:

- Accepts the already-complete domain record (its own original identifier, timestamp, provenance reference, and initial status) and stores it directly — mints no identifier, advances no sequence counter.
- Re-runs the exact same `requireExistingProvenance`/`requireResolvableEndpoint` referential-integrity checks its own `create*` counterpart already uses.
- Checks for an already-occupied identifier first: identical content is accepted idempotently (returns normally, no mutation); different content throws `IllegalStateException` (corruption).
- Holds no `PermissionEngine` reference and performs no permission evaluation, identical to every other operation this class implements.
- Is `internal`, unreachable through `MemoryCore` or `MemoryRetrieval` — confirmed both by direct reading and by a dedicated reflection-based test.

---

## Failure and Corruption Behaviour

`internal sealed class MemoryCoreRecoveryException`: `DurabilityLogUnreadable` (the log itself could not be read, or any entry — including one with an unsupported schema version — could not be decoded); `RestorationFailed` (a creation entry's own referential-integrity check failed); `ConflictingDuplicateIdentity` (a repeated identifier carries different content); `MissingTransitionTarget` (a `StatusTransitioned` entry's own target does not exist yet); `PriorStatusMismatch`; `ImpossibleTransition`.

`recover()` has exactly two outcomes: a fully reconstructed `InMemoryMemoryCore` (success), or a thrown exception (failure) — there is no third, partial-return path. A genuinely empty or missing log produces a genuinely empty, successfully recovered instance; any unreadable, unsupported, inconsistent, or corrupt condition throws rather than ever producing an empty-looking store from real, present, invalid data. No incomplete-terminal-data leniency exists, per the disclosed governance-gap resolution above.

---

## Tests Added

`tests/runtime/InMemoryMemoryCoreTest.kt` (+7): identity/timestamp/provenance preservation for `restoreProvenance`/`restoreEntity` (with a dependent-record referential check standing in for `Provenance`'s own missing direct-lookup method); broken-provenance and broken-relationship-endpoint rejection; idempotent-duplicate acceptance; conflicting-duplicate rejection; internal-visibility structural check.

`tests/runtime/MemoryCoreRecoveryTest.kt` (+24): empty-log recovery; restoration of each of the five creation kinds; identity/timestamp preservation at the orchestration level; single and multi-step lifecycle transition replay; dependency-aware ordering (provenance → entities → relationship, in one log); broken-provenance and broken-relationship-reference rejection; missing-transition-target, prior-status-mismatch, and impossible-transition rejection; conflicting-duplicate-identity rejection (creation-level); repeated-identical-record idempotence (both creation-level and transition-level, the latter specifically proving the duplicate-transition-replay design decision is correct — a naive re-application would throw); recovery-failure-exposes-no-partial-store; unsupported-schema-version rejection and malformed-non-terminal-line rejection (both against a real `FileSystemMemoryCoreDurabilityLog` over `@TempDir`, per this task's own instruction); the incomplete-terminal-data test proving no leniency exists; internal-visibility and prohibited-dependency structural checks.

**31 tests added in total.**

---

## Targeted Results

```
$ ./gradlew test --tests "*InMemoryMemoryCoreTest*"
BUILD SUCCESSFUL
```
`InMemoryMemoryCoreTest`: `tests="47" skipped="0" failures="0" errors="0"`.

```
$ ./gradlew test --tests "*MemoryCoreRecoveryTest*"
BUILD SUCCESSFUL
```
`MemoryCoreRecoveryTest`: `tests="24" skipped="0" failures="0" errors="0"`.

---

## Full-Suite Result

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 47s
```

Aggregated across every test-result XML: `tests="1856" skipped="5" failures="0" errors="0"`.

1856 total, up from the pre-Unit-4 baseline of 1825 by exactly the 31 tests this Unit adds. Zero failures, zero errors, the same 5 pre-existing skips as before — zero regression in any other subsystem.

---

## Self-Review, Performed Before This Document Was Presented as Complete

**Every quotation in both production files' own KDoc was checked word-for-word against its cited source before being presented in quotation marks.** Three defects were found and corrected during this self-review, the same discipline this session's prior units have each applied:

1. `MemoryCoreRecovery.kt` presented "Provenance before dependents" in quotation marks as though quoting Contract Design Section 6 — it is this file's own paraphrase, not the source's actual text. Corrected to quote the real sentence directly.
2. The same file's `ConflictingDuplicateIdentity` KDoc bridged two non-adjacent fragments of one sentence via ellipsis ("a repeated identifier carrying different content... is corruption"). Corrected to quote the full, contiguous sentence.
3. `InMemoryMemoryCore.kt`'s new restore-function KDoc paraphrased `MemoryCoreLifecycleTransitions`'s own KDoc ("internal, not private, so it can be exercised directly without ever becoming public API") as though it were a direct quotation. Corrected to quote the actual source sentence in full, without ellipsis.

All three corrections were re-verified by re-running the affected test suites after each fix, confirming every correction was comment-only.

- **No architectural decision was invented beyond the two disclosed resolutions** (the standalone-recovery-component structural gap; the incomplete-terminal-data conservative default), both grounded directly in already-accepted authority or this task's own explicit instruction.
- **The additive-only constraint on `InMemoryMemoryCore.kt` is confirmed literally, not merely asserted**: `git diff` shows 137 insertions, 0 deletions.
- **No new public `MemoryCore`/`MemoryRetrieval` surface exists** — confirmed by dedicated reflection tests on both the restore* functions' own visibility and `MemoryCoreRecovery`'s own visibility, and by `src/interfaces/MemoryCore.kt` remaining absent from every modified-file list.
- **No Permission Engine, Knowledge Memory, runtime composition, Docker, or filesystem-path leakage** through `MemoryCoreRecovery.recover`'s own public signature — confirmed by a dedicated reflection-based test, not merely asserted.
- **Identifier-counter restoration was not implemented** — confirmed by direct reading of the new code (no sequence-counter field is touched anywhere in the five restore* functions).

---

## Final Git Status

```
$ git status --short
 M src/runtime/InMemoryMemoryCore.kt
 M tests/runtime/InMemoryMemoryCoreTest.kt
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_4_REPLAY_STARTUP_RECOVERY_COMPLETION_REVIEW.md
?? src/runtime/MemoryCoreRecovery.kt
?? tests/runtime/MemoryCoreRecoveryTest.kt
```

Nothing staged, committed, or pushed.

---

## Recommended Next Step

An Independent Constitutional Review of this Unit follows, mirroring this session's own unbroken pattern, with particular scrutiny on the nine points this task's own governing instruction names explicitly.
