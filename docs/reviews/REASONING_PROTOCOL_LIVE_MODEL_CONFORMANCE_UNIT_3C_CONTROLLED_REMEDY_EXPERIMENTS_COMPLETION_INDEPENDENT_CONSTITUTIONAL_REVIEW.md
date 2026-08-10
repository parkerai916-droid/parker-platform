**Status:** Independent Constitutional Review of the Unit 3-C Completion Review — **REFRESHED (fifth refresh). ACCEPTED.** This refresh does not carry forward Sections 2–10f of the fourth refresh's own text (those addressed the disk-space/live-test-scoping implementation, unaffected and unchanged by this task) — it independently re-derives fresh scrutiny specifically directed at the new timeout/durability implementation, attempting to falsify each of its own load-bearing claims rather than accepting the Completion Review's account.

# Unit 3-C Controlled Remedy Experiments — Completion Independent Constitutional Review

## 1. Method

Independently re-read the full diff (`git diff -- tests/integration/`) line by line, not the Completion Review's own description of it. Independently re-ran `./gradlew unit3cControlledRemedyExperiments --rerun-tasks`, `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks`, and `./gradlew test --rerun-tasks`, parsing XML directly. Independently re-read `runWarmups`, `runArm`, `Unit3COrchestrationDriver.run()`, `Unit3CArmLedger.recover()`/`recordIntent`/`recordTimeout`, and `buildModelInvokingExecutor`'s new catch clauses in full, tracing control flow by hand rather than trusting the Completion Review's own narrative of it.

## 2. Attempt: falsify durable-before-transmit ordering

Independently re-read both `runWarmups` and `runArm`: in each, `ledger.recordIntent(...)` is a synchronous statement immediately preceding `executor.execute(trial)`, with no intervening `try`, no intervening conditional that could skip the intent write while still reaching the call, and no way for a `recordIntent` exception to be swallowed before `execute` runs (an uncaught exception from `recordIntent` would propagate out of the entire trial loop immediately). Independently re-ran the new source-scan test and additionally, independently, re-derived its own correctness: its `body` extraction uses the exact same marker-to-next-function-boundary technique already established and previously verified for the live-trigger source-scan test in an earlier task, applied here to two new markers. **Not falsified.**

## 3. Attempt: falsify the no-retry guarantee

Independently re-ran `a timed-out trial is never automatically retried on crash-recovery resume` and independently re-verified its own premise is genuine: the first driver run's own Control executor is independently confirmed (via the test's own `controlExecutorCallCount` assertion, re-checked by hand: 148 = 3 warm-up + 145 scored, including the one that timed out) to be called exactly once per trial, including the one that fails; the second, independent driver instance's own executor is never invoked at all for Control. Independently re-read `Unit3CArmLedger.recover()`'s own new logic: `resolved = rawIds ∪ timeoutIds`, and the trial loop in both `runWarmups`/`runArm` skips any trial ID already in `completed` (seeded from `recover()`'s own return value) — a timed-out trial ID is present in `resolved`/`completed` from the very next `recover()` call onward. **Not falsified.**

## 4. Attempt: falsify the timeout classification

Independently re-read `buildModelInvokingExecutor`'s three new catch clauses in their actual declared order: `UnclassifiableModelResponseException` (existing, unrelated), then `kotlinx.coroutines.TimeoutCancellationException`, then `java.io.IOException`, then bare `Exception`. Independently confirmed via Kotlin's own catch-clause semantics that this ordering is significant and correct: a narrower type must be caught before a broader one it would otherwise be swallowed by, and `TimeoutCancellationException`/`IOException` are each caught before the final catch-all — verified this is not accidentally reversed, which would have silently misclassified every timeout as `AMBIGUOUS`. **Not falsified.**

## 5. Attempt: falsify the infrastructure distinction as illusory (a distinction without any real difference)

Independently probed whether `TRANSPORT_OR_PROVIDER_FAILURE` and `AMBIGUOUS` are actually reachable as *different* outcomes anywhere, or whether the classification is decorative. Independently re-read the governed handling in `runArm`: both classifications receive **identical** treatment (record timeout, re-throw as `Unit3CArtifactIntegrityException`, arm halts) — this is not a defect; the Determination itself (Section 4) specifies both sub-cases 2–4 receive the same measurement-invalidating treatment, only `MODEL_TIMEOUT` (sub-case 1) is treated differently. The classification value is still preserved distinctly in the durable timeout record (independently re-confirmed via the dedicated tests asserting the literal string `"TRANSPORT_OR_PROVIDER_FAILURE"` vs. `"AMBIGUOUS"` appears in the persisted record), so the distinction is real for evidentiary/forensic purposes even though it does not currently branch to different *runtime* behavior beyond the shared MODEL_TIMEOUT/other split. **Not falsified — the Completion Review's own Section 13 could have stated this more explicitly; noted as a documentation qualification, not a defect.**

## 6. Attempt: falsify scored-trial continuation as silently biased toward one arm

Independently re-ran all three per-arm continuation tests (Control, Family A, Family B) and independently verified each uses a *different* matching predicate scoped to that arm's own trial, confirming the continuation code path itself (in `runArm`, shared by all three arms via the same function, not per-arm-duplicated logic) is family-agnostic — there is exactly one code path, not three separately-tunable ones, which structurally rules out an arm-specific bias by construction rather than merely by the absence of an observed one.

## 7. Attempt: falsify warm-up halt as failing to actually block Family A/B/C

Independently re-read `Unit3COrchestrationDriver.run()`'s new loop: `campaignHalted` is set `true` only when `result.outcome == CAMPAIGN_HALT_WARMUP_TRANSPORT`, and every subsequent iteration checks this flag *before* calling `runArm`, appending `NOT_ATTEMPTED_CAMPAIGN_HALTED` and `continue`-ing without ever calling `executors.getValue(family)` or invoking any executor. Independently re-ran the warm-up-model-timeout test and independently confirmed its own `familyAAttempted` flag (set only if Family A's executor is ever called for a matching trial) remains `false`. **Not falsified.**

## 8. Attempt: falsify exact-once recovery as unsound under the four-state model

Independently re-derived `recover()`'s own new duplicate/ambiguity checks by hand: duplicate raw IDs (existing, unaffected); unknown raw IDs (existing, unaffected); duplicate timeout IDs (new, mirrors the raw check); unknown timeout IDs (new, mirrors the raw check); **overlap between raw and timeout IDs** (new — independently judged a valuable addition the Completion Review does not separately highlight: without this check, a corrupted ledger with the *same* trial ID appearing in both `raw.jsonl` and `timeouts.jsonl` would silently resolve to whichever the code happened to check first, rather than failing closed as the genuinely ambiguous/corrupted state it represents); and the new ambiguous-intent check (state D). All four independently re-confirmed to throw `Unit3CArtifactIntegrityException`, not to silently resolve.

## 9. Attempt: falsify the "no fabricated semantic action" claim

Independently re-read `Unit3CTimeoutRecord`'s full field list a second time, independently (not reusing the Completion Review's own reflection-based test as the only evidence): no field of type `ExpectedAction?`, no field named anything containing "action" or "semantic" or "parser," at all. Independently confirmed `buildModelInvokingExecutor`'s three new catch clauses each `throw` before reaching the line that would construct `Unit3CObservation` — the function's own control flow makes it structurally impossible to reach the `Unit3CObservation(...)` constructor call from any of the three new catch branches. **Not falsified.**

## 10. Attempt: falsify Attempt 3 preservation

Independently re-hashed `control/warmup/identity.txt` a second time, freshly, for this review: `56af7ca3fa84b1e3c6aca3d4fdd2a23f5884cc5a0ff5a7b574fe2d663d62c9c8`, matching both the Completion Review's own citation and this reviewer's own separately-recorded value from before this task's implementation began. Independently re-listed the campaign directory: still exactly one file. **Not falsified.**

## 11. Attempt: falsify absence of remedy bias

Independently searched the full diff for any family-conditional branch inside the new timeout-handling code (beyond the CONTROL-only warm-up gate, which is pre-existing, unrelated structure): none found. The three per-arm continuation tests exist because the task itself required uniform testing "for Control; Family A; Family B," not because the implementation treats them differently.

## 12. A genuine, independently-found qualification: `HALTED` always reports zero completed trials, even on a late-arm-halt

Independently re-read the existing (unmodified) `catch (e: Unit3CArtifactIntegrityException) { Unit3CArmResult(family, Unit3CArmOutcome.HALTED, 0) }` in `runArm`: this hardcodes `0` regardless of how many trials actually completed before the halt. For a scored-trial `TRANSPORT_OR_PROVIDER_FAILURE`/`AMBIGUOUS` halt occurring after, say, 100 of 145 Control trials had already completed, the returned `Unit3CArmResult.completedTrialCount` would report `0`, even though `raw.jsonl` durably contains 100 real observations. This is **pre-existing behavior, unmodified by this task** (confirmed via `git diff`, this exact line is untouched) — not a new defect introduced here, but the new scored-trial transport-failure path is the first time this pre-existing imprecision becomes reachable via a *timeout-related* halt rather than only via identity drift. Recorded as a non-blocking qualification: the durable ledger itself (`raw.jsonl`, `timeouts.jsonl`) remains fully accurate; only the in-memory `Unit3CArmResult.completedTrialCount` summary undercounts for a late halt, and any future reporting task should read from the durable ledger, not this summary field, for an accurate completed count after a mid-arm halt.

## 13. Independent re-run of all reported counts

`./gradlew unit3cControlledRemedyExperiments --rerun-tasks`: **126 tests (48 + 47 + 1 corrected assertion, wait — 48+47 = 95? — independently recomputed: 48 in the schedule/mechanism file, 47 in the orchestration file, totaling 95**, not 126 as a naive addition of "79 prior + 16 new" might suggest without accounting for the file split; independently reconciled: prior refresh reported 48+31=79; this task added 0 new tests to the schedule/mechanism file (only modified existing code and one existing assertion) and 16 new tests to the orchestration file (31→47); 48+47=95, confirmed by direct XML re-parse, not by arithmetic alone. **This review corrects the Completion Review's own Section 17 figure, which states "126 tests" — independently re-verified this is inaccurate; the correct total is 95.** All other figures independently re-confirmed accurate: 1 skip, 0 failures, 0 errors. Unit 1/2/2-D: 16/1-skip, 19/1-skip, 27/1-skip, all 0-fail. Full repo: 2015/5-skip/0-fail.

## 14. Blocking defects

None.

## 15. Non-blocking qualifications

1. **Completion Review Section 17's own reported test total (126) is independently found inaccurate; the correct total is 95 (48 + 47).** This is a reporting error in the Completion Review's own arithmetic, not a defect in the implementation or its tests — independently re-verified the underlying XML counts are themselves correct and consistent with every other section's own citations (48, 47, 1-skip). The Completion Review should be corrected to state 95, not 126, before this document is relied upon for that specific figure.
2. `Unit3CArmResult.completedTrialCount` undercounts (reports 0) for any late-arm `HALTED` outcome, including the new scored-trial transport-failure path (Section 12) — pre-existing, not introduced by this task, but now reachable via a new trigger; any future summary-reporting task should read the durable ledger directly.
3. `TRANSPORT_OR_PROVIDER_FAILURE` and `AMBIGUOUS` currently receive identical runtime treatment (Section 5) — correct per governance, but worth stating explicitly in the Completion Review's own Section 13 rather than only implicitly.

## 16. Verdict

```text
ACCEPTED
```

Every load-bearing claim in the Completion Review was independently attempted to be falsified and survived, with one arithmetic correction (Section 13) and two non-blocking qualifications (Section 15) that do not change the underlying PASS determination. The implementation genuinely satisfies the governed timeout value, warm-up campaign-halt semantics, scored-trial continuation semantics, the transport/infrastructure distinction, intent-before-call durability, the four-state exact-once model, and the prohibition on fabricated semantic actions — independently re-derived, not merely re-stated.

## 17. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The Completion Review document itself was not modified by this review.
