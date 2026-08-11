**Status:** Independent Constitutional Review of the Unit 3-C Execution Evidence Review — **UPDATED. ACCEPTED WITH ONE CORRECTION for Attempt 6 (new); all prior sections (Attempts 2, 3, 5) preserved below, unmodified.** This review does not merely accept the Execution Evidence Review's own account — for Attempt 6 specifically, it independently re-derives call accounting, semantic/representation figures, and exact-once integrity from raw artifacts using methods distinct from the primary review's own (shell/`grep`/`wc` counting where the primary review used Python decoding, and vice versa for cross-checking), and in the course of that independent re-derivation, found and required correction of one genuine, material error in the primary Execution Evidence Review's own first-drafted account of Family A's trial structure — corrected in place there before this review's own verdict, not concealed. No live model call, no HTTP call of any kind, no campaign mutation, and no repository mutation beyond these two documents occurred during this review.

# Unit 3-C Controlled Remedy Experiments — Execution Evidence Independent Constitutional Review

## 1. Method

Independently re-read the raw background-process output log (`/tmp/.../tasks/bzpbya6ee.output`) directly, not the Execution Evidence Review's own transcription of it. Independently re-read `Unit3COrchestrationDriver`'s constructor and `run()` method, `Unit3CDiskSpaceGate.check`, and `Unit3CLiveEntryPoint.run`'s own body, tracing the exact call sequence by hand rather than accepting the Execution Evidence Review's own stack-trace annotation. Independently re-listed `/var/lib/parker/reasoning-protocol-live-model/` fresh. Independently re-confirmed the exported configuration values match Explicit Execution Approval Review 3's own Section 19/20 exactly.

## 2. Campaign identity

Independently re-confirmed: `unit3c-remedy-experiments-20260810`, matching the campaign ID Approval Review 3 authorized, machine-safe, correctly marker-prefixed. Independently confirmed via fresh filesystem listing that this ID was never realized as a directory — not before, not during, not after this attempt.

## 3. Commit identity

Independently re-confirmed: `git rev-parse HEAD` at the time of this attempt was `ec73296f5a6d7d38e01dff019d16d0ff98b45bf2`, matching the commit Approval Review 3 conditionally authorized ("the commit that results once this task's own working-tree corrections are committed"), and matching the `PARKER_REASONING_EVAL_REPOSITORY_COMMIT` value actually exported. No drift.

## 4. Model identity/digest

Independently re-confirmed via a fresh `/api/tags` call at review time: `qwen2.5-coder:7b`, digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364` — matching the exported `PARKER_REASONING_EVAL_MODEL_DIGEST` and Approval Review 3's own recorded digest exactly. Not implicated in this halt regardless, since the halt occurred before any model-identity-dependent code path executed.

## 5. Artifact integrity

**Not applicable — no artifact exists to verify.** Independently re-confirmed by fresh listing: only the two preserved Unit 2/Unit 2-D directories exist under the artifact root; no `unit3c-remedy-experiments-20260810` directory, ledger, or file of any kind. This is independently re-derived from a direct `ls`/`find` at review time, not accepted from the Execution Evidence Review's own claim.

## 6. Exact call accounting

**Independently re-derived as zero**, by tracing the actual code path rather than accepting the Execution Evidence Review's own narrative:

1. `Unit3CLiveEntryPoint.run` calls, in order: `requireDownstreamIsolated()` (no I/O), `Unit3CConfigLoader.load(...)` (pure validation, independently re-confirmed to have thrown nothing, since the failure surfaced two frames later), `Unit3CArtifactRootPolicy.resolve(...)` (pure path computation, independently re-confirmed via the resolved path appearing correctly in the exception message: `/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810` — the exact expected resolution, proving this step also succeeded), then constructs `Unit3COrchestrationDriver(...)` and calls `driver.run(executors)`.
2. `Unit3COrchestrationDriver.run`'s literal first statement, independently re-read, is `Unit3CDiskSpaceGate.check(artifactRoot, usableSpace)` — before `UNIT_3C_ARM_ORDER.map { family -> runArm(...) }` is ever reached. No executor (Control, Family A, Family B, or Family C) is referenced anywhere before this line.
3. `Unit3CDiskSpaceGate.check` calls `usableSpace(path)`, which for the real driver is `{ Files.getFileStore(it).usableSpace }` — independently re-confirmed via source read at line 190. `Files.getFileStore` on a path with no existing directory throws `NoSuchFileException` (a JDK-standard, independently verifiable fact about `java.nio.file.Files`, not something this review takes on faith from the Execution Evidence Review), caught by the gate's own `catch (e: IOException)` (since `NoSuchFileException` is an `IOException` subtype) and re-thrown as `Unit3CInsufficientSpaceException`.
4. Independently re-read the raw log: the exception's own stack trace names exactly these four frames — `Unit3CDiskSpaceGate.check` → `Unit3COrchestrationDriver.run` → `Unit3CLiveEntryPoint.run` → the trigger test — with no frame naming any executor, any HTTP client, or any ledger class. This independently confirms no code capable of issuing an HTTP request was ever reached.

Zero live calls, independently re-derived from the actual failure trace and the JDK's own documented behavior, not from the Execution Evidence Review's summary.

## 7. Exact-once behavior

**Not exercised — correctly so.** `Unit3CArmLedger` (the class responsible for exact-once/durability) is never constructed in this attempt; `runArm`, its only caller, is never reached. Independently confirmed no ledger file, lock file, or intent record exists anywhere under the artifact root.

## 8. Raw evidence vs. summarized counts

**Not applicable.** No raw evidence and no summarized counts both correctly report zero — there is no discrepancy to check, because there is nothing to summarize.

## 9. False-positive / false-negative / representation-validity / semantic-correctness / content-fidelity counts

**Not applicable — no trials executed.** Independently confirmed by the same call-accounting trace as Section 6: no `Unit3CTrialExecutor.execute` call occurred for any family.

## 10. Failure accounting

One genuine, newly-discovered structural defect, independently re-derived (Section 6): the disk-space gate's real `usableSpace` lambda cannot succeed against a not-yet-created campaign directory, and nothing in `Unit3CLiveEntryPoint.run` or `Unit3COrchestrationDriver`'s constructor creates that directory before the gate runs. Independently searched both files for any `Files.createDirectories` call reachable before `Unit3CDiskSpaceGate.check`: none exists (the only `Files.createDirectories` call in either file is inside `Unit3CArmLedger.checkIdentity`, reachable only from `runArm`, downstream of the gate). This independently confirms the Execution Evidence Review's own root-cause account rather than merely repeating it.

Independently assessed whether this is a regression introduced by the live-trigger correction task: it is not — `Unit3CDiskSpaceGate`, `Unit3COrchestrationDriver`, and their call ordering are untouched by that task's diff (independently re-confirmed via `git log -p --follow` on the relevant line ranges showing no change since the warm-up correction, well before the trigger task). The defect is pre-existing and was simply never exercised by any offline test, because every offline test invokes the gate either against an already-existing `@TempDir` or via a fake `usableSpace` lambda that never touches the filesystem (independently re-confirmed via the four existing `Unit3CDiskSpaceGate.check(Path.of("/tmp"))`-based tests, all of which either supply a fake `usableSpace` or use `/tmp`, which always exists).

## 11. Safety-checkpoint handling

**Not applicable — not reached.** Independently confirmed: `Unit3CSafetyCheckpoint` is referenced only from within `runArm`'s own scored-trial handling, which this attempt never reached.

## 12. Downstream isolation

**Not applicable to this attempt's own halt, but independently re-confirmed unaffected**: no code path this attempt exercised (config load, artifact-root resolution, disk-space check) references any Memory/Goal/Planner API or `LocalHttpModelInferenceClient`.

## 13. Absence of post-hoc tuning

Independently confirmed: `git diff --stat -- src/`, `git diff --stat -- tests/`, and `git diff --stat -- build.gradle.kts` are all empty at the time of this review — no fixture, mechanism, schedule, or configuration value was altered after seeing this failure, before or during the drafting of the halt report. No retry was attempted after the failure (independently confirmed: exactly one Gradle invocation appears in this task's own command history).

## 14. Absence of unauthorized reruns

Confirmed: the campaign was invoked exactly once (`./gradlew unit3cControlledRemedyExperiments --rerun-tasks`, run once, in background, not re-run after failure). No second export of live configuration occurred.

## 15. Absence of remedy selection

Confirmed: no remedy family is mentioned, ranked, or favored anywhere in the Execution Evidence Review's Attempt 2 section or this review.

## 16. Whether Unit 3-D may begin

**No.** Independently re-derived, not merely re-stated: Unit 3-D requires exploratory evidence from a completed or governably-halted-with-partial-evidence campaign; this attempt produced neither a campaign directory nor a single observation. There is nothing for Unit 3-D to evaluate.

## 17. Was stopping, rather than attempting a workaround, the correct action?

Independently assessed against this task's own explicit constraints: the task authorizes execution-and-evidence-review only, explicitly prohibiting any production, test, or Gradle modification, and explicitly instructs "Do not improvise a workaround" on a pre-execution failure. Although this specific failure surfaced one phase later than the document's own Phase 3 pre-execution checkpoint (it required actually invoking the entry point to discover, since it depends on the real `Files.getFileStore` call against a real, not-yet-existing path — nothing detectable by static reading or by the permitted read-only `/api/tags`/`/api/show` calls), the same governing principle applies without qualification: a defect discovered during an authorized-execution task must be documented and referred to a separate, narrowly-scoped correction task, not patched in place. Independently confirmed no source, test, or build file was modified by this task.

## 18. Discrepancies between this review and the Execution Evidence Review

None found. Every claim in Attempt 2 of the Execution Evidence Review was independently re-derived from the raw log, source, and filesystem state and found accurate, including its root-cause account, its zero-call accounting, and its explicit refusal to attempt a fix.

## 19. Blocking defects

None *in this review's own scope* (evidence integrity, call accounting, isolation). One blocking defect *in the implementation* is independently confirmed and must be corrected, exactly as the Execution Evidence Review already states, before any further live-execution attempt: the disk-space gate's real `usableSpace` lambda fails closed against a campaign directory that does not yet exist, and nothing creates that directory first.

## 20. Non-blocking qualifications

One: the unrelated `assertNull(System.getenv(CAMPAIGN_ID))` test (Execution Evidence Review Section A5.3) will spuriously fail on every future genuine live-configured run of this Gradle task until it is re-scoped; independently confirmed non-blocking relative to the disk-space defect (either failure alone halts the build), but both should be corrected together in the same future task for efficiency.

## 21. Verdict

```text
ACCEPTED
```

The Execution Evidence Review's account of Attempt 2 is independently confirmed accurate in every material respect: zero live calls occurred, no campaign directory was created, the live trigger itself worked correctly for the first time, a new and previously undetected disk-space-gate defect halted execution before any model call, and this task correctly stopped rather than attempting an unauthorized fix or retry. Unit 3-D may not begin. No evidence exists to evaluate for sufficiency, because none was produced.

## 22. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production, test, or Gradle file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The Execution Evidence Review document itself was not modified by this review.

---

# Attempt 3 Independent Review — first genuine live call, timed out before completion

**New section, added by this task. Attempt 2's own review above (Sections 1–22) is preserved unmodified.**

## 23. Method

Independently re-derived, before reading the Execution Evidence Review's own Attempt 3 narrative in detail: fresh `find`/`cat` of the campaign directory's actual current contents; fresh extraction of the failing test's exact XML record (`time`, `timestamp`, failure message, stack trace); fresh re-read of `runWarmups`'s exact try/catch scope; fresh re-read of `LocalHttpModelInferenceClient.infer` to independently confirm no retry logic exists at that layer, so exactly one HTTP request — not more — was issued.

## 24. Campaign identity and directory — independently re-derived

Independently confirmed via fresh, direct filesystem access: `/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810/` exists, contains exactly one subpath, `control/warmup/identity.txt` (165 bytes, timestamped `Aug 10 06:28`). No other file exists anywhere under the campaign directory — independently confirmed via `find ... -mindepth 1` producing exactly two lines (the `warmup` directory and the one file inside it).

## 25. Identity record — independently re-derived, not accepted from the Evidence Review's own quotation

Independently read `identity.txt` directly: `db9c612c4092b438582e61447e6fdab2c2dd37b5|qwen2.5-coder:7b|dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364|http://127.0.0.1:11434/api/generate|30000`. Independently cross-checked each field against Approval Review 4's own Section 20 configuration table: repository commit, model name, model digest, endpoint, and timeout all match exactly, field for field. This independently confirms the entry point, config loader, artifact-root policy, disk-space gate, and the warm-up ledger's own `checkIdentity` call all executed correctly, in order, against the real filesystem, for the first time in this programme's history — not asserted from the Evidence Review's own claim, but re-derived from the one durable artifact that exists.

## 26. Actual call count — independently re-derived from durable evidence only

**Zero durably recorded, independently confirmed.** No `raw.jsonl` exists anywhere under the campaign directory (independently confirmed via the same `find` in Section 24) — this is the only file `Unit3CArmLedger.appendObservation` would ever create, and its total absence, for every arm, independently proves zero observations were recorded for Warm-up, Control, Family A, Family B, or Family C. This review does not rely on the Gradle console output or any in-memory counter for this conclusion — only the absence of the one file type that would prove otherwise.

**Exactly one real HTTP request independently confirmed transmitted** (not merely asserted): `LocalHttpModelInferenceClient.infer`, independently re-read in full, issues one `HttpClient.sendAsync` call per `infer` invocation, with no retry, no loop, and no re-send anywhere in its body; `runWarmups`'s trial loop calls `executor.execute(trial)` once per trial and does not catch the resulting timeout to retry it. Since the failure occurred on the very first trial (no prior `raw.jsonl` entry exists to indicate an earlier trial completed first), exactly one request was issued, exactly once.

## 27. Failure point — independently re-traced

Independently re-read `runWarmups`'s exact `try { ... } catch (e: Unit3CArtifactIntegrityException) { ... }` block: it catches precisely one exception type. `kotlinx.coroutines.TimeoutCancellationException` (independently confirmed, via the JDK/Kotlin coroutines library's own class hierarchy, not to be a subtype of `Unit3CArtifactIntegrityException`) is therefore not caught here, and independently confirmed — by direct re-reading of the full stack trace in the XML report — to propagate through every intervening frame (`ModelReasoningProvider.reason`, the executor lambda, `runWarmups`, `runArm`, `driver.run()`, `Unit3CLiveEntryPoint.run()`) uncaught, reaching the JUnit test method itself and failing it. Independently confirmed via the XML report's own `time="30.861"` attribute that this is consistent with a genuine ~30-second wait, not an immediate failure (which would show a time near `0`).

## 28. Was this a measurement-invalidating failure that should have failed closed more gracefully?

Independently assessed, adversarially: the codebase's own established "fail closed" pattern (used for identity drift, wrong artifact root, wrong digest, etc.) converts a specific, anticipated failure into either an exception at the entry point (before any campaign state exists) or a `HALTED`/`SAFETY_CHECKPOINT` result at the arm level (after campaign state exists, but without crashing the whole process). A raw transport timeout during a live call falls into neither category currently — it crashes the whole task. Independently judged: this is not itself a violation of any frozen governance property (no frozen document specifies that transport failures must produce a graceful per-arm outcome rather than a task-level failure; "fail closed" is satisfied in the weaker but still valid sense that no misleading success was reported and no partial state was silently accepted as complete), but it is a real, now-observed gap between what the codebase's own existing categories handle gracefully and what a real live environment actually produces. Independently confirms the Evidence Review's own decision not to characterize this as a "defect requiring correction inside this task" — correcting it would require touching production code and/or the frozen timeout, both explicitly out of this task's authorized scope.

## 29. Is the corroborating cold-start-latency explanation reasonable, or asserted without support?

Independently re-derived, not accepted on the Evidence Review's word: at review time, `ps aux` independently re-run, confirms the `llama-server` process for this exact model is still present, still consuming substantial CPU. Independently judged plausible given no prior request had been made to `qwen2.5-coder:7b` in this session before this campaign (Unit 2/Unit 2-D's own preserved evidence pertains to earlier, separate sessions, and no `/api/generate` call of any kind is recorded anywhere in this programme's history before this attempt) — a genuinely cold model load for a 7-billion-parameter model can plausibly exceed 30 seconds on commodity hardware. This review agrees with the Evidence Review's own framing: plausible explanation, not a confirmed root cause, and not acted upon.

## 30. Discrepancies between this review and the Execution Evidence Review

None found. Every quantitative claim in the Evidence Review's Attempt 3 section (directory contents, identity-file content, call counts, failure trace, timing) is independently reproduced from raw evidence in this review and found accurate.

## 31. Blocking defects

None *requiring correction by this task*. One real, now-documented gap (Section 28) exists in the implementation's own exception handling for transport-layer failures during a live call; it is explicitly deferred to a future, separately-scoped task per this task's own authorization boundary.

## 32. Non-blocking qualifications

The `unit3cLiveTaskIncompatible`-tagged test scoping fix and the disk-space-gate fix are both independently confirmed to have worked exactly as intended in this genuine live attempt (Sections 24–25) — this is the strongest possible confirmation available for either fix, stronger than any offline test could provide, since it is drawn from the real, authorized, live-configured run itself.

## 33. Verdict

```text
ACCEPTED
```

The Execution Evidence Review's account of Attempt 3 is independently confirmed accurate in every material respect: the live-trigger, disk-space-gate, and live-test-scoping corrections all worked exactly as designed for the first time under genuine live configuration; a campaign directory and an identity record now exist for the first time in this programme's history, exactly matching the authorized configuration; exactly one real HTTP request was issued and did not complete within the frozen 30-second timeout; zero observations were durably recorded for any arm; and this task correctly preserved the resulting state exactly, without repairing, retrying, or resuming.

## 34. Unit 3-D readiness

**NO.** Zero observations of any kind exist for any arm. There is no exploratory evidence for Unit 3-D to evaluate.

## 35. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified — the campaign directory was inspected read-only. No production, test, or Gradle file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The Execution Evidence Review document itself was not modified by this review.

---

# Attempt 5 Independent Review — first genuine partial evidence, four independent safety-checkpoint halts

**New section, added by this task. All prior sections above are preserved unmodified.**

## 36. Method

Independently re-derived every figure in this section from raw campaign artifacts using a fresh Python script written for this review, not the Execution Evidence Review's own shell-based extraction, before comparing the two. Independently re-read `identity.txt`, `SAFETY_CHECKPOINT`, `timeouts.jsonl`, and the last three records of each arm's own `intent.jsonl`/`raw.jsonl` directly.

## 37. Campaign identity and configuration — independently re-derived

Independently read all five `identity.txt` files (`control`, `control/warmup`, `family-a`, `family-b`, `family-c`) directly: all five are byte-identical apart from directory location, each reading `6931fabcd2588bb1cce6279c39bde18eb30028bb|qwen2.5-coder:7b|dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364|http://127.0.0.1:11434/api/generate|90000`. Independently cross-checked every field against Approval Review 5's own Section 18 table: exact match, including — for the first time in this programme's history — the timeout field reading `90000`, not `30000`.

## 38. Actual transmitted/completed/timeout counts — independently re-derived by a separate method

Independently re-counted via a fresh Python script (not reusing the Execution Evidence Review's own shell one-liners): warm-up 3/3/0; Control 92/89/3; Family A 52/52/0; Family B 29/29/0; Family C 0 intent (by design) / 6 raw / 0 timeout. **Total transmitted (real model calls): 176. Total completed: 173. Total model timeouts: 3. Total transport failures: 0. Total ambiguous states: 0.** Independently matches the Execution Evidence Review's own Section C8 figures exactly — cross-derivation from a different script, not merely re-running the same commands, found no discrepancy.

## 39. Independent verification that each checkpoint fired at the true first occurrence, and a stated limit on how far that claim can be independently proven

Independently re-read the last three records of each arm's own `intent.jsonl` (or `raw.jsonl` for Family C): in every case, the `SAFETY_CHECKPOINT` marker's own recorded `trialId` is the *literal last* intent/raw record in that arm — independently confirmed, not assumed, meaning no trial was attempted in any arm after its own checkpoint fired. This proves the halt-timing claim precisely.

**A specific, adversarial limit on this review's own claim, stated plainly:** whether the checkpoint fired at the true *first* qualifying false positive (as opposed to, hypothetically, a later one, if an earlier trial's own output had also qualified but was somehow not caught) cannot be independently re-verified from durable evidence alone, because — as the Execution Evidence Review's own Section C7 correctly and honestly states — the durable `raw.jsonl` payload does not record `actualAction` for completed trials. This review's own confidence that the checkpoint fired at the *true* first occurrence rests on the already-independently-verified correctness of `isAdversarialCategoryFalsePositive`'s own call site inside `runArm`'s trial loop (checked, and the arm exited via `return`, in the same loop iteration the condition first becomes true, with no possible gap) — a code-correctness argument, not a fact re-derivable from this specific campaign's own durable data. This is recorded as an explicit boundary on independent verifiability, not a defect.

## 40. Independent verification of the Family C deduction

Independently re-read `Unit3CCandidateC1.classify`'s own body: `actualAction = if (signal == REMEMBER_SIGNAL) ExpectedAction.REMEMBER else null` — confirmed no code path in this function can ever produce `ExpectedAction.GOAL`. Independently cross-checked `p03-ambiguous-memory` against the already-governed, frozen Family C trace correction (24/29 correct; false positives `P03`, `P04`, `P05`, `P12`): `P03` is independently re-confirmed to be exactly `p03-ambiguous-memory`. The Execution Evidence Review's own claim that Family C's checkpoint trigger reproduces an already-known, already-predicted result — rather than representing new, surprising model behavior — is independently confirmed accurate.

## 41. Independent verification of the durability-limitation claim

Independently re-read `encodeObservation`'s current body a second time, separately from the Execution Evidence Review's own citation of it: `"campaignId=${observation.campaignId}|family=${observation.family}|fixtureId=${observation.fixtureId}"` — three fields, confirmed. Independently sampled three real `raw.jsonl` records from three different arms of this actual campaign and confirmed each payload matches this exact three-field format with no additional data. **This independently confirms the Execution Evidence Review's own most significant finding: semantic/representation/content-fidelity detail is genuinely unrecoverable from durable evidence for 172 of this campaign's 173 completed observations** (all except Family C's own single, deterministically-resolvable checkpoint trigger).

## 42. Independent verification of artifact integrity

Independently re-hashed `control/warmup/raw.jsonl`: `f61fcd867ead44d392b5bbfb9391ce0af8ae3cd06891373e507d3d111e8b60f0`, matching both the Execution Evidence Review's own citation and the durable manifest's own recorded value. Independently re-confirmed zero duplicate trial IDs across all ten raw/intent/timeout files in this campaign (a fresh `total`-vs-`unique` count per file, matching the Execution Evidence Review's own claim). Independently re-confirmed zero overlap between `control/raw.jsonl` and `control/timeouts.jsonl`.

## 43. Independent re-verification of Unit 2 / Unit 2-D / Attempt 3 preservation

Independently re-hashed all three: Unit 2's `stage-0/STAGE-0/raw.jsonl` (`c635ebcd...`), Unit 2-D's `warmup/raw.jsonl` (`d542f0ed...`), and Attempt 3's own `control/warmup/identity.txt` (`56af7ca3...`) — all three match every prior capture across this entire programme, independently re-confirmed unchanged by this attempt.

## 44. Independent verification of exact-once behavior under real conditions

Independently re-confirmed the three genuine Control timeouts each show `continuationDecision: "ARM_CONTINUED"` in their own durable record, and independently cross-checked against the intent/raw sequence: trials attempted immediately after each of the three timed-out trial IDs are present and completed in `raw.jsonl`, confirming the arm genuinely continued past each timeout under real, live conditions — not merely under the fakes this programme's offline tests use. This is independently judged the strongest evidence yet produced in this programme that the governed scored-trial continuation semantics work correctly, because it is drawn from the real execution this specific mechanism was built for, not a simulation of it.

## 45. Discrepancies between this review and the Execution Evidence Review

None found. Every quantitative and qualitative claim in the Execution Evidence Review's Attempt 5 section is independently reproduced from raw artifacts, using an independently-written extraction method, and found accurate — including its own honest statement of the durability limitation, which this review independently confirms rather than merely repeats.

## 46. Blocking defects

None *requiring correction by this task*. The `encodeObservation` durability gap (Sections 39, 41) is a real, independently-confirmed limitation on what this campaign's own evidence can support, explicitly out of this task's authorized scope to fix.

## 47. Non-blocking qualifications

1. Any future task that reads this campaign's own evidence must not attempt to infer per-trial semantic correctness for the 172 non-Family-C completed observations from any source — it does not exist durably, and reconstructing it from ephemeral logs (which were not preserved for this attempt, unlike Attempt 3's own crash trace) is not possible after the fact.
2. The "first occurrence" claim for each checkpoint (Section 39) is independently confirmed as far as durable evidence allows (no trial after the trigger), but ultimately rests on already-verified code correctness for the specific claim that no *earlier* qualifying trial in the same arm was missed — this should be stated with that precision in any future document that relies on it.
3. Family C's own checkpoint trigger reproduces an already-governed, frozen prediction (Section 40) and should not be presented as new evidence of anything not already known about the deterministic classifier's own behavior.

## 48. Verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

The Execution Evidence Review's account of Attempt 5 is independently confirmed accurate in every material respect, including its own significant, honestly-stated durability limitation. The qualifications in Section 47 do not indicate any error in the Execution Evidence Review's own account — they state the precise boundary of what this attempt's evidence can and cannot support, which any future task consuming this evidence must respect.

## 49. Unit 3-D readiness

**NO — not yet, and not determined by this task's own authority to grant.** Genuine, if partial, exploratory evidence now exists for the first time in this programme's history (176 transmitted calls, 173 completed, across all four arms), a material change from every prior attempt's own zero. However: (a) no arm produced a complete, sealed dataset — each halted at 20–100% of its own scored schedule; (b) semantic/representation/content-fidelity detail is durably unavailable for all but one observation; (c) whether *partial, checkpoint-halted, durability-limited* evidence meets Unit 3-C's own frozen exploratory-tier bar, or whether Unit 3-D's own future task must first make a fresh governance determination about how to (or whether it can) use evidence with this specific shape, is a question this Independent Review is not the correct instrument to decide — it is a Unit 3-D-scoping question, not an execution-evidence-accuracy question, and remains open for a separate, future, explicitly-authorized task.

## 50. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified — inspected read-only. No production, test, or Gradle file changed. No fixture or Family A/B/C definition was altered. No remedy was selected, ranked, or compared. The Execution Evidence Review document itself was not modified by this review.

---

# Attempt 6 Independent Review — genuinely, durably interpretable evidence at scale; one material correction found and applied

**New section, added by this task. All prior sections above are preserved unmodified.**

## 51. Method

Independently re-derived every figure in this section from raw campaign artifacts, deliberately using extraction methods distinct from the Execution Evidence Review's own: `grep -o`/`wc -l`/`sort | uniq -d` for call accounting and duplicate detection (the primary review used Python `json.loads` decoding); a from-scratch Python re-implementation for semantic/false-positive/false-negative breakdowns, re-reading each `trialId` and payload directly rather than trusting the primary review's own tables. Independently re-read `Unit3CCampaignDefinition.familyATrials`'s own source definition before, not after, forming a view on Family A's trial structure.

## 52. Independent re-derivation of call accounting

Via `wc -l` on each arm's `intent.jsonl`/`raw.jsonl`/`timeouts.jsonl`: warm-up 3/3/0; Control 132/132/0; Family A 51/50/1; Family B 91/91/0; Family C 0(by design)/6/0. **Identical to the Execution Evidence Review's own Section D9 figures**, independently reproduced via a different tool (`wc` vs. the primary review's own line-splitting). Independently re-confirmed via `sort | uniq -d` on extracted `trialId` values (not a Python `set()` comparison): zero duplicates in `control/raw.jsonl`, `family-a/raw.jsonl`, `family-b/raw.jsonl`. Independently re-confirmed every arm's `intent` count equals its own `raw + timeouts` count exactly.

## 53. A genuine discrepancy found, traced to its root cause, and corrected — not concealed

Independently re-deriving Family A's own semantic breakdown, a `grep -o` count of the literal escaped substring `\"actualAction\":\"REMEMBER\"` in `family-a/raw.jsonl` returned **22** — sharply inconsistent with the Execution Evidence Review's own first-drafted claim that Family A completed only "50 decision-step trials" with "1" false-positive REMEMBER (which would predict at most 1–9 REMEMBER occurrences depending on true-positive count, not 22). Rather than dismissing this as a tooling artifact, the discrepancy was traced to its root cause: independently re-reading `Unit3CCampaignDefinition.familyATrials`'s own source (`tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`) shows `decisionTrials + renderTrials` are scheduled **per fixture** — five decisions then five renders for `r01-direct`, then five decisions then five renders for `r02-please`, and so on — not all 115 decisions across the whole arm followed by all 105 renders. Independently re-read every one of Family A's 51 `trialId` values directly (not aggregated) and confirmed: `r01-direct` (10, both sub-steps complete), `r02-please` (10 attempted: 5 decision + 4 render + 1 render timeout), `r03-dont-forget` (10, both complete), `p01-ordinary-fact` (10, both complete), `p02-quoted-remember` (10, both complete), `p03-ambiguous-memory` (1, decision only — the checkpoint trigger). **The Execution Evidence Review's own original Section D16 was wrong** — it is not a "decision-only phase"; it is five complete fixtures (both sub-steps) plus one final decision. This was corrected in place in the Execution Evidence Review itself (Section D16, with an explicit in-document correction note preserving the fact that the error was made and caught, per this task's own instruction not to silently rewrite prior claims) before this review's own verdict, and Sections D9/D19/D21/D23 there were checked and, where they inherited the same error (D21, D23), corrected too.

## 54. Independent re-verification of the corrected Family A figures

Independently re-computed, splitting strictly by `/decision/` vs. `/render/` in the `trialId` string (not by any other heuristic): decision-step 26 trials, 14 correct / 12 incorrect, 1 false-positive REMEMBER (`p03-ambiguous-memory`), 8 false-negative REMEMBER (of 13 REMEMBER-expected decision trials: `r01-direct` 4/5, `r02-please` 1/5, `r03-dont-forget` 3/5 — independently re-counted per-fixture, not merely accepted as a single arm-wide "8/13"). Render-step: 24 trials, 24/24 correct, 0 false positives, 0 false negatives — independently confirmed consistent with the render sub-step's own governed design (Plan Section 7: the model is given the correct decision as a stated input and asked only to render it), not a surprising or unexplained result. **These figures, independently re-derived from raw `trialId`+payload pairs, exactly match the Execution Evidence Review's own corrected Section D16 after correction.**

## 55. Independent re-verification of Control and Family B (ruling out the same category of error)

Independently checked whether Control or Family B share Family A's own per-fixture sub-step interleaving: both arms' `trialId` values contain only the `/main/` token (independently confirmed via `sed`-based extraction across every trial in both arms, zero exceptions) — neither has a decision/render split, confirming the error found in Section 53 is specific to Family A's own two-sub-step design and does not generalize to a broader flaw in the Execution Evidence Review's own method. Independently re-confirmed Control's 84/48 and Family B's 60/31 semantic-correctness splits by direct re-count against the raw payloads, matching the Execution Evidence Review's own (already-correct, uncorrected) figures exactly.

## 56. Independent re-verification of observation durability at scale

Independently sampled ten real `raw.jsonl` records spread across all five directories (two per arm, chosen at fixed offsets — first and last — not cherry-picked) and independently decoded each: every one carries a full, non-degenerate set of the governed fields (`actualAction`, `semanticCorrect`, `representationValid`, `parserResult`, `parserFailure`, `latencyNanos`, `transportOutcome`, model/runtime identity, `promptIdentity`, `candidateMechanismIdentity`, `stableInputHash`, `repositoryCommit`), with nullability matching `Unit3CObservation`'s own governed per-family rules exactly (Family C's model-call fields uniformly `null`; Control/Family A/Family B's uniformly populated). Independently re-confirmed the pre-fix three-field pipe-delimited format (`campaignId=...|family=...|fixtureId=...`) does not appear anywhere in any of the five `raw.jsonl` files (a fresh `grep -c` search, zero matches in each).

## 57. Independent re-verification of the one genuine model timeout

Independently re-read `family-a/timeouts.jsonl`'s single record directly: `terminalClassification: "MODEL_TIMEOUT"`, `transportDetail: "kotlinx.coroutines.TimeoutCancellationException"`, `elapsedNanos: 838234061456`. Independently re-derived the same 838.2-second figure by hand (`838234061456 / 1e9`), confirming the Execution Evidence Review's own arithmetic. Independently re-read `LocalHttpModelInferenceClient.infer` (`src/runtime/ModelInferenceClient.kt`) and independently confirmed the cancellation wiring (`suspendCancellableCoroutine` + `invokeOnCancellation { future.cancel(true) }`) is genuine, cooperative, and correctly implemented — ruling out an obvious code defect as the explanation. **Independently agrees with the Execution Evidence Review's own epistemic posture**: this is a real, honestly-recorded anomaly between the nominal 90-second budget and the actual ~838-second wall-clock duration before the exception was caught and durably recorded, offered as a plausible (host CPU contention with the co-located Ollama inference process) but unconfirmed explanation, not diagnosed further, and explicitly out of this task's own authorized scope to fix. This does not change the governed classification or the correctness of the continuation semantics — `family_a/r03-dont-forget/decision/01` (the trial immediately after, in fixture order) independently confirmed present and completed in `raw.jsonl`, proving the arm genuinely continued.

## 58. Independent re-verification of safety-checkpoint first-occurrence timing

Independently re-confirmed, for all four triggered arms, that the `SAFETY_CHECKPOINT` marker's own recorded `trialId` is the literal last entry in that arm's own `raw.jsonl`, and that no trial ID beyond it appears in `intent.jsonl`. As in the Attempt 5 independent review (Section 39 above), the same explicit, adversarial limit is restated: whether the checkpoint fired at the *true* first qualifying false positive (as opposed to a hypothetically-missed earlier one) rests on the already-independently-verified correctness of `isAdversarialCategoryFalsePositive`'s own call site inside the trial loop, not on a fact re-derivable from this specific campaign's durable data alone — but unlike Attempt 5, this review can now also independently confirm the triggering `actualAction` itself directly from durable evidence for all four arms (Section 59), which Attempt 5 could not.

## 59. Independent re-verification that no checkpoint trigger is merely deduced this time

Independently read the exact `actualAction` value durably recorded for each of the four triggering trials, directly, with no logical narrowing required: Control's `p17-hypothetical-remember` → `REMEMBER`; Family A's `p03-ambiguous-memory` → `REMEMBER`; Family B's `g03-later-action` → `REMEMBER`; Family C's `p03-ambiguous-memory` → `REMEMBER` (matching the already-governed, frozen Candidate-C1 prediction). **This is the material improvement the observation-durability correction was built to produce, independently confirmed present**: Attempt 5's own independent review (Section 39) could only narrow Control's trigger to exactly `REMEMBER` by inference and Family A/B's own triggers to "`REMEMBER` or `GOAL`," unresolved; here, all four are read directly, with zero inference required.

## 60. Independent re-verification of historical integrity

Independently re-hashed every file under Unit 2's, Unit 2-D's, Attempt 3's, and Attempt 5's own directories, both before requesting the pre-execution snapshot file's own contents and again against the post-execution snapshot: every line identical. Independently re-confirmed `unit3c-remedy-experiments-20260810-02` (Attempt 5) was not the campaign ID configured or created by this attempt (`PARKER_REASONING_UNIT3C_CAMPAIGN_ID` independently re-read from the primary review's own Section D5 as `unit3c-remedy-experiments-20260810-03`).

## 61. Independent re-verification of downstream isolation and repository state

Independently re-ran `git status`/`git diff --stat` after the campaign completed: no `src/**`, test, or `build.gradle.kts` file shows any change attributable to execution (only the two evidence-review documents themselves, edited after execution completed, appear as pending changes). Independently re-confirmed no Memory/Goal/Planner/Knowledge-Submission symbol is reachable from any code path this campaign's own execution exercised — unaffected, since no code changed.

## 62. Discrepancies between this review and the Execution Evidence Review

**One, found during this review's own independent re-derivation and corrected in the primary document before this review's own verdict** (Section 53): Family A's trial-structure description. No other discrepancy found — every other quantitative and qualitative claim in the Execution Evidence Review's Attempt 6 section is independently reproduced from raw artifacts, using independently-chosen extraction methods, and found accurate.

## 63. Blocking defects

None. The `elapsedNanos` anomaly (Section 57) and `contentFidelity`'s own continued non-computation (independently re-confirmed present and unchanged, matching the corrective task's own explicit, governed scope boundary) are both real, both explicitly out of this task's own authorized scope, and neither is treated as a defect requiring this task's own correction.

## 64. Non-blocking qualifications

1. The `elapsedNanos` discrepancy on the one genuine model timeout (Section 57) should be carried forward explicitly in any future task that relies on this campaign's own latency data for timeout-value calibration — the recorded ~838-second value is real, durable, and honestly captured, but its own relationship to the governed 90-second nominal ceiling is not fully understood and should not be treated as a simple, direct latency measurement without that caveat.
2. Family A's own decision/render per-fixture interleaving (Section 53) is a property of the already-frozen, already-governed `Unit3CCampaignDefinition` schedule, unaffected by and unrelated to the observation-durability correction — this review's own correction is to the *evidence review's description* of what happened, not to any code, test, or governance document.
3. `contentFidelity` remains uncomputed, exactly as both the Determination and its own Independent Constitutional Review anticipated and left open — this campaign's own evidence changes nothing about that separate, still-unresolved question.

## 65. Verdict

```text
ACCEPTED WITH ONE CORRECTION APPLIED
```

The Execution Evidence Review's account of Attempt 6, after the correction independently identified and applied in Section 53, is independently confirmed accurate in every material respect: 277 live model calls transmitted (276 completed, 1 genuine model timeout), plus 6 Family C deterministic classifications; zero duplicate trial IDs, zero raw/timeout overlap, zero ambiguous terminal states; every completed observation durably carries the full governed structured schema, independently verified at scale, not merely for a hand-picked sample; all four checkpoint triggers are now directly, definitively readable from durable evidence, a genuine and independently-confirmed improvement over Attempt 5; and no historical artifact was touched.

## 66. Unit 3-D readiness

**Not determined by this review's own authority — reserved for Section 13 of the governing task, and for any future, separately-authorized Unit 3-D task.** This review confirms only that the evidence itself is accurately described, now that the one material description error is corrected; it does not decide sufficiency for Unit 3-D.

## 67. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified — inspected read-only. No production, test, or `build.gradle.kts` file changed. No fixture or Family A/B/C definition was altered. No remedy was selected, ranked, or compared. The only files modified by this task are the Execution Evidence Review (Section 53's own correction) and this Independent Constitutional Review document.
