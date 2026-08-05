# Evidence Intelligence Contract Design Amendment 2 — Independent Constitutional Review

## Status

**Independent review only. No file modified during this review.** No Kotlin implemented. No Gradle run. Nothing staged, committed, or pushed.

---

## 1. Repository Baseline

`main` at `eac43cbbb9d4e02731608769107fc44b89f367af` (`eac43cb`). Working tree matched the expected state exactly before this review began:

```
 M docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md
 M docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md
?? docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_OCR_MECHANISM_AMENDMENT_PROPOSAL.md
?? docs/reviews/EVIDENCE_PROCESSING_OCR_OWNERSHIP_AND_SEQUENCING_REVIEW.md
?? docs/reviews/EVIDENCE_PROCESSING_UNIT_5_OCR_PLANNING_REVIEW.md
```

No discrepancy.

---

## 2. Documents Reviewed

Parker Constitution; Evidence Intelligence Contract Design (post-Amendment-2, full, cross-checked against its pre-amendment text); Evidence Intelligence Scope Lock (post-Amendment-2, full, cross-checked against its pre-amendment text); CDR-007 (full); CDR-006 (by title and citation, where referenced); Evidence Processing Searchable PDF Scope Lock; Evidence Processing Boundary Clarification; OCR Planning Review; OCR Ownership and Sequencing Review; the final revised OCR Mechanism Amendment Proposal (the authoritative scope limit for this amendment); the two prior Independent Constitutional Reviews of that proposal (this conversation); Amendment 1's own accepted structure, read as the binding style/mechanism precedent.

---

## 3. Amendment Diff Reviewed

```
 docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md | 58 +++++++++++++++++++++-
 docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md      | 12 +++++
 2 files changed, 69 insertions(+), 1 deletion(-)
```

Four locations changed, exactly: Contract Design Status section (new "### Amendment 2" subsection, five paragraphs); Contract Design §12 (opening sentence qualified; one new table row); Scope Lock Status section (new, short "### Amendment 2 (Mirrored)" subsection); Scope Lock §4 (one new table row, identical to the Contract Design's). No other hunk exists in either file.

---

## 4. Amendment Fidelity

Every sentence in the new text was traced to a specific clause of the final revised proposal:

- §12's opening-sentence replacement is a **verbatim** application of proposal §2.1's own "Smallest amendment" text (only "this amendment" → "Amendment 2").
- §12's new table row and its Purpose-column truth-authority clause are a **near-verbatim** application of proposal §2.2's row text and its mandatory carry-forward sentence.
- The Scope Lock's new row is a **verbatim** duplicate of the Contract Design's, exactly as proposal §2.3 directs ("Add the identical fourth row... with no other change").
- The Status-section paragraphs restate proposal §1 (Amendment Objective), §4 (Ownership Analysis, all four bullets), §11 (CDR/constitutional assessment), and the non-authorisation list synthesises proposal §4, §6, §7, §8, and §12 step 6.

**No sentence introduces new constitutional reasoning, broadens the proposal, or resolves a question the proposal deliberately deferred.** Two sentences are synthesised rather than quoted, and are flagged here for transparency rather than as fidelity violations:

1. "CDR-006's own classification of original evidence custody and immutability is unaffected and unreopened by this amendment" — assembled from CDR-006's own title plus the proposal's §11 statement ("not a reopening of CDR-006 or CDR-007"). No new claim beyond what both sources already establish.
2. "Evidence Processing's own ownership of OCR detection... is likewise unaffected; this amendment touches no Evidence Processing governance document" — assembled from the proposal's own explicit scope boundary (Status section: only the EI Contract Design and, where mirrored, the EI Scope Lock) and its §7 references to `RequiresOcr`/`EvidenceExtractionCoordinator.extract`. This is an observable, structural fact (which files this amendment touches), not an invented finding.

Neither weakens an existing rule or expands what the proposal authorised.

---

## 5. Existing-Text Preservation

**The one substantively altered existing sentence** is Contract Design §12's opening line. Original: "Every dependency below already exists in this repository's governed contracts. **No new platform subsystem is introduced by this document.**" The amendment replaces this two-sentence unit with the proposal's single qualified sentence, which does not restate any form of the second sentence ("No new platform subsystem is introduced").

- **What was removed:** the unqualified guarantee that no new platform subsystem is introduced by the Contract Design.
- **Was the change necessary?** Yes, in substance — the proposal's own §2.1 "Why stale" reasoning is explicit that leaving this sentence intact would be a direct, false claim once an OCR-mechanism dependency exists. Simply deleting it, rather than reformulating it, is the literal, minimal reading of the proposal's own one-sentence "Smallest amendment" text (which replaces, not supplements, the quoted two-sentence "Current text").
- **Was a guarantee weakened?** In the *document's own self-description*, yes: the "no new platform subsystem" guarantee is now absent from §12 entirely, with nothing in its place — not even the more precise "no new *peer* subsystem, in CDR-006/CDR-007's sense" formulation the proposal's own §11 constitutional-tier argument would support. §11's reasoning is real and is cited in the amendment's own Status subsection ("this remains a Contract Design evolution only"), but its *substance* — capability-tier, not subsystem-tier — is never restated in the amended §12 text itself.
- **Does the revised sentence remain accurate for both existing and newly authorised dependencies?** Yes, on its own terms — it is factually accurate as written. The defect is not in what it says, but in what silently disappeared from its surroundings (§6, below).

---

## 6. OCR Dependency Analysis

The amendment authorises exactly, and only:

- an abstract, unnamed OCR mechanism dependency (zero or one), structurally parallel to `ReasoningProvider`;
- Evidence Intelligence's ownership of OCR execution, consistent with CDR-007 §1 and the Contract Design's own §1 "Producing candidate derivative artefacts" (already naming "an OCR transcription");
- the fact that further governance is required before this dependency connects to anything concrete.

Confirmed **not** authorised, by direct inspection of the diff: a concrete provider; OCRmyPDF; Docling; Tesseract; an interface name; Kotlin (grep for `OcrMechanism`/`` `Ocr[A-Z] `` and for "OCRmyPDF"/"Docling"/"Tesseract" across both diffs returns zero matches); runtime composition (explicitly excluded); orchestration (explicitly excluded); output-quality validation (explicitly excluded); implementation (explicitly excluded). Machine-triggered invocation is not authorised either, but see §11, below, on how precisely this is named.

---

## 7. Ownership Analysis

All six boundaries the task named are explicitly present in the amendment's own text: Evidence Custodian's ownership of accepted artefacts; provenance ownership (`Provenance`/`CandidateProvenance`/`EvidenceArtifactId`/`CandidateEvidenceArtifact` named directly); original-evidence immutability (via CDR-006, by name); Evidence Processing's ownership of OCR detection (`RequiresOcr`, named directly); Evidence Intelligence's ownership of OCR execution (the amendment's central objective); parser non-authority over truth (stated twice — once in the Status subsection, once in the table's own Purpose column, in both files). No boundary is narrowed, silently or otherwise.

---

## 8. Constitutional-Tier Analysis

The amendment's Status subsection asserts the *conclusion* of proposal §11 ("No operative CDR-007 decision changes... this remains a Contract Design evolution only, of the same tier and character as Amendment 1") without carrying forward the *reasoning* that supports it — the capability-tier/subsystem-tier distinction (CDR-006 and CDR-007 each performed subsystem-tier classification; an OCR mechanism is dependency-tier, mirroring `ReasoningProvider`, which itself needed no CDR). This omission is directly connected to the finding in §5 and §13, below: had this distinction been stated in the amended text itself, it would also have supplied the exact qualifying language needed to keep §12's opening sentence, the Final Recommendation, and the Scope Lock's own exclusion table mutually consistent. As drafted, the conclusion is present and correct; the load-bearing reasoning behind it is not visible in the amended document itself.

---

## 9. Public-Model Analysis

Confirmed: no fifth `EvidenceAnalysisResult` category, no new Evidence-Intelligence-owned public type, no new interface (`EvidenceIntelligence` remains the sole one), no new acceptance disposition (§6 Acceptance Paths untouched), no changed ownership invariant (§5's ownership-transfer rule untouched). The Scope Lock's own "No fifth new public type or interface is authorised... These four are the entire new public surface" (§4) remains accurate and was correctly left unedited, consistent with proposal §2.5's own "not stale" finding.

---

## 10. Scope Lock Consistency

The Contract Design's new row and the Scope Lock's new row are **byte-identical**, confirmed by direct comparison of both diff hunks. No wording divergence, no inconsistent status language, no different future-governance framing, no accidental authority expansion in either direction.

---

## 11. Future-Governance Preservation

The amendment's non-authorisation list names: OCR implementation, OCR provider selection, OCR runtime composition, an OCR output-validation mechanism, OCR orchestration ("including who consumes Evidence Processing's own `RequiresOcr` disclosure"), and the future OCR mechanism Contract Design. This covers every item the task's checklist named, but two of the proposal's most carefully-reasoned, individually-named open questions — **machine-triggered invocation as an owner-control question** (proposal §7, its own dedicated paragraph against Constitutional Test 1) and **permission gating for output-rejection** (proposal §8's "one genuinely open sliver") — are folded into the single generic phrase "OCR orchestration" rather than named individually. The deferral itself is intact (nothing is resolved that should not be), but a future reader of the Contract Design alone, without the proposal in hand, would not learn that owner-control for machine-triggered invocation is a *specifically identified* unresolved constitutional question, only that "orchestration" in general is future work. This is a precision loss, not a scope violation.

---

## 12. Citation Verification

Every citation newly added by Amendment 2 was checked directly against its source:

- "'an OCR transcription' is already named there as an example of 'Producing candidate derivative artefacts'" (Contract Design §1) — verified exact quote.
- "CDR-006's own classification of original evidence custody and immutability" — verified accurate to CDR-006's own title.
- "the `RequiresOcr` disclosure `EvidenceExtractionCoordinator.extract` already produces" — verified accurate to `ExtractionOutcome.RequiresOcr` / `EvidenceExtractionCoordinator.extract` as implemented.
- Scope Lock's citation, "'Exactly which public contracts may be depended upon is frozen at precisely the Contract Design's own §12 table — no more, no less'" — verified **exact, word-for-word** quote of Scope Lock §4's own opening sentence.

**No citation error found in the text Amendment 2 itself adds.** The two citation misattributions found in an earlier round of review (a Boundary Clarification section that does not exist; a Constitution section misattribution) were both in the *proposal* document and were corrected there in a prior revision pass; neither erroneous form was carried into the amendment — the amendment's own parser-authority and constitutional-tier sentences do not repeat either citation.

---

## 13. Findings

**Finding 1 (primary — internal inconsistency, not fidelity violation).** Amendment 2 was drafted with full fidelity to its proposal, and correctly avoided touching any paragraph the proposal did not identify. But the proposal's own "Exact Paragraphs Requiring Amendment" list (§2) never searched the Contract Design or Scope Lock for *other* paragraphs asserting the same fact §12's opening sentence asserted — and at least two exist, both now stale, both left unedited because the proposal never named them:

- **Contract Design, "Final Recommendation"** (untouched by Amendment 1 or Amendment 2): "three new public types, one new public interface, **zero new platform subsystems**, no amendment to any existing contract introduced *by this document*." This is the identical claim §12's own opening sentence was just qualified to avoid making unconditionally — the Final Recommendation now makes it unconditionally, four sections later.
- **Scope Lock §3, Explicit Exclusions table**, the "Planning, or any dependency on Planner Runtime" row: "Not named in Contract Design §12's dependency table; **the three-row table is exhaustive**." The table now has four rows.

Both are now factually false statements sitting, unedited, in the same canonical documents Amendment 2 modifies. Neither reflects a wrong constitutional judgment — the proposal's own §11 argument (capability-tier, not subsystem-tier; correctly not requiring a new CDR) is sound and is what would resolve both, if its precise language ("no new *peer* subsystem," not "zero new platform subsystems" unqualified) had been used consistently everywhere the original claim appears, not only at its first occurrence.

**Finding 2 (secondary — precision loss, not a scope violation).** §11, above: the amendment's own text asserts the *conclusion* of the capability-tier/subsystem-tier distinction without restating the *reasoning*, and the non-authorisation list compresses two individually-significant deferred questions (owner-control for machine-triggered invocation; output-rejection permission gating) into one generic "orchestration" phrase.

No other defect was found. Ownership boundaries, parser non-authority, public-model invariants, Scope Lock consistency, and citation accuracy all pass without qualification.

---

## 14. Required Corrections, if any

1. Correct the Contract Design's "Final Recommendation" section's "zero new platform subsystems" claim to acknowledge Amendment 2 — e.g., qualifying it the same way §12 itself was qualified, or adding the precise "no new *peer* subsystem" formulation from proposal §11.
2. Correct the Scope Lock §3 "the three-row table is exhaustive" citation to reflect the now-four-row table (the underlying exclusion — no Planner Runtime dependency — remains correct and does not itself need to change).
3. Optional, not blocking: strengthen the amendment's own Status-subsection text to state the capability-tier/subsystem-tier distinction explicitly, and to name machine-triggered invocation and output-rejection permission gating individually in the non-authorisation list, matching the proposal's own precision.

None of these corrections requires new constitutional reasoning beyond what the proposal (§11 in particular) already supplies — each is a textual-consistency fix, not a substantive one.

---

## 15. Constitutional Verdict

**FORMAL AMENDMENT DEFECT**

The amendment's constitutional substance is sound: the OCR dependency is correctly scoped as capability-tier, ownership boundaries are fully preserved, no public contract is expanded, no CDR is reopened, and the Scope Lock mirror is exact. The defect is formal, not substantive — implementing the proposal exactly as scoped has left two now-false statements elsewhere in the same canonical documents, uncorrected, because the proposal itself never identified them as requiring a matching edit. This must be corrected before the amendment is complete and internally self-consistent.

---

## 16. Recommended Next Step

Do not accept Amendment 2 as final in its current form. Apply Required Corrections 1 and 2 above (a narrow, citation/consistency-only pass, mirroring exactly the discipline already used to correct the proposal's own two citation defects in a prior round) directly to the same two locations, then perform one final, narrow confirmation check — not a full re-review — verifying only that the "zero new platform subsystems" and "three-row table is exhaustive" statements are now consistent with the amended §12/§4 tables. Only after that confirmation should Steve perform local verification, staging, commit, and push.
