**Status:** Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review (4) — **AUTHORIZED.** This document authorizes exactly one future campaign execution and does not itself execute it. Explicit Execution Approval Reviews 1–3 are preserved unmodified as the historical record of the warm-up, live-trigger, and disk-space-gate defects each discovered and corrected in turn; this is a wholly fresh review, not an edit of any of them. It specifically supersedes the authority of Review 3, which was never fully exercised: the attempt to use it discovered the disk-space-gate and live-test-scoping defects recorded in the Execution Evidence Review's Attempt 2, and Review 3's own authorization is explicitly not relied upon here. Only read-only `/api/tags` and `/api/show` HTTP calls occurred during this review; no `/api/generate` call, no campaign directory creation, and no code/test/Gradle modification occurred during this review itself (the corrections it examines were completed and independently verified in earlier phases of this same task).

# Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review (4)

## 1. Status

Final governance gate before live execution, performed fresh following: (a) independent confirmation of the disk-space-gate and live-test-scoping defects recorded in the Execution Evidence Review's Attempt 2; (b) their narrow, test/integration-and-Gradle-tier-only corrections; (c) the Disk-Space and Live-Test-Scoping Defect Independent Constitutional Review (`ACCEPTED`); (d) the fourth refresh of the Completion Review (`PASS`), Completion Independent Constitutional Review (`ACCEPTED`), Implementation Readiness Review (`READY`), and Readiness Independent Constitutional Review (`ACCEPTED`) — the last of which explicitly corrects its own immediately prior version's own omission (never having independently traced the disk-space gate as a distinct link) before reaching its own `ACCEPTED` verdict this time.

## 2. Authority

Controlled by the frozen Unit 3-C Scope Lock, the corrected frozen Implementation/Execution Plan, the full defect-correction chain (Family C trace, warm-up orchestration, live-trigger, disk-space/live-test-scoping), and the four-times-refreshed Completion/Readiness review chain (this task). Repository baseline: `77a9917647d7edc39ad790fa712ff1c958ec5a64` (`HEAD` = `origin/main` at the start of this task), with the disk-space and live-test-scoping corrections and this task's own governance documents present in the working tree, uncommitted, exactly as this task's own instructions require.

## 3. Repository baseline

`HEAD` independently re-confirmed `77a9917647d7edc39ad790fa712ff1c958ec5a64`. Working tree contains, uncommitted: `build.gradle.kts` (one new `useJUnitPlatform { excludeTags(...) }` block, comment); `ReasoningProtocolUnit3COrchestrationTest.kt` (disk-space check now targets `artifactRoot.parent`, two new tests); `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` (one test tagged, one new constant); the four refreshed review documents; the two new Disk-Space/Live-Test-Scoping Defect review documents; and this document itself. `git diff --stat -- src/` independently re-confirmed empty throughout.

## 4. Implementation identity

`unit3cControlledRemedyExperiments` filters to exactly the two Unit 3-C test classes and now additionally excludes tag `unit3cLiveTaskIncompatible`; `liveModelEvaluation` remains structurally detached from `test`/`check`/`build`. `./gradlew unit3cControlledRemedyExperiments --rerun-tasks` re-run fresh for this review: **79 tests (48 + 31), 0 failures, 0 errors, 1 skipped** — the skip is the live trigger test itself, correctly aborting on the absent real campaign-ID environment variable. `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks` re-run fresh: 142 tests/4-skip/0-fail across five classes, independently confirmed the schedule/mechanism class reports 49 there (the tagged offline-only test present and passing). `./gradlew test --rerun-tasks` re-run fresh: 2015/5-skip/0-fail, zero Unit 3-C result files.

## 5. Exact 483-call derivation

Unaffected by either correction, re-verified fresh for this review: 3 warm-up + 145 Control + 220 Family A + 115 Family B + 0 Family C = 483, matching the driver's own measured invocation count.

## 6. Family C corrected trace verification

Re-confirmed unchanged: 24/29 correct; false positives `P03`, `P04`, `P05`, `P12`; false negative `R03`. Not referenced by either correction.

## 7. Model identity

Fresh read-only `/api/tags` call, performed for this review: `qwen2.5-coder:7b` installed, digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364` — identical to every prior review's own capture.

## 8. Runtime identity

Fresh checks, performed for this review: hostname `parker`; Ollama version `0.32.5` — unchanged from every prior review.

## 9. Inference configuration

Re-confirmed unchanged: `qwen2.5-coder:7b` only; no sampling parameters; 30,000 ms timeout; production request-body shape. Neither correction touches `Unit3CConfigLoader` or `LiveEvaluationConfigLoader`.

## 10. Campaign identity

`unit3c-remedy-experiments-20260810`, unchanged, never consumed — independently re-confirmed absent from the artifact root (Section 11).

## 11. Artifact-root verification

Fresh listing: `/var/lib/parker/reasoning-protocol-live-model/` contains only the two preserved Unit 2/Unit 2-D directories — no `unit3c-*` directory. `Unit3CArtifactRootPolicy` untouched by either correction.

## 12. Disk-space verification

Fresh `df -B1`: **3,927,920,640 bytes available** — a modest, expected decrease from Review 3's own reading (3,930,902,528 bytes), consistent with ordinary system activity, still well above the governed 2 GiB minimum with over 1.66 GiB of margin. **Additionally, and specifically for this review:** the disk-space *gate itself* is now independently re-confirmed to check this exact durable parent path (`/var/lib/parker/reasoning-protocol-live-model`), not a not-yet-created campaign subdirectory — the defect that halted the real attempt this authorization boundary otherwise matches exactly. Re-verified via `driver's real default disk-space check succeeds against a not-yet-created campaign directory when its parent exists with sufficient space`, re-run fresh for this review.

## 13. Unit 2 artifact-integrity result

Fresh re-hash: `stage-0/STAGE-0/raw.jsonl` → `c635ebcd051a7eeb02e154e3b07a4ba9e101fcd71f019bebc5990961f8179d5f`, matching every prior review.

## 14. Unit 2-D artifact-integrity result

Fresh re-hash of the three manifest-tracked track artifacts: `warmup/raw.jsonl` → `d542f0ede35fffb51c3907452afac7c2f2cbfd6533fdad0def34be7c640d5c3d`; `production-track/raw.jsonl` → `568f08f2421dd5a63e949268d2519cae3e0635abc713d437352e141636bcfde7`; `candidate-track/raw.jsonl` → `c12de361ffec2e39ce4e10b2ee0398cadd1b0ea15e30f5ae10dc41e5b789677f` — all match the campaign's own manifest exactly.

## 15. Safety-checkpoint verification

Unaffected by either correction; re-run fresh, passes.

## 16. Exact-once verification

Unaffected by either correction; re-run fresh, passes.

## 17. Downstream-isolation verification

Re-confirmed absolute via a fresh, exhaustive import search across all three modified files, including the two new disk-space tests and the tag annotation: no forbidden symbol found.

## 18. Live-trigger reachability (carried forward, re-verified unaffected)

Unaffected by this task's own corrections: the trigger function, the source-scan test proving exactly one real call gated by exactly two `assumeTrue` checks, and the four config/artifact-root fail-closed tests are all independently re-confirmed present and passing, unchanged from Approval Review 3's own Section 18 findings, via a fresh re-run.

## 19. Disk-space gate and live-test-scoping — proven corrected, not merely asserted

This is the specific gap Approval Review 3 could not have checked (the defect did not yet exist as a known issue) and the specific gap the immediately prior Readiness ICR refresh admits its own prior version missed. Independently re-traced for this review:

1. **Disk-space gate now targets the correct path.** `Unit3COrchestrationDriver.run()`'s first two statements, independently re-read: `val spaceCheckTarget = requireNotNull(artifactRoot.parent) { ... }`, then `Unit3CDiskSpaceGate.check(spaceCheckTarget, usableSpace)`. For the real live campaign, `artifactRoot` = `/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810` (not yet existing); `.parent` = `/var/lib/parker/reasoning-protocol-live-model` (independently re-confirmed existing, `steve:steve`, `700`, 3.93 GB free — Section 12). The gate will check the correct, existing path.
2. **Proven against the real, unmocked filesystem call.** `driver's real default disk-space check succeeds against a not-yet-created campaign directory when its parent exists with sufficient space` exercises the actual default `{ Files.getFileStore(it).usableSpace }` lambda — the exact function whose `NoSuchFileException` halted the real attempt — against a genuinely non-existent directory, and confirms all four arms reach `SEALED`. Re-run fresh for this review: passes.
3. **Live-test scoping resolved without weakening.** The previously-unconditionally-selected `assertNull` test is now tagged and excluded from `unit3cControlledRemedyExperiments`'s own selection (48 tests, tag absent from that task's XML report) while remaining selected and passing under the general offline `reasoningProtocolLiveModelEvaluation` task (49 tests, tag present and `passed`) — independently re-confirmed via fresh runs of both tasks for this review.
4. **No frozen property changed.** `git diff --stat -- src/` empty; `UNIT_3C_MINIMUM_FREE_BYTES`, `UNIT_3C_ARTIFACT_ROOT_PREFIX`, `FAMILY_B_CANDIDATE_SHA256`, and `liveModelCallCount == 483` all independently re-confirmed absent from every diff hunk in this task.

A genuine first-ever campaign, under this exact corrected code, will now reach the disk-space gate, pass it against the real, existing, sufficiently-large durable parent, and proceed into the warm-up and arm loop — independently proven by the test in item 2 above, which reproduces the exact real-world condition (a not-yet-existing campaign directory) that previously blocked it.

## 20. Exact execution configuration

Repository commit `77a9917647d7edc39ad790fa712ff1c958ec5a64` (or the commit resulting once this task's own working-tree corrections are committed — see Section 21); endpoint `http://127.0.0.1:11434/api/generate`; model `qwen2.5-coder:7b`; digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`; timeout `30000`; campaign ID `unit3c-remedy-experiments-20260810`; artifact root `/var/lib/parker/reasoning-protocol-live-model`. None of these values was exported or activated during this review.

## 21. Authorization boundary

This review authorizes exactly: **one campaign ID** (`unit3c-remedy-experiments-20260810`); **one repository commit** — the commit that results once this task's own working-tree corrections (the disk-space and live-test-scoping fixes and this governance chain) are committed, matching the exact source this review examined; **one model digest** (`dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`); **one runtime identity** (Ollama on host `parker`, version `0.32.5`); **one artifact parent** (`/var/lib/parker/reasoning-protocol-live-model`); **the one frozen, now genuinely reachable and genuinely disk-space-gate-passable, 483-call schedule**; **the frozen Family A/B/C mechanisms exactly as committed, with zero post-hoc modification during execution**; **invocation exclusively via the gated live trigger test under the detached `unit3cControlledRemedyExperiments` Gradle task**, with complete, explicit `PARKER_REASONING_EVAL_*`/`PARKER_REASONING_UNIT3C_*` environment configuration matching Section 20 exactly. No repeat campaign under this authorization. No continuation past any governed halt without new authority. No Unit 3-D evaluation. No remedy selection. This authorization lapses if the repository commit executed against differs from the one this review examined, or if the disk-space gate, live trigger, or test-selection mechanism examined in Sections 18–19 is modified before execution.

## 22. Prohibited actions confirmed not taken

No `/api/generate` call — only `/api/tags` and `/api/show`, both read-only. No live Gradle task invocation. No campaign directory created. No environment variable exported in a way that would activate execution. No code, test, or Gradle modification during this review itself. No remedy selected. No Unit 3-D work performed. Nothing staged, committed, or pushed by this review.

## 23. Final verdict

```text
AUTHORIZED
```

**This document authorizes exactly one future campaign execution, under the exact boundary stated in Section 21, and does not itself execute it.** Both defects discovered by the first genuine live-execution attempt — the disk-space gate checking a not-yet-created path, and a verification test unconditionally selected by the one task incompatible with it — are independently confirmed corrected, proven against the real, unmocked functions that actually failed, and confirmed to leave every frozen governance property unchanged. All other approval dimensions — model identity, runtime identity, inference configuration, campaign identity, artifact-root and disk-space verification, Unit 2 and Unit 2-D artifact integrity, the safety checkpoint, exact-once durability, downstream isolation, and live-trigger reachability — are independently re-verified fresh and found sound. The next step, outside this review's own scope, is committing the corrected implementation and this governance chain, then proceeding to actual execution under the exact, narrow boundary this document establishes — neither of which this task performs.
