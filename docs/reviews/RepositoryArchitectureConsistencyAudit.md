# Repository Architecture Consistency Audit

## Status

This is a **read-only architecture audit**. No specification, architecture
document, ADR, schema, contract, `src/`, or `tests/` file was modified to
produce it. Every finding below is cited to specific text in the reviewed
repository as it exists today. Where this audit found no issue, it says so
explicitly rather than implying completeness by omission.

Reviewed directly, in full: `docs/architecture/ARCHITECTURE_DECISIONS.md`,
`docs/architecture/IMPLEMENTATION_ORDER.md`,
`docs/architecture/INTER_SPECIFICATION_CONTRACTS.md`,
`docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`,
`docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`,
`docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`,
`docs/architecture/IdentityService.md`, `docs/architecture/action-mapping.md`,
`docs/architecture/tool-registry.md`,
`docs/specifications/volume-03-core-interfaces/{PermissionEngine,ExecutionPipeline,ToolRegistry,ResourceRegistry,EventBus,EventType}.md`,
`docs/specifications/volume-02-core-schemas/Task-Schema.md`,
`docs/diagrams/task-lifecycle-state-machine.mmd`,
`src/contracts/{ExecutionRequest,ExecutionLifecycle,PrincipalLifecycle,ToolLifecycle}.kt`,
`docs/adr/ADR-{001,002,003,012,013,017,018,019,021}-*.md`,
`docs/architecture/IMPLEMENTATION_GAPS.md`,
`docs/reviews/{AgentRuntimeSpecificationReview,TaskManagerRuntimeSpecificationReview,ArchitectureDecisionsReview,Phase3ArchitecturePositionReview}.md`.

## 1. Executive Summary

Parker's Phase 3 architecture — Planner Runtime, Task Manager Runtime,
Agent Runtime, and the Trust Framework beneath them (Identity Service,
Permission Engine, Resource Registry, Execution Pipeline, Tool Registry,
EventBus) — is **internally consistent as a complete platform
architecture**. This audit re-derived the cognitive control chain, the
authority model, and all sixteen Architecture Decisions directly from the
specifications' own text rather than from the governance documents'
summaries of themselves, and found no case where a specification
contradicts another specification, contradicts an Architecture Decision,
or creates a second, competing authority for anything the platform depends
on having exactly one authority for.

No architectural contradiction was found. No trust-boundary violation —
Planner-to-execution, Task-Manager-to-tool-execution, Agent-Runtime-direct-
invocation, permission bypass, identity bypass, Memory/World Model
mutation, or hidden/background execution — was found anywhere in the
reviewed documents; every one of these paths is explicitly and
independently closed by at least two specifications stating the same rule
from both sides of the relationship. All sixteen Architecture Decisions
are accurately evidenced by the specifications they cite, including the
two decisions (AD-009's corrected event count, AD-002's four-part chain)
that were the subject of the immediately preceding correction pass.

This audit did find a small number of **documentation consistency issues**
— stale cross-references and un-propagated recommendations from earlier
reviews — none of which are architecture flaws and none of which block
implementation. These are catalogued in Sections 9 and 10 below, separated
clearly from the pre-existing, already-disclosed contract gaps this audit
also re-confirms (Section 9).

**Implementation may proceed.** For the two specifications that have
already completed their own independent review-and-correction cycle (Agent
Runtime, Task Manager Runtime), remaining work is contract completion, not
redesign. The Planner Runtime Specification has not yet completed that
same independent cycle (a fact `ARCHITECTURE_DECISIONS.md` itself already
discloses) and should receive one, per the platform's own established
process, before it is treated as equally settled — this is a process step
already recommended by the existing documents, not a new finding this
audit invents.

## 2. Architecture Overview

Parker's Phase 3 architecture, as specified today, has three layers:

- **Intelligent/coordinating subsystems** — Planner Runtime (proposes),
  Task Manager Runtime (orchestrates), Agent Runtime (performs work within
  a Task) — each with its own independently-tracked lifecycle
  (`PlannerRuntimeSpecification.md` §5, `TaskManagerRuntimeSpecification.md`
  §5, `AgentRuntimeSpecification.md` §5), and each explicitly non-
  overlapping in what it owns.
- **Authority and execution services** — Identity Service (identity),
  Permission Engine (authorisation), Execution Pipeline (execution),
  Tool Registry (tool dispatch), Resource Registry (resource ownership/
  sensitivity), EventBus (event transport) — cross-cutting, consumed
  identically by every layer above, owned by none of them.
- **Reserved future layers** — Memory, World Model, Workflow Runtime,
  Android integration — named as seams in every existing specification's
  Non-Goals and "Relationship to Future Systems" sections, not yet
  specified, not implemented anywhere.

This structure was itself distilled into `ARCHITECTURE_DECISIONS.md`
(sixteen decisions) from the specifications rather than authored first and
imposed on them (`ARCHITECTURE_DECISIONS.md` §5). This audit's job was to
check that the distillation is accurate and that the specifications still
agree with each other and with it, not to re-derive it from nothing.

## 3. Validated Platform Structure

Checking for duplicated responsibilities, conflicting ownership, undefined
ownership, overlapping authority, and circular responsibility:

- **Task ownership.** Exactly one owner: the Task Manager Runtime
  (`TaskManagerRuntimeSpecification.md` §1, §4, "the Task Manager Runtime
  is the sole owner of Task Manager Task state"). The Planner Runtime
  produces Task Proposals only, never a Task (§6). The Agent Runtime
  performs Agent Runs within a Task but never writes its `status`
  (`AgentRuntimeSpecification.md` §5, §11). No document defines a
  competing Task abstraction — checked directly in all three Phase 3
  specifications' Core Concepts and Non-Goals sections.
- **Planning.** Exactly one owner: the Planner Runtime. Chapter 20's
  "Deliberation Service" is not separately specified anywhere in this
  repository; the Planner Runtime Specification's own Plan Decision
  (§4) explicitly absorbs that role pending an open question about
  whether it should split out (§1) — this is a disclosed scope question,
  not an ownership conflict, since nothing else in the repository claims
  to perform deliberation.
- **Execution.** Exactly one path: `ExecutionPipeline.submit`, restated
  identically as the sole path in all three Phase 3 specifications and in
  `ExecutionPipeline.md`'s own normative requirement ("The pipeline MUST
  NOT bypass PermissionEngine").
- **Tool invocation.** Exactly one holder of an invocable `Tool` reference:
  the Execution Pipeline, via `ToolRegistry.resolve`, confirmed
  identically in `tool-registry.md`, `ToolRegistry.md`'s normative
  requirements, and all three Phase 3 specifications' own restatements.
- **Identity.** Exactly one authority: the Identity Service, with
  `updateStatus` as the sole sanctioned mutation path
  (`IdentityService.md`). No Phase 3 document defines its own identity
  store or mutation path.
- **Permission.** Exactly one authority: the Permission Engine, evaluated
  exactly once per `ExecutionRequest` regardless of origin
  (`PermissionEngine.md`; `action-mapping.md`'s "Multiple Actions").
- **Audit/event publication.** Exactly one transport (EventBus), with each
  domain's events produced by exactly one publisher: the Task Manager
  Runtime is stated as "the sole producer of every Task Event" even for
  events that report on Agent Run activity
  (`TaskManagerRuntimeSpecification.md` §10); the Agent Runtime and
  Planner Runtime make the identical claim for their own domains
  (`AgentRuntimeSpecification.md` §9, `PlannerRuntimeSpecification.md`
  §11). No cross-domain publishing overlap was found.

**No duplicated responsibility, conflicting ownership, undefined
ownership, overlapping authority, or circular responsibility was found.**
Every subsystem checked has exactly one clearly defined responsibility,
stated consistently from more than one document where the relationship
spans two specifications.

## 4. Validated Cognitive Control Chain

```text
User Intent / Goal
  -> Planner Runtime
  -> Task Proposal
  -> Task Manager Runtime
  -> Task Manager Task
  -> Agent Runtime
  -> Agent Run
  -> Execution Pipeline
  -> Tool Invocation
  -> Tool Registry / Tool Runtime
```

Every link in this chain is defined exactly once, by exactly one document,
and only ever referenced (never redefined) elsewhere:

- **User Intent / Goal.** Defined once, by `AgentRuntimeSpecification.md`
  §4; the Planner Runtime Specification explicitly "does not redefine
  Goal" and consumes it as-is (`PlannerRuntimeSpecification.md` §4).
- **Planner Runtime -> Task Proposal.** `PlannerRuntimeSpecification.md`
  §6, §13: the Planner Runtime's only output; no operation writes a
  `taskId` or Task `status` directly.
- **Task Proposal -> Task Manager Runtime.** Submitted for the Task
  Manager Runtime's own acceptance decision
  (`PlannerRuntimeSpecification.md` §6); the disposition mechanism itself
  is an open contract gap (Section 9, Gap 1/2 below), not a broken link —
  both documents agree the handoff should happen, only the exact
  mechanism is undefined.
- **Task Manager Runtime -> Task Manager Task.** Sole ownership confirmed
  from three independent directions: the Task Manager Runtime Specification
  itself, the Agent Runtime Specification ("does not define a separate
  'Agent Task' abstraction"), and the Planner Runtime Specification ("The
  Task Manager Runtime decides whether Task Proposals become Task Manager
  Tasks").
- **Task Manager Task -> Agent Runtime -> Agent Run.** The Task Manager
  Runtime "may, at its own discretion, create" an Agent Run
  (`TaskManagerRuntimeSpecification.md` §6); the Planner Runtime never
  creates one directly (`PlannerRuntimeSpecification.md` §7). The named
  object actually passed to trigger that creation is not shaped by either
  document — a disclosed contract gap (Section 9, Gap 7), not a missing or
  contradicted step.
- **Agent Run -> Execution Pipeline.** Every proposed action becomes an
  `ExecutionRequest` submitted through the unchanged Execution Pipeline
  (`AgentRuntimeSpecification.md` §6); implemented and tested today
  (`src/contracts/ExecutionRequest.kt`, `RequestOrigin.AGENT`).
- **Execution Pipeline -> Tool Invocation -> Tool Registry / Tool
  Runtime.** `ExecutionPipeline.submit -> PermissionEngine.evaluate ->
  ToolRegistry.resolve -> Tool.execute` is implemented and tested
  (`DefaultExecutionPipeline`, `InMemoryToolRegistry`), confirmed by direct
  reading of `ExecutionPipeline.md` and `ToolRegistry.md`'s normative
  requirements.

**No step in this chain is missing, duplicated, or bypassed**, and no step
contradicts another document. Two links in the chain (Task Proposal ->
Task Manager Runtime disposition; Task Manager Task -> Agent Run creation
request) are specified only as prose/sequence-diagram description rather
than a named, field-shaped object — these are genuine, already-disclosed
contract gaps (Section 9), not breaks in the chain: every document on both
sides of each gap agrees the link should exist and what it is for, and
disagrees only on what to call the object that would carry it.

## 5. Validated Trust Boundaries

Each of the ten paths named in the audit scope was checked directly
against the reviewed specifications, not inferred from their own claims
about themselves:

| Path checked | Found? | Evidence |
|---|---|---|
| Planner -> Tool execution | **Not found** | `PlannerRuntimeSpecification.md` §3, §13: "no operation that invokes `Tool.execute` or holds an invocable `Tool` reference." |
| Planner -> Task creation | **Not found** | §6, §13: "no operation that creates a Task Manager Task record." |
| Task Manager -> Tool execution | **Not found** | `TaskManagerRuntimeSpecification.md` §3, §7, §12: "never queries `ToolRegistry.resolve` directly and never holds a `Tool` reference." |
| Agent Runtime -> direct Tool invocation | **Not found** | `AgentRuntimeSpecification.md` §6, §11: "An Agent Instance never queries `ToolRegistry.resolve` directly." |
| Execution outside Execution Pipeline | **Not found** | `ExecutionPipeline.md`: "MUST NOT bypass PermissionEngine"; restated as the sole path in all three Phase 3 specifications; `tool-registry.md`: "nothing except the Execution Pipeline ever holds a live `Tool` reference." |
| Identity bypass | **Not found** | Every Principal resolution path (Task Owner, Task Assignee, Agent Identity, Planning Session initiating Principal) routes through the Identity Service; no Phase 3 document defines a local identity store. |
| Permission bypass | **Not found, with one disclosed exception noted below** | Every `ExecutionRequest`-bearing path passes through `PermissionEngine.evaluate` unconditionally in every reviewed document. |
| Memory mutation | **Not found** | All three Context models (Task Context, Agent Context, Planning Context) state explicitly "this is not Memory"; no document defines a read or write path to Chapter 17. |
| World Model mutation | **Not found** | Identical reasoning; all three Context models state explicitly "this is not the World Model." |
| Hidden / background autonomous execution | **Not found** | All three specifications include an explicit "no hidden background execution/planning" safety boundary (`TaskManagerRuntimeSpecification.md` §12, `AgentRuntimeSpecification.md` §11 plus the dedicated "WAITING_FOR_INPUT Trust Boundary" subsection, `PlannerRuntimeSpecification.md` §13). |

**The one disclosed exception**, already named in the specification's own
text rather than found by inference: `TaskManagerRuntimeSpecification.md`
§8 states plainly, in its own words, that cross-Principal Task control
actions (e.g. cancelling another Principal's Task) are "not
Permission-Engine-gated by default" today, and records this as an open
question rather than silently assuming a gate exists. Because the
specification discloses this itself, in its own normative text, it is not
an *accidental* trust-boundary gap this audit is surfacing for the first
time — it is a known, named, deliberately-left-open design question,
already catalogued in `INTER_SPECIFICATION_CONTRACTS.md` §6 (Gap 5) and
`ARCHITECTURE_DECISIONS.md` AD-007's Future Considerations. It is recorded
here because the audit's scope explicitly asked whether this path exists
(it does, by specified design, not by oversight) — not because it
constitutes an undisclosed violation.

**No other path in the list above exists anywhere in the reviewed
documents, disclosed or otherwise.**

## 6. Validated Platform Decisions

All sixteen decisions in `ARCHITECTURE_DECISIONS.md` were checked directly
against the specification text they cite, not against the decision
document's own restatement of that text:

- **AD-001 (Identity First) through AD-008 (Identity Decisions Belong to
  Identity Service).** Each decision's cited evidence was located verbatim
  or near-verbatim in the named source document during this audit's own
  reading (e.g. `TaskManagerRuntimeSpecification.md` §8's "Attribution is
  not authorisation" text matches AD-007's citation exactly). No
  contradiction found.
- **AD-009 (Everything Important Is Auditable).** This audit independently
  recounted the event tables in all three Phase 3 specifications:
  `AgentRuntimeSpecification.md` §9 has **17** rows (AgentCreated through
  AgentCancelled), `TaskManagerRuntimeSpecification.md` §10 has **19**
  rows (TaskCreated through TaskSuperseded), and
  `PlannerRuntimeSpecification.md` §11 has **13** rows
  (planner.session_created through planner.session_cancelled). All three
  counts match `ARCHITECTURE_DECISIONS.md` AD-009 exactly, confirming the
  immediately preceding correction pass (16 -> 17 for Agent Runtime) is
  now accurate. One stale reference to the old, incorrect count of 16
  remains elsewhere in the repository — see Section 10, Finding T-1.
- **AD-010 (Model Independence) through AD-014 (Architecture Before
  Implementation).** Evidence confirmed directly; no contradiction found.
  AD-014's own Future Considerations already discloses that the Planner
  Runtime Specification has not yet received its own dedicated review-
  and-correction pass — this audit confirms that disclosure is still
  accurate as of this reading (no such review exists in `docs/reviews/`
  beyond `Phase3ArchitecturePositionReview.md`, which examined the Planner
  Runtime Specification only as part of a cross-specification check).
- **AD-015 (Invalid Is Not Denied).** Confirmed: `action-mapping.md`,
  `IdentityService.md`, `AgentRuntimeSpecification.md` §10, and
  `TaskManagerRuntimeSpecification.md` §8 all state the identical
  distinction, in each case citing or mirroring `action-mapping.md`'s
  original "Unknown Actions" language. The Planner Runtime Specification's
  `FAILED`/`REJECTED` split (§5) independently applies the same
  distinction without using the same vocabulary, exactly as AD-015's
  Affected Specifications field describes.
- **AD-016 (Terminal Lifecycle States Are Final).** Confirmed directly
  against the actual state machines, not just the prose describing them:
  `ExecutionLifecycleTransitions`, `PrincipalLifecycleTransitions`, and
  `ToolLifecycleTransitions` (`src/contracts/`) all define their terminal
  states with an empty outgoing-transition set in the Kotlin itself, not
  only in prose — `COMPLETED`, `FAILED`, `DENIED`, `DEFERRED`, `CANCELLED`,
  and `EXPIRED` each map to `emptySet()` in `ExecutionLifecycleTransitions`;
  `ARCHIVED` and `REMOVED` do likewise in the other two. The Task Manager
  Task lifecycle (`docs/diagrams/task-lifecycle-state-machine.mmd`), Agent
  Run lifecycle, and Planning Session lifecycle all show the identical
  pattern in their Mermaid diagrams. No resurrection edge exists in any
  lifecycle this audit checked.

**No specification contradicts any of the sixteen Architecture
Decisions.** Every decision's cited evidence was independently confirmed
against the actual document text during this audit, not merely
cross-checked against the decision document's own summary of it.

## 7. Validated Contracts

`INTER_SPECIFICATION_CONTRACTS.md`'s fourteen-row Contract Map was checked
row by row against the specifications it cites:

- Rows marked **Existing schema-backed contract** (`ExecutionRequest`,
  Execution Pipeline -> Tool Registry dispatch, Permission Decision,
  Identity resolution, Resource Registry, EventBus mechanism) were
  confirmed to have corresponding, unchanged Kotlin in `src/contracts/`
  and `src/interfaces/`, consistent with `IMPLEMENTATION_ORDER.md` §2's
  101-test foundation claim.
- Rows marked **Approved specification contract** (Task Manager Task,
  `task.*`/`agent.*`/`planner.*` event sets) were confirmed present with
  the exact structure claimed, with one stale detail found (Section 10,
  Finding T-1: the Agent Events row's event count).
- Rows marked **Proposed contract** or **Open dependency** (Task Proposal
  intake, Agent Run Request, Tool Registry capability consumption by
  Planner/Task Manager) were confirmed to be genuinely undefined in both
  documents they span, not merely undefined in one — i.e. these are real
  two-sided gaps, not a case where one document already has an answer the
  other simply failed to cite.

No row in the Contract Map was found to overstate its own status (e.g. no
row claims "Existing schema-backed" for something this audit could not
find corresponding Kotlin for), and no row was found to understate a
contract that is actually already closed.

## 8. Contract Gap vs. Architecture Flaw vs. Implementation Detail

Applying the distinction the audit scope requires, to every gap this audit
encountered:

- **Contract gaps** (both sides agree a relationship should exist; the
  object or mechanism carrying it is merely unnamed or unshaped): Task
  Proposal intake and disposition (Gaps 1–2 below), the Planner
  `SUBMITTED -> REJECTED` event gap (Gap 3), Agent Run Request/cancellation
  request objects (Gaps 7 and 11), proposal-to-proposal Dependency
  resolution (Gap 10). None of these represent disagreement about
  direction or intent between the specifications involved.
- **Architecture flaws** (a specification's stated rule conflicts with
  another's, or a component's authority overlaps another's): **none
  found** by this audit, anywhere in the reviewed documents.
- **Implementation details** (a decision correctly left open because it
  affects only how a specification is realised in Kotlin, not what the
  architecture requires): the exact cascading-revocation rule (Gap 9), the
  precise Principal-status-change detection mechanism (poll vs. subscribe,
  recorded as an open question in all three Phase 3 specifications), and
  the Tool Registry's `TOOL_AMBIGUOUS` tie-break rule (`tool-registry.md`'s
  own Open Questions). These do not need architectural resolution before
  implementation of the layers that already exist; they need a human
  decision at implementation time, exactly as each source document already
  frames them.

## 9. Remaining Contract Gaps

Restating `INTER_SPECIFICATION_CONTRACTS.md` §6's ten gaps (confirmed
still accurate and still open, one by one, against the current
specification text) plus the one gap `Phase3ArchitecturePositionReview.md`
identified but which has not yet been folded back into that catalogue
(Finding T-2, Section 10):

1. Task Proposal intake contract in Task Manager Runtime — still open.
2. Task Manager response/disposition contract (accept/defer/split/merge/
   reject) back to the Planner Runtime — still open.
3. Planner `SUBMITTED -> REJECTED` event gap (downstream of Gap 2) — still
   open.
4. Identity revocation detection/propagation (`resolve()` suppression,
   `identity.*` events — `IMPLEMENTATION_GAPS.md` #37/#39) — still open,
   affecting all three Phase 3 specifications identically.
5. Permission gating for cross-Principal Task control — still open,
   explicitly disclosed in `TaskManagerRuntimeSpecification.md` §8 (also
   Section 5 of this audit).
6. Deliberation Service vs. Planner's internal Plan Decision boundary —
   still open.
7. Agent Run Request has no named, shaped object — still open.
8. `PermissionEngine.evaluate` not yet wired to resolve identity first
   (`IMPLEMENTATION_GAPS.md` #40) — still open.
9. Exact cascading-revocation rule undecided (`IMPLEMENTATION_GAPS.md`
   #35) — still open, an implementation detail per Section 8 above.
10. Proposal-to-proposal Dependency resolution unspecified — still open.
11. **(Not yet in `INTER_SPECIFICATION_CONTRACTS.md`'s own catalogue, but
    already evidenced and cited elsewhere.)** No named contract for the
    Task Manager Runtime requesting an Agent Run's *cancellation* —
    `Phase3ArchitecturePositionReview.md` §6 identified this as the
    identical asymmetry Gap 7 already names for Agent Run *creation*, and
    `ARCHITECTURE_DECISIONS.md` AD-006's Future Considerations already
    cites it by name ("Phase3ArchitecturePositionReview.md §6, Gap 11").
    `INTER_SPECIFICATION_CONTRACTS.md` §6 itself, however, still lists
    only ten gaps and does not yet contain this eleventh one — see Finding
    T-2, Section 10.

Every one of these eleven items is a **contract gap**, per Section 8's
distinction: in each case, every specification that touches the gap
already agrees the relationship should exist and roughly what it is for;
what is missing is a named, field-shaped mechanism, not agreement.

## 10. Documentation Consistency Findings (Editorial)

These are terminology, cross-reference, and staleness issues — not
architecture flaws, not contract gaps, not trust-boundary issues. None
requires a specification or architecture-decision change; each is a
housekeeping correction to a document that has fallen slightly behind a
document it depends on.

- **T-1 — Stale event count in `INTER_SPECIFICATION_CONTRACTS.md`.**
  Section 3's Contract Map row for "Agent Runtime | Agent Events (`agent.*`)
  | EventBus" states "16-event table," a figure that predates the
  correction pass already applied to `ARCHITECTURE_DECISIONS.md` AD-009
  (16 -> 17). This audit independently recounted
  `AgentRuntimeSpecification.md` §9 and confirmed 17 rows. This single row
  in `INTER_SPECIFICATION_CONTRACTS.md` was not updated when the AD-009
  correction was made elsewhere. **Recommendation:** update this one cell
  from "16-event table" to "17-event table" the next time that document
  receives a maintenance pass; this does not require a review-and-
  correction cycle of its own, since it is a factual count correction, not
  a substantive change.
- **T-2 — Recommendation not yet propagated: Gap 11 missing from
  `INTER_SPECIFICATION_CONTRACTS.md`'s own catalogue.**
  `Phase3ArchitecturePositionReview.md` §9 explicitly recommended "Add Gap
  11 ... to `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` §6 and,
  correspondingly, to the 'Agent Run Request / Agent Run Reference' row of
  that document's §3 Contract Map." This audit confirms that
  recommendation has not yet been carried out: `INTER_SPECIFICATION_CONTRACTS.md`
  §6 still lists only ten gaps. `ARCHITECTURE_DECISIONS.md` already cites
  Gap 11 directly from the review document, so no specification or
  decision is currently inaccurate as a result — but the catalogue
  document that exists specifically so a contributor does not have to
  cross-reference multiple review documents by hand is, on this one point,
  not yet doing that job. **Recommendation:** fold Gap 11 into
  `INTER_SPECIFICATION_CONTRACTS.md` §6 and its Section 3 Contract Map row,
  exactly as already recommended.
- **T-3 — Recommendation not yet propagated: Planner lifecycle
  "naming coincidence" disclaimer.** The same review (§9, recommendation
  2) recommended adding an explicit "Relationship to the Task Manager Task
  and Agent Run Lifecycles" subsection to `PlannerRuntimeSpecification.md`
  §5, mirroring the dedicated subsection `AgentRuntimeSpecification.md` §5
  already has. This audit confirms `PlannerRuntimeSpecification.md` §5
  still has no such subsection as of this reading — it states the
  Planning Session lifecycle is "distinct from the Task Manager Task
  lifecycle and the Agent Run lifecycle, tracked independently of both"
  (§4) but without the dedicated, explicit "coincidence, not shared"
  treatment the Agent Runtime Specification gives the same kind of
  overlap. Not a contradiction — no document claims the three lifecycles
  are shared — but an asymmetry in how explicitly the non-sharing is
  documented. **Recommendation:** this is a natural candidate to fold into
  the Planner Runtime Specification's own future review-and-correction
  pass (Section 11 below), rather than a standalone edit, since it touches
  the same section (§5) that pass would already be examining.
- **T-4 — `IMPLEMENTATION_ORDER.md` predates the Planner Runtime
  Specification's existence and has not been updated to reflect it.**
  `IMPLEMENTATION_ORDER.md` §1 states Parker "has no Planner" and §4's
  Recommended Order table lists "Planner Runtime Specification (Chapter
  20) | **Not yet specified**" as its Order 1 recommendation. Both
  statements were accurate when that document was written, but the
  Planner Runtime Specification now exists
  (`docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`),
  commissioned per that same document's own Order 1 recommendation. This
  is not a contradiction — nothing in `IMPLEMENTATION_ORDER.md` is
  factually wrong about the state of the repository *when it was written*,
  and `IMPLEMENTATION_ORDER.md` §7's own Open Questions already anticipates
  this ("Should this document itself be revisited ... once the Planner
  Runtime Specification exists") — but it does mean a contributor reading
  `IMPLEMENTATION_ORDER.md` today without also checking
  `docs/specifications/volume-06-planner-runtime/` would form a stale
  picture of what has been specified. **Recommendation:** revisit
  `IMPLEMENTATION_ORDER.md` once the Planner Runtime Specification
  completes its own review-and-correction pass, updating Section 2/3's
  "Approved Design Baselines" to include it and Section 4's table to
  reflect World Model as the new Order 1 recommendation — exactly the
  update path §7 already anticipates, not a new process this audit
  invents.
- **T-5 — `action-mapping.md`'s "the Planner" predates, and is not
  precisely reconciled with, the scoped Planner Runtime Specification.**
  `action-mapping.md` (v0.7-alpha, written before any Phase 3 specification
  existed) states "the Planner performs the lookup **before** the
  `ExecutionRequest` is considered `VALIDATED`" and that "the Planner is
  the sole owner of the translation" from proposed-action text to
  `PermissionAction`. Read literally against the now-existing Planner
  Runtime Specification, this does not line up: the actual Planner
  Runtime's Planning Session reaches its own terminal state (`SUBMITTED ->
  COMPLETED`) once a Task Proposal is accepted, well before any Task
  Manager Task or Agent Run — and therefore any `ExecutionRequest` — exists
  to be validated. `PlannerRuntimeSpecification.md` §8 is explicit that its
  own permission-related output is "advisory labelling... not a
  `PermissionDecision`," deferring the authoritative vocabulary lookup to
  "whatever component later constructs the real `ExecutionRequest`" — i.e.
  the Agent Runtime (via an Agent Step) or the Task Manager Runtime (for
  direct execution), not the Planner Runtime itself. No trust boundary is
  crossed either way — nobody's authority changes depending on which
  component is understood to perform the lookup, and the same deterministic
  vocabulary table governs it regardless — but the word "Planner" in
  `action-mapping.md`'s normative "MUST" language and the "Planner Runtime"
  `PlannerRuntimeSpecification.md` defines are not the same actor at the
  point action-mapping.md describes. **Recommendation:** this is a
  candidate for a clarifying note in either document (most naturally
  `action-mapping.md`, since it is the older document making the more
  specific timing claim) — noting that the vocabulary lookup at
  `ExecutionRequest`-validation time is performed by whichever component
  constructs the request, using the same Planner-originated vocabulary
  table the Planner Runtime's own advisory labelling already draws on —
  not a redesign of either document's actual behaviour.

**None of T-1 through T-5 blocks implementation of anything already
approved.** All five are corrections to cross-references and staleness,
not to substantive rules.

## 11. Architectural Contradictions

**None found.** This audit specifically checked, independently of the
prior reviews' own claims to have checked the same things:

- Disagreement between the Agent Runtime and Task Manager Runtime
  specifications about which side owns a Task Status transition — none;
  both state the identical "Agent Run state does not directly mutate Task
  state" rule from their own sides.
- Disagreement about Goal's definition or origin between the Planner
  Runtime and Agent Runtime specifications — none; the Planner Runtime
  Specification explicitly consumes, and does not redefine, the Agent
  Runtime's Goal.
- Inconsistent reuse of existing enums (`RequestOrigin`, `RequestPriority`,
  `RiskEstimate`) across the Task Manager Runtime and Planner Runtime
  specifications — none; both reuse the same enums for the same
  underlying facts.
- Any specification asserting a rule that a sibling specification's own
  stated rule would prevent — none found.
- Any Architecture Decision contradicted by a specification it cites as
  evidence — none found (Section 6).
- Any lifecycle state machine permitting resurrection from a terminal
  state, contradicting AD-016 — none found, checked directly in the Kotlin
  transition tables, not only in prose (Section 6).

## 12. Terminology Issues

- **Deliberate, disclosed naming coincidences (not issues).** The Agent
  Run lifecycle and Task Manager Task lifecycle share several state-name
  tokens (`CREATED`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`);
  `AgentRuntimeSpecification.md` §5 explicitly labels this "a naming
  coincidence between two deliberately separate state machines, not a
  shared one." This is correct handling, not a problem.
- **Asymmetric disclosure of the same coincidence (Finding T-3, Section
  10).** The Planning Session lifecycle shares an equivalent set of
  tokens with both other lifecycles but lacks the Agent Runtime
  Specification's equally explicit dedicated subsection. Not a
  contradiction; an editorial asymmetry, already recommended for closure
  by a prior review (Section 10).
- **"The Planner" attribution gap between `action-mapping.md` and
  `PlannerRuntimeSpecification.md` (Finding T-5, Section 10).** Already
  detailed above; a terminology/attribution precision issue, not a
  disagreement about behaviour.
- **No duplicate concepts, undefined concepts, or competing terminology
  found beyond the two items above.** Task Context, Agent Context, and
  Planning Context are each explicitly scoped, non-overlapping, and
  cross-referenced correctly by ID rather than by copied state
  (`TaskManagerRuntimeSpecification.md` §9, `AgentRuntimeSpecification.md`
  §8, `PlannerRuntimeSpecification.md` §9, checked directly). Task
  Constraint, Agent Policy, and Planner Policy are three distinct,
  correctly-scoped narrowing mechanisms, not the same concept under three
  names — each is scoped to its own runtime and explicitly stated never to
  expand authority.

## 13. Dependency Issues

Checking for circular dependencies, layer violations, cross-layer
ownership, and hidden dependencies:

- **Directed architecture confirmed.** Planner -> Task Manager -> Agent
  Runtime -> Execution Pipeline -> Tool Registry is a strict directed
  chain (`IMPLEMENTATION_ORDER.md` §5, `INTER_SPECIFICATION_CONTRACTS.md`
  §4, both confirmed against the specifications' own text, not merely
  against each other). No specification defines a dependency running
  backward along this chain (e.g. the Execution Pipeline does not depend
  on the Task Manager Runtime for anything).
- **No circular dependency found.** Identity Service, Permission Engine,
  Resource Registry, and EventBus are consumed by every orchestration
  layer and depend on none of them — confirmed by checking that none of
  the four cross-cutting services' own specifications reference a Task,
  Agent Run, or Planning Session as something they depend on to function.
- **No layer violation found.** No orchestration-layer document (Planner,
  Task Manager, Agent Runtime) claims authority reserved for an authority-
  layer service (Identity, Permission), and no authority-layer document
  claims to orchestrate.
- **No cross-layer ownership found.** Every concept (Task, Agent Run,
  Planning Session, Principal, Resource, Tool, Event) has exactly one
  owning document, confirmed in Section 3.
- **One hidden dependency, already self-disclosed rather than actually
  hidden:** the Planner Runtime Specification's Task Proposal creates a
  dependency on a Task Manager Runtime intake mechanism that does not yet
  exist (Section 9, Gap 1) — this is disclosed explicitly in the Planner
  Runtime Specification's own Status header ("This document introduces
  one new boundary object... that the existing Task Manager Runtime
  Specification does not yet define an intake operation... for"), so it is
  a known, named dependency, not one this audit discovered by inference.

## 14. Governance Assessment

`ARCHITECTURE_DECISIONS.md` §5's governance hierarchy (Architecture
Decisions -> Specifications -> Inter-specification Contracts ->
Implementation) is confirmed consistent with how the repository's history
actually developed: the sixteen decisions were distilled from
already-existing, already-reviewed specifications (as that section's own
corrected text now states), not authored first and imposed on them. This
audit did not find any case where a specification was written or corrected
*to match* an Architecture Decision rather than the reverse — consistent
with the document's own honest framing of itself.

The transparency note `ARCHITECTURE_DECISIONS.md` §1 already carries
(disclosing that AD-002, AD-005, AD-010, AD-011, and AD-012 partly rely on
the not-yet-independently-reviewed Planner Runtime Specification) is
confirmed still accurate: no standalone `PlannerRuntimeSpecificationReview.md`
exists in `docs/reviews/` as of this audit. `Phase3ArchitecturePositionReview.md`
examined the Planner Runtime Specification only as one input to a
cross-specification architecture check, not as a standalone internal-
consistency review of that document alone — the same distinction
`ARCHITECTURE_DECISIONS.md` §1 already draws.

**Governance is sound.** The one gap in governance practice this audit
found is process, not structure: two recommendations from
`Phase3ArchitecturePositionReview.md` (Findings T-2 and T-3, Section 10)
have not yet been carried out, even though nothing in the repository
disputes them. This is a backlog item, not a defect in the governance
model itself.

## 15. Implementation Readiness

- **Already safe to build against:** the Trust Framework foundation
  (Identity Service, Permission Engine interface, Execution Pipeline, Tool
  Registry, Resource Registry, EventBus) — already implemented and tested
  (101 tests, per `IMPLEMENTATION_ORDER.md` §2), unaffected by anything
  this audit reviewed.
- **Ready for its own implementation phase, pending only contract
  completion, not redesign:** the Agent Runtime Specification and the Task
  Manager Runtime Specification. Both have already completed an
  independent review-and-correction cycle (`docs/reviews/AgentRuntimeSpecificationReview.md`,
  `docs/reviews/TaskManagerRuntimeSpecificationReview.md`, both followed by
  a correction pass), and this audit found no new architectural issue in
  either beyond the already-catalogued, already-disclosed contract gaps
  (Section 9). Implementing either today would mean implementing against a
  design that is internally consistent, with the caveat that Task
  Proposal intake (Gap 1/2) will need to be added to the Task Manager
  Runtime's own eventual Kotlin surface once the Planner Runtime
  Specification's disposition mechanism is defined.
- **Not yet ready for its own implementation phase:** the Planner Runtime
  Specification. Not because this audit found an architectural problem in
  it — none was found — but because `IMPLEMENTATION_ORDER.md` §6's own
  rule ("Do not implement Planner before the Planner Runtime Specification
  is approved... the same process already applied to both existing
  baselines") and `ARCHITECTURE_DECISIONS.md` AD-014's own precedent both
  require a specification to complete its independent review-and-
  correction cycle before implementation, and the Planner Runtime
  Specification has not yet done so. This is a process gate already
  established by the platform's own documents, not a new requirement this
  audit adds.
- **Remaining work is overwhelmingly contract completion, not
  redesign.** Of the eleven items in Section 9, none requires changing an
  existing specification's stated rules — each requires *adding* a
  currently-missing operation, event, or named object to close a
  handoff both sides already agree should exist.

## 16. Recommendations

These are documentation and process recommendations only; none proposes
implementation or alters existing architecture.

1. Give the Planner Runtime Specification its own independent
   review-and-correction pass, mirroring the process already applied to
   the Agent Runtime and Task Manager Runtime Specifications, before it is
   treated as an equally settled design baseline. While doing so, fold in
   Finding T-3 (the "naming coincidence" disclaimer subsection) as part of
   that same pass rather than as a separate edit.
2. Correct Finding T-1 (the stale "16-event table" reference in
   `INTER_SPECIFICATION_CONTRACTS.md` §3) the next time that document is
   touched — a one-line factual correction, not a review-triggering
   change.
3. Fold Gap 11 (Finding T-2) into `INTER_SPECIFICATION_CONTRACTS.md` §6 and
   its Section 3 Contract Map, exactly as `Phase3ArchitecturePositionReview.md`
   §9 already recommended.
4. Add a short clarifying note resolving Finding T-5 (the "Planner"
   attribution gap between `action-mapping.md` and
   `PlannerRuntimeSpecification.md`), most naturally in `action-mapping.md`
   itself since it makes the more specific timing claim.
5. Once the Planner Runtime Specification completes recommendation 1
   above, revisit `IMPLEMENTATION_ORDER.md` (Finding T-4) to reflect its
   existence and to re-derive the next recommended specification order,
   exactly as that document's own Open Questions section already
   anticipates.
6. Continue closing Section 9's contract gaps through the same
   review-and-correction-pass process already established, rather than
   resolving any of them silently inside whichever specification is
   drafted next (World Model, per the current recommended order) —
   restating `IMPLEMENTATION_ORDER.md` §6's own rule, not adding a new one.

## 17. Final Assessment

**Architecture Validated With Minor Contract Gaps.**

Every subsystem checked has exactly one clearly defined responsibility. The
complete cognitive control chain from User Intent through Tool Invocation
is defined without a missing, duplicated, or bypassed step. No trust
boundary — Planner-to-execution, Task-Manager-to-tool-execution,
direct-Tool-invocation, execution-outside-the-pipeline, identity bypass,
permission bypass, Memory/World Model mutation, or hidden autonomous
execution — was found open anywhere in the reviewed documents. All sixteen
Architecture Decisions are accurately evidenced by the specifications they
cite. No architectural contradiction was found anywhere in this audit's
scope.

The gaps that remain — eleven contract gaps (Section 9) and five
documentation-consistency findings (Section 10) — are exactly that: gaps
in contract completeness and cross-reference upkeep, not disagreements
about direction, not competing authorities, and not trust-boundary
failures. Every one of them was already disclosed, in the specifications'
own words, before this audit began; this audit's contribution is
confirming each is still accurate today and that none has quietly become
something worse.

Implementation may proceed on the already-reviewed-and-corrected Agent
Runtime and Task Manager Runtime Specifications. The Planner Runtime
Specification should complete its own independent review-and-correction
cycle — a process step, not an architectural correction — before it is
treated as equally settled.

## Related

- `docs/architecture/ARCHITECTURE_DECISIONS.md`
- `docs/architecture/IMPLEMENTATION_ORDER.md`
- `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md`
- `docs/reviews/Phase3ArchitecturePositionReview.md`
- `docs/reviews/AgentRuntimeSpecificationReview.md`
- `docs/reviews/TaskManagerRuntimeSpecificationReview.md`
- `docs/reviews/ArchitectureDecisionsReview.md`
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
- `docs/architecture/IdentityService.md`
- `docs/architecture/action-mapping.md`
- `docs/architecture/tool-registry.md`
- `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
- `docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`
- `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`
- `docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`
- `docs/specifications/volume-03-core-interfaces/EventBus.md`
- `docs/specifications/volume-03-core-interfaces/EventType.md`
- `docs/specifications/volume-02-core-schemas/Task-Schema.md`
- `docs/diagrams/task-lifecycle-state-machine.mmd`
- `src/contracts/ExecutionRequest.kt`
- `src/contracts/ExecutionLifecycle.kt`
- `src/contracts/PrincipalLifecycle.kt`
- `src/contracts/ToolLifecycle.kt`
- `docs/architecture/IMPLEMENTATION_GAPS.md`
- `docs/adr/ADR-001-models-never-execute-tools.md`
- `docs/adr/ADR-002-memory-context-world-model-separation.md`
- `docs/adr/ADR-003-single-execution-pipeline.md`
- `docs/adr/ADR-012-task-and-workflow-separation.md`
- `docs/adr/ADR-018-immutable-execution-requests.md`
