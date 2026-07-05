# Parker Engineering Standard

## Engineering Standard

### Identifier: PES-001

### Status: Normative

A normative document defines mandatory engineering practice. Deviations
require explicit approval through the Architecture Decision process.

### Version: 2.0

By this version, this document no longer describes a workflow alone. It
defines governance, authority, evidence, review policy, risk
classification, source-of-truth hierarchy, and completion criteria, in
addition to the workflow itself. Engineering Workflow is therefore one
chapter of this Standard, not the whole of it.

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
- Architecture Decision
- Specification
- Full engineering workflow

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

# Final Principle

> Parker is engineered, not merely programmed.

Every implementation unit should leave the platform in a better state
than it was found, not only through additional capability, but through
improved understanding, traceability and maintainability.

The engineering process is therefore considered part of Parker's
architecture.

It exists to preserve trust, evidence, and long-term maintainability as
the platform evolves.
