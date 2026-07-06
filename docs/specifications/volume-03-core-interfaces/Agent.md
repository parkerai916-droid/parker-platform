# Background Agent Interface

## Status
Version: 0.6-alpha3 (content unchanged since the v0.6 Volume 3 release;
stamped during the v0.7 Architecture Completion Phase for consistency
with other Volume 3 documents -- see
docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md §3.6).

**Sprint 5 terminology clarification.** `docs/reviews/ARCHITECTURE_V2_BASELINE_REVIEW.md`
found that "Agent" had come to name two unrelated concepts in this
repository: the long-lived, daemon-style worker this document describes
(`start()`/`stop()`/`health()`), and the bounded, per-Task execution
model Sprint 3, Track C actually specified and implemented as "Agent
Runtime" (`AgentRun`, `AgentRunCommandChannel`, `AgentStep`,
`InMemoryAgentRuntime`). This document is retitled **Background Agent
Interface** to make that distinction unmistakable in prose. The Kotlin
interface name itself is unchanged -- `src/interfaces/Agent.kt` still
declares `interface Agent`, and this clarification renames no code.

**This interface is not the thing Agent Runtime executes.** Agent
Runtime does not instantiate, hold, start, stop, or health-check an
`Agent`. A repository-wide search confirms `src/interfaces/Agent.kt` is
referenced nowhere in `src/runtime/InMemoryAgentRuntime.kt` or anywhere
else in `src/`. `Agent.kt` remains an excluded, later-phase interface
stub (`build.gradle.kts`; `AgentHealth` is still undefined, per
`docs/architecture/IMPLEMENTATION_GAPS.md` #20) unless and until a
future unit implements background agents. It is not included in build
scope by this clarification, and no future connection between this
interface and Agent Runtime is authorised by this document -- any such
connection would require its own explicit Architecture Decision or
Contract Design pass, per PES-001 Stage 2A.

## Purpose

A Background Agent is a specialised internal worker with explicit
identity and permissions, distinct from the bounded, per-Task Agent Run
that Agent Runtime executes (see `AgentRuntimeSpecification.md`'s own
terminology clarification for the corresponding note from that side).

## Responsibilities

- Perform background work
- React to events
- Maintain limited operational state
- Submit ExecutionRequests when action is required

## Required Operations

```kotlin
interface Agent {
    val principalId: PrincipalId
    suspend fun start()
    suspend fun stop()
    suspend fun health(): AgentHealth
}
```

**`AgentHealth` is currently undefined.** No document specifies its
shape, and `src/interfaces/Agent.kt` is excluded from compilation
(`build.gradle.kts`) until the Agent Framework phase defines it. Recorded
in `docs/architecture/IMPLEMENTATION_GAPS.md` #20; not implemented by the
targeted refinement pass, which is documentation-only for this item.

## Normative Requirements

- Agents MUST be Principals.
- Agents MUST NOT bypass Runtime.
- Agents MUST submit ExecutionRequests for executable work.
- Agent actions MUST be auditable.

## Related

- Chapter 14 – Agent Framework
- `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
  -- the bounded, per-Task Agent Run/Agent Step execution model
  ("Agent Runtime"), a distinct concept from the Background Agent this
  document describes; see that document's own terminology
  clarification.
- `docs/reviews/ARCHITECTURE_V2_BASELINE_REVIEW.md` -- records the
  finding that prompted this clarification.

No ADR currently exists for this interface specifically; a prior draft of
this document cited a nonexistent "ADR-004" (`docs/adr/` numbering jumps
003→006), the same defect already fixed for `ADR-005` in `EventBus.md`.
The dangling citation is removed here rather than backfilled with an
invented ADR, per the same rule applied there. Whether a dedicated ADR
should be authored for Agent-specific accountability rules is recorded in
`docs/architecture/IMPLEMENTATION_GAPS.md` as a human decision.
