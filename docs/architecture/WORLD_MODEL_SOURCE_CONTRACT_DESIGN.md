# World Model Source Integration — Contract Design

## Status

**Implemented.** `src/interfaces/WorldModelSource.kt` and
`InMemoryWorldModel`'s implementation of it now exist exactly as Section 2
below shapes them; `DefaultReasoningContextAssembler` now constructs
`WorldQuery` exactly as Section 3 (Resolved) below states; `ParkerRuntime`
now wires them exactly as Section 8 below shows. See
`docs/implementation/IMPLEMENTATION_HISTORY.md`'s own "Unit 8 -- World
Model Source Integration Implementation" entry for the full record. Not
yet accepted pending Steven's own local build verification.

**Sprint 11, Unit 8. PES-001 Stage 3 (Contract Design).** Builds on
`docs/architecture/WORLD_MODEL_SOURCE_GOVERNANCE_REVIEW.md` (the
governance review and repository findings this design implements) and the
third and final deferred boundary
`docs/architecture/PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
Section 4.2 named for World Model Source. Companion to
`docs/implementation/WORLD_MODEL_SOURCE_INTEGRATION_SCOPE_LOCK.md`, the
binding Included/Excluded terms this design's own boundaries are frozen
into.

**This document is contract design only.** No Kotlin is implemented,
proposed as a diff, or changed by it. Neither `src/` nor `tests/` is
touched. **This is not a request to design or implement a world model** —
the World Model already exists, fully specified and implemented.

---

## Constitutional Boundary

Stated without hedging, mirroring `MEMORY_SOURCE_CONTRACT_DESIGN.md`'s own
Constitutional Boundary section, since every design decision below is
checked against it:

**`WorldModelSource` exposes current world-model state. It does not
determine what becomes world state. It does not create world state. It
does not modify world state. It does not reconcile world state. It does
not forget world state.**

Those responsibilities remain with the World Model owner (`WorldModel`,
backed by `InMemoryWorldModel`) alone, exercised entirely through
`WorldModelUpdatePolicy`, itself never reachable from outside
`InMemoryWorldModel`. `WorldModelSource` is a passive read interface
only — every operation it exposes accepts a query and returns
already-current belief; none of them issues an instruction, a decision,
or a mutation to the World Model or to any other subsystem.

---

## 1. Ownership

**Who owns the World Model? Who is authoritative?** `WorldModel` (backed
by `InMemoryWorldModel`) is the sole, authoritative owner of world-model
state, unchanged by this Unit. `WORLD_MODEL_RUNTIME_ARCHITECTURE.md`
Section 6: "No subsystem outside the World Model owns acceptance of an
Observation into current belief... regardless of which of the seven
sources... submitted the Observation." `WorldModelSource` does not
change, share, or dilute this authority.

**Who may modify it?** Only `WorldModel.observe`, called by one of the
seven named sources (Sensors, Plugins, Agents, Planner [never actually
submits], Runtime, Memory [never actually submits], User), with
acceptance decided exclusively by the internal `WorldModelUpdatePolicy`
seam. `WorldModelSource` cannot modify the World Model under any
circumstance — it does not declare `observe`, so no implementation of it
can expose that capability by accident.

**Who may only read it?** `DefaultReasoningContextAssembler`, via
`WorldModelSource` — the one new, narrow, read-only consumer this Unit
introduces. `WorldModelSource` is not a second store, and not a
component that could disagree with `WorldModel`'s own current state: it
is a narrower view over the same instance, exactly as `MemorySource` is a
narrower view over `MemoryStore`'s own instance
(`MEMORY_SOURCE_CONTRACT_DESIGN.md` Section 1's identical structure,
carried forward here). If a `WorldModelSource` read and `WorldModel`'s
own current state ever disagree, `WorldModel`'s state is correct and the
read is simply stale — the same projection discipline already
established for Conversation History Source and Memory Source.

---

## 2. Read Boundary

### 2.1 The abstraction

```kotlin
// src/interfaces/WorldModelSource.kt (new file)
fun interface WorldModelSource {
    suspend fun recall(query: WorldQuery): List<WorldBelief>
}
```

One operation, `suspend`-declared, mirroring `MemorySource.recall`'s own
single-method shape and naming convention. `recall` reuses `WorldQuery`
and `WorldBelief` unchanged — no new query type, no new result type, no
new field. Named `recall`, not `current` or `query`, for the identical
reason `MemorySource.recall` is not named `retrieve`: a caller holding
only a `WorldModelSource` reference must never be confused for one
holding a full `WorldModel` reference, even though the underlying
behaviour is identical to `WorldModel.query`.

### 2.2 Why `query`, not `current`, is the operation exposed

`WorldModel` exposes two read operations: `current(subject): WorldBelief?`
(one exact subject) and `query(WorldQuery): List<WorldBelief>` (substring
match, confidence floor, result cap). `query`'s own capability is a
strict superset of `current`'s — a caller wanting one exact subject's
belief can supply that subject as `WorldQuery.subjectMatch` with
`maximumResults = 1` and receive the identical answer, since an exact
string is also a valid (and maximally narrow) substring match against
itself. Exposing only `recall(query: WorldQuery)` therefore keeps
`WorldModelSource` to the smallest interface that still lets the Assembler
express either "the belief for this one exact subject" or "beliefs
matching this broader criterion," without adding a second method whose
capability the first already subsumes. This mirrors exactly how Memory
Source excluded `remember`/`forget` from its own narrower interface
(`MEMORY_SOURCE_CONTRACT_DESIGN.md` Section 4.1) — here, the exclusion is
of a redundant *read* method, not a mutating one, but the minimalism
reasoning is the same: do not expose a second surface a caller could
reach through the first.

### 2.3 Backing implementation

**Decision: `InMemoryWorldModel` implements `WorldModelSource` directly,
as a second, narrower interface over the same instance and the same
owned state — not a second store, and not a new method added to
`WorldModel` itself.** Identical in kind to Unit 7's own decision for
`InMemoryMemoryStore`/`MemorySource`:

```kotlin
class InMemoryWorldModel(
    private val updatePolicy: WorldModelUpdatePolicy = DefaultWorldModelUpdatePolicy(),
) : WorldModel, WorldModelSource {
    // observe/current/query unchanged (WorldModel)
    override suspend fun recall(query: WorldQuery): List<WorldBelief> = query(query)
}
```

`recall` is a direct, zero-logic delegate to the already-implemented,
already-tested `query`. No new map, no new lock, no new field is added to
`InMemoryWorldModel`.

**Why not add `recall` to `WorldModel` itself.** A caller holding a
`WorldModelSource`-typed reference must be structurally unable to call
`observe` — the same reasoning already applied twice this Sprint to keep
`submitTurn`/`resolveConversationId` unreachable through
`ConversationHistorySource`, and `remember`/`forget` unreachable through
`MemorySource`. Declaring `recall` on a separate interface means
`DefaultReasoningContextAssembler` can be given a `WorldModelSource`
reference and nothing more: it can read current belief, and it cannot
create, update, invalidate, or reconcile one, enforced by the Kotlin type
system, not by convention.

**The Assembler should request world-model information. It should not
construct, infer, update, merge, or reconcile world state.** This
`recall`-only surface makes each of those structurally impossible except
"construct... world-model information" in one narrow sense addressed
directly in Section 3, below: the Assembler must still construct the
*query it sends*, which is a different act from constructing *world
state itself* and remains entirely within bounds — it is the same kind of
query-shaping responsibility the Assembler already holds for Memory
(`MEMORY_SOURCE_CONTRACT_DESIGN.md` Section 5).

---

## 3. Retrieval

**The Assembler expresses retrieval intent only. The retrieval algorithm
belongs entirely to the World Model implementation.** `WorldQuery` states
what the caller is asking for; it is a request shape, not an algorithm.
How `subjectMatch` is matched, how `minimumConfidence` is applied, how
staleness is excluded, and in what order results are ultimately returned
within the caller's stated `maximumResults` is owned entirely by the
World Model implementation behind `WorldModel` (`InMemoryWorldModel.query`'s
own KDoc, already implemented and tested). This Contract Design does not
specify, and `WorldModelSource` does not define: ranking, scoring,
semantic search, heuristics, embeddings, reconciliation, or contradiction
handling. Every one of those either belongs to a deferred seam this
architecture already reserves (a future retrieval-ranking seam, symmetric
to Memory's own deferred `MemoryRetrievalPolicy`) or is `WorldModelUpdatePolicy`'s
own, already-implemented, internal concern (contradiction handling at
Observation-acceptance time, not at read time).

**Resolved: constructing the `WorldQuery` itself.** Memory Source's own
Contract Design Section 5 resolved an analogous question by supplying the
Assembler's current request text as `MemoryQuery.relevance`, because
`MemoryQuery.relevance` matches against free-text knowledge content a
natural-language request plausibly overlaps with. `WorldQuery.subjectMatch`
matches against `WorldBelief.subject` — a structured topic key, not
free-text content (Governance Review Finding 3) — so that same resolution
could not simply be reused: supplying the inbound request's own text as
`subjectMatch` would have required inventing a mapping from
natural-language text to a specific world-model subject key, a
classification or inference step this Unit's own Scope Lock excludes.

This question was carried, disclosed, into
`docs/architecture/WORLD_MODEL_SOURCE_QUERY_CONSTRUCTION_DECISION.md`,
which found the existing contract genuinely incompatible with every
non-inventive option available at the time (that document's own
Outcome section records the full history) and identified the smallest
possible fix: widen `WorldQuery.subjectMatch` from a mandatory `String` to
an optional `String? = null`. That fix was then reviewed and approved
through its own separate, narrowly-scoped governance track —
`docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_GOVERNANCE_REVIEW.md`,
`docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md`, and
`docs/implementation/WORLD_QUERY_OPTIONAL_SUBJECT_SCOPE_LOCK.md` — and
implemented, tested, locally verified (BUILD SUCCESSFUL), committed, and
pushed as commit `eb25d64` ("feat: allow unfiltered world model queries").
`WorldQuery.subjectMatch` is now nullable; `null` means no subject
filter — match every currently-current belief regardless of subject,
subject only to `minimumConfidence` and `maximumResults`.

**The Assembler may now construct a valid, unfiltered `WorldQuery`:**

- `subjectMatch = null` — no subject filter.
- `maximumResults` — a caller-supplied, implementation-defined bound
  (policy, not architecture, exactly as `MEMORY_QUERY_MAXIMUM_RESULTS`
  already is for Memory Source).
- `minimumConfidence` — the existing, unchanged optional field, supplied
  where a future implementation Unit's own requirements call for it.

The Assembler does not infer, classify, parse, or fabricate a subject —
`null` is a literal absence of a filter, not a computed or guessed value.
**World Model ownership is unchanged: `InMemoryWorldModel.query` alone
owns subject matching, confidence filtering, staleness exclusion, and
result selection; `WorldModelSource` and the Assembler each remain a
passive pass-through of a query and a returned list, exactly as Section
2 above already establishes.**

---

## 4. Separation From Memory

```
Memory
  ↓
retained experiences

World Model
  ↓
current believed state
```

These remain different architectural concepts, per `reasoning-context.md`
and `WORLD_MODEL_RUNTIME_ARCHITECTURE.md` Section 6's own ownership table:
"Memory is a separate, parallel knowledge layer, not a source of live
signal about current reality; nothing in this architecture has Memory
reporting an Observation to the World Model." Concretely: `WorldModelSource`
never reads `MemorySource`, `MemoryStore`, or any `MemoryRecord`; nothing
retrieved by `WorldModelSource` is ever promoted into Memory, and nothing
retrieved by `MemorySource` is ever fed into the World Model as an
Observation. The Assembler consumes both as entirely independent
collaborators and combines only their *rendered entries* into one
`ReasoningContext` — it never cross-references or reconciles their
content.

---

## 5. Separation From Conversation History

```
Conversation History
  ↓
what was said

World Model
  ↓
what Parker currently believes
```

Distinct, unrelated boundaries. `ConversationHistorySource.history`
returns `Turn`s scoped to one `ConversationId`; `WorldModelSource.recall`
returns `WorldBelief`s scoped to nothing but a query's own matching
criteria — no `ConversationId` field exists anywhere on `WorldBelief`,
`WorldObservation`, or `WorldQuery`. `WorldModelSource` introduces no
dependency on `ConversationHistorySource`, `ConversationEngine`, or any
Conversation identifier, and vice versa.

---

## 6. Ordering

**Ordering belongs entirely to the World Model owner. The Assembler
preserves returned order.** Unlike Memory Source (which inherits
`InMemoryMemoryStore.retrieve`'s explicit, deterministic
most-recently-promoted-first guarantee), `InMemoryWorldModel.query`'s own
KDoc makes **no ordering guarantee at all**: "results are returned in
whatever order the underlying map iterates... a caller must not depend on
any particular ordering." `WorldModelSource` inherits this absence of a
guarantee unchanged — it does not invent one, and `DefaultReasoningContextAssembler`
must not assume, document, or test for any specific order beyond "the
order `recall` happens to return," mirroring exactly how it already makes
no ordering assumption beyond what `ConversationHistorySource.history`
and `MemorySource.recall` each, separately, guarantee.

---

## 7. Failure Behaviour

- **Empty result.** `recall` returns an empty `List<WorldBelief>` — never
  an exception — when nothing matches `WorldQuery`, the same convention
  `WorldModel.query` itself already establishes and
  `ConversationHistorySource.history`/`MemorySource.recall` already
  share. An empty result renders zero entries, exactly as "no tools," "no
  prior Turns," and "no memories" already render nothing.
- **Unavailable source.** No `try`/`catch` is added to
  `DefaultReasoningContextAssembler` for this collaborator, consistent
  with its existing, unmodified discipline
  (`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 6, "no
  `try`/`catch` anywhere in this class"). If `worldModelSource.recall`
  throws, the fault propagates unchanged to `assemble`'s own caller,
  identical treatment to a fault from `identityService.resolve`,
  `toolRegistry.listAll`, `conversationHistorySource.history`, or
  `memorySource.recall` today.
- **Partial data.** A `WorldBelief` returned by `recall` is rendered
  exactly as received — no field is treated as more or less trustworthy
  than another, and a belief with, for example, low `confidence` is
  neither excluded nor flagged specially by `WorldModelSource` or the
  Assembler (excluding a low-confidence belief, if ever wanted, is what
  `WorldQuery.minimumConfidence` already exists to express, at the
  caller's own discretion — not a behaviour `WorldModelSource` imposes
  unconditionally).
- **No fabrication.** No confidence is computed, estimated, or defaulted
  beyond what `WorldBelief.confidence` already carries (a required field,
  unlike Memory's optional one — so no "absent confidence" case exists
  for a `WorldBelief` at all); no provenance is fabricated beyond
  `WorldBelief.source`/`derivedFrom`, both already present on the type.

---

## 8. Runtime Construction

```
ParkerRuntime
  ↓
WorldModel owner (InMemoryWorldModel)
  ↓
WorldModelSource
  ↓
Assembler
```

**No production wiring exists today** (Governance Review Finding 1):
`ParkerRuntime.buildAndRegisterRuntimeGraph()` constructs no
`WorldModel`/`InMemoryWorldModel` anywhere. This Unit, like Memory Source
before it, requires a genuinely new construction step:

```kotlin
// New, inside buildAndRegisterRuntimeGraph(), alongside the other
// stateless collaborators already constructed there:
val inMemoryWorldModel = InMemoryWorldModel()
val worldModelSource: WorldModelSource = inMemoryWorldModel

reasoningContextAssembler = stage("Reasoning Context Assembler construction") {
    DefaultReasoningContextAssembler(
        identityService,
        toolRegistry,
        conversationHistorySource,
        memorySource,
        worldModelSource,
    )
}
```

`InMemoryWorldModel()` takes its default `DefaultWorldModelUpdatePolicy()`
— no new configuration, no new `ParkerRuntimeConfig` field. **No
duplicate ownership, no duplicate state:** exactly one `InMemoryWorldModel`
instance is constructed, exposed to the Assembler only through the
narrower `WorldModelSource` type, mirroring precisely how
`InMemoryMemoryStore`/`InMemoryConversationEngine` are each constructed
once and exposed through two interfaces on the same instance.

---

## 9. Test Strategy (architectural, no implementation)

- **`InMemoryWorldModel` as `WorldModelSource`.** A small, additive set of
  structural tests confirming `InMemoryWorldModel` satisfies
  `WorldModelSource` (that `recall` returns what `query` would, and that a
  `WorldModelSource`-typed reference structurally cannot reach `observe`)
  — mirroring `InMemoryMemoryStoreTest.kt`'s own equivalent tests for
  `MemorySource`. `query`'s own substantive behaviour — substring
  matching, `minimumConfidence` filtering, `maximumResults` capping,
  staleness exclusion — is **already exhaustively tested** by
  `InMemoryWorldModelTest.kt`'s 20 existing tests; a future implementation
  Unit does not re-prove those, only that `recall` inherits them by
  direct delegation.
- **`DefaultReasoningContextAssembler` tests.** A `FakeWorldModelSource`
  (lambda-based, call-count/call-argument tracking, mirroring
  `FakeMemorySource`) would exercise: empty `recall` result renders
  nothing but is still called once; a single `WorldBelief` renders one
  entry; multiple beliefs render in the order `recall` returns them,
  never reordered; a `recall` failure propagates unchanged; the
  structural constructor-dependency test updated to include
  `WorldModelSource`. Whatever `WorldQuery` the Assembler is found to
  construct (Section 3, once resolved) would additionally need its own,
  specific assertions at that time — not specified here, since the
  construction itself is not yet decided.
- **One real-collaborator integration test.** A test constructing a real
  `InMemoryWorldModel`, calling `observe` directly to seed one accepted
  belief, then calling `DefaultReasoningContextAssembler.assemble`
  directly (not through `ParkerRuntime`) to confirm the seeded belief is
  retrieved and rendered end-to-end through the real backing
  implementation — mirroring Memory Source's own identical test pattern.
- **Disclosed limitation, carried forward unchanged from Memory Source's
  own precedent:** a full `ParkerRuntime`-level (HTTP-prompt) integration
  test proving a belief renders in a real production request is not
  achievable within this Unit's own scope by the same reasoning Memory
  Source's Contract Design Section 10 already gave — `ParkerRuntime`
  constructs its `InMemoryWorldModel` privately, with no external seeding
  hook, and this Unit's own Scope Lock excludes "creating world state."

---

## 10. Disclosed Limitations

- **`WorldQuery` construction is now resolved** (Section 3) — `subjectMatch`
  is nullable, `null` means no subject filter, and the fix was implemented
  and committed (`eb25d64`) under its own separate governance track. What
  remains open is not the construction question itself but the rest of
  this Unit's own implementation (`WorldModelSource`, Assembler wiring,
  `ParkerRuntime` wiring), none of which has begun.
- **No ordering guarantee, inherited from `WorldModel.query` itself**
  (Section 6) — weaker than Memory Source's own deterministic ordering,
  disclosed rather than silently assumed away.
- **No retrieval-ranking seam.** `InMemoryWorldModel.query`'s existing
  fixed substring-match strategy is inherited unchanged; any future
  ranking capability remains a separately-scoped, separately-justified
  extension, symmetric to Memory's own deferred `MemoryRetrievalPolicy`.
- **The constructed `InMemoryWorldModel` starts, and in production would
  remain, empty** until a separate, future, out-of-scope mechanism calls
  `observe` — the identical, disclosed consequence Memory Source's own
  Contract Design Section 9 already named for `remember`.
- **No `ParkerRuntime`-level integration test for a populated belief
  rendering end-to-end** (Section 9) — a disclosed, deliberate
  test-coverage gap, carried forward from Memory Source's own identical
  disclosure.
- **No operating-Principal precondition on `WorldModelSource`/`WorldModel`**
  (Governance Review Finding 6) — an inherited Sprint-4 characteristic,
  not something this Unit introduces or corrects.
- **This document does not design, redesign, or shape the World Model
  itself** — `WorldModel`, `WorldBelief`, `WorldObservation`, `WorldQuery`,
  and `WorldModelUpdatePolicy` are unchanged, exactly as approved in
  `WORLD_MODEL_CONTRACT_DESIGN.md`.
