**Status:** Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review (5) — **AUTHORIZED.** This document authorizes exactly one future campaign execution and does not itself execute it. Explicit Execution Approval Reviews 1–4 are preserved unmodified as the historical record of the warm-up, live-trigger, disk-space/live-test-scoping, and (implicitly, by Attempt 3's own halt) timeout defects each discovered in turn; this is a wholly fresh review, not an edit of any of them. It specifically supersedes the authority of Review 4, which was exercised (Attempt 3) and discovered the timeout/durability gap this review's own governing implementation now closes. Only read-only `/api/tags` and `/api/show`-class HTTP calls occurred during this review; no `/api/generate` call, no campaign directory creation, and no code/test/Gradle modification occurred.

# Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review (5)

## 1. Baseline

`HEAD` = `origin/main` = `e06f85c308094b52abf8aa3b77b376d795f1e01a`, clean, independently re-confirmed at task start. `git show --stat e06f85c` independently re-confirmed to contain exactly the timeout/durability implementation and its four refreshed governance documents (Completion Review, Completion ICR, Readiness Review, Readiness ICR) — nothing else.

## 2. Governance authority

Read fresh, this task: Unit 3-C Scope Lock; Implementation/Execution Plan; Timeout and Inference Latency Investigation Review and its ICR; Timeout + Durability Scope Lock Amendment and Implementation Plan Amendment and their joint ICR; Scored-Trial Timeout Semantics Determination and its ICR; the fifth-refresh Completion Review (`PASS`) and Completion ICR (`ACCEPTED`); the fifth-refresh Implementation Readiness Review (`READY`) and Readiness ICR (`ACCEPTED`); the committed implementation (both `tests/integration/` files, read directly, not from review prose); the full Execution Evidence Review history including Attempt 3. All findings below are independently re-derived from this fresh reading and from fresh source/test execution, not carried forward from any prior document's own claims without re-verification.

## 3. Campaign identity

**`unit3c-remedy-experiments-20260810-02`** — a fresh identity, deliberately distinct from Attempt 3's own `unit3c-remedy-experiments-20260810`. **Reuse of the Attempt 3 campaign ID is not authorized**, and independently confirmed to be not merely inadvisable but mechanically self-defeating: Attempt 3's own preserved `control/warmup/identity.txt` records `timeoutMs=30000` (the pre-amendment value); `Unit3CArmLedger.checkIdentity` would compare that recorded line against the current, governed `90000` value on any reuse attempt and immediately throw `Unit3CArtifactIntegrityException` ("identity drift detected") before any live call — independently re-verified by direct comparison of the recorded identity line against the current `UNIT_3C_TIMEOUT_MS` constant. The proposed fresh ID independently re-confirmed to satisfy `Unit3CArtifactRootPolicy`'s own campaign-ID regex and marker-prefix requirements, and its own directory independently re-confirmed absent under the artifact root.

## 4. Model / digest

Fresh read-only `/api/tags` call, performed for this review: `qwen2.5-coder:7b` installed, digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364` — identical to every prior review's own capture across this entire programme.

## 5. Runtime identity

Fresh checks: hostname `parker`; Ollama version `0.32.5` — unchanged.

## 6. 90-second timeout proof

Independently re-read `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt:57`: `private const val UNIT_3C_TIMEOUT_MS = 90_000L`. Independently re-read its sole consumer, `Unit3CConfigLoader.load`'s own `require(live.timeoutMs == UNIT_3C_TIMEOUT_MS)` — the active, executable gate every real or fixture configuration must satisfy. Independently re-searched for any remaining active `30000`/`30_000L` path: the only two `"30000"` string literals found were in two test-fixture environments, both independently confirmed already corrected to `"90000"` in the committed diff; the one active assertion checking this constant (`model and timeout constants match the amended Plan`) independently confirmed to assert `90_000L`, re-run fresh and passing. Historical documents (Investigation Review, Attempt 3's own evidence, both Amendment documents) still correctly describe the *original, superseded* 30,000 ms value as a historical fact — independently confirmed these are not, and must not be treated as, active configuration.

## 7. Intent-before-call proof

Independently re-traced actual executable ordering, not merely confirmed the function exists: `runWarmups` and `runArm`, both independently re-read in full, each call `ledger.recordIntent(buildIntentRecord(...))` as a synchronous statement immediately preceding `executor.execute(trial)`/`.execute(trial)`, with no intervening code capable of skipping the intent write while still reaching the call, and no catch clause positioned to swallow a `recordIntent` failure before `execute` runs. Independently re-ran the dedicated source-scan test (`intent is durable before the executor is ever invoked`), which independently re-confirms this textual ordering for both functions programmatically. Independently re-ran the intent-coverage test confirming exactly 145 intent records exist for Control's 145 scored trials (including one that times out in that test), and the Family-C-exemption test confirming zero intent records exist for Family C, which makes no model call. **No model request can be transmitted before durable intent exists — proven by call-graph tracing and by dedicated coverage tests, not asserted from the function's mere existence.**

## 8. Timeout terminal durability

Independently re-read `Unit3CTimeoutRecord`'s full field declaration and `buildTimeoutRecord`'s construction: call ID, campaign/family/fixture/trial identity, elapsed nanoseconds, the governed `timeoutMs` (90,000), terminal classification, `responseBytesReceived` (honestly hardcoded `false` with a stated reason — the client cannot observe partial bytes with the current transport), transport detail, model/digest identity, and the continuation/checkpoint decision actually taken, all present and durably written via `ledger.recordTimeout`. Independently re-confirmed, via reflection over `Unit3CTimeoutRecord::class.java.declaredFields` in a dedicated test, that no field named or typed to hold a parser result or semantic classification exists at all — not merely that the code happens not to populate one.

## 9. Warm-up timeout behavior

Independently re-traced: a `Unit3CTransportException` of any classification raised during a warm-up trial causes `runWarmups` to call `ledger.recordTimeout(...)` then `return Unit3CArmOutcome.CAMPAIGN_HALT_WARMUP_TRANSPORT` immediately — no further warm-up trials are attempted, and `ledger.seal(...)` is never reached, so the warm-up sub-campaign can never be sealed as successful. Independently re-traced `Unit3COrchestrationDriver.run()`'s own loop: it checks `result.outcome == CAMPAIGN_HALT_WARMUP_TRANSPORT` after Control's own result and, if true, marks every subsequent arm `NOT_ATTEMPTED_CAMPAIGN_HALTED` without calling `executors.getValue(family)` or `runArm` for it at all. Independently re-ran all three warm-up-classification tests (model timeout, transport failure, ambiguous): all confirm Family A's own executor is never invoked (`familyAAttempted` remains `false` in the dedicated test built specifically to detect this), Control's own scored `raw.jsonl` is never created, and no retry occurs (each test's executor is invoked exactly once for the failing trial).

## 10. Scored-trial timeout behavior

Independently re-traced, for Control/Family A/Family B uniformly (one shared code path in `runArm`, not three separately-tunable ones — independently confirmed by re-reading the function once, not once per arm): a `MODEL_TIMEOUT` classification durably records the timeout, marks the trial resolved, and `continue`s the loop — the arm still seals, later trials still execute, the campaign still proceeds to the next arm. Independently re-ran the three per-arm dedicated tests: Control seals with 144 raw + 1 timeout = 145 resolved; Family A with 219 raw + 1 timeout = 220; Family B with 114 raw + 1 timeout = 115. No fabricated `GOAL`/`REPLY`/`REMEMBER`/`NOACTION` — independently re-confirmed via direct string search of the persisted timeout record.

## 11. Infrastructure distinction

Independently re-read `buildModelInvokingExecutor`'s three new catch clauses in their declared order — `TimeoutCancellationException` (specific) before `IOException` (broader) before bare `Exception` (catch-all) — confirming a genuine, correctly-ordered, type-based classification, not a message-string heuristic. `TRANSPORT_OR_PROVIDER_FAILURE` and `AMBIGUOUS` are independently re-confirmed to receive identical treatment in `runArm` (record timeout, re-throw as `Unit3CArtifactIntegrityException`, arm halts) and in `runWarmups` (both cause the same campaign halt) — this is correct per the governed Determination, which explicitly accepts this collapse for a loopback-only endpoint, not an implementation gap. Neither classification is ever converted into a fabricated `Unit3CObservation`; both are independently re-confirmed to be excluded from the remedy-performance path entirely (the arm halts before any further trial, scored or not, is attempted).

## 12. Exact-once proof

Independently re-read `Unit3CArmLedger.recover()`'s current logic: computes `resolved = rawIds ∪ timeoutIds` (states B+C, "do not retry"); independently re-confirmed a new check throws `Unit3CArtifactIntegrityException` whenever any intent ID has neither a raw nor a timeout resolution (state D, fails closed). Independently re-ran the crash-recovery test: a second, independent `Unit3COrchestrationDriver` instance run over an already-timed-out trial invokes the executor zero additional times for it. Independently re-ran the ambiguous-intent test (a bare `recordIntent` with no resolution, `recover()` called directly): throws as required. Independently re-ran the never-transmitted-distinguishable test: `hasIntent` correctly reports `false` for an untouched trial ID and `true` only after `recordIntent` is called for it. **A call that may have reached the model cannot be silently retried — independently re-confirmed at both the unit (ledger) and integration (driver) level.**

## 13. Executable 483-call proof

Independently re-traced the full path rather than inferring from arithmetic alone: detached Gradle task (`unit3cControlledRemedyExperiments`, unaffected by this task's diff — `git diff --stat -- build.gradle.kts` independently re-confirmed empty across the entire timeout/durability implementation) → gated live trigger (`live Unit 3-C campaign is skipped...`, unaffected, still reports `skipped` under this review's own fresh offline run) → `Unit3CLiveEntryPoint.run` (unaffected — independently re-read its full body, unchanged: constructs `Unit3COrchestrationDriver` and wires all four family executors exactly as before) → `Unit3CConfigLoader.load`/`Unit3CArtifactRootPolicy.resolve`/`Unit3CDiskSpaceGate.check` (unaffected) → `driver.run(executors)` (the modified sequential loop, independently re-read) → `runWarmups` (3 calls) → `runArm(CONTROL)` (145) → `runArm(FAMILY_A)` (220) → `runArm(FAMILY_B)` (115) → `runArm(FAMILY_C)` (0 model calls, deterministic classifier only) → durable artifacts via `Unit3CArmLedger`. Independently re-derived the schedule constants directly (`WARMUP_ATTEMPTS = 3`, `REPETITIONS_MODEL_ARMS = 5`, 29 fixtures for Control, 23 for Family A/B, 29 for Family C at 1 rep each) and independently re-confirmed `Unit3CCampaignDefinition`'s own `init`-time `check(liveModelCallCount == 483)` still executes and passes on every test run, including this review's own fresh re-run (95 tests, 0 failures). **Confirmed the timeout-handling changes introduce no retry call, no replacement call, no hidden warm-up, no duplicate call, and no Family C model call** — independently verified via the dedicated tests proving exactly one executor invocation per trial ID in every scenario tested (including the timeout scenarios, where the executor is called exactly once and the resulting failure is recorded, not retried), and via the Family-C-specific tests confirming its own executor never raises or requires `Unit3CTransportException` handling at all (it makes no model call, by construction, unaffected by this task).

## 14. Artifact root

`/var/lib/parker/reasoning-protocol-live-model`, independently re-confirmed via fresh `stat`: owned `steve:steve`, mode `700`. `Unit3CArtifactRootPolicy` unaffected by this task's diff. The proposed new campaign directory (`unit3c-remedy-experiments-20260810-02`) independently re-confirmed absent.

## 15. Free space

Fresh `df -B1`: **3,922,468,864 bytes available** — a modest, expected decrease from the prior review's own reading, consistent with ordinary system activity, still well above the governed 2 GiB (2,147,483,648 bytes) minimum with over 1.63 GiB of margin. The disk-space gate itself (checking `artifactRoot.parent`, unaffected by this task) independently re-confirmed unchanged and still correctly targets the durable, already-existing parent.

## 16. Historical artifact integrity

Fresh re-hash, performed for this review: Unit 2's `stage-0/STAGE-0/raw.jsonl` → `c635ebcd051a7eeb02e154e3b07a4ba9e101fcd71f019bebc5990961f8179d5f`, matching every prior review. Unit 2-D's three manifest-tracked artifacts (`warmup`, `production-track`, `candidate-track`) → `d542f0ed...`, `568f08f2...`, `c12de361...`, all matching. **Attempt 3's own preserved evidence** (`unit3c-remedy-experiments-20260810/control/warmup/identity.txt`) → `56af7ca3fa84b1e3c6aca3d4fdd2a23f5884cc5a0ff5a7b574fe2d663d62c9c8`, byte-for-byte unchanged from every capture across the two intervening governance-only tasks and this task's own implementation work. No mutation anywhere.

## 17. Downstream isolation

Independently re-confirmed via a fresh run of the existing forbidden-import tests (unmodified by this task) and a direct read of every line this task's diff added: no Memory/Goal/Planner/tool/communication/Knowledge-Submission symbol appears anywhere in the new timeout/durability code. Experimental execution remains structurally incapable of mutating production state or evidence outside the governed artifact root.

## 18. Exact future execution configuration

```text
Repository commit:     e06f85c308094b52abf8aa3b77b376d795f1e01a
Campaign ID:           unit3c-remedy-experiments-20260810-02
Endpoint:               http://127.0.0.1:11434/api/generate
Model:                  qwen2.5-coder:7b
Digest:                 dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364
Timeout:                90000 ms
Artifact root:          /var/lib/parker/reasoning-protocol-live-model
Runtime identity:       host parker, Ollama 0.32.5

PARKER_REASONING_EVAL_ENDPOINT_URL=http://127.0.0.1:11434/api/generate
PARKER_REASONING_EVAL_MODEL_NAME=qwen2.5-coder:7b
PARKER_REASONING_EVAL_TIMEOUT_MS=90000
PARKER_REASONING_EVAL_OUTPUT_PATH=build/unit3c-legacy-output-unused
PARKER_REASONING_EVAL_REPOSITORY_COMMIT=e06f85c308094b52abf8aa3b77b376d795f1e01a
PARKER_REASONING_EVAL_MODEL_DIGEST=dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364
PARKER_REASONING_UNIT3C_CAMPAIGN_ID=unit3c-remedy-experiments-20260810-02
PARKER_REASONING_UNIT3C_ARTIFACT_ROOT=/var/lib/parker/reasoning-protocol-live-model

Gradle-set system property (automatic, part of the unit3cControlledRemedyExperiments
task registration, never exported manually): parker.reasoning.unit3c.enabled=true
```

None of these values was exported or activated during this review.

## 19. Authorization boundary

This review authorizes exactly: **one campaign ID** (`unit3c-remedy-experiments-20260810-02`); **one repository commit** (`e06f85c308094b52abf8aa3b77b376d795f1e01a`, matching the exact source this review examined); **one model digest** (`dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`); **one runtime identity** (Ollama on host `parker`, version `0.32.5`); **the 90,000 ms governed timeout**; **the frozen 483-call schedule**; **the current exact-once/durability semantics (four-state, intent-before-call, terminal timeout records)**; **the current warm-up (campaign-halting) and scored-trial (arm-continuing) timeout semantics**; **the current Family A/B/C mechanisms exactly as committed, with zero post-hoc modification during execution**; **invocation exclusively via the gated live trigger under the detached `unit3cControlledRemedyExperiments` Gradle task**, with complete, explicit environment configuration matching Section 18 exactly. No repeat campaign under this authorization. No automatic recovery/continuation after any governed halt (warm-up campaign-halt, scored-trial arm-halt, or safety checkpoint) without new, separate authority. No reuse of the Attempt 3 campaign ID under any circumstance. No Unit 3-D evaluation. No remedy selection. This authorization lapses if the repository commit executed against differs from the one this review examined, or if any of the timeout/durability/trigger mechanisms examined in Sections 6–13 are modified before execution.

## 20. Prohibited actions confirmed not taken

No `/api/generate` call — only `/api/tags`, plus read-only version/filesystem checks. No live Gradle task invocation. No campaign directory created. No environment variable exported in a way that would activate execution. No code, test, or Gradle modification during this review. No governance amendment. No remedy selected. No Unit 3-D work performed. Nothing staged, committed, or pushed.

## 21. Final verdict

```text
AUTHORIZED
```

**This document authorizes exactly one future campaign execution, under the exact boundary stated in Section 19, and does not itself execute it.** Every governed timeout/durability mechanism implemented since Attempt 3's own halt — the 90,000 ms ceiling, intent-before-call durability, terminal timeout records, four-state exact-once, warm-up campaign-halt semantics, scored-trial arm-continuation semantics, and the model/infrastructure transport distinction — is independently re-traced from committed source (not review prose) and confirmed genuinely reachable via the same, unchanged entry-point wiring this programme has already established. Attempt 3's own evidence remains preserved, untouched, and is not reused. A fresh campaign identity is required and has been determined. The next step, outside this review's own scope, is the actual execution under this exact boundary — not performed here.
