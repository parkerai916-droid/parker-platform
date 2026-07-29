# Parker Architecture History

This document records major architectural milestones in the evolution of the Parker Platform.

Unlike the Git commit history, this document records why significant architectural decisions were made and the principles they established.

---

# Architecture v1.0 — Constitutional Foundation

**Date:** 5 July 2026

## Summary

This milestone establishes the immutable constitutional principles of the Parker Platform.

From this point forward, implementation is governed by constitutional architecture rather than evolving feature-by-feature.

The Constitution becomes the highest architectural authority within Parker.

---

## Introduced

- Parker Constitution
- User Authorship & Evidence
- Reasoning Context

---

## Established Principles

- Parker owns authority. Modules provide capability.
- Cognition proposes. Trust authorises. Runtime executes.
- Three-layer knowledge architecture.
- User rights.
- Constitutional governance.
- Model-agnostic reasoning providers.
- Local-first architecture.
- Trust-first architecture.

---

## Architectural Significance

This milestone separates Parker's identity from any individual reasoning model or implementation technology.

Parker is defined by its constitutional principles rather than by the AI models it employs.

Future implementations may replace reasoning providers, plugins, runtime components and services without altering Parker's constitutional identity.

---

## Implementation Impact

From Architecture v1.0 onwards:

- All new architecture must comply with the Constitution.
- All implementation must comply with the Constitution.
- Constitutional documents require explicit architectural review before modification.
- Runtime modules provide capability only.
- Authority remains exclusively within Parker's Trust and Execution architecture.

---

## Constitutional Documents

- parker-constitution.md
- user-authorship-and-evidence.md
- reasoning-context.md
- 09-trust-framework.md

---

## Notes

This milestone marks the completion of Parker's Constitutional Foundation and the beginning of implementation under constitutional governance.

---

# Architecture v2.0 — Runtime Layer Complete

**Date:** 7 July 2026

## Summary

This milestone establishes Parker's first complete runtime layer on top
of the Architecture v1.0 constitutional foundation. Agent Runtime,
Planner Runtime, Memory Runtime, and World Model Runtime are each
specified, contract-designed, implemented, tested, and reviewed.
`docs/reviews/ARCHITECTURE_V2_BASELINE_REVIEW.md` records the
independent baseline assessment supporting this milestone: Architecture
v2.0 achieved, with minor reservations (an Agent terminology
clarification and an orientation-document refresh, both resolved by
this same Sprint 5 cleanup).

## Introduced

- Runtime Foundation (Execution Pipeline, Tool Registry, EventBus,
  Resource Registry, Identity Service — Sprint 1)
- Agent Runtime (bounded, per-Task Agent Run/Agent Step execution —
  Sprint 3, Track C)
- Planner Runtime (Plan Candidate / Plan Decision / Planning Session —
  Sprint 3, Track D)
- Memory Runtime (Sprint 4, Track A)
- World Model Runtime (Sprint 4, Track B)
- The Architecture → Contract Design → Implementation →
  Self-Traceability Review → Post-Implementation Review engineering
  workflow (PES-001 v2.1)

## Established Principles

- Contract Design is required before implementing new public runtime
  contracts (PES-001 v2.1, Stage 2A).
- A Self-Traceability Review is mandatory for Level 2/3 implementation
  units (PES-001 v2.1, Stage 9).
- Memory and the World Model are context providers, never orchestration
  systems (AD-012), with a settled shape for observability event
  publication (ADR-023) that does not grant either subsystem autonomous
  authority.
- Policy seams (`AgentStepSource`, `PlanDecision`,
  `MemoryPromotionPolicy`, `WorldModelUpdatePolicy`) are internal,
  injected, `suspend`-capable decision providers, never authorities
  (PES-001 v2.1, Chapter 7.2).
- Core runtime foundations — Execution Pipeline authority, the Context
  Provider boundary, the policy seam pattern, and the engineering
  workflow itself — are now treated as stable platform law, changeable
  only through a deliberate, evidenced Architecture Decision
  (`docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md` Category D).

## Architectural Significance

This milestone separates Parker's runtime behaviour from any single
implementation attempt: four independently-built subsystems converged
on the same policy-seam shape and the same "one interface, no wrapper"
boundary without being told to, before PES-001 named the pattern
explicitly. Parker's runtime identity is now demonstrated by repetition
across subsystems, not asserted by a single one of them.

## Known Reservations (open at the time of this milestone)

- World Model event publication (`IMPLEMENTATION_GAPS.md` #47) remains
  open; ADR-023 settles its architectural shape without implementing
  it.
- A background, long-lived Agent concept
  (`docs/specifications/volume-03-core-interfaces/Agent.md`, "Background
  Agent Interface") remains specified but unimplemented and excluded
  from compilation; Agent Runtime does not instantiate or depend on it.

## Related Documents

- `docs/reviews/ARCHITECTURE_V2_BASELINE_REVIEW.md`
- `docs/reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md`
- `docs/reviews/SPRINT_4_ENGINEERING_CONSOLIDATION.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `docs/adr/ADR-023-context-provider-event-publication.md`

## Notes

This milestone does not supersede or alter any principle established by
Architecture v1.0. It records what was built on top of it.

---

# Constitutional Foundation Extension — Epistemic Integrity

**Date:** 29 July 2026

## Summary

This milestone ratifies Constitutional Amendment No. 1 — Epistemic
Integrity as the fourth document of Parker's Constitutional Foundation,
alongside the Parker Constitution, User Authorship and Evidence, and
Reasoning Context established at Architecture v1.0. It is recorded as
its own milestone, rather than folded into the 5 July 2026 entry above,
so that the historical record of what was introduced at v1.0 is not
rewritten.

This entry deliberately does not claim a new "Architecture vX.Y" number.
It extends the existing Constitutional Foundation rather than
introducing a new runtime or architecture layer, and any renumbering of
the platform's own architecture-version track remains a decision for
the maintainers.

## Introduced

- Epistemic Integrity (`docs/architecture/epistemic-integrity.md`),
  Constitutional Amendment No. 1, Version 1.0 — Ratified: 19 Articles
  and 69 Constitutional Tests governing how Parker represents
  knowledge.

## Established Principles

- Representation rather than absolute truth: Parker communicates the
  best-supported understanding the evidence justifies, not a claim of
  absolute truth.
- A constitutional separation between reasoning and representation:
  reasoning providers propose; the Constitution governs what may be
  claimed.
- A fourteen-state evidential taxonomy (from direct observation to
  indeterminate), with constitutional criteria distinguishing adjacent
  states.
- Propositional Integrity: a proposition's framing, assumptions, and
  structure must be examined before evidence is evaluated for or
  against it.
- A narrowly scoped Burden of Justification: existence, precedence, and
  the absence of a contradicting account do not, by themselves,
  discharge a proposition's burden of justification.
- Evidential integrity as distinct from provenance: original evidence
  must be distinguished from derivative evidence, and authenticity must
  be distinguished from truth of content.
- Negative evidence: an unexplained absence of expected evidence may be
  relevant, but is never proof, and requires disclosure of what was
  expected and what alternative explanations were considered.
- Temporal integrity and revision: Parker's understanding may be
  revised as new evidence arrives, without rewriting the historical
  record of what was known when an earlier conclusion was reached.

## Review Process

This amendment underwent four drafting rounds and one independent
adversarial constitutional audit before ratification:

1. Initial drafting (Version 0.1).
2. Constitutional review and expansion (Version 0.2).
3. Constitutional review and narrowed doctrine (Version 0.3).
4. Independent adversarial constitutional audit of Version 0.3 —
   twelve findings identified across materiality, evidential-state
   definition, interpretive hierarchy, disclosure obligations, test
   coverage, and terminology consistency.
5. Narrow hardening amendment resolving all twelve findings, with no
   new constitutional doctrine introduced (Version 0.4).
6. Ratification as Version 1.0.

Full drafting and audit history is preserved in `docs/reviews/`:
`EPISTEMIC_INTEGRITY_DRAFT_V0_1.md` through `_V0_3.md`,
`EPISTEMIC_INTEGRITY_CONSTITUTIONAL_AUDIT_V0_3.md`,
`EPISTEMIC_INTEGRITY_V0_4_RATIFICATION_SOURCE.md`, the
`EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REGISTER.md`, and the
`EPISTEMIC_INTEGRITY_RATIFICATION_RECORD.md`.

## Architectural Significance

This milestone extends Parker's constitutional discipline from
authority (v1.0's original scope) to knowledge: the same principle that
no intelligence within Parker holds unchecked authority now has a
counterpart for what Parker is permitted to claim to know. Capability,
persuasiveness, official status, and repetition are each explicitly
denied as substitutes for evidence.

## Implementation Impact

From this milestone onwards:

- All reasoning providers, memory systems, retrieval engines, document
  processors, world models, workflows, agents, tools, and plugins must
  comply with Epistemic Integrity's 19 Articles.
- Future capabilities inherit these obligations by function (storing,
  retrieving, transforming, or representing a material proposition),
  not merely by being named in Article XIX's illustrative list.
- `docs/architecture/epistemic-integrity.md` is constitutionally frozen;
  future changes require a formal constitutional amendment, following
  a review process of the kind recorded in
  `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REGISTER.md`.

## Constitutional Documents

- parker-constitution.md
- user-authorship-and-evidence.md
- reasoning-context.md
- epistemic-integrity.md

## Notes

This milestone does not supersede or alter any principle established by
Architecture v1.0 or v2.0. It adds a fourth constitutional document to
the Constitutional Foundation established at v1.0.
