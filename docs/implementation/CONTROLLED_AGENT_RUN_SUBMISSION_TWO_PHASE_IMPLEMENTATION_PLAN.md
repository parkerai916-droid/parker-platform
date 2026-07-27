# Controlled Agent Run Submission — Two-Phase Implementation Plan

**Status:** Mechanical implementation plan only. No source file modified. No
governance document further amended. Nothing staged, committed, or pushed.
**Implementation may begin only after this plan, the Contract Design
Amendment 1, and the Scope Lock Amendment have all been explicitly
approved.**

**Authoritative inputs:**
- `docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_CONTRACT_DESIGN.md`, Amendment 1
- `docs/implementation/CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md`, Amendment (Section A.1-A.8)
- `docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_ACCEPTANCE_EXECUTION_SEPARATION_RECONCILIATION.md`

This plan does not make any new design decision. Every item below is a
mechanical restatement of what the two amendments above already froze,
organized for execution.

---

## 1. Exact production files

| File | Change |
|---|---|
| `src/contracts/AgentRunCommand.kt` | Add `interface AgentRunExecutionTrigger { suspend fun execute(agentRunId: AgentRunId) }` alongside the existing `AgentRunCommandChannel` interface. No change to `AgentRunCommandChannel`'s own signature or to `AgentRunCommandResult`'s shape — only its `Accepted` variant's documented KDoc meaning changes (Contract Design Amendment 1, A1.1). |
| `src/runtime/InMemoryAgentRuntime.kt` | Split `start(command)` at the point immediately after `READY` is reached (current `agent.ready` publish, ~line 273) — **not** after `RUNNING`/`agent.started` (Lifecycle State Correction, Contract Design Amendment 1 §A1.8). Everything up to and including `agent.ready`, plus the existing `mutex.withLock { runStates[agentRunId] = RunState() }` line, remains in `start()`; `start()` returns `Accepted(agentRunId, command.commandType)` there — **before** the existing `READY -> RUNNING` `advanceInitial` call and **before** the existing `agent.started` publish, both of which move into the new method. A new method, `override suspend fun execute(agentRunId: AgentRunId)`, implementing `AgentRunExecutionTrigger`, looks up the existing `AgentRun`/`RunState` for `agentRunId`, performs `advanceInitial(run, AgentRunStatus.RUNNING)`, publishes `agent.started`, then calls the existing, unmodified `runLoop(...)` with the same arguments `start()` used to pass it directly. `runLoop()`'s own body is not touched. Class declaration gains `, AgentRunExecutionTrigger` alongside its existing `AgentRunCommandChannel`. |
| `src/runtime/InMemoryTaskManagerRuntime.kt` | Constructor gains `agentRunExecutionTrigger: AgentRunExecutionTrigger` as a fourth parameter. `submitProposal()`'s single `mutex.withLock { ... }` block is split: everything through the `Accepted`/`Rejected` branch's own publish-and-transition bookkeeping stays inside the lock; the lock block ends there. Immediately after (outside the lock), `if (result is AgentRunCommandResult.Accepted) agentRunExecutionTrigger.execute(result.agentRunId)` is called before the function returns `TaskProposalDisposition.Accepted(...)`. |
| `src/composition/ParkerRuntime.kt` | `InMemoryTaskManagerRuntime(...)`'s construction call gains a fourth argument, `agentRunExecutionTrigger = agentRuntime` — the same `InMemoryAgentRuntime` instance already passed as `agentRunCommandChannel`. No new object constructed, no reordering beyond what is already frozen (Contract Design Section 9). |

No other production file changes. `runLoop()`, `tryAdvance`, `advanceInitial`, `suspendRun`, `resumeRun`, `cancelRun`, the run-initiation permission block, the duplicate-START check, and every per-action Trust-gating call site are untouched.

---

## 2. Exact contract files

Covered by §1's first row — `src/contracts/AgentRunCommand.kt` is both the one contract file touched and the one production file touched for the new interface (Scope Lock Amendment A.5: "no new file... declared alongside `AgentRunCommandChannel`"). `AgentRunId` (already defined in `src/contracts/`) is reused unchanged as the new interface's sole parameter type.

---

## 3. Exact tests

### 3.1 Signature-only updates (constructor call sites, no behavioral change needed)

`InMemoryTaskManagerRuntime(...)` gains a fourth argument at every existing
call site. All of the following already pass a fake or real
`AgentRunCommandChannel` as the third argument from the prior corrective
pass; each needs one more argument, a fake `AgentRunExecutionTrigger` (a
no-op or call-counting fake, narrowest per file, mirroring the existing
`RejectingAgentRunCommandChannel`/`PlannerRejectingAgentRunCommandChannel`/
`VerticalSliceRejectingAgentRunCommandChannel` per-file-local convention):

- `tests/runtime/InMemoryTaskManagerRuntimeTest.kt` — ~27 call sites, one mechanical addition each.
- `tests/runtime/InMemoryPlannerRuntimeTest.kt` — 18 identical call sites, one `replace_all`.
- `tests/runtime/RuntimeLifecycleEventPublicationTest.kt` — 4 call sites.
- `tests/runtime/EventCollectorTest.kt` — 1 call site.
- `tests/runtime/VerticalSliceEndToEndTest.kt` — 1 call site.
- `tests/runtime/TaskManagerAgentRunSubmissionIntegrationTest.kt` — 1 call site, passing the same real `agentRuntime` instance for both arguments (mirrors production).

`InMemoryAgentRuntime(...)`'s constructor is unchanged in arity (still eight
parameters, Scope Lock Amendment A.2) — no call site needs a new
constructor argument for this reason. It does need `, AgentRunExecutionTrigger`
added to its declared supertype list, which is a production-file change
only (§1), invisible to callers.

### 3.2 Behavioral updates (the real work — every direct `InMemoryAgentRuntime.submit()` caller that used to rely on it running to completion)

- `tests/runtime/InMemoryAgentRuntimeTest.kt` — every existing test calling
  `runtime.submit(startCommand())` (or equivalent) and then asserting a
  terminal `AgentRun` status, `agent.completed`/`agent.failed` publication,
  or step-count behavior immediately afterward needs
  `runtime.execute(result.agentRunId)` (or the DENIED/DEFERRED-branch
  equivalent, which needs no `execute()` call since phase 1 alone already
  returns `Rejected`) inserted between the `submit()` call and those
  assertions. This is the file's entire existing test body, not a subset —
  it predates this milestone and was written against the single-call
  contract.
- `tests/runtime/EventCollectorTest.kt` — the one test using
  `buildAgentRuntime(bus)` + `runtime.submit(startCommand())` directly (not
  through `InMemoryTaskManagerRuntime`) needs the same added `execute()`
  call.
- `tests/runtime/RuntimeLifecycleEventPublicationTest.kt` — both
  `InMemoryAgentRuntime` call sites (`buildAgentRuntime`'s tests, and the
  standalone "unresolvable Agent Identity" test) need the same treatment —
  except the identity-rejection test, which already returns `Rejected` from
  phase 1 and needs no `execute()` call (its assertion, `listOf("agent.created")`,
  is unaffected either way).
- `tests/runtime/VerticalSliceEndToEndTest.kt` — its one direct
  `agentRuntime.submit(command)` call (already present for a different
  reason — it manually drives the Agent Runtime independently of
  `InMemoryTaskManagerRuntime`'s own now-automatic submission) needs an
  added `agentRuntime.execute(commandAccepted.agentRunId)` call before its
  post-completion assertions (`AgentRunStatus.COMPLETED`, tool-invocation
  counts, the full event-list assertion). This test's event-list assertion
  also changes shape: `task.agent_run_rejected` (added in the prior
  corrective pass, now obsolete for this test's own flow — that test used a
  *Rejecting* fake channel for `InMemoryTaskManagerRuntime`, unaffected by
  this amendment) stays where it is; the manually-driven `agentRuntime`
  portion of the sequence gains no new events, only a later completion
  point.
- `tests/runtime/TaskManagerAgentRunSubmissionIntegrationTest.kt` — the
  accepted flow's event-order assertion is rewritten to the frozen sequence
  in Scope Lock Amendment A.3 (see §5 below) and now asserts
  `task.agent_run_started` strictly precedes `agent.completed`. The rejected
  flow is unaffected in substance (still no `AgentRun`, still `task.agent_run_rejected`,
  still `QUEUED`) but should add an assertion that the fake/spy
  `AgentRunExecutionTrigger` (if the test is restructured to use one instead
  of the real object end-to-end) was never invoked — optional, strengthens
  Scope Lock Amendment A.6 item 4's coverage but is not itself a new
  obligation beyond what A.6 already lists.

### 3.3 New assertions required by Scope Lock Amendment A.6

- A.6 item 1: a spy/call-counting `AgentStepSource` proving `nextStep` is
  never called between `submit()` returning `Accepted` and `execute()` being
  called — new test in `InMemoryAgentRuntimeTest.kt`.
- A.6 item 2: `execute(agentRunId)` called directly (without a prior
  `submit()` in the same test, using a fixture that pre-accepts a run)
  reaches the same terminal states `submit()` alone used to — new test in
  `InMemoryAgentRuntimeTest.kt`.
- A.6 item 4: a spy `AgentRunExecutionTrigger` with `executeCallCount == 0`
  after a `Rejected` `submitProposal()` — new test in
  `InMemoryTaskManagerRuntimeTest.kt`.
- A.6 item 5: no new introspective assertion — satisfied by the accepted
  flow integration test passing at all (Mutex exposes no public "is held"
  API; passing where it previously deadlocked is the evidence, per A.6's own
  text).

### 3.4 Test fixture (separately tracked, unaffected by this amendment)

`tests/runtime/EventCollector.kt`'s `SPRINT_1_EVENT_TYPES` still needs
`task.agent_run_started` and `task.agent_run_rejected` added — agreed
separately, still outstanding, still test-fixture-only, still requires no
production change.

---

## 4. Exact documentation files

- `docs/architecture/CONTROLLED_AGENT_RUN_SUBMISSION_CONTRACT_DESIGN.md` — already amended (Amendment 1). No further edit expected during implementation unless Native Verification surfaces a discrepancy (Scope Lock Amendment A.7).
- `docs/implementation/CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md` — already amended. No further edit expected during implementation.
- `docs/implementation/IMPLEMENTATION_HISTORY.md` — new entry (or an addendum to the existing Controlled Agent Run Submission entry) documenting the two-phase correction, cross-referencing both reconciliation documents and both amendments, once implementation is complete and verified.
- `docs/architecture/IMPLEMENTATION_GAPS.md` — the existing Controlled-Agent-Run-Submission update paragraph should gain one sentence noting that `AgentRunCommandChannel.submit()` for `START` now returns before execution, and that a caller wanting the pre-amendment "submit and wait for completion" behavior must call `execute()` immediately after — relevant only if any future reader might otherwise assume `submit()` alone still suffices.
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md` — no further change expected; §16's correction (already made) described `AgentRunCommandChannel` as implemented, which remains true; it did not describe `submit()`'s completion semantics in enough detail to now be wrong.

---

## 5. Constructor call-site updates (summary table)

| Constructor | Old arity | New arity | Call sites |
|---|---|---|---|
| `InMemoryTaskManagerRuntime(...)` | 3 | 4 (`+ agentRunExecutionTrigger`) | 6 files, ~52 call sites total (§3.1) |
| `InMemoryAgentRuntime(...)` | 8 | 8 (unchanged — gains a supertype, not a parameter) | 0 call sites need a new argument |

---

## 6. Expected event-sequence changes

| Flow | Before this amendment (as implemented, deadlocking) | After this amendment |
|---|---|---|
| Accepted, synchronous completion | *(never observed — deadlocked before returning)* | `task.created, task.ready, agent.created, agent.initialised, agent.ready, task.agent_run_started, [Task QUEUED→RUNNING], execute(agentRunId), agent.started, agent step/execution/permission events, agent.completed, task.completed` |
| Rejected | `task.created, task.ready, task.agent_run_rejected` (already correct, unaffected) | Unchanged |

No event gains or loses a payload field. No event is renamed. Three things
change: the relative position of `task.agent_run_started` (now before,
never after, `agent.completed`); its causal relationship to
`Task QUEUED -> RUNNING` (now published-then-transitioned inside the same
lock section that created the `Task`, rather than after an entire run's
worth of nested work); and the relative position of `agent.started` itself,
which now publishes *after* `task.agent_run_started` and `Task QUEUED ->
RUNNING`, inside `execute()`, rather than as the last event phase 1
published before returning. `agent.started` now truthfully coincides with
`AgentRunStatus.RUNNING` being reached, not with `READY` (Lifecycle State
Correction, Contract Design Amendment 1 §A1.8).

---

## 7. Rollback conditions

Restated from Scope Lock Amendment A.7, plus one addition specific to
sequencing this plan:

1. Splitting `start()` requires changing any behavior inside `runLoop()`
   itself — stop, report, return to Contract Design Amendment 1.
2. Any `InMemoryAgentRuntimeTest.kt` test cannot be made to pass by adding
   an `.execute()` call alone — stop, report, return to Contract Design
   Amendment 1.
3. The frozen event order (§6 above / Scope Lock Amendment A.3) is not what
   Native Verification actually observes — stop, report, return to Scope
   Lock Amendment.
4. **(New)** Implementing `execute()` reveals that `RunState` or any other
   per-run bookkeeping `start()` currently initializes inline is not yet
   safely readable by a *separate* method call (e.g., a race between phase
   1's own lock release and a hypothetical concurrent second `execute()`
   call for the same `agentRunId`) — stop, report; this would mean phase
   1/phase 2's boundary needs an additional guard (e.g., a "phase 2 not yet
   triggered" flag) that neither amendment currently specifies, and adding
   one without approval would be exactly the kind of silent scope
   broadening Section 15's Implementation Discipline prohibits.
5. Any discovery that `InMemoryTaskManagerRuntime` or `InMemoryAgentRuntime`
   has a production caller neither amendment identified — stop, report
   (restated from the original Scope Lock's own Section 14, still in force).

---

## 8. What this plan does not do

No source file has been modified. No governance document has been further
amended. Nothing has been staged, committed, or pushed. This plan awaits
explicit approval, separate from and in addition to the Contract Design
Amendment 1 and Scope Lock Amendment approvals it depends on.
