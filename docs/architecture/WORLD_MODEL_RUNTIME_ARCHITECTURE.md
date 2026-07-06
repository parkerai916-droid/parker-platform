# World Model Runtime Architecture

## Status

Sprint: Sprint 4, Track B, Unit B1 (Architecture)
Version: 0.1-draft
Status: **Design proposal, not yet reviewed or accepted.**

**This document is architecture only.** No Kotlin is implemented,
proposed as a diff, or changed by it. Neither `src/` nor `tests/` is
touched. `docs/implementation/IMPLEMENTATION_HISTORY.md` and
`docs/architecture/IMPLEMENTATION_GAPS.md` are both untouched. This
document does not redefine the Parker Constitution, the Architecture
Decisions, PES-001, the Runtime Foundation, `reasoning-context.md`, or the
Memory Runtime Architecture already adopted — it defines how the World
Model integrates with all of them, without altering any of them. It
serves the same role for Track B that `MEMORY_RUNTIME_ARCHITECTURE.md`
served for Track A, `MULTI_STEP_AGENT_RUN_DESIGN.md` served for Track C,
and `PLANNER_RUNTIME_PROGRESSION_DESIGN.md` served for Track D.

## Review

Reviewed, in priority order:

1. `docs/architecture/parker-constitution.md` — "Cognition proposes.
   Trust authorises. Runtime executes."; "Memory and the World Model
   inform reasoning and proposals but carry no authority of their own to
   act."
2. `docs/architecture/ARCHITECTURE_DECISIONS.md`, especially AD-011
   (Context Is Reference-Based), AD-012 (Memory and World Model Are
   Context Providers — "Memory and the World Model, once specified, are
   read sources that inform planning and context. Neither is, or may
   become, an orchestration system with authority to trigger execution or
   mutate Task, Agent Run, or Planning Session state"), and AD-013
   (Specifications Define Contracts).
3. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001).
4. `docs/architecture/reasoning-context.md` — the constitutional-tier
   statement of the Memory / World Model / Reasoning Context split this
   document must conform to, not restate differently: "The World Model is
   Parker's live, current understanding of the state of the world
   relevant to the user... It represents Parker's best current belief,
   not a permanent record."
5. `docs/architecture/16-world-model.md` (Chapter 16) — "The World Model
   represents Parker's current understanding of reality. It answers: what
   does Parker currently believe is true? World information is transient,
   sourced, timestamped and confidence-scored."
6. `docs/specifications/volume-03-core-interfaces/WorldModel.md` — the
   existing Purpose, Responsibilities, Required Operations, and Normative
   Requirements already adopted for the `WorldModel` interface.
7. `src/interfaces/WorldModel.kt` — the existing, currently-excluded-from-
   build interface stub naming `observe`, `current`, and `query` over
   four supporting types (`WorldObservation`, `ObservationResult`,
   `WorldState`, `WorldQuery`) that are named but not yet field-defined
   anywhere in the repository.
8. `docs/adr/ADR-002-memory-context-world-model-separation.md` — "Context
   stores immediate conversation state. World Model stores current
   reality. Memory stores long-term knowledge."
9. `docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md` and
   `docs/architecture/MEMORY_CONTRACT_DESIGN.md` — Track A's own,
   already-accepted placement of Memory, including Memory's own stated
   side of the Memory/World Model boundary (§9 of that document), which
   this document must integrate with and must not contradict.
10. `docs/architecture/PLANNER_RUNTIME_PROGRESSION_DESIGN.md` and
    `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` — the Planner
    Runtime this document must not redesign, and the precedent for how a
    replaceable decision seam (`PlanDecision`) is named and justified.
11. `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
    and the Track C design/implementation record — the Agent Runtime this
    document must not redesign.
12. `docs/architecture/IMPLEMENTATION_ORDER.md` §4–7 — recommending World
    Model be specified early because "a Planner that decomposes Goals
    into Tasks and Agent Runs plausibly needs to consult current-state
    beliefs (e.g. 'is it within the allowed time window') to do so
    meaningfully"; §6's explicit rule, "Do not implement Memory or World
    Model as orchestration systems... neither owns a Goal, a Task Manager
    Task, an Agent Run, or any lifecycle transition."

### What already exists, and what this unit adds

The World Model is not being invented here. Four things already exist and
are treated as given, not redefined:

- `docs/architecture/16-world-model.md`: the World Model "represents
  Parker's current understanding of reality," and world information "is
  transient, sourced, timestamped and confidence-scored."
- `docs/architecture/reasoning-context.md`: the World Model is "what
  Parker believes is true right now," expected to change frequently,
  "driven by sensors and other live inputs," and explicitly distinct from
  both Memory and Reasoning Context.
- `docs/specifications/volume-03-core-interfaces/WorldModel.md`: a
  Purpose, a five-item Responsibilities list (store transient state,
  track confidence, expire stale observations, resolve current state,
  publish state change events), and four Normative Requirements, one of
  which — "World Model MUST NOT become Memory" — is a constitutional-
  grade boundary this document reaffirms rather than restates
  differently.
- `src/interfaces/WorldModel.kt`: an interface naming three operations —
  `observe`, `current`, `query` — over four types (`WorldObservation`,
  `ObservationResult`, `WorldState`, `WorldQuery`) that are named but not
  yet field-defined anywhere in the repository. This is the same
  situation `MemoryStore.kt` was in before Track A's Unit A1/A2, and the
  same situation `PlanDecision` was in before Track D's Unit D1A: a
  concept named at the interface level, without a field-level contract
  behind it yet.

What this unit adds is the missing placement: how these existing,
named-only pieces relate to the Runtime Foundation, to Memory, to the
Planner, and to the constitutional boundaries every Parker component
inherits. It does not add a field-level contract for `WorldObservation`,
`ObservationResult`, `WorldState`, or `WorldQuery` — that remains a
future unit's job (a likely Unit B2, mirroring Track A's Unit A2 and
Track D's Unit D1A), and it is not decided by this document.

---

## 1. Purpose

**What the World Model is.** The World Model is Parker's live, current
belief about the state of the world relevant to the user: device state,
environment, ongoing activity, and other facts that change over time and
must be kept current. It answers exactly one question — "what does Parker
currently believe is true?" (`16-world-model.md`) — and nothing more.
World Model content is transient, sourced, timestamped, and
confidence-scored (`16-world-model.md`; `WorldModel.md`'s Normative
Requirements).

**How the World Model differs from Memory.** Memory is what Parker has
*learned*: durable, deliberately promoted, slow to change, and retained
across tasks, sessions, and time (`reasoning-context.md`;
`MEMORY_RUNTIME_ARCHITECTURE.md` §1). The World Model is what Parker
currently *believes*: transient, continuously updated by sensors and
other live inputs, and expected to change frequently. Memory answers "what
is true in general, based on what has been learned"; the World Model
answers "what is true right now." Neither may become the other
(`WorldModel.md`: "World Model MUST NOT become Memory"; this is addressed
further in §9, below).

**How the World Model differs from Reasoning Context.** Reasoning
Context is the temporary, task-scoped working set a reasoning provider
actually reasons over, assembled from relevant portions of Memory and the
World Model plus task-specific information, and discarded once the task
concludes (`reasoning-context.md`). The World Model is not task-scoped: it
persists independently of any one task, is continuously maintained
whether or not a task is currently running, and is one of the two sources
(alongside Memory) that Reasoning Context is assembled *from* — it is
never itself a Reasoning Context, and it is never discarded when a task
ends.

## 2. Architectural Principles

**World Model represents present belief. World Model never decides.
World Model never acts. Every decision about what to do with that belief
belongs to another subsystem.**

This is the anchor every other section of this document elaborates,
stated in the same form Track A's Memory Runtime Architecture states its
own Architectural Principle, so that the two knowledge layers are seen as
structurally parallel, not merely thematically similar.

**Current belief is replaceable. Historical knowledge is not.** The
World Model exists solely to represent Parker's best current
understanding of reality. Every belief is provisional and may be
replaced, invalidated, or expire. Permanence belongs exclusively to
Memory. This companion principle is what separates the World Model's
mutability (below) from carelessness: belief is expected to change, but
that expectation is itself architectural, not a gap to be closed later.

Four supporting principles follow directly from these two anchors, and
from the sources reviewed above:

- **The World Model is mutable.** Unlike Memory, which changes only
  through deliberate evaluation and promotion, the World Model is
  expected to change continuously as new Observations arrive
  (`reasoning-context.md`). Mutability is not a defect to be managed; it
  is the World Model's defining characteristic.
- **The World Model is never historical storage.** It holds current
  belief, not a log of past belief. When belief changes, the prior belief
  does not remain in the World Model as a retained record (see §5,
  Replacement) — if a historical record of a past belief is ever needed,
  that is a separate, deliberate act governed by Memory's own promotion
  path (§9), never something the World Model does on its own behalf.
- **The World Model never becomes Memory automatically.** No Observation,
  Update, or belief the World Model holds is ever copied into Memory as a
  side effect of existing in the World Model. The only path into Memory
  is Memory's own Observation → Candidate Memory → Evaluation → Promotion
  lifecycle (`MEMORY_RUNTIME_ARCHITECTURE.md` §4), which requires a
  deliberate submission by some subsystem — the World Model does not
  submit on its own initiative, and no other subsystem is authorised to
  treat a World Model belief as already-promoted knowledge merely because
  it exists.
- **The World Model is authoritative only until replaced.** A belief the
  World Model holds is Parker's best current understanding for as long as
  it stands, but it carries no permanence: the moment a new Observation
  supersedes it, is explicitly invalidated, or expires, it stops being
  authoritative (§5). "Authoritative" here means "the current answer to a
  query," not "guaranteed correct" or "permanently binding" — the World
  Model's confidence-scoring exists precisely because current belief can
  be wrong or stale.

## 3. Responsibilities

The World Model owns exactly the five responsibilities
`WorldModel.md` already establishes, unchanged by this document:

- **Store transient state** — hold Parker's current belief about the
  world, in a form that reflects what is true now, not what was true at
  some point in the past.
- **Track confidence** — every belief carries a confidence score,
  because current belief can be uncertain, partially observed, or in
  conflict with another source.
- **Expire stale observations** — a belief not reaffirmed by a new
  Observation within some policy-defined window must expire or degrade,
  never persist indefinitely as if it were still current (§5).
- **Resolve current state** — answer "what is currently true" for a
  given scope, deterministically and without ambiguity about which belief
  is authoritative when more than one Observation exists for the same
  scope.
- **Publish state change events** — make belief changes visible to the
  rest of the Runtime through the existing Event Bus mechanism, so that
  other components may observe that current belief changed without
  polling the World Model directly.

The World Model does not own:

- **Long-term knowledge.** What Parker has learned, durably, belongs to
  Memory, not to the World Model (`ADR-002`; `MEMORY_RUNTIME_ARCHITECTURE.md`
  §2). The World Model has no promotion mechanism of its own and does not
  decide what is "worth remembering" beyond its own transient purpose.
- **Reasoning.** Interpreting current belief, deciding what to propose, or
  generating a Plan Candidate belongs to a reasoning provider or the
  Planner Runtime, not to the World Model. The World Model supplies belief
  that reasoning may draw on; it never reasons itself
  (`reasoning-context.md` §"Core Principles").
- **Execution.** Carrying out an authorized action belongs solely to the
  Execution Pipeline. The World Model has no path to the Tool Registry,
  the Resource Registry, or any tool invocation.
- **Permissions.** Deciding whether an action or a disclosure is
  authorized belongs solely to the Permission Engine. The World Model does
  not grant, evaluate, or withhold authorization, including for its own
  contents.
- **Planning, Task, or Agent Run lifecycle.** The World Model owns no
  Goal, no Task, no Agent Run, and no Planning Session state, and it never
  triggers a lifecycle transition in any of them (AD-012).

## 4. Information Categories

The World Model is organized into architectural categories describing
*what kind* of current belief is held, not *how* it is stored. Storage
schemas, indexing, and persistence are explicitly out of scope for this
unit (see Out of Scope, below); the categories below classify content,
not structure.

- **Device state** — the current, live condition of a device or
  resource Parker can observe or act on: on/off, connected/disconnected,
  battery level, location, and similar directly-observable facts.
- **Environment** — ambient conditions surrounding the user or a device:
  time, location context, network availability, and similar
  externally-sourced facts not owned by any single device.
- **User activity** — what the user currently appears to be doing:
  present engagement with a task, a device, or an application, as
  distinct from a durable preference (Memory) or a task-scoped working
  set (Reasoning Context).
- **Runtime state** — the current status of in-flight Runtime activity
  as observed from the outside: for example, that an Agent Run or Task is
  currently active. The World Model may hold a read-only belief that
  such activity is occurring; it does not own the Agent Run or Task
  itself, and it is not the authoritative record of that lifecycle — the
  Agent Runtime and Task Manager remain that (§3; AD-012).
- **External services** — the current, live status of services outside
  Parker's own Runtime that a Tool or Agent Run might depend on:
  reachability, availability, or reported status.
- **Derived beliefs** — a belief computed from combining other current
  beliefs rather than sourced from a single direct Observation (for
  example, "the user is likely away from home," derived from device and
  environment beliefs together). A derived belief is still transient,
  sourced, timestamped, and confidence-scored exactly like any other
  belief; it differs only in that its immediate source is other current
  belief rather than a sensor. Derived beliefs remain hypotheses, not
  facts: their confidence reflects the confidence of the observations
  they were derived from, and no future reasoning provider or inference
  mechanism may treat a derived belief as equivalent in certainty to a
  direct Observation.

These categories describe the World Model's content, not its ownership or
lifecycle — every category is subject to the same lifecycle, ownership,
and constitutional boundaries defined below. This unit does not decide
whether these categories are represented as one belief type with a
discriminating field, several distinct types, or something else — that is
a field-level contract decision for a future unit, not an architectural
one.

## 5. Lifecycle

Belief progresses through the World Model as follows:

```
Observation
   |
   v
Validation
   |
   v
Update
   |
   v
Replacement / Invalidation / Expiry
```

- **Observation** is any reported signal about current reality submitted
  to the World Model — from a sensor, a plugin, an agent's own action, a
  user's direct statement, or a derivation from existing belief. An
  Observation, on its own, has not yet changed what the World Model
  currently believes.
- **Validation** checks that a submitted Observation is well-formed and
  eligible to be considered at all: it is timestamped, it carries a
  confidence score, and it identifies what it is an Observation *of*
  (`WorldModel.md`'s Normative Requirements: "World state MUST be
  timestamped," "World state MUST have confidence"). Validation is a
  structural check, not an authorization check — it has nothing to do
  with the Permission Engine, and it does not decide whether the
  Observation's *source* was allowed to submit it (that remains the
  Permission Engine's and Identity Service's separate concern, per §10).
- **Update** is the deliberate act of folding a validated Observation into
  current belief, governed by `WorldModelUpdatePolicy` (§7): given an
  existing belief for the same scope (if any) and a newly validated
  Observation, the policy decides whether the new Observation now
  represents current belief. An Update either establishes a first belief
  for a previously-unobserved scope, or supersedes an existing one.
  Multiple Observations may legitimately disagree about the same scope —
  two sources reporting conflicting device state, for example.
  Resolving contradictory Observations is `WorldModelUpdatePolicy`'s
  responsibility, not an edge case left for a future contract design
  unit to discover; this document names it explicitly so it is designed
  for from the start.
- **Replacement** is what happens to the belief an Update supersedes: it
  stops being current. The World Model does not retain the replaced
  belief as a historical record alongside the new one (Architectural
  Principles, above) — it holds current belief, not a log of prior
  belief. If a superseded belief is ever worth retaining beyond the
  moment of replacement, that is a separate, deliberate act through
  Memory's own promotion path (§9), never an automatic side effect of
  replacement.
- **Invalidation** is the deliberate act of retracting a belief before it
  would otherwise expire — for example, a source corrects an earlier
  Observation, or a strongly contradicting Observation arrives that
  `WorldModelUpdatePolicy` judges sufficient to invalidate the existing
  belief outright rather than merely lowering its confidence. Like
  Update, Invalidation is a policy-governed decision, not a bare
  overwrite.
- **Expiry** is what happens to a belief that receives no reaffirming
  Observation within a policy-defined window: per `WorldModel.md`'s
  Normative Requirement, "Stale state MUST expire or degrade." A belief
  that expires ceases to be treated as current — it is no longer returned
  as an answer to `current` or `query` — and, exactly as with Replacement,
  the World Model does not itself retain it as a historical record once
  it has expired.

**When a belief ceases to exist.** A belief the World Model holds ceases
to be authoritative the moment it is superseded by an Update for the same
scope, is explicitly invalidated, or expires per policy. In every case,
the World Model itself retains no permanent record of the belief once it
is no longer current; this is what distinguishes it from Memory, whose
entire purpose is durable retention (`MEMORY_RUNTIME_ARCHITECTURE.md`
§4). This lifecycle does not by itself define the specific expiry
algorithm, confidence-decay function, or conflict-resolution rule — those
are `WorldModelUpdatePolicy` implementation questions for a future
contract design and implementation unit, not decided here.

## 6. Ownership

| Source | May propose an Observation? | Owns acceptance? |
| --- | --- | --- |
| Sensors | Yes — the primary, continuous source of live signal. | No. |
| Plugins | Yes — device and service integrations reporting observed state. | No. |
| Agents (Agent Runtime) | Yes — as a byproduct of executing a step (for example, confirming an action's observed real-world effect). | No. |
| Planner | No. The Planner consults current belief (§8) but does not itself observe reality; it has no architectural role as an Observation source. | No. |
| Runtime | Yes — the Execution Pipeline and Task Manager may submit an Observation as a byproduct of execution, exactly as Agents may. | No. |
| Memory | No. Memory is a separate, parallel knowledge layer (§9), not a source of live signal about current reality; nothing in this architecture has Memory reporting an Observation to the World Model. | No. |
| User | Yes — a direct statement about current reality (for example, "I am no longer at home"), submitted like any other Observation. | No. |

No subsystem outside the World Model owns acceptance of an Observation
into current belief. Acceptance — validating an Observation and deciding
whether it now represents current belief — belongs to the World Model
alone, exercised through `WorldModelUpdatePolicy` (§7), regardless of
which of the seven sources above submitted the Observation.

**Ownership at the point of submission.** Mirroring the same explicit
clarification Track A's Memory Runtime Architecture makes for Candidate
Memory (`MEMORY_RUNTIME_ARCHITECTURE.md` §5): the World Model owns an
Observation from the moment it is submitted for validation, regardless of
which subsystem submitted it. This is stated explicitly because more than
one source — a sensor, a plugin, an agent, the user — may submit
Observations concurrently; without an explicit ownership boundary at the
point of submission, it would be ambiguous which subsystem is responsible
for an Observation while it awaits Update. There is exactly one answer:
the World Model is.

## 7. Runtime Interfaces

This section identifies the public contracts the World Model will
eventually require, and each one's responsibility — not Kotlin
signatures, which remain future, field-level contract design work.
Consistent with the contract-minimalism discipline Track A's Unit A2
established ("do not create types merely for symmetry"), each interface
below is included because a concrete responsibility requires it, not
because a sibling Runtime component happens to have something similarly
named.

- **`WorldModel`**, already named in this repository
  (`src/interfaces/WorldModel.kt`,
  `docs/specifications/volume-03-core-interfaces/WorldModel.md`),
  responsible for: accepting an Observation, resolving current belief for
  a given scope, and querying current belief matching broader criteria.
  This interface already exists at the operation-naming level
  (`observe`, `current`, `query`). Its supporting types —
  `WorldObservation`, `ObservationResult`, `WorldState`, and `WorldQuery`
  — are named by that interface but have no field-level definition
  anywhere in the repository yet. This is the same situation
  `MemoryStore.kt` was in before Track A's Unit A2, and it is not
  resolved by this document; a future contract design unit must define
  those fields before implementation.
- **`WorldModelUpdatePolicy`**, a named architectural seam responsible for
  deciding, given a validated Observation and any existing belief for the
  same scope, whether the Observation now represents current belief —
  covering conflict resolution between competing Observations, confidence
  comparison, explicit invalidation, and the expiry/degradation
  determination `WorldModel.md`'s own Normative Requirements already
  require ("Stale state MUST expire or degrade"). `WorldModelUpdatePolicy`
  SHALL determine whether a validated Observation becomes the current
  World Belief. This policy is internal to the World Model and is never
  exposed as a callable Runtime service: no external subsystem —
  Planner, Agent Runtime, Tool Execution, or otherwise — may invoke it
  directly or substitute its own judgment for what the World Model
  currently believes. Submitting an Observation (§6) is the only external
  interaction any subsystem has with this seam; the evaluation itself is
  Runtime-invisible, mirroring exactly the same internal-only boundary
  Track A's Unit A2 established for `MemoryPromotionPolicy`
  (`MEMORY_RUNTIME_ARCHITECTURE.md` §7; `MEMORY_CONTRACT_DESIGN.md` §6).
  This is a genuinely new
  architectural concept this document introduces (see Self-Traceability
  Review, below). Its *existence* is justified independently of symmetry
  with Memory's `MemoryPromotionPolicy` — the calling-convention
  boundary above is deliberately borrowed because it is already proven,
  but the seam itself is not invented merely because Memory has one: the
  World Model's existing Normative
  Requirements already describe *what* must happen (confidence tracking,
  expiry, conflict resolution implied by "resolve current state") without
  naming *what decides it* — exactly the gap `MemoryPromotionPolicy` and
  `PlanDecision` filled for their own Runtimes before an interface stub
  existed to hold their logic. Expiry and conflict-resolution are
  combined into one seam, not split into two, because both answer the
  same underlying question — "does this scope's belief remain current
  right now" — one evaluated when a new Observation arrives, the other
  evaluated by the passage of time; splitting them would create two
  interfaces answering variations of one question, which the Unit A2
  minimalism discipline weighs against. This gives Parker four parallel
  policy seams, one per Runtime Foundation component that must decide
  something without being the thing that stores or executes:

  ```
  AgentStepSource       -- Agent Runtime:   which step runs next
  PlanDecision          -- Planner Runtime: which candidate is selected
  MemoryPromotionPolicy -- Memory:          which candidate is promoted
  WorldModelUpdatePolicy -- World Model:    which belief is current
  ```

No additional public contract beyond these two is identified as required
for the World Model to take its place in the Runtime Foundation.
Categories of belief (§4) are not, by themselves, assumed to require
category-specific public interfaces; that determination is left to the
future contract design unit that gives these interfaces their fields. A
separate `WorldModelRuntime` wrapper interface, distinct from `WorldModel`
itself, is deliberately not proposed: no responsibility identified in §3
requires a second public interface beyond the store/query surface
`WorldModel` already names, mirroring Unit A2's own conclusion that
`MemoryStore` needed no separate `MemoryRuntime` alongside it.

**A naming observation, not a decision.** The existing stub calls current
belief `WorldState`. Both `16-world-model.md` ("what does Parker currently
*believe* is true?") and `reasoning-context.md` ("Parker's best current
*belief*") consistently use "belief," not "state," when describing this
concept in prose — suggesting a future contract design unit may find
`WorldBelief` a clearer name than `WorldState`, exactly as Unit A2 found
`MemoryRecord` clearer than the original stub's bare `Memory`. This
document does not rename the type: renaming is a field-level contract
decision, not an architectural one, and is noted here only so the future
contract design unit does not have to rediscover it.

**A scoping observation, not a decision.** `WorldModel.kt`'s existing
`current(resourceId: ResourceId): WorldState?` signature suggests belief
is keyed to an existing `ResourceId`. That fits Device State cleanly, but
it is not obvious every Information Category in §4 maps onto a
`ResourceId` — User Activity and Derived Beliefs, in particular, may not
correspond to any registered Resource. Whether `WorldQuery` and `current`
need a broader or different key than `ResourceId` is left entirely to the
future contract design unit; this document only flags the question so it
is not silently dropped.

## 8. Planner Interaction

```
Planner
   |
   v
World Model
   |
   v
Planner
```

The Planner Runtime may consult the World Model as a read source when
assembling its own Planning Context, consistent with AD-011 (Context Is
Reference-Based): the World Model is referenced, not copied into the
Planner's own state. A Planner Runtime session may query the World Model
for current belief relevant to the Goal it is planning against — for
example, "is it within the allowed time window"
(`IMPLEMENTATION_ORDER.md` §4) — and the World Model returns whatever
current belief matches that query.

The Planner holds no write authority over the World Model. It cannot
submit an Observation as part of planning (§6), cannot invalidate a
belief, and cannot influence `WorldModelUpdatePolicy`'s determination of
what is current. The Planner's only relationship to the World Model is
read: consulting current belief to make its own decomposition more
informed, never authoring or altering that belief itself. This is the
same reference-based, read-only relationship Memory has with the Planner
(`MEMORY_RUNTIME_ARCHITECTURE.md` §8), applied here to a different
knowledge layer, not a different rule.

The Planner SHALL NOT cache World Model beliefs as its own authoritative
state beyond the lifetime of the current Planning Session. A belief
consulted while assembling Planning Context is a reference resolved at
the time it is needed, per AD-011 — not a value copied into the Planner's
own state that could silently drift from what the World Model now
believes. If a Planning Session needs current belief again later in its
own lifetime, it consults the World Model again; it does not rely on
what it read earlier as if that were still current.

## 9. Memory Interaction

The World Model and Memory answer different questions and must remain
separate, per `ADR-002` and `reasoning-context.md`, neither of which this
document redefines. This section restates that boundary from the World
Model's side; it must not, and does not, contradict Memory's own
statement of the same boundary in `MEMORY_RUNTIME_ARCHITECTURE.md` §9:

- **World Model** = what Parker currently believes. Transient, sourced,
  timestamped, confidence-scored, and expected to change frequently as
  sensors and other live inputs update it.
- **Memory** = what Parker has learned. Durable, deliberately promoted,
  slow to change, and retained across tasks, sessions, and time.

**Why neither owns the other.** Orchestration authority, and the
authority to decide what is durably worth keeping, is deliberately not
given to either layer (AD-012). If the World Model could write into
Memory directly, or Memory could reach into and mutate current belief,
each would inherit a second, ungoverned responsibility outside what it
was built to do, and the separation `reasoning-context.md` establishes
would collapse into a single, conflated knowledge store.

**How they cooperate.** The only legitimate path from a World Model
belief into Memory is the same path any other observation takes: some
subsystem — not the World Model itself, acting autonomously — decides a
current belief is worth retaining beyond its transient relevance, and
submits it as a Candidate Memory through Memory's own Observation →
Candidate Memory → Evaluation → Promotion lifecycle
(`MEMORY_RUNTIME_ARCHITECTURE.md` §4, §6). The World Model has no
promotion mechanism, no Candidate Memory submission path of its own
initiative, and no visibility into whether something it currently
believes has ever been promoted. Symmetrically, Memory has no path to
directly update, invalidate, or query the World Model's own internal
Update/Invalidation/Expiry mechanics (§5) — a subsystem that wants Memory
to inform current belief must submit a fresh Observation to the World
Model exactly as any other source would (§6), never reach in and rewrite
it. This is the concrete meaning of `WorldModel.md`'s own normative
requirement, "World Model MUST NOT become Memory," reaffirmed here from
the World Model's own side as its mirror image: Memory MUST NOT become
the World Model, either.

## 10. Runtime-Wide Consultation

Identity, Trust (the Permission Engine), the Planner (§8), the Agent
Runtime, Tool Execution, and the Task Manager may each consult the World
Model as a read source. None of them is owned by the World Model, and the
World Model owns none of them (AD-012). Consultation never implies
ownership:

- **Identity** resolves who is asking. The World Model may scope a query
  to a requesting Principal where relevant, but Identity Service alone
  establishes who that Principal is; the World Model never performs
  identity resolution itself.
- **Trust (the Permission Engine)** may consult current belief as part of
  evaluating a permission decision — for example, whether an action falls
  within a currently-permitted time window or a currently-observed
  device state (`IMPLEMENTATION_ORDER.md` §4's own example). The World
  Model supplies the current fact; the Permission Engine alone decides
  what that fact means for authorization. The World Model never grants,
  evaluates, or overrides a permission decision itself.
- **The Agent Runtime** may consult the World Model as part of assembling
  context for a step, exactly as it references Task, Resource, or event
  data today (AD-011) — the World Model does not drive an Agent Run's
  step transitions or lifecycle, and may itself receive an Observation
  from an Agent Run as a byproduct of a step (§6), without that
  submission granting the Agent Runtime any acceptance authority over it.
- **Tool Execution** may consult current belief as part of validating
  preconditions (`Tool.validate`) or informing execution parameters —
  for example, checking a device's currently-believed reachability before
  attempting to invoke it. This remains strictly read-only: the World
  Model is never a path by which a Tool triggers its own execution or
  bypasses the Execution Pipeline, and consulting it grants Tool
  Execution no authority to alter what the World Model currently
  believes.
- **The Task Manager** may reference the World Model read-only when it is
  useful context for a Task Proposal's provenance, exactly as it
  references other Context sources today; it does not gain a write path
  into the World Model, and the World Model does not own any Task, Task
  Proposal, or Task lifecycle state.

The World Model must not own any of these six systems, their state, or
their lifecycle transitions, per AD-012's existing rule that Memory and
the World Model are context providers, never orchestration systems.

## 11. Constitutional Boundaries

Confirmed, without exception:

- **The World Model never executes actions.** It has no path to the Tool
  Registry, the Resource Registry, or any tool invocation.
- **The World Model never plans.** It does not generate, evaluate, or
  select a Plan Candidate, and it does not decide what Parker should do
  next.
- **The World Model never promotes Memory.** It has no Candidate Memory
  submission path of its own initiative, and no belief it holds becomes a
  Memory record without a separate subsystem's deliberate submission
  through Memory's own promotion path (§9).
- **The World Model never reacts autonomously to events beyond its own
  belief.** Updating its own current belief in response to a validated
  Observation is the World Model's core function, not an exception to
  this rule — that is precisely what §5's lifecycle describes. What the
  World Model never does is treat an Update to its own belief as, by
  itself, a trigger for a plan, an Agent Run, a Tool invocation, or any
  other execution-path event. An updated belief is available to be
  consulted (§8, §10); it is never, on its own, a cause of action. Only
  cognition — the Planner or an Agent Runtime — consulting that belief
  and proposing something, followed by Trust authorising it, can lead to
  action.
- **The World Model never rewrites Memory.** It has no write path into
  Memory's internal records, and it does not reach into Memory to
  invalidate, correct, or supersede anything Memory has promoted (§9).
- **The World Model provides state, never authority.** Every interaction
  described in this document — with the Planner, the Agent Runtime, Tool
  Execution, the Task Manager, Identity, and Trust — is the World Model
  being read from, never the World Model directing any of them.

This is a direct instance of the Constitution's own rule: "Memory and the
World Model inform reasoning and proposals but carry no authority of
their own to act." Nothing in this document grants the World Model
authority the Constitution withholds from it, and nothing in this
document is required to make that true — it is already true of every
existing component the World Model integrates with.

## 12. Out of Scope

This document does not design: storage engines, databases, graph
technology, embeddings, retrieval algorithms, synchronization mechanisms,
networking, or implementation classes. Those belong to later units —
specifically, to the field-level contract design and implementation units
that follow this one, once this architecture has been reviewed and
accepted.

The World Model remains independent of any storage or synchronization
technology. Nothing in this document, or in any future contract design
that follows it, should be read as fixing the World Model to a particular
storage engine, message-passing mechanism, or synchronization protocol —
"World Model" names an architectural role, not any specific technology
that might one day implement it.

## 13. Engineering Review

**Missing seams.** No responsibility identified in §3 or Normative
Requirement in `WorldModel.md` is left without a corresponding seam:
storage/query is `WorldModel` itself; the one genuine decision point —
whether an Observation now represents current belief, including conflict
resolution and expiry — is `WorldModelUpdatePolicy` (§7). No further seam
is identified as missing.

**Unnecessary seams.** A separate `WorldModelRuntime` wrapper interface is
deliberately not proposed (§7): nothing in §3's responsibilities requires
a second public interface beyond `WorldModel` itself, mirroring Unit A2's
identical conclusion for `MemoryStore`/`MemoryRuntime`. A separate expiry
policy, split from `WorldModelUpdatePolicy`, is also deliberately not
proposed (§7): expiry and conflict resolution are two evaluations of the
same underlying question, and splitting them would multiply interfaces
without adding clarity, which the Unit A2 minimalism discipline weighs
against.

**Overlap with Memory.** None identified. §9 establishes the boundary
explicitly and from both directions: the World Model has no promotion
path into Memory, and Memory has no write path into the World Model's own
Update/Invalidation/Expiry mechanics. The only connection between them is
a deliberate, subsystem-initiated Candidate Memory submission — never an
automatic mirror in either direction.

**Overlap with Planner.** None identified. §8 establishes the World
Model's role as strictly read-only consultation; the Planner retains
exclusive ownership of decomposition, Plan Candidate generation,
evaluation, and selection (`PLANNER_RUNTIME_CONTRACT_DESIGN.md`), none of
which this document touches or duplicates.

**A disclosed, unresolved question, not a gap in this architecture.** §7
flags, without resolving, whether `WorldQuery`/`current` should be keyed
by something broader than the existing `ResourceId`, since not every
Information Category in §4 obviously maps onto a registered Resource.
This is deliberately left to the future contract design unit rather than
decided here, consistent with this document's own scope (architecture,
not field-level contracts) — it is named explicitly so it is not silently
dropped.

## 14. Conclusion

This document places the World Model inside Parker's existing Runtime
Foundation without redefining the Constitution, the Architecture
Decisions, PES-001, `reasoning-context.md`, or the Memory Runtime
Architecture already adopted. It identifies the World Model's purpose,
its responsibilities and non-responsibilities, its architectural
categories, its lifecycle, the ownership of each lifecycle activity, the
public contracts it will eventually require, and its interaction with the
Planner, Memory, and the rest of the Runtime. It confirms that the World
Model never executes, never plans, never promotes Memory, never reacts
autonomously beyond updating its own belief, never rewrites Memory, and
provides state only — consistent with, and required by, the Parker
Constitution.

Track B architecture is sufficiently defined to permit contract design
without introducing new architectural concepts.

## Self-Traceability Review

Every runtime interface this document proposes, and whether it is
explicitly derived from existing architecture or represents a genuinely
new architectural concept:

| Interface | Derived, or new? |
| --- | --- |
| `WorldModel` | **Derived.** Already exists, verbatim, in `src/interfaces/WorldModel.kt` and `docs/specifications/volume-03-core-interfaces/WorldModel.md`. This document does not rename, restructure, or re-scope its three operations; it only places the existing interface within the Runtime Foundation's ownership and interaction model. |
| `WorldObservation` | **Derived.** Already named as the parameter type of `WorldModel.observe` in the existing stub. This document does not shape its fields; it only confirms the role (an input from one of the seven sources in §6) that a future contract design unit must give a field-level definition. |
| `ObservationResult` | **Derived.** Already named as the return type of `WorldModel.observe` in the existing stub. This document does not decide its shape or whether it should be retained, merged, or replaced by a future contract design unit — that determination is explicitly left open (§7), exactly as Unit A2 left, and then resolved, a comparable question for `MemoryStore`'s original four supporting types. |
| `WorldState` | **Derived**, with a disclosed naming observation. Already named as the return type of `WorldModel.current`/`query` in the existing stub. This document does not rename it, but flags (§7) that `WorldBelief` may better match the vocabulary `16-world-model.md` and `reasoning-context.md` already use — a naming question for a future contract design unit, not a new concept. |
| `WorldQuery` | **Derived.** Already named as the parameter type of `WorldModel.query` in the existing stub. This document does not shape its fields, but flags (§7) an open scoping question (whether `ResourceId` alone suffices as a key) for a future contract design unit to resolve. |
| `WorldModelUpdatePolicy` | **Genuinely new.** No interface by this name, or performing this role, exists anywhere in the repository prior to this document. It is introduced in §7 to fill a gap the existing Normative Requirements describe in effect ("Stale state MUST expire or degrade"; "resolve current state" implies conflict resolution) but do not assign to any named seam. Its justification is independent of symmetry with `MemoryPromotionPolicy`, `PlanDecision`, or `AgentStepSource`, even though, once introduced, it takes its place as a fourth parallel instance of the same general pattern those three already establish. |

No proposed interface beyond these six is introduced by this document. A
`WorldModelRuntime` wrapper interface was considered and explicitly
rejected (§7, §13) rather than silently omitted.
