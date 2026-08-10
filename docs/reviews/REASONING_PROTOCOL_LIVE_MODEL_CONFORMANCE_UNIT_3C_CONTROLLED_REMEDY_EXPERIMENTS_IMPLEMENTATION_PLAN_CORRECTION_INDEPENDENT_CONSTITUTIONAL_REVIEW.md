**Status:** Correction Independent Constitutional Review of the Unit 3-C Controlled Remedy Experiments Implementation/Execution Plan — **ACCEPTED.** A fresh, independent adversarial review of the corrected document, not a reuse of the prior verdict. The original Independent Constitutional Review (`5ee5dccb8913defc07532dbf26158e325380de4286a75b044256390d79d02f54`, byte-identical before and after this task, independently re-verified) is preserved unmodified as historical evidence of the defects this correction addresses. No live model call, no HTTP call, no campaign, and no repository mutation beyond the Plan's targeted edits and this new document occurred.

# Unit 3-C Implementation/Execution Plan — Correction Independent Constitutional Review

## 1. Method

Independently re-extracted the corrected Plan's actual section headers (now twenty-five, following the addition of one new consolidation section) and independently re-checked every internal `Section N` reference against that ground truth — thirty-one occurrences in total, not just the nine already known. Independently re-hashed the Family B candidate prompt text directly from the corrected file. Independently re-derived the exact live-call total by arithmetic. Independently re-read all six supplemental fixtures word-for-word against their pre-correction text. Independently re-hashed the original ICR before drawing any conclusion about its integrity.

## 2. Were all 8 original cross-reference findings real?

Yes, independently re-derived from the pre-correction header list, not accepted from the original ICR's own word: exploratory-tier/comparability pointer (§4→ wrongly 15, should be 20), production-path pointer (§7→ wrongly 9, should be 13), downstream-isolation pointer (§9→ wrongly 13, should be 19), reproducibility pointer (§9→ wrongly 18, should be 22), the repetition-identity Scope-Lock over-citation (§10), adaptive-experimentation pointer (§11→ wrongly 12, should be 15), stop-rules pointer (§15→ wrongly 17, should be 18), and the implementation-level-verification pointer (§19→ wrongly 21, should be 22). All eight independently confirmed genuine.

## 3. How many affected occurrences existed?

Eight, one per finding — each finding in the original ICR corresponded to exactly one occurrence in the pre-correction Plan (unlike the Scope Lock's own earlier correction, where some findings had multiple occurrences).

## 4. Were all corrected?

Yes, all eight independently re-checked at their original locations and found correct post-edit.

## 5. Were any additional bad pointers found?

**Yes — one, found during this task's own fresh Phase-2 read, not present in the original ICR's list.** Plan §7's "Exact totals in Section 12" (referring to the exact call-accounting table) pointed to Section 12 (Model and inference identity) when the call-accounting table is actually Section 11 (Exact call accounting). This is a pure navigation defect — the target section exists, is correctly labeled in its own right, and correcting the number requires no policy interpretation — and was corrected during this task per Phase 6's explicit permission, recorded here rather than silently folded into the other eight.

An exhaustive, complete re-sweep of the corrected document (thirty-one internal `Section N` occurrences, independently checked one by one against the ground-truth header list, plus the newly added Section 25's own three internal references and its external Scope Lock citations) found **no further defect** beyond the nine now corrected (the original eight plus this one).

## 6. Was the original ICR preserved byte-identical?

Yes. Independently recomputed its SHA-256 (`5ee5dccb8913defc07532dbf26158e325380de4286a75b044256390d79d02f54`) both before and after this task's edits; identical both times.

## 7. Did completeness note 1 require only clarification or a substantive change?

Only clarification. Independently verified: Family A's measurement axes (decision-step semantic accuracy, rendering-step representation validity and content fidelity, false-positive REMEMBER/GOAL handling) were already fully specified in the pre-correction Plan §7; the added "what would count as evidence against" paragraph restates these same, already-established axes in their negative/falsifying form and explicitly disclaims inventing any new threshold. No new acceptance criterion, number, or measurement was introduced.

## 8. Did completeness note 2 require only consolidation or a substantive change?

Only consolidation. Independently verified: every claim in the new Section 25 traces to a requirement already present elsewhere in the Plan or in the frozen Scope Lock — the per-family dimension mapping restates Plan §7–9's own measurement definitions; the "no exploratory result proves full conformance" statement restates Plan §2/§11's own exploratory-tier framing; the four unresolved Unit 3-A dimensions restate Scope Lock §18 verbatim; the deferred/excluded-family firewall restates Scope Lock §24 verbatim, with the Family E/F cross-references correctly pointing to the Plan's own already-existing Section 12 (Model and inference identity), which already froze exactly those two constraints. No family classification was reopened, revised, or reinterpreted.

## 9. Did any fixture change?

No. Independently re-read all six supplemental fixtures (`P14`, `P15`, `P16`, `P17`, `P18`, `G06`) in full, word-for-word, against the versions reviewed by the original ICR: identical text, identical IDs, identical ordering, identical section placement.

## 10. Did any expected action change?

No. All six remain REPLY (`P14`–`P18`) or GOAL (`G06`), exactly as before.

## 11. Did Family A change?

No. Plan §7's decision-step and rendering-step design, scoring rules, and cost accounting are unchanged; only its own internal pointer (§9→13) was corrected, and one new paragraph (the counts-against criterion) was appended without altering any existing sentence.

## 12. Did Family B change? Did its candidate hash change?

No to both. The candidate text block was independently re-extracted from the corrected file and rehashed: `cfd5cb7f07d0d7da941b069e00b3f479fc5faf5e71662a83bda51f25bd629d60` — identical to the value the original ICR verified.

## 13. Did Family C change?

No. The five-step mechanism specification and the twenty-nine-row predicted trace table (25/29 correct, 3 predicted false positives on `P03`/`P04`/`P05`, 1 predicted false negative on `R03`) are byte-for-byte unchanged; only the internal pointer within Family C's own section (§9→13, §9→22) was corrected.

## 14. Did repetition change?

No. n=5 for Control/Family A/Family B and n=1 for Family C's deterministic arm are unchanged; only the citation supporting the "identical unless justified" practice was corrected from an over-citation of Scope Lock §15 to an accurate citation of Scope Lock §12, without changing the practice itself.

## 15. Did the exact call total remain 483?

Yes, independently re-derived by direct arithmetic: warm-up 3 + control (115 base + 30 supplemental = 145) + Family A (115 decision + 105 rendering = 220) + Family B 115 + Family C 0 = **483**, matching both the pre-correction and post-correction stored totals exactly.

## 16. Did model strategy change?

No. `qwen2.5-coder:7b` only, unchanged.

## 17. Did inference configuration change?

No. Current production configuration, unchanged, including the 30,000 ms timeout.

## 18. Did artifact semantics change?

No. The Section 16 field table and its per-arm nullability rules are untouched.

## 19. Did exact-once semantics change?

No. Section 17 is untouched.

## 20. Did stop conditions change?

No. Section 18's measurement-invalidating/remedy-performance distinction and the non-numeric, first-occurrence checkpoint trigger are untouched; only an unrelated forward-pointer citing this section from elsewhere (§15's pre-registration list) was corrected to point to it correctly.

## 21. Did downstream isolation change?

No. Section 19 is untouched in substance; the one internal citation error found *within* Family C's section pointing to it (previously mislabeled "Section 13") now correctly reads "Section 19."

## 22. Did implementation surface change?

No. Section 21's determination (no `src/` change required, no Boundary Review triggered) is untouched.

## 23. Did remedy neutrality change?

No. Section 2's explicit denial of remedy selection, Unit 3-D comparison, Unit 3-E selection, and Unit 4 implementation is untouched; the new Section 25 reinforces rather than weakens this by stating plainly that no exploratory result proves full conformance.

## 24. Did deferred/excluded family status change?

No. The new Section 25.B restates the Scope Lock §24 classification verbatim (Family D deferred, E deferred, F excluded, G split deferred/excluded, H excluded, I deferred) without reopening or altering any of it.

## 25. Did Unit 3-D, Unit 3-E, or Unit 4 authority leak backward?

No. No comparison, selection, or implementation content appears anywhere in the corrected Plan; the new Section 25 explicitly reinforces this boundary rather than crossing it.

## 26. Is implementation authorized by this correction?

No. Every edit made during this task was either a section-number digit, a short clarifying paragraph restating already-established measurement axes, or a consolidation section restating already-established constraints. No code, fixture implementation, or production change is authorized anywhere.

## 27. Is live execution authorized?

No. No endpoint, campaign identity, or execution instruction was introduced by any edit.

## 28. Is the corrected Plan ready to freeze?

Yes, independently assessed on the merits of the corrected document as a whole: the frozen Scope Lock is faithfully translated; Family A, B, and C designs remain exactly as previously reviewed and are now additionally auditable via an explicit falsification criterion and a consolidated traceability/firewall statement; the exact 483-call total, the Family B hash, and the Family C predicted trace are all independently re-verified unchanged; the original eight cross-reference defects plus the one additional defect found during this task's own fresh read are all corrected; and a complete, exhaustive re-sweep of every internal and external citation in the corrected document found no remaining defect of any kind.

## 29. Blocking defects

None found, before or after this task.

## 30. Non-blocking qualifications

None remain.

## 31. Verdict

```text
ACCEPTED
```

## 32. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production, test, or Gradle file changed. No fixture code was added. No remedy was selected, prototyped, or endorsed. The original Independent Constitutional Review remains preserved, unmodified, as the historical record of the defects this correction addresses.
