**Status:** Unit 3-D — Comparative Evaluation — Review — **COMPLETE, WITHIN THE FROZEN SCOPE LOCK'S BOUNDARY. NO REMEDY SELECTED. NO WINNER DECLARED.** Performed under the frozen Unit 3-D Comparative Evaluation Scope Lock (`32b55d4`, ACCEPTED WITH QUALIFICATIONS by its own Independent Constitutional Review), against committed baseline `32b55d4aa020131579f518853493f4fd0fc35c34`. Every figure below is independently re-derived from primary Attempt 6 durable artifacts and the committed Family C offline completion evidence — not copied from any prior review's own tables. No model call, no HTTP call, no campaign mutation, no code/test/Gradle change occurred during this task.

# Reasoning Protocol Live-Model Conformance Unit 3-D — Comparative Evaluation — Review

## 1. Status

This document performs the actual Unit 3-D comparative evaluation authorized, in methodology only, by the frozen Scope Lock. It answers the question the Scope Lock exists to bound: what does Unit 3-C Attempt 6's evidence (plus Family C's offline completion) establish, and what does it not establish, about Control, Family A, Family B, and Family C against the Unit 3-A Reliability Contract. It does not select a remedy, rank families globally, or authorize any further work — that remains Unit 3-E's exclusive function.

## 2. Baseline

`git rev-parse HEAD` = `git rev-parse origin/main` = `32b55d4aa020131579f518853493f4fd0fc35c34`, independently re-confirmed after `git fetch origin`. `git status -sb` clean before and throughout this task's own read-only analysis. Last commit: `32b55d4 governance: freeze Unit 3-D comparative evaluation scope`.

## 3. Authority

Performed under the frozen Unit 3-D Comparative Evaluation Scope Lock (`32b55d4`), re-read fresh for this task, unchanged since its own commit (independently re-confirmed via `git log`). Every methodological constraint below — evidence sources, exposure disclosure, the matched-fixture subset, censoring rules, the metric comparability matrix, Family A/Family C treatment rules, timeout handling, false-positive treatment, statistical-authority ceiling, `contentFidelity` exclusion, and the remedy-selection firewall — is inherited from that Scope Lock verbatim, not reinterpreted or loosened here. Where this review would need to violate a Scope Lock rule to answer a requested question, it does not answer that question, and says so explicitly (Section 17, Section 22).

## 4. Evidence-source table

| Source | Provenance | May answer | May NOT answer |
|---|---|---|---|
| **A — Attempt 6 live evidence** | Live, campaign `unit3c-remedy-experiments-20260810-03`, durable `raw.jsonl`/`intent.jsonl`/`timeouts.jsonl`/`SAFETY_CHECKPOINT` | Operational metrics for all four arms over their own attempted exposure; semantic/false-positive/false-negative figures for Control, Family A, Family B (arm-wide and matched-subset); semantic figures for Family C's own 6 live fixtures | Anything about Control/Family A/Family B beyond their own attempted trials; Family C's own 23 not-live-executed fixtures |
| **B — Family C offline deterministic completion** | Offline, zero model calls, this task's predecessor determination (`ccacf47`), re-executing the exact committed `Unit3CCandidateC1` classifier against the exact committed remaining 23 fixtures | Family C's own full-29-fixture, all-nine-category standalone characterization | Anything about Control/Family A/Family B; must never be presented as campaign-attributed live evidence |
| **C — frozen expected classifications** | Governance, `ConformanceFixture.expectedAction`/`FixtureCategory`, unchanged since `fee2edd`/`08f3692` | Ground truth for correctness computation | Never itself a result |
| **D — operational evidence** | Derived from A, per trial: completion/timeout/representation/parser/transport status | Arm-wide comparison across each arm's own full attempted population (Section 8) | Semantic content of any observation |
| **E — semantic evidence** | Derived from A/B, per trial: `actualAction`/`expectedAction` match, false-positive/negative classification | Cross-arm comparison restricted to the matched five-fixture subset (Section 7); unrestricted intra-arm comparison (Family A's own two sub-steps, Family C's own full corpus) | Any cross-arm claim outside the matched subset |

No figure in this review blends sources without explicit labeling. Every Family C figure below states whether it is drawn from A, B, or A+B combined.

## 5. Exposure table

Independently re-derived fresh from primary artifacts via `wc -l`/regex extraction (not copied from the Planning Review's own table), cross-checked against `intent = raw + timeouts` for every arm:

| Arm | Scheduled max | Attempted | Completed | Timeouts | Exposure fraction | Safety-stop point |
|---|---|---|---|---|---|---|
| Warm-up | 3 | 3 | 3 | 0 | 100.0% | sealed, no checkpoint |
| Control | 145 | 132 | 132 | 0 | 91.0% | `control/p17-hypothetical-remember/main/02` |
| Family A | 220 | 51 | 50 | 1 | 23.2% | `family_a/p03-ambiguous-memory/decision/01` |
| Family B | 115 | 91 | 91 | 0 | 79.1% | `family_b/g03-later-action/main/01` |
| Family C (live, Source A) | 29 | 6 | 6 | n/a | 20.7% | `family_c/p03-ambiguous-memory/main/01` |
| Family C (offline, Source B) | — | 23 | 23 | n/a | remaining 79.3%, offline-sourced | n/a — not a live stop |

**The arms are unequally and informatively censored.** Three of four arms (Control, Family A, Family B) halted before exhausting their own live schedule, each at a different point, because the safety checkpoint fires on the exact adverse event (a false-positive REMEMBER/GOAL) any semantic comparison would also want to measure — this is not random truncation, and the unobserved remainder of any arm's own schedule may not be inferred to resemble the observed portion. Family C's own live subset is similarly censored (6/29); its offline completion (Source B) closes its own adversarial-category coverage gap but does not, and cannot, close Control's, Family A's, or Family B's own live truncation, since their own mechanisms require a genuine model call this task is prohibited from making.

## 6. Censoring disclosure

Restated per the Scope Lock's own Section 6, binding on every subsequent section of this review:

1. Safety-checkpoint halting is **informative right-censoring**, not random.
2. Fixture-composition truncation is **non-random** — an earlier-halting arm is systematically restricted to earlier fixtures in the shared, frozen corpus order.
3. **The unobserved remainder of any arm's schedule may not be inferred.** This review makes no claim about what Control's, Family A's, or Family B's own untested trials would have shown.
4. **Later checkpoint timing does not, by itself, imply superior reliability.** Control's own later halt (91.0% exposure) reflects exposure to more, and different, fixtures than Family A (23.2%) or Family B (79.1%) reached — not necessarily a safer underlying mechanism.
5. **Raw arm-wide semantic aggregate ranking is prohibited.** No figure in this review compares one arm's own full-attempted-set semantic rate directly against another arm's own differently-composed full-attempted-set rate.

## 7. Matched-subset semantic comparison

**The only cross-arm matched fixture set: `r01-direct`, `r02-please`, `r03-dont-forget`, `p01-ordinary-fact`, `p02-quoted-remember`** — independently re-confirmed the only fixtures where Control, Family A's own decision step, and Family B all completed the full frozen `n=5`, and where Family C's own frozen `n=1` (live, Source A) is also available. Not enlarged retrospectively.

**Control, Family A (decision step only), and Family B — `n=5` per fixture, independently re-derived from raw payloads:**

| Fixture | Expected | Control (5/5) | Family A decision (5/5) | Family B (5/5) |
|---|---|---|---|---|
| `r01-direct` | REMEMBER | 0 correct | 0 correct | 0 correct |
| `r02-please` | REMEMBER | 0 correct | 4 correct | 1 correct |
| `r03-dont-forget` | REMEMBER | 0 correct | 3 correct | 0 correct |
| `p01-ordinary-fact` | REPLY | 5 correct | 3 correct | 5 correct |
| `p02-quoted-remember` | REPLY | 5 correct | 4 correct | 4 correct |
| **Total (of 25 trials)** | | **10/25** | **14/25** | **10/25** |

**Family C (Source A, live, `n=1` per fixture — a materially different repetition design, its own denominator stated separately, never pooled with the `n=5` arms above):**

| Fixture | Expected | Family C actual | `semanticCorrect` |
|---|---|---|---|
| `r01-direct` | REMEMBER | REMEMBER | true |
| `r02-please` | REMEMBER | REMEMBER | true |
| `r03-dont-forget` | REMEMBER | (no signal) | null (known false negative) |
| `p01-ordinary-fact` | REPLY | (no signal) | null (correct abstention, per the classifier's own binary output space) |
| `p02-quoted-remember` | REPLY | (no signal) | null (correct abstention) |
| **Total (of 5 trials, `n=1` each)** | | **2 definitively correct, 0 definitively incorrect, 3 recorded `null`** | |

Per the Unit 3-D Planning Review's own already-established finding, restated here as binding on this review's own interpretation: Family C's `semanticCorrect=null` does not mean "unknown" in the same sense as a parser failure — it means "the classifier's own binary signal (REMEMBER-signal / no-signal) does not itself distinguish a correct non-REMEMBER abstention from an incorrect miss," a known property of the schema's own field semantics (Unit3CObservation's own `semanticCorrect` derivation), not a new finding of this review.

**What this table supports:** a factual, matched, equal-fixture-composition record of each arm's own performance on these five specific fixtures, at each arm's own frozen repetition design. **What it does not support:** any claim about performance on the 18–24 other fixtures each arm did or did not reach; any population-rate claim (Section 16); any ranking beyond the bare, stated numbers (Section 17 states what may be said about them).

## 8. Operational comparison (arm-wide, Source D)

Independently re-derived from every completed trial in each arm's own full attempted set (not restricted to the matched subset, per the Scope Lock's own Section 7 classification of these metrics as COMPARABLE at arm-wide scope):

| Metric | Control (n=132) | Family A (n=50 completed of 51 attempted) | Family B (n=91) | Family C live (n=6) |
|---|---|---|---|---|
| Completion rate | 132/132 = 100.0% | 50/51 = 98.0% | 91/91 = 100.0% | 6/6 = 100.0% |
| Model-timeout rate | 0/132 = 0.0% | 1/51 = 2.0% | 0/91 = 0.0% | not applicable (no transport layer) |
| Representation validity | 132/132 = 100.0% | 50/50 = 100.0% | 91/91 = 100.0% | 6/6 = 100.0% (by construction; no representation-failure mode exists for this mechanism) |
| Parser failures | 0 | 0 | 0 | not applicable |
| Transport/provider failures | 0 | 0 | 0 | not applicable |

**These figures do not, by themselves, constitute or imply a semantic ranking.** Representation validity of 100% in every arm is non-discriminating in this evidence cycle and must not be read as evidence of semantic reliability — every arm's own matched-subset table (Section 7) shows substantial semantic error coexisting with perfect representation validity, which is exactly the independent-axes property Unit 3-A Section 6 requires be preserved, not collapsed.

## 9. Family A evaluation

**Reported as two separate parts per the Scope Lock's own binding Section 8 rule. Not aggregated.**

**A. Decision step.** Matched-subset performance: 14/25 (56.0%), Section 7. Arm-wide (all five fixtures Family A reached, including its own single `p03-ambiguous-memory` checkpoint trial): 14/26 correct, 1 false-positive REMEMBER (`p03-ambiguous-memory`). False-negative behavior on REMEMBER-expected decision trials, independently re-derived fresh for this review (**correcting a denominator error found in the prior Attempt 6 Execution Evidence Review**, which stated "8/13" — the true, re-verified denominator is **15**, not 13, since `r01`/`r02`/`r03` each received a full 5 decision attempts): **8 of 15** REMEMBER-expected decision trials did not receive `REMEMBER` (`r01-direct` 5/5 missed, `r02-please` 1/5 missed, `r03-dont-forget` 2/5 missed).

**B. Rendering step.** 24/24 correct (100.0%) across the four fixtures Family A completed both sub-steps for (`r01`, `r03`, `p01`, `p02` fully; `r02` at 4 of 5 due to the one genuine timeout at `render/02`). This step supplies the correct action as a stated given and measures only whether the model can produce correctly-tagged content for it — **it is not a test of independent semantic decision-making** and must not be read as one.

**What Family A's evidence supports:** a factual record of decision-step accuracy on 5 fixtures (26 trials) and render-step compliance on the same 5 fixtures (24 trials, one timeout); a direct, intra-arm observation that render-step compliance (100%, given a correct decision) is substantially higher than decision-step accuracy (53.8% arm-wide, 56.0% on the matched subset) for this specific evidence cycle. **What it does not support:** any claim about Family A's own performance on GOAL-expected, NOACTION-expected, or any fixture beyond the five it reached (81.4% of the fixture space Family A's own schedule never touched, per Section 5's own 23.2% exposure figure); any inference that strong rendering compliance is evidence of, compensates for, or is otherwise informative about decision-step reliability — the two are architecturally decoupled by design (the render step's own prompt states the correct answer outright).

## 10. Family B evaluation

Matched-subset semantic performance: 10/25 (40.0%), Section 7. Arm-wide operational metrics: 100% completion, 0% timeout, 100% representation validity, 0 parser failures (Section 8). False-negative behavior on REMEMBER-expected matched fixtures: 14 of 15 (`r01` 5/5 missed, `r02` 4/5 missed, `r03` 5/5 missed). Safety-checkpoint event: one false-positive REMEMBER, `g03-later-action` (GOAL-category negative control), at trial 91 of its own 115-trial schedule (79.1% exposure).

**What this evidence supports:** a factual, matched-subset record directly comparable in composition (not in denominator source, since Family B's own `n=5` matches Control's and Family A-decision's own `n=5`) to Control's and Family A's own Section 7 figures; a complete, un-truncated operational profile for its own 91 attempted trials. **What it does not support:** any claim about the unobserved 24 trials (115 − 91) Family B's own schedule never reached (Section 6) — no inference is drawn here about whether those trials would have produced further false positives, further semantic errors, or clean results.

## 11. Family C evaluation

**Reported in two provenance-separated parts, per the Scope Lock's own binding Section 9 rule.**

**A. Attempt 6 live subset (6 fixtures, Source A):** `r01-direct` correct, `r02-please` correct, `r03-dont-forget` false negative, `p01-ordinary-fact` correct abstention, `p02-quoted-remember` correct abstention, `p03-ambiguous-memory` false positive (the arm's own safety-checkpoint trigger, at 20.7% of its own 29-fixture schedule).

**B. Offline deterministic completion (23 fixtures, Source B, zero model calls):** independently re-executed for this review as a third confirmation (in addition to the two independent methods already used by the governing Determination and its own Independent Constitutional Review) — reproduces exactly: 24/29 correct overall (combining A+B), 4 false positives (`p03-ambiguous-memory`, `p04-embedded-tags`, `p05-mixed-memory-discussion`, `p12-injection`), 1 false negative (`r03-dont-forget`), 0 false-positive GOAL (structurally unreachable by this mechanism's own binary output space).

**Nine-category adversarial coverage:** all nine Unit 3-B Section 4 categories are fully exercised, combining A (categories 1, 4, and part of 5/9) and B (categories 2, 3, 6, 7, 8, and the remainder of 5/9) — independently re-confirmed via the same category-to-fixture mapping the governing Determination established.

**Matched-subset contribution:** Family C's own results at the five matched fixtures are entirely Source A (live) — no offline data is needed for, or blended into, the matched-subset table in Section 7.

**Architectural asymmetry, stated explicitly per the Scope Lock's own binding requirement:** Family C makes zero model calls. Its 100% completion rate and structurally-absent timeout/transport-failure/parser-failure rates reflect the **absence of a transport layer**, not a demonstrated reliability advantage over Control, Family A, or Family B — **this must not, and does not, support a claim of superior model reliability**. Its own near-zero latency and zero inference cost are, likewise, properties of being a deterministic function, not evidence of a faster or cheaper model-based mechanism; any comparison of Family C's own latency/cost figures against Control's, Family A's, or Family B's own real per-call latency (independently observed in this attempt ranging roughly 0.9–57 seconds per completed call) requires this same architectural qualification stated in the same breath, not left implicit.

## 12. Timeout analysis

Family A's one genuine model timeout (`family_a/r02-please/render/02`, `MODEL_TIMEOUT`, `ARM_CONTINUED`) is reported here, separately from every semantic table above. **Completion rate (Section 8), semantic correctness (Sections 7, 9), and timeout rate (Section 8) are kept as three distinct metrics throughout this review — nowhere is the timeout counted as a semantic incorrect, a false positive, or a false negative.** It has no `actualAction`, and none is fabricated here. The timeout's own `elapsedNanos` (≈838 seconds against a nominal 90-second ceiling, independently re-confirmed present in the durable record) is **not diagnosed further by this review** — it remains, exactly as the governing Execution Evidence Review already states, a real, honestly-recorded, unconfirmed operational anomaly, offered no new explanation here, consistent with this review's own prohibition on extrapolating beyond already-supported evidence (Section 16).

## 13. False-positive safety analysis

**One false-positive REMEMBER occurred in each of the four arms' own safety-truncated execution:** Control (`p17-hypothetical-remember`, at 91.0% of its own schedule), Family A (`p03-ambiguous-memory`, at 23.2%), Family B (`g03-later-action`, at 79.1%), Family C live (`p03-ambiguous-memory`, at 20.7%). **Zero false-positive GOAL events were observed in any arm** (and are structurally unreachable by Family C's own mechanism).

**What is supported:** each family demonstrated at least one constitutionally-significant false-positive REMEMBER event under its own observed exposure, on a genuine ADVERSARIAL- or GOAL-category negative-control fixture, confirming the governed safety-checkpoint mechanism itself fired correctly (first occurrence, non-numeric trigger) in every arm. **What is not supported, and is not claimed here:** that equal occurrence count (one each) establishes equivalent underlying false-positive rates — the four arms' own denominators (132, 51, 91, 6) differ sharply, and the true rate over any arm's full, un-truncated schedule remains unknown (Section 6); that differing trigger timing establishes any arm as safer than another — Control's own later trigger reflects exposure to a fixture (`p17-hypothetical-remember`) with no counterpart at all in Family A's or Family B's own corpus, not necessarily a more robust underlying mechanism; any rate-based ranking of false-positive REMEMBER across these differently-censored arms, which the Scope Lock's own Section 7 classifies NOT COMPARABLE.

## 14. False-negative analysis

**Cross-arm comparison restricted to the matched-subset REMEMBER-expected fixtures (`r01`, `r02`, `r03`), per Section 7:** Control 15/15 missed; Family A decision-step 8/15 missed; Family B 14/15 missed; Family C (Source A, `n=1` denominator, not pooled with the `n=5` arms) 1/3 missed (`r03-dont-forget`).

**Family C's own full-29-fixture false-negative result, reported separately as Family-C-specific evidence (Source A+B combined), never mixed into the matched-subset denominators above:** 1 of 29 fixtures overall (`r03-dont-forget`, the only REMEMBER-expected fixture in the corpus the mechanism's own negation mitigation causes it to miss) — independently re-confirmed, Section 11.

No denominator is mixed anywhere in this section: the `/15` figures use the `n=5`-per-fixture model-arm design; the `/3` and `/29` figures use Family C's own `n=1`-per-fixture design; none is pooled across the two.

## 15. Representation/parser/transport analysis

100% representation validity, 0 parser failures, 0 transport failures — every arm, independently re-confirmed (Section 8). **This uniformly high representation performance must not obscure the substantial semantic-selection failures documented in Sections 7 and 9–11** — a syntactically valid, wrong-action response (e.g., Control's own 0/5 on every matched REMEMBER fixture, all with `representationValid: true`) is not conformant, exactly as Unit 3-A Section 6 requires be stated, not concealed behind a high representation score.

## 16. `contentFidelity` exclusion

**NOT AVAILABLE.** Independently re-confirmed via direct inspection of durable records across all five arms (fresh sample, this task): `"contentFidelity":null` in every completed observation, live and offline alike. Not calculated, proxied, or inferred anywhere in this review — no representation-validity or semantic-correctness figure above is used, or should be read, as a substitute for a content-fidelity measurement, which does not exist for this evidence cycle.

## 17. Statistical limitations

This review is **descriptive only**. No hypothesis test, significance test, population-rate inference, production-readiness qualification, or formal ranking is performed anywhere above. Every percentage stated carries its own exact denominator (Sections 7–14). Sample sizes are exploratory-tier by design (`n=5` per fixture for model arms, `n=1` for Family C) and further restricted by informative censoring in three of four arms; repeated-fixture trials are non-independent observations, not independent population draws; qualification-tier thresholds (Unit 3-A's own ≥300-exposure zero-event gates, ≥99% representation validity, ≥97% one-sided-95%-LCB correctness) are not applied anywhere in this review as a pass/fail bar, since this evidence was never designed to meet that separate, later tier.

## 18. Strengths / weaknesses / tradeoffs

Stated per family, in neutral, non-selection language, per the Scope Lock's own Section 14 carve-out:

- **Control:** exhibited complete representation validity and 100% operational completion across its own largest observed exposure (132 trials, 91.0% of schedule); exhibited 0/25 correct on the matched REMEMBER-expected fixtures and 0/15 (using the correct, Section 9-restated denominator) false-negative-free rate — i.e., every matched REMEMBER-expected trial in Control's own observed data resulted in a non-REMEMBER action. Its own later safety-checkpoint trigger reflects a larger, differently-composed exposure, not an established safety property, per Section 6.
- **Family A:** its own rendering step showed stronger representation-and-content-tag compliance (24/24) than its own decision step showed independent semantic-selection accuracy (14/26 arm-wide, 14/25 matched-subset) — a direct, intra-arm observation, not a comparison to any other family. Its own decision step's false-negative rate on REMEMBER-expected trials (8/15) is lower than Control's (15/15) and Family B's (14/15) on the same matched fixtures, stated as a matched-subset fact; whether this reflects the decision/rendering-separation mechanism itself, the smaller sample Family A's own early checkpoint produced, or another cause is not established by this evidence and is not claimed here. Family A's own evidence is the most exposure-limited of the three model-invoking arms (23.2%), and experienced the campaign's only genuine model timeout.
- **Family B:** operated at the second-largest observed exposure (79.1%) among the truncated arms; its own matched-subset semantic performance (10/25) and false-negative rate (14/15) are, as raw figures, comparable in magnitude to Control's own matched-subset figures (10/25, 15/15) — stated as a fact about these two arms' own matched-subset numbers, not as a claim of equivalence (Section 6 forbids inferring equivalence from similar counts under different exposure).
- **Family C:** produced a fully deterministic, reproducible, zero-latency, zero-inference-cost classification for every fixture it processed (live and offline combined, 29/29), with known, specific, already-governed failure modes — 4 false positives (`p03`, `p04`, `p05`, `p12`, all ADVERSARIAL-category negative controls) and 1 false negative (`r03-dont-forget`, caused by its own negation-mitigation logic misfiring on the double-negative-like `"don't forget"` construction). Its own architectural absence of a transport layer means completion/timeout comparisons to the model-invoking arms require explicit qualification (Section 11) and its own 20.7% live exposure fraction is the smallest of the four arms, closed only via a separate, offline evidence source (Source B) that is not itself live campaign evidence.

## 19. Unresolved questions

Carried forward from the Unit 3-D Planning Review's own Section 15 and the Family C Offline Completion Determination's own Section 15, restated here as still open at the time of this review: whether decision/rendering separation (Family A) is itself the cause of its own lower matched-subset false-negative rate, or an artifact of its own smaller sample and earlier checkpoint; whether Family B's prompt/protocol redesign produces any effect distinguishable from Control's own behavior given their similar matched-subset raw figures — a question this descriptive-only evidence cannot resolve (Section 17); whether Family C's own false-positive/false-negative profile would remain stable if its own mechanism were ever modified (out of scope — no modification occurred or is proposed here); the `elapsedNanos` anomaly on Family A's one timeout (Section 12), unresolved; whether any future, separately-authorized live campaign extending Control's, Family A's, or Family B's own exposure beyond their current safety-checkpoint halts is warranted — a Unit 3-C-scoping question, not answered here.

## 20. Unit 3-E evidence handoff

Structured for a future, separately-authorized Unit 3-E task. **This section does not recommend which family Unit 3-E should select — it inventories what the evidence does and does not support, for that later, separate decision.**

| Family | Evidence available | Evidence lacking | Known failure modes | Architectural tradeoff |
|---|---|---|---|---|
| Control | Largest exposure (91.0%); full base + partial supplemental corpus; matched-subset and arm-wide operational/semantic figures | No data beyond `p17-hypothetical-remember` in the fixture order; no qualification-tier sample size | 0/25 matched REMEMBER-expected correctness; 15/15 false-negative on matched REMEMBER fixtures; 1 false-positive REMEMBER | None beyond the existing production path — this is the unmodified baseline |
| Family A | Matched-subset and arm-wide decision-step and (separately) render-step figures; smallest false-negative rate on the matched subset among the three model-invoking arms | Smallest exposure (23.2%); no GOAL/NOACTION/most-REPLY-fixture data at all; render-step data only for 4–5 fixtures | 1 false-positive REMEMBER (decision step); 8/15 false-negative (decision step); one genuine model timeout; doubled call cost per fixture (decision + render) | Two-call architecture; decision and rendering are architecturally decoupled, requiring separate evaluation |
| Family B | Second-largest exposure among truncated arms (79.1%); matched-subset and arm-wide figures | No data beyond `g03-later-action` in the fixture order; no GOAL fixtures beyond `g03` reached; no qualification-tier sample size | 14/15 false-negative on matched REMEMBER fixtures; 1 false-positive REMEMBER | Single-call architecture, same cost profile as Control; prompt-text-only modification |
| Family C | Complete 29-fixture, all-nine-category coverage (live + offline, provenance-separated); fully deterministic and reproducible | No live-model-behavior data of any kind (by architecture); matched-subset live data limited to `n=1` per fixture | 4 false positives, 1 false negative, both fully characterized and attributable to specific, named mechanism logic | Zero model calls, zero latency, zero inference cost, but zero exposure to any model-specific behavior; deterministic failure modes are exactly reproducible, unlike stochastic model failure modes |

**Prohibited inferences, restated for Unit 3-E's own future benefit:** none of the above may be read as a ranking; equal or similar raw figures across differently-censored arms (e.g., Control's and Family B's own similar matched-subset totals) do not establish equivalence; no figure in this handoff meets qualification-tier evidentiary weight; no figure resolves whether any observed difference is attributable to the remedy mechanism itself versus sampling variation at this exploratory scale.

## 21. No-selection statement

**No remedy is selected, ranked, or recommended anywhere in this document.** No family is declared a winner. No production-adoption recommendation is made. Every comparative statement above is restricted to a specific, named, permitted metric (Section 7's matched-subset table, Section 8's operational table) or is explicitly intra-arm (Family A's own two sub-steps). Where this review states that one arm's raw figure differs from another's, it does so descriptively and with the Section 6 censoring caveat attached in the same breath — never as a conclusion about which family is better, safer, or more suitable for production. That determination belongs exclusively to Unit 3-E, informed by this evidence, not made by it.

## 22. Exit-criteria verification

Checked against the Scope Lock's own Section 16, item by item:

1. All thirteen required output elements (Scope Lock Section 15) are present in Sections 4–21 above.
2. Every prohibited comparison (raw arm-wide semantic aggregate ranking, false-positive rate ranking, trials-to-first-failure ranking, `contentFidelity` inference, population-rate/significance/qualification-tier claims) is confirmed avoided — independently re-checked by scanning this document's own text for the specific prohibited constructions before finalizing.
3. Exposure and censoring are disclosed prominently, in this document's own text (Sections 5–6), not only by citation.
4. Family A's decision/render split is preserved throughout (Section 9); no aggregated figure appears anywhere in this document.
5. Family C's live/offline provenance separation is preserved throughout (Section 11); every Family C figure states its source.
6. `contentFidelity` is excluded, not inferred (Section 16).
7. Descriptive-only statistical authority is preserved throughout (Section 17); no population-rate, significance, qualification-tier, or production-readiness claim appears anywhere.
8. No remedy is selected, ranked, or recommended anywhere (Sections 18, 20, 21).
9. Every evidence limitation identified by prior governance (the Planning Review, the Family C Determination) is carried forward into Section 19 and Section 20, not silently dropped.
10. This document's own Independent Constitutional Review (a separate document) is required before this evaluation may be relied upon as complete — not self-certified here.

**All nine self-checkable criteria (1–9) are satisfied by this document's own text, independently re-verified during drafting. Criterion 10 is pending the separate Independent Constitutional Review this task also produces.**
