# Memory Source Integration — Governance Review

## Status

**Sprint 11, Unit 7. PES-001 pre-Contract-Design governance review.**
Companion to `docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md` (the
interface design this review clears the way for) and
`docs/implementation/MEMORY_SOURCE_INTEGRATION_SCOPE_LOCK.md` (the binding
Included/Excluded terms). This document is governance only — no Kotlin is
implemented, proposed as a diff, or changed by it. Neither `src/` nor
`tests/` is touched.

**Explicit project decision, restated.** Steven has designated the next
implementation unit: **Sprint 11 Unit 7 — Memory Source Integration**,
closing the second of the three deferred dependency boundaries
`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 4.2 named
(Memory Source, World Model Source, Conversation History Source —
Conversation History Source closed by Unit 6). This is not a request to
implement a new memory system; Memory already exists (Sprint 4, Track A).
This Unit defines the narrow read boundary through which
`DefaultReasoningContextAssembler` can receive memory information.

---

## 1. Repository Review (fresh reads, this Unit)

Read directly, not from memory, immediately before this document was
written:

- `docs/architecture/parker-constitution.md`.
- `docs/architecture/reasoning-context.md` — the Memory / World Model /
  Reasoning Context three-layer split this Unit must not blur.
- `docs/architecture/PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
  Sections 4.1–4.3 — Memory Source's own named, undesigned boundary.
- `docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md` (Sprint 4, Track A,
  Unit A1) and `docs/architecture/MEMORY_CONTRACT_DESIGN.md` (Unit A2) —
  Memory's existing, already-accepted architecture and field-level
  contracts.
- `docs/architecture/17-memory-architecture.md`,
  `docs/architecture/33-memory-consolidation.md`,
  `docs/adr/ADR-002-memory-context-world-model-separation.md`,
  `docs/adr/ADR-008-memory-promotion.md`.
- `docs/architecture/CONVERSATION_HISTORY_SOURCE_CONTRACT_DESIGN.md` and
  `docs/implementation/CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` — the
  direct architectural precedent this Unit follows wherever the two
  situations are genuinely alike, and departs from, explicitly, wherever
  they are not.
- `docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md` — checked
  for any conflict or precondition.
- The current, real source of `src/interfaces/MemoryStore.kt`,
  `src/runtime/InMemoryMemoryStore.kt`,
  `src/runtime/DefaultMemoryPromotionPolicy.kt`,
  `src/runtime/DefaultReasoningContextAssembler.kt`,
  `src/composition/ParkerRuntime.kt`, and
  `tests/runtime/InMemoryMemoryStoreTest.kt`.

### Central findings, not previously recorded this precisely

**Finding 1 — Memory has no production wiring today.** A direct grep of
`src/composition/ParkerRuntime.kt` for `MemoryStore` or `WorldModel`
returns no matches. Unlike `ConversationEngine` (already constructed and
wired into the real pipeline before Unit 6 began), `InMemoryMemoryStore`
is constructed nowhere in the running system — it exists only as a
Sprint 4 implementation exercised by its own isolated test suite
(`tests/runtime/InMemoryMemoryStoreTest.kt`). Memory Source Integration
therefore requires a **new** production construction step in
`ParkerRuntime.buildAndRegisterRuntimeGraph()`, not merely a reordering of
an already-wired dependency, as Unit 6 required. This is a materially
larger wiring change than Unit 6's, even though the interface-narrowing
pattern itself is identical in kind.

**Finding 2 — `MemoryStore.retrieve` already mandates two parameters
Conversation History Source's own read never needed.** `ConversationHistorySource.history`
takes only a `ConversationId` — the key alone, already resolved upstream.
`MemoryQuery`, by contrast, requires a non-blank `relevance: String` and a
positive `maximumResults: Int` (enforced by `require(...)` in
`MemoryQuery`'s own `init` block), in addition to a
`requestingPrincipalId` and a non-blank `correlationId`. Nothing before
this Unit decides what an Assembler-driven query should supply for
`relevance` and `maximumResults` — this is a genuine design question this
Unit's Contract Design must resolve explicitly, not a mechanical
reordering like Unit 6's.

**Finding 3 — `MemoryRecord` already carries confidence and provenance;
nothing needs to be invented.** `MemoryRecord.confidence: Double?`,
`sourceSubsystem: String`, `correlationId: String`,
`originatingPrincipalId: PrincipalId?`, `promotedAt: Instant`, and
`history: List<String>` already exist on the type `retrieve` returns.
Memory Source's own job is to surface these already-existing fields when
rendering, never to compute a new confidence figure or fabricate
provenance that isn't already on the record.

**Finding 4 — `InMemoryMemoryStore.retrieve` already has a deterministic
ordering, and an established "empty, never throw" absence convention.**
Confirmed directly against `InMemoryMemoryStoreTest.kt`
("`retrieve returns deterministic, most-recently-promoted-first
results`") and against the implementation itself
(`records.values.filter { ... }.asReversed().take(query.maximumResults)`,
backed by `LinkedHashMap` insertion order, not wall-clock comparison,
avoiding same-tick ties). "No matches" already returns an empty list,
never throws — the same convention `ConversationHistorySource.history`
and `IdentityService.resolve` already established. Memory Source inherits
this ordering and this absence semantic; this Unit does not redesign
either.

**Finding 5 — `MemoryStore` has no operating-Principal precondition, an
asymmetry against `ConversationEngine`/`IdentityService`.**
`InMemoryMemoryStore`'s constructor takes only a `MemoryPromotionPolicy`
(defaulted) — no `IdentityService` dependency, and no
`requireOperatingPrincipalRegistered()`-equivalent check anywhere in its
implementation, unlike `InMemoryConversationEngine`. This is an existing,
Sprint-4-era characteristic this Unit inherits and discloses, not a defect
this Unit is responsible for correcting or introducing.

**Finding 6 — `reasoning-context.md`'s own architecture already names this
exact path.** Section "Architectural Responsibilities": "Memory is
responsible for durable storage and retrieval of long-term knowledge, and
for **exposing only the portions relevant to a given task when Reasoning
Context is assembled**." Memory Source Integration is the realisation of
this already-accepted sentence, not a new architectural invention.
`MEMORY_CONTRACT_DESIGN.md` Section 9 independently confirms "Reasoning
Context assembly" as one of `MemoryStore`'s own already-anticipated,
read-only, caller-facing consumers (Unit A1 §9, cited there).

---

## 2. Governance Review / Conflict Check

Checked directly against the frozen, current state of each governing
document and component:

- **Authentication & Trust** (`AUTHENTICATION_AND_TRUST_GOVERNANCE.md`):
  no reference to Memory, `MemoryStore`, or any reasoning-pipeline read
  boundary anywhere in its sections. No conflict; this Unit introduces no
  authentication, authorisation, or trust concept of its own — it is
  capability, read only, exactly as Conversation History Source already
  is.
- **`reasoning-context.md`'s three-layer separation**: Memory Source reads
  from Memory only. It never reads the World Model, never blends the two,
  and never allows what it reads to be re-promoted into Memory as a side
  effect (`reasoning-context.md`: "Promotion into Memory is never
  automatic... Nothing in Reasoning Context is written into Memory simply
  because it was used, discussed, or reasoned over during a task"). This
  Unit's own read is one further instance of exactly that rule: reading a
  memory into a `ReasoningContext` entry does not promote, re-promote, or
  alter it.
- **`MemoryStore`'s existing three-operation contract** (`remember`,
  `retrieve`, `forget`): none of the three signatures, behaviours, or
  `MEMORY_CONTRACT_DESIGN.md`'s own constitutional boundaries ("Memory
  stores knowledge. Memory never decides. Memory never acts.") are altered
  by this Unit. Memory Source adds a third, narrower, read-only interface
  alongside `MemoryStore` — the same additive pattern Unit 6 already used
  for `ConversationEngine`/`ConversationHistorySource` — not a change to
  `MemoryStore` itself.
- **`DefaultReasoningContextAssembler`**: its determinism, statelessness,
  and side-effect-freedom (`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
  Section 5) are preserved — a new read-only collaborator is not a side
  effect, exactly as `identityService.resolve`, `toolRegistry.listAll`,
  and (since Unit 6) `conversationHistorySource.history` already are not.
- **Conversation History Source Contract Design and Scope Lock**: both
  remain entirely unaltered by this Unit. Memory Source is a sibling
  boundary, not a revision of Conversation History Source's own design.
  Where this Unit's Contract Design departs from Unit 6's precedent (see
  Finding 2, above — query-shaping is a genuinely new problem Unit 6 never
  faced), the departure is named and justified there, not silently
  introduced.
- **`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 4.2**: this
  is the one document that explicitly authorised naming Memory Source as a
  future boundary, without designing it ("Retrieval, relevance, and volume
  are entirely undecided here"). This Unit is that future Contract Design
  revision Section 4.2 anticipated; it does not contradict Section 4.2, it
  fulfils it.

**Conclusion: no architectural conflict exists.** Memory Source
Integration can proceed to Contract Design. The one open design question
this review surfaces and defers to the Contract Design (Section 3, below,
of that document) is how an Assembler-driven `MemoryQuery` is constructed
from a `ResolvedInboundMessage`, given `MemoryQuery`'s own pre-existing
mandatory fields.

---

## 3. Readiness Determination

- **Governance**: clear. No document conflicts with introducing Memory
  Source; `reasoning-context.md` and `MEMORY_CONTRACT_DESIGN.md` both
  already anticipate this exact read path.
- **Contract Design**: not yet written before this review; produced
  alongside this document (`MEMORY_SOURCE_CONTRACT_DESIGN.md`).
- **Scope Lock**: not yet written before this review; produced alongside
  this document (`MEMORY_SOURCE_INTEGRATION_SCOPE_LOCK.md`).
- **Dependencies**: `MemoryStore`/`InMemoryMemoryStore` already exist,
  already tested (`InMemoryMemoryStoreTest.kt`, 14 tests covering
  promotion, retrieval scoping, category narrowing, `maximumResults`
  capping, deterministic ordering, forgetting, and the "no caller-facing
  promote" boundary). No unimplemented dependency blocks this Unit.
- **Blockers**: none found. The only genuine, disclosed design decision
  (query construction, Finding 2) is resolvable within this Unit's own
  scope, using fields already present on `ResolvedInboundMessage` — it
  does not require inventing new memory semantics, ranking, or a change
  to `MemoryQuery` itself.

Implementation may proceed once the Contract Design and Scope Lock below
are both accepted — not before, and not as part of this governance pass.
