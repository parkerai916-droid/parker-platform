# Communication-to-Conversation Wiring Implementation Plan (Sprint 7, Unit C2)

## Status

**Stage 3 Implementation Plan, PES-001. Not yet Scope Locked.** No Kotlin
is written by this document. Restating PES-001's own Stage 3 definition
directly: "Break architectural work into independently verifiable
implementation units. Each unit defines: Included work, Excluded work,
Dependencies, Acceptance Criteria, Unit Stop Conditions." This document
performs exactly that breakdown for one unit — no more.

**Grounded exclusively in the as-built code and the documents that
authorised it**, all already accepted/verified earlier this Sprint:

1. `src/interfaces/CommunicationIntake.kt`, `src/runtime/InMemoryCommunicationIntake.kt`
   (Sprint 7, Unit C1 — as built, not re-derived from architecture here).
2. `src/interfaces/ConversationEngine.kt`, `src/runtime/InMemoryConversationEngine.kt`
   (Sprint 7 Conversation Engine unit — as built).
3. `src/interfaces/ReasoningProvider.kt` (Sprint 7 Conversation Engine
   unit — as built).
4. `src/runtime/ConversationTurnReasoningCoordinator.kt` (Sprint 7
   Conversation Engine unit — as built).
5. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`, `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md`,
   `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`,
   `docs/architecture/19-conversation-engine.md` — the accepted Contract
   Designs and Architecture governing each side of this wiring.
6. `docs/architecture/IMPLEMENTATION_GAPS.md` #53, as most recently
   updated — the gap this Unit narrows.
7. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001) — Stage
   3's own definition, above.

**A self-consistency note on gap #53's own text.** Gap #53's most recent
update (recorded by the prior Unit) named, as recommended next steps,
"(i) a Contract Design pass connecting `CommunicationIntake`'s accepted
messages to `ConversationEngine.submitTurn`." That was a *recommendation*,
explicitly marked as "not a decision." This Plan proceeds directly to
Stage 3 instead, per this task's own explicit instruction, and that
choice is defensible on the same grounds the prior Conversation Engine
Implementation Plan already established and had accepted: combining
already-approved contracts through a small, concrete, non-interface-backed
coordinator that introduces no new field-shaped public contract type does
not cross PES-001 Stage 2A's own "Required when" threshold. Section 5,
Decision 1 below applies that same reasoning to this Unit's one small new
type, explicitly, rather than silently assuming it. If that reasoning is
rejected at Scope Lock, this Plan should be sent back for a Contract
Design pass instead — this document does not itself settle which reading
is correct, only names the two paths honestly.

---

## Pre-Scope-Lock Review Response

Eight observations were raised on this Plan across four rounds of
review. Addressed here, before Scope Lock, rather than silently folded
in:

1. **Confirmed; no change required.** Not modifying
   `InMemoryCommunicationIntake` was affirmed as this Plan's strongest
   decision — it keeps `CommunicationIntake` exactly what its contract
   says it is, an intake boundary, never a workflow orchestrator. The
   resulting five-component layering (`CommunicationIntake` →
   `CommunicationConversationCoordinator` →
   `ConversationTurnReasoningCoordinator` → `ConversationEngine` →
   `ReasoningProvider`, each with exactly one job) is unchanged.
2. **Addressed by revising Decision 1 (Section 5).** The question posed:
   can the rejection path be represented without introducing a new
   public outcome type, while still preserving the rejection reason? Four
   options were checked honestly (throwing; a nullable response; reusing
   `CommunicationIntakeDisposition` unmodified; `kotlin.Result`) — each
   fails, either by dropping the reason or by breaking an established
   precedent (detail retained in Decision 1 itself, below). The honest
   answer is **no**. But the deeper concern raised alongside the
   question — a bespoke wrapper type accumulating at every future
   coordination layer (`CommunicationConversationOutcome`,
   `ConversationPlannerOutcome`, `PlannerExecutionOutcome`, ...) — is the
   more important one, and is now solved directly: Decision 1 no longer
   proposes a bespoke, single-purpose type. It proposes one small,
   generic wrapper instead.
3. **Addressed by adding an explicit statelessness invariant**
   (Section 4a), a corresponding structural test (Section 6), and a
   corresponding acceptance criterion (Section 7): the coordinator owns
   no state between invocations and must remain a pure orchestration
   component, stated explicitly rather than left to be assumed.
4. **Addressed by correcting Decision 1's own wording, and by naming
   `GatedOutcome<T>`'s abstraction explicitly, in two parts:**
   - **Reuse is a suitability claim, not a prediction.** The original
     draft said `GatedOutcome<T>` is "intended for reuse by every future
     coordinator with this same shape" — a design prediction stated as
     though it were architectural fact, before the abstraction has
     appeared more than once. Corrected to: `GatedOutcome<T>` **is
     suitable for reuse** by future coordinators with the same gating
     semantics; **future units should reuse it where appropriate** rather
     than introducing an equivalent wrapper type of their own. This
     leaves reuse as an engineering decision at the point it is actually
     needed, consistent with this program's own established rejection of
     unvalidated speculative generality elsewhere (`IMPLEMENTATION_GAPS.md`
     #22's "100,000-line test"; `PRE_MODULE_ID_MULTIPLICITY_DECISION.md`'s
     identical deferral).
   - **`GatedOutcome<T>` is a generic upstream-admission-gate
     abstraction, not a Communication abstraction, and this document now
     says so explicitly.** It models "admit this unit of work, producing
     one value, or reject it with a reason" — not "the message was
     accepted." Nothing in its shape ever referred to messages or
     channels; only this document's surrounding prose did, and that
     framing has been corrected in Decision 1, below. This matters because
     every future admission point this platform is likely to grow (Tool
     Invocation, a Webhook, a Scheduler, an Agent, a Workflow, a Plugin)
     will face the identical admit-or-reject question `CommunicationIntake`
     already answers today — and `GatedOutcome<T>`'s genuine generality
     should not be obscured by describing it in this one caller's own
     vocabulary.
5. **Addressed by adding an explicit Message Pass-Through Invariant**
   (Section 4b), 4a's direct sibling: 4a forecloses state accumulated
   *across* calls; this invariant forecloses alteration *within* a single
   call. The coordinator must never mutate or reinterpret an accepted
   `InboundOwnerMessage` — it sequences only. Enforced structurally in
   Section 6 by a full field-level equality assertion between the
   original message and what the downstream call ultimately receives, not
   merely a claim that "some `Turn`" was produced.
6. **Addressed by adding an explicit exactly-once-invocation acceptance
   criterion** (Section 7), with a corresponding call-count test
   (Section 6): a single accepted message causes exactly one call to
   `CommunicationIntake.submitInboundMessage` and exactly one call to
   `ReasoningProvider.reason`; a single rejected message causes zero
   calls to the latter. No retry, loop, batching, or duplicate-invocation
   behaviour exists, verified by exact counts, not by inspection — and
   structurally impossible in the first place, since the coordinator's
   own method signature accepts exactly one message, never a collection.
7. **Addressed by adding an explicit Exception Propagation Invariant**
   (Section 4c): the coordinator must not recover from, translate, retry,
   or suppress an exception thrown by either dependency — such failures
   propagate unchanged to the caller. This closes a real gap: the
   document thoroughly covered *rejection* (an expected, structural
   outcome) without separately covering *runtime failure* (an
   unexpected fault), and the two must never collapse into one — a
   `try { ... } catch (...) { return NotAccepted(...) }` would silently
   convert a system failure into an admission rejection, which a caller
   reading `GatedOutcome.NotAccepted` has no way to distinguish from a
   genuine structural rejection. Enforced in Section 6 by tests in which
   each dependency throws and the coordinator's own method is asserted
   to propagate that exact exception unchanged.
8. **Addressed by strengthening the exactly-once acceptance criterion**
   (Section 7) to be explicitly unconditional: no `ReasoningProviderResponse`
   variant — `NoAction`, `Goal`, or `Reply` — may ever cause a second
   call to `ReasoningProvider.reason`. This forecloses a plausible-looking
   future change ("if `NoAction` comes back, maybe ask the model again")
   that would violate this Unit's stop condition without looking
   obviously wrong at the point someone makes it. Retry policy of any
   kind is now named explicitly in Section 8's Out-of-Scope list, not
   merely absent by omission.

---

## Required Analysis

### 1. Which runtime currently owns the point where an accepted `InboundOwnerMessage` ends?

`InMemoryCommunicationIntake.submitInboundMessage` (`src/runtime/InMemoryCommunicationIntake.kt`).
On acceptance, it appends the message to an internal, `Mutex`-guarded
list and returns `CommunicationIntakeDisposition.Accepted(correlationId,
message)`. Nothing else happens. The only way to observe an accepted
message afterward is `acceptedMessages()`/`acceptedMessageFor(correlationId)`
— both explicitly documented as "observability methods outside the formal
`CommunicationIntake` interface... not a queue-consumption API for a real
consumer." No production code anywhere in this repository calls either
method or otherwise consumes an accepted message. This is precisely the
open end `IMPLEMENTATION_GAPS.md` #53 names.

### 2. Exactly where should the Conversation Engine be injected?

**Not inside `InMemoryCommunicationIntake`.** `COMMUNICATION_CONTRACT_DESIGN.md`
Section 9 fixes `CommunicationIntake`'s dependency list at exactly two
collaborators — `ModuleRegistry` and `IdentityService` — and
`InMemoryCommunicationIntake`'s own KDoc states plainly that engaging any
further subsystem "is Cognition's own decision, downstream of an accepted
message — not `CommunicationIntake`'s." Adding a `ConversationEngine`
dependency there would modify an existing, already-tested, already-shipped
class outside this Unit's authority and outside Contract Design's own
boundary.

**Injected at a new point, above both existing components.** A new,
small, standalone class (Section 5, Decision 2) sits between
`CommunicationIntake` and the existing `ConversationTurnReasoningCoordinator`,
calling the former first and, only on `Accepted`, calling the latter. This
mirrors exactly how `ConversationTurnReasoningCoordinator` itself was
injected between `ConversationEngine` and `ReasoningProvider` in the prior
Unit — a new composition point, not a modification to either side.

### 3. Whether an existing helper can be reused

**Yes, in full, on both sides:**

- `ConversationTurnReasoningCoordinator.submitTurnAndReason` already
  performs the exact `ConversationEngine.submitTurn` →
  `ReasoningProvider.reason` sequence this Unit's own downstream half
  needs. It is reused unchanged — no modification, no subclassing.
- `CommunicationIntake.submitInboundMessage` already performs the
  exact structural acceptance check this Unit's own upstream half needs.
  It is reused unchanged, through its existing interface type (not the
  concrete `InMemoryCommunicationIntake` class), preserving testability
  with the existing `tests/runtime/FakeCommunicationIntake.kt` fixture.

No existing class needs a new method, a new constructor parameter, or any
behavioural change.

### 4. Whether a new coordinator is required

**Yes.** Nothing in this repository currently calls
`ConversationTurnReasoningCoordinator.submitTurnAndReason` with a
`CommunicationIntake`-accepted message — the two components exist, are
each independently tested, and are simply not wired to each other. A new,
small coordinator (Section 5, Decision 2) is required to bridge them. This
is the smallest safe unit that closes that specific gap: it introduces no
new behaviour on either side, only sequences two already-correct
components.

### 5. Constructor dependency changes required

**None, to any existing class.** `CommunicationIntake`,
`InMemoryCommunicationIntake`, `ConversationEngine`,
`InMemoryConversationEngine`, `ReasoningProvider`, and
`ConversationTurnReasoningCoordinator` all keep their exact current
constructor signatures. The new coordinator's own constructor accepts
exactly two dependencies, both already-existing interface/class types:
`CommunicationIntake` and `ConversationTurnReasoningCoordinator` (Section
4, Section 5 Decision 3).

### 6. Identity requirements

**None new.** `CommunicationIntake`'s sender-resolution check
(`IdentityService.resolve(message.senderPrincipalId)`) already ran before
acceptance. `InMemoryConversationEngine.submitTurn` already resolves its
own operating identity (`system.conversation-engine`) before acting,
unchanged by this Unit. The new coordinator introduced here has no
operating identity of its own — it performs no `IdentityService` call,
holds no Principal constant, and makes no accountability claim beyond
sequencing two already-identity-checked calls. No new
`IdentityService.register` requirement is introduced anywhere.

### 7. EventBus implications (if any)

**None.** Neither `CommunicationIntake`, `ConversationEngine`,
`ReasoningProvider`, nor `ConversationTurnReasoningCoordinator` publishes
or subscribes to `EventBus` today. This Unit's new coordinator likewise
publishes nothing and subscribes to nothing. `conversation.*`/`reasoning.*`
event publication remains exactly as authorised-but-not-required as the
respective Architecture documents already left it — this Unit does not
change that state.

### 8. Whether this changes any existing runtime lifecycle

**No.** `CommunicationIntakeDisposition` (`Accepted`/`Rejected`),
`ConversationDisposition`, and `ReasoningProviderResponse`
(`Goal`/`Reply`/`NoAction`) are all consumed exactly as they already exist
— no field added, removed, or reinterpreted. No `ModuleStatus`,
`PrincipalStatus`, or any other lifecycle enum is touched. The new
coordinator itself has no internal state and therefore no lifecycle of
its own — a single, stateless, sequential call per invocation.

### 9. Test strategy

See Section 6 below (full detail). Summary: a new
`tests/runtime/CommunicationConversationCoordinatorTest.kt`, reusing the
existing `FakeCommunicationIntake` and `FakeReasoningProvider` fixtures
unchanged, covering the accepted path (both calls happen, in order, with
the correct data threaded through), the rejected path (the downstream
coordinator is never invoked, and the rejection reason is preserved,
observable), and a structural test proving the new coordinator's
constructor has no slot for any prohibited subsystem.

### 10. Files to be modified

**All additions. No existing file (`src/` or `tests/`) requires
modification.** See Section 3 for the complete list.

---

## 1. Objective

Implement, and make independently testable, the smallest coordinator that
connects the already-implemented Communication Runtime to the
already-implemented Conversation Engine + Reasoning Provider unit:

```
Owner Message
    ↓
Communication Runtime (CommunicationIntake.submitInboundMessage)
    ↓
ConversationEngine.submitTurn(...)      [via ConversationTurnReasoningCoordinator, reused unchanged]
    ↓
ReasoningProvider.reason(...)
    ↓
stop
```

If `CommunicationIntake` rejects the message, the coordinator stops there
and never reaches `ConversationEngine` or `ReasoningProvider` at all. If it
accepts the message, the coordinator delegates to the existing
`ConversationTurnReasoningCoordinator` unchanged and returns whatever
`ReasoningProviderResponse` results. **This is the unit's stop condition,
restated from the task's own instruction: nothing routes a `Goal` onward
to Planner Runtime, nothing routes a `Reply` onward to Response Delivery,
and nothing else happens.**

## 2. Included Work

- One new, small, standalone, non-interface-backed coordinator (proposed
  name: `CommunicationConversationCoordinator`, Section 5 Decision 2) with
  exactly one public method, that:
  1. Calls `CommunicationIntake.submitInboundMessage(message)`.
  2. If the result is `CommunicationIntakeDisposition.Rejected`, stops
     immediately and returns an outcome carrying the rejection reason
     unchanged — `ConversationEngine` and `ReasoningProvider` are never
     called.
  3. If the result is `CommunicationIntakeDisposition.Accepted`, calls
     `ConversationTurnReasoningCoordinator.submitTurnAndReason(disposition.message, reasoningContext)`
     unchanged, and returns an outcome carrying the resulting
     `ReasoningProviderResponse`.
- One new, small, generic upstream-admission-gate wrapper (proposed name:
  `GatedOutcome<T>`, Section 5 Decision 1) representing exactly two
  cases — `Produced(value: T)` and `NotAccepted(reason: String)` — used
  here as `GatedOutcome<ReasoningProviderResponse>`, suitable for reuse by
  future coordinators with the same gating semantics, not scoped to this
  Unit alone.
- Unit tests for the coordinator, per Section 6.

## 3. Files Expected to Change

All additions. **No existing `src/` or `tests/` file requires
modification** — `CommunicationIntake`, `InMemoryCommunicationIntake`,
`ConversationEngine`, `InMemoryConversationEngine`, `ReasoningProvider`,
and `ConversationTurnReasoningCoordinator` are all consumed exactly as
they exist today.

| File | New/Modified | Contents |
| --- | --- | --- |
| `src/runtime/GatedOutcome.kt` | New | `GatedOutcome<T>` (sealed: `Produced<T>`, `NotAccepted`) — a small, generic upstream-admission-gate wrapper, suitable for reuse beyond this Unit's coordinator (Section 5, Decision 1). |
| `src/runtime/CommunicationConversationCoordinator.kt` | New | The `CommunicationConversationCoordinator` class described in Section 2, returning `GatedOutcome<ReasoningProviderResponse>`. |
| `tests/runtime/CommunicationConversationCoordinatorTest.kt` | New | Tests per Section 6, reusing `FakeCommunicationIntake` and `FakeReasoningProvider` unchanged. |

No new file is added under `src/interfaces/` or `tests/contracts/` — the
new result type is a generic implementation-level utility, not a public
contract (Section 5, Decision 1).

## 4. Dependencies

**The new coordinator's only dependencies: `CommunicationIntake` and
`ConversationTurnReasoningCoordinator`, both constructor-injected, both
already-existing types, neither modified.**

This is the Unit's primary structural enforcement mechanism, not merely
an assertion (mirroring the prior Unit's own identical framing): the new
coordinator's constructor has no slot for `PlannerRuntime`,
`ExecutionPipeline`, `PermissionEngine`, `ToolRegistry`, `MemoryStore`, or
`WorldModel` — and because both of its actual dependencies are themselves
already structurally proven to lack any such slot (`CommunicationIntake`
per `COMMUNICATION_CONTRACT_DESIGN.md` Section 9; `ConversationTurnReasoningCoordinator`
per `CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` Section 4), the new
coordinator inherits that same guarantee transitively — there is nothing
to call because there is nothing reachable, at any depth, to call it with.

No dependency on `ModuleRegistry` or `IdentityService` directly — both are
already reached, exactly once each, inside `CommunicationIntake` and
`InMemoryConversationEngine` respectively; the new coordinator does not
re-check or duplicate either.

## 4a. Statelessness Invariant

**The coordinator owns no state between invocations. It must remain a
pure orchestration component.** Stated explicitly here, not left to be
assumed obvious: `CommunicationConversationCoordinator` holds exactly its
two constructor-injected dependencies (Section 4) as its only fields — no
`var`, no mutable collection, no cache of the last `Conversation`, `Turn`,
`InboundOwnerMessage`, or `ReasoningProviderResponse` it handled, and no
`Mutex` (a `Mutex` would itself be evidence of state worth guarding,
which this class must not have). Each call to its one public method must
be fully independent of every other call — two concurrent or sequential
invocations for different (or the same) owners must never observably
interact.

**Why this is named explicitly rather than left implicit.** Every field
this class could plausibly be tempted to add later — "the last
`Conversation` seen," "a per-owner cache to skip redundant
`CommunicationIntake` calls," "a short-lived buffer to batch reasoning
calls" — would silently reintroduce exactly the kind of continuity state
`19-conversation-engine.md` and `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`
already assign, deliberately and solely, to `ConversationEngine` itself
(Architecture Section 4's "sole owned state" framing). A stateful
coordinator would not just be untidy — it would create a second, informal,
untested place Conversation continuity could diverge from the one the
architecture actually authorises, the exact failure mode of "conversations
mysteriously bleed into one another" this invariant exists to foreclose.
This is enforced structurally in Section 6 (a reflective test asserting
the class declares no field beyond its two constructor-injected
dependencies), not only stated in KDoc.

## 4b. Message Pass-Through Invariant

**The coordinator must never mutate or reinterpret an accepted
`InboundOwnerMessage`. It sequences only.** Stated explicitly here, as
4a's direct sibling: 4a forecloses state accumulated *across* calls; this
invariant forecloses alteration *within* a single call.

Concretely: the coordinator receives an `InboundOwnerMessage`, passes it
unchanged to `CommunicationIntake.submitInboundMessage`, and — on
`Accepted` — passes `disposition.message` (the message `CommunicationIntake`
itself returned, per Section 6's existing "Accepted message identity"
test) unchanged into `ConversationTurnReasoningCoordinator.submitTurnAndReason`.
It must never construct a new `InboundOwnerMessage`, never call `.copy()`
to alter any field (`channelId`, `senderPrincipalId`, `text`, `timestamp`,
`correlationId`, `metadata`), and must never read `message.text` to make
any branching or interpretive decision of its own. This restates, one
layer further out, the same rule `CommunicationIntake`'s own contract
already states for itself ("does not interpret `message.text`, does not
decide whether the message implies an action") — the new coordinator
inherits that discipline rather than being newly exempt from it just
because it sits outside `CommunicationIntake`'s own class boundary.

**Why this is named explicitly rather than left implicit.** The
coordinator sits directly between two components that each already,
independently, guarantee message identity is preserved end-to-end —
`CommunicationIntake` returns the accepted message unchanged;
`ConversationTurnReasoningCoordinator`'s own `Turn` wraps the message
unchanged. A future maintainer touching only this coordinator, and
reasoning about it in isolation, could plausibly believe "this is just
glue, I can trim/normalise/redact something here" without realising doing
so would silently break an identity guarantee both of its neighbours
already depend on, and that no test anywhere else in this codebase is
positioned to catch, since neither neighbour's own tests exercise this
coordinator's code. This is enforced structurally in Section 6 (a test
asserting full field-level equality between the original message and
whatever `Turn.message` the downstream call ultimately received), not
only stated in KDoc.

## 4c. Exception Propagation Invariant

**The coordinator must not recover from, translate, retry, or suppress
exceptions thrown by either dependency. Such failures propagate
unchanged to the caller and are outside this Unit's responsibility.**
Stated explicitly here because this document has, until now, thoroughly
covered *rejection* (`CommunicationIntakeDisposition.Rejected`,
represented as `GatedOutcome.NotAccepted`) without separately covering
*runtime failure* — and the two are architecturally different things,
not two names for the same outcome. Rejection is `CommunicationIntake`'s
own considered, expected, structural answer to a routine condition (a
disabled channel, an unresolvable sender); an exception thrown by either
dependency is an unexpected fault this coordinator has no basis to
interpret, categorise, or paper over.

Concretely: this coordinator must contain no `try`/`catch` of any kind
around either dependency call. In particular, it must never do this:

```
try {
    ...
} catch (e: Exception) {
    return GatedOutcome.NotAccepted(...)
}
```

Doing so would silently convert a system failure into an admission
rejection — collapsing two categories `CommunicationIntakeDisposition`
itself already keeps separate (a routine `Rejected` outcome versus
whatever `IdentityService`/`ModuleRegistry` faults `CommunicationIntake`'s
own implementation does not itself catch either) into one, at exactly the
layer meant only to sequence, not to reinterpret. A caller that receives
`GatedOutcome.NotAccepted` must be able to trust that it means "this
message was structurally rejected," never "something downstream broke and
this coordinator hid it."

This is enforced in Section 6 by a test in which a dependency throws, and
the coordinator's own method is asserted to propagate that exact
exception unchanged — not a `GatedOutcome` of any variant, not a
different exception type, not a swallowed/logged-and-continued failure.

## 5. Required Implementation Decisions

Mirroring this program's own established practice (the `IDR-001`
precedent; the two prior Conversation Engine Plan decisions this Unit
directly continues), three genuine interpretive forks exist that no
existing document resolved, each named here with a proposed, conservative
default, awaiting confirmation before Kotlin proceeds.

### Decision 1 — Shape of the new result type, and why it does not require a Contract Design pass

Neither `CommunicationIntakeDisposition` nor `ReasoningProviderResponse`
has a shape that represents "this message was rejected before reasoning
ever ran" versus "this message produced a reasoning outcome." Silently
dropping the rejection reason (for example, by returning a nullable
`ReasoningProviderResponse?`) would lose real, meaningful information this
codebase's own discipline elsewhere never drops (compare: `task.started`'s
"if any" `agentRunId` handling, `CommunicationIntakeDisposition.Rejected.reason`
itself).

**Four alternatives to a new type were checked explicitly, and each was
rejected:**

- **Throw on rejection.** Rejected: `CommunicationIntake`'s own KDoc is
  explicit that rejection is "an expected, routine outcome of ordinary
  channel lifecycle timing... not a caller-misuse condition," which is
  precisely why it returns a sealed disposition rather than throwing.
  Throwing at this layer would misrepresent that same routine condition
  as caller misuse, one layer up.
- **Return `ReasoningProviderResponse?` (nullable).** Rejected: avoids a
  new type but silently discards the rejection reason, which this Plan
  treats as real, meaningful information, not incidental detail.
- **Reuse `CommunicationIntakeDisposition` unmodified.** Rejected:
  structurally impossible — its `Accepted` variant has no field to carry
  a `ReasoningProviderResponse`, and adding one would mean modifying an
  already-accepted Contract Design type, outside this Plan's authority.
- **`kotlin.Result<ReasoningProviderResponse>`.** Rejected: avoids a new
  *Parker* type, but this codebase has zero existing precedent for it —
  every other "expected, non-exceptional outcome" in this repository
  (`TaskProposalDisposition`, `CommunicationIntakeDisposition`,
  `ObservationResult`, `ConversationDisposition`, `PlanningSessionResult`,
  ...) is a custom sealed class. Introducing `Result` here would add a
  second, inconsistent idiom for the same concept, not reduce type
  proliferation.

**None of the four preserves the rejection reason without either
breaking precedent or requiring out-of-scope modification.** A new type
is genuinely required. But the deeper concern this raises — a bespoke,
single-purpose wrapper type accumulating at every future coordination
layer this program builds (a `ConversationPlannerOutcome`, a
`PlannerExecutionOutcome`, and so on, each translated between by hand) —
is real, and is addressed directly rather than deferred:

**Proposed default: one small, generic, reusable wrapper, not a bespoke
type for this coordinator alone.**

```
sealed class GatedOutcome<out T> {
    data class Produced<out T>(val value: T) : GatedOutcome<T>()
    data class NotAccepted(val reason: String) : GatedOutcome<Nothing>()
}
```

**What `GatedOutcome<T>` models, stated precisely and without borrowing
Communication-specific vocabulary:** an upstream gate that either admits
work, producing exactly one value of type `T`, or rejects it, with a
reason. `Produced` carries whatever the downstream step produced;
`NotAccepted` carries the upstream rejection reason, unchanged. Nothing
in its shape, its two variant names, or its type parameter refers to
messages, channels, or Communication at all — it was written generically
from the start (Decision 1's own first draft already gave it a bare type
parameter and a plain `reason: String`), and this document now names that
generality explicitly rather than leaving it to be inferred from context.

**On reuse: suitable, not predicted.** `GatedOutcome<T>` is **suitable
for reuse by future coordinators with the same gating semantics** — a
structural gate that either blocks a downstream step (with a reason) or
admits input to a step that produces exactly one value. **Future units
should reuse it where appropriate rather than introducing an equivalent
wrapper type of their own**, but this Plan does not claim, and Scope Lock
should not read it as claiming, that such reuse will in fact occur. This
program's own established discipline elsewhere (rejecting "unvalidated
speculative generality" under the "100,000-line test," `IMPLEMENTATION_GAPS.md`
#22; deferring ID multiplicity until a real consumer exists,
`PRE_MODULE_ID_MULTIPLICITY_DECISION.md`) already rejects exactly the
move of treating anticipated future reuse as an architectural fact before
it has actually happened more than once. This document does the same
here: reuse is left as an engineering decision for whichever future unit
needs it, not asserted as this Unit's own accomplishment. If
`CommunicationConversationCoordinator` remains, for the foreseeable
future, `GatedOutcome<T>`'s only caller, that is a fully acceptable
outcome — a small, generic, single-purpose-today type costs little to
have, and nothing in this Plan is weakened if it is never reused.

**On scope: this abstraction is not owned by the Communication track.**
Every future admission point this platform is likely to grow —
`Tool Invocation`, a `Webhook`, a `Scheduler`, an `Agent`, a `Workflow`, a
`Plugin` — will eventually face the identical question `CommunicationIntake`
already answers today: admit this unit of work, or reject it with a
reason, before anything downstream runs. `GatedOutcome<T>` models that
general shape, not "Communication acceptance" specifically — for example,
a hypothetical future coordinator could reuse it unchanged as
`GatedOutcome<PlanningSessionResult>` or `GatedOutcome<ExecutionResult>`,
gated by whatever upstream check that coordinator's own admission point
requires, with no dependency on anything Communication-specific. This
Plan does not implement, name, or authorise any of those future admission
points — it only documents, honestly, that the type it is adding here
does not conceptually belong to Communication, so a future unit is not
left to rediscover that on its own. `GatedOutcome` lives in
`src/runtime/GatedOutcome.kt` — its own small file, not bundled into this
coordinator's file, precisely because it is not specific to this Unit.

**Why this still does not require a Stage 2A Contract Design pass,
restated explicitly (per the Status section's self-consistency note
above):** `GatedOutcome<T>` is a generic implementation-level utility, not
a domain contract — it carries no Parker-specific field, names no
Parker-specific concept, and is not itself a candidate for appearing in
any accepted Architecture or Contract Design document (compare:
`Mutex`/`withLock`, already used throughout this codebase's `Runtime`
classes without any Contract Design of their own). It is not shared across
an architectural trust boundary; the type parameter `T` is filled in
independently by each coordinator that uses it. This mirrors the same
reasoning `CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` Section 5 Decision
2 already used and had accepted for `ConversationTurnReasoningCoordinator`
introducing no new Contract Design surface — generalised here one step
further, from "introduces no new contract type" to "introduces no new
type at all, beyond a reusable, content-free wrapper shape." If this
reasoning is rejected at Scope Lock, the smallest correction is to reduce
this to a plain nullable `ReasoningProviderResponse?` (discarding the
rejection reason) rather than to open a Contract Design pass for a type
this generic and this narrow — but that smaller correction is not this
Plan's proposed default, since it discards real information for no
structural reason.

### Decision 2 — Shape of the new coordinator

Whether the code that sequences `CommunicationIntake.submitInboundMessage`
and `ConversationTurnReasoningCoordinator.submitTurnAndReason` is a small
standalone class, a top-level function, or a method added to one of the
two existing components, is not decided by any existing document.

**Proposed default: a small, standalone class** (proposed name:
`CommunicationConversationCoordinator`), constructor-injected with a
`CommunicationIntake` and a `ConversationTurnReasoningCoordinator`,
exposing one method that performs the sequence in Section 2. This is
preferred over adding a method to either existing component for the same
reason `CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` Section 5 Decision 2
already gave: it keeps each existing component exactly matching its own
already-contracted, single-purpose shape, and keeps this Unit's own
dependency list (Section 4) visibly minimal and separately testable.

**This decision is made explicit, not left implicit: the new coordinator
is intentionally not interface-backed, for the identical reason
`ConversationTurnReasoningCoordinator` itself is not** — it sequences two
already-shaped components and introduces no new field, no new domain
concept beyond Decision 1's narrow result type, and no state of its own.
**If a future caller needs to depend on this coordinated behaviour
abstractly, that need must be met by a later Contract Design pass or an
explicit, disclosed additive-interface decision at that time — not by
silently promoting this Unit's own concrete class into a public
contract.**

### Decision 3 — Reuse `ConversationTurnReasoningCoordinator` whole, rather than re-deriving its sequence

An alternative design would give the new coordinator its own direct
`ConversationEngine` and `ReasoningProvider` dependencies and re-implement
the `submitTurn` → `reason` sequence itself, bypassing
`ConversationTurnReasoningCoordinator` entirely.

**Proposed default: reuse `ConversationTurnReasoningCoordinator` as a
single, opaque dependency, unchanged.** This avoids duplicating sequencing
logic that is already implemented and already tested, keeps this Unit's
own dependency list at exactly two entries instead of three, and means
any future change to how a Turn is bound to reasoning (within
`ConversationTurnReasoningCoordinator`'s own existing scope) is
automatically reflected here without this Unit's own code changing. The
new coordinator therefore has **no direct dependency on `ConversationEngine`
or `ReasoningProvider` at all** — only on `ConversationTurnReasoningCoordinator`,
which itself already depends on both.

## 6. Testing Strategy

**`CommunicationConversationCoordinatorTest.kt`:**

- **Accepted path.** Given a `FakeCommunicationIntake` configured to
  return `CommunicationIntakeDisposition.Accepted(correlationId, message)`,
  and a `ConversationTurnReasoningCoordinator` built from a
  (fake-or-real) `ConversationEngine` and a `FakeReasoningProvider`
  configured to return `Goal("do the thing")`: the new coordinator calls
  `CommunicationIntake.submitInboundMessage` first, then reaches
  `ConversationTurnReasoningCoordinator.submitTurnAndReason` with exactly
  the accepted message and the caller-supplied `ReasoningContext`, and
  returns `GatedOutcome.Produced(Goal("do the thing"))`.
- Same test repeated for a `Reply` response and a `NoAction` response —
  each passes through unchanged, wrapped in `Produced`.
- **Rejected path.** Given a `FakeCommunicationIntake` configured to
  return `CommunicationIntakeDisposition.Rejected(correlationId, "channel
  not enabled")`: the new coordinator returns
  `GatedOutcome.NotAccepted("channel not enabled")`, and
  `FakeReasoningProvider.reasonCallCount` remains `0` — the **structural
  proof that a rejected message never reaches `ConversationEngine` or
  `ReasoningProvider` at all**, not merely that its result is discarded.
- **Accepted message identity / pass-through invariant (Section 4b).**
  The `Turn` produced downstream wraps `disposition.message` (the message
  `CommunicationIntake` itself returned as accepted), not a
  separately-held reference to the original input — verified even though
  the two are expected to be identical today, so a future
  `CommunicationIntake` implementation that ever normalises a message
  before accepting it cannot silently desynchronise this coordinator from
  what was actually accepted. Asserted by full field-level equality
  (`channelId`, `senderPrincipalId`, `text`, `timestamp`, `correlationId`,
  `metadata`) between the message supplied to the coordinator's own test
  input and `Turn.message` as ultimately received downstream — not merely
  reference equality, and not merely that some `Turn` was produced — the
  direct structural proof that Section 4b's "sequences only, never
  mutates or reinterprets" invariant holds.
- **Exactly-once invocation, no retries, loops, batching, or duplicate
  calls.** For a single call to the coordinator's own method:
  `FakeCommunicationIntake.submitInboundMessageCallCount` is exactly `1`,
  and `FakeReasoningProvider.reasonCallCount` is exactly `1` (the latter
  transitively proving `ConversationTurnReasoningCoordinator.submitTurnAndReason`
  itself ran exactly once, since that coordinator's own already-verified
  behaviour calls `reason` exactly once per call). Both counts are
  re-checked after the rejected-path test (Section 6, above) to confirm
  they remain `0`/`1` respectively — a rejection must not somehow still
  trigger a downstream call, and an acceptance must not trigger more than
  one. No test constructs the coordinator with more than one message per
  invocation — its method signature accepts exactly one
  `InboundOwnerMessage`, not a list or a batch, so no internal iteration,
  retry-on-first-failure, or batching behaviour is structurally possible
  in the first place, not merely absent from this Unit's own
  implementation choice.
- **No conditional re-invocation based on response content.** The same
  `reasonCallCount == 1` assertion above is checked once more, separately,
  for each of the three `ReasoningProviderResponse` variants
  (`NoAction`, `Goal`, `Reply`) individually — not only for whichever
  variant the earlier pass-through tests (Section 6, above) happened to
  use. This exists specifically to foreclose a plausible-looking future
  change: nothing about `NoAction` (or any other variant) may ever cause
  the coordinator to call `ReasoningProvider.reason` a second time "to
  try again" or "to ask for a more useful answer." Retry policy of any
  kind — content-based or otherwise — belongs to a future architecture
  this Unit does not design, and is explicitly outside its scope
  (Section 8).
- **Exception propagation, not recovery (Section 4c).** With
  `FakeCommunicationIntake` configured to throw from
  `submitInboundMessage`, the coordinator's own method call is asserted
  to propagate that exact exception — via `assertFailsWith` on the
  thrown type — not to return any `GatedOutcome` variant. The identical
  test is repeated with `FakeReasoningProvider` configured to throw from
  `reason` instead. Neither test constructs, expects, or tolerates a
  `try`/`catch` anywhere in the coordinator's own code.
- **Negative/structural test.** The new coordinator's own constructor
  accepts only a `CommunicationIntake` and a
  `ConversationTurnReasoningCoordinator` — there is no dependency slot for
  `PlannerRuntime`, `ExecutionPipeline`, `MemoryStore`, `WorldModel`,
  `ModuleRegistry`, or `IdentityService` to even construct the fixture
  with one. A compile-time property, not a runtime assertion that could
  be accidentally weakened later — mirroring
  `ConversationTurnReasoningCoordinatorTest.kt`'s own identical structural
  test.
- **Statelessness test (Section 4a).** A reflective test asserting
  `CommunicationConversationCoordinator::class.java.declaredFields`
  contains exactly its two constructor-injected dependencies and nothing
  else — no additional field of any kind, mutable or not. A second test
  runs the same `CommunicationConversationCoordinator` instance across two
  independent invocations, for two different owners' messages, and
  asserts the second call's outcome is unaffected by the first (no
  observable interaction between calls).

**`GatedOutcome<T>` (as a shared, generic type, not scoped to this
coordinator's own test file):** minimal construction tests confirming
`Produced` holds its `value` unchanged for any `T`, and `NotAccepted`
rejects a blank `reason` (mirroring this codebase's established
blank-rejecting `init` convention). These may live alongside
`CommunicationConversationCoordinatorTest.kt` or in their own small file
— either is acceptable, since `GatedOutcome` has no dependency of its own
to isolate.

**Full Gradle test suite.** Per this program's own established
discipline, run the complete suite (not only the tests above) once
implementation is complete, and report a real, Android-Studio-verified
result. If the sandbox used to prepare this repository cannot execute
Gradle, report an honest, arithmetic-projected total with an explicit
"not verified" disclosure and wait for external verification before any
`IMPLEMENTATION_HISTORY.md` update (Section 10).

## 7. Acceptance Criteria

- `CommunicationConversationCoordinator` exists, is independently
  constructible with only a `CommunicationIntake` and a
  `ConversationTurnReasoningCoordinator`, and its own tests (Section 6)
  pass.
- `GatedOutcome<T>` has exactly two variants (`Produced`, `NotAccepted`)
  — no `Failed` variant, no third case invented — and is defined in its
  own file, not bundled into the coordinator's, so it is genuinely
  reusable by a future coordinator without a dependency on this Unit's
  own class.
- **The coordinator is stateless (Section 4a).** It declares no field
  beyond its two constructor-injected dependencies, and two independent
  invocations never observably interact — verified structurally
  (Section 6), not merely by inspection.
- **The coordinator never mutates or reinterprets an accepted
  `InboundOwnerMessage` (Section 4b).** Every field of the message
  received downstream (`Turn.message`) is field-for-field identical to
  the message `CommunicationIntake` itself returned as accepted — no
  construction of a new message, no `.copy()`, no branching on
  `message.text` — verified structurally (Section 6), not merely by
  inspection.
- **Exactly one call to each downstream component per invocation — no
  retries, loops, batching, or duplicate invocation, unconditionally.**
  For a single accepted message, `CommunicationIntake.submitInboundMessage`
  is called exactly once and `ReasoningProvider.reason` is called exactly
  once; for a single rejected message, `ReasoningProvider.reason` is
  called zero times. **This holds regardless of which
  `ReasoningProviderResponse` variant comes back** — `NoAction`, `Goal`,
  and `Reply` are all treated identically for invocation-count purposes;
  none of them may ever trigger a second call "to try again" or "to ask
  for a better answer." Retry policy of any kind is explicitly outside
  this Unit (Section 4c, Section 8) — verified by exact call-count
  assertions per variant (Section 6), not merely by inspection.
- **The coordinator never recovers from, translates, retries, or
  suppresses an exception thrown by either dependency (Section 4c).**
  Any such exception propagates to the coordinator's own caller
  unchanged — never converted into a `GatedOutcome.NotAccepted`, never
  swallowed — verified structurally (Section 6), not merely by
  inspection.
- A rejected `InboundOwnerMessage` never reaches `ConversationEngine` or
  `ReasoningProvider` — verified structurally (Section 6), not merely by
  inspection.
- An accepted `InboundOwnerMessage` reaches `ReasoningProvider.reason`
  exactly once, via `ConversationTurnReasoningCoordinator`, unchanged from
  that coordinator's own existing, already-tested behaviour.
- No production (`src/`) code added by this Unit references
  `PlannerRuntime`, `PlanningRequest`, `ExecutionPipeline`,
  `ExecutionRequest`, `OutboundParkerResponse`, `PermissionEngine`,
  `ToolRegistry`, `MemoryStore`, or `WorldModel`, anywhere — verified by
  the dependency list in Section 4 and the structural test in Section 6.
- No existing `src/` or `tests/` file is modified (Section 3).
- All tests listed in Section 6 pass, and the full Gradle suite passes
  (or a projected count is honestly reported, per Section 6's own
  disclosure discipline).

## 8. Implementation Boundaries — Out of Scope

Restating the task's own explicit prohibited list, each grounded in why
it is excluded, not merely named:

- **PlannerRuntime.** No `PlannerRuntime` reference exists anywhere this
  Unit's code can reach (Section 4). A `Goal` response is wrapped in
  `Produced` and returned to the new coordinator's own caller; nothing
  routes it onward.
- **TaskManagerRuntime.** No reference exists anywhere this Unit's code
  can reach; not imported, not referenced.
- **AgentRuntime.** No reference exists anywhere this Unit's code can
  reach; not imported, not referenced.
- **ExecutionPipeline.** No reference exists anywhere this Unit's code
  can reach.
- **Memory Runtime.** No `MemoryStore` reference exists anywhere this
  Unit's code can reach.
- **World Model.** No `WorldModel` reference exists anywhere this Unit's
  code can reach.
- **Response Delivery.** No `Tool`, `ToolRegistry`, or
  `OutboundParkerResponse` reference exists anywhere this Unit's code can
  reach. A `Reply` response is wrapped in `Produced` and returned to the
  new coordinator's own caller; nothing delivers it anywhere.
- **Android.** No Android dependency, API, or lifecycle reference is
  introduced.
- **Speech.** No speech-to-text, text-to-speech, or audio handling is
  introduced.
- **UI.** No user-interface code of any kind is introduced.
- **Persistence.** The new coordinator holds no state of its own between
  calls; nothing it produces is written to disk, a database, or any
  durable store.
- **`ReasoningContext` assembly.** Unchanged from the prior Unit's own
  identical deferral: this Unit's tests and its coordinator's own method
  signature both accept an already-assembled `ReasoningContext` as an
  external input. "Reasoning Context assembly" ownership remains exactly
  as unassigned as `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 9
  leaves it.
- **Any real, model-backed `ReasoningProvider` implementation.** This Unit
  wires existing components together; it does not add a new
  `ReasoningProvider` implementation of any kind.
- **`conversation.*`/`reasoning.*`/any new `EventBus` publication.**
  Restating Required Analysis item 7 — this Unit publishes nothing.
- **Retry policy of any kind.** Whether based on response content
  (`NoAction` prompting a second `reason` call), on a thrown exception, or
  on any other condition, retrying either dependency belongs to a future
  architecture this Unit does not design (Section 4c, Section 7). This
  Unit calls each dependency exactly once per invocation, unconditionally.
- **Exception handling of any kind around either dependency.** No
  `try`/`catch`, no fallback value, no logging-and-continuing. A thrown
  exception is this Unit's caller's problem, not this coordinator's to
  solve (Section 4c).

## 9. Architectural Traceability

| Implemented element | Authorised by |
| --- | --- |
| `CommunicationConversationCoordinator` calling `CommunicationIntake.submitInboundMessage` first | `COMMUNICATION_CONTRACT_DESIGN.md` Section 6 (the interface's own defined public surface, unchanged) |
| Delegating to `ConversationTurnReasoningCoordinator` unchanged, on `Accepted` only | This document, Section 5 Decision 3 — a Stage 3-level implementation choice, not an architectural claim |
| `CommunicationConversationCoordinator`'s non-interface-backed shape | This document, Section 5 Decision 2, mirroring `CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` Section 5 Decision 2's identical, already-accepted reasoning |
| `GatedOutcome<T>`'s generic upstream-admission-gate shape, and its exemption from a Stage 2A pass | This document, Section 5 Decision 1 |
| No routing of `Goal`/`Reply` beyond this Unit's own return value | This task's own explicit instruction: "and then stop," combined with `IMPLEMENTATION_GAPS.md` #53's own still-open routing question |
| Structural (not asserted) trust-boundary enforcement | Both prior Contract Design documents' and the prior Implementation Plan's own identical "structural guarantee, not merely a stated rule" framing |
| The coordinator's statelessness invariant | This document, Section 4a — a review-driven addition, not present in the original draft |
| The coordinator's message pass-through invariant | This document, Section 4b — a review-driven addition, not present in the original draft |
| Exactly-once invocation of each downstream component, no retries/loops/batching, unconditional across all `ReasoningProviderResponse` variants | This document, Section 7 — a review-driven addition, strengthened across two review rounds |
| The coordinator's exception propagation invariant (no recovery, translation, retry, or suppression) | This document, Section 4c — a review-driven addition, not present in the original draft |

## 10. Completion Criteria

- `IMPLEMENTATION_GAPS.md` and `IMPLEMENTATION_HISTORY.md` are **not**
  touched by this Plan, and must not be touched during implementation
  until every test in Section 6 passes and the full Gradle suite result
  is known (real or honestly disclosed as projected).
- After verified implementation: `IMPLEMENTATION_HISTORY.md` **may** be
  updated, recording this Unit exactly as delivered (files added, tests
  added, verified result).
- After verified implementation: `IMPLEMENTATION_GAPS.md` #53 **may be
  clarified further, not closed.** This Unit implements the wiring
  between an accepted `InboundOwnerMessage` and a `ReasoningProviderResponse`
  in full, but performs no routing of that response anywhere — gap #53's
  own two closure paths ((a) `ExecutionRequest`'s content-carrying gap,
  (b) `Goal`/`Reply` routing to Planner Runtime/Response Delivery) both
  remain fully open. Any clarifying update should state plainly: an
  accepted inbound message can now reach a `ReasoningProviderResponse`
  through one, real, tested, production code path; nothing yet consumes
  that response.
- No architecture or Contract Design document
  (`19-conversation-engine.md`, `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`,
  `REASONING_PROVIDER_ARCHITECTURE.md`,
  `REASONING_PROVIDER_CONTRACT_DESIGN.md`, `COMMUNICATION_CONTRACT_DESIGN.md`,
  `COMMUNICATION_CHANNEL_ARCHITECTURE.md`) is modified at any point during
  this Unit's implementation.

## 11. Scope Lock

**Not yet locked.** Restating this program's own established two-step
process: this Plan defines the boundary; a separate, explicit human
instruction ("Scope Lock has been achieved") is required before any
Kotlin is written against it, per PES-001's Human-primary-authority model
for Stage 3 through Stage 5.

**What becomes frozen once locked:** exactly the file list in Section 3,
the dependency list in Section 4, the statelessness invariant in Section
4a, the message pass-through invariant in Section 4b, the exception
propagation invariant in Section 4c, the three Required Implementation
Decisions in Section 5 (as resolved — this document proposes conservative
defaults for each; Scope Lock should either confirm or override them
explicitly before Kotlin begins), the testing strategy in Section 6, and
the Out-of-Scope list in Section 8. Any change to any of these after
Scope Lock requires a new planning pass, not a silent adjustment during
implementation.

## Conclusion

**This document defines one Stage 3 Implementation Plan for the smallest
safe unit connecting the already-implemented Communication Runtime to the
already-implemented Conversation Engine + Reasoning Provider unit.** No
existing component is modified; a single new, small, non-interface-backed
coordinator is added, reusing both existing sides whole, returning a
small, generic upstream-admission-gate wrapper (`GatedOutcome<T>`) —
documented as modelling "admit, or reject with a reason" in general, not
"Communication acceptance" specifically, and suitable for reuse by future
coordinators with the same gating semantics, without claiming that reuse
as a predicted fact — rather than a bespoke type of its own. The
coordinator's statelessness (Section 4a), its obligation to never mutate
or reinterpret an accepted message (Section 4b), its refusal to recover
from, translate, retry, or suppress a thrown exception (Section 4c), and
its unconditional exactly-once invocation of each downstream component
regardless of response content (Section 7) are all stated as explicit
invariants, not left to be assumed, and each is enforced structurally by
a dedicated test. Three genuine interpretive gaps are named explicitly
as Required Implementation Decisions, each with a conservative,
precedent-matching proposed default, including an explicit, honest
treatment of whether this wiring should instead have gone through a Stage
2A Contract Design pass first, and an explicit record of which
alternatives to a new result type were checked and rejected, and why.
Every boundary this task named — no Planner, no Task Manager, no Agent
Runtime, no Execution Pipeline, no Memory, no World Model, no Response
Delivery, no Android, no Speech, no UI, no persistence — is enforced
structurally, by the absence of any dependency capable of reaching them,
not merely asserted in prose.

This Plan does not implement anything itself, does not modify any
architecture or Contract Design document, and does not touch
`IMPLEMENTATION_GAPS.md` or `IMPLEMENTATION_HISTORY.md`. It awaits an
explicit Scope Lock instruction before any Kotlin is written.

## Related

- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md`
- `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md`
- `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`
- `docs/architecture/19-conversation-engine.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md` (#53)
- `docs/implementation/CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` (direct precedent for Decisions 1 and 2)
- `src/interfaces/CommunicationIntake.kt`, `src/runtime/InMemoryCommunicationIntake.kt`
- `src/interfaces/ConversationEngine.kt`, `src/runtime/InMemoryConversationEngine.kt`
- `src/interfaces/ReasoningProvider.kt`
- `src/runtime/ConversationTurnReasoningCoordinator.kt`
- `tests/runtime/FakeCommunicationIntake.kt`, `tests/runtime/FakeReasoningProvider.kt` (reused unchanged)
