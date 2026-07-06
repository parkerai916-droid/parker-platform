# Architecture v2.0 — Independent Audit Triage

## Status

Triage document only. Classifies the six findings from the independent,
skeptical architectural audit ("assume you are a senior architect joining
Parker for the first time... ignore implementation history"). This
document does not fix anything, does not redesign anything, and does not
add findings beyond the six being classified. It does not modify Kotlin,
tests, `IMPLEMENTATION_HISTORY.md`, or `IMPLEMENTATION_GAPS.md`. Nothing
in this document has been committed.

## Purpose

Each finding below is recorded with: a summary, the affected
subsystem/files, its classification, the action it requires, the risk of
ignoring it, and the recommended next unit to address it — as a separate,
future piece of work, not performed here.

Classification categories used: **present defect** (currently wrong
behavior), **latent architectural risk** (correct today, breaks under a
foreseeable future condition), **documentation/process issue** (the code
is fine; a document, evidence step, or disclosure is missing or
overclaims), **deferred scale issue** (correct and low-risk at current
scale; expensive to retrofit once scale/usage changes).

Action categories used: **immediate action**, **logged gap**, **ADR /
design unit**, **no action**.

---

## Finding 1 — Deterministic parent-derived IDs cap multiplicity for AgentRunId and TaskProposalId

**Summary.** `InMemoryAgentRuntime` mints `AgentRunId("run-for-${taskId}")`
and rejects a second `START` for the same Task. `InMemoryPlannerRuntime`
mints `TaskProposalId("${planningSessionId}-proposal-1")`. Both governing
specifications reserve one-to-many multiplicity here (Task Manager
associates "zero, one, or many Agent Runs" with a Task; the Planner
Runtime progression design allows "one or more Task Proposals" per
session). The implementations structurally foreclose the "many" case.
Neither cap is recorded in `IMPLEMENTATION_GAPS.md`.

**Affected subsystem/files.** `src/runtime/InMemoryAgentRuntime.kt:143`;
`src/runtime/InMemoryPlannerRuntime.kt:223`;
`docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`;
`docs/architecture/PLANNER_RUNTIME_PROGRESSION_DESIGN.md`;
`docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md`.

**Classification.** Latent architectural risk. The cap itself is
disclosed at runtime (an exception message names it explicitly), but its
absence from `IMPLEMENTATION_GAPS.md` means it is not disclosed in the
one place a future contributor is expected to check before building on
top of either subsystem.

**Action required.** Logged gap. No code change and no ADR are needed to
address the disclosure problem; the cap itself is a reasonable Sprint
1-era scope reduction, not a defect.

**Risk if ignored.** A future unit that assumes one-Task-to-many-Agent-Runs
or one-session-to-many-Task-Proposals (retry logic, competing plan
candidates surfaced as separate proposals) will discover the cap only by
hitting a runtime exception, after having already designed around the
specification's stated multiplicity.

**Recommended next unit.** A Level 1 documentation unit adding two entries
to `IMPLEMENTATION_GAPS.md`: one for `AgentRunId` minting capping Agent
Runs at one per Task, one for `TaskProposalId` minting capping Task
Proposals at one per Planning Session.

---

## Finding 2 — Planner Runtime publisher identity is hardcoded and unresolved

**Summary.** `InMemoryAgentRuntime` resolves its Agent identity through
`identityService.resolve(agentIdentityPrincipalId)` before a run starts,
and republishes events under that verified identity. `InMemoryPlannerRuntime`
instead publishes under a hardcoded `PrincipalId("system.planner-runtime")`
that is never registered with or resolved through `IdentityService`. This
is masked today because the wired-in `EventBus` authenticator allows all
publishers unconditionally — but `EventBus`'s own documentation names a
real, identity-backed authenticator as the intended next step for that
seam.

**Affected subsystem/files.**
`src/runtime/InMemoryPlannerRuntime.kt:108, 312` (contrast
`src/runtime/InMemoryAgentRuntime.kt:166, 539`); `src/runtime/InMemoryEventBus.kt`
(current permissive authenticator); `docs/architecture/EventBus.md`;
ADR-005.

**Classification.** Latent architectural risk, currently masked by a
permissive authenticator. Not a present defect: nothing observably
misbehaves today.

**Action required.** Logged gap now. Escalate to an ADR / design
precondition at the point a real, identity-backed `EventBus` authenticator
is proposed — that unit should not proceed without first resolving this.

**Risk if ignored.** Silent, total failure of Planner Runtime's event
publication the instant identity-backed authentication ships, with no
existing test capable of catching it, since no test currently exercises a
non-permissive authenticator.

**Recommended next unit.** A Level 1 documentation unit adding an entry to
`IMPLEMENTATION_GAPS.md` describing the unresolved system Principal, and a
one-line precondition attached to any future EventBus-authentication
Contract Design: "confirm all current publishers use identity-resolved
Principals before switching the authenticator."

---

## Finding 3 — EventBus publish is synchronous; one slow subscriber can block all publishers

**Summary.** `InMemoryEventBus.publish()` iterates subscribers and calls
`subscription.deliver(event)` sequentially, inside the publisher's own
coroutine. A throwing subscriber is caught and isolated; a slow or
blocking one is not. All four runtime subsystems share one `EventBus`
instance.

**Affected subsystem/files.** `src/runtime/InMemoryEventBus.kt:88-112`;
every runtime subsystem as a publisher; any future Audit or logging
subscriber as the prospective slow consumer.

**Classification.** Deferred scale issue. Correct and low-risk today —
no current subscriber performs blocking work. Becomes an architectural
bottleneck the moment one does.

**Action required.** ADR / design unit, but not urgently: the decision
(concurrent delivery, per-subscriber timeout, or an explicit
fire-and-forget dispatch boundary) should be made and recorded before the
first subscriber that performs real I/O or non-trivial work is added, not
before that.

**Risk if ignored.** Once a real Audit or logging subscriber exists,
every publisher in the system inherits its latency. This is exactly the
kind of bottleneck the platform's own "100 plugins / 20 reasoning
providers / concurrent agents" future-scale scenarios would expose, and
retrofitting delivery isolation underneath many already-built publishers
is more expensive than deciding the shape now.

**Recommended next unit.** An ADR ("EventBus Delivery Isolation") authored
alongside — not before — the first Contract Design that introduces a
subscriber doing real work. No standalone unit needed today.

---

## Finding 4 — Durability/auditability boundaries are not structurally defined despite Memory being called durable

**Summary.** `MemoryStore.md`'s own Purpose statement calls Memory
"Parker's durable long-term knowledge," but `InMemoryMemoryStore` — like
every other runtime store, including Identity's Principals — loses all
state on process restart. No document specifies a persistence boundary
for any subsystem. Separately, `AuditService` does not exist and
`EventBus` is explicitly at-most-once with no replay, so the
Constitution's auditability guarantee ("every authorized action leaves a
record sufficient to reconstruct...") currently has no durable mechanism
behind it anywhere in the platform.

**Affected subsystem/files.**
`docs/specifications/volume-03-core-interfaces/MemoryStore.md` (Purpose
language); `src/interfaces/InMemoryMemoryStore.kt`; `InMemoryIdentityService`;
`src/runtime/InMemoryEventBus.kt` (no persistence/replay); Parker
Constitution's Auditability principle; absent `AuditService`.

**Classification.** Two distinct sub-issues bundled in the audit finding:
a documentation/process issue (the word "durable" overclaims relative to
what is implemented today) and a deferred scale issue (no subsystem has a
defined persistence boundary, which is acceptable for an in-memory
reference phase but is a foundational decision still outstanding).

**Action required.** ADR / design unit for the persistence/durability
boundary itself (which layer owns durability, what "durable" is scoped to
mean until a real store exists). A smaller, separate documentation
correction to `MemoryStore.md`'s Purpose language could precede that ADR,
but is not performed in this unit.

**Risk if ignored.** Every subsystem that eventually needs real storage
invents its own persistence assumption independently; callers treat
"durable" as a load-bearing guarantee before checking the implementation;
audit/compliance expectations get built on a constitutional promise with
no mechanism behind it, which is far more expensive to discover after
other subsystems have started depending on it than to name now.

**Recommended next unit.** An ADR defining the persistence/durability
boundary across Memory, Identity, and Audit, including whether
`MemoryStore.md`'s current language should be scoped ("logically durable
within process lifetime; physical durability is a reserved seam") until
that ADR lands.

---

## Finding 5 — Sprint 3/4 test totals are recorded as projections, not confirmed runs

**Addendum (post-triage correction).** Android Studio verification has
since been performed and recorded: 360/360 passing after Sprint 4 Track A
Unit A3 (Memory Runtime), and 413/413 passing after Sprint 4 Track B Unit
B3 (World Model Runtime). `docs/implementation/IMPLEMENTATION_HISTORY.md`
has been corrected accordingly (repository-history correction, not a code
fix). This finding is **no longer a test-execution blocker** for those two
units. The original summary below is retained for the record; the
classification, action, and final-ordering placement have been updated to
reflect the current state. Sprint 3 Unit D2's own static projection
(315/315) was not separately re-verified by this correction and is not
claimed to be confirmed — see `IMPLEMENTATION_GAPS.md` and
`IMPLEMENTATION_HISTORY.md` for its unchanged status.

**Summary (original).** The only test count in this repository's own
history backed by an actually executed run was, at the time of the
original audit, the Phase 2 checkpoint (101 tests,
`v0.8-runtime-complete-checklist.md`). Every subsequent addition —
several hundred more tests across four runtime subsystems built in Sprint
3 and Sprint 4 — was recorded in `IMPLEMENTATION_HISTORY.md` as an
"expected total... not authoritative until confirmed by Android Studio."

**Affected subsystem/files.** `docs/implementation/IMPLEMENTATION_HISTORY.md`
(Sprint 3/4 unit entries); all Sprint 3/4 Kotlin source; PES-001 Chapter 5
(Engineering Evidence), which assigns this verification step to human
authority.

**Classification.** Was a documentation/process issue (an open evidence
gap, not a confirmed compilation failure). Now: **resolved for Unit A3 and
Unit B3 specifically** by human-executed Android Studio verification;
still an open documentation/process issue for any other Sprint 3/4 entry
that remains an unconfirmed projection (e.g. Unit D2's 315/315).

**Action required.** Was immediate action (a human, Android-Studio-executed
verification step). For A3/B3: **action taken, recorded in
`IMPLEMENTATION_HISTORY.md`.** For any remaining unconfirmed Sprint 3/4
entry: the same human verification step is still recommended, at ordinary
priority, not blocking priority — it no longer sits ahead of new
implementation, since the two most architecturally load-bearing recent
units (Memory Runtime, World Model Runtime) are now confirmed.

**Risk if ignored.** Was: every "stable platform law" document produced
this cycle rests on source-level reading of unconfirmed code. Now
substantially reduced for Memory Runtime and World Model Runtime
specifically; residual risk is limited to whichever earlier Sprint 3
entries (e.g. Unit D2) remain unconfirmed projections.

**Recommended next unit.** No longer a dedicated, blocking verification
unit. Optional, ordinary-priority follow-up: confirm any remaining
unconfirmed Sprint 3/4 projection (Unit D2's 315/315) in Android Studio
the next time that code path is touched, rather than as a standalone
unit.

---

## Finding 6 — Agent/Agent Runtime ambiguity is fixed in prose only, not structurally

**Summary.** Sprint 5 added reciprocal clarification notes to `Agent.md`,
`AgentRuntimeSpecification.md`, and `14-agent-framework.md` distinguishing
the long-lived Background Agent from the bounded Agent Runtime. This is
real and currently sufficient — a repository-wide search confirms zero
code-level references between them, and `Agent.kt` remains excluded from
build scope. But nothing at the code or build level enforces the
distinction beyond that exclusion; the guard is documentation discipline,
not structure.

**Affected subsystem/files.**
`docs/specifications/volume-03-core-interfaces/Agent.md`;
`docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`;
`docs/architecture/14-agent-framework.md`; `src/interfaces/Agent.kt`
(excluded); `src/runtime/InMemoryAgentRuntime.kt` (confirmed no reference).

**Classification.** Documentation/process issue, substantially already
addressed. Residual exposure is structural but currently low-risk while
`Agent.kt` stays excluded from the build.

**Action required.** No action beyond what exists. The clarification
documents already state that any future connection requires its own ADR
or Contract Design pass under PES-001 Stage 2A — that is the enforcement
mechanism, and it does not need duplicating in code today.

**Risk if ignored.** Low while `Agent.kt` remains excluded. Rises only if
a future contributor works from code first and skips the prose
clarification; the existing build-scope exclusion checklist is the
natural place to catch this if `Agent.kt` is ever proposed for inclusion.

**Recommended next unit.** None required now. Optionally, a future
revision of `BUILD_SCOPE_EXCLUSION_CHECKLIST.md` could add an explicit
check for this specific pairing when `Agent.kt`'s exclusion is ever
lifted — not urgent, not a standalone unit.

---

## Final Ordering

**Must fix before more implementation**
- None currently. Finding 5 was the sole occupant of this bucket; it is
  resolved for Unit A3/Unit B3 by Android Studio verification now recorded
  in `IMPLEMENTATION_HISTORY.md` (Task 2, this follow-up). See Finding 5's
  addendum above.

**Should fix soon**
- Finding 1 — log the AgentRunId/TaskProposalId cardinality caps in
  `IMPLEMENTATION_GAPS.md`. Cheap, disclosure-only, closes the
  "undisclosed" part of the risk immediately.
- Finding 2 — log Planner Runtime's unresolved system Principal in
  `IMPLEMENTATION_GAPS.md`. Equally cheap, and removes the silent-failure
  surprise before anyone touches EventBus authentication.

**Needs ADR/design unit**
- Finding 3 — EventBus delivery isolation, authored alongside the first
  subscriber that does real work, not before.
- Finding 4 — persistence/durability boundary across Memory, Identity,
  and Audit, including scoping `MemoryStore.md`'s "durable" language
  until that boundary exists.

**Can defer**
- None. Findings 3 and 4 are low-urgency today but are placed in the ADR
  bucket above rather than here, since deferring them without a recorded
  decision is exactly how they become expensive — the recommendation is
  to write the ADR now and implement later, not to defer the decision
  itself.

**No action recommended**
- Finding 6 — already substantially resolved by the Sprint 5
  documentation-only clarification; no further unit is warranted while
  `Agent.kt` remains excluded from build scope.
