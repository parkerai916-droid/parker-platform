package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.Conversation
import parker.core.interfaces.ConversationDisposition
import parker.core.interfaces.ConversationEngine
import parker.core.interfaces.ConversationHistorySource
import parker.core.interfaces.ConversationId
import parker.core.interfaces.IdentityService
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import java.time.Instant
import java.util.UUID

/**
 * The in-memory [ConversationEngine] implementation, originally Sprint 7
 * Stage 3, revised for Sprint 11 Unit 5 (Conversation Continuity
 * Implementation) per
 * `docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` ("the
 * Continuity Contract Design"), and further revised for Sprint 11 Unit 6
 * (Conversation History Source) per
 * `docs/architecture/CONVERSATION_HISTORY_SOURCE_CONTRACT_DESIGN.md` --
 * this class now additionally implements [ConversationHistorySource], a
 * second, narrower, read-only interface over the same owned state, never
 * a second store (see [turnsById] and [history], below).
 *
 * **No longer stateless.** Sprint 7's own "Required Implementation
 * Decision 1" (every inbound Turn begins a new Conversation, because
 * "there is no stored state to consult") is retired by this revision: real
 * continuity recognition requires knowing which continuity keys currently
 * have an open Conversation, so this class now holds two private,
 * `Mutex`-guarded maps (Continuity Contract Design Section 7 -- state
 * required for identity selection and active-Conversation tracking, not
 * for computing an identifier from nothing).
 *
 * **Continuity key.** `(channelId, senderPrincipalId)` -- Continuity
 * Contract Design Section 3. Never `correlationId` (distinct from
 * `ConversationId` by design, Frozen Fact 3) and never message content
 * (Contract Design's own "no semantic or content-based recognition"
 * requirement).
 *
 * **Concurrency.** One private [Mutex] guards both maps together, so
 * [resolveConversationId]'s check-and-mint sequence and [submitTurn]'s
 * own read-and-append sequence are each atomic with respect to every other
 * call, for any key -- per this Unit's own instruction, no per-key lock
 * and no lock registry exist here; correctness, not throughput, is this
 * revision's own concern, and no evidence of contention exists to justify
 * more. The lock is held only across the map reads/writes themselves --
 * never across `identityService.resolve` (a separate precondition, not a
 * continuity-state operation) and never across anything downstream of
 * this class (reasoning, model invocation, response composition/delivery
 * all happen in later, unrelated calls this class has no reference to).
 *
 * **Out of scope, disclosed, not designed here** (Continuity Contract
 * Design Section 7): termination, expiry, reopening, and cleanup policy.
 * Once a continuity key's Conversation is opened by [resolveConversationId],
 * it remains the active mapping for that key indefinitely -- exactly as
 * the Contract Design's own Section 3 discloses as a known consequence of
 * adopting this rule now, not a defect introduced by this implementation.
 *
 * **Required Implementation Decision 3** (unchanged): this engine's own
 * operating identity is `system.conversation-engine`. Verified
 * independently at the top of both [resolveConversationId] and
 * [submitTurn] -- each is now a genuine, separately callable public
 * operation, so each independently guards its own precondition rather than
 * relying on the other to have already checked it.
 *
 * @param identityService Used only to resolve this engine's own operating
 *   Principal before acting. This is the only dependency this
 *   implementation accepts -- its absence of any other constructor
 *   parameter is itself the structural guarantee that this engine cannot
 *   reach `PlannerRuntime`, `ExecutionPipeline`, `KnowledgeStore`, `WorldModel`,
 *   or any reasoning provider.
 */
class InMemoryConversationEngine(
    private val identityService: IdentityService,
) : ConversationEngine, ConversationHistorySource {

    /** Continuity Contract Design Section 3: `(channelId, senderPrincipalId)`, and nothing else. */
    private data class ContinuityKey(val channelId: ModuleId, val senderPrincipalId: PrincipalId)

    private val stateLock = Mutex()

    /** Continuity key -> the active [ConversationId] currently open for it. Never shrinks in this Unit (no termination). */
    private val activeConversationIds = mutableMapOf<ContinuityKey, ConversationId>()

    /** [ConversationId] -> its [Conversation] record, so far. Grows a Turn at a time via [submitTurn]. */
    private val conversationsById = mutableMapOf<ConversationId, Conversation>()

    /**
     * [TurnId] -> its [Turn] value, Sprint 11 Unit 6
     * (`CONVERSATION_HISTORY_SOURCE_CONTRACT_DESIGN.md` Section 4.2). Prior
     * to this Unit, `submitTurn` constructed a [Turn] and returned it once,
     * without retaining it -- [conversationsById]'s own [Conversation.turnIds]
     * recorded only Turn *order*, never Turn *content*. This map retains
     * exactly what `submitTurn` already, momentarily, held -- not new
     * authority, only retention of an existing one. Populated inside the
     * same [stateLock]-guarded section [submitTurn] already uses; read by
     * [history] under the same lock.
     */
    private val turnsById = mutableMapOf<TurnId, Turn>()

    private companion object {
        val CONVERSATION_ENGINE_PRINCIPAL_ID = PrincipalId("system.conversation-engine")
    }

    private suspend fun requireOperatingPrincipalRegistered() {
        identityService.resolve(CONVERSATION_ENGINE_PRINCIPAL_ID)
            ?: throw IllegalStateException(
                "Conversation Engine operating Principal " +
                    "'${CONVERSATION_ENGINE_PRINCIPAL_ID.value}' is not registered with IdentityService"
            )
    }

    private fun continuityKeyOf(message: InboundOwnerMessage) =
        ContinuityKey(message.channelId, message.senderPrincipalId)

    /**
     * The sole authoritative continuity decision (Continuity Contract
     * Design Section 4). Atomic and idempotent-while-active (Section 5.1,
     * Guarantees 1-2): the entire check-or-mint sequence runs inside
     * [stateLock], so two concurrent calls for the same continuity key can
     * never each mint a distinct [ConversationId], and repeated calls for
     * a still-open key always return the identical value.
     */
    override suspend fun resolveConversationId(message: InboundOwnerMessage): ConversationId {
        requireOperatingPrincipalRegistered()

        val key = continuityKeyOf(message)
        return stateLock.withLock {
            activeConversationIds[key] ?: ConversationId(UUID.randomUUID().toString()).also { minted ->
                activeConversationIds[key] = minted
            }
        }
    }

    /**
     * Consumes, never re-decides, the supplied [conversationId] (Section
     * 5.1, Guarantee 3). Validates it is the exact, currently-active
     * identifier for `message`'s own continuity key -- not merely "known
     * to this engine at all" -- before recording a Turn under it, so an
     * identifier that belonged to a *different* continuity key (or one no
     * longer active) is rejected exactly as an unknown one would be, never
     * silently repaired or substituted.
     *
     * @throws IllegalArgumentException if [conversationId] is not the
     *   active identifier [resolveConversationId] would return right now
     *   for `message`'s own continuity key -- this repository's own
     *   established convention for a caller-supplied precondition failure
     *   (`IdentityService`/`ModuleRegistry`/`ToolRegistry`, and this same
     *   class's own [IllegalStateException] above, all throw directly
     *   rather than returning a sealed result); no new exception type is
     *   introduced (see `IMPLEMENTATION_HISTORY.md`, Sprint 11 Unit 5, for
     *   the narrowest-existing-mechanism review this decision rests on).
     *   Per Guarantee 4, this failure prevents any Turn from being
     *   recorded and propagates unchanged to the caller.
     */
    override suspend fun submitTurn(message: InboundOwnerMessage, conversationId: ConversationId): ConversationDisposition {
        requireOperatingPrincipalRegistered()

        val key = continuityKeyOf(message)

        return stateLock.withLock {
            val activeForKey = activeConversationIds[key]
            require(activeForKey != null && activeForKey == conversationId) {
                "submitTurn was supplied ConversationId '${conversationId.value}', which is not the " +
                    "active identifier for continuity key (channelId='${message.channelId.value}', " +
                    "senderPrincipalId='${message.senderPrincipalId.value}') -- resolveConversationId " +
                    "must be called first, and its exact result supplied unchanged, never substituted " +
                    "or silently repaired."
            }

            val turnId = TurnId(UUID.randomUUID().toString())
            val turn = Turn(
                turnId = turnId,
                conversationId = conversationId,
                message = message,
                receivedAt = Instant.now(),
            )

            val existingConversation = conversationsById[conversationId]
            val isNewConversation = existingConversation == null
            val conversation = if (existingConversation != null) {
                existingConversation.copy(turnIds = existingConversation.turnIds + turnId)
            } else {
                Conversation(
                    conversationId = conversationId,
                    ownerPrincipalId = message.senderPrincipalId,
                    channelId = message.channelId,
                    turnIds = listOf(turnId),
                )
            }
            conversationsById[conversationId] = conversation
            turnsById[turnId] = turn

            ConversationDisposition(
                conversation = conversation,
                turn = turn,
                isNewConversation = isNewConversation,
            )
        }
    }

    /**
     * Conversation History Source's sole operation
     * (`ConversationHistorySource.kt`'s own KDoc). A pure read: returns the
     * [Turn]s recorded for [conversationId], oldest first, matching
     * [Conversation.turnIds]'s own append-only order -- or an empty list,
     * never a thrown exception, if [conversationId] has no Turns recorded
     * (including one this engine has never seen via
     * [resolveConversationId]). Guarded by the same [stateLock] [submitTurn]
     * uses, so a concurrent, in-flight [submitTurn] for the same
     * [conversationId] can never be observed half-applied (a [Turn]
     * recorded in [conversationsById] but not yet in [turnsById], or vice
     * versa).
     */
    override suspend fun history(conversationId: ConversationId): List<Turn> {
        requireOperatingPrincipalRegistered()

        return stateLock.withLock {
            conversationsById[conversationId]?.turnIds.orEmpty().mapNotNull { turnId -> turnsById[turnId] }
        }
    }
}
