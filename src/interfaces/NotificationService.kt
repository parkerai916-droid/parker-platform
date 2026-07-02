package parker.core.interfaces

interface NotificationService {
    suspend fun notify(notification: ParkerNotification): NotificationResult
    suspend fun cancel(notificationId: NotificationId): CancellationResult
}
