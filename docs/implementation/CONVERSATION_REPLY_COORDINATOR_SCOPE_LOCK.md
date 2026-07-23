# ConversationReplyCoordinator — Scope Lock

## Status

**Stage 5 Scope Lock, PES-001.** This document freezes
`docs/implementation/CONVERSATION_REPLY_COORDINATOR_IMPLEMENTATION_PLAN.md`
("the Plan") exactly as reviewed and accepted. It does not redesign, add
to, or narrow anything the Plan already decided — it restates the Plan's
own decisions as frozen scope, per PES-001's own Stage 3/Stage 5
relationship ("Implementation Decisions clarify approved architecture.
They do not replace or redefine it," applied here one level up: a Scope
Lock clarifies which of a Plan's own already-approved decisions are now
frozen; it does not relitigate them).

**Governance Review status.** The Sprint-following Governance Review of
this Plan concluded **Ready for Scope Lock**, with **no required
correction**. This document proceeds on that basis; it does not
re-derive the Plan's own reasoning, only freezes its output.

**This is not a place to resolve a future production composition
root.** Nothing about registering modules, Tools, or vocabulary entries,
selecting a `ReasoningProvider` implementation, configuring an endpoint,
or validating live HTTP behaviour is decided, narrowed, or hinted at by
this document. Those remain entirely for a future, separately-scoped
unit, exactly as the Plan itself already states.

No Kotlin is written by this document. No test is created by this
document. `IMPLEMENTATION_GAPS.md` and `IMPLEMENTATION_HISTORY.md` are
not modified by this document. No commit, tag, or push is performed by
this document.

---

## 1. Governing Documents

In order of authority, per this program's own established hierarchy
(Constitution > PES-001 > Accepted ADRs > Contract Design > Implementation
Plans > Sprint Handovers):

1. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage 3
   (Implementation Plan) and Stage 5 (Scope Lock) definitions; Chapter 4's
   Level 2 classification for this Unit (no new runtime subsystem, no
   lifecycle redesign, no trust-model change, no new public contract
   type).
2. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` — Section 8,
   "Cognition proposes. Trust authorises. Runtime executes," preserved
   without exception by this Unit; Section 9's disclosure that
   `ReasoningContext` assembly ownership is unassigned, unchanged here.
3. `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` — Section 2
   (`ReasoningContext`'s minimal, opaque shape), Section 5
   (`ReasoningContext` ownership "is not assigned by this document," and
   remains not assigned by this one either).
4. `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` — Section 8,
   the architectural basis `ReplyDeliveryCoordinator` (this Unit's own
   downstream dependency) already implements; not reopened here.
5. `docs/adr/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md` and
   `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md` —
   both Accepted; both load-bearing for `ResponseDelivery`'s own already-
   frozen internals, reached only transitively through
   `ReplyDeliveryCoordinator`, never directly, and not reopened by this
   Unit.
6. `docs/architecture/IMPLEMENTATION_GAPS.md` #53 — the gap this Unit
   narrows (Section 20, below) and no further.
7. `docs/implementation/REPLY_DELIVERY_COORDINATOR_SCOPE_LOCK.md` —
   confirms `ReplyDeliveryCoordinator`'s own frozen, as-implemented,
   verified (599/599) signature and behaviour, treated here as fixed
   architecture, not redesigned.
8. `docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md` — confirms
   `ResponseComposer`'s own frozen behaviour one further layer down
   (589/589 verified), relevant only to the message-forwarding
   discussion (Section 13, below).
9. **`docs/implementation/CONVERSATION_REPLY_COORDINATOR_IMPLEMENTATION_PLAN.md`
   itself, as reviewed and accepted — the direct source this Scope Lock
   freezes**, in full.
10. **The current implemented source**, re-confirmed unchanged at the
    time of this Scope Lock (no drift since the Plan or its Governance
    Review): `src/runtime/CommunicationConversationCoordinator.kt`,
    `src/runtime/ReplyDeliveryCoordinator.kt`, `src/runtime/GatedOutcome.kt`,
    `src/interfaces/ReasoningProvider.kt` (`ReasoningProviderResponse`,
    `ReasoningContext`), `src/interfaces/CommunicationIntake.kt`
    (`InboundOwnerMessage`), `src/runtime/InMemoryCommunicationIntake.kt`.

---

## 2. Included Work

Restated from the Plan's own Included Work, unchanged:

- One new, small, standalone, non-interface-backed coordinator,
  `ConversationReplyCoordinator`, with exactly one public method,
  `submitAndDeliver`, performing exactly the five-step sequence in
  Section 10, below.
- Unit tests for the coordinator, per Section 16, below.

## 3. Excluded Work

Restated from the Plan's own Excluded Work, unchanged:

- Constructing a `ReasoningContext`.
- Any reasoning or planning of any kind.
- Inspecting or routing `Goal`.
- Formatting a response, or constructing/mutating an
  `OutboundParkerResponse`.
- Performing delivery, or invoking `ExecutionPipeline` directly.
- Performing authorisation, or invoking `PermissionEngine`.
- Introducing transport logic of any kind.
- A production composition root of any kind.
- Selecting or configuring a concrete model provider.
- Validating or changing live HTTP behaviour.

## 4. Production File

**Frozen. No other production file may be added or modified under this
Scope Lock.**

| File | Status |
| --- | --- |
| `src/runtime/ConversationReplyCoordinator.kt` | New |

No existing `src/` file is modified. No new file is added under
`src/interfaces/` — this Unit introduces no public contract type; its
return type, `GatedOutcome<ExecutionResult>`, is already
`ReplyDeliveryCoordinator.composeAndDeliver`'s own return type, reused
verbatim.

## 5. Test File

**Frozen. No other test file may be added or modified under this Scope
Lock.**

| File | Status |
| --- | --- |
| `tests/runtime/ConversationReplyCoordinatorTest.kt` | New — built entirely on existing fixtures (`FakeCommunicationIntake`, `FakeReasoningProvider`, `FakeIdentityService`, `FakeResourceRegistry`, `FakeExecutionPipeline`), all pre-existing and unmodified. |

No existing `tests/` file is modified. **This Unit adds no new test
fixture of its own.**

## 6. Dependencies

**Frozen at exactly two, constructor-injected, both already-existing,
concrete, unmodified types:**

- **`CommunicationConversationCoordinator`** — called once, first, to
  obtain either a structural rejection or a `ReasoningProviderResponse`.
- **`ReplyDeliveryCoordinator`** — called once, only when the first call
  succeeds, to compose and deliver.

**No third dependency is authorised.** The following are explicitly
prohibited as direct constructor dependencies of
`ConversationReplyCoordinator` — every one of them remains reachable
only indirectly, inside the two named dependencies' own already-approved
internals:

`CommunicationIntake`, `ConversationTurnReasoningCoordinator`,
`ConversationEngine`, `ReasoningProvider`, `ResponseComposer`,
`ResponseDelivery`, `IdentityService`, `ExecutionPipeline`,
`PermissionEngine`, `PlannerRuntime`, `ModelReasoningProvider`,
`LocalHttpModelInferenceClient`, `MemoryStore`, `WorldModel`.

## 7. Frozen Class Name

**`ConversationReplyCoordinator`.** Per the Plan's own Status section:
consistent with this program's established chain-naming convention
(`CommunicationConversationCoordinator`, `ConversationTurnReasoningCoordinator`,
`ReplyDeliveryCoordinator` — each named for the two things it bridges),
and deliberately using "Reply" rather than "Response" to avoid sitting
one term away from `OutboundParkerResponse`/`ResponseComposer`/
`ResponseDelivery`, none of which this class constructs, touches, or
depends on directly. Any deviation from this name is a Scope Lock
violation requiring a new planning pass.

## 8. Frozen Constructor

```kotlin
class ConversationReplyCoordinator(
    private val communicationConversationCoordinator: CommunicationConversationCoordinator,
    private val replyDeliveryCoordinator: ReplyDeliveryCoordinator,
)
```

Exactly two `private val` constructor parameters, in this order, these
types, these names. No default value, no nullable type, no additional
parameter.

## 9. Frozen Method Name and Signature

```kotlin
suspend fun submitAndDeliver(
    message: InboundOwnerMessage,
    reasoningContext: ReasoningContext,
): GatedOutcome<ExecutionResult>
```

**Frozen elements specifically:** the method name (`submitAndDeliver` —
per the Plan's own verb-and-verb naming rationale: `submit`, mirroring
`CommunicationConversationCoordinator.submitAndReason`'s own leading
verb, and `Deliver`, mirroring `ReplyDeliveryCoordinator.composeAndDeliver`'s
own trailing verb); the parameter list (`message: InboundOwnerMessage`,
`reasoningContext: ReasoningContext`); the `suspend` modifier; and the
return type (`GatedOutcome<ExecutionResult>`). The full frozen body:

```kotlin
val reasoned = communicationConversationCoordinator.submitAndReason(message, reasoningContext)
return when (reasoned) {
    is GatedOutcome.NotAccepted -> reasoned
    is GatedOutcome.Produced -> replyDeliveryCoordinator.composeAndDeliver(message, reasoned.value)
}
```

Any deviation from this signature or body discovered during
implementation is a Scope Lock violation requiring a new planning pass,
not a silent adjustment.

## 10. Sequencing Behaviour

Frozen exactly as the Plan's own Sequencing Behaviour establishes:

1. Accept `InboundOwnerMessage` and `ReasoningContext` as input. This
   Unit does not assemble either.
2. Call `communicationConversationCoordinator.submitAndReason(message,
   reasoningContext)` **exactly once**, first, on every invocation.
3. If the result is `GatedOutcome.NotAccepted`: return it **unchanged**
   — `replyDeliveryCoordinator.composeAndDeliver` is **never** called on
   this branch.
4. If the result is `GatedOutcome.Produced(reasoningProviderResponse)`:
   call `replyDeliveryCoordinator.composeAndDeliver(message,
   reasoningProviderResponse)` **exactly once**, passing the original
   input message (Section 13, below) and the produced
   `ReasoningProviderResponse`.
5. Return the downstream `GatedOutcome<ExecutionResult>` **unchanged**,
   whatever it is (`Produced` or `NotAccepted`) — no additional
   wrapping, exploiting `GatedOutcome<T>`'s declared `out T` covariance
   exactly as both upstream dependencies already do.

## 11. Branch Behaviour

Frozen exactly as the Plan establishes:

| `communicationConversationCoordinator.submitAndReason(...)` result | `replyDeliveryCoordinator.composeAndDeliver` called? | Result |
| --- | --- | --- |
| `GatedOutcome.NotAccepted(reason)` | No | Returned unchanged — same `reason`, no new instance constructed |
| `GatedOutcome.Produced(reasoningProviderResponse)` | Yes, exactly once | `replyDeliveryCoordinator.composeAndDeliver(message, reasoningProviderResponse)`'s own result returned unchanged — `Produced(executionResult)` or `NotAccepted(reason)`, whichever the downstream chain itself produces |

No other branch, variant, or fallback exists. This class does not itself
inspect which `ReasoningProviderResponse` variant (`Goal`/`Reply`/
`NoAction`) was produced.

## 12. Exception Behaviour

Frozen exactly as the Plan establishes:

- This class must not recover from, translate, retry, or suppress an
  exception thrown by either dependency. Such failures propagate
  unchanged to the caller.
- `GatedOutcome.NotAccepted` is reserved exclusively for the structural
  outcomes each dependency already defines for itself — never for
  catching and repackaging a thrown exception.
- No `try`/`catch` exists anywhere in `ConversationReplyCoordinator`.

## 13. Message Forwarding Behaviour

**Frozen exactly as the Plan's own disclosed limitation establishes, not
redesigned here:**

- `ConversationReplyCoordinator` forwards its own original
  `InboundOwnerMessage` (the `message` parameter it was itself called
  with) into `replyDeliveryCoordinator.composeAndDeliver`'s
  `originalMessage` argument.
- **It does not attempt to recover, reconstruct, or otherwise obtain
  `CommunicationIntake`'s own accepted-disposition message.** This is
  necessary, not a design choice this Unit made freely:
  `CommunicationConversationCoordinator.submitAndReason`'s own return
  type, `GatedOutcome<ReasoningProviderResponse>`, does not expose that
  accepted message back to its caller — confirmed directly against the
  current, real source of `CommunicationConversationCoordinator.kt`.
- **The upstream return type is not redesigned by this Unit, or by this
  Scope Lock.** `CommunicationConversationCoordinator` remains exactly
  as Sprint 7, Unit C2 left it, Scope Locked and unmodified.
- **Recorded, confirmed empirically at Plan and Governance Review time
  and re-confirmed here: the current real `InMemoryCommunicationIntake`
  returns the identical `message` reference it receives** —
  `CommunicationIntakeDisposition.Accepted(correlationId =
  message.correlationId, message = message)`, per
  `src/runtime/InMemoryCommunicationIntake.kt`'s own current source —
  so this limitation has **no observable effect against any concrete
  implementation that exists in this repository today**.
  `CommunicationConversationCoordinatorTest.kt`'s own Message
  Pass-Through Invariant test exercises a fake intake that deliberately
  returns a *different* message solely to prove that coordinator's own
  internal discipline in isolation — it does not describe
  `InMemoryCommunicationIntake`'s real behaviour, and must not be read
  as evidence that today's production behaviour differs.
- If a future `CommunicationIntake` implementation is ever introduced
  that legitimately substitutes a different accepted message, this
  class's own behaviour would require re-examination at that time — a
  disclosed, narrow, non-blocking limitation, not silently assumed away,
  and not resolved by this Scope Lock.

## 14. No-Construction / No-Mutation Invariant

Frozen exactly as the Plan establishes:

`ConversationReplyCoordinator` never constructs an `InboundOwnerMessage`,
`ReasoningContext`, `ReasoningProviderResponse`, `OutboundParkerResponse`,
or `ExecutionResult`, and never mutates or reinterprets anything either
dependency returns. It passes `message` and `reasoningContext` unchanged
into `submitAndReason`, and — on `Produced` — passes `message` (Section
13, above) and `reasoned.value` (the exact `ReasoningProviderResponse`
value `CommunicationConversationCoordinator` itself returned) unchanged
into `composeAndDeliver`. No `.copy()` call exists anywhere in this
class. **Verified by direct code review of the five-line method body
(Section 9, above)** — no runtime reference-identity assertion is
required or made, mirroring `ReplyDeliveryCoordinator`'s own identical,
already-accepted precedent for this specific invariant.

## 15. Architectural Boundaries

Frozen exactly as the Plan establishes, restated explicitly per this
Scope Lock's own governing instruction to preserve this separation
without exception:

- **Communication intake remains upstream**, inside
  `CommunicationConversationCoordinator`'s own `CommunicationIntake`
  dependency — untouched, unreachable directly from this Unit.
- **Conversation reasoning remains inside
  `CommunicationConversationCoordinator`** — this Unit calls it whole,
  once, and does not re-implement or duplicate any part of its own
  `submitAndReason` -> `submitTurnAndReason` -> `reason` sequence.
- **Reply composition and delivery remain inside
  `ReplyDeliveryCoordinator`** — this Unit calls it whole, once, and
  does not re-implement or duplicate any part of its own
  `composeAndDeliver` -> `compose`/`deliver` sequence.
- **This new coordinator only sequences the two.** It holds no
  responsibility, business logic, or decision beyond the single branch
  in Section 11, above.

**The coordinator must not, and structurally cannot** (no dependency
slot exists — Section 6, above):

- Assemble `ReasoningContext`.
- Inspect or route `Goal`.
- Perform reasoning.
- Perform planning.
- Format output.
- Construct or mutate `OutboundParkerResponse`.
- Perform delivery.
- Invoke `ExecutionPipeline`.
- Perform authorisation.
- Select a model provider.
- Configure an endpoint.
- Validate live HTTP behaviour.
- Create a production composition root.

## 16. Test Strategy

Frozen exactly as the Plan establishes, for
`tests/runtime/ConversationReplyCoordinatorTest.kt`, using the primary
fixture (a real `CommunicationConversationCoordinator` built from
`FakeCommunicationIntake` and a real `ConversationTurnReasoningCoordinator`
built from a pass-through `ConversationEngine` fake and
`FakeReasoningProvider`; a real `ReplyDeliveryCoordinator` built from a
real `ResponseComposer` via `FakeIdentityService` and a real
`ResponseDelivery` via `FakeResourceRegistry`/`FakeExecutionPipeline`) —
**no new fake is authorised**:

1. Upstream `NotAccepted` — `submitAndDeliver` returns the identical
   rejection reason; every `ReplyDeliveryCoordinator`-side dependency
   shows zero call counts.
2. Upstream `Produced`, successful end-to-end composition and delivery —
   `submitAndDeliver` returns `Produced` carrying `ResponseDelivery`'s
   own `ExecutionResult`; the composed request's fields are verified at
   the value level.
3. Downstream `NotAccepted` (from `ResponseComposer`, via `Goal`/
   `NoAction`) — the identical `NotAccepted` `ResponseComposer` itself
   produced is returned; identity/resource/pipeline call counts all
   remain `0`.
4. Downstream `NotAccepted` (from `ResponseDelivery`, e.g. no matching
   channel Resource) — `ResponseDelivery`'s own rejection reason
   returned unchanged; identity resolution succeeded (`1`) but
   `submitCallCount` remains `0`.
5. Exact upstream and downstream call counts across sequential calls of
   different outcomes.
6. **Downstream not called on upstream rejection** — restated
   explicitly from item 1: this is the specific, named assertion that
   `ReplyDeliveryCoordinator`'s own dependencies are never touched when
   `CommunicationConversationCoordinator` itself rejects.
7. Sequencing evidence — established via the same technique
   `CommunicationConversationCoordinatorTest.kt` already uses: a
   rejected-intake test proves the downstream reasoning call count stays
   at `0`, combined with a successful test proving it reaches `1`, in
   place of a dedicated sequencing-log fixture.
8. Exception propagation from upstream (`CommunicationConversationCoordinator`'s
   own first dependency, `CommunicationIntake`) — propagates unchanged;
   every `ReplyDeliveryCoordinator`-side call count remains `0`.
9. Exception propagation from downstream (`ReplyDeliveryCoordinator`'s
   own dependencies, once reached) — propagates unchanged.
10. Constructor structure — accepts only a
    `CommunicationConversationCoordinator` and a `ReplyDeliveryCoordinator`.
11. Statelessness — `ConversationReplyCoordinator::class.java.declaredFields`
    contains exactly its two constructor-injected dependencies and
    nothing else; no companion object exists on this class, so no
    `Modifier.isStatic` filtering is required.
12. **One real compatibility/end-to-end test, using the existing
    fake-based pattern** — full real `InMemoryCommunicationIntake`/
    `InMemoryConversationEngine`/`InMemoryResourceRegistry`/
    `InMemoryToolRegistry`/`InMemoryModuleRegistry`/
    `InMemoryToolInvocationBinding`/`InMemoryEventBus`/
    `FakePermissionEngine`/`DefaultExecutionPipeline`/real
    `LocalTextChannelDeliverTool`/real `InMemoryIdentityService` stack,
    with **`FakeReasoningProvider`, not `ModelReasoningProvider`**, for
    the reasoning step — `ModelReasoningProvider` and a live model
    server are explicitly **not required** anywhere in this Unit's own
    test suite.

Full Gradle test suite run once implementation is complete; a static,
honestly-disclosed projected count if the sandbox cannot resolve the
Kotlin Gradle plugin, pending Steven's Android Studio verification
(Section 18, below).

## 17. Acceptance Criteria

Frozen exactly as the Plan establishes:

- `ConversationReplyCoordinator` exists, is independently constructible
  with only a `CommunicationConversationCoordinator` and a
  `ReplyDeliveryCoordinator`, and its own tests (Section 16, above) pass.
- An accepted message whose reasoning produces a `Reply`, which composes
  and delivers successfully, results in `GatedOutcome.Produced` carrying
  `ResponseDelivery`'s own `ExecutionResult`, unchanged.
- A rejected message, a `Goal`, a `NoAction`, or any downstream
  `ResponseComposer`- or `ResponseDelivery`-level rejection is returned
  as `GatedOutcome.NotAccepted` unchanged, with no asymmetry between the
  four sources of rejection.
- The coordinator constructs no data-carrying type of its own and
  mutates nothing it is given — verified structurally.
- The coordinator is stateless, holds no dependency beyond the two
  named, and resolves no identity, composes no response, and delivers
  nothing directly — verified structurally.
- Exactly one call to each of the two direct dependencies per invocation
  on the applicable branch — no retries, unconditionally.
- The coordinator never recovers from, translates, retries, or
  suppresses a thrown exception.
- No production (`src/`) code added by this Unit references any type
  named in Section 6's prohibited list, anywhere.
- No existing `src/` or `tests/` file is modified.
- All tests listed in Section 16 pass, and the full Gradle suite passes
  (or a projected count is honestly reported).

## 18. Verification Requirements

Restated from the established pattern
(`REPLY_DELIVERY_COORDINATOR_SCOPE_LOCK.md` Section 14;
`RESPONSE_COMPOSER_SCOPE_LOCK.md` Section 14), unchanged in kind:

1. Steven opens the `parker-platform` project in Android Studio.
2. Runs the full test suite (`./gradlew test` or equivalent) — not only
   `ConversationReplyCoordinatorTest.kt` — confirming no existing test
   regresses, including the 599 already-verified tests from the prior
   two Units.
3. Confirms each of the twelve tests in Section 16, above, passes
   individually.
4. Records the real pass/fail count and compares it against the static
   projection reported at implementation time, and against the
   pre-existing baseline (599 passing prior to this Unit); any
   discrepancy is itself a finding to report.
5. Only after a real, verified pass does Section 19 (Documentation
   Follow-up), below, become eligible to proceed.

## 19. Documentation Follow-up

Restated from the Plan, unchanged, and explicitly deferred — not
performed by this document:

- `IMPLEMENTATION_HISTORY.md` **may** be updated after verified
  implementation, recording this Unit exactly as delivered (files
  added, tests added, a real Gradle result) — not before.
- `IMPLEMENTATION_GAPS.md` #53 **may be narrowed further, not closed**,
  after verified implementation — restated precisely from Section 20,
  below; any documentation update must say so explicitly, not merely by
  omission.
- No architecture or Contract Design document is modified at any point
  during this Unit's implementation.
- Neither `IMPLEMENTATION_GAPS.md` nor `IMPLEMENTATION_HISTORY.md` is
  touched by this Scope Lock document itself.

## 20. Gap #53 Impact

**Frozen precisely, not to be broadened during or after
implementation:**

- **This Unit closes only the missing coordinator-to-coordinator
  sequencing gap** between `CommunicationConversationCoordinator` and
  `ReplyDeliveryCoordinator`. Before this Unit: nothing in this
  repository calls `ReplyDeliveryCoordinator.composeAndDeliver` with
  `CommunicationConversationCoordinator`'s own output. After: one real,
  tested, production-shaped Kotlin code path connects an inbound message
  through reasoning, composition, and delivery.
- **This Unit does not close, and must never be described as closing:**
  - **Production composition root** — no real startup registration of
    modules, Tools, or vocabulary entries, and no real construction of
    any component for general use, is added.
  - **`Goal` / Planner Runtime routing** — entirely untouched.
  - **`ReasoningContext` ownership** — entirely untouched; this Unit
    continues to accept one as a parameter, never assembling one.
  - **`LocalHttpModelInferenceClient` live HTTP validation** — entirely
    unrelated; this Unit holds no dependency capable of reaching it.

**Gap #53 is not marked closed by this Unit, in any respect.**

## 21. Stop Conditions

This Scope Lock does not authorise:

- Writing any Kotlin file not listed in Sections 4 and 5, above.
- Modifying any existing `src/` or `tests/` file.
- Modifying `REPLY_DELIVERY_COORDINATOR_SCOPE_LOCK.md`,
  `RESPONSE_COMPOSER_SCOPE_LOCK.md`, the Plan this document freezes, any
  architecture document, any ADR, or any Contract Design.
- Introducing a third constructor dependency (Section 6, above).
- Any production composition root, real startup registration, model
  provider selection, endpoint configuration, or live HTTP validation
  (Sections 3, 15, above).
- Assembling a `ReasoningContext`, or inspecting/routing `Goal`, inside
  this class.
- Redesigning `CommunicationConversationCoordinator.submitAndReason`'s
  own return type to expose `disposition.message` (Section 13, above) —
  that remains a separate, future decision, not authorised here.
- Updating `IMPLEMENTATION_GAPS.md` or `IMPLEMENTATION_HISTORY.md`.
- Any commit, tag, or push.
- Any deviation from the class name (Section 7), constructor (Section
  8), method signature (Section 9), sequencing behaviour (Section 10),
  branch behaviour (Section 11), exception behaviour (Section 12), or
  message-forwarding behaviour (Section 13) frozen above, without a new
  planning pass.

If implementation reveals that any frozen element above cannot be
satisfied as written, work stops and returns to Steven for a new
decision — it is not silently resolved during implementation.

---

## Conclusion

**This Scope Lock freezes `ConversationReplyCoordinator` exactly as
reviewed and accepted in
`docs/implementation/CONVERSATION_REPLY_COORDINATOR_IMPLEMENTATION_PLAN.md`,
following a Governance Review that concluded Ready for Scope Lock with
no required correction.** One production file
(`src/runtime/ConversationReplyCoordinator.kt`) and one test file
(`tests/runtime/ConversationReplyCoordinatorTest.kt`) are authorised,
both additions, no existing file modified, no new test fixture
introduced. The coordinator holds exactly two dependencies
(`CommunicationConversationCoordinator`, `ReplyDeliveryCoordinator`),
performs sequencing only, and preserves the architectural boundary
between communication intake, conversation reasoning, reply composition,
and delivery exactly as each already stands. The disclosed
message-forwarding limitation is frozen as documented, not resolved or
redesigned — this Unit does not touch `CommunicationConversationCoordinator`'s
own return type. This document closes no part of `IMPLEMENTATION_GAPS.md`
#53 itself; it authorises the work that, once implemented and verified,
narrows #53 by exactly the coordinator-sequencing item, and no more. This
document does not implement anything, does not create any test, does not
modify `IMPLEMENTATION_GAPS.md` or `IMPLEMENTATION_HISTORY.md`, and does
not commit, tag, or push.

**This Unit is frozen and ready for implementation.**

## Related Documents

- `docs/implementation/CONVERSATION_REPLY_COORDINATOR_IMPLEMENTATION_PLAN.md` (the Plan this document locks)
- `docs/implementation/REPLY_DELIVERY_COORDINATOR_SCOPE_LOCK.md` (downstream dependency's frozen behaviour)
- `docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md` (two layers down, referenced for the message pass-through discipline)
- `docs/implementation/COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md` (upstream dependency's own governing Plan)
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`
- `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
- `docs/adr/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`, `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#53)
- `src/runtime/CommunicationConversationCoordinator.kt`, `src/runtime/ReplyDeliveryCoordinator.kt`, `src/runtime/GatedOutcome.kt`
- `src/runtime/InMemoryCommunicationIntake.kt` (confirms the message-forwarding limitation has no observable effect today)
- `tests/runtime/CommunicationConversationCoordinatorTest.kt`, `tests/runtime/ReplyDeliveryCoordinatorTest.kt` (the exact existing fixtures this Unit's own Test Strategy reuses unchanged)
