# Parker Engineering Standard

## Engineering Standard

### Identifier: PES-001

### Status: Normative

A normative document defines mandatory engineering practice. Deviations
require explicit approval through the Architecture Decision process.

### Version: 2.1

By this version, this document no longer describes a workflow alone. It
defines governance, authority, evidence, review policy, risk
classification, source-of-truth hierarchy, and completion criteria, in
addition to the workflow itself. Engineering Workflow is therefore one
chapter of this Standard, not the whole of it.

**Version 2.1 amendment.** Added following
`docs/reviews/SPRINT_4_ENGINEERING_CONSOLIDATION.md`, a read-only
cross-subsystem review conducted after Agent Runtime, Planner Runtime,
Memory Runtime, and World Model Runtime had each independently
completed the full engineering workflow at least once. That review
found no constitutional or architectural violation in any of the four
subsystems, but found four practices that had already converged
independently, in practice, without being stated here as a
requirement: a Contract Design stage before new public runtime
contracts are implemented (Stage 2A, below), a mandatory
Self-Traceability Review for Level 2/3 units (Stage 9, below), an
in-memory concurrency discipline (Chapter 7), and guidance on the shape
of a policy seam (Chapter 7). This amendment states each as a rule
rather than leaving it as a norm a future unit would otherwise have to
re-discover. It also considered, and explicitly declined, a fifth
candidate rule -- standardising public interface naming across
subsystems -- see `docs/reviews/SPRINT_4_ENGINEERING_CONSOLIDATION.md`
for why.

This document's identifier is **PES-001**. Future Architecture Decisions
that modify this Standard should cite it by identifier and section, for
example: "This decision modifies PES-001 Section 4.3."

---

# Purpose

This document defines the mandatory engineering workflow used when
developing Parker.

Its purpose is not merely to produce working software.

Its purpose is to ensure that every implementation unit:

- preserves the approved architecture;
- remains traceable;
- is independently verifiable;
- records implementation rationale;
- explicitly records technical debt;
- leaves the repository in a consistent and reviewable state.

Parker is intentionally engineered architecture-first.

Implementation follows documented decisions rather than creating them.

---

# Scope

This standard governs the engineering process used to develop Parker
itself.

It does not define runtime behaviour, architectural policy, or
user-facing functionality except where necessary to govern engineering
practice.

---

# Core Engineering Principle

> Architecture decides.
>
> Implementation follows.
>
> Verification proves.
>
> Documentation records.

Code is never the place where architectural decisions are first made.

If implementation exposes behavioural ambiguity, implementation pauses
until the ambiguity has been resolved through documented engineering
decisions.

Architectural clarification during implementation is evidence of
engineering discipline, not evidence of planning failure.

---

# Engineering Invariants

Every implementation unit shall:

- preserve approved architecture;
- preserve runtime boundaries;
- preserve trust boundaries;
- preserve ownership boundaries;
- remain independently testable.

These invariants are already implied throughout this Standard. They are
stated explicitly here so that no implementation unit, review, or
retrospective needs to re-derive them.

---

# Chapter 1 — Engineering Workflow

## Standard Workflow

```text
                        Architecture
                             │
                             ▼
                  Architecture Review
                             │
                             ▼
                   Contract Design
              (required for new public
                runtime contracts)
                             │
                             ▼
                  Implementation Plan
                             │
                             ▼
             Implementation Decisions
                             │
                             ▼
                       Scope Lock
                             │
                             ▼
                     Implementation
                             │
                 ┌───────────┴───────────┐
                 │                       │
                 ▼                       │
          Ambiguity Discovered?          │
                 │                       │
               Yes                       │
                 │                       │
                 ▼                       │
        Implementation Decisions Updated │
                 │                       │
                 └───────────────────────┘
                             │
                             ▼
             Android Studio Verification
                             │
                             ▼
            Git Commit (Implementation)
                             │
                             ▼
                 Engineering Validation
                     ┌──────────────┐
                     ▼              ▼
        Engineering Checkpoint   Post-Implementation Review
                     │              │
                     └──────┬───────┘
                            ▼
             Documentation Follow-up (if required)
                            │
                            ▼
             Git Commit (Documentation)
                            │
                            ▼
                        Git Push
                            │
                            ▼
        ───────────────────────────────────────
        Feedback into the next Implementation Plan
        (sequencing, assumptions and process improvements)
```

## Stage 1 – Architecture

**Purpose**

Define behaviour before implementation begins.

Architecture establishes:

- responsibilities;
- ownership;
- trust boundaries;
- runtime boundaries;
- lifecycle;
- invariants;
- security model.

Architecture intentionally avoids implementation details.

**Why this exists**

Architecture prevents coding by assumption.

## Stage 2 – Architecture Review

**Purpose**

Determine whether the architecture is sufficiently complete for
implementation.

Questions include:

- Are responsibilities explicit?
- Are ownership rules complete?
- Are runtime boundaries defined?
- Are invariants documented?
- Are unresolved questions identified?

**Why this exists**

Implementation should not discover missing architecture.

## Stage 2A – Contract Design

**Purpose**

Define the field-level public contracts a new runtime subsystem
exposes, before an Implementation Plan is written against them.

**Required when**

A new runtime subsystem introduces public types it does not already
have approved, field-level Kotlin shapes for -- in practice, this means
every new runtime subsystem's first implementation unit, and any later
unit that adds a genuinely new public contract to an existing
subsystem.

A Contract Design document:

- reviews the existing stub or prior art without assuming it is
  correct;
- determines the minimum required set of public contracts, explicitly
  stating what is required, what is excluded, and what is deferred, and
  why;
- resolves named outstanding design questions against approved
  architecture, never inventing new architecture to answer them;
- states whether a separate "Runtime" wrapper interface is needed, or
  whether one interface already suffices;
- ends with a Self-Traceability Review (see Stage 9) reviewing its own
  proposed contracts before implementation begins, not only after.

**Why this exists**

`docs/architecture/MEMORY_CONTRACT_DESIGN.md` and
`docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md` were each written
this way independently, and each produced an implementation stage that
required no unauthorised-contract correction afterward.
`docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` was added one
unit later than the rest of the Planner Runtime track, specifically to
correct an exploratory implementation that had drifted from approved
architecture in the absence of one. Three independent subsystems
converging on the same missing stage, and the one subsystem that
skipped it needing a correction pass to recover, is sufficient evidence
to require it going forward rather than leave it optional.

## Stage 3 – Implementation Plan

**Purpose**

Break architectural work into independently verifiable implementation
units.

Each unit defines:

- Included work
- Excluded work
- Dependencies
- Acceptance Criteria
- Unit Stop Conditions

**Why this exists**

Small implementation units reduce architectural drift.

## Stage 4 – Implementation Decisions

**Purpose**

Resolve behavioural ambiguity before implementation.

Examples include:

- lifecycle transitions;
- ordering;
- event sequencing;
- conservative defaults;
- runtime behaviour.

Implementation Decisions clarify approved architecture.

They do not replace or redefine it.

**Why this exists**

Code should implement decisions.

It should not invent them.

## Stage 5 – Scope Lock

Scope Lock is **not** a document.

It is the formal engineering gate between planning and implementation.

Before implementation begins, the implementation team confirms that:

- the approved Implementation Plan is complete;
- all required Implementation Decisions have been recorded;
- Included and Excluded scope is frozen;
- Acceptance Criteria are agreed;
- Unit Stop Conditions are accepted.

If implementation later discovers ambiguity, implementation pauses and
returns to Implementation Decisions before coding continues.

**Why this exists**

Scope Lock confirms that planning is complete before Kotlin is written.

## Stage 6 – Implementation

**Purpose**

Implement exactly one approved implementation unit.

Implementation remains inside the approved scope.

Unrelated improvements are intentionally deferred.

**Why this exists**

Scope discipline produces predictable engineering.

## Stage 7 – Android Studio Verification

**Purpose**

Android Studio is Parker's authoritative verification environment.

Verification includes:

- successful compilation;
- complete test execution;
- regression detection.

Shell builds are informative.

Android Studio verification is the authoritative verification environment
for implementation acceptance.

**Why this exists**

Passing Android Studio verification is Parker's definition of
implementation correctness.

## Stage 8 – Git Commit (Implementation)

**Purpose**

Capture one coherent implementation milestone.

Normally occurs immediately after successful Android Studio verification.

**Why this exists**

A commit records verified implementation, not work in progress.

## Stage 9 – Engineering Validation

Engineering Validation confirms implementation before publication.

It may consist of one or both of:

- Engineering Checkpoint
- Post-Implementation Review

The required validation depends on the implementation's risk level (see
Chapter 4 — Risk-Based Engineering Levels).

### Engineering Checkpoint

**Purpose**

Determine whether implementation validated the project.

Typical questions:

- Were architectural assumptions confirmed?
- Is sprint sequencing still correct?
- Were new implementation gaps discovered?
- Should future implementation plans change?

Engineering Checkpoints evaluate project direction.

They do not review code quality.

### Post-Implementation Review

**Purpose**

Evaluate the completed implementation.

Typical questions:

- Is the implementation correct?
- Are runtime boundaries preserved?
- Are tests sufficient?
- Is documentation complete?
- Does implementation comply with approved specifications?

**Required component: Self-Traceability Review.** For every Level 2 or
Level 3 implementation unit, the Post-Implementation Review must
include, or reference, a Self-Traceability Review: for every public
type or interface the unit introduced or modified, state where it is
authorised in the governing Contract Design (or Architecture, where no
Contract Design exists), whether it is required, excluded, or deferred
by that document, and whether the implementation matches that
determination. If any public contract is not authorised this way, the
review must say so and stop rather than accept the implementation as
complete.

This does not require a specific document format. It requires that the
question be asked and answered, in writing, before an implementation
unit is treated as complete -- the table format used in
`docs/reviews/SPRINT_4_TRACK_B_UNIT_B3_POST_IMPLEMENTATION_REVIEW.md` is
a reusable example, not the only acceptable one.

Post-Implementation Reviews evaluate implementation quality.

They do not redefine architecture.

## Stage 10 – Documentation Follow-up

**Purpose**

Record any documentation corrections identified during Engineering
Validation.

Typical examples:

- implementation history;
- implementation gaps;
- commit references;
- wording clarification.

Documentation updates may result in a separate documentation-only commit.

**Why this exists**

Engineering review occasionally discovers documentation improvements
after implementation has already been committed.

## Stage 11 – Git Push

**Purpose**

Publish the verified engineering baseline.

The repository should not be pushed while:

- tests fail;
- documentation is inconsistent;
- implementation gaps are inaccurate;
- implementation history is incomplete.

**Why this exists**

The remote repository should always represent a trustworthy engineering
state.

---

# Chapter 2 — Engineering Responsibility Model

Parker deliberately separates authorship from authority.

| Activity | Primary Authority | Typical Author |
|----------|-------------------|-----------------|
| Architecture | Human | Human or AI |
| Architecture Review | Human | Human + AI |
| Implementation Planning | Human | Human + AI |
| Implementation Decisions | Human | Human + AI |
| Kotlin Implementation | Human-approved | AI |
| Unit Tests | Human-approved | AI |
| Static Review | Human-approved | AI |
| Android Studio Verification | Human | Human |
| Git Commit | Human | Human |
| Git Push | Human | Human |
| Final Acceptance | Human | Human |

Authority and authorship are intentionally different.

AI may draft architecture, specifications, implementation plans, Kotlin,
tests, documentation and reviews.

Final architectural authority, Android Studio verification, repository
publication and implementation acceptance remain human responsibilities
unless an Architecture Decision explicitly changes that governance model.

---

# Chapter 3 — Source of Truth

Where conflicts occur, precedence is:

1. Architecture
2. Architecture Decisions
3. Specifications
4. Implementation Plans
5. Implementation Decisions
6. Verified Kotlin Implementation
7. Engineering Reviews
8. Retrospectives

Architecture Decisions define constitutional rules. Specifications
implement those rules. Specifications do not supersede Architecture
Decisions.

Reviews interpret evidence.

They do not replace it.

---

# Chapter 4 — Risk-Based Engineering Levels

## Level 1 – Documentation Maintenance

Examples:

- spelling;
- formatting;
- broken links;
- commit references;
- wording clarification.

Required:

- Sanity Review
- Commit
- Push

A Sanity Review is a brief verification that documentation accurately
reflects repository state.

It is **not** a formal Post-Implementation Review.

## Level 2 – Behavioural Implementation

Examples:

- runtime implementation;
- new behaviour;
- policy changes;
- new tests.

Required:

- Full engineering workflow.

Engineering Validation always includes a Post-Implementation Review.

The Post-Implementation Review at this level must include the
Self-Traceability Review defined in Stage 9.

Engineering Checkpoints are required only where implementation may affect
future sequencing, architecture or implementation planning.

## Level 3 – Architectural Change

Examples:

- new runtime subsystem;
- lifecycle redesign;
- trust model changes;
- major architectural restructuring.

Requires:

- Architecture
- Architecture Review
- Contract Design (Stage 2A), for any new public runtime contract
- Architecture Decision
- Specification
- Full engineering workflow

The Post-Implementation Review at this level must include the
Self-Traceability Review defined in Stage 9.

---

# Chapter 5 — Engineering Evidence

Engineering conclusions shall always be traceable to one or more of:

- approved architecture;
- approved specifications;
- verified implementation;
- Android Studio verification.

Reviews, retrospectives and implementation decisions may interpret
evidence.

They do not replace evidence.

---

# Chapter 6 — Definition of Complete

An implementation unit is complete when, appropriate to its engineering
level:

- implementation is finished;
- Android Studio verification passes;
- implementation gaps are current;
- implementation history is current;
- required Engineering Validation is complete;
- documentation is accurate;
- Git history is current;
- repository is synchronized with GitHub;
- working tree is clean;
- implementation scope has been fully closed or explicitly deferred.

---

# Chapter 7 — Runtime Implementation Discipline

This chapter states two implementation-level rules distilled from
Agent Runtime, Planner Runtime, Memory Runtime, and World Model
Runtime's independent construction, per
`docs/reviews/SPRINT_4_ENGINEERING_CONSOLIDATION.md`. Both apply to any
`InMemory*` or other concrete runtime implementation, present or
future.

## 7.1 In-Memory Concurrency Rule

A mutex, lock, or other exclusion mechanism guarding a runtime
component's own state shall not be held across a `suspend` call to an
injected, replaceable collaborator (a policy seam, an `EventBus`, an
`ExecutionPipeline`, or any other externally-supplied interface),
unless a specific Architecture Decision authorises holding it for a
named reason.

**Reasoning.** Agent Runtime and Planner Runtime were each built to
this discipline from the start: a lock is acquired only for short,
synchronous reads and writes of the component's own map-backed state,
and is released before calling out to an injected collaborator whose
own duration this component does not control. Memory Runtime and World
Model Runtime were each built holding their lock across such a call
instead. Neither approach is incorrect today, because every current
policy implementation (`DefaultMemoryPromotionPolicy`,
`DefaultWorldModelUpdatePolicy`) happens to be fast and synchronous in
practice. The risk is latent, not active: the moment either policy
seam is given a slower implementation -- one that consults an
embedding service, an external store, or any other collaborator with
unbounded duration -- holding the lock across that call would
serialise every other operation on that subsystem for the call's full
duration, platform-wide, for as long as it takes.

**Consequence.** Any future `InMemory*` runtime implementation must
release its own lock before calling an injected collaborator, and
reacquire it only to apply the collaborator's result to its own state
-- mirroring `InMemoryAgentRuntime`'s and `InMemoryPlannerRuntime`'s
existing "read state, unlock, call policy, lock, write state" shape,
not `InMemoryMemoryStore`'s or `InMemoryWorldModel`'s existing "lock,
call policy, write state, unlock" shape. Bringing the latter two into
line with this rule is recorded as deferred, non-urgent engineering
debt (see `docs/reviews/SPRINT_4_ENGINEERING_CONSOLIDATION.md`), not an
immediate defect requiring emergency correction.

## 7.2 Policy Seam Guidance

A policy seam -- the interface by which a runtime component consults an
injected, replaceable collaborator to decide an outcome for one
submitted input (`AgentStepSource`, `PlanDecision`,
`MemoryPromotionPolicy`, `WorldModelUpdatePolicy`) -- shall be:

- **internal**, never invoked directly by an external caller of the
  runtime component it serves;
- **injected**, supplied to the runtime component's constructor,
  defaulted to a concrete `Default*` implementation where one exists,
  never hard-coded;
- **`suspend`-capable**, even where a current implementation never
  actually suspends, so a future implementation that must (an external
  call, a model invocation, a human-in-the-loop wait) does not require
  a breaking interface change;
- **a decision provider, not an authority** -- it decides an outcome
  among or for what it is given; it never grants permission, creates a
  Task, mutates state outside the runtime component that consults it,
  or is itself treated as a downstream authorisation step.

This restates, as a standing rule, the lineage each of the four
existing policy seams already independently cites in its own KDoc. It
does not require identical naming or identical method shape across
seams -- `AgentStepSource.nextStep`, `PlanDecision.decide`,
`MemoryPromotionPolicy.evaluate`, and `WorldModelUpdatePolicy.evaluate`
remain named as they are. A policy seam combining more than one
distinct decision responsibility into a single interface (as
`WorldModelUpdatePolicy` currently combines an acceptance judgment with
an independent staleness judgment) is not prohibited, but should be a
deliberate choice recorded in that unit's Contract Design, not a
default.

---

# Final Principle

> Parker is engineered, not merely programmed.

Every implementation unit should leave the platform in a better state
than it was found, not only through additional capability, but through
improved understanding, traceability and maintainability.

The engineering process is therefore considered part of Parker's
architecture.

It exists to preserve trust, evidence, and long-term maintainability as
the platform evolves.
