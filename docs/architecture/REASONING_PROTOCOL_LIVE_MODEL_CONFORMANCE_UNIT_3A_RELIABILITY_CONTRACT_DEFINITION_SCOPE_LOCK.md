**Status:** Unit 3-A Reliability Contract Definition Scope Lock — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** Governance only, against committed baseline `7e9e388`. No implementation, live call, experiment, or remedy selection is authorized by this document. Nothing beyond this Scope Lock is created.

# Reasoning Protocol Live-Model Conformance Unit 3-A — Reliability Contract Definition Scope Lock

## 1. Status and authority

Proposed governance, implementing the Unit 3 Planning Review's (`7e9e388`, ACCEPTED WITH QUALIFICATIONS by its own Independent Planning Review) determination to proceed to Unit 3-A Scope Lock drafting now. Authority for Unit 3-A's own existence and boundary traces to the top-level programme Planning Review (`05d4c2a`), which names Unit 3 "Reliability Contract and Remedy Selection... freeze semantic/rendering contract, schema/config/retry rules, model and timeout decision criteria based on Unit 2... contract and constitutional reviews before code," and reserves actual implementation for a separate Unit 4. This document does not amend any prior accepted governance, does not reopen Unit 2 or Unit 2-D, and does not authorize Unit 3-B, 3-C, 3-D, 3-E, or Unit 4.

## 2. The Unit 3-A boundary

Unit 3-A defines the required, externally observable reliability contract of Parker's reasoning boundary — **what** result must occur. It does not select **how** that result is achieved. Every requirement in this document is written as an observable behavior a correctly-functioning reasoning boundary must exhibit, never as a named mechanism.

**Permitted contract statement (example):** "An explicit, unambiguous instruction to remember a stated fact must be semantically classified as REMEMBER."
**Not permitted as a Unit 3-A requirement (example):** "Explicit Remember instructions must be handled by deterministic classification."

Every section below is held to this test. Where a mechanism is mentioned at all, it is mentioned only as *evidence already gathered about the current implementation* or as an *example a future remedy might or might not use* — never as a requirement.

## 3. Evidence and governance reviewed

Read fresh: the top-level programme Planning Review and its Independent Constitutional Review (`05d4c2a`); Unit 1's accepted governance and harness implementation; the Unit 2 Scope Lock, Implementation/Execution Plan, Stage 0 Failure Review, its Independent Constitutional Review, and the Post-Stage-0-Failure Governance Determination; the Post-Unit-2 Diagnostic Planning Review; the Unit 2-D Scope Lock, Implementation/Execution Plan, Execution Evidence Review, Interpretation and Closure Review, and its Independent Constitutional Review; the Unit 3 Planning Review and its Independent Planning Review; and the current production `ReasoningProviderResponse`, `TaggedReasoningResponseParser`, `DefaultReasoningPromptBuilder`, and `ModelReasoningProvider` (re-inspected directly, confirmed unchanged since long before this whole program of work — last touched `80e561a`).

## 4. Inherited frozen requirements register

Every requirement below is traced to its authoritative source, not to the Unit 3 Planning Review's own citation of it — each numerical figure was independently re-read from the cited primary document during this Scope Lock's drafting.

| Requirement | Authoritative source | Status | Existing threshold | Unit 3-A action |
|---|---|---|---|---|
| Two-stage evidence design (Characterisation vs. Qualification) | Programme Planning Review §6 | Frozen | Characterisation: ≥30 independent trials/cell (discovery sample, does not qualify). Qualification: sample size pre-registered from acceptance threshold; critical zero-event gates use ≥300 exposures (`1 - 0.05^(1/300)` ≈ one-sided 95% upper failure bound below 1%) | Inherited unchanged |
| Zero observed false-positive REMEMBER/GOAL on all negative/adversarial exposures | Programme Planning Review §6 | Frozen | Zero observed events, at qualification-tier sample size | Inherited unchanged |
| Zero material mutation or invention in accepted consequential outputs | Programme Planning Review §6 | Frozen | Zero observed events; non-material paraphrase is separately measured, not automatically disqualifying (§3's own taxonomy: paraphrase = class B, distinct from failure; "material change is failure") | Inherited unchanged |
| ≥99% representation validity | Programme Planning Review §6 | Frozen | 99% | Inherited unchanged |
| Correct action selection, one-sided 95% LCB ≥97%, per action and full-context stratum | Programme Planning Review §6 | Frozen | 97% | Inherited unchanged |
| "A valid wrong action remains a failure" | Programme Planning Review §6 | Frozen | N/A (qualitative) | Inherited unchanged |
| No aggregate score may conceal an action-specific failure | Programme Planning Review §6 | Frozen | N/A (qualitative) | Inherited unchanged |
| Timeout/transport measured as separate operational axes (H/I), never scored as semantic answers | Programme Planning Review §3, §12 | Frozen | N/A (qualitative) | Inherited unchanged |
| Production timeout *value* must derive from measured percentiles, explicit margin, and maximum | Programme Planning Review §12 | Frozen requirement, unresolved value | None frozen — "historical 30-second setting and temporary 90-second override are evidence, not new authority" | Marked unresolved; not decided here |
| Model qualification attaches to immutable digest/configuration; deployment fails or warns closed on mismatch | Programme Planning Review §10 | Frozen requirement, unresolved choice | Fail-vs-warn choice explicitly deferred ("as later governance decides") | Marked unresolved; not decided here |
| Alternative-model evaluation requires the identical blind corpus and hardware-aware protocol; selection by per-stratum conformance, not size/reputation | Programme Planning Review §11 | Frozen | N/A (qualitative process requirement) | Inherited unchanged |
| No retry may escalate a valid non-consequential result into REMEMBER/GOAL without separately accepted semantic-retry governance | Programme Planning Review §8, §10 | Frozen | N/A (qualitative) | Inherited unchanged |
| `Remember` carries no confidence, importance, or evidential-weight judgment, "and never may" (the source doc comment analogizes `Goal`/`Reply` to this classification in kind, but does not independently restate the same guarantee for either) | `src/interfaces/ReasoningProvider.kt` (`Remember`'s own doc comment) | Frozen (existing domain-type invariant) | N/A (qualitative) | Inherited unchanged for `Remember`; forecloses confidence-scored variants of `Remember` specifically absent a separately governed contract change — not independently established for `Goal`/`Reply` by this source |
| Downstream authorization, Memory admission, Goal handling remain unchanged unless a later unit expressly governs a change | Programme Planning Review §10 | Frozen | N/A (qualitative) | Inherited unchanged |
| Adding confidence, uncertainty, multiple acts, repair semantics, or a new action requires a separately governed contract change | Programme Planning Review §9 | Frozen | N/A (qualitative) | Inherited unchanged |

No numerical value above was interpolated, rounded, strengthened, or weakened from its source. Where the source itself defers a value ("as later governance decides," "historical... setting... is evidence, not new authority"), this Scope Lock preserves that deferral rather than resolving it.

## 5. Reliability contract dimensions

Twenty-three dimensions, classified per Phase 5's four categories. **FROZEN REQUIREMENT** = both the qualitative requirement and any applicable numerical threshold are already established by cited governance. **FROZEN REQUIREMENT WITH UNRESOLVED THRESHOLD** = the qualitative requirement is frozen but a needed number is not. **DEFERRED GOVERNANCE QUESTION** = no accepted governance yet answers this. **OUT OF UNIT 3-A** = belongs to different governance layer entirely.

1. **Semantic action-selection correctness** — FROZEN REQUIREMENT (Section 4, row 5).
2. **REMEMBER true-positive behaviour** — FROZEN REQUIREMENT: an explicit, unambiguous instruction to remember a stated fact must be classified `REMEMBER` (source: `Remember`'s own doc comment; Programme Planning Review §4 corpus).
3. **REMEMBER false-positive behaviour** — FROZEN REQUIREMENT (Section 4, row 2).
4. **GOAL true-positive behaviour** — FROZEN REQUIREMENT: an explicit request for planned, multi-step, or tool-requiring work must be classified `GOAL` (source: Programme Planning Review §4 corpus; same per-action accuracy requirement).
5. **GOAL false-positive behaviour** — FROZEN REQUIREMENT (Section 4, row 2).
6. **REPLY behaviour** — FROZEN REQUIREMENT: ordinary conversational input, and any case of genuine doubt about REMEMBER/GOAL intent, must be classified `REPLY` (source: `Remember`'s doc comment — "Where any doubt exists... `Reply` or `NoAction` is the correct classification instead, never this one" — plus the same per-action accuracy requirement).
7. **NOACTION behaviour** — FROZEN REQUIREMENT: reserved for cases the reasoning boundary can confidently determine warrant neither reply nor goal, not a default for uncertainty (source: `NoAction`'s own doc comment — "confidently determined" — plus the same per-action accuracy requirement).
8. **Representation validity** — FROZEN REQUIREMENT (Section 4, row 4).
9. **Content fidelity** — FROZEN REQUIREMENT: exact retention is the target; non-material paraphrase is measured but not automatically disqualifying; material mutation or invention is zero-tolerance at qualification tier (Section 4, row 3).
10. **Malformed output** — FROZEN REQUIREMENT: malformed, untagged, multiply-tagged, and blank/partial output must be classified as representation failure, never silently repaired or treated as a semantic answer (source: Programme Planning Review §3 taxonomy classes C/E/F/G; existing `TaggedReasoningResponseParser` performs no semantic repair, already governed unchanged behavior).
11. **Ambiguous owner input** — FROZEN REQUIREMENT for the qualitative principle only: genuine doubt must route to `REPLY`/`NoAction`, never `REMEMBER`/`GOAL` (Section 4, row 12; item 6 above). Whether the reasoning boundary must ever actively *seek clarification*, as opposed to merely declining the consequential action, is a **DEFERRED GOVERNANCE QUESTION** — no accepted governance requires or forbids this, and Unit 3-A does not manufacture a universal clarification rule.
12. **Timeout behaviour** — FROZEN REQUIREMENT WITH UNRESOLVED THRESHOLD (Section 4, row 8).
13. **Transport/provider failure** — FROZEN REQUIREMENT: measured as a distinct operational class (I), never scored as a semantic answer (Section 4, row 8).
14. **Unavailable provider/model** — **DEFERRED GOVERNANCE QUESTION.** No accepted governance defines required externally observable behavior when the provider itself cannot be reached at all (distinct from a single transport-failure event). Not resolved here.
15. **Unqualified provider/model** — FROZEN REQUIREMENT WITH UNRESOLVED THRESHOLD: deployment on an unqualified digest/configuration must fail or warn closed, not silently proceed; the fail-vs-warn choice itself remains deferred (Section 4, row 9).
16. **Fail-closed behaviour** — FROZEN REQUIREMENT (Section 4, row 10, and the entirety of Programme Planning Review §10).
17. **Downstream isolation** — FROZEN REQUIREMENT (Section 4, row 13).
18. **Consequential-action prevention on ambiguous/failed classification** — FROZEN REQUIREMENT: empty consequential content and schema-valid-but-semantically-invalid content must fail closed; no mechanism may escalate a non-consequential result into a consequential one without separately governed authority (Programme Planning Review §10).
19. **Observability** — FROZEN REQUIREMENT for the qualitative principle: reasoning-boundary behavior must be attributable to repository commit, model identity/digest, and configuration (Programme Planning Review §7). The specific evaluation-artifact record schema is measurement-programme tooling, not itself a contract requirement on production behavior — **OUT OF UNIT 3-A** at that level of detail.
20. **Provenance/auditability** — FROZEN REQUIREMENT: the same identity-attribution principle as item 19, reinforced by every unit's own artifact-integrity/hash-chain precedent (Units 1, 2, 2-D).
21. **Reproducibility** — FROZEN REQUIREMENT WITH UNRESOLVED THRESHOLD: must be measured (Programme Planning Review §6: "A deterministic configuration is repeated to detect residual nondeterminism"); no separate numerical tolerance for variation exists beyond the per-action accuracy thresholds already governed (item 1).
22. **Model/provider qualification** — FROZEN REQUIREMENT (Section 4, row 6).
23. **Regression protection** — FROZEN REQUIREMENT for the qualitative principle: no change may silently degrade a currently-correct action while improving another (Programme Planning Review §6, §11); no separate numerical threshold beyond the same per-action accuracy gates (item 1).

## 6. Semantic action-selection contract

The required semantics of the four actions, preserved unchanged from existing, already-frozen governance (Section 5, items 2–7):

- **REMEMBER** — required exactly when the owner gives a direct, unambiguous instruction to remember a specific, stated fact. The classification carries no confidence or evidential-weight judgment (Section 4).
- **GOAL** — required exactly when the owner requests work requiring planning, execution, tools, later action, or multiple coordinated steps.
- **REPLY** — required for ordinary conversational input, and is the constitutionally correct fallback whenever genuine doubt exists about REMEMBER or GOAL intent.
- **NOACTION** — required only where the reasoning boundary can confidently determine neither a reply nor a goal is warranted; not a default for uncertainty or brevity.

**Semantic correctness** (did the reasoning boundary select the action a reasonable, informed reader of the owner's actual words would select) and **representation correctness** (was that selection expressed in a form the protocol can parse) are required as two independently enforceable properties, exactly as Unit 2 and Unit 2-D established empirically:

- A syntactically valid output selecting the wrong action is **not conformant** — representation validity never excuses semantic error.
- A correctly-intended action expressed in an invalid or unparseable form is also **not conformant** — semantic intent never excuses representation failure.

No future measurement, remedy evaluation, or qualification report may collapse these two properties into a single pass/fail score.

## 7. False-positive safety

`REMEMBER` and `GOAL` receive special constitutional treatment because a false positive on either creates real downstream consequences (Memory admission; goal/planning pursuit) that a false positive on `REPLY` or `NOACTION` does not.

**Required:** zero *observed* false-positive `REMEMBER` and `GOAL` classifications across the full negative/adversarial exposure set at qualification tier (Section 4). This requirement is **not weakened, not strengthened, and not reinterpreted** here — it is restated exactly as the programme Planning Review froze it.

**Precision required by this Scope Lock, not previously stated explicitly:** "zero tolerated by the contract" and "statistical proof the true probability is literally zero" are different propositions, and this contract asserts only the former. The qualification regime requires that the *observed* exposure set (at least the pre-registered, ≥300-minimum sample) show zero such events before a configuration may be treated as meeting this requirement — exactly as the 300-exposure rationale itself states ("This does not prove impossibility; it makes the residual statistical claim explicit," Programme Planning Review §6). Any future remedy evaluation must report results in this same, explicit, non-overclaiming form.

No permissible non-zero false-positive rate is invented here. If governance ever wishes to accept a non-zero rate for either action, that requires its own separate, explicit governance act — not a Unit 3-A default.

## 8. Content fidelity

After the correct semantic action is selected, the required externally observable property is: the content attached to that action must be a faithful representation of the owner's actual words — exact retention is the target, non-material paraphrase is permitted and separately measured, and material mutation or invention is zero-tolerance at qualification tier (Section 4, item 9; Section 5, item 9).

This applies to `REMEMBER` (the proposition to be remembered), `GOAL` (the work requested), and `REPLY` (the substance of what is being responded to, where a fixture defines an expected content constraint). No extraction algorithm, prompt format, structured-output scheme, deterministic parsing rule, semantic-repair mechanism, or rewriting technique is prescribed anywhere in this section — only the externally observable property the eventual mechanism must achieve.

## 9. Ambiguity and uncertainty

The contract does not, and constitutionally must not, require Parker to infer unknowable owner intent. Where genuine ambiguity exists, the required behavior is: decline the consequential action (`REMEMBER`/`GOAL`) and select `REPLY` or `NoAction` instead (Section 6). This is the entirety of what existing governance supports.

Whether the reasoning boundary must, may, or must not actively request clarification from the owner in ambiguous cases is explicitly **not decided here** — it is recorded as a deferred governance question (Section 5, item 11). No universal "always ask for clarification" rule is introduced by this Scope Lock; doing so would itself be a mechanism decision this document is not authorized to make.

## 10. Failure contract

Required externally observable behavior, mechanism-free:

- **Malformed reasoning output / parser failure** — must be recorded as a representation failure, never silently repaired, never scored as a semantic answer.
- **Timeout** — must be recorded as a distinct operational outcome, never as a semantic answer; the specific production timeout value remains unresolved (Section 5, item 12).
- **Transport failure** — must be recorded as a distinct operational outcome, never as a semantic answer.
- **Provider unavailable** — behavior required here is currently undefined; deferred (Section 5, item 14).
- **Model unavailable** — same as provider unavailable, where distinct; deferred.
- **Unqualified model/provider** — deployment must fail or warn closed rather than silently proceed; the specific fail-vs-warn choice remains deferred (Section 5, item 15).
- **Internal classification uncertainty** — must resolve to the non-consequential fallback (`REPLY`/`NoAction`), never to a guessed consequential action, exactly as Section 9 requires.

No specific technical remedy — retry, repair, fallback model, second model, structured output, rule engine, or prompt rewrite — is required, implied, or prescribed by any bullet above. None of the mechanisms enumerated in Section 13 is already constitutionally required by cited governance for any of the failure modes above; where cited governance already constrains a mechanism's permissible behavior (Section 4's escalation-prevention row), that constraint is recorded there, not selected or foregrounded here.

## 11. Model/provider qualification

The reliability contract attaches at **both** constitutional levels, per the frozen governance already cited:

- **Universally, to the reasoning boundary** — the required behaviors in Sections 6–10 apply regardless of which model or provider is in use.
- **Individually, to each specific model/provider/configuration** — qualification (meeting the Section 4/5 thresholds) attaches to an immutable digest and configuration, not to a mutable model name, and must be re-established for any change (Programme Planning Review §10, §11).

A model is not qualified merely because it passed a single, isolated execution — qualification requires the full blind-corpus, per-stratum process already governed (Programme Planning Review §11). Changing models is not, by itself, a remedy; it is one candidate family among several (Unit 3 Planning Review, family 7), subject to the same qualification obligation as the current configuration. This Scope Lock defines the qualification *obligation*; it does not select a qualification *mechanism* beyond what is already frozen, and it does not evaluate or endorse any specific alternative model.

## 12. Measurement and conformance contract

Before any reasoning configuration may be declared conformant, it must be demonstrated, using the already-governed two-stage design (Section 4, row 1):

- against the frozen fixture corpus and representative input classes (Programme Planning Review §4);
- across repeated trials at the appropriate tier (≥30/cell characterisation; qualification sample size pre-registered from the acceptance threshold, ≥300 for critical zero-event gates);
- with semantic classification and representation classification reported and reasoned about separately (Section 6);
- with content fidelity reported per Section 8;
- with false-positive exposure and zero-event evidence reported per Section 7;
- through the actual, unmodified production prompt/provider/parser chain, not a hand-maintained imitation (Programme Planning Review §2, reaffirmed by every subsequent unit's own production-path-fidelity requirement);
- with full artifact integrity (hash/size/line-count verification, exactly as Units 1, 2, and 2-D already practiced);
- with reproducibility measured via repeated identical trials;
- with model identity and runtime/configuration identity pinned and recorded;
- with regression checked against every currently-correct action, not only the action under investigation.

Where a threshold is not already frozen by cited governance (Section 4), it is recorded here as **unresolved**, not invented: the specific numeric confidence requirement or lower-confidence-bound target for any *new* mechanism-specific metric a future remedy family might introduce; the specific production timeout value; the fail-vs-warn choice on qualification mismatch. Unit 3-A closes with these gaps explicit rather than fabricating numbers to complete the table.

## 13. Remedy-neutrality firewall

This contract must remain equally capable of evaluating any future remedy family without favoring one. It does not privilege: decision/rendering separation; structured/schema-constrained output; prompt/protocol redesign; deterministic/rule-assisted classification; retry; semantic repair; inference-control changes; model substitution; multi-model strategies; or hybrid strategies. None of those terms describes a requirement anywhere in Sections 6–12 — each section was independently checked against this list during drafting.

Unit 2-D's DQ5 result (0/18 pooled joint-task vs. 2/5 decision-only correct `REMEMBER` selections) is cited exactly once in this document (here, as diagnostic background) and nowhere converted into a constitutional preference. The programme Planning Review's own observation (§9) that a future structured transport "need not change the domain contract" is noted here solely as evidence about *domain-contract stability* — not as an endorsement of adopting structured output — and is not otherwise relied upon elsewhere in this document. No section of this document uses the words "preferred," "best," "leading," "highest priority," or "recommended architecture" for any remedy family — checked explicitly against the full document text before completion.

This Scope Lock defines the examination standard every future remedy family must be measured against. It does not, and constitutionally must not, declare a winner.

## 14. Downstream authority boundary

Unit 3-A changes no authority held by Memory, Goals, Knowledge Submission, planning, execution, tools, permission policy, authorization purpose, evidence systems, or any other consequential downstream coordinator. The reliability contract governs what may validly *emerge* from the reasoning boundary — it does not expand what downstream systems may *do* with a valid result. A correct `REMEMBER` classification, meeting every requirement in this document, still does not itself grant memory-write authority; it remains subject to exactly the same downstream permission and admission controls that govern it today, entirely unchanged by anything in this Scope Lock.

## 15. Exit criteria

Unit 3-A is complete once:

1. every contract dimension (Section 5) is classified;
2. every inherited numerical threshold is traced to authoritative frozen governance (Section 4);
3. unresolved thresholds are explicitly recorded, not invented (Sections 5, 10, 12);
4. deferred governance questions are explicitly recorded (Sections 5, 9, 10);
5. semantic correctness is defined independently of representation correctness (Section 6);
6. false-positive safety is preserved unweakened and unstrengthened (Section 7);
7. content fidelity is addressed without prescribing mechanism (Section 8);
8. failure behavior is defined without prescribing mechanism (Sections 9–10);
9. model/provider qualification obligation is defined without selecting a qualification mechanism (Section 11);
10. measurement/conformance obligations are defined, inheriting rather than inventing thresholds (Section 12);
11. remedy neutrality is demonstrated by explicit check (Section 13);
12. downstream authority is confirmed unchanged (Section 14);
13. no remedy is selected anywhere in this document;
14. an Independent Constitutional Review is completed and reaches an accepting verdict.

Unit 3-A may close with explicitly unresolved questions (Sections 5, 9, 10, 12) where those questions properly belong to Unit 3-B or later governance. This is a valid, complete closure state, not a deficiency requiring invented answers.

## 16. Disposition

```text
PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```

Not frozen. Implementation planning, experiments, remedy comparison, remedy selection, live execution, and Unit 3-B/3-C/3-D/3-E/Unit 4 work of any kind remain unauthorized pending that review and any further required governance.
