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
- *No ADR currently exists for event authentication requirements.* This section
  previously cited "ADR-005", which does not exist in `docs/adr/` (ADR
  numbering jumps from 003 to 006). Per IMPLEMENTATION_GAPS.md #4, the
  dangling citation has been removed rather than backfilled with an
  invented ADR. If the normative requirements above need ADR-level backing,
  a new ADR should be authored and referenced here explicitly.
