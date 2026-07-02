# Agent Interface

## Status
Version: 0.6-alpha3 (content unchanged since the v0.6 Volume 3 release;
stamped during the v0.7 Architecture Completion Phase for consistency
with other Volume 3 documents -- see
docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md §3.6).

## Purpose

An Agent is a specialised internal worker with explicit identity and permissions.

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

## Normative Requirements

- Agents MUST be Principals.
- Agents MUST NOT bypass Runtime.
- Agents MUST submit ExecutionRequests for executable work.
- Agent actions MUST be auditable.

## Related

- Chapter 14 – Agent Framework

No ADR currently exists for this interface specifically; a prior draft of
this document cited a nonexistent "ADR-004" (`docs/adr/` numbering jumps
003→006), the same defect already fixed for `ADR-005` in `EventBus.md`.
The dangling citation is removed here rather than backfilled with an
invented ADR, per the same rule applied there. Whether a dedicated ADR
should be authored for Agent-specific accountability rules is recorded in
`docs/architecture/IMPLEMENTATION_GAPS.md` as a human decision.
