# Parker Architecture v1.1 Review Brief

## Purpose

Sprint 1 (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md`) built one
fixed, deterministic, executable path through the platform's core
runtime — Goal through real Tool execution — under test, in Kotlin, for
the first time. This brief reconciles what that implementation actually
proved, clarified, or exposed against `architecture-v1.0`
(`docs/architecture/ARCHITECTURE_DECISIONS.md`, the Volume 3–6
specifications, and `docs/architecture/IMPLEMENTATION_GAPS.md`).

This is a review, not a rewrite. No architecture document is modified by
this brief. No Sprint 2 implementation work is started or specified
beyond readiness recommendations. Every claim below is either **proven in
code** (an executable test exists and passes), **clarified by
implementation** (a real decision was made where the architecture was
silent or ambiguous), **still unresolved** (a documented gap the
implementation did not close), or **proposed for v1.1** (a documentation
or decision change this brief recommends but does not itself make).

## Current Baseline

- Repository: `parker-platform`, branch `main`, tag `sprint1-complete`,
  commit `795544d`.
- Android Studio: 234/234 tests passing.
- Architecture baseline going in: `architecture-v1.0`
  (`docs/architecture/ARCHITECTURE_DECISIONS.md`, 16 Accepted decisions,
  AD-001–AD-016).
- Sprint 1's own stated goal (`SPRINT_1_VERTICAL_SLICE_PLAN.md` §1): "a
  proof of the *shape* of the chain working end-to-end in memory, under
  test, with fixed inputs — nothing more." Not a claim of production
  readiness, policy completeness, or general-purpose Planner/Task
  Manager/Agent Runtime behaviour.

## Sprint 1 Implementation Summary

Eleven units (`docs/implementation/IMPLEMENTATION_HISTORY.md`), each one
commit, one review, one Android Studio gate:

| Unit | What it added |
|---|---|
| 4 | `MockTool` fixture (deterministic, in-memory `Tool`) |
| 5 | `DeterministicPlannerHarness` — fixed Goal → one `TaskProposal` |
| 6 | `InMemoryTaskManagerRuntime` — accept-only intake, `TaskProposal` → `Task` → `AgentRunCommand` |
| 7 | `InMemoryAgentRuntime` — `AgentRunCommand` → one Agent Step → `ExecutionRequest` |
| 8 | Standalone `FakePermissionEngine` test coverage (no behaviour change) |
| 9 | Runtime lifecycle event publication (`planner.*`/`task.*`/`agent.*`), inserted ahead of the original plan's Unit 9 |
| 10 | `EventCollector` test harness |
| 11A | Wired `DefaultExecutionPipeline` to `ToolInvocationBinding` — real `Tool.validate()`/`Tool.execute()` invocation |
| 11B | `TaskProposal.resourceReferences` — resource reference propagation and the first honest end-to-end proof |

Final state: `Goal → Planner → TaskProposal → Task Manager → Task →
AgentRunCommand → Agent Runtime → ExecutionRequest → Permission Engine →
Action Mapping → Tool Registry → ToolInvocationBinding →
Tool.validate() → Tool.execute() → ExecutionResult → Lifecycle Events →
EventCollector`, exercised by `tests/runtime/VerticalSliceEndToEndTest.kt`
with no hand-built `ExecutionRequest` and no manual patching anywhere in
the chain.

## What v1.0 Got Right

- **AD-002/AD-003 (Proposal Before Authority / Execution Pipeline is
  sole execution authority) held under real code, not just on paper.**
  Every unit that could have taken a shortcut didn't: the Planner never
  writes a `taskId` (Unit 5), the Task Manager never resolves a `Tool`
  (Unit 6), the Agent Runtime never calls `Tool.execute` directly (Unit
  7) — it constructs an `ExecutionRequest` and submits it through the one
  existing `DefaultExecutionPipeline`. `ToolInvocationBinding`, even once
  wired (Unit 11A), is called from exactly one place:
  `DefaultExecutionPipeline.executeResolvedTool`.
- **AD-015 (Invalid Is Not Denied) required no new logic — it was
  already correct, pre-Sprint-1 behaviour Unit 11A's new failure paths
  simply had to preserve.** An unbound Tool, a failed `Tool.validate`,
  and a failed `Tool.execute` all had to be threaded through as `FAILED`,
  never `DENIED` — proven directly by
  `tests/runtime/DefaultExecutionPipelineTest.kt`'s Unit 11A tests.
- **AD-009 (Everything Important Is Auditable) shaped the actual
  sequencing of Unit 9's event wiring, not just its existence.** The
  requirement that `execution.request_received` fire before Permission
  Engine evaluation (so a later-rejected request still has an audit
  record) and that a step only be reported `agent.step_completed` after
  its real outcome is known are both consequences of taking AD-009
  seriously at the ordering level, confirmed end-to-end in
  `VerticalSliceEndToEndTest.kt`.
- **The placeholder-identity pattern (`system.planner-runtime`,
  `system.task-manager-runtime`, `system.event-collector`) scaled cleanly
  across five units** without needing early invention of a real Sprint 1
  identity-allocation scheme — exactly the kind of "future architecture
  is documented rather than implemented early" discipline
  `IMPLEMENTATION_HISTORY.md`'s own Implementation Principles state.
- **The shared-`correlationId` design (`TaskProposal.correlationId` →
  `AgentRunCommand.correlationId` → `ExecutionRequest.correlationId`)
  was already implicit in the Volume 4–6 specifications and required no
  new decision** — Unit 9 and 11B both simply threaded the existing field
  through; `VerticalSliceEndToEndTest.kt` proves one correlationId
  reaches all 18 published events.

## What Sprint 1 Proved

Proven in executable, passing code (234/234):

- A `Tool` bound via `ToolInvocationBinding` is actually invoked —
  `validate()` then `execute()`, in that order, only after
  `PermissionEngine.evaluate` returns `APPROVED`/`APPROVED_WITH_CONFIRMATION`
  — closing `IMPLEMENTATION_GAPS.md` #32 (Unit 11A,
  `src/runtime/DefaultExecutionPipeline.kt`).
- A `ResourceId` a caller supplies at the Planner boundary
  (`DeterministicPlannerHarness.run`'s `targetResourceReferences`) is the
  *same* `ResourceId` object observed inside the real `ExecutionRequest`
  `InMemoryAgentRuntime` builds internally — proven by direct
  `ResourceId` object-equality assertions at every hop
  (`TaskProposal.resourceReferences` → `AgentRunCommand.resourceReferences`
  → `ExecutionRequest.targetResources`), not string comparison or
  reconstruction (Unit 11B, `tests/runtime/VerticalSliceEndToEndTest.kt`).
- The full causal event sequence for one Goal, across Planner/Task
  Manager/Agent Runtime/Execution Pipeline, is exactly 18 events in a
  fixed, now-twice-independently-verified order
  (`tests/runtime/EventCollectorTest.kt` and
  `VerticalSliceEndToEndTest.kt` agree), sharing one `correlationId`.
- `Task.kt` genuinely does not carry a `resourceReferences` field —
  confirmed by direct reading, not assumed — meaning the resource
  reference chain that exists today is `TaskProposal → AgentRunCommand →
  ExecutionRequest` only, with no continuity through the canonical `Task`
  record itself.
- `contextReferences` and `resourceReferences` are independently testable
  and do not interfere with each other on `TaskProposal`
  (`tests/contracts/TaskProposalTest.kt`).

## Clarifications Required for v1.1

These are places where v1.0 was silent or ambiguous, and Sprint 1's
implementation had to make a concrete choice — not because the
architecture was wrong, but because it hadn't yet said which of several
reasonable options applied:

1. **`TaskProposal` needed a Resource-carrying field the Proposal Model
   never listed.** `PlannerRuntimeSpecification.md` §10 ("Proposal
   Model") has no Resource-references field, but §9 ("Planning Context
   Model") already says, in its own words, that Planning Context's
   Resource references are "identifiers of Resources a Plan Candidate or
   **Task Proposal may target**." Unit 11B's
   `TaskProposal.resourceReferences` closes this the same way Blocker 3
   (`AgentRunCommand`) closed an identical prose-anticipated-but-unshaped
   gap. **v1.1 should record this as the second instance of that
   pattern** (a specification's own prose names a capability its field
   list omits), since AD-005/AD-002 already treat the Planner as the
   correct owner of this data.
2. **The vertical slice could not honestly reach `SUCCESS` without this
   field.** `SPRINT_1_VERTICAL_SLICE_PLAN.md` §2 assumed "Planning
   Context gathering is trivial (no real Resource/Task/Agent Run
   references needed for the fixed Goal)," but §7's own acceptance
   criteria required a genuine `SUCCESS` `ExecutionResult` from the real
   (non-hand-built) chain — which is structurally impossible without
   `ActionMapper.map` receiving a non-empty `targetResourceTypes` set.
   This tension was real, not a misreading, and Unit 11B's split from
   Unit 11A exists specifically because resolving it required an
   additive contract change requiring separate authorization.
3. **`contextReferences` and `resourceReferences` are deliberately
   different concepts and must not be merged.** `contextReferences`
   remains "opaque identifiers into Planning Context, resolved only by
   the Planner Runtime that produced them... never copied inline"
   (`TaskProposal.kt`); `resourceReferences` is concrete, already-known
   `ResourceId`s carried forward for downstream consumption. Sprint 1
   never blurred this distinction (verified by direct test coverage that
   the two fields are independent), but v1.0 has no single document
   stating the general principle "opaque planner-internal references and
   concrete downstream-consumable identifiers are different field
   categories, even when a proposal-shaped object might carry both."
4. **Unit 9 existing at all was itself a clarification, not just an
   implementation step.** The original plan assumed lifecycle events
   already existed before an `EventCollector` unit could usefully
   observe them; when Units 5–7 turned out not to publish any events,
   this was resolved by inserting a new Unit 9 (event publication) ahead
   of the renumbered EventCollector unit, rather than by silently
   expanding Unit 10's own scope or leaving the plan unimplementable.
   AD-009 was always the rule; what v1.0's own Sprint 1 plan hadn't
   accounted for was *which unit* was responsible for making it true.

## Architecture Gaps Still Open

Carried forward, unresolved by Sprint 1 (still accurate as of commit
`795544d`):

- **`ToolInvocationBinding`'s "Execution-Pipeline-only" access remains
  convention-based, not construction-enforced.**
  `InMemoryToolInvocationBinding`'s own KDoc states this explicitly:
  enforcing it "would mean introducing a caller-identity or visibility
  mechanism that does not exist anywhere else in this repository today
  (including on `ToolRegistry.resolve` itself, which this type is
  deliberately built to match)." `SPRINT_1_VERTICAL_SLICE_PLAN.md` §7's
  own acceptance criterion ("true by construction... not merely true by
  convention") is not fully met. Wiring Unit 11A did not change this —
  it inherited the same limitation `ToolRegistry.resolve` already had.
- **`PermissionEngine` policy remains fake/test-only.**
  `FakePermissionEngine` (`tests/runtime/FakePermissionEngine.kt`) is a
  caller-supplied decision function, not authorization policy.
  `IMPLEMENTATION_GAPS.md` #25 records this as a "deliberate scope
  boundary, not an oversight" — no policy (who may do what, under what
  circumstances) is specified anywhere in the architecture yet. Sprint 1
  did not touch this; Unit 8 only added direct test coverage of the
  existing fixture.
- **Identity Service does not enforce non-ACTIVE Principal status at the
  points that should check it.** Two independent, already-documented
  gaps: `resolve()` does not suppress Revoked/Archived Principals
  (`IMPLEMENTATION_GAPS.md` #37 — "requires human decision"), and
  `PermissionEngine.evaluate` is not wired to resolve identity first or
  short-circuit to `DENIED` for a Suspended/Revoked Principal
  (`IMPLEMENTATION_GAPS.md` #40 — "deliberately not done, explicit
  instruction for this phase"). Sprint 1's fixed happy path never
  exercised a non-ACTIVE Principal, so neither gap was touched.
- **Planner Runtime remains a deterministic, partial harness, not a
  general Planner Runtime.** `DeterministicPlannerHarness` models 5 of
  the Planner Runtime Specification's 10 lifecycle states by design (its
  own KDoc), generates exactly one Plan Candidate, performs no Plan
  Decision, and never calls `TaskProposalIntake.submitProposal` itself —
  a caller must do that. This was never claimed otherwise, but v1.1
  readers should not infer general Planner behaviour from Sprint 1's
  passing tests.
- **Task Manager Runtime remains accept-only.**
  `InMemoryTaskManagerRuntime.submitProposal` unconditionally accepts any
  proposal with a resolvable owner; `Deferred`/`Split`/`Merged`, and
  business-reason `Rejected`, are real, specified outcomes
  (`TaskManagerRuntimeSpecification.md` §15) with no implementation.
- **Agent Runtime remains START-only.** `SUSPEND`/`RESUME`/`CANCEL` all
  return `AgentRunCommandResult.Rejected` with an explicit reason, never
  a silent no-op — but they are not implemented, and
  `WAITING_FOR_PERMISSION`/`WAITING_FOR_INPUT`/`SUSPENDED` are never
  driven by this synchronous runtime.
- **EventBus wildcard/prefix subscription remains an open question, not
  a gap Sprint 1 was expected to close.** `EventCollector` subscribes to
  a fixed, hand-maintained snapshot of concrete `EventType`s
  (`SPRINT_1_EVENT_TYPES`) because `EventType.md`'s own Open Questions
  section leaves prefix/wildcard matching unresolved. This is correctly
  scoped as pre-existing, not something Unit 10 was meant to resolve.
- **No cryptographic signature verification exists for trust-sensitive
  events** (`permission.*`/`execution.*` — presence/non-blank check
  only, `IMPLEMENTATION_GAPS.md` #26), and **`EventBus` publisher/
  subscriber authentication is a documented `AllowAllPrincipalAuthenticator`
  placeholder**, both untouched by Sprint 1 and exploited safely only
  because every Sprint 1 principal is itself a placeholder.

## Architecture Gaps Closed by Sprint 1

- **`IMPLEMENTATION_GAPS.md` #32 ("No concrete `Tool` implementation
  exists to actually invoke") is closed.** `DefaultExecutionPipeline` now
  obtains the bound `Tool` via `ToolInvocationBinding.invocableFor`,
  calls `validate()` then `execute()`, and a `SUCCESS` `ExecutionResult`
  means a Tool actually ran. **This entry, and its listing under
  "Deliberate scope boundaries / known, documented limitations" in the
  Phase 2 Gap Closure Summary table at the end of
  `IMPLEMENTATION_GAPS.md`, are now stale text and are the single most
  concrete v1.1 documentation action item** (see below).
- **The resource-reference propagation gap this brief's own review
  surfaced during Unit 11's pre-code analysis is closed for the one path
  that structurally exists** (`TaskProposal.resourceReferences` →
  `AgentRunCommand.resourceReferences` → `ExecutionRequest.targetResources`).
  It was never a gap recorded in `IMPLEMENTATION_GAPS.md` under its own
  number — it was discovered and closed within Sprint 1 itself (Unit
  11B) — so there is no existing gap-file entry to retire for it, only a
  new decision to record (see Proposed New Architecture Decisions).

## Proposed v1.1 Documentation Updates

Concrete, low-risk edits — not made by this brief, recommended for a
follow-up documentation pass:

1. **`docs/architecture/IMPLEMENTATION_GAPS.md` #32** — update its
   `**Status:**` line from "Known, expected limitation — not a defect in
   this phase's scope" to reflect closure, citing Unit 11A / commit
   `13c9322`. Move its listing in the "Phase 2 Runtime — Gap Closure
   Summary" table from "Deliberate scope boundaries" to "Resolved."
2. **`docs/architecture/IMPLEMENTATION_GAPS.md`** — add one new,
   explicitly-numbered entry (or a clearly-marked Sprint 1 addendum)
   recording the `ToolInvocationBinding` access-enforcement gap in its
   *current* form (post-11A: the binding is wired and used, but still
   only convention-restricted to the Execution Pipeline) — distinct from
   the pre-11A #32, which was about the binding not existing/being
   called at all.
3. **`docs/implementation/IMPLEMENTATION_HISTORY.md`** — already updated
   through Unit 11B as part of this same reconciliation pass (Repository
   Status, Unit 11A/11B sections, Current Vertical Slice diagram, Current
   Runtime Chain, and Current Known Architecture Gaps all current as of
   commit `795544d`). No further action needed here for v1.1.
4. **`PlannerRuntimeSpecification.md` §10 ("Proposal Model")** — add a
   Resource references field entry, citing §9's own already-existing
   "Task Proposal may target" language as the source, so the
   specification's field list matches what `TaskProposal.kt` now
   implements. This is documentation catch-up, not a new decision — the
   Kotlin already exists and is tested.
5. **A short cross-reference note in `TaskManagerRuntimeSpecification.md`
   §9 ("Task Context")** clarifying that Task Context's own "Resource
   references" concept (resources "touched or targeted... for
   continuity") remains distinct from, and not yet wired to,
   `TaskProposal.resourceReferences` — since `Task.kt` itself still
   carries no such field. This prevents a future reader from assuming
   Unit 11B closed continuity at the Task level, which it did not.

## Proposed New or Revised Architecture Decisions

Candidates only — none adopted by this brief, evaluated against
`ARCHITECTURE_DECISIONS.md` §7's own criteria (multiple specifications
depend on it, platform-wide rather than one component's internal
mechanic, changing it would affect multiple subsystems):

- **Candidate: "Prose-anticipated capabilities require an explicit
  contract addition before implementation, named by the same
  evidence-first standard as an Architecture Decision."** Evidenced
  twice now — `AgentRunCommand`'s Suspend/Resume/Cancel shape (Blocker
  3) and `TaskProposal.resourceReferences` (Unit 11B) — both cases where
  a specification's own prose already implied a capability its formal
  field/operation list omitted, and both were resolved the same way:  a
  pure additive contract change, cited directly to the anticipating
  prose. Two independent instances meets the spirit of §7's "if a third
  or fourth specification independently repeats it, it would meet
  Section 7's criteria" bar closely enough to flag now rather than wait
  for a third. **Not yet meeting the letter of §7** (two data points, not
  three-plus), so proposed for tracking, not immediate adoption.
- **Candidate: distinguish, as a named platform-wide vocabulary, "opaque
  producer-only reference" (e.g. `contextReferences`) from "concrete
  downstream-consumable identifier" (e.g. `resourceReferences`,
  `ResourceId` generally) as two different field categories.** This is
  presently enforced only by convention and by the specific KDoc on each
  field; nothing states it as a cross-cutting rule the way AD-011
  (Context Is Reference-Based) states reference-based Context as a rule.
  This may be better captured as a clarifying addendum to AD-011 rather
  than a new AD — recommend evaluating both options during the v1.1
  documentation pass rather than deciding here.
- **Not proposing** a decision about `ToolInvocationBinding` access
  enforcement — this remains a single-component implementation gap
  (`ToolRegistry.resolve` has the identical gap), not a platform-wide
  rule in dispute; AD-003 already states the rule correctly, and closing
  the gap is enforcement work, not a decision change.

## Sprint 2 Readiness Assessment

**Ready:**
- The executable chain itself: Goal → real Tool execution → audited
  `ExecutionResult`, proven under test with no manual patching, is a
  stable foundation to build additional Sprint 2 units against.
- The event model and correlation-ID discipline are proven consistent
  end-to-end and require no rework to extend.
- The additive-contract-change precedent (Blocker 3, Unit 11B) gives
  Sprint 2 a tested pattern for closing future prose-anticipated-but-
  unshaped gaps without inventing new architecture ad hoc.

**Not ready without a decision:**
- Any Sprint 2 unit that exercises a non-ACTIVE Principal (suspended,
  revoked, archived) will hit the two open Identity Service enforcement
  gaps (#37, #40) immediately — these should be resolved or explicitly
  deferred-with-a-plan before such a unit starts, not discovered
  mid-unit.
- Any Sprint 2 unit that requires real Permission Engine policy (as
  opposed to the fixed always-`APPROVED` fixture) has no specified
  policy to implement against yet — this is a specification gap, not an
  implementation one, and blocks any unit that assumes real
  authorization logic.
- Any Sprint 2 unit that touches Task Manager dispositions other than
  Accept, or Agent Run commands other than START, is starting genuinely
  new behavior, not extending Sprint 1's — should be scoped with the same
  pre-code architecture analysis discipline Units 11A/11B used, given
  precedent that such units can surface real, unanticipated contract
  gaps.

## Recommended Pre-Sprint-2 Actions

1. Execute the five documentation updates listed above (all editorial —
   correcting now-stale status text and specification field lists to
   match already-completed, already-tested implementation; none require
   new Kotlin).
2. Make an explicit human decision on `IMPLEMENTATION_GAPS.md` #37
   (should `resolve()` suppress non-Active Principals) and #40 (wiring
   `PermissionEngine.evaluate` to Identity Service) before any Sprint 2
   unit assumes either behavior either way.
3. Decide whether the "prose-anticipated capability" pattern (Blocker 3 /
   Unit 11B) warrants a formal Architecture Decision now, given it has
   two independent instances, or should wait for a third per
   `ARCHITECTURE_DECISIONS.md` §7's stated bar.
4. Confirm whether Sprint 2's first unit is expected to extend the
   Task Manager's disposition set, the Agent Runtime's command set, or
   real Permission Engine policy — each implies a different specification
   dependency and a different pre-code analysis, and picking one now
   avoids re-deriving this brief's own findings mid-Sprint-2.
