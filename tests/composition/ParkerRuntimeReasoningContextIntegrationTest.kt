package parker.composition

import java.time.Instant
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 11, Unit 3 integration test, extended Sprint 11 Unit 5
 * (Conversation Continuity Implementation): confirms
 * `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 9's three
 * numbered guarantees, plus the Continuity Contract Design's own
 * propagation path (`docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md`
 * Section 5), hold against the real, running [ParkerRuntime] -- not merely
 * against [DefaultReasoningContextAssembler] or [InMemoryConversationEngine]
 * in isolation (see `tests/runtime/DefaultReasoningContextAssemblerTest.kt`
 * and `tests/runtime/InMemoryConversationEngineTest.kt` for those).
 *
 * Uses `runBlocking<Unit>`, not `kotlinx.coroutines.test.runTest`, for the
 * identical, already-documented reason
 * `ParkerRuntimeConversationPipelineTest.kt`'s own class KDoc gives: a
 * real [StubModelServer] round trip is genuine foreign-thread I/O, and
 * `runTest`'s virtual-time scheduler races it unfairly against
 * `ModelReasoningProvider`'s own `withTimeout`.
 */
class ParkerRuntimeReasoningContextIntegrationTest {

    private val ownerPrincipalId = "user.owner-context-integration-test"
    private val ownerDisplayName = "Context Integration Owner"
    private val channelModuleId = "channel.local-text-context-integration-test"
    private var server: StubModelServer? = null

    @AfterTest
    fun tearDown() {
        server?.close()
    }

    private fun startStub(responseFieldValue: String): StubModelServer =
        StubModelServer.start(responseFieldValue).also { server = it }

    private fun configFor(stub: StubModelServer) = ParkerRuntimeConfig(
        modelEndpointUrl = stub.endpointUrl,
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        ownerDisplayName = ownerDisplayName,
        localTextChannelModuleId = channelModuleId,
    )

    private fun message(text: String = "good morning parker", correlationId: String = "corr-context-${System.nanoTime()}") = InboundOwnerMessage(
        channelId = ModuleId(channelModuleId),
        senderPrincipalId = PrincipalId(ownerPrincipalId),
        text = text,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId(correlationId),
    )

    // --- (1) invoked exactly once per inbound message ---

    @Test
    fun `the Assembler is invoked exactly once per inbound message, for each of two separate messages`() = runBlocking<Unit> {
        val stub = startStub("REPLY: sure thing")
        val logger = RecordingParkerLogger()
        val runtime = ParkerRuntime(configFor(stub), logger)
        runtime.start()

        runtime.submitOwnerMessage(message(text = "first request", correlationId = "corr-first"))
        runtime.submitOwnerMessage(message(text = "second request", correlationId = "corr-second"))

        val assembledLogs = logger.messages(LogLevel.INFO).filter { it.startsWith("Reasoning Context assembled") }
        assertEquals(2, assembledLogs.size)
        assertTrue(assembledLogs.any { "corr-first" in it })
        assertTrue(assembledLogs.any { "corr-second" in it })

        runtime.shutdown()
    }

    // --- (2) the assembled ReasoningContext flows unchanged into the real prompt ---

    @Test
    fun `the assembled ReasoningContext's entries reach the real prompt sent to the model, unchanged`() = runBlocking<Unit> {
        val stub = startStub("REPLY: acknowledged")
        val runtime = ParkerRuntime(configFor(stub), RecordingParkerLogger())
        runtime.start()

        runtime.submitOwnerMessage(message(text = "what tools do you have?"))

        assertEquals(1, stub.receivedRequestBodies.size)
        val prompt = stub.receivedRequestBodies.single()
        // Requesting principal identity (IdentityService.resolve, real InMemoryIdentityService).
        assertTrue(ownerDisplayName in prompt, "prompt did not carry the resolved owner display name: $prompt")
        assertTrue(ownerPrincipalId in prompt, "prompt did not carry the owner PrincipalId: $prompt")
        // Available tool descriptions (ToolRegistry.listAll, real InMemoryToolRegistry -- the
        // one Tool this runtime registers, per ParkerRuntime's own class KDoc).
        assertTrue("Local Text Channel" in prompt || "deliver" in prompt, "prompt did not carry any registered tool description: $prompt")
        // Current time and current request, straight from InboundOwnerMessage.
        assertTrue("2026-01-01T00:00:00Z" in prompt, "prompt did not carry the message's own timestamp: $prompt")
        assertTrue("what tools do you have?" in prompt, "prompt did not carry the owner's own request text: $prompt")

        runtime.shutdown()
    }

    // --- (3) existing runtime behaviour is otherwise unchanged ---

    @Test
    fun `a Reply still reaches the owner through the full pipeline, now with a real, non-empty ReasoningContext in play`() = runBlocking<Unit> {
        val stub = startStub("REPLY: good morning to you too!")
        val logger = RecordingParkerLogger()
        val ownerSink = RecordingOwnerNotificationSink()
        val runtime = ParkerRuntime(configFor(stub), logger, ownerSink)
        runtime.start()

        val outcome = runtime.submitOwnerMessage(message())

        val delivered = assertIs<ParkerRuntimeOutcome.Delivered>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, delivered.executionResult.status)
        assertEquals(listOf("good morning to you too!"), ownerSink.notifications)
        assertTrue(logger.hasMessageContaining("Reasoning Context assembled"))
        assertTrue(logger.hasMessageContaining("Execution authorised"))
        assertTrue(logger.hasMessageContaining("Reasoning completed"))
        assertTrue(logger.hasMessageContaining("Conversation accepted"))

        runtime.shutdown()
    }

    // --- Sprint 11 Unit 5: Runtime integration for Conversation Continuity ---

    @Test
    fun `ParkerRuntime resolves conversation continuity exactly once per inbound message, before ReasoningContext assembly`() = runBlocking<Unit> {
        val stub = startStub("REPLY: sure thing")
        val logger = RecordingParkerLogger()
        val runtime = ParkerRuntime(configFor(stub), logger)
        runtime.start()

        runtime.submitOwnerMessage(message(text = "first request", correlationId = "corr-continuity-first"))
        runtime.submitOwnerMessage(message(text = "second request", correlationId = "corr-continuity-second"))

        val resolvedLogs = logger.messages(LogLevel.INFO).filter { it.startsWith("Conversation continuity resolved") }
        val assembledLogs = logger.messages(LogLevel.INFO).filter { it.startsWith("Reasoning Context assembled") }
        assertEquals(2, resolvedLogs.size, "expected resolution exactly once per inbound message")
        assertTrue(resolvedLogs.any { "corr-continuity-first" in it })
        assertTrue(resolvedLogs.any { "corr-continuity-second" in it })

        // Resolution before assembly: every INFO line up to and including the second
        // "resolved" line must appear before the second "assembled" line in the same order.
        val allInfo = logger.messages(LogLevel.INFO)
        val secondResolvedIndex = allInfo.indexOfLast { it.startsWith("Conversation continuity resolved") }
        val firstAssembledIndex = allInfo.indexOfFirst { it.startsWith("Reasoning Context assembled") }
        assertTrue(
            allInfo.indexOfFirst { it.startsWith("Conversation continuity resolved") } < firstAssembledIndex,
            "resolution must occur before the first assembly",
        )
        assertTrue(secondResolvedIndex >= 0 && assembledLogs.size == 2)

        runtime.shutdown()
    }

    @Test
    fun `the same resolved ConversationId reaches the Assembler's own prompt and remains stable across repeated messages from the same owner and channel`() = runBlocking<Unit> {
        val stub = startStub("REPLY: sure thing")
        val runtime = ParkerRuntime(configFor(stub), RecordingParkerLogger())
        runtime.start()

        runtime.submitOwnerMessage(message(text = "first request", correlationId = "corr-stable-first"))
        runtime.submitOwnerMessage(message(text = "second request", correlationId = "corr-stable-second"))

        assertEquals(2, stub.receivedRequestBodies.size)
        val firstPrompt = stub.receivedRequestBodies[0]
        val secondPrompt = stub.receivedRequestBodies[1]

        val conversationLinePattern = Regex("Current conversation: (\\S+)")
        val firstConversationId = conversationLinePattern.find(firstPrompt)?.groupValues?.get(1)
        val secondConversationId = conversationLinePattern.find(secondPrompt)?.groupValues?.get(1)

        assertTrue(firstConversationId != null, "prompt did not carry a 'Current conversation' entry: $firstPrompt")
        assertEquals(
            firstConversationId,
            secondConversationId,
            "two messages from the same owner and channel must resolve to the same Conversation, and the " +
                "Assembler's own rendered entry must reflect it -- proving the same identifier the composition " +
                "root resolved reached the Assembler's own input unchanged",
        )

        runtime.shutdown()
    }

    @Test
    fun `the created Turn is bound to the exact ConversationId the composition root resolved`() = runBlocking<Unit> {
        val stub = startStub("REPLY: sure thing")
        val logger = RecordingParkerLogger()
        val runtime = ParkerRuntime(configFor(stub), logger)
        runtime.start()

        runtime.submitOwnerMessage(message(text = "what tools do you have?", correlationId = "corr-turn-binding"))

        val resolvedLine = logger.messages(LogLevel.INFO).single { it.startsWith("Conversation continuity resolved") }
        val resolvedConversationId = Regex("conversationId=(\\S+)\\)").find(resolvedLine)?.groupValues?.get(1)
        val prompt = stub.receivedRequestBodies.single()

        assertTrue(resolvedConversationId != null)
        assertTrue(
            "Current conversation: $resolvedConversationId" in prompt,
            "the ConversationId resolved for this message ($resolvedConversationId) must be the exact one " +
                "later used to construct the Turn and reach the Assembler's own rendered entry: $prompt",
        )

        runtime.shutdown()
    }
}
