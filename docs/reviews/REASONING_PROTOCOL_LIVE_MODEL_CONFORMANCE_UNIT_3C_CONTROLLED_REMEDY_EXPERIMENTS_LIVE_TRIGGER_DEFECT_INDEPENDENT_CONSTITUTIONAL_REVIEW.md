**Status:** Independent Constitutional Review of the Unit 3-C Live Trigger Defect Confirmation Review and its correction — **ACCEPTED.** A fresh, independently adversarial review of the implemented fix, not a ratification of the Defect Confirmation Review's own prose. No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred.

# Unit 3-C Controlled Remedy Experiments — Live Trigger Defect Independent Constitutional Review

## 1. Method

Independently re-read the entire diff (`git diff -- tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`) line by line, not the Defect Confirmation Review's own description of it. Independently re-ran `./gradlew unit3cControlledRemedyExperiments --rerun-tasks`, `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks`, and `./gradlew test --rerun-tasks`, parsing the XML reports directly rather than trusting console tails. Independently re-derived, from the diff alone, whether the correction could plausibly have introduced a live call, a campaign directory write, a schedule change, or a governance-document conflict.

## 2. Defect classification — independently re-derived

Confirmed accurate: a structural omission (missing caller), not a logic defect. The diff adds exactly nine new `@Test` functions and one `private fun completeUnit3CEnvironment()` helper to the existing test class, plus a doc-comment correction on `Unit3CLiveEntryPoint` itself (from "never invoked by any test" — now false — to an accurate description of the gated trigger). No existing function body, other than the doc comment, is modified.

## 3. Root cause — independently re-derived, not merely re-stated

Confirmed: every one of the six prior governance documents in this chain (Completion Review, Completion ICR, both prior Implementation Readiness Reviews, both prior Explicit Execution Approval Reviews) verified `Unit3CLiveEntryPoint.run`'s own internal gate ordering and never asked whether any caller reached it under real conditions. Independently spot-checked this claim against the Completion ICR's own Section 7 ("Did any live execution occur during this task? ... `Unit3CLiveEntryPoint` itself is untouched by this task's diff") and the first Readiness ICR — both examine the function's own correctness, never its reachability. The claim holds.

## 4. Correction scope — independently re-verified against the diff, not the plan

`git diff --stat` shows exactly one file changed: `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, +123/-4. `git diff --stat -- src/` and `git diff --stat -- build.gradle.kts` are both independently confirmed empty (zero output). The correction is confined entirely to the `liveModelEvaluation` source set's own test file, exactly as Phase 3's Question D concluded — independently re-confirmed here, not merely re-quoted.

## 5. Is the fix itself minimal, or does it exceed the narrowest correction?

**Independently scrutinized, found minimal and appropriately scoped — with one deliberate judgment call worth recording.** The Defect Confirmation Review's own Section 5 concluded the correction should be exactly one gated `@Test` calling `Unit3CLiveEntryPoint.run(System.getenv(), Path.of("."))`. The actual diff adds that one function plus eight additional tests. This reviewer independently assessed whether the eight additional tests exceed "the minimum correction necessary" (Phase 4's own wording): they do not modify or extend `Unit3CLiveEntryPoint`, `Unit3CConfigLoader`, or `Unit3CArtifactRootPolicy` in any way — every one of the eight is a read-only exercise of code that already existed before this task, added because Phase 5 of the governing task explicitly enumerates thirteen offline-verification requirements (fail-closed on missing/malformed config, wrong model, wrong root; reachability proofs) that a single trigger test cannot, by itself, satisfy. Classified as required verification work, not scope creep.

## 6. Production isolation

Independently re-confirmed: zero bytes changed under `src/`. `Unit3CLiveEntryPoint.run`'s own body is byte-for-byte unchanged (only its doc comment changed); `Unit3CConfigLoader`, `Unit3CArtifactRootPolicy`, `Unit3COrchestrationDriver`, `Unit3CArmLedger`, and every fixture/mechanism class are unchanged (independently confirmed: none appears in the diff hunks at all).

## 7. Governance conformity

Independently re-checked Phase 4's fifteen-item constraint list against the diff:

| # | Constraint | Independently verified |
|---|---|---|
| 1 | test/integration tier only | Yes — same file, same source set |
| 2 | detached from ordinary lifecycle | Yes — `./gradlew test` result (2015/5-skip/0-fail) independently confirmed to contain zero Unit 3-C test-result files |
| 3 | requires explicit live-execution enablement | Yes — two `assumeTrue` gates, independently re-read |
| 4 | sources config from approved env/property interface | Yes — `System.getenv()`, `Unit3CConfigLoader.CAMPAIGN_ID`, `UNIT_3C_PROPERTY` — all pre-existing constants, none new |
| 5 | fails closed when config absent | Yes — independently re-ran; trigger test result is `skipped`, not `passed`, confirming `assumeTrue` short-circuits before any config load |
| 6 | invokes `run` exactly once | Yes — independently re-derived via the new source-scan test's own regex count (`= 1`), itself independently re-read line by line, not merely trusted |
| 7 | uses existing governed driver | Yes — `Unit3CLiveEntryPoint.run` itself unchanged |
| 8 | preserves exact-once semantics | Yes — `Unit3CArmLedger` untouched |
| 9 | preserves safety checkpoint | Yes — `Unit3CSafetyCheckpoint` untouched |
| 10 | preserves artifact-root restriction | Yes — `Unit3CArtifactRootPolicy` untouched; independently re-confirmed via the new "wrong artifact root" test still throwing `Unit3CArtifactRootViolationException` |
| 11 | preserves disk-space gate | Yes — `Unit3CDiskSpaceGate` untouched, not referenced by the diff |
| 12 | preserves downstream isolation | Yes — independently re-ran the existing isolation tests; both pass |
| 13 | preserves frozen model/config | Yes — `UNIT_3C_MODEL_NAME`, `UNIT_3C_TIMEOUT_MS` untouched |
| 14 | preserves 483-call schedule | Yes — `liveModelCallCount == 483` check line unchanged, independently re-read at its current line number |
| 15 | zero live calls during offline testing | Yes — independently re-confirmed no `PARKER_REASONING_*` variable was set during any of this task's three test runs, and the trigger test's own result is `skipped` |

No violation found.

## 8. Execution isolation

Independently re-confirmed: `env | grep -i PARKER_REASONING` returned nothing before, during (checked between runs), and after this task's test executions. No campaign directory exists under `/var/lib/parker/reasoning-protocol-live-model/` beyond the two preserved Unit 2/Unit 2-D directories (independently re-listed). None of the new tests constructs an HTTP client, a `LocalHttpModelInferenceClient`, or any network resource — independently confirmed by reading all nine new test bodies: `completeUnit3CEnvironment`, `Unit3CConfigLoader.load`, and `Unit3CArtifactRootPolicy.resolve` are pure, no-I/O functions (independently re-read their full bodies to confirm no `Files.write`, `Files.createDirectories`, or `HttpClient` call appears in either).

## 9. Fail-closed behavior

Independently re-run and re-confirmed for each new negative test: wrong model name → `IllegalArgumentException` (from `require(live.modelName == UNIT_3C_MODEL_NAME)`); blank digest → `EvaluationConfigurationException`; malformed campaign ID → `IllegalArgumentException` (from the campaign-ID regex/marker `require`s inside `Unit3CConfigLoader.load`); wrong artifact root → `Unit3CArtifactRootViolationException`. All four independently re-traced to the exact `require`/`throw` statement each one exercises, not merely observed to fail.

## 10. Exact-once behavior

Independently re-confirmed via the source-scan test's own two assertions (call count `== 1`, `assumeTrue` count `== 2`), and independently re-read the trigger function's literal text a second time, separately from that test's own logic, to confirm the assertion is not vacuously true against a differently-shaped function.

## 11. Call-count invariance

Independently re-derived: 3 (warm-up) + 145 (Control) + 220 (Family A) + 115 (Family B) + 0 (Family C) = 483, unchanged; the check enforcing this (`Unit3CCampaignDefinition.init`) is untouched by the diff.

## 12. Model/config invariance

Independently re-confirmed unchanged: `UNIT_3C_MODEL_NAME = "qwen2.5-coder:7b"`, `UNIT_3C_TIMEOUT_MS = 30_000L`, `UNIT_3C_ARTIFACT_ROOT_PREFIX = "/var/lib/parker/reasoning-protocol-live-model"`, `FAMILY_B_CANDIDATE_SHA256 = "cfd5cb7f07d0d7da941b069e00b3f479fc5faf5e71662a83bda51f25bd629d60"` — none appears in the diff.

## 13. Downstream isolation

Independently re-ran the existing forbidden-import tests fresh: both pass. The new test bodies were independently searched for any of the denylisted symbols (`LocalHttpModelInferenceClient`, memory/goal/planner APIs): none found.

## 14. Did any frozen governance document require amendment?

**No, independently re-derived.** The Scope Lock and Implementation/Execution Plan both describe live execution as occurring "via the detached Gradle task, gated by explicit environment configuration" without specifying the test-tier wiring mechanism; the correction fills exactly that unspecified implementation detail, the same category of gap the original implementation task filled for every other piece of wiring in this instrument. No fixture, mechanism, schedule, or evidence-tier definition is touched.

## 15. Adversarial check: does the fix's own verification tests contain a false-positive risk (i.e., could they pass even if the trigger did not actually work)?

Specifically probed, since this is exactly the failure mode this review chain has twice previously missed (Family C's trace and the warm-up wiring). Independently re-verified the trigger test's own live path is *not* merely simulated: `Unit3CLiveEntryPoint.run(System.getenv(), Path.of("."))` is the literal call, not a stand-in or fake; when this task's own test runs (`unit3cControlledRemedyExperiments`, property always `true` for that task's scope, campaign ID always absent), the second `assumeTrue` genuinely short-circuits — independently confirmed by checking the JUnit XML result for this specific test method, which reports `skipped`, a status JUnit only reports when `Assumptions.assumeTrue` actually throws `TestAbortedException`, not a status a normal `passed` test could produce by coincidence. This rules out the false-positive risk: the test cannot report `skipped` unless the `assumeTrue` gate was genuinely reached and genuinely failed on the absent campaign ID.

## 16. Blocking defects

None.

## 17. Non-blocking qualifications

One, carried forward accurately rather than newly discovered: as in every prior review in this chain, the real, live-calling HTTP path itself (`buildModelInvokingExecutor`'s actual network call) remains structurally unexercised by any test, because no test in a no-live-calls-constrained task can exercise it. This is unchanged by, and not newly introduced by, this correction — the correction adds a genuine *caller* of the already-existing, already-reviewed live path; it does not and cannot itself prove the network call succeeds, which was never its purpose.

## 18. Verdict

```text
ACCEPTED
```

The correction is independently confirmed minimal, correctly scoped to test/integration tier, free of any production/fixture/mechanism/schedule/config change, genuinely fail-closed on every governed negative case, genuinely reachable exactly once when both gates pass, and genuinely inert (skipped, not merely non-executing by chance) under every offline test run performed during this task. Proceed to refreshing the Completion Review, Completion ICR, Readiness Review, and Readiness ICR.

## 19. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The Defect Confirmation Review document itself was not modified by this review.
