# Reply to OutboundParkerResponse Construction — Implementation Plan (Sprint 10, Unit 1)

## Status

**Stage 3 Implementation Plan, PES-001. Not yet Scope Locked.** No Kotlin
is written by this document. This document performs PES-001's own Stage 3
breakdown — Included work, Excluded work, Dependencies, Acceptance
Criteria, Unit Stop Conditions — for one unit only.

**Fourth revision note (title correction, pre-Scope-Lock).** This
document was originally titled "(Sprint 9)," reflecting when it was
drafted — as the single unnamed candidate `SPRINT_9_HANDOVER.md` Section 3
identified ("close gap #53 item 1 — Reply → `OutboundParkerResponse` →
`ResponseDelivery` wiring"), written during Sprint 9 but explicitly not
yet authorised ("This is a recommendation for Steven's own sequencing
decision, not a Scope Lock, not an Implementation Plan, and not an
authorisation to begin. Per `PARKER_ENGINEERING_STANDARD.md`, Sprint 10
begins only when Steven says so"). Steven has since confirmed Sprint 10
has begun and that this Plan is its first implementation unit. The title
is corrected here to **"(Sprint 10, Unit 1)"** — matching this
repository's own established convention for a discrete, sequenced Plan
within a numbered Sprint (`RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md`
"(Sprint 7, Unit C4)"; `COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md`
"(Sprint 7, Unit C2)") — and to avoid colliding with Sprint 9's own
already-complete, already-documented, differently-scoped deliverable
(`MODEL_REASONING_PROVIDER_IMPLEMENTATION_PLAN.md`, 578/578 verified,
per `SPRINT_9_HANDOVER.md`). This is a label correction only. No
technical decision in this document (Sections 1 through 18) is altered
by it.

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

**Second revision note (post-Reconciliation, pre-Scope-Lock).** A separate
governance track briefly explored replacing this document's own design
with a different component, `ReplyDeliveryCoordinator`, that constructed
`OutboundParkerResponse` directly and called `ResponseDelivery` itself.
That alternative has been withdrawn in full, and this document's own
design — `ResponseComposer`, composition-only, `ResponseDelivery` not a
dependency, `IdentityService.resolve`-before-use identity handling — is
confirmed as the canonical design for `IMPLEMENTATION_GAPS.md` #53 item 1,
per `RECONCILIATION_ADDENDUM_RESPONSE_COMPOSER_VS_REPLY_DELIVERY_COORDINATOR.md`,
Option C. No technical decision already recorded in this document (Sections
1 through 9, as they already existed) is altered by that reconciliation —
this note records the outcome; it does not itself change anything above.
The name `ReplyDeliveryCoordinator` is retained, redefined, for a separate,
thin orchestrator that calls this component and then `ResponseDelivery` in
sequence — addressed in a companion Stage 3 Implementation Plan,
`REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`, not this document.

**Third revision note (pre-Scope-Lock correction — identity resolution
scope).** `IdentityService.resolve` was originally called unconditionally,
before branching on `reasoningResponse`'s variant — meaning a `Goal` or
`NoAction` Turn still caused a `system.response-composer` resolution
attempt, and still threw `IllegalStateException` if that Principal were
unregistered, even though no `OutboundParkerResponse` was ever going to be
constructed on either branch. This was inconsistent with the
Composition-Only Invariant's own "constructs nothing on the Goal/NoAction
branch" claim (Section 1, Section 4d) and imposed an unnecessary
precondition on two branches that need it not at all. **Corrected here:
identity resolution now happens only inside the `Reply` branch of
`compose`, exactly once, only at the moment an `OutboundParkerResponse` is
actually about to be constructed.** `Goal` and `NoAction` never call
`IdentityService.resolve` and return `GatedOutcome.NotAccepted`
unconditionally, regardless of whether `system.response-composer` is
registered. This changes Section 4c, Decision 3 (Section 5), the proposed
signature (Section 10), the field mapping (Section 11), the testing
strategy (Section 7), and the acceptance criteria (Section 8) — each
updated in place below, not left inconsistent with this note. The
*selected identity* (`system.response-composer`) and the
*resolve-before-use rule itself* are unchanged; only the scope of when
resolution occurs is corrected.

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
| `tests/runtime/FakeIdentityService.kt` | New | A lambda-based, call-counting `IdentityService` fake, mirroring `FakeExecutionPipeline`/`FakeResourceRegistry`'s established precedent (`ResponseDeliveryTest.kt`) — added by the Third revision note's correction, to make `resolve`'s exactly-once-for-Reply / zero-for-Goal-or-NoAction call counts verifiable (Section 7). Only `resolve` is exercised by `ResponseComposer`; `register`/`updateStatus`/`touch`/`listByOwner` throw if reached, the same structural guard `FakeExecutionPipeline` and `FakeResourceRegistry` already use for their own unused methods. |

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

**One deliberate, disclosed exception path, reachable from exactly one
branch, and no other.** `IdentityService.resolve` is called only inside
the `Reply` branch (Third revision note, above; Decision 3, Section 5) —
`compose` never calls it for `Goal` or `NoAction`. If, and only if,
`reasoningResponse` is `Reply` and `IdentityService.resolve` returns
`null` for this Unit's own operating Principal, `compose` throws
`IllegalStateException` — this is this Unit's own genuine precondition
failure (an unregistered operating identity is a deployment/setup defect,
not a routine outcome), mirroring `InMemoryConversationEngine.submitTurn`'s
own identical, already-accepted pattern for its own operating Principal.
No `try`/`catch` exists anywhere in this class. Since `IdentityService` is
this Unit's only dependency (Revision note, above), this is also this
Unit's *only* possible thrown-exception source — there is no second
dependency call whose exception this invariant needs to separately
address.

**A `Goal` or `NoAction` Turn can never throw this exception, or any
exception, regardless of whether `system.response-composer` is registered
with `IdentityService`.** This is a direct, structural consequence of
resolution happening only inside the `Reply` branch, not merely a claim
about typical behaviour — `compose`'s own code on those two branches
never reaches the line that could throw. Verified by test (Section 7:
"Goal/NoAction never call resolve, even when unregistered").

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

**Corrected scope of resolution (Third revision note, above):
`IdentityService.resolve` is called only inside the `Reply` branch of
`compose`, exactly once, and only at the moment an `OutboundParkerResponse`
is actually about to be constructed.** The `Goal` and `NoAction` branches
never call `resolve` and never depend on whether `system.response-composer`
is registered — there is nothing on those branches for an operating
identity to be responsible for. This is a tighter reading of
"resolve-before-use" than the original draft applied: "use" means the
moment of construction specifically, not the moment `compose` is entered.
The identity itself and the resolve-before-use rule are unchanged; only
the scope of when resolution occurs is corrected.

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

**Identity fixture, corrected (Third revision note).** The isolated tests
below use `FakeIdentityService` (Section 3), not `InMemoryIdentityService`
— it is interface-backed and call-counting, so `resolve`'s exact
invocation count can be asserted per branch, which `InMemoryIdentityService`
alone cannot make precise without an additional wrapper. This mirrors
`ResponseDeliveryTest.kt`'s own established choice (`FakeResourceRegistry`/
`FakeExecutionPipeline` for `ResponseDelivery`'s isolated tests; the real
`InMemoryResourceRegistry`/`DefaultExecutionPipeline` stack reserved for
one dedicated end-to-end compatibility test). `InMemoryIdentityService` is
still used, unchanged, in the compatibility test below, since that test's
own purpose is to exercise real production wiring.

- **Reply path.** Given a `Reply("hello, owner")` and an
  `InboundOwnerMessage`, and a `ResponseComposer` built from a
  `FakeIdentityService` configured to return the
  `system.response-composer` `Principal` on `resolve`: `compose` returns
  `GatedOutcome.Produced` carrying an `OutboundParkerResponse` with
  `text == "hello, owner"`, `channelId`/`correlationId` copied unchanged
  from the message, and `senderPrincipalId == PrincipalId("system.response-composer")`
  — and `fakeIdentityService.resolveCallCount == 1`.
- **Goal path.** Given `Goal("some goal")`, asserts `GatedOutcome.NotAccepted`
  is returned, naming that the variant was not a `Reply`, **and
  `fakeIdentityService.resolveCallCount == 0`** — `resolve` is never
  called on this branch (Third revision note; Section 4c).
- **NoAction path.** Same shape of assertion, for `NoAction`, including
  `resolveCallCount == 0`.
- **Goal/NoAction never throw, even when unregistered (Section 4c).**
  With a `FakeIdentityService` configured to return `null` for
  `system.response-composer` (i.e. simulating "unregistered"), both the
  `Goal` and `NoAction` branches still return `GatedOutcome.NotAccepted`
  normally — no exception is thrown, and `resolveCallCount` remains `0`
  in both cases. This is the direct, structural proof that these two
  branches never depend on `system.response-composer`'s registration
  state at all.
- **Field pass-through / non-mutation (Section 4b).** Full field-level
  assertion that `channelId`, `correlationId` on the composed
  `OutboundParkerResponse` exactly equal the corresponding fields on the
  original `InboundOwnerMessage`.
- **Unregistered operating identity — Reply branch only.** With a
  `FakeIdentityService` configured to return `null` for
  `system.response-composer`, and a `Reply` response: `compose` is
  asserted to throw `IllegalStateException` — mirroring
  `InMemoryConversationEngineTest`'s own identical test for
  `system.conversation-engine`, now explicitly scoped to the one branch
  capable of reaching it (contrast with the Goal/NoAction test
  immediately above, using the identical unregistered fixture to prove
  the opposite outcome on those branches).
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
- **Exactly-once identity resolution, corrected per branch.** For a
  single `compose` call: `fakeIdentityService.resolveCallCount == 1`
  when the result is `Produced` (i.e. `reasoningResponse` was `Reply`);
  `resolveCallCount == 0` when the result is `NotAccepted` from a `Goal`
  or `NoAction` input. This replaces the original draft's unconditional
  "exactly once" claim, which held only because the original draft
  resolved identity before branching — no longer true after the Third
  revision note's correction.
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
  operating-identity precondition (Section 4c), **reachable only from the
  `Reply` branch — a `Goal` or `NoAction` input never throws, and never
  calls `IdentityService.resolve` at all, regardless of whether
  `system.response-composer` is registered** — verified by test.
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
  `ResponseDelivery` together.** Out of this Unit's own scope —
  assembling that orchestrator is exactly the redefined
  `ReplyDeliveryCoordinator`'s own job, addressed in a separate, companion
  PES-001 Stage 3 Implementation Plan
  (`REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`), not this
  document's (Second revision note, above).

## 10. Proposed Signatures

Restated here as an explicit code block, for Scope Lock review — this is
a proposed shape, not yet-written Kotlin:

```kotlin
package parker.core.runtime

import parker.core.interfaces.IdentityService
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.OutboundParkerResponse
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningProviderResponse
import java.time.Instant

/**
 * Composes a [ReasoningProviderResponse.Reply] into an [OutboundParkerResponse].
 * Composition only -- never reasons, plans, authorises, or delivers
 * (Section 4d). See `docs/implementation/REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md`.
 */
class ResponseComposer(
    private val identityService: IdentityService,
) {

    private companion object {
        val RESPONSE_COMPOSER_PRINCIPAL_ID = PrincipalId("system.response-composer")
    }

    suspend fun compose(
        originalMessage: InboundOwnerMessage,
        reasoningResponse: ReasoningProviderResponse,
    ): GatedOutcome<OutboundParkerResponse> {
        return when (reasoningResponse) {
            is ReasoningProviderResponse.Reply -> {
                // Identity is resolved here, and only here -- exactly at the
                // moment an OutboundParkerResponse is actually about to be
                // constructed (Third revision note; Section 4c; Decision 3).
                val composerIdentity = identityService.resolve(RESPONSE_COMPOSER_PRINCIPAL_ID)
                    ?: throw IllegalStateException(
                        "Response Composer operating Principal " +
                            "'${RESPONSE_COMPOSER_PRINCIPAL_ID.value}' is not registered with IdentityService",
                    )

                GatedOutcome.Produced(
                    OutboundParkerResponse(
                        channelId = originalMessage.channelId,
                        senderPrincipalId = composerIdentity.principalId,
                        text = reasoningResponse.text,
                        timestamp = Instant.now(),
                        correlationId = originalMessage.correlationId,
                    ),
                )
            }
            is ReasoningProviderResponse.Goal -> GatedOutcome.NotAccepted(
                "not a Reply; reasoningResponse was Goal",
            )
            ReasoningProviderResponse.NoAction -> GatedOutcome.NotAccepted(
                "not a Reply; reasoningResponse was NoAction",
            )
        }
    }
}
```

`compose` is declared `suspend` because its only dependency call,
`IdentityService.resolve`, is itself `suspend`
(`src/interfaces/IdentityService.kt` line 34) — not a new design choice,
just an accurate reflection of the one call this method actually makes.
**That call now sits inside the `Reply` branch only** (Third revision
note) — the `Goal` and `NoAction` branches contain no `suspend` call of
their own, though the method as a whole remains `suspend` because one
branch requires it and Kotlin's `when` cannot be partially `suspend`.

## 11. Field Mapping

Every field of the composed `OutboundParkerResponse`, traced to its exact
source, so no field is left to be inferred at implementation time:

| `OutboundParkerResponse` field | Type | Source | Derivation |
| --- | --- | --- | --- |
| `channelId` | `ModuleId` | `originalMessage.channelId` | Copied unchanged (Section 4b). |
| `senderPrincipalId` | `PrincipalId` | `identityService.resolve(RESPONSE_COMPOSER_PRINCIPAL_ID)!!.principalId` | The resolved operating Principal's own `principalId`, never the constant directly and never `originalMessage.senderPrincipalId` (Decision 3; mirrors `InMemoryPlannerRuntime`'s `publisherPrincipalId = plannerIdentity.principalId` pattern, the fix for `IMPLEMENTATION_GAPS.md` #49). **Resolution happens inside this same `Reply` branch, immediately before this field is populated (Third revision note) — not earlier in the method.** |
| `text` | `String` | `reasoningResponse.text` (`Reply` branch only) | Copied unchanged, read exactly once (Section 4b). |
| `timestamp` | `Instant` | `Instant.now()` | Set at the moment of construction (Decision 6). |
| `correlationId` | `CorrelationId` | `originalMessage.correlationId` | Copied unchanged (Section 4b). |
| `metadata` | `Map<String, String>` | *(not set)* | Left at its default, `emptyMap()` (Decision 6). |

On the `Goal`/`NoAction` branch, no `OutboundParkerResponse` is
constructed at all, and — as of the Third revision note's correction —
no `IdentityService.resolve` call is made either. No field mapping
applies; `GatedOutcome.NotAccepted` carries only a `reason: String`
naming which variant was received (Decision 4).

## 12. Verification Procedure

Since this program's sandbox cannot resolve the Kotlin Gradle plugin
(Section 7's own disclosed limitation), verification of this Unit
requires the following procedure, to be carried out by Steven in Android
Studio, once Scope Locked and implemented:

1. Open the `parker-platform` project in Android Studio.
2. Run `./gradlew test` (or the equivalent IDE test-runner action) against
   the full suite, not only `ResponseComposerTest.kt` — this confirms no
   existing test regresses, restating Section 3's "no existing file
   modified" claim as an observed, not merely asserted, result.
3. Confirm `ResponseComposerTest.kt`'s own tests (Section 7) all pass
   individually: Reply path, Goal path, NoAction path, field pass-through,
   unregistered-identity exception, statelessness, structural
   composition-only shape, exactly-once identity resolution, and the
   `ResponseDelivery` compatibility test.
4. Record the real pass/fail count and compare it against the static
   projection reported at implementation time (Section 7) — any
   discrepancy is itself a finding to report, not to silently reconcile.
5. Only after a real, verified pass is confirmed does
   `IMPLEMENTATION_HISTORY.md` become eligible for an update (Section 15).

This Plan does not itself claim any test has passed — Sections 7 and 8
describe what will be tested and why it will hold; this section describes
how that claim gets checked against reality.

## 13. Architectural Traceability

| Implemented element | Authorised by |
| --- | --- |
| `ResponseComposer.compose` accepting `InboundOwnerMessage` + `ReasoningProviderResponse`, not `Turn` | Required Analysis, Question 3; this document, Decision 1 |
| `ResponseComposer`'s non-interface-backed shape, named `ResponseComposer` not `...Coordinator` | This document, Decision 2, mirroring `CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` Section 5 Decision 2 and `COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md` Section 5 Decision 2's identical, already-accepted reasoning |
| `system.response-composer` operating identity, resolved via `IdentityService.resolve` before use, **scoped to the `Reply` branch only** | This document, Decision 3 (as corrected by the Third revision note), mirroring `InMemoryConversationEngine`'s `CONVERSATION_ENGINE_PRINCIPAL_ID` and `InMemoryPlannerRuntime`'s `PLANNER_RUNTIME_PRINCIPAL_ID` precedent, the resolve-before-use fix for `IMPLEMENTATION_GAPS.md` #49 |
| `GatedOutcome<OutboundParkerResponse>` as the return type, introducing no new type | This document, Decision 4, exercising `COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md` Section 5 Decision 1's own documented reuse intent |
| `ResponseDelivery` excluded as a dependency; compatibility proven by test, not design | This document, Decision 5 — a Revision-note-driven correction, not present in the original draft |
| No routing of `Goal` onward to Planner Runtime; no delivery of `Reply` | This task's own explicit instruction, combined with `IMPLEMENTATION_GAPS.md` #53's own still-open routing/delivery questions |
| Structural (not asserted) trust-boundary enforcement — no dependency slot for `ReasoningProvider`/`PlannerRuntime`/`PermissionEngine`/`ResponseDelivery` | Section 4, Section 4d; mirrors both prior Contract Design documents' and `COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md`'s identical "structural guarantee, not merely a stated rule" framing |
| The composer's statelessness invariant | Section 4a, mirroring `CommunicationConversationCoordinator`'s own identical, already-accepted invariant |
| The composer's Reply pass-through invariant | Section 4b, mirroring `CommunicationConversationCoordinator`'s own Message Pass-Through Invariant, one layer further downstream |
| The composer's exception propagation invariant (identity precondition only) | Section 4c, mirroring `InMemoryConversationEngine.submitTurn`'s own identical, already-accepted pattern |
| The composer's Composition-Only invariant | Section 4d — a review-driven addition, per the Revision note |
| Canonical status of this design over the withdrawn `ReplyDeliveryCoordinator` alternative | `RECONCILIATION_ADDENDUM_RESPONSE_COMPOSER_VS_REPLY_DELIVERY_COORDINATOR.md`, Option C (Second revision note, above) |

## 14. Self-Traceability Review (Stage 9 Preview)

PES-001 Stage 9 requires a Self-Traceability Review for Level 2/3
implementation units once Kotlin exists. This Unit qualifies (it is not a
trivial, Level 1 change). This section states, in advance, what that
review will need to check, so Scope Lock can confirm the plan itself is
reviewable before any code is written — it does not perform the review
itself, since no Kotlin yet exists to review:

- Every test in `ResponseComposerTest.kt` (Section 7) must trace to
  exactly one invariant or decision in Sections 4a–4d or 5 — no test
  should exist that does not verify something this Plan named, and no
  invariant or decision named in Sections 4a–4d or 5 should lack a
  corresponding test.
- Every field of a constructed `OutboundParkerResponse` must trace to
  exactly one row of Section 11's Field Mapping table — no field should
  be populated by logic this Plan did not describe.
- The final `ResponseComposer.kt` constructor must match Section 4's
  dependency list exactly (one parameter, `IdentityService`) — any
  additional parameter is a Scope Lock violation, not a permissible
  implementation-time addition.
- The final `compose` signature must match Section 10's proposed
  signature exactly, or any deviation must be disclosed and justified
  against Sections 1–9, not silently introduced.
- **`identityService.resolve` must appear exactly once in the final
  `ResponseComposer.kt`, textually inside the `Reply` branch of the
  `when` — not before the `when`, and not in a shared prelude reachable
  by all three branches** (Third revision note) — grep/code-review
  verifiable, and the direct implementation-level check corresponding to
  the `resolveCallCount` test assertions in Section 7.
- No reference to `ResponseDelivery`, `ExecutionPipeline`,
  `PermissionEngine`, `PlannerRuntime`, `ReasoningProvider`,
  `ToolRegistry`, `MemoryStore`, or `WorldModel` may appear anywhere in
  `src/runtime/ResponseComposer.kt` — grep-verifiable, restating Section
  8's acceptance criterion as a literal, mechanical check.

## 15. Documentation Updates (Planned, Not Performed by This Document)

This Plan does not itself modify `IMPLEMENTATION_GAPS.md`,
`IMPLEMENTATION_HISTORY.md`, or any architecture or Contract Design
document. After verified implementation (Section 12) only:

- `IMPLEMENTATION_HISTORY.md` **may** be updated, recording this Unit
  exactly as delivered — files added, tests added, a real (not
  projected) Gradle result.
- `IMPLEMENTATION_GAPS.md` #53 **may be clarified further, not closed.**
  This Unit implements construction of an `OutboundParkerResponse` from a
  `Reply` in full, but performs no delivery and no routing — gap #53's
  own remaining items (Goal/Planner routing; no production composition
  root; `ReasoningContext` assembly ownership; the untested live HTTP
  path) all remain open, and the *wiring that would actually call
  `ResponseDelivery`* remains open until the companion
  `ReplyDeliveryCoordinator` Unit (Section 9; see the companion Plan) is
  itself implemented and verified. Any clarifying update should state
  plainly: a `Reply` can now be composed into a delivery-ready
  `OutboundParkerResponse` through one, real, tested, production code
  path; nothing yet calls that path from a real conversation turn, and
  nothing yet delivers its output automatically.
- No architecture or Contract Design document
  (`COMMUNICATION_CONTRACT_DESIGN.md`, `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`,
  `REASONING_PROVIDER_CONTRACT_DESIGN.md`, `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`)
  is modified at any point during this Unit's implementation.
- The withdrawn `ReplyDeliveryCoordinator` design and the governance
  documents that produced it (the Sprint 10 Governance Review, the
  withdrawn Contract Design Addendum, the withdrawn Sprint 10
  Implementation Plan) are not migrated, referenced as authoritative, or
  otherwise acted on by this documentation update — per the
  Reconciliation Addendum's own explicit document-handling instruction.

## 16. Implementation Sequence

The order Stage 6 work would follow once this Plan is Scope Locked — not
performed by this document, stated here so Scope Lock can review the
sequence itself, not only the end state:

1. Add `RESPONSE_COMPOSER_PRINCIPAL_ID = PrincipalId("system.response-composer")`
   as a private, class-scoped companion constant inside
   `src/runtime/ResponseComposer.kt` (Section 10) — no shared/public
   constants file, mirroring `InMemoryConversationEngine`'s identical
   placement.
2. Implement `ResponseComposer` per Section 10's proposed signature —
   with identity resolution placed inside the `Reply` branch specifically
   (Third revision note).
3. Implement `tests/runtime/FakeIdentityService.kt` (Section 3) before
   `ResponseComposerTest.kt`, since the latter depends on it for
   per-branch `resolveCallCount` assertions.
4. Implement `ResponseComposerTest.kt`, in the order listed in Section 7:
   Reply path, Goal path, NoAction path, Goal/NoAction-never-throw-when-
   unregistered, field pass-through, unregistered-identity exception
   (Reply branch only), statelessness, structural composition-only
   shape, exactly-once-per-branch identity resolution, then the
   `ResponseDelivery` compatibility test last (it is the most expensive
   test to construct, reusing the full Sprint 8 stack and, unlike the
   others, still using `InMemoryIdentityService`).
5. Register `system.response-composer` only inside test setup — no
   production registration path is added (Section 9).
6. Run the full Gradle suite locally if possible; otherwise report a
   static, honestly-disclosed projected count (Section 7).
7. Hand off to Steven for the Verification Procedure (Section 12).
8. Only after a real, verified pass: apply the Documentation Updates
   (Section 15).

This sequence does not begin until an explicit Scope Lock instruction is
given (Section 18) — it is documented in advance, not performed now.

## 17. Completion Criteria

- `IMPLEMENTATION_GAPS.md` and `IMPLEMENTATION_HISTORY.md` are **not**
  touched by this Plan, and must not be touched during implementation
  until every test in Section 7 passes and the full Gradle suite result
  is known (real or honestly disclosed as projected).
- After verified implementation: the Documentation Updates in Section 15
  may proceed.
- No architecture or Contract Design document is modified at any point
  during this Unit's implementation (Section 15).
- This Unit's own completion does **not** close `IMPLEMENTATION_GAPS.md`
  #53 — it narrows the "construction" half of item 1 only. The companion
  `ReplyDeliveryCoordinator` Unit remains required before gap #53 item 1
  can be considered fully closed.

## 18. Scope Lock

**Not yet locked.** Restating this program's own established two-step
process: this Plan defines the boundary; a separate, explicit human
instruction ("Scope Lock has been achieved") is required before any
Kotlin is written against it, per PES-001's Human-primary-authority model
for Stage 3 through Stage 5.

**What becomes frozen once locked:** exactly the file list in Section 3,
the dependency list in Section 4, the statelessness invariant in Section
4a, the Reply pass-through invariant in Section 4b, the exception
propagation invariant in Section 4c, the composition-only invariant in
Section 4d, the six Required Implementation Decisions in Section 5, the
testing strategy in Section 7, the Out-of-Scope list in Section 9, the
proposed signature in Section 10, and the field mapping in Section 11.
Any change to any of these after Scope Lock requires a new planning pass,
not a silent adjustment during implementation.

## Conclusion

**This document defines one Stage 3 Implementation Plan for the smallest
safe unit that composes a `ReasoningProviderResponse.Reply` into an
`OutboundParkerResponse`.** No existing component is modified; a single
new, small, non-interface-backed class, `ResponseComposer`, is added, with
exactly one dependency (`IdentityService`) and one public method
(`compose`), returning `GatedOutcome<OutboundParkerResponse>` — the
existing, generic, already-documented-as-reusable result type, not a new
one. The composer's statelessness (Section 4a), its obligation to never
mutate or reinterpret its inputs (Section 4b), its single disclosed
exception path — reachable only from the `Reply` branch, exactly when a
response is about to be constructed, per the Third revision note's
correction (Section 4c) — and its composition-only discipline — never
reasoning, planning, authorising, or delivering (Section 4d) — are all
stated as explicit invariants, each enforced structurally by a dedicated
test, not left to be assumed. `ResponseDelivery` is explicitly
excluded as a dependency; compatibility with it is proven by a dedicated
test external to the composer itself, not by the composer performing the
call. A separate governance track's alternative design
(`ReplyDeliveryCoordinator`, constructing `OutboundParkerResponse`
directly and calling `ResponseDelivery` itself) was withdrawn in full
before implementation began, and this document's own design is confirmed
canonical (Second revision note, above). This Plan does not implement
anything itself, does not modify any architecture or Contract Design
document, and does not touch `IMPLEMENTATION_GAPS.md` or
`IMPLEMENTATION_HISTORY.md`. It awaits an explicit Scope Lock instruction
before any Kotlin is written.

## Related

- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md`
- `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`
- `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#53)
- `docs/implementation/COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md` (direct structural precedent for Decisions 2 and 4)
- `docs/implementation/CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` (direct precedent for Decision 2, operating-identity pattern)
- `src/interfaces/ReasoningProvider.kt`, `src/interfaces/ConversationEngine.kt`, `src/interfaces/CommunicationIntake.kt`
- `src/runtime/ConversationTurnReasoningCoordinator.kt`, `src/runtime/CommunicationConversationCoordinator.kt`
- `src/runtime/ResponseDelivery.kt`, `src/runtime/InMemoryConversationEngine.kt`, `src/runtime/InMemoryPlannerRuntime.kt`, `src/runtime/GatedOutcome.kt`
- `RECONCILIATION_ADDENDUM_RESPONSE_COMPOSER_VS_REPLY_DELIVERY_COORDINATOR.md` (Option C, canonical-design determination)
- `REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md` (companion Unit, this Plan's own downstream caller)
- `docs/implementation/SPRINT_9_HANDOVER.md` (Section 3, the Sprint 10 candidate identification this Plan's title now reflects — Fourth revision note, above)
