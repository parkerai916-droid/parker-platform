**Status:** Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review — **NOT AUTHORIZED — CORRECTIVE ACTION REQUIRED.** A genuine, material implementation defect was discovered during this review's own independent re-derivation of the call schedule: the committed orchestration driver never executes the three frozen warm-up trials. No live model call, no HTTP call beyond the explicitly permitted read-only `/api/tags` and `/api/show` identity checks, no campaign directory creation, and no code/test/Gradle modification occurred during this review.

# Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review

## 1. Status

This is the final governance gate before live execution. It decides whether one specific Unit 3-C live campaign is authorized. It does not execute that campaign. **Verdict: not authorized, pending a narrow, identified correction.**

## 2. Authority

Controlled by the frozen Unit 3-C Scope Lock (`fee2edd`), the corrected frozen Implementation/Execution Plan (`08f3692`), the Family C Trace Defect Confirmation Review and its Independent Constitutional Review, and the committed implementation (`c38169c`) together with its Completion Review (PASS), Completion Independent Constitutional Review (ACCEPTED), Implementation Readiness Review (READY), and Readiness Independent Constitutional Review (ACCEPTED). This review does not reopen any of those documents' own findings except where its own fresh, independent re-derivation surfaces something none of them caught.

## 3. Repository baseline

`HEAD` and `origin/main` both independently confirmed `c38169c4086ed0edde9190257e2538462df8f757`; working tree clean at the start of this review. The committed implementation consists of exactly the files the prior task's own final report listed: `build.gradle.kts`, `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, `ReasoningProtocolUnit3COrchestrationTest.kt`, and the four review documents. Confirmed unchanged throughout this review (no edit was made).

## 4. Implementation identity

The `unit3cControlledRemedyExperiments` Gradle task is confirmed, by fresh re-read of the committed `build.gradle.kts`, to filter to exactly `ReasoningProtocolUnit3CControlledRemedyExperimentsTest` and `ReasoningProtocolUnit3COrchestrationTest`, use `shouldRunAfter(tasks.test)` (never `dependsOn`), and set `parker.reasoning.unit3c.enabled`. The `liveModelEvaluation` source set remains structurally detached from the ordinary `test` source set (`tests/contracts`, `tests/runtime`, `tests/composition` only). `./gradlew unit3cControlledRemedyExperiments --rerun-tasks` was re-run fresh during this review: 65 tests, 0 failures, 0 errors, 0 skipped, entirely offline.

## 5. Exact 483-call derivation — **material finding**

The call-total **as a number, derived from the schedule definition**, is independently re-confirmed 483: `Unit3CCampaignDefinition.liveModelCallCount` (its own `init`-time structural derivation) and the standalone from-scratch arithmetic test both independently agree, exactly as the Completion Review reported.

**However, this review went one step further than any prior review in this chain: it traced the schedule *definition* forward into the *orchestration driver's own executable code path*, and found a discrepancy.** `Unit3COrchestrationDriver.trialsFor(Unit3CFamily.CONTROL)` returns exactly `Unit3CCampaignDefinition.controlTrials` (145 trials) — it does **not** include `Unit3CCampaignDefinition.warmupTrials` (3 trials). An exhaustive search of `ReasoningProtocolUnit3COrchestrationTest.kt` — the file containing the only runnable driver — for the substring `warmup` (case-insensitive) returns **zero matches**. The three warm-up trials exist only in the abstract schedule list (`Unit3CCampaignDefinition.allTrials`, used solely to derive the *count* 483) and in one test asserting that count; no code path in the actual driver ever constructs, issues, or accounts for them as real calls.

**Consequence:** if the live campaign were run today, exactly as implemented, it would issue **480** real model calls (145 Control + 220 Family A + 115 Family B), not 483 — a silent, structural under-execution of the frozen schedule by precisely the warm-up count. This was not caught by the Completion Review, its Independent Constitutional Review, the Readiness Review, or the Readiness Independent Constitutional Review, because each of those verified the *count* 483 as a derivable number (correctly) without independently tracing whether the *runnable driver* actually reaches that count in practice — a distinction this final gate exists specifically to catch.

**Per Phase 4's own instruction, this is treated as a REJECT-triggering discrepancy between the derived total and what the committed instrument would actually execute.**

## 6. Family C corrected trace verification

Independently re-confirmed against committed source, fresh: 24/29 correct; false positives `P03`, `P04`, `P05`, `P12`; false negative `R03`. The mechanism, fixture texts, and expected actions are all unchanged since the frozen, corrected Plan; no attempt to alter the mechanism after seeing its predicted failures was found anywhere in the committed history. This matches the frozen governance exactly — no defect here.

## 7. Model identity

Read-only `/api/tags` call (permitted): `qwen2.5-coder:7b` is installed, digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364` — identical to the digest independently recorded during Unit 2-D's own diagnostic evidence earlier in this programme. No substitution, no drift. `llama3.2:3b` is also present but is not the configured model for any Unit 3-C arm and is not touched by anything in this campaign's design.

## 8. Runtime/container identity

Hostname `parker`; `Linux parker 7.0.0-29-generic #29-Ubuntu SMP PREEMPT_DYNAMIC Fri Jul 17 20:52:35 UTC 2026 x86_64`; Ollama version `0.32.5`. Read-only `/api/show` call for `qwen2.5-coder:7b` (permitted): raw response 50,696 bytes, SHA-256 `ca0bc98ad9b95b049d1b98a289fee7fbfc85a1973dfa9d267dfc84290b6551fd`. **Noted precisely, not as a defect:** the committed `Unit3CConfigLoader` does not itself require a separate `/api/show`-hash environment variable the way Unit 2-D's own, more elaborate `DiagnosticConfigLoader` did — it requires only the model digest via the shared `LiveEvaluationConfigLoader`. The `/api/show` hash captured here is recorded as available identity evidence for the record, not because committed code currently enforces it as a gate.

## 9. Inference configuration

Confirmed unchanged from frozen: `qwen2.5-coder:7b` only; request body exactly `{"model":..., "prompt":..., "stream":false}`, no `temperature`/`seed`/`top_p`/`top_k`/`num_predict`; timeout `30_000` ms (`UNIT_3C_TIMEOUT_MS`, enforced by `Unit3CConfigLoader`'s own `require`). No Family E leakage found anywhere in committed source.

## 10. Campaign identity

Proposed, consistent with the frozen `unit3c-remedy-experiments-` marker and machine-safe grammar: `unit3c-remedy-experiments-20260810`. **Not created.** Verified absent from the filesystem (Section 12).

## 11. Artifact-root verification

`/var/lib/parker/reasoning-protocol-live-model` exists, owned `steve:steve`, mode `700` at the top level — writable by the same user this review and any future live task would run as. Confirmed, by `readlink -f`, entirely outside `/home/steve/parker-platform` (the repository root) — no overlap with `build/reports`, `src`, `tests`, or `docs`. `Unit3CArtifactRootPolicy.resolve` (committed, tested) hard-restricts to exactly this parent and guards against resolving into either preserved campaign directory. The proposed campaign path (`.../unit3c-remedy-experiments-20260810`) resolves exactly once beneath the parent, with no traversal.

## 12. Disk-space verification

`df -B1` at the artifact-root mount point: **3,934,146,560 bytes available**, against the committed `2 GiB` (`2,147,483,648` bytes) minimum — passes with roughly 1.79 GiB of margin. The overall filesystem is at 87% utilization, worth noting for operational awareness, but the specific, governed check passes.

## 13. Unit 2 artifact-integrity result

Re-verified read-only: `stage-0/STAGE-0/raw.jsonl` freshly hashed to `c635ebcd051a7eeb02e154e3b07a4ba9e101fcd71f019bebc5990961f8179d5f`, exactly matching the value recorded in the campaign's own `manifest.txt`. `stage-0.failed` marker present; no `campaign.sealed` marker (correct — it never reached sealing). **Unchanged, preserved Stage 0 failure confirmed intact.**

## 14. Unit 2-D artifact-integrity result

Re-verified read-only: all four hashable artifacts (`production-track/raw.jsonl`, `candidate-track/raw.jsonl`, `warmup/raw.jsonl`, `manifest.txt`) freshly recomputed and compared against the campaign's own `artifact-hash-inventory.txt` — **all four match exactly, byte for byte.** `campaign.sealed` marker present. **Unchanged, sealed state confirmed intact.**

## 15. Family C safety-checkpoint verification

Independently re-confirmed wired into the actual execution path (not merely defined and tested in isolation): `runArm` calls `isAdversarialCategoryFalsePositive` immediately after appending each observation (line 231 of the committed orchestration file) and, on a true result, calls `checkpoint.trigger(...)` and returns `SAFETY_CHECKPOINT` without proceeding to further trials in that arm. The triggering observation is preserved (appended before the check runs). Sealing is structurally blocked while the checkpoint marker file exists. No code path anywhere clears the marker automatically — `clearForExplicitlyAuthorizedContinuation()` is a distinct, separately-named method invoked only by its own dedicated test. This mechanism cannot itself cause a model call — it only inspects an already-received observation.

## 16. Exact-once/durability verification

Independently re-confirmed wired into `runArm`: identity check before any trial; duplicate prevention via the ledger's recovered completed-set; raw-append before checkpoint write; checkpoint-without-raw and campaign/model identity drift both throw `Unit3CArtifactIntegrityException`, caught by the driver and converted to a `HALTED` arm result rather than a process crash; seal requires the full registered trial set; no code path reruns a completed trial. All thirteen original ledger tests plus the orchestration-level tests re-run fresh and pass.

## 17. Downstream-isolation verification

Re-confirmed absolute: an exhaustive `grep` for `Memory`, `Goal`, `Planner`, `Tool`, `Knowledge`, and `parker.composition` across every import line in both committed files returns nothing. `Unit3CCandidateC1.classify`'s output is routed only into an `Unit3CObservation` record; no consequential API is reachable from either file.

## 18. Live-bridge-unexercised determination

**Superseded by the Section 5 finding.** The Readiness Review and its Independent Constitutional Review (prior task) reasoned that the frozen warm-up calls, already part of the single authorized 483-call schedule, were the appropriate, already-governed mitigation for the live bridge's necessarily-unexercised status — Option A, "acceptable and expected before Explicit Execution Approval." **That reasoning assumed the warm-ups would actually execute.** This review's own independent trace of the driver's code confirms they do not. Given this, the correct determination is **B — a blocking readiness defect**, specifically located in the gap between the schedule's own definition and the driver's own execution of it, not in the broader question of whether an unexercised bridge is acceptable in principle. No preflight requirement is invented here beyond what the frozen Plan already specifies (Section 11's own three warm-up calls) — the finding is that the *already-frozen* requirement is not yet correctly implemented, not that a *new* requirement is needed.

## 19. Exact execution configuration (recorded for the future, correct authorization; not exported or activated during this review)

| Key | Value |
|---|---|
| Repository commit | `c38169c4086ed0edde9190257e2538462df8f757` |
| `PARKER_REASONING_EVAL_ENDPOINT_URL` | `http://127.0.0.1:11434/api/generate` (confirmed reachable via `/api/tags`) |
| `PARKER_REASONING_EVAL_MODEL_NAME` | `qwen2.5-coder:7b` |
| `PARKER_REASONING_EVAL_MODEL_DIGEST` | `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364` |
| `PARKER_REASONING_EVAL_TIMEOUT_MS` | `30000` |
| `PARKER_REASONING_EVAL_OUTPUT_PATH` | (a path beneath the same accepted artifact parent; exact value not fixed by this review) |
| `PARKER_REASONING_EVAL_REPOSITORY_COMMIT` | `c38169c4086ed0edde9190257e2538462df8f757` |
| `PARKER_REASONING_UNIT3C_CAMPAIGN_ID` | `unit3c-remedy-experiments-20260810` |
| `PARKER_REASONING_UNIT3C_ARTIFACT_ROOT` | `/var/lib/parker/reasoning-protocol-live-model` |
| Runtime/container identity (for the record) | hostname `parker`; Ollama `0.32.5`; `/api/show` SHA-256 `ca0bc98ad9b95b049d1b98a289fee7fbfc85a1973dfa9d267dfc84290b6551fd` |

None of these values was exported, set, or otherwise activated during this review.

## 20. Authorization boundary (for the future, correct approval, once granted)

If and when authorized, approval must be limited to exactly: one campaign ID (`unit3c-remedy-experiments-20260810`, or whatever value is current at the time correction and re-review are complete); one repository commit; one model digest; one runtime identity; one artifact parent; the one frozen 483-call schedule (with warm-ups genuinely executed first); zero remedy modification during execution; no repeat campaign without new authority; no continuation after a governed halt without new authority; no Unit 3-D evaluation; no remedy selection. This review does not itself grant that authority.

## 21. Prohibited actions confirmed not taken

No `/api/generate` call. No live Gradle task invocation. No campaign directory created. No code, test, or Gradle modification. No fixture, Family A/B/C definition, repetition count, or call-schedule alteration. No remedy selected. No Unit 3-D work performed. Nothing staged, committed, or pushed.

## 22. Final verdict

```text
NOT AUTHORIZED — CORRECTIVE ACTION REQUIRED
```

**Exact blocking reason:** `Unit3COrchestrationDriver.trialsFor(Unit3CFamily.CONTROL)` must include `Unit3CCampaignDefinition.warmupTrials` (or the driver must otherwise execute all three warm-up trials, unscored, before any scored trial in any arm), so that a live run genuinely issues 483 calls — not 480 — and so that the warm-up-first bridge verification the Readiness Review relied upon as its governing rationale for proceeding actually occurs in practice. This is a narrow, precisely located code change confined to the orchestration driver's trial-selection logic; it does not require altering the frozen schedule definition, the fixture corpus, any Family A/B/C mechanism, the repetition counts, or any governance document. Once corrected, the Completion Review, its Independent Constitutional Review, the Readiness Review, and its Independent Constitutional Review should each be refreshed to independently re-verify the fix (mirroring exactly how the prior Family C trace defect was handled), and this Explicit Execution Approval Review must be repeated fresh — not merely amended — before any live call may occur.
