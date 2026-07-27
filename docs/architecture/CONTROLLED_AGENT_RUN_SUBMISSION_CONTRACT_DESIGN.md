# Controlled Agent Run Submission — Contract Design

**Status:** Approved; amended once (see Amendment 1, end of document). No source files modified by this document. No implementation code written. Nothing staged, committed, or pushed.

**Builds on:** `docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_GOVERNANCE_REVIEW.md` (approved; "Ready for Contract Design"). Every finding, gap, and open question cited below refers to that document's own section numbers unless stated otherwise.

**Repository baseline:** Branch `main`, commit `992538e`, working tree clean, synchronized with `origin/main`.

**Constitutional constraint carried forward unchanged:** "Cognition proposes. Trust authorises. Runtime executes." This design adds exactly one new authority checkpoint — a run-initiation permission evaluation — and changes nothing about how "Trust authorises" is already enforced for every action inside an Agent Run.

---

## 0. Decisions This Document Freezes

The fourteen decisions below were handed down as already-resolved. This document does not re-litigate them; it gives each one an exact, buildable shape and resolves the secondary questions each one raises that were not themselves specified.

1. `AgentRunCommandChannel` is injected directly into `InMemoryTaskManagerRuntime`.
2. `InMemoryTaskManagerRuntime` is the sole production caller of `AgentRunCommandChannel.submit()` for `START`.
3. `AgentRunCommand.START` undergoes an explicit run-initiation permission evaluation before `InMemoryAgentRuntime` starts the run.
4. `requestingPrincipalId` is the evaluated Principal in that authorisation decision.
5. Per-action Trust-gating inside `InMemoryAgentRuntime` (via `ExecutionPipeline`/`PermissionEngine`) is unchanged and remains mandatory.
6. On `AgentRunCommandResult.Accepted`: publish `task.agent_run_started`; transition the Task `QUEUED -> RUNNING`.
7. On `AgentRunCommandResult.Rejected`: do not transition the Task; publish `task.agent_run_rejected`; preserve the rejection reason.
8. A new production `DeterministicAgentStepSource` is created under `src/runtime/`.
9. It is a new implementation, never a relocation or reuse of a test fixture.
10. `AgentPolicy(maxAgentSteps = 10)` is frozen as the initial production default, explicitly documented as configurable and replaceable.
11. The existing duplicate-`START` protection in `InMemoryAgentRuntime` is preserved unchanged.
12. `InMemoryAgentRuntime` is constructed in the production composition root.
13. Integration coverage spans the full chain: `TaskProposal` → Task creation → `START` authorisation → `AgentRunCommandChannel.submit()` → Agent Run creation → Task transition → emitted audit events.
14. The stale "no implementation exists" documentation claims are corrected.

---

## 1. Component Responsibilities

| Component | Responsibility under this design | Change from today |
|---|---|---|
| `InMemoryTaskManagerRuntime` | Accepts a `TaskProposal`, creates and queues a `Task`, constructs `AgentRunCommand.START`, **and now submits it** via its injected `AgentRunCommandChannel`. Interprets the result: on `Accepted`, publishes `task.agent_run_started` and drives `QUEUED -> RUNNING`; on `Rejected`, publishes `task.agent_run_rejected` and leaves the Task at `QUEUED`. | Gains one constructor dependency, one method call, two new event-publishing branches, one new lifecycle transition call site. |
| `InMemoryAgentRuntime` | Unchanged: implements `AgentRunCommandChannel`, drives the ten-state Agent Run lifecycle, routes every proposed action through `ExecutionPipeline`/`PermissionEngine`. **New:** performs one additional Trust Framework check — a run-initiation permission evaluation — before any Agent Run is allowed to leave `CREATED`. | Gains one constructor dependency (`PermissionEngine`, used only for this one new check) and one new early step inside `start()`. |
| `PermissionEngine` (`DefaultPermissionEngine`, already composed) | Unchanged interface, unchanged implementation. Now called from two independent call sites instead of one: `DefaultExecutionPipeline` (per-action, unchanged) and `InMemoryAgentRuntime.start()` (new, run-initiation only). No new method, no new behaviour inside the engine itself. | Reused, not modified. |
| `DeterministicAgentStepSource` (new) | Supplies the one deterministic, non-Planner step decision production wiring needs today, per `AgentStep.kt`'s own KDoc and `MULTI_STEP_AGENT_RUN_DESIGN.md` §11. Behaviourally identical to `tests/runtime/SingleStepAgentStepSource.kt`, but a wholly separate, honestly-labelled production class. | New file; no existing file changes shape. |
| `ParkerRuntime` (composition root) | Constructs `DeterministicAgentStepSource`, the frozen `AgentPolicy` default, `InMemoryAgentRuntime`, and wires it into `InMemoryTaskManagerRuntime`'s new constructor parameter. Reuses the already-composed `PermissionEngine` instance for both `ExecutionPipeline` and `InMemoryAgentRuntime` — one shared instance, not two. | Composition ordering change (Section 8); no new system identity required (Section 4). |

---

## 2. Constructor Dependency Changes

```
class InMemoryTaskManagerRuntime(
    private val identityService: IdentityService,
    private val eventBus: EventBus,
    private val agentRunCommandChannel: AgentRunCommandChannel,   // NEW
) : TaskProposalIntake
```

```
class InMemoryAgentRuntime(
    private val identityService: IdentityService,
    private val executionPipeline: ExecutionPipeline,
    private val eventBus: EventBus,
    private val agentStepSource: AgentStepSource,
    private val agentPolicy: AgentPolicy,
    private val permissionEngine: PermissionEngine,               // NEW
) : AgentRunCommandChannel
```

`permissionEngine` is used exclusively for the new run-initiation check inside `start()` (Section 5). It is never passed to, substituted for, or consulted by `executionPipeline`'s own internal per-action evaluation — that call path (`ExecutionPipeline.submit` → `PermissionEngine.evaluate`, `DefaultExecutionPipeline.kt:150`) is untouched, per decision 5. In the production composition root, `permissionEngine` in `InMemoryAgentRuntime`'s constructor and the `PermissionEngine` instance already wired into `ExecutionPipeline` are the **same object** — one Permission Engine instance for the whole runtime, not two independently configured ones (Section 8).

No other production class's constructor changes. `AgentRunCommand`, `AgentRunCommandResult`, `AgentRunCommandChannel`, `TaskProposal`, `Task`, `AgentRun` are all unchanged as value types — this design adds no new field to any of them.

---

## 3. Permission Request Shape

This is the one place this design must resolve a genuine mismatch the Governance Review's Section 2.6/Section 5 flagged but did not resolve: `PermissionEngine.evaluate(request: ExecutionRequest): PermissionDecision` is shaped around "may this Principal perform this Action on this Resource" (`Permission.kt`'s own header comment), and every existing caller resolves `proposedActions` (free text) through `ActionMapper`/`ActionVocabulary` into a `(PermissionAction, ResourceType)` pair, then matches that pair against a `PermissionPolicyRule`. "May this Principal start this Agent Run" is not naturally a resource-verb question — but the repository already reserves exactly the right pair for it: `ResourceType.AGENT` and `PermissionAction.EXECUTE` already exist (`Resource.kt`, `Permission.kt`), and neither is currently used for this purpose or any other in production code. This design adopts that pair rather than inventing a new enum value on either type — no change to `Resource.kt` or `Permission.kt` is required.

**Frozen shape.** `InMemoryAgentRuntime.start()` constructs a synthetic `ExecutionRequest` — never persisted, never passed to `ExecutionPipeline.submit` (that would additionally attempt Tool resolution and execution, which a run-initiation check must never do) — and calls `permissionEngine.evaluate(request)` directly:

```
ExecutionRequest(
    requestId = RequestId("run-init-${command.taskId.value}"),
    principalId = command.requestingPrincipalId,      // decision 4
    origin = RequestOrigin.AGENT,
    intent = "Start Agent Run for Task '${command.taskId.value}': ${command.goalDescription}",
    targetResources = command.resourceReferences,
    proposedActions = listOf("agent_run.start"),
    priority = RequestPriority.NORMAL,
    createdAt = Instant.now(),
    correlationId = command.correlationId,
)
```

`"agent_run.start"` is a new `ActionVocabulary` entry this design names and reserves, mapping to `(PermissionAction.EXECUTE, ResourceType.AGENT)` — mirroring the dotted-name convention already used for event types (`agent.completed`, `task.created`). Naming it here is this Contract Design's job; **authoring the actual `ActionVocabulary` registration and a `PermissionPolicyRule` for `(EXECUTE, AGENT)` is explicitly not part of this document's scope** (Section 10, Non-Goals) — it is production policy *content*, the same category of decision `DefaultPermissionPolicy.kt`'s own KDoc already reserves for "a caller," never hardcoded by the engine itself.

**Disclosed, accepted consequence.** Until that vocabulary entry and at least one policy rule exist in production configuration, `ActionMapper.map` will resolve no mapping for `"agent_run.start"`, and `DefaultPermissionPolicy`'s own documented "Unknown Action → DENIED" conservative default (`DefaultPermissionPolicy.kt:116-119`) applies: **every `START` submission will be denied by default** until that configuration is added. This is treated as a feature, not a defect, of a Trust Framework that fails closed — but it is named here explicitly so it is never mistaken for a bug during Native Verification. Scope Lock must decide whether authoring one minimal, permissive default rule is in-scope for this milestone or deferred as its own follow-on configuration task; this document does not decide that for it.

**`command.resourceReferences` as `targetResources`.** No Resource-Registry-registered entity represents "the Agent Run about to be created" (it doesn't exist yet), so `targetResources` is populated from whatever Resources the originating `TaskProposal` already carried forward (`AgentRunCommand.resourceReferences`, unchanged field). If that list is empty — the common case today, since no production caller currently populates it — the evaluation still proceeds (an empty `targetResources` list is not itself an error; `ActionMapper.map` simply resolves against an empty `resourceTypes` set, which also resolves to "no mapping," and therefore also denies by the same conservative default). This is consistent, not a special case.

**`APPROVED_WITH_CONFIRMATION` and `DEFERRED` collapse to a binary result**, since `AgentRunCommandResult` has exactly two variants (`Accepted`/`Rejected`) and this design does not add a third:

| `PermissionDecisionOutcome` | Effect |
|---|---|
| `APPROVED` | Proceed to Agent Identity resolution (existing behaviour, Section 5). |
| `APPROVED_WITH_CONFIRMATION` | Treated identically to `APPROVED` — mirrors `DefaultExecutionPipeline`'s own existing "confirmation workflow is out of scope" treatment of the same outcome (`DefaultExecutionPipeline.kt:153-156`). |
| `DENIED` | `AgentRunCommandResult.Rejected(START, "run-initiation permission DENIED for requestingPrincipalId '<id>'")`. No Agent Run record is created. |
| `DEFERRED` | `AgentRunCommandResult.Rejected(START, "run-initiation permission DEFERRED for requestingPrincipalId '<id>'")`. Distinct reason text from `DENIED`, same terminal outcome — see Section 10, Non-Goals, for why this collapse is deliberate. No Agent Run record is created. |

---

## 4. Identity and Principal Semantics

Two identities remain distinct, exactly as the Governance Review named them (Section 2.5):

- **`AgentRunCommand.requestingPrincipalId`** — unchanged in provenance (`InMemoryTaskManagerRuntime` still sets it to the Task's resolved owner). Newly **load-bearing**: this is the exact Principal the run-initiation permission evaluation authorises or denies (Section 3). It was previously carried for attribution only; it now has real authorisation consequence for the first time.
- **The Agent Identity** (`agent-for-${taskId.value}`) — unchanged. Still resolved and validated (`INTERNAL_AGENT`, non-null `owner`) exactly as today, and still evaluated strictly *after* the new permission check succeeds (Section 5's ordering). A run whose requesting Principal is denied never reaches Agent Identity resolution at all.

**No new system identity is required by this milestone.** `InMemoryAgentRuntime` publishes every `agent.*` event under `run.agentIdentityPrincipalId` (the per-Task identity), not a fixed system identity of its own — unlike `InMemoryPlannerRuntime`/`InMemoryTaskManagerRuntime`, it has no `private companion object` principal literal today and needs none for this change. `TASK_MANAGER_RUNTIME_PRINCIPAL_ID` (already registered) remains the publisher for both new `task.*` events (Section 6). The identity-registration item the Governance Review flagged as a possible open cost (Section 2.11) resolves to zero new registrations.

---

## 5. `InMemoryAgentRuntime.start()` — Revised Sequence

Frozen ordering, replacing the current sequence with the minimum insertion needed:

1. **Duplicate check** (unchanged): `check(agentRunId !in agentRuns)` under `mutex.withLock`, using the existing deterministic `taskId`-derived ID (decision 11).
2. **Run-initiation permission evaluation (new)**: construct the synthetic `ExecutionRequest` (Section 3) and call `permissionEngine.evaluate(request)`. On `DENIED`/`DEFERRED`, return `AgentRunCommandResult.Rejected` immediately (Section 3's table) — evaluated *before* Agent Identity resolution, since authority to attempt the run at all is logically prior to determining which Agent Identity would carry it out.
3. **Agent Identity resolution** (unchanged): resolve `agent-for-${taskId.value}` through `IdentityService`, require `INTERNAL_AGENT` with non-null `owner`, `Rejected` otherwise.
4. **Lifecycle drive and multi-step loop** (unchanged): `CREATED -> INITIALISED -> READY -> RUNNING`, `runLoop`, per-action Trust-gating via `ExecutionPipeline` (decision 5, untouched).

**Open item deferred to Scope Lock, not resolved here:** the exact resulting `AgentRunStatus` for a permission-denied `START` — whether it mirrors the existing "stuck at `CREATED`" precedent set for identity-resolution rejection, or is recorded as immediately `FAILED` — is a one-line implementation choice with no architectural consequence either way, and is named here rather than silently assumed.

---

## 6. Accepted and Rejected Submission Flows

### 6.1 Accepted

```
submitProposal(proposal):
    ... (Task created, Task -> QUEUED, exactly as today, unchanged) ...
    command = AgentRunCommand(START, ...)                         // unchanged construction
    result = agentRunCommandChannel.submit(command)                // NEW
    when (result) {
        is Accepted -> {
            publish("task.agent_run_started", taskId, correlationId,
                    payload = mapOf("agentRunId" to result.agentRunId.value))
            TaskLifecycleTransitions.requireValidTransition(QUEUED, RUNNING)
            tasks[taskId] = task.copy(status = RUNNING)
        }
        is Rejected -> { ... see 6.2 ... }
    }
    return TaskProposalDisposition.Accepted(proposal.taskProposalId, taskId)   // UNCHANGED, either branch
```

**`TaskProposalDisposition.Accepted` is returned regardless of the `AgentRunCommandResult`.** Proposal intake and run authorisation are independently reported outcomes — the `TaskProposal` was legitimately accepted and its `Task` legitimately exists either way; `TaskProposalDisposition`'s own existing documentation already anticipates this ("Agent Runtime may be involved later... at the Task Manager Runtime's own discretion" — discretion that can be declined). This resolves an ambiguity the frozen decision list did not itself address.

**Compatibility with existing `agent.completed` handling, verified, not just assumed:** `applyCompletedTransition`'s existing `TaskStatus.RUNNING` branch already handles "a Task already `RUNNING` takes only the second edge" (`InMemoryTaskManagerRuntime.kt:387-396`, Sprint 2 Unit B2). Since this design causes `RUNNING` to be reached at `Accepted` time rather than only at `agent.completed` time, a subsequent real `agent.completed` event for the same Task lands on the already-existing `RUNNING` branch and drives `RUNNING -> COMPLETED` exactly as already implemented and tested. **No change to `applyCompletedTransition` is required.** This resolves Governance Review Open Question 3 (Section 2.8): the Task transitions at `START` acceptance, not deferred to the Agent Run's own terminal event.

### 6.2 Rejected

```
        is Rejected -> {
            publish("task.agent_run_rejected", taskId, correlationId,
                    payload = mapOf("reason" to result.reason, "commandType" to result.commandType.name))
            // Task remains at QUEUED -- no TaskLifecycleTransitions call.
        }
```

The Task is left exactly at `QUEUED`. No later code path in `InMemoryTaskManagerRuntime` currently re-attempts submission for a `Rejected` command — retry, if ever wanted, is out of this milestone's scope (Section 10).

---

## 7. Task Lifecycle Transitions

No new `TaskStatus` value and no new edge in `TaskLifecycleTransitions` — `QUEUED -> RUNNING` is already a valid, already-implemented edge (Sprint 2 Unit B2). This design adds a second call site for an edge that already exists, and, per Section 6.1, does so compatibly with the existing call site rather than in tension with it. A `Rejected` `START` performs no transition at all — the Task simply stays `QUEUED`, exactly like every other "no applicable rule" case `InMemoryTaskManagerRuntime` already handles (e.g. an unresolvable owner never reaching `CREATED` at all).

---

## 8. Event Definitions and Payloads

| Event | Trigger | Publisher | Payload | Status |
|---|---|---|---|---|
| `task.agent_run_started` | `AgentRunCommandResult.Accepted` for a `START` command | `TASK_MANAGER_RUNTIME_PRINCIPAL_ID` | `{"agentRunId": <value>}` | Already named and reserved by `TaskManagerRuntimeSpecification.md` §10 (`TaskAgentRunStarted`); never previously publishable because no Agent Run genuinely existed (Governance Review Section 2.7). This design is what first makes it truthfully publishable. |
| `task.agent_run_rejected` | `AgentRunCommandResult.Rejected` for a `START` command | `TASK_MANAGER_RUNTIME_PRINCIPAL_ID` | `{"reason": <result.reason>, "commandType": "START"}` | **New.** Not currently named in `TaskManagerRuntimeSpecification.md`'s §10 event table. This design adds it; Documentation Reconciliation must add the corresponding table row (Section 12, decision 14 extended). |

All existing `agent.*` events published by `InMemoryAgentRuntime` (`agent.created` through `agent.failed`, and the `agent.completed`/`agent.failed` subscriptions already wired in `InMemoryTaskManagerRuntime`'s `init` block) are unchanged by this design — Section 2.10 of the Governance Review already found this side fully solved.

---

## 9. Production Composition Changes

`ParkerRuntime.kt`'s composition root must be extended, with one real ordering constraint: `InMemoryAgentRuntime` must now be constructed **before** `InMemoryTaskManagerRuntime`, reversing today's implicit assumption that `InMemoryTaskManagerRuntime` has no runtime-side dependency.

```
private companion object {
    ... existing literals unchanged ...
    val DEFAULT_AGENT_POLICY = AgentPolicy(maxAgentSteps = 10)   // decision 10 — see note below
}

val deterministicAgentStepSource = DeterministicAgentStepSource()

val agentRuntime = InMemoryAgentRuntime(
    identityService = identityService,
    executionPipeline = executionPipeline,     // already composed, unchanged
    eventBus = eventBus,
    agentStepSource = deterministicAgentStepSource,
    agentPolicy = DEFAULT_AGENT_POLICY,
    permissionEngine = permissionEngine,       // SAME instance already composed into executionPipeline
)

val taskManagerRuntime = InMemoryTaskManagerRuntime(
    identityService = identityService,
    eventBus = eventBus,
    agentRunCommandChannel = agentRuntime,     // InMemoryAgentRuntime satisfies AgentRunCommandChannel
)
// plannerRuntime, planCandidateGenerator, goalPlanningHandoffCoordinator: unchanged, still built from taskManagerRuntime
```

**`AgentPolicy(maxAgentSteps = 10)` is frozen as a named composition-root constant, explicitly documented in its own KDoc as a Sprint-phase default, not a permanent or authoritative production value** — mirroring exactly how every other composition-root literal in this file (`PLANNER_RUNTIME_PRINCIPAL_ID`, etc.) is already documented as "configuration, not a claim about a real allocation scheme." No specification currently authorises a specific `maxAgentSteps` value; this document freezes `10` as the initial value precisely because the only existing precedent for it (`tests/runtime/SingleStepAgentStepSource.kt`'s own `DEFAULT_AGENT_POLICY`) already uses it, and promotes it to production status explicitly rather than silently.

**Zero new system identity registrations** (Section 4). **One reused, not duplicated, `PermissionEngine` instance.**

---

## 10. `DeterministicAgentStepSource` Contract

```
// src/runtime/DeterministicAgentStepSource.kt
package parker.core.runtime

class DeterministicAgentStepSource : AgentStepSource {
    override suspend fun nextStep(context: AgentStepContext): AgentStepDecision =
        if (context.stepNumber == 1) {
            AgentStepDecision.Propose(context.goal, context.resourceReferences)
        } else {
            AgentStepDecision.Complete
        }
}
```

Behaviourally identical to `tests/runtime/SingleStepAgentStepSource.kt` — deliberately so, since both implement the same fixed, one-step-then-complete shape `AgentStep.kt`'s own KDoc and `MULTI_STEP_AGENT_RUN_DESIGN.md` §11 already anticipate as the correct stand-in "for any production wiring that exists before a real Planner does." **This is a new, separate production implementation — not a relocation of the test fixture** (decisions 8-9), mirroring `DefaultPlanCandidateGenerator`'s own precedent from the immediately preceding milestone exactly, down to the same justification: a production class must live in `src/`, be independently documented as a deliberate stand-in, and never be mistaken for the real thing (Chapter 20's future Planner-backed `AgentStepSource`) it reserves the seam for.

**Naming discrepancy resolved.** `AgentStep.kt`'s own KDoc currently cites a class named `FixedSequenceAgentStepSource`, which has never existed under that name — the real file is `tests/runtime/SingleStepAgentStepSource.kt`. This design does not rename the test fixture (out of scope; it is working, tested, and serves tests correctly under its current name). It freezes the *new* production class's name as `DeterministicAgentStepSource` — distinct from both existing names — and requires, as a Documentation Reconciliation item (Section 12), that `AgentStep.kt`'s KDoc be corrected to reference `DeterministicAgentStepSource` (the real production stand-in) rather than the never-existent `FixedSequenceAgentStepSource`, while noting `SingleStepAgentStepSource`'s continued, legitimate, test-only existence alongside it.

No test fixture is deleted, moved, or modified by this design.

---

## 11. Failure Propagation

Distinguish, as this repository consistently does, a **typed outcome** (`AgentRunCommandResult.Rejected`, handled per Section 6.2) from a **genuine fault** (an unexpected exception from `identityService`, `eventBus`, or the injected `agentRunCommandChannel` itself). Per the fault-behaviour precedent already frozen for `GoalPlanningHandoffCoordinator` in the preceding milestone: an unexpected exception thrown by `agentRunCommandChannel.submit(command)` is **not caught** by `InMemoryTaskManagerRuntime.submitProposal()` — it propagates uncaught to the caller. The Task remains at whichever state it last successfully reached before the fault (`QUEUED`, since Task creation and queuing both complete, and only the submission call itself faults) — no partial or invented state is fabricated, and no event is published describing a submission outcome that never actually occurred. This mirrors the same "do not swallow, do not silently reinterpret a fault as a typed rejection" discipline already established and tested for the Planner integration milestone.

---

## 12. Documentation Reconciliation (Decision 14, Scoped)

To be performed at Documentation Reconciliation time, not by this Contract Design or its eventual Implementation phase in isolation — named here so Scope Lock can assign it precisely:

- `src/contracts/AgentRunCommand.kt` — correct `AgentRunCommandChannel`'s KDoc, which currently states "No implementation of this interface exists in this repository." It has, since `InMemoryAgentRuntime`'s Track C Unit C2 work, predating this milestone.
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md` §16 — same correction, plus adding `task.agent_run_rejected` to the §10 event table (Section 8).
- `src/contracts/AgentStep.kt` — correct the KDoc's `FixedSequenceAgentStepSource` citation per Section 10 above.
- `docs/architecture/IMPLEMENTATION_GAPS.md` — update the Gap #53 entry (`IMPLEMENTATION_GAPS.md:2513-2517`), which currently states "no `AgentRunCommandChannel` implementation exists anywhere in this repository, so `InMemoryTaskManagerRuntime`'s constructed `AgentRunCommand` is never submitted or consumed" — both halves of that sentence become false once this milestone lands.
- `docs/implementation/IMPLEMENTATION_HISTORY.md` — new entry, per the established per-milestone convention.

---

## 13. Test Obligations

1. **`tests/runtime/InMemoryTaskManagerRuntimeTest.kt`** (modified) — new constructor argument threaded through every existing test (a fake or stub `AgentRunCommandChannel`, distinct from `InMemoryAgentRuntime`, for tests not specifically about submission); new tests for the `Accepted`/`Rejected` branches (Section 6), `task.agent_run_started`/`task.agent_run_rejected` payload content (Section 8), and confirming `TaskProposalDisposition.Accepted` is returned in both branches (Section 6.1).
2. **`tests/runtime/InMemoryAgentRuntimeTest.kt`** (modified) — new constructor argument (a fake `PermissionEngine` that approves by default, threaded through existing tests so their existing assertions are undisturbed); new tests for `DENIED`/`DEFERRED` run-initiation outcomes (Section 3's table), confirming no Agent Run is created past whatever state Scope Lock assigns (Section 5's open item) and that Agent Identity resolution is never attempted when run-initiation is denied.
3. **`tests/runtime/DeterministicAgentStepSourceTest.kt`** (new) — direct unit coverage of the `Propose`-then-`Complete` contract (Section 10), mirroring `DefaultPlanCandidateGeneratorTest.kt`'s precedent for giving a new production stand-in its own dedicated test file.
4. **A new integration test** (proposed name: `tests/runtime/TaskManagerAgentRunSubmissionIntegrationTest.kt`; exact name and file left for Scope Lock to fix) — real `InMemoryTaskManagerRuntime` wired to a real `InMemoryAgentRuntime` (with `DeterministicAgentStepSource`, a real `IdentityService`/`EventBus`/`ExecutionPipeline`, and a `PermissionEngine` configured to approve `agent_run.start`), exercising the full chain named in decision 13 end-to-end: submit a `TaskProposal` → observe `Task` creation and queuing → observe the run-initiation authorisation → observe `AgentRunCommandChannel.submit()` produce `Accepted` → observe the Agent Run actually running → observe the `QUEUED -> RUNNING` Task transition → observe `task.agent_run_started` on the event bus. A second scenario in the same file exercises the `Rejected` path (a `PermissionEngine` configured to deny), asserting the Task never leaves `QUEUED` and `task.agent_run_rejected` carries the exact denial reason.
5. **`tests/composition/ParkerRuntimeConversationPipelineTest.kt`** or a new, narrower composition-level test — confirms `InMemoryAgentRuntime` is genuinely constructed and reachable from `ParkerRuntime`'s composition root with the frozen `AgentPolicy` default (Section 9); a wiring/existence check, not a second full end-to-end traversal (item 4 already covers that at the unit level).

This directly closes the one coverage gap the Governance Review named as significant (Section 2.12): no existing test exercises `InMemoryTaskManagerRuntime` and `InMemoryAgentRuntime` together.

---

## 14. Explicit Non-Goals

- **No new `AgentRunCommandResult` variant.** `APPROVED_WITH_CONFIRMATION` collapses into the accepted path and `DEFERRED` collapses into `Rejected` (Section 3's table) — a `Deferred` result type is not introduced. A future milestone may revisit this; this one does not.
- **No new `PermissionAction` or `ResourceType` enum value.** `EXECUTE`/`AGENT`, both already existing and previously unused for this purpose, are reused as-is (Section 3).
- **No production `ActionVocabulary` entry or `PermissionPolicyRule` content is authored by this document.** The mechanism is fully specified (Section 3); the actual policy content that would let a real `START` be approved is explicitly left to Scope Lock or a follow-on configuration task, and until it exists, every `START` denies by design, not by accident.
- **No real Planner-backed `AgentStepSource`.** `DeterministicAgentStepSource` (Section 10) remains a deliberate, honestly-labelled stand-in; Chapter 20 is untouched.
- **`SUSPEND`/`RESUME`/`CANCEL` submission wiring is out of scope.** This design concerns `START` only, matching every one of the fourteen frozen decisions, none of which mention the other three command types. `InMemoryAgentRuntime`'s own handling of them (already implemented and tested) is untouched.
- **The other four `TaskProposalDisposition` outcomes** (`Deferred`, business-`Rejected`, `Split`, `Merged`) remain unimplemented, exactly as before — untouched by this design.
- **Scheduling, Workflow Engine implementation, multi-agent orchestration, distributed execution, advanced retry policies, plugin ecosystem expansion, Home Assistant integration, Android integration, and unrelated refactoring** remain out of scope, restated unchanged from the Governance Review (Section 7 there).
- **Per-action Trust-gating inside `InMemoryAgentRuntime.runLoop()` is not touched, extended, or re-validated** by this design (decision 5) — it was already correct and already tested before this milestone began.
- **Retry of a `Rejected` `START`** (automatic or otherwise) is not designed here — a `Rejected` command today simply leaves the Task at `QUEUED` with no further automatic action.

---

## 15. Proposed Scope Lock Boundaries

**Permitted production files:**
- `src/runtime/InMemoryTaskManagerRuntime.kt` (modify — Sections 2, 6, 7, 8)
- `src/runtime/InMemoryAgentRuntime.kt` (modify — Sections 2, 3, 5)
- `src/runtime/DeterministicAgentStepSource.kt` (new — Section 10)
- `src/composition/ParkerRuntime.kt` (modify — Section 9)
- `src/contracts/AgentRunCommand.kt` (KDoc-only correction — Section 12)
- `src/contracts/AgentStep.kt` (KDoc-only correction — Section 12)

**Permitted documentation files:**
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md` (Section 12)
- `docs/architecture/IMPLEMENTATION_GAPS.md` (Section 12)
- `docs/implementation/IMPLEMENTATION_HISTORY.md` (Section 12)

**Permitted test files:**
- `tests/runtime/InMemoryTaskManagerRuntimeTest.kt` (modify)
- `tests/runtime/InMemoryAgentRuntimeTest.kt` (modify)
- `tests/runtime/DeterministicAgentStepSourceTest.kt` (new)
- `tests/runtime/TaskManagerAgentRunSubmissionIntegrationTest.kt` or Scope Lock's own chosen name (new)
- `tests/composition/ParkerRuntimeConversationPipelineTest.kt` or a new, narrower composition test (modify or new)

**Frozen exclusions:** everything named in Section 14, plus every item the Governance Review's own Section 7 already excluded unless strictly required (none of which this design found a need to touch).

**Scope Lock must additionally fix, since this document deliberately leaves them open:**
- The exact resulting `AgentRunStatus` for a permission-denied `START` (Section 5).
- The exact new integration test's file name and class name (Section 13, item 4).
- Whether authoring one minimal, permissive `PermissionPolicyRule`/`ActionVocabulary` entry for `(EXECUTE, AGENT)` is in-scope for this milestone or deferred (Section 3).

---

## 16. Recommendation

**Ready for Scope Lock.** Every one of the fourteen frozen decisions now has an exact, buildable shape; the three items Section 15 leaves open are narrow, non-architectural, and suitable for Scope Lock to close without returning to Contract Design.

---

## Amendment 1 — Two-Phase Agent Run Operation (Acceptance/Execution Separation)

**Status:** Approved Contract Design Amendment, itself corrected once (see "Amendment 1, Lifecycle State Correction" immediately below it) before implementation began. Triggered by a genuine production defect found during Native Verification (a self-deadlock in `InMemoryTaskManagerRuntime.submitProposal()`) and a subsequent rejected first correction (releasing the mutex alone still let `task.agent_run_started` observably follow `task.completed`, changing the event's effective meaning). Authoritative basis: `docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_ACCEPTANCE_EXECUTION_SEPARATION_RECONCILIATION.md`, Option 1. The rejected first correction is recorded, marked superseded, in `docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_DEADLOCK_DESIGN_RECONCILIATION.md`.

**Lifecycle State Correction applied below, before implementation began.** This amendment originally ended phase 1 at `RUNNING`. That was found internally inconsistent — a state named `RUNNING` must mean execution has actually begun; a run that has not yet entered `runLoop()` cannot truthfully be `RUNNING`. A1.1 and A1.2 below already reflect the corrected boundary: **phase 1 ends at `READY`; phase 2 performs `READY -> RUNNING`, publishes `agent.started`, and only then calls `runLoop()`.** No future reader of this section needs to know a correction happened to use it correctly; this paragraph exists only as the provenance record — see §A1.8 for the full account of what changed and why.

**What this amendment establishes, and why it is an amendment rather than a clarification:** Sections 2, 5, and 6 above described `AgentRunCommandChannel.submit()` for `START` as a single call whose `Accepted` result means "authorised and finished" — a meaning inherited, unexamined, from `InMemoryAgentRuntime.start()`'s pre-existing (Sprint 3, Track C, Unit C2) behaviour of running its entire multi-step loop to completion before ever returning. The reconciliation found this conflation architecturally invalid: no restructuring of `InMemoryTaskManagerRuntime`'s own locking can make `task.agent_run_started` precede `agent.completed`/`task.completed` while `submit()` remains one atomic, non-decomposable call, because the caller cannot learn "Accepted" — and therefore cannot correctly publish that event — until the entire run has already finished. This amendment changes what `Accepted` means for `START`, and introduces a new interface `InMemoryTaskManagerRuntime` depends on. Both are genuine contract changes, not implementation detail.

### A1.1 Phase 1 — START acceptance (revises Section 5's steps 1-3; supersedes step 4 for `START` specifically)

`AgentRunCommandChannel.submit()`, for a `START` command, must:

1. Evaluate run-initiation permission (Section 3, unchanged).
2. On `DENIED`/`DEFERRED`: return `Rejected` without creating any `AgentRun` record (Section 5 step 2, unchanged).
3. Enforce duplicate-START protection (`check(agentRunId !in agentRuns)`, Section 5 step 1, unchanged — still the first act after permission approval, still owned exclusively by `InMemoryAgentRuntime`).
4. Create the `AgentRun` record.
5. Resolve Agent Identity (Section 5 step 3, unchanged; `Rejected` on failure, no `AgentRun` progresses past `CREATED`).
6. Advance the run through `CREATED -> INITIALISED -> READY`, publishing `agent.created`, `agent.initialised`, `agent.ready`. **`agent.started` is not published here** — it is published only when execution genuinely begins (A1.2).
7. Publish those Agent lifecycle events (already covered by step 6; restated for completeness against the required-elements list this amendment must satisfy).
8. Return `Accepted(agentRunId)` — **the `READY -> RUNNING` transition has not occurred, and the run loop is not invoked.** This is phase 1's terminal act.
9. **`runLoop()` is not called from within `submit()`/`start()` any longer, and neither is the `READY -> RUNNING` transition.** This is the structural change: everything Section 5 step 4 previously bundled into the same call now stops at `READY` — not `RUNNING`, which would itself be untruthful for a run that has not yet entered `runLoop()`.

**Frozen meaning of `Accepted` (supersedes Section 5's and Section 6.1's prior, unexamined meaning):**

> `Accepted` means: START has been authorised, the `AgentRun` record exists, Agent Identity has been resolved, and the `AgentRun` is `READY` for execution. **Execution has not yet begun.**

This is a narrower, more precise meaning than "authorised and finished," and a semantically truthful one: no state named `RUNNING` is ever occupied by a run whose step loop has not actually started. It does not change `AgentRunCommandResult`'s shape (still exactly `Accepted(agentRunId, commandType)` / `Rejected(commandType, reason)`, decision 3.1's "no new variant" non-goal is unaffected) — only what the existing `Accepted` variant is understood to certify.

### A1.2 Phase 2 — execution, via a new, narrow contract

```kotlin
interface AgentRunExecutionTrigger {
    suspend fun execute(agentRunId: AgentRunId)
}
```

(Final name may differ only if an existing repository naming convention clearly requires it — no such convention was found; `AgentRunExecutionTrigger` is adopted as proposed.)

The execution operation must:

1. Accept only a previously accepted (phase-1-completed) Agent Run — i.e. one already at `READY` with an existing `RunState`.
2. Perform the `READY -> RUNNING` transition — the one lifecycle edge phase 1 no longer performs.
3. Publish `agent.started` — now truthfully marking the moment execution actually begins, not a moment roughly one call earlier when it merely became possible.
4. Invoke the existing deterministic step loop (`runLoop()`, unchanged internals).
5. Preserve all existing per-action Trust evaluation (`ExecutionPipeline`/`PermissionEngine`, decision 5, untouched — this amendment does not touch `runLoop()`'s own body).
6. Preserve existing terminal and suspended outcomes (`COMPLETED`, `FAILED`, `SUSPENDED` via `maxAgentSteps`, `CANCEL`/`SUSPEND`/`RESUME` handling) — all unchanged.
7. Publish existing Agent lifecycle events (`agent.step_started` through `agent.completed`/`agent.failed`) exactly as `runLoop()` already does, in addition to `agent.started` (item 3).
8. Remain synchronous on the calling coroutine — no `launch`, no detached execution.
9. Introduce no owned background coroutine and no new shutdown responsibility — `execute()` is a plain suspend function; when it returns, the run has reached a terminal or suspended state, exactly as `submit()` used to guarantee, just one call later.

**`InMemoryAgentRuntime` is the sole implementation of both `AgentRunCommandChannel` and `AgentRunExecutionTrigger`** — the same object, not two cooperating instances. This preserves decision 12 (constructed once, in the composition root) and decision 2 (sole caller remains `InMemoryTaskManagerRuntime`, now via two interface references to the one object).

### A1.3 Revised constructor dependencies (supersedes Section 2's `InMemoryTaskManagerRuntime` shape)

```kotlin
class InMemoryTaskManagerRuntime(
    private val identityService: IdentityService,
    private val eventBus: EventBus,
    private val agentRunCommandChannel: AgentRunCommandChannel,       // unchanged
    private val agentRunExecutionTrigger: AgentRunExecutionTrigger,   // NEW
) : TaskProposalIntake
```

`InMemoryAgentRuntime`'s own constructor shape (Scope Lock Section 4's eight parameters, as actually implemented, including the `runInitiationVerbPhrase` Implementation Clarification) is unaffected by this amendment — it gains no new constructor parameter; it gains a second implemented interface.

### A1.4 Revised accepted flow (supersedes Section 6.1)

```
submitProposal(proposal):
    mutex.withLock {
        ... Task created, Task -> QUEUED, exactly as today, unchanged ...
        command = AgentRunCommand(START, ...)
        result = agentRunCommandChannel.submit(command)     // phase 1 only -- cannot trigger agent.completed
        when (result) {
            is Accepted -> {
                publish("task.agent_run_started", taskId, correlationId,
                        payload = mapOf("agentRunId" to result.agentRunId.value))
                TaskLifecycleTransitions.requireValidTransition(QUEUED, RUNNING)
                tasks[taskId] = task.copy(status = RUNNING)
            }
            is Rejected -> { ... unchanged, Section 6.2 ... }
        }
    }   // mutex released here
    if (result is Accepted) {
        agentRunExecutionTrigger.execute(result.agentRunId)  // phase 2, OUTSIDE the mutex
    }
    return TaskProposalDisposition.Accepted(proposal.taskProposalId, taskId)
```

`task.agent_run_started` is now published while phase 1's own lock section is still held — genuinely before execution can begin, not merely before `submit()` returns. Phase 2 runs after the lock is released, so the synchronous `agent.completed`/`agent.failed` delivery back into `InMemoryTaskManagerRuntime`'s own subscriptions never re-enters a held mutex (invariant 8 of the approval).

**`TaskStatus.RUNNING` and `AgentRunStatus.RUNNING` are two different enums with two different, non-synchronized meanings — named explicitly here so the Lifecycle State Correction (below) is never mistaken for having touched this line.** `TaskStatus.RUNNING` (`InMemoryTaskManagerRuntime`'s own state machine) means "this Task has an accepted, dispatched Agent Run" — true the moment phase 1 returns `Accepted`, regardless of whether that Agent Run's own internal step loop has started yet. `AgentRunStatus.RUNNING` (`InMemoryAgentRuntime`'s own ten-state machine, `AgentRunLifecycle.kt`) means "this Agent Run's step loop has actually begun" — true only once phase 2 performs `READY -> RUNNING`. The Lifecycle State Correction below changes when the *second* of these is reached; the Task-side transition above, at `Accepted` time, was always correct and is unchanged.

**Compatibility with existing `agent.completed` handling, re-verified under the new sequence:** by the time phase 2 can possibly publish `agent.completed`, the Task is already `TaskStatus.RUNNING` (phase 1's own follow-up, inside the first lock section, already performed that transition). `applyCompletedTransition`'s existing `TaskStatus.RUNNING` branch (Unit B2, unchanged) applies exactly as designed — no new idempotency guard is required, because the ordering is now genuinely correct by construction, not merely assumed.

### A1.5 Revised rejected flow

Unchanged in substance from Section 6.2: `Rejected` is returned entirely within phase 1; phase 2 is never invoked on this branch (see the `if (result is Accepted)` guard in A1.4); no `task.agent_run_started` is ever published for a rejected `START` (this was already true; it remains true, now provably so since phase 2 is gated on `Accepted` explicitly rather than on `submit()` having returned at all).

### A1.6 Non-Goals — unaffected

Every non-goal in Section 14 remains unaffected: no new `AgentRunCommandResult` variant, no new `PermissionAction`/`ResourceType` value, no production policy content authored here, no real Planner-backed `AgentStepSource`, `SUSPEND`/`RESUME`/`CANCEL` submission wiring still out of scope, no coroutine scope or shutdown-lifecycle change (explicitly required by the approval's invariant 10).

### A1.7 Recommendation

**Ready for Scope Lock Amendment.** This section fixes the two-phase contract shape completely; Scope Lock must now fix the exact file boundary, exact sequence freeze, and exact test-obligation expansion this amendment implies.

### A1.8 Lifecycle State Correction (provenance record)

Before implementation began, this amendment as originally approved ended
phase 1 at `AgentRunStatus.RUNNING` and had phase 1 publish `agent.started`.
That was reviewed and found internally inconsistent: a state named
`RUNNING` must mean execution has actually begun; a run that has not yet
entered `runLoop()` cannot truthfully occupy it, and `agent.started`
published before any step has been attempted is not a truthful "execution
began" signal. This is a semantic-integrity issue, not an editorial one —
future contributors must never have to remember that `RUNNING` doesn't
actually mean running.

**Corrected, and reflected directly in A1.1/A1.2 above (no residual
"translate as you read" text remains anywhere in this document):**

- Phase 1 ends at `AgentRunStatus.READY`, not `RUNNING`.
- Phase 1 publishes `agent.created`, `agent.initialised`, `agent.ready` —
  not `agent.started`.
- Phase 2 (`AgentRunExecutionTrigger.execute`) performs `READY -> RUNNING`,
  publishes `agent.started`, and only then calls `runLoop()`.
- `Accepted`'s frozen meaning is corrected to "the `AgentRun` is `READY` for
  execution," not "has reached `RUNNING`."

No other part of Amendment 1 changes: the interface shape, the constructor
dependency, the mutex-release discipline, the `Rejected` flow, and the
non-goals are all unaffected by this correction.
