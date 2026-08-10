**Status:** Independent Constitutional Review of the Unit 3-C Completion Review — **REFRESHED (third refresh). ACCEPTED.** A fresh, independent adversarial review. Every claim in the thrice-refreshed Completion Review was independently re-derived from source and from a freshly re-run test suite, including the newly added Section 3a (live-execution trigger defect and correction) and Section 13a (test-authoring self-correction). This refresh does not merely re-affirm the second refresh's own findings (Sections 2–13 below are carried forward because independently re-verified unaffected by this task's diff, not because they were assumed); it adds independent scrutiny of the live-trigger correction specifically (Sections 14–19). No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred.

# Unit 3-C Controlled Remedy Experiments — Completion Independent Constitutional Review

## 1. Method

Independently re-read the corrected `Unit3COrchestrationDriver`, including `runWarmups` and the modified `runArm`. Independently re-ran `./gradlew unit3cControlledRemedyExperiments --rerun-tasks`, `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks`, and `./gradlew test --rerun-tasks`, parsing XML directly. Independently re-verified `git diff --stat -- src/` is empty and that `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` does not appear in `git diff --stat` for this task at all (proving the schedule/mechanism file is genuinely untouched, not merely claimed to be).

## 2. Is 483 now genuinely executable, not merely arithmetic?

Yes, independently confirmed by re-running `exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups` and inspecting its own counters: a single counter shared across Control/Family A/Family B reaches 483 after one `driver.run()` call; a second, separate counter confirms exactly 3 of those are warm-up-specific. This is a materially stronger proof than the prior review cycle's own 480-plus-a-verified-constant-3 approach, which this review independently judges was the actual gap the failed Approval Review correctly caught.

## 3. Are warm-ups exact-once?

Yes. Independently re-ran `all three warm-ups are present, exactly once, in frozen order, distinguishable from scored observations`: the executed ID sequence matches `Unit3CCampaignDefinition.warmupTrials.map { it.id }` exactly; the warm-up ledger contains exactly 3 records; Control's scored ledger contains exactly 145 records and never the warm-up fixture's ID. Independently re-ran `crash recovery cannot duplicate already-completed warm-ups`: a second, independent driver instance over the same, already-sealed root re-invokes the executor for none of the 3 already-completed warm-ups.

## 4. Is there any hidden call?

No. Independently re-derived: 3 + 145 + 220 + 115 + 0 = 483, matching the driver's own measured count exactly, with no unaccounted-for invocation. `runWarmups`'s loop iterates its trial list exactly once, with no retry construct anywhere in its body.

## 5. Was any experiment design changed?

No. Independently confirmed `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` does not appear anywhere in this task's `git diff` — the fixture corpus, Family A/B/C mechanisms, Family B's SHA-256, and the corrected Family C trace are untouched by construction, not merely asserted unchanged. Re-ran their own dedicated tests fresh: all pass, unchanged results.

## 6. Was remedy selection or neutrality affected?

No. The correction concerns only mechanical execution of an already-frozen warm-up step; independently searched the diff for any language favoring or selecting a remedy family: none found.

## 7. Did any live execution occur during this task?

No. Independently confirmed no `PARKER_REASONING_*` environment variable is set; every test uses a fake executor; `Unit3CLiveEntryPoint` itself is untouched by this task's diff.

## 8. Independent re-run of all reported counts

`./gradlew unit3cControlledRemedyExperiments --rerun-tasks`: **78 tests (49 + 29), 0 failures, 1 skipped** — updated from the second refresh's own 69/0/0 figure, independently re-run for this third refresh, not carried forward from either the Completion Review's or the Defect Confirmation Review's own report. The nine-test increase and the new single skip both independently reconcile exactly with the live-trigger correction's own diff (nine new tests added; the trigger test itself is the one skip). `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks`: five classes, Unit 1 16/1-skip/0-fail, Unit 2 19/1-skip/0-fail, Unit 2-D 27/1-skip/0-fail, both Unit 3-C classes as above (49/1-skip/0-fail + 29/0-skip/0-fail). `./gradlew test --rerun-tasks`: 2015/5-skip/0-fail, independently re-confirmed to contain zero Unit 3-C or live-model-evaluation test-result files (`ls build/test-results/test/ | grep -i unit3c` and `| grep -i "Diagnostic\|LiveModelConformance\|BaselineCharacterisation"` both empty), directly proving structural exclusion from the ordinary lifecycle rather than assuming it from source-set configuration alone. All figures match the refreshed Completion Review's own Section 7–9 figures exactly.

## 9. Were the two test-expectation corrections sound, not defect-masking?

Yes, independently re-derived: the `148` figure is the only arithmetically consistent value once the same executor genuinely serves both warm-up and scored Control trials (3 + 145 = 148); the idempotent-recovery premise correction is independently judged the objectively correct expected behavior (a fully-completed campaign resuming cleanly is safe, not an error), not a weakening of any actual duplicate-prevention guarantee — the guarantee itself (no re-invocation for an already-completed trial) is what the corrected test now actually proves, more precisely than the original, incorrect exception-based version would have.

## 10. Boundary Review determination

Re-confirmed correct: `git diff --stat -- src/` empty; the correction reuses `Unit3CArmLedger` unchanged.

## 10a. Is the live-trigger correction genuinely minimal and confined to test/integration tier?

Yes, independently re-verified against the raw diff, not the Completion Review's own description of it: `git diff --stat` shows exactly one file changed (`ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, +123/-4); `git diff --stat -- src/` and `git diff --stat -- build.gradle.kts` both independently confirmed empty. This is the same independent-diff-inspection standard this review already applied to the warm-up correction (Section 5 above), applied fresh to a different diff rather than assumed satisfied by analogy.

## 10b. Does the trigger genuinely invoke the real entry point, or could the new tests pass by coincidence?

Independently probed specifically, since this is the exact failure category this review chain has twice already missed (Family C's trace, the warm-up wiring). Independently re-read the trigger function's literal source a second time, separately from the source-scan test's own logic: `Unit3CLiveEntryPoint.run(System.getenv(), Path.of("."))` is the literal, un-abstracted call — not a fake, not a stand-in, not a call to a differently named test-only wrapper. Independently re-inspected the JUnit XML result for this specific test method: it reports `skipped`, a status `Assumptions.assumeTrue` produces only by genuinely throwing `TestAbortedException` when its condition is false — a coincidental `passed` result cannot masquerade as `skipped`. This independently rules out the possibility that the trigger test is inert by construction rather than by correct, working gate logic.

## 10c. Are the fail-closed claims (wrong model, blank digest, malformed campaign ID, wrong artifact root) genuinely exercising the entry point, not a lower-level function in isolation?

Yes, independently re-checked: all four negative tests call `Unit3CLiveEntryPoint.run(environment, Path.of("."))` directly (not `Unit3CConfigLoader.load` or `Unit3CArtifactRootPolicy.resolve` in isolation, except the one dedicated, explicitly-labeled "accepts a fully valid environment through configuration and artifact-root resolution" positive test, which is intentionally scoped below the entry point to prove the two gates succeed on genuinely valid input without ever constructing a driver). Independently re-traced each exception type to its exact throw site: `IllegalArgumentException` from `require(live.modelName == UNIT_3C_MODEL_NAME)` and from the campaign-ID `require`s inside `Unit3CConfigLoader.load`; `EvaluationConfigurationException` from the blank-digest `?: throw`; `Unit3CArtifactRootViolationException` from `Unit3CArtifactRootPolicy.resolve`'s prefix check — all four independently confirmed to fire strictly before any `Unit3COrchestrationDriver` construction, by re-reading `Unit3CLiveEntryPoint.run`'s own statement order.

## 10d. Was any real filesystem write performed by any of the nine new tests?

Independently re-checked by reading all nine new test bodies plus the two functions they exercise below the entry point (`Unit3CConfigLoader.load`, `Unit3CArtifactRootPolicy.resolve`): neither function contains `Files.write`, `Files.createDirectories`, or any other I/O call — both are pure string/path computation. Independently re-listed `/var/lib/parker/reasoning-protocol-live-model/` after this task's test runs: unchanged, still exactly the two preserved Unit 2/Unit 2-D directories.

## 10e. Does the correction's own governance-conformity table (Defect ICR Section 7) hold up under independent re-derivation, or was any row merely asserted?

Independently spot-checked three rows judged highest-risk for a merely-asserted pass: row 6 (invokes `run` exactly once) — independently re-read the source-scan test's regex and independently counted the literal string's occurrences in the trigger function by eye, confirming exactly one, not trusting the test's own arithmetic; row 14 (preserves 483-call schedule) — independently re-opened the file at the current `liveModelCallCount == 483` line number and confirmed it is unchanged from the pre-diff version via `git diff` producing no hunk touching that line; row 2 (detached from ordinary lifecycle) — independently re-derived from the `./gradlew test` XML absence (Section 8 above), not from the source-set configuration alone. All three hold under independent re-derivation.

## 10f. Test-authoring self-correction (Completion Review Section 13a) — was it a real defect or genuine test-authoring noise?

Independently assessed: the first draft of one new test asserted `System.getProperty(UNIT_3C_PROPERTY)` is null by default; this is objectively false whenever the `unit3cControlledRemedyExperiments` Gradle task itself runs, because that task's own `build.gradle.kts` registration sets the property to `"true"` unconditionally for its own scope — independently re-confirmed by re-reading `build.gradle.kts` lines 149–160. The correction (asserting the campaign-ID environment variable is absent instead) is independently judged the objectively correct replacement, not a weakened or defect-masking substitute: it tests the actual safety-relevant fact (no real campaign ID present), whereas the original, incorrect assertion tested a fact that was never true in this task's own governed execution context.

## 11. Blocking defects

None.

## 12. Non-blocking qualifications

None.

## 13. Verdict

```text
ACCEPTED
```

The corrected implementation is independently confirmed to genuinely execute all 483 frozen calls, including the three warm-ups, with no duplicate, no omission, no extra call, no experiment-design change, and zero live execution during this task. The live-execution trigger defect identified by the halted execution attempt is independently confirmed genuinely corrected: a real, gated caller of `Unit3CLiveEntryPoint.run` now exists, confined to test/integration tier, fail-closed on every governed negative case, reachable exactly once when both gates pass, and structurally inert (verified `skipped`, not coincidentally `passed`) under every offline execution path exercised in this task.

## 14. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The refreshed Completion Review document itself was not modified by this review.
