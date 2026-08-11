**Status:** Family A Decision-Step Evidence Extension — Planning Review — **PLANNING ONLY. NO CAMPAIGN AUTHORIZED. NO REMEDY SELECTED. NO SCOPE LOCK DRAFTED.** Against committed baseline `e40c5a57b0eacfb5589386f6a049682712270681`. Determines whether a bounded live-evidence extension targeting Family A's decision step is capable of resolving a material uncertainty that prevented Unit 3-E from selecting a remedy, freshly and without assuming the Post-Selection Disposition Planning Review's own prior recommendation survives closer scrutiny.

# Family A Decision-Step Evidence Extension — Planning Review

## 1. Baseline

`git rev-parse HEAD` = `git rev-parse origin/main` = `e40c5a57b0eacfb5589386f6a049682712270681`, independently re-confirmed after `git fetch origin`. `git status --short`/`git status -sb` clean. Last commit: `e40c5a5 governance: plan post-selection disposition`.

## 2. Authority read fresh

Read fresh, in full, for this task: the original top-level Reasoning Protocol Live-Model Conformance and Structured-Output Reliability Planning Review (`b34f8d0`); the Unit 3-A Reliability Contract Definition Scope Lock (`ab27f18`); the Unit 3-B Remedy Experiment Scoping Scope Lock (`55af571`); the Unit 3-C Scope Lock, Implementation/Execution Plan, and Timeout + Durability Amendments; the Attempt 6 Execution Evidence Review and its Independent Constitutional Review (`b5c0749`); the Unit 3-D Comparative Evaluation Planning Review (`239c6c1`), Scope Lock (`32b55d4`), Review and Independent Constitutional Review (`2e468ed`); the Unit 3-E Remedy Selection Planning Review and Planning Independent Review (`e4d691a`), Scope Lock and its Independent Constitutional Review (`cf22a9a`), and Remedy Selection Review and Independent Constitutional Review (`23b500f`); the Post-Selection Disposition Planning Review and its Independent Review (`e40c5a5`) — this task's own immediate predecessor and the source of the specific recommendation this document now re-examines from first principles; and, directly from source, `Unit3CCampaignDefinition`, `FamilyADecisionPromptBuilder`, `FamilyARenderingPromptBuilder`, and the frozen 23-fixture base corpus in `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` (unchanged since `e06f85c`, independently re-confirmed via `git log`).

**This task does not treat the Post-Selection Disposition Planning Review's own recommendation as a foregone conclusion.** That review recommended, as its own principal path, "targeted additional-evidence governance directed specifically at Family A's own decision-step semantic accuracy." This document independently re-derives, from primary evidence and fresh counterfactual reasoning, whether that recommendation withstands closer scrutiny — and finds, in Section 8 below, that it requires material qualification.

## 3. Family A evidence, independently reconstructed fresh

Independently re-derived from `/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810-03/family-a/` (confirmed unchanged, read-only, 26 files across the whole campaign) via a fresh Python decode pass, cross-checked against the frozen `Unit3CBaseCorpus` definition read directly from source:

**Exposure:** 51 attempted (intent) of 220 scheduled (23.2%); 50 completed; 1 genuine `MODEL_TIMEOUT` (`family_a/r02-please/render/02`, `elapsedNanos=838234061456`, `ARM_CONTINUED`). Safety-checkpoint trigger: `family_a/p03-ambiguous-memory/decision/01` (adversarial-category false positive) — the literal last entry in the arm's own `raw.jsonl`, confirming no trial was attempted after the halt.

**Decision-step observations (n=26):** expected-action distribution `{REMEMBER: 15, REPLY: 11}`; actual-action distribution `{REPLY: 14, NOACTION: 4, REMEMBER: 8}`; `semanticCorrect` true=14, false=12; representation validity 26/26 (100%); parser failures 0.

**Render-step observations (n=24):** expected-action distribution `{REMEMBER: 14, REPLY: 10}`; actual-action distribution identical to expected in every case; `semanticCorrect` true=24, false=0; representation validity 24/24 (100%).

**Matched-subset observations** (the five fixtures — `r01-direct`, `r02-please`, `r03-dont-forget`, `p01-ordinary-fact`, `p02-quoted-remember` — at full `n=5` decision-step repetition, independently re-confirmed the only fixtures Family A's own decision step reached at full frozen repetition): 25 trials, 14 correct.

**False-negative REMEMBER (decision step, matched subset, denominator independently re-confirmed = 15, i.e. `r01`+`r02`+`r03` × 5 each):** 8/15 — `r01-direct` 5/5 missed, `r02-please` 1/5 missed, `r03-dont-forget` 2/5 missed.

**False-positive REMEMBER (decision step):** 1, `p03-ambiguous-memory` (the arm's own safety-checkpoint trigger — its only decision-step attempt).

**False-positive GOAL (decision step):** 0 — but see Section 4 below on why this zero is not informative.

**Timeout observations:** 1, render step only (`r02-please/render/02`), correctly excluded from every decision-step and render-step semantic figure above.

**Representation validity / parser failures / transport failures:** 100% / 0 / 0, both sub-steps, no exceptions.

**Fixtures never reached, independently enumerated fresh against the frozen 23-fixture base corpus:** decision step touched exactly 6 of 23 fixtures (`r01`, `r02`, `r03`, `p01`, `p02`, `p03`); **17 of 23 (73.9%) were never reached at the decision step at all** — independently itemized: `p04-embedded-tags`, `p05-mixed-memory-discussion`, `p06-greeting`, `p07-factual-question`, `p08-explanation`, `p09-long-distractor`, `p10-acknowledgement`, `p11-short-casual`, `p12-injection`, `p13-reply-v-goal`, `g01-multistep`, `g02-tool`, `g03-later-action`, `g04-planning`, `g05-mixed-work`, `n01-heartbeat`, `n02-unicode-whitespace`. **Every single GOAL fixture (5/5) and every single NOACTION fixture (2/2) was never reached.** Of the ten REPLY/ADVERSARIAL fixtures beyond the two touched (`p01`, `p02`), all ten remain untested. Render step reached only 5 of 21 render-eligible fixtures.

**Decision-step and render-step evidence are kept permanently separate throughout this document; render-step's 24/24 result is never used to offset, average against, or imply anything about decision-step's 14/26.**

## 4. The actual unresolved uncertainty, stated in testable terms

Independently re-derived, not assumed from the Post-Selection Disposition Review's own framing. Two genuinely distinct propositions are conflated if stated loosely as "we need more Family A data" — this document separates them:

**Proposition 1 (the one the prior Planning Review actually targeted): "Is Family A's decision-step REMEMBER-recognition rate on direct, explicit instructions (`r01`/`r02`/`r03`) — observed at 7/15 correct (46.7%; equivalently 8/15 missed) — a genuine, reproducible property of the decision/rendering-separation architecture, or an artifact of a 15-trial sample?"** This is testable: repeating `r01`/`r02`/`r03` decision-step trials at a larger `n` would narrow the confidence interval on Family A's own true rate on this specific fixture set.

**Proposition 2 (independently identified by this document, not previously isolated as its own question): "Does Family A's decision step exhibit acceptable false-positive REMEMBER/GOAL behavior, and does it perform adequately on GOAL and NOACTION discrimination, given that its own current evidence covers exactly one adversarial fixture (`p03`, which *was* a false positive) and zero GOAL or NOACTION fixtures of any kind?"** This is a **separate, independently testable, and currently entirely unaddressed proposition** — not a restatement of Proposition 1, and not resolved by any amount of additional `r01`/`r02`/`r03` repetition.

**Determination: the uncertainty actually before Unit 3-E was closer to Proposition 1 than Proposition 2** — the Unit 3-E Remedy Selection Review's own rationale (Section 16 there) cited Family A's matched-subset false-negative rate specifically, and the Post-Selection Disposition Review's own recommendation (Section 14 there) followed that same framing. **This document finds that framing incomplete.** Unit 3-A Section 7 gives false-positive REMEMBER/GOAL *primary*, zero-tolerance-at-qualification-tier constitutional weight — textually and substantively a *higher* priority than REMEMBER-recognition rate. Family A's own single adversarial-fixture exposure, which was itself a false positive, is **the thinnest and most alarming evidence of any axis for any candidate** — one trial, one failure, zero corroborating or disconfirming data. Proposition 2 is at least as material to any future Unit 3-E reconsideration as Proposition 1, and is independently judged **more materially unresolved**, given its near-total absence of any evidence at all (1 adversarial trial; 0 GOAL trials; 0 NOACTION trials) compared to Proposition 1's own thin-but-nonzero 15-trial base.

**Both propositions are material to the Unit 3-E no-selection decision** — Proposition 1 because it was the specific figure the decision's own rationale cited; Proposition 2 because it bears directly on the same false-positive-safety concern the Unit 3-E Scope Lock treats as a "serious risk factor" for every candidate, and because Family A's own evidence on this axis is even thinner than the REMEMBER-recognition evidence that motivated the original recommendation.

## 5. Counterfactual value of additional evidence

**Mandatory analysis, performed before any experiment design, exactly as required.**

### 5.1 A materially stronger result

Suppose an extension yields, e.g., Family A decision-step REMEMBER-recognition rising to a well-sampled ~75–85% on `r01`/`r02`/`r03` (addressing Proposition 1), **and** zero further false positives across a reasonably broadened adversarial/GOAL/NOACTION exposure (addressing Proposition 2).

- **Could this make Family A selectable under the existing frozen Unit 3-E criteria?** Not automatically — the Unit 3-E Scope Lock's own Section 11 explicitly prohibits any numeric threshold from mechanically triggering selection ("no automatic selection triggered by a single metric crossing an undisclosed or ad hoc threshold"). A future Unit 3-E task would still need to reach its own qualitative, narrative determination.
- **But it would represent genuinely changed evidence**, independently satisfying the Unit 3-E Scope Lock's own Section 4 anti-bar-lowering requirement ("what in the evidence itself... changed"), and would materially weaken the specific dispositive factor (poor, thin semantic performance) that drove the original no-selection outcome. **A future Unit 3-E task could plausibly, though not certainly, reach a different disposition on this evidence.**
- **This counterfactual requires improvement on *both* propositions to be genuinely persuasive** — a result strong on Proposition 1 alone (REMEMBER-recognition) but silent on Proposition 2 (false-positive/GOAL/NOACTION) would leave exactly the gap Section 4 identifies, and a future Unit 3-E task applying the same rigor as the current one would likely find the evidence still incomplete on the axis Unit 3-A itself weights more heavily.

### 5.2 A result approximately consistent with existing evidence

Suppose the extension confirms Family A's own ~50% decision-step REMEMBER-recognition rate at a larger, more statistically credible sample, and confirms (or slightly worsens) its false-positive profile on a broadened adversarial set.

- **Would this make Family A selectable?** No.
- **Would this eliminate Family A?** Not formally — the Unit 3-E Scope Lock explicitly declines to invent an elimination threshold, and this document does not invent one either.
- **Would NO REMEDY SELECTED remain the disposition?** Yes, with materially higher confidence that this is Family A's genuine ceiling rather than a small-sample artifact — a real, if modest, informational gain, but not a decision-changing one.
- **Net effect:** reduces uncertainty without changing the decision — precisely the outcome the governing task's own Phase 4 instructs to weigh against the cost of obtaining it.

### 5.3 A materially weaker result

Suppose the extension shows Family A's own true decision-step REMEMBER-recognition rate is substantially below the current 46.7% figure (i.e., the current small sample was itself favorably lucky), and/or reveals additional false positives on the previously-untested adversarial/GOAL/NOACTION fixtures.

- **Would this make Family A selectable?** No.
- **Would this eliminate Family A?** Not formally, for the same reason as 5.2, but it would extinguish the specific evidentiary basis (the numerically-best-among-model-arms false-negative rate) that made Family A the Post-Selection Disposition Review's own principal candidate for further investment in the first place.
- **Would NO REMEDY SELECTED remain the disposition?** Yes, and more clearly so.

### 5.4 Synthesis

**Only the "materially stronger, on both propositions simultaneously" counterfactual (5.1) has a plausible, non-trivial chance of altering the Unit 3-E disposition — and even then, not with certainty, only with the possibility of prompting a future reconsideration.** Counterfactuals 5.2 and 5.3 both leave the disposition unchanged, differing only in the confidence with which it is held. **No realistically obtainable Family A result under a *narrowly-scoped, Proposition-1-only* extension (i.e., more `r01`/`r02`/`r03` repetitions alone, the design the Post-Selection Disposition Review's own recommendation actually specified) can, by itself, satisfy the "materially stronger" counterfactual's own precondition of also resolving Proposition 2** — because a Proposition-1-only design generates no data on Proposition 2 at all. This is the central finding of this Planning Review, developed further in Sections 7–8.

## 6. Bounded experiment design (presented conditionally, for completeness — see Section 8 for why this is not currently recommended)

Presented as an illustrative sketch of what a *genuinely* decision-relevant extension would require, not as a proposal this document endorses executing now.

- **Exact question:** both Proposition 1 (REMEMBER-recognition reliability) and Proposition 2 (false-positive/GOAL/NOACTION behavior) — a Proposition-1-only design is independently found insufficient (Section 5.4).
- **Exact component under test:** Family A's decision step only — the render step's own 24/24 result is not in question and requires no further evidence; extending it would add cost with no decision value.
- **Fixture population:** the existing frozen 23-fixture base corpus — no new fixture text required. Minimum coverage to inform both propositions: `r01`/`r02`/`r03` (REMEMBER, for Proposition 1, at meaningfully increased repetition); the remaining, never-reached adversarial/distractor fixtures `p04`, `p05`, `p09`, `p12` (REPLY/ADVERSARIAL, for Proposition 2's false-positive axis); at least one representative from each of the five GOAL fixtures and both NOACTION fixtures (for Proposition 2's action-discrimination completeness, currently at zero coverage).
- **Existing fixtures suffice; no new fixtures required.**
- **Illustrative repetition count:** `r01`/`r02`/`r03` extended from 5 to approximately 15–20 each (+30–45 calls beyond the existing 15); the four additional adversarial fixtures at 5 each (+20 calls); the seven GOAL/NOACTION fixtures at 2–3 each as a minimum-coverage check, not a full characterization (+14–21 calls). **Illustrative total: roughly 65–85 additional decision-step-only live model calls** — well below the original 220-call Family A schedule and far below the full 483-call campaign, but not negligible.
- **Stopping/safety conditions:** the existing, unmodified `isAdversarialCategoryFalsePositive` safety checkpoint (first-occurrence, non-numeric) would apply exactly as before — see Section 9 on why this must not be altered.
- **Timeout treatment:** the existing, unmodified 90,000 ms ceiling and governed scored-trial timeout semantics — no change.
- **Durability/exact-once:** the existing, already-corrected `encodeObservation`/`Unit3CArmLedger` machinery — no change required.
- **Existing machinery sufficiency:** the current test-tier `FamilyADecisionPromptBuilder`, `Unit3CTrial`/`Unit3CCampaignDefinition` structures, and orchestration driver already support decision-step-only, arbitrary-fixture, arbitrary-repetition trial sets in principle — **but no currently-committed campaign definition expresses this specific, narrower schedule**; a new or amended trial-schedule definition would need to be authored (test-tier only, no `src/` change anticipated) as part of any future Implementation Plan — not performed here.
- **Governance amendment required:** yes — a fresh Unit 3-C-tier Scope Lock (or a narrowly-scoped amendment to the existing one) defining this specific sub-schedule, its own Implementation Plan, Readiness Review, and Explicit Execution Approval Review — the full chain, not an abbreviated one, since this remains a genuine live-model campaign.
- **Fresh campaign ID required:** yes — reusing `unit3c-remedy-experiments-20260810-03` is foreclosed by the existing identity-drift fail-closed mechanism the moment any configuration or schedule differs.
- **Estimated runtime:** based on Attempt 6's own observed per-call latency (roughly 0.9–57 seconds per completed decision-step call, plus the possibility of further genuine timeouts at up to 90 seconds each), an 65–85-call campaign is estimated at very roughly 15–45 minutes of wall-clock time, excluding governance-document drafting time.
- **Artifact storage:** negligible — Attempt 6's own full 26-file, ~280 KB artifact set for 283 total observations implies a proportionally small footprint for a sub-100-call extension.

## 7. Comparability and contamination review

- **Model/digest:** would need to pin the identical `qwen2.5-coder:7b` digest already qualified for this programme, re-verified fresh at execution time exactly as every prior attempt required — no change anticipated, but not guaranteed absent fresh verification (model files can change between sessions).
- **Runtime/provider:** same `LocalHttpModelInferenceClient`/Ollama assumptions, unchanged.
- **Timeout consistency:** must remain 90,000 ms — any change would itself require fresh Unit 3-C Scope Lock Amendment-tier governance and would break comparability with Attempt 6.
- **Prompt/fixture identity:** must reuse the exact, byte-identical `FamilyADecisionPromptBuilder` and fixture text — no paraphrase, no "improved" wording, which would silently change the estimand from "does this mechanism work" to "does a different mechanism work."
- **Does adding fixtures change the estimand?** Yes, and this must be disclosed, not concealed: extending to previously-untested GOAL/NOACTION/adversarial fixtures measures a **broader** estimand (full-corpus decision-step reliability) than Attempt 6's own narrow, five-fixture matched-subset result — the two are complementary, not directly poolable into one "before/after" comparison without care (Section 7's own next point).
- **Does repetition change the evidential interpretation?** Increasing `r01`/`r02`/`r03` repetition narrows the confidence interval on the *same* estimand Attempt 6 already measured — this portion **is** directly poolable with Attempt 6's own existing 15 trials, provided provenance is kept explicit (Section 7's final point).
- **Would the existing safety-checkpoint semantics prematurely censor the extension?** Very plausibly yes, and this must be planned for explicitly, not discovered mid-run: Family A's own decision step has already produced one false positive on its only adversarial exposure to date (`p03`); extending exposure to four more adversarial fixtures materially increases the chance of an early, informative-but-truncating halt, exactly as governed. **This is not a defect to route around — a checkpoint firing during the extension is itself valid, interpretable evidence about Proposition 2**, and any future Scope Lock for this extension must explicitly anticipate and accept a possible early, partial result rather than treating a checkpoint as a failure of the experiment design.
- **Would disabling or changing the checkpoint constitute a governance change?** Yes, absolutely — and **this document does not propose, and would find constitutionally impermissible, any weakening of the existing checkpoint mechanism** to "get more data" past a triggering event. The checkpoint firing IS the answer to part of Proposition 2, not an obstacle to it.
- **Provenance/pooling rule:** historical Attempt 6 evidence and any new extension evidence **must remain explicitly provenance-separated** in any future reporting (Attempt 6 vs. extension, by campaign ID and execution date), exactly as Family C's own live/offline distinction is already required to be kept separate — even where the same fixture (e.g., `r01-direct`) is tested in both, the two campaigns' own trial counts must be reported as what they are (e.g., "5 from Attempt 6 + 15 from the extension = 20 total, drawn from two separate campaigns") and never silently merged as if from one continuous, uninterrupted execution.

## 8. Proportionality

Weighed explicitly, not as a formality:

- **Expected model calls:** ~65–85 for a genuinely dual-proposition design (Section 6); as few as ~30–45 for a Proposition-1-only design, but Section 5.4 already establishes this narrower design carries materially lower decision value.
- **Expected elapsed runtime:** modest (15–45 minutes), not itself a significant burden.
- **Server/hardware load:** modest, well within what this programme has already exercised repeatedly.
- **Artifact volume:** negligible.
- **Governance work required:** **substantial, and this is the dominant cost, not the call count.** Independently observed across this entire program's own history (visible in this very task chain): a new live-model campaign of *any* size requires its own full Scope Lock, Implementation Plan, Readiness Review, Explicit Execution Approval Review, Execution Evidence Review, and Independent Constitutional Reviews of each — the same roughly seven-to-nine-document governance chain regardless of whether the campaign involves 50 or 483 calls. **The fixed governance overhead does not scale down with a narrower experiment**, meaning the "boundedness" of a smaller campaign buys real safety and cost savings on the *execution* side but very little savings on the *governance* side.
- **Probability the evidence materially changes the Unit 3-E position:** independently assessed, from Section 5's own synthesis, as **plausible but not favored** — Control's and Family B's own already-observed, fairly consistent poor performance on the same REMEMBER-recognition axis (0/15 and 1/15 respectively) provides a weak but real prior that Family A's own larger sample is more likely to regress toward similarly poor performance than to confirm or exceed its current small-sample 46.7% figure. This is not a certainty in either direction, but it does not favor optimism.
- **Could an offline or deterministic analysis answer the question instead?** No — both Proposition 1 and Proposition 2 are inherently questions about live model behavior; no offline computation (unlike Family C's own deterministic mechanism) can substitute for it.

**Determination: the governance overhead of authorizing any new live campaign is high and roughly fixed regardless of scope, while the probability of the resulting evidence actually changing the Unit 3-E disposition is genuinely uncertain and, if anything, weakly disfavored by the existing cross-candidate pattern.** This is not a case where "more data is always comforting" is being indulged (the governing task's own explicit concern) — this is a case where the cost is real, largely fixed, and not clearly outweighed by a correspondingly strong expected evidentiary payoff.

## 9. Disposition

```text
C, WITH B AS A QUALIFIED SECONDARY FINDING — EXISTING EVIDENCE AND COUNTERFACTUAL ANALYSIS SHOW A NARROWLY-SCOPED (PROPOSITION-1-ONLY) FAMILY A EXTENSION IS UNLIKELY TO CHANGE THE UNIT 3-E DISPOSITION; A BROADER, DUAL-PROPOSITION EXTENSION CAPABLE OF GENUINE DECISION VALUE IS THEORETICALLY USEFUL BUT NOT CURRENTLY PROPORTIONATE, GIVEN SUBSTANTIAL FIXED GOVERNANCE OVERHEAD WEIGHED AGAINST AN UNCERTAIN, NOT CLEARLY FAVORABLE, EXPECTED EVIDENTIARY PAYOFF.

PAUSE/CLOSURE REMAINS THE PROPORTIONATE CURRENT DISPOSITION.
```

This document therefore **materially qualifies, rather than simply ratifies, the Post-Selection Disposition Planning Review's own principal recommendation.** That review recommended a Family A decision-step extension without fully separating Proposition 1 from Proposition 2 (Section 4 above) or performing the fixed-versus-marginal governance-cost analysis in Section 8. Having performed both, this document finds the originally-recommended narrow design would not, by itself, be capable of resolving the more constitutionally significant open question (Proposition 2), and the broader design that would address both is not currently proportionate to its own uncertain payoff.

**Exact next governance step, if this disposition is accepted:** no further evidence-gathering governance is initiated. The program's own existing "NO REMEDY SELECTED — programme paused" state (Unit 3-E Remedy Selection Review, Section 15/22) continues in effect, unchanged, requiring no further action to remain lawful. Should a future task identify independent grounds — e.g., a specific, narrower, cheaper way to gain evidence on Proposition 2 that this document has not considered, or a change in the governance overhead structure that would lower the fixed cost identified in Section 8 — that task would need to perform its own fresh proportionality analysis, not treat this document's own conclusion as permanently foreclosing reconsideration. **This document does not draft a Scope Lock or Implementation Plan for the illustrative design in Section 6**, consistent with the governing task's own explicit prohibition.

## 10. Confirmation

No live model call was made during this task beyond none at all — this document performs no live evidence gathering. No campaign was created, resumed, or modified. No production, test, or Gradle file was modified. No Attempt 6 or historical campaign artifact was altered. No remedy was selected. No Scope Lock or Implementation Plan was drafted. Unit 4 was not begun.
