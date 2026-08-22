# Document Ingestion Programme — Canonical Governance Alignment (Unit 1)

## Status

**Draft for owner review. Not yet accepted, canonical, or
implementation-authorising.** Programme: **Document Ingestion — Canonical
Governance Alignment, Unit 1.** This document drafts the five narrow
canonical-governance amendments identified as prerequisites by
`DOCUMENT_INGESTION_GOVERNANCE_AMENDMENT_MAP.md`, following independent
re-verification of that map against primary sources (Section 0). It is a
governance instrument only: no Kotlin is implemented, proposed as a diff, or
changed; no dependency is added; no interface is implemented; no persistence
technology is chosen; no parser is installed; no evidence is ingested.

**This document reopens, redesigns, or reinterprets none of the following —
all remain frozen, unmodified, and unreopened:**
`docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`,
`docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`,
`docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`,
`docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` and its
Amendments 1-2, `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`,
`docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md`,
`docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md`,
`docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`,
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`,
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`,
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`,
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` and its Errata 001-004,
`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, the RKS/QMD Contract
Design/Scope Lock/Amendments, and `docs/adr/ADR-024-module-event-audit-durability-boundary.md`.
Every amendment below is a sibling clarification/vocabulary instrument,
following this repository's own established pattern (`EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`,
`EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`,
`MEMORY_CORE_CONTRACT_DESIGN_ERRATA_001.md`), never an edit to frozen text.

## 0. Method and independent re-verification

Each of the five areas below is worked through seven steps: (1) identify the
existing canonical authority; (2) quote/locate the current rule; (3) identify
the precise silence or interface mismatch; (4) establish why an amendment is
required; (5) draft the narrowest possible amendment; (6) verify existing
authority is not weakened; (7) verify no unrelated governance changes.

The amendment map was not assumed correct. Direct re-reading of primary
sources for this document surfaced two corrections/refinements to the map's
own account, both incorporated below rather than silently used:

1. **Evidence Intelligence is not paused.** The Evidence Processing Boundary
   Clarification's own closing prose calls Evidence Intelligence "currently
   paused." `docs/reviews/EVIDENCE_PROCESSING_OCR_OWNERSHIP_AND_SEQUENCING_REVIEW.md`
   §12 discloses this as a factual staleness in that frozen document's own
   prose: "Evidence Intelligence Units 1–8 are complete, verified, committed,
   and pushed," with a live `analyseEvidence` entry point in
   `src/composition/ParkerRuntime.kt`. Amendment 5 (Section 5, below) states
   the corrected, current status rather than repeating the stale "paused"
   characterisation the original three ingestion drafts inherited.
2. **OCR Mechanism Units 1-11 are complete; only Unit 12 (Runtime
   Composition) is blocked**, and it is blocked by eight named, pre-existing
   governance items — not by anything this programme creates. See Section 6.

No other correction to the amendment map's factual claims was found. Every
remaining citation in Sections 1-5 below was independently re-read against
the file and, where code-level, against the actual Kotlin rather than a
description of it.

## 1. Alignment Amendment 1 — Derivative Generation Record / Evidence vocabulary

**1.1 Existing authority.** `EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`
("the Contract Design"), status: "Accepted and frozen following independent
constitutional verification and Final Freeze Verification" (lines 11-13),
under CDR-006 Model B. `EvidenceCustodian.kt` implements
`AcceptedEvidenceArtifact` as exactly `evidenceArtifactId: EvidenceArtifactId`
and `acceptedAt: Instant` (lines 260-263) — independently re-confirmed by
reading the file directly.

**1.2 Current rule.** The Contract Design's Constitutional Optimisation
Safeguard (§8): "No derivative artefact, however accurate or convenient, is
ever substituted for, or used to justify discarding, the original it was
derived from." The Contract Design describes derivatives only descriptively
("an OCR output, a transcript, an extract, a summary, a thumbnail," §7) and
defines no `artifactType` field — deliberately removed by design, per
`EvidenceCustodian.kt`'s own correction history.

**1.3 Silence identified.** The Contract Design has no vocabulary
distinguishing (A) the authoritative source Evidence Artifact, (B) a
separately custodied byte-backed derivative or child source, and (C) an
immutable non-byte structural derivative with no single byte representation
(e.g. a MIME tree summary, a completeness manifest). This is silence — the
Contract Design was never asked this question — not a conflict: nothing in
it forbids naming these three cases.

**1.4 Why an amendment is required.** Without named vocabulary for (C), an
ingestion coordinator has only two choices: force every structural derivative
into `EvidenceArtifact` (contrary to the owner's Decision 1 that non-byte
derivatives must not be forced into custody merely to satisfy an API), or
leave (C) ungoverned. Both are worse than naming it.

**1.5 Narrowest amendment.** Adopt, as governance vocabulary only (no schema,
no Kotlin, no wire format):

- **Source Evidence Artifact** — the existing, unchanged `EvidenceArtifact`
  concept: authoritative original bytes, custodied by Evidence Custodian.
- **Derivative/child-source artifact** — the existing, unchanged case of a
  byte-backed derivative custodied via the existing `EvidenceCustodian`
  acceptance path, including a child source accepted under Amendment 5's
  precedent (Section 2 of `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`).
- **Derivative Generation Record** (new name) — an immutable,
  provenance-bearing record for a non-byte structural derivative, minted by
  the same Parker-owned ingestion coordinator that mints `EvidenceArtifactId`
  for byte-backed cases, never by a plugin. At governance level it must be
  capable of carrying: opaque immutable identity; source/parent lineage;
  derivative type; producer/plugin identity; producer version; adapter
  version where applicable; configuration identity; transformation
  identity/history; creation time; derivative content identity/hash where
  applicable; warnings/completeness status; and structural
  location/confidence where applicable. This is a capability list, not a
  frozen schema — the exact field shape is implementation-plan work, not
  fixed here, consistent with "do not prematurely freeze an implementation
  schema."

**1.6 Existing authority not weakened.** `EvidenceArtifact` and
`AcceptedEvidenceArtifact` are not redefined, extended, or given new fields.
The Constitutional Optimisation Safeguard is restated, not altered — a
Derivative Generation Record is, if anything, more clearly barred from ever
substituting for a source than an unnamed concept would be. CDR-006 and the
Contract Design's frozen status are untouched.

**1.7 No unrelated governance changed.** This amendment touches no field,
type, or authority outside the new, additive vocabulary above.

## 2. Alignment Amendment 2 — Derivative review target

**2.1 Existing authority.** `EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
§6 ("Human Review Lifecycle"), implemented in `src/interfaces/DerivativeReview.kt`.

**2.2 Current rule — re-verified directly against the code.**
`DerivativeReviewRecord` (lines 56-63):

```
data class DerivativeReviewRecord(
    val evidenceArtifactId: EvidenceArtifactId,
    val documentId: DocumentId,
    val state: DerivativeReviewState,
    val recordedAt: Instant,
    val reviewingPrincipalId: PrincipalId? = null,
    val note: String? = null,
)
```

`DerivativeReviewRegistry` (lines 81-85):

```
interface DerivativeReviewRegistry {
    suspend fun recordReviewState(record: DerivativeReviewRecord)
    suspend fun currentReviewState(evidenceArtifactId: EvidenceArtifactId): DerivativeReviewState?
}
```

The gating rule (file header, lines 26-31): no consumer may treat a
derivative's extracted text as human-verified unless
`DerivativeReviewRegistry.currentReviewState` for that derivative's own
`EvidenceArtifactId` returns exactly `DerivativeReviewState.APPROVED`.
`DerivativeReviewState` is a closed four-value enum: `PENDING_REVIEW`,
`APPROVED`, `REJECTED`, `NEEDS_CORRECTION` (lines 42-47).

**2.3 Silence/interface mismatch identified.** Both the record and the
registry's lookup operation are hard-typed to `EvidenceArtifactId`. This is
not a defect — the registry was built for the searchable-PDF unit's own
byte-backed derivative case and has never been asked to review a Derivative
Generation Record (Amendment 1) — but it is a genuine interface mismatch for
that new case: a `DerivativeGenerationId` cannot be passed where an
`EvidenceArtifactId` is required without pretending the two are the same
identity.

**2.4 Why an amendment is required.** Owner Decision 1 (prior review) and
this document's Amendment 1 forbid forcing non-byte structural derivatives
into `EvidenceArtifact`. Without broadening the review-target identity, such
a derivative could never enter the same governed human-review lifecycle
`DerivativeReviewState` already provides — an unacceptable gap, not a
justification for the forcing this programme already rejected.

**2.5 Narrowest amendment.** Introduce a closed, two-member review-target
identity — `EvidenceArtifactId` for byte-backed derivatives, or the new
`DerivativeGenerationId` (Amendment 1) for non-byte structural derivatives —
and widen `DerivativeReviewRecord` and `DerivativeReviewRegistry.currentReviewState`
to accept either, at governance level only (no Kotlin drafted here). This
review-target identity must remain closed (exactly two members), mirroring
`DerivativeReviewState`'s own closed-enumeration discipline (file comment,
lines 34-40: "deliberately unlike Memory Core's own open `entityType`/
`documentType` strings, because a downstream gate must be able to test for
exactly `APPROVED` without needing to know every string a future caller
might invent").

**2.6 Existing authority not weakened.** `DerivativeReviewState`'s four
values, the `APPROVED`-only gating rule, and the searchable-PDF unit's
existing byte-backed usage are all untouched. This is a widening of *what
may be reviewed*, never a change to *who may review* or *what review means*
— the Boundary Clarification's own "no consumer may treat extracted text as
human-verified" rule applies identically to both review-target kinds.
Because this amendment changes neither the custody/legal-ownership
distinction, the Custodian/Evidence Intelligence separation, the Memory Core
boundary, nor any Permission Engine gating requirement (the four triggers
the Evidence Custodian Scope Lock's own Change Control, §10, names as
requiring a new/amended CDR), it is a narrow implementation-level extension,
not a constitutional-boundary change.

**2.7 No unrelated governance changed.** No other Boundary Clarification
determination, and no other field of `DerivativeReviewRecord`, is touched.

## 3. Alignment Amendment 3 — Memory Core provenance cross-reference

**3.1 Existing authority.** `MEMORY_CORE_CONTRACT_DESIGN.md` §7 ("Provenance
Contract"), described there as "the mandatory, first-class origin record
every other Memory Core record must reference" and load-bearing to Memory
Core's own claim to be "the authoritative system of record" (lines 368-373).

**3.2 Current rule — exact field set, re-verified directly (lines
375-459).** Required fields: Identifier; Source identifier; Source type;
Creator (optional free text) and Creator principal reference (optional,
separate); Claimed creation time (optional); Acquisition time (required);
Ingestion time (required, Memory-Core-assigned); Derived-from references (a
list of `Provenance` identifiers, defaults empty); Extracted-from reference
(an optional `Document` identifier); Processing history (a list of free-text
audit entries, "additive-only... entries are appended, never removed or
rewritten," line 424); Integrity information (an optional hash of the
provenance record itself, "distinct from `Document.integrityHash`," line
428); Confidence (optional, of the provenance record itself); Content nature
(required, closed five-value enum `ORIGINAL`/`EXTRACTED`/`SUMMARISED`/
`INFERRED`/`UNKNOWN`); Sensitivity (optional).

**3.3 Silence identified.** No field carries plugin/adapter identity,
software version, or configuration digest. `Processing history` is
free-text and additive-only, not a structured, ordered, multi-generation
transformation chain. Memory Core's `Document` record (§5) "never
represents a document's parsed contents, extracted text, page structure, or
any interpretation of what the document says" (lines 251-256) — explicitly
deferred to "Document Handling's own, later, separate concern." This is
silence on ingestion-specific structured facts, not a prohibition on
recording them somewhere.

**3.4 Why an amendment is required.** Without a documented convention,
either Memory Core Provenance would need new fields (weakening its
deliberately generic, cross-domain shape and reopening a frozen contract to
serve one caller's needs) or ingestion facts would go unrecorded anywhere
authoritative. Neither is acceptable.

**3.5 Narrowest amendment.** No change to Memory Core's schema. Document, as
a convention (recorded here and cross-referenced from a future Memory Core
governance note, never edited into the frozen Contract Design itself), that
Parker's ingestion coordinator:

- populates the existing `sourceIdentifier` field with the source
  artefact's identity;
- populates `extractedFromReference` with the source `Document` identifier
  when applicable;
- populates `derivedFromReferences` with the immediate parent's `Provenance`
  identifier for derivative generations;
- appends exactly one `processingHistory` entry that names and points to the
  external ingestion Source Manifest / Derivative Generation Record
  (Amendment 1) holding the detailed facts.

This mirrors the already-established, already-accepted precedent for OCR:
OCR's own detailed recognition-identity and fidelity disclosure
(`OcrRecognitionIdentity`, `TranscriptionFidelity` — re-confirmed present in
`src/interfaces/OcrMechanism.kt`) already lives outside Memory Core
Provenance and is only referenced by it, never duplicated into it. Memory
Core need not duplicate parser configuration, OCR coordinates, the
transformation chain, parser warnings, or completeness accounting — all of
that remains Parker-owned ingestion provenance, cross-referenced, not
copied.

**3.6 Existing authority not weakened.** Memory Core Provenance remains,
unamended, "the mandatory, first-class origin record" and the sole
cross-domain origin-information authority. No competing "universal
provenance system" is created — the ingestion Source Manifest and
Derivative Generation Record are detail records referenced *from*
Provenance, never a parallel record claiming Provenance's own role. Who may
create a Provenance/Document record is unchanged: Memory Core's own gate
remains "any caller whose proposed action resolves... to
`PermissionAction.WRITE` on `ResourceType.MEMORY` or `DOCUMENT`, and is
`APPROVED` by `PermissionEngine.evaluate`" (Contract Design §10), evaluated
before Memory Core is invoked, never by Memory Core itself (Scope Lock §6:
"Memory Core never evaluates permissions"). This amendment does not touch
that gate; it only says what the already-permitted ingestion-coordinator
caller writes into fields the schema already has.

**3.7 No unrelated governance changed.** No field is added, removed, or
redefined in Memory Core's Contract Design, Scope Lock, or any Errata.

## 4. Alignment Amendment 4 — Parker-owned ingestion audit authority

**4.1 Existing authority.** ADR-024, and the implemented
`FileSystemEvidenceDeletionAudit` (re-confirmed present via
`evidenceDeletionAuditLogPath`, referenced across
`src/composition/ParkerRuntime*.kt` and its composition tests).

**4.2 Current rule — re-verified.** ADR-024: "no `AuditService`
implementation exists, so the Constitution's Auditability principle
currently has no durable mechanism behind it anywhere in the platform"
(line 33-35, as read directly). "What must eventually be durable: Memory
Records..., Principal records..., and an Audit log satisfying the
Constitution's 'every authorized action leaves a record sufficient to
reconstruct' guarantee" (Section D). Evidence Custodian's own deletion
audit already satisfies this for its own domain via a purpose-built
durable log (`FileSystemEvidenceDeletionAudit`), not the generic,
unimplemented `AuditService`.

**4.3 Silence identified.** No document assigns ownership of a durable
ingestion attempt-audit record. This is silence, not a conflict: ADR-024
states the general Auditability obligation and leaves each subsystem to
supply its own durable mechanism until a platform-wide one exists — exactly
what Evidence Custodian's deletion audit already did.

**4.4 Why an amendment is required.** Without a named authority
requirement, an ingestion coordinator could default to no durable audit at
all (violating the Constitution's Auditability guarantee) or a plugin could
be tempted to own its own audit trail (violating ADR-024's module-write
boundary and the "Parker, not the plugin, owns ingestion audit" owner
decision).

**4.5 Narrowest amendment.** Establish, as an authority requirement only —
no persistence technology chosen, no port implemented here — that a future
Parker-owned ingestion audit mechanism/port must record, at minimum where
applicable: source identity; digest; plugin identity/version; adapter
identity/version; configuration identity; authorisation context; start/end
time; outcome; warnings/errors; transformations; derivatives produced;
derivative identities/hashes; completeness/accounting result; child-source
acceptance attempts/outcomes; and resource/policy blocks where material.
Plugins may supply audit-relevant facts as part of their attempt outcome;
they may never own or persist the authoritative record — that authority
belongs exclusively to Parker's ingestion coordinator, following the
`FileSystemEvidenceDeletionAudit` precedent's shape rather than waiting on
a general-purpose `AuditService` that does not exist.

**4.6 Existing authority not weakened.** ADR-024's module-write boundary is
restated, not altered — this amendment is the ingestion-specific
application of a rule ADR-024 already establishes generally. No claim is
made that this authority requirement satisfies ADR-024's Auditability
guarantee until the port is actually implemented and durable; this document
creates no such implementation and makes no such claim.

**4.7 No unrelated governance changed.** No other subsystem's audit
obligations are touched. This amendment creates a new authority requirement
for a not-yet-existing port; it amends no existing port or contract.

## 5. Alignment Amendment 5 — CDR-007 / OCR / Evidence Intelligence cross-reference

**5.1 Existing authority.** CDR-007 (frozen); the Evidence Processing
Boundary Clarification (frozen); the OCR Mechanism Contract Design and Scope
Lock; the OCR Mechanism Programme Completion Review; the OCR Ownership and
Sequencing Review.

**5.2 Current rule — re-verified directly.** CDR-007, lines 518-519: "...are
classified as Evidence Intelligence's own analytical functions, including
document ingestion, OCR, transcription, and extraction." The Boundary
Clarification's Determination 1 (lines 55-67, quoted in full):

> "Deterministic extraction of an already-encoded PDF text layer is not an
> exercise of Evidence Intelligence's analytical functions. CDR-007's
> 'extraction' sits in a list with OCR, transcription, and translation —
> every other member of that list requires interpreting an image, a sound,
> or a language into a claim about its content. Reading text objects a
> searchable PDF already stores natively involves no interpretation, no
> pattern recognition, and no judgment of any kind; it is mechanical
> file-format parsing... This determination is narrow and does not extend
> to OCR, transcription, translation, or any technique that requires
> interpreting a representation of content rather than reading content
> already encoded as text."

The Boundary Clarification's own excluded-items list (§2) states: "This
document does not reopen, narrow, or reinterpret CDR-007, and does not
authorise the paused Evidence Intelligence Contract Design to resume" —
this "paused" characterisation is the stale prose corrected by Section 0
above; Evidence Intelligence is, at the time of this document, live
(Units 1-8 complete, `analyseEvidence` composed in `ParkerRuntime.kt`).

**5.3 Silence/status identified.** CDR-007 and the Boundary Clarification
already draw the exact mechanical/interpretive line the ingestion
programme's Tier A/Tier B distinction needs — for PDF text-layer reading
specifically. They are silent on whether the identical reasoning extends
descriptively to the other Tier A mechanical formats in the ingestion
drafts' scope (CSV field parsing, OOXML part reading, MIME structural
decoding) — none of which was in front of CDR-007 or the Boundary
Clarification when either was written.

**5.4 Why an amendment is required.** Without a recorded cross-reference,
a future reader could either (a) wrongly conclude CDR-007 already covers
CSV/OOXML/MIME mechanical parsing (it never considered them), or (b) wrongly
conclude the ingestion programme is reinterpreting CDR-007 by classifying
them as Tier A (it is not — it is applying the Boundary Clarification's own
stated reasoning to materially identical cases).

**5.5 Narrowest amendment.** No change to CDR-007 or the Boundary
Clarification. Record, as a cross-reference for a future Document Ingestion
scope lock: (a) Tier A (mechanical/deterministic parsing — MIME decoding,
CSV parsing, OOXML parsing, literal searchable-PDF extraction) rests on the
Boundary Clarification's Determination 1 reasoning, extended by descriptive
analogy to formats it did not itself decide, without reopening it; (b)
Tier B (recognition/model-backed processing — OCR, model-backed layout
interpretation, Docling model-backed table/layout reconstruction) remains
fully inside CDR-007's existing, unnarrowed Evidence Intelligence/OCR
Mechanism assignment and must route through that existing boundary; (c)
Tier C (evidential reasoning — credibility, legal significance, truth
determination, contradiction resolution, intent inference, semantic
evidence summarisation) is never entered by ingestion at any tier. This
integration boundary clarifies where ingestion's own tiers meet existing
authority; it creates no new reasoning authority and decides no case CDR-007
or the Boundary Clarification did not already decide or leave open on
identical terms.

**5.6 Existing authority not weakened.** CDR-007's broad OCR/transcription/
extraction assignment to Evidence Intelligence is restated, not narrowed.
The Boundary Clarification's narrow carve-out is restated at the same
narrowness, not widened beyond PDF text-layer reading by anything other
than descriptive analogy to cases it never considered. Tier B routing
through "the existing OCR Mechanism/Evidence Intelligence boundary" is
accurate to what exists (Section 6) but authorises no invocation by itself.

**5.7 No unrelated governance changed.** No field, outcome type, or
authority in CDR-007, the Boundary Clarification, the OCR Mechanism
Contract Design, or the Evidence Intelligence Contract Design is touched.

## 6. Pre-existing OCR PermissionAction question — inspection result

Inspected, not resolved, per the task's instruction. Two independent
sources were re-read directly:

- OCR Mechanism Contract Design §10 ("Deferred to Future Scope Lock Work"),
  items 1, 2, and 4: owner control/authorisation for machine-triggered OCR
  invocation; "[w]hether a disclosed-poor recognition... warrants its own
  dedicated `PermissionAction`/`ResourceType` pairing, distinct from
  ordinary analytical judgement, is not decided here"; "[w]hether a
  dedicated Permission Engine proposal class is required for OCR invocation
  specifically... a CDR-005 Model C question, not exercised by this
  document."
- OCR Mechanism Implementation Plan §16 ("Blocked Work — Requires Future
  Governance"), consolidating the same items (its own items 1 and 3
  correspond to Contract Design §10 items 1 and 4) into an eight-item list
  gating specifically **Unit 12 (Runtime Composition)** — the unit that
  would make OCR actually invocable in production. "Units 1-11 of this plan
  do not require items 1-7 to be resolved first... Unit 12 requires items
  1-3 at minimum."

**Finding: this question is real, currently open, and pre-existing.** It
was not created by, and is not particular to, the Document Ingestion
programme. OCR Mechanism Units 1-11 (the pure interface/computation layer —
`OcrMechanism`, `OcrProviderAdapter`, `OcrExecutionSequencer`) are complete
and independently usable for testing; only Unit 12 (wiring OCR to be
callable in a live, owner-authorised way, including whichever
`PermissionAction`/proposal-class shape items 1-3 eventually settle on) is
blocked. Docling additionally remains, independently, "Out of Scope"
(Contract Design §13, Scope Lock line 245) for the OCR Mechanism programme
itself — Tier B routing for Docling specifically depends on both this
question's resolution *and* a separate future extension of OCR Mechanism
governance to cover Docling at all, an earlier-stage dependency than the
`PermissionAction` question alone.

**Does it block this Unit's five amendments?** No. None of Amendments 1-5
above authorises, invokes, or depends on OCR runtime composition; Amendment
5 states only that Tier B *routes through* the existing (built, not yet
runtime-composed) boundary, which is true regardless of how items 1-3
resolve. The five amendments are ready for owner acceptance independent of
this question.

**Does it block adoption of the ingestion programme as a whole?** Partially,
and only for one specific case, inherited rather than created: no ingestion
capability may actually *invoke* Tier B (OCR or, later, Docling) processing
in production until OCR Mechanism's own Unit 12 governance is separately
resolved. Tier A (mechanical parsing) ingestion is unaffected and has no
dependency on this question. This is recorded here; it is left untouched,
as instructed — it belongs to the OCR Mechanism programme's own future
governance stage, not to this one.

## 7. Owner completeness decision — incorporation confirmation

The owner's now-resolved completeness decision (mandatory format-specific
minimum checks; failure semantics: no derivative destruction, no partial-
result erasure, no false "fully accounted for" claim, an explicit qualified/
incomplete outcome, and auditability) has been incorporated directly into
`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` §4, which now states
the mandatory minimum check per format (EML, CSV, DOCX, PDF, Scanned PDF,
Image — including the Scanned PDF row the prior draft's PDF/Image rows did
not separately distinguish) and the five failure-consequence rules verbatim
to the owner's own wording, and §8 item 5, updated from "partially resolved"
to fully resolved. Terminology is aligned with the existing five-state
`AccountedFor`/`AccountedForWithQualifications`/`KnownIncomplete`/
`NotAssessable`/`NotApplicable` vocabulary already adopted in the prior
governance pass — confirmed, by the prior independent review, to be novel
but non-conflicting (no existing canonical document defines a competing
"completeness" term).

## 8. Source manifest, child-source, multi-parser, and reprocessing — confirmation

These four models were already established in the prior governance pass and
are confirmed unchanged and consistent by this Unit:

- **Source manifest ownership** (`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
  §1, as amended): the Parker-owned ingestion coordinator owns/mints the
  authoritative source manifest; the parser does not. It records source
  Evidence Artifact identity; digest algorithm and digest as a pair; byte
  length; received/detected media type where available; original filename
  metadata where available; parent lineage where applicable; and ingestion
  correlation identity. SHA-256 is the current required algorithm,
  represented so that future algorithm agility requires no redesign. A
  digest-addressed manifest/envelope requires canonical, deterministic
  serialisation before its own digest carries evidential meaning — stated,
  not newly defined, in that document; no serialisation format is fixed by
  this Unit.
- **Child source model** (`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §1, §4;
  Owner Decision 2): a plugin may only propose candidate decoded bytes (e.g.
  an EML attachment). It cannot confer authority. Only Parker's explicit,
  separately governed acceptance through Evidence Custodian creates a
  custodied child source. The child source is authoritative only for its
  own accepted bytes; its lineage to the parent is permanent; its authority
  never extends backward to the parent's encoded representation.
- **Multi-parser rule** (`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §6;
  Invariant I-10): neither of two independent derivatives from one source
  (e.g. a Tika literal derivative and a Docling structural derivative from
  one searchable PDF) silently overrides the other. Material disagreement is
  preserved. A downstream reconciliation or synthesis, if persisted, is
  itself a new governed derivative. No "preferred truth" arises merely
  because one parser ran later.
- **Reprocessing/generation model** (`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
  §2, as amended; Invariant I-11): reprocessing always creates a new,
  immutable derivative generation; historical generations are never
  mutated. Generation identity is opaque and immutable, carried by an
  identifier plus an immutable parent link and creation time, never by a
  fragile sequential integer counter as the sole identity.

None of these four models required further amendment in this Unit; each was
independently re-checked against Section 1's Amendment 1 vocabulary and found
consistent (a Derivative Generation Record is a valid target for source
manifest attachment, child-source lineage, multi-parser coexistence, and
generation identity exactly as a byte-backed derivative already was).

## 9. Source immutability — unweakened

Every amendment above remains subordinate to: "No ingestion plugin may
modify, replace, normalize, or redefine the authoritative source artifact.
Every extraction, OCR result, structural interpretation, decoded
representation, and convenience form is a provenance-linked derivative."
No amendment in this Unit relaxes, qualifies, or creates an exception to
this rule. Amendment 1 makes it more precise (by naming what a "derivative"
may be, including non-byte cases) without narrowing what counts as
requiring provenance-linkage. Amendment 2 extends who may be *reviewed*, not
who may claim source authority. Amendments 3-5 touch no source-authority
question at all.
