# Sprint 1 Blocker Closure

## Status

This is a **contract/specification preparation record**, not an
implementation report. It documents work performed to close the four
blockers `docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` Section 4
and Section 12 identified as required before Sprint 1 coding could begin.
No runtime behaviour was implemented. No file under `src/runtime` or
`tests/` was modified. The only `src/` additions are three new,
purely-additive Kotlin files under `src/contracts/`, each containing
value classes, data classes, sealed types, and interface declarations
only — no function body beyond `init` validation blocks, no concrete
implementation of any new interface.

## 1. Purpose

`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` Section 12 recorded
four ordered blockers preventing Sprint 1 coding from beginning cleanly:
a Task Proposal intake contract, a Task Manager disposition/response
contract, an Agent Run Request contract, and a resolution to the gap
between `ToolRegistry.resolve()` and something an Execution Pipeline can
actually invoke. This document records how each was closed, what was
changed to close it, what remains open, and whether Sprint 1 coding can
now begin.

## 2. Blockers Identified and Resolved

### Blocker 1 — Task Proposal intake contract

**Identified as:** no operation, object, or event set existed by which a
Task Proposal could be submitted to the Task Manager Runtime.

**Resolution:** `src/contracts/TaskProposal.kt` adds `PlanningSessionId`,
`TaskProposalId`, `TaskId` (value classes), `ProposalDependency` (sealed:
`OnExistingTask` / `OnProposal`), the `TaskProposal` data class (all
fields the task specified: proposal id, Planning Session reference,
initiating Principal, proposed owner, proposed assignee, goal, source,
priority, constraints, dependencies, required capabilities, expected
outputs, permission-anticipation flags, context references, rationale,
risk estimate, correlation id), and the `TaskProposalIntake` interface
(`suspend fun submitProposal(proposal: TaskProposal): TaskProposalDisposition`).
`TaskManagerRuntimeSpecification.md` Section 15 ("Task Proposal Intake",
newly added) names this as the operation closing
`PlannerRuntimeSpecification.md` Section 6's previously-unclosed
dependency. **No implementation of `TaskProposalIntake` exists.**

### Blocker 2 — Task Manager disposition/response contract

**Identified as:** even once a Task Proposal could be received, no
mechanism existed for the Task Manager Runtime to communicate
accept/defer/split/merge/reject back to the Planner Runtime.

**Resolution:** `src/contracts/TaskProposal.kt`'s `TaskProposalDisposition`
sealed class (`Accepted`, `Deferred`, `Rejected`, `Split`, `Merged`), each
carrying exactly the fields needed to name its outcome (e.g. `Accepted`
carries the resulting `TaskId`; `Split` carries two-or-more `TaskId`s;
`Merged` carries the merged `TaskId` and the sibling proposal ids it
absorbed) — returned directly from `submitProposal`, not solely
communicated via an event. `TaskManagerRuntimeSpecification.md` Section
15's five-row table states, for each disposition: whether a Task Manager
Task is created, whether further Planner input is needed, whether the
Agent Runtime may be involved later, the required event implication, and
whether the disposition is terminal for the proposal (all five are
terminal, per AD-016 applied to the disposition itself, mirroring the
"no resurrection from a terminal state" pattern already used everywhere
else in this repository).

### Blocker 3 — Agent Run command contract

**Identified as:** the object the Task Manager Runtime would pass to the
Agent Runtime to create, suspend, resume, or cancel an Agent Run was only
implied by a sequence diagram and prose ("explicit suspend request,"
"external cancellation request"), never named or field-shaped.

**Resolution:** `src/contracts/AgentRunCommand.kt` adds `AgentRunId`
(value class), `AgentRunCommandType` (enum: `START`, `SUSPEND`, `RESUME`,
`CANCEL`), the `AgentRunCommand` data class (command type, associated
`TaskId`, `AgentRunId?` — required non-null for `SUSPEND`/`RESUME`/
`CANCEL`, required null for `START`, enforced by an `init` block —
requesting Principal, target Agent Capability set, goal/step description,
context references, permission scope reference, resource references,
correlation id, and `cancellationReason` — required non-blank for
`CANCEL` only, also enforced by `init`), the `AgentRunCommandResult`
sealed class (`Accepted` / `Rejected`), and the `AgentRunCommandChannel`
interface (`suspend fun submit(command: AgentRunCommand):
AgentRunCommandResult`). **No implementation of `AgentRunCommandChannel`
exists** — no concrete Agent Runtime behaviour was introduced.
`TaskManagerRuntimeSpecification.md` Section 16 ("Agent Run Command
Channel", newly added) and `AgentRuntimeSpecification.md`'s Status header
addendum both name this as the mechanism their existing prose already
anticipated without naming.

This single contract closes both the Agent Run *creation* gap
(`INTER_SPECIFICATION_CONTRACTS.md` Gap 7) and the Agent Run
*cancellation/suspend/resume* gap (newly catalogued there as Gap 11,
evidenced by `docs/reviews/Phase3ArchitecturePositionReview.md` Section 6
but never previously folded into that document's own numbered list — the
audit, `docs/reviews/RepositoryArchitectureConsistencyAudit.md` Section
10 Finding T-2, flagged this omission), since both are the same
underlying need: a named channel from the Task Manager Runtime to the
Agent Runtime.

### Blocker 4 — Tool invocation gap (`IMPLEMENTATION_GAPS.md` #32)

**Investigation.** `ToolRegistry.resolve()` returns a `ToolResolution`,
whose `Resolved` case carries only a `ToolDescriptor` — never a live
`Tool` — by explicit, documented design (`ToolResolution.kt`'s own doc
comment: "no concrete `Tool` implementations exist yet to resolve to").
`DefaultExecutionPipeline.submit()` calls `toolRegistry.resolve(...)`,
branches on `ToolResolution.Resolved`, and returns a `SUCCESS`
`ExecutionResult` **without ever calling `Tool.execute`** — its own KDoc
states this plainly: a `SUCCESS` result "means every orchestration stage
up to and including finding the right Tool succeeded," not that a Tool
actually ran. `ToolRegistry.register` and `ToolRegistry.resolve` are both
already implemented and exercised by `tests/runtime/InMemoryToolRegistryTest.kt`.

**Options considered** (per the task's own four-way framing):
1. `ToolRegistry.resolve` itself returns an invocable `ParkerTool` —
   rejected: would require changing `ToolResolution.Resolved`'s shape,
   an already-tested Volume 3 interface's return type, which the task
   explicitly did not authorise ("do not modify production runtime
   logic").
2. `ToolResolution` includes a callable runtime handle — same objection:
   changes an already-tested existing type.
3. `ExecutionPipeline` uses a separate `ToolRuntime` lookup — closest to
   the choice made, but "Runtime" naming risks implying a full execution
   environment rather than a minimal binding lookup.
4. **Chosen: a minimal, additive, free-standing contract** —
   `src/contracts/ToolInvocationBinding.kt`'s `ToolInvocationBinding`
   interface (`suspend fun bind(descriptor: ToolDescriptor, tool: Tool)`;
   `suspend fun invocableFor(descriptor: ToolDescriptor): Tool?`). No
   existing class is required to implement it; its addition breaks
   nothing already built or tested. It is documented as
   Execution-Pipeline-only use, preserving `tool-registry.md`'s existing
   rule verbatim ("nothing except the Execution Pipeline ever holds a
   live `Tool` reference").

**This is recorded as the safe, non-breaking closure for this
contract-preparation task, not as a claim that it is the permanent, best
long-term shape.** The cleaner long-term fix — folding an invocable
handle directly into `ToolResolution.Resolved` so `resolve` itself is the
only lookup an Execution Pipeline ever needs — remains open (Section 4
below). **No implementation of `ToolInvocationBinding` exists, and
`DefaultExecutionPipeline` is not wired to call it** — this closes the
contract-shape half of `IMPLEMENTATION_GAPS.md` #32 only, not the gap
itself.

## 3. Files Changed

**New files (`src/contracts/`, purely additive, no existing file
modified to accommodate them):**

- `src/contracts/TaskProposal.kt` (321 lines) — Blocker 1 and Blocker 2.
- `src/contracts/AgentRunCommand.kt` (196 lines) — Blocker 3.
- `src/contracts/ToolInvocationBinding.kt` (106 lines) — Blocker 4.

All three compile under the existing `build.gradle.kts` `srcDirs("src/contracts", ...)`
configuration with no exclusion changes needed; all three use the
existing `parker.core.interfaces` package and reuse existing types
(`PrincipalId`, `TaskId` newly introduced here since no Task Manager
Runtime Kotlin exists yet, `RequestOrigin`, `RequestPriority`,
`PermissionAction`, `RiskEstimate`, `ResourceId`, `ToolDescriptor`, `Tool`)
rather than inventing parallel vocabularies. No naming collisions exist
against any other file in `src/`.

**Specification documents updated (addenda only; no existing section
renumbered, no existing normative statement altered):**

- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
  — Status header addendum; new Section 15 ("Task Proposal Intake"); new
  Section 16 ("Agent Run Command Channel"); two new Open Questions items;
  five new Related entries.
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
  — Status header addendum; Section 6's sequence diagram and dependency
  paragraph updated to name the now-closed mechanism; Section 11's
  `SUBMITTED --> REJECTED` note updated; one Open Questions item marked
  resolved (retained, not deleted); four new Related entries.
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
  — Status header addendum; Section 5's "explicit suspend request" and
  "external cancellation request" language annotated with the now-named
  mechanism; Section 10's "Safe suspension" bullet annotated; four new
  Related entries.
- `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` — Status addendum;
  Contract Map rows for Task Proposal and Agent Run Command updated;
  Tool Invocation row updated; the stale "16-event table" reference for
  Agent Runtime events corrected to 17 (audit Finding T-1); Gaps 1, 2, and
  7 marked closed; Gap 3 narrowed (no longer blocking); Gap 8 cross-
  referenced to the new Tool Invocation Binding closure; new Gap 11 added
  (previously evidenced but never catalogued — audit Finding T-2); seven
  new Related entries.
- `docs/architecture/IMPLEMENTATION_GAPS.md` — Gap #32 given a Sprint 1
  contract-closure addendum recording the `ToolInvocationBinding` design
  decision, explicitly not claiming the gap itself is closed.

**Not touched, by deliberate scope decision:**

- `docs/architecture/ARCHITECTURE_DECISIONS.md` — not in the task's list
  of likely targets. Its Future Considerations notes for AD-005/AD-006
  remain accurate as written; a follow-up refresh citing the new
  contracts would be reasonable but is out of this task's scope.
- Every architecture decision listed in the task's instructions is
  preserved unmodified: the Planner still never creates Tasks (Section 6
  of the Planner spec still states this plainly; `TaskProposalIntake` is
  the only new surface, and it is Task-Manager-owned); the Task Manager
  still owns Tasks (`TaskProposalDisposition.Accepted`/`Split`/`Merged`
  each name a `TaskId` but none of the three contract files gives the
  Planner Runtime, or anything else, a path to create one directly); the
  Agent Runtime still never owns Tasks (`AgentRunCommand.taskId` is a
  reference, not a grant of ownership); the Execution Pipeline remains
  the sole execution authority (`ToolInvocationBinding` is documented as
  Execution-Pipeline-only use and does not change `ToolRegistry.resolve`
  or who may call it); the Permission Engine still owns permission
  decisions (`TaskProposal.anticipatedPermissionActions` and
  `AgentRunCommand.permissionScopeReference` are both explicitly labelled
  advisory-only, never a `PermissionDecision`); the Identity Service still
  owns identity (every `PrincipalId` field in all three new files is
  resolved through it, never a local store, per each file's own KDoc);
  Invalid is not Denied and Terminal lifecycle states are final are both
  restated explicitly in `TaskProposalDisposition`'s own KDoc.

## 4. Remaining Open Questions

None of the following block Sprint 1 coding as scoped by
`SPRINT_1_VERTICAL_SLICE_PLAN.md`; each is recorded rather than decided,
consistent with this task's instructions:

- Whether a `Deferred` `TaskProposalDisposition` should have its own
  dedicated Task Event and Planning Event, rather than none.
- Whether `TaskProposalIntake` and `AgentRunCommandChannel`, once
  implemented, should be exposed as Volume 3 interfaces alongside a
  future formal Task Manager Runtime interface.
- Whether a dedicated `planner.session_rejected` Planning Event should
  still be added for `SUBMITTED --> REJECTED`, now that the disposition
  is a named, direct return value rather than an undefined mechanism.
- Whether the long-term Tool invocation fix should instead fold an
  invocable handle directly into `ToolResolution.Resolved` (Blocker 4's
  "cleaner long-term fix," deliberately not chosen here to avoid touching
  an already-tested interface).
- How a proposal-to-proposal `ProposalDependency.OnProposal` should be
  resolved by the Task Manager Runtime at acceptance time (ordering,
  atomicity) — pre-existing open question, unaffected by this closure.
- `docs/architecture/ARCHITECTURE_DECISIONS.md`'s Future Considerations
  for AD-005/AD-006 would benefit from a follow-up refresh citing these
  new contracts, not performed in this pass (out of scope, per Section 3
  above).

## 5. Sprint 1 Coding Readiness

All four blockers `SPRINT_1_VERTICAL_SLICE_PLAN.md` Section 12 named are
now closed at the contract level: `TaskProposal`/`TaskProposalDisposition`/
`TaskProposalIntake`, `AgentRunCommand`/`AgentRunCommandResult`/
`AgentRunCommandChannel`, and `ToolInvocationBinding` each give Sprint 1's
Implementation Units (Section 6 of that plan) a named, field-shaped
target to implement against, where previously none existed. No new
architecture decision was required to close any of them — each is either
a direct restatement of already-approved specification prose (the five
Task Manager dispositions, the four Agent Run commands) or the
narrowest, most conservative, non-breaking option among several the task
itself offered (the Tool invocation binding). No architecture decision
listed in Section 3 above was violated, no direct execution bypass was
introduced, and no source code behaviour changed — every new file adds
only types and interface signatures, with zero implementations.

Ready for Sprint 1 coding: Yes
