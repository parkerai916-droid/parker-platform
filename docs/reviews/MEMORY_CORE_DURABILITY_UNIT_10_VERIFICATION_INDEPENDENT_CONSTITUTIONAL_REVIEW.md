# Memory Core Durability — Unit 10: Verification — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the governing documents re-read fresh and against the actual, current file contents — not against the Completion Review's own summary of them. This document does not amend any test file, the Completion Review, or any governance document.

---

## 1. Baseline and Diff-Shape Re-Verification

Independently re-confirmed: `git status --short` shows exactly three modified test files (`tests/runtime/DurableMemoryCoreTest.kt`, `tests/runtime/InMemoryMemoryCoreTest.kt`, `tests/composition/ParkerRuntimeMemoryCoreDurabilityCompositionTest.kt`) and one new review document at this point in the review — no `src/` file appears anywhere, independently satisfying this Unit's own "Production files expected to change: None." No trace of the throwaway `DockerRestartProbe.kt` remains — confirmed by direct filesystem search (`find ... -iname "*DockerRestartProbe*"`, zero results) and by `git status --short --ignored`, which shows nothing matching either.

---

## 2. Re-Verification of the Central Governance Question — Was Item 7 Genuinely a Conflict, or Could the Plan's Text Have Been Honoured?

**Test:** re-read `MemoryCoreRecoveryTest.kt`'s own actual test body fresh, independent of the Completion Review's own account. `an incomplete or malformed final line fails recovery just like any other malformed line -- no leniency is implemented for it`, read directly: appends a genuine, valid `ProvenanceCreated` entry, then appends a deliberately truncated line (`"kind=EntityCreated\tschemaVersion=1\tentityId=partial-and-tr"`) via raw `Files.write` with `StandardOpenOption.APPEND` — precisely simulating an interrupted trailing write. Asserts `MemoryCoreRecoveryException.DurabilityLogUnreadable` is thrown. This is unambiguous: the currently-accepted, currently-passing behaviour is that a truncated final line **fails** recovery. Independently re-read Unit 4's own Completion Review (Section on governance sufficiency): "no such leniency is implemented... the strictly safer, fail-closed reading, independently required by both governing documents regardless." Independently re-read Unit 4's own Independent Constitutional Review, Section 5 ("Can Corruption Ever Become a Silent Empty Store?"): traces `MemoryCoreRecovery.recover`'s own control flow directly, confirms no code path could produce a truncated-but-accepted result, verdict `ACCEPTED`. **There is no way to write a passing test for item 7's own literal text ("recovery succeeds using every entry before it") without contradicting an already-accepted, already-independently-re-verified test asserting the opposite.** The conflict was genuine, not overstated, and the resolution selected (close item 7 via the existing test, disclose the Plan's own stale wording) is the only resolution that does not either weaken an already-accepted safety property or silently ignore the Plan's own text. **Sound.**

---

## 3. Challenge — Are the Six New Tests Genuinely Capable of Failing, or Are Any of Them Tautological?

Pressed directly, since a test that cannot fail proves nothing. For each new test, traced what a plausible regression would look like and whether the test would catch it:

- The three new `DurableMemoryCoreTest` restart-cycle tests (Document, Assertion, Relationship) each construct a **second, independent** `DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logFile))` — no shared reference to the first instance exists anywhere in the test body (confirmed by direct re-read). If recovery for any of these three kinds were broken (e.g., a hypothetical future regression in `MemoryCoreRecovery.recover`'s own per-kind `applyEntry` branch), `second.getDocument`/`getAssertion`/`getRelationship` would return `null`, and `assertEquals(created, ...)` would fail against `null`. Genuinely capable of failing.
- The lifecycle-transition restart test similarly constructs a second instance and asserts `ARCHIVED` specifically (not merely non-null) — a regression collapsing the two-step transition to a single jump, or losing the transition entirely, would produce `ACTIVE` or `DISPUTED` instead, failing the assertion. Genuinely capable of failing.
- The Relationship counter test asserts the *exact* next identifier (`relationship-4`, one past the restored `relationship-3`) — a regression in `restoreIdentifierCounters`'s own Relationship branch (e.g., failing to scan the Relationship store at all) would produce `relationship-1` instead, failing the assertion. Genuinely capable of failing.
- The item 10 composition test asserts exact object equality (`assertEquals(seededEntity, ...)`, `assertEquals(seededDocument, ...)`, `assertEquals(seededAssertion, ...)`) against three *independently* reflected consumer fields, plus an exact identifier check (`provenance-2`, `document-2`) after a genuine `submitEvidence` call — a regression in any of the three consumers' own wiring, or in post-restart identifier continuity, would produce a different value or throw. Genuinely capable of failing.

**None of the six new tests is tautological.**

---

## 4. Challenge — Does the Item 10 Test's Own "Pre-Populated Independently of `ParkerRuntime`" Claim Hold Up, or Does It Secretly Reuse `ParkerRuntime` Machinery?

Checked directly: the seeding step constructs `FileSystemMemoryCoreDurabilityLog(logPath)` and `DurableMemoryCore.create(...)` directly — no `ParkerRuntime`, no `ParkerRuntimeConfig`, no `stage()` helper anywhere in the seeding code. The subsequent `ParkerRuntime(config(...), ...)` is a wholly separate construction, reading the same file path only because `config()` was passed the same `logPath` string. **The independence claim is accurate, not overstated.**

---

## 5. Challenge — Does the Item 11 Live Verification Actually Prove Genuine Record Survival, or Could the "Restart" Have Been Too Weak to Matter?

Independently re-examined the exact sequence from the Completion Review's own Section 4e: `docker compose restart parker` is Docker's own documented container-restart primitive — stops the running container's own PID 1 process (delivering SIGTERM, triggering the same graceful-shutdown hook the earlier Unit 8/9 live verifications already exercised, confirmed here again by the log sequence `Shutdown signal received` → `Runtime shutting down` → `Runtime stopped` → `Runtime starting` → `Runtime started`) and starts a **new** container process from the same image against the same named volume. This is not a weaker operation than a full `docker compose down && docker compose up` for the purpose being tested (volume persistence) — Docker volumes are not container-lifecycle-scoped at all; they persist independently of whether the container is merely restarted or fully removed and recreated, provided the volume itself is not explicitly removed (`down` without `-v`, which this step did not use). The probe's own `read` invocation, run via a fresh `docker exec` *after* this restart, constructs its own **new** `DurableMemoryCore.create` call — no in-memory state from the `write` invocation (a separate JVM process entirely, already exited) could have survived by any means other than the file itself. **The verification is genuine and the restart was not weaker than what item 11 requires.**

---

## 6. Challenge — Was Anything Beyond Unit 10's Own Scope Touched?

Checked directly: `git diff --stat` shows exactly three files, all under `tests/`, zero insertions or deletions in any `src/` file. No governance document appears in the diff. No later programme's own file was touched. **Sound.**

---

## 7. Full, Independent Quotation Audit

| Quoted fragment | Cited source | Verified |
| --- | --- | --- |
| "A truncated final entry is discarded; recovery succeeds using every entry before it" | Implementation Plan, Unit 10, item 7 | Exact match, confirmed by direct re-read. |
| "no such leniency is implemented... the strictly safer, fail-closed reading, independently required by both governing documents regardless" | Unit 4 Completion Review | Exact match (lightly elided, contiguous), confirmed by direct re-read; independently re-verified in Section 2, above. |
| "any gap found between what a prior Unit's own 'Test files expected to change' already covers and this list is closed here, not deferred further" | Implementation Plan, Unit 10, Test files expected to change | Exact match, confirmed by direct re-read. |
| "Document, Assertion, and Relationship have no restored records at all" | `InMemoryMemoryCoreTest.kt`'s own pre-existing test comment | Exact match, confirmed by direct re-read of the unmodified surrounding test. |

No further quoted fragment appears in the Completion Review beyond ordinary identifiers. **No defect found.**

---

## Findings

No required correction was found. The central governance question (item 7) is independently re-derived from the actual test bodies and both of Unit 4's own prior reviews, not merely re-accepted. All six new tests are independently confirmed genuinely capable of failing, not tautological. The item 10 test's independence from `ParkerRuntime` machinery during seeding, and the item 11 live verification's genuineness, are each independently re-examined and confirmed sound.

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction. No Defect Confirmation Review is necessary.

---

## Recommended Next Step

Per this task's own explicit stop point: work halts here. This is the final Unit of the Memory Core Durability Programme — all twelve Section 18 Acceptance Criteria items now pass, per the Completion Review's own Section 9 checklist. No subsequent programme work is begun. Nothing is staged, committed, or pushed.

---

## Final Git Status at Time of This Review

```
$ git status --short
 M tests/composition/ParkerRuntimeMemoryCoreDurabilityCompositionTest.kt
 M tests/runtime/DurableMemoryCoreTest.kt
 M tests/runtime/InMemoryMemoryCoreTest.kt
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_10_VERIFICATION_COMPLETION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_10_VERIFICATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
