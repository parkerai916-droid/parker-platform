**Status:** Independent Constitutional Review of the Unit 3-D Comparative Evaluation Review — **ACCEPTED WITH QUALIFICATIONS (non-blocking).** This review independently re-derives every quantitative claim from primary Attempt 6 durable artifacts and the committed Family C offline completion evidence, using extraction methods distinct from those the Comparative Evaluation Review itself used, and specifically hunts for ranking language disguised as descriptive comparative findings. No model or HTTP call occurred. No campaign was created, resumed, or modified. No remedy was ranked or selected. No code, test, Gradle, or Unit 3-C artifact was modified.

# Unit 3-D Comparative Evaluation — Independent Constitutional Review

## 1. Method

Independently re-read the Comparative Evaluation Review's own text in full, checking every table cell against a fresh re-derivation rather than accepting its own arithmetic. Independently re-derived the matched-subset semantic table via a **fourth distinct method** (a `grep`/`wc -l`-based shell pipeline, counting literal `\"semanticCorrect\\":true` occurrences per fixture-prefixed `trialId`), not reusing the Python-based decoding either the Review or any prior task in this programme used. Independently re-read the frozen Scope Lock (`32b55d4`) a second time, checking the Review's own compliance claims against the Scope Lock's actual text rather than the Review's own paraphrase of it. Specifically re-read every sentence of the Review's Section 18 (strengths/weaknesses/tradeoffs) and Section 20 (Unit 3-E handoff) — the two sections most likely to contain disguised ranking language — word by word.

## 2. Independent re-derivation: exposure denominators

Independently re-counted via fresh `wc -l` on every arm's own `intent.jsonl`/`raw.jsonl`/`timeouts.jsonl`: warm-up 3/3/0, Control 132/132/0, Family A 51/50/1, Family B 91/91/0, Family C 0(by design)/6/0. **Identical to the Review's own Section 5 table.** Independently re-confirmed `intent = raw + timeouts` holds exactly for every arm with an `intent.jsonl` (Family C has none, by design, and this is independently re-confirmed correct — Family C makes no model call, so no intent record is architecturally required).

## 3. Independent re-derivation: matched subset

Independently re-derived, from raw artifacts, which fixtures show `n=5` in Control, Family A's own decision sub-step, and Family B simultaneously: exactly `r01-direct`, `r02-please`, `r03-dont-forget`, `p01-ordinary-fact`, `p02-quoted-remember` — no sixth fixture qualifies, and none of the five is disqualified. **Independently confirmed the matched subset is neither artificially narrowed nor artificially enlarged.**

## 4. Independent re-derivation: semantic counts (fourth distinct method)

Independently re-derived the full matched-subset table via a `grep`-based shell pipeline (distinct from the Python-based decoding used by the Review and by every prior task in this programme):

| Fixture | Control | Family A decision | Family B |
|---|---|---|---|
| `r01-direct` | 0/5 | 0/5 | 0/5 |
| `r02-please` | 0/5 | 4/5 | 1/5 |
| `r03-dont-forget` | 0/5 | 3/5 | 0/5 |
| `p01-ordinary-fact` | 5/5 | 3/5 | 5/5 |
| `p02-quoted-remember` | 5/5 | 4/5 | 4/5 |
| **Total** | **10/25** | **14/25** | **10/25** |

**Exactly matches the Review's own Section 7 table in every cell.** Independently confirmed via a genuinely separate extraction method, not a re-run of the same script.

## 5. Independent re-derivation: the Family A denominator correction

Specifically re-verified the Review's own claim that a prior document (the Attempt 6 Execution Evidence Review) stated an incorrect "8/13" denominator for Family A's decision-step false-negative count. Independently re-counted decision-step trials for `r01`, `r02`, `r03` directly: 5 + 5 + 5 = **15**, not 13. Independently re-confirmed the false-negative count itself (5 + 1 + 2 = 8) is unchanged and correct in both the original and corrected framing — **only the stated denominator was wrong in the earlier document; the Review's own correction (8/15) is independently verified accurate.** This is judged a genuine, valuable correction, appropriately disclosed in place rather than silently substituted, consistent with this programme's own established discipline for handling discovered errors.

## 6. Independent re-derivation: Family A split

Independently re-confirmed the Review's own Section 9 maintains strict separation throughout: decision-step figures (14/26 arm-wide, 14/25 matched, 8/15 false-negative) and render-step figures (24/24) never appear combined, averaged, or summed into one Family-A-wide score anywhere in the document — independently searched the full document text for any arithmetic combination of these two figures (e.g., "38/50," "76%," any figure that would result from summing 14+24 against 26+24) and found none. **The Scope Lock's own binding Section 8 prohibition is independently confirmed observed.**

## 7. Independent re-derivation: Family C provenance

Independently re-read every Family C figure in the Review (Sections 7, 11, 14, 18, 20) and independently tagged each as Source A, Source B, or A+B-combined-and-labeled: the matched-subset table (Section 7) is correctly all-Source-A; Section 11's part A/B split is correctly labeled; Section 14's "1 of 29" figure is correctly labeled "Source A+B combined"; Section 20's handoff table correctly states "live + offline, provenance-separated" rather than presenting a bare "29/29." Independently re-executed the classifier a **third time** for this review (a third independent implementation, following the pattern already established by the governing Determination's own two methods and this task's own predecessor verification), confirming 24/29, 4 false positives, 1 false negative, unchanged. **No provenance blending found.**

## 8. Independent re-derivation: censoring treatment

Independently re-checked whether the Review's own Section 6 correctly applies the Scope Lock's five frozen censoring rules, rule by rule, rather than merely restating them: rule 3 (no inference on unobserved remainder) is independently confirmed observed — no sentence anywhere in the Review states or implies what an untested trial "would have shown"; rule 4 (later timing ≠ superiority) is independently confirmed observed in Section 6 itself and reinforced explicitly in Section 13 and Section 18's own Control discussion ("Its own later safety-checkpoint trigger reflects a larger, differently-composed exposure, not an established safety property"). **Correctly classified and correctly applied throughout, independently re-confirmed.**

## 9. Independent re-derivation: timeout handling

Independently re-confirmed Family A's one timeout (`family_a/r02-please/render/02`) is excluded from every semantic count in the Review — independently re-verified the render-step denominator (24 of a possible 25 trials across the five completed fixtures: `r01`×5, `r02`×4, `r03`×5, `p01`×5, `p02`×5 = 24) correctly reflects the timeout's own absence rather than miscounting it as a completed trial with a fabricated outcome. **No survivorship-bias violation found** — Section 12 explicitly pairs the timeout's own existence with the surrounding completion-rate context rather than presenting decision/render figures in isolation.

## 10. Independent re-derivation: false-positive interpretation

Independently re-checked Section 13's own permitted-versus-prohibited framing against the raw facts: one false positive per arm, independently re-confirmed via the four arms' own `SAFETY_CHECKPOINT` files (`p17-hypothetical-remember`, `p03-ambiguous-memory` [Family A], `g03-later-action`, `p03-ambiguous-memory` [Family C]) — exact match to the Review's own citations. Independently confirmed the Review does not, anywhere, state or imply that equal occurrence establishes equal safety, and does not rank the four events by trigger timing as a safety metric. **Section 13 is independently judged the strongest, most carefully-hedged section in the document — no violation found.**

## 11. Independent re-derivation: false-negative interpretation

Independently re-confirmed Section 14 keeps three distinct denominators separate throughout (`/15` for the model-arm matched subset, `/3` for Family C's own matched-subset trials at its own `n=1` design, `/29` for Family C's own full-corpus figure) and never pools them. Independently re-verified the `/3` figure specifically, since it is the one most likely to be silently conflated with the `/15` figures given the shared fixture identities: Family C's own `r01`/`r02`/`r03` results (2 correct, 1 false negative, `n=1` each) independently re-confirmed distinct from, and correctly never summed with, the model arms' own `n=5`-each figures. **No denominator mixing found.**

## 12. Independent re-derivation: representation/parser/transport interpretation

Independently re-confirmed 100% representation validity, 0 parser failures, 0 transport failures in every arm, and independently re-confirmed Section 15's own explicit statement that this uniform result "must not obscure the substantial semantic-selection failures documented in Sections 7 and 9–11" is not merely a disclaimer but is followed by a concrete cross-reference to Control's own 0/25 matched-REMEMBER result as the illustrating example. **Correctly stated, not merely gestured at.**

## 13. Independent re-derivation: `contentFidelity` exclusion

Independently re-sampled five fresh `raw.jsonl` records (one per arm, different trial indices than any prior sampling in this programme's history — the third record in each file rather than the first, to rule out a systematic first-record artifact) and confirmed `"contentFidelity":null` in every one. **Independently confirmed NOT AVAILABLE is accurate and no proxy is used anywhere in Section 16 or elsewhere.**

## 14. Independent re-derivation: statistical authority

Independently re-scanned the full Review text for any of the specifically prohibited constructions (a p-value, a confidence interval, the words "significant"/"significantly," "population," "qualifies," "production-ready," or any percentage presented without its own denominator in the same sentence or table). None found. Every percentage in the document is independently confirmed to carry an explicit fraction alongside it. **Statistical authority is not overstated.**

## 15. Independent re-derivation: remedy-selection firewall — the central adversarial check

This is treated as the review's own most consequential task, per the governing instruction to specifically hunt for ranking language disguised as comparative findings. Independently re-read Section 18 and Section 20 sentence by sentence, the two sections carrying the highest risk.

**Section 18, Family A paragraph:** states "Family A's own decision step's false-negative rate on REMEMBER-expected trials (8/15) is lower than Control's (15/15) and Family B's (14/15) on the same matched fixtures, stated as a matched-subset fact." Independently scrutinized this sentence specifically for ranking language: it uses the comparative "lower than," a numeric relation, immediately followed by "whether this reflects the decision/rendering-separation mechanism itself, the smaller sample Family A's own early checkpoint produced, or another cause is not established by this evidence and is not claimed here." **Independently judged: this is a permitted descriptive statement of a COMPARABLE-WITH-QUALIFICATION metric (false-negative rate on the matched subset, per Scope Lock Section 7), not a ranking** — it states a numeric fact and explicitly, in the same sentence group, forecloses the causal/superiority inference a ranking would require. This is exactly the boundary the Scope Lock's own Section 14 example ("Family A's rendering step showed stronger representation compliance than its decision step showed semantic selection accuracy") illustrates as permitted (an explicit comparative-language example the Scope Lock itself sanctions). **Not a violation, but flagged as the single passage in this document closest to the line**, and worth explicit note for exactly that reason (Section 17 below).

**Section 18, Family B paragraph:** "comparable in magnitude to Control's own matched-subset figures... stated as a fact about these two arms' own matched-subset numbers, not as a claim of equivalence." Independently judged this hedging sufficient and, if anything, more conservative than the Family A passage — "comparable in magnitude" is a weaker claim than "lower than," and the explicit denial of equivalence is present. **Not a violation.**

**Section 20 handoff table:** independently re-read every cell for ranking language. Found none — every cell states raw evidence-availability facts ("largest exposure," "smallest exposure," specific counts) without a comparative value judgment attached. The table's own "Prohibited inferences" closing paragraph independently re-confirmed to explicitly forbid exactly the reading a careless Unit 3-E task might otherwise draw from the table's own raw numbers. **Not a violation.**

**Independently searched the full document** for the specific terms this programme has already flagged as selection-adjacent ("best," "preferred," "recommended," "wins," "superior," "outperforms," "should be selected," "should be adopted"): zero occurrences in any comparative sense. The word "stronger" appears once (Section 9's own intra-arm Family A comparison, explicitly Scope-Lock-sanctioned) and once in Section 18's restatement of the same intra-arm point — never applied across remedy families.

## 16. Independent re-derivation: exit criteria

Independently re-checked each of the Review's own Section 22 self-verification claims against the document's actual content rather than accepting the self-check at face value: item 4 (Family A split preserved) independently re-confirmed (Section 6 above); item 5 (Family C provenance preserved) independently re-confirmed (Section 7 above); item 8 (no remedy selected) independently re-confirmed (Section 15 above, the most scrutinized claim). **All nine self-checkable criteria independently re-verified accurate**, not merely restated from the Review's own say-so.

## 17. Discrepancies found

None requiring correction. One passage (Section 18's Family A false-negative-rate sentence, Section 15 above) is flagged as the document's own closest approach to the ranking-language boundary — independently judged to remain on the permitted side of that boundary, but recorded as a non-blocking qualification given how directly the governing task instructed this review to hunt for exactly this pattern.

## 18. Blocking defects

None.

## 19. Non-blocking qualifications

1. Section 18's Family A false-negative-rate sentence ("lower than Control's... and Family B's") is independently judged permitted but is the single passage in the document most likely to be misquoted, out of context, as a ranking claim by a future reader who stops reading before the sentence's own second half. A future Unit 3-D output should consider restating comparative-numeric observations of this kind as two fully separate sentences — the number, then the disclaimer — rather than one compound sentence, to reduce this specific misreading risk.
2. The Family A denominator correction (Section 5 above) is accurate and appropriately disclosed, but this review notes that the earlier document containing the original "8/13" error (the Attempt 6 Execution Evidence Review) is not itself amended by this correction — a future reader consulting that earlier document directly, without also reading this Comparative Evaluation Review, would still encounter the uncorrected figure. This is not a defect in either document (amending Attempt 6's own evidence record is explicitly out of scope for both that document's own governance chain and this task), but is worth flagging so a future task does not assume the correction has propagated backward.

## 20. Verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

Every quantitative claim in the Comparative Evaluation Review is independently re-derived from primary sources — via a fourth distinct extraction method for the matched-subset table and a third independent classifier re-implementation for Family C — and found accurate. Every Scope Lock rule (evidence-source separation, exposure disclosure, the matched five-fixture subset, the five censoring rules, the metric comparability matrix, the Family A split prohibition, Family C provenance separation, timeout/survivorship-bias handling, false-positive/false-negative denominator discipline, `contentFidelity` exclusion, descriptive-only statistical authority, and the remedy-selection firewall) is independently re-verified observed. The specific, targeted hunt for ranking language disguised as comparative findings (Section 15) found one passage worth flagging for future phrasing improvement but no actual violation. The qualifications in Section 19 are non-blocking and do not require the Comparative Evaluation Review's own text to change before it may be relied upon as Unit 3-D's own complete, evidence-faithful output.

## 21. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified — Attempt 6's own artifacts were inspected read-only, independently re-confirmed unchanged (26 files). No production, test, or Gradle file was modified. No Unit 3-C artifact, fixture, or mechanism was altered. No remedy was ranked, preferred, or selected. Unit 3-E and Unit 4 were not begun. The Comparative Evaluation Review document itself was not modified by this review.
