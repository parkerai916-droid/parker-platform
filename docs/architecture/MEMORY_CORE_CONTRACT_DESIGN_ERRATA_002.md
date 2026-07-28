# Memory Core Contract Design — Errata 002

## Status

**Documentation only.** This document amends no field on any already-
implemented record type (`Entity`, `Document`, `Assertion`, `Provenance`,
`Relationship` — Units 1–6, all previously reviewed and approved). It adds
five new, previously-undesignated supporting contract types and resolves
one internal contradiction in how the future `MemoryCore` write interface
must be shaped to honour a rule the Contract Design already froze. Neither
`src/` nor `tests/` is touched by this document. It supersedes no part of
Errata 001, which remains independently valid.

---

## 1. The contradiction

`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` §4–§8 (restated in
`docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md` §15) states that
identity assignment for Memory Core's own record kinds is **"minted
internally, never accepted from a caller."** The same principle appears
per-contract, e.g. §4 for `Entity`: *"Stable identifier — assigned once,
by Memory Core, at creation... never accepted from a caller."*

But every one of `Entity`, `Document`, `Assertion`, `Relationship`, and
`Provenance` (Units 1–6, `src/interfaces/MemoryCore.kt`) declares its own
identifier — `entityId`, `documentId`, `assertionId`, `relationshipId`,
`provenanceId` respectively — as the **first, mandatory, non-defaulted
constructor parameter.** A caller cannot construct any of these five
values without already possessing the identifier that field requires.
Consequently, any write-interface method whose parameter type is one of
these five stored record types (e.g. a hypothetical
`MemoryCore.registerEntity(entity: Entity)`) necessarily receives an
identifier the caller chose — directly contradicting "never accepted
from a caller."

This was invisible through Units 1–6 because none of those units defined
an operation that *submits* a record for creation; each unit built one
already-complete record type in isolation. It became load-bearing only
at Unit 7, the first point where a real submission-shaped method
signature had to be written.

## 2. Why candidate types are required

Two ways exist to make "minted internally" literally true:

- Accept the already-ID'd stored record and simply ignore or overwrite
  the caller-supplied identifier — silently discarding a value the
  caller was nonetheless forced to invent, which is confusing, easy to
  misuse, and leaves the stored type's constructor still lying about
  what's actually required from a submitter.
- Give the caller a second, ID-less shape to submit, and have Memory
  Core construct the identified, stored record internally. The caller's
  input type structurally cannot contain an identifier, so "never
  accepted from a caller" is enforced by the type system rather than by
  a promise in prose.

The second is the only option that removes the contradiction rather than
papering over it, and it does so without weakening any already-approved
field on the stored record types themselves.

## 3. Repository precedent

This is not a new pattern for Parker. Today's `MemoryStore` already
solves exactly this problem, exactly this way:

```
CandidateMemory
    ↓
MemoryStore.remember(...)
    ↓
MemoryRecord
```

`CandidateMemory` carries the caller-supplied content only; `remember`
mints the identifier and returns the durable, identified `MemoryRecord`.
The candidate types introduced below extend this existing, already-
accepted repository convention to Memory Core's five record kinds rather
than inventing a new one.

## 4–6. Field-by-field candidate shapes, Memory Core-owned fields, caller-owned fields

For each stored type, every field is classified as **caller-supplied**
(carried into the candidate type unchanged) or **Memory Core-owned**
(excluded from the candidate type; assigned internally at creation).
Classification rule applied uniformly: a record's own identifier, and
any timestamp or status-shaped field whose default value represents "the
state of a record at the moment Memory Core creates it," is Memory
Core-owned. Everything else — every field expressing something the
submitter is asserting or providing — is caller-supplied.

### CandidateEntity (from `Entity`, §4)

| Field | Ownership |
|---|---|
| `entityId` | Memory Core-owned — excluded |
| `entityType` | caller-supplied |
| `primaryLabel` | caller-supplied |
| `provenanceId` | caller-supplied (references an already-recorded `Provenance`) |
| `createdAt` | Memory Core-owned — excluded |
| `aliases` | caller-supplied |
| `relatedPrincipalId` | caller-supplied |
| `status` | Memory Core-owned — excluded (always begins `ACTIVE`) |
| `metadata` | caller-supplied |

`CandidateEntity` = `entityType`, `primaryLabel`, `provenanceId`,
`aliases`, `relatedPrincipalId`, `metadata`.

### CandidateDocument (from `Document`, §5)

| Field | Ownership |
|---|---|
| `documentId` | Memory Core-owned — excluded |
| `documentType` | caller-supplied |
| `locationReference` | caller-supplied |
| `provenanceId` | caller-supplied |
| `registeredAt` | Memory Core-owned — excluded |
| `integrityHash` | caller-supplied |
| `processingStatus` | caller-supplied (see note) |
| `status` | Memory Core-owned — excluded (always begins `ACTIVE`) |
| `metadata` | caller-supplied |

Note on `processingStatus`: this is not the record's lifecycle status —
that is the separate `status: MemoryCoreRecordStatus` field, which
Memory Core alone governs under the frozen lifecycle rules.
`processingStatus` instead expresses the submitter's own knowledge or
intent about external processing (e.g. a caller registering a document
that will never need processing may supply `NOT_APPLICABLE` directly).
It is therefore classified caller-supplied-optional, defaulting to
`REGISTERED` when omitted, consistent with `Document`'s own existing
default.

`CandidateDocument` = `documentType`, `locationReference`,
`provenanceId`, `integrityHash`, `processingStatus`, `metadata`.

### CandidateAssertion (from `Assertion`, §6)

| Field | Ownership |
|---|---|
| `assertionId` | Memory Core-owned — excluded |
| `statement` | caller-supplied |
| `provenanceId` | caller-supplied |
| `confidence` | caller-supplied |
| `status` | Memory Core-owned — excluded (always begins `ACTIVE`) |
| `metadata` | caller-supplied |

`Assertion` has no independent creation timestamp (a deliberate Unit 5
decision), so there is no timestamp to exclude here.

`CandidateAssertion` = `statement`, `provenanceId`, `confidence`,
`metadata`.

### CandidateRelationship (from `Relationship`, §8, as corrected by Errata 001)

| Field | Ownership |
|---|---|
| `relationshipId` | Memory Core-owned — excluded |
| `relationshipType` | caller-supplied |
| `fromEndpoint` | caller-supplied |
| `toEndpoint` | caller-supplied |
| `directional` | caller-supplied (no default, per the existing deliberate design) |
| `provenanceId` | caller-supplied |
| `createdAt` | Memory Core-owned — excluded |
| `status` | Memory Core-owned — excluded (always begins `ACTIVE`) |

`Relationship` carries no metadata field (Errata 001, Correction 2), so
none is excluded or carried here.

`CandidateRelationship` = `relationshipType`, `fromEndpoint`,
`toEndpoint`, `directional`, `provenanceId`.

## 7. Whether Provenance requires the same treatment

**Yes.** `Provenance` is explicitly one of the *"five record kinds"*
named in Implementation Plan §15 whose identity Memory Core mints
internally — the exception clause in this task's own instructions
("unless the existing contract explicitly requires Memory Core to mint
its identifier through the same write surface") is therefore triggered.
`Provenance`'s own constructor requires `provenanceId: ProvenanceId` as
its first, mandatory, non-defaulted parameter — the identical structural
contradiction as the other four types. Leaving it unresolved here would
be exactly the "second hidden inconsistency" this task warned against.

### CandidateProvenance (from `Provenance`, §7)

| Field | Ownership |
|---|---|
| `provenanceId` | Memory Core-owned — excluded |
| `sourceIdentifier` | caller-supplied |
| `sourceType` | caller-supplied |
| `acquisitionTime` | caller-supplied (when the source information was itself acquired) |
| `ingestionTime` | Memory Core-owned — excluded (the moment Memory Core records it — structurally the same role as `createdAt`/`registeredAt` elsewhere) |
| `contentNature` | caller-supplied (mandatory; no default, per the existing deliberate design) |
| `creator` | caller-supplied |
| `creatorPrincipalId` | caller-supplied |
| `claimedCreationTime` | caller-supplied |
| `derivedFrom` | caller-supplied |
| `extractedFrom` | caller-supplied |
| `processingHistory` | caller-supplied |
| `integrityInformation` | caller-supplied |
| `confidence` | caller-supplied |
| `sensitivity` | caller-supplied |

`Provenance` has no `status` field, so none is excluded on that basis.

`CandidateProvenance` = `sourceIdentifier`, `sourceType`,
`acquisitionTime`, `contentNature`, `creator`, `creatorPrincipalId`,
`claimedCreationTime`, `derivedFrom`, `extractedFrom`,
`processingHistory`, `integrityInformation`, `confidence`, `sensitivity`.

Because every other candidate type's `provenanceId` field references an
**already-recorded** `Provenance`, `CandidateProvenance` must be
submitted, and its `MemoryCore.createProvenance` operation completed,
*before* any `CandidateEntity`/`CandidateDocument`/`CandidateAssertion`/
`CandidateRelationship` referencing it can be submitted. This is not a
new rule — it is the same dependency ordering already established by the
Implementation Plan's own corrected 9-unit build sequence, now carried
into the write interface's own call order.

## 8. Revised MemoryCore method signatures (design level)

No Kotlin is written by this document; the following states shape only,
for Unit 7 to implement:

```
MemoryCore.createProvenance(candidate: CandidateProvenance): Provenance
MemoryCore.createEntity(candidate: CandidateEntity): Entity
MemoryCore.registerDocument(candidate: CandidateDocument): Document
MemoryCore.createAssertion(candidate: CandidateAssertion): Assertion
MemoryCore.createRelationship(candidate: CandidateRelationship): Relationship
```

`createProvenance` — not `recordProvenance` — per the user's own
amendment to this errata. Naming consistency favours `create*` for
`Provenance`, `Entity`, `Assertion`, and `Relationship`, each of which
brings a genuinely new record into existence. `registerDocument`
deliberately keeps its distinct verb: a `Document` records the
*registration* of an external artefact Memory Core did not itself
originate, not the *creation* of that artefact — the same distinction
already implicit in `Document`'s own field name, `registeredAt`, as
opposed to `createdAt` on `Entity` and `Relationship`. The shared
status-transition operation (covering dispute, supersession, archival,
and owner-erasure across all five kinds) is unaffected by this errata —
it accepts an existing record's kind and identifier, never a candidate,
and was already understood to be a separate operation.

## 9. Units 1–6 stored record types remain valid

`Entity`, `Document`, `Assertion`, `Provenance`, and `Relationship`
(`src/interfaces/MemoryCore.kt`) require **no field addition, removal,
or modification.** Every field on every one of these five types remains
exactly as implemented and approved in Units 1–6. What changes is only
which *interface method* a caller uses to bring a new instance of one of
these types into existence — the types themselves, and everything
already tested against them (`MemoryCoreContractsTest.kt`,
`ProvenanceTest.kt`, `EntityTest.kt`, `DocumentTest.kt`,
`AssertionTest.kt`, `RelationshipTest.kt`), remain fully valid and
require no revision.

## 10. Implementation impact on Unit 7 only

Unit 7 must now, in addition to its already-specified scope, define five
new candidate types (`CandidateProvenance`, `CandidateEntity`,
`CandidateDocument`, `CandidateAssertion`, `CandidateRelationship`) in
`src/interfaces/MemoryCore.kt`, and shape the `MemoryCore` write
interface's methods to accept candidates and return completed stored
records, per Section 8 above. No other unit is affected: `MemoryRetrieval`
and its request/result types (the other half of Unit 7's scope) have no
relationship to this contradiction, since retrieval never constructs a
new record. Units 8 and 9 (`InMemoryMemoryCore`, event publication) are
unaffected in scope, though Unit 8's implementation will naturally accept
candidates rather than stored records as a direct consequence of this
errata, once reached.

---

## What this document does not do

It does not add a new record kind, retrieval mode, event, permission
rule, persistence behaviour, or lifecycle behaviour. It does not change
any field on any of the five already-implemented stored record types. It
does not authorise any change to `MEMORY_CORE_SCOPE_LOCK.md`, which
remains frozen and normative, unaffected by this errata. It does not
reopen Units 1–6, and it does not by itself implement anything — Unit 7
still carries out the actual construction of these five candidate types
and the revised `MemoryCore` interface.

---

```
ERRATA ACCEPTED — UNIT 7 MAY PROCEED
```
