# EventBus Interface

## Purpose

The EventBus provides structured internal communication between Parker services.

## Responsibilities

- Publish authenticated events
- Route events to subscribers
- Preserve correlation IDs
- Reject unauthenticated publishers
- Support diagnostic observability

## Required Operations

```kotlin
interface EventBus {
    suspend fun publish(event: ParkerEvent): PublishResult
    fun subscribe(eventType: EventType, handler: EventHandler): Subscription
}
```

## Normative Requirements

- Events MUST identify publisher Principal.
- Unauthenticated events MUST NOT influence Memory, World Model, Execution or Trust.
- Event payloads SHOULD be schema validated.

## Related

- Chapter 13 – Event Bus
- ADR-005
