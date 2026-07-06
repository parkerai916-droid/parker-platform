# Sprint 3 Track C Unit C2 — Post-Implementation Review

## Status

Unit:
Sprint 3, Track C, Unit C2 — Multi-Step Agent Run Implementation

Design basis:
`docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md` (Unit C1, commit
`affc46a`, reviewed and accepted before this unit began)

Commit:
pending (this review is written before commit, per PES-001 — see the
Process Note below)

Android Studio:
Not yet run by a human in Android Studio. Static count only:
`InMemoryAgentRuntimeTest.kt` went from 12 tests to 26 (net +14 against
the prior suite total of 269/269); if every test passes unchanged, the
expected total is 283/283. This figure is an arithmetic projection from
the source, not a verified run.

Date reviewed:
2026-07-06

Performed **before** commit, unlike Sprint 2 Unit B2's retroactive review
(`docs/reviews/SPRINT_2_B2_POST_IMPLEMENTATION_REVIEW.md` §6) — Unit C2 is
also a Level 2 (Behavioural Implementation) unit under PES-001
(`docs/architecture/PARKER_ENGINEERING_STANDARD.md`) Chapter 4, and this
time the required Post-Implementation Review is being written as part of
Engineering Validation (Stage 9), before Stage 10/11 (commit, human
approval), rather than after. No process finding of the B2 kind applies
here.

---

## 1. Is the implementation correct?

Yes, against `MULTI_STEP_AGENT_RUN_DESIGN.md`'s own Section 11 "WILL
implement" list, checked item by item.

`src/contracts/AgentStep.kt` adds exactly the four types Section 4.2/4.3
specify: `AgentStepContext`, `AgentStepDecision` (`Propose`/`Complete`/
`Fail`, no fourth variant), `AgentStepSource`, `AgentPolicy`
(`maxAgentSteps`, `maxAgentRunDuration`). No field or variant beyond what
the design document names was added.

`src/runtime/InMemoryAgentRuntime.kt`'s `runLoop` drives the per-step
sequence Section 5.1 specifies: `agent.step_started` published, then
`AgentStepSource.nextStep` consulted, then branching on the decision.
`Propose` submits exactly one `ExecutionRequest` per step via
`ExecutionPipeline.submit`, unchanged from Unit 7's own construction
pattern. `Complete` is honoured only when `state.successfulSteps >= 1`
(`InMemoryAgentRuntime.kt`'s `runLoop`, the `AgentStepDecision.Complete`
branch); a `Complete` decision reached with zero prior successful steps is
treated as `Fail`, exactly matching Section 5.1 item 4 and
`AgentStepDecision.Complete`'s own KDoc. `DENIED` ends the Agent Run at
`FAILED`; `DEFERRED` ends the current step at `SUSPENDED` — these are now
two distinct branches (`ExecutionResultStatus.DENIED` vs
`ExecutionResultStatus.DEFERRED` in the `when` block), not the single
collapsed `FAILED` outcome Unit 7 produced for both. `AgentPolicy.maxAgentSteps`
is checked at the top of each loop iteration, before consulting
`AgentStepSource` again; reaching it transitions `RUNNING -> SUSPENDED`,
never `FAILED`, confirmed by the dedicated
`reaching AgentPolicy maxAgentSteps... ends... at SUSPENDED, not FAILED`
test. `SUSPEND` sets a `pendingSuspend` flag consulted only at the next
step boundary (`suspendRun`, then `runLoop`'s own top-of-loop check) —
confirmed by the `an explicit SUSPEND is honoured at the next step
boundary, not mid-step` test, which proves the second `AgentStepSource`
consultation never happens. `RESUME` transitions `SUSPENDED -> RUNNING`
and continues `runLoop` from `state.successfulSteps + 1`, not step 1 —
confirmed by the `RESUME... continues with the next step number, not
resetting to 1` test, which asserts the exact sequence `[1, 2]` seen by
`AgentStepContext.stepNumber`. `CANCEL` applies immediately from any
non-terminal state (`cancelRun`'s `AgentRunLifecycleTransitions.isTerminal`
check, not restricted to any specific status), followed by a best-effort
`ExecutionPipeline.cancel` call — confirmed by three dedicated tests
(`CANCEL` from `CREATED`, from `SUSPENDED`, and mid-step via
`ControllableTool`) plus a fourth proving `CANCEL` from a terminal state
is rejected.

The locking model redesign (design document §8) is implemented as
specified: `mutex.withLock` blocks in `InMemoryAgentRuntime.kt` are all
short, synchronous map operations; `executionPipeline.submit(...)` (the
one call in the loop that may suspend for an unbounded time) is made with
no lock held. This is proven, not merely asserted, by the `a slow Agent
Step in one Agent Run does not block a command for a different,
independent Agent Run` test — which would hang until timeout under the
old (Unit 7-derived) `mutex.withLock { when(...) }`-wraps-everything
structure, since a second `submit()` call would contend for the same
`Mutex` object held for the whole first call's duration.

## 2. Are runtime boundaries preserved?

Yes. `InMemoryAgentRuntime`'s constructor dependencies are unchanged in
kind (`IdentityService`, `ExecutionPipeline`, `EventBus`), with two
additions the design document itself specifies (`AgentStepSource`,
`AgentPolicy`) — no new dependency on `Task Manager Runtime`,
`PermissionEngine`, `ToolRegistry`, or `ToolInvocationBinding` was
introduced; those remain reached only indirectly, through
`ExecutionPipeline.submit`, exactly as Unit 7 already established. AD-006
(Agent Runtime Never Owns Tasks) is unaffected — this unit does not touch
`InMemoryTaskManagerRuntime` at all. AD-007 (Permission Decisions Belong
to the Permission Engine) is unaffected — every `Propose`d action is still
independently evaluated by `PermissionEngine.evaluate` via
`ExecutionPipeline.submit`; no `AgentStepDecision` variant grants,
bypasses, or pre-approves anything (`AgentStep.kt`'s own KDoc states this
explicitly). `ExecutionPipeline`, `EventBus`, and `AgentRunCommandChannel`
interfaces are all unmodified — `InMemoryAgentRuntime` remains their only
implementation, unchanged in shape. `AgentRunLifecycleTransitions`
(`src/contracts/AgentRunLifecycle.kt`) was not modified: no new
`AgentRunStatus` value, no new transition edge. This unit simply drives
more of the already-specified 10-state machine than Unit 7 did.

## 3. Are tests sufficient?

Sufficient against the explicit list of scenarios required for this unit:
multi-step success (`a multi-step Agent Run submits one ExecutionRequest
per Propose...`); `Complete` after ≥1 successful step (the happy-path
test, and the multi-step test); `Complete` before any successful step
(`a Complete decision before any Agent Step has succeeded ends... at
FAILED`); `DENIED` vs `DEFERRED` as distinct outcomes (two dedicated
tests); `maxAgentSteps` causing `SUSPENDED` not `FAILED`; `SUSPEND` at a
step boundary; `RESUME` continuing with the next step number; `CANCEL`
from several non-terminal states (`CREATED`, `SUSPENDED`, and mid-step via
`ControllableTool`) and rejection from a terminal state; and a
concurrency proof that `ExecutionPipeline.submit` is not called while the
Agent Runtime's own mutex would otherwise block an unrelated command. All
12 of Sprint 1 Unit 7's original tests are preserved in effect — ten
unchanged in spirit (their `buildRuntime` call sites were updated only for
the new constructor parameters), two ("SUSPEND is Rejected as not
implemented", "RESUME and CANCEL are also Rejected as not implemented")
removed as no longer true and replaced by the dedicated `SUSPEND`/`RESUME`/
`CANCEL` sections, mirroring exactly how Unit B2 superseded an obsolete
Unit B1-era test rather than silently deleting it.

Two new test-only fixtures were added: `FakeAgentStepSource`
(`tests/runtime/FakeAgentStepSource.kt`, mirroring `FakePermissionEngine`'s
lambda-based fake precedent) and `ControllableTool`
(`tests/runtime/ControllableTool.kt`, mirroring `MockTool`'s "test-only
fixture, not `src/runtime`" precedent) — the latter needed because
`MockTool`'s immediately-returning `execute()` cannot exercise either
concurrency property this unit's design requires proof of.

One limitation, disclosed rather than hidden: the "SUSPEND is rejected
while momentarily WAITING_FOR_PERMISSION" edge (see §5 below) has no
dedicated test — it is a narrow race window not named in this unit's
required test list, and is instead documented as a recorded
implementation decision in `IMPLEMENTATION_HISTORY.md`'s Unit C2 entry.
Android Studio itself has not yet been run against this suite by a human
(see Status above); this review's test-sufficiency finding is a static
read of the test source, not a confirmed green run.

## 4. Is documentation complete?

Yes, with two additions beyond the minimum this unit's own instructions
required. `docs/implementation/IMPLEMENTATION_HISTORY.md` gained both a
Unit C2 entry and a backfilled Unit C1 entry (Unit C1's design-only work
had no chronological entry yet; adding one alongside C2's own entry avoids
a gap in Sprint 3's own record — no C1 content, commit, or tag was
altered in doing so, only a new history entry describing what was already
true). `docs/architecture/IMPLEMENTATION_GAPS.md` was updated in two
places, per this unit's "only if required" instruction: a correction to
gap #42's text (its claim that "no production code emits `agent.cancelled`/
`agent.action_denied`/`agent.action_deferred`" is now stale — Unit C2's
`InMemoryAgentRuntime` does emit all three — though the gap's own status,
Task Manager Runtime not subscribing to them, is unchanged and still
open, since Unit C2 does not touch Task Manager Runtime), and a new gap
(#44) for a previously-latent limitation this unit's own honest `CANCEL`
handling surfaces (see §5 and §8 below). This document (the Post-
Implementation Review itself) is the third deliverable this unit's
instructions named.

## 5. Does implementation comply with approved specifications?

Yes, against the design document's own priority order (Constitution → AD
→ PES-001 → Governance → Sprint 2 Plan → existing specs), with two
implementation choices the design document left open, both recorded
rather than silently decided:

- **`SUSPEND` acceptance window.** The design document specifies `SUSPEND`
  is "honoured at the next step boundary" but does not specify whether a
  `SUSPEND` arriving while a run is momentarily `WAITING_FOR_PERMISSION`
  (i.e. an `ExecutionRequest` for the current step is already submitted)
  should be accepted-and-queued or rejected. This implementation rejects
  it — `suspendRun` only records a pending suspend while the stored status
  is exactly `RUNNING`. A caller whose `SUSPEND` lands in that narrow
  window receives `Rejected` and would need to retry once the run returns
  to `RUNNING` (which happens automatically after a successful step, or
  the run ends up terminal/`SUSPENDED` on its own from that step's
  outcome). This avoids a stale `pendingSuspend` flag surviving past a
  `SUSPENDED` state a `DEFERRED` result already produced independently,
  at the cost of a small caller-visible rejection window.
- **`RESUME` and an exhausted `maxAgentSteps` budget.** The design
  document does not specify whether `RESUME` should re-check
  `AgentPolicy.maxAgentSteps` specially. This implementation does not
  special-case it: `RESUME` always transitions `SUSPENDED -> RUNNING`,
  and `runLoop`'s own existing budget check (identical code, no bypass)
  applies at the top of the very next iteration. If the run was suspended
  because the budget was already exhausted, `RESUME` will transition it
  back to `RUNNING` and then immediately back to `SUSPENDED` again,
  without attempting a further step — a "bounce," not an error, and not a
  silent budget override.

Every "will NOT implement" item in Section 11 was confirmed absent:
`agentStepSource` is an injected interface with no Planner behind it in
this unit; no `WAITING_FOR_INPUT` transition exists anywhere in
`InMemoryAgentRuntime.kt`; no Agent Capability type or check was added;
`AgentPolicy.maxAgentRunDuration` is accepted by the data class but never
read by `InMemoryAgentRuntime`; no World Model, Memory, Workflow Engine,
or cross-Agent orchestration reference exists anywhere in the new code.

## 6. Process Note

Unlike Sprint 2 Unit B2, this review was written before this unit's own
commit, as part of Engineering Validation (PES-001 Stage 9), ahead of
Stage 10/11 (human commit and approval) — closing the sequencing gap §6
of the B2 review flagged, on this unit rather than only recommending it
for a future one.

## 7. Was the Architecture Validated?

Unit C2 implemented, rather than changed, the already-accepted Unit C1
design. No Architecture Decision (AD-003 Single Execution Pipeline, AD-006
Agent Runtime Never Owns Tasks, AD-007 Permission Decisions Belong to the
Permission Engine, AD-009 Everything Important Is Auditable, AD-010 Model
Independence, AD-011 Context Is Reference-Based) required revision.
`AgentRunLifecycleTransitions`'s already-specified 10-state machine is now
driven more fully (`WAITING_FOR_PERMISSION`, `SUSPENDED`, and cancellation
from more states) but not altered. The one open architectural question
Unit C1's own design document flagged as out of scope — a real Planner
behind `AgentStepSource` — remains open, correctly not decided here.

## 8. Remaining Gaps

- `IMPLEMENTATION_GAPS.md` #42 — Task Manager Runtime still does not
  subscribe to `agent.cancelled`, `agent.action_denied`, or
  `agent.action_deferred`; these are now genuinely emitted (this unit's
  own change), but consuming them remains explicitly out of this unit's
  scope (Task Manager Runtime is not modified).
- `IMPLEMENTATION_GAPS.md` #44 (new, logged alongside this review) —
  `ExecutionPipeline.cancel` cannot interrupt a `Tool.execute()` call
  already `EXECUTING`; this unit's `cancelRun` calls it on a best-effort
  basis and does not depend on it succeeding, but the underlying
  architectural gap (no cooperative-cancellation contract for `Tool`) is
  real and previously undocumented.
- The two implementation decisions recorded in §5 above (`SUSPEND`'s
  narrow rejection window; `RESUME`'s non-special-cased budget re-check)
  are documented choices, not defects, but are genuinely open design
  questions a future unit could revisit.
- No Planner exists behind `AgentStepSource` — unchanged from Unit C1's
  own explicitly out-of-scope item.
- Android Studio has not yet been run against this suite by a human; the
  283/283 figure in this review's Status section is a static projection,
  not a confirmed result.

## 9. Decision

**Proceed unchanged.** The implementation matches the accepted Unit C1
design in full, adds nothing from its "will NOT implement" list, modifies
no interface or specification this unit was instructed not to touch, and
its two open implementation choices are recorded rather than hidden. This
unit is complete pending a human running the test suite in Android Studio
and, if it passes, committing — no further design or implementation work
is recommended before that commit.
