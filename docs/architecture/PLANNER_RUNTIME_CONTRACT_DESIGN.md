# Planner Runtime Contract Design

## Status

Sprint: Sprint 3, Track D, Unit D1A (Design)
Version: 0.1-draft
Status: **Design proposal, not yet reviewed or accepted.**

**This document is specification-level design only.** No Kotlin is
implemented, proposed as a diff, or changed by it. Neither `src/` nor
`tests/` is touched, and no existing test is modified.
`docs/implementation/IMPLEMENTATION_HISTORY.md` and
`docs/architecture/IMPLEMENTATION_GAPS.md` are both untouched — per this
unit's own instructions, those remain unchanged until an implementation
unit actually changes `src/`/`tests/` again.

### Why this unit exists

Sprint 3, Track D, Unit D2 ("Plan Decision Mechanism Implementation") was
attempted directly against `docs/architecture/PLANNER_RUNTIME_PROGRESSION_DESIGN.md`
(Unit D1). A subsequent architectural traceability review found that most
of the public Kotlin types Unit D2 introduced — `PlanCandidateId`,
`PlanRejection`, `PlanDecisionResult`, the exact shape of `PlanCandidate`
and `PlanDecision`, `PlanningRequest`, `PlanningSessionResult`,
`PlannerSessionStatus`/`PlannerSessionLifecycleTransitions`, and
`InMemoryPlannerRuntime`'s entire public surface — were shaped **during**
that implementation attempt, not pre-specified at the field/signature
level by an accepted architecture document. Unit D1 authorised two
concepts by name only ("a `PlanCandidate` type," "a concrete Plan Decision
mechanism"); it did not specify their fields, and it did not anticipate a
new production lifecycle contract or a `PlannerRuntime` interface at all.

This is a structurally different situation from Track C, where Unit C1
specified `AgentStepContext`/`AgentStepDecision`/`AgentStepSource`/
`AgentPolicy` field-by-field *before* Unit C2 wrote any Kotlin. Track D's
Unit D1 was, correctly, scoped narrower by the authoritative
`SPRINT_2_IMPLEMENTATION_PLAN.md` ("does not itself define a Plan Decision
mechanism, a Plan Candidate schema... it identifies what such a follow-on
design would need to define") — but that narrower scope means the
field-level design work Unit C1 did for Track C was never done for Track
D. Unit D2's implementation attempt is therefore being treated as
**exploratory only**, not committed, and this document performs the
missing design pass retroactively, before any Kotlin from that attempt is
accepted.

## Review

Reviewed, in priority order:

1. `docs/architecture/parker-constitution.md`
2. `docs/architecture/ARCHITECTURE_DECISIONS.md` (especially AD-002,
   AD-005, AD-007, AD-010, AD-013)
3. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001)
4. `docs/architecture/PROJECT_GOVERNANCE.md` (still empty; no rule beyond
   PES-001/Constitution to consult)
5. `docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md`, Track D
6. `docs/architecture/PLANNER_RUNTIME_PROGRESSION_DESIGN.md` (Unit D1,
   accepted)
7. `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
8. `src/contracts/TaskProposal.kt` (the only existing Planner-adjacent
   production contract), `src/contracts/AgentRunCommand.kt`
   (`AgentRunCommandChannel`, the closest existing "runtime public
   interface" precedent), `src/runtime/InMemoryTaskManagerRuntime.kt`,
   `src/runtime/InMemoryAgentRuntime.kt` (both examples of the
   established "`InMemory*` implements a pre-existing interface" pattern)
9. The exploratory Unit D2 attempt itself (`src/contracts/PlanDecision.kt`,
   `src/contracts/PlannerSessionLifecycle.kt`,
   `src/runtime/DefaultPlanDecision.kt`,
   `src/runtime/InMemoryPlannerRuntime.kt`, and their tests) — read as
   *evidence of a design attempt to be judged*, not as a pre-approved
   baseline to be rubber-stamped.

---

## Constitutional Boundaries

Restated up front, since every type below is checked against these:

- **Planner proposes only.** No type in this document creates a Task
  Manager Task; the only channel remains `TaskProposalIntake.submitProposal`
  (unmodified, `src/contracts/TaskProposal.kt`).
- **Task Manager owns tasks.** No type here reads or writes `Task`/`TaskStatus`.
- **Permission Engine authorises execution.** No type here produces a
  `PermissionDecision`; any permission-shaped field (e.g.
  `anticipatedPermissionActions`) is advisory labelling only, exactly as
  `TaskProposal.kt` already establishes.
- **Execution Pipeline executes.** No type here references `Tool`,
  `ToolInvocationBinding`, or `ExecutionRequest`.
- **Planner must not create tasks directly.** `PlanningSessionResult.Completed`
  carries a `TaskProposalId` returned *by* `TaskProposalIntake`, never a
  `TaskId` minted by the Planner Runtime itself.
- **Planner must not grant permission.** Confirmed for every type below;
  none has a field or method that evaluates, grants, or bypasses
  permission.
- **Planner must not execute tools.** Confirmed; no type below holds or
  calls an invocable `Tool` reference.

---

## Type-by-Type Design

### 1. `PlanCandidateId`

- **Purpose.** Identifies one Plan Candidate within the scope of a single
  Plan Decision evaluation.
- **Ownership.** Planner Runtime only; never referenced by the Task
  Manager Runtime, Permission Engine, or Execution Pipeline.
- **Fields.** `value: String` (value class), non-blank — identical shape
  to every other `*Id` value class in `src/contracts/Identifiers.kt`
  (`PrincipalId`, `ResourceId`, `RequestId`) and to `TaskProposalId`/`TaskId`/
  `PlanningSessionId`.
- **Allowed values.** Any non-blank string. Uniqueness *within one Plan
  Decision call* is enforced by `PlanDecision` itself (a duplicate is a
  rejection, not a construction-time error) — this type does not enforce
  uniqueness on its own, deliberately, so a duplicate can be constructed
  and tested against.
- **Lifecycle role.** Exists only during `ANALYSING`. It is never carried
  onto the resulting `TaskProposal` (confirmed: `TaskProposal.kt` has no
  `planCandidateId` field) — its last use is as event payload data
  (`planner.candidate_generated`/`planner.candidate_rejected`/
  `planner.proposal_created`) for audit/tracing.
- **Relationship to `PlannerRuntimeSpecification.md`.** Section 4 ("Plan
  Candidate") and Section 11's event table ("candidate reference") imply a
  candidate identity but never shape one. This type gives that implied
  reference a concrete shape for the first time.
- **Relationship to `TaskProposal`.** None directly — no field of
  `TaskProposal` is a `PlanCandidateId`.
- **`src/contracts` or internal?** `src/contracts` — structurally
  identical to six already-approved identifier types; the lowest-risk
  type in this document.
- **Required for D2, or deferred?** Required. Multiple candidates cannot
  be distinguished without it.
- **Disposition.** **Approved as designed** in the Unit D2 attempt, no
  change.

### 2. `PlanCandidate`

- **Purpose.** One internally-generated candidate decomposition of a Goal
  (`PlannerRuntimeSpecification.md` Section 4), supplied to `PlanDecision`
  for evaluation.
- **Ownership.** Produced by whatever mechanism generates candidates (out
  of scope for Track D entirely — "Planner reasoning"/"LLM integration"
  are explicitly excluded); consumed by `PlanDecision` and, if selected,
  by the Planner Runtime's `TaskProposal`-construction step.
- **Fields — two tiers, not one flat list:**
  - **Required (needed for Plan Decision's own evaluation):**
    `planCandidateId: PlanCandidateId`, `goal: String`.
  - **Optional carry-forward fields (needed only so a selected candidate
    can become a well-formed `TaskProposal` without a separate re-entry
    step):** `rationale: String = ""`, `riskEstimate: RiskEstimate? = null`,
    `requiredCapabilities: Set<PermissionAction> = emptySet()`,
    `anticipatedPermissionActions: Set<PermissionAction> = emptySet()`,
    `constraints: List<String> = emptyList()`,
    `dependencies: List<ProposalDependency> = emptyList()`,
    `contextReferences: List<String> = emptyList()`,
    `resourceReferences: List<ResourceId> = emptyList()`,
    `expectedOutputs: String = ""`. Every one of these has an identical
    name, type, and default in `TaskProposal` already — none is a newly
    invented field shape; each is a reuse of an already-approved
    `TaskProposal` field, not new schema.
- **Allowed values.** `goal` may legitimately be blank or mismatched with
  the session's own Goal — **deliberately no constructor-level
  validation**, unlike `TaskProposal.goal`. A `PlanCandidate` is
  pre-evaluation input; recognising it as malformed is `PlanDecision`'s
  job (see below), not something construction should forbid, since
  forbidding it would make the rejection path untestable.
- **Lifecycle role.** Exists only during `ANALYSING`; a selected instance
  is read once (to build a `TaskProposal`) and then discarded.
- **Relationship to `PlannerRuntimeSpecification.md`.** Section 4 defines
  the concept; no schema is given anywhere in the specification or in any
  existing contract — `DeterministicPlannerHarness.kt`'s own KDoc confirms
  "no formal schema exists anywhere in this repository for Plan
  Candidate." This document is the first place a schema is actually
  approved.
- **Relationship to `TaskProposal`.** Field-for-field carry-forward for
  every optional field, as described above — a selected `PlanCandidate`'s
  optional fields map 1:1 onto the constructed `TaskProposal`'s fields of
  the same name.
- **`src/contracts` or internal?** `src/contracts`.
- **Required for D2, or deferred?** The two required fields are needed
  for D2. The optional carry-forward fields are not strictly required by
  Plan Decision's own logic (which only reads `planCandidateId`/`goal`),
  but are approved now as low-risk, zero-new-schema, forward-compatible
  fields rather than deferred, since deferring them would require a
  breaking field addition later the moment any candidate needs to carry a
  risk estimate or constraint.
- **Disposition.** **Approved after adjustment**: the field *set* the
  Unit D2 attempt used is correct; what was missing was this explicit
  two-tier justification (required vs. carry-forward) and the absence of
  a constructor-level `goal` check, which the attempt already got right
  but did not explain as a deliberate design choice at the architecture
  level.

### 3. `PlanRejection`

- **Purpose.** Represents one declined `PlanCandidate`
  (`PlannerRuntimeSpecification.md` Section 4, "Plan Rejection" — "not an
  error").
- **Ownership.** Produced by `PlanDecision`; consumed by the Planner
  Runtime for event publication and surfaced to the caller via
  `PlanDecisionResult`/`PlanningSessionResult`.
- **Fields — corrected from the Unit D2 attempt:**
  ```
  enum class PlanRejectionReason {
      DUPLICATE_CANDIDATE_ID,
      BLANK_GOAL,
      GOAL_MISMATCH,
      NOT_SELECTED,
  }

  data class PlanRejection(
      val planCandidateId: PlanCandidateId,
      val reason: PlanRejectionReason,
      val detail: String,
  )
  ```
  The Unit D2 attempt used a single free-text `reason: String`, requiring
  every test that checked *why* a candidate was rejected to substring-match
  human prose (`reason.contains("duplicate", ignoreCase = true)`). That is
  a code smell, not a virtue: this unit's own "completely explainable and
  repeatable" requirement is better served by a closed, four-value enum
  (exhaustive over every rule `PlanDecision` can apply) plus a free-text
  `detail` for the specific offending value — machine-checkable *and*
  human-readable, rather than only the latter.
- **Allowed values.** `reason` is exactly one of the four enum values,
  corresponding 1:1 to `PlanDecision`'s four evaluation rules (see below).
  `detail` must be non-blank.
- **Lifecycle role.** Exists only during `ANALYSING`; never carried past
  it.
- **Relationship to `PlannerRuntimeSpecification.md`.** Section 4 defines
  "Plan Rejection" as a concept only; no schema exists anywhere. This
  document is the first approval of one.
- **Relationship to `TaskProposal`.** None.
- **`src/contracts` or internal?** `src/contracts` (both `PlanRejection`
  and `PlanRejectionReason`).
- **Required for D2, or deferred?** Required.
- **Disposition.** **Revise to match this design** — the `reason: String`
  field must become `reason: PlanRejectionReason` + `detail: String`
  before this type is accepted.

### 4. `PlanDecisionResult`

- **Purpose.** The sealed outcome of one `PlanDecision.decide` call.
- **Ownership.** Produced by `PlanDecision`; consumed by the Planner
  Runtime.
- **Fields.**
  ```
  sealed class PlanDecisionResult {
      data class Selected(
          val winner: PlanCandidate,
          val rejections: List<PlanRejection>,
      ) : PlanDecisionResult()

      data class NoViableCandidate(
          val rejections: List<PlanRejection>,
      ) : PlanDecisionResult()
  }
  ```
  Unchanged from the Unit D2 attempt except that `rejections` now carries
  the corrected `PlanRejection` shape.
- **Allowed values.** Exactly these two variants — mirrors
  `AgentStepDecision`'s "sealed, exhaustive, no silent third case"
  precedent. A future additional variant (e.g. if `PlanDecision` itself
  ever needs a "defer this decision" outcome) would be an additive change
  to this sealed type, not a redesign, mirroring `AgentStepDecision`'s own
  forward-compatibility note — no such variant is needed or added now.
- **Lifecycle role.** Transient — constructed and consumed within one
  `ANALYSING` step.
- **Relationship to `PlannerRuntimeSpecification.md`.** Not itself named;
  Section 4 describes the outcome only in prose ("a Plan Candidate that
  is not selected is a Plan Rejection"). New architecture, approved here.
- **Relationship to `TaskProposal`.** None directly — this is an
  intermediate structure consumed before `TaskProposal` construction.
- **`src/contracts` or internal?** `src/contracts` — must be visible to
  any external `PlanDecision` implementation (see AD-010, below).
- **Required for D2, or deferred?** Required.
- **Disposition.** **Approved as designed**, contingent only on
  `PlanRejection`'s own correction above.

### 5. `PlanDecision`

- **Purpose.** The seam by which a Planning Session chooses among its
  supplied Plan Candidates — the "concrete Plan Decision mechanism" Unit
  D1 Section 11 names.
- **Ownership.** Injected into the Planner Runtime; `DefaultPlanDecision`
  is today's only implementation.
- **Fields (interface signature) — corrected from the Unit D2 attempt:**
  ```
  interface PlanDecision {
      suspend fun decide(goal: String, candidates: List<PlanCandidate>): PlanDecisionResult
  }
  ```
  The Unit D2 attempt declared `decide` non-`suspend`, reasoning that a
  deterministic, no-external-state decision needs no suspension. That
  reasoning is correct for `DefaultPlanDecision` specifically, but it is
  the wrong basis for the *interface* signature: `PlannerRuntimeSpecification.md`
  Section 14 ("Relationship to Future Systems") states plainly that
  "whatever mechanism eventually generates Plan Candidates -- rule-based
  logic, a hosted or local model, a human-in-the-loop tool, or some
  combination -- is upstream of this document and interchangeable" (AD-010,
  Model Independence) — and the same reasoning applies to what *chooses*
  among them. A future model-backed or human-in-the-loop `PlanDecision`
  would need to suspend (an HTTP call, a UI wait). Declaring `decide`
  non-`suspend` now would force a breaking interface change the moment
  such an implementation is needed, exactly the kind of foreclosed option
  `AgentStepSource` avoided by being declared `suspend` from the start
  even though its own deterministic test/fixture implementations never
  suspend either. `DefaultPlanDecision` gains a mechanical `suspend`
  modifier with no other change to its body.
- **Allowed values.** N/A (behavioural interface).
- **Lifecycle role.** Consulted exactly once per Planning Session, during
  `ANALYSING`.
- **Relationship to `PlannerRuntimeSpecification.md`.** Section 4 ("Plan
  Decision"), Section 2 (Design Goals — Model Independence), AD-010.
- **Relationship to `TaskProposal`.** None directly — `PlanDecision`'s
  output is a `PlanCandidate`, converted to a `TaskProposal` by the
  Planner Runtime, not by `PlanDecision` itself.
- **`src/contracts` or internal?** `src/contracts`.
- **Required for D2, or deferred?** Required — this is the exact
  authorisation Unit D1 gave.
- **Disposition.** **Revise to match this design** — `decide` must become
  `suspend fun` before this interface is accepted.

### 6. `PlanningRequest`

- **Purpose.** The input that starts one Planning Session
  (`PlannerRuntimeSpecification.md` Section 4, "Planning Request" — "a
  Goal, an initiating Principal, and (optionally) declared Constraints").
- **Ownership.** Caller-supplied (whatever originates a Planning Request);
  consumed by the Planner Runtime.
- **Fields — corrected from the Unit D2 attempt:**
  ```
  data class PlanningRequest(
      val planningSessionId: PlanningSessionId,
      val initiatingPrincipalId: PrincipalId,
      val goal: String,
      val correlationId: String,
      val source: RequestOrigin = RequestOrigin.TEXT,
      val priority: RequestPriority = RequestPriority.NORMAL,
  )
  ```
  **The Unit D2 attempt also embedded `candidates: List<PlanCandidate>`
  directly on this type. That is a genuine design defect, not a
  simplification worth keeping.** Section 4 does not mention candidates as
  part of a Planning Request at all — a Planning Request precedes
  `CONTEXT_GATHERING`/`ANALYSING`, and Plan Candidates are only supposed to
  exist *during* `ANALYSING` (generated internally, by whatever mechanism,
  after the request already exists). Bundling `candidates` into the
  request conflates "the thing that starts a Planning Session" with "the
  not-yet-generated-at-request-time candidate set," which only appeared
  reasonable because Unit D2 has no real candidate-generation step to
  place between them. Candidates must instead be a separate parameter to
  the Planner Runtime's own `plan` operation (see the `PlannerRuntime`
  interface, below), keeping `PlanningRequest` itself spec-faithful.
- **Allowed values.** `goal`/`correlationId` non-blank (unchanged from the
  attempt).
- **Lifecycle role.** Constructed once per Planning Session; consumed at
  `CREATED`.
- **Relationship to `PlannerRuntimeSpecification.md`.** Direct
  implementation of Section 4's "Planning Request" concept — now field-
  faithful to it, unlike the attempt.
- **Relationship to `TaskProposal`.** `source`/`priority` reuse
  `TaskProposal`'s own reuse of `RequestOrigin`/`RequestPriority`,
  mirroring the same pattern the specification's Section 10 already
  establishes for `TaskProposal` itself.
- **`src/contracts` or internal?** `src/contracts`.
- **Required for D2, or deferred?** Required, with the field-shape
  correction above.
- **Disposition.** **Revise to match this design** — `candidates` must be
  removed from this type and passed as a separate argument instead.

### 7. `PlanningSessionResult`

- **Purpose.** The terminal outcome of one Planning Session, returned to
  whatever called the Planner Runtime.
- **Ownership.** Returned by the Planner Runtime.
- **Fields.**
  ```
  sealed class PlanningSessionResult {
      data class Completed(
          val planningSessionId: PlanningSessionId,
          val taskProposalId: TaskProposalId,
          val disposition: TaskProposalDisposition,
          val rejections: List<PlanRejection>,
      ) : PlanningSessionResult()

      data class Rejected(
          val planningSessionId: PlanningSessionId,
          val taskProposalId: TaskProposalId,
          val disposition: TaskProposalDisposition.Rejected,
          val rejections: List<PlanRejection>,
      ) : PlanningSessionResult()

      data class Failed(
          val planningSessionId: PlanningSessionId,
          val reason: String,
          val rejections: List<PlanRejection>,
      ) : PlanningSessionResult()
  }
  ```
  Unchanged from the Unit D2 attempt (aside from `rejections`'s corrected
  element type). `Failed.reason` was considered for the same
  enum-plus-detail treatment as `PlanRejection.reason` and deliberately
  **not** changed: unlike a per-candidate rejection (evaluated
  automatically, many times, by a fixed rule set an enum can exhaustively
  name), a Planning Session's own terminal failure is a one-off summary
  for a human/log to read, with only two origins today (unresolvable
  identity; no viable candidate) that are already distinguishable by
  which branch of code returns `Failed` at all. A free-text `reason`
  remains appropriate here.
- **Allowed values.** N/A (data shape).
- **Lifecycle role.** Constructed exactly once, at whichever terminal
  state the Planning Session reaches.
- **Relationship to `PlannerRuntimeSpecification.md`.** Not itself named
  — the specification describes internal lifecycle states (Section 5),
  never a caller-facing return value. This is genuinely new architecture,
  approved here for the first time (there is no prior precedent even like
  `AgentRunCommandResult`, since no Planner Runtime implementation has
  ever existed before this attempt).
- **Relationship to `TaskProposal`.** `Completed`/`Rejected` both carry
  the `TaskProposalId` `TaskProposalIntake.submitProposal` returned a
  disposition for — never a `TaskId` the Planner Runtime mints itself
  (Constitutional Boundary: "Planner must not create tasks directly").
- **`src/contracts` or internal?** `src/contracts`.
- **Required for D2, or deferred?** Required.
- **Disposition.** **Approved as designed**, contingent only on
  `PlanRejection`'s own correction.

### 8. `PlannerSessionStatus` / 9. `PlannerSessionLifecycleTransitions`

- **Purpose.** Production representation of the Planning Session
  lifecycle subset the Planner Runtime actually drives.
- **Ownership.** Shared architecture-level state model, mirroring
  `TaskStatus`/`TaskLifecycleTransitions` and
  `AgentRunStatus`/`AgentRunLifecycleTransitions`'s existing precedent
  placement and shape.
- **Fields.**
  ```
  enum class PlannerSessionStatus {
      CREATED, CONTEXT_GATHERING, ANALYSING, PROPOSING, SUBMITTED,
      COMPLETED, REJECTED, FAILED,
  }
  ```
  with the transition map:
  `CREATED -> CONTEXT_GATHERING -> ANALYSING -> {PROPOSING, FAILED}`,
  `PROPOSING -> SUBMITTED -> {COMPLETED, REJECTED}`. Unchanged from the
  Unit D2 attempt.
- **Allowed values.** Exactly the eight states above; no other value.
  Eight of `PlannerRuntimeSpecification.md` Section 5's ten specified
  states — `WAITING_FOR_INPUT` and `CANCELLED` are real, specified states
  this subset does not reach (no Planner reasoning to detect insufficient
  context; no cancellation command channel), a documented coverage gap,
  not an invented restriction. No edge beyond what Section 5's own diagram
  contains was added — in particular, **no `CREATED -> FAILED` edge**:
  Section 5 has none, so an unresolvable initiating Principal is handled
  as a pre-`CREATED` guard (no session record ever created), never a
  lifecycle transition.
- **Lifecycle role.** This *is* the lifecycle role — tracks one Planning
  Session end to end.
- **Relationship to `PlannerRuntimeSpecification.md`.** Direct,
  faithful subset of Section 5's ten-state diagram, transcribed rather
  than invented, exactly mirroring how `TaskLifecycle.kt`'s own header
  states its enum is "transcribed exactly... no invented branching."
- **Relationship to `TaskProposal`.** None.
- **`src/contracts` or internal?** `src/contracts` — matches
  `TaskLifecycle.kt`/`AgentRunLifecycle.kt`'s placement exactly, and must
  be public regardless, since it is the return type of a public
  `getSessionStatus`-style method.
- **Required for D2, or deferred?** This is the one pair of types Unit
  D1 never anticipated *at all* — not named in Section 11's WILL-implement
  list, and Unit D1's own Section 5 review concluded the *existing*
  ten-state specification needed no correction, which is a different
  question from whether a *new production Kotlin representation* of it
  should be authorised. This document authorises it now, as new
  architecture, because the explicit Unit D2 testing requirement
  ("lifecycle transitions") cannot be met without some observable
  production state, and because every other runtime in this codebase
  (Task Manager, Agent) already has exactly this kind of contract.
- **Disposition.** **Approved as designed**, but explicitly understood as
  new architecture being granted by *this* document, not something Unit
  D1 already implicitly authorised.

### 10. Planner Runtime public interface

- **Purpose.** Close the traceability finding that `InMemoryPlannerRuntime`
  implemented no pre-existing interface at all — unlike every other
  `InMemory*Runtime` in this codebase (`InMemoryTaskManagerRuntime :
  TaskProposalIntake`, `InMemoryAgentRuntime : AgentRunCommandChannel`,
  `InMemoryIdentityService : IdentityService`), all of which implement an
  interface named in an earlier phase before the concrete class existed.
- **Ownership.** The Planner Runtime component; one implementation today.
- **Fields (interface signature):**
  ```
  interface PlannerRuntime {
      suspend fun plan(request: PlanningRequest, candidates: List<PlanCandidate>): PlanningSessionResult
  }
  ```
  Named `PlannerRuntime` directly (rather than a "Channel"/"Intake"-style
  functional name) since, unlike `AgentRunCommandChannel` (which
  multiplexes several `AgentRunCommandType`s through one `submit` method)
  or `TaskProposalIntake` (a single intake operation named for its
  receiving role), the Planner Runtime's public contract is genuinely just
  "run a Planning Session" — one operation, named for what it does. This
  naming choice is this document's own decision and may be revisited on
  review.
- **Allowed values.** N/A.
- **Lifecycle role.** The entry point for every Planning Session.
- **Relationship to `PlannerRuntimeSpecification.md`.** No prior interface
  existed for the Planner Runtime anywhere in `src/interfaces` or
  `src/contracts` — this is the first one. `PlannerRuntimeSpecification.md`
  itself has never been promoted to an implementation phase before this
  Track, per its own Status header.
- **Relationship to `TaskProposal`.** Indirect only, via `PlanningRequest`/
  `PlanningSessionResult`.
- **`src/contracts` or internal?** `src/contracts`, matching
  `AgentRunCommand.kt`'s placement of `AgentRunCommandChannel` and
  `TaskProposal.kt`'s placement of `TaskProposalIntake`.
- **Required for D2, or deferred?** Required — this is the core
  corrective action of this document.
- **Disposition.** **New requirement, not present in the Unit D2
  attempt.** Must be added before `InMemoryPlannerRuntime` is accepted.

### 11. Must `InMemoryPlannerRuntime` implement that interface?

**Yes, required, no exception.** Every other in-memory runtime in this
codebase implements a pre-existing interface; a Planner Runtime
implementation that does not would be the only structural exception, for
no stated reason. `InMemoryPlannerRuntime` must be declared
`class InMemoryPlannerRuntime(...) : PlannerRuntime`, with its `plan`
method's signature updated to accept `candidates` as a separate parameter
(per `PlanningRequest`'s correction above) rather than reading
`request.candidates`. `getSessionStatus` may remain a class-specific
method, not part of the formal interface — mirroring how
`InMemoryTaskManagerRuntime.getTask`/`listTasks` and
`InMemoryAgentRuntime.getAgentRun`/`listAgentRuns` are all
implementation-specific inspection methods, not part of
`TaskProposalIntake`/`AgentRunCommandChannel`.

---

## Disposition of the Exploratory Unit D2 Implementation

Three options were posed: accepted after adjustment, revised to match the
approved contracts, or discarded and rewritten.

**Determination: revised to match the approved contracts.**

Reasoning: the exploratory implementation's *substance* is sound and
should not be discarded —

- `DefaultPlanDecision`'s four-rule evaluation (duplicate id, blank goal,
  goal mismatch, not-selected-in-generation-order) is exactly right and
  matches every requirement (deterministic, explainable, no heuristic
  scoring, no risk-based ranking) this Track's instructions specified. It
  needs only a mechanical `suspend` modifier and to construct
  `PlanRejection` with the corrected `reason`/`detail` shape.
- `InMemoryPlannerRuntime`'s event sequencing, lifecycle progression, and
  its handling of the unresolvable-Principal pre-`CREATED` guard and the
  `Deferred`/`Split`/`Merged` -> `COMPLETED` classification are all
  correctly reasoned against the specification and require no change in
  behaviour — only the addition of the `PlannerRuntime` interface and the
  `candidates`-as-parameter signature change.
- The test suite's *coverage* (zero/one/multiple candidates, duplicate
  and invalid rejection, event publication, lifecycle transitions,
  boundary conditions) is complete and does not need to be re-conceived —
  only updated at the call sites affected by the signature changes above,
  and at the handful of assertions that currently substring-match
  `PlanRejection.reason` text (these become exact-equality checks against
  `PlanRejectionReason` values, which is a strictly stronger, not weaker,
  assertion).

This is more than a cosmetic "adjustment" (real interface and field-shape
changes are required, several of which ripple into the runtime class and
existing tests), which is why "accepted after adjustment" is not the
right characterisation — but it is far short of "discarded and
rewritten," since the algorithm, event model, and test scenarios all carry
forward unchanged in intent.

---

## Conclusion

**Unit D2 may proceed after aligning to this contract design.**
