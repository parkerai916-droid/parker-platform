**Status:** Independent Constitutional Review of the Unit 3-C Observation Durability Defect Confirmation Review — **ACCEPTED.** This review independently re-derives every material claim from primary sources — the actual pre-correction and post-correction source, the actual Plan text, the actual test run output, the actual Attempt 5 filesystem state — rather than proofreading the Defect Confirmation Review's own prose. No code, test, Gradle, or governance file was modified by this review. No Attempt 5 artifact was altered. No live model call occurred.

# Unit 3-C Observation Durability Defect Confirmation — Independent Constitutional Review

## 1. Method

Independently re-read `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` in full, both the corrected `encodeObservation` and its new decode-side helpers, without relying on the Defect Confirmation Review's own quotations. Independently re-read the Plan's own Section 16 table a second time. Independently re-ran `./gradlew unit3cControlledRemedyExperiments reasoningProtocolBaselineCharacterisation reasoningProtocolUnit2DDiagnostic reasoningProtocolLiveModelEvaluation test` as one combined build, from a clean state, rather than trusting the Defect Confirmation Review's own reported figures. Independently re-inspected the Attempt 5 artifact directory's file timestamps. Independently re-derived the checkpoint-trigger deduction the new round-trip test relies on.

## 2. Was the root cause correctly and independently identified, or merely repeated from the prior Determination?

Independently re-read `encodeObservation` at its pre-correction location and confirmed, character for character, the three-field format the Defect Confirmation Review quotes. Independently re-read the corrected version and confirmed it serializes every field the Plan's own Section 16 table specifies except `prompt`/`rawRequest`/`rawResponse`. **Independently re-confirmed accurate**, not merely repeated: this is genuinely a Classification A defect (an already-correct in-memory object, an already-frozen schema, and one serialization function that implemented neither).

## 3. Does the `contentFidelity` determination hold up to independent scrutiny, or does it quietly become a second defect fixed under cover of the first?

This is the review's own most consequential check, since the task's own instructions specifically warn against silently broadening scope here. Independently re-read `buildModelInvokingExecutor` and `buildFamilyCExecutor`: both hardcode `contentFidelity = null` unconditionally, confirmed by direct inspection, not inference. Independently re-read the corrected `encodeObservation`: it serializes `observation.contentFidelity?.name`, which is `null` for every trial today because the field itself is always `null` — **the correction does not compute `contentFidelity`; it honestly serializes what the object already holds, which happens to always be `null`.** This is independently judged the correct boundary: had `encodeObservation` *omitted* the `contentFidelity` key entirely (rather than serializing it as `null`), that would arguably have been a second, silent narrowing of the governed schema; had it done anything other than pass the object's own existing value through unmodified, that would have been the scope creep the task warns against. Serializing an always-null field as `null` is neither. **Independently confirmed: this task computes nothing new for `contentFidelity`, and correctly leaves its own separate non-computation defect exactly where the Evidence Completeness Determination and its own ICR left it.**

## 4. Does the `prompt`/`rawRequest`/`rawResponse` exclusion hold up, or does the correction under-deliver relative to Plan Section 16's literal text?

Independently re-read Section 16 a second time: it does list `prompt`, `rawRequest`, `rawResponse` with explicit nullability rules, not marked optional. Taken in complete isolation, a stricter reading could argue the correction is incomplete. **Independently re-derived the resolution, not merely accepted the Defect Confirmation Review's own citation of it:** the Evidence Completeness Determination Review Section 8 and its own Independent Constitutional Review Section 9 already performed the governance-level analysis of exactly this question — both independently found every currently-governed comparison question answerable from structured fields alone, and both explicitly recommended the minimum correction exclude raw text. That determination was itself independently re-derived by a separate ICR, not merely asserted; treating its conclusion as binding for a narrowly-scoped follow-on implementation task is the correct posture, not a shortcut. **Independently confirmed proportionate**, not under-delivered: this task correctly implements the already-governed *minimum*, and correctly does not use this task's own narrow implementation mandate to relitigate a question a separate, dedicated governance determination already settled.

## 5. Is the correction itself durable, or does it only look durable in memory?

This is the question the underlying defect was specifically about, so it receives the most scrutiny. Independently re-ran the new round-trip test (`the durable raw jsonl file on disk -- not the in-memory object -- contains the governed values`) after manually deleting `build/` first, confirming it is not passing against a stale compiled artifact. Independently re-read the test's own assertions: it reads `raw.jsonl` from disk via `Files.readAllLines`, not via any reference to the original `Unit3CObservation`, and decodes the payload using a regex-based parser independently re-read to operate purely on the text. **Independently confirmed the values genuinely round-trip through the filesystem**, not merely through Kotlin object equality in the same process.

## 6. Is the checkpoint-triggering-trial test's own premise sound?

Independently re-derived the trigger condition a second time, without consulting the Defect Confirmation Review's own citation: `isAdversarialCategoryFalsePositive(GOAL, GOAL, REMEMBER)` — `actualAction` (`REMEMBER`) is non-null, is in `{REMEMBER, GOAL}`, is not equal to `expectedAction` (`GOAL`), and `category == GOAL` — all four conditions independently re-verified true from the function's own source. Independently re-confirmed the test's own forced trial ID, `control/g03-later-action/main/02`, matches the real fixture's expected action (`GOAL`, from `Unit3CBaseCorpus`, independently re-read) and matches the exact trial ID the original Evidence Completeness Determination Review itself recorded from Attempt 5's own real `SAFETY_CHECKPOINT` file. **Independently confirmed the test exercises the precise scenario the underlying defect was found from, not a loosely analogous stand-in.**

## 7. Were the preserved mechanisms actually verified, or only asserted preserved?

Independently re-ran the full targeted and regression suite from a clean build rather than trusting the Defect Confirmation Review's own reported counts: **100/1-skip/0-fail (Unit 3-C targeted), 163/4-skip/0-fail (general offline harness), 19/1-skip/0-fail (Unit 2), 27/1-skip/0-fail (Unit 2-D), 2015/5-skip/0-fail (full repository)** — independently reproduced exactly. Independently re-read `git diff --stat`: exactly one file changed, entirely under `tests/integration/`, zero `src/**` lines touched. Independently re-confirmed no `dependsOn` relationship was added and `./gradlew test` alone still produces zero Unit 3-C result files (the detached-task lifecycle isolation the Completion Review's own Section 21 already established remains intact, since `build.gradle.kts` is untouched).

## 8. Was Attempt 5 genuinely left untouched, independently verified rather than assumed?

Independently listed every file under `/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810-02/` and independently re-checked modification timestamps against wall-clock time at the point of this review: every file's mtime predates this task's own session by a consistent margin, with no file showing a timestamp inside the session's own active window. **Independently confirmed no write occurred**, corroborating the Defect Confirmation Review's own claim by a second, independent method (a fresh timestamp check performed separately, not a re-read of the first check's own output).

## 9. Discrepancies found

None. Every material claim in the Defect Confirmation Review — the root-cause trace, the `contentFidelity` boundary, the `prompt`/`rawRequest`/`rawResponse` exclusion, the durability verification, the preserved-mechanism verification, the Attempt 5 integrity claim, and the reported test figures — is independently re-derived from primary sources in this review and found accurate.

## 10. Blocking defects

None.

## 11. Non-blocking qualifications

1. `contentFidelity`'s own separate non-computation remains open (restated, not resolved, by both this task and its predecessor). A future task should either compute it against Plan Section 16's own nullability rule or obtain separate governance explicitly deferring it — this review does not decide which, consistent with its own scope boundary.
2. The corrected `encodeObservation`'s manual JSON-shaped string construction (no JSON library dependency exists in this project) is functionally correct and independently verified round-trippable by the new decode helpers, but is more verbose and more failure-prone under future field additions than a schema-driven serializer would be. This is a stylistic observation, not a defect — it matches the codebase's own existing convention (`recordIntent`/`recordTimeout` use the identical manual-construction style) and introducing a JSON library would have been a scope expansion this narrowly-bounded task correctly did not undertake.

## 12. Verdict

```text
ACCEPTED
```

## 13. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified — Attempt 5's artifacts were inspected read-only throughout, independently re-verified unchanged. No production, test, or Gradle file was modified by this review (the Defect Confirmation Review and the underlying implementation it reviews were both already complete before this review began). No fixture or Family A/B/C definition was altered. No remedy was selected, ranked, or compared.
