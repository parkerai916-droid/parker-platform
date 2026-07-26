package parker.core.interfaces

/**
 * Conversation History Source contract, Sprint 11 Unit 6, per
 * `docs/architecture/CONVERSATION_HISTORY_SOURCE_CONTRACT_DESIGN.md`
 * ("the Contract Design") Section 4.1, freezing
 * `docs/implementation/CONVERSATION_HISTORY_SOURCE_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 1's single responsibility: **given a
 * [ConversationId], retrieve the already-existing, ordered, read-only
 * Turns recorded for it.** Nothing more.
 *
 * **One operation, deliberately.** No `count`, `since`, pagination, or
 * filter parameter -- Scope Lock Section 1a excludes relevance, ranking,
 * or any semantic criterion from this boundary. A future, separately
 * justified component may add those on top of this one; this Unit does
 * not anticipate that need.
 *
 * **Read-only, never throws for "nothing recorded."** [history] returns
 * an empty list -- never an exception -- for a [ConversationId] with no
 * Turns recorded yet, including one [ConversationEngine] has never seen.
 * This mirrors [IdentityService.resolve]'s own established "return
 * null/empty, do not throw, for a not-found lookup" convention.
 *
 * **Ordering.** Turns are returned oldest-first -- the same order
 * [ConversationEngine] itself records them in, since Turns are only ever
 * appended, never reordered or removed.
 *
 * **Never a second source of truth.** Every implementation of this
 * interface must be a pure projection of [ConversationEngine]'s own owned
 * state (Scope Lock Section 3, Governing Principle) -- it must never
 * construct a [Turn], never mutate a [Conversation], and never call
 * anything equivalent to [ConversationEngine.submitTurn]. This interface
 * is declared separately from [ConversationEngine] specifically so a
 * caller holding only a [ConversationHistorySource] reference is
 * structurally unable to reach [ConversationEngine.resolveConversationId]
 * or [ConversationEngine.submitTurn] -- capability narrowing enforced by
 * the type system, not by convention alone.
 *
 * **Disclosed limitation (Contract Design Section 4.3), not a defect this
 * Unit is responsible for closing.** [Turn.message] is an
 * [InboundOwnerMessage] -- the owner's inbound message only. No Turn
 * record anywhere in this runtime today captures the outbound reply
 * Parker composed for it, so [history] returns a one-sided history: the
 * owner's own prior messages, in order, never Parker's own prior replies.
 *
 * **Not decided by this interface (Contract Design Section 5/8):** any
 * bound on history volume or age; whether outbound replies are ever
 * captured; termination, expiry, reopening, or cross-channel span (all
 * remain `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md`'s own, unchanged,
 * open questions).
 */
fun interface ConversationHistorySource {
    suspend fun history(conversationId: ConversationId): List<Turn>
}
