package parker.core.runtime

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ConversationId
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Proves `ModelReasoningProvider.reason`'s own orchestration
 * (`docs/implementation/reviews/ModelReasoningProvider_Implementation_Review.md`
 * Section 4/5/8) using the three fake collaborators
 * ([FakeReasoningPromptBuilder], [FakeModelInferenceClient],
 * [FakeReasoningResponseParser]) -- independently of
 * [DefaultReasoningPromptBuilder], [LocalHttpModelInferenceClient], and
 * [TaggedReasoningResponseParser]'s own real logic, each covered by its
 * own dedicated test file.
 */
class ModelReasoningProviderTest {

    private fun turn(text: String = "hello") = Turn(
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

    private fun request(context: ReasoningContext = ReasoningContext(emptyList())) =
        ReasoningProviderRequest(turn = turn(), reasoningContext = context)

    @Test
    fun `reason calls buildPrompt with the request's exact turn and reasoningContext`() = runTest {
        val promptBuilder = FakeReasoningPromptBuilder { _, _ -> "built prompt" }
        val inferenceClient = FakeModelInferenceClient { "REPLY:ok" }
        val responseParser = FakeReasoningResponseParser { ReasoningProviderResponse.Reply("ok") }
        val provider = ModelReasoningProvider(promptBuilder, inferenceClient, responseParser)
        val context = ReasoningContext(listOf("entry one"))
        val req = request(context)

        provider.reason(req)

        assertEquals(1, promptBuilder.buildPromptCallCount)
        assertEquals(req.turn, promptBuilder.lastTurn)
        assertEquals(context, promptBuilder.lastReasoningContext)
    }

    @Test
    fun `reason passes buildPrompt's exact result to infer, unchanged`() = runTest {
        val promptBuilder = FakeReasoningPromptBuilder { _, _ -> "the exact prompt" }
        val inferenceClient = FakeModelInferenceClient { "REPLY:ok" }
        val responseParser = FakeReasoningResponseParser { ReasoningProviderResponse.Reply("ok") }
        val provider = ModelReasoningProvider(promptBuilder, inferenceClient, responseParser)

        provider.reason(request())

        assertEquals(1, inferenceClient.inferCallCount)
        assertEquals("the exact prompt", inferenceClient.lastPrompt)
    }

    @Test
    fun `reason passes infer's exact raw result to parse, unchanged, and returns parse's result unchanged`() = runTest {
        val promptBuilder = FakeReasoningPromptBuilder { _, _ -> "prompt" }
        val inferenceClient = FakeModelInferenceClient { "GOAL:book a flight" }
        val goalResponse = ReasoningProviderResponse.Goal("book a flight")
        val responseParser = FakeReasoningResponseParser { raw ->
            assertEquals("GOAL:book a flight", raw)
            goalResponse
        }
        val provider = ModelReasoningProvider(promptBuilder, inferenceClient, responseParser)

        val result = provider.reason(request())

        assertEquals(1, responseParser.parseCallCount)
        assertEquals("GOAL:book a flight", responseParser.lastRaw)
        assertEquals(goalResponse, result)
    }

    @Test
    fun `an exception thrown by infer propagates uncaught, and parse is never called`() = runTest {
        val promptBuilder = FakeReasoningPromptBuilder { _, _ -> "prompt" }
        val inferenceClient = FakeModelInferenceClient { throw IllegalStateException("model unreachable") }
        val responseParser = FakeReasoningResponseParser { ReasoningProviderResponse.NoAction }
        val provider = ModelReasoningProvider(promptBuilder, inferenceClient, responseParser)

        assertFailsWith<IllegalStateException> { provider.reason(request()) }
        assertEquals(0, responseParser.parseCallCount)
    }

    @Test
    fun `an exception thrown by parse propagates uncaught`() = runTest {
        val promptBuilder = FakeReasoningPromptBuilder { _, _ -> "prompt" }
        val inferenceClient = FakeModelInferenceClient { "garbage" }
        val responseParser = FakeReasoningResponseParser {
            throw UnclassifiableModelResponseException("garbage")
        }
        val provider = ModelReasoningProvider(promptBuilder, inferenceClient, responseParser)

        assertFailsWith<UnclassifiableModelResponseException> { provider.reason(request()) }
    }

    @Test
    fun `infer exceeding timeoutMs surfaces TimeoutCancellationException, and parse is never called`() = runTest {
        val promptBuilder = FakeReasoningPromptBuilder { _, _ -> "prompt" }
        val inferenceClient = FakeModelInferenceClient {
            delay(1_000)
            "REPLY:too slow"
        }
        val responseParser = FakeReasoningResponseParser { ReasoningProviderResponse.NoAction }
        val provider = ModelReasoningProvider(promptBuilder, inferenceClient, responseParser, timeoutMs = 10)

        assertFailsWith<TimeoutCancellationException> { provider.reason(request()) }
        assertEquals(0, responseParser.parseCallCount)
    }

    @Test
    fun `the constructor accepts exactly four parameters -- three collaborators and timeoutMs`() {
        // ModelReasoningProvider's constructor has a default value for timeoutMs (Plan
        // Decision 4), so the Kotlin compiler emits a second, synthetic constructor
        // (ACC_SYNTHETIC, with trailing `int` bitmask + `DefaultConstructorMarker`
        // parameters) alongside the real one, to support default-argument call sites --
        // unrelated to ModelReasoningProvider's own declared shape. Filtering it out
        // isolates the one real, declared constructor.
        val constructor = ModelReasoningProvider::class.java.declaredConstructors
            .single { !it.isSynthetic }
        assertEquals(4, constructor.parameterCount)
    }
}
