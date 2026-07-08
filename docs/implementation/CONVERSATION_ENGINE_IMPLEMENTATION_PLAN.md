# Conversation Engine Implementation Plan (Inbound Half + Reasoning Provider Invocation)

## Status

**Stage 3 Implementation Plan, PES-001. Not yet Scope Locked.** No
Kotlin is written by this document. Restating PES-001's own Stage 3
definition directly: "Break architectural work into independently
verifiable implementation units. Each unit defines: Included work,
Excluded work, Dependencies, Acceptance Criteria, Unit Stop Conditions."
This document performs exactly that breakdown for one unit — no more.

**Grounded exclusively in the six sources named for this task**, each
already reviewed and, in the case of the two Contract Design documents,
already corrected per their own Stage 2A reviews earlier this Sprint:

1. `docs/architecture/19-conversation-engine.md` (Stage 1, accepted).
2. `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md` (Stage 2A).
3. `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md` (Stage 1,
   accepted).
4. `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` (Stage 2A,
   corrected).
5. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` — the further
   upstream contract set reused transitively (`InboundOwnerMessage`,
   `CorrelationId`, `PrincipalId`).
6. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage
   3's own definition, above.

**Why this unit spans two Contract Designs, not one.** `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`
Section 1 explicitly anticipated this moment: "If a future Contract
Design pass (once the reasoning-provider contract exists) finds a
second operation is genuinely needed, it is added additively... not
speculatively reserved here." That contract now exists
(`REASONING_PROVIDER_CONTRACT_DESIGN.md`). This unit combines both
already-approved surfaces into one implementation pass, stopping at the
exact boundary this task specifies: obtaining a `ReasoningProviderResponse`,
and no further. Combining them does not require a third Contract Design
pass, because — as reasoned in Section 5 below — the composition itself
introduces no new public contract type; it is ordinary wiring between
two interfaces that are each already fully shaped.

---

## 1. Objective

Implement, and make independently testable:

1. **`ConversationEngine`'s inbound half**, exactly as
   `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` already defines it — one
   operation, binding an already-`CommunicationIntake`-accepted
   `InboundOwnerMessage` into a `Conversation`/`Turn`, returning a
   `ConversationDisposition`. No change to this contract's shape.
2. **The `ReasoningProvider` contract's Kotlin shape**, exactly as
   `REASONING_PROVIDER_CONTRACT_DESIGN.md` already defines it — the
   `ReasoningProvider` interface, `ReasoningProviderRequest`,
   `ReasoningProviderResponse` (`Goal`/`Reply`/`NoAction`), and
   `ReasoningContext`. No change to this contract's shape. No concrete,
   real (model-backed or otherwise production-intended)
   `ReasoningProvider` implementation — that remains explicitly deferred
   by that document's own Section 9.
3. **A minimal composition step** that, given an accepted
   `InboundOwnerMessage` and an already-assembled `ReasoningContext`,
   calls `ConversationEngine.submitTurn`, builds a
   `ReasoningProviderRequest` from the resulting `Turn`, calls
   `ReasoningProvider.reason`, and returns the resulting
   `ReasoningProviderResponse` unchanged to its own caller. **This is
   the unit's stop condition, restated from the task's own instruction:
   the unit's own code path ends here. Nothing routes a `Goal` onward to
   Planner Runtime, nothing routes a `Reply` onward to Response
   Delivery, and nothing else happens.**

## 2. Included Work

- `ConversationId`, `TurnId`, `Conversation`, `Turn`,
  `ConversationDisposition`, `ConversationEngine` (interface) — field-
  shaped exactly per `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2.
- `InMemoryConversationEngine` — the concrete implementation of
  `ConversationEngine.submitTurn`, resolving its own operating Principal
  identity through `IdentityService` before acting (Contract Design
  Section 7; Architecture Section 5), mirroring
  `PLANNER_RUNTIME_PRINCIPAL_ID`/`TASK_MANAGER_RUNTIME_PRINCIPAL_ID`'s
  established precedent (`InMemoryPlannerRuntime.kt`,
  `InMemoryTaskManagerRuntime.kt`) — see Section 5, Required
  Implementation Decision 3.
- `ReasoningContext`, `ReasoningProviderRequest`,
  `ReasoningProviderResponse` (`Goal`, `Reply`, `NoAction`),
  `ReasoningProvider` (interface) — field-shaped exactly per
  `REASONING_PROVIDER_CONTRACT_DESIGN.md` Sections 2 and 3.
- One minimal composition unit (exact shape: Required Implementation
  Decision 2, Section 5) that sequences `ConversationEngine.submitTurn`
  and `ReasoningProvider.reason`, stopping at the resulting
  `ReasoningProviderResponse`.
- A test-only, deterministic, configurable fake `ReasoningProvider`
  implementation (`tests/`, not `src/`) — mirroring
  `FakePermissionEngine`/`FakeCommunicationIntake`'s established
  precedent — used solely to exercise the composition unit's own logic
  in isolation. This is explicitly **not** a production or
  model-backed implementation; see Section 8.
- Unit tests for every type above, per Section 6.

## 3. Files Expected to Change

All additions. **No existing `src/` file requires modification** —
`CommunicationIntake`, `ModuleRegistry`, `IdentityService`, and every
other already-implemented interface are consumed exactly as they exist
today.

| File | New/Modified | Contents |
| --- | --- | --- |
| `src/interfaces/ConversationEngine.kt` | New | `ConversationId`, `TurnId`, `Conversation`, `Turn`, `ConversationDisposition`, `ConversationEngine` — per `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2. |
| `src/runtime/InMemoryConversationEngine.kt` | New | Concrete `ConversationEngine` implementation. |
| `src/interfaces/ReasoningProvider.kt` | New | `ReasoningContext`, `ReasoningProviderRequest`, `ReasoningProviderResponse` (sealed), `ReasoningProvider` — per `REASONING_PROVIDER_CONTRACT_DESIGN.md` Sections 2 and 3. |
| `src/runtime/ConversationTurnReasoningCoordinator.kt` | New (proposed name — Required Implementation Decision 2) | The minimal composition unit described in Section 1, Item 3. |
| `tests/runtime/InMemoryConversationEngineTest.kt` | New | Tests for `submitTurn` in isolation. |
| `tests/runtime/FakeReasoningProvider.kt` | New | Configurable, deterministic test double. |
| `tests/runtime/ConversationTurnReasoningCoordinatorTest.kt` | New | Tests for the composition unit, using the fake above. |
| `tests/contracts/ReasoningProviderContractTest.kt` | New | Construction/validation tests for `ReasoningContext`, `ReasoningProviderRequest`, `ReasoningProviderResponse`'s three variants (blank-rejecting `init` checks, mirroring this codebase's existing identifier/value-type convention). |

## 4. Dependencies

**`InMemoryConversationEngine`'s only dependency: `IdentityService`.**
Restating `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 7 exactly —
used once, to resolve the Conversation Engine's own operating Principal
identity. No dependency on `ModuleRegistry` (no re-check of
`CommunicationIntake`'s own structural checks — Contract Design Section
1's own precondition), `PlannerRuntime`, `ExecutionPipeline`,
`PermissionEngine`, `MemoryStore`, or `WorldModel`.

**The composition unit's only dependencies: `ConversationEngine` and
`ReasoningProvider`, both as interfaces, both constructor-injected.**
No dependency on anything either of those two interfaces themselves
don't already depend on. In particular — and this is the unit's primary
structural enforcement mechanism, not merely an assertion (Section 7) —
**the composition unit's constructor accepts no reference to
`PlannerRuntime`, `ExecutionPipeline`, `PermissionEngine`, `ToolRegistry`,
`MemoryStore`, or `WorldModel`.** There is nothing to call because there
is nothing to call it *with*.

**`ReasoningProvider` (the interface) has zero dependencies of its own**,
restating `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 7 — the fake
test implementation built for this unit (Section 2) likewise depends on
nothing beyond the request/response shapes it's asked to honor.

## 5. Required Implementation Decisions

Mirroring this program's own established practice (the `IDR-001`
precedent; the `task.completed`/`AgentRunId`-sourcing decision recorded
for `IMPLEMENTATION_GAPS.md` #43's first closure unit): three genuine
interpretive forks exist that neither Contract Design resolved, each
named here with a proposed, conservative default, awaiting confirmation
before Kotlin proceeds.

### Decision 1 — Conversation continuation recognition rule

`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 3's own Lifecycle
section left "the exact rule for recognising that a new inbound message
continues an existing Conversation" undecided (`19-conversation-engine.md`
Architecture Section 13 Item 3, restated in Contract Design). No
algorithm can be silently invented here without exceeding this Plan's
own authority to decide behaviour Contract Design deliberately deferred.

**Proposed default: every Turn begins a new Conversation.** Concretely,
`InMemoryConversationEngine.submitTurn` always takes the "Conversation
creation" branch (`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 3
Item 1) — `ConversationDisposition.isNewConversation` is always `true`,
and every `Conversation.turnIds` therefore always contains exactly one
element for this unit. This is the same conservative, "build only what's
already decided" discipline already applied to Communication Runtime
Unit C1 (structural acceptance only) and the Local Text Channel
(inbound only): it makes no claim about a recognition algorithm this
unit has no authority to invent, and it is honestly, observably testable
(Section 6). A future, separately-scoped unit — once Architecture Section
13 Item 3 is actually resolved — replaces this default; nothing about
this unit's contract shapes needs to change when that happens, since
`isNewConversation` and `Conversation.turnIds` already exist to carry a
richer answer.

### Decision 2 — Shape of the composition unit

Whether the code that sequences `ConversationEngine.submitTurn` and
`ReasoningProvider.reason` is a small standalone class (proposed:
`ConversationTurnReasoningCoordinator`, Section 3), a top-level
function, or an additional method on `InMemoryConversationEngine` itself
that is *not* part of the `ConversationEngine` interface, is not decided
by either Contract Design — because it introduces no new public
contract type, this choice does not require a Contract Design pass at
all (Status, above), but it is still a real implementation choice this
Plan should name rather than leave to be improvised during Stage 6.

**Proposed default: a small, standalone class**, constructor-injected
with a `ConversationEngine` and a `ReasoningProvider`, exposing one
method that performs the sequence described in Section 1 Item 3. This
is preferred over adding a non-interface method to
`InMemoryConversationEngine` because it keeps `InMemoryConversationEngine`
exactly matching its own contracted, single-purpose shape (Contract
Design Section 1), and keeps the composition's own dependency list
(Section 4) visibly minimal and separately testable.

**This decision is made explicit, not left implicit: the coordinator is
intentionally not interface-backed in this first unit, because it is
implementation wiring, not a new public contract surface.** Every other
component in this codebase (`ConversationEngine`, `ReasoningProvider`,
`CommunicationIntake`, `PlannerRuntime`, `TaskManagerRuntime`,
`AgentRuntime`, `IdentityService`, `ModuleRegistry`) is interface-backed
because each represents an approved architectural boundary with its own
Contract Design. The coordinator represents neither — it sequences two
already-shaped interfaces and introduces no new field, no new domain
concept, and no new state of its own (Status, above). Giving it an
interface now would imply it is an architectural boundary of its own
standing, which this Plan does not claim and PES-001 Stage 2A has not
been asked to authorise.

**If a future caller needs to depend on the coordinated behaviour
abstractly** — for example, to substitute a test double for "submit a
Turn and obtain reasoning output" as a single unit, or to allow more than
one concrete coordination strategy — **that need must be met by a later
Contract Design pass, or by an explicit, disclosed additive-interface
decision at that time, not by silently promoting this unit's own
concrete class into a public contract.** This unit does not anticipate
that need speculatively; it names the boundary so a future unit does not
have to rediscover it.

### Decision 3 — Conversation Engine's operating Principal identity value

`19-conversation-engine.md` Section 13 Item 10 names the exact value as
"Stage 3/6 territory." Mirroring `InMemoryPlannerRuntime.PLANNER_RUNTIME_PRINCIPAL_ID`
(`PrincipalId("system.planner-runtime")`) and
`InMemoryTaskManagerRuntime.TASK_MANAGER_RUNTIME_PRINCIPAL_ID`'s
identical, already-implemented precedent:

**Proposed default:** `CONVERSATION_ENGINE_PRINCIPAL_ID = PrincipalId("system.conversation-engine")`,
resolved via `IdentityService.resolve` once, at construction or first
use, failing fast (not silently proceeding) if unresolvable — the same
"resolve before acting" discipline `InMemoryPlannerRuntime`/
`InMemoryAgentRuntime`/`InMemoryTaskManagerRuntime` already establish.

**Explicit requirement, not left to be assumed at Stage 6:**
`system.conversation-engine` must be registered with `IdentityService`
in every test fixture that constructs `InMemoryConversationEngine`
before that fixture is used, and that registration's resolvability must
itself be verified — mirroring whatever
`InMemoryPlannerRuntimeTest`/`InMemoryTaskManagerRuntimeTest` already do
for their own system Principals. Without this, `InMemoryConversationEngine`'s
own "fails fast if unresolvable" behaviour (above) would surface in
tests as a false negative — a missing test-fixture registration
mistaken for a real defect — rather than exercising the fail-fast path
deliberately. See Section 6 and Section 7 for the specific tests and
acceptance criteria this requirement produces.

**A related, narrower question this unit also resolves conservatively:**
`REASONING_PROVIDER_ARCHITECTURE.md` Section 14 Open Question 5 (whether
a Reasoning Provider invocation needs a Principal identity distinct from
its caller's) remains genuinely open at the architecture level — this
unit does not resolve it, because `ReasoningProviderRequest` has no
field for one at all (`REASONING_PROVIDER_CONTRACT_DESIGN.md`'s own
Minimalism Review excludes a caller-principal field). The composition
unit (Decision 2) reuses the Conversation Engine's own already-resolved
identity for its own internal accountability/logging purposes only —
never passed into `ReasoningProviderRequest`, never a claim that this
resolves Open Question 5.

## 6. Testing Strategy

**`InMemoryConversationEngineTest.kt`:**
- `submitTurn` given an `InboundOwnerMessage` returns a
  `ConversationDisposition` with `isNewConversation == true`, a freshly
  generated `ConversationId`, and a `Turn` wrapping the same message
  unchanged.
- Two separate `submitTurn` calls (two distinct messages) produce two
  distinct `ConversationId`s and two distinct `TurnId`s — verifying
  Decision 1's default is actually followed, not accidentally reusing
  state.
- Each resulting `Conversation.turnIds` contains exactly the one
  `TurnId` produced by that call.
- **Registered principal resolves successfully.** With
  `system.conversation-engine` registered in the test's `IdentityService`
  fixture beforehand, the Conversation Engine resolves
  `CONVERSATION_ENGINE_PRINCIPAL_ID` before returning a disposition —
  restating Decision 3's explicit registration requirement (Section 5).
- **Missing/unregistered principal fails fast.** With
  `system.conversation-engine` deliberately *not* registered, constructing
  or using `InMemoryConversationEngine` fails fast — mirroring
  `InMemoryPlannerRuntime`'s own equivalent test coverage — rather than
  silently proceeding or substituting a default identity.
- **The message sender Principal is never substituted for the
  Conversation Engine's own operating Principal.** Given an
  `InboundOwnerMessage` whose `senderPrincipalId` is a distinct,
  separately-registered owner Principal, `submitTurn` resolves and acts
  under `CONVERSATION_ENGINE_PRINCIPAL_ID` for its own operating
  identity while leaving `Turn.message.senderPrincipalId` unchanged and
  distinct — the two identities are never conflated, restating
  `19-conversation-engine.md` Section 5's "never treats the message's
  sender as its own identity" rule at the test level.
- `submitTurn` never calls anything beyond `IdentityService` — enforced
  structurally by the fixture's own dependency list (Section 4), not
  merely asserted.

**`ReasoningProviderContractTest.kt`:**
- `ReasoningContext`, `ReasoningProviderRequest`, `Goal`, and `Reply`
  each reject blank/empty required string content, mirroring this
  codebase's established blank-rejecting `init` convention
  (`CorrelationId`, `PrincipalId`, `InboundOwnerMessage.text`, etc.).
- `NoAction` carries no fields and is a single, comparable value.
- Exactly one of the three `ReasoningProviderResponse` variants can be
  held by a given reference at once (a compile-time property of the
  sealed type, exercised by a representative `when` test over all three
  branches).

**`FakeReasoningProvider.kt`:** a small, deterministic, configurable
test double — constructed with a pre-set `ReasoningProviderResponse` (or
a function from `ReasoningProviderRequest` to one) to return on `reason`,
mirroring `FakePermissionEngine`'s established configurable-response
precedent. Records the exact `ReasoningProviderRequest` it received for
later assertion. Not a production implementation; lives in `tests/`
only (Section 8).

**`ConversationTurnReasoningCoordinatorTest.kt`:**
- Given an `InboundOwnerMessage`, a `ReasoningContext`, and a
  `FakeReasoningProvider` configured to return `Goal("do the thing")`:
  the coordinator calls `ConversationEngine.submitTurn` first, then
  calls `ReasoningProvider.reason` with a `ReasoningProviderRequest`
  whose `turn` is exactly the `Turn` `submitTurn` returned and whose
  `reasoningContext` is exactly the one supplied, then returns
  `Goal("do the thing")` unchanged to its own caller.
- Same test repeated for a `Reply` response and for a `NoAction`
  response — each passes through unchanged.
- **Negative/structural test:** the coordinator's own constructor
  accepts only a `ConversationEngine` and a `ReasoningProvider` — there
  is no dependency slot for `PlannerRuntime`, `ExecutionPipeline`,
  `MemoryStore`, or `WorldModel` to even construct the fixture with one.
  This is the primary verification that Section 8's boundaries hold: a
  compile-time property, not a runtime assertion that could be
  accidentally weakened later.
- The coordinator's own return value is exactly the
  `ReasoningProviderResponse` obtained — no wrapping, no additional
  fields, confirming Section 1 Item 3's stop condition is observable in
  the test, not just claimed in prose.

**Full Gradle test suite.** Per this program's own established
discipline, run the complete suite (not only the tests above) once
implementation is complete, and report a real, Android-Studio-verified
result. If the sandbox used to prepare this repository cannot execute
Gradle, report an honest, arithmetic-projected total with an explicit
"not verified" disclosure and wait for external verification before any
`IMPLEMENTATION_HISTORY.md` update (Section 10).

## 7. Acceptance Criteria

- `ConversationEngine`, `Conversation`, `Turn`,
  `ConversationDisposition`, `ConversationId`, `TurnId` are implemented
  exactly matching `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2 —
  no field added, removed, or renamed.
- `ReasoningProvider`, `ReasoningProviderRequest`,
  `ReasoningProviderResponse` (`Goal`/`Reply`/`NoAction`),
  `ReasoningContext` are implemented exactly matching
  `REASONING_PROVIDER_CONTRACT_DESIGN.md` Sections 2 and 3 — no field
  added, removed, or renamed, and no `Failed` variant introduced
  (restating that document's own Section 3 clarification: genuine
  failure, if it occurs during this unit's own testing, surfaces as an
  ordinary Kotlin exception, never as a fabricated `NoAction`).
- The composition unit (Decision 2) exists, is independently
  constructible with only a `ConversationEngine` and a
  `ReasoningProvider`, and its own tests (Section 6) pass.
- All tests listed in Section 6 pass, and the full Gradle suite passes
  (or a projected count is honestly reported, per Section 6's own
  disclosure discipline).
- No production (`src/`) code added by this unit references
  `PlannerRuntime`, `PlanningRequest`, `ExecutionPipeline`,
  `ExecutionRequest`, `OutboundParkerResponse`, `PermissionEngine`,
  `ToolRegistry`, `MemoryStore`, or `WorldModel`, anywhere — verified by
  the dependency lists in Section 4 and the structural tests in Section
  6, not merely by inspection.
- No existing `src/` file is modified (Section 3).
- **`system.conversation-engine` resolves successfully when registered.**
  A test fixture that registers `CONVERSATION_ENGINE_PRINCIPAL_ID` with
  `IdentityService` before constructing `InMemoryConversationEngine`
  demonstrates successful resolution before any Turn is bound (Section
  6).
- **A missing/unregistered `system.conversation-engine` fails fast.** A
  test fixture that omits that registration demonstrates
  `InMemoryConversationEngine` failing fast rather than proceeding with
  an unresolvable or substituted identity (Section 6).
- **The message sender Principal is never substituted for the
  Conversation Engine's own operating Principal.** A test demonstrates
  `Turn.message.senderPrincipalId` (the owner) and the Conversation
  Engine's own resolved `CONVERSATION_ENGINE_PRINCIPAL_ID` remain
  distinct and are never conflated, for a message whose sender is a
  separately-registered Principal (Section 6).

## 8. Implementation Boundaries — Out of Scope

Restating the task's own explicit list, each grounded in why it is
excluded, not merely named:

- **Invoke Planner Runtime.** No `PlannerRuntime` reference exists
  anywhere this unit's code can reach (Section 4). A `Goal` response is
  returned to the composition unit's own caller and goes no further.
- **Construct `PlanningRequest`.** Not imported, not referenced, not
  constructed, anywhere in `src/` added by this unit.
- **Invoke Response Delivery.** No `Tool`, `ToolRegistry`, or
  `ExecutionPipeline` reference exists anywhere this unit's code can
  reach. A `Reply` response is returned to the composition unit's own
  caller and goes no further.
- **Create `OutboundParkerResponse`.** Not imported, not referenced, not
  constructed, anywhere in `src/` added by this unit.
- **Modify Memory.** No `MemoryStore` reference exists anywhere this
  unit's code can reach.
- **Modify the World Model.** No `WorldModel` reference exists anywhere
  this unit's code can reach.
- **Invoke Execution Pipeline.** No `ExecutionPipeline` reference exists
  anywhere this unit's code can reach.
- **`ReasoningContext` assembly.** This unit does not read Memory or the
  World Model to build a `ReasoningContext` — every test and every
  composition-unit call accepts an already-assembled `ReasoningContext`
  as an external input (Section 1). "Reasoning Context assembly"
  ownership remains exactly as unassigned as
  `REASONING_PROVIDER_ARCHITECTURE.md` Section 14 Open Question 6 and
  `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 9 both leave it.
- **Any real, model-backed, or otherwise production-intended
  `ReasoningProvider` implementation.** Restating
  `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 9's own "provider
  implementations" deferral exactly — this unit builds the interface and
  a test-only fake, nothing more.
- **A `Failed` domain variant, or any resolution of the failure-handling
  question.** Restating `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section
  3's own clarification: this unit relies on ordinary exception
  propagation for genuine faults during its own tests; it does not
  design or name a domain-level failure shape.
- **`conversation.*` or `reasoning.*` `EventBus` publication.** Both
  remain authorised-but-not-required per their respective Architecture
  documents; this unit publishes nothing.
- **Persistence.** `Conversation`/`Turn` state lives in memory only for
  this unit's own tests; no durability claim is made or implied,
  restating `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 8's own
  identical deferral.
- **Provider selection or routing.** Only one `ReasoningProvider`
  reference is ever wired into the composition unit at a time (via
  constructor injection); no registry, no selection logic.
- **Any Android, voice, CLI, networking, or UI behaviour.**

## 9. Architectural Traceability

| Implemented element | Authorised by |
| --- | --- |
| `ConversationEngine`, `Conversation`, `Turn`, `ConversationDisposition` | `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Sections 1–2 |
| `InMemoryConversationEngine`'s "always new Conversation" default | This document, Section 5, Decision 1 — a Stage 3/4-level conservative default, not an architectural claim |
| `InMemoryConversationEngine`'s `IdentityService`-only dependency | `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 7 |
| `CONVERSATION_ENGINE_PRINCIPAL_ID` | `19-conversation-engine.md` Section 13 Item 10 ("Stage 3/6 territory"), mirroring the `PLANNER_RUNTIME_PRINCIPAL_ID`/`TASK_MANAGER_RUNTIME_PRINCIPAL_ID` precedent (Gap #49's closure) |
| `ReasoningProvider`, `ReasoningProviderRequest`, `ReasoningProviderResponse`, `ReasoningContext` | `REASONING_PROVIDER_CONTRACT_DESIGN.md` Sections 1–3 |
| No `Failed` variant; exception-based fault signalling | `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 3, reinforced by `19-conversation-engine.md` Section 11 |
| The composition unit's existence and stop condition | This task's own explicit instruction: "must stop after obtaining a `ReasoningProviderResponse`," combined with `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 1's own anticipation of "a second operation... added additively" |
| The composition unit's zero-new-contract-type status (no separate Contract Design required) | PES-001 Stage 2A's own "Required when" clause: contract design is required only when "public types" without approved field-level shapes are introduced; ordinary sequencing of two already-shaped interfaces introduces none |
| Fake `ReasoningProvider` test double confined to `tests/` | `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 9 ("provider implementations" deferred); mirrors `FakePermissionEngine`/`FakeCommunicationIntake` precedent |
| Structural (not asserted) trust-boundary enforcement | Both Contract Design documents' own "structural guarantee, not merely a stated rule" framing for runtime boundaries |

## 10. Completion Criteria

- `IMPLEMENTATION_GAPS.md` and `IMPLEMENTATION_HISTORY.md` are **not**
  touched by this Plan, and must not be touched during implementation
  until every test in Section 6 passes and the full Gradle suite result
  is known (real or honestly disclosed as projected).
- After verified implementation: `IMPLEMENTATION_HISTORY.md` **may** be
  updated, recording this unit exactly as delivered (files added, tests
  added, verified result).
- After verified implementation: `IMPLEMENTATION_GAPS.md` #53 **may be
  clarified, not closed.** This unit implements neither of gap #53's two
  named closure paths in full — it implements Turn binding and reaches a
  `ReasoningProviderResponse`, but performs no routing (path (b) remains
  open; path (a), the `ExecutionRequest` content-carrying gap, is
  untouched). Any clarifying update should state plainly: inbound Turn
  binding and Reasoning Provider invocation are now implemented; routing
  a `Goal` to Planner Runtime and a `Reply` to Response Delivery remain
  unimplemented; gap #53 remains open.
- No architecture or Contract Design document
  (`19-conversation-engine.md`, `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`,
  `REASONING_PROVIDER_ARCHITECTURE.md`,
  `REASONING_PROVIDER_CONTRACT_DESIGN.md`) is modified at any point
  during this unit's implementation.

## 11. Scope Lock

**Not yet locked.** Restating this program's own established two-step
process: this Plan defines the boundary; a separate, explicit human
instruction ("Scope Lock has been achieved") is required before any
Kotlin is written against it, per PES-001's Human-primary-authority
model for Stage 3 through Stage 5.

**What becomes frozen once locked:** exactly the file list in Section 3,
the dependency lists in Section 4, the three Required Implementation
Decisions in Section 5 (as resolved — this document proposes
conservative defaults for each; Scope Lock should either confirm or
override them explicitly before Kotlin begins), the testing strategy in
Section 6, and the Out-of-Scope list in Section 8. Any change to any of
these after Scope Lock requires a new planning pass, not a silent
adjustment during implementation — restating this program's own
"contradiction between plan and repository: stop, do not resolve in
Kotlin" discipline, applied here pre-emptively.

## Conclusion

**This document defines one Stage 3 Implementation Plan combining two
already-approved Contract Designs — `ConversationEngine`'s inbound half
and the `ReasoningProvider` contract — into a single, minimal,
independently verifiable unit that binds a Turn, invokes a Reasoning
Provider, and stops.** Three genuine interpretive gaps neither Contract
Design resolved are named explicitly as Required Implementation
Decisions, each with a conservative, precedent-matching proposed
default, rather than silently assumed. Every boundary this task named —
no Planner, no `PlanningRequest`, no Response Delivery, no
`OutboundParkerResponse`, no Memory, no World Model, no Execution
Pipeline — is enforced structurally, by the absence of any dependency
capable of reaching them, not merely asserted in prose.

This Plan does not implement anything itself, does not modify any
architecture or Contract Design document, and does not touch
`IMPLEMENTATION_GAPS.md` or `IMPLEMENTATION_HISTORY.md`. It awaits an
explicit Scope Lock instruction before any Kotlin is written.

## Related

- `docs/architecture/19-conversation-engine.md`
- `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md`
- `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md`
- `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`
- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#53)
- `src/runtime/InMemoryPlannerRuntime.kt`, `src/runtime/InMemoryTaskManagerRuntime.kt` (Principal-identity precedent)
- `tests/runtime/FakePermissionEngine.kt`, `tests/runtime/FakeCommunicationIntake.kt` (fake-implementation precedent)
