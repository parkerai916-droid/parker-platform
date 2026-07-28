# Memory Core — Governance Review

## Status

Programme: **Programme 2, Unit 1 — Memory Core.**
Phase: **Governance review only.** No Kotlin is implemented, proposed as a
diff, or changed by this document. Neither `src/` nor `tests/` is touched.
Nothing is staged, committed, or pushed.

This document was written after a direct inspection of the current
repository state — not from memory of prior sprints, and not from the
Sprint 4 / Sprint 11 documents' own prose alone. Every capability claim
below is traced to a specific file, and, where the claim is about
production behaviour, to `src/composition/ParkerRuntime.kt`'s actual
construction graph, not to a class's own name or KDoc.

---

## 1. Executive Summary

Parker already has a subsystem called **Memory** — `MemoryStore`,
`InMemoryMemoryStore`, `MemoryPromotionPolicy`, and a read-only
`MemorySource` projection — designed across Sprint 4 (Track A, Units
A1–A3) and extended in Sprint 11 (Unit 7). It is real: field-level
contracts exist, an implementation exists, fourteen-plus tests exist, and
its read path (`MemorySource.recall`) is genuinely wired into the
production `DefaultReasoningContextAssembler`. This is not a placeholder
or a stub in the way some other named concepts in this repository are.

But it is **not** the thing this Unit's brief calls the Memory Core. The
existing Memory subsystem is a single, generic, flat **long-term
knowledge/preference store** — one payload string, one category enum, a
handful of provenance-shaped fields, and a promote/reject decision. It
has no concept of an Entity, no concept of a Document as a governed
record, no concept of an Evidence Item or Assertion distinct from a
generic "memory," and no provenance model rich enough to prove
non-contemporaneity. It was never designed to be those things — Sprint 4
Unit A1 explicitly scoped Memory as "durable facts, preferences, prior
context, and history," not as a general-purpose entity/document/evidence
substrate.

This creates the central finding of this review, stated plainly because
it changes what "first implementation unit" can honestly mean: **there
is a name collision, not a capability overlap, between the existing
"Memory" subsystem and the newly-scoped "Memory Core."** They are not the
same thing wearing two names, and Memory Core is not simply "the next
increment" of existing Memory. Existing Memory is a narrow, working,
already-governed knowledge store. Memory Core, as scoped for this
Programme, is a broader identity/provenance/retrieval foundation for
entities, documents, conversations, and evidence — a layer existing
Memory could plausibly sit on top of, but does not today, and was never
asked to.

A second major finding: **the one constitutional requirement existing
Memory's own specification already imposes on itself — "Sensitive
memories MUST require appropriate permission" — is not enforced
anywhere in the running system.** `InMemoryMemoryStore` has no
`PermissionEngine` dependency at all, `ResourceType.MEMORY` is defined
but never used to register a Resource, and no `ActionVocabularyEntry`
exists for any memory-related verb phrase. This is not a defect
introduced by this review — it is a pre-existing, disclosed-here-for-
the-first-time-at-this-precision gap that any Memory Core design must
resolve before a sensitive Entity, Document, or Evidence Item can be
safely admitted into whatever this Programme builds.

A third finding, load-bearing for the recommended scope: Parker already
has a mature, production-composed, tested **Conversation** subsystem
(`ConversationEngine` / `ConversationHistorySource`) that independently
satisfies a meaningful slice of what Memory Core's brief calls
"conversation memory" — raw Turn storage, keyed by `ConversationId`,
oldest-first, one-sided (owner messages only). Memory Core must not
duplicate this; it must decide, explicitly, whether it wraps it,
references it, or leaves it alone.

**Final classification: NOT READY FOR CONTRACT DESIGN.** Section 21
states exactly what must be resolved first. None of the blockers require
new architecture to be invented from nothing — each is a genuine,
answerable decision this review surfaces but does not have the authority
to make on the owner's behalf.

---

## 2. Constitutional Position

Restated, not reinterpreted, from this Unit's own brief and from
`docs/architecture/parker-constitution.md` as already applied to Memory
by `MEMORY_RUNTIME_ARCHITECTURE.md` §1 and §11:

```
Owner
  ↓
Parker Constitution
  ↓
Trust Framework
  ↓
Runtime
  ↓
Reasoning Provider
```

Reasoning providers are advisory, not sovereign. Memory Core sits inside
the Runtime layer, beneath the Trust Framework — it does not sit beside
it or above it. Two governing principles apply to Memory Core exactly as
they already apply to existing Memory, with no weakening and no
exception carved out for "this is just storage":

- **The owner remains in control.**
- **Cognition proposes. Trust authorises. Runtime executes.**

Concretely, restated from `MEMORY_RUNTIME_ARCHITECTURE.md`'s own
Architectural Principle, and adopted here as Memory Core's principle
too, unaltered: **Memory stores knowledge. Memory never decides. Memory
never acts.** A stored Entity, Document, Conversation record, or
Evidence Item carries no authority merely because it exists in Memory
Core. Storage is not truth, storage is not permission, and storage is
not a decision. Every decision that touches a Memory Core record —
whether it may be created, corrected, disputed, disclosed, or
deleted — belongs to another subsystem: the submitting caller proposes,
Memory Core's own internal policy (mirroring `MemoryPromotionPolicy`'s
existing role) decides what enters the store, and the Permission Engine
— not Memory Core itself — decides who may read or act on what is
stored.

This document finds no tension between this constitutional position and
anything already built. The tension it does find (Section 5) is that the
Permission Engine's role in that sentence is currently unimplemented for
Memory's existing records, not that the principle is wrong.

---

## 3. Existing Repository Findings

Everything below was confirmed by direct inspection of the cited file,
not inferred from a name.

### 3.1 The existing Memory subsystem (Sprint 4 / Sprint 11)

| Artifact | File | What it is |
| --- | --- | --- |
| Field-level contracts | `src/interfaces/MemoryStore.kt` | `MemoryId`, `MemoryCategory` (5-value closed enum: `EPISODIC`, `SEMANTIC`, `PROCEDURAL`, `USER_PREFERENCES`, `RELATIONSHIPS`), `CandidateMemory`, `MemoryRecord`, `MemoryPromotionDecision` (`Promote`/`Reject`), `MemoryPromotionPolicy`, `MemoryQuery`, `MemoryStore` (`remember`/`retrieve`/`forget`). |
| Read-only projection | `src/interfaces/MemorySource.kt` | `fun interface MemorySource { suspend fun recall(query: MemoryQuery): List<MemoryRecord> }` — a capability-narrowed sibling of `MemoryStore`, structurally unable to reach `remember`/`forget`. |
| Implementation | `src/runtime/InMemoryMemoryStore.kt` | Implements both `MemoryStore` and `MemorySource` on one instance. Backed by a plain `mutableMapOf<MemoryId, MemoryRecord>()` behind a `Mutex` — no persistence of any kind. |
| Default policy | `src/runtime/DefaultMemoryPromotionPolicy.kt` | Promotes if `explicitlyRequested == true`, or if `confidence >= 0.7`; otherwise rejects with a free-text reason. Implements two of `33-memory-consolidation.md`'s six named factors; the other four (repetition, non-explicit user importance, goal relevance, frequency) are explicitly, disclosedly unimplemented. |
| Architecture | `docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md` (Unit A1) | Defines Memory's purpose, lifecycle (Observation → Candidate Memory → Evaluation → Promotion → Long-term Memory → Retrieval), ownership table, input sources, and constitutional boundaries. |
| Contract design | `docs/architecture/MEMORY_CONTRACT_DESIGN.md` (Unit A2) | Field-level design for all eight approved contracts; excludes `CandidateMemoryId`, `MemoryQueryResult`, `MemoryRuntime`, `MemoryObservation`; defers `MemoryRetrievalPolicy` and a combined retention/consolidation seam. |
| Memory Source contract design | `docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md`, `docs/architecture/MEMORY_SOURCE_GOVERNANCE_REVIEW.md`, `docs/implementation/MEMORY_SOURCE_INTEGRATION_SCOPE_LOCK.md` (Sprint 11 Unit 7) | The read-only `MemorySource` boundary and its production wiring into `DefaultReasoningContextAssembler`. |
| Older prose architecture | `docs/architecture/17-memory-architecture.md`, `docs/architecture/33-memory-consolidation.md`, `docs/adr/ADR-002-memory-context-world-model-separation.md`, `docs/adr/ADR-008-memory-promotion.md` | Pre-Sprint-4 chapter-level prose; short, and already superseded in detail by the two documents above, but not contradicted by them. |
| Specification | `docs/specifications/volume-03-core-interfaces/MemoryStore.md` | A summary of the already-approved, already-implemented behaviour; states two normative requirements load-bearing for this review: "Sensitive memories MUST require appropriate permission" and "Forgetting MUST be auditable." |
| Tests | `tests/runtime/InMemoryMemoryStoreTest.kt`, `tests/runtime/DefaultMemoryPromotionPolicyTest.kt`, `tests/runtime/FakeMemoryPromotionPolicy.kt`, `tests/runtime/FakeMemorySource.kt`, `tests/contracts/MemoryContractsTest.kt` | Real, substantive unit coverage of submission, evaluation, promotion, rejection, retrieval scoping, category narrowing, `maximumResults` capping, deterministic ordering, and forgetting. |

**Production composition, traced directly against
`src/composition/ParkerRuntime.kt` lines 285–310 and 436–460:**

```kotlin
val inMemoryMemoryStore = InMemoryMemoryStore()
val memorySource: MemorySource = inMemoryMemoryStore
...
reasoningContextAssembler = stage("Reasoning Context Assembler construction") {
    DefaultReasoningContextAssembler(identityService, toolRegistry, conversationHistorySource, memorySource, worldModelSource)
}
```

`InMemoryMemoryStore` is constructed exactly once, and is exposed to the
rest of the running system **only through its narrower `MemorySource`
type.** No other line in `ParkerRuntime.kt` holds a `MemoryStore`-typed
reference to it. Confirmed directly by reading
`DefaultReasoningContextAssembler.kt` in full: it declares a
`memorySource: MemorySource` constructor parameter, calls
`memorySource.recall(memoryQuery)` exactly once per `assemble()`, and
declares no field or parameter of type `MemoryStore` anywhere in the
class. Its own KDoc states this explicitly: "This class never calls
`MemoryStore.remember` or `MemoryStore.forget`."

**Consequence, not previously stated this precisely anywhere in the
repository: nothing in production ever calls `remember()` or
`forget()`.** A repository-wide search for call sites of
`InMemoryMemoryStore` / `MemoryStore.remember` / `MemoryStore.forget`
outside test files returns none. The only place a `CandidateMemory`
could be constructed and submitted in the real, running system does not
exist — no coordinator, no Tool, no Agent Run step, and no Planner
Runtime code path builds one. This means Memory's write path is fully
implemented and fully tested in isolation, but **structurally dormant in
production**: the store `DefaultReasoningContextAssembler` reads from is
guaranteed to be empty for the whole lifetime of a running `ParkerRuntime`
instance, because nothing ever writes to it. `MemoryPromotionPolicy` is
technically "production-composed" (it is constructed, as
`InMemoryMemoryStore`'s defaulted dependency), but it is never actually
consulted in production for the same reason.

### 3.2 The World Model (adjacent, not in scope, but load-bearing for the boundary)

`src/interfaces/WorldModel.kt` (`WorldBelief`, `WorldObservation`,
`ObservationResult`, `WorldModelUpdatePolicy`, `WorldQuery`, `WorldModel`)
and `src/runtime/InMemoryWorldModel.kt` follow an identical pattern to
Memory: production-composed only through the narrower `WorldModelSource`
type, read by the same `DefaultReasoningContextAssembler`. One detail
worth carrying into Memory Core's own provenance design: `WorldObservation`
already carries a `sourceTimestamp: Instant?`, separate from the
authoritative `WorldBelief.timestamp` the store itself assigns — a
real precedent, in this same codebase, for distinguishing "when the
underlying thing happened" from "when the record was created," which
`CandidateMemory`/`MemoryRecord` do **not** currently have (see Section
7).

### 3.3 Conversation storage (adjacent, production-composed, and directly relevant to Section 12)

`src/interfaces/ConversationEngine.kt` / `ConversationHistorySource.kt`
and `src/runtime/InMemoryConversationEngine.kt` (not re-read line by
line for this review beyond the interfaces, since Programme 2's brief
scopes conversation memory as a boundary question, not an
implementation target) already provide: a `Conversation` record keyed by
`ConversationId`, owned by one `ownerPrincipalId`, holding an ordered
list of `TurnId`s; a `Turn` record (`turnId`, `conversationId`,
`message: InboundOwnerMessage`, `receivedAt: Instant`); atomic,
idempotent continuity resolution; and a narrower, capability-limited
`ConversationHistorySource.history(conversationId): List<Turn>`
read projection, production-wired into the same
`DefaultReasoningContextAssembler`. This is genuinely production-composed
(confirmed: `conversationEngine = inMemoryConversationEngine` is
constructed and used for real Turn recording on every inbound message,
unlike Memory's dormant write path). Its own KDoc discloses real,
relevant limits: one-sided (owner messages only, no Parker reply
captured), no retention/expiry policy, and — like everything else in
this repository — in-memory only, not persisted.

### 3.4 What does not exist anywhere in the repository

Confirmed absent by direct search of `src/contracts/`, `src/interfaces/`,
and `src/runtime/` (24 files in `src/contracts/`, 22 in
`src/interfaces/`, all enumerated and checked):

- **No `Entity` contract or record type**, anywhere, in any form.
- **No `Document` record type.** `ResourceType.DOCUMENT` exists as one
  value in `src/contracts/Resource.kt`'s fourteen-value enum, but no
  `Resource` of that type is ever registered in `ParkerRuntime.kt`, and
  no document-shaped record (filename, MIME type, page count, extraction
  status) exists anywhere.
- **No `Evidence`, `Assertion`, or `Claim` contract.** `MemoryCategory`
  is a five-value classification of a generic knowledge payload, not an
  evidentiary model — it has no field distinguishing an owner statement
  from a third-party statement, an observed event from an allegation, or
  a verified fact from a disputed one.
- **No dedicated `Provenance` record type.** `MemoryRecord` carries a
  handful of provenance-*shaped* fields (`sourceSubsystem: String`,
  `correlationId: String`, `originatingPrincipalId: PrincipalId?`,
  `promotedAt: Instant`, `confidence: Double?`, `history: List<String>`),
  but nothing resembling the fourteen-item minimal set this Programme's
  brief requires (source identifier, source type, original location,
  creator/sender, creation time versus acquisition time versus ingestion
  time, extracted-from/derived-from relationships, a hash or integrity
  identifier, processing method, or an original/extracted/summarised/
  inferred flag).
- **No `memory.*` (or any Memory-related) `EventType`.** A direct search
  of `src/contracts/EventContracts.kt` and of every literal string
  matching `"memory.` anywhere under `src/` returns nothing.
  `InMemoryMemoryStore`'s constructor takes only a `MemoryPromotionPolicy`
  — it holds no `EventBus` reference at all, so it could not publish an
  event even if one were named. Every lifecycle transition Memory
  currently performs (promote, reject, forget) is silent to the rest of
  the runtime.
- **No `AuditService` implementation.** The interface
  (`src/interfaces/AuditService.kt`: `record`/`query`) exists, but a
  repository-wide search for a class implementing it, or for any
  `AuditRecord(...)` construction, returns nothing. It is not
  constructed in `ParkerRuntime.kt` and is referenced nowhere outside its
  own interface file and one incidental type reference in
  `ExecutionResult.kt`.
- **No persistence layer of any kind, for Memory or for anything else in
  this repository.** Every "store" in this codebase — Memory, World
  Model, Conversation, Identity, Resource Registry, Tool Registry — is an
  `InMemory*` class backed by a `mutableMapOf` and a `Mutex`. This is not
  a Memory-specific gap; it is this entire repository's current storage
  posture. It is stated here because Memory Core's brief explicitly asks
  about "durable local storage" as a requirement candidate, and the
  honest answer is that no precedent for it exists anywhere in this
  codebase to build on.

---

## 4. Current Memory Capability Classification

Per the required taxonomy (implemented and production-composed /
implemented but isolated / placeholder / test-only / absent), applied to
every item this Unit's brief asked about:

| Capability | Classification | Basis |
| --- | --- | --- |
| `MemoryStore.remember` / promotion / rejection | **Implemented, but isolated.** | Fully implemented and unit-tested (`InMemoryMemoryStoreTest.kt`); zero production call sites. |
| `MemoryStore.forget` | **Implemented, but isolated.** | Same basis; also never called in production. |
| `MemoryStore.retrieve` | **Implemented, but isolated** for direct `MemoryStore`-typed callers (none exist in production). | Only reachable in production via the narrower `MemorySource.recall` path (below). |
| `MemorySource.recall` (read projection) | **Implemented and production-composed.** | `DefaultReasoningContextAssembler.kt` line 309: `memorySource.recall(memoryQuery)`, called on every `ParkerRuntime.submitOwnerMessage`. Structurally guaranteed to return an empty list today, since nothing ever writes to the store it reads (Section 3.1). |
| `MemoryPromotionPolicy` / `DefaultMemoryPromotionPolicy` | **Implemented, but effectively dormant in production** (constructed, never consulted, since `remember` is never called). | `InMemoryMemoryStore()`'s default constructor argument. |
| `MemoryCategory` (5-value classification) | **Implemented and production-composed** as a type; **never populated in production** for the reason above. | `src/interfaces/MemoryStore.kt`. |
| `ReasoningContextAssembler` memory integration | **Implemented and production-composed.** | Confirmed above; renders a "Memories" entry section that is currently always empty in production. |
| Entity contract/record | **Absent.** | No file, no type, anywhere. |
| Document record (beyond an unused `ResourceType` value) | **Absent.** | `ResourceType.DOCUMENT` defined, never used. |
| Conversation record/storage | **Implemented and production-composed**, but owned by a separate, already-governed subsystem (`ConversationEngine`), not by Memory. | Section 3.3. |
| Evidence/Assertion/Claim model | **Absent.** | No file, no type, anywhere. |
| Provenance record type | **Absent** as a dedicated type; **partially implemented** as scattered fields on `MemoryRecord`/`CandidateMemory`. | Section 3.4/3.1. |
| Non-contemporaneous document proof | **Absent.** | No field anywhere distinguishes real-world creation time from ingestion/promotion time for a `MemoryRecord`; `promotedAt` is the only timestamp `MemoryRecord` carries, and it records only when Memory itself acted. |
| Memory-related events | **Absent.** | No `memory.*` `EventType`, no `EventBus` reference inside `InMemoryMemoryStore`. |
| Identity/permission mechanisms for memory access | **Placeholder.** | `ResourceType.MEMORY` and `PermissionAction.READ`/`WRITE`/`DELETE` all already exist as enum values sufficient to express a memory permission check, but no `ActionVocabularyEntry`, no registered `Resource`, and no `PermissionEngine` call path exists to use them for any Memory operation, read or write. |
| Storage abstraction reusable for Memory Core | **Absent** beyond the repository-wide `InMemory*` + `Mutex` convention (itself not an abstraction, just a shared idiom). | Section 3.4. |
| Tests covering memory/retrieval | **Implemented, test-only** for the behaviours they cover (which are entirely the existing Memory subsystem's own read/write mechanics, not anything Memory-Core-shaped). | Section 3.1 table. |
| Documentation claiming Memory capability | **Accurate, not overclaiming**, but capable of being read as more complete than it is. | Section 3.5, below. |

### 3.5 Documentation accuracy check

`README.md` states, in its "Milestone" and "For Developers" framing
(lines 146–151, 538–544, 641–645): "Production Reasoning Context assembly
now draws from Conversation History, Memory, and World Model sources
while preserving their distinct ownership and lifecycle boundaries," and
lists "Memory" as one of three Knowledge Architecture layers ("What
Parker has learned"). This is **not a false claim** — the read path
genuinely is production-composed, exactly as stated, and the README does
not claim the write path is wired, or that Memory currently holds
anything. It is, however, silent on the fact that the write path is
unreachable in production and the store is therefore always empty today
— a reader could reasonably assume "Memory source is wired into
Reasoning Context" implies Memory actually contributes knowledge, when
today it structurally cannot. This is noted as an observation for a
future documentation pass, not something this governance-only review is
authorised to correct (out of scope: no file outside this new document
is modified here).

---

## 5. Authority and Trust Boundaries

**Who may write memory today:** no one, in production (Section 3.1).
Architecturally, per `MEMORY_RUNTIME_ARCHITECTURE.md` §6, Candidate
Memory may originate from the Planner Runtime, the Agent Runtime, the
World Model, a direct user instruction, a plugin, or a workflow — a
non-exhaustive, non-closed list. None of these gains promotion authority
merely by submitting; only `MemoryPromotionPolicy`, consulted internally
by whatever store implementation handles `remember`, decides.

**Whether reasoning providers may directly persist memory:** no,
structurally. `ModelReasoningProvider` and every type in
`src/interfaces/ReasoningProvider.kt` (not modified or newly read for
this review beyond confirming no `MemoryStore` reference exists in
`ParkerRuntime.kt`'s reasoning-provider construction stage) has no path
to `MemoryStore`. A reasoning provider's output flows through
`ConversationTurnReasoningCoordinator` and then either
`ResponseComposer`/`ResponseDelivery` or `GoalPlanningHandoffCoordinator`
— neither of which touches Memory. This is correct and should remain
true for Memory Core: a reasoning provider proposing that something be
remembered is cognition proposing, never persistence.

**Whether Tools may write memory:** no tool in this repository does
today (only `LocalTextChannelDeliverTool` exists in production, and it
has no Memory dependency), and no contract currently gives a Tool a path
to `MemoryStore.remember`. This absence should be treated as
provisionally correct for Memory Core too, pending an explicit decision
(Section 20) about whether a future Tool category is ever allowed to
submit a Candidate Memory/Entity/Document directly, or must always route
through a reasoning provider or the Planner Runtime's own judgment.

**Whether Agent Runs may propose memory writes:** architecturally
anticipated (`MEMORY_RUNTIME_ARCHITECTURE.md` §6 names the Agent Runtime
as a legitimate Candidate Memory source) but not implemented — no step
in `InMemoryAgentRuntime`'s `runLoop` constructs or submits a
`CandidateMemory` anywhere.

**What role the Trust Framework must play:** per
`MEMORY_CONTRACT_DESIGN.md`'s own Constitutional Boundaries and
`MemoryStore.md`'s own normative requirement, the Permission Engine must
authorise disclosure of sensitive memories — Memory itself must never
evaluate that question. **This is currently not true of the running
system in the only sense that matters: there is no call path by which
that authorisation could ever be requested**, because
`InMemoryMemoryStore` holds no `PermissionEngine` reference, and
`DefaultReasoningContextAssembler.recall()`'s call site performs no
permission check before rendering a memory into every reasoning context
it assembles. Today this is masked by Section 3.1's finding (the store
is always empty), but it is not something Memory Core can inherit
unexamined: if Memory Core stores a sensitive Entity, Document, or
Evidence Item and is read the same way existing Memory is read, it would
be surfaced into reasoning context with **zero** permission evaluation,
the first time anything actually writes to it.

**Whether the owner can inspect, correct, quarantine, or delete memory:**
`forget` exists and is auditable at the identifier level only
(`wasForgotten(memoryId): Boolean`, `src/runtime/InMemoryMemoryStore.kt`
line 172 — itself not part of the public `MemoryStore` interface, a
test/inspection-only method). There is no inspect-all, no
quarantine-without-deleting, and no correction operation of any kind
(Section 10). The owner has no path today to review what Memory holds
beyond whatever a `MemoryQuery` happens to match.

**Whether memory records can ever override owner authority:**
no — nothing in the existing design or implementation gives a
`MemoryRecord` any executable or decision-making capacity. This
constraint is easy to keep and this review finds no risk of it being
violated by anything currently built.

### Recommended explicit authority boundaries for Memory Core

1. Every write path into Memory Core (however its record types are
   finally shaped) must be an explicit, caller-invoked submission,
   mirroring `MemoryStore.remember`'s existing shape — never a
   subscription-triggered or autonomous write.
2. Every read path that could surface a record marked sensitive (using
   whatever sensitivity model Memory Core adopts — see Section 6's
   recommendation against reusing a bare `Boolean`) must pass through a
   real `PermissionEngine.evaluate` call before that content leaves
   Memory Core's boundary. This is not true of existing Memory's
   `MemorySource.recall` path today, and Memory Core must not repeat
   that gap — closing it should be treated as a precondition for
   admitting any genuinely sensitive record type, not a follow-up.
3. `ResourceType.MEMORY` and `ResourceType.DOCUMENT` already exist and
   should very likely be the resource types Memory Core's Permission
   Engine integration uses — but no `Resource` should be registered
   per individual record (that would be a large, probably unworkable
   volume of Resource Registry entries); the shape of that integration
   (one Resource per record? one Resource per owner/collection? a
   dedicated resource-type-level policy rule mirroring the
   `EXECUTE`/`AGENT` boundary-resource pattern `ParkerRuntime.kt` already
   uses for Agent Run initiation?) is an open question for Contract
   Design, not decided here.

---

## 6. Truth, Evidence and Assertion Model

**"Storage does not create truth"** is not violated by anything
currently implemented — `MemoryRecord` carries no field that could be
mistaken for a truth verdict, and nothing in `DefaultReasoningContextAssembler`
renders a memory as more authoritative than any other context entry (it
is rendered as one line among "Prior message," "World belief," and
"Available tool," all in the same undifferentiated `entries: List<String>`
— itself worth flagging: **today, a promoted memory and a raw prior
message are rendered with equal apparent authority to the reasoning
provider**, which is a Reasoning Context assembly concern, not a Memory
Core one, but is relevant background for how seriously Memory Core's
own truth-status fields will need to be taken downstream).

Existing `MemoryCategory` (`EPISODIC`, `SEMANTIC`, `PROCEDURAL`,
`USER_PREFERENCES`, `RELATIONSHIPS`) classifies *kind of knowledge*, not
*truth status* — it does not distinguish an owner statement from a
third-party statement, an observed event from an allegation, or a
verified fact from a disputed one, and was never intended to (Section
3.1). Memory Core's brief requires exactly this distinction, so it
cannot reuse `MemoryCategory` unmodified; it needs its own, separate
classification axis.

**Recommendation: truth/evidence status belongs on a separate
Assertion-shaped record, referencing a Document/Entity/Evidence Item by
identifier, not as a field baked directly onto the underlying stored
content.** Reasoning: the same underlying source material (a document,
a conversation turn) can be the subject of more than one assertion over
time — an initial characterisation, a later dispute, an eventual
supersession — and a single flat "truth status" field on the content
record itself cannot represent that history without either mutating the
record in place (violating "Correction is not Destruction," Section 10)
or accumulating an unbounded number of status fields on one type. This
mirrors the same structural reasoning `MEMORY_CONTRACT_DESIGN.md` §5
already used to justify keeping `MemoryPromotionDecision` a lightweight,
per-evaluation outcome rather than a field baked onto `MemoryRecord`
itself, applied here to truth status instead of promotion status. This
document does not design that Assertion record's fields — only that it
should exist as its own type, separate from whatever Memory Core's
"content" record types turn out to be.

**Explicitly not designed here, per this Unit's own scope:** a general
epistemology engine, a confidence-scoring algorithm across conflicting
assertions, or automated dispute resolution. Memory Core's job is to
preserve enough structure that a later subsystem (or the owner) *can*
reason about disputed/superseded information — not to do that reasoning
itself. This is the same boundary `MEMORY_RUNTIME_ARCHITECTURE.md` §2
already draws for existing Memory ("Reasoning... belongs to a reasoning
provider or the Planner Runtime, not to Memory") and Memory Core should
inherit it unchanged.

---

## 7. Provenance Requirements

Existing `MemoryRecord`/`CandidateMemory` provenance fields, confirmed
by direct reading of `src/interfaces/MemoryStore.kt`:
`sourceSubsystem: String` (which subsystem submitted it — not the
original real-world source), `correlationId: String` (ties back to a
task/session, not to a document or conversation), `originatingPrincipalId:
PrincipalId?`, `promotedAt: Instant` (when Memory itself acted, not when
the content originated), `confidence: Double?`, and `history:
List<String>` (free-text audit entries; only "promoted at ..." is ever
actually written by the real implementation — "consolidated,"
"forgotten," and "superseded" are named in `MemoryRecord`'s own KDoc as
anticipated future audit events but **no code path in
`InMemoryMemoryStore` writes any of the three today**; `forget` removes
the record from `records` and adds only the bare `MemoryId` to a
separate `forgottenIds` set, recording no history entry on the record
itself, since the record is gone).

This is real, but it is not close to the fourteen-item minimal set
Memory Core's brief requires. Missing entirely: a source identifier
distinct from `correlationId`; a source *type* (document, message,
observation, third-party statement); the original location/URI of the
source material; the creator or sender of the underlying content
(`originatingPrincipalId` names a Parker-side Principal, not
necessarily whoever authored the original material — these can differ,
e.g. Parker observing a statement made by someone who is not itself a
registered Principal); a distinct real-world creation time separate
from acquisition time and from ingestion time (three different
moments this repository currently collapses into one `promotedAt`);
extracted-from/derived-from relationships (`MemoryRecord.relatedMemoryIds:
List<MemoryId>` exists but is untyped as to *why* two records are
related — it cannot distinguish "derived from" from "consolidated with"
from "supersedes"); a hash or integrity identifier; the processing
method that produced the record; and an explicit
original/extracted/summarised/inferred flag.

**Non-contemporaneous documents — the case this Programme's brief
singles out — cannot be proven today, structurally, not just as a
missing feature.** There is no field anywhere that records "when the
underlying real-world thing this content describes actually happened or
was created," independent of "when Parker ingested/promoted it." Without
that field, existing Memory (or anything built directly on its pattern)
cannot distinguish a document that is contemporaneous with an event
from one manufactured or edited afterward and merely ingested later —
the only timestamp available, `promotedAt`, is equally early or late for
both cases. **File-timestamp trust is correctly out of scope to assume**
(per this Unit's own instruction) and nothing here assumes it; the
finding is narrower and more basic: no *claimed* creation time field
exists at all yet for Memory Core to later decide whether or how much to
trust.

**Recommendation.** A dedicated Provenance record (Section 8) should
carry, at minimum: a source identifier; a source type (open, not a
closed enum — mirroring `CandidateMemory.sourceSubsystem`'s own
deliberate choice of `String` over an enum, per
`MEMORY_RUNTIME_ARCHITECTURE.md` §6's "illustrative... not closed by
architectural necessity"); an original location/reference; a
creator/sender identifier (which may or may not resolve to a registered
`PrincipalId`); three distinct, independently-nullable timestamps —
**claimed creation time** (asserted, not verified, by whoever/whatever
submitted it — following `WorldObservation.sourceTimestamp`'s existing
precedent in this exact codebase, Section 3.2), **acquisition time**
(when Parker's own boundary first encountered the material), and
**ingestion time** (when Memory Core itself recorded it, playing
`MemoryRecord.promotedAt`'s existing role); an extracted-from/derived-from
reference, typed by relationship kind, not a bare untyped list; an
optional integrity hash; a processing-method description; a confidence
figure; and an explicit
original/extracted/summarised/inferred classification. None of this is
designed field-by-field here — this section states requirements, per
this Unit's own brief, not a Kotlin shape.

---

## 8. Recommended Core Record Types

Assessed against what exists (Section 3) and what is absent (Section
3.4), not against an abstract ideal:

- **Entity** — required; currently absent entirely. Needed as the
  addressable "who/what" a Document, Conversation, or Evidence Item can
  reference (a person, an organisation, a place, a thing) — without it,
  "Relationships" (already a `MemoryCategory` value with nothing
  structural behind it) has nothing concrete to relate.
- **Document** — required as a governed record distinct from
  `ResourceType.DOCUMENT`'s current unused enum value; this record
  should be a registration/identity/provenance record only (Section 13
  draws the line against Document Handling's file-parsing concerns).
- **Conversation / Message (Turn)** — likely **not** a new Memory Core
  record type; `ConversationEngine`'s existing `Conversation`/`Turn`
  types already satisfy this need and are already production-composed
  (Section 3.3, Section 12). Memory Core should reference them, not
  duplicate them, pending the explicit boundary decision Section 12
  requires.
- **Evidence Item** — required; currently absent. Distinct from
  Document: a Document is a governed reference to a source; an Evidence
  Item is a claim that a specific Document (or Conversation Turn, or
  other source) supports or contradicts something.
- **Assertion / Claim** — required, per Section 6's own recommendation,
  as the carrier of truth/dispute/supersession status, separate from the
  content record it is about.
- **Provenance Record** — required, per Section 7 — either as its own
  addressable type, or (an open question for Contract Design, not
  decided here) as a mandatory, non-optional embedded value on every
  other Memory Core record type rather than a separately identified and
  queryable record. Both shapes satisfy "provenance must minimally
  preserve..."; they differ only in whether provenance is independently
  queryable/correctable or fixed to its parent.
- **Retrieval Result** — **not recommended as a new type.** Existing
  `MemoryQuery`/`retrieve` already establishes the precedent
  (`MEMORY_CONTRACT_DESIGN.md` §7's own "Why not `MemoryQueryResult`?"
  reasoning: a plain list is sufficient until pagination or per-result
  scoring becomes a concrete need) and nothing in this review identifies
  a concrete need Memory Core has that existing Memory did not.
- **Memory Link / Relationship** — required in some form once Entity
  exists, but should very likely be a typed relationship between two
  Entity/Document/Evidence identifiers (mirroring the correction Section
  7 recommends for `MemoryRecord.relatedMemoryIds`'s own untyped list),
  not a generic bag of related IDs.

**This document does not recommend collapsing everything into one
generic record**, for the same reason `MEMORY_CONTRACT_DESIGN.md`
already gave when it chose *not* to split `MemoryCategory` into five
separate types (§4: "no category-specific field requirement has been
identified"): here, the reverse concern applies — Entity, Document, and
Evidence Item each already have concretely different, identified field
requirements (an Entity has no file location; a Document has no
relationship-to-other-entity list; an Evidence Item has no MIME type),
so collapsing them into one generic "memory core record" would recreate
exactly the ambiguity `MemoryRecord`'s own single flat
`knowledgePayload: String` already exhibits today (Section 3.1) — a
payload whose internal shape nothing enforces or types.

---

## 9. Identity, Permission and Privacy

Covered substantively in Section 5. Summarised here against the specific
sub-questions this Unit's brief poses:

- **Existing `PermissionEngine`, `ResourceType`, action vocabulary
  sufficiency:** the *type* substrate is sufficient — `PermissionAction.READ`/
  `WRITE`/`DELETE`, `ResourceType.MEMORY`/`DOCUMENT`, and
  `ActionVocabularyEntry`'s existing `Set<ActionResourceMapping>` shape
  (already supporting composite actions) could express every Memory
  Core permission check this review can currently anticipate, with zero
  new enum values needed on the `PermissionAction`/`ResourceType` side.
  What is missing is not new taxonomy — it is the actual wiring
  (registered Resources, registered vocabulary entries, and a
  `PermissionEngine`-typed dependency on whatever implements Memory
  Core), exactly Section 5's finding.
- **New permission actions/resource types that may be required:** none
  identified. This review does not add any (per this Unit's own explicit
  instruction not to).
- **Multi-user support:** every Principal-scoping mechanism Memory
  Core would need already exists in shape (`PrincipalId`,
  `IdentityService.resolve`, `MemoryQuery.requestingPrincipalId`'s
  existing precedent for identity-scoped retrieval) — this is a
  forward-compatible foundation already, not a gap specific to Memory
  Core.
- **Sensitivity classification mismatch, not previously flagged this
  precisely:** `Resource.sensitivity` uses a nine-value
  `ResourceSensitivity` enum (`PUBLIC`, `PERSONAL`, `HOUSEHOLD`,
  `FINANCIAL`, `MEDICAL`, `LEGAL`, `SECURITY_SENSITIVE`,
  `CREDENTIALS_SECRETS`, `THIRD_PARTY_PERSONAL_DATA`) everywhere else a
  Resource is registered in this system. `CandidateMemory.sensitive`
  and `MemoryRecord.sensitive` are a bare `Boolean`. If Memory Core's
  own sensitive records are ever backed by real `Resource` registration
  (as Section 5 recommends investigating), this mismatch will need
  resolving — either Memory Core's records adopt `ResourceSensitivity`
  directly, or an explicit, justified mapping from a Memory-Core-specific
  scheme to it is designed. Left open here, not decided.

---

## 10. Immutability, Correction and Deletion

**Existing behaviour, exactly as implemented:** `forget(memoryId)`
removes the record from the retrievable map and adds the bare
`MemoryId` to a separate, non-public `forgottenIds` set — proving only
that an identifier once existed and was later forgotten, never
preserving what the forgotten content actually was. There is no
`supersede`, `dispute`, or `amend` operation anywhere in `MemoryStore`
or `InMemoryMemoryStore`; `MemoryRecord.history`'s own KDoc names
"consolidated"/"forgotten"/"superseded" as anticipated future audit
events, but confirmed by direct reading of the implementation, only
"promoted" is ever actually written.

**This is closer to Destruction than to Correction, judged against this
Programme's own stated distinction.** `forget` is total, content-losing
deletion with only identifier-level proof of prior existence — it is not
a "quarantine," not a "mark disputed," and not a content-preserving
amendment. For Memory Core's brief, which explicitly requires
distinguishing Correction from Destruction and explicitly asks whether
source records should be immutable and corrected only through linked
amendments, existing `forget` is not a model to extend; it is a
narrower, different operation (true deletion) that Memory Core should
keep, unchanged, for its own narrower purpose (an owner-requested erasure
path, Section 20), while adding a **separate**, non-destructive
correction path for everything short of that.

**Recommendation.** Source/original records (a submitted Document, a
recorded Conversation Turn, an ingested Evidence Item) should be
immutable once created — never edited in place — with correction
expressed as a new, linked Assertion or amendment record referencing the
original, exactly as Section 6 already recommends for truth status.
`forget`'s existing shape (identifier-level audit trail only, content
genuinely removed) remains appropriate as the one true-deletion path,
reserved for owner-requested erasure specifically, not for ordinary
correction of a mistaken record.

---

## 11. Retrieval Boundary

Existing `retrieve`/`recall`: identity-scoped (principal-null records are
visible to everyone; principal-set records are visible only to that
principal — a coarse, binary scoping rule, not a general ACL), optionally
category-narrowed, case-insensitive substring-matched against one flat
`knowledgePayload` field, capped by a caller-supplied
`maximumResults`, ordered most-recently-promoted-first via insertion
order (deliberately not wall-clock comparison, to avoid same-tick ties —
a genuinely careful, already-tested design choice worth preserving as a
pattern). `MemoryRetrievalPolicy` is named in the Contract Design as a
deferred, not-yet-implemented pluggable ranking seam.

Against this Unit's brief's own retrieval list: exact-identifier lookup
exists trivially (`records[memoryId]`, though not exposed as a public
`MemoryStore` operation today — only `retrieve`/`recall` by query is
public); entity lookup and document lookup are impossible today because
Entity/Document don't exist as types; chronological retrieval exists
only as an implicit, non-configurable "most recent first" default, not
as a caller-selectable ordering; relationship traversal is impossible
(no typed relationship model, Section 8); provenance-aware retrieval is
impossible (no dedicated provenance to filter or sort by); text search
exists only as unweighted substring matching; metadata filtering exists
only for one field (`category`).

**Semantic/embedding search: correctly not required for a first
Memory Core unit**, consistent with this Programme's own brief and with
`MEMORY_CONTRACT_DESIGN.md` §8's identical prior determination for
existing Memory. Nothing in this review's own findings creates new
pressure to reconsider that. Whether semantic retrieval ever belongs in
Memory Core itself, or is layered on afterward as a separate, later
capability querying the same underlying records, is left as an open
question (Section 20) rather than pre-decided — this review's
recommended first unit (Section 18) does not require an answer either
way.

---

## 12. Conversation Memory Boundary

This is the section where this review's findings most directly
constrain the recommended first unit, so it is stated as plainly as
Section 3.3 already established: **a real, tested, production-composed
Conversation subsystem already exists**, independent of Memory, and
already does most of what "preserve raw conversation, treat summaries as
derived" would ask for — full raw `InboundOwnerMessage` per Turn, speaker
identity (`senderPrincipalId`), timestamp (`receivedAt`, distinct from
the message's own claimed `timestamp` — itself a small, already-existing
instance of exactly the claimed-versus-recorded timestamp distinction
Section 7 recommends generalising), channel (`channelId`), and
conversation identifier (`conversationId`).

What it does **not** do, confirmed from its own KDoc, not inferred:
capture Parker's own outbound replies (one-sided); capture attachments;
reference Entities or Documents; generate or store summaries; apply any
retention policy; or persist beyond process lifetime.

**Recommendation.** Memory Core must not build a second, competing
conversation-storage mechanism. The open question genuinely is a
boundary question, not a design-from-scratch question: does Memory Core
(a) treat `ConversationEngine`'s existing Turns as an external source it
may reference (via `correlationId`/`conversationId`, the same
loose-coupling pattern `CandidateMemory` already uses toward tasks and
sessions), never owning or duplicating them; or (b) eventually absorb
conversation storage into Memory Core's own governed record set,
deprecating `ConversationEngine`'s standalone role. This review
recommends (a) as the default posture for a first unit — it requires no
change to an already-working, already-tested, already-production-composed
subsystem — but does not treat (b) as foreclosed for a later Programme 2
unit, since `ConversationEngine`'s own real limitations (one-sided,
no attachments, no entity references) are exactly the kind of gap a
mature Memory Core might eventually be asked to close. Default
recommendation, per this Unit's brief's own instruction: **preserve raw
source material; treat summaries as derived, never as a replacement for
the raw record** — consistent with how `ConversationEngine` already
behaves (it has no summarisation step at all today) and with how
existing Memory already treats a `CandidateMemory`'s payload as carried
forward unchanged, never rewritten, into `MemoryRecord`.

---

## 13. Document Handling Boundary

Restated from this Unit's own brief and confirmed to require no
correction against anything found in this repository: Memory Core may
register and govern a Document *record* — its identity, its provenance,
its relationship to Entities and Evidence Items, and its correction/
supersession history. It must not absorb file discovery, import,
parsing, OCR, format handling, attachment extraction, page structure,
image analysis, corruption handling, duplicate detection, or document
classification — all of that is Document Handling's own, later,
separate Programme concern. Nothing in the current repository blurs this
line today (there is no document-parsing code anywhere to accidentally
inherit), so this boundary can be adopted cleanly, with no untangling
required.

---

## 14. Storage and Technology Considerations

Per Section 3.4: no persistence precedent exists anywhere in this
repository, for any subsystem. Every store is `InMemory*` plus a
`Mutex`. This is stated as a finding, not a recommendation either way —
Memory Core does not need to solve durable persistence for this whole
repository as a side effect of its own first unit.

**Recommendation, consistent with `MEMORY_CONTRACT_DESIGN.md`'s own
already-accepted approach for existing Memory:** define Memory Core's
record contracts and runtime responsibilities independent of any storage
technology, exactly as `MemoryStore`'s own interface says nothing about
how `InMemoryMemoryStore` stores its `mutableMapOf`. A first
implementation should very likely remain in-memory, for the same reason
existing Memory's first implementation did (no concrete persistence
requirement has been identified by this review, and inventing one now
would be exactly the kind of "fashionable database" premature choice
this Unit's brief warns against) — but this document flags, more
insistently than existing Memory's own Contract Design did for itself,
that **Memory Core's own architecture explicitly claims durability**
("Memory... persists across tasks, sessions, and time" —
`MEMORY_RUNTIME_ARCHITECTURE.md` §1, a claim this Programme's brief
implicitly inherits by calling its own foundation "Memory Core"), while
its only implementation today does not survive a process restart. This
tension is not new to Memory Core — it already exists for the current
Memory subsystem — but Memory Core's brief raises the stakes
considerably: Entities, Documents, and Evidence Items backing an
"evidence intelligence" use case (Section 17) are far less useful if they
disappear on every restart than a handful of learned user preferences
are. This review recommends the first implementation unit explicitly and
honestly document "in-memory, non-durable" as a stated, temporary
limitation — not silently implied by omission the way it currently is
for existing Memory — so a future reader does not mistake "Memory Core
exists" for "Memory Core's contents survive a restart."

Relational storage, object/file storage, graph relationships, full-text
indexing, and embeddings are all real, plausible future directions
(graph relationships in particular map naturally onto Section 8's
recommended typed Memory Link/Relationship record) but none has a
concrete, identified requirement today; naming them as future
possibilities, not committing to any of them, is this review's full
position, consistent with `MEMORY_RUNTIME_ARCHITECTURE.md`'s own Out of
Scope section for the analogous existing-Memory case.

---

## 15. Events and Auditability

Per Section 3.4/3.1: no Memory-related event exists today, `InMemoryMemoryStore`
has no `EventBus` dependency, and `AuditService` has no implementation
anywhere. `MemoryRecord.history`'s own promised four-event audit
trail (`promoted`/`consolidated`/`forgotten`/`superseded`) is
three-quarters unrealised in the actual code.

**Assessment against the candidate event names this Unit's brief lists**
(`memory.entity_created`, `memory.document_registered`,
`memory.conversation_recorded`, `memory.assertion_recorded`,
`memory.record_amended`, `memory.record_disputed`,
`memory.record_superseded`, `memory.record_deleted`,
`memory.retrieval_performed`): this review does not freeze any of these
names, per its own instruction. It does find, based on Section 3.1's
concrete finding that existing Memory's complete silence on the
`EventBus` has caused no operational problem *because nothing observes
it yet*, that **write-side events (creation, amendment, dispute,
supersession, deletion) are constitutionally and operationally
necessary for Memory Core specifically**, for a reason existing Memory
never had to face: an owner inspecting what Memory Core knows, or a
future audit/compliance need (explicitly named in this Programme's
"evidence intelligence" framing) cannot be satisfied by `history: List<String>`
strings alone once real Entities/Documents/Evidence Items with real
provenance are at stake — a durable, queryable, cross-record event
trail is a different and stronger guarantee than a free-text list
embedded on each record. **`memory.retrieval_performed`-shaped events are
not recommended as necessary** for a first unit: nothing in this
Programme's brief or in existing Memory's own precedent identifies a
concrete need to log every read, and doing so unconditionally would be a
volume/privacy-sensitive default this review is not prepared to endorse
without a stated purpose.

---

## 16. Failure, Conflict and Uncertainty

Existing Memory's own posture, confirmed directly: unknown/missing
metadata is handled by nullability (`confidence: Double?`,
`originatingPrincipalId: PrincipalId?`) rather than by any sentinel or
fabricated default; "no matches" is always an empty list, never an
exception (`retrieve`, `recall`); a missing `MemoryId` on `forget`
returns `false`, never throws. This is a genuinely good, consistent
precedent already established in this codebase (the same "empty/null,
never throw, for not-found" convention `IdentityService.resolve` and
`ConversationHistorySource.history` both already share) and Memory Core
should adopt it unchanged for its own equivalent lookups.

What existing Memory has **no occasion to face**, because it has no
Entity/Document/provenance model, is genuinely new to Memory Core:
unknown author, unknown creation date, uncertain identity match (is this
newly-submitted Entity the same as an already-known one, or a
different person with the same name?), disputed provenance, failed
extraction, and conflicting records asserting different things about the
same subject. **Recommendation, consistent with the nullable-field
precedent above and with this Unit's own instruction not to manufacture
certainty:** every one of these should be representable as an explicit,
first-class "unknown" or "uncertain" state on the relevant field or
record — never silently defaulted, never inferred without a disclosed
inference step, and never resolved automatically by picking one
candidate over another without a caller-visible decision (mirroring
`WorldModel.md`'s own confidence-comparison precedent at
`ObservationResult.Rejected`, where a losing observation is explicitly
rejected with a stated reason, not silently discarded).

---

## 17. Evidence Intelligence Readiness

Tested directly against the example questions this Unit's brief poses:
"which documents support or contradict a claim," "who made a statement,"
"when was a document actually created and was it contemporaneous," "which
later documents rely on an earlier unsupported characterisation," "what
source created a summary," "has an assertion been disputed or
superseded."

**None of these is answerable today**, because none of the record types
or relationships they depend on exist (Section 3.4). This is not a
surprising finding given Section 3.4's own inventory, but it is the
correct, honest answer to give directly, rather than implying partial
readiness that does not exist. What Memory Core must *preserve now* to
make these questions answerable *later* — without itself performing any
evidence analysis, per this Unit's own explicit instruction — is exactly
the combination Sections 7, 8, and 10 already separately justify: typed
provenance (to answer "who/when/from where"), typed relationships (to
answer "which documents rely on which"), and a non-destructive
correction/dispute/supersession model (to answer "has this been
disputed or superseded"). No new requirement is introduced by this
section beyond what those three sections already establish — this
section exists to confirm that the combination, taken together, is
sufficient groundwork for the evidence use case this Programme names as
its test, not to add a fourth, separate requirement.

---

## 18. Recommended First Implementation Unit

Consistent with Sections 3–17's findings, and explicitly not treating
this Unit's brief's own suggested default ("durable identity, provenance
and retrieval foundations rather than advanced semantic memory") as
foreclosed, but confirming it independently: this review agrees with
that default, for reasons grounded in what was actually found, not
merely because the brief proposed it.

**Proposed scope for a first Memory Core implementation unit**, offered
as a starting point for Contract Design once Section 21's blockers are
resolved, not as a pre-approved design:

- **Proposed contracts:** an `Entity` contract; a `Document` contract
  (registration/identity/provenance only, per Section 13's boundary); a
  `Provenance` contract per Section 7's minimal field set; an `Assertion`
  contract per Section 6's truth/dispute/supersession model; a typed
  `MemoryCoreLink`/relationship contract per Section 8. Evidence Item is
  deliberately *not* proposed for the first unit — Section 17 finds it
  depends on Entity, Document, Provenance, and Assertion all already
  existing, so it is better sequenced as a second unit once those four
  are real and tested.
- **Proposed record types:** the same four/five listed above; explicitly
  excluding a new Conversation/Message type (Section 12) and excluding
  Retrieval Result (Section 8).
- **Proposed runtime responsibilities:** a single store implementation
  (mirroring `InMemoryMemoryStore`'s own "one interface, one class"
  precedent) responsible for identity assignment, provenance capture at
  submission, and enforcing the immutable-original/linked-amendment
  correction model — explicitly not responsible for evidence reasoning,
  document parsing, or semantic retrieval.
- **Proposed permission boundary:** every write path requires an
  explicit `PermissionEngine.evaluate` call before commit; every read
  path that could return a sensitive record requires the same, closing
  the gap Section 5 identifies as existing Memory's own unresolved
  weakness — Memory Core should not inherit it silently.
- **Proposed events:** write-side lifecycle events only (creation,
  amendment, dispute, supersession, deletion), per Section 15; no
  retrieval-performed event.
- **Proposed tests:** unit coverage mirroring `InMemoryMemoryStoreTest.kt`'s
  own existing depth (submission, identity scoping, provenance
  preservation, correction-not-destruction, forgetting/auditability) plus
  new coverage specific to non-contemporaneity proof (Section 7) and to
  the boundary decision Section 12 resolves (a test confirming Memory
  Core does *not* duplicate `ConversationEngine`'s own storage, whichever
  way that boundary is decided).
- **Explicit exclusions:** semantic/embedding search; document
  parsing/OCR/file handling of any kind; autonomous learning or
  background consolidation; any Tool- or reasoning-provider-initiated
  write path (until Section 20's open question is resolved); any new
  `PermissionAction`/`ResourceType` enum value (none is needed, Section
  9); persistence/durability (Section 14) beyond an explicitly-documented
  in-memory-only first implementation.
- **Unresolved governance questions:** Section 20, in full.
- **Recommended sequencing for later Memory Core units:** (1) this
  proposed first unit; (2) Evidence Item and Assertion-to-Evidence
  linkage, once (1) is real; (3) the Conversation boundary decision
  (Section 12), implemented either as a reference-only integration or as
  an absorption, once explicitly decided; (4) any retrieval enhancement
  beyond exact/chronological/relationship lookup, including whether
  semantic search ever belongs in Memory Core itself; (5) reconciling
  existing Memory's own `MemoryStore`/`MemoryRecord` with whatever Memory
  Core becomes — Section 20's first, most consequential open question.

---

## 19. Explicit Exclusions

Restated from this Unit's own brief, confirmed by this review to remain
correct exclusions for a first unit, with no repository finding creating
pressure to reconsider any of them: the complete future Memory system;
the World Model; document parsing or OCR; autonomous learning; a vector
database project. Also excluded, per this review's own findings: any
change to existing `MemoryStore`/`InMemoryMemoryStore`/`MemorySource`
(Sections 3.1, 20); any change to `ConversationEngine`
(Section 12); any new `PermissionAction`/`ResourceType` value (Section
9); semantic retrieval (Section 11); evidence *analysis*, as distinct
from evidence *preservation* (Section 17).

---

## 20. Open Governance Questions

Presented in the order they must realistically be resolved, since later
questions depend on earlier ones:

1. **The naming/relationship collision (Section 1, Executive Summary) —
   the single most consequential open question.** Is "Memory Core" a
   new, separate foundational layer that existing "Memory"
   (`MemoryStore`) will eventually be re-platformed onto, reference, or
   remain entirely independent of? Or was "Memory Core" intended, when
   this Programme was scoped, to *be* the next evolution of the existing
   Memory subsystem, under a new name? This review cannot answer this on
   the owner's behalf — the two readings lead to materially different
   Contract Design starting points (a brand-new package/namespace with no
   dependency on `src/interfaces/MemoryStore.kt` at all, versus an
   extension of it), and proceeding to Contract Design without this
   answered risks building against the wrong one.
2. **The sensitive-record permission gap (Section 5).** Must Memory
   Core's first unit close the permission-enforcement gap Section 5
   identifies as already existing in production for current Memory, or
   is it acceptable to build Memory Core's first unit without any
   sensitive record types at all (deferring the gap, rather than
   inheriting it unexamined), and close it only when a genuinely
   sensitive record type is first proposed?
3. **The Conversation boundary (Section 12).** Reference-only, or
   eventual absorption? This does not block a first unit that excludes
   Conversation/Message record types entirely (as Section 18 proposes),
   but it does need answering before Section 18's sequencing step (3) can
   begin.
4. **The sensitivity-model mismatch (Section 9).** Adopt
   `ResourceSensitivity`'s existing nine-value enum directly for Memory
   Core records, or design a separate scheme with an explicit mapping?
5. **The provenance-record shape (Section 7/8).** A separately
   addressable, independently queryable Provenance record, or a
   mandatory embedded value on every other record type? Both satisfy
   this review's own minimal field-set requirement; they are not
   equivalent in cost or flexibility, and this review does not have
   enough concrete downstream-consumer information to prefer one over
   the other with confidence.
6. **Whether a Tool or a reasoning provider may ever submit directly to
   Memory Core** (Section 5), or whether every write must route through
   the Planner Runtime's or Agent Runtime's own judgment first, the way
   existing Memory's architecture anticipates but never actually
   restricts today (nothing currently *prevents* a hypothetical future
   Tool from calling `MemoryStore.remember` directly — the restriction
   exists only because no such Tool has been built yet, not because the
   architecture forbids it).
7. **Durability commitment (Section 14).** Is an explicitly-documented,
   in-memory-only first Memory Core unit acceptable, given the "evidence
   intelligence" use case this Programme names as its own test case
   plausibly depends on the content surviving a restart sooner than
   existing Memory's own preference-store use case ever did?

---

## 21. Recommendation

This review finds substantial, real, already-governed capability in this
repository directly adjacent to Memory Core's brief — a working Memory
subsystem, a working Conversation subsystem, and a type substrate
(`ResourceType`, `PermissionAction`, `ActionVocabularyEntry`) already
sufficient to express Memory Core's eventual permission boundary without
any new enum values. It also finds that none of this adjacent capability
*is* Memory Core, that one of it (existing Memory's own sensitive-content
permission enforcement) has a real, previously-underdocumented gap
Memory Core must not inherit silently, and that the single largest
open question — the relationship between existing "Memory" and the newly
scoped "Memory Core" — is not something this review is positioned to
resolve without the owner's own decision.

Proceeding directly to Contract Design today would mean either silently
assuming an answer to Section 20 Question 1 (a real risk, given how
easily "Memory Core" and "Memory" could be conflated by a reader moving
quickly) or writing a Contract Design that hedges on it, which would
produce a weaker, less binding document than this Programme's own
governance discipline requires everywhere else in this repository.

```
NOT READY FOR CONTRACT DESIGN
```

**What must be resolved first, exactly, before Contract Design begins:**
Section 20, Questions 1 and 2, at minimum — the relationship between
existing Memory and Memory Core, and whether the sensitive-record
permission gap must be closed within Memory Core's first unit or may be
deferred until a sensitive record type is first proposed. Questions 3–7
can reasonably be resolved during Contract Design itself, as
`MEMORY_SOURCE_CONTRACT_DESIGN.md`'s own precedent shows a Contract
Design document resolving a genuine open design question (query
construction, in that case) within its own text — but Questions 1 and 2
are architectural-boundary and constitutional-authority questions of the
same kind Governance Review, not Contract Design, exists to settle.
