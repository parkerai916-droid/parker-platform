**Status:** Unit 3-E — Remedy Selection — Review — **DECISION REACHED: NO REMEDY SELECTED.** Governance-only, against committed baseline `cf22a9a7f428668e1524bdd2b68c26e3474e5fa1`, performed under the frozen Unit 3-E Remedy Selection Scope Lock (`cf22a9a`, ACCEPTED WITH QUALIFICATIONS by its own Independent Constitutional Review). This is the actual Unit 3-E governance decision. No candidate is implemented. No production change is authorized. No Unit 4 work begins. No qualification occurs.

# Reasoning Protocol Live-Model Conformance Unit 3-E — Remedy Selection — Review

## 1. Status

This document performs the Unit 3-E remedy-selection decision itself, applying the frozen Scope Lock's own methodology to the frozen Unit 3-D evidence, independently reconstructed fresh for this task. The decision reached is **NO REMEDY SELECTED**, not because any candidate performed badly enough to be eliminated, but because the admissible evidence — independently re-confirmed unchanged from Unit 3-D's own "insufficient to select" finding — does not clear the frozen selection bar for any candidate, and no fresh evidence or separately-governed bar revision exists to justify a different conclusion.

## 2. Baseline

`git rev-parse HEAD` = `git rev-parse origin/main` = `cf22a9a7f428668e1524bdd2b68c26e3474e5fa1`, independently re-confirmed after `git fetch origin`. `git status -sb` clean. No Unit 3-C live execution environment variables active. No campaign running (the sole background Java process observed is an idle Gradle daemon, not an active campaign task). Last commit: `cf22a9a governance: freeze Unit 3-E remedy selection scope`.

## 3. Authority

Performed under the frozen Unit 3-E Remedy Selection Scope Lock (`cf22a9a`), re-read fresh, verbatim, in full for this task, unchanged since its own commit (independently re-confirmed via `git log`). **Independently re-confirmed, from the Scope Lock's own text and its own cited primary sources:**

- Unit 3-E may make a governance selection among already-tested candidates, or explicitly decline to select any (Scope Lock Section 2, quoting the programme-level Unit 3 Planning Review's own governing definition).
- Unit 3-E does not implement the selected mechanism — "a document, not code" (Section 2).
- Unit 3-E does not authorize production use and does not constitute formal qualification — qualification is a separate, later Unit 5 requiring qualification-tier evidence no candidate possesses (Section 2).
- A selection, if made, does not automatically begin Unit 4 — it directs a future, separately-authorized Unit 4 task, which retains its own full Planning/Boundary/Completion/Independent-Review gate sequence (Section 13).
- No evidence limitation identified by prior governance may be silently erased — every limitation carried by Unit 3-D's own evidence remains fully binding on this document (Section 15, item 12).
- Remedy selection (a governance decision about direction) and remedy implementation (actual code) are constitutionally distinct decisions, owned by different units (Unit 3-E and Unit 4 respectively), never collapsed into one act.

**The repository fully supports every one of these propositions; no conflict was found. This task proceeds.**

## 4. Controlling governance

Read fresh, in full, for this task, not from summary: the original top-level Reasoning Protocol Live-Model Conformance and Structured-Output Reliability Planning Review (`b34f8d0`); the programme-level Unit 3 Reliability Contract and Remedy Selection Planning Review (`7e9e388`); the Unit 3-A Reliability Contract Definition Scope Lock (`ab27f18`); the Unit 3-B Remedy Experiment Scoping Scope Lock (`55af571`); the Unit 3-C Scope Lock and its Timeout + Durability Amendments; the Attempt 6 Execution Evidence Review and its Independent Constitutional Review (`b5c0749`); the Unit 3-C Evidence Completeness and Durability Determination (`4430e8b`); the Unit 3-D Comparative Evaluation Planning Review (`239c6c1`); the Unit 3-D Family C Offline Completion Determination and its Independent Constitutional Review (`ccacf47`); the Unit 3-D Comparative Evaluation Scope Lock and its Independent Constitutional Review (`32b55d4`); the Unit 3-D Comparative Evaluation Review and its Independent Constitutional Review (`2e468ed`); the Unit 3-E Remedy Selection Planning Review and its Independent Review (`e4d691a`); and the Unit 3-E Remedy Selection Scope Lock and its Independent Constitutional Review (`cf22a9a`), read verbatim in Section 3 above. Where any summary encountered during this task's own drafting conflicted with the primary text of any of these documents, the primary text controls — no conflict was in fact found.

## 5. Evidence sources and provenance (independently reconstructed, not copied)

Independently re-derived fresh from the campaign's own primary artifacts (`/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810-03/`, confirmed unchanged, 26 files) using a shell/`grep`-based extraction method distinct from every prior derivation in this programme's history:

| Source | Provenance | Fresh re-verification result |
|---|---|---|
| **A — Attempt 6 live evidence** | Live, campaign-attributed | Warm-up 3/3/0 (intent/raw/timeout); Control 132/132/0; Family A 51/50/1; Family B 91/91/0; Family C 0(by design)/6/0 — identical to every prior capture |
| **B — Family C offline deterministic completion** | Offline, zero model calls, not campaign-attributed | `Family C mechanism reproduces every row of the frozen predicted trace...` test independently re-run fresh this task: BUILD SUCCESSFUL, 24/29 correct, 4 FP, 1 FN — unchanged |
| **C — frozen expected classifications** | Governance, unchanged since `fee2edd`/`08f3692` | Ground truth only, never itself a result |
| **D — operational evidence** | Derived from A | Completion/timeout/representation/parser/transport status, arm-wide |
| **E — semantic evidence** | Derived from A/B | `actualAction`/`expectedAction` match, restricted to the matched five-fixture subset for cross-arm use |

No figure below blends these sources without explicit labeling.

## 6. Exposure/censoring disclosure

Independently re-derived fresh (`wc -l` on every arm's own `intent.jsonl`/`raw.jsonl`/`timeouts.jsonl`):

| Arm | Scheduled max | Attempted | Completed | Timeouts | Exposure fraction |
|---|---|---|---|---|---|
| Warm-up | 3 | 3 | 3 | 0 | 100.0% |
| Control | 145 | 132 | 132 | 0 | **91.0%** |
| Family A | 220 | 51 | 50 | 1 | **23.2%** |
| Family B | 115 | 91 | 91 | 0 | **79.1%** |
| Family C (live, Source A) | 29 | 6 | 6 | n/a | **20.7%** |
| Family C (offline, Source B) | — | 23 | 23 | n/a | remaining 79.3%, offline-sourced |

**Exactly matches the figures this task's own governing prompt stated as expected**, independently re-derived rather than assumed. **The arms are unequally and informatively censored** — three of four arms halted before exhausting their own live schedule, each at a different point, because the safety checkpoint fires on the exact adverse event (a false-positive REMEMBER/GOAL) any semantic comparison would also want to measure. No inference is drawn anywhere in this document about what any arm's own unobserved remainder would have shown, and no candidate's later or earlier checkpoint timing is treated as evidence of relative safety.

## 7. Matched-subset disclosure

**Independently re-confirmed, fresh, via `grep`-based fixture-level counting (a method distinct from every prior derivation in this programme):** `r01-direct`, `r02-please`, `r03-dont-forget`, `p01-ordinary-fact`, `p02-quoted-remember` remain the only fixtures where Control, Family A's own decision step, and Family B all completed the full frozen `n=5`, with Family C's own frozen `n=1` also available for the same five (live, Source A). **Not enlarged.** This is the exclusive basis for any cross-arm semantic-correctness or false-negative comparison in this document, per Scope Lock Section 5 (inherited from the Unit 3-D Scope Lock).

## 8. Candidate-by-candidate analysis

Each of the twenty dimensions the governing task requires is addressed for every candidate, using only Scope-Lock-admissible evidence (Section 5 of the Scope Lock).

### CONTROL

1. **Admissible evidence:** Matched-subset semantic/false-negative results (Source A, `n=5`/fixture); arm-wide operational metrics (Source D, 132 trials, 91.0% exposure); one false-positive REMEMBER event (Source A).
2. **Semantic strengths:** 10/10 correct on the two REPLY-expected matched fixtures (`p01`, `p02`).
3. **Semantic weaknesses:** 0/15 correct on the three REMEMBER-expected matched fixtures (`r01`, `r02`, `r03`) — independently re-confirmed fresh (Section 6 above).
4. **False-negative behavior:** 15/15 on matched REMEMBER-expected trials — every single one.
5. **False-positive safety behavior:** one false-positive REMEMBER (`p17-hypothetical-remember`, a supplemental fixture with no counterpart in Family A's or Family B's own corpus), zero false-positive GOAL.
6. **Representation validity:** 132/132 (100%).
7. **Parser behaviour:** zero failures.
8. **Transport behaviour:** zero failures.
9. **Completion/timeout behaviour:** 132/132 completed, zero timeouts.
10. **Evidence coverage:** largest of any arm (91.0%, 27 of 29 distinct fixtures touched).
11. **Censoring/exposure limitations:** halted at a supplemental fixture no other model-invoking arm's own corpus contains at all.
12. **Implementation complexity:** none — Control is the unmodified, already-deployed production path; "selecting" it requires zero Unit 4 implementation effort.
13. **Architectural intrusiveness:** none.
14. **Model dependence:** full — Control's own behavior is entirely a function of the current model's own compliance with the current, unmodified prompt.
15. **Deterministic characteristics:** none — fully model-dependent, stochastic in principle.
16. **Reversibility:** not applicable — nothing would change.
17. **Known failure modes:** systematic non-compliance with the REMEMBER instruction pattern on the matched subset (0/15), independently re-confirmed the most severe semantic weakness of any candidate on this specific, narrow evidence.
18. **Constitutional risk:** one false-positive REMEMBER, plus the single worst matched-subset REMEMBER-recognition rate of any candidate — the exact failure pattern (PF01, DQ1, DQ3, DQ4) that originally motivated this entire Unit 3 programme.
19. **Unresolved questions:** whether Control's own 0/15 result on this narrow, five-fixture sample generalizes to its own full, un-truncated exposure — not resolved by this evidence.
20. **Prohibited inferences:** that Control's own later checkpoint timing (91.0% exposure) indicates greater safety than any other candidate (Section 6 of the Scope Lock forbids this); that Control's own status as the current production baseline exempts it from the same evidentiary scrutiny applied to Families A/B/C.

### FAMILY A

1. **Admissible evidence:** Matched-subset decision-step semantic/false-negative results (Source A, `n=5`/fixture); arm-wide decision-step and render-step figures, kept separate (Source A); one genuine model timeout (Source D).
2. **Semantic strengths (decision step):** the smallest matched-subset false-negative rate among the three model-invoking arms (8/15, versus Control's 15/15 and Family B's 14/15) — a raw, descriptive fact, independently re-confirmed fresh, not itself proof of superiority (Section 12 below explains why).
3. **Semantic weaknesses (decision step):** still 14/25 (56.0%) matched-subset correctness — well short of any plausible reliability bar; still missed 8 of 15 REMEMBER-expected matched trials.
4. **False-negative behaviour:** 8/15 (decision step, matched subset) — independently re-confirmed fresh; the earlier Attempt 6 Execution Evidence Review's own "8/13" denominator error is not repeated here.
5. **False-positive safety behaviour:** one false-positive REMEMBER (`p03-ambiguous-memory`, decision step — the same fixture Family C's own mechanism also flags), zero false-positive GOAL.
6. **Representation validity:** 50/50 (100%), both sub-steps.
7. **Parser behaviour:** zero failures.
8. **Transport behaviour:** zero failures (the one timeout is a model-side latency event, not a transport/provider failure, per the already-governed Scored-Trial Timeout Semantics Determination).
9. **Completion/timeout behaviour:** 50/51 completed (98.0%), one genuine `MODEL_TIMEOUT` (render step, `r02-please/render/02`) — kept fully separate from every semantic figure above, per Scope Lock Section 7.
10. **Evidence coverage:** smallest of the three model-invoking arms (23.2%, 5 of 23–29 fixtures touched) — no GOAL, no NOACTION, and only two of thirteen REPLY-category fixtures reached at all.
11. **Censoring/exposure limitations:** the narrowest evidence base of any candidate by a wide margin.
12. **Implementation complexity:** doubled call cost per fixture (a decision call plus a render call); requires a genuinely new two-call orchestration pattern, which the programme-level Planning Review's own architectural-boundary analysis (Unit 3 Planning Review Section 13) already flags as conflicting with `ModelReasoningProvider`'s documented single-call, no-retry invariant absent fresh governance.
13. **Architectural intrusiveness:** among the higher of the four candidates — a new intermediate decision type may be required depending on implementation choices (already flagged in the programme-level architectural-boundary analysis).
14. **Model dependence:** full, at both sub-steps.
15. **Deterministic characteristics:** none.
16. **Reversibility:** not evidentially established — no rollback scenario was ever tested by any Unit 3-C experiment.
17. **Known failure modes:** one false-positive REMEMBER on the same fixture Family C also fails on; substantial residual decision-step false-negative rate even at its own best matched-subset figure.
18. **Constitutional risk:** one false-positive REMEMBER; the smallest evidence base of any candidate, meaning constitutional risk over the 76.8% of its own schedule it never reached is genuinely unknown, not merely unfavorable.
19. **Unresolved questions:** whether the smaller matched-subset false-negative rate (8/15) reflects the decision/rendering-separation mechanism itself, or is an artifact of Family A's own smaller sample and earlier truncation — explicitly not resolved by Unit 3-D and not resolved here.
20. **Prohibited inferences:** that the strong render-step figure (24/24) says anything about decision-step reliability (Scope Lock Section 7, absolute); that the numerically lowest matched-subset false-negative rate among the three model arms establishes Family A as the safest or most reliable candidate — a relative-superiority inference the governing task itself explicitly forbids treating as sufficient for selection.

### FAMILY B

1. **Admissible evidence:** Matched-subset semantic/false-negative results (Source A, `n=5`/fixture); arm-wide operational metrics (Source D, 91 trials, 79.1% exposure); one false-positive REMEMBER event.
2. **Semantic strengths:** 9/10 correct on the two REPLY-expected matched fixtures.
3. **Semantic weaknesses:** 1/15 correct on the three REMEMBER-expected matched fixtures — independently re-confirmed fresh.
4. **False-negative behaviour:** 14/15 on matched REMEMBER-expected trials.
5. **False-positive safety behaviour:** one false-positive REMEMBER (`g03-later-action`, a GOAL-category negative control), zero false-positive GOAL.
6. **Representation validity:** 91/91 (100%).
7. **Parser behaviour:** zero failures.
8. **Transport behaviour:** zero failures.
9. **Completion/timeout behaviour:** 91/91 completed, zero timeouts.
10. **Evidence coverage:** second-largest among the truncated arms (79.1%, 18 of 23 base fixtures touched).
11. **Censoring/exposure limitations:** halted before reaching two of five GOAL fixtures and any supplemental fixture (Family B's own corpus never includes supplemental fixtures at all, by design).
12. **Implementation complexity:** lowest of the three model-invoking remedy candidates — single-call architecture, identical cost profile to Control, prompt-text-only modification.
13. **Architectural intrusiveness:** lowest of the three model-invoking remedy candidates — no new domain type, no new call pattern, no parser change.
14. **Model dependence:** full.
15. **Deterministic characteristics:** none.
16. **Reversibility:** not evidentially established, though architecturally the simplest of the three remedy candidates to revert (a prompt-text change only).
17. **Known failure modes:** one false-positive REMEMBER; matched-subset semantic performance numerically similar in magnitude to Control's own (10/25 each) — **not established as equivalent**, per Scope Lock Section 8, given the two arms' own different exposure and censoring.
18. **Constitutional risk:** one false-positive REMEMBER; a matched-subset REMEMBER-recognition rate (1/15) only marginally better than Control's own (0/15).
19. **Unresolved questions:** whether Family B's own prompt/protocol redesign produces any effect genuinely distinguishable from Control's own behavior — not resolved by this descriptive-only evidence.
20. **Prohibited inferences:** that Family B's own similarity to Control's own matched-subset figures means the redesign "made no difference" (a claim this evidence cannot support either way); that Family B's own lower implementation complexity is itself a reason to prefer it absent sufficient semantic evidence.

### FAMILY C

1. **Admissible evidence:** Full 29-fixture deterministic result (Sources A+B combined, provenance-labeled); matched-subset results at Family C's own `n=1` denominator (Source A only); all nine adversarial categories (Sources A+B).
2. **Semantic strengths:** the only candidate with complete, all-nine-category adversarial coverage; exact, reproducible behavior on every fixture it has ever been run against, live or offline.
3. **Semantic weaknesses:** 4 of 29 fixtures produce a false-positive REMEMBER; 1 of 29 produces a false negative — independently re-confirmed fresh via the existing committed test, re-run this task.
4. **False-negative behaviour:** 1/29 overall (`r03-dont-forget`); at the matched subset (its own `n=1` denominator, never pooled with the model arms' `n=5`): 1/3 REMEMBER-expected matched trials missed.
5. **False-positive safety behaviour:** 4 false-positive REMEMBER (`p03-ambiguous-memory`, `p04-embedded-tags`, `p05-mixed-memory-discussion`, `p12-injection`), 0 false-positive GOAL (structurally unreachable by its own binary output space, not a demonstrated safety property).
6. **Representation validity:** 100% by construction — this mechanism has no representation-failure mode at all, a structural fact, not an earned result against a real parser.
7. **Parser behaviour:** not applicable — no parser is involved.
8. **Transport behaviour:** not applicable — no transport layer exists; its own 100% "reliability" here reflects absence of the risk being measured, never superior resilience to it.
9. **Completion/timeout behaviour:** 100% completion by construction; no timeout concept applies.
10. **Evidence coverage:** complete for its own intended 29-fixture, nine-category surface (live 20.7% + offline the remainder) — but zero live-model-interaction data exists for this mechanism, by architecture, at any exposure fraction.
11. **Censoring/exposure limitations:** its own live subset is as truncated as any other arm (20.7%); its own offline completion, while eliminating fixture-coverage gaps, does not and cannot supply any live-model behavioral data.
12. **Implementation complexity:** low at the mechanism level (already a fully specified, five-step pure function) but a genuinely large architectural boundary crossing — moving any classification outside the model contradicts this programme's own consistently-stated "model as sole semantic authority" design, independently re-confirmed as the single largest boundary crossing named anywhere in the programme-level architectural-boundary analysis.
13. **Architectural intrusiveness:** the highest of any candidate, by the programme's own prior, already-governed assessment (Unit 3 Planning Review Section 13).
14. **Model dependence:** none — its own correctness is entirely independent of model identity, digest, or behavior.
15. **Deterministic characteristics:** fully deterministic and exactly reproducible — a genuine, evidence-grounded property.
16. **Reversibility:** not evidentially established by any Unit 3-C experiment; architecturally, a keyword-rule mechanism is trivially removable, but this is an architectural observation, not a tested finding.
17. **Known failure modes:** four specific, fully characterized false positives (each attributable to named classifier logic: literal-substring triggering with insufficient mitigation) and one specific, fully characterized false negative (negation-mitigation misfiring on a double-negative-like construction).
18. **Constitutional risk:** 4 known false positives (the most of any candidate in absolute count, though at its own full 29-fixture denominator, not the truncated denominators the other three arms carry) — each fully explained, none mysterious, but real and not yet mitigated.
19. **Unresolved questions:** whether this mechanism's own known failure modes could be mitigated without introducing new ones — not tested by any Unit 3-C experiment; whether a rule-based mechanism operating with zero awareness of the vast majority of realistic owner input (any message containing neither "remember" nor "forget") is architecturally suited to be a general reasoning-boundary remedy at all, as opposed to a narrow supplementary check — not resolved here.
20. **Prohibited inferences:** that zero model calls means superior model reliability (Scope Lock Section 9, absolute); that full determinism means automatic safety (same section); that complete adversarial-category coverage means qualification-readiness (same section, explicit).

## 9. Minimum-gate matrix

Applied exactly as the Scope Lock's own Section 11 freezes — three honest, non-invented gates only:

| Candidate | Representation validity gate | Parser reliability gate | Zero false-positive GOAL gate |
|---|---|---|---|
| Control | **PASS** (132/132) | **PASS** (0 failures) | **PASS** (0 observed) |
| Family A | **PASS** (50/50) | **PASS** (0 failures) | **PASS** (0 observed) |
| Family B | **PASS** (91/91) | **PASS** (0 failures) | **PASS** (0 observed) |
| Family C | **PASS** (100%, by construction) | **NOT APPLICABLE** (no parser exists for this mechanism) | **PASS** (structurally unreachable) |

**No "zero false-positive REMEMBER" gate is applied, per Scope Lock Section 11's own explicit prohibition** — imposing one would fail every candidate, including Control. **No `NOT ESTABLISHED` result is silently reinterpreted as `PASS` anywhere in this table** — Family C's own parser gate is honestly marked `NOT APPLICABLE`, not `PASS`, since the underlying property (a parser existing and behaving reliably) is architecturally absent, not merely unobserved.

**All four candidates clear every gate actually authorized.** This is not a discriminating result — it confirms baseline eligibility for narrative consideration (Section 10 of the Scope Lock), nothing more. The gates were never designed, and are not now used, to select among four candidates that all trivially pass them.

## 10. False-positive safety analysis

Every candidate produced at least one false-positive REMEMBER event under its own observed evidence: Control (1, `p17-hypothetical-remember`), Family A (1, `p03-ambiguous-memory`), Family B (1, `g03-later-action`), Family C (4, across its own full 29-fixture corpus — `p03`, `p04`, `p05`, `p12`). Zero false-positive GOAL events occurred anywhere, for any candidate.

**Restated directly here, not merely by cross-reference to Unit 3-C governance, per this task's own explicit instruction:** a false-positive REMEMBER event is a material constitutional concern under Unit 3-A Section 7's own zero-tolerance framing at qualification tier. This document does **not** invent a numeric disqualification threshold — no governance anywhere in this programme states that one, two, three, or four observed false positives at exploratory-tier sample sizes automatically bars a candidate. This document does **not** mechanically disqualify every candidate on the strength of this fact alone, because doing so would eliminate Control — the already-deployed baseline — by the same stroke, which no cited governance intends. This document does **not** minimize the risk merely because every candidate exhibited at least one occurrence: each occurrence remains a real, durably-recorded, named event on a genuine ADVERSARIAL- or GOAL-category negative-control fixture, and every one is restated by name in Section 8 above, not summarized into an aggregate count that would obscure which fixture and which candidate.

**Family C's own four-versus-one-each comparison to the model-invoking arms is explicitly not read as evidence of relatively worse safety** — Family C's own denominator (29, its full corpus) is not comparable to the model arms' own truncated, differently-composed denominators (132, 51, 91), and the model arms' own true rates over their own full, un-truncated schedules remain genuinely unknown. No ranking of the four candidates by false-positive count or rate is performed anywhere in this document.

## 11. False-negative analysis

Treated as substantive failures throughout, never disguised by strong representation validity or clean parser behavior (Section 13 below addresses this directly). Matched-subset REMEMBER-expected false-negative counts, independently re-confirmed fresh: Control 15/15, Family A (decision step) 8/15, Family B 14/15, Family C 1/3 (its own `n=1` denominator). **These figures are stated as raw, descriptive facts.** No ranking or superiority claim is drawn from them in this document's own final decision (Section 15) — the governing task explicitly instructs against selecting "the least bad candidate merely because the task is called Remedy Selection," and Family A's own numerically lowest matched-subset rate is explicitly not treated as sufficient grounds for selection on its own (Section 8, Family A, item 20; Section 15 below).

## 12. Operational evidence

Independently re-confirmed, fresh: 100% completion for Control, Family B, and Family C (live); 98.0% for Family A (one genuine model timeout, `MODEL_TIMEOUT` classification, `ARM_CONTINUED`, correctly excluded from every semantic figure throughout this document). 100% representation validity, zero parser failures, zero transport failures — every candidate. **These uniformly strong operational results are explicitly not permitted to compensate for, or be blended with, the substantial semantic-selection weaknesses documented in Sections 8 and 11** — a syntactically valid, wrong-action response (e.g., Control's own 0/15 on every matched REMEMBER-expected trial, every one with `representationValid: true`) remains non-conformant regardless of its own clean representation, exactly as Unit 3-A Section 6 requires.

## 13. Unavailable evidence

`contentFidelity`: **NOT AVAILABLE**, independently re-confirmed via fresh sampling of `raw.jsonl` records across all five arms this task — `"contentFidelity":null` in every completed observation, live and offline alike. Not calculated, proxied, estimated, or inferred anywhere in this document, for any candidate. Absence of this evidence is not treated as evidence of acceptable fidelity for any candidate — it is treated as a genuine, unresolved gap in what any selection (had one been made) could honestly claim to know.

Also unavailable: any qualification-tier sample for any candidate (all four remain exploratory-tier, per Section 14); any evidence for any untested hybrid or hypothetical combination of candidates (none was gathered, none is permitted to be invented here).

## 14. Evidence-change-since-Unit-3-D determination

**Treated as a first-class checklist item, not buried in narrative, per this task's own explicit instruction.**

**Question:** what evidence has changed since Unit 3-D concluded that the evidence was sufficient to narrow candidates but not sufficient to select?

**Answer, independently re-derived by tracing every commit between Unit 3-D's own completion (`2e468ed`) and this task's own baseline (`cf22a9a`):**

```text
b5c0749  (already prior to Unit 3-D's own completion — no new commit)
2e468ed  governance: complete Unit 3-D comparative evaluation   <- Unit 3-D's own "insufficient" finding
32b55d4  governance: freeze Unit 3-D comparative evaluation scope  <- precedes 2e468ed; already accounted for
ccacf47  governance: complete Unit 3-D Family C offline evidence  <- precedes 2e468ed; already accounted for
e4d691a  governance: plan Unit 3-E remedy selection   <- pure planning, explicitly forbidden from gathering evidence
cf22a9a  governance: freeze Unit 3-E remedy selection scope   <- pure governance freeze, no evidence
```

**No admissible evidence source (Section 5 above) has changed at all since Unit 3-D's own completion.** No live campaign ran. No `/api/generate` call occurred. No offline computation added new fixture data — Family C's own offline completion (`ccacf47`) was already fully incorporated into Unit 3-D's own Comparative Evaluation Review (`2e468ed`), which post-dates it. The exposure fractions, matched-subset figures, false-positive events, and false-negative counts independently re-derived fresh in this document (Sections 6–11) are **identical**, to the fixture, to what Unit 3-D's own evidence already contained when it reached its own "sufficient to narrow, not sufficient to select" finding.

**Constitutional consequence, per Scope Lock Section 4's own explicit rule:** since no admissible evidence has changed, this document may not reach an affirmative selection without either (a) evidence this task does not have, or (b) redefining the sufficiency standard itself — which Section 4 forbids without "a fresh, separately-governed, explicit act changing the standard itself," which does not exist. **This determination is dispositive of Section 15's own final decision, not merely informative.**

## 15. Selection decision

```text
NO REMEDY SELECTED.
```

## 16. Rationale

Stated directly, following from the evidence and the frozen gates, not from a preference for caution as an end in itself:

1. **Section 14's own determination is dispositive.** No admissible evidence has changed since Unit 3-D's own "insufficient to select" finding. Absent new evidence or a separately-governed bar revision, this document is constitutionally required to reach the same sufficiency conclusion Unit 3-D and the Unit 3-E Planning Review already reached — and that conclusion was "not sufficient to select one," restated here as unchanged.
2. **The minimum gates (Section 9) provide zero discriminating power.** All four candidates pass every gate actually authorized; the gates confirm baseline eligibility, not selection-worthiness.
3. **No candidate clears an honest reliability threshold on the one rigorously comparable semantic metric available** — the matched five-fixture subset. Every model-invoking candidate's own REMEMBER-recognition rate on this subset is poor (Control 0/15, Family B 1/15, Family A 8/15) — even Family A's own numerically best figure represents missing more than half of the direct, unambiguous instructions to remember a stated fact, the single most basic case this entire programme exists to get right. Family C's own full-corpus result (24/29, 4 false positives) is real and complete, but carries its own, different, disqualifying-for-selection-purposes limitation (zero live-model-interaction data of any kind, and the single largest architectural-intrusiveness profile of any candidate).
4. **This document does not select the "least bad" candidate merely because the task is named Remedy Selection**, per this task's own explicit instruction. Family A's own numerically lowest matched-subset false-negative rate (8/15) is real and stated (Section 8, Section 11) but is not, by itself, sufficient grounds for selection — it remains a minority-correct result on the narrowest evidence base of any candidate (23.2% exposure), and Section 8's own item 20 for Family A explicitly forecloses treating relative superiority as adequate absolute performance.
5. **No candidate's own evidence approaches qualification tier** (Unit 3-A Section 4's own ≥300-exposure zero-event gates), and this document does not convert exploratory evidence into qualification evidence by treating "narrowed" as equivalent to "selected."
6. **No confidence is manufactured.** This document does not claim the evidence "probably" favors one candidate, does not speculate about what a fuller campaign would likely show, and does not treat the absence of elimination (Section 8 shows no candidate is eliminated) as equivalent to the presence of sufficient grounds for selection — those are different findings, and only the latter would justify an affirmative decision.

**No candidate is eliminated by this decision.** Every candidate remains exactly as viable, or as unproven, as it was before this task began. This is a statement that the evidentiary basis itself is not yet adequate to responsibly commit Unit 4 implementation effort to any one of them — not a statement that any one of them has failed.

## 17. Unresolved questions

Carried forward, unchanged, from the Unit 3-D Comparative Evaluation Review's own Section 19 and the Unit 3-E Remedy Selection Planning Review's own Section 17: whether decision/rendering separation (Family A) is itself the cause of its own lower matched-subset false-negative rate, or an artifact of its own smaller, earlier-truncated sample; whether Family B's own prompt/protocol redesign produces any effect distinguishable from Control's own behavior; whether Family C's own known failure modes could be mitigated without introducing new ones; the `elapsedNanos` anomaly on Family A's one timeout; whether any currently-deferred remedy family should be reconsidered now that individual-family baseline evidence exists; and, new to this document: whether a future, separately-governed Unit 3-E policy decision to adopt a deliberately modest, explicitly-justified selection bar (rather than awaiting further live evidence) would change this outcome — a genuine, open policy question this document surfaces but, consistent with Scope Lock Section 4, does not resolve unilaterally.

## 18. Implications of the decision

No Unit 4 implementation work is directed, authorized, or implied for any candidate. No candidate is eliminated from future consideration. The evidentiary record (Sections 6–13) remains available, unchanged, for a future Unit 3-E task operating under either fresh evidence or an explicitly, separately-governed bar revision. The current, unmodified production path (Control) continues to operate exactly as it did before this task, since no change is authorized or implied by a no-selection outcome.

## 19. Unit 4 firewall

Restated directly, per this task's own instruction not to rely merely on cross-reference: this document does not design, draft, or imply any production implementation; does not modify `DefaultReasoningPromptBuilder`, `TaggedReasoningResponseParser`, or any other production file; does not implement deterministic classification or any other mechanism in `src/`; does not define a rollout or migration plan; does not alter the production runtime in any way; does not authorize deployment of any kind. Because no candidate is selected, no Unit 4 direction of any kind is issued by this document — the question of what Unit 4 would need to do for any given candidate remains entirely unaddressed, correctly, since no candidate was selected for it to address.

## 20. Qualification firewall

Restated directly: this document does not perform, authorize, or approximate formal qualification (Unit 5) for any candidate. No candidate's evidence is treated as meeting, approaching, or being exempted from Unit 3-A Section 4's own qualification-tier thresholds (≥300-exposure zero-event gates, ≥99% representation validity, ≥97% one-sided-95%-LCB correctness). This document's own no-selection outcome does not itself require or trigger any qualification activity, since none is being directed toward Unit 4 in the first place.

## 21. Prohibited interpretations

This document may not be read as: a statement that any candidate has failed or been eliminated (Section 8, Section 16); a statement that Control, as the current production path, is safe or acceptable merely by virtue of not having been replaced (Control's own matched-subset performance is, on this narrow evidence, the weakest of the four candidates on the REMEMBER-recognition axis specifically); a permanent or final judgment foreclosing future selection (Section 4 of the Scope Lock, restated in Section 14 above, explicitly anticipates and permits a different future outcome on fresh evidence or an explicit bar revision); an implicit endorsement of any hybrid or untested combination (none is considered anywhere in this document); a qualification-tier or production-readiness claim of any kind, for any candidate; authorization of any live model call, any new Unit 3-C campaign, or any modification to Unit 3-C/3-D artifacts (none occurred); or authorization of any Unit 4 or Unit 5 work of any kind.

## 22. Exit disposition

```text
UNIT 3-E DECISION COMPLETE — NO REMEDY SELECTED — GOVERNANCE ONLY — NO IMPLEMENTATION AUTHORIZED
```

Pending this document's own Independent Constitutional Review.
