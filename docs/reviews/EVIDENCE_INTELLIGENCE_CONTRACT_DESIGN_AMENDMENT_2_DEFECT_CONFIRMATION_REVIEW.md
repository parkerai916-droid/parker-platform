# Evidence Intelligence Contract Design Amendment 2 — Defect Confirmation Review

## Status

**Narrow defect-confirmation review only. No file modified.** Not a fresh constitutional review; does not reopen any question already settled by the prior Independent Constitutional Review. No Kotlin implemented. No Gradle run. Nothing staged, committed, or pushed.

---

## 1. Repository Baseline

`main` at `eac43cbbb9d4e02731608769107fc44b89f367af` (`eac43cb`). Working tree matched the expected state exactly before this review began:

```
 M docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md
 M docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md
?? docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_AMENDMENT_2_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_OCR_MECHANISM_AMENDMENT_PROPOSAL.md
?? docs/reviews/EVIDENCE_PROCESSING_OCR_OWNERSHIP_AND_SEQUENCING_REVIEW.md
?? docs/reviews/EVIDENCE_PROCESSING_UNIT_5_OCR_PLANNING_REVIEW.md
```

No discrepancy.

---

## 2. Review Scope

This review verifies only whether the four items in the Independent Constitutional Review's §14 ("Required Corrections, if any") were applied accurately and narrowly, plus a regression check against categories that must not have been disturbed. It does not re-examine amendment fidelity, ownership analysis, public-model analysis, or citation accuracy in general — all already settled by the prior review — except where the correction pass itself touched adjacent text.

---

## 3. Correction 1 Verification — Contract Design Final Recommendation

**Required:** distinguish "no new first-class or peer subsystem" from "one additional capability-level dependency within Evidence Intelligence," and not describe the OCR dependency as a subsystem.

**Found, at the Final Recommendation section:**

> "...interface, zero new first-class or peer platform subsystems — Amendment 2 (Status, above) authorises one additional capability-level dependency inside the existing Evidence Intelligence subsystem, never a subsystem of its own — no amendment to any existing contract introduced *by this document*..."

Both required distinctions are present, in the required direction: "zero new first-class or peer platform subsystems" (the general claim, now correctly qualified) and "one additional capability-level dependency inside the existing Evidence Intelligence subsystem, never a subsystem of its own" (the OCR dependency, explicitly denied subsystem status). No other clause in the Final Recommendation was touched — confirmed by diff; the sentence continues unchanged into the Reasoning Provider Contract Design cross-reference immediately following.

**Result: PASS.**

---

## 4. Correction 2 Verification — Scope Lock §3 Dependency-Table Statement

**Required:** correct "the three-row table is exhaustive" to accurately describe the now-four-row table, without changing any exclusion category or authority.

**Found, at Scope Lock §3, the "Planning, or any dependency on Planner Runtime" row:**

> "Not named in Contract Design §12's dependency table; that table (four rows, following Amendment 2) remains exhaustive"

Accurate: the table has four rows (confirmed by direct count of `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` §12's own table). The exclusion itself — Planner Runtime is not a dependency — is unchanged; no new authority is granted or implied. No other row in the exclusion table was altered (confirmed by diff — this is the only changed line in that table).

**Result: PASS.**

---

## 5. Correction 3 Verification — Constitutional-Tier Reasoning

**Required:** the Amendment 2 Status text must state that CDR-006/CDR-007 classified first-class subsystems; that the OCR dependency is capability/module-tier; that it is analogous in tier to `ReasoningProvider`; that no new CDR is required; and must cite the Constitution's "Parker owns authority. Modules provide capability" principle accurately.

**Found**, a new paragraph in the Contract Design's Amendment 2 subsection:

> "CDR-006 and CDR-007 each performed a subsystem-tier constitutional classification — deciding that Evidence Custodian, and then Evidence Intelligence, exist at all as first-class, peer subsystems (CDR-007 §1). The OCR mechanism this amendment authorises is not a peer subsystem of that kind; it is a capability-level dependency *within* the existing Evidence Intelligence subsystem, constitutionally analogous in tier to `ReasoningProvider` — which itself required no CDR of its own to be authorised as a dependency (§12, below). This mirrors the Constitution's own 'Parker owns authority. Modules provide capability' principle (Core Principles)... On this basis, no operative CDR-007 decision changes, and no constitutional-tier document changes; this remains a Contract Design evolution only..."

Checked against each required element:

- CDR-006/CDR-007 classified first-class subsystems — present, by name.
- OCR dependency is capability/module-tier — present ("capability-level dependency *within* the existing Evidence Intelligence subsystem").
- Analogous in tier to `ReasoningProvider` — present, by name.
- No new CDR required — present in substance ("no operative CDR-007 decision changes... Contract Design evolution only"); not stated as the literal phrase "no new CDR is required," but the meaning is unambiguous and equivalent.
- Constitution citation accuracy — **verified directly against `docs/architecture/parker-constitution.md`**: "Parker owns authority. Modules provide capability." is the exact heading of a subsection under "## Core Principles" (confirmed by the Constitution's own table of contents). The citation "(Core Principles)" names the correct parent section and makes no claim about a specific subsection name — this avoids the earlier, now-corrected misattribution pattern (a prior draft of the related proposal had wrongly cited "Architectural Responsibilities"; this text does not repeat that error).

**Result: PASS.**

---

## 6. Correction 4 Verification — Deferred Governance Questions

**Required:** separately and explicitly preserve as unresolved (a) owner control and authorisation for machine-triggered OCR invocation, and (b) permission gating and disposition of OCR output rejected during output-quality validation; neither resolved or pre-authorised.

**Found**, in the amendment's non-authorisation paragraph:

> "Two further questions remain separately, individually unresolved, neither one collapsed into the other or into 'orchestration' generally: **owner control and authorisation for any machine-triggered OCR invocation** — an open question against Constitutional Test 1, not decided by this amendment and not decided by anything it relies upon; and **permission gating and disposition of OCR output rejected during output-quality validation** — whether such a rejection requires its own disclosed `PermissionAction`/`ResourceType` pairing, distinct from ordinary analytical judgment, is not decided by this amendment."

Both questions are named individually, each with its own bolded label, each explicitly marked as undecided. Neither authorises a coordinator, provider, result type, runtime path, or validation mechanism — no such Kotlin-shaped or concrete name appears anywhere in the added text (confirmed by grep: zero matches for `OcrMechanism`, backticked PascalCase `Ocr*` identifiers, "OCRmyPDF," "Docling," or "Tesseract" across both files).

**Result: PASS.**

---

## 7. Regression Check

Checked directly against the current text of both files:

| Category | Status |
| --- | --- |
| Parser truth authority | Unaffected — "never a truth authority... no parser, no OCR engine, and no external library... ever possesses authority over truth" present, untouched by the correction pass, in both files' table rows and the Status paragraph |
| Original-evidence immutability | Unaffected — "CDR-006's own classification of original evidence custody and immutability is unaffected and unreopened" present, untouched |
| Evidence Processing ownership | Unaffected — "Evidence Processing's own ownership of OCR detection... is likewise unaffected; this amendment touches no Evidence Processing governance document" present, untouched |
| Evidence Custodian ownership | Unaffected — "Evidence Custodian retains exclusive ownership of any accepted OCR-transcription artefact after acceptance, unchanged" present, untouched |
| Provenance ownership | Unaffected — "No ownership change is made to `EvidenceArtifactId`, `CandidateEvidenceArtifact`, `Provenance`, or any Memory Core or Knowledge Memory type" present, untouched |
| Public-contract expansion | None — no new `EvidenceAnalysisResult` variant, public type, or interface introduced by the correction pass |
| Concrete provider selection | None — grep confirms no OCRmyPDF/Docling/Tesseract/Kotlin identifier anywhere in either file |
| Implementation authority | None — `PermissionAction`/`ResourceType` appear only as citations to already-existing, already-governed Parker vocabulary (used identically elsewhere in the same document, e.g. the CDR-005 Model C paragraph), not as new authority |
| CDR-006/CDR-007 reopening | None — both cited only to state their existing classifications are unaffected and not reopened |
| Contract Design / Scope Lock mismatch | None — the new OCR dependency row is byte-identical in both files (confirmed by direct diff comparison); the two files' Status-subsection prose differs in length only, consistent with the established "identical table, non-identical surrounding prose" mirroring pattern the original amendment already used |

No new defect found in any regression category.

---

## 8. Findings

All four required corrections were applied accurately, narrowly, and without introducing new constitutional reasoning beyond what the proposal (§11 in particular) and the Constitution already supply. No unrelated wording was changed in either file beyond the four targeted locations. No regression was introduced in any of the ten checked categories. A repository-wide re-check for the same "exhaustive"/"zero new subsystem" staleness pattern found no further unaddressed instance in either file.

No new finding.

---

## 9. Verdict

**READY FOR FINAL ACCEPTANCE**

---

## 10. Recommended Next Step

No further governance-document work is required before Steve performs local verification, staging, commit, and push of Amendment 2 (both `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` and `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`). Future governance — the OCR mechanism's own Contract Design, invocation/orchestration design, provider selection, permission gating, and output-quality validation — remains correctly deferred and unauthorised, to be addressed as separate, later governance stages.
