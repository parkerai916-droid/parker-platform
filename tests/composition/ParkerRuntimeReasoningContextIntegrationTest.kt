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
 * Sprint 11, Unit 3 integration test: confirms
 * `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 9's three
 * numbered guarantees hold against the real, running
 * [ParkerRuntime] -- not merely against [DefaultReasoningContextAssembler]
 * in isolation (see `tests/runtime/DefaultReasoningContextAssemblerTest.kt`
 * for that).
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
}
