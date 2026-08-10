**Status:** Unit 3-C Controlled Remedy Experiments — Completion Review — **REFRESHED (third refresh). PASS.** Implementation only, against committed baseline `c4b840f` (the halted live-execution attempt's own Execution Evidence Review, committed). No live model call, no HTTP call, no live campaign directory, no production (`src/`) change, no remedy selected. This refresh supersedes the prior (second) refresh in place: it preserves that refresh's own warm-up orchestration defect history unchanged (Section 3) and adds the live-execution trigger defect discovered by the halted execution attempt and its correction, verified independently in this task (new Section 3a).

# Unit 3-C Controlled Remedy Experiments — Completion Review

## 1. Files changed

- **Modified in the immediately prior (warm-up) correction, unchanged by this refresh's own task:** `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` — added a private `Unit3COrchestrationDriver.runWarmups` method, wired unconditionally into `runArm` for the Control family only, before any scored trial; updated two pre-existing tests whose stated expectations were stale relative to the newly-correct behavior; added four new tests proving warm-up presence, order, exactly-once execution, distinguishability from scored observations, crash-recovery safety, arm-scoped failure isolation, and unconditional execution.
- **Modified in this task:** `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` — nine new `@Test` functions and one private helper added (Section 3a); the frozen fixture corpus, Family A/B/C mechanisms, `Unit3CConfigLoader`, `Unit3CArtifactRootPolicy`-consuming logic, and `Unit3CLiveEntryPoint.run`'s own executable body are all byte-for-byte unchanged — only its doc comment was corrected from "never invoked by any test" (now false) to an accurate description of the gated trigger.
- **Not touched:** `build.gradle.kts` (the existing `includeTestsMatching` class-level filter already covers the new test methods without modification); any frozen governance document; any campaign artifact directory; any `src/**` file.

## 2. Boundary Review status

**Not required**, re-confirmed: the correction reuses the already-frozen `Unit3CArmLedger` class unchanged, adding only orchestration-level control flow around it. No production interface was touched.

## 3. Warm-up orchestration defect and correction

The failed Explicit Execution Approval Review (`2dca602`) discovered that the committed orchestration driver counted three warm-up trials toward the frozen 483-call total but never executed them — `trialsFor(CONTROL)` returned only the 145 scored Control trials, and an exhaustive search of the driver file for `warmup` returned zero matches. Independently re-confirmed in this task before any correction was made (Defect Confirmation Review). **Corrected:** `runArm` now calls a new `runWarmups` method unconditionally for the Control family, executing all three warm-up trials, in frozen order, through their own dedicated `control/warmup/` ledger — structurally separate from Control's own scored `control/` ledger — before Control's scored trials are attempted. A warm-up failure halts the Control arm only, preserving the same arm-level fault isolation every other measurement-invalidating defect already receives; Family A, B, and C are unaffected.

## 3a. Live-execution trigger defect and correction

The committed Execution Evidence Review (`c4b840f`) discovered, while preparing to actually configure and invoke the authorized live campaign, that no code path anywhere in the repository connected the detached `unit3cControlledRemedyExperiments` Gradle task to a real, environment-driven invocation of `Unit3CLiveEntryPoint.run`: that function was referenced in exactly two places — its own declaration, and one existing test proving only that it fails closed on an empty map. Independently re-confirmed in this task, fresh, before any correction was made (Live Trigger Defect Confirmation Review). **Corrected:** one new `@Test` function, gated by two `assumeTrue` checks (the Gradle-set `parker.reasoning.unit3c.enabled` system property, then a real, non-blank `PARKER_REASONING_UNIT3C_CAMPAIGN_ID` environment variable), whose body — reached only when both gates pass — is exactly one call to `Unit3CLiveEntryPoint.run(System.getenv(), Path.of("."))`. This reuses the already-governed entry point verbatim; it is not a re-implementation of any part of it. Eight further tests were added alongside it, all read-only exercises of already-existing, unmodified code: source-level proof the trigger invokes the entry point exactly once, gated by exactly two `assumeTrue` checks; source-level proof the entry point wires all four family executors into the real orchestration driver; and fail-closed proofs for a wrong model name, a blank model digest, a malformed campaign identity, and a wrong artifact root, each independently traced to the specific `require`/`throw` statement it exercises. No file other than `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` was touched by this correction; `git diff --stat -- src/` and `git diff --stat -- build.gradle.kts` are both empty.

## 4. Corrected Family C trace

Unchanged since the second-prior review cycle, re-confirmed by a fresh test run: 24/29 correct, four false positives (`P03`, `P04`, `P05`, `P12`), one false negative (`R03`). Not touched by this task's correction, which was confined entirely to warm-up execution wiring.

## 5. Executable 483-call schedule

**Warm-up: 3. Control: 145. Family A: 220. Family B: 115. Family C: 0. Total: 483 — now proven genuinely executable by the driver itself, not merely arithmetically derivable from the schedule definition.**

Two structurally independent proofs: (1) `Unit3CCampaignDefinition`'s own driver-independent `init`-time check, unchanged, still confirms 483 as a number derived from the schedule; (2) `exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups` runs the actual driver against counting fake executors and confirms the executor is invoked exactly 483 times in total, with exactly 3 of those being warm-up invocations specifically — a genuine end-to-end proof neither the schedule-only nor the driver-only tests provided before this task. Control's own reported `completedTrialCount` remains 145 (warm-ups do not contaminate the scored count, verified directly).

Also independently verified: no duplicate calls (warm-ups execute exactly once each, in frozen order; a second, idempotent driver run over an already-complete campaign re-invokes the executor for none of them); no missing calls (all three warm-up trial IDs present in the recovered ledger after one run); Family C remains exactly zero model calls (unchanged); no hidden retry (the reused, unmodified `Unit3CArmLedger` duplicate-prevention logic); artifact validation (the disk-space gate) makes zero calls before it passes (unchanged, re-verified); safety-checkpoint handling makes zero additional calls (unchanged, re-verified, and unaffected by the warm-up change since it concerns only scored trials).

## 6. Supplemental-fixture, Family A, Family B, artifact-root, disk-space, safety-checkpoint, exact-once, downstream-isolation, and lifecycle-isolation results

Unchanged from the prior Completion Review and re-confirmed by the fresh, full test run in this task; not affected by the warm-up correction, which touched only Control's own trial-execution entry point.

## 7. Targeted tests

**78 tests, 0 failures, 0 errors, 1 skipped** (`./gradlew unit3cControlledRemedyExperiments --rerun-tasks`) — 49 in the schedule/mechanism file (40 prior, unchanged, + 9 new from the live-trigger correction), 29 in the orchestration file (unchanged by this task). The one skip is the new live trigger test itself, correctly and structurally skipped: the detached task's own `parker.reasoning.unit3c.enabled` system property is always `true` for that task's scope, but no `PARKER_REASONING_UNIT3C_CAMPAIGN_ID` environment variable is present during ordinary offline execution, so the trigger's second `assumeTrue` gate aborts before any configuration is loaded or any client constructed — independently confirmed via the JUnit XML result, which reports `skipped` rather than `passed`, a status only reachable if `Assumptions.assumeTrue` genuinely threw.

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

## 13a. Test correction made during this task's own verification

One of the nine new tests, as first drafted, incorrectly asserted `System.getProperty(UNIT_3C_PROPERTY)` is globally absent by default; running it under the detached `unit3cControlledRemedyExperiments` task immediately failed, because that task itself sets the property to `"true"` for its own execution scope by design (`build.gradle.kts`'s `systemProperty("parker.reasoning.unit3c.enabled", "true")`), independent of whether real live configuration is present. Corrected to assert the property actually relied upon for safety: that no real `PARKER_REASONING_UNIT3C_CAMPAIGN_ID` environment variable is present during ordinary offline execution — the actual gate that determines whether the trigger's second `assumeTrue` aborts. This is a test-authoring correction, not a defect in the correction's own design; the original two-gate trigger function itself required no change.

## 14. Completion verdict

```text
PASS
```

Ready for Independent Constitutional Review of Completion.
