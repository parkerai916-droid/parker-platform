# Document Ingestion Governance Amendment Map

## Status

**Draft for human review. Governance Unit 1 companion document.** This is a
review/amendment-map record, not itself a canonical document and not an
implementation authority. It consolidates the narrow amendments that
`DOCUMENT_INGESTION_PLUGIN_CONTRACT.md`, `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
and `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` identify as
prerequisites to implementation. It amends nothing itself. No frozen or
scope-locked canonical document is edited by this pass; each row below states
what a future, separately governed amendment must do.

**Status update (Document Ingestion — Canonical Governance Alignment, Unit
1):** Amendments 1-5 below have been drafted, following independent
re-verification against primary sources, as
`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`. That document is
itself still a draft awaiting owner acceptance; this map is unedited
otherwise.

## Method

Each row names the exact canonical document and clause the amendment touches,
states why the current text does not already cover the ingestion need, and
states the narrowest change that resolves it without reopening surrounding
constitutional scope. "Conflict" is distinguished from "silence": no row below
was found to contradict a canonical rule. Every row addresses silence — an area
the canon does not yet cover — not a correction of an existing rule.

## Amendment 1 — Evidence Artifact Contract: three-kind descriptive vocabulary

**Document:** `EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`.
**Current state:** Describes originals and derivatives; has no formal
source/derivative/child-source vocabulary and no `artifactType` field (removed
by design — see `EvidenceCustodian.kt` correction history).
**Why silence, not conflict:** The Constitutional Optimisation Safeguard already
forbids a derivative ever substituting for or displacing an original; three-kind
vocabulary only names distinctions the contract already assumes.
**Narrow amendment:** Add source/derivative/child-source as descriptive
governance vocabulary. Grant no new authority. Do not add a formal `artifactType`
enum to `EvidenceCustodian`'s existing types — that removal was deliberate and
this amendment does not reopen it.

## Amendment 2 — `DerivativeReviewRegistry`/`DerivativeReviewRecord`: broadened review-target identity

**Document:** `EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
Section 6; implemented in `src/interfaces/DerivativeReview.kt`.
**Current state:** `DerivativeReviewRecord.evidenceArtifactId: EvidenceArtifactId`
is the sole key; `DerivativeReviewRegistry.currentReviewState` takes only an
`EvidenceArtifactId` (confirmed in code, lines 56-58 and 81-84).
**Why silence, not conflict:** This registry was scoped to the searchable-PDF
unit's own custodied-derivative case. It was never asked to review a non-byte
structural derivative, so its narrow keying is not a defect — it is simply
insufficient for the new case Amendment 2 of the Provenance Model introduces.
**Narrow amendment:** Introduce a review-target identity that is
`EvidenceArtifactId` for byte-backed derivatives or a new `DerivativeGenerationId`
for non-byte structural derivatives (a closed two-member sum type, not an open
string), and widen `DerivativeReviewRecord`/`DerivativeReviewRegistry` to accept
either. Do not change `DerivativeReviewState`'s four-value enum or the gating
rule that only `APPROVED` permits downstream treatment as human-verified. Do not
touch the searchable-PDF unit's existing byte-backed usage.

## Amendment 3 — Memory Core Provenance: ingestion cross-reference convention (no schema change)

**Document:** `MEMORY_CORE_CONTRACT_DESIGN.md` Section 7.
**Current state:** `Provenance.processingHistory` is a list of free-text,
additive-only audit entries; no field for plugin/adapter identity, software
version, or configuration digest exists, by design. `Provenance` is confirmed as
"the one, single place origin information lives" for cross-domain purposes.
**Why silence, not conflict:** Memory Core Provenance was never asked to carry a
structured, ordered, multi-generation ingestion transformation chain — the same
way it does not carry OCR's own detailed recognition-identity disclosure today.
**Narrow amendment:** None to Memory Core's schema. Document, as a convention in
Memory Core's own governance (or a short linking note), that an ingestion
coordinator populates `sourceIdentifier`/`extractedFromReference`/
`derivedFromReferences` and adds one `processingHistory` entry pointing to the
external ingestion Source Manifest / Derivative Generation record. This mirrors
the OCR precedent exactly and requires no field addition, no relaxation of
`processingHistory`'s free-text/additive-only shape, and no change to who may
call Memory Core (still gated solely by the existing
`PermissionAction.WRITE`/`ResourceType.MEMORY`/`DOCUMENT` check, evaluated
before Memory Core is invoked, never by Memory Core itself).

## Amendment 4 — Ingestion audit persistence port (new, Parker-owned)

**Document:** none amended; a new port is created, analogous to
`FileSystemEvidenceDeletionAudit`.
**Current state:** No general `AuditService` implementation exists anywhere in
the platform (ADR-024). Evidence Custodian's own deletion audit already uses a
purpose-built durable log (`FileSystemEvidenceDeletionAudit`,
`evidenceDeletionAuditLogPath`), not the generic, unimplemented `AuditService`.
**Why silence, not conflict:** ADR-024 states the constitutional Auditability
obligation but leaves each subsystem to supply its own durable mechanism until a
platform-wide one exists. Ingestion inherits the same obligation, not an
exemption from it.
**Narrow amendment:** Define a new, purpose-built ingestion-audit durable port
(attempt/correlation ID, source identity, routing/outcome/completeness facts per
Routing Policy Section 5) following the `FileSystemEvidenceDeletionAudit`
precedent's shape, owned by Parker's ingestion coordinator. Do not wait for or
depend on a general `AuditService`; do not let a plugin author or hold this port.

## Amendment 5 — CDR-007 / OCR Mechanism: no reinterpretation, explicit cross-reference

**Document:** none amended in substance. `EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
Determination 1 already draws the mechanical/interpretive line this programme
needs; `CDR-007` already assigns OCR/transcription/extraction to Evidence
Intelligence without narrowing.
**Current state:** Confirmed by direct reading: CDR-007's assignment is broad
("document ingestion, OCR, transcription, and extraction are Evidence
Intelligence's own analytical functions"); the Boundary Clarification narrows
only "deterministic extraction of an already-encoded PDF text layer," and states
its determination "is narrow and does not extend to OCR, transcription,
translation, or any technique that requires interpreting a representation of
content rather than reading content already encoded as text."
**Why silence, not conflict:** The ingestion drafts apply the identical,
already-accepted reasoning to the other Tier A mechanical formats in scope (CSV,
OOXML, MIME structural decoding) without asking CDR-007 or the Boundary
Clarification to say anything new about OCR or Docling.
**Narrow amendment:** None required to CDR-007 or the Boundary Clarification
themselves. Record, as a cross-reference in a future Document Ingestion scope
lock, that Tier A routing decisions rest on the Boundary Clarification's existing
Determination 1 reasoning, extended by descriptive analogy, and that Tier B (OCR,
Docling) remains fully inside CDR-007's existing Evidence Intelligence/OCR
Mechanism boundary, unchanged.

## Amendment 6 — OCR-specific `PermissionAction`/`ResourceType` pairing (pre-existing open question, not created by ingestion)

**Document:** `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` Amendment 2;
`OCR_MECHANISM_CONTRACT_DESIGN.md` Section 10; `OCR_MECHANISM_SCOPE_LOCK.md`
Sections 12/18.
**Current state:** Already on record as unresolved: "whether such a rejection
requires its own disclosed `PermissionAction`/`ResourceType` pairing, distinct
from ordinary analytical judgment, is not decided by this amendment."
**Why this is not an ingestion-created problem:** Document ingestion's Tier B
gate (Amendment 5 above; Owner Decision 6) depends on this question being
resolved, but does not itself need to resolve it — ingestion routes to the
existing gate whatever shape that gate ultimately takes.
**Narrow amendment:** None proposed here. Flagged as a dependency: the OCR
Mechanism/Evidence Intelligence programme's own unresolved question should be
closed before, or in step with, an ingestion scope lock that routes to it.

## Non-amendments (explicitly not required)

- CDR-006 is not reopened. Original-evidence custody and immutability while
  retained remain exactly as frozen.
- `AcceptedEvidenceArtifact` is not extended with hash/length/media-type/lineage
  fields. Its two-field minimalism is preserved; ingestion facts live in the new
  Source Manifest / Derivative Generation records (Amendments 2-3), never
  overloaded onto it.
- Evidence Custodian's sole-custodian status, owner-only deletion, and
  `OwnerEvidenceDeletionAuthority`'s isolation are not touched.
- RKS/QMD, `ReasoningKnowledgeSource`, and `DefaultReasoningContextAssembler`
  are not touched; ingestion has no relationship to their query-time retrieval
  scope beyond the existing prohibition on ingestion writing into them.
- ADR-024's module-write boundary is not amended; ingestion is confirmed to be
  an ordinary case of it, not an exception requiring new constitutional text.

## Sequencing

Amendments 1-4 must land, in this repository's existing convention, as their own
governance units (each with its own scope lock and, where a constitutional
boundary is touched, its own CDR or amendment to an existing CDR) before a
Document Ingestion implementation plan is written. Amendment 5 requires only a
cross-reference, not new governance text. Amendment 6 is a dependency to track,
not a deliverable of this programme.
