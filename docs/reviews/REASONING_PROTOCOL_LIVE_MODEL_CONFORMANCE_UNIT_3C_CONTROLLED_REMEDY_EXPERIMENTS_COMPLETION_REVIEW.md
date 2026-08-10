**Status:** Unit 3-C Controlled Remedy Experiments — Completion Review — **REFRESHED (fourth refresh). PASS.** Implementation only, against committed baseline `77a9917` (the disk-space-gate execution halt, recorded and committed). No live model call, no HTTP call, no live campaign directory, no production (`src/`) change, no remedy selected. This refresh supersedes the prior (third) refresh in place: it preserves that refresh's own warm-up and live-trigger defect history unchanged (Sections 3, 3a) and adds the disk-space-gate and live-test-scoping defects discovered by the first genuine authorized live-execution attempt and their corrections, verified independently in this task (new Section 3b).

# Unit 3-C Controlled Remedy Experiments — Completion Review

## 1. Files changed

- **Modified in the warm-up correction (unchanged by this refresh's own task):** `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` warm-up sections — added `Unit3COrchestrationDriver.runWarmups`, wired unconditionally into `runArm` for Control.
- **Modified in the live-trigger correction (unchanged by this refresh's own task):** `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` — nine `@Test` functions and one helper added; `Unit3CLiveEntryPoint.run`'s own doc comment corrected.
- **Modified in this task:** `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` — `Unit3COrchestrationDriver.run()`'s disk-space check now targets `artifactRoot.parent`; two new tests added. `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` — one existing test tagged `@Tag("unit3cLiveTaskIncompatible")`, one new tag constant added. `build.gradle.kts` — the detached `unit3cControlledRemedyExperiments` task's `useJUnitPlatform` now `excludeTags` that tag, with an explanatory comment.
- **Not touched:** any frozen governance document; any campaign artifact directory; any `src/**` file.

## 2. Boundary Review status

**Not required**, re-confirmed: the correction reuses the already-frozen `Unit3CArmLedger` class unchanged, adding only orchestration-level control flow around it. No production interface was touched.

## 3. Warm-up orchestration defect and correction

The failed Explicit Execution Approval Review (`2dca602`) discovered that the committed orchestration driver counted three warm-up trials toward the frozen 483-call total but never executed them — `trialsFor(CONTROL)` returned only the 145 scored Control trials, and an exhaustive search of the driver file for `warmup` returned zero matches. Independently re-confirmed in this task before any correction was made (Defect Confirmation Review). **Corrected:** `runArm` now calls a new `runWarmups` method unconditionally for the Control family, executing all three warm-up trials, in frozen order, through their own dedicated `control/warmup/` ledger — structurally separate from Control's own scored `control/` ledger — before Control's scored trials are attempted. A warm-up failure halts the Control arm only, preserving the same arm-level fault isolation every other measurement-invalidating defect already receives; Family A, B, and C are unaffected.

## 3a. Live-execution trigger defect and correction

The committed Execution Evidence Review (`c4b840f`) discovered, while preparing to actually configure and invoke the authorized live campaign, that no code path anywhere in the repository connected the detached `unit3cControlledRemedyExperiments` Gradle task to a real, environment-driven invocation of `Unit3CLiveEntryPoint.run`: that function was referenced in exactly two places — its own declaration, and one existing test proving only that it fails closed on an empty map. Independently re-confirmed in this task, fresh, before any correction was made (Live Trigger Defect Confirmation Review). **Corrected:** one new `@Test` function, gated by two `assumeTrue` checks (the Gradle-set `parker.reasoning.unit3c.enabled` system property, then a real, non-blank `PARKER_REASONING_UNIT3C_CAMPAIGN_ID` environment variable), whose body — reached only when both gates pass — is exactly one call to `Unit3CLiveEntryPoint.run(System.getenv(), Path.of("."))`. This reuses the already-governed entry point verbatim; it is not a re-implementation of any part of it. Eight further tests were added alongside it, all read-only exercises of already-existing, unmodified code: source-level proof the trigger invokes the entry point exactly once, gated by exactly two `assumeTrue` checks; source-level proof the entry point wires all four family executors into the real orchestration driver; and fail-closed proofs for a wrong model name, a blank model digest, a malformed campaign identity, and a wrong artifact root, each independently traced to the specific `require`/`throw` statement it exercises. No file other than `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` was touched by this correction; `git diff --stat -- src/` and `git diff --stat -- build.gradle.kts` are both empty.

## 3b. Disk-space-gate and live-test-scoping defects and corrections

The first genuine, fully-authorized live-execution attempt (Explicit Execution Approval Review 3, campaign ID `unit3c-remedy-experiments-20260810`, commit `ec73296`) exported real environment configuration and invoked the detached Gradle task for the first time under those conditions. Zero live model calls occurred; the build failed with two independent test failures, recorded in the Execution Evidence Review's Attempt 2. Independently re-confirmed in this task, fresh, before any correction was made (Disk-Space and Live-Test-Scoping Defect Confirmation Review):

- **Disk-space gate:** `Unit3COrchestrationDriver.run()` called `Unit3CDiskSpaceGate.check` against the campaign-specific artifact directory, which does not yet exist for a first-ever campaign; the real `Files.getFileStore` lambda throws `NoSuchFileException` against a nonexistent path, which the gate's own fail-closed design correctly converts into `Unit3CInsufficientSpaceException` — meaning the approved campaign could never pass this gate regardless of how much free space the durable parent actually had. **Corrected:** the driver now checks the campaign directory's already-existing, durable *parent* (`artifactRoot.parent`) instead — the gate's own contract, the 2 GiB minimum, and every existing gate test are unchanged; only the caller's choice of target path changed. Two new tests prove the corrected behavior, one of them exercising the real, unmocked `Files.getFileStore` default against a genuinely non-existent campaign directory, directly reproducing (and proving fixed) the exact condition the real attempt hit.
- **Live-test scoping:** a verification test asserting no real campaign-ID environment variable is present (correct for ordinary offline verification) was, by construction, always going to fail during any genuine live run, since a genuine live run necessarily sets that variable — and the detached live task's whole-class test filter selected it unconditionally. **Corrected:** the test is now tagged `@Tag("unit3cLiveTaskIncompatible")`; the detached task's `useJUnitPlatform` now `excludeTags` that tag. The assertion's own text and meaning are unchanged; it still runs, and still passes, under the general offline `reasoningProtocolLiveModelEvaluation` task (independently re-confirmed present there, 49 tests, 0 failures).
- **Incidental Unit 1 side effect:** the first draft of the Gradle-task comment explaining the tag exclusion happened to contain the literal substring `reasoningProtocolLiveModelEvaluation`, which fell within an existing Unit 1 lifecycle-isolation test's own broad regex scan window and produced a false-positive failure. Corrected by rewording the comment only; the Unit 1 test itself was not modified and remains an accurate, unweakened detector.

No file other than `build.gradle.kts`, `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, and `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` was touched by these corrections; `git diff --stat -- src/` remains empty.

## 4. Corrected Family C trace

Unchanged since the second-prior review cycle, re-confirmed by a fresh test run: 24/29 correct, four false positives (`P03`, `P04`, `P05`, `P12`), one false negative (`R03`). Not touched by this task's correction, which was confined entirely to warm-up execution wiring.

## 5. Executable 483-call schedule

**Warm-up: 3. Control: 145. Family A: 220. Family B: 115. Family C: 0. Total: 483 — now proven genuinely executable by the driver itself, not merely arithmetically derivable from the schedule definition.**

Two structurally independent proofs: (1) `Unit3CCampaignDefinition`'s own driver-independent `init`-time check, unchanged, still confirms 483 as a number derived from the schedule; (2) `exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups` runs the actual driver against counting fake executors and confirms the executor is invoked exactly 483 times in total, with exactly 3 of those being warm-up invocations specifically — a genuine end-to-end proof neither the schedule-only nor the driver-only tests provided before this task. Control's own reported `completedTrialCount` remains 145 (warm-ups do not contaminate the scored count, verified directly).

Also independently verified: no duplicate calls (warm-ups execute exactly once each, in frozen order; a second, idempotent driver run over an already-complete campaign re-invokes the executor for none of them); no missing calls (all three warm-up trial IDs present in the recovered ledger after one run); Family C remains exactly zero model calls (unchanged); no hidden retry (the reused, unmodified `Unit3CArmLedger` duplicate-prevention logic); artifact validation (the disk-space gate) makes zero calls before it passes (unchanged, re-verified); safety-checkpoint handling makes zero additional calls (unchanged, re-verified, and unaffected by the warm-up change since it concerns only scored trials).

## 6. Supplemental-fixture, Family A, Family B, artifact-root, disk-space, safety-checkpoint, exact-once, downstream-isolation, and lifecycle-isolation results

Unchanged from the prior Completion Review and re-confirmed by the fresh, full test run in this task; not affected by the warm-up correction, which touched only Control's own trial-execution entry point.

## 7. Targeted tests

**79 tests, 0 failures, 0 errors, 1 skipped** (`./gradlew unit3cControlledRemedyExperiments --rerun-tasks`) — 48 in the schedule/mechanism file (49 total minus the one tagged test now correctly excluded from this task's own selection), 31 in the orchestration file (29 prior + 2 new disk-space tests). The one skip is the live trigger test itself, correctly and structurally skipped (no real campaign ID present offline). Independently re-confirmed via a fresh XML report that the tagged `assertNull` test does not appear in this task's own result set at all (not skipped — genuinely not selected).

## 7a. Confirmation the tagged test still runs offline

`./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks` (no tag filter): schedule/mechanism file reports **49** tests, including the tagged test, passing (0 failures) — confirming the correction excludes the test from one specific task's selection without weakening or deleting the check itself.

## 8. Unit 1/2/2-D regression results

Run together via the unfiltered `reasoningProtocolLiveModelEvaluation` task: Unit 1 16/1-skip/0-fail; Unit 2 19/1-skip/0-fail; Unit 2-D 27/1-skip/0-fail. Zero regression. (Unit 1's own suite initially showed one failure during this task's own verification, traced to an incidental comment-wording side effect, Section 3b — corrected, then re-confirmed 16/1-skip/0-fail.)

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

That very test — correct in substance under ordinary offline verification, but selected unconditionally by the live task's own whole-class filter — is the exact test whose scope this task's own Section 3b corrects; it was not caught until the first genuine live-configured run, because no earlier task ever actually exported real live configuration against the live task before this one did.

## 13b. Test correction made during this task's own verification

The first draft of the Gradle-task comment explaining the new `excludeTags` call incidentally contained the literal substring `reasoningProtocolLiveModelEvaluation`, which caused an existing Unit 1 lifecycle-isolation test to report a false positive (Section 3b). Corrected by rewording the comment only, with no change to the Unit 1 test, the regex it uses, or any lifecycle task definition.

## 14. Completion verdict

```text
PASS
```

Ready for Independent Constitutional Review of Completion.
