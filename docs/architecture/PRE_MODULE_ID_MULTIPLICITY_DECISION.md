# Pre-Module Readiness Unit 2 — ID Multiplicity Decision

## Status

Design decision, not yet implemented. This document decides the disposition
of `docs/architecture/IMPLEMENTATION_GAPS.md` #48; the implementation unit
that follows it implements only what this document authorises.

## Purpose

`InMemoryAgentRuntime.start()` mints `AgentRunId("run-for-${taskId}")` and
rejects a second `START` for the same Task
(`src/runtime/InMemoryAgentRuntime.kt:143`, `:158-161`).
`InMemoryPlannerRuntime.plan()` mints
`TaskProposalId("${planningSessionId}-proposal-1")`
(`src/runtime/InMemoryPlannerRuntime.kt:268`) and rejects a second `plan()`
call for the same Planning Session
(`src/runtime/InMemoryPlannerRuntime.kt:174-177`). Both implementations
therefore make it structurally impossible to produce more than one Agent
Run per Task, or more than one Task Proposal per Planning Session —
gap #48, surfaced by the independent architecture audit
(`docs/reviews/ARCHITECTURE_V2_INDEPENDENT_AUDIT_TRIAGE.md`, Finding 1).

Both governing specifications describe the wider concept as open:

- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
  line 275: "A Task MAY have zero, one, or many Agent Run References over
  its lifetime."
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
  line 152: "A Planning Session may decompose a Goal into one or more Task
  Proposals," and line 393: "SUBMITTED — one or more Task Proposals... have
  been submitted to the Task Manager."

This document decides whether that gap between specification and
implementation should be closed by removing the cap now (Option A) or by
formally recording the cap as the current platform phase's deliberate,
documented constraint (Option B) — and, either way, settles ID-generation
strategy, contract impact, and required test coverage precisely enough
that the following implementation unit does not have to invent anything.

## Review

Reviewed, in authority order:

1. `docs/architecture/parker-constitution.md`
2. `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-004 (Task Manager
   Owns Canonical Tasks), AD-006 (Agent Runtime Never Owns Tasks)
3. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001)
4. `docs/reviews/ARCHITECTURE_V2_INDEPENDENT_AUDIT_TRIAGE.md` (Finding 1,
   the finding this document resolves) and
   `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md` (the "100,000-line
   test" framing applied throughout this repository's recent review
   history)
5. `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
   (Sections 6, 7)
6. `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
   (Sections 5, 6)
7. `docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md` (Section 7, "Error
   Handling" — retry-via-a-different-proposed-action is explicitly named
   Planner-level, out of Agent Runtime's own scope)
8. `src/runtime/InMemoryAgentRuntime.kt`, `src/runtime/InMemoryPlannerRuntime.kt`,
   `src/runtime/InMemoryMemoryStore.kt` (the existing `MemoryId` sequence
   precedent, consulted for Section 5 below), `tests/runtime/InMemoryAgentRuntimeTest.kt`,
   `tests/runtime/InMemoryPlannerRuntimeTest.kt`

No architecture was invented where an existing document already answers
the question.

---

## 1. Is There a Concrete Consumer Today?

Before deciding how to support multiplicity, this document checks whether
anything in this repository currently needs it. It does not.

- **A second Agent Run per Task** would be needed by retry logic (attempt
  a Task again after a failed Agent Run) or by a Workflow Engine
  re-attempting a Task. Retry-via-a-different-attempt is explicitly named
  as out of Agent Runtime's own scope in
  `MULTI_STEP_AGENT_RUN_DESIGN.md` Section 7 ("continuing via a different
  proposed action is retry logic this design does not implement"), and no
  Workflow Engine exists (`docs/architecture/00-index.md`'s own "Not yet
  specified" list names Workflow Runtime, Chapter 38).
- **More than one Task Proposal per Planning Session** would be needed by
  "Multi-agent planning" or "Resource optimisation" (decomposing one Goal
  into several concurrently-submitted proposals) — both are named and
  explicitly excluded from `InMemoryPlannerRuntime`'s own scope in its
  class-level KDoc ("out of this Unit's scope (no 'Multi-agent
  planning'/'Resource optimisation')").
- No test, no calling code, and no other runtime subsystem in this
  repository constructs, expects, or depends on more than one `AgentRun`
  per `TaskId`, or more than one `TaskProposal` per `PlanningSessionId`.

There is therefore no present consumer whose behaviour Option A (removing
the cap now) would serve. Building general multiplicity support ahead of
a real consumer is exactly the kind of speculative generality
`docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`'s "100,000-line test"
exists to catch: would writing a general multi-Agent-Run/multi-Proposal
mechanism now, with no concrete caller to validate its shape against,
still look like a good decision after 100,000 more lines? No — it would
look like an unvalidated abstraction carried for years before its first
real user arrives, at which point that user's actual requirements would
likely demand a different shape than whatever was guessed here.

## 2. Decision

**Option B: formally constrain the current platform phase to one Agent
Run per Task and one Task Proposal per Planning Session. Multiplicity is
DEFERRED, not prohibited, and not implemented now.**

"Deferred" (not "prohibited") because the two governing specifications
already, and correctly, reserve the wider multiplicity — this document
does not narrow `TaskManagerRuntimeSpecification.md` Section 6/7 or
`PlannerRuntimeSpecification.md` Section 5/6's own language, and does not
propose amending either. It formally records, for the current
implementation only, that:

- `InMemoryAgentRuntime` supports exactly one Agent Run per Task.
- `InMemoryPlannerRuntime` supports exactly one Task Proposal per Planning
  Session.
- Both caps are deliberate, reviewed, and load-bearing for the current
  platform phase — not an accidental side effect of an ID-minting
  shortcut, which is what the independent audit found and gap #48
  recorded.

This satisfies gap #48's own finding directly: the finding was never that
one-per-parent is wrong today, only that it was undisclosed as a decision.
Recording it as a decision closes that finding without inventing
capability no part of this repository yet needs.

### 2.1 Why not Option A

Removing the cap now would require, at minimum: a decision on Task
Manager Runtime's own behaviour when more than one Agent Run Reference
exists for a Task (`docs/architecture/IMPLEMENTATION_GAPS.md` item #42's
own text already flags this exact question as "explicitly out of scope");
a decision on how `InMemoryPlannerRuntime` selects, orders, or
concurrently submits more than one Task Proposal from a single
`PlanDecisionResult.Selected` outcome (today's algorithm selects exactly
one winning `PlanCandidate`); and a decision on how a caller distinguishes
between concurrent Agent Runs for the same Task in `AgentStepContext` or
equivalent. None of these are settled anywhere in this repository. Making
the ID scheme multiplicity-capable while leaving all three open would
produce a mechanism nothing exercises correctly, which is worse than
today's honest, disclosed single-instance behaviour.

## 3. ID Generation Strategy

**No change now.** Both `AgentRunId("run-for-${taskId}")` and
`TaskProposalId("${planningSessionId}-proposal-1")` remain exactly as
implemented. Under Option B, a deterministic, single, parent-derived ID
is the *correct* shape for "exactly one child per parent" — it needs no
counter, no generated randomness, and is directly greppable in logs and
tests. Changing it now, for a multiplicity this document is deliberately
not implementing, would be motion without purpose.

**For when multiplicity is eventually implemented** (recorded here so the
future unit that does so does not have to re-derive this from nothing):
a per-parent **monotonic counter/sequence** suffix (e.g.
`"run-for-<taskId>-<n>"`, `"<planningSessionId>-proposal-<n>"`), not a
generated/random identifier and not a caller-supplied one.

- A counter preserves the determinism this repository consistently
  prefers for testability (every existing ID scheme in this codebase --
  `AgentRunId`, `TaskProposalId`, and `MemoryId`'s own
  `"memory-${nextSequence++}"` scheme in `InMemoryMemoryStore.kt` -- is
  either fully deterministic or a simple incrementing sequence; none uses
  a random UUID for a primary record identifier).
- A caller-supplied ID would be a genuine contract change (a new
  parameter on `AgentRunCommand.START` or `PlanningRequest`) and would
  hand ID-uniqueness responsibility to a caller this repository has never
  trusted with it -- every other runtime-minted identifier in this
  codebase is minted by the runtime that owns the record, never supplied
  by the caller requesting it.
- This is guidance for a future unit's own Contract Design pass, not an
  authorisation to implement it now.

## 4. Public Contracts

**No public contract changes.** `AgentRunId`, `TaskProposalId`,
`AgentRunCommand`, and `PlanningRequest` are unchanged in shape. This
document authorises no new field, type, or interface.

## 5. What Tests Must Prove

Both caps already have existing, passing test coverage that this document
confirms is sufficient and correctly targeted, and does not need to be
replaced:

- `tests/runtime/InMemoryAgentRuntimeTest.kt`: "resubmitting START for the
  same taskId is rejected as caller misuse."
- `tests/runtime/InMemoryPlannerRuntimeTest.kt`: "resubmitting the same
  planningSessionId is rejected as caller misuse."

What is missing, and what the implementation unit must add, is a test-level
record that this behaviour is a *decided* constraint, not an accident:

- Both existing tests' failure messages should be checked for language
  that ties the rejection to this deliberate, documented constraint
  (updated exception message content, not new behaviour).
- No new multiplicity-supporting behaviour is tested, because none is
  implemented.

## 6. What the Implementation Unit May Do

Authorised:

- Update the exception messages `InMemoryAgentRuntime.start` and
  `InMemoryPlannerRuntime.plan` already throw on a duplicate Task/Planning
  Session, so each explicitly states that single-instance-per-parent is a
  deliberate, current-phase constraint recorded in this document and in
  `IMPLEMENTATION_GAPS.md` #48 -- not a change to when either exception is
  thrown, or to which exception type is thrown.
- Update KDoc on `InMemoryAgentRuntime.start`, `InMemoryPlannerRuntime.plan`,
  and the two companion-object/constant declarations they mint IDs from,
  to cite this decision.
- Tighten the two existing tests named in Section 5 to assert on the
  updated message content.
- Update `IMPLEMENTATION_GAPS.md` #48 to reflect this decision (formally
  constrained, not closed as "fixed," since no defect existed -- only an
  undisclosed decision).
- Update `IMPLEMENTATION_HISTORY.md` with a new unit entry.

Not authorised:

- Any new `AgentRunId`/`TaskProposalId` generation mechanism (counters,
  UUIDs, or caller-supplied values).
- Any change to `AgentRunCommand`, `PlanningRequest`, or any other public
  contract.
- Any retry, forking, or multi-instance orchestration logic in either
  runtime.
- Any module access of any kind.
- Any change to `EventBus`, Memory Runtime, or World Model Runtime.

## 7. Acceptance Criteria

- Gap #48 is resolved as "formally constrained, deferred," not "closed as
  fixed" -- no defect existed to fix.
- Architecture (the two specifications' own "zero, one, or many"/"one or
  more" language) and implementation (exactly one, for now, by explicit
  decision) no longer disagree by omission -- the disagreement is now a
  disclosed, reasoned, current-phase decision.
- No Kotlin behaviour changes: the same two operations that throw
  `IllegalStateException` today continue to do so, unchanged, for the
  same triggering condition.
- No public contract changes.
- No retry, forking, or module access is introduced.
- The two existing tests continue to pass, updated only to check
  strengthened message content.

---

## Conclusion

**The implementation unit that follows this document may proceed,
implementing exactly Section 6's "Authorised" list and nothing else.**
Gap #48 is closed not by adding capability nothing in this repository yet
needs, but by converting an undisclosed implementation shortcut into a
disclosed, reasoned architectural decision -- consistent with how gap #49
(Planner Runtime publisher identity, Pre-Module Readiness Unit 1) was
closed by disclosure and correction rather than by speculative redesign,
and consistent with every "100,000-line test" judgment already recorded
in `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`.
