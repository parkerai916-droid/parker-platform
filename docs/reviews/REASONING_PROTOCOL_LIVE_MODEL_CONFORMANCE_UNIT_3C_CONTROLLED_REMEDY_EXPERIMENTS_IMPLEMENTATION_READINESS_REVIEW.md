**Status:** Unit 3-C Controlled Remedy Experiments — Implementation Readiness Review — **REFRESHED (fourth refresh). READY.** The prior refresh's Section 2a table proved every link it enumerated, but — as this refresh now states plainly rather than glossing over — it never enumerated the disk-space gate as a distinct link at all, which is exactly why the first genuine live-execution attempt still halted despite that "proof." This refresh adds the missing link explicitly (Section 2a) and corrects it (Section 6). This document does not authorize execution.

# Unit 3-C Controlled Remedy Experiments — Implementation Readiness Review

## 1. Question

Is the implemented instrument technically and constitutionally ready to proceed to the separate, future Explicit Execution Approval governance step? Prior refreshes answered "READY" twice before, each time superseded by a live-execution attempt finding a gap the review's own "proof" had not actually covered: first, warm-ups counted but never run; second, no caller reaching the entry point at all; third — the immediately prior refresh's own gap, only now visible — a full path table (Section 2a) that proved trigger reachability, config validation, artifact-root validation, and driver/executor wiring, but never listed the disk-space gate as its own link, so its failure against a real, not-yet-created campaign directory was never caught by that "proof." This refresh does not assume any prior "READY" verdict; it re-derives readiness fresh, explicitly re-enumerating every link in the path, including the one the immediately prior refresh omitted.

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
| **Disk-space gate, on a real, existing filesystem, succeeds even though the campaign directory does not yet exist** — *the link the immediately prior refresh's own table omitted entirely* | `driver.run()`'s literal first statement now checks `artifactRoot.parent`, the already-existing, already-governed durable root, not the not-yet-created campaign subdirectory; proven both by a fake-lambda test asserting the checked path equals the parent, and by a second test using the real, unmocked `Files.getFileStore` default against a genuinely non-existent child directory | Fresh test run, both fake and real filesystem call |
| Driver includes warm-ups, then Control/Family A/Family B/Family C in frozen order, totaling 483 | Unaffected by this task's diff; independently re-run `exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups` — unchanged, still passing | Fresh test run (pre-existing test, unaffected) |
| Durable artifacts | `Unit3CArmLedger` (unmodified) writes exact-once, sealed, recoverable ledgers; unaffected by this task's diff | Unaffected pre-existing coverage |

No live call is required to establish any row above: every row is either a source-level structural fact (independently re-read, not merely quoted from a prior review) or the result of an offline test using fake or, for the disk-space row specifically, real (non-network) filesystem calls. **This table now explicitly includes the disk-space gate as its own link, corrected specifically because its absence from the immediately prior refresh's own version of this table is exactly why that refresh's "proven, not assumed" verdict still missed the defect the first genuine live attempt found.** This refresh does not repeat that omission for any other link: every step between the Gradle task and the first possible live HTTP call has now been individually enumerated and individually proven.

## 3. Live task isolation

**Ready**: detached Gradle task, structural source-set exclusion, config-gated entry point; the config gate is proven (Section 2a) to be reached by a real caller. **Additionally corrected this refresh:** the detached task's own test selection previously included one offline-only verification test (asserting no real campaign ID is present) unconditionally, which would necessarily fail during any genuine live-configured run of that exact task. Now excluded from that task's selection via `@Tag("unit3cLiveTaskIncompatible")` + `excludeTags`, while remaining selected and passing under the general offline `reasoningProtocolLiveModelEvaluation` task — independently re-confirmed via a fresh run showing 49 tests (including the tagged one) there, versus 48 (excluding it) under the live task.

## 4. Model/config identity requirements

**Ready**, unchanged.

## 5. Artifact-root requirements

**Ready**, unchanged from the immediately prior refresh; unaffected by the warm-up correction, which added a new sub-directory (`control/warmup/`) beneath the same, already-restricted parent, not a new root.

## 6. Disk-space requirements

**Ready — corrected this refresh, and now proven against the exact real-world condition that previously halted execution.** The gate still runs once, before the arm loop begins, still enforces the unchanged 2 GiB minimum, and still fails closed on any unreadable filesystem state. What changed: it now checks the campaign directory's already-existing, durable *parent* rather than the not-yet-created campaign directory itself — the first genuine live-execution attempt discovered that checking the campaign directory directly meant the gate could never pass on a first-ever campaign, since nothing creates that directory before the gate runs. Proven both by a fake-lambda test asserting the correct path is checked, and by a second test exercising the real, unmocked `Files.getFileStore` default against a genuinely non-existent directory.

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

## 15. The residual note, re-derived and materially narrowed, and honestly corrected where the immediately prior version overstated it

**The immediately prior refresh's own version of this note claimed "every gate before the HTTP call is now independently proven reachable" — that claim was false, though not dishonestly so: it was true of every link the prior refresh's own Section 2a table actually enumerated, but that table omitted the disk-space gate as a distinct link, so its failure mode was simply never checked by the "proof" the note relied on.** This refresh does not repeat that overstatement. With the disk-space gate now explicitly enumerated (Section 2a) and corrected (Section 6) and proven against the real, unmocked filesystem call, the claim can now honestly be made with that gate included: the path from the detached Gradle task through the trigger, config validation, artifact-root validation, the disk-space gate, and into the real orchestration driver is genuinely reachable and genuinely passable. What remains unexercised — and structurally must remain unexercised under this task's own no-live-calls constraint — is exactly and only the actual HTTP request `buildModelInvokingExecutor`'s live executor issues once the driver calls it, and the actual response the runtime returns. This residual is not smaller than what the prior refresh *claimed*, but it is, for the first time, actually true as claimed, rather than true-except-for-one-unenumerated-gate.

## 16. Readiness determination

```text
READY
```

**All dimensions independently re-derived as ready**, not assumed, with the disk-space gate now explicitly included as its own dimension rather than folded silently into "driver reachability" the way the immediately prior refresh did. The warm-up defect, the live-trigger defect, the disk-space-gate defect, and the live-test-scoping defect are all independently confirmed corrected and independently re-verified; every other dimension is either unchanged and re-confirmed, or more rigorously verified than before. The one residual note (Section 15) remains, and for the first time can be stated without a hidden gap: only the live HTTP call's own success or failure is unverified. Every gate and every caller between the Gradle task and that HTTP call — including, now, the disk-space gate specifically — is independently proven to exist, to be reachable, and to actually pass under real (non-network) filesystem conditions.
