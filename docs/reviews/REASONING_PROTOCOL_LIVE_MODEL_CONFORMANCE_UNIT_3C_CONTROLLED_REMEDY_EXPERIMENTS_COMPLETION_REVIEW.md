**Status:** Unit 3-C Controlled Remedy Experiments — Completion Review — **REFRESHED (second refresh). PASS.** Implementation only, against committed baseline `2dca602` (the failed Explicit Execution Approval Review, committed). No live model call, no HTTP call, no live campaign directory, no production (`src/`) change, no remedy selected. This refresh supersedes the prior Completion Review in place: it reflects the warm-up orchestration defect discovered by that failed Approval Review and its correction, verified independently in this task.

# Unit 3-C Controlled Remedy Experiments — Completion Review

## 1. Files changed

- **Modified:** `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` — added a private `Unit3COrchestrationDriver.runWarmups` method, wired unconditionally into `runArm` for the Control family only, before any scored trial; updated two pre-existing tests whose stated expectations were stale relative to the newly-correct behavior; added four new tests proving warm-up presence, order, exactly-once execution, distinguishability from scored observations, crash-recovery safety, arm-scoped failure isolation, and unconditional execution.
- **Not touched:** `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` (schedule, fixtures, Family A/B/C mechanisms, artifact schema, config loader — all byte-for-byte unchanged); `build.gradle.kts`; any frozen governance document; any campaign artifact directory; any `src/**` file.

## 2. Boundary Review status

**Not required**, re-confirmed: the correction reuses the already-frozen `Unit3CArmLedger` class unchanged, adding only orchestration-level control flow around it. No production interface was touched.

## 3. Warm-up orchestration defect and correction

The failed Explicit Execution Approval Review (`2dca602`) discovered that the committed orchestration driver counted three warm-up trials toward the frozen 483-call total but never executed them — `trialsFor(CONTROL)` returned only the 145 scored Control trials, and an exhaustive search of the driver file for `warmup` returned zero matches. Independently re-confirmed in this task before any correction was made (Defect Confirmation Review). **Corrected:** `runArm` now calls a new `runWarmups` method unconditionally for the Control family, executing all three warm-up trials, in frozen order, through their own dedicated `control/warmup/` ledger — structurally separate from Control's own scored `control/` ledger — before Control's scored trials are attempted. A warm-up failure halts the Control arm only, preserving the same arm-level fault isolation every other measurement-invalidating defect already receives; Family A, B, and C are unaffected.

## 4. Corrected Family C trace

Unchanged since the second-prior review cycle, re-confirmed by a fresh test run: 24/29 correct, four false positives (`P03`, `P04`, `P05`, `P12`), one false negative (`R03`). Not touched by this task's correction, which was confined entirely to warm-up execution wiring.

## 5. Executable 483-call schedule

**Warm-up: 3. Control: 145. Family A: 220. Family B: 115. Family C: 0. Total: 483 — now proven genuinely executable by the driver itself, not merely arithmetically derivable from the schedule definition.**

Two structurally independent proofs: (1) `Unit3CCampaignDefinition`'s own driver-independent `init`-time check, unchanged, still confirms 483 as a number derived from the schedule; (2) `exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups` runs the actual driver against counting fake executors and confirms the executor is invoked exactly 483 times in total, with exactly 3 of those being warm-up invocations specifically — a genuine end-to-end proof neither the schedule-only nor the driver-only tests provided before this task. Control's own reported `completedTrialCount` remains 145 (warm-ups do not contaminate the scored count, verified directly).

Also independently verified: no duplicate calls (warm-ups execute exactly once each, in frozen order; a second, idempotent driver run over an already-complete campaign re-invokes the executor for none of them); no missing calls (all three warm-up trial IDs present in the recovered ledger after one run); Family C remains exactly zero model calls (unchanged); no hidden retry (the reused, unmodified `Unit3CArmLedger` duplicate-prevention logic); artifact validation (the disk-space gate) makes zero calls before it passes (unchanged, re-verified); safety-checkpoint handling makes zero additional calls (unchanged, re-verified, and unaffected by the warm-up change since it concerns only scored trials).

## 6. Supplemental-fixture, Family A, Family B, artifact-root, disk-space, safety-checkpoint, exact-once, downstream-isolation, and lifecycle-isolation results

Unchanged from the prior Completion Review and re-confirmed by the fresh, full test run in this task; not affected by the warm-up correction, which touched only Control's own trial-execution entry point.

## 7. Targeted tests

**69 tests, 0 failures, 0 errors, 0 skipped** (`./gradlew unit3cControlledRemedyExperiments`) — 40 in the schedule/mechanism file (unchanged), 29 in the orchestration file (25 prior + 4 new, 2 of the prior 25 updated for correctness).

## 8. Unit 1/2/2-D regression results

Run together via the unfiltered `reasoningProtocolLiveModelEvaluation` task: Unit 1 16/1-skip/0-fail; Unit 2 19/1-skip/0-fail; Unit 2-D 27/1-skip/0-fail. Zero regression.

## 9. Full ordinary repository test result

`./gradlew test`: 2015 tests, 5 skipped (pre-existing), 0 failures, 0 errors.

## 10. Live model/HTTP calls made

**Zero.** No live environment variable was set at any point during this task.

## 11. Campaign directories created

**None.** `/var/lib/parker/reasoning-protocol-live-model/` contains only the two pre-existing Unit 2/Unit 2-D directories.

## 12. Blocking defects

**None.**

## 13. Test corrections made during this task's own verification

Two pre-existing tests required updating, both due to their own stated expectations becoming stale relative to newly-correct behavior, not due to any defect in the correction itself: (1) `driver prevents duplicate execution of an already-completed trial` expected Control's executor to be called 145 times; corrected to 148 (145 scored + 3 warm-up), since the same executor now correctly serves both; its actual duplicate-prevention proof is unchanged. (2) The newly-added crash-recovery test's first draft incorrectly expected a full second run over an already-sealed campaign to throw an exception; corrected to assert the executor is never re-invoked for any already-completed trial, the behavior that actually matters — a full idempotent second run is safe, not an error condition.

## 14. Completion verdict

```text
PASS
```

Ready for Independent Constitutional Review of Completion.
