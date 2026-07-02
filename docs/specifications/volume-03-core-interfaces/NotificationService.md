# NotificationService Interface

## Status
Version: 0.6-alpha3 (content unchanged since the v0.6 Volume 3 release;
stamped during the v0.7 Architecture Completion Phase for consistency
with other Volume 3 documents -- see
docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md §3.6).

## Purpose

The NotificationService communicates with the user through approved channels.

## Responsibilities

- Deliver notifications
- Respect priority
- Apply quiet hours
- Protect sensitive content
- Route to appropriate channels

## Required Operations

```kotlin
interface NotificationService {
    suspend fun notify(notification: ParkerNotification): NotificationResult
    suspend fun cancel(notificationId: NotificationId): CancellationResult
}
```

## Normative Requirements

- Sensitive notifications MUST respect privacy policy.
- Critical alerts MAY bypass quiet hours only under policy.
- Notifications requiring action MUST be traceable to source.

## Related

- Chapter 25 – Notification Framework
