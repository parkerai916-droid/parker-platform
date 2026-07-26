# Plan Candidate to PlannerRuntime Integration — Contract Design

## Status

**Contract Design only. No Kotlin is implemented, proposed as a diff, or
changed by it, beyond illustrative signatures used to state a design
decision precisely.** Neither `src/` nor `tests/` is touched. This
document does not produce a Scope Lock and authorises no implementation —
it determines whether Scope Lock may begin, and on what terms.

**Repository position.** Branch `main`, clean. Latest verified commit
includes `PlanCandidateGenerator`, `DefaultPlanCandidateGenerator`, and
the Candidate Generation governance/design/scope-lock documents. Every
prior Governance Review, Contract Design, Scope Lock, and implementation
— including `docs/architecture/PLAN_CANDIDATE_TO_PLANNER_INTEGRATION_GOVERNANCE_REVIEW.md`
(this Unit's own immediate predecessor, approved) — is treated as settled
and is not reopened here.

**Objective, restated exactly.** Define how a real Goal planning path
changes from `PlanningRequest → Planning deferred` to `PlanningRequest →
PlanCandidateGenerator.generate(...) → PlannerRuntime.plan(...) →
planning outcome`. Orchestration and outcome propagation only — this
document must not silently absorb unrelated Task Manager or execution
work.

---

## Repository Review (fresh reads, this Unit)

Read directly, at the level cited, immediately before this document was
written -- every type name and field below is transcribed, not recalled:

- `src/contracts/PlanDecision.kt` lines 228–296 — `PlanningSessionResult`
  (sealed, three variants: `Completed(planningSessionId, taskProposalId,
  disposition, rejections)`, `Rejected(planningSessionId, taskProposalId,
  disposition: TaskProposalDisposition.Rejected, rejections)`,
  `Failed(planningSessionId, reason: String, rejections)`) and
  `PlannerRuntime` (`suspend fun plan(request: PlanningRequest,
  candidates: List<PlanCandidate>): PlanningSessionResult`), re-confirmed
  verbatim. **None of the three `PlanningSessionResult` variants carries
  `correlationId` or an initiating-Principal field.**
- `src/runtime/InMemoryPlannerRuntime.kt` lines 99–104 — constructor
  re-confirmed verbatim: `class InMemoryPlannerRuntime(private val
  identityService: IdentityService, private val eventBus: EventBus,
  private val taskProposalIntake: TaskProposalIntake, private val
  planDecision: PlanDecision = DefaultPlanDecision())`. **`taskProposalIntake`
  has no default; `identityService` and `eventBus` do not either, but both
  already exist as composition-root-scoped values in `ParkerRuntime.kt`
  today.** `plan()` calls `taskProposalIntake.submitProposal(proposal)`
  only when `PlanDecisionResult.Selected` — pre-existing, unmodified,
  already-approved behaviour.
- `src/runtime/InMemoryTaskManagerRuntime.kt` line 219 — constructor
  re-confirmed verbatim: `class InMemoryTaskManagerRuntime(private val
  identityService: IdentityService, private val eventBus: EventBus) :
  TaskProposalIntake`. **Both dependencies already exist in
  `ParkerRuntime.kt`'s composition root; no new dependency type is
  required to construct this class.** Confirmed, by grep, the sole
  existing implementation of `TaskProposalIntake`.
- `src/runtime/GoalPlanningHandoffCoordinator.kt` — re-read in full.
  Current constructor: exactly `planningSessionIdFactory: () -> String`.
  Current method: `initiatePlanning(originalMessage, goal):
  GoalPlanningHandoffOutcome`, always returns `Deferred`.
- `src/runtime/ConversationOutcome.kt` — re-read in full. Current shape:
  `sealed class ConversationOutcome { ReplyDelivered(executionResult);
  PlanningDeferred(outcome: GoalPlanningHandoffOutcome);
  NotAccepted(reason) }`.
- `src/composition/ParkerRuntimeOutcome.kt` — re-read in full. Current
  shape: `sealed class ParkerRuntimeOutcome { Delivered(executionResult);
  NotAccepted(reason); Failed(stage: PipelineStage, cause: Throwable);
  PlanningDeferred(outcome: GoalPlanningHandoffOutcome) }`. `PipelineStage`
  enum: `REASONING, UNKNOWN` — its own KDoc states plainly that every
  component without a structurally-distinguishable exception type is
  classified `UNKNOWN` "deliberately, not guessed at... reporting anything
  more specific... would be fabricating a fact this runtime cannot
  actually observe."
- `src/runtime/ConversationReplyCoordinator.kt` — re-read in full.
  Current three dependencies (`communicationConversationCoordinator`,
  `replyDeliveryCoordinator`, `goalPlanningHandoffCoordinator`); current
  `Goal` branch: `ConversationOutcome.PlanningDeferred(goalPlanningHandoffCoordinator.initiatePlanning(message,
  response))`; `Reply`/`NoAction` route to `deliverReply`, never touching
  `goalPlanningHandoffCoordinator`.
- `src/composition/ParkerRuntime.kt` lines 340–373, 509–548 — re-read in
  full. Composition root constructs `GoalPlanningHandoffCoordinator(planningSessionIdFactory
  = { UUID.randomUUID().toString() })` as its **one and only existing
  production call site**; constructs no `PlannerRuntime`,
  `InMemoryPlannerRuntime`, `TaskManagerRuntime`, or
  `InMemoryTaskManagerRuntime` anywhere. `submitOwnerMessage`'s `when`
  block re-confirmed verbatim, including its current `"Planning initiation
  deferred"` log line. `registerSystemIdentities` re-read in full: four
  `registerActive(...)` calls today (`SYSTEM_PARKER_PRINCIPAL_ID`,
  `CONVERSATION_ENGINE_PRINCIPAL_ID`, `RESPONSE_COMPOSER_PRINCIPAL_ID`,
  the owner Principal), each using the identical, already-established
  helper pattern.
- `InMemoryPlannerRuntime`'s and `InMemoryTaskManagerRuntime`'s own
  `PLANNER_RUNTIME_PRINCIPAL_ID`/`TASK_MANAGER_RUNTIME_PRINCIPAL_ID` are
  each declared inside a **`private companion object`** — not visible
  outside their own file.

---

## Primary Architectural Constraint (addressed before any signature)

**Two distinct decisions, kept explicitly separate, per this Unit's own
charter:**

1. **Integrating the abstract `PlannerRuntime` contract into the handoff
   coordinator.** This is purely a dependency on an interface type.
   `GoalPlanningHandoffCoordinator` (revised, Section 2) never references
   `InMemoryPlannerRuntime`, `TaskProposalIntake`, or
   `InMemoryTaskManagerRuntime` by name anywhere in its own source. A
   test can supply any fake `PlannerRuntime` implementation with zero
   Task Manager involvement. **This decision is fully specifiable now,
   requires no PlannerRuntime contract change, and is this Contract
   Design's actual subject matter.**
2. **Production assembly of a concrete `InMemoryPlannerRuntime`.**
   Requires a real `TaskProposalIntake` — `InMemoryTaskManagerRuntime` is
   the only one that exists. **This is a distinct decision, addressed on
   its own terms in Section 9, not blurred into decision 1.**

**The finding that changes this Unit's own staging, discovered by this
Contract Design, not by the preceding Governance Review:**
`GoalPlanningHandoffCoordinator` **already has exactly one live
production call site** (`ParkerRuntime.kt`'s composition root). Revising
its constructor to require `PlanCandidateGenerator` and `PlannerRuntime`
(both without defaults, per Section 2) means that call site **will not
compile** until it supplies real values for both. `PlanCandidateGenerator`
is trivial (`DefaultPlanCandidateGenerator()`, zero-argument). `PlannerRuntime`
is not: no default, model-independence-driven design (Section 2)
correctly forbids inventing one. **This is the one respect in which
concrete `PlannerRuntime` assembly cannot be kept perfectly separate from
this Unit** — not because Task Manager wiring is architecturally
difficult, but because Kotlin's own type system forces `ParkerRuntime.kt`'s
one existing call site to be updated in the same commit that changes the
coordinator's signature. Section 9 gives this its own full treatment and
resolves it with narrower staging, not a blanket "further governance."

---

## 1. Integration Boundary

**This Contract Design covers:**

- Generation of candidates (`PlanCandidateGenerator.generate`, already
  implemented, unmodified).
- Invocation of `PlannerRuntime.plan(request, candidates)`
  (already implemented, unmodified).
- Receipt of the resulting `PlanningSessionResult`.
- Mapping into `ConversationOutcome`.
- Propagation into `ParkerRuntimeOutcome`.

**This Contract Design does not cover:**

- Task execution.
- Tool invocation.
- Scheduling.
- Agent dispatch.
- Permission changes.
- Task lifecycle redesign.

**Task Proposal submission — stated exactly as required:**

- **It is pre-existing behaviour of `PlannerRuntime`.**
  `InMemoryPlannerRuntime.plan` already calls
  `taskProposalIntake.submitProposal(proposal)` when `PlanDecisionResult.Selected`
  — approved by `PLANNER_RUNTIME_CONTRACT_DESIGN.md`, implemented in
  Sprint 3 Track D, unmodified by anything in this document.
- **It is outside the responsibility of `GoalPlanningHandoffCoordinator`.**
  The revised coordinator (Section 2) never references
  `TaskProposalIntake`, `TaskProposal`, or `TaskManagerRuntime` by name,
  directly or transitively through its own declared dependencies. Any
  Task Proposal submission that occurs, occurs entirely inside
  `PlannerRuntime`'s own, already-approved implementation — invisible to,
  and not orchestrated by, this coordinator.

---

## 2. Coordinator Ownership

**Confirmed, not revised: `GoalPlanningHandoffCoordinator` owns the
sequence** `construct PlanningRequest → generate candidates → invoke
PlannerRuntime → return handoff result`. The Governance Review's Section
2 recommendation stands. (Section 9 revises *staging* of the production
wiring that follows from this, not *ownership* of the sequence itself.)

**Constructor dependencies — three, all evaluated:**

```kotlin
class GoalPlanningHandoffCoordinator(
    private val planningSessionIdFactory: () -> String,
    private val planCandidateGenerator: PlanCandidateGenerator,
    private val plannerRuntime: PlannerRuntime,
)
```

- `planningSessionIdFactory: () -> String` — **retained, unchanged**, no
  default, exactly as frozen by the Reasoning-to-Planning Handoff Scope
  Lock.
- `planCandidateGenerator: PlanCandidateGenerator` — **new**, no default.
  A default of `DefaultPlanCandidateGenerator()` was considered and
  rejected: `planningSessionIdFactory`'s own precedent already establishes
  "no default inside the coordinator itself... both production wiring and
  every test must supply this explicitly" — consistency favours the same
  discipline here, not a special case.
- `plannerRuntime: PlannerRuntime` — **new**, no default, for the
  identical reason, and because no honest default value could exist
  (Section 2.1 of the preceding Governance Review already rejected
  `PlannerRuntime` inventing its own candidate-generation fallback; the
  same reasoning forbids inventing a fallback `PlannerRuntime` here).

**The existing one-dependency structural guarantee is intentionally
retired — stated explicitly, not disguised.** `GoalPlanningHandoffCoordinator`'s
current KDoc claims: "the absence of any other constructor parameter is
itself the structural guarantee that this class cannot reach
`PlannerRuntime`." **That specific guarantee ends here, by design.** It is
replaced by a narrower, still-real guarantee: **exactly three constructor
dependencies, all named in this section, is the guarantee that this class
cannot reach `IdentityService`, `EventBus`, `TaskManagerRuntime`,
`TaskProposalIntake`, `ExecutionPipeline`, `PermissionEngine`,
`ToolRegistry`, `MemoryStore`, `WorldModel`, `ReasoningContext`, or
`ConversationEngine`, directly.** This is not a new pattern in this
codebase: `ConversationReplyCoordinator` underwent the identical
retirement once already, when it gained `goalPlanningHandoffCoordinator`
and its own prior "cannot reach Planning" guarantee was replaced by
"reaches Planning only through a coordinator that itself cannot invoke
`plan()` directly." Each layer's guarantee moving one level outward as the
pipeline is built out is this codebase's own established, healthy
pattern — not a regression, and not something this document pretends is
still in force where it is not.

**Precedent for a coordinator-shaped class holding a top-level executive
component directly, confirmed, not novel:** `ResponseDelivery(resourceRegistry,
executionPipeline)` already holds `ExecutionPipeline` directly.
`PlannerRuntime` is the same *kind* of dependency for this integration.

---

## 3. Public Coordinator Contract

**Method name: `initiatePlanning` — retained.** Not merely un-renamed for
convenience: this name becomes *more* accurate after this revision, not
less. Before this Unit, calling this method never actually initiated a
Planning Session — it only prepared one and reported deferral. After this
Unit, calling it genuinely does initiate a Planning Session, since it
genuinely calls `PlannerRuntime.plan()`. The name was, if anything,
mildly aspirational before; it is now literally true.

**Inputs — unchanged:**

```kotlin
suspend fun initiatePlanning(
    originalMessage: InboundOwnerMessage,
    goal: ReasoningProviderResponse.Goal,
): GoalPlanningHandoffOutcome
```

No new parameter. `PlanCandidateGenerator`/`PlannerRuntime` are
constructor-injected (Section 2), not method parameters — this method's
own public signature is unchanged in shape, only its return value's
internal meaning changes (Section 4).

**Class name: `GoalPlanningHandoffCoordinator` — retained. Not renamed.**
Evaluated against the instruction to rename only if materially
misleading, not for cosmetic reasons. "Handoff" describes the boundary
this class sits at (Reasoning → Planning), not whether it waits for a
terminal result. `ConversationOutcome.ReplyDelivered` already establishes
this codebase's own precedent for a name describing "the pipeline ran
to a real terminal value," without asserting the value was necessarily a
success — a "handoff" that confirms real receipt (by awaiting and
returning `plan()`'s own terminal result) is a coherent, ordinary reading
of the word, not a misleading one. **This is a closer call than the
method name**, and is recorded as such (Unresolved Questions) rather than
forced to a confident conclusion beyond what the evidence supports — but
this document's own recommendation is: do not rename.

---

## 4. Handoff Outcome Model

**`GoalPlanningHandoffOutcome` is revised, not replaced, and not fully
renamed.** The sealed type itself keeps its name (still accurately "the
outcome of a Goal-to-Planning handoff"); its **single existing variant is
retired and replaced**, not merely extended alongside a surviving
`Deferred`.

```kotlin
sealed class GoalPlanningHandoffOutcome {
    data class Planned(val planningSessionResult: PlanningSessionResult) : GoalPlanningHandoffOutcome()
}
```

- **`Deferred` and `PlanningDeferralReason` are removed, not retained as
  an unreachable-but-defined legacy shape.** Once this Unit is
  implemented, the coordinator calls `plannerRuntime.plan()`
  unconditionally, for every well-formed input, including an empty
  candidate list (Section 6) — there is no remaining code path under
  which this coordinator would honestly decline to attempt planning.
  Keeping `Deferred` "just in case" would be exactly the dead, misleading
  code this Unit's own charter warns against retaining "merely because
  changing it is inconvenient." No other named future reason for
  deferral exists in any settled document today (`WAITING_FOR_INPUT`
  remains unimplemented and out of this Unit's scope to invent a signal
  for).
- **A three-variant mirror of `PlanningSessionResult` (`Completed`/
  `Rejected`/`Failed`) at this layer was considered and rejected.** This
  Unit's own instruction — "do not duplicate failure information already
  owned by `PlannerRuntime`... prefer carrying existing planner outcomes
  rather than inventing parallel status models" — directly forbids it.
  `PlanningSessionResult` already fully, correctly expresses which of the
  three occurred; re-deriving that distinction one layer up would create
  two sources of truth for the same fact.
- **Exact semantics.** `Planned(planningSessionResult)` means: a real
  Planning Session ran, via `PlannerRuntime.plan()`, to one of its own
  three terminal states. It carries that `PlanningSessionResult` value
  completely unchanged — no field is read, re-labelled, or dropped by
  this coordinator. Whether the underlying attempt "succeeded" is a
  question answered by inspecting `planningSessionResult`'s own variant
  (`Completed`, `Rejected`, or `Failed`), never by this outer wrapper's
  own name.
- **`PlanningRequest` is not carried alongside.** Considered and
  rejected: `PlanningSessionResult`'s own `planningSessionId` already
  provides sufficient correlation for every documented downstream
  consumer (logging); nothing consumes the original `PlanningRequest`
  after this point, and adding it would be an unused, parallel field.

---

## 5. PlannerRuntime Result Mapping

Every `PlanningSessionResult` variant maps uniformly — the coordinator
does not branch on which variant it received; it wraps whichever one
`plan()` returns, unchanged, at every layer:

| `PlanningSessionResult` (actual, re-confirmed) | `GoalPlanningHandoffOutcome` | `ConversationOutcome` | `ParkerRuntimeOutcome` |
| --- | --- | --- | --- |
| `Completed(planningSessionId, taskProposalId, disposition, rejections)` | `Planned(planningSessionResult = Completed(...))` | `Planned(outcome)` | `Planned(outcome)` |
| `Rejected(planningSessionId, taskProposalId, disposition: TaskProposalDisposition.Rejected, rejections)` | `Planned(planningSessionResult = Rejected(...))` | `Planned(outcome)` | `Planned(outcome)` |
| `Failed(planningSessionId, reason: String, rejections)` | `Planned(planningSessionResult = Failed(...))` | `Planned(outcome)` | `Planned(outcome)` |

**`ConversationOutcome`/`ParkerRuntimeOutcome` each gain exactly one
variant, renamed from `PlanningDeferred` to `Planned`, same payload type
(`GoalPlanningHandoffOutcome`) as today — not three new variants each.**
This is the direct consequence of Section 4's "no parallel status model"
determination: uniform wrapping at every layer, inspection of the real
distinction (`Completed`/`Rejected`/`Failed`) deferred to whoever actually
needs it (today: `ParkerRuntime.submitOwnerMessage`'s own log line,
Section 11).

**Naming precedent for reusing one word across a genuine failure case,
already established in this codebase, cited directly:** `ConversationOutcome.ReplyDelivered`
already wraps an `ExecutionResult` whose own `status` may be `FAILURE` —
"Delivered" describes that the pipeline ran to a real terminal value, not
that it necessarily succeeded. `Planned` is used here for the identical
reason and by the identical precedent.

---

## 6. Empty Candidate Behaviour

**Frozen: the coordinator must call `plannerRuntime.plan(request,
emptyList())` when `planCandidateGenerator.generate(request)` returns an
empty list — never short-circuited locally.**

Nothing in `PlannerRuntime`'s actual contract makes this impossible —
`plan(request: PlanningRequest, candidates: List<PlanCandidate>)` accepts
any list, including empty, today, and `InMemoryPlannerRuntime.plan`
already, correctly, turns it into `PlanDecisionResult.NoViableCandidate`
→ `PlanningSessionResult.Failed(reason = "no Plan Candidates were
supplied for this Planning Session")`. **Ownership of this outcome
belongs to `PlannerRuntime`/`PlanDecision`, entirely, already:**
introducing a coordinator-level "no candidates" rule would (a) duplicate
logic `plan()` already owns correctly, (b) require the coordinator to
re-derive a distinction it has no business making, and (c) resurrect
exactly the "deferred without really trying" framing Section 4 retires.
No coordinator-specific "no candidates" rule is introduced.

---

## 7. Fault Propagation

- **`PlanCandidateGenerator` throwing.** Propagates uncaught through
  `initiatePlanning` — no `try`/`catch` added. `plannerRuntime.plan()` is
  never reached in this case, as an ordinary consequence of sequential
  execution (the exception aborts the method before that call), not
  because of any special-case code.
- **`PlannerRuntime` throwing.** Identical treatment — propagates
  uncaught, no `try`/`catch` added.
- **Planner returning a failed result** (`PlanningSessionResult.Failed`).
  Not an exception. A valid, ordinary return value, wrapped in
  `GoalPlanningHandoffOutcome.Planned` exactly like `Completed`/`Rejected`
  — no special-casing (Section 4, Section 5).
- **Planner returning no viable candidate.** Not a distinct case at the
  `PlanningSessionResult` level — `PlanDecisionResult.NoViableCandidate`
  is already fully absorbed into `PlanningSessionResult.Failed` inside
  `plan()` itself, indistinguishable at this coordinator's own vantage
  point from an unresolvable-identity `Failed` except by reading
  `Failed.reason`/`Failed.rejections`, which this coordinator does not
  need to, and does not, inspect.

**Both `PlanCandidateGenerator` and `PlannerRuntime` faults propagate,
uncaught, through `ConversationReplyCoordinator` (also no `try`/`catch`,
unchanged), to `ParkerRuntime.submitOwnerMessage`'s existing outer
boundary — this outer catch remains fully sufficient, no revision
required.** No new exception type, no new `try`/`catch` anywhere in the
coordinator chain, per this Unit's own explicit instruction against
adding one merely to relabel exceptions.

**Resolved: no new `PipelineStage` is introduced.** The Governance
Review left this as an open question; this Contract Design closes it.
`PipelineStage`'s own KDoc states its governing principle plainly: a
component is classified more specifically than `UNKNOWN` only when it
throws a "structurally-distinguishable exception type" (as
`TimeoutCancellationException` uniquely identifies `ModelReasoningProvider`'s
own timeout). Neither `PlanCandidateGenerator` nor `PlannerRuntime` has
any such tagged exception type — both contracts simply permit "a genuine
fault" without naming one. Introducing a `PLANNING` stage with no
structural way to distinguish it would fabricate specificity this runtime
cannot honestly claim, exactly what `PipelineStage`'s own principle
already forbids. **Both fault types are correctly classified `UNKNOWN`,
unchanged.**

---

## 8. Outcome Naming

- **`PlanningDeferred` (in both `ConversationOutcome` and
  `ParkerRuntimeOutcome`) — no longer truthful once this Unit is
  implemented; renamed to `Planned` (Section 5).** Once the coordinator
  unconditionally attempts planning, "deferred" is false for every
  reachable case.
- **`GoalPlanningHandoffOutcome.Deferred` — retired, not merely
  renamed alongside a survivor (Section 4).**
- **The corresponding `ParkerRuntimeOutcome` variant — renamed to
  `Planned`, identical reasoning, same payload type.**
- **No other variant, in any of the three types, is touched.**
  `ConversationOutcome.ReplyDelivered`/`NotAccepted` and
  `ParkerRuntimeOutcome.Delivered`/`NotAccepted`/`Failed` are unaffected —
  this rename is scoped exactly to the one affected path, per this Unit's
  own "avoid broad renaming outside the affected path" instruction.

---

## 9. Production Assembly Constraint

Answered directly, in order:

**Can `GoalPlanningHandoffCoordinator` depend only on the `PlannerRuntime`
abstraction?** **Yes.** Its own source (Section 2) declares and uses only
the `PlannerRuntime` interface type — never `InMemoryPlannerRuntime`,
never `TaskProposalIntake`, never `TaskManagerRuntime`, anywhere.

**Can tests use a fake or stub `PlannerRuntime` without Task Manager
wiring?** **Yes, trivially.** A test fake implementing `PlannerRuntime`
needs no `TaskProposalIntake` at all — that dependency belongs to
`InMemoryPlannerRuntime`'s own internal implementation, invisible to
anything programming against the interface.

**Does production assembly of `InMemoryPlannerRuntime` require
`InMemoryTaskManagerRuntime`?** **Yes**, confirmed by direct reading
(Repository Review, above): `taskProposalIntake: TaskProposalIntake` has
no default, and `InMemoryTaskManagerRuntime` is the sole implementation.

**Is that assembly change part of this eventual implementation unit, or a
later wiring unit?**

**Clean separation (zero change to `ParkerRuntime.kt`'s composition root)
is not achievable — stated plainly, not disguised.** `GoalPlanningHandoffCoordinator`
already has one live production call site. Revising its constructor
(Section 2) without a default on `plannerRuntime` means that call site
will not compile until it is updated with a real value. The preferred
design principle (staging Task Manager wiring into a separate unit)
cannot be honoured in its purest form here, for a narrow, mechanical
reason specific to this coordinator's history — not because concrete
`PlannerRuntime`/`TaskManagerRuntime` assembly is itself architecturally
difficult.

**Narrower staging, as the instruction's own fallback recommends, is
achievable, and is what this Contract Design recommends instead of a
blanket "further governance":**

- The only unavoidable inclusion is two mechanical constructions, both
  using classes that already exist, unmodified, and both using only
  dependencies (`identityService`, `eventBus`) already present in
  `ParkerRuntime.kt`'s composition root today:
  ```kotlin
  val taskManagerRuntime = InMemoryTaskManagerRuntime(identityService, eventBus)
  val plannerRuntime = InMemoryPlannerRuntime(identityService, eventBus, taskManagerRuntime)
  ```
  No new configuration, no new external dependency, no Task acceptance
  policy, no Task lifecycle behaviour is designed, altered, or decided by
  this inclusion — both classes are used exactly as already approved.
- **A disclosed friction point, not a blocker.** `PLANNER_RUNTIME_PRINCIPAL_ID`
  ("system.planner-runtime") and `TASK_MANAGER_RUNTIME_PRINCIPAL_ID`
  ("system.task-manager-runtime") are each declared inside a `private
  companion object` in their own file — `ParkerRuntime.kt` cannot
  reference them as shared constants today. Either of these classes'
  identity constant is minimally exposed (a visibility change only, not a
  behavioural redesign), or `ParkerRuntime.kt`'s own
  `registerSystemIdentities` duplicates the literal string with a comment
  citing exactly where it must match. This document does not choose
  between them — it names the choice for Scope Lock.
  `registerSystemIdentities` needs exactly two more `registerActive(...)`
  calls, using the same, already-established helper pattern as its four
  existing calls.
  Without this registration, `InMemoryPlannerRuntime.plan`'s and
  `InMemoryTaskManagerRuntime`'s own precondition checks
  (`IdentityService.resolve` on their own publisher identity) would fail
  every single Planning Session — this is not optional if real planning
  is meant to actually run.
- **This inclusion must be named explicitly in the Scope Lock's own file
  boundary and rationale, not folded silently into "modifications to
  `ParkerRuntime.kt` for the coordinator's new dependencies."** Doing so
  satisfies this Unit's own instruction against silently absorbing
  unrelated Task Manager work: the inclusion is disclosed, minimal,
  fully specified, and justified by mechanical necessity, not by scope
  creep into Task Manager policy design.
- **A genuinely positive, disclosed side effect, not a new dependency:**
  once `InMemoryTaskManagerRuntime`/`InMemoryPlannerRuntime` publish real
  `planner.*`/`task.*` events, the already-existing `RuntimeEventLogger`
  (subscribed broadly, constructed once, unchanged) surfaces them through
  already-existing observability, with zero additional wiring.

**No Task acceptance policy, Task lifecycle event choreography beyond
what `InMemoryTaskManagerRuntime` already implements, or any other
substantive Task Manager design decision is introduced by this inclusion**
— the boundary this Unit's own Objective drew ("must not silently absorb
unrelated Task Manager or execution work") is honoured by explicit
disclosure and minimal scope, not by pretending the dependency does not
exist.

---

## 10. Conversation Routing

**`ConversationReplyCoordinator`'s own dependency list, routing logic,
and structural guarantee are unchanged, except for one renamed
expression.**

- **`Reply`:** routes to `deliverReply` exactly as today.
  `goalPlanningHandoffCoordinator` is never called on this branch — this
  guarantee is structural (no reference to it exists in that branch's own
  code path) and is unchanged by this Unit.
- **`NoAction`:** identical, unchanged.
- **`Goal`:** calls `goalPlanningHandoffCoordinator.initiatePlanning(message,
  response)` exactly once, wrapping the result in `ConversationOutcome.Planned(...)`
  (renamed from `ConversationOutcome.PlanningDeferred(...)` — the only
  line in this file that changes).

**Zero-call guarantees, frozen, restated exactly:**

- `Reply` must not call the Goal planning coordinator. Unchanged,
  already true.
- `NoAction` must not call it. Unchanged, already true.
- `Goal` must call it exactly once. Unchanged, already true.

**`ConversationReplyCoordinator`'s own "cannot reach `PlannerRuntime`
directly" guarantee is not retired by this Unit — it remains fully
intact.** This class still holds no direct reference to `PlannerRuntime`
or `PlanCandidateGenerator`; it reaches Planning only transitively,
through the one coordinator whose own guarantee is what actually retires
(Section 2). This distinction matters: not every layer's guarantee
changes just because one layer's does.

**How the returned handoff outcome becomes a `ConversationOutcome`:**
`ConversationOutcome.Planned(outcome: GoalPlanningHandoffOutcome)` —
`outcome` is `initiatePlanning`'s own return value, carried unchanged, no
unwrapping, no re-inspection of its internal `PlanningSessionResult` at
this layer.

---

## 11. ParkerRuntime Propagation

- **Logging ownership: unchanged — `ParkerRuntime.submitOwnerMessage`,
  not any coordinator.** No coordinator in this codebase's existing chain
  holds a logger dependency; this Unit does not introduce one.
- **Mapping of successful planning.** `ConversationOutcome.Planned(outcome)`
  where `outcome.planningSessionResult` is `Completed` or `Rejected` →
  `ParkerRuntimeOutcome.Planned(outcome)`, unchanged payload, logged.
- **Mapping of planner failure.** `ConversationOutcome.Planned(outcome)`
  where `outcome.planningSessionResult` is `Failed` → **also**
  `ParkerRuntimeOutcome.Planned(outcome)` — **not** `ParkerRuntimeOutcome.Failed`.
  `PlanningSessionResult.Failed` is a legitimate, non-exceptional terminal
  planning outcome (no viable candidate, or an unresolvable identity),
  categorically distinct from `ParkerRuntimeOutcome.Failed`, which is
  reserved for genuine, uncaught runtime faults (Section 7). Conflating
  the two would misrepresent an honest planning result as a system fault.
- **Outer exception conversion.** Unchanged: `TimeoutCancellationException`
  → `Failed(PipelineStage.REASONING, e)`; any other `CancellationException`
  rethrown unchanged; any other `Exception` → `Failed(PipelineStage.UNKNOWN,
  e)` (Section 7 confirms no new stage is warranted).
- **Preservation of correlation and principal provenance.** **`PlanningSessionResult`
  itself carries neither `correlationId` nor an initiating-Principal
  field** (Repository Review, confirmed by direct reading of all three
  variants) — this is not a gap this Unit needs to close.
  `ParkerRuntime.submitOwnerMessage` already has `message.correlationId.value`
  in scope at the exact point it logs every branch of its own `when`
  block, today, for `NotAccepted`, `ReplyDelivered`, and the current
  `PlanningDeferred` — continuing to log using `message`'s own
  already-available field, unchanged, is sufficient. No field is added to
  `PlanningSessionResult`, `PlannerRuntime`, or any Volume-governed
  contract for this purpose (which would be "redesigning `PlannerRuntime`,"
  explicitly excluded).

**Illustrative, not frozen verbatim (unlike the Candidate Generation
Unit's rationale string, this wording is not asserted upon by any
existing test and is not fixed here):**

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

**No new logging layer is introduced** — the existing `when` block gains
one renamed branch and one reworded log line; no new logger dependency,
no new logging component.

---

## 12. Existing Contract Impact

| Contract | Change required? | Exact change |
| --- | --- | --- |
| `GoalPlanningHandoffCoordinator` | **Yes** | Constructor gains `planCandidateGenerator: PlanCandidateGenerator`, `plannerRuntime: PlannerRuntime` (Section 2); `initiatePlanning`'s body sequences `generate` then `plan` (Sections 5–7); returns `GoalPlanningHandoffOutcome.Planned` always. |
| `GoalPlanningHandoffOutcome` | **Yes** | `Deferred`/`PlanningDeferralReason` removed; `Planned(planningSessionResult: PlanningSessionResult)` added (Section 4). |
| `ConversationOutcome` | **Yes** | `PlanningDeferred` renamed to `Planned`, same payload type (Section 5, Section 8). `ReplyDelivered`/`NotAccepted` untouched. |
| `ConversationReplyCoordinator` | **Yes, minimal** | Exactly one expression changes: `ConversationOutcome.PlanningDeferred(...)` → `ConversationOutcome.Planned(...)`. Dependency list, routing structure, and every existing structural guarantee besides the one retired in Section 2 are unchanged (Section 10). |
| `ParkerRuntime` | **Yes** | Composition root: two new, disclosed constructions (`InMemoryTaskManagerRuntime`, `InMemoryPlannerRuntime`), two new `registerSystemIdentities` calls, `DefaultPlanCandidateGenerator()` supplied to the revised `GoalPlanningHandoffCoordinator` call site (Section 9). `submitOwnerMessage`: one renamed `when` branch, one reworded log line (Section 11). |
| `ParkerRuntimeOutcome` | **Yes** | `PlanningDeferred` renamed to `Planned`, same payload type (Section 5, Section 8). `Delivered`/`NotAccepted`/`Failed` untouched. |

**Confirmed: no changes required to the following five contracts.**

- **`PlanningRequest`** — already the correct, sufficient, shared input;
  unchanged construction inside the coordinator.
- **`PlanCandidate`** — unchanged; this integration consumes it exactly
  as already produced.
- **`PlanDecision`** — consumed internally by `PlannerRuntime.plan()`,
  unchanged; this integration introduces no new caller of it.
- **`PlanCandidateGenerator`** — excluded from modification by this
  Unit's own charter, and none is architecturally motivated.
- **`PlannerRuntime`** — `plan(request, candidates)`'s signature is
  already sufficient (confirmed independently by the preceding Governance
  Review's Section 3, re-confirmed here by direct reading). This Unit is
  the first real caller, not a reason to change the interface.

The repository does not prove otherwise for any of the five.

---

## 13. Test Contract (Architectural Description Only)

- **Exact call sequence.** Request creation, then candidate generation,
  then planner invocation, then outcome propagation — provable via
  ordered-invocation-recording fakes for `PlanCandidateGenerator` and
  `PlannerRuntime`.
- **Candidate list passed unchanged.** The exact `List<PlanCandidate>`
  `generate` returns is the exact list `plan` receives — same elements,
  same order, not filtered, reordered, or mutated.
- **`PlanningRequest` passed unchanged.** The same constructed instance
  is the argument to both `generate` and `plan` — no reconstruction, no
  field drift.
- **Empty list passed to `PlannerRuntime`.** A fake generator returning
  `emptyList()` still results in `plan` being called with `emptyList()`,
  not skipped (Section 6, provable structurally, not just by inspection).
- **Generator called exactly once.** Invocation-count fake.
- **Planner called exactly once.** Invocation-count fake.
- **Planner not called when generation throws.** A throwing fake
  generator; assert the fake `PlannerRuntime`'s `plan` is never invoked.
- **Exceptions propagated unchanged.** Both a throwing generator and a
  throwing planner cause the exact exception instance to propagate,
  uncaught, out of `initiatePlanning`.
- **`Reply`/`NoAction` zero-call guarantees.** Poisoned
  `planCandidateGenerator`/`plannerRuntime` (throw if invoked) exercised
  via `ConversationReplyCoordinator` with `Reply`/`NoAction` input;
  normal completion proves neither was called — mirroring the existing
  poisoned-`planningSessionIdFactory` pattern already in
  `ConversationReplyCoordinatorTest.kt`.
- **Correct `ConversationOutcome` mapping.** A `Goal` input produces
  `ConversationOutcome.Planned(outcome)` wrapping exactly what
  `goalPlanningHandoffCoordinator.initiatePlanning` returned, unmodified.
- **Correct `ParkerRuntimeOutcome` mapping.** For each of
  `Completed`/`Rejected`/`Failed`, a composition-level test (via fakes)
  confirms `ParkerRuntimeOutcome.Planned` carries the exact
  `PlanningSessionResult` value forward unchanged.
- **No task execution or tool invocation introduced by the coordinator.**
  Structural proof: `GoalPlanningHandoffCoordinator`'s declared fields are
  exactly its three constructor-injected dependencies — no `Tool`,
  `ToolRegistry`, `ExecutionPipeline`, `PermissionEngine`, or
  `TaskProposalIntake` reference exists on the coordinator itself.
  Whatever Task Proposal submission occurs happens entirely inside
  `PlannerRuntime`'s own, separately-tested implementation.

No tests are written by this document.

---

## Risks and Dependencies

- **The private-principal-ID friction (Section 9)** is the one place this
  Contract Design leaves a named choice, rather than a decision, for
  Scope Lock: expose a constant, or duplicate a literal with a citing
  comment. Either is small and reversible; neither is designed here.
- **The two-construction, two-registration inclusion in `ParkerRuntime.kt`
  (Section 9) must be named explicitly in the Scope Lock's file boundary
  and rationale** — the single greatest risk to this Unit's own integrity
  is treating that inclusion as an incidental side effect rather than a
  disclosed, bounded decision.
- **`GoalPlanningHandoffCoordinator`'s class name (Section 3)** is the one
  genuinely close call this document does not force to a confident
  conclusion beyond what the evidence supports.

---

## Explicitly Out of Scope

Restated for completeness, unchanged from this Unit's charter: modifying
candidate-generation policy or `DefaultPlanCandidateGenerator`;
redesigning `PlannerRuntime`, `PlanDecision`, or `PlanCandidate`;
designing Planning Context; reintroducing `ReasoningContext`; redesigning
Task Manager (as distinct from the minimal, disclosed, unmodified
construction in Section 9); implementing task execution; invoking tools;
adding permissions; changing authentication; modifying Chapter 23 or
`ResponseComposer`; touching voice or Home Assistant; unrelated cleanup.

---

## Unresolved Questions (for Scope Lock to settle, not blockers)

- Whether `PLANNER_RUNTIME_PRINCIPAL_ID`/`TASK_MANAGER_RUNTIME_PRINCIPAL_ID`
  are exposed as constants or duplicated as literals in `ParkerRuntime.kt`
  (Section 9).
- Whether `GoalPlanningHandoffCoordinator`'s class name is retained
  (recommended) or reconsidered (Section 3).
- Exact final wording of the composition-root log line (Section 11,
  illustrative only).

None of these blocks Scope Lock from proceeding — each is a narrow,
well-bounded choice, not a sign of unresolved architectural uncertainty
about the integration itself.

---

## Recommendation

**Ready for Scope Lock.**

Every section this Unit was chartered to complete reached a definite,
justified conclusion. The Primary Architectural Constraint — whether
concrete `PlannerRuntime` assembly can be cleanly separated from Task
Manager production wiring — was answered honestly, not disguised: clean
separation is not achievable, for a specific, narrow, mechanical reason
(`GoalPlanningHandoffCoordinator`'s existing production call site), and
this document resolves it with the narrower staging the instructions
themselves invited, not a blanket deferral of the whole integration or an
escalation to further governance. The required inclusion is fully
specified, minimal (two constructions and two identity registrations,
all using already-existing, unmodified classes and already-available
composition-root values), and explicitly bounded away from any
substantive Task Manager design decision.

Ownership, the outcome model, the exact mapping table, fault propagation,
routing, and Parker-runtime-level propagation are each settled with no
remaining architectural fork. The three Unresolved Questions above are
narrow implementation choices, not open architecture, and do not block a
Scope Lock scoped to: `GoalPlanningHandoffCoordinator`'s revised
constructor and body; `GoalPlanningHandoffOutcome`'s revised shape; the
renamed `ConversationOutcome`/`ParkerRuntimeOutcome` variant; the minimal,
named `ConversationReplyCoordinator` and `ParkerRuntime.kt` changes
(including the disclosed `InMemoryTaskManagerRuntime`/`InMemoryPlannerRuntime`
construction and identity registration). It must not expand into Task
acceptance policy, Task lifecycle redesign, execution, tool invocation,
permissions, or any topic named in Explicitly Out of Scope.
