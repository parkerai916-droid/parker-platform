# Sprint 4 Architecture Actions

## Status

Consolidation document. Read-only-to-code: no Kotlin, no test, no
`IMPLEMENTATION_GAPS.md`, and no `IMPLEMENTATION_HISTORY.md` was
touched to produce this document. This is the only file this
consolidation creates or modifies.

## Purpose

`docs/reviews/` now holds a Simplicity and Long-Term Maintainability
Review that raised roughly a dozen observations, findings, and
discussion points. Not all of them are the same kind of thing. Some are
live contradictions that mislead a reader today. Some are known,
intentional trade-offs already reasoned about and correctly left alone.
Some are genuinely interesting design questions with no wrong answer
and no present cost to leaving as they are. And some of what the
platform has done four times independently now has earned the right to
stop being reconsidered at all.

This document sorts every finding from that review into exactly one of
those four categories, and applies one test to every recommendation
before it is allowed to stand: *if Parker had another 100,000 lines of
code tomorrow, would this recommendation still make sense?* Where the
answer is no, the recommendation is rejected here, explicitly, rather
than carried forward as unfinished business. The burden of proof in
this document is on changing the architecture, not on preserving it.

This document does not invent new architecture, does not recommend
renaming anything for cosmetic consistency, and does not recommend
adding an abstraction for symmetry alone. Where a finding does not meet
that bar, it is recorded as a discussion (Category C) or as accepted
debt (Category B), not elevated into an action.

---

## Category A — Immediate Architecture Corrections

Findings that represent an inconsistency, a contradiction, or an
incorrect source of truth, and should result in architectural work.

### A1. `WorldModel.md` and `MemoryStore.md` describe interfaces that no longer exist

**Problem.** `docs/specifications/volume-03-core-interfaces/WorldModel.md`'s
own "Required Operations" code block still declares
`current(resourceId: ResourceId): WorldState?` and
`query(query: WorldQuery): List<WorldState>`.
`docs/specifications/volume-03-core-interfaces/MemoryStore.md`'s still
declares `addCandidate(candidate: CandidateMemory): MemoryId`,
`promote(memoryId: MemoryId): Memory`, and a `ForgetResult` return type.
Every one of these was deliberately renamed or removed --
`WorldState` → `WorldBelief`, `ResourceId` → a plain `String` subject,
`promote` folded into `MemoryStore.remember`, `ForgetResult` dropped
entirely in favour of a plain `Boolean` -- and each change is already
documented and justified in `WORLD_MODEL_CONTRACT_DESIGN.md` and
`MEMORY_CONTRACT_DESIGN.md` respectively. Neither Volume 3 document was
ever updated to match.

**Affected documents.** `docs/specifications/volume-03-core-interfaces/WorldModel.md`,
`docs/specifications/volume-03-core-interfaces/MemoryStore.md`, and, by
extension, `docs/architecture/PARKER_ENGINEERING_STANDARD.md` Chapter 3
(Source of Truth), whose current precedence order ranks Specifications
above Verified Kotlin Implementation.

**Why it matters.** This is not a stale-comment problem. PES-001's own
Chapter 3 tells a future contributor to trust a Specification over
Verified Kotlin Implementation if the two disagree. They now disagree,
on two separate subsystems, and the Specifications are the ones that
are wrong. A new contributor who reads `WorldModel.md` or `MemoryStore.md`
first -- the natural place to start, given their location under "core
interfaces" -- would be actively misled about the shape of a working,
tested interface, and PES-001's own precedence rule would tell them
they were right to trust the misleading document.

**Recommended action.** A Level 1 Documentation Maintenance correction
(PES-001 Chapter 4, Level 1: wording clarification, not new
architecture) to each Volume 3 document's "Required Operations" block,
bringing it into agreement with `src/interfaces/WorldModel.kt` and
`src/interfaces/MemoryStore.kt`, with a short note on each pointing to
the governing Contract Design document as the field-level authority.
This is a documentation-accuracy fix, not an architectural change --
no new decision is being made, only an already-made decision being
reflected in the one place it is currently missing.

**100,000-line test.** Would correcting these two documents still make
sense after another 100,000 lines? Yes, more so: by then, far more
contributors will have read "core interfaces" as their starting point,
and the cost of the same confusion compounds with every one of them.
The cost of the fix itself does not grow with the codebase -- it is
two short document edits, unrelated to line count. Accepted.

---

## Category B — Engineering Debt

Genuine technical debt, intentionally deferred, with a stated
condition under which it should be revisited.

### B1. Memory Runtime and World Model Runtime hold their mutex across a call to an injected policy

**Current state.** `InMemoryMemoryStore.remember` and
`InMemoryWorldModel.observe`/`current`/`query` each hold their single
`Mutex` for the entire operation, including the `suspend` call into
their own injected policy (`MemoryPromotionPolicy.evaluate`,
`WorldModelUpdatePolicy.evaluate`/`isStillCurrent`). `InMemoryAgentRuntime`
and `InMemoryPlannerRuntime` instead release their lock before calling
an injected collaborator, per the discipline PES-001 v2.1 Chapter 7.1
now states as a standing rule.

**Why it is acceptable today.** Every current policy implementation
(`DefaultMemoryPromotionPolicy`, `DefaultWorldModelUpdatePolicy`) is
fast and synchronous. Holding the lock across the call does not cause
incorrect behaviour -- if anything, it is the more conservative of the
two designs, since it serialises every operation on the subsystem by
construction. The cost is latent (throughput), not active (a bug).
Rewriting either class now, with no evidence of an actual problem,
would be exactly the churn this consolidation exists to prevent.

**Trigger to revisit.** The moment either policy seam receives an
implementation with unbounded or externally-dependent duration -- one
that consults an embedding service, an external store, a model, or any
other collaborator whose latency this subsystem does not control. That
is the condition PES-001 v2.1 Chapter 7.1 already names; nothing new
is being added here beyond restating it as still-open, still-deferred
debt.

**100,000-line test.** Would deferring this fix still make sense after
another 100,000 lines? Yes, provided the trigger condition above still
has not occurred. Fixing correctly-functioning code pre-emptively,
because it differs stylistically from a sibling subsystem, is not a
maintainability improvement -- it is churn justified by symmetry alone,
which this document's own constraints exclude. Accepted as deferred,
not rejected, not escalated.

### B2. `WorldModelUpdatePolicy` bundles two decision responsibilities in one interface

**Current state.** `WorldModelUpdatePolicy` declares both `evaluate`
(accept/reject a submitted observation) and `isStillCurrent` (an
unrelated staleness judgment) on the same interface, unlike
`AgentStepSource`, `PlanDecision`, and `MemoryPromotionPolicy`, each of
which has exactly one decision-relevant method. PES-001 v2.1 Chapter
7.2 already permits this explicitly, rather than requiring a split.

**Why it is acceptable today.** No concrete implementation has ever
needed to vary the two judgments independently. Splitting the interface
now, with no consumer that needs the split, would add a seam
(`WorldModelExpiryPolicy` or similar) purely so the type shape matched
its three siblings -- an abstraction added for symmetry, which this
document's constraints explicitly reject.

**Trigger to revisit.** A future, concrete implementation that needs a
different staleness rule than its acceptance rule (for example, a
policy that accepts observations one way but expires beliefs on an
entirely separate, externally-configured schedule).

**100,000-line test.** Would splitting this today still make sense
after another 100,000 lines? No -- there is no more evidence for the
split at that scale than there is today, only more code that would
need to be touched to make it. Rejected as an action; retained as
accepted, named debt.

### B3. Agent Runtime and Planner Runtime each guard all of their subsystem's concurrent work with one shared lock

**Current state.** `InMemoryAgentRuntime`'s `mutex` guards every Agent
Run simultaneously; `InMemoryPlannerRuntime`'s guards every Planning
Session simultaneously. Two unrelated Agent Runs, or two unrelated
Planning Sessions, always contend for the same lock object, even though
their state lives at different map keys and their held-duration is
already short per Chapter 7.1's own discipline.

**Why it is acceptable today.** No measurement anywhere in this
platform's history demonstrates contention under real concurrent load
-- "multiple concurrent agents" is, today, a future scenario, not an
observed one. Sharding the lock (per `AgentRunId`, per
`PlanningSessionId`) would add real bookkeeping complexity -- lock
lifecycle management, potential cross-lock ordering concerns -- to
solve a problem that has not been shown to exist.

**Trigger to revisit.** A measured instance of lock contention under
genuine concurrent-agent or concurrent-planning-session load. Not a
projection -- a demonstrated one.

**100,000-line test.** Would sharding this lock today still make sense
after another 100,000 lines, with no measured problem in hand either
now or then? No. This is the clearest instance in this review of a
recommendation that fails its own test on inspection: the correct
answer at any code size, absent a measured bottleneck, is not to
change it. Rejected as an action; recorded as a named, specific
condition under which it should be reconsidered, not as work to do now.

---

## Category C — Design Discussions

Interesting architectural questions with no present cost to leaving
unresolved, and no clear improvement large enough to justify touching
working, tested code.

### C1. `ObservationResult`'s three variants versus two

`Accepted(belief)` and `Invalidated(subject)` could be collapsed into
one `Result(subject, belief: WorldBelief?)` variant, alongside
`Rejected`, mirroring `MemoryPromotionDecision`'s own two-variant
`Promote`/`Reject` shape. The three-variant form buys exhaustive
`when`-pattern-matching over "something now exists" versus "nothing
does"; the two-variant form buys one fewer sealed subtype. Both are
internally consistent; neither is a defect.

**Why stability wins here.** The three-variant shape is already
implemented, already tested, and already reasoned about in
`WORLD_MODEL_CONTRACT_DESIGN.md` §3. Collapsing it would require
touching `InMemoryWorldModel`, `DefaultWorldModelUpdatePolicy`, and
every test that pattern-matches on the current three variants, for a
purely ergonomic gain with no behavioural or correctness improvement.

**100,000-line test.** Would collapsing this after another 100,000
lines still make sense? No -- by then, more code will pattern-match
against the existing three-variant shape than does today, which makes
the same ergonomic argument strictly more expensive to act on for the
same marginal benefit it offers right now. The correct time to have
made this choice differently was before implementation, not after.
Rejected as an action; retained as a documented discussion only.

### C2. `PlanCandidate`'s eleven fields, two of which are decision-relevant

`DefaultPlanDecision` only ever reads `planCandidateId` and `goal`; the
other nine fields exist solely as carry-forward so a selected candidate
becomes a well-formed `TaskProposal` without a re-entry step -- already
disclosed as a deliberate two-tier design in the type's own KDoc. This
could be split into a decision-relevant type and a separate
carry-forward type.

**Why stability wins here.** Splitting the type would not reduce the
amount of information that must be carried from a Plan Candidate to a
Task Proposal -- it would only relocate where that information lives,
adding a fourth Planner contract type to hold data the current single
type already holds correctly and already carries through one call path.

**100,000-line test.** Would splitting this after another 100,000
lines still make sense? No -- more code across a larger platform means
more call sites threading whichever split types through, for zero net
reduction in what must be carried end to end. Rejected as an action;
retained as a documented discussion only.

### C3. Interface naming taxonomy (repository / context provider / command channel / decision seam)

This is not reopened here. The prior Sprint 4 stress review recommended
standardising interface naming across `MemoryStore`, `WorldModel`,
`PlannerRuntime`, and `AgentRunCommandChannel`; that recommendation was
withdrawn during Sprint 4 Engineering Consolidation once it was
established that the four names track four genuinely different
abstractions, not four inconsistent labels for the same one. The only
trace retained is a non-binding taxonomy observation, explicitly not a
rule. It is listed here only for completeness, since a reader of the
original review might otherwise expect it to reappear as a live
discussion. It does not, and no further action attaches to it.

---

## Category D — Architecture Now Considered Stable

Decisions that have survived multiple independent implementations and
should now be treated as settled platform law -- not reopened by
routine future work, only by a deliberate, evidenced Architecture
Decision.

**Execution Pipeline authority (AD-003).** Verified directly in Kotlin,
not assumed from documentation, across all four implemented runtime
subsystems: Memory and World Model touch `ExecutionPipeline`,
`ToolRegistry`, and `ToolInvocationBinding` nowhere at all; Agent
Runtime and Planner Runtime route every action with external effect
through it without exception. No counter-instance has been found across
two independent review passes. *100,000-line test:* settling this now,
while reversing it would still be cheap, is strictly better than
leaving it open until 100,000 more lines depend on it implicitly.
Stable.

**Context Provider architecture (AD-012, and now ADR-023).** Memory and
World Model independently converged on the same shape -- a read source
with no write path into Task, Agent Run, or Planning Session state, and
no subscription to anything -- without being told to converge, and
ADR-023 now gives the one open question (event publication) a settled
shape without either subsystem needing to implement it yet. Two
independent implementations agreeing, plus a governing ADR, is the
evidence this category asks for. Stable.

**Policy seam pattern (`AgentStepSource`, `PlanDecision`,
`MemoryPromotionPolicy`, `WorldModelUpdatePolicy`).** Four seams, in
four subsystems, built by different units at different times, each
independently arrived at internal / injected / `suspend`-capable /
decision-not-authority, each citing the previous one as precedent, well
before PES-001 v2.1 Chapter 7.2 named the pattern explicitly. The
convergence itself, achieved without a rule forcing it, is the
evidence. Stable, with the specific naming/method-shape variation among
the four (Section 4 of the prior review) correctly left unstandardised,
since that variation was never the load-bearing part of the pattern.

**Architecture → Contract Design → Implementation workflow.** Memory
and World Model each followed this order from their first
implementation unit and needed zero correction passes. Planner did not
have this stage available for its first attempt and needed a fourth
document (`PLANNER_RUNTIME_CONTRACT_DESIGN.md`, the "Alignment Pass")
specifically to correct drift the missing stage allowed. This is a
natural experiment with both a control and a treatment case in the
platform's own history, and the treatment group is the only one that
needed rework. Now mandatory per PES-001 v2.1 Stage 2A. Stable.

**Self-Traceability Review.** Already proven as a detection mechanism,
not merely a paperwork step: it is the mechanism that surfaced, in
writing, that `WorldModelUpdateDecision` and `WorldModelRuntime` were
correctly excluded rather than silently added, and it is what this very
consolidation relies on to know which contracts exist by design rather
than by accident. Now mandatory per PES-001 v2.1 for Level 2/3 units.
Stable.

**Post-Implementation Review.** The oldest and most exercised of every
process step reviewed here -- used across every sprint, not only
Sprint 4 -- with no counter-evidence of it failing to catch a real issue
anywhere in either review pass conducted so far. Stable.

**Engineering Consolidation process.** Provisionally stable, not yet at
the same evidentiary bar as the items above. It has been exercised
exactly twice so far: once to produce the Sprint 4 PES-001 amendment,
ADR-023, and the build-scope checklist, and once more in the form of
this very document, which is itself a second-order consolidation of a
review's findings. Two cycles, both productive, is real evidence but a
smaller sample than "survived multiple independent implementations"
represents for the other items in this category. Recorded as stable
enough to keep doing, not yet stable enough to claim the same
confidence as Execution Pipeline authority or Post-Implementation
Review.

**Constitutional separation of Runtime, Memory, World Model, and
Planning.** Checked directly against Kotlin, not against
documentation's own claims, across all seven areas the prior review
named (owner authority, model independence, local-first, trust-first,
replaceable policy seams, context-provider boundaries, Execution
Pipeline authority) in two independent review passes, with no violation
found in either. Stable.

**100,000-line test, applied once to the whole category.** Would
treating all eight of these as settled -- reopenable only by a
deliberate Architecture Decision, not by routine implementation work --
still make sense after another 100,000 lines? Yes, and more so: the
cost of reversing any one of them only grows as more code comes to
depend on it, which means the correct time to lock each in is now,
while that cost is still low, not later, once it is not.

---

## Summary

One finding required action: the two stale Volume 3 specification
documents, corrected as a documentation-accuracy fix, not an
architectural change. Three findings are accepted, named, bounded
engineering debt, each with an explicit condition under which it should
be revisited and not before. Three findings are genuine design
questions with real arguments on both sides and no present cost to
leaving exactly as built. Eight decisions have now earned the status of
settled platform law, reopenable only by a deliberate, evidenced
Architecture Decision going forward.

Parker's architecture, as it stands after Sprint 3 Runtime Foundation
and Sprint 4 Tracks A and B, does not need rewriting. It needs one
small documentation correction, and it needs the discipline, now that
several things have been tried more than once and agreed with
themselves, to stop re-litigating them.
