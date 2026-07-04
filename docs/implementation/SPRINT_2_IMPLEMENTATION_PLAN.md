# Sprint 2 Implementation Plan

## Status

**Version: v1.0.**

This document is a controlled implementation schedule, not a design
document. Changes to architecture must occur in the architecture first
and only then be reflected here.

This is an **implementation planning document**, not an architecture
document and not code. It proposes no new architectural principle,
alters no existing specification, and adds no file under `src/` or
`tests/`. Its job is to sequence and scope Sprint 2 against
`docs/architecture/PARKER_SPRINT_2_ARCHITECTURE_REVIEW.md` ("the
Readiness Review") — specifically its recommended priority order
(Section 6), recommended first candidate (Section 9), success criteria
(Section 11), non-goals (Section 12), and risk register (Section 13).
This plan does not reorder that priority, does not expand Sprint 2
scope, and does not add a new Architecture Decision. Where a candidate
area cannot yet be scoped as a coding unit — because a human decision or
a specification design step must happen first — this document says so
and names the blocker; it does not invent a decision to fill the gap.

This plan follows the same rhythm
`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` established: name
each unit, name what must close before coding begins, sequence units by
real dependency, and require a review checkpoint and an Android Studio
verification gate before advancing to the next unit.

Reviewed to produce this plan:
`docs/architecture/PARKER_SPRINT_2_ARCHITECTURE_REVIEW.md`,
`docs/architecture/IMPLEMENTATION_GAPS.md`,
`docs/architecture/ARCHITECTURE_DECISIONS.md`,
`docs/specifications/volume-03-core-interfaces/IdentityService.md`,
`docs/specifications/volume-03-core-interfaces/PermissionEngine.md`,
`docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`,
`docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`,
`docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`,
`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md`, and the existing
`src/interfaces/`, `src/runtime/`, `src/contracts/` Kotlin and
`tests/contracts/`, `tests/runtime/` test suites.

## Sprint Dashboard

Update only the Status column as work proceeds
(`Not Started` / `In Progress` / `Blocked` / `Complete`). This table,
and the per-unit Definition of Done checklists in Section 6, are the
only parts of this document meant to change routinely once the plan is
frozen — see the Governance Statement at the end.

| Track | Units | Status |
|---|---|---|
| A — Identity and Permission Hardening | A1, A2 | Not Started |
| B — Task Manager Event Handling | B1, B2 | Not Started |
| C — Agent Runtime Multi-Step Control | C1, C2 | Not Started |
| D — Planner Runtime Progression | D1, D2 | Not Started |
| E — Resource Discovery | E1, E2 | Not Started |

## Estimated Effort

Not every unit is the same size. This is a rough sizing to show where
the risk concentrates, not a time commitment:

| Unit | Size |
|---|---|
| A1 | Small |
| A2 | Medium |
| B1 | Small |
| B2 | Small |
| C1 | Documentation |
| C2 | Large |
| D1 | Documentation |
| D2 | Large |
| E1 | Documentation |
| E2 | Medium |

The pattern is not accidental: every track's second unit (C2, D2) is
sized Large or Medium precisely because its scope cannot be pinned down
until the track's own design/specification unit (C1, D1) is reviewed
(Section 4) — the size itself is a signal that these units carry design
risk, not just implementation effort.

## 1. Plan Purpose

The Readiness Review answered *what* Sprint 2 should do and in *what
order*. This plan answers the question Sprint 1's own plan answered for
Sprint 1: **how does that order become a sequence of small, individually
reviewable, individually testable units** — each with a stated
objective, a stated scope, named files, named architecture dependencies,
acceptance criteria, a review checkpoint, an Android Studio verification
gate, and a documentation update, in that order, before the next unit
begins.

## 2. Scope

Sprint 2 work is organized into five tracks, matching the Readiness
Review's priority order (Section 6) exactly:

- **Track A — Identity and Permission Hardening** (Priority 1; the
  Readiness Review's recommended starting point, Section 9).
- **Track B — Task Manager Event Handling** (Priority 2).
- **Track C — Agent Runtime Multi-Step Control** (Priority 3).
- **Track D — Planner Runtime Progression** (Priority 4).
- **Track E — Resource Discovery** (Priority 5).

Workflow Runtime (Priority 6 in the draft order) is not a track in this
plan at all — it is a Sprint 2 Non-Goal (Readiness Review Section 12)
and is out of scope here, not merely deferred to last.

## 3. Non-Scope

This plan carries forward the Readiness Review's Non-Goals (Section 12)
without modification. Sprint 2 does not:

- Build Workflow Runtime.
- Introduce autonomous planning beyond what a reviewed Planner Runtime
  Specification defines.
- Introduce memory consolidation.
- Implement scheduling.
- Redesign the Execution Pipeline.
- Change the permission architecture (as opposed to defining permission
  *policy content* within the existing architecture).
- Replace any Sprint 1 contract (`TaskProposal`, `AgentRunCommand`,
  `ToolInvocationBinding`, or the Sprint 1 lifecycle contracts).

**Overbuilding risk, restated from Sprint 1's own plan.** Sprint 1's
plan named "overbuilding" as its single largest risk — noticing, mid-unit,
that "real" behaviour needs more than the unit's stated scope. The same
risk applies here with higher surface area, since Sprint 2 touches five
subsystems instead of one vertical slice. Each unit below states its
scope narrowly on purpose; anything not named in a unit's Scope field is
not that unit's responsibility, regardless of how small it looks once
the surrounding code exists.

### No Scope Expansion

If implementation of a unit uncovers additional desirable behaviour
beyond that unit's stated Scope, that behaviour becomes a candidate for
a future sprint — not an addition to the current unit — unless it
blocks the current unit's own Acceptance Criteria from being met, in
which case it is handled under Unit Stop Conditions (following Section
6), not silently absorbed into the unit's scope.

## 4. Required Decisions and Specification Work Before Coding

Each row below already exists as a named gap or an explicit finding in
the Readiness Review — this plan invents none of them. Columns follow
`IMPLEMENTATION_GAPS.md`'s own status language rather than restating it
differently.

| Item | Status | Blocks | Recommended closure |
|---|---|---|---|
| **Gap #40** — `PermissionEngine.evaluate` not wired to resolve identity first | "Deliberately not done — explicit instruction for this phase," **not** flagged "requires human decision." | Nothing — ready to implement now. | Implement directly (Unit A1). |
| **Gap #25** — no authorisation policy content specified anywhere | "Deliberate scope boundary." Readiness Review 4.2: genuine specification gap, not an implementation gap. | Unit A2 cannot be scoped in Kotlin until policy content exists. | A short Permission Policy specification note (data-shaped, per Readiness Review Section 13's mitigation: "policy content as data the Permission Engine evaluates, not conditional logic") must be written and reviewed before Unit A2 begins. |
| **Gap #41** — `ToolInvocationBinding.invocableFor`/`ToolRegistry.resolve` restrict callers by convention only | Explicitly "Requires human decision." | Cannot be scoped as a coding unit at all until decided. | Human decision: introduce a caller-identity/visibility mechanism, or reaffirm convention-based restriction as an accepted, documented scope boundary. Not scheduled as a unit in this plan; see Section 11. |
| **Gaps #35/#36/#37** — cascading revocation, lifecycle edge set, `resolve()` suppression | Each explicitly "Requires human decision." | Cannot be scoped as coding units until decided. | Human decisions, independent of each other and of Unit A1. Notably, gap #37's own text names the Permission Engine as the intended future enforcement point for "Revoked/Archived cannot act" — meaning Unit A1 does not need any of these three decided first; see Section 6, Unit A1. |
| **Task Manager Agent-Event subscription** | Already specified (`TaskManagerRuntimeSpecification.md` §6, §11); not yet a numbered gap. | Nothing architecturally — bookkeeping only. | Log a new numbered entry in `IMPLEMENTATION_GAPS.md` before Unit B1 begins, per Readiness Review Section 8. |
| **Multi-step Agent Run model** | Not specified in any executable form (Readiness Review 4.5: "a real design gap, not merely an unwired command type"). | Unit C2 cannot be scoped in Kotlin until a model exists. | Unit C1 (design-only) must produce and get reviewed a design against `AgentRuntimeSpecification.md` §5's existing state machine before Unit C2 begins. |
| **Planner Runtime Specification review-and-correction pass** | Not yet performed (AD-014; the only Volume 4–6 specification without one). | Unit D2 cannot be scoped until the review exists. | Unit D1 (documentation-only) performs this pass, mirroring the Agent Runtime and Task Manager Runtime reviews already completed. |
| **Resource discovery operation** | Does not exist in `ResourceRegistry.md`'s Required Operations at all (Readiness Review 4.7: "missing operation, not a wiring task"). | Unit E2 cannot be scoped until the operation is specified. | Unit E1 (design-only) specifies the new operation, additive per the `ToolInvocationBinding` precedent (a companion interface rather than a change to `ResourceRegistry`). |

## 5. Proposed Sprint 2 Sequence

```text
Track A (ready now)
  Unit A1 -> [Permission Policy spec] -> Unit A2
  [#35/#36/#37/#41 human decisions -- parallel, not scheduled, do not block A1/A2]

Track B (ready after gap log entry)
  Unit B1 -> Unit B2

Track C (design-gated)
  Unit C1 (design) -> Unit C2 (implementation)

Track D (design-gated)
  Unit D1 (spec review) -> Unit D2 (implementation)

Track E (design-gated)
  Unit E1 (spec design) -> Unit E2 (implementation)
```

Tracks A and B can proceed independently and in parallel once their own
prerequisites close. Tracks C, D, and E each open with a design or
specification-review unit, per Section 4 — none of their second units
should be scoped in detail until the first unit in the same track has
been reviewed and accepted, mirroring how Sprint 1's own plan refused to
scope Kotlin against an unclosed contract gap (Sprint 1 Plan, Section
12).

## 6. Implementation Units

### Track A — Identity and Permission Hardening (Priority 1)

#### Unit A1: Identity-Aware Permission Engine

- **Objective.** Close gap #40 by giving the platform its first real
  (non-test-fixture) `PermissionEngine` implementation, whose `evaluate`
  resolves `request.principalId` via `IdentityService` as its first
  step and short-circuits to `DENIED` for a Suspended, Revoked, or
  Archived Principal, exactly as `IdentityService.md`'s "Integration
  with Permission Engine" section already specifies.
- **Scope.** Identity resolution and the Suspended/Revoked/Archived
  short-circuit only. For any Principal not caught by that check, this
  unit delegates to the same caller-supplied-decision shape
  `tests/runtime/FakePermissionEngine.kt` already uses for its
  non-identity behaviour — it does not invent policy content, which
  remains gap #25's separate, not-yet-specified concern (Unit A2).
  `tests/runtime/FakePermissionEngine.kt` itself is not modified; it
  remains the orchestration-only test fixture it was built to be.
- **Files expected to change.** New: `src/runtime/DefaultPermissionEngine.kt`,
  `tests/runtime/DefaultPermissionEngineTest.kt`. No change to
  `src/runtime/DefaultExecutionPipeline.kt` — `PermissionEngine` is
  already constructor-injected there, so a new implementation can be
  supplied without touching the pipeline itself.
- **Architecture dependencies.** `IdentityService` (existing,
  `InMemoryIdentityService`), `PermissionEngine` interface (existing,
  unchanged), AD-007 (Permission Decisions Belong to Permission Engine),
  AD-008 (identity authority). Does **not** depend on gaps #35/#36/#37
  being decided first — gap #37's own text names the Permission Engine
  as the future caller expected to treat non-Active Principals as
  "cannot act," which is precisely this unit's short-circuit check, not
  a change to `resolve()` or the lifecycle transition table.
- **Acceptance criteria.** A unit test proves: (1) `evaluate` calls
  `IdentityService.resolve` for `request.principalId` before any other
  logic; (2) a Suspended, Revoked, or Archived Principal always yields
  `DENIED`, regardless of the caller-supplied decision; (3) an Active
  Principal's request reaches the caller-supplied decision unchanged;
  (4) an unresolvable `principalId` yields `DENIED` (mirroring
  `IdentityService.resolve`'s existing "not found" contract, per
  AD-015's "invalid is not denied" distinction — this is a genuine
  denial, not a validation failure, since the identity itself, not the
  request shape, is the problem).
- **Review checkpoint.** Confirm no other component (Task Manager,
  Agent Runtime, Execution Pipeline) gained its own identity-status
  check as a shortcut — this remains the Permission Engine's sole
  responsibility, per AD-007.
- **Android Studio verification.** Full existing suite (234/234) plus
  new `DefaultPermissionEngineTest.kt` cases pass; no existing test
  file's behaviour changes, since `FakePermissionEngine` and
  `DefaultExecutionPipeline` are both untouched.
- **Documentation updates required before the next unit.** Close gap
  #40 in `IMPLEMENTATION_GAPS.md` (move to "Resolved," retain original
  text as historical context, per the established convention). Add an
  `IMPLEMENTATION_HISTORY.md` entry for this unit, following the exact
  format Sprint 1's units used.
- **Definition of Done.** Unit A1 is complete when:
  - `DefaultPermissionEngine` exists in `src/runtime/`.
  - `evaluate` resolves `request.principalId` via `IdentityService`
    before any other logic.
  - Suspended, Revoked, and Archived Principals are always denied.
  - An Active Principal's request reaches the caller-supplied decision
    unchanged.
  - Full existing suite (234/234) plus new tests pass.
  - Gap #40 is closed and `IMPLEMENTATION_HISTORY.md` is updated.

#### Unit A2: Permission Policy Model and Enforcement

- **Objective.** Close gap #25 by giving `DefaultPermissionEngine` real,
  data-defined authorisation policy in place of Unit A1's
  caller-supplied-decision placeholder.
- **Scope.** Gated on the Permission Policy specification note required
  by Section 4 — this unit's Files/Acceptance criteria below assume that
  note exists and defines policy as data the engine evaluates (per
  Readiness Review Section 13's risk mitigation), not conditional logic
  embedded in `DefaultPermissionEngine` itself. This plan does not
  pre-write that policy content; doing so here would repeat exactly the
  sequencing error AD-013/AD-014 exist to prevent (specification-shaped
  content invented inside an implementation plan).
- **Files expected to change.** `src/runtime/DefaultPermissionEngine.kt`
  (replace the Unit A1 placeholder decision path); possibly a new
  `src/contracts/PermissionPolicy.kt` if the specification note defines
  policy as a distinct data type; `tests/runtime/DefaultPermissionEngineTest.kt`
  extended, not replaced.
- **Architecture dependencies.** Unit A1 (this unit extends, not
  replaces, its identity short-circuit). AD-007. The not-yet-written
  Permission Policy specification note (Section 4).
- **Acceptance criteria.** To be finalized once the policy specification
  note exists; at minimum, a policy-driven decision must be provable
  via an assertable, inspectable path (`PermissionEngine.explain`,
  already in the interface) rather than by reading engine source.
- **Review checkpoint.** Confirm policy content lives in data the engine
  reads, not in new conditional branches added to
  `DefaultPermissionEngine.kt` itself — the specific risk named in
  Readiness Review Section 13.
- **Android Studio verification.** Full suite passes; Unit A1's
  identity-short-circuit tests continue to pass unmodified.
- **Documentation updates required before the next unit.** Close gap
  #25 in `IMPLEMENTATION_GAPS.md`. Update `PermissionEngine.md` if the
  policy specification note is folded into it rather than kept
  standalone.
- **Definition of Done.** Unit A2 is complete when:
  - Policy content is expressed as data the engine evaluates, not
    conditional logic added to `DefaultPermissionEngine.kt`.
  - A policy-driven decision is provable via `PermissionEngine.explain`.
  - Unit A1's identity short-circuit tests continue to pass unmodified.
  - Full suite passes.
  - Gap #25 is closed and `IMPLEMENTATION_HISTORY.md` is updated.

**Not scheduled as units in this plan:** gaps #35, #36, #37, and #41
each require a human decision this plan is not authorised to make
(Section 4). They are independent of Track A's two units above and do
not block Track A's start or completion. Once each is decided, it
should be added to a plan revision as its own unit with the same
eight-field structure — not folded silently into Unit A1 or A2.

### Track B — Task Manager Event Handling (Priority 2)

#### Unit B1: Task Manager Agent-Event Subscription

- **Objective.** Implement the Agent-Event subscription
  `TaskManagerRuntimeSpecification.md` §6 already specifies but Sprint 1
  never wired: the Task Manager Runtime subscribing to
  `agent.completed`, `agent.failed`, `agent.cancelled`,
  `agent.action_denied`, and `agent.action_deferred`.
- **Scope.** Subscription and receipt only — this unit makes
  `InMemoryTaskManagerRuntime` aware that these events occurred and
  records them against the relevant Task. It does not yet decide what a
  Task's own status should do in response; that is Unit B2, per §6's own
  "may inform Task state, but do not automatically control it."
- **Files expected to change.** `src/runtime/InMemoryTaskManagerRuntime.kt`
  (add an `EventBus.subscribe` call in construction, mirroring
  `EventBus.subscribe`'s existing `subscriberPrincipalId` parameter
  convention already used elsewhere in the codebase);
  `tests/runtime/InMemoryTaskManagerRuntimeTest.kt` extended.
- **Architecture dependencies.** `EventBus` (existing,
  `InMemoryEventBus`), the `agent.*` event types Sprint 1 Unit 9 already
  publishes, `TaskManagerRuntimeSpecification.md` §6/§11 (already
  approved, unmodified by this unit).
- **Acceptance criteria.** A test proves: given a Task with an
  associated Agent Run, publishing `agent.completed` for that run
  results in the Task Manager Runtime recording the event against the
  correct Task (by `taskId`), without changing `TaskStatus` — that
  remains Unit B2's responsibility.
- **Review checkpoint.** Confirm the Task Manager Runtime still never
  calls `Tool.execute` or holds an invocable `Tool` reference (AD-002,
  AD-003) — subscribing to Agent Events is observation, not a new
  execution path.
- **Android Studio verification.** Full suite passes; existing
  `InMemoryTaskManagerRuntimeTest.kt` cases pass unmodified.
- **Documentation updates required before the next unit.** Add the new
  numbered gap entry named in Section 4 to `IMPLEMENTATION_GAPS.md`
  *before* this unit's own status line is written, then mark it
  resolved by this unit — following the append-only convention (never
  delete, move between summary categories). Add an
  `IMPLEMENTATION_HISTORY.md` entry.
- **Definition of Done.** Unit B1 is complete when:
  - `InMemoryTaskManagerRuntime` subscribes to `agent.completed`,
    `agent.failed`, `agent.cancelled`, `agent.action_denied`, and
    `agent.action_deferred`.
  - A published `agent.completed` event is recorded against the correct
    Task without changing `TaskStatus`.
  - The Task Manager Runtime still holds no invocable `Tool` reference.
  - The new gap entry is logged and marked resolved.
  - Full suite passes; existing tests unmodified.

#### Unit B2: Task Completion Evaluation Rule

- **Objective.** Give the Task Manager Runtime the smallest concrete
  rule for what an Agent Event received in Unit B1 does to a Task's
  status, consistent with §6's "own configured rules" framing.
- **Scope.** Deliberately minimal, mirroring Sprint 1's own precedent of
  a fixed, non-general stand-in (`DeterministicPlannerHarness`) rather
  than a general policy engine: a Task with exactly one associated
  Agent Run transitions to `Completed` when that run publishes
  `agent.completed`, and is left unchanged (with the event recorded) for
  `agent.failed`/`agent.cancelled`/`agent.action_denied`/`agent.action_deferred`.
  Multi-Agent-Run-per-Task coordination and configurable rules remain
  explicitly out of this unit's scope, the same way Sprint 1's plan
  excluded multi-Task coordination from its own scope.
- **Files expected to change.** `src/runtime/InMemoryTaskManagerRuntime.kt`;
  `tests/runtime/InMemoryTaskManagerRuntimeTest.kt` extended.
- **Architecture dependencies.** Unit B1 (consumes the events it
  subscribes to). `TaskLifecycleTransitions`
  (`src/contracts/TaskLifecycle.kt`, existing, unmodified — this unit
  drives an already-legal transition, it does not add one).
- **Acceptance criteria.** A test proves the one-Agent-Run-per-Task
  happy path transitions `Queued -> Completed` on `agent.completed`, and
  a negative test proves `agent.failed` does **not** silently complete
  the Task.
- **Review checkpoint.** Confirm this unit is documented as a fixed,
  minimal rule and not represented anywhere as the general
  Task-completion policy the specification leaves open — the same
  honesty discipline Sprint 1 applied to `DeterministicPlannerHarness`.
- **Android Studio verification.** Full suite passes.
- **Documentation updates required before the next unit.** Update
  `IMPLEMENTATION_HISTORY.md`. Note in `IMPLEMENTATION_GAPS.md` (as a
  new entry or an amendment to the entry closed by Unit B1) that general
  Task-completion policy for multi-Agent-Run Tasks remains open,
  mirroring how Sprint 1's Task Manager Runtime scope note was recorded
  rather than silently left undocumented.
- **Definition of Done.** Unit B2 is complete when:
  - A Task with one Agent Run transitions `Queued -> Completed` on
    `agent.completed`.
  - `agent.failed`/`agent.cancelled`/`agent.action_denied`/`agent.action_deferred`
    do not silently complete the Task.
  - The rule is documented explicitly as fixed and minimal, not general
    policy.
  - Full suite passes.
  - `IMPLEMENTATION_HISTORY.md` is updated and the open
    multi-Agent-Run question is recorded.

### Track C — Agent Runtime Multi-Step Control (Priority 3)

#### Unit C1: Multi-Step Agent Run Model (Design)

- **Objective.** Produce a reviewed design for how an Agent Run can
  contain more than one step, against `AgentRuntimeSpecification.md`
  §5's already-specified `WAITING_FOR_PERMISSION`/`WAITING_FOR_INPUT`/`SUSPENDED`
  states and the already-named `SUSPEND`/`RESUME`/`CANCEL`
  `AgentRunCommand` values.
- **Scope.** Documentation only — no Kotlin. This unit's deliverable is
  a design note (a new section or addendum in
  `AgentRuntimeSpecification.md`, or a standalone design document) that
  states how many steps an Agent Run may take, what state is retained
  between steps, and how a paused run resumes. It does not implement
  anything.
- **Files expected to change.**
  `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
  (or a new standalone design document under `docs/architecture/`, if
  the addition is large enough to warrant one — that choice is this
  unit's own first decision, not pre-made here).
- **Architecture dependencies.** `AgentRuntimeSpecification.md` §5
  (existing, unmodified in substance — this is an addition, not a
  correction, unlike the Architecture v1.1 reconciliation pass). AD-010
  (Model Independence) — the design must not assume any particular
  Planner or model drives step count.
- **Acceptance criteria.** The design note is reviewed and confirms it
  introduces no contradiction with the existing state machine, and
  states explicitly what Unit C2 will and will not implement.
- **Review checkpoint.** Treat this the same way the Agent Runtime and
  Task Manager Runtime Specifications' own review-and-correction passes
  were treated (AD-014) — a specification-level review, before any
  Kotlin is written against it.
- **Android Studio verification.** Not applicable — no source or test
  file changes; confirm the full suite still passes unmodified (a
  documentation-only unit should never cause a regression).
- **Documentation updates required before the next unit.** The design
  note itself is the required update; Unit C2 should not begin until it
  is reviewed and accepted.
- **Definition of Done.** Unit C1 is complete when:
  - A design note exists defining step count, retained state, and
    resume behaviour for a multi-step Agent Run.
  - The design introduces no contradiction with the existing §5 state
    machine.
  - The design states explicitly what Unit C2 will and will not
    implement.
  - The full suite still passes unmodified (no Kotlin touched).
  - The design note is reviewed and accepted.

#### Unit C2: SUSPEND/RESUME/CANCEL Implementation

- **Objective.** Implement `AgentRunCommandType.SUSPEND`, `RESUME`, and
  `CANCEL` in `InMemoryAgentRuntime`, currently explicitly rejected, per
  the design Unit C1 produces.
- **Scope.** To be finalized once Unit C1's design is accepted; this
  plan does not pre-scope its files or acceptance criteria in detail,
  consistent with not inventing the design Unit C1 is responsible for
  producing.
- **Files expected to change.** `src/runtime/InMemoryAgentRuntime.kt` at
  minimum; exact additional files depend on Unit C1's design.
- **Architecture dependencies.** Unit C1 (design). AD-006 (Agent Runtime
  Never Owns Tasks) — a multi-step run must not begin writing to the
  Task Unit B1/B2 own.
- **Acceptance criteria.** To be finalized after Unit C1.
- **Review checkpoint.** Confirm the current single-`ExecutionRequest`-per-run
  happy path (Sprint 1's vertical slice) continues to pass unmodified —
  multi-step support must be additive, not a breaking change to the
  existing `START`-only path.
- **Android Studio verification.** Full suite passes, including
  `tests/runtime/VerticalSliceEndToEndTest.kt` unmodified.
- **Documentation updates required before the next unit.** Update
  `IMPLEMENTATION_HISTORY.md`; update
  `docs/architecture/IMPLEMENTATION_GAPS.md`'s "Agent Runtime supports
  START only" line (`IMPLEMENTATION_HISTORY.md`'s own "Current Known
  Architecture Gaps" list).
- **Definition of Done.** Unit C2 is complete when:
  - `SUSPEND`, `RESUME`, and `CANCEL` are implemented per Unit C1's
    accepted design.
  - The existing single-`ExecutionRequest`-per-run happy path passes
    unmodified.
  - No Task ownership bypass is introduced (AD-006).
  - Full suite passes, including `VerticalSliceEndToEndTest.kt`
    unmodified.
  - `IMPLEMENTATION_HISTORY.md` and `IMPLEMENTATION_GAPS.md` are
    updated.

### Track D — Planner Runtime Progression (Priority 4)

#### Unit D1: Planner Runtime Specification Review-and-Correction Pass

- **Objective.** Give `PlannerRuntimeSpecification.md` the same
  dedicated review-and-correction pass the Agent Runtime and Task
  Manager Runtime Specifications already received, per AD-014's own
  transparency note that Planner's pass "has not yet" happened.
- **Scope.** Documentation only. Reviews the existing specification for
  internal consistency and for consistency with what Sprint 1 actually
  built (`DeterministicPlannerHarness`'s real, if partial, lifecycle
  coverage), the same kind of pass performed for
  `TaskManagerRuntimeSpecification.md` and `AgentRuntimeSpecification.md`
  during the Architecture v1.1 reconciliation. Does not itself define a
  Plan Decision mechanism, a Plan Candidate schema, or resource
  selection rules — it identifies what such a follow-on design (Unit
  D2) would need to define.
- **Files expected to change.**
  `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`.
- **Architecture dependencies.** AD-014 (Architecture Before
  Implementation), AD-010 (Model Independence) — any correction must
  preserve model independence, not narrow it.
- **Acceptance criteria.** The reviewed specification is internally
  consistent and consistent with `DeterministicPlannerHarness`'s actual
  behaviour; any prose-anticipated-capability pattern (AD-013's
  Implementation note) is flagged explicitly rather than silently left
  ambiguous.
- **Review checkpoint.** Same standard as the Agent Runtime/Task Manager
  Runtime reviews: a specification-level review of the specification
  itself, not of Sprint 2 code (none exists yet for this track).
- **Android Studio verification.** Not applicable — confirm the full
  suite still passes unmodified.
- **Documentation updates required before the next unit.** The reviewed
  specification itself; Unit D2 should not be scoped in detail until
  this review is accepted.
- **Definition of Done.** Unit D1 is complete when:
  - `PlannerRuntimeSpecification.md` has received a documented
    review-and-correction pass.
  - The reviewed specification is internally consistent and consistent
    with `DeterministicPlannerHarness`'s actual behaviour.
  - Any prose-anticipated-capability pattern is flagged explicitly.
  - The full suite still passes unmodified (no Kotlin touched).
  - The review is accepted before Unit D2 is scoped in detail.

#### Unit D2: Plan Decision Mechanism Implementation

- **Objective.** Implement a concrete (even if minimal) Plan Decision
  mechanism, per whatever Unit D1's review defines.
- **Scope.** To be finalized after Unit D1; not pre-scoped here, for the
  same reason Unit C2 is not pre-scoped ahead of Unit C1.
- **Files expected to change.** At minimum a new Plan Candidate/Plan
  Decision contract type under `src/contracts/`; exact scope depends on
  Unit D1.
- **Architecture dependencies.** Unit D1. AD-010 (Model Independence) —
  Readiness Review Section 13's specific risk mitigation for this track
  applies directly.
- **Acceptance criteria.** To be finalized after Unit D1.
- **Review checkpoint.** Confirm `DeterministicPlannerHarness` (test-only)
  is not silently promoted into production code as a side effect of this
  unit — a new, separate Plan Decision implementation is expected, not a
  relocation of the existing fixture.
- **Android Studio verification.** Full suite passes, including
  existing `DeterministicPlannerHarnessTest.kt` unmodified.
- **Documentation updates required before the next unit.** Update
  `IMPLEMENTATION_HISTORY.md`.
- **Definition of Done.** Unit D2 is complete when:
  - A concrete Plan Decision mechanism exists per Unit D1's accepted
    review.
  - `DeterministicPlannerHarness` remains test-only, unpromoted to
    production code.
  - Model independence (AD-010) is preserved.
  - Full suite passes, including `DeterministicPlannerHarnessTest.kt`
    unmodified.
  - `IMPLEMENTATION_HISTORY.md` is updated.

### Track E — Resource Discovery (Priority 5)

#### Unit E1: Resource Discovery Operation Specification

- **Objective.** Specify the capability/type-based discovery operation
  `ResourceRegistry.md` does not currently define at all (Readiness
  Review 4.7).
- **Scope.** Documentation only. Follows the `ToolInvocationBinding`
  precedent explicitly: an additive companion interface or a new
  `ResourceRegistry` operation is specified without changing
  `ResourceRegistry`'s existing, already-tested
  `register`/`resolve`/`update`/`listByOwner` operations.
- **Files expected to change.**
  `docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`
  (or a new companion specification document, mirroring
  `ToolInvocationBinding.md`'s relationship to `tool-registry.md`).
- **Architecture dependencies.** The `ToolInvocationBinding` precedent
  (`src/contracts/ToolInvocationBinding.kt`,
  `docs/specifications/volume-03-core-interfaces/ToolInvocationBinding.md`).
  Readiness Review Section 13's risk mitigation: this operation must be
  specified independently of any particular Planner consumer.
- **Acceptance criteria.** The specification defines the new operation's
  signature and semantics without requiring any change to
  `ResourceRegistry`'s existing four operations or their existing tests.
- **Review checkpoint.** Confirm the new operation is additive, matching
  the same "prefer an additive interface over changing an already-tested
  one" discipline `ToolInvocationBinding` established.
- **Android Studio verification.** Not applicable — confirm the full
  suite still passes unmodified.
- **Documentation updates required before the next unit.** The new
  specification itself; Unit E2 should not be scoped in detail until it
  is reviewed and accepted.
- **Definition of Done.** Unit E1 is complete when:
  - A resource discovery operation is specified, additive to
    `ResourceRegistry`.
  - `ResourceRegistry`'s existing four operations and their tests are
    unmodified.
  - The specification is reviewed and accepted.
  - The full suite still passes unmodified (no Kotlin touched).

#### Unit E2: Resource Discovery Implementation

- **Objective.** Implement the operation Unit E1 specifies.
- **Scope.** To be finalized after Unit E1.
- **Files expected to change.** At minimum a new interface file under
  `src/interfaces/` (if a companion interface is chosen) and its
  in-memory implementation under `src/runtime/`; exact scope depends on
  Unit E1.
- **Architecture dependencies.** Unit E1.
- **Acceptance criteria.** To be finalized after Unit E1.
- **Review checkpoint.** Confirm `InMemoryResourceRegistry`'s existing,
  already-tested behaviour is unmodified by this unit.
- **Android Studio verification.** Full suite passes, including
  `tests/runtime/InMemoryResourceRegistryTest.kt` unmodified.
- **Documentation updates required before the next unit.** Update
  `IMPLEMENTATION_HISTORY.md`.
- **Definition of Done.** Unit E2 is complete when:
  - The Unit E1 operation is implemented (new interface/companion plus
    its in-memory implementation).
  - `InMemoryResourceRegistry`'s existing, already-tested behaviour is
    unmodified.
  - Full suite passes, including `InMemoryResourceRegistryTest.kt`
    unmodified.
  - `IMPLEMENTATION_HISTORY.md` is updated.

## Unit Stop Conditions

If implementation of any unit reveals:

- a contract gap,
- a specification contradiction, or
- a new architectural boundary,

implementation of that unit pauses. The issue is classified before
coding continues, and the architecture is corrected — using the same
Architecture → Implementation → Reconciliation → Retrospective process
Sprint 1 established — before implementation resumes. This is not
hypothetical: Sprint 1 hit exactly this pattern while implementing Unit
11A, when a missing `ToolRegistry.resolve()`-to-invocable-`Tool` binding
surfaced mid-implementation and had to be closed as a named contract
gap before coding continued. This section formalises the response
Sprint 1 improvised, rather than relying on it being rediscovered.

## 7. Safety Invariants

These must remain true throughout Sprint 2, checked against every unit
in Section 6 before it is considered done — restating the Readiness
Review's own opening principle (Section 10), not introducing a new one:

- **No second path to Tool execution** (AD-003). No unit in any track
  introduces an alternate route to `Tool.execute` outside
  `DefaultExecutionPipeline`.
- **No second permission-evaluation authority** (AD-007). Unit A1/A2's
  `DefaultPermissionEngine` remains the only component that decides
  APPROVED/DENIED/DEFERRED; no other track's unit substitutes its own
  judgement (Unit B1's checkpoint above states this explicitly for Task
  Manager).
- **No second identity store** (AD-008). All identity resolution
  continues to go through `IdentityService`.
- **No unaudited lifecycle transition** (AD-009). Every new transition
  introduced by Units B2 and C2 publishes its corresponding event, the
  same discipline Sprint 1 Unit 9 established.
- **No Task/Agent Run ownership bypass** (AD-004–AD-006). Unit B1/B2
  never holds an invocable `Tool`; Unit C1/C2 never writes to a Task
  directly.
- **Invalid is not Denied; terminal states remain final** (AD-015,
  AD-016). Any new validation-failure path introduced by a track (none
  currently anticipated, but to be checked at each review checkpoint)
  must use `FAILED`, never `DENIED`, for shape/resolution problems.

## 8. Success Criteria

Directly reusing the Readiness Review's own Section 11 criteria, mapped
to the units that close them:

| Success criterion (Readiness Review §11) | Closed by |
|---|---|
| Identity status enforced before execution | Unit A1 |
| Permission decisions policy-driven | Unit A2 |
| Task Manager reacts to Agent completion events | Units B1, B2 |
| Agent Runtime supports controlled multi-step execution | Units C1, C2 |
| Planner Runtime progresses beyond the deterministic harness | Units D1, D2 |
| No architectural boundary proven during Sprint 1 is bypassed | Section 7 (all units, every review checkpoint) |
| All Sprint 2 units are reflected in Architecture v1.2 | Section 10 below (Output), performed once all tracks close |

Sprint 2 is not required to complete every track to be considered
successful in the same binary sense Sprint 1 was — Track A and Track B
are ready now; Tracks C, D, and E each open on a design/specification
unit whose timeline this plan cannot predict. A partial Sprint 2 that
completes Tracks A and B cleanly, with C/D/E's design units underway, is
consistent with the Readiness Review's own priority order and is not a
failure of this plan.

## 9. Risks

- **Unit A2 being coded before its policy specification note exists.**
  The single most likely place for AD-013/AD-014's sequencing error to
  recur, since Unit A1's placeholder decision path in
  `DefaultPermissionEngine.kt` will compile and pass tests without it.
  Mitigation: Unit A2's own Scope field states the gating explicitly;
  the review checkpoint for Unit A1 should confirm the placeholder is
  documented as a placeholder, not quietly treated as done.
- **Unit B2's minimal rule being mistaken for the general Task-completion
  policy.** Mitigation: Unit B2's review checkpoint and documentation
  update both require this to be stated explicitly, mirroring how
  `DeterministicPlannerHarness` was never allowed to be mistaken for a
  real Planner.
- **Tracks C, D, and E skipping their design/specification unit and
  proceeding straight to code**, since a design unit produces no
  visible Kotlin and may feel like "no progress" compared to Track A/B.
  Mitigation: Section 4's table and this plan's own unit numbering
  (C1 before C2, D1 before D2, E1 before E2) make the dependency
  explicit and non-optional.
- **Decision-gated gaps (#35, #36, #37, #41) never being decided,
  quietly blocking Track A from ever being considered "complete."**
  Mitigation: these are explicitly not scheduled as units in this plan
  (Section 6); Track A's completion is defined by Units A1 and A2 only,
  and the four decision-gated gaps should be tracked as a separate,
  parallel decision backlog that does not gate Sprint 2's Section 8
  success criteria.
- **Cross-track interference.** Track B's Unit B1 subscribes to `agent.*`
  events that Track C's Unit C2 will eventually make more complex
  (multiple completions per Agent Run, once multi-step exists).
  Mitigation: Unit B1/B2 are scoped explicitly against Sprint 1's
  current one-`ExecutionRequest`-per-run Agent Runtime; if Track C lands
  first, Track B's units should be re-reviewed for continued correctness
  before being considered final, not silently assumed compatible.

## 10. Output

**Ready for Sprint 2 coding: Partially.**

- **Track A, Unit A1** is ready to begin immediately — no specification
  or decision gap blocks it (Section 4).
- **Track A, Unit A2** is blocked until the Permission Policy
  specification note (Section 4) is written and reviewed.
- **Track B, Unit B1** is ready once the new gap entry (Section 4) is
  logged in `IMPLEMENTATION_GAPS.md` — a bookkeeping step, not a design
  blocker.
- **Track B, Unit B2** depends on Unit B1.
- **Track C, D, and E** each open on a documentation-only design or
  specification-review unit (C1, D1, E1); their second units (C2, D2,
  E2) are not yet scoped in detail and should not be, until the first
  unit in the same track is reviewed and accepted.
- **Gaps #35, #36, #37, and #41** remain outside this plan's scope
  entirely, pending human decisions this plan is not authorised to make.

**The first implementation task is Unit A1**, matching the Readiness
Review's own Section 9 recommendation and Section 4's finding that it is
the only track-opening unit requiring neither a specification nor a
human decision first.

Once all tracks reach the state Section 8 describes, this plan's
closing step is the same one Sprint 1's plan named in its own Section
11: review the completed work against Section 7's invariants, and
reflect every completed unit in Architecture v1.2, per the Readiness
Review Section 11's final criterion.

## Sprint Exit Criteria

Sprint 2 is complete when:

- Every planned implementation unit is Complete.
- All Android Studio tests pass.
- `IMPLEMENTATION_HISTORY.md` is updated.
- `IMPLEMENTATION_GAPS.md` reflects all resolved and remaining gaps.
- Architecture v1.2 reconciliation is complete.
- Sprint 2 Retrospective is written.
- Sprint 3 Readiness Review has been drafted.

## Governance Statement

Sprint 2 proceeds one unit at a time. No subsequent unit begins until
the current unit has: an architecture review confirming no invariant
(Section 7) is violated; a completed implementation; a passing Android
Studio verification; reconciled documentation (per that unit's own
Documentation field); and a committed repository state. Only then does
the next unit begin. This is the same discipline that made Sprint 1
successful, made explicit rather than left implicit.

Once accepted, this plan is frozen as **v1.0**. During implementation,
this document should only be edited to update the Sprint Dashboard's
Status column, to check off a Definition of Done item, or to record a
change already approved through the Unit Stop Conditions process
(above) — never to quietly absorb a mid-implementation change in scope
or sequence. If a unit uncovers something that genuinely requires a
different plan, the architecture is corrected first, and this plan is
revised afterward, deliberately, as a new reviewed version — not edited
in place during coding.

## Related

- `docs/architecture/PARKER_SPRINT_2_ARCHITECTURE_REVIEW.md`
- `docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md`
- `docs/implementation/IMPLEMENTATION_HISTORY.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md`
- `docs/architecture/ARCHITECTURE_DECISIONS.md`
- `docs/architecture/IdentityService.md`
- `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
- `docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`
- `docs/specifications/volume-03-core-interfaces/ToolInvocationBinding.md`
- `docs/architecture/tool-registry.md`
