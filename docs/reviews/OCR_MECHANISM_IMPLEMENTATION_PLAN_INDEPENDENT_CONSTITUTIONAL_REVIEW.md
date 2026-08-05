# OCR Mechanism Implementation Plan — Independent Constitutional Review

## Status

**Independent review only. No file modified.** `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md` is reviewed, not edited. No implementation performed. No Kotlin written. Nothing staged, committed, or pushed.

---

## 1. Repository Baseline

`main` at `172eb70de1943bc0aeeb270200bf265c71c83674` (`172eb70`). Working tree matched the expected state exactly before this review began:

```
?? docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md
```

No discrepancy.

---

## 2. Files Reviewed

Parker Constitution; CDR-006 (full); CDR-007 (full); `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` (full); `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` (full, current corrected text, re-read fresh for this review); `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` (as amended by Amendment 2); `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` (as amended); `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`; `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`; `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md` (the subject, full, re-read fresh).

---

## 3. Files Created

None.

## 4. Files Modified

None.

---

## 5. Findings

### 5.1 Constitutional consistency, ownership, and authority boundaries — sound

The plan does not reopen, redesign, or reinterpret the Contract Design, the Scope Lock, CDR-006, CDR-007, or Amendment 2 anywhere. Every objective in §3 traces to a specific Scope Lock or Contract Design section. Ownership boundaries are preserved throughout: Unit 4 explicitly denies any `EvidenceCustodian` dependency; Unit 8 explicitly denies any Memory Core dependency and explicitly states no `Provenance` value is ever constructed by Units 1–8; Unit 9 exists solely to prove, structurally, that none of Scope Lock §13's eight excluded dependencies is present. Authority boundaries — no truth determination, no acceptance, no rejection, no self-triggering, no background work — are restated correctly and consistently across every unit that touches them. No file in the "Files That Must Not Change" list (§13 of the plan) is targeted by any unit.

### 5.2 Implementation sequencing — a genuine, well-reasoned improvement, correctly disclosed

The plan's own §7 reframes four of the task's twelve suggested units (4, 8, 9, 10) and structurally blocks a fifth (12), each with a specific citation to the Scope Lock provision that would otherwise be violated by the task's literal framing. This is not scope creep — in every one of the four reframings, the effect is to *narrow* what the unit builds relative to its suggested name, never to broaden it. Unit 9 in particular correctly recognises that "Evidence Custodian integration," as named by the drafting task, would directly violate Scope Lock §13 if built as a literal integration, and reframes it into the structural-absence proof that §13 actually calls for. This is the review's single strongest finding in the plan's favour.

### 5.3 Unit ordering and dependency correctness — one genuine gap

**Unit 5 (OCR Execution Pipeline) lists its own dependencies as "Units 1, 2, 3, 4" — not including Unit 7 — yet Unit 5's own Verification Requirements explicitly demand end-to-end tests covering "each of the seven failure distinctions."** Unit 7 (Failure Handling) is the unit that "design[s] the concrete representation of the Scope Lock's seven non-collapsible failure distinctions," and is sequenced numerically *after* Unit 5. Unit 1 does establish the seven distinctions "as a closed set" at the shape level, but Unit 7's own Purpose statement is explicit that the *concrete, testable representation* is not designed until Unit 7 — "the first tier at which Scope Lock §10 permits this." The plan does not state whether Unit 5's own failure-distinction tests are meant to operate on Unit 1's bare shape alone (in which case Unit 7's later "concrete representation" work is partially redundant with what Unit 5 already tested) or whether Unit 5 cannot, in practice, satisfy its own verification requirements until Unit 7 exists (in which case the dependency list is incomplete and the units are numbered out of the order they must actually be built in). This is an internal-consistency defect in the unit breakdown, not a constitutional one — nothing about it touches an ownership, authority, provenance, or immutability boundary — but it is squarely within the "unit ordering" and "internal consistency" categories this review was asked to check, and it would concretely confuse whoever attempts to build Unit 5 before Unit 7.

### 5.4 Cross-reference accuracy — two minor defects

- **§16 ("Blocked Work — Requires Future Governance") silently omits one of the Scope Lock's own eight §18 items.** The plan's Status section correctly states Scope Lock §18 "lists eight items that remain open," and §16 opens by claiming to be "consolidating every place above that names a block." Scope Lock §18's eighth item — "exact Kotlin names, method signatures, and file layout" — is not restated anywhere in §16's own seven-item list. In substance this omission is defensible (item 8 is ordinary engineering work each unit's own "Files expected to change" field already defers, not a governance blocker in the same sense as items 1–7), but the section's own framing ("every place above that names a block") is not literally true as written, since it silently drops one of its own cited source's eight items without acknowledging the omission.
- **Unit 2's citation "(§14, below)" for why writing a concrete provider adapter is "out of scope for this plan" does not match §14's actual content.** §14 ("Explicitly Prohibited") forbids "provider-specific APIs reachable from any public contract this plan defines" — a narrower claim about leakage, not a general prohibition on writing a concrete adapter at all. The more accurate basis for Unit 2's own claim is §16 item 6 ("Concrete provider identity and adapter implementation" — listed as blocked future work) or the Status section's own provider-neutrality principle. This is a citation-precision defect, not a substantive one — the underlying constraint (no concrete adapter is authorised by this plan) is correct and is stated correctly elsewhere; only the specific section pointer is imprecise.

### 5.5 Implementation leakage — one minor, precedented style observation

Unit 12's "Files explicitly prohibited" names `src/composition/ParkerRuntime.kt` — a real, existing repository file path — as a prohibition target. This document's own Style constraints (mirrored from the drafting task) state "No package names." Citing an existing path only as something that must *not* be touched is a materially different use from defining a new Kotlin name, and the intent is sound, but it is the same category of minor style-discipline finding this review's own predecessor (the OCR Mechanism Scope Lock's Independent Constitutional Review, Finding 4, regarding `submitOwnerMessage`) already identified and treated as non-blocking. Noted here for consistency, not severity.

### 5.6 Provider neutrality, provenance preservation, original-evidence immutability, structural safeguards, completion criteria, deferred-governance preservation, runtime authority, prohibited dependencies — sound

Each of these categories was checked independently against the Scope Lock's own corresponding section and found correctly and consistently preserved:

- **Provider neutrality** — no provider is named, selected, or preferred; §15's own compatibility statement (OCRmyPDF, Tesseract, EasyOCR, PaddleOCR, future providers) correctly avoids choosing among them.
- **Provenance preservation** — Unit 8 correctly frames disclosure-sufficiency rather than construction; no `Provenance`/`CandidateProvenance` value is ever built by Units 1–8.
- **Original-evidence immutability** — Unit 4 and Unit 9 jointly and correctly guarantee no write path to any original exists, under any condition, including failure.
- **Structural safeguards** — all five required proofs (§10 of the plan) are mapped to a specific unit's own test; safeguard 5 is honestly disclosed as only partially testable today, since no runtime component yet exists (Unit 12 blocked) — a transparent limitation, not a hidden gap.
- **Completion criteria** — correctly and explicitly independent of Unit 12, mirroring the Evidence Processing precedent the plan itself cites.
- **Deferred-governance preservation** — machine-triggered invocation, the `RequiresOcr` consumer, the Permission Engine proposal-class question, output-quality validation policy, and rejected-output permission gating are each correctly left open, with no unit silently resolving any of them.
- **Runtime authority / prohibited dependencies** — Unit 12 is honestly and completely blocked; no `ParkerRuntime` wiring, Resource registration, or `ActionVocabulary` registration is authorised anywhere in the document.

No finding was made in any of these seven categories.

---

## 6. Constitutional Verdict

**REQUIRES REVISION**

The plan's constitutional substance is sound: it faithfully converts the already-accepted Contract Design and Scope Lock into implementable units, correctly and transparently narrows four of the task's own suggested units to stay inside the Scope Lock's boundary, and correctly blocks the one unit (runtime composition) that cannot lawfully proceed today. The defects found are all textual or sequencing-level, not constitutional: one genuine unit-ordering/dependency gap (§5.3, Unit 5's premature reliance on Unit 7), two minor cross-reference imprecisions (§5.4), and one precedented, non-blocking style observation (§5.5).

---

## 7. Recommended Next Step

Apply a narrow, targeted correction pass — mirroring exactly the discipline already used for the OCR Mechanism Scope Lock's own two-round defect-correction cycle — addressing: (a) Unit 5's dependency list and verification requirements, either by adding Unit 7 as a dependency or by explicitly scoping Unit 5's own failure-distinction tests to Unit 1's shape-level representation only, deferring full failure-injection coverage to Unit 7 and Unit 11; (b) §16's own framing, to either include the Scope Lock's eighth `§18` item or explicitly disclose why it is intentionally excluded; (c) Unit 2's citation, corrected to point to §16 item 6 or the Status/Objective-level provider-neutrality principle rather than §14. Item (d), the `ParkerRuntime.kt` naming in Unit 12, may be corrected at the same time or left as-is at Steve's discretion, consistent with how the equivalent finding was treated as non-blocking in the Scope Lock's own review. Only after this pass should the plan's own status move from Draft toward acceptance, and only then should Units 1–11 be authorised to begin.

---

## 8. Confirmation No Implementation Occurred

No Kotlin, no interface, no method signature, no production or test file was written or modified during this review.

## 9. Confirmation Nothing Staged, Committed, or Pushed

Confirmed — only `Read`, `Write` (for this review document only), and read-only `Bash`/`grep`/`git status`/`git rev-parse` were used.

## 10. Final `git status --short`

```
?? docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md
?? docs/reviews/OCR_MECHANISM_IMPLEMENTATION_PLAN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```
