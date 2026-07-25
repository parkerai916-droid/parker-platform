# World Model Source Integration — Scope Lock

## Status

**Sprint 11, Unit 8. Binding, frozen Scope Lock for a future
implementation Unit.** Companion to
`docs/architecture/WORLD_MODEL_SOURCE_GOVERNANCE_REVIEW.md` (repository
review and readiness determination) and
`docs/architecture/WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md` (the interface
design and integration point this Scope Lock freezes). Included and
Excluded lists below are binding contract terms a future implementation
must satisfy exactly, not redesign.

**This document authorises no implementation.** Producing this Scope Lock
is the final deliverable of this governance/design pass — no Kotlin is
written, staged, committed, or pushed as part of this Unit's own work.

**The `WorldQuery` construction question that was open when this Scope
Lock was written is now resolved.** Exactly how
`DefaultReasoningContextAssembler` would construct the `WorldQuery` it
passes to `WorldModelSource.recall` (`WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md`
Section 3) was investigated in
`docs/architecture/WORLD_MODEL_SOURCE_QUERY_CONSTRUCTION_DECISION.md`,
found genuinely blocked under the contract as it existed then, and
resolved by a separately governed, separately approved, and now
implemented contract revision — see that document's own Outcome section
for the full record.

**The `WorldQuery` blocker has been resolved by the separately governed
contract revision.** `docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_GOVERNANCE_REVIEW.md`,
`docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md`, and
`docs/implementation/WORLD_QUERY_OPTIONAL_SUBJECT_SCOPE_LOCK.md` governed
and approved widening `WorldQuery.subjectMatch` to `String? = null`,
`null` meaning "no subject filter." That revision was implemented, passed
local Gradle verification (BUILD SUCCESSFUL), and was committed and pushed
as commit `eb25d64` ("feat: allow unfiltered world model queries").

**Unit 8 is now implemented.** Following explicit re-approval of this
Scope Lock and its three companion documents as a reconciled set,
`src/interfaces/WorldModelSource.kt` now exists, `InMemoryWorldModel` now
implements it, `DefaultReasoningContextAssembler` now takes it as a fifth
constructor dependency, and `ParkerRuntime` now constructs one
`InMemoryWorldModel` instance and wires it in -- exactly, and only, what
this Scope Lock's own Included list below authorises. See
`docs/implementation/IMPLEMENTATION_HISTORY.md`'s own "Unit 8 -- World
Model Source Integration Implementation" entry for the full record. This
Unit is implemented but **not yet accepted** pending Steven's own local
build verification, per PES-001 Stage 7.

**The Scope Lock is otherwise unchanged.** No Included item is added, no
Excluded item is removed, and no boundary below is reinterpreted by this
update — see the one clarifying sentence added to the Excluded section,
below, which discloses a fact about how the `WorldQuery` blocker came to
be resolved without altering what this Unit's own Excluded list forbids.

---

## 1. Responsibilities — What World Model Source Owns

World Model Source owns exactly one responsibility: **retrieving
already-current `WorldBelief`s matching a caller-supplied `WorldQuery`.**

That single responsibility decomposes into what it must be able to
answer, in prose, without presupposing more than the Contract Design
already fixes:

- **Matching beliefs.** Given a `WorldQuery`, World Model Source returns
  the `WorldBelief`s `WorldModel.query` already, deterministically,
  filters, scopes, and caps — subject-substring matched, confidence-floor
  narrowed if supplied, staleness-excluded, and truncated to the supplied
  `maximumResults`.
- **Nothing beyond retrieval.** It does not decide which beliefs are
  accepted, does not rank beyond `InMemoryWorldModel.query`'s existing
  fixed (and explicitly unordered) strategy, does not reconcile
  contradictory beliefs, and does not decide what a caller should ask
  for — query construction is the Assembler's own responsibility (once
  resolved, per the open question above), not World Model Source's.

World Model Source retrieves already-current beliefs. Nothing more.

---

## Included / Excluded (binding)

### Included

- A new, single-method, `suspend`-declared interface, `WorldModelSource`
  (`src/interfaces/WorldModelSource.kt`), returning `List<WorldBelief>`
  for a `WorldQuery`, delegating directly to `WorldModel.query`'s own
  existing behaviour (filtering, staleness exclusion, capping, and
  absence of any ordering guarantee — all unchanged).
- `InMemoryWorldModel` implementing `WorldModelSource` directly, as a
  second interface over its own existing owned state and existing `query`
  implementation — no new map, no new lock, no new field.
- One additional constructor dependency on
  `DefaultReasoningContextAssembler` (`worldModelSource: WorldModelSource`),
  and rendering of zero or more additional `ReasoningContext` entries, one
  per returned `WorldBelief`, in the exact order `recall` returns them.
- The one new production wiring step this requires in `ParkerRuntime`:
  constructing `InMemoryWorldModel()` (default
  `DefaultWorldModelUpdatePolicy()`) and passing it to
  `DefaultReasoningContextAssembler`'s constructor under the
  `WorldModelSource` type, in addition to its existing four collaborators.
- Focused unit and integration tests for `InMemoryWorldModel` as
  `WorldModelSource` (structural proof only — behavioural coverage of
  `query` already exists), Assembler-level tests via a
  `FakeWorldModelSource`, and one real-collaborator (real
  `InMemoryWorldModel`, seeded via `observe` directly) Assembler-level
  integration test (Contract Design Section 9).
- Documentation updates: `IMPLEMENTATION_HISTORY.md` and
  `IMPLEMENTATION_GAPS.md`, where a future implementation Unit materially
  changes the documented state (mirroring Units 6 and 7's own
  convention).
- Only behaviour already supported by `WorldModel`'s existing,
  already-approved contracts (`WORLD_MODEL_CONTRACT_DESIGN.md`) —
  staleness exclusion and absence-of-ordering semantics inherited
  unchanged, invented nowhere.

### Excluded

- **Creating world state.** No new path by which a `WorldObservation` is
  submitted is introduced. `observe` is never called by
  `DefaultReasoningContextAssembler`, `WorldModelSource`, or any part of
  a future implementation Unit's own production wiring.
- **Modifying world state.** `WorldModelSource` never calls `observe`,
  and no future revision may grant it that capability without first
  revising this Scope Lock.
- **Merging beliefs.** World Model Source does not combine, average, or
  otherwise merge two or more `WorldBelief`s into one. Whatever `recall`
  returns is rendered as separate entries, unmerged.
- **Contradiction resolution.** If multiple retrieved `WorldBelief`s
  disagree in content, World Model Source and the Assembler render all of
  them, unresolved — contradiction handling, to the extent it exists at
  all, remains `WorldModelUpdatePolicy`'s own, already-implemented,
  Observation-acceptance-time concern, never a read-time one.
- **Inference.** World Model Source does not infer, guess, or classify
  which world-model subject a request concerns. Any mapping from a
  request to a specific `WorldQuery` must be non-inventive and disclosed
  (Contract Design Section 3) — a future implementation Unit must not
  quietly invent a classification rule under the guise of a wiring
  detail.
- **Planning.** World Model Source performs no planning, never invokes
  `PlannerRuntime`, and never reasons about what a retrieved belief
  implies should happen next.
- **Prediction.** World Model Source never projects, forecasts, or
  extrapolates a future belief from current or historical ones — it
  exposes only `WorldBelief.value` as currently held, per
  `WorldModel.kt`'s own "never historical storage" guarantee.
- **Reasoning.** World Model Source performs no reasoning of its own over
  what it retrieves — reasoning over a rendered `ReasoningContext` remains
  the reasoning provider's own, sole responsibility (Constitution:
  "Cognition proposes").
- **Confidence invention.** No confidence figure is computed, estimated,
  or defaulted beyond what `WorldBelief.confidence` already carries (a
  required field on the type itself).
- **Provenance invention.** No `source` or `derivedFrom` value is
  fabricated. Only what a `WorldBelief` already carries is surfaced.
- **Semantic search.** World Model Source performs no ranking or ordering
  beyond `InMemoryWorldModel.query`'s existing, already-implemented,
  already-tested fixed strategy (case-insensitive substring match against
  `subject`, no ordering guarantee).
- **Embeddings.** No vectorisation, embedding, or index of any
  `WorldBelief` content is introduced.
- **Authentication implementation.** No authentication or trust concept
  is introduced. `AUTHENTICATION_AND_TRUST_GOVERNANCE.md` remains
  untouched and ungoverned by this document.
- **Permissions.** No `PermissionEngine`, `PermissionPolicy`, or
  authorisation decision is introduced or altered. World Model Source is
  capability, not authority.
- **Planner.** No dependency on `PlannerRuntime`, `PlanDecision`, or any
  planning concept (restated for emphasis alongside "Planning," above).
- **Tool execution.** No dependency on `ToolRegistry.resolve`, any `Tool`
  handle, `ToolInvocationBinding`, or the Execution Pipeline.
- **Memory creation.** World Model Source never calls
  `MemoryStore.remember`, and this Unit does not change how memories are
  created.
- **Conversation history changes.** This Unit does not touch, extend, or
  resolve Conversation History Source's own disclosed limitations, and
  does not add a bound to its own unbounded-history behaviour.
- **Unrelated refactoring.** No change to `WorldModel`, `WorldBelief`,
  `WorldObservation`, `WorldQuery`, `ObservationResult`, or
  `WorldModelUpdatePolicy`, or any file not directly named in the
  Included list above. **Clarification:** this exclusion applies to
  changes made under this Unit's own authority. The separately governed
  `WorldQuery` optional-subject revision (`WORLD_QUERY_OPTIONAL_SUBJECT_GOVERNANCE_REVIEW.md`,
  `WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md`,
  `WORLD_QUERY_OPTIONAL_SUBJECT_SCOPE_LOCK.md`) was completed outside this
  Scope Lock, under its own binding terms, and is now an accepted
  prerequisite this Unit inherits rather than a violation of this
  exclusion.

---

## 2. Explicit Exclusions — What It Must Never Own

Restated in the same closing form
`MEMORY_SOURCE_INTEGRATION_SCOPE_LOCK.md` Section 2 already established:
World Model Source references information. It does not own systems.

- **World state creation, acceptance, or invalidation.** Exclusively
  `WorldModel`'s own, sole, unchanged responsibility, invoked only through
  `observe` — an operation `WorldModelSource` cannot even express.
- **Memory.** A distinct, separate, already-closed architectural concept
  (Unit 7). World Model Source must never be conflated with Memory,
  substituted for it, or used to work around its own disclosed
  limitations.
- **Conversation History.** A distinct, separate, already-closed
  architectural concept (Unit 6). World Model Source must never be
  conflated with it, substituted for it, or used to work around its own
  disclosed limitations.
- **Planner.** World Model Source performs no planning, never invokes
  `PlannerRuntime`, and never reasons about what a retrieved belief
  implies should happen next.
- **Tool execution.** No dependency on `ToolRegistry.resolve`, any `Tool`
  handle, `ToolInvocationBinding`, or the Execution Pipeline exists or is
  ever introduced.
- **Ranking or contradiction policy.** World Model Source does not
  implement a retrieval-ranking seam or any substitute for
  `WorldModelUpdatePolicy`'s own contradiction handling. Any such
  capability, if ever needed, belongs to a future, separately-scoped and
  separately-justified component — not folded into this boundary "since
  it's already there."

---

## 3. Governing Principle

**World Model Source is a read boundary, not a world-model owner. It
exposes current belief. It never creates, modifies, invalidates, or
reconciles one.**

Every item World Model Source can ever return has exactly one other home:
`WorldModel`'s own owned belief state. World Model Source originates
nothing and never becomes canonical. If a World Model Source read and
`WorldModel`'s own current state ever disagree, `WorldModel`'s state is
correct and the read is simply stale — the identical projection
discipline already established for `ReasoningContext` itself, for
Conversation History Source, and for Memory Source.

---

## 4. Ownership

- **Exactly one production owner.** `parker.composition.ParkerRuntime`
  constructs World Model Source (via `InMemoryWorldModel`), exactly once,
  at startup — mirroring `MEMORY_SOURCE_INTEGRATION_SCOPE_LOCK.md`
  Section 4's identical ownership shape.
- **Exactly one production caller.** `DefaultReasoningContextAssembler` —
  calling `worldModelSource.recall(query)` directly inside `assemble`,
  mirroring how it already reads `identityService`, `toolRegistry`,
  `conversationHistorySource`, and `memorySource` directly. No
  coordinator, no Reasoning Provider, and no other runtime component may
  become a second caller without a future Scope Lock revision.

---

## 5. Lifetime

- **Construction.** Once, at startup, alongside every other stateless
  collaborator `ParkerRuntime` builds. Construction failure is reported
  the same way every other collaborator's construction failure already is
  (`ParkerRuntime`'s own existing `stage()` pattern).
- **Use.** Read-only, per request. Each read is independent; nothing
  observed or returned by one read is retained for, or influences, the
  next.
- **Disposal.** A returned `List<WorldBelief>` is discarded once its
  caller is finished with it. World Model Source itself is never torn
  down and reconstructed per request.

---

## 6. Threading

- **Sharing.** A single World Model Source instance is shared across
  every concurrent request the runtime handles, exactly as every other
  stateless, constructed-once collaborator in this runtime already is —
  `InMemoryWorldModel`'s own existing `Mutex` already guards concurrent
  `query` calls, unchanged by this Unit.
- **Immutability.** Whatever World Model Source returns is immutable from
  the moment it is returned. A given returned list is never shared across
  more than one caller's own handling of one request.
- **Coroutine expectations.** `recall` is `suspend`-declared, consistent
  with every other read-only dependency this runtime already relies on.

---

## 7. Architectural Principles

Carried forward unchanged from Units 6 and 7, all preserved by this
Scope Lock:

- **One owner.** `WorldModel`/`InMemoryWorldModel` remains the sole
  owner of world-model state.
- **One responsibility.** World Model Source retrieves already-current
  belief. Nothing more.
- **One authoritative state.** No second store, no cache capable of
  disagreeing with `WorldModel`'s own owned state.
- **Passive read interface.** `recall` is a pure read; no mutation is
  reachable through `WorldModelSource`.
- **Narrow dependency.** `DefaultReasoningContextAssembler` receives
  `WorldModelSource`, never `WorldModel` — structurally incapable of
  calling `observe`.
- **Additive architecture.** `WorldModel`'s existing three-operation
  contract is unaltered; `WorldModelSource` is added alongside it, the
  same pattern already used twice this Sprint.
- **Implementation independence.** `WorldModelSource` is declared as a
  plain `fun interface`, independent of `InMemoryWorldModel` — a future,
  different `WorldModel` implementation could satisfy `WorldModelSource`
  without changing the Assembler's own dependency type.

---

## 8. Constitutional Boundary

`WorldModelSource` exposes current world-model state. It does not
determine what becomes world state. It does not create world state. It
does not modify world state. It does not reconcile world state. It does
not forget world state. Those responsibilities remain with the World
Model owner.

`docs/architecture/parker-constitution.md`: "Parker owns authority.
Modules provide capability" and "Cognition proposes. Trust authorises.
Runtime executes." World Model Source is capability, not authority — it
proposes nothing, authorises nothing, and executes nothing. It is a read
boundary `DefaultReasoningContextAssembler` draws on, exactly as
`IdentityService`, `ToolRegistry`, `ConversationHistorySource`, and
`MemorySource` already are — never itself a source of proposal,
authority, or execution.

---

## 9. Acceptance of This Scope Lock

This Scope Lock is binding once accepted. A future implementation Unit
authorised against it must satisfy the Included list exactly, must not
implement anything in the Excluded list, must resolve the one explicitly
open question (`WorldQuery` construction, Section "Status," above)
through a disclosed, non-inventive Implementation Decision before writing
Kotlin, and must treat any other discovered need to exceed either list as
grounds to pause and request a Scope Lock revision — not as licence to
proceed under the existing one.
