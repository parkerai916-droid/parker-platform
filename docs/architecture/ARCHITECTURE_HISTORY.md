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
