# Memory Core Contract Design — Errata 004

## Status

**Interface amendment.** Unlike Errata 001–003 (documentation-only or
purely additive), this errata amends the signatures of eleven already-
implemented, already-approved methods across `MemoryCore` and
`MemoryRetrieval` (Unit 7, amended by the Errata 003 follow-up). It adds
no new record kind, retrieval mode, event, lifecycle state, or
permission action/resource type. Neither `src/` nor `tests/` is touched
by this document — the amendment is recorded here; Unit 10 itself carries
it out.

---

## 1. The contradiction

Unit 10 requires `PermissionGatedMemoryCore` to evaluate, before every
write, "may `requestingPrincipalId` perform this action" — and requires
`PermissionFilteredMemoryRetrieval` to evaluate the same question before
returning any result. Both decorators are specified to implement
`MemoryCore`/`MemoryRetrieval` directly (the same delegation-chain shape
`EventPublishingMemoryCore` already uses). But neither interface's
methods carry a caller-identity parameter everywhere they would need
one:

- All five `MemoryCore` creation operations take only a `Candidate*`
  value — none of the five candidate types carries a
  `requestingPrincipalId` (Errata 002's own field-by-field tables
  confirm this: each candidate is deliberately caller-*content*-only).
- `MemoryCore.transitionStatus` takes only a `MemoryCoreRecordReference`
  and a target status — no principal.
- `MemoryRetrieval`'s four direct identifier-lookup methods
  (`getEntity`/`getDocument`/`getAssertion`/`getRelationship`) take only
  the target identifier — no principal. (The six query-based retrieval
  methods are unaffected: `EntityLookupQuery` and its five siblings
  already carry `requestingPrincipalId`, added in Unit 7 "for
  auditability only.")

A decorator cannot form the question "may this Principal act on this
Resource" without a Principal to name. This was invisible through Units
7–9 because none of them evaluated a permission decision — the design
was deliberately permission-neutral until Unit 10 became the first point
where a real per-caller decision had to be made.

## 2. Why candidate identity is rejected

A `Candidate*` type describes *what a caller is proposing to store* --
never *who is asking, with what authority*. Folding a
`requestingPrincipalId` into `CandidateEntity`/`CandidateDocument`/etc.
would conflate two genuinely different concerns Contract Design has kept
separate since Section 3's earliest cross-cutting decisions: content
(what the record says) and authority (who may cause it to be stored).
It would also create an asymmetry with `MemoryCoreRecordReference`
(Errata 003), which deliberately carries no such field either, for the
identical reason. Keeping candidates identity-free means the same
`CandidateEntity` value is byte-for-byte reusable regardless of which
Principal ultimately submits it — a content description, not a signed
request.

## 3. Why principal-scoped factories are rejected

A factory producing a fresh, principal-bound `MemoryCore`/`MemoryRetrieval`
instance per caller would avoid touching either interface's signatures,
but it replaces one long-lived, singly-composed Runtime service with an
unbounded number of short-lived ones, each closing over an ambient
identity no call site can see just by reading the call. That is the
"hidden ambient identity" this errata's own constitutional reason
section rules out: two calls that look identical at the call site could
run under different authority depending on which factory-produced
instance happens to be in scope, which is exactly the non-determinism
and non-auditability an explicit parameter avoids. It would also leave
`ParkerRuntime`'s own composition graph — "Memory Core simply becomes
available as a runtime service" — meaning something different from every
other service `ParkerRuntime` composes today, none of which are
principal-scoped factories.

## 4. Amended write signatures

All six, exactly as specified:

```
suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance
suspend fun createEntity(requestingPrincipalId: PrincipalId, candidate: CandidateEntity): Entity
suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument): Document
suspend fun createAssertion(requestingPrincipalId: PrincipalId, candidate: CandidateAssertion): Assertion
suspend fun createRelationship(requestingPrincipalId: PrincipalId, candidate: CandidateRelationship): Relationship
suspend fun transitionStatus(requestingPrincipalId: PrincipalId, reference: MemoryCoreRecordReference, targetStatus: MemoryCoreRecordStatus): MemoryCoreRecord
```

`requestingPrincipalId` is the new first parameter on every operation --
consistent ordering, so every `MemoryCore` call reads "as this Principal,
do this" left to right. `InMemoryMemoryCore` (Unit 8) accepts and
threads the parameter through unchanged but never branches on it, never
filters with it, and never stores it anywhere a record's own fields
don't already call for it -- it remains exactly as permission-neutral as
before this amendment, just no longer permission-*blind* (it now has the
information a wrapper needs; it still does nothing with it itself).

## 5. Amended direct-read signatures

All four, exactly as specified:

```
suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity?
suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document?
suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion?
suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship?
```

Return types are unchanged (`Entity?`, `Document?`, `Assertion?`,
`Relationship?`) -- see Section 8 below for why the nullable shape itself
is now load-bearing for non-disclosure, not just for "not found."

## 6. Query-based retrieval identity, clarified

`EntityLookupQuery`, `DocumentLookupQuery`, `RelationshipTraversalQuery`,
`ChronologicalLookupQuery`, `MetadataLookupQuery`, and
`ProvenanceLookupQuery` require no signature change -- each already
carries `requestingPrincipalId`. Unit 7 documented that field as
existing "for auditability only... never as a filter this contract
applies on its own." That restriction was correct for Units 7–9 (no
decorator existed yet to apply it as anything else) and remains correct
for `InMemoryMemoryCore` today (it still must never filter on it). It
now serves a second, additional purpose starting with Unit 10:
`PermissionFilteredMemoryRetrieval` reads the same field to know which
Principal each returned record must be checked against. Nothing about
the field's shape, requiredness, or validation changes -- only which
code is permitted to read it for a purpose beyond audit.

## 7. Permission-action / resource-type mapping, and how resources are represented

Frozen exactly as specified:

| Operation | Action | Resource type |
|---|---|---|
| `createProvenance` | `WRITE` | `MEMORY` |
| `createEntity` | `WRITE` | `MEMORY` |
| `createAssertion` | `WRITE` | `MEMORY` |
| `createRelationship` | `WRITE` | `MEMORY` |
| `registerDocument` | `WRITE` | `DOCUMENT` |
| `transitionStatus`, target ≠ `DELETED` | `WRITE` | `MEMORY`/`DOCUMENT` (per the transitioned record's own kind) |
| `transitionStatus`, target = `DELETED` | `DELETE` | `MEMORY`/`DOCUMENT` (per the transitioned record's own kind) |
| Retrieval of a `Document` | `READ` | `DOCUMENT` |
| Retrieval of any other Memory Core record | `READ` | `MEMORY` |

No new `PermissionAction` or `ResourceType` value is introduced --
`WRITE`, `READ`, and `DELETE` (`src/contracts/Permission.kt`) and
`MEMORY`/`DOCUMENT` (`src/contracts/Resource.kt`) already exist and are
unused today outside this Programme, exactly as the Governance Review
originally found.

**The existing `PermissionEngine` input shape**, confirmed by reading
`PermissionEngine.evaluate` (`src/interfaces/PermissionEngine.kt`) and
its only real implementation, `DefaultPermissionEngine` →
`DefaultPermissionPolicy` (`src/runtime/DefaultPermissionPolicy.kt`), is
`evaluate(request: ExecutionRequest): PermissionDecision`.
`ExecutionRequest.targetResources` is a `List<ResourceId>`, and
`DefaultPermissionPolicy.evaluate` resolves it by calling
`resourceRegistry.resolve(it)` for each entry, keeping only the ones the
Resource Registry actually has -- "if something is not represented
within the Resource Registry, Parker assumes it is inaccessible"
(`Resource.kt`'s own quoted invariant). It then derives the
`(PermissionAction, ResourceType)` pairs to check from those *resolved*
resources' own `resourceType`, via `ActionMapper`.

**This means an unregistered `ResourceId` and an empty `targetResources`
list behave identically** -- both resolve to an empty `resourceTypes`
set. Memory Core records are not, and (per this Unit's own scope) will
not become, entries in the Resource Registry -- Memory Core has always
been its own subsystem, never wired to `ResourceRegistry` (Governance
Review's own finding, unchanged since). This is true not only for
creation (no identifier exists yet) but equally for an *already-existing*
Entity/Document/Assertion/Relationship being read or transitioned --
none of them is a `Resource`, so none of them ever has a `ResourceId` a
`ResourceRegistry` could resolve.

The correct, honest representation is therefore: **`targetResources` is
always `emptyList()`** for every Memory Core permission check, creation
or otherwise -- there is never a Resource Registry entry to name, at any
point in a Memory Core record's lifecycle. `proposedActions` carries one
descriptive string per operation (mirroring the existing
`action-mapping.md`/`ActionMapper` convention, e.g.
`"memory.create_entity"`, `"memory.register_document"`,
`"memory.transition_status"`, `"memory.retrieve"`), which
`PermissionGatedMemoryCore`/`PermissionFilteredMemoryRetrieval` populate
from the frozen table above, not from a Resource Registry lookup.

**Consequence this errata discloses rather than hides:** because
`DefaultPermissionPolicy.evaluate`'s own resolution path depends on
`resourceRegistry.resolve` succeeding to derive a `resourceType`, and no
Memory Core call will ever have a resolvable target Resource, routing
Memory Core's checks through *that exact, already-wired,
production* `DefaultPermissionPolicy` instance (the one
`DefaultExecutionPipeline` uses) would deny every Memory Core operation
unconditionally, regardless of the frozen mapping table above -- not a
policy decision, simply an artifact of a resolution path built for a
different caller shape. Correctly evaluating the frozen mapping table
therefore requires `ParkerRuntime` composition (Unit 10's own scope,
"production wiring") to supply `PermissionGatedMemoryCore`/
`PermissionFilteredMemoryRetrieval` with a `PermissionEngine` (or
`DefaultPermissionPolicy` `rules: List<PermissionPolicyRule>` set)
capable of deciding `(WRITE, MEMORY)`, `(WRITE, DOCUMENT)`,
`(READ, MEMORY)`, `(READ, DOCUMENT)`, `(DELETE, MEMORY)`, and
`(DELETE, DOCUMENT)` without requiring a resolved target Resource --
this is a Runtime-composition wiring choice (which rules/policy instance
Memory Core's decorators are constructed with), not a change to
`PermissionEngine`'s interface, `ExecutionRequest`'s shape, or any
existing production `DefaultPermissionPolicy` instance already serving
the Execution Pipeline. No new permission action or resource type is
required to make this work -- only a policy/engine composition decision,
made once, at `ParkerRuntime` construction.

## 8. Direct-read non-disclosure behaviour

**Recommendation: `null`, not a distinguishable denial result.**
`getEntity`/`getDocument`/`getAssertion`/`getRelationship` keep their
existing nullable return shape (Section 5) -- they are not changed to a
sealed `EntityLookupResult`/similar type carrying a `Denied` case. This
is not merely convenient; it is the only shape available that already
satisfies "must not leak the existence of a protected record" by
construction: `Entity?` already collapses "no such record" and "record
exists, not shown" into the identical, indistinguishable absence signal
a caller receives either way. Introducing any second, distinguishable
"denied" outcome (a sealed type, a thrown exception distinct from a
generic error, a sentinel value) would itself be the leak -- a caller
could tell "protected record" apart from "no record" by which shape
came back. This matches existing Parker convention:
`InMemoryMemoryStore.retrieve` already treats "nothing visible to this
requester" and "nothing exists" identically (an empty list, never a
distinguishable reason), and `MemoryStore.forget`'s own `Boolean` return
was deliberately kept boolean rather than gaining a `NotFound` case for
a similar "do not over-distinguish absence" reason (`MemoryStore.kt`'s
own file header KDoc). `PermissionFilteredMemoryRetrieval` therefore
returns `null` both when `InMemoryMemoryCore` itself returns `null` and
when it returns a record the requesting Principal is not permitted to
see.

## 9. Decorator composition consequences

`PermissionGatedMemoryCore : MemoryCore`, wrapping a `MemoryCore`
delegate (in the frozen stack, `EventPublishingMemoryCore`) exactly as
`EventPublishingMemoryCore` itself wraps `InMemoryMemoryCore` --
unchanged from Unit 10's own original instructions, now unblocked. Each
of its six operations: builds an `ExecutionRequest` per Section 7 above
using the now-explicit `requestingPrincipalId`; calls
`permissionEngine.evaluate(request)`; on `APPROVED`/
`APPROVED_WITH_CONFIRMATION`, calls the identically-named delegate
operation (now also carrying `requestingPrincipalId`, passed straight
through) and returns its result unchanged; on `DENIED`/`DEFERRED`,
throws rather than reaching the delegate at all (mirroring this
Programme's own established "hard failure for a rejected precondition,
never a silently-produced partial record" discipline) -- meaning no
event is published either, since `EventPublishingMemoryCore` sits
*below* `PermissionGatedMemoryCore` in the frozen stack and is never
reached for a denied call.

`PermissionFilteredMemoryRetrieval : MemoryRetrieval`, wrapping
`InMemoryMemoryCore` directly (the frozen stack has no
`EventPublishingMemoryCore` equivalent on the read side, since retrieval
was already established to publish nothing). Its four identifier-lookup
methods: retrieve unconditionally from the delegate, then evaluate
permission against the actual retrieved record's own kind (Section 7's
Document/other-kind split), returning the record if permitted or `null`
otherwise (Section 8). Its six query-based methods: retrieve the full,
unfiltered structural match set from the delegate (using
`query.requestingPrincipalId` for the query itself, unchanged), then
evaluate each result individually and keep only the permitted ones --
never filtering, ranking, or otherwise altering `InMemoryMemoryCore`'s
own structural matching, only removing entries the requester may not
see.

## 10. Implementation impact on Units 7–10

- **`MemoryCore`** (Unit 7): six method signatures gain a leading
  `requestingPrincipalId: PrincipalId` parameter. No method added,
  removed, or renamed.
- **`MemoryRetrieval`** (Unit 7): four method signatures
  (`getEntity`/`getDocument`/`getAssertion`/`getRelationship`) gain a
  leading `requestingPrincipalId: PrincipalId` parameter. The six
  query-based methods are unchanged (Section 6).
- **`EventPublishingMemoryCore`** (Unit 9): every overridden method's
  signature changes to match `MemoryCore`'s amended shape and threads
  `requestingPrincipalId` straight to its own delegate call; no change
  to which events it publishes, when, or with what payload.
- **`InMemoryMemoryCore`** (Unit 8): every overridden method's signature
  changes to match; the parameter is accepted and passed through where
  a delegate call requires it (there is none currently -- this class is
  the base of the stack) but is not read for any filtering, authorising,
  or content decision anywhere in its body.
- **Their existing tests**: `MemoryCoreInterfacesTest.kt`,
  `MemoryCoreCandidatesTest.kt` (signature-shape assertions only, no
  candidate-type change), `InMemoryMemoryCoreTest.kt`, and
  `EventPublishingMemoryCoreTest.kt` all require call-site updates
  wherever they invoke one of the ten amended methods.

**Candidate types and stored record types are unchanged** -- `Entity`,
`Document`, `Assertion`, `Relationship`, `Provenance`,
`CandidateEntity`/`CandidateDocument`/`CandidateAssertion`/
`CandidateRelationship`/`CandidateProvenance`, and
`MemoryCoreRecordReference` all keep every field they have today. **No
stored data migration is required** -- nothing about how a record is
represented once created changes; only how a caller requests its
creation, transition, or retrieval.

## 11. No new authority channel is created

This errata adds exactly one new way for authority to enter a Memory
Core call -- an explicit `requestingPrincipalId` parameter, evaluated by
the same `PermissionEngine`/`ExecutionRequest` mechanism every other
permission-checked operation in this repository already uses (Section
7). It does not add a second identity system, a bypass, a trusted-caller
allowlist, an internal-only unchecked path, or any implicit/ambient
source of authority. Every write and every direct read passes through
exactly one explicit parameter and exactly one existing
`PermissionEngine`; there is no other route into either interface.

---

```
ERRATA ACCEPTED — INTERFACE AMENDMENT REQUIRED BEFORE UNIT 10
```
