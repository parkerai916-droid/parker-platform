# Multi-Step Agent Run Model — Design

## Status

Sprint: Sprint 3, Track C, Unit C1 (Design)
Version: 0.1-draft
Status: **Design proposal, not yet reviewed or accepted.**

**This document is specification-level design only.** No Kotlin is
implemented, proposed as a diff, or changed by it. Neither `src/` nor
`tests/` is touched. No existing architecture document is modified. No
existing test is modified. This document adds no entry to
`IMPLEMENTATION_HISTORY.md` or `IMPLEMENTATION_GAPS.md` — per this unit's
own instructions, those remain untouched until an implementation unit
(Unit C2) actually changes `src/`/`tests/`. Nothing described here is
authorised for implementation until this document is reviewed and
accepted, per PES-001 (`docs/architecture/PARKER_ENGINEERING_STANDARD.md`)
Chapter 1, Stage 1 (Architecture) preceding Stage 2 (Specification)
preceding any coding stage, and per AD-014 (Architecture Before
Implementation).

This document is written as a standalone design document under
`docs/architecture/`, per `docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md`'s
own Unit C1 "Files expected to change" field, which names this as one of
two acceptable forms (the other being a new section inside
`AgentRuntimeSpecification.md` itself). A standalone document was chosen
because the content below is substantial enough to warrant its own file,
mirroring the precedent already set by `docs/architecture/tool-registry.md`,
`docs/architecture/action-mapping.md`, and `docs/architecture/IdentityService.md`
— each an architecture-level design proposal that preceded, and was later
partially backfilled into, a Volume 3 specification once implemented
(gap #21's `ToolRegistry.md` backfill is the exact precedent). This
document does not amend `AgentRuntimeSpecification.md` in any way; it is
additive, and every design choice below is checked against that
specification's existing text rather than silently overriding it.

## Review

Reviewed, in the authority order this unit's own instructions specify:

1. `docs/architecture/parker-constitution.md` (Parker Constitution)
2. `docs/architecture/ARCHITECTURE_DECISIONS.md` (Architecture Decisions, AD-001–AD-016)
3. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001)
4. `docs/architecture/PROJECT_GOVERNANCE.md` (Project Governance) — this
   file is currently empty (a pre-existing condition, flagged by the
   Sprint 2 Health Review and its Post-Implementation Review; not
   something this design unit resolves, since authoring governance
   content is a human decision, not a design-unit task). No governance
   rule beyond PES-001 and the Constitution could be consulted from it.
5. `docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md`, Track C, Unit C1
   (the objective, scope, and acceptance criteria this document must
   satisfy)
6. `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
   (Sections 4, 5, 6, 7, 8, 9, 10, 11, 12 — Agent lifecycle, execution
   model, identity, context, events, failure, safety boundaries, future
   systems)
7. `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
   (Sections 6, 7 — Task/Agent Run relationship, unaffected by this
   design)
8. `src/contracts/AgentRunCommand.kt`, `src/contracts/AgentRunLifecycle.kt`,
   `src/contracts/AgentRun.kt` (existing, already-committed contracts this
   design builds on without modification)
9. `src/runtime/InMemoryAgentRuntime.kt`, `src/runtime/DefaultExecutionPipeline.kt`
   (the current Sprint 1 Unit 7 implementation and the Execution Pipeline
   it calls, to ground every design choice in what actually exists today,
   not an assumed or idealised runtime)

No architecture was invented where an existing document already answers
the question; every such case is cited by section number below instead
of being restated as a new decision.

---

## 1. Purpose

Sprint 1, Unit 7 (`src/runtime/InMemoryAgentRuntime.kt`) implements
exactly one Agent Step per Agent Run: `START` constructs one
`ExecutionRequest`, submits it once, and the Agent Run ends at
`COMPLETED` or `FAILED` based on that single result. `SUSPEND`, `RESUME`,
and `CANCEL` are accepted as named `AgentRunCommandType` values but are
explicitly rejected by this runtime as unimplemented
(`InMemoryAgentRuntime.submit`'s own `when` branch).

`AgentRuntimeSpecification.md` §5 already specifies a ten-state Agent Run
lifecycle that assumes more than this: `WAITING_FOR_PERMISSION`,
`WAITING_FOR_INPUT`, and `SUSPENDED` are named, reachable states with
specified entry and exit edges, and `AgentRunCommand.kt` already names
`SUSPEND`/`RESUME`/`CANCEL` as commands an external caller (the Task
Manager Runtime) may issue. None of this is implemented today.

This document's purpose is to specify, precisely enough that Unit C2 can
implement it without inventing architecture mid-coding:

- how an Agent Run takes more than one Agent Step in sequence;
- what state is carried between Agent Steps;
- how `SUSPEND` and `RESUME` pause and continue an in-progress Agent Run;
- how `CANCEL` terminates one, including the one already-in-flight step
  case the current synchronous Execution Pipeline makes unavoidable; and
- exactly which parts of this remain deliberately out of Unit C2's scope
  (Section 11), so C2 is not silently expected to also build a Planner.

This document does **not** decide what any given Agent Instance's Goal
actually is, how many steps it truly needs, or what makes one proposed
action better than another — those are Planner (Chapter 20) concerns,
explicitly out of scope by `AgentRuntimeSpecification.md` §3 and this
document's own Section 3.

## 2. Architectural Context

Per the Constitution's central chain — cognition proposes, trust
authorises, runtime executes — nothing in this design changes which role
any component plays:

- **Cognition proposes.** Whatever decides an Agent Run needs another
  step, and what that step's proposed action should be, remains upstream
  of this document (Section 4's `AgentStepSource` seam), exactly as
  `AgentRuntimeSpecification.md` §2 already requires ("Model
  independence... the Agent Runtime would behave identically regardless
  of what — or whether a model — sits upstream").
- **Trust authorises.** Every Agent Step's proposed action still passes
  through exactly one `PermissionEngine.evaluate` call, reached only via
  `ExecutionPipeline.submit` (AD-007, AD-003). Multi-step execution means
  this happens once per step, not once per Agent Run — it does not
  introduce a second authorisation path, a cached decision reused across
  steps, or any Agent-Runtime-local permission logic.
- **Runtime executes.** The Execution Pipeline remains the only component
  that ever calls `Tool.execute` (AD-003). The Agent Runtime's new
  responsibility is orchestrating *when* it calls `ExecutionPipeline.submit`
  again, and *whether* to pause or stop between calls — never executing
  anything itself.
- **The owner remains in control.** `SUSPEND` and `CANCEL` are commands
  issued by an authenticated Principal via `AgentRunCommandChannel.submit`
  (already specified, `AgentRunCommand.kt`) — this design adds no
  self-suspend or self-cancel operation an Agent Instance could invoke on
  its own Agent Run; only an external, attributable command can pause or
  stop it, mirroring `TaskManagerRuntimeSpecification.md` §8's identical
  "every Task operation is performed by an authenticated Principal"
  requirement for its own cancellation cascade.
- **Modules provide capability, Parker owns authority.** The `AgentStepSource`
  seam this document introduces (Section 4) is a capability provider — it
  decides *what* to propose next — but it is never consulted for, and
  never returns, a permission decision. Its output is always still routed
  through `ExecutionPipeline.submit`/`PermissionEngine.evaluate` before it
  can have any effect. A misbehaving or malicious `AgentStepSource`
  implementation can waste Agent Steps (bounded by Agent Policy, Section
  4) or propose actions that get denied — it cannot itself grant
  authority, exactly as Agent Capability and Agent Policy already cannot
  (`AgentRuntimeSpecification.md` §7, "Agents cannot expand their own
  authority").

No constitutional principle is loosened, bypassed, or reinterpreted by
anything in this document.

## 3. Responsibilities

**In scope for this design (and, per Section 11, for Unit C2):**

- Driving an Agent Run through more than one Agent Step, in sequence,
  within the existing ten-state lifecycle (`AgentRunLifecycleTransitions`,
  unmodified — no new state, no new edge).
- Carrying the minimum state (Section 6) needed for a second and later
  Agent Step to be proposed, without duplicating Task Context, Memory, or
  World Model (AD-011, ADR-002).
- Implementing `SUSPEND`, `RESUME`, and `CANCEL` against the already-legal
  transitions those commands already target (`AgentRunLifecycleTransitions`
  already permits `RUNNING -> SUSPENDED`, `SUSPENDED -> RUNNING`, and
  `-> CANCELLED` from every non-terminal state).
- Bounding an Agent Run by Agent Policy's `maxAgentSteps` (Section 4),
  already named as a concept by `AgentRuntimeSpecification.md` §4
  ("Agent Policy... maximum Agent Steps per Agent Run") but never given a
  Kotlin shape.
- Publishing the full `agent.*` event set `AgentRuntimeSpecification.md`
  §9 already specifies but Unit 7 never had cause to publish (`agent.step_started`,
  `agent.action_proposed`, `agent.permission_required`,
  `agent.action_approved`, `agent.action_denied`, `agent.action_deferred`,
  `agent.suspended`, `agent.resumed`, `agent.cancelled`), since a
  single-step Agent Run never previously reached most of them.

**Explicitly not this design's responsibility (unchanged, restated for
clarity, not newly decided here):**

- Deciding what an Agent Instance's Goal is, or what its next proposed
  action should be — Planner territory (`AgentRuntimeSpecification.md`
  §3, §12; this document's Section 4).
- Any Task Manager Task state change. The Agent Run lifecycle remains
  independently tracked from the Task lifecycle (`AgentRuntimeSpecification.md`
  §5, "Relationship to the Task Manager Task Lifecycle"; AD-006). Track
  B's already-shipped `InMemoryTaskManagerRuntime` behaviour (Units B1/B2)
  requires no change for this design to be implemented — it already
  reacts only to `agent.completed`/`agent.failed`, both of which continue
  to fire exactly once per Agent Run, at the same two points in the
  lifecycle, regardless of how many Agent Steps preceded them.
- Real input-supply infrastructure for `WAITING_FOR_INPUT` (Section 6's
  own subsection; Section 11).
- Any permission policy content, identity resolution logic, or Tool
  implementation — all unchanged, all exactly as Track A and Sprint 1
  already built them.

**Ownership.** The Agent Runtime remains the sole owner of Agent Run
records and Agent Context, exactly as it is today — this design adds no
second owner and no shared-write path. Symmetrically to AD-004 ("Task
Manager Owns Canonical Tasks"), no other component (Task Manager,
Execution Pipeline, EventBus) ever writes an `AgentRun`'s `status` or
Agent Context directly; every mutation happens inside
`InMemoryAgentRuntime` (or its successor) exclusively.

## 4. Public Interfaces

Presented as specification-level signatures (matching the convention
`AgentRuntimeSpecification.md` and `TaskManagerRuntimeSpecification.md`
already use throughout their own prose — e.g. `AgentRunCommandChannel.submit`
is shown as a signature in spec text without that constituting Kotlin
implementation). No file under `src/` is created or changed by this
document; Unit C2 is responsible for turning the shapes below into real
Kotlin.

### 4.1 `AgentRunCommandChannel` (existing, unchanged)

```
interface AgentRunCommandChannel {
    suspend fun submit(command: AgentRunCommand): AgentRunCommandResult
}
```

No change to this interface, `AgentRunCommand`, `AgentRunCommandType`, or
`AgentRunCommandResult` (`src/contracts/AgentRunCommand.kt`). `SUSPEND`,
`RESUME`, and `CANCEL` are already fully specified there, including
`CANCEL`'s required `cancellationReason` — this design implements what
that file already named, and adds nothing to it.

### 4.2 `AgentStepSource` (new, proposed)

The seam by which "what should this Agent Run's next step be" is
decided, kept strictly separate from "should that step be allowed to have
an effect" (the Permission Engine's job, unchanged). This is the seam
AD-010 (Model Independence) requires: nothing in this design, or in
Unit C2's implementation of it, may assume a specific reasoning approach
sits behind this interface.

```
interface AgentStepSource {
    suspend fun nextStep(context: AgentStepContext): AgentStepDecision
}

sealed class AgentStepDecision {
    data class Propose(
        val proposedAction: String,
        val targetResources: List<ResourceId> = emptyList(),
    ) : AgentStepDecision()

    object Complete : AgentStepDecision()

    data class Fail(val reason: String) : AgentStepDecision()
}
```

**Forward compatibility note.** `AgentStepDecision` has exactly three
variants today (`Propose`, `Complete`, `Fail`) — deliberately no
`Suspend`/"pause pending external input" variant, since this design does
not reach `WAITING_FOR_INPUT` (Section 5.2, Section 6.6, Section 11).
Future versions of this seam may introduce additional `AgentStepDecision`
variants — for example, a variant corresponding to "stop here until
external information exists," once a real input-supply channel exists to
resume from — without affecting the runtime architecture this document
specifies: the multi-step loop (Section 6.1), the locking model (Section
8), and the lifecycle transitions (Section 5) are all written against
"however many `AgentStepDecision` variants exist today," not against the
assumption that exactly three ever will. Adding a variant later is an
additive change to a sealed type and its one call site (Section 5.1, step
2), not a redesign of anything above it. This is recorded so a future
unit does not have to rediscover that the seam was built to be extended.

`AgentStepSource` is a **decision provider, not an authority** — mirroring
exactly how `DefaultPermissionPolicy` is an injected, data-shaped decision
provider for the Permission Engine (Sprint 2, Unit A2) and how
`ActionMapper` is an injected vocabulary lookup for the Execution
Pipeline. `AgentStepDecision.Propose` never itself causes an effect; it
only causes the Agent Runtime to construct and submit one more
`ExecutionRequest`, which is then independently evaluated exactly as
every other request is (Section 6).

`AgentStepContext` (below) is the read-only view `AgentStepSource` is
given each time it is consulted:

```
data class AgentStepContext(
    val agentRunId: AgentRunId,
    val taskId: TaskId,
    val goal: String,
    val stepNumber: Int,                       // 1-indexed; 1 on the first call
    val priorResult: ExecutionResult?,          // null on the first call
    val resourceReferences: List<ResourceId>,   // accumulated, Section 6
    val deniedActions: List<String>,            // accumulated, Section 6
)
```

Every field above already exists as a named concept in
`AgentRuntimeSpecification.md` §8 (Agent Context Model) — `stepNumber`
maps to "Agent Step state," `resourceReferences` maps to "Resource
references," `deniedActions` maps to "Permission scope" (the read-only
cache of past decisions §8 already describes), and `priorResult` is the
natural reference an adaptive step-2-or-later decision needs to consult.
No field here is invented beyond what §8 already lists; this interface
only gives that existing list a concrete shape.

**This document does not specify what implements `AgentStepSource`.**
Per Section 11, Unit C2 supplies a fixed, deterministic, non-Planner
stand-in — mirroring Sprint 1's own `DeterministicPlannerHarness`
precedent exactly — not a real Planner. A future Planner (Chapter 20)
implementing this same interface is the seam this design reserves,
without specifying it.

### 4.3 `AgentPolicy` (new, proposed)

Already named as a concept, unshaped, by `AgentRuntimeSpecification.md`
§4 ("the bounded configuration governing an Agent Instance's
operation — for example, maximum Agent Steps per Agent Run, maximum
Agent Run duration, or which Agent Capabilities are in scope"). This
design gives it the minimal shape Unit C2 needs and no more:

```
data class AgentPolicy(
    val maxAgentSteps: Int,
    val maxAgentRunDuration: Duration? = null,
)
```

`maxAgentRunDuration` is included for shape-completeness with §4's own
text but Section 11 states plainly that Unit C2 is not required to
enforce it (Section 7). Agent Capability scoping is out of this design's
scope entirely — no `AgentCapability` Kotlin type is proposed here, since
no Unit C2 acceptance criterion depends on it existing yet.

### 4.4 No change to `ExecutionPipeline`, `PermissionEngine`, `ToolRegistry`, `EventBus`, `IdentityService`

Every existing interface this design's runtime calls is used exactly as
specified today: `ExecutionPipeline.submit`, `ExecutionPipeline.cancel`
(newly *used* by this design, per Section 7, but not newly *changed*),
`EventBus.subscribe`/`publish`, `IdentityService.resolve`. No signature
change is proposed to any of them.

## 5. State Model

**No new `AgentRunStatus` value. No new transition edge.**
`AgentRunLifecycleTransitions` (`src/contracts/AgentRunLifecycle.kt`)
already permits every transition this design needs:

```
RUNNING -> WAITING_FOR_PERMISSION -> {RUNNING, SUSPENDED, FAILED}
RUNNING -> WAITING_FOR_INPUT -> {RUNNING, SUSPENDED}
RUNNING -> SUSPENDED -> {RUNNING, FAILED, CANCELLED}
RUNNING -> {COMPLETED, FAILED, CANCELLED}
```

This design's job is entirely to specify *when* each already-legal edge
fires across more than one Agent Step — not to add to the diagram
`AgentRuntimeSpecification.md` §5 already fixes.

### 5.1 Per-step transition sequence

For Agent Step *N* (`N` starting at 1):

1. `agent.step_started` is published (the Agent Run is already `RUNNING`
   — this is an informational event, not itself a transition, matching
   §9's own "Published when: A new Agent Step begins" wording).
2. `AgentStepSource.nextStep(context)` is consulted (Section 4). This is
   an internal call, not itself observable as an Agent Event.
3. On `AgentStepDecision.Propose`:
   a. `agent.action_proposed` is published.
   b. `RUNNING -> WAITING_FOR_PERMISSION` (a real, if momentary, lifecycle
      transition — see "Momentary states," below).
   c. `agent.permission_required` is published.
   d. `ExecutionPipeline.submit` is called once for the constructed
      `ExecutionRequest` (Section 6).
   e. Based on `ExecutionResult.status` (Section 7 covers every branch):
      - `SUCCESS` / `PARTIAL_SUCCESS`: `agent.action_approved`, then
        `agent.step_completed`, then `WAITING_FOR_PERMISSION -> RUNNING`.
        Step *N* is done; the loop returns to step 1 for step *N+1*,
        unless the step budget (Section 4's `AgentPolicy.maxAgentSteps`)
        is now exhausted, in which case `RUNNING -> SUSPENDED` instead
        (Section 7, "Step budget exhaustion").
      - `DENIED`: `agent.action_denied`, then
        `WAITING_FOR_PERMISSION -> FAILED`. The Agent Run ends here
        (Section 7 explains why this design keeps Unit 7's existing
        choice rather than silently changing it).
      - `DEFERRED`: `agent.action_deferred`, then
        `WAITING_FOR_PERMISSION -> SUSPENDED`. This is a real, waiting
        -for-a-`RESUME` pause, not a momentary state (Section 7).
      - `FAILED` / `EXPIRED` / `CANCELLED` (Tool/pipeline-level, not a
        permission outcome): `agent.step_completed` (recording the
        failure), then `WAITING_FOR_PERMISSION -> FAILED`. Same "ends the
        Agent Run" choice as `DENIED`, for the same reason (Section 7).
4. On `AgentStepDecision.Complete`: `agent.completed`,
   `RUNNING -> COMPLETED`. Rejected (treated as `AgentStepDecision.Fail`
   with a runtime-supplied reason) if no Agent Step has yet completed
   successfully for this Agent Run — `AgentRuntimeSpecification.md` §4
   states an Agent Run "consists of one or more Agent Steps"; an
   `AgentStepSource` that claims completion after zero steps is
   misbehaving, not legitimately done, and this design does not silently
   accept that claim.
5. On `AgentStepDecision.Fail`: `agent.step_completed` is not published
   (no Execution Pipeline call was made), but the Agent Run still ends
   observably — `agent.failed` is published directly, then
   `RUNNING -> FAILED`.

### 5.2 Momentary states

`WAITING_FOR_PERMISSION` is entered and exited within the same
synchronous call chain for the `APPROVED`/`APPROVED_WITH_CONFIRMATION`
and `DENIED` outcomes — `ExecutionPipeline.submit` (and, inside it,
`PermissionEngine.evaluate`) resolves synchronously today
(`DefaultExecutionPipeline.submit`'s own implementation), so there is
nothing to genuinely wait on for those three outcomes. This mirrors
Unit 7's own KDoc reasoning ("since `ExecutionPipeline.submit`... is
synchronous... there is nothing to pause for") but corrects its scope:
Unit 7 used that reasoning to justify *never* entering
`WAITING_FOR_PERMISSION` at all, collapsing `DENIED` and `DEFERRED` into
an identical `FAILED` outcome. This design keeps the state, entered and
exited immediately, because (a) it is what §5 specifies, (b) the two
Agent Events either side of it (`agent.permission_required` and one of
`agent.action_approved`/`agent.action_denied`/`agent.action_deferred`)
are real, required audit points (§9, AD-009) that Unit 7 never had
occasion to publish, and (c) exactly one of the three outcomes —
`DEFERRED` — does **not** immediately resolve back to `RUNNING`; it
becomes a genuine, indefinite pause (`SUSPENDED`) requiring an external
`RESUME` command. Distinguishing `DENIED` from `DEFERRED` (which Unit 7
does not do today) is required precisely so that only `DEFERRED` reaches
this real pause — collapsing them, as Unit 7 currently does, would make a
`DENIED` outcome un-resumable in exactly the same way a `DEFERRED` one
correctly is, which is wrong.

`WAITING_FOR_INPUT` is not entered by anything in this design's own
scope (Section 11) — `AgentStepSource` has no `RequestInput` decision
variant in Section 4's shape, deliberately, since no input-supply channel
exists to resume from yet (Section 6, "WAITING_FOR_INPUT," and Section
11). The state, its already-specified Trust Boundary
(`AgentRuntimeSpecification.md` §5), and its transitions remain exactly
as specified; this design does not add a way to reach it, and does not
remove or alter it either.

## 6. Runtime Behaviour

### 6.1 Multi-step loop

`InMemoryAgentRuntime`'s successor drives:

```
CREATED -> INITIALISED -> READY -> RUNNING
loop while budget remains and no terminal decision reached:
    Section 5.1's per-step sequence
RUNNING -> {COMPLETED, FAILED, SUSPENDED}
```

`CREATED -> INITIALISED -> READY -> RUNNING` is unchanged from Unit 7 —
identity resolution and Agent Capability/Policy binding still happen
once, before the first Agent Step, exactly as today.

### 6.2 State carried between steps

Per Section 4.2, `AgentStepContext` is reconstructed before each
`AgentStepSource.nextStep` call from state the Agent Runtime already
owns:

- `stepNumber` — an incrementing counter, reset to 1 at `RUNNING`'s first
  entry for this Agent Run (not reset on `RESUME` — resuming continues
  the same Agent Run, per §4's "Agent Run," so step numbering continues
  rather than restarting).
- `priorResult` — the most recent `ExecutionResult` this Agent Run
  received, or `null` before the first step.
- `resourceReferences` — every `ResourceId` from every step's
  `AgentStepDecision.Propose.targetResources` and every
  `ExecutionResult.affectedResources` so far, accumulated (§8, "Resource
  references": "for continuity across Agent Steps").
- `deniedActions` — every `proposedAction` string for which the
  corresponding step reached `DENIED`, accumulated (§8, "Permission
  scope": "useful... to avoid re-proposing already-denied actions" — this
  design surfaces exactly that cache, nothing more).

This is held as ordinary in-memory state associated with the `AgentRun`
record (mirroring how `agentEvents`/`agentRunCommands` are held alongside
`tasks` in `InMemoryTaskManagerRuntime`), not a new persisted store, and
not Memory or the World Model (Section 9, "Interaction with Memory" /
"Interaction with World Model"). It does not survive process restart —
no requirement anywhere in `AgentRuntimeSpecification.md` §8 or this
design asks it to.

### 6.3 `SUSPEND`

An `AgentRunCommand` with `commandType SUSPEND` targets a specific,
existing `agentRunId` (already enforced by `AgentRunCommand`'s own
`init` block). This design specifies:

- **`SUSPEND` takes effect at the next step boundary, never mid-step.**
  `Tool.execute()` is a single, non-cooperatively-interruptible suspend
  call in this codebase (`Tool.md`'s existing contract has no cancellation
  hook `DefaultExecutionPipeline` calls into). A `SUSPEND` received while
  step *N*'s `ExecutionPipeline.submit` call is in flight is recorded
  (Section 8's concurrency model specifies how) and applied once step
  *N*'s result is known and step 5.1's per-step sequence would otherwise
  begin step *N+1* — at that point, instead of starting step *N+1*, the
  Agent Run transitions `RUNNING -> SUSPENDED` and `agent.suspended` is
  published.
- **A `SUSPEND` received while the Agent Run is not `RUNNING`** (e.g.
  already `SUSPENDED`, or in a terminal state) is rejected — `AgentRunCommandResult.Rejected`
  with an explicit reason — mirroring `InMemoryTaskManagerRuntime.submitProposal`'s
  existing "resubmission is caller misuse, not a disposition" precedent.
  `AgentRunLifecycleTransitions.isValidTransition` is the mechanical check
  (`SUSPENDED -> SUSPENDED` is not a listed edge).

### 6.4 `RESUME`

- **`RESUME` transitions `SUSPENDED -> RUNNING`** and the multi-step loop
  (Section 6.1) continues from wherever it left off — `stepNumber`
  continues incrementing, `AgentStepSource.nextStep` is consulted again
  with the same accumulated `AgentStepContext`, per §5's own "resume
  re-enters `RUNNING` and re-derives whether a wait state is immediately
  needed again."
- **A `RESUME` received while the Agent Run is not `SUSPENDED`** is
  rejected, same mechanism as Section 6.3's rejection case.

### 6.5 `CANCEL`

- **`CANCEL` is accepted from any non-terminal state**, matching §5's
  "reachable from every non-terminal state" exactly, and
  `AgentRunLifecycleTransitions`'s existing edges (every non-terminal
  state already permits `-> CANCELLED`).
- **The Agent Run's own state moves to `CANCELLED` immediately** — not
  deferred to the next step boundary the way `SUSPEND` is (Section 6.3).
  This asymmetry is deliberate: `SUSPEND`'s deferral exists so the Agent
  Run's *observable* state (`SUSPENDED`) is only ever reported once a
  step has genuinely, cleanly finished — the Agent Run remains coherent
  to resume from. `CANCEL` has no resume path to keep coherent (`CANCELLED`
  is terminal), so recording it immediately, rather than delaying it
  behind whatever step happens to be in flight, is both simpler and more
  responsive to the cancelling Principal's request — mirroring
  `TaskManagerRuntimeSpecification.md` §5's identical "a Task actively
  executing must be Cancelled to stop it, not silently replaced" urgency
  for its own cancellation semantics.
- **A step already in flight when `CANCEL` is recorded is not forcibly
  interrupted** — this design does not claim an ability this codebase
  does not have (`Tool.execute()` cannot be pre-empted). Instead,
  `ExecutionPipeline.cancel(requestId)` (already specified,
  `ExecutionPipeline.md`; already implemented,
  `DefaultExecutionPipeline.cancel`) is called for the in-flight
  `ExecutionRequest`, on a best-effort basis: if the underlying
  `ExecutionLifecycleState` has not yet reached a terminal state,
  cancellation is recorded there too; if the Tool has already progressed
  past a cancellable point, `DefaultExecutionPipeline.cancel` already
  returns `CancellationResult(cancelled = false, ...)` rather than
  fabricating success (`DefaultExecutionPipeline.cancel`'s existing,
  unmodified behaviour) — this design surfaces that existing honest
  result, it does not paper over it. Whatever `ExecutionResult` the
  in-flight step eventually produces is still recorded (Section 9,
  auditability) but has no further effect on the Agent Run, which is
  already `CANCELLED`.
- **A `CANCEL` received while the Agent Run is already terminal** is
  rejected, same mechanism as Sections 6.3/6.4.

### 6.6 `WAITING_FOR_INPUT`

Not reachable through anything this design specifies (Section 5.2,
Section 11). `AgentRuntimeSpecification.md` §5's "WAITING_FOR_INPUT Trust
Boundary" remains exactly as written and is not weakened, narrowed, or
contradicted by this document — it simply is not exercised by Unit C2's
scope.

## 7. Error Handling

- **`DENIED` ends the Agent Run.** `AgentRuntimeSpecification.md` §5
  states explicitly that whether a `DENIED` outcome ends the whole Agent
  Run or allows continuation via an alternate proposed action "is an
  Agent Policy/Planner-level decision this document does not mandate."
  Unit 7's existing implementation already made this choice once (a
  non-`SUCCESS` result always ends the Agent Run at `FAILED`). This
  design keeps that same choice for `DENIED` explicitly, rather than
  silently reopening it, because reversing it would require the
  `AgentStepSource` seam (Section 4) to be given the denied step's own
  outcome and asked whether to try again — which is exactly the kind of
  Planner-level retry logic both this document and
  `AgentRuntimeSpecification.md` §10 ("Tool failure") name as explicitly
  out of scope. A future Planner-backed `AgentStepSource` MAY choose to
  propose a different action after seeing a step recorded in
  `deniedActions` (Section 4.2) — but only for the *next* Agent Run's
  step 1, or, if this design's loop structure is revisited later, for a
  future version of this Agent Run; it does not happen automatically
  within this design's own step loop.
- **`DEFERRED` suspends, does not fail.** Distinct from `DENIED`
  (Section 5.2) — this is the one outcome this design must not collapse
  into `FAILED`, since §5 specifies `WAITING_FOR_PERMISSION -> SUSPENDED`
  as `DEFERRED`'s own edge, not `-> FAILED`.
- **Tool/pipeline failure (`FAILED`, `EXPIRED`, `CANCELLED` at the
  `ExecutionResult` level) ends the Agent Run**, same treatment as
  `DENIED` and for the same reason — continuing via a different proposed
  action is retry logic this design does not implement (§10, "Tool
  failure").
- **Step budget exhaustion suspends, does not fail.**
  `AgentRuntimeSpecification.md` §10 ("Timeout") already specifies this
  exactly: "An Agent Run exceeding an Agent Policy-defined maximum...
  Agent Step count... MUST transition to `SUSPENDED` (recoverable) rather
  than `FAILED`, since exceeding a configured bound is not itself
  evidence of an unrecoverable error." This design does not reinterpret
  that — reaching `AgentPolicy.maxAgentSteps` after a successful step
  transitions `RUNNING -> SUSPENDED`, and a subsequent `RESUME` (Section
  6.4) is rejected by policy re-check rather than immediately re-running
  past the same bound (the exact re-check mechanism — e.g., requiring a
  Task-Manager-supplied policy override to `RESUME` past a previously-hit
  bound — is left to Unit C2's own implementation choice, not decided
  here, since no acceptance criterion depends on it).
- **A misbehaving `AgentStepSource`** (Section 5.1, item 4 — claiming
  `Complete` after zero successful steps) is treated as `Fail`, not
  silently honoured, and not thrown as an exception — matching this
  repository's established "resolve-or-reject, never throw for a data
  condition" convention (`InMemoryTaskManagerRuntime.submitProposal`,
  `InMemoryIdentityService.register`).
- **Identity revocation** is unchanged from `AgentRuntimeSpecification.md`
  §7/§10: this design introduces no new detection mechanism and inherits
  the same currently-open dependency on Identity Service gaps #37/#39.
  Multi-step execution means a revocation check (wherever Unit C2 chooses
  to place it, per §7's own undecided "poll `resolve()` before each Agent
  Step, subscribe to `identity.*` events, or both") now has more natural
  checkpoints — before each Agent Step is a defensible choice, but this
  design does not mandate it, since §7 itself leaves the exact mechanism
  undecided.

## 8. Concurrency Model

**Per-Agent-Run sequential execution; cross-Agent-Run concurrency
permitted.** An Agent Run's own Agent Steps are strictly sequential —
there is no concurrent-step concept anywhere in `AgentRuntimeSpecification.md`
§4 ("An Agent Run consists of one or more Agent Steps," singular
progression). Different Agent Runs, however, have no ordering
relationship to each other and may be processed concurrently.

**This design recommends changing `InMemoryAgentRuntime`'s existing
locking granularity, and states this explicitly as a genuine design
decision this document settles, not one left to Unit C2.** Today,
`InMemoryAgentRuntime.submit` holds its single `Mutex` for the entire
duration of one Agent Step, including the suspend call to
`ExecutionPipeline.submit` (`mutex.withLock { ... start(command) ... }`,
where `start` itself calls `executionPipeline.submit`). This was
harmless for a single, short-lived step; it is not acceptable once one
Agent Run may take many steps, potentially against slow, real Tools,
because:

- it would serialise **all** Agent Runtime activity — a second Agent
  Run's `START` cannot even begin until the first Agent Run's current
  step finishes; and
- it would make a `SUSPEND`/`CANCEL` command aimed at Run A wait behind
  Run B's in-flight step, even though A and B are unrelated — directly
  undermining Section 6.3/6.5's "takes effect at the next step boundary"
  design, which assumes a command can at least be *recorded* promptly.

**Specified locking model for Unit C2:**

- One `Mutex` guards only the shared `agentRuns: MutableMap<AgentRunId, AgentRun>`
  (and any per-run accumulated state from Section 6.2), for the duration
  of each individual read-or-write, exactly as `InMemoryTaskManagerRuntime.agentEventsFor`/
  `recordAgentEvent` already lock only around their own map access, not
  around an entire multi-step operation.
- The suspend call to `ExecutionPipeline.submit` (Section 5.1, step 3d)
  happens **outside** any held mutex.
- A pending `SUSPEND`/`CANCEL` request for a given `agentRunId` is
  recorded as a short, mutex-guarded write (e.g. a pending-command flag
  or queue entry associated with that Agent Run) that the per-step loop
  checks, under the same short lock, immediately after each step's
  `ExecutionPipeline.submit` call returns and before starting the next
  step or reporting the current one's outcome.
- Two commands concurrently targeting the *same* `agentRunId` (e.g. two
  `CANCEL`s, or a `SUSPEND` racing a `RESUME`) are serialised by the same
  short mutex acquisitions above — whichever is recorded first is the one
  the next boundary check observes; this design does not specify a
  priority order between simultaneous commands beyond "first recorded,
  first observed," since nothing in `AgentRunCommand.kt` or
  `AgentRuntimeSpecification.md` requires more than that.

**Thread-safety requirement:** no two concurrent callers may observe or
produce an inconsistent `AgentRun.status` for the same `agentRunId` —
every transition in Section 5 must be applied via the same
`requireValidTransition`-then-store pattern `InMemoryAgentRuntime.advance`
already uses, unchanged, just no longer wrapped in a lock that spans an
entire Agent Step.

## 9. Security Considerations

- **No new authority path.** Every proposed action from `AgentStepSource`
  (Section 4.2) still passes through `PermissionEngine.evaluate` exactly
  once via `ExecutionPipeline.submit` (Section 2). Nothing in this design
  gives a second-or-later Agent Step a cached, reused, or pre-approved
  decision — `deniedActions` (Section 6.2) is read-only planning
  convenience for `AgentStepSource`, never a substitute for evaluation
  (mirrors §8's own "Permission scope... is a cache of past decisions for
  planning convenience, not itself a source of authority").
- **`SUSPEND`/`RESUME`/`CANCEL` require an authenticated, attributable
  Principal.** `AgentRunCommand.requestingPrincipalId` is already a
  required field (`AgentRunCommand.kt`) — this design adds no anonymous
  or Agent-Instance-self-issued path to any of the three commands. An
  Agent Instance cannot suspend, resume, or cancel its own Agent Run
  directly; only an external caller (in practice, the Task Manager
  Runtime, per `TaskManagerRuntimeSpecification.md` §7's sequence
  diagram) issues these via `AgentRunCommandChannel.submit`.
- **`AgentStepSource` is untrusted with authority by construction.**
  Section 2 already states this; restated here because it is the one new
  seam this design introduces. Its interface (Section 4.2) has no return
  path that grants, approves, or bypasses anything — `AgentStepDecision`
  has exactly three variants, none of which is or resembles a
  `PermissionDecision`.
- **`ExecutionPipeline.cancel`'s best-effort nature (Section 6.5) is
  disclosed, not concealed.** A cancelled-but-still-completing Tool
  action is a known, bounded risk this design names explicitly rather
  than implying a stronger cancellation guarantee than
  `DefaultExecutionPipeline.cancel` already provides.
- **Event auditability is strictly increased, not decreased.** Every
  `agent.*` event Section 5.1 specifies publishing is one
  `AgentRuntimeSpecification.md` §9 already requires; this design adds no
  new event type and removes none. Multi-step execution means
  significantly more of §9's table is actually exercised in practice than
  Unit 7 ever exercised (Section 3), which is a net improvement to
  AD-009 ("Everything Important Is Auditable") compliance, not a new
  obligation invented here.
- **Identity revocation dependency is unchanged and still disclosed**
  (Section 7) — this design does not claim to close gaps #37/#39, and
  does not let multi-step execution imply a stronger revocation guarantee
  than the platform currently supports.

## 10. Relationship to Existing Components

- **Task Manager Runtime.** No change required to
  `InMemoryTaskManagerRuntime` (Units B1/B2, already shipped). It
  continues to subscribe to exactly `agent.completed`/`agent.failed` and
  continues to apply exactly the fixed rule Unit B2 already implements.
  Multi-step execution changes *when* those two events fire (after
  however many Agent Steps an Agent Run actually takes) but not their
  meaning, payload correlation (`taskId`), or the Task Status transition
  rule already built. AD-006 ("Agent Runtime Never Owns Tasks") is
  unaffected — this design adds no Agent-Runtime-to-Task-state write
  path.
- **Permission Engine.** Called exactly as today, once per
  `ExecutionRequest`, via `ExecutionPipeline.submit` only. No direct call
  from the Agent Runtime to `PermissionEngine.evaluate` is introduced or
  implied (Section 2, Section 9).
- **Execution Pipeline.** `submit` is now called once per Agent Step
  (previously once per Agent Run, since Unit 7 modelled exactly one
  step) — a change in call *frequency*, not in how the call is made or
  what it returns. `cancel` is a newly-*exercised* call (Section 6.5);
  its signature and behaviour are unchanged.
- **Event Bus.** No change to `EventBus`'s interface. This design
  significantly increases which `agent.*` `EventType`s are actually
  published (Section 3, Section 9) — all already named by
  `AgentRuntimeSpecification.md` §9, none newly invented.
- **Memory.** No interaction. Chapter 17 (Memory Architecture) has no
  specification document in this repository yet (confirmed absent from
  `docs/architecture/`), and `AgentRuntimeSpecification.md` §3/§12
  already excludes it from this document's scope. Multi-step Agent
  Context (Section 6.2) does not read from or write to Memory in any
  form.
- **World Model.** No interaction. `docs/architecture/16-world-model.md`
  exists as an architecture chapter but is not implemented, and
  `AgentRuntimeSpecification.md` §3/§8/§12 already excludes World Model
  consultation from Agent Context. This design does not have
  `AgentStepSource` or any other new seam query the World Model — a
  future Planner-backed `AgentStepSource` implementation MAY choose to,
  entirely outside this design's own scope, per §12's reserved-but-
  unspecified seam.
- **Identity Service.** Unchanged — `IdentityService.resolve` is called
  once at `INITIALISED` exactly as today (Section 7's revocation
  dependency note aside, which is inherited, not newly introduced).
- **Resource Registry / Tool Registry / Tool.** Unchanged — reached only
  through `ExecutionPipeline.submit`, never directly, exactly as
  `AgentRuntimeSpecification.md` §6 already requires and as Unit 7
  already respects.

## 11. Future Implementation Notes

**Unit C2 WILL implement, per this design:**

- The multi-step loop (Section 6.1) and the per-step event/transition
  sequence (Section 5.1).
- `AgentStepSource` and `AgentStepContext` (Section 4.2), plus a fixed,
  deterministic, non-Planner default implementation of `AgentStepSource`
  for testing and for any production wiring that exists before a real
  Planner does — mirroring `DeterministicPlannerHarness`'s exact
  precedent (a named, honestly-labelled stand-in, never mistaken for the
  general mechanism). This document does not name or shape that default
  implementation beyond "fixed and deterministic," since doing so would
  itself be inventing Planner-adjacent behaviour ahead of Chapter 20.
- `AgentPolicy` (Section 4.3) with `maxAgentSteps` enforced (Section 7,
  "Step budget exhaustion"). `maxAgentRunDuration` is present in the
  shape but its enforcement is explicitly deferred (below).
- `SUSPEND`, `RESUME`, and `CANCEL` handling (Sections 6.3–6.5), including
  the locking-granularity change specified in Section 8.
- Every `agent.*` event Section 5.1 names, using the existing `ParkerEvent`/
  `EventBus.publish` mechanism Unit 7 already established the pattern
  for.

**Unit C2 will NOT implement (explicitly out of scope, not silently
assumed):**

- A real Planner, or any actual step-proposal intelligence.
  `AgentStepSource`'s production behaviour beyond a fixed, deterministic
  stand-in remains Chapter 20 territory.
- `WAITING_FOR_INPUT`'s actual input-supply channel, authentication, or
  payload validation (Section 5.2, Section 6.6). The state and its Trust
  Boundary remain specified but unreachable through this design's own
  scope; a future unit may name and implement the "supply input"
  operation the Trust Boundary already anticipates.
- `AgentPolicy.maxAgentRunDuration` enforcement (wall-clock timeout while
  a step is in flight, mid-Tool-execution). Only step-count bounding
  (`maxAgentSteps`) is required by this design.
- Any Agent Capability shape or enforcement — no `AgentCapability` Kotlin
  type is proposed by this design (Section 4.3).
- Any change to how a `RESUME` past a previously step-budget-exhausted
  `SUSPENDED` Agent Run is authorised (Section 7's own note that the
  re-check mechanism is left to Unit C2, not decided here).
- Identity revocation detection timing (Section 7) — inherited, unclosed
  dependency on gaps #37/#39, not this design's or Unit C2's job to
  close.
- Any Task Manager, Permission Engine, Tool Registry, or EventBus
  interface change. None is required (Section 10).
- Cross-Agent-Run or cross-Task orchestration (Workflow Engine, Chapter
  38) — a single Agent Run's own lifecycle only, exactly as
  `AgentRuntimeSpecification.md` §12 already scopes it.
- Agent Instance re-use across more than one Agent Run — still the same
  open question `AgentRuntimeSpecification.md` §8/§12 already leaves
  open; this design does not resolve it.

## 12. Acceptance Criteria

This design is complete when, and only to the extent that:

- **No new lifecycle state or edge is introduced.** Section 5 uses only
  transitions `AgentRunLifecycleTransitions` already permits.
- **`AgentStepSource` introduces no authority.** Section 4.2's interface
  has no variant that approves, denies, or bypasses Permission Engine
  evaluation; Section 9 states this explicitly.
- **`SUSPEND`, `RESUME`, and `CANCEL` are each given a precise, testable
  behaviour** (Sections 6.3–6.5), including the one case the current
  synchronous Execution Pipeline makes unavoidable (a command arriving
  while a step is in flight), rather than leaving it to be discovered
  mid-implementation.
- **The concurrency model is precise enough to implement without further
  design decisions.** Section 8 states exactly what is locked, for how
  long, and states explicitly that the existing whole-step lock is being
  changed, and why.
- **`DENIED` vs. `DEFERRED` are treated distinctly**, correcting Unit 7's
  existing collapse of both into `FAILED` — Section 5.2 and Section 7
  state this change and its justification explicitly, rather than
  silently perpetuating the collapse into a many-step design where it
  would matter far more.
- **No existing component's contract, behaviour, or test is required to
  change.** Section 10 confirms Task Manager Runtime, Permission Engine,
  Execution Pipeline's interface, EventBus, and Identity Service all
  continue to operate exactly as already implemented and already tested.
- **Every design choice is either already specified elsewhere (and
  cited) or is marked as a genuine decision this document is making**
  (the locking-granularity change, Section 8; the `DENIED`-ends-the-run
  choice, Section 7; the zero-step-`Complete`-is-`Fail` rule, Section
  5.1) — nothing is left ambiguous for Unit C2 to decide on its own
  authority.
- **Unit C2's scope is bounded precisely.** Section 11 states plainly
  what will and will not be implemented, mirroring the same discipline
  `SPRINT_2_IMPLEMENTATION_PLAN.md` required of every other unit's own
  Definition of Done.
- **No constitutional principle, Architecture Decision, or existing
  specification is contradicted.** Section 2 checks this design against
  the Constitution's own chain explicitly; no section elsewhere in this
  document reopens a question AD-001 through AD-016 already settles.

---

## Conclusion

**Track C implementation (Unit C2) can safely begin, once this design
document itself has been reviewed and accepted, on the basis of this
document alone — no additional architectural work is required first.**

Every question `SPRINT_2_IMPLEMENTATION_PLAN.md`'s own Unit C1 objective
poses (how many steps, what state is retained, how a paused run resumes)
is answered above with a citation back to already-approved architecture
wherever one exists, and with an explicitly-labelled, narrowly-scoped new
decision everywhere one did not (Sections 4.2, 4.3, 5.1 item 4, 6.5's
immediate-vs-deferred asymmetry, 7's `DENIED`-ends-the-run restatement,
and 8's locking-granularity change). No Architecture Decision requires
revision, no existing specification requires correction, and no existing
component's already-shipped, already-tested behaviour (Track A, Track B,
Sprint 1's vertical slice) requires modification.

The one open item this review surfaced that is **not** resolved here,
and is not blocking, is `docs/architecture/PROJECT_GOVERNANCE.md`'s
continued emptiness (Section "Review," item 4) — a pre-existing
documentation gap unrelated to Track C's technical content, already
recorded by the Sprint 2 Health Review, and appropriately left to a human
decision rather than invented here.

Recommendation: **proceed to Unit C2, implementing exactly what Section
11 names, once this document is reviewed and accepted.**
