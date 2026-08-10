**Status:** Unit 3-C Controlled Remedy Experiments — Execution Evidence Review — **NO COMPLETE CAMPAIGN HAS YET PRODUCED EVIDENCE, across three independent attempts.** Attempt 1 discovered the live-execution trigger did not exist (corrected in a later task). Attempt 2, using the corrected trigger, discovered the disk-space gate checked a not-yet-created path (corrected in a later task). **Attempt 3 (new, appended below) is the first attempt in this programme's history to genuinely reach a real `/api/generate` call** — every gate before it (trigger, config validation, artifact-root validation, disk-space gate, ledger identity check) passed for the first time, and a campaign directory now exists on disk for the first time. The live call itself did not complete within the frozen 30,000 ms timeout; the resulting exception propagated uncaught and crashed the task before any observation was durably recorded. Zero observations exist; zero arms sealed; Unit 3-D evidence remains not yet available. No code, test, or Gradle file was modified during any of the three attempts' own governance tasks.

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
