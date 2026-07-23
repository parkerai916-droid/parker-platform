# Reply Delivery Coordinator — Implementation Plan (Sprint 10, Unit 2)

## Status

**Stage 3 Implementation Plan, PES-001. Not yet Scope Locked.** No Kotlin
is written by this document.

**This is a redefinition, not the original Sprint 10 design.** A prior
draft of `ReplyDeliveryCoordinator` constructed `OutboundParkerResponse`
directly, depended on `ResponseDelivery` as its sole dependency, and used
an unresolved `PARKER_SYSTEM_PRINCIPAL_ID` constant as sender identity.
That design was withdrawn in full, per
`RECONCILIATION_ADDENDUM_RESPONSE_COMPOSER_VS_REPLY_DELIVERY_COORDINATOR.md`,
Option C — it reintroduced the same defect class `IMPLEMENTATION_GAPS.md`
#49 already fixed once (an identity constant used without resolution).
This document defines a different component that happens to keep the same
name: a thin, non-interface-backed orchestrator with exactly two
dependencies, `ResponseComposer` and `ResponseDelivery`, that constructs
nothing itself and resolves no identity of its own.

**This Unit depends on `ResponseComposer`, defined by the companion Stage
3 Plan `REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md` — neither
component is implemented yet.** This document proceeds on the assumption
that `ResponseComposer` is implemented, tested, and Scope Locked first
(that Plan's own Section 16, Implementation Sequence); this Plan's own
Implementation Sequence (Section 14) restates that ordering as a
precondition, not an assumption left implicit.

**Grounded exclusively in the as-built code, the as-planned companion
Unit, and the documents that authorise both:**

1. `src/runtime/CommunicationConversationCoordinator.kt`,
   `src/runtime/ConversationTurnReasoningCoordinator.kt` — as built, the
   direct structural precedent this document mirrors: a small,
   non-interface-backed class, constructor-injected with exactly the two
   components it sequences, one public method, no business logic of its
   own.
2. `docs/implementation/COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md`
   — the direct structural precedent Plan for that same pair of classes,
   mirrored section-for-section below.
3. `REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md` (companion Unit,
   as planned, Sections 10–11) — `ResponseComposer`'s proposed signature,
   `suspend fun compose(originalMessage: InboundOwnerMessage,
   reasoningResponse: ReasoningProviderResponse): GatedOutcome<OutboundParkerResponse>`.
4. `src/runtime/ResponseDelivery.kt` — as built:
   `suspend fun deliver(response: OutboundParkerResponse): GatedOutcome<ExecutionResult>`,
   constructor-injected with `ResourceRegistry` and `ExecutionPipeline`
   only. Reused unchanged; not modified by this Unit.
5. `src/runtime/GatedOutcome.kt` — as built (`sealed class GatedOutcome<out T>`,
   `Produced<out T>(value: T)`, `NotAccepted(reason: String) : GatedOutcome<Nothing>()`).
   The `out T` variance is load-bearing for this Unit's own return
   behaviour (Section 5, Decision 1).
6. `RECONCILIATION_ADDENDUM_RESPONSE_COMPOSER_VS_REPLY_DELIVERY_COORDINATOR.md`
   — Option C, the governing decision this Plan implements.
7. `docs/architecture/IMPLEMENTATION_GAPS.md` #49 — the identity-constant
   defect class this Unit's own design must not reintroduce (restated
   from the companion Plan's own Second revision note).
8. `docs/architecture/IMPLEMENTATION_GAPS.md` #53 — the gap this Unit,
   together with its companion, closes the routing/delivery half of.
9. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001).

---

## Required Analysis

### 1. What exact contracts are involved?

Reused unchanged (once the companion Unit is implemented):

- `ResponseComposer.compose(originalMessage: InboundOwnerMessage,
  reasoningResponse: ReasoningProviderResponse): GatedOutcome<OutboundParkerResponse>`
  — companion Unit, as planned.
- `ResponseDelivery.deliver(response: OutboundParkerResponse): GatedOutcome<ExecutionResult>`
  — already built, already verified (Sprint 8).
- `GatedOutcome<T>` — reused unchanged, as both an input and an output
  shape.
- `InboundOwnerMessage`, `ReasoningProviderResponse` — this Unit's own
  input types, unchanged, passed straight through to `ResponseComposer`.

**No existing or companion contract requires a new field, a new variant,
or any modification.**

### 2. Does this require a new architecture note, specification update, or ADR?

**No.** Identical reasoning to
`COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md` Required
Analysis, applied here: this Unit introduces zero new public contract
types. Its return type is the existing `GatedOutcome<ExecutionResult>` —
already `ResponseDelivery.deliver`'s own return type, reused verbatim,
not merely reused in shape. Its two dependencies are both existing,
concrete, already-planned-or-built classes. A small, concrete,
non-interface-backed component sequencing two already-approved components
is ordinary Stage 3 implementation-level wiring, not a new architectural
boundary — the same conclusion the precedent Plan reached for the
structurally identical `CommunicationConversationCoordinator`.

**Conclusion: proceed directly to a Stage 3 Implementation Plan, no
Contract Design revision, no ADR, no architecture note.**

### 3. Where does this Unit sit, structurally?

```
InboundOwnerMessage, ReasoningProviderResponse
    v
ReplyDeliveryCoordinator.composeAndDeliver(originalMessage, reasoningResponse)   [NEW -- this Unit]
    v
ResponseComposer.compose(originalMessage, reasoningResponse)   [companion Unit, called exactly once]
    v
  GatedOutcome.NotAccepted(reason)  -> returned unchanged; ResponseDelivery is never called; stop
  GatedOutcome.Produced(response)   -> continue
    v
ResponseDelivery.deliver(response)   [existing, called exactly once]
    v
  GatedOutcome<ExecutionResult>  -> returned unchanged, whatever it is (Produced or NotAccepted)
```

This is structurally identical in shape to
`CommunicationConversationCoordinator.submitAndReason` (Required Analysis
item 1 of the precedent Plan): call the first dependency, branch on its
`GatedOutcome`, call the second dependency only on the admitted branch,
return unchanged. The only structural difference: the precedent's second
dependency returns a plain `ReasoningProviderResponse`, which the
precedent then wraps in `GatedOutcome.Produced` itself. Here, the second
dependency (`ResponseDelivery.deliver`) already returns a
`GatedOutcome<ExecutionResult>` — so this Unit returns that value
directly, with no additional wrapping layer (Section 5, Decision 1).

### 4. Whether an existing helper can be reused

**Yes, in full, on both sides — the entire point of this Unit.**
`ResponseComposer.compose` and `ResponseDelivery.deliver` are each reused
completely unchanged, through their existing concrete types. No existing
class needs a new method, a new constructor parameter, or any behavioural
change.

### 5. Whether a new coordinator is required

**Yes.** Nothing in this repository, once the companion Unit lands, calls
`ResponseDelivery.deliver` with a `ResponseComposer`-composed
`OutboundParkerResponse` — the two components exist independently and are
not wired to each other. This is the smallest safe unit that closes that
gap.

### 6. Constructor dependency changes required

**None, to any existing or companion class.** `ResponseComposer` and
`ResponseDelivery` both keep their exact planned/current constructor
signatures. This Unit's own constructor accepts exactly two dependencies:
`ResponseComposer` and `ResponseDelivery`.

### 7. Identity requirements

**None new, and none at all.** `ResponseComposer` already resolves its
own operating identity (`system.response-composer`) before constructing a
response (companion Plan, Section 4c). `ResponseDelivery` resolves
resources via `ResourceRegistry`, not `IdentityService`, and has no
identity concept of its own. This Unit introduces **no `IdentityService`
dependency, holds no Principal constant, and makes no accountability
claim of its own** — it is pure sequencing. This is the central lesson
the Reconciliation Addendum drew from `IMPLEMENTATION_GAPS.md` #49: this
Unit does not get to have an operating identity, because it constructs
nothing that would need one.

### 8. EventBus implications

**None.** Neither `ResponseComposer` nor `ResponseDelivery` publishes to
or subscribes from `EventBus` on this Unit's behalf, and this Unit itself
publishes nothing.

### 9. Test strategy

See Section 7 below. Summary: because neither `ResponseComposer` nor
`ResponseDelivery` is interface-backed (both are concrete, non-open
classes, per this program's own established precedent of small,
concrete, non-interface-backed components), neither can be replaced by a
call-counting Fake through the type system, the technique
`CommunicationConversationCoordinatorTest` used for its own two
interface-backed dependencies. Section 5, Decision 2 names this
explicitly and proposes the corrected, honest alternative: real
instances of both, observed through each one's own already-existing
*interface-backed* constructor dependencies one level further down — a
`FakeIdentityService` behind `ResponseComposer` (companion Plan, Section
3), and the exact `FakeResourceRegistry`/`FakeExecutionPipeline` fixtures
`ResponseDeliveryTest.kt` itself already uses behind `ResponseDelivery`,
giving genuine call counts and captured field values rather than an
inferred proxy or an unproven object-reference claim.

### 10. Files to be modified

**All additions. No existing `src/` or `tests/` file, and no companion-Unit
file, requires modification.**

---

## 1. Objective

Implement, and make independently testable, the smallest coordinator that
connects the (companion-Unit) `ResponseComposer` to the already-built
`ResponseDelivery`:

1. Accept the same inputs required to invoke `ResponseComposer`:
   `originalMessage: InboundOwnerMessage`, `reasoningResponse:
   ReasoningProviderResponse`.
2. Call `ResponseComposer.compose(originalMessage, reasoningResponse)`
   exactly once.
3. If the result is `GatedOutcome.NotAccepted`, stop and return it
   unchanged — `ResponseDelivery` is never called.
4. If the result is `GatedOutcome.Produced(response)`, call
   `ResponseDelivery.deliver(response)` exactly once.
5. Return `ResponseDelivery.deliver`'s own `GatedOutcome<ExecutionResult>`
   unchanged — whatever it is, `Produced` or `NotAccepted`.

**This is this Unit's entire responsibility. Nothing else happens.**

## 2. Included Work

- One new, small, standalone, non-interface-backed coordinator (proposed
  name: `ReplyDeliveryCoordinator`, retaining the name per the
  Reconciliation Addendum, Decision 2 below) with exactly one public
  method that performs the five-step sequence in Section 1.
- Unit tests for the coordinator, per Section 7.

## 3. Files Expected to Change

**All additions. No existing `src/` or `tests/` file requires
modification** — `ResponseDelivery` and `GatedOutcome` are consumed
exactly as they exist today; `ResponseComposer` is consumed exactly as
the companion Plan defines it; `FakeResourceRegistry`,
`FakeExecutionPipeline`, and `FakePermissionEngine` (all existing test
fixtures, per `ResponseDeliveryTest.kt`) are reused unchanged.

| File | New/Modified | Contents |
| --- | --- | --- |
| `src/runtime/ReplyDeliveryCoordinator.kt` | New | The class described in Section 2, returning `GatedOutcome<ExecutionResult>`. |
| `tests/runtime/ReplyDeliveryCoordinatorTest.kt` | New | Tests per Section 7, built on the companion Plan's `FakeIdentityService` and the existing `FakeResourceRegistry`/`FakeExecutionPipeline` (Decision 2, corrected). |

No new file is added under `src/interfaces/` — this Unit introduces no
public contract type. Unlike the companion Plan, this Unit adds no new
test fixture of its own — it reuses `FakeIdentityService` (companion
Plan, Section 3) and the pre-existing `FakeResourceRegistry`/
`FakeExecutionPipeline` (`tests/runtime/`, already committed).

## 4. Dependencies

**Exactly two, constructor-injected, both already-existing (or
companion-planned) concrete types, neither modified:**

- **`ResponseComposer`** — called once, to obtain a composed
  `OutboundParkerResponse` or a structural rejection.
- **`ResponseDelivery`** — called once, only when composition succeeds,
  to deliver the composed response.

**Explicitly not a dependency, and explicitly prohibited:**
`IdentityService`, `ExecutionPipeline`, `PermissionEngine`,
`ReasoningProvider`, `ResourceRegistry`, `ToolRegistry`, `PlannerRuntime`,
`MemoryStore`, `WorldModel`. This is the Unit's primary structural
enforcement mechanism, mirroring `CommunicationConversationCoordinator`'s
own identical framing: the constructor has exactly two parameters, and
because both of its actual dependencies already carry their own
structural guarantees (`ResponseComposer` per the companion Plan's
Section 4, Section 4d; `ResponseDelivery` per its own existing
Contract Design), this coordinator inherits those guarantees
transitively — there is nothing to call because there is nothing
reachable, at any depth, to call it with.

## 4a. Statelessness Invariant

The coordinator holds exactly its two constructor-injected dependencies as
its only fields — no `var`, no mutable collection, no cache of any prior
`InboundOwnerMessage`, `ReasoningProviderResponse`, `OutboundParkerResponse`,
or `ExecutionResult`, and no `Mutex`. Each call to its one public method is
fully independent of every other call. Enforced structurally in Section 7
(a reflective test), mirroring `CommunicationConversationCoordinator`'s
own identical, already-accepted invariant.

## 4b. No-Construction, No-Mutation Invariant

**This coordinator never constructs an `OutboundParkerResponse` and never
mutates or reinterprets anything either dependency returns.** It passes
`originalMessage` and `reasoningResponse` unchanged into
`ResponseComposer.compose`, and — on `Produced` — passes
`composed.value` (the exact `OutboundParkerResponse` `ResponseComposer`
itself returned) unchanged into `ResponseDelivery.deliver`. It never
calls the `OutboundParkerResponse` constructor, never calls `.copy()` on
it, never adds, removes, or alters any `metadata` entry, and never
reads or branches on `response.text`. This is stronger than the
precedent's own Message Pass-Through Invariant in one respect: this
coordinator does not even have a type it is capable of constructing —
`OutboundParkerResponse` has no public constructor call anywhere in this
Unit's own code, which is itself part of what "construct nothing" means
structurally (Section 4, absence of any field-populating logic).

**Verification note (Correction 2):** this invariant is verified by
direct code review of the five-line method body (Section 10) — no
`OutboundParkerResponse` constructor call, no `.copy()` call, no
`metadata` mutation appears anywhere in it — not by a runtime
reference-identity test. `ResponseComposer` and `ResponseDelivery` are
both concrete, non-interface-backed types passed through directly, with
nothing available to intercept or capture the argument value between the
two calls; Section 7's own tests verify the observable *values* that
result (via `FakeResourceRegistry`/`FakeExecutionPipeline`), not object
identity.

## 4c. Exception Propagation Invariant

**This coordinator must not recover from, translate, retry, or suppress
an exception thrown by either dependency.** Such failures propagate
unchanged to the caller, identical in reasoning to
`CommunicationConversationCoordinator`'s own Exception Propagation
Invariant: `GatedOutcome.NotAccepted` is reserved for the one
structural, expected outcome each dependency already defines for itself
(`ResponseComposer`'s "not a Reply"; `ResponseDelivery`'s "no channel
resource found" / "ambiguous resource" / etc.) — never for catching and
repackaging a thrown exception. No `try`/`catch` exists anywhere in this
class.

## 4d. Sequencing-Only Invariant (No Business Logic)

**`ReplyDeliveryCoordinator` sequences two calls and nothing else. It
does not reason, plan, authorise, resolve identity, retry, or make any
decision beyond the single branch named in Section 1, step 3.** Stated
explicitly, mirroring the companion Plan's own Composition-Only
Invariant, one layer further out:

- **Never resolves identity.** No `IdentityService` dependency exists
  (Required Analysis, item 7). Sender identity was already resolved
  inside `ResponseComposer`, and this coordinator never re-touches it.
- **Never retries.** Each dependency is called exactly once per
  invocation, unconditionally — restated as an acceptance criterion
  (Section 8) and enforced by test (Section 7).
- **Never routes `Goal`/`NoAction` beyond propagating `ResponseComposer`'s
  own `NotAccepted`.** This coordinator does not itself inspect
  `reasoningResponse`'s variant, does not construct its own
  `NotAccepted` reason string, and does not distinguish "was a Goal" from
  "was NoAction" from "was a Reply that failed delivery" — it only
  branches on `GatedOutcome`'s own two cases (`Produced`/`NotAccepted`),
  whichever dependency produced them.
- **Never modifies response content or adds metadata.** Restated from
  Section 4b — this is a business-logic exclusion as much as a
  structural one: adding metadata (a delivery timestamp, a routing tag)
  would be a genuine design decision this Unit has no authority to make
  silently.
- **Never owns mutable state.** Restated from Section 4a.
- **Never depends directly on `ExecutionPipeline`, `PermissionEngine`,
  `ReasoningProvider`, or `IdentityService`.** Restated from Section 4 —
  each of these is reachable only indirectly, through `ResponseComposer`
  or `ResponseDelivery`'s own already-approved internals, never directly
  from this coordinator.

**Enforced structurally, not merely stated:** the constructor dependency
list (Section 4) has no slot for any of the above. A dedicated
constructor-shape test (Section 7) makes this a compile-time property.

## 5. Required Implementation Decisions

### Decision 1 — Return type: `GatedOutcome<ExecutionResult>`, returned unchanged from `ResponseDelivery`, no additional wrapping

Unlike `CommunicationConversationCoordinator` (whose second dependency
returns a plain `ReasoningProviderResponse`, requiring that coordinator to
wrap it in `GatedOutcome.Produced` itself), this Unit's second dependency,
`ResponseDelivery.deliver`, **already returns a
`GatedOutcome<ExecutionResult>`.** Wrapping it again
(`GatedOutcome.Produced(responseDelivery.deliver(...))`) would produce a
`GatedOutcome<GatedOutcome<ExecutionResult>>` — a nonsensical double
layer no test or caller needs, and a new shape not authorised anywhere.

**Proposed default:** `composeAndDeliver` returns
`GatedOutcome<ExecutionResult>` directly, and on the `Produced` branch,
returns exactly `responseDelivery.deliver(response)`'s own result,
unchanged — whether that result is itself `Produced` or `NotAccepted`.
On the `NotAccepted` branch from `ResponseComposer`, this coordinator
returns that exact `GatedOutcome.NotAccepted` value.

**Why the identical `NotAccepted` value type-checks as
`GatedOutcome<ExecutionResult>` without any conversion.** `GatedOutcome<T>`
is declared `sealed class GatedOutcome<out T>`, and `NotAccepted` is
declared `data class NotAccepted(val reason: String) : GatedOutcome<Nothing>()`
(`src/runtime/GatedOutcome.kt`). Because `T` is declared covariant
(`out T`) and `Nothing` is Kotlin's bottom type — a subtype of every
type, including `ExecutionResult` — `GatedOutcome<Nothing>` is itself a
subtype of `GatedOutcome<ExecutionResult>`. The exact `NotAccepted`
instance `ResponseComposer.compose` returned can therefore be returned
directly from `composeAndDeliver`, with the same `reason` string,
without constructing a new `NotAccepted`, without any cast, and without
any `as` or `@Suppress`. This is not a coincidence of this Unit's
design — it is precisely what `GatedOutcome<T>`'s own documented "suitable
for reuse by future coordinators with the same gating semantics" design
intent was for, exercised here for the second time (the first being the
companion Plan's own Decision 4).

### Decision 2 — Testing technique, corrected: verify via each dependency's own existing interface-backed seams, not object-reference identity

Neither `ResponseComposer` nor `ResponseDelivery` is interface-backed
(both are small, concrete, non-`open` classes). Kotlin's type system
therefore does not permit a substitutable call-counting Fake for either
directly — the technique `CommunicationConversationCoordinatorTest` used
for its own two, interface-backed dependencies
(`FakeCommunicationIntake`, `FakeReasoningProvider`) does not apply to
`ResponseComposer` or `ResponseDelivery` themselves, and this Unit does
not make either class `open`, does not introduce an interface for either,
does not add a testing-only production abstraction to either, and does
not modify `ResponseDelivery` — all explicitly out of this Unit's
authority.

**Corrected default: test with real instances of both, verified through
each one's own already-existing, already interface-backed constructor
dependencies — a legitimate, already-established seam this repository
already relies on, not a new testing abstraction this Unit invents:**

- **`ResponseComposer`, observed via `FakeIdentityService` (companion
  Plan, Section 3).** Per the companion Plan's own Third revision note —
  a correction made alongside this one — `IdentityService.resolve` is
  now called only inside `compose`'s `Reply` branch, exactly once, and
  not at all for `Goal`/`NoAction`. This Unit's own tests reflect that
  corrected behaviour, not the earlier "resolve is called on every
  invocation" assumption: `fakeIdentityService.resolveCallCount == 1`
  when `reasoningResponse` is `Reply` (regardless of whether
  `ResponseDelivery` subsequently accepts or rejects); `resolveCallCount
  == 0` when it is `Goal` or `NoAction`.
- **`ResponseDelivery`, observed via `FakeResourceRegistry` and
  `FakeExecutionPipeline` — the exact seam `ResponseDeliveryTest.kt`
  itself already uses for `ResponseDelivery`'s own isolated tests.**
  `ResourceRegistry` and `ExecutionPipeline` are both interface-backed,
  and `ResponseDelivery`'s own constructor already accepts them as
  interface types (`src/runtime/ResponseDelivery.kt`) — a real
  `ResponseDelivery` can therefore be constructed with
  `FakeResourceRegistry`/`FakeExecutionPipeline` with no modification to
  `ResponseDelivery` itself. This gives three genuine, already-typed
  observation points, not an inferred proxy:
  - **`resources.listByOwnerCallCount`** — how many times
    `ResponseDelivery.deliver` actually began executing (its first line
    calls `resourceRegistry.listByOwner`), independent of whether it
    goes on to reject or succeed.
  - **`pipeline.submitCallCount`** — how many times `deliver` reached the
    point of submitting an `ExecutionRequest`, i.e. successfully resolved
    exactly one channel Resource.
  - **`pipeline.lastSubmittedRequest`** — the actual `ExecutionRequest`
    `ResponseDelivery` constructed from the `OutboundParkerResponse` it
    received, exposing `principalId` (== `response.senderPrincipalId`),
    `correlationId` (== `response.correlationId.value`), and
    `metadata[RESPONSE_TEXT_METADATA_KEY]` (== `response.text`), per
    `ResponseDelivery.kt`'s own as-built construction. **This is the
    honest replacement for the withdrawn reference-identity claim:**
    this Unit's tests do not, and cannot, prove that the exact
    `OutboundParkerResponse` object instance reached `ResponseDelivery`
    — they prove that the *values* `ResponseComposer` produced (sender
    identity, correlation, text) are the same values that reached
    `ExecutionPipeline`, by inspecting the one place those values are
    next observable. No claim of object-reference identity is made
    anywhere in this Unit's tests (Section 7).

**One additional, separate, real end-to-end test is retained**
(mirroring the companion Plan's own compatibility test): the full real
`InMemoryResourceRegistry`/`InMemoryToolRegistry`/`InMemoryModuleRegistry`/
`InMemoryToolInvocationBinding`/`InMemoryEventBus`/`FakePermissionEngine`/
`DefaultExecutionPipeline`/real `LocalTextChannelDeliverTool` stack, with
its owner-notification callback. **Corrected scope of this test's own
claim:** it proves one thing only — on the successful, `Produced`/
`SUCCESS` path, the exact `text` ultimately reaches the owner, unchanged,
through fully real production wiring. **It does not prove, and this
document no longer claims it proves, that `ResponseDelivery.deliver` was
invoked exactly once on every possible outcome.** A callback that never
fires is equally consistent with "delivery was never entered" (the
`NotAccepted`-from-`ResponseComposer` case) and "delivery was entered and
itself rejected before reaching the tool" (the
`NotAccepted`-from-`ResponseDelivery` case, e.g. no matching channel
Resource) — the callback alone cannot distinguish the two. The
`FakeResourceRegistry`/`FakeExecutionPipeline` stack above is what
distinguishes them, via `listByOwnerCallCount` and `submitCallCount`
separately, and is the primary technique; the real end-to-end test is a
single, additional, narrower-scoped confirmation, not the source of any
call-count claim.

This is named here as an explicit Required Implementation Decision, not
silently assumed, because it corrects two things at once: the earlier,
unproven reference-identity and unconditional-callback claims (both
withdrawn here), and an implicit dependency on `ResponseComposer`'s
pre-correction resolve-every-branch behaviour, which no longer holds
after the companion Plan's own Third revision note.

### Decision 3 — Shape and name: a small, non-interface-backed, standalone class, retaining the name `ReplyDeliveryCoordinator`

Mirrors `CommunicationConversationCoordinator`/`ConversationTurnReasoningCoordinator`'s
own identical, already-accepted precedent. The name `ReplyDeliveryCoordinator`
is retained, per the Reconciliation Addendum's own explicit instruction,
for this redefined component — it is a coordinator in the same structural
sense `CommunicationConversationCoordinator` is: it sequences two
already-shaped components and introduces no new field, no new domain
concept, and no state of its own. **If a future caller needs to depend on
this behaviour abstractly, that need must be met by a later Contract
Design pass or an explicit, disclosed additive-interface decision — not
by silently promoting this Unit's own concrete class into a public
contract.**

### Decision 4 — Method name: `composeAndDeliver`

Mirrors the precedent's own naming convention
(`submitAndReason`, `submitTurnAndReason`) — a verb-and-verb name
describing exactly the two calls the method makes, in order, and nothing
more. **Proposed default:** `suspend fun composeAndDeliver(originalMessage:
InboundOwnerMessage, reasoningResponse: ReasoningProviderResponse):
GatedOutcome<ExecutionResult>`.

## 6. EventBus Implications

**None.** Neither dependency publishes to or subscribes from `EventBus`
on this Unit's behalf (Required Analysis, item 8), and this coordinator
itself publishes nothing.

## 7. Testing Strategy

**`ReplyDeliveryCoordinatorTest.kt`:**

**Primary fixture (Decision 2, corrected).** A real `ResponseComposer`
built from a `FakeIdentityService` (companion Plan, Section 3), and a
real `ResponseDelivery` built from a `FakeResourceRegistry` and a
`FakeExecutionPipeline` (the exact fixtures `ResponseDeliveryTest.kt`
already uses). Every test below uses this fixture unless stated
otherwise.

- **Produced path (Reply, successful delivery).** With
  `fakeIdentityService` configured to return the
  `system.response-composer` `Principal`, and `fakeResourceRegistry`
  configured to return exactly one matching `TOOL` `Resource`: for a
  `Reply("hello, owner")` and a matching `InboundOwnerMessage`,
  `composeAndDeliver` returns `GatedOutcome.Produced` carrying the
  `ExecutionResult` `fakeExecutionPipeline` was configured to return, and
  `pipeline.lastSubmittedRequest` carries `principalId ==
  PrincipalId("system.response-composer")`, `correlationId ==
  originalMessage.correlationId.value`, and
  `metadata[RESPONSE_TEXT_METADATA_KEY] == "hello, owner"` — the honest,
  value-level proof (Decision 2) that the composed response's own fields
  reached `ExecutionPipeline` unchanged, without claiming object-reference
  identity.
- **NotAccepted path (Goal).** Given the same fixture but a
  `Goal("some goal")` response: `composeAndDeliver` returns
  `GatedOutcome.NotAccepted` with the identical reason string
  `ResponseComposer.compose` itself produced; `fakeIdentityService.resolveCallCount
  == 0` (companion Plan's Third revision note — `compose` never resolves
  identity for `Goal`); and `resources.listByOwnerCallCount == 0` /
  `pipeline.submitCallCount == 0` — the structural, call-counted proof
  that `ResponseDelivery` was never entered at all, not merely that its
  result was discarded.
- **NotAccepted path (NoAction).** Same shape of assertion, for
  `NoAction`, including `resolveCallCount == 0`,
  `listByOwnerCallCount == 0`, `submitCallCount == 0`.
- **NotAccepted path (delivery-level rejection).** Given a `Reply` that
  composes successfully (`resolveCallCount` reaches `1`), but
  `fakeResourceRegistry` configured to return no matching `Resource`:
  `composeAndDeliver` returns `GatedOutcome.NotAccepted` with
  `ResponseDelivery`'s own rejection reason ("no channel Resource
  found..."), unchanged; `resources.listByOwnerCallCount == 1` (delivery
  *was* entered) but `pipeline.submitCallCount == 0` (it never reached
  `ExecutionPipeline`) — the call-counted distinction between "never
  entered `ResponseDelivery`" (the `Goal`/`NoAction` cases above) and
  "entered `ResponseDelivery` but was itself rejected" (this case),
  which an owner-notification callback alone cannot make (Decision 2).
- **No construction, no mutation (Section 4b).** Verified by direct code
  review of the five-line method body (Section 10), per Section 4b's own
  Verification note — no runtime reference-identity assertion is made.
- **Exactly-once invocation, no retries, per-branch counts (Decision
  2).** For a single `composeAndDeliver` call: `resolveCallCount == 1`
  when `reasoningResponse` is `Reply`, `0` otherwise;
  `listByOwnerCallCount == 1` whenever `ResponseComposer` produces
  (i.e. on `Reply`), `0` otherwise; `submitCallCount == 1` only when a
  matching Resource is found, `0` otherwise. No count exceeds `1` on any
  branch, for any dependency, in any test.
- **Exception propagation, not recovery (Section 4c).** With
  `fakeIdentityService` configured to throw from `resolve`: the
  coordinator's own method call is asserted to propagate that exact
  exception, via `assertFailsWith`, not to return any `GatedOutcome`
  variant, and `resources.listByOwnerCallCount` / `pipeline.submitCallCount`
  both remain `0` — `ResponseDelivery` is never reached.
- **Structural / negative test.** `ReplyDeliveryCoordinator`'s own
  constructor accepts only a `ResponseComposer` and a `ResponseDelivery`
  — no dependency slot exists for `IdentityService`, `ExecutionPipeline`,
  `PermissionEngine`, `ReasoningProvider`, `ResourceRegistry`,
  `ToolRegistry`, `PlannerRuntime`, `MemoryStore`, or `WorldModel` to
  even construct the fixture with one. A compile-time property, mirroring
  `ConversationTurnReasoningCoordinatorTest.kt`'s own identical structural
  test.
- **Statelessness test (Section 4a).** A reflective test asserting
  `ReplyDeliveryCoordinator::class.java.declaredFields` contains exactly
  its two constructor-injected dependencies and nothing else.
- **Real end-to-end test, narrower scope (Decision 2, corrected).**
  Mirroring the companion Plan's own compatibility test: a full real
  `InMemoryResourceRegistry`/`InMemoryToolRegistry`/`InMemoryModuleRegistry`/
  `InMemoryToolInvocationBinding`/`InMemoryEventBus`/`FakePermissionEngine`/
  `DefaultExecutionPipeline`/real `LocalTextChannelDeliverTool` stack,
  plus a real `InMemoryIdentityService` with `system.response-composer`
  registered. For a `Reply`, asserts the owner-notification callback
  receives exactly the reply text, unchanged, through fully real
  production wiring. **This test's claim is scoped to the successful
  path only** — it does not assert anything about `deliver` invocation
  counts on rejection paths (those are covered by the primary Fake-based
  fixture above, which alone can distinguish "never entered" from
  "entered and rejected").

**Full Gradle test suite.** Run the complete suite once implementation is
complete (of both this Unit and its companion) and report a real,
Android-Studio-verified result, per this program's own established
disclosure discipline (companion Plan, Section 7, Section 12).

## 8. Acceptance Criteria

- `ReplyDeliveryCoordinator` exists, is independently constructible with
  only a `ResponseComposer` and a `ResponseDelivery`, and its own tests
  (Section 7) pass.
- **A `Reply` that composes and delivers successfully results in
  `GatedOutcome.Produced` carrying `ResponseDelivery`'s own
  `ExecutionResult`, unchanged.**
- **A `Goal`, `NoAction`, or any `ResponseComposer`-level rejection never
  reaches `ResponseDelivery`** — verified structurally (Section 7), not
  merely by inspection.
- **A `ResponseDelivery`-level rejection (e.g. no matching channel
  Resource) is returned unchanged**, exactly as `ResponseComposer`-level
  rejections are — no asymmetry between the two sources of
  `NotAccepted`.
- The coordinator constructs no `OutboundParkerResponse` and mutates
  neither dependency's input or output (Section 4b) — verified
  structurally.
- The coordinator is stateless (Section 4a), holds no `IdentityService`
  dependency, and resolves no identity of its own (Section 4d) —
  verified structurally.
- **Exactly one call to each dependency per invocation on the applicable
  branch — no retries, unconditionally** (Section 4d, Decision 2) —
  verified by the call-counting technique in Section 7.
- The coordinator never recovers from, translates, retries, or suppresses
  a thrown exception (Section 4c) — verified by test.
- No production (`src/`) code added by this Unit references
  `IdentityService`, `ExecutionPipeline`, `PermissionEngine`,
  `ReasoningProvider`, `ResourceRegistry`, `ToolRegistry`,
  `PlannerRuntime`, `MemoryStore`, or `WorldModel`, anywhere — verified
  by the dependency list in Section 4 and the structural test in
  Section 7.
- No existing `src/` or `tests/` file, and no companion-Unit file, is
  modified (Section 3).
- All tests listed in Section 7 pass, and the full Gradle suite passes
  (or a projected count is honestly reported).

## 9. Implementation Boundaries — Out of Scope

- **Constructing `OutboundParkerResponse`.** Never performed by this
  Unit — restated from Section 4b; this is `ResponseComposer`'s job
  alone.
- **Resolving identity of any kind.** No `IdentityService` dependency
  exists (Section 4, Section 4d, Required Analysis item 7).
- **Direct dependency on `ExecutionPipeline`.** Reachable only indirectly,
  inside `ResponseDelivery`'s own already-approved internals.
- **Direct dependency on `PermissionEngine`.** Same reasoning.
- **Dependency on `ReasoningProvider`.** This Unit consumes whatever
  `ReasoningProviderResponse` it is given as an input parameter; it does
  not produce one and holds no dependency capable of producing one.
- **Routing `Goal`/`NoAction` beyond propagating `ResponseComposer`'s own
  `NotAccepted`.** Restated from Section 4d — this Unit does not itself
  distinguish which variant caused a rejection.
- **Modifying response content or adding metadata.** Restated from
  Section 4b/4d.
- **Retry policy of any kind.** Restated from Section 4d; each dependency
  is called exactly once, unconditionally.
- **Exception handling of any kind around either dependency.** No
  `try`/`catch`, no fallback value, no logging-and-continuing (Section
  4c).
- **Owning mutable state of any kind.** Restated from Section 4a.
- **Introducing a new public contract or result type.** This Unit's
  return type is the existing `GatedOutcome<ExecutionResult>`, already
  `ResponseDelivery.deliver`'s own return type (Decision 1) — nothing new
  is introduced.
- **Modifying `ResponseComposer`, `ResponseDelivery`, or `GatedOutcome`.**
  All three are reused exactly as they exist (or are companion-planned) —
  restating "do not redesign completed architecture."
- **A production composition root.** No production startup path is added
  by this Unit to construct a real `ReplyDeliveryCoordinator` for
  general use — its own tests construct it directly, exactly as every
  prior Sprint 7–9 coordinator's tests did.

## 10. Proposed Signature

```kotlin
package parker.core.runtime

import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.contracts.ExecutionResult

/**
 * Sequences [ResponseComposer] and [ResponseDelivery]. Introduces no
 * business logic of its own -- see
 * `docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`.
 */
class ReplyDeliveryCoordinator(
    private val responseComposer: ResponseComposer,
    private val responseDelivery: ResponseDelivery,
) {

    suspend fun composeAndDeliver(
        originalMessage: InboundOwnerMessage,
        reasoningResponse: ReasoningProviderResponse,
    ): GatedOutcome<ExecutionResult> {
        val composed = responseComposer.compose(originalMessage, reasoningResponse)
        return when (composed) {
            is GatedOutcome.NotAccepted -> composed
            is GatedOutcome.Produced -> responseDelivery.deliver(composed.value)
        }
    }
}
```

Exactly five lines of logic in the method body, matching Section 1's
five-step responsibility list one for one. `composed` on the
`NotAccepted` branch type-checks as `GatedOutcome<ExecutionResult>`
without conversion, per Decision 1's covariance argument.

## 11. Architectural Traceability

| Implemented element | Authorised by |
| --- | --- |
| `ReplyDeliveryCoordinator` calling `ResponseComposer.compose` first, `ResponseDelivery.deliver` only on `Produced` | This document, Section 1; structurally identical to `CommunicationConversationCoordinator.submitAndReason`'s own precedent |
| Returning `ResponseDelivery.deliver`'s own `GatedOutcome<ExecutionResult>` unchanged, no additional wrapping | This document, Section 5 Decision 1, exercising `GatedOutcome<T>`'s covariance |
| No `IdentityService` dependency; no identity resolution performed by this Unit | This document, Section 4, Section 4d; `RECONCILIATION_ADDENDUM_RESPONSE_COMPOSER_VS_REPLY_DELIVERY_COORDINATOR.md`, the `IMPLEMENTATION_GAPS.md` #49 lesson |
| Exactly two constructor dependencies (`ResponseComposer`, `ResponseDelivery`) | This document, Section 4; Reconciliation Addendum, Option C |
| Non-interface-backed shape, name retained as `ReplyDeliveryCoordinator` | This document, Section 5 Decision 3; Reconciliation Addendum's explicit naming instruction |
| Testing via each dependency's own existing interface-backed seams (`FakeIdentityService`, `FakeResourceRegistry`, `FakeExecutionPipeline`), value-level not reference-identity verification | This document, Section 5 Decision 2 (corrected) — reuses `ResponseDeliveryTest.kt`'s own already-established fixtures rather than inventing a new testing abstraction |
| Statelessness invariant | Section 4a, mirroring `CommunicationConversationCoordinator`'s identical invariant |
| No-construction, no-mutation invariant | Section 4b |
| Exception propagation invariant | Section 4c |
| Sequencing-only invariant (no business logic) | Section 4d |
| Withdrawal of the original identity-constant design | Reconciliation Addendum, Option C (Status section, above) |

## 12. Self-Traceability Review (Stage 9 Preview)

Stated in advance, as the companion Plan's Section 14 does, so Scope Lock
can confirm the plan itself is reviewable before any code is written:

- Every test in `ReplyDeliveryCoordinatorTest.kt` (Section 7) must trace
  to exactly one invariant or decision in Sections 4a–4d or 5.
- The final `ReplyDeliveryCoordinator.kt` constructor must match Section
  4's dependency list exactly (two parameters: `ResponseComposer`,
  `ResponseDelivery`) — any additional parameter is a Scope Lock
  violation.
- The final `composeAndDeliver` signature and five-line body must match
  Section 10's proposed signature, or any deviation must be disclosed
  and justified against Sections 1–9.
- No reference to `IdentityService`, `ExecutionPipeline`,
  `PermissionEngine`, `ReasoningProvider`, `ResourceRegistry`,
  `ToolRegistry`, `PlannerRuntime`, `MemoryStore`, or `WorldModel` may
  appear anywhere in `src/runtime/ReplyDeliveryCoordinator.kt` —
  grep-verifiable.
- No call to the `OutboundParkerResponse` constructor or `.copy()` method
  may appear anywhere in `src/runtime/ReplyDeliveryCoordinator.kt` —
  grep-verifiable, the literal check for Section 4b.
- No test in `ReplyDeliveryCoordinatorTest.kt` may assert object-reference
  equality (`assertSame`) on an `OutboundParkerResponse` — value-level
  assertions against `pipeline.lastSubmittedRequest`'s fields are the
  only sanctioned technique for this Unit (Decision 2, corrected;
  Section 4b's Verification note).

## 13. Documentation Updates (Planned, Not Performed by This Document)

Identical discipline to the companion Plan's Section 15. After verified
implementation of **both** Units (this Unit depends on the companion for
its own correctness, so neither's `IMPLEMENTATION_HISTORY.md` entry is
meaningful in isolation) only:

- `IMPLEMENTATION_HISTORY.md` **may** be updated, recording both Units as
  delivered together — files added, tests added, a real Gradle result.
- `IMPLEMENTATION_GAPS.md` #53 **may be closed for item 1 in full** —
  restating the companion Plan's own Section 15: with this Unit
  implemented, an accepted `InboundOwnerMessage` can now reach a real
  `ResponseDelivery` call, end to end, through one, real, tested,
  production code path (once a production composition root exists to
  construct and register these components — itself still out of scope
  for both Units, Section 9). Any closing update should state plainly
  which two files (`ResponseComposer.kt`, `ReplyDeliveryCoordinator.kt`)
  closed the item, and should not overstate closure of any other #53
  item (Goal/Planner routing; production composition root; `ReasoningContext`
  assembly ownership; the untested live HTTP path all remain open).
- No architecture or Contract Design document is modified at any point
  during either Unit's implementation.
- The withdrawn original `ReplyDeliveryCoordinator` design and the
  governance documents that produced it are not migrated, referenced as
  authoritative, or otherwise acted on by this documentation update.

## 14. Implementation Sequence

1. **Precondition: the companion Unit (`ResponseComposer`) is Scope
   Locked, implemented, and its own tests pass first.** This Unit's own
   tests (Section 7) construct a real `ResponseComposer` — it must exist.
2. Implement `ReplyDeliveryCoordinator` per Section 10's proposed
   signature.
3. Implement `ReplyDeliveryCoordinatorTest.kt`, in the order listed in
   Section 7: Produced path, NotAccepted-from-Goal,
   NotAccepted-from-NoAction, NotAccepted-from-delivery-rejection,
   no-construction/no-mutation (code review), exactly-once/per-branch
   counts, exception propagation, structural, statelessness, then the
   real end-to-end test last (Decision 2, corrected — narrower-scoped,
   successful path only).
4. Run the full Gradle suite locally if possible; otherwise report a
   static, honestly-disclosed projected count.
5. Hand off to Steven for verification in Android Studio, mirroring the
   companion Plan's own Section 12 Verification Procedure exactly (run
   the full suite; confirm each test individually; compare real vs.
   projected counts).
6. Only after a real, verified pass of both Units: apply the
   Documentation Updates (Section 13).

This sequence does not begin until an explicit Scope Lock instruction is
given for this Unit specifically (Section 16) — it is documented in
advance, not performed now.

## 15. Completion Criteria

- `IMPLEMENTATION_GAPS.md` and `IMPLEMENTATION_HISTORY.md` are **not**
  touched by this Plan, and must not be touched during implementation
  until every test in Section 7 passes and the full Gradle suite result
  is known for both Units.
- After verified implementation of both Units: the Documentation Updates
  in Section 13 may proceed.
- No architecture or Contract Design document is modified at any point
  during this Unit's implementation.

## 16. Scope Lock

**Not yet locked.** A separate, explicit human instruction ("Scope Lock
has been achieved") is required before any Kotlin is written against
this Plan, independent of whatever Scope Lock decision is made for the
companion Plan — the two are related but separately locked, since this
Unit's own implementation cannot begin until the companion is not just
locked but actually implemented (Section 14, step 1).

**What becomes frozen once locked:** exactly the file list in Section 3,
the dependency list in Section 4, the four invariants in Sections
4a–4d, the four Required Implementation Decisions in Section 5, the
testing strategy in Section 7, the Out-of-Scope list in Section 9, and
the proposed signature in Section 10. Any change to any of these after
Scope Lock requires a new planning pass.

## Conclusion

**This document defines one Stage 3 Implementation Plan for the smallest
safe unit that connects the companion `ResponseComposer` Unit to the
already-built `ResponseDelivery`.** No existing component is modified; a
single new, small, non-interface-backed class, `ReplyDeliveryCoordinator`,
is added, with exactly two dependencies (`ResponseComposer`,
`ResponseDelivery`) and one public method (`composeAndDeliver`),
returning `ResponseDelivery`'s own `GatedOutcome<ExecutionResult>`
unchanged — no new type, no additional wrapping, exploiting
`GatedOutcome<T>`'s own declared covariance to pass a `NotAccepted`
through untouched regardless of its source. This redefines, and does not
resurrect, the earlier withdrawn design: this coordinator holds no
`IdentityService` dependency, resolves no identity, and constructs no
`OutboundParkerResponse` — the exact defect class `IMPLEMENTATION_GAPS.md`
#49 already fixed once, and which the withdrawn design would have
reintroduced (Status section, above). The coordinator's statelessness
(Section 4a), its prohibition on constructing or mutating anything
(Section 4b), its refusal to recover from a thrown exception (Section
4c), and its sequencing-only discipline (Section 4d) are all stated as
explicit invariants, each enforced structurally by a dedicated test.
Verification relies on each dependency's own already-existing
interface-backed seams (`FakeIdentityService`, `FakeResourceRegistry`,
`FakeExecutionPipeline`) rather than object-reference identity, which
neither `ResponseComposer` nor `ResponseDelivery`'s concrete, non-open
shape permits; where a claim cannot be verified this precisely (whether
the exact `OutboundParkerResponse` instance crossed the boundary), this
Plan states so honestly and relies on direct code review of the
coordinator's own five-line body instead (Section 4b, Decision 2). This
Plan does not implement anything itself, does not modify any architecture
or Contract Design document, and does not touch `IMPLEMENTATION_GAPS.md`
or `IMPLEMENTATION_HISTORY.md`. It awaits an explicit Scope Lock
instruction — separate from, and subsequent to, the companion Plan's own
Scope Lock and implementation — before any Kotlin is written.

## Related

- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#49, #53)
- `docs/implementation/COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md` (direct structural precedent)
- `REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md` (companion Unit, this Plan's own upstream dependency)
- `RECONCILIATION_ADDENDUM_RESPONSE_COMPOSER_VS_REPLY_DELIVERY_COORDINATOR.md` (Option C, governing decision)
- `src/runtime/CommunicationConversationCoordinator.kt`, `src/runtime/ConversationTurnReasoningCoordinator.kt`
- `src/runtime/ResponseDelivery.kt`, `src/runtime/GatedOutcome.kt`
- `src/runtime/InMemoryPlannerRuntime.kt` (the `IMPLEMENTATION_GAPS.md` #49 fix precedent this Unit's own identity-free design avoids needing to repeat)
- `tests/runtime/ResponseDeliveryTest.kt`, `tests/runtime/FakeResourceRegistry.kt`, `tests/runtime/FakeExecutionPipeline.kt` (the existing seams Decision 2's corrected testing technique reuses unchanged)
