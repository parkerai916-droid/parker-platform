**Status:** Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review (2) — **AUTHORIZED.** This document authorizes exactly one future campaign execution and does not itself execute it. The first Explicit Execution Approval Review (`docs/reviews/..._EXPLICIT_EXECUTION_APPROVAL_REVIEW.md`) is preserved unmodified as the historical record of the warm-up orchestration defect it discovered; this is a wholly fresh review, not an edit of it. No live model call, no HTTP call beyond the explicitly permitted read-only `/api/tags` and `/api/show` identity checks, and no campaign directory creation occurred during this review.

# Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review (2)

## 1. Status

Final governance gate before live execution, re-performed fresh following the warm-up orchestration defect's correction and the subsequent refresh of the Completion Review (PASS), Completion Independent Constitutional Review (ACCEPTED), Implementation Readiness Review (READY), and Readiness Independent Constitutional Review (ACCEPTED). This review does not re-litigate any of those documents' own findings; it independently re-verifies the specific facts an execution-approval decision depends on.

## 2. Authority

Controlled by the frozen Unit 3-C Scope Lock, the corrected frozen Implementation/Execution Plan (`08f3692`), the Family C Trace Defect review chain, the Warm-Up Orchestration Defect Confirmation Review and its Independent Constitutional Review (this task), and the twice-refreshed Completion/Readiness review chain (this task). Repository baseline: `2dca60282043a0fc38a87908da9507ca5bcd4e77` (HEAD = origin/main at the start of this task), with the warm-up correction and this task's own governance documents present in the working tree, uncommitted, exactly as this task's own instructions require.

## 3. Repository baseline

`HEAD` independently re-confirmed `2dca60282043a0fc38a87908da9507ca5bcd4e77`. Working tree contains, uncommitted: the corrected `ReasoningProtocolUnit3COrchestrationTest.kt`; the four refreshed review documents; the two new Warm-Up Orchestration Defect review documents; and this document itself. No other file is modified. `git diff --stat -- src/` remains empty throughout.

## 4. Implementation identity

`unit3cControlledRemedyExperiments` filters to exactly the two Unit 3-C test classes; `liveModelEvaluation` remains structurally detached from `test`/`check`/`build`; `Unit3CLiveEntryPoint.run` requires complete `PARKER_REASONING_EVAL_*` and `PARKER_REASONING_UNIT3C_*` configuration, absent by default. `./gradlew unit3cControlledRemedyExperiments --rerun-tasks` re-run fresh for this review: 69 tests, 0 failures, 0 errors, 0 skipped.

## 5. Exact 483-call derivation — corrected and re-verified

**483, now independently re-confirmed genuinely executable, not merely arithmetic.** `exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups` was re-run fresh for this review: a counting fake executor confirms exactly 483 total invocations across Control (148, including 3 warm-ups), Family A (220), and Family B (115), with Family C's 0 confirmed structurally. This directly closes the specific finding the first Explicit Execution Approval Review made. `runWarmups` is independently re-confirmed, by fresh source read, to be called unconditionally at the start of `runArm` for Control, before `trialsFor(family)` is evaluated for that arm.

## 6. Family C corrected trace verification

Re-confirmed unchanged and unaffected by the warm-up correction: 24/29 correct; false positives `P03`, `P04`, `P05`, `P12`; false negative `R03`. Fixture texts and expected actions independently re-confirmed unchanged (the schedule/mechanism file was not touched by this task's diff at all).

## 7. Model identity

Fresh read-only `/api/tags` call: `qwen2.5-coder:7b` installed, digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364` — identical to the digest recorded in the first Approval Review and to Unit 2-D's own diagnostic evidence. No drift, no substitution.

## 8. Runtime/container identity

Fresh read-only `/api/show` call for `qwen2.5-coder:7b`: raw response 50,696 bytes, SHA-256 `ca0bc98ad9b95b049d1b98a289fee7fbfc85a1973dfa9d267dfc84290b6551fd` — byte-for-byte identical to the first Approval Review's own capture, confirming the runtime is genuinely stable across the intervening work, not merely re-asserted. Hostname `parker`; Ollama version `0.32.5`, unchanged.

## 9. Inference configuration

Re-confirmed unchanged: `qwen2.5-coder:7b` only; no sampling parameters; 30,000 ms timeout; production request-body shape. No Family E leakage anywhere in the corrected source.

## 10. Campaign identity

`unit3c-remedy-experiments-20260810`, unchanged from the first review, re-confirmed valid against the frozen marker and machine-safe grammar.

## 11. Artifact-root verification

Fresh listing of `/var/lib/parker/reasoning-protocol-live-model/`: unchanged, contains only the two preserved Unit 2/Unit 2-D directories. `Unit3CArtifactRootPolicy` (unaffected by this task's correction) re-confirmed hard-restricted to this exact parent, with nesting guards against both preserved directories.

## 12. Disk-space verification

Fresh `df -B1`: **3,933,016,064 bytes available** at the artifact-root mount point — a negligible, expected change from the first review's reading (3,934,146,560 bytes), consistent with ordinary system activity in the interim, and still well above the governed 2 GiB (2,147,483,648 bytes) minimum with over 1.78 GiB of margin.

## 13. Unit 2 artifact-integrity result

Fresh re-hash: `stage-0/STAGE-0/raw.jsonl` → `c635ebcd051a7eeb02e154e3b07a4ba9e101fcd71f019bebc5990961f8179d5f`, identical to the manifest's own recorded value and to the first review's own capture. Preserved Stage 0 failure, unchanged.

## 14. Unit 2-D artifact-integrity result

Fresh re-hash of all four artifacts (`production-track/raw.jsonl`, `candidate-track/raw.jsonl`, `warmup/raw.jsonl`, `manifest.txt`): all four identical to the campaign's own recorded inventory and to the first review's own capture. Sealed state, unchanged.

## 15. Safety-checkpoint verification

Re-confirmed unaffected by the warm-up correction, since the checkpoint logic operates only on scored trials, downstream of the (now-corrected) warm-up gate: non-numeric, first-occurrence trigger; preserves the triggering observation; blocks sealing; no auto-clear. Re-run fresh: passes.

## 16. Exact-once verification

Re-confirmed, and now additionally proven for the warm-up step specifically (which the first review could not confirm, since it did not yet exist as an execution path): intent-before-call, durable raw persistence, checkpoint ordering, duplicate prevention, crash-recovery idempotence, identity-drift protection, and arm-scoped fail-closed behavior all independently re-verified for warm-ups via this task's four new tests, re-run fresh for this review.

## 17. Downstream-isolation verification

Re-confirmed absolute via a fresh, exhaustive import search across both files, including the new `runWarmups` code: no forbidden symbol found.

## 18. Live-bridge-unexercised determination — re-derived, not carried forward

**A — acceptable and expected before Explicit Execution Approval.** This determination is different from the first review's own Section 18 finding (which reclassified this to "B — blocking," specifically because the warm-up mitigation the "acceptable" reasoning depended on turned out not to be wired in). That specific defect is now corrected and independently re-verified (Sections 5 and 16 above): the three warm-ups genuinely execute, first, before any of the 480 scored/experimental calls, using the same shared bridge every other live-calling arm depends on. The real HTTP execution path itself remains, as it structurally must under this task's own no-live-calls constraint, unexercised by any test — but the mitigation this review relies on to accept that residual fact is no longer an unverified assumption.

## 19. Exact execution configuration

Unchanged from the first review's own Section 19 table, re-confirmed current: repository commit `2dca60282043a0fc38a87908da9507ca5bcd4e77` (or the commit resulting once this task's own corrections are committed — see Section 20); endpoint `http://127.0.0.1:11434/api/generate`; model `qwen2.5-coder:7b`; digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`; timeout `30000`; campaign ID `unit3c-remedy-experiments-20260810`; artifact root `/var/lib/parker/reasoning-protocol-live-model`. None of these values was exported or activated during this review.

## 20. Authorization boundary

This review authorizes exactly: **one campaign ID** (`unit3c-remedy-experiments-20260810`); **one repository commit** — the commit that results once this task's own working-tree corrections (the warm-up fix and this governance chain) are committed, matching the exact source this review examined, not a different or later state; **one model digest** (`dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`); **one runtime identity** (the current Ollama instance on host `parker`, version `0.32.5`); **one artifact parent** (`/var/lib/parker/reasoning-protocol-live-model`); **the one frozen, now-genuinely-executable 483-call schedule**; **the frozen Family A/B/C mechanisms exactly as committed, with zero post-hoc modification during execution**. No repeat campaign under this authorization. No continuation past any governed halt (measurement-invalidating failure or safety checkpoint) without new authority. No Unit 3-D evaluation. No remedy selection. This authorization lapses if the repository commit executed against differs from the one this review examined.

## 21. Prohibited actions confirmed not taken

No `/api/generate` call. No live Gradle task invocation. No campaign directory created. No environment variable exported in a way that would activate execution. No code, test, or Gradle modification beyond what the correction task itself already performed and this review examined read-only. No remedy selected. No Unit 3-D work performed. Nothing staged, committed, or pushed by this review.

## 22. Final verdict

```text
AUTHORIZED
```

**This document authorizes exactly one future campaign execution, under the exact boundary stated in Section 20, and does not itself execute it.** The warm-up orchestration defect that blocked the first Explicit Execution Approval Review is independently confirmed corrected: the schedule's own arithmetic and the orchestration driver's own measured execution both now genuinely agree at 483 calls, including all three warm-ups, in frozen order, exactly once. All other approval dimensions — model identity, runtime identity, inference configuration, campaign identity, artifact-root and disk-space verification, Unit 2 and Unit 2-D artifact integrity, the safety checkpoint, exact-once durability, and downstream isolation — are independently re-verified fresh and found sound. The next step, outside this review's own scope, is committing the corrected implementation and this governance chain, then proceeding to actual execution under the exact, narrow boundary this document establishes.
