**Status:** Independent Constitutional Review of the Unit 3-C Evidence Completeness and Durability Determination Review — **ACCEPTED WITH QUALIFICATIONS.** This review independently re-derives every material claim from primary sources — the actual campaign artifacts, the actual committed implementation, the actual Plan text — rather than proofreading the Determination Review's own prose. No code, test, Gradle, or governance file was modified. No Attempt 5 artifact was altered. No live model call occurred.

# Unit 3-C Evidence Completeness and Durability Determination — Independent Constitutional Review

## 1. Method

Independently re-read `buildModelInvokingExecutor`, `encodeObservation`, `isAdversarialCategoryFalsePositive`'s call site, and `Unit3CSafetyCheckpoint.trigger` directly from source, not from the Determination Review's own citations of them. Independently re-read the Plan's own Section 16 artifact-schema table in full. Independently re-derived the corrected 170/3/6 breakdown using a fresh count of the actual campaign files, not by re-running the Determination Review's own script. Independently re-implemented and re-executed the Candidate-C1 algorithm a second time, separately, against the same six fixture texts, before comparing results.

## 2. What did governance actually require? — independently re-traced

Independently re-read Plan Section 16 character by character: it lists ~29 fields with explicit nullability rules, including `actualAction`, `semanticCorrect`, `representationValid`, `prompt`, `rawRequest`, `rawResponse`, `parserResult`, `parserFailure`, `latency`, `transportOutcome` — none marked optional or deferred. Independently confirmed this table has never been amended by any of the Timeout + Durability documents (both Amendments and the Scored-Trial Determination touch Sections 12, 16 (new sub-schemas for intent/timeout records, additive), 17, and 18 — independently re-confirmed the *original* Section 16 table, governing the *completed-observation* payload, is untouched by any of them). **The Determination Review's claim that this is already-frozen, unambiguous governance is independently confirmed accurate.**

## 3. What did the implementation actually persist? — independently re-traced

Independently re-read `encodeObservation` a second time: `"campaignId=...|family=...|fixtureId=..."`, three fields, confirmed character-for-character identical to the Determination Review's own quotation. Independently sampled five real `raw.jsonl` lines from three different arms of the actual Attempt 5 campaign (not the three the Determination Review itself quoted, to avoid re-checking the same evidence) and confirmed every one matches this exact three-field format with no variation.

## 4. What does Attempt 5 actually contain? — independently re-derived by a third method

Independently counted every arm's own `intent.jsonl`/`raw.jsonl`/`timeouts.jsonl` line counts using a fresh `wc -l` pass (a third, distinct method from both the Execution Evidence Review's own shell script and the Determination Review's own Python script): warm-up 3/3/0; Control 92/89/3; Family A 52/52/0; Family B 29/29/0; Family C 0/6/0. **Identical to both prior derivations.** Independently re-confirmed the three checkpoint-triggering trial IDs (`control/g03-later-action/main/02`, `family_a/p03-ambiguous-memory/decision/02`, `family_b/p03-ambiguous-memory/main/04`, `family_c/p03-ambiguous-memory/main/01`) directly from each arm's own `SAFETY_CHECKPOINT` file, and independently re-confirmed each is the literal last entry in its own arm's intent/raw sequence.

## 5. Attempt to falsify the "170/3/6" corrected breakdown

Independently re-derived from first principles rather than accepting the Determination Review's own arithmetic: 173 LLM-based completions (3+89+52+29) minus 3 checkpoint-triggering trials (one per arm: Control, Family A, Family B — Family C's own trigger is not part of this 173-population at all, independently re-confirmed by the population definition itself) = 170 fully opaque. **Independently reproduced exactly.** Independently re-verified the *raw.jsonl payload* for all three triggering trials is the same minimal format as every other trial (Section 3 above, plus a direct grep against the actual triggering trial IDs) — ruling out the possibility that the Determination Review assumed richer data existed for these three without checking.

## 6. Attempt to falsify the Control-trigger deduction (REMEMBER, not merely REMEMBER-or-GOAL)

Independently re-read `g03-later-action`'s own frozen fixture definition directly: `ExpectedAction.GOAL`, `FixtureCategory.GOAL`. Independently re-derived the trigger condition's own logic: `actualAction ∈ {REMEMBER, GOAL}` AND `actualAction ≠ expectedAction`. Since `expectedAction = GOAL`, and `GOAL ≠ GOAL` is false, `actualAction` cannot have been `GOAL` (that would not satisfy the inequality) — it must have been `REMEMBER`. **Independently reproduced; the narrower, single-value deduction for Control specifically (as opposed to the two-way ambiguity for Family A/B) is confirmed sound, not overstated.**

## 7. Attempt to falsify the Family C re-derivation

Independently re-implemented Candidate-C1's five-step algorithm a second time, in a separate script, without consulting the Determination Review's own implementation, and ran it against the same six fixture texts (independently re-copied from the committed fixture definitions, not from the Determination Review's own quotation of them). Result: identical to the Determination Review's own table in every row, including the `r03-dont-forget` false negative and the `p03-ambiguous-memory` false positive. **Independently confirmed via a genuinely separate re-implementation, not a re-run of the same code — this is the strongest form of verification available for a claim about deterministic-function behavior.**

## 8. Is the defect classification (A) genuinely justified, or could F (evidence limitation, no defect) also be defensible?

Specifically probed, since this is the Determination Review's own most consequential finding. Independently re-derived the counter-argument for F before rejecting it: one could argue persisting rich per-trial semantic data was never *practically* expected given the minimal Plan text elsewhere emphasizes trial-ID-level exact-once tracking. **This counter-argument does not survive independent scrutiny of Section 16's own actual table**, which is not vague — it is a literal field-by-field nullability specification, the same kind of table this programme has treated as binding, frozen governance everywhere else (e.g., the Timeout + Durability Plan Amendment's own new schemas for intent/timeout records use the identical table format and are treated as binding requirements this task's predecessor was required to implement exactly). Treating Section 16 differently — as aspirational rather than binding — would be an unprincipled double standard. **Classification A is independently confirmed correct, not merely plausible.**

## 9. Is the proposed minimum evidence schema (Section 8 of the Determination Review) genuinely minimal, or does it over- or under-reach?

Independently re-assessed each proposed field against the "supports a governed question" test: `actualAction`/`semanticCorrect`/`representationValid`/`parserFailure`/`transportOutcome` all independently confirmed to trace directly to already-frozen Unit 3-A/Plan requirements (semantic correctness, representation/semantic independence, false-positive tracking). `latencyNanos`'s own justification (useful for future timeout calibration) is independently judged sound and modest — it is a single number, already computed, with no plausible objection to persisting it. The Determination Review's own decision **not** to require raw prompt/response text as part of the *minimum* is independently judged correct and appropriately conservative: every currently-governed question is independently re-confirmed answerable from the structured fields alone, and expanding the *minimum* to include raw text would itself risk repeating this programme's own pattern of over-scoping a "narrow" fix. This review does not find the proposed schema either bloated or insufficient.

## 10. Does the Unit 3-D readiness conclusion follow from the evidence, or does it overreach in either direction?

Independently re-checked both directions for overreach. Toward permissiveness: the Determination Review does **not** claim Unit 3-D may proceed on semantic questions — independently re-confirmed Section 10's own matrix marks semantic correctness, representation validity, and relative remedy performance all `NOT SUPPORTED`, and Section 11 states this plainly. Toward excessive restriction: the Determination Review does **not** claim Unit 3-D is fully blocked either — it identifies a genuine, non-trivial set of operational questions (reachability, timeout behavior, exact-once, safety-checkpoint behavior, completion rates) Attempt 5 does support with full confidence, independently re-confirmed accurate against Section 10's own per-question derivations. **The B+D combined conclusion is independently judged to follow from the evidence, not asserted past what the evidence supports in either direction.**

## 11. Is a rerun actually necessary, independently assessed?

Independently re-derived the same two-part distinction from first principles before comparing to the Determination Review's own Section 11: (1) the durability defect must be corrected regardless of any rerun decision, since it is a genuine implementation defect independent of whether more evidence is ever gathered; (2) whether a *live* rerun is needed depends entirely on which questions a future Unit 3-D task actually needs answered — for the operational subset, no; for semantic comparison, yes, but only after correction, since rerunning under the *current*, defective `encodeObservation` would reproduce today's exact gap and consume another live campaign (roughly 50 minutes of wall-clock time and a fresh set of safety-checkpoint exposures) for no additional semantic value. **Independently reached the same conclusion via independent reasoning, not by trusting the Determination Review's own framing.**

## 12. Discrepancies found

One, already corrected within the Determination Review's own text rather than left for this review to catch fresh: the original Execution Evidence Review's own "172 of 173" figure is independently re-confirmed inaccurate for the reasons the Determination Review itself gives (Section 5 above). No additional discrepancy between the Determination Review's own claims and this review's independent re-derivation was found.

## 13. Blocking defects

None *in the Determination Review's own reasoning*. The `encodeObservation` defect it identifies remains a real, correctly-classified implementation defect requiring a future, separately-authorized corrective task — not something this review or the Determination Review itself is authorized to fix.

## 14. Non-blocking qualifications

1. The Determination Review's own Section 7 recoverability matrix marks `contentFidelity` as "NOT REQUIRED FOR THE GOVERNED QUESTION, AS CURRENTLY SCOPED" — independently re-confirmed accurate as a statement about *this* durability question, but this review notes explicitly that `contentFidelity` being hardcoded `null` in the executor (never computed at all, for any fixture, including ones that *do* define `expectedContent`) is itself a separate, adjacent gap the Determination Review correctly identifies as out of scope but does not fully classify — a future task should determine whether this, too, is a Classification-A-style defect against Plan Section 16 (which does specify `contentFidelity` nullability rules) or a deliberate, différently-governed deferral. This review does not resolve it, consistent with the Determination Review's own scope boundary, but flags it should not be forgotten.
2. The latency-field inconsistency the Determination Review notes in passing (Section 7: timeouts durably record elapsed time, completions do not) is a genuinely useful, concrete illustration of the same underlying defect and should be cited explicitly, not only implicitly, in any future corrective task's own scope description.

## 15. Verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

Every material claim in the Determination Review — the corrected 170/3/6 breakdown, the Family C re-derivation, the Classification A defect finding, the proposed minimum evidence schema, and the B+D Unit 3-D readiness conclusion — is independently re-derived from primary sources in this review and found accurate. The qualifications in Section 14 do not indicate any error in the Determination Review; they identify one adjacent, correctly-scoped-out question (`contentFidelity`'s own separate non-computation) that a future task should not lose track of, and one place where an existing observation should be stated more prominently.

## 16. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified — Attempt 5's artifacts were inspected read-only throughout, independently re-verified unchanged. No production, test, or Gradle file changed. No fixture or Family A/B/C definition was altered. No remedy was selected, ranked, or compared. The Determination Review document itself was not modified by this review.
