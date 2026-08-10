**Status:** Unit 3-C Controlled Remedy Experiments — Execution Evidence Review — **EXECUTION DID NOT OCCUR. A blocking, structural pre-execution defect was discovered while configuring the authorized campaign, before any live call was attempted.** This document is a halt report, not a record of campaign results — no campaign ran. No `/api/generate` call occurred. No campaign directory was created. No code, test, or Gradle file was modified.

# Unit 3-C Controlled Remedy Experiments — Execution Evidence Review

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

## 28. Verdict

```text
EXECUTION DID NOT OCCUR — BLOCKING STRUCTURAL DEFECT DISCOVERED, CORRECTIVE ACTION REQUIRED
```

The Explicit Execution Approval Review 2's own `AUTHORIZED` verdict is not itself invalidated by this finding — the campaign it authorized remains authorized in principle, under the same boundary, once the missing trigger mechanism is added and passes fresh review. This document records that the attempt to actually exercise that authorization, in this task, discovered the authorized mechanism does not yet exist in committed code, and that this task correctly stopped rather than working around that gap.
