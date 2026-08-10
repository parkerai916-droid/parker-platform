**Status:** Independent Constitutional Review of the Unit 3-C Execution Evidence Review — **ACCEPTED (halt confirmed independently; no evidence exists to evaluate).** This review does not merely accept the Execution Evidence Review's own account of Attempt 2 — it independently re-derives the halt from raw source, the actual background-run output log, and a fresh filesystem listing, before agreeing with it. No live model call, no HTTP call beyond none (this review made none), no campaign, and no repository mutation beyond this document occurred.

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
