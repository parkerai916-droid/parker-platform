# Chapter 10 — Permission Engine

**Status:** Constitutional governance document. This chapter is a draft,
prepared to complete Chapter 10's own position in Parker's numbered
architecture sequence, pending independent constitutional review and a
Final Freeze Verification before it is treated as frozen governance. It
is not yet frozen — this document does not claim a status it has not yet
earned. Upon freeze, it is intended to be authoritative for the Permission
Engine's own constitutional contract, subordinate only to the Parker
Constitution (`docs/architecture/parker-constitution.md`) and to Trust
Framework's own constitutional statement (Chapter 9,
`docs/architecture/09-trust-framework.md`), of which the Permission
Engine is one named constituent. No Kotlin, no interface, no data class,
no field layout, no API, and no implementation sequencing exists anywhere
in this document or arises from it. This document does not draft,
anticipate, or pre-decide Phase 1 Amendment 3 (Knowledge Submission
Permission Gating) of the Parker Constitutional Remediation Programme —
it exists so that amendment becomes possible to draft safely, not to
perform it.

This document replaces this chapter's previous two-line placeholder
content in place. It preserves Chapter 10's position in the numbered
architecture sequence, immediately following Chapter 9 — Trust Framework.
It does not create a second, competing Permission Engine governance
document: `docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md`
remains scoped to Authentication only, as it always has been, and is
unmodified by this document; `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
remains the current, versioned, implementation-facing interface
specification, unmodified by this document, and is expressly
non-authoritative for the constitutional content this chapter now
supplies. This chapter is, upon freeze, the single canonical
constitutional authority for the Permission Engine; no other document is
authorised to state or imply a competing one. Proposal-class admission
(Sections 3 and 10) is governed by the now-frozen
`docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`,
which this chapter depends upon without restating or duplicating its
authority.

The Permission Engine determines whether a requested action may proceed.
Its original, retained core question: *may this Principal perform this
Action on this Resource under these circumstances?* This document
generalises that question, in Section 3 below, beyond any single
implementation-level request shape, without abandoning it.

---

## 1. Constitutional Purpose

The Permission Engine is the sole authority for turning a proposal into
an authorised action. This is not a role this document invents; it is
stated directly by the Parker Constitution's own Architectural
Responsibilities: "The Permission Engine is the sole authority for
turning a proposal into an authorized action. It alone applies
owner-defined policy to a proposal." This chapter exists to give that
one-sentence constitutional assignment the fuller governance content a
sole authority requires.

The Permission Engine applies owner-defined policy — it does not
originate policy, and it does not decide what the owner's policy ought to
be. It is the mechanism by which policy the owner already holds is
applied to a specific proposal, consistent with the Constitution's own
"Parker owns authority. Modules provide capability" and "The owner
remains in control."

The Permission Engine is constitutionally distinct from:

- **Authentication** — establishing who is asking. Per
  `docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md` §5:
  "Authorisation... is the existing constitutional responsibility already
  held by the Permission Engine... Authentication & Trust is upstream of
  it, supplying a richer basis for that existing decision to be made
  against; it does not rename, relocate, or duplicate the Permission
  Engine's own authority." Authentication answers *who*; the Permission
  Engine answers *whether, having established who, this may proceed*.
- **Execution** — carrying out an already-authorised action. Per the
  Constitution: "Cognition proposes. Trust authorises. Runtime executes...
  No stage may absorb another... Trust may not execute itself."
- **Knowledge evaluation and promotion** — assessing the substantive
  merit, confidence, or evidential status of submitted material. This
  belongs to the domain system that owns that evaluation (for example,
  Knowledge Memory, per `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
  §5, §11), never to the Permission Engine, which decides only whether an
  act may proceed, not whether its content is true, sufficient, or
  worthy.

## 2. Ownership

- **Constitutional owner: Trust Framework.** Chapter 9
  (`docs/architecture/09-trust-framework.md`) names "Identity, Resource
  Registry, Permission Engine, Policy Service, Confirmation Engine and
  Audit Framework" as the Trust Framework's own constituents. The
  Permission Engine's authority is established directly by the
  Constitution and exercised at the Trust Framework layer, per the
  governing hierarchy: Owner → Constitution → Trust Framework → Runtime →
  Reasoning Provider.
- **Operational caller: Runtime.** `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`
  states, for its own domain and by pattern generalisable to any domain
  requiring permission gating: "Runtime owns every `PermissionEngine.evaluate`
  call required before any `MemoryCore` write and before any sensitive
  `MemoryRetrieval` read reaches its requester." Runtime invokes the
  Permission Engine; invoking it does not make Runtime its constitutional
  owner (Section 7 restates this explicitly).
- **Enforcement owner: Runtime.** Once a decision is made, carrying it
  out — including declining to proceed on denial — is Runtime's
  responsibility, per the Constitution's "Runtime executes" stage and its
  own Execution Pipeline.
- **Policy source: the Owner, through constitutional governance.** The
  Permission Engine applies policy; it does not author it. Policy
  originates from the owner and from the constitutional and governance
  documents that give the owner's authority durable, inspectable form
  (Constitution: "Every claim Parker makes about what it will and will
  not do is backed by a structural mechanism that enforces it"). Where
  policy scales with an action's risk,
  `docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md` §6–§7
  already reserves the Trust Levels and Risk Levels concepts and their
  mapping to one another as policy, "deferred to future work" there —
  this chapter does not itself define authentication policy or any risk
  category; the Permission Engine's role remains, as stated above, to
  apply whatever policy, risk-scaled or otherwise, the Owner has
  established.
- **Implementation location does not determine constitutional authority.**
  `src/interfaces/PermissionEngine.kt` and `DefaultPermissionEngine`
  are today's implementation evidence of this chapter's subject, quoted
  here only as evidence, not as the source of the Permission Engine's
  authority. Per this task's own governing instruction, and consistent
  with the Constitution's "If a safeguard cannot be pointed to in the
  architecture, it does not count as a guarantee" — architecture, not
  code location, is what this document treats as authoritative.

## 3. Proposal Abstraction

Constitutionally, a **proposal** (interchangeably, a **permission
request**) is any act, originating from cognition, a module, or a domain
system, that seeks to have some effect beyond mere reasoning, and that
therefore requires the Permission Engine's authorisation before it may
proceed. A proposal constitutionally consists of, at minimum: an
identified or identifiable requesting Principal; a description of the
action sought; the resource or record class the action would affect; and
whatever circumstantial context bears on whether the action should be
authorised.

A proposal is distinguished from ordinary internal computation by whether
it carries a real, external, or state-changing consequence beyond
Parker's own internal reasoning — for example, writing, amending, or
superseding a durable record; granting access to a sensitive record or
capability; or otherwise reaching beyond pure interpretation into an
effect an owner would recognise as an action taken on their behalf.
Ordinary internal computation — reasoning over already-available
information, structural retrieval of non-sensitive records, or any
operation whose effect is confined to producing an answer rather than
taking an action — does not, by itself, constitute a proposal and does
not require Permission Engine authorisation. This general admission
criterion is illustrated, but not exhausted, by Memory Core's own
precedent (`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §5: gating
applies "before any `MemoryCore` write and before any sensitive
`MemoryRetrieval` read," expressly distinguished there from Memory
Core's own "structural, non-semantic retrieval," which is not gated) —
that domain's own specific line is evidence of how one domain applies
this general criterion, not a rule this chapter imports wholesale for
every domain. Applying this general criterion to any specific domain's
own acts is that domain's own classification exercise, performed and
disclosed as CDR-005 requires (Section 10), not something this chapter
performs on a domain's behalf.

This concept is deliberately independent of any single implementation
shape. Today, the only implemented request shape is `ExecutionRequest`,
an Execution Pipeline/Tool-invocation concept — quoted here strictly as
evidence of current scope, not as a constitutional limit. This chapter
does not treat `ExecutionRequest` as the permanent or exclusive
constitutional form a proposal must take. A **proposal class** is a
constitutionally recognised category of act requiring authorisation —
Execution Pipeline/Tool invocation is one such class; a domain system's
own submission act (for example, but not limited to, a future Knowledge
Candidate submission) may constitute another, once the domain governance
that owns that act defines how its act maps onto this abstraction
(Section 10). This document does not itself define that mapping for any
specific domain — doing so is the responsibility of the governance
document that owns the requesting domain, consistent with this chapter's
own Section 9.

## 4. Permission Outcomes

Consistent with this chapter's own existing, retained terminology and
with `src/contracts/Permission.kt`'s current implementation (quoted here
as confirming evidence, not as the source of these outcomes' authority),
four constitutional outcomes exist, corresponding to the categories this
task requires be addressed:

- **Approved** (permitted) — the proposal may proceed without further
  condition.
- **Denied** (denied) — the proposal may not proceed. Per Section 6, a
  denial must be traceable to the specific insufficiency of evidence or
  policy that produced it.
- **Deferred** (indeterminate or insufficiently established) — the
  Permission Engine cannot yet resolve the proposal to Approved or
  Denied. Per Section 9, a Deferred outcome is never treated as
  permission to proceed; it constitutionally behaves as a denial for as
  long as it remains unresolved.
- **Approved With Confirmation** (conditional, owner-confirmation-
  required) — the proposal may proceed only after an explicit,
  affirmative act of confirmation by the owner or an authorised party,
  consistent with `docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md`
  §5's own definition of "Approval" as "an explicit act, by an owner or
  an authorised party, affirmatively permitting an action."

No outcome beyond these four is introduced. These four are the same four
this chapter's own prior text already named; this document supplies
their constitutional meaning rather than replacing them.

## 5. Constitutional Guarantees

- **Sole-authority guarantee.** The Permission Engine is the sole
  authority for turning a proposal into an authorised action (Section 1;
  Constitution, Architectural Responsibilities).
- **No-bypass guarantee.** "There is no path from proposal to execution
  that does not pass through the Permission Engine... Convenience is
  never a justification for a shortcut around trust" (Constitution).
- **Owner-policy guarantee.** The Permission Engine applies only policy
  the owner holds; it never originates, expands, or reinterprets policy
  on its own initiative (Constitution: "Parker owns authority. Modules
  provide capability"; "The owner remains in control").
- **Fail-closed / inaction-on-uncertainty guarantee.** "Where... the
  Permission Engine cannot establish that an action is authorized,
  Parker's only correct behavior is to decline to act. Uncertainty about
  trust never defaults to permissiveness. It defaults to inaction"
  (Constitution); reinforced by `AUTHENTICATION_AND_TRUST_GOVERNANCE.md`
  §4's "Failure defaults to deny" and §10's "Sensitive actions fail
  closed."
- **Separation from execution.** "Trust may not execute itself... Runtime
  may not reinterpret what was authorized" (Constitution).
- **Separation from authentication.** The Permission Engine does not
  establish who is asking; that determination is a prerequisite input to
  it, not something it assumes or performs itself (Constitution,
  Architectural Responsibilities: "The Identity Service establishes who
  or what is making a request, and this determination is a prerequisite
  input to the Permission Engine, not something the Permission Engine
  assumes").
- **Separation from domain evaluation.** The Permission Engine decides
  only whether an act may proceed, never the substantive merit of what
  the act concerns. `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §6
  establishes this boundary from the domain side ("Memory Core never
  evaluates permissions... No contract implementing `MemoryCore` or
  `MemoryRetrieval` may hold a `PermissionEngine` reference"); this
  guarantee states the converse, binding on the Permission Engine itself:
  it never assesses knowledge truth, computes confidence, decides
  evidential state, or promotes or rejects material on quality grounds —
  those remain exclusively the responsibility of the domain system that
  owns them (for example, Knowledge Memory, per Contract Design V2 §5,
  §11's own "no caller-facing promotion" guarantee).
- **Traceability and auditability.** "Every authorized action leaves a
  record sufficient to reconstruct what was proposed, what was
  authorized, by what authority, and what was executed" (Constitution);
  `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`'s
  own Normative Requirements already state "Denied decisions MUST include
  a reason" and "Permission evaluation MUST be auditable," quoted here as
  confirming, non-authoritative evidence.
- **Technology independence.** Per the Constitution's own discipline of
  stating "what must remain true regardless of implementation" and its
  Design Goal to "make trust a structural property of Parker, verifiable
  by inspection of the architecture rather than by claims about
  behaviour" — no reasoning provider, storage technology, or specific
  request implementation is load-bearing for this chapter's guarantees.
- **Proposal-class extensibility.** Unlike the guarantees above, no
  prior repository text states this guarantee for the Permission Engine
  specifically — it is established here, for the first time, as this
  chapter's own direct response to the gap
  `docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md` and
  `docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md` both independently
  found and disclosed: that the Permission Engine's only implemented
  request shape, `ExecutionRequest`, has "no documented mapping to or
  from a Knowledge Candidate submission." It is grounded in the
  Constitution's own "Modular capability... Parker's capability grows
  through modules" and "Future Considerations: Future architecture
  documents may add detail, nuance, and domain-specific rules" — this
  document exercises exactly that latitude, and no more of it than
  Section 3 and Section 10 require.

## 6. Boundaries

The Permission Engine is explicitly excluded from:

- executing actions (Section 1; Constitution's "Runtime executes");
- authenticating identity (Section 1; `AUTHENTICATION_AND_TRUST_GOVERNANCE.md`
  §5);
- originating owner authority — it applies authority the owner already
  holds, and never grants itself, expands, or invents authority
  (Constitution: "No module may grant itself authority," applied here by
  the same principle to the Permission Engine's own operation: applying
  policy is not authoring it);
- assessing knowledge truth;
- computing confidence values;
- deciding evidential state;
- promoting or rejecting knowledge on quality grounds (the prior four
  items in this list are the Permission Engine's own mirror of
  `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
  §11's "no caller-facing promotion, and no caller-supplied confidence or
  evidential state" guarantee — the Permission Engine no more decides
  these than a submitter does); and
- silently converting uncertainty into permission — an unresolved,
  indeterminate, or unavailable evaluation must never be treated,
  explicitly or by default, as authorisation to proceed (Section 9;
  Constitution's "Uncertainty about trust never defaults to
  permissiveness").

## 7. Runtime Relationship

Runtime:

- **presents proposals for permission evaluation** — it is the layer
  that invokes the Permission Engine on behalf of whichever domain
  system's act requires authorisation (Section 2; Memory Core Scope
  Lock's "Runtime owns every `PermissionEngine.evaluate` call");
- **enforces decisions** — carrying out only what has been authorised,
  exactly as authorised (Constitution: "The Execution Pipeline carries
  out only what has been authorized, exactly as authorized");
- **must not bypass or reinterpret a denial** — a Denied or unresolved
  Deferred outcome constitutionally ends the proposal's path to
  execution; Runtime has no authority to proceed around it or to treat a
  denial as anything other than what it was ("Runtime may not
  reinterpret what was authorized");
- **must not treat unavailability or indeterminacy as permission** — if
  the Permission Engine cannot be reached, or cannot resolve a proposal,
  Runtime's only constitutionally correct behaviour is to treat the
  proposal as not authorised (Section 9); and
- **does not become the constitutional owner of the Permission Engine by
  invoking it** — operational invocation and constitutional ownership are
  deliberately distinct (Section 2); Runtime's role here is identical in
  kind to its role as operational caller elsewhere in Parker's
  architecture, and confers no authority to redefine what the Permission
  Engine is or does.

## 8. Domain Consumer Relationship

Domain systems — Memory Core, Knowledge Memory, and any future system
whose acts carry real consequence — relate to the Permission Engine as
follows:

- **they may require permission gating** for acts within their own
  domain, exactly as Memory Core's own Scope Lock already establishes for
  writes and sensitive reads, and as
  `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §7
  already names, without yet mechanically defining, for Knowledge
  Memory's own submission boundary ("Evaluation B");
- **they do not own the Permission Engine** — `MEMORY_CORE_SCOPE_LOCK.md`
  §6 states this explicitly for Memory Core ("Memory Core never evaluates
  permissions... No contract implementing `MemoryCore` or
  `MemoryRetrieval` may hold a `PermissionEngine` reference"), and this
  chapter generalises the same principle to every domain system: none
  may hold, construct, or implement Permission Engine authority itself;
- **they do not directly override permission outcomes** — a domain
  system receives a decision; it does not substitute its own judgment for
  the Permission Engine's, in either direction; and
- **their own substantive evaluation remains separate and downstream** —
  whatever a domain system does after a proposal is authorised (for
  example, Knowledge Memory's own promotion evaluation, Contract Design
  V2 §5) is a distinct, later act, never a re-litigation of, nor a
  substitute for, the permission decision itself (Contract Design V2 §7's
  own "these are two genuinely different acts, not one act checked
  twice," stated there for Evaluation A and Evaluation B specifically,
  restated here as a general principle governing every domain
  consumer).

This section is deliberately general. It does not define how any
specific domain system's own act (including a future Knowledge Candidate
submission) maps onto the proposal abstraction of Section 3 — that
mapping is the responsibility of the governance document owning that
domain (for Knowledge Memory, a future Contract Design V2 amendment), not
this chapter.

## 9. Failure Semantics

- **Permission denied.** The proposal does not proceed. The denial must
  be traceable to the specific insufficiency of evidence or policy that
  produced it (Section 5, Traceability and auditability guarantee).
- **Permission indeterminate (Deferred, or otherwise unresolved).** The
  proposal does not proceed while unresolved; an unresolved state is
  never treated as, or silently converted into, authorisation
  (Constitution: "Uncertainty about trust never defaults to
  permissiveness. It defaults to inaction").
- **Permission Engine unavailable.** Constitutionally identical in
  consequence to an indeterminate result: the proposal does not proceed.
  Unavailability of the sole authority for authorisation is not, and can
  never be treated as, a condition that authorises anything.
- **A caller attempts bypass.** Constitutionally prohibited outright,
  not merely discouraged: "No capability may bypass trust... There is no
  path from proposal to execution that does not pass through the
  Permission Engine" (Constitution). Any path that reaches execution, or
  reaches a domain system's own recording or evaluation step, without
  having passed through the Permission Engine is, by the Constitution's
  own terms, a violation of Parker's architecture, not a permitted edge
  case.
- **Downstream evaluation fails after permission was granted.** An
  authorised proposal that later fails a domain system's own substantive
  evaluation (for example, Knowledge Memory's promotion criteria) does
  not retroactively invalidate the permission grant, and the permission
  grant never guaranteed a favourable evaluation outcome (Section 8;
  Contract Design V2 §7's "two genuinely different acts"). The two
  outcomes — permission and evaluation — remain independently disclosed
  and independently traceable.

## 10. Extensibility

Recognition of a new proposal class follows the constitutional procedure
established by `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`,
which this chapter depends upon and does not restate in full or
duplicate the authority of:

- **Chapter 10 owns the admission criteria** — the general boundary
  stated in Section 3 belongs to this chapter alone; no domain document
  may invent, weaken, or substitute its own admission test.
- **Domains define their own acts** — a domain's own governance document
  (for example, a future Contract Design V2 amendment) describes the act
  it is introducing.
- **Domains explicitly classify consequential acts against Chapter 10's
  criteria** — every newly introduced act with a real, external, or
  state-changing consequence (Section 3) receives an explicit, disclosed
  classification in the domain's own document, citing the Chapter 10
  criterion relied upon, whether the act is classified as a Permission
  Engine proposal class or as ordinary internal computation outside this
  chapter's scope. Neither a positive nor a negative classification is
  asserted or omitted silently.
- **Contested classifications escalate through the established CDR
  process** — where a domain's classification is genuinely contested,
  ambiguous, or would require choosing between constitutionally plausible
  readings of Chapter 10's criteria, the domain does not resolve the
  question itself; it is escalated to a Constitutional Decision Record,
  exactly as CDR-005 requires.
- **Chapter 10 itself is reopened only when its own constitutional
  content changes** — its sole-authority rule, ownership assignments,
  Runtime relationship, guarantees, boundaries, permission outcomes,
  failure semantics, or the general admission criteria of Section 3 —
  never merely because a new domain act, cleanly fitting existing
  criteria, is being recognised.

This recognises future proposal classes — including, but not limited to,
a future Knowledge Candidate submission act — without weakening the
sole-authority rule (every proposal class, however it is expressed, is
authorised by this same Permission Engine and no other), without
creating alternate permission engines (a domain system may define how
its own act maps onto Section 3's abstraction, but may never construct,
implement, or substitute a second authority in the Permission Engine's
place, per Section 8), and without binding this chapter to one interface
shape (`ExecutionRequest` remains today's only implemented request type,
per Section 3, but this chapter's own guarantees do not depend on that
remaining permanently true).

## 11. Cross-Document Consistency Check

This chapter's content was checked for consistency against:

- **`docs/architecture/parker-constitution.md`** — no inconsistency
  found; every guarantee and boundary in this chapter traces directly to
  constitutional text quoted above.
- **`docs/architecture/09-trust-framework.md`** — no inconsistency found;
  this chapter completes, rather than contradicts, that document's own
  one-sentence naming of the Permission Engine as a Trust Framework
  constituent. Chapter 9 itself is unmodified.
- **`docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md`** — no
  inconsistency found; this chapter relies on, and does not restate or
  alter, that document's own account of Authentication as upstream of,
  and distinct from, the Permission Engine's authority. That document is
  unmodified.
- **`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`** — no inconsistency
  found; Sections 2, 7, and 8 of this chapter generalise, and do not
  contradict, that document's own Runtime-ownership and
  Memory-Core-never-evaluates-permissions language. That document is
  unmodified.
- **`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`**
  — no inconsistency found; this chapter's Section 8 and Section 10 are
  deliberately written to remain compatible with, and do not pre-decide,
  §7's own still-unspecified Evaluation B mechanism. That document is
  unmodified.
- **`docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`** and
  **`docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md`** — no
  inconsistency found; this chapter's Proposal Abstraction (Section 3)
  and Extensibility (Section 10) sections directly address the specific
  gap both documents independently identified and disclosed. Neither
  document is modified.
- **`docs/governance/PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md`**
  — no inconsistency found; the Roadmap's own Amendment 3 entry names
  exactly this gap ("no dedicated, detailed Trust Framework contract-
  design document defining `PermissionEngine`'s own shape was located");
  this chapter is that document's completion, not a contradiction of it.
  The Roadmap is unmodified.
- **Frozen Amendments 1, 2, and 5** (`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`
  §§18–19; `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
  §16) — no inconsistency found; none of the three concerns the
  Permission Engine's own contract, and none is touched by this chapter.
- **CDR-001 through CDR-004** — no inconsistency found; all four concern
  Memory Record Comparison and Provenance Identifier Resolution, a
  distinct subject area with no textual overlap with this chapter's
  subject. None is touched by this chapter.
- **CDR-005** — no inconsistency found; this chapter's Section 3 and
  Section 10 depend upon, reference, and apply CDR-005's own frozen
  admission procedure without restating its analysis or duplicating its
  authority. CDR-005 is not modified by this chapter.

No conflict was found anywhere in this review.

---

## Relationship to Existing Constitution and Trust Framework

This chapter is an extension of Parker's existing constitution
(`docs/architecture/parker-constitution.md`) and of Chapter 9 — Trust
Framework, not a replacement of either. The Constitution's governing
invariants — "Parker owns authority. Modules provide capability." and
"Cognition proposes. Trust authorises. Runtime executes." — remain
exactly as written and exactly as binding. This chapter states, at the
level of detail a sole constitutional authority requires, what the
Constitution already assigns to the Permission Engine in one sentence; it
does not reinterpret, weaken, or supersede any principle the Constitution
or Chapter 9 already state.

## Deliverable

A constitutional governance document, replacing this chapter's own prior
two-line placeholder in place, at
`docs/architecture/10-permission-engine.md`, preserving Chapter 10's
position in Parker's numbered architecture sequence. No code, no
interfaces, no implementation, no tests, and no amendment to any other
document. This chapter does not draft Phase 1 Amendment 3; it exists so
that amendment can be drafted safely once this chapter itself has
completed independent constitutional review and Final Freeze
Verification.
