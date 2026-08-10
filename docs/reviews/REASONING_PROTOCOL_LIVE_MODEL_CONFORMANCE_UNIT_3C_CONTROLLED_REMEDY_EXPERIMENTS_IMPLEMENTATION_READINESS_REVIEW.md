**Status:** Unit 3-C Controlled Remedy Experiments — Implementation Readiness Review — **REFRESHED (third refresh). READY.** The residual note from the second refresh (Section 15) is not merely carried forward: it is materially narrowed by this task's own correction, which for the first time proves — not assumes — that the full executable path from the detached Gradle task down to the real orchestration driver actually exists and is reachable. This document does not authorize execution.

# Unit 3-C Controlled Remedy Experiments — Implementation Readiness Review

## 1. Question

Is the implemented instrument technically and constitutionally ready to proceed to the separate, future Explicit Execution Approval governance step? The second-prior version of this review answered "READY" but was superseded by the failed Explicit Execution Approval Review, which found the three warm-up trials were never actually run. The immediately prior refresh corrected that and re-derived readiness fresh. That refresh was itself then superseded a second time: the next live-execution attempt discovered that no code path anywhere in the repository actually invoked `Unit3CLiveEntryPoint.run` with real, environment-sourced configuration — meaning every prior "READY" verdict, including the immediately prior one, was correct about the instrument's own internal logic but had never verified that the instrument was *reachable* at all. This refresh does not assume any prior "READY" verdict; it re-derives readiness fresh, dimension by dimension, and — per this task's own explicit instruction — proves, rather than assumes, that the full path now exists (Section 2a).

## 2. Clean exact call schedule

**Ready — and now more rigorously so than at any prior point in this programme.** 483 is independently derived by the schedule's own driver-free arithmetic (unchanged) *and*, for the first time, by direct measurement of the orchestration driver's own executor-invocation count during a full run (`exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups`). This closes exactly the gap between "the schedule says 483" and "the driver does 483" that the failed Approval Review identified.

## 2a. Full executable path — proven, not assumed

This is the specific gap the immediately prior "READY" verdict did not close, discovered only when a live-execution attempt tried to exercise it for real. Each link below is proven by direct, offline, structural evidence gathered in this task — not inferred from any single link's own internal correctness in isolation.

| Link | Evidence | Proof type |
|---|---|---|
| Detached Gradle task exists and is isolated from ordinary lifecycle | `build.gradle.kts` `unit3cControlledRemedyExperiments` registration; `./gradlew test --rerun-tasks` produces zero Unit 3-C test-result files | Fresh source read + fresh operational run |
| Detached task's test filter reaches the new trigger method | `includeTestsMatching("parker.integration.ReasoningProtocolUnit3CControlledRemedyExperimentsTest")` is a whole-class pattern; the task's own re-run reports 49 tests from that class (40 prior + 9 new, including the trigger) | Fresh operational run, count reconciliation |
| Trigger is gated, not unconditional | Trigger test result is `skipped` under this task's own execution (property true, campaign ID absent) — a status reachable only via a genuinely-thrown `assumeTrue` abort | Fresh operational run, JUnit XML inspection |
| Trigger calls the real entry point, exactly once, with real environment | Source-scan test independently counts exactly one occurrence of `Unit3CLiveEntryPoint.run(System.getenv(), Path.of("."))` inside the trigger function body, gated by exactly two `assumeTrue` calls | Structural/source-level test, independently re-read |
| Entry point performs config validation before anything else | `Unit3CConfigLoader.load` is the first call inside `Unit3CLiveEntryPoint.run`; four new negative tests independently confirm it fails closed (wrong model, blank digest, malformed campaign ID) before any artifact-root or driver code runs | Fresh test run + source read |
| Entry point performs artifact-root validation next | `Unit3CArtifactRootPolicy.resolve` is the second call; one new negative test independently confirms it fails closed (wrong root) before any driver is constructed | Fresh test run + source read |
| Entry point constructs the real, already-governed orchestration driver and invokes it with all four family executors | Source-scan test independently confirms the entry point's body contains `Unit3COrchestrationDriver(config.campaignId, artifactRoot, config.identity)`, `driver.run(executors)`, and executor entries for all four `Unit3CFamily` values | Structural/source-level test |
| Driver includes warm-ups, then Control/Family A/Family B/Family C in frozen order, totaling 483 | Unaffected by this task's diff; independently re-run `exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups` — unchanged, still passing | Fresh test run (pre-existing test, unaffected) |
| Durable artifacts | `Unit3CArmLedger` (unmodified) writes exact-once, sealed, recoverable ledgers; unaffected by this task's diff | Unaffected pre-existing coverage |

No live call is required to establish any row above: every row is either a source-level structural fact (independently re-read, not merely quoted from a prior review) or the result of an offline test using fake executors. This closes the exact gap the halted execution attempt found: previously, every link past "entry point" was correct in isolation but the chain had no first link connecting the Gradle task to it at all.

## 3. Live task isolation

**Ready**, unchanged: detached Gradle task, structural source-set exclusion, config-gated entry point. **Additionally now proven** (Section 2a) that the config gate is not merely present in the entry point's own code but is actually reached by a real caller.

## 4. Model/config identity requirements

**Ready**, unchanged.

## 5. Artifact-root requirements

**Ready**, unchanged from the immediately prior refresh; unaffected by the warm-up correction, which added a new sub-directory (`control/warmup/`) beneath the same, already-restricted parent, not a new root.

## 6. Disk-space requirements

**Ready**, unchanged. The disk-space gate runs once, before the arm loop (including the now-corrected warm-up step) begins; unaffected by the correction's location entirely within arm-level execution.

## 7. Exact-once/recovery

**Ready, and now genuinely exercised by warm-up-specific scenarios it was not exercised by before.** The correction reuses `Unit3CArmLedger` entirely unchanged, but this task added dedicated tests proving exact-once behavior specifically for the warm-up sub-ledger: exactly-once execution, order preservation, idempotent crash-recovery (a second full run re-invokes the executor for none of the three already-completed warm-ups), and correct arm-scoped failure isolation (a corrupted warm-up identity halts only Control, not Family A/B/C). This is strictly more verification than existed before this task, not merely a re-confirmation.

## 8. Campaign identity

**Ready**, unchanged.

## 9. Supplemental fixtures

**Ready**, unchanged; not touched by this task's correction.

## 10. Family C safety coverage

**Ready**, unchanged; the corrected Plan trace (24/29, four false positives including `P12`, one false negative) remains correctly reflected, re-verified by a fresh test run, untouched by this task's diff.

## 11. Downstream isolation

**Ready**, unchanged, re-verified across both files including the new warm-up code.

## 12. Stop conditions

**Ready**, unchanged in substance; the warm-up correction adds one new, narrow application of the already-existing fail-closed pattern (a corrupted warm-up ledger halts Control) rather than a new stop-condition category.

## 13. Sealing

**Ready**, unchanged; warm-ups now participate in the same seal discipline as every other ledger, verified by a dedicated test confirming Control's own scored ledger is never even created if the warm-up gate fails.

## 14. Evidence integrity

**Ready**, unchanged.

## 15. The residual note, re-derived and materially narrowed, not merely carried forward

**This residual note is narrower than either prior refresh's own version of it, because this task closed the specific gap that made the prior versions incomplete.** The prior refresh's note described the entire live-calling executor bridge as "structurally unexercised" — at the time, this was true in the strongest possible sense, because no caller reached the bridge at all; the bridge's own internal correctness had never even been placed on any executable path from the Gradle task. That is no longer the case. Section 2a proves, by direct offline evidence, that the path from the detached Gradle task through the trigger, through config validation, through artifact-root validation, into the real orchestration driver, is now genuinely reachable. What remains unexercised — and structurally must remain unexercised under this task's own no-live-calls constraint — is narrower and more specific: the actual HTTP request `buildModelInvokingExecutor`'s live executor issues once the driver calls it, and the actual response the runtime returns. Every gate *before* that HTTP call is now independently proven reachable; only the HTTP call's own success or failure, which by construction cannot be tested without violating this task's own prohibition, remains unverified. This is a materially smaller, more honestly bounded residual than either prior refresh could truthfully claim.

## 16. Readiness determination

```text
READY
```

**All dimensions independently re-derived as ready**, not assumed, including — for the first time — the reachability dimension itself (Section 2a), which no prior version of this review actually proved rather than implicitly assumed. The warm-up orchestration defect is corrected and independently re-verified; the live-execution trigger defect discovered by the halted execution attempt is corrected and independently re-verified; every other dimension is either unchanged and re-confirmed, or more rigorously verified than before. The one residual note (Section 15) remains, narrower than at any prior point in this programme: only the live HTTP call's own success or failure, which no offline test can exercise under this task's own constraint, is unverified. Every gate and every caller between the Gradle task and that HTTP call is now independently proven to exist and to be reachable.
