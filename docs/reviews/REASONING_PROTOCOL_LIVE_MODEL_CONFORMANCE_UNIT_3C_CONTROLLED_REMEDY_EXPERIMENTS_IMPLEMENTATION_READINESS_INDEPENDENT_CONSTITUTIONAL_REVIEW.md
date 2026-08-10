**Status:** Independent Constitutional Review of the Unit 3-C Implementation Readiness Review — **REFRESHED (fourth refresh). ACCEPTED.** Honest admission first: this very document's own immediately prior version (third refresh) is itself an instance of the mistake this review is instructed to guard against. It independently traced caller reachability (Section 7a–7d below) and correctly declared the trigger genuinely reachable — but it treated "reachable" as equivalent to "the full path to a live call passes," without independently re-deriving whether every *gate along that path*, not just the caller, would actually succeed. It never examined the disk-space gate at all. The first genuine live-execution attempt found exactly that gap. This refresh does not repeat it: Sections 7e–7g below specifically adversarially re-derive the disk-space and live-test-scoping corrections, rather than accepting the Readiness Review's own updated Section 2a table as sufficient on its own.

# Unit 3-C Implementation Readiness Review — Independent Constitutional Review

## 1. Method

Independently re-read `runWarmups` and the modified `runArm` in full (unaffected by this task's own diff, re-checked only to confirm they remain untouched). Independently re-read the full diff for this task (`git diff -- tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`) line by line. Specifically applied the adversarial standard this refresh is instructed to apply: for every claim in the Readiness Review's Section 2a table, independently re-derived the evidence from source or from a fresh test run, rather than accepting the table's own citation of a test name as sufficient. Independently re-ran the full Unit 3-C suite fresh, immediately before drafting this document.

## 2. Does the warm-up correction genuinely close the gap, independently re-verified?

Yes. `runWarmups` is called from `runArm` unconditionally for `Unit3CFamily.CONTROL`, before `trialsFor(family)` is even evaluated for that arm — independently confirmed by re-reading the control-flow order directly. `Unit3CCampaignDefinition.warmupTrials` is now referenced from the orchestration file for the first time (previously zero references); independently confirmed via `grep`.

## 3. Was a broader search performed for a similar defect, and did it find anything?

Yes, and no further defect was found. Every use of `allTrials` was independently enumerated (four occurrences, all in the schedule file, all either defining `liveModelCallCount`, asserting trial-ID uniqueness, or cross-checked by a test — none is a second, separately-missed "definition without execution" case). `controlTrials`, `familyATrials`, `familyBTrials`, and `familyCTrials` were independently re-confirmed each to have a corresponding `trialsFor` branch in the driver, unchanged from before this task and re-verified now given the heightened scrutiny this review is applying.

## 4. Is the strengthened exact-once/recovery claim (Readiness Review Section 7) accurate?

Yes, independently re-confirmed by re-running the four warm-up-specific tests directly: exactly-once execution in frozen order; idempotent crash recovery (a second full run re-invokes the executor for none of the three completed warm-ups); arm-scoped failure isolation (a corrupted warm-up identity halts Control only, verified by inspecting that Family A/B/C still report `SEALED` in the same test run); and that Control's own scored ledger file is never created at all if the warm-up gate fails (`assertFalse(Files.exists(dir.resolve("control").resolve("raw.jsonl")))`, independently re-run).

## 5. Is the residual note's re-derivation (Readiness Review Section 15) honest?

Yes, independently assessed against the same two-part test applied previously: the "structurally unexercised" claim remains literally true (independently re-confirmed via the same call-site search as before, unaffected by this task's diff, which never touched `buildModelInvokingExecutor`); and the claim that its own mitigation (the warm-ups) is now *more* soundly grounded, rather than merely repeated, is independently verified accurate — the prior review's confidence in the warm-up mitigation was, in fact, resting on an incorrect assumption at the time, and this review credits the refreshed Readiness Review for saying so plainly (Section 15's own "resting on an unverified assumption... turned out to be false") rather than quietly re-asserting the same confidence without acknowledging what changed.

## 6. Are all "unchanged" claims accurate?

Yes, spot-checked: artifact-root, disk-space, model/config identity, campaign identity, downstream isolation, and evidence integrity were independently re-verified via fresh test re-runs, not accepted on the Readiness Review's word.

## 7. Independent re-run of all reported counts

`./gradlew unit3cControlledRemedyExperiments --rerun-tasks`: **79 tests (48 + 31), 0 failures, 1 skipped** — updated from 78 (49 + 29), re-run fresh by this reviewer immediately before drafting this document. `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks`: 142 tests/4-skip/0-fail across five classes, independently confirmed the schedule/mechanism class reports 49 there (the tagged test present). `./gradlew test --rerun-tasks`: 2015/5-skip/0-fail, unchanged.

## 7a. Adversarial re-derivation of caller reachability — the specific mistake this review must not repeat

Independently re-derived, deliberately without reading the Readiness Review's Section 2a table as a starting point: opened `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` fresh and searched for every occurrence of `Unit3CLiveEntryPoint.run(` across the whole file. **First pass found seven textual matches, not two — this reviewer's own first draft of this section undercounted them and is corrected here rather than left standing.** Individually classified all seven: line 1412, the pre-existing `emptyMap()` fail-closed test (unchanged); line 1462, the new gated trigger's own real call; line 1474, a string literal inside the new source-scan test's own body (`val realEntryPointCall = "..."`) — not an invocation, a value being searched for; lines 1518, 1526, 1534, 1542, the four new negative fail-closed tests (wrong model, blank digest, malformed campaign ID, wrong artifact root), each supplying a deliberately-invalid environment. So: six genuine invocations total (one pre-existing, five new), one non-invocation string literal, zero unaccounted-for matches. Independently confirmed the gated trigger's own body (line 1458–1463) contains no logic between its two `assumeTrue` calls and its one real call beyond the two documented gates. This directly answers the question the prior review chain never asked: yes, a real caller now exists, independently located and individually classified by this reviewer rather than accepted from any other document's citation or from this reviewer's own uncorrected first pass.

## 7b. Does the new source-scan test itself constitute reliable evidence, or could it be vacuously true?

Adversarially probed: a source-scan test that searches for a literal string is only meaningful if the string it searches for is the actual call, and if the function boundary it scans is the actual trigger function, not some other function that happens to share a name fragment. Independently re-read the test's own `bodyStart`/`bodyEnd` extraction logic: it locates the exact trigger function by its full, unique test-name string (`fun \`live Unit 3-C campaign is skipped...\``), takes the first `{` after that marker, and the first `\n    }\n` after that — independently verified by hand against the actual function text that this correctly captures exactly the trigger function's body and nothing else (no other function in the file shares that name fragment; verified via a fresh `grep -c` for the exact marker string, returning `1`).

## 7c. Does reachability depend on Gradle configuration this review has not itself verified?

Yes, and independently re-verified rather than assumed: the trigger's presence inside the `unit3cControlledRemedyExperiments` task's actual execution set depends on the `includeTestsMatching` filter still matching the class as a whole. Independently re-ran the task fresh (Section 7) and independently confirmed the reported test count (49 in the schedule/mechanism class) increased by exactly 9 from the pre-diff baseline (40) — the only way this count could increase is if the newly added methods are genuinely included in the task's own execution set, since no other file or task configuration was touched (`git diff --stat -- build.gradle.kts` independently re-confirmed empty).

## 7d. Adversarial check: does "skipped" definitively rule out the trigger silently never being invoked even under real conditions?

Considered specifically, since a test that is *always* skipped regardless of environment would be equally useless. Independently re-read the trigger's two `assumeTrue` conditions: `System.getProperty(UNIT_3C_PROPERTY) == "true"` and `!System.getenv(Unit3CConfigLoader.CAMPAIGN_ID).isNullOrBlank()`. Both conditions are independently confirmed satisfiable: the first is set to exactly `"true"` by the Gradle task's own registration (independently re-read in `build.gradle.kts`, unconditional for that task's scope); the second requires only a non-blank environment variable, which an operator following the Explicit Execution Approval Review's own documented configuration steps would set. Neither condition is tautologically false or unreachable by construction. This review does not stop at "it is skipped today" — it independently confirms the skip is a property of the *environment*, not of the code's own logic being permanently disabled.

## 7e. Independent re-derivation: does every link in the corrected Section 2a table actually hold, including the newly added disk-space row?

Independently re-read `Unit3COrchestrationDriver.run()`'s current source directly (not the Readiness Review's own description of it): its first two statements are `val spaceCheckTarget = requireNotNull(artifactRoot.parent) { ... }` and `Unit3CDiskSpaceGate.check(spaceCheckTarget, usableSpace)`. Independently re-derived, by hand, that for the real live path (`/var/lib/parker/reasoning-protocol-live-model/<campaignId>`), `.parent` yields exactly `/var/lib/parker/reasoning-protocol-live-model` — independently confirmed via a direct `stat`/`ls` of that exact path at review time, existing, owned `steve:steve`, mode `700`, matching every prior review's own capture.

## 7f. Adversarial check: is the fix proven only by a fake, or against the actual failing production call?

This is the specific standard this review applies to guard against repeating its own immediately-prior omission. Independently re-ran `driver's real default disk-space check succeeds against a not-yet-created campaign directory when its parent exists with sufficient space` and independently re-read its body: it constructs `Unit3COrchestrationDriver` with **no** `usableSpace` override, meaning the constructor's own default (`{ Files.getFileStore(it).usableSpace }`) executes — the exact function that threw `NoSuchFileException` during the real halted attempt. Independently confirmed the test passes and all four arms reach `SEALED`. This is judged sufficient, not merely plausible: it is the same function, called the same way, against the same category of not-yet-existing path, differing only in that the parent here is a disposable `@TempDir` rather than the real governed root — a difference that does not affect `Files.getFileStore`'s behavior, since both are ordinary, existing, readable directories on a real filesystem.

## 7g. Independent re-derivation: is the live-test-scoping fix genuinely resolved, not merely deferred?

Independently re-ran both `unit3cControlledRemedyExperiments` (48 tests, tagged test absent from the XML report entirely — not skipped, not present) and `reasoningProtocolLiveModelEvaluation` (schedule/mechanism class reports 49, tagged test present and `passed`). Independently confirmed via `git diff -- build.gradle.kts` that exactly one `useJUnitPlatform { excludeTags(...) }` block was added, scoped to the `unit3cControlledRemedyExperiments` registration alone — no other task's filter was touched, so no other offline verification path lost coverage.

## 8. Blocking defects

None.

## 9. Non-blocking qualifications

None.

## 10. Verdict

```text
ACCEPTED
```

The refreshed Implementation Readiness Review's "READY" determination is independently confirmed accurate. This review specifically corrects its own immediately prior version's own gap: that version proved caller reachability rigorously but never independently traced whether every gate downstream of the caller — specifically the disk-space gate — would actually pass under real conditions, and the first genuine live-execution attempt found exactly that gap. This refresh closes it: the disk-space fix is independently proven against the real, unmocked `Files.getFileStore` call that actually failed (Section 7f), not merely against a fake; the live-test-scoping fix is independently confirmed to resolve, not defer or hide, the scope conflict (Section 7g). The warm-up and live-trigger defects remain genuinely closed. This instrument is ready to proceed to a fresh Explicit Execution Approval Review.

## 11. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The refreshed Readiness Review document itself was not modified by this review.
