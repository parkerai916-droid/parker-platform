**Status:** Unit 3 — Reliability Contract and Remedy Selection — Planning Review — **PASS.** Planning/governance only, against committed baseline `911d1c6`. No implementation, no live call, no remedy selection, and no repository mutation beyond this document occurred.

# Reasoning Protocol Live-Model Conformance Unit 3 — Reliability Contract and Remedy Selection — Planning Review

## 1. Status and authority

This is the Planning Review required before any Unit 3 Scope Lock. It authorizes nothing beyond planning. Unit 3's own existence, name, and boundary are not invented here — they are inherited from the original, top-level `REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_AND_STRUCTURED_OUTPUT_RELIABILITY_PLANNING_REVIEW.md` (`05d4c2a`, ACCEPTED by its own Independent Constitutional Review), which this review treats as controlling, primary authority for Unit 3's scope, and from the intervening Unit 2/2-D chain, which supplies the evidence that document anticipated Unit 3 would need.

## 2. Accepted baseline

HEAD independently confirmed `911d1c64d530ac31980b459a365c268eae8914be`, equal to `origin/main`, working tree clean.

## 3. Unit 2/2-D evidence inherited

Unit 2 (`qwen25coder7b-baseline-20260809`) closed adverse and truncated at its Stage 0 fail-closed gate: one trustworthy observation, PF01, a genuine semantic action-selection failure (`R01-direct`, expected `REMEMBER`, actual `REPLY`, representation valid, classification `D`). Unit 2-D (`qwen25coder7b-llama32-3b-diagnostic-20260809`) closed with its diagnostic purpose fulfilled: 24 real observations across DQ1–DQ6, independently interpreted and closed with its own Independent Constitutional Review's acceptance. Both campaigns remain preserved, sealed, and untouched. Unit 2-D's Interpretation and Closure Independent Constitutional Review authorized **Unit 3 planning only** — naming and prioritizing candidate remedy families, never selecting one. This review does not relitigate either closed unit's findings; Section 4 below treats them as evidence to build on.

## 4. Unit 3 purpose

Read directly from the original Planning Review's own programme table (§13): *"3 — Reliability Contract and Remedy Selection | freeze semantic/rendering contract, schema/config/retry rules, model and timeout decision criteria based on Unit 2 | architecture/scope/implementation-plan documents | contract and constitutional reviews before code."* Critically, that same table names a **separate, later Unit 4 — Selected Remedy Implementation** for actual code. Unit 3 was never intended to write code; it was intended to produce governance documents. Unit 2-D's insertion (via the Post-Unit-2 Diagnostic Planning Review) between Unit 2 and Unit 3 qualifies this table by enriching Unit 3's evidence base — Unit 3 is now based on Unit 2 *and* Unit 2-D — without altering Unit 3's fundamental character or its separation from Unit 4.

**A. Reliability Contract:** the behavioral guarantees Parker's reasoning boundary must provide before being considered production-reliable — addressed in Section 5.

**B. Remedy Selection:** the evidence-based process for choosing which mechanism(s) satisfy that contract. Critically, Unit 2/2-D evidence is *diagnostic/characterisation-tier* (Unit 2-D: 1–10 attempts per cell; Unit 2's own single PF01 observation). The original Planning Review (§6) already establishes a *higher*, separate evidentiary tier — **Qualification** — required before any remedy can be *selected*: pre-registered sample sizes, zero-event gates at ≥300 exposures for critical consequential guarantees. Unit 2/2-D evidence informs *which* remedy families deserve that qualification-tier investment; it does not itself constitute that investment.

**On the wire representation's mutability:** the original Planning Review (§9) already, explicitly, neither freezes nor mandates replacing the current `GOAL:`/`REPLY:`/`REMEMBER:`/`NOACTION` tagged-string representation: *"A later structured transport can map a validated action/content object into the existing sealed `ReasoningProviderResponse` domain model; that alone need not change the domain contract."* The **domain contract** (the sealed four-variant `ReasoningProviderResponse` type) is treated as comparatively stable — changing it (a fifth action, confidence scores, repair semantics) requires its own separately governed contract change. The **wire representation** is explicitly open to revision. This review adopts that same distinction rather than assuming either extreme.

## 5. Reliability Contract analysis

| Property | Classification | Basis |
|---|---|---|
| Semantic action-selection correctness (all four actions) | **MANDATORY** | core programme driver; already governed at qualification tier (original Planning Review §6) |
| Explicit REMEMBER instruction recognition | **MANDATORY** | the specific, characterized failure (PF01, DQ1, DQ3, DQ4) |
| Avoidance of false-positive REMEMBER | **MANDATORY** | already a zero-tolerance qualification gate (§6: "zero observed false-positive REMEMBER and GOAL on all negative/adversarial exposures") |
| GOAL selection correctness | **MANDATORY** | same contract; DQ2 recorded one comparable miss |
| REPLY selection correctness | **MANDATORY** | same contract; DQ2 recorded two correct instances |
| NOACTION selection correctness | **MANDATORY** | same contract; DQ2 recorded a representation failure here, not yet a semantic one |
| Representation validity | **MANDATORY** | independent axis; already governed (≥99%, §6) |
| Parser compatibility | **MANDATORY**, subordinate to representation validity | any remedy must remain compatible with, or deliberately and separately supersede, `TaggedReasoningResponseParser`'s contract |
| Content fidelity | **MANDATORY** | already governed (zero material mutation/invention, §6) |
| Ambiguity handling (doubt routes to REPLY, never REMEMBER) | **MANDATORY** | existing, already-governed design principle, reaffirmed unmodified throughout Units 1–2-D |
| Deterministic/fail-closed behavior where required | **MANDATORY** | Parker-wide constitutional invariant, reaffirmed in every unit's own stop-condition design |
| Retry behavior | **CANDIDATE** | not a contract mandate; if ever adopted, already pre-constrained (fixed limit, full attempt evidence, no non-consequential-to-consequential escalation, §8/§10) |
| Malformed-output handling (measured, not silently dropped) | **MANDATORY** | classes C/E/F/G already governed measurement axes |
| Transport failure handling | **MANDATORY** | class I; zero observed in Unit 2-D, still a required measured axis |
| Timeout failure handling | **MANDATORY** as a measured/handled axis; **UNRESOLVED** as a specific value | original Planning Review §12: any runtime timeout value "must derive from measured percentiles... explicit safety margin"; not yet done |
| Model/provider independence | **MANDATORY at the process level** (qualification must be model-agnostic in method, §11); **UNRESOLVED** which model(s) ultimately qualify |
| Downstream isolation (no Memory/Goal/Planner path touched) | **MANDATORY** | absolute, reaffirmed constitutional invariant across the entire chain |
| Observability/provenance | **MANDATORY** | already governed (opt-in artifacts, immutable digest tracking, §7) |
| Measurable acceptance thresholds exist and are pre-registered | **MANDATORY** that they exist; **UNRESOLVED** for any threshold not already frozen |
| Actual remedy implementation | **OUT OF SCOPE** for Unit 3 itself (reserved for Unit 4) |
| Live model calls during Unit 3 planning/contract work | **OUT OF SCOPE** |
| Reopening Memory Core/Goal governance | **OUT OF SCOPE** |

No numerical threshold is invented in this table beyond what the original Planning Review already froze (§6): zero false-positive REMEMBER/GOAL; zero material mutation/invention; ≥99% representation validity; ≥97% one-sided-95%-lower-confidence-bound correct action selection per action/full-context stratum; the 300-exposure zero-event statistical rationale. Any threshold beyond these — timeout values, mechanism-specific metrics for a not-yet-existing remedy family — is marked **UNRESOLVED**, requiring its own explicit governance decision before use.

## 6. Remedy-family analysis

### 1. Semantic decision/rendering separation
**MECHANISM:** split the single joint decide-and-render model completion into a decision-only step and a separate content-rendering step. **PROBLEM IT COULD ADDRESS:** possible coupling between composition burden and action selection. **UNIT 2/2-D EVIDENCE SUPPORT:** DQ5 — 2/5 correct `REMEMBER` under the decision-only variant vs. 0/18 pooled across every joint-task REMEMBER-expected trial in this campaign. **EVIDENCE AGAINST:** one of the two successes (attempt 02) did not follow the fixed placeholder exactly, muddying a clean read of "removing content composition" as the sole active variable; single model, single fixture, five attempts. **RISKS:** the decision step remains a model-authored semantic authority — this relocates, not eliminates, reliance on model judgment (original Planning Review §8: "classifier remains semantic authority; adds model/path complexity"). **CONSTITUTIONAL/ARCHITECTURAL IMPACT:** could map into the existing `ReasoningProviderResponse` domain contract unchanged in principle (§9), but a genuine two-call flow conflicts with `ModelReasoningProvider`'s documented single-call, no-retry, no-repair invariant and would need fresh governance. **NEW EVIDENCE REQUIRED:** larger repeated-trial comparison; cross-model replication; a content-preserving (not placeholder-only) variant to isolate format from composition-burden as separate variables. **CURRENT STATUS:** **WARRANTS CONTROLLED INVESTIGATION.**

### 2. Structured/schema-constrained model output
**MECHANISM:** constrain generation via JSON schema or grammar (Ollama's `format` parameter). **PROBLEM IT COULD ADDRESS:** representation-layer failures (classes C/E/F/G) specifically. **EVIDENCE SUPPORT:** none direct (explicitly untested by Unit 2-D's own design); DQ2's `N01-heartbeat` (`"NORESPONSE"`) and the Llama warm-up's blank-content failure are exactly the *kind* of event structured output could structurally prevent. **EVIDENCE AGAINST:** PF01 and 17 of Unit 2-D's 24 observations were representation-valid and semantically wrong — the *dominant* observed failure mode has nothing for structured output to fix; a schema-constrained `{"action":"REPLY"}` would have been exactly as wrong. **RISKS:** "laundering" a wrong decision behind valid syntax — already anticipated and guarded against at the acceptance-criteria level by the original Planning Review's own adversarial check, not by the mechanism itself. **CONSTITUTIONAL IMPACT:** touches `LocalHttpModelInferenceClient`'s request construction, explicitly forbidden without fresh governance throughout this programme. **NEW EVIDENCE REQUIRED:** a controlled trial comparing representation-failure *rates* under constrained vs. free-text decoding, plus continued semantic-accuracy measurement under constraint (untested whether constraining decode space shifts semantic tendency at all — genuinely open, not resolved by definition). **CURRENT STATUS:** **PLAUSIBLE BUT CURRENTLY WEAKLY EVIDENCED** for representation-layer failures specifically; **NOT RESPONSIVE TO THE DOMINANT OBSERVED FAILURE** (semantic misses with valid representation).

### 3. Prompt/protocol redesign
**MECHANISM:** revise instruction wording, ordering, or emphasis beyond the frozen production text. **PROBLEM IT COULD ADDRESS:** semantic weakness if attributable to instruction clarity. **EVIDENCE SUPPORT:** `DefaultReasoningPromptBuilder`'s own history records one prior successful revision (adding `SELECTION_GUIDANCE` after live testing showed NOACTION over-defaulting) — precedent that prompt revision can work for *some* symptoms. **EVIDENCE AGAINST:** the current prompt already contains an explicit worked example ("Remember that X") nearly identical to `R01-direct`, and still missed 16 of 18 pooled REMEMBER-expected trials — this is not an obviously under-specified prompt. **RISKS:** regression of currently-correct actions (§8); already-governed stop rule exists ("two materially distinct, evidence-led prompt revisions fail," §11). **CONSTITUTIONAL IMPACT:** touches `DefaultReasoningPromptBuilder`, forbidden without fresh governance throughout Units 1–2-D. **NEW EVIDENCE REQUIRED:** bounded, pre-registered, held-out-fixture trials per the already-governed §11 process. **CURRENT STATUS:** **WARRANTS CONTROLLED INVESTIGATION** (legitimate, precedented, not yet run).

### 4. Retry
**MECHANISM:** automatic re-invocation on failure, under already-governed constraints (fixed limit, full attempt evidence, no non-consequential-to-consequential escalation). **PROBLEM IT COULD ADDRESS:** transient/stochastic misses. **EVIDENCE SUPPORT:** none positive — 0/18 pooled joint-task REMEMBER attempts ever succeeded. **EVIDENCE AGAINST:** DQ1's high (9/10) consistency in the *specific* wrong answer argues against this looking like transient noise around a mostly-correct baseline. **RISKS:** the original Planning Review's own "semantic retry" authority-escalation concern; `ModelReasoningProvider`'s documented no-retry invariant. **CONSTITUTIONAL IMPACT:** requires new, separately governed authority for any semantic-level retry. **NEW EVIDENCE REQUIRED:** larger same-prompt repeated-sampling data to check for any correct draws at all. **CURRENT STATUS:** **NOT RESPONSIVE TO OBSERVED FAILURE** for semantic misses. Retry restricted to representation-class failures (C/E/F/G) only, already contemplated separately by the original Planning Review (§8), remains a narrower, distinct, less-evidenced-either-way question — zero timeout/transport/representation-failure-triggered-retry scenarios were exercised in Unit 2-D (0 timeouts, 0 transport failures observed).

### 5. Semantic validation and repair
**MECHANISM:** a secondary process reviewing or correcting the primary model's semantic decision. **PROBLEM IT COULD ADDRESS:** post-hoc correction of wrong-action selections. **EVIDENCE SUPPORT:** none tested. **EVIDENCE AGAINST:** the original Planning Review already prohibits a second model as content-fidelity judge ("no second model acts as the acceptance judge"); the same reasoning extends with more force to semantic repair, which would silently override rather than merely assess a decision. **RISKS:** highest of any family considered — compounds rather than resolves unreliability, reduces auditability, and "semantic retry" is already named as excluded from any initial remedy sequence pending its own constitutional authority. **CONSTITUTIONAL IMPACT:** the deepest new governance of any family here. **NEW EVIDENCE REQUIRED:** first, whether any validation signal can detect a wrong decision without already knowing ground truth — a difficult, possibly circular prerequisite question, not yet even framed as investigable. **CURRENT STATUS:** **CONTRAINDICATED BY CURRENT EVIDENCE AND GOVERNANCE** as a near-term direction; rule-based (non-model) validation for narrow explicit cases is a distinct question, addressed under family 9.

### 6. Inference controls
**MECHANISM:** temperature/seed/sampling changes. **PROBLEM IT COULD ADDRESS:** stochastic variance, if material. **EVIDENCE SUPPORT:** none — explicitly untested by every Scope Lock in this programme to date. **EVIDENCE AGAINST:** DQ1's 9/10 consistency is a weak prior against high stochasticity being the dominant driver under current (default) sampling. **RISKS:** any change invalidates prior characterisation, which measured only default settings; otherwise contained (a configuration change, not a structural one). **CONSTITUTIONAL IMPACT:** touches production request construction, forbidden without fresh governance. **NEW EVIDENCE REQUIRED:** entirely untested; would need its own bounded, isolated trial. **CURRENT STATUS:** **PLAUSIBLE BUT CURRENTLY WEAKLY EVIDENCED** (genuinely open in either direction).

### 7. Model selection/substitution
**MECHANISM:** replace `qwen2.5-coder:7b`. **PROBLEM IT COULD ADDRESS:** a genuinely model-specific capability gap. **EVIDENCE SUPPORT:** none positive — the one tested alternative (Llama) also missed. **EVIDENCE AGAINST:** DQ4 directly weakens, without eliminating, a model-specific-only narrative. **RISKS:** the original Planning Review (§11) already requires full blind-corpus qualification, "not model size or reputation," for any substitution decision — a single confounded n=1 comparison is far short of that bar regardless of its outcome. **CONSTITUTIONAL IMPACT:** deployment/configuration change in principle, but qualification cost is large (comparable in scale to Unit 2's own multi-thousand-trial design). **NEW EVIDENCE REQUIRED:** full blind-corpus qualification per the already-governed §11 process. **CURRENT STATUS:** **NOT RESPONSIVE TO OBSERVED FAILURE AS A NEAR-TERM, LOW-COST OPTION** — current evidence gives no reason to expect a different model performs better, and the governed process for even considering it is expensive; not ruled out, not prioritized.

### 8. Multi-model or fallback strategies
**MECHANISM:** ensemble, voting, or fallback among multiple models. **PROBLEM IT COULD ADDRESS:** hedging against any single model's specific weakness. **EVIDENCE SUPPORT:** none tested; DQ4's shared miss is, if anything, weak evidence *against* simple two-model voting being obviously useful here (both agreed on the wrong answer). **EVIDENCE AGAINST:** substantial added complexity, cost, and latency for uncertain benefit; not previously considered anywhere in this programme's governance. **RISKS:** disagreement-resolution logic itself becomes a new semantic authority, echoing family 5's concerns; "model/provider independence" becomes materially harder to reason about. **CONSTITUTIONAL IMPACT:** requires a new decision layer above the existing single-provider architecture — substantial. **NEW EVIDENCE REQUIRED:** everything; untested design space. **CURRENT STATUS:** **OUTSIDE UNIT 3** for now — architecturally heavy and evidentially ungrounded relative to simpler candidates; revisit only if better-evidenced families prove insufficient.

### 9. Deterministic/rule-assisted classification for suitable cases
**MECHANISM:** for sufficiently explicit, low-ambiguity input patterns, apply rule-based pre-classification instead of, or as a check alongside, model judgment, reserving model reasoning for ambiguous cases. **PROBLEM IT COULD ADDRESS:** directly targets DQ1's finding — an explicit, unambiguous instruction (matching the prompt's own worked example) was missed with high consistency. **EVIDENCE SUPPORT:** DQ1's near-total consistency on a fixture chosen specifically for its unambiguity makes a real case that a narrow, targeted safety net for this exact pattern class could help. **EVIDENCE AGAINST / RISK:** brittle keyword matching; the frozen corpus's own negative fixtures (`P02-quoted-remember`, `P03-ambiguous-memory`, and similarly-shaped adversarial cases) exist specifically because near-miss phrasing is common and dangerous to mishandle; language variation far exceeds the fixed, narrow phrasing tested; risk of bypassing model reasoning improperly for cases that only superficially resemble the matched pattern; risk of silent divergence between the deterministic and model paths producing an inconsistent, harder-to-audit system. **CONSTITUTIONAL/ARCHITECTURAL IMPACT:** potentially the largest of any family considered — moving any classification outside the model contradicts this programme's consistent design (the model as sole semantic authority, explicit in `ModelReasoningProvider`'s own documented collaborator boundary) and would very likely require a new intermediate decision type and its own deep constitutional review. **NEW EVIDENCE REQUIRED:** a much broader adversarial/near-miss fixture set tested against any candidate rule specifically to measure *its own* false-positive/false-negative rate, held to at least the same bar as the model path. **CURRENT STATUS:** **WARRANTS CONTROLLED INVESTIGATION**, explicitly flagged as the single highest-constitutional-stakes family — any investigation must begin with the adversarial/false-positive risk surface, not the easy explicit-case win, because the failure mode of getting this wrong (silently promoting a non-instruction into `REMEMBER` via brittle matching) is more dangerous than the problem it would solve.

### 10. Hybrid approaches
**MECHANISM:** combine two or more of the above. **PROBLEM IT COULD ADDRESS:** complementary strengths of multiple mechanisms. **EVIDENCE SUPPORT:** indirect only, by combining the two best-evidenced individual signals (families 1 and 9); no hybrid itself has been tested. **EVIDENCE AGAINST:** none specific; general risk that combining under-evaluated components compounds rather than resolves uncertainty. **RISKS:** compounded complexity; harder attribution of which component drives any observed effect. **CONSTITUTIONAL IMPACT:** inherits the union of whichever components are combined. **NEW EVIDENCE REQUIRED:** each constituent family's own evidence base must mature first. **CURRENT STATUS:** **PLAUSIBLE BUT CURRENTLY WEAKLY EVIDENCED** — a reasonable eventual direction, premature to prioritize ahead of its constituent families.

No remedy is ranked here merely for technical attractiveness; every status above traces to a specific piece of Unit 2/2-D evidence, a specific programme-level governance constraint, or an explicit "untested" finding.

## 7. DQ5 special analysis

Testing the five propositions independently, exactly as required, without collapsing them:

**A. Decision/rendering coupling exists.** **Supported, not proven.** The 0/18 (pooled joint-task) vs. 2/5 (decision-only) asymmetry is genuine, recorded evidence, not merely a hypothesis — but drawn from small samples.

**B. Separating them improves reliability.** **Suggested, not established.** The observed shift is consistent with a real improvement but is also consistent with chance at this sample size (n=5 cannot distinguish a moderate true effect from noise). "Improves" is a causal, reproducible claim this data alone cannot support.

**C. Separation alone is sufficient.** **Not supported — directly contradicted by DQ5's own data.** 3 of 5 decision-only attempts still missed. Whatever coupling contributes, it is not the sole cause of the observed unreliability.

**D. Separation should become Parker's architecture.** **Not supported; explicitly prohibited by governance.** This is a remedy-*selection*-tier claim requiring qualification-tier evidence, a full architectural-impact review (Section 12), and its own constitutional review of a new decision flow. Nothing in Unit 2-D's diagnostic-tier evidence approaches this bar, and no part of this review makes this claim.

**E. Separation warrants controlled comparison against other remedy families.** **Supported.** This is the evidence-proportionate conclusion: DQ5 justifies including decision/rendering separation as a candidate in a future, controlled, comparative Unit 3 experiment — nothing more, and the prior Independent Constitutional Review's warning against "best-evidenced candidate" language sliding into informal selection is honored by stopping exactly here.

## 8. Structured-output analysis

Distinguishing what structured output can and cannot plausibly guarantee, as required: it can plausibly guarantee **syntactic/representation enforcement** — that generated output takes one of an enumerated set of shapes, eliminating malformed, untagged, multiple, or blank output (classes C/E/F/G). It **cannot** guarantee **semantic action-selection correctness** — constraining the decode space to `{GOAL, REPLY, REMEMBER, NOACTION}` does not compel the model to choose the *correct* member of that set. PF01 and 17 of Unit 2-D's 24 observations demonstrate this directly: syntactically valid, wrong-action output was not a hypothetical risk but the dominant observed failure mode. Valid JSON or schema compliance must never be treated as proof, or even evidence, of semantic correctness — the original Planning Review's own adversarial check (a schema-valid `{action: "NOACTION"}` for an explicit Remember instruction) already establishes this as settled programme reasoning, independently reconfirmed here against Unit 2-D's actual data.

## 9. Deterministic-classification analysis

Investigation-planning only, per Phase 8's explicit instruction — no recommendation to implement. The canonical case ("Remember that X") is exactly `R01-direct`, the fixture with the campaign's strongest, most consistent evidence of a semantic miss. A future Unit 3 sub-unit could investigate whether a narrow, rule-assisted path for *only* the most explicit instruction patterns — while preserving full model reasoning for every ambiguous case — is worth pursuing. Constitutional risks, all real and all requiring resolution before any implementation is even prototyped: brittle keyword matching; false positives on the frozen corpus's own quoted/hypothetical/discussion negative fixtures; language variation far exceeding any fixed pattern set; contextual meaning a rule cannot weigh; improperly bypassing model reasoning for cases that only superficially match; and silent divergence between a deterministic and a model-driven path producing an inconsistent, harder-to-audit system. Any future investigation of this family must be designed to stress-test its false-positive surface first, not its easy-case success rate.

## 10. Remaining evidence gaps

None of these gaps block Unit 3 *planning* (Section 14); they define what Unit 3's own later sub-units must close before any remedy is selected:

- Qualification-tier evidence (≥300-exposure zero-event gates) does not exist for any candidate — Unit 2/2-D evidence is diagnostic-tier only, by design.
- DQ5's coupling signal is single-model, single-fixture, and confounded by one attempt's non-literal placeholder use.
- DQ4's model-comparison is a single, doubly-confounded (size and specialization) data point.
- DQ2's GOAL and NOACTION findings are each single attempts.
- DQ3's context-sensitivity evidence covers 2 of 9 possible profiles.
- Inference-configuration effects, broader prompt-design space, structured output, retry, and deterministic-classification false-positive behavior are entirely untested.

## 11. Proposed Unit 3 internal structure

```text
C — a planning unit followed by experimental remedy units and final selection
```

Consistent with the original programme's own Unit 3/Unit 4 separation, and directly serving the stated objective of preventing an experimental candidate's implementation from silently becoming the selected architecture:

- **Unit 3-A — Reliability Contract Definition.** Freezes Section 5's mandatory properties and resolves (via explicit governance, not invention) the thresholds this review left unresolved. A Scope-Lock-tier document; no code, no live call.
- **Unit 3-B — Remedy Experiment Scope Lock(s).** One or more narrowly scoped, Unit-2-D-precedented experiment designs for the highest-priority candidates (Section 6 families 1, 3, and 9 as currently best-evidenced), each requiring its own full Scope Lock → Implementation/Execution Plan → Readiness Review → Explicit Execution Approval chain before any live call — no experiment may skip a gate Unit 2-D itself was required to pass.
- **Unit 3-C — Controlled Remedy Experiments.** The live executions themselves, authorized only under 3-B, structurally comparing candidate mechanisms against the existing baseline — evidence-gathering, not deployment.
- **Unit 3-D — Comparative Evaluation.** Mirrors Unit 2-D's own Interpretation and Closure pattern: determines what the experiment evidence establishes, with its own Independent Constitutional Review.
- **Unit 3-E — Remedy Selection.** A governance decision, backed by 3-D's evidence, selecting one architecture or mechanism (or declining to select one). A document, not code.

Unit 4 — Selected Remedy Implementation — remains exactly where the original programme already placed it: after Unit 3-E, outside Unit 3's own scope, requiring its own Planning Review, Boundary Review, and full verification/Completion/Independent Constitutional Review cycle.

## 12. Acceptance methodology

No new numerical threshold is invented here. Already-frozen, inherited thresholds (Section 5, from the original Planning Review §6) apply as the qualification-tier bar any eventually-selected remedy must clear: zero false-positive REMEMBER/GOAL across all negative/adversarial exposures; zero material content mutation/invention; ≥99% representation validity; ≥97% one-sided-95%-lower-confidence-bound correct action selection per action and full-context stratum; the 300-exposure zero-event statistical design for critical gates. Required evidence dimensions for any remedy comparison, per Phase 11's checklist: frozen fixtures (already exist, Unit 2 corpus); repeated trials at qualification scale (not yet run for any remedy); semantic and representation correctness reported separately (already established methodology); false-positive rates specifically for REMEMBER/GOAL; cross-context behavior; cross-model behavior only where a family's own evidence needs it; regression checks against currently-correct REPLY/GOAL/NOACTION behavior (DQ2's baseline is the reference point); malformed-output, transport, and timeout behavior; reproducibility; artifact integrity; production-path fidelity (real `DefaultReasoningPromptBuilder`/`ModelReasoningProvider`/parser chain, exactly as Units 1–2-D required). Thresholds specific to a not-yet-defined mechanism (for example, an acceptable false-positive ceiling for deterministic classification, or a coupling-improvement significance criterion for decision/rendering separation) are **UNRESOLVED** and require their own explicit governance step inside Unit 3-A or the relevant Unit 3-B, not invention here.

## 13. Architectural boundary analysis

Identified, not amended, per Phase 12's explicit instruction:

- **`ReasoningProviderResponse` semantics** — the sealed four-variant domain contract is treated as stable; any remedy adding a new action, confidence score, or repair semantics crosses this boundary and needs its own contract-change governance (original Planning Review §9).
- **`TaggedReasoningResponseParser` contract** — every Scope Lock in this programme has forbidden modifying it in place; its own documentation already anticipates a *pluggable replacement* satisfying the same one-method `ReasoningResponseParser` interface (structured output would use this route, not an in-place edit).
- **`DefaultReasoningPromptBuilder` responsibility** — likewise forbidden to modify in place; `ReasoningPromptBuilder` is also a swappable `fun interface`, the same architectural seam Unit 2-D's own `DecisionOnlyPromptBuilder` already used for diagnostic purposes without touching production code.
- **Moving semantic classification outside the model** — the single largest boundary crossing under consideration (family 9); contradicts this programme's consistent "model as sole semantic authority" design and has no precedent anywhere in this chain.
- **A new intermediate decision type** — relevant if family 1 (decision/rendering separation) is implemented as a genuinely new typed result rather than internal orchestration collapsing back to the existing domain contract; requires deliberate design care to know which side of this boundary any given implementation falls on.
- **Retry/repair orchestration** — `ModelReasoningProvider`'s documented "no retry, no repair" is an explicit existing invariant; any retry mechanism requires new, separately governed authority (families 4, 5).
- **Provider-specific behavior** — the current design is provider-agnostic by construction (`LocalHttpModelInferenceClient`'s overridable formatter/parser functions exist specifically to avoid this); any remedy hardcoding Ollama-specific behavior more broadly than that seam crosses it.
- **Memory/Goal downstream authority** — absolute, reaffirmed constitutional invariant; no family considered in Section 6 touches it, and none should.
- **Fail-closed semantics** — absolute; any remedy's own failure modes must still fail closed, never silently default to an action.

## 14. Principal planning determination

```text
A — Proceed to Unit 3 Scope Lock drafting now
```

Specifically: draft the Scope Lock for **Unit 3-A (Reliability Contract Definition)**, and within that same document, formally freeze the Section 11 internal structure (3-A through 3-E) as a binding governance requirement for everything that follows — not authorizing 3-B/3-C/3-D/3-E's actual content yet. This is justified because: Unit 3's charter (Section 4) is itself a governance-document task, not an evidence-gathering task — it does not need qualification-tier evidence to *begin*, only to eventually *select*; the original Planning Review already supplies most of the Reliability Contract's mandatory content and thresholds (Section 5); and Unit 2/2-D's evidence, while diagnostic-tier only, is sufficient to responsibly triage the remedy-family landscape (Section 6) without requiring new live evidence first (Section 10's gaps are Unit 3's own future work, not a precondition to Unit 3 existing).

## 15. Authority created

**Planning only.** This review authorizes drafting a Unit 3-A Scope Lock next. It does not itself freeze the Reliability Contract, authorize any live call, select any remedy, or authorize implementation of any kind. This does not exceed planning, consistent with every prior unit's own gate sequence in this programme.

## 16. Prohibited premature conclusions

Not established or claimed anywhere in this review: that decision/rendering separation is or should become Parker's architecture; that structured output solves the observed failure (it does not address the dominant failure mode at all); that deterministic classification is safe to implement (its highest-stakes risks are explicitly foregrounded, not minimized); that Llama or any other model is a viable substitute (n=1, confounded, and the governed qualification process for this decision is expensive); that retry would help (evidence argues against it for semantic misses); that any numerical acceptance threshold beyond those already frozen by the original Planning Review has been decided; that Unit 3 may proceed to Scope Lock drafting for anything beyond Unit 3-A; that production implementation of any kind is authorized.

## 17. Exact next governance step

Drafting the Unit 3-A Reliability Contract Definition Scope Lock, incorporating Section 5's mandatory-property table, Section 11's five-part internal structure, and Section 13's architectural boundaries as binding constraints — not performed, authorized, or initiated by this review.
