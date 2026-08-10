**Status:** Independent Constitutional Review of the Unit 3-C Completion Review — **REFRESHED (sixth refresh). ACCEPTED.** Sections 1–17 below are preserved as the fifth refresh's own independent scrutiny of the timeout/durability implementation (still accurate for what it checked; not re-litigated here). Section 0 and Sections 18–24 are new to this refresh, independently attempting to falsify the sixth Completion Review refresh's own load-bearing claims about the observation-durability correction.

# Unit 3-C Controlled Remedy Experiments — Completion Independent Constitutional Review

## 0. Sixth refresh — scope and relationship to the fifth refresh

The fifth refresh (Sections 1–17) independently verified the timeout/durability implementation and, in its own Section 12, separately flagged (as a non-blocking qualification) that `Unit3CArmResult.completedTrialCount` undercounts on a late halt — unrelated to, and unaffected by, this sixth refresh. This refresh independently re-scrutinizes only what the Completion Review's own sixth-refresh Sections 0/25–31 newly claim: that `encodeObservation` now durably persists the governed fields it previously discarded, that `contentFidelity` was correctly left uncomputed rather than silently fixed, and that every previously-verified mechanism remains intact. This review was performed independently of, and did not simply adopt, the Observation Durability Defect Confirmation Review's own Independent Constitutional Review — both reviews were read fresh against primary sources, and their independent agreement is reported, not assumed, in Section 24 below.

## 1. Method (fifth refresh, preserved)

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

## 16. Verdict (fifth refresh)

```text
ACCEPTED
```

Every load-bearing claim in the Completion Review was independently attempted to be falsified and survived, with one arithmetic correction (Section 13) and two non-blocking qualifications (Section 15) that do not change the underlying PASS determination. The implementation genuinely satisfies the governed timeout value, warm-up campaign-halt semantics, scored-trial continuation semantics, the transport/infrastructure distinction, intent-before-call durability, the four-state exact-once model, and the prohibition on fabricated semantic actions — independently re-derived, not merely re-stated.

## 17. Confirmation (fifth refresh)

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The Completion Review document itself was not modified by this review.

## 18. Sixth refresh — attempt: falsify that the correction actually persists the governed fields, not merely that it looks correct on inspection

Independently re-read the corrected `encodeObservation` character by character, independently re-confirmed it references every field of `Unit3CObservation` except `prompt`/`rawRequest`/`rawResponse`. Independently re-ran the new round-trip test with `build/` deleted first (ruling out a stale compiled artifact silently passing), and independently constructed a second, separate ad hoc observation (different values from the test's own `richObservation()` fixture — a Family B trial, `semanticCorrect = false`, a non-null `parserFailure`) directly from a scratch script invoking `encodeObservation`/`decodeObservationPayload` via the Gradle test classpath, confirming the round trip holds for values the shipped test suite does not itself exercise. **Not falsified.**

## 19. Sixth refresh — attempt: falsify that `contentFidelity` was left alone rather than quietly computed

Specifically probed, since this is the sixth refresh's own most consequential claim. Independently re-read `buildModelInvokingExecutor` and `buildFamilyCExecutor` a second time: both still hardcode `contentFidelity = null`, unconditionally — this task's diff contains no edit to either function (independently re-confirmed via `git diff -- tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, which is empty). Independently re-read the corrected `encodeObservation`'s own `contentFidelity` line: `jsonStringField("contentFidelity", observation.contentFidelity?.name)` — this reads the object's existing field, computes nothing. **Independently confirmed: no computation was added; the always-`null` value now simply survives to disk instead of being silently dropped, which is not the same defect the Determination flagged as separate and unresolved.** Not falsified.

## 20. Sixth refresh — attempt: falsify that the checkpoint-triggering-trial test proves what it claims

Independently re-derived `isAdversarialCategoryFalsePositive(FixtureCategory.GOAL, ExpectedAction.GOAL, ExpectedAction.REMEMBER)` by hand from the unmodified function source: `actualAction` non-null, in `{REMEMBER, GOAL}`, unequal to `expectedAction`, category `GOAL` — all four conditions true, independently confirmed the checkpoint must fire. Independently re-ran the test with a debugger-equivalent manual trace (temporarily inserting a print of the decoded payload, then reverting the change — confirmed via `git diff` showing zero net change after reversion) and independently observed the decoded `actualAction` read `"REMEMBER"` directly from the `raw.jsonl` line on disk. **Not falsified.**

## 21. Sixth refresh — attempt: falsify that preserved mechanisms remain preserved, not merely re-asserted

Independently re-read `runWarmups`, `runArm`, `Unit3CArmLedger.recover()`/`recordIntent`/`recordTimeout`, and `isAdversarialCategoryFalsePositive`'s call site a second time in full, line by line, confirming none contains any edit — the only change touching those functions' own surrounding code is the two call sites' resolution of `encodeObservation(observation)` from a now-deleted private member to the corrected top-level function, which does not alter what argument is passed or when. **Not falsified.**

## 22. Sixth refresh — attempt: falsify the reported test counts

Independently re-ran `./gradlew clean test reasoningProtocolBaselineCharacterisation reasoningProtocolUnit2DDiagnostic reasoningProtocolLiveModelEvaluation unit3cControlledRemedyExperiments` as one combined build from a clean state and independently parsed the resulting XML with a fresh script (not reusing the Completion Review's own or the Defect Confirmation Review's own parsing): **test 2015/5-skip/0-fail; reasoningProtocolBaselineCharacterisation 19/1-skip/0-fail; reasoningProtocolUnit2DDiagnostic 27/1-skip/0-fail; reasoningProtocolLiveModelEvaluation 163/4-skip/0-fail; unit3cControlledRemedyExperiments 100/1-skip/0-fail (48+52 across the two files).** All independently reproduced exactly. **Not falsified.**

## 23. Sixth refresh — attempt: falsify Attempt 5 integrity

Independently listed `/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810-02/` (26 files) and independently computed each file's age relative to wall-clock time at the point of this review, confirming every file predates this task's session start by a materially larger margin than the session's own duration — ruling out even a same-session write followed by an immediate revert (which could in principle leave a matching final byte content but a fresh mtime). **Not falsified.**

## 24. Sixth refresh — cross-check against the Observation Durability Defect Confirmation Review's own Independent Constitutional Review

Read after, not before, completing Sections 18–23 above, specifically to check for independent agreement rather than to derive from it. Both reviews independently reached the same conclusions on every material point: the root cause, the `contentFidelity` boundary, the `prompt`/`rawRequest`/`rawResponse` exclusion, durability verification, preserved-mechanism verification, Attempt 5 integrity, and the reported test figures. No discrepancy found between the two independent reviews.

## 25. Blocking defects (sixth refresh)

None.

## 26. Non-blocking qualifications (sixth refresh)

`contentFidelity`'s own separate non-computation remains open (Section 19); this is inherited, not new, and is correctly out of both this task's and this review's own scope.

## 27. Verdict (sixth refresh)

```text
ACCEPTED
```

The observation-durability correction genuinely persists the governed fields, without quietly expanding scope to compute `contentFidelity` or capture raw prompt/response text, without weakening any previously-verified mechanism, and without touching Attempt 5. Independently re-derived, not merely re-stated — including one independent verification method (Section 18's separate ad hoc script) not present anywhere in the Completion Review's or the Defect Confirmation Review's own text.

## 28. Confirmation (sixth refresh)

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified — Attempt 5's artifacts were inspected read-only throughout. No production file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The Completion Review document itself was not modified by this review beyond what the Completion Review's own sixth refresh (Section 0/25–31 there) already states.
