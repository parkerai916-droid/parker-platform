# Parker Sprint 2 Architecture Readiness Review

## Status

This is an **architecture review, not an implementation plan and not
Sprint 2 code.** No Kotlin, test, or Gradle file is touched by this
document, and no Sprint 2 implementation unit begins here. It expands a
Sprint 2 planning draft into an evidence-grounded review, citing
`docs/architecture/IMPLEMENTATION_GAPS.md`, `docs/architecture/ARCHITECTURE_DECISIONS.md`,
`docs/implementation/IMPLEMENTATION_HISTORY.md`,
`docs/reviews/SPRINT_1_RETROSPECTIVE.md`, and the Volume 3–6
specifications directly, rather than restating the draft's own framing
as settled fact. Where the draft's questions already have a
specification-level answer, this document says so and cites it. Where
they do not, this document says that too, rather than inventing one.

This document does not create a new Architecture Decision, does not
amend any existing specification, and does not commit Sprint 2 to any
of the candidate areas below beyond the recommendation in Section 6.

## 1. Purpose

Sprint 1 proved that Parker's core architecture can execute one complete
request from Goal to Tool, with permission checking, resource
propagation, lifecycle events, and event collection, all under test
(`docs/reviews/SPRINT_1_RETROSPECTIVE.md`, Section 12: "a fixed Goal now
reaches real Tool execution through every architecturally-mandated
stage"). Sprint 2's purpose is to expand that proven foundation into a
more capable runtime while preserving the architectural discipline
Sprint 1 established — not to add capability for its own sake, and not
to redesign the core runtime the vertical slice already exercises.

## 2. Sprint 1 Baseline (Confirmed State)

| Fact | Value | Source |
|---|---|---|
| Architecture version | v1.1 | `IMPLEMENTATION_HISTORY.md` Architecture Baseline (pending the version-stamp update noted in the Architecture v1.1 Review Report) |
| Repository tags | `sprint1-complete`, `architecture-v1.1` | `docs/reviews/SPRINT_1_RETROSPECTIVE.md` Status |
| Tests | 234/234 passing | `IMPLEMENTATION_HISTORY.md` Repository Status |
| Vertical slice | Proven end-to-end, real objects at every hop | `tests/runtime/VerticalSliceEndToEndTest.kt`; `IMPLEMENTATION_HISTORY.md` "Current Vertical Slice" |
| Tool execution | Real (`Tool.validate()`/`Tool.execute()` actually invoked) | Unit 11A, commit `13c9322`; closed `IMPLEMENTATION_GAPS.md` #32 |
| Resource references | Propagate `TaskProposal` → `AgentRunCommand` → `ExecutionRequest` | Unit 11B, commit `4a44abe` |
| Lifecycle events | Published (`planner.*`/`task.*`/`agent.*`) and independently observed | Units 9–10 |
| Documentation | Reconciled against implementation | Architecture v1.1 documentation pass and final consistency reviews |

Nothing in this baseline is disputed by this review. The question this
document addresses is what to build on top of it, not whether it is
real.

## 3. The Sprint 2 Review Question

**What should Parker become capable of next, without weakening the
foundation Sprint 1 proved?** Sprint 2 should expand capability, not
redesign the core runtime — the same constraint Sprint 1 itself operated
under relative to Phase 2's foundation.

## 4. Candidate Sprint 2 Focus Areas

Each area below is assessed against what is already specified, what is
already implemented, and what Sprint 1 left as a genuine open question —
distinguishing "needs new specification work" from "needs an
implementation of something already specified" wherever the evidence
allows.

### 4.1 Planner Runtime

**Current state.** `DeterministicPlannerHarness` (Unit 5) is a fixed
test fixture in `tests/runtime/`, not a production Planner — deliberately,
per AD-014: the Planner Runtime Specification "has not yet received its
own dedicated review-and-correction pass," unlike the Agent Runtime and
Task Manager Runtime Specifications. It models 5 of the specification's
10 lifecycle states and produces exactly one `PlanCandidate` per run.

**What is already specified.** Plan Decision (the Planner's internal
selection among Plan Candidates) is a named concept
(`PlannerRuntimeSpecification.md` §4), and `WAITING_FOR_INPUT` is already
a fully specified clarification-path state (§5: "paused pending an input
it cannot supply itself (e.g. a clarification only a human or another
system can provide)"), with its own event (`planner.clarification_requested`-class
events per §11) and valid-transition rules. Model independence (AD-010)
already establishes that Plan Candidate generation and scoring must not
assume any specific reasoning approach.

**What is genuinely open.** How Plan Decision actually scores or selects
among multiple Plan Candidates is deliberately unspecified — AD-010's
model-independence stance means this document does not, and should not,
mandate an algorithm. Resource selection (which Resources a Plan
Candidate should target) has no defined mechanism today beyond the
`TaskProposal.resourceReferences` field itself, which only carries
already-known `ResourceId`s forward (Unit 11B) — it does not discover
which Resources are relevant to a Goal in the first place (see 4.7
below).

**Likely output, if pursued.** A Planner Runtime Specification
review-and-correction pass (mirroring the Agent Runtime and Task Manager
Runtime reviews already completed) is the prerequisite this document's
own AD-014 evidence points to — not new Kotlin. A Plan Candidate schema,
a concrete (even if minimal) Plan Decision mechanism, and resource
selection rules would all be outputs of that review, not inputs to it.

### 4.2 Permission Engine

**Current state.** `FakePermissionEngine` always returns a
caller-supplied decision; Sprint 1 Unit 8 verified this fixture directly
but changed nothing about it. Real Permission Engine policy is
`IMPLEMENTATION_GAPS.md` #25, a "deliberate scope boundary" since Phase 2,
not a Sprint 1 regression.

**What is already specified.** `PermissionEngine.md`'s normative
requirements ("Every ExecutionRequest MUST be evaluated before
execution") and `action-mapping.md`'s deterministic action-to-permission
translation are both implemented and unaffected. What is not specified
anywhere is the actual policy content — which actions are always
allowed, which require confirmation, which are denied by default. This
is a genuine specification gap, not an implementation gap: no document
in this repository defines policy rules, only the mechanism that would
enforce them.

**What is genuinely open.** Confirmation policy, revocation rules, and
memory of past permission decisions are not specified anywhere. Identity
status affecting permission evaluation *is* specified
(`IdentityService.md`, "Integration with Permission Engine": `evaluate`
"MUST resolve `request.principalId` via the Identity Service as its
first step, and short-circuit to `DENIED` for a Suspended/Revoked
Principal") but not implemented — this is `IMPLEMENTATION_GAPS.md` #40,
explicitly deferred during Phase 2 and still open.

**Likely output, if pursued.** A permission policy model and
confirmation policy are new specification work (nothing to review
against, since none exists). Closing gap #40 (wiring identity resolution
into `evaluate`) is implementation against an existing, already-approved
specification — a materially different, lower-risk kind of work than
inventing policy from nothing.

### 4.3 Identity and Trust Hardening

**Current state.** Four items are already tracked, numbered, and
explicitly marked "Requires human decision" in `IMPLEMENTATION_GAPS.md`:
#35 (cascading revocation rule undecided), #36 (`PrincipalLifecycleTransitions`
permits only the literal linear chain — no direct `Active → Revoked` or
`Suspended → Active` edge), #37 (`resolve()` does not suppress
Revoked/Archived Principals), and #40 (`PermissionEngine.evaluate` not
wired to resolve identity first). A fifth, #41, was opened during the
Architecture v1.1 pass: `ToolInvocationBinding.invocableFor` and
`ToolRegistry.resolve` restrict callers to the Execution Pipeline by
convention only, not by construction — also explicitly marked "Requires
human decision."

**What Sprint 1 changed about the stakes, not the gaps themselves.** AD-007's
Architecture v1.1 amendment records this precisely: before Unit 11A, an
incorrect `APPROVED` decision had no downstream consequence, because
nothing ever executed. After Unit 11A, the same incorrect decision
causes a real `Tool.execute()` call. None of #35/#36/#37/#40/#41 are new
— all five predate or were only renamed during the v1.1 pass — but
Sprint 1 is the reason their consequences are no longer hypothetical.

**What is genuinely open.** Each of the five gaps above is explicitly
recorded as requiring a human decision, not an invented default. #41
specifically requires deciding whether a caller-identity or visibility
mechanism should be introduced (which would be a first for this
repository — `ToolRegistry.resolve` has the identical, older limitation)
or whether convention-based restriction remains acceptable for the
platform's current trust model.

**Likely output, if pursued.** Human decisions on #35–#37, #40, and #41,
each already scoped by the gap entries themselves; an identity-status
enforcement design for #40's wiring; and an explicit decision on #41,
recorded either as a closed gap (if a mechanism is introduced) or as a
reaffirmed, documented scope boundary (if convention remains acceptable
for now).

### 4.4 Task Manager Expansion

**Current state.** `InMemoryTaskManagerRuntime` (Unit 6) is
accept-only: every `TaskProposal` with a resolvable owner is `Accepted`.
`Deferred`, `Split`, `Merged`, and business-reason `Rejected` are real,
specified dispositions (`TaskManagerRuntimeSpecification.md` §15's
five-outcome table) with no Sprint 1 implementation.

**What is already specified but not implemented — the sharper finding.**
`TaskManagerRuntimeSpecification.md` §6 already specifies that "the Task
Manager Runtime subscribes to (or is otherwise informed of) relevant
Agent Events (`agent.completed`, `agent.failed`, `agent.cancelled`,
`agent.action_denied`, `agent.action_deferred`)" and §11 specifies that
a `FAILED` Agent Run publishes `task.agent_run_completed`. Sprint 1's
Unit 6 and Unit 9 did not wire this: `InMemoryTaskManagerRuntime` has no
`EventBus` subscription to Agent Events at all today. This is a real gap
between specification and implementation that is not yet a numbered
entry in `IMPLEMENTATION_GAPS.md` — it was not in Sprint 1's scope and
was not surfaced by the Architecture v1.1 reconciliation pass, since
that pass reconciled documentation against what Sprint 1 built, not
against everything the Task Manager Runtime Specification already
specifies.

**What is genuinely open.** The specification is explicit that Agent
Events "may inform Task state, but do not automatically control it" —
the actual rules for when partial Agent Run completion should transition
a Task to `Completed` versus leaving it active are Task Manager's "own
configured rules," deliberately left to be evaluated, not specified as
an algorithm.

**Likely output, if pursued.** Implementing the Agent-Event-subscription
behaviour §6 already specifies is implementation work against an
existing, approved specification. Task disposition rules beyond Accept,
and the exact Task-completion evaluation rules, are narrower design
decisions within an already-specified framework, not new architecture.

### 4.5 Agent Runtime Expansion

**Current state.** `InMemoryAgentRuntime` (Unit 7) implements
`AgentRunCommandType.START` only; `SUSPEND`, `RESUME`, and `CANCEL` are
explicitly rejected, not silently ignored.

**The structural reason, not just the scope note.** Unit 7's own
implementation notes are specific about why: "`WAITING_FOR_PERMISSION`,
`WAITING_FOR_INPUT`, and `SUSPENDED` are not driven — the Execution
Pipeline call in this codebase is synchronous, so there is nothing
asynchronous to pause for." `AgentRuntimeSpecification.md` §5 already
fully specifies these states, their transitions, and even names the
`AgentRunCommand` values that would drive them (`SUSPEND`/`RESUME`/`CANCEL`,
already defined in `src/contracts/AgentRunCommand.kt`) — but implementing
them meaningfully requires the Agent Run loop to support pausing
mid-run, which the current synchronous, single-`ExecutionRequest`-per-run
implementation does not need to do, because a run either completes or
fails in one synchronous call.

**What is genuinely open.** How many steps an Agent Run may contain, and
what a multi-step Agent Run loop looks like, is not something Sprint 1
implemented or needed to — `InMemoryAgentRuntime`'s `START` handling maps
one command to one `ExecutionRequest`. A multi-step model is a real
design gap, not merely an unwired command type.

**Likely output, if pursued.** This is the one candidate area where
"wire the remaining command types" understates the work: `SUSPEND`/`RESUME`/`CANCEL`
require a multi-step Agent Run model to have any meaningful semantics at
all, since there is currently nothing to suspend *between*. Agent Run
control semantics and a multi-step model are the same design problem,
not two separate ones.

### 4.6 Workflow Runtime

**Current state.** Workflow Runtime is not specified and not
implemented. `IMPLEMENTATION_ORDER.md`'s dependency map states its role
precisely: it "*composes* Planner and Task Manager output into
structured multi-step processes; it does not sit below them in the
execution path and does not give Agent Runtime or Execution Pipeline a
second entry point." `INTER_SPECIFICATION_CONTRACTS.md` Section 5
categorises it under "Future integration" — explicitly out of scope for
every current specification.

**What this means for sequencing.** Workflow Runtime composes Planner
and Task Manager output. Composing an intelligent Planner that does not
yet exist (4.1) and a Task Manager whose disposition and completion
rules are still accept-only (4.4) would mean designing composition
semantics against two subsystems still in flux underneath it.

**Likely output, if pursued now:** premature. The evidence supports
deferring, or at most defining the smallest possible workflow slice as a
design exercise without committing to sequencing/retry/branching
ownership, until the Planner and Task Manager work above has landed.

### 4.7 Resource Registry

**Current state.** `register`/`resolve`/`update`/`listByOwner` are
implemented and tested (`ResourceRegistry.md`). Sprint 1 (Unit 11B)
proved *propagation* of a caller-supplied `ResourceId` through
`TaskProposal → AgentRunCommand → ExecutionRequest` — it did not touch
discovery.

**The sharper finding: discovery has no supporting operation today, not
just no implementation.** `ResourceRegistry.md`'s Required Operations
are exactly `register`, `resolve(resourceId)`, `update`, and
`listByOwner(owner)`. There is no capability-based or type-based
discovery query anywhere in the interface — a caller must already know
a Resource's ID to resolve it, or must own it to list it. "How does
Parker discover relevant resources" is therefore not an implementation
gap in an existing operation; it is a missing operation, which is
specification work, not a wiring task. This is a larger gap than the
draft's framing ("resource selection model," "resource capability
schema") implies on its own.

**Likely output, if pursued.** A resource discovery/selection model
would need to define a new `ResourceRegistry` operation (or a companion
interface, mirroring how `ToolInvocationBinding` was added alongside
`ToolRegistry` rather than changing it) before any Planner-to-Resource-Registry
interaction could be built. This has the same shape as the
`ToolInvocationBinding` precedent: prefer an additive interface over
changing an already-tested one.

## 5. Cross-Cutting Observation: Three Different Kinds of Gap

The seven areas above do not represent one kind of work. Distinguishing
them changes how risky each is to start:

- **Already specified, not implemented** (lower risk — implementation
  against an existing, approved design): Task Manager's Agent-Event
  subscription (4.4), identity-status wiring into `PermissionEngine.evaluate`
  (#40, 4.3), `AgentRunCommand`'s already-named `SUSPEND`/`RESUME`/`CANCEL`
  (4.5, though real semantics also require new design — see below).
- **Specified in principle, but requiring new design before
  implementation is possible** (medium risk): a multi-step Agent Run
  model (4.5), a concrete Plan Decision mechanism (4.1), permission
  policy content (4.2).
- **Not specified at all — missing an operation, not missing a caller**
  (highest risk to start casually): Resource discovery (4.7), Workflow
  Runtime composition semantics (4.6).

Gaps in the first category can proceed directly to an implementation
unit, following Sprint 1's own units-per-commit discipline. Gaps in the
second and third categories need a specification or specification-review
step first, per AD-013 and AD-014 — exactly the discipline
`docs/reviews/SPRINT_1_RETROSPECTIVE.md` Section 7 recommends continuing.

## 6. Recommended Priority Order (Reviewed Against Evidence)

The draft's proposed order is:

1. Permission and Identity hardening
2. Task Manager completion/event handling
3. Agent Runtime multi-step/control semantics
4. Planner Runtime design
5. Resource Registry/resource discovery
6. Workflow Runtime only if the above is stable

This order holds up against the evidence gathered above, for reasons
beyond the draft's own "bad permission behaviour now has real
consequences" framing:

- Identity/Permission hardening (#35–#37, #40, #41) is the only area
  where every open item is already a numbered, scoped gap requiring a
  human *decision*, not new specification *design*. It is genuinely the
  lowest-design-risk starting point.
- Task Manager's Agent-Event subscription is "already specified, not
  implemented" (Section 5's first category) — the same low-design-risk
  shape as identity hardening, and a natural second step since it
  directly depends on `agent.*` events Sprint 1 already publishes.
- Agent Runtime multi-step/control semantics is correctly placed third,
  not first, precisely because it is the one item in Section 4.5 that
  is not simply "wire an existing command" — it requires new design (a
  multi-step model) before implementation can start, which the draft's
  ordering already reflects by placing it after the two
  lower-design-risk items.
- Planner Runtime design fourth is consistent with AD-014: the
  specification itself has not had its review-and-correction pass yet,
  so starting Planner work before the higher-certainty items above would
  mean building on the least architecturally settled foundation of the
  six.
- Resource Registry discovery fifth is correctly not first, given
  Section 4.7's finding that it requires a new interface operation, not
  a wiring task — and a Resource discovery mechanism is most useful once
  a real Planner (item 4) exists to consume it, since
  `DeterministicPlannerHarness` has no use for discovering resources it
  is never asked to reason about.
- Deferring Workflow Runtime is directly supported by Section 4.6: it
  composes two subsystems (Planner, Task Manager) that are either not
  yet designed or only partially implemented.

This review finds no reason to reorder the draft's sequence.

## 7. Recommended Sprint 2 Theme

Sprint 2 should turn Parker from a single deterministic execution path
into a controlled, permission-aware runtime capable of handling richer
tasks and agent lifecycles — expanding what Sprint 1 proved can execute,
not replacing how it executes.

## 8. Recommended Sprint 2 Architecture Deliverables (Before Coding)

Consistent with AD-013 and AD-014, and with the practice
`docs/reviews/SPRINT_1_RETROSPECTIVE.md` Section 7 recommends continuing
("pre-code architecture analysis before coding begins"):

- This Sprint 2 Architecture Readiness Review (this document).
- A Sprint 2 Implementation Plan, scoped unit-by-unit the way
  `docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` was.
- An `IMPLEMENTATION_GAPS.md` update: at minimum, a new numbered entry
  for the Task Manager Agent-Event-subscription gap identified in
  Section 4.4, which is not yet tracked.
- Any Architecture v1.2 amendments genuinely required — this review did
  not identify a need to supersede or contradict any of the 16 existing
  Architecture Decisions; new decisions, if any, should follow
  `ARCHITECTURE_DECISIONS.md` §7's own evidence-first standard.
- A clearly scoped Unit 1 for Sprint 2, following Section 9 below.

## 9. Recommended First Sprint 2 Implementation Candidate

**Identity and Permission hardening** is the safest first coding unit,
for a reason grounded in Sprint 1's own evidence rather than general
caution: Unit 11A made Tool execution real, which is precisely what
AD-007's Architecture v1.1 amendment flags as raising gap #40's
real-world stakes. Every other candidate area either requires new
specification design first (Planner, Resource discovery, Agent Run
multi-step semantics) or depends on identity/permission being trustworthy
underneath it (Task Manager's Agent-Event handling still passes through
`ExecutionPipeline.submit`, which still depends on `PermissionEngine.evaluate`).
Starting here also matches Section 5's finding that this area's open
items are decisions, not designs — the lowest-risk shape of work to
begin Sprint 2 with.

## 10. Recommended Sprint 2 Opening Principle

Sprint 2 must expand Parker's capability without bypassing the
execution, permission, identity, event, and tool boundaries proven in
Sprint 1. Concretely, this means: no Sprint 2 unit introduces a second
path to Tool execution (AD-003), a second permission-evaluation
authority (AD-007), a second identity store (AD-008), an unaudited
lifecycle transition (AD-009), or a Task/Agent Run abstraction that
bypasses the Task Manager or Agent Runtime's existing ownership (AD-004–AD-006).
This is a direct restatement of already-Accepted decisions, not a new
one.

## 11. Sprint 2 Success Criteria

Sprint 1 was successful because it had one measurable objective: can the
architecture execute one complete vertical slice? Sprint 2 should be
held to an equally measurable standard rather than an open-ended list of
capability improvements — each of the seven candidate areas in Section 4
could otherwise absorb indefinite additional scope with no natural
stopping point.

Sprint 2 will be considered complete when:

- Identity status is enforced before execution (closing
  `IMPLEMENTATION_GAPS.md` #40).
- Permission decisions are policy-driven rather than fixed test
  responses (closing #25).
- Task Manager can react to Agent completion events (per
  `TaskManagerRuntimeSpecification.md` §6; Section 4.4 above).
- Agent Runtime supports controlled multi-step execution (Section 4.5
  above).
- Planner Runtime progresses beyond the deterministic harness.
- No architectural boundary proven during Sprint 1 is bypassed (Section
  10 above).
- All Sprint 2 units are reflected in Architecture v1.2.

These criteria are deliberately binary, mirroring Sprint 1's own single
measurable objective rather than a graded or partial standard. A Sprint
2 Implementation Plan should scope units against this list directly, not
against the broader candidate areas in Section 4 on their own.

## 12. Sprint 2 Non-Goals

Sprint 2 deliberately does not attempt to:

- **Build Workflow Runtime.** Deferred per Section 4.6 and Section 6: it
  composes Planner and Task Manager output, both still in flux.
- **Introduce autonomous planning.** A concrete Plan Decision mechanism
  (Section 4.1) is in scope only to the extent a Planner Runtime
  Specification review-and-correction pass defines one; autonomous,
  self-directed goal generation is not specified anywhere in this
  repository and is not a Sprint 2 target.
- **Introduce memory consolidation.** Memory remains, per AD-012, a
  read-only Context provider with no orchestration authority, and no
  Memory Specification exists yet to consolidate against
  (`IMPLEMENTATION_ORDER.md` §4, Order 2–3 lists it as future work).
- **Implement scheduling.** A background execution queue is recorded as
  carried-forward technical debt in `docs/reviews/SPRINT_1_RETROSPECTIVE.md`
  Section 8, not a Sprint 2 target.
- **Redesign the Execution Pipeline.** AD-003 (Execution Pipeline Is the
  Sole Execution Authority) is restated, not revisited, by Section 10's
  opening principle — Sprint 2 extends what calls into it, not how it
  works.
- **Change the permission architecture.** Sprint 2 defines permission
  *policy* (Section 4.2) — content, not mechanism. `PermissionEngine.evaluate`'s
  existing contract and AD-007's authority boundary are unchanged.
- **Replace any Sprint 1 contracts.** `TaskProposal`, `AgentRunCommand`,
  `ToolInvocationBinding`, and the Sprint 1 lifecycle contracts remain as
  built; Sprint 2 work is additive, following the same precedent
  `ToolInvocationBinding` itself set — a new interface alongside
  `ToolRegistry`, not a change to it.

These exclusions exist to preserve the architectural stability Sprint 1
proved, not because any of them is unimportant. Each is either
explicitly deferred elsewhere in this review (Workflow Runtime),
explicitly out of scope for every current specification (Memory), or
would put a Sprint 1 boundary — execution authority, permission
authority, or contract shape — at risk for capability Sprint 2 does not
need in order to meet Section 11's success criteria.

## 13. Sprint 2 Architecture Risks

| Risk | Mitigation |
|---|---|
| Permission policy becoming intertwined with business logic. | Express policy content (Section 4.2) as data the Permission Engine evaluates, not as conditional logic embedded in Task Manager or Agent Runtime callers, preserving AD-007's "subsystems never self-authorise" boundary. |
| Agent Runtime becoming stateful before its lifecycle model is complete. | Design the multi-step Agent Run model (Section 4.5) against the already-specified `AgentRuntimeSpecification.md` §5 state machine in full, rather than adding incremental state to `InMemoryAgentRuntime` command-by-command. |
| Planner becoming model-specific instead of model-independent. | Require any Plan Decision mechanism defined during a Planner Runtime Specification review to satisfy AD-010 (Model Independence) explicitly, the same way `DeterministicPlannerHarness`'s own KDoc already cites it by ID. |
| Resource discovery becoming coupled to Planner implementation. | Define the Resource discovery operation on `ResourceRegistry` (or a companion interface, Section 4.7) independently of any Planner consumer, mirroring how `ToolInvocationBinding` was specified without reference to `DefaultExecutionPipeline`'s particular implementation. |
| Workflow Runtime being introduced before Planner and Task Manager stabilise. | Hold Workflow Runtime out of Sprint 2 entirely (Sections 6 and 12) until both dependencies have their own completed review-and-correction or implementation pass. |

This register covers the risks specific to the priority order in Section
6; it is not a general project risk register.

## 14. Review Outcome

This review is complete when:

- Every Sprint 2 candidate area (Section 4) has been assessed against
  what is specified, implemented, and open.
- Implementation work has been classified by risk (Section 5).
- Specification work has been identified separately from implementation
  work, per that same classification.
- Priorities have been established and checked against evidence
  (Section 6), not merely asserted.
- Sprint 2 can begin with no unresolved architectural uncertainty
  affecting its initial implementation sequence — Section 9's candidate
  first unit and Section 11's success criteria are both concrete enough
  to scope a Sprint 2 Implementation Plan against directly.

All five conditions are met as of this document. This does not itself
conclude that Sprint 2 should proceed on any particular timeline — only
that, architecturally, there is no remaining ambiguity about where it
should start.

## 15. Final Assessment

Sprint 2 should begin with architecture, not code — the same discipline
Sprint 1 validated in Section 7 of its own retrospective. Sprint 1
answered: can Parker's architecture execute? Sprint 2 should answer: can
Parker's architecture execute safely, repeatedly, and under richer
real-world conditions, without weakening any of the boundaries Sprint 1
proved real? Of the seven candidate areas reviewed here, Identity and
Permission hardening is the only one whose open items are already fully
scoped decisions rather than open designs, which is why it is
recommended as Sprint 2's starting point rather than merely the
draft's preferred one.

## 16. Transition to Sprint 2

This is an architecture readiness review, not a Sprint 2 Implementation
Plan, and no Sprint 2 unit should be scoped directly from it. The
intended document chain, mirroring the separation Sprint 1 itself
maintained between architecture and implementation documents, is:

```
Architecture Review
        ↓
Architecture Decisions
        ↓
Sprint 2 Implementation Plan
        ↓
Sprint 2 Unit 1
```

This review's own output stops at Sections 6–14: a priority order, a
theme, an opening principle, success criteria, non-goals, a risk
register, and the review-completion check in Section 14. Where those
recommendations require a decision recorded
against `ARCHITECTURE_DECISIONS.md` (following its own §7 evidence-first
standard — Section 6 above found no existing decision needs to be
superseded or contradicted to proceed), that decision should be made and
recorded before a Sprint 2 Implementation Plan is written, and the
Implementation Plan should exist and be reviewed before any Sprint 2
Unit 1 code is written — the same architecture-before-implementation
ordering `docs/reviews/SPRINT_1_RETROSPECTIVE.md` Section 7 recommends
continuing.

## Closing Statement

This review deliberately ends at the architectural boundary. Sequencing
units, estimating effort, and scoping individual commits belong to the
Sprint 2 Implementation Plan, not to this document. Discoveries made
during Sprint 2 implementation should be reconciled back into the
architecture using the same Architecture → Implementation →
Reconciliation → Retrospective process Sprint 1 established, not by
reopening this review; once Sprint 2 begins, this review should not be
revisited to accommodate implementation decisions.

## Related

- `docs/reviews/SPRINT_1_RETROSPECTIVE.md`
- `docs/implementation/IMPLEMENTATION_HISTORY.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md`
- `docs/architecture/ARCHITECTURE_DECISIONS.md`
- `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md`
- `docs/architecture/IMPLEMENTATION_ORDER.md`
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
- `docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`
- `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
- `docs/architecture/IdentityService.md`
