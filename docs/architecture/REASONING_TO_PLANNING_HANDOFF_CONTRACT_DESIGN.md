# Reasoning-to-Planning Handoff — Contract Design

## Status

**Contract Design only. No Kotlin is implemented, proposed as a diff, or
changed by this document.** Revision 2, incorporating required changes to
ID minting, outcome naming/shape, and clarification of remaining contract
points, all requested before Scope Lock. Revision 1's overall
architecture is unchanged and remains accepted: `GoalPlanningHandoffCoordinator`
as the new component; direct `Goal.text` → `PlanningRequest.goal`
mapping; interception before `ResponseComposer`, at
`ConversationReplyCoordinator`; no invocation of `PlannerRuntime.plan()`;
a new conversation-level outcome type; propagation of planning deferral
through `ParkerRuntimeOutcome`.

Companion to
`docs/architecture/REASONING_TO_PLANNING_HANDOFF_GOVERNANCE_REVIEW.md`
(commit `4490e36`, accepted) and to the Parker Platform Continuation
Brief's Mandatory Constraint. This document does not implement Kotlin,
does not generate `PlanCandidate` objects, does not invoke
`PlannerRuntime.plan()` under any condition, does not wire
`PlannerRuntime` or `TaskManagerRuntime` into production, does not modify
`ResponseComposer`, does not introduce a first-class Goal domain type,
does not add authentication or permission enforcement, and does not
touch `docs/architecture/23-goal-manager.md`.

---

## 1. The Central Design Tension, Restated

"Invocation responsibility" for `PlannerRuntime` is discharged
architecturally, not operationally, in this Unit. `GoalPlanningHandoffCoordinator`
is positioned as the component that *would* call `PlannerRuntime.plan()`
once it is safe to do so, but its complete implementation, defined by
this document, never holds a `PlannerRuntime` reference and never calls
it. A future, separately-governed Unit — which must also supply
legitimate `PlanCandidate` generation and production composition wiring —
is the one that adds a `PlannerRuntime` dependency to this coordinator
and replaces its current behaviour with a real `plan()` call. This
document defines only that current behaviour.

---

## 2. The New Coordinator

**Name: `GoalPlanningHandoffCoordinator`.** **File:**
`src/runtime/GoalPlanningHandoffCoordinator.kt` (proposed, not created by
this document).

**Responsibility.** Given an `InboundOwnerMessage` and the
`ReasoningProviderResponse.Goal` reasoning about it produced, constructs
a fully-formed `PlanningRequest` (the same object `PlannerRuntime.plan`
already accepts, unchanged), and reports — as a distinct, structured,
non-`NotAccepted` outcome — that the planner is not invoked and planning
is not initiated, because no legitimate `PlanCandidate` source exists yet
to supply `plan`'s second, mandatory argument.

**Required Implementation Decision (mirroring `ConversationTurnReasoningCoordinator`'s
own precedent).** Concrete, not interface-backed. Introduces no new
public contract type beyond the two small value types in Section 3, and
is ordinary Stage 3 implementation-level wiring between two
already-approved contracts — not a new architectural boundary requiring
an interface seam.

**Public contract:**

```kotlin
class GoalPlanningHandoffCoordinator(
    private val planningSessionIdFactory: () -> String,
) {
    suspend fun initiatePlanning(
        originalMessage: InboundOwnerMessage,
        goal: ReasoningProviderResponse.Goal,
    ): GoalPlanningHandoffOutcome
}
```

- **Input.** `originalMessage: InboundOwnerMessage` — the same value
  `ConversationReplyCoordinator.submitAndDeliver` already receives as its
  own `message` parameter; no `Turn` is required, since everything this
  coordinator needs (`senderPrincipalId`, `correlationId`) is already
  present on `InboundOwnerMessage` directly. `goal:
  ReasoningProviderResponse.Goal` — the already-produced value, not the
  full `ReasoningProviderResponse` sealed type, making it structurally
  impossible for this coordinator to be called with a `Reply` or
  `NoAction` by mistake.
- **Output.** `GoalPlanningHandoffOutcome` (Section 3) — never `null`.
  See Section 6 for exactly when it throws instead of returning.
- **Dependency: exactly one, mandatory, explicit — `planningSessionIdFactory:
  () -> String`.** No default value. Both production wiring
  (`ParkerRuntime.kt`) and every test must supply one explicitly; see
  Section 4.

**Why `suspend`, even though nothing this dependency provides requires
it.** `planningSessionIdFactory` is a plain, non-suspend function type
(`() -> String`); calling it does not require `suspend`. `initiatePlanning`
is nonetheless declared `suspend` for exactly the same forward-compatibility
reason already established as precedent by `PlanDecision.decide`
(`src/contracts/PlanDecision.kt`): "`suspend` now... even though [the
current implementation]'s own body never actually suspends, avoids a
breaking interface change later." The day a future Unit adds a real
`PlannerRuntime` dependency (a suspend interface) and replaces this
method's body with an actual `plan()` call, this signature does not need
to change a second time, and neither does any existing call site.
`ConversationReplyCoordinator.submitAndDeliver`, this method's only
caller, is itself already `suspend`, so this costs nothing today.

---

## 3. `PlanningSessionId` Generation

**Repository precedent check (performed for this revision): no existing
injected ID-generation convention exists anywhere in this codebase.**
Confirmed by direct search: every ID in every runtime component
(`ConversationId`, `TurnId` in `InMemoryConversationEngine`; every event
ID in every `publish` helper) is minted inline via
`java.util.UUID.randomUUID().toString()`, never through an injected
factory, generator, or clock abstraction. Timestamps are the same —
`Instant.now()` is called inline everywhere, with no injected `Clock`.
**This Unit introduces the first instance of an injected,
deterministic-for-testing ID-minting seam in this repository.** That is
disclosed here as a genuinely new pattern, not misrepresented as
following an existing one.

**Design:**

```kotlin
private val planningSessionIdFactory: () -> String
```

- **Newly minted per handoff.** Called exactly once, inside
  `initiatePlanning`, once per invocation — never cached, never reused
  across calls, never called speculatively.
- **Non-blank.** Enforced structurally, not by this coordinator's own
  code: `PlanningSessionId`'s own `init` block
  (`src/contracts/TaskProposal.kt`) already requires
  `value.isNotBlank()`. The coordinator does not duplicate this check.
- **Opaque.** The returned `String` carries no required internal
  structure this coordinator or any consumer may parse or assume.
- **Production-unique and not derived from `correlationId`, message
  text, or `Goal.text`.** This is a documentation-enforced obligation on
  whatever function is *supplied* to this parameter, not something the
  coordinator's own code can verify at runtime — it has no way to inspect
  whether a returned string was derived from its own inputs. The
  production implementation (Section 4) satisfies this by construction
  (`UUID.randomUUID()` reads system entropy, not any argument). This
  limitation — a documented contract, not a compiler-checked invariant —
  is recorded here explicitly rather than silently assumed enforced,
  mirroring this repository's own established treatment of similar
  convention-based-not-construction-enforced boundaries (e.g.
  `IMPLEMENTATION_GAPS.md` #41, `ToolInvocationBinding`'s
  Execution-Pipeline-only restriction).
- **Generated once, structurally provable.** Since `planningSessionIdFactory`
  is called exactly one place in `initiatePlanning`'s own body, a test
  supplying a call-counting fake can assert exactly one invocation per
  `initiatePlanning` call (Section 9).

**Production supply:** `{ UUID.randomUUID().toString() }`, passed
explicitly at the `ParkerRuntime.kt` construction site for
`GoalPlanningHandoffCoordinator` — not defaulted inside the class itself,
so the production choice is visible and auditable at the composition
root, matching this repository's general preference for explicit
construction of real behaviour in `ParkerRuntime.kt` (e.g.
`DefaultPermissionPolicy`'s own rules are constructed explicitly there,
not left to a class-internal default).

**Test supply:** any fixed value or sequence, e.g. `{ "fixed-planning-session-id" }`
or a counting fake `{ "planning-session-${counter++}" }`, injected
directly through the same constructor parameter — no test double,
mocking framework, or subclassing required.

---

## 4. `PlanningRequest` Construction, Field by Field

Constructed once, inside `GoalPlanningHandoffCoordinator.initiatePlanning`:

| Field | Source | Justification |
| --- | --- | --- |
| `planningSessionId` | `PlanningSessionId(planningSessionIdFactory())` | Freshly minted per handoff, per Section 3. |
| `initiatingPrincipalId` | `originalMessage.senderPrincipalId` | Already field-shaped on `InboundOwnerMessage`; `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 4 already lists this as reused via `Turn.message.senderPrincipalId`. Extraction only, no construction. |
| `goal` | `goal.text` | Unchanged, per the Governance Review's Section 5.1 determination (direct string mapping, no first-class Goal type). |
| `correlationId` | `originalMessage.correlationId.value` | One `.value` unwrap: `InboundOwnerMessage.correlationId: CorrelationId` (value class) to `PlanningRequest.correlationId: String`. |
| `source` | **Left at the field's own existing default: `RequestOrigin.TEXT`** (`src/contracts/PlanDecision.kt`: `val source: RequestOrigin = RequestOrigin.TEXT`) | **Why this default is valid for this path, confirmed directly:** `InboundOwnerMessage` carries no field of its own from which a `RequestOrigin` could be derived (confirmed by direct reading of `src/interfaces/CommunicationIntake.kt`: `channelId`, `senderPrincipalId`, `text`, `timestamp`, `correlationId`, `metadata` — none maps to `RequestOrigin`). The only production channel capable of producing an `InboundOwnerMessage` that reaches this coordinator today is `LocalTextChannel`/`DefaultLocalTextChannel` — `RequestOrigin.TEXT` is factually accurate for every message this coordinator can receive in the current system, not a placeholder standing in for a real value. **Disclosed limitation:** when a second channel type (voice, Home Assistant) exists, this default will need a real derivation mechanism — not decided or invented here. |
| `priority` | **Left at the field's own existing default: `RequestPriority.NORMAL`** (`src/contracts/PlanDecision.kt`: `val priority: RequestPriority = RequestPriority.NORMAL`) | **Why this default is valid for this path:** identical reasoning to `source` — no field on `InboundOwnerMessage`, `Turn`, `ReasoningProviderRequest`, or `ReasoningProviderResponse.Goal` carries any priority signal to derive from (confirmed by direct reading of each type's fields). No priority-elevation or priority-inference mechanism exists anywhere upstream of this coordinator. Defaulting to `NORMAL` is the only honest option available, not an oversight. |

**No `PlanningRequest` field is invented, renamed, or given a new
meaning.** All six fields already exist, unchanged, exactly as approved
by `PLANNER_RUNTIME_CONTRACT_DESIGN.md` Section 6.

---

## 5. New Types

### 5.1 `PlanningDeferralReason`

```kotlin
enum class PlanningDeferralReason {
    CANDIDATE_GENERATION_UNAVAILABLE,
}
```

A single-member enum today, deliberately — not a placeholder forgotten
mid-design. This is the **authoritative** reason; the human-readable
`detail` field (below) is supplementary, never authoritative, directly
addressing the instruction to avoid a free-form string as the source of
truth. A future caller (a test, a log line, an `EventBus` payload) can
branch on this outcome's meaning structurally, never by parsing free
text. Adding a second member (e.g. once real submission exists) is
itself a future, governed contract revision — not decided or performed
here.

### 5.2 `GoalPlanningHandoffOutcome`

**Renamed from Revision 1's `PlanningInitiationOutcome` for accuracy: the
planner is never invoked and planning is never initiated by this Unit, so
no name implying initiation is used anywhere in this design.**

```kotlin
sealed class GoalPlanningHandoffOutcome {
    data class Deferred(
        val planningRequest: PlanningRequest,
        val reason: PlanningDeferralReason,
        val detail: String,
    ) : GoalPlanningHandoffOutcome()
}
```

**Sealed, not a plain data class, and `class`, not `interface`** — this
repository uses `sealed class` exclusively for this kind of outcome type
(confirmed by direct search: eighteen existing `sealed class`
declarations across `src/`, zero `sealed interface`); this design follows
that convention rather than introducing a second style. Sealed with
exactly one variant today, matching the same reasoning as `PlanningDeferralReason`'s
single member: this signals structurally, at the type level, that
`Deferred` is the *only* outcome this Unit can honestly produce, and
that a future variant (e.g. a real `Submitted` case, once `PlannerRuntime.plan()`
is actually called) is a distinct, separately-governed addition, not a
silent extension.

- `planningRequest`: the fully-constructed `PlanningRequest` (Section 4)
  — carried even though never submitted anywhere by this Unit's own
  code, so a caller (logging, a future `EventBus` payload, a future real
  submission path) has the already-correct value available without
  reconstructing it, and so this Unit's own tests can assert the
  field-mapping is correct.
- `reason: PlanningDeferralReason` — **authoritative**, exactly one value
  possible today (Section 5.1).
- `detail: String`, required non-blank (`init { require(detail.isNotBlank())
  ... }`) — **supplementary, never authoritative**, mirroring the
  existing `PlanRejection` precedent in this exact same file
  (`src/contracts/PlanDecision.kt`: `PlanRejection(planCandidateId,
  reason: PlanRejectionReason, detail: String)`), which already
  establishes an enum-reason-plus-string-detail shape in this codebase.
  Fixed wording, not caller-supplied: `"Planning initiation for this Goal
  is deferred: PlanCandidate generation is not yet implemented in this
  repository. PlannerRuntime.plan() was not invoked."`
- **Not a rejection, not a failure, not a completed planning initiation.**
  It is neither `GatedOutcome.NotAccepted` (that shape is reserved for
  genuine rejections — a malformed message, an invalid `Reply` —
  elsewhere in this pipeline) nor a thrown exception (Section 6) nor a
  `PlanningSessionResult` of any kind (that type belongs exclusively to
  `PlannerRuntime.plan`'s own real return value, never fabricated here).

**Placement.** `src/runtime/GoalPlanningHandoffCoordinator.kt`
(co-located with the coordinator), mirroring `GatedOutcome`'s own
explicit precedent (`src/runtime/GatedOutcome.kt`): "a generic
implementation-level utility, not a domain contract." Neither new type
here is a Volume 1–3 governed contract. This is also the concrete reason
**no first-class Goal domain type is introduced**: `GoalPlanningHandoffOutcome`
carries a `PlanningRequest` (already-approved, unchanged), not a new
`Goal` type of any kind.

---

## 6. Errors and Failure Semantics

**`GoalPlanningHandoffCoordinator.initiatePlanning` does not throw under
a well-formed, non-throwing `planningSessionIdFactory`, provable by
construction:**

- `PlanningRequest.goal` requires non-blank — supplied from `goal.text`,
  already guaranteed non-blank by `ReasoningProviderResponse.Goal`'s own
  `init` block (`src/interfaces/ReasoningProvider.kt`).
- `PlanningRequest.correlationId` requires non-blank — supplied from
  `originalMessage.correlationId.value`, already guaranteed non-blank by
  `CorrelationId`'s own `init` block (`src/interfaces/CommunicationIntake.kt`).
- `GoalPlanningHandoffOutcome.Deferred.detail` is a fixed, non-blank
  literal (Section 5.2) — never caller-supplied, never able to violate
  its own `require`.

**Exact failure behaviour if `planningSessionIdFactory` returns blank or
throws — the one real fault surface this design has:**

- **Returns blank:** `PlanningSessionId(planningSessionIdFactory())`
  throws `IllegalArgumentException("PlanningSessionId must not be
  blank")` — `PlanningSessionId`'s own existing `init` check
  (`src/contracts/TaskProposal.kt`), not a new check this coordinator
  adds.
- **Throws:** whatever the factory itself throws propagates unchanged.

**In both cases, no `try`/`catch` exists inside `GoalPlanningHandoffCoordinator`**
— matching every other coordinator's own established discipline in this
codebase. The exception propagates to `initiatePlanning`'s caller
(`ConversationReplyCoordinator.submitAndDeliver`), which also has no
`try`/`catch`, propagating further to `ParkerRuntime.submitOwnerMessage`'s
own existing outer boundary — the one place in this repository already
documented as the correct location for a genuine runtime fault to be
caught (`src/composition/ParkerRuntime.kt`'s own KDoc: "No coordinator
between `ConversationReplyCoordinator` and the model/Tool call sites
catches anything itself... This method is therefore the correct, and
only correct, place in this repository's own existing architecture for a
genuine runtime fault... to be caught at all"). The result surfaces as
`ParkerRuntimeOutcome.Failed(PipelineStage.UNKNOWN, e)` — `UNKNOWN`
because, per `PipelineStage`'s own existing KDoc, only `REASONING`-stage
exceptions are structurally distinguishable from this runtime's vantage
point, and a blank/throwing ID factory is neither structurally tagged nor
one of the two named exceptions `PipelineStage.REASONING` already
recognises. **No new exception type, and no new `ParkerRuntimeOutcome`
variant, is introduced for this failure path** — it reuses the existing,
already-governed `Failed`/`UNKNOWN` machinery exactly as it already
handles every other untagged fault in this pipeline.

---

## 7. `Reply` / `NoAction` / `Goal` Branching Responsibility

**Interception point, unchanged from Revision 1: `ConversationReplyCoordinator.submitAndDeliver`.**
It already holds the raw `ReasoningProviderResponse` (as `reasoned.value`,
inside `GatedOutcome.Produced`) before forwarding it anywhere else.

**Revised behaviour (proposed, not implemented by this document) —
simplified from Revision 1 to avoid double-wrapping a `GatedOutcome`
inside a `ConversationOutcome`:**

```kotlin
class ConversationReplyCoordinator(
    private val communicationConversationCoordinator: CommunicationConversationCoordinator,
    private val replyDeliveryCoordinator: ReplyDeliveryCoordinator,
    private val goalPlanningHandoffCoordinator: GoalPlanningHandoffCoordinator, // new
) {
    suspend fun submitAndDeliver(
        message: InboundOwnerMessage,
        reasoningContext: ReasoningContext,
        conversationId: ConversationId,
    ): ConversationOutcome {
        val reasoned = communicationConversationCoordinator.submitAndReason(message, reasoningContext, conversationId)
        return when (reasoned) {
            is GatedOutcome.NotAccepted -> ConversationOutcome.NotAccepted(reasoned.reason)
            is GatedOutcome.Produced -> when (val response = reasoned.value) {
                is ReasoningProviderResponse.Goal ->
                    ConversationOutcome.PlanningDeferred(goalPlanningHandoffCoordinator.initiatePlanning(message, response))
                else -> when (val delivered = replyDeliveryCoordinator.composeAndDeliver(message, response)) {
                    is GatedOutcome.NotAccepted -> ConversationOutcome.NotAccepted(delivered.reason)
                    is GatedOutcome.Produced -> ConversationOutcome.ReplyDelivered(delivered.value)
                }
            }
        }
    }
}
```

- `Reply` and `NoAction` reach `replyDeliveryCoordinator.composeAndDeliver`
  exactly as they do today; `ResponseComposer` and `ReplyDeliveryCoordinator`
  are **not modified at all**. `composeAndDeliver`'s own existing
  `GatedOutcome<ExecutionResult>` result is unwrapped one level here
  (rather than double-wrapped, correcting Revision 1's design) so that
  a downstream rejection on the Reply path (e.g. `ResponseComposer`
  declining a malformed `Reply`, or `ResponseDelivery` failing) collapses
  into `ConversationOutcome.NotAccepted` exactly as it already does today
  — no behavioural change on this path, only a renamed wrapper type.
- `Goal` is intercepted here and never reaches `ReplyDeliveryCoordinator`
  or `ResponseComposer` at all.

### 7.1 `ConversationOutcome` — full variant list and mapping rules

**File:** `src/runtime/ConversationOutcome.kt` (proposed).

```kotlin
sealed class ConversationOutcome {
    data class ReplyDelivered(val executionResult: ExecutionResult) : ConversationOutcome()
    data class PlanningDeferred(val outcome: GoalPlanningHandoffOutcome) : ConversationOutcome()
    data class NotAccepted(val reason: String) : ConversationOutcome()
}
```

**Renamed from Revision 1's `PlanningInitiated` to `PlanningDeferred`**,
for the identical accuracy reason as Section 5.2 — nothing is initiated.

Three variants, exactly, replacing `GatedOutcome<ExecutionResult>` as
`ConversationReplyCoordinator.submitAndDeliver`'s return type:

| Source | Maps to |
| --- | --- |
| `communicationConversationCoordinator.submitAndReason` returns `GatedOutcome.NotAccepted(reason)` | `ConversationOutcome.NotAccepted(reason)` |
| `reasoned.value` is `Goal` | `ConversationOutcome.PlanningDeferred(goalPlanningHandoffCoordinator.initiatePlanning(...))` |
| `reasoned.value` is `Reply`/`NoAction`, and `replyDeliveryCoordinator.composeAndDeliver` returns `GatedOutcome.Produced(executionResult)` | `ConversationOutcome.ReplyDelivered(executionResult)` |
| `reasoned.value` is `Reply`/`NoAction`, and `replyDeliveryCoordinator.composeAndDeliver` returns `GatedOutcome.NotAccepted(reason)` | `ConversationOutcome.NotAccepted(reason)` |

This is a small, generic-shaped, purely additive sealed type, following
`GatedOutcome`'s own explicit precedent of being "a generic
implementation-level utility, not a domain contract."

---

## 8. `ParkerRuntimeOutcome` — Full Mapping

**One new variant, added to the existing sealed class
(`src/composition/ParkerRuntimeOutcome.kt`):**

```kotlin
sealed class ParkerRuntimeOutcome {
    data class Delivered(val executionResult: ExecutionResult) : ParkerRuntimeOutcome()
    data class NotAccepted(val reason: String) : ParkerRuntimeOutcome()
    data class Failed(val stage: PipelineStage, val cause: Throwable) : ParkerRuntimeOutcome()
    data class PlanningDeferred(val outcome: GoalPlanningHandoffOutcome) : ParkerRuntimeOutcome() // new
}
```

`Delivered`, `NotAccepted`, and `Failed` are entirely unchanged in shape
and meaning. `ParkerRuntime.submitOwnerMessage`'s own `when` block, mapping
`ConversationOutcome` (Section 7.1) to `ParkerRuntimeOutcome`, one to one,
no nested unwrapping required:

| `ConversationOutcome` | `ParkerRuntimeOutcome` |
| --- | --- |
| `NotAccepted(reason)` | `NotAccepted(reason)` — unchanged from today's identical `GatedOutcome.NotAccepted` handling. |
| `ReplyDelivered(executionResult)` | `Delivered(executionResult)` — unchanged from today's identical `GatedOutcome.Produced` handling. |
| `PlanningDeferred(outcome)` | `PlanningDeferred(outcome)` — new, passed through unchanged. |

`Failed` is unaffected by this Unit — it is produced exclusively by
`submitOwnerMessage`'s own outer `try`/`catch`, not by any `when` branch
over `ConversationOutcome` (see Section 6 for the one new fault path this
Unit adds, which resolves to `Failed`, not `PlanningDeferred`).

---

## 9. Exhaustiveness Impact — Confirmed Call Sites

Checked directly, this revision: every existing consumer of
`ConversationReplyCoordinator.submitAndDeliver`'s return type or of
`ParkerRuntimeOutcome`.

**Direct consumers of `submitAndDeliver` (grep-confirmed, four files
total):** `src/composition/ParkerRuntime.kt` (production caller — its own
`when` **requires** rewriting for the three new `ConversationOutcome`
variants; the Kotlin compiler enforces this if `when` is used as an
expression, converting a silently-missed case into a compile error rather
than a runtime gap); `src/runtime/ConversationReplyCoordinator.kt` itself
(Section 7); `tests/runtime/ConversationReplyCoordinatorTest.kt`
(**requires** updating — every existing assertion against the old
`GatedOutcome<ExecutionResult>` return type must be rewritten against the
new `ConversationOutcome` variants).

**Direct consumers of `ParkerRuntimeOutcome` (grep-confirmed, six files
total):** `src/composition/ParkerRuntime.kt` (produces it; Section 8);
`src/composition/ParkerRuntimeException.kt` (unrelated reference,
confirmed by inspection to be a KDoc mention, not a `when` over the
type); `src/composition/ParkerRuntimeOutcome.kt` itself;
`tests/composition/ParkerRuntimeConversationPipelineTest.kt`,
`tests/composition/ParkerRuntimeFailureHandlingTest.kt`,
`tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` —
**each requires inspection during implementation**; any of the three
using an exhaustive `when` expression over `ParkerRuntimeOutcome` will
fail to compile until updated for the new `PlanningDeferred` variant
(the compiler surfaces this automatically); any using `is`/`as`
type-checks only will continue to compile unchanged but should still be
reviewed for completeness.

No other file in this repository references either type (confirmed by
the same searches). This is the complete, final list of files touched by
implementing this design — restated in Section 11.

---

## 10. Minimum Dependencies (Summary)

- `GoalPlanningHandoffCoordinator`: **exactly one**, mandatory, explicit
  — `planningSessionIdFactory: () -> String`. No `PlannerRuntime`, no
  `IdentityService`, no `EventBus`.
- `ConversationReplyCoordinator`: **one new** dependency added
  (`GoalPlanningHandoffCoordinator`), alongside its two existing ones.
- No change to `ReplyDeliveryCoordinator`'s, `ResponseComposer`'s, or
  `ConversationTurnReasoningCoordinator`'s dependency lists.
- `ParkerRuntime.kt`'s composition root gains exactly one new
  construction line — `GoalPlanningHandoffCoordinator(planningSessionIdFactory
  = { UUID.randomUUID().toString() })` — passed into
  `ConversationReplyCoordinator`'s existing construction call. **This is
  not "wiring `PlannerRuntime` into production"** — no
  `PlannerRuntime`/`InMemoryPlannerRuntime` instance is constructed
  anywhere by this Unit. `TaskManagerRuntime` is similarly untouched.

---

## 11. Test Obligations

**New: `GoalPlanningHandoffCoordinatorTest`**

- Given an `InboundOwnerMessage`, a `Goal`, and a fixed
  `planningSessionIdFactory` returning `"fixed-id"`, the returned
  `GoalPlanningHandoffOutcome.Deferred.planningRequest` has
  `planningSessionId == PlanningSessionId("fixed-id")`,
  `initiatingPrincipalId == message.senderPrincipalId`,
  `correlationId == message.correlationId.value`, `goal == goal.text`,
  `source == RequestOrigin.TEXT`, `priority == RequestPriority.NORMAL`.
- A counting fake `planningSessionIdFactory` proves exactly one
  invocation per `initiatePlanning` call.
- Two calls with two different factory return values produce two
  different `planningSessionId` values — no caching or reuse.
- `reason == PlanningDeferralReason.CANDIDATE_GENERATION_UNAVAILABLE` on
  every call — there is no code path to any other value today.
- `detail` is non-blank and names the actual reason — a direct string
  assertion, since the wording is fixed by this design.
- A `planningSessionIdFactory` returning a blank string causes
  `initiatePlanning` to throw `IllegalArgumentException` — asserting
  Section 6's documented failure behaviour directly, not merely hoping it
  holds.
- A throwing `planningSessionIdFactory` causes that same exception to
  propagate out of `initiatePlanning` unchanged.
- **Structural test**, mirroring this codebase's own established
  precedent: confirm `GoalPlanningHandoffCoordinator`'s constructor
  accepts exactly one parameter, of type `() -> String` — the structural
  proof that this class cannot call `PlannerRuntime`, `IdentityService`,
  or `EventBus`.

**Revised: `ConversationReplyCoordinatorTest`**

- All existing `Reply`/`NotAccepted`-path tests updated to the new
  `ConversationOutcome` return type.
- New: given `reasoned.value` is a `Goal`, `replyDeliveryCoordinator.composeAndDeliver`
  is never called — structural, exactly-zero-invocations, mirroring
  `CommunicationConversationCoordinatorTest`'s own existing "`ReasoningProvider.reason`
  is never called" precedent.
- New: given `reasoned.value` is a `Goal`,
  `goalPlanningHandoffCoordinator.initiatePlanning` is called exactly
  once, with the same `message` and the same `Goal` instance, and the
  returned `ConversationOutcome.PlanningDeferred.outcome` is exactly what
  `initiatePlanning` returned, unwrapped.
- New: given `reasoned.value` is `Reply` or `NoAction`,
  `goalPlanningHandoffCoordinator.initiatePlanning` is never called — the
  symmetric structural proof.
- New: given `composeAndDeliver` returns `GatedOutcome.NotAccepted`, the
  result is `ConversationOutcome.NotAccepted` with the same reason —
  proving the one-level unwrap (Section 7) preserves existing rejection
  behaviour exactly.

**Revised: `tests/composition/ParkerRuntimeConversationPipelineTest.kt`,
`ParkerRuntimeFailureHandlingTest.kt`, `ParkerRuntimeReasoningContextIntegrationTest.kt`**

- Each inspected for exhaustive `when` over `ConversationOutcome`/`ParkerRuntimeOutcome`
  and updated as the compiler requires (Section 9).
- A `Goal`-producing fake `ReasoningProvider` results in
  `ParkerRuntimeOutcome.PlanningDeferred` — not `Delivered`, not
  `NotAccepted`, not `Failed`, not a swallowed/absent result.
- `ParkerRuntimeOutcome.PlanningDeferred.outcome.reason ==
  PlanningDeferralReason.CANDIDATE_GENERATION_UNAVAILABLE`, confirming
  the outcome is observable all the way from the one production entry
  point (`submitOwnerMessage`) to its caller.
- **Negative, repository-wide proof:** no test double or fake anywhere in
  this Unit's own new test files constructs a non-empty `PlanCandidate`
  list or calls `PlannerRuntime.plan` — true by construction, since
  `GoalPlanningHandoffCoordinator` holds no `PlannerRuntime` reference at
  all; the structural constructor-arity test (above) is the proof, not a
  hope that no test happens to call `plan`.

---

## 12. Compliance Checklist Against This Unit's Explicit Constraints

| Constraint | Status |
| --- | --- |
| Must not implement Kotlin | Satisfied — this document is Contract Design only. |
| Must not generate `PlanCandidate` objects | Satisfied — no code path in this design constructs one. |
| Must not invoke `PlannerRuntime.plan()` without legitimate candidates | Satisfied — `PlannerRuntime.plan()` is never invoked anywhere in this design, under any input. |
| Must not wire `PlannerRuntime` or `TaskManagerRuntime` into production | Satisfied — neither is constructed, referenced, or imported anywhere in this design. |
| Must not modify `ResponseComposer` | Satisfied — zero changes; its `Goal -> NotAccepted` branch is simply no longer reached from production. |
| Must not introduce a first-class Goal domain type | Satisfied — `GoalPlanningHandoffOutcome` carries the existing `PlanningRequest`; no `Goal` type of any kind is introduced. |
| Must not add authentication or permission enforcement | Satisfied — no identity, authentication, or permission check exists anywhere in this design. |
| Must not touch Chapter 23 Goal Manager | Satisfied — `docs/architecture/23-goal-manager.md` is not read, cited as authority, or altered by any type in this design. |
| Interim outcome must explicitly report deferral, not discard, fabricate, or misrepresent | Satisfied — `GoalPlanningHandoffOutcome.Deferred`/`PlanningDeferralReason` exist specifically to make this a structurally distinct, non-`NotAccepted`, non-silent, non-initiated outcome. |
| Reason must be structured/machine-readable, not an authoritative free-form string | Satisfied — `PlanningDeferralReason` enum is authoritative; `detail` is supplementary only, matching the `PlanRejection` precedent. |
| `PlanningSessionId` minting must be injected and deterministic-for-testing | Satisfied — `planningSessionIdFactory: () -> String`, mandatory, no default, production supplies `UUID.randomUUID()`, tests supply fixed values. |

---

## 13. Files Proposed (Not Created by This Document)

- `src/runtime/GoalPlanningHandoffCoordinator.kt` — new. Defines
  `GoalPlanningHandoffCoordinator`, `GoalPlanningHandoffOutcome`,
  `PlanningDeferralReason`.
- `src/runtime/ConversationOutcome.kt` — new. Defines `ConversationOutcome`.
- `src/runtime/ConversationReplyCoordinator.kt` — modified (dependency,
  branch, return type; Sections 7, 8).
- `src/composition/ParkerRuntimeOutcome.kt` — modified (one new variant;
  Section 8).
- `src/composition/ParkerRuntime.kt` — modified (one new construction
  line for `GoalPlanningHandoffCoordinator`, explicitly supplying `{
  UUID.randomUUID().toString() }`; `submitOwnerMessage`'s `when` block
  updated for the new `ConversationOutcome`/`ParkerRuntimeOutcome`
  shapes; Sections 8, 9, 10).
- `tests/runtime/GoalPlanningHandoffCoordinatorTest.kt` — new (Section 11).
- `tests/runtime/ConversationReplyCoordinatorTest.kt` — modified (Section 11).
- `tests/composition/ParkerRuntimeConversationPipelineTest.kt`,
  `tests/composition/ParkerRuntimeFailureHandlingTest.kt`,
  `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` —
  each inspected, and modified if it uses an exhaustive `when` over
  either changed type (Section 9, Section 11).

None of the above is created or edited by this document. All are
proposed for the future Implementation Unit, contingent on Scope Lock and
explicit approval.

---

## 14. Readiness Determination

This Contract Design is complete within its charter: coordinator name and
full public shape; injected, deterministic, explicitly-supplied
`PlanningSessionId` minting; exact field-by-field `PlanningRequest`
construction with justified defaults; a renamed, sealed,
enum-reason-plus-supplementary-detail outcome type that cannot be
confused with a rejection, a failure, or a completed planning initiation;
the full `Reply`/`NoAction`/`Goal` branching mechanism; the full
`ConversationOutcome` and `ParkerRuntimeOutcome` variant lists and
mapping tables; a confirmed, complete list of every exhaustiveness-sensitive
call site; exact failure semantics for the one real fault surface this
design has (a blank or throwing ID factory); the two required non-Volume
contract revisions; minimum dependencies; and test obligations. It does
not implement Kotlin, does not generate `PlanCandidate`s, does not invoke
`PlannerRuntime.plan()`, does not wire `PlannerRuntime`/`TaskManagerRuntime`
into production, does not modify `ResponseComposer`, does not introduce a
first-class Goal type, does not add authentication/permission
enforcement, and does not touch Chapter 23.

**Awaiting Scope Lock and explicit implementation approval before any
Kotlin is written.**
