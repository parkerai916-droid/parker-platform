# Reply to OutboundParkerResponse Construction — Implementation Plan (Sprint 9)

## Status

**Stage 3 Implementation Plan, PES-001. Not yet Scope Locked.** No Kotlin
is written by this document. This document performs PES-001's own Stage 3
breakdown — Included work, Excluded work, Dependencies, Acceptance
Criteria, Unit Stop Conditions — for one unit only.

**Revision note (post-review, pre-Scope-Lock).** The reviewing human
approved this Plan's technical content in full, subject to two changes:
(1) rename the component from `ReplyToOutboundResponseCoordinator` to
`ResponseComposer`, reflecting its actual, narrower responsibility; (2)
add an explicit invariant stating the component performs composition only
and never reasons, plans, authorises, or delivers. Applying (2) honestly
required more than a label: the prior draft's own Decision 5 had this
component call `ResponseDelivery.deliver` directly, which a "never
delivers" invariant would immediately contradict if left in place. This
revision removes `ResponseDelivery` as a dependency entirely, narrows the
return type from `GatedOutcome<ExecutionResult>` to
`GatedOutcome<OutboundParkerResponse>`, and moves the delivery call out of
this component and into a separate, test-only compatibility check
(Section 7) — so the stated invariant and the actual design agree. This
is a consequential correction, not a cosmetic rename; it is called out
explicitly here rather than folded in silently, per this campaign's own
established discipline for review-driven changes.

**Grounded exclusively in the as-built code and the documents that
authorised it**, all freshly re-read for this Sprint:

1. `src/interfaces/ReasoningProvider.kt` — as built (`ReasoningProviderResponse`
   sealed type: `Goal`, `Reply(text: String)`, `NoAction`).
2. `src/interfaces/ConversationEngine.kt` — as built (`Turn`, `Conversation`,
   `ConversationDisposition`).
3. `src/interfaces/CommunicationIntake.kt` — as built (`InboundOwnerMessage`,
   `OutboundParkerResponse`).
4. `src/runtime/ConversationTurnReasoningCoordinator.kt`,
   `src/runtime/CommunicationConversationCoordinator.kt` — as built, the two
   existing coordinators this Unit sits downstream of.
5. `src/runtime/ResponseDelivery.kt` — as built. No longer a dependency of
   this Unit's own component (Revision note, above) — read here only to
   confirm the exact `OutboundParkerResponse` shape it expects, for the
   compatibility test (Section 7).
6. `src/runtime/InMemoryConversationEngine.kt` — as built, the
   `CONVERSATION_ENGINE_PRINCIPAL_ID` / `IdentityService.resolve`
   operating-identity precedent this Unit's own Decision 3 follows.
7. `src/runtime/GatedOutcome.kt` — as built, reused unchanged by this Unit.
8. `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 3 —
   confirms `Reply.text` is "deliberately shaped to be directly usable as a
   future `OutboundParkerResponse.text`... once the calling component
   decides to route it there."
9. `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 8 —
   confirms constructing an `OutboundParkerResponse` from a `Reply` is
   "a future, separately-scoped coordinator's job — structurally the same
   'who builds this' question `CommunicationConversationCoordinator`'s own
   Contract Design and Implementation Plan already resolved for the
   inbound side... left unresolved here for the outbound side,
   deliberately." Section 11 Deferred Item 3 restates this identically.
   Note: this document names the "who builds this" job as *composition*
   only — Section 8 never says the same component must also invoke
   delivery, and this revision reads it that way deliberately.
10. `docs/implementation/COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md`
    — the direct structural precedent this document mirrors in full:
    a small, non-interface-backed coordinator sequencing already-approved
    contracts, proceeding straight to Stage 3 without a Stage 2A pass, and
    reusing `GatedOutcome<T>` rather than inventing a new result type.
11. `docs/architecture/IMPLEMENTATION_GAPS.md` #53, most recently updated
    (Sprint 8) — the gap this Unit narrows. Its own "What remains open"
    list names, first: "Constructing an `OutboundParkerResponse` from a
    `ReasoningProviderResponse.Reply` — the wiring that would actually call
    `ResponseDelivery` in response to a real conversation turn — remains
    unimplemented. Nothing in this repository calls `ResponseDelivery.deliver`
    from a real caller today." This Unit closes the *construction* half of
    that sentence; the *wiring that would actually call `ResponseDelivery`*
    remains a future, separately-scoped caller's job (Section 9).
12. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage
    2A's "Required when" text and Stage 4's "Implementation Decisions
    clarify approved architecture. They do not replace or redefine it,"
    both applied below.

---

## Required Analysis (Sprint 9 Steps 1–3: governance review, contracts, ADR/spec determination)

### 1. What exact contracts are involved?

Reused unchanged, all already Contract-Design-approved and Kotlin-built:

- `ReasoningProviderResponse` (sealed: `Goal`, `Reply(text: String)`,
  `NoAction`) — `src/interfaces/ReasoningProvider.kt`.
- `InboundOwnerMessage` (`channelId`, `senderPrincipalId`, `text`,
  `timestamp`, `correlationId`, `metadata`) — `src/interfaces/CommunicationIntake.kt`.
- `OutboundParkerResponse` (`channelId`, `senderPrincipalId`, `text`,
  `timestamp`, `correlationId`, `metadata`) — same file, already
  field-shaped, never modified by any Sprint since it was designed. This
  Unit's entire output.
- `IdentityService` — to resolve this Unit's own operating Principal,
  mirroring `InMemoryConversationEngine`'s `CONVERSATION_ENGINE_PRINCIPAL_ID`
  precedent exactly.
- `GatedOutcome<T>` (`src/runtime/GatedOutcome.kt`) — the existing generic
  admit/reject wrapper, reused as the return type (Decision 4, below) —
  already explicitly documented as "suitable for reuse by future
  coordinators with the same gating semantics."

**Named but explicitly not a dependency of this Unit's own component:**
`ResponseDelivery.deliver(response: OutboundParkerResponse): GatedOutcome<ExecutionResult>`
(`src/runtime/ResponseDelivery.kt`) — already built, already verified
541/541 end-to-end against the real `LocalTextChannelDeliverTool`
(Sprint 8). This Unit's output is shaped to be accepted by it unchanged
(proven in the compatibility test, Section 7), but invoking it is a
separate, subsequent caller's responsibility, never this Unit's own
(Composition-Only Invariant, Section 4d).

**No existing contract requires a new field, a new variant, or any
modification.** `OutboundParkerResponse` already has every field this Unit
needs to populate; `ReasoningProviderResponse.Reply` already carries
exactly the `text` this Unit needs to read.

### 2. Does this require a new architecture note, specification update, or ADR?

**No.** Applying the identical test `COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md`
already established and had accepted (itself applying PES-001 Stage 2A's
own "Required when" text: a Stage 2A pass is required only when a unit
"introduces public types it does not already have approved, field-level
Kotlin shapes for"):

- This Unit introduces **zero new public contract types.** Its return
  type is the existing `GatedOutcome<OutboundParkerResponse>` (Decision 4,
  below) — both `GatedOutcome<T>` and `OutboundParkerResponse` already
  exist. Its input types (`InboundOwnerMessage`, `ReasoningProviderResponse`)
  are unchanged.
- The one genuinely new value this Unit introduces — an operating
  `PrincipalId` string constant for `OutboundParkerResponse.senderPrincipalId`
  — is exactly the same class of value `CONVERSATION_ENGINE_PRINCIPAL_ID`
  and `PLANNER_RUNTIME_PRINCIPAL_ID` already are: a Stage 4 Implementation
  Decision (an implementation-level default the architecture deliberately
  left unspecified), not a Contract Design element. `COMMUNICATION_CONTRACT_DESIGN.md`
  Section 3 already states `OutboundParkerResponse.senderPrincipalId` must
  be "a real, resolved `PrincipalId`, threaded through explicitly, never a
  hardcoded constant" — it does not name which Principal, deliberately.
  Naming one now is exactly the kind of "resolve behavioural ambiguity
  before implementation" Stage 4 authorises, restating PES-001's own
  text: "Implementation Decisions clarify approved architecture. They do
  not replace or redefine it."
- This mirrors, point for point, the precedent's own applied reasoning:
  a small, concrete, non-interface-backed component composing one
  already-approved contract from another (here: `OutboundParkerResponse`
  from `ReasoningProviderResponse.Reply`) is ordinary Stage 3
  implementation-level wiring, not a new architectural boundary — and
  narrowing this Unit to composition only (Revision note, above) makes
  that boundary smaller than the original draft, not larger.
- Confirmed against the distinguishing test this campaign's own Sprint 8
  Contract Boundary Review established: a value never addressed at
  Contract Design time and silently deferred (here: which Principal
  composes an outbound reply) is legitimately a Stage 4 decision; a value
  explicitly decided and stated at Contract Design time (no such value
  exists here — Contract Design named this exact question and explicitly
  deferred it, rather than deciding it) would require a revision instead.
  This value was named and deferred, not decided, so naming it now is a
  Stage 4 decision, not a redefinition.

**Conclusion: proceed directly to a Stage 3 Implementation Plan, no
Contract Design revision, no ADR, no architecture note.** If this reading
is rejected at Scope Lock, the correction is to send this Plan back for a
Contract Design pass instead — this document does not itself foreclose
that possibility, only names the applied reasoning honestly, mirroring the
precedent's own identical self-consistency framing.

### 3. Where does this Unit sit, structurally?

```
InboundOwnerMessage
    v
CommunicationConversationCoordinator.submitAndReason(...)   [existing, reused unchanged]
    v
GatedOutcome<ReasoningProviderResponse>
    v
  [caller unwraps Produced; on Rejected, this Unit is never reached]
    v
ResponseComposer.compose(originalMessage, reasoningResponse)   [NEW -- this Unit]
    v
  Reply    -> GatedOutcome.Produced(OutboundParkerResponse)   -- composition only, nothing delivered
  Goal     -> GatedOutcome.NotAccepted("not a Reply; Goal routing is out of scope")
  NoAction -> GatedOutcome.NotAccepted("not a Reply; no action was warranted")
    v
  [a separate, future caller -- not this Unit -- may pass the composed
   OutboundParkerResponse to ResponseDelivery.deliver; this Unit's own
   compatibility test (Section 7) proves that hand-off works today,
   without this Unit performing it]
```

**Why this Unit takes `InboundOwnerMessage` directly, not `Turn`.**
Neither `ConversationTurnReasoningCoordinator.submitTurnAndReason` nor
`CommunicationConversationCoordinator.submitAndReason` returns the `Turn`
they construct — both return only the resulting `ReasoningProviderResponse`
(wrapped in `GatedOutcome`, in the latter case). Modifying either
return type is **out of this Unit's authority** ("Do not redesign
completed architecture"). But `OutboundParkerResponse` only needs
`channelId` and `correlationId` (Section 1, above) — both fields already
present, unchanged, on the original `InboundOwnerMessage` a caller already
holds before ever calling `CommunicationConversationCoordinator`. This
Unit therefore takes that same, already-available `InboundOwnerMessage`
as an input, alongside the resulting `ReasoningProviderResponse` — no
`Turn` dependency, no coordinator modification, no new field threaded
through anything.

---

## 1. Objective

Implement, and make independently testable, the smallest component that
converts a `ReasoningProviderResponse.Reply` into an `OutboundParkerResponse`
— composition only, nothing more:

- Given the `InboundOwnerMessage` that began a Turn, and the
  `ReasoningProviderResponse` reasoning about that Turn produced:
  - If `Reply`: construct one `OutboundParkerResponse` (fields fixed by
    Section 5) and return it wrapped in `GatedOutcome.Produced`.
  - If `Goal` or `NoAction`: construct nothing, and return
    `GatedOutcome.NotAccepted` naming which variant was received. **This
    is this Unit's stop condition:** nothing routes a `Goal` onward to
    Planner Runtime; this Unit's only job is the Reply branch.
- **This component never calls `ResponseDelivery`, or anything else.**
  Its one method returns a value; it performs no side effect (Section 4d).

## 2. Included Work

- One new, small, standalone, non-interface-backed component (name:
  `ResponseComposer`, Decision 2), with exactly one public method,
  `compose(originalMessage, reasoningResponse)`, performing exactly the
  branch in Section 1 and nothing else.
- Unit tests for the composer, per Section 7, including a compatibility
  test proving its output is accepted by the real `ResponseDelivery`
  unchanged, performed *by the test*, not by the composer.

## 3. Files Expected to Change

**All additions. No existing `src/` or `tests/` file requires
modification** — `ReasoningProvider`, `ConversationEngine`,
`CommunicationIntake`, `ResponseDelivery`, `IdentityService`, and
`GatedOutcome` are all consumed exactly as they exist today.

| File | New/Modified | Contents |
| --- | --- | --- |
| `src/runtime/ResponseComposer.kt` | New | The component described in Section 2, returning `GatedOutcome<OutboundParkerResponse>`. |
| `tests/runtime/ResponseComposerTest.kt` | New | Tests per Section 7, including the `ResponseDelivery` compatibility test. |

No new file is added under `src/interfaces/` — this Unit introduces no
public contract type (Required Analysis, Question 2).

## 4. Dependencies

**Exactly one, constructor-injected, an already-existing type, not
modified:**

- **`IdentityService`** — to resolve this Unit's own operating Principal
  before constructing an `OutboundParkerResponse` (Decision 3). Not used
  for anything else — in particular, never used to re-resolve
  `originalMessage.senderPrincipalId`, which was already resolved
  upstream by `CommunicationIntake`.

**Explicitly not a dependency: `ResponseDelivery`, `ExecutionPipeline`,
`ResourceRegistry`, `ToolRegistry`, `ToolInvocationBinding`,
`PermissionEngine`, `PlannerRuntime`, `ReasoningProvider`, `MemoryStore`,
`WorldModel`.** This is the Unit's primary structural enforcement
mechanism for the Composition-Only Invariant (Section 4d) — not merely a
stated rule: the component's constructor has exactly one parameter, and
there is nothing reachable, at any depth the constructor itself exposes,
through which this component could reason, plan, authorise, or deliver
anything.

## 4a. Statelessness Invariant

The composer holds exactly its one constructor-injected dependency as its
only field — no `var`, no mutable collection, no cache of any prior
`InboundOwnerMessage`, `ReasoningProviderResponse`, or
`OutboundParkerResponse`, and no `Mutex`. Each call to `compose` is fully
independent of every other call. Enforced structurally in Section 7 (a
reflective test asserting the class declares no field beyond its one
constructor-injected dependency), not only stated in KDoc — mirroring
`CommunicationConversationCoordinator`'s own identical precedent.

## 4b. Reply Pass-Through Invariant

`reply.text` is read exactly once and carried, unchanged, into
`OutboundParkerResponse.text` — no trim, case change, truncation, or
reformatting of any kind. `originalMessage.channelId` and
`originalMessage.correlationId` are copied unchanged into
`OutboundParkerResponse.channelId`/`correlationId` respectively — never
re-derived, never defaulted. This component never mutates or reinterprets
`originalMessage`, mirroring `CommunicationConversationCoordinator`'s own
identical "sequences/composes only" discipline, one layer further
downstream.

## 4c. Exception Propagation Invariant

**One deliberate, disclosed exception path, and no other.** If
`IdentityService.resolve` returns `null` for this Unit's own operating
Principal, `compose` throws `IllegalStateException` — this is this Unit's
own genuine precondition failure (an unregistered operating identity is a
deployment/setup defect, not a routine outcome), mirroring
`InMemoryConversationEngine.submitTurn`'s own identical, already-accepted
pattern for its own operating Principal. No `try`/`catch` exists anywhere
in this class. Since `IdentityService` is now this Unit's only dependency
(Revision note, above), this is also now this Unit's *only* possible
thrown-exception source — there is no second dependency call whose
exception this invariant needs to separately address.

`GatedOutcome.NotAccepted` is reserved exclusively for the one
structural, expected outcome this Unit defines — `reasoningResponse` is
`Goal` or `NoAction`, not `Reply` — never used to catch and repackage a
thrown exception.

## 4d. Composition-Only Invariant

**`ResponseComposer` performs composition only. It never reasons, plans,
authorises, or delivers.** Stated explicitly here, per the reviewing
human's own instruction, not left to be inferred from the absence of a
dependency:

- **Never reasons.** `ResponseComposer` does not call `ReasoningProvider.reason`,
  does not hold a `ReasoningProvider` dependency, and does not interpret,
  re-derive, or second-guess `reply.text`'s own content — it is read once
  and carried through unchanged (Section 4b). Interpretation happened
  upstream, entirely outside this Unit.
- **Never plans.** `ResponseComposer` does not call `PlannerRuntime.plan`,
  does not construct a `PlanningRequest`, and holds no `PlannerRuntime`
  dependency. A `Goal` causes this component to return
  `GatedOutcome.NotAccepted` and stop — never routed anywhere.
- **Never authorises.** `ResponseComposer` does not call
  `PermissionEngine.evaluate`, holds no `PermissionEngine` dependency, and
  makes no claim that its own output is pre-approved. Constructing an
  `OutboundParkerResponse` is never itself a permission decision — the
  `ExecutionRequest` `ResponseDelivery` eventually constructs from it is
  still evaluated by `PermissionEngine`, unchanged, exactly as it already
  is today (`RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 7).
- **Never delivers.** `ResponseComposer` does not call
  `ResponseDelivery.deliver`, does not hold a `ResponseDelivery`
  dependency (Section 4, revised), and performs no `ExecutionPipeline`,
  `Tool`, or `EventBus` call of any kind. Its one method returns a value;
  it has no side effect whatsoever — a stronger, more literal statelessness
  than "no mutable field" (Section 4a) also holding for the method body
  itself. Whether, when, and by what component a composed
  `OutboundParkerResponse` is ever actually delivered is entirely a
  future, separately-scoped caller's decision, not this Unit's own
  (Section 9).

**Enforced structurally, not merely stated:** the constructor dependency
list (Section 4) has no slot for `ReasoningProvider`, `PlannerRuntime`,
`PermissionEngine`, or `ResponseDelivery` — there is nothing to call
because there is nothing reachable to call it with. A dedicated
constructor-shape test (Section 7) makes this a compile-time property, not
a runtime assertion that could be silently weakened later.

## 5. Required Implementation Decisions

### Decision 1 — Input shape: `InboundOwnerMessage` + `ReasoningProviderResponse`, not `Turn`

Restating Required Analysis Question 3: neither existing coordinator
returns a `Turn`, and modifying either to do so is outside this Unit's
authority. `OutboundParkerResponse` needs only `channelId` and
`correlationId`, both already present, unchanged, on the
`InboundOwnerMessage` a caller already holds. **Proposed default: this
Unit's public method signature is `compose(originalMessage:
InboundOwnerMessage, reasoningResponse: ReasoningProviderResponse)`.**

### Decision 2 — Shape and name: a small, non-interface-backed, standalone class named `ResponseComposer`

Mirrors `CommunicationConversationCoordinator`/`ConversationTurnReasoningCoordinator`'s
own identical, already-accepted precedent for being non-interface-backed:
it introduces no new field, no new domain concept, and no state of its
own. **Named `ResponseComposer`, not `...Coordinator`** (renamed at
review) because, unlike those two, it no longer sequences a call to a
second component — it performs exactly one pure transformation and
returns. "Coordinator" would now overstate what this class does;
"Composer" names it precisely. **If a future caller needs to depend on
this behaviour abstractly, that need must be met by a later Contract
Design pass or an explicit, disclosed additive-interface decision — not
by silently promoting this Unit's own concrete class into a public
contract.**

### Decision 3 — Operating identity: a new Principal, `system.response-composer`, resolved via `IdentityService`

`OutboundParkerResponse.senderPrincipalId` must be "the `Principal`
responsible for the response's content... never a hardcoded constant"
(`COMMUNICATION_CONTRACT_DESIGN.md` Section 3). This Unit is that
Principal's responsible component. **Proposed default:** a new
`PrincipalId("system.response-composer")` constant, resolved via
`IdentityService.resolve` before constructing any `OutboundParkerResponse`
— structurally and terminologically mirroring
`CONVERSATION_ENGINE_PRINCIPAL_ID` (`InMemoryConversationEngine`) and
`PLANNER_RUNTIME_PRINCIPAL_ID` (`InMemoryPlannerRuntime`) exactly, and now
also matching the class's own renamed identity (Decision 2). This is a
Stage 4 Implementation Decision naming an implementation-level default
for a value Contract Design explicitly left unspecified (Required
Analysis, Question 2) — not a new architectural concept. A production
composition root would need to register this Principal before this
component could run for real — out of this Unit's own scope, exactly as
Sprint 8's registration was test-level only.

### Decision 4 — Return type: `GatedOutcome<OutboundParkerResponse>`, introduce no new type

On the `Reply` branch, this Unit produces exactly one
`OutboundParkerResponse` and returns it, wrapped in `GatedOutcome.Produced`.
On the `Goal`/`NoAction` branch, no `OutboundParkerResponse` exists to
produce — the structurally correct outcome is exactly
`GatedOutcome.NotAccepted`, naming which variant was received and that it
was not a `Reply`. **Proposed default: this Unit's own return type is
`GatedOutcome<OutboundParkerResponse>`, introducing no new result type at
all**, directly exercising `GatedOutcome<T>`'s own documented "suitable
for reuse by future coordinators with the same gating semantics" design
intent (`COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md`
Section 5 Decision 1) for the first time since it was introduced. (Revised
from the original draft's `GatedOutcome<ExecutionResult>` — that shape
implied this Unit itself produced an `ExecutionResult`, i.e. that it
delivered something. `GatedOutcome<OutboundParkerResponse>` correctly
reflects that this Unit's own output is the composed response, not a
delivery outcome.)

**Why `NotAccepted` is the right shape for "not a Reply," not a thrown
exception.** A `Goal` or `NoAction` reaching this component is an
expected, routine outcome of ordinary conversation flow (most Turns will
not produce a `Reply`), not a caller-misuse condition — the identical
reasoning `CommunicationIntakeDisposition.Rejected` and
`GatedOutcome.NotAccepted` already established for their own respective
"routine, not exceptional" outcomes.

### Decision 5 — `ResponseDelivery` is not a dependency; compatibility is proven by test, not by design

**Revised at review.** The original draft made `ResponseDelivery` a
constructor dependency and had this component call `deliver` directly on
the `Reply` branch. That directly conflicts with the Composition-Only
Invariant (Section 4d) the review added, so it is removed here, not
merely relabelled. **Proposed default:** `ResponseComposer` never
references `ResponseDelivery` in `src/`. Compatibility between this
Unit's output and `ResponseDelivery`'s expected input is instead proven
by a dedicated test (Section 7) that separately constructs a real
`ResponseDelivery` (plus the full Sprint 8 stack) and passes this Unit's
composed `OutboundParkerResponse` to it directly, in the test — never
inside `ResponseComposer` itself. Whichever future component actually
calls `ResponseDelivery.deliver` in production (a composition-root-level
decision, explicitly out of this Unit's scope, Section 9) may reuse
`ResponseComposer`'s output as-is; this Unit does not decide, or need to
decide, who that caller is.

### Decision 6 — `timestamp` and `metadata`

`OutboundParkerResponse.timestamp` is set to `Instant.now()` at the moment
this component constructs the response — mirroring `ResponseDelivery`'s
own identical `createdAt = Instant.now()` pattern when it constructs an
`ExecutionRequest`. `OutboundParkerResponse.metadata` is left at its
default, empty — no metadata is invented; nothing in this Unit's own
scope requires one.

## 6. EventBus Implications

**None.** `IdentityService` (this Unit's only dependency) does not publish
or subscribe to `EventBus` on this Unit's behalf. This Unit itself
publishes nothing — restating Section 4d's "no side effect whatsoever."

## 7. Testing Strategy

**`ResponseComposerTest.kt`:**

- **Reply path.** Given a `Reply("hello, owner")` and an
  `InboundOwnerMessage`, and a `ResponseComposer` built from a real
  `InMemoryIdentityService` (with `system.response-composer` registered):
  `compose` returns `GatedOutcome.Produced` carrying an
  `OutboundParkerResponse` with `text == "hello, owner"`,
  `channelId`/`correlationId` copied unchanged from the message, and
  `senderPrincipalId == PrincipalId("system.response-composer")`.
- **Goal path.** Given `Goal("some goal")`, asserts `GatedOutcome.NotAccepted`
  is returned, naming that the variant was not a `Reply`.
- **NoAction path.** Same shape of assertion, for `NoAction`.
- **Field pass-through / non-mutation (Section 4b).** Full field-level
  assertion that `channelId`, `correlationId` on the composed
  `OutboundParkerResponse` exactly equal the corresponding fields on the
  original `InboundOwnerMessage`.
- **Unregistered operating identity.** With an `InMemoryIdentityService`
  that never registered `system.response-composer`, `compose` is asserted
  to throw `IllegalStateException` — mirroring
  `InMemoryConversationEngineTest`'s own identical test for
  `system.conversation-engine`.
- **Statelessness test (Section 4a).** Reflective test asserting
  `ResponseComposer::class.java.declaredFields` contains exactly its one
  constructor-injected dependency (`identityService`).
- **Structural composition-only test (Section 4d).** A compile-time /
  constructor-shape check: `ResponseComposer`'s constructor accepts only
  an `IdentityService` — there is no dependency slot for
  `ResponseDelivery`, `ExecutionPipeline`, `PermissionEngine`,
  `PlannerRuntime`, or `ReasoningProvider` to even construct the fixture
  with one — mirroring every prior Sprint 7/8 coordinator's identical
  structural test.
- **Exactly-once identity resolution.** For a single `compose` call
  resulting in `Produced`, `IdentityService.resolve` is called exactly
  once (verifiable via a thin call-counting wrapper or by reusing
  `InMemoryIdentityService` directly and asserting no unexpected second
  registration/resolution side effect).
- **Compatibility test — proves this Unit's output works with the real
  `ResponseDelivery`, without `ResponseComposer` performing the call
  itself (Sprint 9 Step 7's own explicit "compatible with the existing
  local text delivery path" requirement, and Decision 5).** Mirrors
  `LocalTextChannelDeliverToolTest.kt`'s own established end-to-end
  pattern for the delivery half: a real `InMemoryResourceRegistry`,
  `InMemoryToolRegistry`, `InMemoryModuleRegistry`,
  `InMemoryToolInvocationBinding`, `InMemoryActionVocabulary`/`ActionMapper`,
  `InMemoryEventBus`, `FakePermissionEngine` (configured for
  `NOTIFY`/`TOOL`, per `ResponseDeliveryTest.kt`'s own precedent),
  `DefaultExecutionPipeline`, a real `LocalTextChannelDeliverTool`, and a
  real `ResponseDelivery` — all wired together exactly as Sprint 8's own
  test wires them. The test itself, not `ResponseComposer`, then: (1)
  calls `ResponseComposer.compose(message, Reply(text))` to obtain an
  `OutboundParkerResponse`; (2) separately calls
  `responseDelivery.deliver(composedResponse)` directly; (3) asserts the
  result is `GatedOutcome.Produced` with `ExecutionResultStatus.SUCCESS`,
  and that the Local Text Channel's injected owner-notification callback
  received exactly `text`, unchanged. This is the direct, structural proof
  that a `ResponseComposer`-composed response is delivery-ready today,
  without fabricating an Android display claim (Sprint 8's own
  established discipline, restated here) and without `ResponseComposer`
  itself ever touching `ResponseDelivery`.

**Full Gradle test suite.** Run the complete suite once implementation is
complete and report a real, Android-Studio-verified result. This program's
sandbox cannot resolve the Kotlin Gradle plugin (an established, disclosed
limitation, unchanged since Sprint 8) — a static `@Test`-count projection
will be reported honestly, with an explicit "not verified" disclosure,
pending Steven's own Android Studio run, exactly as Sprint 8 required.

## 8. Acceptance Criteria

- `ResponseComposer` exists, is independently constructible with only an
  `IdentityService`, and its own tests (Section 7) pass.
- A `Reply` is composed into exactly one correct `OutboundParkerResponse`,
  with `text`, `channelId`, and `correlationId` field-for-field traceable
  to the original `Reply`/`InboundOwnerMessage` — verified structurally.
- A `Goal` or `NoAction` never produces an `OutboundParkerResponse` —
  `compose` returns `GatedOutcome.NotAccepted` in both cases.
- **The composer never reasons, plans, authorises, or delivers (Section
  4d)** — verified structurally, by the absence of any dependency slot
  for `ReasoningProvider`, `PlannerRuntime`, `PermissionEngine`, or
  `ResponseDelivery`, not merely by inspection.
- The composer is stateless (Section 4a), verified structurally.
- The composer never mutates or reinterprets `originalMessage` or
  `reasoningResponse` (Section 4b), verified structurally.
- The composer's only possible thrown exception is the disclosed
  operating-identity precondition (Section 4c) — verified by test.
- **A separate compatibility test (Section 7), external to the composer
  itself, proves its output is accepted by the real `ResponseDelivery` /
  `LocalTextChannelDeliverTool` stack unchanged.**
- No production (`src/`) code added by this Unit references
  `PlannerRuntime`, `PlanningRequest`, `ExecutionPipeline`,
  `ResponseDelivery`, `PermissionEngine`, `ToolRegistry`, `MemoryStore`,
  `WorldModel`, or `ReasoningProvider`, anywhere.
- No existing `src/` or `tests/` file is modified (Section 3).
- All tests listed in Section 7 pass, and the full Gradle suite passes (or
  a projected count is honestly reported, per Section 7's own disclosure
  discipline).

## 9. Implementation Boundaries — Out of Scope

Restating the task's own explicit prohibited list, each grounded in why it
is excluded:

- **A model-backed `ReasoningProvider` implementation.** This Unit
  consumes whatever `ReasoningProviderResponse` it is given; it does not
  produce one, and holds no `ReasoningProvider` dependency (Section 4d).
- **Planner Runtime / Goal routing.** No `PlannerRuntime` reference exists
  anywhere this Unit's code can reach. A `Goal` response causes this
  component to return `GatedOutcome.NotAccepted` and stop — nothing
  routes it onward.
- **Agents.** No `AgentRuntime` reference exists anywhere this Unit's code
  can reach.
- **Invoking `ResponseDelivery.deliver` from within this component.**
  Explicitly out of scope by design (Decision 5, Section 4d) — this Unit
  composes; it does not deliver. Which future component actually performs
  that call, and when, is a separate, later decision this Plan does not
  make.
- **A production composition root.** This Unit's own registration of
  `system.response-composer` with `IdentityService` happens only inside
  its own tests — no production startup path exists in this repository to
  perform that registration for real, exactly as Sprint 8's own
  `LocalTextChannelDeliverTool` registration was test-level only.
- **Modifying `CommunicationConversationCoordinator`,
  `ConversationTurnReasoningCoordinator`, `ConversationEngine`,
  `ReasoningProvider`, `ResponseDelivery`, or `GatedOutcome`.** All are
  reused exactly as they exist today — restating "do not redesign
  completed architecture."
- **A full owner-message-to-delivery orchestrator wrapping
  `CommunicationConversationCoordinator`, `ResponseComposer`, and
  `ResponseDelivery` together.** Out of this Un