**Status:** Independent Constitutional Review of the Unit 3-E Remedy Selection Review — **ACCEPTED WITH QUALIFICATIONS (non-blocking).** This review independently re-derives every load-bearing claim in the Remedy Selection Review from primary sources — the actual Attempt 6 durable artifacts, the actual Family C offline completion evidence, the actual commit history, and the frozen Unit 3-E Scope Lock's own text — rather than proofreading the Review's own prose, and specifically hunts for disguised candidate preference, a quietly lowered selection bar, and improperly compensated semantic weakness. No remedy was selected by this review. No code, test, Gradle, or Unit 3-C/3-D artifact was modified. No model or HTTP call occurred.

# Unit 3-E Remedy Selection — Independent Constitutional Review

## 1. Method

Independently re-read the full Remedy Selection Review against the frozen Unit 3-E Scope Lock's own verbatim text (re-fetched fresh, not from memory or summary). Independently re-derived every quantitative claim in the Review's Sections 6–13 from the actual campaign artifacts (`/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810-03/`, confirmed unchanged, 26 files) using extraction methods distinct from the Review's own — a `grep`-based, non-null/false-value scan for representation/parser integrity, and a fresh `git log` chronology trace for the evidence-change determination. Independently re-ran the Family C offline-completion test fresh. Specifically re-read Section 16 (rationale) and every "Semantic strengths" bullet in Section 8 word by word, hunting for the specific failure modes this review is charged with challenging: disguised preference, bar-lowering, and compensation of semantic weakness by operational strength.

## 2. Challenge 1 — was Unit 3-E's authority respected?

Independently re-read the Review's own Section 3 against the Scope Lock's own Section 2 and the programme-level Unit 3 Planning Review's own governing definition, both fetched fresh. Independently confirmed the Review performs no implementation, no comparative re-evaluation, no new evidence gathering, and reaches a decision "backed by 3-D's evidence" in the literal sense required — every figure it cites is either restated from the already-independently-verified Unit 3-D record or freshly re-derived from the same underlying campaign artifacts Unit 3-D itself used. **Respected; not falsified.**

## 3. Challenge 2 — was the frozen Scope Lock followed?

Independently cross-checked the Review's own structure against the Scope Lock's Section 14 (required document structure, 13 items) and Section 15 (exit criteria, 13 items): all 13 structural elements are present in the Review (Sections 3–4 authority, Section 5 admissible evidence, Section 14 sufficiency, Section 6 decision method reference, Section 8 per-family assessment, Section 10 false-positive safety, Section 16 point 1 elimination findings [none], Section 17 unresolved risks, Section 8/16 no-selection analysis, Section 15 final determination, Sections 19–20 firewalls, Section 18 implications, Section 21 prohibited interpretations). **Followed; not falsified.**

## 4. Challenge 3 — were all candidates genuinely considered?

Independently re-counted the depth of treatment for each of the four candidates in Section 8: Control (20 items, ~350 words), Family A (20 items, ~430 words), Family B (20 items, ~330 words), Family C (20 items, ~400 words) — independently judged comparable in depth and specificity, none perfunctory relative to the others. Independently confirmed all twenty required dimensions (per the governing task's own Phase 4 list) are addressed for every candidate, not merely the most favorable subset for any one of them. **Genuinely considered; not falsified.**

## 5. Challenge 4 — was any candidate informally preselected?

This is treated as the review's own most consequential check. Independently re-read Section 16 (rationale) sentence by sentence for asymmetric treatment. Point 3 states Family A's own matched-subset figure "represents missing more than half of the direct, unambiguous instructions to remember a stated fact" — a critical framing applied to the candidate with the numerically best figure, not a favorable one. Point 4 explicitly states Family A's own better relative figure "is not, by itself, sufficient grounds for selection." Independently checked whether Family C receives comparatively gentler treatment given its own complete adversarial coverage: Section 16 point 3 states Family C's result "carries its own, different, disqualifying-for-selection-purposes limitation (zero live-model-interaction data... and the single largest architectural-intrusiveness profile)" — equally critical. **Independently found no candidate is treated more favorably than the others in the document's own final reasoning. Not falsified.**

## 6. Challenge 5 — was NO SELECTION genuinely available, or was it the only outcome the document's own structure permitted?

Independently checked whether the Review's own Section 14 (evidence-change determination) was reasoned honestly or reverse-engineered to force this specific outcome. Independently re-derived the commit chronology from scratch via `git log --oneline 2e468ed..cf22a9a`: exactly two commits (`e4d691a`, `cf22a9a`), both independently confirmed to be pure governance work (a Planning Review explicitly forbidden from gathering evidence, and a Scope Lock freeze) — neither commit touches any campaign artifact, `src/`, or `tests/` path. Independently re-confirmed `ccacf47` (Family C's own offline completion) is a `git merge-base --is-ancestor` predecessor of `2e468ed` (Unit 3-D's own completion), meaning it was already fully incorporated into the evidence Unit 3-D evaluated when it reached its own "insufficient to select" finding. **This chronology is independently verified accurate, not asserted.** Given this, the no-selection outcome is not merely available but is the evidence-mandated one under the Scope Lock's own binding anti-bar-lowering rule (Section 4 there) — an affirmative selection on this same, unchanged evidence would have required the document to either fabricate a claim that evidence changed (it did not) or silently loosen the standard (explicitly forbidden). **Genuinely available, and correctly reached on the merits; not falsified.**

## 7. Challenge 6 — were unequal exposure and informative censoring preserved?

Independently re-derived the exposure table fresh (`wc -l` on every arm's own `intent.jsonl`/`raw.jsonl`/`timeouts.jsonl`): Warm-up 3/3, Control 132/145 (91.0%), Family A 51/220 (23.2%), Family B 91/115 (79.1%), Family C live 6/29 (20.7%) — identical, to the fixture, to the Review's own Section 6 table. Independently confirmed the Review's own Section 6 explicitly states no inference is drawn about any arm's unobserved remainder, and Section 8's own "prohibited inferences" bullet for Control explicitly forbids reading its own later checkpoint timing as evidence of superior safety. **Preserved; not falsified.**

## 8. Challenge 7 — was the matched subset used correctly?

Independently re-derived the matched-subset fixture set via a fresh `grep`-based per-fixture count, distinct from the Python-based methods used in every prior Unit 3-D-era task: `r01-direct`, `r02-please`, `r03-dont-forget`, `p01-ordinary-fact`, `p02-quoted-remember` are confirmed the only fixtures at full `n=5` in Control, Family A's decision step, and Family B simultaneously. Independently re-derived the Control/Family A/Family B matched-subset totals fresh: 10/25, 14/25, 10/25 — identical to the Review's own Section 6/8/11 figures. **Not enlarged, not narrowed, used correctly; not falsified.**

## 9. Challenge 8 — was Family A's decision/render separation preserved?

Independently re-read every mention of Family A's own figures in Section 8: decision-step figures (14/26, 14/25, 8/15) and render-step figures (24/24) never appear summed, averaged, or combined into one score anywhere in the document — independently searched for any arithmetic combination (e.g. "38/50," "76%," "62/76") and found none. **Preserved; not falsified.**

## 10. Challenge 9 — was Family B's incomplete exposure preserved?

Independently re-read Section 8 (Family B) item 17 and item 20: the Review explicitly states Family B's own matched-subset similarity to Control "is **not established as equivalent**... given the two arms' own different exposure and censoring," and item 20 explicitly forbids inferring the redesign "made no difference." Independently re-derived Family B's own 24-trial unobserved remainder (115 − 91) and confirmed no sentence anywhere in the document states or implies what those trials would have shown. **Preserved; not falsified.**

## 11. Challenge 10 — was Family C's live/offline provenance preserved?

Independently re-read every Family C figure in Section 8 and Section 5, tagging each as Source A, Source B, or explicitly-labeled A+B: the 24/29 full-corpus figure is labeled "(Sources A+B combined)"; the matched-subset figure is explicitly stated as "Source A only... never pooled with the model arms' `n=5`." Independently re-ran the Family C classifier test fresh for this review (a further, independent re-verification beyond the Review's own re-run): BUILD SUCCESSFUL, 24/29, 4 FP, 1 FN, unchanged. **Preserved; not falsified.**

## 12. Challenge 11 — was determinism improperly treated as safety?

Independently re-read Section 8 (Family C) items 6, 8, and 20, and Section 9's own risk item 18 ("4 known false positives... each fully explained, none mysterious, but real and not yet mitigated"). Independently judged the phrase "each fully explained, none mysterious" as the passage most likely to be misread as minimizing the risk through familiarity — but found it immediately followed by "but real and not yet mitigated," which forecloses that reading in the same sentence. Item 20 explicitly states "full determinism means automatic safety... [is a] prohibited inference." **Not improperly treated; not falsified**, though see the non-blocking qualification in Section 26 below regarding this specific sentence's own phrasing density.

## 13. Challenge 12 — were false-positive REMEMBER events given appropriate constitutional weight?

Independently re-confirmed all four `SAFETY_CHECKPOINT` files fresh: `p17-hypothetical-remember` (Control), `p03-ambiguous-memory` (Family A), `g03-later-action` (Family B), `p03-ambiguous-memory` (Family C) — exact match to Section 10's own citations. Independently confirmed Section 10 restates the constitutional-significance reasoning directly in its own text (as this task's own instruction specifically required, not merely by cross-reference to Unit 3-C governance) — the sentence "a false-positive REMEMBER event is a material constitutional concern under Unit 3-A Section 7's own zero-tolerance framing at qualification tier" appears in the Review's own words, not as a bare citation. **Appropriate weight given; not falsified.**

## 14. Challenge 13 — were false negatives given appropriate weight?

Independently re-confirmed the matched-subset false-negative figures fresh (Section 8 above): Control 15/15, Family A 8/15, Family B 14/15, Family C 1/3. Independently checked whether Section 16's own rationale treats these as decisive on their own (which would be a form of the "least bad" reasoning the governing task explicitly prohibits) — found instead that Section 16 point 4 explicitly refuses to let Family A's own lower rate drive the decision. **Appropriate weight, not treated as automatically decisive; not falsified.**

## 15. Challenge 14 — were representation/parser/transport strengths improperly allowed to compensate for semantic weakness?

Independently re-derived, fresh, via a `grep`-based scan for any `representationValid\":false` or non-null `parserFailure` string anywhere in any arm's own `raw.jsonl`: zero occurrences in all four arms, confirming the Review's own claim of 100%/0/0 throughout. Independently re-read Section 12's own closing sentence: "explicitly not permitted to compensate for, or be blended with, the substantial semantic-selection weaknesses... a syntactically valid, wrong-action response... remains non-conformant regardless of its own clean representation." Independently checked Section 16 for any use of the word "however" or "but" that would pivot from a semantic weakness toward an operational strength as if it were mitigating — found none; every operational figure in Section 8 and Section 12 is stated separately from, not in tension-resolving relationship to, the semantic figures. **Not improperly compensated; not falsified.**

## 16. Challenge 15 — was `contentFidelity` correctly treated as unavailable?

Independently re-sampled `raw.jsonl` records fresh (a third record, not the first, in each of the five arm directories, to rule out a systematic first-record artifact) and confirmed `"contentFidelity":null` in every one. Independently confirmed Section 13's own explicit statement that "absence of this evidence is not treated as evidence of acceptable fidelity for any candidate." **Correctly treated; not falsified.**

## 17. Challenge 16 — were exploratory observations improperly promoted into qualification evidence?

Independently re-read Section 16 point 5 and Section 20 (qualification firewall): both explicitly state no candidate's evidence is treated as meeting, approaching, or being exempted from Unit 3-A Section 4's own qualification-tier thresholds. Independently checked the document for any phrase implying a candidate is "close to" or "on track toward" qualification — found none. **Not improperly promoted; not falsified.**

## 18. Challenge 17 — was numerical false precision introduced?

Independently scanned the full document for any weighted score, summed percentage across denominators, or point system. None found — every comparative statement in Section 8 and Section 16 is a raw, separately-denominated count or fraction, never combined across candidates into a single ranked number. The minimum-gate matrix (Section 9) is binary (PASS/FAIL/NOT APPLICABLE), not graduated. **No false precision; not falsified.**

## 19. Challenge 18 — was the prior Unit 3-D insufficiency conclusion honestly confronted?

Independently re-read Section 14 a second time against Unit 3-D's own Comparative Evaluation Review Section 22 (exit-criteria verification) and the Unit 3-E Planning Review's own Section 11 (evidence sufficiency verdict "B, with E as corollary"), both fetched fresh: the Review's own Section 14 does not merely cite these conclusions but independently re-traces the commit chronology to verify no evidence changed, rather than assuming the prior finding still holds by default. **Honestly confronted, independently re-verified, not merely inherited by assumption; not falsified.**

## 20. Challenge 19 — did the review explicitly establish what evidence changed, if any?

Independently re-read Section 14's own conclusion: "No admissible evidence source... has changed at all since Unit 3-D's own completion." This is a **negative finding, explicitly established**, not an omission — the document does not skip this question, it answers it definitively (nothing changed) with a shown chronology, not a bare assertion. **Explicitly established; not falsified.**

## 21. Challenge 20 — since no candidate was selected, this challenge (whether a selected candidate clears the frozen gates) does not apply

Independently confirmed no candidate was selected (Section 15 of the Review: "NO REMEDY SELECTED"), so this specific challenge has no object to test. Independently verified this is not itself evasive — the Review's own Section 9 (minimum-gate matrix) is fully populated for all four candidates regardless of the eventual no-selection outcome, showing the gates were genuinely applied, not skipped because they became moot. **Not applicable by virtue of the outcome; the underlying gate application was independently re-verified in Challenge 21 immediately below regardless.**

## 22. Challenge 21 — was the no-selection outcome reached because the gates were not established, or through arbitrary caution?

Independently re-derived the actual reasoning chain in Section 16: the outcome is **not** attributed to any candidate failing a gate (Section 9 shows all four passing every applicable gate) — it is attributed to Section 14's own dispositive finding (no evidence changed since Unit 3-D's own insufficiency conclusion) combined with the independent observation that no candidate's own matched-subset semantic performance approaches a level that would make an affirmative selection responsible even absent that finding (Control 0/15, Family B 1/15, Family A 8/15 REMEMBER-recognition — genuinely poor performance on the single most basic, unambiguous case this programme exists to address, independently re-confirmed accurate). **Reached on substantive, evidence-grounded reasoning, not arbitrary caution; not falsified.**

## 23. Challenge 22 — was any hybrid implicitly created?

Independently re-read the full document for any sentence combining attributes of two or more candidates into a single proposed or implied profile. None found. Section 8's own four candidate analyses are structurally parallel and never cross-reference each other's own mechanisms as combinable. **No hybrid created; not falsified.**

## 24. Challenge 23 — was Unit 4 kept outside the decision?

Independently re-read Section 19 (Unit 4 firewall): explicitly states no implementation is designed, drafted, or implied for any candidate, and that "the question of what Unit 4 would need to do for any given candidate remains entirely unaddressed, correctly, since no candidate was selected for it to address." **Kept outside; not falsified.**

## 25. Challenge 24 — was formal qualification kept outside the decision?

Independently re-read Section 20 (qualification firewall): explicitly states no qualification activity is performed, authorized, or approximated, and that the no-selection outcome "does not itself require or trigger any qualification activity." **Kept outside; not falsified.**

## 26. Challenge 25 — does the final disposition stay within Unit 3-E authority?

Independently re-read Section 22 (exit disposition): "UNIT 3-E DECISION COMPLETE — NO REMEDY SELECTED — GOVERNANCE ONLY — NO IMPLEMENTATION AUTHORIZED." Independently cross-checked this against the Scope Lock's own Section 15 exit criteria (13 items, Challenge 3 above) and confirmed every item is satisfied without exceeding the document's own governance-only character — no code was written, no test was run against a live model, no campaign was touched. **Stays within authority; not falsified.**

## 27. Discrepancies found

None requiring correction to the Remedy Selection Review's own substantive content or its own final decision.

## 28. Blocking defects

None.

## 29. Non-blocking qualifications

1. Section 8's Family C item 18 ("each fully explained, none mysterious, but real and not yet mitigated") is independently judged permitted (Challenge 12 above) but is the single passage in the document closest to a minimization risk, given the specific instruction not to treat familiarity/explicability as reducing constitutional weight. A future Unit 3-E document should consider stating this as two fully separate sentences rather than one compound clause, exactly the same phrasing recommendation the Unit 3-D Comparative Evaluation Independent Constitutional Review already made for an analogous passage there.
2. Section 16 point 4 (Family A's own lower false-negative rate is "not, by itself, sufficient grounds for selection") is independently confirmed the document's own closest approach to a preference signal (Challenge 5/22 above), correctly hedged, but — consistent with the same pattern flagged in both the Unit 3-D and prior Unit 3-E Independent Reviews — a future document should state the numeric fact and its own disclaimer as two separate sentences rather than one, to reduce the risk of a reader quoting the number without the disclaimer attached.
3. This review notes, as a process observation rather than a defect, that this is now the third consecutive Independent Constitutional Review in this programme's Unit 3-D/3-E chain to flag the identical "state the number, then separately state the disclaimer" phrasing pattern (Unit 3-D Comparative Evaluation ICR Section 17 item 1; Unit 3-E Scope Lock ICR Section 16 item 1; this review's own items 1–2 above). A future governance task, if one is ever authorized to revisit this programme's own documentation conventions, might consider freezing this as a standing drafting rule rather than re-discovering it each time — not performed or authorized here.

## 30. Verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

The Remedy Selection Review's decision — NO REMEDY SELECTED — is independently re-derived from primary evidence and found correct on the merits: no admissible evidence has changed since Unit 3-D's own "insufficient to select" finding (independently re-verified via fresh commit-chronology tracing), all four candidates trivially pass every applicable minimum gate without those gates providing any discriminating power, and no candidate's own matched-subset semantic performance approaches a level that would responsibly justify an affirmative selection even setting the evidence-sufficiency finding aside. Every one of the twenty-five required adversarial challenges is independently tested against primary sources and found not to falsify the Review's own account. The qualifications in Section 29 are non-blocking phrasing recommendations, not substantive defects, and do not require the Remedy Selection Review's own decision or reasoning to change.

## 31. Confirmation

No remedy was selected, ranked, or preselected during this review. No model or HTTP call occurred. No campaign was created, resumed, or modified — Attempt 6's own artifacts were inspected read-only throughout, independently re-confirmed unchanged (26 files). No production, test, or Gradle file was modified. No Unit 3-C or Unit 3-D artifact was altered. Unit 4 and Unit 5 were not begun. The Remedy Selection Review document itself was not modified by this review.
