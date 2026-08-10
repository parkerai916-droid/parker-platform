**Status:** Unit 3-C — Controlled Remedy Experiments — Scope Lock — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** Freezes the lawful experimental structure of Unit 3-C for remedy Families A, B, and C. Contains no implementation, no experiment execution, no remedy selection, and authorizes no live model call.

# Reasoning Protocol Live-Model Conformance Unit 3-C — Controlled Remedy Experiments — Scope Lock

## 1. Baseline

Drafted against committed baseline `8a59cdef07fee88ad3afb487fa77a21f01366f74`. Controlling authority: the frozen Unit 3-A Reliability Contract Definition Scope Lock (`ab27f18`), the frozen Unit 3-B Remedy Experiment Scoping Scope Lock (`55af571`) and its accepted Independent Constitutional Review, the committed Unit 3-C Controlled Remedy Experiments Planning Review (`4d55632`), and the committed Unit 3-C Family C Adversarial Fixture Coverage Audit (`8a59cde`). Where this Scope Lock adopts a Planning Review or Audit conclusion, it does so because independent re-reading during drafting found no constitutional defect requiring departure.

## 2. Authority and unit purpose (Phase A)

Unit 3-C is the controlled-experiment unit for remedy Families A (decision/rendering separation), B (prompt/protocol redesign), and C (deterministic/rule-assisted handling) only, per the frozen Unit 3-B classification. Its purpose is to produce exploratory evidence (Section 11) suitable for later Unit 3-D comparative evaluation. It does not rank remedies, select a remedy, qualify any remedy for production, or authorize production implementation.

| Unit | Function |
|---|---|
| 3-C | Controlled remedy experiments (this document's subject) |
| 3-D | Comparative evaluation |
| 3-E | Remedy selection |
| Unit 4 | Selected-remedy implementation |

No later-unit authority may leak backward into this Scope Lock.

## 3. Campaign structure (Phase B)

Frozen, per the Unit 3-C Planning Review's own reasoned determination: **one governed Unit 3-C campaign containing isolated experimental arms** — a control arm, a Family A arm, a Family B arm, and a Family C arm — rather than separate campaigns per family. Each arm must have independent identity and independent fault isolation within the campaign, mirroring Unit 2-D's own accepted warmup/production-track/candidate-track design. **Binding requirement:** a measurement defect specific to one arm must not silently invalidate, halt, or contaminate another arm's evidence collection; a future Implementation/Execution Plan must specify the exact mechanism (e.g., independent per-arm ledgers and checkpoint/halt state) that satisfies this requirement. No implementation class, file structure, or code-level design is defined here.

## 4. Common control (Phase C)

Frozen: one shared control arm serves all three families. The control must use the exact, unmodified current Parker reasoning protocol — the current production-generated prompt (`DefaultReasoningPromptBuilder`, unmodified), the current parser (`TaggedReasoningResponseParser`, unmodified), the current model/provider (Section 13), the current inference configuration (Section 14), the authoritative base fixture corpus (Section 5), matching context profiles where a given comparison requires them, the same semantic/representation classification system (Unit 1's `PrimaryClassification` taxonomy and Unit 3-A's independent semantic/representation axes, both reused unchanged), and the same artifact/provenance rules (Section 20) as every candidate arm it is compared against. No remedy arm may define its own, easier, or differently-configured control; every family's comparison is against this one shared baseline.

## 5. Authoritative corpus (Phase D)

Frozen: the authoritative base corpus for Unit 3-C is `BaselineCorpus`, defined in `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt`, independently verified byte-for-byte against the frozen Unit 2 Scope Lock's Section 3 table (twenty-three fixtures: `R01-direct` through `N02-unicode-whitespace`). This document does not duplicate that corpus; it is incorporated by reference to its existing, frozen, authoritative source.

**Explicitly and bindingly stated:** `SyntheticConformanceCorpus`, defined in `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`, is **not** the authoritative Unit 3-C corpus. It is a smaller, differently-worded, differently-IDed collection used only by an unrelated, earlier harness-mechanics self-test (`ReasoningProtocolLiveModelConformanceTest.kt`) and played no role in Unit 2's actual execution. This is stated here specifically to close the naming-collision risk the Family C Fixture Coverage Audit documented (`8a59cde`, Section 4).

## 6. Family A — decision/rendering separation (Phase E)

**Frozen experimental question:** whether separating semantic action decision from response-content rendering changes reliability measured against the frozen Unit 3-A contract. This Scope Lock does not freeze a production two-call architecture, and no future Family A experiment may be represented as having designed one.

**Binding measurement requirement — the experiment must distinguish:** (1) decision-step semantic action accuracy; (2) rendering-step representation validity; (3) rendering-step content fidelity; (4) false-positive REMEMBER; (5) false-positive GOAL. The experiment must exercise the full authoritative base corpus (Section 5), not the original `R01-direct`/`PF01` fixture alone.

**DQ5's role, bound:** DQ5 (Unit 2-D) is evidentiary authority justifying that this family warrants investigation. It is not a template a future experiment must copy unchanged, and it does not itself constitute or excuse any part of the measurement requirement above. A result favorable to Family A remains exploratory evidence only (Section 11) — it does not, by itself, qualify, select, or authorize implementation of anything.

## 7. Family B — prompt/protocol redesign (Phase F)

**Frozen control:** the current, unmodified `DefaultReasoningPromptBuilder` output.

**Frozen experimental variable, narrowly bound:** Family B may experimentally vary only the classification/selection-guidance component of the prompt (`SELECTION_GUIDANCE`, as currently implemented) unless a later, separate governance act expressly expands this variable. The experiment must **not**: alter the tag grammar `TaggedReasoningResponseParser` parses; alter parser semantics in any way; embed any fixture-specific answer into a candidate's guidance text; alter model or provider identity; alter inference configuration; simultaneously introduce structured or schema-constrained output (Family D, deferred); or simultaneously introduce retry or repair (Family G, deferred/excluded per Section 24).

**Pre-registration, binding:** candidate prompt definitions must be frozen, in full and in writing, before any live execution. A run-inspect-tweak-rerun sequence may never be presented as one registered experiment's evidence (Section 15).

No candidate prompt wording is written, selected, or implied anywhere in this Scope Lock.

## 8. Family C — deterministic/rule-assisted handling (Phase G)

**Frozen experimental purpose:** whether a deterministic or rule-assisted mechanism can correctly distinguish constitutionally explicit cases without producing prohibited false-positive REMEMBER or GOAL behavior.

**Explicitly not selected by this Scope Lock, and not to be selected without separate future governance:** a standalone pre-classifier architecture; a rule-assisted model-signal architecture; a regex mechanism; a keyword mechanism; or any specific production integration architecture. These remain later experiment-design questions, deferred exactly as the Unit 3-C Planning Review's own Section 9 recommended (measuring a to-be-defined rule's standalone accuracy/false-positive profile, independent of any integration-architecture commitment).

## 9. Family C fixture precondition — binding constitutional gate (Phase H)

The committed Family C Adversarial Fixture Coverage Audit (`8a59cde`) found, against the nine categories Unit 3-B Section 4 requires:

- **Fully covered:** category 1 (direct explicit Remember instruction), category 4 (quoted Remember instruction), category 9 (ambiguous-boundary case).
- **Partial:** category 5 (hypothetical Remember instruction), category 6 (negated/discussed Remember instruction — "discussed" half only), category 8 (GOAL-like language with overlapping trigger vocabulary — semantic-field adjacency only, not literal lexical overlap).
- **Absent:** category 2 (ordinary statement of fact containing memory-related vocabulary), category 3 (question about remembering), category 7 (conversational mention of memory).

**Binding gate, stated without qualification:** Family C **must not proceed to implementation, readiness review, or live execution** until exact supplemental fixtures have been governed and frozen that create coverage for categories 2, 3, and 7; strengthen categories 5, 6, and 8 with more precisely disambiguating text; and preserve categories 1, 4, and 9 unchanged. Every supplemental fixture must have a constitutionally defensible expected action and must be independently reviewed before any Family C execution. This gate applies to Family C only.

**Family A and Family B are not blocked by this gate** — their experiments require only the authoritative base corpus (Section 5), which is fully present and unaffected by the coverage gap.

No exact supplemental fixture text is invented, drafted, or implied anywhere in this Scope Lock.

## 10. Fixture governance tier (Phase I)

**Determination, tested against both available precedents in this programme:** exact supplemental Family C fixture text is retained at the **Implementation/Execution Plan tier**, not elevated to Scope Lock tier — but this placement is bound by a compensating rigor requirement, reasoned as follows.

Two precedents were weighed. Unit 2's own Scope Lock froze its entire twenty-three-fixture corpus directly within the Scope Lock itself (Section 3 there) — appropriate because designing that corpus from nothing was itself the central substantive question the Scope Lock existed to settle. Unit 2-D's Scope Lock, by contrast, deferred its fixture definitions to its own implementation/test-file tier — appropriate because Unit 2-D's fixtures were almost entirely reused, frozen text, with only one narrow new construct (the decision-only variant, reusing `r01Direct`'s own prose unchanged). Family C's supplemental fixtures are substantively novel, closer to Unit 2's situation than Unit 2-D's — this argues for elevating them to Scope Lock tier.

However, administratively deferring exact text to the Implementation/Execution Plan tier avoids reopening and re-freezing this Scope Lock merely to add fixture prose, and is not itself constitutionally unsound provided the later tier is held to Scope-Lock-grade rigor rather than ordinary implementation-detail review. This Scope Lock resolves the tension by **retaining** the deferral, bound by the following requirement, which is itself frozen and binding: before any Family C execution, the future Implementation/Execution Plan's fixture section must state, for every supplemental fixture, its exact text, its expected action, its rationale, its category mapping (against the nine categories), and its adversarial purpose, and that section must receive its own dedicated Independent Constitutional Review — not merely ordinary Completion or Readiness review — before Family C may proceed past that gate (Section 9).

No fixture text is invented here to test this determination.

## 11. Evidence tier (Phase J)

Frozen: all Unit 3-C evidence is **exploratory evidence**, per the evidence-tier distinction Unit 3-B Section 6 already froze. Explicitly, Unit 3-C evidence is **not automatically**: qualification evidence; production-selection evidence; proof of Unit 3-A conformance; or production-readiness evidence. Unit 3-D may compare exploratory evidence produced here. Unit 3-E owns remedy selection. Separate, later qualification governance — meeting Unit 3-A Section 4's own already-frozen thresholds (≥30 trials per characterisation cell, ≥300-exposure zero-event qualification gates) — remains required before any production implementation, regardless of how favorable any Unit 3-C exploratory result is.

## 12. Repetition (Phase K)

The exact exploratory repeat count remains **unresolved** and is not invented here. Frozen governing principle instead: repetition must be pre-registered before any live call; justified per experimental purpose (a fixture or arm with established low variance may justify a smaller count than one with unknown variance); sufficient to distinguish obvious stochastic instability from an isolated single observation; and never represented as qualification-scale evidence unless later governance explicitly authorizes that interpretation. **The exact count must be resolved and frozen by the future Implementation/Execution Plan before any live campaign execution** — not by this Scope Lock, and not informally during execution.

## 13. Model strategy (Phase L)

Frozen: **qwen2.5-coder:7b only**, digest-verified, for causal experimentation across Families A, B, and C. Reason: Family F (model/provider substitution) is excluded from near-term remedy experimentation by the frozen Unit 3-B classification; introducing a second model as a comparison or replication arm within Family A/B/C's own causal experiments would reopen that excluded question as an uncontrolled side effect and confound attribution of any observed effect to the remedy under test versus to model choice. Unit 3-C must not become a model benchmark. Llama, or any other model, may not be authorized as a Unit 3-C comparison arm without a separate, future governance act expressly amending this scope.

## 14. Inference configuration (Phase M)

Frozen: the current production inference configuration is held identical across the control arm and every Family A/B/C arm. No arm may introduce a Family E (inference-control) change of any kind. Future execution identity must pin and record: model identity and digest; Ollama/runtime identity; the exact request-body shape (currently `model`, `prompt`, `stream:false` only — no `temperature`, `seed`, `top_p`, `top_k`, or `num_predict`); the timeout value (`ModelReasoningProvider`'s current `30_000` ms default, unless a future, separately-governed act states an affirmative reason to vary it); the endpoint; and any Ollama server/model-file default capable of silently affecting inference behavior even though Parker's own request never sets it explicitly. No temperature, seed, top-p, or top-k experiment belongs within Unit 3-C's scope under any family.

## 15. Adaptive-experimentation prohibition (Phase N)

Frozen: before any live execution, the following must be pre-registered and frozen: campaign identity; arm definitions; candidate mechanism identities; the Family B candidate prompt identity; the Family C experimental mechanism identity; the fixture corpus, including any Family C supplemental fixtures (Section 10); expected actions; context profiles; the repetition schedule (Section 12); stop rules (Section 16); model/configuration identity (Sections 13–14); and the artifact schema (Section 20). No run → inspect → tweak → rerun sequence may be presented as one registered experiment's evidence. A materially changed candidate mechanism, at any point after pre-registration, requires separate, fresh governance — not a silent amendment of the running experiment.

## 16. Stop conditions (Phase O)

Frozen distinction: **measurement-invalidating failures** — repository identity mismatch, model identity mismatch, configuration drift, harness defect, parser/classifier measurement defect, artifact-integrity failure, call-accounting ambiguity, unauthorized downstream action, campaign corruption — fail closed. **Remedy-performance failures** — wrong semantic action, false-positive REMEMBER, false-positive GOAL, representation failure, content-fidelity failure — are evidence and must be recorded, not concealed; a candidate performing badly does not, by itself, stop the experiment.

**Binding safety requirement, without an invented threshold:** a governed manual review checkpoint is required if concentrated false-positive REMEMBER/GOAL events create a credible safety concern — distinguishing genuine remedy-performance evidence from a harness or rule-wiring defect masquerading as such. The exact concentration threshold that triggers this checkpoint is not invented here and must be defined by the future Implementation/Execution Plan.

## 17. Semantic/representation separation (Phase P)

Frozen, unweakened from Unit 3-A Section 6: semantic correctness and representation validity are independent axes. No candidate arm passes because syntax improved while the semantic action selected remained wrong, or because the semantic action improved while representation became invalid. Content fidelity (Unit 3-A Section 8) remains a separate, additional measure where a fixture defines a content-fidelity expectation, and may not be used to conceal a deficiency on either axis.

## 18. Unit 3-A traceability (Phase Q)

Frozen: every arm must map its own measurements to the frozen Unit 3-A Reliability Contract dimensions it actually tests. No arm, and no comparison across arms, may claim overall reliability, or claim satisfaction of the Reliability Contract as a whole, based on partial coverage of it. The following Unit 3-A dimensions remain unresolved by current governance and are **not** resolved by this Scope Lock: **#11** (ambiguity/clarification), **#12** (timeout value), **#14** (unavailable-provider behaviour), **#15** (fail-versus-warn choice). Unit 3-C must not fabricate an answer to any of them.

## 19. Unit 3-D comparability (Phase R)

Frozen: Unit 3-C must produce, for fair later comparison: a shared control (Section 4); a shared base corpus (Section 5); matched model (Section 13); matched inference configuration (Section 14); matched context profiles; matched repetitions within each family's own control-versus-candidate pairing; a common semantic classification scheme; a common representation classification scheme; false-positive results, reported with their own dedicated visibility, never omitted; content-fidelity evidence where applicable; and full artifact provenance (Section 20 below) per family.

**Explicitly identified as not numerically comparable across families without additional, separately-governed methodology:** Family C's supplemental adversarial-fixture results (no equivalent exists for Family A or B unless they are also run against the identical supplemental set, which this Scope Lock does not require); and Family A's rendering-step cost/call-count structure (the control issues one call, Family A's candidate arm issues at least two — an asymmetry that must be reported transparently, never hidden or normalized away).

## 20. Artifact/provenance requirements (Phase S)

Frozen, governance-level, not implemented here: every observation must be attributable to campaign; family; arm; fixture; context profile; trial sequence; expected action; actual action; semantic result; representation result; content-fidelity result where applicable; model/configuration identity; prompt/protocol identity; candidate mechanism identity; the raw provider request and response where applicable; latency/transport result; and artifact hash/provenance. The future Implementation/Execution Plan must define the exact schema satisfying this requirement.

## 21. Downstream isolation (Phase T)

Frozen, absolute, unqualified, applying equally to Families A, B, and C: no experimental output may trigger Memory admission, Goal creation, Knowledge Submission, planning, tool invocation, external communication, evidence mutation, production state mutation, or any other consequential execution. Every Unit 3-C experiment observes reasoning behaviour only.

## 22. Exact-once and durability principles (Phase U)

Frozen, governance-level, not implemented here: durable recording of intent before any call; durable recording of the raw observation; completion/checkpoint state; duplicate-call prevention; fail-closed handling of any ambiguous execution state; no silent automatic rerun of already-completed evidence; explicit campaign seal/halt semantics; and a full artifact-integrity inventory. The future Implementation/Execution Plan must satisfy these requirements, mirroring Unit 2-D's own exact-once driver design.

## 23. No winner in Unit 3-C (Phase V)

Stated prominently and bindingly: **Unit 3-C does not select a remedy.** A family may perform well, perform poorly, or outperform the control on some exploratory measure, and still not be constitutionally selected by virtue of that result. Unit 3-D compares. Unit 3-E selects. No report produced under this Scope Lock may declare a winner.

## 24. Deferred and excluded families (Phase W)

Frozen, unchanged from Unit 3-B: Unit 3-C does not reopen Family D (structured/schema-constrained output, deferred), Family E (inference-control changes, deferred), Family F (model/provider substitution, excluded on current evidence as a near-term remedy), Family G-representation-only-retry (deferred) or Family G-semantic-retry/repair (excluded on current evidence), Family H (multi-model/fallback strategies, excluded on current evidence), or Family I (hybrid approaches, deferred) — whether directly or indirectly through any Family A/B/C experiment design — unless a separate governance act expressly amends the frozen Unit 3-B scope.

## 25. Exit criteria (Phase X)

This Scope Lock is ready to freeze once: the Unit 3-B/C/D/E/Unit 4 boundary is defined (Section 2); the campaign architecture is defined (Section 3); the common control is defined (Section 4); the authoritative corpus is defined and `SyntheticConformanceCorpus` is explicitly excluded (Section 5); the Family C fixture precondition is an explicit, binding gate (Section 9); the fixture-governance tier is resolved (Section 10); the evidence tier is frozen (Section 11); the repetition principle (not the exact count) is frozen (Section 12); the model and inference-configuration strategies are frozen (Sections 13–14); pre-registration is frozen (Section 15); stop semantics are frozen (Section 16); Unit 3-A traceability is frozen (Section 18); Unit 3-D comparability requirements are frozen (Section 19); artifact/provenance and downstream-isolation obligations are frozen (Sections 20–21); no remedy has been selected anywhere in this document (Section 23); and this Scope Lock has passed Independent Constitutional Review with a verdict of ACCEPTED or ACCEPTED WITH QUALIFICATIONS whose qualifications are non-blocking. This Scope Lock may close with the exact repeat count and exact supplemental Family C fixture text unresolved, because both are explicitly and bindingly assigned to a later, more rigorously reviewed governance artifact before any execution that would depend on them (Sections 10, 12). A verdict of TARGETED AMENDMENT REQUIRED or REJECTED means this document is not ready to freeze.
