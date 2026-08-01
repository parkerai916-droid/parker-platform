# CDR-007: Constitutional Classification of Evidence Intelligence

## Status

**Accepted. Canonical. Frozen.** Independent Constitutional Verification
and Final Freeze Verification have both been completed. This
Constitutional Decision Record is now adopted and becomes normative
governance for Parker. No Kotlin is implemented, proposed, or changed by
this record. Neither `src/` nor `tests/` is touched. `docs/architecture/epistemic-integrity.md`,
`docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`,
`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`,
`docs/architecture/user-authorship-and-evidence.md`, and
`docs/decisions/CDR-001` through `CDR-006` are all unmodified by this
record. Nothing is staged, committed, or pushed. This record is a
constitutional classification only — it is not a Contract Design, not a
Scope Lock, and not an Implementation Plan, and it does not authorise
any of those to begin.

## Context

The Evidence Custodian programme is complete (Phases 1–10, verified,
committed, pushed). A read-only Programme Boundary and Planning Review
for a successor programme, Evidence Intelligence, was performed and
accepted. That review found that CDR-006 and its Contract Design each
deliberately declined to decide Evidence Intelligence's own
constitutional shape, deferring it explicitly:

- CDR-006's own Decision Rules: *"'Evidence Intelligence' and 'Document
  Handling' are used here as provisional labels for whichever subsystem
  is eventually built to hold custody; this record does not decide, and
  leaves to the Evidence Artifact Contract Design, whether that
  subsystem is organised as part of a broader analytical Evidence
  Intelligence programme or as a separate, first-class infrastructure
  subsystem."*
- The Evidence Artifact Contract Design, in turn, fixed the
  Custodian/Evidence Intelligence *boundary* (§6.3) but explicitly left
  "the shape, governance, or internal structure of Evidence Intelligence
  or any other analytical capability... only its boundary with the
  Custodian" (§10, Out of Scope) to a document neither it nor any later
  Evidence Custodian document ever wrote.

This record is that document. It reviewed, in full: the Parker
Constitution; Epistemic Integrity Amendment No. 1 (all 19 Articles); CDR-001
through CDR-006; the Evidence Artifact Contract Design; the Evidence
Custodian Scope Lock; the Evidence Custodian Implementation Plan; the
Evidence Custodian Phase 7 Boundary Clarification; Programme 3's Knowledge
Memory Contract Design (Version 2); `user-authorship-and-evidence.md`;
`docs/architecture/21-document-intelligence.md`; and the current production
shapes of `MemoryCore`, `KnowledgeStore`, `EvidentialState`, and
`ReasoningProvider`. It invents no new mechanism where an existing one
already answers the question — the Repository Reuse Summary, below,
states exactly what was reused rather than re-derived, and what was
deliberately not adopted rather than silently omitted.

## Constitutional Purpose

Evidence Intelligence exists to transform governed evidence into
governed analytical products while preserving complete provenance, and
without acquiring authority over truth, custody, or knowledge promotion.

This is Evidence Intelligence's defining constitutional statement.
Every section that follows exists to make it precise and enforceable,
not to add to it.

## Decision Question

> What is Evidence Intelligence, constitutionally — what is it not, where
> does its boundary lie, which already-existing Parker subsystems does it
> reuse without modification, which responsibilities remain exclusively
> owned elsewhere, and which analytical artefacts is it permitted to
> produce?

## Constitutional Constraints

Any answer must remain consistent with:

- **Article XIX (Epistemic Integrity).** Evidence Intelligence is already
  named, by the Constitution itself, as a subsystem bound in full by
  Epistemic Integrity Amendment No. 1. This record does not grant that
  binding — it already exists — and cannot narrow it.
- **Article XV (Epistemic Integrity).** No subsystem that generates
  candidate inferences, explanations, or conclusions — including,
  without limitation, Evidence Intelligence — may assign final
  evidential status to its own output. This separation is structural,
  not a matter of internal restraint.
- **Article III, IV, and XVI (Epistemic Integrity).** No material
  proposition may be represented as exclusive where reasonable competing
  explanations exist, and Parker's understanding must remain capable of
  preserving, not erasing, conflicting evidence. Evidence Intelligence
  must never discard, suppress, or silently omit contradictory evidence
  merely because a preferred interpretation exists — this restates, and
  does not add to, Article III's "reasonable competing explanations"
  discipline, Article IV's prohibition on elevating one account to
  exclusivity without justification, and Programme 3's own existing rule
  that Knowledge Memory "must not promote one and silently omit the
  other" where a `CONTRADICTS` relationship exists between two
  Assertions.
- **CDR-006's Model B and its Decision Rules**, unmodified, unreopened:
  the Evidence Custodian, not Evidence Intelligence, holds technical
  custody; Evidence Intelligence never acquires custody, modification,
  or deletion authority merely by consuming custodied evidence; the
  Constitutional Optimisation Safeguard binds every subsystem, named or
  not.
- **Contract Design §6.3**, unmodified, unreopened: Evidence Intelligence
  is "a separate, downstream subsystem," never a component of, and never
  organisationally superior to, the Evidence Custodian.
- **Programme 3 Contract Design V2's own governing statement**,
  unmodified, unreopened: *"Memory Core remains the constitutional source
  of stored evidence. Knowledge Memory becomes the constitutional source
  of promoted knowledge."* Neither role is available for Evidence
  Intelligence to assume.
- **The existing `ReasoningProvider` abstraction**, unmodified,
  unbroadened: it remains a pure callee with no custody, provenance, or
  promotion obligations of its own, regardless of who calls it.
- **Minimal-reopening discipline** (CDR-004/CDR-005's own precedent):
  a model that does not force reopening an already-frozen document
  without a repository-grounded reason is preferred over one that does.
- **Maximise reuse.** Per this record's own governing instruction: where
  an existing contract, record type, provenance mechanism, relationship
  model, retrieval mechanism, or permission mechanism already satisfies a
  need, this record adopts it and does not invent a parallel one.

## 1. What Evidence Intelligence Is

**Evidence Intelligence is the Parker capability responsible for
analytical processing of evidence already held in Evidence Custodian
custody, and for proposing — never authoritatively asserting — candidate
propositions, comparisons, and derivative artefacts arising from that
evidence, bound in full by Epistemic Integrity Amendment No. 1 and
holding no authority of its own to determine what any of its outputs may
finally be represented as.**

Its constitutional role restates, on the epistemic axis, the same
discipline the Constitution already applies on the trust axis:

> Evidence Intelligence proposes. Knowledge Memory's existing constitutional
> review determines what may be represented as knowledge. Neither Evidence
> Custodian's custody nor Memory Core's registration is disturbed by
> either step.

This is not a new principle. It is Article XV ("Cognition proposes.
Epistemic governance determines what may be claimed") and the
Constitution's own "cognition proposes, trust authorises, runtime
executes" restated for the specific case of a subsystem that reasons
about custodied evidence, rather than one that executes actions.

Evidence Intelligence is a **first-class, downstream, analytical
subsystem** — a peer of Evidence Custodian and Knowledge Memory in
organisational tier, never a component of either, mirroring exactly the
peer relationship the Contract Design already established between
Evidence Custodian and Memory Core (§6.1).

**The capabilities previously discussed under "Document Intelligence" are
classified, by this record, as Evidence Intelligence's own.**
`docs/architecture/21-document-intelligence.md` was never a ratified
constitutional decision — it carries no `Status:` header, no
ratification, and no relationship statement to CDR-006. It is historical
architectural background, predating this Programme's own governance
discipline, and carries no constitutional authority. CDR-006 itself
already treated "Evidence Intelligence" and "Document Handling"/"Document
Intelligence" as *provisional labels for the same not-yet-governed
subsystem*, not as two named alternatives, and the Contract Design's own
exclusion list (§5, "Perform OCR") already attributes OCR to "Evidence
Intelligence." This record simply completes that classification: document
ingestion, OCR, transcription, and extraction are Evidence Intelligence's
own analytical functions.

**Evidence Intelligence orchestrates governed analytical processing; it is
not itself a Reasoning Provider.** It may employ one or more existing
`ReasoningProvider` implementations as internal analytical mechanisms — a
pattern mirroring `ConversationTurnReasoningCoordinator`'s own existing
use of the same interface — but reasoning is one capability Evidence
Intelligence may draw upon, never its constitutional identity. The
`ReasoningProvider` abstraction itself is not broadened, extended, or
reinterpreted by this record: it remains exactly what the Reasoning
Provider Contract Design and `src/interfaces/ReasoningProvider.kt` already
define it to be — a pure callee with no custody, provenance, or promotion
obligations of its own. Evidence Intelligence's own obligations — governed
evidence access (Contract Design §6.3), provenance (Article VIII,
CDR-006), and the Knowledge-Memory-submission boundary (Programme 3 §7) —
belong to Evidence Intelligence as the orchestrating subsystem, never to
whichever Reasoning Provider it may internally invoke. This obligation
does not depend on classifying Evidence Intelligence as a reasoning
provider under Article I: Article XIX already binds Evidence Intelligence
directly, by name, as a subsystem that "transforms" and "represents"
material propositions — the same obligation attaches whether or not any
internal Reasoning Provider call occurs at all.

## 2. What Evidence Intelligence Is Not

- **Not an evidence store.** Evidence Custodian already is this,
  exclusively (CDR-006; Contract Design §3–4). Evidence Intelligence
  holds no original evidence of its own and never acquires one; it holds,
  at most, a governed, observational reference obtained through
  `EvidenceCustodian.retrieve`.
- **Not a memory system.** Memory Core already is this, exclusively
  (Memory Core Contract Design §1, unmodified, unreopened). Evidence
  Intelligence does not duplicate registration, identity assignment, or
  provenance ownership — it is a *caller* of Memory Core's existing
  contract, exactly as Evidence Registration Coordinator and Knowledge
  Memory already are.
- **Not a truth authority.** Epistemic Integrity Articles III, VI, and
  VII already foreclose *any* subsystem from declaring truth. Evidence
  Intelligence's outputs are, at most, candidate propositions carrying a
  provisional evidential characterisation; final evidential-state
  assignment remains exclusively Knowledge Memory's, per Article XV and
  Programme 3 Contract Design V2 §4 ("Who assigns it").
- **Not a Reasoning Provider.** Evidence Intelligence may employ one or
  more existing `ReasoningProvider` implementations internally, exactly
  as other coordinators in this repository already do, but it is not
  itself classified as one. Article XIX names "Evidence Intelligence"
  and "successor reasoning providers" as two separate items in its own
  illustrative list — the Constitution itself treats them as distinct
  categories. Reasoning is a capability Evidence Intelligence may draw
  upon; it is never Evidence Intelligence's own constitutional identity,
  and the `ReasoningProvider` abstraction is not extended to describe
  Evidence Intelligence itself.
- **Not a constitutional authority.** "Parker owns authority. Modules
  provide capability" applies to Evidence Intelligence exactly as to any
  other module. It cannot authorise its own actions, expand its own
  scope, or create a path around Permission Engine evaluation.
- **Not a replacement for Knowledge Memory.** Knowledge Memory retains
  exclusive ownership of promotion decisions, final evidential-state
  assignment, and the durable Knowledge Item lifecycle (Programme 3
  Contract Design V2, unmodified, unreopened by this record). Evidence
  Intelligence may *submit* to that pipeline; it may never perform any
  part of it itself.
- **Not a replacement for Evidence Custodian.** Evidence Custodian
  retains exclusive custody, immutability enforcement, and deletion
  authority (CDR-006; Contract Design; Scope Lock; Phase 7 Boundary
  Clarification — none reopened). Evidence Intelligence has no custody
  authority, no modification capability, and no deletion capability over
  any custodied original, regardless of how extensively it analyses that
  original (Contract Design §6.3, restated, not altered, here).

## 3. Constitutional Boundary

**Upstream dependencies:**

- **Evidence Custodian**, read-only, through the already-frozen
  `EvidenceCustodian.retrieve` boundary (Contract Design §6.3). No new
  read path is authorised or required.
- **Memory Core**, as a referencing caller, through its already-frozen
  `MemoryCore`/`MemoryRetrieval` contracts — never as an owner of
  identity, registration, or provenance.
- **One or more Reasoning Providers** (optional, internal), through the
  already-frozen `ReasoningProvider` interface, as a pure callee exactly
  as every existing caller of it already treats it.

**Downstream consumers:**

- **Knowledge Memory**, as the sole path by which anything Evidence
  Intelligence produces may become durable, authoritative knowledge
  (Programme 3 §7, Evaluation B) — a consumption relationship in the
  same sense Evidence Intelligence itself consumes Evidence Custodian,
  never a call-authority relationship in the other direction.
- **Reasoning Context assembly**, only indirectly, through whatever
  Memory/Knowledge/World Model sources already expose to it today.
  This record does not make Evidence Intelligence a new, direct
  Reasoning Context source.

**Constitutional interfaces:**

- **The Permission Engine** — mandatory on every act of Evidence
  Intelligence's own that changes custody state, writes a Memory Core
  record, or submits a Knowledge Candidate. No new gating *mechanism* is
  authorised; Evidence Intelligence is a new *caller* of the one that
  already governs every other subsystem.
- **Knowledge Memory's own promotion evaluation** — the embodiment,
  already built, of Article XV's "constitutional review" requirement.
  Evidence Intelligence has no independent epistemic-authorisation
  mechanism of its own and is not authorised to build one.

**Explicitly outside Evidence Intelligence's boundary:**

- Custody, immutability enforcement, and deletion of original evidence
  (Evidence Custodian's exclusive domain).
- Identity assignment, registration, and provenance ownership for Memory
  Core records in general (Memory Core's exclusive domain — Evidence
  Intelligence may be a governed *writer* of specific record instances,
  exactly as other callers already are, without owning the contract).
- Promotion, final evidential-state assignment, and Knowledge Item
  lifecycle management (Knowledge Memory's exclusive domain).
- Legal or factual truth determination (no subsystem's domain, per
  Epistemic Integrity Articles III, VI, VII).
- Sending, filing, publishing, or otherwise executing any drafted or
  derived output. `user-authorship-and-evidence.md` is "deliberately
  silent" on this for communication assistance, and the same silence
  extends here: that authority is the Permission Engine's and the
  Execution Pipeline's alone.

## 4. Existing Parker Subsystems Reused

The following are adopted, unmodified, as sufficient for Evidence
Intelligence's constitutional needs. None requires a new contract:

- `EvidenceCustodian.retrieve` — the sole read path to original evidence.
- `EvidenceCustodian.accept` (or a coordinator mirroring
  `EvidenceRegistrationCoordinator`'s already-frozen shape) — the sole
  path by which any Evidence-Intelligence-produced derivative (an OCR
  transcript, a translation) enters custody as its own, separately
  identified artefact.
- `Provenance`/`CandidateProvenance`, including the already-existing
  `derivedFrom`/`extractedFrom` fields — the sole mechanism connecting a
  derivative to its original (already required by CDR-006's own
  "mandatory traceability" rule).
- `Document`/`CandidateDocument` — for registering any derivative
  artefact, exactly as Evidence Custodian's own registration path
  already does.
- `Assertion`/`CandidateAssertion`, including its existing, optional
  `confidence: Double?` field — the record shape for a proposed claim.
- `Relationship`/`CandidateRelationship`, including its already-named
  `SUPPORTS`/`CONTRADICTS` relationship types and its open,
  non-enumerated `RelationshipEndpoint.recordKind` — the record shape for
  comparison and contradiction findings, requiring no new Memory Core
  record type.
- `MemoryRetrieval`'s existing time-range and relationship-traversal
  queries — sufficient for timeline and issue-mapping needs without new
  retrieval infrastructure.
- `EvidentialState` (Article IV, fourteen states, already implemented) —
  the sole formal evidential-classification vocabulary for anything
  Evidence Intelligence proposes, once classified.
- `KnowledgeCandidate` and Knowledge Memory's own promotion evaluation
  (Programme 3 §7, Evaluation B) — the sole path to durable, authoritative
  knowledge; already forbids a submitter from declaring its own
  confidence or evidential state, which answers, without a new rule,
  what would otherwise need deciding here.
- `ReasoningProvider`, unmodified and unbroadened — for any internal
  reasoning step Evidence Intelligence chooses to employ, subject to
  Article XV's already-existing constraint and Section 1's own
  orchestration/identity distinction, above.
- The Permission Engine / `PermissionPolicyRule` / `ActionVocabulary` /
  `ResourceRegistry` mechanism, and its established composition-root
  convention (disclosed-but-unregistered action/resource pairs) — the
  same admission pattern already used for every gated action in this
  repository, including Evidence Custodian's own five.
- CDR-001/002/003's already-adopted canonical comparison model — the
  governing basis for any Evidence Intelligence comparison capability;
  not re-derived here.
- CDR-006's own "Derived Work Product is never evidence" rule and its
  mandatory-traceability rule — already, directly, by name, governing
  much of what Section 6 below would otherwise need to newly decide.

No new `PermissionAction` or `ResourceType` value is found necessary by
this record. Whether `ResourceType.MEMORY` suffices for gating a Knowledge
Candidate submission, or a new value is eventually justified, is left to
Contract Design — a technology/mapping-level question, not a
constitutional one.

## 5. Exclusive Responsibilities

| Subsystem | Owns exclusively | Evidence Intelligence may not |
| --- | --- | --- |
| **Evidence Custodian** | Custody; immutability enforcement while retained; deletion authorisation and execution; physical storage of original evidence bytes | Acquire custody, modification, or deletion authority over any original, however extensively it analyses it (Contract Design §6.3, restated) |
| **Memory Core** | Identity assignment; registration; the general-purpose fact-registration substrate itself | Assign its own identifiers, bypass registration, or treat itself as an alternative record-of-fact to Memory Core |
| **Knowledge Memory** | Promotion decisions; final evidential-state assignment; durable Knowledge Item lifecycle (promotion/revision/retirement/restoration); the Article XV "constitutional review" function | Promote anything itself, assign final evidential state to anything itself, or build a parallel promotion mechanism of its own |
| **Reasoning Provider(s)** | Generation of candidate inferences from a supplied context, and nothing more (its own already-frozen contract: "never executes or authorises actions") | Treat a Reasoning Provider's own output as already-authorised or already-classified, or blur its own identity with a Reasoning Provider it employs |

This table restates, consolidates, and cross-references already-frozen
boundaries; it creates none of them.

## 6. Permitted Analytical Artefacts

Four categories, in ascending order of constitutional weight, each
illustrated with representative examples. **The examples are illustrative
only — they classify what kind of thing an artefact is constitutionally;
they do not design its storage, format, or implementation.**

1. **Transient, discardable working output.** Anything Evidence
   Intelligence produces that is never accepted into Evidence Custodian,
   never written to Memory Core, and never submitted as a Knowledge
   Candidate carries no evidentiary status, may be freely regenerated or
   discarded, and must never be represented to a user or another
   subsystem as though it had passed through governed acceptance when it
   has not. This is CDR-006's own "Derived Work Product is never
   evidence" rule, adopted here without modification, extended to cover
   every artefact at this stage regardless of eventual disposition.
   *Representative examples, before acceptance or submission:* a draft
   OCR transcription; an in-progress translation; a draft document
   comparison; a working contradiction report; a draft timeline or
   chronology; a working issue map or entity graph; a draft analytical
   summary or analytical report.
2. **Durable evidence artefacts.** A derivative (an OCR transcription,
   an extraction, a translation) that Evidence Intelligence causes to be
   accepted into Evidence Custodian custody via the existing acceptance
   path, and registered in Memory Core with `extractedFrom`/`derivedFrom`
   set, becomes a first-class, separately-identified custodied artefact
   — governed identically to any other accepted evidence (Scope Lock §6's
   separate-identity requirement), never a distinguished "Evidence
   Intelligence artefact type." *Representative examples, once accepted:*
   an OCR transcription; extracted text; a translation.
3. **Memory Core facts.** An `Assertion` or `Relationship` (including a
   `CONTRADICTS`/`SUPPORTS` relationship between two Assertions) that
   Evidence Intelligence proposes and that is accepted through Memory
   Core's own existing governed write path becomes a durable Memory Core
   record — inspectable, provenance-linked, but carrying no promotion
   status and no constitutional claim to being "knowledge" in Programme
   3's sense. *Representative examples:* a candidate assertion; a
   candidate relationship expressing a document comparison outcome or a
   contradiction between two accounts; a timeline or chronology expressed
   as a sequence of time-ordered Relationships; an issue map or entity
   graph expressed as a set of Relationships between named entities and
   documents.
4. **Knowledge Candidates.** The only route by which anything Evidence
   Intelligence produces may become authoritative, promotable knowledge.
   Evidence Intelligence may submit a Knowledge Candidate referencing
   Memory Core evidence (including its own newly-written Assertions or
   Relationships), but — per Programme 3 Contract Design V2 §2 and §5,
   already frozen, not reopened here — it may never attach its own
   confidence or evidential-state value to that submission. Knowledge
   Memory alone computes both, exactly as it already must for every
   other submitter. *Representative example:* a candidate finding,
   submitted with reference to the Memory Core evidence (including any
   Assertions or Relationships Evidence Intelligence itself proposed)
   that supports it, and nothing else.

**"Analytical summaries," "analytical reports," "comparison outputs,"
"timelines," "chronologies," "issue maps," and "entity graphs" are
Derived Work Product** (CDR-006's own term, extended here by direct
application, not by new decision) — never evidence, always traceable to
the originals they rely upon through Memory Core's existing Provenance
mechanism, and never a substitute for the preserved originals they
describe. Which of these ever cross from category 1 into categories 2–4
is a matter of what Evidence Intelligence chooses to formalise into
Memory Core or submit to Knowledge Memory — this record classifies the
boundary; it does not decide which artefacts Evidence Intelligence will,
in practice, produce or promote.

**Ownership of an analytical artefact transfers on acceptance.** Evidence
Intelligence owns the production of any artefact described in this
section up to the point of its acceptance into a governed Parker
subsystem. Once accepted — into Evidence Custodian as a durable evidence
artefact, into Memory Core as an Assertion or Relationship, or into
Knowledge Memory as a promoted Knowledge Item — constitutional ownership
of that artefact belongs exclusively to the accepting subsystem, not to
Evidence Intelligence. This states explicitly what the acceptance paths
described throughout this section already imply; it does not alter any
subsystem's existing ownership.

**Analytical computation is not constitutional classification.** Evidence
Intelligence may perform intermediate analytical computations — scores,
similarity measures, or other numeric or qualitative outputs that assist
its own analysis — as part of producing the artefacts described above.
No such computation constitutes constitutional evidential classification.
Confidence computation for promotion, final evidential-state assignment,
knowledge promotion, and Knowledge Item lifecycle decisions remain
exclusively governed by Knowledge Memory, exactly as Sections 1, 2, and 5
already establish. Legal or factual truth authority belongs to no
subsystem. An intermediate analytical computation is, at most, transient
working output under category 1 above, unless and until it is itself
accepted as one of the governed artefacts this section describes.

## Decision

```
Evidence Intelligence — classified as a first-class, downstream,
analytical subsystem, peer to Evidence Custodian and Knowledge Memory,
bound in full by Epistemic Integrity Amendment No. 1 — Adopted.

Document Intelligence as an independently governed subsystem — Not
adopted; its previously-discussed capabilities are classified as
Evidence Intelligence's own.

A new, Evidence-Intelligence-specific epistemic-authorisation mechanism
— Not adopted; Knowledge Memory's existing promotion evaluation is
adopted as the Article XV "constitutional review" step in full.

A new Memory Core record type for Evidence-Intelligence-specific
findings — Not adopted; Assertion and Relationship, unmodified, are
adopted as sufficient.

A new Reasoning Provider abstraction specific to Evidence Intelligence
— Not adopted; the existing ReasoningProvider interface is adopted
unmodified, and Evidence Intelligence is classified as its orchestrator
where used, never as an instance of it.
```

## Decision Rules

- Evidence Intelligence is a first-class Parker infrastructure subsystem,
  organisationally a peer of Evidence Custodian and Knowledge Memory,
  never a component of either and never organisationally superior to
  either.
- Evidence Intelligence inherits Epistemic Integrity Amendment No. 1 in
  full, per Article XIX's own express naming, without exception or
  narrowing.
- Evidence Intelligence has no authority to assign final evidential
  status, confidence, or promotion outcome to anything it produces.
  Where such a determination is sought, it must be routed through
  Knowledge Memory's own, already-governed promotion evaluation.
- Evidence Intelligence must never discard, suppress, or silently omit
  contradictory evidence merely because a preferred interpretation
  exists. This restates Article III's and Article IV's existing
  prohibition on unjustified exclusivity and Programme 3's existing
  contradiction-preservation rule; it creates no new principle.
- Evidence Intelligence may employ one or more existing `ReasoningProvider`
  implementations as internal analytical mechanisms. It is not itself
  classified as a Reasoning Provider, and the `ReasoningProvider`
  abstraction itself is not broadened, extended, or reinterpreted to
  describe Evidence Intelligence.
- Evidence Intelligence's only path to original evidence is
  `EvidenceCustodian.retrieve`, governed exactly as it already is for
  every other consumer. No new read mechanism is authorised.
- Evidence Intelligence's only path to accepting a derivative artefact
  into custody is the existing governed acceptance path. No new
  acceptance mechanism, and no Evidence-Intelligence-exclusive custody
  privilege, is authorised.
- Evidence Intelligence's Memory Core writes use `Assertion` and
  `Relationship` unmodified. No new Memory Core record type is
  authorised by this record.
- Evidence Intelligence's only path to durable, authoritative knowledge
  is a Knowledge Candidate submission to Knowledge Memory's existing
  pipeline, carrying no self-declared confidence or evidential state.
- The capabilities previously discussed under "Document Intelligence"
  are classified as Evidence Intelligence's own analytical functions,
  including document ingestion, OCR, transcription, and extraction.
  `docs/architecture/21-document-intelligence.md` remains historical
  background only and carries no constitutional authority.
- Any comparison capability Evidence Intelligence builds must be
  reconciled with, not independently re-derived from, CDR-001/002/003's
  already-adopted canonical comparison model.
- No excluded responsibility named in Section 5 may be assumed by
  Evidence Intelligence without a new or amended constitutional decision.
- Ownership of an analytical artefact transfers, on acceptance, to the
  Parker subsystem that constitutionally governs it (Evidence Custodian,
  Memory Core, or Knowledge Memory); Evidence Intelligence retains no
  ownership of an artefact once accepted.
- An intermediate analytical computation Evidence Intelligence performs
  is not a constitutional evidential classification; confidence
  computation for promotion, final evidential-state assignment, and
  promotion decisions remain exclusively governed by Knowledge Memory,
  while legal or factual truth authority belongs to no subsystem.

## Constitutional Visibility

This record creates no new registry. Visibility is preserved
distributively, consistent with this Programme's existing practice: this
record itself becomes the single place Evidence Intelligence's
constitutional classification lives, discoverable independently by any
future reviewer; the future Evidence Intelligence Contract Design, once
written, becomes the next layer, built against this record exactly as
the Evidence Artifact Contract Design was built against CDR-006.

## Consequences

- **CDR-006:** unaffected. Its Model B, its custody/legal-ownership
  distinction, and its Contract-Design deferral are all fulfilled, not
  reopened, by this record.
- **Evidence Artifact Contract Design, Scope Lock, Implementation Plan,
  Phase 7 Boundary Clarification:** unaffected, unmodified, unreopened.
- **Programme 3 Knowledge Memory governance:** unaffected. Section 10's
  own "no Document Intelligence capability is designed, referenced, or
  assumed" statement remains true of Programme 3's own contracts; this
  record does not add one to them — it fixes what Evidence Intelligence
  itself may do when it later becomes a caller of Knowledge Memory's
  already-existing submission boundary.
- **The `ReasoningProvider` interface and its Contract Design:**
  unaffected, unmodified, unbroadened. This record adds an orchestrator
  of it; it does not add a new kind of it.
- **`docs/architecture/21-document-intelligence.md`:** unaffected as a
  document; it is not amended, deleted, or rewritten. Its constitutional
  standing is clarified, not changed — it was never a ratified decision,
  and this record confirms it remains historical background, while
  classifying the capabilities it once discussed as Evidence
  Intelligence's own.
- **Future Evidence Intelligence Contract Design:** becomes the next
  required governance stage. Explicitly deferred; not begun by this
  record.

## Non-Decisions

CDR-007 does not decide, and no future reader should treat it as having
decided:

- Any implementation interface, Kotlin type, storage mechanism, or API
  for Evidence Intelligence — all explicitly deferred to a future
  Contract Design.
- Whether Knowledge Memory's own write-path Runtime Integration becomes
  a prerequisite of, or a parallel track alongside, Evidence
  Intelligence's own eventual Runtime Integration — a sequencing
  question for the Implementation Plan stage, not a constitutional one.
- Whether `ResourceType` requires a new value for gating Knowledge
  Candidate submission, or whether `MEMORY` suffices — a Contract
  Design/technology-mapping question.
- The specific vocabulary-reconciliation mechanism between Article IV's
  fourteen `EvidentialState` values and `user-authorship-and-evidence.md`'s
  six communication-layer categories, beyond fixing (Section 1, above)
  that `EvidentialState` is the formal classification any Evidence
  Intelligence output is ultimately judged against once it reaches
  Knowledge Memory. The two vocabularies' relationship for
  communication-layer representation remains open, and is better
  resolved by that document's own amendment process than invented here.
- The specific document types, volumes, or comparison goals any real
  evidentiary case work requires. This record classifies constitutional
  capability, not product scope.
- Any change to CDR-001 through CDR-006, Article IX, or any Evidence
  Custodian governance document — all reviewed, none reopened.

## Verification Criteria

A future reviewer may confirm this decision has been followed by
checking that:

- Any future Evidence Intelligence Contract Design cites this record as
  its own constitutional basis, and does not re-argue Sections 1–6
  above.
- No future Evidence Intelligence implementation grants itself custody,
  modification, or deletion authority over original evidence.
- No future Evidence Intelligence implementation assigns a confidence or
  evidential-state value to a Knowledge Candidate it submits.
- No future Evidence Intelligence implementation discards, suppresses,
  or silently omits contradictory evidence because a preferred
  interpretation exists.
- No future Evidence Intelligence implementation is classified as, or
  merges its own identity with, a Reasoning Provider; the
  `ReasoningProvider` interface itself remains unmodified and
  unbroadened.
- No new Memory Core record type is introduced for Evidence-Intelligence-specific
  findings; `Assertion` and `Relationship` remain the record shapes used.
- No governance document revives "Document Intelligence" as an
  independently governed subsystem without a new constitutional decision
  superseding this one.
- Any Evidence Intelligence comparison capability's design cites
  CDR-001/002/003's canonical comparison model rather than treating
  comparison as previously unaddressed.

## Repository Reuse Summary

### Reused Without Modification

**Governance:**
- Parker Constitution — "Parker owns authority. Modules provide
  capability"; "cognition proposes, trust authorises, runtime executes";
  no-bypass principle.
- Epistemic Integrity Amendment No. 1 — Articles I, III, IV, VI, VII,
  VIII, IX, XIV, XV, XIX in particular, applied without amendment.
- CDR-006 — Model B, the custody/legal-ownership distinction, the
  Constitutional Optimisation Safeguard, "Derived Work Product is never
  evidence," and its own deferral of Evidence Intelligence's internal
  shape to this record.
- Evidence Artifact Contract Design §6.3 — the Custodian/Evidence
  Intelligence boundary, adopted verbatim, not re-derived.
- Evidence Custodian Scope Lock and Phase 7 Boundary Clarification —
  cited for precedent (owner-only structural separation, disclosed-but-unregistered
  Permission Engine admission convention) without modification.
- Programme 3 Knowledge Memory Contract Design V2 — the Knowledge
  Candidate submission boundary, the "no caller-declared confidence or
  evidential state" rule, the two-non-overlapping-evaluations permission
  pattern, its own contradiction-preservation rule, and the explicit,
  still-true "no Document Intelligence capability" statement in its own
  Section 10.
- `user-authorship-and-evidence.md` — its "deliberately silent on
  execution" discipline, extended by direct analogy.
- CDR-001/002/003 — the canonical comparison model, adopted as governing
  for any future Evidence Intelligence comparison capability.
- CDR-004 — cited as methodological precedent for minimal-reopening
  discipline, not for its own substantive subject matter.

**Contracts and abstractions:**
`EvidenceCustodian` (`retrieve`, `accept`), `Provenance`/`CandidateProvenance`
(including `derivedFrom`/`extractedFrom`), `Document`/`CandidateDocument`,
`Assertion`/`CandidateAssertion`, `Relationship`/`CandidateRelationship`
(including `SUPPORTS`/`CONTRADICTS`), `RelationshipEndpoint`,
`MemoryRetrieval`, `EvidentialState`, `KnowledgeCandidate`,
`ReasoningProvider` (as an unmodified, orchestrated abstraction, not an
identity Evidence Intelligence assumes), `PermissionEngine`/`DefaultPermissionPolicy`/
`ActionVocabulary`/`ResourceRegistry`.

**Constitutional decisions:**
CDR-001, CDR-002, CDR-003 (comparison model), CDR-006 (custody
classification, Derived Work Product, Optimisation Safeguard).

### Explicitly Not Adopted

- **A new evidence store.** Evidence Custodian remains the sole custody
  layer.
- **A new memory system.** Memory Core remains the sole general-purpose
  fact-registration substrate.
- **A new comparison model.** CDR-001/002/003's canonical comparison
  model is adopted; no parallel model is authorised.
- **A new provenance model.** `Provenance`'s existing
  `derivedFrom`/`extractedFrom` mechanism is adopted; no parallel
  traceability mechanism is authorised.
- **A new promotion system.** Knowledge Memory's existing Knowledge
  Candidate/promotion pipeline is adopted; no parallel epistemic-review
  mechanism is authorised.
- **Document Intelligence as an independently governed subsystem.** Its
  previously-discussed capabilities are classified as Evidence
  Intelligence's own; `21-document-intelligence.md` remains historical
  background, carrying no constitutional authority.
- **An Evidence-Intelligence-specific Reasoning Provider abstraction.**
  The existing `ReasoningProvider` interface is adopted unmodified;
  Evidence Intelligence is classified as its orchestrator, never as a
  new kind of it.

### Remaining Unresolved Questions

Explicitly not decided here, and left to Contract Design or a further
CDR if genuinely constitutional:

1. Whether Knowledge Memory's write-path Runtime Integration is a
   prerequisite for Evidence Intelligence's own Runtime Integration.
2. Whether a new `ResourceType` value is warranted, or `MEMORY` suffices.
3. How Article IV's `EvidentialState` and `user-authorship-and-evidence.md`'s
   six-category vocabulary relate at the communication-representation
   layer.
4. The concrete document types, volumes, and comparison goals of any
   specific real-world evidentiary case work this platform is intended
   to support — product scope, not constitutional classification.

## Architectural Observation

This decision intentionally introduces very little new constitutional
machinery. Its principal contribution is the constitutional
classification and orchestration of existing Parker capabilities —
Evidence Custodian's custody, Memory Core's registration, Knowledge
Memory's promotion, and the existing `ReasoningProvider` abstraction —
rather than the creation of new abstractions. No new evidence store, no
new memory system, no new comparison model, no new provenance model, no
new promotion system, and no new reasoning-provider category are
authorised anywhere in this record.

Parker's constitutional architecture has reached a level of stability at
which a new programme's primary work is integration against
already-established governance, not invention of new governance. Memory
Core, Knowledge Memory, and Evidence Custodian each settled a boundary
that Evidence Intelligence now simply reuses; the absence of new
abstractions here is an intentional architectural outcome of that prior
work, not an omission in this one. A programme that finds most of what
it needs already governed is the expected result of a constitution that
has been built, deliberately, to be extended rather than re-litigated.
Reuse, on this record's own terms, is evidence of constitutional
maturity — not a sign that this classification is incomplete.

## Final Report

**Document created:** `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`.

**Decision:** Evidence Intelligence classified as a first-class,
downstream, analytical subsystem bound in full by Epistemic Integrity
Amendment No. 1, with no authority over its own outputs' final
evidential status; the capabilities previously discussed under "Document
Intelligence" classified as Evidence Intelligence's own; no new Memory
Core record type, no new epistemic-authorisation mechanism, no new
Reasoning Provider abstraction, and no new custody or promotion path
introduced.

**Status:** Accepted. Canonical. Frozen. Independent Constitutional
Verification and Final Freeze Verification are both complete; this
record is now adopted as normative Parker governance.

CDR-007 — ACCEPTED, CANONICAL, AND FROZEN — ADOPTED AS NORMATIVE PARKER
GOVERNANCE

Confirmed: no production code modified; no tests modified; CDR-001
through CDR-006 not modified; the Evidence Custodian Contract Design,
Scope Lock, Implementation Plan, and Phase 7 Boundary Clarification not
modified; Programme 3 Knowledge Memory governance not modified; the
Reasoning Provider Contract Design and interface not modified;
`docs/architecture/21-document-intelligence.md` not modified; nothing
staged; nothing committed; nothing pushed; Evidence Intelligence Contract
Design not started.
