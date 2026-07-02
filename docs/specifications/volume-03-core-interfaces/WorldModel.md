# WorldModel Interface

## Purpose

The WorldModel maintains Parker's current understanding of reality.

## Responsibilities

- Store transient state
- Track confidence
- Expire stale observations
- Resolve current state
- Publish state change events

## Required Operations

```kotlin
interface WorldModel {
    suspend fun observe(observation: WorldObservation): ObservationResult
    suspend fun current(resourceId: ResourceId): WorldState?
    suspend fun query(query: WorldQuery): List<WorldState>
}
```

## Normative Requirements

- World state MUST be timestamped.
- World state MUST have confidence.
- Stale state MUST expire or degrade.
- World Model MUST NOT become Memory.

## Related

- Chapter 16 – World Model
