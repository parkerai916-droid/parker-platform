package parker.core.runtime

import parker.core.interfaces.Conversation
import parker.core.interfaces.ConversationDisposition
import parker.core.interfaces.ConversationEngine
import parker.core.interfaces.ConversationId
import parker.core.interfaces.IdentityService
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import java.time.Instant
import java.util.UUID

/**
 * The in-memory [ConversationEngine] implementation for Sprint 7, Stage 3
 * Implementation Unit, per `docs/implementation/CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md`.
 *
 * **Required Implementation Decision 1** (accepted at Scope Lock): every
 * inbound Turn begins a new Conversation. This first unit does not attempt
 * conversation-continuation recognition -- there is no stored state to
 * consult, and [ConversationDisposition.isNewConversation] is therefore
 * always `true`. This is a conservative, explicit default, not an
 * oversight: continuation recognition is deferred, in full, to a later
 * unit.
 *
 * Because every Turn begins a new Conversation, this implementation is
 * **deliberately stateless** -- it holds no internal mutable state, no
 * `Mutex`, and no stored map of prior Conversations (unlike, for example,
 * `InMemoryCommunicationIntake`'s own stored-message pattern). Adding such
 * storage now would exceed this unit's scope: the Implementation Plan's
 * Out-of-Scope list explicitly excludes persistence, and no lookup is
 * needed while Decision 1 holds.
 *
 * **Required Implementation Decision 3** (accepted at Scope Lock): this
 * engine's own operating identity is `system.conversation-engine`,
 * mirroring the `PLANNER_RUNTIME_PRINCIPAL_ID` / `TASK_MANAGER_RUNTIME_PRINCIPAL_ID`
 * precedent set by `InMemoryPlannerRuntime` / `InMemoryTaskManagerRuntime`.
 * This engine resolves its own operating identity before acting, and never
 * substitutes the inbound message's sender Principal for it.
 *
 * @param identityService Used only to resolve this engine's own operating
 *   Principal before acting. This is the only dependency this
 *   implementation accepts -- its absence of any other constructor
 *   parameter is itself the structural guarantee that this engine cannot
 *   reach `PlannerRuntime`, `ExecutionPipeline`, `MemoryStore`, `WorldModel`,
 *   or any reasoning provider.
 */
class InMemoryConversationEngine(
    private val identityService: IdentityService,
) : ConversationEngine {

    private companion object {
        val CONVERSATION_ENGINE_PRINCIPAL_ID = PrincipalId("system.conversation-engine")
    }

    override suspend fun submitTurn(message: InboundOwnerMessage): ConversationDisposition {
        identityService.resolve(CONVERSATION_ENGINE_PRINCIPAL_ID)
            ?: throw IllegalStateException(
                "Conversation Engine operating Principal " +
                    "'${CONVERSATION_ENGINE_PRINCIPAL_ID.value}' is not registered with IdentityService"
            )

        val conversationId = ConversationId(UUID.randomUUID().toString())
        val turnId = TurnId(UUID.randomUUID().toString())

        val turn = Turn(
            turnId = turnId,
            conversationId = conversationId,
            message = message,
            receivedAt = Instant.now(),
        )

        val conversation = Conversation(
            conversationId = conversationId,
            ownerPrincipalId = message.senderPrincipalId,
            channelId = message.channelId,
            turnIds = listOf(turnId),
        )

        return ConversationDisposition(
            conversation = conversation,
            turn = turn,
            isNewConversation = true,
        )
    }
}
