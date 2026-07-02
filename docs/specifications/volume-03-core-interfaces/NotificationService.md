# NotificationService Interface

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
