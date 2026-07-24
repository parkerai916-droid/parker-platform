package parker.core.runtime

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ConversationId
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Sprint 7, Stage 3 acceptance test, revised Sprint 11 Unit 5
 * (Conversation Continuity Implementation) per
 * `docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` ("the
 * Continuity Contract Design"). Covers [InMemoryConversationEngine] alone
 * -- not the coordinator, not any reasoning provider -- including its
 * revised two-operation contract ([resolveConversationId], [submitTurn])
 * and the four Binding Guarantees (Continuity Contract Design Section
 * 5.1).
 */
class InMemoryConversationEngineTest {

    private fun principal(id: String = "user-1") = Principal(
        principalId = PrincipalId(id),
        principalType = PrincipalType.USER,
        displayName = "Test Principal",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun conversationEnginePrincipal() = Principal(
        principalId = PrincipalId("system.conversation-engine"),
        principalType = PrincipalType.SYSTEM,
        displayName = "Conversation Engine",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun message(
        text: String = "hello",
        senderPrincipalId: String = "user-1",
        channelId: String = "channel.local-text",
        correlationId: String = "corr-1",
    ) = InboundOwnerMessage(
        channelId = ModuleId(channelId),
        senderPrincipalId = PrincipalId(senderPrincipalId),
        text = text,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId(correlationId),
    )

    private suspend fun readyIdentityService(vararg extra: Principal): InMemoryIdentityService {
        val identity = InMemoryIdentityService()
        identity.register(conversationEnginePrincipal())
        extra.forEach { identity.register(it) }
        return identity
    }

    // ================= submitTurn, given an already-resolved ConversationId =================

    @Test
    fun `submitTurn for a fresh continuity key returns a disposition with isNewConversation true`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))
        val msg = message()

        val conversationId = engine.resolveConversationId(msg)
        val disposition = engine.submitTurn(msg, conversationId)

        assertTrue(disposition.isNewConversation)
    }

    @Test
    fun `submitTurn produces a Turn wrapping the inbound message unchanged`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))
        val inbound = message(text = "what's on my calendar")

        val conversationId = engine.resolveConversationId(inbound)
        val disposition = engine.submitTurn(inbound, conversationId)

        assertEquals(inbound, disposition.turn.message)
        assertEquals(conversationId, disposition.turn.conversationId)
        assertEquals(disposition.conversation.conversationId, disposition.turn.conversationId)
    }

    @Test
    fun `submitTurn produces a Conversation with turnIds containing exactly the new Turn, for a fresh key`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))
        val msg = message()

        val conversationId = engine.resolveConversationId(msg)
        val disposition = engine.submitTurn(msg, conversationId)

        assertEquals(listOf(disposition.turn.turnId), disposition.conversation.turnIds)
    }

    @Test
    fun `submitTurn's resulting Conversation owner is the message sender, not the operating Principal`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal("user-1")))
        val msg = message(senderPrincipalId = "user-1")

        val conversationId = engine.resolveConversationId(msg)
        val disposition = engine.submitTurn(msg, conversationId)

        assertEquals(PrincipalId("user-1"), disposition.conversation.ownerPrincipalId)
        assertNotEquals(PrincipalId("system.conversation-engine"), disposition.conversation.ownerPrincipalId)
    }

    // ================= Resolution: continuity key semantics =================

    @Test
    fun `repeated resolution for the same continuity key returns the same active ConversationId`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))
        val first = message(correlationId = "corr-1")
        val second = message(correlationId = "corr-2")

        val firstId = engine.resolveConversationId(first)
        val secondId = engine.resolveConversationId(second)

        assertEquals(firstId, secondId)
    }

    @Test
    fun `CorrelationId differences do not affect continuity -- two messages sharing channel and sender resolve to the same Conversation`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))
        val first = message(correlationId = "corr-1")
        val second = message(correlationId = "corr-2")

        val firstDisposition = engine.submitTurn(first, engine.resolveConversationId(first))
        val secondDisposition = engine.submitTurn(second, engine.resolveConversationId(second))

        assertEquals(firstDisposition.conversation.conversationId, secondDisposition.conversation.conversationId)
        assertNotEquals(firstDisposition.turn.turnId, secondDisposition.turn.turnId)
        assertTrue(firstDisposition.isNewConversation)
        assertTrue(!secondDisposition.isNewConversation)
        assertEquals(
            listOf(firstDisposition.turn.turnId, secondDisposition.turn.turnId),
            secondDisposition.conversation.turnIds,
        )
    }

    @Test
    fun `message content differences do not affect continuity -- same channel and sender still resolve to the same Conversation`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))
        val first = message(text = "first request", correlationId = "corr-1")
        val second = message(text = "an entirely different request", correlationId = "corr-2")

        val firstId = engine.resolveConversationId(first)
        val secondId = engine.resolveConversationId(second)

        assertEquals(firstId, secondId)
    }

    @Test
    fun `a different channel produces a different Conversation, for the same sender`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))
        val onChannelA = message(channelId = "channel.a")
        val onChannelB = message(channelId = "channel.b")

        val idA = engine.resolveConversationId(onChannelA)
        val idB = engine.resolveConversationId(onChannelB)

        assertNotEquals(idA, idB)
    }

    @Test
    fun `a different sender produces a different Conversation, for the same channel`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal("user-1"), principal("user-2")))
        val fromUser1 = message(senderPrincipalId = "user-1")
        val fromUser2 = message(senderPrincipalId = "user-2")

        val id1 = engine.resolveConversationId(fromUser1)
        val id2 = engine.resolveConversationId(fromUser2)

        assertNotEquals(id1, id2)
    }

    // ================= Concurrency (Section 5.1, Guarantees 1-2) =================

    @Test
    fun `concurrent resolution for the same continuity key returns one identical ConversationId`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))
        val msg = message()

        val results = coroutineScope {
            List(50) { async { engine.resolveConversationId(msg) } }.awaitAll()
        }

        assertEquals(1, results.toSet().size, "expected exactly one active ConversationId across 50 concurrent resolutions for the same key")
    }

    @Test
    fun `concurrent resolution for different continuity keys resolves each key correctly and independently`() = runTest {
        val identity = readyIdentityService(principal("user-1"), principal("user-2"), principal("user-3"))
        val engine = InMemoryConversationEngine(identity)
        val keys = listOf(
            message(senderPrincipalId = "user-1", channelId = "channel.a"),
            message(senderPrincipalId = "user-2", channelId = "channel.a"),
            message(senderPrincipalId = "user-3", channelId = "channel.b"),
        )

        val resultsByKey = coroutineScope {
            keys.map { keyMessage ->
                keyMessage to List(20) { async { engine.resolveConversationId(keyMessage) } }.awaitAll()
            }
        }

        resultsByKey.forEach { (_, results) ->
            assertEquals(1, results.toSet().size, "expected exactly one active ConversationId per key across 20 concurrent resolutions")
        }
        val idsByKey = resultsByKey.map { (_, results) -> results.first() }
        assertEquals(idsByKey.toSet().size, idsByKey.size, "distinct continuity keys must never resolve to the same ConversationId")
    }

    // ================= Submission validation (Section 5.1, Guarantee 3) =================

    @Test
    fun `submitTurn accepts a ConversationId it just resolved`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))
        val msg = message()

        val conversationId = engine.resolveConversationId(msg)

        // Does not throw.
        engine.submitTurn(msg, conversationId)
        assertTrue(true)
    }

    @Test
    fun `submitTurn rejects a ConversationId that was never resolved, and creates no Turn`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))
        val msg = message()
        val neverResolved = ConversationId("never-resolved-by-this-engine")

        assertFailsWith<IllegalArgumentException> {
            engine.submitTurn(msg, neverResolved)
        }

        // No silent substitution: the message's own continuity key is still unresolved --
        // the very next resolution for it must still behave as a fresh key (isNewConversation true).
        val conversationId = engine.resolveConversationId(msg)
        val disposition = engine.submitTurn(msg, conversationId)
        assertTrue(disposition.isNewConversation)
    }

    @Test
    fun `submitTurn rejects a ConversationId belonging to a different continuity key`() = runTest {
        val identity = readyIdentityService(principal("user-1"), principal("user-2"))
        val engine = InMemoryConversationEngine(identity)
        val messageForKeyA = message(senderPrincipalId = "user-1", channelId = "channel.a")
        val messageForKeyB = message(senderPrincipalId = "user-2", channelId = "channel.a")

        val idForKeyB = engine.resolveConversationId(messageForKeyB)

        // idForKeyB is a real, engine-minted identifier -- just not the active one for keyA.
        // submitTurn must reject it for keyA's own message, never substitute keyA's real identifier
        // in its place (Guarantee 3: no silent repair, no re-resolution).
        assertFailsWith<IllegalArgumentException> {
            engine.submitTurn(messageForKeyA, idForKeyB)
        }
    }

    @Test
    fun `rejection does not create a Turn -- a subsequent correct submission for the same key still reports isNewConversation true`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))
        val msg = message()

        assertFailsWith<IllegalArgumentException> {
            engine.submitTurn(msg, ConversationId("bogus-id"))
        }

        val conversationId = engine.resolveConversationId(msg)
        val disposition = engine.submitTurn(msg, conversationId)
        assertTrue(disposition.isNewConversation, "a rejected submission must not have created a Conversation record")
        assertEquals(listOf(disposition.turn.turnId), disposition.conversation.turnIds)
    }

    // ================= Operating Principal resolution (unchanged) =================

    @Test
    fun `a registered operating Principal resolves successfully before resolveConversationId proceeds`() = runTest {
        val engine = InMemoryConversationEngine(readyIdentityService(principal()))

        // Does not throw.
        engine.resolveConversationId(message())
        assertTrue(true)
    }

    @Test
    fun `a missing operating Principal causes resolveConversationId to fail fast`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal()) // "system.conversation-engine" deliberately not registered
        val engine = InMemoryConversationEngine(identity)

        assertFailsWith<IllegalStateException> {
            engine.resolveConversationId(message())
        }
    }

    @Test
    fun `a missing operating Principal causes submitTurn to fail fast, independently of continuity validation`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal()) // "system.conversation-engine" deliberately not registered
        val engine = InMemoryConversationEngine(identity)

        assertFailsWith<IllegalStateException> {
            engine.submitTurn(message(), ConversationId("irrelevant-since-identity-check-runs-first"))
        }
    }

    @Test
    fun `the sender Principal is never substituted for the missing operating Principal`() = runTest {
        val identity = InMemoryIdentityService()
        // Only the sender is registered -- the operating Principal is not, even though a
        // Principal exists in the service. A buggy implementation that resolved *some*
        // Principal rather than specifically "system.conversation-engine" would incorrectly
        // succeed here.
        identity.register(principal("user-1"))
        val engine = InMemoryConversationEngine(identity)

        assertFailsWith<IllegalStateException> {
            engine.resolveConversationId(message(senderPrincipalId = "user-1"))
        }
    }
}
