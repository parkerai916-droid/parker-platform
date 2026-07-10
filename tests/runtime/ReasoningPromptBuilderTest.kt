package parker.core.runtime

import parker.core.interfaces.ConversationId
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves `DefaultReasoningPromptBuilder`'s exact template
 * (`docs/implementation/MODEL_REASONING_PROVIDER_IMPLEMENTATION_PLAN.md`
 * Section 6, Decision A).
 */
class ReasoningPromptBuilderTest {

    private val instruction = "Respond with exactly one of the following prefixes: GOAL:, REPLY:, or " +
        "NOACTION, followed by your response text. Use NOACTION alone, with no " +
        "text after it."

    private fun turn(text: String) = Turn(
        turnId = TurnId("turn-1"),
        conversationId = ConversationId("conv-1"),
        message = InboundOwnerMessage(
            channelId = ModuleId("channel.local-text"),
            senderPrincipalId = PrincipalId("user-1"),
            text = text,
            timestamp = Instant.parse("2026-01-01T00:00:00Z"),
            correlationId = CorrelationId("corr-1"),
        ),
        receivedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    fun `buildPrompt with multiple context entries lists them one per line, in order, before the owner's message`() {
        val builder = DefaultReasoningPromptBuilder()
        val context = ReasoningContext(listOf("entry one", "entry two"))

        val prompt = builder.buildPrompt(turn("what's the weather?"), context)

        val expected = "entry one\nentry two\nwhat's the weather?\n\n$instruction"
        assertEquals(expected, prompt)
    }

    @Test
    fun `buildPrompt with a single context entry places it before the owner's message`() {
        val builder = DefaultReasoningPromptBuilder()
        val context = ReasoningContext(listOf("only entry"))

        val prompt = builder.buildPrompt(turn("hi"), context)

        val expected = "only entry\nhi\n\n$instruction"
        assertEquals(expected, prompt)
    }

    @Test
    fun `buildPrompt with zero context entries omits the context block entirely`() {
        val builder = DefaultReasoningPromptBuilder()

        val prompt = builder.buildPrompt(turn("hello"), ReasoningContext(emptyList()))

        val expected = "hello\n\n$instruction"
        assertEquals(expected, prompt)
    }

    @Test
    fun `the owner's message text appears in the prompt unmodified`() {
        val builder = DefaultReasoningPromptBuilder()
        val message = "  mixed CASE, punctuation! and\ttabs\t"

        val prompt = builder.buildPrompt(turn(message), ReasoningContext(emptyList()))

        assertTrue(prompt.contains(message))
    }

    @Test
    fun `the fixed instruction names all three response prefixes`() {
        val builder = DefaultReasoningPromptBuilder()

        val prompt = builder.buildPrompt(turn("hi"), ReasoningContext(emptyList()))

        assertTrue(prompt.contains("GOAL:"))
        assertTrue(prompt.contains("REPLY:"))
        assertTrue(prompt.contains("NOACTION"))
    }

    @Test
    fun `buildPrompt is pure -- calling it twice with equal inputs produces an identical result`() {
        val builder = DefaultReasoningPromptBuilder()
        val context = ReasoningContext(listOf("entry"))
        val t = turn("repeat me")

        assertEquals(builder.buildPrompt(t, context), builder.buildPrompt(t, context))
    }
}
