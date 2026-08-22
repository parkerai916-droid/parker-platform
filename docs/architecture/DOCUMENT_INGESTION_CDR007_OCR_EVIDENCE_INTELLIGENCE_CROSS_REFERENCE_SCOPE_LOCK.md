# Document Ingestion — CDR-007 / OCR / Evidence Intelligence Cross-Reference Scope Lock

## Status

**Draft for owner review. Not yet accepted, canonical, or
implementation-authorising.** Programme: **Document Ingestion —
Governance Alignment Unit 4**, scope-locking Alignment Amendment 5 only
(CDR-007 / OCR / Evidence Intelligence cross-reference), as identified by
`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md` §5 (adopted
`84cc061`) and by the Governance Amendment Map's own Sequencing note
("Amendment 5 requires only a cross-reference, not new governance
text"). No Kotlin is implemented, proposed as a diff, or changed by this
document. No dependency is added. No interface, port, adapter, or
persistence is created. No parser is installed. No evidence is ingested.
Alignment Amendments 3 and 4 remain out of scope and are not begun here.

**This document reopens, redesigns, or reinterprets none of:** CDR-006,
CDR-007, `EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`, `EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`,
`EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`,
`EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`,
`EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` and Amendments 1-2,
`EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, `OCR_MECHANISM_CONTRACT_DESIGN.md`,
`OCR_MECHANISM_SCOPE_LOCK.md`, `OCR_MECHANISM_IMPLEMENTATION_PLAN.md`,
`EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`,
`EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`,
`EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`,
`MEMORY_CORE_CONTRACT_DESIGN.md` and Errata 001-004, `MEMORY_CORE_SCOPE_LOCK.md`,
the RKS/QMD Contract Design/Scope Lock/Amendments, ADR-024, and the seven
documents already adopted at `84cc061`/`4faaeb8`/`1958730`. It amends none
of those seven either — it takes the cross-reference already sketched in
`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md` §5 and the
three-tier table already adopted in `DOCUMENT_INGESTION_PLUGIN_CONTRACT.md`
§9.1, and freezes them as their own dedicated governance instrument,
exactly as Units 2 and 3 already did for Amendments 1 and 2.

## 1. Executive Summary

Alignment Amendment 5 exists to prevent a future reader from drawing
either of two wrong conclusions about Document Ingestion's already-
adopted three-tier framework: that CDR-007 already covers the mechanical
formats (CSV, OOXML, MIME) it never considered, or that Document
Ingestion is silently reinterpreting CDR-007 by classifying them as
non-analytical. Neither is true. This scope lock freezes the exact,
narrow cross-reference that makes both readings impossible, changes
nothing in CDR-007 or any OCR/Evidence Intelligence governance, and
creates no new authority anywhere.

## 2. Frozen Objectives

1. CDR-007 is not reopened, narrowed, widened, or reinterpreted by this
   document in any respect.
2. OCR authority remains exactly, and only, where CDR-007 and OCR
   Mechanism governance already place it — inside Evidence Intelligence's
   analytical functions, gated by the OCR Mechanism boundary, not owned
   by Document Ingestion.
3. Evidence Intelligence authority remains exactly, and only, where
   CDR-007 and the Evidence Intelligence Contract Design already place
   it — analytical, proposing-not-asserting, never a truth authority.
4. Document Ingestion performs, authorises, or implies no semantic
   analysis, evidential assessment, inference, reasoning, factual
   reconciliation, or legal analysis, at any tier, under any
   circumstance.
5. Recognition and model-backed interpretation (Tier B) are routed to
   the existing OCR Mechanism/Evidence Intelligence boundary by Document
   Ingestion; they are never owned, performed, or gated by Document
   Ingestion itself.
6. No new authority, port, interface, or governance rule is created by
   this document.

## 3. Canonical authorities inspected fresh (this unit)

Read fresh, directly, for this scope lock — not from prior-unit summary:
`docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
(status header; classification text, lines 518-519; "Not a truth
authority," "Not a memory system," "Not a Reasoning Provider," "Not a
constitutional authority," lines 195-224); `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`
("Purpose and Scope," lines 220-241; §1 Responsibilities, line 245
onward; the four-way content-kind distinction, lines 672-703);
`docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("No caller-declared
confidence," line 74; "What the OCR mechanism's output is not," line
89); `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
Determination 1 and its Classification Boundary (§1, §2 — already
quoted and re-verified in Units 1 and 2, held fresh in this session's own
context and re-confirmed unchanged); `docs/reviews/EVIDENCE_PROCESSING_OCR_OWNERSHIP_AND_SEQUENCING_REVIEW.md`
§12 (the disclosed "currently paused" staleness correction). Cross-
checked against, and confirmed still unmodified since their own
adoption: `DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md` (§5, its
own Amendment 5 sketch, and §0's Evidence Intelligence status
correction), `DOCUMENT_INGESTION_GOVERNANCE_AMENDMENT_MAP.md` (Amendment
5's own row and the Sequencing section), `DOCUMENT_INGESTION_PLUGIN_CONTRACT.md`
§9.1 (the three-tier table), `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
§7 item 5, `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` §2's
tier-classification paragraph and §8 Owner Decision 6, `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
§10 item 4 (Tier B conditional field), and `DOCUMENT_INGESTION_DERIVATIVE_REVIEW_TARGET_SCOPE_LOCK.md`
§8 (OCR authority untouched by review). `EvidenceCustodian.kt` (full
file, already fresh in this session, re-confirmed unchanged: `accept`/
`retrieve` only, no mutation path, `AcceptedEvidenceArtifact` two fields).

## 4. Core question findings

**A. Exact CDR-007 rule/boundary cross-referenced.** CDR-007, lines
518-519: "\[document ingestion, OCR, transcription, and extraction\] are
classified as Evidence Intelligence's own analytical functions." This is
the operative classification the entire cross-reference turns on. It is
broad and unnarrowed by CDR-007 itself — the narrowing exists only in a
separate, subordinate document (Boundary Clarification Determination 1),
not in CDR-007.

**B. OCR boundary.** OCR authority — invoking recognition, selecting a
provider, judging recognition quality, deciding whether a recognition is
worth producing as a candidate — remains exclusively inside the OCR
Mechanism/Evidence Intelligence boundary CDR-007 and OCR Mechanism
governance already establish; Document Ingestion acquires none of it.
What Document Ingestion **may** record without acquiring that authority:
that a Tier B transformation occurred (the Plugin Contract §5
`OCR`/`MODEL_INFERENCE` transformation-class disclosure, unmodified);
that a generation is OCR-derived (`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`'s
own I-7 "OCR identity" invariant and Unit 2's Derivative Generation
Record conditional model-identity field, both unmodified); routing
metadata (which capability was selected, per
`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` §2, unmodified).
None of this constitutes performing, gating, or judging recognition —
only disclosing, honestly and without embellishment, that recognition
was routed to and occurred elsewhere, under existing authority.

**C. Evidence Intelligence boundary.** Evidence Intelligence's authority
— analysis, comparison, extraction (in EI's own analytical sense),
transcription judgement, contradiction detection, chronology
construction, and proposing (never asserting) candidate propositions —
remains exclusively where CDR-007 and the Evidence Intelligence Contract
Design already place it. CDR-007 itself further confines even Evidence
Intelligence's own authority: its outputs are "at most, candidate
propositions carrying a provisional evidential characterisation; final
evidential-state assignment remains exclusively Knowledge Memory's"
(CDR-007, lines 207-210). Document Ingestion acquires none of Evidence
Intelligence's authority, and — the same relation CDR-007 already fixes
between EI and Knowledge Memory — acquires none of Knowledge Memory's
either.

**D. What Document Ingestion may perform, authorise, or imply.**
Distinguished precisely, per tier (Plugin Contract §9.1, unmodified):

- **Recognition, interpretation (Tier B only):** Document Ingestion may
  **route to** the existing OCR Mechanism/Evidence Intelligence
  boundary for these — it does not itself perform, own, or newly
  authorise them. Declaring Tier B capability in an ingestion
  descriptor is a claim only; invocation authority remains entirely
  with the existing boundary (Plugin Contract §4, unmodified).
- **Semantic analysis, evidential assessment, inference (in the
  evidentiary sense), reasoning, factual reconciliation, legal
  analysis:** never, at any tier, under any circumstance. These are
  Tier C, and Tier C is never entered by Document Ingestion (Plugin
  Contract §9.1; Unit 2 §7's own "no cross-source combination" rule;
  Unit 3 §8, unmodified).
  Document Ingestion's own reconciliation-generation concept (Unit 2
  §16) is explicitly a **mechanical** act — naming two prior
  generations as parents of a new generation — never an evidential
  judgement about which is more credible or true; the two are not the
  same thing and must never be conflated.

**E. Distinguishing the four categories.** Already adopted, unmodified,
restated for this cross-reference:

| Category | Owner | Governing authority |
| --- | --- | --- |
| Mechanical/structural ingestion transformation (Tier A) | Document Ingestion | Boundary Clarification Determination 1, extended by descriptive analogy (Unit 1 §5.5) |
| OCR recognition (Tier B) | OCR Mechanism, gated by Evidence Intelligence's authorisation boundary | CDR-007 (unnarrowed); OCR Mechanism Contract Design/Scope Lock |
| Evidence Intelligence analysis | Evidence Intelligence | CDR-007; Evidence Intelligence Contract Design |
| Downstream reasoning/analysis (Tier C) | Never Document Ingestion; a separately governed downstream mechanism if it exists at all | Plugin Contract §9 |

This table adds no new category and reassigns no existing one; it only
displays, in one place, ownership already fixed elsewhere.

**F. Whether Amendment 5 requires new governance.** **Textual cross-
reference/clarification only.** Confirmed independently, not merely
assumed: (1) the Governance Amendment Map's own Sequencing section
already states this ("Amendment 5 requires only a cross-reference, not
new governance text"); (2) the Canonical Governance Alignment document's
own §5.5 already states "No change to CDR-007 or the Boundary
Clarification. Record, as a cross-reference..."; (3) independent fresh
re-reading of CDR-007, the Boundary Clarification, the OCR Mechanism
governance, and the Evidence Intelligence Contract Design in this unit
found every rule this document relies on already fixed, unnarrowed, and
unconflicted by anything Document Ingestion has adopted. No STOP
condition is triggered; drafting proceeds.

**G. Neither OCR nor Evidence Intelligence output gains evidential
authority merely by being recorded/referenced.** Confirmed. Recording
that a transformation occurred, that a generation is OCR-derived, or
that a completeness check passed is a **disclosure**, never an
**endorsement**. Unit 2 §15 already establishes this precisely for
confidence ("a working, transient disclosure, never evidential truth");
this document extends the identical principle to OCR/EI output generally
— appearing in a Derivative Generation Record's transformation history
or a routing-audit record confers no more evidential weight than the
mechanism that originally produced it already carried under its own,
unchanged governance.

**H. `AcceptedEvidenceArtifact`/Evidence Custodian authority unchanged.**
Confirmed by direct, fresh re-inspection of `EvidenceCustodian.kt`: the
interface still declares exactly `accept` and `retrieve`;
`AcceptedEvidenceArtifact` still carries exactly `evidenceArtifactId` and
`acceptedAt`; no mutation path exists. Nothing in this cross-reference
touches either.

**I. Human review/`APPROVED` semantics unchanged; not OCR/EI approval.**
Confirmed, and explicitly reinforced, not merely inherited silently:
Unit 3 §8 already states `APPROVED` "does not... grant OCR authority...
grant Evidence Intelligence authority." `APPROVED` remains solely a
human-verification signal for downstream consumers (Unit 3 §4.C,
unmodified) — it is never itself an act of OCR recognition or Evidence
Intelligence analysis, and reviewing an OCR-derived or EI-referenced
generation invokes neither.

**J. Conflicts/ambiguities/staleness/duplicated or expanded authority.**
One staleness was found and is disclosed here, not newly discovered:
the Boundary Clarification's own closing prose still calls Evidence
Intelligence "paused" — already identified and corrected in the
Canonical Governance Alignment document (§0) and restated accurately
throughout Units 2-3; this document's own citations (Section 3, above)
use the corrected, current status (EI live, Units 1-8 complete,
`analyseEvidence` composed in `ParkerRuntime.kt`) and introduce no new
staleness. No duplicated authority and no accidental authority expansion
were found anywhere in CDR-007, OCR Mechanism governance, the Evidence
Intelligence Contract Design, or the seven documents already adopted —
see Section 9 for the full conflict re-check.

## 5. The exact cross-reference being frozen

Formalising `DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md` §5.5
and `DOCUMENT_INGESTION_PLUGIN_CONTRACT.md` §9.1 as one dedicated,
citable governance record:

1. **Tier A (mechanical/deterministic parsing)** — MIME decoding, CSV
   field parsing, OOXML part reading, literal searchable-PDF text-layer
   reading — rests on the Boundary Clarification's Determination 1
   reasoning (mechanical file-format parsing, "no interpretation, no
   pattern recognition, and no judgment of any kind"), extended by
   descriptive analogy from the one case (PDF text-layer reading) the
   Boundary Clarification itself decided to the other mechanical formats
   in Document Ingestion's own scope, which neither CDR-007 nor the
   Boundary Clarification ever considered. This is analogy, not
   reinterpretation: no case CDR-007 or the Boundary Clarification
   actually decided is decided differently here.
2. **Tier B (recognition/model-backed processing)** — OCR, model-backed
   layout interpretation, Docling model-backed table/layout
   reconstruction — remains fully and exclusively inside CDR-007's
   existing, unnarrowed Evidence Intelligence/OCR Mechanism assignment.
   Document Ingestion's own Tier B routing (Plugin Contract §9.1,
   Routing Policy §2, Owner Decision 6) is a claim/routing act only; it
   neither invokes, gates, nor authorises recognition itself, and
   inherits whatever authorisation shape OCR Mechanism's own still-open
   Unit 12 governance eventually settles (recorded independently in the
   Canonical Governance Alignment document's own §6, "Pre-existing OCR
   PermissionAction question — inspection result," unmodified here).
3. **Tier C (evidential/legal reasoning)** — credibility, meaning, legal
   significance, conflict resolution, inference about truth or intent —
   is never entered by Document Ingestion at any tier, and this
   cross-reference creates no path by which it could be.

No case is decided here that CDR-007 or the Boundary Clarification did
not already decide or knowingly leave open on identical terms. No new
`PermissionAction`, `ResourceType`, interface, or authority pairing is
introduced.

## 6. Required / conditional / optional / forbidden semantics for this cross-reference

Unlike Units 2 and 3, this document defines no new record type or field
shape — it fixes an interpretive boundary, not a data structure. The
classification below governs how *any future Document Ingestion
governance or implementation* may treat the tiers, not a Kotlin type.

**A. Required.** Every future Document Ingestion governance document,
scope lock, or implementation must: classify every mechanical/deterministic
transformation as Tier A and every recognition/model-backed
transformation as Tier B, using the Plugin Contract §5 transformation
vocabulary as the basis for that classification, never an ad hoc label;
route Tier B invocation through the existing OCR Mechanism/Evidence
Intelligence boundary, never a self-constructed gate; disclose, honestly
and without embellishment, that a Tier B transformation occurred,
wherever transformation history is recorded (Unit 2 §11).

**B. Conditionally required.** Where a future format is classified Tier
A by descriptive analogy (as CSV/OOXML/MIME already were), the analogy's
own reasoning — why the format involves "no interpretation, no pattern
recognition, and no judgment of any kind," mirroring Determination 1 —
must be stated, not merely asserted, exactly as Unit 1 §5.5 already did
for the three formats it classified.

**C. Optional.** A future scope lock may cross-reference this document
by section number for brevity rather than restating its reasoning in
full; this is a convenience, not a requirement.

**D. Forbidden.** No future Document Ingestion governance or
implementation may: reclassify any Tier B transformation as Tier A to
avoid routing through the OCR Mechanism/Evidence Intelligence boundary;
reclassify any Tier C activity as Tier B to bring it inside Document
Ingestion's own authority; treat a disclosed Tier B occurrence as itself
constituting Evidence Intelligence's own analysis, acceptance, or
evidential characterisation; treat CDR-007 as narrowed, or the Boundary
Clarification as widened, beyond what Section 5 above states; construct
a new `PermissionAction`/`ResourceType` pairing for Tier B invocation
under Document Ingestion's own authority rather than deferring to OCR
Mechanism's own still-open Unit 12 governance.

## 7. Non-goals

Explicitly out of scope for this document:

- no reinterpretation, narrowing, or widening of CDR-007;
- no reinterpretation of the Boundary Clarification's Determination 1
  beyond the descriptive-analogy extension already adopted in Unit 1
  §5.5;
- no resolution of OCR Mechanism's own open Unit 12 governance (owner
  control/authorisation for machine-triggered OCR invocation; the
  composition-level coordinator; the `PermissionAction`/`ResourceType`
  pairing question) — recorded, not resolved, exactly as the Canonical
  Governance Alignment document's own §6 already recorded it;
- no Kotlin implementation of any kind — no sealed type, no interface,
  no gate;
- no Memory Core change (Amendment 3 not begun);
- no ingestion audit authority/implementation decision (Amendment 4 not
  begun);
- no Knowledge, RKS/QMD, or Reasoning Context change;
- no `EvidenceArtifact`, `AcceptedEvidenceArtifact`, Derivative
  Generation Record, or Derivative Review Target redesign — Units 1, 2,
  and 3 remain unmodified;
- no source mutation of any kind;
- no real-evidence ingestion;
- no persistence technology selection;
- no dependency addition;
- any Runtime Integration work of any kind.

## 8. Amendment 3/4 dependency check

**None found.** This cross-reference concerns exclusively the boundary
among Document Ingestion, CDR-007, OCR, and Evidence Intelligence — it
touches no Memory Core field or write-path question (Amendment 3's own
domain) and no ingestion-audit persistence question (Amendment 4's own
domain). Nothing in Sections 4-6 above required assuming, deciding, or
presupposing any Memory Core field shape, cross-reference mechanic, or
ingestion audit authority/storage decision. Amendment 5 is, and remains,
fully independent of Amendments 3 and 4, confirming the Governance
Amendment Map's own Sequencing note.

## 9. Conflicts discovered (full re-check)

**None.** Every rule in this document is a direct, unmodified
restatement of CDR-007, the Boundary Clarification, OCR Mechanism
governance, the Evidence Intelligence Contract Design, or the seven
already-adopted Document Ingestion documents, freshly re-verified in
Section 3 above. The one staleness identified (Section 4.J) was already
corrected in adopted governance (Unit 1 §0) and is not repeated by this
document. No duplicated authority, no accidental authority expansion, no
contradiction with `EvidenceCustodian.kt`, and no contradiction with
Units 2 or 3's own already-adopted OCR/Evidence Intelligence
cross-references (Unit 2 §10, Unit 3 §8) were found.

## 10. Constitutional self-certification

| Authority | Check | Result |
| --- | --- | --- |
| Parker Constitution | Parker owns authority; modules provide capability | Section 4.D/E: Document Ingestion routes to, never owns, Tier B authority |
| Epistemic Integrity | No fabrication; unknown stated as unknown | Section 4.G: disclosure, never endorsement, of OCR/EI output |
| CDR-006 | Original evidence custody/immutability frozen | Not reopened; unaffected by a cross-reference concerning derivatives only |
| CDR-007 | OCR/extraction assigned to Evidence Intelligence; EI not a truth authority | Section 4.A/C restates verbatim; not narrowed or widened (Section 5) |
| Evidence Custodian | Sole custodian; `accept`/`retrieve` only | Section 4.H; `EvidenceCustodian.kt` re-verified unchanged |
| OCR Mechanism governance | No caller-declared confidence; not itself a governed record | Section 4.B; Document Ingestion's own disclosure obligations do not contradict this |
| Evidence Intelligence Contract Design | Proposes, never asserts; four-way content distinction | Section 4.C, Section 4.E; Document Ingestion's tiers map onto, never duplicate, this taxonomy |
| Document Ingestion Units 1-3 (`84cc061`/`4faaeb8`/`1958730`) | Governing authority for this programme | Every section cites and restates, never contradicts, already-adopted rules |
| Memory Core, RKS/QMD, Reasoning Context | Untouched | Section 7; no field, write path, or boundary touched |
| ADR-024 | Modules never write directly to platform state | Unaffected; this document creates no write path |

## Final Recommendation

**READY FOR OWNER REVIEW** (scope-lock stage).
