# Response Delivery Implementation Plan (Sprint 7, Unit C4)

## Status

**Stage 3 Implementation Plan, PES-001. Not yet Scope Locked.** No Kotlin
is written by this document. Restating PES-001's own Stage 3 definition,
per this program's own established precedent: "Break architectural work
into independently verifiable implementation units. Each unit defines:
Included work, Excluded work, Dependencies, Acceptance Criteria, Unit Stop
Conditions." This document performs exactly that breakdown for one unit —
no more.

**Both Stage 3 Blocking Prerequisites `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
named are now resolved, and this Plan proceeds on that basis:**

1. **Prerequisite 1** (`Resource.ownerPrincipalId = PrincipalId(moduleId.value)`
   not being approved architecture) — resolved by
   `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`
   (Accepted). Decision 2's Resource-location mechanism now depends on
   settled architecture, not a disclosed-as-unsettled interpretive choice.
2. **Prerequisite 2** (no `ActionVocabularyEntry` for `PermissionAction.NOTIFY`)
   — resolved in shape by
   `docs/implementation/RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md`,
   which fixes the entry's exact form
   (`ActionVocabularyEntry(verbPhrase = "notify owner", mappings = setOf(ActionResourceMapping(PermissionAction.NOTIFY, ResourceType.TOOL)))`).
   Registering it is this Unit's own Included Work (Section 2, below) —
   that document explicitly left the call site to this Plan.

**Grounded exclusively in the as-built code and the documents that
authorised it:**

1. `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` (Accepted,
   Stage 2A) — the complete contract this Plan implements, unmodified and
   not reopened.
2. `docs/architecture/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md` and
   `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md` —
   the two accepted architectural decisions this Plan builds on.
3. `docs/implementation/RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md` —
   the vocabulary entry this Plan registers.
4. `src/runtime/GatedOutcome.kt`, `src/interfaces/ResourceRegistry.kt`,
   `src/runtime/InMemoryResourceRegistry.kt`, `src/interfaces/ExecutionPipeline.kt`,
   `src/runtime/DefaultExecutionPipeline.kt`, `src/contracts/ExecutionRequest.kt`,
   `src/contracts/ExecutionResult.kt`, `src/interfaces/CommunicationIntake.kt`
   (`OutboundParkerResponse`), `src/contracts/ActionMapping.kt`,
   `src/runtime/ActionMapper.kt` — every dependency this Unit's own code
   touches, as built, not re-derived.
5. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage 3's
   own definition, above, and Chapter 7's in-memory concurrency/policy-seam
   discipline.

---

## Required Analysis

### 1. Where does `ResponseDelivery` sit — what calls it?

**Not designed here.** `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 8
explicitly places "constructing an `OutboundParkerResponse` from a
`ReasoningProviderResponse.Reply`" and any caller wiring out of scope —
"a future, separately-scoped coordinator's job." This Plan implements
`ResponseDelivery` itself, independently testable and constructible, with
no caller of any kind. Nothing in this Unit references `ConversationEngine`,
`ReasoningProvider`, `PlannerRuntime`, or any coordinator.

### 2. Dependency injection

`ResponseDelivery`'s constructor accepts exactly two already-existing
types: `ResourceRegistry` and `ExecutionPipeline` — both interfaces, both
already implemented (`InMemoryResourceRegistry`, `DefaultExecutionPipeline`),
neither modified by this Unit. No new interface is introduced for either.

### 3. Whether an existing helper can be reused

**Yes, in full:** `GatedOutcome<T>` (`src/runtime/GatedOutcome.kt`, Sprint
7 Unit C2) is reused unchanged as `GatedOutcome<ExecutionResult>` — no
modification, no subclassing, no third variant added.

### 4. Whether a new type is required

**Yes, exactly one:** `ResponseDelivery` itself, a concrete class (Contract
Design Decision 1). No data type is introduced — every value this Unit's
code handles (`OutboundParkerResponse`, `ExecutionRequest`, `ExecutionResult`,
`GatedOutcome<T>`, `Resource`, `ResourceId`, `PrincipalId`) already exists.

### 5. Constructor dependency changes required to existing classes

**None.** `ResourceRegistry`, `InMemoryResourceRegistry`, `ExecutionPipeline`,
`DefaultExecutionPipeline`, `ActionVocabulary`, `InMemoryActionVocabulary`,
and `ActionMapper` all keep their exact current constructor signatures and
public methods. This Unit calls only already-existing public methods on
each.

### 6. Identity requirements

**None new.** `ResponseDelivery` performs no `IdentityService` call of its
own — `response.senderPrincipalId` is threaded through unchanged into
`ExecutionRequest.principalId` (Contract Design Section 1 Step 2), and
whatever identity resolution `ExecutionPipeline.submit`/`PermissionEngine.evaluate`
already perform for any other caller applies identically here, unmodified.

### 7. `EventBus` implications

**None new.** `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Deferred Item 6
already confirms `DefaultExecutionPipeline` publishes its own existing
`execution.*` lifecycle events for any `ExecutionRequest`, including one
`ResponseDelivery` constructs — this Unit adds no new publication, and
`ResponseDelivery` itself neither publishes nor subscribes to anything.

### 8. Where is the `NOTIFY` vocabulary entry registered?

Confirmed by `RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md` Q5: no
composition root exists in this repository today (no `fun main(` under
`src/`), and `InMemoryActionVocabulary` holds zero entries anywhere in the
current codebase. This Plan's own tests (Section 6) construct an
`InMemoryActionVocabulary`, register the one `NOTIFY` entry via
`ActionVocabulary.register(...)`, and inject that same instance into the
`ActionMapper` used by the `DefaultExecutionPipeline` under test —
mirroring how every existing Sprint 7 test already assembles its own
dependency graph by hand (`CommunicationConversationCoordinatorTest`,
`DefaultExecutionPipelineTest`). **A future, real composition root must
perform the identical `ActionVocabulary.register` call once, at startup**
— named here so it is not silently forgotten when one is eventually built;
that composition root does not exist yet and is out of this Unit's scope.

### 9. Test strategy

See Section 6 (full detail). Summary: a new `tests/runtime/ResponseDeliveryTest.kt`
using new `FakeResourceRegistry`/`FakeExecutionPipeline` fixtures for
isolated, exception-injectable unit tests (mirroring `FakeCommunicationIntake`/`FakeReasoningProvider`'s
own established pattern), plus one end-to-end-style test wiring the real
`InMemoryResourceRegistry` and `DefaultExecutionPipeline` together
(mirroring the existing `VerticalSliceEndToEndTest.kt` precedent) to prove
the full, real stack — including the newly registered `NOTIFY` vocabulary
entry — actually delivers.

### 10. Files to be modified

**All additions. No existing `src/` or `tests/` file requires
modification.** See Section 3 for the complete list.

---

## 1. Objective

Implement, and make independently testable, the smallest component that
carries out `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`'s complete Section 1
contract:

```
OutboundParkerResponse
    ↓
ResponseDelivery.deliver(response)
    1. ResourceRegistry.listByOwner(PrincipalId(response.channelId.value))
       -> filter TOOL -> exactly one? else GatedOutcome.NotAccepted(reason)
    2. construct one ExecutionRequest (Section 1, Step 2)
    3. ExecutionPipeline.submit(request) -> GatedOutcome.Produced(result)
    ↓
GatedOutcome<ExecutionResult>
```

**This is the unit's stop condition:** `ResponseDelivery` is delivered as
a standalone, callable, fully-tested component. Nothing calls it. Nothing
constructs an `OutboundParkerResponse` for it. No routing, coordination, or
caller wiring is added.

## 2. Included Work

- One new, small, concrete, non-interface-backed class (`ResponseDelivery`,
  `src/runtime/ResponseDelivery.kt`) with exactly one public `suspend`
  method, `deliver(response: OutboundParkerResponse): GatedOutcome<ExecutionResult>`,
  implementing `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 1 exactly:
  1. Calls `resourceRegistry.listByOwner(PrincipalId(response.channelId.value))`,
     filters to `resourceType == ResourceType.TOOL`.
  2. If the filtered result's size is not exactly `1`, returns
     `GatedOutcome.NotAccepted(reason)`, with `reason` distinguishing "no
     matching Resource" from "more than one matching Resource" (Contract
     Design Section 5). No `ExecutionRequest` is constructed in this case.
  3. If exactly one match, constructs one `ExecutionRequest` (Section 5,
     Decision 1, below, fixes every field this Plan itself must decide)
     and calls `executionPipeline.submit(request)`, returning
     `GatedOutcome.Produced(result)`.
- A public, well-known metadata-key constant (proposed name:
  `RESPONSE_TEXT_METADATA_KEY`, `src/runtime/ResponseDelivery.kt`,
  top-level `const val`) — discharging `ADR-025`'s own Stage 4 deferral of
  the exact key (Section 5, Decision 1, below), and exposed so a future
  "deliver" Tool implementation (Contract Design Section 4, Deferred Item
  2) can read `request.metadata[RESPONSE_TEXT_METADATA_KEY]` without
  redefining the key itself.
- Registering the one `ActionVocabularyEntry` fixed by
  `RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md` — performed inside this
  Unit's own tests (Section 6; Required Analysis Item 8), not in any
  production file, since no composition root exists to register it at.
- Unit and integration tests, per Section 6.

## 3. Files Expected to Change

**All additions. No existing `src/` or `tests/` file requires
modification** — `ResourceRegistry`, `InMemoryResourceRegistry`,
`ExecutionPipeline`, `DefaultExecutionPipeline`, `ActionVocabulary`,
`InMemoryActionVocabulary`, `ActionMapper`, `OutboundParkerResponse`,
`ExecutionRequest`, `ExecutionResult`, and `GatedOutcome<T>` are all
consumed exactly as they exist today.

| File | New/Modified | Contents |
| --- | --- | --- |
| `src/runtime/ResponseDelivery.kt` | New | The `ResponseDelivery` class described in Section 2, plus the `RESPONSE_TEXT_METADATA_KEY` constant. |
| `tests/runtime/FakeResourceRegistry.kt` | New | A configurable `ResourceRegistry` test double — canned `listByOwner` results, exception injection — mirroring `FakeCommunicationIntake`'s own established shape. |
| `tests/runtime/FakeExecutionPipeline.kt` | New | A configurable `ExecutionPipeline` test double — canned `submit` results, exception injection, call recording (the submitted `ExecutionRequest`, call count) — mirroring `FakeReasoningProvider`'s own established shape. |
| `tests/runtime/ResponseDeliveryTest.kt` | New | Tests per Section 6, using both fakes for isolated behaviour, plus one end-to-end test using the real `InMemoryResourceRegistry` + `DefaultExecutionPipeline` + the registered `NOTIFY` vocabulary entry (mirroring `VerticalSliceEndToEndTest.kt`'s own established pattern). |

No new file is added under `src/interfaces/` or `tests/contracts/` — per
Contract Design Decision 1, `ResponseDelivery` is a concrete class, not a
public contract type of its own.

## 4. Dependencies

**The new class's only dependencies: `ResourceRegistry` and
`ExecutionPipeline`, both constructor-injected, both already-existing
interface types, neither modified.**

Mirroring this program's own established structural-enforcement discipline
(not merely an assertion): `ResponseDelivery`'s constructor has no slot for
`ToolRegistry`, `PermissionEngine`, `ModuleRegistry`, `IdentityService`,
`CommunicationIntake`, `ConversationEngine`, `ReasoningProvider`,
`PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or `WorldModel` — per
Contract Design Section 1's own "Dependencies" paragraph, restated and now
enforced in code, not only in prose. `ToolRegistry` and `PermissionEngine`
are reached only indirectly, inside `ExecutionPipeline.submit`, exactly as
they already are for every other existing caller.

## 4a. Statelessness Invariant

**`ResponseDelivery` owns no state between invocations. It must remain a
pure orchestration component.** Restated in code terms from Contract
Design Section 10: it declares exactly its two constructor-injected
dependencies as fields — no `var`, no mutable collection, no cache of any
prior `OutboundParkerResponse`/`ExecutionRequest`/`ExecutionResult`, no
`Mutex`. Each call to `deliver` is fully independent of every other call,
mirroring `CommunicationConversationCoordinator`'s own identical, already-
accepted invariant and its own identical justification (a stateful
component here would create a second, informal, untested place delivery
behaviour could diverge from what this Plan actually authorises).

## 4b. Response Pass-Through Invariant

**`ResponseDelivery` must never mutate or reinterpret an
`OutboundParkerResponse`. It reads four fields — `text`, `senderPrincipalId`,
`correlationId`, `channelId` — and constructs a new `ExecutionRequest` from
them; it never constructs a new `OutboundParkerResponse`, never calls
`.copy()` on the one it receives, and never branches on `response.text`'s
own content.** Restated in code terms from Contract Design Section 10
("Response pass-through"). Enforced in Section 6 by an exact-value
assertion between the input `response`'s four fields and the corresponding
fields on the `ExecutionRequest` `FakeExecutionPipeline` actually receives.

## 4c. Exception Propagation Invariant

**`ResponseDelivery` must not recover from, translate, retry, or suppress
an exception thrown by either dependency. Such failures propagate
unchanged to the caller.** Restated in code terms from Contract Design
Section 10 ("No exception recovery"), mirroring
`CommunicationConversationCoordinator`'s own identical, already-accepted
invariant and enforcement style. Concretely: `ResponseDelivery.deliver`
contains no `try`/`catch` of any kind around either `resourceRegistry.listByOwner`
or `executionPipeline.submit`. `GatedOutcome.NotAccepted` is constructed
**only** for the two structural Resource-location outcomes Contract Design
Section 5 names (zero matches, more than one match) — never to catch and
repackage a thrown exception. Enforced in Section 6 by tests in which each
dependency throws and `deliver`'s own call is asserted to propagate that
exact exception unchanged.

## 5. Required Implementation Decisions

Mirroring this program's own established practice: two genuine
interpretive forks remain, not resolved by any existing document, each
named here with a proposed, conservative default, awaiting confirmation
before Kotlin proceeds.

### Decision 1 — Exact `ExecutionRequest` field values

`RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 1 Step 2 fixes each field's
*source* but leaves two concrete string values unfixed, both proposed here
as conservative defaults matching the Contract Design's own stated
examples exactly, changing nothing:

- **`intent`**: `"deliver response"` — the exact example Contract Design
  Section 1 Step 2 itself gives ("e.g. 'deliver response'"), adopted
  verbatim rather than inventing a different phrase with no stated reason
  to.
- **`proposedActions`**: `listOf("notify owner")` — fixed by
  `RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md` Q1/Q2, not re-decided
  here.
- **The `ExecutionRequest.metadata` key carrying `response.text`**
  (Contract Design Section 3; Deferred Item 1; `ADR-025`'s own explicit
  Stage 4 deferral): **`RESPONSE_TEXT_METADATA_KEY = "response.text"`**, a
  `const val` in `src/runtime/ResponseDelivery.kt`. This is the one
  concrete Stage 4 Implementation Decision this Plan resolves directly,
  rather than deferring to a separate document — the key must be a fixed
  string for `deliver`'s own implementation to compile against, and
  exposing it as a named, public constant (rather than an inline literal)
  lets a future "deliver" Tool implementation (Contract Design Section 4)
  read `request.metadata[RESPONSE_TEXT_METADATA_KEY]` without redefining
  or guessing the string. `metadata = mapOf(RESPONSE_TEXT_METADATA_KEY to response.text)`
  — exactly one entry, nothing else, per Contract Design Section 3's own
  "exactly one metadata entry."
- **`priority`, `sessionId`, `riskEstimate`, `expiresAt`**: left at
  `ExecutionRequest`'s own existing defaults, per Contract Design Section
  1's own explicit instruction ("this document identifies no requirement
  to set any of them").

### Decision 2 — Distinguishing the two `NotAccepted` reasons

Contract Design Section 5 requires the zero-match and many-match cases to
be distinguished by `reason`, but does not fix the exact string. **Proposed
default:** two distinct, human-readable strings —
`"no channel Resource found for channelId '<value>'"` for the zero-match
case, and `"ambiguous: <n> channel Resources found for channelId '<value>'"`
for the many-match case (`<n>` = the actual count). Neither string is
parsed by any caller anywhere in this repository today (`GatedOutcome.NotAccepted.reason`
is `String`, consumed only for logging/display per every existing
precedent use), so this is a presentation-only choice with no structural
consequence — named explicitly so it is not improvised inconsistently
during implementation.

## 6. Testing Strategy

**`ResponseDeliveryTest.kt`, isolated (via `FakeResourceRegistry`/`FakeExecutionPipeline`):**

- **Zero-match path.** `FakeResourceRegistry.listByOwner` returns an empty
  list (or a list with no `TOOL`-type Resource). `deliver` returns
  `GatedOutcome.NotAccepted(reason)` naming the zero-match case;
  `FakeExecutionPipeline.submitCallCount` remains `0` — the structural
  proof that no `ExecutionRequest` is ever constructed or submitted for
  this case, not merely that its result is discarded.
- **Many-match path.** `FakeResourceRegistry.listByOwner` returns two or
  more `TOOL`-type Resources for the same owner. `deliver` returns
  `GatedOutcome.NotAccepted(reason)` naming the ambiguous case, distinctly
  from the zero-match reason; `FakeExecutionPipeline.submitCallCount`
  remains `0`.
- **Exactly-one-match path.** `FakeResourceRegistry.listByOwner` returns
  exactly one `TOOL`-type Resource (plus, in one variant of this test, an
  additional non-`TOOL` Resource for the same owner, proving the `TOOL`
  filter is applied, not merely a bare size check). `FakeExecutionPipeline`
  is configured to return a canned `ExecutionResult`. `deliver` returns
  `GatedOutcome.Produced(result)` wrapping that exact `ExecutionResult`
  unchanged.
- **`ExecutionRequest` field-construction correctness (Section 4b).** For
  the exactly-one-match path, the `ExecutionRequest`
  `FakeExecutionPipeline.submit` actually receives is asserted
  field-by-field: `principalId == response.senderPrincipalId`,
  `origin == RequestOrigin.TEXT`, `intent == "deliver response"`,
  `targetResources == listOf(theOneResource.resourceId)`,
  `proposedActions == listOf("notify owner")`,
  `correlationId == response.correlationId.value`,
  `metadata == mapOf(RESPONSE_TEXT_METADATA_KEY to response.text)` exactly
  — not merely containing that entry among others.
- **Exception propagation, not recovery (Section 4c).** With
  `FakeResourceRegistry` configured to throw from `listByOwner`,
  `deliver`'s own call is asserted (via `assertFailsWith`) to propagate
  that exact exception — not any `GatedOutcome` variant. The identical
  test is repeated with `FakeExecutionPipeline` configured to throw from
  `submit` instead. Neither test constructs, expects, or tolerates a
  `try`/`catch` anywhere in `ResponseDelivery`'s own code.
- **Statelessness (Section 4a).** A reflective test asserting
  `ResponseDelivery::class.java.declaredFields` contains exactly its two
  constructor-injected dependencies and nothing else. A second test runs
  one `ResponseDelivery` instance across two independent invocations, for
  two different `OutboundParkerResponse`s, and asserts the second call's
  outcome is unaffected by the first.
- **Response pass-through (Section 4b).** `response` itself (not only the
  resulting `ExecutionRequest`) is asserted unchanged after the call — no
  field of the `OutboundParkerResponse` instance is mutated (structurally
  guaranteed already, since `OutboundParkerResponse` is a `data class`
  with `val` fields, but asserted explicitly as a regression guard).
- **Exactly-once invocation.** For a single call to `deliver`:
  `FakeResourceRegistry.listByOwnerCallCount` is exactly `1`, and, for the
  exactly-one-match path, `FakeExecutionPipeline.submitCallCount` is
  exactly `1`. Re-checked as `0` for the `ExecutionPipeline` call count on
  both the zero-match and many-match paths.

**`ResponseDeliveryEndToEndTest.kt` (or a section within
`ResponseDeliveryTest.kt`), using real implementations — mirroring
`VerticalSliceEndToEndTest.kt`'s own established pattern:**

- A real `InMemoryResourceRegistry`, with one `TOOL`-type `Resource`
  registered with `ownerPrincipalId = PrincipalId(<channelId>.value)`
  (per `ADR-026`'s now-settled convention).
- A real `InMemoryActionVocabulary`, with the one `NOTIFY` entry from
  `RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md` registered via
  `.register(...)` (Required Analysis Item 8).
- A real `DefaultExecutionPipeline`, wired with that `ActionVocabulary`
  (via `ActionMapper`), a real `InMemoryToolRegistry` with a minimal test
  `Tool`/`ToolDescriptor` registered supporting `PermissionAction.NOTIFY`
  against `ResourceType.TOOL`, and whatever `PermissionEngine` test
  configuration `DefaultExecutionPipelineTest.kt` already establishes as
  its own permissive baseline (reused, not reinvented).
- Calling `ResponseDelivery(resourceRegistry, executionPipeline).deliver(response)`
  end-to-end and asserting `GatedOutcome.Produced` with a `SUCCESS`
  (or otherwise expected) `ExecutionResult` — the concrete proof that
  `ADR-026`'s convention, the registered `NOTIFY` vocabulary entry, and
  `ResponseDelivery`'s own construction logic all cohere as a real,
  running path, not only as isolated, fake-backed unit tests.

**Full Gradle test suite.** Per this program's own established discipline,
run the complete suite (not only the tests above) once implementation is
complete, and report a real, Android-Studio-verified result. If the
sandbox used to prepare this repository cannot execute Gradle, report an
honest, arithmetic-projected total with an explicit "not verified"
disclosure and wait for external verification before any
`IMPLEMENTATION_HISTORY.md` update (Section 10).

## 7. Acceptance Criteria

- `ResponseDelivery` exists, is independently constructible with only a
  `ResourceRegistry` and an `ExecutionPipeline`, and its own tests
  (Section 6) pass.
- **Zero or more than one matching Resource never produces an
  `ExecutionRequest` or a call to `ExecutionPipeline.submit`** — verified
  structurally (Section 6), not merely by inspection.
- **Exactly one matching Resource produces exactly one, field-correct
  `ExecutionRequest`, submitted exactly once** — verified structurally
  (Section 6, field-by-field), not merely by inspection.
- **The component is stateless (Section 4a)** — declares no field beyond
  its two constructor-injected dependencies; two invocations never
  observably interact — verified structurally (Section 6).
- **The component never mutates or reinterprets an `OutboundParkerResponse`
  (Section 4b)** — verified structurally (Section 6).
- **The component never recovers from, translates, retries, or suppresses
  an exception thrown by either dependency (Section 4c)** — verified
  structurally (Section 6).
- **The `NOTIFY` vocabulary entry, once registered (Section 6, end-to-end
  test), resolves correctly through the real `ActionMapper`/`ExecutionPipeline`
  stack** — verified by the end-to-end test, not only by isolated fakes.
- No production (`src/`) code added by this Unit references `ToolRegistry`,
  `PermissionEngine`, `ModuleRegistry`, `IdentityService`,
  `CommunicationIntake`, `ConversationEngine`, `ReasoningProvider`,
  `PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or `WorldModel`, anywhere
  — verified by the dependency list in Section 4.
- No existing `src/` or `tests/` file is modified (Section 3).
- All tests listed in Section 6 pass, and the full Gradle suite passes (or
  a projected count is honestly reported, per Section 6's own disclosure
  discipline).

## 8. Implementation Boundaries — Out of Scope

Restating `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`'s own Out of Scope
(Section 12) and Non-Responsibilities (Section 8), each grounded in why it
is excluded here, not merely named:

- **Constructing an `OutboundParkerResponse` from a `Reply`.** No code
  path in this Unit produces one — every test supplies an
  already-constructed `OutboundParkerResponse` as input.
- **Any caller of `ResponseDelivery.deliver`.** None is added. No
  `ConversationEngine`, `ReasoningProvider`, or coordinator reference
  exists anywhere this Unit's code can reach.
- **Planner integration, Goal routing, `PlanCandidate` generation, Workflow
  Runtime.** No reference exists anywhere this Unit's code can reach.
- **Android, UI, Speech, Notifications.** No such dependency or API is
  introduced.
- **Persistence, Memory writes, World Model writes.** `ResponseDelivery`
  holds no state between calls; nothing it produces is written to disk, a
  database, or any durable store, or to `MemoryStore`/`WorldModel`.
- **Retry policy, queueing, streaming responses, multiple recipients,
  multi-channel fan-out, batch delivery.** Exactly one `ExecutionRequest`
  is submitted per call, to exactly one channel, unconditionally (Section
  4a, 4c).
- **Registering the Local Text Channel's own "deliver" `ToolDescriptor`.**
  Contract Design Deferred Item 2 — a separate, additive revision to
  `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`, not performed here. This Unit's
  end-to-end test (Section 6) registers a minimal test `Tool` for its own
  isolated verification only — not the real Local Text Channel deliver
  Tool, and not a production registration.
- **A composition root / production wiring for the `NOTIFY` vocabulary
  registration.** Named explicitly (Required Analysis Item 8) as a future
  unit's responsibility, since none exists in this repository today.
- **Any redesign of `ExecutionRequest`, `ExecutionPipeline`, `ToolRegistry`,
  `PermissionEngine`, `ResourceRegistry`, `ActionMapper`, or
  `ActionVocabulary`.** Every one is reused exactly as it already exists;
  none is modified by this Unit (Section 3).
- **A `response_delivered`/`response_delivery_failed` observability event.**
  Contract Design Deferred Item 6 — not designed or implemented here;
  `DefaultExecutionPipeline`'s existing `execution.*` events already cover
  observability for the `ExecutionRequest` this Unit constructs.

## 9. Architectural Traceability

| Implemented element | Authorised by |
| --- | --- |
| `ResponseDelivery.deliver`'s three-step sequence | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 1 |
| Zero/many-match handling returning `GatedOutcome.NotAccepted` | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 5 |
| `ResourceRegistry.listByOwner(PrincipalId(response.channelId.value))` as the Resource-location mechanism | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Decision 2; `ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md` |
| `ResponseDelivery`'s non-interface-backed shape | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Decision 1 |
| `GatedOutcome<ExecutionResult>` as the return type | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 1, Minimalism Review |
| `RESPONSE_TEXT_METADATA_KEY` exact value | This document, Section 5, Decision 1 — discharging `ADR-025`'s own Stage 4 deferral |
| `proposedActions = listOf("notify owner")`; the registered `NOTIFY` vocabulary entry | `RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md` |
| `response.metadata` not forwarded | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 3 |
| Statelessness invariant (Section 4a) | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 10 |
| Response pass-through invariant (Section 4b) | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 10 |
| Exception propagation invariant (Section 4c) | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 10 |
| No caller, no `OutboundParkerResponse` construction | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 8, Section 12 |

## 10. Completion Criteria

- `IMPLEMENTATION_GAPS.md` and `IMPLEMENTATION_HISTORY.md` are **not**
  touched by this Plan, and must not be touched during implementation
  until every test in Section 6 passes and the full Gradle suite result is
  known (real or honestly disclosed as projected).
- After verified implementation: `IMPLEMENTATION_HISTORY.md` **may** be
  updated, recording this Unit exactly as delivered (files added, tests
  added, verified result).
- After verified implementation: `IMPLEMENTATION_GAPS.md` #53 **may be
  clarified further, not closed.** This Unit implements Response Delivery
  in full, on its own, but nothing yet calls it — gap #53's own routing
  question (Goal/Reply routing to Planner Runtime/Response Delivery)
  remains open for the `Reply` side exactly as it was before, minus the
  fact that a real, tested delivery mechanism now exists to route to.
  `IMPLEMENTATION_GAPS.md` #52's own first item may separately be
  clarified to note `ADR-026`'s acceptance, distinct from and not
  performed by this Plan.
- No architecture or Contract Design document (`RESPONSE_DELIVERY_CONTRACT_DESIGN.md`,
  `COMMUNICATION_CONTRACT_DESIGN.md`, `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`,
  either ADR) is modified at any point during this Unit's implementation.

## 11. Scope Lock

**Not yet locked.** Restating this program's own established two-step
process: this Plan defines the boundary; a separate, explicit human
instruction ("Scope Lock has been achieved") is required before any Kotlin
is written against it, per PES-001's Human-primary-authority model for
Stage 3 through Stage 5.

**What becomes frozen once locked:** the file list in Section 3, the
dependency list in Section 4, the three invariants in Sections 4a-4c, the
two Required Implementation Decisions in Section 5 (as resolved — this
document proposes conservative defaults for each; Scope Lock should either
confirm or override them explicitly before Kotlin begins), the testing
strategy in Section 6, and the Out-of-Scope list in Section 8. Any change
to any of these after Scope Lock requires a new planning pass, not a
silent adjustment during implementation.

## Conclusion

**This document defines one Stage 3 Implementation Plan for the smallest
safe unit implementing `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`'s complete
Section 1 contract as a real, independently testable Kotlin component.**
No existing component is modified; one new, small, non-interface-backed
class (`ResponseDelivery`) is added, along with the one `NOTIFY` vocabulary
entry `RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md` already fixed and
the one metadata-key constant this Plan itself resolves (Section 5,
Decision 1). Both Stage 3 Blocking Prerequisites the Contract Design named
are resolved before this Plan proceeds — `ADR-026` for the Resource-
ownership convention, the vocabulary decision document for `NOTIFY` — and
this Plan cites both rather than re-deriving either. Statelessness (Section
4a), response pass-through (Section 4b), and exception propagation (Section
4c) are stated as explicit invariants, each enforced structurally by a
dedicated test, mirroring every prior Sprint 7 Unit's own discipline. Every
boundary the Contract Design named — no caller, no Planner, no Memory, no
World Model, no Android, no Speech, no UI, no persistence, no retry, no
multi-channel fan-out — is enforced structurally, by the absence of any
dependency capable of reaching them, not merely asserted in prose.

This Plan does not implement anything itself, does not modify any
architecture, Contract Design, or ADR document, and does not touch
`IMPLEMENTATION_GAPS.md` or `IMPLEMENTATION_HISTORY.md`. It awaits an
explicit Scope Lock instruction before any Kotlin is written.

## Related

- `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
- `docs/architecture/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`
- `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`
- `docs/implementation/RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md`
- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#52, #53)
- `src/runtime/GatedOutcome.kt`
- `src/interfaces/ResourceRegistry.kt`, `src/runtime/InMemoryResourceRegistry.kt`
- `src/interfaces/ExecutionPipeline.kt`, `src/runtime/DefaultExecutionPipeline.kt`
- `src/contracts/ActionMapping.kt`, `src/runtime/ActionMapper.kt`
- `src/interfaces/CommunicationIntake.kt` (`OutboundParkerResponse`)
- `tests/runtime/VerticalSliceEndToEndTest.kt` (precedent for the
  end-to-end test in Section 6)
