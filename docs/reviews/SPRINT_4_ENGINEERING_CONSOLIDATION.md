# Sprint 4 Engineering Consolidation

## Status

Read-only-to-code consolidation. No Kotlin, no tests, and no runtime or
source files were changed to produce this document or the amendments it
accompanies (`docs/architecture/PARKER_ENGINEERING_STANDARD.md` v2.1,
`docs/adr/ADR-023-context-provider-event-publication.md`,
`docs/process/BUILD_SCOPE_EXCLUSION_CHECKLIST.md`). Nothing here has
been committed.

## Why This Consolidation Exists

By the end of Sprint 4, Parker had four independently-built runtime
subsystems -- Agent Runtime, Planner Runtime, Memory Runtime, World
Model Runtime -- each of which had gone through the full engineering
workflow at least once: architecture, a contract or progression design,
implementation, tests, and a Post-Implementation Review. A cross-
subsystem stress review was conducted to ask one question: knowing
everything learned building all four, what would change before the
platform grows another 20,000 lines?

That review found no constitutional or architectural violation in any
of the four subsystems -- the Cognition-proposes/Trust-authorises/
Runtime-executes chain holds everywhere, and every subsystem's own
boundary review held up under direct inspection. What it found instead
was narrower and more specific: several questions had already been
answered, in practice, independently, by more than one subsystem's
authors, without ever being written down as a platform-wide rule. This
consolidation exists to close that gap -- not by inventing new
architecture, but by stating, once, what four independent units had
already each separately worked out for themselves.

## What Was Learned From Agent, Planner, Memory, and World Model

**Agent Runtime and Planner Runtime** were both built holding their
locking discipline to the same rule from the start: acquire a mutex
only for short, synchronous reads and writes of the component's own
state, and release it before calling out to an injected collaborator
(`AgentStepSource`, `PlanDecision`, `ExecutionPipeline`) whose own
duration the component does not control. Planner Runtime additionally
needed a second pass -- `PLANNER_RUNTIME_CONTRACT_DESIGN.md`'s
"Alignment Pass" -- after an earlier exploratory implementation drifted
from architecture in the absence of a dedicated Contract Design stage,
and that correction is itself the clearest evidence for requiring one.

**Memory Runtime and World Model Runtime** were both built one Contract
Design document ahead of their own implementation (`MEMORY_CONTRACT_DESIGN.md`,
`WORLD_MODEL_CONTRACT_DESIGN.md`), and neither needed a correction pass
afterward -- unlike Planner, which needed one precisely because it
lacked this stage the first time. Both also held their locking
discipline to a different rule than Agent/Planner: hold the mutex for
the whole operation, including the call into an injected policy seam.
Both are internally consistent and correct today; the difference from
Agent/Planner was never reconciled against that earlier precedent,
because nothing required it to be.

**World Model Runtime**, being the most recent of the four, also
carried the most rigorous Self-Traceability Review of any of them --
a standalone, per-type table stating, for every public contract
introduced, whether it is required, excluded, or deferred, and why.
Memory's own equivalent review is a single sentence inside a broader
Engineering Review; Planner's and Agent Runtime's design documents
predate the practice entirely. Rigor increased with each subsystem
built, which is a healthy trend, but it had not yet been made a
requirement -- only an example.

## What Became Standard Practice

Four practices are promoted from precedent to requirement by this
consolidation:

1. **Contract Design before implementing new public runtime
   contracts** (PES-001 v2.1, Stage 2A). Evidenced by Memory and World
   Model each needing no correction pass, and Planner needing one
   precisely because it lacked this stage.
2. **A mandatory Self-Traceability Review for every Level 2/3
   implementation unit's Post-Implementation Review** (PES-001 v2.1,
   Stage 9 and Chapter 4). Evidenced by World Model's own practice,
   now required rather than exemplary.
3. **An in-memory concurrency rule**: a lock guarding a runtime
   component's own state must not be held across a `suspend` call to
   an injected, replaceable collaborator, absent a specific
   Architecture Decision authorising it (PES-001 v2.1, Chapter 7.1).
   Evidenced by Agent Runtime's and Planner Runtime's existing
   discipline, generalised as the platform standard going forward.
4. **A reusable build-scope exclusion checklist**
   (`docs/process/BUILD_SCOPE_EXCLUSION_CHECKLIST.md`), evidenced by
   Phase 2, Unit A3, and Unit B3 each independently re-deriving the
   same nine steps correctly, three times in a row.

A fifth practice -- how a context-provider subsystem may publish
observability events without gaining orchestration authority -- is
recorded as a new Architecture Decision Record
(`docs/adr/ADR-023-context-provider-event-publication.md`) rather than
a PES-001 rule, since it governs a platform-wide rule about how
subsystems relate to each other (per `ARCHITECTURE_DECISIONS.md`
Section 7's own criteria for when a new decision belongs there), not an
engineering-process rule. It resolves the architectural shape
`docs/architecture/IMPLEMENTATION_GAPS.md` gap #47 already reasoned out
informally, without closing that gap -- `InMemoryWorldModel` still does
not publish events as of this consolidation.

## What Was Deliberately Not Standardised

**Interface naming.** The original stress review recommended converging
`MemoryStore`, `WorldModel`, `PlannerRuntime`, and `AgentRunCommandChannel`
toward a single naming convention. That recommendation is withdrawn.
`MemoryStore` names a repository. `WorldModel` names a context
provider. `AgentRunCommandChannel` names a command interface. These are
different abstractions performing different roles, and the difference
in their names is tracking something real about each subsystem's own
shape, not an inconsistency to be smoothed away. Forcing identical
naming across them would make the code less expressive, not more
consistent in any way that matters.

The only trace of this observation retained anywhere is non-binding: a
taxonomy exists, informally, in which Parker's runtime interfaces fall
into recognisable shapes -- repository, context provider, command
channel, decision seam -- and a future author may find it useful to
name which shape they are building before choosing an interface name.
This is an observation, not a rule. No document requires a new
subsystem's interface to fit one of these four shapes, and none should,
unless a future subsystem's interface naming genuinely causes
confusion in practice -- the same bar `ARCHITECTURE_DECISIONS.md`
Section 7 already sets for adding any new platform-wide rule.

## What Remains Deferred

- **Bringing `InMemoryMemoryStore` and `InMemoryWorldModel`'s locking
  discipline into line with the new Chapter 7.1 rule.** Both are
  correct today because their current policy implementations are fast
  and synchronous. This is recorded as non-urgent engineering debt, not
  an immediate defect -- a future unit may close it, but nothing about
  today's behaviour requires an emergency fix.
- **Closing `IMPLEMENTATION_GAPS.md` gap #47** (World Model event
  publication). ADR-023 settles the architectural shape a closing unit
  must follow; it does not implement anything, and the gap remains open
  exactly as before.
- **Whether `WorldModelUpdatePolicy`'s bundled `evaluate` +
  `isStillCurrent` responsibilities should ever be split into two
  seams.** PES-001 v2.1's policy seam guidance (Chapter 7.2) explicitly
  permits this bundling as a deliberate choice; splitting it remains a
  future unit's decision, not a requirement.
- **A formal lifecycle enum for `MemoryRecord`/`WorldBelief`.** Neither
  subsystem currently needs one -- both have genuinely fewer states
  than Agent Run, Task, or Planning Session. This remains worth
  revisiting only if Memory's deferred Consolidation/Retention seams,
  or a real "Superseded" concept for World Belief, are ever
  implemented.
- **Whether `memory.*`/`worldmodel.*` should join `EventBus.md`'s
  trust-sensitive domain list.** Explicitly left open in ADR-023's own
  Future Considerations, pending whichever future unit actually
  implements publication for either subsystem.

## Closing Note

Nobody designed the sequence Architecture → Contract Design →
Implementation → Post-Implementation Review → Tests → Git Tag →
cross-subsystem review → PES-001 update in advance. It emerged because
each time a problem surfaced -- Planner's drift without a Contract
Design stage, Memory's and World Model's diverging locking discipline,
three independent rediscoveries of the same build-scope process -- it
was solved once, permanently, rather than patched around locally. This
document is that same pattern applied to itself: a cross-subsystem
review found four things worth keeping and one thing worth explicitly
declining, and both kinds of finding are now written down rather than
left to be rediscovered by a fifth or sixth subsystem.
