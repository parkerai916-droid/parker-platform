# Memory Source Integration — Contract Design

## Status

**Sprint 11, Unit 7. PES-001 Stage 3 (Contract Design).** Builds on
`docs/architecture/MEMORY_SOURCE_GOVERNANCE_REVIEW.md` (the governance
review and repository findings this design implements) and the deferred
boundary `docs/architecture/PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
Section 4.2 named for Memory Source. Companion to
`docs/implementation/MEMORY_SOURCE_INTEGRATION_SCOPE_LOCK.md`, the binding
Included/Excluded terms this design's own boundaries are frozen into.

**This document is contract design only.** No Kotlin is implemented,
proposed as a diff, or changed by it. Neither `src/` nor `tests/` is
touched.

**Revision note (pre-approval architectural refinement pass).** This
revision strengthens three boundaries before this document is submitted
for approval, at Steven's explicit direction: (1) `maximumResults` is no
longer fixed at a specific value anywhere in this document — the
architecture states only that the caller supplies an implementation-
defined maximum, neutral to whatever policy a future implementation
chooses; (2) the retrieval algorithm (matching, ranking, scoring) is
stated explicitly as owned entirely by the Memory implementation behind
`MemoryStore`, never by this contract; (3) a dedicated Constitutional
Boundary section is added, stating plainly what `MemorySource` does not
do. No scope changes. No Kotlin, test, or runtime file is touched by this
revision.

---

## Constitutional Boundary

Restated up front, mirroring `MEMORY_CONTRACT_DESIGN.md`'s own
Constitutional Boundaries section, since every design decision below is
checked against it:

**`MemorySource` exposes retained memories. It does not determine what
becomes memory. It does not promote information into memory. It does not
modify memory. It does not forget memory.**

Memory creation, promotion, modification, and forgetting remain the sole
responsibility of the Memory owner (`MemoryStore`, backed by
`InMemoryMemoryStore`). `MemorySource` is a passive read interface only —
every operation it exposes accepts a query and returns already-existing
`MemoryRecord`s; none of them issues an instruction, a decision, or a
mutation to Memory or to any other subsystem. This is not a new
constraint invented for this Unit — it is `MEMORY_CONTRACT_DESIGN.md`'s
own constitutional boundary ("Memory stores knowledge. Memory never
decides. Memory never acts.") applied to this Unit's own narrower
boundary, restated here so it is checked explicitly rather than assumed.

---

## 1. Ownership of Memory Data

**`MemoryStore` remains the sole, authoritative owner of memory data.**
Nothing in this design introduces a second store, a cache, or a
projection that could disagree with `MemoryStore`'s own records.
`MEMORY_CONTRACT_DESIGN.md` Section 9 already settles `MemoryStore` as
"Memory's one public interface," consulted read-only by "Identity, Trust,
the Planner, the Agent Runtime, the Task Manager... and Reasoning Context
assembly" (Unit A1 §9, cited there) — this Unit is the literal
realisation of that already-named, already-approved consumer.

Memory Source is **not a second component that also owns memory data.**
It is a narrower read-only view over the same instance `MemoryStore`
already is, exactly as `ConversationHistorySource` is a narrower view over
`InMemoryConversationEngine`'s own owned state (Unit 6), never an
independent source of truth. If a Memory Source read and `MemoryStore`'s
own current state ever disagree, `MemoryStore`'s state is correct by
definition — the same projection discipline
`CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 3 already established
for Conversation History Source, restated here for this boundary.

**Whether the existing `MemoryStore` or another component is the
authoritative owner:** the existing `MemoryStore` (backed by
`InMemoryMemoryStore`) is confirmed as authoritative. No other component
is proposed, considered, or required.

---

## 2. How Memory Differs From Conversation History

Both are read-only boundaries feeding `DefaultReasoningContextAssembler`,
and both follow the identical capability-narrowing pattern (a separate,
narrower interface implemented by the same concrete class that owns the
underlying state). The architectural difference is not in shape, but in
what each layer is, per `reasoning-context.md`:

- **Conversation History** is this Conversation's own prior Turns —
  scoped to one `ConversationId`, automatic and unconditional (every
  submitted Turn is retained, no promotion gate), and, per Unit 6's own
  disclosed limitation, one-sided (owner messages only).
- **Memory** is Parker's durable, cross-session, cross-conversation
  knowledge store — not scoped to any one Conversation, deliberately
  gated by `MemoryPromotionPolicy` (a `CandidateMemory` becomes a
  `MemoryRecord` only if promoted; most are not, by design), and
  retrieved by relevance-text matching against a Principal's entire
  retained knowledge, not by conversation membership.

Concretely: Conversation History answers "what did this Conversation
already say?"; Memory answers "what has Parker already learned about this
Principal, generally, that might bear on the current request?" A
Conversation's Turns are never themselves memories unless a separate,
explicit act promotes one (`reasoning-context.md`: "Promotion into Memory
is never automatic") — this Unit does not create, and explicitly excludes
(Scope Lock Section 2), any path from Conversation History into Memory.

---

## 3. How Memory Differs From World Model

Per `reasoning-context.md`'s own three-layer definition: the **World
Model** is Parker's live, current-state belief ("what Parker believes is
true right now"), continuously updated by sensors and other live inputs,
expected to change frequently. **Memory** is durable, slow-changing,
deliberately-entered knowledge ("what Parker has learned"). Memory Source
reads only from Memory. It does not read, reference, or depend on the
World Model in any way — `World Model Source` remains the third, still
undesigned, still deferred boundary
`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 4.2 names, and
this Unit does not narrow that gap, design around it, or introduce any
World Model dependency (Scope Lock Section 2).

---

## 4. The Narrow Read Interface Required by the Assembler

### 4.1 The abstraction

```kotlin
// src/interfaces/MemorySource.kt (new file)
fun interface MemorySource {
    suspend fun recall(query: MemoryQuery): List<MemoryRecord>
}
```

One operation, `suspend`-declared, mirroring `ConversationHistorySource`'s
own single-method shape. `recall` delegates directly to
`MemoryStore.retrieve` — no new query type, no new result type, no new
field. `MemoryQuery` and `MemoryRecord` are already fully designed,
already reviewed, and already implemented (`MEMORY_CONTRACT_DESIGN.md`
Sections 3, 7; `src/interfaces/MemoryStore.kt`) — this Unit reuses them
unchanged.

**`MemorySource` accepts a query representing the caller's retrieval
intent. It expresses no retrieval algorithm of its own.** `MemoryQuery`
states *what* the caller is asking for (a requesting Principal, a
relevance criterion, a correlation identifier, a maximum result count) —
it is a request shape, not an algorithm. **The retrieval algorithm itself
— how `relevance` is matched, how candidates are ranked, how ties are
broken, how many are ultimately returned within the caller's stated
maximum — is owned entirely by the Memory implementation behind
`MemoryStore`, today and in every future revision.** `MemorySource`'s own
contract does not define, name, or presuppose substring search, semantic
search, embeddings, a ranking algorithm, a scoring function, or any other
retrieval heuristic — every one of those remains an implementation detail
of whichever concrete `MemoryStore` a future `MemorySource` is backed by,
exactly as `MEMORY_CONTRACT_DESIGN.md` Section 8 already reserves that
territory for the deferred `MemoryRetrievalPolicy` seam. This contract
exposes only the read boundary: submit a query, receive matching records.
How those records are found is never this contract's concern.

The method is named `recall`, not `retrieve`, deliberately: a caller
holding a `MemorySource`-typed reference must never be confused for a
caller holding a `MemoryStore`-typed reference, even though the
underlying operation is identical — the same naming discipline that keeps
`ConversationHistorySource.history` visibly distinct from
`ConversationEngine.submitTurn`/`resolveConversationId` at every call
site.

### 4.2 Backing implementation

**Decision: `InMemoryMemoryStore` implements `MemorySource` directly, as
a second, narrower interface over the same instance and the same owned
state — not a second store, and not a new method added to the
`MemoryStore` interface itself.** This is the identical pattern Unit 6
established for `ConversationEngine`/`ConversationHistorySource`:

```kotlin
class InMemoryMemoryStore(
    private val promotionPolicy: MemoryPromotionPolicy = DefaultMemoryPromotionPolicy(),
) : MemoryStore, MemorySource {
    // remember/retrieve/forget unchanged (MemoryStore)
    override suspend fun recall(query: MemoryQuery): List<MemoryRecord> = retrieve(query)
}
```

`recall` is a direct, zero-logic delegate to the already-implemented,
already-tested `retrieve`. No new map, no new lock, no new field is
added to `InMemoryMemoryStore` — unlike Unit 6, which had to additively
extend `InMemoryConversationEngine`'s own owned state (`turnsById`) because
`ConversationEngine` did not yet retain Turn content. `MemoryStore.retrieve`
already returns everything Memory Source needs; this Unit adds no new
owned state anywhere.

**Why not add `recall` to `MemoryStore` itself.** A caller holding a
`MemorySource`-typed reference must be structurally unable to call
`remember` or `forget` — the same reasoning Unit 6 already applied to keep
`submitTurn`/`resolveConversationId` unreachable through
`ConversationHistorySource`. Declaring `recall` on a separate interface
means `DefaultReasoningContextAssembler` can be given a `MemorySource`
reference and nothing more: it can read memories, and it cannot create,
promote, or forget one, enforced by the Kotlin type system, not by
convention.

**Why not an independent store, and why not a `MemoryRetrievalPolicy`
seam.** `MEMORY_CONTRACT_DESIGN.md` Section 8 already named
`MemoryRetrievalPolicy` as an approved-but-deferred seam for relevance
ranking, explicitly "not required for a first implementation." This Unit
does not implement it, invent a substitute for it, or reach around it —
`InMemoryMemoryStore.retrieve`'s existing fixed strategy (substring
match, most-recently-promoted-first) is exactly the "simple, fixed
strategy" that document anticipated a first implementation would use.
Memory Source inherits this strategy unchanged; introducing any ranking
logic of its own would both duplicate `MemoryRetrievalPolicy`'s
already-named future role and violate this Unit's own Scope Lock
exclusion on "semantic search unless already contractually required."

---

## 5. Constructing a `MemoryQuery` — the Genuine New Design Question

Unlike `ConversationHistorySource.history(conversationId)`, which takes
only an already-resolved key, `MemoryQuery` mandates four fields the
Assembler must supply on every call: `requestingPrincipalId`,
`relevance: String` (non-blank), `correlationId: String` (non-blank), and
`maximumResults: Int` (positive). This is a real difference from Unit 6,
named plainly rather than smoothed over.

**Decision: `DefaultReasoningContextAssembler` constructs the `MemoryQuery`
itself, from fields already present on its own `ResolvedInboundMessage`
input, using no new derived concept:**

- `requestingPrincipalId` = `resolvedMessage.message.senderPrincipalId` —
  the same `PrincipalId` already used to render "Requesting principal."
- `relevance` = `resolvedMessage.message.text` — the current request's own
  text, passed as-is into `InMemoryMemoryStore.retrieve`'s existing,
  already-implemented, already-tested case-insensitive substring match
  against `MemoryRecord.knowledgePayload`. This is not semantic search and
  invents no ranking algorithm; it reuses the one matching behaviour
  `MemoryQuery.relevance` already, contractually, requires every caller to
  supply a value for.
- `correlationId` = `resolvedMessage.message.correlationId.value` — the
  same correlation identifier already threaded through this Unit's own
  logging (`ParkerRuntime.submitOwnerMessage`'s "Reasoning Context
  assembled (correlationId=...)").
- `maximumResults` = **the caller specifies the maximum number of
  memories requested; this document does not fix that number.**
  `MemoryQuery`'s own `init` block requires a positive value, so the
  Assembler must supply one, but *which* value is implementation policy,
  not architectural design — this Contract Design deliberately does not
  name a specific figure, so that it remains neutral to whatever policy an
  implementation chooses: a fixed limit, a dynamic token budget, a
  model-specific limit, or a configurable policy value. This is the
  identical caller-supplies-the-bound assignment
  `MEMORY_CONTRACT_DESIGN.md` Section 7 already gives `MemoryQuery` itself
  ("the caller decides *how many* it wants back") — this Unit's own
  implementation chooses a concrete number when it is built; this
  document does not anticipate or constrain that choice.
- `category` = `null` — no category narrowing. Nothing in this Unit's
  scope justifies the Assembler guessing a `MemoryCategory` for an
  arbitrary inbound request.

This decision is called out explicitly, not left implicit, because it is
the one place this Unit exercises real judgment beyond wiring: it decides
*what the Assembler asks Memory for*, not merely *how the answer is
rendered*. This mirrors, and extends, the same responsibility the
Assembler already holds for Conversation History's rendering format
(`CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 1: "Nothing about
assembling that excerpt... belongs to it [the Source]") — here, the
Assembler additionally decides the shape of what it asks for, because
`MemoryQuery`'s own pre-existing contract requires that shape from
somewhere, and no other component in this architecture is positioned to
supply it.

---

## 6. Ordering, Provenance, Confidence, and Absence Semantics

All four are **already supported by the existing repository** — this
Unit surfaces them, invents none:

- **Ordering.** `InMemoryMemoryStore.retrieve` already returns results
  most-recently-promoted-first, deterministically
  (`InMemoryMemoryStoreTest.kt`: "`retrieve returns deterministic,
  most-recently-promoted-first results`"). This ordering is, and remains,
  the Memory implementation's own algorithm, not `MemorySource`'s —
  Memory Source neither defines nor influences it, only inherits whatever
  order the implementation behind `MemoryStore` produces. The Assembler
  renders entries in the order `recall` returns them, exactly as it
  already does for `ConversationHistorySource.history`.
- **Provenance.** `MemoryRecord.sourceSubsystem`, `correlationId`, and
  `originatingPrincipalId` already exist. The Assembler's rendered entry
  surfaces `sourceSubsystem` (e.g. "source: <sourceSubsystem>") — nothing
  is fabricated; a record with no `originatingPrincipalId` (a
  system-originated memory) renders without one, never a placeholder
  value invented to fill the gap.
- **Confidence.** `MemoryRecord.confidence: Double?` already exists. The
  Assembler renders it when present ("confidence: <value>") and omits it,
  rather than defaulting to a fabricated number, when `null` — the same
  "render what's there, disclose what isn't" discipline already governing
  `IdentityService.resolve`'s "identity not resolved" fallback entry.
- **Absence.** `retrieve` (and therefore `recall`) already returns an
  empty `List<MemoryRecord>`, never throws, when nothing matches. Memory
  Source inherits this convention unchanged — the same "empty, never
  throw" pattern already shared by `ConversationHistorySource.history` and
  `IdentityService.resolve`. An empty result renders zero entries, exactly
  as "no tools" and "no prior Turns" already render nothing.

---

## 7. Failure Behaviour

No `try`/`catch` is added to `DefaultReasoningContextAssembler` for this
collaborator, consistent with its existing, unmodified discipline
(`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 6, "no
`try`/`catch` anywhere in this class"). If `memorySource.recall` throws,
the fault propagates unchanged to `assemble`'s own caller
(`ParkerRuntime.submitOwnerMessage`'s existing outer `try`/`catch`) —
identical treatment to a fault from `identityService.resolve`,
`toolRegistry.listAll`, or `conversationHistorySource.history` today.
"No matches" is never treated as a failure (Section 6, above); only a
genuine fault (e.g. an unexpected exception from a future, different
`MemorySource` implementation) propagates as one.

---

## 8. Reads Are Passive and Side-Effect Free

`recall` delegates directly to `retrieve`, a pure `Mutex`-guarded read of
`InMemoryMemoryStore`'s own `records` map
(`records.values.filter { ... }.asReversed().take(query.maximumResults)`)
— no mutation, no promotion, no forgetting, no write of any kind.
`MemorySource`'s own declared type makes `remember` and `forget`
structurally unreachable through it, so no future caller holding only a
`MemorySource` reference could introduce a side effect even by mistake.
This satisfies `reasoning-context.md`'s own rule directly: "Nothing in
this flow allows a reasoning provider to write back into Memory or the
World Model directly; any such change is a separate, governed act, not a
side effect of reasoning" — reading a memory to populate a
`ReasoningContext` entry is exactly the kind of read that rule anticipates
and permits; nothing about this design writes back.

---

## 9. Runtime Construction and Dependency Direction

**This is the one place this Unit's wiring differs materially from
Unit 6's** (Governance Review Finding 1): `InMemoryMemoryStore` is
constructed **nowhere** in `ParkerRuntime` today. This Unit adds a new
construction step, not merely a reordering:

```kotlin
// New, inside buildAndRegisterRuntimeGraph(), alongside the other
// stateless collaborators already constructed there:
val inMemoryMemoryStore = InMemoryMemoryStore()
val memorySource: MemorySource = inMemoryMemoryStore

reasoningContextAssembler = stage("Reasoning Context Assembler construction") {
    DefaultReasoningContextAssembler(
        identityService,
        toolRegistry,
        conversationHistorySource,
        memorySource,
    )
}
```

`InMemoryMemoryStore()` takes its default `DefaultMemoryPromotionPolicy()`
— no new configuration, no new `ParkerRuntimeConfig` field. Construction
order requires only that `inMemoryMemoryStore` exist before
`DefaultReasoningContextAssembler` is built, exactly as
`inMemoryConversationEngine` already must (Unit 6) — no other ordering
constraint is introduced. Dependency direction is unchanged in kind:
`ParkerRuntime` constructs `MemorySource` and injects it into the
Assembler; the Assembler never constructs, looks up, or reaches for its
own collaborator.

**A disclosed characteristic this Unit does not change (Governance
Review Finding 5):** `InMemoryMemoryStore` performs no operating-Principal
check of its own. This Unit does not add one — doing so would be an
architectural change to `MemoryStore`/`InMemoryMemoryStore` this Unit is
not authorised to make (Scope Lock Section 2). Memory Source's own
`recall` is exactly as permissive, in this one respect, as `MemoryStore.retrieve`
already is.

**A disclosed, currently-inert consequence:** since nothing in this
Unit's own scope creates memories (Scope Lock Section 2: "changing how
memories are created" is excluded), the newly-constructed
`InMemoryMemoryStore` will, in production, always be empty at the moment
of `ParkerRuntime.start()` and will remain empty unless some future,
separately-scoped mechanism calls `remember`. `recall` will therefore
return an empty list on every real request until such a mechanism exists.
This is not a defect in this Unit's design — it is the correct, honest
consequence of wiring a read boundary before any write path exists, named
here rather than discovered later.

---

## 10. Test Strategy

- **`InMemoryMemoryStore` as `MemorySource`.** A small, additive set of
  tests confirming `InMemoryMemoryStore` satisfies `MemorySource` (that
  `recall` returns what `retrieve` would, and that a `MemorySource`-typed
  reference structurally cannot reach `remember`/`forget` — a
  compile-time/structural proof, mirroring
  `InMemoryMemoryStoreTest.kt`'s own existing "no caller-facing promote"
  structural test). `retrieve`'s own substantive behaviour — identity
  scoping, category narrowing, `maximumResults` capping, deterministic
  ordering — is **already exhaustively tested** by
  `InMemoryMemoryStoreTest.kt`'s 14 existing tests; this Unit does not
  re-prove those, only that `recall` inherits them by direct delegation.
- **`DefaultReasoningContextAssembler` tests.** A `FakeMemorySource`
  (lambda-based, call-count/call-argument tracking, mirroring
  `FakeConversationHistorySource`) exercises: empty `recall` result
  renders nothing but is still called once; a single `MemoryRecord`
  renders one entry, correctly surfacing confidence/provenance when
  present and omitting them when absent; multiple records render in the
  order `recall` returns them; a `recall` failure propagates unchanged;
  the `MemoryQuery` the Assembler constructs carries the exact
  `senderPrincipalId`, request `text`, and `correlationId` from the
  `ResolvedInboundMessage` under test; the structural constructor-
  dependency test is updated to
  `setOf("IdentityService", "ToolRegistry", "ConversationHistorySource", "MemorySource")`.
- **One real-collaborator integration test.** A test constructing a real
  `InMemoryMemoryStore`, calling `remember` directly to seed one promoted
  memory, then calling `DefaultReasoningContextAssembler.assemble`
  directly (not through `ParkerRuntime`) to confirm the seeded memory is
  retrieved and rendered end-to-end through the real backing
  implementation — mirroring how `ConversationReplyCoordinatorTest.kt`'s
  own "real stack" test already exercises a real
  `InMemoryConversationEngine` rather than only a fake.

**Disclosed test-coverage limitation.** A full `ParkerRuntime`-level
(HTTP-prompt) integration test proving a memory renders in a real
production request is **not achievable within this Unit's own scope**:
`ParkerRuntime` constructs its `InMemoryMemoryStore` privately, with no
external seeding hook, and this Unit's own Scope Lock excludes "changing
how memories are created" — so no in-scope path exists for a
`ParkerRuntime`-level test to place a promoted memory into the instance
`ParkerRuntime` builds internally before submitting a message. This is
recorded here as a disclosed, deliberate gap, not silently worked around
by adding an out-of-scope seeding capability — the same honest-disclosure
convention Unit 5 already used for its own analogous test-coverage gap.
The real-collaborator integration test above (Assembler-level, not
`ParkerRuntime`-level) is this Unit's best available substitute and is
judged sufficient.

---

## 11. Disclosed Limitations

- **No `MemoryRetrievalPolicy`.** Ranking remains the fixed, existing
  substring-match/most-recent-first strategy (Section 4.2). A future,
  separately-scoped Unit may implement the deferred
  `MemoryRetrievalPolicy` seam `MEMORY_CONTRACT_DESIGN.md` Section 8
  already named; this Unit does not.
- **`maximumResults`'s concrete value is deliberately left to a future
  implementation Unit, not fixed here.** This document states only that
  the caller supplies an implementation-defined maximum appropriate for
  the current reasoning context (Section 5) — a fixed limit, a dynamic
  token budget, a model-specific limit, or a configurable policy are all
  equally compatible with this Contract Design. This is a deliberate
  architectural neutrality, not an open gap: naming a specific figure here
  would smuggle implementation policy into architecture, which this
  revision specifically avoids.
- **The constructed `InMemoryMemoryStore` starts, and in production
  remains, empty** (Section 9) until a separate, future, out-of-scope
  mechanism populates it via `remember`. This Unit's own read path is
  therefore inert in production until that future mechanism exists —
  disclosed here rather than treated as a defect of this Unit.
- **No `ParkerRuntime`-level integration test for memory rendering**
  (Section 10) — a disclosed, deliberate test-coverage gap, not silently
  worked around.
- **No operating-Principal precondition on `MemorySource`/`MemoryStore`**
  (Section 9) — an inherited Sprint-4 characteristic, not something this
  Unit introduces or corrects.
- **World Model Source remains entirely undesigned** — this Unit closes
  only the Memory Source boundary; `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
  Section 4.2's third named boundary is untouched.
