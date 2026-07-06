# Memory Runtime Architecture

## Status

Sprint: Sprint 4, Track A, Unit A1 (Architecture)
Version: 0.1-draft
Status: **Design proposal, not yet reviewed or accepted.**

**This document is architecture only.** No Kotlin is implemented,
proposed as a diff, or changed by it. Neither `src/` nor `tests/` is
touched. `docs/implementation/IMPLEMENTATION_HISTORY.md` and
`docs/architecture/IMPLEMENTATION_GAPS.md` are both untouched. This
document does not redefine the Parker Constitution, the Architecture
Decisions, PES-001, the Runtime Foundation, the Planner Runtime, the
Agent Runtime, the Execution Pipeline, or the World Model principles
already adopted — it defines how Memory integrates with all of them,
without altering any of them.

### Why this unit exists

Sprint 3 completed Parker's Runtime Foundation: Identity, Trust, a
multi-step Agent Runtime, and a Planner Runtime that progresses Plan
Candidates into Task Proposals, all operating together
(`docs/reviews/SPRINT_3_RUNTIME_FOUNDATION.md`). That foundation has no
long-term knowledge layer of its own yet. Memory already exists in the
repository as a named concept — a one-paragraph architecture chapter, a
consolidation chapter, two one-line ADRs, and a `MemoryStore` interface
stub naming operations without specifying the fields they operate on —
but it has never been placed in relation to the Runtime Foundation Sprint
3 just completed. This unit does that placement. It is architecture, not
specification: it does not produce a field-level contract (that is a
future unit's job, mirroring Track D's Unit D1A), and it does not
implement anything.

## Review

Reviewed, in priority order:

1. `docs/architecture/parker-constitution.md` — "Memory and the World
   Model inform reasoning and proposals but carry no authority of their
   own to act."
2. `docs/architecture/ARCHITECTURE_DECISIONS.md`, especially AD-011
   (Context Is Reference-Based) and AD-012 (Memory and World Model Are
   Context Providers).
3. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001).
4. `docs/reviews/SPRINT_3_RUNTIME_FOUNDATION.md` — the Runtime Foundation
   this unit builds on top of.
5. `docs/specifications/volume-06-planner-runtime/PlannerRuntimeSpecification.md`
   and `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` — the
   Planner Runtime this unit must not redesign.
6. `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
   and the Track C design/implementation record — the Agent Runtime this
   unit must not redesign.
7. `docs/architecture/reasoning-context.md` — the existing, constitutional
   statement of the Memory / World Model / Reasoning Context split this
   unit must conform to, not restate differently.
8. `docs/architecture/16-world-model.md`, `docs/specifications/volume-03-core-interfaces/WorldModel.md`,
   `src/interfaces/WorldModel.kt`, `docs/adr/ADR-002-memory-context-world-model-separation.md`
   — the World Model principles already adopted.
9. `docs/architecture/17-memory-architecture.md`, `docs/architecture/33-memory-consolidation.md`,
   `docs/adr/ADR-008-memory-promotion.md`, `docs/specifications/volume-03-core-interfaces/MemoryStore.md`,
   `src/interfaces/MemoryStore.kt` — Memory's own existing, pre-Sprint-4
   scaffolding.
10. `docs/architecture/IMPLEMENTATION_ORDER.md` §5–6 — the existing
    dependency map placing Memory as a read-only context provider to the
    Planner, Task Manager, and Agent Runtime, and its explicit rule: "Do
    not implement Memory or World Model as orchestration systems."

### What already exists, and what this unit adds

Memory is not being invented here. Three things already exist and are
treated as given, not redefined:

- `docs/architecture/17-memory-architecture.md`: "Memory is Parker's
  long-term knowledge system... Memory is earned. Candidate memories are
  evaluated before promotion."
- `docs/architecture/33-memory-consolidation.md`: promotion factors
  (repetition, user importance, goal relevance, frequency, confidence,
  explicit request) and the rule that "Memory promotion is conservative
  and auditable."
- `src/interfaces/MemoryStore.kt` and its specification,
  `docs/specifications/volume-03-core-interfaces/MemoryStore.md`: an
  interface naming four operations — `addCandidate`, `promote`,
  `retrieve`, `forget` — over four types (`CandidateMemory`, `Memory`,
  `MemoryQuery`, `ForgetResult`) that are named but not yet field-defined
  anywhere in the repository. This is the same situation Track D's Unit
  D1 left for Track D's Unit D2: a concept named at the interface level,
  without a field-level contract behind it yet.

What this unit adds is the missing placement: how these existing,
named-only pieces relate to the Runtime Foundation, to each other, and to
the constitutional boundaries every Parker component inherits. It does
not add a field-level contract for `CandidateMemory`, `Memory`,
`MemoryQuery`, or `ForgetResult` — that remains a future unit's job, per
this document's own conclusion.

---

## 1. Purpose of Memory

**What Memory is.** Memory is Parker's long-term, durable knowledge
store. It holds what Parker has learned about the user and the world
that remains relevant beyond a single task or session: durable facts,
preferences, prior context, and history. Information enters Memory
deliberately, through evaluation and promotion, not automatically. Once
promoted, it persists across tasks, sessions, and time, until it is
retrieved, consolidated, or deliberately forgotten.

**What Memory is not.** Memory is not the World Model: it does not hold
current, transient state, and it is never updated directly by a sensor
or live input (`docs/architecture/16-world-model.md`; `WorldModel.md`'s
own normative requirement, "World Model MUST NOT become Memory"). Memory
is not Reasoning Context: it does not hold the temporary, task-scoped
working set a reasoning provider reasons over, and nothing in Memory is
assembled or discarded on a per-task basis
(`docs/architecture/reasoning-context.md`). Memory is not a reasoning
system: it does not interpret, infer, or generate proposals — it stores
and returns what has already been learned. Memory is not an execution or
orchestration system: it holds no Goal, no Task, no Agent Run, and no
Planning Session state, and it never triggers a lifecycle transition in
any of them (AD-012).

### Architectural Principle

Memory stores knowledge. Memory never decides. Memory never acts. Every
decision involving Memory belongs to another subsystem.

This is the single sentence every other section of this document
elaborates. It is stated here, once, as a first-class rule a future
contributor can quote on its own — not merely inferred by reading the
lifecycle, ownership, and interaction sections and noticing that they all
say the same thing.

## 2. Responsibilities

Memory owns long-term learned knowledge: the durable record of what has
been observed, evaluated, and promoted, and the mechanism for retrieving
it later.

Memory does not own:

- **Current world state.** What is true right now — device state,
  environment, the live status of an ongoing task — belongs to the World
  Model, not to Memory (`ADR-002`; `16-world-model.md`).
- **Reasoning.** Interpreting a request, deciding what to propose, or
  generating a Plan Candidate belongs to a reasoning provider or the
  Planner Runtime, not to Memory. Memory supplies knowledge that reasoning
  may draw on; it never reasons itself (`reasoning-context.md` §"Core
  Principles").
- **Execution.** Carrying out an authorized action belongs solely to the
  Execution Pipeline. Memory has no path to the Tool Registry, the
  Resource Registry, or any tool invocation.
- **Permissions.** Deciding whether an action or a disclosure is
  authorized belongs solely to the Permission Engine. Memory does not
  grant, evaluate, or withhold authorization — including for its own
  contents (`MemoryStore.md`: "Sensitive memories MUST require appropriate
  permission" is a requirement Memory is subject to, not one it enforces
  unilaterally).

## 3. Memory Categories

Memory is organized into architectural categories describing *what kind*
of long-term knowledge is held, not *how* it is stored. Storage format,
indexing, and retrieval algorithms are explicitly out of scope for this
unit (see Out of Scope, below); the categories below are a
classification of content, not a schema.

- **Episodic** — specific past events: what happened, when, and in what
  context. A record of things Parker observed or participated in.
- **Semantic** — general facts and knowledge that hold independent of any
  one event: who someone is, what something means, durable facts about
  the user's world.
- **Procedural** — how something is done: learned steps, routines, or
  approaches that proved useful and were promoted for reuse.
- **User Preferences** — durable statements of what the user wants,
  prefers, or has asked Parker to do or not do, distinct from a one-off
  instruction scoped to a single task.
- **Relationships** — durable facts about how people, places, tools, or
  things relate to the user and to each other.

These categories describe Memory's content, not its ownership or
lifecycle — every category is subject to the same lifecycle, ownership,
and constitutional boundaries defined below. This unit does not decide
whether these categories are represented as one Memory type with a
discriminating field, several distinct types, or something else — that
is a field-level contract decision for a future unit, not an
architectural one.

## 4. Lifecycle

Information progresses through Memory in one direction:

```
Observation
   |
   v
Candidate Memory
   |
   v
Evaluation
   |
   v
Promotion
   |
   v
Long-term Memory
   |
   v
Retrieval
```

- **Observation** is anything Parker encounters that might be worth
  remembering: something stated during a task, something inferred during
  reasoning, or something already resolved by the World Model. An
  observation, by itself, is not Memory and is not retained.
- **Candidate Memory** is an observation proposed for retention.
  Proposing a Candidate Memory carries no authority of its own — it is
  cognition proposing (per the Constitution's central discipline), not a
  durable record yet.
- **Evaluation** weighs a Candidate Memory against Memory's own
  promotion policy — the factors `docs/architecture/33-memory-consolidation.md`
  already names (repetition, user importance, goal relevance, frequency,
  confidence, explicit request) — and decides whether it is worth
  retaining, conservatively and auditably.
- **Promotion** is the deliberate act of moving an evaluated Candidate
  Memory into long-term Memory. Promotion is never automatic and never a
  side effect of a Candidate Memory merely having been proposed, observed,
  or discussed (`reasoning-context.md`: "Promotion into Memory is never
  automatic").
- **Long-term Memory** is the durable record: what Parker has learned,
  available beyond the task or session that produced it, until
  consolidated, retained past its usefulness, or deliberately forgotten.
- **Retrieval** is how long-term Memory is read back out — by the
  Planner, the Agent Runtime, or Reasoning Context assembly — as a
  read-only operation that never itself alters what is stored. Retrieval
  returns the most relevant memories matching the supplied query, not an
  unfiltered dump of everything stored; ranking strategy is
  implementation-defined and is not specified by this document.

This lifecycle does not by itself define Consolidation, Retention, or
Deletion as separate stages; those are covered under Ownership below, as
governance activities that act on Long-term Memory after promotion, not
additional stops on the Observation-to-Retrieval path. This section
describes progression, not implementation: no method signature, storage
mechanism, or algorithm is specified by it.

### Runtime Flow

The lifecycle above describes Memory's own internal progression. Placed
in the context of a full Parker interaction, it sits between reasoning
and retrieval like this:

```
User
   |
   v
Planner
   |
   v
Reasoning
   |
   v
Candidate Memory
   |
   v
Memory Promotion Policy
   |
   v
Memory Store
   |
   v
Retrieval
   |
   v
Reasoning Context
```

This is illustrative, not a redefinition of `reasoning-context.md`'s own
information-flow diagram, which remains authoritative for how Memory and
the World Model feed Reasoning Context. It exists only to show, in one
picture, where the Lifecycle stages above sit relative to the Planner,
reasoning, and the next task's Reasoning Context.

## 5. Ownership

| Activity | Owning subsystem |
| --- | --- |
| Observation | Whatever subsystem encounters the information first — a reasoning provider, the Planner Runtime, the Agent Runtime, or the World Model resolving a fact worth surfacing. Observation is not Memory's own act; Memory receives what is observed elsewhere. |
| Promotion | Memory, applying Parker's memory policy (the evaluation criteria in `33-memory-consolidation.md`). No other subsystem may promote a Candidate Memory into Long-term Memory on Memory's behalf. |
| Retrieval | Memory, in response to a query from an authorized caller. Memory decides what is returned for a given query; it does not decide who is allowed to ask. |
| Consolidation | Memory. Combining, deduplicating, or summarising related Long-term Memory records is a Memory-internal governance activity, not something any other subsystem performs on Memory's behalf. |
| Retention | Memory, subject to the same conservative, auditable policy that governs promotion. Deciding how long a Long-term Memory record remains eligible for retrieval before it must be reconsidered is Memory's own responsibility. |
| Deletion | Memory, via the existing `forget` operation. Deletion (or forgetting) must remain auditable (`MemoryStore.md`: "Forgetting MUST be auditable") — the record of the deletion having occurred is retained even though the memory itself is not. |

No subsystem outside Memory performs Promotion, Retrieval, Consolidation,
Retention, or Deletion on Memory's internal records. This is a direct
consequence of AD-012: Memory is a context provider, never an
orchestration system, and no other component reaches into it to mutate
its state directly.

**Ownership of Candidate Memory.** The Observation row above is
deliberately not Memory's own act — an Observation belongs to whichever
subsystem first encounters it. That changes the instant an Observation is
submitted as a Candidate Memory: Memory owns Candidate Memory from the
moment it is submitted for evaluation, regardless of which subsystem
submitted it. This is stated explicitly, not left implicit, because more
than one subsystem — the Planner, the Agent Runtime, a plugin, a
workflow — may submit Candidate Memories concurrently (see Input Sources,
below); without an explicit ownership boundary at the point of
submission, it would be ambiguous which subsystem is responsible for a
Candidate Memory while it awaits Evaluation. There is exactly one answer:
Memory is.

## 6. Input Sources

Memory never observes the world itself. It has no sensor, no direct
access to a task, and no standing visibility into a Planning Session or
Agent Run — it only ever receives what another subsystem submits to it as
a Candidate Memory.

Candidate Memory may originate from:

- the Planner Runtime,
- the Agent Runtime,
- the World Model,
- a direct user instruction,
- a plugin,
- a workflow, or
- a future reasoning provider.

None of these gains promotion authority merely by submitting a Candidate
Memory. Submission is cognition proposing, per the Constitution's central
discipline; only Memory's own promotion policy (§7) decides whether a
submitted Candidate Memory is promoted. This list is illustrative of
today's known sources, not closed by architectural necessity — a future
source of Candidate Memory does not require revisiting this document, as
long as it, too, only submits and never promotes on its own behalf.

## 7. Interfaces

This section identifies the public contracts Memory will eventually
require, and each one's responsibility — not Kotlin signatures, which
remain future, field-level contract design work.

- **A Memory read/write contract**, already named in this repository as
  `MemoryStore` (`src/interfaces/MemoryStore.kt`,
  `docs/specifications/volume-03-core-interfaces/MemoryStore.md`),
  responsible for: accepting a Candidate Memory, promoting an evaluated
  Candidate Memory into Long-term Memory, retrieving Long-term Memory
  matching a query, and forgetting a Long-term Memory record. This
  interface already exists at the operation-naming level. Its four
  supporting types — a Candidate Memory representation, a promoted
  Memory representation, a Memory query representation, and a forget
  result representation — are named by that interface but have no
  field-level definition anywhere in the repository yet. This is the same
  situation Track D's Unit D1 left open for Unit D2, and it is not
  resolved by this document; a future contract design unit must define
  those fields before implementation, exactly as Unit D1A did for the
  Planner Runtime. Long-term Memory records are individually
  identifiable — every other long-lived object Parker owns already is
  (`AgentId`, `TaskId`, `ResourceId`, `PlanCandidateId`), and Memory is no
  exception. The exact identifier contract (its type, its uniqueness
  scope) is left to that same future contract-design phase; this document
  only confirms that identifiability is required, not what form it takes.
- **`MemoryPromotionPolicy`**, a named architectural seam responsible for
  evaluating a Candidate Memory against the promotion factors
  `33-memory-consolidation.md` already names and producing a
  promote/reject decision. `MemoryPromotionPolicy` SHALL be a replaceable
  architectural seam, described exactly the way `AgentPolicy` (Agent
  Runtime) and `PlanDecision` (Planner Runtime) already are: a small
  decision-making seam separate from the store itself, so that how
  promotion is decided can evolve independently of how Memory is stored
  and retrieved. This gives Parker three parallel policy seams, one per
  Runtime Foundation component that must decide something without being
  the thing that stores or executes:

  ```
  AgentPolicy           -- Agent Runtime:   which step runs next
  PlanDecision          -- Planner Runtime: which candidate is selected
  MemoryPromotionPolicy -- Memory:          which candidate is promoted
  ```

  Naming `MemoryPromotionPolicy` now, at the architecture stage, is
  deliberate: it is the same discovery Unit D1A made for the Planner
  Runtime, applied here before implementation rather than after it.
- **A retention/consolidation contract**, responsible for the ongoing
  governance of already-promoted Long-term Memory: deciding when related
  records should be consolidated, and when a record's retention should be
  reconsidered. This is distinct from the promotion policy above, which
  governs entry into Memory, not its ongoing lifecycle once there.
- **A Memory query contract**, responsible for describing what a caller
  is asking Memory to retrieve — scoped, at minimum, by requesting
  Principal and by task relevance — without itself performing retrieval;
  the query is a request shape, `MemoryStore.retrieve` (or its eventual
  successor) is the operation that acts on it.

No additional public contract beyond these four is identified as required
for Memory to take its place in the Runtime Foundation. Categories of
Memory (§3) are not, by themselves, assumed to require category-specific
public interfaces; that determination is left to the future contract
design unit that gives these interfaces their fields.

## 8. Interaction with Planner Runtime

```
Planner
   |
   v
Memory
   |
   v
Planner
```

The Planner Runtime may consult Memory as a read source when assembling
its own Planning Context, consistent with AD-011 (Context Is
Reference-Based): Memory is referenced, not copied into the Planner's own
state. A Planner Runtime session may query Memory for durable knowledge
relevant to the Goal it is planning against — prior preferences, prior
outcomes, known relationships — and Memory returns whatever Long-term
Memory records match that query.

Memory does not perform planning. It does not generate a Plan Candidate,
evaluate one, select a winner, or produce a Plan Decision Result — all of
that remains entirely the Planner Runtime's and its `PlanDecision`
mechanism's responsibility, unchanged by this document. Memory's role
ends at returning knowledge; interpreting that knowledge into a plan is
cognition's job, carried out by the Planner Runtime (and whatever
reasoning provider it is paired with), never by Memory itself.

## 9. Interaction with World Model

Memory and the World Model answer different questions and must remain
separate, per `ADR-002` and `docs/architecture/reasoning-context.md`,
neither of which this document redefines:

- **Memory** = what Parker has learned. Durable, deliberately promoted,
  slow to change, and retained across tasks, sessions, and time.
- **World Model** = what Parker currently believes. Transient, sourced,
  timestamped, confidence-scored, and expected to change frequently as
  sensors and other live inputs update it (`16-world-model.md`).

Neither may become the other. `WorldModel.md`'s own normative requirement
— "World Model MUST NOT become Memory" — is reaffirmed here from
Memory's side: Memory must not treat a World Model observation as
already-promoted knowledge merely because it exists. If a World Model
observation is ever worth retaining beyond its transient relevance, it
must enter Memory through the same Observation → Candidate Memory →
Evaluation → Promotion path as any other observation — never through a
silent copy or an automatic mirroring of World Model state into Memory.

## 10. Interaction with Runtime

Identity, Trust (the Permission Engine), the Planner, the Agent Runtime,
the Execution Pipeline, and the Task Manager may each consult Memory as a
read source. None of them is owned by Memory, and Memory owns none of
them (AD-012):

- **Identity** resolves who is asking. Memory may scope retrieval to a
  requesting Principal, but Identity Service alone establishes who that
  Principal is; Memory never performs identity resolution itself.
- **Trust (the Permission Engine)** authorizes disclosure of sensitive
  memories. Per `MemoryStore.md`'s own normative requirement, "Sensitive
  memories MUST require appropriate permission" — Memory defers that
  authorization decision to the Permission Engine exactly as every other
  component does; it does not evaluate its own permission checks.
- **The Planner Runtime** consults Memory as described in §8, as a
  read-only input to planning; it does not gain any orchestration
  authority over Memory in return.
- **The Agent Runtime** may consult Memory as part of assembling context
  for a step, exactly as it references Task, Resource, or event data
  today (AD-011) — Memory does not drive an Agent Run's step transitions
  or lifecycle.
- **The Execution Pipeline** has no direct relationship to Memory.
  Whatever knowledge Memory contributed has already been folded into a
  proposal, and then authorized, long before execution; the Execution
  Pipeline executes what was authorized and has no cause to query Memory
  itself.
- **The Task Manager** may reference Memory read-only when it is useful
  context for a Task Proposal's provenance, exactly as it references
  other Context sources today; it does not gain a write path into Memory,
  and Memory does not own any Task, Task Proposal, or Task lifecycle
  state.

Memory must not own any of these six systems, their state, or their
lifecycle transitions, per AD-012's existing rule that Memory and the
World Model are context providers, never orchestration systems.

## 11. Constitutional Boundaries

Confirmed, without exception:

- **Memory never executes.** It has no path to the Tool Registry, the
  Resource Registry, or any tool invocation.
- **Memory never authorises.** It has no path to grant, evaluate, or
  override a Permission Engine decision — including for its own,
  sensitive contents.
- **Memory never plans.** It does not generate, evaluate, or select a
  Plan Candidate, and it does not decide what Parker should do next.
- **Memory never reacts autonomously to events.** Promotion happens only
  through Evaluation against `MemoryPromotionPolicy`, never as a direct
  response to an event crossing the Event Bus. An event may prompt a
  subsystem to submit a Candidate Memory, but no event, by itself, ever
  promotes one — there is no `Event -> Memory promotes automatically`
  path anywhere in this architecture, and none may be added without
  revisiting this document first.
- **Memory provides knowledge only.** Every interaction described in this
  document — with the Planner, the Agent Runtime, the Task Manager, the
  Execution Pipeline, Identity, and Trust — is Memory being read from,
  never Memory directing any of them.

This is a direct instance of the Constitution's own rule: "Memory and the
World Model inform reasoning and proposals but carry no authority of
their own to act." Nothing in this document grants Memory authority the
Constitution withholds from it, and nothing in this document is required
to make that true — it is already true of every existing component
Memory integrates with.

## Out of Scope

This document does not design: embeddings, vector databases, LLM prompts,
storage engines, indexing, retrieval algorithms, persistence, or Android
APIs. Those belong to later units — specifically, to the field-level
contract design and implementation units that follow this one, once this
architecture has been reviewed and accepted.

Memory remains independent of any retrieval technology. Nothing in this
document, or in any future contract design that follows it, should be
read as fixing Memory to a particular storage or retrieval mechanism —
"Memory" names an architectural role, not a vector database, an
embedding model, or any other specific technology that might one day
implement it.

## Conclusion

This document places Memory inside Parker's existing Runtime Foundation
without redefining the Constitution, the Architecture Decisions,
PES-001, the Planner Runtime, the Agent Runtime, the Execution Pipeline,
or the World Model principles already adopted. It identifies Memory's
purpose, its responsibilities and non-responsibilities, its architectural
categories, its lifecycle, the ownership of each lifecycle and
governance activity, the public contracts it will eventually require,
and its interaction with the Planner Runtime, the World Model, and the
rest of the Runtime. It confirms that Memory never executes, never
authorises, never plans, never reacts autonomously to events, and
provides knowledge only — consistent with, and required by, the Parker
Constitution.

Track A architecture is now sufficiently defined to permit contract
design without introducing new architectural concepts.
