**Status:** Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review (3) — **AUTHORIZED.** This document authorizes exactly one future campaign execution and does not itself execute it. Explicit Execution Approval Reviews 1 and 2 (`docs/reviews/..._EXPLICIT_EXECUTION_APPROVAL_REVIEW.md`, `..._EXPLICIT_EXECUTION_APPROVAL_REVIEW_2.md`) are preserved unmodified as the historical record of the warm-up orchestration defect and its correction; this is a wholly fresh review, not an edit of either. It specifically supersedes the authority of Review 2, which was never exercised: the attempt to use it discovered the live-execution trigger defect recorded in the committed Execution Evidence Review (`c4b840f`), and Review 2's own authorization is explicitly not relied upon here. Only read-only `/api/tags` and `/api/show` HTTP calls occurred during this review; no `/api/generate` call, no campaign directory creation, and no code/test/Gradle modification occurred during this review itself (the trigger correction it examines was completed and independently verified in earlier phases of this same task, prior to this review).

# Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review (3)

## 1. Status

Final governance gate before live execution, performed fresh following: (a) independent confirmation of the live-execution trigger defect recorded in the committed Execution Evidence Review; (b) its narrow, test/integration-tier-only correction; (c) the Live Trigger Defect Independent Constitutional Review (`ACCEPTED`); (d) the third refresh of the Completion Review (`PASS`), Completion Independent Constitutional Review (`ACCEPTED`), Implementation Readiness Review (`READY`), and Readiness Independent Constitutional Review (`ACCEPTED`), each of which now explicitly proves — not assumes — that the full executable path from the detached Gradle task to the real orchestration driver exists. This review does not re-litigate any of those documents' own findings; it independently re-verifies the specific facts an execution-approval decision depends on, and additionally performs the one check none of the prior review chain (including both prior Approval Reviews) ever actually performed: proving the complete executable path end-to-end without executing it.

## 2. Authority

Controlled by the frozen Unit 3-C Scope Lock, the corrected frozen Implementation/Execution Plan (`08f3692`), the Family C Trace Defect review chain, the Warm-Up Orchestration Defect review chain, the committed Execution Evidence Review (`c4b840f`), the Live Trigger Defect Confirmation Review and its Independent Constitutional Review (this task), and the thrice-refreshed Completion/Readiness review chain (this task). Repository baseline: `c4b840f87e53d2854629ef795679f44c94fc9f76` (`HEAD` = `origin/main` at the start of this task), with the live-trigger correction and this task's own governance documents present in the working tree, uncommitted, exactly as this task's own instructions require.

## 3. Repository baseline

`HEAD` independently re-confirmed `c4b840f87e53d2854629ef795679f44c94fc9f76`. Working tree contains, uncommitted: the corrected `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` (nine new tests, one new helper, one corrected doc comment); the four refreshed review documents (Completion Review, Completion ICR, Readiness Review, Readiness ICR); the two new Live Trigger Defect review documents; and this document itself. `git diff --stat -- src/` and `git diff --stat -- build.gradle.kts` both independently re-confirmed empty throughout.

## 4. Implementation identity

`unit3cControlledRemedyExperiments` filters to exactly the two Unit 3-C test classes (unchanged filter, `build.gradle.kts` untouched by this task); `liveModelEvaluation` remains structurally detached from `test`/`check`/`build`, independently re-confirmed via a fresh `./gradlew test --rerun-tasks` producing zero Unit 3-C or live-model-evaluation test-result files. `Unit3CLiveEntryPoint.run` requires complete `PARKER_REASONING_EVAL_*` and `PARKER_REASONING_UNIT3C_*` configuration, absent by default. `./gradlew unit3cControlledRemedyExperiments --rerun-tasks` re-run fresh for this review: **78 tests, 0 failures, 0 errors, 1 skipped** — the skip is the live trigger test itself, correctly aborting on the absent real campaign-ID environment variable.

## 5. Exact 483-call derivation

Unaffected by the trigger correction and re-verified fresh for this review: a counting fake executor confirms exactly 483 total invocations across Control (148, including 3 warm-ups), Family A (220), and Family B (115), with Family C's 0 confirmed structurally. `Unit3CCampaignDefinition`'s own `init`-time check (`liveModelCallCount == 483`) independently re-read at its current line number, unchanged.

## 6. Family C corrected trace verification

Re-confirmed unchanged and unaffected by the trigger correction: 24/29 correct; false positives `P03`, `P04`, `P05`, `P12`; false negative `R03`. Fixture texts and expected actions independently re-confirmed unchanged (the schedule/mechanism file's diff for this task touches only the live-trigger region at the end of the file; the fixture/mechanism sections earlier in the file do not appear in `git diff` at all).

## 7. Model identity

Fresh read-only `/api/tags` call, performed for this review: `qwen2.5-coder:7b` installed, digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364` — identical to the digest recorded in both prior Approval Reviews and to Unit 2-D's own diagnostic evidence. No drift, no substitution. (`llama3.2:3b` is also installed, unrelated to Unit 3-C, which recognizes exactly `qwen2.5-coder:7b`.)

## 8. Runtime/container identity

Fresh read-only `/api/show` call for `qwen2.5-coder:7b`, performed for this review: raw response 50,696 bytes, SHA-256 `ca0bc98ad9b95b049d1b98a289fee7fbfc85a1973dfa9d267dfc84290b6551fd` — byte-for-byte identical to both prior Approval Reviews' own captures, confirming the runtime is genuinely stable across the intervening work (including the entire live-trigger correction task), not merely re-asserted. Hostname `parker`; Ollama version `0.32.5`, unchanged.

## 9. Inference configuration

Re-confirmed unchanged: `qwen2.5-coder:7b` only; no sampling parameters; 30,000 ms timeout; production request-body shape. No Family E leakage anywhere in the corrected source. `Unit3CConfigLoader.load` and `LiveEvaluationConfigLoader.load` both independently re-read, unchanged by the trigger correction's diff.

## 10. Campaign identity

`unit3c-remedy-experiments-20260810`, unchanged from both prior reviews. Independently re-confirmed valid against the frozen marker and machine-safe grammar, and independently re-confirmed never consumed: the halted execution attempt prepared but never exported this exact ID (Execution Evidence Review Section 6), and it does not appear anywhere under the artifact root today.

## 11. Artifact-root verification

Fresh listing of `/var/lib/parker/reasoning-protocol-live-model/`: unchanged, contains only the two preserved Unit 2/Unit 2-D directories — no `unit3c-*` directory of any kind, independently re-confirmed via `find ... -iname "*unit3c*"` returning no matches. `Unit3CArtifactRootPolicy` (unaffected by this task's correction) re-confirmed hard-restricted to this exact parent, with nesting guards against both preserved directories; independently re-exercised by this task's own new "wrong artifact root" test, which confirms the policy still rejects any other value before any driver is constructed.

## 12. Disk-space verification

Fresh `df -B1`: **3,930,902,528 bytes available** at the artifact-root mount point — a modest, expected decrease from Review 2's own reading (3,933,016,064 bytes), consistent with ordinary system activity across the intervening tasks, still well above the governed 2 GiB (2,147,483,648 bytes) minimum with over 1.7 GiB of margin.

## 13. Unit 2 artifact-integrity result

Fresh re-hash, performed for this review: `stage-0/STAGE-0/raw.jsonl` → `c635ebcd051a7eeb02e154e3b07a4ba9e101fcd71f019bebc5990961f8179d5f`, identical to the manifest's own recorded value and to both prior Approval Reviews' own captures. Preserved Stage 0 failure marker (`stage-0.failed`) present, unchanged.

## 14. Unit 2-D artifact-integrity result

Fresh re-hash of all four manifest-tracked artifacts, performed for this review: `warmup/raw.jsonl` → `d542f0ede35fffb51c3907452afac7c2f2cbfd6533fdad0def34be7c640d5c3d`; `production-track/raw.jsonl` → `568f08f2421dd5a63e949268d2519cae3e0635abc713d437352e141636bcfde7`; `candidate-track/raw.jsonl` → `c12de361ffec2e39ce4e10b2ee0398cadd1b0ea15e30f5ae10dc41e5b789677f` — all three independently cross-checked against the values recorded inside the campaign's own `manifest.txt`, matching exactly. `campaign.sealed` marker present, unchanged. Sealed state confirmed.

## 15. Safety-checkpoint verification

Re-confirmed unaffected by the trigger correction, since the checkpoint logic operates on scored trials inside the orchestration driver, which the trigger correction does not touch: non-numeric, first-occurrence trigger; preserves the triggering observation; blocks sealing; no auto-clear. Re-run fresh: passes.

## 16. Exact-once verification

Re-confirmed, unaffected by the trigger correction: intent-before-call, durable raw persistence, checkpoint ordering, duplicate prevention, crash-recovery idempotence, identity-drift protection, and arm-scoped fail-closed behavior all independently re-verified via the existing (unmodified) orchestration test suite, re-run fresh for this review.

## 17. Downstream-isolation verification

Re-confirmed absolute via a fresh, exhaustive import search across both files, including the nine new trigger/verification tests: no forbidden symbol found. Independently re-read all nine new test bodies directly (not merely re-run their assertions) to confirm none constructs an `HttpClient`, a `LocalHttpModelInferenceClient`, or any Memory/Goal/Planner API reference.

## 18. Live-trigger reachability — proven end-to-end, not inferred from arithmetic alone

This is the specific check the prior review chain, including both prior Approval Reviews, never performed — each of them verified `Unit3CLiveEntryPoint.run`'s own internal correctness and treated that as sufficient. This review does not repeat that mistake. Each link below is independently traced from source and fresh test evidence gathered during this review and the immediately preceding phases of this same task:

1. **Detached Gradle task exists and is isolated from ordinary lifecycle.** Fresh-read `build.gradle.kts:149-160`: `unit3cControlledRemedyExperiments` uses `liveModelEvaluation.output.classesDirs`/`runtimeClasspath`, filters to the two Unit 3-C classes, sets `parker.reasoning.unit3c.enabled=true` for its own scope. Fresh `./gradlew test --rerun-tasks` independently re-confirmed to produce zero Unit 3-C test-result files.
2. **Live trigger exists and is gated.** `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt:1458`, function `live Unit 3-C campaign is skipped before any configuration is loaded unless the Gradle-set property and a real campaign ID are both present` — independently re-read in full: two `assumeTrue` gates (Gradle-set property, then real campaign-ID environment variable), then exactly one call to `Unit3CLiveEntryPoint.run(System.getenv(), Path.of("."))`. Fresh run of the detached task reports this test `skipped` (JUnit status only reachable via a genuinely-thrown `assumeTrue` abort), confirming the gate is live and functioning, not vacuous.
3. **Configuration validation.** `Unit3CConfigLoader.load`, first call inside `Unit3CLiveEntryPoint.run` — independently re-confirmed to validate timeout, model name, campaign-ID format/marker, artifact-root presence, and digest presence, in that order, before any further step; independently re-exercised fresh by four negative tests added in this task (wrong model, blank digest, malformed campaign ID) plus the pre-existing empty-map test.
4. **Artifact-root validation.** `Unit3CArtifactRootPolicy.resolve`, second call inside `Unit3CLiveEntryPoint.run` — independently re-exercised fresh by the new "wrong artifact root" test, confirmed to throw `Unit3CArtifactRootViolationException` before any driver is constructed.
5. **Orchestration driver construction and full family-executor wiring.** `Unit3CLiveEntryPoint.run`'s remaining body — independently re-read in full for this review, confirmed to construct `Unit3COrchestrationDriver(config.campaignId, artifactRoot, config.identity)` and call `driver.run(executors)` with an executor map keyed by all four `Unit3CFamily` values, independently cross-checked against the new structural test asserting the same facts from source.
6. **Warm-ups, then Control/Family A/Family B/Family C, in frozen order, totaling 483.** Unaffected by this task's diff; independently re-run fresh for this review (`exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups`): 3 warm-up + 145 Control + 220 Family A + 115 Family B + 0 Family C = 483, matching the driver's own measured invocation count exactly, not merely the schedule's own arithmetic.
7. **Durable artifacts.** `Unit3CArmLedger`, unmodified by this task, independently re-confirmed via its own existing exact-once test suite (re-run fresh for this review) to write sealed, recoverable, duplicate-free ledgers per arm.

No step in this chain was accepted on the strength of any single component's own internal correctness in isolation; each was independently traced to either a fresh operational test result or a fresh, direct source read performed during this review or the immediately preceding phases of this task.

## 19. Exact execution configuration

Unchanged from both prior reviews' own tables, re-confirmed current: repository commit `c4b840f87e53d2854629ef795679f44c94fc9f76` (or the commit resulting once this task's own working-tree corrections are committed — see Section 20); endpoint `http://127.0.0.1:11434/api/generate`; model `qwen2.5-coder:7b`; digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`; timeout `30000`; campaign ID `unit3c-remedy-experiments-20260810`; artifact root `/var/lib/parker/reasoning-protocol-live-model`. None of these values was exported or activated during this review.

## 20. Authorization boundary

This review authorizes exactly: **one campaign ID** (`unit3c-remedy-experiments-20260810`); **one repository commit** — the commit that results once this task's own working-tree corrections (the live-trigger fix and this governance chain) are committed, matching the exact source this review examined, not a different or later state; **one model digest** (`dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`); **one runtime identity** (the current Ollama instance on host `parker`, version `0.32.5`); **one artifact parent** (`/var/lib/parker/reasoning-protocol-live-model`); **the one frozen, now genuinely reachable, 483-call schedule**; **the frozen Family A/B/C mechanisms exactly as committed, with zero post-hoc modification during execution**; **invocation exclusively via the gated live trigger test under the detached `unit3cControlledRemedyExperiments` Gradle task, with `parker.reasoning.unit3c.enabled=true` and complete, explicit `PARKER_REASONING_EVAL_*`/`PARKER_REASONING_UNIT3C_*` environment configuration matching this section exactly.** No repeat campaign under this authorization. No continuation past any governed halt (measurement-invalidating failure or safety checkpoint) without new authority. No Unit 3-D evaluation. No remedy selection. This authorization lapses if the repository commit executed against differs from the one this review examined, or if the trigger mechanism examined in Section 18 is modified before execution.

## 21. Prohibited actions confirmed not taken

No `/api/generate` call — only `/api/tags` and `/api/show` were called, both read-only. No live Gradle task invocation (the detached task was run offline only, with no `PARKER_REASONING_*` environment variable set). No campaign directory created. No environment variable exported in a way that would activate execution. No code, test, or Gradle modification during this review itself (the corrections it examines were completed and independently verified in the phases immediately preceding this review, within the same task). No remedy selected. No Unit 3-D work performed. Nothing staged, committed, or pushed by this review.

## 22. Final verdict

```text
AUTHORIZED
```

**This document authorizes exactly one future campaign execution, under the exact boundary stated in Section 20, and does not itself execute it.** The live-execution trigger defect that halted the previous execution attempt is independently confirmed corrected and, for the first time in this programme, the complete executable path — detached Gradle task through live trigger, configuration validation, artifact-root validation, orchestration driver construction, all four family executors, warm-ups, and the frozen 483-call schedule — is independently proven reachable end-to-end (Section 18), not merely asserted from the correctness of any single link. All other approval dimensions — model identity, runtime identity, inference configuration, campaign identity, artifact-root and disk-space verification, Unit 2 and Unit 2-D artifact integrity, the safety checkpoint, exact-once durability, and downstream isolation — are independently re-verified fresh and found sound. The next step, outside this review's own scope, is committing the corrected implementation and this governance chain, then proceeding to actual execution under the exact, narrow boundary this document establishes — neither of which this task performs.
