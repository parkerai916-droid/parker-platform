package parker.core.interfaces

interface EventBus {
    suspend fun publish(event: ParkerEvent): PublishResult

    /**
     * @param subscriberPrincipalId the Principal this subscription is created on behalf of.
     *   Added by the targeted refinement pass (IMPLEMENTATION_GAPS.md #27):
     *   previously there was no way for a caller to assert real subscriber
     *   identity, so implementations had to guess or use a placeholder.
     *   This parameter only makes identity explicit -- it does not itself
     *   authenticate or authorise the caller (no IdentityService
     *   integration or authentication policy is implied or implemented by
     *   this change).
     */
    fun subscribe(eventType: EventType, subscriberPrincipalId: PrincipalId, handler: EventHandler): Subscription
}
