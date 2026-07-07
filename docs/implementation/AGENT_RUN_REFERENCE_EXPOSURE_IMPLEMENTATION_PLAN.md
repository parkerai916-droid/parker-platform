# Agent Run Reference Exposure — Implementation Plan

## Status

**Stage 3 (Implementation Plan), PES-001.** Closes the remaining half of
`docs/architecture/IMPLEMENTATION_GAPS.md` #43 — `task.started`'s
"Agent Run Reference, if any" payload field
(`TaskManagerRuntimeSpecification.md` §10), left deliberately
unpopulated by `docs/implementation/TASK_EVENT_PAYLOAD_COMPLETION_IMPLEMENTATION_PLAN.md`'s
own Section 8 decision, since no authoritative source exposed a real
`AgentRunId` at the time that plan was written.

This is a Level 2 (Behavioural Implementation) unit per PES-001 Chapter
4: it changes existing runtime behaviour but introduces no new public
type, no new interface, and no new lifecycle edge, so no Stage 2A
Contract Design applies — the same reasoning already applied to both
halves of gap #43.

**Readiness.** Confirmed by the architecture-verification-only review
immediately preceding this plan: `AgentRuntimeSpecification.md` Section
9 explicitly delegates every Agent Event's payload schema to
"implementation-phase content," and does not reserve or forbid an
`agentRunId` payload key. That same review found `InMemoryAgentRuntime.kt`
routes every one of its sixteen currently-emitted event types through
one shared `publish(run: AgentRun, eventType: String)` helper
(`src/runtime/InMemoryAgentRuntime.kt`, line 543) — confirmed by
inspecting all 21 call sites — so adding `agentRunId` there once
exposes it uniformly, not as a hand-picked, inconsistent subset. Stage 1
and Stage 2 are therefore satisfied for this unit by that review; this
document is the missing Stage 3 artifact.

**Unit Dashboard**

| Field | Value |
| --- | --- |
| Unit identifier | Agent Run Reference Exposure |
| Closes | `IMPLEMENTATION_GAPS.md` #43, in full (both halves) |
| Governing specifications | `AgentRuntimeSpecification.md` §9; `TaskManagerRuntimeSpecification.md` §10 |
| Engineering level (PES-001 Ch. 4) | Level 2 — Behavioural Implementation |
| Stage 2A required? | No — no new public type or interface |
| Status | Not Started |

---

## 1. Objective

Expose each Agent Run's own `AgentRunId` on every Agent Event
`InMemoryAgentRuntime` publishes, by adding it to the one shared payload
constructor those events already share — and then have
`InMemoryTaskManagerRuntime` read that value off the triggering
`agent.completed` event and thread it into `task.started`'s own payload,
closing the one remaining half of `IMPLEMENTATION_GAPS.md` #43 without
reconstructing the identifier locally or widening Task Manager's
existing subscription boundary.

---

## 2. Scope

1. **`InMemoryAgentRuntime.kt`'s shared `publish(run: AgentRun, eventType: String)` helper**
   (line 543) gains one additional payload entry: `"agentRunId" to
   run.agentRunId.value`, alongside the existing `"taskId" to
   run.taskId.value`. Because every one of this class's sixteen
   currently-emitted event types (`agent.created`, `agent.initialised`,
   `agent.ready`, `agent.started`, `agent.suspended`,
   `agent.step_started`, `agent.action_proposed`,
   `agent.permission_required`, `agent.action_approved`,
   `agent.action_denied`, `agent.action_deferred`, `agent.step_completed`,
   `agent.resumed`, `agent.completed`, `agent.failed`, `agent.cancelled`)
   already calls this one shared helper, this single change exposes
   `agentRunId` consistently across all of them — not `agent.completed`
   alone.
2. **`InMemoryTaskManagerRuntime.kt`'s `applyCompletedTransition`**
   (the handler already subscribed to `agent.completed`, per gap #42's
   established boundary) reads `event.payload["agentRunId"]` from the
   triggering event it already receives as its own `event` parameter,
   and threads it into `task.started`'s payload as `"agentRunId" to
   <value>`, only in the one branch that publishes `task.started`
   (the `TaskStatus.QUEUED` branch). If the incoming event has no
   `agentRunId` entry (e.g. a synthetic test event omitting it), the
   `task.started` payload published is `emptyMap()`, matching §10's own
   "if any" language and this class's established "missing field is
   ignored, not an error" convention (`recordAgentEvent`,
   `applyCompletedTransition`'s own existing `taskId`-missing handling).

---

## 3. Non-Scope

Explicitly excluded from this unit, matching Steven's own instruction:

- `docs/architecture/AgentRuntimeSpecification.md` — not modified.
  Section 9 already permits this change without amendment (Status,
  above); nothing about payload shape is architecture-level content
  this unit would need to change.
- `src/contracts/AgentRun.kt` — not modified. `AgentRun.agentRunId` and
  `AgentRun.correlationId` both already exist with the shapes this unit
  reads from; no field is added, renamed, or reinterpreted.
- **The `correlationId` vs. `AgentRunId` interpretive tension** the
  preceding architecture-verification review surfaced (Section 9's
  prose says `correlationId` is "set to the Agent Run ID"; `AgentRun.kt`'s
  own KDoc documents `correlationId` as the shared, cross-subsystem
  value instead, not literally `AgentRunId.value`) — **is not resolved,
  addressed, or touched by this unit in any way.** `correlationId`'s
  existing behaviour, on every event this unit touches, is completely
  unchanged. See Section 9 of this plan.
- Any new public type, interface, constructor parameter, or lifecycle
  edge — none is introduced anywhere in `src/`.
- Any Agent Event *type* name — all sixteen remain exactly as specified
  in `AgentRuntimeSpecification.md` §9's own table; none is added,
  removed, or renamed.
- Any `AgentRunLifecycleTransitions`, `TaskLifecycleTransitions`, or
  other lifecycle state machine — unchanged.
- Reconstructing `AgentRunId` locally inside
  `InMemoryTaskManagerRuntime` — not done. The value threaded into
  `task.started`'s payload is read directly from the incoming event this
  class already receives, never derived, guessed, or computed from
  `taskId` or any other locally-known value.
- Task Manager's existing event-subscription boundary
  (`IMPLEMENTATION_GAPS.md` #42) — unchanged. This unit adds no new
  `EventBus.subscribe` call; it reads one additional payload key off the
  same `agent.completed` event `applyCompletedTransition` already
  receives as its own handler parameter.
- `task.completed`'s payload, `Task.schema.json`, and every other
  Task Event's payload — unchanged; only `task.started`'s payload
  (the one branch that publishes it) is touched.
- Communication Runtime, the Local Text Channel, Cognition, Planner
  Runtime, Memory Runtime, and World Model — no file belonging to any
  of these is touched.
- `IMPLEMENTATION_GAPS.md` #44, #45, #46, #47, #50, #51, #52's
  own remaining open items — each is separate and not addressed here.

---

## 4. Exact Implementation Files Expected to Change

| File | Change |
| --- | --- |
| `src/runtime/InMemoryAgentRuntime.kt` | Add `"agentRunId" to run.agentRunId.value` to the shared `publish(run, eventType)` helper's payload map (line 543 area). No other line in this file changes. |
| `src/runtime/InMemoryTaskManagerRuntime.kt` | In `applyCompletedTransition`, read `event.payload["agentRunId"]` once, and pass it as `task.started`'s payload (`mapOf("agentRunId" to it)` if present, `emptyMap()` if absent) at the one existing `publish(eventType = "task.started", ...)` call site. No other line in this file changes. |

No other `src/` file changes. In particular:

- `src/contracts/*.kt` — unchanged. No new or modified data type.
- `docs/schemas/*.json` — unchanged.
- Any architecture, ADR, Contract Design, or other Implementation Plan
  document — unchanged.

---

## 5. Exact Tests Expected to Change or Add

**`tests/runtime/InMemoryAgentRuntimeTest.kt`.** No existing test in this
file currently subscribes to the `EventBus` or asserts on any published
event's payload — confirmed by inspection; this file has zero payload
assertions today. This unit adds the first such coverage:

- `buildRuntime`'s private test helper (line 130) must additionally
  return the `InMemoryEventBus` instance it already constructs
  internally (today discarded after being passed to
  `DefaultExecutionPipeline` and `InMemoryAgentRuntime`'s own
  constructors) — a test-infrastructure change, not a production
  change, needed so a test can subscribe before triggering an Agent Run.
- One new test proving `agent.completed`'s published payload contains
  `"agentRunId"` matching the real, returned `AgentRunId` for that run.
- One new test proving at least one *other* event type already exercised
  by this file's existing scenarios (e.g. `agent.created` or
  `agent.started`) also carries the same `"agentRunId"` value — the
  consistency proof the architecture-verification review's own finding
  (Section 3 of this plan) requires, so this unit's own claim ("exposed
  uniformly, not `agent.completed` alone") is itself tested, not merely
  asserted in prose.

**`tests/runtime/InMemoryTaskManagerRuntimeTest.kt`.**

- Extend `` `agent-completed for a QUEUED Task publishes both task-started and task-completed, proving both edges fired` `` (already captures both events' payloads, per the Task Event Payload Completion unit) to construct its synthetic `agent.completed` fixture (the `agentEvent(...)` helper, line ~266) with an `"agentRunId"` payload entry, and assert `task.started`'s published payload now equals `mapOf("agentRunId" to <that value>)`, replacing the current assertion that it is `emptyMap()`.
- Extend the `agentEvent(...)` test fixture itself to accept an optional `agentRunId` parameter (defaulted so every other, unrelated existing test that calls it without one is unaffected).
- Add one new test proving a synthetic `agent.completed` event with **no** `agentRunId` payload entry (mirroring this file's own established "missing field is ignored, not an error" pattern) still produces `task.started`'s payload as `emptyMap()` — proving the "if any" absence path remains safe, not merely the presence path.
- The dedicated deliberate-emptiness test this unit's predecessor added (`` `task-started's payload remains deliberately empty...` ``) is **superseded, not silently deleted** — matching this codebase's own established precedent (e.g. Sprint 2 Unit B2's treatment of an obsolete Unit B1-era test) — replaced by the two tests above, since `task.started`'s payload is no longer always empty. A short comment records why, exactly as this file already does elsewhere for its Unit B1→B2 supersession.

No new test file, no new fixture class, and no fake/mock beyond the
existing `agentEvent(...)` helper's own extended parameter are required.

---

## 6. Acceptance Criteria

1. Every one of `InMemoryAgentRuntime`'s sixteen currently-emitted event
   types carries `"agentRunId"` in its published payload, verified by
   at least two distinct event types in test (Section 5), not asserted
   for `agent.completed` alone.
2. `task.started`'s published payload carries `"agentRunId"` when the
   triggering `agent.completed` event has one, and remains `emptyMap()`
   when it does not — both paths tested.
3. No `AgentRunId` is computed, guessed, or derived anywhere in
   `InMemoryTaskManagerRuntime`; the value used is always read directly
   from the incoming event's own payload.
4. `correlationId`'s value and behaviour, on every event either class
   publishes, is byte-for-byte unchanged.
5. No existing test's assertions are weakened; the one superseded test
   (Section 5) is replaced with an explanatory note, not silently
   deleted, matching this codebase's own established convention.
6. No new public type, interface, constructor parameter, lifecycle edge,
   or `EventBus.subscribe` call is introduced anywhere in `src/`.
7. `AgentRuntimeSpecification.md` and `src/contracts/AgentRun.kt` are
   byte-for-byte unmodified.

---

## 7. Closing `IMPLEMENTATION_GAPS.md` #43 in Full

**Not done by this document** — a Stage 3 Implementation Plan only, per
Steven's own instruction to stop after creating it. Specified here for
whoever verifies implementation:

Once Section 6's acceptance criteria are met and the complete Gradle
test suite passes with no regression, `IMPLEMENTATION_GAPS.md` #43 may
be updated from its current **"Partially resolved -- `task.completed`
closed; `task.started` remains open"** to **fully Resolved**: both
halves §10 names (`task.started`'s Agent Run Reference and
`task.completed`'s Task Result summary) are then implemented and
verified. The update should state plainly that the remaining half was
closed by exposing `agentRunId` on Agent Runtime's own shared event
payload (this plan), not by any of the two paths gap #43's own prior
text had contemplated and rejected (local reconstruction; a
correlationId-based inference) — and should reference this plan by
name, mirroring how the `task.completed` half's own closure was recorded.

---

## 8. Recording in `IMPLEMENTATION_HISTORY.md`

**Not done by this document.** Once tests are verified passing, a new
entry should be added — same discipline as every prior unit this
session: no entry while any result remains an unverified static
projection. That entry should record: both files changed and the exact
payload shapes added (Section 4); that all sixteen Agent Event types now
carry `agentRunId`, not `agent.completed` alone, and why (Section 2/3);
that `task.started`'s payload is now conditionally populated depending
on the triggering event's own content, with the "if any" absence path
also tested; the superseded test and its replacement (Section 5); the
verified test count; and a cross-reference to this plan and to
`IMPLEMENTATION_GAPS.md` #43's resulting **fully Resolved** status
(Section 7). If verified results are not obtainable in whatever
environment performs the implementation, the same disclosed-static-
projection convention already established throughout
`IMPLEMENTATION_HISTORY.md` applies instead.

---

## 9. The `correlationId` vs. `AgentRunId` Wording Tension — Separate, Not Resolved Here

The immediately preceding architecture-verification review found that
`AgentRuntimeSpecification.md` Section 9's prose ("`correlationId` set
to the Agent Run ID") does not match `AgentRun.kt`'s own documented
field provenance (`correlationId` is the shared, cross-subsystem
correlation value threaded from the originating `AgentRunCommand`, the
same convention `TaskProposal`/`ExecutionRequest` already use — not
literally `AgentRunId.value`). **This unit does not resolve, interpret,
or paper over that tension.** It exposes `AgentRunId` through a
different, additive channel entirely — a payload key, not
`correlationId` — leaving the wording question exactly as open as the
architecture-verification review found it. Whether Section 9's prose
should be corrected, clarified, or left as an intentionally loose
description is a separate documentation/specification question for a
future, explicitly-scoped pass, not a byproduct of this Implementation
Plan or its eventual implementation.

---

## Related Documents

- `docs/architecture/IMPLEMENTATION_GAPS.md` (#42, #43)
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md` (§9)
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md` (§10)
- `src/contracts/AgentRun.kt` (read for Section 3's finding; not modified by this plan)
- `docs/implementation/TASK_EVENT_PAYLOAD_COMPLETION_IMPLEMENTATION_PLAN.md` (this unit's direct predecessor; closes the half that plan's own Section 8 left open)
- `src/runtime/InMemoryAgentRuntime.kt`
- `src/runtime/InMemoryTaskManagerRuntime.kt`
- `tests/runtime/InMemoryAgentRuntimeTest.kt`
- `tests/runtime/InMemoryTaskManagerRuntimeTest.kt`

## Governance Statement

This document is a Stage 3 Implementation Plan per PES-001. It does not
implement anything, and does not modify `AgentRuntimeSpecification.md`,
`src/contracts/AgentRun.kt`, any other architecture or contract
document, any other Implementation Plan, `IMPLEMENTATION_GAPS.md`, or
`IMPLEMENTATION_HISTORY.md` — the latter two are addressed only as
forward-looking instructions (Sections 7–8) for whoever verifies
implementation. The `correlationId`/`AgentRunId` wording tension
surfaced by the preceding review is explicitly left open (Section 9),
not resolved by this plan.
