**Status:** Independent Constitutional Review of the Disk-Space and Live-Test-Scoping Defect Confirmation Review — **ACCEPTED.** A fresh, independently adversarial review of both defects and both corrections, not a ratification of the Confirmation Review's own prose. No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred.

# Unit 3-C Controlled Remedy Experiments — Disk-Space and Live-Test-Scoping Defect Independent Constitutional Review

## 1. Method

Independently re-read the full diff (`git diff -- build.gradle.kts tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt`) line by line. Independently re-ran `./gradlew unit3cControlledRemedyExperiments --rerun-tasks`, `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks`, and `./gradlew test --rerun-tasks`, parsing XML directly. Independently re-derived both root causes from source rather than accepting the Confirmation Review's own account.

## 2. Defect 1 — is the disk-space fix genuinely correct, not merely plausible?

Independently re-read `Unit3COrchestrationDriver.run()` post-fix: `val spaceCheckTarget = requireNotNull(artifactRoot.parent) { ... }; Unit3CDiskSpaceGate.check(spaceCheckTarget, usableSpace)`. Independently re-derived that `artifactRoot.parent`, for the real live path `/var/lib/parker/reasoning-protocol-live-model/<campaignId>`, is exactly `/var/lib/parker/reasoning-protocol-live-model` — the durable, already-governed, always-existing artifact root. This is not asserted from the Confirmation Review's own claim; it follows directly from `Path.getParent()`'s own well-defined semantics applied to the exact string `Unit3CArtifactRootPolicy.resolve` is proven (by its own existing, unaffected tests) to produce.

Independently re-ran the two new tests: `driver disk-space gate checks the durable parent, not the not-yet-created campaign directory itself` (asserts, inside the fake `usableSpace` lambda itself, that the checked path equals the `@TempDir` parent, not the non-existent child) and `driver's real default disk-space check succeeds against a not-yet-created campaign directory when its parent exists with sufficient space` (uses the real, unmocked `Files.getFileStore` default, against a genuinely non-existent child of a real, existing directory, and confirms all four arms reach `SEALED`). Both pass. The second test is judged the stronger proof: it does not merely assert what path *should* be checked, it demonstrates the *actual production code path* (`Files.getFileStore(...).usableSpace`, the exact function that failed during the real halted attempt) now succeeds under the exact failure condition previously observed.

## 3. Does the disk-space fix regress any existing coverage?

Independently re-checked all four standalone `Unit3CDiskSpaceGate.check` tests and the pre-existing `driver refuses to run at all when disk space is insufficient, before any executor is invoked` test: none asserts an exact path string in any exception message, and all four use either `Path.of("/tmp")` directly (unaffected by a driver-level change) or a fake `usableSpace` lambda that ignores its input path entirely. Independently confirmed via the fresh test run: all pass unchanged. No regression.

## 4. Defect 2 — does the tag-based fix genuinely solve the problem, or only hide it?

Independently probed the risk that "excluding from the live task" could be a way of silently disabling correct verification rather than genuinely resolving the scope conflict. Independently re-ran `reasoningProtocolLiveModelEvaluation --rerun-tasks` and independently confirmed, from its own fresh XML report, that `ReasoningProtocolUnit3CControlledRemedyExperimentsTest` reports **49** tests there (not 48) — meaning the tagged test is still selected and still runs under the general offline aggregate task, exercising exactly the same assertion, unweakened. Independently re-ran `unit3cControlledRemedyExperiments --rerun-tasks` and confirmed **48** tests there (one fewer) with no test-result file for the tagged test's name appearing in that task's own XML output. This is independently verified exclusion from one specific task, not deletion or weakening of the check itself.

## 5. Was the assertion's own semantic purpose preserved, as this task explicitly required?

Yes, independently confirmed: `git diff` for the test file shows the assertion's own body (`assertNull(System.getenv(Unit3CConfigLoader.CAMPAIGN_ID), "...")`) is byte-for-byte unchanged; only a `@Tag(...)` annotation line was added above the function, and a new named constant was added elsewhere in the file. The Confirmation Review's own stated preference for "filtering/tagging over changing the assertion's semantic purpose" is independently confirmed honored, not merely claimed.

## 6. The incidental Unit 1 regression — was it genuinely caused by this task's own change, and was the fix appropriately scoped?

Independently re-derived from first principles rather than accepting the Confirmation Review's account: extracted the exact regex from `ReasoningProtocolLiveModelConformanceTest.kt` (`tasks\.(test|check|build|assemble)[^{]*\{[^}]*reasoningProtocolLiveModelEvaluation`, `DOT_MATCHES_ALL`) and independently ran it against both the pre-fix and post-fix `build.gradle.kts` text using a standalone script, not the JUnit test itself. Confirmed: it matches the pre-fix (first comment) text, starting from an unrelated, pre-existing `shouldRunAfter(tasks.test)` reference and scanning forward — through the `unit3cControlledRemedyExperiments` block's own opening brace and comment text — until it reaches the literal substring in the comment. Confirmed it does not match the post-fix (reworded comment) text. This independently reproduces the Confirmation Review's own finding by an independent method (a fresh Python re-implementation of the exact regex, not a re-run of the Kotlin test alone), and independently confirms the fix (rewording only, no change to the regex, the test, or any lifecycle task definition) is the narrowest possible correction — it does not touch the Unit 1 test at all, which remains an accurate, unweakened detector.

## 7. Independent re-run of all reported counts

`./gradlew unit3cControlledRemedyExperiments --rerun-tasks`: schedule/mechanism class 48/1-skip/0-fail, orchestration class 31/0-skip/0-fail = **79 tests, 1 skipped, 0 failures**. `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks`: Unit 1 16/1-skip/0-fail, Unit 2 19/1-skip/0-fail, Unit 2-D 27/1-skip/0-fail, Unit 3-C schedule/mechanism 49/1-skip/0-fail, Unit 3-C orchestration 31/0-skip/0-fail = **142 tests, 4 skipped, 0 failures**. `./gradlew test --rerun-tasks`: **2015 tests, 5 skipped, 0 failures, 0 errors**, independently re-confirmed to contain zero Unit 3-C or live-model-evaluation result files. All figures match the Confirmation Review's own reported figures exactly.

## 8. Governance boundary re-derivation

Independently spot-checked the highest-risk rows in the Confirmation Review's own Section 5 table: `UNIT_3C_MINIMUM_FREE_BYTES` — independently `grep`'d the diff, found it appears only as an unchanged reference inside the two new tests' own fake lambdas (`{ UNIT_3C_MINIMUM_FREE_BYTES + 1 }`), never redefined; `liveModelCallCount == 483` and `FAMILY_B_CANDIDATE_SHA256` — independently confirmed absent from both diffs entirely (not even as context, since the schedule/mechanism file's own diff hunk is confined to the imports and the new tag constant, far from those definitions). `git diff --stat -- src/` independently re-confirmed empty.

## 9. Blocking defects

None.

## 10. Non-blocking qualifications

None.

## 11. Verdict

```text
ACCEPTED
```

Both defects are independently confirmed to be genuine implementation defects (Classification A), independently confirmed correctly and minimally fixed, independently confirmed not to regress any existing coverage, independently confirmed not to alter any frozen governance property, and independently confirmed to have resolved — rather than merely relocated or hidden — the one incidental side effect (the Unit 1 regex false positive) discovered during verification. Proceed to refreshing the Completion Review, Completion ICR, Readiness Review, and Readiness ICR.

## 12. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The Defect Confirmation Review document itself was not modified by this review.
