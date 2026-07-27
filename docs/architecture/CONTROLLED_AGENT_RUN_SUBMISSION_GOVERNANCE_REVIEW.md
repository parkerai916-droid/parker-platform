# Controlled Agent Run Submission — Governance Review

**Status:** Read-only governance review. No source files modified. No implementation code written. Nothing committed or pushed.

**Repository baseline reviewed:**
```
Branch: main
Latest commit: 992538e
Previous implementation commit: 8f6c3d1
Working tree: clean
Repository: synchronized with origin/main
```

**Milestone under review:** Controlled Agent Run Submission — the transition from an authorised `TaskProposal` and a queued `Task` into a controlled, Trust-gated `AgentRunCommand` submission.

**Constitutional constraint under which this review is conducted:** "Cognition proposes. Trust authorises. Runtime executes." Creating or queueing a Task Proposal or Task must never itself confer execution authority. Any `AgentRunCommand` submission must pass through Parker's existing identity, permission, Trust Framework, and runtime controls.

---

## 1. Executive Summary

This review's central finding reframes the milestone. Before this review, it was reasonable to assume "Controlled Agent Run Submission" meant building a Trust-gated Agent Runtime from scratch. It does not. `InMemoryAgentRuntime` (`src/runtime/InMemoryAgentRuntime.kt`) already **is** a complete, production-quality `AgentRunCommandChannel` implementation, and it already routes every proposed Agent Step through the existing Trust-gated `ExecutionPipeline.submit()` → `PermissionEngine.evaluate()` path. Per-action Trust-gating for Agent Runs is already solved, already tested (29 tests in `InMemoryAgentRuntimeTest.kt`), and requires no new design.

What is actually missing is narrower and falls into exactly two gaps:

1. **Nothing in production ever calls `AgentRunCommandChannel.submit()`.** `InMemoryTaskManagerRuntime.submitProposal()` constructs a well-formed `AgentRunCommand.START` for every accepted `TaskProposal` (`InMemoryTaskManagerRuntime.kt:291-307`) and stores it for observation, but the class's own KDoc states plainly: "Constructs, but never submits, an `AgentRunCommand`." Nothing consumes that stored command.
2. **`InMemoryAgentRuntime`'s mandatory `agentStepSource: AgentStepSource` dependency has zero production implementations.** Only three test fixtures exist (`SingleStepAgentStepSource`, `FakeAgentStepSource`, `ControllableAgentStepSource`, all under `tests/runtime/`). `InMemoryAgentRuntime` cannot be constructed in `ParkerRuntime.kt`'s composition root today without a production `AgentStepSource` to hand it.

This is the third occurrence, in this same project history, of the identical pattern: a fully-built runtime component with a mandatory constructor dependency that has never been given a production implementation (`TaskProposalIntake` for `InMemoryPlannerRuntime`; `PlanCandidateGenerator` for `GoalPlanningHandoffCoordinator`; now `AgentStepSource` for `InMemoryAgentRuntime`). Both prior instances were resolved the same way: a small, honestly-labelled, deterministic production implementation, plus a thin coordinator wiring it into the composition root. That precedent — not a redesign of Trust-gating, which already works — is what this review recommends repeating.

A second, independent finding concerns the constitutional constraint itself: `AgentRunCommand.requestingPrincipalId` is carried by every command but is not currently checked against anything by `InMemoryAgentRuntime`. This does not currently violate the "Trust authorises" principle, because every proposed action inside the Agent Run still passes through `PermissionEngine.evaluate()` regardless of who requested the run. But it means the *decision to start an Agent Run at all* is not itself a Trust Framework decision today — it is an unconditional acceptance of any well-formed `START` command. Section 5 below treats this as the review's principal open trust-boundary question, not as a defect to silently fix.

**Recommendation:** Ready for Contract Design, with two design questions this review deliberately leaves open for that stage to resolve (Section 8, Section 10).

---

## 2. Review Objectives — Findings

### 2.1 Where is `AgentRunCommand` currently constructed?

Exactly one call site: `InMemoryTaskManagerRuntime.submitProposal()` (`src/runtime/InMemoryTaskManagerRuntime.kt:291-307`), immediately after a `TaskProposal` is accepted and its `Task` reaches `QUEUED`. Every field is populated from the `TaskProposal` and the resolved owner:

```
AgentRunCommand(
    commandType = AgentRunCommandType.START,
    taskId = taskId,
    requestingPrincipalId = owner.principalId,
    targetAgentCapability = proposal.requiredCapabilities,
    goalDescription = proposal.goal,
    contextReferences = proposal.contextReferences,
    resourceReferences = proposal.resourceReferences,
    correlationId = proposal.correlationId,
)
```

`agentRunId` is omitted (`null`), correctly, since `AgentRunCommand`'s own `init` block requires `agentRunId == null` for `START`. The command is appended to an in-memory `agentRunCommands: MutableMap<TaskId, MutableList<AgentRunCommand>>` and exposed only via the test/inspection accessor `agentRunCommandsFor(taskId)`. No other production file constructs an `AgentRunCommand` anywhere in the repository.

### 2.2 Is it currently submitted anywhere?

No. `InMemoryTaskManagerRuntime` holds no reference to an `AgentRunCommandChannel` at all — it is not a constructor parameter, not a field, not imported. The class's own KDoc says so directly: "this class never calls `AgentRunCommandChannel.submit` — no implementation of that interface exists yet (Unit 7)." That KDoc sentence is now stale (see 2.3), but the behavioural claim — no submission occurs — remains accurate today. `grep` across `src/` confirms zero call sites of `.submit(` against any `AgentRunCommand`-typed value outside test files.

### 2.3 Existing contracts

All five contracts named in the review request already exist, are fully specified, and are more mature than a first read suggests:

- **`TaskProposal`** (`src/contracts/TaskProposal.kt`) — 18-field data class; `TaskProposalDisposition` sealed class with five outcomes (`Accepted`, `Deferred`, `Rejected`, `Split`, `Merged`); `TaskProposalIntake` interface. Fully implemented by `InMemoryTaskManagerRuntime` for the `Accepted`/unresolvable-owner-`Rejected` pair only, per its own documented Sprint 1 scope — `Deferred`/`Split`/`Merged` and business-reason `Rejected` remain unimplemented, which is out of this milestone's scope and not revisited here.
- **Task records and task states** (`src/contracts/Task.kt`, `src/contracts/TaskLifecycle.kt`) — `Task` data class and the full 9-state `TaskStatus` machine (`TaskLifecycleTransitions`, with `isValidTransition`/`requireValidTransition` helpers). Currently driven `CREATED → QUEUED` (always, on accept) and `QUEUED → RUNNING → COMPLETED` (only in response to a real `agent.completed` event — see 2.4). No path currently drives `FAILED`, `CANCELLED`, `PAUSED`, `EXPIRED`, or `SUPERSEDED`.
- **`AgentRunCommand`** (`src/contracts/AgentRunCommand.kt`) — `AgentRunCommandType` (`START`/`SUSPEND`/`RESUME`/`CANCEL`), `AgentRunCommand` data class with an `init` block enforcing the `agentRunId`-null-iff-`START` invariant and a non-blank `cancellationReason` for `CANCEL` only, and `AgentRunCommandResult` (`Accepted`/`Rejected`). Fully specified, fully implemented as a value type; nothing about this contract needs to change for this milestone.
- **`AgentRunCommandChannel`** (`src/contracts/AgentRunCommand.kt`, same file) — a one-method interface, `suspend fun submit(command: AgentRunCommand): AgentRunCommandResult`. **Its own KDoc is stale**: it states "No implementation of this interface exists in this repository," and `TaskManagerRuntimeSpecification.md` §16 repeats the same claim ("No implementation of `AgentRunCommandChannel` exists yet"). Both predate `InMemoryAgentRuntime`'s Track C Unit C2 work, which does implement this interface in full. This is a documentation staleness finding, not an architectural gap — see Section 7.
- **Agent Runtime submission and outcomes** (`src/runtime/InMemoryAgentRuntime.kt`) — `submit()` dispatches on `AgentRunCommandType` to `start()`/`suspendRun()`/`resumeRun()`/`cancelRun()`, returning `AgentRunCommandResult.Accepted`/`Rejected` per command. `start()` mints deterministic, taskId-derived IDs (`AgentRunId("run-for-${taskId.value}")`, `PrincipalId("agent-for-${taskId.value}")`), atomically rejects a duplicate `START` for the same Task under the same `mutex.withLock` pattern `InMemoryPlannerRuntime` and `InMemoryTaskManagerRuntime` already use, resolves the Agent Identity through `IdentityService` (rejecting unless `PrincipalType.INTERNAL_AGENT` with a non-null `owner`), and then drives the full ten-state `AgentRunLifecycleTransitions` machine through a real multi-step loop, submitting every proposed action as an `ExecutionRequest` via `executionPipeline.submit()`. This is the most mature of the five contracts reviewed and needs no new design work.

### 2.4 Which component should own command submission?

`MULTI_STEP_AGENT_RUN_DESIGN.md` §9 already answers this, in the specification, not as this review's own invention: "only an external caller (in practice, the Task Manager Runtime, per `TaskManagerRuntimeSpecification.md` §7's sequence diagram) issues these via `AgentRunCommandChannel.submit`." `TaskManagerRuntimeSpecification.md` §15 independently confirms the same shape: an `Accepted` disposition may lead to Agent Runtime involvement "at the Task Manager Runtime's own discretion... via Section 16's Agent Run Command Channel."

Two design shapes satisfy this without contradicting either document, and Contract Design should choose between them rather than this review pre-deciding:

- **(a) Direct injection.** `InMemoryTaskManagerRuntime` takes an `AgentRunCommandChannel` constructor dependency and calls `.submit(command)` immediately after constructing it, in the same `mutex.withLock` block, replacing the current "construct and store" step with "construct and submit."
- **(b) Thin coordinator.** A new, single-purpose coordinator (mirroring `GoalPlanningHandoffCoordinator`'s established shape from the prior milestone) sits between `InMemoryTaskManagerRuntime` and `AgentRunCommandChannel`, so `InMemoryTaskManagerRuntime` itself never depends on the Agent Runtime at all.

(a) is more literally what §9's "in practice, the Task Manager Runtime... issues these" describes, and avoids introducing a fourth class name into an already-large composition root. (b) keeps `InMemoryTaskManagerRuntime` single-responsibility (Task lifecycle and proposal intake only) and repeats a pattern the user has now approved twice this session. This review flags the choice; it does not make it.

### 2.5 Which system identity should perform submission?

Two identities are in play and should not be conflated:

- **`AgentRunCommand.requestingPrincipalId`** — currently set to `owner.principalId`, i.e. the Task's owner (a real, external Principal), not a system identity. This is attribution of *who the run is for*, per the field's own KDoc citing `TaskManagerRuntimeSpecification.md` §8's "every Task operation is performed by an authenticated Principal."
- **The identity that *calls* `AgentRunCommandChannel.submit()`** — if InMemoryTaskManagerRuntime is the caller (2.4), no new system identity is strictly required; `TASK_MANAGER_RUNTIME_PRINCIPAL_ID` (`PrincipalId("system.task-manager-runtime")`) already exists as that class's own operating identity and already publishes `task.*` events under it. If a new coordinator is chosen instead (2.4b), it would need its own system identity registered in `ParkerRuntime.kt`'s composition root, following the exact `PLANNER_RUNTIME_PRINCIPAL_ID`/`TASK_MANAGER_RUNTIME_PRINCIPAL_ID` precedent from the prior milestone.

Neither choice requires inventing a new *kind* of identity — only, possibly, one new literal `PrincipalId` value, exactly as the prior milestone's Scope Lock resolved the same question for the Planner/Task-Manager pair.

### 2.6 Which Trust Framework and Permission Engine checks must apply?

Two distinct layers already exist and must not be collapsed into one:

- **Per-action gating (already fully solved).** Every `AgentStepDecision.Propose` inside `InMemoryAgentRuntime.runLoop()` becomes a real `ExecutionRequest` submitted through `executionPipeline.submit()`, which unconditionally calls `permissionEngine.evaluate(request)` (`DefaultExecutionPipeline.kt:150`) before anything executes, mapping `APPROVED`/`APPROVED_WITH_CONFIRMATION` → proceed, `DENIED` → terminal `DENIED` result, `DEFERRED` → terminal `DEFERRED` result. This is unconditional, unchanged by this milestone, and requires no new work. `MULTI_STEP_AGENT_RUN_DESIGN.md` §9 states the same guarantee explicitly: "Nothing in this design gives a second-or-later Agent Step a cached, reused, or pre-approved decision."
- **Run-initiation gating (not currently a Trust Framework decision).** `InMemoryAgentRuntime.start()` checks only that the *Agent Identity* being started resolves and is a valid `INTERNAL_AGENT` with an `owner`. It does not call `PermissionEngine.evaluate` (or any other Trust Framework check) against `requestingPrincipalId` or against the command itself before minting the Agent Run. Today, in the absence of any real caller, this has no observable effect — but once a real caller exists, an accepted `TaskProposal`'s owner will be able to cause an Agent Run to start purely because the Task Manager Runtime constructed the command, with no independent authorisation step in between. Whether this is acceptable, or whether `AgentRunCommandChannel.submit` (or its production caller) should itself request a Permission Engine evaluation before calling `InMemoryAgentRuntime.start()`, is this review's central open trust-boundary question (Section 5, Section 10).

### 2.7 How should submission acceptance, rejection, and failure propagate?

`AgentRunCommandResult` already models this fully: `Accepted(agentRunId, commandType)` or `Rejected(commandType, reason)`. Once a real caller exists, the outcome needs to reach the constitutional principle's own required place — an auditable Task Event, not a silent return value. `TaskManagerRuntimeSpecification.md` §10 already names the specific event for exactly this purpose: `TaskAgentRunStarted` / `task.agent_run_started`, "An Agent Run is created within or on behalf of this Task." `InMemoryTaskManagerRuntime`'s own KDoc explains precisely why this event is not published today: "this class only *constructs* an `AgentRunCommand` value object, never submits it... no Agent Run exists yet for this method to truthfully report." Once submission genuinely occurs, `task.agent_run_started` becomes truthfully publishable for the first time. A `Rejected` result should likewise be surfaced as a Task Event (no exact event name is currently reserved for this in the specification; Contract Design should either reuse `task.blocked` or name a new one).

### 2.8 When should Task state change?

Partially answered already, and partially still open. `InMemoryTaskManagerRuntime` already implements `QUEUED → RUNNING → COMPLETED` in response to `agent.completed`, and leaves `agent.failed` as record-only with no transition (Sprint 2 Track B Unit B2, `SPRINT_2_B2_IMPLEMENTATION_DECISIONS.md`). This logic is fully built and fully tested today — but because nothing ever submits a real `AgentRunCommand`, no real Task in production can ever reach a state where a genuine `agent.completed` event exists to trigger it. Once submission is wired, this existing logic becomes live for the first time, with no changes needed to it.

What remains genuinely open: **whether a `Task` should transition at the moment `START` is *accepted*** (e.g. an implicit or explicit `QUEUED → RUNNING` at acceptance, ahead of the Agent Run's own first step) or only in response to the Agent Run's own terminal events, as now. The Task Manager specification's own event table (`TaskStarted` / `task.started`, "The Task begins active progress, directly or via an Agent Run... Real transition: `Queued --> Running`") is compatible with either timing and does not resolve it. This is Contract Design's decision, not this review's.

### 2.9 How must duplicate command submission be prevented?

The mechanism already exists and is already exercised by two independent tests: `InMemoryAgentRuntime.start()` performs an atomic `check(agentRunId !in agentRuns)` under `mutex.withLock`, using the deterministic `taskId`-derived `AgentRunId`, so a second `START` for the same `taskId` is rejected at the Agent Runtime layer regardless of who calls it or how many times. `InMemoryAgentRuntimeTest.kt` explicitly covers this ("resubmitting START for the same taskId is rejected as caller misuse", "the one-Agent-Run-per-Task cap is a deliberate, documented decision, not an accidental limitation"). No new duplicate-prevention design is required; the existing mechanism already satisfies this objective once a caller exists to exercise it.

A second-order question worth naming: `InMemoryTaskManagerRuntime.submitProposal()` also independently prevents duplicate submission at its own layer (`check(taskId !in tasks)`, one `AgentRunCommand` constructed per accepted proposal). So even under design option 2.4(a), duplicate prevention would exist at two independent layers — proposal-level and run-level — which is redundant but not contradictory, and does not need to be resolved by this review.

### 2.10 Which runtime and audit events should be emitted?

Almost everything needed already exists and already fires correctly, purely because `InMemoryAgentRuntime` has been fully built and tested independent of any real caller: `agent.created`, `agent.initialised`, `agent.ready`, `agent.started`, `agent.step_started`, `agent.action_proposed`, `agent.permission_required`, `agent.action_approved`/`agent.action_denied`/`agent.action_deferred` (as applicable), `agent.step_completed`, `agent.suspended`, `agent.resumed`, `agent.cancelled`, `agent.completed`, `agent.failed` — all already publish through `eventBus.publish` with `publisherPrincipalId = run.agentIdentityPrincipalId` and a `taskId`/`agentRunId`-bearing payload. The one genuinely new event this milestone should add is `task.agent_run_started` (2.7), already reserved by name in the specification but never yet publishable because no Agent Run has ever genuinely existed. No new event *type* needs to be invented; only one already-specified, already-named event needs an emitter now that the underlying fact it reports can become true.

### 2.11 What production composition changes will be required?

`ParkerRuntime.kt`'s composition root currently constructs neither `InMemoryAgentRuntime` nor any `AgentStepSource` (confirmed by direct inspection — zero matches for either symbol in that file). At minimum, Contract Design will need to decide and freeze:

- A production `AgentStepSource` implementation (Section 3) and where it lives.
- Construction of `InMemoryAgentRuntime` itself, including its `AgentPolicy` (a default `maxAgentSteps` value must be chosen — the existing test-only `DEFAULT_AGENT_POLICY = AgentPolicy(maxAgentSteps = 10)` is a plausible but not yet authorised production default).
- The wiring of `AgentRunCommandChannel` submission into `InMemoryTaskManagerRuntime` (or a new coordinator), per whichever of 2.4(a)/(b) is chosen.
- Any new system `PrincipalId` literal(s) this wiring requires, following the existing composition-root literal-duplication convention.
- Registration of the Agent Runtime's own operating identity (distinct from any per-Task `agent-for-<taskId>` identity minted at `start()` time) as an active system identity, if 2.4(b)'s coordinator approach is chosen.

### 2.12 What tests already exist, and what verification gaps remain?

Existing coverage is extensive and already exercises almost everything this milestone touches, in isolation:

- `InMemoryAgentRuntimeTest.kt` — 29 tests covering `START`/`SUSPEND`/`RESUME`/`CANCEL`, identity resolution and rejection, `DENIED`/`DEFERRED` divergence, multi-step loops, `maxAgentSteps` suspension, duplicate-`START` rejection, and independent-run isolation.
- `InMemoryTaskManagerRuntimeTest.kt` — 24 tests covering proposal acceptance/rejection, `AgentRunCommand` construction fidelity (including `targetAgentCapability` and `resourceReferences` propagation), `agent.completed`/`agent.failed` event recording, and the `QUEUED → RUNNING → COMPLETED` transition logic.
- `AgentRunCommandTest.kt`, `TaskProposalTest.kt`, `TaskLifecycleTransitionsTest.kt`, `AgentRunLifecycleTransitionsTest.kt` — contract-level invariant tests (not yet re-inspected line-by-line in this review, but confirmed to exist).

**The one coverage gap this review identifies as significant:** there is no test anywhere in the repository that exercises `InMemoryTaskManagerRuntime` and `InMemoryAgentRuntime` together — no test constructs a real `AgentRunCommand` via `submitProposal()` and feeds it into a real `AgentRunCommandChannel.submit()` call. Every existing test treats the two classes as fully independent units. This is the exact shape of integration test this milestone's own Definition of Complete (Section 9) should require, mirroring `ParkerRuntimeConversationPipelineTest.kt`'s end-to-end role from the prior milestone.

---

## 3. Architectural Gaps (Consolidated)

1. **No production `AgentStepSource`.** Zero implementations of `AgentStepSource` exist outside `tests/runtime/`. `AgentStep.kt`'s own KDoc already anticipates and sanctions exactly this kind of gap-filling stand-in: "a fixed, deterministic, non-Planner stand-in for testing and for any production wiring that exists before a real Planner does" — directly citing a class it names `FixedSequenceAgentStepSource`. **Naming discrepancy, not silently reconciled:** the actual current file implementing this role is `tests/runtime/SingleStepAgentStepSource.kt`, not `FixedSequenceAgentStepSource`. `MULTI_STEP_AGENT_RUN_DESIGN.md` §11 independently confirms this was always the intended shape: "a fixed, deterministic, non-Planner default implementation of `AgentStepSource` for testing and for any production wiring that exists before a real Planner does — mirroring `DeterministicPlannerHarness`'s exact precedent." This is precisely the same shape as `DefaultPlanCandidateGenerator`, built and approved in the immediately preceding milestone.
2. **No production submission call site.** `AgentRunCommandChannel.submit()` is never called by any production code path (Section 2.2).
3. **`InMemoryAgentRuntime` is not constructed anywhere in `ParkerRuntime.kt`.** Confirmed by direct inspection of the composition root.
4. **Two stale KDoc/specification claims.** `AgentRunCommand.kt`'s `AgentRunCommandChannel` KDoc and `TaskManagerRuntimeSpecification.md` §16 both still assert no implementation exists. Both predate `InMemoryAgentRuntime`'s Track C Unit C2 work and should be corrected once this milestone's implementation phase lands (not by this review, which is read-only).
5. **`requestingPrincipalId` is inert.** Carried on every `AgentRunCommand` for attribution but not currently evaluated by `InMemoryAgentRuntime.start()` against any Trust Framework check (Section 2.6). Not a violation of the constitutional principle today (no caller exists to exploit it), but a design question that must be resolved before a caller is added.

---

## 4. Ownership Analysis

See Section 2.4 for the two candidate shapes and Section 2.5 for identity implications. No new ownership question exists beyond who calls `AgentRunCommandChannel.submit()` — every other responsibility in this milestone (per-step Trust-gating, event publication, lifecycle transitions, duplicate prevention) already has a clear, already-implemented owner and does not need to be reassigned.

---

## 5. Trust-Boundary Analysis

The constitutional principle survives this milestone's current-state review intact, with one caveat requiring an explicit decision rather than an implicit default:

- **"Cognition proposes."** A `TaskProposal`'s acceptance and a Task reaching `QUEUED` confer no execution authority today, and would confer none under either ownership option in Section 2.4 — accepting a proposal only ever *constructs* a command; nothing about acceptance itself starts an Agent Run.
- **"Trust authorises."** Fully intact for every action *within* an Agent Run — `ExecutionPipeline`/`PermissionEngine` gates every proposed action unconditionally, unchanged by this milestone. **Not yet extended to the decision to start the Agent Run itself.** Today, an accepted `TaskProposal` plus a call to `submit()` is sufficient to start an Agent Run; no Permission Engine evaluation stands between "Task Manager decided to submit" and "Agent Run exists and begins executing its first step." Whether that gap is acceptable — on the reasoning that the *proposal's own acceptance* was already an authorised act, and every subsequent action is independently re-evaluated anyway — or whether `START` submission itself should require an explicit Permission Engine evaluation, is the one substantive trust-boundary decision this review surfaces for Contract Design rather than resolving unilaterally.
- **"Runtime executes."** Fully intact and unchanged — `InMemoryAgentRuntime` is the only component that ever calls `ExecutionPipeline.submit`, exactly as `TaskManagerRuntimeSpecification.md` requires ("The Task Manager Runtime introduces no second path").

---

## 6. Recommended Design Direction

1. Add a small, honestly-labelled, deterministic production `AgentStepSource` implementation under `src/runtime/`, as a **new** file — not a relocation of `tests/runtime/SingleStepAgentStepSource.kt` — mirroring the explicit precedent set for `DefaultPlanCandidateGenerator` in the immediately preceding milestone ("a new, separate production implementation, not a relocation of a test fixture").
2. Wire `AgentRunCommandChannel` submission into production via one of the two shapes in Section 2.4, choosing between direct injection into `InMemoryTaskManagerRuntime` and a new thin coordinator, as a Contract Design decision.
3. Resolve the Section 5 trust-boundary question (whether `START` submission requires its own Permission Engine evaluation) explicitly, rather than defaulting silently to "no."
4. Publish `task.agent_run_started` once a real Agent Run genuinely exists to report (Section 2.7, Section 2.10) — the only new audit event this milestone requires.
5. Add the one missing integration test class identified in Section 2.12, exercising `InMemoryTaskManagerRuntime` and `InMemoryAgentRuntime` together end-to-end, mirroring `ParkerRuntimeConversationPipelineTest.kt`'s role in the prior milestone.
6. Correct the two stale "no implementation exists" claims (Section 3, item 4) as part of this milestone's own documentation reconciliation step — not now, since this review is read-only.

---

## 7. In-Scope / Out-of-Scope (Restated per the User's Own Boundaries)

**In scope**, per the review request, and confirmed compatible with current-state findings: production `AgentRunCommandChannel` usage (already implemented; needs a caller); authorised command submission (Section 2.4); identity registration (Section 2.5, Section 2.11); permission evaluation (Section 2.6, Section 5); task-state transitions (Section 2.8, already substantially implemented); command outcome propagation (Section 2.7); runtime audit events (Section 2.10, one new event); production composition-root wiring (Section 2.11); targeted and full-suite tests (Section 2.12).

**Out of scope, unless strictly required** (restated verbatim from the review request, and confirmed by this review's findings to be genuinely avoidable): scheduling; Workflow Engine implementation; multi-agent orchestration; distributed execution; advanced retry policies; plugin ecosystem expansion; Home Assistant integration; Android integration; unrelated refactoring. Nothing found in this review requires touching any of these — the entire gap is confined to the two items in Section 1's summary plus the trust-boundary decision in Section 5.

---

## 8. Risks

- **Silent authority expansion, if the Section 5 question is skipped rather than decided.** If `START` submission is wired without an explicit decision on Permission Engine evaluation, the project will have quietly answered "no Trust Framework check at run-initiation" by default rather than by design — precisely the outcome the constitutional constraint is meant to prevent being reached accidentally.
- **`AgentStepSource` naming confusion carried forward.** If a new production `AgentStepSource` is authored without addressing the `FixedSequenceAgentStepSource`-vs-`SingleStepAgentStepSource` KDoc/filename mismatch (Section 3, item 1), a future reader will have three inconsistently-named classes performing the same conceptual role. Contract Design should settle the naming once component design is frozen.
- **`AgentPolicy.maxAgentSteps` default has no authorised value yet.** The only existing value (`10`) lives in a test file, not in any specification or decision document, and should not be silently promoted to a production default without an explicit decision.
- **Redundant duplicate-prevention across two layers** (Section 2.9) is not a correctness risk today, but could become one if a future change makes the two layers' keys diverge (e.g. if `AgentRunId` minting ever stops being purely `taskId`-derived).

---

## 9. Open Questions

1. Direct injection into `InMemoryTaskManagerRuntime`, or a new thin coordinator, for `AgentRunCommandChannel` submission (Section 2.4)? This review does not resolve it.
2. Should `AgentRunCommand.START` submission require its own explicit Permission Engine evaluation before `InMemoryAgentRuntime.start()` is invoked, or is per-action gating inside the run sufficient (Section 5)? This is the review's single most important open question.
3. Should a Task transition at `START` acceptance, or only in response to the Agent Run's own terminal events, as now (Section 2.8)?
4. What production default should `AgentPolicy.maxAgentSteps` take, and where should that default be authorised (a specification, a decision document, or the composition root itself)?
5. Should the naming discrepancy between `AgentStep.kt`'s KDoc (`FixedSequenceAgentStepSource`) and the actual file (`SingleStepAgentStepSource`) be resolved by renaming, by correcting the KDoc, or left as historical record with a note? Not blocking, but worth a decision before a new, similarly-named production class is added alongside it.
6. What Task Event (if any) should represent a `Rejected` `AgentRunCommandResult`, given that `task.agent_run_started` (Section 2.7) only covers the accepted case?

---

## 10. Proposed Definition of Complete

For this milestone to be considered complete:

1. A production `AgentStepSource` implementation exists in `src/runtime/`, is a new implementation (not a relocated test fixture), and is honestly documented as a deterministic stand-in pending a real Planner (Chapter 20), per `AgentStep.kt`'s own KDoc and `MULTI_STEP_AGENT_RUN_DESIGN.md` §11.
2. `InMemoryAgentRuntime` is constructed in `ParkerRuntime.kt`'s composition root with a frozen `AgentPolicy`.
3. Exactly one component — `InMemoryTaskManagerRuntime` directly, or a new frozen coordinator — calls `AgentRunCommandChannel.submit()` for every accepted `TaskProposal`'s constructed `AgentRunCommand.START`, per whichever of Section 2.4(a)/(b) Contract Design freezes.
4. The Section 5 trust-boundary question is explicitly answered and, if the answer is "yes, evaluate," implemented; if the answer is "no, per-action gating is sufficient," that reasoning is recorded in the Contract Design or Scope Lock document, not left implicit.
5. `task.agent_run_started` is published on successful submission, per `TaskManagerRuntimeSpecification.md` §10.
6. Duplicate `START` submission remains prevented (already true today; verified, not re-designed).
7. At least one new integration test exercises `InMemoryTaskManagerRuntime` and `InMemoryAgentRuntime` together, submitting a real `TaskProposal` through to a real, running Agent Run.
8. The stale "no implementation exists" claims in `AgentRunCommand.kt` and `TaskManagerRuntimeSpecification.md` §16 are corrected as part of this milestone's Documentation Reconciliation stage.
9. `.\gradlew.bat test` passes in full, confirmed by the user, before any commit.

---

## 11. Recommendation

**Ready for Contract Design.**

The review found no architectural blocker requiring further governance work: every contract this milestone touches already exists, is well-specified, and (with the two named exceptions — a production `AgentStepSource` and a submission call site) is already implemented and tested. The one substantive decision Contract Design must make that this review does not make for it is the Section 5 trust-boundary question; everything else in Sections 9-10 is either a naming/default detail or a direct restatement of an answer the specifications already give.
