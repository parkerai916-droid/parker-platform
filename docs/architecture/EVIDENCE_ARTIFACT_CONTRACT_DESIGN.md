# Evidence Artifact Contract Design

## Status

Programme: **Evidence Custodian — Contract Design, Phase 1.**
Phase: **Governance and design only.** No Kotlin is implemented, proposed
as a diff, or changed by this document. No API, database schema, hashing
algorithm, or storage technology is specified. Neither `src/` nor
`tests/` is touched. Nothing is staged, committed, or pushed.

**Ratification status:** Accepted and frozen following independent
constitutional verification and Final Freeze Verification
(`docs/reviews/EVIDENCE_CUSTODIAN_CDR-006_FINAL_FREEZE_REVIEW.md`,
1 August 2026).

This document accepts `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
("CDR-006") as the controlling, already-decided constitutional basis for
everything below and does not reopen it. CDR-006's Model B — Evidence
Intelligence Custody — is restated here only as the constraint this
design must satisfy, not re-argued. In particular, this document does
not revisit: whether Memory Core or the Evidence Custodian holds
artefact custody (decided: the Custodian); whether the Custodian may
determine legal ownership, copyright, or proprietary interest (decided:
never); or whether Memory Core's Document Contract or Scope Lock §4's
exclusion of Document Handling/OCR/PDF parsing/image analysis should be
reopened (decided: no).

**Revised at this stage.** This document now treats the Evidence
Custodian as a first-class Parker infrastructure subsystem — a peer of
Memory Core, not a component of Evidence Intelligence — with Evidence
Intelligence repositioned as a separate, downstream analytical consumer
of custodied artefacts. §6.3 explains this in full, together with why it
does not reopen or contradict CDR-006. This document also now names an
optional, conceptual **Archived** lifecycle stage (§7), added without
weakening any preservation obligation.

**The constitutional principle governing every responsibility below,
restated verbatim and applied literally:** *the owner owns evidence. The
Evidence Custodian provides technical custody only — it preserves the
artefact, protects it from modification, maintains its provenance
relationships, and enforces its immutability while retained. It does not
reason about the artefact, does not interpret it, and does not determine
who legally owns it.* (CDR-006, Context.)

**No language syntax is specified anywhere in this document.** Every
responsibility, boundary, and lifecycle stage below is described by
purpose, obligation, and relationship — never by Kotlin type, interface,
enum, or method signature — mirroring `MEMORY_CORE_CONTRACT_DESIGN.md`'s
own explicit "implementation independence" discipline, applied here for
the same reason: this Programme is not yet at the stage where a
technology choice may safely be made.

---

## 1. Executive Summary

CDR-006 decided that a future Evidence Custodian — not Memory Core —
holds technical custody of preserved original evidence artefacts, and
that this custody is never a legal-ownership determination. This document
is the next required governance stage: it defines the Evidence
Custodian's constitutional contract — what it is responsible for, what it
is explicitly not responsible for, how it interacts with every adjacent
subsystem, and the conceptual lifecycle a custodied artefact passes
through — without specifying how any of it is built.

The Evidence Custodian is positioned explicitly as a **first-class
Parker infrastructure subsystem**, architecturally comparable to Memory
Core, rather than a component belonging to Evidence Intelligence.
Evidence Intelligence — whatever future analytical, OCR, or interpretive
capability that name eventually denotes — is a separate, downstream
consumer that requests authorised access to what the Custodian holds; it
never acquires custody authority merely by consuming custodied evidence.
This document also adds an optional, purely conceptual **Archived** stage
to the artefact lifecycle, distinguishing evidence that remains fully
preserved but is no longer expected to participate in routine operational
workflows, without altering, weakening, or creating any exception to
Article IX's preservation obligations.

This document exists so that a future Evidence Custodian Scope Lock and
Implementation Plan have a single, disclosed architectural contract to
implement against, exactly as Memory Core's own Contract Design preceded
its Scope Lock and Implementation Plan.

---

## 2. Constitutional Basis

- **CDR-006** — controlling. Establishes Model B (Evidence Intelligence
  Custody), the custody/legal-ownership distinction, the
  separate-identity requirement, the Constitutional Optimisation
  Safeguard, and the "Derived Work Product is never evidence" rule. Every
  section below traces back to a specific CDR-006 Decision Rule. §6.3
  addresses, and does not silently resolve, one point of textual tension
  in CDR-006's own wording — CDR-006 itself has since been clarified to
  state that "Evidence Intelligence" and "Document Handling" are
  provisional labels only, and that the Custodian's precise
  organisational position is left to this document.
- **Epistemic Integrity, Article IX (Integrity of Evidence), as
  amended** — the absolute custody-preservation obligation and the
  Constitutional Optimisation Safeguard's binding constitutional text.
  Article XIX already extends these obligations to "Evidence
  Intelligence" and "Document Intelligence" by name. This document reads
  Article XIX as binding *whichever* subsystem eventually performs
  custody and *whichever* subsystem eventually performs analysis — it
  does not read Article XIX as requiring those to be the same subsystem,
  and this document's own separation (§6.3) is consistent with, not
  contrary to, that reading.
- **User Authorship and Evidence** — governs cognition's treatment of
  evidence when drafting; this document's Non-Responsibilities section
  (§5) is written to keep that document's evidence/inference/opinion
  distinction intact by keeping the Custodian entirely out of the
  reasoning layer that document governs.
- **Memory Core Contract Design §5 (Document), §7 (Provenance), §12
  (Immutability); Memory Core Scope Lock §4** — Memory Core's
  registration-only boundary and its explicit exclusion of Document
  Handling/OCR/PDF parsing/image analysis, which this document is
  designed to satisfy, not reopen.
- **Programme 3 Knowledge Memory Contract Design V2 and Scope Lock** —
  "Derived knowledge is not evidence" and "Knowledge Memory reads Memory
  Core; it never writes to it" are both reaffirmed, unaltered, and relied
  upon in §6.4 below.
- **CDR-001 through CDR-005** — precedent only, for drafting discipline
  (technology independence; explicit Non-Decisions; escalation-on-genuine-doubt).
  None of their own subject matter is reopened.

---

## 3. Purpose and Scope

The Evidence Custodian is a **first-class Parker infrastructure
subsystem** — architecturally a peer of Memory Core, not a component,
module, or sub-capability of Evidence Intelligence or any other
analytical layer. Its purpose is narrow and entirely technical: it holds
technical custody of a preserved original evidence artefact — a PDF, an
email, a screenshot, a photograph, an audio recording, a video recording,
or metadata accompanying any of these — from the point Parker accepts it
into custody until either it remains retained indefinitely or the owner
authorises its deletion. Preserve, protect, and make traceable — never
reason, interpret, or adjudicate.

This document does not create a new umbrella platform, programme, or
constitutional hierarchy grouping the Evidence Custodian together with
Memory Core, Evidence Intelligence, or anything else — no "Evidence
Platform" is invented here merely for organisational tidiness. The
Evidence Custodian is simply its own, independently governed
infrastructure subsystem, exactly as Memory Core and Knowledge Memory are
already each their own, independently governed subsystem today without
requiring a shared umbrella label.

This document is in scope for: the Custodian's required responsibilities
(§4); what it must never do (§5); its boundaries with every adjacent
subsystem, including its explicit separation from Evidence Intelligence
(§6); the conceptual lifecycle a custodied artefact passes through,
including the optional Archived stage (§7); and the constitutional
guarantees this contract, once implemented, must uphold (§8).

This document is out of scope for anything listed in §10 (Out of Scope),
most importantly: no storage technology, API, database schema, hashing
algorithm, or Kotlin type is chosen or implied anywhere below.

---

## 4. Required Responsibilities

**Accepting evidence into custody.** The Custodian is responsible for
the act of taking a submitted artefact into preserved custody, following
an authorised acceptance decision (§6.6, Permission Engine). Acceptance
is the only point at which an artefact's status changes from
not-yet-custodied to custodied; the Custodian does not accept evidence
implicitly, silently, or as a side effect of any other operation,
including any request originating from Evidence Intelligence (§6.3).

**Maintaining immutable preserved originals.** Once accepted, the
Custodian is responsible for ensuring the original artefact's content
can never be modified, overwritten, replaced, or obscured for as long as
it remains retained — the absolute, unconditional obligation Article IX
(as amended) states in binding constitutional terms, and which this
document does not restate as optional or practicability-qualified in any
circumstance, including where the artefact has been designated Archived
(§7).

**Maintaining technical custody.** The Custodian is responsible for the
artefact's continued availability and integrity for as long as it
remains retained — a distinct responsibility from immutability
(unchanged content) and closer to durability (continued existence and
resolvability). Both responsibilities are held together, never
separately, since a durably-stored but silently-corrupted artefact would
satisfy neither Article IX nor this contract.

**Preserving provenance relationships.** The Custodian is responsible for
ensuring that whatever provenance information Memory Core's Provenance
contract requires at registration (source, acquisition time, content
nature, and — where the Custodian can supply one — an integrity
verifier) remains accurate and available for as long as the artefact it
describes remains custodied. The Custodian does not itself own or write
Memory Core's Provenance records (§6.2); it is responsible only for not
allowing custody-side facts underlying those records to silently drift
from what Memory Core was told at registration.

**Supporting derivative artefact generation.** The Custodian is
responsible for making a custodied original available, on authorised
request — including a request from Evidence Intelligence acting only as
a consumer, never as a custody authority (§6.3) — as the input to a
derivative-generating process (OCR, transcription, extraction,
thumbnailing, summarisation, or any other transformation), without
itself performing that transformation (§5), and without that support
ever taking the form of handing over the original for modification in
place.

**Preserving traceability.** The Custodian is responsible for ensuring
that any derivative artefact generated from a custodied original can be,
and remains, linked back to that original via Memory Core's existing
Provenance mechanism (`derivedFromReferences`/`extractedFromReference`) —
supporting, not replacing, Memory Core's own ownership of that
mechanism.

**Preserving evidence identity.** The Custodian is responsible for
ensuring a custodied original's identity is never reassigned, merged
with, or confused with any derivative artefact's identity, regardless of
how faithfully that derivative reproduces the original's content
(CDR-006's separate-identity requirement).

**Supporting authorised deletion requests.** The Custodian is
responsible for executing an owner-authorised, audited deletion of a
custodied original when instructed to do so through the same gated
mechanism Memory Core Scope Lock §8 already establishes for its own
`DELETED` status — never on its own initiative, never as a consequence
of any other subsystem's convenience, and never on Evidence
Intelligence's own request or authority (§6.3, §6.6, §8).

**Refusing unauthorised modification.** The Custodian is responsible for
refusing — structurally, not merely by policy — any request, from any
subsystem, reasoning provider, or process, to modify, overwrite,
replace, or obscure a custodied original outside the single authorised
deletion path above. Refusal is the Custodian's default posture, not an
exception it must be separately configured to apply.

**Enforcing the Constitutional Optimisation Safeguard.** The Custodian
is responsible for refusing any request — including one originating from
Evidence Intelligence, and including one it might itself be asked to
originate — to destroy, alter, replace, or lose a preserved original on
the grounds that a derivative artefact (OCR output, a transcript, an
extraction, a summary, an embedding, a normalised copy) is more efficient
to store, process, or retrieve. This applies without exception to the
Custodian's own future implementation, which must never be built with a
general "optimise storage by discarding the original" capability of any
kind.

---

## 5. Explicit Non-Responsibilities

The Evidence Custodian does **not**:

- **Reason.** Reasoning about what an artefact means, implies, or
  supports is cognition's responsibility (Parker Constitution:
  "cognition proposes"), never the Custodian's.
- **Analyse evidence.** Evidentiary analysis — weighing, comparing,
  cross-referencing — belongs to Evidence Intelligence, a separate,
  downstream subsystem under its own future governance (§6.3); the
  Custodian never performs it and Evidence Intelligence never performs
  custody.
- **Generate conclusions.** Conclusions are a reasoning-layer output,
  governed by Epistemic Integrity Articles II–VII; the Custodian
  produces no representation of what an artefact shows.
- **Create Knowledge Items.** `KnowledgeItem` creation belongs exclusively
  to Knowledge Memory's own, already-frozen promotion path (Programme 3,
  Unit 6); the Custodian has no promotion capability and no write access
  to Knowledge Memory.
- **Perform memory retrieval.** `MemoryRetrieval` is Memory Core's own
  interface; the Custodian neither implements nor is implemented on top
  of it — it is a distinct subsystem, decoupled from Memory Core by
  reference only (§6.1), not a caller of Memory Core's retrieval
  contract.
- **Determine legal ownership.** Per CDR-006, legal ownership, copyright,
  and proprietary interest are never determined, asserted, or required as
  a precondition of custody by the Custodian or by any document
  governing it.
- **Determine evidential weight.** Weight is an evidential-classification
  concept (Article IV, Programme 3's `EvidentialState`); the Custodian
  assigns none.
- **Determine truth.** Truth-of-content determination is expressly
  outside cognition's own authority (Epistemic Integrity, Article III);
  it is further outside the Custodian's, which sits entirely outside the
  reasoning layer.
- **Determine authenticity.** Authenticity — whether an artefact
  genuinely originates from its purported source (Article IX) — is an
  evidentiary judgment for Evidence Intelligence's own future governance,
  not a custody function.
- **Perform OCR.** Optical character recognition remains, exactly as
  Memory Core Scope Lock §4 already excludes it, an Evidence Intelligence
  analysis capability — architecturally downstream of, and never
  identical with, the Custodian (§6.3).
- **Summarise evidence.** Summarisation produces Derived Work Product
  (§6.5); the Custodian holds what is summarised, never performs the
  summarisation.
- **Interpret evidence.** Interpretation of any kind — linguistic,
  visual, or contextual — is excluded for the same reason analysis and
  OCR are: it is Evidence Intelligence's own, separately governed
  responsibility, never the custody layer's.

Each of these remains, following this exclusion, the responsibility of
whichever subsystem this document names in the corresponding bullet
above — this document assigns none of them to the Custodian by omission,
implication, or convenience. Conversely, and equally important: none of
these capabilities, wherever they are eventually governed, ever confers
custody authority, modification capability, or deletion authority over
the artefacts they analyse (§6.3).

---

## 6. Contract Boundaries

```text
Evidence Custodian
        |
        v
Evidence Intelligence
        |
        v
Knowledge and Derived Work Product
```

This diagram states a consumption relationship, not a containment or
call-authority one: Evidence Intelligence consumes what the Custodian
holds, under its own separate authorisation and governance, and
Knowledge Memory / Derived Work Product in turn consume what Evidence
Intelligence produces. It does not depict Memory Core, whose own
reference-based relationship to the Custodian is unchanged and is
described fully in §6.1; the two relationships coexist and neither
supersedes the other.

### 6.1 Memory Core

Memory Core's Document Contract (§5) and Provenance Contract (§7) are
unchanged by this document, exactly as CDR-006 requires. The relationship
is reference-based, not call-based: whoever accepts an artefact into the
Custodian's custody receives back a stable reference; that reference is
what a Memory Core `Document.locationReference` names when a Document
record is subsequently registered. The Custodian does not call Memory
Core, and Memory Core does not call the Custodian — precisely mirroring
Memory Core's own existing disclosure that it "never fetches, opens, or
validates" a Document's location reference. This keeps the two
subsystems decoupled: Memory Core continues to know *that* a document
exists and *where* it can be found; the Custodian is what actually holds
it there. Both Memory Core and the Evidence Custodian are first-class
infrastructure subsystems standing at the same architectural tier —
neither contains, nor is contained by, the other.

### 6.2 Provenance

The Custodian never owns, writes, or maintains a Memory Core Provenance
record. It is responsible for keeping the underlying custody-side facts
(existence, integrity, availability) consistent with whatever Provenance
was told at registration (§4), and for supplying an integrity verifier
where it can — but the Provenance record itself, its `contentNature`,
`derivedFromReferences`, and `extractedFromReference` fields, remain
exclusively Memory Core's own contract, unamended.

### 6.3 Evidence Intelligence

**The Evidence Custodian is not part of Evidence Intelligence and is not
an intelligence capability.** This is a first-class architectural
decision, distinguishing the Custodian's role from any framing that would
treat it as a component "within" a future Evidence Intelligence
Programme.

Evidence Intelligence is a separate, downstream subsystem that may
request authorised access to artefacts held by the Evidence Custodian.
Evidence Intelligence may analyse, compare, extract, interpret, or reason
from evidence only under its own future governance — a Contract Design
and Scope Lock this document does not draft, define, or presume the
shape of. Evidence Intelligence does not inherit custody authority merely
because it consumes custodied artefacts. Specifically, and without
exception:

- **Evidence Intelligence cannot modify preserved originals.** Every
  Article IX obligation and every Required Responsibility in §4 applies
  regardless of which subsystem is requesting access.
- **Evidence Intelligence cannot authorise deletion.** Deletion remains
  gated exclusively by owner-authorised, audited, Permission-Engine-mediated
  request (§4, §6.6); Evidence Intelligence has no standing to request,
  approve, or trigger it on the artefact's behalf.
- **Evidence Intelligence cannot bypass the Permission Engine.** Every
  access Evidence Intelligence makes to a custodied artefact is itself a
  proposal requiring Permission Engine authorisation, exactly as any
  other subsystem's access would be (§6.6) — Evidence Intelligence
  receives no standing exemption by virtue of being the artefact's
  primary intended consumer.
- **Evidence Intelligence cannot assume the authority of the Evidence
  Custodian.** Custody, immutability enforcement, and deletion
  authorisation remain exclusively the Custodian's; no future Evidence
  Intelligence design may be built to perform any of them directly,
  even where doing so would appear more efficient.
- **Every retrieval outcome preserves the identity of the artefact
  requested, including a denied one.** Whatever the Custodian returns in
  response to a request naming a specific artefact identifies that
  artefact throughout: a successful, authorised read returns the
  requested identifier together with the retrieved content; an
  authorised request for an identifier nothing exists under returns the
  requested identifier alone; and a denied request returns the requested
  identifier together with the existing plain-language reason for
  denial. Retrieval remains exactly the three outcomes already governed
  above (a successful read, an authorised request for a nonexistent
  artefact, and a denial), and this creates no batch-retrieval contract
  of any kind — a request still concerns exactly one artefact
  identifier.
- **The identifier accompanying a denied outcome carries no new
  constitutional weight of its own.** It identifies only the artefact
  whose retrieval was denied, and it echoes an identifier the requesting
  caller already supplied to make the request — disclosing nothing the
  caller did not already possess. It grants no access to the artefact
  and discloses no evidence content of any kind; it changes no
  Permission Engine decision — the request remains denied, exactly as
  before; and it creates no custody, retrieval, deletion, acceptance,
  storage, or ownership authority of any kind, for Evidence Intelligence
  or any other consumer. This introduces no alternate retrieval
  taxonomy, and nothing about the successful-read or nonexistent-artefact
  outcome changes.
- **Membership in a later, broader programme grouping, if the repository
  is ever organised that way, does not collapse this boundary.** Should
  a future document choose to group the Evidence Custodian and Evidence
  Intelligence under a shared programme label for organisational
  convenience, that grouping is administrative only and does not, by
  itself, grant Evidence Intelligence any custody authority or exempt it
  from any constraint stated above. This document does not create such a
  grouping and does not require one to exist.

The Custodian, correspondingly, never analyses, interprets, or reasons
about what it holds (§5) — the separation is symmetric: the Custodian
does not become an analytical capability by holding evidence Evidence
Intelligence will eventually consume, exactly as Evidence Intelligence
does not become a custodian by consuming it.

### 6.4 Knowledge Memory

Unchanged. Knowledge Memory continues to read only Memory Core, never
the Custodian directly, and never writes to Memory Core (Programme 3
Knowledge Memory Scope Lock, already frozen). The Custodian is invisible
to Knowledge Memory: Knowledge Memory's own `KnowledgeItem.evidenceReference`
resolves through Memory Core exactly as it already does today, regardless
of where the underlying artefact bytes a Memory Core `Document` describes
actually live.

### 6.5 Derived Work Product

Any report, chronology, summary, witness-statement draft, or briefing
paper (Derived Work Product, per CDR-006, "never evidence") that
ultimately traces a factual claim back to a custodied original does so
through the same Memory Core Provenance chain every other derived
representation already uses — never through a direct, bypassing request
to the Custodian for "convenience," and never through Evidence
Intelligence asserting custody-level access on Derived Work Product's
behalf. This preserves CDR-006's mandatory traceability rule as a single,
uniform chain rather than two parallel ones.

### 6.6 Permission Engine

Every custody-changing act — accepting an artefact into custody,
authorising its deletion — is a proposal that must pass through Permission
Engine authorisation before the Custodian executes it, exactly as the
Constitution's "no path from proposal to execution that does not pass
through the Permission Engine" requires. The Custodian never
self-authorises either act, and neither does Evidence Intelligence on the
Custodian's behalf (§6.3). By contrast, the Custodian's *refusal* of an
unauthorised modification request (§4) requires no new permission
decision each time — refusal is the Custodian's structural default
posture, not an action Permission Engine must separately approve on
every occurrence; only the two affirmative acts above are gated
proposals.

---

## 7. Lifecycle

The following are conceptual stages an evidence artefact's relationship
with the Custodian passes through — descriptive categories for this
document's own reasoning, not a prescribed state machine, enum, or
implementation-level status field. Stages are not necessarily exclusive
or strictly sequential; several may be true of an artefact at once.

- **Accepted.** An artefact has been submitted and an authorised
  acceptance decision has been made (§6.6); the Custodian has taken it
  into preserved custody.
- **Preserved.** The artefact's content is held immutable, exactly as
  accepted, for as long as this stage continues — the enduring, default
  condition of any accepted artefact.
- **Referenced.** A Memory Core `Document` record (or records) names this
  artefact via a location reference; other Memory Core records
  (`Provenance`, `Relationship`) may in turn reference that Document.
  An artefact may be referenced by zero, one, or many records at once,
  and referencing is never itself a custody-changing act.
- **Derived.** One or more derivative artefacts (an OCR output, a
  transcript, an extract, a summary, a thumbnail) have been generated
  from this original, each with its own separate identity and its own
  Provenance-chain link back to this original (§4, §6.2). An artefact may
  have many derivatives, one, or none, and having derivatives never
  changes the original's own preserved status.
- **Retained.** The artefact continues to be held in custody
  indefinitely, absent an owner-authorised deletion — the expected,
  ordinary continuation of the Preserved stage, named separately here
  only to make explicit that retention is the default and requires no
  renewed authorisation to continue.
- **Archived (optional, conceptual).** A custodied original remains
  preserved, immutable, traceable, and retrievable, but is designated as
  inactive for ordinary operational use. Archival does not alter the
  artefact, remove it from custody, break provenance, authorise
  deletion, or reduce any constitutional preservation obligation. This
  stage is:
  - **optional** — an artefact may remain Retained indefinitely without
    ever being designated Archived; nothing in this document requires
    archival to occur at any point;
  - **purely conceptual** — it names a category for this document's own
    reasoning, not an implementation state, storage tier, Kotlin enum, or
    database field;
  - **compatible with continued preservation** — every obligation
    attached to Preserved and Retained above continues to apply,
    unmodified, to an artefact designated Archived;
  - **not a deletion precursor** — designating an artefact Archived
    carries no implication, expectation, or default trajectory toward
    deletion; the only path to non-custody remains Owner-authorised
    deletion, below, entirely independent of archival status.

  This document does not define when archival occurs, who may request
  it, or what mechanism realises it — these are future governance
  decisions, not answered here, and no existing constitutional material
  reviewed for this document answers them either. What this document
  does fix, permanently, is the boundary any future answer must respect:
  archival is a labelling concept layered over continued preservation, never
  a relaxation of it.

- **Owner-authorised deletion.** An explicit, authorised, and audited
  deletion request (§4, §6.6) has been executed; the artefact is no
  longer custodied. This is the sole path by which a custodied original's
  preserved status ends, mirroring Memory Core Scope Lock §8's own
  `DELETED`-is-terminal, owner-erasure-only precedent, and applies
  identically whether or not the artefact was ever designated Archived.

No implementation state, Kotlin enum, or status field is defined or
implied by the stages above; a future Scope Lock or Implementation Plan
is free to represent them however best fits whatever storage mechanism
is eventually chosen.

---

## 8. Constitutional Guarantees

Once implemented per this contract, the Evidence Custodian must be
capable of guaranteeing:

- An original evidence artefact's content, once accepted, is never
  modified, overwritten, replaced, or obscured while retained (Article
  IX, as amended; CDR-006) — including while designated Archived.
- No derivative artefact, however accurate or convenient, is ever
  substituted for, or used to justify discarding, the original it was
  derived from (the Constitutional Optimisation Safeguard).
- An original and every derivative produced from it remain separately
  identified, never merged (CDR-006's separate-identity requirement).
- Every material factual representation in Derived Work Product remains
  traceable, through Memory Core's existing Provenance mechanism, back to
  the preserved original it relies upon (CDR-006's mandatory
  traceability rule).
- Deletion of a custodied original is available only through an
  explicit, authorised, and audited request, gated by Permission Engine,
  and is never conditioned on a determination of legal ownership
  (CDR-006).
- No subsystem — including the Custodian itself — ever determines legal
  ownership, copyright, or proprietary interest in a custodied artefact.
- Evidence Intelligence's consumption of a custodied artefact never
  grants it custody authority, modification capability, or deletion
  authority over that artefact, regardless of how the two subsystems are
  organisationally grouped in the future (§6.3).
- Designating an artefact Archived never authorises deletion, replacement,
  reduced integrity protection, severed provenance, or any relaxation of
  the guarantees above; it is a label over continued preservation, never
  an exception to it (§7).

---

## 9. Contract Inventory

| Concern | Owned by | This document's role |
| --- | --- | --- |
| Artefact custody, immutability enforcement | Evidence Custodian (first-class infrastructure) | Defines (this document) |
| Document registration, location reference | Memory Core (Document Contract) | Unchanged, relied upon |
| Provenance record, derivation chain | Memory Core (Provenance Contract) | Unchanged, relied upon |
| Evidence analysis, OCR, interpretation | Evidence Intelligence (separate, downstream, own future governance) | Boundary defined (§6.3); no capability, authority, or containment relationship granted |
| Knowledge Item promotion/revision/retirement/restoration | Knowledge Memory (Programme 3) | Unaffected, unchanged |
| Derived Work Product generation | A future, separate capability | Traceability obligation defined (§6.5); generation itself not defined here |
| Permission gating of acceptance/deletion/analytical access | Permission Engine | Boundary defined (§6.6); gating logic itself not defined here |
| Archival designation | Not assigned by this document | Concept and safeguards defined (§7); mechanism and authority to designate deferred |

---

## 10. Out of Scope

This document does not define, and no future reader should treat it as
having defined:

- any storage technology, file system, database, or object store;
- any hashing algorithm, integrity-verification scheme, or cryptographic
  method;
- any API, RPC contract, or wire format;
- any Kotlin interface, class, enum, or method signature;
- any database schema or persistence model;
- a Scope Lock or Implementation Plan for the Evidence Custodian — both
  remain the next, separate governance stages;
- the shape, governance, or internal structure of Evidence Intelligence
  or any other analytical capability (analysis, OCR, interpretation,
  summarisation) — only its boundary with the Custodian (§6.3);
- any amendment to Memory Core's Document or Provenance contract;
- any question of legal ownership, copyright, proprietary interest, or
  lawful possession of any artefact (per CDR-006, permanently out of
  scope for this entire Programme);
- when archival occurs, who may request it, or what mechanism realises
  it (§7) — deferred to future governance;
- whether the Evidence Custodian and Evidence Intelligence are ever
  grouped under a shared programme label — an organisational question
  this document neither decides nor requires an answer to.

---

## 11. Verification

**Satisfies CDR-006.** Every Required Responsibility (§4) and Explicit
Non-Responsibility (§5) traces to a named CDR-006 Decision Rule; the
custody/legal-ownership distinction, the separate-identity requirement,
the Constitutional Optimisation Safeguard, and "Derived Work Product is
never evidence" are each restated and operationalised, never
reinterpreted or narrowed. CDR-006's Model B (Memory Core =
registration/provenance; Custodian = custody/enforcement) is preserved
exactly (§6.1, §9). CDR-006 has itself been clarified to confirm that
this document, not CDR-006, decides the Custodian's precise
organisational position relative to Evidence Intelligence (§6.3).

**Preserves Memory Core Scope Lock.** §6.1 and §6.2 confirm no amendment
to Memory Core's Document or Provenance contract; Scope Lock §4's
exclusion of Document Handling/OCR/PDF parsing/image analysis is
reaffirmed by §5 and §6.3, which place every one of those capabilities
outside the Custodian and outside this document's own scope.

**Introduces no constitutional conflicts.** §6.6 confirms every
custody-changing and analytical-access act remains Permission-Engine-gated,
consistent with the Constitution's no-bypass principle; §4 and §7's
deletion handling mirrors Memory Core Scope Lock §8's own owner-erasure
precedent without narrowing it; §5 keeps reasoning, analysis,
truth-determination, and authenticity-determination entirely outside the
Custodian, consistent with Epistemic Integrity's cognition/representation
separation and the Constitution's "cognition proposes" principle; no
legal-ownership determination is required or implied anywhere in this
document, consistent with CDR-006's own clarification.

**Remains implementation independent.** No storage technology, hashing
scheme, API, schema, or Kotlin type appears anywhere in this document
(§10); every responsibility, boundary, and lifecycle stage — including
Archived — is described by purpose and relationship only, following
`MEMORY_CORE_CONTRACT_DESIGN.md`'s own explicit discipline for the same
reason.

---

## 12. Required Review Questions

**1. Is the Evidence Custodian now clearly independent from Evidence
Intelligence?** Yes. §3 and §6.3 state this as a first-class
architectural decision: the Custodian is a peer infrastructure subsystem,
not a component, module, or sub-capability of Evidence Intelligence.

**2. Can Evidence Intelligence consume evidence without acquiring
custody authority?** Yes. §6.3 states this explicitly and enumerates five
specific things Evidence Intelligence cannot do (modify preserved
originals, authorise deletion, bypass the Permission Engine, assume the
Custodian's authority, or acquire authority merely through future shared
programme grouping).

**3. Does the Archived concept preserve every Article IX obligation?**
Yes. §7 and §8 state that Archived status changes nothing about
immutability, protection, provenance, or retrievability — it is a label
over continued preservation, and every Article IX obligation continues
to apply identically to an Archived artefact.

**4. Could Archived be mistaken for deletion, replacement, expiry, or
reduced-integrity storage?** No, by explicit design: §7 states directly
that archival does not mean deletion, does not authorise replacement,
does not sever Memory Core references or provenance, does not permit
lower integrity standards, and creates no expiry or retention-policy
authority for any subsystem, including Evidence Intelligence.

**5. Does the revised design remain technology independent?** Yes. §10
excludes storage technology, APIs, schemas, hashing algorithms, and
Kotlin types exactly as before; the Archived stage and the Custodian/Evidence
Intelligence separation are both stated in purpose-and-relationship terms
only, with no implementation state or enum introduced (§7).

**6. Does any wording require a change to CDR-006 or the Article IX
amendment?** No further change to either is required. CDR-006 has
already been updated to state that "Evidence Intelligence" and "Document
Handling" are provisional labels only, leaving the Custodian's precise
organisational position to this document (see CDR-006, Decision Rules,
"Who holds custody"). Article IX's amended text requires no change: it
already describes the preservation obligation in terms of "original
evidence... while it remains retained," language that already
accommodates an Archived artefact without modification, since Archived
evidence remains, by this document's own definition, retained.

---

## Final Recommendation

This Contract Design was presented for constitutional review and
accepted, per the Evidence Custodian CDR-006 Final Freeze Review
(`docs/reviews/EVIDENCE_CUSTODIAN_CDR-006_FINAL_FREEZE_REVIEW.md`,
1 August 2026). The next governance stages, an Evidence Custodian Scope
Lock followed by an Implementation Plan — neither begun, nor authorised
to begin, by this document itself — have since each been separately
drafted and ratified.

EVIDENCE ARTIFACT CONTRACT DESIGN (PHASE 1) — ACCEPTED — INDEPENDENT
CONSTITUTIONAL VERIFICATION COMPLETE

Confirmed: no Kotlin implemented; no API, schema, or storage technology
defined; no lifecycle enum or implementation state defined; Memory Core
Contract Design and Scope Lock unmodified; Knowledge Memory governance
unmodified; nothing staged; nothing committed; nothing pushed.
