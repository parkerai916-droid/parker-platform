# Planner Runtime Progression — Design and Specification Review

## Status

Sprint: Sprint 3, Track D, Unit D1 (Design)
Version: 0.1-draft
Status: **Design/review proposal, not yet reviewed or accepted.**

**This document is specification-level review and design context only.**
No Kotlin is implemented, proposed as a diff, or changed by it. Neither
`src/` nor `tests/` is touched. No pre-existing test is modified. This
document adds no entry to `IMPLEMENTATION_HISTORY.md` or
`IMPLEMENTATION_GAPS.md` — per this unit's own instructions, those remain
untouched until an implementation unit (Unit D2) actually changes
`src/`/`tests/`. Nothing described here is authorised for implementation
until this document is reviewed and accepted, per PES-001
(`docs/architecture/PARKER_ENGINEERING_STANDARD.md`) Chapter 1's ordering
of Architecture before Specification before any coding stage, and per
AD-014 (Architecture Before Implementation).

### A scope note, stated up front rather than silently resolved

Two documents define what "Unit D1" is, and they do not ask for quite the
same deliverable:

- `docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md` (this repository's
  actual, frozen, authoritative Sprint 3 Implementation Plan, despite its
  own filename) defines Unit D1 as **"Planner Runtime Specification
  Review-and-Correction Pass"** — documentation only, its own "Files
  expected to change" naming exactly one file:
  `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
  itself, corrected in place. Its own scope line is explicit: this pass
  "does not itself define a Plan Decision mechanism, a Plan Candidate
  schema, or resource selection rules — it identifies what such a
  follow-on design (Unit D2) would need to define."
- This unit's own instructions ask for **exactly one standalone design
  document**, in a 12-section form that mirrors Unit C1's own template
  (`MULTI_STEP_AGENT_RUN_DESIGN.md`) — a template built for a case where
  new architecture (`AgentStepSource`, `AgentStepContext`,
  `AgentStepDecision`, `AgentPolicy`) was being proposed for the first
  time. Track D's Unit D1 is not that kind of unit: little to nothing
  named below is new architecture.

Resolution adopted here, consistent with this engagement's existing
precedent of surfacing this kind of tension rather than silently picking
a side (e.g. the `PROJECT_GOVERNANCE.md` "investigate, don't populate"
decision; Unit C1's own "add a forward-compatibility note, don't add the
behaviour" decision): this document **is** the one requested standalone
document, and it performs genuine Unit D1 review work inside it — an
internal-consistency check of `PlannerRuntimeSpecification.md`, a check of
that specification's consistency with what Sprint 1/2 actually built
(`DeterministicPlannerHarness.kt`, `InMemoryTaskManagerRuntime.kt`), and
explicit flagging of every prose-anticipated-capability gap found (AD-013's
Implementation note pattern). Where the Sprint Plan's own Unit D1 scope
line reserves something for Unit D2 (the Plan Decision mechanism, the Plan
Candidate schema, resource selection rules), this document does not invent
it — it names the gap precisely and defers the decision, per Section 11
below. **No text inside `PlannerRuntimeSpecification.md` itself is changed
by this document.** One concrete correction is identified (Section 6,
below) with proposed replacement text; applying it to the specification
file is a follow-up action for whoever accepts this review, not something
this document performs itself, consistent with "produce exactly one
document" and "do not commit."

## Review

Reviewed, in the authority order this unit's own instructions specify:

1. `docs/architecture/parker-constitution.md` (Parker Constitution)
2. `docs/architecture/ARCHITECTURE_DECISIONS.md` (Architecture Decisions,
   AD-001–AD-016 — especially AD-002, AD-005, AD-007, AD-010, AD-012,
   AD-013, AD-014, AD-015, AD-016)
3. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001)
4. `docs/architecture/PROJECT_GOVERNANCE.md` (Project Governance) — this
   file remains empty (reconfirmed this unit; unchanged since the Sprint 2
   Health Review first flagged it). No governance rule beyond PES-001 and
   the Constitution could be consulted from it.
5. `docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md`, Track D, Unit D1
   and Unit D2 (the objective, scope, and acceptance criteria this
   document must satisfy and hand off to)
6. Existing Runtime Architecture: `src/contracts/TaskProposal.kt`
   (`PlanningSessionId`, `TaskProposalId`, `TaskId`, `ProposalDependency`,
   `TaskProposal`, `TaskProposalDisposition`, `TaskProposalIntake`),
   `src/runtime/InMemoryTaskManagerRuntime.kt` (the first, and so far only,
   `TaskProposalIntake` implementation),
   `docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md` and
   `src/runtime/InMemoryAgentRuntime.kt` (Track C's just-completed
   multi-step Agent Runtime, reviewed for precedent, not modified)
7. Existing Specifications:
   `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
   (in full), `tests/runtime/DeterministicPlannerHarness.kt` (in full, the
   test-only fixture whose actual behaviour this review checks the
   specification against)

No architecture is invented where an existing document already answers
the question; every such case is cited by section number below instead of
being restated as a new decision.

---

## 1. Purpose

`PlannerRuntimeSpecification.md` is, by its own governing document's
account, the only Volume 4–6 runtime specification that has never
received a dedicated review-and-correction pass — AD-014's own Future
Considerations note states the Planner Runtime Specification "has not yet
received its own dedicated review-and-correction pass, unlike the Agent
Runtime and Task Manager Runtime Specifications." The Sprint Plan's own
Readiness Review table treats this as a real design gap, not bookkeeping:
"Unit D2 cannot be scoped until the review exists."

Track D exists to close that gap in two units. Unit D1 (this document)
performs the review itself: confirming the specification is internally
consistent, confirming it accurately describes what has actually been
built (`DeterministicPlannerHarness.kt`, and — since this review was
performed — `InMemoryTaskManagerRuntime.kt`'s `TaskProposalIntake`
implementation), and drawing a precise line between what is already
architecturally settled and what remains a genuine open design decision.
Unit D2 (out of scope here) will then implement a concrete Plan Decision
mechanism against that settled ground, without having to make design
decisions inside Kotlin that this document should have made instead.

## 2. Architectural Context

The Constitution's authority chain — "Parker owns authority... Cognition
proposes. Trust authorises. Runtime executes." — places the Planner
Runtime squarely in the "Cognition proposes" role. AD-002 (Proposal Before
Authority) names four distinct roles — propose, orchestrate, authorise,
execute — and the Planner Runtime occupies exactly one of them: propose.
AD-005 (Planner Never Creates Tasks) is the sharpest single constraint on
this track: a Task Proposal is a recommendation, and only the Task Manager
Runtime's acceptance of one ever produces a canonical Task Manager Task.

Track D sits downstream, chronologically, of Track C (Agent Runtime
Multi-Step Control, just completed) and upstream of Track E (Resource
Discovery). Architecturally, though, the Planner Runtime is *upstream* of
both the Task Manager Runtime and the Agent Runtime: a Planning Session
produces Task Proposals; the Task Manager Runtime decides whether they
become Tasks; only after that may an Agent Run — potentially using Track
C's own new multi-step machinery — ever be created. Track D does not
touch, and must not touch, the Agent Runtime, Execution Pipeline,
Permission Engine, Tool Registry, Tool Invocation Binding, Resource
Registry, or Event Bus implementations themselves; it only reviews how the
Planner Runtime Specification describes its (still entirely prose-level)
relationship to them.

## 3. Responsibilities

**Track D (D1 + D2, together) owns:** the correctness and internal
consistency of `PlannerRuntimeSpecification.md` (D1), and — pending D1's
acceptance — a concrete Plan Decision mechanism and Plan Candidate schema
sufficient to let a Planning Session select among more than one Plan
Candidate (D2).

**Unit D1 (this document) owns only:** the specification-review content
below. It owns no Kotlin, no `src/contracts` additions, no runtime
behaviour, and no test changes.

**Track D explicitly does not own, and must not redesign:**

- Identity-aware execution (Identity Service) — Section 8's dependence on
  `IdentityService.resolve` is reviewed, not altered.
- Permission Engine — Section 8's advisory-labelling-only relationship is
  reviewed, not altered; the Permission Engine remains the sole authority
  for any `PermissionDecision`.
- Execution Pipeline — no Planner Runtime operation invokes
  `ExecutionPipeline.submit` today, and nothing here proposes one should.
- Tool Registry / Tool Invocation Binding — Section 3's "no direct tool
  execution" Non-Goal is unchanged.
- Resource Registry — Track D reviews how Planning Context (Section 9)
  *references* Resources, not how the Resource Registry itself resolves
  them (Track E's territory).
- Task Manager Runtime — the disposition mechanism
  (`TaskProposalIntake.submitProposal`) is reviewed for consistency, not
  redefined; its five-outcome model (`Accepted`, `Deferred`, `Rejected`,
  `Split`, `Merged`) is unchanged.
- Multi-step Agent Runtime, Suspend/Resume/Cancel, Event-driven runtime
  coordination — all Track C output, cited for precedent only (Section 8,
  below) and otherwise untouched.

## 4. Public Interfaces

Per the scope note above, this section **cites, and does not invent**.
The only Kotlin contracts already committed that shape Planner-adjacent
concepts are in `src/contracts/TaskProposal.kt`:

- `PlanningSessionId`, `TaskProposalId`, `TaskId` — value-class
  identifiers.
- `ProposalDependency` (`OnExistingTask` / `OnProposal`) — the two
  Dependency cases `PlannerRuntimeSpecification.md` Section 4/Section 10
  distinguishes.
- `TaskProposal` — the full Proposal Model (Section 10), field-by-field
  reviewed for consistency in Section 6 below.
- `TaskProposalDisposition` (`Accepted`, `Deferred`, `Rejected`, `Split`,
  `Merged`) — the five outcomes Section 6 and
  `TaskManagerRuntimeSpecification.md` Section 15 both name.
- `TaskProposalIntake` — the single-method interface
  (`suspend fun submitProposal(proposal: TaskProposal): TaskProposalDisposition`)
  the Task Manager Runtime implements and the Planner Runtime depends on
  but does not implement.

**What does not exist, and what this document does not create:** no
`PlanCandidate` type exists in `src/contracts` (only a test-only,
single-field `PlanCandidate` inside `DeterministicPlannerHarness.kt`); no
`PlanDecision` interface exists anywhere (analogous to `AgentStepSource`
for Track C); no `PlannerPolicy` type exists in `src/contracts` (only
named in prose, Section 4). The Sprint Plan's own Unit D1 scope line
excludes defining any of these ("does not itself define a Plan Decision
mechanism, a Plan Candidate schema, or resource selection rules"), and
this document honours that exclusion. Section 11 names precisely what
Unit D2 will need to define here.

## 5. State Model

`PlannerRuntimeSpecification.md` Section 5 already defines the complete
Planning Session lifecycle — ten states (`CREATED`, `CONTEXT_GATHERING`,
`ANALYSING`, `PROPOSING`, `WAITING_FOR_INPUT`, `SUBMITTED`, `REJECTED`,
`CANCELLED`, `FAILED`, `COMPLETED`) and every valid transition between
them, including the rule that `WAITING_FOR_INPUT` always resumes into one
of `CONTEXT_GATHERING`/`ANALYSING`/`PROPOSING` and never directly into
`SUBMITTED`. This review does not redesign it; it checks it.

**Consistency check performed:** `DeterministicPlannerHarness.kt`'s own
`PlanningSessionLifecycleState` enum names exactly 5 of the 10 specified
states (`CREATED, CONTEXT_GATHERING, ANALYSING, PROPOSING, SUBMITTED`) and
`PlanningSessionLifecycleTransitions` models exactly the 4 edges that
connect them in sequence. The harness's own class-level KDoc already
states this is "a documented subset, not a claim that those 5 states don't
exist," and the modelled path —
`CREATED → CONTEXT_GATHERING → ANALYSING → PROPOSING → SUBMITTED` — is
precisely the path Section 5 itself calls "the only path into a submitted
Task Proposal." **Finding: fully consistent. No correction to Section 5
is needed.** The remaining 5 states
(`WAITING_FOR_INPUT, REJECTED, CANCELLED, FAILED, COMPLETED`) are real,
specified, and simply never exercised by any code in this repository yet
— a coverage gap, not a specification defect (see Section 7, Failure
Behaviour, below, for the same finding applied to failure paths
specifically).

## 6. Runtime Behaviour

Sections 6, 7, and 9 of the specification already describe the intended
execution flow and its interactions with the Agent Runtime, Task Manager
Runtime, and Planning Context. This review checks each interaction named
in this unit's own required content list.

- **Task Manager Runtime.** Section 6's sequence diagram and prose match
  what actually exists: `InMemoryTaskManagerRuntime` (`src/runtime/`,
  Sprint 1 Unit 6) implements `TaskProposalIntake.submitProposal`,
  resolves `proposedOwnerPrincipalId` via `IdentityService`, and returns
  either `Accepted` (owner resolves) or `Rejected` (owner does not
  resolve) — consistent with Section 6's disposition shape. **One
  correction identified:** Section 6's own closing paragraph currently
  reads, in part: *"No implementation of `TaskProposalIntake` exists yet
  ... That remains Sprint 1 coding work
  (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` Unit 6)."* This
  is now stale — `InMemoryTaskManagerRuntime.kt`'s own class KDoc header
  identifies itself as exactly that Sprint 1 Unit 6 work, already
  committed. Proposed replacement text, for whoever accepts this review to
  apply:

  > **Dependency on the Task Manager Runtime Specification — closed, and
  > implemented.** `src/contracts/TaskProposal.kt` names
  > `TaskProposalIntake.submitProposal`, and
  > `TaskManagerRuntimeSpecification.md` Section 15 states what each of
  > the five `TaskProposalDisposition` outcomes means.
  > `src/runtime/InMemoryTaskManagerRuntime.kt` (Sprint 1, Unit 6) is the
  > first implementation of `TaskProposalIntake`: accept-only, for a
  > resolvable proposed owner — every other disposition
  > (`Deferred`/`Split`/`Merged`, and any business-reason `Rejected`)
  > remains specified but unimplemented, since this runtime has no Plan
  > Decision or acceptance-policy logic to weigh against yet.

  This is a textual correction to stale prose, not a change to the
  architecture the specification describes — the contract shape, the
  five-outcome model, and the "disposition is a direct return value"
  design are all unaffected and remain correct.
- **Agent Runtime.** Section 7's five bullets (no Agent Run creation, only
  a capability *hint*, Agent Runs only after Task Manager orchestration,
  no tool invocation via agents, Agent events inform but never control)
  remain fully consistent with Track C's just-completed multi-step
  rewrite. Track C changed how an already-created Agent Run executes
  multiple steps; it did not change how — or whether — an Agent Run comes
  to exist in the first place, which is still exclusively a Task Manager
  Runtime decision Section 7 already describes correctly. **No
  correction needed.**
- **Permission Engine.** Section 8's "advisory labelling, not a
  `PermissionDecision`" framing for `anticipatedPermissionActions`
  matches `TaskProposal.kt`'s own field documentation verbatim (citing
  AD-007, AD-015). **No correction needed.**
  Consistent with AD-013's Implementation note pattern: this comment must
  be checked, not asserted from prose, since Sections 8/9 anticipate a
  "Required capabilities" field and a "Permission requirements" field —
  both already have a shaped, non-optional field in `TaskProposal.kt`
  (`requiredCapabilities`, `anticipatedPermissionActions`), so no
  prose-anticipated-capability gap exists here, unlike the one found
  below for Plan Decision.
- **Event Bus.** Section 11 specifies 13 `planner.*` events.
  `DeterministicPlannerHarness.kt`'s own KDoc states it publishes exactly
  5 of them (`session_created`, `context_requested`, `analysis_started`,
  `proposal_created`, `proposal_submitted`) and explains, event by event,
  why each of the other 8 is not reached by its fixed happy path. Every
  one of the 5 published event-type strings matches Section 11's table
  exactly (no misnamed or reordered event found). **Finding: fully
  consistent. No correction needed** — the 8-event gap is coverage, not a
  defect, and is exactly the kind of thing Unit D2 will need to close as
  it implements branches this harness never modelled.
- **Memory / World Model.** Section 14 already marks both as future,
  unspecified, read-only-when-they-exist seams; Section 3's Non-Goals and
  Section 13's Safety Boundaries both restate "no direct Memory
  mutation"/"no direct World Model mutation." No code or specification
  text anywhere in this repository claims otherwise. **No correction
  needed.**

**The one genuine gap this review surfaces (flagged, not corrected, per
this unit's scope):** Section 4 states a Planning Session "MAY generate
one or many Plan Candidates before selecting among them (Plan Decision,
below)." `DeterministicPlannerHarness.kt` generates exactly one, always,
by construction — its own KDoc states plainly that "Plan Decision ... is
out of scope [for this harness] — there is nothing to decide among when
only one candidate is ever produced." **No code in this repository, test
or production, has ever exercised an actual Plan Decision comparing two or
more Plan Candidates.** This is not a specification defect — Section 2
(Design Goals) deliberately does not mandate a particular Plan Decision
algorithm, consistent with AD-010 (Model Independence) — but it is the
single most important prose-anticipated-capability gap this review found,
and it is precisely what Unit D2 exists to close (Section 11, below).

## 7. Failure Behaviour

Section 12 already defines every category this unit's required content
list asks for:

- **Recoverable failures:** insufficient context and permission
  uncertainty both route to `WAITING_FOR_INPUT` or a recorded Assumption,
  not directly to `FAILED`.
- **Terminal failures:** conflicting constraints, malformed goal,
  impossible proposal (every Plan Candidate rejected), and timeout all
  route to `FAILED` — explicitly distinguished from `REJECTED`, which
  Section 12 reserves for the Task Manager Runtime's own external decision
  ("Task Manager rejection... an external decision, not a Planner Runtime
  failure").
- **Retry policy:** not separately named; Section 12's "Timeout" bullet
  states a Planning Session exceeding a Planner-Policy-defined maximum
  duration SHOULD transition to `FAILED` rather than retry indefinitely —
  consistent with both existing baselines' identical treatment.
- **Cancellation behaviour:** Section 12's "Cancellation" bullet and
  Section 5's own diagram agree: available from any pre-`SUBMITTED` state,
  terminal, and stops further Plan Candidate generation immediately —
  mirroring (not copying) the Agent Runtime's own immediate-cancellation
  semantics from Track C.
- **Suspend behaviour:** `WAITING_FOR_INPUT` is explicitly named the
  "sole general-purpose recoverable pause state" (Section 12, "Safe
  suspension"), mirroring the Agent Runtime's `SUSPENDED` and the Task
  Manager Runtime's `Paused` — the same "exactly one wait state" pattern
  Section 5 itself cites.

**Consistency check performed:** none of Section 12's failure categories
are exercised by `DeterministicPlannerHarness.kt` at all — it has no
failure branch, being "a fixed, deterministic, always-succeeds harness"
by its own design. This is the same kind of documented, deliberate
coverage gap already found in Section 5 above, not a new defect. **No
correction to Section 12 is needed**; it is simply, like the lifecycle
states themselves, specified but not yet exercised by any code.

## 8. Concurrency Model

`PlannerRuntimeSpecification.md` has no dedicated Concurrency section
today (unlike, for example, the Agent Runtime Specification's own explicit
locking discussion). This review does not invent one — inventing a
concurrency model here would itself be new architecture, which is outside
Unit D1's scope and would pre-empt a decision the Sprint Plan explicitly
reserves for Unit D2 as "implementation depends on Unit D1." What this
review confirms instead:

- `DeterministicPlannerHarness` is explicitly single-use per instance
  (`run()` may be called only once, enforced by a `check()`), so no
  concurrent-access question exists in the code today.
- `InMemoryTaskManagerRuntime` and `InMemoryAgentRuntime` both already
  establish a working precedent for this kind of in-memory runtime: a
  single `Mutex`-guarded store, with any suspend call to another component
  (e.g. `IdentityService.resolve`, `ExecutionPipeline.submit`) made
  without holding that mutex across the call. **Recommendation, not a
  requirement this document imposes:** a future Planner Runtime
  implementation (Unit D2 or later) should be expected to follow the same
  precedent rather than invent a new concurrency discipline, but the
  actual ownership, locking, and event-ordering decisions for a concrete
  Planner Runtime implementation remain Unit D2's to make, against
  whatever shape its own Plan Decision mechanism takes.

## 9. Security

Section 8 already states the security-relevant rules this unit's content
list asks for, and this review confirms Track D does not alter any of
them:

- **Constitutional authority is preserved.** A Planning Session's
  initiating Principal must resolve through the Identity Service (Section
  8); an unresolvable Principal is invalid, mirroring both existing
  baselines. No Planner-Runtime-local identity store exists or is
  proposed.
- **Trust boundaries are unchanged.** A Constraint, a Planner Policy, or a
  Plan Decision (whatever form Unit D2 gives it) can only narrow what a
  Planning Session may propose — none of them can expand authority. The
  only source of authority for any eventual effect remains a
  `PermissionDecision` the Permission Engine returns for a real
  `ExecutionRequest`, exactly as for any other origin (Section 8,
  restated by Section 13's "No silent privilege expansion").
- **No component gains authority it does not already possess.** Confirmed
  directly: `TaskProposalDisposition`'s five outcomes are all
  orchestration decisions (AD-002's "orchestrate" role, held by the Task
  Manager Runtime), never a substitute for a later, separate Permission
  Engine evaluation (`TaskProposal.kt`'s own closing KDoc paragraph states
  this explicitly, citing AD-002 and AD-007). Nothing in this review
  changes that boundary, and nothing proposed for Unit D2 (a Plan Decision
  mechanism that only ever selects among *proposals*, never grants
  execution) could cross it without itself being a Level 3 Architectural
  Change under PES-001 Chapter 4 — which is not what Unit D2 is scoped as.

## 10. Relationship to Existing Components

This unit's required list — Runtime, Trust, Resources, Events, Planner
(future), Memory (future) — is inherited from Unit C1's own template,
where "Planner (future)" correctly named a downstream system the Agent
Runtime design needed to leave a seam for. Track D *is* that Planner work,
so item-for-item mapping is adjusted accordingly rather than forced to fit
a list written for a different unit:

- **Runtime (Agent Runtime / Task Manager Runtime).** No code relationship
  exists yet. Section 6/7 (reviewed above) already specify the intended
  seam: Task Proposal → Task Manager disposition → (optionally) Task
  Manager-created Agent Run → (Track C's own multi-step machinery, already
  built and unaffected by this review).
- **Trust (Identity Service, Permission Engine).** Advisory-only, resolved
  through existing services, reviewed in Section 8/9 above. No direct
  call path from the Planner Runtime to `PermissionEngine.evaluate`
  exists or is proposed.
- **Resources (Resource Registry).** Section 9's Context Reference and
  `TaskProposal.resourceReferences` are opaque/concrete `ResourceId`
  references respectively, resolved elsewhere, never resolved by the
  Planner Runtime itself (Section 3's "no direct tool execution" Non-Goal
  extends to "no direct resource resolution" by the same logic). Track E
  (Resource Discovery) is a separate, independent track; nothing here
  depends on or blocks it.
- **Events (Event Bus).** Reviewed in full in Section 6 above.
- **Memory (future) / World Model (future).** Section 14 already commits
  both to context-provider-only status, mirroring AD-012 (Memory/World
  Model Are Context Providers) exactly — neither gains orchestration
  authority under any future integration this document anticipates.

## 11. Unit D2 Scope

Per the Sprint Plan's own instruction ("Scope. To be finalized after Unit
D1"), this section is this review's actual deliverable to Unit D2 — the
precise, implementation-ready boundary the Sprint Plan asked Unit D1 to
produce.

**Unit D2 WILL implement:**

- A concrete Plan Decision mechanism capable of generating, and choosing
  among, **two or more** Plan Candidates for a single Goal — the one
  capability named in Section 4 that has never been exercised by any code
  in this repository (Section 6, above).
- A `PlanCandidate` type promoted from `DeterministicPlannerHarness.kt`'s
  test-only, single-field shape into a real `src/contracts` schema,
  informed by whatever Section 4/Section 10 concepts a real Plan Decision
  needs to compare across candidates (e.g. Risk, rationale-for-rejection /
  Plan Rejection).
- Enough of a real Planner Runtime execution path to actually call the
  now-implemented `TaskProposalIntake.submitProposal`
  (`InMemoryTaskManagerRuntime`) with a proposal chosen by that mechanism,
  rather than a fixed, always-selected single candidate.
- A decision on how `PlannerPolicy` (named in Section 4 prose only) is
  represented as a Kotlin type, if Unit D2's own scoping determines one is
  needed for its minimal implementation — mirroring `AgentPolicy`'s
  precedent from Track C, but not assumed here as a requirement.

**Unit D2 will NOT implement:**

- Any change to `PlannerRuntimeSpecification.md` Section 5's state
  machine or Section 11's event model — both are reviewed here as
  internally consistent and require no architectural change, only (per
  Section 6 above) one stale-prose correction.
- Any change to `TaskProposalDisposition`'s five-outcome model or to
  `TaskManagerRuntimeSpecification.md`.
- `Deferred`/`Split`/`Merged` disposition handling inside
  `InMemoryTaskManagerRuntime` — that runtime's own "accept-only" scope is
  a pre-existing, separately-tracked limitation (its own KDoc already
  documents this), not something Unit D2 is scoped to close.
- Resource selection rules (explicitly excluded by the Sprint Plan's own
  Unit D1 scope line; if needed, this is a candidate for its own future
  design unit, not silently folded into D2).
- Any Memory, World Model, or Workflow Runtime integration (Section 14's
  future seams, untouched).
- Any change to the Agent Runtime, Execution Pipeline, Permission Engine,
  Tool Registry, or Resource Registry.
- A general-purpose concurrency model for multiple simultaneous Planning
  Sessions, beyond following the existing `InMemoryTaskManagerRuntime` /
  `InMemoryAgentRuntime` mutex-and-two-phase precedent (Section 8, above)
  — a fuller concurrency treatment remains open for whenever it is
  actually needed.
- Promotion of `DeterministicPlannerHarness` itself into production code —
  the Sprint Plan's own review checkpoint for Unit D2 requires confirming
  a *new*, separate Plan Decision implementation is built, not a
  relocation of the existing test-only fixture.

## 12. Acceptance Criteria

This review is complete, and Track D ready to proceed to Unit D2, when:

- `PlannerRuntimeSpecification.md`'s state model (Section 5), event model
  (Section 11), Core Concepts (Section 4), and Failure/Recovery (Section
  12) are confirmed internally consistent with each other — **confirmed,
  this review** (Sections 5–7, above).
- The specification is confirmed consistent with what has actually been
  built (`DeterministicPlannerHarness.kt`,
  `InMemoryTaskManagerRuntime.kt`) — **confirmed, with one stale-prose
  correction identified** (Section 6, above: the "no implementation of
  `TaskProposalIntake` exists yet" claim).
- Every prose-anticipated-capability pattern is flagged explicitly rather
  than silently left ambiguous, per the Sprint Plan's own Unit D1
  acceptance criterion — **done**: the Plan Decision / multi-candidate gap
  (Section 6), the concurrency-model silence (Section 8), and the
  unexercised failure/lifecycle states (Sections 5, 7) are each named
  individually rather than glossed over.
- Unit D2's scope is separated into WILL/will-NOT implement, in
  implementation-ready form — **done** (Section 11).
- No Kotlin, test, `IMPLEMENTATION_HISTORY.md`, or `IMPLEMENTATION_GAPS.md`
  changes were made by this document — **confirmed**: this review touches
  none of them.
- The full existing test suite is unaffected — **confirmed by
  construction**: nothing under `src/` or `tests/` was read for the
  purpose of modification, and nothing in either directory is changed by
  this document.

---

## Conclusion

**Track D is ready for implementation.**

`PlannerRuntimeSpecification.md` is, on review, an unusually mature
specification — it already self-corrects at least one prior gap (the
`resourceReferences` field addition, Sprint 1 Unit 11B), already tracks
its own open questions explicitly (7 recorded, 1 resolved), and is
internally consistent across its lifecycle, event, and concept models.
This review found exactly one factual correction needed (Section 6's
stale `TaskProposalIntake`-has-no-implementation claim, with proposed
replacement text supplied above) and one substantive, but entirely
expected and already-anticipated, gap: no code anywhere has yet exercised
a real Plan Decision over more than one Plan Candidate. That gap is not
architectural uncertainty blocking implementation — it is precisely the
implementation-level design space the specification deliberately, and
correctly, leaves open for Unit D2 to fill (AD-010, Model Independence),
now bounded by Section 11's explicit WILL/will-NOT scope. Unit D2 should
be able to proceed without making architecture decisions inside Kotlin.
