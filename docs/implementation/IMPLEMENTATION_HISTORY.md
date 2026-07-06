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
4ceeb0e

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
e7e1bbf

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
115fb42 (implementation), aa5c507 (documentation finalization)

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

## Sprint 3

### Sprint 3 Track C Unit C1 – Multi-Step Agent Run Design

Commit:
affc46a

Completed:
2026-07-06

Android Studio Tests:
269/269 (unchanged — design-only unit; no Kotlin was written or modified)

Summary
- Added `docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md`.
- Specified `AgentStepSource`, `AgentStepContext`, `AgentStepDecision` (Propose/Complete/Fail), and `AgentPolicy` as new public interfaces.
- Specified the multi-step Agent Run execution model, the SUSPEND-deferred/CANCEL-immediate asymmetry, `maxAgentSteps` enforcement, and a revised Agent Runtime locking model in which `ExecutionPipeline.submit` is never called while the Agent Runtime's own mutex is held.
- Reviewed against the Parker Constitution, Architecture Decisions, PES-001, Project Governance, the Sprint 2 Plan, and the existing Agent Runtime specification, in that priority order.
- Concluded Go, with one requested change (see below) before proceeding to implementation.

Implementation Notes
- Design-only unit: no source, test, or existing architecture document was modified.
- One reviewer-requested change was made after initial drafting: a forward-compatibility note beside `AgentStepDecision` stating that future variants (for example, a "stop for external input" variant) may be added later without redesigning the runtime architecture this document specifies — no such variant was added now.
- This entry was added retrospectively, alongside Unit C2's own entry below, so Sprint 3's chronological record has no gap between "design accepted" and "design implemented." No commit, tag, or content from Unit C1 was altered in doing so.

---

### Sprint 3 Track C Unit C2 – Multi-Step Agent Run Implementation

Commit:
pending

Completed:
2026-07-06

Android Studio Tests:
Not yet run by a human in Android Studio (PES-001: test execution and commit remain Human authority). Static count: the previous suite stood at 269/269; `InMemoryAgentRuntimeTest.kt` went from 12 tests to 26 (12 removed/superseded, 26 added), a net change of +14. If every existing and new test passes unchanged, the expected total is 283/283 — this figure is an arithmetic projection from the source, not a verified run, and should be confirmed in Android Studio before commit.

Summary
- Implemented `docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md` (Unit C1) exactly, per that document's Section 11 "WILL implement" list and no further.
- Added `src/contracts/AgentStep.kt`: `AgentStepContext`, `AgentStepDecision` (`Propose`/`Complete`/`Fail`), `AgentStepSource`, `AgentPolicy`.
- Rewrote `src/runtime/InMemoryAgentRuntime.kt` in full: multi-step execution loop; `SUSPEND` (deferred to the next step boundary), `RESUME` (continues at the next step number, not step 1), and `CANCEL` (immediate from any non-terminal state, with a best-effort `ExecutionPipeline.cancel` of any in-flight step) are now implemented, superseding Sprint 1 Unit 7's "not implemented" rejections; `DENIED` and `DEFERRED` are now distinct outcomes (`FAILED` vs `SUSPENDED`) instead of both collapsing into `FAILED`; `AgentPolicy.maxAgentSteps` is enforced, ending a run at `SUSPENDED`, never `FAILED`; the locking model was redesigned so the Agent Runtime's own mutex is only ever held for short map reads/writes, never across the suspending `ExecutionPipeline.submit` call, so independent Agent Runs progress independently and a `SUSPEND`/`CANCEL` for one Agent Run is never blocked by another Agent Run's in-flight step.
- Rewrote `tests/runtime/InMemoryAgentRuntimeTest.kt` and added two new test-only fixtures: `FakeAgentStepSource` (mirrors `FakePermissionEngine`'s lambda-based fake precedent) and `ControllableTool` (a genuinely pausable `Tool`, mirroring `MockTool`'s "test fixture, not `src/runtime`" precedent, needed to prove the concurrency properties above that an immediately-returning `Tool` cannot exercise).

Implementation Notes
- Implements only Section 11's "WILL implement" list: `AgentStepSource`/`AgentStepContext`/`AgentStepDecision` (three variants only)/`AgentPolicy` (`maxAgentSteps` enforced, `maxAgentRunDuration` accepted but not enforced), multi-step execution, `SUSPEND`/`RESUME`/`CANCEL`, required `agent.*` event publication, and the revised locking model.
- Deliberately does not implement anything from Section 11's "will NOT implement" list: no Planner, no `WAITING_FOR_INPUT`, no Agent Capability system, no `maxAgentRunDuration` enforcement, no World Model or Memory integration, no Workflow Engine, no cross-Agent orchestration.
- `AgentRunLifecycleTransitions` (`src/contracts/AgentRunLifecycle.kt`) was not modified — no new `AgentRunStatus` value or transition edge was added; this unit simply drives more of the already-specified 10-state machine (`WAITING_FOR_PERMISSION`, `SUSPENDED`, and cancellation from more states) than Unit 7 did.
- The Task Manager Runtime, Permission Engine, `ExecutionPipeline` interface, and `EventBus` interface were not modified. `DefaultExecutionPipeline` is called exactly as any other existing caller would call it.
- Two implementation decisions were made that the design document left open, and are recorded here rather than only in code comments: (1) an explicit `SUSPEND` is accepted only while a run is `RUNNING`, not while it is momentarily `WAITING_FOR_PERMISSION` for an already in-flight step — a caller whose `SUSPEND` lands in that narrow window receives a `Rejected` and would need to retry, rather than the runtime silently queuing it; (2) `RESUME` does not re-check `AgentPolicy.maxAgentSteps` specially — if a run was suspended because the bound was already reached, `RESUME` still transitions it back to `RUNNING`, and the loop's own existing bound check immediately re-suspends it before any further step is attempted, rather than the runtime inventing a bound-override or bound-increase mechanism.
- `ExecutionPipeline.cancel` is called on a best-effort basis when `CANCEL` arrives mid-step; this repository's own `ExecutionLifecycleTransitions` has no `EXECUTING -> CANCELLED` edge, so that call will typically report `cancelled = false` while a `Tool.execute()` call is genuinely in flight — the Agent Run's own `CANCELLED` status does not wait on, or depend on, that call succeeding.
- All 12 of Sprint 1 Unit 7's original tests are preserved in spirit; two ("SUSPEND is Rejected as not implemented", "RESUME and CANCEL are also Rejected as not implemented") were removed as no longer true and replaced by dedicated `SUSPEND`/`RESUME`/`CANCEL` test sections below, mirroring exactly how Sprint 2 Unit B2 superseded an obsolete Unit B1-era test.

---

### Sprint 3 Track D Unit D1 – Planner Runtime Progression Design and Specification Review

Commit:
pending

Completed:
2026-07-06

Android Studio Tests:
283/283 (unchanged — design/review-only unit; no Kotlin was written or modified)

Summary
- Added `docs/architecture/PLANNER_RUNTIME_PROGRESSION_DESIGN.md`: a review-and-correction pass over `PlannerRuntimeSpecification.md`, checking it for internal consistency and consistency with `DeterministicPlannerHarness.kt`'s actual (narrower) behaviour, per `SPRINT_2_IMPLEMENTATION_PLAN.md`'s Unit D1 scope.
- Found the specification internally consistent (state model, event model, and Core Concepts all agree with each other and with what Sprint 1/2 actually built), with one stale-prose correction identified: `PlannerRuntimeSpecification.md` Section 6 claimed no `TaskProposalIntake` implementation existed; `InMemoryTaskManagerRuntime` (Sprint 1, Unit 6) had already closed that gap.
- Flagged, rather than closed, the one substantive prose-anticipated-capability gap: no code anywhere had exercised a real Plan Decision over more than one Plan Candidate — named as Unit D2's own scope in the design document's Section 11.
- Concluded "Track D is ready for implementation."

Implementation Notes
- Design/review-only unit: no source or test file was modified. `PlannerRuntimeSpecification.md` Section 6 was corrected in place after review acceptance (the one identified stale-prose fix), per explicit approval — this is a documentation correction, not an architecture change; the contract shape and disposition model it describes are unchanged.
- Section 11 of the design document ("Unit D2 Scope") is the direct, implementation-ready input to Unit D2 below.

---

### Sprint 3 Track D Unit D1A – Planner Runtime Contract Design

Commit:
pending

Completed:
2026-07-06

Android Studio Tests:
283/283 (unchanged — design-only unit; no Kotlin was written or modified during this unit itself)

Summary
- Added `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md`, a field-level architectural review of every public type the exploratory Unit D2 attempt introduced, performed after an architectural traceability review found that most of those types (all but the bare concept names "a `PlanCandidate` type" and "a concrete Plan Decision mechanism") were shaped during that implementation attempt rather than pre-specified by Unit D1.
- Approved as designed: `PlanCandidateId`, `PlanCandidate` (with its field set now explicitly justified as required-vs-optional-carry-forward), `PlanDecisionResult`, `PlanningSessionResult`, `PlannerSessionStatus`/`PlannerSessionLifecycleTransitions` (the last two explicitly flagged as new architecture this document grants, not something Unit D1 anticipated).
- Required revisions identified: `PlanRejection.reason: String` -> `reason: PlanRejectionReason` (a closed enum) + `detail: String`; `PlanDecision.decide` must become `suspend fun` (AD-10, Model Independence, future model-backed implementations); `PlanningRequest` must not carry `candidates` (candidates are generated by planning, not supplied by callers); a new `PlannerRuntime` interface must exist, and `InMemoryPlannerRuntime` must implement it, matching the pattern `TaskProposalIntake`/`AgentRunCommandChannel`/`IdentityService` already establish.
- Determined the exploratory Unit D2 implementation should be revised to match this contract design (not accepted as-is, not discarded and rewritten) and concluded "Unit D2 may proceed after aligning to this contract design."

Implementation Notes
- Design-only unit: no source or test file was modified by this unit itself. The exploratory Unit D2 implementation was treated as evidence for this review, not a pre-approved baseline.

---

### Sprint 3 Track D Unit D2 – Plan Decision Mechanism Implementation (Alignment Pass)

Commit:
pending

Completed:
2026-07-06

Android Studio Tests:
Not yet run by a human in Android Studio (PES-001: test execution and commit remain Human authority; no working Kotlin/Gradle toolchain was available in this session's sandbox either). Static count: the prior suite stood at 283/283; `DefaultPlanDecisionTest.kt` contributes 14 tests and `InMemoryPlannerRuntimeTest.kt` contributes 18 (one more than the original exploratory attempt, adding explicit `PlannerRuntime`-interface-conformance coverage; `FakeTaskProposalIntake.kt` remains a fixture, no tests of its own), a net addition of +32. If every existing and new test passes unchanged, the expected total is 315/315 — this figure is an arithmetic projection from the source, not a verified run, and must be confirmed in Android Studio before commit.

Summary
- Aligned the exploratory Unit D2 implementation to `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` (Unit D1A), per its own "revise to match the approved contracts" determination. The exploratory implementation was not discarded — its algorithm, event model, and test coverage all carried forward; only the following were changed:
  - `src/contracts/PlanDecision.kt`: `PlanRejection.reason: String` replaced with `reason: PlanRejectionReason` (a new closed, four-value enum: `DUPLICATE_CANDIDATE_ID`, `BLANK_GOAL`, `GOAL_MISMATCH`, `NOT_SELECTED`) plus a non-blank `detail: String`; `PlanDecision.decide` changed to `suspend fun`; `PlanningRequest` no longer carries `candidates`; a new `PlannerRuntime` interface added (`suspend fun plan(request: PlanningRequest, candidates: List<PlanCandidate>): PlanningSessionResult`).
  - `src/runtime/DefaultPlanDecision.kt`: `decide` is now `suspend` (mechanical change only; the body still never actually suspends), and constructs `PlanRejection` with the new `reason`/`detail` shape.
  - `src/runtime/InMemoryPlannerRuntime.kt`: now `class InMemoryPlannerRuntime(...) : PlannerRuntime`; `plan` takes `candidates` as a second parameter instead of reading it off `request`; `planner.candidate_rejected` events now carry `reason` (the enum's `.name`) and a separate `detail` payload entry.
  - `tests/runtime/DefaultPlanDecisionTest.kt` and `tests/runtime/InMemoryPlannerRuntimeTest.kt`: every call site updated for the new `plan(request, candidates)`/`decide` signatures (wrapped in `runTest` throughout, since `decide` is now `suspend`); every assertion that previously substring-matched a rejection's prose `reason` now checks the exact `PlanRejectionReason` value instead (a strictly stronger assertion, not a weaker one); one new test added confirming `InMemoryPlannerRuntime` is usable entirely through the `PlannerRuntime` interface type.
- `PlanCandidateId`, `PlanCandidate`'s field set, `PlanDecisionResult`, `PlanningSessionResult`, `PlannerSessionStatus`, and `PlannerSessionLifecycleTransitions` were confirmed to already match Unit D1A and were not changed.

Implementation Notes
- No additional public contract was introduced beyond what Unit D1A names. `getSessionStatus` remains a class-specific method on `InMemoryPlannerRuntime`, not part of the `PlannerRuntime` interface, mirroring `InMemoryTaskManagerRuntime.getTask`/`InMemoryAgentRuntime.getAgentRun`'s identical "observability method, not part of the formal interface" precedent.
- The Plan Decision algorithm itself (evaluate in generation order; duplicate/blank/mismatch/not-selected, in that precedence; no risk-based or other scoring) is unchanged, per this Unit's explicit "keep the deterministic Plan Decision algorithm unchanged unless required by D1A" instruction — D1A required no change to the algorithm itself, only to how a rejection's reason is represented.
- `PlannerRuntimeSpecification.md`, `TaskManagerRuntimeSpecification.md`, `TaskProposal.kt`, `TaskProposalIntake`, `AgentRunLifecycleTransitions`, `TaskLifecycleTransitions`, the Agent Runtime, the Execution Pipeline, the Permission Engine, the Tool Registry, and the Resource Registry were not modified.
- No Memory, World Model, LLM reasoning, resource optimisation, Workflow Engine, Android integration, or multi-agent planning was added.

---

### Sprint 4 Track A Unit A3 – Memory Runtime Implementation

Commit:
pending

Completed:
2026-07-06

Android Studio Tests:
Android Studio verified: 360/360 passing (Human authority, PES-001). `tests/contracts/MemoryContractsTest.kt` contributes 19 tests, `tests/runtime/DefaultMemoryPromotionPolicyTest.kt` contributes 10, and `tests/runtime/InMemoryMemoryStoreTest.kt` contributes 16 (`tests/runtime/FakeMemoryPromotionPolicy.kt` is a fixture, no tests of its own), a net addition of +45 over the prior static count of 315/315. This total has been run and confirmed in Android Studio; it is no longer a static projection. (The prior 315/315 figure itself was recorded as a static projection at the time of Unit D2 and is not separately re-asserted as independently verified by this correction — only the current, full 360/360 total is being reported as confirmed.)

Summary
- Implemented every contract `docs/architecture/MEMORY_CONTRACT_DESIGN.md` (Unit A2) approved as required, field-by-field, in `src/interfaces/MemoryStore.kt` (replacing the original four-operation, named-only stub in place): `MemoryId` (the established blank-rejecting identifier pattern); `MemoryCategory` (a single five-value enum: `EPISODIC`, `SEMANTIC`, `PROCEDURAL`, `USER_PREFERENCES`, `RELATIONSHIPS`); `CandidateMemory` (knowledge payload, proposed category, provenance, optional confidence, explicit-request flag, sensitivity flag); `MemoryRecord` (required identity/metadata, optional metadata, knowledge payload, and a plain-`List<String>` history); `MemoryPromotionDecision` (a two-variant sealed `Promote`/`Reject` outcome, `Reject.reason` a free-text `String`); `MemoryPromotionPolicy` (a `suspend fun evaluate(candidate, memoryId): MemoryPromotionDecision` seam); and `MemoryQuery` (requesting Principal, relevance text, optional category, correlation id, and a required, positive `maximumResults`).
- Added `src/runtime/DefaultMemoryPromotionPolicy.kt`: the concrete, deterministic `MemoryPromotionPolicy` this Unit supplies. Promotes unconditionally if `explicitlyRequested`; otherwise promotes if `confidence >= 0.7` (configurable); otherwise rejects with a plain-language reason. Implements 2 of `33-memory-consolidation.md`'s 6 named promotion factors (explicit request, confidence) — the remaining four (repetition, user importance beyond explicit request, goal relevance, frequency) are not implemented and are recorded as `IMPLEMENTATION_GAPS.md` #46, not silently dropped.
- Added `src/runtime/InMemoryMemoryStore.kt`: the first in-memory `MemoryStore` implementation. `remember` performs submission, Evaluation (via the injected `MemoryPromotionPolicy`, defaulted to `DefaultMemoryPromotionPolicy`), and Promotion in one call, returning the `MemoryPromotionDecision` directly — there is no separate, caller-facing "promote" operation. `retrieve` implements the minimal, deterministic matching this Unit is scoped to (Principal-scoped, category-narrowed, case-insensitive substring match on the knowledge payload, most-recently-promoted-first via internal insertion order, truncated to `maximumResults`) — `MemoryRetrievalPolicy` (a deferred seam) is not implemented. `forget` removes a record from retrieval and records the forgotten `MemoryId` in an internal, auditable set, inspectable via the class-specific `wasForgotten` method (not part of `MemoryStore`, mirroring `InMemoryPlannerRuntime.getSessionStatus`/`InMemoryTaskManagerRuntime.getTask`'s identical "observability method outside the formal interface" precedent).
- Added `tests/contracts/MemoryContractsTest.kt` (construction-time validation for `MemoryId`, `CandidateMemory`, `MemoryRecord`, `MemoryQuery`, `MemoryPromotionDecision`), `tests/runtime/DefaultMemoryPromotionPolicyTest.kt` (the two-factor evaluation rule, determinism, custom threshold), `tests/runtime/InMemoryMemoryStoreTest.kt` (promotion approved/rejected, retrievability following the decision, category narrowing, `maximumResults`, deterministic ordering, Principal scoping, forgetting and its auditability, forgetting a nonexistent id, the `MemoryStore` public-surface boundary, internal-only `MemoryPromotionPolicy` invocation, and scope discipline), and `tests/runtime/FakeMemoryPromotionPolicy.kt` (a lambda-based test fixture mirroring `FakePermissionEngine`'s precedent).

Implementation Notes
- `src/interfaces/MemoryStore.kt`'s original stub conflicted with the approved contract design in three ways, each corrected in place rather than preserved: (1) the promoted-record type is now named `MemoryRecord`, not `Memory` (`MEMORY_CONTRACT_DESIGN.md` §3's naming clarification); (2) `promote(memoryId: MemoryId): Memory` no longer exists as a public, caller-facing operation — `remember` now expresses submission, Evaluation, and Promotion as one caller-facing step, per `MEMORY_CONTRACT_DESIGN.md` §9's architectural decision that external callers never invoke promotion directly; (3) `forget`'s return type is a plain `Boolean`, not a `ForgetResult` — `ForgetResult` was never one of Unit A2's eight approved required contracts, and shaping it now would have introduced an unauthorised ninth contract.
- No excluded contract (`CandidateMemoryId`, `MemoryQueryResult`, `MemoryRuntime`, `MemoryObservation`) was implemented. No deferred seam (`MemoryRetrievalPolicy`, the combined retention/consolidation seam) was implemented; `retrieve`'s minimal matching is hard-coded inside `InMemoryMemoryStore`, not a pluggable policy.
- No dependency on the Planner Runtime, the Agent Runtime, the Permission Engine, or the Event Bus was introduced. `InMemoryMemoryStore`'s constructor takes only a `MemoryPromotionPolicy` (defaulted). No Memory-to-World-Model mutation and no World-Model-to-Memory automatic copying exist anywhere in this Unit's code.
- `docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md`, `docs/architecture/MEMORY_CONTRACT_DESIGN.md`, the Parker Constitution, the Architecture Decisions, and PES-001 were not modified.

---

### Sprint 4 Track B Unit B3 – World Model Runtime Implementation

Commit:
pending

Completed:
2026-07-06

Android Studio Tests:
Android Studio verified: 413/413 passing (Human authority, PES-001). Built on the confirmed 360/360 total after Unit A3, plus 53 newly added tests (`tests/contracts/WorldModelContractsTest.kt`: 19, `tests/runtime/DefaultWorldModelUpdatePolicyTest.kt`: 12, `tests/runtime/InMemoryWorldModelTest.kt`: 22; `tests/runtime/FakeWorldModelUpdatePolicy.kt` is a fixture, no tests of its own). This total has been run and confirmed in Android Studio; it is no longer a projection.

Summary
- Implemented every contract `docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md` (Unit B2) approved as required, field-by-field, in `src/interfaces/WorldModel.kt` (replacing the original three-operation, named-only stub in place): `WorldBelief` (renamed from `WorldState`; subject, value, confidence [required], timestamp, source, optional derivedFrom); `WorldObservation` (subject, confidence [required], source, optional value [required unless retracting], optional sourceTimestamp, optional derivedFrom, retraction indicator); `ObservationResult` (a three-variant sealed outcome: `Accepted`, `Invalidated`, `Rejected` with a free-text reason); `WorldQuery` (subjectMatch, maximumResults, optional minimumConfidence — deliberately no requesting-Principal field or correlation identifier); and `WorldModelUpdatePolicy` (a `suspend fun evaluate(observation, existing): ObservationResult` plus a `suspend fun isStillCurrent(belief): Boolean` seam). `current`'s parameter changed from `resourceId: ResourceId` to a plain, non-blank subject `String`, per Unit B2's Resource Identity resolution.
- Added `src/runtime/DefaultWorldModelUpdatePolicy.kt`: the concrete, deterministic `WorldModelUpdatePolicy` this Unit supplies. A retraction invalidates an existing belief or is rejected if none exists; absent any existing belief, the first valid Observation is always accepted; otherwise an Observation is accepted only at or above the existing belief's confidence, and rejected (with reason) if weaker. `isStillCurrent` compares a belief's timestamp against now, bounded by a configurable `staleAfter` (default 15 minutes) — a pure, read-time check with no background sweep. Implements exactly the minimal deterministic rule set this Unit's own instructions named; no probabilistic reasoning or sensor fusion.
- Added `src/runtime/InMemoryWorldModel.kt`: the first in-memory `WorldModel` implementation. `observe` performs Validation, evaluation (via the injected `WorldModelUpdatePolicy`, defaulted to `DefaultWorldModelUpdatePolicy`), and Update/Invalidation in one call, guarded by an internal `Mutex` so concurrent Observations for the same subject are resolved entirely inside the class. `current` and `query` both consult `WorldModelUpdatePolicy.isStillCurrent` before returning a belief, lazily excluding any that have become stale; `query` additionally applies a case-insensitive subject-substring match and an optional minimum-confidence filter, truncated to `maximumResults`, with no ranking algorithm.
- Added `tests/contracts/WorldModelContractsTest.kt` (construction-time validation for `WorldBelief`, `WorldObservation`, `ObservationResult`, `WorldQuery`), `tests/runtime/DefaultWorldModelUpdatePolicyTest.kt` (the four-rule evaluation, determinism, custom `staleAfter`), `tests/runtime/InMemoryWorldModelTest.kt` (acceptance, replacement, rejection, invalidation, timestamp ownership, derived-belief carry-forward, query matching/bounds/filtering, lazy expiry, internal-only policy invocation, the `WorldModel` public-surface boundary, real concurrent-submission safety, and structural proof that no excluded/deferred type exists), and `tests/runtime/FakeWorldModelUpdatePolicy.kt` (a lambda-based test fixture mirroring `FakeMemoryPromotionPolicy`'s precedent).

Implementation Notes
- `src/interfaces/WorldModel.kt`'s original stub conflicted with the approved contract design in two ways, each corrected in place rather than preserved: (1) the current-belief type is now named `WorldBelief`, not `WorldState` (`WORLD_MODEL_CONTRACT_DESIGN.md`'s naming resolution); (2) `current(resourceId: ResourceId)` is now `current(subject: String)` — not every Information Category maps onto a registered Resource, so the World Model no longer requires Resource registration for every subject it can hold a belief about.
- No excluded contract (`WorldModelUpdateDecision`, `WorldModelRuntime`) was implemented. No deferred addition (a belief-category enumeration, a `WorldModelPolicy` bounded-configuration record, richer derivation lineage, pagination/ranking metadata, a requesting-Principal field, or a correlation identifier on `WorldQuery`) was implemented.
- `WorldModelUpdatePolicy` is invoked only from inside `InMemoryWorldModel`; no public `WorldModel` member reaches it, and `DefaultWorldModelUpdatePolicy` constructs the accepted `WorldBelief` (including its authoritative timestamp) as part of evaluation — a disclosed interpretation of `WORLD_MODEL_CONTRACT_DESIGN.md` §5's "does not itself construct a `WorldBelief`" language, read as a boundary against an external actor constructing one, not against any class inside the World Model's own, entirely internal implementation. See this Unit's Post-Implementation Review for the full reasoning.
- No `EventBus` publication was added. `WorldModel.md` names "Publish state change events" as a Responsibility; this Unit reports, rather than resolves, how that could be added without granting orchestration authority (see `docs/architecture/IMPLEMENTATION_GAPS.md` #47) and does not implement it.
- No dependency on Memory, the Planner Runtime, the Agent Runtime, or the Permission Engine was introduced. `InMemoryWorldModel`'s constructor takes only a `WorldModelUpdatePolicy` (defaulted).
- `docs/architecture/WORLD_MODEL_RUNTIME_ARCHITECTURE.md`, `docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md`, the Parker Constitution, the Architecture Decisions, and PES-001 were not modified.

---

### Pre-Module Readiness Unit 1 -- Planner Runtime Publisher Identity (gap #49)

Commit:
pending

Completed:
2026-07-07

Android Studio Tests:
Not yet run by a human in Android Studio (PES-001: test execution and commit remain Human authority; no working Kotlin/Gradle toolchain was available in this session's sandbox either). Static count: the prior confirmed total stood at 413/413 (Sprint 4, Track B, Unit B3); `tests/runtime/InMemoryPlannerRuntimeTest.kt` gains 3 new tests (publisher identity resolves and `plan` proceeds; an unresolvable publisher identity produces a safe `Failed` result with no session record and no events published; published events carry the resolved publisher Principal), a net addition of +3. If every existing and new test passes unchanged, the expected total is 416/416 -- this figure is an arithmetic projection from the source, not a verified run, and must be confirmed in Android Studio before commit.

Summary
- Closed `docs/architecture/IMPLEMENTATION_GAPS.md` #49 (Planner Runtime's publisher identity was hardcoded and never resolved through `IdentityService`, unlike Agent Runtime's own `agentIdentityPrincipalId`), per the independent architecture audit's triage (`docs/reviews/ARCHITECTURE_V2_INDEPENDENT_AUDIT_TRIAGE.md` Finding 2) and Pre-Module Readiness Unit 1's own scope.
- `src/runtime/InMemoryPlannerRuntime.kt`: `plan` now calls `identityService.resolve(PLANNER_RUNTIME_PRINCIPAL_ID)` before publishing anything, checked before the pre-existing initiating-Principal resolution check. If unresolved, `plan` rolls back its tentative session reservation and returns `PlanningSessionResult.Failed` with an explicit reason -- no session record is created, no event is published, exactly mirroring the pre-existing unresolvable-initiating-Principal precondition and `InMemoryAgentRuntime`'s own `agentIdentityPrincipalId` precedent. The resolved `Principal.principalId` (not the raw `PLANNER_RUNTIME_PRINCIPAL_ID` constant) is threaded explicitly through `publish`/`publishRejections` for the remainder of that `plan` call.
- `tests/runtime/InMemoryPlannerRuntimeTest.kt`: added `identityServiceWithPlannerRegistered()`, an `InMemoryIdentityService` with `system.planner-runtime` pre-registered as a `SYSTEM` Principal; every existing test that expects `plan` to proceed now uses it in place of a bare `InMemoryIdentityService()` (mechanical update, no behavioural change to what those tests assert). Added three new tests proving: resolution succeeds and `plan` proceeds when the identity is registered; an unregistered publisher identity produces a `Failed` result with no session record and no events published; and every published event's `publisherPrincipalId` equals the resolved identity, not a hardcoded value. The two pre-existing "unresolvable initiating Principal" tests were tightened to assert on `"initiatingPrincipalId"` specifically, so they remain isolated to that failure path now that a second, distinct identity-resolution failure path exists.

Implementation Notes
- No public contract changed. `PlanningSessionResult.Failed` already existed with a `reason` field; no new variant or field was added.
- No EventBus authentication change. `InMemoryEventBus`'s `AllowAllPrincipalAuthenticator` is untouched.
- No Planner Runtime redesign. The Plan Decision algorithm, Task Proposal construction, lifecycle transitions, and event sequence are all unchanged; only the publisher-identity precondition was added, following `InMemoryAgentRuntime`'s existing pattern exactly.
- No module access was introduced.
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`, `docs/architecture/ARCHITECTURE_DECISIONS.md`, and every ADR were not modified.

---

### Pre-Module Readiness Unit 2 -- ID Multiplicity Decision (gap #48)

Commit:
pending

Completed:
2026-07-07

Android Studio Tests:
Not yet run by a human in Android Studio (PES-001: test execution and commit remain Human authority; no working Kotlin/Gradle toolchain was available in this session's sandbox either). Static count: the prior static projection stood at 416/416 (Pre-Module Readiness Unit 1); `tests/runtime/InMemoryAgentRuntimeTest.kt` gains 1 new test and `tests/runtime/InMemoryPlannerRuntimeTest.kt` gains 1 new test, a net addition of +2. If every existing and new test passes unchanged, the expected total is 418/418 -- this figure is an arithmetic projection from the source, not a verified run, and must be confirmed in Android Studio before commit.

Summary
- Created `docs/architecture/PRE_MODULE_ID_MULTIPLICITY_DECISION.md`, a design decision (performed before any Kotlin change, per this unit's own instruction) resolving `docs/architecture/IMPLEMENTATION_GAPS.md` #48 (deterministic parent-derived IDs capping `AgentRunId`/`TaskProposalId` multiplicity, per the independent architecture audit's Finding 1). Decision: **Option B** -- formally constrain the current platform phase to one Agent Run per Task and one Task Proposal per Planning Session, as a deliberate, documented decision. Multiplicity is **deferred**, not prohibited: no consumer in this repository today (no Workflow Engine, no retry logic, no Multi-agent planning/Resource optimisation) needs it, so implementing general multiplicity support now would be unvalidated speculative generality, rejected under the same "100,000-line test" already applied in `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`.
- `src/runtime/InMemoryAgentRuntime.kt` and `src/runtime/InMemoryPlannerRuntime.kt`: no functional change to when or how either duplicate-submission exception is thrown. Both exception messages, and the KDoc/comments at each ID-minting site, were updated to state explicitly that the one-per-parent cap is a deliberate, documented constraint (citing gap #48 and the decision document by name), not an accidental limitation.
- `tests/runtime/InMemoryAgentRuntimeTest.kt` and `tests/runtime/InMemoryPlannerRuntimeTest.kt`: the pre-existing "resubmitting..." tests for each subsystem were tightened to assert on (unchanged-behaviour, updated-message) content; one new test per file asserts the message explicitly cites this decision and gap #48.
- Closed `docs/architecture/IMPLEMENTATION_GAPS.md` #48 as "formally constrained, deferred" -- not "fixed," since no defect existed, only an undisclosed decision.

Implementation Notes
- No public contract changed: `AgentRunId`, `TaskProposalId`, `AgentRunCommand`, and `PlanningRequest` are unchanged in shape.
- No ID-generation mechanism changed. Both IDs remain deterministic, parent-derived strings -- the decision document's own Section 3 explains why that is the *correct* shape for "exactly one child per parent," and records (for a future unit, not for this one) that a per-parent monotonic counter, not a generated/random or caller-supplied identifier, is the recommended shape if and when multiplicity is eventually implemented.
- No retry, forking, or multi-instance orchestration logic was introduced in either runtime.
- No module access of any kind was introduced.
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`, `docs/architecture/ARCHITECTURE_DECISIONS.md`, every ADR, `TaskManagerRuntimeSpecification.md`, and `PlannerRuntimeSpecification.md` were not modified -- both specifications' own "zero, one, or many"/"one or more" language is deliberately left open, not narrowed.

---

### Pre-Module Readiness Unit 3 -- Module, Event, Audit, and Durability Boundary (ADR-024)

Commit:
pending

Completed:
2026-07-07

Android Studio Tests:
418/418 (unchanged -- design-only unit; no Kotlin was written or modified).

Summary
- Created `docs/adr/ADR-024-module-event-audit-durability-boundary.md`, an ADR/design unit only, addressing `docs/architecture/IMPLEMENTATION_GAPS.md` #47 (World Model event publication), #50 (EventBus delivery isolation), and #51 (persistence/durability/audit boundary), plus a module-access boundary not previously defined anywhere in this repository.
- Decided: modules are capability/Tool providers by constitutional definition, may also be read-only event subscribers under ADR-023's existing discipline, and are never granted a fourth category of implicit authority (Section A). World Model's event-publication shape is reaffirmed as already governed by ADR-023; Memory Runtime is explicitly not authorised to publish events at this time, for lack of both a named specification responsibility and a present consumer (Section B). EventBus remains synchronous for today's fast, in-process subscriber set; per-subscriber isolated dispatch (not a durable queue) is the target semantic, required before any slow/blocking subscriber -- Audit or module -- is added (Section C). Memory Records, Principal records, and an Audit log must eventually be durable; World Model beliefs and ordinary working state may remain in-memory; Memory may not be treated as durable across a restart, and no document may claim AD-009 is satisfied in a durable sense, until a real persistence/Audit mechanism exists (Section D). A two-bucket pre-module rule is recorded: stateless, non-subscribing, non-Memory/World-Model-reading capability providers may be introduced without waiting on any of the three gaps; subscribing, World-Model-dependent, or durability-dependent modules must wait on the corresponding gap; no module is ever granted implicit trust or a bypass of the Permission Engine (Section E).
- Updated `docs/architecture/IMPLEMENTATION_GAPS.md` #47, #50, and #51 to record that each now has an architectural decision governing its eventual implementation -- none was implemented, and none was closed.

Implementation Notes
- No Kotlin, test, or existing specification was modified. `InMemoryWorldModel.kt`, `InMemoryEventBus.kt`, `InMemoryMemoryStore.kt`, and `InMemoryIdentityService.kt` are all unchanged.
- No module access was introduced -- no module system exists in this repository, and this ADR authorises none; it defines the boundary a future module-access proposal must respect.
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md` and `docs/architecture/ARCHITECTURE_DECISIONS.md` were not modified. ADR-023 was not modified -- ADR-024 reaffirms and builds on it without reopening it.

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
- Agent Runtime now supports multi-step execution and `START`/`SUSPEND`/`RESUME`/`CANCEL` (Sprint 3, Unit C2); it still has no Planner, no `WAITING_FOR_INPUT`, no Agent Capability system, no `maxAgentRunDuration` enforcement, no World Model or Memory integration, no Workflow Engine, and no cross-Agent orchestration.
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
