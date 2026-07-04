# Parker Architecture Decisions

## 1. Purpose

This document is Parker's canonical record of fundamental, platform-wide
architectural decisions. It is **not another specification**. A
specification (the Agent Runtime Specification, the Task Manager Runtime
Specification, the Planner Runtime Specification, and the Volume 1/3
interface documents) describes *what a component does*: its lifecycle,
its events, its fields, its boundaries. An Architecture Decision describes
*why the platform as a whole is shaped the way it is* — the small set of
rules that hold across every specification, that no single specification
invents on its own, and that a contributor would otherwise have to infer
by reading all of them and noticing the same pattern repeated.

Every decision recorded here is already evidenced by the existing
repository — this document invents no new principle. Its purpose is to
give a future contributor a place to read *why* Parker requires identity
before authority, why the Planner never creates a Task, or why the
Execution Pipeline is the only path to tool execution, without needing to
cross-reference three specifications' Non-Goals sections to notice they
all say the same thing for the same underlying reason.

**A note on Planner Runtime Specification review status.** Several
decisions in Section 4 (AD-002, AD-005, AD-010, AD-011, and AD-012) cite
`docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
as evidence. That specification has not yet completed the same
independent review-and-correction cycle already applied to the Agent
Runtime Specification (`docs/reviews/AgentRuntimeSpecificationReview.md`
→ correction pass) and the Task Manager Runtime Specification
(`docs/reviews/TaskManagerRuntimeSpecificationReview.md` → correction
pass) — `docs/reviews/Phase3ArchitecturePositionReview.md` examined it
only as part of a cross-specification architecture check, not as a
standalone review of its own internal consistency. The decisions that
cite it remain Accepted because each is also cross-supported by
already-approved architecture and trust-boundary rules that do not
depend on the Planner Runtime Specification alone (identity-before-
authority, no-execution-bypass, and reference-based Context are each
independently stated by the Agent Runtime and Task Manager Runtime
Specifications too). A future Planner Runtime Specification review may
refine Planner-specific wording, gaps, or terminology, but should not
silently alter an already-accepted platform-wide decision recorded here —
any such effect must go through this document's own correction process
(Section 6), not happen as a side effect of a specification-level
review.

## 2. How to Use This Document

- **Decisions explain intent.** An Architecture Decision states a rule
  and why it exists. It does not define a lifecycle, an event schema, or
  a field list — that is a specification's job.
- **Specifications explain behaviour.** Each specification
  (`docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`,
  `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`,
  `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`,
  and the Volume 1/3 interface documents) implements the decisions
  recorded here in the specific shape of one component. When a
  specification and this document appear to disagree, the specification
  is more likely to contain the error — but see Section 6: a disagreement
  is a signal to review the specification, not to silently reinterpret
  the decision.
- **Implementation follows specifications, not decisions directly.** No
  contributor should write Kotlin against an Architecture Decision
  alone; implementation targets a specification, per
  `docs/architecture/IMPLEMENTATION_ORDER.md`'s existing rule that
  nothing is authorised for implementation until an explicitly-declared
  implementation phase promotes it.
- **Reviews validate specifications against decisions (among other
  things).** The review-and-correction-pass pattern already used for the
  Agent Runtime and Task Manager Runtime Specifications
  (`docs/reviews/AgentRuntimeSpecificationReview.md`,
  `docs/reviews/TaskManagerRuntimeSpecificationReview.md`) and for
  cross-specification architecture
  (`docs/reviews/Phase3ArchitecturePositionReview.md`) is where a
  specification's consistency with these decisions is checked, not this
  document itself.
- **Architecture decisions rarely change.** A specification may be
  drafted, reviewed, and corrected multiple times as Parker grows; the
  decisions in Section 4 are the load-bearing rules every one of those
  specifications already depends on, and changing one would require
  revisiting every specification and inter-specification contract built
  on it (Section 7). This document expects long stability where
  specifications expect iteration.

## 3. Decision Catalogue

Each entry in Section 4 records:

- **Decision ID** — a stable `AD-NNN` identifier, distinct from this
  repository's existing `ADR-NNN` Architecture Decision Records
  (`docs/adr/`), which record narrower, single-topic decisions (e.g.
  ADR-003, "Single Execution Pipeline"). An Architecture Decision may
  restate or generalise one or more existing ADRs across multiple
  specifications; it does not replace or supersede them.
- **Title** — a short, memorable name.
- **Status** — see Section 6.
- **Decision** — the rule itself, stated plainly.
- **Reasoning** — why the rule exists.
- **Evidence** — the specific existing document(s) and section(s) that
  already state or depend on this rule. No decision in Section 4 lacks
  cited evidence.
- **Affected specifications** — which existing specifications implement
  or depend on this decision.
- **Consequences** — what follows from the rule being true.
- **Future considerations** — any open question or dependency, recorded
  elsewhere, that bears on this decision without this document resolving
  it.

## 4. Initial Decision Set

### AD-001 — Identity First

**Status:** Accepted

**Decision:** Identity is established before authority, execution,
planning, or orchestration. No component may act, propose, or coordinate
work on behalf of a Principal that cannot be resolved.

**Reasoning:** Every other trust question — is this allowed, is this
authorised, is this auditable — presupposes an answer to "who is asking."
Answering "are they allowed" before "who is asking" would mean evaluating
permission for an unidentified actor, which cannot be meaningfully
audited or attributed.

**Evidence:** `docs/specifications/volume-01-core-contracts/Principal.md`
("Parker MUST NOT execute any request without an identified Principal");
`docs/architecture/IdentityService.md` ("who is requesting this" kept
strictly separate from Permission's "are they allowed," and "Principal
Resolution": an unresolvable Principal is invalid, not denied); ADR-013
(Agents and Services Use Principal Identities); `AgentRuntimeSpecification.md`
§7 ("No Agent Instance may reach `INITIALISED`... without a resolvable
`PrincipalId`"); `TaskManagerRuntimeSpecification.md` §8 ("Every Task has
an owner Principal"); `PlannerRuntimeSpecification.md` §8 ("Every
Planning Session has an initiating Principal").

**Affected specifications:** Agent Runtime Specification, Task Manager
Runtime Specification, Planner Runtime Specification, `IdentityService.md`,
`Principal.md`.

**Consequences:** An unresolvable Principal is always treated as invalid
(a validation failure), never as denied (a permission outcome) — these
are different failure categories with different audit meanings across
every specification. No component maintains its own identity store.

**Future considerations:** The exact cascading-revocation rule for owned
Principals remains undecided (`IMPLEMENTATION_GAPS.md` #35); `resolve()`
does not yet suppress or flag non-Active Principals (#37); `identity.*`
events are not yet published (#39). These are enforcement-completeness
gaps in this decision's implementation, not a change to the decision
itself.

---

### AD-002 — Proposal Before Authority

**Status:** Accepted

**Decision:** Parker's control chain has four distinct kinds of role, and
no subsystem collapses more than one of them into itself:

- **Intelligent subsystems propose.** The Planner Runtime proposes work
  (Task Proposals). Proposing, by itself, has no external effect.
- **Runtime coordination services orchestrate.** The Task Manager
  Runtime coordinates and decides — accepting, deferring, splitting,
  merging, or rejecting proposals, and coordinating Agent Runs against
  the Tasks it owns. Orchestrating is neither proposing nor authorising
  nor executing; it is a distinct role that decides *whether and how*
  proposed work proceeds, without itself being the authority that
  approves an action or the mechanism that carries one out.
- **Authority services authorise.** The Permission Engine is the sole
  source of permission for any action with external effect.
- **Execution services execute.** The Execution Pipeline (dispatching to
  the Tool Registry and a resolved Tool) is the sole mechanism by which
  an authorised action actually has an external effect.

**Reasoning:** Separating "what should happen" (propose), "should this
proceed" (orchestrate), "is this allowed" (authorise), and "make it
happen" (execute) keeps every point where judgement — potentially
model-driven, potentially non-deterministic, or simply coordinating
competing work — is exercised strictly distinct from the point where
permission is decided and the point where an irreversible or auditable
action occurs. Collapsing any two of these roles into one subsystem would
let that subsystem grant itself something only a separate role is meant
to grant: a Planner that could orchestrate would not need Task Manager
acceptance; a Task Manager that could authorise would not need the
Permission Engine; an Execution Pipeline that could propose would not
need anything upstream of it at all.

**Evidence:** `PlannerRuntimeSpecification.md` §2 ("Proposal-before-
authority... Authority to act always comes from a later, separate
decision by the Task Manager Runtime, the Permission Engine, or both —
never from the act of proposing"); `TaskManagerRuntimeSpecification.md`
§1 ("The Task Manager coordinates work. It does not execute tools
directly, does not bypass permissions, and does not replace the
Execution Pipeline" — the orchestrating role stated in the Task Manager
Runtime Specification's own words, distinct from both proposing and
authorising/executing); ADR-001 (Models Never Execute Tools);
`docs/architecture/action-mapping.md` ("Why the Permission Engine must
never parse intent"); `AgentRuntimeSpecification.md` §6 (an Agent
Instance's only channel for effect is constructing an `ExecutionRequest`,
never executing directly); `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
and `docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`
(the authorising and executing roles respectively).

**Affected specifications:** Planner Runtime Specification (proposes),
Task Manager Runtime Specification (orchestrates), `PermissionEngine.md`
(authorises), `ExecutionPipeline.md` (executes), Agent Runtime
Specification (performs Agent Runs within the orchestrated chain, itself
proposing actions rather than authorising or executing them),
`action-mapping.md`.

**Consequences:** A Task Proposal, a Task Manager Task's existence, and
an Agent Run's proposed action are all recommendations, coordination
decisions, or requests — never grants. The only operations that ever have
external effect are `PermissionEngine.evaluate` followed by
`ExecutionPipeline.submit` succeeding. The Task Manager Runtime's
orchestration decisions (accept, defer, split, merge, reject) are
themselves never a substitute for Permission Engine evaluation of any
resulting `ExecutionRequest` — orchestrating that a Task should proceed
is not the same as authorising a specific action within it.

**Future considerations:** Whether Chapter 20's "Deliberation Service" is
distinct from the Planner Runtime's own internal Plan Decision remains an
open question (`PlannerRuntimeSpecification.md` §1, Open Questions);
either way, this decision is unaffected, since Plan Decision itself is
still upstream of authority and remains part of the "propose" role, not
the "orchestrate" role.

---

### AD-003 — Execution Pipeline Is the Sole Execution Authority

**Status:** Accepted

**Decision:** All execution passes through the Execution Pipeline. No
subsystem holds an invocable Tool reference or has a second path to an
external effect.

**Reasoning:** A single, mandatory execution path is what makes every
other trust guarantee (permission evaluation, audit logging, resource
sensitivity checks) structurally impossible to route around, rather than
a convention every future component must remember to follow.

**Evidence:** ADR-003 (Single Execution Pipeline); `docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`
("The pipeline MUST NOT accept unvalidated requests... MUST NOT bypass
PermissionEngine"); `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`
("`resolve` is the only operation that may yield something invocable, and
... is intended to be called only by the Execution Pipeline"); restated
identically in `AgentRuntimeSpecification.md` §6/§11,
`TaskManagerRuntimeSpecification.md` §7/§12, and `PlannerRuntimeSpecification.md`
§3/§8/§13. Sprint 1 Unit 11A confirms this by construction, not just by
specification: `docs/specifications/volume-03-core-interfaces/ToolInvocationBinding.md`
and `DefaultExecutionPipeline.executeResolvedTool` are the only call site
anywhere in the repository that invokes `ToolInvocationBinding.invocableFor`,
`Tool.validate`, or `Tool.execute` -- so the rule now governs a real
invocation, not merely a resolution, as it did before `IMPLEMENTATION_GAPS.md`
#32 closed.

**Affected specifications:** Agent Runtime Specification, Task Manager
Runtime Specification, Planner Runtime Specification,
`ExecutionPipeline.md`, `ToolRegistry.md`, `ToolInvocationBinding.md`,
`docs/architecture/tool-registry.md`.

**Consequences:** Every origin — voice, text, scheduled task, Agent, Plugin,
Task-Manager-direct — submits the identical `ExecutionRequest` shape
through the identical path; no origin gets a shortcut, richer request
type, or elevated default.

**Future considerations:** None currently open against this decision
itself.

---

### AD-004 — Task Manager Owns Canonical Tasks

**Status:** Accepted

**Decision:** The Task Manager Task, as defined by `Task-Schema.md` and
ADR-012, is the platform's one canonical unit of tracked work. No other
subsystem defines, extends, or owns a competing Task abstraction.

**Reasoning:** Multiple independently-invented "Task" concepts across
Planner, Agent Runtime, and any future Workflow Runtime would fragment
tracked-work state and make it impossible to answer "what is the
authoritative status of this piece of work" without knowing which
subsystem's version to trust.

**Evidence:** ADR-012 ("Tasks track work. Workflows define structured
multi-step behaviour"); `docs/specifications/volume-02-core-schemas/Task-Schema.md`;
`TaskManagerRuntimeSpecification.md` §1, §2, §4 ("the Task Manager
Runtime is the sole owner of Task Manager Task state"); `AgentRuntimeSpecification.md`
§4 ("The Agent Runtime does not define a separate 'Agent Task'
abstraction"); `PlannerRuntimeSpecification.md` §6 ("The Task Manager
Runtime decides whether Task Proposals become Task Manager Tasks").

**Affected specifications:** Task Manager Runtime Specification, Agent
Runtime Specification, Planner Runtime Specification, `Task-Schema.md`,
ADR-012.

**Consequences:** A future Workflow Runtime Specification must compose
Task Manager Tasks rather than define its own (`docs/architecture/IMPLEMENTATION_ORDER.md`
§6); every specification that mentions "Task" defers to this one
canonical definition rather than redefining it.

**Future considerations:** None currently open against this decision
itself; see AD-005 and AD-006 for the two decisions that depend on it.

---

### AD-005 — Planner Never Creates Tasks

**Status:** Accepted

**Decision:** The Planner Runtime produces Task Proposals only. It has no
operation that creates a Task Manager Task record directly.

**Reasoning:** This is AD-002 and AD-004 applied together: the Planner is
an intelligent, proposing subsystem (AD-002), and the Task Manager is the
canonical Task owner (AD-004); a Planner that could create Tasks directly
would let a proposing component grant itself the authority AD-004
reserves for the Task Manager.

**Evidence:** `PlannerRuntimeSpecification.md` §6 ("The Planner Runtime
does not create Tasks directly... There is no operation by which the
Planner Runtime writes a `taskId`... itself"); §13 ("No direct Task
creation").

**Affected specifications:** Planner Runtime Specification, Task Manager
Runtime Specification.

**Consequences:** A Task Proposal is not, and is never treated as, a Task
Manager Task; it becomes one only if and when the Task Manager Runtime
accepts it.

**Future considerations:** Sprint 1 Unit 6 gives the previously-open Task
Proposal intake operation and disposition mechanism a real, tested
implementation: `InMemoryTaskManagerRuntime.submitProposal` is the intake
operation, and `TaskProposalDisposition` is the disposition mechanism
reported back. Only the `Accept` disposition is implemented --
`Deferred`, `Split`, `Merged`, and any business-reason `Rejected` remain
unimplemented, a Sprint 1 scope boundary rather than a contract gap. The
contract shape itself (`docs/architecture/INTER_SPECIFICATION_CONTRACTS.md`
§6, Gaps 1–2) is no longer open; what remains is implementation breadth,
not a disagreement about this decision.

---

### AD-006 — Agent Runtime Never Owns Tasks

**Status:** Accepted

**Decision:** The Agent Runtime performs Agent Runs and Agent Steps only.
An Agent Run may execute within a Task Manager Task but never owns,
mutates, or redefines that Task's state.

**Reasoning:** Symmetric to AD-005: the Agent Runtime is the component
that performs work within a Task (AD-004), not the component that decides
what that Task's own status is; allowing an Agent Run to write Task
status directly would let execution-layer activity silently override the
Task Manager's own deliberate rules.

**Evidence:** `AgentRuntimeSpecification.md` §5 ("Relationship to the Task
Manager Task Lifecycle": the two lifecycles "MUST NOT be inferred from
one another"); §11 ("An Agent Instance never writes a Task's `status`
directly"); `TaskManagerRuntimeSpecification.md` §6 ("Agent Run state
does not directly mutate Task state" — the identical rule stated from the
Task Manager's own side).

**Affected specifications:** Agent Runtime Specification, Task Manager
Runtime Specification.

**Consequences:** The Agent Run lifecycle and Task Manager Task lifecycle
are independently tracked state machines, even though they happen to
share several state-name tokens (`CREATED`, `RUNNING`, `COMPLETED`,
`FAILED`, `CANCELLED`) — explicitly documented as a naming coincidence,
not a shared machine.

**Future considerations:** `AgentRunCommand` (Blocker 3, pre-Sprint-1
contract closure) is the previously-missing named contract, and Sprint 1
Units 6–7 give it a real construction path
(`InMemoryTaskManagerRuntime`) and consumption path
(`InMemoryAgentRuntime`). Only `START` is implemented -- `SUSPEND`,
`RESUME`, and `CANCEL` remain unimplemented, each returning an explicit
`Rejected` outcome rather than a silent no-op. The contract shape itself
(`docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` §6, Gap 7;
`docs/reviews/Phase3ArchitecturePositionReview.md` §6, Gap 11) is no
longer open; what remains is implementation breadth, not a disagreement
about this decision.

---

### AD-007 — Permission Decisions Belong to the Permission Engine

**Status:** Accepted

**Decision:** Subsystems never self-authorise. Every `ExecutionRequest`,
regardless of origin, is evaluated exactly once by `PermissionEngine.evaluate`,
and no component substitutes its own judgement for that decision.

**Reasoning:** If every origin could decide for itself whether its own
request should proceed, "permission" would mean whatever each origin
chose to enforce, defeating the purpose of having a single evaluation
authority at all.

**Evidence:** `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
("Every ExecutionRequest MUST be evaluated before execution");
`docs/architecture/action-mapping.md` ("Why the Permission Engine must
never parse intent"); `AgentRuntimeSpecification.md` §11 ("No
self-permissioning... An Agent Instance cannot approve its own proposed
action"); `TaskManagerRuntimeSpecification.md` §12 ("No permission
bypass"); `PlannerRuntimeSpecification.md` §8 ("The Planner Runtime
cannot expand its own authority").

**Affected specifications:** Agent Runtime Specification, Task Manager
Runtime Specification, Planner Runtime Specification, `PermissionEngine.md`,
`action-mapping.md`.

**Consequences:** Pure lifecycle transitions with no external effect
(e.g. a Task Status change alone) are not gated behind Permission Engine
evaluation by default, since AD-007 applies to what can have an external
effect, not to internal bookkeeping — but every path that reaches an
`ExecutionRequest` is gated unconditionally.

**Future considerations:** `PermissionEngine.evaluate` is not yet wired to
resolve identity first (`IMPLEMENTATION_GAPS.md` #40); cross-Principal
Task control actions (e.g. cancelling another Principal's Task) are not
Permission-Engine-gated by default today, disclosed explicitly as an open
question in `TaskManagerRuntimeSpecification.md` §8. Both are
enforcement-completeness or scope-boundary gaps, not violations of this
decision. Gap #40's real-world stakes have increased since Sprint 1 Unit
11A: before Unit 11A, an incorrect `APPROVED` decision had no downstream
consequence (no Tool ever ran); after Unit 11A, an incorrect `APPROVED`
decision now causes a real `Tool.execute()` call. The gap itself is
unchanged; its consequence is not.

---

### AD-008 — Identity Decisions Belong to Identity Service

**Status:** Accepted

**Decision:** Subsystems never invent or mutate identity. `updateStatus`
is the only sanctioned way to move a Principal through its lifecycle, and
no other component writes a `Principal` record directly.

**Reasoning:** Identity is the foundation AD-001 depends on; if any
consumer could mutate a Principal's own state, "who is requesting this"
would no longer have one authoritative answer.

**Evidence:** `docs/architecture/IdentityService.md` ("`updateStatus` is
where the Identity Service becomes the single enforcement point for this
state machine — no other component may write a Principal's `status`
field directly"); restated in `AgentRuntimeSpecification.md` §11 ("No
mutation of identity or permission state except through authorised
runtime services"), `TaskManagerRuntimeSpecification.md` §12 ("No
identity mutation"), `PlannerRuntimeSpecification.md` §13 (implicitly, by
never defining an identity-mutating operation).

**Affected specifications:** All three Phase 3 specifications,
`IdentityService.md`.

**Consequences:** Revoked or suspended Principal detection is always
reported by the Identity Service, never independently detected by a
consumer — every specification that depends on this explicitly frames it
as a dependency, not an assumption.

**Future considerations:** `resolve()` does not yet suppress non-Active
Principals (#37); `identity.*` events are not yet published (#39); the
exact cascading-revocation rule remains undecided (#35). All three are
enforcement-completeness gaps in this decision, recorded identically
across all three Phase 3 specifications.

---

### AD-009 — Everything Important Is Auditable

**Status:** Accepted

**Decision:** Every meaningful lifecycle transition emits an event onto
the EventBus, namespaced `<domain>.<event>`, so that Chapter 43 (Audit
and Observability) has the same visibility into any subsystem's activity.

**Reasoning:** A trust architecture that cannot reconstruct what happened
and why is not meaningfully auditable regardless of how carefully
permission and identity are enforced at the moment of action.

**Evidence:** `docs/specifications/volume-03-core-interfaces/EventBus.md`;
`docs/specifications/volume-03-core-interfaces/EventType.md`'s
`<domain>.<event>` convention; `TaskManagerRuntimeSpecification.md` §10
(19-event table) and §12 ("No unaudited lifecycle transition");
`AgentRuntimeSpecification.md` §9 (17-event table) and §11 ("No
unaudited action execution"); `PlannerRuntimeSpecification.md` §11
(13-event table) and §13.

**Affected specifications:** All three Phase 3 specifications, `EventBus.md`,
`EventType.md`.

**Consequences:** Every real lifecycle transition in the Task Manager
Task, Agent Run, and Planning Session lifecycles has a corresponding
event, with one disclosed exception (the Planner's `SUBMITTED --> REJECTED`
transition has no dedicated event yet, pending the Task Manager response
contract — AD-005's future considerations).

**Clarification (Architecture v1.1):** Sprint 1 distinguishes three
concepts this Decision's single sentence does not separate by name:
**publication** (a producer calling `EventBus.publish` for a meaningful
lifecycle transition — Unit 9 exercises this across the `planner.*`,
`task.*`, and `agent.*` domains), **observation** (any subscriber
consuming published events — `EventCollector`, Unit 10, is the concrete
Sprint 1 example), and **audit reconstruction** (Chapter 43's own Audit
and Observability responsibility, which no component in this repository
implements). `EventCollector`'s own KDoc states this plainly: it is a
"test-only fixture... nothing in the specifications requires a
production auditing/collection component; this exists solely so a test
can reconstruct what a run actually published." "Auditable" as used in
this Decision's text means "structurally reconstructable by some future
or test-time subscriber" — Sprint 1 satisfies publication and
demonstrates observation is possible, without implementing audit
reconstruction itself. This clarifies the existing rule; it does not
narrow or weaken it — every meaningful lifecycle transition still MUST
emit an event.

**Future considerations:** Whether `task.*`, `agent.*`, and `planner.*`
should be formally added to `EventBus.md`'s trust-sensitive domain list is
recorded as an open question in every specification that defines one of
these domains.

---

### AD-010 — Model Independence

**Status:** Accepted

**Decision:** Platform contracts are independent of any specific
reasoning approach, model, or prompting strategy. No specification
assumes a particular model produces a Goal, a proposed action, or a Plan
Candidate.

**Reasoning:** Per ADR-001, whatever produces a proposal is upstream of
the runtime and holds no executable authority; keeping every contract
agnostic to what (or whether) a model sits upstream means the runtime's
own behaviour is deterministic and swapping reasoning approaches never
requires rearchitecting the runtime.

**Evidence:** ADR-001 (Models Never Execute Tools);
`AgentRuntimeSpecification.md` §2 ("Model independence... the Agent
Runtime would behave identically regardless of what — or whether a model
— sits upstream"); `PlannerRuntimeSpecification.md` §2 ("Model
independence") and §14 ("External models... upstream of this document and
interchangeable"); `docs/architecture/action-mapping.md`'s
Planner-owned, deterministic lookup table.

**Affected specifications:** Agent Runtime Specification, Planner
Runtime Specification, `action-mapping.md`, ADR-001.

**Consequences:** No specification requires, recommends, or precludes any
particular reasoning approach; Plan Candidate generation and proposed-
action generation are both explicitly swappable implementation details.

**Future considerations:** Integrating any specific external model
remains future, implementation-phase work
(`PlannerRuntimeSpecification.md` §14), not an architectural decision any
current specification makes.

---

### AD-011 — Context Is Reference-Based

**Status:** Accepted

**Decision:** The Planner Runtime, Task Manager Runtime, and Agent
Runtime each hold their own transient Context (Planning Context, Task
Context, Agent Context respectively) built from references to Resources,
Tasks, Agent Runs, and events — never by copying another component's
internal state, and never by directly owning Memory or World Model state.

**Reasoning:** Reference-based Context keeps each runtime's transient
state small, bounded, and traceable back to its source, and prevents the
three Context stores from silently duplicating or diverging from each
other or from a future Memory/World Model implementation.

**Evidence:** ADR-002 (Memory, Context and World Model Remain Separate);
`TaskManagerRuntimeSpecification.md` §9 ("Task Context is not Agent
Context... Each side reads the other only by resolving the reference
through its owning component"); `AgentRuntimeSpecification.md` §8;
`PlannerRuntimeSpecification.md` §9 ("Planning Context is temporary,
bounded, and auditable").

**Affected specifications:** All three Phase 3 specifications, ADR-002.

**Consequences:** No Context store persists beyond its owning session's,
run's, or task's active lifetime in any form other than the underlying
record itself (the Task, the Agent Result, the Task Proposal) and
whatever Audit retains of event history.

**Future considerations:** How a future Memory or World Model reference
would actually resolve is reserved as a seam, not specified, by all three
Context models.

---

### AD-012 — Memory and World Model Are Context Providers

**Status:** Accepted

**Decision:** Memory and the World Model, once specified, are read
sources that inform planning and context. Neither is, or may become, an
orchestration system with authority to trigger execution or mutate Task,
Agent Run, or Planning Session state.

**Reasoning:** Orchestration authority is already deliberately
concentrated in the Task Manager (AD-004), the Agent Runtime (AD-006),
and the Planner (AD-005); giving Memory or the World Model a write path
into any of their state would create a second, ungoverned orchestration
surface outside the chain those three decisions establish.

**Evidence:** ADR-002; every existing specification's Non-Goals section
excludes Memory and World Model implementation; `docs/architecture/IMPLEMENTATION_ORDER.md`
§6 ("Do not implement Memory or World Model as orchestration systems...
neither owns a Goal, a Task Manager Task, an Agent Run, or any lifecycle
transition"); `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` §7
(restating the same rule as cross-cutting).

**Affected specifications:** All three Phase 3 specifications, ADR-002,
`IMPLEMENTATION_ORDER.md`.

**Consequences:** A future Memory Specification or World Model
Specification must be written as a read/context interface, not an
execution-triggering one, to remain consistent with this decision.

**Future considerations:** The Memory Specification and World Model
Specification are both still-unwritten future work
(`IMPLEMENTATION_ORDER.md` §4, Order 2–3).

---

### AD-013 — Specifications Define Contracts

**Status:** Accepted

**Decision:** A specification describes an interface and its behaviour
before any implementation exists for it. No specification in this
repository is written alongside, or after, the Kotlin it describes,
except where a document explicitly backfills an already-built,
already-tested component (e.g. `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`).

**Reasoning:** Writing the contract first, and reviewing it before
implementation, is what makes later implementation an act of following an
already-agreed design rather than an act of inventing the design and the
code simultaneously.

**Evidence:** Every specification's own Status header states it is
"specification only... Nothing described here is authorised for
implementation until an explicitly-declared implementation phase promotes
it," citing the same pattern already used by `docs/architecture/IdentityService.md`,
`docs/architecture/tool-registry.md`, and `docs/architecture/action-mapping.md`
before each of their own implementation phases.

**Affected specifications:** All specifications in this repository.

**Consequences:** No specification's own creation, review, or correction
pass touches `src/` or `tests/`; the 101 tests referenced in
`docs/architecture/IMPLEMENTATION_ORDER.md` §2 exercise only the
already-implemented foundation, not any Phase 3 document.

**Implementation note (Architecture v1.1):** Sprint 1 exposed a case this
Decision's text does not name: a contract addition grounded in a
specification's own existing prose, ahead of that specification's
normative field list being updated to match. This is not "writing a
specification alongside the Kotlin it describes" (the Decision's already-
named backfill exception) — the specification predates the Kotlin in
both instances — but it is also not full alignment, since the field list
itself lags the prose. Two independent instances exist:
`AgentRunCommand`'s Suspend/Resume/Cancel shape (Blocker 3, pre-Sprint-1
contract closure) and `TaskProposal.resourceReferences` (Unit 11B,
grounded in `PlannerRuntimeSpecification.md` §9's own "a Plan Candidate
or Task Proposal may target" language, even though §10's field list did
not yet name the field). The corresponding specification field lists
should be updated to match where not already done. This is recorded as
an implementation note, not a change to the Decision text itself, which
remains correct.

**Future considerations:** None currently open against this decision
itself.

---

### AD-014 — Architecture Before Implementation

**Status:** Accepted

**Decision:** Platform architecture — a specification and its
cross-specification contracts — is completed, reviewed, and corrected
before major implementation begins on it.

**Reasoning:** Reviewing a specification's consistency against the rest
of the architecture (identity, permission, execution-pipeline, and
Task/Agent-Run boundaries) is cheaper and less risky to do on paper than
to discover the same issue after Kotlin has been written against a flawed
design.

**Evidence:** The review-and-correction-pass pattern already applied to
the Agent Runtime Specification (`docs/reviews/AgentRuntimeSpecificationReview.md`
→ correction pass) and the Task Manager Runtime Specification
(`docs/reviews/TaskManagerRuntimeSpecificationReview.md` → correction
pass); `docs/architecture/IMPLEMENTATION_ORDER.md` §6 ("All new design
documents should receive review and correction pass before
implementation"); `docs/reviews/Phase3ArchitecturePositionReview.md`,
which validates the same architecture across specifications rather than
within one.

**Affected specifications:** Agent Runtime Specification, Task Manager
Runtime Specification, Planner Runtime Specification (not yet reviewed —
see Future considerations), and any future specification.

**Consequences:** `docs/architecture/IMPLEMENTATION_ORDER.md`'s
recommended specification order (Planner, World Model, Memory, Workflow
Runtime, Android) is itself a consequence of this decision: each is
sequenced to be specified, reviewed, and corrected before the next
depends on it being stable.

**Future considerations:** The Planner Runtime Specification has not yet
received its own dedicated review-and-correction pass, unlike the Agent
Runtime and Task Manager Runtime Specifications; `docs/reviews/Phase3ArchitecturePositionReview.md`
reviewed it only as part of the cross-specification architecture check,
not as a standalone review of its own internal consistency.

---

### AD-015 — Invalid Is Not Denied

**Status:** Accepted

**Decision:** Invalid requests and denied requests are distinct outcomes,
and no component treats one as the other. **Invalid** means the request
is malformed, structurally impossible, references a missing or unknown
object, violates a schema, or otherwise cannot be evaluated at all.
**Denied** means the request was well-formed enough to evaluate, but
authority to perform it was not granted. Systems must not treat an
Invalid outcome as a permission denial, and must not treat a Denied
outcome as a schema or validation failure.

**Reasoning:** Conflating the two would let a well-formed but forbidden
action look identical, in an audit trail, to a nonsensical or malformed
one — weakening exactly the kind of explainability Chapter 43 (Audit and
Observability) depends on. Keeping them distinct also keeps the
Permission Engine's own semantics clean: it only ever renders a decision
about something it could actually evaluate, never about something that
was never well-formed enough to reach it.

**Evidence:** `docs/architecture/action-mapping.md` ("Unknown Actions":
"An unresolvable proposed action is treated as **invalid, not denied**
... Conflating the two would let a well-formed but forbidden action look
identical to a nonsensical one in audit logs, which would weaken
explainability"); `docs/architecture/IdentityService.md` ("Principal
Resolution": "An unresolvable `PrincipalId` (not found, or found but
`Revoked`/`Archived`) is treated the same way
`docs/architecture/action-mapping.md` treats an unresolvable proposed
action: **invalid, not denied**"); `AgentRuntimeSpecification.md` §10
("Malformed action... is, per `action-mapping.md`'s existing 'Unknown
Actions' section, **invalid, not denied** — the underlying
`ExecutionRequest` never reaches `PermissionPending`"); `TaskManagerRuntimeSpecification.md`
§8 ("An unresolvable Task Owner is invalid, not denied, mirroring
`Principal.md`'s existing rule and the Agent Runtime Specification's
identical treatment of an unresolvable Agent Identity"). Sprint 1 Unit
11A extends this same rule to a later, post-approval pipeline stage,
proven by `tests/runtime/DefaultExecutionPipelineTest.kt`: a resolved
Tool with no invocable `ToolInvocationBinding` produces a `FAILED`
result, never `DENIED`; a Tool that fails `validate()` produces a
`FAILED` result, never `DENIED`; and a Tool whose `execute()` reports
failure produces a `FAILED` result, never `DENIED` —
`DefaultExecutionPipeline.executeResolvedTool`'s own KDoc cites this
Decision by name. These are the first Evidence instances of this
Decision applying after `PermissionEngine.evaluate` has already
approved a request, rather than before it.

**Affected specifications:** `action-mapping.md`, `IdentityService.md`,
Agent Runtime Specification, Task Manager Runtime Specification. The
Planner Runtime Specification independently applies the same underlying
distinction in its own terms (`FAILED` vs. `REJECTED`, §5: "`FAILED`
means the Planning Session itself could not produce a well-formed,
submittable Task Proposal... `REJECTED` means a well-formed Task Proposal
was produced and submitted, but the Task Manager Runtime declined all of
it"), without using the "invalid/denied" vocabulary verbatim.

**Consequences:** Permission Engine decisions remain semantically clean —
a `PermissionDecisionOutcome` is only ever rendered for something that
was well-formed enough to evaluate. Validation failures remain separate
from authorisation failures throughout the platform. Audit trails can
distinguish "this request made no sense" from "this request was refused,"
which is a meaningfully different fact for anyone reviewing platform
behaviour after the fact.

**Future considerations:** None currently open against this decision
itself; individual specifications remain free to name their own
Invalid/Denied-equivalent outcomes in their own vocabulary (e.g. the
Planner Runtime Specification's `FAILED`/`REJECTED`), provided the
underlying distinction is preserved.

---

### AD-016 — Terminal Lifecycle States Are Final

**Status:** Accepted

**Decision:** When a runtime object enters a terminal lifecycle state, it
does not resume. A completed, failed, cancelled, expired, rejected, or
superseded lifecycle object is not restarted or mutated back into an
active state in place. Further work requires a new lifecycle object — a
new Agent Run, a new Task Manager Task, a new Planning Session, or
another explicit replacement object, depending on the subsystem.

**Reasoning:** A lifecycle whose terminal states could be reopened would
make "what happened" ambiguous after the fact — was this object always
in this state, or did it get there, leave, and come back? Treating
terminal states as genuinely final keeps every lifecycle deterministic to
reason about and keeps the audit history of a terminal object stable
once written.

**Evidence:** `docs/specifications/volume-02-core-schemas/Task-Schema.md`
("No transition out of any terminal state (Completed, Failed, Cancelled,
Expired, Superseded)"); ADR-012 (Task and Workflow Separation, the
governing boundary that a Task tracks a single unit of work rather than a
resumable, branching process); `AgentRuntimeSpecification.md` §5 ("Any
transition out of `COMPLETED`, `FAILED`, or `CANCELLED`... matching every
other lifecycle state machine in this repository
(`ExecutionLifecycleState`, `ToolLifecycleTransitions`,
`PrincipalLifecycleTransitions`, and the Task Manager Task lifecycle
itself) — none of which permit resurrection out of a terminal state");
`TaskManagerRuntimeSpecification.md` §5 ("No transition out of any
terminal state" and "Retryable states... **None, by transition**...
'Retry' at the Task Manager Runtime level means creating a **new** Task
Manager Task"); `PlannerRuntimeSpecification.md` §5 ("Any transition out
of `COMPLETED`, `REJECTED`, `CANCELLED`, or `FAILED`... matching every
other lifecycle state machine in this repository"). Sprint 1 added three
new lifecycle machines that each independently enforce this same rule --
`PlanningSessionLifecycleTransitions`, `TaskLifecycleTransitions`, and
`AgentRunLifecycleTransitions` -- with dedicated negative tests per
`SPRINT_1_VERTICAL_SLICE_PLAN.md` §7's requirement for a terminal-state
test at each new lifecycle introduced: `tests/runtime/DeterministicPlannerHarnessTest.kt`
("SUBMITTED is terminal for this fixed harness"), and contract-level
tests for `TaskLifecycleTransitions` and `AgentRunLifecycleTransitions`.

**Affected specifications:** Task Manager Runtime Specification, Agent
Runtime Specification, Planner Runtime Specification, `Task-Schema.md`,
ADR-012, and the already-implemented `ExecutionLifecycleState`,
`ToolLifecycleTransitions`, and `PrincipalLifecycleTransitions` state
machines.

**Consequences:** Lifecycle reasoning remains deterministic across every
subsystem: a terminal state is always a dead end for that specific
object. Audit history remains stable, since a terminal object's record is
never rewritten by a later resumption. Recovery from a terminal state
(retrying a failed Task, restarting a cancelled Agent Run, re-planning
after a rejected proposal) always happens through a new lifecycle object,
never by mutating the terminal one — consistent with ADR-018's identical
precedent for `ExecutionRequest` ("changes require creation of a new
ExecutionRequest linked by correlation ID").

**Future considerations:** None currently open against this decision
itself.

## 5. Relationship to Specifications

```text
Architecture Decisions
  ↓
Specifications
  ↓
Inter-specification Contracts
  ↓
Implementation
```

Architecture Decisions (Section 4) are the platform-wide rules every
specification is written to satisfy. Specifications
(`AgentRuntimeSpecification.md`, `TaskManagerRuntimeSpecification.md`,
`PlannerRuntimeSpecification.md`, and the Volume 1/3 interface documents)
apply those rules to one component's own lifecycle, events, and
boundaries. Inter-specification Contracts
(`docs/architecture/INTER_SPECIFICATION_CONTRACTS.md`) catalogue the
seams between specifications — which object one produces that another
consumes, and whether that seam is already real or still an open
dependency. Implementation (Kotlin under `src/`, exercised by `tests/`)
follows only once a specification, and the contracts it depends on, are
stable enough to build against — per AD-014 and
`docs/architecture/IMPLEMENTATION_ORDER.md`'s own ordering rule.

A change flows top-down: a new or revised Architecture Decision would
require revisiting every specification and contract downstream of it
(Section 7). A gap discovered at the Inter-specification Contracts level
(e.g. the Task Proposal intake contract) is resolved by revising the
affected specification(s), not by revising an Architecture Decision — the
decisions in Section 4 are already satisfied by the *intent* of both the
Planner Runtime Specification and the Task Manager Runtime Specification;
what is missing is the contract shape between them, not agreement on the
underlying rule.

This diagram describes an **authority ordering** — which document wins
when two levels appear to disagree — not the historical order in which
these documents were written. All sixteen decisions in
Section 4 were themselves distilled *from* already-existing, already-
approved and reviewed specifications, not authored independently ahead of
them (Section 1). Going forward, that direction reverses: these decisions
now serve as governance constraints any *future* specification (Memory,
World Model, Workflow Runtime, Android integration, or a Planner Runtime
correction pass) is expected to satisfy, the same way the existing three
Phase 3 specifications already do. Where a contract gap or an
inter-specification inconsistency is found, it should be closed by
revising the affected specification(s) or the
`docs/architecture/INTER_SPECIFICATION_CONTRACTS.md` catalogue entry that
describes it — never by weakening, narrowing, or quietly reinterpreting
an already-accepted decision in Section 4 to make the gap appear closed.

## 6. Decision Lifecycle

- **Proposed.** A candidate platform-wide rule has been identified —
  typically because a new specification would otherwise have to invent it
  independently, or because a cross-specification review
  (`docs/reviews/Phase3ArchitecturePositionReview.md`-style) surfaces a
  rule multiple specifications already assume without stating it as a
  shared decision. Not yet recorded in Section 4.
- **Accepted.** The decision is recorded in Section 4, evidenced by
  existing specifications, and governs how future specifications must be
  written. All sixteen decisions in Section 4 are Accepted.
- **Superseded.** A later decision explicitly replaces an earlier one,
  with both entries cross-referencing each other and the superseded
  entry's Status updated accordingly. No decision in this document is
  currently Superseded.
- **Deprecated.** A decision no longer reflects platform intent but has
  not yet been formally replaced. No decision in this document is
  currently Deprecated.
- **Rejected.** A candidate decision was proposed and considered but not
  adopted, recorded here (if at all) only for historical traceability. No
  decision in this document is currently Rejected.

## 7. Future Decisions

A new Architecture Decision should be added to Section 4 only when:

- **Multiple specifications depend on it.** A rule that appears in
  exactly one specification and nowhere else is that specification's own
  concern, not a platform-wide decision.
- **It represents a platform-wide rule**, not an implementation detail or
  a single component's internal behaviour. Section 4's existing entries
  are all rules about how subsystems relate to each other (identity,
  authority, execution, ownership, auditability), not about any one
  subsystem's internal mechanics.
- **Changing it would affect multiple subsystems.** If revising the rule
  would require revisiting only one specification, it is that
  specification's own decision to make, not a platform-wide one recorded
  here.

A new entry should follow the same evidence-first standard already
applied throughout Section 4: it must be evidenced by what existing
specifications already state or already depend on, not introduced as a
new, speculative principle. Where a candidate decision is not yet
evidenced this way, it belongs in a specification's own Open Questions
section until enough specifications converge on it independently to
justify recording it here.

**Future consideration, not yet a decision.** Parker's existing
specifications show a repeated preference for reusing an existing schema,
enum, or contract over inventing a parallel vocabulary — for example,
`TaskManagerRuntimeSpecification.md` §4 reuses `RequestOrigin` and
`RequestPriority` (`src/contracts/ExecutionRequest.kt`) for Task Source
and Task Priority rather than defining new enums, and
`PlannerRuntimeSpecification.md` §4 independently does the same for its
own Source and Priority fields, plus reuses `RiskEstimate` for Risk. This
is evidenced twice, independently, but is narrower in scope (a
data-modelling convention) than the decisions in Section 4, which are
load-bearing rules about how subsystems relate to each other. It is
recorded here as a pattern worth watching, not promoted to an Architecture
Decision — if a third or fourth specification independently repeats it,
it would meet Section 7's criteria and should be added at that point,
rather than being adopted now on two data points alone.

## 8. Related Documents

- `docs/architecture/IMPLEMENTATION_ORDER.md`
- `docs/architecture/INTER_SPECIFICATION_CONTRACTS.md`
- `docs/reviews/Phase3ArchitecturePositionReview.md`
- `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
- `docs/specifications/volume-05-task-manager-runtime/TaskManagerRuntimeSpecification.md`
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
- `docs/reviews/AgentRuntimeSpecificationReview.md`
- `docs/reviews/TaskManagerRuntimeSpecificationReview.md`
- `docs/architecture/IdentityService.md`
- `docs/specifications/volume-02-core-schemas/Task-Schema.md`
- `docs/adr/ADR-012-task-and-workflow-separation.md`
- `docs/specifications/volume-03-core-interfaces/ExecutionPipeline.md`
- `docs/specifications/volume-03-core-interfaces/PermissionEngine.md`
- `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`
- `docs/architecture/tool-registry.md`
- `docs/specifications/volume-03-core-interfaces/ResourceRegistry.md`
- `docs/specifications/volume-03-core-interfaces/EventBus.md`
- `docs/specifications/volume-03-core-interfaces/EventType.md`
- `docs/architecture/action-mapping.md`
- `docs/specifications/volume-01-core-contracts/Principal.md`
- `docs/architecture/IMPLEMENTATION_GAPS.md`
- `docs/adr/` (existing per-topic Architecture Decision Records, e.g.
  ADR-001, ADR-002, ADR-003, ADR-013)
