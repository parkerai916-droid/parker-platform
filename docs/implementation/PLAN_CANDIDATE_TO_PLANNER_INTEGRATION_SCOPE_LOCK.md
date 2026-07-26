# Plan Candidate to PlannerRuntime Integration -- Scope Lock

**Status: Documentation-only. No Kotlin written. No implementation performed.
No commit made.** Freezes
`docs/architecture/PLAN_CANDIDATE_TO_PLANNER_INTEGRATION_GOVERNANCE_REVIEW.md`
and
`docs/architecture/PLAN_CANDIDATE_TO_PLANNER_INTEGRATION_CONTRACT_DESIGN.md`
into an exact, bounded implementation task. Every code block below is
frozen text for a future Implementation Unit to copy, not a proposal open
for further design discussion.

**Path correction, per this Scope Lock's own "use actual repository
paths" instruction.** The task's own illustrative file list names
`src/runtime/GoalPlanningHandoffOutcome.kt` and
`src/runtime/ParkerRuntimeOutcome.kt`. Neither exists at that path in this
repository:

- `GoalPlanningHandoffOutcome` (and `PlanningDeferralReason`) are declared
  inside `src/runtime/GoalPlanningHandoffCoordinator.kt` -- there is no
  separate `GoalPlanningHandoffOutcome.kt` file, and this Unit does not
  create one (splitting the file is an unrelated, unauthorised cleanup).
- `ParkerRuntimeOutcome` lives at `src/composition/ParkerRuntimeOutcome.kt`,
  and `ParkerRuntime` itself lives at `src/composition/ParkerRuntime.kt` --
  both under `src/composition/`, not `src/runtime/`.

Section 12 below uses the corrected paths throughout.

---

## 1. Coordinator Revision (Freeze)

Class name, unchanged:

```kotlin
GoalPlanningHandoffCoordinator
```

Constructor, exactly as specified:

```kotlin
class GoalPlanningHandoffCoordinator(
    private val planningSessionIdFactory: () -> String,
    private val planCandidateGenerator: PlanCandidateGenerator,
    private val plannerRuntime: PlannerRuntime,
)
```

Public method signature, unchanged from today:

```kotlin
suspend fun initiatePlanning(
    originalMessage: InboundOwnerMessage,
    goal: ReasoningProviderResponse.Goal,
): GoalPlanningHandoffOutcome
```

Internal sequence, frozen in full (the first four lines -- `PlanningRequest`
construction -- are **unchanged** from the current implementation; only the
lines after it are new):

```kotlin
suspend fun initiatePlanning(
    originalMessage: InboundOwnerMessage,
    goal: ReasoningProviderResponse.Goal,
): GoalPlanningHandoffOutcome {
    val planningRequest = PlanningRequest(
        planningSessionId = PlanningSessionId(planningSessionIdFactory()),
        initiatingPrincipalId = originalMessage.senderPrincipalId,
        goal = goal.text,
        correlationId = originalMessage.correlationId.value,
    )
    val candidates = planCandidateGenerator.generate(planningRequest)
    val result = plannerRuntime.plan(planningRequest, candidates)
    return GoalPlanningHandoffOutcome.Planned(result)
}
```

Frozen, non-negotiable properties of this sequence:

- The exact same `planningRequest` local value -- one instance, constructed
  once -- is passed to both `planCandidateGenerator.generate` and
  `plannerRuntime.plan`. No second `PlanningRequest` is ever constructed.
- `candidates`, the exact `List<PlanCandidate>` `generate` returns, is
  passed to `plan` unchanged and in the same order -- no `sortedBy`,
  `filter`, `map`, `toList()` copy-with-reordering, or any other
  transformation.
- `plan`'s return value is wrapped in `GoalPlanningHandoffOutcome.Planned`
  and returned immediately -- no inspection of which `PlanningSessionResult`
  variant it is before wrapping.
- `PlanningDeferralReason` and the top-level `DEFERRAL_DETAIL` constant
  (both currently in `GoalPlanningHandoffCoordinator.kt`) are deleted in
  full -- neither is referenced by the revised method.

---

## 2. Empty-Candidate Behaviour (Freeze)

When `planCandidateGenerator.generate(planningRequest)` returns
`emptyList()`, the coordinator calls:

```kotlin
plannerRuntime.plan(planningRequest, emptyList())
```

exactly as it would for any non-empty list -- there is no `if
(candidates.isEmpty())` branch anywhere in this method. `PlannerRuntime`
remains the sole owner of no-viable-candidate handling: `InMemoryPlannerRuntime.plan`
already handles this case correctly today, with no change required --
`PlanDecision.decide(request.goal, emptyList())` returns
`PlanDecisionResult.NoViableCandidate(rejections = emptyList())` (confirmed
directly from `src/contracts/PlanDecision.kt`'s own KDoc: "This includes the
zero-candidates case, where `rejections` is empty because there was nothing
to reject"), and `InMemoryPlannerRuntime.plan` already returns
`PlanningSessionResult.Failed(reason = "no Plan Candidates were supplied for
this Planning Session", rejections = emptyList())` for exactly this case.
No change to either file is required or authorised for this behaviour.

---

## 3. Fault Behaviour (Freeze)

`GoalPlanningHandoffCoordinator.initiatePlanning` gains no `try`/`catch` --
none exists today, and none is added. Frozen:

- An exception thrown by `planCandidateGenerator.generate` propagates
  unchanged; `plannerRuntime.plan` is never called in that case (the second
  statement never executes).
- An exception thrown by `plannerRuntime.plan` propagates unchanged.
- `PlanningSessionResult.Failed` is a normal, non-exceptional **returned
  value** -- not a thrown exception -- and is wrapped in
  `GoalPlanningHandoffOutcome.Planned` exactly like `Completed`/`Rejected`.
  It must never be intercepted, rethrown, or treated as a fault by this
  coordinator.
- Either propagated exception reaches `ConversationReplyCoordinator.submitAndDeliver`
  (no `try`/`catch` there either, unchanged), then
  `ParkerRuntime.submitOwnerMessage`'s existing outer `try`/`catch`
  (`src/composition/ParkerRuntime.kt`, confirmed lines 514-548), which
  already has no special handling for a "planning" fault: it falls into the
  final `catch (e: Exception)` block and is reported as
  `ParkerRuntimeOutcome.Failed(PipelineStage.UNKNOWN, e)`. Neither
  `PlanCandidateGenerator` nor `PlannerRuntime` throws
  `TimeoutCancellationException` anywhere in this repository today, so this
  path is never classified `PipelineStage.REASONING`.
- No new `PipelineStage` value is introduced. `PipelineStage.UNKNOWN`'s
  existing KDoc and meaning are unchanged.

---

## 4. Handoff Outcome Replacement (Freeze)

Removed in full, from `src/runtime/GoalPlanningHandoffCoordinator.kt`:

```kotlin
enum class PlanningDeferralReason { CANDIDATE_GENERATION_UNAVAILABLE }

data class Deferred(
    val planningRequest: PlanningRequest,
    val reason: PlanningDeferralReason,
    val detail: String,
) : GoalPlanningHandoffOutcome()

private const val DEFERRAL_DETAIL = "..."
```

Replacement, frozen exactly as specified:

```kotlin
sealed class GoalPlanningHandoffOutcome {
    data class Planned(
        val planningSessionResult: PlanningSessionResult,
    ) : GoalPlanningHandoffOutcome()
}
```

`GoalPlanningHandoffOutcome` remains exactly one variant, mirroring its own
current "sealed with exactly one variant" precedent. **No other variant is
authorised in this Unit**, specifically forbidding any wrapper named for
completed, rejected, failed, no-viable-candidate, deferred, or
waiting-for-input -- `PlanningSessionResult` itself already distinguishes
`Completed`/`Rejected`/`Failed`; inventing a parallel `GoalPlanningHandoffOutcome`
variant per `PlanningSessionResult` variant would duplicate that
distinction one layer up for no reason. `PlanningSessionResult` is carried
into `Planned.planningSessionResult` completely unchanged -- no field is
copied out, renamed, or re-derived.

---

## 5. ConversationOutcome (Freeze)

Current source (`src/runtime/ConversationOutcome.kt`, confirmed by direct
reading):

```kotlin
data class PlanningDeferred(val outcome: GoalPlanningHandoffOutcome) : ConversationOutcome()
```

Frozen replacement -- rename only, field name and type unchanged:

```kotlin
data class Planned(val outcome: GoalPlanningHandoffOutcome) : ConversationOutcome()
```

Exact field type: `outcome: GoalPlanningHandoffOutcome` (the sealed
supertype, not narrowed to `.Planned` -- matching how the current
`PlanningDeferred.outcome` field is also typed as the sealed supertype, not
narrowed to `.Deferred`). No parallel planner-result variant is added at
this layer; `ConversationOutcome` remains exactly three variants
(`ReplyDelivered`, `Planned`, `NotAccepted`).

---

## 6. ParkerRuntimeOutcome (Freeze)

Current source (`src/composition/ParkerRuntimeOutcome.kt`, confirmed by
direct reading):

```kotlin
data class PlanningDeferred(val outcome: GoalPlanningHandoffOutcome) : ParkerRuntimeOutcome()
```

Frozen replacement -- rename only, field name and type unchanged:

```kotlin
data class Planned(val outcome: GoalPlanningHandoffOutcome) : ParkerRuntimeOutcome()
```

Frozen semantic mapping, explicit and exhaustive:

| Source result | `GoalPlanningHandoffOutcome` | `ConversationOutcome` | `ParkerRuntimeOutcome` |
|---|---|---|---|
| `PlanningSessionResult.Completed` | `Planned(result)` | `Planned(outcome)` | `Planned(outcome)` |
| `PlanningSessionResult.Rejected` | `Planned(result)` | `Planned(outcome)` | `Planned(outcome)` |
| `PlanningSessionResult.Failed` | `Planned(result)` | `Planned(outcome)` | `Planned(outcome)` |
| Uncaught exception (generator or planner) | -- (never constructed) | -- (never constructed) | existing `Failed(PipelineStage.UNKNOWN, e)`, unchanged |

A planner-returned `Failed` is the result of an attempted, completed
Planning Session -- it is not an uncaught runtime failure, and must never be
converted into `ParkerRuntimeOutcome.Failed`. Only a genuine thrown
exception (from `PlanCandidateGenerator`, `PlannerRuntime`, or anywhere
upstream) produces `ParkerRuntimeOutcome.Failed`, exactly as today.

---

## 7. ConversationReplyCoordinator Routing (Freeze)

Current source (`src/runtime/ConversationReplyCoordinator.kt`, confirmed by
direct reading), the one line that changes:

```kotlin
is ReasoningProviderResponse.Goal ->
    ConversationOutcome.PlanningDeferred(goalPlanningHandoffCoordinator.initiatePlanning(message, response))
```

Frozen replacement -- rename only:

```kotlin
is ReasoningProviderResponse.Goal ->
    ConversationOutcome.Planned(goalPlanningHandoffCoordinator.initiatePlanning(message, response))
```

Frozen, unchanged by this Unit:

- The `Reply` branch continues to call `deliverReply`, unchanged.
- The `NoAction` branch continues to call `deliverReply`, unchanged.
- `goalPlanningHandoffCoordinator.initiatePlanning` is called exactly once,
  only on the `Goal` branch.
- `Reply` and `NoAction` each make **zero** calls to
  `goalPlanningHandoffCoordinator` -- the `when` block's structure already
  guarantees this, unchanged.
- `Goal` makes **zero** calls to `replyDeliveryCoordinator` -- unchanged.
- The constructor (`communicationConversationCoordinator`,
  `replyDeliveryCoordinator`, `goalPlanningHandoffCoordinator` -- three
  dependencies) does not change.
- No other line in this file is authorised to change.

---

## 8. ParkerRuntime Composition (Freeze)

Current source (`src/composition/ParkerRuntime.kt`, confirmed lines
357-365):

```kotlin
val goalPlanningHandoffCoordinator = GoalPlanningHandoffCoordinator(
    planningSessionIdFactory = { UUID.randomUUID().toString() },
)
```

Frozen replacement, in the same composition method, immediately before this
existing construction:

```kotlin
val taskManagerRuntime = InMemoryTaskManagerRuntime(identityService, eventBus)
val plannerRuntime = InMemoryPlannerRuntime(identityService, eventBus, taskManagerRuntime)
val planCandidateGenerator = DefaultPlanCandidateGenerator()

val goalPlanningHandoffCoordinator = GoalPlanningHandoffCoordinator(
    planningSessionIdFactory = { UUID.randomUUID().toString() },
    planCandidateGenerator = planCandidateGenerator,
    plannerRuntime = plannerRuntime,
)
```

Constructors used, confirmed by direct reading, exactly as declared today
-- no change to either class:

```kotlin
class InMemoryTaskManagerRuntime(
    private val identityService: IdentityService,
    private val eventBus: EventBus,
) : TaskProposalIntake

class InMemoryPlannerRuntime(
    private val identityService: IdentityService,
    private val eventBus: EventBus,
    private val taskProposalIntake: TaskProposalIntake,
    private val planDecision: PlanDecision = DefaultPlanDecision(),
) : PlannerRuntime
```

Frozen properties of this construction:

- `identityService` and `eventBus` are the composition method's own
  already-existing local values (confirmed already reused for
  `RuntimeEventLogger`, `ResponseComposer`, `InMemoryCommunicationIntake`,
  and others earlier in the same method) -- no new dependency is
  constructed or introduced to support this wiring.
- `planDecision` is left at its default (`DefaultPlanDecision()`) -- no
  explicit value is passed.
- `taskManagerRuntime`, `plannerRuntime`, and `planCandidateGenerator` are
  local `val`s inside the same composition method, matching the existing
  convention for `communicationIntake`, `reasoningProvider`,
  `responseComposer`, `responseDelivery`, and `replyDeliveryCoordinator` (all
  local `val`s, never promoted to instance properties). This Unit adds no
  new class-level property. `ParkerRuntime`'s own `stop()` method (confirmed
  lines 573-591) shuts down only `runtimeEventLogger` today, guarded by
  `::runtimeEventLogger.isInitialized`; neither `InMemoryTaskManagerRuntime`
  nor `InMemoryPlannerRuntime` exposes a shutdown method, so no change to
  `stop()` is required or authorised.
- No new dependency-injection framework, service locator, global singleton,
  new assembly module, or new runtime layer is introduced. This is exactly
  three additional local-`val` construction statements in the one existing
  composition method, wired using values the composition root already
  holds.

---

## 9. Task Manager Boundary (Freeze)

`InMemoryTaskManagerRuntime` is constructed in exactly one place --
`ParkerRuntime.kt`'s composition method -- solely because
`InMemoryPlannerRuntime`'s constructor requires a `TaskProposalIntake`
value with no default. Frozen:

- `GoalPlanningHandoffCoordinator` depends only on `PlannerRuntime`
  (Section 1's constructor) -- it never declares a `TaskProposalIntake` or
  `InMemoryTaskManagerRuntime` parameter, and never references
  `taskManagerRuntime` by any name.
- No task acceptance policy, task execution, lifecycle rule, scheduling
  logic, tool invocation, or task-status mapping is added anywhere in this
  Unit's scope.
- No new public contract exposes `TaskProposalIntake` or
  `InMemoryTaskManagerRuntime` beyond its existing, unchanged interface.
- **Disclosed, confirmed-safe boundary on what this construction can
  actually do at runtime**, from direct reading of
  `InMemoryTaskManagerRuntime.kt`'s own class KDoc: it "Constructs, but
  never submits, an `AgentRunCommand`" -- `START` is built and stored, but
  `AgentRunCommandChannel.submit` is never called, because "no
  implementation of that interface exists yet." Wiring this class into the
  live conversational path therefore allows a real `Task` record to be
  created and to reach `QUEUED`, but cannot cause any Agent Run, tool
  invocation, or execution to actually occur -- there is no code path in
  this repository today capable of consuming the constructed
  `AgentRunCommand`. This is stated here as a verified fact grounding
  Exclusion 15's "no execution or tool invocation," not as a new design
  decision.

---

## 10. System Principal IDs (Resolved, Frozen)

**Chosen option: 3 -- duplicate the literals, because the repository
already treats these IDs as composition-root configuration values.**
Confirmed by direct reading of `ParkerRuntime.kt`'s own `private companion
object` (lines 593-598):

```kotlin
private companion object {
    const val NOTIFY_OWNER_VERB_PHRASE = "notify owner"
    val SYSTEM_PARKER_PRINCIPAL_ID = PrincipalId("system.parker")
    val CONVERSATION_ENGINE_PRINCIPAL_ID = PrincipalId("system.conversation-engine")
    val RESPONSE_COMPOSER_PRINCIPAL_ID = PrincipalId("system.response-composer")
}
```

None of these three existing constants is imported from the component it
identifies -- each is `ParkerRuntime.kt`'s own independently-declared
literal, registered via `registerActive` in `registerSystemIdentities`
(confirmed lines 375-386). This is the established convention this Unit
follows, rather than exposing `InMemoryPlannerRuntime`'s and
`InMemoryTaskManagerRuntime`'s own `private companion object` constants
(both are `private`, per direct reading of each file, and are not visible
to `ParkerRuntime.kt`) or using reflection to reach them (both explicitly
forbidden).

Frozen addition to the same `private companion object`:

```kotlin
private companion object {
    const val NOTIFY_OWNER_VERB_PHRASE = "notify owner"
    val SYSTEM_PARKER_PRINCIPAL_ID = PrincipalId("system.parker")
    val CONVERSATION_ENGINE_PRINCIPAL_ID = PrincipalId("system.conversation-engine")
    val RESPONSE_COMPOSER_PRINCIPAL_ID = PrincipalId("system.response-composer")
    val PLANNER_RUNTIME_PRINCIPAL_ID = PrincipalId("system.planner-runtime")
    val TASK_MANAGER_RUNTIME_PRINCIPAL_ID = PrincipalId("system.task-manager-runtime")
}
```

**The two literal string values are not new inventions -- they must exactly
match** the already-existing private constants confirmed by direct reading:
`InMemoryPlannerRuntime.kt` line 120,
`PLANNER_RUNTIME_PRINCIPAL_ID = PrincipalId("system.planner-runtime")`; and
`InMemoryTaskManagerRuntime.kt` line 226,
`TASK_MANAGER_RUNTIME_PRINCIPAL_ID = PrincipalId("system.task-manager-runtime")`.
`PrincipalId` is a `@JvmInline value class(val value: String)`, so identity
resolution is by string-value equality -- `ParkerRuntime.kt`'s own copy must
carry the identical string, or `InMemoryPlannerRuntime.plan`'s and
`InMemoryTaskManagerRuntime`'s own internal `identityService.resolve` calls
will fail to find a registered Principal and both runtimes will report
`PlanningSessionResult.Failed`/silently drop events. This is exactly why
duplicating the literal (not re-deriving or guessing it) is safe here: both
strings are already fixed, published-in-KDoc values, not free choices.

Frozen registration, in `registerSystemIdentities` (confirmed lines
375-386), two new calls following the exact existing pattern:

```kotlin
private suspend fun registerSystemIdentities(identityService: InMemoryIdentityService) {
    stage("system identity registration") {
        registerActive(identityService, SYSTEM_PARKER_PRINCIPAL_ID, PrincipalType.SYSTEM, "Parker System")
        registerActive(identityService, CONVERSATION_ENGINE_PRINCIPAL_ID, PrincipalType.SYSTEM, "Conversation Engine")
        registerActive(identityService, RESPONSE_COMPOSER_PRINCIPAL_ID, PrincipalType.SYSTEM, "Response Composer")
        registerActive(identityService, PLANNER_RUNTIME_PRINCIPAL_ID, PrincipalType.SYSTEM, "Planner Runtime")
        registerActive(identityService, TASK_MANAGER_RUNTIME_PRINCIPAL_ID, PrincipalType.SYSTEM, "Task Manager Runtime")
        registerActive(
            identityService,
            PrincipalId(config.ownerPrincipalId),
            PrincipalType.USER,
            config.ownerDisplayName,
        )
    }
}
```

Frozen facts:

- Exact principal ID values: `PrincipalId("system.planner-runtime")`,
  `PrincipalId("system.task-manager-runtime")`.
- Exact registration location: `ParkerRuntime.registerSystemIdentities`,
  the existing method, no new method introduced.
- Exact number of new registrations: 2.
- Exact identity role/type: `PrincipalType.SYSTEM` for both, matching the
  other three system identities registered in the same method.
- No authentication or permission-policy redesign occurs -- `registerActive`
  and `PrincipalType.SYSTEM` are both pre-existing, unchanged.

---

## 11. Logging (Freeze)

Current source (`src/composition/ParkerRuntime.kt`, confirmed lines
529-537):

```kotlin
is ConversationOutcome.PlanningDeferred -> when (val handoffOutcome = outcome.outcome) {
    is GoalPlanningHandoffOutcome.Deferred -> {
        logger.info(
            "Planning initiation deferred (correlationId=${message.correlationId.value}, " +
                "reason=${handoffOutcome.reason})",
        )
        ParkerRuntimeOutcome.PlanningDeferred(handoffOutcome)
    }
}
```

Frozen replacement -- the minimum wording change required, since
`handoffOutcome.reason` (a `PlanningDeferralReason`) no longer exists:

```kotlin
is ConversationOutcome.Planned -> when (val handoffOutcome = outcome.outcome) {
    is GoalPlanningHandoffOutcome.Planned -> {
        val sessionResult = handoffOutcome.planningSessionResult
        logger.info(
            "Planning attempted (correlationId=${message.correlationId.value}, " +
                "planningSessionId=${sessionResult.planningSessionId.value}, " +
                "result=${sessionResult::class.simpleName})",
        )
        ParkerRuntimeOutcome.Planned(handoffOutcome)
    }
}
```

Frozen, permitted log content -- exactly three values, all already public,
non-sensitive identifiers or type names carried on
`PlanningSessionResult` itself:

- `message.correlationId.value` (already logged today, unchanged).
- `sessionResult.planningSessionId.value` (an opaque, UUID-derived session
  identifier, not user content).
- `sessionResult::class.simpleName` (`"Completed"`, `"Rejected"`, or
  `"Failed"` -- a type name, not a message).

Explicitly not logged, and none of `PlanningSessionResult`'s three variants
carries any of these fields to begin with, so this exclusion is satisfied
by the type shape itself, not by a runtime check this code must perform:
full memory content, conversation history, secrets, tool arguments, or task
payloads. `PlanningSessionResult.Failed.reason` (free text) and
`rejections` (structured but potentially verbose) are deliberately **not**
logged here -- only the terminal variant name and session ID are, keeping
this line's information content equivalent to today's
`reason=${handoffOutcome.reason}` (an enum name), not an expansion of what
is disclosed.

Logging remains owned exclusively by `ParkerRuntime`. No logging is added
inside `GoalPlanningHandoffCoordinator`, `PlanCandidateGenerator`, or
`ConversationReplyCoordinator` -- none of the three requires it to satisfy
any obligation in this Scope Lock, so the "unless unavoidable" exception is
not triggered.

---

## 12. Permitted Production Files (Freeze, Corrected Paths)

Authorised for modification, exact repository paths:

1. `src/runtime/GoalPlanningHandoffCoordinator.kt` -- contains both
   `GoalPlanningHandoffCoordinator` and `GoalPlanningHandoffOutcome` (and,
   before this Unit, `PlanningDeferralReason` and `DEFERRAL_DETAIL`, both
   removed). No separate `GoalPlanningHandoffOutcome.kt` file exists or is
   created.
2. `src/runtime/ConversationOutcome.kt`.
3. `src/runtime/ConversationReplyCoordinator.kt`.
4. `src/composition/ParkerRuntime.kt` (not `src/runtime/ParkerRuntime.kt`).
5. `src/composition/ParkerRuntimeOutcome.kt` (not
   `src/runtime/ParkerRuntimeOutcome.kt`).

**Not authorised for any change**, confirmed to all live outside the five
files above:

- `PlanningRequest`, `PlanCandidate`, `PlanDecision`, `PlannerRuntime` --
  all four declared in `src/contracts/PlanDecision.kt`; this file is
  excluded from modification in full.
- `PlanCandidateGenerator` -- `src/contracts/PlanCandidateGenerator.kt`.
- `DefaultPlanCandidateGenerator` --
  `src/runtime/DefaultPlanCandidateGenerator.kt`.
- `InMemoryPlannerRuntime` -- `src/runtime/InMemoryPlannerRuntime.kt`.
- `InMemoryTaskManagerRuntime` --
  `src/runtime/InMemoryTaskManagerRuntime.kt`.

This Unit's own design (Sections 1, 8) constructs instances of the last two
classes and calls methods already present on all eight -- it requires no
signature or behavioural change to any of them, confirmed by the exact
constructor and method signatures transcribed in Sections 1, 2, 8, and 9
above. **If compilation genuinely requires a change to any file in this
list, implementation must stop and report the conflict, not expand scope.**

---

## 13. Tests (Freeze)

Files to modify:

- `tests/runtime/GoalPlanningHandoffCoordinatorTest.kt` -- rewritten for
  the new three-dependency constructor and delegating behaviour (today's
  file tests the zero-dependency `Deferred`-only behaviour; that entire
  test class's assumptions are obsolete under this revision).
- `tests/runtime/ConversationReplyCoordinatorTest.kt` -- updated for the
  `Planned` rename and, if not already present, a fake
  `GoalPlanningHandoffCoordinator`/return value adjusted to the new outcome
  shape.
- `tests/composition/ParkerRuntimeConversationPipelineTest.kt` (and any
  sibling composition-level test asserting on `ParkerRuntimeOutcome` for a
  `Goal`-producing message) -- updated for the `Planned` rename; inspected,
  and modified only if compilation requires it, per the existing
  "inspect first" convention from prior Units.

New fakes/stubs, following this repository's existing "hand-written fake
class implementing the interface" convention (no mocking framework is
present or introduced):

- A fake `PlanCandidateGenerator` capturing its received `request` and
  returning a caller-configured `List<PlanCandidate>` (including
  `emptyList()`), or throwing a caller-configured exception.
- A fake `PlannerRuntime` capturing its received `request`/`candidates` and
  returning a caller-configured `PlanningSessionResult` (one test per
  variant), or throwing a caller-configured exception.

Numbered coverage, mapped to the files above:

1. `PlanningRequest` constructed once -- `GoalPlanningHandoffCoordinatorTest.kt`: assert the fake generator and fake planner both observe the identical `PlanningSessionId`/fields.
2. Generator called exactly once -- `GoalPlanningHandoffCoordinatorTest.kt`: fake generator records call count.
3. Planner called exactly once -- `GoalPlanningHandoffCoordinatorTest.kt`: fake planner records call count.
4. Same `PlanningRequest` instance passed to generator and planner -- `GoalPlanningHandoffCoordinatorTest.kt`: reference-equality (`===`) assertion on the captured value.
5. Candidate list passed unchanged -- `GoalPlanningHandoffCoordinatorTest.kt`: fake planner's captured `candidates` equals the fake generator's configured return value.
6. Candidate ordering preserved -- `GoalPlanningHandoffCoordinatorTest.kt`: configure a multi-element, deliberately-unsorted candidate list; assert order is identical on the planner side.
7. Empty candidate list passed to planner -- `GoalPlanningHandoffCoordinatorTest.kt`: configure the fake generator to return `emptyList()`; assert the fake planner receives `emptyList()`, not a short-circuited result.
8. Planner not called if generator throws -- `GoalPlanningHandoffCoordinatorTest.kt`: fake generator throws; assert fake planner's call count is zero.
9. Generator exception propagates unchanged -- `GoalPlanningHandoffCoordinatorTest.kt`: assert the same exception instance (or type + message) surfaces from `initiatePlanning`.
10. Planner exception propagates unchanged -- `GoalPlanningHandoffCoordinatorTest.kt`: same pattern, fake planner throws.
11. Every `PlanningSessionResult` variant wrapped unchanged -- `GoalPlanningHandoffCoordinatorTest.kt`: three tests (`Completed`, `Rejected`, `Failed`), each asserting `result.planningSessionResult === ` the exact configured value.
12. `Reply` makes zero planning calls -- `ConversationReplyCoordinatorTest.kt`: existing/updated fake `GoalPlanningHandoffCoordinator` call-count assertion on the `Reply` branch.
13. `NoAction` makes zero planning calls -- `ConversationReplyCoordinatorTest.kt`: same, `NoAction` branch.
14. `Goal` makes exactly one planning call -- `ConversationReplyCoordinatorTest.kt`: call-count assertion on the `Goal` branch.
15. Correct `ConversationOutcome.Planned` mapping -- `ConversationReplyCoordinatorTest.kt`: assert the returned `ConversationOutcome` is `Planned(outcome)` where `outcome` is exactly the coordinator's configured return value.
16. Correct `ParkerRuntimeOutcome.Planned` mapping -- `ParkerRuntimeConversationPipelineTest.kt` (or equivalent): assert `submitOwnerMessage` returns `Planned(outcome)` for a `Goal`-producing message.
17. Planner-returned `Failed` does not become `ParkerRuntimeOutcome.Failed` -- same file: configure the real or fake planning path to yield `PlanningSessionResult.Failed`; assert the top-level result is `ParkerRuntimeOutcome.Planned`, never `Failed`.
18. Uncaught exceptions still become the existing runtime failure outcome -- same file: an existing test already covers this for other stages; add or confirm one exercising a thrown exception from the planning path, asserting `ParkerRuntimeOutcome.Failed(PipelineStage.UNKNOWN, e)`.
19. Production composition constructs the required concrete runtimes -- `ParkerRuntimeConversationPipelineTest.kt`: a composition-level test submitting a `Goal`-producing message through the real, production-constructed `ParkerRuntime` (real `InMemoryPlannerRuntime` + real `InMemoryTaskManagerRuntime` + real `DefaultPlanCandidateGenerator`), asserting the result is `ParkerRuntimeOutcome.Planned` carrying a real, non-fabricated `PlanningSessionResult`. **The exact terminal variant (`Completed` vs. `Rejected` vs. `Failed`) is not frozen here** -- the current wiring makes `Completed` the most likely outcome (a verbatim single-candidate match against `DefaultPlanDecision`'s rules, followed by `InMemoryTaskManagerRuntime.submitProposal`'s "accept-only, for a resolvable owner" rule, with the message's sender already registered as the Owner Principal), but asserting an exact variant without having run the code would be stating a fact this document cannot actually observe; the implementer must assert whatever the real, observed outcome is.
20. No direct Task Manager call occurs from the coordinator -- `GoalPlanningHandoffCoordinatorTest.kt`: structural test (mirroring `DefaultPlanCandidateGeneratorTest`'s own "zero declared fields beyond..." precedent) confirming `GoalPlanningHandoffCoordinator`'s only fields are `planningSessionIdFactory`, `planCandidateGenerator`, and `plannerRuntime` -- no `TaskProposalIntake`/`InMemoryTaskManagerRuntime` field exists.
21. No tool, execution, or scheduling dependency is introduced -- same structural test as #20, extended to confirm no field of type `ExecutionPipeline`, `PermissionEngine`, `AgentRunCommandChannel`, or any Tool-related interface exists on `GoalPlanningHandoffCoordinator`.

No mocking framework is introduced -- all fakes are hand-written classes,
matching `DeterministicPlannerHarness.kt`'s and every existing coordinator
test's own convention.

---

## 14. Documentation Changes (Freeze)

Authorised files, exactly two:

- `docs/implementation/IMPLEMENTATION_HISTORY.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md`

`IMPLEMENTATION_HISTORY.md`: one new entry, inserted after the existing
"Plan Candidate Generation -- Contract and Reference Implementation" entry
and before "Current Vertical Slice," following that entry's own established
structure (What changed / Tests / Verification). The entry's prose must
state, at minimum, all five required facts:

1. `DefaultPlanCandidateGenerator` is now wired into the Goal planning path
   via `GoalPlanningHandoffCoordinator`'s revised constructor.
2. `PlannerRuntime.plan()` is now genuinely invoked from the production
   conversational path for the first time in this repository.
3. Planning results (`PlanningSessionResult.Completed`/`Rejected`/`Failed`)
   propagate unchanged through `GoalPlanningHandoffOutcome.Planned`,
   `ConversationOutcome.Planned`, and `ParkerRuntimeOutcome.Planned`.
4. `InMemoryTaskManagerRuntime` is assembled in `ParkerRuntime.kt`'s
   composition root **only** to satisfy `InMemoryPlannerRuntime`'s existing,
   mandatory `TaskProposalIntake` constructor dependency -- not as a
   separately-designed Task Manager production-wiring decision.
5. No task execution, tool invocation, or scheduling is introduced by this
   Unit -- `InMemoryTaskManagerRuntime` constructs but never submits an
   `AgentRunCommand` (no `AgentRunCommandChannel` implementation exists),
   so a real Task record can now be created and reach `QUEUED`, but no
   Agent Run or tool call can result from it.

`IMPLEMENTATION_GAPS.md`: one update appended to Gap #53, following the
existing pattern of prior updates to the same gap (each stating plainly
that "the underlying gap... remains Open" while narrowing one named item).
This update must state that this Unit closes the "`PlannerRuntime.plan()`
is still never called anywhere in production" item and the "`PlannerRuntime`/`TaskManagerRuntime`
remain absent from `ParkerRuntime.kt`'s production composition root" item
(both now false, given Section 8's construction), while explicitly leaving
open, unchanged: any remaining Goal-naming-collision item tied to Chapter
23/Goal Manager (not touched by this Unit), and, per this Scope Lock's own
explicit instruction, any execution, task-lifecycle, or tool-use gap --
which remains genuinely open per Section 9's own disclosed finding (Task
creation now reaches `QUEUED`, but no Agent Run is ever submitted or
executed). **Gap #53 as a whole is not closed by this update** -- only
these two named items are marked resolved within it, matching every prior
partial-narrowing update to this same gap.

---

## 15. Frozen Exclusions

This Unit does not, under any circumstance:

- Revise `PlanningRequest`, `PlanCandidate`, `PlanDecision`,
  `PlanCandidateGenerator`, `DefaultPlanCandidateGenerator`'s candidate
  generation policy, `PlannerRuntime`, `InMemoryPlannerRuntime`, or
  `InMemoryTaskManagerRuntime`.
- Add Planning Context, or give any component in this Unit access to
  `ReasoningContext`.
- Add new failure categories or a new `PipelineStage` value.
- Implement `WAITING_FOR_INPUT` or `CANCELLED` handling.
- Add safety-refusal distinctions or cross-goal conflict handling.
- Redesign Goal Manager, or touch Chapter 23 in any way.
- Modify `ResponseComposer`.
- Implement task execution, invoke any tool, or add scheduling logic.
- Redesign permissions or authentication.
- Touch persistent memory, voice, or Home Assistant integration.
- Perform any cleanup unrelated to this Unit's own frozen file list
  (Section 12) -- including not splitting `GoalPlanningHandoffCoordinator.kt`
  into separate files, and not relocating `ParkerRuntime`/`ParkerRuntimeOutcome`.

---

## Completion Report

- **Exact constructor dependencies:** `GoalPlanningHandoffCoordinator(planningSessionIdFactory: () -> String, planCandidateGenerator: PlanCandidateGenerator, plannerRuntime: PlannerRuntime)` -- Section 1.
- **Exact coordinator sequence:** construct `PlanningRequest` once -> `planCandidateGenerator.generate(planningRequest)` once -> `plannerRuntime.plan(planningRequest, candidates)` once, same instance and unchanged/unreordered list both times -> `GoalPlanningHandoffOutcome.Planned(result)` -- Section 1.
- **Exact outcome types:** `GoalPlanningHandoffOutcome` reduced to one variant, `Planned(planningSessionResult: PlanningSessionResult)`; `ConversationOutcome.PlanningDeferred` renamed to `Planned(outcome: GoalPlanningHandoffOutcome)`; `ParkerRuntimeOutcome.PlanningDeferred` renamed to `Planned(outcome: GoalPlanningHandoffOutcome)` -- Sections 4-6.
- **Exact mapping semantics:** `Completed`/`Rejected`/`Failed` all route to `Planned` at every layer; only a genuine uncaught exception produces `ParkerRuntimeOutcome.Failed(PipelineStage.UNKNOWN, e)` -- Section 6.
- **Exact identity registrations:** two new `private companion object` constants in `ParkerRuntime.kt` (`PLANNER_RUNTIME_PRINCIPAL_ID = PrincipalId("system.planner-runtime")`, `TASK_MANAGER_RUNTIME_PRINCIPAL_ID = PrincipalId("system.task-manager-runtime")`, matching the target classes' own existing private literals exactly), plus two new `registerActive(..., PrincipalType.SYSTEM, ...)` calls in `registerSystemIdentities` -- Section 10.
- **Exact production assembly:** `InMemoryTaskManagerRuntime(identityService, eventBus)` -> `InMemoryPlannerRuntime(identityService, eventBus, taskManagerRuntime)` -> supplied into the revised `GoalPlanningHandoffCoordinator`, all as local `val`s in `ParkerRuntime.kt`'s existing composition method, using only already-available composition-root values -- Section 8.
- **Exact permitted file list:** `src/runtime/GoalPlanningHandoffCoordinator.kt`, `src/runtime/ConversationOutcome.kt`, `src/runtime/ConversationReplyCoordinator.kt`, `src/composition/ParkerRuntime.kt`, `src/composition/ParkerRuntimeOutcome.kt` -- Section 12 (paths corrected from the task's own illustrative list).
- **Exact tests:** `tests/runtime/GoalPlanningHandoffCoordinatorTest.kt` (rewritten), `tests/runtime/ConversationReplyCoordinatorTest.kt` (updated), `tests/composition/ParkerRuntimeConversationPipelineTest.kt` (updated), covering all 21 numbered items -- Section 13.
- **Exact documentation changes:** one new `IMPLEMENTATION_HISTORY.md` entry (5 required facts) and one Gap #53 update (two named items closed, gap as a whole remains Open, execution/tool-use item explicitly still open) -- Section 14.
- **All exclusions:** Section 15, in full.
- **Blocker:** none identified. Every constructor and method signature this Unit depends on has been confirmed by direct reading, not inferred from memory; no permitted file requires a change to any of the eight excluded files to compile.

## Conclusion

**Ready for Implementation.**
