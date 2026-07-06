# Parker Platform Architecture v0.4

## Project History

- [ARCHITECTURE_HISTORY.md](ARCHITECTURE_HISTORY.md) — why major architectural and constitutional milestones were made, not just what changed.

## Constitutional Foundation

These documents sit above the numbered chapters below and take precedence over them. They define Parker's philosophy and behavioural contracts rather than implementation detail, and every chapter that follows is a specialization of what they establish.

1. [Parker Constitution](parker-constitution.md) — Parker's highest-level, immutable values and architectural constraints.
2. [User Authorship and Evidence](user-authorship-and-evidence.md) — how Parker assists communication without fabricating evidence or rewriting the user's account.
3. [Reasoning Context](reasoning-context.md) — the three knowledge layers (Memory, World Model, Reasoning Context) and how information flows between them.

Architecture v1.0 (Constitutional Foundation) is established by the constitutional documents: parker-constitution.md, user-authorship-and-evidence.md, reasoning-context.md, and the existing trust model. The wider numbered architecture set remains on its existing version track until a separate versioning pass updates it.

This folder contains Chapters 1-20 of the Parker Platform Architecture working draft.

## Runtime Architecture (Sprint 1–4)

Architecture v2.0 -- the runtime layer built on top of the
Constitutional Foundation above -- is achieved, with minor
reservations, per
[ARCHITECTURE_V2_BASELINE_REVIEW.md](../reviews/ARCHITECTURE_V2_BASELINE_REVIEW.md).
Four runtime subsystems are specified, contract-designed, implemented,
tested, and reviewed:

- **Agent Runtime** — bounded, per-Task Agent Run/Agent Step execution.
  [AgentRuntimeSpecification.md](../specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md),
  [MULTI_STEP_AGENT_RUN_DESIGN.md](MULTI_STEP_AGENT_RUN_DESIGN.md).
- **Planner Runtime** — Plan Candidate generation, Plan Decision, and
  Planning Session progression to a Task Proposal.
  [PLANNER_RUNTIME_PROGRESSION_DESIGN.md](PLANNER_RUNTIME_PROGRESSION_DESIGN.md),
  [PLANNER_RUNTIME_CONTRACT_DESIGN.md](PLANNER_RUNTIME_CONTRACT_DESIGN.md).
- **Memory Runtime** — durable, evaluated long-term knowledge.
  [MEMORY_RUNTIME_ARCHITECTURE.md](MEMORY_RUNTIME_ARCHITECTURE.md),
  [MEMORY_CONTRACT_DESIGN.md](MEMORY_CONTRACT_DESIGN.md).
- **World Model Runtime** — current, replaceable belief about present
  reality.
  [WORLD_MODEL_RUNTIME_ARCHITECTURE.md](WORLD_MODEL_RUNTIME_ARCHITECTURE.md),
  [WORLD_MODEL_CONTRACT_DESIGN.md](WORLD_MODEL_CONTRACT_DESIGN.md).

All four are governed by the same engineering workflow: Architecture →
Contract Design → Implementation → Self-Traceability Review →
Post-Implementation Review, per
[PARKER_ENGINEERING_STANDARD.md](PARKER_ENGINEERING_STANDARD.md)
(PES-001). Core runtime foundations reached by this point — Execution
Pipeline authority, the Context Provider boundary, the policy seam
pattern, and the engineering workflow itself — are now treated as
stable platform law; see
[SPRINT_4_ARCHITECTURE_ACTIONS.md](../reviews/SPRINT_4_ARCHITECTURE_ACTIONS.md)
Category D for the evidence this rests on. Future work should treat
these as changeable only through a deliberate, evidenced Architecture
Decision, not as routine implementation work.

**Not yet specified:** Workflow Runtime (Chapter 38), Android
Integration (Chapter 27), and a background, long-lived Agent concept
distinct from Agent Runtime (see
[Agent.md](../specifications/volume-03-core-interfaces/Agent.md)'s own
Status section for that distinction). World Model event publication
(`IMPLEMENTATION_GAPS.md` #47) remains an open, disclosed gap, not a
closed one.

`docs/implementation/IMPLEMENTATION_HISTORY.md` is the authoritative,
unit-by-unit implementation record; this index is an orientation
pointer, not a substitute for it.
