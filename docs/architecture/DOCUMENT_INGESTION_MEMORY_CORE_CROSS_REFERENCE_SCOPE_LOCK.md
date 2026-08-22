# Document Ingestion — Memory Core Cross-Reference Scope Lock

## 1. Status and scope

**Draft for owner review. Not yet accepted, canonical, or
implementation-authorising.** Programme: **Document Ingestion —
Governance Alignment Unit 6**, scope-locking Alignment Amendment 3 only
(Memory Core cross-reference), the last of the five amendments
`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md` §3 identified. No
Kotlin is implemented, proposed as a diff, or changed by this document.
No dependency is added. No interface, port, adapter, persistence,
schema, or runtime wiring is created. No parser is installed. No
evidence is ingested. No implementation-plan unit and no programme
closure review begin here.

**This document reopens, redesigns, or reinterprets none of:** CDR-006,
CDR-007, CDR-004, CDR-008, `EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`,
`EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`, `EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`,
`EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`,
`EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` and Amendments 1-2,
`EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, `OCR_MECHANISM_CONTRACT_DESIGN.md`,
`OCR_MECHANISM_SCOPE_LOCK.md`, `EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`,
`EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`,
`EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`,
`MEMORY_CORE_CONTRACT_DESIGN.md` and Errata 001-004, `MEMORY_CORE_SCOPE_LOCK.md`,
the RKS/QMD Contract Design/Scope Lock/Amendments, ADR-024, and the nine
documents already adopted for Document Ingestion (`84cc061`/`4faaeb8`/
`1958730`/`ff589be`/`969e688`). It amends none of those either.

## 2. Authoritative sources inspected (fresh, this unit)

Read fresh, directly, for this scope lock: `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`
(§7 "Provenance Contract" in full, re-verified field-by-field with line
numbers; §10 "Who creates?"; §15/§16 boundary statements including the
Conversation-reference precedent, lines 115-127, and the Knowledge
Memory boundary, lines 903-913); `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`
(lines 157, 172: "Knowledge Memory reads Memory Core... never writes to
it"; "Memory Core never evaluates permissions"); `docs/decisions/CDR-004_CONSTITUTIONAL_CLASSIFICATION_OF_PROVENANCE_IDENTIFIER_RESOLUTION.md`
(the adopted classification, line 102); `docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md`
(Invariant 12, line 160: "Memory Core remains Parker's sole
authoritative system of record... No downstream relevance mechanism may
create, become, constitute, or be treated as a persistent parallel or
secondary source of canonical truth"); `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
(lines 207-210, already fresh-verified in Unit 4 and re-confirmed
unchanged this unit); `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
§4 (the `EvidenceRegistrationCoordinator`/`CandidateProvenance`/
`CandidateDocument` field-population precedent, re-read in full, exact
field names `extractedFrom`/`derivedFrom` confirmed directly against
`MEMORY_CORE_CONTRACT_DESIGN.md` lines 417-420, 465-466, superseding this
program's own earlier, slightly imprecise paraphrase "extractedFromReference"/
"derivedFromReferences"). Cross-checked against, and confirmed unmodified
since adoption: all six prior Document Ingestion documents in full
(`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md` §3 and §0;
`DOCUMENT_INGESTION_GOVERNANCE_AMENDMENT_MAP.md` Amendment 3's own row;
`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`; `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
§§2, 7, 17, 21; `DOCUMENT_INGESTION_DERIVATIVE_REVIEW_TARGET_SCOPE_LOCK.md`
§8, §22; `DOCUMENT_INGESTION_CDR007_OCR_EVIDENCE_INTELLIGENCE_CROSS_REFERENCE_SCOPE_LOCK.md`
§4; `DOCUMENT_INGESTION_AUDIT_AUTHORITY_SCOPE_LOCK.md` §22).

## 3. Frozen objective

Determine, from adopted governance alone, the minimum relationship
Document Ingestion requires with Memory Core and Knowledge. The burden
is on justifying any relationship, not on assuming one; no relationship
this document does not affirmatively derive from existing authority is
created.

## 4. Existing Memory Core/Knowledge authority

- **Memory Core's Provenance Contract** (§7) is "the mandatory,
  first-class origin record every other Memory Core record must
  reference" — required fields include `sourceIdentifier` (open-shaped,
  "a URI, a `ConversationId`/`TurnId`, a `DocumentId`, an external system
  reference"), `sourceType`, `derivedFrom` (a list of `Provenance`
  identifiers, defaults empty), `extractedFrom` (an optional `Document`
  identifier), `processingHistory` (free-text, additive-only),
  `contentNature` (closed enum: `ORIGINAL`/`EXTRACTED`/`SUMMARISED`/
  `INFERRED`/`UNKNOWN`), among others already fixed and unmodified by
  this document.
- **Write gate:** "Any caller whose proposed action resolves... to
  `PermissionAction.WRITE` on `ResourceType.MEMORY` or `DOCUMENT`, and is
  `APPROVED` by `PermissionEngine.evaluate`" (Contract Design §10) — a
  generic, caller-identity-neutral gate; "Memory Core never evaluates
  permissions" itself (Scope Lock §6/§172).
- **Existing reference-not-duplication precedent, already adopted, not
  invented here:** "Memory Core never constructs, mutates, or duplicates
  a `Conversation` or `Turn`. It may *reference* a `ConversationId`/
  `TurnId` as a Provenance source... never a structural dependency, never
  a copy of Conversation content into a Memory Core record" (Contract
  Design, lines 115-120).
- **Knowledge Memory boundary:** "Knowledge Memory owns... evaluation and
  promotion decisions over Memory Core content, and nothing about Memory
  Core's own record types, lifecycle, or provenance. Knowledge Memory
  reads Memory Core; it never writes to it. A future Knowledge Memory
  promotion produces a `KnowledgeRecord` — Knowledge Memory's own type,
  not a new Memory Core record — referencing whichever Memory Core
  records it was promoted from by identifier" (Contract Design, lines
  903-913). Symmetrically, "Memory Core never... constructs a
  `KnowledgeRecord`-equivalent, and it never reads Knowledge Memory's own
  state — the dependency runs one way only" (lines 121-125).
- **CDR-007:** Evidence Intelligence "proposes — never authoritatively
  asserts"; its outputs are "at most, candidate propositions carrying a
  provisional evidential characterisation; final evidential-state
  assignment remains exclusively Knowledge Memory's" (lines 207-210,
  already fresh-verified for Unit 4 and unchanged).
- **CDR-008 Invariant 12:** "Memory Core remains Parker's sole
  authoritative system of record for Memory Core records within its
  governed domain. No downstream relevance mechanism may create, become,
  constitute, or be treated as a persistent parallel or secondary source
  of canonical truth" — governs RKS/QMD directly, and by the identical
  reasoning forecloses any ingestion-adjacent mechanism from acquiring
  that status either.
- **Existing precedent for a Parker-owned coordinator populating these
  fields for a *byte-backed* derivative:** `EvidenceRegistrationCoordinator`
  (already implemented, `src/runtime/`), used by the searchable-PDF
  extraction coordinator, builds `CandidateProvenance`/`CandidateDocument`
  using only Memory Core's already-existing fields — `extractedFrom` (the
  original's `DocumentId`), `contentNature = EXTRACTED`, `creator` (free
  text), and a single structured `processingHistory` entry (the
  `ExtractionIdentity` record: parser identity, configuration profile,
  normalisation profile) — explicitly: "No new field is added to
  `Provenance`, `Document`, `CandidateProvenance`, or `CandidateDocument`"
  (Boundary Clarification §4). `CandidateProvenance.confidence` is left
  `null` for deterministic extraction, "inventing a numeric confidence
  figure... would be fabricated precision."

## 5. Existing Document Ingestion authority

Restated, unmodified, from Units 1-5: Evidence Custodian remains sole
custodian of authoritative source bytes (unaffected here). A Derivative
Generation Record "never becomes, is never treated as, and can never be
mistaken for... a Memory Core record" (Unit 2 §2 Frozen Objective 1) —
this is not reopened; it is restated. Derivative Review's `APPROVED` is
"solely a human-verification signal" (Unit 3 §4.C, §8), granting no
Evidence Custodian, Memory Core, OCR, Evidence Intelligence, or reasoning
authority (Unit 3 §8, verbatim). Document Ingestion's Tier A/B/C
framework (Unit 4) is unmodified: Tier C (evidential reasoning) is never
entered by ingestion. Ingestion audit (Unit 5) records that ingestion
events occurred; Unit 5 §22 explicitly deferred any Memory Core
relationship to this Amendment by name, recording the possibility of "an
opaque reference to an already-governed Memory Core identity" without
requiring or shaping one — this document now resolves that deferral
(Section 13).

## 6. Canonical ownership boundary

Restated as the load-bearing structure this entire document applies:
Evidence Custodian owns source bytes; Document Ingestion (Units 1-5) owns
source manifests, Derivative Generation Records, derivative review
history, and ingestion audit history; Memory Core owns Provenance/
Document/Entity/Assertion/Relationship records and is "the authoritative
system of record" for them (CDR-008 Invariant 12); Knowledge Memory owns
promotion decisions and `KnowledgeRecord`s. No ownership boundary already
fixed by adopted governance is moved by this document. The only question
this document resolves is the *reference* relationship between the
Document Ingestion domain and the Memory Core domain — never a transfer
of ownership in either direction.

## 7. Direct-write decision

**Document Ingestion does not require, and is not granted, any new
direct-write authority into Memory Core or Knowledge.** Specifically,
answering Question A directly:

- create/modify/delete Memory Core records: not ingestion's authority.
  Memory Core records, once created, are governed elsewhere (this
  document neither grants nor removes any mutation capability, because
  none exists to remove — Memory Core's own contract has no mutation
  operation for these record kinds at all);
- create/modify/delete Knowledge Items: never ingestion's authority —
  exclusively Knowledge Memory's, per Section 4 above, unaffected;
- assign canonical-memory status, assign evidential state, or promote
  extracted text or propositions into canonical memory: never ingestion's
  authority — evidential-state assignment is exclusively Knowledge
  Memory's (CDR-007, Section 4 above).

Where a Memory Core `Provenance`/`Document` record is ever created *for*
ingested content (Section 9, below), the **creating act is a Memory Core
registration act, governed by Memory Core's own existing write gate**
(Section 4) — it is not something Document Ingestion does *as*
ingestion, in the same sense accepting evidence is something Evidence
Custodian does. A Parker-owned coordinator may be authorised to perform
it (mirroring `EvidenceRegistrationCoordinator`'s already-adopted
precedent), but authorisation to do so comes from Memory Core's own
generic gate, not from any authority Document Ingestion's own governance
grants.

## 8. Candidate-information boundary

Confirmed and frozen as a five-stage, non-automatic chain, each stage a
separate, independently governed act:

1. **Mechanically extracted/generated derivative content** (Tier A/B) —
   a Derivative Generation Record or byte-backed derivative
   `EvidenceArtifact` (Units 1-2). Existing on its own; implies nothing
   about later stages.
2. **Reviewed derivative content** — a `DerivativeReviewRecord` reaching
   `APPROVED` (Unit 3). A human-verification signal only; implies
   nothing about later stages (Unit 3 §8, restated Section 5 above).
3. **Evidence Intelligence candidate propositions** — CDR-007's own
   "proposes, never asserts" output, produced only if Evidence
   Intelligence is separately invoked (never automatically triggered by
   1 or 2). Implies nothing about later stages (Section 4 above,
   CDR-007 lines 207-210).
4. **Memory Core `Provenance`/`Document` registration** — this
   document's own subject (Section 9). A separate, optional, later act;
   never implied by 1, 2, or 3.
5. **Knowledge Items** — a `KnowledgeRecord`, created exclusively by
   Knowledge Memory's own promotion decision, reading from Memory Core
   (Section 4). Never implied by 1, 2, 3, or 4.

No stage's success — ingestion, review, audit, or Evidence Intelligence
analysis — implies or triggers promotion into the next stage. This
directly answers, and forecloses, the presumption Question B warns
against.

## 9. Provenance cross-reference decision

**Existing Memory Core Provenance fields are sufficient; no new field is
required.** Following the `EvidenceRegistrationCoordinator` precedent
exactly (Section 4): where a Memory Core `Provenance`/`Document` record
is ever created for ingested content, it references ingestion-owned
identities using fields Memory Core's Contract Design already has:

- `sourceIdentifier` — an ingestion-owned `EvidenceArtifactId` or
  `DerivativeGenerationId` value, exactly as `sourceIdentifier` already
  accepts a `ConversationId`/`TurnId`/`DocumentId`/external reference
  (Section 4);
- `extractedFrom` — the original's `DocumentId`, where the derivative was
  extracted from an already-registered Document, mirroring the existing
  Tika-derivative case precisely;
- `derivedFrom` — other `Provenance` identifiers, where the ingested
  content derives from other already-registered Memory Core material;
- `processingHistory` — one structured entry (mirroring
  `ExtractionIdentity`'s own established shape: fixed fields, never free
  narrative) naming and pointing to the external, ingestion-owned Source
  Manifest and/or Derivative Generation Record holding the detailed
  facts (producer identity, transformation history, digest, completeness
  state) that Memory Core does not, and need not, duplicate;
- `contentNature` — `EXTRACTED` for mechanically derived content,
  mirroring the existing Tika case, or another already-defined value as
  the specific case warrants; never a new value.

No duplicate identifier is invented. `EvidenceArtifactId` and
`DerivativeGenerationId` remain Document Ingestion's own opaque
identities (Units 1-2, unmodified); Memory Core references them by
value, exactly as it already references `ConversationId`/`TurnId`.

## 10. Identity/reference decision

Restated concretely: **reference over duplication, in both directions,
with no exception.** Memory Core never copies a Derivative Generation
Record's, source manifest's, review record's, or audit record's own
field values into its own record — it holds only the opaque identity.
Document Ingestion's own records never gain a forward-pointing field
into Memory Core or Knowledge — causality runs one way: a Memory Core
record, if and when created, is created *after* and *pointing back at*
an already-existing, already-immutable ingestion identity, never the
reverse. This mirrors Unit 5 §13's "reference versus duplication rule"
extended, by identical reasoning, across the Document Ingestion/Memory
Core boundary rather than invented anew.

**A reference transfers no authority in either direction.** A Memory
Core `Provenance` record naming an `EvidenceArtifactId` by value does not
thereby make Memory Core an Evidence Custodian, a source of evidential
truth about that artifact, or anything beyond a holder of a reference —
exactly as Memory Core already holding a `ConversationId` reference does
not make it an authority over Conversations (Section 4). Symmetrically,
a source manifest, Derivative Generation Record, review record, or audit
record does not gain Memory Core authority, canonical status, or
promoted status merely because a later Memory Core record references it
— being referenced is not an event Document Ingestion's own governance
observes, reacts to, or is altered by in any way.

## 11. Evidence Intelligence handoff

Fresh-reconciled with CDR-007 and Unit 4. Two coexisting paths, neither
new, both already implied by already-adopted governance:

- **Tier A (mechanical) → Memory Core, directly, without Evidence
  Intelligence.** The `EvidenceRegistrationCoordinator`/Tika precedent
  (Section 4) already establishes this path for byte-backed mechanical
  extraction — no Evidence Intelligence involvement is required or
  implied, because Tier A content, per the Boundary Clarification's
  Determination 1 (already restated in Unit 4), "involves no
  interpretation, no pattern recognition, and no judgment of any kind."
  The identical path is available, by direct analogy, for a Tier A
  Derivative Generation Record.
- **Tier B (recognition/model-backed) → Evidence Intelligence → Memory
  Core.** CDR-007 assigns OCR/recognition to Evidence Intelligence's own
  analytical functions (Unit 4 §4.A, unnarrowed). Where Tier B content is
  ever registered in Memory Core, it is expected to pass through
  whichever acceptance mechanism Evidence Intelligence's own governance
  (EI Contract Design §6, "how... outputs are accepted by the subsystems
  that govern them") already establishes for its own candidate output —
  this document does not invent, wire, or shortcut that mechanism, and
  does not authorise a Tier B bypass directly into Memory Core.

In both paths, the candidate/canonical distinction is preserved
unweakened: whatever enters Memory Core is, itself, a `Provenance`/
`Document` record subject to Memory Core's own existing rules (including
that Memory Core "does not weigh or resolve conflicting Assertions — it
preserves them, unresolved, side by side" (Contract Design, restated
Section 14 below); it is never
thereby "canonical" in the sense of final evidential-state assignment,
which remains Knowledge Memory's alone. Evidence Intelligence is not
collapsed into Document Ingestion or Memory Core by either path.

## 12. Derivative Review effect

**None, beyond what Unit 3 already fixed.** `APPROVED` remains a
human-verification signal only (Unit 3 §4.C, §8, restated Section 5 and
8 above). This document adds no rule under which `APPROVED` causes,
implies, or accelerates Memory Core registration or Knowledge promotion.
A reviewed, `APPROVED` Derivative Generation Record is referenced by a
future Memory Core `Provenance` record identically to an unreviewed one
— review state is not a precondition this document creates for Memory
Core registration, nor does registration retroactively grant review
significance it did not already have.

## 13. Ingestion audit effect

**No promotion effect; a reference relationship resolved here, as Unit 5
itself deferred.** Unit 5 §22 explicitly left "[w]hether ingestion audit
ever needs an opaque reference to an already-governed Memory Core
identity... recorded here only as a possibility, never as a requirement"
— deferring the question to Alignment Amendment 3 by name. This document
now resolves it, narrowly: **no new mechanism is created.** If a Memory
Core `Provenance` record ever needs to reference the ingestion audit
trail for an attempt, it uses the identical `processingHistory` entry
mechanism Section 9 already fixes for the Source Manifest and Derivative
Generation Record — never a new field, and never a required one. Audit
completion (or failure) is otherwise invisible to, and has no bearing on,
whether a Memory Core registration or Knowledge promotion ever occurs.
Audit success is never a hidden promotion mechanism, directly answering
Question H.

## 14. Source/derivative immutability

Memory Core/Knowledge activity is **read-only** with respect to every
ingestion-owned record. A Memory Core `Provenance` record may reference
a source manifest, Derivative Generation Record, review record, or audit
record by identity; it never mutates any of them, because Memory Core
holds no write path into Document Ingestion's own domain at all (the
relationship runs, structurally, only in the direction established by
Section 10). If a later fact changes — a human disputes or corrects
something Memory Core already recorded — that is expressed as Memory
Core's own new record or `Assertion`, coexisting unresolved alongside the
earlier one (Memory Core Contract Design: "it preserves them, unresolved,
side by side") or as a new Document Ingestion generation (Unit 2's own immutable-
generation rule) — never as a rewrite of ingestion history, and never as
a rewrite of Memory Core's own prior record either.

## 15. Reconciliation/multi-parent treatment

Fresh-checked against Unit 2's own same-root multi-parent rule (§7). A
Memory Core/Knowledge cross-reference to a reconciliation generation:

- **references that generation as one immutable derivative generation**,
  by its own single `DerivativeGenerationId` — this is the only relation
  Memory Core needs and the only one this document authorises;
- **does not need, and is not given, direct awareness of, or references
  to, its parent generations** — that lineage is Unit 2's own governed
  internal structure (§7), discoverable, if ever needed, only through
  Document Ingestion's own governed provenance lookup, never duplicated
  into a Memory Core record;
- **does not reopen cross-source synthesis.** Unit 2 §7 already forbids
  a reconciliation generation from naming parents whose roots differ;
  because Memory Core only ever sees one opaque identity — never the
  underlying multi-parent structure — a Memory Core reference adds no
  mechanism by which a forbidden cross-source combination could be
  smuggled through. The prohibition is enforced entirely upstream, before
  any Memory Core relationship exists, and this document narrows nothing
  about it.

## 16. Retrieval/QMD/RKS boundary

**No new authority, storage relationship, or responsibility is created
for Memory Core, Knowledge, QMD, RKS, or any other retrieval mechanism.**
RKS is, unmodified, "retrieval/relevance over already-promoted
knowledge" (Unit 1, restated); it never touches an ingestion-owned
record directly, whether or not that record is ever referenced by a
Memory Core `Provenance`. QMD remains, per CDR-008 Invariant 12, never a
"persistent parallel or secondary source of canonical truth" — this
document creates no path by which ingestion content could reach QMD/RKS
other than through the identical, already-governed Memory Core →
Knowledge Memory → RKS/QMD path every other kind of Memory Core content
already uses, unmodified. Document Ingestion has, and gains, no
responsibility for semantic retrieval.

## 17. Deletion/retention consequences

Governance-level only, per instruction; several items explicitly
**DEFERRED** rather than invented:

- **Source deletion vs. Memory Core/Knowledge records — resolved by
  direct analogy, not invention.** Mirroring the Conversation precedent
  (Section 4: Memory Core holds only a reference, never a structural
  dependency) and Unit 2/5's own already-adopted non-cascade rules:
  deleting a source `EvidenceArtifact` (via the existing, owner-only
  `OwnerEvidenceDeletionAuthority`) does not imply deleting any Memory
  Core `Provenance`/`Document` that references it by identity — the
  reference becomes a reference to a deleted-but-once-real identity,
  exactly as Unit 2 §17 already establishes for a Derivative Generation
  Record's own lineage pointing at a deleted source.
- **Derivative deletion vs. Memory Core/Knowledge records** — identical
  reasoning and identical resolution, by the same analogy.
- **Memory Core deletion vs. ingestion provenance — DEFERRED.** No
  adopted Memory Core governance inspected establishes a general
  deletion mechanism for `Provenance`/`Document`/`Entity`/`Assertion`
  records at all; this document does not invent one, and therefore
  cannot resolve what such a mechanism, if it is ever built, would do to
  an ingestion-owned reference it points at. Preserved position:
  whatever mechanism is eventually built must not be permitted to mutate
  or delete ingestion's own records as a side effect (Section 14) — that
  much is already settled; the reverse question (does Memory Core
  deletion remove the ingestion-side reference) is not yet decided
  anywhere and is marked deferred here.
- **Knowledge deletion vs. ingestion provenance — DEFERRED**, for the
  identical reason: no adopted Knowledge Memory deletion mechanism was
  found to reason about.
- **Audit deletion vs. Memory Core/Knowledge — doubly deferred.** Unit 5
  §17 already marks ingestion-audit-record deletion itself as deferred;
  its interaction with Memory Core/Knowledge is deferred on top of that,
  not newly resolved here.

No cascading deletion is invented in any direction by this document.

## 18. Failure/transaction boundary

Six separate, non-atomic acts, explicitly distinguished per Question M:
(1) derivative admission (Unit 2); (2) derivative review (Unit 3); (3)
ingestion audit completion (Unit 5); (4) Evidence Intelligence
processing (CDR-007/Unit 4); (5) Memory Core registration (this
document, Section 9); (6) Knowledge/Memory promotion (Knowledge Memory,
Section 4). **These are not one transaction, and this document does not
introduce distributed atomicity between any of them.** Ingestion's own
completion (per Unit 2's atomicity rule and Unit 5's audit-before-success
rule) is final and independently observable *before* any Memory Core
registration is even attempted — a failure at stage 5 or 6 can never
retroactively un-admit a Derivative Generation Record, un-approve a
review, or un-record an audit fact; conversely, a failure at any earlier
stage (1-3) simply means stage 5 never becomes eligible to begin, per
Section 8's own non-automatic chain.

## 19. Required semantics

- If a Memory Core `Provenance`/`Document` record is ever created for
  ingested content, it must reference ingestion-owned identities by
  value only, using Memory Core's existing fields (Section 9) — never a
  new field, never duplicated content.
- Such a record's creation must pass Memory Core's existing
  `PermissionAction.WRITE`/`ResourceType.MEMORY`|`DOCUMENT` gate,
  evaluated by `PermissionEngine` — never a bypass.
- The five-stage chain (Section 8) must remain non-automatic at every
  boundary.
- Ingestion's own completion (admission, review, audit) must remain
  observable, immutable, and independent of whether Memory Core
  registration ever occurs (Section 18).

## 20. Conditional semantics

- Memory Core registration of an ingested derivative occurs only if, and
  when, a governed Parker-owned coordinator undertakes it — never
  automatically upon admission, review, or audit.
- Which coordinator performs registration (the ingestion coordinator
  itself, acting in a later, separate step, or a distinct downstream
  registration coordinator) is unresolved by this document and left to
  implementation-plan work (Section 24).
- Tier B content's registration path runs through Evidence Intelligence's
  own acceptance mechanism (Section 11); Tier A content's registration
  path may bypass it, mirroring the Tika precedent.
- `contentNature`, `extractedFrom`, and `derivedFrom` population depend
  on the specific derivative's own lineage and are populated only where
  applicable, exactly as Memory Core's existing contract already
  requires generally.

## 21. Optional/extensible semantics

- Whether the ingestion coordinator is ever granted the Memory Core
  write permission directly, versus a wholly separate promotion
  coordinator always performing registration, is an implementation-plan
  choice this document does not fix.
- A future, separately governed mechanism for deciding *which* ingested
  derivatives are worth registering in Memory Core at all (not every
  Tier A/B output need ever be registered) is left open, non-authorised,
  and non-prohibited by this document.

## 22. Forbidden semantics

Per Question O, explicitly:

- automatic memory promotion from ingestion admission, review, or audit
  success;
- automatic Knowledge Item creation from parsing or OCR output;
- automatic promotion from Derivative Review `APPROVED`;
- automatic promotion from ingestion audit success;
- Document Ingestion modifying, creating, or deleting a Memory Core
  record;
- Document Ingestion modifying, creating, or deleting a Knowledge Item;
- Memory Core mutating Document Ingestion's own provenance or history
  (source manifest, Derivative Generation Record, review record, audit
  record);
- Knowledge Memory mutating Document Ingestion's own provenance or
  history;
- treating Evidence Intelligence candidate propositions as canonical
  facts without the governed downstream acceptance decision Section 11
  already requires;
- QMD, RKS, or any search/index mechanism acquiring canonical authority
  over ingestion-derived content (CDR-008 Invariant 12, Section 16);
- cross-source synthesis being smuggled into Memory Core through a
  reconciliation generation's own reference (Section 15) — Unit 2 §7's
  same-root prohibition is not reopened, narrowed, or bypassable through
  this document's own reference mechanism;
- inventing a new Memory Core field, a new Knowledge Memory write path,
  or a new identifier type to make any of the above more convenient.

## 23. Downstream consumers

| Consumer | Classification | Reasoning |
| --- | --- | --- |
| Memory Core's `Provenance`/`Document` contract | unaffected; existing fields reused | Section 9 — no new field |
| `EvidenceRegistrationCoordinator` | unaffected; precedent only, not modified | Section 4 |
| A future ingestion-side registration coordinator | requires later implementation-plan unit | Section 20-21; not designed here |
| Evidence Intelligence | unaffected | Section 11; candidate/canonical distinction preserved |
| Knowledge Memory | unaffected | Section 4; promotion authority untouched |
| RKS/QMD | unaffected | Section 16 |
| Derivative Review (Unit 3) | unaffected | Section 12 |
| Ingestion audit (Unit 5) | unaffected | Section 13 |
| Derivative Generation Record (Unit 2) | unaffected, referenced only | Section 10, 14 |

## 24. Future implementation surfaces

Distinguished per Question P; none touched by this document:

- **Required future implementation work (only if/when Memory Core
  registration of ingested content is actually undertaken):** a
  coordinator populating `CandidateProvenance`/`CandidateDocument` for a
  Derivative Generation Record, mirroring `EvidenceRegistrationCoordinator`'s
  own existing shape (Section 4). Not designed, named, or begun here.
- **Optional future integration:** whether that coordinator is the
  ingestion coordinator itself or a separate downstream component
  (Section 21).
- **No-change surfaces (confirmed by direct inspection, this unit):**
  `MemoryCore.kt`'s `Provenance`/`Document` types; `EvidenceRegistrationCoordinator`
  itself; `EvidenceCustodian.kt`; `DerivativeReview.kt`; the OCR Mechanism
  interfaces; Knowledge Memory's own (unread by this document, per its
  own "Memory Core never reads Knowledge Memory's own state" boundary,
  Section 4) internal promotion mechanism; RKS/QMD.

## 25. Explicit deferrals

- Which coordinator performs Memory Core registration (Section 20-21).
- Whether every ingested derivative should ever be registered in Memory
  Core, or only some, and by what policy (Section 21).
- Memory Core deletion's effect on an ingestion-owned reference (Section
  17).
- Knowledge deletion's effect on an ingestion-owned reference (Section
  17).
- The precise mechanics of Tier B (Evidence Intelligence-mediated)
  registration, left to Evidence Intelligence's own governance and the
  still-open OCR Unit 12 question (`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`
  §6, unaffected here).
- Any Kotlin type, schema, or persistence technology (out of scope by
  instruction, Section 1).

## 26. Constitutional self-check

| Authority | Check | Result |
| --- | --- | --- |
| Parker Constitution | Parker owns authority; modules provide capability | Section 7: no new authority created; registration uses Memory Core's own existing generic gate |
| CDR-006 | Original evidence custody/immutability frozen | Not reopened; unaffected — this document concerns derivatives and Memory Core references only |
| CDR-007 | EI proposes, never asserts; final evidential state is Knowledge Memory's | Section 4, 8, 11 restate verbatim, unweakened |
| CDR-008 | Memory Core sole authoritative system of record; no downstream parallel truth | Section 16, 22 — RKS/QMD gains no canonical authority |
| CDR-004 | Provenance identifier resolution classified as existing identifier-lookup mode | Not reopened; this document's reference-only relationship uses that same, already-classified lookup shape |
| Memory Core Contract Design §7, §10 | Provenance fields fixed; generic write gate; Knowledge Memory read-only | Section 9-10 reuse fields unmodified; Section 7 confirms no bypass of the gate |
| Document Ingestion Units 1-5 | Governing authority for this programme | Every section cites and restates, never contradicts, already-adopted rules |
| ADR-024 | Modules never write directly to platform state | Section 7: no module (ingestion) gains a Memory Core write path; only a Memory-Core-authorised coordinator, via Memory Core's own gate, ever writes |

## 27. Conflict analysis

**None.** Every rule in this document is either a direct, unmodified
restatement of Memory Core's own already-adopted Contract Design/Scope
Lock, CDR-004/007/008, or the already-implemented
`EvidenceRegistrationCoordinator` precedent (Section 2, 4, all freshly
re-read and quoted with exact line numbers), or a narrow, disclosed
analogy extending Units 1-5's own already-adopted "reference over
duplication," "no automatic promotion," and "no cascading deletion"
principles into the Memory Core relationship those units always deferred
to this one. No genuine contradiction between Amendment 3 and any
already-adopted governance was found. No missing rule prevented this
unit from being coherently scope-locked — where adopted governance is
silent (Memory Core's own deletion mechanics), this document defers
rather than invents (Section 17, 25). No existing implementation shape
conflicts with adopted governance.

## 28. Frozen conclusions

1. Document Ingestion requires **no direct write authority** into Memory
   Core or Knowledge.
2. A **provenance-reference-only relationship** is the maximum this
   document authorises: Memory Core's own existing fields
   (`sourceIdentifier`, `extractedFrom`, `derivedFrom`, `processingHistory`,
   `contentNature`) may reference ingestion-owned `EvidenceArtifactId`/
   `DerivativeGenerationId` values, by value, never by duplication.
3. Creating such a reference is an act of **Memory Core's own governed
   registration** (mirroring `EvidenceRegistrationCoordinator`), gated by
   Memory Core's own existing, unmodified `PermissionEngine` check — not
   a new ingestion authority.
4. **Promotion into Knowledge remains exclusively Knowledge Memory's**,
   unaffected, unreachable from ingestion, review, audit, or Evidence
   Intelligence output directly.
5. The five-stage candidate-information chain (Section 8) is
   **non-automatic at every boundary**; no stage's success implies the
   next.
6. **No cascading deletion** is created in either direction; where
   adopted governance does not yet decide a deletion interaction, it is
   marked deferred, not invented.
7. **No new authority is created for QMD, RKS, Evidence Intelligence, or
   any other subsystem** by this document.
8. Amendment 3 is now resolved: Document Ingestion's relationship to
   Memory Core and Knowledge is reference-only, optional, non-automatic,
   and governed entirely by mechanisms that already exist.
