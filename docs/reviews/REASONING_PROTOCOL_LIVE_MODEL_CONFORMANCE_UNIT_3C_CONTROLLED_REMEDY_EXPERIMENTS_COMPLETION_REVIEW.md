**Status:** Unit 3-C Controlled Remedy Experiments — Completion Review — **REFRESHED (sixth refresh). PASS.** Implementation against committed baseline `4430e8b7eeb281d936ca3266fff13df7ed6bcc87` (the Unit 3-C Evidence Completeness and Durability Determination's own completeness classification, committed). This refresh corrects the durable observation-encoding defect the Determination found: `encodeObservation` persisted only 3 of ~26 already-governed, already-computed fields. Sections 1–24 below are preserved from the fifth refresh as the historical record of what passed before this defect was found (Section 0 explains why that record stands); Section 0 and Sections 25–31 are new to this refresh. No live model call, no HTTP call, no live campaign directory created or mutated, no production (`src/`) change, no remedy selected, no Attempt 5 artifact touched.

# Unit 3-C Controlled Remedy Experiments — Completion Review

## 0. Sixth refresh — observation-durability correction (new)

**What this refresh adds, and why the fifth refresh's own PASS is not erased.** The Unit 3-C Evidence Completeness and Durability Determination Review (`0a4823b`) and its Independent Constitutional Review (`4430e8b`) independently found that `encodeObservation` — the function responsible for turning a fully, correctly populated in-memory `Unit3CObservation` into the durable `raw.jsonl` record — persisted only `campaignId`, `family`, and `fixtureId`, discarding `actualAction`, `semanticCorrect`, `representationValid`, `parserResult`, `parserFailure`, `latencyNanos`, `transportOutcome`, model/digest/endpoint/timeout/inference-config identity, `promptIdentity`, `candidateMechanismIdentity`, `stableInputHash`, and `repositoryCommit` at the moment of persistence, even though every one of those fields was already correctly computed in memory. **Every prior Completion Review (first through fifth refresh) genuinely, correctly verified `raw.jsonl`'s own existence, trial-ID correctness, line counts, and exact-once semantics — and none of those checks were wrong.** What none of them did, independently confirmed by a fresh search of this file's own test history performed for the Determination Review, was assert the *payload content* of a `raw.jsonl` record against Plan Section 16's own field-by-field schema. That is a distinct, also-governed property this program had not yet tested for, not a case of an existing test giving a wrong answer. The fifth refresh's own PASS verdict (Sections 1–24 below) remains historically accurate for everything it actually checked; this refresh does not retroactively invalidate it, and does not delete or rewrite it. The Observation Durability Defect Confirmation Review (`docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3C_OBSERVATION_DURABILITY_DEFECT_CONFIRMATION_REVIEW.md`) and its own Independent Constitutional Review are this refresh's controlling authority; see Sections 25–31 below for what changed and how it was verified.

## 1. Defect and correction history (consolidated, preserved from prior refreshes)

Five distinct defects have been discovered and corrected across this programme's history, each independently confirmed, each passed through its own full review chain before this refresh: (1) the Family C trace prediction error (Plan corrected, mechanism unchanged); (2) the warm-up orchestration defect (warm-ups counted in the schedule but never executed; corrected by wiring `runWarmups` into `runArm`); (3) the live-execution trigger defect (`Unit3CLiveEntryPoint.run` existed but nothing called it with real configuration; corrected by a gated live-trigger test); (4) the disk-space-gate and live-test-scoping defects (the gate checked a not-yet-created path; a verification test was unconditionally selected by the live task; both corrected); (5) the timeout value (30,000 ms) was empirically too short and the durability model lacked intent-before-call recording, both discovered by the first genuine live-execution attempt (Attempt 3, `db9c612`), which reached a real `/api/generate` call for the first time, timed out, and left no durable record of having been attempted beyond an ephemeral build log; corrected by the fifth refresh below. **(6) new, corrected by this sixth refresh (Section 0 above, Sections 25–31 below):** the observation-durability defect — `encodeObservation` discarded ~23 of ~26 already-governed, already-computed fields at the point of durable persistence, discovered not by a live-execution attempt but by the Evidence Completeness and Durability Determination's own forensic re-trace of Attempt 5's genuine live evidence.

## 2. This task's own scope (fifth-refresh implementation; superseded as "this task" by Section 25 for the sixth refresh)

Implements, for the first time, the governance frozen by three prior tasks: the Timeout + Durability Scope Lock Amendment, the Timeout + Durability Implementation Plan Amendment, and the Scored-Trial Timeout Semantics Determination (all committed, `0072c01`/`144cf51`). No new governance is created or amended by this task; only implementation.

## 3. Planning Review (re-derived, not assumed)

Independently traced the complete call path before editing: Gradle detached task → gated live trigger → `Unit3CLiveEntryPoint.run` → `Unit3CConfigLoader.load`/`Unit3CArtifactRootPolicy.resolve` → `Unit3CDiskSpaceGate.check` → `Unit3COrchestrationDriver.run` → `runWarmups`/`runArm` → `Unit3CTrialExecutor.execute` (the real, live-calling implementation inside `buildModelInvokingExecutor`) → `ModelReasoningProvider.reason` (`withTimeout(timeoutMs) { ... }`) → `LocalHttpModelInferenceClient.infer` → terminal result. Identified: `UNIT_3C_TIMEOUT_MS` (single private constant) as the sole owner of the governed timeout value; no per-trial intent record existed anywhere before this task; transmission occurred inside `buildModelInvokingExecutor`'s own `runBlocking { provider.reason(...) }` call; the only exception boundary in that function caught `UnclassifiableModelResponseException` alone — `TimeoutCancellationException` and any `IOException` propagated uncaught, exactly reproducing Attempt 3's own crash; warm-up failures (`Unit3CArtifactIntegrityException` only) halted the Control arm alone, with Family A/B/C proceeding independently via `driver.run()`'s own unconditional `UNIT_3C_ARM_ORDER.map { ... }`; scored-trial failures had no distinct transport-failure handling at all; the ledger's own `recover()` had no concept of a "resolved but not completed" trial state.

## 4. Boundary Review

**NOT REQUIRED.** No `src/**` file is touched anywhere in this task's diff (`git diff --stat -- src/` independently re-confirmed empty). Every production class the implementation touches (`ModelReasoningProvider`, `LocalHttpModelInferenceClient`, `TaggedReasoningResponseParser`, `DefaultReasoningPromptBuilder`) is used exactly as it already was; only the *set of exception types the test-tier executor wrapper catches* around the pre-existing `provider.reason(...)` call changed, entirely within `tests/integration/`.

## 5. Files changed

- `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`: `UNIT_3C_TIMEOUT_MS` changed `30_000L` → `90_000L` with an evidence-citing comment; two stale active-path `"30000"` string fixtures corrected to `"90000"`; the `model and timeout constants match the...Plan` test corrected to assert `90_000L`; `buildModelInvokingExecutor`'s single catch clause extended to three new, ordered catch clauses (`TimeoutCancellationException` → `MODEL_TIMEOUT`; `IOException` → `TRANSPORT_OR_PROVIDER_FAILURE`; `Exception` → `AMBIGUOUS`), each throwing `Unit3CTransportException` instead of fabricating an observation.
- `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt`: new types `Unit3CTransportClassification`, `Unit3CTransportException`, `Unit3CIntentRecord`, `Unit3CTimeoutRecord`, helper functions `promptIdentityFor`/`buildIntentRecord`/`buildTimeoutRecord`; two new `Unit3CArmOutcome` values (`CAMPAIGN_HALT_WARMUP_TRANSPORT`, `NOT_ATTEMPTED_CAMPAIGN_HALTED`); `Unit3COrchestrationDriver.run()` changed from an unconditional arm-order map to a sequential, early-exiting loop; `runWarmups` and `runArm` both extended to call `ledger.recordIntent(...)` before every live call and to catch `Unit3CTransportException` with governed classification-specific handling; `Unit3CArmLedger` (defined in the OTHER file, `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`) extended with `recordIntent`/`recordTimeout`/`hasIntent`/`hasTimeout` and a four-state-aware `recover()`.
- **Not touched:** `build.gradle.kts`; any frozen governance document; any campaign artifact directory.

## 6. Timeout implementation — 90,000 ms executable value

`UNIT_3C_TIMEOUT_MS = 90_000L`, independently re-verified as the current, active value read by `Unit3CConfigLoader.load`'s own `require(live.timeoutMs == UNIT_3C_TIMEOUT_MS)` check, which every real or fixture-driven configuration must satisfy.

## 7. Stale 30-second executable-path search — result

Exhaustively searched (`grep -n "30_000L\|30000"` across both implementation files). Found and corrected: the governed constant itself; two `completeUnit3CEnvironment()`/config-rejection-test fixture strings that would otherwise have caused the `campaign ID missing the required marker` test to exercise the *wrong* failure mode (timeout mismatch instead of campaign-ID rejection) once the constant changed; one active assertion (`model and timeout constants match the...Plan`) that directly checked the stale value. **Not changed, correctly:** three arbitrary fake-identity fixtures (`sampleIdentity()`, `fakeObservationFor`, two ledger-identity-drift test fixtures) that use `30_000L` as an unrelated placeholder value, never cross-checked against the governed constant anywhere — changing them would be cosmetic only and was judged out of the narrowest necessary scope. **Historical evidence, correctly left untouched:** the Execution Evidence Review, Investigation Review, and both prior Amendment documents, which describe the *original, superseded* 30,000 ms timeout as a historical fact — none of these was rewritten.

## 8. Intent-before-call result

Implemented via `Unit3CArmLedger.recordIntent`, called from `runWarmups`/`runArm` immediately before `executor.execute(trial)`, for every trial with `makesModelCall == true` (Family C, which makes none, is correctly and structurally exempt — independently re-verified via a dedicated test showing no `intent.jsonl` is ever created for it). Ordering is proven two ways: (1) direct code reading — `recordIntent` and `.execute(trial)` are sequential statements with no intervening try/catch that could swallow a `recordIntent` failure and still proceed; (2) a new source-scan test independently confirms, for both `runWarmups` and `runArm`, that `ledger.recordIntent(` appears in the source text strictly before `.execute(trial)` within each function's own body. Written with `StandardOpenOption.SYNC` (genuinely fsynced), deliberately stricter than `appendObservation`'s own pre-existing, unmodified buffered-write durability level, matching the governed "written and fsynced" requirement specifically for intent records.

## 9. Terminal timeout durability result

`Unit3CTimeoutRecord` is written for every call that raises `Unit3CTransportException`, containing call/campaign/family/fixture/trial identity, elapsed duration, the governed 90,000 ms ceiling, terminal classification, `responseBytesReceived` (honestly hardcoded `false`, with a comment explaining the client's own inability to observe partial bytes — not guessed at), transport detail, model/config identity, and the continuation/checkpoint decision actually taken. The type has no field capable of holding a parser result or semantic classification at all — independently verified via reflection over `Unit3CTimeoutRecord::class.java.declaredFields` in a dedicated test, not merely by code inspection.

## 10. Exact-once four-state result

`Unit3CArmLedger.recover()` now independently tracks intent IDs, raw (completed) IDs, and timeout IDs, and fails closed (`Unit3CArtifactIntegrityException`) if any intent ID has neither a raw nor a timeout resolution (state D). States B and C are both included in the returned "do not retry" set. Independently re-verified via a dedicated crash-recovery test: a second, independent driver instance run over an already-timed-out trial invokes the executor zero additional times. State A remains distinguishable via `hasIntent(id) == false`, independently re-verified via a dedicated test. **Backward compatibility independently re-verified**: four pre-existing standalone ledger tests (`ledger rejects continuation after seal`, `seal fails closed when the registered trial set is incomplete`, `exact observation and call accounting...`, `manifest hash integrity...`) call `appendObservation` directly without ever calling `recordIntent` first — this is deliberately still permitted at the ledger's own generic API level (no intent-existence check was added to `appendObservation` itself, since doing so would have broken these four already-governed, general-purpose tests); the intent-before-call *policy* is enforced at the orchestration call-site level (Section 8), not inside the ledger's own low-level primitive, and `recover()`'s new ambiguous check is a no-op whenever no intent file exists at all, so these four tests are independently confirmed unaffected.

## 11. Warm-up timeout result

A `Unit3CTransportException` of any classification (model timeout, transport/provider failure, or ambiguous) raised during any of the three warm-up trials now causes `runWarmups` to durably record the timeout and return the new `Unit3CArmOutcome.CAMPAIGN_HALT_WARMUP_TRANSPORT` outcome. `Unit3COrchestrationDriver.run()`'s own loop, independently re-read, detects exactly this outcome and marks every subsequent arm `NOT_ATTEMPTED_CAMPAIGN_HALTED` without invoking their executors at all — independently re-verified via three dedicated tests (one per classification), each confirming Family A's own executor is never called. Never classified as remedy-performance evidence (no `Unit3CObservation` is ever constructed in this path). No automatic retry. Campaign state (Control's own scored `raw.jsonl`) is independently re-confirmed never created when warm-up halts the campaign.

## 12. Scored-trial timeout result

A `Unit3CTransportException` raised during a scored Control/Family A/Family B trial is handled per its classification: `MODEL_TIMEOUT` durably records the timeout, marks the trial resolved (never retried), and the loop `continue`s — independently re-verified for all three arms via three dedicated tests, each confirming the arm still seals with exactly one fewer raw observation and exactly one timeout record. `TRANSPORT_OR_PROVIDER_FAILURE`/`AMBIGUOUS` durably record the timeout, then re-throw as `Unit3CArtifactIntegrityException`, reusing the arm's own existing `HALTED` handling — independently re-verified to halt only the affected arm, with every other arm still sealing (matching the pre-existing, unmodified isolation pattern for every other measurement-invalidating cause). No fabricated `GOAL`/`REPLY`/`REMEMBER`/`NOACTION` in any case — independently re-verified via string-level assertions against the durable timeout record and via the type-level field-absence proof (Section 9).

## 13. Transport/infrastructure distinction

Classification is derived from the actual exception type the JVM/coroutines runtime raises, never a message string — independently re-read: `kotlinx.coroutines.TimeoutCancellationException` (specific, caught first) → `MODEL_TIMEOUT`; `java.io.IOException` (broader, catches `ConnectException`/`HttpConnectTimeoutException`) → `TRANSPORT_OR_PROVIDER_FAILURE`; any other `Exception` → `AMBIGUOUS`. This is the exact reasoning the Scored-Trial Timeout Semantics Determination's own Section 4 froze, honestly implemented with the acknowledged limitation that sub-cases 2 (transport) and 3 (provider unavailable) are not distinguishable from each other at this layer and are therefore represented by one combined classification value, exactly as the Determination's own accepted reasoning anticipated for a loopback-only endpoint.

## 14. Attempt 3 preservation

Independently re-hashed `control/warmup/identity.txt` under `/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810/` before and after this task's own implementation and test runs: `56af7ca3fa84b1e3c6aca3d4fdd2a23f5884cc5a0ff5a7b574fe2d663d62c9c8`, byte-for-byte unchanged. No file was added, removed, or modified under that campaign directory. No new campaign was created; no campaign ID was reused.

## 15. Family A/B/C invariance

Independently re-confirmed unchanged: `FamilyADecisionPromptBuilder`, `FamilyARenderingPromptBuilder`, `FamilyBCandidatePromptBuilder`/`FAMILY_B_CANDIDATE_SELECTION_GUIDANCE`/its SHA-256, `Unit3CCandidateC1`'s five-step mechanism — none appears in this task's diff at all. Family C's own executor (`buildFamilyCExecutor`) is untouched and never raises `Unit3CTransportException` (it makes no model call, by construction).

## 16. 483-call schedule invariance

`Unit3CCampaignDefinition` (fixtures, repetitions, `liveModelCallCount == 483`) is untouched by this task's diff — independently re-confirmed via a dedicated new test re-asserting the full breakdown (3/145/220/115/29) and via `git diff` showing no hunk touching `Unit3CCampaignDefinition`'s own definition.

## 17. Targeted tests

**95 tests, 0 failures, 0 errors, 1 skipped** (`./gradlew unit3cControlledRemedyExperiments --rerun-tasks`) — 48 in the schedule/mechanism file (unchanged count from the prior refresh; the timeout-catching change is internal to an existing function, not a new test) plus one corrected assertion, 47 in the orchestration file (31 prior + 16 new, covering all timeout/durability governance dimensions). All 79 pre-existing tests continue to pass unchanged, on the very first run after implementation, with no test correction required.

## 18. Unit 1/2/2-D regression

16/1-skip/0-fail; 19/1-skip/0-fail; 27/1-skip/0-fail — all unchanged from every prior baseline.

## 19. Full ordinary repository result

`./gradlew test`: 2015 tests, 5 skipped, 0 failures, 0 errors — unchanged.

## 20. Downstream isolation

Unaffected; independently re-confirmed via the existing forbidden-import tests (unmodified) and a fresh read of every new line added by this task: no Memory/Goal/Planner/Knowledge-Submission symbol appears anywhere in the new code.

## 21. Lifecycle isolation

Unaffected; `build.gradle.kts` is untouched by this task, so the detached task's own `unit3cLiveTaskIncompatible` tag exclusion and the structural `liveModelEvaluation` source-set exclusion from `test`/`check`/`build` both remain exactly as previously governed and independently re-confirmed (`./gradlew test` contains zero Unit 3-C result files).

## 22. Live model/HTTP calls made

**Zero.** No live environment variable was set at any point during this task.

## 23. Campaign directories created or mutated

**None.** Attempt 3's own preserved directory is unchanged (Section 14); no new campaign directory exists anywhere under the artifact root.

## 24. Blocking defects (fifth-refresh scope)

**None**, for the timeout/durability scope that refresh implemented. (The observation-durability defect, Section 0/25–31, had not yet been found when the fifth refresh was written; it is not a retroactive blocking defect against that refresh's own, narrower scope — it is a new, separately-scoped correction.)

## 25. Sixth refresh — this task's own scope

Implements the sole corrective task identified by the Unit 3-C Evidence Completeness and Durability Determination Review (Section 13 there): correct `encodeObservation` so the fields the accepted Plan Section 16 schema requires, and that `Unit3CObservation` already computes, survive into the durable `raw.jsonl` record. No new governance is created or amended by this task. `contentFidelity`'s own separate, pre-existing non-computation (hardcoded `null` regardless of whether a fixture defines `expectedContent`) is independently re-confirmed out of this task's scope — the Determination's own Independent Constitutional Review flagged it as a distinct, unresolved question, and this task does not compute it (see the Observation Durability Defect Confirmation Review, Section 5, for the full determination).

## 26. Observation-durability implementation path (re-traced)

`buildModelInvokingExecutor`/`buildFamilyCExecutor` (`ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, unmodified by this task) construct a fully-populated `Unit3CObservation`. `runWarmups`/`runArm` (`ReasoningProtocolUnit3COrchestrationTest.kt`) call `ledger.appendObservation(trial.id, encodeObservation(observation))`. `encodeObservation`, previously a private member of `Unit3COrchestrationDriver` serializing only `campaignId`/`family`/`fixtureId` as a pipe-delimited string, is now a top-level function in the same file serializing every governed field — `arm`, `fixtureCategory`, `contextProfileId`, `trialSequence`, `expectedAction`, `actualAction`, `semanticCorrect`, `representationValid`, `contentFidelity`, `modelName`, `modelDigest`, `runtimeIdentity`, `endpointIdentifier`, `timeoutMs`, `inferenceConfigIdentity`, `promptIdentity`, `parserResult`, `parserFailure`, `latencyNanos`, `transportOutcome`, `candidateMechanismIdentity`, `stableInputHash`, `repositoryCommit` — as a JSON-object-shaped string, excluding only `prompt`/`rawRequest`/`rawResponse` (per the Determination's own accepted minimum-evidence analysis). `Unit3CArmLedger.appendObservation`'s own outer format, `trialId` extraction, checkpoint timing, and duplicate-prevention logic are all untouched — only the string passed as `payload` changed.

## 27. Files changed (sixth refresh)

`tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` only: `encodeObservation` corrected and relocated to top level; new private helpers `jsonQuote`/`jsonUnquote`/`jsonStringField`/`jsonBooleanField`/`jsonLongField`/`payloadField`/`decodeObservationPayload`/`extractRawPayload`; five new tests. No other file in the repository is touched — independently re-confirmed via `git diff --stat`.

## 28. Boundary Review (sixth refresh)

**NOT REQUIRED.** `git diff --stat` confirms exactly one file changed, entirely under `tests/integration/`. No `src/**` change of any kind.

## 29. Durability verification (sixth refresh)

Five new tests prove round-trip durability: encode→decode without a filesystem; encode→write→read-from-disk→decode (with a regression guard against the old three-field format reappearing); a representation-failure case preserving null `actualAction`/`semanticCorrect` and the real `parserFailure` message; Family C's null-field nullability branch, with its `actualAction` now durably, directly readable rather than only deducible; and a full, real `Unit3COrchestrationDriver` run whose durable `raw.jsonl` is read from disk and shown to contain the real `actualAction` for the exact safety-checkpoint-triggering trial ID recorded in Attempt 5 (`control/g03-later-action/main/02`). Full detail: Observation Durability Defect Confirmation Review, Section 10.

## 30. Preserved-mechanism, Attempt 5 integrity, and test results (sixth refresh)

Independently re-verified unchanged: intent-before-call durability, terminal timeout durability, four-state exact-once semantics, warm-up/scored-trial timeout semantics, the transport/infrastructure distinction, no-automatic-retry, safety-checkpoint behavior (now additionally proven correct on the *durable* record, not merely the in-memory one), Family A/B/C mechanisms, and the frozen 483-call schedule — none of the files defining them appear in this task's diff. Attempt 5's own 26 files under `unit3c-remedy-experiments-20260810-02/` carry modification times that predate this task's session; none was read-write opened by any command this task issued. Test results: targeted Unit 3-C **100 tests, 1 skipped, 0 failures, 0 errors**; general offline harness (includes Unit 1) **163 tests, 4 skipped, 0 failures, 0 errors**; Unit 2 **19 tests, 1 skipped, 0 failures, 0 errors**; Unit 2-D **27 tests, 1 skipped, 0 failures, 0 errors**; full repository **2015 tests, 5 skipped, 0 failures, 0 errors**. Zero live environment variables set; zero `/api/generate` calls; zero HTTP calls.

## 31. Blocking defects (sixth refresh)

**None.** `contentFidelity`'s own separate non-computation remains open, correctly out of this task's scope, and is restated (not silently dropped) in Section 25 above.

## 32. Completion verdict

```text
PASS
```

Ready for Independent Constitutional Review of Completion.
