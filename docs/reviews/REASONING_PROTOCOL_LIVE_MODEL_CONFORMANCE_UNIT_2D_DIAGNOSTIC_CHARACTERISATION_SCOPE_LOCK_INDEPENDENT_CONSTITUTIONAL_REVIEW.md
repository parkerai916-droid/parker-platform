**Status:** Independent Constitutional Review of the Unit 2-D Diagnostic Characterisation Scope Lock — **ACCEPTED WITH QUALIFICATIONS.** The proposed Scope Lock and its prior read-only Review were both treated as evidence, not authority, and independently re-derived from primary sources: the accepted planning/governance chain, direct source inspection, and the live Ubuntu campaign artifacts. Governance review only, against committed baseline `fd7f221`. No live HTTP call, no campaign creation, and no repository mutation beyond this one new document occurred. Neither the Scope Lock nor its prior Review was modified.

# Unit 2-D Diagnostic Characterisation Scope Lock — Independent Constitutional Review

## 1. Status

This is the Independent Constitutional Review anticipated by the Scope Lock's own §25. It does not authorize implementation, execution, or campaign creation. It does not amend the Scope Lock; where it finds a defect, that defect is recorded here, not corrected in place.

## 2. Review authority and baseline

Authorized by the task's own instruction to conduct this review. HEAD independently verified as `fd7f221c222d6caabcbcdb054b474aa75f93c67c`, equal to `origin/main`. Working tree independently verified clean apart from exactly the two expected untracked documents (`docs/architecture/...UNIT_2D_..._SCOPE_LOCK.md`, `docs/reviews/...SCOPE_LOCK_REVIEW.md`) prior to this review's own output.

## 3. Evidence reviewed

Read fresh in this review: the proposed Scope Lock in full; the existing Scope Lock Review in full; `REASONING_PROTOCOL_POST_UNIT_2_DIAGNOSTIC_PLANNING_REVIEW.md`; `REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_POST_STAGE_0_FAILURE_GOVERNANCE_DETERMINATION.md`; both Unit 2 Stage 0 Failure Review documents; the Unit 2 Scope Lock and Implementation/Execution Plan (already established in this session's record and re-confirmed unchanged by `git diff --stat` across the intervening commits); `src/runtime/ModelInferenceClient.kt`, `src/runtime/ReasoningPromptBuilder.kt`, `src/runtime/ReasoningResponseParser.kt`, `src/runtime/ModelReasoningProvider.kt`; and `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` / `ReasoningProtocolBaselineCharacterisationTest.kt`. Independently re-verified by direct grep, not assumed from the Scope Lock's own prose: `defaultOllamaRequestBody` sets only `model`/`prompt`/`stream:false`, and no `Memory`/`Goal`/`Planner`/composition import or reference exists anywhere in the evaluation harness. Both checks match the Scope Lock's factual claims exactly.

## 4. Constitutional standard applied

Tested against all ten criteria (A–J) in the task, not merely internal consistency. Findings are organized below by the task's own mandatory challenges, since they subsume and sharpen the ten criteria.

## 5. Unit-designation review (Mandatory Challenge 1)

Independently confirmed: the Unit 2 Baseline Characterisation Scope Lock's §13 states "Unit 3—Reliability Contract and Remedy Selection—receives immutable evidence only," reserving that name for remedy-selection work. "Unit 2-D" is therefore constitutionally preferable to "Unit 3" — using the latter would misrepresent this work as the reserved remedy unit.

Residual ambiguity risk, genuinely tested rather than dismissed: the bare token "2-D," quoted without its surrounding sentence, could plausibly be misread by a future, less-informed reader as "Unit 2, Part D" — i.e., a subordinate phase of Unit 2 itself rather than a distinct sibling unit. The Scope Lock's own §3 text disambiguates this explicitly ("Not 'Unit 3.'... It sits between Unit 2 (closed, adverse, truncated at Stage 0) and Unit 3 (not started)"), and §2 states plainly that Unit 2's closure is not reopened. This review finds the substance adequate — the disambiguating text exists and is unambiguous in context — but records this as a **non-blocking naming-clarity qualification**: any future document or informal reference to this unit should use the full designation with its parenthetical status, not the bare "2-D," to avoid exactly this misreading outside the frozen text's own context.

## 6. DQ1–DQ6 review (Mandatory Challenge 2)

| DQ | Uncertainty resolved | Decision-critical? | Evidence sufficient to answer it? | Framed too broadly? | Duplicates another? | Drifts into remedy selection? |
|---|---|---|---|---|---|---|
| DQ1 repeatability | near-deterministic vs. stochastic REMEMBER miss | Yes — reframes the remedy problem entirely depending on the answer | Yes, for the narrow question it asks (Section 8 below) | No — one fixture/profile/model, tightly pinned | No | No |
| DQ2 breadth | is the miss REMEMBER-specific or protocol-wide | Yes — determines whether a future remedy should be narrow or general | Only at the coarse "any comparable miss at all" level (n=1/fixture); honestly scoped, not oversold | No — exactly one fixture per remaining action family | No | No |
| DQ3 context | does context change the outcome | Weak — the least decision-critical of the six, as the prior Diagnostic Planning Review itself already tiered it | Only weakly (single attempt, explicitly must be read jointly with DQ1) | No — two profiles, one fixture | No | No |
| DQ4 model specificity | model-specific vs. protocol-general contribution | Yes, as part of the joint evidence set, not alone | Only directionally (n=1), honestly hedged | No — one fixture, one model pairing | No | No |
| DQ5 coupling | decision/rendering coupling contribution | Moderate — informs whether task decomposition is worth investigating later | Suggestively, not causally-cleanly (Section 10 below) | No — one variant, one fixture | No | Closest to the boundary; addressed at length in Section 10 |
| DQ6 representation independence | whether the two axes vary independently across this unit's own data, not just PF01's single instance | Modest — largely confirmatory of what PF01 already demonstrated in one trial; costs zero additional calls | Only if representation failures actually occur, which is expected to be rare given Unit 1/2's prior reliability at that axis | No | Partially overlaps with what PF01 already established, but at zero marginal cost this is not a defect | No |

DQ5 and DQ6 were specifically tested for remaining diagnostic, as required. DQ6 remains diagnostic and free — its main weakness is low expected information yield, not scope creep. DQ5 is examined in full in Section 10; this review's conclusion there is that it remains on the diagnostic side of the boundary, closely but not disqualifyingly.

## 7. Independent 24-call reconstruction (Mandatory Challenge 3)

Reconstructed directly from Scope Lock §18, not accepted on the document's own arithmetic summary:

```text
DQ1 repeatability:  R01-direct × minimal-production-context × Qwen  × 10 attempts = 10
DQ2 breadth:        {P01, P06, G01, N01} × minimal-production-context × Qwen × 1  =  4
DQ3 context:        R01-direct × {mixed-full-production-like, conversation-history}
                     × Qwen × 1 attempt each                                      =  2
DQ4 model:           R01-direct × minimal-production-context × Llama × 1          =  1
DQ5 decision-only:   R01-direct (decision-only variant) × Qwen × 5 attempts       =  5
Warm-up:             one per model (Qwen, Llama) × 1                              =  2
                                                                       TOTAL       = 24
```

10 + 4 + 2 + 1 + 5 + 2 = 24. **The arithmetic is confirmed correct.**

Per-class necessity assessment:
- DQ1 (10): necessary at roughly this order of magnitude for the stated triage purpose (Section 8). Fewer (e.g., 5) would materially weaken the "clearly distinguishable from noise" claim §8 makes for a mixed result; more would push toward statistical framing this unit explicitly disclaims.
- DQ2 (4): necessary and minimal — one attempt per remaining action family is the floor for any breadth signal at all; cannot be reduced without losing a whole action family, and there is no support in the evidence for adding more without a repeatability dimension DQ2 does not attempt.
- DQ3 (2): the softest justification of the six components, consistent with the tiering already applied in the antecedent planning review; defensible at its trivial marginal cost (2 of 24), but the first candidate for removal if the campaign ever needed to shrink. Not a defect as written.
- DQ4 (1): necessary — without it, no cross-model signal exists at any cost; not reducible further without eliminating the comparison outright, and not usefully increased without approaching benchmarking (Section 9).
- DQ5 (5): proportionate to its status as the secondary, most experimental axis; reasoned as half of DQ1's count rather than an independent invention (Section 8's own text). Acceptable.
- Warm-up (2): produces no evidence toward any DQ and is not claimed to. Purely infrastructure sanity, mirroring Unit 1/2's own warm-up convention (three warm-ups there, one per model here — proportionate to a two-model, much smaller campaign). No contamination risk: warm-up responses are structurally excluded from every DQ's analysis, exactly as Unit 2's warm-ups were excluded from scoring.

```text
CAMPAIGN SIZE:
ACCEPT WITH QUALIFICATION
```

Accepted: the total, the arithmetic, and every component's individual sizing rationale. Qualification: DQ3 remains the weakest-justified component of the six and should be the first candidate for reduction if the future Implementation/Execution Plan faces resource pressure — this is a standing note, not a required change.

## 8. Repetition analysis (Mandatory Challenge 4)

Independently tested, not merely accepted: what does each possible DQ1 outcome license?

- **10/10 or 0/10 (uniform).** Legitimately supports near-deterministic behavior for this exact fixture/config — ten independent trials landing entirely on one side is a strong signal by ordinary reasoning, even without formal confidence bounds. Does **not** license "REMEMBER always fails" (one fixture, one context, one commit) or a specific rate.
- **9/10 or 1/10.** Still strongly suggestive of near-determinism with one outlier — legitimately interpretable as "predominantly one way," not proof of a precise 90% rate.
- **5/10.** The genuinely hard case, and the Scope Lock's own §8 already concedes this class of result honestly: it supports "stochastic variation is a material contributor" but licenses no numeric rate claim. This review agrees that is the correct, disciplined reading and that nothing in the document tempts a reader toward "the failure rate is 50%."

The document's interpretation rules (§21, rules 1–2) explicitly state both directions of this limitation before any data exists — pre-registered, not written after seeing a result. This review finds the claim that these are "triage-grade, not population-estimation" sample sizes intellectually defensible: the sizing is explicitly justified against what a triage distinction requires (Section 8's own text), not asserted without reasoning, and the interpretation rules correctly refuse to let any outcome, including the ambiguous 5/10 case, be read as a rate. No invented statistical precision was found anywhere in either document.

## 9. Llama comparison determination (Mandatory Challenge 5)

**Case for exclusion**, argued at full strength: one attempt provides no repeatability evidence of its own; `llama3.2:3b` and `qwen2.5-coder:7b` differ in both size and specialization simultaneously, so no clean causal attribution is available; a result inviting "Llama did better" is only one loose interpretive discipline away from becoming an informal model recommendation, and Parker's own governance culture throughout this session shows informal conclusions tend to calcify into institutional assumptions even when a document technically disclaims them.

**Case for inclusion**, argued at full strength: the marginal cost is one HTTP call against an already-configured, already-installed model — effectively free; without it, DQ4's entire model-specific-vs-protocol-general distinction is unanswerable at any cost, permanently, since Unit 2's own closed campaign cannot supply it either; the interpretation rules (§21 rules 3–4) are pre-registered, specific, and directly name the exact overclaims ("Llama is better," "switch models") this comparison risks and forbid them by name, not by vague caution.

Independently weighing these: the inclusion case is stronger. The confound is real but is disclosed, not hidden (§9's explicit limitation paragraph), and the cost asymmetry — one call versus a permanent evidentiary gap — favors inclusion. The exclusion case's strongest point (informal conclusions calcifying despite disclaimers) is a real, general risk that applies to interpretation discipline everywhere in this Scope Lock, not specifically to the Llama comparison, and is already recorded as a standing limitation in this review (Section 17).

```text
LLAMA COMPARISON:
KEEP
```

## 10. DQ5 decision-only variant determination (Mandatory Challenge 6)

This is treated as the highest-risk boundary in the document, as instructed, and is tested most adversarially of all findings here.

**Is changing the prompt changing the system under diagnosis?** Yes, unambiguously, and the Scope Lock concedes this itself (§16: the decision-only variant "must be permanently labeled non-production"). DQ5 does not characterize the currently deployed protocol; it characterizes a hypothesis about a different, not-yet-existing task shape.

**What precise causal question does it answer?** Whether separating the semantic-decision task from the response-generation task changes semantic accuracy for this fixture, under the pinned Qwen configuration — a real, dimension-I-relevant question the Scope Lock's own §6 non-goals correctly identify as "evidence relevant to" architectural judgment, not a resolution of it.

**Does it isolate coupling cleanly?** No, and this review does not accept the Scope Lock's framing at face value on this point: constructing a decision-only task is not a controlled ablation of "remove rendering, hold everything else constant" — it is a different task, with its own instruction-following demands, that may differ from the joint task for reasons unrelated to coupling. §21 rule 6 is appropriately modest about this ("suggestive," not "dispositive," in this review's own words matching the Scope Lock's actual hedged language) — but the underlying epistemic weakness is real and is best understood as *inherent to what any single-prompt experiment of this kind can show*, not a drafting flaw.

**Could the same question be answered without modifying prompt instructions?** No — this review finds no way to test decision/rendering coupling without constructing some form of decision-only task; the causal question is inseparable from prompt variation by its nature. That confirms the risk rather than resolving it.

**Would success on the variant tempt an unauthorized "prompt rewriting is the remedy" conclusion?** This is the genuine open risk. §21 rule 6 and §16 explicitly forbid reading DQ5 as "the fix" or a viable production design, and the Remedy Firewall (§23) names "prompt rewriting" as a firewalled category regardless of DQ5's outcome. But — as this review already found independently for the Llama-benchmarking risk (Section 9) and notes again here — this protection is **procedural, not structural**: nothing in §18's schedule or §20's stop conditions would prevent a favorable DQ5 result from being informally treated, outside the campaign's own artifacts, as validating task decomposition before Unit 3 does its own independent remedy-comparison work. The Scope Lock's exit criteria (§22) require the interpretation worksheet to be reviewed for integrity but do not specifically require certifying that DQ5's result was not informally characterized as a production recommendation anywhere in surrounding communication.

**Is five attempts sufficient for the narrow inference claimed?** Yes, for the modest, comparison-only claim §21 rule 6 actually licenses ("materially different accuracy... supports coupling being a real contributor") — proportionate to its status as a secondary axis, per the same triage-grade reasoning already accepted in Section 8.

**Does this belong in Unit 2-D or the later Remedy Selection/comparison phase?** On balance, Unit 2-D — a diagnostic unit that could not investigate dimension I at all would leave Unit 3 with strictly less information at strictly higher future cost (a separate campaign would need its own full governance cycle to ask exactly this question later, with no cost advantage over asking it now under an already-open, already-reviewed Scope Lock). Deferring it entirely is not free; it is a real, avoidable loss of cheap evidence.

```text
DQ5 DECISION-ONLY VARIANT:
KEEP
```

**Non-blocking qualification (not a blocking defect):** the future Implementation/Execution Plan or the exit-review contemplated by §22 should explicitly require the reviewing Independent Constitutional Review to certify that DQ5's result was not, in fact, informally treated as validating a production design anywhere in the campaign's surrounding record — closing the gap between §21's textual prohibition and actual practice. This review does not find the current text defective enough to block freeze over; it finds the protection real but incomplete, and records the gap rather than silently trusting the text to enforce itself.

## 11. Structured-output boundary determination (Mandatory Challenge 7)

Independently re-derived, not accepted from the Scope Lock's own conclusion: PF01's accepted facts are semantic correctness FAILED, representation correctness PASSED. Structured/schema-constrained output is a representation-axis intervention. Therefore it has no established defect to address for the one failure this whole diagnostic unit exists to investigate, and including it inside Unit 2-D would risk exactly the error the task warns against — a syntactically valid but semantically wrong schema-constrained output being misread as progress. §15's "C — nowhere yet" is independently correct.

Checked separately, as instructed, for quiet bias in the *other* direction: does the Scope Lock foreclose structured output as a future remedy, rather than merely excluding it from this unit? Re-reading §15's closing sentence: "If it is ever evaluated, that belongs strictly to a properly, separately governed remedy-comparison unit under Unit 3." This leaves the door open; it does not say structured output is unnecessary or wrong, only that this unit is not where it is tested. No bias in either direction was found. This is one of the strongest sections of the document.

## 12. Context-control determination (Mandatory Challenge 8)

Independently confirmed necessary at the margin (§10's two profiles), sufficiently controlled (exactly one fixture crossed with exactly two profiles, no crossing with the other four fixture classes or with Llama — this review verified this restriction is actually written into §10's text, not merely claimed), and correctly flagged as weak, single-attempt evidence that "cannot, by itself, be attributed to context" without joint reading against DQ1 — the Scope Lock states this limitation itself rather than this review having to supply it. Repetition was considered by the Scope Lock and rejected as disproportionate (§8); this review agrees expanding DQ3's repetition would meaningfully grow the campaign for the least decision-critical of the six questions, and does not recommend it. No unjustified combinatorial expansion was found.

## 13. Inference-control determination (Mandatory Challenge 9)

Independently re-verified by direct source inspection (Section 3 above): `defaultOllamaRequestBody` in `src/runtime/ModelInferenceClient.kt` sets exactly `model`, `prompt`, `stream:false` — the Scope Lock's factual claim is correct, not merely asserted. Preserving this exact shape unchanged for DQ1–DQ4 is the only choice consistent with the unit's own diagnostic premise (characterizing the actually-deployed protocol); changing it would answer a different, unauthorized question.

Conclusions this review finds prohibited by the unpinned configuration, stated more explicitly than the Scope Lock's own text: no DQ1 result may be characterized as reflecting a fixed, controllable failure rate, since the effective sampling behavior producing any variation is itself unmeasured and potentially version-dependent; no repeatability finding may be assumed to transfer to a differently configured or differently versioned deployment; and no claim may be made about *why* variation occurs (sampling-parameter-driven versus genuine model uncertainty) since the two are conflated by the absence of pinned parameters. No configuration value was invented anywhere in this review or in the Scope Lock.

## 14. Stop-condition review (Mandatory Challenge 10)

Independently re-derived rationale, not merely restated: Unit 2's Stage 0 gate protects entry into a 3,900-trial statistical commitment whose validity depends on a proven instrument — the cost of proceeding on a broken instrument there is large and irreversible in effect (contaminating a large, expensive dataset), which justifies halting on the first anomaly. Unit 2-D carries no downstream statistical commitment of that kind; its entire purpose is characterizing failure, including repeated failure, at a scale where the "cost of proceeding" on a genuine finding is exactly zero — proceeding *is* the finding. Treating fail-closed governance as a single fixed trigger condition rather than the same underlying discipline (halt when evidence validity or safety is actually threatened) applied to what is actually at stake in each context would be the constitutional error, not the Scope Lock's differentiated approach.

Confirmed independently that hard stops remain intact for exactly the six items the task requires: identity mismatch, configuration mismatch, harness/measurement defect, artifact-integrity defect, unauthorized consequential action, and (implicitly, via §2/§17's explicit non-reopening language and prohibition list) governance-boundary violation. Confirmed that nothing in §20 alters, weakens, or reinterprets Unit 2's own historical fail-closed determination — Unit 2's gate is untouched; a new, separately justified gate is defined for a separate unit.

## 15. Interpretation-rule review (Mandatory Challenge 11)

Checked against every prohibited claim named in the task: "Qwen is unsuitable" — not licensed by any rule (rules 1–2 stay fixture/config-specific). "Llama is better" — explicitly barred (rule 3). "The prompt is defective" — not licensed; rule 4's protocol-contribution language is explicitly hedged against being "*the* cause." "Structured output is unnecessary" — not claimed; §15 explicitly leaves this open for Unit 3. "Deterministic routing is required" — not claimed; deferred to Unit 3 by §6's non-goals. "Model replacement is required" — barred by rules 3–4 and the firewall (§23). "A particular observed percentage is a population failure probability" — explicitly and repeatedly barred (rules 1, 2, and Section 8 above). Every one of the task's named overclaims is guarded against by name or by direct implication. Tiny-cell results (DQ3, DQ4) are consistently described only as directional/weak signal throughout §21, never elevated to confirmed findings.

## 16. Exit-criteria review (Mandatory Challenge 12)

Independently confirmed §22 does not require all six DQs to reach a decisive result — only that each "have a recorded result (whichever way each comes out)." A failed or inconclusive individual DQ does not block closure; only incomplete data collection or unresolved integrity questions do. This review finds that correct: the exit bar is appropriately about completeness and integrity of the pre-registered evidence set, not about achieving any particular finding. §22's own text ("that insufficiency is itself a valid, recordable finding") functionally provides an "inconclusive but diagnostically complete" outcome, though it does not use that exact phrase. Non-blocking suggestion: a future revision could adopt that phrase explicitly as a named status for clarity; not required for freeze.

## 17. Remedy-firewall review

Independently confirmed §23's twelve-item list is closed and named, not illustrative, and is cross-checked against every candidate remedy raised elsewhere in the document (structured output, prompt rewriting including DQ5's variant, model replacement including the Llama comparison) — each is explicitly named in the firewall. No item discussed diagnostically anywhere in the document is absent from the firewall. The firewall's actual enforcement, as already noted in Sections 9 and 10, depends on the interpretation worksheet and the exit-time Independent Constitutional Review actually being held to it — a procedural, not mechanical, guarantee, consistent with how Unit 2's own equivalent protections operated throughout this session.

## 18. Campaign-isolation review

Independently confirmed §17's isolation rules are complete against the task's own checklist: new campaign identity with a mandatory `diagnostic` marker; new artifact root explicitly outside `qwen25coder7b-baseline-20260809`'s directory; pinned commit, both model digests, endpoint, runtime, container identity, timeout; per-observation recording of prompt/input, raw envelope, extracted response, parser result, semantic classification, representation classification, content fidelity, and latency/transport metadata; reuse of Unit 1's existing serialization shapes. Independently re-verified on the live host during this review (Section 19): the existing `qwen25coder7b-baseline-20260809` campaign remains completely untouched, and no diagnostic campaign directory of any kind exists yet — consistent with §17's own statement that it "does not create that campaign identity."

## 19. Independent artifact and repository verification

Read-only, during this review: `/var/lib/parker/reasoning-protocol-live-model/qwen25coder7b-baseline-20260809/stage-0.failed`, `manifest.txt`, and `raw.jsonl` hashes recomputed and found identical to every prior check in this session's record; `stage-0.sealed` absent; no `stage-1/`, `stage-2/`, or diagnostic campaign directory exists anywhere under the artifact root. `git status` before and after this review's own document creation shows only the two pre-existing untracked governance documents plus, after this review, this one new file — no other change. Both reviewed documents' SHA-256 hashes, recorded before this review began, are unchanged (Section 24).

## 20. Strongest case for acceptance

The Scope Lock is finite (five fixtures, six questions, a fixed 24-call schedule with no expansion mechanism), genuinely diagnostic (every candidate remedy is named and firewalled, not merely gestured at), proportionate (two orders of magnitude smaller than Unit 2, sized from its own questions rather than inherited corpus size), does not reopen Unit 2 (independently reverified on the live host, not merely asserted), preserves fail-closed governance applied to what is actually at stake in a much smaller unit, and — unusually for a first-draft governance document in this repository's own history — pre-empts nearly every adversarial attack the task itself specifies, including ones (confounded model comparison, non-generalizing Qwen-only findings, weak single-attempt context evidence) that a less careful drafter would have glossed over. Its self-awareness of its own limitations is a genuine strength, independently verified rather than merely asserted by the document itself.

## 21. Strongest case against acceptance

Three internal cross-references are independently verified as incorrect (Section 22): §8 cites "Section 15" for content that is actually in Section 21; §10 cites "Section 21, Point 6" for combinatorial-growth reasoning that Section 21 Point 6 does not contain (it is about DQ5's Qwen-only scope); §21 rule 6 cites "Section 9's DQ5 scope," but Section 9 is entirely about the Llama comparison (DQ4) and never mentions DQ5. A document intended to become frozen governance — where correcting even a citation later requires the same amendment machinery this repository has applied to substantive defects throughout this session — arguably should not freeze with verified, findable errors of this kind, however narrow. Separately, DQ5's remedy-adjacency risk (Section 10), while judged here as not disqualifying, is real, and a stricter reviewer could reasonably require its exit-time certification gap to be closed by textual amendment before freeze rather than carried forward as a qualification.

## 22. Blocking defects

None. No finding in this review rises to a defect that changes what the Scope Lock actually authorizes, forbids, or protects against. The three cross-reference errors identified in Section 21 are independently confirmed as citation-only: in every case, the substantive content the citation was meant to point to exists correctly elsewhere in the document (single-attempt limitations are in fact stated in §21 rules 3, 4, and 7; combinatorial-growth reasoning is in fact stated inline in §10 itself; DQ5's Qwen-only scope is in fact stated in §5 and §16). Nothing is missing; three pointers are wrong. This review classifies citation-only errors, where the underlying substance is present and correct, as non-blocking.

## 23. Non-blocking qualifications

1. Three internal cross-reference errors, independently verified (Section 21), should be corrected: `§8` → cite Section 21, not Section 15; `§10` → cite Section 10's own inline reasoning or remove the parenthetical, not "Section 21, Point 6"; `§21 rule 6` → cite Sections 5/16, not Section 9. (Note: this same "Section 9's DQ5 scope" error also appears, uncorrected, in the prior Scope Lock Review's own adversarial-attack §4 point 10 — that review did not independently verify this cross-reference either; this finding was only surfaced by this review's own direct re-reading against the Scope Lock's actual section contents.)
2. The bare designation "Unit 2-D," quoted outside its own §3 context, carries a residual risk of being misread as a subordinate phase of Unit 2 rather than a distinct sibling unit (Section 5); future references should carry the full designation with its disambiguating clause.
3. DQ5's remedy-adjacency protection is procedural, not structural (Section 10); the future Implementation/Execution Plan or exit review should explicitly require certifying that DQ5's result was not informally treated as a production recommendation anywhere in the campaign's surrounding record.
4. DQ3 (context) remains the least decision-critical and most cuttable of the six questions (Section 7); no change required, but it is the correct first candidate if the campaign ever needs to shrink.
5. §22's "insufficiency is itself a valid, recordable finding" language functionally provides an inconclusive-but-complete exit outcome but does not name it as such (Section 16); a named status would improve clarity without changing substance.

## 24. Required amendments

None required as a precondition to freeze. The qualifications in Section 23 are recommended corrections, appropriate to apply at the next natural editorial opportunity (for example, when this review's findings are otherwise being incorporated into the record), but none of them changes what the Scope Lock substantively authorizes, forbids, sizes, or protects against, and this review does not require a further amendment-and-re-review cycle before the Scope Lock may be treated as frozen governance.

## 25. Final constitutional verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

The proposed Scope Lock is ready to freeze and commit as governance for Unit 2-D, carrying forward the five non-blocking qualifications in Section 23. Implementation/Execution Planning for Unit 2-D may begin only after that freeze — this review is the Independent Constitutional Review the Scope Lock's own §25 names as the first required gate, not a substitute for the Implementation/Execution Plan, its own Independent Constitutional Review, the Implementation Readiness Review and its Independent Constitutional Review, or explicit execution approval, all of which remain required and unauthorized by this document. Live execution of any kind remains unauthorized.

## 26. Exact next authorized step

Freezing and committing the Scope Lock text exactly as currently written (the qualifications in Section 23 are recommended, not required, corrections) is the next available governance action, followed — only after that freeze — by drafting an Implementation/Execution Plan scoped strictly to Scope Lock §18's twenty-four-call schedule. Neither is performed, authorized, or initiated by this review.

## 27. Confirmation

No model or HTTP call occurred during this review. No `/api/generate`, `/api/tags`, or `/api/show` call occurred. The Unit 2 campaign artifacts (`qwen25coder7b-baseline-20260809`) remain byte-identical to every prior verification in this session. Both reviewed documents (the Scope Lock and its prior Review) remain byte-identical to their pre-review SHA-256 hashes, recorded before this review began. No production, test, or Gradle file changed. Nothing was staged, committed, or pushed.
