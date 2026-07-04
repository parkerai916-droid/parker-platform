# Parker Sprint 1 Retrospective

## Status

Sprint:
1

Architecture:
v1.1

Repository Tags:
sprint1-complete
architecture-v1.1

Android Studio:
234 / 234 tests passing

Completion Date:
2026-07-04

---

## 1. Sprint Objective

Sprint 1's stated goal was to produce Parker's first executable vertical
slice: a real, running path from a fixed Goal through to an actual Tool
execution, exercised end-to-end by tests rather than argued for on paper.
Before Sprint 1, the repository consisted of reviewed, corrected
specifications (Agent Runtime, Task Manager Runtime, Planner Runtime),
Volume 1/3 contracts, and a Phase 2 runtime foundation (Tool Registry,
Identity Service, Permission Engine fixture, Execution Pipeline) — all
individually specified and unit-tested, but never connected into one
running chain. `IMPLEMENTATION_GAPS.md` #32 stated the consequence
plainly at the time: a `SUCCESS` `ExecutionResult` meant "every
orchestration stage up to and including finding the right Tool
succeeded," not that a Tool actually ran, because no concrete `Tool`
implementation existed to invoke.

Sprint 1's scope, per `docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md`,
was deliberately narrow: one fixed Goal, one deterministic Planner
fixture, an accept-only Task Manager, a `START`-only Agent Runtime, and
one Mock Tool — enough to prove the full chain is real, not enough to
constitute a general-purpose runtime. Closing named, pre-existing
contract gaps (Task Proposal intake, Agent Run Command, Tool invocation
binding) was treated as a prerequisite to coding, not something to
invent unilaterally during implementation.

---

## 2. What Was Delivered

- **An executable vertical slice.** A fixed Goal now flows through
  `Planner → TaskProposal → Task Manager → Task → AgentRunCommand →
  Agent Runtime → ExecutionRequest → Permission Engine → Action Mapping
  → Tool Registry → ToolInvocationBinding → Tool.validate() →
  Tool.execute() → ExecutionResult → Lifecycle Events → EventCollector`,
  exercised by `tests/runtime/VerticalSliceEndToEndTest.kt` using real
  objects at every hop, not reconstructed or asserted by inference.
- **Planner Runtime.** `DeterministicPlannerHarness` (Unit 5) connects a
  Goal to a `TaskProposal`, modelling 5 of the specification's 10
  lifecycle states. It is a test fixture living in `tests/runtime/`, not
  a production Planner implementation — the Planner Runtime Specification
  itself has not been promoted to an implementation phase.
- **Task Manager Runtime.** `InMemoryTaskManagerRuntime` (Unit 6)
  implements accept-only Task Proposal intake and constructs
  `AgentRunCommand`.
- **Agent Runtime.** `InMemoryAgentRuntime` (Unit 7) consumes
  `AgentRunCommand` (`START` only) and submits an `ExecutionRequest`
  through the unmodified Execution Pipeline.
- **Execution Pipeline / Permission Engine integration.** Verified, not
  newly built: Unit 8 added a standalone test of the existing
  `FakePermissionEngine` fixture, confirming it already satisfies
  Sprint 1's fixed, always-`APPROVED` path without modification.
- **Tool Registry / ToolInvocationBinding.** Unit 11A wired
  `DefaultExecutionPipeline` to the previously-unimplemented
  `ToolInvocationBinding` contract, so a resolved `ToolDescriptor` is now
  bound to a real `Tool`, validated, and executed — closing
  `IMPLEMENTATION_GAPS.md` #32.
- **Lifecycle events / EventCollector.** Unit 9 added real
  `planner.*`/`task.*`/`agent.*` event publication to the three runtime
  components; Unit 10 added `EventCollector`, a test-only subscriber that
  records and later queries the resulting event stream by correlation
  ID.
- **Architecture v1.1 reconciliation.** A separate, subsequent
  documentation-only pass reconciled `ARCHITECTURE_DECISIONS.md`,
  `IMPLEMENTATION_GAPS.md`, the Planner and Task Manager Runtime
  Specifications, `IMPLEMENTATION_ORDER.md`, `INTER_SPECIFICATION_CONTRACTS.md`,
  and `docs/architecture/tool-registry.md` against what Sprint 1 actually
  built, and added a backfill specification,
  `docs/specifications/volume-03-core-interfaces/ToolInvocationBinding.md`,
  for the interface Unit 11A wired.

This is a narrow, honest slice, not a general-purpose runtime. Task
Manager disposition is accept-only; Agent Run commands are `START`-only;
Permission Engine policy is a fixed-decision fixture; the Planner remains
a test harness. None of that is disguised in this document or in
`IMPLEMENTATION_HISTORY.md`'s own "Current Known Architecture Gaps"
section.

---

## 3. Architecture Decisions Validated

- **AD-002 — Proposal Before Authority.** Evidence: the four roles
  (propose / orchestrate / authorise / execute) map onto real, distinct
  Sprint 1 components — `DeterministicPlannerHarness` proposes,
  `InMemoryTaskManagerRuntime` orchestrates, `FakePermissionEngine`
  authorises, `DefaultExecutionPipeline` executes — with no unit
  collapsing two roles into one. Outcome: the decision held under real
  code, not just under specification review.
- **AD-003 — Execution Pipeline Is the Sole Execution Authority.**
  Evidence: `DefaultExecutionPipeline.executeResolvedTool` is the only
  call site anywhere in the repository that invokes
  `ToolInvocationBinding.invocableFor`, `Tool.validate`, or
  `Tool.execute`. Outcome: the rule now governs a real invocation, not
  merely a resolution, since before Unit 11A nothing downstream of
  resolution ever ran.
- **AD-004 — Task Manager Owns Canonical Tasks.** Evidence:
  `src/contracts/Task.kt` is the only Task-shaped type Sprint 1 produces;
  `TaskProposal` and `AgentRun` remain distinct, never conflated with
  `Task`. Outcome: no competing Task abstraction was introduced under
  implementation pressure.
- **AD-005 — Planner Never Creates Tasks.** Evidence:
  `DeterministicPlannerHarness` has no operation that writes a `TaskId`;
  only `InMemoryTaskManagerRuntime.submitProposal` creates one. Outcome:
  structurally, not just behaviourally, impossible for the Planner
  fixture to create a Task.
- **AD-006 — Agent Runtime Never Owns Tasks.** Evidence:
  `InMemoryAgentRuntime` reads `command.taskId` as a reference only and
  has no dependency capable of writing Task state. Outcome: enforced by
  the constructor signature, not by convention.
- **AD-007 — Permission Decisions Belong to the Permission Engine.**
  Evidence: `DefaultExecutionPipeline.executeResolvedTool` is reached
  only from the `APPROVED`/`APPROVED_WITH_CONFIRMATION` branch, strictly
  after `PermissionEngine.evaluate` returns; a `DENIED` decision never
  reaches Tool resolution (`validateCallCount`/`executeCallCount` both
  0). Outcome: validated, with a genuine caveat — the still-open gap
  that `PermissionEngine.evaluate` does not yet resolve identity first
  (#40) now has higher real-world stakes, since an incorrect `APPROVED`
  decision causes a real `Tool.execute()` call rather than nothing.
- **AD-009 — Everything Important Is Auditable.** Evidence: Unit 9
  publishes real events across three domains; Unit 10's `EventCollector`
  independently confirms observation is possible. Outcome: the decision
  was clarified, not just satisfied — implementation exposed a real
  distinction between publication, observation, and audit reconstruction
  that the original decision text did not separately name.
- **AD-010 — Model Independence.** Evidence:
  `DeterministicPlannerHarness`'s own KDoc cites AD-010 directly and is
  demonstrated deterministic under test (identical arguments produce
  identical output). Outcome: the one Architecture Decision Sprint 1's
  own implementation cites by ID in its own documentation.
- **AD-013 — Specifications Define Contracts.** Evidence: every one of
  the 9 implementation units cites a specific specification section as
  its authorising basis. Outcome: validated, and one edge case was
  exposed and named — a contract addition (`TaskProposal.resourceReferences`)
  grounded in a specification's existing prose, ahead of that
  specification's own field list being updated to match.
- **AD-014 — Architecture Before Implementation.** Evidence:
  `DeterministicPlannerHarness` was deliberately built as a minimal test
  fixture in `tests/runtime/`, not a production Planner, specifically
  because the Planner Runtime Specification has not received the same
  review-and-correction pass as the Agent Runtime and Task Manager
  Runtime Specifications. Outcome: the decision was followed under
  pressure to "just build the Planner," not merely stated.
- **AD-015 — Invalid Is Not Denied.** Evidence: Unit 11A's three new
  cases — an unbound `ToolDescriptor`, a failed `Tool.validate()`, and a
  failed `Tool.execute()` — each independently produce `FAILED`, never
  `DENIED`. Outcome: strengthened — the first evidence of this rule
  applying after Permission Engine approval, not only before it.
- **AD-016 — Terminal Lifecycle States Are Final.** Evidence: three new
  lifecycle machines (`PlanningSessionLifecycleTransitions`,
  `TaskLifecycleTransitions`, `AgentRunLifecycleTransitions`), each with
  a dedicated negative test proving no transition out of a terminal
  state. Outcome: strengthened by three new, independently-tested
  instances of an already-Accepted rule.

Decisions not listed here (AD-001, AD-008, AD-011, AD-012) were reviewed
during the Architecture v1.1 reconciliation and found either unchanged
by Sprint 1's scope or not meaningfully exercised — they are not
included because Sprint 1 did not actually generate new evidence for
them, not because they were found wanting.

---

## 4. What Implementation Taught Us

- **Separating `ToolRegistry` from `ToolInvocationBinding` was the
  correct call, and coding proved why.** `ToolRegistry.resolve` was
  already implemented and tested before Sprint 1; changing its return
  shape to carry a live `Tool` would have meant touching an
  already-tested Volume 3 interface and its one existing implementation.
  Adding a separate, additive interface let Unit 11A close the gap
  without touching either. The cost is real and is now tracked as its
  own item (#41): the Execution-Pipeline-only restriction on the new
  interface is enforced by documentation only, the same limitation
  `ToolRegistry.resolve` already had.
- **Resource propagation had to be made explicit, not inferred.** Unit
  11B could not honestly claim an end-to-end vertical slice without a
  field carrying a caller-supplied `ResourceId` from `TaskProposal`
  through to `ExecutionRequest`. The specification's own prose
  (`PlannerRuntimeSpecification.md` §9: "a Plan Candidate or Task
  Proposal may target" a Resource) already anticipated this; its
  Section 10 field list did not name it. Implementation is what surfaced
  the gap between what a specification implies and what it formally
  declares.
- **Publication and observation are not the same guarantee.** Unit 9
  (publication) and Unit 10 (observation) could have been treated as one
  unit; building them separately made clear that "an event was
  published" and "something can prove an event was published" are
  different claims, and that neither is "this is audited" in the Chapter
  43 sense, since no Audit component exists in this repository.
- **Executable architecture reveals assumptions specifications don't
  state.** Nothing in `ExecutionPipeline.md` or `ToolRegistry.md` says
  `resolve()` must be called before `Tool.validate()`, or that `validate()`
  must precede `execute()` — but writing the actual sequence forced an
  explicit, ordered decision the prose never had to make.
- **Architecture-first development paid for itself directly.** Every
  Sprint 1 unit built against an already-reviewed specification section;
  the one exception (the Planner) was deliberately built as a minimal
  test fixture rather than a production component, specifically because
  its specification had not yet received a review-and-correction pass.
  No unit had to be reworked because the underlying specification was
  wrong.

---

## 5. Unexpected Discoveries

- **`ToolInvocationBinding` became its own architectural interface, not
  a Task Manager or Execution Pipeline implementation detail.** The
  original blocker (#32) could have been closed by changing
  `ToolResolution.Resolved` to carry a `Tool` field. It was not, because
  that would have required modifying an already-tested Volume 3
  interface. This mattered because it produced a smaller, safer change,
  but it also produced a new, permanent seam in the architecture that
  now needs its own specification document and its own tracked gap
  (#41) — a genuine, ongoing cost of the safer choice.
- **The resource propagation gap.** Nothing before Unit 11B's test-writing
  revealed that `TaskProposal` had no way to carry a target Resource
  forward. This mattered because it was not caught by specification
  review, AD reconciliation, or any of Units 4–11A's own test suites — it
  was caught only by the discipline of insisting the end-to-end test use
  real objects rather than reconstructed ones.
- **A parser failure caused by an accidental `*/` inside a KDoc comment.**
  `tests/runtime/VerticalSliceEndToEndTest.kt` contained prose reading
  "agent.* + execution.*/permission.* on one shared bus"; the literal
  `*/` closed the enclosing KDoc block 25 lines early, producing roughly
  251 cascading parser errors that looked like a much larger failure
  than it was. This mattered because it demonstrated that a
  documentation-only change (prose inside a comment) can produce a
  compile-blocking failure indistinguishable, at first glance, from a
  logic error.
- **Implementation exposed stale architecture documents that had never
  been wrong before.** `docs/architecture/tool-registry.md`,
  `AgentRuntimeSpecification.md`, and `TaskManagerRuntimeSpecification.md`
  each contained a sequence diagram showing `ToolRegistry.resolve()`
  handing back a bound `Tool` directly — accurate when written, since
  `ToolInvocationBinding` did not exist yet, but silently wrong the
  moment Unit 11A shipped. This mattered because none of these documents
  were touched by the units that made them stale; only a dedicated,
  later documentation reconciliation pass caught them.
- **Documentation drift compounds silently.** `INTER_SPECIFICATION_CONTRACTS.md`
  claims in its own Status section to describe current repository state,
  yet nine separate rows still said "not yet implemented in Kotlin" for
  work Sprint 1 had already completed. This mattered because the
  document's own stated purpose (letting a contributor avoid
  cross-referencing six specification volumes by hand) fails exactly
  when it goes stale without anyone noticing.

---

## 6. Mistakes and Corrections

- **A Kotlin trailing-lambda parameter-order bug.** Adding a defaulted
  `toolFor` parameter after a required `decisionFor` parameter in a test
  helper broke 9 pre-existing call sites, because Kotlin binds a trailing
  lambda to the last parameter regardless of which parameter is
  defaulted. Correction: reordered the parameters so the lambda-bound
  parameter remained last; verified via a full static re-read of all
  affected call sites before reporting the fix.
- **An initial end-to-end test proved success, not propagation.** The
  first version of `VerticalSliceEndToEndTest.kt` asserted `SUCCESS` but
  did not prove the same `ResourceId` object survived the full chain.
  Correction: rewrote the test to extract and directly `assertEquals`
  the same `ResourceId` object at each hop (`TaskProposal` →
  `AgentRunCommand` → `ExecutionRequest`), rather than accepting a
  passing status as sufficient proof.
- **Stale documentation caused by predating the interfaces they now
  described.** Several architecture documents described the Tool
  resolution path from before `ToolInvocationBinding` existed and were
  never revisited when it was added. Correction: the Architecture v1.1
  documentation reconciliation pass located and corrected every
  sequence diagram and prose chain describing this stage.
- **An implementation gap that changed meaning after coding.**
  `IMPLEMENTATION_GAPS.md` #32 was originally about no concrete `Tool`
  implementation existing to invoke at all. Once Unit 11A closed it, the
  document required a second pass to distinguish it from a related but
  different, still-open gap: the Execution-Pipeline-only restriction on
  the new interface being convention-based, not construction-enforced.
  Correction: #32 was marked closed with its original text retained for
  historical record, and a new, separately-numbered gap (#41) was opened
  rather than reusing #32's number for a materially different concern.
- **A specification's field list lagged its own prose.** `TaskProposal.resourceReferences`
  had no home in `PlannerRuntimeSpecification.md` §10's field list, even
  though §9's prose already described the concept. Correction: the field
  was added to §10 during the Architecture v1.1 reconciliation, citing
  §9's existing language rather than inventing new scope.

None of the above required reopening or reworking a completed
implementation unit. Every correction was either a test-code fix, a
documentation correction, or an additive contract change explicitly
scoped and approved before being made.

---

## 7. Engineering Practices Worth Repeating

- **Architecture before implementation.** Every unit built against an
  already-reviewed specification section; the one component without a
  reviewed specification (the Planner) was deliberately scoped down to a
  test fixture rather than built as production code. This should remain
  standard practice because it is the direct reason no unit needed
  architectural rework.
- **Implementation in small vertical slices, one unit per commit.** Each
  of the 9 units added one clearly-scoped capability and produced one
  commit, one review, and one test count. This should continue because
  it made every regression traceable to a single, small change rather
  than a large batch.
- **Pre-code architecture analysis before coding begins.** Units 6
  through 11B each began with an explicit scope statement and boundary
  check before any Kotlin was written. This should continue because it
  is what caught the resource-propagation gap and the Task
  Proposal/Agent Run Command contract gaps before they became rushed
  mid-implementation decisions.
- **Static verification before claiming a result.** Every fix in this
  sprint (the trailing-lambda bug, the KDoc parser failure) was
  confirmed via direct re-reading of the affected files before being
  reported as resolved. This should continue because it is what
  prevented a partial fix from being reported as complete.
- **Android Studio as the authoritative compilation and test gate.**
  Static verification caught structural issues, but every unit's actual
  pass/fail state was confirmed by a real Android Studio run, not
  inferred. This should continue because static analysis and an actual
  compiler are not substitutes for each other.
- **Documentation reconciliation before declaring a milestone
  complete.** The Architecture v1.1 pass caught real, pre-existing
  contradictions (stale sequence diagrams, a contracts-tracking document
  claiming to be current while nine rows were stale) that no individual
  implementation unit would have caught, because none of those units
  touched the documents in question. This should continue because
  implementation and documentation drift independently unless someone
  deliberately reconciles them.
- **Milestone tagging.** Tagging `sprint1-complete` at the point the
  vertical slice was proven, separately from tagging the documentation
  baseline (`architecture-v1.1`) once reconciliation was verified
  complete, kept "the code is done" and "the documentation describing it
  is accurate" as two distinct, separately-checkable claims. This should
  continue because collapsing them into one tag would have hidden the
  gap between Unit 11B's completion and the documentation catching up to
  it.

---

## Things We Deliberately Did Not Build

These were excluded from Sprint 1's scope by design, per
`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md`'s own Non-Scope
list — not overlooked, and not implied to be easy:

- Production Planner intelligence (the Planner Runtime Specification
  remains unpromoted to an implementation phase; `DeterministicPlannerHarness`
  is a fixed test fixture, not a reasoning component).
- Real Permission Engine policy (`FakePermissionEngine` always returns a
  caller-supplied decision).
- Scheduling (no background execution queue; processing is synchronous
  within `submit()`).
- Persistence (all runtime state is in-memory only).
- Distributed execution (a single-runtime-instance assumption throughout).
- Full Task Manager dispositions (`Deferred`/`Split`/`Merged`/business-reason
  `Rejected` are specified but not implemented; only `Accepted` is).
- Full Agent Runtime command handling (`SUSPEND`/`RESUME`/`CANCEL` are
  explicitly rejected, not implemented; only `START` is).
- Workflow Runtime (not specified or implemented; Task Manager Tasks are
  not yet composed into structured multi-step processes).
- Production audit reconstruction (`EventCollector` is a test-only
  observation fixture; no Chapter 43 Audit and Observability component
  exists in this repository).

---

## 8. Technical Debt Intentionally Carried Forward

- **`ToolInvocationBinding` and `ToolRegistry.resolve` access restriction
  remains convention-based, not construction-enforced** (`IMPLEMENTATION_GAPS.md`
  #41). No caller-identity or visibility mechanism exists anywhere in
  this repository to enforce "Execution-Pipeline-only" by construction.
- **Richer Permission Engine policy.** `FakePermissionEngine` remains a
  test-only fixture that always returns a caller-supplied decision; no
  real authorisation policy is specified anywhere (#25).
- **Suspended Agent lifecycle.** Only `AgentRunCommandType.START` is
  implemented; `SUSPEND`, `RESUME`, and `CANCEL` are explicitly rejected,
  not silently no-opped, but not implemented.
- **Task Manager disposition beyond Accept.** `Deferred`, `Split`,
  `Merged`, and business-reason `Rejected` are real, specified
  dispositions with no Sprint 1 implementation.
- **`PermissionEngine.evaluate` not wired to resolve identity first**
  (#40) — a pre-existing gap whose real-world consequence increased once
  Unit 11A made Tool execution real.
- **Identity revocation propagation and non-Active Principal suppression**
  (#35, #36, #37) — each explicitly recorded as requiring a human
  decision, not invented during Sprint 1.
- **Persistence.** All Sprint 1 runtime components are in-memory only;
  nothing survives process restart.
- **Scheduling.** No background execution queue exists; `DefaultExecutionPipeline`
  processes synchronously within `submit()`.
- **Distributed runtime.** Every Sprint 1 component assumes a single
  runtime instance; no multi-instance consistency has been specified or
  built.
- **Planner Runtime remains unpromoted to an implementation phase.**
  `DeterministicPlannerHarness` is a test fixture standing in for a
  production Planner that does not yet exist, pending the Planner Runtime
  Specification's own review-and-correction pass (AD-014).

---

## 9. Sprint Metrics

| Metric | Value |
|---|---|
| Architecture version | v1.0 → v1.1 |
| Repository tags | `sprint1-complete`, `architecture-v1.1` |
| Implementation units completed | 9 |
| Architecture Decisions reviewed | 16 |
| New Architecture Decisions adopted | 0 |
| Android Studio tests | 234/234 |
| Major runtime components implemented or wired | `DeterministicPlannerHarness`, `InMemoryTaskManagerRuntime`, `InMemoryAgentRuntime`, `InMemoryToolInvocationBinding`, `MockTool`, `EventCollector` |
| Implementation gaps closed by Sprint 1 | #32 |
| New implementation gaps opened by Sprint 1 | #41 |

- Architecture version: v1.0 at Sprint 1 start → v1.1 at Sprint 1 close.
- Repository tags: `sprint1-complete`, `architecture-v1.1`.
- Implementation units completed: 9 (Units 4, 5, 6, 7, 8, 9, 10, 11A,
  11B).
- Architecture Decisions reviewed against Sprint 1 evidence: 16 of 16;
  12 found to have new supporting evidence (Section 3 above); none
  superseded, replaced, or contradicted; none newly created (a candidate
  AD-017 was evaluated and explicitly not adopted, short of the
  two-of-three-instances threshold this repository's own governance
  document requires).
- Implementation gaps: 41 tracked entries as of this retrospective. One
  gap (#32) was closed specifically by Sprint 1 (Unit 11A); the
  remaining ~20 resolved/partially-resolved entries in
  `IMPLEMENTATION_GAPS.md`'s closure summary predate Sprint 1 (Phase 2
  Runtime work). One new gap (#41) was opened as a direct consequence of
  Sprint 1's own implementation.
- Android Studio tests: 146 (Unit 4) → 234 (Unit 11B), 234/234 passing
  at Sprint 1 close.
- Major runtime components added or wired: `DeterministicPlannerHarness`,
  `InMemoryTaskManagerRuntime`, `InMemoryAgentRuntime`,
  `InMemoryToolInvocationBinding`, `MockTool`, `EventCollector`; one
  existing component (`DefaultExecutionPipeline`) extended; one existing
  fixture (`FakePermissionEngine`) independently verified, unmodified.

---

## 10. Sprint 2 Readiness

Sprint 1 answered the structural question Parker's architecture had not
yet been forced to answer: does the specified chain actually run,
end-to-end, with real objects and real Tool execution, or only on paper.
It now does, under test, with the propagation of a real `ResourceId`
proven at every hop and the full lifecycle event sequence independently
observed by `EventCollector`. The Architecture v1.1 reconciliation pass
additionally confirmed that the architecture documents describing this
chain are no longer contradicted by stale sequence diagrams or a
contracts-tracking document reporting superseded status.

This means Sprint 2 does not need to spend effort re-proving that the
Planner-to-Execution-Pipeline chain is real, that permission evaluation
gates execution, or that lifecycle events are observable — those are now
demonstrated facts, not architectural claims awaiting implementation.
Sprint 2 can instead focus on capability expansion within an already-
proven skeleton: broadening Task Manager disposition beyond Accept,
implementing Agent Run `SUSPEND`/`RESUME`/`CANCEL`, and building real
Permission Engine policy, among the items listed in Section 8, each of
which now has a real, tested chain to extend rather than a specification
to first prove executable.

---

## 11. Principles Earned Through Implementation

- **Architecture should lead implementation, not follow it.** Every
  Sprint 1 unit that built against an already-reviewed specification
  section required no rework; the one component built ahead of its
  specification's review (the Planner) was deliberately scoped down to a
  test fixture rather than treated as production-ready.
- **Explicit contracts are preferable to implicit behaviour.**
  `TaskProposal.resourceReferences` and `AgentRunCommand` (Blocker 3)
  both exist because a specification's prose implied a capability its
  field list did not name; both were closed by adding a named field or
  interface rather than inferring the behaviour at the call site.
- **Execution authority belongs in exactly one place.** `DefaultExecutionPipeline.executeResolvedTool`
  is the only call site in the repository that invokes
  `ToolInvocationBinding.invocableFor`, `Tool.validate`, or
  `Tool.execute` — confirmed by direct reading of every production file,
  not assumed from the specification.
- **Event publication and event observation are separate
  responsibilities.** Unit 9 (publication) and Unit 10 (observation)
  were built as separate units specifically so that "an event was
  published" and "an event's publication can be independently verified"
  remained two distinct, separately-testable claims.
- **Documentation should be reconciled immediately after major
  milestones, not deferred.** The Architecture v1.1 pass found real,
  load-bearing documents (`INTER_SPECIFICATION_CONTRACTS.md`, three
  specification-level sequence diagrams) that had drifted silently; none
  of that drift was caught by any individual implementation unit, only
  by a dedicated reconciliation pass run immediately after Sprint 1
  closed.
- **Executable architecture is the strongest architectural validation
  available.** Sprint 1 exposed one real specification gap (resource
  propagation) that survived architecture review, AD reconciliation, and
  every earlier unit's own test suite, and was caught only by insisting
  an end-to-end test use real, traceable objects rather than an assumed
  success path.

---

## Sprint 1 in One Paragraph

Sprint 1 established Parker's first executable architectural baseline.
Rather than maximising features, it proved that the core runtime
architecture described by the specifications could execute a complete
request from Goal to Tool through the Planner, Task Manager, Agent
Runtime, Permission Engine, Execution Pipeline, Tool Registry,
ToolInvocationBinding, and lifecycle event system. Architecture v1.1 then
reconciled the specifications against the implementation so the
documented architecture again matched the running system. Sprint 2
begins from a verified foundation rather than an architectural
hypothesis.

---

## 12. Final Assessment

**Was Sprint 1 successful?** Yes, measured against what it set out to
prove: a fixed Goal now reaches real Tool execution through every
architecturally-mandated stage (Planner, Task Manager, Agent Runtime,
Permission Engine, Tool Registry, ToolInvocationBinding), verified by
234/234 passing tests and an end-to-end test that traces a single
`ResourceId` object through the entire chain rather than inferring
success from a status code. It was not successful at, and did not
attempt, general-purpose runtime behaviour — Task Manager disposition,
Agent Run control, and Permission Engine policy all remain intentionally
narrow, and that narrowness is disclosed throughout `IMPLEMENTATION_HISTORY.md`
and this document rather than hidden.

**What practices should unquestionably continue?** Architecture-first
development with an explicit review-and-correction pass before
implementation; small, single-responsibility vertical slices with one
commit and one review per unit; static verification before any result is
reported, followed by an actual Android Studio run as the real gate; and
documentation reconciliation as a distinct, mandatory step immediately
after a milestone, not an optional cleanup deferred to "later."

**What would be done differently if Sprint 1 were repeated?** The
resource-propagation gap and the stale Tool-resolution sequence diagrams
across three specification documents both survived every review pass
that preceded coding and were caught only afterward — by an end-to-end
test in the first case, and by a dedicated post-hoc consistency review in
the second. A repeat of Sprint 1 would benefit from a lighter-weight,
earlier check for exactly this failure mode: before implementation
begins, explicitly trace one full example object (a Resource reference,
a Tool invocation) through every specification document it touches, not
only through the specification sections directly relevant to the unit
being built, to catch field-list omissions and now-obsolete diagrams
before they reach implementation or require a separate reconciliation
pass to find.
