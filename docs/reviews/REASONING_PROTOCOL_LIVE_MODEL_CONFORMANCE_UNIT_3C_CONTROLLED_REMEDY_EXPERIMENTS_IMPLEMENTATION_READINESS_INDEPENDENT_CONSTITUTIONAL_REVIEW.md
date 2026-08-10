**Status:** Independent Constitutional Review of the Unit 3-C Implementation Readiness Review — **REFRESHED (fifth refresh). ACCEPTED.** This review's specific, standing mandate — established after this programme repeatedly discovered "defined but never reached" defects (warm-ups counted but unrun; a live entry point nothing called; a disk-space gate checked against the wrong path) — is to never accept "READY" on the strength of passing tests alone. It independently re-traces whether the new timeout/durability code is actually reachable from the real entry point, not only exercised by tests that construct it directly.

# Unit 3-C Implementation Readiness Review — Independent Constitutional Review

## 1. Method

Independently re-read `Unit3CLiveEntryPoint.run`'s complete, current body directly from source (not from the Readiness Review's own description), tracing every line for what it constructs and calls. Independently re-read `Unit3COrchestrationDriver.run()`, `runWarmups`, and `runArm` in full. Independently re-ran the full offline suite fresh, immediately before drafting this document.

## 2. Is the new timeout-handling code actually reachable from the real entry point, or only from tests that bypass it?

**Reachable, independently re-traced, not assumed from the Readiness Review's own table.** `Unit3CLiveEntryPoint.run`'s own body, independently re-read line by line: constructs `Unit3COrchestrationDriver(config.campaignId, artifactRoot, config.identity)` — the exact same class this task's diff modified — and calls `driver.run(executors)`, where `executors` includes `buildModelInvokingExecutor(...)` for Control/Family A/Family B — the exact same function this task's diff extended with three new catch clauses. Neither the driver construction nor the executor construction was itself touched by this task; only the *internals* of `driver.run()` (the new sequential loop) and `buildModelInvokingExecutor` (the new catch clauses) changed. This means the new code is reached via the *same, unchanged, already-proven* wiring path — independently judged the strongest possible form of reachability evidence available without an actual live call, because it does not depend on any new wiring this review would need to separately verify for correctness.

## 3. Adversarial check: could the new code exist but never actually execute in the real (non-test) path, due to a subtle wiring gap the Readiness Review's own table didn't catch?

Specifically probed, since this is the exact failure category (code present, never reached) this programme has repeatedly discovered. Independently verified: `Unit3COrchestrationDriver.run()`'s new sequential loop **is** the function's entire body past the disk-space gate — there is no alternate, older code path left behind that the real entry point could still be calling instead (independently confirmed via `git diff` showing the old `UNIT_3C_ARM_ORDER.map { ... }` line was *replaced*, not left as dead code alongside a new one). Independently verified `runArm`'s new intent-recording and transport-exception-catching logic is inside the *same* trial loop the pre-existing, already-proven-reachable trial-execution logic uses — not a parallel, separately-invoked loop. **No wiring gap found.**

## 4. Does "READY" rest on any claim this review cannot independently confirm without a live call?

Independently re-checked Section 14 of the Readiness Review (the residual note) against its own wording: it explicitly limits its own claim to "every gate, every caller, and now every timeout-handling branch downstream of a failed call" being proven reachable — it does *not* claim the live HTTP call itself has been proven to succeed, or that Ollama's real timing behavior has been verified. Independently judged this claim boundary honest and internally consistent with what offline testing can and cannot prove.

## 5. Independent re-verification of the four new offline dimensions (intent, terminal, warm-up transport, scored-trial transport)

Independently re-ran the specific tests underlying each: `intent is durable before the executor is ever invoked -- source order proves no path can transmit first` (passes, independently re-read its own extraction logic to confirm it targets the correct function bodies); `a genuine warm-up model timeout halts the whole campaign, not merely the Control arm` (passes, independently re-confirmed `familyAAttempted` remains `false`); `a scored Control-trial model timeout terminates only that trial` (passes, independently re-confirmed 144 raw + 1 timeout = 145); `timeout record type has no field capable of holding a semantic action` (passes, independently re-confirmed via a second, manual read of `Unit3CTimeoutRecord`'s field list that no such field exists, not merely trusting the reflection-based test's own result).

## 6. Does this refresh repeat the earlier mistake of treating disk-space/live-trigger readiness as settled without re-checking?

Independently re-ran the pre-existing disk-space-gate and live-trigger tests fresh for this review (not merely citing their earlier-refresh pass): both still pass, unaffected by this task's diff, independently re-confirmed via `git diff` showing no hunk in either area.

## 7. Independent re-run of all reported counts

`./gradlew unit3cControlledRemedyExperiments --rerun-tasks`: **95 tests (48 + 47), 0 failures, 1 skipped** — independently re-counted from the XML directly, not from the Readiness Review's own citation, and independently cross-checked against the Completion ICR's own corrected figure (which found and corrected a 126-vs-95 arithmetic error in the Completion Review). `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks`: Unit 1 16/1-skip/0-fail, Unit 2 19/1-skip/0-fail, Unit 2-D 27/1-skip/0-fail, unchanged. `./gradlew test --rerun-tasks`: 2015/5-skip/0-fail, unchanged.

## 8. Blocking defects

None.

## 9. Non-blocking qualifications

Carried forward from the Completion ICR, since they bear directly on readiness: `Unit3CArmResult.completedTrialCount` undercounts (reports 0) for any late-arm halt, including the new scored-trial transport-failure path — pre-existing, not newly introduced, but now reachable via a new trigger; a future Approval Review or execution-evidence review must read the durable ledger directly for an accurate completed count after any halt, not this summary field.

## 10. Verdict

```text
ACCEPTED
```

The corrected implementation's readiness is independently confirmed genuine: the new timeout/durability code is reached via the same, unchanged, already-proven entry-point wiring, not a parallel or untested path; no wiring gap of the kind this programme has repeatedly discovered elsewhere was found here; and the residual note's own claim boundary is honest about what remains genuinely unverifiable without a live call. This instrument is ready to proceed to a fresh Explicit Execution Approval Review, which this task does not create.

## 11. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The Readiness Review document itself was not modified by this review.
