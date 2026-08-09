**Status:** Unit 2-D Implementation/Execution Plan — Correction and Independent Constitutional Re-Review — **ACCEPTED. READY TO FREEZE.** This document does not erase, replace, or supersede the original adverse finding recorded in `REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2D_DIAGNOSTIC_CHARACTERISATION_IMPLEMENTATION_PLAN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` (verdict: **TARGETED AMENDMENT REQUIRED BEFORE IMPLEMENTATION**, preserved unmodified) — it records the correction made in response to that finding and performs a fresh, independent adversarial review of the corrected Plan, not a mechanical upgrade of the earlier verdict. No live HTTP call, no campaign creation, and no repository mutation beyond editing the Plan in place and creating this one new document occurred.

# Unit 2-D Implementation/Execution Plan — Correction and Independent Constitutional Re-Review

## 1. Auditable history

The original Independent Constitutional Review of this Plan (same filename minus `_CORRECTION`) found one blocking defect (DQ5 prompt/parser incompatibility) and two non-blocking qualifications (an artifact record-count inconsistency; an unstated warm-up fixture text), and returned **TARGETED AMENDMENT REQUIRED BEFORE IMPLEMENTATION**. That document is unmodified by this task and remains the record of that finding. This document is the correction and re-review it called for.

## 2. Baseline verification

HEAD independently confirmed `d316032c...`, equal to `origin/main`. Working tree confirmed to contain, before this task's own edits, exactly the two documents named in the task: the Plan and its original Independent Constitutional Review — no unrelated changes present.

## 3. Evidence reviewed

Read fresh: the frozen Unit 2-D Scope Lock (unmodified by this task); its Independent Constitutional Review; the Plan, both before and after correction; the Plan's original Independent Constitutional Review; `src/runtime/ReasoningResponseParser.kt`; `src/interfaces/ReasoningProvider.kt`; `src/runtime/ModelReasoningProvider.kt`; `src/runtime/ReasoningPromptBuilder.kt`; `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt` (confirming the exact frozen warm-up fixture text used by Unit 2, reused verbatim in the correction).

## 4. Defect confirmation, independently re-derived from source

**Defect 1 (DQ5 parser/representation incompatibility) — confirmed.** Direct re-inspection of `src/runtime/ReasoningResponseParser.kt` shows `TaggedReasoningResponseParser.parse` matches only `trimmed.startsWith("GOAL:")`, `trimmed.startsWith("REPLY:")`, `trimmed.startsWith("REMEMBER:")`, or `trimmed == "NOACTION"`; the Plan's pre-correction DQ5 template requested bare, colon-less category words, which would fail to parse for three of the four categories. Independently confirmed.

**A second problem, found while designing the correction and not present in the original review's own text — confirmed and addressed.** `src/interfaces/ReasoningProvider.kt` shows `Goal`, `Reply`, and `Remember` each `require(text.isNotBlank())` in their constructors. Restoring the colon alone, with nothing after it (`"GOAL:"`), would not merely avoid throwing cleanly — `TaggedReasoningResponseParser`'s own `classifyRejected` helper treats any bare, colon-terminated tag as `PrimaryClassification.G` (blank/partial) *regardless of which tag it was*, which would have collapsed `GOAL:`, `REPLY:`, and `REMEMBER:` into one indistinguishable outcome and defeated DQ5 just as thoroughly as the original defect, in a subtler way. This review independently re-traces that reasoning against the source (Section 5 below) and confirms it holds.

**Defect 2 (artifact record-count inconsistency) — confirmed.** The pre-correction Plan Section 8 labeled `production-track/raw.jsonl` as "19 records" while DQ1+DQ2+DQ3+DQ4's own stated counts sum to 17; the discrepancy was traceable to the two warm-up records being silently, undeclared, folded into that figure.

**Defect 3 (unstated warm-up fixture text) — confirmed.** The pre-correction Plan scheduled two warm-up calls by ID (`warmup-acknowledgement`) without stating their literal input text anywhere in the document.

## 5. DQ5 correction — verified from source, not merely from the Plan's own explanation

The corrected Plan (Section 6) now specifies: `GOAL:`, `REPLY:`, or `REMEMBER:` followed by exactly the fixed placeholder word `"SELECTED"`, or exact `NOACTION` with nothing else. Independently traced through the unmodified parser and domain types:

- `"GOAL: SELECTED"`.trim() → starts with `"GOAL:"` → `Goal(trimmed.removePrefix("GOAL:").trim())` → `Goal("SELECTED")`. `"SELECTED".isNotBlank()` is `true` → constructs without throwing. `actualAction` is correctly captured as `GOAL`.
- The same reasoning holds for `"REPLY: SELECTED"` → `Reply("SELECTED")` and `"REMEMBER: SELECTED"` → `Remember("SELECTED")`, independently re-verified against each constructor's `require(text.isNotBlank())` check.
- `"NOACTION"` exactly → `trimmed == NOACTION_TAG` → `NoAction`, unchanged from existing production/DQ1–DQ4 behavior.

**No modification to `TaggedReasoningResponseParser`, `Goal`, `Reply`, `Remember`, or `NoAction` is required or made.** All four of DQ5's possible outcomes are now distinguishable by the unmodified parser and correctly attributable to a specific semantic action — the property the original defect destroyed.

**Independently tested for a second-order risk the corrected Plan itself already flags:** `contentFidelity` will trivially show `DEVIATION_OR_PARAPHRASE` for every successfully parsed DQ5 record, since `"SELECTED"` is never the fixture's real expected content. The corrected Plan (Sections 2, 6, 12) explicitly and repeatedly states this is expected, non-evidentiary, and must not be read as a finding — this review confirms that discipline is present everywhere DQ5's evidence is discussed, not only in Section 6.

**Independently re-tested against the frozen Scope Lock's own DQ5 authorization:** Scope Lock §5 defines DQ5 as testing accuracy when the model is "asked to select only the action category, without generating response content." A literal reading could ask whether requiring the fixed word `"SELECTED"` still counts as "generating response content." This review's independent judgment: no — Scope Lock's phrase is best read as targeting the *pragmatic composition burden* (deciding what to say, how to phrase a genuine reply or fact), not the presence of any non-blank string at all; a fixed, deliberately meaningless placeholder required only to satisfy an unrelated, pre-existing domain-type invariant is categorically different from generating content in the sense Scope Lock's DQ5 concerns, and the corrected design still varies exactly the one thing Scope Lock authorized varying (composition burden) while holding everything else constant. This review finds the correction **faithful to the frozen Scope Lock**, not merely internally consistent.

**Does DQ5 still measure decision/rendering coupling rather than merely formatting?** Yes, independently confirmed: the manipulated variable (Section 6 of the corrected Plan) is precisely "is genuine content composition required," and the held-constant variables (same four-way action set, same selection-guidance criteria, same tag syntax, same fixture/context/model/commit) isolate that one variable as cleanly as a single-prompt experiment of this kind can (a limitation on how *clean* an ablation any such experiment can be was already, correctly, disclosed by the Scope Lock's own Independent Constitutional Review, and is not reopened here).

## 6. Artifact-count correction — verified

Independently recomputed against the frozen 24-call schedule (Section 3, unchanged): 2 (warm-up) + 10 (DQ1) + 4 (DQ2) + 2 (DQ3) + 1 (DQ4) + 5 (DQ5) = 24, matching exactly. The corrected Section 8 now separates warm-up records into their own `warmup/raw.jsonl` (2), leaving `production-track/raw.jsonl` at exactly 17 (DQ1+DQ2+DQ3+DQ4) and `candidate-track/raw.jsonl` at exactly 5 (DQ5) — 2+17+5 = 24, reconciled. Every other count in Section 8's new accounting table (intent records = 24 unified; checkpoints = 2/17/5 matching their respective raw files; one unified manifest; six campaign-level, non-per-call metadata files) was independently re-added and found consistent, not merely asserted. No use of "record" remains ambiguous between raw observations, intent entries, and non-per-call metadata files.

## 7. Warm-up fixture correction — verified

The corrected Plan reuses, verbatim, Unit 2's own already-reviewed warm-up fixture text: `"Synthetic warm-up request: reply with a brief acknowledgement."`, expected action `REPLY`, observed but not scored — independently confirmed by this review as consistent with the raw evidence this session already inspected directly on the Ubuntu host (Unit 2's own warm-up records use this exact text). No new fixture or semantic experiment was introduced under cover of "warm-up"; the corrected text is synthetic, non-consequential, and structurally identical in kind to every other fixture already authorized in this Plan.

## 8. Fresh Phase 2 internal-consistency pass, independently re-run against the corrected document

All thirteen items independently re-checked against the corrected Plan's actual current text (not assumed from memory of drafting it): exact 24-call total intact (Sections 3, 8); DQ1–DQ6 mapping unchanged in substance and still faithful to Scope Lock §§5, 7, 9, 10, 21; no new fixture class introduced (DQ5 still uses `R01-direct` as its underlying fixture, only the instruction wrapper differs); no additional model comparison added (DQ4 remains the sole Llama cell); DQ5 remains exactly one candidate variant; parser/classifier reuse claims are now independently verified true (Section 5 above); artifact counts reconcile (Section 6 above); warm-up text is frozen (Section 7 above); Section 10's stop conditions are byte-for-byte unchanged by this correction; Section 1/8's isolation of the failed Unit 2 campaign is unchanged; Section 13's outcome-neutral exit states are unchanged; Section 16's remedy firewall is unchanged in substance and, if anything, reinforced by the correction's explicit "no schema/structured output introduced" language; Sections 17–18's execution-authorization prohibitions are unchanged.

## 9. New, non-blocking observations from this fresh review

Two minor documentation-clarity points, found independently during this re-review rather than inherited from the original review's three named defects — neither is judged to rise to a blocking defect, and neither is corrected in this pass, consistent with recording rather than silently repairing every observation found:

1. Section 3's deterministic-ordering table labels DQ5's trial-ID fixture slot as `R01-direct-decision-only`, while Section 2 describes DQ5's fixture as `R01-direct` under a separately-named variant. This is a pre-existing minor vocabulary looseness (present before this correction round too, not introduced by it) between "fixture identity" and "trial-ID label" — harmless for a human reader but worth tightening in a future revision.
2. The Plan does not explicitly state that Unit B must implement a second, DQ5-specific class satisfying the existing public `ReasoningPromptBuilder` interface (`src/runtime/ReasoningPromptBuilder.kt`) distinct from `DefaultReasoningPromptBuilder`, though this is the only architecturally sound way to realize Section 6's design without modifying production code, and is strongly implied by Section 6 and Section 15's own text. Independently confirmed structurally sound: `ModelReasoningProvider` takes its `promptBuilder` collaborator by constructor injection against the interface type (verified directly in `src/runtime/ModelReasoningProvider.kt`), so a second implementation requires no change to `ModelReasoningProvider`, `ReasoningPromptBuilder.kt`, or any other production file. Making this explicit in a future revision would remove any residual ambiguity for whoever implements Unit B.

## 10. Answers to the twelve required questions

1. **Was the original DQ5 defect genuinely corrected?** Yes, verified by direct source trace (Section 5), not accepted from the corrected Plan's own prose.
2. **Does the corrected DQ5 design remain within the frozen Scope Lock?** Yes (Section 5's Scope Lock §5 analysis above).
3. **Can `TaggedReasoningResponseParser` parse every permitted DQ5 action representation without modification?** Yes, all four outcomes independently traced (Section 5).
4. **Does DQ5 still measure decision/rendering coupling rather than merely formatting?** Yes (Section 5, final paragraph).
5. **Are artifact counts now internally and arithmetically consistent?** Yes (Section 6).
6. **Is the warm-up fixture now completely deterministic?** Yes (Section 7).
7. **Did any correction accidentally change the 24-call design?** No — independently re-verified unchanged (Sections 6, 8).
8. **Did any correction leak into remedy selection?** No — no schema, structured output, retry, or parser change was introduced; the firewall is reinforced, not weakened.
9. **Did any correction require production/test/Gradle changes?** No — every correction is prose within the Plan document; zero source files were touched by this task.
10. **Is the corrected Plan now safe to freeze?** Yes, subject to the two non-blocking observations in Section 9 being addressed at a future convenient revision, which is not required before freezing.
11. **Is implementation authorized after acceptance?** Only after this Plan is frozen/committed — this review's acceptance is not itself that authorization, and implementation additionally requires everything the Plan's own Section 17 already specifies.
12. **Is live execution authorized?** No — unaffected by this correction; the Plan's Section 17 gate sequence (Implementation Readiness Review, its own Independent Constitutional Review, explicit execution approval) remains entirely unreached.

## 11. Independent verification of isolation from the failed Unit 2 campaign

Read-only, during this task: `/var/lib/parker/reasoning-protocol-live-model/qwen25coder7b-baseline-20260809/` inspected but not modified; `stage-0.failed`, `manifest.txt`, `raw.jsonl` hashes recomputed and found identical to every prior check across this session; `stage-0.sealed` absent; no `stage-1/`, `stage-2/`, or diagnostic campaign directory exists anywhere on the host.

## 12. Final constitutional verdict

```text
ACCEPTED. READY TO FREEZE.
```

This verdict applies to the corrected Plan as it now stands, independently re-tested against the frozen Scope Lock, the actual parser and domain-type source, the harness's isolation properties, the exact 24-call arithmetic, and the reconciled artifact accounting — not granted as a mechanical upgrade of the prior TARGETED AMENDMENT REQUIRED finding. Implementation (Plan Section 15, Units A and B) is authorized to begin only after this Plan is frozen and committed. Live execution of any kind remains unauthorized and requires the full remaining gate sequence in the Plan's own Section 17 (Implementation Readiness Review, its Independent Constitutional Review, and explicit execution approval), none of which this document performs.

## 13. Confirmation

No model or HTTP call occurred during this task. No `/api/generate`, `/api/tags`, or `/api/show` call occurred. The Unit 2 campaign artifacts (`qwen25coder7b-baseline-20260809`) remain byte-identical to every prior verification in this session. The frozen Unit 2-D Scope Lock and its own Independent Constitutional Review were not modified. The Plan's original (pre-correction) Independent Constitutional Review was not modified — its `TARGETED AMENDMENT REQUIRED BEFORE IMPLEMENTATION` verdict remains intact and unaltered as the historical record this document builds on. No production, test, or Gradle file changed. Nothing was staged, committed, or pushed.
