**Status:** Unit 3-C Controlled Remedy Experiments — Completion Review — **REFRESHED. PASS.** Implementation only, against committed baseline `08f3692` (the frozen Plan, corrected). No live model call, no HTTP call, no live campaign directory, no production (`src/`) change, no remedy selected. This refresh supersedes the prior Completion Review in place: it reflects the corrected Family C trace and the four previously-open readiness gaps (orchestration driver, artifact-root hard restriction, disk-space check, manual safety-review checkpoint), all now closed.

# Unit 3-C Controlled Remedy Experiments — Completion Review

## 1. Files changed

- **Modified:** `build.gradle.kts` — the detached, opt-in `unit3cControlledRemedyExperiments` `Test` task now filters to both Unit 3-C test classes.
- **Modified:** `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` — `Unit3CArmLedger.checkIdentity` now creates its directory before writing (a real bug found and fixed during this task's own verification, described in Section 21); `Unit3CLiveEntryPoint` is now fully wired to the real orchestration driver with real, live-calling executors for Control/Family A/Family B and the offline executor for Family C, reachable only after every gate (configuration, artifact-root, disk-space) passes.
- **Created:** `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` (new file) — the orchestration driver, the artifact-root hard-restriction policy, the disk-space gate, and the manual false-positive safety-review checkpoint, plus 25 offline tests.
- **Not touched:** any `src/**` file; any existing, previously-frozen `tests/integration/**` file other than the one already-owned Unit 3-C file named above; any fixture definition anywhere; any campaign artifact directory. The frozen Plan itself (already corrected and committed at `08f3692`, prior to this task) was not touched by this task.

## 2. Boundary Review status

**Not required**, re-confirmed independently for the new code exactly as for the original: the orchestration driver, artifact-root policy, disk-space gate, and safety checkpoint are all test-tier code; the driver's only production-class touchpoints are through the same, already-frozen `ReasoningPromptBuilder`/`ModelReasoningProvider`/`TaggedReasoningResponseParser`/`LocalHttpModelInferenceClient`/`DefaultReasoningPromptBuilder` interfaces the original implementation already used, invoked exactly as production already invokes them, never modified.

## 3. Corrected Family C trace

The implementation was independently re-verified to already conform to the frozen, corrected Plan (`08f3692`): the test suite's expected-value table (`p12-injection` → `REMEMBER_SIGNAL`, `assertEquals(24, correct)`) was written during the prior implementation task, before the Plan itself was corrected, and is now confirmed to match the corrected Plan's own Section 9 table exactly — no reconciliation was needed, and none was performed. **Final result: 24 of 29 correct, four predicted false positives (`P03`, `P04`, `P05`, `P12`), one predicted false negative (`R03`)**, unchanged from the prior Completion Review and matching the frozen Plan exactly.

## 4. Orchestration-driver result

**Implemented and fully tested.** `Unit3COrchestrationDriver` owns the frozen four-arm schedule (Control → Family A → Family B → Family C, in that fixed order); enforces exact campaign and arm identities; derives trial lists from `Unit3CCampaignDefinition`; prevents duplicate execution via each arm's own `Unit3CArmLedger` (a sealed or in-progress arm never re-executes a completed trial, verified two ways — a `driver prevents duplicate execution` test and a direct re-append rejection against a sealed ledger); makes Family C's arm issue zero calls to anything resembling a model invocation (verified by constructing every Family C observation with all model-identity/prompt/response fields `null`); preserves arm-level fault isolation (an identity mismatch injected into one arm's directory halts only that arm; the other three still seal, verified directly); and records remedy-performance outcomes as evidence rather than stopping the arm, while measurement-invalidating defects (identity drift, corrupted ledger state) fail closed. `Unit3CLiveEntryPoint` now constructs this driver with real, live-calling executors for Control/Family A/Family B (via `ModelReasoningProvider`/`LocalHttpModelInferenceClient`/`TaggedReasoningResponseParser`, exactly as production already wires them) and the offline `Unit3CCandidateC1`-based executor for Family C — reachable only after `Unit3CConfigLoader`, `Unit3CArtifactRootPolicy`, and `Unit3CDiskSpaceGate` all pass, and invoked by no test anywhere in this codebase.

## 5. Artifact-root result

**Implemented and fully tested.** `Unit3CArtifactRootPolicy.resolve` hard-restricts the live artifact parent to exactly `/var/lib/parker/reasoning-protocol-live-model` — the same, already-governed durable root Unit 2 and Unit 2-D already use — reusing Unit 2-D's own established `acceptedArtifactParent`/`campaignArtifactRoot` pattern rather than inventing a new one, per this task's own explicit instruction. Nine dedicated tests independently prove rejection of: relative paths; repository-relative paths; `build/reports`; `src`; `tests`; `docs`; path traversal; an already campaign-qualified parent; the wrong parent entirely; and collision with either of the two preserved Unit 2/Unit 2-D campaign directories. No live campaign directory was created by any test — every test operates on path strings and, where a real directory is needed, a `@TempDir`.

## 6. Disk-space result

**Implemented and fully tested.** `Unit3CDiskSpaceGate.check` enforces a 2 GiB minimum before any write, reusing Unit 2-D's own already-governed `DIAGNOSTIC_MINIMUM_FREE_BYTES` value — the frozen Plan states neither a number nor a derivation, so this reuses the immediately analogous predecessor unit's already-accepted precedent rather than inventing a new threshold, exactly per this task's own instruction. Four dedicated tests independently prove: sufficient capacity passes; insufficient capacity fails closed; an unreadable filesystem state (a simulated `IOException`) fails closed rather than passing by default; and exact boundary equality (`available == minimum`) passes (`>=`, not `>`). The driver calls this gate before invoking any executor, verified directly — a test with an intentionally insufficient simulated capacity confirms zero executor invocations occur.

## 7. Safety-checkpoint result

**Implemented and fully tested.** The trigger is defined exactly as the frozen Plan requires: non-numeric, first-occurrence, on any trial whose fixture category is `ADVERSARIAL`, `REPLY`, or `GOAL` (i.e., a negative-control fixture) where the actual action is a false-positive `REMEMBER` or `GOAL`. When triggered: the triggering observation is preserved (already durably appended to the ledger before the check runs); the arm halts immediately, issuing no further trials; a durable `SAFETY_CHECKPOINT` marker file records the triggering trial ID and reason; sealing is structurally impossible while the marker exists (verified directly); and no automatic clearing path exists anywhere in the codebase — `clearForExplicitlyAuthorizedContinuation()` exists as its own, separate, explicitly-named method, never called by the driver itself. The checkpoint does not delete, rerun, adjust, or repair anything, verified by inspection: no such operation appears anywhere in `Unit3CSafetyCheckpoint` or in the driver's own handling of a triggered checkpoint.

## 8. Supplemental-fixture, Family A, Family B, repetition, model/config results

Unchanged from the prior Completion Review and re-confirmed by the fresh, full test run in this task: all six supplemental fixtures reproduce frozen text/action/category exactly; Family A's decision/rendering builders are unchanged and re-verified; Family B's candidate reproduces SHA-256 `cfd5cb7f07d0d7da941b069e00b3f479fc5faf5e71662a83bda51f25bd629d60` exactly; repetition is n=5 (Control/A/B) and n=1 (Family C), unchanged; model strategy (`qwen2.5-coder:7b` only) and inference configuration (no sampling parameters, 30,000 ms timeout) are unchanged.

## 9. Exact 483-call derivation from the executable schedule

**483, independently derived a third and fourth way in this task, beyond the two already established:** `exact 483-call total is derivable from the orchestration schedule, not merely the campaign definition object` runs the actual driver against fake, call-counting executors for Control/Family A/Family B (which increment a counter) and a non-counting executor for Family C, and confirms the counter reaches exactly 480 for the three model-invoking arms, which plus the 3 already-independently-verified warm-up calls equals 483. Separately, `driver executes all four arms in frozen order and seals each on success` confirms each arm's own completed-trial count (145, 220, 115, 29) against the frozen table. No hidden retries (duplicate-prevention tests), no double execution (sealed-ledger rejection test), no omitted cell (per-arm counts match exactly), no Family C model call (verified structurally), no extra warm-up, and no accidental call during artifact validation or safety-checkpoint handling (the disk-space-failure test proves zero executor calls occur before that gate passes; the safety-checkpoint test proves the arm halts, issuing no further calls, immediately upon the triggering observation).

## 10. Artifact/exact-once, downstream-isolation, and Gradle lifecycle-isolation results

Unchanged in substance from the prior Completion Review (all thirteen original ledger tests still pass, plus the `checkIdentity` fix described in Section 21); re-verified fresh. Downstream isolation re-confirmed absolute in both files. Gradle lifecycle isolation re-confirmed: `./gradlew :test :check --dry-run` shows `unit3cControlledRemedyExperiments` in neither dependency graph, and the `liveModelEvaluation` source set remains structurally detached.

## 11. Targeted tests

**65 tests, 0 failures, 0 errors, 0 skipped** (`./gradlew unit3cControlledRemedyExperiments`) — 40 in the original file, 25 in the new orchestration file. Three real defects were found and fixed during this task's own offline verification (Section 21).

## 12. Unit 1/2/2-D regression results

Run together via the unfiltered `reasoningProtocolLiveModelEvaluation` task: `ReasoningProtocolLiveModelConformanceTest` (Unit 1) 16/1-skip/0-fail; `ReasoningProtocolBaselineCharacterisationTest` (Unit 2) 19/1-skip/0-fail; `ReasoningProtocolDiagnosticCharacterisationTest` (Unit 2-D) 27/1-skip/0-fail. Zero regression.

## 13. Full ordinary repository test result

`./gradlew test`: 2015 tests, 5 skipped (pre-existing), 0 failures, 0 errors.

## 14. Live model/HTTP calls made

**Zero.** No live environment variable was set at any point during this task.

## 15. Campaign directories created

**None.** `/var/lib/parker/reasoning-protocol-live-model/` contains only the two pre-existing Unit 2/Unit 2-D directories.

## 16. Blocking defects

**None.**

## 17. Defects found and fixed during this task's own offline verification

1. **`Unit3CArmLedger.checkIdentity` missing directory creation:** the method wrote directly to `identityFile` without first calling `Files.createDirectories(directory)`, causing a `NoSuchFileException` on any arm's first identity check in a fresh directory. Fixed by adding the missing `Files.createDirectories(directory)` call at the method's start. Independently re-verified fixed by the full, passing test run.
2. **Gradle task filter omitted the new test class:** the `unit3cControlledRemedyExperiments` task's filter initially matched only the original test class name; the new orchestration test class was silently excluded from the opt-in task (though never from ordinary `test`, which cannot reach it regardless). Fixed by adding a second `includeTestsMatching` pattern.
3. **Self-referential isolation-check test:** the new orchestration file's own "constructs no real inference client" test initially searched its own source text for the literal forbidden constructor names, which matched its own denylist literal — the same class of bug Unit 2-D's own equivalent check once required fixing. Fixed by building the searched-for strings via string concatenation so the literal substrings never appear verbatim in the test's own source.

None of these three reflects a defect in the frozen governance (Scope Lock, Plan, or either trace-defect review) — all three are implementation-level bugs, found and fixed entirely within this task's own offline verification, before any live call was ever contemplated.

## 18. Completion verdict

```text
PASS
```

Ready for Independent Constitutional Review of Completion.
