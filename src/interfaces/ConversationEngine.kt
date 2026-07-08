package parker.core.interfaces

import java.time.Instant

/**
 * Conversation Engine contracts (Sprint 7, Stage 3 Implementation Unit),
 * implementing exactly the shapes approved by
 * `docs/architecture/CONVERSATION_ENGINE_CONTRACT_DESIGN.md` ("Conversation
 * Engine Contract Design"), itself built on
 * `docs/architecture/19-conversation-engine.md` ("Conversation Engine
 * Architecture"). No concept here is new: every type below is cited,
 * section by section, in Contract Design's own Section 2 (Core Contract
 * Types) and Section 1 (Public Conversation Engine Contract). This file
 * does not redesign, extend, or reinterpret either document -- it is
 * their literal Kotlin realisation.
 *
 * **Scope, restated from Contract Design's own Conclusion.** This unit
 * implements the *inbound half* only: binding an already-`CommunicationIntake`-
 * accepted [InboundOwnerMessage] into a [Conversation]/[Turn]. It does not
 * engage a reasoning provider, does not construct a `PlanningRequest`, and
 * does not deliver an `OutboundParkerResponse` -- Contract Design Section 8
 * deferred all three in full, pending the reasoning-provider contract
 * (`docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`, see
 * `ReasoningProvider.kt`) and Response Delivery, respectively.
 */

/**
 * Contract Design Section 2: a single, non-blank string value, matching
 * [PrincipalId]/[ModuleId]/[CorrelationId]'s identical established shape.
 * Distinct from [CorrelationId] (Architecture Section 13 Item 3, restated
 * in Contract Design Section 2): `CorrelationId` ties one inbound message
 * to whatever eventually answers it, scoped to a single [Turn];
 * `ConversationId` groups many Turns -- each still carrying its own
 * `CorrelationId` -- under one continuing exchange.
 */
@JvmInline
value class ConversationId(val value: String) {
    init {
        require(value.isNotBlank()) { "ConversationId must not be blank" }
    }
}

/**
 * Contract Design Section 2: the one genuinely new identifier a [Turn]
 * requires, same shape as [ConversationId].
 */
@JvmInline
value class TurnId(val value: String) {
    init {
        require(value.isNotBlank()) { "TurnId must not be blank" }
    }
}

/**
 * Contract Design Section 2: the continuity record Architecture Section 4
 * names as the Conversation Engine's sole owned state.
 *
 * @param conversationId This Conversation's own identity.
 * @param ownerPrincipalId The owner whose [InboundOwnerMessage.senderPrincipalId]
 *   began this Conversation -- never the Conversation Engine's own operating
 *   identity (Architecture Section 5).
 * @param channelId Which Communication Channel this Conversation is, so
 *   far, associated with -- a plain [ModuleId], mirroring
 *   `COMMUNICATION_CONTRACT_DESIGN.md` Section 1's identical "no separate
 *   `ChannelId` type" precedent. Whether a Conversation may span more than
 *   one channel (Architecture Section 13 Item 5) is not decided here.
 * @param turnIds Which Turns belong to this Conversation, restating
 *   Architecture Section 4's own ownership definition verbatim.
 */
data class Conversation(
    val conversationId: ConversationId,
    val ownerPrincipalId: PrincipalId,
    val channelId: ModuleId,
    val turnIds: List<TurnId>,
)

/**
 * Contract Design Section 2: the bounded per-message record Architecture
 * Section 6 names. One Turn, one [InboundOwnerMessage].
 *
 * @param turnId This Turn's own identity.
 * @param conversationId Which Conversation this Turn belongs to.
 * @param message The inbound message this Turn was created for, reused
 *   unchanged.
 * @param receivedAt When this Turn was created -- not
 *   [InboundOwnerMessage.timestamp], which records when the owner sent the
 *   message, not when it was processed.
 */
data class Turn(
    val turnId: TurnId,
    val conversationId: ConversationId,
    val message: InboundOwnerMessage,
    val receivedAt: Instant,
)

/**
 * Contract Design Section 2: the outcome of [ConversationEngine]'s one
 * operation, kept as a **plain data class, not a sealed type** -- unlike
 * [CommunicationIntakeDisposition]'s two-variant sealed shape. No
 * `Rejected` variant exists because `submitTurn`'s only precondition is an
 * already-`CommunicationIntake`-accepted message; Architecture Section 11
 * treats correlation failure or ambiguity as non-blocking, resolved by
 * opening a new Conversation, not a rejectable error.
 *
 * @param conversation The resulting Conversation, whether newly created or
 *   continued, in its state immediately after this Turn was bound.
 * @param turn The newly created Turn.
 * @param isNewConversation Whether this call began a new Conversation or
 *   continued an existing one -- intrinsic, not transient: the caller
 *   supplies no [ConversationId] on the way in, so this is the only way to
 *   observe which branch occurred without duplicating this engine's own
 *   owned lookup outside it.
 */
data class ConversationDisposition(
    val conversation: Conversation,
    val turn: Turn,
    val isNewConversation: Boolean,
)

/**
 * Contract Design Section 1: the single public interface for Turn
 * consumption and Conversation continuity binding, mirroring
 * [CommunicationIntake]'s exact shape and minimalism (one method, one
 * payload type, one outcome type).
 *
 * Given an [InboundOwnerMessage] already accepted by [CommunicationIntake],
 * an implementation determines whether it continues an existing
 * Conversation or begins a new one, creates a [Turn] bound to that
 * Conversation, and returns a [ConversationDisposition] describing the
 * result.
 *
 * **What an implementation must not do (Contract Design Section 1):**
 * engage a reasoning provider; construct a `PlanningRequest`; construct an
 * `OutboundParkerResponse`; call `PlannerRuntime`, `ExecutionPipeline`,
 * `PermissionEngine`, `MemoryStore`, or `WorldModel`; repeat, second-guess,
 * or bypass [CommunicationIntake]'s own two structural checks.
 */
interface ConversationEngine {
    suspend fun submitTurn(message: InboundOwnerMessage): ConversationDisposition
}
