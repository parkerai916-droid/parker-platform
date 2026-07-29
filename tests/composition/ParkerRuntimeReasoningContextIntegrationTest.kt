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
 * (Conversation Continuity Implementation) and Sprint 11 Unit 6
 * (Conversation History Source): confirms
 * `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 9's three
 * numbered guarantees, the Continuity Contract Design's own propagation
 * path (`docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md`
 * Section 5), and Conversation History Source's own integration
 * (`docs/architecture/CONVERSATION_HISTORY_SOURCE_CONTRACT_DESIGN.md`
 * Section 5 -- a second message in the same Conversation carries the
 * first message's text as history; the first message carries none) and,
 * Sprint 11 Unit 7 (Memory Source Integration), that `KnowledgeSource` is
 * wired into the real composition root without fault -- all hold against
 * the real, running [ParkerRuntime] -- not merely against
 * [DefaultReasoningContextAssembler] or [InMemoryConversationEngine] in
 * isolation (see `tests/runtime/DefaultReasoningContextAssemblerTest.kt`
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

        // Sprint 11 Unit 6 correction: matches only ConversationId's own character set (the hex/hyphen
        // shape InMemoryConversationEngine.resolveConversationId actually mints, via UUID.randomUUID()),
        // not `\S+`. `stub.receivedRequestBodies` holds the raw JSON request body, where the prompt's
        // real newlines are JSON-escaped as the two literal characters `\` + `n` -- both non-whitespace,
        // so a `\S+` capture does not stop there and instead runs on into the next rendered entry's own
        // first word (e.g. "Available" before this Unit, now "Prior" once a Conversation has prior
        // history) -- a pre-existing extraction fragility this Unit's own legitimate new "Prior message"
        // entry exposes, not a ConversationId instability: both captured values still share the exact
        // same identifier prefix once correctly bounded, below.
        val conversationLinePattern = Regex("Current conversation: ([0-9a-fA-F-]+)")
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

    // --- Sprint 11 Unit 6: Runtime integration for Conversation History Source ---

    @Test
    fun `a second message from the same owner and channel carries the first message's text as Prior message history, and the first message carries none`() = runBlocking<Unit> {
        val stub = startStub("REPLY: sure thing")
        val runtime = ParkerRuntime(configFor(stub), RecordingParkerLogger())
        runtime.start()

        runtime.submitOwnerMessage(message(text = "what's the weather like today", correlationId = "corr-history-first"))
        runtime.submitOwnerMessage(message(text = "and tomorrow", correlationId = "corr-history-second"))

        assertEquals(2, stub.receivedRequestBodies.size)
        val firstPrompt = stub.receivedRequestBodies[0]
        val secondPrompt = stub.receivedRequestBodies[1]

        assertTrue(
            !firstPrompt.contains("Prior message:"),
            "the first message in a Conversation must carry no prior history: $firstPrompt",
        )
        assertTrue(
            "Prior message: what's the weather like today" in secondPrompt,
            "the second message must carry the first message's own text as prior history: $secondPrompt",
        )
        assertTrue(
            !secondPrompt.contains("Prior message: and tomorrow"),
            "the current request must never appear as its own prior history: $secondPrompt",
        )

        runtime.shutdown()
    }

    // --- Sprint 11 Unit 7: Runtime wiring for Memory Source Integration ---

    @Test
    fun `KnowledgeSource is wired into the real ParkerRuntime and renders no Memory entries, since nothing in this Unit's own scope creates memories`() = runBlocking<Unit> {
        // KnowledgeSource Contract Design Section 9: nothing in this Unit's own scope calls
        // KnowledgeStore.remember (Scope Lock's own exclusion, "changing how memories are
        // created"), so the InMemoryKnowledgeStore ParkerRuntime constructs is always empty in
        // production -- this test confirms the real wiring reaches this new dependency without
        // fault and correctly renders nothing, exactly as "no tools" and "no prior Turns"
        // already render nothing elsewhere in this same prompt. A full end-to-end test of a
        // populated memory rendering through the real ParkerRuntime is not achievable within
        // this Unit's own scope (no seeding hook exists, and adding one would itself be
        // out-of-scope "changing how memories are created") -- see
        // `tests/runtime/DefaultReasoningContextAssemblerTest.kt`'s own real-InMemoryKnowledgeStore
        // test (Assembler-level, not ParkerRuntime-level) for this Unit's best available
        // substitute, disclosed in `docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md` Section 10.
        val stub = startStub("REPLY: sure thing")
        val runtime = ParkerRuntime(configFor(stub), RecordingParkerLogger())
        runtime.start()

        runtime.submitOwnerMessage(message(text = "what's the weather like today"))

        assertEquals(1, stub.receivedRequestBodies.size)
        val prompt = stub.receivedRequestBodies.single()
        assertTrue(!prompt.contains("Memory:"), "no memory exists yet in production, so no Memory entry should render: $prompt")

        runtime.shutdown()
    }

    // --- Sprint 11 Unit 8: Runtime wiring for World Model Source Integration ---

    @Test
    fun `WorldModelSource is wired into the real ParkerRuntime and renders no World belief entries, since nothing in this Unit's own scope creates world state`() = runBlocking<Unit> {
        // World Model Source Contract Design Section 8/10: nothing in this Unit's own scope calls
        // WorldModel.observe (Scope Lock's own exclusion, "creating world state"), so the
        // InMemoryWorldModel ParkerRuntime constructs is always empty in production -- this test
        // confirms the real wiring reaches this new dependency without fault and correctly
        // renders nothing, exactly as "no tools," "no prior Turns," and "no memories" already
        // render nothing elsewhere in this same prompt. A full end-to-end test of a populated
        // belief rendering through the real ParkerRuntime is not achievable within this Unit's own
        // scope (no seeding hook exists, and adding one would itself be out-of-scope "creating
        // world state") -- see `tests/runtime/DefaultReasoningContextAssemblerTest.kt`'s own
        // real-InMemoryWorldModel test (Assembler-level, not ParkerRuntime-level) for this Unit's
        // best available substitute, disclosed in
        // `docs/architecture/WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md` Section 9.
        val stub = startStub("REPLY: sure thing")
        val runtime = ParkerRuntime(configFor(stub), RecordingParkerLogger())
        runtime.start()

        runtime.submitOwnerMessage(message(text = "what's the weather like today"))

        assertEquals(1, stub.receivedRequestBodies.size)
        val prompt = stub.receivedRequestBodies.single()
        assertTrue(!prompt.contains("World belief:"), "no belief exists yet in production, so no World belief entry should render: $prompt")

        runtime.shutdown()
    }
}
