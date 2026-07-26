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
 * Contract Design Section 2: the outcome of [ConversationEngine.submitTurn],
 * kept as a **plain data class, not a sealed type** -- unlike
 * [CommunicationIntakeDisposition]'s two-variant sealed shape.
 *
 * @param conversation The resulting Conversation, whether newly created or
 *   continued, in its state immediately after this Turn was bound.
 * @param turn The newly created Turn.
 * @param isNewConversation Whether this call bound the first Turn ever
 *   recorded for [conversation]'s own [Conversation.conversationId], or
 *   appended to one that already had at least one. **Revised
 *   (`CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` Section 5, Contract
 *   Authority and Propagation Correction):** the caller now *does* supply a
 *   [ConversationId] on the way in (resolved beforehand by
 *   [ConversationEngine.resolveConversationId]) -- this field no longer
 *   exists because the caller supplies none; it exists because the caller
 *   is not told, and must not decide for itself, whether that identifier
 *   already had a Conversation record. [submitTurn] alone determines this,
 *   from its own owned state, exactly as before.
 */
data class ConversationDisposition(
    val conversation: Conversation,
    val turn: Turn,
    val isNewConversation: Boolean,
)

/**
 * Contract Design Section 1; revised by
 * `docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md`
 * ("the Continuity Contract Design", Sprint 11 Unit 5) Sections 4-6. The
 * single public interface for conversation continuity recognition and Turn
 * binding. **Two operations, not one** -- an additive extension of this
 * interface's original single-operation shape, made because Question 2 of
 * the Continuity Contract Design (making a `ConversationId` available
 * before a Turn exists) cannot be answered without exposing continuity
 * recognition as its own, separately-callable step. Both operations remain
 * exclusively owned by, and implemented by, [ConversationEngine] -- this
 * is an additive extension of *how many operations* the same, single owner
 * exposes to reach its one, unchanged responsibility (Continuity Contract
 * Design Section 6), not a reassignment of that responsibility, and not a
 * redesign of it.
 *
 * **[resolveConversationId] -- the sole authoritative continuity
 * decision.** Given an [InboundOwnerMessage], determines the continuity
 * key (`message.channelId`, `message.senderPrincipalId` -- Continuity
 * Contract Design Section 3) and returns the [ConversationId] of whichever
 * Conversation is currently open for that key, minting a new one if none
 * is open. This is a **stateful, authoritative operation, not a passive
 * lookup** (Continuity Contract Design Section 5.1's naming caution) --
 * calling it may register a continuity key as newly open, an observable
 * change to this engine's own state, even though it also returns a value.
 *
 * Binding guarantees (Continuity Contract Design Section 5.1), required of
 * every implementation, not merely encouraged:
 *
 * 1. **Atomic.** Two concurrent calls for the same continuity key must
 *    never each mint a distinct [ConversationId] -- at most one is ever
 *    active per key.
 * 2. **Idempotent while active.** Repeated calls for the same,
 *    still-open continuity key return the identical [ConversationId]
 *    every time.
 *
 * **[submitTurn] -- consumes, never re-decides, the resolved identity.**
 * Given an [InboundOwnerMessage] and the exact [ConversationId]
 * [resolveConversationId] already produced for it, creates a [Turn] bound
 * to that identifier and returns a [ConversationDisposition]. Guarantee 3
 * (Continuity Contract Design Section 5.1): this method must not invoke
 * the continuity rule, consult continuity-key state, or compute or
 * substitute any identifier other than the one supplied -- its only
 * permitted responses to [conversationId] are to accept it (when it is the
 * active identifier for `message`'s own continuity key) or reject it
 * (when it is not -- see [IllegalArgumentException], thrown, never
 * silently repaired). Guarantee 4: rejection here prevents every
 * downstream effect (no Turn recorded, no reasoning, no delivery),
 * propagating unchanged to the caller exactly as any other fault from this
 * engine already does.
 *
 * **What an implementation must not do (Contract Design Section 1,
 * unchanged):** engage a reasoning provider; construct a
 * `PlanningRequest`; construct an `OutboundParkerResponse`; call
 * `PlannerRuntime`, `ExecutionPipeline`, `PermissionEngine`, `MemoryStore`,
 * or `WorldModel`; repeat, second-guess, or bypass [CommunicationIntake]'s
 * own two structural checks; implement termination, expiry, reopening, or
 * cleanup policy (Continuity Contract Design Section 7 -- out of this
 * Unit's scope, left open).
 */
interface ConversationEngine {
    suspend fun resolveConversationId(message: InboundOwnerMessage): ConversationId

    suspend fun submitTurn(message: InboundOwnerMessage, conversationId: ConversationId): ConversationDisposition
}
