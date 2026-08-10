**Status:** Unit 3-B — Remedy Experiment Scoping — Planning Review — **PASS.** Planning/governance only, against committed baseline `ab27f18`. No implementation, live call, experiment, or remedy selection occurred. This document plans what a future Unit 3-B Scope Lock should contain; it is not that Scope Lock and does not freeze anything.

# Reasoning Protocol Live-Model Conformance Unit 3-B — Remedy Experiment Scoping — Planning Review

## 1. Status

Planning only. Recommends whether and how to proceed to a Unit 3-B Scope Lock. Authorizes nothing beyond planning.

## 2. Baseline

HEAD independently confirmed `ab27f188d7a3d2664806989bb4f15e132ef57cef`, equal to `origin/main`, working tree clean.

## 3. Authority

Unit 3-B's existence and boundary trace to: the top-level programme Planning Review (`05d4c2a`, naming Unit 3 "Reliability Contract and Remedy Selection" and reserving implementation for a separate Unit 4); the Unit 3 Planning Review (`7e9e388`) and its Independent Planning Review, which proposed the five-part internal structure (3-A Contract → 3-B Experiment Scope Lock(s) → 3-C Controlled Experiments → 3-D Comparative Evaluation → 3-E Remedy Selection) and named this exact structure as the mechanism preventing an experimental candidate's implementation from silently becoming the selected architecture; and the now-frozen Unit 3-A Reliability Contract Definition Scope Lock (`ab27f18`) and its two accepted constitutional reviews, which this document treats as the controlling contract every proposed experiment must ultimately be judged against.

## 4. Evidence reviewed

Read fresh: the programme-level Planning Review and its Independent Constitutional Review; the full Unit 2 and Unit 2-D governance and evidence chain (Scope Locks, Stage 0 Failure Review and its Independent Constitutional Review, Post-Stage-0 Governance Determination, Post-Unit-2 Diagnostic Planning Review, Execution Evidence Review, Interpretation and Closure Review and its Independent Constitutional Review); the Unit 3 Planning Review and its Independent Planning Review; the frozen Unit 3-A Scope Lock together with its original and correction Independent Constitutional Reviews; and, read-only, the current `ReasoningProviderResponse`, `TaggedReasoningResponseParser`, `DefaultReasoningPromptBuilder`, and `ModelReasoningProvider` (confirmed unchanged since long before this programme of work began).

## 5. Purpose of Unit 3-B

Unit 3-B determines *which* remedy-experiment families warrant later controlled evaluation, and freezes the *experiment architecture* (fixtures, controls, measurement, isolation, comparison rules) any such evaluation must use — against the frozen Unit 3-A contract, without selecting or implementing any remedy. It is governance about experiments, not the experiments themselves (reserved for Unit 3-C), not their interpretation (Unit 3-D), and not remedy selection (Unit 3-E).

## 6. Candidate remedy-family analysis

Each family reuses the Unit 3 Planning Review's own evidence base, re-examined here against the specific dimensions this task requires: contract-dimension impact, semantic/representation/both, new false-positive risk, isolability, and comparison meaningfulness.

### A. Decision/rendering separation
**Evidence basis:** DQ5 — 0/18 pooled joint-task vs. 2/5 decision-only correct `REMEMBER`. **Constitutional relevance:** directly tests where, in the current single-call design, unreliability originates. **Contract dimensions affected:** #1 (semantic action-selection), #2 (REMEMBER true-positive), #4 (GOAL true-positive, untested), #8 (representation validity of the decision step itself). **Failure axis:** primarily semantic; both axes operationally, since any two-call design has its own representation-validity requirement per call. **Material risks:** relocates rather than eliminates model semantic authority; a two-call flow conflicts with `ModelReasoningProvider`'s documented single-call, no-retry, no-repair invariant, requiring fresh architectural governance. **New false-positive modes:** a decision-only classifier's own boundary-case behavior on negative/adversarial fixtures is entirely untested — DQ5 only exercised the one positive fixture. **Isolable:** yes, cheaply, exactly as Unit 2-D's own DQ5 already demonstrated without touching production code. **Meaningful comparison:** yes, but only if measured against the *full* contract (semantic accuracy, false-positive rate, representation validity, content fidelity), not DQ5's single positive metric alone. **Determination: INCLUDED FOR EXPERIMENTAL SCOPING.**

### B. Prompt/protocol redesign
**Evidence basis:** the current prompt already contains an explicit worked example matching the failing fixture and still misses; `DefaultReasoningPromptBuilder`'s own history records one prior successful revision for a different symptom. **Constitutional relevance:** tests whether instruction clarity, not model capability, drives the miss. **Contract dimensions affected:** potentially all of #1–#7 simultaneously — this is the family with the broadest possible blast radius, which is itself the central risk. **Failure axis:** primarily semantic; secondarily representation, if wording changes inadvertently affect tag-format compliance. **Material risks:** regression across currently-correct actions; requires touching `DefaultReasoningPromptBuilder`, forbidden without fresh governance; an already-governed stop rule exists (two evidence-led revisions failing qualification gates). **New false-positive modes:** real and concrete — a revision emphasizing REMEMBER recognition could plausibly shift the model toward over-triggering REMEMBER/GOAL on the corpus's own adversarial fixtures (`P02`, `P03`, `P12`), the opposite failure direction from what is currently observed. **Isolable:** yes, via held-out fixture evaluation. **Meaningful comparison:** yes, against the full frozen corpus, not `R01-direct` alone. **Determination: INCLUDED FOR EXPERIMENTAL SCOPING**, with a mandatory full-corpus (not single-fixture) evaluation requirement given the breadth of its potential impact.

### C. Deterministic/rule-assisted handling of explicit cases
**Evidence basis:** DQ1's high (9/10) consistency on a fixture chosen specifically for its unambiguity. **Constitutional relevance:** the only family testing whether classification authority may ever validly sit outside the model at all — a first-of-kind architectural question for this programme. **Contract dimensions affected:** #1, #2, #3 (REMEMBER false-positive — the dimension most exposed by this family specifically), #11 (ambiguity handling), #16 (fail-closed), #17 (downstream isolation, if implemented as any kind of bypass path). **Failure axis:** semantic, plus a meta-level architectural question about where semantic authority resides. **Material risks:** highest of any family under consideration — brittle keyword matching, false positives on the corpus's own quoted/hypothetical/discussion negative fixtures, language variation far exceeding any fixed pattern, contextual meaning no rule can weigh, improperly bypassing model reasoning, and silent divergence between a deterministic and a model path. **New false-positive modes:** the most direct and severe of any family — this is precisely the risk profile a brittle rule creates. **Isolable:** yes, but only responsibly if any experiment design tests the adversarial/false-positive surface *first*, not the easy explicit-case win. **Meaningful comparison:** yes, but the decisive metric is false-positive rate on negative and adversarial fixtures, not raw accuracy on the case the rule was designed to catch. **Determination: INCLUDED FOR EXPERIMENTAL SCOPING**, conditioned explicitly on adversarial-fixture-first experiment design — this condition is not optional and must be written into any Unit 3-B Scope Lock as a binding constraint, not a suggestion.

### D. Structured/schema-constrained output
**Evidence basis:** none direct — explicitly untested by Unit 2-D's own design. **Constitutional relevance:** targets representation validity specifically. **Contract dimensions affected:** #8 primarily; #1 only speculatively (untested whether constrained decoding shifts semantic tendency at all). **Failure axis:** representation, explicitly not semantic per Unit 3-A Section 6/8's own distinction. **Material risks:** "laundering" a wrong decision behind valid syntax (already guarded against at the acceptance-criteria level, not the mechanism level); touches `LocalHttpModelInferenceClient`, forbidden without fresh governance. **New false-positive modes:** a schema forcing one of four actions removes the malformed-output escape hatch, which could make false-positive events *more visible* as a fraction of representation-valid output rather than more frequent in absolute terms — a measurement nuance, not a straightforward new risk. **Isolable:** yes. **Meaningful comparison:** only on representation-validity grounds; comparing it on semantic-accuracy grounds is not meaningful without first establishing whether it affects that axis at all. **Determination: DEFERRED** — not contraindicated, but representation validity is already high in observed evidence (22/24 in Unit 2-D), so this family's marginal value is lower than A/B/C, which target the dominant, still-unresolved semantic failure mode.

### E. Inference-control changes
**Evidence basis:** none — untested by every Scope Lock to date; DQ1's own 9/10 consistency is a weak prior against high stochasticity being the dominant driver. **Constitutional relevance:** targets reproducibility (#21). **Contract dimensions affected:** #21 primarily, #1 speculatively. **Failure axis:** unknown, genuinely untested in either direction. **Material risks:** any change invalidates prior characterisation, which measured only default settings; touches production request construction. **New false-positive modes:** unknown. **Isolable:** yes, cleanly, as a single-variable change. **Meaningful comparison:** yes, if isolated. **Determination: DEFERRED** — genuinely open, but lower priority than A/B/C given the weak prior against stochasticity being dominant.

### F. Model/provider substitution
**Evidence basis:** DQ4 — one doubly-confounded (size and specialization) data point; the tested alternative also missed. **Constitutional relevance:** tests model/provider qualification (#22) directly. **Contract dimensions affected:** #22 and, by extension, all of #1–#9 under a full qualification run. **Failure axis:** both, since qualification re-measures everything. **Material risks:** none architecturally (qualification is already fully governed by Unit 3-A Section 11), but cost is disproportionate to current evidence — a full blind-corpus qualification run is comparable in scale to Unit 2's own multi-thousand-trial design, and cannot be meaningfully approximated by a smaller experiment, since the already-governed qualification process itself requires full-corpus, per-stratum evaluation. **New false-positive modes:** unknown until tested at qualification scale. **Isolable:** only at full qualification scale — a small comparison, like DQ4 itself, is explicitly insufficient by existing governance. **Meaningful comparison:** only at that scale. **Determination: EXCLUDED ON CURRENT EVIDENCE**, specifically as a near-term Unit 3-C candidate — not forbidden permanently, but the evidence bar for even beginning this experiment is far higher than for A/B/C/D/E, and current evidence gives no affirmative reason to prioritize that cost now.

### G. Retry or semantic repair
**Evidence basis:** DQ1's 9/10 consistency argues against simple retry being effective for semantic misses; no positive evidence for either sub-mechanism. **Constitutional relevance:** directly implicates the already-frozen escalation-prevention requirement (Unit 3-A Section 4). **Contract dimensions affected:** #1, #16, and #3/#5 if retry or repair ever touches a consequential action. **Failure axis:** representation-only retry — representation axis, narrow, low risk; semantic retry/repair — semantic axis, high risk. **Material risks:** semantic retry/repair already requires new, separately-governed authority per existing programme governance; repair specifically risks silently overriding a decision, reducing auditability. **New false-positive modes:** high risk specifically for semantic retry/repair — repeated sampling until a "better-looking" answer appears is exactly the practice that could inflate false-positive rates if not tightly bounded; low risk for representation-only retry. **Isolable:** representation-only retry — yes, narrowly; semantic retry/repair — not safely, without first resolving whether any validation signal can detect a wrong decision without already knowing ground truth, an unresolved prerequisite question, not yet even framed as investigable. **Meaningful comparison:** representation-only retry — yes, narrow; semantic retry/repair — not yet. **Determination:** representation-failure-only retry — **DEFERRED** (architecturally cheap and low-risk, but zero representation-failure-triggered scenarios occurred in Unit 2-D, so there is no observed problem motivating it yet); semantic retry/semantic repair — **EXCLUDED ON CURRENT EVIDENCE**, both by DQ1's own evidence against effectiveness and by existing governance's escalation-risk and no-model-as-judge principles.

### H. Multi-model or fallback strategies
**Evidence basis:** none tested; DQ4's shared miss is, if anything, weak evidence against simple two-model voting being obviously useful here. **Constitutional relevance:** would require an entirely new decision layer. **Contract dimensions affected:** potentially all, in a currently unspecified way. **Failure axis:** both, undefined. **Material risks:** substantial new complexity; a disagreement-resolution mechanism becomes a new semantic authority, echoing families C and G's concerns; "model/provider independence" becomes materially harder to reason about. **New false-positive modes:** unknown, entirely undesigned. **Isolable:** not yet — no design exists to isolate. **Meaningful comparison:** not yet possible. **Determination: EXCLUDED ON CURRENT EVIDENCE** — no prior governance consideration, no design, complexity disproportionate to any evidence motivating it.

### I. Hybrid approaches
**Evidence basis:** indirect only, by combining families A and C's individual signals; no hybrid itself tested. **Constitutional relevance/contract dimensions/failure axis:** inherits whichever components are combined. **Material risks:** compounds constituent risks; harder attribution of which component drives any observed effect. **New false-positive modes:** inherits and potentially compounds constituent risks. **Isolable:** only once constituents are independently understood. **Meaningful comparison:** premature. **Determination: DEFERRED** — a reasonable eventual direction, premature ahead of its constituent families' own results.

**Summary:** three families (A, B, C) warrant experimental scoping; four (D, E, representation-only-retry from G, I) are deferred as plausible-but-lower-priority or evidence-incomplete; three (F, semantic-retry/repair from G, H) are excluded on current evidence as near-term candidates, none permanently foreclosed. This is a differentiated classification, not a blanket inclusion of every family named.

## 7. Frozen Unit 3-A contract traceability

| Contract dimension (Unit 3-A §5) | A | B | C | D | E |
|---|---|---|---|---|---|
| #1 Semantic action-selection | direct target | direct target | direct target | not addressed | speculative |
| #2 REMEMBER true-positive | direct target | direct target | direct target | not addressed | speculative |
| #3 REMEMBER false-positive | untested by DQ5 | real regression risk | highest risk of any family | not addressed | untested |
| #4 GOAL true-positive | untested | possible target | possible target | not addressed | speculative |
| #5 GOAL false-positive | untested | real regression risk | high risk | not addressed | untested |
| #6 REPLY behaviour | untested | regression risk | regression risk | not addressed | untested |
| #7 NOACTION behaviour | untested | regression risk | regression risk | not addressed | untested |
| #8 Representation validity | secondary (per-call) | secondary | secondary | direct target | untested |
| #9 Content fidelity | untested | possible regression | possible regression | not addressed | untested |
| #16 Fail-closed behaviour | must preserve | must preserve | highest scrutiny required | must preserve | must preserve |
| #21 Reproducibility | untested | untested | untested | untested | direct target |
| #22 Model/provider qualification | n/a | n/a | n/a | n/a | n/a |

No current evidence — from any family — addresses dimensions #14 (unavailable provider), #15's fail-vs-warn choice, #12's timeout value, or #11's clarification-seeking question; all remain exactly as unresolved as Unit 3-A left them. No numerical threshold is invented here to fill these gaps; they remain open governance questions for whichever unit eventually resolves them.

## 8. Proposed controlled-experiment architecture (for a future Unit 3-B Scope Lock to freeze)

1. **Control/baseline:** every experiment must include a control arm using the exact, unmodified production path, run under identical fixtures/context/model/commit as the candidate arm — mirroring exactly how Unit 2-D's DQ1 (production) and DQ5 (candidate) were structured.
2. **Fixture reuse vs. new fixtures:** reuse Unit 2's frozen 23-fixture corpus as the primary basis; new fixtures (particularly for family C's adversarial surface) require their own governance review before use.
3. **Explicit REMEMBER cases:** all three (`R01`, `R02`, `R03`), not `R01-direct` alone — a real gap Unit 2-D left open.
4. **Ordinary-fact false-positive controls:** `P01` plus the corpus's quoted/hypothetical/ambiguous negative fixtures (`P02`, `P03`), mandatory for family C.
5. **GOAL false-positive controls:** the corpus's negative-GOAL fixtures (discussion-vs-instruction distinctions), not yet exercised by Unit 2-D at all.
6. **REPLY behaviour:** multiple REPLY fixtures as regression baseline, not `P06` alone.
7. **NOACTION behaviour:** `N01`/`N02`, given Unit 2-D's own representation-failure observation on `N01` warrants re-examination under any candidate mechanism.
8. **Ambiguous-boundary cases:** `P02`/`P03`/`P04`/`P05`/`P12`-class fixtures, mandatory for family C, valuable for A and B.
9. **Content fidelity:** measured per Unit 3-A Section 8, unchanged, not re-defined here.
10. **Representation validity:** measured per Unit 3-A Section 6/8, unchanged.
11. **Semantic action selection:** measured independently of representation, per Unit 3-A Section 6, unchanged.
12. **Material mutation/invention:** zero-tolerance per Unit 3-A Section 4/8, unchanged.
13. **Model/provider qualification:** any experiment introducing a new model must use the full governed qualification process (Unit 3-A Section 11) — no shortcut for a smaller trial.
14. **Context profiles:** reuse Unit 1's frozen nine profiles; Unit 2-D exercised three; expanding coverage is a per-profile scope decision, not a blanket requirement to test all nine in the first wave.
15. **Inference configuration:** preserved unchanged for families A/B/C/D experiments, exactly as every prior Scope Lock in this programme required; family E, if ever scoped, isolates this as its own single variable, never combined with another family's change in the same trial.
16. **Repetition requirements:** triage-grade, bounded repeat counts per cell (Unit 2-D's own DQ1/DQ5 precedent), not qualification-scale (≥30/cell, ≥300-exposure) sampling — that remains a later, separate, more expensive undertaking for whichever family survives exploratory triage.
17. **Statistical interpretation:** exploratory-tier results carry the same epistemic humility Unit 2-D's own interpretation rules required — no population-rate claims from small samples.
18. **Artifact integrity:** full hash/size/line-count verification, exactly as Units 1, 2, and 2-D already practiced.
19. **Exact-once execution:** the same durable ledger/checkpoint/intent-record discipline Unit 2-D's driver already implemented — reused, not reinvented.
20. **Stop conditions:** semantic disagreement between candidate and control arms is not a stop condition — it is the evidence being gathered. Identity, configuration, artifact-integrity, and harness defects remain hard stops, exactly mirroring Unit 2-D's own distinction.
21. **Isolation:** absolute — any experimental driver terminates at the recorded observation; no path to Memory, Goal, planning, tools, or execution, regardless of which family is under test.
22. **Comparison rules:** experiments are only comparable when run against the same frozen corpus, context, and model baseline; no family may be favorably compared against another using different fixtures, sample sizes, or context profiles.
23. **Prohibition on declaring a winner prematurely:** a single exploratory-tier result — including DQ5's own 2/5 — is never sufficient for Remedy Selection (Unit 3-E); only qualification-tier evidence, evaluated against the *full* Unit 3-A contract through Unit 3-D's comparative evaluation, can support selection.

**Evidence-tier distinction, restated explicitly, not collapsed:** **exploratory evidence** (small, triage-grade, like Unit 2-D's own design) decides only whether a family deserves further investment; **qualification evidence** (large, pre-registered, zero-event-gated per Unit 3-A Section 4) determines whether a specific candidate meets the contract, and is required before any remedy may be selected; **production-selection evidence** is not itself new data collection — it is Unit 3-E's governance decision, informed by qualification-tier evidence via Unit 3-D. None of these three may substitute for another.

## 9. DQ5 interpretation

DQ5's evidence (0/18 pooled joint-task vs. 2/5 decision-only) justifies exactly one thing: including family A (decision/rendering separation) in Unit 3-B's scoped-for-experimentation list, with an experiment design that tests the *full* contract (Section 7), not merely a repeat of the single positive semantic-accuracy metric DQ5 already produced. It does not justify skipping directly to qualification-tier evidence for family A, does not justify treating family A as "the" remedy, and does not exempt family A from the same false-positive, regression, and representation-validity scrutiny every other included family receives. The strongest positive diagnostic signal in Unit 2-D's evidence remains exactly that — a diagnostic signal — not a preferred architecture.

## 10. False-positive safety requirements

Every experiment, regardless of family, must measure false-positive `REMEMBER`/`GOAL` on the corpus's negative and adversarial fixtures, not only true-positive accuracy on positive fixtures — this is the single most consistent gap across Unit 2-D's own evidence (which tested breadth and repeatability but never adversarial resistance) and the single most load-bearing requirement carried over from Unit 3-A Section 7 unchanged. No experiment result may be reported as favorable based on true-positive performance alone while omitting false-positive exposure.

## 11. Artifact and execution isolation requirements

Every future Unit 3-C experiment must: use a new, distinctly-marked campaign identity, never reusing or nesting inside the failed Unit 2 or the closed Unit 2-D campaign directories; produce full raw/checkpoint/intent/manifest artifacts with independent hash verification; terminate strictly at the recorded observation with zero path to any downstream coordinator; and require its own full Scope Lock → Implementation/Execution Plan → Readiness Review → Explicit Execution Approval chain before any live call — no experiment may skip a gate Unit 2-D itself was required to pass.

## 12. Relationship between Units 3-B, 3-C, 3-D, 3-E, and Unit 4

- **3-B (this document's subject):** experiment governance only — which families, what architecture, what comparison rules. No live call.
- **3-C:** the controlled experiments themselves, authorized only under 3-B's frozen architecture and each family's own further Scope Lock. Evidence-gathering, not deployment.
- **3-D:** comparative evaluation of 3-C's results against the full Unit 3-A contract, mirroring Unit 2-D's own Interpretation and Closure pattern.
- **3-E:** the remedy-selection governance decision itself, informed by 3-D, expressed as a document, not code.
- **Unit 4:** implementation of whatever 3-E selects — outside Unit 3 entirely, already reserved by the original programme Planning Review, requiring its own Planning Review, Boundary Review, and full Completion/Independent Constitutional Review cycle.

Adversarially tested: does this Planning Review itself perform any 3-C/3-D/3-E/4 work? No live call, no experiment design executed, no comparative evaluation of any result (none exists), no remedy selected, no implementation code proposed anywhere in this document — checked explicitly against Sections 6–11 above.

## 13. Unresolved questions

Carried forward, not resolved here: the specific triage-grade repeat count for any Unit 3-B-scoped experiment (bounded by precedent, not yet fixed); which of the corpus's negative/adversarial fixtures beyond `P02`/`P03`/`P12` should be mandatory per family; whether family C's experiment should be gated behind a stricter approval threshold than A/B given its materially higher risk; the four contract dimensions (#11, #12, #14, #15) no family's evidence currently addresses at all.

## 14. Prohibited interpretations

Not established anywhere in this document: that decision/rendering separation is or should become Parker's architecture; that any family is "the" answer; that families D, E, or the deferred half of G are unworkable (they are lower-priority, not disproven); that F or H are permanently excluded (they are excluded *on current evidence*, a revisable determination); that any numerical threshold beyond those already frozen by Unit 3-A has been decided; that exploratory-tier evidence of any kind is sufficient for remedy selection; that Unit 3-C, 3-D, 3-E, or Unit 4 work is authorized by this document.

## 15. Self-review (Phase 9)

Checked explicitly against each listed risk: remedy leakage — none found, no family is selected or implemented. Informal winner selection — none found; Section 9 explicitly rejects treating DQ5 as architecture-selecting. Unsupported DQ5 preference — checked; family A receives the same full-contract scrutiny requirement as B and C, not lighter treatment. Invented thresholds — none found; Section 7's table and Section 8 both explicitly decline to number anything beyond Unit 3-A's own frozen figures. Accidental production design — none found; Section 8 defines architecture for *experiments*, never production. Semantic/representation conflation — checked; every family's analysis in Section 6 keeps the two axes distinct. False-positive safety weakening — none found; Section 10 elevates, not weakens, this requirement relative to Unit 2-D's own gap. Unjustified model ranking — none found; family F is excluded on cost/evidence-proportionality grounds, not ranked against any specific alternative. Hidden live-execution authority — none found; Section 11 requires a full future gate sequence before any call. Unit 3-C/D/E/4 scope leakage — tested explicitly in Section 12, none found.

## 16. Recommended next governance step

Draft a Unit 3-B Scope Lock incorporating: the family classifications in Section 6 (A, B, C included; D, E, and representation-only retry deferred; F, semantic retry/repair, and H excluded on current evidence); the traceability matrix in Section 7; the twenty-three experiment-architecture requirements in Section 8; the evidence-tier distinction; the DQ5 interpretation in Section 9; the false-positive and isolation requirements in Sections 10–11; and the Unit 3-B/C/D/E/4 boundary in Section 12. Not performed by this document.

## 17. Final planning verdict

```text
A — PROCEED TO UNIT 3-B SCOPE LOCK
```

Justified because: Unit 3-A already resolved the contract-level questions Unit 3-B depends on, with its own explicitly unresolved items correctly carried forward rather than blocking; the remedy-family landscape is now differentiated with clear, evidence-traceable reasoning (not a blanket inclusion); the experiment-architecture requirements are derivable now, from already-accepted governance and Unit 2-D's own precedent, without requiring new live evidence first; and no genuine open governance question blocks *drafting* a Scope Lock, as opposed to being properly resolved *within* one (Section 13).
