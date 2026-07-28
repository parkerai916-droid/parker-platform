# Memory Core — Scope Lock

## Status

Programme: **Programme 2 — Memory Core Scope Lock.**
Phase: **Final governance document before implementation.** No Kotlin is
implemented, proposed as a diff, or changed by this document. Neither
`src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

**This document is binding.** It does not redefine any constitutional
decision already made. `docs/architecture/MEMORY_CORE_GOVERNANCE_REVIEW.md`,
`docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md`, and
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` are frozen, normative
inputs — this document does not reopen the layering, the terminology,
the record shapes, or the permission model those three already settled.
Its own purpose is narrower and final: fix, without ambiguity, exactly
what Version 1 of Memory Core builds, and exactly what it does not. Every
capability considered below is marked `IN SCOPE` or `OUT OF SCOPE`. There
is no third category. "Future consideration" is not a classification
this document uses anywhere.

**Scope Lock Principle.** The first Memory Core implementation shall
establish a truthful, governed system of record. It is not intended to
become a complete knowledge system. Where a candidate capability is
plausible, useful, or eventually necessary but not required to satisfy
that one sentence, it is `OUT OF SCOPE` — the burden of proof favours
exclusion, not inclusion, throughout this document.

---

## 1. Executive Summary

Version 1 of Memory Core implements exactly the seven public contracts
`MEMORY_CORE_CONTRACT_DESIGN.md` already designed — `Entity`, `Document`,
`Assertion`, `Provenance`, `Relationship`, `MemoryCore` (the write
interface), and `MemoryRetrieval` (the read interface) — with the
lifecycle, immutability rules, permission boundary, and five-event
publication model that document already froze. It implements no storage
technology beyond an in-memory implementation, no document interpretation
of any kind, no semantic or ranked retrieval, no connection to the World
Model, and no promotion or evaluation logic (that remains Knowledge
Memory's own, separately-scoped responsibility). The objective is a
narrow, complete, internally consistent system of record — not a partial
implementation of something larger.

---

## 2. Frozen Objectives

Implementation shall achieve, and shall be judged against, exactly the
following:

1. Every Entity, Document, Assertion, and Relationship record created by
   Memory Core carries a mandatory, valid Provenance reference — never
   optionally, never as a default-filled placeholder.
2. No public operation exists anywhere in Memory Core capable of
   altering an immutable field after a record's creation (Contract
   Design §12). Every correction is a new, linked record.
3. The frozen lifecycle (Section 8, below) is enforced exactly — no
   transition outside it succeeds, and no record silently skips a
   required transition.
4. The seven frozen retrieval modes (Section 10, below) are the only
   read operations Memory Core exposes. No ranking, scoring, or semantic
   matching of any kind is present.
5. Memory Core holds no `PermissionEngine` reference anywhere in its own
   contracts, and performs no principal-based filtering internally
   (Section 6).
6. Exactly the five frozen events (Section 9, below) are published, each
   exactly once per corresponding act.
7. Memory Core depends on nothing outside itself except `Provenance`'s
   own loosely-coupled, by-identifier-only references to Conversation
   and future Knowledge Memory content (Contract Design §8) — it is
   never depended upon by, and never depends upon, the World Model.

---

## 3. Mandatory Deliverables

| Deliverable | Classification |
| --- | --- |
| Entity contract implementation | **REQUIRED** |
| Document registration | **REQUIRED** |
| Assertion records | **REQUIRED** |
| Provenance records | **REQUIRED** |
| Relationship records | **REQUIRED** |
| `MemoryCore` write interface | **REQUIRED** |
| Retrieval interface (`MemoryRetrieval`) | **REQUIRED** |
| Lifecycle management (Section 8's state machine, all five statuses and every transition it defines) | **REQUIRED** |
| Audit support | **REQUIRED**, in exactly the structural form Contract Design §12 defined — `Relationship` records plus `memory.record_status_changed` events. **No separate, bespoke audit-log type or query surface is required**, and none may be substituted for this structural mechanism without a Scope Lock revision. |
| Event publication | **REQUIRED**, limited exactly to the five events Section 9 freezes. Publishing any additional event is equally a scope violation as failing to publish one of the five. |

All ten deliverables are `REQUIRED`. Version 1 has no optional or
partial deliverable among the seven contracts Contract Design already
designed — a Version 1 that ships five of the six record/interface
contracts and defers the sixth is not an acceptable interpretation of
this Scope Lock; it is a different, smaller scope that would itself
require a revision to this document before proceeding.

---

## 4. Explicit Exclusions

Every item below is `OUT OF SCOPE` for Version 1, with the reason it is
excluded stated directly, not merely asserted:

| Excluded capability | Reason |
| --- | --- |
| OCR | Belongs to the future Document Handling Programme (Contract Design §17, Governance Review §13). Memory Core registers a Document's existence and location; it never reads or interprets its contents. |
| PDF parsing | Same Document Handling boundary. No format-specific interpretation of any kind is designed anywhere in the seven frozen contracts. |
| File import | Memory Core's `Document` contract records a location reference supplied by a caller (Contract Design §5); it never discovers, fetches, uploads, or imports a file itself. |
| Image analysis | Same Document Handling boundary as OCR. No image interpretation capability exists in any contract this document freezes. |
| Semantic retrieval | Explicitly and repeatedly excluded across all three frozen documents (Contract Design §9, Governance Review §11). No concrete need has been identified; it remains a future, separately-justified layer, not an extension of `MemoryRetrieval`. |
| Embeddings | No retrieval algorithm beyond the seven structural modes (Section 10) is designed. Embeddings would commit Version 1 to a specific technology this document has no justified need to select. |
| Vector databases | Same reasoning as Embeddings. Version 1 selects no storage technology beyond an in-memory implementation (Governance Review §14); a vector database is a premature technology commitment with no identified concrete requirement. |
| AI summarisation | Memory Core never generates content — it only records what it is given. `Provenance.contentNature`'s `SUMMARISED` value (Contract Design §7) lets a caller *declare* that content it submits is a summary; Memory Core never produces one itself. |
| Knowledge promotion | Exclusively Knowledge Memory's own, structurally downstream responsibility (Reconciliation §5, §8, §9). Memory Core never evaluates, decides, or promotes anything into durable knowledge. |
| World Model integration | Constitutionally excluded (Reconciliation §10, restated Contract Design §2). The World Model remains fully independent of the whole Memory family, in both directions, permanently. |
| Workflow integration | Named nowhere in any of the three frozen documents as a Memory Core responsibility. Introducing it now would be exactly the kind of unjustified scope expansion this document's own Scope Lock Principle prohibits. |
| Conversation migration | Conversation History remains its own, separately owned, already production-composed subsystem (Reconciliation §7, §12). Memory Core references it by identifier only (Provenance's `sourceIdentifier`) and never absorbs, copies, or migrates its data. |
| External databases | No storage technology beyond an in-memory implementation is selected for Version 1 (Governance Review §14). |
| Network synchronisation | No multi-node, multi-device, or replication requirement has been identified in any frozen document. This would be speculative infrastructure with no stated purpose. |
| Cloud storage | Same reasoning as External databases — no technology commitment is made; Version 1 is in-process and in-memory, consistent with every other subsystem's existing implementation pattern in this repository. |

---

## 5. Runtime Boundary

Frozen exactly as Contract Design §15 established, restated here as
binding, not merely referenced:

**Runtime owns:** every `PermissionEngine.evaluate` call required before
any `MemoryCore` write and before any sensitive `MemoryRetrieval` read
reaches its requester; publication wiring for the five frozen events
(Section 9) via an `EventBus` dependency Memory Core's own implementation
holds directly; and identity *resolution* (who is asking) via
`IdentityService`, performed before a `requestingPrincipalId` is ever
passed into Memory Core.

**Memory Core owns:** identity assignment for its own five record kinds;
mandatory provenance capture and enforcement; lifecycle and
status-transition validation; immutability enforcement; and structural,
non-semantic retrieval.

**Knowledge Memory owns:** evaluation and promotion decisions over
Memory Core content, and nothing about Memory Core's own record types,
lifecycle, or provenance. Knowledge Memory reads Memory Core; **it never
writes to it.**

**No overlap is permitted.** Specifically, and without exception: Memory
Core shall never perform a permission evaluation; Runtime shall never
construct, validate, or transition a Memory Core record directly,
bypassing `MemoryCore`'s own operations; and Knowledge Memory shall never
mint a new `Entity`, `Document`, `Assertion`, or `Relationship` on Memory
Core's behalf, under any circumstance, including as a side effect of
promotion.

---

## 6. Permission Boundary

Frozen without qualification: **Memory Core never evaluates permissions.
Runtime performs all permission decisions before invoking Memory Core.**

This applies to every operation `MemoryCore` and `MemoryRetrieval`
expose, without exception — creation, amendment, dispute, supersession,
archival, owner-requested deletion, and every one of the seven retrieval
modes. No contract implementing `MemoryCore` or `MemoryRetrieval` may
hold a `PermissionEngine` reference, construct a `PermissionDecision`, or
perform any principal-based filtering of its own (Contract Design §9's
own disclosed correction against today's `MemoryStore.retrieve`
precedent, frozen here permanently for Memory Core).

**This principle shall not be altered during implementation.** If an
implementation unit discovers a case that appears to require Memory Core
to evaluate a permission itself, that is grounds to pause and request a
Scope Lock revision — never grounds to add an internal check under the
belief that it is a small, load-bearing exception.

---

## 7. Provenance Rules

Frozen from Contract Design §7. Every `Entity`, `Document`, `Assertion`,
and `Relationship` record requires a valid `Provenance` reference; no
creation operation may succeed without one.

**Mandatory Provenance fields** (never null; a submission lacking any of
these is rejected outright):

- Identifier (assigned by Memory Core).
- Source identifier.
- Source type.
- Acquisition time.
- Ingestion time (assigned by Memory Core).
- Content nature (with an explicit, valid `UNKNOWN` value — see below).

**Fields that may be unknown** (nullable; a genuinely unknown value is
recorded as absent, never fabricated):

- Creator (free text).
- Creator principal reference.
- Claimed creation time.
- Derived-from references (defaults to empty, not unknown, when there is
  genuinely nothing to reference).
- Extracted-from reference.
- Integrity information.
- Confidence.
- Sensitivity.

**Unknown values must remain explicit.** `contentNature` is a required
field, but `UNKNOWN` is one of its five valid values — a submission that
cannot say whether content is original, extracted, summarised, or
inferred states `UNKNOWN` outright; it never defaults silently to
`ORIGINAL`. Every nullable field above means, when null, "genuinely not
known" — never "not applicable," never a placeholder standing in for a
value someone forgot to supply. No future implementation unit may
introduce a default value for any of these fields to fill a gap in a
submission.

---

## 8. Lifecycle Rules

**Confirmed and frozen**, exactly as `MEMORY_CORE_CONTRACT_DESIGN.md`
§11 designed it. The compressed form given at the start of this task is
confirmed correct as far as it goes; the frozen version below states it
completely, since the compressed form leaves `ARCHIVED`'s and
`DELETED`'s own reachability implicit:

```
ACTIVE ⇄ DISPUTED → SUPERSEDED

ACTIVE, DISPUTED, and SUPERSEDED may each transition to ARCHIVED.
ARCHIVED may transition back to whichever status preceded it
  (archival is reversible).

Any status (ACTIVE, DISPUTED, SUPERSEDED, or ARCHIVED)
  may transition to DELETED.
DELETED is terminal. No transition out of DELETED exists.
```

Applies identically to `Entity`, `Document`, `Assertion`, and
`Relationship` — one shared status type, not four independent ones.

**No changes are recommended to the lifecycle Contract Design already
designed.** The two simplifications that document already made relative
to this task's own original example lifecycle — collapsing `Created`/
`Registered` into a single entry state, and dropping `Referenced` as a
non-independent, derivable fact rather than a formal status — are
confirmed here as correct and are not revisited. Introducing either of
them now, at the Scope Lock stage, would add structure with no concrete
need identified anywhere in the three frozen documents, contrary to this
document's own Scope Lock Principle.

`DELETED` remains reserved exclusively for the owner-requested erasure
path (Section 6's permission boundary, `PermissionAction.DELETE`) — it
is not a status any ordinary correction, dispute, or supersession act
ever produces.

---

## 9. Event Scope

**Frozen: exactly five events. Only these may be implemented.**

1. `memory.entity_created`
2. `memory.document_registered`
3. `memory.assertion_created`
4. `memory.relationship_created`
5. `memory.record_status_changed`

**Rejected, explicitly, and not to be revisited without a Scope Lock
revision:** `memory.record_amended` and `memory.record_superseded`
(each fully reconstructable from `memory.relationship_created` plus the
corresponding `*_created` event, per Contract Design §13's own
reasoning) and `memory.retrieved` (no concrete need identified; a
volume- and privacy-sensitive default this Programme has twice now
declined to endorse — first in the Governance Review, again in Contract
Design). No other speculative event name — however plausible it might
seem during implementation — may be added without a Scope Lock revision.
An implementation unit that finds itself wanting a sixth event is
required to stop and request one, not to add it under the reasoning that
it is clearly useful.

---

## 10. Retrieval Scope

**Frozen: exactly seven retrieval modes.**

1. Identifier lookup.
2. Entity lookup.
3. Document lookup.
4. Relationship traversal.
5. Metadata filtering.
6. Provenance-aware lookup.
7. Chronological lookup.

**Everything else is excluded** — most pointedly, semantic retrieval of
any kind (already named in Section 4, restated here because it is the
one exclusion most likely to be quietly reintroduced under a different
name, such as "relevance-ranked retrieval" or "smart lookup," during
implementation). Every one of the seven modes above returns records
matching **structural** criteria only — no scoring, no ranking beyond
Section 8's own status/chronological ordering, and no relevance judgment
of any kind. `MemoryRetrieval` performs no principal-based visibility
filtering either (Section 6) — every one of the seven modes returns
whatever structurally matches, unfiltered by who is asking, with
visibility enforced entirely by Runtime before or around the call.

---

## 11. Performance Expectations

No benchmark is defined. The following architectural expectations are
frozen instead:

- **Deterministic behaviour.** Given the same stored state and the same
  query, every one of the seven retrieval modes returns the same result,
  in the same order, every time. No randomness, no wall-clock-dependent
  tie-breaking, and no external I/O influences a retrieval result.
- **Immutable history.** No sequence of operations, however long, ever
  alters an immutable field (Contract Design §12) on an existing record.
  This is an architectural guarantee, not merely an expected practice —
  it is enforced by the simple absence of any operation capable of doing
  otherwise (Section 2, Objective 2).
- **Repeatable retrieval.** A query issued twice against an unchanged
  store returns identical results both times — not merely "similar" or
  "equivalent" results.
- **Stable identifiers.** Every identifier, once assigned, is never
  reassigned, reused, or recycled — including for a `DELETED` record,
  whose identifier remains meaningful to an audit trail after its
  content is gone (mirroring today's `MemoryStore.forget`'s own
  already-correct precedent).
- **No hidden state.** Every observable effect of a `MemoryCore`
  operation is either a returned value, a change visible through
  `MemoryRetrieval`, or one of the five frozen events (Section 9) — never
  a side effect invisible to all three.

---

## 12. Test Scope

Frozen minimum required test surface, restated from Contract Design §16
as binding acceptance requirements, described by behaviour, not
implementation:

- Creation of any Entity, Document, Assertion, or Relationship without a
  valid Provenance reference is rejected.
- No operation exists capable of altering any field Contract Design §12
  names immutable, for any of the four record types.
- `Entity.aliases` accepts only additive changes; no operation removes or
  rewrites an existing alias.
- Creating an `Assertion` produces no observable side effect on any
  other record's status or existence.
- `Provenance.contentNature` defaults to `UNKNOWN`, never `ORIGINAL`,
  when a submission does not specify it.
- Referential integrity is enforced for Memory-Core-owned `Relationship`
  endpoint kinds and is not enforced for external endpoint kinds
  (Conversation, future Knowledge Memory).
- Every lifecycle transition Section 8 defines succeeds; every
  transition it does not define is rejected, including any transition
  out of `DELETED`.
- Every one of the seven retrieval modes (Section 10) respects a
  required, positive `maximumResults` where applicable, and returns
  empty (never throws) for no matches.
- `MemoryRetrieval` holds no `PermissionEngine` dependency and applies no
  principal-based filtering — verified structurally, not merely
  described.
- Each of the five frozen events (Section 9) is published exactly once
  per corresponding act, and no event outside that list is ever
  published under any test scenario exercised.

---

## 13. Success Criteria

Version 1 is complete when, and only when, all of the following are
objectively true:

- Entity, Document, Assertion, and Relationship records can be created
  through `MemoryCore`, each carrying a valid, mandatory Provenance
  reference.
- Provenance is preserved unchanged for the life of every record that
  references it — no field Section 7 names mandatory or optional is ever
  silently altered after creation.
- Retrieval through `MemoryRetrieval` is deterministic and repeatable,
  exactly as Section 11 requires.
- The frozen lifecycle (Section 8) is enforced — every valid transition
  succeeds, every invalid one is rejected.
- The permission boundary (Section 6) is respected — no `PermissionEngine`
  reference exists anywhere inside `MemoryCore` or `MemoryRetrieval`, and
  no internal visibility filtering occurs.
- The five frozen events (Section 9), and no others, are published
  correctly.
- Immutable history is maintained — no operation exists capable of
  rewriting an immutable field; every correction takes the form of a new,
  linked record.

---

## 14. Acceptance Criteria

- Memory Core **SHALL** reject creation of any `Entity`, `Document`,
  `Assertion`, or `Relationship` lacking a valid `Provenance` reference.
- Memory Core **SHALL NOT** expose any operation capable of altering an
  immutable field (Contract Design §12) after a record's creation.
- Memory Core **SHALL NOT** hold, construct, evaluate, or reference a
  `PermissionEngine` decision anywhere within `MemoryCore` or
  `MemoryRetrieval`.
- `MemoryRetrieval` **SHALL NOT** apply principal-based visibility
  filtering internally; visibility enforcement **SHALL** occur entirely
  within Runtime.
- Memory Core **SHALL** publish exactly the five events named in Section
  9, and **SHALL NOT** publish any event outside that list.
- Memory Core **SHALL** support exactly the seven retrieval modes named
  in Section 10, and **SHALL NOT** implement any ranked, scored, or
  semantic retrieval mode.
- Memory Core **SHALL** enforce the lifecycle state machine defined in
  Section 8 and **SHALL** reject any transition not defined within it.
- Memory Core **SHALL NOT** depend on, call, or reference the World
  Model, in either direction.
- Memory Core **SHALL NOT** depend on Knowledge Memory. Knowledge Memory
  **SHALL** depend on Memory Core, never the reverse.
- Knowledge Memory **SHALL NOT** create, amend, or otherwise write any
  Memory Core record, under any circumstance, including as a side effect
  of promotion.
- Memory Core **SHALL** use an in-memory, non-persistent storage
  implementation for Version 1, and **SHALL NOT** introduce any external
  database, cloud storage, or network synchronisation dependency.
- Every `MemoryCore` write operation **SHALL** be independently
  invocable and independently permission-gateable by Runtime — no two
  operations **SHALL** be bundled such that Runtime cannot evaluate a
  permission decision for one without also authorising the other.
- Correction of any record **SHALL** take the form of a new, linked
  record plus a typed `Relationship`; no implementation **SHALL** mutate
  an existing record's immutable fields to express a correction.

---

## 15. Out-of-Scope Register

A complete register of deferred capability, collected from every
exclusion named across the three frozen documents and this one. This
register is the starting point for later Memory programmes — nothing
listed here is rejected permanently; each is deferred to a named future
scope.

| Deferred capability | Deferred to |
| --- | --- |
| OCR, PDF parsing, file import, image analysis, document classification, corruption handling, duplicate detection | Document Handling Programme |
| Evidence Item record type | A future Memory Core unit, once Entity/Document/Provenance/Assertion are real (Governance Review §18) |
| Semantic / embedding-based retrieval | A future, separately-justified read layer alongside `MemoryRetrieval` |
| A `MemoryRetrievalPolicy`-equivalent ranking seam | Same, only if a concrete ranking need is ever identified |
| Knowledge Memory's own read boundary onto Memory Core (mirroring `MemorySource`'s capability-narrowing pattern) | A future Knowledge Memory Contract Design revision (Reconciliation §16, step 4) |
| Renaming today's `MemoryStore` to Knowledge Memory's own vocabulary (`KnowledgeStore` et al.) | The same future Knowledge Memory Contract Design revision |
| Persistent / durable storage (relational, object/file, graph) | A future, separately-justified storage design, once a concrete durability requirement is identified |
| An explicit `UNCLASSIFIED` value on the shared `ResourceSensitivity` enum | A future implementation or Scope Lock decision (Contract Design §19, Open Question 1) |
| Audit-identifier validation mechanics for `requestingPrincipalId` | The Implementation Plan that follows this Scope Lock (Contract Design §19, Open Question 2) |
| `EventBus`/`PermissionEngine`/`IdentityService` composition-ordering in `ParkerRuntime.kt` | The Implementation Plan (Contract Design §19, Open Question 3) |
| World Model integration, in either direction | Permanently out of scope for Memory Core; not deferred, excluded (Reconciliation §10) |
| Conversation absorption into Memory Core (versus the current reference-only relationship) | Left open by the Reconciliation (its Section 12, Question 3) — not scheduled to any specific future unit |
| Workflow integration, network synchronisation, cloud storage | No future Programme currently named; excluded until a concrete requirement is identified |

---

## 16. Risks

- **Risk: Version 1's in-memory-only storage undermines Memory Core's
  own claim to be a "system of record."** A record that does not survive
  a process restart is a weaker guarantee than "system of record"
  implies, especially for the evidence-intelligence use case that
  motivated this Programme (Governance Review §17). **Mitigation:** the
  implementation's own documentation must state this limitation
  explicitly, not by omission (mirroring the Governance Review's own
  recommendation); persistence is named in the Out-of-Scope Register
  above as a required follow-up before Memory Core is relied upon for
  anything genuinely durable, not an optional enhancement.
- **Risk: an external permission-check omission would silently reopen
  the sensitive-record gap this Programme exists to close**, since
  Memory Core itself has no internal safety net by design (Section 6).
  **Mitigation:** the Test Scope (Section 12) requires a structural test
  proving no `PermissionEngine` dependency exists inside Memory Core;
  the future Implementation Plan must additionally specify, and test at
  the composition level, exactly where Runtime's own permission gate is
  wired before any write reaches `MemoryCore`.
- **Risk: open, non-closed classification fields (`entityType`,
  `documentType`, `sourceType`, `relationshipType`) invite inconsistent,
  uncontrolled values over time** (e.g., "person" vs. "Person" vs.
  "individual"), with no central registry enforcing consistency, unlike
  the closed `MemoryCategory` enum used elsewhere in this repository.
  **Mitigation:** not a Version 1 blocker — no registry is introduced
  now, since doing so would add structure with no concrete need yet
  identified. Recorded here so a future unit, once real usage patterns
  exist, can decide whether a lightweight vocabulary convention is
  warranted.
- **Risk: unverified cross-boundary `Relationship` endpoints** (pointing
  at a Conversation Turn or a future Knowledge Memory record) **could
  reference something that never existed or was later removed**, with
  Memory Core never knowing. **Mitigation:** accepted, disclosed, not
  otherwise mitigated for Version 1 — verifying these would require
  Memory Core to depend on external subsystems' own state, directly
  violating the one-directional dependency boundary Section 5 freezes.
  This mirrors `correlationId`'s own long-established, unverified,
  by-identifier-only precedent elsewhere in this repository.
- **Risk: the five frozen events have no consumer yet, inviting a future
  implementer to treat publication as non-load-bearing and skip it.**
  **Mitigation:** Section 12's Test Scope requires each event to be
  proven published for its corresponding act — omission is a test
  failure, not a silent gap.

---

## 17. Recommendation

Every mandatory deliverable (Section 3) traces directly to a contract
`MEMORY_CORE_CONTRACT_DESIGN.md` already designed in full; every
exclusion (Section 4) traces to a reason already established across the
three frozen documents, not invented here; the permission boundary,
lifecycle, event scope, and retrieval scope (Sections 6, 8, 9, 10) are
restatements of already-frozen decisions, not new ones. This document
introduces no new architecture — it only fixes, without ambiguity, the
boundary an implementation must build inside.

```
READY FOR IMPLEMENTATION PLAN
```
