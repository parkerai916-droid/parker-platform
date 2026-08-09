**Status:** Independent Constitutional Review of the Unit 2-D Implementation/Execution Plan — **TARGETED AMENDMENT REQUIRED BEFORE IMPLEMENTATION.** One blocking defect was found in the Plan's DQ5 prompt-control specification (Section 9 below). The Plan was treated as evidence, not authority, and tested against the frozen Scope Lock and direct source inspection, not merely for internal consistency. No live HTTP call, no campaign creation, and no repository mutation beyond this one new document occurred. Neither the Plan nor any previously frozen document was modified.

# Unit 2-D Implementation/Execution Plan — Independent Constitutional Review

## 1. Status

This review tests `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2D_DIAGNOSTIC_CHARACTERISATION_IMPLEMENTATION_EXECUTION_PLAN.md` against the frozen Scope Lock it claims to implement. It does not authorize implementation. Where it finds a defect, the defect is recorded here; the Plan itself is not edited.

## 2. Review authority and baseline

HEAD independently confirmed `d316032c...`, equal to `origin/main`, working tree clean before this review began, apart from the two expected new documents this task produced (the Plan and, now, this review).

## 3. Evidence reviewed

Read fresh: the Plan in full; the frozen Scope Lock, its prior Review, and its Independent Constitutional Review; `REASONING_PROTOCOL_POST_UNIT_2_DIAGNOSTIC_PLANNING_REVIEW.md`; `REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_POST_STAGE_0_FAILURE_GOVERNANCE_DETERMINATION.md`; both Unit 2 Stage 0 Failure Review documents; the Unit 2 Scope Lock and Implementation/Execution Plan. Independently re-inspected by direct source read, not accepted from the Plan's own prose: `src/runtime/ReasoningResponseParser.kt` (`TaggedReasoningResponseParser`'s exact matching logic — this is where the blocking defect below was found); `src/runtime/ReasoningPromptBuilder.kt`; `src/runtime/ModelInferenceClient.kt`; `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt` (confirming `BaselineCorpus` and `OllamaIdentityEvidence` are file-private, and confirming the exact fixture texts the Plan claims to reuse verbatim); `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` (confirming `ContextProfileId`, `SyntheticContextProfiles`, `ConformanceFixture`, `PrimaryClassification`, `ContentFidelity` are public, and confirming zero Memory/Goal/Planner/composition imports).

## 4. Fidelity to the frozen Scope Lock

Tested clause by clause, not merely trusted. The Plan's Section 2 fixture/profile/model/attempt table for DQ1–DQ6 was independently checked against Scope Lock §§5, 7, 9, 10 and found to match exactly — same fixture IDs and text, same profiles, same models, same attempt counts, same expected actions. The Plan's Section 3 schedule was independently re-derived from Scope Lock §18 rather than accepted (Section 5 below). The Plan's Section 16 remedy firewall is a superset of Scope Lock §23's twelve items plus the task's own explicit additions, and every item discussed diagnostically elsewhere in the Plan (structured output, DQ5's variant, DQ4's model comparison) is present in it. No provision was found where the Plan silently broadens, narrows, or reinterprets the Scope Lock — with the one specific exception examined in Section 9, which is a fidelity problem in a different sense: the Plan's own internal specification is not self-consistent, not that it deviates from the Scope Lock's boundary.

## 5. Independent 24-call reconstruction

Reconstructed directly from the Plan's own Section 3 table and cross-checked against Scope Lock §18:

```text
DQ1:  10
DQ2:   4
DQ3:   2
DQ4:   1
DQ5:   5
Warm-up: 2
TOTAL: 24
```

10+4+2+1+5+2 = 24. Confirmed correct, and confirmed to match the Scope Lock's own §18 total exactly — no addition, no omission.

## 6. DQ-to-evidence traceability

Each of DQ1–DQ6 in the Plan's Section 2 names an exact fixture, profile, model, attempt count, expected action, and a permitted/prohibited interpretation pair. Cross-checked against Scope Lock §21's seven interpretation rules: every permitted/prohibited pair in the Plan is either a direct restatement of, or a strict narrowing of, the corresponding Scope Lock rule. No DQ's evidence requirement in the Plan exceeds what the Scope Lock actually authorized collecting.

## 7. Absence of remedy leakage

Section 16's firewall is comprehensive and consistent with Scope Lock §23. Checked specifically for the risk this task calls out: does specifying DQ5's exact template (Section 6 of the Plan) itself cross into "prompt rewriting" as a remedy? This review finds it does not — the Plan explicitly frames the variant as non-production, firewalled from deployment consideration, and necessary only to make the Scope Lock's already-authorized DQ5 question executable; a Plan cannot be "precise" and "executable," as required by this task, while leaving DQ5's actual wording undefined. This is the same reasoning this review's own antecedent (the Scope Lock's Independent Constitutional Review) already applied to DQ5's existence in principle; this review independently re-applies it to the Plan's concrete instantiation and reaches the same conclusion. No leakage found on this point.

## 8. Isolation from the failed Unit 2 campaign

Section 8's artifact design places the new campaign in a sibling directory under the same parent (`/var/lib/parker/reasoning-protocol-live-model/`), never inside `qwen25coder7b-baseline-20260809/`, and — independently verified as a genuine strength, not merely asserted — requires a mandatory execution-time isolation-guard assertion that the resolved artifact root is neither equal to nor nested inside the Unit 2 campaign directory, failing closed if that assertion does not hold. This goes beyond what the Scope Lock's own §17 literally required (a naming convention) and adds a mechanical safeguard against an operational path-construction bug, which this review considers a genuine improvement rather than scope creep, since it protects the same boundary the Scope Lock already drew. Independently re-verified on the live host during this review: `qwen25coder7b-baseline-20260809`'s artifacts remain untouched (Section 15 below).

## 9. Model-comparison discipline

Section 4's identity-verification approach was independently checked against the actual code, not accepted from the Plan's description: `OllamaIdentityEvidence.exactModelDigest` is confirmed `private` inside Unit 2's own file (`private object OllamaIdentityEvidence`, `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt:736`), so the Plan's requirement to reimplement rather than import this technique is correct and necessary, not merely cautious. Section 9's model-comparison interpretation constraints (DQ4) correctly restate Scope Lock §21 rules 3–4 without weakening them.

## 10. DQ5 firewall — **BLOCKING DEFECT FOUND**

This is the most consequential finding of this review, and it is a defect in the Plan's own internal consistency, not in the Scope Lock or in DQ5's underlying rationale.

Plan Section 6 specifies the DQ5 candidate-track template as: *"Respond with exactly one of: GOAL, REPLY, REMEMBER, or NOACTION — the category alone, with no colon, no explanation, and no additional text."*

Plan Section 9 states that classification is "reused unchanged, by direct import from Unit 1's public harness types," and Plan Section 15 (Unit B) lists `TaggedReasoningResponseParser` among the production types the new file imports unchanged to parse DQ5's output.

Independently re-inspected directly against `src/runtime/ReasoningResponseParser.kt` during this review: `TaggedReasoningResponseParser.parse` matches only `trimmed.startsWith("GOAL:")`, `trimmed.startsWith("REPLY:")`, `trimmed.startsWith("REMEMBER:")`, or `trimmed == "NOACTION"` exactly; anything else throws `UnclassifiableModelResponseException`. A response of the bare, colon-less form the Plan's own DQ5 template requests — for example a model producing exactly `REMEMBER` — does not match any of the first three branches (no colon present) and is not the literal string `NOACTION`, so it throws. The one category that happens to work by coincidence is `NOACTION`, since `NOACTION_TAG` itself carries no colon requirement; `GOAL`, `REPLY`, and `REMEMBER` do not.

**Consequence, independently traced through to its effect on the actual diagnostic question:** if implemented exactly as Plan Section 6 specifies, every DQ5 trial where the model correctly or incorrectly selects GOAL, REPLY, or REMEMBER would be recorded as a representation failure (`representationValid = false`, a rejected/malformed classification) regardless of the model's actual category choice — not because the model failed to decide, but because the parser cannot recognize the format the Plan itself asked the model to use. This does not merely produce noisy data: it would make DQ5 systematically unable to measure the one thing it exists to measure (semantic accuracy under a decision-only task, Section 2 of the Plan), and — because the failure mode is action-dependent (`NOACTION` alone would parse) rather than uniform — a naive reader of the resulting worksheet could easily and wrongly read an artifact of the parser mismatch as a substantive finding about representation independence (DQ6) or about the coupling hypothesis (DQ5) itself. This is exactly the kind of overclaiming Section 12's own interpretation rules exist to prevent, and the defect would produce it structurally, not through any misreading of correct data.

This is judged a **blocking defect**: it does not merely weaken DQ5's evidentiary strength, it inverts it — the worse the parser mismatch, the more confidently wrong a reader could become. It requires correction to Plan Section 6 (and the corresponding evidence description in Plan Section 2's DQ5 entry) before implementation may proceed on this Plan.

**Nature of the required correction, stated for the record without performing it:** the DQ5 template must request output in a form `TaggedReasoningResponseParser` can still classify — for example, retaining the existing colon-tagged prefixes (`GOAL:`, `REPLY:`, `REMEMBER:`, or exact `NOACTION`) while removing only the requirement to supply content after the prefix, so that the single varied element remains "is content generation required," not "is the tag itself recognizable." This review does not make this edit; it is recorded here as the defect's substance so a corrected Plan can be drafted and re-reviewed.

## 11. Sampling-state limitations

Section 7's treatment of the unpinned sampling configuration was independently re-verified against `src/runtime/ModelInferenceClient.kt`'s `defaultOllamaRequestBody` (confirmed to set only `model`/`prompt`/`stream:false`) and found to state the resulting interpretive limitation precisely and consistently with the Scope Lock's own §11 and §21 rules — no invented configuration value, no claim of statistical confidence the unpinned state does not support. No defect found here.

## 12. Artifact durability/integrity — non-blocking defect found

Plan Section 8 labels `production-track/raw.jsonl` as holding "DQ1-DQ4 observations (19 records)." Independently recomputed: DQ1 (10) + DQ2 (4) + DQ3 (2) + DQ4 (1) = **17**, not 19. The figure of 19 is only reachable if both warm-up records (2) are silently included in "production-track," which the Plan's prose does not state — Section 3's schedule places both warm-ups before their respective model blocks but Section 8 never assigns them to either `raw.jsonl` file. This is a genuine, findable arithmetic/labeling inconsistency, smaller in kind than Section 10's finding but real: a document whose own stated purpose is to "show the arithmetic explicitly" (per this task's own instruction) should not contain an uncorroborated total. **Non-blocking** — the underlying 24-call total (Section 5 above) is unaffected and correct; only this one sub-total's label is wrong. Correction required: either state explicitly that warm-ups are stored in `production-track/raw.jsonl` (making 19 correct) or correct the figure to 17 and account for warm-ups separately.

A second, related **non-blocking** completeness gap: Plan Section 3's schedule references the `warmup-acknowledgement` fixture ID twice (for the Qwen and Llama warm-ups) but no section of the Plan states its literal text. Unit 2's own precedent fixture text (`"Synthetic warm-up request: reply with a brief acknowledgement."`) is the obvious candidate for reuse, and this review expects that to be the intended text, but the Plan does not say so explicitly, and a document meant to be "executable" without further invention should.

## 13. Stop-condition correctness

Plan Section 10 was independently checked line-by-line against Scope Lock §20 and found to be an exact translation — same hard-stop list, same not-a-stop-condition list, same reasoning for why Unit 2's Stage 0 gate does not transfer unmodified. No broadening or narrowing found. The Plan's addition of the Section 8 isolation-guard and Section 5 execution-time re-verification as explicit hard-stop triggers is a specification of Scope Lock §20's existing "identity, configuration... mismatch" category, not a new category — correctly classified.

## 14. Downstream isolation

Plan Section 11's claim that the existing harness and driver contain zero Memory/Goal/Planner/tool/composition imports was independently re-verified by direct grep during this review (repeated from the prior Scope Lock review's own verification, re-run rather than assumed): confirmed zero such imports in `ReasoningProtocolLiveModelEvaluationHarness.kt`. The Plan's requirement that Unit 2-D's new file be held to the identical, source-scan-verifiable standard is sound and appropriately specific (naming a concrete verification method, not just an aspiration).

## 15. Outcome-neutral exit criteria

Plan Section 13's three named exit states (`CAMPAIGN_COMPLETE_INTEGRITY_VERIFIED`, its `DIAGNOSTICALLY COMPLETE — INCONCLUSIVE` sub-state, and `CAMPAIGN_HALTED_INSTRUMENT_DEFECT`) were independently checked against Scope Lock §22 and found faithful: no exit state requires a particular substantive result, and the inconclusive sub-state is correctly treated as a valid success rather than a failure, matching Scope Lock §22's own text. This is a genuine improvement in clarity over the Scope Lock's own prose (which described the same idea without naming it), consistent with this review's own prior recommendation on that point.

## 16. Independent repository and artifact verification

Read-only, during this review: `/var/lib/parker/reasoning-protocol-live-model/qwen25coder7b-baseline-20260809/` inspected but not modified; `stage-0.failed`, `manifest.txt`, and `raw.jsonl` hashes recomputed and found identical to every prior check across this session; `stage-0.sealed` absent; no `stage-1/`, `stage-2/`, or diagnostic campaign directory exists. `git status` shows only the Plan and this review as new files; no production, test, or Gradle file changed.

## 17. Whether implementation may safely proceed after acceptance

Not on the Plan as currently drafted. Section 10's blocking defect must be corrected in the Plan (Section 6's DQ5 template, and Section 2's corresponding DQ5 evidence description) and the corrected Plan re-reviewed before Unit A or Unit B (Section 15 of the Plan) may be implemented. The two non-blocking findings in Section 12 above should be corrected in the same pass, as a matter of document quality, though they do not by themselves block the sequence.

## 18. Blocking defects

One: **Plan Section 6's DQ5 prompt template is incompatible with `TaggedReasoningResponseParser`, the exact parser Plan Sections 9 and 15 say will be reused unchanged to classify its output**, independently confirmed by direct inspection of `src/runtime/ReasoningResponseParser.kt` (Section 10 above). As specified, DQ5 would systematically misclassify three of its four possible outcomes as representation failures regardless of the model's actual semantic choice, defeating the diagnostic purpose DQ5 exists to serve and risking a false representation-independence or coupling conclusion built on a parser artifact rather than model behavior.

## 19. Non-blocking qualifications

1. Plan Section 8's "19 records" label for `production-track/raw.jsonl` does not reconcile with DQ1–DQ4's own stated counts (17) unless warm-ups are silently included; state this explicitly one way or the other (Section 12 above).
2. Plan Section 2/3 never states the literal warm-up fixture text; state explicitly that Unit 2's own precedent text is reused, or specify different text and justify it.

## 20. Required amendments

Before this Plan may be treated as ready for implementation: correct Section 6's DQ5 template to a form `TaggedReasoningResponseParser` can classify without modification (Section 10 above defines the nature of the required change without performing it), and correct the corresponding DQ5 evidence description in Section 2. The two non-blocking items in Section 19 should be corrected in the same revision. A corrected Plan requires a fresh Independent Constitutional Review before implementation begins; this review does not pre-approve a future, unseen correction.

## 21. Final verdict

```text
TARGETED AMENDMENT REQUIRED BEFORE IMPLEMENTATION
```

This verdict is not a rejection of the Plan's overall design, which this review found faithful to the frozen Scope Lock in every other respect tested (Sections 4–9, 11, 13–16 above all found no defect). It is a specific, narrow, corrigible defect in one prompt-template specification whose consequence — if implemented as written — would be severe enough to undermine the one diagnostic question (DQ5) it governs. Per this task's own instruction, this review does not repair it and does not approve implementation while it stands.

## 22. Exact next authorized step

Correcting Plan Section 6 (and Section 2's DQ5 line) to specify a parser-compatible DQ5 template, and Section 8's warm-up record accounting, is the next available governance action — not performed, authorized, or initiated by this review. After that correction, a fresh Independent Constitutional Review of the corrected Plan is required before Unit A or Unit B implementation may begin. Live execution remains additionally gated by the Implementation Readiness Review sequence in the Plan's own Section 17, unaffected by and unreached by this finding.

## 23. Confirmation

No model or HTTP call occurred during this review. No `/api/generate`, `/api/tags`, or `/api/show` call occurred. The Unit 2 campaign artifacts (`qwen25coder7b-baseline-20260809`) remain byte-identical to every prior verification in this session. No production, test, or Gradle file changed. The Plan and every previously frozen governance document remain unmodified by this review. Nothing was staged, committed, or pushed.
