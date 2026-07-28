# Memory Core — Contract Design

## Status

Programme: **Programme 2 — Memory Core Contract Design.**
Phase: **Governance and design only.** No Kotlin is implemented, proposed
as a diff, or changed by this document. Neither `src/` nor `tests/` is
touched. Nothing is staged, committed, or pushed.

This document accepts `docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md`'s
conclusion (`READY FOR MEMORY CORE CONTRACT DESIGN`) as frozen and does
not reopen it. The layering it froze is restated here only as a
constraint this design must satisfy, not re-argued:

```
Owner
  │
  ▼
Conversation History
  │
  ▼
Memory Core
  │
  ▼
Knowledge Memory
  │
  ▼
Reasoning Context

World Model
  │
  └──────────────► Reasoning Context
```

**The constitutional principle governing every contract below, restated
verbatim and applied literally, not merely cited:** *Memory Core is a
system of record, not a system of belief. It preserves what Parker has
observed, received or derived together with its provenance. Knowledge
Memory forms durable knowledge from those records. Storage does not
create truth, and reasoning does not overwrite history.*

Every design choice in this document is checked against that sentence
directly. Where a choice is not obvious from the sentence alone, this
document says so and states its own reasoning, rather than asserting
conformance without showing it.

**No language syntax is specified anywhere in this document.** Every
contract below is described by responsibility, required fields (named
and typed in prose, not Kotlin), relationships, and lifecycle — exactly
as this Unit's own instruction requires for the Entity Contract
specifically, and applied here as a document-wide discipline, in service
of the required "implementation independence" and "technology
independence" principles. This is a deliberate departure from
`MEMORY_CONTRACT_DESIGN.md`'s own style, which did include Kotlin
snippets in places — that precedent is not followed here, on this Unit's
own explicit instruction.

---

## 1. Executive Summary

Memory Core is the layer of Parker's Memory family that answers one
question, and only one: **what has Parker observed, received, or
derived, and where did it come from?** It is the **authoritative system
of record** for that question — not an index, not a cache, and not a
belief store. Nothing else in this repository is authoritative for that
question today; the Governance Review found the closest existing
candidate (`MemoryRecord`'s scattered provenance fields) explicitly
insufficient for it.

Memory Core does not decide what is true, does not decide what is worth
remembering long-term, and does not decide who may see what it holds.
Those are, respectively, an Assertion's own bounded truth-status claim
(never Memory Core's own verdict), Knowledge Memory's job (per the
Reconciliation's own layering), and the Permission Engine's job (Section
10). Memory Core's own single responsibility is narrower and more
foundational than any of those: identify, record, preserve, and make
retrievable, with mandatory provenance, everything Parker has genuinely
encountered — never mutating that record of encounter once made, no
matter what is later reasoned about it.

This document designs six contracts the prior Governance Review already
named as the correct first scope (Entity, Document, Assertion,
Provenance, Relationship, MemoryRetrieval), plus one further contract
this document adds and discloses explicitly, not silently: a single
public write interface (**`MemoryCore`**) through which every creation,
amendment, and status transition described in Sections 10–12 actually
occurs. The prior documents named the record and retrieval shapes but
did not name the operation through which a caller submits to them; this
document does not leave that operation undesignated, following the same
"one public interface" precedent `MemoryStore`/`WorldModel`/`ToolRegistry`
already established elsewhere in this repository.

---

## 2. Architectural Responsibilities

**What Memory Core owns, exactly:**

- The identity and structural existence of an Entity, a Document
  registration, an Assertion, a Provenance record, and a Relationship
  between any two Memory-Core-addressable records.
- Mandatory provenance capture and enforcement — no record of any of the
  five kinds above may exist without a valid, referenced Provenance
  record. This is the one universal invariant this design treats as
  non-negotiable, per the Reconciliation's own Section 11 mandate.
- The lifecycle and status of its own records (Section 11), and the
  immutability of their core content once created (Section 12).
- Structural retrieval of its own records (Section 9) — never ranked,
  scored, or semantic retrieval.

**What Memory Core does not own, stated against each named adjacent
subsystem specifically, so no overlap is left implicit:**

- **Conversation.** Memory Core never constructs, mutates, or duplicates
  a `Conversation` or `Turn`. It may *reference* a `ConversationId`/
  `TurnId` as a Provenance source (Section 7), exactly the same
  loose-coupling-by-identifier already established for `correlationId`
  elsewhere in this repository — never a structural dependency, never a
  copy of Conversation content into a Memory Core record.
- **Knowledge Memory.** Memory Core never evaluates, promotes, or
  decides what is worth durable retention. It never constructs a
  `KnowledgeRecord`-equivalent, and it never reads Knowledge Memory's own
  state — the dependency runs one way only (Knowledge Memory depends on
  Memory Core, never the reverse, per the Reconciliation's Section 5/9
  determination). Memory Core also does not weigh or resolve conflicting
  Assertions — it preserves them, unresolved, side by side (Section 14).
- **World Model.** Memory Core has no relationship to the World Model at
  all, in either direction — no read, no write, no shared type. This is
  not a gap; it is the Reconciliation's own Section 10 finding, restated
  here as a hard boundary this design must not cross.
- **The Permission Engine.** Memory Core never evaluates a permission
  decision itself (Section 10). It carries enough structure — a
  sensitivity classification on Provenance (Section 7) — for an external
  Permission Engine to evaluate against, and nothing more.
- **Document Handling** (a distinct, later Programme). Memory Core
  registers that a Document exists and where it is; it never fetches,
  parses, OCRs, or interprets the Document's own contents (Section 5).
- **Evidence, semantic retrieval, learning, workflow integration.** Named
  explicitly in this Unit's own exclusions; not designed anywhere below,
  and not silently anticipated through some other contract's field
  either (checked in Section 19).

---

## 3. Core Contracts

Seven contracts, six named directly by this Unit's brief plus one
disclosed addition:

| Contract | Kind | Responsibility |
| --- | --- | --- |
| `Entity` | Record | Identifies a durable "who/what" — a person, organisation, place, or thing Parker has encountered. |
| `Document` | Record | Registers that a source document exists and where it can be found. Never its parsed contents. |
| `Assertion` | Record | Records a claim, with a bounded truth-status, never an automatic verdict. |
| `Provenance` | Record | The mandatory, first-class origin record every other Memory Core record must reference. |
| `Relationship` | Record | The one, single mechanism connecting any two Memory-Core-addressable records — including across the Conversation and future Knowledge Memory boundary, by identifier only. |
| `MemoryRetrieval` | Read-only interface | The narrow, structural, non-semantic read boundary onto everything above. |
| `MemoryCore` | Write interface (this document's own disclosed addition, Section 1) | The single public interface through which every creation, amendment, dispute, supersession, archival, and owner-erasure act described in Sections 10–12 is actually invoked. |

**One cross-cutting design decision, stated once here because it applies
to every contract below and would otherwise need repeating five times:
no Memory Core record type carries its own ad hoc "source," "origin," or
"related-record" field.** Provenance is the one, single place origin
information lives (Section 7); Relationship is the one, single place
any connection between two records lives (Section 8). This directly
answers several of this Unit's own per-contract field lists (Document's
"relationships," Assertion's "source"/"supporting references"/
"contradicting references") without duplicating a scattered-fields
pattern the Governance Review already found and criticised in today's
`MemoryRecord` (`sourceSubsystem`, `correlationId`,
`originatingPrincipalId`, `relatedMemoryIds` — four separate, untyped
provenance/relationship approximations on one type). Each section below
states explicitly how it satisfies its own brief-literal field list
through this shared mechanism, rather than leaving the mapping implicit.

**A second cross-cutting decision: sensitivity lives on `Provenance`,
not on `Entity`/`Document`/`Assertion`/`Relationship` individually.**
Every record's effective sensitivity is its referenced Provenance's
sensitivity. This avoids four duplicate sensitivity fields that could
disagree with one another, and reflects that sensitivity is
fundamentally a question about a record's origin, not about the record
itself.

---

## 4. Entity Contract

**Purpose.** Identifies a durable "who/what" Parker has encountered —
the addressable subject a Document, Assertion, or Relationship can refer
to. Carries no belief about the entity beyond its own bare identity and
classification; whatever Parker has learned *about* an Entity lives in
Assertions and Relationships that reference it, never as fields
appended directly to the Entity record itself.

**Required fields:**

- **Stable identifier** — assigned once, by Memory Core, at creation;
  never reassigned, never reused, retained even if the Entity's status
  later becomes `ARCHIVED` or `DELETED` (Section 11) so an audit trail
  can still name it — following the same established identifier pattern
  every other long-lived Parker object already uses (a single,
  non-blank value, validated at construction, no behaviour beyond
  identity).
- **Entity type** — an open, non-blank classification (for example:
  person, organisation, place, thing), deliberately **not** a closed
  enumeration. This mirrors `CandidateMemory.sourceSubsystem`'s own,
  already-established "illustrative, not closed by architectural
  necessity" treatment: the set of entity kinds Parker may ever need to
  represent is not enumerable today, and a closed list would foreclose a
  legitimate future kind for no concrete benefit.
- **Primary label** — a required, non-blank display name.
- **Aliases** — a list of alternate names or references this Entity is
  also known by. Defaults to empty. **Additive-only** once set (Section
  12) — an alias, once recorded, is never silently removed or rewritten;
  a later finding that an alias was wrong is expressed through a dispute
  or amendment (Section 6/12), never a quiet deletion from this list.
- **Related principal reference** — an optional reference to a
  registered Principal, present only when this Entity happens to
  correspond to one (for instance, the owner, or another registered
  user). Absent for the common case of an Entity with no corresponding
  Principal (a place, an organisation, an unregistered person).
- **Creation metadata** — the timestamp at which Memory Core itself
  created this record. Distinct from, and never a substitute for, any
  of the three timestamps Provenance carries (Section 7) — this field
  answers only "when did Memory Core record this," never "when did the
  underlying thing happen."
- **Provenance reference** — mandatory, non-optional, referencing a
  `Provenance` record (Section 7). No Entity may be constructed without
  one; this is enforced at construction, not by convention.
- **Status** — the shared lifecycle status (Section 11).
- **Metadata** — an open, non-authoritative key/value map, present for
  the same reason `Resource.metadata` already exists elsewhere in this
  repository: to support structural retrieval filtering (Section 9)
  without inventing a new typed field for every conceivable future
  filter criterion.

**What this contract intentionally does not carry:** any relationship to
another record (Section 8 owns that); any sensitivity classification of
its own (inherited from its Provenance, per Section 3's cross-cutting
decision); any belief, confidence, or truth-status field (that is
Assertion's job, never Entity's); any embeddings or ranking signal.

---

## 5. Document Contract

**Purpose.** Registers that a source document exists, where it can be
found, and what Memory Core currently knows about its provenance and
processing state — **registration only.** This contract never represents
a document's parsed contents, extracted text, page structure, or any
interpretation of what the document says. That is Document Handling's
own, later, separate concern (Section 17), and nothing about this
contract anticipates or reserves space for it beyond the one field
(`processingStatus`, below) needed to say, truthfully, "something else
has or has not yet acted on this."

**Required fields:**

- **Identifier** — assigned once by Memory Core, following the same
  established pattern as Entity's own identifier.
- **Document type** — an open, non-blank classification (for example:
  email, PDF, image, plain text), for the same reason Entity type is
  open rather than closed.
- **Location reference** — a required, non-blank reference to where the
  document can be found (a URI, a file path, an external system
  reference). Memory Core never fetches, opens, or validates this
  reference's reachability — it is recorded, not resolved.
- **Integrity hash** — an optional identifier (for example, a content
  hash) captured at registration time, when available, to support later
  tamper or change detection. Nullable, since not every registration
  path can supply one at registration time (for instance, a reference to
  an external, mutable location).
- **Registration metadata** — the timestamp at which Memory Core
  registered this Document, in the same sense, and with the same
  distinction from Provenance's own timestamps, as Entity's creation
  metadata above.
- **Provenance reference** — mandatory, exactly as Entity's.
- **Processing status** — a status distinct from the shared lifecycle
  status (Section 11), reflecting whether anything external to Memory
  Core has processed this Document's own contents: `REGISTERED` (no
  processing attempted or requested), `PROCESSING_REQUESTED`,
  `PROCESSED_EXTERNALLY` (some other subsystem — Document Handling, once
  it exists — has acted on it), `PROCESSING_FAILED`, or `NOT_APPLICABLE`
  (for a Document type registration is never expected to be processed
  further). Memory Core only ever records which of these is currently
  true; it never performs, triggers, or interprets the processing
  itself.
- **Status** — the shared lifecycle status (Section 11), independent of
  `processingStatus` above — a Document can be `SUPERSEDED` as a *record*
  while its `processingStatus` remains whatever it was, and vice versa.
- **Metadata** — an open key/value map, as Entity's.

**Relationships, satisfied without an embedded field.** This Unit's own
brief lists "relationships" among Document's required fields. Per
Section 3's cross-cutting decision, this is satisfied entirely through
`Relationship` records (Section 8) that name this Document as an
endpoint — never through a field embedded on `Document` itself. This is
a deliberate correction of the same untyped-embedded-list pattern the
Governance Review already found and criticised in today's
`MemoryRecord.relatedMemoryIds`; repeating it here for Document would
reintroduce the exact weakness `Relationship` was designed to remove.

---

## 6. Assertion Contract

**Purpose.** Records a claim. **An Assertion is never automatically
truth, and its own existence never causes any other record's status to
change.** This is checked explicitly in Section 16's test surface,
because it is the one guarantee this contract exists specifically to
protect.

**Required fields:**

- **Identifier** — assigned once by Memory Core, same pattern as above.
- **Statement** — a required, non-blank, free-text claim. This contract
  does not shape the statement's internal structure (a natural-language
  claim, a structured proposition) — that remains, deliberately, out of
  scope, the same way `CandidateMemory.knowledgePayload`'s internal
  shape has always been left unspecified.
- **Confidence** — an optional figure (`0.0`–`1.0`), present only when
  the submitter has one to offer. Distinct from, and never a substitute
  for, Provenance's own confidence in the *origin* of the claim (Section
  7) — this field is confidence in the *claim itself*.
- **Status** — the shared lifecycle status (Section 11), carrying the
  most operational weight of any record type this document designs: an
  Assertion's status (`ACTIVE`, `DISPUTED`, `SUPERSEDED`, ...) is the
  primary place a reader learns whether a claim currently stands,
  currently stands but is contested, or has been replaced.
- **Provenance reference** — mandatory, exactly as above. **This is also
  where "source," in this Unit's own brief's sense, lives** — per
  Section 3's cross-cutting decision, Assertion carries no separate
  `source` field of its own; Provenance already answers "where did this
  claim come from" more completely (source identifier, source type,
  creator, three distinct timestamps) than a single bare `source` field
  ever could.
- **Metadata** — an open key/value map, as above.

**Supporting and contradicting references, satisfied without an embedded
field.** This Unit's own brief lists "supporting references" and
"contradicting references" among Assertion's required fields. Per
Section 3's cross-cutting decision, both are satisfied by typed
`Relationship` records (Section 8, `SUPPORTS`/`CONTRADICTS` relationship
types) connecting this Assertion to a Document, an Entity, or another
Assertion — never by two parallel embedded list fields on `Assertion`
itself. This keeps "what supports this claim" and "what contradicts it"
each individually queryable, extensible to new endpoint kinds later
(Section 8), and — critically for this contract's own central guarantee
— makes clear that recording a support or contradiction is itself a
separate, explicit, auditable act (creating a `Relationship`), never an
implicit consequence of creating the `Assertion` alone.

**How this contract keeps its own central promise.** Nothing in this
shape gives an Assertion any path to mark another record `DISPUTED`,
`SUPERSEDED`, or otherwise altered as a side effect of its own creation.
Marking a *different* record disputed or superseded because of an
Assertion is always a second, separate, explicit act (a status
transition on that other record, itself permission-gated and audited,
Sections 10–12) — never something `Assertion` creation triggers on its
own. An Assertion existing, by itself, changes nothing about anything
else Memory Core holds.

---

## 7. Provenance Contract

**Purpose.** The mandatory, first-class origin record every other Memory
Core record must reference. This is the contract this whole design
treats as load-bearing: everything Section 2 calls "the authoritative
system of record" is true only to the extent Provenance is genuinely
complete, genuinely mandatory, and genuinely honest about what it does
not know.

**Required fields:**

- **Identifier** — assigned once by Memory Core.
- **Source identifier** — a required, non-blank reference to the origin
  (a URI, a `ConversationId`/`TurnId`, a `DocumentId`, an external
  system reference). Open-ended in shape, not a closed type, mirroring
  `CandidateMemory.sourceSubsystem`'s own established treatment.
- **Source type** — an open, non-blank classification of what kind of
  thing the source identifier names (for example: conversation,
  document, user-instruction, plugin, external-import) — again
  deliberately open, not closed, for the reason Section 4 already gives
  for Entity type.
- **Creator** — an optional, free-text description of who or what
  produced the underlying content. Deliberately free-text rather than a
  mandatory `PrincipalId`, because the underlying creator may not
  resolve to a registered Principal at all (the Governance Review's own
  finding: a creator "may or may not resolve to a registered Principal").
- **Creator principal reference** — a separate, optional reference to a
  registered Principal, present only when the creator does resolve to
  one. Both fields may be absent together (an entirely unknown creator);
  both may be present together (a known creator who is also a registered
  Principal); or only the free-text field may be present (a known name,
  no registered identity).
- **Claimed creation time** — an optional timestamp: the real-world time
  the underlying content *claims* to have originated, asserted, never
  verified, by whoever or whatever submitted it. This directly mirrors
  `WorldObservation.sourceTimestamp`'s existing precedent in this exact
  repository (the Governance Review's own citation) and is the field
  that makes non-contemporaneity provable: a Document whose claimed
  creation time is inconsistent with its acquisition time, once both are
  captured honestly and independently, is exactly the discrepancy a
  later reader would need to notice. This document does not design the
  comparison logic that notices it — only ensures the two timestamps
  exist, independently, to be compared.
- **Acquisition time** — a required timestamp: when Parker's own
  boundary first encountered this material. Always knowable, since it is
  Parker's own act, and therefore never nullable.
- **Ingestion time** — a required timestamp: when Memory Core itself
  recorded this Provenance. Assigned by Memory Core at creation, playing
  the role today's `MemoryRecord.promotedAt` plays alone — except here
  it is one of three distinct, independently meaningful timestamps,
  never the only one.
- **Derived-from references** — a list of other `Provenance` identifiers
  this material was derived from. Defaults to empty.
- **Extracted-from reference** — an optional `Document` identifier, when
  this material was extracted from a specific, already-registered
  Document.
- **Processing history** — a list of free-text audit entries describing
  what has been done to produce or refine this Provenance record.
  Additive-only (Section 12) — entries are appended, never removed or
  rewritten.
- **Integrity information** — an optional identifier (for example, a
  hash) for the provenance information itself, distinct from
  `Document.integrityHash`, which concerns the document's own content.
- **Confidence** — an optional figure (`0.0`–`1.0`): confidence in the
  provenance information itself, distinct from an Assertion's own
  confidence in its claim (Section 6) and distinct from any confidence a
  future Document Handling extraction step might report.
- **Content nature** — a required classification with exactly five
  values: `ORIGINAL`, `EXTRACTED`, `SUMMARISED`, `INFERRED`, and
  `UNKNOWN`. **`UNKNOWN` is an explicit, first-class, valid value, never
  a placeholder to be avoided** — a submission that genuinely cannot say
  which of the first four applies must say `UNKNOWN` rather than
  defaulting silently to `ORIGINAL`. This is the concrete mechanism
  through which this design satisfies "the contracts must represent
  uncertainty explicitly": uncertainty about content nature is not
  represented by a null or an omission, but by naming it outright.
- **Sensitivity** — an optional classification. Per this document's
  Section 3 decision, this is where sensitivity lives for every Memory
  Core record that references this Provenance, not duplicated per
  record. This document recommends reusing `ResourceSensitivity`
  (`src/contracts/Resource.kt`'s existing nine-value enum), the same
  classification already governing every other Resource in this
  repository, rather than inventing a Memory-Core-specific scheme —
  directly resolving the sensitivity-model mismatch the Governance
  Review left open (its own Section 20, Question 4). Left nullable,
  deliberately: a null sensitivity means "not yet classified," and
  **must be treated conservatively — at least as protectively as the
  most sensitive defined value — by any Permission Engine evaluating it,
  never treated as equivalent to `PUBLIC`.** This document does not
  modify `ResourceSensitivity` itself (an existing, shared, already-
  approved type this document is not authorised to change); it flags, as
  an open question (Section 19), whether a future implementation unit
  should propose adding an explicit `UNCLASSIFIED` value to that shared
  enum rather than relying on nullability and a documented convention.

**"Unknown values," satisfied throughout, not as a single field.** This
Unit's own brief lists "unknown values" among Provenance's requirements.
This document does not add one field named `unknown` — it makes every
field above that could genuinely be unknown (`creator`,
`creatorPrincipalId`, `claimedCreationTime`, `derivedFrom`,
`extractedFrom`, `integrityInformation`, `confidence`, `sensitivity`)
independently nullable, and gives `contentNature` its own explicit
`UNKNOWN` value where a closed classification would otherwise force a
false choice. Section 14 states the governing principle this pattern
follows throughout: an unknown value is never fabricated, defaulted, or
inferred without a disclosed inference step.

---

## 8. Relationship Contract

**Purpose.** The one, single mechanism connecting any two Memory-Core-
addressable records — and, by identifier only, records outside Memory
Core entirely (a Conversation Turn, a future Knowledge Memory record).
No other contract in this design carries its own embedded reference to
another record; every connection this whole design needs is expressed
here.

**Required fields:**

- **Identifier** — assigned once by Memory Core.
- **Relationship type** — an open, non-blank classification, **not** a
  closed enumeration, for extensibility (this Unit's own explicit
  instruction: "Design for extensibility"). A small set of **recognised**
  values is named below because Memory Core's own retrieval and
  lifecycle logic (Sections 9, 11, 12) treats them specially; any other,
  caller-defined value is accepted and stored, but carries no special
  interpretation:
  - `SUPPORTS` / `CONTRADICTS` — connects an Assertion to whatever
    supports or contradicts it (Section 6).
  - `AMENDS` — connects a new record to the original it amends (Section
    12).
  - `SUPERSEDES` — connects a new record to the original it replaces as
    the current answer (Section 12).
  - `DISPUTES` — connects a new Assertion (or a status-transition act) to
    the record it disputes.
  - `SAME_AS` — connects two records asserted, by a caller, to represent
    the same real-world thing (Section 14's answer to duplicate-entity
    handling: Memory Core never merges records on its own; a caller
    expressing that two Entities are the same does so through this
    relationship type, not by requesting a merge).
  - `EXTRACTED_FROM` — mirrors `Provenance.extractedFrom` at the
    relationship level, for cases where the relationship itself (not
    just the provenance record) needs to be independently queryable and
    traversable.
  - `REFERENCES` — a general-purpose, non-evidentiary connection, for
    any relationship not better described by one of the above.
- **From endpoint / to endpoint** — each endpoint is a pair: a **record
  kind** (an open, non-blank tag — `entity`, `document`, `assertion`,
  `relationship`, `conversation-turn`, `knowledge-record`, extensible to
  future kinds) and a **record identifier** (the raw identifier value of
  whatever typed identifier that kind actually uses). This shape,
  deliberately, does not require `Relationship` to hold a structural
  Kotlin-level dependency on every kind of record it might ever connect
  — in particular, it must never depend on Knowledge Memory's own,
  not-yet-designed types, preserving the one-directional dependency
  Section 2 and the Reconciliation both already require. A Relationship
  pointing at a `conversation-turn` or a future `knowledge-record` is
  accepted and stored by identifier and kind tag alone, exactly the same
  loose-coupling-by-identifier pattern `correlationId` already uses
  throughout this repository, never verified against that external
  subsystem's own state (Section 14).
- **Directional flag** — a required boolean. Some recognised types are
  inherently directional (`SUPERSEDES` points from the new record to the
  old one; `SUPPORTS`/`CONTRADICTS` point from the Assertion to what
  supports or contradicts it); others may be symmetric. This flag tells
  a future traversal operation (Section 9) whether to also traverse the
  reverse edge.
- **Provenance reference** — mandatory, exactly as every other record
  type. A Relationship is itself a claim that a connection exists, and
  that claim needs the same origin accountability as any other record —
  in particular, this is what lets a future reader ask "who asserted
  that these two Entities are `SAME_AS` each other, and on what basis."
- **Creation metadata** — the timestamp Memory Core created this
  Relationship record.
- **Status** — the shared lifecycle status (Section 11).

**Referential integrity, stated once here since it governs every
endpoint above:** enforced only for endpoint kinds Memory Core itself
owns and can verify (`entity`, `document`, `assertion`, `relationship`)
— a Relationship naming a nonexistent Entity identifier is rejected at
creation. Endpoints of kinds Memory Core does not own
(`conversation-turn`, `knowledge-record`) are accepted unverified, by
identifier alone, since Memory Core has no access to `ConversationEngine`'s
or Knowledge Memory's own state to check against, and must not be given
one merely to support this check (Section 2's own boundary).

---

## 9. Retrieval Contract (`MemoryRetrieval`)

**Purpose.** The narrow, structural, non-semantic read boundary onto
everything Sections 4–8 define. Supports exactly the seven retrieval
modes this Unit's own brief names, and no others.

- **Identifier lookup** — given an identifier and its record kind,
  return that one record or nothing. Never throws for "not found,"
  mirroring the same "empty/null, never throw" convention already
  established throughout this repository (`IdentityService.resolve`,
  `ConversationHistorySource.history`, today's own `MemoryStore.retrieve`).
- **Entity lookup** — given criteria (label or alias match, entity type,
  status), return matching Entities, bounded by a required, positive
  `maximumResults`, mirroring `MemoryQuery.maximumResults`'s own
  established, non-negotiable requirement that retrieval never implies
  "return everything matching."
- **Document lookup** — given criteria (document type, location
  reference match, processing status), return matching Documents, same
  bounding requirement.
- **Relationship traversal** — given a starting endpoint and, optionally,
  a relationship type and a direction, return the connected
  `Relationship` records. Deliberately returns `Relationship` records
  themselves, not the resolved endpoint records they connect — a caller
  that wants the connected Entity or Document performs a second,
  separate identifier lookup. This keeps traversal type-safe without
  `MemoryRetrieval` needing a generic "any record" return shape capable
  of representing every possible endpoint kind, including future ones.
- **Chronological lookup** — given a time range and, optionally, a
  record kind, return matching records ordered by their own creation
  timestamp (Entity's creation metadata, Document's registration
  metadata, and so on — never by any Provenance-carried claimed or
  acquisition timestamp, which describes the underlying content, not the
  Memory Core record itself).
- **Metadata filtering** — given a key/value filter, return records of a
  specified kind whose `metadata` map matches.
- **Provenance-aware lookup** — given criteria over Provenance's own
  fields (source type, creator, content nature, sensitivity), return
  records whose referenced Provenance matches.

**Explicitly excluded, per this Unit's own instruction:** no semantic or
embedding-based retrieval mode exists here, and none is anticipated by
this contract's shape — exactly the same, already-precedented deferral
`MemoryStore`'s own `MemoryRetrievalPolicy` seam already established for
today's Memory, extended here rather than reopened.

**A deliberate, disclosed departure from today's `MemoryStore.retrieve`'s
own precedent:** `MemoryRetrieval` performs **no internal, principal-based
visibility filtering of its own.** Today's `InMemoryMemoryStore.retrieve`
silently narrows its own results by `originatingPrincipalId` before
returning them — a soft, internal form of authorisation the Reconciliation's
own Trust Boundary decision (its Section 12) explicitly rules out for
Memory Core: **"Memory Core never authorises itself."** Every operation
below still accepts a `requestingPrincipalId`, but only for auditability
(recording who asked) — never as a filter this contract applies on its
own. Whatever composes `MemoryRetrieval` into the running system is
responsible for evaluating a real Permission Engine decision, per record
or per query, before a result ever reaches the requester (Section 10).
This is named here as a correction, not an oversight, because it is easy
to miss: today's Memory looks like it already does identity-scoped
retrieval safely, and a future implementation unit could reasonably
assume `MemoryRetrieval` should do the same. It must not.

---

## 10. Permission Boundary

Restates and finalises the Reconciliation's own Section 12 decision,
made concrete against the actual contracts designed above.

**Memory Core never authorises itself.** No contract in Sections 4–9
holds, or is permitted to hold, a `PermissionEngine` reference. Every
permission decision below is made by whatever composes `MemoryCore` and
`MemoryRetrieval` into the running system — mirroring exactly how
`ExecutionPipeline`, not any individual `Tool`, is the one place a Tool
invocation's permission check happens today.

| Question | Answer |
| --- | --- |
| Who creates? | Any caller whose proposed action resolves, via the existing `ActionVocabularyEntry`/`ActionMapper` mechanism, to `PermissionAction.WRITE` on `ResourceType.MEMORY` or `DOCUMENT`, and is `APPROVED` by `PermissionEngine.evaluate` before `MemoryCore`'s corresponding creation operation is invoked. |
| Who updates (amends)? | The same `WRITE` check, evaluated against the *new*, linked amendment record being created — amendment is structurally a creation (Section 12), never a separate "update" action on the original. |
| Who disputes? | The same `WRITE` check, targeted at the new `Assertion` or `DISPUTES`-typed `Relationship` being created. |
| Who supersedes? | The same `WRITE` check, targeted at the new record and its `SUPERSEDES`-typed `Relationship`. |
| Who deletes? | `PermissionAction.DELETE` on the same resource types, reserved specifically for the owner-requested erasure path (Section 11, Section 12) — never used for ordinary, non-destructive correction. |
| Who retrieves? | `PermissionAction.READ`, evaluated by the caller/runtime composing `MemoryRetrieval` — per Section 9, never evaluated inside `MemoryRetrieval` itself. A record whose Provenance sensitivity is unclassified (`null`) must be treated at least as protectively as the most sensitive defined `ResourceSensitivity` value until classified (Section 7). |

**No new `PermissionAction` or `ResourceType` value is required.** Every
check above is expressible with the enum values that already exist in
`src/contracts/Permission.kt` and `src/contracts/Resource.kt` today.

---

## 11. Lifecycle

This Unit's own brief offers an example lifecycle
(`Created → Registered → Referenced → Amended → Disputed → Superseded →
Archived`) and explicitly instructs this document to "determine
appropriate transitions" — not to adopt it uncritically. This document
does not adopt it uncritically, and states why.

**`Created` and `Registered` are not two separate states.** Nothing in
any of the six record contracts above involves a genuine two-phase
submit-then-register process (unlike, for instance, Agent Run's own,
separately-approved two-phase acceptance/execution split, which exists
for a concrete, disclosed reason). Collapsing them avoids inventing
structure with no identified concrete need, consistent with the
contract-minimalism discipline `MEMORY_CONTRACT_DESIGN.md` itself already
established for today's Memory.

**`Referenced` is not a meaningful independent status.** Whether a
record has been referenced by at least one `Relationship` is an
observable fact, answerable by traversal (Section 9), not a state
anything transitions into or out of. Making it a formal status would
create an ambiguous, redundant value that must be kept in sync with
`Relationship` data it merely restates. It is not included as a status
value.

**Recommended lifecycle**, applying uniformly to `Entity`, `Document`,
`Assertion`, and `Relationship` (a single shared status type, not four
near-duplicate ones):

```
                 ┌──────────────┐
                 │    ACTIVE    │◄────────────┐
                 └──────┬───────┘              │
             dispute    │      dispute         │ dispute
             recorded   │      resolved        │ withdrawn
                 ┌──────▼───────┐              │
                 │   DISPUTED   │──────────────┘
                 └──────┬───────┘
        SUPERSEDES relationship created,
        naming this record as the target
                 ┌──────▼───────┐
                 │  SUPERSEDED  │
                 └──────┬───────┘
      ┌──────────────────┴───────────────────┐
      │      (from ACTIVE, DISPUTED, or        │
      │       SUPERSEDED -- archival is         │
      │       reversible)                        │
┌─────▼──────┐                            ┌──────▼─────┐
│  ARCHIVED  │◄──────────────────────────►│  (unarchive) │
└─────┬──────┘                            └────────────┘
      │
      │  owner-requested erasure only (from ANY status)
      ▼
┌────────────┐
│   DELETED  │  (terminal; irreversible)
└────────────┘
```

- **`ACTIVE`** — the initial resting state, entered immediately at
  creation (no separate "created but not yet active" gap).
- **`DISPUTED`** — reachable from `ACTIVE`, entered when a dispute is
  formally recorded (a new `Assertion` or `DISPUTES`-typed
  `Relationship`, Section 10's own permission-gated act). Reversible: a
  formally withdrawn or resolved dispute returns the record to `ACTIVE`
  — itself an explicit, audited transition (Section 12), never a silent
  reversion.
- **`SUPERSEDED`** — reachable from `ACTIVE` or `DISPUTED`, entered when
  a `SUPERSEDES`-typed `Relationship` is created naming this record as
  its target. The record remains permanently readable; only its
  "current-ness" changes.
- **`ARCHIVED`** — reachable from any of the three states above, and
  itself reversible (unarchiving returns the record to whatever status
  it held before archival) — a soft, non-destructive "excluded from
  default retrieval emphasis" state, distinct from deletion.
- **`DELETED`** — reachable from any status, reserved exclusively for
  the owner-requested erasure path (Section 10's `DELETE` permission
  check), terminal and irreversible, and — per Section 12 — content-
  removing while identifier-level proof of prior existence is retained,
  mirroring today's `MemoryStore.forget`'s own already-correct shape for
  this one specific, narrow purpose.

---

## 12. Immutability

**Immutable fields, once a record is created:** its own identifier; its
Provenance reference; its creation timestamp; and its "core content" —
`Entity.entityType` and `Entity.primaryLabel`; `Document.locationReference`,
`Document.documentType`, and `Document.integrityHash`;
`Assertion.statement`; `Relationship.relationshipType` and both
endpoints. None of these fields has any operation, on any contract in
this design, capable of altering it after creation. A correction to any
of them is always expressed as a new record plus an `AMENDS`- or
`SUPERSEDES`-typed `Relationship` (below) — never an in-place rewrite.
This is the literal, mechanical realisation of the constitutional
principle's own closing clause: **reasoning does not overwrite history.**

**Mutable fields:** `status` (Section 11, always via an explicit,
audited transition operation on `MemoryCore`, never a silent field
write); `Entity.aliases` (additive-only — new aliases may be appended;
an existing alias is never removed or rewritten, since doing so would
itself quietly rewrite what Memory Core once recorded); `Document.processingStatus`
(reflects ongoing external work, not a rewrite of registration facts);
`Provenance.processingHistory` (additive-only, same reasoning as
aliases); and every record's `metadata` map (additive/updatable, since
it is explicitly non-authoritative structural filtering support, never
core content).

**Amendment process.** A substantive correction to any record is
expressed as: (1) a new record of the same type, carrying its own
Provenance; (2) an `AMENDS`-typed `Relationship` connecting the new
record to the original; and (3), where the amendment is meant to become
the current answer rather than merely sit alongside the original, a
further `SUPERSEDES`-typed `Relationship` and the corresponding status
transition on the original (Section 11). The original's own fields are
never touched by either step.

**Supersession.** The same mechanism, using `SUPERSEDES` specifically.
This document deliberately does **not** design any default "prefer the
non-superseded record" filtering behaviour inside `MemoryRetrieval`
(Section 9) — doing so would be exactly the kind of implicit ranking
policy this Unit's own exclusions (and today's already-deferred
`MemoryRetrievalPolicy` seam) put out of scope. A caller wanting only
current records filters by status explicitly.

**Audit behaviour.** This design does not add a separate, bespoke audit
log type. Auditability is satisfied structurally, by two things acting
together, both already first-class, permanent, queryable records in
their own right: every status transition (itself required to be
represented as one of the frozen events, Section 13) and every
`Relationship` recording an amendment, dispute, or supersession
(itself an ordinary, permanent, immutable Memory Core record, fully
subject to Section 9's own retrieval modes). A future reader reconstructs
a record's full history by traversing its `Relationship`s and reading its
transition events — not by consulting a separate log this design would
otherwise have to keep consistent with the records it describes.

---

## 13. Event Contracts

This Unit's own brief names eight candidate event names and instructs
this document to "freeze only those justified." Five are justified;
three are not, for stated reasons — the same rigour the Governance
Review already applied to Memory's own candidate event list, extended
here rather than relaxed:

| Candidate | Determination |
| --- | --- |
| `memory.entity_created` | **Frozen.** A genuine, discrete, auditable creation act. |
| `memory.document_registered` | **Frozen.** Same reasoning. |
| `memory.assertion_created` | **Frozen.** Same reasoning, and especially load-bearing given Section 6's own truth-status stakes. |
| `memory.relationship_created` | **Frozen.** The single most important event of the five: per Section 12, `Relationship` creation *is* how an amendment, dispute, or supersession becomes visible to the rest of the runtime — this event is the audit trail's own real-time signal. |
| `memory.record_amended` | **Not frozen, separately.** Fully reconstructable from `memory.relationship_created` (type `AMENDS`) together with the corresponding `*_created` event for the new record. Freezing a second, parallel event for the same fact risks the two streams drifting out of sync with no benefit over reading `relationship_created` directly. |
| `memory.record_disputed` | **Not frozen, separately** — folded into the status-transition event below, since a dispute is fundamentally a status change, not always accompanied by a new record. |
| `memory.record_superseded` | **Not frozen, separately** — same reasoning; folded below. |
| **`memory.record_status_changed`** (this document's own addition, replacing the two immediately above) | **Frozen.** Carries the record kind, record identifier, prior status, and new status — one general event covering every transition in Section 11's lifecycle (`DISPUTED`, `SUPERSEDED`, `ARCHIVED`, `DELETED`, and reversions), rather than a separate named event per status value. This is a disclosed, justified reduction from the brief's own literal list, in the same spirit as the Governance Review's own prior finding that not every candidate name deserves freezing. |
| `memory.retrieved` | **Not frozen.** No concrete need is identified anywhere in this design or its precedents, and this mirrors the Governance Review's own identical, already-reasoned finding for today's Memory: an unconditional per-read event is a volume- and privacy-sensitive default this document is not prepared to endorse without a stated purpose. |

**Five events frozen in total:** `memory.entity_created`,
`memory.document_registered`, `memory.assertion_created`,
`memory.relationship_created`, `memory.record_status_changed`.

**Publication is a Runtime Responsibility (Section 15), not something
this document assumes exists automatically.** Today's
`InMemoryMemoryStore` has no `EventBus` dependency at all (a Governance
Review finding). A future Memory Core implementation must acquire one
deliberately — this document recommends its own implementation own that
dependency directly and publish these five events itself, mirroring how
other runtime components (`InMemoryTaskManagerRuntime`,
`InMemoryAgentRuntime`) already own their own `EventBus` reference,
rather than introducing a separate wrapping coordinator whose only job
would be to publish on Memory Core's behalf.

---

## 14. Failure Behaviour

The governing principle, stated once and applied to every case below:
**an unknown value is never fabricated, defaulted, or inferred without a
disclosed inference step**, and a conflict between two records is never
resolved by Memory Core silently preferring one over the other.

- **Missing provenance.** Structurally impossible, by design: a
  `MemoryCore` creation operation for any record type rejects the
  request outright if no valid Provenance reference is supplied. There
  is no "create now, attach provenance later" path.
- **Unknown author / unknown dates.** Represented by `Provenance.creator`,
  `creatorPrincipalId`, and `claimedCreationTime` each being genuinely
  null — an explicit, valid, non-erroring state (Section 7), never a
  fabricated placeholder value.
- **Duplicate Entity.** Memory Core performs no identity resolution or
  deduplication of its own — two Entity records that happen to represent
  the same real-world thing are both accepted as independent, valid
  records. Memory Core has no reliable basis to judge sameness on its
  own, and manufacturing that judgment would itself be a form of
  fabricated certainty this design's own governing principle forbids.
  A caller confirming two Entities are the same expresses it through a
  `SAME_AS`-typed `Relationship` (Section 8) — an explicit, attributable,
  disputable claim, not a silent merge.
- **Duplicate Document.** The same reasoning: a matching
  `integrityHash` may be surfaced as informational at registration
  time, but registration itself always succeeds; any deduplication
  judgment is a caller decision, expressed the same way as duplicate
  Entities, never performed automatically.
- **Conflicting Assertions.** Both stand, independently, each retaining
  its own status. A conflict is surfaced structurally through
  `CONTRADICTS`-typed `Relationship`s connecting them — never resolved,
  ranked, or arbitrated by Memory Core itself (Section 2's own boundary
  against Memory Core ever deciding what is true).
- **Missing references.** Governed by Section 8's referential-integrity
  rule: rejected outright for endpoint kinds Memory Core owns and can
  verify; accepted, unverified, by identifier alone, for endpoint kinds
  outside Memory Core's own boundary (`conversation-turn`,
  `knowledge-record`).
- **Invalid relationships** (for example, an endpoint referencing itself,
  or a blank relationship type). Rejected at construction with a clear,
  stated reason — never silently coerced into something valid.

---

## 15. Runtime Responsibilities

**Runtime (whatever composes Memory Core into the running system)
owns:**

- Every `PermissionEngine.evaluate` call required by Section 10, before
  any `MemoryCore` write and before any sensitive `MemoryRetrieval` read
  reaches its requester.
- Publishing the five events named in Section 13, via a real `EventBus`
  dependency this document recommends `MemoryCore`'s own implementation
  hold directly (a deliberate, disclosed departure from today's
  `InMemoryMemoryStore`, which holds none).
- Resolving *who* is asking, via `IdentityService`, before a
  `requestingPrincipalId` is ever passed into a `MemoryCore` or
  `MemoryRetrieval` operation — Memory Core itself never performs
  identity resolution, exactly as `MEMORY_RUNTIME_ARCHITECTURE.md` §10
  already established for today's Memory and this design inherits
  unchanged.

**Memory Core owns:**

- Identity assignment for its own five record kinds, minted internally,
  never accepted from a caller.
- Provenance capture and mandatory-reference enforcement (Section 7,
  Section 14).
- Lifecycle and status-transition validation (Section 11) and
  immutability enforcement (Section 12).
- Structural, non-semantic retrieval (Section 9).

**Memory Core does not own:** permission evaluation (Section 10);
principal identity *resolution* (it only ever carries a `PrincipalId`
reference supplied to it, already resolved elsewhere); Conversation
storage; semantic ranking; Document parsing; and, per Section 9's own
disclosed correction, any internal, self-directed visibility filtering
of its retrieval results.

**Knowledge Memory owns**, restated from the Reconciliation and made
concrete here: evaluation and promotion decisions over Memory Core
content, and nothing about Memory Core's own record types, lifecycle, or
provenance. **Knowledge Memory reads Memory Core; it never writes to
it.** A future Knowledge Memory promotion produces a `KnowledgeRecord` —
Knowledge Memory's own type, not a new Memory Core record — referencing
whichever Memory Core records it was promoted from by identifier, never
minting a new Entity, Document, Assertion, or Relationship on Memory
Core's behalf. This closes off what would otherwise be a backdoor write
path into Memory Core through Knowledge Memory, keeping Memory Core's
write surface limited to genuinely Memory-Core-originated submissions.

---

## 16. Test Surface

Recommended minimum coverage, described by behaviour, not by
implementation:

- **Entity.** Creation without a Provenance reference is rejected.
  Aliases are additive-only — an attempt to remove or rewrite an
  existing alias has no corresponding operation to perform it.
  `entityType`/`primaryLabel` are immutable after creation.
- **Document.** Registration never attempts to fetch or read the
  referenced location. `processingStatus` can change independently of
  the shared lifecycle `status`. No embedded relationship field exists
  on `Document` at all (a structural/API-shape test).
- **Assertion.** Creating an Assertion has no observable side effect on
  any other record's status or existence (the contract's own central
  guarantee, tested directly). `supportingReferences`/`contradictingReferences`
  from the brief's own literal field list are absent as embedded fields;
  the same information is only reachable via `Relationship` traversal.
- **Provenance.** Every other record type's creation operation fails
  without a valid Provenance reference. The three timestamps
  (`claimedCreationTime`, `acquisitionTime`, `ingestionTime`) behave
  independently — setting one has no effect on another.
  `contentNature` defaults to `UNKNOWN` when the caller does not supply
  a value; it never silently defaults to `ORIGINAL`.
- **Relationship.** Referential integrity is enforced for
  Memory-Core-owned endpoint kinds and is not enforced (accepted
  unverified) for external endpoint kinds. A self-referencing
  relationship is rejected. The `directional` flag correctly governs
  whether traversal returns the reverse edge.
- **`MemoryRetrieval`.** Identifier lookup returns nothing, never throws,
  for an unknown identifier. Every list-returning operation respects a
  required, positive `maximumResults`. Chronological ordering is
  correct and stable. A structural/API-shape test confirms
  `MemoryRetrieval` takes no `PermissionEngine` dependency and performs
  no principal-based filtering of its own results (Section 9's own
  disclosed correction, verified, not merely asserted).
- **Lifecycle.** Every transition Section 11 draws succeeds; every
  transition it does not draw (for example, `DELETED` to any other
  status) is rejected. `DISPUTED → ACTIVE` (dispute withdrawal) succeeds
  and is itself represented by a `memory.record_status_changed` event.
- **Immutability.** For every field Section 12 names immutable, no
  public `MemoryCore` operation exists capable of altering it after
  creation — the same "the guarantee is that no such operation exists"
  test shape today's `MemoryStore` tests already use to confirm no
  caller-facing `promote` operation exists.
- **Events.** Each of the five frozen events (Section 13) is published
  exactly once per corresponding act, carrying the correct record
  identifiers, and `memory.retrieved` is never published under any
  circumstance this test surface exercises.

---

## 17. Future Extension Points

Identified, not designed, per this Unit's own instruction:

- **Evidence.** A future record type built atop `Assertion` and
  `Relationship` (`SUPPORTS`/`CONTRADICTS`), consistent with the
  Governance Review's own finding that Evidence depends on Entity,
  Document, Provenance, and Assertion already existing before it can be
  meaningfully designed.
- **Document Handling.** A future, separate Programme that consumes
  `Document.locationReference`, updates `processingStatus`, and may
  register further Memory Core records (extracted Entities, for
  instance) through `MemoryCore`'s own ordinary, permission-gated write
  path — Document Handling is a *caller* of Memory Core, never a
  modification to it.
- **Knowledge Memory.** Reads Memory Core records as input to its own
  promotion evaluation, through a future, narrower read boundary
  mirroring `MemorySource`'s own existing capability-narrowing pattern —
  not designed here, per the Reconciliation's own sequencing (its
  Section 16, step 4).
- **Semantic retrieval.** A future, separate read capability, layered
  alongside `MemoryRetrieval`, not a modification to any operation this
  document defines — mirroring today's already-deferred
  `MemoryRetrievalPolicy` seam.
- **World Model.** Not an extension point at all. This document confirms
  the Reconciliation's own finding stands unchanged: no future Memory
  Core capability should introduce a relationship to the World Model in
  either direction.

---

## 18. Contract Summary

| Contract | Kind | Identifier type | Mandatory Provenance? | Mandatory permission gate? |
| --- | --- | --- | --- | --- |
| `Entity` | Record | Entity identifier | Yes | Write: create/amend; Read: retrieve |
| `Document` | Record | Document identifier | Yes | Write: register/amend; Read: retrieve |
| `Assertion` | Record | Assertion identifier | Yes | Write: create/dispute; Read: retrieve |
| `Provenance` | Record | Provenance identifier | N/A (is itself the provenance) | Write: create; Read: via referencing record |
| `Relationship` | Record | Relationship identifier | Yes | Write: create; Read: traverse |
| `MemoryRetrieval` | Read-only interface | N/A | N/A | Enforced externally, never internally (Section 9) |
| `MemoryCore` | Write interface (this document's disclosed addition) | N/A | N/A | Every operation gated per Section 10's table |

Shared supporting types: a single lifecycle status (Section 11, used
identically by `Entity`, `Document`, `Assertion`, `Relationship`); a
five-value content-nature classification (Section 7); an open,
string-typed relationship-type convention with seven recognised values
(Section 8); an open, string-typed record-kind tag for `Relationship`
endpoints (Section 8); a non-authoritative metadata key/value map,
present on every record type (Sections 4–8).

---

## 19. Open Questions

Limited strictly to questions that genuinely block implementation — not
speculative future concerns:

1. **Should `ResourceSensitivity` gain an explicit `UNCLASSIFIED` value?**
   This document's own recommendation (Section 7) relies on nullable
   sensitivity plus a documented conservative-handling convention rather
   than modifying the existing, shared `ResourceSensitivity` enum. A
   future implementation unit must decide whether to propose that
   additive change instead, before or during implementation — this
   affects Permission Engine integration mechanics, not this document's
   own record shapes, so it does not block Contract Design acceptance,
   but does need resolving before the Scope Lock can commit to one
   concrete mechanism.
2. **What identifier type does `MemoryCore` accept for a `requestingPrincipalId`
   passed for audit purposes only (Section 9), and how is it validated
   against `IdentityService` before use?** This document establishes
   that Memory Core never resolves identity itself, but does not specify
   whether `MemoryCore`/`MemoryRetrieval` should reject an unresolvable
   `PrincipalId` outright or accept it uncritically for audit purposes
   only. A Scope Lock–level decision, not a Contract Design blocker.
3. **Where exactly does the `MemoryCore` implementation's `EventBus`
   dependency get constructed and wired in `ParkerRuntime.kt`, relative
   to the Permission Engine and Identity Service it also needs?** A
   composition-ordering question for the eventual implementation unit,
   not a contract-shape question this document needs to resolve.

None of the three blocks acceptance of the contracts themselves — each
is a downstream Scope Lock or implementation-sequencing question, not an
unresolved architectural ambiguity in what is designed above.

---

## 20. Recommendation

Every contract above was checked against this Unit's own required design
principles directly, not merely asserted to satisfy them: single
responsibility (Section 2's explicit non-overlap statements against
Conversation, Knowledge Memory, World Model, Permission Engine, and
Document Handling); constitutional ownership (Section 5's "storage does
not create truth" enforcement, tested explicitly in Section 16);
provenance-first (Section 7's mandatory, non-optional reference on every
record, enforced structurally, not by convention, per Section 14);
auditability (Section 12's structural answer — `Relationship` plus
status-transition events, not a bolted-on log); immutability of source
history (Section 12's explicit immutable-field list and amendment-only
correction path); explicit uncertainty (Section 7's `UNKNOWN` content
nature and pervasive nullability, Section 14's governing principle);
implementation independence and technology independence (no language
syntax anywhere in this document, no storage technology named or
assumed); and future extensibility (Section 17, plus the open,
non-closed classification fields used throughout Sections 4–8).

```
READY FOR SCOPE LOCK
```
