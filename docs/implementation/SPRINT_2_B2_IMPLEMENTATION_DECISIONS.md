# Sprint 2 Unit B2 — Implementation Decisions

Not a specification. Not a review. A pre-coding checklist so Unit B2's own coding turn is mechanical, not interpretive. Every answer below is either already committed in `SPRINT_2_IMPLEMENTATION_PLAN.md`'s frozen (v1.0) Unit B2 scope text, or a direct read of `src/contracts/TaskLifecycle.kt` as it exists today — nothing here is invented.

**1. Exact `TaskStatus` transition on `agent.completed`?**
For a Task with exactly one Agent Run Reference: `QUEUED -> RUNNING`, then `RUNNING -> COMPLETED` — two already-valid edges applied in sequence, not one new edge. `InMemoryTaskManagerRuntime` today only ever drives `CREATED -> QUEUED` (Unit 6); nothing currently moves a Task to `RUNNING`. `TaskLifecycleTransitions` has no `QUEUED -> COMPLETED` edge, only `RUNNING -> COMPLETED`, so both steps are required to stay inside the existing state machine. If a Task is already `RUNNING` when the event arrives, only the second step applies — check current status, don't assume `QUEUED`. Publish `task.started` for the first edge and `task.completed` for the second (both already-specified Task Events, `TaskManagerRuntimeSpecification.md` §10).

**2. Exact transition on `agent.failed`?**
None. Per the Implementation Plan's own frozen Unit B2 scope: the Task "is left unchanged (with the event recorded) for `agent.failed`/`agent.cancelled`/`agent.action_denied`/`agent.action_deferred`." Unit B1 already records the event; Unit B2 adds no status mutation for it. Consequence, stated so it isn't rediscovered mid-unit: a Task can sit at `QUEUED` with a recorded `agent.failed` event and no status-level signal that its Agent Run failed. Accepted, not a bug.

**3. Is retry part of B2 or later?**
Later. Plan text: "Multi-Agent-Run-per-Task coordination and configurable rules remain explicitly out of this unit's scope." Retry means creating a new Task (`TaskManagerRuntimeSpecification.md` §5, "Retryable states"), not reopening this one — not named to any unit yet.

**4. Does every transition already exist?**
Yes. `QUEUED -> RUNNING` and `RUNNING -> COMPLETED` are both already defined in `TaskLifecycleTransitions` (`src/contracts/TaskLifecycle.kt`). No new state or edge is needed — only the sequencing decision in item 1.

**5. Are any architecture decisions required?**
No. Nothing above touches `PermissionEngine`, `IdentityService`, `ExecutionPipeline`, `ToolRegistry`, `AgentRuntime`, or any Architecture Decision. Applying two existing edges in sequence is a Kotlin-level choice inside `InMemoryTaskManagerRuntime`'s own existing `TaskLifecycleTransitions` usage.

**6. Any ambiguities left?**
One, named rather than left implicit: `TaskManagerRuntimeSpecification.md` §6 leaves the *general*, multi-Agent-Run-per-Task rule open ("necessary evidence... but not sufficient by itself if the Task has other, still-incomplete Agent Run References"). That's already out of scope per item 3 and not Unit B2's job to resolve. For Unit B2's own one-Agent-Run-per-Task scope, nothing is open — items 1 and 2 above are complete answers, not placeholders.

**Bottom line:** every answer is settled. Unit B2 coding is a two-edge transition on `agent.completed`, a no-op on `agent.failed`, and two tests proving each — no design decision remains to be made during implementation.
