**Status:** Unit 3-C Controlled Remedy Experiments — Implementation Readiness Review — **REFRESHED. READY**, with one explicitly named, structurally-unavoidable residual note for the future Explicit Execution Approval Review to consider — not a blocking implementation gap. This document does not authorize execution.

# Unit 3-C Controlled Remedy Experiments — Implementation Readiness Review

## 1. Question

Is the implemented instrument technically and constitutionally ready to proceed to the separate, future Explicit Execution Approval governance step? The prior version of this review answered "NOT YET FULLY READY," identifying five gaps. Four were implementation gaps, closed in this task. The fifth (the Plan's own Family C trace-table inaccuracy) was closed separately, by governance correction, before this task began. This refresh reassesses all sixteen dimensions fresh.

## 2. Clean exact call schedule

**Ready.** 483, now independently derived a fourth way — directly from the orchestration driver's own execution against counting fake executors, in addition to the two derivations already established and the per-arm trial-list counts.

## 3. Live task isolation

**Ready**, unchanged and re-verified: detached Gradle task (now correctly filtering both test classes), structural source-set exclusion from `test`/`check`/`build`, config-gated entry point.

## 4. Model/config identity requirements

**Ready**, unchanged.

## 5. Artifact-root requirements

**Ready — closed in this task.** `Unit3CArtifactRootPolicy` hard-restricts the live artifact parent to exactly the same, already-governed durable root Unit 2 and Unit 2-D use, with an explicit nesting guard against both preserved campaign directories, matching Unit 2-D's own precedent exactly. Nine dedicated tests independently prove rejection of every category the original gap analysis named.

## 6. Disk-space requirements

**Ready — closed in this task.** 2 GiB minimum, reusing Unit 2-D's own already-governed value (the Plan itself states neither a number nor a derivation), checked before any executor is invoked, fails closed on both insufficient and unreadable filesystem state, four dedicated tests.

## 7. Exact-once/recovery

**Ready**, and one real bug found and fixed in this task (`checkIdentity`'s missing directory creation) — the component is now more genuinely ready than the previous review's "ready" assessment realized, since that bug would have caused a crash on any arm's very first identity check in a fresh campaign directory.

## 8. Campaign identity

**Ready**, unchanged.

## 9. Supplemental fixtures

**Ready**, unchanged.

## 10. Family C safety coverage

**Ready, and the previously-noted documentation caveat is now fully resolved.** The frozen Plan's own Section 9 table is corrected and committed (`08f3692`); the implementation already matched the correction before this task began. All nine mandatory adversarial categories remain represented; the classifier's true, now four-times-independently-verified predicted-error profile (24/29 correct, four false positives, one false negative) is available for any future execution's interpretation against an accurate governing document.

## 11. Downstream isolation

**Ready**, unchanged, re-verified across both files.

## 12. Stop conditions

**Ready — the previously-missing half is closed in this task.** The fail-closed half (identity drift, checkpoint-without-raw, seal violations) remains ready as before. The manual, non-numeric, first-occurrence safety-review checkpoint for adversarial-category false positives is now implemented exactly as the frozen Plan requires, verified by a dedicated test proving the triggering observation is preserved, the arm halts before completing its schedule, sealing is blocked while the checkpoint is set, and no automatic continuation path exists.

## 13. Sealing

**Ready**, unchanged.

## 14. Evidence integrity

**Ready**, unchanged.

## 15. One explicitly named residual note — not a blocking gap

The real, live-calling executor code inside `Unit3CLiveEntryPoint` (constructed via `buildModelInvokingExecutor`, bridging to real `ModelReasoningProvider`/`LocalHttpModelInferenceClient`/`TaggedReasoningResponseParser` instances) has, by this task's own explicit and repeated prohibition on live calls, **never been executed even once** — not in this task, not in the prior one. Every other component of this instrument (schedule, fixtures, mechanisms, artifact schema, ledger, artifact-root policy, disk-space gate, safety checkpoint, and the orchestration driver's own control flow) is exercised by dozens of passing offline tests using fake executors. The bridge to the real, live HTTP path is not — it cannot be, without violating this task's own governing constraint. Its correctness rests on code review and on the fact that it reuses, unmodified, exactly the same production classes Unit 1, Unit 2, and Unit 2-D's own already-proven live paths already use, in the same way — a reasoned basis for confidence, not a substitute for the bridge's own first real exercise. This is not characterized as a defect or a gap requiring further pre-execution implementation work; it is a structurally unavoidable property of any live-calling code built under a no-live-calls constraint, named explicitly so it is not mistaken for something this review overlooked. The natural mitigation — a small warm-up before committing to the full 483-call schedule — is already part of the frozen Plan's own call accounting (Section 11's three warm-up calls) and is exactly the kind of check the future Explicit Execution Approval Review should specifically confirm succeeds before authorizing the remaining schedule.

## 16. Readiness determination

```text
READY
```

**All sixteen dimensions are ready.** The four implementation gaps this review previously identified (orchestration driver, artifact-root hard restriction, disk-space check, manual safety-review checkpoint) are closed and independently tested in this task. The fifth, previously-identified gap (the Plan's own trace-table inaccuracy) was closed separately, by governance correction, before this task began, and the implementation is confirmed to already match the corrected Plan. One residual note (Section 15) is named explicitly for the Explicit Execution Approval Review's own attention — the live-calling bridge's first real exercise necessarily occurs during actual execution, not before — but this does not itself withhold a READY determination, since no further pre-execution implementation work would resolve it; only execution itself can.
