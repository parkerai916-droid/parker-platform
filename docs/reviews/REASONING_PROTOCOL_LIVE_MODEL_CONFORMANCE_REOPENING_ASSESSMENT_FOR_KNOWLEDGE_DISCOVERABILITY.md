**Status:** Reopening Assessment — **PLANNING REVIEW ONLY. DOES NOT REOPEN THE PROGRAMME. NO REMEDY SELECTED. NO IMPLEMENTATION AUTHORIZED. NO MODEL RUN AUTHORIZED.** Against committed baseline `081858d02a03900b99040bf32b1b434555037d36` (`HEAD` == `origin/main`, clean). This document determines whether Knowledge Discoverability's newly-established, concretely-demonstrated downstream blocking dependency on the paused Reasoning Protocol Live-Model Conformance programme is sufficient justification to reopen that programme, and, if so, identifies the single smallest lawful next governance action — without itself performing the reopening act, selecting a remedy, drafting a Scope Lock or Implementation Plan, authorizing implementation, or running a model.

# Reasoning Protocol Live-Model Conformance — Reopening Assessment for Knowledge Discoverability

## 1. Status and scope

This is a documentation-only Planning Review of a **separate, pre-existing programme** (Reasoning Protocol Live-Model Conformance and Structured-Output Reliability), triggered by a dependency discovered from a **different programme** (Knowledge Discoverability and Reasoning Context). It does not belong to either programme's own internal unit numbering. It creates no new gap number and no new programme identity. It performs exactly one function: determine whether reopening is justified and, if so, name the smallest next lawful governance step — nothing more.

**This document does not:** select a remedy; authorize any production, test, or build file change; authorize any live model call; authorize a third Knowledge Discoverability live-verification attempt; lower the Unit 3-E selection bar; modify, pool, or reinterpret any existing campaign evidence; or itself change the programme's status from PAUSED to any other state. Where this document recommends reopening, that recommendation is advisory to a future, separate governance act (Section 10, Section 11).

## 2. Baseline

```text
git rev-parse HEAD    = 081858d02a03900b99040bf32b1b434555037d36
git rev-parse origin/main = 081858d02a03900b99040bf32b1b434555037d36
git status --short --branch = ## governance/reasoning-protocol-reopening-assessment (clean)
```

No model process, campaign, experiment, or implementation action occurred before or during this task. Durable Knowledge Discoverability live-verification evidence independently re-hashed at the start of this task and found unchanged:

```text
parker-unit5-live-attempt-1-negative-report.txt = 4e8b150e66f6040848fbe3cdb6900eae2ac22995091aa1e743c3cad17c6af449
parker-unit5-live-attempt-1-negative.tar.gz      = 0806b648446a796cd52b46171bc134eef5f21a6f6c78cc2e1765dcc93218dc43
parker-unit5-live-attempt-2-negative-report.txt = 63be617fa2c5edb6199419c033e45a965dcca95293d8b342722bba4bfd6be218
parker-unit5-live-attempt-2-negative.tar.gz      = b234b293d8bb9276da920724784982f799260b71af8e797cd3a8f6e7700d0357
```

## 3. Documents read in full for this task

Knowledge Discoverability side: `docs/reviews/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_LIVE_VERIFICATION_ATTEMPTS_1_2_REVIEW.md` (commit `1152280f6d533cd966a7ea2d13fe2841af47e0d8`) and its `..._INDEPENDENT_REVIEW.md`; `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_SCOPE_LOCK.md` §13; `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_IMPLEMENTATION_PLAN.md` §20.

Reasoning Protocol Live-Model Conformance side, read fresh, in full, from primary text (not from any prior summary): the top-level `REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_AND_STRUCTURED_OUTPUT_RELIABILITY_PLANNING_REVIEW.md` (`b34f8d0`); Unit 2 Baseline Characterisation Scope Lock and Unit 2-D Diagnostic Characterisation Scope Lock plus its Independent Constitutional Review; Unit 3-A Reliability Contract Definition Scope Lock; Unit 3-B Remedy Experiment Scoping Scope Lock (`55af571`); Unit 3-D Comparative Evaluation Review (`2e468ed`); Unit 3-E Remedy Selection Review (`23b500f`); Post-Selection Disposition Planning Review (`e40c5a5`); Family A Decision-Step Evidence Extension Planning Review and its Independent Constitutional Review (`0d9ae8b`); Programme Disposition Closure Review and its Independent Constitutional Review.

Production source, re-inspected directly at this task's own baseline: `src/runtime/ReasoningPromptBuilder.kt`, `src/runtime/ReasoningResponseParser.kt`, `src/runtime/ModelInferenceClient.kt`, `src/runtime/ModelReasoningProvider.kt`, `src/runtime/ConversationReplyCoordinator.kt` — confirmed unchanged from the versions read during the Knowledge Discoverability live-verification review; the `git log -1` for each remains a pre-programme or in-programme commit that predates `b34f8d0`, independently re-confirmed via `git log --oneline -- <path> | tail -1` and `git merge-base --is-ancestor`.

Where any document not read in full for this task is cited, it is cited only for a proposition that a document actually read in full for this task independently restates and traces to the same primary source (e.g., Unit 3-C Attempt 6 figures, re-derived independently three separate times across Unit 3-D's Review, Unit 3-E's Review, and their own Independent Reviews, all reproducing identical numbers).

## 4. The downstream dependency, stated precisely

Knowledge Discoverability's Implementation Plan §20 requires, as a non-waivable step in its own Final Programme Evidence Sequence, "real, same-runtime live verification of the required end-to-end proof" (Scope Lock §13, §2), which explicitly states "a friendly reply alone is insufficient." Two independent live attempts against genuine local Ollama + `llama3.2:3b` on this machine both failed to produce that proof:

- **Attempt 1** (commit baseline `7ad3afa`): raw model response not captured by tooling; `TAG_SELECTION=UNOBSERVED`; no promotion observed; `CAUSE=UNDETERMINED`.
- **Attempt 2** (commit baseline `7ad3afa`): raw model response directly captured for both turns; both selected `REPLY:`, not `REMEMBER:`, for a direct "Remember that X" instruction and a follow-up recall query.

The accepted factual review and its Independent Review (Section 4 there) trace this exact failure pattern — a direct, unambiguous "Remember X" instruction not being classified `REMEMBER` — to `PF01`, mapped to fixture `R01-direct` in the Unit 2 Baseline Characterisation Scope Lock, the founding problem of the Reasoning Protocol Live-Model Conformance programme. Knowledge Discoverability's own governance chain never claims authority to resolve reasoning-boundary reliability; it is a downstream consumer of the reasoning boundary's output, not an owner of it.

**This is a new fact, not previously present in any document in the Reasoning Protocol Live-Model Conformance programme's own history:** a second, separate, independently-governed programme is now concretely, demonstrably blocked — not hypothetically, not by design intent, but by a real, dated, evidence-preserved operational failure — by exactly the defect this programme exists to address.

## 5. Determination 1 — does this satisfy the reopening condition?

The Programme Disposition Closure Review §16 states future reopening conditions are "illustrative, non-exhaustive, non-authorizing examples," explicitly including as its seventh and final bullet: "**Another specific, precisely-stated development** — this list is illustrative, not closed; a future task proposing reopening on a basis not named here must still state its own specific justification, not merely cite the existence of this list." None of the six named examples (new model capability, new evidence on existing candidates, a freshly-justified deferred/excluded family, resolved `contentFidelity`, a non-outcome-driven bar revision, or changed hardware economics) literally describes a downstream consumer becoming blocked. This dependency is evaluated against the seventh, catch-all bullet, and against §17's prohibited-grounds list (desire for a winner; passage of time alone; unmodified rerun; lowering standards; least-bad selection; untested-component combination; absence-of-new-evidence-as-trigger; "implementation pressure from Unit 4") — none of which this dependency resembles. It is not implementation pressure from this programme's own Unit 4 (Unit 4 has no standing authority and is not the source; Knowledge Discoverability is a wholly separate, independently-governed programme). It is not a desire for a winner, not a rerun, not a bar-lowering request, and it does not ask to select a remedy.

```text
REOPENING_TRIGGER=SATISFIED — a specific, precisely-stated, evidence-preserved, dated downstream operational consequence (Section 4) qualifies under the Disposition Closure Review §16's own catch-all bullet and is not excluded by any §17 prohibited ground.
```

This determination concerns only whether the *condition* for considering reopening is met — it is not itself the reopening act (Section 10).

## 6. Determination 2 — admissibility of Attempts 1 and 2

**Admissible only as new operational-impact/trigger evidence. Not admissible as remedy-comparison evidence, and never to be pooled into any existing campaign figure.**

Reasoning, applying the Unit 3-B Scope Lock §10 comparison discipline (fixture sets must be identical across compared arms; effects attributed only to the single isolated variable; "no model or provider may be ranked against another from evidence below the qualification tier") and the Unit 3-D/3-E provenance-separation practice (Family C's live/offline split; Attempt 6 vs. any future extension) as the controlling precedent, even though neither document was written contemplating Knowledge Discoverability specifically:

- Attempts 1–2 were not executed under any Unit 3-C-tier campaign ledger, fixture-ID assignment, or adversarial-category tagging. They carry no `Unit3CObservation`-equivalent schema, no exact-once/durability harness, and no independent multi-method re-derivation of the kind every admitted Unit 3-C/3-D/3-E figure received.
- They are `n=1`-per-turn, single-session, uncontrolled for time-of-day/model-load/context-length variation against the frozen corpus, and Attempt 1 additionally has an **unobserved** raw tag (Section 4), which by itself makes it inadmissible as a semantic data point at all — only as evidence that live verification failed to complete, not evidence of *which* tag was chosen.
- Pooling either attempt into Control's own matched-subset `r01-direct` figures (Unit 3-D Review §7: Control 0/5 on `r01-direct`) would violate the explicit rule against pooling heterogeneous-provenance trials into a population rate — a rule this same programme's own Unit 2-D Independent Constitutional Review already applied to a structurally similar situation (warning against pooling DQ1/DQ3/DQ4/DQ5 into one rate).
- What Attempts 1–2 *do* legitimately establish, and are cited for in this document only: that the reasoning-boundary reliability defect this programme already characterized in a controlled harness has now manifested in a real, owner-facing, production-adjacent interactive session, with real consequences (a blocked downstream programme) — corroborating evidence of *materiality*, not new evidence of *rate*.

```text
ATTEMPTS_1_2_ADMISSIBILITY=OPERATIONAL-IMPACT/TRIGGER EVIDENCE ONLY — provenance-separated from Control/Family A/B/C's own matched-subset and full-corpus figures; never pooled; Attempt 1 additionally inadmissible even as a single semantic data point (tag unobserved).
```

## 7. Determination 3 — candidate path classification

Re-applying the Post-Selection Disposition Planning Review's own path taxonomy (§5–6 there) fresh, in light of the new trigger, without assuming any path is now authorized merely because the trigger condition (Section 5) is satisfied:

| Path | Description | Classification (unchanged by the new trigger unless noted) |
|---|---|---|
| A | Remain paused, take no action | **PERMITTED BY CURRENT GOVERNANCE, ALREADY FULLY AUTHORIZED** — remains lawful; the trigger creates a documented reason to *revisit* this choice (this document), not an obligation to abandon it. |
| B | Resume evidence work within the existing Unit 3-A–E chain on an already-tested candidate (e.g., extend Family A's decision-step exposure) | **REQUIRES NEW GOVERNANCE** (fresh Unit 3-C-tier Scope Lock/Plan/Readiness/Execution-Approval chain). The Family A Extension Planning Review already performed exactly this analysis and concluded PAUSE/CLOSURE remains proportionate **on evidentiary grounds independent of downstream stakes** (Section 9 below). |
| C | Reconsider an already-tested candidate using existing evidence only (no new live call) | **PRESERVED EVIDENCE REMAINS AVAILABLE; RECONSIDERATION ITSELF STILL REQUIRES A FRESH, EXPRESSLY AUTHORIZED GOVERNANCE ACT** — Family C's full 29-fixture profile (24/29, 4 FP, 1 FN) is complete and already incorporated into Unit 3-E's own no-selection decision (Section 8 below); that existing evidence alone already produced NO REMEDY SELECTED and supplies no automatic new decision — any renewed Unit 3-E reconsideration of Family C, or any candidate, on this same evidence requires its own fresh, expressly authorized governance act, not merely the passage of time or the existence of this Assessment. |
| D — deterministic direct-Remember only | Handling only the canonical "Remember that X" pattern deterministically | **ALREADY FAMILY C** — see Determination 4 (Section 8). Not new, not a hybrid. |
| E — prompt/protocol modification | Redesign prompt text only | **ALREADY FAMILY B** — tested, matched-subset 1/15 (or 10/25 arm-wide-with-REPLY), a real but weak improvement over Control's 0/15, already fully evaluated by Unit 3-D/3-E; reconsideration requires "new governance" per Post-Selection Disposition Path C/D classification, and no new evidence bears on it. |
| F — alternative model/configuration qualification | Try `qwen2.5-coder:7b` or another model | **REQUIRES NEW GOVERNANCE (Family F, excluded on current evidence)** — see Determination 5 (Section 9). |
| G — new architecture/hybrid outside existing authorization | Any combination of untested components | **NOT CLASSIFIED, NOT PROPOSED, NOT AUTHORIZED** — no such candidate is named or implied anywhere in this document; the Unit 3-E Scope Lock's own hybrid rule (§10) forecloses this absent a specific, separately-governed proposal, which does not exist. |

```text
FAMILY_A_STATUS=TESTED, EXPLORATORY-TIER (matched-subset decision-step 8/15 correct); FAMILY A EXTENSION ALREADY FOUND NOT PROPORTIONATE (Family A Extension Review §9), INDEPENDENTLY REVIEWED AND ACCEPTED — reasoning unaffected by the new downstream trigger (Section 9 below).
FAMILY_B_STATUS=TESTED, EXPLORATORY-TIER (matched-subset 1/15 REMEMBER-recognition, 10/25 arm-wide with REPLY fixtures); NOT SELECTED; NO EVIDENCE-DRIVEN BASIS TO RECONSIDER (Post-Selection Disposition Review §8).
FAMILY_C_STATUS=TESTED, EXPLORATORY-TIER, FULL 29-FIXTURE ADVERSARIAL PROFILE COMPLETE (live 6 + offline 23), SATISFYING UNIT 3-B §4's MANDATORY ADVERSARIAL-COVERAGE REQUIREMENT; 24/29 correct, 4 FALSE POSITIVES (p03-ambiguous-memory, p04-embedded-tags, p05-mixed-memory-discussion, p12-injection), 1 FALSE NEGATIVE (r03-dont-forget); CLEARED EVERY GATE UNIT 3-E ACTUALLY AUTHORIZED (representation validity PASS, zero false-positive GOAL PASS; parser gate NOT APPLICABLE) — NO ZERO-FALSE-POSITIVE-REMEMBER GATE WAS APPLIED TO ANY CANDIDATE (Unit 3-E Review §9/§11); NOT SELECTED, NOT ELIMINATED; its known false positives, complete absence of live-model-interaction evidence, and the highest architectural intrusiveness of any candidate are why the existing record does not support production selection — a sufficiency/proportionality finding, not a gate failure.
```

## 8. Determination 4 — deterministic direct-Remember route classification

Grounded directly in the Unit 3-B Scope Lock's own frozen §3 text: Family C is defined as "deterministic or rule-assisted handling of constitutionally explicit cases" — this is, by its own words, exactly a mechanism that deterministically recognizes direct, explicit Remember instructions. §4's own safety boundary is written specifically *about* this class of mechanism: "Family C shall not be considered to have demonstrated success by correctly recognizing an unambiguous, canonical instruction such as 'Remember that X.' Correct handling of the easy explicit case is a necessary, not sufficient, condition," requiring exercise of all nine adversarial categories before any success claim is credited.

**A "deterministic direct-Remember-only" route is not a new family and not a hybrid — it is already Family C, already governed by an existing, binding safety boundary that anticipated exactly this proposal.** Two distinct sub-cases follow:

1. **Reusing the exact, already-tested Family C classifier** (`Unit3CCandidateC1`, independently re-confirmed reproduced three separate times — Unit 3-D Review §11, Unit 3-E Review §5/§8, and this task's own primary-source read of the Unit 3-D Review) — this exact candidate has **already been run against the full nine-category corpus**, satisfying Unit 3-B §4's own mandatory adversarial-coverage requirement, and that evaluation **exposed 4 observed false-positive REMEMBER events and 1 false negative** (Section 7). Unit 3-E's own minimum-gate matrix applied no zero-false-positive-REMEMBER gate to any candidate ("imposing one would fail every candidate, including Control," Unit 3-E Review §9), and Family C cleared every gate Unit 3-E actually authorized (Unit 3-E Review §9: representation validity PASS, zero false-positive GOAL PASS, parser gate NOT APPLICABLE). Family C was not eliminated by this evidence — it was not selected. Its known, named false positives, combined with its complete absence of live-model-interaction evidence and the highest architectural-intrusiveness profile of any candidate (Unit 3-E Review §8, Family C, item 13), are the reasons the existing record does not support treating this exact candidate as a lawful basis for any production change absent first resolving those known failure modes — a sufficiency/proportionality finding about the existing record, not a gate failure.
2. **A new or more conservative Family C variant** (e.g., narrower pattern matching intended to avoid the four known false positives) — remains within Family C's already-frozen classification (no fresh Unit 3-B governance is needed to *classify* it), but **is a new, untested candidate** whose false-positive behavior on the same nine adversarial categories is unknown and cannot be asserted, inferred, or assumed safe from the existing 24/29 result, which describes a different rule set. Per Unit 3-B §4's own binding requirement, any claim about this variant's safety requires it to be run against the full adversarial corpus under fresh Unit 3-C-tier governance (Scope Lock, Implementation Plan, Readiness Review, Explicit Execution Approval) before any success or safety claim may be made.

```text
DETERMINISTIC_DIRECT_REMEMBER_CLASSIFICATION=ALREADY FAMILY C. The existing tested candidate satisfied Unit 3-B §4's mandatory adversarial-coverage requirement and, in doing so, exposed 4 observed false positives and 1 false negative; it cleared every gate Unit 3-E actually authorized (no zero-false-positive-REMEMBER gate exists in this programme) and was not eliminated, only not selected. Any new/narrower variant is a genuinely untested Family C candidate requiring fresh Unit 3-C-tier governance before any safety claim — not a lawful narrowing that inherits the existing candidate's results.
```

## 9. Determination 5 — alternative model classification

Unit 3-A Reliability Contract §11: "A model is not qualified merely because it passed a single, isolated execution... Changing models is not, by itself, a remedy; it is one candidate family among several... subject to the same qualification obligation as the current configuration." Family F (model/provider substitution) is classified **excluded on current evidence** by Unit 3-B §3, reaffirmed unrevised by the Post-Selection Disposition Review §8: "originally excluded because the qualification cost is disproportionate to the single, doubly-confounded prior data point (DQ4). No Unit 3-C experiment touched model substitution. Unchanged; not evidence-driven if reconsidered now." The Unit 2-D Scope Lock's own limitation (independently re-confirmed: `llama3.2:3b` and `qwen2.5-coder:7b` differ in both size and specialization simultaneously, so DQ4 cannot cleanly isolate either variable, and "model size is never to be read as a proxy for expected correctness") remains unaddressed by any subsequent Unit 3-C/3-D/3-E work — no experiment in this programme's entire history has ever exercised model substitution.

A single, informal, ad hoc trial of `qwen2.5-coder:7b` (or any other model) right now, outside a governed campaign, would be exactly the "single, isolated execution" Unit 3-A §11 explicitly states does not constitute qualification, would not use the frozen blind-corpus per-stratum protocol, would repeat DQ4's own already-flagged confound, and would occur while the programme remains in its no-remedy-selected, paused state with no live-call authorization active.

```text
ALTERNATIVE_MODEL_CLASSIFICATION=UNAUTHORIZED BYPASS if attempted now, informally, outside governed campaign structure. Would become LAWFUL DIAGNOSTIC EVIDENCE (not remedy comparison, not qualification) only under a fresh, explicitly-scoped Unit 3-B/3-C-tier governance chain reconsidering Family F on its own newly-stated basis (Post-Selection Disposition Review Path D: "REQUIRES NEW GOVERNANCE; CURRENTLY NOT JUSTIFIED BY EVIDENCE"). True qualification requires the full blind-corpus process (Unit 3-A §11); no model has completed that process within this programme, including the current, unmodified `llama3.2:3b` configuration, which has been characterized/evaluated only at exploratory tier, below qualification-tier thresholds.
```

## 10. Determination 6 — proportionality reassessment

The Family A Decision-Step Evidence Extension Planning Review §8 concluded PAUSE/CLOSURE remains proportionate because governance overhead for any new live campaign is "substantial, and... roughly fixed regardless of scope," while "the probability the evidence materially changes the Unit 3-E position" was assessed as "plausible but not favored" — an **evidentiary-value** judgment, independent of how much the outcome matters to any consumer.

**The new downstream dependency changes the stakes side of the proportionality equation; it does not change the evidentiary-value side.** Specifically:

- It does not increase the probability that a Family A decision-step extension would produce a materially stronger, dual-proposition result (§5 of that Review) — nothing about Knowledge Discoverability's failure bears on Family A's own thin, 23.2%-exposure evidence base or its own weak cross-candidate prior.
- It does not resolve the Unit 3-E §14 "no admissible evidence has changed" determination for Control, Family A, or Family B's own matched-subset figures.
- It does not, by itself, make any specific remedy-comparison campaign more likely to succeed — a higher-stakes context does not manufacture evidence.
- It does raise the cost of continued pure inaction from hypothetical ("some future consumer might someday need this") to concrete and dated (a real, evidence-preserved programme is blocked today, `2026-08-13`) — this is a genuine change, and it is exactly the kind of "specific, precisely-stated development" the Disposition Closure Review §16's own catch-all bullet contemplates as capable of justifying a fresh governance act, distinct from and not requiring any change in the underlying remedy evidence itself.

```text
PROPORTIONALITY_CHANGED=PARTIALLY. The Family A Extension Review's own specific finding (a narrow, Proposition-1-only extension has low decision value) is UNCHANGED — this is an evidentiary-design conclusion the new trigger does not touch. What has changed is whether continued, wholly undocumented silence is the most proportionate response to the paused state itself — the new trigger justifies a fresh, documented governance act (Section 11) but does not by itself justify, or increase the expected payoff of, any specific evidence-gathering campaign, remedy reconsideration, or bar revision.
```

## 11. Determination 7 — axis separation

Six axes, kept explicitly distinct per Unit 3-A §6/§7's own independent-axes requirement, none conflated with another anywhere in this document:

1. **Action-selection reliability** (does the boundary pick the right tag): measured, exploratory-tier, all four candidates poor on the matched REMEMBER subset — Control 0/15, Family B 1/15, Family A 8/15, Family C 1/3 (own denominator).
2. **False-positive REMEMBER safety** (does it wrongly fire on non-instructions): measured, every candidate produced ≥1 observed event — Control 1, Family A 1, Family B 1, Family C 4 (own full corpus) — none qualification-tier, none zero.
3. **Content fidelity** (is the remembered text exact): **never measured for any candidate** — `contentFidelity` is `null` in every observation, live and offline, across the programme's entire history.
4. **Model identity/qualification** (is this specific model+digest qualified): **no model has completed this programme's own qualification-tier process** — the current, unmodified `llama3.2:3b` configuration has only been characterized/evaluated at exploratory tier (Unit 2, Unit 2-D, Unit 3-C/3-D/3-E, all below the ≥300-exposure zero-event qualification gates); no alternative model has ever been qualified or informally substituted; DQ4 is a single, exploratory-tier, doubly-confounded observation, not a qualification data point.
5. **Implementation complexity** (cost/intrusiveness of a candidate mechanism): varies sharply — Control zero, Family B lowest of the remedy candidates, Family A doubled-call architecture with the highest exposure gap, Family C low mechanism cost but the single largest architectural-boundary crossing (moving classification outside the model).
6. **Live operational acceptance** (does it work in a genuine, owner-facing production session): **this is the axis Knowledge Discoverability's Attempts 1–2 actually speak to**, and the only axis where new evidence exists since the Disposition Closure Review — distinct from, and not a substitute for, axes 1–5, which remain governed exclusively by the existing Unit 3-C/3-D/3-E exploratory-tier record.

No finding in this document merges any of these six axes into a combined score, and none is used to justify a conclusion properly belonging to a different axis.

## 12. Determination 8 — recommended next governance action

**Recommended: a dedicated, standalone "Reasoning Protocol Live-Model Conformance — Reopening Decision" document** — programme-level, peer to the Post-Selection Disposition Planning Review (not a new "Unit 3-F," not a Scope Lock, not an Implementation Plan) — that performs exactly the state-change act this Reopening Assessment does not perform: deciding, on the record, whether the programme's status moves from PAUSED to ACTIVE, using this document's Section 5 determination as its evidentiary input.

**Why this, and not a larger step:** Every path that would touch evidence, code, or model behavior (Sections 7–9) independently requires its own fresh Unit 3-B/3-C-tier governance chain and is not itself justified by the mere fact that the reopening condition (Section 5) is met — meeting the reopening *condition* is not the same as having selected a reopening *path*, exactly as the Post-Selection Disposition Review's own precedent (a Planning Review deciding structure, not a Scope Lock deciding mechanism) establishes. A Reopening Decision is smaller than any of Paths B/D/E/F(model) because it authorizes no live call, no code change, and no campaign — it only changes the programme's own status label and, if it reopens, names which single already-classified path (Section 7's table) is authorized to proceed to *its own* dedicated Planning Review, mirroring exactly how the Family A Extension Review was itself preceded by, and required, the Post-Selection Disposition Planning Review's own separate act.

**Why not a smaller step:** Remaining silently paused with no documented act at all remains lawful (Path A, Section 7) and is not foreclosed by this Assessment — but it would leave the now-concrete, dated downstream consequence unaddressed by any decision-maker with authority over this specific programme's own status, which the "illustrative, non-exhaustive... a future task proposing reopening... must still state its own specific justification" language of Disposition Closure Review §16 anticipates being done through exactly this kind of dedicated act, not through silence.

**Why other paths are not yet authorized:** Path B (Family A extension) was already found not proportionate on its own evidentiary merits, unaffected by the new trigger (Section 10). Path D (deterministic direct-Remember) either reuses a candidate whose known, material false-positive behavior contributed to Unit 3-E's overall no-selection finding alongside its absence of live-model-interaction evidence and its architectural intrusiveness — none of these three factors was singly decisive, and none eliminated the candidate — or requires fresh Unit 3-C-tier governance for an untested variant (Section 8); neither is a lawful immediate next step. Path F (alternative model) requires fresh Family F reconsideration governance not yet performed (Section 9). A selection-bar revision carries the sharpest outcome-driven-governance risk of any path, and the presence of new downstream pressure is precisely the condition under which such a revision would be most suspect, not least (Post-Selection Disposition Review §9).

```text
RECOMMENDED_NEXT_ACTION=Author a standalone Reopening Decision document (programme-level, not this document, not a Scope Lock) that: (a) decides PAUSED-vs-ACTIVE using this Assessment's Section 5 finding; (b) if ACTIVE, names exactly one Section-7-classified path as authorized to proceed to its own separate Planning Review only (not a Scope Lock); (c) if the decision-maker judges the fixed governance overhead still unjustified relative to the single, narrow, non-remedy-comparative contribution Attempts 1-2 provide, remaining paused is an equally lawful outcome this Assessment does not foreclose.
```

## 13. Determination 9 — entry and stop conditions for the Reopening Decision

**Entry conditions** (all required before that future document may be authored):

1. This Reopening Assessment and its own Independent Constitutional Review (not yet performed) are both read fresh, in full, by the Reopening Decision's own author.
2. Repository baseline reconfirmed clean and current (`HEAD == origin/main`) at that task's own start.
3. Knowledge Discoverability's four durable evidence hashes (Section 2) reconfirmed unchanged.
4. No admissible remedy-comparison evidence is claimed to have changed beyond what Section 6 of this document already states (operational-impact evidence only).

**Stop conditions** (the Reopening Decision must not proceed past a bare status decision if any apply):

1. It must not select a remedy or imply a preference among Control/Family A/Family B/Family C within the same act.
2. It must not draft a Scope Lock or Implementation Plan — authorizing a path's own future Planning Review is the maximum scope.
3. It must not pool Attempts 1–2 into any existing campaign statistic or matched-subset figure.
4. It must not authorize any live model call, any new campaign, or any code/test/build change.
5. If, upon independent reflection, no single Section-7 path is judged to have a plausible, non-speculative chance of resolving anything beyond what Sections 8–10 already establish, the correct output is "REMAIN PAUSED, TRIGGER NOTED" rather than a forced reopening.

```text
ENTRY_CONDITIONS=Fresh read of this Assessment + its own Independent Review; baseline reconfirmed clean/current; evidence hashes reconfirmed; no evidence-change claim beyond Section 6's operational-impact-only admissibility.
STOP_CONDITIONS=No remedy selection; no Scope Lock/Implementation Plan drafted; no pooling of Attempts 1-2 into campaign statistics; no live call/campaign/code change authorized; "remain paused" remains an available, non-forced outcome of that future document.
```

## 14. Determination 10 — preserved non-claims

```text
REMEDY_SELECTED=NO
IMPLEMENTATION_AUTHORIZED=NO
MODEL_RUN_AUTHORIZED=NO
THIRD_KNOWLEDGE_DISCOVERABILITY_ATTEMPT_AUTHORIZED=NO
SELECTION_THRESHOLD_LOWERED=NO
EXISTING_CAMPAIGN_EVIDENCE_MODIFIED_OR_POOLED=NO
NEW_GAP_NUMBER_OR_PROGRAMME_IDENTITY_ASSIGNED=NO
KNOWLEDGE_DISCOVERABILITY_CLOSURE=REMAINS BLOCKED
PROGRAMME_STATUS_CHANGED_BY_THIS_DOCUMENT=NO — remains PAUSED pending the separate Reopening Decision recommended in Section 12
```

## 15. Verification performed

```text
$ git diff --check
(no output — clean)

$ git status --short --branch
## governance/reasoning-protocol-reopening-assessment
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_ASSESSMENT_FOR_KNOWLEDGE_DISCOVERABILITY.md
(exactly one new, untracked file; nothing staged, nothing else modified)

$ git diff --stat -- src/ tests/ build.gradle.kts settings.gradle.kts
(no output — no production, test, or build file touched)
```

Every commit hash, section number, and figure cited in Sections 4–11 above was independently re-derived from the primary document's own text during this task's own reading pass (Sections 3, 5–10), not copied from any prior summary. The Family C 24/29/4-FP/1-FN figures were independently cross-checked against three separate primary re-derivations (Unit 3-D Comparative Evaluation Review §11, Unit 3-E Remedy Selection Review §5/§8, and this task's own direct read of the Unit 3-D Review), all identical. The Disposition Closure Review §16/§17 text was re-read directly via `sed` extraction for this document rather than trusted from its own Independent Review's paraphrase.

```text
CITATION_AUDIT=PASS — every cited section/figure independently re-derived from primary source during this task; no citation trusted solely from a prior summary or from a document's own Independent Review's restatement.
DIFF_CHECK=PASS (no output)
FILES_CHANGED=1 (docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_ASSESSMENT_FOR_KNOWLEDGE_DISCOVERABILITY.md, new, untracked)
GIT_STATUS=clean except the one new untracked file; not staged; not committed; not pushed
```

## 16. Disposition

```text
PLANNING REVIEW ONLY — REOPENING CONDITION SATISFIED — PROGRAMME STATUS UNCHANGED (REMAINS PAUSED) — NO REMEDY SELECTED — NO IMPLEMENTATION AUTHORIZED — RECOMMENDATION: A SEPARATE REOPENING DECISION DOCUMENT SHOULD BE AUTHORED NEXT, PER SECTION 12
```

Pending this document's own Independent Constitutional Review, not yet performed and not authorized to be assumed.
