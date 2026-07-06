# Sprint 3 Runtime Foundation

## Status

Type: Historical engineering review.
Scope: Sprint 3 in full (Track C, Track D).
Not an architecture document. Not a specification. This document creates
no obligation and authorises no future work; it records what Sprint 3
achieved, the engineering lessons discovered while achieving it, and the
state of the platform at Sprint 3's completion.

Date reviewed:
2026-07-06

## Sprint 3 Runtime Foundation

Sprint 3 established Parker's first complete Runtime Foundation: a single,
coherent execution substrate spanning identity, permission, multi-step
agent execution, and planning, built from components that had previously
existed only individually or partially.

This milestone is significant because it is the first point at which
these components operate together, in a demonstrated end-to-end path, as
one Runtime rather than as separate pieces awaiting integration. Before
Sprint 3, Parker had an Identity Service, a Permission Engine, a
single-step Agent Runtime, and a Task Manager Runtime that accepted
proposals — each independently real, but with no multi-step orchestration
and no Planner Runtime to drive proposals into that Task Manager on a
deterministic basis. Sprint 3 closes both of those gaps. The result is not
a new capability layered on top of the existing Runtime; it is the
Runtime becoming load-bearing for the first time — a foundation
subsequent cognitive work can be built on rather than around.

## What Sprint 3 Delivered

### Track C — Multi-Step Agent Runtime

Track C extended the Agent Runtime from a single-step execution model to a
sequential, multi-step one. It delivered: sequential step orchestration
across a bounded run; suspend, resume, and cancel as first-class run
states, not just start-and-finish; an explicit Agent Runtime event
progression describing a run's lifecycle from start to completion,
suspension, or cancellation; and a deterministic execution model, in which
a given sequence of steps and decisions produces the same outcome every
time, with no hidden state or randomness governing what happens next.

### Track D — Planner Runtime

Track D introduced the Planner Runtime: the first capability able to
evaluate more than one candidate plan and produce a single, deterministic
outcome from it. It delivered: a Planner Runtime interface through which
a Planning Session is run; a Plan Decision mechanism that evaluates Plan
Candidates and selects a winner (or determines none is viable); a Plan
Candidate model describing what a candidate plan is; an explicit Planning
Session lifecycle; deterministic plan selection, with no scoring, ranking,
or randomness involved in choosing a winner; and progression from a
selected Plan Candidate into a Task Proposal, handed to the existing Task
Manager Runtime exactly as any other proposal would be.

Track D also included a Contract Design unit (Unit D1A) and a subsequent
alignment pass, both described further under Engineering Lessons below —
these were part of how Track D was delivered correctly, not separate
deliverables in their own right.

This section summarises capability, not implementation. It does not
enumerate the Kotlin types, files, or test counts involved; those are
recorded in `docs/implementation/IMPLEMENTATION_HISTORY.md` and the
per-unit post-implementation reviews under `docs/reviews/`.

## Runtime Capabilities at Sprint 3 Completion

Described in terms of capability rather than class, the Runtime now
supports:

- Identity-aware execution — every run and every planning session is tied
  to a resolvable Principal.
- Permission-gated execution — execution remains subject to the
  Permission Engine's authorisation, unchanged by anything Sprint 3 added.
- Multi-step agent runs — a run may consist of a sequence of steps, not
  only one.
- Planner-driven task progression — a Planning Session can produce a Task
  Proposal without a caller hand-assembling one.
- Suspend / Resume / Cancel — an agent run has real, observable states
  beyond "running" and "finished."
- Sequential orchestration — steps execute in a defined order, one after
  another, not concurrently or out of sequence.
- Event-driven coordination — Runtime state changes are visible as
  events, not only as return values.
- Deterministic planning — a Planning Session's outcome is fully
  determined by its inputs.
- Deterministic execution — an agent run's outcome is fully determined by
  its steps and decisions.

Taken together, these capabilities mean the Runtime now provides the
execution substrate upon which future cognition will operate: a place
where a plan can be decided, a task can be proposed, and a multi-step run
can be carried out, under identity and permission, in a way whose outcome
can be explained and reproduced.

## Engineering Lessons

Sprint 3 surfaced several engineering lessons, each an improvement to how
Parker's engineering process works rather than a mistake to be corrected:

- **Separating behavioural architecture from implementation.** Track C's
  Unit C1 (design) and Unit C2 (implementation) demonstrated that
  specifying behaviour first, in full, before writing Kotlin, produces an
  implementation with no open design questions left to resolve mid-code.
  Track D's own experience (below) demonstrated what happens when that
  separation is incomplete, which sharpened rather than weakened the
  case for it.
- **Discovery that Planner Runtime required an additional Contract Design
  phase (Unit D1A).** Track D's initial design unit (Unit D1) correctly
  scoped itself to naming concepts — "a Plan Candidate type," "a concrete
  Plan Decision mechanism" — without specifying their fields, since the
  authoritative Sprint 2 implementation plan called for exactly that
  narrower scope at the time. When implementation then needed field-level
  shapes those concepts did not yet have, Parker's engineering process
  responded by inserting a dedicated design pass (Unit D1A) before
  accepting any of the resulting code, rather than by accepting
  implementation-shaped contracts after the fact. This is the process
  working as intended: a gap was found before it was allowed to become
  permanent.
- **Public contracts should never be invented during implementation.** A
  traceability review of Track D's initial implementation attempt found
  that nearly every public type it introduced had been shaped during
  coding rather than pre-specified. None of those types were accepted
  until Unit D1A had reviewed and, where necessary, revised them. This
  confirms a standing principle rather than establishing a new one: the
  order is architecture, then specification, then implementation, and
  Sprint 3 is the first time that order was tested against real pressure
  to skip a step.
- **Architectural traceability is part of engineering quality.** Track D's
  final alignment pass concluded with an explicit review tracing every
  public Planner Runtime contract back to the specific architecture
  document and section that authorises it. Being able to produce that
  trace, on demand, is now treated as part of what "done" means for a
  Runtime unit, alongside passing tests and complete documentation.
- **Implementation may pause when architecture is incomplete.** When
  Track D's implementation attempt revealed that its authorising
  architecture was not yet specific enough to build against safely, work
  paused, the attempt was retained as exploratory evidence rather than
  discarded, and a design unit was run before implementation resumed.
  Sprint 3 demonstrates that pausing in this situation is a normal,
  expected outcome of the process, not a failure of it.

## Architectural Integrity

Throughout Sprint 3, Parker's constitutional principles were maintained
without exception: Parker owns authority; modules provide capability;
cognition proposes; trust authorises; runtime executes; the owner remains
in control. Neither Track C nor Track D introduced any path by which the
Agent Runtime or the Planner Runtime creates a task directly, grants
itself permission, or executes outside the existing Permission Engine and
Execution Pipeline.

Sprint 3 strengthened these principles by ensuring public contracts were
architecturally approved before implementation — most visibly in Track
D, where implementation-shaped contracts were identified, held back from
acceptance, and replaced with contracts an accepted architecture document
authorised, before that code was allowed to stand as the Runtime's public
surface.

## Current Runtime Position

At the completion of Sprint 3, Parker possesses Identity, Trust,
Execution, a Multi-Step Runtime, and a Planner Runtime, working together
as one coherent Runtime Foundation. Each of these existed, in some form,
before Sprint 3; what Sprint 3 established is that they now operate as a
single execution substrate rather than as separate components each
awaiting the others.

## What Remains Before Full Cognition

Future work will build upon this Runtime Foundation through capabilities
such as Memory, World Model, Workflow Engine, Reasoning Providers,
Knowledge integration, and higher-level planning. This section is
deliberately not a roadmap: it names the categories of future capability
the Runtime Foundation anticipates without committing to their order,
scope, or timing.

Each of these future capabilities will operate on top of the Runtime
Foundation established in Sprint 3, rather than replacing any part of it.
Identity, Trust, Execution, the Multi-Step Runtime, and the Planner
Runtime are the substrate; what comes next is cognition operating within
it.

## Conclusion

Sprint 3 represents the transition of Parker from a collection of runtime
components into an integrated execution platform. Identity, Trust,
Execution, multi-step agent runs, and planner-driven task progression now
operate as one Runtime Foundation rather than as separate, individually
functioning pieces.

With this foundation in place, future development can focus on expanding
cognitive capability rather than continuing to build fundamental runtime
infrastructure.
