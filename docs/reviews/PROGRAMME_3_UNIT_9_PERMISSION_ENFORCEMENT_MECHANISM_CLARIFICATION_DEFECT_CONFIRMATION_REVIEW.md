# Programme 3 — Unit 9 Permission Enforcement Mechanism Clarification — Defect Confirmation Review

## Status

**Defect confirmation review only — not a new Independent Constitutional Review.** This review does not re-examine the enforcement-location determination, the two-tier granularity mechanism, the resource/action disclosure, the evaluation order and count, the principal/correlation-identifier treatment, the denial disposition, or the Unit boundary — the genuine Independent Constitutional Review already confirmed each of those sound. It confirms only that the two required corrections were applied precisely, and that no regression was introduced. No Kotlin is implemented, proposed, or changed. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD:** `8b929c223ba11af5df66da061373239002b12269`
- **Branch:** `main`
- **Staged changes:** none.

---

## Defects Reviewed

The Independent Constitutional Review's two Required Corrections, both citation-location defects in `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md`:

1. Section 7's closing sentence cited "(Section 12)" for the "Explicit Non-Expansions" constraint; the correct section is 11.
2. Section 8, steps 2 and 6, cited "(Section 6.2.1)" and "(Section 6.2.2)" — headings that do not exist; Section 6.2 contains an unlabeled numbered list, not sub-headed subsections.

---

## Verification

Re-checked directly, by re-reading each of the three affected locations against the Clarification's own actual section numbering:

1. **Section 7** now reads "...broader than this document's own minimal, disclosed authority (Section 11)." Confirmed correct: Section 11 of the Clarification is "Explicit Non-Expansions," the section actually naming this constraint.
2. **Section 8, step 2** now reads "Evaluate the act-level gate (Section 6.2's own first item)...". Confirmed this resolves to an actual location in the document (Section 6.2's numbered list, item 1 — the act-level gate).
3. **Section 8, step 6** now reads "Evaluate the item-level gate (Section 6.2's own second item)...". Confirmed this resolves to an actual location (Section 6.2's numbered list, item 2 — the item-level gate).

Confirmed by direct search (`grep -n "Section 12)\|Section 6\.2\.1\|Section 6\.2\.2"`) that no remaining occurrence of either defective citation exists anywhere in the document.

**Nothing else was changed.** All three edits are citation-text substitutions only; no substantive sentence, determination, quotation, or reasoning was altered.

**Both corrections confirmed correctly and completely applied.**

---

## Regression Check

- **Substantive content** — unchanged: the self-gating determination (Section 6.1), the two-tier granularity mechanism (Section 6.2), the resource/action disclosure (Section 7, apart from its corrected citation), the evaluation order and count (Section 8, apart from its two corrected citations), the correlation-identifier treatment (Section 9), the denial disposition (Section 10), the Explicit Non-Expansions and Programme Boundary sections (11, 12), and the freeze-status disclosure (Section 14) are all byte-identical to the version the Independent Constitutional Review examined.
- **Quotations** — unchanged: no quoted text from any governing document was touched by this remediation.
- **No governance document other than the Clarification itself was modified** — confirmed by `git diff --name-only`.
- **No `src/` or `tests/` file was touched** — confirmed by `git diff --name-only`; this remains a documentation-only remediation, exactly as its predecessor act (drafting the Clarification) was.

**No regression found.**

---

## Confirmations

- Both required corrections are present, verified against the exact text of each of the three affected locations.
- No substantive or architectural determination changed.
- No constitutional conclusion from the Independent Constitutional Review's other ten review areas was revisited or altered.
- No new governance document was created; only the Clarification itself was edited, plus this document.
- The Independent Constitutional Review document itself was not modified.

---

## Recommended Next Step

The Clarification may now be treated as adopted. Unit 9.5 implementation may proceed on the enforcement-mechanism dimension it settles: self-gating, evaluated once per query (act-level) plus once per matched-and-lifecycle-shaped candidate item (item-level, before bounding), using one fixed resource/action pair, with `query.correlationId` propagated unchanged into every constructed `ExecutionRequest`, act-level denial expressed as `KnowledgeRetrievalDisposition.NotAuthorised`, and item-level denial expressed as silent filtering from `KnowledgeRetrievalResult.entries`.

---

## Final Git Status

```
$ git status --short
?? docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md
?? docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
