**Status:** Unit 3-C Controlled Remedy Experiments — Execution Evidence Review — **GENUINE, FULLY-INTERPRETABLE EXPLORATORY EVIDENCE NOW EXISTS, produced by Attempt 6, across six independent attempts.** Attempts 1–2 discovered the live-trigger and disk-space-gate defects (both corrected). Attempt 3 was the first to genuinely reach a real `/api/generate` call but timed out under the pre-amendment 30,000 ms ceiling before any observation was durably recorded (zero evidence). Attempt 4 does not exist as a separate execution record. Attempt 5, under the amended 90,000 ms timeout and the newly-implemented intent/terminal-timeout durability, genuinely executed 176 live model calls, producing 173 completed observations and 3 durably-recorded model timeouts, before each arm independently reached its own first governed safety checkpoint — but discovered, honestly recorded, and did not fix a significant durability limitation: the durable payload captured only campaign/family/fixture identity, not per-trial semantic detail. That defect was corrected in a separate task (`deba366`) and freshly authorized for one further attempt (Explicit Execution Approval Review 6). **Attempt 6 (new, appended below), executed under the corrected observation-durability implementation, genuinely transmitted 277 live model calls plus 6 Family C deterministic classifications, producing 276 completed model responses (1 genuine model timeout) and 6 completed classifications — all 282 completed observations durably carrying the full governed Plan Section 16 structured schema (`actualAction`, `semanticCorrect`, `representationValid`, `parserResult`, `parserFailure`, `latencyNanos`, `transportOutcome`, model/runtime identity, `promptIdentity`, `candidateMechanismIdentity`, `stableInputHash`, `repositoryCommit`) — before each of the four model-invoking/classifying arms independently reached its own first governed adversarial-category safety checkpoint and halted, exactly as designed.** This is the first campaign in this programme's history whose evidence is genuinely, durably semantically interpretable at scale, not merely reachable. No arm sealed as fully complete; none was expected to under this outcome. No code, test, or Gradle file was modified during any attempt's own governance task.

# Unit 3-C Controlled Remedy Experiments — Execution Evidence Review

# Attempt 1 — halted: live-execution trigger did not exist (historical, preserved)

**This section is preserved exactly as originally recorded, against the baseline current at the time (`6ebae033c5eb4c7e21b7632d0777395cda2f6e5a`). It is not restated or edited to reflect anything learned later. The defect it discovered was independently confirmed and corrected in a separate, subsequent task (Live Trigger Defect Confirmation Review, Live Trigger Defect Independent Constitutional Review), prior to Attempt 2 below.**

## 1. Committed baseline

`HEAD` = `origin/main` = `6ebae033c5eb4c7e21b7632d0777395cda2f6e5a`, confirmed clean at the start of this task and unchanged throughout — independently re-confirmed immediately before drafting this document.

## 2. Execution authority

Explicit Execution Approval Review 2 (`docs/reviews/..._EXPLICIT_EXECUTION_APPROVAL_REVIEW_2.md`), verdict `AUTHORIZED`, authorizing exactly one campaign under campaign ID `unit3c-remedy-experiments-20260810`, repository commit `6ebae033c5eb4c7e21b7632d0777395cda2f6e5a` (confirmed to be the exact commit this review examined), model digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`, and artifact parent `/var/lib/parker/reasoning-protocol-live-model`. This authority remains intact and unused — nothing in this task consumed or exceeded it, because execution was never attempted past the configuration-verification step.

## 3. Pre-execution checkpoint

Every item Phase 3 required was independently re-verified and **passed**, fresh, before configuration began:

| Item | Result |
|---|---|
| Repository | PASS — clean, matches `6ebae033...` |
| Campaign directory absent | PASS |
| Model | `qwen2.5-coder:7b`, confirmed installed |
| Model digest | `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364` — PASS |
| Ollama runtime identity | hostname `parker`, version `0.32.5` — unchanged from both prior Approval Reviews |
| Inference config | unchanged, matches frozen configuration |
| Timeout | 30,000 ms, PASS |
| Artifact root | `/var/lib/parker/reasoning-protocol-live-model`, PASS |
| Artifact root ownership/permissions | `steve:steve`, `700` — PASS |
| Free space | 3,932,594,176 bytes available (≥ 2 GiB) — PASS |
| Unit 2 artifact integrity | re-hashed, matches recorded manifest exactly — PASS |
| Unit 2-D artifact integrity | all four artifacts re-hashed, match recorded inventory exactly — PASS |
| Family C corrected trace governance | 24/29, FP = P03/P04/P05/P12, FN = R03 — confirmed in committed source — PASS |
| Executable call schedule (as a number) | 3/145/220/115/0 = 483, confirmed by fresh, full, offline test run (69/69 passing) — PASS |
| Exact-once | PASS (offline tests) |
| Safety checkpoint | PASS (offline tests) |
| Downstream isolation | PASS (offline tests) |

**Every item Phase 3 explicitly enumerated passed.** The defect this review discovered was not on that list — it surfaced only when this task proceeded to Phase 4 and attempted to determine the exact mechanism by which the Gradle task would actually reach `Unit3CLiveEntryPoint.run`.

## 4. Exact configuration prepared (never activated)

The following values were assembled and printed for comparison against Approval Review 2, and confirmed to match it exactly, but **were never exported into any process environment**:

`PARKER_REASONING_EVAL_ENDPOINT_URL=http://127.0.0.1:11434/api/generate`; `PARKER_REASONING_EVAL_MODEL_NAME=qwen2.5-coder:7b`; `PARKER_REASONING_EVAL_TIMEOUT_MS=30000`; `PARKER_REASONING_EVAL_OUTPUT_PATH=build/unit3c-legacy-output-unused`; `PARKER_REASONING_EVAL_REPOSITORY_COMMIT=6ebae033c5eb4c7e21b7632d0777395cda2f6e5a`; `PARKER_REASONING_EVAL_MODEL_DIGEST=dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`; `PARKER_REASONING_UNIT3C_CAMPAIGN_ID=unit3c-remedy-experiments-20260810`; `PARKER_REASONING_UNIT3C_ARTIFACT_ROOT=/var/lib/parker/reasoning-protocol-live-model`. Independently re-confirmed absent from the shell environment immediately before drafting this document.

## 5. Model/runtime identity

Confirmed, read-only, immediately before the configuration step: `qwen2.5-coder:7b`, digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`; hostname `parker`; Ollama `0.32.5`. Unchanged from both prior Approval Reviews.

## 6. Campaign ID

`unit3c-remedy-experiments-20260810` — prepared, never used. Confirmed absent from the artifact root both before and after this task.

## 7. Start/end result

**Execution never started.** Discovered, before any environment variable was exported and before any Gradle invocation with live configuration was attempted, that no code path in the committed implementation would have reached `Unit3CLiveEntryPoint.run` with real arguments even if the environment had been fully configured (Section 24).

## 8. Gradle task result

Not invoked with live configuration. The task was re-run once more, offline only, as part of the pre-execution checkpoint (Section 3): `./gradlew unit3cControlledRemedyExperiments --rerun-tasks` — 69 tests, 0 failures, entirely using fake executors, no live call.

## 9. Actual model-call accounting

Not applicable — zero calls of any kind were made. Warm-up: 0. Control: 0. Family A: 0. Family B: 0. Family C: 0. Total: 0.

## 10. Campaign state

No campaign was created. `/var/lib/parker/reasoning-protocol-live-model/` contains only the two pre-existing Unit 2 and Unit 2-D directories, independently re-confirmed via fresh filesystem listing.

## 11. Artifact inventory

None produced.

## 12. Artifact hashes

None produced.

## 13. Warm-up result

Not applicable — no warm-up call was issued.

## 14–17. Control/Family A/Family B/Family C descriptive results

Not applicable — no observations of any kind were produced by this task. The Family C *deterministic mechanism's own predicted, governance-frozen* profile (24/29 correct, four false positives, one false negative) remains exactly as already documented in the frozen, corrected Plan and the Warm-Up Orchestration Defect review chain — this review does not restate it as if it were newly-observed live evidence, since no live campaign ran to produce any.

## 18–21. Semantic/representation, false-positive, timeout/transport outcomes

Not applicable — no trials were executed.

## 22. Safety-checkpoint outcome

Not triggered — no trials were executed for it to trigger on.

## 23. Exact-once result

Not exercised live in this task; its offline test coverage was re-confirmed passing (Section 3) but no live durability behavior was observed, since nothing was written to any campaign directory.

## 24. The blocking discovery — exact cause

**No code path anywhere in the committed repository connects the `unit3cControlledRemedyExperiments` Gradle task to `Unit3CLiveEntryPoint.run` with real, environment-sourced arguments.**

`Unit3CLiveEntryPoint.run` is referenced in exactly two places in the entire tracked repository: its own declaration, and one existing test, `live entry point fails closed absent required environment configuration`, which calls it with a literal `emptyMap()` — proving only that the function itself fails closed when given no configuration. No test anywhere calls `System.getenv()` and passes it to `Unit3CConfigLoader.load` or `Unit3CLiveEntryPoint.run`.

This is structurally different from, and was not caught by, any prior review in this chain, because every prior review (Completion Review and its ICR, both Readiness Reviews and their ICRs, both Explicit Execution Approval Reviews) independently verified that `Unit3CLiveEntryPoint.run` *itself* correctly gates on configuration and correctly wires the driver once configuration is present — all true, and all independently re-confirmed accurate by this review. None of them checked whether the Gradle task's own `useJUnitPlatform()` test run would ever actually *invoke* that function under real conditions, because the question "is there a real trigger test, analogous to Unit 2-D's own `live Unit 2-D diagnostic campaign skips before definition client or configuration construction unless explicit property is enabled`" was never explicitly asked.

**Unit 2-D's own precedent, independently re-read for this review, makes the gap unambiguous by direct comparison.** Unit 2-D's file contains a real `@Test` function that: checks the Gradle-set system property via `assumeTrue`; checks a real campaign-ID environment variable via `assumeTrue`; and, only if both are present, loads real configuration, constructs the real harness and runner, and executes the live campaign — entirely inside that one `@Test` function's own body. **No equivalent function exists anywhere in either Unit 3-C file.**

## 25. Prohibited interpretations

This finding must not be read as: a defect in `Unit3CLiveEntryPoint.run`'s own internal logic (independently re-confirmed correct — configuration gate, artifact-root gate, disk-space gate, real executor construction, and driver invocation are all present and correctly ordered); a defect in the orchestration driver, the warm-up correction, the ledger, the safety checkpoint, or any Family A/B/C mechanism (none of these was exercised or implicated); a reason to doubt the frozen 483-call schedule (the schedule itself remains correctly derivable and, per the immediately prior task's own correction, genuinely executable by the driver once invoked); or a reason to reopen any governance document's own verdict (Completion PASS, both ICRs ACCEPTED, both Readiness Reviews READY, both Approval Reviews' own reasoning) — all of those verdicts concerned questions they were asked and answered correctly; none of them was asked "is there a real trigger."

## 26. Readiness for Unit 3-D

**Not applicable, and not reached.** Unit 3-D performs comparative evaluation of exploratory evidence; no exploratory evidence exists, because no campaign ran. Unit 3-D may not begin.

## 27. Exact next required step (not performed in this task)

A narrow, precisely-scoped implementation task — separately authorized, following this same programme's own established pattern for the Family C trace defect and the warm-up orchestration defect — must add one `@Test` function to `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` or `ReasoningProtocolUnit3COrchestrationTest.kt`, mirroring Unit 2-D's own precedent exactly: gate on the Gradle-set `parker.reasoning.unit3c.enabled` system property and a real campaign-ID environment variable via `assumeTrue`, then call `Unit3CLiveEntryPoint.run(System.getenv(), Path.of("."))`. Once added, that correction must pass through the same Completion Review → Completion ICR → Readiness Review → Readiness ICR → Explicit Execution Approval Review sequence this programme has consistently required for every prior correction, before any live execution is attempted again.

**Post-hoc note, added when Attempt 2 was recorded below, not part of the original Attempt 1 record:** this step was subsequently completed in a separate task (commit `ec73296f5a6d7d38e01dff019d16d0ff98b45bf2`), independently reviewed through the full chain named above, and independently re-confirmed reachable in Attempt 2 (Section 2a below). The trigger mechanism itself is not implicated in Attempt 2's own halt.

## 28. Attempt 1 verdict

```text
EXECUTION DID NOT OCCUR — BLOCKING STRUCTURAL DEFECT DISCOVERED, CORRECTIVE ACTION REQUIRED
```

The Explicit Execution Approval Review 2's own `AUTHORIZED` verdict is not itself invalidated by this finding — the campaign it authorized remains authorized in principle, under the same boundary, once the missing trigger mechanism is added and passes fresh review. This document records that the attempt to actually exercise that authorization, in this task, discovered the authorized mechanism does not yet exist in committed code, and that this task correctly stopped rather than working around that gap.

---

# Attempt 2 — halted: disk-space gate fails closed against a not-yet-created campaign directory

**New record, added by this task.** Baseline: `ec73296f5a6d7d38e01dff019d16d0ff98b45bf2` (`HEAD` = `origin/main`, clean, independently re-confirmed at this task's start). Authority: Explicit Execution Approval Review 3 (`AUTHORIZED`). This attempt used the corrected live trigger for the first time under real, complete environment configuration.

## A1. Execution authority

Explicit Execution Approval Review 3, verdict `AUTHORIZED`, authorizing exactly one campaign under campaign ID `unit3c-remedy-experiments-20260810`, repository commit `ec73296f5a6d7d38e01dff019d16d0ff98b45bf2` (the exact commit this task examined and executed against), model digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`, and artifact parent `/var/lib/parker/reasoning-protocol-live-model`. This authority remains unconsumed: nothing in this attempt exceeded it, because execution halted before the first live call.

## A2. Pre-execution checkpoint (Phase 3, this task)

Every item independently re-verified and **passed**, fresh, before configuration:

| Item | Result |
|---|---|
| Repository | PASS — clean, matches `ec73296...` |
| Campaign directory absent | PASS |
| Model | `qwen2.5-coder:7b`, confirmed installed |
| Model digest | `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364` — PASS |
| Ollama runtime identity | hostname `parker`, version `0.32.5` — unchanged from every prior review |
| Inference config | unchanged |
| Timeout | 30,000 ms, PASS |
| Artifact root | `/var/lib/parker/reasoning-protocol-live-model`, PASS |
| Artifact root ownership/permissions | `steve:steve`, `700` — PASS |
| Free space | 3,930,304,512 bytes available (≥ 2 GiB) — PASS |
| Unit 2 artifact integrity | re-hashed, matches recorded manifest exactly — PASS |
| Unit 2-D artifact integrity | all four manifest-tracked artifacts re-hashed, match exactly — PASS |
| Family C corrected trace governance | 24/29, FP = P03/P04/P05/P12, FN = R03 — confirmed in committed source — PASS |
| Executable call schedule (offline) | 483, confirmed by a fresh, full offline test run (78/78 excluding the trigger's own skip, 0 failures) — PASS |
| Exact-once, safety checkpoint, downstream isolation | PASS (offline tests) |
| Live trigger reachability | PASS — trigger present, correctly gated, correctly skipped under the offline pre-check run |

**Every pre-execution item passed.** The defect this attempt discovered was not on this list — it surfaced only when Phase 5 actually exported the real environment and invoked the Gradle task.

## A3. Configuration exported

`PARKER_REASONING_EVAL_ENDPOINT_URL=http://127.0.0.1:11434/api/generate`; `PARKER_REASONING_EVAL_MODEL_NAME=qwen2.5-coder:7b`; `PARKER_REASONING_EVAL_TIMEOUT_MS=30000`; `PARKER_REASONING_EVAL_OUTPUT_PATH=build/unit3c-legacy-output-unused`; `PARKER_REASONING_EVAL_REPOSITORY_COMMIT=ec73296f5a6d7d38e01dff019d16d0ff98b45bf2`; `PARKER_REASONING_EVAL_MODEL_DIGEST=dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`; `PARKER_REASONING_UNIT3C_CAMPAIGN_ID=unit3c-remedy-experiments-20260810`; `PARKER_REASONING_UNIT3C_ARTIFACT_ROOT=/var/lib/parker/reasoning-protocol-live-model`. Exported into exactly one background shell invocation of `./gradlew unit3cControlledRemedyExperiments --rerun-tasks`; the shell (and its environment) terminated when that single command finished. No other process observed these values.

## A4. Command executed

```
./gradlew unit3cControlledRemedyExperiments --rerun-tasks --info
```

Run exactly once, in the background, with the configuration in A3 exported. Not re-run after failure.

## A5. What actually happened

The Gradle task ran the full 78-test class set under live configuration. Two tests failed; the build failed. Full detail:

**A5.1 — The live trigger genuinely worked, for the first time.** The test `live Unit 3-C campaign is skipped before any configuration is loaded unless the Gradle-set property and a real campaign ID are both present` did **not** skip this time (both `assumeTrue` gates passed, since real configuration was present) — it proceeded to call `Unit3CLiveEntryPoint.run(System.getenv(), Path.of("."))` for the first time in this programme's history under genuinely complete, real configuration. This is independent, positive confirmation that the live-trigger correction (Attempt 1's own follow-up) works exactly as designed: config validation passed, artifact-root resolution passed, and the call reached `Unit3COrchestrationDriver.run`.

**A5.2 — A new, previously undetected defect then halted execution before any live call.** `Unit3COrchestrationDriver.run` calls `Unit3CDiskSpaceGate.check(artifactRoot, usableSpace)` (`ReasoningProtocolUnit3COrchestrationTest.kt:193`) before entering the arm loop, exactly as designed — but `artifactRoot` here is the *campaign-specific* subdirectory (`.../unit3c-remedy-experiments-20260810`), which does not yet exist on disk for a first-ever campaign. The real `usableSpace` lambda (`{ Files.getFileStore(it).usableSpace }`, line 190) requires the path to already exist; called against a non-existent path it throws `NoSuchFileException`, which `Unit3CDiskSpaceGate.check`'s own fail-closed `catch (e: IOException)` block (correctly, per its own contract) converts into:

```
parker.integration.Unit3CInsufficientSpaceException: unable to determine usable space for /var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810: /var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810
    at parker.integration.Unit3CDiskSpaceGate.check(ReasoningProtocolUnit3COrchestrationTest.kt:104)
    at parker.integration.Unit3COrchestrationDriver.run(ReasoningProtocolUnit3COrchestrationTest.kt:193)
    at parker.integration.Unit3CLiveEntryPoint.run(ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt:818)
    at parker.integration.ReasoningProtocolUnit3CControlledRemedyExperimentsTest.live Unit 3-C campaign is skipped...(ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt:1462)
```

This is a genuine, real defect, in the same category as the two defects discovered earlier in this programme (Family C's trace, the warm-up wiring): code that is correct in every offline test that exercises it, because every offline test happens to run the gate against a path that already exists (an existing `@TempDir`, or a fake `usableSpace` lambda that never touches the filesystem) — never against a real, not-yet-created campaign directory, which is exactly the condition every genuine first-ever live campaign will always start from. **This gate's own fail-closed design worked correctly** — it refused to proceed rather than silently treating "cannot determine free space" as "space is fine" — but its precondition (that `artifactRoot` already exists) was never true for a first campaign, and nothing in the implementation creates that directory before the gate runs.

**A5.3 — A second, independent, lower-severity test failure occurred in parallel.** `live Unit 3-C trigger requires a real campaign ID even when the detached task's own property is already set` failed: `assertNull(System.getenv(CAMPAIGN_ID))` — this assertion, added alongside the trigger correction specifically to prove no real campaign ID is present during *ordinary offline* execution, is unconditionally false during any *genuine* live execution by construction, since a genuine live execution necessarily sets that variable. This is not a defect in the trigger's own gating logic (independently confirmed unaffected: A5.1 shows the trigger correctly reached the entry point regardless of this unrelated test's own failure) — it is a scoping oversight in that one verification-only test, which needs to be conditioned on the absence of live configuration (or removed and replaced by a purely offline-simulated equivalent) so it does not spuriously fail every genuine campaign run. Independent of, and non-blocking relative to, A5.2's own disk-space finding — either failure alone would have failed the build and halted this attempt.

## A6. Actual model-call accounting

**Zero**, independently confirmed by three lines of evidence: (1) the stack trace shows the exception thrown from `Unit3CDiskSpaceGate.check`, called before `Unit3COrchestrationDriver.run` reaches its arm loop, structurally before any executor (Control/Family A/Family B/Family C) is ever invoked; (2) no campaign directory of any kind exists under `/var/lib/parker/reasoning-protocol-live-model/` after this attempt — a fresh listing shows only the two preserved Unit 2/Unit 2-D directories, confirming not even the campaign directory itself, let alone any ledger or raw-response file, was created; (3) no other process or script issued any HTTP request during this attempt — the only network-capable code path this attempt reached was the disk-space gate, which performs a local filesystem stat, not an HTTP call. Warm-up: 0. Control: 0. Family A: 0. Family B: 0. Family C: 0. Total: 0.

## A7. Campaign state

No campaign was created. `/var/lib/parker/reasoning-protocol-live-model/` contains only the two pre-existing Unit 2 and Unit 2-D directories, independently re-confirmed via fresh filesystem listing immediately after this attempt.

## A8. Artifact inventory / hashes

None produced.

## A9. Prohibited interpretations

This finding must not be read as: a defect in the live trigger itself (independently confirmed working correctly — A5.1); a defect in `Unit3CConfigLoader`, `Unit3CArtifactRootPolicy`, or config/root validation (all independently confirmed to have passed correctly before the disk-space gate ran); a defect in the warm-up correction, the ledger, the safety checkpoint, or any Family A/B/C mechanism (none of these was reached); a reason to doubt the frozen 483-call schedule (unaffected, still independently proven executable by offline tests using a pre-existing tempdir); or a reason to reopen any prior governance document's own verdict on the questions it was actually asked (Completion PASS, all four ICRs ACCEPTED, both Readiness Reviews READY, Approval Reviews 1–3's own reasoning) — none of them was asked "does the disk-space gate work against a campaign directory that does not yet exist."

## A10. Readiness for Unit 3-D

**Not applicable, and not reached.** No exploratory evidence exists, because no campaign ran. Unit 3-D may not begin.

## A11. Exact next required step (not performed in this task)

This task is execution-and-evidence-review only and is not authorized to modify production code, test code, or `build.gradle.kts`. A separate, narrowly-scoped correction task — following this programme's own established pattern — should independently confirm this defect and implement the narrowest fix (most plausibly: run the disk-space check against the artifact root's already-existing *parent*, or create the campaign directory via `Files.createDirectories` before the gate runs, with the correct choice determined by that task's own independent analysis, not assumed here), verified offline against a genuinely non-existent target path (the specific condition no existing test covers), then pass through the same Completion → Completion ICR → Readiness → Readiness ICR → Explicit Execution Approval Review sequence before any further live-execution attempt. The unrelated `assertNull` test scoping issue (A5.3) should be corrected in the same task.

## A12. Attempt 2 verdict

```text
EXECUTION DID NOT OCCUR — NEW BLOCKING STRUCTURAL DEFECT DISCOVERED (DISK-SPACE GATE), CORRECTIVE ACTION REQUIRED
```

Explicit Execution Approval Review 3's own `AUTHORIZED` verdict is not itself invalidated by this finding — the campaign it authorized remains authorized in principle, under the same boundary, once the disk-space gate defect is corrected and passes fresh review. This document records that the attempt to actually exercise that authorization discovered a second, previously undetected structural defect, unrelated to the trigger mechanism that Approval Review 3 specifically verified, and that this task correctly stopped rather than working around, retrying, or fixing it outside its own authorized scope.

---

# Attempt 3 — first real `/api/generate` call attempted; timed out before any observation was durably recorded

**New record, added by this task.** This is the first attempt in this programme's entire history in which every gate before the live HTTP call passed and a real call was genuinely issued.

## B1. Baseline commit

`db9c612c4092b438582e61447e6fdab2c2dd37b5` — `HEAD` = `origin/main`, clean, independently re-confirmed at this task's start.

## B2. Authorization

Explicit Execution Approval Review 4, verdict `AUTHORIZED`, authorizing exactly one campaign under campaign ID `unit3c-remedy-experiments-20260810`, repository commit `db9c612c4092b438582e61447e6fdab2c2dd37b5` (the exact commit this task examined and executed against), model digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`, artifact parent `/var/lib/parker/reasoning-protocol-live-model`.

## B3. Pre-execution checkpoint (Phase 4, this task)

Every item independently re-verified and **passed**, fresh, before configuration:

| Item | Result |
|---|---|
| Repository | PASS — clean, matches `db9c612...` |
| Authorization | PASS — Approval Review 4 genuinely tied to `db9c612` (its own working-tree description matches this commit's diff file-for-file) |
| Campaign absent | PASS |
| Model name | `qwen2.5-coder:7b` — PASS |
| Model digest | `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364` — PASS |
| Runtime identity | hostname `parker`, Ollama `0.32.5` — PASS |
| Inference configuration | unchanged — PASS |
| Timeout | 30,000 ms — PASS |
| Artifact root | `/var/lib/parker/reasoning-protocol-live-model` — PASS |
| Ownership/permissions | `steve:steve`, `700` — PASS |
| Disk space | 3,927,314,432 bytes available (≥ 2 GiB) — PASS |
| Unit 2 artifact integrity | re-hashed, matches — PASS |
| Unit 2-D artifact integrity | all four re-hashed, match — PASS |
| Family C governed trace | 24/29, FP P03/P04/P05/P12, FN R03 — PASS |
| Live trigger reachability | PASS (offline pre-check: 79 tests, 1 skip, 0 failures) |
| Disk-space gate reachability | PASS (two new tests passing) |
| Live-task test filtering | PASS (tagged test absent from this task's own offline pre-check results) |
| Exact-once, safety checkpoint, downstream isolation | PASS (offline tests) |
| Executable call schedule | 483, PASS (offline, pre-existing test) |

**Every item passed.** Execution proceeded to Phase 5–7.

## B4. Configuration exported

`PARKER_REASONING_EVAL_ENDPOINT_URL=http://127.0.0.1:11434/api/generate`; `PARKER_REASONING_EVAL_MODEL_NAME=qwen2.5-coder:7b`; `PARKER_REASONING_EVAL_TIMEOUT_MS=30000`; `PARKER_REASONING_EVAL_OUTPUT_PATH=build/unit3c-legacy-output-unused`; `PARKER_REASONING_EVAL_REPOSITORY_COMMIT=db9c612c4092b438582e61447e6fdab2c2dd37b5`; `PARKER_REASONING_EVAL_MODEL_DIGEST=dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`; `PARKER_REASONING_UNIT3C_CAMPAIGN_ID=unit3c-remedy-experiments-20260810`; `PARKER_REASONING_UNIT3C_ARTIFACT_ROOT=/var/lib/parker/reasoning-protocol-live-model`. Exported into exactly one background shell invocation; that shell terminated when the command finished.

## B5. Exact command

```
./gradlew unit3cControlledRemedyExperiments --rerun-tasks --info
```

`--rerun-tasks` was deliberately chosen (Phase 6) because the task's declared inputs would otherwise be identical to the offline pre-check run performed immediately before, risking a false UP-TO-DATE no-op. Run exactly once. Not re-run after the failure below.

## B6. What actually happened

The Gradle task started, recompiled (forced by `--rerun-tasks`), and began executing the 79-test class set under live configuration. The gated live trigger test's two `assumeTrue` checks both passed for the first time under genuine live configuration (property `true`, campaign ID present). Execution proceeded into `Unit3CLiveEntryPoint.run`: `Unit3CConfigLoader.load` succeeded; `Unit3CArtifactRootPolicy.resolve` succeeded; `Unit3CDiskSpaceGate.check` — now checking the durable parent per the disk-space-gate correction — succeeded; `Unit3COrchestrationDriver` was constructed and `run()` was called; `runArm(CONTROL)` called `runWarmups`, which called `ledger.checkIdentity(identity)` — **this succeeded and wrote `control/warmup/identity.txt`, independently re-confirmed to contain exactly `db9c612c4092b438582e61447e6fdab2c2dd37b5|qwen2.5-coder:7b|dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364|http://127.0.0.1:11434/api/generate|30000` — a byte-for-byte match to the authorized configuration.**

`runWarmups` then called `executor.execute(trial)` for the first warm-up trial, which invokes `ModelReasoningProvider.reason` → `LocalHttpModelInferenceClient.infer`, issuing the first genuine `/api/generate` HTTP request in this programme's history. The call did not return within the frozen 30,000 ms timeout:

```
kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 30000 ms
    at parker.core.runtime.LocalHttpModelInferenceClient.infer(ModelInferenceClient.kt:150)
    at parker.core.runtime.ModelReasoningProvider$reason$raw$1.invokeSuspend(ModelReasoningProvider.kt:73)
    at parker.core.runtime.ModelReasoningProvider.reason(ModelReasoningProvider.kt:73)
    at parker.integration.ReasoningProtocolUnit3CControlledRemedyExperimentsTestKt$buildModelInvokingExecutor$1$response$1.invokeSuspend(...)
```

Independently re-confirmed via the JUnit XML report: the failing test's own reported `time="30.861"` seconds, and the suite's own `timestamp="2026-08-10T06:28:22"`, are consistent with a genuine ~30-second wait for an HTTP response, not an instant connection failure.

**This exception was never caught.** Independently re-read `runWarmups`'s own `try { ... } catch (e: Unit3CArtifactIntegrityException) { HALTED }` — it catches exactly one exception type (artifact-integrity/identity drift), not generic transport or timeout failures. `TimeoutCancellationException` propagated uncaught through `runWarmups`, `runArm`, `driver.run()`, `Unit3CLiveEntryPoint.run()`, and the trigger test itself, causing the test to `FAILED` and the whole Gradle task to `FAILED`. No `HALTED` or `SAFETY_CHECKPOINT` outcome was ever recorded, because the driver's own code never reached the point of returning one for this arm — the crash occurred one level below that handling.

## B7. Corroborating evidence for a genuine, real network call (not a connection failure)

Independently checked, read-only, immediately after the failure: `ps aux` showed the Ollama `llama-server` subprocess for `qwen2.5-coder:7b` (PID `53349`) running since `06:28` — the exact same time the campaign started — consuming `88.9`–`160%` CPU and `~75%` of system memory, consistent with active model loading/inference, not an idle or crashed process. `/api/tags` remained responsive throughout and after. This is consistent with genuine cold-start model-loading latency (the model had not been resident in memory before this campaign; no prior warm-up request had been issued to this specific model in this session) exceeding the frozen 30-second timeout, rather than a connectivity or code defect. This is offered as a plausible explanation, not a confirmed root cause — no code change is proposed or made based on it.

## B8. Actual model-call accounting (durable evidence)

Derived from durable evidence, not from any in-memory counter or the Gradle result alone:

| Arm | Actual (durable) |
|---|---|
| Warm-up | 0 completed/recorded; exactly 1 real HTTP request believed transmitted (B7), never completing within the governed timeout, never durably recorded |
| Control (scored) | 0 — never reached, since the warm-up gate did not return `SEALED` |
| Family A | 0 — never reached |
| Family B | 0 — never reached |
| Family C | 0 — never reached (would have been zero regardless, by design) |
| **Total durably recorded** | **0** |

Independently verified by direct inspection: `find /var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810 -mindepth 1` returns exactly one file, `control/warmup/identity.txt`. No `raw.jsonl` exists anywhere under the campaign directory (the file `Unit3CArmLedger.appendObservation` would have created), confirming zero observations were ever durably appended, for any arm.

## B9. Campaign state

Campaign directory `/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810` **now exists** — the first Unit 3-C campaign directory ever created. Contains exactly `control/warmup/identity.txt`. No `raw.jsonl`, `checkpoint.txt`, `SEALED`, `manifest.txt`, or `SAFETY_CHECKPOINT` marker exists anywhere under it. State is neither sealed nor gracefully halted; it is a genuine mid-flight crash, preserved exactly as produced, not repaired or resumed.

## B10. Descriptive experiment results

**Not applicable — zero observations of any kind exist for any arm.** No semantic-correctness, representation-validity, content-fidelity, false-positive, false-negative, parser-failure, or latency data exists to report for Control, Family A, Family B, or Family C.

## B11. Safety checkpoint, exact-once, downstream isolation

Safety checkpoint: not reached (no scored trial was ever attempted). Exact-once: not exercised past the single `checkIdentity` write; no duplicate or missing record exists to evaluate. Downstream isolation: unaffected; no code path this attempt exercised references any forbidden symbol.

## B12. Anomalies

One: a raw network/timeout failure (as opposed to an artifact-integrity failure) during a live call is not caught by `runWarmups`/`runArm`'s own exception handling and crashes the entire Gradle task rather than producing a graceful `HALTED` or similar arm-level outcome. This is recorded as an observation for a future task's own consideration, not diagnosed as a defect or corrected here — this task is execution-and-evidence-review only and does not modify code, tests, timeout, or retry behavior.

## B13. Prohibited interpretations

This finding must not be read as: a defect in the live-trigger, disk-space-gate, or live-test-scoping corrections (all three are independently confirmed to have worked exactly as designed — B6 traces the successful path through every one of them, further than any prior attempt reached); evidence that the model, endpoint, or digest are misconfigured (identity.txt's exact match rules this out); or campaign evidence of any kind (zero observations exist, so no descriptive or comparative claim about Control, Family A, Family B, or Family C can be made).

## B14. Readiness for Unit 3-D

**Not applicable, and not reached.** Zero observations exist. Unit 3-D may not begin.

## B15. Exact next required step (not performed in this task)

Outside this task's own scope: an operator/governance decision on whether the frozen 30,000 ms timeout is sufficient for a cold-start (not-yet-resident) model load on this hardware, and whether `runWarmups`/`runArm` should catch transport/timeout failures gracefully (e.g., as a new `Unit3CArmOutcome` category) rather than crashing the whole task. Any such change is itself a frozen-governance-touching decision and must go through this programme's own Scope Lock / Plan amendment process, not be made inside an execution task.

## B16. Attempt 3 verdict

```text
EXECUTION DID NOT PRODUCE EVIDENCE — FIRST GENUINE LIVE CALL ATTEMPTED, TIMED OUT BEFORE COMPLETION, ZERO OBSERVATIONS DURABLY RECORDED
```

Explicit Execution Approval Review 4's own `AUTHORIZED` verdict is not itself invalidated by this finding: every mechanism it verified (trigger reachability, disk-space gate, live-test scoping, config/artifact-root validation) is independently confirmed in this attempt to have worked correctly, for the first time, all the way to a genuine live HTTP call. This document records that the call itself did not complete within the frozen timeout, that this task correctly stopped rather than repairing, retrying, or resuming, and that campaign state was preserved exactly as produced.

---

# Attempt 5 — first campaign to produce genuine exploratory evidence; four independent safety-checkpoint halts

**New record, added by this task.** Intervening work between Attempt 3 and this attempt (not itself a separate live-execution attempt): the Timeout and Inference Latency Investigation Review, the Timeout + Durability Scope Lock and Implementation Plan Amendments, the Scored-Trial Timeout Semantics Determination, their implementation, and Explicit Execution Approval Review 5 (`AUTHORIZED`).

## C1. Baseline

`HEAD` = `origin/main` = `6931fabcd2588bb1cce6279c39bde18eb30028bb`, clean, independently re-confirmed at this task's start.

## C2. Authorization

Explicit Execution Approval Review 5, verdict `AUTHORIZED`, authorizing exactly one campaign under campaign ID `unit3c-remedy-experiments-20260810-02` (a fresh identity, distinct from Attempt 3's own, whose reuse was independently shown to be self-defeating given its recorded `timeoutMs=30000` would fail identity-drift against the now-governed `90000`), repository commit `6931fabcd2588bb1cce6279c39bde18eb30028bb`, model digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`, 90,000 ms timeout, artifact parent `/var/lib/parker/reasoning-protocol-live-model`.

## C3. Campaign ID

`unit3c-remedy-experiments-20260810-02`. Independently re-confirmed absent before this attempt; now exists, containing genuine evidence (Section C9 onward).

## C4. Pre-execution checkpoint

Every item independently re-verified and **passed**, fresh, before configuration: repository (clean, matches `6931fab`); authorization (Approval Review 5 examined `e06f85c`, independently re-confirmed via `git diff e06f85c 6931fab --stat` to differ only by the addition of the review document itself — zero implementation drift); campaign absent; model `qwen2.5-coder:7b`, digest matching; runtime `parker`/`0.32.5`; artifact root `steve:steve`/`700`; free space 3,921,977,344 bytes (≥ 2 GiB); Unit 2/Unit 2-D artifact integrity (re-hashed, matching); Attempt 3 preservation (re-hashed, matching, unchanged throughout); offline suite (95 tests, 1 skip, 0 failures) covering live-trigger reachability, intent-before-call reachability, terminal timeout durability, exact-once, warm-up/scored-trial timeout semantics, transport/infrastructure distinction, downstream isolation, and the 483-call schedule.

## C5. Configuration exported

`PARKER_REASONING_EVAL_ENDPOINT_URL=http://127.0.0.1:11434/api/generate`; `PARKER_REASONING_EVAL_MODEL_NAME=qwen2.5-coder:7b`; `PARKER_REASONING_EVAL_TIMEOUT_MS=90000`; `PARKER_REASONING_EVAL_OUTPUT_PATH=build/unit3c-legacy-output-unused`; `PARKER_REASONING_EVAL_REPOSITORY_COMMIT=6931fabcd2588bb1cce6279c39bde18eb30028bb`; `PARKER_REASONING_EVAL_MODEL_DIGEST=dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`; `PARKER_REASONING_UNIT3C_CAMPAIGN_ID=unit3c-remedy-experiments-20260810-02`; `PARKER_REASONING_UNIT3C_ARTIFACT_ROOT=/var/lib/parker/reasoning-protocol-live-model`. Cross-checked directly against Approval Review 5's own Section 18 table before export: exact match. Exported into exactly one background shell invocation; that shell terminated when the command finished.

## C6. Exact command

```
./gradlew unit3cControlledRemedyExperiments --rerun-tasks --info
```

`--rerun-tasks` chosen for the same reason as every prior attempt (avoiding a false UP-TO-DATE no-op after the immediately preceding offline pre-check run). Run exactly once, in the background. `BUILD SUCCESSFUL in 50m 4s`. Not re-run.

## C7. Execution state and a significant, honestly-stated durability limitation

The campaign genuinely executed. All four model-invoking/classifying arms (Control, Family A, Family B, Family C) independently reached their own first governed adversarial-category false-positive safety checkpoint (Scope Lock §16's own non-numeric, first-occurrence trigger) and halted — **none sealed as fully complete, and none was expected to under this outcome: the safety checkpoint's own governed behavior is to halt further live calls in the affected arm only, pending manual review, never to be silently cleared or retried.** Only the three warm-ups sealed successfully.

**A significant limitation, discovered by this attempt, is recorded honestly rather than concealed or worked around:** `Unit3COrchestrationDriver`'s own `encodeObservation` function (unchanged by the timeout/durability implementation, a pre-existing characteristic of the original Unit 3-C implementation) persists only `campaignId`, `family`, and `fixtureId` as the durable `raw.jsonl` payload for a completed trial — independently re-confirmed by direct inspection of real persisted records from this attempt. It does **not** durably persist `actualAction`, `semanticCorrect`, `representationValid`, `contentFidelity`, `latencyNanos`, `rawResponse`, or `parserResult` — the richer fields `Unit3CObservation` carries only in memory during the campaign run. **Consequence: for the 173 completed observations this attempt produced, this review can prove exactly which trial IDs completed and that they completed, but cannot determine per-trial semantic correctness, representation validity, or content fidelity from durable evidence alone.** This is not a defect this task is authorized to fix (no implementation changes are permitted here) and is not attributable to the timeout/durability work specifically — `encodeObservation`'s own minimal design predates this task. It is recorded here because Attempt 5 is the first attempt to actually produce completed observations at scale, which is the first point at which this pre-existing gap becomes practically consequential rather than theoretical.

## C8. Transmitted call accounting (durable evidence, independently derived)

| Arm | Intent (transmitted) | Raw (completed) | Timeouts | Outcome |
|---|---|---|---|---|
| Warm-up | 3 | 3 | 0 | SEALED |
| Control | 92 | 89 | 3 | SAFETY_CHECKPOINT |
| Family A | 52 | 52 | 0 | SAFETY_CHECKPOINT |
| Family B | 29 | 29 | 0 | SAFETY_CHECKPOINT |
| Family C | 0 (makes no model call) | 6 (classifier invocations) | 0 | SAFETY_CHECKPOINT |
| **Total live model calls transmitted** | **176** | **173 completed** | **3 model timeouts** | — |

Independently re-verified: every arm's `intent.jsonl` count equals its own `raw.jsonl` + `timeouts.jsonl` count exactly (92=89+3; 52=52+0; 29=29+0; 3=3+0), confirming every transmitted call resolved to exactly one of the two governed terminal states, with zero ambiguous-state (D) records and zero transport/provider-failure records anywhere in this campaign. Independently re-confirmed zero duplicate trial IDs in any file (`total` == `unique` for every raw/intent/timeout file) and zero overlap between any arm's `raw.jsonl` and `timeouts.jsonl`. Expected schedule total: 483. Actual transmitted: 176 (36.4%) — the campaign halted at four independent, governed safety checkpoints before exhausting the full schedule; this is the correct, designed behavior for this outcome, not an incomplete or defective run.

## C9. Completed-response and timeout accounting

173 completed responses (89 Control + 52 Family A + 29 Family B + 3 warm-up); 3 model timeouts, all in Control, all independently re-confirmed via `terminalClassification: "MODEL_TIMEOUT"` in the durable timeout record, all with `transportDetail: "kotlinx.coroutines.TimeoutCancellationException"` (governed sub-case 1, provider reachable, client budget exhausted) and `responseBytesReceived: false`; 0 transport/provider failures; 0 ambiguous terminal states. All 3 Control timeouts independently re-confirmed durably recorded with `continuationDecision: "ARM_CONTINUED"`, and the arm did continue past each one (later Control trials, up through the eventual checkpoint, completed normally) — direct, positive proof the governed scored-trial continuation semantics work under real conditions, not only under fakes.

## C10. Campaign state

`unit3c-remedy-experiments-20260810-02` exists. Warm-up: `SEALED`, manifest hash `f61fcd867ead44d392b5bbfb9391ce0af8ae3cd06891373e507d3d111e8b60f0`, independently re-verified by fresh re-hash of its own `raw.jsonl`. Control, Family A, Family B, Family C: each has a `SAFETY_CHECKPOINT` marker, none sealed, no manifest (correctly absent — manifests are written only on seal). No arm's own scored ledger shows any sign of corruption, partial-write, or inconsistency.

## C11. Artifact inventory

Per arm: `identity.txt` (all five, byte-identical apart from directory, all confirming `6931fab...|qwen2.5-coder:7b|dae161e...|http://127.0.0.1:11434/api/generate|90000` — the governed 90,000 ms value, confirmed active in every real identity record for the first time in this programme's history); `intent.jsonl` (absent for Family C, by design); `raw.jsonl`; `checkpoint.txt`; `SAFETY_CHECKPOINT` (Control/Family A/Family B/Family C); `timeouts.jsonl` (Control only, 3 records); `manifest.txt` and `SEALED` (warm-up only).

## C12. Artifact hashes

Warm-up `raw.jsonl` → `f61fcd867ead44d392b5bbfb9391ce0af8ae3cd06891373e507d3d111e8b60f0`, independently re-derived and matching the durable manifest's own recorded value exactly.

## C13. Control descriptive results

92 transmitted, 89 completed, 3 model timeouts. **Semantic correctness, representation validity, and content fidelity for the 89 completed observations cannot be determined from durable evidence** (Section C7). One safety-checkpoint-triggering event: trial `control/g03-later-action/main/02` (fixture `g03-later-action`, category GOAL, expected action GOAL). Independently deduced, not directly observed: since the governed trigger condition (`isAdversarialCategoryFalsePositive`) requires `actualAction` to be `REMEMBER` or `GOAL` and to differ from `expectedAction`, and `expectedAction` here is `GOAL` itself, the triggering `actualAction` must specifically have been `REMEMBER` — a false-positive REMEMBER on a GOAL-category fixture acting as its own negative control.

## C14. Family A descriptive results

52 transmitted, 52 completed, 0 timeouts. Same durable-evidence limitation applies. Checkpoint-triggering event: `family_a/p03-ambiguous-memory/decision/02` (fixture `p03-ambiguous-memory`, category ADVERSARIAL, expected REPLY). Independently deduced: `actualAction` was `REMEMBER` or `GOAL` (both satisfy the trigger condition against an ADVERSARIAL-category fixture); which of the two specifically cannot be determined from durable evidence.

## C15. Family B descriptive results

29 transmitted, 29 completed, 0 timeouts. Same limitation. Checkpoint-triggering event: `family_b/p03-ambiguous-memory/main/04`, same fixture as Family A's own trigger. Same REMEMBER-or-GOAL ambiguity, undeterminable from durable evidence alone.

## C16. Family C descriptive results

0 model calls (by design — Family C is Candidate-C1, a deterministic, offline classifier); 6 classifier invocations recorded. Checkpoint-triggering event: `family_c/p03-ambiguous-memory/main/01`. **Unlike Families A/B, this one is fully, definitively resolvable**, because Candidate-C1's own mechanism (independently re-read: `actualAction = if (signal == REMEMBER_SIGNAL) REMEMBER else null`) can only ever produce `REMEMBER` or no signal — never `GOAL`. The triggering `actualAction` was therefore definitively `REMEMBER`. **This is not new information: `p03-ambiguous-memory` is one of the four already-governed, frozen-prediction false positives (`P03`, `P04`, `P05`, `P12`) established by the corrected Family C trace analysis long before this attempt.** This attempt is the first to confirm that prediction reproduces exactly through the real, live orchestration path, not merely through direct, isolated invocation of the classifier.

## C17. Completion rates

Warm-up 100% (3/3); Control 96.7% of transmitted calls completed (89/92, 3 timed out); Family A 100% (52/52); Family B 100% (29/29); Family C 100% (6/6, deterministic, no transport layer involved). Kept explicitly separate from semantic correctness, which cannot be determined for this campaign (Section C7).

## C18. Semantic outcomes, representation outcomes, content fidelity

**Not determinable from durable evidence for Control/Family A/Family B's own 170 completed observations, for the reason stated in Section C7.** Family C's own single checkpoint-triggering observation is the one exception, independently resolved in Section C16.

## C19. False positives / false negatives

**One false-positive REMEMBER definitively confirmed** (Family C, Section C16, matching the already-governed frozen prediction). **Three further false-positive events independently deduced to have occurred** (Control's own REMEMBER-on-GOAL trigger, Section C13; Family A's and Family B's own REMEMBER-or-GOAL-on-ADVERSARIAL triggers, Sections C14–C15) but not more precisely classifiable from durable evidence. No false-negative count is determinable at all from durable evidence for this campaign.

## C20. Timeout records

Three, all Control, all independently re-confirmed complete per the governed terminal-timeout schema (Section C9), all `MODEL_TIMEOUT` classification, all `ARM_CONTINUED`, none fabricating a parser result or semantic action (independently re-confirmed: no `actualAction`/`parserResult` field exists in the timeout record schema at all).

## C21. Exact-once result

Clean across the entire campaign — independently re-verified zero duplicate trial IDs, zero raw/timeout overlap, every intent resolved to exactly one terminal state. No automatic retry occurred anywhere (each timed-out trial's own executor was invoked exactly once, per the durable count).

## C22. Safety/halt behavior

All four governed checkpoints (Control, Family A, Family B, Family C) independently re-confirmed to have fired on the *first* adversarial-category false-positive event in each arm (not a later one — each arm's own completed-count matches exactly the number of trials preceding, and including, its own triggering trial, with zero trials attempted afterward in that arm), matching the frozen "first occurrence, non-numeric" design exactly. No checkpoint was cleared, resumed, or worked around during this task.

## C23. Downstream isolation

Unaffected; no code path this campaign's own execution exercised references any Memory/Goal/Planner/Knowledge-Submission symbol — independently re-confirmed via the unmodified forbidden-import tests (part of the 95-test offline suite, re-run before this attempt) and by the fact that no code was changed during or after execution.

## C24. Anomalies

The `encodeObservation` durability limitation (Section C7) is the primary anomaly this attempt surfaces — real, honestly stated, out of this task's own authorized scope to fix. Secondary, informational only: four independent safety-checkpoint halts in a single campaign is a higher concentration than any prior Unit programme campaign has produced, consistent with this being the first time qwen2.5-coder:7b's actual behavior against the full adversarial fixture surface has been observed at this scale live (Unit 2/Unit 2-D exercised smaller, different fixture subsets).

## C25. Limitations

Genuine exploratory evidence exists for 176 transmitted calls (36.4% of the frozen 483-call schedule) but is incomplete relative to the full schedule, halted by design at each arm's own first safety checkpoint. Semantic/representation/content-fidelity detail is unavailable from durable evidence for all but one of the four checkpoint-triggering observations (Section C7/C16). No arm produced a complete, sealed dataset. This evidence is exploratory-tier only (per the Unit 3-C Scope Lock's own frozen evidence-tier distinction) and was always going to be regardless of this specific outcome.

## C26. Readiness for Unit 3-D

**Not reached by this task, and this task does not determine it — reserved for the Independent Execution Evidence Review and any future, separately-authorized Unit 3-D task.** Genuine, if partial and durability-limited, exploratory evidence now exists, unlike every prior attempt.

## C27. Attempt 5 verdict

```text
EXECUTION OCCURRED — GENUINE PARTIAL EXPLORATORY EVIDENCE PRODUCED — ALL FOUR MODEL-INVOLVING ARMS HALTED AT THEIR OWN GOVERNED FIRST SAFETY CHECKPOINT — A DURABILITY LIMITATION IN THE PRE-EXISTING OBSERVATION-PAYLOAD ENCODING IS DISCOVERED AND HONESTLY RECORDED, NOT CONCEALED
```

Explicit Execution Approval Review 5's own `AUTHORIZED` verdict is not invalidated: every mechanism it verified (90,000 ms timeout, intent-before-call durability, terminal timeout records, warm-up/scored-trial semantics, transport distinction, exact-once) is independently confirmed by this attempt's own real, live evidence to work exactly as governed. This attempt was not repaired, retried, or resumed after any checkpoint fired; campaign state is preserved exactly as produced.

---

# Attempt 6 — first campaign with genuinely, durably interpretable evidence at scale; four independent safety-checkpoint halts; one genuine model timeout

**New record, added by this task. Attempts 1–5 above are preserved exactly, unmodified — including Attempt 5's own honestly-recorded durability limitation, which this record does not retroactively rewrite.** Intervening work between Attempt 5 and this attempt (not itself a separate live-execution attempt): the Evidence Completeness and Durability Determination Review, its Independent Constitutional Review, the Observation Durability Defect Confirmation Review and its Independent Constitutional Review, the corrective implementation (`deba366`), the sixth-refresh Completion/Readiness reviews and their ICRs, and Explicit Execution Approval Review 6 (`AUTHORIZED`).

## D1. Baseline

Execution began with `HEAD` = `origin/main` = `aee4e4568096b0ed2df6a227730274f3104d1972`, clean, independently re-confirmed at task start. This differs from Approval Review 6's own examined commit (`deba3666839c2ad88f54e6582fc13cf1358ba088`) only by the addition of Approval Review 6's own document — independently re-confirmed via `git diff --quiet deba366..aee4e45 -- build.gradle.kts src tests` (exit code `0`) before execution began. `PARKER_REASONING_EVAL_REPOSITORY_COMMIT` was set to the true, current `aee4e45`, not the reviewed `deba366`, so the durable identity record reflects the actual code genuinely executed.

## D2. Authorization

Explicit Execution Approval Review 6, verdict `AUTHORIZED`, authorizing exactly one campaign under campaign ID `unit3c-remedy-experiments-20260810-03`, model `qwen2.5-coder:7b`, digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`, timeout `90000` ms, artifact parent `/var/lib/parker/reasoning-protocol-live-model`, frozen maximum schedule 483 live model calls.

## D3. Campaign ID

`unit3c-remedy-experiments-20260810-03`. Independently re-confirmed absent before this attempt (fresh `ls`/`stat` immediately prior to configuration export); now exists, containing genuine evidence (Section D9 onward).

## D4. Pre-execution checkpoint

Every item independently re-verified and **passed**, fresh, before configuration: repository (clean, `HEAD == origin/main == aee4e45`); commit reconciliation (Section D1); model `qwen2.5-coder:7b`, digest matching via fresh `/api/tags`; runtime `parker`/Ollama `0.32.5` via fresh `/api/version`; artifact root `steve:steve`/`0700`; free space `4,260,184,064` bytes (≥ 2 GiB, ≈1.99 GiB margin); no stale live environment variables present; campaign directory absent; offline suite fresh (`unit3cControlledRemedyExperiments`, 100 tests, 1 skip, 0 failures — re-run immediately before configuration export, matching every figure Approval Review 6 itself reported); schedule re-derived from source (3/145/220/115/0 = 483).

## D5. Configuration exported

`PARKER_REASONING_EVAL_ENDPOINT_URL=http://127.0.0.1:11434/api/generate`; `PARKER_REASONING_EVAL_MODEL_NAME=qwen2.5-coder:7b`; `PARKER_REASONING_EVAL_TIMEOUT_MS=90000`; `PARKER_REASONING_EVAL_OUTPUT_PATH=build/unit3c-legacy-output-unused`; `PARKER_REASONING_EVAL_REPOSITORY_COMMIT=aee4e4568096b0ed2df6a227730274f3104d1972`; `PARKER_REASONING_EVAL_MODEL_DIGEST=dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`; `PARKER_REASONING_UNIT3C_CAMPAIGN_ID=unit3c-remedy-experiments-20260810-03`; `PARKER_REASONING_UNIT3C_ARTIFACT_ROOT=/var/lib/parker/reasoning-protocol-live-model`. Exported into exactly one background shell invocation; that shell terminated when the command finished.

## D6. Exact command

```
./gradlew unit3cControlledRemedyExperiments --rerun-tasks --info
```

Run exactly once, in the background. `BUILD SUCCESSFUL in 58m 32s`. Not re-run, not invoked a second time, no trial manually replayed.

## D7. Execution state

All four model-invoking/classifying arms (Control, Family A, Family B, Family C) independently reached their own first governed adversarial-category false-positive safety checkpoint and halted. The warm-up sub-campaign sealed successfully (3/3). No arm sealed as fully complete; none was expected to under this outcome — the safety checkpoint's own governed behavior is to halt further live calls in the affected arm only, pending manual review, never to be silently cleared or retried.

## D8. Observation durability — genuinely confirmed at scale, not merely reachable

Independently sampled real `raw.jsonl` records from all five directories (warm-up, Control, Family A, Family B, Family C). Every completed observation durably carries: `campaignId`, `family`, `arm`, `fixtureId`, `fixtureCategory`, `contextProfileId`, `trialSequence`, `expectedAction`, `actualAction`, `semanticCorrect`, `representationValid`, `contentFidelity` (present as the literal `null` the object always holds — Section D25), model/digest identity, `runtimeIdentity`, `endpointIdentifier`, `timeoutMs`, `inferenceConfigIdentity`, `promptIdentity`, `parserResult`, `parserFailure`, `latencyNanos`, `transportOutcome`, `candidateMechanismIdentity`, `stableInputHash`, `repositoryCommit` — independently re-parsed from the raw on-disk text via a fresh script, not accepted from any in-memory assumption. **This is the corrective task's own intended effect, now independently confirmed under genuine live conditions at 282 completed observations, not 6 out of 179 as in Attempt 5.**

## D9. Transmitted/completed/timeout call accounting (durable evidence, independently derived)

| Arm | Intent (transmitted, model calls) | Raw (completed) | Timeouts | Outcome |
|---|---|---|---|---|
| Warm-up | 3 | 3 | 0 | SEALED |
| Control | 132 | 132 | 0 | SAFETY_CHECKPOINT |
| Family A | 51 | 50 | 1 | SAFETY_CHECKPOINT |
| Family B | 91 | 91 | 0 | SAFETY_CHECKPOINT |
| Family C | 0 (makes no model call) | 6 (classifier invocations) | 0 | SAFETY_CHECKPOINT |
| **Total** | **277 live model calls transmitted; 6 deterministic classifications** | **276 completed model responses + 6 classifications = 282** | **1 model timeout** | — |

Independently re-verified: every arm's `intent.jsonl` count equals its own `raw.jsonl` + `timeouts.jsonl` count exactly (132=132+0; 51=50+1; 91=91+0), confirming every transmitted call resolved to exactly one governed terminal state, with zero ambiguous-state (D) records anywhere. Zero duplicate trial IDs in any file. Zero overlap between any arm's `raw.jsonl` and `timeouts.jsonl`. Expected maximum schedule: 483. Actual transmitted (model calls): 277 (57.3% of the maximum — a substantially deeper run than Attempt 5's 36.4%) — halted at four independent, governed safety checkpoints before exhausting the full schedule; this is the correct, designed behavior for this outcome, not an incomplete or defective run.

## D10. Safety-checkpoint events — independently verified

| Arm | Triggering trial | Fixture category | Expected action | Actual action (durably recorded) |
|---|---|---|---|---|
| Control | `control/p17-hypothetical-remember/main/02` | ADVERSARIAL | REPLY | REMEMBER (false positive) |
| Family A | `family_a/p03-ambiguous-memory/decision/01` | ADVERSARIAL | REPLY | REMEMBER (false positive) |
| Family B | `family_b/g03-later-action/main/01` | GOAL | GOAL | REMEMBER (false positive) |
| Family C | `family_c/p03-ambiguous-memory/main/01` | ADVERSARIAL | REPLY | REMEMBER (false positive, matches the already-governed frozen Family C trace prediction exactly) |

Independently re-confirmed for every arm: the `SAFETY_CHECKPOINT` marker's own recorded `trialId` is the literal last entry in that arm's own `raw.jsonl`, and `intent.jsonl`'s count exactly equals `raw.jsonl` + `timeouts.jsonl`'s combined count — proving no trial was attempted in any arm after its own checkpoint fired. Each is independently confirmed a **first-occurrence adversarial-category false-positive REMEMBER** — no false-positive GOAL occurred anywhere in this campaign. **This is the first attempt in this programme's history where the triggering `actualAction` for every checkpoint is directly, durably read from the raw payload — not deduced, narrowed, or left ambiguous**, unlike Attempt 5, where Control's and Family A/B's own triggers could only be partially reconstructed by logical deduction from the trigger condition.

## D11. The one genuine model timeout — independently examined, including an honestly-recorded anomaly

`family_a/r02-please/render/02`: `terminalClassification: "MODEL_TIMEOUT"`, `transportDetail: "kotlinx.coroutines.TimeoutCancellationException"`, `responseBytesReceived: false`, `continuationDecision: "ARM_CONTINUED"`. The arm genuinely continued afterward — independently confirmed by 49 further Family A raw observations following this timeout in sequence before the arm's own eventual checkpoint. **Anomaly, recorded honestly rather than smoothed over:** the record's own `elapsedNanos` is `838234061456` (≈838.2 seconds, ≈13 minutes 58 seconds) — substantially longer than the governed `90,000` ms ceiling that nominally bounds it. Independently re-read `LocalHttpModelInferenceClient.infer` (`src/runtime/ModelInferenceClient.kt`): the HTTP call is genuinely cancellable (`suspendCancellableCoroutine` with `future.cancel(true)` wired via `invokeOnCancellation`), ruling out an obvious "non-cancellable blocking call" explanation. The most plausible account, **offered as a plausible explanation, not a confirmed root cause, exactly as this programme's own established discipline for Attempt 3's cold-start finding requires**: `elapsedNanos` is measured as wall-clock time from just before the call to the moment the exception is caught, and if the JVM's own coroutine delay-scheduling or the underlying HTTP client's cancellation signal was itself delayed by host-level resource contention (this host runs the Ollama inference process itself, competing for CPU with the JVM issuing the request), the *nominal* 90-second budget and the *actual* wall-clock delay before the exception is caught and durably recorded can diverge substantially. This is not diagnosed further and no code change is proposed or made based on it — outside this task's authorized scope. It does not change the governed classification (`MODEL_TIMEOUT`, `ARM_CONTINUED`) or the correctness of the continuation semantics, which durably, correctly recorded and handled the event regardless of its own real duration.

## D12. Campaign state

`unit3c-remedy-experiments-20260810-03` exists. Warm-up: `SEALED`, manifest `raw.sha256=ca8ac68d91cda4fa92e956f516901486679f3bdd6d34b33cc32fa5d45ea553a5`, independently re-verified by fresh re-hash of its own `raw.jsonl` — exact match. Control, Family A, Family B, Family C: each has a `SAFETY_CHECKPOINT` marker, none sealed, no manifest (correctly absent — manifests are written only on seal).

## D13. Artifact inventory

Per arm: `identity.txt` (all five, byte-identical apart from directory, all reading `aee4e4568096b0ed2df6a227730274f3104d1972|qwen2.5-coder:7b|dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364|http://127.0.0.1:11434/api/generate|90000`); `intent.jsonl` (absent for Family C, by design); `raw.jsonl`; `checkpoint.txt`; `SAFETY_CHECKPOINT` (Control/Family A/Family B/Family C); `timeouts.jsonl` (Family A only, 1 record); `manifest.txt` and `SEALED` (warm-up only).

## D14. Artifact hashes

Warm-up `raw.jsonl` → `ca8ac68d91cda4fa92e956f516901486679f3bdd6d34b33cc32fa5d45ea553a5`, independently re-derived and matching the durable manifest's own recorded value exactly.

## D15. Control descriptive results

132 transmitted, 132 completed, 0 timeouts. Semantic correctness: 84 correct / 48 incorrect / 0 null (representation validity 132/132, so every trial's `actualAction` is a real, parsed value). Representation validity: 132/132 valid, 0 parser failures. False-positive REMEMBER: 1 (`p17-hypothetical-remember`, the checkpoint trigger). False-positive GOAL: 0. False-negative REMEMBER (expected REMEMBER, actual something else): 15 of 15 REMEMBER-expected trials attempted (`r01-direct`, `r02-please`, `r03-dont-forget`, each attempted 5 times) — every one of Control's own REMEMBER-expected trials in this campaign resulted in a non-REMEMBER action.

## D16. Family A descriptive results

**Correction recorded in place, not silently fixed: an earlier draft of this section incorrectly stated the checkpoint fired "during the decision phase, before any rendering-step trial... occurred." Independent re-inspection of the actual `trialId` values (not merely the aggregate count) found this wrong — `Unit3CCampaignDefinition.familyATrials` schedules `decisionTrials + renderTrials` per fixture, not all decisions across the arm followed by all renders — so Family A's 51 transmitted trials span five complete fixtures (both sub-steps) plus one final decision-only trial, not a decision-only phase.**

51 transmitted, 50 completed, 1 model timeout (`family_a/r02-please/render/02` — Section D11). Trial sequence, in the frozen fixture order: `r01-direct` (decision ×5, render ×5 — all 10 complete); `r02-please` (decision ×5 complete, render ×4 complete + 1 timeout at render attempt 2); `r03-dont-forget` (decision ×5, render ×5 — all 10 complete); `p01-ordinary-fact` (decision ×5, render ×5 — all 10 complete); `p02-quoted-remember` (decision ×5, render ×5 — all 10 complete); `p03-ambiguous-memory` (decision ×1 — the checkpoint trigger, arm halted before its own render sub-step or any further trial).

**Decision-step only** (26 completed, the axis Plan Section 7 scores semantic correctness on): 14 correct / 12 incorrect. False-positive REMEMBER: 1 (`p03-ambiguous-memory`, the checkpoint trigger — the same fixture Family C's own deterministic mechanism also flags as its own predicted false positive). False-positive GOAL: 0. False-negative REMEMBER (a REMEMBER-expected decision that did not receive REMEMBER): 8 of the 13 REMEMBER-expected decision trials attempted (`r01-direct` 4/5, `r02-please` 1/5, `r03-dont-forget` 3/5).

**Render-step only** (24 completed, the axis Plan Section 7 scores content fidelity on — content fidelity itself is not computed by this experiment, Section D25): 24/24 semantically correct — every render-step trial, given its own fixture's correct decision as a stated given, produced output that parsed to the matching action tag. 0 representation failures.

Representation validity (both sub-steps combined): 50/50, 0 parser failures.

## D17. Family B descriptive results

91 transmitted, 91 completed, 0 timeouts. Semantic correctness: 60 correct / 31 incorrect / 0 null. Representation validity: 91/91, 0 parser failures. False-positive REMEMBER: 1 (`g03-later-action`, the checkpoint trigger — a GOAL-category fixture, its own negative control for REMEMBER). False-positive GOAL: 0. False-negative REMEMBER: 14 of 14 REMEMBER-expected trials attempted.

## D18. Family C descriptive results

0 model calls (by design); 6 classifier invocations recorded, all independently re-verified to reproduce the already-governed, frozen Candidate-C1 trace prediction exactly, in fixture order: `r01-direct` → REMEMBER (correct); `r02-please` → REMEMBER (correct); `r03-dont-forget` → no signal (the already-predicted false negative); `p01-ordinary-fact` → no signal (correct abstention); `p02-quoted-remember` → no signal (correct abstention); `p03-ambiguous-memory` → REMEMBER (the already-predicted false positive, and this arm's own checkpoint trigger). **Identical to Attempt 5's own Family C results, as expected of a deterministic, non-stochastic mechanism given the same, unchanged fixture set and processing order.**

## D19. Completion rates by arm

Warm-up: 100% (3/3 of schedule). Control: 91.0% of its own 145-trial schedule attempted (132/145), 100% completion of attempted calls (132/132). Family A: 23.2% of its own 220-trial schedule attempted (51/220), 98.0% completion of attempted calls (50/51, one timeout). Family B: 79.1% of its own 115-trial schedule attempted (91/115), 100% completion of attempted calls (91/91). Family C: 20.7% of its own 29-fixture schedule attempted (6/29), 100% completion (6/6, deterministic, no transport layer involved).

## D20. Timeout rates by arm

Control: 0/132 (0%). Family A: 1/51 (2.0%). Family B: 0/91 (0%). Family C: not applicable (no transport layer). Kept explicitly separate from semantic correctness, per the governed Scored-Trial Timeout Semantics Determination's own dual-purpose treatment (Section 3 there) — operational reliability, never merged into or averaged with semantic-correctness scoring.

## D21. Semantic correctness by arm (descriptive only — not a comparison, not a ranking)

Control: 84/132 (63.6%) of completed trials matched their own fixture's expected action. Family A, split by the two sub-steps Plan Section 7 itself scores separately (Section D16): decision step 14/26 (53.8%); render step 24/24 (100.0%, given the correct decision as a stated input). Family B: 60/91 (65.9%). Family C: 2/6 definitively correct, 1/6 definitively incorrect, 3/6 recorded as `null` (Family C's own schema convention for "no REMEMBER-signal produced," which is semantically correct for 2 of those 3 REPLY-expected fixtures and a genuine miss for the third, `r03-dont-forget` — the schema's own `semanticCorrect` field does not itself distinguish these two cases; this review states the distinction here as a description of the existing field's own known behavior, not a proposed schema change). **No ranking, comparison, or remedy-performance conclusion is drawn from these figures; they are reported descriptively, per this task's own explicit instruction.**

## D22. Representation validity by arm

Control 132/132 (100%). Family A 50/50 (100%). Family B 91/91 (100%). Family C 6/6 (100%, by construction — its own mechanism has no representation-validity failure mode). Zero parser failures anywhere in this campaign.

## D23. False-positive / false-negative counts

False-positive GOAL: 0 in every arm. False-positive REMEMBER: exactly 1 in each of the four arms (each is that arm's own checkpoint trigger, per the governed non-numeric, first-occurrence design). False-negative REMEMBER (a REMEMBER-expected trial that did not receive REMEMBER): Control 15/15 of its own attempted REMEMBER-expected trials; Family A 8/13 of its own attempted REMEMBER-expected **decision-step** trials (render-step trials are given the correct decision as an input and are not a test of whether the model would independently arrive at REMEMBER, so are excluded from this count); Family B 14/14; Family C 1/1 (`r03-dont-forget`, the already-known, already-governed predicted false negative).

## D24. Parser failures

Zero, in every arm, across all 282 completed observations.

## D25. `contentFidelity`

Durably recorded as literal `null` for all 282 completed observations, exactly as the corrective task's own Independent Constitutional Review anticipated (Readiness ICR Section 17, qualification 2): this reflects the field's own true, honest, never-computed value, not a durability gap re-introduced by this attempt. No fixture's content-fidelity expectation was evaluated by this campaign.

## D26. Exact-once result

Clean across the entire campaign — independently re-verified zero duplicate trial IDs, zero raw/timeout overlap, every intent resolved to exactly one terminal state, zero ambiguous (state D) records. No automatic retry occurred anywhere.

## D27. Downstream isolation

Unaffected; no code path this campaign's own execution exercised references any Memory/Goal/Planner/Knowledge-Submission symbol — independently re-confirmed via the unmodified forbidden-import tests (part of the same offline suite re-run before this attempt) and by the fact that no code was changed during or after execution.

## D28. Historical integrity

Independently hashed every file under Unit 2's, Unit 2-D's, Attempt 3's, and Attempt 5's own directories both immediately before configuration export and again after the campaign completed: **every hash line identical, byte-for-byte, with zero exceptions.** No historical artifact was resumed, modified, repaired, resealed, deleted, or otherwise touched. `unit3c-remedy-experiments-20260810-02` (Attempt 5) was not reused as this campaign's own ID.

## D29. Anomalies

The `elapsedNanos` discrepancy on the one genuine model timeout (Section D11) is the primary anomaly this attempt surfaces — real, honestly stated, offered as a plausible-but-unconfirmed explanation, out of this task's own authorized scope to diagnose further or fix. Secondary, informational only: this campaign progressed substantially further into its own schedule than Attempt 5 (57.3% vs. 36.4% of the 483-call maximum) before its own arms' respective checkpoints fired, consistent with different, later-occurring adversarial trigger fixtures in three of the four arms (only Family C's, being deterministic, reproduced identically).

## D30. Prohibited interpretations

This record must not be read as: a ranking or comparison of Control/Family A/Family B/Family C (none is performed); a selection of any remedy (none is selected); Unit 3-D analysis (not performed, reserved for a separate, future, explicitly-authorized task); a claim that the `elapsedNanos` anomaly (Section D11) is diagnosed or fixed (it is not); a claim that this campaign's own 91.0%/23.2%/79.1%/20.7% attempted-schedule fractions represent anything other than where each arm's own independent, governed safety checkpoint happened to fire this specific run.

## D31. Readiness for Unit 3-D

**Not determined by this record — reserved for the Independent Execution Evidence Review (below) and any future, separately-authorized Unit 3-D task**, consistent with this task's own explicit instruction that Unit 3-D readiness is Section 13's own separate question, not an execution-evidence-review finding.

## D32. Attempt 6 verdict

```text
EXECUTION OCCURRED — GENUINE, DURABLY INTERPRETABLE EXPLORATORY EVIDENCE PRODUCED AT SCALE — ALL FOUR MODEL-INVOLVING/CLASSIFYING ARMS HALTED AT THEIR OWN GOVERNED FIRST SAFETY CHECKPOINT — ONE GENUINE MODEL TIMEOUT, CORRECTLY CLASSIFIED AND CONTINUED — THE OBSERVATION-DURABILITY CORRECTION IS INDEPENDENTLY CONFIRMED WORKING UNDER REAL, LIVE CONDITIONS
```

Explicit Execution Approval Review 6's own `AUTHORIZED` verdict is not invalidated: every mechanism it verified (90,000 ms timeout, the corrected observation-durability encoding, intent-before-call durability, terminal timeout records, warm-up/scored-trial semantics, transport distinction, exact-once, the frozen 483-call schedule, experimental invariance) is independently confirmed by this attempt's own real, live evidence to work exactly as governed. This attempt was not repaired, retried, or resumed after any checkpoint fired; campaign state is preserved exactly as produced. No second campaign was run. No remedy was selected. Unit 3-D was not begun.
