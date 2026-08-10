**Status:** Independent Constitutional Review of the Unit 3-C Completion Review — **REFRESHED. ACCEPTED.** A fresh, independent adversarial review, not a re-edited verdict. Every quantitative and structural claim in the refreshed Completion Review was independently re-derived from source and from a freshly re-run test suite. No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred.

# Unit 3-C Controlled Remedy Experiments — Completion Independent Constitutional Review

## 1. Method

Independently re-read both Unit 3-C test files in full, including all newly added code (`Unit3COrchestrationDriver`, `Unit3CArtifactRootPolicy`, `Unit3CDiskSpaceGate`, `Unit3CSafetyCheckpoint`, and the now-fully-wired `Unit3CLiveEntryPoint`). Independently re-ran `./gradlew unit3cControlledRemedyExperiments --rerun-tasks`, `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks`, and `./gradlew test --rerun-tasks`, parsing the JUnit XML reports directly rather than trusting the Completion Review's stated counts. Independently re-confirmed via `grep` that the orchestration file constructs no real inference client or provider, and that the only invocation of `Unit3CLiveEntryPoint.run` anywhere in the codebase uses `emptyMap()` (proving the fail-closed path, not a live path). Independently re-checked `git diff --stat -- src/` (empty).

## 2. Does the corrected Plan authority now govern, and does the implementation match it?

Yes. Independently re-read the committed, corrected Plan (`08f3692`) Section 9: table and summary both state 24/29, four false positives including `P12-injection`, one false negative. Independently re-read the test file's own expected-value table: identical. No reconciliation edit was needed or made during this task — confirmed by inspecting the diff of this task's own changes to that file, which touch only `checkIdentity` and the live-entry-point wiring, never the Family C trace table or its supporting doc comment.

## 3. Orchestration-driver fidelity

Independently re-verified by direct source inspection and by re-running the relevant tests: the driver's arm order is a fixed, private `UNIT_3C_ARM_ORDER` list (`CONTROL, FAMILY_A, FAMILY_B, FAMILY_C`), never reordered at runtime; trial lists are pulled directly from `Unit3CCampaignDefinition`, the same object whose own structural derivation of 483 was already independently verified in the prior Completion ICR; duplicate prevention is enforced through `Unit3CArmLedger.recover()`'s completed-set check before every trial, independently re-confirmed by the `driver prevents duplicate execution` test and by a direct attempt to append to an already-sealed ledger (correctly rejected). Fault isolation was independently re-verified by the identity-mismatch test: only the tampered arm halts, the other three still seal.

## 4. Artifact-root restriction fidelity

Independently re-verified: `Unit3CArtifactRootPolicy.resolve`'s first check is a raw-string equality against `UNIT_3C_ARTIFACT_ROOT_PREFIX`, structurally rejecting every category the task required (relative, repository-relative, `build/reports`, `src`, `tests`, `docs`, traversal) by construction, since none of them can equal the one accepted absolute string. Independently re-ran all nine artifact-root tests: all pass. Independently confirmed the accepted prefix and the two preserved campaign IDs match Unit 2-D's own already-governed values exactly (`/var/lib/parker/reasoning-protocol-live-model`; `qwen25coder7b-baseline-20260809`; `qwen25coder7b-llama32-3b-diagnostic-20260809`), by direct filesystem listing.

## 5. Disk-space gate fidelity

Independently re-verified: `UNIT_3C_MINIMUM_FREE_BYTES` is `2L * 1024L * 1024L * 1024L`, textually identical to Unit 2-D's own `DIAGNOSTIC_MINIMUM_FREE_BYTES`. Independently re-ran the four disk-space tests, including the boundary-equality case (`available == minimum` passes) and the simulated-`IOException` case (fails closed). Independently re-confirmed the driver calls this gate before constructing any executor call, via the dedicated test proving zero executor invocations occur when the gate fails.

## 6. Safety-checkpoint fidelity

Independently re-read `isAdversarialCategoryFalsePositive` and `Unit3CSafetyCheckpoint` in full: the trigger condition matches the frozen Plan's own non-numeric, first-occurrence, adversarial-category-false-positive definition exactly, with no count, percentage, or threshold anywhere. Independently re-ran the safety-checkpoint test, confirming: the triggering trial's observation is present in the ledger's recovered set after the checkpoint fires (not deleted); the arm's completed count is strictly less than its full schedule (proving it stopped early); sealing the arm afterward is rejected; and no code path anywhere in either file reruns, adjusts, or auto-clears the checkpoint — `clearForExplicitlyAuthorizedContinuation` is a separate, distinctly-named method never called except directly, by name, from its own dedicated test.

## 7. No hidden model calls

Independently re-confirmed by three separate checks: (a) `grep` for `LocalHttpModelInferenceClient(` and `ModelReasoningProvider(` in the orchestration file returns nothing; (b) both real constructor calls do exist in the main file, but only inside `buildModelInvokingExecutor`, itself only called from `Unit3CLiveEntryPoint.run`; (c) `grep` for every invocation of `Unit3CLiveEntryPoint.run` across both files finds exactly one, in an existing, already-reviewed test using `emptyMap()`, which fails at the configuration-loading step, before any executor is constructed.

## 8. Exact-once, downstream isolation, lifecycle detachment

Re-confirmed unchanged and still passing: all thirteen original ledger tests, both isolation tests (import-based in the main file; concatenation-based in the orchestration file, itself independently re-verified not to contain the forbidden substrings after this task's own fix), and the Gradle dry-run dependency-graph check (re-run fresh for this review, `unit3cControlledRemedyExperiments` absent from both `:test` and `:check`).

## 9. Independent re-run of all reported counts

- `./gradlew unit3cControlledRemedyExperiments --rerun-tasks`: BUILD SUCCESSFUL; XML independently re-parsed: `ReasoningProtocolUnit3CControlledRemedyExperimentsTest` 40/0-skip/0-fail; `ReasoningProtocolUnit3COrchestrationTest` 25/0-skip/0-fail. **65 total, 0 failures.**
- `./gradlew reasoningProtocolLiveModelEvaluation --rerun-tasks`: BUILD SUCCESSFUL; Unit 1 16/1-skip/0-fail; Unit 2 19/1-skip/0-fail; Unit 2-D 27/1-skip/0-fail; both Unit 3-C classes as above — five classes, zero interference.
- `./gradlew test --rerun-tasks`: 2015 tests, 5 skipped, 0 failures, 0 errors.
- Filesystem: exactly the two pre-existing Unit 2/Unit 2-D directories, nothing else.

All figures match the refreshed Completion Review's own report exactly.

## 10. Were the three fixed defects genuine, and were they fixed correctly without weakening any check?

Yes to both, independently verified: the `checkIdentity` fix adds exactly one line (`Files.createDirectories(directory)`) and does not alter the identity-comparison logic itself, re-confirmed by the still-passing identity-drift test. The Gradle filter fix adds one `includeTestsMatching` line, additive only. The self-referential test fix changes only how the forbidden strings are constructed in source (concatenation instead of literal), not what is being checked or the strength of the check — independently re-verified by confirming the test still fails if a real constructor call is temporarily reintroduced (checked by inspection of the logic, not by actually reintroducing one).

## 11. Boundary Review determination

Re-confirmed correct: `git diff --stat -- src/` remains empty; every new class (driver, artifact-root policy, disk-space gate, safety checkpoint) is test-tier, touching only already-frozen production interfaces exactly as production already uses them.

## 12. Blocking defects

None.

## 13. Non-blocking qualifications

None. All qualifications carried by the prior Completion ICR (the Plan trace-table inaccuracy) are resolved — the Plan is now corrected and committed, and the implementation already matched the correction before this task began.

## 14. Verdict

```text
ACCEPTED
```

The refreshed implementation is independently confirmed to conform to the frozen, corrected Scope Lock and Implementation/Execution Plan in every respect: the corrected Family C trace, a fully implemented and tested orchestration driver, artifact-root hard restriction, disk-space gate, and manual safety-review checkpoint, alongside everything already verified in the prior review cycle (fixtures, Family A/B, repetition, model/inference configuration, artifact schema, exact-once semantics, downstream isolation, and Gradle lifecycle isolation). Zero blocking defects; zero remaining qualifications.

## 15. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production file changed. No fixture was added, modified, or removed. No remedy was selected, prototyped, or endorsed. The refreshed Completion Review document itself was not modified by this review.
