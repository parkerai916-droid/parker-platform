**Status:** Unit 3-C Controlled Remedy Experiments — Observation Durability Defect Confirmation Review — **CONFIRMED. CORRECTED.** Narrowly-scoped implementation correction against committed baseline `4430e8b7eeb281d936ca3266fff13df7ed6bcc87` (the Unit 3-C Evidence Completeness and Durability Determination's own completeness classification, committed). Corrects `encodeObservation` so that the fields the accepted Plan Section 16 schema requires, and that `Unit3CObservation` already computes, survive into the durable `raw.jsonl` record. No live model call, no HTTP call, no live campaign directory created or mutated, no production (`src/`) change, no remedy selected, no Attempt 5 artifact touched.

# Unit 3-C Observation Durability Defect Confirmation Review

## 1. Baseline and scope

Drafted against `HEAD == origin/main == 4430e8b7eeb281d936ca3266fff13df7ed6bcc87`, independently re-confirmed clean at task start. Authority: the Unit 3-C Evidence Completeness and Durability Determination Review (`0a4823b`) and its Independent Constitutional Review (`ACCEPTED WITH QUALIFICATIONS`, `4430e8b`), both re-read fresh for this task, not assumed correct from their own summaries. This task performs only the narrow corrective implementation those two documents identified as the exact next governance step (Determination Review Section 13); it does not reopen any other Unit 3-C question.

## 2. Independent re-trace of the implementation path

Re-read, from source, not from either prior review's own quotations:

1. `buildModelInvokingExecutor` (`ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`) constructs a fully-populated `Unit3CObservation` for every completed Control/Family A/Family B trial: `actualAction`, `semanticCorrect`, `representationValid`, `parserResult`, `parserFailure`, `latencyNanos`, `transportOutcome`, `promptIdentity`, model/digest/endpoint/timeout/inference-config identity, `stableInputHash`, `repositoryCommit` — all independently re-confirmed correctly computed. `contentFidelity` is hardcoded `null` there, independent of whether a fixture defines `expectedContent` (Section 5 below). `rawRequest`/`rawResponse` are hardcoded `null` — never captured, at any point, in memory.
2. `buildFamilyCExecutor` constructs the equivalent object for Family C, with the model-call-only fields correctly `null` per `Unit3CObservation`'s own constructor invariant, and `candidateMechanismIdentity = "candidate-c1"`.
3. `runWarmups`/`runArm` (`ReasoningProtocolUnit3COrchestrationTest.kt`) call `ledger.appendObservation(trial.id, encodeObservation(observation))` for every completed trial. Prior to this task's correction, `encodeObservation` (a `private fun` member of `Unit3COrchestrationDriver`, line 686 of the pre-correction file) read: `"campaignId=${observation.campaignId}|family=${observation.family}|fixtureId=${observation.fixtureId}"` — independently re-confirmed character-for-character identical to both prior reviews' own quotation.
4. `Unit3CArmLedger.appendObservation` wraps that string, unmodified, as the `payload` field of a `{"trialId":...,"payload":...}` JSON line appended to `raw.jsonl`.

**Independently re-confirmed: three of ~26 governed, already-computed fields survived to the durable record; every other field the object already held — `actualAction`, `semanticCorrect`, `representationValid`, `contentFidelity` (always null, see Section 5), model/digest/endpoint/timeout/inference-config identity, `promptIdentity`, `parserResult`, `parserFailure`, `latencyNanos`, `transportOutcome`, `candidateMechanismIdentity`, `stableInputHash`, `repositoryCommit` — was discarded at exactly this step.**

## 3. Plan requirement, re-read directly

The accepted Unit 3-C Implementation/Execution Plan Section 16 (Artifact schema, Phase 17) specifies a full field table for the completed-observation record with explicit per-field nullability, independently re-read in full for this task (not paraphrased from the Determination Review). None of the Timeout + Durability Scope Lock Amendment, the Timeout + Durability Implementation Plan Amendment, or the Scored-Trial Timeout Semantics Determination amends this table — each of those documents adds *new, additive* schemas (the intent record, the terminal-timeout record) and independently re-confirmed leaves the original completed-observation table untouched. Section 16 is therefore controlling, unamended, frozen governance.

## 4. Root cause

`encodeObservation` is, and before this correction was, the single function responsible for turning an already-correct in-memory `Unit3CObservation` into the durable `raw.jsonl` payload. It implemented a three-field ad hoc format instead of the governed schema. This is a **Classification A defect — an implementation defect against already-frozen governance**, independently re-derived here rather than accepted from the Determination Review's own classification: Section 16's schema was correctly and specifically frozen from the start; `Unit3CObservation` correctly implements it in memory; the gap is confined entirely to the one serialization step. It is not a governance gap (Classification C/D) and not an inherent evidence limitation (Classification F) — the missing values were already computed and simply never written down.

## 5. `contentFidelity` — determination

Independently re-examined, per this task's own instruction not to silently broaden scope. `contentFidelity` is hardcoded `null` in both `buildModelInvokingExecutor` and `buildFamilyCExecutor`, **unconditionally** — including for fixtures that define a non-null `expectedContent` (e.g. Family A's rendering-step fixtures, per the Plan's own Section 7). This is a **separate, pre-existing non-computation defect**, not the durability defect this task corrects: even if `encodeObservation` had always persisted every field, `contentFidelity` would still have serialized as `null` for every trial, because the value is never computed, not merely never persisted. The Evidence Completeness Determination's own Section 7 classified this as "NOT REQUIRED FOR THE GOVERNED QUESTION, AS CURRENTLY SCOPED," and its Independent Constitutional Review (Section 14, qualification 1) flagged it as a distinct, correctly-scoped-out question a future task should not lose track of, without resolving it. **This task does not compute `contentFidelity`.** The corrected `encodeObservation` serializes the field exactly as `Unit3CObservation` already holds it — `null`, for every trial, today — which is an honest, non-fabricated durable record of the object's actual current state, not a fix to the non-computation gap. That gap remains open, unresolved, and out of this task's scope, exactly as both prior reviews left it.

## 6. `prompt`/`rawRequest`/`rawResponse` — determination

Independently re-confirmed against the Determination Review's own Section 8 minimum-evidence analysis and its Independent Constitutional Review Section 9 (which independently re-derived, not merely accepted, the same conclusion): every currently-governed Unit 3-A/Unit 3-C/Unit 3-D question (semantic correctness, representation validity, false-positive/negative tracking, completion/timeout rate) is answerable from the structured fields alone. `rawRequest`/`rawResponse` are not computed anywhere in memory today (hardcoded `null` even before serialization) — capturing them would require changing `classifyResponse`'s own narrowing behavior or `buildModelInvokingExecutor`'s response handling, a separate, larger change with no currently-governed requirement forcing it now. `prompt` is computed transiently but its own persistence is, per the same analysis, forensically useful but not required by any governed question. **This task excludes `prompt`, `rawRequest`, and `rawResponse` from the corrected durable schema**, matching the accepted minimum-evidence determination exactly, and does not expand the artifact schema beyond that existing authority.

## 7. Correction implemented

`encodeObservation` (relocated from a private member of `Unit3COrchestrationDriver` to a top-level function in the same file, so it can be shared with new decode-side test helpers without breaking encapsulation) now serializes every field enumerated in Section 2 above except `prompt`/`rawRequest`/`rawResponse` (Section 6), as a JSON-object-shaped string embedded in the existing `payload` field — preserving `Unit3CArmLedger.appendObservation`'s own existing outer format, `trialId` extraction regex, and every existing exact-once/duplicate-prevention/crash-recovery mechanism completely unchanged. No field is fabricated: every value written is exactly the value `Unit3CObservation`'s own constructor already computed; a `null` in the object serializes as literal `null`, never a guessed or default value.

## 8. Files changed

- `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` only. `encodeObservation` corrected and moved to top level; new private helpers `jsonQuote`/`jsonUnquote`/`jsonStringField`/`jsonBooleanField`/`jsonLongField`/`payloadField`/`decodeObservationPayload`/`extractRawPayload` added (decode-side helpers exist to prove durability by test, not to affect production encoding); five new tests added proving round-trip durability (Section 10 below).
- **Not touched:** `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`; any other test file; `build.gradle.kts`; any `src/**` file; any governance document (this review and its own Independent Constitutional Review are new documents, not amendments to existing frozen text); any campaign artifact directory.

## 9. Boundary Review

**NOT REQUIRED.** `git diff --stat` independently re-confirmed to touch exactly one file, entirely under `tests/integration/`. No production interface, class, or behavior is touched. The existing test-tier Unit 3-C machinery (`Unit3CObservation`, `Unit3CArmLedger.appendObservation`) was already fully sufficient to carry the correction; nothing required implementing outside it.

## 10. Durability verification

Five new tests, all offline, all passing on first run after correction:

1. `a completed observation containing every governed field survives encode-decode round trip` — direct encode→decode, no filesystem, asserting every field in Section 2's list.
2. `the durable raw jsonl file on disk -- not the in-memory object -- contains the governed values` — writes via `Unit3CArmLedger.appendObservation`, reads the line back from disk, decodes, and asserts values, plus a regression guard that the pre-fix three-field pipe format no longer appears.
3. `a representation failure preserves null actualAction and semanticCorrect, plus the real parserFailure message, through durable encoding` — proves nullability rules (Section 16's own `semanticCorrect` null-iff-`actualAction`-null rule) survive, and that no semantic value is fabricated for an unclassifiable response.
4. `Family C's null model-call fields durably survive encoding exactly as the schema requires` — proves the Family-C-only nullability branch of the schema, and that Family C's `actualAction` (previously only "partially recoverable by deduction," per the Evidence Completeness Determination) is now durably, directly readable.
5. `a full campaign run durably preserves actualAction for every trial, including the one that triggers the safety checkpoint` — runs the real `Unit3COrchestrationDriver` end to end against a fake executor forcing the exact checkpoint-triggering scenario the Determination Review's own Section 6 could previously only narrow by inference (`control/g03-later-action/main/02`), and independently confirms the real `actualAction` (`REMEMBER`) is now present verbatim in the durable `raw.jsonl` line on disk.

## 11. Preserved-mechanism verification

Independently re-verified, each via the full existing Unit 3-C test suite passing unchanged plus targeted re-reading:

- **Intent-before-call durability:** `recordIntent`/`buildIntentRecord` untouched; the existing source-order test (`intent is durable before the executor is ever invoked`) still passes unmodified.
- **Terminal timeout durability:** `recordTimeout`/`buildTimeoutRecord`/`Unit3CTimeoutRecord` untouched; this correction touches only the *completed-observation* encoding path, never the timeout-record path, which was already correct (Evidence Completeness Determination Section 10: "the timeout durability mechanism ... performed exactly as governed").
- **Four-state exact-once semantics:** `Unit3CArmLedger.recover()` untouched; the checkpoint-timing and duplicate-prevention logic in `appendObservation` untouched — only the *content* of the string passed into it changed, never its call contract.
- **Warm-up timeout semantics, scored-trial timeout semantics, transport/infrastructure distinction, no-automatic-retry:** none of `runWarmups`/`runArm`'s own control flow was touched; only the argument to `encodeObservation(observation)` calls (now resolving to the corrected top-level function) changed.
- **Safety-checkpoint behavior:** `isAdversarialCategoryFalsePositive`'s call site is untouched, still evaluated against the real, complete `observation` object before encoding occurs; Section 10 test 5 above additionally proves the checkpoint-triggering trial's own durable record is now correct, not merely that the checkpoint still fires.
- **Family A/B/C mechanisms and the frozen 483-call schedule:** neither file defining them (`Unit3CCampaignDefinition`, the Family A/B/C prompt builders, `Unit3CCandidateC1`) appears in this task's diff at all; the existing `483-call schedule and arithmetic remain unaffected` test still passes unchanged.
- **Downstream isolation:** no Memory/Goal/Planner/Knowledge-Submission symbol is referenced anywhere in the new code; the existing static-import test is unaffected.

## 12. Attempt 5 integrity

Independently re-confirmed via filesystem inspection: every file under `/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810-02/` (26 files) carries a modification time that predates this task's own session start by roughly two hours, and no command issued during this task wrote to any path under `/var/lib/parker/reasoning-protocol-live-model`. No Attempt 5 artifact was read-write opened, migrated, or reinterpreted by any code change — the correction applies only to code executed by a *future* campaign; it has no retroactive effect on any historical artifact, and none was attempted (per this task's own explicit instruction not to retrofit historical evidence).

## 13. Targeted and regression test results

- Targeted (`./gradlew unit3cControlledRemedyExperiments`): **100 tests, 1 skipped, 0 failures, 0 errors** (48 in the schedule/mechanism file, unchanged; 52 in the orchestration file, 47 prior + 5 new).
- Unit 1/general offline harness (`./gradlew reasoningProtocolLiveModelEvaluation`): **163 tests, 4 skipped, 0 failures, 0 errors.**
- Unit 2 (`./gradlew reasoningProtocolBaselineCharacterisation`): **19 tests, 1 skipped, 0 failures, 0 errors.**
- Unit 2-D (`./gradlew reasoningProtocolUnit2DDiagnostic`): **27 tests, 1 skipped, 0 failures, 0 errors.**
- Full ordinary repository (`./gradlew clean test`): **2015 tests, 5 skipped, 0 failures, 0 errors.**

All five Gradle invocations ran in a single combined build with zero `PARKER_REASONING_UNIT3C_*`/live-evaluation environment variables set at any point; zero `/api/generate` calls were made; zero HTTP calls of any kind were made.

## 14. Blocking defects

**None,** for the narrow scope of this task (durable persistence of already-governed, already-computed fields). `contentFidelity`'s own separate non-computation (Section 5) remains open, correctly out of scope, and is restated here (not silently dropped) so it is not lost a second time.

## 15. Verdict

```text
CONFIRMED — CLASSIFICATION A DEFECT, INDEPENDENTLY RE-TRACED FROM SOURCE — CORRECTED — contentFidelity REMAINS A SEPARATE, UNRESOLVED, OUT-OF-SCOPE GAP
```
