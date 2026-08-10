**Status:** Unit 2-D Diagnostic Interpretation and Closure Review — **UNIT 2-D CLOSED. DIAGNOSTIC PURPOSE FULFILLED.** Governance/interpretation only, against committed baseline `5ac3b1f`. No model call, no `/api/generate` call, no campaign creation, and no repository mutation beyond this document occurred.

# Unit 2-D Diagnostic Characterisation — Interpretation and Closure Review

## 1. Status

This document interprets the completed 24-observation Unit 2-D campaign against the frozen Scope Lock's own pre-registered rules, determines whether Unit 2-D's diagnostic purpose is fulfilled, and determines Unit 3 readiness. It selects no remedy and authorizes no implementation.

## 2. Authority and baseline

HEAD independently confirmed `5ac3b1f9921979be2a1538bddab1c137771523ea`, equal to `origin/main`, working tree clean. This commit contains the Execution Evidence Review; the sealed campaign artifacts live outside the repository, on the Ubuntu host, and were independently re-inspected in Section 3.

## 3. Artifact-integrity verification

Independently recomputed, not accepted from the Execution Evidence Review's own tables:

```text
warmup/raw.jsonl            sha256=d542f0ede35fffb51c3907452afac7c2f2cbfd6533fdad0def34be7c640d5c3d  (2 lines)
production-track/raw.jsonl  sha256=568f08f2421dd5a63e949268d2519cae3e0635abc713d437352e141636bcfde7  (17 lines)
candidate-track/raw.jsonl   sha256=c12de361ffec2e39ce4e10b2ee0398cadd1b0ea15e30f5ae10dc41e5b789677f  (5 lines)
```

All three match the Execution Evidence Review's table and `manifest.txt`'s own recorded hashes exactly. `campaign.sealed` present; `campaign.halted` absent. Total observations: 2+17+5 = **24**, no more, no fewer. The only file bearing a modification time after `campaign.sealed` is `artifact-hash-inventory.txt` — expected, since the driver's own `sealCampaign()` writes the manifest and seal marker first and the inventory (which must cover the seal marker itself) second; not evidence of post-seal mutation. The frozen Unit 2 campaign (`qwen25coder7b-baseline-20260809`) was independently re-hashed across all seven frozen artifacts and found unchanged, `stage-0.sealed` still absent.

## 4. DQ1 — Repeatability

**QUESTION:** Under `R01-direct`, `minimal-production-context`, pinned `qwen2.5-coder:7b`, is the REMEMBER→non-REMEMBER miss reproduced consistently across repeated independent calls, or does it vary?

**EVIDENCE:** 10 independent attempts, same fixture/profile/model/commit.

**OBSERVED RESULT:** 10/10 diverge from `REMEMBER`. Not perfectly uniform in *which* wrong action: 9/10 selected `REPLY`, 1/10 (attempt 07) selected `NOACTION`. All 10 have `representationValid = true` — every miss was a clean, well-formed, wrong-action tag, never a malformed one. No timeout, no transport failure.

**SUPPORTED INTERPRETATION:** The higher-level question ("will this exact call select REMEMBER or not") resolved unanimously and reproducibly to "not," across ten independent draws — near-deterministic, reliable semantic weakness for this exact fixture/profile/model/commit combination (Scope Lock §21 rule 1). The lower-level question ("which specific wrong action") showed a small amount of genuine run-to-run variation (one outlier), meaning the miss is reproducible in kind (never REMEMBER) but not perfectly rigid in its exact form.

**UNSUPPORTED INTERPRETATIONS:** No population-level failure probability for `REMEMBER` in general. No claim that the true underlying non-REMEMBER rate is 100% — per the Scope Lock's own pre-registered caveat, ten repeats cannot rule out a moderate true correct-selection rate (for example 10–20%) that simply did not surface in this sample. No claim about any other fixture, profile, or model. No claim this is deterministic in a formal sense — nine-of-ten agreement on the *specific* wrong answer is strong but not total uniformity.

**RESIDUAL UNCERTAINTY:** Whether a larger sample would show occasional correct selections; whether the one `NOACTION` outlier reflects genuine stochastic variation or an artifact of this particular draw.

**DECISION RELEVANCE:** Decision-critical — this is the single strongest, most repeated observation in the entire campaign and the anchor against which DQ3, DQ4, and DQ5 are all read jointly.

## 5. DQ2 — Action-family breadth

**QUESTION:** Do `P01-ordinary-fact` (REPLY), `P06-greeting` (REPLY), `G01-multistep` (GOAL), and `N01-heartbeat` (NOACTION) show any comparable single-attempt semantic miss, or is the weakness concentrated in REMEMBER?

**EVIDENCE:** One attempt each, minimal context, Qwen.

**OBSERVED RESULT:** `P01-ordinary-fact` → `REPLY`, correct (`A`). `P06-greeting` → `REPLY`, correct (`A`). `G01-multistep` → `REPLY`, **wrong** (`D`) — expected `GOAL`. `N01-heartbeat` → `"NORESPONSE"`, an untagged, non-conforming token — a **representation failure** (`E`, `representationValid = false`), not a semantic classification at all, since the parser could not classify an action to compare against `NOACTION`.

**SUPPORTED INTERPRETATION:** The two REPLY-family fixtures were handled correctly in both observed instances. The GOAL-family fixture shows one comparable single-attempt semantic miss — the weakness is **not cleanly isolated to REMEMBER**; at minimum one other action family also missed once. The NOACTION-family fixture cannot be assessed semantically at all in this instance, because representation itself failed first — this is evidence on the *representation* axis (relevant to DQ6), not the semantic axis, and must not be counted as either a semantic success or failure for NOACTION specifically. Kept separate rather than flattened together, as required: one genuine semantic miss (GOAL) and one genuine representation miss (NOACTION) are two different kinds of event, not two data points on the same scale.

**UNSUPPORTED INTERPRETATIONS:** No claim that GOAL selection is "unreliable" (n=1). No claim that REPLY selection is "reliable" (n=2). No claim that NOACTION handling is broken (representation, not semantics, failed here, and n=1). No claim that the weakness is definitively "REMEMBER-specific" — that framing is weakened, not confirmed, by this single GOAL miss.

**RESIDUAL UNCERTAINTY:** Whether GOAL's single miss reflects a genuine, comparable weakness or an unlucky single draw (exactly DQ1's own repeatability question, untested for GOAL). Whether NOACTION's representation failure is fixture-specific or would recur.

**DECISION RELEVANCE:** Decision-critical for remedy scoping — directly bears on whether any eventual remedy investigation should be REMEMBER-specific or broader, and this evidence argues against assuming the narrower scope by default.

## 6. DQ3 — Context sensitivity

**QUESTION:** For `R01-direct`, does semantic action selection differ under `mixed-full-production-like` or `conversation-history` context, relative to the minimal-context baseline?

**EVIDENCE:** One attempt each.

**OBSERVED RESULT:** `mixed-full-production-like` → `REPLY`, wrong. `conversation-history` → `REPLY`, wrong. Both match DQ1's majority-mode outcome exactly.

**SUPPORTED INTERPRETATION:** In these two specific instances, richer context did not resolve the miss — weak evidence that context enrichment alone is not sufficient to fix REMEMBER selection for this fixture, read jointly with DQ1's high (though not total) consistency, per Scope Lock §21 rule 7.

**UNSUPPORTED INTERPRETATIONS (what two single attempts cannot establish):** No claim that context has no effect in general — only two of the nine possible Unit 1 context profiles were tested, both in the direction of "did not help." No claim that these two results are themselves reproducible (DQ1's own finding — that even a 9/10-consistent pattern still contains variation — applies with even more force to n=1 observations). No causal claim: association between context and outcome is explicitly not causation.

**RESIDUAL UNCERTAINTY:** Whether either profile would repeat its result on a second attempt; whether the seven untested profiles behave differently; whether context matters at all as a variable, versus being irrelevant to this particular miss.

**DECISION RELEVANCE:** Useful but not decision-critical — informative for future prompt-design triage, not sufficient alone to rule context in or out as a contributing factor.

## 7. DQ4 — Model specificity

**QUESTION:** Does `llama3.2:3b` select REMEMBER where `qwen2.5-coder:7b` does not, or does it show the same miss?

**EVIDENCE:** One attempt, identical fixture/prompt/harness, `llama3.2:3b`.

**OBSERVED RESULT:** `REPLY`, wrong — the same miss, same specific wrong action, as Qwen's majority-mode outcome.

**SUPPORTED INTERPRETATION:** Both models missed on the identical fixture — supports the protocol/prompt design being at least a partial contributor to the miss, weakening (not eliminating) a purely Qwen-specific explanation, per Scope Lock §21 rule 4.

**PROHIBITED / UNSUPPORTED INTERPRETATIONS:** **No model-ranking conclusion of any kind.** Not "Llama is worse," not "Llama is comparable to Qwen," not "the protocol is broken for all models." `llama3.2:3b` (3B, general-purpose) and `qwen2.5-coder:7b` (7B, code-specialized) differ in both size and specialization simultaneously — this single, confounded data point cannot isolate either variable. Not evidence about Llama's reliability generally (n=1). Must be read jointly with DQ1: Qwen itself only failed 10/10 with high-not-total consistency, so a single Llama attempt landing on the majority outcome is unsurprising and modest evidence at best.

**RESIDUAL UNCERTAINTY:** Whether Llama would ever select REMEMBER correctly on repeated attempts (DQ1-equivalent data does not exist for Llama). Whether the shared miss reflects genuine protocol-level difficulty or coincidence given the small sample.

**DECISION RELEVANCE:** Decision-critical as part of the joint evidence set (distinguishing model-specific from protocol-general remedy families), not sufficient alone.

## 8. DQ5 — Decision/rendering coupling

**QUESTION:** Does a decision-only variant change semantic accuracy relative to the joint decide-and-render task, under pinned Qwen?

**EVIDENCE:** 5 attempts, `r01-direct-decision-only`, Qwen, compared against DQ1's 10 joint-task attempts on the underlying-identical fact.

**OBSERVED RESULT:** 2/5 correctly selected `REMEMBER` (attempts 02, 03), both classified `B` (correct action, non-exact content — structurally unavoidable given the fixed placeholder design, never `A`). Attempt 03 followed the placeholder instruction exactly (`"REMEMBER: SELECTED"`); attempt 02 selected correctly but substituted brief content of its own (`"REMEMBER: black coffee mug"`) rather than the literal placeholder. The other 3/5 missed (`NOACTION` once, `REPLY` twice). Against DQ1's 0/10 correct baseline, pooling across this campaign's own REMEMBER-expected trials: 2 correct out of 18 total (DQ1's 10 + DQ3's 2 + DQ4's 1 + DQ5's 5), and **both** successes occurred under the decision-only track specifically.

**SUPPORTED INTERPRETATION:** This is genuine, recorded evidence — not merely a hypothesis — that decision/rendering coupling is a real, non-zero contributor: correct selection occurred exclusively under the track that removed the content-composition burden, never once under the 18 joint-task attempts. This is the single most positively-supported novel finding in the campaign (Scope Lock §21 rule 6).

**Explicitly distinguished, as required:**
- **Evidence of coupling:** Yes, to the extent above — a real, small-sample signal, not proof.
- **Evidence of improvement under the candidate representation:** The observed rate moved from 0/10 (~0%) to 2/5 (40%) within this campaign's own small samples. This is suggestive, not statistically established — with samples this small, a modest true difference and a large apparent difference are not reliably distinguishable from each other or from chance.
- **Evidence sufficient to select a production architecture:** **No.** The decision-only format was never evaluated for content generation, downstream usability, or any other production requirement; it is a diagnostic ablation, not a candidate design.
- **Evidence sufficient to justify controlled remedy investigation:** **Yes** — this is precisely the kind of triage signal Unit 2-D exists to produce: strong enough to make "investigate decision/rendering separation" a reasonable candidate for a future Unit 3 Planning Review to prioritize, without being strong enough to shortcut past that review into a selected design.

**UNSUPPORTED INTERPRETATIONS:** Not a remedy recommendation of any kind. Not established as generalizing beyond `qwen2.5-coder:7b` (Qwen-only, Scope Lock §21 rule 6's own limit). Not evidence that either successful attempt would repeat.

**RESIDUAL UNCERTAINTY:** Whether the 0/10 → 2/5 shift reflects a genuine, reproducible effect of decoupling decision from rendering, or a smaller true effect inflated by chance in a 5-attempt sample; whether it would hold under Llama or other models; whether it would hold using the literal placeholder only (attempt 02's self-substituted content muddies a completely clean read of "the format alone" as the active variable).

**DECISION RELEVANCE:** Decision-critical — the primary candidate-remedy-family signal this whole unit was designed to surface.

## 9. DQ6 — Representation independence

**QUESTION:** Does representation validity vary independently of semantic correctness across all DQ1–DQ5 observations?

**EVIDENCE:** Cross-cutting analysis over all 22 non-warm-up observations (10+4+2+1+5); zero additional calls.

**OBSERVED RESULT:** `representationValid = false` occurred exactly twice: `N01-heartbeat` (DQ2, `"NORESPONSE"`, classification `E`) and, among the two warm-ups excluded from DQ1–DQ5 proper but observed in the same raw data, the Llama warm-up (`"REPLY:"` with blank content, classification `G`, caught by `Reply`'s own non-blank-text constructor validation). All 17 semantic-only misses (`D`) had `representationValid = true` — clean, well-formed, wrong-action tags. The two representation failures and the seventeen semantic failures never co-occurred on the same trial.

**SUPPORTED INTERPRETATION:** Semantic protocol reliability cannot be reduced to output-format validity alone — the two axes are empirically distinct in this campaign's own data, not merely in the single PF01 instance that first established the principle. A trial can fail representation while never reaching a semantic judgment (`N01-heartbeat`), or pass representation cleanly while failing semantics (all 17 `D` cases) — collapsing these into one score would have hidden that DQ1's 10/10 divergence is a pure semantic phenomenon, not a formatting artifact.

**UNSUPPORTED INTERPRETATIONS:** No representation-failure rate (2/24 is a count in this specific small campaign, not a population estimate). No claim that representation problems are a significant driver of overall unreliability (the opposite is closer to true here: representation was overwhelmingly reliable, 22/24, while semantics was the dominant source of miss).

**RESIDUAL UNCERTAINTY:** Whether the two representation failures share a common cause or are unrelated one-offs.

**DECISION RELEVANCE:** Foundational — this finding is what makes DQ1–DQ5's semantic findings interpretable as semantic findings at all, rather than confounded formatting noise.

## 10. Cross-DQ synthesis

| Explanation | Class | Basis |
|---|---|---|
| PF01 was an isolated stochastic accident | **A — contradicted** (as stated) | DQ1's 10/10 reproduction directly contradicts "isolated"; a residual possibility of *some* non-zero correct rate remains open per DQ1's own caveat |
| Transport instability | **A — contradicted** | 0/24 transport failures observed |
| Timeout instability | **A — contradicted** | 0/24 timeouts; latencies 0.95s–65.4s, all well under the 90s ceiling |
| Parser defect | **A — contradicted** | parser handled all 24 cases, including two genuine edge cases, exactly as designed and offline-tested; zero modification |
| Harness/classifier defect | **A — contradicted** | same basis |
| Simple malformed-output explanation (for DQ1 specifically) | **A — contradicted** | all 10 DQ1 misses had `representationValid = true` — clean, wrong-action tags, not malformed ones |
| REMEMBER-specific semantic weakness | **B — weakened** | strongly evidenced for REMEMBER (16/18 pooled misses) but not cleanly isolated, given DQ2's one GOAL miss |
| Broader action-selection weakness (beyond REMEMBER) | **C — still plausible** | weakly suggested by one GOAL miss; far too thin (n=1) to be positively supported |
| Context sensitivity (as a fix) | **B — weakened** | both tested enrichments failed to resolve the miss; not eliminated as a variable generally (7/9 profiles untested) |
| Qwen-specific weakness | **B — weakened** | Llama shares the identical miss on the one tested fixture |
| Model-independent protocol difficulty | **C/D — still plausible, positively supported at the "partial contributor" level** | DQ4's shared miss is exactly this kind of evidence, per Scope Lock §21 rule 4's own framing |
| Decision/rendering coupling | **D — positively supported** (not proven) | 0/18 joint-task vs 2/5 decision-only is the strongest positive signal in the campaign |
| Inference-setting effects (temperature/seed/sampling) | **E — not tested** | Scope Lock forbade varying these; zero evidence either way |
| Prompt-design effects beyond the one DQ5 variant | **E — not tested** | only one candidate variant authorized |
| Structured/schema-constrained output effects | **E — not tested** | explicitly excluded from Unit 2-D's scope |
| Retry/repair effects | **E — not tested** | no retry logic exists anywhere in this campaign |
| Model-size effects specifically | **E — not tested (uninterpretable)** | DQ4's one data point confounds size and specialization inseparably |

No explanation is claimed disproven beyond what the evidence in Sections 4–9 actually supports; several remain open (E) precisely because Scope Lock's own boundaries kept them untested by design, not by oversight.

## 11. Gradle execution-status qualification

The wrapping `:reasoningProtocolUnit2DDiagnostic` Gradle task reported `FAILED` overall because one unrelated offline test (`absent explicit environment configuration means the live entry point skips regardless of the property`) asserts that the live environment variables are absent — an assumption necessarily false during the deliberate, authorized live run that set them.

1. **Did it affect any of the 24 authorized calls?** No. That test method shares no state, no files, and no code path with the live entry-point test; it only reads `System.getenv` and asserts. Independently confirmed by direct inspection of both test bodies and by the fact that all 24 raw records, correctly attributed and hash-verified, exist.
2. **Did it affect campaign sealing?** No. `campaign.sealed` was written by the live entry-point test's own successful completion of `DiagnosticCampaignRunner.run`, a code path entirely independent of any other `@Test` method's pass/fail status — JUnit executes each test method independently.
3. **Did it affect artifact integrity?** No — Section 3's independent hash recomputation confirms this directly.
4. **Does it affect confidence in the diagnostic observations?** No — the failing assertion never touches observation-generating code.
5. **Does it reveal a future improvement opportunity?** Yes — this test's assumption ("env vars are always absent") is incompatible with the one legitimate scenario (deliberate live execution) where it will predictably and harmlessly fail, producing a confusing red/FAILED Gradle status alongside a fully successful campaign. A future revision should scope that assertion to run only when the live property itself is absent, mirroring the live test's own first `assumeTrue` gate.

```text
CLASSIFICATION: NON-INVALIDATING IMPLEMENTATION/TEST-ERGONOMICS DEFECT
```

Real, but strictly cosmetic to this campaign's evidentiary validity — not "harmless expected behavior" (nobody designed it to fail here), and not diagnostic-invalidating (nothing about the 24 observations is touched). No implementation change is made in response; this task's own instructions prohibit it, and doing so now would be premature relative to the governed correction workflow this whole programme has used throughout.

## 12. Frozen exit-criteria assessment (Scope Lock §22)

| Criterion | Result |
|---|---|
| DQ1 through DQ5 each have a recorded result | **PASS** |
| DQ6's cross-cutting analysis is complete | **PASS** |
| Every artifact passes hash/integrity verification | **PASS** |
| An Independent Constitutional Review confirms the campaign followed the Scope Lock exactly | **PASS WITH QUALIFICATION** — this Closure Review independently re-verified fixture/profile/model/attempt fidelity for all 24 trials against the frozen schedule (Section 3–9) and found exact conformance, no truncation, no expansion; formal finalization of this criterion is deferred to the Independent Constitutional Review of this document (Phase 11), produced alongside it |
| The pre-registered interpretation worksheet has been applied to actual results, traceable to raw records | **PASS** — Sections 4–10 above, every claim traceable to a named trial ID |

```text
UNIT 2-D DIAGNOSTIC PURPOSE:
FULFILLED
```

No preferred outcome was required or assumed; several DQs resolved to genuinely bounded, non-triumphant findings (DQ2's breadth ambiguity, DQ3's weak signal, DQ4's confound) and are accepted as such.

## 13. Residual uncertainty register

- DQ1: whether a larger sample would surface occasional correct REMEMBER selections; nature of the one `NOACTION` outlier.
- DQ2: whether GOAL's single miss is a genuine, comparable weakness or an unlucky draw; NOACTION's representation failure's generality.
- DQ3: whether either context result would reproduce; the seven untested profiles.
- DQ4: whether the shared miss is protocol-driven or coincidental at this sample size; Llama's own repeatability, untested.
- DQ5: whether the 0/18 → 2/5 shift is a genuine, reproducible coupling effect or a small-sample artifact; generalization to other models; the effect of the literal placeholder alone versus the looser instruction attempt 02 exhibited.
- DQ6: whether the two representation failures share a cause.
- Cross-cutting: inference-configuration effects, broader prompt-design space, structured output, and retry/repair remain entirely untested by design.

## 14. Unit 2-D closure determination

```text
A — CLOSE. DIAGNOSTIC PURPOSE FULFILLED.
```

No genuine execution defect invalidates any observation (Section 11); no authorized evidence is missing (Section 12); the residual uncertainties in Section 13 are carried forward as exactly that — open questions for Unit 3 to weigh, not gaps requiring further Unit 2-D execution. Re-running Unit 2-D, or extending it, is not authorized or recommended by this review.

## 15. Unit 3 readiness determination

```text
A — UNIT 3 PLANNING ONLY
```

Unit 2-D's evidence is sufficient to inform a **Unit 3 Planning Review** — genuine triage signal exists (Section 10) for what candidate remedy families deserve consideration, in what rough priority (decision/rendering coupling and REMEMBER-specific/broader-breadth triage are the best-evidenced candidates; structured output, retry/repair, and inference controls remain entirely untested and would need their own justification). It is **not** sufficient to authorize Unit 3 Scope Lock drafting (which presupposes specific experimental design decisions a Planning Review should establish first, exactly mirroring Unit 2-D's own path — Planning Review preceded Scope Lock throughout this entire programme, with no exception), and obviously not implementation (no remedy has been selected, let alone designed).

Unit 3 planning may **name and prioritize** candidate remedy families — semantic decision/rendering separation, structured/schema-constrained output, prompt/protocol redesign, retry or semantic repair, inference controls, model selection, and other reliability mechanisms — as topics worth a future, separately governed investigation. It may not **select** any of them, prototype any of them, or claim any of them is validated by Unit 2-D's evidence.

## 16. Prohibited overclaims

Explicitly not established by this campaign, restated for the record: a population-level failure probability for any action; that REMEMBER "never" or "always" works; any model ranking or replacement conclusion; that decision/rendering separation is a viable or selected production design; that structured output would help (untested); that context has no effect (only two of nine profiles tested); that the GOAL/NOACTION findings generalize (n=1 each); that this campaign's Gradle-reported failure reflects any defect in the 24 observations themselves.

## 17. Confirmation: no remedy selected

No production code, harness, parser, prompt, or Gradle configuration was modified during this review. No remedy family is recommended, prioritized as "the" answer, or implemented anywhere in this document — Section 15 explicitly limits authorization to planning-level naming and prioritization, not selection.

## 18. Confirmation: no live execution during this review

No `/api/generate`, `/api/tags`, or `/api/show` call occurred. No campaign was created, resumed, or modified. All evidence in this document was drawn from the sealed, read-only campaign artifacts and independently recomputed hashes.

## 19. Repository state

No production, test, or Gradle file changed. The only new artifact this task produces (so far) is this document; a second, independent review follows in the same task per Phase 11.
