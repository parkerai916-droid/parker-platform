# Evidence Intelligence — Implementation Plan

## Status

Programme: **Evidence Intelligence — Implementation Plan.**
Phase: **Final design document before production coding begins.** No
Kotlin is implemented, proposed as a diff, or changed by this document.
No interface, method signature, API, schema, or storage technology is
defined. No implementation has begun. Neither `src/` nor `tests/` is
touched. Nothing is staged, committed, or pushed.

**Ratification status: Accepted. Canonical. Implementation
Authorised.** Independent Constitutional Review of this document is
complete. Acceptance is subject to the mandatory Unit 2 verification
gate (§8 Unit 2, the authoritative statement of that condition; §9,
§10, and §13 below reference it rather than restate it). This
document's own authority remains derivative, not original (§2, below).
Acceptance authorises Evidence Intelligence engineering work to begin,
proceeding unit by unit in the frozen order (§7); no unit may begin
before every unit it depends on has independently met its own
completion criteria (§8, §10).

**Normative inputs, frozen, not redefined:**
`docs/architecture/parker-constitution.md`;
`docs/architecture/epistemic-integrity.md` (Epistemic Integrity
Amendment No. 1); `docs/decisions/CDR-001` through `CDR-007`, in
particular `CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`;
`docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` ("the
Contract Design" — Accepted, Canonical); and
`docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` ("the Scope
Lock" — Accepted, Canonical, Implementation Frozen). This document
describes **how** the already-approved Evidence Intelligence
architecture will eventually be built — sequencing, dependencies,
verification, and completion conditions. It introduces no
responsibility, no exclusion, no subsystem boundary, no permission
rule, no ownership rule, and no dependency beyond what those documents
already froze. It selects no storage technology, no API shape, no
schema, and no Kotlin design. Where this plan states a unit name or
ordering choice, that choice is disclosed as a planning-level
convenience, never as a new architectural decision.

---

## 1. Purpose

This document decomposes the frozen Evidence Intelligence architecture
— the Contract Design's own responsibilities, inputs, outputs,
acceptance paths, and dependency model, and the Scope Lock's own
scope, exclusions, dependency freeze, ownership freeze, sequencing
freeze, stop conditions, invariants, and verification requirements —
into a staged, dependency-ordered set of implementation units.

**Implementation order only. No architectural authority.** This
document decides *in what order* already-authorised work is built and
*how* its completion is verified. It decides nothing about *what* is
authorised — every responsibility, exclusion, dependency, ownership
rule, and sequencing rule below is a restatement of the Contract
Design or the Scope Lock, cited to the specific section it comes from,
never a new determination this document makes on its own. Where
implementation reveals an ambiguity, a missing rule, or a genuine need
the Contract Design and Scope Lock do not already answer, this document
does not resolve it by inventing an answer — engineering work stops and
governance resumes (§4, §13, below).

---

## 2. Authority

This document's authority comes from, and only from:

- **The Parker Constitution** (`docs/architecture/parker-constitution.md`)
  — the platform's own highest-level, immutable values and
  architectural constraints, including "cognition proposes, trust
  authorises, runtime executes" and the no-bypass principle every
  boundary below assumes.
- **The Evidence Intelligence Contract Design**
  (`docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`) —
  Accepted, Canonical; the source of every responsibility,
  non-responsibility, public type, input, output, acceptance path,
  comparison-model relationship, provenance model, confidence model,
  interface, failure model, and dependency this plan sequences.
- **The Evidence Intelligence Scope Lock**
  (`docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`) —
  Accepted, Canonical, Implementation Frozen; the source of every
  frozen scope boundary, exclusion, dependency freeze, ownership
  freeze, sequencing freeze, stop condition, implementation invariant,
  and verification requirement this plan sequences, including the two
  completed Scope Lock decisions (the Evidence Intelligence acceptance
  coordinator, §6 of the Scope Lock; the invocation-gating Permission
  Engine decision, §6, §7, §11 of the Scope Lock).

**This document derives authority from these three. It cannot modify
any of them.** It does not reopen, narrow, widen, or reinterpret any
decision the Constitution, the Contract Design, or the Scope Lock
already made. CDR-007 and CDR-001 through CDR-006 are not a fourth,
independent authority this document draws on directly — they are
already incorporated into the Contract Design and the Scope Lock, which
this document treats as its own sole, sufficient, already-settled
inputs. Where this document is silent on a question, that silence means
the question was already answered by one of the three documents above,
not that this document has discretion to answer it.

---

## 3. Implementation Objectives

Restated from the Contract Design and the Scope Lock, exactly as they
already state them — no objective below is new:

- **Governed, reference-only input.** Every analysis operates only on
  an `EvidenceArtifactId` or Memory-Core-addressable reference already
  resolvable through `EvidenceCustodian.retrieve` or `MemoryRetrieval`
  — never on raw, uncustodied evidence supplied directly (Contract
  Design §4; Scope Lock §2).
- **Bounded, non-authoritative output.** Every analytical artefact
  produced is one of exactly four permitted categories — transient
  output, candidate derivative artefact, candidate Memory Core record,
  or Knowledge Candidate — and none asserts truth, evidential state, or
  promotion on its own authority (Contract Design §5; Scope Lock §2).
- **Claim-level traceability.** Every material analytical claim within
  a transient output is traceable to the specific governed evidence
  reference(s) supporting it, not merely to one reference list for the
  output as a whole (Contract Design §5, §8; Scope Lock §2, §9).
- **Content-type honesty.** Extracted content, observed content,
  inferred analytical conclusions, and model-generated explanatory
  language remain distinguishable in any analytical output (Contract
  Design §5; Scope Lock §8, §9).
- **Transcription-fidelity honesty.** Verbatim transcription,
  normalised transcription, and inferred reconstruction are never
  silently conflated where a candidate derivative artefact results from
  transcription (Contract Design §5; Scope Lock §8, §9).
- **Contradiction preservation.** Contradictory evidence encountered
  during analysis is always disclosed, never silently resolved in
  favour of one account (Contract Design §1; Scope Lock §8, §9).
- **Analytical confidence isolation.** Analytical confidence
  computations remain transient only; no durable confidence or
  evidential-state field is ever populated (Contract Design §2, §9;
  Scope Lock §8).
- **Ownership-transfer discipline.** Evidence Intelligence produces
  candidates only; ownership of any accepted artefact or record
  transfers exclusively to the accepting subsystem, never retained,
  never duplicated (Contract Design §5; Scope Lock §5).
- **Sequenced Knowledge Candidate construction.** A Knowledge Candidate
  referencing a newly proposed Memory Core record is constructed only
  after that record has been accepted and assigned a governed
  identifier (Contract Design §5, §6; Scope Lock §6).
- **Partial-completion honesty.** An analysis that succeeds for some
  referenced inputs and fails for others represents the successful
  portion as genuine output and signals the rest by disclosed failure
  means — never a fabricated result, never a silent omission (Contract
  Design §11; Scope Lock §2, §8, §9).
- **Governed acceptance orchestration.** Every produced candidate
  reaches its accepting subsystem only through that subsystem's own
  existing, unmodified acceptance interface, invoked only by the
  Evidence Intelligence acceptance coordinator — never by Evidence
  Intelligence itself (Contract Design §6; Scope Lock §6, §7).
- **Invocation-level governance.** Every invocation of Evidence
  Intelligence's own operation requires Permission Engine evaluation of
  a dedicated proposal class before analysis begins, separate from and
  additional to the existing read and acceptance gates (Scope Lock §6,
  §7, §11).

No objective above authorises redesign of any cited section; each is a
restatement, not a reinterpretation.

---

## 4. Implementation Principles

- **Smallest verifiable increment.** Each unit in §8 is scoped to the
  smallest slice of behaviour that can be independently verified
  against a specific Contract Design or Scope Lock obligation — no unit
  bundles unrelated behaviour merely because it is convenient to build
  together.
- **Reuse existing Parker components.** Every dependency this plan
  sequences already exists (§5, below); no unit introduces a
  replacement for, or parallel to, an existing Evidence Custodian,
  Memory Core, Knowledge Memory, Reasoning Provider, or Permission
  Engine mechanism.
- **Preserve ownership boundaries.** No unit grants Evidence
  Intelligence, or the acceptance coordinator, a residual claim,
  reference-holding privilege, or modification right over anything
  after acceptance (Scope Lock §5).
- **Constitutional correctness before functionality.** A unit that
  produces plausible-looking output through a code path that violates a
  frozen boundary — a dependency the Scope Lock excludes, an ownership
  rule it fixes, a sequencing rule it requires — is not complete,
  regardless of how well it appears to work.
- **Verification before progression.** No unit is treated as a
  dependency for a later unit until its own verification goals (§8) are
  demonstrated, not merely asserted.
- **No implementation shortcuts.** A convenience that would require
  bypassing Permission Engine evaluation, collapsing the accept-before-
  Knowledge-Candidate ordering, or granting Evidence Intelligence or the
  coordinator a dependency this plan does not list, is never taken —
  regardless of engineering convenience.
- **Burden of proof favours exclusion.** Where it is unclear whether a
  candidate behaviour is authorised, the Scope Lock's own governing
  principle applies unchanged: absence of explicit authorisation means
  exclusion, not permission.

---

## 5. Existing Components Reused

Every dependency below already exists in this repository's governed
contracts, exactly as the Contract Design (§12, §13) and the Scope Lock
(§4) already fix. **No additional dependency may be introduced by this
plan or by the implementation it sequences.**

**Depended upon directly by Evidence Intelligence:**

- `EvidenceCustodian.retrieve` — read-only access to custodied
  originals and derivatives (Evidence Artifact Contract Design).
- `MemoryRetrieval` — read-only access to registered Memory Core
  records (Memory Core Contract Design).
- `ReasoningProvider` (zero or more) — internal analytical mechanism,
  orchestrated, never itself (Reasoning Provider Contract Design).

**Existing public types reused, unmodified, by Evidence Intelligence's
own output:**

- `EvidenceArtifactId`, `CandidateEvidenceArtifact` (Evidence Custodian).
- `Assertion`, `CandidateAssertion`, `Relationship`,
  `CandidateRelationship` — including the `SUPPORTS`/`CONTRADICTS`
  relationship kinds (Memory Core).
- `Provenance`, `CandidateProvenance` — including the
  `derivedFrom`/`extractedFrom` mechanism (Memory Core).
- `Document`, `CandidateDocument` (Memory Core).
- `KnowledgeCandidate` (Programme 3 Knowledge Memory) — produced, never
  submitted, by Evidence Intelligence itself.
- `KnowledgeItem`, `EvidentialState` (Knowledge Memory) — referenced
  only in the sense that Evidence Intelligence's output may eventually
  be judged against them by Knowledge Memory; never constructed, held,
  or referenced directly by Evidence Intelligence's own model.
- `ReasoningProviderRequest`, `ReasoningProviderResponse`,
  `ReasoningContext`, `ReasoningSubject` (Reasoning Provider Contract
  Design, Amendment 1) — Evidence Intelligence's own `EvidenceAnalysisRequest`
  (§5, above) is the payload carried by `ReasoningSubject.OfEvidenceAnalysisRequest`,
  reused unmodified; `ReasoningSubject` itself remains owned by the
  Reasoning Provider Contract Design, never by Evidence Intelligence.
- `PrincipalId` (existing platform identifier), for audit purposes
  only.
- CDR-001/002/003's canonical comparison model (Model B) — reused by
  reconciliation requirement only, never re-derived (Contract Design
  §7; Scope Lock §11).
- The Permission Engine's existing gating mechanism at every boundary
  Evidence Intelligence or the acceptance coordinator calls.
- `PermissionAction.EXECUTE` and `ResourceType.DOCUMENT` — both
  pre-existing enum values (`src/contracts/Permission.kt`,
  `src/contracts/Resource.kt`); no enum value is invented anywhere in
  this plan. Their pairing for the "invoke Evidence Intelligence"
  domain act is a new, disclosed proposal class (Unit 6, below) — not a
  new enum value, and not a reinterpretation of either existing value's
  own meaning elsewhere in this repository (§6, §8, §11, §12, below).
- `EvidenceRetrievalResult` (Evidence Custodian's own existing,
  per-artefact disposition — `Found`/`NotFound`/`Rejected`) — the
  already-existing mechanism this plan reuses, not invents, to disclose
  which individually-referenced inputs could not be retrieved within
  one invocation (§8 Unit 4/5, §9, below).

**Depended upon only by the Evidence Intelligence acceptance
coordinator, never directly by Evidence Intelligence:**

- `EvidenceCustodian.accept`.
- `MemoryCore`'s public write interface.
- Knowledge Memory's Knowledge Submission interface.
- Each of the three acceptance interfaces' own existing
  success/rejection disposition shape — reused, not invented, as the
  coordinator's own outcome representation (§8 Unit 7, below).

**Cited as structural precedent only, not a dependency of any kind:**

- `EvidenceRegistrationCoordinator` (`src/runtime/EvidenceRegistrationCoordinator.kt`)
  — a concrete, non-interface-backed class, the existing shape the
  Scope Lock (§6) cites for "sequencing two subsystems without either
  calling the other," offered as precedent for the acceptance
  coordinator's own shape (§8 Unit 7, below), not as something the
  coordinator depends upon or extends.
- `ConversationTurnReasoningCoordinator` and
  `CommunicationConversationCoordinator` — cited only for their shared,
  already-established "concrete, non-interface-backed sequencing
  mechanism" precedent (§8 Unit 7's own justification, below); neither
  is a dependency of any kind.

**Do not invent additional dependencies.** This list is exhaustive. A
future implementer who discovers a genuine need beyond it does not
resolve that need by adding a dependency during coding; the need
returns to governance (a Contract Design amendment or a new
Constitutional Decision Record, as it warrants), exactly as the Scope
Lock (§4) already requires.

---

## 6. New Components

Described architecturally, by responsibility only. **No Kotlin name,
method signature, package, or source-file is defined for any of the
six below.** Item 5's own implementation *shape* — concrete or
interface-backed, its exact dependencies, its input, its outcome
representation, and its ordering guarantee — is frozen in full in §8
Unit 7, completing a decision the Scope Lock (§6, §10) explicitly left
to this plan; its literal Kotlin name and internal algorithm remain out
of scope (§11, below) regardless.

1. **The Evidence Intelligence operation.** A single, public,
   request-in/result-list-out operation. Responsibility: given a
   request naming governed evidence and/or Memory Core references and
   an analysis classification, perform analytical processing —
   optionally orchestrating one or more Reasoning Providers internally
   — and return zero, one, or many analytical results. Holds no
   persistence responsibility, no acceptance responsibility, and no
   Permission Engine reference of its own (Contract Design §10; Scope
   Lock §4, §6).
2. **The analysis-request shape.** Responsibility: bundle a list of
   custodied-evidence references, a list of Memory-Core-addressable
   references, an optional assembled reasoning context, an open
   analysis classification, and a requesting principal, into one
   addressable input — carrying no caller-declared confidence or
   evidential-state value of any kind (Contract Design §4).
3. **The analysis-result shape.** Responsibility: represent exactly one
   of four permitted output categories per value — transient output,
   candidate derivative artefact, candidate Memory Core record, or
   Knowledge Candidate — never a fifth category, never a caller-visible
   confidence or evidential-state field (Contract Design §3, §5).
4. **The payload selector for "Candidate record produced."** A closed,
   two-case value shape, carried solely as that category's own payload
   (Contract Design §3, as amended; Scope Lock §4, as amended).
   Responsibility: select exactly one of an existing, unmodified
   `CandidateAssertion` or an existing, unmodified `CandidateRelationship`
   — never a third case, and no data beyond the one selected candidate.
   Behaviour-free; grants no acceptance, persistence, retrieval,
   reasoning, confidence, evidential-state, provenance, or ownership
   authority of its own; does not modify either candidate type it
   selects; is not a generic union abstraction reusable for any other
   pair of types; creates no independent dependency entitlement for any
   other subsystem (Scope Lock §4, §5, as amended).
5. **The Evidence Intelligence acceptance coordinator.** A separate,
   composition-level mechanism, structurally outside Evidence
   Intelligence — **concrete, non-interface-backed** (justified in full
   in §8 Unit 7). Responsibility: consume the result list the Evidence
   Intelligence operation returns, and sequence calls to the three
   existing acceptance interfaces (Evidence Custodian's, Memory Core's,
   Knowledge Memory's) in the order the Scope Lock (§6) fixes, ensuring
   a Memory Core record is accepted before any Knowledge Candidate
   referencing it is submitted. It **submits** a Knowledge Candidate;
   it never **constructs** one — construction remains Evidence
   Intelligence's own responsibility (Contract Design §1 "Producing
   Knowledge Candidates"; §8 Unit 5, Unit 7, below, resolves this
   precisely). Holds no custody, truth, promotion, deletion, or
   evidential-state authority; cannot bypass Permission Engine
   evaluation; invokes no interface beyond the three named; retains no
   candidate after dispatch (Scope Lock §6, in full; §8 Unit 7, below).
6. **The invocation-gating decision point.** Not a class or a
   component in its own right — a governance requirement that whatever
   composes Evidence Intelligence into the running system evaluates a
   dedicated Permission Engine proposal class, `PermissionAction.EXECUTE`
   on `ResourceType.DOCUMENT` (§5, above; §8 Unit 6, below), before
   calling the Evidence Intelligence operation (item 1, above), and
   does not proceed to input resolution, analysis, or Reasoning
   Provider invocation on denial. Responsibility: recognise "invoke
   Evidence Intelligence" as its own permission-relevant domain act,
   held and evaluated by the composing caller, never by Evidence
   Intelligence's own code (Scope Lock §6, step 0; §7; §11).

No seventh new component is introduced anywhere in this plan. Item 4
(immediately above) is the one new public type this plan's own
correction adds — already authorised by name and frozen property list
by the amended Contract Design (§3) and Scope Lock (§4, §5), not newly
decided by this plan. Neither item 5 nor item 6 is a new public
platform subsystem: item 5 is composition-level wiring between three
already-governed interfaces (mirroring `EvidenceRegistrationCoordinator`'s
own precedent, §5, above), and item 6 is a new *pairing* of two
pre-existing enum values, never a new enum value, new interface, or new
authority.

---

## 7. Implementation Sequence

Eight units, in dependency order. No unit may begin before every unit
it depends on has met its own completion criteria (§8). This ordering
is a planning-level convenience, not a new architectural decision — no
unit below authorises anything the Contract Design or Scope Lock does
not already authorise, and no unit may be skipped, merged in a way that
hides a verification gap, or reordered so that a later unit's guarantee
is assumed rather than demonstrated.

1. **Input/output shape foundation** — establishes the analysis-request
   shape, the analysis-result shape, and the payload selector for
   "Candidate record produced" (§6, items 2–4). Depends on nothing
   below this plan; depends only on the existing types §5 lists.
2. **Governed input resolution** — establishes read-only resolution of
   custodied-evidence and Memory-Core references. Depends on Unit 1.
3. **Reasoning Provider orchestration** — establishes optional, internal
   invocation of existing Reasoning Providers. Depends on Unit 1;
   independent of Unit 2 (either may be built first without the other).
4. **Analytical output discipline** — establishes claim-level
   traceability, the four-way content-type distinction, the three-way
   transcription-fidelity distinction, contradiction preservation, and
   transient-only confidence. Depends on Units 1–3, since it governs
   what those units' own output must honestly represent.
5. **The Evidence Intelligence operation** — assembles Units 1–4 behind
   the single public operation (§6, item 1), including legitimate
   partial completion across many referenced inputs. Depends on Units
   1–4. **May be exercised in isolation (direct invocation, for testing
   only) once complete, but may not be registered, exposed, or
   reachable in production composition (Unit 8) until Unit 6 is also
   complete** (§8 Unit 5, Unit 6, below).
6. **Invocation permission gating** — establishes the dedicated
   Permission Engine proposal class evaluation preceding every call to
   Unit 5's own operation (§6, item 6). Depends on Unit 5 existing to be
   gated; otherwise independent of Units 1–4. **Production reachability
   of Unit 5 is gated on this unit's own completion** (immediately
   above; §8 Unit 6, below).
7. **The Evidence Intelligence acceptance coordinator** — establishes
   the composition-level sequencing mechanism (§6, item 5) that
   consumes Unit 5's own output. Depends on Unit 5's result shape
   (Unit 1); does not depend on Unit 6.
8. **Runtime composition and full verification** — wires Units 1–7 into
   Parker's own composition root exactly as an already-authorised,
   already-gated subsystem, and confirms every objective in §3
   behaviourally, per §9, before the Programme is considered complete.
   Depends on all seven units above, **and specifically may not
   register Unit 5 for production reachability without Unit 6 already
   composed alongside it.**

---

## 8. Unit Breakdown

Each unit below is independently implementable and independently
testable. No artificial phase boundary separates units that could be
verified together; no unit bundles unrelated behaviour to make the
sequence appear shorter than it is.

### Unit 1 — Input/Output Shape Foundation

- **Purpose.** Establish the three new, already-authorised public
  shapes — the analysis request, the analysis result, and the closed,
  two-case payload selector for the result's "Candidate record
  produced" category — as the sole new public surface besides the
  operation itself (Contract Design §3, as amended; Scope Lock §4, as
  amended).
- **Responsibilities.** Bundle existing-type references only (no
  content copies); represent exactly four output categories, one per
  value; structurally exclude any caller-declared confidence or
  evidential-state field on any of the three shapes. **The payload
  selector.** Closed to exactly two cases — an existing, unmodified
  `CandidateAssertion`, or an existing, unmodified `CandidateRelationship`
  — never a third; owns no data beyond the one selected candidate
  value; used only as the payload of the "Candidate record produced"
  category, never elsewhere; grants no acceptance, persistence,
  retrieval, reasoning, confidence, evidential-state, provenance, or
  ownership authority of its own; does not modify either candidate type
  it selects; is not a generic union abstraction reusable for any other
  pair of types; creates no independent dependency entitlement for any
  other subsystem (Contract Design §3, as amended; Scope Lock §4, §5,
  as amended). **Claim-level traceability, without a separate claim
  type (resolving the earlier `AnalyticalClaim` misstep).** No separate
  public claim type is introduced. Each material analytical claim is
  represented as its own `TransientOutput` value, carrying one
  non-blank prose string and one or more governed references, using
  only the existing `EvidenceArtifactId` and/or `RelationshipEndpoint`
  types (Contract Design §5's own claim-level traceability
  requirement). A single invocation may return multiple `TransientOutput`
  values in one `EvidenceAnalysisResult` list to represent multiple,
  independently traceable claims — never one `TransientOutput`
  internally bundling several claims, and never a claim carrying no
  governed reference at all.
- **Dependencies.** `EvidenceArtifactId`, Memory-Core-addressable
  reference shapes, `ReasoningContext`, `PrincipalId`,
  `CandidateEvidenceArtifact`, `CandidateAssertion`,
  `CandidateRelationship`, `KnowledgeCandidate` (§5, above) — read-only
  reuse only.
- **Verification goals.** No field on any of the three shapes is
  reachable that could carry a confidence or evidential-state value;
  the result shape cannot represent a fifth output category; the
  payload selector cannot represent a third case beyond
  `CandidateAssertion` and `CandidateRelationship`; no public type named
  `AnalyticalClaim`, or any other dedicated claim type, exists anywhere
  in the compiled repository; every `TransientOutput` value carries at
  least one governed reference; the payload selector is not, and cannot
  be used as, a general-purpose union mechanism for any pair of types
  other than the two it is closed to.
- **Completion criteria.** All three shapes exist, carry only
  existing-type references, and no code path anywhere can construct a
  caller-declared confidence or evidential-state value on any of them.
  Exactly four public runtime types exist across the full programme
  once all units are complete (`EvidenceAnalysisRequest`,
  `EvidenceAnalysisResult`, and the payload selector — all three from
  this Unit — plus `EvidenceIntelligence`, defined by Unit 5) — never a
  fifth. `EvidenceAnalysisResult` remains exactly four categories. The
  payload selector remains exactly two cases. No separate
  `AnalyticalClaim` public type, or any other new sealed or closed
  shape competing with, expanding, or standing beside
  `EvidenceAnalysisResult`'s own four-category taxonomy, exists
  anywhere in the compiled repository.

### Unit 2 — Governed Input Resolution

- **Purpose.** Resolve every referenced evidence artefact and Memory
  Core record through the two existing, already-gated read boundaries
  only, and disclose per-input retrieval failure using the mechanism
  those boundaries already provide for it.
- **Responsibilities.** Resolve `EvidenceArtifactId` values via
  `EvidenceCustodian.retrieve`; resolve Memory-Core-addressable
  references via `MemoryRetrieval`; accept an empty evidence-artefact
  list for an analysis operating only on Memory Core records; never
  accept raw, uncustodied evidence bytes. **Partial-completion
  disclosure (resolving the correction task's item 3).** Each
  `EvidenceCustodian.retrieve` call already returns its own existing,
  per-artefact `EvidenceRetrievalResult` disposition
  (`Found`/`NotFound`/`Rejected` — an Evidence Custodian contract this
  plan reuses, not invents); Evidence Intelligence does not fabricate,
  retry silently, or paper over a `NotFound`/`Rejected` disposition for
  one referenced artefact while others in the same request resolve as
  `Found` (Contract Design §11's own "surfaced by
  `EvidenceCustodian.retrieve`'s own existing result shape" language).
  A per-input disposition of this kind, together with the existing
  composition-level observability/logging mechanism this repository
  already uses for every other recoverable failure (mirroring
  `RuntimeEventLogger`'s own precedent of observing already-published
  structural facts, never conversation content), is the **already-
  authorised existing mechanism** that discloses which referenced
  inputs could not be retrieved within one invocation — never a
  wrapper around `EvidenceAnalysisResult`, never a fifth result
  variant, never a silent omission, and never an empty list standing
  in for the disclosure. No governance blocker is identified for this
  question: the mechanism the Contract Design itself already names
  (§11) is sufficient, and this Unit reuses it rather than inventing a
  side channel.
- **Mandatory implementation verification gate (Independent
  Constitutional Review condition).** The governance-level conclusion
  above — that `EvidenceRetrievalResult` is sufficient — is accepted,
  not final until independently confirmed during Unit 2's own
  implementation. Before Unit 2 is treated as complete, implementation
  must positively verify that `EvidenceRetrievalResult`, as it actually
  exists in `EvidenceCustodian`'s own contract, allows every failed,
  missing, or rejected input in a multi-input analysis to remain
  individually identifiable and externally disclosable alongside every
  successful analytical result produced in the same invocation. If
  verification confirms this, Unit 2 proceeds exactly as designed
  above. If verification finds the existing shape insufficient,
  implementation must stop at Unit 2 and return to governance — it may
  not resolve the gap by: extending `EvidenceRetrievalResult` ad hoc;
  introducing a wrapper type; adding a fifth `EvidenceAnalysisResult`
  variant; disclosing the failure through logging alone while leaving
  structured results silent on it; silently omitting the failed input;
  or inventing a new side channel. This is a verification gate on an
  already-frozen design choice, not licence to redesign it.
- **Dependencies.** Unit 1; `EvidenceCustodian.retrieve`;
  `MemoryRetrieval`; `EvidenceRetrievalResult` (reused, unmodified).
- **Verification goals.** No code path resolves an evidence reference
  by any means other than the two named boundaries; no code path
  accepts raw evidence content as if it were an already-custodied
  reference; no code path converts a `NotFound`/`Rejected` disposition
  for one input into a fabricated result, or discards it without a
  corresponding, disclosed observation.
- **Completion criteria.** Every referenced input in a request is
  resolved exclusively through `EvidenceCustodian.retrieve` or
  `MemoryRetrieval`, a request naming no retrievable evidence and no
  resolvable Memory Core reference is rejected before any further step
  (Contract Design §11), every per-input retrieval disposition —
  successful or not — is observably disclosed through the existing
  mechanisms named above, **and** the mandatory verification gate above
  has run and passed. Absent that gate having run and passed, Unit 2 is
  not complete, regardless of the other criteria.

### Unit 3 — Reasoning Provider Orchestration

- **Purpose.** Establish Evidence Intelligence as an orchestrating
  caller of zero or more existing Reasoning Providers, never a
  Reasoning Provider itself.
- **Responsibilities.** Invoke an existing `ReasoningProvider` by
  constructing a `ReasoningProviderRequest` whose `subject` is
  `ReasoningSubject.OfEvidenceAnalysisRequest`, wrapping this Programme's
  own, unmodified `EvidenceAnalysisRequest` (Reasoning Provider Contract
  Design, Amendment 1); treat each invocation as a pure, stateless callee
  with no obligations beyond what the Reasoning Provider Contract Design
  already defines. The top-level `ReasoningProviderRequest.reasoningContext`
  — not `EvidenceAnalysisRequest.reasoningContext` — is the sole context
  each invocation supplies to `ReasoningProvider.reason`, per that
  document's own frozen invariant; `EvidenceAnalysisRequest.reasoningContext`
  retains its existing meaning and ownership within Evidence
  Intelligence's own analysis (Evidence Intelligence Contract Design
  §4), unaffected by this.
- **Dependencies.** Unit 1; `ReasoningProvider`,
  `ReasoningProviderRequest`, `ReasoningProviderResponse`,
  `ReasoningSubject`.
- **Verification goals.** No class fulfilling the Evidence Intelligence
  operation itself implements `ReasoningProvider`; no `ReasoningProvider`
  implementation retains a reference back to Evidence Intelligence.
- **Completion criteria.** Zero or more `ReasoningProvider` invocations
  can be orchestrated per analysis, each independently verifiable
  against the Reasoning Provider Contract Design's own guarantees, with
  no identity conflation in either direction.

### Unit 4 — Analytical Output Discipline

- **Purpose.** Ensure every analytical output Units 2–3 make possible
  is represented honestly, at claim level, without blurring distinct
  evidential kinds of content.
- **Responsibilities.** Attach, to each material analytical claim
  within a transient output, the specific governed reference(s)
  supporting it; distinguish extracted content, observed content,
  inferred analytical conclusions, and model-generated explanatory
  language within any output; distinguish verbatim transcription,
  normalised transcription, and inferred reconstruction wherever a
  candidate derivative artefact results from transcription; disclose,
  never suppress, any contradiction encountered; confine any analytical
  confidence computation to transient output only.
- **Dependencies.** Units 1–3.
- **Verification goals.** A transient output with more than one
  material claim never carries only one undifferentiated reference
  list; content-type and transcription-fidelity distinctions are
  discoverable, not merely asserted in prose; no durable confidence or
  evidential-state field is ever populated; a genuine contradiction
  between two Memory Core Assertions is never represented as only a
  `SUPPORTS`-typed output for one side.
- **Completion criteria.** Every objective in §3 concerning
  traceability, content-type honesty, transcription-fidelity honesty,
  contradiction preservation, and confidence isolation is demonstrated,
  not asserted.

### Unit 5 — The Evidence Intelligence Operation

- **Purpose.** Assemble Units 1–4 behind the one public operation the
  Contract Design (§10) authorises.
- **Responsibilities.** Accept one analysis request; resolve inputs
  (Unit 2); perform analysis, optionally via Reasoning Providers (Unit
  3); apply output discipline (Unit 4); return a list of results — zero,
  one, or many. **Partial completion** is represented by returning a
  genuine, non-empty result list for every input that resolved and
  analysed successfully, while any input that failed resolution is
  disclosed exclusively through Unit 2's own already-authorised
  mechanism (`EvidenceRetrievalResult`, plus existing observability) —
  never by a wrapper, a fifth result variant, or an empty list standing
  in for a genuine failure. The operation's own responsibility ends at
  returning that list; it does not accept anything into any downstream
  subsystem, and it evaluates no permission of its own. **Knowledge
  Candidate construction (resolving the correction task's item 4).**
  Constructing a `KnowledgeCandidate` that references a Memory Core
  record remains this operation's own, sole responsibility (Contract
  Design §1) in every case, including a record only just accepted: the
  operation is invoked a **second time**, after acceptance, with that
  now-governed identifier resolvable through `MemoryRetrieval` (Unit
  2) as an ordinary Memory-Core-addressable reference, and this second,
  ordinary invocation's own analysis may then produce a "Candidate
  knowledge produced" result referencing it — never within the same
  result list that first proposed the underlying record (Contract
  Design §5's own prohibition on that combination structurally rules
  out any other mechanism: the "Candidate knowledge produced" variant
  requires an already-accepted reference at the moment this operation
  produces it, which a not-yet-accepted record cannot supply within one
  invocation). Whether and when to perform that second invocation is an
  ordinary scheduling decision for whatever composes Evidence
  Intelligence into the running system — the same composing caller
  already responsible for Unit 6's own gate — gated by the identical
  dedicated proposal class (Unit 6) as any other invocation; no new
  mechanism, authority, or component is introduced for it.
- **Dependencies.** Units 1–4.
- **Verification goals.** No code path within the operation invokes any
  acceptance interface; an empty result list is never used to signal a
  genuine analytical failure; a partial-completion case is never
  silently collapsed into either total success or total failure; no
  "Candidate knowledge produced" result ever references a Memory Core
  record that was proposed, not yet accepted, within the same
  invocation's own result list.
- **Completion criteria.** The operation is reachable exactly once per
  request, returns a result list satisfying every rule in Units 1–4,
  and holds no dependency reachable, at any depth, to any of the eight
  named items the Scope Lock (§4, §9) excludes. **Production
  reachability constraint (resolving the correction task's item 5).**
  This Unit may be exercised in isolation — direct invocation against a
  test-constructed request, bypassing Unit 6 — for verification
  purposes only; it may not be registered, exposed, or otherwise made
  reachable within Parker's own production composition (Unit 8) until
  Unit 6 has also met its own completion criteria. A composition that
  wires this Unit into the running system without Unit 6 already
  present is not complete, regardless of how correctly this Unit itself
  behaves in isolation.

### Unit 6 — Invocation Permission Gating

- **Purpose.** Complete the Scope Lock's own frozen requirement that
  invoking the Evidence Intelligence operation is itself a distinct,
  permission-relevant domain act (Scope Lock §6, step 0; §7; §11), by
  freezing the exact, existing-compatible proposal-class names this
  correction task requires.
- **Frozen proposal class (resolving the correction task's item 1):
  `PermissionAction.EXECUTE` on `ResourceType.DOCUMENT`.** Both values
  are pre-existing members of `src/contracts/Permission.kt`'s and
  `src/contracts/Resource.kt`'s own, already-frozen enums — neither is
  invented here. Only their **pairing**, as a new, disclosed proposal
  class for this one domain act, is new, exactly as the Scope Lock
  (§7, §11) anticipated Implementation Plan work would supply.
  Justification, self-certified against the same "minimum required,
  narrow in what it grants" discipline this repository's own
  composition root already applies elsewhere:
  - **`EXECUTE`, not `READ`.** Invoking Evidence Intelligence runs a
    bounded analytical operation, not a data read — the same
    distinction this repository's own composition root already draws
    for "start agent run." `READ` would understate what invocation
    triggers.
  - **`DOCUMENT`, not `AGENT`.** `AGENT` denotes the Agent Runtime's own
    execution boundary specifically; reusing it here would conflate
    Evidence Intelligence with a constitutionally distinct subsystem
    (CDR-007 §1–§2). `DOCUMENT` is the same `ResourceType` this
    repository's own composition root already uses for Evidence
    Custodian's "Intake" and "Retrieval" boundaries — including
    `EvidenceCustodian.retrieve` itself (a direct dependency, §5,
    above) — making it the closest existing classification that avoids
    that conflation.
  - **No governance blocker.** Both values already exist, in
    combination they do not distort either value's own established
    meaning elsewhere in this repository, and the pairing itself is
    new only in the sense every prior disclosed proposal class
    (`NOTIFY`/`TOOL`, `EXECUTE`/`AGENT`, `WRITE`/`DOCUMENT`,
    `READ`/`DOCUMENT`, `WRITE`/`MEMORY`, `DELETE`/`DOCUMENT`) was once
    new. This resolution is disclosed for review, not asserted as
    beyond question.
  - **Not decided here:** the specific `PermissionLevel` (`AUTOMATIC`,
    `CONFIRMATION_REQUIRED`, or another) this proposal class resolves
    to, and the literal string values for its own boundary Resource
    identifier and action-vocabulary verb phrase, remain ordinary
    composition-root policy content, decided the same way every other
    disclosed pairing's own policy content already was — a coding-time
    decision, not a constitutional one, and not required to be fixed by
    this plan.
- **Responsibilities.** Evaluate this dedicated proposal class before
  the Evidence Intelligence operation (Unit 5) is called; stop before
  any evidence retrieval, Memory Core read, or Reasoning Provider
  invocation on denial. This evaluation is performed by whatever
  composes Evidence Intelligence into the running system — never by a
  Permission Engine reference the operation holds itself.
- **Dependencies.** Unit 5 (as the thing being gated); the Permission
  Engine's own existing evaluation mechanism; `PermissionAction.EXECUTE`,
  `ResourceType.DOCUMENT` (both pre-existing, §5 above).
- **Verification goals.** No call to the Evidence Intelligence
  operation occurs anywhere in the system without a preceding
  evaluation of this exact proposal class; no class implementing the
  operation holds a Permission Engine reference of its own beyond what
  the two existing read boundaries already provide.
- **Completion criteria.** Denial of the `EXECUTE`/`DOCUMENT` proposal
  class demonstrably prevents evidence retrieval, Memory Core reads,
  and Reasoning Provider invocation for that request; this gate is
  demonstrated as additional to, never a replacement for, the existing
  gates on `EvidenceCustodian.retrieve` and `MemoryRetrieval`; and Unit
  5 is confirmed unreachable in production composition (Unit 8) until
  this Unit is also complete (Unit 5, above).

### Unit 7 — The Evidence Intelligence Acceptance Coordinator

- **Purpose.** Complete the Scope Lock's own frozen acceptance-
  orchestration mechanism (Scope Lock §6, step 4) at implementation-
  shape level (resolving the correction task's item 2) — full source
  code and internal algorithms remain out of scope (§11, below); the
  shape below does not.
- **Concrete, non-interface-backed.** The same shape as
  `EvidenceRegistrationCoordinator`, `ConversationTurnReasoningCoordinator`,
  and `CommunicationConversationCoordinator` (§5, above): no new public
  contract type, no new domain concept beyond what it sequences, no
  state beyond its constructor-injected dependencies. An interface here
  would itself be the new public type the Scope Lock's own "no fifth
  new public type or interface" freeze (§4, as amended) already
  excludes. A future
  need to depend on this behaviour abstractly is a later Contract
  Design question, not one resolved by promoting this class now.
- **Exact existing contracts it depends on.** `EvidenceCustodian.accept`;
  `MemoryCore`'s public write interface; Knowledge Memory's Knowledge
  Submission interface; and the Permission Engine gate each of those
  three already, independently enforces. No dependency beyond these
  four (§4, §5, above; Scope Lock §4).
- **The input it consumes.** One `EvidenceAnalysisResult` list, exactly
  as the Evidence Intelligence operation (Unit 5) returned it for one
  invocation — never a reconstructed, filtered, re-ordered, or
  otherwise reinterpreted copy.
- **The outcome it returns.** Not a new public type (Scope Lock §4's
  own "no fifth new public type" freeze, as amended, applies to this
  Unit as much as to Evidence Intelligence's own model). For each
  candidate in the
  input list, the coordinator dispatches to the one acceptance
  interface that candidate's own kind already names (§6, above), and
  the observable outcome for that candidate is exactly whichever of
  three already-existing possibilities occurred, each expressed
  through that accepting subsystem's own, already-existing,
  unmodified contract — never a new shape this plan introduces:
  1. **Acceptance.** The accepting interface's own existing
     success disposition. The coordinator proceeds to the next
     candidate, retaining nothing about the one just accepted.
  2. **Rejection.** The accepting interface's own existing structural
     rejection disposition (an authorisation or validation outcome
     that interface already defines). The coordinator does not retry
     silently and does not substitute a fabricated acceptance; it
     surfaces that existing disposition through the same
     composition-level observability convention this repository
     already uses for every other rejected proposal (mirroring
     `RuntimeEventLogger`'s own precedent).
  3. **Implementation failure.** A genuine fault (a thrown exception)
     from the accepting interface itself. This is a fault, not a
     value, and propagates or is caught exactly as this repository's
     own established composition-level fault-handling convention
     already does elsewhere (mirroring `ParkerRuntime`'s own outer
     fault handling) — never silently swallowed, never reinterpreted
     as a successful acceptance.
- **How it avoids retaining accepted candidates.** Statelessness, by
  the identical discipline `CommunicationConversationCoordinator`'s own
  "Statelessness invariant" already establishes (§5, above): the
  coordinator holds no `var`, no mutable collection, and no cache of
  any candidate, outcome, or identifier across candidates or across
  invocations — only its constructor-injected dependencies (immediately
  above) as fields. Each candidate is dispatched, its outcome (above)
  is observed, and nothing about it is retained afterward.
- **How it performs Memory Core acceptance before any dependent
  Knowledge Candidate construction/submission.** Because the Contract
  Design (§5) already prohibits a single `EvidenceAnalysisResult` list
  from ever combining a newly proposed, not-yet-accepted
  `CandidateAssertion`/`CandidateRelationship` with a Knowledge
  Candidate referencing it, no list the coordinator ever receives can
  structurally contain that invalid combination — Evidence Intelligence
  (Unit 5) is itself the enforcement point (§8 Unit 5, above), and
  Knowledge Candidate **construction** is Evidence Intelligence's own,
  exclusive responsibility (Contract Design §1), never the
  coordinator's (immediately above, "Concrete, non-interface-backed").
  The coordinator's own, additional, defensive contribution: before
  **submitting** any Knowledge Candidate to Knowledge Memory's
  Knowledge Submission interface, it confirms the Memory Core record
  that candidate references already carries a governed identifier —
  a check that should, per the Contract Design's own structural
  guarantee, never fail; if it ever does, the coordinator treats that
  as a genuine implementation fault (outcome 3, above), never a silent
  proceed, since a caller other than Evidence Intelligence's own
  operation supplying such a list would itself be a violation this
  Unit's own verification (below) must catch.
- **Verification goals.** No coordinator implementation is, or holds a
  reference to, a class implementing the Evidence Intelligence
  operation, in either direction; no coordinator implementation holds a
  reference, at any depth, to `OwnerEvidenceDeletionAuthority`,
  `EvidenceArtifactStorage`, `EvidenceDeletionAudit`, or any Knowledge
  Memory promotion/revision/retirement/restoration mechanism; every
  acceptance call the coordinator makes is independently evaluated by
  that interface's own existing Permission Engine gate; no field on the
  coordinator retains a candidate, outcome, or identifier beyond the
  dispatch that produced it.
- **Completion criteria.** Every candidate Unit 5 can produce reaches
  its accepting subsystem only through the coordinator, only through
  that subsystem's own unmodified acceptance interface; no Knowledge
  Candidate is ever submitted referencing a Memory Core record without
  a confirmed governed identifier; and the coordinator's own concrete,
  non-interface-backed shape, its four named dependencies, its
  three-outcome representation, and its statelessness are each
  demonstrated, not merely asserted.

*Acceptance-tracking note (added after the fact, not part of this unit's
original purpose/responsibilities/dependency/verification text above,
which is unchanged).* `EvidenceIntelligenceAcceptanceCoordinator`
(`src/runtime/EvidenceIntelligenceAcceptanceCoordinator.kt`) implements
this Unit against the now-satisfied Programme 3 Unit 8 dependency
(`docs/reviews/PROGRAMME_4_UNIT_7_DEPENDENCY_RECORD.md`). Its four
constructor dependencies are exactly `EvidenceCustodian`, `MemoryCore`,
`KnowledgeSubmission`, and `PermissionEngine`; it holds its own
`PermissionEngine` reference solely for the `CandidateRecordProduced`
leg, using a new, disclosed-but-unregistered resource/action pair
(`EvidenceIntelligenceAcceptanceCoordinator.MEMORY_CORE_ACCEPTANCE_RESOURCE_ID`/
`ACCEPT_MEMORY_CORE_CANDIDATE_ACTION_NAME`), distinct from
`EvidenceRegistrationCoordinator`'s own pair. `EvidenceCustodian.accept`
and `KnowledgeSubmission.submit` are invoked unchanged, both already
self-gating; neither is re-evaluated by this class. The per-candidate
outcome representation is an internal, non-public sealed type
(`EvidenceIntelligenceAcceptanceOutcome`, with `CandidateMemoryCoreRecordResult`
as its Memory-Core-write payload selector), reusing `EvidenceAcceptanceResult`
and `KnowledgeSubmissionDisposition` unchanged, adding only the one case
neither existing public type can express -- a Memory Core write denied by
this class's own Permission Engine decision. Tests added:
`tests/runtime/EvidenceIntelligenceAcceptanceCoordinatorTest.kt`. Runtime
composition (registering this coordinator, Unit 5, and Unit 6 into
`ParkerRuntime`) remains Unit 8's own, separately governed responsibility,
not begun here.

### Unit 8 — Runtime Composition and Full Verification

- **Purpose.** Wire Units 1–7 into Parker's own composition root, and
  confirm every implementation objective (§3) behaviourally before this
  Programme is considered complete.
- **Responsibilities.** Register the Evidence Intelligence operation
  (Unit 5), the `EXECUTE`/`DOCUMENT` invocation gate (Unit 6), and the
  acceptance coordinator (Unit 7) into the running system, adding no
  new authority, no new bypass, and no relationship beyond what the
  Contract Design's and Scope Lock's own dependency and ownership
  freezes already fix. **Unit 5 is registered for production
  reachability only together with Unit 6 — never before it, never
  without it** (§7, §8 Unit 5, Unit 6, above).
- **Dependencies.** Units 1–7, in full.
- **Verification goals.** Every verification requirement in §9 passes
  against the fully composed system, not against any unit in isolation
  only; no composition state exists in which Unit 5 is reachable
  without Unit 6 already present alongside it.
- **Completion criteria.** Every completion criterion in §10 is
  objectively true of the composed system.

---

## 9. Verification Strategy

Preferring structural or architectural verification over behavioural
assertion wherever a structural mechanism can prove the same invariant
— restating, not narrowing, the Scope Lock's own §9 preference. No
implementation detail is prescribed by any category below; each states
a property a future test suite must demonstrate, regardless of how a
unit is eventually implemented.

- **Unit verification.** Each unit in §8 is verified independently
  against its own stated verification goals and completion criteria,
  before it is relied upon as a completed dependency for any later
  unit.
- **Integration verification.** Once composed (Unit 8), the full
  request-to-result-to-acceptance path is exercised end to end,
  confirming that Units 1–7 cooperate exactly as §7's sequence
  requires, with no step skipped, reordered, or collapsed.
- **Constitutional verification.** Every implementation invariant the
  Scope Lock (§8) fixes, and every objective in §3 above, is checked
  against the composed system — never asserted without a corresponding
  check.
- **Boundary verification.** Every dependency exclusion the Scope Lock
  (§4, §9) and Contract Design (§12, §15) fix is checked by dependency
  reachability, not by inspection of a sampled subset: no class
  reachable from the Evidence Intelligence operation or the acceptance
  coordinator holds a reference, at any depth, to any of the excluded
  items either document names.
- **Regression verification.** Every verification above is repeatable
  and re-run whenever any of Units 1–8 changes, so that a later
  modification cannot silently reintroduce a dependency, ownership
  change, or sequencing violation this plan's own units already closed
  out.
- **Unit 2 verification gate.** Subject to the mandatory Unit 2
  verification gate (§8 Unit 2, the authoritative statement) — verified
  structurally, as part of Unit verification above, before Unit 2 is
  relied upon by any later unit.

---

## 10. Completion Criteria

Implementation of Evidence Intelligence may be considered complete only
when all of the following are objectively true:

1. Every objective in §3 is demonstrated by a passing verification from
   §9 — not asserted, demonstrated.
2. No code path exists by which Evidence Intelligence accepts raw,
   uncustodied evidence, or resolves a reference by any means other than
   `EvidenceCustodian.retrieve` or `MemoryRetrieval`.
3. No code path exists by which the Evidence Intelligence operation is
   invoked without a preceding, dedicated Permission Engine evaluation
   of the `EXECUTE`/`DOCUMENT` "invoke Evidence Intelligence" proposal
   class (§8 Unit 6), and no composition state exists in which the
   operation (Unit 5) is reachable in production without this gate
   (Unit 6) already present alongside it (§8 Unit 5, Unit 8).
4. No code path exists by which a produced candidate reaches its
   accepting subsystem other than through the Evidence Intelligence
   acceptance coordinator — a concrete, non-interface-backed,
   stateless class (§8 Unit 7) — invoking that subsystem's own
   unmodified acceptance interface.
5. No code path exists by which a Knowledge Candidate is constructed
   referencing a Memory Core record that has not yet been accepted and
   assigned a governed identifier; every Knowledge Candidate
   referencing a newly accepted record is constructed only by a
   second, ordinary Evidence Intelligence invocation (§8 Unit 5), never
   by the acceptance coordinator (§8 Unit 7).
6. No code path exists by which Evidence Intelligence, or the
   acceptance coordinator, retains a reference, modification right, or
   residual claim over an artefact or record after it has been accepted.
7. No code path exists by which a durable confidence field or an
   `EvidentialState` value is populated by Evidence Intelligence.
8. No code path exists, reachable at any depth from the Evidence
   Intelligence operation, the acceptance coordinator, or either new
   public shape, to any of the items the Scope Lock (§4, §9) and
   Contract Design (§12, §15) name as excluded.
9. Every material analytical claim is represented as its own
   `TransientOutput` value, carrying one non-blank prose string and one
   or more governed references (`EvidenceArtifactId` and/or
   `RelationshipEndpoint`) — never one `TransientOutput` internally
   bundling several claims, and never a claim carrying no governed
   reference; each `TransientOutput` distinguishes extracted content,
   observed content, inferred analytical conclusions, and
   model-generated explanatory language within its own single claim.
10. Every candidate derivative artefact resulting from transcription
    distinguishes verbatim transcription, normalised transcription, and
    inferred reconstruction wherever more than one applies.
11. No genuine contradiction between two Memory Core Assertions is ever
    represented as only a `SUPPORTS`-typed output for one side.
12. No empty or fabricated `EvidenceAnalysisResult` list is ever used to
    signal a genuine analytical failure or a partial-completion case;
    every per-input retrieval failure within a partially-successful
    invocation is disclosed exclusively through `EvidenceRetrievalResult`
    and existing observability (§8 Unit 2), never a wrapper, a fifth
    variant, or a silent omission.
13. The mandatory Unit 2 verification gate (§8 Unit 2, the
    authoritative statement; §9, above) has run and passed.
    Implementation is not complete, and no unit depending on Unit 2 may
    be relied upon, otherwise.
14. No excluded capability from §11 (below) has been introduced,
    directly or indirectly.
15. No new constitutional decision has been made in the course of
    implementation without a corresponding governance record.
16. Exactly four public runtime types exist across the full programme
    — `EvidenceAnalysisRequest`, `EvidenceAnalysisResult`, the payload
    selector for "Candidate record produced" (§6, item 4, above), and
    `EvidenceIntelligence` (§8 Unit 5) — never a fifth.
    `EvidenceAnalysisResult` remains exactly four categories; the
    payload selector remains exactly two cases (`CandidateAssertion`,
    `CandidateRelationship`); no separate `AnalyticalClaim` public
    type, or any other dedicated claim type, exists anywhere in the
    compiled repository; every `TransientOutput` value is
    independently traceable to its own governed reference(s), never
    relying on a claim collection or wrapper type of any kind; and no
    fifth `EvidenceAnalysisResult` category, general-purpose union
    type, or other new sealed or closed shape competing with the
    frozen four-category taxonomy exists anywhere.

---

## 11. Explicitly Out of Scope

Every architectural exclusion already frozen by the Contract Design and
the Scope Lock, restated here in full — none invented, none narrowed,
none widened:

- Evidence Custodian redesign, of any kind.
- Memory Core redesign, of any kind.
- Knowledge Memory redesign, of any kind.
- Provenance redesign, a new provenance type, or mirrored/duplicate
  provenance, at claim level or otherwise.
- OCR, transcription, translation, or any other analysis kind's own
  internal algorithm.
- A document ingestion pipeline, or any acceptance of raw, uncustodied
  evidence bytes.
- A user interface of any kind.
- Android or any client/mobile-layer work.
- Planning, or any dependency on Planner Runtime.
- Execution, or any dependency on Execution Pipeline.
- Autonomous or self-initiated analysis.
- Agent or Agent Runtime changes.
- Repository restructuring, a new source directory, or a new module
  boundary.
- Acceptance orchestration performed by Evidence Intelligence itself
  (as distinct from the separate acceptance coordinator, §6 above).
- A new evidence store, memory system, comparison engine, or promotion
  system of Evidence Intelligence's own.
- A Reasoning Provider abstraction specific to Evidence Intelligence.
- An `EvidenceIntelligenceRegistry` or similar discovery mechanism.
- A caller-declared confidence or evidential-state value, accepted as
  input.
- Any durable confidence field population.
- Any `EvidentialState` assignment, or dependency reaching one.
- Any Knowledge promotion, revision, retirement, or restoration act.
- Any dependency, at any depth, on `OwnerEvidenceDeletionAuthority`,
  `EvidenceArtifactStorage.delete`, or `EvidenceDeletionAudit`.
- Any dependency on `EvidenceCustodian.accept`, `MemoryCore`'s public
  write interface, or Knowledge Memory's Knowledge Submission interface
  — held only by the acceptance coordinator, never by Evidence
  Intelligence itself.
- A Permission Engine dependency of Evidence Intelligence's own (the
  invocation gate, §6 item 6 above, is held by the composing caller,
  never by Evidence Intelligence).
- A bespoke comparison-result type, or a comparison mechanism not
  reconciled with Model B.
- A fifth `EvidenceAnalysisResult` category, a new failure-result
  taxonomy, a partial-result wrapper, an alternate output taxonomy, or
  any new sealed or closed shape that competes with, expands, or stands
  beside the frozen four-category `EvidenceAnalysisResult` taxonomy
  (Scope Lock §3, as amended). This exclusion does not reach the
  payload selector authorised above (§6, item 4) — it is not an output
  category, not a failure taxonomy, and not a partial-result mechanism;
  it exists solely as "Candidate record produced"'s own payload.
- Any storage technology, file system, database, or object store.
- Any hashing algorithm, integrity-verification scheme, or cryptographic
  method.
- Any API, RPC contract, or wire format.
- Any Kotlin interface, class, enum, or method signature.
- Any database schema or persistence model.
- The acceptance coordinator's literal Kotlin identifiers — its actual
  class name, its actual method and constructor signatures as source
  code, and its internal algorithm — remain out of scope (this plan
  fixes no source code, in accordance with §2). Its implementation
  *shape* — that it is concrete and non-interface-backed, its exact
  four existing-contract dependencies, the single-list input it
  consumes, its three-way outcome representation, its statelessness,
  and its ordering/defensive-check behaviour toward Memory Core
  acceptance — is not out of scope; it is frozen by this plan, §8
  Unit 7, resolving the decision the Scope Lock, §6, assigned here.
- The literal `PermissionLevel` value and any literal string
  identifiers used in registering the invocation-gating proposal
  remain out of scope, as composition-root policy content decided at
  implementation time, not by this plan. The proposal class itself —
  `PermissionAction.EXECUTE` on `ResourceType.DOCUMENT` — is not out
  of scope; it is frozen by this plan, §8 Unit 6, resolving the
  decision the Scope Lock, §6, §7, §11, assigned here.
- Any specific analysis kind's own internal algorithm.
- Any amendment to Memory Core's, Evidence Custodian's, or Knowledge
  Memory's own contract, schema, or interface.
- Any question of legal ownership, copyright, proprietary interest, or
  lawful possession of any artefact.
- Comparison reconciliation against Model B's nine guarantees is
  performed at implementation-review time, per unit, not decided by
  this plan in the abstract.

No unit in §7 or §8 may be read as authorising any of the above merely
because it would be convenient during implementation. Any perceived
need for one of these during actual engineering work is a signal to
stop and escalate to governance, not to proceed.

---

## 12. Design Rules Compliance

A direct self-check against every named governing authority, performed
once here rather than asserted without demonstration:

**Contract Design.** Every unit in §8 traces to a specific Contract
Design section; no unit builds a responsibility, output category,
interface, or dependency the Contract Design does not already
authorise (§3, §5, §6, above).

**Scope Lock.** Every exclusion (§11, above), every dependency (§5,
above), every ownership rule (§3, above), every sequencing step (§7,
above), and every invariant (§8, §10, above) restates the Scope Lock's
own §2 through §9 without narrowing, widening, or reinterpreting any of
them — including the two completed Scope Lock decisions (the acceptance
coordinator, Unit 7; the invocation gate, Unit 6), neither of which this
plan treats as still open.

**Constitution.** No unit creates a path around "cognition proposes,
trust authorises, runtime executes": Evidence Intelligence proposes
candidate output only (Unit 5); the acceptance coordinator never
self-authorises an acceptance call (Unit 7); the invocation gate is
evaluated before analysis, never bypassed (Unit 6).

**Ownership.** No unit grants Evidence Intelligence, or the acceptance
coordinator, ownership of anything beyond production up to acceptance
(§3, Unit 7, above); every ownership-transfer rule in the Scope Lock's
own §5 is restated, not altered.

**Dependency boundaries.** §5's reused-component list is exhaustive and
closed; no unit in §7 or §8 introduces a dependency beyond it; the
acceptance coordinator's own dependency boundary (§5, §6 items 4 and 7,
above) is held structurally separate from Evidence Intelligence's own.

**Reuse rules.** Every existing type, interface, and mechanism this
plan sequences is reused unmodified (§5, above); no unit proposes an
amendment to any of them.

**Permission boundaries.** Three permission-relevant boundaries are
sequenced, never conflated: the invocation gate (Unit 6), the two
existing read gates (Unit 2), and the three existing acceptance gates
(Unit 7) — each independently evaluated, none replacing another.

**Evidence Custodian boundaries.** No unit holds a dependency on
`EvidenceCustodian.accept` outside the acceptance coordinator (Unit 7),
and none at all on `OwnerEvidenceDeletionAuthority`,
`EvidenceArtifactStorage.delete`, or `EvidenceDeletionAudit`, at any
depth, anywhere in this plan.

**Memory Core boundaries.** No unit holds a dependency on `MemoryCore`'s
public write interface outside the acceptance coordinator (Unit 7);
every Memory Core type this plan sequences is reused unmodified (§5,
above).

**Correction-task decisions, resolved rather than deferred.** The four
questions the accepted Scope Lock assigned to this Implementation Plan
(Scope Lock §6, §7, §11) are resolved, not deferred, each as defined in
full at its own home section: the invocation proposal class (§8 Unit
6), the acceptance coordinator's implementation shape (§8 Unit 7), the
partial-completion disclosure mechanism (§8 Unit 2), and the Knowledge
Candidate sequencing mechanism (§8 Unit 5). None surfaced a governance
blocker; each is a disclosed application of an already-authorised
existing mechanism, not a forced fit; none narrows, widens, or
reinterprets any Contract Design or Scope Lock decision.

---

## 13. Implementation Readiness

Independent Constitutional Review of this Implementation Plan is
complete, exactly as the Contract Design and the Scope Lock each
already completed before this document could exist. This document is
now accepted and binding: Evidence Intelligence engineering may begin.

**Authorisation.** Implementation must proceed unit by unit, strictly
in the frozen order this plan already fixes (§7, §8); no later unit may
begin before every unit it depends on has independently met its own
completion criteria (§8, §10) — a unit is not "close enough" to
complete for a dependent unit to start against it. This authorisation
carries one mandatory stop condition, not an option: the Unit 2
verification gate (§8 Unit 2, the authoritative statement; §9; §10,
item 13). This plan does not, and cannot, resolve that gate's outcome
for itself in advance.

**Status: Accepted. Canonical. Implementation Authorised.** No
Constitutional Decision Record is created by this document. No other
document — the Contract Design, the Scope Lock, any CDR, the Parker
Constitution, or any other governance record — is modified by this
document.

EVIDENCE INTELLIGENCE IMPLEMENTATION PLAN — ACCEPTED — CANONICAL —
IMPLEMENTATION AUTHORISED

Confirmed: no Kotlin implemented; no interface, method signature, API,
schema, or storage technology defined; no pseudocode, diagram, or
implementation example included; the Contract Design and the Scope Lock
unmodified; CDR-001 through CDR-007 unmodified; the Parker Constitution
unmodified; nothing staged; nothing committed; nothing pushed; no
production code touched; no Constitutional Decision Record created;
Unit 1 not begun by this document; the Unit 2 verification gate (§8
Unit 2; §9; §10, item 13, above) stands as a mandatory, binding stop
condition on all implementation that follows.
