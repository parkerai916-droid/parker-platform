# Memory Contract Design

## Status

Sprint: Sprint 4, Track A, Unit A2 (Contract Design)
Version: 0.1-draft
Status: **Design proposal, not yet reviewed or accepted.**

**This document is contract design only.** No Kotlin is implemented,
proposed as a diff, or changed by it. Neither `src/` nor `tests/` is
touched. `docs/implementation/IMPLEMENTATION_HISTORY.md` and
`docs/architecture/IMPLEMENTATION_GAPS.md` are both untouched. No other
document is modified or created.

### Why this unit exists

Sprint 4, Track A, Unit A1 (`docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md`)
placed Memory inside Parker's Runtime Foundation: what it is, what it is
not, its responsibilities, its lifecycle, its ownership, and its
constitutional boundaries. It deliberately stopped short of field-level
design — it identified that a public contract would eventually be
needed (a read/write store, a promotion-policy seam, a
retention/consolidation seam, a query shape) without shaping any of
their fields, exactly as Track D's Unit D1 stopped short for the Planner
Runtime. This document is Unit A1's Unit D1A: it performs the field-level
design pass Unit A1 explicitly deferred, so that a future implementation
unit builds Memory by implementing an already-approved contract set,
never by inventing one mid-Kotlin.

This document also performs, up front and throughout, the contract
minimalism review this Sprint requires: every contract below is included
only where it has a distinct architectural justification, two contracts
are merged wherever merging loses no clarity or extensibility, and four
of the fifteen candidate names this unit was asked to consider are
recommended for exclusion, with reasons. Minimalism is not treated as a
final pass over an already-decided list — it is the standard each
candidate is measured against from the start.

## Review

Reviewed, in priority order:

1. `docs/architecture/parker-constitution.md`.
2. `docs/architecture/ARCHITECTURE_DECISIONS.md`, especially AD-011
   (Context Is Reference-Based), AD-012 (Memory and World Model Are
   Context Providers), and AD-013 (Specifications Define Contracts).
3. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001).
4. `docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md` (Unit A1, accepted)
   — the architecture this document implements as contracts and does not
   redefine.
5. `docs/architecture/reasoning-context.md` — the Memory / World Model /
   Reasoning Context split this document conforms to.
6. `docs/architecture/16-world-model.md`,
   `docs/specifications/volume-03-core-interfaces/WorldModel.md` — the
   World Model principles already adopted.
7. `docs/architecture/17-memory-architecture.md`,
   `docs/architecture/33-memory-consolidation.md`,
   `docs/adr/ADR-008-memory-promotion.md` — Memory's existing prose
   architecture.
8. `src/interfaces/MemoryStore.kt` and
   `docs/specifications/volume-03-core-interfaces/MemoryStore.md` — the
   existing, already-approved interface-naming-level contract this
   document must implement as field-level design, not redefine at the
   operation-naming level.
9. `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` (Unit D1A) and
   `docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md` (Unit C1) — this
   document's own direct precedent and required point of symmetry
   comparison.
10. `src/contracts/PlanDecision.kt`, `src/contracts/AgentStep.kt`,
    `src/contracts/TaskProposal.kt`, `src/contracts/Identifiers.kt` — the
    concrete, already-implemented Kotlin these contracts must match in
    pattern (identifier shape, sealed-outcome shape, policy-seam shape).

---

## Constitutional Boundaries

Restated up front, since every contract below is checked against these —
identical in substance to Unit A1's own Architectural Principle and
Constitutional Boundaries sections, not re-derived differently here:

- **Memory stores knowledge. Memory never decides. Memory never acts.**
  No contract in this document gives Memory a path to decide what Parker
  should do, or to act on that decision itself.
- **Memory never plans.** No contract here generates, evaluates, or
  selects a Plan Candidate, or produces anything resembling a
  `PlanDecisionResult`.
- **Memory never authorises.** No contract here grants, evaluates, or
  overrides a Permission Engine decision, including for Memory's own,
  sensitive contents.
- **Memory never executes.** No contract here references `Tool`,
  `ToolInvocationBinding`, or `ExecutionRequest`.
- **Memory never reacts autonomously.** No contract here is triggered
  directly by an event crossing the Event Bus; every Memory operation is
  invoked by an explicit caller, never by a subscription that promotes or
  forgets on its own.
- **Memory provides knowledge only.** Every contract below either
  accepts knowledge (`CandidateMemory`) or returns it
  (`MemoryRecord`/`retrieve`); none of them issues an instruction to any
  other subsystem.
- **Memory never modifies its own contents except through explicit
  Runtime operations governed by Memory policy.** Every change to a
  `MemoryRecord` — its creation via Promotion, its combination via
  Consolidation, its reconsideration via Retention, its removal via
  `forget` — happens only as the direct, explicit result of an operation
  `MemoryStore` exposes, evaluated by the policy seam that governs it
  (`MemoryPromotionPolicy`, and the deferred retention/consolidation
  seam). This principle exists to rule out a specific failure mode none
  of the contracts above happen to name directly: nothing in this
  architecture may promote, rewrite, summarise, or delete a
  `MemoryRecord` in the background, on a timer, or as a side effect of
  some other subsystem's activity, without passing through one of these
  explicit, policy-governed operations. This is the same rule already
  stated behaviourally in Unit A1 §11 ("Memory never reacts autonomously
  to events... there is no `Event -> Memory promotes automatically`
  path") — restated here as a boundary on Memory's own contents
  specifically, covering rewriting, summarisation, and deletion as well
  as promotion, so that no future contract is read as licensing any of
  those four as an unattended, automatic act.

---

## Contract Minimalism Review — Summary

Fifteen candidate names were posed for this unit's consideration. The
determination for each, in brief (each is justified in full in its own
section below):

| Candidate | Determination |
| --- | --- |
| `MemoryId` | **Include.** Follows the established identifier pattern. |
| `CandidateMemoryId` | **Exclude — merge into `MemoryId`.** |
| `CandidateMemory` | **Include.** |
| `MemoryRecord` | **Include**, with a naming clarification against the existing stub. |
| `MemoryCategory` | **Include**, as a single closed enum, not a type hierarchy. |
| `MemoryPromotionDecision` | **Include**, shaped more simply than its Planner analogue. |
| `MemoryPromotionPolicy` | **Include.** The central seam this unit designs. |
| `MemoryQuery` | **Include.** |
| `MemoryQueryResult` | **Exclude.** `retrieve` returns `List<MemoryRecord>` directly. |
| `MemoryRetrievalPolicy` | **Include, but deferred** — not required for a first implementation. |
| `MemoryStore` | **Include.** Already exists; remains Memory's one public interface. |
| `MemoryRuntime` | **Exclude — merge into `MemoryStore`.** |
| `MemoryObservation` | **Exclude.** Observation is explicitly not Memory's own contract. |
| `MemoryConsolidationPolicy` | **Deferred, and not split from Retention** — kept as Unit A1's single combined seam. |
| `MemoryRetentionPolicy` | **Deferred, and not split from Consolidation** — see above. |

Net result: **eight required contracts** (`MemoryId`, `CandidateMemory`,
`MemoryRecord`, `MemoryCategory`, `MemoryPromotionDecision`,
`MemoryPromotionPolicy`, `MemoryQuery`, `MemoryStore`), **two deferred
seams** (`MemoryRetrievalPolicy` and a single combined
retention/consolidation seam, neither required for a first
implementation), and **four candidates excluded outright**
(`CandidateMemoryId`, `MemoryQueryResult`, `MemoryRuntime`,
`MemoryObservation`), each with the reasoning below.

---

## 1. `MemoryId`

- **Purpose.** Identifies one Memory record across its entire lifecycle
  — from the moment a `CandidateMemory` is submitted through however
  long the resulting `MemoryRecord` remains retrievable.
- **Responsibilities.** Nothing beyond identity: distinguishing one
  Memory record from another. It carries no promotion status, no
  category, and no content.
- **Ownership.** Assigned by `MemoryStore` at submission time (see
  `CandidateMemory`, below); never minted by a caller.
- **Relationships.** Referenced by `CandidateMemory` implicitly (it is
  the return value of submission, not a field the submitter supplies),
  and by `MemoryRecord`, `MemoryPromotionDecision`, and `MemoryQuery`
  explicitly.
- **Required public members.** A single, non-blank string value.
- **Required lifecycle.** Assigned once, at submission; never reassigned,
  never reused for a different record, retained for the life of the
  record including through `forget` (a forgotten record's `MemoryId` is
  not recycled — `MemoryStore.md`'s "Forgetting MUST be auditable"
  requires the identifier to remain meaningful in an audit trail even
  after the content it named is gone).
- **Future extensibility.** None needed; an identifier type is not
  expected to grow fields.

### Does `MemoryId` follow the established Parker pattern?

**Yes, explicitly.** Every other long-lived object Parker owns already
uses the same shape: a single-field value type wrapping a non-blank
`String`, validated at construction, with no behaviour beyond identity —
`PrincipalId`, `ResourceId`, `RequestId`, `DecisionId`, `ResultId`
(`src/contracts/Identifiers.kt`), `PlanCandidateId`, `TaskProposalId`,
`TaskId`, `PlanningSessionId` (`src/contracts/PlanDecision.kt`,
`src/contracts/TaskProposal.kt`), and `AgentRunId`
(`src/contracts/AgentRunCommand.kt`). `MemoryId` should follow this
pattern exactly: a validated, blank-rejecting, single-value identifier,
no different in kind from any of the above. This is not a new decision —
`src/interfaces/MemoryStore.kt` already names a type called `MemoryId` in
this exact role; this document confirms that name and shape are correct
and gives it the same construction-time validation every sibling
identifier already has.

### Why not `CandidateMemoryId` as well?

**Excluded.** The existing, already-approved `MemoryStore.kt` stub
already settles this question, and this document only needed to notice
it: `addCandidate(candidate: CandidateMemory): MemoryId` returns a bare
`MemoryId`, not a `CandidateMemoryId`, and `promote(memoryId: MemoryId): Memory`
accepts that same type back. The existing interface already commits to
one identifier space, assigned at submission and carried unchanged
through promotion — it does not model Candidate Memory and Long-term
Memory as two different objects each needing their own identity.

This is a structurally different situation from the Planner Runtime's
`PlanCandidateId` versus `TaskProposalId`/`TaskId`, and the difference is
real, not cosmetic: a winning `PlanCandidate` is *read once* to construct
a wholly separate object (`TaskProposal`), owned by a different
subsystem (the Task Manager), which is then read again to construct a
*third* object (`TaskManagerTask`), owned by yet another lifecycle — three
genuinely different objects crossing two ownership boundaries, each
correctly needing its own identity space. A Candidate Memory becoming a
Memory Record is not that: both are owned by Memory throughout (Unit A1's
own "Memory owns Candidate Memory from the moment it is submitted"), and
Promotion is a state change of one record, not the construction of a
different object owned by a different subsystem. Minting a second
identifier type here would be symmetry with `PlanCandidateId` for its
own sake, contradicted by the existing interface's own signature. It is
excluded.

## 2. `CandidateMemory`

- **Purpose.** What a subsystem submits when it observes something that
  might be worth remembering — the shape of a proposal for retention,
  before Evaluation, per Unit A1 §4 (Lifecycle).
- **Responsibilities.** Carries exactly what `MemoryPromotionPolicy`
  needs to evaluate the submission, and what a promoted `MemoryRecord`
  will need to be constructed from if the submission is promoted.
  Carries nothing else.
- **Ownership.** Constructed by the submitting subsystem (see Input
  Sources, Unit A1 §6); owned by Memory from the instant it is submitted
  (Unit A1's explicit ownership clarification). `MemoryStore.addCandidate`
  is the only path by which a `CandidateMemory` enters Memory's custody.
- **Relationships.** Consumed by `MemoryPromotionPolicy`; its fields, if
  promoted, seed the corresponding fields of the resulting
  `MemoryRecord` — a field-for-field carry-forward relationship, the
  same pattern `PlanCandidate`'s optional fields already use against
  `TaskProposal`.

**What it must carry:**

- A **knowledge payload** — the content actually being proposed for
  retention. This document does not shape its internal structure (that
  is storage-format territory, explicitly out of scope), only that a
  payload must be present; a blank or absent payload is not a valid
  submission.
- A **proposed category** (`MemoryCategory`, below) — the submitting
  subsystem's best classification of what kind of knowledge this is.
  Proposed, not final: `MemoryPromotionPolicy` may confirm or correct it
  during Evaluation, since the submitter's own classification carries no
  authority (cognition proposes; Memory's own policy decides).
- **Provenance** — a reference to where this observation came from: the
  submitting subsystem, and, where one exists, the originating Principal
  and a correlation identifier tying this submission back to the task or
  session that produced it. Required because `MemoryStore.md`'s own
  normative requirement — "Memories MUST preserve provenance" — cannot
  be satisfied retroactively if it is not captured at submission.
- A **confidence indicator**, where the submitter has one, since
  `33-memory-consolidation.md` names "confidence" as one of the six
  promotion factors `MemoryPromotionPolicy` weighs. Optional at
  submission: not every submitting subsystem will have a confidence
  figure to offer, and `MemoryPromotionPolicy` must not assume one is
  always present.
- An **explicit-request flag**, since `33-memory-consolidation.md` names
  "explicit request" as its own distinct promotion factor, separate from
  confidence or repetition — a user directly asking Parker to remember
  something is architecturally different evidence than Parker noticing a
  pattern on its own, and `CandidateMemory` must be able to say which
  happened.

**What it intentionally does not carry:**

- **No promotion decision, and no `MemoryRecord` fields that only make
  sense once promoted** (no retention window, no consolidation history —
  those belong to `MemoryRecord`, constructed only after Evaluation
  decides in this submission's favour).
- **No authority of any kind.** Submitting a `CandidateMemory` is
  cognition proposing (Unit A1 §4); it carries no permission grant, no
  execution instruction, and no ability to force its own promotion,
  regardless of which subsystem submits it (Unit A1 §6, "None of these
  gains promotion authority merely by submitting").
- **No self-reported repetition or frequency figure.** Unlike
  confidence and explicit-request (which only the submitter can know),
  repetition and frequency are evaluated by comparing a submission
  against Memory's own existing records — that comparison is
  `MemoryPromotionPolicy`'s job, performed during Evaluation, not
  something the submitter can assert about itself.
- **No ranking or relevance score.** That is a retrieval-time concept
  (`MemoryRetrievalPolicy`, deferred below), not a submission-time one.

- **Required lifecycle.** Constructed once by the submitting subsystem;
  submitted via `MemoryStore.addCandidate`, which returns the `MemoryId`
  assigned to it; consumed exactly once by `MemoryPromotionPolicy`
  during Evaluation; discarded after Evaluation regardless of outcome
  (on promotion, its fields seed a new `MemoryRecord`; on rejection, only
  the `MemoryPromotionDecision.Reject` and its `MemoryId` are retained,
  for auditability, not the full candidate).
- **Future extensibility.** Additional optional provenance fields (e.g.
  a richer source-context reference) can be added the same low-risk way
  `PlanCandidate`'s own optional carry-forward fields were — additive,
  defaulted, never a breaking change to what already exists.

## 3. `MemoryRecord`

- **Purpose.** The durable, promoted representation of a Long-term
  Memory (Unit A1 §4). The result of a successful Promotion, and the
  type `retrieve` returns.
- **Responsibilities.** Represent one durable piece of learned
  knowledge, identifiable, attributable, and classifiable, without
  specifying how it is stored.
- **Ownership.** Constructed by `MemoryStore` at the moment of
  Promotion; owned by Memory for the whole of its retained lifetime,
  including through Consolidation, Retention review, and eventual
  Deletion.
- **Relationships.** Constructed from a promoted `CandidateMemory`'s
  fields (its knowledge payload, its confirmed category, its provenance)
  plus fields that only exist after Promotion (its `MemoryId`, its
  promotion timestamp). Returned by `retrieve`, consumed by whichever
  subsystem queried Memory (Unit A1 §9).

**Field groups, separated as required:**

- **Required identity.** `MemoryId`. Nothing else is required to
  identify a record; no secondary or compound key is needed.
- **Required metadata.** The confirmed `MemoryCategory`; the provenance
  captured at submission (source subsystem, originating Principal where
  one exists); the promotion timestamp; a sensitivity indicator
  sufficient for the Permission Engine to evaluate a disclosure decision
  against (`MemoryStore.md`: "Sensitive memories MUST require
  appropriate permission" — Memory must carry enough to ask the
  question, even though it never answers it itself, per Unit A1 §10).
- **Optional metadata.** A confidence figure (carried forward from the
  `CandidateMemory` if one was supplied); a retention hint (if
  Evaluation or a later Retention review sets one); references to
  related `MemoryRecord`s (relevant chiefly to the Relationships
  category, but not restricted to it).
- **Knowledge payload.** The actual learned content, carried forward
  unchanged from the promoted `CandidateMemory`. This document does not
  shape its internal structure — that is storage-format territory,
  explicitly out of scope for this unit.
- **History.** A record sufficient to reconstruct what happened to this
  `MemoryRecord` over time. At minimum, the audit trail must be able to
  express four kinds of event: **promoted** (when, and from what
  submission), **consolidated** (combined with, or absorbed into, which
  other record(s)), **forgotten** (per `MemoryStore.forget`), and
  **superseded** — a record whose knowledge has been replaced by a later,
  more current one, without the earlier record being forgotten outright
  (the earlier record remains part of the auditable history; it is no
  longer the current answer `retrieve` should prefer). Superseded is
  named here as an acknowledged future lifecycle and audit event, not a
  fully designed one: this document does not define what triggers a
  supersession, what field records it, or how `retrieve` should treat a
  superseded record differently from a merely-old one — those are
  field-level and implementation questions for a future unit. Naming it
  now only ensures the audit trail's shape is not designed as though
  supersession will never happen, which would foreclose it later.
  Required because both `33-memory-consolidation.md` ("Memory promotion
  is conservative and auditable") and `MemoryStore.md` ("Forgetting MUST
  be auditable") require an audit trail, and an audit trail cannot be
  reconstructed from a record that discards its own history the moment
  something happens to it.

- **Required lifecycle.** Constructed once, at Promotion; may be updated
  by Consolidation (combined with related records) or by a Retention
  review (its retention hint revised); ultimately either remains
  retrievable indefinitely or is forgotten via `MemoryStore.forget`,
  which removes the record from `retrieve`'s results while preserving an
  auditable trace that the deletion occurred (Unit A1 §5).
- **Future extensibility.** Additional optional metadata fields (e.g. a
  richer relationship-reference shape) can be added additively, exactly
  as `MemoryRecord`'s companion contracts allow.

### A naming clarification against the existing stub

`src/interfaces/MemoryStore.kt` currently names this type `Memory` (`fun
promote(memoryId: MemoryId): Memory`). This document recommends renaming
it to `MemoryRecord` when the field-level Kotlin is written. This is a
naming clarification, not a redefinition of the existing architecture:
every operation, every responsibility, and every field group above is
identical to what `MemoryStore.md` already commits to; only the type's
name changes. The reason is concrete, not cosmetic: "Memory" already
names the subsystem itself throughout this document, Unit A1, the
Constitution, and `reasoning-context.md`; using the identical bare word
`Memory` as a concrete return type's name, too, invites exactly the kind
of ambiguity a reader would otherwise have to resolve from context every
time it appears. `MemoryRecord` says the same thing unambiguously. This
recommendation is flagged explicitly, as this session's practice
requires, rather than applied silently.

## 4. `MemoryCategory`

- **Purpose.** Classify a `CandidateMemory` or `MemoryRecord` by kind of
  knowledge, per Unit A1 §3.
- **Responsibilities.** Nothing beyond classification — it carries no
  behaviour, and it does not itself determine promotion, retention, or
  retrieval eligibility (those remain `MemoryPromotionPolicy`'s,
  Retention's, and `MemoryRetrievalPolicy`'s jobs respectively; a
  category is an input to each of those, never a decision by itself).
- **Ownership.** Proposed by the submitting subsystem on `CandidateMemory`;
  confirmed (or corrected) by `MemoryPromotionPolicy` on promotion; fixed
  thereafter on `MemoryRecord`.
- **Relationships.** A field of both `CandidateMemory` and `MemoryRecord`.

**Shape.** A single closed enumeration with five values — Episodic,
Semantic, Procedural, User Preferences, Relationships — exactly as Unit
A1 §3 names them, and nothing more. This document deliberately does not
model the five categories as five separate types (e.g. a sealed
`EpisodicMemory`/`SemanticMemory`/... hierarchy carrying
category-specific fields), even though Unit A1 explicitly left that
choice open. A single discriminating enum field is the minimum structure
that satisfies Unit A1 §3's own framing — "these categories describe
Memory's content, not its ownership or lifecycle... every category is
subject to the same lifecycle, ownership, and constitutional
boundaries" — and no category-specific field requirement has been
identified anywhere in Unit A1 or in this document's own review. Building
five parallel schemas now, with no known field that needs to differ
between them, would be exactly the kind of structure this Sprint's
minimalism review exists to prevent. If a future unit discovers a field
that only makes sense for one category, that is the point at which
splitting becomes justified — not before.

- **Required lifecycle.** Assigned once (proposed at submission,
  confirmed at promotion); may be corrected during Consolidation if two
  records are found to be miscategorised relative to each other, but is
  not expected to change routinely.
- **Future extensibility.** A sixth category can be added to the
  enumeration additively if a genuinely new kind of knowledge is
  identified; this is a low-risk, backward-compatible kind of change
  (existing records and code that switch over the enum must handle the
  addition, exactly as any closed-enum addition requires elsewhere in
  this codebase).

## 5. `MemoryPromotionDecision`

- **Purpose.** The outcome of one `MemoryPromotionPolicy` evaluation of
  one `CandidateMemory`.
- **Responsibilities.** State, exhaustively, whether a submission was
  promoted or rejected, and carry enough detail to explain why.
- **Ownership.** Produced by `MemoryPromotionPolicy`; consumed by
  `MemoryStore`, which acts on it (see `MemoryStore`, below), and
  retained, at least in summary, as part of the resulting record's or
  rejection's audit trail.
- **Relationships.** Consumes a `CandidateMemory`; if it promotes,
  its `Promote` variant is the direct precursor of a constructed
  `MemoryRecord`.

**Shape.** A sealed outcome with exactly two variants: `Promote` (the
submission is accepted, carrying the confirmed `MemoryCategory` and the
`MemoryId` already assigned at submission) and `Reject` (the submission
is declined, carrying that same `MemoryId` and a non-blank, free-text
`reason`).

This is deliberately simpler than `PlanDecisionResult`, and the
difference is a considered one, not an oversight: `PlanDecisionResult`
evaluates a whole batch of `PlanCandidate`s together and must express
"no candidate in this batch was viable" as its own case
(`NoViableCandidate`), because Plan Decision is a one-time, whole-batch
choice among competitors. Memory's Evaluation is not a batch choice among
competitors — each `CandidateMemory` is submitted and evaluated
individually, on its own merits, whenever it arrives, with no other
candidate competing against it in the same call. There is no
"NoViableCandidate"-equivalent case to express, because there is no
batch. Two variants are sufficient.

The `Reject` variant's `reason` is deliberately a free-text `String`,
not a closed enum like `PlanRejection`'s `PlanRejectionReason`, and this
is also a considered difference, not an inconsistency with the Planner
Runtime's own precedent. `PlanRejectionReason`'s four values are exact,
mechanically-checkable structural rules (duplicate id, blank goal, goal
mismatch, not-selected) — each `PlanCandidate` either violates one of
them or it does not, with no judgment involved. A Memory promotion
decision is not a structural-validity check; it is a weighing of several
named factors together (`33-memory-consolidation.md`: repetition, user
importance, goal relevance, frequency, confidence, explicit request),
none of which is individually a pass/fail rule. Collapsing that
multi-factor judgment into one closed enum value would misrepresent it as
a single discrete rule, which it is not. A free-text `reason` — the same
treatment `PlanningSessionResult.Failed.reason` already receives for an
analogous "one-off summary, not a repeatedly-applied rule set" case — is
the honest shape here.

- **Required lifecycle.** Constructed exactly once per `CandidateMemory`
  evaluated; consumed immediately by `MemoryStore`; not retained in full
  afterward (only its `Promote`/`Reject` outcome and `reason`, where
  applicable, persist as part of the audit trail).
- **Future extensibility.** A third variant is not anticipated and is
  not designed for here; if one is ever needed (e.g. a "defer this
  evaluation" outcome, symmetrical to a possible future
  `PlanDecisionResult` addition), it is an additive change to this sealed
  type, not a redesign.

## 6. `MemoryPromotionPolicy`

- **Purpose.** The seam by which Memory decides whether a submitted
  `CandidateMemory` is promoted — Unit A1 §5's "Promotion, applying
  Parker's memory policy," given a concrete shape for the first time.
  Stated without hedging: **`MemoryPromotionPolicy` SHALL determine
  whether a `CandidateMemory` becomes a `MemoryRecord`.** Nothing else in
  this architecture makes that determination, and nothing downstream of
  it is entitled to second-guess it — `MemoryStore` acts on the decision
  this seam returns; it does not re-evaluate it.
- **This is a Memory-internal policy decision, not advice to an external
  caller.** An external caller — the Planner Runtime, the Agent Runtime,
  a plugin, a workflow — submits a `CandidateMemory` and is entitled to
  learn the outcome (via the result `MemoryStore` returns for the
  submission, per §9 below); it is never entitled to invoke
  `MemoryPromotionPolicy` itself, to see its reasoning before the
  decision is final, or to override, appeal, or bypass it. This mirrors
  exactly how a caller of the Planner Runtime never invokes
  `PlanDecision` directly — the decision seam is a collaborator internal
  to the runtime that owns it, not a service exposed to whoever is
  outside asking a question.
- **Responsibilities.** Evaluate one `CandidateMemory` against the
  promotion factors `33-memory-consolidation.md` already names and
  produce a `MemoryPromotionDecision`. Nothing else: it does not store
  anything, does not retrieve anything, and does not itself construct a
  `MemoryRecord` (that remains `MemoryStore`'s job, acting on the
  decision this seam returns).
- **Ownership.** Injected into `MemoryStore`'s implementation; today's
  Memory Runtime has no implementation of its own yet, exactly as
  `PlanDecision` had none until `DefaultPlanDecision`.
- **Relationships.** Consumes `CandidateMemory`; produces
  `MemoryPromotionDecision`; consulted internally by `MemoryStore` as
  part of handling a submission — never called directly by an external
  caller of Memory, per the architectural decision in §9 below that
  promotion is not a caller-facing operation.

**Required public members.** One operation: given a `CandidateMemory`,
produce a `MemoryPromotionDecision`. `MemoryPromotionPolicy` SHALL be
declared suspending, for the same reason `PlanDecision.decide` is:
`33-memory-consolidation.md`'s own promotion factors (frequency,
repetition) may eventually require comparing a submission against a
large or externally-stored population of existing records, and a future
policy implementation — model-backed, human-in-the-loop, or simply
backed by a persistence layer with real I/O — must not be foreclosed by
a non-suspending signature decided now, before any implementation exists.
This mirrors `PlanDecision`/`AgentStepSource`'s own reasoning exactly:
none of today's simplest implementations need to suspend, but the
interface must leave room for the ones that will.

**Required lifecycle.** Consulted exactly once per submitted
`CandidateMemory`, during Evaluation.

**Future extensibility.** Replaceable independently of `MemoryStore`
itself — a smarter or model-backed promotion policy can be substituted
without changing how Memory is stored or retrieved, exactly as
`DefaultPlanDecision` can be swapped without touching
`InMemoryPlannerRuntime`'s own structure.

### Comparing the three policy seams

`MemoryPromotionPolicy`'s genuine structural sibling is `PlanDecision`
(Planner Runtime) and `AgentStepSource` (Agent Runtime) — not
`AgentPolicy`. This is a precision worth stating explicitly, since Unit
A1's own §6 compared `MemoryPromotionPolicy` to "`AgentPolicy` in the
Agent Runtime," and that comparison, while directionally right in
spirit, names the wrong Agent Runtime type. `AgentPolicy`
(`src/contracts/AgentStep.kt`) is a bounded-configuration data class —
`maxAgentSteps`, `maxAgentRunDuration` — consumed by the Agent Runtime as
a limit, with no decision-making method of its own. The Agent Runtime's
actual decision-making seam, structurally identical to `PlanDecision`,
is `AgentStepSource` (`suspend fun nextStep(context: AgentStepContext): AgentStepDecision`).
The three genuine decision seams, side by side:

```
AgentStepSource        -- Agent Runtime:   which step runs next
PlanDecision           -- Planner Runtime: which candidate is selected
MemoryPromotionPolicy  -- Memory:          which candidate is promoted
```

Each is a `suspend fun` accepting the thing to be decided about and
returning a sealed outcome; each is injected into its runtime rather than
owned by it; each was named as a concept before this Sprint's
contract-design pass gave it a shape.

`AgentPolicy` has no equivalent proposed for Memory here, and this is a
deliberate omission, not an oversight: Track D itself declined to add an
equivalent `PlannerPolicy` bounded-configuration type, because "this
implementation needs no configurable bound (no `maxPlanCandidates`, no
timeout)." The same reasoning applies to Memory: no configurable bound
(a maximum candidates-per-batch limit, a promotion-evaluation timeout) has
been identified as necessary by Unit A1 or by this document's own review.
If one is identified later — for instance, if a future implementation
needs to cap how many `CandidateMemory` submissions are evaluated per
batch — a `MemoryPolicy` bounded-configuration record can be added then,
additively, exactly as `AgentPolicy` was added when Track C needed one.
Adding it now, with no concrete need, would be inventing a type for
symmetry alone.

## 7. `MemoryQuery`

- **Purpose.** Describe what a caller is asking Memory to retrieve —
  Unit A1 §6's "Memory query contract," given a shape.
- **Responsibilities.** Carry a request; perform no retrieval itself
  (`MemoryStore.retrieve` is the operation that acts on it).
- **Ownership.** Constructed by the caller (the Planner Runtime, the
  Agent Runtime, the Task Manager, or Reasoning Context assembly, per
  Unit A1 §9); consumed by `MemoryStore`.
- **Relationships.** Passed to `MemoryStore.retrieve`; conceptually
  consulted by `MemoryRetrievalPolicy` (deferred, below) once that seam
  exists.

**Required public members.** The requesting Principal (so retrieval can
be scoped by identity, per Unit A1 §9 — "Memory may scope retrieval to a
requesting Principal"); the relevance criteria the caller is asking
against (at minimum, free text describing the topic or Goal in question,
optionally narrowed by `MemoryCategory`); a correlation identifier, for
the same auditability reason `TaskProposal.correlationId` and
`PlanningRequest.correlationId` already exist; and a **`maximumResults`
bound**.

`maximumResults` is an architecturally justified constraint, not an
implementation detail smuggled into the query shape: Memory retrieval
must not imply "return everything matching," and without a caller-stated
bound, nothing in this contract prevents a query from resolving to an
unbounded flood of records — which is itself a quiet violation of Unit
A1 §4's own retrieval bullet ("Retrieval returns the most relevant
memories matching the supplied query, not an unfiltered dump of
everything stored"). Stating that bound is the caller's responsibility,
belonging on the request, not on `MemoryRetrievalPolicy` — the policy
decides *which* matching records are most relevant; the caller decides
*how many* it wants back. This document does not define how
`maximumResults` interacts with ranking, or what a reasonable default or
ceiling is — that remains `MemoryRetrievalPolicy`'s and a future
implementation's concern, per this unit's own scope.

**What it must not carry.** A ranking or scoring formula, or any
parameter that presupposes a specific retrieval algorithm — this
document defines the request's shape only, per this unit's own explicit
instruction, and leaves how relevance is computed entirely to
`MemoryRetrievalPolicy`.

- **Required lifecycle.** Constructed once per retrieval request;
  consumed once by `MemoryStore.retrieve`; not retained afterward beyond
  whatever audit trail Memory keeps of who asked for what.
- **Future extensibility.** Additional optional filters (e.g. a time
  range, a specific `MemoryCategory` set) can be added additively.

### Why not `MemoryQueryResult`?

**Excluded.** `MemoryStore.retrieve` already returns `List<Memory>` (this
document's `List<MemoryRecord>`) directly, and this document recommends
keeping it that way rather than introducing a wrapper type. A wrapper
would only be justified if a caller needs something beyond the matching
records themselves — a total-match count distinct from a truncated page,
a per-result ranking score, a pagination cursor — and none of those has a
concrete, identified need yet: this unit is explicitly barred from
designing ranking algorithms, and no caller described in Unit A1 §9 is
described as needing paginated or scored results rather than a plain,
already-filtered list. Introducing `MemoryQueryResult` now would be
adding structure to match `PlanDecisionResult`/`AgentStepDecision`'s own
sealed-wrapper shape by analogy, but those wrap a *multi-variant
behavioural outcome* (selected-versus-rejected, propose-versus-complete);
a query result is not a decision with variants, it is a plain read
result. If pagination or per-result ranking metadata becomes a concrete
requirement later, adding `MemoryQueryResult` then is a clean, additive
change — nothing about keeping `List<MemoryRecord>` today forecloses it.

## 8. `MemoryRetrievalPolicy` (deferred)

- **Purpose.** The seam by which relevance and ordering are computed when
  `MemoryStore.retrieve` is called — what Unit A1 §4's "ranking strategy
  is implementation-defined" cashes out to.
- **Responsibilities.** Given a `MemoryQuery` and the set of `MemoryRecord`s
  it structurally matches, determine which are most relevant and in what
  order — without this document specifying how.
- **Ownership.** Would be injected into `MemoryStore`, exactly as
  `MemoryPromotionPolicy` is, if and when it is implemented.
- **Relationships.** Would consume `MemoryQuery` and candidate
  `MemoryRecord`s; would produce the ordered `List<MemoryRecord>`
  `retrieve` returns.

This document includes `MemoryRetrievalPolicy` in the approved contract
set, but recommends it be **deferred, not required for a first Memory
Runtime implementation.** The justification for including it at all is
concrete, not symmetry with `MemoryPromotionPolicy`: Unit A1 already
committed, in its own accepted text, to "ranking strategy is
implementation-defined" — meaning something pluggable is already
promised. Naming that seam now, without shaping its algorithm, is what
keeps that promise honest: it prevents a future implementer from simply
hard-coding a ranking rule directly inside `MemoryStore`, which would
quietly foreclose the "implementation-defined" flexibility Unit A1
already told a future reader to expect.

It is deferred because a first, minimal `MemoryStore` implementation can
retrieve by a simple, fixed strategy (for instance, exact category or
keyword matching, most-recent-first) with no pluggable seam at all, and
nothing in Unit A1 or in any caller's description requires more than that
on day one. Naming the seam now and implementing it later, when a real
need for pluggable ranking materialises, costs nothing; implementing it
now, with no concrete ranking requirement to satisfy, would be
speculative structure.

- **Required public members, if implemented.** One operation: given a
  `MemoryQuery` and a candidate set of `MemoryRecord`s, return an ordered
  subset. No algorithm is specified.
- **Future extensibility.** Exists precisely to be replaced without
  changing `MemoryQuery`'s or `MemoryStore.retrieve`'s own shape.

## 9. `MemoryStore` (Memory's one public interface)

- **Purpose.** The single public contract through which every other
  Runtime Foundation component reaches Memory — already named,
  operation-by-operation, in `src/interfaces/MemoryStore.kt`.
- **Responsibilities.** Accept a `CandidateMemory` and assign it a
  `MemoryId`; evaluate and, where the evaluation decides in its favour,
  promote a submission into a `MemoryRecord`; retrieve matching
  `MemoryRecord`s for a `MemoryQuery`; forget a `MemoryRecord` (auditably).
- **Ownership.** The Memory subsystem's own boundary. One implementation
  is expected eventually (this document does not name it beyond noting
  the established `InMemory*` convention), consulting an injected
  `MemoryPromotionPolicy` internally, exactly as `InMemoryPlannerRuntime`
  consults an injected `PlanDecision`.
- **Relationships.** Consumes `CandidateMemory` and `MemoryQuery`;
  produces `MemoryId`, `MemoryRecord`, and (via its internal collaborator)
  `MemoryPromotionDecision`; consulted, read-only, by Identity, Trust, the
  Planner, the Agent Runtime, the Task Manager (Unit A1 §9), and never by
  the Execution Pipeline directly.

**Required public members.** Operations covering submission, retrieval,
and forgetting, matching the existing stub's naming at the level of
intent — but see the architectural decision below for why "promote" is
not, and must not become, an operation an external caller invokes.

### Should external callers invoke `promote` directly? A clear architectural decision

The previous revision of this document flagged that the existing
`MemoryStore.promote(memoryId: MemoryId): Memory` shape "may be
problematic" and left the question open. It is not left open any
longer.

**Decision: no. External callers never invoke promotion. Memory owns
evaluation and promotion internally, end to end, and does not expose
either as a separate operation a caller orchestrates.**

The reasoning: a caller that submits a `CandidateMemory` and then
separately calls `promote(memoryId)` itself would be the caller
deciding *when*, and implicitly *whether*, a submission becomes durable
knowledge — which is precisely the authority Unit A1's Architectural
Principle withholds from everyone but Memory ("Memory stores knowledge.
Memory never decides... Every decision involving Memory belongs to
another subsystem" run in reverse: no other subsystem may make Memory's
own decision for it, either). A two-step, externally-orchestrated
submit-then-promote sequence would also create a window — however
narrow — in which a `CandidateMemory` sits promotable-but-not-yet-
evaluated at the caller's discretion, which is exactly the kind of
externally-triggered mutation the new Constitutional Boundary above
rules out. Promotion must instead be something that happens *to* a
submission, decided by `MemoryPromotionPolicy` and carried out by
`MemoryStore`, both entirely inside Memory's own boundary, with the
caller only ever seeing the outcome.

Concretely, this means:

- **The submission operation** (`addCandidate` in the existing stub) may
  remain named that way, or may be renamed in a future field-level
  Kotlin contract to better reflect "remember this" semantics (for
  instance, something closer to a plain `submit` or `remember`) — this
  document does not mandate a specific name, only that whatever it is
  called, it remains the single entry point by which a `CandidateMemory`
  enters Memory's custody.
- **Promotion is not an external caller operation.** No public
  `MemoryStore` member exists for a caller to invoke that says "promote
  this specific, already-submitted candidate now." Promotion is not
  something Memory exposes to be called; it is something Memory does.
- **`MemoryPromotionPolicy` is invoked internally by the Memory
  implementation**, as part of handling a submission — never surfaced as
  a separate step a caller drives, and never called directly by anything
  outside Memory's own implementation (restated from §6 above).
- **The public `MemoryStore` contract must expose the result of
  submission — including whatever Evaluation and Promotion decided — in
  a single outcome shape capable of expressing either a promoted or a
  rejected result.** A caller submits once and learns the outcome once;
  it does not submit, then separately poll or call again to find out
  whether promotion happened.
- **A future implementation unit must not preserve a public
  `promote(memoryId): MemoryRecord`-shaped operation merely because the
  existing stub happened to name one.** The existing stub's naming was a
  first, operation-level approximation, not a binding field-level
  signature (per AD-013) — this document's architectural decision
  supersedes it. The field-level Kotlin contract that follows this
  document must reflect submit-and-learn-the-outcome, not
  submit-then-separately-promote, regardless of what the four
  originally-named operations were called.

This document does not write the resulting Kotlin signature — that
remains a field-level contract-design or implementation task — and does
not invent a method name beyond what is necessary to state this rule
(no new type or member name is introduced by this decision; it governs
how existing and future members must behave, not what they are called).

**Required lifecycle.** No internal state machine of its own beyond
whatever a given `MemoryRecord` moves through (submitted, evaluated,
promoted or rejected, optionally consolidated or retention-reviewed,
optionally forgotten) — `MemoryStore` is the boundary these transitions
happen behind, not a session or run with its own lifecycle states the
way a Planning Session or Agent Run has.

**Future extensibility.** A retention/consolidation seam (below) and
`MemoryRetrievalPolicy` (above) can each be added as additional injected
collaborators without changing `MemoryStore`'s own public surface —
submit-and-learn-the-outcome, retrieve, forget — per the architectural
decision above that promotion is not a separate, caller-facing
operation.

### Why not a separate `MemoryRuntime` interface?

**Excluded — merged into `MemoryStore`.** This is the central minimalism
decision in this document, so it is stated plainly: no subsystem in this
codebase has two public interfaces where one already suffices.
`ToolRegistry` is Tool Registry's one interface, implemented directly by
`InMemoryToolRegistry`. `IdentityService` is Identity's one interface,
implemented directly by `InMemoryIdentityService`. `TaskProposalIntake`
is the Task Manager's one caller-facing interface;
`AgentRunCommandChannel` is the Agent Runtime's. `PlannerRuntime` was
added in Unit D1A precisely *because* `InMemoryPlannerRuntime` had **no**
pre-existing interface at all — that gap was the defect being corrected,
not a precedent for adding a second interface where one already exists.

Memory is not in that situation. `MemoryStore` already exists, already
names every operation Memory needs to expose, and already carries no
responsibility a second wrapper interface could meaningfully add.
Introducing `MemoryRuntime` as a further public interface — whether
identical to `MemoryStore` or a thin pass-through wrapping it — would
duplicate an already-adequate contract for the sake of matching
`PlannerRuntime`/`AgentRunCommandChannel`'s naming pattern, which is
exactly "creating a type merely for symmetry."

**Determination:** `MemoryRuntime` should not exist as a separate public
interface. `MemoryStore` is, and remains, Memory's one public contract.
The eventual concrete class — whatever it is named, following this
codebase's established `InMemory*` convention (`InMemoryMemoryStore` or
`InMemoryMemoryRuntime` are both consistent with existing naming; this
document does not decide between them, since that is a naming choice for
the implementation unit, not an architectural one) — implements
`MemoryStore` directly, exactly as `InMemoryToolRegistry : ToolRegistry`
and `InMemoryIdentityService : IdentityService` already do. "The Memory
Runtime" remains the correct informal name for the subsystem as a whole
(parallel to "the Planner Runtime," "the Agent Runtime"), but that name
does not require, and should not force, a second formal interface
distinct from `MemoryStore`.

## 10. Why not `MemoryObservation`?

**Excluded.** Unit A1 §5 is explicit that "Observation is not Memory's
own act; Memory receives what is observed elsewhere" — Memory's contract
surface begins at `CandidateMemory`, the point at which Memory takes
ownership, not at Observation, which belongs to whichever subsystem
first encounters the information. A formal `MemoryObservation` public
contract would either duplicate `CandidateMemory`'s own "what it must
carry" definition with no distinct responsibility of its own, or it
would hand Memory a say over how other subsystems represent their own
pre-submission observations — which is exactly the ownership boundary
Unit A1 was careful to draw and this document must not blur. Whatever
form an "observation" takes inside the Planner, the Agent Runtime, or a
plugin, before it is worth submitting, is that subsystem's own concern,
not Memory's.

## 11. Retention and Consolidation (deferred, kept combined)

Unit A1 §7 already named these together as a single "retention/
consolidation contract," and this document does not split that into two
separate public interfaces (`MemoryConsolidationPolicy` and
`MemoryRetentionPolicy`), even though both were posed as candidate names.

Consolidation (combining or deduplicating related `MemoryRecord`s) and
Retention (deciding how long a record remains eligible before
reconsideration) do answer different questions, and a future unit may
find good reason to give them independent, separately-pluggable seams —
that determination is not foreclosed here. But nothing in Unit A1 or in
this document's own review identifies a concrete need for them to vary
*independently* of one another today: no caller, no test scenario, and no
constitutional boundary requires a Consolidation strategy that can be
swapped without also swapping Retention, or vice versa. Naming two
separate public interfaces now, before that need is concrete, would be
manufacturing a distinction not yet earned — precisely the kind of
premature structure this Sprint's minimalism review exists to catch.

**Determination:** one combined, deferred governance seam, not required
for a first implementation, covering both Consolidation and Retention as
Unit A1's own Ownership table already assigns them (both to Memory
itself). This document does not mint a new Kotlin-facing name for it —
it remains, for now, what Unit A1 already called it. Splitting it into
two named interfaces remains available as a future, additive refinement,
the moment a concrete need to vary them independently is identified.

---

## Constitutional Checks

Every contract above, checked against Unit A1's six-point Constitutional
Boundary restatement:

| Contract | Stores knowledge | Never plans | Never authorises | Never executes | Never reacts autonomously | Provides knowledge only |
| --- | --- | --- | --- | --- | --- | --- |
| `MemoryId` | N/A (identity only) | ✓ | ✓ | ✓ | ✓ | ✓ |
| `CandidateMemory` | ✓ (proposed) | ✓ no plan/candidate field | ✓ no permission field | ✓ no tool reference | ✓ submitted only by explicit caller | ✓ |
| `MemoryRecord` | ✓ | ✓ | ✓ carries sensitivity data, not a decision | ✓ | ✓ | ✓ |
| `MemoryCategory` | N/A (classification only) | ✓ | ✓ | ✓ | ✓ | ✓ |
| `MemoryPromotionDecision` | ✓ (outcome only) | ✓ decides retention, not action | ✓ | ✓ | ✓ produced only when `MemoryPromotionPolicy` is called | ✓ |
| `MemoryPromotionPolicy` | ✓ | ✓ evaluates retention-worthiness only | ✓ | ✓ | ✓ consulted, never subscribed to events | ✓ |
| `MemoryQuery` | N/A (request only) | ✓ | ✓ | ✓ | ✓ | ✓ |
| `MemoryStore` | ✓ | ✓ no planning operation | ✓ no authorisation operation | ✓ no execution operation | ✓ every operation is caller-invoked | ✓ every operation is read or accept, never direct |

No contract in this document fails any column.

---

## Engineering Review

**Architectural consistency.** Every contract traces to a specific Unit
A1 section (cited throughout above) and to `MemoryStore.kt`/`MemoryStore.md`'s
already-approved operation names. Nothing here introduces a concept Unit
A1 did not already anticipate at the concept level.

**Model independence.** `MemoryPromotionPolicy` and `MemoryRetrievalPolicy`
are both declared as suspending, replaceable seams for the same reason
`PlanDecision` and `AgentStepSource` are (AD-010) — a future model-backed
implementation of either must not require a breaking interface change.

**Future compatibility.** Every field group identified as required is
additive-friendly: `CandidateMemory` and `MemoryRecord` can each gain
optional fields without breaking existing callers; `MemoryCategory` can
gain a sixth value; a `MemoryPolicy` bounded-configuration record (§6) and
`MemoryQueryResult` (§7) can each be added later without changing
anything designed here today.

**Contract minimalism.** Performed throughout, not as an afterthought:
four candidate names are excluded outright (`CandidateMemoryId`,
`MemoryQueryResult`, `MemoryRuntime`, `MemoryObservation`), two are
included but explicitly deferred (`MemoryRetrievalPolicy`, the combined
retention/consolidation seam), and `MemoryCategory` is deliberately
shaped as one enum rather than a type hierarchy. Each exclusion is
justified by a concrete, stated reason, not merely asserted.

**Traceability.** Every required contract's authorising document is
named in its own section above (`MEMORY_RUNTIME_ARCHITECTURE.md` for the
concept, `MemoryStore.kt`/`MemoryStore.md` for the four pre-existing
operation names, `33-memory-consolidation.md` for the promotion factors).

**Symmetry with Agent Runtime.** Corrected, not merely asserted: this
document identifies that `MemoryPromotionPolicy`'s true sibling is
`AgentStepSource`, not `AgentPolicy` (§6) — a precision Unit A1's own
text did not draw. `AgentPolicy`'s own bounded-configuration role has no
Memory equivalent proposed here, for the same reason Track D declined a
`PlannerPolicy`: no configurable bound has a concrete need yet.

**Symmetry with Planner Runtime.** `MemoryPromotionDecision` deliberately
does *not* mirror `PlanDecisionResult`'s batch/no-viable-candidate shape,
and `MemoryPromotionDecision.Reject.reason` deliberately does *not*
mirror `PlanRejection`'s closed-enum shape — both differences are
justified in §5 above by a real structural difference (individual
evaluation versus batch competition; multi-factor judgment versus
exact structural rules), not by a failure to notice the Planner Runtime's
pattern.

### What this review recommends removing, or adding, before implementation begins

- **Remove (already excluded above):** `CandidateMemoryId`,
  `MemoryQueryResult`, `MemoryRuntime`, `MemoryObservation`. None should
  be implemented.
- **A decision now recorded, not left as an open gap:** external callers
  do not promote memories; Memory owns promotion. The previous revision
  of this document only observed that `MemoryStore.promote(memoryId: MemoryId): Memory`
  "may be problematic," since its signature returns a record
  unconditionally with no way to express a `MemoryPromotionPolicy`
  rejection. §9 above now resolves this as an architectural decision, not
  an open question: promotion is never a caller-invoked operation;
  submission and its outcome are one caller-facing step, evaluated and
  decided entirely inside Memory's own boundary; and the future
  field-level Kotlin contract **must** reflect that — it must not
  preserve a public `promote(memoryId): MemoryRecord`-shaped operation
  merely because the existing stub happened to name one. This is no
  longer a finding for a future unit to resolve; it is a constraint that
  future unit must build against, exactly as binding as any other
  disposition recorded in this document.

---

Track A is ready for Memory Runtime implementation.
