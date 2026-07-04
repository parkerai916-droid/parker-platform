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
- Sprint 1 in progress

Purpose:
- First executable vertical slice.

---

## Repository Status

Default Branch:
- main

Latest Implementation Commit:
- 770c204

Current Android Studio Test Count:
- 199/199 passing

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
Execution Pipeline
```

---

## Current Known Architecture Gaps

- Execution Pipeline access is still enforced by convention rather than construction.
- Planning Session models only the Sprint 1 deterministic lifecycle subset.
- Task Manager implements only the minimal acceptance path.
- Agent Runtime currently supports START only.
- DefaultExecutionPipeline is not yet wired to ToolInvocationBinding.
- Real PermissionEngine policy is not yet implemented.

---

## Current Runtime Chain

As of the latest implementation, the executable runtime path is:

Planner
→ Task Proposal
→ Task Manager
→ Task
→ Agent Run Command
→ Agent Runtime
→ Execution Request
→ Permission Engine
→ Execution Pipeline

Tool execution, EventBus publication, and full runtime orchestration
remain intentionally deferred to later Sprint 1 units.
