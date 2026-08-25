# Evidence Intelligence — Contract Design

## Status

**Accepted. Canonical. Ready for Scope Lock.** This document is
contract design only. No Kotlin is implemented, proposed as a diff, or
changed by it — every shape below is described in prose, not as a
`kotlin`-fenced signature block. No storage technology, database schema,
hashing algorithm, or wire format is specified. Neither `src/` nor
`tests/` is touched. No Scope Lock and no Implementation Plan is produced
by this document. Independent constitutional review has now concluded
and accepted this Contract Design; both the Scope Lock and the
Implementation Plan are accordingly authorised to begin as the next
governance stage, but neither is begun by this document itself. Nothing
is staged, committed, or pushed.

### Amendment 1 — ReasoningSubject Integration

This document is amended by Amendment 1, which brings this Contract
Design current with Reasoning Provider Contract Design Amendment 1
(`docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`):
`ReasoningProviderRequest` now carries `subject: ReasoningSubject` in
place of `turn: Turn`. Evidence Intelligence's own orchestration of
`ReasoningProvider` (§1, §12, below) proceeds via
`ReasoningSubject.OfEvidenceAnalysisRequest`, wrapping this document's
own, already-frozen `EvidenceAnalysisRequest` unmodified.
`ReasoningSubject` is owned by the Reasoning Provider Contract Design,
not by Evidence Intelligence, and Evidence Intelligence reuses it
unmodified, exactly as it already reuses `ReasoningProvider`,
`ReasoningProviderResponse`, and `ReasoningContext`. This amendment
introduces no new Evidence-Intelligence-owned public type, no new
responsibility, no new ownership, and no new dependency — Evidence
Intelligence's own public type count, dependency table, and CDR-007's
classification are all unaffected; Conversation Engine's own exclusive
ownership of `Turn` and `Conversation` is likewise unaffected. Amendment
1's own scope was fixed in advance by
`docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_AMENDMENT_1_PROPOSAL.md`;
every paragraph below marked "Amendment 1" implements exactly what that
proposal identified, and no other paragraph in this document is altered
by it.

### Amendment 2 — OCR Mechanism Dependency

This document is amended by Amendment 2, which authorises a concrete OCR
mechanism as a fourth Evidence Intelligence dependency (§12, below),
structurally parallel to the existing `ReasoningProvider` row, so that
`EvidenceIntelligence.analyse` may internally invoke it exactly as
CDR-007 and this document's own §1 Responsibilities already say Evidence
Intelligence is responsible for doing ("an OCR transcription" is already
named there as an example of "Producing candidate derivative
artefacts"). This amendment introduces no new `EvidenceAnalysisResult`
variant, no new public type, no new responsibility, and no change to
acceptance orchestration — it is a narrow dependency-list correction,
not a redesign.

Evidence Intelligence gains ownership of nothing new by this amendment;
it becomes a consumer of a new dependency edge, exactly as it already is
for `ReasoningProvider`. A future, separate OCR mechanism Contract
Design — not drafted, not begun, and not named by this amendment — will
own the OCR mechanism's own concrete contract, mirroring exactly how the
Reasoning Provider Contract Design owns `ReasoningProvider`. Evidence
Custodian retains exclusive ownership of any accepted OCR-transcription
artefact after acceptance, unchanged. No ownership change is made to
`EvidenceArtifactId`, `CandidateEvidenceArtifact`, `Provenance`, or any
Memory Core or Knowledge Memory type. CDR-006's own classification of
original evidence custody and immutability is unaffected and unreopened
by this amendment. Evidence Processing's own ownership of OCR detection
— the `RequiresOcr` disclosure `EvidenceExtractionCoordinator.extract`
already produces — is likewise unaffected; this amendment touches no
Evidence Processing governance document.

This dependency remains, and can only ever be authorised as, a
mechanical, orchestrated mechanism — exactly the same standing
`ReasoningProvider` already has: no parser, no OCR engine, and no
external library this dependency may eventually name ever possesses
authority over truth, is a constitutional classifier, or is capable of
assigning `EvidentialState` or determining what its own output means.

**This amendment does not authorise, and no future reader should treat
it as having authorised:** OCR implementation; OCR provider selection;
OCR runtime composition; OCR orchestration (including who consumes
Evidence Processing's own `RequiresOcr` disclosure); or the future OCR
mechanism Contract Design itself. Two further questions remain
separately, individually unresolved, neither one collapsed into the
other or into "orchestration" generally: **owner control and
authorisation for any machine-triggered OCR invocation** — an open
question against Constitutional Test 1, not decided by this amendment
and not decided by anything it relies upon; and **permission gating and
disposition of OCR output rejected during output-quality validation** —
whether such a rejection requires its own disclosed
`PermissionAction`/`ResourceType` pairing, distinct from ordinary
analytical judgment, is not decided by this amendment. Each of these,
and the output-validation mechanism itself, remains future governance,
to be produced separately, at the appropriate governance stage.

CDR-006 and CDR-007 each performed a subsystem-tier constitutional
classification — deciding that Evidence Custodian, and then Evidence
Intelligence, exist at all as first-class, peer subsystems (CDR-007
§1). The OCR mechanism this amendment authorises is not a peer
subsystem of that kind; it is a capability-level dependency *within*
the existing Evidence Intelligence subsystem, constitutionally
analogous in tier to `ReasoningProvider` — which itself required no CDR
of its own to be authorised as a dependency (§12, below). This mirrors
the Constitution's own "Parker owns authority. Modules provide
capability" principle (Core Principles): a module, including a
reasoning provider, a tool integration, or a future extension, supplies
capability to an already-classified subsystem without itself becoming a
new subsystem. On this basis, no operative CDR-007 decision changes,
and no constitutional-tier document changes; this remains a Contract
Design evolution only, of the same tier and character as Amendment 1.
Amendment 2's own scope was
fixed in advance by
`docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_OCR_MECHANISM_AMENDMENT_PROPOSAL.md`;
every paragraph below marked "Amendment 2" implements exactly what that
proposal identified, and no other paragraph in this document is altered
by it.

This document accepts `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
("CDR-007") — Accepted, Canonical, and Frozen — as the controlling,
already-decided constitutional basis for everything below, and does not
reopen it. CDR-007's classification of Evidence Intelligence as a
first-class, downstream, analytical subsystem, bound in full by
Epistemic Integrity Amendment No. 1, orchestrating but never itself
constituting a Reasoning Provider, is restated here only as the
constraint this design must satisfy — not re-argued. This document does
not revisit, and treats as settled: what Evidence Intelligence is or is
not (CDR-007 §1–2); its constitutional boundary (CDR-007 §3); which
subsystems it reuses and which capabilities remain exclusively owned
elsewhere (CDR-007 §4–5); which analytical artefacts it may produce, and
that ownership of each transfers on acceptance to the accepting
subsystem (CDR-007 §6, as clarified); and that legal ownership, custody,
promotion, and truth authority all remain exactly where CDR-006 and
Programme 3 already placed them.

**The constitutional principle governing every responsibility below,
restated verbatim from CDR-007 and applied literally:** *Evidence
Intelligence exists to transform governed evidence into governed
analytical products while preserving complete provenance, and without
acquiring authority over truth, custody, or knowledge promotion.*

**No language syntax is specified anywhere in this document.** Every
responsibility, boundary, and shape below is described by purpose,
obligation, and relationship — never by Kotlin type, interface, enum, or
method signature — mirroring `MEMORY_CORE_CONTRACT_DESIGN.md`'s and
`EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`'s own explicit "implementation
independence" discipline, applied here for the same reason: this
Programme is not yet at the stage where a technology choice may safely
be made. Where an existing contract's field or type is named below (for
example, `EvidenceArtifactId`, `CandidateAssertion`), it is named only to
identify what is reused, exactly as those two documents already name
existing fields and types without thereby specifying Kotlin.

---

## Context and Constitutional Basis

CDR-007 decided that Evidence Intelligence is a first-class, downstream,
analytical subsystem — never a component of Evidence Custodian or
Knowledge Memory, never itself a Reasoning Provider, never a truth
authority, and never in possession of custody, promotion, or
evidential-classification authority. It also found that this Programme's
existing contracts — Evidence Custodian, Memory Core, Programme 3
Knowledge Memory, and the Reasoning Provider — already supply nearly
everything Evidence Intelligence structurally needs, and directed that
this document, the next required governance stage, define Evidence
Intelligence's own contract by maximising that reuse rather than
inventing parallel machinery.

This document reviewed, in full: the Parker Constitution;
`docs/architecture/epistemic-integrity.md` (Epistemic Integrity Amendment
No. 1, all 19 Articles); `docs/decisions/CDR-001` through `CDR-007`;
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`;
`docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`;
`docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`;
`docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`;
`docs/architecture/EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`;
`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`; and
`docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` (as amended,
Amendment 1: `docs/reviews/REASONING_PROVIDER_CONTRACT_DESIGN_AMENDMENT_PROPOSAL.md`,
`docs/reviews/REASONINGSUBJECT_CONTRACT_DESIGN_STUDY.md`,
`docs/reviews/REASONING_PROVIDER_CONTRACT_DESIGN_AMENDMENT_1_INDEPENDENT_REVIEW.md`,
and `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_3_CONSTITUTIONAL_DECISION_MEMORANDUM.md`).
It does not reopen, re-argue, or narrow any decision in any of those
documents. Every section below traces to a specific, named source;
nothing below is invented where an existing contract already answers the
question.

**Governing sources, by section:**

- **CDR-007** — controlling for Evidence Intelligence's own
  classification, boundary, exclusive-responsibility table, and
  permitted-artefact categories (§1 below onward).
- **CDR-001, CDR-002, CDR-003** — controlling for any comparison
  capability (§7).
- **CDR-004** — precedent only, for the general principle that a
  generically defined capability may be extended to cover a
  previously-uncovered case without reopening its own frozen text; not a
  capability this document itself extends.
- **CDR-005** — controlling for how any new Permission Engine proposal
  class this document's operations require is admitted (§12, §15):
  self-certification against Chapter 10's published criteria, escalating
  to a further CDR only if genuinely contested — never decided by fiat
  in this document.
- **CDR-006 and `EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`** — controlling
  for the Evidence Custodian boundary this document must not cross (§2,
  §6, §12).
- **`MEMORY_CORE_CONTRACT_DESIGN.md`** — controlling for every Memory
  Core contract this document reuses (§3–§9, §12).
- **`PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`** — controlling
  for the Knowledge Candidate submission boundary this document must not
  cross (§2, §6, §9, §12).
- **`REASONING_PROVIDER_CONTRACT_DESIGN.md`** — controlling for the
  Reasoning Provider shapes this document reuses when Evidence
  Intelligence internally orchestrates one (§3, §4, §12), as amended by
  Amendment 1 (ReasoningSubject) — §3, §4, §12 each updated accordingly,
  below.

---

## Purpose and Scope

Evidence Intelligence's purpose, exactly as CDR-007 states it, is
narrow: it performs analytical processing of evidence already held in
Evidence Custodian custody or already registered in Memory Core, and
proposes — never authoritatively asserts — candidate propositions,
comparisons, and derivative artefacts arising from that evidence. This
document is in scope for: Evidence Intelligence's required
responsibilities (§1) and explicit non-responsibilities (§2); its public
model (§3); its inputs (§4) and outputs (§5); how those outputs are
accepted by the subsystems that govern them (§6); its relationship to
the canonical comparison model (§7), the Evidence Custodian provenance
mechanism (§8), and analytical confidence (§9); its public interfaces
(§10); its failure model (§11); its dependencies (§12); what it reuses,
extends, and does not reuse (§13); its architectural boundaries (§14);
and the verification a future implementation must satisfy (§15).

This document is out of scope for anything listed in the Out of Scope
section at the end, most importantly: no storage technology, API,
database schema, hashing algorithm, or Kotlin type is chosen or implied
anywhere below; no Scope Lock or Implementation Plan is produced; and no
constitutional question CDR-007 already answered is reopened.

---

## 1. Responsibilities

Evidence Intelligence is responsible for, and only for, what CDR-007
already authorises:

**Analytical processing of governed evidence.** Given a reference to one
or more evidence artefacts held by Evidence Custodian, and/or one or
more records already registered in Memory Core, Evidence Intelligence
performs analysis — comparison, extraction, transcription, translation,
summarisation, contradiction detection, chronology or timeline
construction, or issue/entity mapping — producing candidate propositions
and candidate derivative artefacts (CDR-007 §1, §6).

**Orchestrating existing Reasoning Providers.** Evidence Intelligence
may employ one or more existing `ReasoningProvider` implementations as
internal analytical mechanisms, exactly as `ConversationTurnReasoningCoordinator`
already does for its own purpose. Evidence Intelligence is the
orchestrating subsystem; a Reasoning Provider it invokes remains a pure
callee with no obligations of its own beyond what
`REASONING_PROVIDER_CONTRACT_DESIGN.md` already defines (CDR-007 §1).

**Producing transient working output.** Any draft, intermediate, or
working analytical result that is never submitted for acceptance carries
no evidentiary status and may be freely regenerated or discarded (CDR-007
§6, category 1).

**Producing candidate derivative artefacts.** Where an analytical step
(an OCR transcription, an extraction, a translation) produces a
derivative worth preserving, Evidence Intelligence is responsible for
producing it as a `CandidateEvidenceArtifact` — never for accepting it
into custody, holding it, storing it, or protecting it itself.
Acceptance into Evidence Custodian is a separate responsibility (§6
below; CDR-007 §6, category 2).

**Producing candidate Memory Core records.** Where an analytical step
produces a proposed claim or a proposed comparison/contradiction finding,
Evidence Intelligence is responsible for producing it as a
`CandidateAssertion` or a `CandidateRelationship` (including
`SUPPORTS`/`CONTRADICTS`) — never for registering it into Memory Core
itself. Registration through Memory Core's existing write interface is a
separate responsibility (§6 below; CDR-007 §6, category 3).

**Producing Knowledge Candidates.** Where an analytical finding is
significant enough to be proposed for promotion, Evidence Intelligence is
responsible for producing it as a `KnowledgeCandidate` referencing
already-accepted Memory Core evidence — carrying no confidence or
evidential-state value of its own, and never for submitting it to
Knowledge Memory itself. Submission through Knowledge Memory's existing
submission boundary is a separate responsibility (§6 below; CDR-007 §6,
category 4).

**Preserving contradictory evidence.** Evidence Intelligence must never
discard, suppress, or silently omit contradictory evidence merely because
a preferred interpretation exists. Where evidence conflicts, the
constitutionally correct analytical output discloses that conflict — as
a `CONTRADICTS`-typed `CandidateRelationship`, or as an honest disclosure
within transient working output — never a silent resolution in favour of
one account (CDR-007, Constitutional Constraints; Article III, Article
IV, Article XVI).

**Maintaining traceability.** Every material factual representation
Evidence Intelligence produces, at any output category, retains a
resolvable provenance chain back to the preserved original evidence it
relies upon, using Memory Core's existing `derivedFrom`/`extractedFrom`
mechanism (§8 below; CDR-006's mandatory traceability rule, reaffirmed by
CDR-007).

## 2. Explicit Non-Responsibilities

Evidence Intelligence does **not**:

- **Determine truth.** Truth-of-content determination is outside every
  subsystem's authority (Epistemic Integrity, Article III); Evidence
  Intelligence's outputs are, at most, candidate propositions carrying a
  provisional evidential characterisation (CDR-007 §2).
- **Assign evidential state.** Final evidential-state assignment remains
  exclusively Knowledge Memory's, per Article XV and Programme 3
  Contract Design V2 §4. No field on any type this document defines or
  reuses for Evidence Intelligence's own output carries a caller-settable
  evidential-state value.
- **Populate durable confidence.** Evidence Intelligence's own
  analytical confidence computations remain transient working output
  only (§9). It never populates `CandidateAssertion.confidence` or any
  other durable confidence field governed by Memory Core or Knowledge
  Memory. Promotion confidence remains exclusively within existing
  Knowledge Memory governance, unaffected by anything Evidence
  Intelligence does.
- **Promote Knowledge.** Promotion decisions, and the durable Knowledge
  Item lifecycle (promotion, revision, retirement, restoration), remain
  exclusively Knowledge Memory's (Programme 3 Contract Design V2 §1).
  Evidence Intelligence may produce a Knowledge Candidate; it may never
  perform any part of promotion itself.
- **Own provenance.** Provenance remains exclusively Memory Core's
  contract. Evidence Intelligence never constructs a parallel provenance
  model, and every `Provenance`/`CandidateProvenance` reference its own
  outputs carry uses Memory Core's existing, unmodified fields (§8).
- **Store evidence.** Evidence Intelligence holds no original or
  derivative evidence of its own. It holds, at most, a governed,
  observational reference obtained through `EvidenceCustodian.retrieve`
  for the duration of one analysis, and this document defines no
  persistence responsibility for Evidence Intelligence of any kind
  (CDR-007 §2).
- **Accept evidence.** Acceptance of a `CandidateEvidenceArtifact` into
  Evidence Custodian's custody is a separate responsibility, never
  Evidence Intelligence's own. Evidence Intelligence produces the
  candidate; it does not invoke `EvidenceCustodian.accept` (§1, §6,
  §12).
- **Delete evidence.** Deletion of a custodied original remains
  exclusively `OwnerEvidenceDeletionAuthority`'s, gated exactly as Phase
  7 of the Evidence Custodian Implementation Plan already established.
  Evidence Intelligence holds no dependency, at any depth, on
  `OwnerEvidenceDeletionAuthority` or on `EvidenceArtifactStorage.delete`
  (§12, §15).
- **Register Memory Core records.** Writing a `CandidateAssertion` or
  `CandidateRelationship` into Memory Core is a separate responsibility,
  never Evidence Intelligence's own. Evidence Intelligence produces the
  candidate; it does not invoke `MemoryCore`'s own write interface (§1,
  §6, §12).
- **Modify Memory Core.** Evidence Intelligence never alters Memory
  Core's own contract, schema, lifecycle rules, or read/write
  interfaces, and holds no dependency on, nor ever itself invokes,
  `MemoryCore`'s own write interface (immediately above; §6, §12).
- **Submit Knowledge Candidates.** Submitting a `KnowledgeCandidate` to
  Knowledge Memory's evaluation boundary is a separate responsibility,
  never Evidence Intelligence's own. Evidence Intelligence produces the
  candidate; it does not invoke Knowledge Memory's own submission
  interface (§1, §6, §12).
- **Modify Knowledge Memory.** Evidence Intelligence never alters
  Knowledge Memory's own contract, promotion evaluation, or lifecycle,
  and holds no dependency on, nor ever itself invokes, Knowledge
  Memory's own submission interface (immediately above; §6, §12).
- **Persist outputs.** Evidence Intelligence holds no persistence
  responsibility of any kind for anything it produces, whether transient
  or destined for acceptance elsewhere (§5).
- **Orchestrate downstream acceptance.** Sequencing a produced candidate
  into its accepting subsystem — deciding when, whether, and in what
  order acceptance occurs — belongs to a separate responsibility this
  document does not assign to Evidence Intelligence (§6).
- **Constitute a Reasoning Provider.** Evidence Intelligence may employ
  one or more `ReasoningProvider` implementations internally, but is not
  itself classified as one, and the `ReasoningProvider` interface is not
  broadened, extended, or reinterpreted to describe Evidence Intelligence
  (CDR-007 §1–2).
- **Determine legal ownership.** Legal ownership, copyright, and
  proprietary interest remain outside every subsystem's authority,
  permanently, per CDR-006 — unaffected and unaddressed by anything
  Evidence Intelligence does.
- **Authorise its own actions.** Evidence Intelligence cannot grant
  itself access to evidence it is not authorised to read, cannot bypass
  Evidence Custodian's, Memory Core's, or Knowledge Memory's own
  Permission Engine gates, and holds no independent epistemic- or
  trust-authorisation mechanism of its own (CDR-007 §3).

## 3. Public Model

Consistent with the design rules governing this document — prefer reuse,
prefer composition, prefer references, avoid duplication, avoid
wrappers, avoid mirrors, avoid shadow models, avoid introducing new
authority, and give every public object exactly one constitutional
owner — Evidence Intelligence's own public model is deliberately thin.
It introduces exactly **three** new public types and reuses everything
else.

**New types, owned by Evidence Intelligence:**

- **`EvidenceAnalysisRequest`** — the single input shape for an analysis
  invocation (§4). Owned by Evidence Intelligence because no existing
  contract bundles "which governed evidence, of which kinds, for which
  analytical purpose" into one addressable request; every field it
  carries is either a reference to an existing, unmodified type or an
  open classification string, following exactly the pattern
  `ReasoningProviderRequest` already established for bundling `Turn` and
  `ReasoningContext` without duplicating either.
- **`EvidenceAnalysisResult`** — a sealed output shape with exactly four
  variants, one per CDR-007 §6 permitted-artefact category (§5). Owned
  by Evidence Intelligence for the same reason: no existing type
  represents "one of these four outcomes, and only one." Each variant
  itself carries only an existing, unmodified candidate type, or — for
  transient output — a bare prose string together with a list of
  references to existing, already-governed identifiers (§4, §5) —
  never a new record shape and never a new identifier or provenance
  type.
- **A closed, two-case selector type**, carried solely as the payload of
  `EvidenceAnalysisResult`'s own "Candidate record produced" category
  (§5). Owned by Evidence Intelligence for the same reason as the two
  types above: no existing type represents "exactly one of these two
  existing candidate kinds, and only one," and none of Memory Core's own
  contracts offers a shared classification `CandidateAssertion` and
  `CandidateRelationship` both already satisfy. This type is frozen to
  the following properties, none of which any future revision may
  weaken:

  1. Behaviour-free — no operation beyond the ordinary structural
     operations (equality, textual representation, copying) any plain
     value already has; no operation that itself performs an
     acceptance, persistence, retrieval, reasoning, or any other
     domain act.
  2. Closed to exactly two cases — one selecting an existing,
     unmodified `CandidateAssertion`, one selecting an existing,
     unmodified `CandidateRelationship` — never a third case without a
     further amendment to this document.
  3. Owns no data beyond the one selected candidate value — no field
     beyond the single wrapped `CandidateAssertion` or
     `CandidateRelationship`.
  4. Introduces no fifth `EvidenceAnalysisResult` category —
     `EvidenceAnalysisResult` remains sealed with exactly four direct
     variants (§5); this type is not one of them, only the payload of
     one.
  5. Creates no acceptance, persistence, retrieval, reasoning,
     confidence, evidential-state, or ownership authority of its own —
     a pure selection mechanism; every one of those responsibilities
     remains exactly where §1, §2, §6, and §12 already assign it,
     never to Evidence Intelligence.
  6. Does not modify `CandidateAssertion` or `CandidateRelationship` —
     both remain reused, unmodified (§12); this type only references
     them, never extends, subclasses, or amends either.
  7. Is not a generic, reusable union mechanism — closed to these two
     named, existing types specifically, never a type-parameterised
     abstraction usable for any other pair of types.
  8. Owned exclusively by Evidence Intelligence. No other subsystem is
     authorised to adopt, produce, extend, or depend upon this type for
     any purpose outside consuming the already-authorised "Candidate
     record produced" result. Its public visibility creates no reusable
     utility role, ownership interest, authority, or independent
     dependency entitlement. Its sole purpose is realising this
     document's own §5 content requirement for "Candidate record
     produced."

  No Kotlin name, method signature, or package is defined for this type
  — exactly as this document defines no Kotlin design for any of its
  other types (§2, above).

**No other new public type is introduced.** In particular, this
document does **not** define:

- a new evidence-artefact type (`EvidenceArtifactId`,
  `CandidateEvidenceArtifact`, and `EvidenceArtifact` itself, all
  reused unchanged from Evidence Custodian's own contract);
- a new assertion or relationship type (`Assertion`, `CandidateAssertion`,
  `Relationship`, `CandidateRelationship`, all reused unchanged from
  Memory Core's own contract);
- a new knowledge type (`KnowledgeItem`, `KnowledgeCandidate`, both
  reused unchanged from Programme 3's own contract);
- a new provenance type (`Provenance`, `CandidateProvenance`, reused
  unchanged, §8);
- a new evidential-classification type (`EvidentialState` is referenced
  by name in this document only to state that Evidence Intelligence
  never constructs or assigns one — it is not a type Evidence
  Intelligence's own model touches at all);
- a new reasoning-invocation type (`ReasoningProvider`,
  `ReasoningProviderRequest`, `ReasoningProviderResponse`,
  `ReasoningContext`, `ReasoningSubject`, all reused unchanged, §12);
- a comparison-specific type of any kind (§7 — the canonical comparison
  model belongs to Memory Core, per CDR-003, and is referenced, not
  reimplemented, here).

**Every public object has exactly one constitutional owner.** The table
below states each type this document's own model touches and its owner:

| Type | Owner | This document's relationship to it |
| --- | --- | --- |
| `EvidenceAnalysisRequest` | Evidence Intelligence (new) | Defines |
| `EvidenceAnalysisResult` | Evidence Intelligence (new) | Defines |
| The "Candidate record produced" payload selector (§3, above; name not assigned by this document) | Evidence Intelligence (new) | Defines |
| `EvidenceArtifactId`, `CandidateEvidenceArtifact` | Evidence Custodian | Reuses, unmodified |
| `Assertion`, `CandidateAssertion`, `Relationship`, `CandidateRelationship` | Memory Core | Reuses, unmodified |
| `Provenance`, `CandidateProvenance` | Memory Core | Reuses, unmodified |
| `Document`, `CandidateDocument` | Memory Core | Reuses, unmodified |
| `KnowledgeCandidate`, `KnowledgeItem` | Knowledge Memory | Reuses (`KnowledgeCandidate` only), never touches `KnowledgeItem` |
| `EvidentialState` | Knowledge Memory | Never referenced by Evidence Intelligence's own model at all |
| `ReasoningProvider`, `ReasoningProviderRequest`, `ReasoningProviderResponse`, `ReasoningContext`, `ReasoningSubject` | Reasoning Provider Contract Design | Reuses, as amended by Reasoning Provider Contract Design Amendment 1 (`ReasoningProviderRequest.subject: ReasoningSubject`, orchestrated via `ReasoningSubject.OfEvidenceAnalysisRequest`) — unmodified *by Evidence Intelligence*, as an orchestrated dependency |
| `PrincipalId` | Existing platform identifier | Reuses, unmodified, for audit purposes only |

## 4. Inputs

Every input Evidence Intelligence may consume is a **reference to a
governed Parker artefact, never raw, unmanaged data**:

- **Custodied evidence, by reference.** `EvidenceAnalysisRequest` carries
  a list of `EvidenceArtifactId` values (Evidence Custodian's own,
  unmodified identifier type). Evidence Intelligence resolves each one
  only through `EvidenceCustodian.retrieve` — the same, already-frozen,
  read-only boundary every other consumer uses (Evidence Artifact
  Contract Design §6.3). This list may be empty, for an analysis that
  operates only on already-registered Memory Core records.
- **Memory Core records, by reference.** `EvidenceAnalysisRequest`
  carries a list of Memory-Core-addressable references, using the same
  (record kind, record identifier) pair shape `Relationship`'s own
  endpoints already use (Memory Core Contract Design §8) — never a
  duplicated copy of record content. Evidence Intelligence resolves each
  one only through `MemoryRetrieval`, Memory Core's own, unmodified,
  structural read boundary.
- **An assembled Reasoning Context, where relevant.** An optional
  `ReasoningContext` (Reasoning Provider Contract Design §2, reused
  unchanged), supplied only when this analysis internally invokes one or
  more Reasoning Providers. Its assembly remains, exactly as the
  Reasoning Provider Contract Design already discloses, an unassigned
  responsibility this document does not resolve. **Amendment 1.** Where
  this analysis internally invokes a `ReasoningProvider` via
  `ReasoningSubject.OfEvidenceAnalysisRequest` (Reasoning Provider
  Contract Design, Amendment 1), that invocation's own top-level
  `ReasoningProviderRequest.reasoningContext` — not this field — is the
  sole context `ReasoningProvider.reason` consults, per that document's
  own frozen invariant. This field retains its existing meaning and
  ownership within Evidence Intelligence's own analysis, independent of
  that invocation; it is not modified, merged, or otherwise redefined by
  this amendment.
- **An open analysis classification.** A non-blank `analysisKind`
  string, open and non-enumerated, mirroring `Entity.entityType`'s and
  `Document.documentType`'s own "open, not closed" convention (Memory
  Core Contract Design §4, §5). This document does not freeze a closed
  list of analysis kinds (comparison, extraction, transcription,
  translation, summarisation, and so on remain illustrative, not
  exhaustive) — doing so would foreclose a legitimate future analytical
  capability for no constitutional benefit, exactly the reasoning Memory
  Core's own open classification fields already establish.
- **A requesting principal, for audit purposes only.** A `PrincipalId`,
  reused unchanged, carried for the same reason `MemoryRetrieval`
  accepts one (Memory Core Contract Design §9): recording who asked,
  never as a filter or authorisation decision Evidence Intelligence
  applies on its own.

**Every `analysisKind` value — present or future — remains fully bound
by every constitutional limitation this Contract Design establishes.**
Because `analysisKind` is open and non-enumerated (immediately above), a
future analytical capability may introduce a new value without amending
this document. That extension is purely classificatory: naming a new
`analysisKind` never creates, expands, or implies any new authority, any
new ownership, any new storage right, any new evidential-state
authority, or any new governance power of any kind. Every
non-responsibility this document assigns (§2), every dependency
boundary it fixes (§12), every acceptance path it requires (§6), and
every constraint on confidence, provenance, and ownership it states (§8,
§9) applies identically and without exception to a future `analysisKind`
value as it does to every `analysisKind` value named as illustrative
above. `analysisKind` remains, in every case, a classification of *what
analysis is being requested* — never a grant, however implicit, of *what
Evidence Intelligence is thereby permitted to do* beyond what §1 through
§14 already, and permanently, authorise.

**Never accepted as input, by design:**

- Raw, uncustodied evidence bytes supplied directly by a caller,
  bypassing Evidence Custodian's own acceptance path.
- A caller-declared confidence value or evidential-state value of any
  kind — no field on `EvidenceAnalysisRequest` carries either.
- An instruction to modify, delete, replace, or obscure a custodied
  original, or to bypass the Permission Engine gate governing any
  referenced artefact or record.

## 5. Outputs

Every analytical artefact Evidence Intelligence may produce is
represented by one variant of `EvidenceAnalysisResult`, corresponding
exactly to one of CDR-007 §6's four permitted-artefact categories. Each
is specified below by owner before acceptance, accepting subsystem,
ownership after acceptance, and transience:

| `EvidenceAnalysisResult` variant | Carries | Owner before acceptance | Accepting subsystem (acceptance invoked by the separate responsibility in §6, never by Evidence Intelligence) | Owner after acceptance | Transient or durable |
| --- | --- | --- | --- | --- | --- |
| **Transient output** | A plain, disposable prose result (a draft comparison, a working chronology, a draft summary) whose material analytical claims are each traceable back to the specific governed evidence reference(s) that support them — existing `EvidenceArtifactId` values and/or existing Memory-Core-addressable references (§4) — never a copy of their content, and never only one undifferentiated reference list covering the output as a whole | Evidence Intelligence | None — never submitted anywhere | N/A — discarded or regenerated freely | Transient |
| **Candidate artefact produced** | An existing, unmodified `CandidateEvidenceArtifact` (an OCR transcription, an extraction, a translation) | Evidence Intelligence | Evidence Custodian, via its existing `accept` path | Evidence Custodian | Durable, once accepted |
| **Candidate record produced** | An existing, unmodified `CandidateAssertion` or `CandidateRelationship` (including `SUPPORTS`/`CONTRADICTS`) | Evidence Intelligence | Memory Core, via its existing public write interface | Memory Core | Durable, once written |
| **Candidate knowledge produced** | An existing, unmodified `KnowledgeCandidate`, referencing already-accepted Memory Core evidence, carrying no confidence or evidential-state field | Evidence Intelligence | Knowledge Memory, via its existing Knowledge Submission interface | Knowledge Memory | Durable only if promoted; the Candidate itself is a proposal, not yet knowledge |

**Ownership transfers on acceptance, and only on acceptance.** Restating
CDR-007's own clarification exactly: Evidence Intelligence owns the
production of a candidate output up to the point of its acceptance.
Once accepted, constitutional ownership belongs exclusively to the
accepting subsystem — Evidence Custodian, Memory Core, or Knowledge
Memory — never to Evidence Intelligence. Nothing this document defines
gives Evidence Intelligence any residual claim, reference-holding
privilege, or modification right over an artefact or record once
accepted; a further analysis that needs it again resolves it afresh
through the accepting subsystem's own retrieval boundary (§4), exactly
as any other consumer would.

**One invocation may produce many outputs, of mixed kinds, or none.**
The operation defined in §10 returns a list of `EvidenceAnalysisResult`
values — zero, one, or many — rather than assuming exactly one output
per analysis. A comparison across several documents, for example, may
produce several candidate `CONTRADICTS` relationships and one transient
summary in a single invocation; an analysis that finds nothing
noteworthy may legitimately return an empty list, which is not itself a
failure (§11).

**A Knowledge Candidate may reference only already-accepted Memory Core
evidence.** Programme 3 Contract Design V2 requires a `KnowledgeCandidate`
to reference Memory Core evidence that already exists, carrying a
governed identifier already assigned. A single `EvidenceAnalysisResult`
list must therefore never combine a newly produced, not-yet-accepted
`CandidateAssertion`/`CandidateRelationship` with a `KnowledgeCandidate`
purporting to reference it — no governed identifier exists for the
Knowledge Candidate to name until acceptance (§6) has actually occurred.
Where an analysis proposes both a new Memory Core record and a Knowledge
Candidate that should reference it, the Knowledge Candidate is
constructed only once that record has been accepted and assigned its
governed identifier — never within the same result list that first
proposes the record. This document does not prescribe whether that
requires a second `EvidenceIntelligence` invocation, a step within
acceptance orchestration, or some other mechanism (§6) — only that this
ordering is mandatory.

**Acceptance itself is never performed by Evidence Intelligence.**
Producing a candidate (this section) and accepting it into the governed
subsystem that owns it (§6) are two distinct responsibilities. Evidence
Intelligence performs only the first; the second belongs to a separate
responsibility this document does not assign to Evidence Intelligence.

**Transient output traceability is claim-level, not merely
output-level.** It is not sufficient for a `TransientOutput` to carry one
general reference list alongside an undifferentiated block of prose.
Where a `TransientOutput` states more than one material analytical claim
— for example, several separate observations within one working
chronology, or several distinct points within a draft comparison — each
such claim must itself be traceable back to the specific governed
evidence reference(s) that support it, not only to a reference list for
the output as a whole. This is a clarification of the traceability §1's
"Maintaining traceability" responsibility and CDR-006's mandatory
traceability rule already require, restated at claim level rather than
document level — it introduces no new provenance type, no new
identifier, and no new field. The same, existing `EvidenceArtifactId`
and Memory-Core-addressable reference shapes (§4) already carry this
granularity once a `TransientOutput`'s own prose associates each claim
with the specific reference(s) it draws upon, rather than pooling every
reference into one list at the end.

**Analytical output must distinguish extracted content, observed
content, inferred analytical conclusions, and model-generated
explanatory language.** These are four different evidential kinds of
content, and a `TransientOutput`'s own prose, and any
`CandidateEvidenceArtifact` produced under the "Candidate artefact
produced" row above, must never blur them into one undifferentiated
narrative:

- **Extracted content** — text or data taken directly from a governed
  evidence artefact (an OCR transcription, a direct quotation),
  reproducing what the original already contains.
- **Observed content** — a direct, low-inference description of what a
  governed evidence artefact or Memory Core record contains or shows,
  without adding an evaluative judgement.
- **Inferred analytical conclusions** — a proposition Evidence
  Intelligence's own analysis derives from extracted or observed content
  (a proposed contradiction, a proposed chronological ordering), never
  itself presented as if it were extracted or observed.
- **Model-generated explanatory language** — prose a Reasoning Provider
  Evidence Intelligence orchestrates (§1, §12) produces to explain,
  summarise, or narrate an analytical finding, which must never be
  represented as if it were extracted or observed content itself.

This distinction clarifies how existing output shapes (§3, this section)
must represent what they already carry — it introduces no new
`EvidenceAnalysisResult` variant, no new output type, and no change to
which subsystem owns a produced candidate before or after acceptance
(this section, §6). Where a single piece of output mixes more than one
of these four kinds — for example, an extracted quotation alongside an
inferred conclusion drawn from it — the output must make which portion
is which apparent, so a reader is never left to assume that a model's
own explanatory language is itself extracted evidence.

**Where a `CandidateEvidenceArtifact` is produced by transcription (OCR
or otherwise), it must recognise the distinction between verbatim
transcription, unverified literal transcription, normalised
transcription, and inferred reconstruction, and must never silently
conflate them.** These four carry different evidential meanings: a
**verbatim transcription** reproduces the original's exact characters,
spelling, and layout as read, and is a strict source-fidelity claim that
requires independently established exactness for the classified scope;
an **unverified literal transcription** is a machine-produced attempt to
reproduce readable source content without intentional normalisation,
substantive correction, or inferred reconstruction, where exact
correspondence with the source has not been independently established;
a **normalised transcription** corrects or standardises what was read
(spelling, spacing, character encoding) for readability, at the cost of
no longer being an exact character-for-character reproduction; an
**inferred reconstruction** fills a gap the original does not clearly
support (a damaged, illegible, or ambiguous passage) with Evidence
Intelligence's own best analytical judgement of what the original likely
said. A provider success status, confidence value, fluent output, absence
of warnings, or exact preservation of provider-returned bytes does not
by itself establish verbatim source fidelity. Because these categories
differ in how much of the resulting text is established from the source
versus supplied or transformed by analytical machinery, a
`CandidateEvidenceArtifact` produced by transcription must make which
category applies — or which portions are which, where
more than one applies within the same artefact — apparent, rather than
presenting all four uniformly as if each were an equally direct
rendering of the original. This document does not prescribe the
mechanism by which that distinction is expressed, and introduces no new
artefact class, field, or type to carry it: `CandidateEvidenceArtifact`
(§3, this section) remains Evidence Custodian's own, single, unmodified
type, exactly as already stated; this clarification governs only what
content that existing type may honestly carry when it results from
transcription.

## 6. Acceptance Paths

Producing a candidate (§1, §5) and accepting it into the governed
subsystem that owns it are two distinct responsibilities. **Evidence
Intelligence performs only the first.** Acceptance orchestration —
invoking `EvidenceCustodian.accept`, `MemoryCore`'s own write interface,
or Knowledge Memory's own submission interface — belongs to a separate
responsibility this document does not assign to Evidence Intelligence.
Evidence Intelligence holds no dependency on any of the three acceptance
interfaces (§12), and no operation this document defines (§10) invokes
any of them.

Whichever component eventually performs acceptance orchestration must
still route every candidate through the same already-governed interface
and Permission Engine gate every other caller already uses:

- **Evidence Custodian acceptance.** A `CandidateEvidenceArtifact`
  Evidence Intelligence produces is accepted only through
  `EvidenceCustodian.accept`, gated by Permission Engine exactly as
  Evidence Custodian's own Contract Design §6.6 and the Evidence
  Custodian Implementation Plan already require for every submitter.
  Whoever performs this acceptance resolves to the same
  `PermissionAction`/`ResourceType` pairing every other submitter
  already resolves to; this document introduces no new one.
- **Memory Core acceptance.** A `CandidateAssertion` or
  `CandidateRelationship` Evidence Intelligence produces is accepted
  only through `MemoryCore`'s existing public write interface, gated
  exactly per Memory Core Contract Design §10's own table — the same
  `WRITE` check on `ResourceType.MEMORY`/`DOCUMENT` every other creator
  already resolves to.
- **Knowledge Memory acceptance.** A `KnowledgeCandidate` Evidence
  Intelligence produces is accepted only through Knowledge Memory's
  existing Knowledge Submission interface, gated by Evaluation B
  (Programme 3 Contract Design V2 §7) — a fresh evaluation of the
  submission act itself, which never re-litigates whatever evaluation
  already admitted the underlying evidence into Memory Core (Evaluation
  A).

**Acceptance must occur before a produced record may be referenced by a
Knowledge Candidate.** Restating §5: a `KnowledgeCandidate` may
reference only Memory Core evidence that already carries a governed
identifier at the time the Knowledge Candidate is constructed. Where an
analysis has proposed a new `CandidateAssertion` or
`CandidateRelationship`, that record must first be accepted into Memory
Core — through the acceptance path above — before any Knowledge
Candidate referencing it may be constructed. This ordering is mandatory
regardless of which mechanism performs acceptance orchestration, and no
implementation may collapse the two into a single, simultaneous step
that references an identifier not yet assigned.

**Which mechanism performs acceptance orchestration is a Scope Lock
decision, not fixed here.** CDR-007 §4 names this as an open
implementation shape, and this document does not choose among a
coordinator (mirroring `EvidenceRegistrationCoordinator`'s
already-frozen shape of sequencing two subsystems without either calling
the other), runtime-level orchestration, or another composition
mechanism entirely. This document fixes only the contract boundary:
whichever mechanism is chosen consumes the candidates Evidence
Intelligence's own operation (§10) returns, invokes the three acceptance
interfaces above in the order the sequencing rule directly above
requires, passes every candidate through the Permission Engine gate each
interface already enforces, and is never itself given delete authority,
custody authority, or a bypass of any Permission Engine evaluation. Such
a mechanism, if adopted, is not a new authority — it holds no capability
beyond sequencing calls to interfaces that are already, independently,
fully governed. Evidence Intelligence's own contract ends at producing
the candidate list (§10); nothing beyond that point is Evidence
Intelligence's responsibility.

## 7. Comparison Model

Evidence Intelligence does not, and may not, redefine comparison or
invent a parallel comparison engine. CDR-001 through CDR-003 already
settled this question for Memory Record Comparison: Model B — Canonical
Record Comparison — is adopted, and its nine constitutional guarantees
(Determinism, No mutation, No promotion/no classification, Disclosed
basis, Independence preserved, Comparison is not contradiction
resolution, No ownership transfer, No technology commitment, and
Disclosed asymmetry) are reproduced, and remain frozen, in
`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` Section 18. That capability
— Memory Core's own, promotion-time repetition/frequency comparison — is
not owned by Evidence Intelligence and is not designed, extended, or
reopened by this document.

**Where Evidence Intelligence itself performs comparison as an
analytical step** — comparing two evidence artefacts' content, or two
Memory Core Assertions' statements, for the purpose of proposing a
`SUPPORTS` or `CONTRADICTS` `CandidateRelationship` — this is ordinary
analytical processing under §1 and §9 (Confidence Model), not an
instance of, or a substitute for, the canonical comparison model CDR-003
adopted. CDR-007's own Decision Rules require that any comparison
capability Evidence Intelligence builds be **reconciled with, not
independently re-derived from,** Model B: a future Scope Lock or
Implementation Plan designing Evidence Intelligence's own comparison
analysis must be checked against Model B's nine guarantees and must not
introduce a comparison mechanism that provides materially weaker
guarantees than Memory Core's own adopted model provides for its
narrower purpose. This document does not itself perform that
reconciliation exercise — it fixes only that it is required, and where
(Scope Lock, per CDR-007 §4's own deferral).

**The output of any comparison Evidence Intelligence performs is always
a `CandidateRelationship`** (§5), never a bespoke comparison-result
type. A finding that two accounts contradict, support, or are otherwise
related is expressed exactly as any other Memory Core fact Evidence
Intelligence proposes — through the existing `SUPPORTS`/`CONTRADICTS`
relationship types Memory Core Contract Design §8 already names as
recognised.

## 8. Provenance Model

Evidence Intelligence creates no independent provenance model. Every
`Provenance`/`CandidateProvenance` reference any output it produces
carries is Memory Core's own, unmodified contract (Memory Core Contract
Design §7), used exactly as every other creator of a Memory Core record
already uses it:

- A `CandidateEvidenceArtifact` produced by Evidence Intelligence and
  accepted by Evidence Custodian carries a `CandidateProvenance` whose
  `extractedFrom` or `derivedFrom` field names the original evidence
  artefact it was produced from — the same mechanism CDR-006's mandatory
  traceability rule already requires of every derivative.
- A `CandidateAssertion` or `CandidateRelationship` produced by Evidence
  Intelligence and accepted by Memory Core carries a mandatory
  `Provenance` reference, exactly as every other Assertion or
  Relationship must (Memory Core Contract Design §7, §14 — no creation
  operation succeeds without one).
- A `KnowledgeCandidate` submitted by Evidence Intelligence references
  Memory Core evidence — including its own newly-written Assertions or
  Relationships — never a copy of that evidence, and never a
  provenance reference of any shape other than the one Programme 3
  Contract Design V2 §6 already fixes (identifier-only, immutable once
  issued, never repointed).

**No new field, no new provenance-carrying type, and no relaxation of
Article VIII or Article IX's obligations is introduced anywhere in this
document.** Where Evidence Intelligence's own analysis cannot establish a
provenance fact with confidence (an uncertain source, an unknown
creation date), it represents that honestly through Memory Core's
existing, already-nullable provenance fields — never by fabricating a
value or silently omitting the field (Memory Core Contract Design §14's
own governing principle, unaltered here).

**Transient output's own traceability uses this same governed-reference
mechanism, not a parallel provenance type.** A `TransientOutput` (§5) is
never accepted and therefore never carries Memory Core's own
`Provenance`/`CandidateProvenance` type — that type exists for durable,
accepted records, and this document does not extend it to transient
output. Where §5 requires each material analytical claim within a
`TransientOutput` to be traceable to the specific governed evidence
reference(s) supporting it, that requirement is satisfied entirely
through the same existing, unmodified `EvidenceArtifactId` and
Memory-Core-addressable reference shapes (§4) already named throughout
this document — never through a new field, a new identifier, or a
provenance-shaped type of Evidence Intelligence's own. Strengthening how
those existing references are associated with a `TransientOutput`'s own
individual claims is a clarification of usage, not the introduction of a
second provenance mechanism alongside Memory Core's.

## 9. Confidence Model

Evidence Intelligence's confidence model addresses **analytical
confidence only**, and is deliberately, explicitly separated from three
other things it is never permitted to touch.

**What analytical confidence is.** Evidence Intelligence may compute
intermediate analytical figures — a similarity score, an extraction
confidence, a translation-quality estimate, or any other numeric or
qualitative output that assists its own analysis — as part of producing
the outputs described in §5. Such a computation is ordinary,
unconstitutional analytical machinery; this document does not prescribe
its mechanism, and no Scope Lock decision about it is a constitutional
one.

**What analytical confidence is not**, restated exactly from CDR-007's
own final editorial clarification:

- It is **not** a constitutional evidential classification. No
  analytical computation Evidence Intelligence performs is itself an
  `EvidentialState` assignment, and no output type this document defines
  carries an `EvidentialState` field.
- It is **not** knowledge-promotion confidence. Confidence computation
  for promotion, final evidential-state assignment, knowledge promotion,
  and Knowledge Item lifecycle decisions remain exclusively governed by
  Knowledge Memory (Programme 3 Contract Design V2 §4, §5).
- It is **not** truth authority. Legal or factual truth authority
  belongs to no subsystem, Evidence Intelligence included.

**Evidence Intelligence never populates a durable confidence field.**
Its own analytical confidence computations remain transient working
output only (§5, category 1) — carried, where useful, within a
`TransientOutput`'s own prose and governed-reference list (§3, §5),
never written into any durable record. In particular, Evidence
Intelligence does **not** populate `CandidateAssertion`'s own, existing,
optional `confidence` field (Memory Core Contract Design §6), and does
not populate any other durable confidence field governed by Memory Core
or Knowledge Memory. A `CandidateAssertion` Evidence Intelligence
produces therefore always carries no confidence value from Evidence
Intelligence. Where Knowledge Memory later evaluates a Knowledge
Candidate referencing that Assertion, the absence of a recorded
confidence is handled exactly as Programme 3 Contract Design V2 §3
already requires for any Assertion with no recorded confidence —
Knowledge Memory's own evaluation establishes one, or the resulting
classification expresses that absence honestly; neither path involves
Evidence Intelligence. A `KnowledgeCandidate` Evidence Intelligence
produces carries **no** confidence field of its own in any case
(Programme 3 Contract Design V2 §2) — Knowledge Memory alone computes
whatever confidence its own evaluation attaches at promotion (Programme
3 Contract Design V2 §3, §4). Promotion confidence remains exclusively
within existing Knowledge Memory governance, unaffected and unmodified
by this document.

## 10. Interfaces

Consistent with the one-operation minimalism `MemoryCore`,
`EvidenceCustodian`, and `ReasoningProvider` each already establish,
this document defines exactly one new public interface.

**`EvidenceIntelligence`** — the single public interface for analysis
invocation:

- **One operation:** given an `EvidenceAnalysisRequest` (§4), return a
  list of `EvidenceAnalysisResult` values (§5).
- **Its responsibility ends there.** This operation does not accept
  outputs into any downstream subsystem, does not evaluate permissions,
  and does not persist anything of its own. It interprets its input and
  returns analytical output — restating, for Evidence Intelligence, the
  same discipline `ReasoningProvider.reason`'s own "its responsibility
  ends there" statement already establishes.
- **No separate "Runtime" wrapper interface is needed.** Evidence
  Intelligence's entire contract surface is one request in, a list of
  results out; there is no session, run, or multi-step lifecycle of its
  own to wrap, for the same reason `ReasoningProvider` needs none
  (Reasoning Provider Contract Design §1).
- **No invocation protocol is specified.** Whether this operation is
  synchronous or asynchronous, and how many Reasoning Provider
  invocations it performs internally, if any, is not decided here.

**No further public interface is defined.** In particular, this
document does not define a separate interface for each analysis kind
(no `EvidenceComparator`, no `DocumentExtractor`, no
`ChronologyBuilder`) — `analysisKind`'s own open classification (§4)
carries that distinction without multiplying the interface surface, for
the same reason `Entity.entityType` and `Document.documentType` remain
open string classifications rather than closed type hierarchies.

## 11. Failure Model

Evidence Intelligence's failure model reuses the same shape
`REASONING_PROVIDER_CONTRACT_DESIGN.md` already establishes for exactly
this kind of interface, rather than inventing a parallel one.

**Recoverable failures.** A referenced `EvidenceArtifactId` that cannot
be retrieved (not found, or not authorised for this requester) is
surfaced by `EvidenceCustodian.retrieve`'s own existing result shape —
Evidence Intelligence does not paper over it, retry it silently, or
substitute a fabricated result; it is a rejected input (below). A
Reasoning Provider Evidence Intelligence invokes internally may fault
for a genuine implementation-level reason (timeout, provider crash,
malformed output) exactly as `REASONING_PROVIDER_CONTRACT_DESIGN.md`
Section 3 already allows; such a fault is Evidence Intelligence's own
concrete implementation's concern to signal, by whatever mechanism it
chooses, outside the `EvidenceAnalysisResult` sealed type.

**Constitutional violations.** No field on `EvidenceAnalysisRequest` or
any `EvidenceAnalysisResult` variant can carry a caller-declared
confidence or evidential-state value in the first place (§3, §9) — this
is a structural prevention, not a runtime check Evidence Intelligence
must separately perform. An attempted analysis that would require
Evidence Intelligence to hold custody, deletion, or promotion authority
is likewise structurally impossible, since Evidence Intelligence holds
no dependency capable of exercising any of the three (§12).

**Rejected operations.** An `EvidenceAnalysisRequest` naming no
retrievable evidence and no resolvable Memory Core reference at all is
a request with nothing to analyse; a concrete implementation rejects it
before invoking any analytical or Reasoning Provider step, rather than
returning a fabricated `EvidenceAnalysisResult`. Likewise, constructing a
`KnowledgeCandidate` that would reference a Memory Core record with no
governed identifier yet assigned (§5, §6) is an invalid, rejected
construction, never silently permitted.

**No fourth failure variant is added to `EvidenceAnalysisResult`,
mirroring `ReasoningProviderResponse`'s own precedent exactly.** An
empty result list (§5) means Evidence Intelligence analysed the
available input and confidently found nothing worth proposing — it is a
semantic determination, not a failure. **An empty result list must never
be used as a catch-all for a genuine failure to analyse.** An
implementation that cannot reach a confident determination — because a
referenced artefact could not be retrieved, because an internally
invoked Reasoning Provider faulted, or for any other implementation-level
reason — has not produced an empty, successful result; it has failed,
and must signal that failure by some means other than an empty
`EvidenceAnalysisResult` list, exactly as `ReasoningProviderResponse.NoAction`
must never substitute for a Reasoning Provider's own failure.

**Partial completion is a legitimate, distinct outcome, not confused
with either total success or complete failure.** Where an
`EvidenceAnalysisRequest` references more than one evidence artefact or
Memory Core record, an analysis may legitimately complete only
partially: some referenced inputs may be successfully analysed,
producing genuine `EvidenceAnalysisResult` values (§5), while others
cannot be retrieved or processed, for a recoverable reason already
described above (a referenced `EvidenceArtifactId` that fails
retrieval, an internally invoked Reasoning Provider that faults for one
input but not another). This is neither total success — not every
referenced input was analysed — nor complete failure — genuine
analytical output was still produced for the inputs that could be
analysed — and this document recognises it as its own, third outcome.
Recognising partial completion does not introduce a fourth
`EvidenceAnalysisResult` variant, a partial-result wrapper type, or any
new sealed shape: the successfully analysed portion is represented
exactly as any other successful analysis already is — a non-empty list
of `EvidenceAnalysisResult` values (§5) — and the portion that could not
be retrieved or processed is signalled exactly as "Recoverable failures"
and the paragraph immediately above already require, by some means
other than an empty or fabricated `EvidenceAnalysisResult` list. How a
concrete implementation discloses, alongside its genuine results, which
referenced inputs could not be analysed is a Scope Lock and
Implementation Plan concern, not one this Contract Design resolves; this
document fixes only that partial completion is a real, honestly
representable outcome, distinct from both ends of the success/failure
spectrum, and must never be silently collapsed into either.

## 12. Dependency Model

Every dependency below already exists in this repository's governed
contracts, or is authorised by name below as a new, narrowly-scoped
platform dependency (**Amendment 2**, OCR mechanism).

| Dependency | Direction | Purpose | Already exists in |
| --- | --- | --- | --- |
| `EvidenceCustodian.retrieve` | Evidence Intelligence → Evidence Custodian | Read-only access to custodied originals and derivatives | Evidence Artifact Contract Design, implemented |
| `MemoryRetrieval` | Evidence Intelligence → Memory Core | Read-only access to registered Memory Core records | Memory Core Contract Design |
| `ReasoningProvider` (zero or more) | Evidence Intelligence → Reasoning Provider(s) | Internal analytical mechanism, orchestrated, never itself (as amended, Amendment 1: invoked via `ReasoningSubject.OfEvidenceAnalysisRequest`, wrapping `EvidenceAnalysisRequest` unmodified) | Reasoning Provider Contract Design |
| An OCR mechanism (zero or one) | Evidence Intelligence → OCR mechanism | Internal analytical mechanism, orchestrated, never itself, invoked only when an analysis's own input requires image-to-text interpretation (**Amendment 2**); never a truth authority, never a constitutional classifier, and never itself capable of assigning `EvidentialState` or determining what its own output means | Not yet governed — authorised in principle by Amendment 2; its own concrete contract remains a future, separate Contract Design's responsibility |

**Evidence Intelligence holds no dependency on any acceptance
interface.** `EvidenceCustodian.accept`, `MemoryCore`'s public write
interface, and Knowledge Memory's Knowledge Submission interface are
each already-existing, already-governed interfaces this repository
already relies upon — but they are dependencies of the separate
acceptance-orchestration responsibility (§6), never of Evidence
Intelligence itself. No type or operation this document defines for
Evidence Intelligence (§3, §10) references any of the three.

**Evidence Intelligence holds no dependency on the Permission Engine of
its own.** The two read boundaries it depends on are each already
permission-relevant: `EvidenceCustodian.retrieve` is already
permission-gated (Evidence Artifact Contract Design §6.6);
`MemoryRetrieval` itself performs no internal gating of its own (Memory
Core Contract Design §9's own disclosed correction), so whatever
composes Evidence Intelligence into the running system remains
responsible for gating any sensitive read before its result reaches
Evidence Intelligence, exactly as Memory Core Contract Design §10
already requires of every other caller of `MemoryRetrieval`. This
mirrors exactly `ReasoningProvider`'s own zero-dependency relationship
to the Permission Engine (Reasoning Provider Contract Design,
Constitutional Boundaries) — Evidence Intelligence, like a Reasoning
Provider, is gated by what it calls or by what composes it, not by a
mechanism of its own.

**Evidence Intelligence holds no dependency, at any depth, on:**
`OwnerEvidenceDeletionAuthority`; `EvidenceArtifactStorage.delete`;
`EvidenceDeletionAudit`; `EvidenceCustodian.accept`; `MemoryCore`'s
public write interface; Knowledge Memory's own Knowledge Submission
interface; Knowledge Memory's own promotion, revision, retirement, or
restoration mechanisms; or `EvidentialState`. None of these eight is
reachable from `EvidenceAnalysisRequest`, `EvidenceAnalysisResult`, or
`EvidenceIntelligence` itself, by construction — every field and
operation this document defines is listed in §3 and §10 in full, and
none references any of the eight.

**Whether Evidence Intelligence's own invocation requires a new
Permission Engine proposal class is a Scope Lock decision, governed by
CDR-005's Model C, not decided here.** If Scope Lock finds that invoking
`EvidenceIntelligence`'s own operation — as distinct from the
already-gated reads and writes it performs — itself warrants a
disclosed `PermissionAction`/`ResourceType` pairing (mirroring Evidence
Custodian's own five disclosed conventions), that pairing must be
self-certified against Chapter 10's published admission criteria,
exactly as CDR-005 already requires of every future domain act, and
escalated to a further Constitutional Decision Record only if that
self-certification is genuinely contested. This document does not
perform that self-certification itself, and does not assert that a new
pairing is, or is not, required.

## 13. Repository Reuse

**Reused without modification:**

- `EvidenceCustodian.retrieve`, `EvidenceArtifactId`,
  `CandidateEvidenceArtifact` — Evidence Custodian's own contract,
  reused directly by Evidence Intelligence. `EvidenceCustodian.accept`
  is reused only by the separate acceptance-orchestration responsibility
  (§6), never directly by Evidence Intelligence itself.
- `Document`, `CandidateDocument`, `Assertion`, `CandidateAssertion`,
  `Relationship`, `CandidateRelationship` (including `SUPPORTS`/
  `CONTRADICTS`), `Provenance`, `CandidateProvenance` (including
  `derivedFrom`/`extractedFrom`), `MemoryRetrieval` — Memory Core's own
  contract, reused directly by Evidence Intelligence. `MemoryCore`'s
  public write interface is reused only by the separate
  acceptance-orchestration responsibility (§6), never directly by
  Evidence Intelligence itself.
- `KnowledgeCandidate` — Programme 3's own contract, produced by
  Evidence Intelligence but submitted only by the separate
  acceptance-orchestration responsibility (§6), through Knowledge
  Memory's own Knowledge Submission interface and Evaluation B
  permission boundary, neither of which Evidence Intelligence itself
  depends on. `KnowledgeItem` and `EvidentialState` are reused only in
  the sense that Evidence Intelligence's outputs may eventually be
  judged against them by Knowledge Memory — Evidence Intelligence's own
  model never constructs, holds, or references either directly.
- `ReasoningProvider`, `ReasoningProviderRequest`,
  `ReasoningProviderResponse`, `ReasoningContext`, `ReasoningSubject` —
  the Reasoning Provider's own contract; `ReasoningProviderRequest` now
  amended (Amendment 1) to carry `subject: ReasoningSubject`;
  `ReasoningProvider`, `ReasoningProviderResponse`, and `ReasoningContext`
  remain unbroadened; none modified *by Evidence Intelligence*, which
  reuses `ReasoningSubject` unmodified via its own
  `OfEvidenceAnalysisRequest` case.
- `PrincipalId` — an existing platform identifier, reused for audit
  purposes only.
- CDR-001/002/003's canonical comparison model (Model B) — reused by
  reconciliation requirement (§7), never re-derived.
- The Permission Engine's existing gating mechanism at every boundary
  Evidence Intelligence calls (§12) — reused, never duplicated.

**Extended:**

- **Memory Core's `SUPPORTS`/`CONTRADICTS` relationship types** are
  extended, in the ordinary sense every new caller of an open
  classification extends its use — not in the sense of altering
  `Relationship`'s own contract. Evidence Intelligence becomes a new
  producer of these already-recognised relationship kinds; no new
  relationship type is introduced.
- **CDR-004's structural precedent** (a generically defined capability
  covering a previously-unaddressed case without reopening its own
  frozen text) is extended by analogy to justify, at Scope Lock, how
  Evidence Intelligence's own comparison analysis is reconciled with
  Model B (§7) — the precedent itself is not reopened or altered.

**Not reused, because no existing shape exists to reuse:**

- `EvidenceAnalysisRequest` and `EvidenceAnalysisResult` (§3) — genuinely
  new, and owned exclusively by Evidence Intelligence.

**Not adopted, and explicitly rejected as unnecessary:**

- A new evidence store, memory system, comparison engine, provenance
  model, or promotion system of Evidence Intelligence's own — each
  already rejected by CDR-007 §Decision and Repository Reuse Summary,
  and not reconsidered here.
- A new Reasoning Provider abstraction specific to Evidence Intelligence
  — rejected by CDR-007 for the same reason; Evidence Intelligence
  orchestrates the existing one.
- A separate `EvidenceIntelligenceRegistry` or similar discovery
  mechanism — no concrete need is demonstrated anywhere in CDR-007 or
  this document, mirroring `ReasoningProviderRegistry`'s own,
  already-excluded precedent.

## 14. Architectural Boundaries

Evidence Intelligence's position relative to every adjacent subsystem,
stated once here as a single diagram and then demonstrated point by
point:

```text
Evidence Custodian                 Reasoning Provider(s)
        |                                   ^
        | (read-only)          (orchestrated, |
        v                        internal use) |
   Evidence Intelligence  ---------------------+
        |
        | (produces candidates only —
        |  no acceptance dependency, §6, §12)
        v
 Acceptance Orchestration (separate responsibility, §6;
 mechanism deferred to Scope Lock)
        |
        v
Evidence Custodian / Memory Core  --------->  Knowledge Memory
```

This diagram states consumption and production relationships, never
containment or call-authority ones — mirroring exactly the discipline
Evidence Artifact Contract Design §6 already established for Evidence
Custodian's own boundary diagram. Evidence Intelligence is a peer of
Evidence Custodian and Knowledge Memory in organisational tier, never a
component of either, and never organisationally superior to either
(CDR-007 §1).

**Evidence Intelligence analyses.** Every operation this document
defines (§1, §10) performs analysis and nothing else — it interprets
governed input and returns candidate output.

**Evidence Intelligence produces analytical artefacts.** Every output
this document defines (§5) is one of CDR-007's four permitted
categories; none is a new kind of artefact outside that classification.

**Evidence Intelligence never orchestrates acceptance.** Producing a
candidate and accepting it into its governing subsystem are two
distinct responsibilities (§1, §2, §6); Evidence Intelligence performs
only the first, and holds no dependency capable of performing the
second (§12).

**Evidence Intelligence never governs.** It holds no Permission Engine
dependency of its own (§12); it cannot authorise its own actions, expand
its own scope, or create a path around any subsystem's existing gate
(§2). It has no independent epistemic-authorisation mechanism (CDR-007
§3).

**Evidence Intelligence never stores.** It holds no persistence
responsibility of any kind (§2); every durable output it produces is
held, once accepted by the separate acceptance-orchestration
responsibility (§6), by the subsystem that governs it, never by
Evidence Intelligence itself.

**Evidence Intelligence never classifies constitutionally.** It cannot
assign an `EvidentialState`, cannot promote a `KnowledgeCandidate` into
a `KnowledgeItem`, and cannot declare a proposition true (§2, §9). Its
own confidence model is analytical only, explicitly and permanently
separated from constitutional classification (§9).

## 15. Verification Requirements

The following behavioural contract verification is expected before any
implementation of this Contract Design proceeds. **No implementation
detail is prescribed here** — these are properties a future Scope Lock
and Implementation Plan must be capable of demonstrating, by whatever
concrete mechanism they choose, preferring structural or architectural
verification over source-text pattern matching wherever a structural
mechanism can prove the same invariant, consistent with this Programme's
own established preference (Evidence Custodian Phase 8).

- **No dependency reachability.** No class implementing
  `EvidenceIntelligence`, and no type reachable from
  `EvidenceAnalysisRequest` or `EvidenceAnalysisResult`, holds a
  reference — directly, or through any constructor parameter, function
  parameter, or property, at any depth — to `OwnerEvidenceDeletionAuthority`,
  `EvidenceArtifactStorage`, `EvidenceDeletionAudit`,
  `EvidenceCustodian.accept`, `MemoryCore`'s public write interface,
  Knowledge Memory's Knowledge Submission interface, or any Knowledge
  Memory promotion/revision/retirement/restoration mechanism (§12).
- **No durable confidence or evidential-state authority.** No type this
  document defines carries an `EvidentialState` field, and no code path
  by which Evidence Intelligence produces a `CandidateAssertion`
  populates that Assertion's `confidence` field (§9) — the field remains
  reachable on `CandidateAssertion` as Memory Core's own contract
  already defines it, but no Evidence Intelligence code path writes to
  it.
- **Acceptance-path exclusivity.** Every `CandidateEvidenceArtifact`,
  `CandidateAssertion`, `CandidateRelationship`, and `KnowledgeCandidate`
  Evidence Intelligence produces reaches its accepting subsystem only
  through that subsystem's own existing, unmodified acceptance
  interface, invoked only by the separate acceptance-orchestration
  responsibility (§6) — never invoked by Evidence Intelligence itself,
  and never through any other alternate path.
- **Sequencing correctness.** No `KnowledgeCandidate` is ever
  constructed referencing a Memory Core record that has not yet been
  accepted and assigned a governed identifier; an implementation that
  constructs both within a single, pre-acceptance step is non-compliant
  (§5, §6).
- **Transient output remains provenance-resolvable.** Every
  `TransientOutput` Evidence Intelligence produces carries at least the
  governed references (§3, §4) it was drawn from, sufficient to resolve
  back to the evidence or Memory Core records the transient output
  describes.
- **Ownership-transfer correctness.** No test, and no production code
  path, treats Evidence Intelligence as retaining any reference,
  modification right, or residual claim over an artefact or record after
  it has been accepted by Evidence Custodian, Memory Core, or Knowledge
  Memory (§5).
- **Reasoning Provider orchestration, not identity.** No class
  implementing `EvidenceIntelligence` itself implements
  `ReasoningProvider`, and no `ReasoningProvider` implementation holds a
  reference back to `EvidenceIntelligence` (Reasoning Provider Contract
  Design's own statelessness and non-retention guarantees, unmodified).
- **Comparison reconciliation.** Any comparison capability implemented
  under §7 is checked, at implementation time, against Model B's nine
  constitutional guarantees (`MEMORY_CORE_SCOPE_LOCK.md` §18), and any
  material divergence is disclosed and justified, not silently
  introduced.
- **Contradiction preservation.** Given two Memory Core Assertions
  connected, or connectable, by a genuine contradiction, an Evidence
  Intelligence analysis of them never silently produces only a
  `SUPPORTS`-typed output for one side, and never omits the
  contradiction from any transient output describing them.
- **Failure signalling.** An implementation that cannot reach a
  confident analytical determination never returns an empty
  `EvidenceAnalysisResult` list as its signal for that failure; it
  signals failure by some other, disclosed means (§11).

---

## Out of Scope

This document does not define, and no future reader should treat it as
having defined:

- any storage technology, file system, database, or object store;
- any hashing algorithm, integrity-verification scheme, or cryptographic
  method;
- any API, RPC contract, or wire format;
- any Kotlin interface, class, enum, or method signature;
- any database schema or persistence model;
- a Scope Lock or Implementation Plan for Evidence Intelligence — both
  remain the next, separate governance stages;
- the specific mechanism by which `EvidenceAnalysisRequest`'s Memory
  Core references or Reasoning Provider invocations are assembled — an
  implementation detail, not a contract shape;
- the specific mechanism (a coordinator, runtime orchestration, or
  another composition mechanism) that performs acceptance orchestration
  on behalf of the candidates Evidence Intelligence produces — a Scope
  Lock decision; Evidence Intelligence itself holds no dependency on any
  acceptance interface under any mechanism chosen (§6, §12);
- whether a new `PermissionAction`/`ResourceType` pairing is required
  for Evidence Intelligence's own invocation — a Scope Lock decision,
  governed by CDR-005's Model C (§12), not decided here;
- any specific analysis kind's own internal algorithm (how comparison,
  OCR, translation, or summarisation is actually performed);
- any amendment to Memory Core's, Evidence Custodian's, or Knowledge
  Memory's own contract, schema, or interface — none is proposed, and
  none is required by anything in this document;
- any question of legal ownership, copyright, proprietary interest, or
  lawful possession of any artefact — permanently out of scope per
  CDR-006, unaffected here.

---

## Design Rules Compliance

A direct self-check against the design rules governing this document,
performed once here rather than asserted without demonstration:

**Prefer reuse; prefer composition; prefer references; avoid
duplication.** §3's table shows three new types against ten reused,
unmodified ones; every candidate output (§5) carries an existing type by
reference, never a copy.

**Avoid wrappers.** Acceptance orchestration (§6) is deliberately left
unassigned to any specific mechanism — a coordinator, runtime
orchestration, or another composition mechanism are each compliant,
provided none becomes a new authority (§12) — but Evidence Intelligence
itself is never the one performing it, keeping Evidence Intelligence
itself free of any wrapping responsibility over interfaces that are
already, independently, fully governed.

**Avoid mirrors; avoid shadow models.** No type this document defines
duplicates the shape of `EvidenceArtifact`, `Assertion`, `Relationship`,
or `KnowledgeItem` (§3, restated from the governing instruction's own
explicit prohibition); each output variant carries the existing
candidate type outright.

**Avoid introducing new authority.** Evidence Intelligence holds no
Permission Engine dependency of its own (§12); every gate it is subject
to already exists at a boundary it calls, never one this document
invents.

**Every public object has exactly one constitutional owner.** Stated
explicitly, per type, in §3's table — no type this document touches has
an ambiguous or shared owner.

---

## Final Recommendation

This Contract Design defines Evidence Intelligence's software contract
exactly as CDR-007 governs it: three new public types, one new public
interface, zero new first-class or peer platform subsystems — Amendment
2 (Status, above) authorises one additional capability-level dependency
inside the existing Evidence Intelligence subsystem, never a subsystem
of its own — no amendment to any existing contract introduced *by this
document* — Reasoning Provider Contract
Design has separately been amended by its own governing document
(Amendment 1: `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`),
and Evidence Intelligence reuses that already-amended contract unchanged
— and every analytical output traced to one of CDR-007's own
four permitted categories with an unambiguous accepting subsystem and
ownership-transfer rule. Following the Independent Contract Design
Review's four corrections, this document now maintains a single,
consistent responsibility model throughout: Evidence Intelligence
analyses governed evidence and produces governed analytical candidates
only (§1, §2, §6, §12, §14); acceptance orchestration is a distinct,
unassigned responsibility (§6); Knowledge Candidate construction is
sequenced strictly after the Memory Core evidence it references has
been accepted (§5, §6, §11, §15); transient output remains
provenance-resolvable through existing identifiers alone (§3, §5, §15);
and no durable confidence field is ever populated by Evidence
Intelligence (§2, §9, §15). It resolves no constitutional question
CDR-007 did not already resolve, and reopens none that CDR-006, CDR-001
through CDR-005, Memory Core's, Evidence Custodian's, Programme 3's, or
the Reasoning Provider's own governance already settled.

The next governance stage — an Evidence Intelligence Scope Lock,
followed by an Implementation Plan — is now authorised to begin,
following the Independent Constitutional Review's acceptance of this
Contract Design; neither is begun by this document itself.

EVIDENCE INTELLIGENCE CONTRACT DESIGN — ACCEPTED — CANONICAL — READY FOR
SCOPE LOCK

Confirmed: no Kotlin implemented; no API, schema, or storage technology
defined; CDR-001 through CDR-007 not modified; Memory Core Contract
Design, Evidence Artifact Contract Design, and Programme 3 Knowledge
Memory Contract Design V2 not modified; the Reasoning Provider Contract
Design, already amended by its own Amendment 1, is reused here unchanged
— this document introduces no further amendment to it; nothing staged;
nothing committed; nothing pushed; Evidence Intelligence Scope Lock not
started.
