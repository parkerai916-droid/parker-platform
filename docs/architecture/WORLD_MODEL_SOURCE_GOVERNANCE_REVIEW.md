# World Model Source Integration — Governance Review

## Status

**Implemented.** Following this review's own Readiness Determination, the
`WorldQuery` blocker's separate resolution (commit `eb25d64`), and this
document's own explicit re-approval alongside its three companions as a
reconciled set, Sprint 11 Unit 8 has been implemented. See
`docs/implementation/IMPLEMENTATION_HISTORY.md`'s own "Unit 8 -- World
Model Source Integration Implementation" entry for the full record,
including this session's own honest, sandbox-limited verification
account. This Unit is implemented but **not yet accepted** pending
Steven's own local build verification, per PES-001 Stage 7.

**Sprint 11, Unit 8. PES-001 pre-Contract-Design governance review.**
Companion to `docs/architecture/WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md`
(the interface design this review clears the way for) and
`docs/implementation/WORLD_MODEL_SOURCE_INTEGRATION_SCOPE_LOCK.md` (the
binding Included/Excluded terms). This document is governance only — no
Kotlin is implemented, proposed as a diff, or changed by it. Neither
`src/` nor `tests/` is touched.

**This is not a request to design or implement a world model.** The World
Model already exists, fully specified and implemented (Sprint 4, Track
B). This Unit's objective is narrower: define the narrow read interface
through which `DefaultReasoningContextAssembler` can obtain world-model
information, closing the third and final deferred dependency boundary
`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 4.2 named
(Memory Source and Conversation History Source already closed, Units 7
and 6 respectively).

---

## 1. Repository Review (fresh reads, this Unit)

Read directly, not from memory, immediately before this document was
written:

- `docs/architecture/parker-constitution.md`.
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001).
- `docs/architecture/reasoning-context.md` — the Memory / World Model /
  Reasoning Context three-layer split this Unit must not blur.
- `docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md`.
- `docs/architecture/IMPLEMENTATION_GAPS.md` (Gap #53 and its most recent
  Sprint 11 Unit 7 update).
- `docs/implementation/IMPLEMENTATION_HISTORY.md` (Sprint 11 Units 6 and
  7 entries).
- `docs/architecture/CONVERSATION_HISTORY_SOURCE_CONTRACT_DESIGN.md` and
  `docs/implementation/CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md`.
- `docs/architecture/MEMORY_SOURCE_GOVERNANCE_REVIEW.md`,
  `docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md`, and
  `docs/implementation/MEMORY_SOURCE_INTEGRATION_SCOPE_LOCK.md` — this
  Unit's own direct precedent and required point of symmetry comparison.
- `docs/architecture/ARCHITECTURE_DECISIONS.md` AD-011 (Context Is
  Reference-Based) and AD-012 (Memory and World Model Are Context
  Providers).
- `docs/architecture/WORLD_MODEL_RUNTIME_ARCHITECTURE.md` and
  `docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md` — the World Model's
  existing, already-accepted architecture and field-level contracts.
- `src/interfaces/WorldModel.kt`, `src/runtime/InMemoryWorldModel.kt`,
  `tests/runtime/InMemoryWorldModelTest.kt`.
- `src/runtime/DefaultReasoningContextAssembler.kt`,
  `src/composition/ParkerRuntime.kt`.

### Central findings

**Finding 1 — the World Model has no production wiring today, the same
situation Memory Source faced.** A direct grep of
`src/composition/ParkerRuntime.kt` for `WorldModel` returns no matches.
`InMemoryWorldModel` is constructed nowhere in the running system — it
exists only as a Sprint 4 implementation exercised by its own isolated
test suite (`tests/runtime/InMemoryWorldModelTest.kt`, 20 tests). World
Model Source Integration therefore requires a **new** production
construction step in `ParkerRuntime.buildAndRegisterRuntimeGraph()`, not
merely a reordering — the identical situation Memory Source Integration
(Unit 7) resolved, and the identical departure from Conversation History
Source's own reordering-only wiring (Unit 6).

**Finding 2 — `WorldModel` exposes two read operations, not one, and
neither takes an already-resolved key the way `ConversationHistorySource`
does.** `current(subject: String): WorldBelief?` looks up one exact,
caller-supplied subject; `query(query: WorldQuery): List<WorldBelief>`
performs a substring match of `WorldQuery.subjectMatch` against
`WorldBelief.subject`, narrowed by an optional `minimumConfidence` and
capped by a mandatory `maximumResults`. `query`'s own capability is a
strict superset of `current`'s (a caller wanting one exact subject's
belief can supply that subject as `subjectMatch` with `maximumResults = 1`
and get the identical answer) — this bears directly on the Contract
Design's own interface-minimality requirement.

**Finding 3 — `WorldQuery.subjectMatch` matches against a belief's
*topic key*, not its content, and this is a materially different problem
from Memory Source's `relevance` field.** `MemoryQuery.relevance` matches
against `MemoryRecord.knowledgePayload` — free-text knowledge content a
natural-language request plausibly overlaps with, which is why Memory
Source's Contract Design could defensibly supply the request's own text
as `relevance` without inventing new semantics. `WorldQuery.subjectMatch`
matches against `WorldBelief.subject` — a structured topic identifier
(for example, a device state key or a location key), not natural-language
content. Supplying an inbound request's free text as `subjectMatch` has
no principled justification: it does not reuse an existing, defensible
correspondence the way Memory's did, and constructing a mapping from
request text to a specific world-model subject would itself be a
classification or inference step — squarely excluded by this Unit's own
Scope Lock ("inference"). **This is a genuine, disclosed open question
this Contract Design does not resolve** (see Contract Design Section 3).

**Finding 4 — `WorldModel.query` makes no ordering guarantee at all,
unlike `MemoryStore.retrieve`.** `InMemoryWorldModel.query`'s own KDoc
states plainly: "No ranking or scoring formula is applied — results are
returned in whatever order the underlying map iterates... a caller must
not depend on any particular ordering." This is weaker than Memory's own
explicit, deterministic "most-recently-promoted-first" contract. World
Model Source must inherit this absence of an ordering guarantee, not
invent one.

**Finding 5 — staleness is a first-class, lazy concept already handled
entirely inside the World Model, never by a caller.**
`WorldModelUpdatePolicy.isStillCurrent` is consulted internally by both
`current` and `query`; a stale `WorldBelief` is silently excluded from
both — never returned, never flagged specially. World Model Source
inherits this "stale is treated as absent" convention unchanged; it does
not compute, override, or second-guess staleness itself.

**Finding 6 — `WorldModel` has no operating-Principal precondition,
mirroring `MemoryStore`'s own identical characteristic (Memory Source
Governance Review Finding 5).** `InMemoryWorldModel`'s constructor takes
only a `WorldModelUpdatePolicy` (defaulted) — no `IdentityService`
dependency, no `requireOperatingPrincipalRegistered()`-equivalent check.
An inherited Sprint-4 characteristic, not something this Unit introduces
or corrects.

**Finding 7 — `reasoning-context.md` and AD-012 already name this exact
consumer role.** `reasoning-context.md`: "The World Model is responsible
for... exposing the current-state slice relevant to a given task." AD-012:
"Memory and the World Model, once specified, are read sources that
inform planning and context." World Model Source Integration is the
literal realisation of both already-accepted statements, not a new
architectural invention.

---

## 2. Existing Ownership

`WorldModel` (backed by `InMemoryWorldModel`) is the sole, authoritative
owner of world-model state, per `WORLD_MODEL_RUNTIME_ARCHITECTURE.md`
Section 6's ownership table: seven named sources (Sensors, Plugins,
Agents, Planner, Runtime, Memory, User) may *propose* an Observation, but
"no subsystem outside the World Model owns acceptance of an Observation
into current belief" — acceptance is exercised exclusively through
`WorldModelUpdatePolicy`, internal to `InMemoryWorldModel`. World Model
Source does not change this. It is a second, narrower, read-only view
over the same instance and the same owned state, never a second owner,
never a second source of truth — the identical non-negotiable structure
already established for Conversation History Source and Memory Source.

---

## 3. Constitutional Responsibilities

Per `reasoning-context.md`'s own three-layer definition, restated here
without hedging because this Unit's entire justification depends on it:

**World Model answers: "What does Parker currently believe to be true?"**

It does not answer:

- **what happened in the conversation** — that is Conversation History's
  own, already-closed boundary (Unit 6): a record of prior Turns in one
  Conversation, not a belief about current reality.
- **what Parker remembers** — that is Memory's own, already-closed
  boundary (Unit 7): durable, deliberately-promoted knowledge that
  persists across sessions, not live current state.
- **what should Parker do** — that is the Planner's and the reasoning
  provider's own domain (Constitution: "Cognition proposes"). The World
  Model supplies belief; it never proposes an action, and World Model
  Source inherits that boundary unchanged.

Per `WORLD_MODEL_RUNTIME_ARCHITECTURE.md`'s own Architectural Principles
(mirrored from Memory's identical framing): the World Model never
executes, never plans, never promotes Memory, never reacts autonomously
beyond updating its own belief, and never rewrites Memory. World Model
Source, as a narrower read-only view, inherits every one of these
constraints by construction — it cannot reach `observe` at all (Contract
Design Section 2), so it cannot violate any of them even in principle.

---

## 4. Architectural Boundaries

- **World Model Source is a read boundary, not a world-model owner.** It
  exposes current belief. It never creates, updates, invalidates, or
  reconciles one.
- **No autonomous reaction.** `WORLD_MODEL_RUNTIME_ARCHITECTURE.md`:
  "The World Model never reacts autonomously to events beyond its own
  [Update]." World Model Source performs no subscription, no event
  handling, no background sweep — a passive, per-request read only,
  identical in kind to Memory Source and Conversation History Source.
- **No history.** `WorldBelief` itself carries no history and no
  prior-belief reference (`WorldModel.kt`'s own KDoc: "the World Model is
  never historical storage"). World Model Source cannot expose a history
  its own backing type does not carry.

---

## 5. Dependency Direction

Unchanged in kind from Memory Source and Conversation History Source:
`ParkerRuntime` constructs the concrete `InMemoryWorldModel` and injects
it into `DefaultReasoningContextAssembler`'s constructor under a new,
narrower `WorldModelSource` type. The Assembler never constructs, looks
up, or reaches for its own collaborator; `WorldModelSource` never reaches
back toward the Assembler, the Planner, or any reasoning provider.

---

## 6. Interaction With Reasoning Context

`DefaultReasoningContextAssembler` gains a fifth constructor dependency
(`worldModelSource: WorldModelSource`), following the exact structural
pattern already established for `conversationHistorySource` (Unit 6) and
`memorySource` (Unit 7): a read-only collaborator, consulted once per
`assemble` call, rendering zero or more additional `ReasoningContext`
entries, in the order returned, with no ranking, scoring, or
interpretation performed by the Assembler.

---

## 7. Interaction With Memory

Distinct, parallel knowledge layers, per `reasoning-context.md` and
`WORLD_MODEL_RUNTIME_ARCHITECTURE.md` Section 6's own ownership table row
for Memory: "Memory is a separate, parallel knowledge layer, not a source
of live signal about current reality; nothing in this architecture has
Memory reporting an Observation to the World Model." World Model Source
never reads Memory, never reads `MemorySource`, and never allows a
`WorldBelief` to be confused for, merged with, or promoted into a
`MemoryRecord`. The two Sources remain entirely independent collaborators
of the Assembler — the Assembler alone combines what each returns into
one `ReasoningContext`, and it does so only by concatenating rendered
entries, never by cross-referencing or reconciling the two layers'
content.

---

## 8. Interaction With Conversation History

Distinct, unrelated boundaries. Conversation History answers "what was
said, and in what order, in this Conversation"; World Model answers "what
does Parker currently believe is true, independent of any Conversation."
A `WorldBelief`'s `subject` carries no `ConversationId`, and
`ConversationHistorySource` carries no belief about current reality.
World Model Source introduces no dependency on, or interaction with,
`ConversationHistorySource`, `ConversationEngine`, or any Conversation
identifier.

---

## 9. Interaction With Authentication & Trust

Checked directly: `AUTHENTICATION_AND_TRUST_GOVERNANCE.md` contains no
reference to the World Model, `WorldModel`, or any reasoning-pipeline read
boundary anywhere in its sections. No conflict. World Model Source
introduces no authentication, authorisation, or trust concept of its
own — it is capability, read only, exactly as Memory Source and
Conversation History Source already are. `WorldModel`'s own absence of an
operating-Principal precondition (Finding 6, above) is inherited
unchanged, not introduced by this Unit, and is not an Authentication &
Trust concern this Unit is positioned to resolve.

---

## 10. Conflicts, If Any

**None found.** Checked directly against every governing document listed
in Section 1:

- `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 4.2 named
  World Model Source as a future, undesigned boundary — this Unit is that
  named future Contract Design revision, not a contradiction of it.
- `AD-012` requires the World Model to remain a read source, never an
  orchestration system — World Model Source, a passive read-only
  interface, is consistent with this by construction.
- Conversation History Source's and Memory Source's own Contract Designs
  and Scope Locks are entirely unaltered by this Unit; World Model Source
  is a sibling boundary, not a revision of either.
- `WorldModel`'s own three-operation contract (`observe`, `current`,
  `query`) is unaltered — World Model Source adds a fourth, narrower,
  read-only interface alongside it, the same additive pattern already
  used twice this Sprint.

**One genuine, disclosed open question, not a conflict:** how the
Assembler should construct a `WorldQuery` (specifically, what value to
supply for the mandatory `subjectMatch` field) is not resolved by this
review — see Finding 3, above, and Contract Design Section 3. This is a
real design gap, deliberately not answered by inventing a
request-text-to-subject mapping, which would itself be the kind of
inference this Unit's own Scope Lock excludes.

---

## 11. Readiness Determination

- **Governance:** clear. No document conflicts with introducing World
  Model Source; `reasoning-context.md`, AD-012, and
  `WORLD_MODEL_RUNTIME_ARCHITECTURE.md` all already anticipate this exact
  read path.
- **Contract Design:** produced alongside this document
  (`WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md`).
- **Scope Lock:** produced alongside this document
  (`WORLD_MODEL_SOURCE_INTEGRATION_SCOPE_LOCK.md`).
- **Dependencies:** `WorldModel`/`InMemoryWorldModel` already exist,
  already tested (20 tests covering acceptance, contradiction handling,
  retraction, timestamp authority, derived beliefs, `query` filtering/
  capping, staleness exclusion, and the "no `WorldModelRuntime`" and "no
  external dependency" structural boundaries). No unimplemented dependency
  blocks this Unit.
- **Blockers:** none that prevent producing a Contract Design and Scope
  Lock. One open design question (query construction, Finding 3) is
  real and is carried forward, disclosed, into both documents rather than
  resolved by invention.

Implementation may proceed once the Contract Design and Scope Lock below
are both accepted, and once the open query-construction question is
either resolved by a future Implementation Decision or the implementation
Unit itself proposes and justifies a specific, non-inventive resolution
for review — not before, and not as part of this governance pass.
