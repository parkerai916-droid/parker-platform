# Document Ingestion — Derivative Generation Record Scope Lock

## Status

**Draft for owner review. Not yet accepted, canonical, or
implementation-authorising.** Programme: **Document Ingestion — Governance
Alignment Unit 2**, scope-locking Alignment Amendment 1 only (Derivative
Generation Record / evidence vocabulary), as identified by
`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md` §1 (adopted
`84cc061`). No Kotlin is implemented, proposed as a diff, or changed by
this document. No dependency is added. No interface is implemented. No
persistence technology is chosen. No parser is installed. No evidence is
ingested. Amendments 2-5 of the same Unit 1 alignment document are
explicitly out of scope and not begun here.

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
the RKS/QMD Contract Design/Scope Lock/Amendments, `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`/`reasoning-context.md`,
and ADR-024. It also does not amend the five documents adopted at `84cc061`
(`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`,
`DOCUMENT_INGESTION_GOVERNANCE_AMENDMENT_MAP.md`,
`DOCUMENT_INGESTION_PLUGIN_CONTRACT.md`,
`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`,
`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`) — no contradiction was found in
any of them (Section Y, below); this scope lock only fixes the boundary
Amendment 1 of the alignment document already committed Parker to
producing.

## 1. Executive Summary

This scope lock freezes the semantic and authority boundary of one new
concept, the **Derivative Generation Record**: an immutable,
provenance-bearing record for a non-authoritative derived generation
produced from an authoritative source (or a governed parent), used
specifically for the case where the derivative has no single canonical
byte representation and therefore cannot honestly be custodied as an
`EvidenceArtifact`. It freezes semantics and authority only — field
shape, storage technology, and wire format remain future implementation-
plan work.

## 2. Frozen Objectives

1. A Derivative Generation Record never becomes, is never treated as, and
   can never be mistaken for: `EvidenceArtifact`, `AcceptedEvidenceArtifact`,
   authoritative source evidence, a replacement source, a Memory Core
   record, a Knowledge Item, an Evidence Intelligence conclusion, an
   evidential finding, or a reasoning result.
2. `EvidenceArtifact` and `AcceptedEvidenceArtifact` are not redefined,
   extended, or given new fields by this document. `EvidenceCustodian`
   remains sole custodian of authoritative bytes.
3. A plugin never acquires Parker authority by returning output. Only a
   Parker-owned coordinator may cause a Derivative Generation Record to be
   admitted.
4. Every Derivative Generation Record traces, through immutable parent
   links, to exactly one authoritative root source — including a record
   with more than one parent (Section 7's multi-parent case), where every
   parent's own lineage must resolve to that same single root. No cycle is
   representable. No derivative acquires more evidential authority than
   any parent it has. No Derivative Generation Record may combine content
   from more than one distinct authoritative root.
5. A generation, once admitted, is immutable. Reprocessing mints a new
   generation identity; it never overwrites, mutates, or removes an
   earlier one.

## 3. Canonical authorities inspected and current implementation/domain types inspected

Read in full for this scope lock: the five documents adopted at `84cc061`
(Section 0 above); `parker-constitution.md`; `epistemic-integrity.md`;
`EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`; CDR-006; CDR-007; the Evidence
Processing Searchable-PDF Boundary Clarification (in full, including its
§4 "Extraction Metadata — Reproducibility Contract" table); the OCR
Mechanism Contract Design and Scope Lock; the OCR Mechanism Programme
Completion Review; `MEMORY_CORE_CONTRACT_DESIGN.md` §7; ADR-024;
`EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`.

Kotlin inspected directly, not inferred from README or prose description:
`src/interfaces/EvidenceCustodian.kt` (full file, 571 lines —
`EvidenceArtifactId`, `CandidateEvidenceArtifact`, `AcceptedEvidenceArtifact`,
`EvidenceAcceptanceResult`, `EvidenceCustodian` interface, `EvidenceDeletionResult`,
`OwnerEvidenceDeletionAuthority`); `src/interfaces/DerivativeReview.kt`
(full file — `DerivativeReviewState`, `DerivativeReviewRecord`,
`DerivativeReviewRegistry`); `src/interfaces/MemoryCore.kt` (identity
types, `Provenance`); `src/interfaces/OcrMechanism.kt` (`OcrRecognitionResult.confidence`
and its `0.0..1.0` bound, `TranscriptionFidelity`, `OcrRecognitionIdentity`);
`src/interfaces/EvidenceDeletionAudit.kt`; every `value class ...Id(val value: String)`
identity type across `src/contracts/` and `src/interfaces/` (grep-surveyed
directly — see Section 4).

## 4. Existing identity conventions found

Every identity type in this codebase — `EvidenceArtifactId`, `ProvenanceId`,
`EntityId`, `DocumentId`, `AssertionId`, `RelationshipId`, `CorrelationId`,
`PrincipalId`, `ResourceId`, `RequestId`, `DecisionId`, `ResultId`,
`AuthorizationPurposeId`, `ModuleId`, `AgentRunId`, `PlanningSessionId`,
`TaskProposalId`, `TaskId`, `ConversationId`, `TurnId`, `KnowledgeId`,
`PlanCandidateId` — follows one uniform shape with no exception found:

```kotlin
@JvmInline
value class XxxId(val value: String) {
    init {
        require(value.isNotBlank()) { "XxxId must not be blank" }
    }
}
```

`CorrelationId`'s own KDoc states the governing precedent explicitly:
"a single, non-blank string value, matching `PrincipalId`/`ModuleId`/
`RequestId`'s identical established shape." `EvidenceArtifactId`'s own
KDoc confirms this shape carries no ordering, uniqueness-enforcement, or
issuance guarantee itself — those are later phases' responsibility, not
the identity type's.

**This is a governance-level precedent, not a mere implementation
convenience**: opaque, non-blank, string-valued, minted once by a
designated owner, never parsed for meaning, never reassigned. `DerivativeGenerationId`
should be recognized, at this scope-lock stage, as requiring exactly this
established shape when a future implementation unit defines it — no
alternative shape (composite key, sequential integer, tuple) is
consistent with every other identity this codebase already has. The
literal Kotlin declaration remains implementation-plan work; this scope
lock fixes only that it must conform to the established shape and to
Section 5's semantic requirements, which the bare shape above does not by
itself guarantee (Section 5).

## 5. Identity — `DerivativeGenerationId`

**Semantic requirements (in addition to the established shape, Section
4):**

- **Opaque.** Carries no decodable meaning; a consumer must not parse it
  for source type, format, or timing.
- **Immutable.** Once assigned to a generation, never reassigned, never
  reused for a different generation, including after that generation is
  superseded or found invalid (mirrors `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`'s
  already-adopted generation-identity language).
- **Unique within the governed domain** — no two distinct generations,
  ever, across all sources and all time, share one identity.
- **Non-semantic and non-ordinal.** It is not, and must never be treated
  as, a generation number. "Which generation is newest" is derived from
  the immutable parent-link chain and creation time (Section 6), never
  from the identity value itself. A human-readable ordinal, if ever
  displayed, is a derived, non-authoritative display value computed from
  the chain — never the identity, never persisted as authority, never
  reused if a generation is superseded.
- **Minted only by the Parker-owned ingestion coordinator** (Section 14)
  — never by a plugin, never derived from plugin-supplied input in a way
  that would let a plugin predict or choose it.

## 6. Generation immutability

A Derivative Generation Record, once admitted (Section 14), is
permanently immutable: no field is ever updated, replaced, or removed in
place. Reprocessing the same source with the same or different
configuration always mints a distinct, new `DerivativeGenerationId` and a
new record — it never updates an existing one. An earlier generation is
never deleted, hidden, or superseded merely because a later generation
exists; both remain independently retrievable unless a future, separately
governed retention/deletion authority acts (Section 13). This restates,
for the non-byte case, exactly the discipline `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
Invariant I-11 already fixes for the general derivative case.

## 7. Parent/lineage

A Derivative Generation Record's parent may be:

- **A. an authoritative `EvidenceArtifact`** (the ordinary case — a
  generation produced directly from custodied source bytes);
- **B. a separately accepted/custodied child-source `EvidenceArtifact`**
  (the case established by `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §1's
  child-source model — an already-accepted decoded attachment, for
  example, treated identically to case A once accepted); or
- **C. another Derivative Generation Record** (a generation derived from
  a prior non-byte generation, e.g. a table-reconstruction generation
  derived from a prior structural-parsing generation of the same
  source).

**Authority-safe lineage rule for case C.** A Derivative Generation
Record's evidential authority is never greater than, and is always
strictly subordinate to, each of its immediate parents' (ordinarily
exactly one; see "Multiple parents," below, for the sole exception) —
this is transitive: a grandchild generation is subordinate to its
parent, which is subordinate to the root source, with no accumulation of
authority at any hop. A non-byte Derivative Generation Record can never
itself become a parent's byte-backed source of truth, and no chain of
Derivative Generation Records, however long, ever terminates anywhere but
at an authoritative `EvidenceArtifact` (case A or B). This is the
Constitutional Optimisation Safeguard's own rule ("no derivative
artefact... is ever substituted for... the original it was derived
from"), restated for chained non-byte generations specifically.

**Traversability.** Every Derivative Generation Record must record each
of its immediate parents' identity and kind (root `EvidenceArtifactId` /
child-source `EvidenceArtifactId` / parent `DerivativeGenerationId`;
ordinarily exactly one parent), such that lineage is always traversable,
by immediate-parent links alone, back to exactly one authoritative root
`EvidenceArtifactId` — the identical root across every parent, when the
record has more than one. A record whose lineage cannot be so traversed,
or whose parents resolve to more than one distinct root, is not a valid
Derivative Generation Record.

**No cycles.** A Derivative Generation Record's parent(s) are fixed at
creation and never mutated (Section 6); because parent links are
immutable and assigned only once, at creation, to an already-existing
identity, a cycle is structurally unrepresentable — a record cannot name
a not-yet-existing later record as a parent, and cannot have an
already-recorded parent link changed to point forward. This holds
identically whether a record has one parent or several: each link is
independently fixed once, at creation, to a prior identity, so adding
more parent links does not weaken the argument or require a runtime
cycle-detection algorithm.

**Multiple parents (reconciliation only).** Every Derivative Generation
Record has exactly one parent **except** a reconciliation/synthesis
record created under Section 16's multi-parser coexistence rule, which
may name more than one parent — one per input generation being
reconciled. This is the only case in which multiple parents are
permitted. Two governing constraints apply, both mandatory and neither
waivable by a plugin or by ordinary generation admission:

- **Same-root only.** Every parent named by a multi-parent record must,
  by this section's own traversability rule, resolve to the identical
  authoritative root `EvidenceArtifactId`. "Same root" means the literal
  same `EvidenceArtifactId` reached by each parent's own immediate-
  ancestor chain as defined above — it does not mean "eventually related"
  through some other lineage fact recorded elsewhere (for example, a
  child-source's own separate, permanent lineage note to *its* parent,
  Section 22, is a distinct fact and is never itself treated as a further
  root-tracing hop for the purposes of this section's root definition). A
  record whose parents resolve to two different `EvidenceArtifactId`
  values is not a valid Derivative Generation Record under this scope
  lock, regardless of any other relationship between those two artifacts.
- **No cross-source combination.** This document authorizes multi-parent
  lineage only for reconciling disagreeing outputs of the *same*
  authoritative source (Section 16's own purpose). It does **not**
  authorize, and no wording elsewhere in this document authorizes, one
  Derivative Generation Record to combine content derived from more than
  one distinct authoritative `EvidenceArtifact`. Combining material from
  genuinely independent sources is cross-source synthesis or analysis,
  not ingestion — it is Tier C territory (Plugin Contract §9.1) and
  remains entirely outside Document Ingestion's authority, requiring its
  own, separately governed downstream mechanism should it ever be
  authorized. A Derivative Generation Record is never that mechanism.

## 8. Byte-backed versus non-byte derivatives

Three categories remain distinct, and none collapses into another:

1. **Authoritative source `EvidenceArtifact` bytes** — custodied by
   Evidence Custodian, unchanged by this document.
2. **Separately custodied child-source/derived bytes accepted through
   Evidence Custodian** — also an `EvidenceArtifact`/`AcceptedEvidenceArtifact`,
   also unchanged; this is the case where a derivative byte stream *has*
   been explicitly, separately accepted (`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
   §1).
3. **Non-byte Derivative Generation Records** — this document's own new
   concept, for a derivative with no single canonical byte
   representation.

A Derivative Generation Record **may reference** a byte-backed derivative
artifact (category 1 or 2) — for example, a completeness-manifest
generation that names the `EvidenceArtifactId` of a byte-backed OCR
output it accounts for — **without itself becoming that artifact**. The
reference is a lineage/association fact recorded on the Derivative
Generation Record; it confers no custody, no byte identity, and no
digest onto the Derivative Generation Record itself. Conversely, nothing
in this document permits a Derivative Generation Record to be
retroactively "promoted" into an `EvidenceArtifact` — a category change
of that kind, if ever needed, requires the same explicit, separately
governed acceptance path any other byte stream requires (Section 17),
never an implicit reclassification.

## 9. Content identity / digest

- **Where a derivative has a deterministic byte serialization** (for
  example, an exported literal-text file), a content digest — represented
  as an (algorithm, digest) pair, per `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
  §1's already-adopted hash-agility rule — may be computed and recorded.
  SHA-256 is the current required algorithm where a digest is recorded;
  the pair shape ensures a future governed algorithm addition needs no
  redesign.
- **Where the derivative is structured/non-byte content** (for example,
  an in-memory MIME tree with no single canonical serialization), **no
  digest is fabricated.** A digest is recorded only when a governed
  canonical serialization exists for that derivative kind — this scope
  lock does not invent one; defining a canonical serialization, if one is
  ever needed for a specific derivative kind, is deferred to a future,
  narrower unit (mirroring `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`'s own
  already-adopted rule: "an envelope digest computed over a non-canonical
  or non-deterministic serialisation is not evidence of anything and must
  not be presented as one").
- **Absence of digest is an explicit, first-class, valid state** for a
  Derivative Generation Record — never a null defaulted silently, never
  interpreted as "digest not yet computed" when the true fact is "no
  canonical serialization exists to digest." This mirrors Memory Core
  Provenance's own `UNKNOWN` content-nature discipline: an explicit
  absence, not an omission.

## 10. Producer identity

**Minimum required to identify how a generation was produced**, following
the already-established `ExtractionIdentity` precedent (Evidence
Processing Searchable-PDF Boundary Clarification §4 — "one structured
record, not three independently-worded sentences... a small, flat,
structured type... never composed as prose a future reader must
interpret"):

- plugin/product identity and version (mandatory — "the overall
  extraction framework and its version");
- Parker adapter identity and version where applicable (mandatory when an
  adapter mediates the plugin, mirroring the Boundary Clarification's own
  "actual parser identity" distinction from the framework name/version);
- processing mechanism/configuration identity (mandatory — "a named tag
  identifying the specific parser configuration in force... a different
  configuration could produce different output from the same extractor
  version");
- model identity and version, **conditionally required** — mandatory
  whenever the transformation is Tier B (recognition/model-backed, per
  the Plugin Contract's own §9.1), absent/not-applicable for Tier A
  mechanical parsing.

This is a required, structured record (this document's own "producer
identity" concept, playing the same governance role
`ExtractionIdentity` already plays for the searchable-PDF case) — not
free narrative text, and not three independent optional fields a future
implementation could word inconsistently between generations.

## 11. Transformation history

A Derivative Generation Record must record, in ordered application
sequence, which named transformation(s) produced it, drawn from the
Plugin Contract's own closed transformation vocabulary (§5 —
`CHARACTER_DECODING`, `MIME_TRANSFER_DECODING`, `DECOMPRESSION`, `OCR`,
`STRUCTURAL_PARSING`, `WHITESPACE_NORMALISATION`, `LINE_ENDING_NORMALISATION`,
`DATE_PARSING`, `NUMERIC_INTERPRETATION`, `FIELD_UNQUOTING`,
`ENTITY_DECODING`, `IMAGE_RASTERISATION`, `PAGE_RENDERING`,
`TABLE_RECONSTRUCTION`, `READING_ORDER_INFERENCE`, `MODEL_INFERENCE`,
`METADATA_INTERPRETATION`) — never a vague label such as "processed."
This scope lock does not extend or re-derive that vocabulary; it requires
that every Derivative Generation Record's transformation-history entries
be drawn from it, exactly as the Plugin Contract already requires for
transformation disclosure generally. Extending the vocabulary itself, if
ever needed, is a Plugin Contract amendment, not a matter for this
document.

## 12. Time

Three distinct timestamp concepts, never conflated, mirroring Memory Core
Provenance's own already-adopted three-timestamp discipline (§7:
"Claimed creation time," "Acquisition time," "Ingestion time"):

- **Source-document date(s)** — whatever date(s) the source content
  itself claims or contains (e.g., an email's `Date:` header). Not owned
  or asserted by the Derivative Generation Record itself; if a
  transformation surfaces such a date as parsed content, that is the
  derivative's *content*, not its own generation time.
- **Evidence receipt/custody date** — Evidence Custodian's own
  acceptance time (`AcceptedEvidenceArtifact.acceptedAt`), unchanged,
  outside this document's scope.
- **Generation time** — required on every Derivative Generation Record:
  when *this generation* was produced. Must never be presented as, or
  mistaken for, source chronology. A generation produced today from a
  document dated a decade ago must record today's date as its own
  generation time; the document's own claimed date, if extracted, is
  separately disclosed as content, not substituted for generation time.

## 13. Completeness / warnings

A Derivative Generation Record associates with a completeness/accounting
result and warnings by **reference**, not by duplicating the full
routing/completeness model (`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`
§4) inline:

- it carries the completeness state applicable to it, drawn from that
  policy's already-adopted five-value vocabulary
  (`AccountedFor`/`AccountedForWithQualifications`/`KnownIncomplete`/
  `NotAssessable`/`NotApplicable`) — a reference to that governed
  vocabulary, not a re-definition of it;
- it carries an ordered list of warnings/errors specific to producing
  *this* generation;
- it does not duplicate the attempt-level audit record
  (`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` §5) — the
  audit record references generation identities produced by an attempt;
  the generation record does not re-carry the whole attempt's audit
  facts, only its own completeness/warning outcome.

A generation whose production failed a mandatory completeness check must
carry a qualified/incomplete state (never `AccountedFor`) on the
generation record itself, consistent with the already-adopted
mandatory-check failure semantics — this document does not relax that
rule for the non-byte case.

## 14. Structural provenance

A Derivative Generation Record **may** carry structural
location/provenance where the producing mechanism can supply it — page,
block, paragraph, table, row/cell, MIME entity, CSV record/field, image
region, coordinate/bounding-box — as **conditionally required**
(Section 18-B): required *when the mechanism and derivative kind support
it and it is available*, never required universally. No parser is
obligated to produce every location form; a CSV parser has no page
concept, a MIME decoder has no coordinate concept, and neither is a
defect.

**Absence must be distinguishable where governance requires it** — this
document distinguishes exactly two absence states, mirroring the
completeness vocabulary's own discipline: "not applicable" (this
derivative kind has no meaningful notion of this location type — a CSV
record has no page) versus "not supplied" (the mechanism could
in principle report it but this particular generation does not — e.g., a
parser that can report page numbers but was not asked to, or failed to,
for this generation). A Derivative Generation Record must not silently
conflate the two.

## 15. Confidence

Confidence, when present, belongs **on the generation, and conditionally
on individual derived structural elements** where the producing
mechanism reports per-element confidence (mirroring OCR's own
page/segment-level confidence disclosure) — both are permitted, neither
is required, and a generation-level figure never substitutes for a
missing element-level one or vice versa when the mechanism actually
computed both.

**Deterministic mechanical (Tier A) parsing carries no fabricated
confidence.** Directly following the already-established precedent
(`CandidateProvenance.confidence` left `null` for deterministic literal
PDF extraction, because "inventing a numeric confidence figure for it
would be fabricated precision this repository's own governing principle
... already forbids"): a Tier A Derivative Generation Record's confidence
is absent, not a fabricated `1.0`.

**Recognition/model-backed (Tier B) confidence is a working, transient
disclosure, never evidential truth.** Directly following OCR Mechanism's
own established rule ("any confidence this mechanism reports is its own,
working, transient output only"; bounded to the closed unit interval
`0.0..1.0`; "no caller-declared confidence... may carry a confidence
figure"): a Tier B Derivative Generation Record's confidence, when
present, is bounded `[0.0, 1.0]`, self-reported by the mechanism (never
supplied by a caller), and remains derivative metadata describing the
mechanism's own operation — it is never elevated to, treated as, or
substituted for an evidential-state classification, which remains
exclusively Evidence Intelligence's domain and is not touched by this
document.

## 16. Multi-parser coexistence

Preserved exactly as `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §6 and
Invariant I-10 already require, restated for Derivative Generation
Records specifically:

```
SOURCE
  |
  +-- Derivative Generation A
  |
  +-- Derivative Generation B
```

Neither generation silently overrides or supersedes the other merely
because it exists, ran later, or disagrees with the other. Both remain
independently retrievable, each with its own identity, lineage,
completeness assessment, and warnings. Material disagreement between two
generations of the same source is preserved as an observable fact, not
resolved, hidden, or averaged by this document or by ordinary generation
admission. A reconciliation or synthesis of two or more generations, if
ever authorized by a future, separately governed downstream act, is
itself a **new** Derivative Generation Record (or `EvidenceArtifact`, if
byte-backed and separately accepted) with its own lineage naming both
inputs as parents under Section 7's case-C rule and its multiple-parents
constraints — permitted only because, and only to the extent that, both
inputs share the identical authoritative root. Reconciling generations
that trace to two different roots is cross-source synthesis, not
ingestion, and is not authorized here (Section 7). This is never an
in-place edit to either input generation.

## 17. Retention / deletion

**No new deletion authority is created by this document.** Cross-checked
directly against `src/interfaces/EvidenceCustodian.kt`'s
`OwnerEvidenceDeletionAuthority`: source-artifact deletion is already a
"wholly separate interface... never" reachable through ordinary
accept/retrieve capability, structurally owner-only, with no `reason`
parameter permitting a disguised "cleanup" path. This document does not
propose, imply, or sketch a Kotlin deletion interface for Derivative
Generation Records; it fixes only the following governance relationships,
leaving the mechanism to a future, separately governed unit that must
itself mirror `OwnerEvidenceDeletionAuthority`'s narrow, structurally
isolated shape if and when it is built:

- **Source retention is independent of derivative retention.** Deleting,
  or ceasing to retain, a Derivative Generation Record never authorizes,
  implies, or performs deletion of its source `EvidenceArtifact` — this
  is `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` Invariant I-15
  ("Independent retrieval... derivative deletion/replacement cannot
  affect it"), restated for the non-byte case.
- **Source deletion never authorizes silent derivative mutation.** If a
  source `EvidenceArtifact` is ever deleted through the existing
  owner-only authority, no Derivative Generation Record descended from it
  is thereby mutated, rewritten, or silently altered — it remains exactly
  what it was, with its lineage record intact (now pointing to a deleted,
  not a nonexistent-and-forgotten, source identity). What retention
  policy, if any, should then apply to orphaned generations is
  **explicitly deferred** to a future unit; this document takes no
  position beyond "not silent mutation."
- **Historical-generation retention** (whether/when a superseded
  generation may ever be purged) is likewise deferred. Section 6's
  immutability rule governs only that a generation is never mutated or
  overwritten while retained — it does not itself decide retention
  duration or purge authority.

## 18. Authorization

**Who may cause a Derivative Generation Record to be created/admitted:**
exclusively the Parker-owned ingestion coordinator named throughout the
five documents adopted at `84cc061` — the same coordinator that mints
`EvidenceArtifactId`s for byte-backed cases, performs routing,
permission checks, digest verification, and audit. A plugin returning
output (an `IngestionAttemptOutcome` per the Plugin Contract §3) supplies
**candidate** content and metadata only; it never itself mints a
`DerivativeGenerationId`, never itself marks a candidate "admitted," and
acquires no Parker authority merely by successfully returning a result —
restating, for this specific record type, the Plugin Contract §6 rule
this document does not weaken: "A plugin does not custody, register,
promote, index, or declare them authoritative." Admission requires the
coordinator's own act, gated by whatever authorization/permission check
governs ingestion generally (established by the adopted Unit 1
documents, unchanged here).

## 19. Failure atomicity

Freezing authority-safe outcomes only — no implementation mechanism is
designed here:

- **Parsing succeeds but provenance capture fails:** the candidate output
  is **not** admitted as a Derivative Generation Record. A
  provenance-less derivative must never silently become an admitted
  governed derivative, per the task's own instruction, restated here as
  binding: admission and provenance-capture are one atomic governance
  event, not two independently-observable ones a caller could see split.
- **Derivative content exists but generation identity cannot be
  persisted:** same rule — no identity, no admission. The candidate
  content may still be surfaced as an operational-outcome fact (a
  `Failed`/`Partial` attempt outcome per the Routing/Completeness
  Policy's own outcome vocabulary) but is never presented as a governed,
  reviewable, or referenceable Derivative Generation Record.
- **Completeness accounting fails (cannot be computed at all,
  distinct from a failed check):** the generation, if otherwise validly
  produced and admitted, carries `NotAssessable` (the already-adopted
  vocabulary's own state for exactly this case) — it is not silently
  admitted as `AccountedFor`, and it is not thereby barred from
  admission either; `NotAssessable` is itself a valid, honest completeness
  state, not a failure of admission.
- **Audit recording fails:** fail-closed, mirroring
  `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` §5's own
  already-adopted rule ("Audit failure is fail-closed before declaring
  successful governed acceptance... must not claim a derivative was
  custodied or registered until the corresponding durable operation
  actually succeeded") — restated here to apply identically to
  Derivative Generation Record admission, not only to byte-backed
  custody.
- **Plugin terminates partway through processing:** no partial,
  half-populated Derivative Generation Record is ever admitted. Whatever
  partial content the plugin managed to produce may be surfaced only
  through the attempt's own `Partial`/`Failed` operational outcome, never
  through a Derivative Generation Record lacking required governance
  semantics (Section 20-A).

**Governing rule, stated once for all five cases above:** admission is
all-or-nothing at the governance boundary. A Derivative Generation Record
either satisfies every required-semantics field (Section 20-A) and is
admitted whole, or it does not exist as a governed record at all — there
is no partially-governed intermediate state visible to any consumer.

## 20. Field-shape classification

No Kotlin data class is frozen by this document. The following
classification governs what a future implementation-plan unit's concrete
field shape must, may, and must never do.

**A. Required governance semantics** (every conforming implementation
must preserve all of these; a record missing any of these is not
validly admitted, per Section 19):

- `DerivativeGenerationId` (Section 5);
- one or more immediate parent identities and kinds — root
  `EvidenceArtifactId`, child-source `EvidenceArtifactId`, or parent
  `DerivativeGenerationId` (ordinarily exactly one; more than one only for
  a Section 16 reconciliation record under Section 7's multiple-parents/
  same-root rule) (Section 7);
- root source `EvidenceArtifactId`, traceable from the immediate parent
  chain(s) and, where there is more than one parent, identical across all
  of them (Section 7);
- derivative kind (a name identifying what this generation represents —
  e.g., "MIME structure," "OCR output," "table reconstruction");
- producer identity record (Section 10);
- ordered transformation-history entries, drawn from the closed Plugin
  Contract §5 vocabulary (Section 11);
- generation time (Section 12);
- completeness state, drawn from the already-adopted five-value
  vocabulary (Section 13);
- operational outcome for this generation (usable / not usable, tracking
  the Routing Policy's own outcome vocabulary at the generation level).

**B. Conditionally required semantics** (required exactly when
applicable/available; absent and explicitly marked absent otherwise —
never silently omitted where its absence is itself meaningful):

- content digest, only where a governed canonical serialization exists
  (Section 9);
- model identity/version, only for Tier B generations (Section 10);
- confidence, only where the producing mechanism genuinely computes one,
  bounded `[0.0, 1.0]` (Section 15);
- structural location/coordinates, only where the derivative kind and
  mechanism support it (Section 14);
- warnings specific to this generation (Section 13), present as an
  explicit empty list when there are none, never simply absent.

**C. Optional/extensible semantics** (useful, non-authority-defining;
may vary by derivative kind or implementation without further governance
review):

- human-readable display labels or derived ordinal numbers (Section 5 —
  explicitly never authoritative);
- mechanism-supplied non-authoritative detail fields (Plugin Contract §5:
  "plugins may also provide non-authoritative detail");
- byte-backed-artifact cross-references for convenience (Section 8),
  beyond the minimum lineage requirement.

**D. Forbidden semantics** (would create authority leakage or ambiguity;
no implementation may include these):

- any field allowing a Derivative Generation Record to assert or imply
  it is, or has become, an `EvidenceArtifact`, `AcceptedEvidenceArtifact`,
  or authoritative source;
- any field allowing a plugin-supplied value to substitute for the
  coordinator-minted `DerivativeGenerationId` or the coordinator's own
  admission decision;
- any mutable field on an admitted record (Section 6);
- any field encoding evidential-state classification (credibility, legal
  significance, truth, intent) — this remains exclusively Evidence
  Intelligence's domain, untouched by this document (Plugin Contract
  §9.1's Tier C exclusion);
- a fabricated confidence value on a Tier A (deterministic mechanical)
  generation (Section 15);
- a generation number, counter, or other ordinal treated as identity
  rather than as a derived display value (Section 5);
- any lineage whose parents' respective roots resolve to more than one
  distinct authoritative `EvidenceArtifactId` — cross-source combination
  is not authorized by this document (Section 7's multiple-parents/
  same-root rule).

## 21. Source-manifest relationship

A Derivative Generation Record identifies its source manifest by
**reference only** — the root source `EvidenceArtifactId` (Section 20-A)
is sufficient to locate the source manifest `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
§1 already establishes as owned by the Parker-owned ingestion coordinator.
This document does not duplicate the source manifest's own fields
(digest, byte length, received/detected media type, filename metadata,
ingestion correlation) onto every Derivative Generation Record — a
generation that needs any of those facts obtains them by following its
own root-source reference, exactly as Amendment 3 (Memory Core
cross-reference, not begun by this document) already establishes the
identical reference-not-duplication pattern for Memory Core Provenance.
This reference remains unambiguous even for a multi-parent reconciliation
record (Section 7): because every parent of such a record shares, by
construction, the identical root `EvidenceArtifactId`, there is still
exactly one source manifest to reference, never two competing ones.

## 22. Child-source relationship

Preserved exactly as `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §1/§4 and
Owner Decision 2 already require:

```
Original source
   |
   v
plugin decoding
   |
   v
candidate bytes
   |
   v
Parker explicit acceptance
   |
   v
Evidence Custodian
   |
   v
child EvidenceArtifact
```

A Derivative Generation Record **may record the decoding operation and
lineage** — for example, a "MIME transfer decoding" transformation-
history entry (Section 11) whose output was a *candidate* attachment,
with a reference to the candidate's own hash/length as computed by the
plugin. It **cannot itself confer `EvidenceArtifact` authority** on those
candidate bytes: only the separate, explicit Evidence Custodian
acceptance act does that (Section 18's authorization rule applies
identically here — the Derivative Generation Record documents that
decoding happened; it does not, and cannot, perform or substitute for
acceptance).

## 23. Downstream-reference boundary

This document establishes that a future system **may reference**
`DerivativeGenerationId` as an opaque identity. It does **not**
implement or broaden, and explicitly defers to later units:

- `DerivativeReviewRecord`/`DerivativeReviewRegistry` widening (Alignment
  Amendment 2 — not begun);
- any Memory Core field, convention, or cross-reference wiring (Alignment
  Amendment 3 — not begun);
- any Knowledge Item, Knowledge Store, or `KnowledgeSource` change;
- any RKS/QMD change;
- any Evidence Intelligence contract, scope, or invocation change;
- any Reasoning Context change.

## 24. Non-goals

Explicitly out of scope for this document:

- no parser implementation;
- no plugin integration;
- no OCR invocation;
- no model invocation;
- no review-target widening (`DerivativeReviewRecord`/`DerivativeReviewRegistry`
  remain exactly as implemented today);
- no Memory Core change;
- no Knowledge change;
- no RKS/QMD change;
- no Evidence Intelligence change;
- no Reasoning Context change;
- no `EvidenceArtifact` redesign;
- no `AcceptedEvidenceArtifact` redesign;
- no source mutation of any kind;
- no real-evidence ingestion;
- no Kotlin implementation of any kind;
- no persistence technology selection;
- no dependency addition;
- Alignment Amendments 2, 3, 4, and 5 of the Unit 1 alignment document
  (not begun by this scope lock).

## 25. Conflicts discovered

**None.** Direct re-reading of the five documents adopted at `84cc061`,
`EvidenceCustodian.kt`, `DerivativeReview.kt`, `MemoryCore.kt`,
`OcrMechanism.kt`, `EvidenceDeletionAudit.kt`, CDR-006, CDR-007, the
Evidence Processing Boundary Clarification, the OCR Mechanism Contract
Design/Scope Lock, and ADR-024 found every claim this scope lock relies
on to be accurate and every rule it states to be either a direct
restatement of an already-adopted invariant or a narrow, additive
extension of one (the `ExtractionIdentity` structured-producer-identity
precedent, Section 10; the `OcrRecognitionResult.confidence` transient/
bounded precedent, Section 15; the `OwnerEvidenceDeletionAuthority`
narrow-isolation precedent, Section 17). No document adopted at `84cc061`
required correction. Per the task's own instruction, since no genuine
contradiction was found, none of those five documents is modified by
this pass.

## 26. Constitutional self-certification

Cross-checked directly against each named authority:

| Authority | Check | Result |
| --- | --- | --- |
| Parker Constitution | "Parker owns authority; modules provide capability" | Section 18 (Authorization) restates this for admission specifically; no plugin gains authority |
| Epistemic Integrity | No fabrication; unknown stated as unknown | Section 9 (no fabricated digest), Section 15 (no fabricated confidence) both directly apply this |
| Evidence Artifact governance | Frozen; not reopened | Section 2 item 2; no field added anywhere |
| Evidence Custodian | Sole custodian of authoritative bytes | Section 8; `EvidenceCustodian.kt` unchanged, re-verified directly |
| CDR-006 | Original evidence custody/immutability frozen | Not reopened; Section 7's lineage rule restates, not narrows, its Optimisation Safeguard |
| CDR-007 | OCR/extraction assigned to Evidence Intelligence | Not touched; Section 10's Tier B conditional field is silent on invocation authority, consistent with Alignment Amendment 5 (not begun, not needed here) |
| Document Ingestion Unit 1 (`84cc061`) | Governing authority for this programme | Every section here cites and restates, never contradicts, its already-adopted rules |
| OCR governance | `OcrRecognitionResult.confidence` shape; no caller-declared confidence | Section 15 restates verbatim |
| Evidence Intelligence | Not a truth authority; analytical only | Section 20-D forbids evidential-state fields on a Derivative Generation Record |
| Memory Core | Provenance unchanged; cross-reference only | Section 21; no field touched, no schema change |
| Knowledge | Untouched | Section 23 defers explicitly |
| RKS/QMD | Untouched, retrieval-only | Section 23 defers explicitly |
| Reasoning Context | Untouched | Section 23 defers explicitly |
| Deletion governance | `OwnerEvidenceDeletionAuthority` narrow/isolated | Section 17 mirrors, creates no new authority |
| Audit governance | Fail-closed before claiming success | Section 19 restates verbatim |
| ADR-024 | Modules never write directly to platform state | Section 18; admission is exclusively the coordinator's act |

## Final Recommendation

**READY FOR OWNER REVIEW** (scope-lock stage; see Section AD of the
accompanying report for the formal recommendation label).
