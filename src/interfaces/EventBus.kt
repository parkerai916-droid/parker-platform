package parker.core.interfaces

interface EventBus {
    suspend fun publish(event: ParkerEvent): PublishResult
    fun subscribe(eventType: EventType, handler: EventHandler): Subscription
}
