**Status:** Unit 3-E — Remedy Selection — Scope Lock — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** Freezes the exact selection question, the admissible evidence, the false-positive safety rule, per-family treatment rules, the hybrid prohibition, the decision method, the no-selection path, the Unit 4 firewall, and the required output structure any future Unit 3-E remedy-selection task must satisfy. Selects no remedy, ranks no candidate, and authorizes no implementation.

# Reasoning Protocol Live-Model Conformance Unit 3-E — Remedy Selection — Scope Lock

## 1. Baseline

Drafted against committed baseline `e4d691ae4182af96886696f09754d3f0e7d752dc`. Controlling authority: the programme-level Unit 3 Reliability Contract and Remedy Selection Planning Review (`7e9e388`/`911d1c6`), whose own Section 11 defines Unit 3-E as "a governance decision, backed by 3-D's evidence, selecting one architecture or mechanism (or declining to select one). A document, not code"; the original top-level Reasoning Protocol Live-Model Conformance and Structured-Output Reliability Planning Review (`b34f8d0`), whose own Section 13 five-unit table reserves implementation to a separate Unit 4 and formal qualification/closure to a separate, later Unit 5; the frozen Unit 3-A Reliability Contract Definition Scope Lock (`ab27f18`); the frozen Unit 3-B Remedy Experiment Scoping Scope Lock (`55af571`), whose Section 3 classifies hybrid approaches as deferred and whose Section 10 comparison discipline forbids attributing an effect to any untested combination; the frozen Unit 3-C Scope Lock (`fee2edd`) and its Timeout + Durability Amendments; the committed Unit 3-D Comparative Evaluation Scope Lock, Review, and Independent Constitutional Review (`32b55d4`/`2e468ed`); and the committed Unit 3-E Remedy Selection Planning Review and its Independent Review (`e4d691a`). Where this Scope Lock adopts a Planning Review conclusion, it does so because independent re-reading during drafting found no constitutional defect requiring departure; none was found.

## 2. Unit 3-E boundary (Phase 2)

Unit 3-E produces a governance decision — a document, never code. It is backed exclusively by Unit 3-D's own already-completed comparative evidence; it gathers no new evidence, runs no campaign, and performs no comparison Unit 3-D has not already performed. It may select exactly one already-tested candidate or explicitly decline to select any. It does not implement anything, does not modify production code, and does not itself constitute or authorize production qualification — that remains a separate, later unit (Unit 5, per the original top-level Planning Review's own Section 13) requiring qualification-tier evidence no current candidate possesses. This Scope Lock itself selects no remedy — it freezes only the methodology and boundary a future Unit 3-E selection task must operate within.

## 3. The selection question (Phase 3)

Frozen, exactly, as the question a future Unit 3-E task answers:

> **Whether the available governed evidence justifies directing Unit 4 toward exactly one already-tested candidate architecture/mechanism, or whether NO REMEDY should be selected yet.**

A selection, if eventually made, is **provisional and directional only** — it commits Unit 4 to implementation effort and Unit 5 to eventual formal qualification; it does not itself declare production readiness, does not itself satisfy the Unit 3-A Reliability Contract at qualification tier, and does not foreclose Unit 5 from later declining to qualify the selected candidate.

**Unit 3-E may not, under this frozen question:** design a new candidate mechanism; combine two or more tested candidates into an untested hybrid or composite architecture; select more than one candidate for parallel implementation; or equate an affirmative selection with a declaration of production readiness. Any future Unit 3-E output that does any of the above has exceeded this Scope Lock's own frozen boundary, regardless of how it characterizes itself.

## 4. Current evidence sufficiency (Phase 4)

Frozen, inherited from the Unit 3-E Remedy Selection Planning Review's own independently-reasoned finding, itself independently re-verified by that Planning Review's own Independent Review:

```text
CURRENT EVIDENCE SUFFICIENCY: SUFFICIENT TO NARROW, NOT SUFFICIENT TO SELECT.
```

This is a **current finding**, not a permanent one — a future Unit 3-E task operating under fresh evidence (e.g., a separately-authorized extension of Control's, Family A's, or Family B's own truncated live exposure) may reach a different sufficiency finding on that fresh basis. **What this Scope Lock forbids is redefining the standard itself to manufacture sufficiency out of the existing, unchanged evidence** — no future Unit 3-E task may lower the evidentiary bar, reinterpret "sufficient" more permissively, or otherwise talk itself into a selection the current evidence does not support, without a fresh, separately-governed, explicit act changing the standard itself (not merely applying the existing standard more generously). Any future Unit 3-E task that reaches an affirmative selection on the *current, unchanged* evidence base must explain, explicitly and specifically, what in the evidence itself (not the standard) changed to justify a different sufficiency conclusion than this Scope Lock records — silence on this point is itself a defect a future Independent Constitutional Review must catch.

No candidate is currently eliminated — the evidence is too thin to responsibly select or reject any one candidate on constitutional/safety/reliability grounds alone (Section 6).

## 5. Admissible evidence matrix (Phase 5)

Frozen, binding, adopted from the Unit 3-E Planning Review's own independently-derived matrix, re-verified during this Scope Lock's drafting against the same primary Unit 3-D evidence and found to contain no defect requiring amendment:

| Evidence | Classification | Scope |
|---|---|---|
| Unit 3-D matched-subset semantic results | **ADMISSIBLE** | The only rigorously cross-arm-comparable semantic evidence (Unit 3-D Scope Lock Section 5's frozen five-fixture subset). |
| Unit 3-D false-negative findings | **ADMISSIBLE** | Matched-subset scope only, per the same basis. |
| False-positive REMEMBER occurrence (qualitative fact) | **ADMISSIBLE WITH QUALIFICATION** | The bare fact and triggering fixture/category are usable; a cross-arm rate comparison is not (Unit 3-D Scope Lock: NOT COMPARABLE). |
| False-positive GOAL occurrence | **ADMISSIBLE, NON-DISCRIMINATING** | Zero everywhere in current evidence; usable as a confirmed fact only. |
| Representation validity | **ADMISSIBLE WITH QUALIFICATION** | Usable as a minimum-gate sanity check; non-discriminating among current candidates (100% everywhere). |
| Parser reliability | **ADMISSIBLE WITH QUALIFICATION** | Same basis as representation validity. |
| Transport reliability | **ADMISSIBLE WITH QUALIFICATION** | Same basis; Family C structurally exempt (no transport layer) — the exemption must be stated, never presented as an advantage. |
| Completion rate | **ADMISSIBLE** | Operational, arm-wide comparable per Unit 3-D's own frozen matrix. |
| Timeout rate | **ADMISSIBLE WITH QUALIFICATION** | Comparable operationally; must never be folded into a semantic finding; Family C structurally exempt. |
| Family C 29-fixture deterministic evidence | **ADMISSIBLE, Family-C-specific** | Usable for Family C's own standalone characterization and, at the matched subset, for cross-arm comparison at Family C's own `n=1` denominator — never pooled with the model arms' own `n=5` denominators. |
| Implementation complexity | **ADMISSIBLE** | Legitimate, already-anticipated governance input (Unit 3 Planning Review's own architectural-boundary analysis). |
| Architectural intrusiveness | **ADMISSIBLE** | Same basis, tied to the already-frozen architectural-boundary list (domain-contract stability, parser/prompt-builder pluggability, "model as sole semantic authority," retry/repair invariants). |
| Reversibility | **ADMISSIBLE, PROSPECTIVE ONLY** | A legitimate forward-looking architectural/risk factor; never tested by any Unit 3-C experiment and never presented as an evidentiary finding. |
| Model dependence | **ADMISSIBLE** | Tied to Unit 3-A Section 11's own model/provider qualification obligation. |
| Evidence coverage (exposure fraction) | **ADMISSIBLE, MANDATORY** | Must be disclosed and weighed wherever any other figure from the same arm is cited — not optional. |
| Known failure modes | **ADMISSIBLE** | Directly evidence-based, per family. |
| Constitutional risk | **ADMISSIBLE, PRIMARY** | The single most heavily governance-emphasized criterion in this programme (Section 6). |
| `contentFidelity` | **NOT AVAILABLE** | Never computed for any candidate, any arm, any provenance source. **No proxying, estimation, or inference is permitted, by any means, under any circumstance.** |
| Any population-rate or statistical-significance claim | **NOT ADMISSIBLE** | Descriptive-only statistical authority (Unit 3-D Scope Lock Section 12) governs every figure Unit 3-E may cite. |
| Qualification-tier conformance claims | **NOT ADMISSIBLE** | No candidate's evidence meets Unit 3-A's own qualification-tier bar; applying it here would misapply a later-tier threshold to an earlier-tier decision (Section 9). |
| Trials-to-first-safety-failure (as a ranking factor) | **NOT ADMISSIBLE** | Explicitly NOT COMPARABLE per the Unit 3-D Scope Lock's own informative-censoring analysis. |
| Untested hybrid performance | **NOT AVAILABLE** | No evidence exists for any specific combination (Section 10). |

## 6. False-positive safety (Phase 6)

Frozen:

1. **False-positive REMEMBER is constitutionally significant** — Unit 3-A Section 7's own zero-tolerance framing, unweakened and unstrengthened by this document.
2. **One observed false-positive REMEMBER in every tested family (including Control, the currently-deployed, unmodified baseline) does not prove equivalence.** The four arms' own denominators (132, 51, 91, and 6-live-plus-23-offline) differ sharply under informative censoring; the true underlying rate for any arm's own full, un-truncated schedule remains unknown.
3. **Timing of the first false-positive event may never be used as a ranking factor.** A later trigger reflects a larger or differently-composed exposure, not a demonstrated safety property.
4. **No numeric selection threshold may be invented anywhere in a future Unit 3-E task** — governance does not state, and this Scope Lock does not manufacture, a specific count or rate of false positives that automatically disqualifies a candidate.
5. **False-positive GOAL count remains zero in all observed evidence** — usable as a confirmed, non-discriminating fact.
6. **Qualification-tier zero-tolerance (Unit 3-A Section 7) remains a fully binding future bar** on whatever Unit 4 eventually implements and Unit 5 eventually qualifies — Unit 3-E's own selection does not satisfy, weaken, or substitute for that later requirement.

**Determination, frozen:**

```text
B — a serious risk factor, not automatic elimination.
```

Reasoned, not preferred: applying Unit 3-A Section 7's own qualification-tier zero-tolerance bar mechanically at exploratory-tier sample sizes would disqualify **every current candidate, including Control itself** — an outcome that cannot be the intended reading, since it would render the already-deployed status quo simultaneously unselectable by its own governing standard. This programme's own existing design already establishes the correct posture by direct precedent: the Unit 3-C safety checkpoint (Scope Lock Section 16) responds to a first-occurrence adversarial-category false positive by pausing for manual review, not by automatic, permanent elimination — an analogy from a different governance layer (an in-campaign operational control, not a cross-campaign selection-tier rule), stated here explicitly as an analogy illustrating the correct posture, not as a claim of direct governing authority over Unit 3-E's own selection question. **No exception to item 4 above is created by this determination.**

## 7. Family A rule (Phase 7)

Frozen:

- Decision-step and rendering-step evidence remain permanently separate in any future Unit 3-E output. **No combined Family A score, of any kind, in any form, may ever appear.**
- Rendering-step performance (24/24 in current evidence) may never mathematically or rhetorically compensate for, offset, or be averaged against decision-step weakness (14/26 arm-wide, 14/25 matched-subset current evidence) — the two measure architecturally different questions (independent semantic decision versus content rendering given a known-correct decision as a stated input).
- Family A's one genuine model timeout remains operational evidence only, kept separate from both semantic figures, never scored as a semantic outcome.
- The matched five-fixture subset (Unit 3-D Scope Lock Section 5) is the only permissible cross-family basis for Family A's own decision-step semantic comparison; Family A's own arm-wide figures (which include its own single `p03-ambiguous-memory` decision trial) remain usable for Family A's own intra-arm characterization but not for cross-family ranking outside the matched subset.
- False-positive (one, decision step, `p03-ambiguous-memory`) and false-negative (8/15 on matched REMEMBER fixtures, decision step) behavior remain fully visible in any future Unit 3-E output — never omitted, never summarized away.
- Any architectural implication of Family A's own decision/rendering separation (e.g., "this architecture cleanly isolates a harder task from an easier one") is **descriptive of an observed property only** — it is not, and may not become, an implementation design decision. Designing how such a separation would actually be implemented belongs exclusively to Unit 4, if and when Family A is ever selected.

## 8. Family B rule (Phase 8)

Frozen:

- The matched five-fixture subset is the only permissible cross-family semantic comparison basis for Family B (10/25 current evidence), exactly as for Family A and Control.
- No inference may be drawn about Family B's own censored, unobserved remainder (24 of its own 115-trial schedule never attempted) — no future Unit 3-E output may state or imply what those trials would have shown.
- False-negative (14/15 on matched REMEMBER fixtures) and false-positive (one, `g03-later-action`) behavior remain fully visible, never omitted.
- Operational reliability (100% completion, 100% representation validity, zero parser/transport failures) may be considered only with its own exposure fraction (79.1%) stated in the same breath.
- **Family B's own matched-subset figures being numerically similar in magnitude to Control's own (10/25 each) does not establish equivalence.** Similar raw counts under different, informatively-censored exposure never establish that two arms behave the same way, and no future Unit 3-E output may state or imply that Family B's own prompt/protocol redesign "made no difference" on this basis.

## 9. Family C rule (Phase 9)

Frozen:

- The full 29-fixture deterministic result (24/29 correct, provenance-separated: 6 live/Source A + 23 offline/Source B) is admissible as **Family-C-specific evidence** — usable for Family C's own standalone characterization and, at the matched subset, for cross-arm comparison at Family C's own `n=1`-per-fixture denominator, never pooled with the model arms' own `n=5` denominators.
- 4 false-positive REMEMBER (`p03-ambiguous-memory`, `p04-embedded-tags`, `p05-mixed-memory-discussion`, `p12-injection`), 0 false-positive GOAL (structurally unreachable by this mechanism's own binary output space, not a demonstrated safety property), 1 false negative (`r03-dont-forget`) — all fully characterized and attributable to specific, named classifier logic, restated here as binding reference data any future Unit 3-E output must not contradict without fresh evidence.
- All nine Unit 3-B Section 4 adversarial categories are exercised (live + offline combined), independently re-confirmed by the governing Family C Offline Completion Determination and its own Independent Constitutional Review.
- **Zero model calls is an architectural property, never proof of superior model reliability.** Family C's own 100% completion rate and structurally-absent timeout/transport-failure rates reflect the absence of a transport layer, not a demonstrated resilience to one.
- **Determinism is not automatic safety.** Family C's own known, fixed failure modes are real and constitutionally significant (Section 6); reproducibility of a failure mode is not itself a safety property distinct from the failure mode's own existence.
- Live (Source A, 6 fixtures) and offline (Source B, 23 fixtures) evidence remain provenance-separated in every future Unit 3-E citation — never merged into an unmarked "29 observed" figure, never attributed to any live campaign.
- **Family C may not be treated as qualification-ready on the strength of its own exploratory-tier evidence**, however complete its own fixture/category coverage — completeness of coverage at exploratory tier is not equivalent to, and does not substitute for, qualification-tier sample size or statistical rigor (Section 4).

## 10. Hybrid rule (Phase 10)

Frozen, restated with maximal precision:

**Allowed future selection outcomes:**

A. exactly one already-tested candidate (Control/no-change, Family A, Family B, or Family C);
B. **NO REMEDY SELECTED.**

**Prohibited, absolutely, under any characterization:** an untested hybrid; a combination of two or more tested candidates; a composite architecture; "picking the best parts" from Control/A/B/C into a new design; any mechanism invented, assembled, or proposed at selection time that was not itself an object of Unit 3-C's own live or offline experimentation. **Hybrid approaches (Unit 3-B Section 3, Family I) remain deferred, exactly as already frozen, unless and until a separate, future governance act — a fresh Unit 3-B-tier scoping decision followed by fresh Unit 3-C-tier experimentation for that specific combination — expressly re-governs them.** Unit 3-D's own completion, and this Scope Lock's own existence, do not constitute such an act and may not be cited as having done so.

## 11. Decision method (Phase 11)

Frozen, adopted from the Unit 3-E Planning Review's own reasoning:

```text
QUALITATIVE CONSTITUTIONAL DECISION MATRIX + MANDATORY MINIMUM GATES + NARRATIVE TRADEOFF ANALYSIS.
```

**Mandatory minimum gates** must be stated only where they can be stated honestly, without inventing a threshold governance does not support: representation validity, parser reliability, and zero false-positive GOAL may serve as pass/fail sanity gates, since every current candidate already satisfies each trivially. **A "zero false-positive REMEMBER" gate may not be imposed at this evidentiary tier** — doing so would eliminate every current candidate, including Control, per Section 6's own reasoning.

**Prohibited, absolutely:** weighted numerical scoring of any kind; arbitrary point systems; any construction that sums, averages, or otherwise numerically combines figures drawn from different denominators, different repetition designs, or different evidentiary tiers; significance-style claims ("statistically better," "meaningfully higher") of any kind; hidden or undisclosed weighting of any criterion relative to another; automatic selection triggered by a single metric crossing an undisclosed or ad hoc threshold. **Reasoning:** the admissible evidence (Section 5) spans metrics with fundamentally different denominators (`n=25` pooled for the model arms' matched subset, `n=5` for Family C's own design), different epistemic status (hard counts versus qualitative architectural properties with no numeric scale), and a constitutional risk factor this programme's own governance explicitly declines to reduce to a single number (Section 6). A numeric score would manufacture statistical resolution and comparability the underlying exploratory-tier samples cannot support — functionally an undisclosed significance claim, already prohibited by Section 5.

Any future Unit 3-E selection document must show its own reasoning explicitly, criterion by criterion, against the Section 5 admissible-evidence matrix, in prose — not as a table of scores summing to a total.

## 12. No-selection path (Phase 12)

Frozen: **NO REMEDY SELECTED is a full, lawful, non-failure outcome**, co-equal with an affirmative selection, exactly as the programme-level Unit 3 Planning Review's own governing definition already states ("selecting one architecture or mechanism **or declining to select one**"). If no candidate meets the future selection standard actually applied (Section 11), the eventual selection document must state this plainly, in its own words, not bury it as an implicit non-outcome.

**No-selection must never be characterized, anywhere in a future Unit 3-E output, as a programme failure, a wasted effort, or a deficiency requiring an apologetic justification.** Possible downstream consequences of a no-selection outcome may include additional Unit 3-C-tier evidence gathering, reconsideration of currently-deferred remedy families, additional governance work, or a deliberate programme pause on remedy selection specifically — **this Scope Lock does not pre-authorize, select among, or require any of these paths**; identifying them as plausible is not the same as committing to one, and a future Unit 3-E task that reaches a no-selection outcome need not itself decide what happens next.

## 13. Unit 4 firewall (Phase 13)

Frozen, absolute: Unit 3-E may select one already-tested remedy family or explicitly select none. **Unit 3-E must not, under any characterization or justification**: design a production implementation of any kind; modify `DefaultReasoningPromptBuilder`, `TaggedReasoningResponseParser`, or any other production file; implement deterministic classification or any other mechanism in `src/`; define a rollout plan; define a migration plan; alter the production runtime in any way; authorize deployment of any kind; declare production readiness. Every one of these belongs exclusively to Unit 4 (implementation, per the original top-level Planning Review's own Section 13: "implement only the accepted minimal transport, prompt, parser, or configuration surface... no semantic repair unless expressly authorized") or to the separate, later Unit 5 (qualification and closure). None is performed, drafted, or implied by this Scope Lock, and none may be performed by any future Unit 3-E selection document, which remains "a document, not code" under its own governing definition (Section 2).

## 14. Future remedy-selection document structure (Phase 14)

Frozen as the minimum required structure of any future Unit 3-E Remedy Selection document — not written by this Scope Lock:

1. Authority, restating Section 2's own boundary.
2. Admissible evidence, per Section 5, cited from Unit 3-D's own already-completed record, not re-derived.
3. Current evidence sufficiency, applying Section 4's own standard (or an explicitly, separately justified revision of it).
4. Decision method actually used, per Section 11.
5. Per-family assessment, preserving every rule in Sections 7–9.
6. False-positive safety assessment, per Section 6.
7. Elimination findings, if any — this Scope Lock's own governing Planning Review found none on current evidence; a future task with different evidence might.
8. Unresolved risks, carried forward from every prior governing document in this chain.
9. No-selection analysis, per Section 12 — genuinely considered, not perfunctorily dismissed.
10. Final selection determination — one candidate, or explicit no-selection, with prose reasoning, never a score.
11. Unit 4 firewall, per Section 13, restated in the document's own words.
12. Evidence explicitly carried forward to Unit 4 (if a selection is made) — what Unit 4 may assume as already-established versus what it must still independently verify or mitigate.
13. Prohibited interpretations, naming explicitly what the document must not be read as (a production-readiness claim, a qualification-tier claim, an implicit hybrid endorsement, a permanent elimination of any non-selected candidate).

## 15. Exit criteria (Phase 15)

This Scope Lock is ready to freeze once it has passed Independent Constitutional Review with a verdict of ACCEPTED or ACCEPTED WITH QUALIFICATIONS whose qualifications are non-blocking.

A future Unit 3-E remedy-selection task, operating under this frozen Scope Lock, is complete once:

1. all admissible evidence (Section 5) has been considered;
2. all inadmissible or unavailable evidence has been excluded, with `contentFidelity` explicitly confirmed unproxied and uninferred;
3. Family A's decision/render split is preserved throughout, with no combined score anywhere (Section 7);
4. Family C's live/offline provenance separation and architectural asymmetry are disclosed throughout (Section 9);
5. false-positive safety is addressed per Section 6, with no invented numeric threshold;
6. no hidden ranking, weighting, or scoring appears anywhere (Section 11);
7. no untested hybrid or combination is selected or implied (Section 10);
8. no-selection was genuinely considered as a co-equal outcome, not perfunctorily dismissed (Section 12);
9. exactly one candidate is selected, or none is — never more than one, never an invented combination;
10. no production implementation is designed, drafted, or implied anywhere (Section 13);
11. the Unit 4 firewall is preserved intact throughout;
12. every evidence limitation identified by this Scope Lock and by the Unit 3-D/Unit 3-E governance chain it inherits from is explicitly carried forward, not silently dropped;
13. that task's own Independent Constitutional Review reaches an accepting verdict.

**Unit 3-E's own exit is never conditioned on reaching an affirmative selection.** A remedy-selection document that honestly concludes "no candidate currently meets the standard this Scope Lock defines" satisfies Unit 3-E's own purpose exactly as fully as one that selects a candidate — declining to select is not a deficiency; manufacturing a selection the evidence does not support would be.

## 16. Prohibited interpretations

This Scope Lock may not be read as: performing any remedy selection itself (none is performed — every evidentiary figure cited above is restated, not newly computed, from already-independently-verified Unit 3-D governance); silently lowering the Section 4 sufficiency bar; eliminating any candidate; treating Family A's rendering strength as decision-step evidence; treating Family C's determinism or completeness as automatic safety or qualification-readiness; treating Family B's censored figures as evidence of equivalence to Control; authorizing selection of a hybrid, combination, or newly-invented mechanism; authorizing any live model call, any new Unit 3-C campaign, or any modification to Unit 3-C/3-D artifacts; authorizing any Unit 4 work of any kind; or freezing anything beyond the selection methodology and boundary stated in Sections 3–15.

## 17. Exit disposition

```text
PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```

Not frozen. No remedy selection, candidate ranking, hybrid construction, or Unit 4 work of any kind is authorized by this document pending that review and any further required governance.
