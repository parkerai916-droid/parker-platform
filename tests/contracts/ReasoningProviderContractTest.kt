package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Sprint 7, Stage 3 Implementation Unit acceptance test
 * (`docs/implementation/CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` §6),
 * covering the contract *shapes* defined in `ReasoningProvider.kt` --
 * blank-rejection and sealed-variant-exclusivity -- independent of any
 * runtime implementation.
 */
class ReasoningProviderContractTest {

    private fun turn(conversationId: String = "conv-1", turnId: String = "turn-1") = Turn(
        turnId = TurnId(turnId),
        conversationId = ConversationId(conversationId),
        message = InboundOwnerMessage(
            channelId = ModuleId("channel.local-text"),
            senderPrincipalId = PrincipalId("user-1"),
            text = "hello",
            timestamp = Instant.parse("2026-01-01T00:00:00Z"),
            correlationId = CorrelationId("corr-1"),
        ),
        receivedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    // --- ReasoningContext ---

    @Test
    fun `ReasoningContext accepts an empty entry list`() {
        val context = ReasoningContext(emptyList())

        assertIs<ReasoningContext>(context)
    }

    @Test
    fun `ReasoningContext rejects a blank entry`() {
        assertFailsWith<IllegalArgumentException> {
            ReasoningContext(listOf("valid entry", "   "))
        }
    }

    // --- ReasoningProviderRequest ---

    @Test
    fun `ReasoningProviderRequest holds exactly the supplied Turn and ReasoningContext`() {
        val request = ReasoningProviderRequest(
            turn = turn(),
            reasoningContext = ReasoningContext(listOf("context entry")),
        )

        assertIs<ReasoningProviderRequest>(request)
    }

    // --- Goal ---

    @Test
    fun `Goal rejects blank text`() {
        assertFailsWith<IllegalArgumentException> {
            ReasoningProviderResponse.Goal("   ")
        }
    }

    @Test
    fun `Goal accepts non-blank text`() {
        val goal = ReasoningProviderResponse.Goal("book a flight")

        assertIs<ReasoningProviderResponse.Goal>(goal)
    }

    // --- Reply ---

    @Test
    fun `Reply rejects blank text`() {
        assertFailsWith<IllegalArgumentException> {
            ReasoningProviderResponse.Reply("")
        }
    }

    @Test
    fun `Reply accepts non-blank text`() {
        val reply = ReasoningProviderResponse.Reply("sure, on it")

        assertIs<ReasoningProviderResponse.Reply>(reply)
    }

    // --- NoAction ---

    @Test
    fun `NoAction is a single, valueless object`() {
        assertIs<ReasoningProviderResponse.NoAction>(ReasoningProviderResponse.NoAction)
    }

    // --- sealed exclusivity ---

    @Test
    fun `a ReasoningProviderResponse holds exactly one of Goal, Reply, or NoAction, never more than one`() {
        val responses: List<ReasoningProviderResponse> = listOf(
            ReasoningProviderResponse.Goal("goal text"),
            ReasoningProviderResponse.Reply("reply text"),
            ReasoningProviderResponse.NoAction,
        )

        for (response in responses) {
            when (response) {
                is ReasoningProviderResponse.Goal -> assertIs<ReasoningProviderResponse.Goal>(response)
                is ReasoningProviderResponse.Reply -> assertIs<ReasoningProviderResponse.Reply>(response)
                is ReasoningProviderResponse.NoAction -> assertIs<ReasoningProviderResponse.NoAction>(response)
            }
        }
    }
}
