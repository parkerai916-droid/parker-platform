**Status:** Unit 2-D Explicit Execution Approval Review — **LIVE EXECUTION: AUTHORIZED**, narrowly, for exactly one campaign. Governance/readiness review against committed baseline `705f9f5`. Read-only `/api/tags` and `/api/show` identity checks were performed and are reported in full below (Section 10). **Zero `/api/generate` calls occurred.** No campaign directory was created. No production/test/Gradle file changed. Nothing is staged, committed, or pushed.

# Unit 2-D Diagnostic Characterisation — Explicit Execution Approval Review

## 1. Status

This is the fourth and final gate the frozen Implementation/Execution Plan's own Section 17 names (Independent Constitutional Review of the Plan — accepted; Implementation Readiness Review and its Independent Constitutional Review — both accepted; explicit execution approval — this document). It is not the live execution task itself; no campaign runs as a result of this review.

## 2. Authority and committed baseline

HEAD independently verified `705f9f5cf760fe8d9633ecd95b30fd72a37074ba`, equal to `origin/main`, working tree clean before this review began. This is the exact commit `git show --stat` confirms contains the full Unit 2-D implementation (`build.gradle.kts`, `tests/integration/ReasoningProtocolDiagnosticCharacterisationTest.kt`) and all four prior review documents (Completion Review, its Independent Constitutional Review, Implementation Readiness Review, its Independent Constitutional Review).

## 3. Evidence reviewed

Read fresh: the frozen Scope Lock and its Independent Constitutional Review; the frozen Implementation/Execution Plan and its correction Independent Constitutional Review; the Completion Review and its Independent Constitutional Review; the Implementation Readiness Review and its Independent Constitutional Review; `build.gradle.kts`'s `reasoningProtocolUnit2DDiagnostic` block; the full `DiagnosticConfigLoader`, `DiagnosticCampaignDefinition`, `DiagnosticCampaignRunner`, and `DiagnosticIdentityEvidence` source directly; Unit 1's `LiveEvaluationConfigLoader`; Unit 2's `enforceStageGates`/`DurableCampaignRunner.runBatch` (specifically re-read to resolve Check 4, Section 7 below); and the failed Unit 2 campaign's preserved artifacts, read-only, on the Ubuntu host.

## 4. Implementation integrity (Check 1)

Independently re-verified, not assumed from the prior reviews: `./gradlew compileLiveModelEvaluationKotlin` — clean. `./gradlew reasoningProtocolUnit2DDiagnostic --rerun` — 27 tests, 1 skipped (the live entry point itself), 0 failures, 0 errors. `git diff --stat b34f8d0 HEAD -- src/` — empty; `src/**` has never been touched anywhere in this programme. The compiled-in schedule invariant (`check()` inside `DiagnosticCampaignDefinition.init`) still enforces exactly 24 trials: 2 warm-up, 10 DQ1, 4 DQ2, 2 DQ3, 1 DQ4, 5 DQ5, DQ6 contributing zero. No retry logic exists anywhere in `DiagnosticCampaignRunner` or `executeCandidateTrial`. `TaggedReasoningResponseParser`, `Goal`, `Reply`, `Remember`, `NoAction` remain unmodified (confirmed by the empty `src/` diff); the corrected DQ5 forms (`GOAL: SELECTED`, `REPLY: SELECTED`, `REMEMBER: SELECTED`, `NOACTION`) still parse correctly per the passing `all four DQ5 action forms parse through the unmodified TaggedReasoningResponseParser` test. The committed implementation matches every accepted review exactly — no material drift found.

## 5. Task isolation (Check 2)

`reasoningProtocolUnit2DDiagnostic` requires explicit selection by name; it is filtered to `parker.integration.ReasoningProtocolDiagnosticCharacterisationTest` only, sets its own JVM property, and carries no `dependsOn` reference anywhere in `build.gradle.kts`. Independently re-ran `./gradlew check` — completes without executing `reasoningProtocolUnit2DDiagnostic`, `reasoningProtocolBaselineCharacterisation`, or `reasoningProtocolLiveModelEvaluation`, confirming detachment behaviorally, not merely textually.

## 6. Configuration contract (Check 3)

**Reused from Unit 1's `LiveEvaluationConfigLoader`** (required unless noted): `PARKER_REASONING_EVAL_ENDPOINT_URL` (must be a valid `http`/`https` URI with no embedded credentials/query/fragment — no default); `PARKER_REASONING_EVAL_MODEL_NAME` (no default; Unit 2-D additionally requires this to equal exactly `qwen2.5-coder:7b`); `PARKER_REASONING_EVAL_TIMEOUT_MS` (no default; must be `>0`, and Unit 2-D additionally requires exactly `90000`); `PARKER_REASONING_EVAL_OUTPUT_PATH` (no default; must resolve outside `src`/`tests`/`docs` — **not otherwise read by Unit 2-D's own driver**, which uses its own ledger paths; required only because it is part of the reused Unit 1 loader's completeness check); `PARKER_REASONING_EVAL_REPOSITORY_COMMIT` (no default); `PARKER_REASONING_EVAL_MODEL_DIGEST` (no default; required non-null for Unit 2-D); `PARKER_REASONING_EVAL_RUNTIME_IMAGE_ID` (optional, no default value substituted — remains `null` if absent). `PARKER_REASONING_EVAL_REPETITIONS` is the **one genuine default** anywhere in the reused chain — `?: 1` if absent — and is never read by any Unit 2-D logic (the 24-call schedule is fixed independent of it).

**Unit 2-D-specific** (all required, all in `DiagnosticConfigLoader`, none with a default): `PARKER_REASONING_DIAGNOSTIC_CAMPAIGN_ID`, `PARKER_REASONING_DIAGNOSTIC_ARTIFACT_ROOT` (must equal exactly `/var/lib/parker/reasoning-protocol-live-model`, no normalization escape permitted), `PARKER_REASONING_DIAGNOSTIC_UBUNTU_RUNTIME_ID`, `PARKER_REASONING_DIAGNOSTIC_QWEN_MODEL_SHOW_SHA256` (must match `[0-9a-fA-F]{64}`), `PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_NAME` (must equal exactly `llama3.2:3b`), `PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_DIGEST`, `PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_SHOW_SHA256` (same hash-format check). `PARKER_REASONING_DIAGNOSTIC_CONTAINER_ID` is the one **optional** Unit 2-D value — absent resolves to an empty string, folded into the identity fingerprint as such, not omitted.

**No implicit execution-approval value exists anywhere in this contract** — confirmed by direct source inspection; this is the exact gap Check 4 resolves next.

## 7. Execution approval mechanism determination (Check 4)

Independently re-examined, not assumed from the prior Readiness Independent Constitutional Review's framing of this as an open "non-blocking qualification."

Re-read Unit 2's own `enforceStageGates` directly: `require(stageZeroApproved && scoredApproved)` fires **only** `if (batch.stage.scored)`. `CampaignStage.STAGE_0` carries `scored = false`. Every one of Unit 2's own offline tests invokes `runBatch("STAGE-0", false, false) { ... }` — both approval parameters `false` — and Stage 0 runs to completion regardless. **Unit 2 itself never required a code-level approval flag for its own first eleven live calls.** The two-flag pattern was gating one thing only: the escalation from an unscored, bounded preflight into a scored, 3,900-trial, largely irreversible statistical commitment — an internal escalation Unit 2-D has no equivalent of. Unit 2-D's entire 24-call campaign is, in scale, reversibility, and constitutional character, the direct analogue of Unit 2's own *ungated* Stage 0, not of its gated scored stages.

Independently re-checked the frozen Scope Lock and Plan for any Unit-2-D-specific or programme-wide requirement mandating a code-level flag: none exists. Plan Section 17 requires "explicit execution approval" as a governance act in a defined sequence, not as a specified software mechanism. `PARKER_ENGINEERING_STANDARD.md` and the programme-level Planning Review were independently searched for a general cross-unit principle; none was found.

Weighed against this: the real, acknowledged cost of the gap (nothing prevents an operator who already has a complete, valid configuration from running the live task without this document ever having been consulted). This is a genuine, not merely nominal, absence of mechanical enforcement — but it is not a deviation from Parker's own demonstrated practice at this scale; it is a faithful, evidence-grounded application of it once Unit 2-D's stage-0-equivalent status is properly weighed rather than assumed.

```text
EXECUTION APPROVAL MECHANISM:
GOVERNANCE-LEVEL APPROVAL SUFFICIENT
```

This document, once reaching a final determination, plus deliberate operator assembly of the ~14-value configuration contract in Section 6 (itself a significant, hard-to-stumble-into act — see Section 20), together constitute the "explicit execution approval" the frozen Plan requires.

## 8. Unit 2 isolation (Check 5)

Read-only, this session: `stage-0.failed`, `manifest.txt`, `raw.jsonl`, `campaign-definition.txt`, and `campaign-identity.txt` hashes for `qwen25coder7b-baseline-20260809` all match every prior verification throughout this entire programme, byte-for-byte. `stage-0.sealed` remains absent. `DiagnosticCampaignDefinition`'s constructor and `DiagnosticConfigLoader.campaignArtifactRoot` both independently reject the Unit 2 campaign ID and any path resolving inside its directory; `DiagnosticCampaignRunner.run`'s first executable statement repeats the check a third time for any direct construction path. Unit 2-D campaign creation cannot mutate or reinterpret Unit 2 state — confirmed structurally, not merely by convention.

## 9. New campaign identity (Check 6)

```text
qwen25coder7b-llama32-3b-diagnostic-20260809
```

Machine-safe (`[a-z0-9][a-z0-9.-]*`), contains the mandatory `diagnostic` marker, distinct from `qwen25coder7b-baseline-20260809`, and — verified this session — no directory of this name or containing `diagnostic` exists anywhere under `/var/lib/parker/reasoning-protocol-live-model/`. Not created during this review. This exact ID, and no other, is what the authorization in Section 21 covers.

## 10. Model identity (Check 7)

Read-only identity verification was performed, exactly as this task permits. **Calls made: one `GET /api/tags`, two `POST /api/show`. Zero `POST /api/generate` calls.**

```text
GET http://127.0.0.1:11434/api/tags  ->  200 OK
  qwen2.5-coder:7b  digest: dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364
  llama3.2:3b       digest: a80c4f17acd55265feec403c7aef86be0c25983ab279d83f3bcd3abbcb5b8b72
  (exactly these two models present; no third model installed)

POST http://127.0.0.1:11434/api/show {"model":"qwen2.5-coder:7b"}  ->  200 OK, 50696 bytes
  sha256 of raw response body: ca0bc98ad9b95b049d1b98a289fee7fbfc85a1973dfa9d267dfc84290b6551fd

POST http://127.0.0.1:11434/api/show {"model":"llama3.2:3b"}  ->  200 OK, 49678 bytes
  sha256 of raw response body: ef2f477fd7a923b2209082378173bfeda9d91201d3970c12c20bd283179bcb04
```

The Qwen digest was independently cross-checked against the digest recorded in Unit 2's original, preserved `raw.jsonl` (`grep -o '"modelDigest":"[^"]*"'`): **identical**. The Qwen model has not changed since Unit 2's campaign ran. Both digests are well-formed 64-character hex strings, satisfying `DiagnosticIdentityEvidence.exactModelDigest`'s own validation unmodified. Current model identity does not differ from the frozen assumptions — no stop condition triggered.

No model file, weight, or configuration was altered by these calls; both are read-only per the Ollama API contract, and no other endpoint was touched.

## 11. Runtime identity (Check 8)

```text
Repository commit:  705f9f5cf760fe8d9633ecd95b30fd72a37074ba
Hostname:           parker
OS:                 Ubuntu 26.04 LTS (Resolute Raccoon), kernel 7.0.0-29-generic
Ollama container:   docker, name "ollama", image ollama/ollama:latest,
                    container ID f795e46c5eac, up 31h, port 11434 mapped
Endpoint:           http://127.0.0.1:11434 (/api/generate, /api/tags, /api/show)
Timeout:            90000 ms (Scope Lock 17 / Plan Section 5, frozen)
```

None of these values were invented; all were captured this session via read-only `hostname`, `uname -a`, `/etc/os-release`, and `docker ps`.

## 12. Artifact-root verification (Check 9)

`/var/lib/parker/reasoning-protocol-live-model/` exists, owned `steve:steve`, mode `700` (owner-only) — suitable, matching the ownership/permission pattern the preserved Unit 2 campaign already uses. Its only current child is `qwen25coder7b-baseline-20260809/`. The proposed campaign directory (Section 9) does not exist. Nothing would be overwritten. No campaign directory was created during this review — verified immediately before and after every check in this document.

## 13. Disk-space verification (Check 10)

Implementation's exact threshold (`DIAGNOSTIC_MINIMUM_FREE_BYTES`, read directly from source): `2L * 1024L * 1024L * 1024L` = 2,147,483,648 bytes. Current usable space on the filesystem hosting the artifact parent (`df --output=avail -B1`): 3,946,815,488 bytes (≈3.68 GiB). **Sufficient** — no arbitrary threshold substituted; the exact implemented value was read from source and compared directly.

## 14. Exact 24-call schedule (Check 11)

Independently reconstructed from `DiagnosticCampaignDefinition`'s actual `init` block, matching its compiled-in `check()` invariants and the passing schedule tests:

```text
01     WARMUP-QWEN  / warmup-acknowledgement       / minimal-production-context / qwen  / 01
02-11  DQ1          / r01-direct                   / minimal-production-context / qwen  / 01..10
12     DQ2          / p01-ordinary-fact             / minimal-production-context / qwen  / 01
13     DQ2          / p06-greeting                  / minimal-production-context / qwen  / 01
14     DQ2          / g01-multistep                 / minimal-production-context / qwen  / 01
15     DQ2          / n01-heartbeat                 / minimal-production-context / qwen  / 01
16     DQ3          / r01-direct                    / mixed-full-production-like / qwen  / 01
17     DQ3          / r01-direct                    / conversation-history       / qwen  / 01
18-22  DQ5          / r01-direct-decision-only      / minimal-production-context / qwen  / 01..05
23     WARMUP-LLAMA / warmup-acknowledgement        / minimal-production-context / llama / 01
24     DQ4          / r01-direct                    / minimal-production-context / llama / 01
```

One model transition, at the very end (calls 1–22 Qwen, 23–24 Llama). DQ6 contributes zero calls — a cross-cutting analysis over the 22 non-warm-up observations via Unit 1's own unmodified `deriveCrossTrialObservations`, computed only after the campaign completes.

## 15. Stop semantics (Check 12)

Independently re-confirmed by direct reading of `DiagnosticCampaignRunner.run` and by the passing tests exercising both branches: semantic wrong action, representation-valid semantic disagreement, and repeated semantic failure (including all ten DQ1 repeats diverging) are recorded as evidence and never halt the campaign. Identity mismatch, configuration mismatch, artifact-integrity defects, ambiguous execution state (both terminal markers present, or a sealed/halted campaign re-invoked), duplicate or unknown trial IDs, and any `DiagnosticHardStopException` (harness defect or, architecturally unreachable but defended anyway, a consequential downstream path) all halt immediately and write `campaign.halted` with a reason code. No retry is performed after any completed observation anywhere in the codebase.

## 16. Interpretation boundaries (Check 13), restated before execution

Ten DQ1 repeats are triage-grade evidence of near-determinism versus material variation — never a population failure probability. The single DQ4 Llama comparison is a confound-disclosed directional signal — never a benchmark verdict or a "switch models" conclusion. DQ5 is a firewalled, Qwen-only, content-free-placeholder diagnostic — never a production prompt recommendation, and its content fidelity is expected, non-evidentiary noise, not a finding. DQ3's two context observations are weak signal interpretable only jointly with DQ1's variability — never causal proof standing alone. Structured/schema-constrained output is not exercised anywhere in this campaign and remains entirely untested by it. A successful `campaign.sealed` means evidence collection completed with integrity — nothing about production readiness, deployment suitability, or model replacement. No remedy of any kind is selected, prototyped, or recommended by this campaign regardless of its results.

## 17. Operator configuration (Check 14)

Using the evidence captured in Sections 10–11, and the campaign identity in Section 9:

```text
PARKER_REASONING_EVAL_ENDPOINT_URL=http://127.0.0.1:11434/api/generate
PARKER_REASONING_EVAL_MODEL_NAME=qwen2.5-coder:7b
PARKER_REASONING_EVAL_TIMEOUT_MS=90000
PARKER_REASONING_EVAL_OUTPUT_PATH=build/unit2d-diagnostic-config-echo.jsonl
PARKER_REASONING_EVAL_REPOSITORY_COMMIT=705f9f5cf760fe8d9633ecd95b30fd72a37074ba
PARKER_REASONING_EVAL_MODEL_DIGEST=dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364
PARKER_REASONING_EVAL_RUNTIME_IMAGE_ID=ollama/ollama:latest@f795e46c5eac

PARKER_REASONING_DIAGNOSTIC_CAMPAIGN_ID=qwen25coder7b-llama32-3b-diagnostic-20260809
PARKER_REASONING_DIAGNOSTIC_ARTIFACT_ROOT=/var/lib/parker/reasoning-protocol-live-model
PARKER_REASONING_DIAGNOSTIC_UBUNTU_RUNTIME_ID=parker
PARKER_REASONING_DIAGNOSTIC_CONTAINER_ID=ollama:f795e46c5eac
PARKER_REASONING_DIAGNOSTIC_QWEN_MODEL_SHOW_SHA256=ca0bc98ad9b95b049d1b98a289fee7fbfc85a1973dfa9d267dfc84290b6551fd
PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_NAME=llama3.2:3b
PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_DIGEST=a80c4f17acd55265feec403c7aef86be0c25983ab279d83f3bcd3abbcb5b8b72
PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_SHOW_SHA256=ef2f477fd7a923b2209082378173bfeda9d91201d3970c12c20bd283179bcb04
```

`PARKER_REASONING_EVAL_OUTPUT_PATH` is required by the reused Unit 1 loader's completeness check but is never read by Unit 2-D's own driver, which uses its own ledger paths under the campaign directory instead; the value above is a harmless, valid, outside-`src`/`tests`/`docs` placeholder for that one inert field, not a default the driver could silently fall back to. Every other value above is the exact, freshly-captured evidence from Sections 10–11 — none is a placeholder that could silently resolve to something else. This configuration expires the moment any of Sections 10–13's underlying facts change (model digest, disk space, repository commit) and must be re-verified, not reused, if execution is deferred.

## 18. Operator command (Check 14)

```text
./gradlew reasoningProtocolUnit2DDiagnostic
```

run with exactly the environment in Section 17 exported first. **Not executed during this review.**

## 19. Authorization scope (Check 15)

This authorization covers, and covers only: campaign identity `qwen25coder7b-llama32-3b-diagnostic-20260809`; one execution of the frozen 24-call schedule in Section 14; exactly `qwen2.5-coder:7b` (digest `dae161e...4364`) and `llama3.2:3b` (digest `a80c4f1...8b72`); exactly repository commit `705f9f5cf760fe8d9633ecd95b30fd72a37074ba`; exactly artifact parent `/var/lib/parker/reasoning-protocol-live-model`. No campaign restart after a hard stop without a fresh Defect Confirmation Review; no rerunning individual completed trials; no additional diagnostic calls beyond the 24; no Unit 3 work of any kind. Any drift in repository commit, either model's digest, or the artifact parent voids this authorization and requires a fresh Explicit Execution Approval Review.

## 20. Strongest case against authorization

**No code-level approval flag.** The strongest single objection, addressed at length in Section 7: without it, this document's authorization is not mechanically enforced. Answer: Unit 2's own precedent, examined precisely rather than by surface analogy, supports exactly this design for a stage-0-scale campaign; the ~14-value configuration contract is itself a substantial, hard-to-assemble-by-accident barrier, and every value in Section 17 has a zero-tolerance exact-match or format check with no fallback.

**Model identity freshness.** Addressed by fresh, this-session `/api/tags` read directly, not reused from a stale record — both digests captured live, Qwen's cross-checked against Unit 2's own historical record and found unchanged.

**Artifact collision.** Addressed at three independent code layers (Section 8) plus direct filesystem inspection confirming the proposed directory does not exist.

**Free space.** Addressed with the exact implemented threshold, not an assumed one (Section 13) — comfortable margin (3.68 GiB against a 2 GiB requirement).

**Unpinned inference randomness.** Not addressed by this review, nor should it be — Plan Section 7 explicitly requires preserving the unpinned production request shape unmodified, and Plan Section 12 requires the future interpretation worksheet to state this limitation explicitly rather than resolve it. This is by design, not a gap.

**DQ5 remedy adjacency.** Addressed by the firewall (Plan Section 16) and the fixed placeholder design (Section 16 above) — no path from DQ5's evidence to a production recommendation exists in the implementation.

**Single-call Llama weakness.** Addressed by explicit interpretation-rule hedging (Section 16 above) — the campaign does not claim more than one data point supports.

**Accidental Unit 2 reuse.** Addressed at three independent layers (Section 8), independently re-verified this session.

**Exact-once behavior and campaign-restart risk.** Addressed by the passing exact-once/recovery/duplicate/hard-stop tests (Completion Review Sections 8, 15–20) and by Section 19's explicit prohibition on restart without a fresh review.

None of these objections survives as a blocking defect against the frozen design; each is either already controlled by the implementation or is a deliberate, documented design choice rather than an oversight.

## 21. Final determination

```text
LIVE EXECUTION:
AUTHORIZED
```

**Campaign identity:** `qwen25coder7b-llama32-3b-diagnostic-20260809`
**Repository commit covered:** `705f9f5cf760fe8d9633ecd95b30fd72a37074ba`

Authorization is exactly as narrow as Section 19 states, exactly one execution, and expires on any drift in the identities pinned in Sections 10–11.

## 22. Exact next authorized action

An operator may export the Section 17 configuration and run the Section 18 command — this review does not perform that step. No other action is authorized by this document.

## 23. Explicit prohibited actions

Running the live campaign under any configuration other than Section 17's exact values; restarting or resuming after any hard stop without a fresh Defect Confirmation Review; rerunning any individual trial; adding any call beyond the frozen 24; touching `qwen25coder7b-baseline-20260809` in any way; beginning Unit 3 remedy work under any circumstance; modifying production, test, or Gradle files as part of executing this authorization; staging, committing, or pushing anything in connection with this review or the eventual campaign without separate, explicit instruction.
