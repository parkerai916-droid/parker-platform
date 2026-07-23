package parker.core.runtime

import parker.core.interfaces.IdentityService
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.OutboundParkerResponse
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningProviderResponse
import java.time.Instant

/**
 * Composes a [ReasoningProviderResponse.Reply] into an [OutboundParkerResponse].
 * Sprint 10, Unit 1, per `docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md`
 * (Stage 5 Scope Lock) and the Plan it freezes,
 * `docs/implementation/REPLY_TO_OUTBOUND_RESPONSE_IMPLEMENTATION_PLAN.md`.
 *
 * **Composition only** -- never reasons, plans, authorises, or delivers
 * (Plan Section 4d). Holds no dependency on `ReasoningProvider`,
 * `PlannerRuntime`, `PermissionEngine`, or `ResponseDelivery` -- there is
 * nothing reachable through which this class could do any of those
 * things. Whether, when, and by what component a composed
 * [OutboundParkerResponse] is ever delivered is a separate, later
 * caller's decision (`ReplyDeliveryCoordinator`, Unit 2 -- not
 * implemented here).
 *
 * **Identity resolution is scoped to the `Reply` branch only** (Scope
 * Lock Section 7): [IdentityService.resolve] is called exactly once, and
 * only at the moment an [OutboundParkerResponse] is actually about to be
 * constructed. `Goal` and `NoAction` never call `resolve` and can never
 * throw as a result -- there is nothing on those branches for an
 * operating identity to be responsible for.
 *
 * @param identityService Used only to resolve this component's own
 *   operating Principal ([RESPONSE_COMPOSER_PRINCIPAL_ID]) immediately
 *   before constructing a response. This is the only dependency this
 *   class accepts -- its absence of any other constructor parameter is
 *   itself the structural guarantee that this class cannot reach
 *   `ResponseDelivery`, `ExecutionPipeline`, `ResourceRegistry`,
 *   `ToolRegistry`, `PermissionEngine`, `PlannerRuntime`,
 *   `ReasoningProvider`, `MemoryStore`, or `WorldModel`.
 */
class ResponseComposer(
    private val identityService: IdentityService,
) {

    private companion object {
        val RESPONSE_COMPOSER_PRINCIPAL_ID = PrincipalId("system.response-composer")
    }

    /**
     * Given the [InboundOwnerMessage] that began a Turn, and the
     * [ReasoningProviderResponse] reasoning about that Turn produced:
     * - `Reply`: resolves this component's operating identity, then
     *   constructs one [OutboundParkerResponse] and returns it wrapped in
     *   [GatedOutcome.Produced].
     * - `Goal` / `NoAction`: constructs nothing, resolves no identity,
     *   and returns [GatedOutcome.NotAccepted] naming which variant was
     *   received.
     */
    suspend fun compose(
        originalMessage: InboundOwnerMessage,
        reasoningResponse: ReasoningProviderResponse,
    ): GatedOutcome<OutboundParkerResponse> {
        return when (reasoningResponse) {
            is ReasoningProviderResponse.Reply -> {
                // Identity is resolved here, and only here -- exactly at the
                // moment an OutboundParkerResponse is actually about to be
                // constructed (Scope Lock Section 7).
                val composerIdentity = identityService.resolve(RESPONSE_COMPOSER_PRINCIPAL_ID)
                    ?: throw IllegalStateException(
                        "Response Composer operating Principal " +
                            "'${RESPONSE_COMPOSER_PRINCIPAL_ID.value}' is not registered with IdentityService",
                    )

                GatedOutcome.Produced(
                    OutboundParkerResponse(
                        channelId = originalMessage.channelId,
                        senderPrincipalId = composerIdentity.principalId,
                        text = reasoningResponse.text,
                        timestamp = Instant.now(),
                        correlationId = originalMessage.correlationId,
                    ),
                )
            }
            is ReasoningProviderResponse.Goal -> GatedOutcome.NotAccepted(
                "not a Reply; reasoningResponse was Goal",
            )
            ReasoningProviderResponse.NoAction -> GatedOutcome.NotAccepted(
                "not a Reply; reasoningResponse was NoAction",
            )
        }
    }
}
