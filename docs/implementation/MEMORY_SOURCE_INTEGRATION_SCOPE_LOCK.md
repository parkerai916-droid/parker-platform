# Memory Source Integration — Scope Lock

## Status

**Sprint 11, Unit 7. Binding, frozen Scope Lock for a future
implementation Unit.** Companion to
`docs/architecture/MEMORY_SOURCE_GOVERNANCE_REVIEW.md` (repository review
and readiness determination) and
`docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md` (the interface design
and integration point this Scope Lock freezes). Included and Excluded
lists below (Sections 1 and 2) are binding contract terms a future
implementation must satisfy exactly, not redesign.

**This document authorises no implementation.** Producing this Scope Lock
is the final deliverable of this governance/design pass — per Steven's
explicit instruction, no Kotlin is written, staged, committed, or pushed
as part of this Unit's own work.

**Revision note (pre-approval architectural refinement pass).** This
revision strengthens this Scope Lock before submission for approval, at
Steven's explicit direction: the Included list no longer names a fixed
`maximumResults` value (Section "Included," below — now
implementation-defined, neutral to whatever bounding policy a future
implementation chooses); the Excluded list gains an explicit line ruling
out any retrieval algorithm of Memory Source's own (substring search,
semantic search, embeddings, ranking, scoring, heuristics — all owned by
the Memory implementation behind `MemoryStore`); and Section 3 (Governing
Principle) gains an explicit constitutional restatement of the passive-
read boundary. No scope changes. No Kotlin, test, or runtime file is
touched by this revision.

---

## 1. Responsibilities — What Memory Source Owns

Memory Source owns exactly one responsibility: **retrieving already-
promoted, already-existing `MemoryRecord`s matching a caller-supplied
`MemoryQuery`.**

That single responsibility decomposes into what it must be able to
answer, in prose, without presupposing more than the Contract Design
already fixes:

- **Matching memories.** Given a `MemoryQuery` (constructed by the
  Assembler from fields already present on `ResolvedInboundMessage`, per
  Contract Design Section 5), Memory Source returns the `MemoryRecord`s
  `MemoryStore.retrieve` already, deterministically, returns for that
  query — most-recently-promoted-first, scoped to the requesting
  Principal, capped at the supplied `maximumResults`.
- **Nothing beyond retrieval.** It does not decide which memories are
  promoted, does not rank beyond `InMemoryMemoryStore.retrieve`'s existing
  fixed strategy, does not summarise or reshape a `MemoryRecord`'s
  content, and does not decide what a caller should ask for — query
  construction is the Assembler's own responsibility (Contract Design
  Section 5), not Memory Source's.

Memory Source retrieves already-promoted memories. Nothing more.

---

## Included / Excluded (binding)

### Included

- A new, single-method, `suspend`-declared interface, `MemorySource`
  (`src/interfaces/MemorySource.kt`), returning `List<MemoryRecord>` for a
  `MemoryQuery`, delegating directly to `MemoryStore.retrieve`'s own
  existing behaviour (ordering, scoping, capping, absence semantics
  unchanged).
- `InMemoryMemoryStore` implementing `MemorySource` directly, as a second
  interface over its own existing owned state and existing `retrieve`
  implementation — no new map, no new lock, no new field.
- One additional constructor dependency on
  `DefaultReasoningContextAssembler` (`memorySource: MemorySource`), and
  the Assembler's own construction of a `MemoryQuery` from fields already
  present on `ResolvedInboundMessage` (`senderPrincipalId`, `text`,
  `correlationId`), plus an implementation-defined `maximumResults`
  (a caller-supplied bound, not fixed by architecture — Contract Design
  Section 5) and `category = null`.
- Rendering of zero or more additional `ReasoningContext` entries, one per
  returned `MemoryRecord`, surfacing only fields `MemoryRecord` already
  carries (`knowledgePayload`, `confidence` where present,
  `sourceSubsystem`).
- The one new production wiring step this requires in `ParkerRuntime`:
  constructing `InMemoryMemoryStore()` (default `DefaultMemoryPromotionPolicy()`)
  and passing it to `DefaultReasoningContextAssembler`'s constructor under
  the `MemorySource` type, in addition to its existing three collaborators.
- Focused unit tests for `InMemoryMemoryStore` as `MemorySource` (structural
  proof only — behavioural coverage of `retrieve` already exists), Assembler-
  level tests via a `FakeMemorySource`, and one real-collaborator (real
  `InMemoryMemoryStore`, seeded via `remember` directly) Assembler-level
  integration test (Contract Design Section 10).
- Only behaviour already supported by `MemoryStore`'s existing, already-
  approved contracts (`MEMORY_CONTRACT_DESIGN.md`) — ordering, provenance,
  confidence, and absence semantics inherited unchanged, invented nowhere.

### Excluded

- **Changing how memories are created.** No new path by which a
  `CandidateMemory` is submitted or promoted is introduced. `remember` is
  never called by `DefaultReasoningContextAssembler`, `MemorySource`, or
  any part of this Unit's own production wiring.
- **Memory extraction from conversation.** No mechanism is added that
  observes a Turn, a reply, or any conversation content and proposes it as
  a `CandidateMemory`. That remains an entirely separate, future,
  separately-scoped concern.
- **Memory summarisation.** Memory Source does not condense, compress, or
  paraphrase a `MemoryRecord`'s `knowledgePayload`. Whatever it returns is
  retrieved, not authored.
- **Embeddings.** No vectorisation, embedding, or index of any
  `MemoryRecord` content is introduced.
- **Semantic search, unless already contractually required.** Memory
  Source performs no ranking or ordering beyond
  `InMemoryMemoryStore.retrieve`'s existing, already-implemented,
  already-tested fixed strategy (case-insensitive substring match,
  most-recently-promoted-first). The `relevance` field's existing,
  mandatory substring-match behaviour is inherited because `MemoryQuery`
  already requires it of every caller — this is not new semantic search
  introduced by this Unit.
- **Any retrieval algorithm of its own.** `MemorySource` does not define
  substring search, semantic search, embeddings, a ranking algorithm, a
  scoring function, or any other retrieval heuristic. Every one of those
  is, and remains, an implementation detail owned entirely by the Memory
  implementation behind `MemoryStore`. `MemorySource` exposes only the
  read boundary — submit a query, receive matching records — never the
  means by which a match is found.
- **Confidence invention.** No confidence figure is computed, estimated,
  or defaulted where `MemoryRecord.confidence` is `null`. Absent means
  absent, rendered as absent.
- **Provenance invention.** No `sourceSubsystem`, `originatingPrincipalId`,
  or `correlationId` is fabricated. Only what a `MemoryRecord` already
  carries is surfaced.
- **Forgetting policy.** Memory Source never calls `forget`, and this
  Unit does not change when, why, or by whom a memory is forgotten.
- **Contradiction resolution.** If multiple retrieved `MemoryRecord`s
  disagree in content, Memory Source and the Assembler render all of them,
  unresolved — no reconciliation, preference, or deduplication logic is
  introduced.
- **World Model integration.** `MemorySource` has no dependency on, and
  never reads, the World Model. World Model Source remains the third,
  entirely separate, undesigned boundary
  `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 4.2 names.
- **Authentication implementation.** No authentication or trust concept is
  introduced. `AUTHENTICATION_AND_TRUST_GOVERNANCE.md` remains untouched
  and ungoverned by this document.
- **Permissions changes.** No `PermissionEngine`, `PermissionPolicy`, or
  authorisation decision is introduced or altered. Memory Source is
  capability, not authority.
- **Planner work.** No dependency on `PlannerRuntime`, `PlanDecision`, or
  any planning concept.
- **Tool execution.** No dependency on `ToolRegistry.resolve`, any `Tool`
  handle, `ToolInvocationBinding`, or the Execution Pipeline.
- **Outbound-reply capture.** This Unit does not touch, extend, or resolve
  Conversation History Source's own disclosed one-sided-history
  limitation. That remains Unit 6's own, separately-scoped, unresolved
  disclosure.
- **Conversation History volume limits.** This Unit does not add a bound
  to `ConversationHistorySource.history`'s own, separately disclosed,
  unbounded-volume limitation. Memory Source's own `maximumResults` (fixed
  at 5, Contract Design Section 5) governs Memory Source alone.
- **Unrelated refactoring.** No change to `MemoryStore`, `MemoryRecord`,
  `MemoryQuery`, `MemoryCategory`, `MemoryPromotionDecision`,
  `MemoryPromotionPolicy`, `DefaultMemoryPromotionPolicy`, or any file not
  directly named in the Included list above.

---

## 2. Explicit Exclusions — What It Must Never Own

Restated in the same closing form
`CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 2 already established:
Memory Source references information. It does not own systems.

- **Memory creation, promotion, or forgetting.** Exclusively
  `MemoryStore`'s own, sole, unchanged responsibility, invoked only
  through `remember`/`forget` — operations `MemorySource` cannot even
  express.
- **World Model.** A distinct, separate architectural concept ("what
  Parker currently believes," live and frequently changing, per
  `reasoning-context.md`). Memory Source must never expose World Model
  state, and must never be extended to read it.
- **Conversation History.** A distinct, already-closed architectural
  concept (Unit 6). Memory Source must never be conflated with it,
  substituted for it, or used to work around its own disclosed
  limitations.
- **Planner.** Memory Source performs no planning, never invokes
  `PlannerRuntime`, and never reasons about what a retrieved memory
  implies should happen next.
- **Tool execution.** No dependency on `ToolRegistry.resolve`, any `Tool`
  handle, `ToolInvocationBinding`, or the Execution Pipeline exists or is
  ever introduced.
- **Ranking or relevance policy.** Memory Source does not implement
  `MemoryRetrievalPolicy` (`MEMORY_CONTRACT_DESIGN.md` Section 8,
  deferred) or any substitute for it. Any such capability, if ever needed,
  belongs to a future, separately-scoped and separately-justified
  component — not folded into this boundary "since it's already there."

---

## 3. Governing Principle

**Memory Source is a read boundary, not a memory owner. It exposes
already-promoted memories. It never creates, promotes, forgets, or
mutates one.**

Stated without hedging, mirroring `MEMORY_SOURCE_CONTRACT_DESIGN.md`'s own
Constitutional Boundary section: **Memory Source exposes retained
memories. It does not determine what becomes memory. It does not promote
information into memory. It does not modify memory. It does not forget
memory.** Memory creation, promotion, modification, and forgetting remain
responsibilities of the Memory owner (`MemoryStore`) alone. Memory Source
is a passive read interface only — every operation it exposes accepts a
query and returns already-existing records; none of them decides, acts,
or mutates on Memory's behalf.

Every item Memory Source can ever return has exactly one other home:
`MemoryStore`'s own owned `MemoryRecord` state. Memory Source originates
nothing and never becomes canonical. If a Memory Source read and
`MemoryStore`'s own current state ever disagree, `MemoryStore`'s state is
correct and the read is simply stale — the identical projection
discipline already established for `ReasoningContext` itself
(`PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md` Section 3) and for
Conversation History Source (`CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md`
Section 3).

---

## 4. Ownership

- **Exactly one production owner.** `parker.composition.ParkerRuntime`
  constructs Memory Source (via `InMemoryMemoryStore`), exactly once, at
  startup — mirroring
  `CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 4's identical
  ownership shape.
- **Exactly one production caller.** `DefaultReasoningContextAssembler` —
  calling `memorySource.recall(query)` directly inside `assemble`,
  mirroring how it already reads `identityService`, `toolRegistry`, and
  `conversationHistorySource` directly. No coordinator, no Reasoning
  Provider, and no other runtime component may become a second caller
  without a future Scope Lock revision.

---

## 5. Lifetime

- **Construction.** Once, at startup, alongside every other stateless
  collaborator `ParkerRuntime` builds. Construction failure is reported
  the same way every other collaborator's construction failure already is
  (`ParkerRuntime`'s own existing `stage()` pattern).
- **Use.** Read-only, per request. Each read is independent; nothing
  observed or returned by one read is retained for, or influences, the
  next.
- **Disposal.** A returned `List<MemoryRecord>` is discarded once its
  caller is finished with it. Memory Source itself is never torn down and
  reconstructed per request.

---

## 6. Threading

- **Sharing.** A single Memory Source instance is shared across every
  concurrent request the runtime handles, exactly as every other
  stateless, constructed-once collaborator in this runtime already is —
  `InMemoryMemoryStore`'s own existing `Mutex` already guards concurrent
  `retrieve` calls, unchanged by this Unit.
- **Immutability.** Whatever Memory Source returns is immutable from the
  moment it is returned. A given returned list is never shared across more
  than one caller's own handling of one request.
- **Coroutine expectations.** `recall` is `suspend`-declared, consistent
  with every other read-only dependency this runtime already relies on.

---

## 7. Relationship to the Constitution

`docs/architecture/parker-constitution.md`: "Parker owns authority.
Modules provide capability" and "Cognition proposes. Trust authorises.
Runtime executes." Memory Source is capability, not authority — it
proposes nothing, authorises nothing, and executes nothing. It is a read
boundary `DefaultReasoningContextAssembler` draws on, exactly as
`IdentityService`, `ToolRegistry`, and `ConversationHistorySource` already
are — never itself a source of proposal, authority, or execution.

---

## 8. Acceptance of This Scope Lock

This Scope Lock is binding once accepted. A future implementation Unit
authorised against it must satisfy the Included list exactly, must not
implement anything in the Excluded list, and must treat any discovered
need to exceed either list as grounds to pause and request a Scope Lock
revision — not as licence to proceed under the existing one.
