# Phase 3 Architecture Position Review

## Status

Review-only. This document does not modify any existing specification,
`src/`, or `tests/`. It evaluates the position stated below against the
current content of the documents listed, as committed, and reaches an
independent conclusion. It proposes no implementation and alters no
architecture.

Reviewed against: `docs/architecture/IMPLEMENTATION_ORDER.md`,
`docs/architecture/INTER_SPECIFICATION_CONTRACTS.md`,
`docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`,
`docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`,
`docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`,
`docs/architecture/IdentityService.md`,
`docs/specifications/volume-02-core-schemas/Task-Schema.md`, ADR-012,
`src/contracts/ExecutionRequest.kt`,
`docs/specifications/volume-03-core-interfaces/ToolRegistry.md`,
`docs/architecture/tool-registry.md`,
`docs/specifications/volume-03-core-interfaces/EventType.md`,
`docs/architecture/action-mapping.md`,
`docs/specifications/volume-03-core-interfaces/PermissionEngine.md`,
`docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`,
`docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`,
`docs/specifications/volume-03-core-interfaces/EventBus.md`, and
`docs/architecture/IMPLEMENTATION_GAPS.md`.

## 1. Executive Summary

The position under review is:

> "Parker now has a coherent Phase 3 cognitive control chain: Planner
> Runtime → Task Manager Runtime → Agent Runtime → Execution Pipeline →
> Tool Registry / Tool Runtime, with Identity Service, Permission Engine,
> Resource Registry, and EventBus acting as cross-cutting services. The
> remaining gaps are inter-specification contract gaps, not fundamental
> architectural failures."

This review finds the position **validated**. Every component boundary
the position depends on — the Planner stopping at Task Proposal, the
Task Manager remaining the canonical Task owner, the Agent Runtime never
owning Tasks or bypassing the Task Manager, the Execution Pipeline
remaining the sole execution path, and Identity Service/Permission
Engine/Resource Registry/EventBus operating as cross-cutting rather than
subsystem-owned services — is independently confirmed by direct,
citable text in the specifications themselves, stated consistently from
both sides of every relationship (e.g. the Task Manager Runtime
Specification and the Agent Runtime Specification each independently
state the same non-ownership boundary). No architectural contradiction
was found. No accidental implication of direct tool execution, direct
Memory or World Model mutation, permission bypass, identity bypass, or
hidden autonomous execution was found anywhere in the reviewed
documents. Every remaining gap this review identified — including one
gap not previously catalogued in
`docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` — is a missing or
incomplete *contract definition* (an intake operation, a response
mechanism, a named request object) between two specifications that
already agree on the direction and shape of their relationship, not a
disagreement between them.

## 2. Validated Position

Restating the position's three claims separately, each assessed:

1. **"Parker has a coherent Phase 3 cognitive control chain: Planner
   Runtime → Task Manager Runtime → Agent Runtime → Execution Pipeline →
   Tool Registry / Tool Runtime."** Validated at the specification level.
   All three Phase 3 documents (Planner Runtime §1, Task Manager Runtime
   §1, Agent Runtime §1) state the identical chain using the identical
   names, and `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` §4
   independently restates it a fourth time from a coordination-document
   perspective. "Coherent" here means internally consistent and
   non-contradictory at the design level — it does not mean operational:
   none of the three Phase 3 specifications has corresponding Kotlin yet
   (Section 3, and every specification's own Status header, are explicit
   about this).
2. **"Identity Service, Permission Engine, Resource Registry, and
   EventBus act as cross-cutting services."** Validated — see Section 5.
3. **"The remaining gaps are inter-specification contract gaps, not
   fundamental architectural failures."** Validated — see Section 6 and
   Section 7. This review found one additional contract gap beyond those
   `INTER_SPECIFICATION_CONTRACTS.md` already catalogues (Section 6, Gap
   11), and one minor terminology-consistency observation (Section 7),
   neither of which changes this conclusion.

## 3. Evidence Supporting the Position

This section answers review questions 1–7 directly.

**1. Does the Planner correctly stop at Task Proposal?** Yes.
`PlannerRuntimeSpecification.md` §6 ("The Planner Runtime does not create
Tasks directly... There is no operation by which the Planner Runtime
writes a `taskId`, a `status`, or any other Task Manager Task field
itself") and §13 ("No direct Task creation... No direct Task status
mutation") both state this without qualification. §3 (Non-Goals)
excludes Task Manager implementation entirely.

**2. Does the Task Manager remain canonical owner of Tasks?** Yes, and
confirmed from three independent directions. `TaskManagerRuntimeSpecification.md`
§1, §2, and §4 treat `Task.schema.json`/Chapter 37/ADR-012 as canonical
and state the Task Manager Runtime is "the sole owner of Task Manager
Task state." `AgentRuntimeSpecification.md` §4 independently states "The
Agent Runtime does not define a separate 'Agent Task' abstraction."
`PlannerRuntimeSpecification.md` §6 independently states "The Task
Manager Runtime decides whether Task Proposals become Task Manager
Tasks." All three documents agree without any document needing to defer
to or restate another's authority to reach that agreement.

**3. Does the Agent Runtime avoid owning Tasks or bypassing Task
Manager?** Yes. `AgentRuntimeSpecification.md` §5 ("Relationship to the
Task Manager Task Lifecycle") states the Agent Run lifecycle and Task
Manager Task lifecycle "are tracked independently, with independent
transition rules ... MUST NOT be inferred from one another." §11 (Safety
Boundaries) restates: "An Agent Instance never writes a Task's `status`
directly." `TaskManagerRuntimeSpecification.md` §6 states the reciprocal
rule from its own side ("Agent Run state does not directly mutate Task
state"). Neither document's rule depends on the other for correctness —
each is independently enforceable.

**4. Does the Execution Pipeline remain the only path to tool
execution?** Yes, stated identically in every reviewed specification.
`AgentRuntimeSpecification.md` §6, §11; `TaskManagerRuntimeSpecification.md`
§7, §12; `PlannerRuntimeSpecification.md` §3, §8, §13 all state no
component holds an invocable `Tool` reference or calls
`ToolRegistry.resolve` itself. `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`
independently confirms `resolve` "is intended to be called only by the
Execution Pipeline" and "MUST only return a Tool in `ENABLED` state."
ADR-003 (Single Execution Pipeline) is cited by name in all three Phase 3
documents as the governing rule, not reinterpreted by any of them.

**5. Are Identity Service and Permission Engine preserved as authorities
rather than optional helpers?** Yes, architecturally. No reviewed
document defines a path around either service. `IdentityService.md`
states `updateStatus` is "the only sanctioned way to move a Principal
through its lifecycle" and every Phase 3 document treats an unresolvable
Principal as invalid, not denied, consistent with `Principal.md`.
`PermissionEngine.md`'s "Every ExecutionRequest MUST be evaluated before
execution" is unconditionally restated by both
`TaskManagerRuntimeSpecification.md` §7 and `AgentRuntimeSpecification.md`
§6. The known incompleteness here — `IdentityService.resolve()` not yet
suppressing non-Active Principals (`IMPLEMENTATION_GAPS.md` #37),
`identity.*` events not yet published (#39), and `PermissionEngine.evaluate`
not yet wired to resolve identity first (#40) — is a gap in how
thoroughly the authority is *enforced today*, not a gap in whether the
architecture *treats them as the authority*. Every document that depends
on these two services treats them as non-optional and explicitly records
the enforcement gap as a dependency rather than routing around it
(Section 6 restates this distinction).

**6. Are Resource Registry and EventBus correctly cross-cutting rather
than owned by one subsystem?** Yes. `docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`
is a standalone Volume 3 interface with no ownership claim by Planner,
Task Manager, or Agent Runtime; all three consume it identically through
their own Context models' "Resource references" category (Planner
Runtime §9, Task Manager Runtime §9, Agent Runtime §8), each treating it
as an external authority, not something any one of them defines or
extends. `EventBus.md` is likewise standalone; `task.*`, `agent.*`, and
`planner.*` are three of several namespaced domains it carries
(alongside `execution.*`, `permission.*`, `identity.*`), with no special
status given to any one domain's producer over the bus itself.
`docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` §4 independently
diagrams both as cross-cutting, dotted-line dependencies distinct from
the solid execution path — this review's own reading of the underlying
specifications confirms that diagram is accurate, not just asserted.

**7. Does the current architecture define a coherent cognitive control
chain?** Yes, at the specification level, per point 1 of Section 2 above.
Every link in the chain — Goal, Planning Session, Task Proposal, Task
Manager Task, Agent Run, Execution Pipeline — is defined exactly once,
by exactly one document, and referenced (never redefined) by every other
document that mentions it. No document in this review renames or
reinterprets a concept another document already owns.

## 4. Confirmed Component Responsibilities

- **Planner Runtime.** Turns a Goal, Planning Context, and Constraints
  into Task Proposals; never creates a Task, never executes an Agent
  Run, never invokes a Tool, never evaluates permission
  (`PlannerRuntimeSpecification.md` §1, §3, §6, §7, §13).
- **Task Manager Runtime.** Sole owner of the Task Manager Task
  lifecycle; decides whether to accept, defer, split, merge, or reject a
  Task Proposal; coordinates zero, one, or many Agent Runs per Task;
  never executes a Tool directly
  (`TaskManagerRuntimeSpecification.md` §1, §6, §7, §12).
- **Agent Runtime.** Performs Agent Runs and Agent Steps within a Task
  Manager Task (never owning it); every proposed action becomes an
  `ExecutionRequest` submitted through the unchanged Execution Pipeline;
  never resolves a Tool itself
  (`AgentRuntimeSpecification.md` §1, §5, §6, §11).
- **Execution Pipeline.** The sole entry point (`submit`) by which any
  proposed action, from any origin, can have an external effect; enforces
  Permission Engine evaluation before dispatch (`ExecutionPipeline.md`;
  ADR-003).
- **Tool Registry.** Resolves an invocable Tool only for the Execution
  Pipeline's own use; exposes a descriptor-only discovery surface to
  every planning-time consumer (`docs/specifications/volume-03-core-interfaces/ToolRegistry.md`,
  `docs/architecture/tool-registry.md`).

## 5. Confirmed Cross-Cutting Services

- **Identity Service.** Resolves every Principal (Task Owner, Task
  Assignee, Agent Identity, Planning Session initiating Principal)
  identically for every consumer; no consumer maintains its own identity
  store (`IdentityService.md`; all three Phase 3 specifications' §7/§8).
- **Permission Engine.** Evaluates every `ExecutionRequest` exactly once,
  regardless of origin (Task Manager direct, Agent Run, or any other);
  no consumer evaluates permission on its own authority
  (`PermissionEngine.md`; `docs/architecture/action-mapping.md`'s
  "Multiple Actions" rules).
- **Resource Registry.** Resolves every Resource reference identically
  for every consumer; ownership and sensitivity classification are
  defined once, centrally (`ResourceRegistry.md`).
- **EventBus.** Carries every domain's events (`task.*`, `agent.*`,
  `planner.*`, `execution.*`, `permission.*`, `identity.*`) under one
  shared publish/subscribe mechanism, with authentication and
  correlation-ID preservation applied uniformly, not per-domain
  (`EventBus.md`; `EventType.md`'s shared `<domain>.<event>` convention).

## 6. Remaining Contract Gaps

The following restates
`docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` §6's ten catalogued
gaps (not reproduced individually here — see that document), and adds
one gap this review found evidenced but not yet catalogued there.

**Gap 11 (new): No named contract for the Task Manager Runtime
requesting an Agent Run's cancellation, mirroring Gap 7's (Agent Run
Request) missing-creation-contract pattern.** `TaskManagerRuntimeSpecification.md`
§5 ("Cancellation semantics") states that cancelling a Task "MUST cause
the Task Manager Runtime to request cancellation of every Agent Run
Reference ... still active," citing "the Agent Runtime Specification's
own `--> CANCELLED` edge, reachable from every non-terminal Agent Run
state." However, `AgentRuntimeSpecification.md` defines the `CANCELLED`
state as reachable in its lifecycle diagram, but does not define an
operation, event, or other named mechanism by which an external caller
(the Task Manager Runtime) actually triggers that transition — the same
asymmetry Gap 7 already identifies for Agent Run *creation* exists
identically for Agent Run *cancellation*. This is answered directly by
review question 9: **Agent Run Request (creation) is not the only newly
surfaced missing contract between the Task Manager Runtime and Agent
Runtime** — the cancellation-request direction has the identical gap,
evidenced by the same kind of prose-only cross-reference (a sequence
diagram or citation) standing in for a named, field-shaped object or
operation signature in either document.

Both Gap 7 and Gap 11 are the same *kind* of gap: two specifications
that already agree a request must flow from the Task Manager Runtime to
the Agent Runtime, without either specification naming or shaping the
object that request would take. Neither gap represents disagreement
about *whether* the request should happen — only about what it is
formally called and shaped as. This is precisely the "contract gap, not
architectural failure" distinction the position under review draws.

No other gap beyond `INTER_SPECIFICATION_CONTRACTS.md`'s existing ten and
this review's Gap 11 was found between the Task Manager Runtime and
Agent Runtime specifically; the Agent Event set the Task Manager Runtime
subscribes to for *learning* an Agent Run's outcome (`agent.completed`,
`agent.failed`, `agent.cancelled`, `agent.action_denied`,
`agent.action_deferred` — Task Manager Runtime Specification §6) is
already fully named and specified on both sides, so the reverse
(Agent-Runtime-to-Task-Manager) direction of that particular
relationship is not a gap.

## 7. Architectural Contradictions Found

**None found.** This review specifically checked for: disagreement
between the Agent Runtime and Task Manager Runtime specifications about
which side owns a Task Status transition (none — both state the
identical rule); disagreement about Goal's definition or origin between
the Planner Runtime and Agent Runtime specifications (none — Planner
Runtime explicitly consumes, and does not redefine, the Agent Runtime's
Goal); inconsistent reuse of existing enums (`RequestOrigin`,
`RequestPriority`, `RiskEstimate`) across the Task Manager Runtime and
Planner Runtime specifications (none — both reuse the same enums for the
same underlying facts, without redefining them); and any specification
asserting a rule that a sibling specification's own stated rule would
prevent (none found).

**One terminology-consistency observation, not a contradiction.**
`AgentRuntimeSpecification.md` §5 includes a dedicated subsection
("Relationship to the Task Manager Task Lifecycle") explicitly stating
that its lifecycle's overlapping state names with the Task Manager Task
lifecycle (`CREATED`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`) are
"a naming coincidence between two deliberately separate state machines,
not a shared one." `PlannerRuntimeSpecification.md`'s Planning Session
lifecycle (§5) shares an equivalent, or larger, set of state-name tokens
with both other lifecycles (`CREATED`, `COMPLETED`, `FAILED`,
`CANCELLED` with the Task Manager Task lifecycle; `WAITING_FOR_INPUT`
with the Agent Run lifecycle), and states in its Core Concepts (§4) that
the Planning Session lifecycle is "distinct from the Task Manager Task
lifecycle and the Agent Run lifecycle, tracked independently of both,"
but does not include an equivalently explicit, dedicated subsection
disclaiming the shared tokens as coincidental the way the Agent Runtime
Specification does. This is not a contradiction — nothing in any document
asserts the three lifecycles *are* shared — but it is an asymmetry in how
explicitly that non-sharing is documented, worth closing for consistency
(Section 9).

## 8. Trust Boundary Issues Found

This section answers review question 10 directly, checking for any
document accidentally implying each of the six listed behaviours.

- **Direct tool execution by Planner, Task Manager, or Agent Runtime.**
  Not found. Confirmed absent in Section 3, point 4 above.
- **Direct Memory mutation.** Not found. All three Context models (Task
  Context, Agent Context, Planning Context) explicitly state "this is not
  Memory" (Task Manager Runtime Specification §9; Agent Runtime
  Specification §8; Planner Runtime Specification §9), and no document
  defines a read or write path to the Memory Architecture (Chapter 17).
- **Direct World Model mutation.** Not found, by the identical reasoning
  and identical explicit "not the World Model" statements in all three
  Context models.
- **Permission bypass.** No accidental implication found: every
  `ExecutionRequest`-bearing path in every specification passes through
  `PermissionEngine.evaluate` unconditionally. One related item is
  disclosed, not accidental: `TaskManagerRuntimeSpecification.md` §8
  states explicitly, in its own words, that cross-Principal Task control
  actions (e.g. cancelling another Principal's Task) are "not
  Permission-Engine-gated by default" today, and records this openly as
  an Open Question rather than silently assuming a gate exists. Because
  this is disclosed in the specification's own text rather than merely
  implied or omitted, it does not constitute an *accidental* implication
  of permission bypass — it is a known, named, deliberately-left-open
  design question, already correctly catalogued in
  `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` §6 (Gap 5). It is
  noted here for completeness because it is the trust-boundary item
  closest to the six checked for, not because it fails this review's
  test.
- **Identity bypass.** Not found. Every Principal resolution path in
  every specification routes through the Identity Service; the disclosed
  incompleteness (gaps #37, #39, #40) is a gap in enforcement
  thoroughness, explicitly recorded as a dependency in each affected
  specification, not a silently-assumed or accidentally-implied bypass
  path.
- **Hidden autonomous execution.** Not found. All three specifications
  include an explicit "no hidden background execution/planning" safety
  boundary using near-identical language (Task Manager Runtime
  Specification §12; Agent Runtime Specification §11, including the
  dedicated "WAITING_FOR_INPUT Trust Boundary" subsection; Planner
  Runtime Specification §13), each stating plainly that no execution mode
  continues outside a tracked, active session/run/task state.

**Conclusion for this section: no trust boundary issue was found that
the reviewed documents do not already disclose themselves.**

## 9. Recommendations

These are documentation and specification-coordination recommendations
only; none proposes implementation or alters existing architecture.

1. Add Gap 11 (Section 6 above — the Agent Run cancellation-request
   contract) to `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` §6
   and, correspondingly, to the "Agent Run Request / Agent Run Reference"
   row of that document's §3 Contract Map, since it is the same kind of
   gap already recorded there for Agent Run creation.
2. Consider adding an explicit "Relationship to the Task Manager Task
   and Agent Run Lifecycles" subsection to
   `PlannerRuntimeSpecification.md` §5, mirroring the dedicated subsection
   `AgentRuntimeSpecification.md` §5 already has relative to the Task
   Manager Task lifecycle, to close the terminology-consistency asymmetry
   noted in Section 7. This is an editorial clarification, not a
   substantive lifecycle change, since no document currently claims the
   three lifecycles are shared.
3. No correction is recommended to any specification's substantive rules
   — every rule this review checked held up under cross-examination
   against its sibling documents.
4. Future gap-closure work (per `docs/architecture/IMPLEMENTATION_ORDER.md`
   §6's own rule) should continue to close Section 6's gaps through the
   same review-and-correction-pass process already used for the Agent
   Runtime and Task Manager Runtime Specifications, rather than resolving
   them silently inside whichever document is drafted next.

## 10. Final Assessment

**Position validated.**

Every claim in the position statement under review is supported by
direct, citable, mutually-consistent text across the reviewed
specifications: the cognitive control chain is coherent at the
specification level; the Planner, Task Manager, Agent Runtime, and
Execution Pipeline each hold exactly the responsibilities the position
claims and no more; Identity Service, Permission Engine, Resource
Registry, and EventBus are genuinely cross-cutting rather than owned by
any one subsystem; and every remaining gap this review examined —
including one gap (Section 6, Gap 11) not yet present in the existing
gap catalogue — is a missing or incomplete contract definition between
specifications that already agree on direction and intent, not a
disagreement or an architectural flaw. No contradiction and no
accidental trust-boundary violation was found. The one editorial
observation raised (Section 7) is a documentation-consistency
recommendation, not a defect requiring correction before this position
can be considered valid.

## Related

- `docs/architecture/IMPLEMENTATION_ORDER.md`
- `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md`
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
- `docs/reviews/AgentRuntimeSpecificationReview.md`
- `docs/reviews/TaskManagerRuntimeSpecificationReview.md`
- `docs/architecture/IdentityService.md`
- `docs/specifications/volume-02-core-schemas/Task-Schema.md`
- `docs/adr/ADR-012-task-and-workflow-separation.md`
- `src/contracts/ExecutionRequest.kt`
- `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`
- `docs/architecture/tool-registry.md`
- `docs/specifications/volume-03-core-interfaces/EventType.md`
- `docs/architecture/action-mapping.md`
- `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
- `docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`
- `docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`
- `docs/specifications/volume-03-core-interfaces/EventBus.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md`
