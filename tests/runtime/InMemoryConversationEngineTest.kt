package parker.core.runtime

import kotlinx.coroutines.test.runTest
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
 * Sprint 7, Stage 3 Implementation Unit acceptance test
 * (`docs/implementation/CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` §6),
 * covering [InMemoryConversationEngine] alone -- not the coordinator, not
 * any reasoning provider.
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
        correlationId: String = "corr-1",
    ) = InboundOwnerMessage(
        channelId = ModuleId("channel.local-text"),
        senderPrincipalId = PrincipalId(senderPrincipalId),
        text = text,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId(correlationId),
    )

    // --- successful submitTurn ---

    @Test
    fun `submitTurn with a registered operating Principal returns a disposition with isNewConversation true`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(conversationEnginePrincipal())
        identity.register(principal())
        val engine = InMemoryConversationEngine(identity)

        val disposition = engine.submitTurn(message())

        assertTrue(disposition.isNewConversation)
    }

    @Test
    fun `submitTurn produces a Turn wrapping the inbound message unchanged`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(conversationEnginePrincipal())
        identity.register(principal())
        val engine = InMemoryConversationEngine(identity)
        val inbound = message(text = "what's on my calendar")

        val disposition = engine.submitTurn(inbound)

        assertEquals(inbound, disposition.turn.message)
        assertEquals(disposition.conversation.conversationId, disposition.turn.conversationId)
    }

    @Test
    fun `submitTurn produces a Conversation with turnIds containing exactly the new Turn`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(conversationEnginePrincipal())
        identity.register(principal())
        val engine = InMemoryConversationEngine(identity)

        val disposition = engine.submitTurn(message())

        assertEquals(listOf(disposition.turn.turnId), disposition.conversation.turnIds)
    }

    @Test
    fun `submitTurn's resulting Conversation owner is the message sender, not the operating Principal`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(conversationEnginePrincipal())
        identity.register(principal("user-1"))
        val engine = InMemoryConversationEngine(identity)

        val disposition = engine.submitTurn(message(senderPrincipalId = "user-1"))

        assertEquals(PrincipalId("user-1"), disposition.conversation.ownerPrincipalId)
        assertNotEquals(PrincipalId("system.conversation-engine"), disposition.conversation.ownerPrincipalId)
    }

    // --- two calls produce two distinct Conversations/Turns ---

    @Test
    fun `two calls to submitTurn produce two distinct ConversationIds and TurnIds`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(conversationEnginePrincipal())
        identity.register(principal())
        val engine = InMemoryConversationEngine(identity)

        val first = engine.submitTurn(message(correlationId = "corr-1"))
        val second = engine.submitTurn(message(correlationId = "corr-2"))

        assertNotEquals(first.conversation.conversationId, second.conversation.conversationId)
        assertNotEquals(first.turn.turnId, second.turn.turnId)
    }

    // --- operating Principal resolution ---

    @Test
    fun `a registered operating Principal resolves successfully before submitTurn proceeds`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(conversationEnginePrincipal())
        identity.register(principal())
        val engine = InMemoryConversationEngine(identity)

        // Does not throw.
        engine.submitTurn(message())

        assertTrue(true)
    }

    @Test
    fun `a missing operating Principal causes submitTurn to fail fast`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal()) // "system.conversation-engine" deliberately not registered
        val engine = InMemoryConversationEngine(identity)

        assertFailsWith<IllegalStateException> {
            engine.submitTurn(message())
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
            engine.submitTurn(message(senderPrincipalId = "user-1"))
        }
    }
}
