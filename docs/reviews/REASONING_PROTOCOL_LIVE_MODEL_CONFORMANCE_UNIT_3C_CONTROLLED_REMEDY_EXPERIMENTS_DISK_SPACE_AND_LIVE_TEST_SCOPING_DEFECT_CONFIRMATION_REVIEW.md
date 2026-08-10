**Status:** Unit 3-C Controlled Remedy Experiments — Disk-Space and Live-Test-Scoping Defect Confirmation Review — **BOTH DEFECTS CONFIRMED (Classification A), both independently corrected within the narrowest constitutionally valid scope.** No live model call, no HTTP call, no campaign, and no unauthorized code change occurred.

# Unit 3-C Controlled Remedy Experiments — Disk-Space and Live-Test-Scoping Defect Confirmation Review

## 1. Baseline

`HEAD` = `origin/main` = `77a9917647d7edc39ad790fa712ff1c958ec5a64`, clean, independently re-confirmed at task start. No Unit 3-C campaign directory existed. No live environment variable was set.

## 2. Method

Independently traced both defects from primary source before making any change, per this task's own explicit instruction not to trust the reporting task's own diagnosis blindly.

## 3. Defect 1 — disk-space gate

**Trace:**

1. **Exact path passed into `Unit3CDiskSpaceGate.check`:** independently read `Unit3COrchestrationDriver.run()` (`ReasoningProtocolUnit3COrchestrationTest.kt`, pre-fix): `Unit3CDiskSpaceGate.check(artifactRoot, usableSpace)`, where `artifactRoot` is the constructor's own `private val artifactRoot: Path` — the exact value `Unit3CLiveEntryPoint.run` passes in, which is `Unit3CArtifactRootPolicy.resolve(...)`'s return value.
2. **Campaign directory or durable parent?** Independently re-read `Unit3CArtifactRootPolicy.resolve`: it returns `parentPath.resolve(campaignId).normalize()`, guaranteed equal to `"$UNIT_3C_ARTIFACT_ROOT_PREFIX/$campaignId"`. This is the **campaign-specific subdirectory**, not the durable parent.
3. **Does it exist before the gate runs?** Independently confirmed: no code path reachable before `Unit3CDiskSpaceGate.check` creates any directory. `Unit3CConfigLoader.load` and `Unit3CArtifactRootPolicy.resolve` are both pure computation (no I/O, independently re-confirmed by reading both bodies in full). The only `Files.createDirectories` call in either file is inside `Unit3CArmLedger.checkIdentity`, reachable only from `runArm`, which is downstream of, and never reached before, the disk-space gate. For a first-ever campaign, the campaign directory therefore does not exist when the gate runs.
4. **What `Files.getFileStore` does for a nonexistent path:** independently confirmed via the actual halted live-execution attempt's own raw log (`docs/reviews/..._EXECUTION_EVIDENCE_REVIEW.md`, Attempt 2): `Files.getFileStore` on a nonexistent path throws `NoSuchFileException` (a JDK-standard `IOException` subtype), which `Unit3CDiskSpaceGate.check`'s own `catch (e: IOException)` block converts to `Unit3CInsufficientSpaceException`. This is not a hypothesis — it is exactly what was observed to actually happen during the real, authorized live-execution attempt.
5. **Narrowest correct semantics:** check free space against the campaign directory's **already-existing, durable parent** — the same filesystem/mount the campaign directory will be created under — rather than the not-yet-created campaign directory itself. The parent is guaranteed to exist (it is the hard-restricted, already-governed artifact root, independently confirmed present with `steve:steve`/`700` ownership before every attempt in this programme's history) and is on the same mount, so its usable-space reading is an accurate proxy for the constraint that actually matters.

**Classification: A — IMPLEMENTATION DEFECT CONFIRMED.** The gate's own fail-closed design is correct; its *target path* was wrong. Reproduced independently from the halted live attempt's own raw evidence, not merely accepted from the reporting task's diagnosis.

## 4. Defect 2 — live-test scoping

**Trace:**

1. **Location:** independently located `live Unit 3-C trigger requires a real campaign ID even when the detached task's own property is already set` (`ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`), asserting `assertNull(System.getenv(Unit3CConfigLoader.CAMPAIGN_ID))`.
2. **Does the detached live task select it?** Independently re-read `build.gradle.kts`'s pre-fix `unit3cControlledRemedyExperiments` registration: `filter { includeTestsMatching("parker.integration.ReasoningProtocolUnit3CControlledRemedyExperimentsTest") ... }` — a whole-class pattern with no method-level exclusion. Independently confirmed via the actual halted live-execution attempt's own raw log: this test ran and failed (`expected: <null> but was: <unit3c-remedy-experiments-20260810>`) during that real attempt.
3. **Does genuine live configuration necessarily make it fail?** Yes, by construction: a genuine live run requires `PARKER_REASONING_UNIT3C_CAMPAIGN_ID` to be set (the trigger's own second `assumeTrue` gate depends on it), which is exactly the value this assertion requires to be absent. There is no configuration under which both the trigger fires correctly and this assertion passes.
4. **Where does it belong?** Independently determined: the assertion is a legitimate, correct offline/ordinary-verification fact (no real campaign ID should be present outside a genuine live run) — it is not wrong in *substance*, only in *scope*: it must not be selected by the one task whose entire purpose is to sometimes run with that variable genuinely present.

**Classification: A — IMPLEMENTATION DEFECT CONFIRMED** (test-scoping category). The assertion's own semantic content is correct and must be preserved, per this task's own explicit preference for "explicit test filtering/tagging/selection over changing the assertion's semantic purpose."

## 5. Governance boundary (Phase 3)

Both corrections independently confirmed to leave every frozen property unchanged:

| Property | Status |
|---|---|
| Campaign ID semantics | Unchanged — `Unit3CConfigLoader.CAMPAIGN_ID`, marker prefix, regex all untouched |
| Artifact root parent | Unchanged — `UNIT_3C_ARTIFACT_ROOT_PREFIX` value untouched; the fix changes *which path is checked*, not the accepted root value itself |
| 2 GiB minimum | Unchanged — `UNIT_3C_MINIMUM_FREE_BYTES` value untouched (independently re-confirmed: the constant does not appear in either diff hunk) |
| 483-call schedule | Unchanged — `liveModelCallCount == 483` line untouched (independently re-confirmed absent from the diff) |
| Warm-ups / Control / Family A / Family B / Family C | Unchanged — none referenced by either fix |
| Fixtures / repetitions | Unchanged — not referenced |
| Model / digest requirement / inference configuration / timeout | Unchanged — `UNIT_3C_MODEL_NAME`, `UNIT_3C_TIMEOUT_MS` untouched (independently re-confirmed absent from the diff, appearing only as surrounding context) |
| Exact-once semantics | Unchanged — `Unit3CArmLedger` not referenced by either fix |
| Safety checkpoint | Unchanged — `Unit3CSafetyCheckpoint` not referenced |
| Downstream isolation | Unchanged — no new import, no new dependency |
| Evidence tiers | Unchanged — not referenced |
| Remedy neutrality | Unchanged — neither fix mentions, ranks, or favors any remedy family |

No frozen governance document requires amendment. `git diff --stat -- src/` independently re-confirmed empty.

## 6. Corrections implemented

**Disk-space gate (`ReasoningProtocolUnit3COrchestrationTest.kt`):** `Unit3COrchestrationDriver.run()` now computes `val spaceCheckTarget = requireNotNull(artifactRoot.parent) { ... }` and calls `Unit3CDiskSpaceGate.check(spaceCheckTarget, usableSpace)` — the gate's own contract and every existing standalone gate test are unchanged; only the caller's choice of target path changed. Two new tests added, proving (a) the driver passes the parent, not the campaign directory, to the gate, and (b) the real, default `Files.getFileStore`-based `usableSpace` genuinely succeeds end-to-end against a not-yet-created campaign directory whose parent exists — directly reproducing, entirely offline, the exact condition that halted the real attempt.

**Live-test scoping (`ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, `build.gradle.kts`):** the offending test is now annotated `@Tag("unit3cLiveTaskIncompatible")`; the detached `unit3cControlledRemedyExperiments` task's `useJUnitPlatform` now calls `excludeTags("unit3cLiveTaskIncompatible")`. The assertion's own text and meaning are unchanged. The general offline `reasoningProtocolLiveModelEvaluation` task (no filter, no tag exclusion) still selects and runs it, independently re-confirmed via a fresh run showing the test present and passing there.

## 7. Incidental issue found and corrected during verification

While re-running the full ordinary regression suite after implementing the disk-space fix, a pre-existing Unit 1 test (`detached Gradle task is not connected to ordinary lifecycle tasks`) newly failed. Independently traced (not assumed): the new Gradle-task comment explaining the tag exclusion happened to contain the literal, contiguous substring `reasoningProtocolLiveModelEvaluation`, which fell within that test's own broad regex scan window (`tasks\.(test|check|build|assemble)[^{]*\{[^}]*reasoningProtocolLiveModelEvaluation`, `DOT_MATCHES_ALL`) and produced a false-positive match. This is not a defect in the Unit 1 test; it is an incidental side effect of comment wording. Corrected by rewording the comment to avoid the literal substring, consistent with this codebase's established convention (already used twice elsewhere for the analogous "self-referential denylist string" pattern). Independently re-verified the regex no longer matches, and the full ordinary suite (2015/5-skip/0-fail) is unaffected.

## 8. Verdict

```text
BOTH DEFECTS CONFIRMED (CLASSIFICATION A) — BOTH CORRECTED WITHIN THE NARROWEST VALID SCOPE
```
