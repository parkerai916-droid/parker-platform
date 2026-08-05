# Evidence Intelligence Contract Design Amendment 2 — Final Acceptance Confirmation

## Status

**Acceptance confirmation only. Not a review. Not an amendment.** No canonical text is edited, revised, broadened, or reinterpreted by this document. No new constitutional reasoning is introduced — every statement below restates a finding already recorded in the governance history this document indexes. No Kotlin is implemented. No Gradle is run. Nothing is staged, committed, or pushed.

---

## 1. Repository Baseline

`main` at `eac43cbbb9d4e02731608769107fc44b89f367af` (`eac43cb`). Working tree matched the expected state exactly before this confirmation was drafted:

```
 M docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md
 M docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md
?? docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_AMENDMENT_2_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_AMENDMENT_2_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_OCR_MECHANISM_AMENDMENT_PROPOSAL.md
?? docs/reviews/EVIDENCE_PROCESSING_OCR_OWNERSHIP_AND_SEQUENCING_REVIEW.md
?? docs/reviews/EVIDENCE_PROCESSING_UNIT_5_OCR_PLANNING_REVIEW.md
```

No discrepancy.

---

## 2. Amendment Subject

Evidence Intelligence Contract Design Amendment 2 authorises an **abstract, unnamed OCR mechanism** as a **capability-level dependency within the existing Evidence Intelligence subsystem** — structurally parallel to the already-authorised `ReasoningProvider` dependency, never a subsystem in its own right, and never itself given a Kotlin name, interface, or concrete provider.

---

## 3. Governance History

The following sequence is complete, in order:

1. **OCR Planning Review** (`docs/reviews/EVIDENCE_PROCESSING_UNIT_5_OCR_PLANNING_REVIEW.md`) — identified, without resolving, the constitutional and sequencing risks of introducing OCR into the Evidence Processing pipeline.
2. **OCR Ownership and Sequencing Review** (`docs/reviews/EVIDENCE_PROCESSING_OCR_OWNERSHIP_AND_SEQUENCING_REVIEW.md`) — resolved the unit-numbering collision and OCR ownership question, concluding OCR execution belongs to Evidence Intelligence (CDR-007) and recommending a narrow Evidence Intelligence Contract Design clarification as the governance vehicle.
3. **OCR Mechanism Amendment Proposal** (`docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_OCR_MECHANISM_AMENDMENT_PROPOSAL.md`) — identified the exact paragraphs requiring amendment.
4. **Proposal Independent Review** — an independent constitutional review of the proposal, verdict "Requires Revision," identifying premature Kotlin-shaped naming and two citation misattributions.
5. **Proposal Corrections** — the naming and citation defects corrected; a Final Independent Constitutional Review of the corrected proposal, verdict "Ready for Amendment" (after one further narrow citation-correction pass).
6. **Formal Amendment 2** — drafted in place in `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` and, mirrored, `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, following the same in-place pattern Amendment 1 established.
7. **Amendment 2 Independent Constitutional Review** (`docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_AMENDMENT_2_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`) — verdict "Formal Amendment Defect": sound constitutional substance, but two now-false statements left unedited elsewhere in the same canonical documents ("zero new platform subsystems" in the Final Recommendation; "the three-row table is exhaustive" in Scope Lock §3).
8. **Formal-Defect Correction** — the two statements corrected narrowly; constitutional-tier reasoning and the two previously-compressed deferred questions (machine-triggered invocation owner-control; output-rejection permission gating) added to the amendment's own text.
9. **Defect Confirmation Review** (`docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_AMENDMENT_2_DEFECT_CONFIRMATION_REVIEW.md`) — verified all four corrections applied accurately and narrowly, confirmed no regression, verdict "Ready for Final Acceptance."

---

## 4. Accepted Constitutional Changes

Amendment 2:

- adds exactly one capability-level OCR dependency to Evidence Intelligence's dependency table (Contract Design §12; mirrored, Scope Lock §4);
- preserves Evidence Processing's ownership of detecting that OCR is required (the `RequiresOcr` disclosure `EvidenceExtractionCoordinator.extract` already produces) — no Evidence Processing governance document is touched;
- preserves Evidence Intelligence's ownership of OCR execution, consistent with CDR-007 §1 and the Contract Design's own §1 "Producing candidate derivative artefacts";
- preserves Evidence Custodian's exclusive ownership of any accepted OCR-transcription artefact after acceptance;
- preserves provenance ownership — no change to `EvidenceArtifactId`, `CandidateEvidenceArtifact`, `Provenance`, or any Memory Core or Knowledge Memory type;
- preserves original-evidence immutability — CDR-006's own classification is unaffected and unreopened;
- preserves parser non-authority over truth — no parser, no OCR engine, and no external library this dependency may eventually name ever possesses authority over truth, is a constitutional classifier, or may assign `EvidentialState`;
- creates no new first-class or peer platform subsystem — the OCR mechanism is capability-level, constitutionally analogous in tier to `ReasoningProvider`, within the existing Evidence Intelligence subsystem, never a subsystem of its own;
- creates no new CDR requirement — CDR-006 and CDR-007 each performed subsystem-tier classification already; this amendment does not reopen either and requires no new Constitutional Decision Record.

---

## 5. Explicit Non-Authorisations

Amendment 2 does **not** authorise, and no future reader should treat it as having authorised:

- a concrete OCR provider;
- OCRmyPDF;
- Docling;
- any Kotlin contract, interface, or type name;
- implementation of any kind;
- runtime composition;
- provider selection;
- machine-triggered OCR invocation;
- owner-control rules for such invocation — an open question against Constitutional Test 1, named individually and left unresolved;
- output-quality validation mechanics;
- permission gating or disposition for OCR output rejected during output-quality validation — named individually and left unresolved;
- reporting of any kind.

Each remains future governance, to be produced separately, at the appropriate governance stage.

---

## 6. Review Results

- The **Amendment 2 Independent Constitutional Review** initially found a **formal amendment defect**: sound constitutional substance, undermined by two unedited, now-false statements elsewhere in the same canonical documents.
- The identified defects were **corrected** in a narrow, targeted pass, adding no new constitutional reasoning beyond what the accepted proposal and the Constitution already supplied.
- The **Defect Confirmation Review** verified all four corrections and found **no new findings** and no regression across parser truth authority, original-evidence immutability, Evidence Processing ownership, Evidence Custodian ownership, provenance ownership, public-contract expansion, provider selection, implementation authority, CDR-006/CDR-007 reopening, and Contract Design/Scope Lock consistency.
- **Final verdict of the Defect Confirmation Review: READY FOR FINAL ACCEPTANCE.**

---

## 7. Final Acceptance Decision

**Evidence Intelligence Contract Design Amendment 2 is accepted.**

- The amended `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` is **canonical**.
- The amended `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` is **canonical**.
- **Future OCR work remains blocked** until the separately identified future governance named in §5, above, is completed. No OCR mechanism may be implemented, invoked, or orchestrated under the authority of Amendment 2 alone.

---

## 8. Next Lawful Step

The next governance step — not authorised to begin by this document — is the **narrow OCR mechanism Contract Design**, owning the OCR dependency's own concrete contract (mirroring how the Reasoning Provider Contract Design owns `ReasoningProvider`), together with its associated Scope Lock work. That future Contract Design must, at minimum, settle: a concrete mechanism identity; the machine-triggered-invocation owner-control question (§5, above); and output-rejection permission gating and disposition. No implementation of any kind is authorised before that governance stage is drafted, independently reviewed, and accepted.
