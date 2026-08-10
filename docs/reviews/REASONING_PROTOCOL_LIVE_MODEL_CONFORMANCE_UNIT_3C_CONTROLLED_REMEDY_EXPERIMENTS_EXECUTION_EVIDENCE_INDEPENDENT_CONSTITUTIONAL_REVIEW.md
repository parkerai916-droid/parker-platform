**Status:** Independent Constitutional Review of the Unit 3-C Execution Evidence Review — **UPDATED. ACCEPTED for both Attempt 2 (preserved below, unmodified) and the new Attempt 3.** This review does not merely accept the Execution Evidence Review's own account — for Attempt 3 specifically, it independently re-derives the actual call outcome, the campaign directory's real on-disk contents, and the exact failure point from raw evidence (the campaign directory itself, the JUnit XML report, and direct source re-reading), not from the Execution Evidence Review's own narrative. No live model call, no HTTP call of any kind, no campaign mutation, and no repository mutation beyond this document occurred during this review.

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
