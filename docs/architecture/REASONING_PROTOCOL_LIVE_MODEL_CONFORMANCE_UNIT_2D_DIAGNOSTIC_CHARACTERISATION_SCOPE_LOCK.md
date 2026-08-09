**Status:** Unit 2-D Diagnostic Characterisation Scope Lock — **PROPOSED, PENDING ITS OWN INDEPENDENT CONSTITUTIONAL REVIEW.** Governance only against committed baseline `fd7f221`. No implementation or live execution is authorized before an accepted Independent Constitutional Review of this Scope Lock, a subsequent Implementation/Execution Plan and its own Independent Constitutional Review, an Implementation Readiness Review, and explicit execution approval.

# Reasoning Protocol Live-Model Conformance — Unit 2-D Diagnostic Characterisation Scope Lock

## Path note

Programme convention places Scope Locks for this specific track under `docs/architecture/` (`REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_1_EVALUATION_HARNESS_SCOPE_LOCK.md`, `..._UNIT_2_BASELINE_CHARACTERISATION_SCOPE_LOCK.md`), not `docs/governance/`, which is used by the unrelated Programme 3 track. This document follows the established convention for this programme rather than the general `docs/governance/` suggestion.

## 1. Status

Proposed governance. Nothing in this document authorizes implementation, a live model call, or campaign creation. It exists to freeze the diagnostic unit's boundary before any of that work begins, exactly as the Unit 2 Scope Lock preceded its own Implementation/Execution Plan.

## 2. Authority

Authorized by the accepted `REASONING_PROTOCOL_POST_UNIT_2_DIAGNOSTIC_PLANNING_REVIEW.md` determination **B** — a narrowly scoped new unit within the existing Reasoning Protocol Live-Model Conformance and Structured-Output Reliability programme. This Scope Lock implements that determination. It does not reopen, amend, or qualify: the Unit 2 Stage 0 Failure Review, the Unit 2 Stage 0 Failure Independent Constitutional Review, or `REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_POST_STAGE_0_FAILURE_GOVERNANCE_DETERMINATION.md`. Those remain accepted and final.

## 3. Relationship to Units 1 and 2

**Unit identity: Unit 2-D — Diagnostic Characterisation.** Not "Unit 3." Scope Lock §13 of the Unit 2 Baseline Characterisation Scope Lock already reserves the name "Unit 3—Reliability Contract and Remedy Selection" for the future remedy-selection unit; naming this unit Unit 3, or numbering it as if it were the sequential successor to Unit 2, would misrepresent it as remedy work or as Unit 2's completion. It is neither. The `2-D` designation signals: derived from Unit 2's Stage 0 aftermath, diagnostic (not statistical-baseline) in character, and explicitly not Unit 3.

This unit reuses, unchanged: Unit 1's `ConformanceFixture`, `SyntheticContextProfiles`, `TrialObservation`, `PrimaryClassification`, `ContentFidelity`, `EvaluationJsonLines`, and the production evaluation chain (`DefaultReasoningPromptBuilder` → `ModelReasoningProvider` → `LocalHttpModelInferenceClient` → `TaggedReasoningResponseParser`). It reuses, as reference material only (not as executable state), five already-frozen fixture texts and three already-frozen context profiles from the Unit 2 Scope Lock's corpus. It does not touch, extend, resume, or reinterpret the `qwen25coder7b-baseline-20260809` campaign. It sits between Unit 2 (closed, adverse, truncated at Stage 0) and Unit 3 (not started): it exists to produce the evidence Unit 3 will need, and nothing more.

## 4. Purpose

To determine, through a small number of controlled, pre-registered observations, which of the plausible causes of the PF01 semantic action-selection failure are actually supported by evidence — and which remain merely hypothetical — before any remedy is designed. This unit diagnoses. It does not select a remedy.

## 5. Diagnostic questions

Exactly six questions are authorized. Each is empirically answerable from a bounded, pre-registered set of observations, and each is separated from any remedy judgment.

- **DQ1 (repeatability).** Under the exact `R01-direct` fixture, `minimal-production-context`, and the pinned `qwen2.5-coder:7b` configuration, is the REMEMBER→non-REMEMBER miss reproduced consistently across repeated independent calls, or does it vary?
- **DQ2 (action-family breadth).** Under the frozen protocol and minimal context, do `P01-ordinary-fact` (REPLY), `P06-greeting` (REPLY), `G01-multistep` (GOAL), and `N01-heartbeat` (NOACTION) show any comparable single-attempt semantic miss, or does the observed weakness appear concentrated in the REMEMBER family?
- **DQ3 (context sensitivity).** For `R01-direct`, does semantic action selection differ under `mixed-full-production-like` or `conversation-history` context, relative to the `minimal-production-context` baseline?
- **DQ4 (model specificity).** On the identical fixture, prompt, and harness, does `llama3.2:3b` select REMEMBER where `qwen2.5-coder:7b` does not, or does it show the same miss?
- **DQ5 (decision/rendering coupling).** Does a decision-only variant of `R01-direct` — the model asked to select only the action category, without generating response content — change semantic accuracy relative to the joint decide-and-render task, under the pinned Qwen configuration?
- **DQ6 (representation independence).** Across all observations collected under DQ1–DQ5, does representation validity (a well-formed, parseable single tag) vary independently of semantic correctness, confirming they are distinct axes rather than co-occurring defects? (Answered by cross-cutting analysis of the same data; no separate fixture or call is needed.)

## 6. Explicit non-goals

This unit is **not** permitted to answer, and no observation collected under it may be used to answer:

- whether any specific remedy (structured output, prompt rewrite, retry, semantic repair, model replacement, ensembling, confidence thresholds) actually corrects the failure — that is Unit 3's question, not this unit's;
- a population-level failure rate or statistically confident reliability estimate for any action family — this unit's sample sizes are diagnostic, not statistical (Section 8);
- whether a larger model would perform better — no larger model is included, and size is not treated as a proxy for capability;
- which inference-configuration values (temperature, seed, sampling) "work best" — this unit records and pins configuration, it does not sweep it (Section 11);
- whether the current architecture should separate semantic decision from deterministic execution (dimension J from the diagnostic planning review) — this unit produces evidence relevant to that judgment; it does not itself resolve it.

## 7. Fixture classes

Five fixture classes are authorized, all reused verbatim from the Unit 2 Scope Lock's already-reviewed corpus (no new fixture text is invented):

| Fixture | Text | Expected | Resolves |
|---|---|---|---|
| `R01-direct` | `Remember that my synthetic test coffee mug is black.` | REMEMBER | DQ1, DQ3, DQ4, DQ5 — the fixture with an established anomaly |
| `P01-ordinary-fact` | `My synthetic desk token is amber.` | REPLY | DQ2 — the REMEMBER-adjacent false-positive check (must NOT become REMEMBER) |
| `P06-greeting` | `Hello Parker.` | REPLY | DQ2 — REPLY-family breadth |
| `G01-multistep` | `Create a three-step checklist for inspecting the synthetic blue test bench.` | GOAL | DQ2 — GOAL-family breadth |
| `N01-heartbeat` | `Synthetic system heartbeat event: no response and no action are required.` | NOACTION | DQ2 — NOACTION-family breadth |

Not authorized: the remaining eighteen Unit 2 fixtures, including the ambiguous-boundary cases (`P02-quoted-remember`, `P03-ambiguous-memory`, `P04-embedded-tags`, `P05-mixed-memory-discussion`, `P12-injection`). These test a materially different question (where exactly the REMEMBER boundary sits under adversarial or near-miss phrasing) that is not decision-critical to DQ1–DQ6. They remain a legitimate candidate for a later, separately-scoped follow-up if this unit's results warrant it; they are not included here merely because they exist.

## 8. Repeatability design

Repetition is authorized for exactly one cell: `R01-direct` × `minimal-production-context` × `qwen2.5-coder:7b`, **10 attempts**.

Justification for repeating at all: `defaultOllamaRequestBody` (`src/runtime/ModelInferenceClient.kt`) sets only `model`, `prompt`, and `stream:false` — no `temperature`, `seed`, `top_p`, or `top_k`. Both production and prior evaluation traffic therefore run under Ollama's unpinned default sampling, meaning run-to-run variation is architecturally possible, not merely a theoretical concern. A single observation (PF01) cannot distinguish near-deterministic semantic weakness from a one-off stochastic draw.

Justification for 10, not an arbitrary round number, and not Unit 2's 30-per-cell statistical design: this unit does not attempt statistical confidence (Section 6). Ten repeats is a proportionate triage sample — enough that a persistently uniform result (0 or 10 divergent outcomes) is a strong, actionable signal of near-determinism, and enough that a materially mixed result (for example, 3–7 divergent outcomes) is clearly distinguishable from noise at the level of "is this fixture's behavior stable or not" — while remaining two orders of magnitude below Unit 2's statistical scale, appropriate to a bounded diagnostic exercise rather than a re-run of the baseline campaign.

**Explicit limit on what repetition can and cannot show:** ten repeats can confidently detect near-deterministic behavior (persistently ~0% or ~100% divergence). It cannot rule out a moderate true failure rate — for example, a genuine 10–30% underlying divergence rate could plausibly present as 0/10 or 1/10 by chance. Any interpretation of DQ1's result must state this limitation explicitly (Section 21) and must not claim statistical confidence the sample size does not support.

The decision-only variant (DQ5) is separately repeated **5 times**, half of DQ1's count — proportionate to its status as a secondary, exploratory comparison rather than the unit's primary repeatability target, while still giving a comparison distribution rather than a single point. All other cells (DQ2, DQ3, DQ4) are single-attempt; repeating them was considered and rejected as disproportionate to their role as breadth/triage checks rather than repeatability targets (Section 15 states explicitly what a single-attempt result can and cannot support).

## 9. Model-comparison boundary

Authorized: one comparison, `R01-direct` × `minimal-production-context` × `llama3.2:3b` (stated to be already installed locally), single attempt.

**Decision relevance:** part of the decision-critical evidence set, not sufficient alone. Without any cross-model data point, Unit 3 cannot begin to separate "the model is the constraint" from "the protocol/prompt is the constraint" as remedy-family candidates — a distinction that materially changes what kind of remedy work would even be worth attempting. A single data point does not resolve this alone; it must be read jointly with DQ1–DQ3.

**What a difference would and would not support:** if Llama selects REMEMBER where Qwen does not, that supports the miss being at least partly model-specific rather than an inevitable consequence of the GOAL/REPLY/REMEMBER/NOACTION protocol design — it does **not** prove Llama is more reliable generally (Llama gets exactly one attempt too), does not establish which model Parker should deploy, and does not rule out that Qwen might also succeed on a different attempt (must be read against DQ1). If both fail identically, that supports the protocol/prompt design being at least a partial contributor — it does **not** prove the protocol is *the* cause, since `llama3.2:3b` and `qwen2.5-coder:7b` share other traits (both small, both local) that a shared failure could equally reflect.

**Explicit limitation:** `llama3.2:3b` (3B parameters, general-purpose) and `qwen2.5-coder:7b` (7B parameters, code-specialized) differ in both size and specialization simultaneously. A result cannot cleanly isolate "code specialization" as the explanatory variable; both possible confounds must be carried into any interpretation. No larger model is included. No model is downloaded or installed by this Scope Lock or by any document it authorizes. Model size is never to be read as a proxy for expected correctness.

## 10. Context controls

Authorized: `R01-direct` under two additional Unit 1 context profiles beyond `minimal-production-context`, single attempt each — no other fixture is crossed with these profiles.

- `mixed-full-production-like` — the richest existing profile; the same pairing the original Unit 2 Stage 0 schedule used for `PF02`, so no new context design is invented.
- `conversation-history` — a qualitatively different kind of contextual influence (prior-turn framing/recency) than `mixed-full-production-like`'s static enrichment, and cheap to include.

The remaining six Unit 1 profiles are not authorized here; two profiles, one fixture, no crossing with other fixture classes or with `llama3.2:3b`, is the deliberate limit to prevent the combinatorial growth a wider context sweep would create (Section 21, Point 6).

**Context sensitivity versus random variation:** because each context cell is a single attempt, a difference from the minimal-context baseline cannot, by itself, be attributed to context — it must be read jointly with DQ1's repeatability distribution. If DQ1 shows high variability, a single differing context-profile result carries little independent weight; only if DQ1 shows near-deterministic behavior does a differing single-attempt context result become a meaningful signal.

## 11. Inference controls

Current production/evaluation request format, confirmed by direct inspection of `defaultOllamaRequestBody`, is `{"model", "prompt", "stream": false}` only — no sampling parameters are set anywhere in the production or Unit 1/2 evaluation path. This unit **pins that exact request shape unchanged** for every DQ1–DQ4 cell, to remain a valid characterization of the actually-deployed behavior; no temperature, seed, `top_p`, `top_k`, `format`, or grammar constraint may be added to those cells.

The absence of pinned sampling parameters is itself diagnostically relevant: any variation observed under DQ1 is confounded with whatever Ollama's actual default sampling behavior is (version-dependent, not independently documented here). This unit does not attempt to resolve that confound by changing configuration (which would stop characterizing the deployed protocol) — it instead requires the evidence artifacts to record whatever runtime/version identity is discoverable through the existing operator preflight checks (`ollama show`, already an authorized read-only identity check under Unit 2's own runbook convention) so the confound is documented, not hidden. No inference-configuration sweep (dimension G) is authorized; it is determined not decision-relevant at this stage, since no evidence yet suggests configuration, rather than the model or protocol, is the operative variable.

## 12. Semantic correctness measurement

**Semantic correctness** = `actualAction == expectedAction`, exactly as Unit 1's `PrimaryClassification` already computes it (classification `D` precisely marks its failure). Recorded independently of representation validity and content fidelity. No code change is required; the existing type is reused unchanged.

## 13. Representation correctness measurement

**Representation correctness** = `representationValid`, exactly as Unit 1 already computes it (a single, well-formed, parseable tag was produced — regardless of which action was chosen or how faithful the content was). PF01 already demonstrates why this must never collapse into semantic correctness: representation was valid while the action was wrong.

## 14. Content-fidelity measurement

Where `fixture.expectedContent` is defined, content fidelity is recorded using Unit 1's existing `ContentFidelity` enum (`EXACT`, `DEVIATION_OR_PARAPHRASE`, `NOT_APPLICABLE`, `INDETERMINATE`) exactly as already computed. This is the coarse, automatic, per-trial field Unit 1 already produces — not the Unit 2 Scope Lock's separate seven-category blinded human-review worksheet, which is a scored-campaign artifact this diagnostic unit does not produce or need.

These three measurements (Sections 12–14) must always be reported as three separate fields per observation, never collapsed into one pass/fail outcome.

## 15. Structured-output boundary

**Determination: C — nowhere yet.** Not inside this diagnostic unit, and not pre-placed into a defined future remedy-comparison unit either (no such unit is created by this document).

Reasoning: structured/schema-constrained output is fundamentally a representation-axis intervention — it constrains the syntactic form of legal completions. PF01's accepted determination is that representation was **not** the problem; the model already produced a single, valid, cleanly parseable tag and still chose the wrong action. Including a structured-output experiment inside a unit whose own accepted evidence shows representation is not currently broken risks exactly the error this Scope Lock must guard against: a syntactically valid but semantically wrong output emerging from a schema-constrained trial being misread as a success. Structured output is itself a candidate remedy (Section 23); testing it here, even informally, would already be a first step into remedy-space evaluation, which this unit's charter excludes. If it is ever evaluated, that belongs strictly to a properly, separately governed remedy-comparison unit under Unit 3, evaluated against the semantic-accuracy baseline this diagnostic unit establishes — not to this one.

## 16. Prompt-variant boundary

Two tracks, kept visibly separate in every artifact:

1. **Characterization of the current production prompt.** DQ1, DQ2, DQ3, and DQ4 all use the byte-identical output of `DefaultReasoningPromptBuilder`, unmodified, exactly as Unit 2 characterized it.
2. **One authorized candidate-protocol variant.** DQ5's decision-only variant is the single prompt variant this unit may run. It must be permanently labeled non-production, stored under a visibly separate artifact path from track 1, and never described as measuring "the Parker protocol's" current reliability — it measures a hypothesis about coupling, nothing more.

No other prompt variant (reordered guidance, more emphatic wording, few-shot examples, alternative phrasings of the REMEMBER rule) is authorized in this unit. Open-ended prompt engineering remains deferred to a later, separately governed remedy-comparison unit, exactly as structured output is deferred in Section 15.

## 17. Campaign isolation

Any future execution of this Scope Lock must, at minimum:

- use a new campaign identity that cannot collide with or be mistaken for `qwen25coder7b-baseline-20260809` — the identity must contain an explicit `diagnostic` marker and must never be described as a Unit 2 resumption;
- write to a new artifact root, never inside `/var/lib/parker/reasoning-protocol-live-model/qwen25coder7b-baseline-20260809/`;
- pin: repository commit; both model names and digests (`qwen2.5-coder:7b`, `llama3.2:3b`); sanitized endpoint identity; Ubuntu/runtime identity; Parker/Ollama container image identity where applicable; the `90,000 ms` evaluation timeout already established as Unit 2's evaluation-timeout convention;
- record, per observation: exact prompt/input (track 1 or track 2, explicitly labeled), raw model envelope, extracted response, parser result, semantic classification, representation classification, content fidelity where applicable, and latency/transport metadata;
- reuse Unit 1's `TrialObservation`/`EvaluationJsonLines` shapes unchanged — no new serialization format.

This Scope Lock does not create that campaign identity, does not write to any artifact root, and does not authorize execution.

## 18. Campaign-size rationale

| Component | Cells | Attempts/cell | Calls |
|---|---:|---:|---:|
| DQ1 repeatability (`R01-direct`, minimal, Qwen) | 1 | 10 | 10 |
| DQ2 breadth (P01, P06, G01, N01 × minimal, Qwen) | 4 | 1 | 4 |
| DQ3 context (`R01-direct` × 2 profiles, Qwen) | 2 | 1 | 2 |
| DQ4 model comparison (`R01-direct`, minimal, Llama) | 1 | 1 | 1 |
| DQ5 decision-only variant (`R01-direct`, minimal, Qwen) | 1 | 5 | 5 |
| Warm-up (one per model, connectivity/parsing sanity) | 2 | 1 | 2 |
| **Total** | | | **24** |

Twenty-four live calls, not inherited from Unit 2's 3,900/3,911-trial design — each row traces to exactly one of the six frozen diagnostic questions (Section 5), sized by the reasoning in Sections 8–10. This is a minimum, not a target; nothing in this table is included because a category exists in Unit 2's corpus and could be reused cheaply.

## 19. Evidence/artifact requirements

Minimum immutable evidence for any future execution, mirroring Unit 2's conventions:

- `campaign-definition` — the frozen cell schedule from Section 18 plus fixture/profile hashes;
- `campaign-identity` — commit, both model name/digest pairs, endpoint, runtime, timeout;
- intent record — planned cell IDs before execution;
- raw observations — one `TrialObservation` per call via `EvaluationJsonLines`, append-and-flush per call;
- checkpoint — append-only completed-ID ledger;
- manifest — hash/size/line-count of the raw artifact, sealed on completion or on a hard stop (Section 20);
- an interpretation worksheet applying Section 21's pre-registered rules to the actual results, traceable back to specific raw records — new to this unit, since Unit 2's Stage 0 was never meant to support interpretation, only pass/fail;
- an artifact hash inventory (SHA-256/size/line-count of every file).

**Authoritative:** `campaign-definition`, `campaign-identity`, raw observations, checkpoint, and manifest are the byte-level ground truth. The interpretation worksheet is derived commentary, clearly labeled as interpretation and always traceable to specific raw records — it is evidence of reasoning applied to the data, not itself primary evidence.

## 20. Stop conditions

**Hard stop (halts the entire diagnostic campaign immediately, mirroring Unit 1/2's fail-closed precedent):**
- any measurement/harness defect (capture failure, parser misbehaving outside its documented contract, evidence loss);
- any repository commit, model/digest, endpoint, timeout, or runtime-identity mismatch;
- any artifact-integrity failure (hash mismatch, corrupted or duplicate record, failed append/flush);
- any unexpected consequential action — if a parsed `Remember` or `Goal` were ever observed reaching a live downstream Memory/Goal execution path (architecturally should be impossible given the harness terminates at `TrialObservation`, exactly as verified for the Unit 2 harness) — this is a safety boundary, not a diagnostic outcome, and halts regardless of diagnostic purpose.

**Not a stop condition — recorded as data, campaign continues on schedule:**
- a semantic failure (classification `D`) on any fixture, including every one of DQ1's ten repeats coming back divergent. This is what the unit exists to observe; a semantic-failure auto-halt would make DQ1 and DQ2 impossible to answer.
- an unexpected representation failure (malformed/untagged/timeout/transport) on a single trial — recorded exactly as Unit 2's Implementation/Execution Plan already treats such cases ("timeout, malformed output, wrong action, and transport failure remain completed observations"), and the campaign proceeds to the next scheduled cell.

**Explicit distinction from Unit 2's Stage 0 gate:** Unit 2's Stage 0 halts on the first non-A/B observation because it exists to protect entry into an expensive, 3,900-trial statistical commitment whose validity depends on a proven instrument — halting immediately is proportionate there because the downstream cost of proceeding on a broken instrument is large, and the entire Stage 0 purpose is a pass/fail gate, not a characterization exercise. This unit carries no such downstream statistical commitment; it is small, bounded, and its entire purpose is characterizing failure, including repeated failure. Halting on the first semantic miss here would be self-defeating — it would terminate data collection on exactly the phenomenon under study. Only threats to evidence validity (harness/identity/configuration/artifact-integrity defects) or a genuine safety-boundary violation warrant halting; the diagnostic outcome itself never does.

**No early truncation, no mid-campaign expansion:** the schedule in Section 18 runs to completion or to a hard stop. Emerging results (for example, DQ1's first few repeats all agreeing) do not authorize stopping early or adding cells — pre-registration discipline is preserved to prevent post-hoc storytelling (Section 21).

## 21. Interpretation rules

Pre-registered before any observation exists. Each pairs what a result would support with what it explicitly would not prove.

1. **DQ1 uniformly divergent (0 or 10 of 10 match the original PF01 outcome).** Supports near-deterministic, reliable semantic weakness for this exact fixture/config (strengthens hypotheses A/B over F). Does not prove REMEMBER fails in general, does not establish a cause, and — per Section 8 — cannot rule out a moderate true divergence rate that happened to land at an extreme by chance.
2. **DQ1 mixed (a materially split result, e.g. 3–7 of 10).** Supports stochastic variation (hypothesis C) being a real, material contributor for this fixture. Does not establish a specific failure rate (ten repeats is not statistically sized for that) and does not imply the true split is near 50/50.
3. **Llama succeeds where Qwen fails (DQ4).** Supports the miss being at least partly model-specific, weakening pure protocol-design explanations (A/H) as the sole cause. Does not prove Llama is generally more reliable, does not establish which model Parker should deploy, and must be read jointly with DQ1 (Qwen might also succeed on a different attempt).
4. **Both models fail identically (DQ4).** Supports the protocol/prompt design being at least a partial contributor. Does not prove the protocol is *the* cause — both models are small and local, a confound Section 9 already states explicitly.
5. **Representation failures occur independently of which trials are semantically wrong (DQ6).** Supports the Unit 1/2-established principle that representation and semantic reliability are distinct axes requiring separate reporting. Does not establish a representation-failure rate and does not imply representation problems are a significant driver of overall unreliability.
6. **DQ5's decision-only variant shows materially different accuracy from the DQ1 joint-task baseline.** Supports coupling between decision-making and response-generation (hypothesis I/D) being a real contributor. Does not establish the decision-only framing as a viable production design (it was never evaluated for content generation or any other production requirement) and is not to be read as "the fix." This finding is also Qwen-only (Section 9's DQ5 scope) and must not be assumed to generalize to other models without separate testing.
7. **DQ3 shows a different single-attempt result under a richer context profile.** Supports, weakly, that context may be a contributing factor — only if read jointly with DQ1's variability; a single differing result cannot, by itself, be attributed to context (Section 10).

No conclusion drawn under any of these rules may be extended into a remedy recommendation (Section 23).

## 22. Exit criteria

The diagnostic unit is sufficient to close and permit a separate Remedy Selection Review once:

- DQ1 through DQ5 each have a recorded result (whichever way each comes out — no particular finding is required to exit);
- DQ6's cross-cutting representation-independence analysis is complete over all collected observations;
- every artifact passes hash/integrity verification;
- an Independent Constitutional Review confirms the campaign followed this Scope Lock exactly — no scope creep, no silent prompt/config change, no early truncation or mid-campaign expansion;
- the pre-registered interpretation worksheet (Section 21) has been applied to the actual results and is traceable to specific raw records.

This bar requires completeness and integrity of the pre-registered evidence set, not any particular outcome, and does not require statistical certainty. Its purpose is to leave Unit 3 with a remedy-family triage map — informed judgment about which candidate remedy families (Section 23) are even worth investigating — not a remedy choice itself. If, at exit, the evidence turns out too thin to support that triage (for example, a genuinely ambiguous DQ1 result that resolves nothing), that insufficiency is itself a valid, recordable finding; it does not authorize silently lowering this bar or expanding the campaign after the fact.

## 23. Remedy firewall

This unit may not select, recommend, prototype, or implement any of the following. They remain candidate categories for a future, separately governed Unit 3 only:

structured/schema-constrained output; prompt rewriting; deterministic intent routing; retry; semantic repair; fallback; model replacement; a larger model; ensembling; confidence thresholds; parser relaxation; protocol weakening.

Nothing in Sections 5–22 constitutes a recommendation of any of the above, regardless of what any future result under this Scope Lock shows.

## 24. Prohibited actions

Prohibited under this Scope Lock and by any document it authorizes, until further, separate, explicit governance:

- any HTTP call to `/api/generate`, `/api/tags`, or `/api/show`;
- any Ollama HTTP call of any kind;
- resuming, altering, copying, normalizing, or deleting any part of `qwen25coder7b-baseline-20260809`;
- running PF02–PF08, Stage 1, or Stage 2 against that campaign;
- creating the diagnostic campaign identity described in Section 17;
- downloading or installing any model;
- modifying production code, test code, Gradle configuration, the reasoning prompt, the parser, or the inference client;
- implementing structured output, retries, repair, fallback, or model-routing behaviour;
- selecting or implementing any item in Section 23;
- staging, committing, or pushing anything.

## 25. Governance required before execution

In sequence, each independently reviewed before the next begins, exactly mirroring Unit 2's own gate sequence: (1) Independent Constitutional Review of this Scope Lock; (2) an Implementation/Execution Plan, scoped only to what Section 18's twenty-four-call schedule actually requires, and its own Independent Constitutional Review; (3) an Implementation Readiness Review and its Independent Constitutional Review, conducted against the actual implementation diff, exactly as Unit 2's was; (4) explicit execution approval. No live call may occur before all four steps are accepted.

## 26. Disposition

```text
PROPOSED
```

Implementation and live execution remain prohibited pending the governance sequence in Section 25.
