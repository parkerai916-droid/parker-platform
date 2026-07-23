# Conversation Reply Coordinator — Implementation Plan

## Status

**Stage 3 Implementation Plan, PES-001. Not yet Scope Locked.** No Kotlin
is written by this document.

**Naming deviation, disclosed.** The task that authorised this document
proposed the filename `CONVERSATION_RESPONSE_COORDINATOR_IMPLEMENTATION_PLAN.md`
(class name implied: `ConversationResponseCoordinator`), while explicitly
permitting a better name if the repository's own conventions support
one, with disclosure. This document instead uses **`ConversationReplyCoordinator`**
(`CONVERSATION_REPLY_COORDINATOR_IMPLEMENTATION_PLAN.md`), for two
reasons grounded directly in existing, already-accepted naming:

1. **"Reply" is this codebase's own established vocabulary for exactly
   this concept, and "Response" already means something more specific.**
   `ReasoningProviderResponse.Reply` (`REASONING_PROVIDER_CONTRACT_DESIGN.md`
   Section 3) is the sealed-type variant this coordinator's second call
   ultimately acts on; `ReplyDeliveryCoordinator` (Sprint 10, Unit 2) is
   already named for it. `OutboundParkerResponse` and `ResponseComposer`/
   `ResponseDelivery` already use "Response" for a narrower, later-stage
   concept (the composed, deliverable object) — naming this new class
   `ConversationResponseCoordinator` would sit one term away from three
   existing "Response"-named classes it does not construct, touch, or
   depend on directly, inviting exactly the kind of confusion
   `ReplyDeliveryCoordinator` was itself named to avoid (Reconciliation
   Addendum, Sprint 10 Unit 2 Plan Status section: the withdrawn earlier
   design was also going to be `ReplyDeliveryCoordinator`, and the name
   was deliberately retained through a full redesign specifically
   because it already meant the right thing).
2. **The existing chain-naming pattern names what each coordinator
   connects, not a generic function.** `CommunicationConversationCoordinator`
   connects Communication to Conversation; `ReplyDeliveryCoordinator`
   connects (compositionally) a Reply to Delivery. This class connects
   Conversation (via `CommunicationConversationCoordinator`, which itself
   already produces a `ReasoningProviderResponse`) to Reply (via
   `ReplyDeliveryCoordinator`, which already expects one).
   `ConversationReplyCoordinator` names exactly that link, consistent in
   kind with both neighbours it sits between in the chain.

No test is created by this document. `IMPLEMENTATION_HISTORY.md` and
`IMPLEMENTATION_GAPS.md` are not modified by this document. No commit or
push is performed by this document.

---

## Purpose

Connect the two coordinator chains that already exist and are already
independently verified, but are not yet wired to each other:
`CommunicationConversationCoordinator` (Sprint 7, Unit C2 — produces a
`GatedOutcome<ReasoningProviderResponse>` from an inbound message) and
`ReplyDeliveryCoordinator` (Sprint 10, Unit 2 — consumes an
`InboundOwnerMessage` plus a `ReasoningProviderResponse` and produces a
`GatedOutcome<ExecutionResult>`). Today, nothing in this repository calls
the second with the first's output — confirmed directly against both
classes' current source, not inferred from documentation (Governing
Documents, Review, below). This Plan closes exactly that gap, and no
other.

---

## Governing Documents

Reviewed directly, in authority order (Constitution > PES-001 > Accepted
ADRs > Contract Design > Implementation Plans):

1. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage 3
   (Implementation Plan) purpose ("break architectural work into
   independently verifiable implementation units... included work,
   excluded work, dependencies, acceptance criteria, unit stop
   conditions"); Chapter 4's Level classification (this Unit is Level 2 —
   "runtime implementation; new behaviour" — not Level 3: it introduces
   no new runtime subsystem, no lifecycle redesign, no trust-model
   change, and no new public contract type).
2. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` — Section 8,
   "Cognition proposes. Trust authorises. Runtime executes," restated
   without exception for this Unit; Section 9's own disclosure that
   `ReasoningContext` assembly ownership is unassigned, unchanged by this
   Unit.
3. `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` — Section 2
   (`ReasoningContext` — "an ordered list of already-assembled context
   entries, each a plain prose string"), Section 5 (`ReasoningContext`'s
   ownership "is not assigned by this document," restated here as still
   true and still out of this Unit's own scope), Constitutional
   Boundaries (a Reasoning Provider proposes only).
4. `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` — Section 8
   ("`ResponseDelivery` does not construct an `OutboundParkerResponse`...
   a future, separately-scoped coordinator's job"), the architectural
   basis this Unit's own downstream dependency (`ReplyDeliveryCoordinator`)
   already implements; not reopened here.
5. `docs/architecture/IMPLEMENTATION_GAPS.md` #53 — the gap this Unit
   narrows (Gap #53 Impact, below), read in its current, live text (most
   recent update: Sprint 10, Unit 2), not from a prior summary.
6. `docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`
   — the direct structural precedent this Plan mirrors section-for-
   section (Required Analysis style, invariant naming, Decision
   numbering), and the source of `ReplyDeliveryCoordinator`'s own exact,
   as-built signature this Plan depends on.
7. `docs/implementation/REPLY_DELIVERY_COORDINATOR_SCOPE_LOCK.md` —
   confirms `ReplyDeliveryCoordinator`'s frozen, as-implemented signature
   and behaviour (Sections 6–10), which this Plan treats as fixed,
   already-verified (599/599) architecture, not something to redesign.
8. `docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md` — confirms
   `ResponseComposer`'s own frozen behaviour one layer further down
   (589/589 verified), relevant only in that it establishes the
   `disposition.message` (rather than a caller's raw local reference)
   pass-through discipline this Plan's own Sequencing Behaviour section
   discusses.
9. **The actual current source**, read directly for this Plan, not
   assumed from any prior document:
   - `src/runtime/CommunicationConversationCoordinator.kt` — confirmed
     constructor `(communicationIntake: CommunicationIntake,
     conversationTurnReasoningCoordinator: ConversationTurnReasoningCoordinator)`;
     one method, `suspend fun submitAndReason(message: InboundOwnerMessage,
     reasoningContext: ReasoningContext): GatedOutcome<ReasoningProviderResponse>`.
   - `src/runtime/ReplyDeliveryCoordinator.kt` — confirmed constructor
     `(responseComposer: ResponseComposer, responseDelivery: ResponseDelivery)`;
     one method, `suspend fun composeAndDeliver(originalMessage:
     InboundOwnerMessage, reasoningResponse: ReasoningProviderResponse):
     GatedOutcome<ExecutionResult>`.
   - `src/runtime/GatedOutcome.kt` — confirmed `sealed class GatedOutcome<out T>`,
     `Produced<out T>(val value: T)`, `NotAccepted(val reason: String) : GatedOutcome<Nothing>()`.
     The `out T` covariance is load-bearing for this Unit's own return
     behaviour (Decision 1, below), exercised here for a third time (the
     first being `ReplyDeliveryCoordinator`'s own Decision 1, the second
     being `CommunicationConversationCoordinator`'s own use of `Produced`).
   - `src/interfaces/ReasoningProvider.kt` — confirmed
     `data class ReasoningContext(val entries: List<String>)`, and the
     `ReasoningProviderResponse` sealed type (`Goal`, `Reply`, `NoAction`).
   - `src/interfaces/CommunicationIntake.kt` — confirmed
     `InboundOwnerMessage`'s field shape, unchanged, reused here.
   - `src/runtime/InMemoryCommunicationIntake.kt` — confirmed the only
     real (non-test-fixture) `CommunicationIntake` implementation in this
     repository always returns `CommunicationIntakeDisposition.Accepted(
     correlationId = message.correlationId, message = message)` — the
     exact same `message` reference it was given, never a mutated or
     substituted one (Sequencing Behaviour, below).
   - `tests/runtime/CommunicationConversationCoordinatorTest.kt` — read in
     full; confirms `CommunicationConversationCoordinator`'s own
     Message-Pass-Through Invariant test deliberately exercises a
     `FakeCommunicationIntake` returning a **different** accepted message
     than its input, to prove the coordinator uses `disposition.message`
     internally — this is the source of the dependency limitation this
     Plan discloses explicitly (Sequencing Behaviour, below), not a
     newly-discovered defect.

Cross-checked directly against this source; nothing below is inferred
from a prior session's summary alone.

---

## Included Work

- One new, small, standalone, non-interface-backed coordinator
  (`ConversationReplyCoordinator`) with exactly one public method,
  `submitAndDeliver`, performing exactly the five-step sequence in
  Required Unit Purpose (the task authorising this Plan) and restated in
  Sequencing Behaviour, below.
- Unit tests for the coordinator, per Test Strategy, below.

## Excluded Work

Restated from the task's own Architectural Boundaries, verbatim in kind:

- Constructing a `ReasoningContext` — this Unit continues to accept one
  as a parameter, passed through unchanged; it does not assemble one.
- Any reasoning or planning of any kind.
- Inspecting or routing `Goal` — this Unit does not itself distinguish
  `Goal`/`Reply`/`NoAction`; it passes whatever
  `ReplyDeliveryCoordinator.composeAndDeliver`'s own second parameter
  needs through unchanged, and that class (via `ResponseComposer`) is
  what already, structurally, terminates `Goal`/`NoAction` as
  `NotAccepted`.
- Formatting a response, or constructing/mutating an
  `OutboundParkerResponse` — remains `ResponseComposer`'s job alone, two
  layers down, unreachable directly from this class.
- Performing delivery, or invoking `ExecutionPipeline` directly —
  reachable only indirectly, inside `ReplyDeliveryCoordinator`'s and
  `ResponseDelivery`'s own already-approved internals.
- Performing authorisation, or invoking `PermissionEngine` — same
  reasoning.
- Introducing transport logic of any kind.
- **A production composition root.** No production startup path is
  added by this Unit to register modules, Tools, vocabulary entries, or
  to construct real (non-test-fixture) instances of anything for general
  use — its own tests construct it directly, exactly as every
  Sprint 7–10 coordinator's tests already do.
- **Selecting or configuring a concrete model provider.** This Unit
  holds no dependency on `ReasoningProvider`, `ModelReasoningProvider`,
  or `ModelInferenceClient`, at any depth reachable directly from it.
- **Validating or changing live HTTP behaviour.** Entirely unrelated to
  this Unit; `LocalHttpModelInferenceClient` is not referenced anywhere
  in this Plan's own proposed signature.

## Proposed Production File

| File | New/Modified | Contents |
| --- | --- | --- |
| `src/runtime/ConversationReplyCoordinator.kt` | New | The class described in Proposed Class and Method Signature, below. |

No existing `src/` file is modified. No new file is added under
`src/interfaces/` — this Unit introduces no public contract type; its
return type, `GatedOutcome<ExecutionResult>`, is already
`ReplyDeliveryCoordinator.composeAndDeliver`'s own return type, reused
verbatim.

## Proposed Test File

| File | New/Modified | Contents |
| --- | --- | --- |
| `tests/runtime/ConversationReplyCoordinatorTest.kt` | New | Tests per Test Strategy, below, built on existing fixtures only. |

No existing `tests/` file is modified. **This Unit adds no new test
fixture of its own** — see Test Strategy for the exact existing seams it
reuses.

## Dependencies

**Exactly two, constructor-injected, both already-existing, concrete,
unmodified types — confirmed unavoidable and sufficient by direct
inspection of both classes' current signatures, per the task's own
instruction to stop and report rather than expand the unit if a third
proved necessary. No third dependency was found necessary:**

- **`CommunicationConversationCoordinator`** — called once, to obtain
  either a structural rejection or a `ReasoningProviderResponse`.
- **`ReplyDeliveryCoordinator`** — called once, only when the first call
  succeeds, to compose and deliver.

**Explicitly not a dependency, and explicitly prohibited:**
`CommunicationIntake`, `ConversationTurnReasoningCoordinator`,
`ConversationEngine`, `ReasoningProvider`, `ResponseComposer`,
`ResponseDelivery`, `IdentityService`, `ExecutionPipeline`,
`PermissionEngine`, `ResourceRegistry`, `ToolRegistry`, `PlannerRuntime`,
`MemoryStore`, `WorldModel`. Every one of these is already reachable only
indirectly, inside this Unit's own two dependencies' already-approved
internals — this Unit does not need, and must not gain, a shortcut past
either of them.

## Proposed Class and Method Signature

```kotlin
package parker.core.runtime

import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ReasoningContext

/**
 * Sequences [CommunicationConversationCoordinator] and
 * [ReplyDeliveryCoordinator]. Introduces no business logic of its own --
 * see `docs/implementation/CONVERSATION_REPLY_COORDINATOR_IMPLEMENTATION_PLAN.md`.
 */
class ConversationReplyCoordinator(
    private val communicationConversationCoordinator: CommunicationConversationCoordinator,
    private val replyDeliveryCoordinator: ReplyDeliveryCoordinator,
) {

    suspend fun submitAndDeliver(
        message: InboundOwnerMessage,
        reasoningContext: ReasoningContext,
    ): GatedOutcome<ExecutionResult> {
        val reasoned = communicationConversationCoordinator.submitAndReason(message, reasoningContext)
        return when (reasoned) {
            is GatedOutcome.NotAccepted -> reasoned
            is GatedOutcome.Produced -> replyDeliveryCoordinator.composeAndDeliver(message, reasoned.value)
        }
    }
}
```

**Method name, per this program's own established verb-and-verb
convention** (`submitAndReason`, `submitTurnAndReason`,
`composeAndDeliver`): `submitAndDeliver` — names the first call
(`submit`, mirroring `CommunicationConversationCoordinator.submitAndReason`'s
own leading verb) and the final outcome (`Deliver`, mirroring
`ReplyDeliveryCoordinator.composeAndDeliver`'s own trailing verb),
exactly five lines of logic in the body, matching Required Unit Purpose's
five-step list one for one.

## Dependencies (`GatedOutcome` Return-Type Note)

Restated once, precisely, since it recurs at every layer of this chain:
`composed`/`reasoned` on the `NotAccepted` branch type-checks as
`GatedOutcome<ExecutionResult>` without conversion, because `GatedOutcome<T>`
is declared covariant (`out T`) and `NotAccepted` is declared
`GatedOutcome<Nothing>` — `Nothing` being a subtype of every type,
including `ExecutionResult`. No new `NotAccepted` is constructed by this
class; the exact instance `CommunicationConversationCoordinator.submitAndReason`
itself returned is returned onward unchanged.

## Sequencing Behaviour

1. **Invocation.** A caller supplies an `InboundOwnerMessage` and an
   already-assembled `ReasoningContext` — this Unit does not assemble
   one (Excluded Work).
2. `communicationConversationCoordinator.submitAndReason(message,
   reasoningContext)` is called **exactly once, first**, on every
   invocation.
3. If the result is `GatedOutcome.NotAccepted`: it is returned
   **unchanged** — `ReplyDeliveryCoordinator.composeAndDeliver` is
   **never** called on this branch.
4. If the result is `GatedOutcome.Produced(reasoningProviderResponse)`:
   `replyDeliveryCoordinator.composeAndDeliver(message,
   reasoningProviderResponse)` is called **exactly once**, and its own
   `GatedOutcome<ExecutionResult>` is returned **unchanged**, whatever it
   is (`Produced` or `NotAccepted`).

**Disclosed dependency limitation: this class passes its own `message`
parameter, not `CommunicationIntake`'s own accepted-disposition
message, to `ReplyDeliveryCoordinator`.**
`CommunicationConversationCoordinator.submitAndReason`'s own return type,
`GatedOutcome<ReasoningProviderResponse>`, does not expose
`disposition.message` (the value `CommunicationIntake` itself accepted)
back to its caller — only the reasoning outcome. This class therefore has
no choice but to forward its own `message` parameter onward, unchanged,
into `composeAndDeliver`'s `originalMessage` argument. This is not a
mutation and not a new decision this Plan invents — it is a structural
consequence of `CommunicationConversationCoordinator`'s own, already
Scope-Locked (Sprint 7, Unit C2) signature, which this Plan is not
authorised to reopen. Confirmed directly against the only real,
production-shaped `CommunicationIntake` implementation in this
repository, `InMemoryCommunicationIntake`, whose `Accepted` disposition
always carries `message = message` — the identical reference it was
given, never a substituted one — this limitation has no observable
effect against any concrete implementation that exists in this
repository today. `CommunicationConversationCoordinatorTest.kt`'s own
Message Pass-Through Invariant test exercises a **fake** intake that
deliberately returns a *different* message, solely to prove that
coordinator's own internal discipline in isolation — it does not
describe `InMemoryCommunicationIntake`'s real behaviour. If a future
`CommunicationIntake` implementation is ever introduced that legitimately
substitutes a different accepted message, this class's own behaviour
would need re-examination at that time — named here as a disclosed,
narrow, non-blocking limitation, not silently assumed away.

## Branch Behaviour

| `communicationConversationCoordinator.submitAndReason(...)` result | `replyDeliveryCoordinator.composeAndDeliver` called? | Result |
| --- | --- | --- |
| `GatedOutcome.NotAccepted(reason)` | No | Returned unchanged — same `reason`, no new instance constructed |
| `GatedOutcome.Produced(reasoningProviderResponse)` | Yes, exactly once | `replyDeliveryCoordinator.composeAndDeliver(message, reasoningProviderResponse)`'s own result returned unchanged — `Produced(executionResult)` or `NotAccepted(reason)`, whichever the downstream chain itself produces |

No other branch, variant, or fallback exists. This class does not itself
inspect which `ReasoningProviderResponse` variant (`Goal`/`Reply`/
`NoAction`) was produced — that distinction is made two layers down,
inside `ResponseComposer`, unchanged.

## Exception Behaviour

- **This class must not recover from, translate, retry, or suppress an
  exception thrown by either dependency.** Such failures propagate
  unchanged to the caller.
- `GatedOutcome.NotAccepted` is reserved exclusively for the structural
  outcomes each dependency already defines for itself — never for
  catching and repackaging a thrown exception.
- No `try`/`catch` exists anywhere in `ConversationReplyCoordinator`.

## No-Construction / No-Mutation Invariant

**This coordinator never constructs an `InboundOwnerMessage`,
`ReasoningContext`, `ReasoningProviderResponse`, `OutboundParkerResponse`,
or `ExecutionResult`, and never mutates or reinterprets anything either
dependency returns.** It passes `message` and `reasoningContext`
unchanged into `submitAndReason`, and — on `Produced` — passes `message`
(unchanged, subject to the disclosed limitation above) and
`reasoned.value` (the exact `ReasoningProviderResponse` value
`CommunicationConversationCoordinator` itself returned) unchanged into
`composeAndDeliver`. It never calls any data class's constructor other
than what is structurally required to satisfy Kotlin's `when` exhaustiveness
over `GatedOutcome`'s own two cases, and never calls `.copy()` on
anything. Verified by direct code review of the five-line method body
(Proposed Class and Method Signature, above) — no runtime
reference-identity assertion is required or made (Test Strategy, below,
mirrors `ReplyDeliveryCoordinator`'s own established precedent of relying
on code review for this specific invariant, per that Unit's own Scope
Lock Section 9).

## Test Strategy

`tests/runtime/ConversationReplyCoordinatorTest.kt`. **Primary fixture:**
a real `CommunicationConversationCoordinator` built from
`FakeCommunicationIntake` and a real `ConversationTurnReasoningCoordinator`
(itself built from a pass-through `ConversationEngine` fake and
`FakeReasoningProvider` — the exact fixture combination
`CommunicationConversationCoordinatorTest.kt` already uses for its own
isolated tests), and a real `ReplyDeliveryCoordinator` built from a real
`ResponseComposer` (via `FakeIdentityService`) and a real
`ResponseDelivery` (via `FakeResourceRegistry`/`FakeExecutionPipeline`) —
the exact fixture combination `ReplyDeliveryCoordinatorTest.kt` already
uses. **No new fake is introduced; every seam below already exists and
is already committed.**

1. **`NotAccepted` from `CommunicationConversationCoordinator`.** With
   `FakeCommunicationIntake` configured to reject: `submitAndDeliver`
   returns `GatedOutcome.NotAccepted` carrying the identical rejection
   reason; `ReplyDeliveryCoordinator`'s own dependencies
   (`FakeIdentityService`, `FakeResourceRegistry`, `FakeExecutionPipeline`)
   all show zero call counts — `ReplyDeliveryCoordinator.composeAndDeliver`
   was never entered at all.
2. **`Produced` flowing into `ReplyDeliveryCoordinator` (successful
   end-to-end composition and delivery).** With `FakeCommunicationIntake`
   accepting and `FakeReasoningProvider` returning `Reply("hello,
   owner")`: `submitAndDeliver` returns `GatedOutcome.Produced` carrying
   `ResponseDelivery`'s own `ExecutionResult`; `pipeline.lastSubmittedRequest`
   carries the composed response's fields (mirroring
   `ReplyDeliveryCoordinatorTest.kt`'s own equivalent assertion).
3. **Downstream `NotAccepted` (from `ResponseComposer`, via a `Goal` or
   `NoAction`).** With `FakeReasoningProvider` returning `Goal(...)` or
   `NoAction`: `submitAndDeliver` returns the identical `NotAccepted`
   `ResponseComposer` itself produced, naming the variant;
   `FakeIdentityService.resolveCallCount == 0`;
   `FakeResourceRegistry.listByOwnerCallCount == 0`;
   `FakeExecutionPipeline.submitCallCount == 0`.
4. **Downstream `NotAccepted` (from `ResponseDelivery`, e.g. no matching
   channel Resource).** With `FakeReasoningProvider` returning a `Reply`
   but `FakeResourceRegistry` configured to return no matching Resource:
   `submitAndDeliver` returns `ResponseDelivery`'s own rejection reason
   unchanged; `FakeIdentityService.resolveCallCount == 1` (composition
   succeeded) but `FakeExecutionPipeline.submitCallCount == 0` — the
   call-counted distinction between "never entered delivery" (item 3) and
   "entered delivery but was itself rejected" (this item).
5. **Exact call counts across sequential calls of different outcomes.**
   Sequential `Reply` / `Goal` / `NoAction` / `Reply` calls, asserting
   `FakeCommunicationIntake.submitInboundMessageCallCount`,
   `FakeReasoningProvider.reasonCallCount`,
   `FakeIdentityService.resolveCallCount`,
   `FakeResourceRegistry.listByOwnerCallCount`, and
   `FakeExecutionPipeline.submitCallCount` all increment only on their
   own applicable branch — mirroring both
   `CommunicationConversationCoordinatorTest.kt`'s and
   `ReplyDeliveryCoordinatorTest.kt`'s own identical per-branch-count
   tests, one layer further out.
6. **Call ordering / sequencing evidence.** `FakeCommunicationIntake`'s
   own call count is asserted to have already reached `1` before
   `FakeReasoningProvider`'s call count becomes nonzero is not directly
   observable via counts alone; ordering evidence is instead established
   by asserting `FakeReasoningProvider`'s `reasonCallCount == 0` on a
   rejected-intake test (item 1, above) combined with `== 1` only after a
   successful call — the same technique
   `CommunicationConversationCoordinatorTest.kt`'s own "a rejected
   message... never reaches reasoning" test already uses to prove
   ordering without a dedicated sequencing-log fixture.
7. **Exception propagation from `CommunicationConversationCoordinator`
   (i.e. from its own first dependency, `CommunicationIntake`).** With
   `FakeCommunicationIntake` configured to throw:
   `assertFailsWith<IllegalStateException>` around `submitAndDeliver`;
   every `ReplyDeliveryCoordinator`-side call count remains `0`.
8. **Exception propagation from `ReplyDeliveryCoordinator` (i.e. from its
   own dependencies, once reached).** With `FakeReasoningProvider`
   returning a `Reply` but `FakeIdentityService` configured to throw from
   `resolve`: `assertFailsWith<IllegalStateException>` around
   `submitAndDeliver`; `FakeResourceRegistry`/`FakeExecutionPipeline`
   call counts remain `0`.
9. **Structural / negative test.** The constructor accepts only a
   `CommunicationConversationCoordinator` and a `ReplyDeliveryCoordinator`
   — no dependency slot exists for any prohibited type (Dependencies,
   above). Mirrors both neighbouring coordinators' own identical
   structural tests.
10. **Statelessness test.** A reflective test asserting
    `ConversationReplyCoordinator::class.java.declaredFields` contains
    exactly its two constructor-injected dependencies and nothing else —
    no companion object exists on this class, so no `Modifier.isStatic`
    filtering is required (unlike `ResponseComposer`'s own precedent);
    the simpler `declaredFields.map { it.name }.toSet()` technique
    `ReplyDeliveryCoordinator`'s and `ResponseDelivery`'s own equivalent
    tests already use is sufficient.
11. **Real end-to-end test, narrower scope.** Mirroring the two
    predecessor coordinators' own compatibility tests one layer further
    out: the full real `InMemoryCommunicationIntake`/
    `InMemoryConversationEngine`/`ModelReasoningProvider`-or-
    `FakeReasoningProvider`/`InMemoryResourceRegistry`/`InMemoryToolRegistry`/
    `InMemoryModuleRegistry`/`InMemoryToolInvocationBinding`/
    `InMemoryEventBus`/`FakePermissionEngine`/`DefaultExecutionPipeline`/
    real `LocalTextChannelDeliverTool`/real `InMemoryIdentityService`
    stack, for a `Reply`: the owner-notification callback receives
    exactly the reply text, unchanged, through one single
    `submitAndDeliver` call. **This test's own use of `ReasoningProvider`
    should be `FakeReasoningProvider`, not `ModelReasoningProvider`** —
    wiring a real model-backed provider into this Unit's own test would
    reintroduce the untested live HTTP dependency this Plan's Excluded
    Work explicitly keeps out of scope; `FakeReasoningProvider` is the
    correct, already-established choice for proving wiring correctness
    without depending on a live model server.

Full Gradle test suite run once implementation is complete; a static,
honestly-disclosed projected count if the sandbox cannot resolve the
Kotlin Gradle plugin, pending Steven's Android Studio verification.

## Acceptance Criteria

- `ConversationReplyCoordinator` exists, is independently constructible
  with only a `CommunicationConversationCoordinator` and a
  `ReplyDeliveryCoordinator`, and its own tests (Test Strategy, above)
  pass.
- An accepted message whose reasoning produces a `Reply`, which composes
  and delivers successfully, results in `GatedOutcome.Produced` carrying
  `ResponseDelivery`'s own `ExecutionResult`, unchanged.
- A rejected message, a `Goal`, a `NoAction`, or any downstream
  `ResponseComposer`- or `ResponseDelivery`-level rejection is returned
  as `GatedOutcome.NotAccepted` unchanged, with no asymmetry between the
  four sources of rejection.
- The coordinator constructs no data-carrying type of its own and
  mutates nothing it is given — verified structurally (code review).
- The coordinator is stateless, holds no dependency beyond the two
  named, and resolves no identity, composes no response, and delivers
  nothing directly — verified structurally.
- Exactly one call to each of the two direct dependencies per invocation
  on the applicable branch — no retries, unconditionally — verified by
  the call-counting technique in Test Strategy.
- The coordinator never recovers from, translates, retries, or
  suppresses a thrown exception — verified by test.
- No production (`src/`) code added by this Unit references
  `CommunicationIntake`, `ConversationTurnReasoningCoordinator`,
  `ConversationEngine`, `ReasoningProvider`, `ResponseComposer`,
  `ResponseDelivery`, `IdentityService`, `ExecutionPipeline`,
  `PermissionEngine`, `ResourceRegistry`, `ToolRegistry`,
  `PlannerRuntime`, `MemoryStore`, or `WorldModel`, anywhere.
- No existing `src/` or `tests/` file is modified.
- All tests listed in Test Strategy pass, and the full Gradle suite
  passes (or a projected count is honestly reported).

## Documentation Follow-up

**Not performed by this document — explicitly deferred, per this
program's own established discipline.**

- `IMPLEMENTATION_HISTORY.md` **may** be updated after verified
  implementation, recording this Unit exactly as delivered (files added,
  tests added, a real Gradle result) — not before.
- `IMPLEMENTATION_GAPS.md` #53 **may be narrowed further, not closed**,
  after verified implementation — restated precisely from Gap #53
  Impact, below: this Unit closes only the coordinator-to-coordinator
  sequencing gap; any documentation update must say so explicitly, not
  merely by omission.
- No architecture or Contract Design document is modified at any point
  during this Unit's implementation.
- Neither `IMPLEMENTATION_GAPS.md` nor `IMPLEMENTATION_HISTORY.md` is
  touched by this Plan document itself.

## Gap #53 Impact

**This Unit closes only the missing coordinator-to-coordinator
sequencing gap between `CommunicationConversationCoordinator` and
`ReplyDeliveryCoordinator`.** Precisely, and no more:

- Before this Unit: nothing in this repository calls
  `ReplyDeliveryCoordinator.composeAndDeliver` with
  `CommunicationConversationCoordinator`'s own output. After: one real,
  tested, production-shaped Kotlin code path connects an inbound message
  all the way through reasoning, composition, and delivery.

**This Unit does not close, and must not be described as closing:**

- **Production composition root.** No real startup registration of
  modules, Tools, or vocabulary entries, and no real (non-test-fixture)
  construction of any component for general use, is added by this Unit.
  `ConversationReplyCoordinator`'s own tests construct it directly,
  exactly as every predecessor coordinator's tests do.
- **`Goal` / Planner Runtime routing.** Entirely untouched — a `Goal`
  continues to terminate as `NotAccepted`, unchanged from today's
  behaviour, one layer further removed from this new class.
- **`ReasoningContext` ownership.** Entirely untouched — this Unit
  continues to accept one as a parameter, exactly as
  `CommunicationConversationCoordinator` already does; it does not
  assemble one and does not decide where a production value comes from.
- **`LocalHttpModelInferenceClient` live HTTP validation.** Entirely
  unrelated — this Unit holds no dependency capable of reaching it.

## Preconditions

1. **`CommunicationConversationCoordinator` (Sprint 7, Unit C2) and
   `ReplyDeliveryCoordinator` (Sprint 10, Unit 2) both already exist,
   are both already Scope Locked, implemented, and verified** (519/519
   and 599/599 respectively, at their own time of verification) — both
   fully satisfied today, confirmed directly against the current
   repository, unlike either of those Units' own precondition state at
   the time each was first planned.
2. No other precondition exists. This Unit does not require
   `ReasoningContext` assembly, `Goal` routing, or the live HTTP path to
   be resolved first (Dependencies, above; Gap #53 Impact, above).

## Stop Conditions

This Plan does not authorise, and a future Scope Lock drawn from it must
not authorise:

- Writing any Kotlin file not listed in Proposed Production File and
  Proposed Test File, above.
- Modifying any existing `src/` or `tests/` file.
- Introducing a third constructor dependency.
- Any production composition root, real startup registration, model
  provider selection, or live HTTP validation (Excluded Work, above).
- Assembling a `ReasoningContext` inside this class.
- Inspecting or routing `Goal` inside this class.
- Any deviation from the signature (Proposed Class and Method
  Signature), sequencing behaviour, or branch behaviour frozen above,
  without a new planning pass.
- Updating `IMPLEMENTATION_GAPS.md` or `IMPLEMENTATION_HISTORY.md`.
- Any commit, tag, or push.

If implementation reveals that any element above cannot be satisfied as
written, work stops and returns to Steven for a new decision — it is not
silently resolved during implementation.

---

## Conclusion

**This document defines one Stage 3 Implementation Plan for the smallest
safe unit that connects the two already-verified coordinator chains
currently sitting unconnected in this repository:
`CommunicationConversationCoordinator` and `ReplyDeliveryCoordinator`.**
No existing component is modified; a single new, small,
non-interface-backed class, `ConversationReplyCoordinator`, is added,
with exactly two dependencies and one public method
(`submitAndDeliver`), returning the downstream chain's own
`GatedOutcome<ExecutionResult>` unchanged — no new type, no additional
wrapping, exploiting `GatedOutcome<T>`'s own declared covariance a third
time in this same lineage. One dependency limitation is disclosed
precisely, not silently assumed away: this class cannot access
`CommunicationIntake`'s own accepted-disposition message (only its own
input parameter), a structural consequence of an already-Scope-Locked
upstream signature, confirmed to have no observable effect against the
only real `CommunicationIntake` implementation that exists today. This
Unit closes only the coordinator-to-coordinator sequencing portion of
`IMPLEMENTATION_GAPS.md` #53 — a production composition root, `Goal`/
Planner Runtime routing, `ReasoningContext` ownership, and the live HTTP
path all remain exactly as open as they are today. This Plan does not
implement anything, does not modify any architecture or Contract Design
document, and does not touch `IMPLEMENTATION_GAPS.md` or
`IMPLEMENTATION_HISTORY.md`. It awaits Scope Lock.

**Ready for Governance Review**

## Related Documents

- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`
- `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#53)
- `docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md` (direct structural precedent)
- `docs/implementation/REPLY_DELIVERY_COORDINATOR_SCOPE_LOCK.md` (downstream dependency's frozen behaviour)
- `docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md` (two layers down, referenced for the message pass-through discipline)
- `docs/implementation/COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md` (upstream dependency's own governing Plan)
- `src/runtime/CommunicationConversationCoordinator.kt`, `src/runtime/ReplyDeliveryCoordinator.kt`, `src/runtime/GatedOutcome.kt`
- `src/runtime/InMemoryCommunicationIntake.kt` (confirms the message pass-through limitation has no observable effect today)
- `tests/runtime/CommunicationConversationCoordinatorTest.kt`, `tests/runtime/ReplyDeliveryCoordinatorTest.kt` (the exact existing fixtures this Plan's own Test Strategy reuses unchanged)
