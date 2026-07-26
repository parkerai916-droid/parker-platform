# Reasoning-to-Planning Handoff — Scope Lock

## Status

**Binding, frozen Scope Lock for a future implementation Unit.**
Companion to
`docs/architecture/REASONING_TO_PLANNING_HANDOFF_GOVERNANCE_REVIEW.md`
(commit `4490e36`, repository review and readiness determination) and
`docs/architecture/REASONING_TO_PLANNING_HANDOFF_CONTRACT_DESIGN.md`
(Revision 2, approved — the type-level design this Scope Lock freezes).
The Included and Excluded lists below (Sections 1 and 2) are binding
contract terms a future implementation must satisfy exactly, not
redesign.

**This document authorises no implementation.** Producing this Scope
Lock is the final deliverable of this governance/design pass. No Kotlin
is written, staged, committed, or pushed as part of this Unit's own work.

---

## 1. Responsibilities — What This Handoff Owns

The Reasoning-to-Planning Handoff owns exactly one responsibility:
**receiving a `ReasoningProviderResponse.Goal`, constructing the
`PlanningRequest` it maps to, and honestly reporting that planner
invocation is deferred**, because no legitimate `PlanCandidate` source
exists yet.

That single responsibility decomposes into what it must be able to
answer:

- **Constructing a correct `PlanningRequest`.** Given an
  `InboundOwnerMessage` and a `Goal`, produce a `PlanningRequest` whose
  every field is either extracted unchanged from an already-existing
  source, or left at its own existing, already-approved default — never
  invented, inferred, or derived by any new logic.
- **Reporting deferral truthfully.** Return a structured outcome that
  states, unambiguously, that `PlannerRuntime.plan()` was not called and
  why — never a rejection, never a delivery, never a fabricated success,
  never silence.
- **Nothing beyond this.** It does not generate `PlanCandidate`s, does
  not call `PlannerRuntime`, does not create a Task, does not execute
  anything, and does not decide policy of any kind.

This Unit constructs a request and reports a deferral. Nothing more.

---

## 2. Included / Excluded (Binding)

### 2.1 Included

**New coordinator.**

- **Name: `GoalPlanningHandoffCoordinator`.** Concrete runtime class, not
  interface-backed — ordinary Stage 3 implementation-level wiring between
  two already-approved contracts, per
  `REASONING_TO_PLANNING_HANDOFF_CONTRACT_DESIGN.md` Section 2.
- **Constructor dependency, exactly one, mandatory, explicit:**
  ```kotlin
  planningSessionIdFactory: () -> String
  ```
  **No default factory inside the coordinator.** **No direct dependency
  on `PlannerRuntime`.** **No direct dependency on candidate generation,**
  or on any component that could supply `PlanCandidate`s. **No
  dependency on `IdentityService`, `EventBus`, `TaskManagerRuntime`, or
  any other runtime component.**
- **Public method, exactly this signature:**
  ```kotlin
  suspend fun initiatePlanning(
      originalMessage: InboundOwnerMessage,
      goal: ReasoningProviderResponse.Goal,
  ): GoalPlanningHandoffOutcome
  ```

**`PlanningRequest` construction — exactly these six fields, exactly
these sources, no other derivation or inference permitted:**

| Field | Source |
| --- | --- |
| `planningSessionId` | `PlanningSessionId(planningSessionIdFactory())` — the injected factory, called exactly once per `initiatePlanning` call. |
| `initiatingPrincipalId` | `originalMessage.senderPrincipalId`, unchanged. |
| `correlationId` | `originalMessage.correlationId.value`, unchanged (one `.value` unwrap only). |
| `goal` | `goal.text`, unchanged. |
| `source` | The field's own existing default, `RequestOrigin.TEXT` — not derived, not conditioned on any input. |
| `priority` | The field's own existing default, `RequestPriority.NORMAL` — not derived, not conditioned on any input. |

**Handoff outcome — the sealed class and reason model frozen exactly as
Contract Design Revision 2 defined:**

```kotlin
enum class PlanningDeferralReason {
    CANDIDATE_GENERATION_UNAVAILABLE,
}

sealed class GoalPlanningHandoffOutcome {
    data class Deferred(
        val planningRequest: PlanningRequest,
        val reason: PlanningDeferralReason,
        val detail: String,
    ) : GoalPlanningHandoffOutcome()
}
```

- `planningRequest`: the fully-constructed request, carried unchanged,
  never submitted anywhere by this Unit.
- `reason`: **authoritative.** Exactly one value exists today,
  `CANDIDATE_GENERATION_UNAVAILABLE`. Any future second value is a
  separate, governed contract revision, not performed here.
- `detail`: **supplementary only, never machine-authoritative.** Fixed,
  non-blank, non-caller-supplied wording. No caller, test, or future
  component may treat `detail`'s text as the source of truth for what
  happened — `reason` alone is authoritative, mirroring the existing
  `PlanRejection(reason, detail)` precedent already in this codebase.

**Conversation routing — the flattened `ConversationOutcome` model,
frozen exactly:**

```kotlin
sealed class ConversationOutcome {
    data class ReplyDelivered(val executionResult: ExecutionResult) : ConversationOutcome()
    data class PlanningDeferred(val outcome: GoalPlanningHandoffOutcome) : ConversationOutcome()
    data class NotAccepted(val reason: String) : ConversationOutcome()
}
```

**No nested `GatedOutcome` wrapping is permitted anywhere in this type.**
`ReplyDelivered` carries a plain `ExecutionResult`, not a
`GatedOutcome<ExecutionResult>` — any rejection on the reply-delivery
path (`ResponseComposer` declining, or `ResponseDelivery` failing)
collapses into `ConversationOutcome.NotAccepted`, exactly as it already
does today under the pre-existing `GatedOutcome<ExecutionResult>` shape.

**`ConversationReplyCoordinator` must:**

- Gain exactly one new constructor dependency:
  `goalPlanningHandoffCoordinator: GoalPlanningHandoffCoordinator`.
- **Continue routing `Reply` through the existing delivery path** —
  `replyDeliveryCoordinator.composeAndDeliver` called exactly as it is
  today, unchanged.
- **Route `Goal` to `GoalPlanningHandoffCoordinator.initiatePlanning`**,
  wrapping its result in `ConversationOutcome.PlanningDeferred`.
- **Preserve truthful handling of `NoAction`** — it follows the same
  path as `Reply` (through `replyDeliveryCoordinator.composeAndDeliver`,
  which already handles `NoAction` by returning `GatedOutcome.NotAccepted`
  via `ResponseComposer`), unchanged from today's behaviour, and is never
  routed to `GoalPlanningHandoffCoordinator`.
- **Intercept `Goal` before `ResponseComposer`** — `ResponseComposer` is
  never reached on the `Goal` path once this change lands.
- Return `ConversationOutcome` in place of `GatedOutcome<ExecutionResult>`.

**`ResponseComposer` remains unchanged.** Zero modifications. Its
existing `Goal -> NotAccepted` branch is not removed; it simply becomes
unreachable from the live production path.

**`ParkerRuntimeOutcome` mapping — one new variant, frozen mapping,
exactly:**

```kotlin
data class PlanningDeferred(val outcome: GoalPlanningHandoffOutcome) : ParkerRuntimeOutcome()
```

| `ConversationOutcome` | `ParkerRuntimeOutcome` |
| --- | --- |
| `NotAccepted(reason)` | `NotAccepted(reason)` |
| `ReplyDelivered(executionResult)` | `Delivered(executionResult)` |
| `PlanningDeferred(outcome)` | `PlanningDeferred(outcome)` |

**Planning deferral must remain observable to the caller of
`ParkerRuntime.submitOwnerMessage` and must never be converted into:**

- a rejection (`ParkerRuntimeOutcome.NotAccepted`);
- a delivery success (`ParkerRuntimeOutcome.Delivered`);
- an ordinary planning failure (any `PlanningSessionResult`-shaped value —
  none is ever fabricated, since `PlannerRuntime.plan()` is never
  called);
- a silent discard (an absent, swallowed, or unobserved result).

**Failure behaviour — the existing exception propagation, frozen exactly
as designed, no new machinery:**

- A blank ID returned by `planningSessionIdFactory` fails through
  `PlanningSessionId`'s own existing `init` validation
  (`IllegalArgumentException`) — no new check is added inside
  `GoalPlanningHandoffCoordinator`.
- An exception thrown by `planningSessionIdFactory` itself propagates,
  uncaught, through `GoalPlanningHandoffCoordinator` and through
  `ConversationReplyCoordinator` (neither has a `try`/`catch`).
- `ParkerRuntime.submitOwnerMessage` catches it at its existing outer
  boundary, exactly as it already catches every other untagged runtime
  fault.
- The result is `ParkerRuntimeOutcome.Failed(PipelineStage.UNKNOWN, e)`.

**This Unit must not introduce a new `PipelineStage`, a new exception
type or hierarchy, or a new `ParkerRuntimeOutcome` variant solely for
ID-generation errors.** The existing `Failed`/`UNKNOWN` machinery is
reused exactly as it already handles every other untagged fault in this
pipeline.

**Production wiring, limited to exactly this:**

- `ParkerRuntime.kt` constructs `GoalPlanningHandoffCoordinator(planningSessionIdFactory
  = { UUID.randomUUID().toString() })` and passes it into
  `ConversationReplyCoordinator`'s existing construction call.
- `ParkerRuntime.submitOwnerMessage`'s own `when` block is updated to map
  the three `ConversationOutcome` variants to their corresponding
  `ParkerRuntimeOutcome` variants, per the table above.

**Test obligations — exactly the files and coverage the Contract Design
identifies (Section 4, below).**

### 2.2 Excluded

The Scope Lock prohibits, without exception, absent a stop-and-report on
a genuine blocker:

- **Calling `PlannerRuntime.plan()`.** Under no input, including an
  empty candidate list, does any code this Unit introduces call `plan()`.
- **Production `PlanCandidate` generation.** No mechanism that produces
  a real `PlanCandidate` from a `Goal`, a `PlanningRequest`, or anything
  else is introduced.
- **Fabricated, empty, or test-only candidates used as a stand-in for
  real planning.** No production code path constructs any
  `List<PlanCandidate>`, empty or otherwise, for the purpose of calling
  `plan()`.
- **`PlannerRuntime` production wiring.** No `PlannerRuntime` or
  `InMemoryPlannerRuntime` instance is constructed, referenced, or
  imported anywhere in `src/composition/ParkerRuntime.kt` or any other
  production file by this Unit.
- **`TaskManagerRuntime` production wiring.** Identical prohibition,
  identical reasoning.
- **Task creation.** No `Task`, `TaskProposal`, or `TaskProposalIntake`
  interaction of any kind.
- **Execution orchestration.** No `ExecutionRequest`, `ExecutionPipeline`,
  `ToolRegistry`, or `Tool` interaction of any kind.
- **Permission or authentication changes.** No `PermissionEngine`,
  `PermissionPolicy`, `IdentityService` dependency, or any authentication
  concept is introduced or altered. This handoff is capability, not
  authority — it authorises nothing, and it does not check whether
  anything is authorised, per the Governance Review's own Section 5.6
  determination.
- **First-class Goal domain types.** `Goal.text` maps directly to
  `PlanningRequest.goal` as a plain `String`; no new `Goal`-named type of
  any kind is introduced anywhere in `src/`.
- **Chapter 23 Goal Manager work.** `docs/architecture/23-goal-manager.md`
  is not read as authority, cited, implemented against, or modified.
- **`ResponseComposer` modification.** Zero changes to this file, in any
  form.
- **Persistent memory.** No relation to, dependency on, or interaction
  with `MemoryStore`, `MemorySource`, or any persistence mechanism.
- **Voice or Home Assistant integration.** No relation to, dependency
  on, or interaction with either.
- **Unrelated cleanup or contract redesign.** No change to any file not
  specifically named in Section 3, below, for any reason, including
  formatting, renaming, or "while I'm in here" fixes.
- **`PlanningRequest`, `PlannerRuntime`, `PlanCandidate`,
  `ReasoningProviderResponse`, or any other Volume 1–3 governed contract.**
  All are used exactly as already approved, unchanged.
- **`ReplyDeliveryCoordinator`, `CommunicationConversationCoordinator`,
  `ConversationTurnReasoningCoordinator`, or `CommunicationIntake`.** None
  of the four is modified in any way.
- **Any retrieval, ranking, deliberation, or planning algorithm.** This
  Unit constructs one object and reports one deferred outcome; it
  contains no decision logic of any kind.

---

## 3. Explicit Exclusions — What This Handoff Must Never Own

Restated in the same closing form
`MEMORY_SOURCE_INTEGRATION_SCOPE_LOCK.md` Section 2 and
`CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md` Section 2 already establish:
this handoff prepares a request. It does not own planning.

- **Planning itself.** Exclusively `PlannerRuntime`'s own, sole,
  unchanged responsibility, invoked only through `plan()` — an operation
  this Unit cannot even reach, holding no reference to it.
- **Candidate generation.** A distinct, separate, entirely future
  concern. This handoff never produces, requests, or waits for a
  `PlanCandidate`.
- **Task Management.** A distinct, already-existing, unmodified
  subsystem this Unit has no dependency on.
- **Execution.** A distinct, already-existing, unmodified subsystem this
  Unit has no dependency on.
- **Authorisation.** Exclusively the Permission Engine's own
  constitutional responsibility ("Trust authorises"), downstream of
  Planning, never exercised or anticipated by this handoff.
- **The persistent Goal concept (Chapter 23).** A distinct, separate,
  unimplemented concept this Unit does not touch, extend, or reconcile
  further than the Governance Review's own Section 4 determination
  already did (related but distinct, addressed elsewhere).

---

## 4. Test Obligations (Binding)

**New: `tests/runtime/GoalPlanningHandoffCoordinatorTest.kt`**

- Field-by-field `PlanningRequest` construction correctness (fixed
  factory value; `senderPrincipalId`, `correlationId.value`, `goal.text`
  extraction; `RequestOrigin.TEXT`/`RequestPriority.NORMAL` defaults).
- Exactly-one-invocation proof for `planningSessionIdFactory`, via a
  counting fake.
- Two calls with two different factory outputs produce two different
  `planningSessionId` values.
- `reason == PlanningDeferralReason.CANDIDATE_GENERATION_UNAVAILABLE` on
  every call.
- `detail` non-blank, naming the real reason.
- A blank-returning factory causes `IllegalArgumentException`.
- A throwing factory propagates that exception unchanged.
- Structural test: constructor accepts exactly one parameter, of type
  `() -> String` — proof this class cannot reach `PlannerRuntime`,
  `IdentityService`, or `EventBus`.

**Modified: `tests/runtime/ConversationReplyCoordinatorTest.kt`**

- All existing `Reply`/`NotAccepted` assertions updated to
  `ConversationOutcome`.
- `Goal` input: `replyDeliveryCoordinator.composeAndDeliver` never
  called (structural, zero-invocation proof); `goalPlanningHandoffCoordinator.initiatePlanning`
  called exactly once with the correct arguments; result equals
  `ConversationOutcome.PlanningDeferred` wrapping `initiatePlanning`'s
  own return value, unwrapped, unchanged.
- `Reply`/`NoAction` input: `goalPlanningHandoffCoordinator.initiatePlanning`
  never called (symmetric structural proof).
- `composeAndDeliver` returning `GatedOutcome.NotAccepted` maps to
  `ConversationOutcome.NotAccepted` with the same reason, preserving
  existing rejection behaviour through the one-level unwrap.

**Modified (inspected, updated only if required by the compiler):
`tests/composition/ParkerRuntimeConversationPipelineTest.kt`,
`tests/composition/ParkerRuntimeFailureHandlingTest.kt`,
`tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt`**

- A `Goal`-producing fake `ReasoningProvider` results in
  `ParkerRuntimeOutcome.PlanningDeferred`, with
  `outcome.reason == PlanningDeferralReason.CANDIDATE_GENERATION_UNAVAILABLE`.
- Any exhaustive `when` over `ConversationOutcome` or
  `ParkerRuntimeOutcome` in these three files is updated for the new
  variant(s), as the compiler requires.
- Negative, structural proof (via the constructor-arity test above, not
  a hope): no test double anywhere in this Unit's own new or modified
  test files constructs a non-empty `PlanCandidate` list or calls
  `PlannerRuntime.plan`.

No test file beyond these four is modified by this Unit.

---

## 5. Implementation File Boundary (Binding)

**Exactly these files may be created or modified by the implementation
this Scope Lock authorises. No other production file may be modified —
any discovered need to touch a file not listed here is grounds to stop
and report a genuine blocker, not licence to proceed.**

**New:**

- `src/runtime/GoalPlanningHandoffCoordinator.kt` — `GoalPlanningHandoffCoordinator`,
  `GoalPlanningHandoffOutcome`, `PlanningDeferralReason`.
- `src/runtime/ConversationOutcome.kt` — `ConversationOutcome`.

**Modified:**

- `src/runtime/ConversationReplyCoordinator.kt`
- `src/composition/ParkerRuntimeOutcome.kt`
- `src/composition/ParkerRuntime.kt`

**Test files (only these; Section 4):**

- `tests/runtime/GoalPlanningHandoffCoordinatorTest.kt` (new)
- `tests/runtime/ConversationReplyCoordinatorTest.kt` (modified)
- `tests/composition/ParkerRuntimeConversationPipelineTest.kt` (inspected; modified only if the compiler requires it)
- `tests/composition/ParkerRuntimeFailureHandlingTest.kt` (inspected; modified only if the compiler requires it)
- `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` (inspected; modified only if the compiler requires it)

**Documentation (directly necessary only):**

- `docs/implementation/IMPLEMENTATION_HISTORY.md` — one new entry
  recording this Unit's implementation, mirroring every prior Unit's own
  entry format.
- `docs/architecture/IMPLEMENTATION_GAPS.md` — reconciliation only if
  this Unit closes, narrows, or newly discloses a recorded gap (e.g. the
  Reasoning-to-Planning dead end this Governance Review itself
  identified); no unrelated gap entry is touched.

**No other file — production, test, or documentation — may be created,
modified, or deleted by this Unit.**

---

## 6. Governing Principle

**This handoff prepares a request. It does not plan, decide, execute, or
authorise anything.**

Every value this Unit can ever produce has exactly one shape:
`GoalPlanningHandoffOutcome.Deferred`, carrying a `PlanningRequest`
nothing has yet acted on. This Unit originates no plan, no task, no
execution, and no permission decision. If a future Unit's real
`PlannerRuntime.plan()` call and this Unit's own constructed
`PlanningRequest` ever appear to disagree, the future Unit's real call is
authoritative — this Unit's own output is a prepared value, never a
decision.

---

## 7. Ownership

- **Exactly one production owner.** `parker.composition.ParkerRuntime`
  constructs `GoalPlanningHandoffCoordinator`, exactly once, at startup.
- **Exactly one production caller.** `ConversationReplyCoordinator` —
  calling `goalPlanningHandoffCoordinator.initiatePlanning(...)` directly
  on the `Goal` branch of its own existing sequencing. No other runtime
  component may become a second caller without a future Scope Lock
  revision.

---

## 8. Lifetime and Threading

- **Construction.** Once, at startup, alongside every other stateless
  collaborator `ParkerRuntime` builds.
- **Use.** Stateless, per call. Each `initiatePlanning` call is
  independent; nothing observed or returned by one call is retained for,
  or influences, the next.
- **Sharing.** A single instance is shared across every concurrent
  request the runtime handles — it holds no mutable state of its own to
  guard.
- **Coroutine expectations.** `initiatePlanning` is `suspend`-declared,
  for forward-compatibility only (Contract Design Section 2); its own
  current body never suspends.

---

## 9. Relationship to the Constitution

`docs/architecture/parker-constitution.md`: "Parker owns authority.
Modules provide capability" and "Cognition proposes. Trust authorises.
Runtime executes." This handoff sits entirely within Cognition — it
proposes a request, never authorises, never executes. Restating
`AUTHENTICATION_AND_TRUST_GOVERNANCE.md`'s Core Principle: "Reasoning
cannot grant permissions." Neither can this handoff. It is the component
that would carry a proposal one step closer to Planning; it is not, and
must never become, the component that decides whether that proposal may
proceed.

---

## 10. Acceptance of This Scope Lock

This Scope Lock is binding once accepted. A future implementation Unit
authorised against it must satisfy the Included list (Section 2.1) and
the file boundary (Section 5) exactly, must not implement anything in
the Excluded list (Sections 2.2, 3), and must treat any discovered need
to exceed either as grounds to pause and request a Scope Lock revision —
not as licence to proceed under this one.
