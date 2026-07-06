# Parker Platform — Architecture v2.0 Frozen Baseline

## 1. Purpose

This document exists because Parker has reached a point where the
question a new contributor asks is no longer "what should Parker's
architecture be?" but "what is Parker's architecture, today, and what may
I safely build on top of it?" Every prior architecture document in this
repository was written to *establish* something — a chapter, a
specification, a contract design, a review. This one is written to
*record* what those documents have already established, in one place, so
it does not have to be reconstructed by reading the full history.

**"Frozen Baseline" does not mean finished, and it does not mean
unchangeable.** It means: the runtime foundation, the constitutional
principles, the engineering workflow, and the recurring implementation
patterns described below are no longer open questions to be resolved by
routine implementation work. They are the assumed starting point for any
future unit. Changing any of them is still possible, but it requires the
same weight of evidence and process that established them in the first
place — an Architecture Decision or a Contract Design revision, not a
side effect of building the next feature.

This document does not introduce new architecture. Everything in it has
already been decided, implemented, reviewed, and — where applicable —
independently audited elsewhere in this repository. Its only job is to
state the current baseline plainly enough that a future contributor does
not need to re-derive it.

---

## 2. Runtime Foundation

Six runtime subsystems have been specified, contract-designed,
implemented, tested, and reviewed. Each is described here by
responsibility only.

**Identity Runtime.** Registers Principals and resolves a Principal's
current identity and lifecycle status (Created, Active, Suspended,
Revoked, Archived). It is the trust foundation every other subsystem's
notion of "who is acting" ultimately depends on. It owns no execution
authority and makes no permission decisions itself — it answers "who is
this," not "may they."

**EventBus.** Provides one-way, best-effort publication of observability
events from a runtime subsystem to interested subscribers. It is a
broadcast mechanism, not a control channel: no subscriber's receipt of an
event authorizes any action, and no subsystem publishes in order to
trigger one. Its role is auditability and observability, not
orchestration.

**Planner Runtime.** Evaluates candidate plans against a stated goal and
produces a Plan Decision. On acceptance, a Planning Session progresses to
a Task Proposal. It proposes; it does not create a Task itself and does
not execute anything — proposal authority belongs here, but the Task
Manager retains sole authority over canonical Tasks.

**Agent Runtime.** Executes a bounded, per-Task Agent Run as a sequence of
Agent Steps. Each step's decision is supplied by an injected, replaceable
step source and, where it implies an action with external effect, is
submitted through the Execution Pipeline for authorization and execution.
Agent Runtime never authorizes its own actions and never bypasses the
Execution Pipeline.

**Memory Runtime.** Holds durable, evaluated long-term knowledge that
other subsystems may consult as context. Submission of a candidate is
evaluated against an internal, replaceable promotion decision before
becoming a retrievable record. Memory Runtime is a context provider: it
never initiates action, never subscribes to anything, and holds no
authority of its own.

**World Model Runtime.** Holds current, replaceable belief about present
reality, continuously supersedable by newer observation. Like Memory
Runtime, it is a context provider — a read source with no write path into
any other subsystem's state, no authority, and no autonomous behaviour.

---

## 3. Constitutional Principles

The following principles, already established by the Parker Constitution
and its companion documents, have now been verified against four
independently implemented runtime subsystems across multiple review
passes, with no counter-instance found. They are considered stable.

- **Owner authority.** Parker's authority ultimately rests with its
  Owner. Every subsystem provides capability; none of them owns
  authority in its own right.
- **Cognition proposes.** Any part of the system that reasons, plans, or
  infers — Planner Runtime, an Agent Step's decision source — may only
  propose action. Proposing is never the same as authorizing.
- **Trust authorises.** Only the trust layer (Permission Engine, backed
  by Identity's Principal status) converts a proposal into something
  permitted to happen.
- **Runtime executes.** Only the Execution Pipeline actually carries out
  an action with external effect. Every implemented runtime subsystem
  that can produce such an action routes it through the Execution
  Pipeline without exception.
- **Local-first.** Parker's default posture favours the Owner's own
  device and data over external dependency.
- **Trust-first.** No actor receives implicit trust. Every actor is a
  Principal and must be explicitly authorized before it may act.
- **Model independence (AD-010).** No runtime contract or seam is shaped
  around a specific reasoning or model implementation. Every policy seam
  in the runtime foundation is `suspend`-capable and model-agnostic by
  construction.
- **Context providers (AD-012).** Memory Runtime and World Model Runtime
  supply read-only reference context. Neither one acts, decides, or holds
  authority — a boundary the two independently converged on without being
  told to.
- **Replaceable policy seams.** Every decision point internal to a
  subsystem (plan decision, agent step decision, memory promotion,
  world-model update) is injected and replaceable without changing the
  subsystem's public contract or crossing its constitutional boundary.

---

## 4. Stable Engineering Workflow

Four independent implementations have now followed, or been corrected
into following, the same sequence:

```
Architecture
   |
   v
Contract Design
   |
   v
Implementation
   |
   v
Self-Traceability Review
   |
   v
Post-Implementation Review
   |
   v
Engineering Consolidation
```

Architecture, Contract Design, Implementation, Self-Traceability Review,
and Post-Implementation Review are each stable and repeatedly proven —
this is no longer a proposed process, it is an observed one, with both a
control and a treatment case in this repository's own history. Memory
Runtime and World Model Runtime each had a Contract Design stage
available from their first implementation unit and needed no correction
pass. Planner Runtime's first implementation attempt predated that stage
and required a fourth document — an explicit "alignment pass" — to
correct drift the missing stage had allowed. The difference between those
two outcomes is the evidence for why these five stages are now treated as
part of Parker itself, not merely a documentation convention: skipping a
stage has a measured cost, and following it has none.

PES-001 now codifies this directly (Stage 2A — Contract Design; the
mandatory Self-Traceability Review for Level 2/3 units), so these five
stages are no longer only a pattern observed in past units — they are a
requirement for future ones.

Engineering Consolidation is an established practice but remains
provisionally stable pending additional independent implementations,
consistent with `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md` Category
D. It has been exercised twice so far, not across the "multiple
independent implementations" each of the other five stages can point to,
and that document explicitly declines to give it the same evidentiary
confidence as Contract Design or the Post-Implementation Review. It
remains part of the workflow described here, but future contributors
should not assume it carries the same weight of evidence as the other
five stages.

---

## 5. Stable Runtime Patterns

The following implementation patterns have each been demonstrated
independently, by different units, across more than one of the six
runtime subsystems, without a rule forcing convergence:

- **Repository interfaces** (Memory Runtime) — a durable-storage-shaped
  seam for submitting and retrieving records.
- **Context providers** (Memory Runtime, World Model Runtime) — a
  read-only reference surface with no write path back into any other
  subsystem's authoritative state.
- **Command channels** (Agent Runtime) — an imperative control surface
  for directing a bounded, per-Task runtime.
- **Decision seams** (Agent Runtime's step source, Planner Runtime's plan
  decision, Memory Runtime's promotion policy, World Model Runtime's
  update policy) — internal, injected, `suspend`-capable, and
  decision-not-authority in every case, per PES-001 Chapter 7.2.
- **In-memory reference implementations** — every subsystem's first
  concrete implementation is a dependency-light, deterministic in-memory
  version, proving a contract before any persistence or production
  concern is addressed.

Naming across these patterns is deliberately not standardized. A
repository, a context provider, a command channel, and a decision seam
are genuinely different abstractions, and forcing identical naming across
them would reduce clarity, not improve it. This is a settled, withdrawn
recommendation, not an oversight.

---

## 6. Foundation Boundary

The following are now regarded as architectural foundation, not routine
implementation surface:

- The six runtime subsystems' current public contracts, as described in
  Section 2.
- The constitutional principles in Section 3, including Execution
  Pipeline authority (AD-003) and the Context Provider boundary (AD-012,
  ADR-023).
- The engineering workflow in Section 4: Architecture, Contract Design,
  Implementation, Self-Traceability Review, and Post-Implementation
  Review are stable. Engineering Consolidation is an established practice
  but remains provisionally stable pending additional independent
  implementations, consistent with `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`
  Category D.
- The runtime patterns in Section 5.

Changing any of these normally requires either an Architecture Decision
or a Contract Design revision — not a side effect of implementing a new
feature, closing a gap, or extending an existing subsystem. This mirrors
the standard already applied in `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`
(Category D): these items have survived multiple independent
implementations and repeated review, and the burden of proof for changing
them falls on the change, not on the foundation.

---

## 7. Remaining Architectural Work

The following remain open, at a high level, and are not redefined or
closed by this document. Each is recorded in full in
`docs/architecture/IMPLEMENTATION_GAPS.md`.

- The relationship between the Background Agent interface and Agent
  Runtime remains unimplemented at the code level (gap #20), by design.
- World Model event publication remains an open, disclosed gap (#47).
- Memory Runtime's promotion policy implements two of six named promotion
  factors (#46).
- Execution Pipeline cancellation cannot interrupt a Tool call already in
  flight (#44).
- Planner Runtime's `SUBMITTED → REJECTED` transition has no dedicated
  event (#45).
- Task lifecycle events publish without their full specified payload
  (#43).
- A set of items surfaced by the independent architecture audit are now
  logged for tracking, not yet resolved: deterministic ID minting capping
  multiplicity in Agent Runtime and Planner Runtime (#48); Planner
  Runtime's unresolved publisher identity (#49); EventBus's synchronous,
  non-isolated delivery (#50); and the undefined persistence/durability
  boundary across Memory, Identity, and Audit (#51).
- Several older items remain explicitly recorded as requiring a human
  decision rather than further implementation (gaps #8/#16, #35–#38,
  #41).

None of these are treated as defects in the runtime foundation described
in Sections 2–6. They are disclosed, tracked exceptions to it.

---

## 8. Next Phase

Future development should treat the runtime foundation as a fixed input
and build capability on top of it, rather than redesigning it. This
applies in particular to:

- **Execution Pipeline** — extending its capacity and integrations, not
  its authority model (AD-003 remains settled).
- **Workflow** (Chapter 38) — a new subsystem, not yet specified, that
  should follow the same Architecture → Contract Design → Implementation
  → Self-Traceability Review → Post-Implementation Review → Engineering
  Consolidation sequence described in Section 4, treating the six
  existing runtime subsystems' contracts as given.
- **Resource Registry** — extending cataloguing and visibility filtering
  on top of the existing interface, not reworking its relationship to
  Tool Registry or the Permission Engine.
- **Android integration** (Chapter 27) — a consumer of the runtime
  foundation, not a reason to change it.
- **Plugins** — extending Parker's capability surface within the existing
  trust and execution boundaries, not a reason to introduce a second
  authority path.

Any future work that finds itself needing to change something in Section
6 rather than build on it should stop and produce an Architecture
Decision or Contract Design revision first, per Section 6's own rule.

---

## 9. Conclusion

Parker has reached a stable Architecture v2.0 baseline suitable for
long-term platform development. Four runtime subsystems (Agent, Planner,
Memory, World Model), plus Identity and EventBus, have been specified,
contract-designed, implemented, tested, and reviewed — including one
read-only, skeptical, feature-agnostic independent audit that found no
constitutional violation and no defect requiring an immediate code
change. The items it did surface are disclosed, tracked, and assigned to
either an ADR/design track or ordinary follow-up, per Section 7 — they
are the normal, expected edges of a real foundation, not evidence against
one.

This document, not the full implementation history, should be the first
thing a future contributor reads before changing the platform.
