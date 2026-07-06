# Architecture Baseline Review — Parker v2.0

## Status

Read-only review. No Kotlin, test, ADR, PES-001, architecture document,
or specification was modified to produce this review. This document is
the only file this review creates.

## Framing

This is not a search for improvements. The governing question is
narrower and more concrete: if this repository were handed to a new
engineering team tomorrow, would they understand Parker well enough to
continue building it correctly? Every conclusion below is answered
against that question, as an external principal architect would answer
it -- not against whether the architecture could theoretically be made
more elegant.

---

## 1. Architectural Cohesion

**The platform now presents one coherent model, with one clear
exception a new team would hit almost immediately.**

The coherent part: every implemented runtime subsystem traces back to
the same constitutional chain (Cognition proposes / Trust authorises /
Runtime executes), the same source-of-truth ordering (Architecture →
Contract Design → Implementation), and the same public-contract
discipline (a sealed, exhaustive outcome type with a common identifying
property; a policy seam that is internal, injected, and
`suspend`-capable). None of this was restated identically by
coincidence -- `MEMORY_CONTRACT_DESIGN.md` and
`WORLD_MODEL_CONTRACT_DESIGN.md` each cite `PLANNER_RUNTIME_CONTRACT_DESIGN.md`'s
and each other's precedent explicitly, and PES-001 v2.1 now states the
pattern as a standing rule rather than an emergent one. A new engineer
reading any one subsystem's Contract Design document would recognise
the shape of every other one immediately.

The exception: **"Agent" names two unrelated things in this codebase,
and nothing reconciles them.** `docs/specifications/volume-03-core-interfaces/Agent.md`
describes a long-running background worker with `start()`/`stop()`/
`health(): AgentHealth` -- Chapter 14's "specialised internal worker."
`AgentRuntimeSpecification.md`'s own Overview asserts that "the Agent
Runtime is the execution environment in which a Parker Agent --
Chapter 14's 'specialised internal worker' -- actually runs," implying
the two are the same concept at different layers. But nothing in
`src/` bears this out: `src/interfaces/Agent.kt` remains an excluded,
unimplemented stub (`AgentHealth` is still undefined, per
`IMPLEMENTATION_GAPS.md` #20), and a repository-wide search confirms it
is referenced nowhere else in `src/` -- `InMemoryAgentRuntime` never
constructs, holds, starts, stops, or health-checks an `Agent`. What was
actually built and heavily tested (`AgentRun`, `AgentRunCommandChannel`,
`InMemoryAgentRuntime`, the multi-step Agent Step loop) is a bounded,
per-Task execution model with no daemon lifecycle, no `health()`
concept, and no code-level relationship to `Agent.kt` at all. A new
engineer who reads Chapter 14 and `Agent.md` first, expecting to find a
background-worker daemon underneath "Agent Runtime," would be looking
for something that was never built, while the thing that was built
answers to the same name.

This is not a constitutional problem -- both concepts, where they
exist, respect the same trust chain -- but it is a real cohesion gap,
and it is the single clearest place a new team's first hour of
exploration would go sideways.

---

## 2. Runtime Cohesion

Execution Pipeline, Agent Runtime, Planner Runtime, Memory Runtime, and
World Model Runtime now feel like parts of one runtime, not five
separate projects, for a specific, checkable reason: every one of them
reaches the Execution Pipeline (or deliberately never reaches it) for
the same underlying reason, not by four different conventions. Agent
Runtime and Planner Runtime both construct `ExecutionRequest`s and
`TaskProposal`s through the identical origin-agnostic path every other
caller uses; Memory and World Model both correctly never touch
`ExecutionPipeline`, `ToolRegistry`, or `ToolInvocationBinding` at all,
which is the *same* rule (only the Execution Pipeline executes),
applied consistently to two different answers (yes, this subsystem
proposes execution; no, this one never does).

Where they still read as four separately-built projects rather than
one runtime: locking discipline (Agent/Planner release their lock
before calling an injected collaborator; Memory/World Model hold theirs
across the call -- `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`
Category B1) and lifecycle formality (Agent Run/Planning Session/Task
each get a compiler-checked enum and transitions object; Memory/World
Belief have neither, correctly, since they have fewer real states).
Both differences are already identified, already reasoned about, and
already correctly left alone rather than forced into artificial
symmetry -- which is itself evidence of cohesion at the *process*
level even where the *code* still shows its independent authorship.

---

## 3. Constitutional Integrity

Re-verified directly against the Kotlin, not assumed from the prior two
reviews' conclusions, since intervening work (Engineering Consolidation,
Volume 3 Specification Alignment) touched only documentation and could
not have changed runtime behaviour:

| Principle | Status |
|---|---|
| Owner authority | Intact. No subsystem grants, escalates, or bypasses authorization. |
| Execution Pipeline authority (AD-003) | Intact. Memory and World Model touch it nowhere; Agent and Planner Runtime route every effect through it exclusively. |
| Trust-first (Cognition proposes / Trust authorises / Runtime executes) | Intact in all four runtime subsystems. |
| Local-first | Not weakened; also not yet meaningfully tested, since nothing implemented so far has had a reason to reach outside the process. |
| Replaceable policy seams | Intact for all four (`AgentStepSource`, `PlanDecision`, `MemoryPromotionPolicy`, `WorldModelUpdatePolicy`); `WorldModelUpdatePolicy`'s bundled two responsibilities is a design constraint, not a replaceability defect -- both methods can still be swapped together. |
| Context providers (AD-012) | Intact. Neither Memory nor World Model has a write path into Task, Agent Run, or Planning Session state, and neither subscribes to anything; ADR-023 now gives event publication a settled shape without either subsystem needing to implement it. |
| Model independence (AD-010) | Intact, and verified directly: no policy seam, no `Default*` implementation, and no runtime class references any model or reasoning-provider type anywhere in its signature. |

**No principle has become weakened.** This is the same conclusion the
prior two review passes reached, now confirmed a third time, against
the current state of the repository, including the Volume 3 alignment
work.

---

## 4. Engineering Process

Architecture → Contract Design → Implementation → Self-Traceability
Review → Post-Implementation Review → Engineering Consolidation is
mature enough to be Parker's standard lifecycle, and the evidence is
no longer purely internal reasoning -- it now includes a live natural
experiment. Memory and World Model each followed this order from their
first implementation unit and needed no correction pass. Planner did
not have Contract Design available on its first attempt and needed a
fourth document (the Alignment Pass) specifically to recover from
drift that stage would have prevented. That comparison, across real
implementation work rather than hypothetical reasoning, is why PES-001
v2.1 made Contract Design and the Self-Traceability Review mandatory
rather than exemplary.

The most recent addition to this lifecycle -- Volume 3 Specification
Alignment, closing the one Category A finding from
`SPRINT_4_ARCHITECTURE_ACTIONS.md` -- is itself a second, independent
exercise of the same discipline the lifecycle already prescribes
(PES-001 Stage 10, Documentation Follow-up), applied correctly: a
narrow, evidenced correction, not a redesign. This is one more data
point in the lifecycle's favour, not a new stage it needed.

One caveat, carried forward honestly rather than smoothed over:
Engineering Consolidation itself has now been exercised three times
(the original PES-001 amendment/ADR-023/checklist, the Architecture
Actions sort, and this baseline review's own predecessor reviews) --
real evidence, but a smaller sample than "survived multiple independent
implementations" represents for the workflow's other stages. It is
mature enough to keep doing, not yet old enough to claim the same
confidence as Post-Implementation Review, which has been exercised
across every sprint since the beginning.

---

## 5. Documentation Quality

Assuming a senior engineer with no prior Parker exposure, working
through the material without asking the original author:

**They could understand the architecture, the constitutional model,
and any single subsystem's contract in isolation.** `parker-constitution.md`,
`ARCHITECTURE_DECISIONS.md`, and any one of the four subsystems'
Runtime Architecture / Contract Design pairs are each self-contained,
well cross-referenced, and (following this Sprint's Volume 3 alignment
work) no longer contradicted by their own Volume 3 interface document.

**They would not find the platform's current state from its own stated
entry points, and this is a genuine gap.** `docs/architecture/00-index.md`
still identifies itself as "Parker Platform Architecture v0.4" and
organises the platform around "Chapters 1-20," with no mention that
Sprint 1 through Sprint 4 -- the Runtime Foundation, Agent Runtime,
Planner Runtime, Memory Runtime, and World Model Runtime this review
exists to evaluate -- were ever built. `docs/architecture/IMPLEMENTATION_ORDER.md`,
which describes itself as "an architecture coordination document,"
states plainly in its own Section 2 that Parker "has no Planner,
Memory, World Model, or Workflow Runtime" -- which was true when it was
written and is no longer true today. `docs/architecture/ARCHITECTURE_HISTORY.md`,
whose stated purpose is to record "why significant architectural
decisions were made," has exactly one milestone entry -- "Architecture
v1.0, Constitutional Foundation" -- and was never extended to record
Sprint 1's Runtime Foundation or any of Sprint 3/4's runtime
subsystems as the milestones they plainly are. A new engineer following
any of these three documents as a starting point would form an
inaccurate picture of what already exists, and would have to discover
the real state of the platform by browsing `docs/reviews/` and
`docs/implementation/IMPLEMENTATION_HISTORY.md` directly, without being
pointed there by the documents whose job is to point them somewhere.

Per this review's own constraint, no rewrite is recommended here --
only the gap itself, named precisely: **the platform's orientation
documents (index, implementation order, architecture history) have not
been updated to reflect that four runtime subsystems now exist**,
while the subsystem-level documentation describing each of those four
in detail is in good shape.

**Extension points are documented, but only by example, not by
guide.** `docs/process/BUILD_SCOPE_EXCLUSION_CHECKLIST.md` and PES-001
v2.1's Stage 2A describe *how* to bring a new subsystem's types into
compilation and *when* a Contract Design is required, but there is no
single document that says, in one place, "here is how to add a fifth
runtime subsystem, start to finish" -- a new team would have to
reconstruct that sequence by reading Memory's or World Model's own
document trail as a worked example. This is a real gap, though a
smaller one than the orientation-document gap above, since the worked
examples themselves are complete and consistent.

---

## 6. Source-of-Truth Hierarchy

Architecture → Contract Design → Specifications → Verified Kotlin →
Tests → Reviews, checked for contradictions across all four
implemented subsystems:

**Internally consistent for Memory and World Model, following this
Sprint's Volume 3 alignment.** `MemoryStore.md` and `WorldModel.md` now
match their respective Contract Design documents and the verified
Kotlin exactly -- confirmed by direct comparison of every operation
signature and named type, not merely by re-reading the alignment
work's own report.

**No equivalent contradiction found for Agent Runtime or Planner
Runtime's Volume 3/4/6 documents.** `AgentRuntimeSpecification.md` and
`PlannerRuntimeSpecification.md` were each written as, and remain,
full behavioural specifications rather than thin Volume 3 interface
summaries, and neither has been superseded by a later Contract Design
in the way `MemoryStore.md`/`WorldModel.md` were -- Planner's own
Contract Design (`PLANNER_RUNTIME_CONTRACT_DESIGN.md`) is additive to,
not a correction of, `PlannerRuntimeSpecification.md`'s own field
shapes.

**One remaining contradiction, distinct from the one already closed:
`Agent.md`'s interface versus what Agent Runtime actually is.**
Covered in Section 1 above and not repeated in full here, but it
belongs in this section too: `Agent.md` is not *stale* the way
`WorldModel.md` was (nothing superseded it -- `Agent.kt` was never
implemented at all), so this is a different failure mode than Category
A1's -- not a corrected contract left undocumented, but a specified
contract that was quietly bypassed by a same-named but differently-
shaped implementation elsewhere in the same subsystem's own family of
documents. Whether this rises to the same Category A urgency as the
Volume 3 finding this Sprint already closed is a judgement call for
whoever owns the next architecture-actions pass; this review's own
mandate is to identify it, not adjudicate its priority.

**No other contradiction was found.** `MEMORY_RUNTIME_ARCHITECTURE.md`,
`WORLD_MODEL_RUNTIME_ARCHITECTURE.md`, `PLANNER_RUNTIME_PROGRESSION_DESIGN.md`,
and `MULTI_STEP_AGENT_RUN_DESIGN.md` all remain consistent with their
own Contract Designs and implementations.

---

## 7. Long-Term Maintainability

At 100 runtime modules, 50 plugins, 20 reasoning providers, and 100
million Memory records -- organisational and architectural
maintainability only, not runtime performance:

**The governance model scales better than the current documentation
does.** PES-001's Contract Design stage and mandatory Self-Traceability
Review are exactly the decentralising mechanism a 100-module platform
needs: they let many independently-working teams each build a module
against a fixed, small set of platform-wide rules (the Constitution,
the sixteen-plus Architecture Decisions, PES-001 itself) without a
single central reviewer needing to adjudicate every new interface in
real time. The evidence for this is already in hand, not projected --
four subsystems, built by different units at different times, already
converged on the same policy-seam shape and the same "one interface, no
wrapper" boundary without being told to, before the rule existed in
writing. That convergence is what a decentralised governance model
looks like working correctly, and nothing about it depends on staying
at four subsystems rather than growing toward one hundred.

**The one place this does not yet scale is orientation, not
governance.** Section 5's finding -- that the index, implementation
order, and architecture history documents have not kept pace with what
already exists at four subsystems -- becomes a materially worse problem
at one hundred. A new team joining today can still recover the true
state of the platform by reading `docs/reviews/` directly; a new team
joining once one hundred modules exist, following the same three
stale entry points, would face a much larger gap between what those
documents claim and what is actually true. This is an argument for
treating Section 5's finding as more urgent as the platform grows, not
for changing the governance model itself.

**Memory at 100 million records remains a contract-shape non-issue,
correctly.** `MemoryStore`'s interface does not assume in-memory-forever
storage, and the deferred `MemoryRetrievalPolicy` seam is already the
named escape valve for a scored or indexed retrieval strategy. This was
already true at 10 million records in the prior review and remains true
at 100 million -- the number does not change the answer, because the
interface was never coupled to the reference implementation's own
data structure.

---

## 8. Platform Readiness

**A. Is Parker now architecturally stable enough that future work
should primarily consist of adding capabilities rather than redesigning
foundations?**

Yes, with the one qualification named throughout this review: the
Agent/Agent Runtime naming collision (Sections 1, 3, 6) should be
resolved -- or at minimum, explicitly reconciled in writing -- before
a fifth runtime subsystem reuses either name's vocabulary, since it is
exactly the kind of ambiguity that compounds with scale rather than
resolving itself.

**B. Which architectural areas should now be considered effectively
frozen unless an ADR justifies change?**

Execution Pipeline authority (AD-003). The Context Provider boundary
(AD-012, ADR-023). The policy seam pattern (internal, injected,
`suspend`-capable, decision-not-authority). The Architecture → Contract
Design → Implementation → Self-Traceability Review → Post-Implementation
Review workflow. The constitutional separation of Runtime, Memory,
World Model, and Planning. Each of these has now survived multiple
independent implementations built by different units at different
times, which is the evidentiary bar this review treats as sufficient
for "stable platform law," per `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`
Category D.

**C. Which areas remain intentionally evolutionary?**

The concurrency discipline gap between Agent/Planner and Memory/World
Model (Category B1) -- deferred until a real policy implementation
demonstrates the need. `WorldModelUpdatePolicy`'s bundled
responsibilities (Category B2) -- deferred until a concrete
implementation needs to vary them independently. `ObservationResult`'s
three-variant shape and `PlanCandidate`'s carry-forward field count
(Category C1/C2) -- genuine design questions, correctly left alone.
Memory's deferred retrieval-ranking seam, and any future decision about
Memory or World Model event publication under ADR-023's already-settled
shape.

**D. What single architectural risk deserves the closest attention over
the next year?**

Not a constitutional risk -- none was found, in three independent
review passes now. The risk is documentation drift outpacing
architectural growth: three of the platform's own orientation documents
already describe a version of Parker that stopped existing several
sprints ago, and the gap between "what the entry-point documents say"
and "what is actually true" will only widen, by construction, unless
something is done to keep at least one document synchronised with
reality as new subsystems are added. This is the same category of
problem this Sprint's Volume 3 alignment work just fixed for two
Volume 3 documents specifically -- the risk is that the fix was scoped
to those two documents, and the same failure mode is already visible,
today, one layer up, in the documents meant to orient a reader before
they ever reach a Volume 3 interface page.

---

## 9. Overall Verdict

**Architecture v2.0 achieved with minor reservations.**

The constitutional model is intact, verified directly against Kotlin
in three independent review passes with no exception found. Four
runtime subsystems now share one governance process, one source-of-truth
ordering, and one policy-seam pattern, arrived at independently and
now codified rather than merely observed. The one open Category A
documentation contradiction this review's own predecessor identified
has already been closed. What keeps this from being an unqualified
"achieved" is narrow and specifically named, not diffuse: the
Agent/Agent Runtime naming collision (Sections 1, 3, 6) and the
orientation-document staleness (Section 5, Section 7) are both real,
both evidenced directly rather than inferred, and both are the kind of
finding that gets more expensive to fix the longer they're left --
which is exactly why they are reservations on an otherwise-achieved
baseline, not reasons to withhold it.

---

## Addendum — Sprint 5 Cleanup Resolution

**Status: both reservations resolved.** This addendum records the
resolution; the findings above are left unaltered as the historical
record of what this review found.

The Agent terminology ambiguity (Sections 1, 3, 6) is resolved by
documentation clarification only, no Kotlin renamed: `docs/specifications/volume-03-core-interfaces/Agent.md`
is retitled "Background Agent Interface" and now states explicitly that
Agent Runtime does not instantiate or depend on it;
`docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`'s
Overview and Status sections now distinguish "Agent Instance" (its own
concept, Section 4) from the Background Agent interface by name; and
`docs/architecture/14-agent-framework.md` carries a short reciprocal
note pointing to both governing documents. `src/interfaces/Agent.kt`
remains unmodified, unimplemented, and excluded from `build.gradle.kts`.

The orientation-document staleness (Section 5, Section 7) is resolved
by updating `docs/architecture/00-index.md`, `docs/architecture/IMPLEMENTATION_ORDER.md`,
and `docs/architecture/ARCHITECTURE_HISTORY.md` to reflect that Agent
Runtime, Planner Runtime, Memory Runtime, and World Model Runtime are
now implemented, without claiming any capability beyond what is
actually built (World Model event publication, `IMPLEMENTATION_GAPS.md`
#47, remains explicitly open in all three; the Background Agent
interface is explicitly noted as unimplemented in all three).

No constitutional finding in this review changed as a result of this
cleanup -- both resolutions are documentation-only, per Sprint 5's own
scope.
