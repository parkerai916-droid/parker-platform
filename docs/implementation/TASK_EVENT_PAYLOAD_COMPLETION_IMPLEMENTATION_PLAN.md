# Task Event Payload Completion — Implementation Plan

## Status

**Stage 3 (Implementation Plan), PES-001.** Closes exactly one item:
`docs/architecture/IMPLEMENTATION_GAPS.md` #43 — `task.started` and
`task.completed` currently publish with an empty payload map, though
`docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
§10 already specifies required payload content for both. This is a Level
2 (Behavioural Implementation) unit per PES-001 Chapter 4: it changes
existing runtime behaviour but introduces no new public type, no new
interface, and no new lifecycle edge, so no Stage 2A Contract Design
applies (PES-001 Stage 2A's own "Required when" clause: a Contract Design
is required only when a unit "introduces public types it does not
already have approved, field-level Kotlin shapes for" — this unit does
not).

**Readiness.** Stage 1 (Architecture) is already satisfied —
`TaskManagerRuntimeSpecification.md` §10's event table names the exact
required field(s) for both events (Order 2). Stage 2 (Architecture
Review) is already satisfied — this gap was found by the Sprint 2 Health
Review (`docs/reviews/SPRINT_2_B2_POST_IMPLEMENTATION_REVIEW.md`) and
nothing since has contested it. Stage 4 (Implementation Decision) is now
also resolved — see Section 8 — adopting the conservative option: close
the `task.completed` half of gap #43 now, and leave the `task.started`
Agent Run Reference half open rather than reconstruct a hidden
identifier or widen scope into Agent Runtime. **No Kotlin should be
written against this plan until this document is internally
consistent** — this revision is the internal-consistency pass Section 8
required after the decision above was made.

**Unit Dashboard**

| Field | Value |
| --- | --- |
| Unit identifier | Task Event Payload Completion |
| Closes | `IMPLEMENTATION_GAPS.md` #43, in part — see Section 8 |
| Governing specification | `TaskManagerRuntimeSpecification.md` §10 |
| Engineering level (PES-001 Ch. 4) | Level 2 — Behavioural Implementation |
| Stage 2A required? | No — no new public type or interface |
| Implementation Decision (Stage 4) | Decided — Section 8: `task.completed` in scope, `task.started` Agent Run Reference deferred |
| Status | Not Started |

---

## 1. Objective

Bring `task.started` and `task.completed`'s published event payloads
into conformance with `TaskManagerRuntimeSpecification.md` §10's own
event table, which specifies:

- **`TaskStarted` (`task.started`)** — required payload: **"Agent Run
  Reference, if any."**
- **`TaskCompleted` (`task.completed`)** — required payload: **"Task
  Result summary."**

Today, `InMemoryTaskManagerRuntime.applyCompletedTransition`
(`src/runtime/InMemoryTaskManagerRuntime.kt`, lines 332/336/341) calls
its own `publish(...)` helper for both events with no `payload` argument,
which defaults to `emptyMap()` — confirmed by direct inspection of the
current source, not assumed from the gap's own text. This is a
specification-completeness gap only: it does not affect the correctness
of any `TaskStatus` transition (`TaskLifecycleTransitions` and the
transition logic itself are unaffected by event payload content), and it
is not a violation of any Architecture Decision. Closing it makes two
already-specified, already-approved fields observable that are not
observable today.

---

## 2. Exact Files Expected to Change

| File | Change |
| --- | --- |
| `src/runtime/InMemoryTaskManagerRuntime.kt` | Populate the `payload` argument on both `task.completed` publish calls (lines 336, 341) only. The `task.started` publish call (line 332) is **not** changed — per Section 8's decision, it keeps its current, unpopulated `payload` (defaulting to `emptyMap()`). No new method, no new class, no new constructor dependency. |
| `tests/runtime/InMemoryTaskManagerRuntimeTest.kt` | Extend the existing `task.completed` assertions (see the two tests already named in Section 4) to assert on payload contents, not just event-type ordering. Add one new test asserting `task.started`'s payload remains deliberately empty. No new test file. |

No other file changes. In particular:

- `src/contracts/*.kt` — unchanged. No new or modified data type.
- `docs/schemas/*.json`, including `Task.schema.json` — unchanged.
- Any architecture document, ADR, or Contract Design — unchanged.
- `src/runtime/InMemoryAgentRuntime.kt` — unchanged (see Section 8 —
  this is the direct consequence of that constraint, not an oversight).

---

## 3. Payload Fields to Add

**`task.completed` — fully specifiable now, no open question.**
`TaskManagerRuntimeSpecification.md` §4 defines a Task Result as "the
terminal status reached, and references to the `ExecutionResult`s and
Agent Results... produced on the Task's behalf... a summary view over
already-existing results, not a new execution-outcome type." Of these
components, `InMemoryTaskManagerRuntime` owns exactly one directly: the
terminal status itself (`TaskStatus.COMPLETED`, in both call sites this
plan addresses — the class's own KDoc already states no other terminal
status is currently reachable via this path). It does not track
Execution References at all (no such field exists on this class today),
and its only record of Agent-side activity is the raw `agent.completed`/
`agent.failed` events already retained in `agentEvents[taskId]`
(Sprint 2, Unit B1). The minimal, honest summary this Unit can populate
without fabricating data it does not have is:

```kotlin
mapOf(
    "taskId" to taskId.value,
    "status" to TaskStatus.COMPLETED.name,
)
```

This satisfies §10's "Task Result summary" requirement at the level this
class actually has evidence for — a terminal-status summary — without
claiming Execution Reference or Agent Result content this class does not
itself track. Recording those two categories, if ever wanted, is a
separate, larger gap than #43 (this class has no Execution Reference
tracking of any kind today) and is explicitly out of this plan's scope
(Section 6).

**`task.started` — resolved by Section 8: deferred, not populated.**
§10 specifies "Agent Run Reference, if any" for `TaskStarted`. By the
time `task.started` publishes (inside `applyCompletedTransition`,
triggered by a real, externally-received `agent.completed` event), a
real Agent Run has genuinely been created by whatever `AgentRuntime`
instance is wired to the same `EventBus` — but `InMemoryTaskManagerRuntime`
itself has no field carrying that Agent Run's identifier. Confirmed by
direct inspection: `InMemoryAgentRuntime.publish` (`src/runtime/InMemoryAgentRuntime.kt`,
line 551) sends `agent.completed` with `payload = mapOf("taskId" to
run.taskId.value)` only — no `agentRunId` entry. `InMemoryTaskManagerRuntime`
never calls `AgentRunCommandChannel.submit` and never receives an
`AgentRunId` back (documented in its own class KDoc: "Constructs, but
never submits, an `AgentRunCommand`"). There is therefore no field
already available inside `InMemoryTaskManagerRuntime` today that holds a
genuine Agent Run Reference.

**Decided (Section 8): deferred, not populated, by this unit.** Two
options were identified while drafting this plan — reconstructing
`AgentRunId("run-for-${taskId.value}")` locally from the already-documented
deterministic minting scheme (`IMPLEMENTATION_GAPS.md` #48,
`docs/architecture/PRE_MODULE_ID_MULTIPLICITY_DECISION.md`), or extending
`InMemoryAgentRuntime.publish`'s own payload to carry `agentRunId`
directly. **Both are explicitly rejected for this unit**, per Steven's
own instruction: the first would infer a hidden identifier by duplicating
another subsystem's internal ID-minting scheme rather than reading it
from that subsystem's own output — a real coupling risk if that scheme
ever changes, silently, with no compiler error or test pointing at the
cause; the second requires modifying `InMemoryAgentRuntime.kt`, which is
explicitly out of this unit's scope (Section 6). `task.started`'s payload
therefore remains exactly as it is today — `emptyMap()` — and this unit
does not attempt to populate an Agent Run Reference. `TaskStarted`'s own
required field is phrased "Agent Run Reference, **if any**," so this
absence is within the specification's own contemplated range, not a
violation of it — but it is *not* claimed as closed; see Section 8 for
exactly what this leaves open in `IMPLEMENTATION_GAPS.md` #43.

---

## 4. Testing Strategy

Extend, not replace, the two existing tests in
`tests/runtime/InMemoryTaskManagerRuntimeTest.kt` that already assert on
`task.started`/`task.completed` publication order:

- `` `agent-completed for a QUEUED Task publishes both task-started and task-completed, proving both edges fired` `` (currently asserts only event-type ordering) — extend to also assert `task.completed`'s payload contains `"status" to "COMPLETED"` and `"taskId" to <the Task's own id>`. Per Section 8's decision, this test does **not** assert any populated content for `task.started`'s payload — only that it remains an empty map, asserted deliberately (see below), not left untested.
- `` `agent-completed transitions an already-RUNNING Task to COMPLETED, taking only the second edge` `` (currently asserts only that `task.completed` is the sole event published) — extend to also assert `task.completed`'s payload contents, identically to the test above.

New assertions only — no existing assertion in either test is weakened
or removed. Two additional tests are recommended, mirroring this
codebase's own established "un-populated case" testing pattern (e.g.
`InMemoryModuleRegistryTest`'s treatment of empty `toolsExposed`):

- A dedicated test asserting `task.completed`'s payload never claims an
  Execution Reference or Agent Result field this class does not track
  (a scope-discipline test, mirroring `DefaultLocalTextChannelTest`'s
  own "depends on only..." structural-proof pattern, adapted here to a
  payload-content proof instead of a constructor-shape proof).
- A dedicated test asserting `task.started`'s payload is deliberately
  empty — proving, by test, that the Agent Run Reference's absence is an
  intentional, documented deferral (Section 8) and not an oversight this
  unit failed to notice.

No fixture, no fake, and no new test file are required — both call
sites already run end-to-end through `InMemoryTaskManagerRuntime`'s own
existing test harness (a real `InMemoryIdentityService` and a real
`InMemoryEventBus`, per the existing test class's own setup).

---

## 5. Acceptance Criteria

1. `task.completed`'s published payload contains `"status"` (the
   terminal `TaskStatus` reached) and `"taskId"`, at both call sites
   (the `QUEUED` path and the already-`RUNNING` path).
2. `task.started`'s published payload remains an empty map, per
   Section 8's decision, and this absence is asserted by a dedicated
   test (Section 4) — not a silent, untested empty map, and not
   populated by reconstructing `AgentRunId` locally or by any other
   inferred identifier.
3. No existing test's assertions are weakened; only new assertions are
   added.
4. No `TaskStatus` transition, no `TaskLifecycleTransitions` edge, and
   no event *type* (`task.started`/`task.completed` continue to fire on
   exactly the same conditions as today) changes as a result of this
   unit.
5. No new public type, interface, constructor parameter, or lifecycle
   edge is introduced anywhere in `src/`.
6. `InMemoryAgentRuntime.kt` and every other file outside Section 2's
   list is unmodified.

---

## 6. Non-Scope

Explicitly excluded from this unit, matching Steven's own instruction
and this plan's own findings above:

- Communication Runtime, the Local Text Channel, Cognition, Planner
  Runtime, Agent Runtime, Memory Runtime, and World Model — no file
  belonging to any of these is touched.
- Promoting "Task Result" to a real, structured `Task.schema.json` field
  — `TaskManagerRuntimeSpecification.md`'s own "Open Questions" section
  explicitly leaves this undecided, subject to ADR-019, and this unit
  does not decide it. The payload populated here remains an
  informational `Map<String, String>` entry, exactly as every other
  Task Event's payload already is.
- Tracking Execution References or Agent Results as first-class state on
  `InMemoryTaskManagerRuntime` — not attempted; the `task.completed`
  summary in Section 3 is honestly scoped to what this class already
  tracks (terminal status), not to the full Task Result concept
  `TaskManagerRuntimeSpecification.md` §4 describes.
- Any of the other four "open, pending implementation" gaps
  (`IMPLEMENTATION_GAPS.md` #44, #45, #46, #47) — each requires its own,
  separate human decision and is not addressed here.
- Any change to `agent.completed`'s or `agent.failed`'s own published
  payload (that file belongs to `InMemoryAgentRuntime.kt`, out of
  scope).
- **Reconstructing `AgentRunId` locally.** Explicitly rejected, not
  merely deferred — see Section 8. `InMemoryTaskManagerRuntime` does not
  compute, guess, or derive any form of `AgentRunId` value anywhere in
  this unit, deterministic scheme or otherwise.
- **Any other inference of a hidden or unexposed identifier** as a
  substitute for an authoritative source this class does not have.

---

## 7. Completion Criteria

This unit is complete when:

1. `InMemoryTaskManagerRuntime.kt`'s `task.completed` `publish(...)` call
   sites (Section 2, lines 336 and 341) pass a populated `payload`
   matching Section 3's resolved shape. The `task.started` call site
   (line 332) is deliberately left unchanged, per Section 8's decision —
   its `payload` argument remains omitted (`emptyMap()`), not populated.
2. `InMemoryTaskManagerRuntimeTest.kt`'s extended and new assertions
   (Section 4) all pass.
3. The complete Gradle test suite passes with no regression to any
   previously-passing test.
4. Sections 8 and 9 below have been acted on as this plan specifies.

---

## 8. Required Implementation Decision

**Mirrors this repository's own IDR-001 precedent** (`docs/implementation/LOCAL_TEXT_CHANNEL_IMPLEMENTATION_PLAN.md`
Section 4) — a narrow, disclosed decision resolved before Stage 5 (Scope
Lock), not a redesign. Unlike IDR-001, this decision is now **resolved**,
not left open — recorded below exactly as decided.

> **Subject:** How `task.started`'s "Agent Run Reference, if any"
> payload field is populated, given that `InMemoryTaskManagerRuntime`
> has no field of its own carrying a real `AgentRunId` today (Section 3).
>
> **Decision:** Close the `task.completed` payload defect now. Leave the
> `task.started` Agent Run Reference portion of gap #43 open.
>
> **Rationale:**
> - `task.completed`'s payload (Section 3) can be populated entirely
>   within `InMemoryTaskManagerRuntime` — the terminal status and
>   `taskId` are both already owned by this class, with no dependency on
>   any other subsystem.
> - `task.started`'s Agent Run Reference cannot be fully populated
>   without either reconstructing `AgentRunId` locally (inferring a
>   hidden identifier by duplicating another subsystem's internal
>   minting scheme) or modifying Agent Runtime (`InMemoryAgentRuntime.kt`,
>   to add `agentRunId` to `agent.completed`'s payload) — both exceed
>   this unit's scope.
> - Partial closure is safer than silently introducing cross-runtime
>   coupling: this unit implements exactly what it can verify against an
>   authoritative source it already owns, and defers the rest honestly
>   rather than inferring a value it cannot attest to.
>
> **Status:** Decided. Not open. Does not block Stage 5 (Scope Lock) —
> the decision itself removes the block Section 3 originally raised.

**What this means for `IMPLEMENTATION_GAPS.md` #43 — not done by this
document, but specified for whoever verifies implementation:**

- `IMPLEMENTATION_GAPS.md` #43 **must not be marked Resolved or closed**
  after this unit, under any circumstance.
- It **may only be clarified** — once `task.completed`'s payload is
  implemented, tested, and verified per Sections 5 and 7 — to state that
  the `task.completed`/Task Result summary half of #43 is complete,
  while the `task.started`/Agent Run Reference half remains **open**,
  exactly as this plan leaves it. No rewording of #43 may imply the
  `task.started` half was addressed, resolved, deferred-and-forgotten,
  or otherwise closed by this unit.

**`IMPLEMENTATION_HISTORY.md` may be updated after tests pass** — see
Section 9 for exactly what that entry should say and the same
verified-before-recorded discipline this session has applied throughout.

**No Kotlin should be written against this plan until it is internally
consistent** with the decision recorded above. This revision *is* that
consistency pass: Sections 2 through 7 above have each been updated to
match this decision (only the `task.completed` call sites change; the
`task.started` call site and `InMemoryAgentRuntime.kt` are both
untouched; acceptance and completion criteria no longer describe two
open options). Stage 6 (Implementation) may begin only once Steven
confirms this document, as revised, is ready.

---

## 9. IMPLEMENTATION_HISTORY.md

**May be updated after tests pass — but only after**, mirroring the same
discipline this session already applied to Sprint 7 Unit C3: no entry
should be added while any test result remains an unverified static
projection. Once the complete Gradle test suite is run and every test
(existing and new) is confirmed passing, a new `IMPLEMENTATION_HISTORY.md`
entry should record: Section 8's decision and rationale; the exact
payload shape implemented for `task.completed`; that `task.started`'s
payload was deliberately left unpopulated, and why; the extended/new
test names and their pass status; and a cross-reference to this plan and
to `IMPLEMENTATION_GAPS.md` #43's resulting clarified (not closed)
status, per Section 8. If verified test results are not obtainable in
whatever environment performs the implementation (as was the case for
Sprint 7 Unit C3 in this session's own sandbox), the same
disclosed-static-projection convention already established throughout
`IMPLEMENTATION_HISTORY.md` should be used instead of a false "verified"
claim.

---

## Related Documents

- `docs/architecture/IMPLEMENTATION_GAPS.md` (#43, #48)
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md` (§4, §10)
- `docs/architecture/PRE_MODULE_ID_MULTIPLICITY_DECISION.md`
- `docs/reviews/SPRINT_2_B2_POST_IMPLEMENTATION_REVIEW.md`
- `docs/implementation/SPRINT_2_B2_IMPLEMENTATION_DECISIONS.md`
- `docs/implementation/LOCAL_TEXT_CHANNEL_IMPLEMENTATION_PLAN.md` (IDR-001 precedent for this plan's Section 8 structure)
- `src/runtime/InMemoryTaskManagerRuntime.kt`
- `src/runtime/InMemoryAgentRuntime.kt` (read for Section 3's finding; not modified by this plan)
- `tests/runtime/InMemoryTaskManagerRuntimeTest.kt`

## Governance Statement

This document is a Stage 3 Implementation Plan per PES-001. It does not
implement anything, does not modify any architecture document, ADR,
Contract Design, or existing specification, and does not modify
`IMPLEMENTATION_GAPS.md` or `IMPLEMENTATION_HISTORY.md` — both are
addressed only as forward-looking, explicit instructions (Section 8–9)
for whoever verifies implementation, consistent with Steven's own
instruction to stop after this document. Section 8's Required
Implementation Decision is now resolved (the conservative option:
`task.completed` in scope now, `task.started`'s Agent Run Reference
deferred, no local `AgentRunId` reconstruction, no Agent Runtime
change) — this revision is the internal-consistency pass that decision
required before Stage 6 (Implementation) may begin.
