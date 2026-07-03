# Task Manager Runtime Specification — Review

## Status

Review-only. This document does not modify
`docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`,
`src/`, or `tests/`. It evaluates the specification as committed, against
the existing Parker architecture, for internal consistency and
integration correctness.

Reviewed against: `AgentRuntimeSpecification.md`, `Task-Schema.md`,
ADR-012, `IdentityService.md`, `src/contracts/ExecutionRequest.kt`,
`action-mapping.md`, `ToolRegistry.md` (Volume 3 interface),
`docs/architecture/tool-registry.md`, `EventType.md`, `EventBus.md`,
`ResourceRegistry.md`, `PermissionEngine.md`, `ExecutionPipeline.md`,
`src/contracts/Permission.kt`, `src/contracts/ExecutionResult.kt`,
`src/contracts/EventContracts.kt`, and `docs/architecture/IMPLEMENTATION_GAPS.md`.

## 1. Executive Summary

The Task Manager Runtime Specification is internally consistent and
correctly integrated with the existing Parker architecture. It treats the
Task Manager Task as the single canonical unit of tracked work, reproduces
`Task-Schema.md`'s lifecycle verbatim with no invented states or edges,
and correctly restates — without contradicting — the Agent Runtime
Specification's own boundary that Agent Run state never directly mutates
Task state. Every execution path described routes through the existing
Execution Pipeline, Permission Engine, and Tool Registry with no
Task-specific shortcut, and every proposed concept beyond the current
`Task.schema.json` is explicitly marked **(proposed)** rather than
silently assumed as fact.

No High severity finding was identified: nothing in the document permits
an actual bypass of Identity, Permission, or Execution Pipeline
authority. Three Medium findings were identified, all concerning
under-specification rather than incorrect specification: whether
cross-Principal Task control actions (e.g. cancelling another Principal's
Task) require Permission Engine gating, what Task Status a revoked
Owner/Assignee's Task resolves to, and the acknowledged absence of
dedicated events for the `Expired`/`Superseded` terminal transitions. All
three are already visible in the document itself (as an Open Question,
an underspecified MUST NOT, and an explicit non-goal respectively), so
none represents a hidden problem — they represent decisions correctly
deferred rather than invented. A small number of Low severity, editorial
observations are also recorded.

**Assessment: Needs correction pass** — the corrections identified are
narrow and specific, not a redesign; see Section 7.

## 2. Confirmed Architectural Strengths

- **Single canonical Task abstraction.** Section 4 explicitly states "The
  Agent Runtime does not define a separate 'Agent Task' abstraction," and
  the reciprocal statement already exists in `AgentRuntimeSpecification.md`
  Section 4. Both documents agree, in both directions, that the Task
  Manager Task is the only Task concept in the platform.
- **Lifecycle fidelity.** Section 5's state diagram and prose transitions
  are byte-for-byte consistent with `Task-Schema.md`'s "Valid Transitions"
  and "Invalid Transitions" sections (verified directly against both
  documents): the same four source states, the same edge set, the same
  `Superseded`-not-reachable-from-`Running` and `Failed`-only-from-`Running`
  rules. No state or edge was invented.
- **Symmetric, non-contradictory Agent Runtime boundary.** Section 6's
  "Agent Run state does not directly mutate Task state" is not merely
  asserted here — it is the same boundary `AgentRuntimeSpecification.md`
  Section 5 ("Relationship to the Task Manager Task Lifecycle") already
  states from the Agent Runtime's side. The two documents describe one
  boundary from both sides consistently, rather than one document
  asserting a rule the other is silent on.
- **No execution shortcut.** Section 7's sequence diagram routes both the
  direct and Agent-Run-mediated paths through the identical
  `ExecutionPipeline.submit → PermissionEngine.evaluate → ToolRegistry.resolve`
  chain. Neither path skips a step, and the Task Manager Runtime is never
  shown holding a `Tool` reference.
- **Correct reuse over invention.** Task Source and Task Priority are
  explicitly mapped onto the existing `RequestOrigin` and `RequestPriority`
  enums (`src/contracts/ExecutionRequest.kt`) rather than introducing a
  second, parallel vocabulary that could drift from the first. This is
  verified accurate: both enums exist exactly as cited.
- **Honest schema-vs-proposal labelling.** Section 4 marks every concept
  as **(schema)** or **(proposed)**, and Section 4's own required fields
  (`taskId`, `ownerPrincipalId`, `status`, `createdAt`, `updatedAt`) match
  `Task.schema.json` and `Task-Schema.md` exactly, verified directly.
- **Correct Tool Registry boundary.** Section 7 permits only read-only
  discovery (`listAll`/`findCandidates`) for planning purposes, matching
  `ToolRegistry.md`'s explicit statement that these two operations "MUST
  NOT return anything invocable" and that `resolve` is "intended to be
  called only by the Execution Pipeline."
- **Consistent identity treatment.** Section 8's "unresolvable Task Owner
  is invalid, not denied" mirrors `IdentityService.md`'s "Principal
  Resolution" section verbatim in spirit, and the revocation-dependency
  framing (gaps #37/#39) is cited accurately against
  `IMPLEMENTATION_GAPS.md`'s actual entries for both gaps, verified
  directly.
- **No scope creep.** Sections 3 and 13 both correctly exclude Planner,
  Memory, World Model, Workflow Engine, and Android integration, and
  nothing elsewhere in the document quietly implements any of them (no
  decomposition logic, no belief representation, no long-term storage
  schema, no multi-Task branching/retry logic).
- **Event/EventBus alignment.** All 17 defined events are namespaced
  `task.*` per `EventType.md`'s `<domain>.<event>` convention, and the
  document correctly notes (without overclaiming) that `task.*` is not
  yet on `EventBus.md`'s trust-sensitive domain list, only recommended for
  addition — matching the identical, already-accepted treatment of
  `agent.*` in the Agent Runtime Specification.

## 3. High Severity Findings

None identified. No path was found by which the Task Manager Runtime
bypasses the Permission Engine, the Identity Service, or the Execution
Pipeline; no direct tool invocation; no identity mutation; no competing
Task, Memory, or World Model abstraction.

## 4. Medium Severity Findings

**M1 — Cross-Principal Task control actions are not gated by the
Permission Engine by default.**
Section 8 states plainly that "creating a Task, assigning it, changing
its Task Constraint or Task Dependency set, or requesting its
cancellation ... are all themselves actions attributable to a Principal,"
but attribution is not authorization: nothing in Section 5, 8, or 12
requires a `PermissionAction.CONTROL` (or any) evaluation before one
Principal cancels, reassigns, or otherwise mutates a Task it does not own.
The document is aware of this — it is recorded verbatim as an Open
Question ("Whether Task Status transitions that affect another
Principal's Task ... should require an explicit `PermissionAction.CONTROL`
evaluation, the way Tool Registry registration already does") — so this
is a disclosed gap, not a hidden one. It is nonetheless a real,
trust-relevant absence: as written, any authenticated Principal can
cancel or reassign any Task in the system today. Recommend either (a)
stating an explicit interim default (e.g. "until this is decided, only
the Task Owner, Task Assignee, or a Principal with administrative
standing may perform a control action on a Task not its own") or (b)
elevating the Open Question's language from a values question ("should
this require permission") to an explicit interim constraint, so the
document does not leave a fully open authorization surface for the
interval before this question is settled.

**M2 — The resulting Task Status for a Running Task with a
revoked/inactive Owner or Assignee is not specified.**
Section 8 requires that such a Task "MUST NOT allow a Running Task ...
to acquire further execution," and Section 11 requires it "MUST NOT
proceed into or remain in Running" — both are phrased as constraints on
what must *not* happen, but neither states what Task Status the Task
Manager Runtime should move the Task to instead (`Paused`? `Failed`?
`Cancelled`?). This is a real asymmetry against
`AgentRuntimeSpecification.md`'s identical scenario: Section 10 there
("Identity revocation") is explicit that the Agent Run "stops rather than
continuing (`--> FAILED`)." The Task Manager Specification's equivalent
section does not make the same choice for the Task it concerns. This also
creates minor tension with Section 6's general philosophy that Task
Status transitions are "a Task Manager rule, evaluated deliberately, not
an automatic mirror" — if no explicit rule is stated for this case, a
"MUST NOT remain in Running" requirement has no defined mechanism to
satisfy it. Recommend adding an explicit sentence: either mirroring the
Agent Runtime Specification's `--> FAILED` resolution, or stating that
this is deliberately left as Task-Manager-rule-defined (mirroring Section
6's general pattern) and recording it as an Open Question rather than a
`MUST NOT` with no resolution path.

**M3 — No dedicated Task Events exist for the `Expired` and `Superseded`
terminal transitions.**
Five of the twenty edges in Section 5's lifecycle diagram terminate in
`Expired` or `Superseded`, but Section 10's 17-event table has no
`task.expired` or `task.superseded` equivalent to `task.completed`/
`task.failed`/`task.cancelled`. The document discloses this directly
("This document does not define dedicated Task Events for the `Expired`
or `Superseded` terminal transitions ... recorded as an Open Question"),
so this is not a hidden gap, but it is a genuine lifecycle-coverage gap:
Chapter 43 (Audit and Observability) would have no direct event trail for
roughly a quarter of the lifecycle's terminal edges. Given the pattern
already established for the other three terminal transitions, adding
`task.expired`/`task.superseded` as real-transition events appears to be
a small, low-risk completion rather than a design question — recommend
resolving this in the correction pass rather than leaving it to a future
revision, unless there is a specific reason (not stated in the document)
to treat these two transitions differently from the other three.

## 5. Low Severity Findings

**L1 — `resolve(decision.action, request.targetResources)` does not match
`ToolRegistry.resolve`'s actual signature.**
Section 7's sequence diagram writes `TR->>RR: resolve(resourceId)` following
`EP->>TR: resolve(decision.action, request.targetResources)`, but
`ToolRegistry.md`'s actual interface is
`resolve(action: PermissionAction, resourceTypes: Set<ResourceType>)` — it
takes `ResourceType`s, not the `ResourceId`s in `targetResources`, with no
stated derivation step. This is editorial: the same imprecision already
exists, worded identically, in `AgentRuntimeSpecification.md`'s Section 6
diagram, and was not flagged in that document's own prior review or
publication-readiness pass. Not a new inconsistency introduced by this
document; recommend fixing in both documents together if it is ever
revisited, not treating this document as uniquely at fault.

**L2 — Task Context is not explicitly contrasted against Agent Context in
one place.**
Section 9 explicitly distinguishes Task Context from Memory and the
World Model, but never states outright, in a single sentence, that Task
Context and Agent Context are two distinct, non-overlapping stores that
reference each other only by ID. The boundary is correctly *implied* —
Section 4's "Task Context Reference" and "Agent Run Reference" are
separately defined, and cross-reading `AgentRuntimeSpecification.md`
Section 8's "Agent Step state" (which stores a `taskId` reference, not a
copy of Task Context) confirms there is no duplication — but a reader of
this document alone would need to infer it. Recommend one additional
sentence in Section 9 stating this directly.

**L3 — The Agent-Event subscription mechanism the Task Manager Runtime
uses to learn of Agent Run outcomes is not specified.**
Section 6 says the Task Manager Runtime "subscribes to (or is otherwise
informed of) relevant Agent Events ... for Agent Runs it has a recorded
Agent Run Reference for," without saying whether this is an
`EventBus.subscribe` call scoped by `correlationId`, a query-based
mechanism, or something else. This is reasonably left as
implementation-phase content (consistent with this document's own
stated scoping elsewhere, e.g. Agent Result payload schemas), so it is
Low rather than Medium, but is recorded here for completeness since the
review criteria asked about hidden assumptions in Agent Runtime
integration.

**L4 — `task.suspended`/`Paused` naming divergence is intentional and
already well-documented; no issue exists.**
Noted here explicitly per the review's "if no issue exists, state so"
instruction: Section 10's table already explains that the event name
`task.suspended` deliberately differs from the `Paused` status value for
cross-specification consistency with the Agent Runtime Specification's
`SUSPENDED` vocabulary. This is a clearly-labelled, deliberate choice, not
an inconsistency.

## 6. Confirmed Trust Boundaries

- **No direct tool execution.** Confirmed: no operation in the document
  invokes `Tool.execute` or holds an invocable `Tool` reference (Sections
  3, 7, 12).
- **No permission bypass for execution.** Confirmed: every
  `ExecutionRequest` path in Section 7's diagram passes through
  `PermissionEngine.evaluate` exactly once, matching
  `PermissionEngine.md`'s "Every ExecutionRequest MUST be evaluated before
  execution."
- **No identity mutation.** Confirmed: Section 12 states the Task Manager
  Runtime never writes directly to a `Principal` record; `updateStatus`
  remains exclusively an Identity Service operation, matching
  `IdentityService.md`'s "no other component may write a Principal's
  `status` field directly."
- **No hidden background execution.** Confirmed: Section 12 requires
  every Agent Run Reference and Execution Reference to be recorded in
  Task Context and observable via a Task Event; no execution mode runs
  outside a tracked Task.
- **No direct Memory or World Model mutation.** Confirmed: Section 9
  explicitly excludes both, and Section 12 restates it as a safety
  boundary.
- **Weakness noted (see M1):** Task control actions across Principal
  boundaries (e.g. cancelling another Principal's Task) are attributable
  but not access-controlled by default. This is the one place the review
  found the trust boundary genuinely open rather than merely
  under-described — already surfaced by the document as an Open Question,
  but worth stating plainly here as the review's own finding rather than
  only as a cross-reference.

## 7. Recommended Corrections

1. Resolve M1: add an explicit interim default for cross-Principal Task
   control actions (who may cancel/reassign a Task they do not own, until
   the `PermissionAction.CONTROL` question is formally decided), or
   state explicitly that no such action is currently access-controlled
   beyond attribution, so implementers do not have to infer the current
   state from an Open Question's phrasing alone.
2. Resolve M2: state what Task Status a Running Task moves to when its
   Owner or Assignee is reported non-Active — either mirroring the Agent
   Runtime Specification's `--> FAILED` resolution, or explicitly
   recording the absence of a prescribed target status as a deliberate,
   Task-Manager-rule-governed choice (matching Section 6's general
   pattern) rather than an unresolved `MUST NOT`.
3. Resolve M3: add `task.expired` and `task.superseded` to Section 10's
   event table as real-transition events, consistent with the treatment
   already given to `task.completed`/`task.failed`/`task.cancelled`, or
   state explicitly why these two transitions are being treated
   differently.
4. Optionally address L2: add one sentence to Section 9 explicitly
   contrasting Task Context and Agent Context as non-overlapping,
   reference-only stores.
5. L1 and L3 do not require action in this document alone; L1 should be
   addressed jointly with `AgentRuntimeSpecification.md` if revisited, and
   L3 is appropriately deferred to an implementation phase.

## 8. Open Questions

These are new observations from this review, distinct from the eight
Open Questions the specification itself already records (which this
review does not repeat here, since the specification's own list remains
valid and unaffected by this review's findings):

- Should the correction pass for M1 and M2 be applied to
  `TaskManagerRuntimeSpecification.md` alone, or should
  `AgentRuntimeSpecification.md` also be revisited for the L1 signature
  imprecision it shares, given both documents are meant to be read
  together as one Phase 3 design baseline?
- If M1's interim default is added, should the same interim default also
  govern cross-Principal actions on an Agent Run (e.g. one Principal
  cancelling another's Agent Run), which `AgentRuntimeSpecification.md`
  does not appear to gate either — this review did not check that
  document for the identical gap in depth, since it was scoped to the
  Task Manager Specification, but the parallel is close enough to be
  worth a human decision on whether to check.

## 9. Publication Readiness Assessment

**Needs correction pass.**

The specification is architecturally sound: it is internally consistent,
correctly subordinate to the Execution Pipeline/Permission Engine/Tool
Registry/Identity Service, faithful to the existing Task lifecycle, and
free of any accidental Planner/Memory/World Model/Workflow
implementation. The three Medium findings (M1–M3) are narrow,
well-bounded gaps the document already partially discloses — none
requires rethinking the document's architecture, and none was invented by
this review beyond what the evidence supports. A short, targeted
correction pass addressing M1–M3 (and optionally L2) would bring this
document to the same publication-ready state already reached by the
corrected Agent Runtime Specification.

## Related

- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
- `docs/reviews/AgentRuntimeSpecificationReview.md`
- `docs/specifications/volume-02-core-schemas/Task-Schema.md`
- ADR-012 – Task and Workflow Separation
- `docs/architecture/IdentityService.md`
- `docs/architecture/action-mapping.md`
- `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`
- `docs/architecture/tool-registry.md`
- `docs/specifications/volume-03-core-interfaces/EventType.md`
- `docs/specifications/volume-03-core-interfaces/EventBus.md`
- `docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`
- `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
- `docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`
- `src/contracts/ExecutionRequest.kt`
- `src/contracts/Permission.kt`
- `src/contracts/ExecutionResult.kt`
- `src/contracts/EventContracts.kt`
- `docs/architecture/IMPLEMENTATION_GAPS.md` #37, #39
