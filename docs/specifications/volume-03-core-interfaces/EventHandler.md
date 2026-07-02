# EventHandler Contract (Supporting Type)

## Status
Version: 0.7-alpha
Status: New — closes IMPLEMENTATION_GAPS.md / consistency review §2.2
(v0.7 Architecture Completion Phase, Priority 3).

## Purpose
The type of `EventBus.subscribe(eventType: EventType, handler:
EventHandler): Subscription`'s second parameter (see `EventBus.md`).
Represents the subscriber-supplied callback invoked when a matching event
is published.

## Shape
A single-method functional contract:

```kotlin
fun interface EventHandler {
    suspend fun handle(event: ParkerEvent): Unit
}
```

## Normative Requirements
- `EventHandler.handle` MUST NOT be relied upon to return a value that
  affects delivery — the Event Bus is fire-and-forget from the
  publisher's perspective (see `EventBus.md` "Delivery Guarantees"); a
  handler cannot reject or transform an event after the fact.
- A handler that throws MUST have its exception caught and isolated by
  the Event Bus (see `EventBus.md` "Failure Handling") — one failing
  subscriber MUST NOT prevent delivery to other subscribers of the same
  event, and MUST NOT propagate back to the publisher of `publish()`.
- A handler MUST NOT be assumed to run on any particular thread or
  dispatcher; handlers requiring a specific execution context are
  responsible for their own dispatch internally.
- A handler MUST NOT itself become a parallel execution path (ADR-003):
  if handling an event requires performing further work with side
  effects, the handler MUST submit a new `ExecutionRequest` through the
  Execution Pipeline rather than acting directly.

## Open Questions (not resolved by this entry)
- Whether handlers need a declared timeout, after which the Event Bus
  treats them as failed for that delivery (mirrors
  `ExecutionRequest.timeoutMillis` precedent from the runtime prototype,
  but not yet specified for events).

## Related
- EventBus.md
- ADR-003 – Single Execution Pipeline
- EventType.md
- Subscription.md
