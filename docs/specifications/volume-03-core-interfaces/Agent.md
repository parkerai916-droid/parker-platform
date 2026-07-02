# Agent Interface

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
- ADR-004
