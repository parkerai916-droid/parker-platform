**Status:** Post-Unit-2 Diagnostic Planning Review — **B: A NARROWLY SCOPED NEW UNIT WITHIN THE EXISTING REASONING PROTOCOL LIVE-MODEL CONFORMANCE AND STRUCTURED-OUTPUT RELIABILITY PROGRAMME.** Governance/planning only, against committed baseline `6fff498`. No live HTTP call, no `/api/generate`, `/api/tags`, or `/api/show` call, no campaign mutation, no production/test/Gradle change, and no repository mutation beyond this document occurred. Unit 2's accepted `A — STOP AT THE EXISTING STAGE 0 FAILURE BOUNDARY` determination is not reopened.

# Reasoning Protocol Post-Unit-2 Diagnostic Planning Review

## 1. Status and scope

This document plans, but does not authorize or execute, the next governed diagnostic work following the accepted Unit 2 Stage 0 failure. It defines what evidence and controlled experiments are necessary before Parker changes its reasoning protocol implementation, and recommends a governance path for obtaining that evidence. It selects no remedy, implements no code, launches no campaign, and makes no model call.

## 2. Accepted baseline

```text
Fixture: R01-direct / minimal-production-context
Input: "Remember that my synthetic test coffee mug is black."
Expected: REMEMBER
Observed: REPLY: I understand that your synthetic test coffee mug is black. Is there
          something specific you'd like me to do related to this information?
Parsed: Reply (representation valid)
Primary classification: D (genuine semantic action-selection failure)
Campaign state: PREFLIGHT_FAILED, stage-0.failed = "PF01:D", stage-0.sealed absent,
                Stage 1/2 not run, preserved unchanged
Post-Stage-0 determination (6fff498): A — STOP UNIT 2 AT THE EXISTING BOUNDARY
```

This determination is treated as settled. It is not re-litigated here.

## 3. Evidence reviewed

Read fresh at `6fff498`: the Reasoning Protocol Live-Model Conformance and Structured-Output Reliability Planning Review and its Independent Constitutional Review; the Unit 2 Scope Lock and its Independent Constitutional Review; the Unit 2 Implementation/Execution Plan and its Independent Constitutional Review; the Unit 2 Implementation Readiness Review and its Independent Constitutional Review; both Unit 2 Stage 0 Failure Review documents; `REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_POST_STAGE_0_FAILURE_GOVERNANCE_DETERMINATION.md`; the `REASONING_PROVIDER_CONTRACT_DESIGN.md` architecture document; and the current production implementation: `src/runtime/ReasoningPromptBuilder.kt` (`DefaultReasoningPromptBuilder`), `src/runtime/ReasoningResponseParser.kt` (`TaggedReasoningResponseParser`, `UnclassifiableModelResponseException`), `src/runtime/ModelReasoningProvider.kt`, and `src/runtime/ModelInferenceClient.kt` (`LocalHttpModelInferenceClient`, `defaultOllamaRequestBody`/`defaultOllamaResponseBody`).

Three facts from that reading materially shape this plan:

1. `DefaultReasoningPromptBuilder` is a single, deterministic template with **no few-shot examples and no persona instruction**, and its own doc comment records that the `REMEMBER:` selection guidance was already tightened once before, after live testing showed the model defaulting an ordinary greeting to `NOACTION` — i.e., prompt-guidance revision in response to observed model behavior is an established, precedented activity in this codebase, not a novel one.
2. `TaggedReasoningResponseParser`'s own doc comment states it is explicitly "a starting-point default, not the permanent parsing protocol," and that structured JSON, grammar-constrained output, or function-calling are all pre-authorized future implementations of the same one-method `ReasoningResponseParser` interface, requiring no change to `ModelReasoningProvider` or to any architecture document. Representation strategy is already architecturally swappable; nothing about PF01 required this.
3. `defaultOllamaRequestBody` sets only `model`, `prompt`, and `stream:false` — no `temperature`, `seed`, `top_p`, or `top_k`. Both production and the evaluation harness therefore run under Ollama's default (non-pinned) sampling. Run-to-run variation is architecturally possible; it has never been measured.

## 4. PF01 epistemic decomposition

**Directly observed:**
- The exact prompt sent equals the production-generated prompt for `minimal-production-context` (verified byte-for-byte against `DefaultReasoningPromptBuilder`'s output in prior review).
- The model's single generation was `REPLY: I understand that your synthetic test coffee mug is black. Is there something specific you'd like me to do related to this information?` — one well-formed tag, cleanly parsed, no parser exception.
- Expected REMEMBER, actual REPLY, representation valid, classification D, under one fixed repository commit / model digest / timeout / context profile, with zero temperature/seed control.
- No downstream Memory or Goal action was ever dispatched; the harness terminates at `TrialObservation`.

**Strongly supported inference:**
- The model comprehended the stated fact — it restated it correctly inside the REPLY content — so this is not a comprehension failure of the input. The miss is specifically at the level of choosing which *action* the input calls for, not at understanding what the input said.
- This is not a representation, parser, classifier, harness, or driver defect; those were independently re-verified in the accepted Stage 0 Failure Review and its Independent Constitutional Review.

**Hypothesis (unconfirmed, competing, and not mutually exclusive):**
- (A) The fixed prompt's REMEMBER guidance, however explicit, may be insufficiently weighted relative to the model's general conversational habit of acknowledging stated facts.
- (B) The pinned `qwen2.5-coder:7b` configuration may have weaker pragmatic/dialogue-act classification reliability than a general-purpose chat model of comparable size — plausible given it is a code-specialized variant, not established.
- (D) Deciding the action and composing response text happen inside one single free-text generation; the two may be coupled such that the model's habitual "acknowledge and offer help" completion pattern crowds out the narrower classification decision.
- (E) Context enrichment (fuller `ReasoningContext`) might resolve or worsen the miss; untested.
- (F) This specific completion might be a low-probability sampling outcome for this exact prompt, or a highly reproducible one; untested, and architecturally possible either way given unset sampling parameters.

**Unknown:**
- Whether this generalizes to REMEMBER fixtures generally, to GOAL/REPLY/NOACTION selection, to other context profiles, to repeated sampling of the identical prompt, to other models, or to a decision-only variant of the task.

No single one of hypotheses A–H in the task's framing is established or excluded by PF01 alone. C (representation reliability) is the one hypothesis this evidence base actively argues *against* as an explanation for this specific observation — representation was valid.

## 5. Uncertainty register

| # | Question | Category | Current status |
|---|---|---|---|
| U1 | Is the REMEMBER miss reproducible under identical prompt/model/config, or was it a single stochastic draw? | F | Unknown — untested, architecturally possible either way (no sampling controls set) |
| U2 | Do GOAL, REPLY, and NOACTION selection show comparable single-attempt miss behavior under the frozen fixtures, or is the weakness narrow to REMEMBER? | A/B | Unknown — PF02–PF08 exist for exactly this and are not authorized against the failed campaign |
| U3 | Does context enrichment (the eight non-minimal Unit 1 profiles) change the outcome for `R01-direct`? | E | Unknown |
| U4 | Is the weakness specific to `qwen2.5-coder:7b`, or does a general-purpose model of comparable size show the same miss on the identical fixture? | B | Unknown |
| U5 | Does isolating the action-decision from response-text generation (a decision-only task variant) change semantic accuracy on this fixture? | D | Unknown |
| U6 | Does forcing output through an enumerated/constrained decoding scheme change semantic accuracy (as distinct from representation conformance, which is not the problem here)? | C/D | Unknown, and conceptually distinct from the already-settled representation-validity finding |
| U7 | Do inference parameters (temperature/seed) materially affect the outcome? | G | Unknown, and only decision-relevant if U1 shows meaningful variability |
| U8 | Would a differently-worded or differently-ordered REMEMBER instruction change the outcome under the *current* protocol's semantics? | A | Unknown; must not be conflated with changing the frozen production prompt |
| U9 | Is the current single-call, decide-and-render-together protocol shape itself the wrong architecture for this decision, independent of model or prompt wording? | H | Unknown; only answerable after U5/U6 provide a basis for comparison |

## 6. Decision-critical evidence requirements

**Decision-critical** (the correct remedy family differs sharply depending on the answer, so these must be resolved before any remedy is selected):
- **U1 (repeatability)** — determines whether the remedy target is *consistency* (e.g., verification/sampling strategies) or *capability* (a genuinely wrong classification the model reliably makes).
- **U2 (breadth)** — determines whether the remedy should be REMEMBER-specific (narrow prompt/guidance change, or a dedicated pre-classification step for consequential actions) or protocol-wide (something more structural).
- **U4 (model-specific vs. protocol-general)** — determines whether the remedy family is "change the model" or "change the protocol/prompt," which are different investments with different consequences.
- **U5/U6 (coupling and constrained decoding)** — determine whether structured output or task-decomposition are *live* candidate remedies at all, or a dead end for this specific failure mode (see Section 8).

**Useful but non-critical:**
- U3 (context sensitivity) — informs prompt-design tuning later; does not by itself select a remedy family.
- U7 (inference configuration) — only matters once U1 shows the failure is at least partly stochastic; premature to prioritize before that.

**Merely interesting:**
- U8 in its broadest form (open-ended prompt-wording sweeps) — legitimate only as a narrowly bounded companion to U5, not as an open research programme.
- U9 — a genuine long-term architectural question, but not answerable, and not necessary to answer, before the narrower diagnostic programme below completes; it is a *conclusion* the diagnostic evidence might eventually support, not an input to it.

## 7. Semantic-vs-representation diagnostic analysis

The current `GOAL:`/`REPLY:`/`REMEMBER:`/`NOACTION` protocol asks one model completion to do three things at once: (a) decide the semantic action, (b) emit a syntactically valid tag for it, and (c) generate the response content. PF01 already demonstrates these are *empirically* separable in outcome — (b) succeeded, (a) failed, and (c) was fluent and factually accurate but attached to the wrong action. The measurement layer already keeps these analytically distinct: `TaggedReasoningResponseParser` reports representation validity independently of `primaryClassification`'s action-match check, and this separation required no design change to observe.

What is *not* separated is the **generative act** itself — the model is never asked to decide the action independently of composing the reply. A diagnostic experiment that isolates action-decision from response-generation (e.g., a decision-only forced-choice variant of the identical fixture, scored only on whether the chosen category matches, with no reply content required) would let U5 be answered directly: if decision-only accuracy is materially higher than joint-task accuracy, that is evidence for hypothesis D (coupling); if it is comparably poor, that argues against D and toward B. This is diagnostically valuable and experimentally cheap, but it is a **new prompt variant**, not the current frozen production protocol, and must be labeled and governed as exactly that (Section 12).

## 8. Structured-output diagnostic analysis

Structured/schema-constrained output is not assumed to be a fix. What it could and could not prove must be kept explicit:

- **Could prove:** whether constraining decoding to an enumerated action set (as opposed to free-text-then-parse) changes *semantic* accuracy — a distinct, unproven hypothesis about how constrained decoding might reshape the underlying generative distribution, not merely a syntax guarantee.
- **Could not prove, and does not need to:** that representation becomes more reliable. PF01 already shows representation was not the problem — the model produced one syntactically valid tag on its own. Structured output's most obvious benefit (guaranteeing parseable output) has no defect to fix in this observation. Treating structured output as *the* remedy on the strength of PF01 would be a category error: it would address axis (b)/(c) reliability for a failure that occurred on axis (a).
- Any evaluation of structured output belongs strictly to the U6 diagnostic experiment (Section 5), run as a clearly labeled candidate-protocol variant, never inside a characterization of the current frozen production prompt (Scope Lock §8/§12 forbid adding `format`/JSON/schema/grammar to the frozen production request under Unit-2-style measurement, and that constraint is sound governance to carry forward here too).

## 9. Repeatability analysis

Necessary, not optional. `defaultOllamaRequestBody` sets no `temperature` or `seed`; both production and evaluation traffic run under Ollama's default sampling, so run-to-run variation for an identical prompt is architecturally possible, not merely a theoretical concern raised for completeness. Without repeating the exact PF01 prompt/model/config combination, "genuine semantic action-selection failure" cannot be distinguished from "one unlucky draw from an otherwise mostly-correct distribution" — and those two states call for different remedies (Section 6).

No sample count is asserted here. Scope Lock's own accepted methodology treats 30 attempts per cell as sufficient for the *statistical* baseline (Wilson intervals, confusion matrices). A diagnostic repeatability check answers a narrower question — reliably-wrong versus occasionally-wrong — and does not need statistical-baseline power to be useful; the appropriate count for that narrower purpose should be fixed by the new diagnostic unit's own Scope Lock, reasoned from that narrower purpose, rather than asserted in this planning document.

## 10. Context-sensitivity analysis

Legitimate and low-cost: Unit 1 already froze nine context profiles, and PF02 in the original Stage 0 schedule was exactly `R01-direct` under `mixed-full-production-like`. Observing `R01-direct` under a small number of the existing nine frozen profiles (not the failed campaign's own PF02 — that specific call remains unauthorized against the preserved campaign — but the identical fixture/profile pair run under a *new* campaign identity) would answer U3 without inventing new context designs. This is useful triage information (Section 6) but not decision-critical on its own.

## 11. Model-comparison analysis

Useful, bounded, and hypothesis-driven — not a benchmarking exercise. Comparing `qwen2.5-coder:7b` against `llama3.2:3b` (stated to already be locally available) on the *identical* frozen fixture/prompt/context tests a specific, motivated question: is this a code-specialized-model weakness or a small-local-model-general weakness? That is a legitimate U4 test. It is explicitly **not** "does a bigger model do better" — no larger-model comparison is proposed here; the task correctly warns against recommending model size as a proxy for capability, and a larger-model comparison, if ever wanted, belongs to its own, separately governed future experiment, not to this diagnostic unit's minimum scope. Comparing the *same* Qwen configuration under the decision-only prompt variant (Section 7) is a separate, equally legitimate comparison axis (U5), and must not be conflated with the cross-model comparison.

## 12. Prompt-variant analysis

Two categories must remain explicitly distinct and separately labeled in any future work:

1. **Diagnosing the current frozen protocol** — repeating the exact PF01 prompt (U1), observing it under existing frozen Unit 1 context profiles (U3), and observing it under a different model (U4). None of this changes the prompt; all of it characterizes the protocol exactly as deployed today.
2. **Experimenting with candidate protocol designs** — the decision-only variant (U5) and any constrained-decoding variant (U6). These are deliberately *not* the current production prompt. They exist to test hypotheses about what a future design might do differently, and their results must never be silently folded into a claim about "the" Parker protocol's current reliability, nor adopted into production without their own separate implementation governance.

Any future diagnostic Scope Lock must keep these two categories in visibly separate sections with separate artifact paths, exactly as Unit 2 kept Stage 0 separate from scored Stage 1/2.

## 13. Proposed diagnostic architecture

Reuse, do not reinvent: the same Unit 1 `ConformanceFixture`/`SyntheticContextProfiles`/harness machinery, the same classification scheme (`PrimaryClassification` A–I, `ContentFidelity`), and the same artifact/manifest/hash-verification conventions that Unit 2 already established and that this session independently re-verified as trustworthy. A new, small, explicitly diagnostic unit — sibling to Unit 2, not a continuation of it — would need:

- its own campaign identity, distinct from `qwen25coder7b-baseline-20260809`, touching no existing artifact;
- a small, closed fixture/model/profile matrix sized to the decision-critical questions in Section 6 only (repeatability of `R01-direct` under fixed config; `R01-direct` under a handful of the nine existing profiles; `R01-direct` under `llama3.2:3b`; a decision-only variant of `R01-direct` under the pinned Qwen configuration) — not a re-run of the full 23-fixture corpus, and not a resumption of PF02–PF08 against the failed campaign;
- explicit, permanent labeling of every observation as unscored/diagnostic, never contributing to any future Unit 2 statistical baseline;
- its own Scope Lock, Planning Review, Implementation/Execution Plan, and Independent Constitutional Reviews at each stage, at a scale proportional to its narrower purpose — not skipped, not miniaturized past the point of genuine independent review.

## 14. Minimum necessary experiment classes

1. **Repeatability** (U1): repeated live calls of the exact PF01 prompt/model/config combination.
2. **Breadth under existing frozen fixtures** (U2, informed by but distinct from the unauthorized PF02–PF08): the remaining three expected-action categories (GOAL/REPLY/NOACTION) observed under a new campaign identity, not the preserved one.
3. **Context sensitivity** (U3): `R01-direct` under a small subset of the nine existing frozen context profiles.
4. **Cross-model comparison** (U4): `R01-direct` (and ideally the same small set used above) under `llama3.2:3b`, identical prompt/harness.
5. **Decision/rendering decoupling** (U5): a decision-only variant of `R01-direct` under the pinned Qwen configuration.
6. **Constrained-decoding variant** (U6), lowest priority of the six: only pursued if U5 or U1 leave a live open question that constrained decoding could plausibly resolve — not pursued reflexively.

Each is independently justified in Sections 6–12; none is included merely because it would be interesting.

## 15. Governance requirements

A new diagnostic unit requires, at minimum: an explicit new campaign identity that never touches `qwen25coder7b-baseline-20260809`'s artifacts; a Scope Lock defining the closed experiment classes in Section 14, the sample sizes for each (reasoned, not copied wholesale from Unit 2's statistical design), and an explicit prohibition on treating any output as a statistical characterization or a completed Unit 2 baseline; an Implementation/Execution Plan and Boundary Review before any code exists; Implementation Readiness Review and Independent Constitutional Review before any live call; and explicit execution approval, exactly mirroring the gates Unit 2 itself required. No part of this is authorized by the present document.

## 16. Diagnostic exit criteria

The diagnostic phase is sufficient to hand evidence to remedy selection (Unit 3) once, at minimum:
- U1 has a reasoned repeatability finding (reliably-wrong vs. materially variable) for the PF01 fixture;
- U2 has at least a qualitative breadth finding across GOAL/REPLY/NOACTION under the frozen protocol;
- U4 has at least one cross-model comparison data point on the identical fixture;
- U5 has a result (or an explicit, reasoned decision to deprioritize it) distinguishing coupling from raw capability;
- all results are recorded as immutable, hash-verified, unscored diagnostic evidence and pass their own Completion Review and Independent Constitutional Review.

Reaching this bar authorizes handing evidence to Unit 3. It does not itself select a remedy; Unit 3 remains responsible for that, under its own governance, exactly as Unit 2's charter already required.

## 17. Strongest case against the recommendation

**This new unit duplicates evidence already available.** PF01 already shows genuine semantic failure with valid representation; a critic could argue any further Qwen-specific observation adds little beyond what is already known and accepted. Answer: the marginal value here is real and targeted, unlike the previously-rejected PF02–PF08-continuation proposal (which the Post-Stage-0 determination correctly rejected because it offered only breadth, on a dead campaign that could never combine with anything else). This unit is scoped precisely to the decision-critical gaps PF01 cannot fill — repeatability and cross-model separation — that no existing evidence addresses at all.

**This risks becoming an endless benchmarking exercise.** Real risk, and the reason Section 14 is closed to six bounded experiment classes with explicit exit criteria (Section 16), not an open-ended sweep, and explicitly excludes a larger-model chase.

**This improperly reopens Unit 2.** Guarded against structurally: new campaign identity, no touch to the preserved campaign, explicit non-statistical labeling, and this document does not reopen or qualify the `A — STOP` Post-Stage-0 determination, which stands as accepted.

**This prematurely optimizes for `qwen2.5-coder:7b`.** Guarded against by U4's cross-model comparison, which exists specifically to test whether the finding is model-specific before any Qwen-specific remedy work is even contemplated.

**This conflates representation reliability with semantic reliability.** Section 8 keeps these explicitly separate and states plainly that structured output has no representation defect to fix here — the strongest available discipline against this failure mode.

**This quietly selects a remedy before diagnosis.** No remedy is selected, recommended, or implemented anywhere in this document; Section 16's exit criteria explicitly gate remedy work behind a separate, later Unit 3 act.

**This requires model calls before governance authorization.** It does not — this document authorizes zero live calls; every experiment class in Section 14 requires its own future Scope Lock and execution approval before any call occurs.

**This expands Parker's constitutional architecture unnecessarily.** It does not — Section 13 deliberately reuses the existing Unit 1 harness, classification scheme, and artifact conventions rather than inventing new machinery, and follows the same governance-gate sequence Unit 2 already used.

**Conclusion: the recommendation survives this challenge**, conditioned on the new unit staying within the six bounded experiment classes and explicit exit criteria defined above, and on it never being framed as, or allowed to drift into, a resumption of Unit 2.

## 18. Final determination

```text
B — A NARROWLY SCOPED NEW UNIT WITHIN THE EXISTING REASONING PROTOCOL
    LIVE-MODEL CONFORMANCE AND STRUCTURED-OUTPUT RELIABILITY PROGRAMME
```

Not A: a wholly separate new programme would duplicate governance and harness machinery this programme already provides for no benefit. Not C: immediate remedy design is premature — no decision-critical uncertainty in Section 6 is yet resolved. Not D: no other path is indicated by the evidence.

A new live-model campaign **is** recommended in principle — but only as future, separately authorized diagnostic execution under a new Scope Lock, never as a resumption of the failed campaign and never authorized by this document.

## 19. Exact next authorized step

Drafting a Scope Lock for the proposed diagnostic unit, scoped to Section 14's six experiment classes, is the appropriate next governance artifact — not an implementation plan, since no implementation surface can be defined before the diagnostic unit's fixture/model/sample-size decisions are themselves fixed and independently reviewed. That Scope Lock is not authorized, drafted, or initiated by this document. No other action is authorized or compelled by this review.

## 20. Explicit prohibited actions

The following remain prohibited without further, separate, explicit governance:
- any HTTP call to `/api/generate`, `/api/tags`, or `/api/show`;
- resuming, altering, copying, normalizing, or deleting any part of the existing `qwen25coder7b-baseline-20260809` campaign;
- running PF02–PF08 against that campaign;
- running Stage 1 or Stage 2 against that campaign;
- creating any new live-model campaign;
- modifying production code, tests, Gradle configuration, or the reasoning prompt;
- implementing structured output, retries, repair, fallback, or model-routing behavior;
- selecting or implementing a remedy;
- beginning Unit 3 remedy work.

Corrective action: **NONE.** This document authorizes no implementation, no live execution, and no campaign of any kind.
