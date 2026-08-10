**Status:** Unit 3-C Controlled Remedy Experiments — Implementation Readiness Review — **REFRESHED (second refresh). READY**, with the same one explicitly named, structurally-unavoidable residual note as before — now strengthened, not weakened, by this task's own correction. This document does not authorize execution.

# Unit 3-C Controlled Remedy Experiments — Implementation Readiness Review

## 1. Question

Is the implemented instrument technically and constitutionally ready to proceed to the separate, future Explicit Execution Approval governance step? The immediately prior version of this review answered "READY" — but that determination was subsequently superseded by the failed Explicit Execution Approval Review, which independently traced the schedule into the driver's own execution path and found the three warm-up trials were never actually run. This refresh does not assume the prior "READY" verdict; it re-derives readiness fresh, dimension by dimension, now that the warm-up defect is corrected and independently re-verified.

## 2. Clean exact call schedule

**Ready — and now more rigorously so than at any prior point in this programme.** 483 is independently derived by the schedule's own driver-free arithmetic (unchanged) *and*, for the first time, by direct measurement of the orchestration driver's own executor-invocation count during a full run (`exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups`). This closes exactly the gap between "the schedule says 483" and "the driver does 483" that the failed Approval Review identified.

## 3. Live task isolation

**Ready**, unchanged: detached Gradle task, structural source-set exclusion, config-gated entry point.

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

## 15. The residual note, re-derived, not merely carried forward

The real, live-calling executor bridge (`buildModelInvokingExecutor`, inside `Unit3CLiveEntryPoint`) remains, exactly as before, structurally unexercised by any test in this codebase — this is unaffected by the warm-up correction, which touched only the orchestration driver's *scheduling* of trials, not the executor-construction code that would actually issue a live call. **This residual note is not weakened by the warm-up defect having existed, nor is it resolved by its correction** — it is a separate, independent property of building live-calling code under a no-live-calls constraint. What *has* changed, and materially strengthens the note's own recommended mitigation, is that the warm-up mechanism this note relies on as "the natural check before committing to the full schedule" is now independently verified to actually execute — previously, recommending reliance on the warm-ups as a mitigation was itself resting on an unverified assumption (which turned out to be false); it now rests on a directly, freshly tested guarantee.

## 16. Readiness determination

```text
READY
```

**All sixteen dimensions independently re-derived as ready**, not assumed. The warm-up orchestration defect discovered by the failed Explicit Execution Approval Review is corrected and independently re-verified; every other dimension is either unchanged and re-confirmed, or — in the case of exact-once/recovery and sealing — more rigorously verified than before, specifically because the correction required and produced new, warm-up-specific tests. The one residual note (Section 15) remains, unavoidably, for the same reason it always has: no test in a task bound by a no-live-calls constraint can exercise the live HTTP bridge itself. Its own recommended mitigation is now independently confirmed to be real rather than assumed.
