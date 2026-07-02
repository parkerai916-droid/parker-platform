# Subscription Contract (Supporting Type)

## Status
Version: 0.7-alpha
Status: New — closes IMPLEMENTATION_GAPS.md / consistency review §2.2
(v0.7 Architecture Completion Phase, Priority 3).

## Purpose
The return type of `EventBus.subscribe(eventType, handler): Subscription`
(see `EventBus.md`). Represents a live, cancellable subscription.

## Required Fields
- subscriptionId
- eventType (the `EventType` this subscription matches)
- subscriberPrincipalId (the Principal that owns this subscription — every
  subscriber MUST be a Principal, consistent with Chapter 41's "every
  action has a principal")
- active (whether the subscription is currently receiving deliveries)

## Required Operations
```kotlin
interface Subscription {
    val subscriptionId: String
    val eventType: EventType
    val subscriberPrincipalId: PrincipalId
    val active: Boolean
    suspend fun cancel(): Unit
}
```

## Normative Requirements
- `cancel()` MUST be idempotent — cancelling an already-cancelled
  Subscription is a no-op, not an error (mirrors `CancellationResult`'s
  established pattern for `ExecutionPipeline.cancel`).
- Once cancelled, a Subscription MUST NOT receive further deliveries, even
  if an event was already in-flight to it at the moment of cancellation
  (best-effort: in-flight delivery may still complete, but no new
  delivery may begin).
- A Subscription's `subscriberPrincipalId` MUST be checked against the
  Principal's continued validity (e.g. a Revoked Principal, per
  `docs/diagrams/principal-lifecycle-state-machine.mmd`) — the Event Bus
  MUST cancel all Subscriptions owned by a Principal that transitions to
  `Revoked` or `Archived`, rather than continuing to deliver to a
  subscriber that no longer exists in good standing.

## Open Questions (not resolved by this entry)
- Whether Subscriptions should be individually auditable (Chapter 43) at
  the same granularity as `PermissionDecision`s, or only in aggregate.

## Related
- EventBus.md
- EventType.md
- EventHandler.md
- CancellationResult.md (established idempotent-cancellation precedent)
- docs/diagrams/principal-lifecycle-state-machine.mmd
