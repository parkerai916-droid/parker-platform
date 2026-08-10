**Status:** Independent Constitutional Defect Review — **ACCEPTED.** The Defect Confirmation Review's own claims were independently re-verified from source and from a fresh test run, not accepted on report. No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred.

# Unit 3-C Warm-Up Orchestration Defect — Independent Constitutional Review

## 1. Method

Independently re-ran `git diff --stat` to confirm the change surface before reading any claim about it. Independently re-read the corrected `Unit3COrchestrationDriver` in full, including the new `runWarmups` method and the modified `runArm`. Independently re-ran the full Unit 3-C suite, the combined Unit 1/2/2-D/3-C run, and the full ordinary suite, parsing JUnit XML directly. Independently re-traced the call-accounting arithmetic by hand.

## 2. Was the defect real?

Yes, independently re-confirmed by the same method the Defect Confirmation Review used, performed fresh rather than trusted: a case-insensitive search for `warmup` in the pre-correction orchestration file (recovered via `git show HEAD:tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` for this review's own comparison) returns zero matches, confirming the driver never referenced `warmupTrials` before this task's correction.

## 3. Is the correction narrow?

Yes. `git diff --stat` shows exactly one file changed: `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt`, 171 insertions, 9 deletions. `git diff --stat -- src/` is empty. `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` — containing the schedule, fixtures, Family A/B/C mechanisms, artifact schema, and config loader — is untouched, independently confirmed by its absence from the diff entirely. `build.gradle.kts` is untouched.

## 4. Do all three warm-ups now execute?

Yes, independently re-verified by re-running `all three warm-ups are present, exactly once, in frozen order, distinguishable from scored observations` and inspecting its own assertions: the executed warm-up trial IDs, in order, equal `Unit3CCampaignDefinition.warmupTrials.map { it.id }` exactly; the warm-up ledger's `raw.jsonl` independently contains exactly 3 non-blank lines; Control's own scored `raw.jsonl` independently contains exactly 145 non-blank lines and never contains the warm-up fixture's ID.

## 5. Is the total genuinely 483?

Yes, independently re-confirmed by re-running `exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups` and inspecting its own counters directly: a counting fake executor shared across Control/Family A/Family B reaches exactly 483 invocations after one full `driver.run()` call, with a separate counter confirming exactly 3 of those 483 are warm-up invocations specifically. Cross-checked against the schedule's own independent, driver-free `liveModelCallCount == 483` — both agree.

## 6. Was any extra call added?

No. Independently re-derived: 3 (warm-up) + 145 (Control-scored) + 220 (Family A) + 115 (Family B) + 0 (Family C) = 483, matching exactly, no more and no fewer. The correction adds exactly the 3 previously-missing calls to the executable path — it does not introduce a fourth warm-up, a retry, or any additional call anywhere; independently confirmed by re-reading `runWarmups`, which iterates `Unit3CCampaignDefinition.warmupTrials` exactly once via a single `for` loop with no retry or repeat logic of any kind.

## 7. Are the experiment arms unchanged?

Yes. Family A, Family B, and Family C's own `trialsFor` cases in `Unit3COrchestrationDriver` are byte-for-byte unchanged from before this correction (independently confirmed by diff inspection — the `when` block's three non-`CONTROL` branches are untouched). Their own dedicated tests (decision/rendering builders, candidate hash, five-step mechanism and corrected trace) were independently re-run and pass unchanged.

## 8. Is remedy neutrality unchanged?

Yes. This correction concerns only the mechanical execution of an already-frozen, already-governed warm-up step (verifying the shared live bridge, using the identical `DefaultReasoningPromptBuilder`-based path Control already used) — it selects no remedy, favors no family, and does not touch any Family A/B/C mechanism definition. No language anywhere in the correction expresses a preference for any family's outcome.

## 9. Was any production behavior changed?

No. `git diff --stat -- src/` is empty; independently re-confirmed by direct execution of the same command during this review, not by trusting the Defect Confirmation Review's own report of it.

## 10. Did any live execution occur?

No. Independently confirmed: no `PARKER_REASONING_*` environment variable is set in the current shell; every test in the modified file uses a fake `Unit3CTrialExecutor`; `Unit3CLiveEntryPoint` itself was not touched by this correction and remains reachable only via real, explicit environment configuration this task never supplied.

## 11. Regression re-confirmation

Independently re-ran fresh: `./gradlew unit3cControlledRemedyExperiments --rerun-tasks` → 69 tests (40 + 29), 0 failures. `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks` → five classes, Unit 1 16/1-skip/0-fail, Unit 2 19/1-skip/0-fail, Unit 2-D 27/1-skip/0-fail, both Unit 3-C classes as above. `./gradlew test --rerun-tasks` → 2015 tests, 5 skipped, 0 failures, 0 errors. All figures match the Defect Confirmation Review's own report exactly.

## 12. Were the two test-expectation updates (Section 7 of the Confirmation Review) themselves sound corrections, not defect-masking?

Yes, independently assessed: the `148` update to the pre-existing duplicate-execution test is a direct, necessary arithmetic consequence of the same executor now correctly serving both warm-up and scored trials — the test's own *proof* (that a sealed ledger rejects further appends) is untouched, only the incidental call-count expectation changed to match newly-correct behavior. The rewritten crash-recovery test's premise correction (idempotent no-op recovery rather than an expected exception) was independently re-derived by this review as the objectively correct behavior for a full second run over an already-sealed campaign — asserting otherwise would have required the ledger to treat "everything is already done" as an error, which it correctly does not.

## 13. Blocking defects

None.

## 14. Non-blocking qualifications

None.

## 15. Verdict

```text
ACCEPTED
```

The warm-up orchestration defect is independently confirmed real, its correction independently confirmed narrow (one file, no `src/` change, no governance change), and the corrected driver independently confirmed to genuinely execute all 483 calls — 3 warm-up plus 480 scored/offline — with no duplicate, no omission, no extra call, and zero live execution during this task.

## 16. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production file changed. No fixture, Family A/B/C definition, or governance document was altered. No remedy was selected. The Defect Confirmation Review document itself was not modified by this review.
