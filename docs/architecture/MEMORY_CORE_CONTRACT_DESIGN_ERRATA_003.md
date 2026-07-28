# Memory Core Contract Design — Errata 003

## Status

**Documentation only.** This document adds no field to any already-
implemented record type and changes nothing already approved in Errata
001 or Errata 002, both of which remain independently valid. It resolves
one gap Implementation Unit 7 itself disclosed: the frozen Scope Lock
requires lifecycle management, but the `MemoryCore` interface Unit 7
implemented carries no operation capable of performing a lifecycle
transition. Neither `src/` nor `tests/` is touched by this document.

---

## 1. The issue, restated precisely

`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` Section 8 (unchanged since
the Contract Design first froze it) requires:

```
ACTIVE <-> DISPUTED -> SUPERSEDED
ARCHIVED reversible from ACTIVE, DISPUTED, and SUPERSEDED
DELETED terminal and owner-erasure-only
```

`MemoryCore` (Unit 7, `src/interfaces/MemoryCore.kt`) exposes exactly
five operations -- `createProvenance`, `createEntity`,
`registerDocument`, `createAssertion`, `createRelationship` -- none of
which can change an already-stored record's `status`. Unit 7's own
completion report flagged this gap explicitly rather than inventing a
signature to fill it, per that Unit's own instructions.

One correction to this task's own framing, worth stating plainly before
the analysis below: the lifecycle status this errata concerns applies to
**four** record kinds, not five. `Provenance` carries no `status` field
at all -- it was never one of the four types Contract Design Section 11
and this file's own `MemoryCoreRecordStatus` KDoc describe as sharing
the lifecycle ("the lifecycle status shared identically by every Memory
Core record type -- `Entity`, `Document`, `Assertion`, `Relationship`").
A lifecycle-transition operation therefore has nothing to do to a
`Provenance` record, and must not be shaped as if it did.

---

## 2. Required analysis

### 2.1 Whether lifecycle changes require a public `MemoryCore` operation

Yes. `MemoryCore` is Memory Core's "single public write surface"
(Contract Design Section 3) -- no other public interface exists through
which any Memory Core record's stored state can change. Since a
lifecycle transition changes a stored record's `status` field, and
Scope Lock Section 8 requires that capability to exist in V1, the only
place it can live is a `MemoryCore` operation.

### 2.2 The minimum operation shape needed

One operation: a target record reference, plus the desired target
`MemoryCoreRecordStatus`, returning the record in its new state. Nothing
about the frozen lifecycle table requires more information than "which
record" and "what target status" -- the *reason* for a transition
(which Assertion disputes a record, which new record supersedes it) is
already captured separately, by a `Relationship` record naming the
transitioned record as an endpoint (`DISPUTES`/`SUPERSEDES`/`AMENDS`),
per Contract Design Section 3's own "no embedded relationship/source
fields" decision, already applied consistently everywhere else in this
file. The transition operation itself needs no parameter carrying that
reasoning.

### 2.3 Whether one generic transition operation is sufficient

Yes. The frozen lifecycle is a closed, already-fully-specified table of
valid `(fromStatus, targetStatus)` pairs
(`ACTIVE<->DISPUTED->SUPERSEDED`; `ARCHIVED` reversible from three named
statuses; `DELETED` reachable from any status, terminal). A single
operation taking a target status and validating it against that table is
sufficient to express every transition Scope Lock Section 8 names. No
transition needs information a generic operation could not carry.

### 2.4 Whether deletion requires a distinct operation because it is owner-erasure-only

No. "Owner-erasure-only" describes **who may authorise** a transition to
`DELETED`, not **how** that transition is structurally represented --
and this task's own Boundaries section is explicit that `MemoryCore`
"must not evaluate permissions" and "must not decide whether the caller
is the owner." Authorisation for a `DELETED` transition specifically
(as opposed to any other target status) is therefore entirely a Runtime
concern, enforced *before* this operation is ever invoked -- most
naturally, by the future `PermissionGatedMemoryCore` decorator (not yet
built) requiring a stronger permission action
(`PermissionAction.DELETE`) when the requested target status is
`DELETED`, versus whatever weaker action gates other transitions. From
`MemoryCore`'s own vantage point, `DELETED` is structurally just another
valid target status reachable from any current status -- it carries no
special shape, only a special authorisation requirement that lives
entirely outside this interface. One generic operation covers deletion
without any special-casing.

### 2.5 How the operation identifies the four lifecycle-bearing record kinds

A dedicated reference type is needed, but it should not simply mirror
[RelationshipEndpoint]'s `(recordKind: String, recordId: String)` shape.
`RelationshipEndpoint`'s loose, string-typed coupling exists for a
specific reason: a `Relationship` must be able to name endpoints Memory
Core does not own and has no compile-time dependency on (`conversation-
turn`, `knowledge-record`). A lifecycle transition has no such need --
it can only ever target one of Memory Core's own four lifecycle-bearing
record kinds, every one of which already has a proper, validated
identifier type (`EntityId`, `DocumentId`, `AssertionId`,
`RelationshipId`). Reusing `RelationshipEndpoint`'s loose string typing
here would discard type safety the codebase already has available for
no benefit.

The stronger, better-fitting shape is a small sealed type over the four
existing typed identifiers, mirroring the shape `MemoryCoreRecord`
(Unit 7's own result wrapper) already uses on the *output* side:

```
sealed class MemoryCoreRecordReference {
    data class ToEntity(val entityId: EntityId) : MemoryCoreRecordReference()
    data class ToDocument(val documentId: DocumentId) : MemoryCoreRecordReference()
    data class ToAssertion(val assertionId: AssertionId) : MemoryCoreRecordReference()
    data class ToRelationship(val relationshipId: RelationshipId) : MemoryCoreRecordReference()
}
```

This is compile-time exhaustive (no possibility of an unsupported kind,
a typo'd kind string, or a kind this operation has no authority over,
like `conversation-turn`, ever being passed), requires no independent
blank-validation of its own (each case delegates entirely to its
already-validated identifier value class), and reads as a deliberate,
consistent counterpart to `MemoryCoreRecord` rather than an unrelated
new shape.

### 2.6 What completed record or result it returns

`MemoryCoreRecord` (Unit 7's existing sealed wrapper --
`OfEntity`/`OfDocument`/`OfAssertion`/`OfRelationship`) already suffices
without any change. This directly confirms the return type in this
task's own preferred-direction example is correct as given.

### 2.7 How invalid transitions are represented

As a thrown `IllegalStateException`, not a sealed result type. This
follows the failure-handling discipline the Implementation Plan already
froze (Section 11): a state-precondition violation -- an already-
`DELETED` record targeted again, a target status unreachable from the
record's current status per the frozen table -- is a caller error against
an already-fully-specified rule, not a legitimate competing business
outcome the way `MemoryPromotionDecision.Reject` is for a genuinely
judged, multi-factor Memory promotion decision. This mirrors
`ConversationEngine.submitTurn`'s own "thrown, never silently repaired"
precedent for state mismatch, and needs no new sealed type.

### 2.8 Whether amendment is part of V1 or remains excluded

Amendment remains in V1 scope, exactly as Contract Design Section 12
already described it -- but it is not, and must not become, a dedicated
`MemoryCore` operation of its own. Section 12's own amendment process is
a caller-orchestrated sequence of operations `MemoryCore` already has:
create the replacement record, record an `AMENDS`-typed `Relationship`
connecting it to the original, and, where appropriate, transition the
original to `SUPERSEDED` via this same lifecycle operation. This is
exactly why this task's own Boundaries section requires the lifecycle
operation to "not perform amendments" and "not create replacement
records automatically": amendment is composed from primitives that
already exist (or, after this errata, will exist) on `MemoryCore` --
it is never a side effect the lifecycle operation itself produces.

### 2.9 Whether event publication remains tied to successful transitions only

Yes, unchanged from the architecture already frozen. The lifecycle
operation itself must not publish `memory.record_status_changed` (this
task's own Boundaries section: "must not publish events itself at the
interface level") -- publication is `InMemoryMemoryCore`/Runtime's
responsibility (Unit 8/Unit 9), fired only after a transition completes
successfully. A thrown, rejected transition produces no event, since
nothing changed to record.

### 2.10 Whether Unit 7 requires a small additive interface amendment

Yes -- see Section 4 below.

---

## 3. Assessment of the preferred minimal direction

```
transitionStatus(recordReference, targetStatus): MemoryCoreRecord
```

This shape is correct in outline and is adopted, with one refinement:
`recordReference` should be the sealed `MemoryCoreRecordReference` type
in Section 2.5 above, not a loosely-typed `(kind, id)` pair. With that
refinement, the shape:

- **represents all supported record kinds**: exactly the four that
  genuinely carry a lifecycle (`Entity`, `Document`, `Assertion`,
  `Relationship`), compile-time exhaustively, with no fifth case for
  `Provenance` to be silently or incorrectly implied;
- **represents the frozen lifecycle**: `targetStatus:
  MemoryCoreRecordStatus` accepts any of the five status values;
  validity against the current status is `MemoryCore`'s own
  implementation's job (a future Unit), exactly as
  `MemoryCoreRecordStatus`'s own KDoc already states ("valid
  transitions... enforced by MemoryCore's own implementation, never by
  this enum itself") -- this operation's signature does not, and should
  not, attempt to encode the transition graph in types;
- **represents deletion restrictions**: correctly, by carrying no
  special case for `DELETED` at all -- authorisation is entirely
  external, per Section 2.4;
- **supports deterministic result handling**: a successful call returns
  the one, unambiguous updated record via `MemoryCoreRecord`; a failed
  call throws, per Section 2.7 -- no partial or ambiguous result shape
  exists.

No separate `dispute`/`archive`/`supersede`/`delete` methods are
required or recommended -- nothing in the frozen governance names a
transition whose validity depends on anything beyond the `(fromStatus,
targetStatus)` pair this one operation already fully captures.

---

## 4. Unit 7 impact

Unit 7 requires a small, strictly additive amendment. Nothing already
approved in Unit 7 changes:

| Addition | Type |
|---|---|
| A record-reference type | **Yes** -- `MemoryCoreRecordReference` (Section 2.5), a new sealed type over the four existing lifecycle-bearing identifier types |
| A lifecycle-transition request or parameters | Not a separate request type -- `transitionStatus` takes `MemoryCoreRecordReference` and `MemoryCoreRecordStatus` directly as two parameters, mirroring the simplicity of `MemoryCore`'s other operations rather than introducing a single-use wrapper request type for two values |
| A lifecycle result type | **No** -- the existing `MemoryCoreRecord` (Unit 7) already suffices unchanged |
| One additional `MemoryCore` method | **Yes** -- `suspend fun transitionStatus(reference: MemoryCoreRecordReference, targetStatus: MemoryCoreRecordStatus): MemoryCoreRecord` |
| Corresponding contract tests | **Yes** -- construction/validation of `MemoryCoreRecordReference`, the method's exact signature (parameter types, return type, `suspend`), and its continued absence of any permission/runtime/persistence dependency, mirroring `MemoryCoreInterfacesTest.kt`'s own existing structural approach |

No change to any of the five candidate types, the five existing
`MemoryCore` creation operations, `MemoryRetrieval`, or any of its six
query types is required or authorised by this errata.

---

## What this document does not do

It does not add a new lifecycle state, retrieval mode, event, or record
kind. It does not authorise a permission decision inside `MemoryCore`,
an owner-authority check inside `MemoryCore`, automatic event
publication at the interface level, automatic amendment, or automatic
replacement-record creation -- all four remain exactly as excluded by
this task's own Boundaries section. It does not reopen any of Unit 7's
already-approved five creation operations or `MemoryRetrieval`'s ten
methods. It does not itself implement anything -- the amendment
described in Section 4 is carried out as a follow-up, explicitly-scoped
addition to Unit 7, not by this document.

---

```
ERRATA ACCEPTED — UNIT 7 AMENDMENT REQUIRED
```
