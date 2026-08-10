**Status:** Unit 3-B — Remedy Experiment Scoping — Scope Lock — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** Freezes the purpose, remedy-family classification, and experiment-governance architecture that any future Unit 3-C controlled remedy experiment must satisfy. Contains no implementation, no experiment execution, no remedy selection, and authorizes no live model call.

# Reasoning Protocol Live-Model Conformance Unit 3-B — Remedy Experiment Scoping — Scope Lock

## 1. Baseline

Drafted against committed baseline `05a3338521e7b5a4d832d0e76c5e699b986dff29`. Controlling authority: the frozen Unit 3-A Reliability Contract Definition Scope Lock (`ab27f18`), its two accepted Independent Constitutional Reviews, and the committed Unit 3-B Remedy Experiment Scoping Planning Review (part of `05a3338`), which is treated here as evidence and analysis, not as itself a frozen instrument. Where this Scope Lock adopts a Planning Review conclusion, it does so because independent re-reading of primary governance during drafting found no constitutional defect requiring departure; where a defect exists, it is noted rather than silently propagated (none was found — see Section 15).

## 2. Purpose and unit boundary (Phase 3.A)

Unit 3-B is experiment-governance work. Its sole output is the lawful experimental envelope — which remedy families may be experimented on, and under what architecture — within which future Unit 3-C work must operate. Unit 3-B performs no experiment, no comparison, no selection, and no implementation.

The five-part structure is frozen as binding:

| Unit | Function | Authorized by |
|---|---|---|
| 3-A | Reliability contract definition | frozen, `ab27f18` |
| 3-B | Experiment governance / experiment scope (this document) | this Scope Lock, pending its own Independent Constitutional Review |
| 3-C | Controlled remedy experiments | a future Scope Lock per experiment/family, operating only within 3-B's envelope |
| 3-D | Comparative evaluation of 3-C's results against the full Unit 3-A contract | a future unit, not this one |
| 3-E | Remedy selection — a governance document, not code | a future unit, not this one |
| Unit 4 | Implementation of whatever 3-E selects | outside Unit 3 entirely, reserved by the programme-level Planning Review |

No later-unit authority leaks backward into this document: this Scope Lock does not design, schedule, or pre-approve any specific Unit 3-C trial; does not evaluate any result (none exists); does not select or prefer a remedy; and does not authorize Unit 4 implementation work of any kind.

## 3. Remedy-family classification (Phase 3.B)

The classifications below are frozen, carried forward from the committed Unit 3-B Planning Review. Fresh re-reading of primary governance and the Unit 2-D raw evidence during drafting of this Scope Lock found no constitutional defect in any of the nine determinations — each remains evidence-traceable, and none constitutes or implies remedy selection.

**INCLUDED FOR EXPERIMENTAL SCOPING:**
- **A — decision/rendering separation.** Evidence basis: Unit 2-D DQ5 (0/18 pooled joint-task vs. 2/5 decision-only correct REMEMBER on the principal direct fixture).
- **B — prompt/protocol redesign.** Evidence basis: current production prompt already contains a matching worked example and still misses; one documented precedent of a prior successful revision for a different symptom.
- **C — deterministic or rule-assisted handling of constitutionally explicit cases.** Evidence basis: DQ1's high (9/10) repeatability on a fixture chosen for its unambiguity. Subject to the mandatory safety boundary in Section 4.

**DEFERRED:**
- **D — structured/schema-constrained model output.** Targets representation validity, an axis already measured as high (22/24 representation-valid in Unit 2-D); not contraindicated, lower priority than the dominant unresolved semantic-failure mode.
- **E — inference-control changes.** Genuinely untested; DQ1's own consistency is a weak prior against stochasticity being the dominant driver.
- **G (representation-only retry).** Architecturally narrow and low-risk, but zero representation-failure-triggered scenarios occurred in Unit 2-D to motivate it yet.
- **I — hybrid approaches.** Premature ahead of its constituent families' own individual results.

**EXCLUDED ON CURRENT EVIDENCE, EXPRESSLY REVISABLE BY FUTURE GOVERNANCE:**
- **F — model/provider substitution as a near-term remedy experiment.** Not architecturally prohibited (full qualification is already governed by Unit 3-A Section 11); excluded near-term because the evidence bar to even begin (full blind-corpus qualification) is disproportionate to the single doubly-confounded data point (DQ4) currently available.
- **G (semantic retry/repair).** Both DQ1's own evidence and existing escalation-prevention governance (Unit 3-A Section 4) argue against including this in a first experimental wave.
- **H — multi-model/fallback strategies.** No design exists to isolate; complexity disproportionate to any evidence motivating it.

**Binding interpretive rule:** "Included" does not mean preferred, does not mean expected to succeed, and does not mean any priority ordering exists among A, B, and C. "Deferred" does not mean rejected, disproven, or permanently lower-value — it means insufficient current evidence to schedule ahead of the included families, revisable at any time new evidence arises. "Excluded on current evidence" does not mean permanently prohibited — it means the current evidence-to-cost ratio does not justify scheduling now, and any future governance document may revise this determination on fresh evidence. No remedy winner exists, is implied, or may be inferred from this classification.

## 4. Family C safety boundary (Phase 3.C)

Deterministic or rule-assisted handling of constitutionally explicit cases receives mandatory, non-optional adversarial treatment beyond that required of any other included family, because its own failure mode — brittle pattern/keyword matching — is structurally capable of producing exactly the false-positive REMEMBER/GOAL events Unit 3-A Section 7 gives zero-tolerance treatment.

**Requirement:** Family C shall not be considered to have demonstrated success by correctly recognizing an unambiguous, canonical instruction such as "Remember that X." Correct handling of the easy explicit case is a necessary, not sufficient, condition. Any future Unit 3-C experiment involving Family C must, at minimum, exercise experiment design capable of distinguishing the model's or rule's behavior across:

- a direct explicit REMEMBER instruction;
- an ordinary statement of fact that happens to contain memory-related vocabulary, without instructing retention;
- a question *about* remembering, rather than an instruction to remember;
- a quoted REMEMBER instruction (reported speech, not an instruction to Parker itself);
- a hypothetical REMEMBER instruction ("if you were to remember...");
- a negated or explicitly discussed instruction ("don't remember that," or discussion of what remembering would mean);
- a conversational mention of memory with no operative instruction;
- GOAL-like language containing vocabulary overlapping with REMEMBER's own trigger surface;
- ambiguous-boundary cases not cleanly resolved by any of the above categories.

This Scope Lock freezes the *requirement* for this adversarial negative coverage as a binding precondition on any Family C experiment; it does not invent, select, or freeze the exact fixture corpus used to exercise it, beyond what the already-frozen Unit 2 23-fixture corpus already provides (Section 8, item 2 references specific existing fixture identifiers where they are already established governance; no new fixture identifier is invented by this document). A Family C experiment that has not exercised this negative surface has not satisfied this Scope Lock's requirements and may not proceed to Unit 3-D comparative evaluation.

The false-positive REMEMBER and GOAL safety requirements already frozen by Unit 3-A Section 7 remain fully controlling over Family C and are not weakened, narrowed, or made conditional by this Section in any way.

## 5. Experiment architecture (Phase 3.D)

The following twenty-three requirements, inherited from the Planning Review's own architecture proposal and independently re-verified during drafting, are frozen as binding on any future Unit 3-C experiment operating under this envelope. Where the Planning Review left a number unresolved, it remains unresolved here; none is invented by this Scope Lock.

1. **Control/baseline:** every experiment must include a control arm using the exact, unmodified production reasoning path, run under identical fixtures, context profile, model, and commit as the candidate arm.
2. **Fixture reuse:** the frozen Unit 2 23-fixture corpus is the required primary basis for any experiment.
3. **New-fixture justification:** any fixture not already part of the frozen Unit 2 corpus requires its own governance review before use in any Unit 3-C experiment; no new fixture is authorized by this document.
4. **Explicit REMEMBER cases:** experiments touching REMEMBER behavior must exercise the full set of frozen explicit-REMEMBER fixtures already defined by the Unit 2 corpus, not a single fixture in isolation.
5. **Ordinary-fact controls:** experiments must include the frozen ordinary-fact negative fixture(s) as false-positive controls.
6. **GOAL false-positive controls:** experiments touching GOAL behavior must include the frozen negative-GOAL fixtures distinguishing discussion of a task from an instruction to perform it.
7. **REPLY:** experiments must include multiple frozen REPLY fixtures as a regression baseline, not a single fixture.
8. **NOACTION:** experiments must include the frozen NOACTION fixtures, with particular attention to representation-validity behavior given Unit 2-D's own observed representation failure on this action.
9. **Ambiguous-boundary cases:** experiments — mandatorily for Family C, and required wherever applicable for Families A and B — must include the frozen corpus's adversarial and boundary-case fixtures.
10. **Content fidelity:** measured per Unit 3-A Section 8's already-frozen property; not redefined by this document.
11. **Representation validity:** measured per Unit 3-A Section 6/8's already-frozen property, independently of semantic correctness; not redefined here.
12. **Semantic action selection:** measured per Unit 3-A Section 6's already-frozen property, independently of representation validity; not redefined here.
13. **Material mutation/invention:** zero-tolerance per Unit 3-A Section 4/8, unchanged and unweakened by any experiment.
14. **Model/provider qualification:** any experiment introducing a model or provider not already qualified under Unit 3-A Section 11 must complete that full governed qualification process; no experiment-scale shortcut is authorized.
15. **Context profiles:** experiments reuse Unit 1's frozen context profiles; expanding coverage beyond what a specific experiment's own scope requires is a per-profile justification, not a default.
16. **Inference configuration:** preserved unchanged from the production default for Families A, B, C, and D experiments; any Family E experiment isolates inference-configuration change as its own single variable, never combined with any other family's change in the same trial.
17. **Repetition:** the exact repeat count per experimental cell is left **unresolved** by this Scope Lock. Any future Unit 3-C Scope Lock must state and justify its own repeat count; exploratory-tier experiments are expected to use triage-grade, bounded counts (not qualification-scale ≥30/cell or ≥300-exposure sampling) unless and until a specific candidate is escalated toward qualification.
18. **Statistical interpretation:** exploratory-tier results must be reported with the same epistemic humility already required of Unit 2-D's own interpretation — no population-rate claim may be drawn from a small sample.
19. **Artifact integrity:** full hash, size, and structural verification of every produced artifact, mirroring the practice already established across Units 1, 2, and 2-D.
20. **Exact-once execution:** any future experiment driver must implement the same durable ledger/checkpoint/intent-record discipline already implemented for Unit 2-D, not a weaker mechanism.
21. **Stop conditions:** see Section 12 below.
22. **Downstream isolation:** see Section 13 below.
23. **Cross-family comparison discipline:** see Section 9 below.

## 6. Evidence tiers (Phase 3.E)

Three evidence tiers are frozen as non-interchangeable:

1. **Exploratory evidence** — small-sample, triage-grade evidence of the kind Unit 2-D itself produced (DQ1–DQ5). Its sole legitimate use is to decide whether a remedy family deserves further investment. Exploratory evidence may never, by itself: qualify a remedy; establish production reliability; authorize production implementation; or declare a winning remedy.
2. **Qualification evidence** — large-sample, pre-registered, zero-event-gated evidence meeting the thresholds already frozen by Unit 3-A Section 4 (≥30 trials per characterisation cell; ≥300-exposure zero-event qualification gates; the associated one-sided 95% bound). Required before any remedy may be selected.
3. **Production-selection evidence** — not itself a data-collection activity. It is the governance determination made by Unit 3-E, informed by qualification-tier evidence delivered through Unit 3-D's comparative evaluation.

Unit 3-C is expected to produce exploratory and, where a specific family is escalated under its own further governance, qualification-tier evidence. Unit 3-D owns comparative evaluation of that evidence against the full Unit 3-A contract. Unit 3-E owns remedy selection. Unit 4 owns implementation. No tier substitutes for another, and no experiment authorized under this Scope Lock's envelope may be represented as satisfying a tier it does not meet.

## 7. DQ5 boundary (Phase 3.F)

The accepted Unit 2-D interpretation of DQ5 is frozen as controlling: DQ5 (0/18 pooled joint-task vs. 2/5 decision-only correct REMEMBER) provides evidence sufficient to justify experimental investigation of decision/rendering separation (Family A) under this envelope. It does not establish, and may not be cited as establishing, that decision/rendering separation is preferred over Families B or C; that it is sufficient to satisfy the Unit 3-A contract; that it will satisfy Unit 3-A; that it is production-ready in any sense; that it should be implemented; or that its 2/5 result is comparable in evidentiary weight to qualification-tier evidence. Any future Family A experiment authorized under this Scope Lock must test the full applicable Unit 3-A contract (semantic accuracy, false-positive resistance, representation validity, content fidelity — not PF01 improvement alone) as a binding condition of that experiment's design.

## 8. Unit 3-A traceability (Phase 3.G)

Every future remedy experiment authorized under this Scope Lock must identify, in its own governing document, which frozen Unit 3-A contract dimensions it tests. A remedy may not be considered successful, promising, or worthy of escalation merely because it improves performance on the original REMEMBER fixture (PF01) in isolation; improvement must be evaluated against the dimension(s) it claims to address, and any regression on an unrelated dimension must be reported, not omitted.

The following Unit 3-A contract dimensions are preserved as unresolved by current governance and are **not** resolved by this Scope Lock:

- **#11 — ambiguous owner input / clarification-seeking.**
- **#12 — timeout behaviour (production timeout value).**
- **#14 — unavailable provider/model behaviour.**
- **#15 — unqualified provider/model, fail-versus-warn choice.**

No numerical threshold is invented anywhere in this document to close any of these gaps.

## 9. Semantic/representation separation (Phase 3.H)

Frozen, unweakened from Unit 3-A Section 6: semantic action-selection correctness and representation validity remain independently measured axes in any future experiment. A remedy that improves output syntax while continuing to select the wrong action has not repaired the semantic failure and may not be reported as having done so. A remedy that improves semantic selection while producing invalid representation has not satisfied the reasoning protocol and may not be reported as having done so. Neither axis may be used to conceal, offset, or average away a deficiency on the other; no future experiment's report may present a single blended score in place of both.

## 10. Comparison discipline (Phase 3.I)

Frozen as binding on any future Unit 3-C experiment and any Unit 3-D comparison built from it:

- fixture sets compared across arms or families must be identical in composition and count — unequal fixture sets may not be casually compared;
- any observed effect must be attributed only to the single variable an experiment actually isolates — a model/provider change may not be attributed to a prompt or decision-separation remedy, or vice versa;
- inference-configuration changes must never be silently mixed with prompt or architecture changes within the same trial;
- where multiple mechanisms operate jointly (e.g., a hybrid), no individual mechanism may be credited with an effect attributable only to the combination;
- selective reporting of favorable trials while omitting unfavorable ones from the same experiment is prohibited;
- exploratory-tier observations must never be reported or treated as statistical proof of a population rate;
- no model or provider may be ranked against another from evidence below the qualification tier defined in Section 6.

## 11. Artifact and execution integrity (Phase 3.J)

Frozen as governance-level obligations that any future Unit 3-C Scope Lock and Implementation Plan must address, without this document implementing any of them:

- campaign identity distinct from and never nested inside the closed Unit 2 or Unit 2-D campaign artifacts;
- model/provider identity verification, matching the digest-confirmation discipline already used in Unit 2-D;
- repository and configuration identity verification prior to any live execution;
- immutable, unmodified preservation of raw observations;
- exact-once accounting of every model call;
- checkpoint/recovery semantics sufficient to resume without duplicate or lost observations, where an experiment's scale warrants it;
- explicit sealing/halting semantics marking an experiment's artifact set closed;
- independent hash verification of every produced artifact;
- a full accounting of every execution call made, cross-checked against the intended schedule;
- fail-closed handling of any measurement-invalidating defect discovered during or after execution.

None of these obligations is implemented, scheduled, or partially satisfied by this document; they are frozen as requirements a future Unit 3-C governance chain must independently satisfy, exactly as Unit 2-D's own chain was required to.

## 12. Stop conditions (Phase 3.L)

Frozen distinction, carried forward from Unit 2-D's own precedent: a remedy producing poor semantic results during an experiment is evidence, not a defect, and must not by itself halt or invalidate the experiment. By contrast, harness corruption, model/provider identity mismatch, repository or configuration drift, artifact-integrity failure, any unauthorized downstream action, or any other measurement-invalidating condition must fail closed exactly as Unit 2's own Stage 0 gate and Unit 2-D's own hard-stop conditions required. This Scope Lock does not itself recreate Unit 2's specific Stage 0 gate mechanism for any future experiment; whether and how a particular Unit 3-C experiment needs an analogous gate is left to that experiment's own governing Scope Lock, to be justified on its own facts rather than inherited automatically.

## 13. Downstream isolation (Phase 3.K)

Frozen, absolute, unweakened: no experiment authorized under this envelope, regardless of remedy family, may produce output with any path to Memory admission, Goal creation, planning, tool invocation, external communication, evidence mutation, production state mutation, or any other consequential downstream execution. Every future Unit 3-C experiment observes reasoning behavior only and terminates strictly at the recorded observation, exactly as Unit 2-D's own driver was required to and independently verified to do.

## 14. Unresolved questions (Phase 3.M)

Preserved, explicitly not resolved by this document:

- the exact exploratory-tier repeat count for any specific Family A, B, or C experiment (Section 5, item 17);
- the exact mandatory adversarial fixture set for Family C beyond the categories required by Section 4 (the specific fixture identifiers to be drawn from, or added to, the frozen Unit 2 corpus are not enumerated here);
- whether Family C requires a stricter qualification bar than Families A or B, given its materially higher constitutional risk profile (Section 4);
- the four Unit 3-A contract dimensions listed in Section 8 (#11, #12, #14, #15);
- any mechanism-specific measurement requirement not yet derivable from currently frozen governance.

These remain unresolved. This Scope Lock does not manufacture an answer to any of them.

## 15. Prohibited interpretations (Phase 3.N)

This Scope Lock may not be read as:

- selecting Family A, B, or C as Parker's future architecture;
- preferring Family A because of DQ5's evidence;
- endorsing deterministic or rule-assisted classification as safe or desirable;
- permanently rejecting structured/schema-constrained output (Family D remains deferred, not excluded);
- ranking Qwen against Llama, or any model against any other;
- authorizing any change to `DefaultReasoningPromptBuilder`, any other production prompt, or any production code of any kind;
- authorizing Unit 3-C execution of any experiment;
- authorizing any live model call;
- authorizing Unit 3-D comparative evaluation of any result;
- authorizing Unit 3-E remedy selection;
- authorizing Unit 4 implementation of any kind.

## 16. Exit criteria

This Scope Lock is ready to freeze once it has passed Independent Constitutional Review with a verdict of ACCEPTED or ACCEPTED WITH QUALIFICATIONS whose qualifications are non-blocking. A verdict of TARGETED AMENDMENT REQUIRED or REJECTED means this document is not ready to freeze and must be corrected, in a separate task, before any future Unit 3-C work may cite it as controlling.
