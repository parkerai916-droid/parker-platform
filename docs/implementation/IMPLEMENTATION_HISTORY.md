# Parker Platform

> This document should be updated only when an implementation unit is
> completed and merged into `main`.

## Implementation History

This document records the chronological implementation of the Parker
Platform architecture. It allows contributors to understand how
architectural concepts became executable runtime components over time.
It complements the architecture documentation but does not replace it.
It is not a changelog — it records only completed implementation
milestones, in the order they happened.

---

## Architecture Baseline

Architecture Version:
- architecture-v1.0

Implementation Status:
- Sprint 1 complete — first executable vertical slice achieved

Purpose:
- First executable vertical slice.

---

## Repository Status

Default Branch:
- main

Latest Implementation Commit:
- 4a44abe

Current Android Studio Test Count:
- 234/234 passing

Working Tree:
- Clean

---

## Sprint 1

### Unit 4 – MockTool Fixture

Commit:
308fba8

Completed:
2026-07-04

Android Studio Tests:
146/146

Summary
- Deterministic MockTool fixture.
- Replaced temporary StubTool.
- Added reusable Tool test infrastructure.

Implementation Notes
- Lives in `tests/runtime/`, not `src/runtime/` — a test-only fixture, mirroring the existing `FakePermissionEngine` precedent.
- Supersedes the private `StubTool` inline in Unit 3's own test, which predates this unit and was not written to anticipate it.
- Tracks `validate`/`execute` call counts deterministically; supports simulated failure via the existing `ToolResult`/`ValidationResult` contract shapes, without inventing a new result type.

---

### Unit 5 – Deterministic Planner Harness

Commit:
07055e7

Completed:
2026-07-04

Android Studio Tests:
155/155

Summary
- Added DeterministicPlannerHarness.
- Connected Goal → TaskProposal.
- Deterministic Planning Session lifecycle.

Implementation Notes
- Models only 5 of the Planner Runtime Specification's 10 lifecycle states (`CREATED, CONTEXT_GATHERING, ANALYSING, PROPOSING, SUBMITTED`); `WAITING_FOR_INPUT`, `COMPLETED`, `REJECTED`, `CANCELLED`, and `FAILED` are real, specified states this fixed, always-succeeds harness does not model.
- `PlanCandidate` has no formal schema anywhere in the repository; a minimal, test-only shape (id + goal) is used rather than a speculative general one.
- The harness constructs a well-formed `TaskProposal` but does not call `TaskProposalIntake.submitProposal` — no Task Manager Runtime implementation existed yet at this point.

---

### Unit 6 – Minimal Task Manager Runtime

Commit:
de4d88c

Completed:
2026-07-04

Android Studio Tests:
171/171

Summary
- Introduced Task lifecycle contract.
- Added Task contract.
- Added InMemoryTaskManagerRuntime.
- Connected TaskProposal → Task → AgentRunCommand.

Implementation Notes
- Intake is accept-only: every `TaskProposal` with a resolvable owner is `Accepted`. `Deferred`/`Split`/`Merged` and business-reason `Rejected` are real, specified outcomes not implemented in Sprint 1.
- `TaskId` is derived deterministically from `TaskProposalId` as a documented Sprint 1 placeholder, not a claim about the real allocation scheme.
- Only the proposed owner is resolved through the Identity Service, per the plan's own acceptance text; the proposed assignee is carried through unresolved.
- Re-submitting an already-processed `TaskProposal` throws, rather than inventing a sixth disposition.

---

### Unit 7 – Minimal Agent Runtime

Commit:
890e492

Completed:
2026-07-04

Android Studio Tests:
191/191

Summary
- Introduced Agent Run lifecycle.
- Added AgentRun contract.
- Added InMemoryAgentRuntime.
- Connected AgentRunCommand → ExecutionRequest.

Implementation Notes
- Only `AgentRunCommandType.START` is implemented; `SUSPEND`, `RESUME`, and `CANCEL` are explicitly rejected as not implemented in Sprint 1.
- `WAITING_FOR_PERMISSION`, `WAITING_FOR_INPUT`, and `SUSPENDED` are not driven — the Execution Pipeline call in this codebase is synchronous, so there is nothing asynchronous to pause for.
- A non-`SUCCESS` `ExecutionResult` always ends the Agent Run at `FAILED`; continuing via an alternate proposed action is a real, specified alternative this unit does not implement.
- `AgentRunId` and the Agent Identity's `PrincipalId` are both derived deterministically from `taskId`, the same documented placeholder pattern as Unit 6's `TaskId` derivation.
- `DefaultExecutionPipeline` is called exactly as any other existing caller would call it and is not modified.

---

### Unit 8 – Permission Engine Fixture Verification

Commit:
770c204

Completed:
2026-07-04

Android Studio Tests:
199/199

Summary
- Added standalone FakePermissionEngine tests.
- Verified PermissionEngine fixture independently.
- No production runtime changes.

Implementation Notes
- `FakePermissionEngine` itself is unmodified Phase 2 code; this unit adds the direct, standalone test of its own contract that previously only existed indirectly, through other units' test setup.
- Confirms the existing fixture already satisfies Sprint 1's fixed, always-`APPROVED` happy path without any change.
- Real `PermissionEngine` authorization policy remains unimplemented and out of Sprint 1's scope.

---

### Unit 9 – Runtime Lifecycle Event Publication

Commit:
cfd1b0e

Completed:
2026-07-04

Android Studio Tests:
212/212

Summary
- Added runtime lifecycle event publication.
- Planner, Task Manager and Agent Runtime now publish lifecycle events.
- Completed Sprint 1 runtime observability.

Implementation Notes
- Added EventBus publication to the runtime components without changing their responsibilities.
- Publishes only events corresponding to lifecycle transitions already implemented.
- Uses existing EventBus infrastructure and existing EventType contract.
- Introduced no new lifecycle states or runtime authority.
- Established the event stream consumed by the EventCollector in Unit 10.
- Satisfies AD-009 by making runtime lifecycle transitions observable.

---

### Unit 10 – EventCollector

Commit:
a11def3

Completed:
2026-07-04

Android Studio Tests:
223/223

Summary
- Added EventCollector test harness.
- Records Sprint 1 lifecycle events.
- Enables end-to-end event sequence verification.
- No production runtime changes.

Implementation Notes
- Implemented entirely in `tests/runtime/`; no production runtime code was added or modified.
- Subscribes explicitly to each concrete Sprint 1 `EventType`; wildcard subscription remains intentionally unresolved.
- Collects events in deterministic publish order using the existing `EventBus` interface without publishing or modifying events.
- Provides correlation-based filtering and ordered event inspection for runtime verification.
- Uses a documented Sprint 1 snapshot of known event types rather than introducing a general event catalogue.
- Confirms AD-009 ("Everything Important Is Auditable") through observation rather than altering runtime behaviour.

---

### Unit 11A – ToolInvocationBinding Execution Wiring

Commit:
13c9322

Completed:
2026-07-04

Android Studio Tests:
227/227

Summary
- Wired DefaultExecutionPipeline to ToolInvocationBinding.
- Real Tool.validate() and Tool.execute() now invoked.
- Closed IMPLEMENTATION_GAPS.md #32.

Implementation Notes
- Permission Engine still evaluates before Tool binding.
- SUCCESS now means a Tool actually executed.
- Unbound Tool, failed validation and failed execution all produce FAILED, never DENIED.
- No Planner, Task Manager or Agent Runtime contracts changed.

---

### Unit 11B – Resource Reference Propagation

Commit:
4a44abe

Completed:
2026-07-04

Android Studio Tests:
234/234

Summary
- Added TaskProposal.resourceReferences.
- Propagated ResourceIds through the runtime.
- Completed the first honest end-to-end vertical slice.

Implementation Notes
- contextReferences remain Planner-only and unchanged.
- ResourceIds now flow:
  TaskProposal → AgentRunCommand → ExecutionRequest.
- The vertical slice now reaches real Tool execution without manual patching.
- EventCollector verifies the complete audited execution path.

---

## Sprint 2

### Sprint 2 Unit A1 – Identity-Aware Permission Engine

Commit:
pending

Completed:
2026-07-04

Android Studio Tests:
244/244

Summary
- Added DefaultPermissionEngine.
- IdentityService is consulted before delegated permission decisions.
- Suspended, Revoked, Archived, Created and unresolved Principals are denied before policy evaluation.
- Active Principals delegate unchanged to the supplied decision function.
- Closed IMPLEMENTATION_GAPS.md #40.

Implementation Notes
- No permission policy was introduced.
- Policy remains deferred to Unit A2.
- FakePermissionEngine remains unchanged.
- DefaultExecutionPipeline remains unchanged.

---

### Sprint 2 Unit A2 – Permission Policy Model and Enforcement

Commit:
pending

Completed:
2026-07-05

Android Studio Tests:
253/253

Summary
- Added DefaultPermissionPolicy.
- Added data-defined PermissionPolicyRule support.
- DefaultPermissionEngine now delegates Active-principal requests to DefaultPermissionPolicy after identity-status enforcement.
- Unknown action, unknown resource, unknown permission, and no matching policy rule resolve to DENIED.
- APPROVED_WITH_CONFIRMATION is returned as a policy outcome only; no confirmation UI or workflow was implemented.
- Closed IMPLEMENTATION_GAPS.md #25.

Implementation Notes
- PermissionPolicy.md was implemented without changing its specification.
- Policy evaluation uses ActionMapper and ResourceRegistry to derive the same PermissionAction and ResourceType the Execution Pipeline uses.
- Policy evaluation remains deterministic and side-effect free.
- No RBAC, ABAC, capability security, delegated authority, temporary permissions, persistence, editing UI, organisation policy, or plugin policy was introduced.
- Gap #30's composite-action simplification remains accepted and documented.
- Unit A1 identity gating remains intact.

---

### Sprint 2 Track B Unit B1 – Task Manager Agent-Event Subscription

Commit:
7bbf909

Completed:
2026-07-05

Android Studio Tests:
261/261

Summary
- InMemoryTaskManagerRuntime now subscribes to agent.completed and agent.failed.
- Agent lifecycle events are recorded against the correct Task using taskId.
- Added agentEventsFor(taskId) inspection support mirroring agentRunCommandsFor.
- No Task status transitions were implemented.
- Closed IMPLEMENTATION_GAPS.md #42.

Implementation Notes
- Task Manager remains event-driven only.
- It does not execute Agents.
- It does not alter Task lifecycle.
- It only records Agent lifecycle events.
- agent.cancelled, agent.action_denied and agent.action_deferred remain future work because no production emitters currently exist.
- Task completion and failure semantics remain intentionally deferred to Unit B2.

---

### Sprint 2 Track B Unit B2 – Task Manager Agent-Event Status Transitions

Commit:
pending

Completed:
2026-07-05

Android Studio Tests:
269/269

Summary
- InMemoryTaskManagerRuntime now drives a Task status transition on agent.completed.
- A Task with exactly one Agent Run Reference transitions Queued -> Running -> Completed using existing TaskLifecycleTransitions edges.
- task.started and task.completed are published for the respective edges.
- A Task already Running transitions directly to Completed.
- agent.failed continues to cause no Task status transition; the event is still recorded only.
- Closed IMPLEMENTATION_GAPS.md #42 in full.

Implementation Notes
- No new TaskLifecycleTransitions edge was introduced; both edges already existed.
- Task Manager remains event-driven only.
- It does not call Agent Runtime, ExecutionPipeline, ToolRegistry, or PermissionEngine.
- General Task-completion policy for a Task with more than one Agent Run Reference remains out of scope.
- agent.cancelled, agent.action_denied and agent.action_deferred remain unsubscribed and cause no transition.
- One Unit B1-era test asserting agent.completed left Task status unchanged was removed as obsolete, since Unit B2 deliberately supersedes that behaviour; the scenario is now covered by a test asserting the correct Completed outcome.

---

## Implementation Principles

Sprint 1 follows a strict implementation discipline:

- One architectural unit per commit.
- One review per unit.
- Android Studio is the authoritative compilation and test gate.
- PowerShell is the authoritative Git workflow.
- Deterministic implementations are preferred over speculative behaviour.
- Future architecture is documented rather than implemented early.

---

## Current Vertical Slice

```
Goal
  │
  ▼
Planner
  │
  ▼
TaskProposal
  │
  ▼
Task Manager
  │
  ▼
Task
  │
  ▼
AgentRunCommand
  │
  ▼
Agent Runtime
  │
  ▼
ExecutionRequest
  │
  ▼
Permission Engine
  │
  ▼
Action Mapping
  │
  ▼
Tool Registry
  │
  ▼
ToolInvocationBinding
  │
  ▼
Tool.validate()
  │
  ▼
Tool.execute()
  │
  ▼
ExecutionResult
  │
  ▼
Lifecycle Events
  │
  ▼
EventCollector
```

---

## Current Known Architecture Gaps

- Execution Pipeline access is still enforced by convention rather than construction.
- Planning Session models only the Sprint 1 deterministic lifecycle subset.
- Task Manager implements only the minimal acceptance path.
- Agent Runtime currently supports START only.
- ToolInvocationBinding access remains convention-based rather than construction-enforced.
- Real PermissionEngine policy is not yet implemented.

---

## Current Runtime Chain

As of the latest implementation, the executable runtime path is:

Goal
→ Planner
→ TaskProposal
→ Task Manager
→ Task
→ AgentRunCommand
→ Agent Runtime
→ ExecutionRequest
→ Permission Engine
→ Action Mapping
→ Tool Registry
→ ToolInvocationBinding
→ Tool.validate()
→ Tool.execute()
→ ExecutionResult
→ Lifecycle Events
→ EventCollector

This chain now executes end-to-end under test, from a fixed Goal through
real Tool execution to a verified `SUCCESS` `ExecutionResult`, per Unit
11A (Tool invocation) and Unit 11B (resource reference propagation and
the honest end-to-end proof). Sprint 1's first executable vertical slice
is complete.
