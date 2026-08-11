**Status:** Unit 3-D — Comparative Evaluation — Scope Lock — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** Freezes the evidence sources, exposure-disclosure obligations, matched-subset methodology, censoring rules, metric comparability matrix, family-specific treatment rules, statistical-authority ceiling, and remedy-selection firewall that any future Unit 3-D comparative-evaluation task must satisfy. Contains no comparative evaluation, no metric computation beyond what is already frozen by cited prior governance, no remedy ranking, and authorizes no live model call.

# Reasoning Protocol Live-Model Conformance Unit 3-D — Comparative Evaluation — Scope Lock

## 1. Baseline

Drafted against committed baseline `ccacf470948e8a995d5da849deb06e6a4f3e256a`. Controlling authority: the programme-level Unit 3 Reliability Contract and Remedy Selection Planning Review (`911d1c6`), whose own Section 11 defines Unit 3-D as owning comparative evaluation only ("determines what the experiment evidence establishes") and Unit 3-E as owning remedy selection, exclusively and separately; the frozen Unit 3-A Reliability Contract Definition Scope Lock (`ab27f18`); the frozen Unit 3-B Remedy Experiment Scoping Scope Lock (`55af571`), whose Section 6 (evidence tiers), Section 9 (semantic/representation separation), and Section 10 (comparison discipline) are directly controlling here; the frozen Unit 3-C Scope Lock (`fee2edd`) and its Timeout + Durability Amendments (`0072c01`) and Scored-Trial Timeout Semantics Determination (`144cf51`); the committed Unit 3-C Attempt 6 Execution Evidence Review and its Independent Constitutional Review (`b5c0749`); the committed Unit 3-D Comparative Evaluation Planning Review (`239c6c1`); and the committed Unit 3-D Family C Offline Completion Determination and its Independent Constitutional Review (`ccacf47`). Where this Scope Lock adopts a Planning Review or Determination conclusion, it does so because independent re-reading during drafting found no constitutional defect requiring departure; none was found.

## 2. Unit 3-D boundary (Phase 2)

Unit 3-D determines what Unit 3-C's evidence establishes against the full Unit 3-A Reliability Contract. It does not itself rank, prefer, or select a remedy — that is Unit 3-E's exclusive, later, separately-governed function (Section 14). Unit 3-D performs no implementation, no experiment execution, no live model call, and does not reopen any Unit 3-C mechanism, fixture, schedule, or artifact. This Scope Lock itself authorizes no comparative evaluation — it freezes only the boundary and methodology a future comparative-evaluation task must operate within.

## 3. Admissible evidence sources (Phase 3)

Frozen, with mandatory provenance separation — no future Unit 3-D output may blend these sources without explicit labeling of which source underlies which figure:

| Source | Description | Permitted use |
|---|---|---|
| **A — Attempt 6 live evidence** | The durable `raw.jsonl`/`intent.jsonl`/`timeouts.jsonl`/`SAFETY_CHECKPOINT` artifacts of campaign `unit3c-remedy-experiments-20260810-03`, the only campaign whose observation-durability defect was corrected before execution. | Operational metrics (completion, timeout, representation validity, parser/transport failure) for Control, Family A, Family B, and Family C's own live subset (6 fixtures); semantic/false-positive/false-negative figures for the same. |
| **B — Family C offline deterministic completion evidence** | The 23 additional fixture classifications independently re-derived in the Unit 3-D Family C Offline Completion Determination, produced by re-executing the exact, unmodified, already-committed `Unit3CCandidateC1` classifier against the exact, unmodified, already-committed remaining fixture text — zero model calls, zero campaign attribution. | Family C's own internal, full-29-fixture, all-nine-category semantic/false-positive/false-negative characterization only. **Never** attributable to, or blended with, campaign `unit3c-remedy-experiments-20260810-03` itself. |
| **C — prior governance / frozen expected classifications** | Fixture `expectedAction`, `FixtureCategory`, and the corrected Plan Section 9 predicted trace table. | Ground truth for correctness computation; never itself a result. |
| **D — operational evidence** | Completion, timeout, representation-validity, parser-failure, and transport-failure figures — properties of *whether and how* a trial resolved, independent of *which* fixture was reached. | Arm-wide comparison across the full attempted-exposure population of each arm (Section 4), because these properties do not depend on fixture-composition matching (Unit 3-B Section 10's "unequal fixture sets may not be casually compared" restriction is a semantic-content concern, not an operational-resolution concern). |
| **E — semantic evidence** | `actualAction`/`expectedAction` correctness, false-positive/false-negative classification, and any property that depends on *which* fixture produced the observation. | Cross-arm comparison restricted to the matched five-fixture subset (Section 5); intra-arm (Family A decision-vs-render, Family C's own full-corpus) comparison unrestricted by the matched subset, since no cross-arm fixture-composition mismatch exists at that scope. |

No future Unit 3-D output may cite source B as though it were source A, or compute a combined "Family C: 29/29" figure without stating that 6 are live and 23 are offline. No future Unit 3-D output may compute a semantic comparison across arms using source E outside the matched subset without this Scope Lock's own Section 5 exception process.

## 4. Exposure disclosure (Phase 4)

Frozen, mandatory: every Unit 3-D output presenting any per-arm figure — operational or semantic — must disclose that arm's own exposure fraction and, where semantic, the exact fixture composition underlying it. Minimum frozen exposure table, independently re-verified against primary artifacts by both the Attempt 6 Independent Execution Evidence Review and the Unit 3-D Planning Review, restated here as binding reference data:

| Arm | Scheduled maximum | Attempted | Completed | Timeouts | Exposure fraction |
|---|---|---|---|---|---|
| Warm-up | 3 | 3 | 3 | 0 | 100.0% |
| Control | 145 | 132 | 132 | 0 | 91.0% |
| Family A | 220 | 51 | 50 | 1 | 23.2% |
| Family B | 115 | 91 | 91 | 0 | 79.1% |
| Family C (live) | 29 | 6 | 6 | n/a | 20.7% |
| Family C (offline completion) | — | 23 | 23 | n/a | remaining 79.3%, offline-sourced |

**Prohibited, absolutely:** any report presenting a per-arm rate, count, or figure without its own denominator and exposure fraction stated adjacently; any report presenting Family A's, Family B's, or Control's own truncation as if it were a completed, full-schedule dataset; any silent omission of the fact that three of four arms halted before exhausting their own live schedule.

## 5. Matched semantic subset (Phase 5)

Frozen: **`r01-direct`, `r02-please`, `r03-dont-forget`, `p01-ordinary-fact`, `p02-quoted-remember`** — independently re-derived and re-confirmed, by both the Unit 3-D Planning Review and this Scope Lock's own fresh re-check against Section 4's exposure table, as the only fixture set on which Control, Family A's own decision step, and Family B all completed the full frozen `n=5` repetition, and on which Family C completed its own frozen `n=1` (available from live evidence for these five specifically, independent of the offline completion). **This is the exclusive basis for any cross-arm semantic-correctness or false-negative comparison** unless a future, separately-governed amendment to this Scope Lock — not an ad hoc decision inside a comparative-evaluation task itself — explicitly authorizes another method. The set may not be enlarged by retrospective alignment (e.g., treating a fixture where three of four arms reached `n=5` and one reached `n=4` as "close enough"); partial matches remain outside this subset, full stop. Family A's own render-step results, and Family C's own remaining 24 fixtures (5 already in the matched set, 24 outside it — 23 offline plus `p03`, which is live but not part of the five-fixture semantic-matched set since it is not shared at full repetition across Control/Family A-decision/Family B, its own role being the Family A/Family C false-positive trigger), are not part of the cross-arm matched-subset comparison, though they remain usable for the specific intra-arm and Family-C-internal purposes Sections 8–9 separately authorize.

## 6. Censoring rules (Phase 6)

Frozen, restated as binding methodological constraints, not merely descriptive commentary a future task may weigh differently:

1. **Safety-checkpoint halting is informative right-censoring**, not random censoring — it fires deterministically on the exact adverse event (`isAdversarialCategoryFalsePositive`, first occurrence) that any semantic or false-positive comparison would also want to measure.
2. **Fixture-composition truncation is non-random** — an arm that halts earlier is systematically restricted to the earliest fixtures in the shared, frozen corpus order; this is truncation on the covariate (fixture content/category), not only on the outcome.
3. **The unobserved remainder of any arm's schedule may not be inferred to behave like the observed portion.** No future Unit 3-D output may state or imply what Control's, Family A's, or Family B's own untested trials "would probably have shown."
4. **Later checkpoint timing may not, by itself, be used to imply superior reliability.** An arm that halted later did so in part because it was exposed to a different, larger, or differently-composed set of fixtures — not necessarily because its own underlying mechanism is safer.
5. **Raw arm-wide semantic aggregate ranking is prohibited** — any figure of the form "Arm X: N/M correct" computed over each arm's own full, differently-composed attempted set, presented as directly comparable to another arm's own differently-composed N/M, is a prohibited comparison under this Scope Lock.

Every future Unit 3-D output must state rules 1–5 prominently, in its own text, not merely by citation to this Scope Lock — a reader must not need to retrieve this document to understand the limitation on any comparative claim presented.

## 7. Metric comparability matrix (Phase 7)

Frozen, binding. Adopted from the Unit 3-D Planning Review's own independently-derived matrix, re-verified during this Scope Lock's drafting against the same primary evidence and found to contain no defect requiring amendment:

| Metric | Classification | Binding scope |
|---|---|---|
| Completion rate | **COMPARABLE** | Arm-wide (Source D), all arms, using Section 4's own exposure denominators. |
| Model-timeout rate | **COMPARABLE** | Arm-wide (Source D); Family C excluded (no transport layer, not a null result but a structurally inapplicable metric). |
| Representation validity | **COMPARABLE** | Arm-wide (Source D); non-discriminating in this evidence cycle (100% in every arm), stated as such. |
| Parser-failure rate | **COMPARABLE** | Arm-wide (Source D); non-discriminating (zero everywhere), stated as such. |
| Transport-failure rate | **COMPARABLE** | Arm-wide (Source D); non-discriminating (zero everywhere), stated as such. |
| Semantic correctness | **COMPARABLE WITH QUALIFICATION** | Matched five-fixture subset only (Section 5); raw arm-wide aggregates are a prohibited comparison (Section 6, rule 5). |
| False-negative rate | **COMPARABLE WITH QUALIFICATION** | Matched five-fixture subset only — the only fixtures present as REMEMBER-expected in every relevant arm's own matched data. |
| False-positive REMEMBER occurrence (binary, did it happen) | **COMPARABLE** | Descriptive fact only — every arm's own occurrence (yes/no) and triggering fixture/category may be stated. |
| False-positive REMEMBER rate | **NOT COMPARABLE** | Denominators and fixture exposure differ sharply across arms (Section 6); a rate-based ranking is prohibited. |
| False-positive GOAL | **COMPARABLE, TRIVIALLY** | Zero in every arm; non-discriminating, stated as such; Family C is structurally incapable of this failure mode (its own signal space has no GOAL-equivalent output) and this must be disclosed, not presented as a demonstrated safety property. |
| Trials-to-first-safety-failure | **NOT COMPARABLE** | Directly subject to informative-censoring and fixture-composition bias (Section 6); Control's own trigger fixture (`p17-hypothetical-remember`) has no counterpart exposure in Family A or Family B at all, making any cross-arm ordinal or count-based comparison invalid on its face. |
| `contentFidelity` | **NOT AVAILABLE** | Never computed by the current experiment implementation for any trial, any arm (Section 13). |
| Latency/cost | **COMPARABLE WITH QUALIFICATION** | Family C's own zero-latency, zero-inference-cost profile is an architectural property, not a reliability finding (Section 9); must be reported with that qualification whenever presented alongside Control/Family A/Family B's own real per-call latency. |
| Family C deterministic classification accuracy | **COMPARABLE, Family-C-internal only** | Fully available across all 29 fixtures (Sources A+B combined, provenance-labeled); usable for Family C's own standalone characterization and for the matched-subset cross-arm comparison (Section 5) at the five shared fixtures; not usable to construct any arm-wide aggregate outside the matched subset for cross-arm ranking purposes. |

No metric is classified more favorably here than the Planning Review's own independent derivation without a fresh, primary-evidence-grounded justification; none was found necessary during this Scope Lock's drafting.

## 8. Family A treatment (Phase 8)

Frozen: **Family A's decision-step and rendering-step results must be reported separately, in every future Unit 3-D output. Aggregation into one blended Family-A-wide semantic score is prohibited.** Reason, restated as binding rationale, not merely historical commentary: the two steps measure materially different questions — the decision step asks whether the model, unaided, arrives at the correct semantic action (14/26 correct in the matched-subset-adjacent live data, independently re-confirmed by the Unit 3-D Planning Review); the rendering step supplies that action as a known, stated given and asks only whether the model can produce correctly-tagged content for it (24/24 correct in the same data) — a fundamentally easier, near-guaranteed-compliance task once the hard part (deciding) is already solved externally. A blended aggregate would silently launder the decision step's own weaker performance behind the render step's near-perfect compliance, producing a number that answers no well-posed question and materially overstates Family A's own independent semantic-decision reliability.

**Permitted intra-Family-A comparisons:** decision-step accuracy versus render-step accuracy, reported as two explicit, separately-labeled figures; decision-step performance within the matched five-fixture subset versus decision-step performance across Family A's own full attempted set (23.2% exposure), provided both are labeled with their own exposure/composition; the qualitative observation that render-step compliance, given a correct decision, is near-perfect — stated as evidence about content-rendering fidelity given known intent, not as evidence about Family A's own decision-making reliability. **Prohibited:** any single "Family A semantic correctness: X%" figure that does not state which sub-step it describes.

## 9. Family C treatment (Phase 9)

Frozen:

1. **Provenance separation is mandatory and permanent.** Live Attempt 6 evidence (6 fixtures) and offline deterministic completion evidence (23 fixtures) must remain explicitly labeled wherever cited, individually or in combination, in every future Unit 3-D output — never merged into an unmarked "Family C: 29 observed" figure.
2. **All 29 deterministic classifications may be described for Family C's own standalone characterization** — correctness, false positives, false negatives, and per-category results across the full corpus, per the Family C Offline Completion Determination's own Section 7 findings, independently re-verified by that Determination's own Independent Constitutional Review via four distinct methods.
3. **The matched-subset comparison (Section 5) may use Family C's own results at `r01`, `r02`, `r03`, `p01`, `p02`** — all five already available from live evidence (Source A), independent of the offline completion.
4. **Zero model calls must never be interpreted, stated, or implied as superior model reliability.** Family C's own 100% completion rate and structurally-zero timeout rate reflect the absence of a transport layer, not a reliability finding about resisting timeouts or transport failures — any future output presenting these figures must state this explicitly, adjacent to the figure, not in a footnote a reader could miss.
5. **Timeout, latency, and cost comparisons involving Family C require explicit architectural qualification** (Section 7's own latency/cost row) — Family C's near-zero latency and zero inference cost are properties of being a pure function, not evidence of a faster or cheaper *model-based* mechanism.
6. **The nine-category adversarial coverage gap (Unit 3-B Scope Lock Section 4) is closed** for Family C, per the Family C Offline Completion Determination's own independently-verified finding — all nine categories are now fully exercised, combining live and offline evidence with provenance intact. This closure does not extend to, or imply anything about, Control's, Family A's, or Family B's own truncation, which remains architecturally un-closeable offline (Section 4).

**Deterministic architecture must not be treated as automatically preferable** to a model-invoking mechanism anywhere in a future Unit 3-D output — the absence of stochastic/transport failure modes is a structural fact about the mechanism class, disclosed transparently per items 4–5 above, never presented as a comparative advantage without that qualification attached in the same sentence or table row.

## 10. Timeout treatment (Phase 10)

Frozen, per the already-governed Scored-Trial Timeout Semantics Determination Section 8, restated here as binding on Unit 3-D specifically:

1. **Completion rate and semantic correctness are separate metrics and must always be reported paired** wherever either is presented — never one without the other for the same arm.
2. **A model timeout is operational/reliability evidence, never a semantic answer.** Family A's own one genuine `MODEL_TIMEOUT` (`family_a/r02-please/render/02`) must never be scored, tallied, or counted as a semantic incorrect, a false positive, or a false negative — it has no `actualAction` and none may be fabricated.
3. **Transport/infrastructure failures remain their own, separately classified category** (`TRANSPORT_OR_PROVIDER_FAILURE`/`AMBIGUOUS`), never merged with model timeouts or with semantic outcomes — none occurred in Attempt 6, and this must be stated as an observed fact (zero), not silently omitted.
4. **Unit 3-D must avoid survivorship bias**: any semantic-correctness figure computed only over completed trials must state, in the same breath, the completion rate and timeout count for that same population, so a reader can assess whether the completed subset might be non-representative of the full attempted set.

## 11. False-positive safety rules (Phase 11)

Frozen:

1. **One false-positive REMEMBER occurred in every arm's own safety-truncated execution** (Control: `p17-hypothetical-remember`; Family A: `p03-ambiguous-memory`; Family B: `g03-later-action`; Family C: `p03-ambiguous-memory`, matching its own already-governed frozen prediction) — stated as fact, in every future Unit 3-D output that discusses false-positive safety.
2. **Zero false-positive GOAL events were observed in any arm.**
3. **Equal occurrence count does not establish equivalence.** One false positive in 6 attempts (Family C, live) and one false positive in 132 attempts (Control) are not evidentially equivalent merely because both integer counts are "1" — but the true rate over either arm's full, un-truncated schedule remains unknown (Section 6).
4. **Differing trigger timing does not establish superiority.** No future output may state or imply that Control is "safer" than Family C, or that Family B is "safer" than Family A, on the basis of how many trials preceded each arm's own first false-positive event.
5. **False-positive rates across differently censored arms must not be naively ranked** — consistent with Section 7's own classification of false-positive REMEMBER *rate* as NOT COMPARABLE.

**Permitted descriptive statements:** the bare fact and triggering fixture/category of each arm's own false-positive event; that all four triggers occurred on ADVERSARIAL- or GOAL-category negative-control fixtures (confirming the checkpoint mechanism itself fired on genuine negative controls, not a boundary miscount); that zero false-positive GOAL events occurred anywhere; that Family C's own live trigger (`p03-ambiguous-memory`) is independently, definitively confirmed (not merely deduced) via the corrected observation-durability mechanism, unlike Control's, Family A's, and Family B's own triggers, whose specific `actualAction` values are *also* now definitively durably recorded (a durability-correction fact, not a safety-comparison fact) and must not be conflated with one.

## 12. Statistical authority (Phase 12)

Frozen: **descriptive comparison only.** Unless a future, separately-governed act clearly supports more — none does as of this Scope Lock's own drafting — the following are prohibited in any Unit 3-D output:

- population-rate claims of any kind;
- formal hypothesis testing (no p-values, no significance tests);
- significance claims of any kind, formal or informal ("meaningfully better," "clearly worse");
- qualification-tier claims (Unit 3-A's own ≥300-exposure zero-event gates, ≥99% representation validity, ≥97% one-sided-95%-LCB thresholds may not be applied as a Unit 3-D pass/fail bar — they govern a later, separate, higher evidentiary tier this exploratory-tier evidence was never designed to meet);
- production-readiness claims of any kind;
- extrapolation beyond the observed, attempted exposure of any arm (Section 6).

**Permitted:** exact observed counts and rates, always with explicit denominators; exposure fractions and fixture compositions (Section 4); descriptive statements of what a given figure does and does not, by itself, establish. This ceiling exists because sample sizes are exploratory-tier by design (`n=5` per fixture for model arms, `n=1` for Family C, further truncated in three of four arms), repeated-fixture observations are non-independent, and the informative censoring described in Section 6 means standard confidence-interval machinery does not straightforwardly apply without a censoring-aware treatment no cited governance authorizes inventing inside a Unit 3-D task.

## 13. `contentFidelity` (Phase 13)

Frozen: **`contentFidelity` = NOT AVAILABLE**, for every arm, every fixture, every provenance source (live and offline alike). This field is never computed by the current Unit 3-C experiment implementation (`buildModelInvokingExecutor`/`buildFamilyCExecutor` both hardcode it `null` unconditionally, independently re-confirmed present as literal `null` in every sampled durable record across this programme's history). **Unit 3-D must not invent, estimate, proxy, or infer a content-fidelity result by any means** — not from representation validity, not from semantic correctness, not from qualitative reading of any raw text (raw prompt/response text is itself excluded from the durable schema, per the Observation Durability Defect Confirmation Review's own accepted minimum-evidence determination). Any future content-fidelity work requires its own, separate, future governance act — a new measurement capability, not a Unit 3-D interpretive exercise performed against evidence that does not contain it.

## 14. Remedy-selection firewall (Phase 14)

Frozen, absolute: **Unit 3-D may** identify evidence-supported strengths, weaknesses, tradeoffs, observed failure modes, and unresolved questions; state where one family performs differently from another on a metric this Scope Lock classifies COMPARABLE or COMPARABLE WITH QUALIFICATION, within the scope that classification permits. **Unit 3-D must NOT**: declare a winner; recommend production adoption of any family; select a remedy; authorize implementation of any kind; collapse the evidence into a single ranked recommendation; or begin Unit 4. These functions belong exclusively to Unit 3-E (selection) and Unit 4 (implementation), per the programme-level Unit 3 Planning Review's own Section 11 boundary, unamended and unamendable by this document. Any future Unit 3-D output must avoid language this programme has already recognized as selection-adjacent — "best," "preferred," "recommended," "wins," "superior," "outperforms," or any construction a reasonable reader would understand as a ranking — when characterizing a cross-arm comparison; purely intra-arm or intra-family descriptive language (e.g., "Family A's render step outperforms its own decision step at this task") is not itself prohibited, since it does not compare across remedy families.

## 15. Unit 3-D output requirements (Phase 15)

Frozen as the minimum required structure of any future Unit 3-D comparative-evaluation report — not produced by this Scope Lock:

1. Evidence-source table (Section 3), stating which of A–E underlies each subsequent section.
2. Exposure table (Section 4), for every arm, with fractions and completion/timeout/attempted counts.
3. Censoring disclosure (Section 6), stated in the report's own text, not only by citation.
4. Matched-subset semantic table (Section 5), covering exactly the five frozen fixtures, for every arm with data at that subset.
5. Arm-wide operational table (Section 7's COMPARABLE-classified metrics), covering the full attempted exposure of every arm.
6. Family A split analysis (Section 8): decision-step and render-step results, never aggregated.
7. Family C provenance-separated analysis (Section 9): live and offline results individually labeled, plus the matched-subset contribution and the full-29-fixture standalone characterization.
8. Timeout analysis (Section 10): completion/timeout pairing, no semantic scoring of the one genuine timeout.
9. False-positive safety analysis (Section 11): occurrence facts only, no rate ranking, no timing-based superiority claim.
10. Representation/parser/transport analysis (Section 7's remaining COMPARABLE rows), stated even where non-discriminating (100%/0%/0% in this evidence cycle).
11. Limitations, stated explicitly: the matched-subset restriction's own narrowness (5 of 23–29 fixtures), the un-closeable truncation of Control/Family A/Family B, the descriptive-only statistical ceiling.
12. Unresolved questions, carried forward from the Unit 3-D Planning Review's own Section 15 and the Family C Offline Completion Determination's own Section 15 where still open at the time of that future report.
13. An explicit no-selection statement, restating Section 14's own firewall in the report's own words, not merely by cross-reference.

## 16. Exit criteria (Phase 16)

This Scope Lock is ready to freeze once it has passed Independent Constitutional Review with a verdict of ACCEPTED or ACCEPTED WITH QUALIFICATIONS whose qualifications are non-blocking.

A future Unit 3-D comparative-evaluation task, operating under this frozen Scope Lock, is complete once:

1. every permitted comparison in Section 15's own thirteen required elements is completed;
2. every prohibited comparison (Sections 5–7, 11–13) is confirmed avoided, not merely absent by omission;
3. exposure and censoring are disclosed prominently, in the report's own text (Sections 4, 6);
4. Family A's decision/render split is preserved throughout, with no aggregated figure anywhere (Section 8);
5. Family C's live/offline provenance separation is preserved throughout (Section 9);
6. `contentFidelity` is excluded, not inferred (Section 13);
7. descriptive-only statistical authority is preserved throughout, with no population-rate, significance, qualification-tier, or production-readiness claim anywhere (Section 12);
8. no remedy is selected, ranked, or recommended anywhere (Section 14);
9. every evidence limitation identified by this Scope Lock and by the reports it inherits from is explicitly carried forward into whatever record Unit 3-E later consults, not silently dropped;
10. that task's own Independent Constitutional Review reaches an accepting verdict.

**Unit 3-D's own exit is never conditioned on selecting, preferring, or ranking a remedy** — a comparative-evaluation report that honestly states "the evidence does not support a ranking on this metric" satisfies Unit 3-D's own purpose exactly as fully as one that identifies a clear, evidence-supported difference on a permitted metric. Declining to rank is not a deficiency; manufacturing a ranking the evidence does not support would be.

## 17. Prohibited interpretations

This Scope Lock may not be read as: performing any comparative evaluation itself (none is performed — every figure cited above is restated, not newly computed, from already-independently-verified prior governance); enlarging the matched five-fixture subset; treating Family C's offline completion as live campaign evidence; treating any COMPARABLE WITH QUALIFICATION or NOT COMPARABLE metric as available for unqualified ranking; inventing a `contentFidelity` proxy; authorizing any live model call, any new Unit 3-C campaign, or any modification to Unit 3-C artifacts; selecting, ranking, or preferring any remedy family; authorizing Unit 3-E or Unit 4 work of any kind; or freezing anything beyond the evaluation boundary and methodology stated in Sections 3–16.

## 18. Exit disposition

```text
PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```

Not frozen. No comparative evaluation, remedy ranking, remedy selection, Unit 3-E work, or Unit 4 work of any kind is authorized by this document pending that review and any further required governance.
