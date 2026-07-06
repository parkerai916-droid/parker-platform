# World Model Contract Design

## Status

Sprint: Sprint 4, Track B, Unit B2 (Contract Design)
Version: 0.1-draft
Status: **Design proposal, not yet reviewed or accepted.**

**This document is contract design only.** No Kotlin is implemented,
proposed as a diff, or changed by it. Neither `src/` nor `tests/` is
touched. `docs/implementation/IMPLEMENTATION_HISTORY.md` and
`docs/architecture/IMPLEMENTATION_GAPS.md` are both untouched. No other
document is modified or created.

### Why this unit exists

Sprint 4, Track B, Unit B1 (`docs/architecture/WORLD_MODEL_RUNTIME_ARCHITECTURE.md`)
placed the World Model inside Parker's Runtime Foundation: what it is,
what it is not, its responsibilities, its lifecycle, its ownership, and
its constitutional boundaries. It deliberately stopped short of
field-level design — it identified that the existing `WorldModel`
interface would remain the sole public contract, that a new
`WorldModelUpdatePolicy` seam was required, and that several existing,
named-but-unshaped types (`WorldObservation`, `ObservationResult`,
`WorldState`, `WorldQuery`) would need a future contract design pass —
without shaping any of their fields. This document is Unit B1's Unit A2:
it performs the field-level design pass Unit B1 explicitly deferred, so
that a future implementation unit builds the World Model by implementing
an already-approved contract set, never by inventing one mid-Kotlin.

This document also performs, up front and throughout, the same contract
minimalism review Track A's Unit A2 was required to perform: every
contract below is included only where it has a distinct architectural
justification, one candidate name is merged into an existing type rather
than introduced separately, and one plausible addition (a belief-category
enumeration) is deliberately not introduced at all, with reasons.
Minimalism is the standard each candidate is measured against from the
start, not a final pass over an already-decided list.

## Review

Reviewed, in priority order:

1. `docs/architecture/parker-constitution.md`.
2. `docs/architecture/ARCHITECTURE_DECISIONS.md`, especially AD-010
   (Model Independence), AD-011 (Context Is Reference-Based), AD-012
   (Memory and World Model Are Context Providers), and AD-013
   (Specifications Define Contracts).
3. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001).
4. `docs/architecture/WORLD_MODEL_RUNTIME_ARCHITECTURE.md` (Unit B1,
   accepted) — the architecture this document implements as contracts
   and does not redefine.
5. `docs/specifications/volume-03-core-interfaces/WorldModel.md` — the
   existing, already-approved Purpose, Responsibilities, and Normative
   Requirements this document must implement as field-level design, not
   redefine at the operation-naming level.
6. `src/interfaces/WorldModel.kt` — the existing interface stub. Reviewed
   without assuming it is correct merely because it exists, per this
   unit's own instructions; two of its decisions (the bare `WorldState`
   name and the `ResourceId`-keyed `current` signature) are revised
   below, with reasons.
7. `docs/architecture/MEMORY_CONTRACT_DESIGN.md` (Unit A2) and
   `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` (Unit D1A) —
   this document's own direct precedent and required point of symmetry
   comparison.
8. `docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md` — Memory's own
   accepted architecture, so that this document's statement of the
   Memory/World Model boundary matches Memory's own statement of it
   rather than contradicting it.
9. `src/interfaces/MemoryStore.kt`, `src/contracts/PlanDecision.kt`,
   `src/contracts/AgentStep.kt`, `src/contracts/Identifiers.kt` — the
   concrete, already-implemented Kotlin these contracts must match in
   pattern (identifier shape, sealed-outcome shape, policy-seam shape),
   even though this document itself writes no Kotlin.

---

## Constitutional Boundaries

Restated up front, since every contract below is checked against these —
identical in substance to Unit B1's own Architectural Principles and
Constitutional Boundaries sections, not re-derived differently here:

- **The World Model represents present belief. The World Model never
  decides. The World Model never acts.** No contract in this document
  gives the World Model a path to decide what Parker should do, or to
  act on that decision itself.
- **The World Model never orchestrates Runtime.** No contract here owns
  a Goal, a Task, an Agent Run, or a Planning Session, and no contract
  here triggers a lifecycle transition in any of them.
- **The World Model never executes actions.** No contract here
  references `Tool`, `ToolInvocationBinding`, or `ExecutionRequest`.
- **The World Model never reacts autonomously beyond updating its own
  belief.** Every contract below that changes what the World Model
  currently believes is invoked by an explicit `observe` call; nothing
  here is triggered directly by an event crossing the Event Bus, and
  nothing here treats a belief update as, by itself, a cause of a plan,
  an Agent Run, or a Tool invocation.
- **The World Model never modifies Memory.** No contract here writes,
  promotes, invalidates, or otherwise reaches into a `MemoryRecord`; the
  only legitimate path from a belief into Memory remains a separate
  subsystem's own `CandidateMemory` submission (Unit B1 §9).
- **The Planner consumes the World Model but does not own it.** No
  contract here grants the Planner a write path into current belief, and
  no contract here lets the Planner cache a belief as its own
  authoritative state beyond the current Planning Session (Unit B1 §8).
- **Reasoning consumes the World Model but does not own it.** No
  contract here gives a reasoning provider standing access beyond what is
  explicitly queried, and no contract here lets a reasoning provider
  write back into current belief (`reasoning-context.md`).

---

## Contract Minimalism Review — Summary

| Candidate | Determination |
| --- | --- |
| `WorldState` / `WorldBelief` | **Include, renamed.** The existing stub's `WorldState` becomes `WorldBelief`, matching the vocabulary Unit B1, `16-world-model.md`, and `reasoning-context.md` already use. |
| `WorldObservation` | **Include.** |
| `ObservationResult` | **Include**, reshaped as a three-variant sealed outcome. Absorbs the candidate `WorldModelUpdateDecision` — no second outcome type is introduced. |
| `WorldQuery` | **Include.** |
| `WorldModelUpdateDecision` | **Exclude — merge into `ObservationResult`.** The existing, already-named return type of `observe` already fills this role; naming a second outcome type would duplicate it. |
| `WorldModelUpdatePolicy` | **Include.** The central seam this unit gives a shape to. |
| `WorldModel` | **Include.** Already exists; remains the World Model's one public interface. |
| `WorldModelRuntime` | **Exclude — merge into `WorldModel`.** No responsibility identified requires a second public interface. |
| A belief-category enumeration (e.g. `WorldInformationCategory`) | **Not introduced.** No concrete filtering or classification need has been identified for any contract below; Unit B1 §4's six Information Categories remain descriptive prose, not a formal type, unless and until a real need for one is identified. See §7, below. |

Net result: **five required contracts** (`WorldBelief`, `WorldObservation`,
`ObservationResult`, `WorldQuery`, `WorldModelUpdatePolicy`), **one
retained public interface** (`WorldModel`), and **two candidates excluded
outright** (`WorldModelUpdateDecision`, `WorldModelRuntime`), plus one
plausible addition considered and deliberately not introduced (a
belief-category enumeration), each with the reasoning below.

---

## 1. `WorldBelief` (renamed from the existing stub's `WorldState`)

- **Purpose.** The current, live representation of what the World Model
  believes about one subject — the type `current` and `query` return
  (Unit B1 §1, §2).
- **Responsibilities.** Represent one current belief: identifiable by
  subject, timestamped, confidence-scored, and carrying whatever value it
  asserts. Nothing beyond that — no history, no prior-belief reference
  (Unit B1's own Architectural Principle: "the World Model is never
  historical storage").
- **Ownership.** Constructed by `WorldModel`'s implementation at Update
  time (§5, below), consulting `WorldModelUpdatePolicy`; never
  constructed directly by a caller.
- **Relationships.** Returned by `current` and `query`; constructed from
  an accepted `WorldObservation`'s fields plus the authoritative
  timestamp `WorldModel` itself assigns (see the Timestamp Ownership
  decision, below).

**Required members:**

- A **subject** identifying what this belief is about — a non-blank
  string (see the Resource Identity decision, below, for why this is not
  `ResourceId`).
- A **value** — the actual content of the belief. This document does not
  shape its internal structure, exactly as `MEMORY_CONTRACT_DESIGN.md` §2
  leaves `CandidateMemory`'s knowledge payload unshaped: that is
  storage-format territory, explicitly out of scope for this unit.
- A **confidence** score, required, not optional. This is a deliberate
  point of difference from `CandidateMemory`/`MemoryRecord`'s optional
  confidence field: `WorldModel.md`'s own Normative Requirement is
  unconditional — "World state MUST have confidence" — with no exception
  for a caller that has none to offer, unlike Memory's confidence factor,
  which is only one of several promotion inputs and may legitimately be
  absent.
- A **timestamp**, required, assigned by the World Model itself, never by
  a caller (see the Timestamp Ownership decision, below).
- **Provenance** — which of Unit B1 §6's seven sources produced the
  Observation this belief was accepted from.
- An optional **derivation reference** — present only when this belief
  was computed from other current beliefs rather than sourced from a
  single direct Observation (Unit B1 §4, Derived Beliefs). Its presence,
  not any separate category field, is what marks a belief as derived
  (see the Derived Beliefs decision, below).

**What it intentionally does not carry:**

- **No history and no prior-belief reference.** Unlike `MemoryRecord`,
  which is required to carry an auditable history of promotion,
  consolidation, forgetting, and supersession, `WorldBelief` carries none
  of that. This is not an oversight matching Memory's own treatment
  loosely — it is the direct, required consequence of Unit B1's own
  Architectural Principle that the World Model "is never historical
  storage" and "retains no permanent record of the belief once it is no
  longer current." A `WorldBelief` that carried its own history would
  quietly turn the World Model into a second Memory, which Unit B1 §9 and
  `WorldModel.md`'s own Normative Requirement ("World Model MUST NOT
  become Memory") both forbid.
- **No expiry timestamp or TTL field.** Expiry is computed, not stored:
  given a belief's required timestamp and `WorldModelUpdatePolicy`'s own
  policy-defined staleness window, whether a belief is still current can
  always be computed at read time from information the belief already
  carries. Adding a separate expiry field would be redundant structure,
  not a new capability.

- **Required lifecycle.** Constructed at Update; superseded by a later
  Update for the same subject, cleared by an Invalidation, or excluded
  from future `current`/`query` results once it is judged stale — in
  every case, the World Model retains no copy of it afterward (Unit B1
  §5).
- **Future extensibility.** Additional optional metadata fields (for
  example, a richer provenance shape) can be added additively, exactly as
  `MemoryRecord`'s own optional metadata fields are extensible.

### Lifecycle

Neither `MEMORY_CONTRACT_DESIGN.md` nor `PLANNER_RUNTIME_CONTRACT_DESIGN.md`
draws its own central type's lifecycle as an explicit diagram — both
state it in prose "Required lifecycle" bullets only, leaving the
arrow-diagram treatment to their respective architecture documents
(`MEMORY_RUNTIME_ARCHITECTURE.md` §5, `PLANNER_RUNTIME_PROGRESSION_DESIGN.md`'s
own state diagram). `WorldBelief`'s lifecycle is drawn explicitly here
anyway, as a genuine addition at the contract-design level, not a
restatement of an existing contract-design precedent: `WorldBelief`'s
progression is a genuinely new shape at this level of detail (two
branching paths converging on a single terminal step), and a diagram
communicates that shape more precisely than prose bullets alone would,
for the same reason `MEMORY_RUNTIME_ARCHITECTURE.md` §4 and Unit B1 §5
themselves each draw Memory's and the World Model's own
architecture-level lifecycles as diagrams rather than prose only.

Two paths, both beginning at the same two steps:

```
Observation
   |
   v
Validation
   |
   +--> (accepted) --> Current Belief --> Superseded --> Discarded
   |
   +--> (accepted, retraction) --> Invalidated --> Discarded
   |
   +--> (rejected) --> Discarded
```

- **Observation** — a `WorldObservation` is submitted via `WorldModel.observe`.
- **Validation** — `WorldModelUpdatePolicy` checks the Observation is
  well-formed and weighs it against any existing belief for the same
  subject (§5, above).
- **Current Belief** — Validation accepts the Observation; a new
  `WorldBelief` is constructed and is now the current answer `current`/
  `query` return for its subject (`ObservationResult.Accepted`).
- **Superseded** — a later Observation for the same subject is accepted
  in turn; the previous `WorldBelief` stops being current the moment the
  new one is constructed. This is not a distinct stored state — no field
  on `WorldBelief` marks it "superseded" — it is simply no longer
  returned once a newer `WorldBelief` exists for that subject.
- **Invalidated** — Validation accepts an Observation carrying a
  retraction indicator; the existing `WorldBelief` for that subject is
  cleared, and no belief currently stands (`ObservationResult.Invalidated`).
- **Discarded** — the terminal step on every path. A superseded, an
  invalidated, and a rejected `WorldObservation` all end here identically:
  the World Model retains no copy of the belief or the Observation that
  produced it, beyond whatever audit trail the submission itself already
  received. A `WorldBelief` that has become stale enough for
  `WorldModelUpdatePolicy` to exclude it from `current`/`query` (Expiry,
  Unit B1 §5) reaches this same terminal step by a third route not shown
  above, since Expiry is not triggered by any new Observation at all —
  it is discovered lazily, at the next read, exactly as §3's "Why Expiry
  has no corresponding variant" note already explains.
- **(rejected)** — Validation does not accept the Observation at all; the
  belief for that subject, if any existed, is unchanged, and the
  Observation itself is discarded (`ObservationResult.Rejected`).

This is deliberately a shallower lifecycle than `MemoryRecord`'s own five
named audit events (promoted, consolidated, forgotten, superseded, and
the future supersession event) or `PlannerSessionStatus`'s eight-state
machine — not an inconsistency with either precedent, but the direct
consequence of `WorldBelief` carrying no history (above). Memory's
lifecycle is deep because an auditable trail must be reconstructable
from it; the World Model's is shallow because nothing about a discarded
belief is meant to be reconstructable at all.

## 2. `WorldObservation`

- **Purpose.** What any of Unit B1 §6's seven sources submits when it
  reports something about current reality — the parameter type of
  `observe`.
- **Responsibilities.** Carry exactly what `WorldModelUpdatePolicy` needs
  to validate and evaluate the submission, and what an accepted
  `WorldObservation` will need to construct the resulting `WorldBelief`
  from. Carries nothing else.
- **Ownership.** Constructed by the submitting source; owned by the
  World Model from the instant it is submitted (Unit B1 §6's explicit
  ownership-at-submission clarification, mirrored here exactly as Unit
  A1/A2 established it for `CandidateMemory`).
- **Relationships.** Consumed by `WorldModelUpdatePolicy` during
  Validation and Update; its fields, if accepted, seed the corresponding
  fields of the resulting `WorldBelief`.

**Required members:**

- A **subject**, matching `WorldBelief`'s own subject field — what this
  Observation is about.
- A **value** — what is being asserted. Unshaped, as above.
- A **confidence** score, required (`WorldModel.md`: "World state MUST
  have confidence" applies at submission, not only once accepted).
- **Provenance** — which source is submitting this Observation.
- An optional **source-reported timestamp** — when the submitting source
  itself observed this to be true, retained only for provenance; this is
  never the authoritative timestamp a resulting `WorldBelief` carries
  (see the Timestamp Ownership decision, below).
- An optional **derivation reference** — present when this Observation is
  itself computed from other current beliefs rather than sensed directly
  (Unit B1 §4, Derived Beliefs). Carried forward unchanged onto the
  resulting `WorldBelief` if accepted.
- An optional **retraction indicator** — marks this Observation as an
  explicit request to invalidate the current belief for its subject,
  rather than assert a new value (see the Belief Invalidation decision,
  below). This is the only mechanism this document proposes for
  Invalidation; no separate public operation is introduced for it (§6,
  below).

**What it intentionally does not carry:**

- **No promotion, retention, or any Memory-shaped field.** A
  `WorldObservation` is not a `CandidateMemory`, and nothing here gives a
  source any path from an Observation into Memory (Unit B1 §9).
- **No authority of any kind.** Submitting a `WorldObservation` is
  cognition proposing, exactly as submitting a `CandidateMemory` is; it
  carries no ability to force its own acceptance, regardless of which
  source submits it (Unit B1 §6, "No subsystem outside the World Model
  owns acceptance of an Observation").

- **Required lifecycle.** Constructed once by the submitting source;
  submitted via `WorldModel.observe`; consumed exactly once by
  `WorldModelUpdatePolicy` during Validation and Update; discarded
  afterward regardless of outcome (on acceptance, its fields seed a new
  `WorldBelief`; on rejection, nothing is retained beyond whatever the
  resulting `ObservationResult.Rejected` states, for the same reason
  Memory discards a rejected `CandidateMemory` rather than retaining it
  in full).
- **Future extensibility.** Additional optional provenance fields can be
  added the same low-risk, additive way `CandidateMemory`'s own optional
  fields can.

## 3. `ObservationResult`

- **Purpose.** The outcome of one `observe` call — already named as the
  return type of `observe` in the existing stub, given a concrete shape
  for the first time.
- **Responsibilities.** State, exhaustively, what happened to a submitted
  `WorldObservation`: whether it now represents (or updates) current
  belief, whether it cleared an existing belief without asserting a new
  one, or whether it changed nothing.
- **Ownership.** Produced by `WorldModel`'s implementation, consulting
  `WorldModelUpdatePolicy` internally; consumed by the submitting source.
- **Relationships.** Consumes a `WorldObservation`; its `Accepted`
  variant carries the resulting `WorldBelief`.

**Shape.** A sealed outcome with exactly three variants:

- **`Accepted`** — the Observation now represents current belief for its
  subject, carrying the resulting `WorldBelief`. This single variant
  covers both establishing a first belief for a previously-unobserved
  subject and superseding an existing one (Unit B1 §5, Update and
  Replacement) — no separate "Replaced" variant is introduced, because
  `WorldBelief` retains no reference to what it replaced (§1, above);
  from a caller's perspective, "established" and "superseded" look
  identical, a fresh `WorldBelief` for the subject in question, so a
  single variant is sufficient.
- **`Invalidated`** — a prior belief existed for this subject and has now
  been cleared; no belief currently stands. This is genuinely distinct
  from `Accepted`, because it asserts nothing new — a caller must be able
  to tell "your correction was applied, and now nothing is currently
  believed about this subject" apart from "your correction was applied,
  and here is what is now believed." Collapsing this into `Accepted` with
  a nullable belief field would blur exactly that distinction.
- **`Rejected`** — the Observation was not applied at all; whatever
  belief existed for the subject beforehand (if any) is unchanged.
  Carries a free-text `reason`.

**Why `Rejected.reason` is free text, not a closed enum — compared
explicitly against both existing precedents.** Two sealed-outcome
rejection shapes already exist in this codebase, and they disagree with
each other on purpose, not by accident:

- `PlanRejection.reason` (`PLANNER_RUNTIME_CONTRACT_DESIGN.md` §3) is a
  closed, four-value `PlanRejectionReason` enum, because each value
  corresponds to an exact, mechanically-checkable structural rule
  (duplicate id, blank goal, goal mismatch, not-selected). A
  `PlanCandidate` either violates one of those rules or it does not, with
  no judgment involved — Plan Decision is checking structure, not
  weighing evidence.
- `MemoryPromotionDecision.Reject.reason` (`MEMORY_CONTRACT_DESIGN.md` §5)
  is deliberately free text, because Memory's promotion evaluation weighs
  several named factors together (`33-memory-consolidation.md`:
  repetition, user importance, goal relevance, frequency, confidence,
  explicit request), none of which is individually a pass/fail rule —
  Memory is weighing evidence, not checking structure.

`ObservationResult.Rejected.reason` follows `MemoryPromotionDecision.Reject`,
not `PlanRejection`, and the reason is the same underlying reason, not
merely a similar one: `WorldModelUpdatePolicy`'s own rejection causes —
a confidence comparison against an existing, competing belief, a
malformed or insufficiently-confident submission, or an attempt to
invalidate a subject with no current belief to invalidate — are every
bit as much a weighing of evidence as Memory's promotion factors are, and
none of them is an exact structural rule a closed enum could exhaustively
enumerate the way `PlanRejectionReason` does. Put directly: the World
Model's rejection decision and Memory's promotion decision are the same
*kind* of decision — a confidence-and-relevance judgment, not a
validity check — and this document gives them the same shape for that
reason, not by coincidence of both being sealed outcomes. Collapsing
either into a closed enum would misrepresent a weighing decision as a
single discrete rule, which it is not; the Planner's own closed enum
remains correct for the Planner, precisely because the Planner's
rejection causes really are discrete rules.

**Why Expiry has no corresponding variant.** Expiry is not the outcome of
any specific `observe` call — it is what happens to a belief that
receives no reaffirming Observation over time (Unit B1 §5). It has no
submission event to attach a result to, and is instead evaluated lazily,
whenever `current` or `query` is called (see the Belief Expiry decision,
below). `ObservationResult` therefore has no fourth variant for it, and
none is missing by omission.

- **Required lifecycle.** Constructed exactly once per submitted
  `WorldObservation`; consumed immediately by the submitting source; not
  retained in full afterward beyond whatever audit trail the World Model
  keeps of the submission itself.
- **Future extensibility.** A fourth variant is not anticipated and is
  not designed for here; if one is ever needed, it is an additive change
  to this sealed type, not a redesign, mirroring `MemoryPromotionDecision`'s
  own forward-compatibility note.

## 4. `WorldQuery`

- **Purpose.** Describe what a caller is asking the World Model to
  return via `query` — already named as the parameter type of `query` in
  the existing stub, given a shape for the first time.
- **Responsibilities.** Carry a request; perform no retrieval itself
  (`WorldModel.query` is the operation that acts on it).
- **Ownership.** Constructed by the caller (the Planner, the Agent
  Runtime, Tool Execution, the Task Manager, Trust, or Reasoning Context
  assembly, per Unit B1 §10); consumed by `WorldModel`.
- **Relationships.** Passed to `WorldModel.query`.

**Required members:**

- A **subject-matching criterion** — at minimum, a description of which
  subject or subjects a caller is asking about, free-form and
  implementation-defined in exactly the same sense
  `MEMORY_CONTRACT_DESIGN.md` §7 leaves `MemoryQuery`'s own relevance
  criteria implementation-defined. This document does not shape how
  matching is computed.
- A **`maximumResults`** bound, for the identical architectural reason
  `MemoryQuery` requires one: `query` must not imply "return every belief
  matching," and without a caller-stated bound, nothing in this contract
  prevents an unbounded result set as connected devices, sensors, and
  derived beliefs accumulate.
- An optional **minimum-confidence threshold** — a caller may reasonably
  want only beliefs the World Model holds with some minimum certainty,
  distinct from `WorldModelUpdatePolicy`'s own internal confidence
  comparisons at Update time; this is a caller-side filter on already-
  current beliefs, not a re-evaluation of them.

**What it intentionally does not carry:**

- **A requesting-Principal or correlation identifier.** Both were
  considered, on the model of `MemoryQuery`'s own required members, and
  both are deliberately not included. Principal-scoping exists on
  `MemoryQuery` because Unit A1 §10 explicitly states "Memory may scope
  retrieval to a requesting Principal"; no equivalent statement exists in
  Unit B1 for the World Model, and no permission-sensitive World Model
  content has been identified in any reviewed document — Trust may
  consult current belief as part of its own evaluation (Unit B1 §10),
  but nothing requires the World Model itself to filter by identity to do
  so. A correlation identifier exists on `MemoryQuery` because
  `MemoryStore.md` requires forgetting to be auditable; no equivalent
  auditability requirement has been identified for a World Model query.
  Adding either field now would be inventing structure to match Memory's
  own shape, not to satisfy an identified World Model requirement — the
  same "do not create types merely for symmetry" discipline this Sprint
  requires, applied to fields rather than whole types. Either can be
  added additively the moment a concrete need is identified.
- **A ranking or scoring formula, or a category filter.** This document
  defines the request's shape only; how relevance is computed remains
  entirely a `WorldModelUpdatePolicy`/implementation concern, and no
  category filter is introduced for the reasons given in §7, below.

- **Required lifecycle.** Constructed once per query; consumed once by
  `WorldModel.query`; not retained afterward.
- **Future extensibility.** Additional optional filters (for example, a
  provenance-source filter) can be added additively.

## 5. `WorldModelUpdatePolicy`

- **Purpose.** The seam by which the World Model decides whether a
  validated `WorldObservation` becomes, updates, or invalidates current
  belief — Unit B1 §7's named architectural seam, given a concrete shape
  for the first time. Stated without hedging, exactly as Unit B1's own
  revision states it: **`WorldModelUpdatePolicy` SHALL determine whether
  a validated Observation becomes the current World Belief.** Nothing
  else in this architecture makes that determination.
- **This is a World-Model-internal policy decision, not advice to an
  external caller, and it is never invoked directly by one.** A source
  submits a `WorldObservation` via `WorldModel.observe` and is entitled
  to learn the outcome (the `ObservationResult` `observe` returns); it is
  never entitled to invoke `WorldModelUpdatePolicy` itself, to see its
  reasoning before the decision is final, or to override, appeal, or
  bypass it. This mirrors exactly how a caller of Memory never invokes
  `MemoryPromotionPolicy` directly (`MEMORY_CONTRACT_DESIGN.md` §6), and
  how a caller of the Planner Runtime never invokes `PlanDecision`
  directly — the decision seam is a collaborator internal to the runtime
  that owns it, not a service exposed to whoever is asking a question
  from outside.
- **Responsibilities.** Validate a submitted `WorldObservation`
  (well-formed, timestamped, confidence present); given any existing
  belief for the same subject, decide whether the Observation now
  represents current belief, resolving contradictory Observations by
  weighing confidence and recency rather than by a fixed structural rule
  (Unit B1 §5's Update bullet); decide when an existing belief should be
  treated as invalidated rather than merely superseded; and determine, at
  read time, whether a belief has become stale enough to expire or
  degrade. Nothing else: it does not store anything, and it does not
  itself construct a `WorldBelief` (that remains `WorldModel`'s job,
  acting on the decision this seam returns).
- **Ownership.** Injected into `WorldModel`'s implementation; today's
  World Model Runtime has no implementation of its own yet, exactly as
  `PlanDecision`/`MemoryPromotionPolicy` had none until their own
  `Default*` implementations were written.
- **Relationships.** Consumes `WorldObservation` and any existing
  `WorldBelief` for the same subject; produces `ObservationResult`;
  consulted internally by `WorldModel` as part of handling `observe` and
  `current`/`query` (for expiry) — never called directly by anything
  outside the World Model's own implementation.

**Required members.** Two operations, both required to be free to
suspend, for the identical reason `PlanDecision`, `AgentStepSource`, and
`MemoryPromotionPolicy` are each declared suspending (AD-010, Model
Independence): a future confidence-modelling or sensor-fusion
implementation may need real I/O, and a signature decided now must not
foreclose that.

1. Given a `WorldObservation` and any existing `WorldBelief` for the same
   subject, produce an `ObservationResult`.
2. Given a currently-held `WorldBelief`, determine whether it remains
   current or has become stale enough to be excluded from `current`/`query`
   results. This second operation is what `current`/`query` consult at
   read time to implement Expiry without a stored expiry field (§1,
   above).

**Required lifecycle.** The first operation is consulted exactly once per
submitted `WorldObservation`; the second is consulted whenever `current`
or `query` is about to return a belief, not on any fixed schedule or
background timer — there is no autonomous, self-triggered expiry sweep,
consistent with Unit B1 §11's boundary that the World Model "never reacts
autonomously" beyond updating its own belief in direct response to an
Observation or a read.

**Future extensibility.** Replaceable independently of `WorldModel`
itself — a smarter or model-backed update policy can be substituted
without changing how the World Model is stored or queried, exactly as
`DefaultPlanDecision`/`DefaultMemoryPromotionPolicy` can each be swapped
without touching their respective runtimes' own structure.

### Comparing the four policy seams

`WorldModelUpdatePolicy` takes its place as the fourth parallel instance
of the same pattern `AgentStepSource`, `PlanDecision`, and
`MemoryPromotionPolicy` already establish — a genuinely new seam this
Sprint introduces (Unit B1 §7), not one invented here merely to complete
the set:

```
AgentStepSource        -- Agent Runtime:   which step runs next
PlanDecision           -- Planner Runtime: which candidate is selected
MemoryPromotionPolicy  -- Memory:          which candidate is promoted
WorldModelUpdatePolicy -- World Model:     which belief is current
```

No `AgentPolicy`-equivalent bounded-configuration type is proposed
alongside `WorldModelUpdatePolicy` here, for the same reason
`MEMORY_CONTRACT_DESIGN.md` §6 declined one for Memory: no configurable
bound (a maximum Observations-per-batch limit, an update-evaluation
timeout) has been identified as necessary by Unit B1 or by this
document's own review. One can be added additively — a `WorldModelPolicy`
bounded-configuration record — the moment a concrete need is identified
(see Future Extensibility, below).

## 6. `WorldModel` (the World Model's one public interface)

- **Purpose.** The single public contract through which every other
  Runtime Foundation component reaches the World Model — already named,
  operation-by-operation, in `src/interfaces/WorldModel.kt`.
- **Responsibilities.** Accept a `WorldObservation` and return an
  `ObservationResult`; resolve current belief for a given subject;
  query current belief matching broader criteria.
- **Ownership.** The World Model subsystem's own boundary. One
  implementation is expected eventually, consulting an injected
  `WorldModelUpdatePolicy` internally, exactly as `InMemoryPlannerRuntime`
  consults an injected `PlanDecision` and a future `InMemoryMemoryStore`-
  style World Model implementation would consult
  `WorldModelUpdatePolicy`.
- **Relationships.** Consumes `WorldObservation` and `WorldQuery`;
  produces `ObservationResult` and `WorldBelief`; consulted, read-only,
  by Identity, Trust, the Planner, the Agent Runtime, Tool Execution, and
  the Task Manager (Unit B1 §10).

**Required public members.** Three operations, matching the existing
stub's naming at the level of intent, with two field-level corrections:

1. **`observe`** — accepts a `WorldObservation`, returns an
   `ObservationResult`. Unchanged in intent from the existing stub;
   changed only in that `ObservationResult` now has the three-variant
   shape §3 defines.
2. **`current`** — resolves the single current `WorldBelief` for one
   subject, or nothing if none exists or the existing one has expired.
   **Revised from the existing stub:** its parameter changes from
   `resourceId: ResourceId` to a plain, non-blank subject identifier (see
   the Resource Identity decision, below). This is a deliberate departure
   from the existing, already-approved-at-the-operation-naming-level
   stub, made because this unit's own instructions direct against
   assuming that stub is correct simply because it exists, and because
   Unit B1 §7 already flagged the same concern without resolving it.
3. **`query`** — resolves current belief matching a `WorldQuery`'s
   broader criteria, bounded by `maximumResults`.

### `current` versus `query`: the philosophical difference, stated explicitly

The two read operations are not two ways of doing the same thing with
different bounds — they answer two different kinds of question, and the
distinction is worth stating in exactly these terms rather than leaving
it implicit in their signatures:

- **`current(subject)` asks: "what is the one authoritative belief about
  this specific subject, right now?"** It returns at most one
  `WorldBelief`, or nothing. There is no ranking, no partial match, and
  no sense in which two different answers could both be "current" for
  the same subject at once — Unit B1 §3's own Responsibility to "resolve
  current state... deterministically and without ambiguity" is
  `current`'s entire reason for existing. Calling it is asking a
  question with exactly one right answer.
- **`query(criteria)` asks: "which beliefs, across potentially many
  subjects, match this broader description?"** It returns a bounded list
  (`maximumResults`), because more than one subject can legitimately
  match the same criteria — every Device State belief in a given room,
  every belief whose confidence exceeds some threshold. There is no
  single right answer to a `query` call the way there is to a `current`
  call; there is a set, and the caller has asked for at most
  `maximumResults` of it.

Put another way: `current` is a lookup, keyed and singular, exactly like
`MemoryStore`'s absence of an equivalent single-record lookup is
notable — Memory has no `current`-shaped operation at all, because no
Memory record is ever "the one authoritative answer" the way a current
belief about a specific subject is. `query` is a search, criteria-based
and plural, structurally the closer analogue to `MemoryStore.retrieve`.
The World Model needs both, where Memory needs only the latter, precisely
because the World Model's whole purpose is to answer "what does Parker
currently believe about *this*" as a well-defined, singular question
(Unit B1 §1) — a question Memory, dealing in a durable population of
many records rather than one current belief per subject, never poses in
quite the same way.

No fourth operation is introduced. In particular, no separate `invalidate`
operation is introduced (Invalidation is expressed as a `WorldObservation`
carrying a retraction indicator, per §2, above, consumed through the
existing `observe` path) and no separate `expire`/`sweep` operation is
introduced (Expiry is evaluated lazily by `WorldModelUpdatePolicy`,
consulted from within `current`/`query` themselves, per §5, above).
Extending the existing three operations, rather than adding new ones for
Invalidation and Expiry, is a direct application of this unit's own
instruction to "prefer extending an existing contract over inventing a
new one."

**Required lifecycle.** No internal state machine of its own beyond
whatever a given `WorldBelief` moves through (established, superseded,
invalidated, or allowed to expire) — `WorldModel` is the boundary these
transitions happen behind, not a session or run with its own lifecycle
states.

**Future extensibility.** A belief-category enumeration (§7, below), a
bounded-configuration record for `WorldModelUpdatePolicy` (§5, above),
and richer `WorldQuery` filters (§4, above) can each be added without
changing `WorldModel`'s own three-operation public surface.

### Why no separate `WorldModelRuntime` interface

**Excluded — merged into `WorldModel`.** This is the identical minimalism
decision `MEMORY_CONTRACT_DESIGN.md` §9 reaches for `MemoryStore`/
`MemoryRuntime`, restated here for the World Model rather than re-derived
differently: no subsystem in this codebase has two public interfaces
where one already suffices. `WorldModel` already exists, already names
every operation the World Model needs to expose, and already carries no
responsibility a second wrapper interface could meaningfully add.
Introducing `WorldModelRuntime` as a further public interface would
duplicate an already-adequate contract for the sake of a naming pattern,
which is exactly "creating a type merely for symmetry." The eventual
concrete class — following this codebase's established `InMemory*`
convention — implements `WorldModel` directly, exactly as
`InMemoryToolRegistry : ToolRegistry` and a future `InMemoryMemoryStore : MemoryStore`
already do or will. "The World Model Runtime" remains the correct
informal name for the subsystem as a whole; that name does not require,
and should not force, a second formal interface distinct from
`WorldModel`.

## 7. Why not a belief-category enumeration?

**Not introduced.** Unit B1 §4 names six Information Categories (Device
State, Environment, User Activity, Runtime State, External Services,
Derived Beliefs) as a classification of content, not a required field —
the same framing Unit A1 §3 originally gave Memory Categories before
Unit A2 gave `MemoryCategory` a shape. The two cases are not treated
identically here, and the difference is deliberate, not an oversight:
`MemoryCategory` was introduced because a concrete need for it already
existed in Memory's own accepted architecture — category-scoped
retrieval is named explicitly as something a caller may want
(`MEMORY_RUNTIME_ARCHITECTURE.md` §8, "prior preferences, prior outcomes,
known relationships"). No equivalent concrete need has been identified
anywhere in Unit B1 or in this document's own review: no caller described
in Unit B1 §10 is described as needing to filter by Information Category
rather than by subject, and `WorldQuery`'s own required members (§4,
above) are already sufficient for every consultation Unit B1 names.

Introducing a category enumeration now would be adding structure to
match `MemoryCategory`'s own shape by analogy, which is exactly "creating
a type merely for symmetry" this unit's own instructions forbid. The
Derived Beliefs distinction this document must still express (§1, §2,
above) is satisfied by a much smaller addition — an optional derivation
reference, present or absent — that requires no enumeration at all. If a
concrete filtering or classification need is identified later, adding a
belief-category enumeration then is a clean, additive change to
`WorldObservation`, `WorldBelief`, and `WorldQuery` alike; nothing about
omitting it now forecloses that.

---

## Resolve Outstanding Design Questions

Unit B1 deliberately deferred the following questions to this unit. Each
is resolved here, with a reference to the architecture section that
deferred it and the contract section above that carries the decision
forward.

**WorldState vs. WorldBelief naming.** Resolved: renamed to `WorldBelief`
(§1, above), matching the vocabulary Unit B1 §7 already flagged as
clearer (`16-world-model.md`, `reasoning-context.md`), exactly as
`MemoryRecord` was renamed from the original stub's bare `Memory`.

**`ObservationResult` usefulness.** Resolved: retained, and given the
sealed, three-variant shape (`Accepted`/`Invalidated`/`Rejected`, §3,
above) that absorbs the candidate `WorldModelUpdateDecision` rather than
introducing it as a second type (Unit B1 §7's disclosed, unresolved
"whether it should be retained, merged, or replaced" question).

**Confidence representation.** Resolved: a required (non-optional)
confidence score on both `WorldObservation` and `WorldBelief` (§1, §2,
above), deliberately unlike Memory's optional confidence field, because
`WorldModel.md`'s Normative Requirement is unconditional.

**Timestamp ownership.** Resolved: the authoritative timestamp on
`WorldBelief` is assigned by the World Model itself at Update time, never
trusted from a caller; a submitting source may optionally attach its own
source-reported timestamp to a `WorldObservation` for provenance only
(§1, §2, above). This mirrors `MemoryId`'s own "assigned by `MemoryStore`,
never minted by a caller" treatment, applied here to a timestamp rather
than an identifier, for the same reason: a single, trusted authority for
a value every other Update/Expiry decision depends on.

**Resource identity.** Resolved: `WorldBelief`, `WorldObservation`, and
`WorldModel.current`'s parameter are keyed by a plain, non-blank subject
identifier, not `ResourceId` (§1, §2, §6, above). Unit B1 §7 flagged that
not every Information Category (User Activity, Derived Beliefs in
particular) obviously maps onto a registered Resource; rather than
inventing a new, broader identifier type to cover the gap, this document
widens the existing `current` signature to a plain string, which a
Resource-backed belief may still populate with `ResourceId.value` by
convention, without the World Model itself requiring Resource
registration for every subject it can hold a belief about.

This is worth spelling out at more than the level of "some categories
don't fit," because the scale of the mismatch is what makes `ResourceId`
the wrong key, not merely an imperfect one. `ResourceId` identifies a
registered Resource — Parker's own catalogue of things it can act on or
reference (`docs/architecture/tool-registry.md`'s companion Resource
Registry). But Unit B1 §4's own six Information Categories describe
subjects far broader than that catalogue could ever be expected to hold.
A device (a smart lock, a thermostat) is plausibly a registered Resource
already. A person (the user, a household member the user references) is
not — Identity models Principals, not arbitrary people the World Model
might hold a belief about, and a person is not a Resource. A room or
physical space (the kitchen, "upstairs") is not a Resource either, yet
Environment beliefs are routinely about exactly this kind of subject. A
weather condition is sourced from an External Service, describes no
registered thing at all, and has no owner to register it as one. A
conversation or an in-progress interaction (Runtime State's own example,
"an Agent Run is currently active") is transient by definition — it
would be actively wrong to require registering something as fleeting as
a conversation in the same catalogue that holds durable, addressable
Resources. A temporary, ad hoc runtime concept invented for a single
belief (Derived Beliefs' own "the user is likely away from home") has no
natural registration path at all, and was never meant to have one.
Devices, people, rooms, weather, conversations, and temporary runtime
concepts are all, in principle, equally valid World Model subjects
(Unit B1 §4) — and a device is the *only* one of these six examples that
maps naturally onto `ResourceId` at all. Keying every belief to
`ResourceId` would therefore not merely exclude an edge case; it would
quietly narrow the World Model's actual scope down to "things Parker has
registered," which is a materially smaller set than "things Parker
currently believes something about." A plain subject string imposes no
such narrowing: it accepts a `ResourceId.value` for a device exactly as
readily as it accepts any other non-blank identifier for a person, a
room, a weather condition, a conversation, or an ad hoc concept, without
requiring any of the latter five to first become something they are not.

**Belief replacement.** Resolved: expressed purely behaviourally, not as
a stored field. `ObservationResult.Accepted` covers both establishing a
first belief and superseding an existing one; `WorldBelief` itself
carries no reference to what it replaced, consistent with Unit B1's own
"never historical storage" principle (§1, §3, above).

**Belief invalidation.** Resolved: expressed as a `WorldObservation`
carrying an explicit retraction indicator, consumed through the existing
`observe` operation, producing `ObservationResult.Invalidated`; no
separate public `invalidate` operation is introduced (§2, §3, §6, above).

**Belief expiry.** Resolved: computed lazily by `WorldModelUpdatePolicy`
whenever `current`/`query` is called, from a belief's required timestamp;
no expiry field is stored on `WorldBelief`, and no autonomous background
sweep exists (§1, §5, above).

**Contradictory observations.** Resolved: `WorldModelUpdatePolicy` is
explicitly responsible for resolving them, by weighing confidence and
recency rather than applying a fixed structural rule; `ObservationResult.Rejected`'s
free-text `reason` is the mechanism for expressing a rejection caused by
a stronger competing belief (§3, §5, above).

**Derived beliefs.** Resolved: an optional derivation reference on both
`WorldObservation` and `WorldBelief`, whose presence — not a formal
category field — marks a belief as derived rather than directly sensed,
consistent with the requirement (added to Unit B1's Information
Categories section) that "derived beliefs remain hypotheses, not facts"
(§1, §2, above). No belief-category enumeration is introduced to support
this; see §7, above, for why.

**`WorldModelUpdatePolicy` ownership.** Resolved: internal to the World
Model, injected into `WorldModel`'s implementation, never owned or
substituted by any caller (§5, above).

**Whether external callers ever invoke the update policy directly.**
Resolved: no. `WorldModelUpdatePolicy` has no caller-facing operation; a
source's only interaction with this seam is indirect, through submitting
a `WorldObservation` via `WorldModel.observe` and reading the resulting
`ObservationResult` (§5, above), mirroring `MemoryPromotionPolicy`'s
identical internal-only boundary.

---

## Public Runtime Interface

**Determination: `WorldModel` remains the single public Runtime
interface. No separate `WorldModelRuntime` interface should exist.**

The full justification is given in §6's "Why no separate `WorldModelRuntime`
interface" subsection, above, and is not repeated in full here. In brief:
`WorldModel` already exists, already names every operation this document
identifies as required (`observe`, `current`, `query`), and no
responsibility identified anywhere in Unit B1 or in this document's own
review requires a second public interface to hold. Unlike the Planner
Runtime's own Unit D1A finding — where `InMemoryPlannerRuntime` had no
pre-existing interface at all, a genuine defect that Unit D1A's new
`PlannerRuntime` interface corrected — the World Model has no equivalent
gap: `WorldModel` has existed, named and stable, since before this Sprint
began. Adding a second interface here would not correct a defect; it
would only manufacture one.

---

## Future Extensibility

Seams intentionally deferred, and what would justify introducing each:

- **A belief-category enumeration** (mirroring `MemoryCategory`). Not
  required today: no contract above has an identified need to filter or
  classify by Unit B1 §4's six Information Categories, and Unit B1 §4
  itself states those categories describe content, not a required field.
  It would be justified the moment a concrete caller need for
  category-based filtering on `WorldQuery`, or category-specific
  behaviour in `WorldModelUpdatePolicy`, is identified — for instance, if
  a future Runtime component needs to query "all Environment beliefs"
  specifically rather than by subject.
- **A `WorldModelPolicy` bounded-configuration record** (mirroring
  `AgentPolicy`, and mirroring the deliberately-declined
  `MemoryPolicy`/`PlannerPolicy` equivalents). Not required today: no
  configurable bound on `WorldModelUpdatePolicy`'s own operation has been
  identified. It would be justified the moment a concrete bound is
  needed — for example, a maximum number of Observations evaluated per
  batch, or an update-evaluation timeout.
- **Richer derivation lineage on `WorldBelief`/`WorldObservation`.** The
  optional derivation reference (§1, §2, above) is a minimal signal
  today — presence or absence, not a full dependency graph. A richer
  shape (for example, a weighted list of contributing subjects) would be
  justified the moment a caller needs to reason about *which* underlying
  beliefs most influenced a derived one, not merely that it was derived.
- **Pagination or per-result ranking metadata on `WorldQuery`/`query`**
  (mirroring the declined `MemoryQueryResult`). Not required today:
  `query` returning `List<WorldBelief>` directly is sufficient for every
  caller described in Unit B1 §10. It would be justified the moment a
  caller needs a total-match count distinct from a bounded page, or a
  per-result relevance score, neither of which has a concrete, identified
  need yet.
- **A requesting-Principal field and a correlation identifier on
  `WorldQuery`** (§4, above). Not required today, for the reasons given
  there. Either would be justified the moment a permission-sensitive
  World Model content category or a query-auditability requirement is
  identified.

---

## Constitutional Checks

Every contract above, checked against the Constitutional Boundaries
restated at the top of this document:

| Contract | Represents belief only | Never orchestrates | Never executes | Never reacts autonomously | Never modifies Memory | Consumed, not owned |
| --- | --- | --- | --- | --- | --- | --- |
| `WorldBelief` | ✓ | ✓ (no lifecycle field) | ✓ | ✓ (no self-triggering field) | ✓ | ✓ |
| `WorldObservation` | ✓ (proposed) | ✓ | ✓ no tool reference | ✓ submitted only by explicit caller | ✓ | ✓ |
| `ObservationResult` | ✓ (outcome only) | ✓ | ✓ | ✓ produced only when `observe` is called | ✓ | ✓ |
| `WorldQuery` | N/A (request only) | ✓ | ✓ | ✓ | ✓ | ✓ |
| `WorldModelUpdatePolicy` | ✓ | ✓ decides belief-currency only | ✓ | ✓ consulted, never subscribed to events | ✓ | ✓ internal only |
| `WorldModel` | ✓ | ✓ no orchestration operation | ✓ no execution operation | ✓ every operation is caller-invoked or lazily read-time | ✓ no Memory reference anywhere | ✓ every operation is read or accept, never direct control |

No contract in this document fails any column.

---

## Ownership Summary

A single-table companion to the Constitutional Checks table, above,
answering "who owns what" at a glance rather than requiring a reader to
reconstruct it from each type's own Ownership bullet — the same purpose
`MEMORY_RUNTIME_ARCHITECTURE.md` §6's own Ownership table serves for
Memory, and the same purpose Unit B1 §6's own Ownership table already
serves at the architecture level for the World Model. This table
restates that architecture-level ownership at the contract level,
against the specific types this document defines, rather than
introducing a second, competing ownership model:

| Component | Owns |
| --- | --- |
| A source (Sensor, Plugin, Agent, Runtime, or User) | Submitting a `WorldObservation`, and nothing past that point. No source owns acceptance, a `WorldBelief`, or any decision about its own submission (§2, above; Unit B1 §6). |
| `WorldModel` | The current `WorldBelief` for every subject — construction, replacement, invalidation, and exclusion at expiry. This is the World Model's one true possession: the current answer, not the reasoning behind it. |
| `WorldModelUpdatePolicy` | The decision only — whether a submitted Observation is accepted, invalidates, or is rejected. It owns no stored state of its own and constructs no `WorldBelief` directly (§5, above). |
| The Planner | Read-only consultation of current belief, scoped to the lifetime of one Planning Session; no write path, and no caching of a belief as the Planner's own state beyond that session (Constitutional Boundaries, above; Unit B1 §8). |
| The Agent Runtime, Tool Execution, the Task Manager, Trust, Identity | Read-only consultation, exactly as the Planner's, each for its own purpose (Unit B1 §10); none owns any part of a `WorldBelief`'s lifecycle. |
| Memory | A separate subsystem entirely — no ownership relationship in either direction. Memory does not own, read, or write World Model contracts, and the World Model does not own, read, or write Memory's (Constitutional Boundaries, above; Unit B1 §9). |

No row above overlaps another: exactly one component owns each stage of
a `WorldBelief`'s lifecycle (§1's Lifecycle diagram), and every consuming
component is read-only with no exception.

---

## Concurrency

A correction, made honestly rather than silently: neither
`MEMORY_CONTRACT_DESIGN.md` nor `PLANNER_RUNTIME_CONTRACT_DESIGN.md`
actually states a concurrency requirement explicitly at the contract
level — only the concrete `InMemoryMemoryStore` implementation addresses
it in practice, via an internal `Mutex`, without either contract design
document requiring that of a future implementation in writing. This
document states the requirement explicitly, rather than leaving it to be
independently rediscovered the way `InMemoryMemoryStore` rediscovered it
during implementation: **concurrent Observations for the same subject
must be resolved entirely inside the World Model's own implementation.
Callers must never coordinate updates themselves.**

Concretely, this means a source never checks `current(subject)` before
calling `observe` in order to decide whether its own Observation "should"
win — that would be the caller performing `WorldModelUpdatePolicy`'s own
job, from outside the boundary that is supposed to own it exclusively
(§5, above). This is the World Model's own instance of the same principle
`MEMORY_CONTRACT_DESIGN.md`'s Constitutional Boundaries state directly for
Memory — "Memory never modifies its own contents except through explicit
Runtime operations governed by Memory policy" — and the direct
consequence, for the World Model specifically, of Unit B1 §11's own
boundary that it "never reacts autonomously" beyond updating its own
belief in response to an Observation it itself receives, never in
response to a caller's own coordination logic. Two sources
submitting Observations about the same subject at effectively the same
time must both simply call `observe`; `WorldModel`'s implementation is
responsible for serialising the resulting Validation-and-Update
decisions so that exactly one coherent `WorldBelief` results, never a
race between two callers each assuming their own Observation is the one
that will be accepted. This document does not specify the mechanism (a
lock, an actor, a single-writer queue) — that remains an implementation
concern — only that the responsibility for resolving concurrent
Observations belongs to `WorldModel` alone, never to its callers.

---

## Engineering Review

**Unnecessary contracts removed.** `WorldModelUpdateDecision` is not
introduced as a separate type — it is absorbed into `ObservationResult`,
which already existed and already named the same role. `WorldModelRuntime`
is not introduced — `WorldModel` remains the sole public interface. A
belief-category enumeration is not introduced at all, deferred rather
than added speculatively, because no concrete need for one has been
identified — a stronger form of minimalism than merely deferring a seam
whose need is already clear, since here the need itself is not yet
established.

**Contracts retained.** `WorldModel`'s existing name and three operations
are retained; only `current`'s parameter type changes, from `ResourceId`
to a plain subject string, for the concrete reason given in the Resource
Identity resolution, above — not a wholesale redesign.

**Architectural simplifications.** Replacing `ResourceId` with a plain
subject identifier removes a dependency on Resource registration for
categories of belief that were never obviously Resource-shaped in the
first place. Collapsing Update, Replacement, and the candidate
`WorldModelUpdateDecision` into a single `ObservationResult.Accepted`
variant avoids a redundant second outcome type where `WorldBelief`'s own
"no history" shape already makes "established" and "superseded"
indistinguishable to a caller.

**Remaining deferred work.** A belief-category enumeration, a
`WorldModelPolicy` bounded-configuration record, richer derivation
lineage, pagination/ranking metadata on `WorldQuery`, and a
requesting-Principal/correlation identifier on `WorldQuery` — each named
in Future Extensibility, above, with the concrete trigger that would
justify introducing it.

**Consistency with Memory.** `WorldModelUpdatePolicy` is designed as
`MemoryPromotionPolicy`'s direct structural sibling — internal-only,
never caller-invoked, declared free to suspend for the same AD-010
reason — while two deliberate points of difference are preserved rather
than smoothed away: confidence is required on World Model contracts,
optional on Memory's; `WorldBelief` carries no history, where
`MemoryRecord` is required to carry one. Both differences trace directly
to each subsystem's own accepted architecture, not to inconsistent
design.

**Consistency with Planner.** `ObservationResult.Rejected`'s free-text
`reason` deliberately does not mirror `PlanRejection`'s closed-enum
shape, for the same reason `MemoryPromotionDecision.Reject.reason`
does not: a weighing decision, not a fixed set of mechanically-checkable
structural rules.

**Constitutional consistency.** Confirmed in full by the Constitutional
Checks table, above; no contract fails any column.

### What this review recommends removing, or adding, before implementation begins

- **Remove (already excluded above):** `WorldModelUpdateDecision`,
  `WorldModelRuntime`. Neither should be implemented.
- **Revise from the existing stub:** `WorldState` should be implemented
  as `WorldBelief`; `WorldModel.current`'s parameter should be
  implemented as a plain subject string, not `ResourceId`. Both are
  field-level corrections to an existing, already-approved-at-the-
  operation-naming-level interface, not new architecture — per AD-013,
  the existing stub's naming was a first approximation, not a binding
  field-level signature.
- **Not added, and deliberately so:** a belief-category enumeration, a
  `WorldModelPolicy` bounded-configuration record, a requesting-Principal
  or correlation identifier on `WorldQuery`. Each is named in Future
  Extensibility, above, as an additive change available the moment a
  concrete need is identified — none is needed to begin implementation.

---

## Future Implementation Note

An initial implementation is expected to be an in-memory implementation
following the established `InMemory*` runtime convention already used
throughout this codebase (`InMemoryToolRegistry`, `InMemoryIdentityService`,
`InMemoryPlannerRuntime`, and a future `InMemoryMemoryStore`) — most
plausibly `InMemoryWorldModel`, implementing `WorldModel` directly and
resolving the Concurrency section's own requirement with whatever
in-process synchronisation mechanism (a mutex, most directly, mirroring
`InMemoryMemoryStore`'s own precedent) that implementation unit chooses.
Persistent implementations — anything that survives a process restart or
is backed by real storage — remain future work, out of scope for this
document exactly as storage engines and persistence generally are (Unit
B1's own Out of Scope section). Naming the expected first implementation
here does not authorise writing it; that remains a future implementation
unit's own task, gated on review of this document exactly as Unit A3 was
gated on review of `MEMORY_CONTRACT_DESIGN.md`.

---

## Self-Traceability Review

For every public contract, whether it is derived from existing
architecture or newly introduced by this document:

| Contract | Derived, or new? |
| --- | --- |
| `WorldModel` | **Derived.** Already exists, verbatim, in `src/interfaces/WorldModel.kt`. This document retains its name and its three operations, revising only `current`'s parameter type (see the Resource Identity resolution). |
| `WorldObservation` | **Derived, newly shaped.** Already named as `observe`'s parameter type in the existing stub. This document gives it its first field-level shape. |
| `ObservationResult` | **Derived, newly shaped.** Already named as `observe`'s return type in the existing stub. This document gives it its first field-level shape — a three-variant sealed outcome — and uses it to absorb the candidate `WorldModelUpdateDecision` rather than introducing a second type. |
| `WorldBelief` | **Derived, renamed and newly shaped.** Already named `WorldState` in the existing stub, as the return type of `current`/`query`. This document renames it, per Unit B1 §7's own naming observation, and gives it its first field-level shape. |
| `WorldQuery` | **Derived, newly shaped.** Already named as `query`'s parameter type in the existing stub. This document gives it its first field-level shape. |
| `WorldModelUpdatePolicy` | **Newly introduced by Unit B1, given a contract shape here.** No interface by this name, or performing this role, existed anywhere in the repository before Unit B1 introduced it as a genuinely new architectural concept (Unit B1's own Self-Traceability Review). This document does not introduce the concept; it gives the concept Unit B1 already introduced its first field-level shape. |

No contract beyond these six is proposed by this document.
`WorldModelUpdateDecision` and `WorldModelRuntime` were considered and
explicitly excluded (§3, §6, above) rather than silently omitted. A
belief-category enumeration was considered and explicitly not
introduced (§7, above, and within Future Extensibility) rather than silently
overlooked.

---

Track B is ready for World Model Runtime implementation.
