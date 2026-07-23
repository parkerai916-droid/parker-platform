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

### Sprint 6 Track A Unit M1 -- Module Registry Runtime

Commit:
pending

Completed:
2026-07-07

Android Studio Tests:
Not yet run by a human in Android Studio (PES-001: test execution and commit remain Human authority; no working Kotlin/Gradle toolchain was available in this session's sandbox either). Static count: the prior confirmed/unchanged total stood at 418/418 (Pre-Module Readiness Unit 3); `tests/runtime/InMemoryModuleRegistryTest.kt` adds 23 new tests, a net addition of +23. If every existing and new test passes unchanged, the expected total is 441/441 -- this figure is an arithmetic projection from the source, not a verified run, and must be confirmed in Android Studio before commit.

Summary
- Implemented every contract `docs/architecture/MODULE_CONTRACT_DESIGN.md` approved as required, field-by-field, in `src/contracts/Module.kt`: `ModuleId` (the established blank-rejecting identifier pattern, caller-declared rather than runtime-minted); `ModuleConnectivityDeclaration` (`LOCAL_ONLY`/`CLOUD_CAPABLE`/`CLOUD_REQUIRED`); `ModulePermissionRequirement` (a `PermissionAction`/`ResourceType` pair); `ModuleDescriptor` (moduleId, name, version, `toolsExposed: List<ToolDescriptor>`, `requiredPermissions`, `connectivityDeclaration`, `eventSubscriptions` defaulted empty, optional `minimumPlatformVersion`); `ModuleStatus` (exactly four tracked values: `REGISTERED`, `ENABLED`, `DISABLED`, `REMOVED`); and `ModuleLifecycleTransitions` (the exact adjacency Contract Design Section 4 specifies).
- Added `src/interfaces/ModuleRegistry.kt`: the single public Module Framework interface (`register`/`enable`/`disable`/`remove`/`getModuleDescriptor`/`getModuleStatus`/`listModules`), with no separate `ModuleRuntime`, mirroring `MemoryRuntime`'s identical exclusion. Error handling mirrors `IdentityService.register`/`updateStatus` and `ToolRegistry.setLifecycleState`'s existing throw-based precedent exactly (`IllegalArgumentException` on a duplicate `moduleId` or an illegal lifecycle edge, `NoSuchElementException` for an unknown `moduleId`) rather than introducing a new sealed Accepted/Rejected outcome type -- Contract Design left this exact Kotlin shape undecided, and this is the minimal, precedent-consistent choice.
- Added `src/runtime/InMemoryModuleRegistry.kt`: the first in-memory `ModuleRegistry` implementation. `register` rejects a duplicate `moduleId` and, for each of a module's declared `toolsExposed`, registers a backing `Resource` (`ResourceType.TOOL`) and then the `ToolDescriptor` itself with the injected `ToolRegistry`, tracking each Tool's resulting `ToolLifecycleState` locally. `enable`/`disable` drive every tracked Tool between `REGISTERED`/`DISABLED` and `ENABLED` alongside the module's own `ModuleStatus` transition. `remove` drives every tracked Tool to `REMOVED` via the shortest legal `ToolLifecycleTransitions` path from its current tracked state. `getModuleDescriptor`/`getModuleStatus`/`listModules` are plain map reads guarded by the same `Mutex` pattern as every other `InMemory*` runtime class.
- Added `tests/runtime/InMemoryModuleRegistryTest.kt` (23 tests): registration (incl. no-tools modules, duplicate `moduleId` rejection, Tool Registry wiring, a Tool-descriptor conflict, an exact-duplicate `AlreadyRegistered` conflict, and the disclosed multi-tool non-atomicity case), lifecycle enforcement for `enable`/`disable`/`remove` (every legal edge, every illegal edge this Unit's own instructions named -- enabling an already-`ENABLED` module, disabling a never-enabled module, removing directly from `ENABLED`, removing an already-`REMOVED` module -- and unknown-`moduleId` throws for each operation), re-enabling after disable, lookup (`null` for unregistered, `listModules` including `REMOVED` modules), and two constitutional-boundary tests (`requiredPermissions` are stored and returned unchanged by `enable`, never mutated into a grant; a module's Tool becomes reachable only through `ToolRegistry.resolve`, never through any `ModuleRegistry` method).

Implementation Notes
- No architecture or contract was redesigned. `MODULE_FRAMEWORK_ARCHITECTURE.md`, `MODULE_CONTRACT_DESIGN.md`, `ARCHITECTURE_V2_FROZEN_BASELINE.md`, and PES-001 were not modified.
- No plugin, module loading, module discovery, or dependency-injection framework was implemented. `src/interfaces/Plugin.kt` is untouched and remains excluded from the build.
- No Home Assistant, Weather, or Gmail integration was implemented.
- No live `PermissionEngine` gating of `enable`/`disable`/`remove` was wired in -- a disclosed scope reduction mirroring `IMPLEMENTATION_GAPS.md` #24's identical, pre-existing treatment for Tool Registry's own registration/lifecycle operations. `requestingPrincipalId` is accepted and threaded through but not evaluated against any Identity or Permission check in this Unit.
- The module itself is not registered as an `IdentityService` Principal by this Unit. `ModuleId(moduleId.value)`-derived `PrincipalId`s are used only as the nominal `ownerPrincipalId` on each Tool's backing `Resource`, not as a verified identity.
- `docs/architecture/IMPLEMENTATION_GAPS.md` #52 was added, recording the interpretive decisions this Unit's Resource/Tool wiring required (owner-Principal scheme, sensitivity default, multi-tool registration non-atomicity, and locally-tracked Tool lifecycle state going stale under a cross-module version collision) and the disclosed Permission-Engine deferral above.

---

## Sprint 7

### Sprint 7 Track B Unit C1 -- Communication Runtime (CommunicationIntake) Implementation

Commit:
pending

Completed:
2026-07-07

Android Studio Tests:
Not yet run by a human in Android Studio (PES-001: test execution and commit remain Human authority; no working Kotlin/Gradle toolchain was available in this session's sandbox either). Static count: the prior static projection stood at 441/441 (Sprint 6, Track A, Unit M1, itself unconfirmed by a human at time of writing). `tests/contracts/CommunicationContractsTest.kt` adds 11 tests and `tests/runtime/InMemoryCommunicationIntakeTest.kt` adds 15, a net addition of +26. If every existing and new test passes unchanged, the expected total is 467/467 -- this figure is an arithmetic projection from the source, not a verified run, and must be confirmed in Android Studio before commit.

Summary
- Implemented exactly what `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`'s Conclusion authorises for a first Communication Runtime unit: `CommunicationIntake` (`src/interfaces/CommunicationIntake.kt`) and its supporting contracts -- `CorrelationId`, `InboundOwnerMessage`, `OutboundParkerResponse`, `CommunicationIntakeDisposition` (a two-variant `Accepted`/`Rejected` sealed type, mirroring `TaskProposalDisposition`'s shape, not its five-way richness) -- and its first implementation, `InMemoryCommunicationIntake` (`src/runtime/InMemoryCommunicationIntake.kt`).
- `InMemoryCommunicationIntake.submitInboundMessage` performs exactly Contract Design Section 6's two structural checks, in the order that section names them: (1) `message.channelId` must be a currently `ENABLED` module per the injected `ModuleRegistry`; (2) `message.senderPrincipalId` must resolve to a registered `Principal` per the injected `IdentityService`. Both pass -> `Accepted`; either fails -> `Rejected` with a plain-language reason. Per Section 5, a resolved sender is not additionally required to be `ACTIVE` or `USER`-typed.
- Every accepted `InboundOwnerMessage` is retained in an internal, mutex-guarded list, inspectable via `acceptedMessages()`/`acceptedMessageFor(correlationId)` -- observability methods outside the formal `CommunicationIntake` interface, mirroring `InMemoryMemoryStore.wasForgotten`'s established precedent. This is exactly the shape Contract Design's own Conclusion names as sufficient for a first unit ("making accepted ones inspectable").
- Added `tests/contracts/CommunicationContractsTest.kt` (11 tests: construction-time validation for `CorrelationId`, `InboundOwnerMessage`, `OutboundParkerResponse`, `CommunicationIntakeDisposition`) and `tests/runtime/InMemoryCommunicationIntakeTest.kt` (15 tests: the successful path, both structural checks individually and in combination across every `ModuleStatus` value, check ordering, deterministic and reproducible rejection reasons, thread safety under 50 concurrent submissions, the observability lookup surface, and two scope-discipline tests proving no `ExecutionPipeline`/`ToolRegistry`/`PlannerRuntime`/`AgentRuntime`/`MemoryStore`/`WorldModel` dependency was introduced).

Implementation Notes
- **A real scope conflict between this Unit's own task brief and the governing architecture was surfaced and resolved by explicit human decision before any code was written, not silently picked either way.** The task brief describing this Unit asked for a Communication Runtime that "constructs the appropriate Execution Request," "submits work through the existing Execution Pipeline," "awaits completion," and "returns a Communication Response." `COMMUNICATION_CONTRACT_DESIGN.md`'s own Conclusion explicitly forbids exactly that for a first unit: "Implementation must **not** attempt to resolve, in the same unit, either open item Section 14 names (Cognition's consumption mechanism, or `ExecutionRequest`'s content-carrying gap)... A first implementation unit may reasonably stop at: `CommunicationIntake` accepting or rejecting inbound messages and making accepted ones inspectable... with actual Cognition consumption and actual Tool-based response delivery following once their own respective open items are resolved by a future, separately-scoped unit." Presented as a three-way choice (build only what's authorised / build the Section-7 outbound-delivery path instead / build the full brief as written, exceeding current authorisation) -- the human decision was to build only what Contract Design currently authorises. This Unit therefore does not construct or reference an `ExecutionRequest`, and does not call `ExecutionPipeline`, `PermissionEngine`, or `ToolRegistry`, anywhere.
- `CommunicationIntake` has exactly two collaborators -- `ModuleRegistry` and `IdentityService` -- per Contract Design Section 9. No `PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or `WorldModel` dependency was introduced; engaging any of those remains Cognition's own decision, downstream of an accepted message, per Section 9.
- No Communication Channel module, local text channel, speech, Android, Home Assistant, notification, or LLM/Cognition behaviour was implemented -- all explicitly out of scope per Contract Design Section 11 and this platform's `IMPLEMENTATION_ORDER.md`.
- No existing architecture document, ADR, runtime contract, module contract, or `IMPLEMENTATION_GAPS.md`/`IMPLEMENTATION_HISTORY.md` entry was modified except to record this Unit's own completion and add gap #53 below.
- `docs/architecture/IMPLEMENTATION_GAPS.md` #53 was added, recording that Response Delivery (constructing an `ExecutionRequest` and submitting it through `ExecutionPipeline` for an `OutboundParkerResponse`) and Cognition's consumption of an accepted `InboundOwnerMessage` both remain unimplemented, deferred exactly as Contract Design's own Section 14 already disclosed -- not a defect this Unit introduced, and not silently worked around.

---

### Sprint 7 -- Task Event Payload Completion (Task Manager Runtime; closes `IMPLEMENTATION_GAPS.md` #43, in part)

Commit:
pending

Completed:
2026-07-07

Android Studio Tests:
Android Studio verified: **482/482 passing** (Human authority, PES-001), confirmed by Steven. Prior confirmed total (Sprint 7, Unit C3 -- Local Text Channel) was 480/480, per Steven's own confirmation; that Unit's own entry was not separately recorded in this file, since its Kotlin verification occurred outside this session's own sandbox and this entry is scoped only to Task Event Payload Completion. This Unit adds exactly 2 new test methods (see Summary), a net addition of +2, for the confirmed 482/482 total above. This total has been run and confirmed in Android Studio; it is no longer a static projection.

Summary
- Closed the `task.completed` half of `IMPLEMENTATION_GAPS.md` #43 exactly as `docs/implementation/TASK_EVENT_PAYLOAD_COMPLETION_IMPLEMENTATION_PLAN.md` specifies: `InMemoryTaskManagerRuntime.applyCompletedTransition`'s two `task.completed` publish call sites (`src/runtime/InMemoryTaskManagerRuntime.kt`) now carry a payload of `{"taskId": <the Task's own id>, "status": "COMPLETED"}`, satisfying `TaskManagerRuntimeSpecification.md` §10's "Task Result summary" requirement at the level this class has evidence for -- the terminal status it reaches, since it tracks no Execution Reference or Agent Result of its own.
- `task.started`'s publish call is unchanged -- its payload remains `emptyMap()`, exactly as before this Unit. Per the Implementation Plan's Section 8 decision, this Unit does not populate an Agent Run Reference: `InMemoryTaskManagerRuntime` has no field carrying a real `AgentRunId` (confirmed by direct inspection: `InMemoryAgentRuntime.publish`'s own `agent.completed` payload carries only `taskId`, never `agentRunId`), and closing this would require either reconstructing `AgentRunId` locally (rejected -- inferring a hidden identifier by duplicating another subsystem's internal ID-minting scheme) or modifying `InMemoryAgentRuntime.kt` directly (out of this Unit's scope). Neither was done.
- Extended `tests/runtime/InMemoryTaskManagerRuntimeTest.kt`'s two existing Unit B2 tests (`` `agent-completed for a QUEUED Task publishes both task-started and task-completed, proving both edges fired` `` and `` `agent-completed transitions an already-RUNNING Task to COMPLETED, taking only the second edge` ``) to assert `task.completed`'s exact payload contents (and, in the first, `task.started`'s continued emptiness). Added two new, dedicated tests: `` `task-completed's payload never claims an Execution Reference or Agent Result field this class does not track` `` (a scope-discipline proof that the payload contains exactly `{taskId, status}` and nothing fabricated) and `` `task-started's payload remains deliberately empty -- Agent Run Reference is an intentional deferral, not an oversight` `` (an explicit proof that the omission is intentional, not untested).

Implementation Notes
- No architecture, contract, or implementation plan document was modified. `TaskManagerRuntimeSpecification.md`, `docs/implementation/TASK_EVENT_PAYLOAD_COMPLETION_IMPLEMENTATION_PLAN.md`, and every other existing specification remain exactly as they were.
- `InMemoryAgentRuntime.kt` was not modified. No `AgentRunId` was reconstructed, guessed, or inferred anywhere in this Unit.
- `Task.schema.json` was not modified; "Task Result" remains an informational `Map<String, String>` event-payload entry, not a promoted, structured schema field -- `TaskManagerRuntimeSpecification.md`'s own "Open Questions" section leaves that promotion undecided (subject to ADR-019), and this Unit does not decide it.
- `docs/architecture/IMPLEMENTATION_GAPS.md` #43 was **clarified, not closed** -- see that entry's own updated text. The `task.completed`/Task Result summary half is now implemented and verified; the `task.started`/Agent Run Reference half remains open, depending on future Agent Runtime support (either extending `agent.completed`'s own payload, or some other, not-yet-designed mechanism) that is explicitly out of this Unit's scope.
- No other implementation gap, Communication Runtime, Local Text Channel, Cognition, Planner Runtime, Memory Runtime, or World Model file was touched.

---

### Sprint 7 -- Agent Run Reference Exposure (Agent Runtime + Task Manager Runtime; closes `IMPLEMENTATION_GAPS.md` #43, in full)

Commit:
pending

Completed:
2026-07-08

Android Studio Tests:
Android Studio verified: **484/484 passing** (Human authority, PES-001), confirmed by Steven. Prior confirmed total (Sprint 7, Task Event Payload Completion) was 482/482. This Unit adds 2 new test methods to `tests/runtime/InMemoryAgentRuntimeTest.kt` and a net 0 change to `tests/runtime/InMemoryTaskManagerRuntimeTest.kt` (one new test added, one superseded test removed -- see Summary), a net addition of +2, for the confirmed 484/484 total above. This total has been run and confirmed in Android Studio; it is no longer a static projection.

Summary
- Closed the remaining `task.started` half of `IMPLEMENTATION_GAPS.md` #43 exactly as `docs/implementation/AGENT_RUN_REFERENCE_EXPOSURE_IMPLEMENTATION_PLAN.md` specifies. `InMemoryAgentRuntime`'s single shared `publish(run: AgentRun, eventType: String)` helper (`src/runtime/InMemoryAgentRuntime.kt`) now includes `"agentRunId" to run.agentRunId.value` in every event's payload, alongside the existing `"taskId"` entry. Because every one of this class's currently-emitted event types already routes through this one shared helper, `agentRunId` is now exposed **uniformly across all of them** -- not `agent.completed` alone -- confirmed by a dedicated test proving `agent.created` (a non-terminal event) carries it too.
- `InMemoryTaskManagerRuntime.applyCompletedTransition` (`src/runtime/InMemoryTaskManagerRuntime.kt`) now reads `event.payload["agentRunId"]` off the triggering `agent.completed` event -- the same event it already receives as its own handler parameter, subscribed to since Unit B1 (`IMPLEMENTATION_GAPS.md` #42) -- and threads it into `task.started`'s payload as `mapOf("agentRunId" to it)`. If the triggering event carries no `agentRunId` entry, `task.started`'s payload remains `emptyMap()`, matching `TaskManagerRuntimeSpecification.md` §10's own "if any" language and this class's established missing-field handling.
- **`correlationId` behaviour is unchanged throughout.** Neither `InMemoryAgentRuntime.publish` nor `InMemoryTaskManagerRuntime.applyCompletedTransition`/`publish` alters how `correlationId` is set or threaded on any event. The pre-existing, documented tension between `AgentRuntimeSpecification.md` Section 9's prose ("`correlationId` set to the Agent Run ID") and `AgentRun.kt`'s own KDoc (`correlationId` as the shared, cross-subsystem value, not literally `AgentRunId.value`) is untouched by this Unit -- `agentRunId` is exposed through a separate, additive payload key, not through any change to `correlationId`.
- **No `AgentRunId` is reconstructed anywhere.** `InMemoryTaskManagerRuntime` computes, guesses, or derives nothing; the value it threads into `task.started` is read directly from the incoming event's own payload.
- Extended `tests/runtime/InMemoryAgentRuntimeTest.kt`'s `buildRuntime` test helper to return a new `RuntimeFixture` (replacing `Triple`) so tests can reach the `EventBus` it already constructs internally; every pre-existing 3-component destructuring call site is unaffected (Kotlin destructuring is positional). Added `` `agent-completed's published payload carries the real, returned AgentRunId` `` and `` `agent-created's published payload also carries agentRunId, proving exposure is uniform, not agent-completed alone` ``.
- Extended `tests/runtime/InMemoryTaskManagerRuntimeTest.kt`'s `agentEvent(...)` fixture with an optional, defaulted `agentRunId` parameter (every pre-existing call site unaffected). Extended `` `agent-completed for a QUEUED Task publishes both task-started and task-completed, proving both edges fired` `` to assert a populated `task.started` payload. Added `` `agent-completed with no agentRunId payload entry leaves task-started's payload empty, not a fabricated value` ``. The now-superseded `` `task-started's payload remains deliberately empty -- Agent Run Reference is an intentional deferral, not an oversight` `` test (Task Event Payload Completion Unit) was replaced with an explanatory `NOTE` comment, not silently deleted, mirroring this file's own established Unit B1-->B2 supersession convention.

Implementation Notes
- No architecture, contract, or implementation plan document was modified. `AgentRuntimeSpecification.md`, `TaskManagerRuntimeSpecification.md`, `src/contracts/AgentRun.kt`, `docs/implementation/AGENT_RUN_REFERENCE_EXPOSURE_IMPLEMENTATION_PLAN.md`, and every other existing specification remain exactly as they were.
- No new public type, interface, constructor parameter, lifecycle edge, or `EventBus.subscribe` call was introduced anywhere in `src/`.
- Task Manager's existing event-subscription boundary (`agent.completed`/`agent.failed` only, `IMPLEMENTATION_GAPS.md` #42) is unchanged -- no new subscription was added.
- `docs/architecture/IMPLEMENTATION_GAPS.md` #43 was **closed in full** -- see that entry's own updated text. Both halves §10 names (`task.started`'s Agent Run Reference and `task.completed`'s Task Result summary) are now implemented and verified.
- The `correlationId`-vs-`AgentRunId` wording tension between `AgentRuntimeSpecification.md` Section 9 and `AgentRun.kt`'s own documented convention remains open as a **separate documentation/specification matter**, explicitly not part of gap #43 and not resolved by this Unit.
- No other implementation gap, Communication Runtime, Local Text Channel, Cognition, Planner Runtime, Memory Runtime, or World Model file was touched.

---

### Sprint 7 -- Conversation Engine Inbound Continuity + Reasoning Provider Contract Implementation (updates `IMPLEMENTATION_GAPS.md` #53, in part)

Commit:
pending

Completed:
2026-07-08

Android Studio Tests:
Android Studio verified: **506/506 passing** (Human authority, PES-001), confirmed by Steven. Prior confirmed total (Sprint 7, Agent Run Reference Exposure) was 484/484. This Unit adds `tests/runtime/InMemoryConversationEngineTest.kt` (8 tests), `tests/runtime/ConversationTurnReasoningCoordinatorTest.kt` (5 tests), and `tests/contracts/ReasoningProviderContractTest.kt` (9 tests) -- `tests/runtime/FakeReasoningProvider.kt` is a fixture, no tests of its own -- a net addition of +22, reconciling exactly with the confirmed 506/506 total above. This total has been run and confirmed in Android Studio; it is no longer a static projection.

Summary
- Implemented exactly the Stage 3 Scope-Locked unit `docs/implementation/CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` authorises, combining the previously-approved `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` and `REASONING_PROVIDER_CONTRACT_DESIGN.md` into one first implementation: `ConversationEngine` inbound continuity binding, the `ReasoningProvider` contract types, and a standalone coordinator sequencing the two.
- Added `src/interfaces/ConversationEngine.kt`: `ConversationId`, `TurnId`, `Conversation`, `Turn`, `ConversationDisposition`, and the `ConversationEngine` interface (`submitTurn(message: InboundOwnerMessage): ConversationDisposition`).
- Added `src/interfaces/ReasoningProvider.kt`: `ReasoningContext`, `ReasoningProviderRequest`, `ReasoningProviderResponse` (sealed: `Goal`/`Reply`/`NoAction`, no `Failed` variant), and the `ReasoningProvider` interface (`reason(request: ReasoningProviderRequest): ReasoningProviderResponse`).
- Added `src/runtime/InMemoryConversationEngine.kt`, the first `ConversationEngine` implementation. Per Required Implementation Decision 1, every inbound Turn begins a new Conversation -- `ConversationDisposition.isNewConversation` is always `true` in this first unit, and the class is accordingly stateless (no stored Conversation map, no `Mutex`). Per Required Implementation Decision 3, `submitTurn` resolves its own operating identity, `system.conversation-engine`, through the injected `IdentityService` before acting, and fails fast (throws `IllegalStateException`) if that identity is not registered; the message's own `senderPrincipalId` is never substituted for it. The engine's only dependency is `IdentityService`.
- Added `src/runtime/ConversationTurnReasoningCoordinator.kt`. Per Required Implementation Decision 2, this is a plain, concrete class -- deliberately not interface-backed, since it introduces no new public contract type and is ordinary Stage 3 wiring between two already-approved contracts, not a new architectural boundary. Its constructor accepts only `ConversationEngine` and `ReasoningProvider`; `submitTurnAndReason` calls `conversationEngine.submitTurn`, builds a `ReasoningProviderRequest` from the resulting `Turn` and the caller-supplied `ReasoningContext`, calls `reasoningProvider.reason`, and returns the result unchanged -- the unit's explicit stop condition.
- Added `tests/runtime/FakeReasoningProvider.kt` (a lambda-based fake mirroring `FakeCommunicationIntake`/`FakePermissionEngine`'s established precedent), `tests/runtime/InMemoryConversationEngineTest.kt`, `tests/runtime/ConversationTurnReasoningCoordinatorTest.kt`, and `tests/contracts/ReasoningProviderContractTest.kt`, implementing the Implementation Plan's Section 6 Testing Strategy in full, including the three explicitly required Principal tests (a registered `system.conversation-engine` resolves and `submitTurn` proceeds; a missing/unregistered operating Principal fails fast; the message sender's Principal is never substituted for the operating Principal) and a structural test proving the coordinator's constructor has no slot for `PlannerRuntime`, `ExecutionPipeline`, `MemoryStore`, or `WorldModel`.

Implementation Notes
- Per the Scope Lock's explicit boundary, this Unit implements the inbound half only and stops after obtaining a `ReasoningProviderResponse`. No `PlannerRuntime` integration, no `PlanningRequest` construction, no Response Delivery, no `OutboundParkerResponse` construction, no Memory writes, no World Model writes, no `ExecutionPipeline` calls, and no Tool invocation exist anywhere in this Unit's code. No persistence was introduced -- `InMemoryConversationEngine` holds no stored Conversation/Turn state, consistent with Required Implementation Decision 1 making such storage unnecessary for this first unit.
- No concrete `ReasoningProvider` implementation (model-backed or otherwise) was added -- only the contract and a test-only `FakeReasoningProvider`. `ReasoningContext` assembly ownership remains unassigned, exactly as `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 9 already disclosed.
- Nothing in this Unit wires `CommunicationIntake`'s own accepted-message surface (`acceptedMessages()`/`acceptedMessageFor`) into `ConversationEngine.submitTurn` -- no production code in this repository calls `submitTurn` yet. See this Unit's `IMPLEMENTATION_GAPS.md` #53 update below for what this does, and does not, close.
- A test-fixture defect was found and corrected during Android Studio verification, not a production defect: `tests/runtime/InMemoryConversationEngineTest.kt`'s `conversationEnginePrincipal()` helper initially registered `system.conversation-engine` with `PrincipalStatus.ACTIVE`, but `InMemoryIdentityService.register` requires `PrincipalStatus.CREATED` on every newly registered Principal (matching the established `InMemoryPlannerRuntimeTest.identityServiceWithPlannerRegistered()` precedent, which registers `system.planner-runtime` the same way). This caused all six tests that registered that fixture to fail with `"A newly registered Principal must have status CREATED, was ACTIVE"`. The fixture was corrected to `PrincipalStatus.CREATED`; no production code (`InMemoryIdentityService.kt`, `InMemoryConversationEngine.kt`, or any other `src/` file) was modified to address this.
- No architecture, contract design, or implementation plan document was modified. `19-conversation-engine.md`, `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`, `REASONING_PROVIDER_ARCHITECTURE.md`, `REASONING_PROVIDER_CONTRACT_DESIGN.md`, and `CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` all remain exactly as previously accepted/Scope-Locked.
- No other implementation gap, Communication Runtime, Local Text Channel, Task Manager Runtime, Planner Runtime, Memory Runtime, or World Model file was touched.

---

### Sprint 7, Unit C2 -- Communication-to-Conversation Wiring (updates `IMPLEMENTATION_GAPS.md` #53, in part)

Commit:
pending

Completed:
2026-07-08

Android Studio Tests:
Android Studio verified: **519/519 passing** (Human authority, PES-001), confirmed by Steven. Prior confirmed total (Sprint 7, Conversation Engine Inbound Continuity + Reasoning Provider Contract Implementation) was 506/506. This Unit adds `tests/runtime/CommunicationConversationCoordinatorTest.kt` (13 tests) and no other test file, a net addition of +13, reconciling exactly with the confirmed 519/519 total above. This total has been run and confirmed in Android Studio; it is no longer a static projection.

Summary
- Implemented exactly the Stage 3 Scope-Locked unit `docs/implementation/COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md` authorises: the smallest coordinator connecting the already-implemented Communication Runtime (`CommunicationIntake`, Sprint 7 Unit C1) to the already-implemented Conversation Engine + Reasoning Provider unit (`ConversationTurnReasoningCoordinator`), reusing both sides whole and unchanged.
- Added `src/runtime/GatedOutcome.kt`: `GatedOutcome<T>`, a small, generic upstream-admission-gate wrapper (`Produced<T>`/`NotAccepted`), not specific to Communication -- models "admit, producing one value, or reject with a reason" in general. Introduced only after checking, and rejecting, four alternatives (throwing; a nullable response; reusing `CommunicationIntakeDisposition` unmodified; `kotlin.Result`), each of which either dropped the rejection reason or broke an established codebase precedent (Plan Section 5, Decision 1).
- Added `src/runtime/CommunicationConversationCoordinator.kt`: a non-interface-backed class (mirroring `ConversationTurnReasoningCoordinator`'s own precedent), constructor-injected with exactly `CommunicationIntake` and `ConversationTurnReasoningCoordinator`. Its one method, `submitAndReason`, calls `CommunicationIntake.submitInboundMessage` first; on `Rejected`, it stops immediately and returns `GatedOutcome.NotAccepted` carrying the rejection reason unchanged, never reaching `ConversationEngine` or `ReasoningProvider`; on `Accepted`, it delegates to `ConversationTurnReasoningCoordinator.submitTurnAndReason` unchanged and returns the resulting `ReasoningProviderResponse` wrapped in `GatedOutcome.Produced`. An accepted `InboundOwnerMessage` now reaches a real `ReasoningProvider` invocation through one tested, production code path -- where previously nothing in this repository called that sequence at all.
- Added `tests/runtime/CommunicationConversationCoordinatorTest.kt` (13 tests): the accepted path for all three `ReasoningProviderResponse` variants (`Goal`/`Reply`/`NoAction`), the rejected path (with a structural proof that `ReasoningProvider.reason` is never called), the message pass-through invariant (proved via a deliberately-mismatched accepted message, confirming the coordinator threads `disposition.message`, not its own input parameter, downstream), exactly-once call-count assertions, exception propagation from each dependency (with no `try`/`catch` anywhere in the coordinator), the structural constructor test (no dependency slot beyond the two named types), a reflective statelessness test (no field beyond the two constructor-injected dependencies), an independent-invocations non-interference test, and two `GatedOutcome` construction tests.

Implementation Notes
- **The coordinator is stateless.** It declares no field beyond its two constructor-injected dependencies -- no `var`, no mutable collection, no cache, no `Mutex` -- verified by a reflective test, not only asserted in KDoc.
- **The coordinator never mutates or reinterprets an accepted `InboundOwnerMessage`.** It sequences only: no `InboundOwnerMessage` is constructed, copied, or read for branching purposes anywhere in this Unit's code.
- **No retries, no batching, no exception translation.** `CommunicationIntake.submitInboundMessage` is called exactly once and, on acceptance, `ReasoningProvider.reason` (via `ConversationTurnReasoningCoordinator`) is called exactly once -- unconditionally, regardless of which `ReasoningProviderResponse` variant results. An exception thrown by either dependency propagates to the coordinator's own caller unchanged; it is never caught, translated, retried, or converted into a `GatedOutcome.NotAccepted`.
- **No `PlannerRuntime`, `TaskManagerRuntime`, `AgentRuntime`, `ExecutionPipeline`, Response Delivery, `OutboundParkerResponse` construction, Memory writes, World Model writes, `EventBus` publication, Android, Speech, UI, persistence, or model-backed `ReasoningProvider` implementation exist anywhere in this Unit's code.** Enforced structurally: the coordinator's constructor has no slot for any of the first four, and the dependency list is exhausted by exactly `CommunicationIntake` and `ConversationTurnReasoningCoordinator`, both of which are themselves already structurally proven to lack any such slot.
- No existing `src/` or `tests/` file was modified. `CommunicationIntake`, `InMemoryCommunicationIntake`, `ConversationEngine`, `InMemoryConversationEngine`, `ReasoningProvider`, `ConversationTurnReasoningCoordinator`, `FakeCommunicationIntake`, and `FakeReasoningProvider` are all consumed exactly as they existed before this Unit.
- No architecture, contract design, or implementation plan document was modified. `COMMUNICATION_CONTRACT_DESIGN.md`, `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`, `REASONING_PROVIDER_ARCHITECTURE.md`, `REASONING_PROVIDER_CONTRACT_DESIGN.md`, `19-conversation-engine.md`, and `COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md` all remain exactly as previously accepted/Scope-Locked.
- `docs/architecture/IMPLEMENTATION_GAPS.md` #53 was clarified further, not closed -- see that entry's own updated text. This Unit implements the wiring between an accepted `InboundOwnerMessage` and a `ReasoningProviderResponse` in full, but performs no routing of that response anywhere: `ReasoningContext` assembly, a concrete model-backed `ReasoningProvider`, and the downstream Planner Runtime/Response Delivery path all remain unimplemented, and gap #53's own two closure paths both remain fully open.
- No other implementation gap, Local Text Channel, Task Manager Runtime, Planner Runtime, Memory Runtime, or World Model file was touched.

---

### Sprint 7, Unit C4 -- Response Delivery (updates `IMPLEMENTATION_GAPS.md` #53, in part)

Commit:
pending

Completed:
2026-07-08

Android Studio Tests:
Android Studio verified: **532/532 passing** (Human authority, PES-001), confirmed by Steven. Prior confirmed total (Sprint 7, Unit C2 -- Communication-to-Conversation Wiring) was 519/519. This Unit adds `tests/runtime/ResponseDeliveryTest.kt` (13 tests) -- `tests/runtime/FakeResourceRegistry.kt` and `tests/runtime/FakeExecutionPipeline.kt` are fixtures, no tests of their own -- a net addition of +13, reconciling exactly with the confirmed 532/532 total above. This total has been run and confirmed in Android Studio; it is no longer a static projection.

Summary
- Implemented exactly the Stage 3 Scope-Locked unit `docs/implementation/RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md` authorises, built on `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` (Accepted, Stage 2A), `docs/architecture/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`, and `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`.
- Added `src/runtime/ResponseDelivery.kt`: a concrete, non-interface-backed class, constructor-injected with exactly `ResourceRegistry` and `ExecutionPipeline`. Its one method, `deliver(response: OutboundParkerResponse)`, locates the response's channel's own backing Resource via `ResourceRegistry.listByOwner(PrincipalId(response.channelId.value))`, filtered to `ResourceType.TOOL` (Contract Design Decision 2; formalised as approved architecture by `ADR-026`). Zero matches or more than one match returns `GatedOutcome.NotAccepted(reason)`, with `reason` distinguishing the two cases -- no `ExecutionRequest` is constructed in either case. Exactly one match constructs one `ExecutionRequest` and submits it through the injected `ExecutionPipeline`, returning the resulting `ExecutionResult` wrapped, unchanged, in `GatedOutcome.Produced`.
- The constructed `ExecutionRequest` uses: `principalId = response.senderPrincipalId`; `origin = RequestOrigin.TEXT`; `intent = "deliver response"`; `targetResources = listOf(<the matching Resource's ResourceId>)`; `proposedActions = listOf("notify owner")`; `priority = RequestPriority.NORMAL` (Plan Section 5, Decision 3); `requestId = RequestId("deliver-response-${response.correlationId.value}")` (Plan Section 5, Decision 4); `correlationId = response.correlationId.value`; `metadata = mapOf(RESPONSE_TEXT_METADATA_KEY to response.text)`, where `RESPONSE_TEXT_METADATA_KEY = "response.text"` is a top-level constant in `src/runtime/ResponseDelivery.kt` (Plan Section 5, Decision 1). `response.metadata` is not forwarded, per Contract Design Section 3.
- The `"notify owner"` proposed action and its `ActionVocabularyEntry` (`verbPhrase = "notify owner"`, `mappings = setOf(ActionResourceMapping(PermissionAction.NOTIFY, ResourceType.TOOL))`) are fixed by `docs/implementation/RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md`; the entry is registered inside this Unit's own end-to-end test only, since no composition root exists in this repository to register it at production startup.
- Added `tests/runtime/FakeResourceRegistry.kt` and `tests/runtime/FakeExecutionPipeline.kt` (lambda-based fakes mirroring `FakeCommunicationIntake`/`FakePermissionEngine`'s established precedent; each throws if a method `ResponseDelivery` does not call is reached). Added `tests/runtime/ResponseDeliveryTest.kt` (13 tests): the zero-match and many-match paths (each proving `ExecutionPipeline.submit` is never called), non-TOOL Resources being ignored in both directions, the exactly-one-match path, full field-by-field `ExecutionRequest` construction correctness (including `priority` and `requestId`), the `ExecutionResult` pass-through, exception propagation from each dependency with no `try`/`catch` anywhere in `ResponseDelivery`, the structural constructor test (no dependency slot beyond `ResourceRegistry`/`ExecutionPipeline`), a reflective statelessness test (no field beyond the two constructor-injected dependencies), an independent-invocations non-interference test, and one end-to-end test wiring the real `InMemoryResourceRegistry` and `DefaultExecutionPipeline` together with the registered `NOTIFY` vocabulary entry, asserting `ExecutionResultStatus.SUCCESS`.

Implementation Notes
- **`ResponseDelivery` is stateless.** It declares no field beyond its two constructor-injected dependencies -- no `var`, no mutable collection, no cache -- verified by a reflective test, not only asserted in KDoc.
- **No retry, batching, queueing, or streaming.** Exactly one `ExecutionRequest` is submitted per call, to exactly one channel, unconditionally.
- **No persistence.** `ResponseDelivery` holds no state between calls; nothing it produces is written to disk, a database, or any durable store.
- **No new `EventBus` publication.** `DefaultExecutionPipeline`'s existing `execution.*` lifecycle events already cover the `ExecutionRequest` this Unit constructs; this Unit adds no publication of its own.
- **No Planner integration, Goal routing, `PlanCandidate` generation, or Workflow Runtime** exist anywhere in this Unit's code.
- **No Memory writes, World Model writes, Android, UI, or Speech** dependency was introduced.
- **No production Local Text Channel "deliver" Tool was registered.** This Unit's end-to-end test registers a minimal test `Tool`/`ToolDescriptor` for its own isolated verification only -- `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`'s own `toolsExposed` list is unmodified, and this remains a separate, not-yet-performed unit.
- **No composition root was added.** Production registration of the `NOTIFY` vocabulary entry, at whatever future startup path this platform eventually builds, remains unimplemented and is named explicitly, not silently assumed.
- No existing `src/` or `tests/` file was modified. `ResourceRegistry`, `InMemoryResourceRegistry`, `ExecutionPipeline`, `DefaultExecutionPipeline`, `ActionVocabulary`, `InMemoryActionVocabulary`, `ActionMapper`, `OutboundParkerResponse`, `ExecutionRequest`, `ExecutionResult`, and `GatedOutcome<T>` are all consumed exactly as they existed before this Unit.
- No architecture, contract design, ADR, or implementation plan document was modified during implementation. `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`, `ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`, `ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`, `RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md`, and `RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md` all remain exactly as previously accepted/Scope-Locked. (`RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md` itself was amended twice, under its own Scope Lock, before Kotlin began -- see that document's own Section 5, Decisions 3 and 4 -- both amendments predate, and are unchanged by, this Unit's implementation.)
- `docs/architecture/IMPLEMENTATION_GAPS.md` #53 was clarified further, not closed -- see that entry's own updated text below. This Unit implements Response Delivery in full, on its own, but nothing yet calls it: constructing an `OutboundParkerResponse` from a `Reply`, and the downstream Planner Runtime/Goal-routing path, both remain unimplemented, and the Local Text Channel's own production "deliver" Tool remains unregistered.
- No other implementation gap, Local Text Channel, Task Manager Runtime, Planner Runtime, Memory Runtime, or World Model file was touched.

---

### Sprint 8 -- Local Text Channel Deliver Tool (updates `IMPLEMENTATION_GAPS.md` #53, in part)

Commit:
pending

Completed:
2026-07-08

Android Studio Tests:
Android Studio verified: **541/541 passing** (Human authority, PES-001), confirmed by Steven. Prior confirmed total (Sprint 7, Unit C4 -- Response Delivery) was 532/532. This Unit adds `tests/runtime/LocalTextChannelDeliverToolTest.kt` (9 tests) -- a net addition of +9, reconciling exactly with the confirmed 541/541 total above. This total has been run and confirmed in Android Studio; it is no longer a static projection.

Summary
- Implemented exactly the Stage 3 Scope-Locked unit `docs/implementation/LOCAL_TEXT_CHANNEL_DELIVER_TOOL_IMPLEMENTATION_PLAN.md` authorises, built on `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` (Accepted, Stage 2A, revised to register this Tool in `toolsExposed`), `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md`, `docs/architecture/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`, `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`, and `docs/implementation/RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md`.
- Added `src/runtime/LocalTextChannelDeliverTool.kt`: a concrete, non-interface-backed implementation of the existing `Tool` interface. `descriptor` -- a computed property, not a stored value, so this class declares no backing field for it -- fixes `toolId = "deliver"` (`ToolDescriptor.toolId: String`, the approved public contract; not a `ToolId` value class, which does not exist anywhere in this repository), `displayName`/`description`, `supportedActions = setOf(PermissionAction.NOTIFY)`, and `supportedResourceTypes = setOf(ResourceType.TOOL)`. `moduleId = ModuleId("channel.local-text")` is reused, not invented, matching the identity already established across `tests/runtime/ResponseDeliveryTest.kt` and other Sprint 7 tests.
- `validate(request)` rejects a request whose `request.metadata[RESPONSE_TEXT_METADATA_KEY]` is missing or blank; `execute(request)` reads that same key unchanged (no formatting, trimming, normalisation, or mutation), invokes an injected `suspend (text: String) -> Unit` callback exactly once with the exact text, and returns a successful `ToolResult` (`Tool.execute` returns `ToolResult`, not `ExecutionResult` -- `DefaultExecutionPipeline` builds the `ExecutionResult` from it, unmodified by this Unit).
- **`ToolDescriptor` single-source-of-truth rule respected throughout.** Every reference to this Tool's descriptor -- the `ModuleDescriptor.toolsExposed` entry, `ToolInvocationBinding.bind`'s second argument, and every test fixture -- reads it from `tool.descriptor` directly; no second `ToolDescriptor(...)` literal exists anywhere in this Unit's code or tests.
- Added `tests/runtime/LocalTextChannelDeliverToolTest.kt` (9 tests): descriptor shape (`toolId`, capability, resource-type); `validate` rejecting missing metadata, rejecting blank metadata, and accepting non-blank metadata; `execute` invoking the callback exactly once with the exact text (using deliberately unusual leading/trailing whitespace and mixed-case input to prove nothing is silently normalised) and returning the expected `ToolResult`; a reflective statelessness test (exactly one declared field, the injected callback); a direct `InMemoryToolInvocationBinding` binding test proving `tool.descriptor` reuse succeeds; and one end-to-end test registering this real Tool through `InMemoryModuleRegistry.register`/`enable`, confirming the backing `Resource`'s `ownerPrincipalId` (per `ADR-026`) resolves to exactly one `TOOL`-type match, binding it via `ToolInvocationBinding.bind`, registering the `NOTIFY` vocabulary entry, and calling the existing, unmodified `ResponseDelivery.deliver` through a real `DefaultExecutionPipeline`, asserting `ExecutionResultStatus.SUCCESS` and that the injected callback received the response's exact text.

Implementation Notes
- **`LocalTextChannelDeliverTool` has exactly one side effect.** `execute` invokes the injected callback exactly once, with the exact response text; no file write, network call, `EventBus` publication, or other observable effect exists anywhere in this class.
- **No formatting, trimming, normalisation, or mutation of response text** -- verified by an exact-string-equality test using deliberately unusual input.
- **No Android UI, speech, or real notification/display rendering.** The injected callback is a plain function, supplied entirely by the caller (in this Unit, a capturing test lambda); this Unit makes no claim that a response is shown to a human being.
- **No persistence, no new `EventBus` publication, no Planner integration, no Conversation Engine integration, no Reasoning Provider integration, no Memory, no World Model, no retry policy, no queueing, no streaming, no multiple recipients, no multi-channel fan-out** -- none of these dependencies exist anywhere in this Unit's code.
- **No production composition root was added.** Registration (`ModuleRegistry.register`/`enable`, `ToolInvocationBinding.bind`, the `NOTIFY` vocabulary entry) happens entirely inside this Unit's own end-to-end test, mirroring Unit C4's own identical precedent for the vocabulary entry -- no `fun main(` exists anywhere under `src/` to perform these calls at real startup; a future, real composition root must perform the identical calls once, named here so it is not silently forgotten.
- No existing `src/` or `tests/` file was modified. `ModuleRegistry`, `InMemoryModuleRegistry`, `ResourceRegistry`, `InMemoryResourceRegistry`, `ToolRegistry`, `InMemoryToolRegistry`, `ToolInvocationBinding`, `InMemoryToolInvocationBinding`, `ActionVocabulary`, `InMemoryActionVocabulary`, `ActionMapper`, `DefaultExecutionPipeline`, and `ResponseDelivery` are all consumed exactly as they existed before this Unit.
- No architecture, contract design, ADR, or implementation plan document was modified during implementation. `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`, `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`, `ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`, `ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`, `RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md`, and `LOCAL_TEXT_CHANNEL_DELIVER_TOOL_IMPLEMENTATION_PLAN.md` all remain exactly as previously accepted/Scope-Locked. One restatement error in the Scope Lock instruction itself (a locked decision referring to a `ToolId` type that does not exist in this repository) was identified before any Kotlin was written, reported, and corrected by Steven to `toolId: String` -- the underlying locked Plan document was never incorrect on this point and was not modified.
- `docs/architecture/IMPLEMENTATION_GAPS.md` #53 was clarified further, not closed -- see that entry's own updated text below. The Local Text Channel now has a real, registered, end-to-end-verified deliver Tool, but constructing an `OutboundParkerResponse` from a `Reply`, a model-backed `ReasoningProvider`, and `Goal`/Planner Runtime routing all remain unimplemented, and no production composition root exists to perform this Unit's own registration calls at real startup.
- `docs/architecture/IMPLEMENTATION_GAPS.md` #52 was **not** touched. None of this Unit's work bears on its three remaining open items (the `ResourceSensitivity.PUBLIC` default, non-atomic multi-Tool registration, or stale locally-tracked `ToolLifecycleState`) -- this Unit relies only on the one item `ADR-026` already settled.
- No other implementation gap, Communication Runtime, Conversation Engine, Reasoning Provider, Planner Runtime, Memory Runtime, or World Model file was touched.

---

### Sprint 9 -- Model-Backed ReasoningProvider (updates `IMPLEMENTATION_GAPS.md` #53, in part)

Commit:
pending

Completed:
2026-07-10

Android Studio Tests:
Android Studio verified: **578/578 passing** (Human authority, PES-001),
confirmed by Steven. **BUILD SUCCESSFUL.** Prior confirmed total (Sprint
8, Local Text Channel Deliver Tool) was 541/541. This Unit adds 37 new
test methods (`tests/runtime/ModelReasoningProviderTest.kt` (7),
`ReasoningPromptBuilderTest.kt` (6), `ModelInferenceClientTest.kt` (12),
`ReasoningResponseParserTest.kt` (12) -- `FakeModelInferenceClient.kt`,
`FakeReasoningPromptBuilder.kt`, `FakeReasoningResponseParser.kt` are
fixtures, no tests of their own), a net addition of +37, reconciling
exactly with the confirmed 578/578 total above. This total has been run
and confirmed in Android Studio; it is no longer a projection.

The first run surfaced one failure, in `ModelReasoningProviderTest`'s own
reflective constructor-shape test (`` `the constructor accepts exactly
four parameters -- three collaborators and timeoutMs` ``):
`java.lang.IllegalArgumentException: Array has more than one element`
from `declaredConstructors.single()`. Root cause: `ModelReasoningProvider`'s
constructor has a default value for `timeoutMs` (Plan Decision 4), so the
Kotlin compiler emits a second, synthetic constructor (`ACC_SYNTHETIC`,
carrying trailing `int` bitmask + `DefaultConstructorMarker` parameters)
alongside the real, declared one, to support default-argument call
sites -- a mechanical consequence of the already-Scope-Locked default
value, not an unintended extra constructor in `ModelReasoningProvider`'s
own design. The test's `declaredConstructors.single()` pattern, copied
from `ConversationTurnReasoningCoordinatorTest` (whose own constructor
has no default parameters and so never exercised this case), does not
generalise to a constructor with a default value. Corrected in the test
only -- `declaredConstructors.single { !it.isSynthetic }` -- no
production file was modified. The second run confirmed 578/578, BUILD
SUCCESSFUL.

Summary
- Implemented exactly the Stage 3/5 Scope-Locked unit
  `docs/implementation/MODEL_REASONING_PROVIDER_IMPLEMENTATION_PLAN.md`
  authorises, itself built on
  `docs/implementation/reviews/ModelReasoningProvider_Implementation_Review.md`
  (Accepted, no further architectural redesign required, per Steven's
  own instruction opening this session).
- Added `src/runtime/ModelReasoningProvider.kt`: a concrete `ReasoningProvider`
  implementation and pure orchestrator over three constructor-injected
  collaborators, with no `try`/`catch` anywhere in the class -- `reason`
  builds a prompt, calls the model inference seam under `withTimeout`,
  and parses the raw result, returning it unchanged.
- Added `src/runtime/ReasoningPromptBuilder.kt`: the `ReasoningPromptBuilder`
  `fun interface` and `DefaultReasoningPromptBuilder`, a deterministic
  template (already-assembled context entries, one per line, then the
  owner's message, then a fixed instruction naming the `GOAL:`/`REPLY:`/`NOACTION`
  convention).
- Added `src/runtime/ModelInferenceClient.kt`: the `ModelInferenceClient`
  `fun interface`, `LocalHttpModelInferenceClient` (a JDK
  `java.net.http.HttpClient`-based implementation, `endpointUrl`/`modelName`
  required with no default, cancellation wired through
  `suspendCancellableCoroutine`), and two named, overridable default
  formatting functions, `defaultOllamaRequestBody`/`defaultOllamaResponseBody`,
  using minimal, hand-rolled JSON string construction/extraction -- no
  JSON library or other new Gradle dependency was added.
- Added `src/runtime/ReasoningResponseParser.kt`: the `ReasoningResponseParser`
  `fun interface` -- the frozen architectural component -- and
  `TaggedReasoningResponseParser`, its first, explicitly-replaceable
  default implementation, plus `UnclassifiableModelResponseException`.
  Matching is case-sensitive against the raw string trimmed once:
  `GOAL:`/`REPLY:` prefixes produce `Goal`/`Reply` with the trimmed
  remainder as text (a blank remainder surfaces as the existing
  `IllegalArgumentException` from `Goal`/`Reply`'s own constructor
  validation, not caught here); exactly `NOACTION` with nothing else
  present produces `NoAction`; anything else, including `NOACTION` with
  trailing text, throws `UnclassifiableModelResponseException` carrying
  the original raw text.
- Added `tests/runtime/FakeModelInferenceClient.kt`, `FakeReasoningPromptBuilder.kt`,
  `FakeReasoningResponseParser.kt` (lambda-based fakes mirroring
  `FakeReasoningProvider`'s established precedent) and the four test
  files named above, implementing the Plan's Section 8 Testing Strategy
  in full: `ModelReasoningProvider`'s orchestration against fakes only
  (prompt/infer/parse call sequencing, exception propagation, timeout
  behaviour); `DefaultReasoningPromptBuilder`'s exact template;
  `defaultOllamaRequestBody`/`defaultOllamaResponseBody`'s escaping,
  extraction, and round-trip behaviour; and `TaggedReasoningResponseParser`'s
  full classification convention, including case sensitivity and the
  `NOACTION`-with-trailing-text edge case.

Implementation Notes
- **No existing `src/` or `tests/` file was modified.** All eleven new
  files (four production, three fakes, four tests) are additions.
- **No new Gradle dependency was added; `build.gradle.kts` is unmodified.**
  `LocalHttpModelInferenceClient` uses only the JDK's own
  `java.net.http.HttpClient` (available under this project's existing
  `jvmToolchain(17)`) and the already-present `kotlinx-coroutines-core`.
- **No new file exists under `src/interfaces/`.** This Unit introduces no
  new public Parker contract type -- `ReasoningPromptBuilder`,
  `ModelInferenceClient`, and `ReasoningResponseParser` are `src/runtime`-local
  collaborator interfaces, not Parker contracts, matching the Review's
  own Section 2/13 finding.
- **No `IdentityService`, `PlannerRuntime`, `ExecutionPipeline`,
  `PermissionEngine`, `ToolRegistry`, `ToolInvocationBinding`,
  `MemoryStore`, `WorldModel`, `ModuleRegistry`, `ConversationEngine`,
  `ResponseDelivery`, or `ModelManager` dependency exists anywhere in
  this Unit's code.**
- **`LocalHttpModelInferenceClient`'s own live HTTP path is not exercised
  by the automated test suite** -- no real model server exists in this
  sandbox, disclosed as such by the Review (Risk 1) before implementation
  began, not discovered afterward.
- **No production caller was wired.** `ConversationTurnReasoningCoordinator`
  and `CommunicationConversationCoordinator` are both unmodified; nothing
  in this repository yet constructs a real `ModelReasoningProvider` and
  hands it to either coordinator. That remains a future, separately-scoped
  unit's responsibility, per the Plan's own Excluded Work.
- No architecture, Contract Design, or ADR document was modified.
  `REASONING_PROVIDER_ARCHITECTURE.md`, `REASONING_PROVIDER_CONTRACT_DESIGN.md`,
  and `docs/architecture/reasoning-context.md` all remain exactly as
  previously accepted.
- `docs/architecture/IMPLEMENTATION_GAPS.md` #53 was clarified further,
  not closed -- see that entry's own updated text. The "no concrete,
  model-backed `ReasoningProvider` implementation exists" item is now
  resolved (the implementation exists); every other item under #53
  (Reply-to-`OutboundParkerResponse` construction, `Goal`/Planner Runtime
  routing, `ReasoningContext` assembly ownership, and a production
  composition root) remains open, unaffected by this Unit.

---

### Sprint 10 -- ResponseComposer (Unit 1) (updates `IMPLEMENTATION_GAPS.md` #53, in part)

Commit:
pending

Completed:
2026-07-23

Android Studio Tests:
Android Studio verified: **589/589 passing** (Human authority, PES-001),
confirmed by Steven. **BUILD SUCCESSFUL.** The prior confirmed total
(Sprint 9, Model-Backed ReasoningProvider) was 578/578; this Unit's 11
new test methods (`tests/runtime/ResponseComposerTest.kt`) account for
the difference.

The first verification run surfaced one failure, in
`ResponseComposerTest`'s own reflective statelessness test. That test's
original assertion compared `ResponseComposer::class.java.declaredFields`
against the single expected name `"identityService"`, which did not
account for the field the Kotlin compiler generates to back the private,
class-scoped `RESPONSE_COMPOSER_PRINCIPAL_ID` companion-object constant --
a compiler artefact of the already-Scope-Locked identity constant (Scope
Lock Section 6/7), not an unintended additional field on
`ResponseComposer` itself. Corrected in the test only, after this real
JVM failure surfaced it: the assertion now filters to non-static fields
(`!Modifier.isStatic(it.modifiers)`) before comparing against
`{"identityService"}`, measuring the architectural property -- per-instance
state -- rather than a specific generated field name, and remains correct
regardless of which name a given Kotlin compiler version assigns to the
companion's backing field. `ResponseComposer.kt` itself was not modified
to fix this. The second run confirmed 589/589, BUILD SUCCESSFUL.

Summary
- Implemented exactly the Stage 5 Scope-Locked unit
  `docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md` authorises, itself
  freezing `docs/implementation/REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md`
  (Sprint 10, Unit 1).
- Added `src/runtime/ResponseComposer.kt`. **Composition only:** it
  converts a `ReasoningProviderResponse.Reply` to an
  `OutboundParkerResponse`; it does not deliver; it does not route
  `Goal`; it does not invoke Planner Runtime; it does not create the
  complete owner-message-to-delivery path. One constructor dependency
  (`IdentityService`), one public method (`compose`), returning
  `GatedOutcome<OutboundParkerResponse>`. Identity resolution
  (`system.response-composer`) happens only inside the `Reply` branch,
  exactly once, immediately before construction; `Goal` and `NoAction`
  never resolve identity and return `GatedOutcome.NotAccepted`
  unconditionally, regardless of registration state.
- Added `tests/runtime/FakeIdentityService.kt`: a lambda-based,
  call-counting `IdentityService` fake, mirroring
  `FakeExecutionPipeline`/`FakeResourceRegistry`'s established
  precedent.
- Added `tests/runtime/ResponseComposerTest.kt`: 11 tests covering the
  Reply/Goal/NoAction branches, per-branch identity-resolution call
  counts, the Goal/NoAction-never-throw-when-unregistered invariant,
  field pass-through, the Reply-only unregistered-identity exception,
  statelessness (see the incident above), constructor shape, and a
  real-stack compatibility test proving a composed `OutboundParkerResponse`
  is accepted unchanged by the existing `ResponseDelivery`/
  `LocalTextChannelDeliverTool` stack, without `ResponseComposer` itself
  ever calling `ResponseDelivery`.

Implementation Notes
- **No existing `src/` or `tests/` file was modified.** All three new
  files (one production, two test) are additions.
- No dependency on `ResponseDelivery`, `ExecutionPipeline`,
  `ResourceRegistry`, `ToolRegistry`, `ToolInvocationBinding`,
  `PermissionEngine`, `PlannerRuntime`, `ReasoningProvider`,
  `MemoryStore`, or `WorldModel` exists anywhere in this Unit's
  production code.
- No new file exists under `src/interfaces/`. This Unit introduces no
  new public Parker contract type -- its return type,
  `GatedOutcome<OutboundParkerResponse>`, reuses two already-existing
  types unchanged.
- No architecture, Contract Design, or ADR document was modified.
  `COMMUNICATION_CONTRACT_DESIGN.md`, `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`,
  and `REASONING_PROVIDER_CONTRACT_DESIGN.md` all remain exactly as
  previously accepted.
- **`ReplyDeliveryCoordinator` (Unit 2) was not implemented.** No
  owner-message-to-delivery orchestrator, `Goal`/Planner Runtime
  routing, `ResponseDelivery` wiring, or production composition root
  was added by this Unit -- all remain a separate, future Unit's
  responsibility, per
  `docs/implementation/REPLY_DELIVERY_COORDINATOR_IMPLEMENTATION_PLAN.md`
  (drafted, not yet Scope Locked).
- `docs/architecture/IMPLEMENTATION_GAPS.md` #53 was clarified further,
  not closed -- see that entry's own updated text. The
  `Reply -> OutboundParkerResponse` construction half of item 1 is now
  implemented and verified; delivery orchestration, `Goal`/Planner
  Runtime routing, a production composition root, and `ReasoningContext`
  assembly ownership all remain open.

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
