# PublishResult Contract (Supporting Type)

## Status
Version: 0.7-alpha
Status: New — closes IMPLEMENTATION_GAPS.md / consistency review §2.2
(v0.7 Architecture Completion Phase, Priority 3).

## Purpose
The return type of `EventBus.publish(event: ParkerEvent): PublishResult`
(see `EventBus.md`). Reports the outcome of a publish attempt.

## Shape
A sealed outcome type, following the same success/failure-with-reason
pattern already established by `ValidationResult`
(`src/contracts/ToolDescriptor.kt`):

```kotlin
sealed class PublishResult {
    data class Delivered(val deliveredCount: Int) : PublishResult()
    data class Rejected(val reason: String) : PublishResult()
    data class PartialFailure(
        val deliveredCount: Int,
        val failedSubscriptionIds: List<String>,
    ) : PublishResult()
}
```

## Normative Requirements
- `Rejected` MUST be returned (never a thrown exception) when an event
  fails **authentication** (see `EventBus.md` "Authentication") — e.g. the
  publisher's Principal cannot be established, or a required `signature`
  does not verify. A rejected event MUST NOT reach any subscriber.
- `PartialFailure` MUST be returned when authentication succeeds and at
  least one subscriber's `EventHandler` threw during delivery, per
  `EventHandler.md`'s isolation requirement — the publish itself still
  succeeded; only specific deliveries failed.
- `Delivered` with `deliveredCount == 0` is valid and not an error — it
  means the event was authenticated and accepted but no subscriber was
  currently listening for its `EventType`.
- `PublishResult` MUST NOT itself carry subscriber-identifying information
  beyond `failedSubscriptionIds` needed for diagnostics — full subscriber
  identity is not the publisher's concern (mirrors the Permission
  Engine's scoped-visibility principle applied to the Event Bus).

## Open Questions (not resolved by this entry)
- Whether `Rejected` should distinguish authentication failure from
  schema-validation failure (payload not matching `Event.schema.json`)
  with different reason codes, or a single free-text `reason` suffices
  for v0.7.

## Related
- EventBus.md
- EventType.md
- EventHandler.md
- ValidationResult (`src/contracts/ToolDescriptor.kt`) — precedent pattern
