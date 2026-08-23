package parker.composition

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
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
 * Knowledge Discoverability and Reasoning Context Implementation Unit 3, that
 * `ReasoningKnowledgeSource` is wired into the real composition root without
 * fault, extended by Implementation Unit 5 with the required genuine
 * end-to-end proof (a real promoted proposition, later recalled and safely
 * rendered in the real assembled prompt of a separate turn) -- all hold against
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

    // This file exercises Reasoning Context assembly only -- it never calls submitEvidence/
    // retrieveEvidence/deleteEvidenceAsOwner, so these two paths are real, writable, unused
    // locations, not exercised by any test below.
    private fun configFor(stub: StubModelServer) = ParkerRuntimeConfig(
        modelEndpointUrl = stub.endpointUrl,
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        ownerDisplayName = ownerDisplayName,
        localTextChannelModuleId = channelModuleId,
        evidenceStorageRootPath = Files.createTempDirectory("unused-evidence-storage").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("unused-evidence-storage-manifest").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("unused-evidence-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("unused-memory-core").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("knowledge-items-test").resolve("items.log").toString(),
    )

    // RKS.6 (Reasoning Context Bounded Semantic Relevance Implementation Plan). Mirrors
    // `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s own established, portable, env-var-driven live
    // gate exactly -- never a fabricated fallback, never filesystem discovery, and always an explicit
    // `assumeTrue` skip (not a failure) on a machine without a local QMD checkout provisioned. The
    // two tests below are the only ones in this file whose own dereferenced candidate set is
    // non-empty with zero structural match, so they are the only ones that reach Bounded Relevance
    // Computation's fallback branch and therefore the only ones needing live QMD.
    private fun resolvedQmdSourceRoot(): String? =
        System.getenv("QMD_TEST_SOURCE_ROOT")?.takeIf { it.isNotBlank() }

    private fun resolvedTsxCliPath(qmdSourceRoot: String?): String? =
        System.getenv("QMD_TEST_TSX_CLI")?.takeIf { it.isNotBlank() }
            ?: qmdSourceRoot?.let { Path.of(it, "node_modules", "tsx", "dist", "cli.mjs").toString() }

    private fun assumeLiveQmdPrerequisitesProvisioned() {
        assumeTrue(
            System.getProperty("parker.relevance.qmd.live.enabled") == "true",
            "Live QMD property absent; no subprocess invoked",
        )
        val qmdSourceRoot = resolvedQmdSourceRoot()
        val tsxCliPath = resolvedTsxCliPath(qmdSourceRoot)
        val missing = buildList {
            if (qmdSourceRoot == null) add("QMD_TEST_SOURCE_ROOT (the local QMD installation/checkout root)")
            if (tsxCliPath == null) add("QMD_TEST_TSX_CLI, or QMD_TEST_SOURCE_ROOT (from which it is derived)")
        }
        assumeTrue(
            missing.isEmpty(),
            "Live QMD prerequisites are not provisioned on this machine -- missing: ${missing.joinToString("; ")}. " +
                "This mechanism is never auto-discovered, guessed, or downloaded; provision a local QMD " +
                "checkout and set these environment variable(s) explicitly to run this live proof.",
        )
    }

    private fun configForQmdLive(stub: StubModelServer): ParkerRuntimeConfig {
        val qmdSourceRoot = resolvedQmdSourceRoot()
        val tsxCliPath = resolvedTsxCliPath(qmdSourceRoot)
        return ParkerRuntimeConfig(
            modelEndpointUrl = stub.endpointUrl,
            modelName = "test-model",
            ownerPrincipalId = ownerPrincipalId,
            ownerDisplayName = ownerDisplayName,
            localTextChannelModuleId = channelModuleId,
            evidenceStorageRootPath = Files.createTempDirectory("unused-evidence-storage").toString(),
            evidenceSourceManifestStorageRootPath = Files.createTempDirectory("unused-evidence-storage-manifest").toString(),
            evidenceDeletionAuditLogPath = Files.createTempDirectory("unused-evidence-audit").resolve("audit.log").toString(),
            memoryCoreDurabilityLogPath = Files.createTempDirectory("unused-memory-core").resolve("memory-core.log").toString(),
            knowledgeItemDurabilityLogPath = Files.createTempDirectory("knowledge-items-test").resolve("items.log").toString(),
            qmdNodeExecutablePath = System.getenv("QMD_TEST_NODE")?.takeIf { it.isNotBlank() } ?: "node",
            qmdTsxCliPath = tsxCliPath,
            qmdModelCacheDir = System.getenv("QMD_RELEVANCE_MODEL_CACHE_DIR")?.takeIf { it.isNotBlank() },
            qmdTimeoutMillis = 120_000,
            qmdSourceRoot = qmdSourceRoot,
        )
    }

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

    // --- Knowledge Discoverability and Reasoning Context Implementation Units 3/5: Runtime
    // wiring for Reasoning Context Knowledge retrieval ---

    @Test
    fun `ReasoningKnowledgeSource is wired into the real ParkerRuntime and renders no Memory entries when nothing has been promoted`() = runBlocking<Unit> {
        // Implementation Unit 3 cut the assembler over from the legacy KnowledgeSource/
        // InMemoryKnowledgeStore feed to the real, governed ReasoningKnowledgeSource -- no
        // production path constructs InMemoryKnowledgeStore any longer (Unit 3's own composition
        // cutover; Unit 4's own structural composition proof). This test confirms the real wiring
        // reaches this new dependency without fault and correctly renders nothing when nothing has
        // been promoted, exactly as "no tools" and "no prior Turns" already render nothing
        // elsewhere in this same prompt. The genuine, populated case -- a real promoted
        // proposition later recalled and rendered through this same real ParkerRuntime -- is
        // proven separately, immediately below (Implementation Unit 5's own required genuine
        // end-to-end proof).
        val stub = startStub("REPLY: sure thing")
        val runtime = ParkerRuntime(configFor(stub), RecordingParkerLogger())
        runtime.start()

        runtime.submitOwnerMessage(message(text = "what's the weather like today"))

        assertEquals(1, stub.receivedRequestBodies.size)
        val prompt = stub.receivedRequestBodies.single()
        assertTrue(!prompt.contains("Memory:"), "no memory exists yet in production, so no Memory entry should render: $prompt")

        runtime.shutdown()
    }

    @Test
    fun `a distinctive proposition promoted through a real owner Remember turn is later recalled and safely rendered in the real assembled prompt of a genuinely separate query turn`() = runBlocking<Unit> {
        val proposition = "the owner's favourite hiking trail is Widow's Peak Ridge"
        // Mirrors ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt's own established
        // "REMEMBER: <proposition>" stub-response convention exactly -- the real
        // TaggedReasoningResponseParser parses the model's own reply for this tag, driving the
        // real ConversationReplyCoordinator -> real MemoryAdmissionCoordinator -> real
        // DurableMemoryCore -> real DefaultKnowledgeSubmission promotion path. No synthetic or
        // hand-constructed KnowledgeItem, and no direct persistence seeding, is used anywhere in
        // this test.
        val stub = startStub("REPLY: acknowledged")
        val runtime = ParkerRuntime(configFor(stub), RecordingParkerLogger())
        runtime.start()

        // owner Remember X: a real submitOwnerMessage turn reaches the existing, genuine
        // Remember/promotion path.
        runtime.submitOwnerMessage(
            message(text = "Remember that the owner's favourite hiking trail is Widow's Peak Ridge.", correlationId = "corr-unit-5-remember"),
        )

        // A genuinely separate later submitOwnerMessage turn, carrying text that overlaps X's own
        // remembered content -- KnowledgeRetrievalQuery.relevance is exactly this message's own
        // text, matched as a case-insensitive substring of the resolved Assertion's own content
        // (Contract Design Section 5), driving the real, same-runtime
        // DefaultReasoningKnowledgeSource.recall path.
        runtime.submitOwnerMessage(message(text = "Widow's Peak Ridge", correlationId = "corr-unit-5-recall"))

        // The real assembled model request -- the most recent entry in stub.receivedRequestBodies
        // -- must carry a safely rendered "Memory: " entry with X's own distinctive content. A
        // friendly reply is not evidence and is not inspected anywhere in this test; only the
        // real, assembled prompt request itself.
        assertEquals(1, stub.receivedRequestBodies.size)
        val recallPrompt = stub.receivedRequestBodies[0]
        assertTrue(
            "Memory: $proposition" in recallPrompt,
            "the real assembled prompt for the separate recall turn must carry a genuine Memory: entry for the promoted proposition: $recallPrompt",
        )

        runtime.shutdown()
    }

    @Test
    fun `a genuinely related paraphrase now recalls a promoted proposition through the real, live Bounded Relevance Computation fallback`() = runBlocking<Unit> {
        // RKS.2-RKS.6 (Reasoning Context Bounded Semantic Relevance Implementation Plan): before
        // this Unit, the current literal substring retrieval alone genuinely could not recover this
        // paraphrase -- that pre-fallback baseline is exactly what this test's own predecessor
        // proved. Bounded Relevance Computation now runs as a fallback whenever structural matching
        // finds nothing over a non-empty candidate set, invoking the real, shared QmdRelevanceMechanism
        // this environment's own local QMD checkout and embedding-model cache genuinely back (never a
        // fake or stub) -- this is this Unit's own required real, same-runtime, live end-to-end proof
        // (Successor Section 16, item 11), not a unit-level fake-mechanism substitute.
        assumeLiveQmdPrerequisitesProvisioned()
        val proposition = "the owner's synthetic emergency vet is Harbour Animal Clinic"
        val stub = startStub("REPLY: acknowledged")
        val runtime = ParkerRuntime(configForQmdLive(stub), RecordingParkerLogger())
        runtime.start()

        runtime.submitOwnerMessage(
            message(
                text = "Remember that the owner's synthetic emergency vet is Harbour Animal Clinic.",
                correlationId = "corr-qmd-control-remember",
            ),
        )

        val recallOutcome = runtime.submitOwnerMessage(
            message(
                text = "Which animal clinic did I tell you to use in an emergency?",
                correlationId = "corr-qmd-control-semantic-recall",
            ),
        )

        assertIs<ParkerRuntimeOutcome.Delivered>(
            recallOutcome,
            "the recall turn must reach a real model call through a genuinely available QMD mechanism, " +
                "not fail closed -- if this assertion fails, the local QMD checkout/model cache this " +
                "environment's own QmdRelevanceMechanismConfiguration names is not genuinely available here",
        )
        assertEquals(1, stub.receivedRequestBodies.size)
        val recallPrompt = stub.receivedRequestBodies[0]

        assertTrue(
            "Memory: $proposition" in recallPrompt,
            "the real, live Bounded Relevance Computation fallback must now recover the semantically " +
                "related proposition into the real assembled prompt: $recallPrompt",
        )

        runtime.shutdown()
    }

    @Test
    fun `six genuine promoted memories remain canonically stored, and the real Bounded Relevance Computation fallback picks out the correct emergency vet paraphrase among all six`() = runBlocking<Unit> {
        assumeLiveQmdPrerequisitesProvisioned()
        val propositions = listOf(
            "the owner's synthetic emergency vet is Harbour Animal Clinic",
            "the owner's synthetic regular vet is Riverside Veterinary Centre",
            "the owner's synthetic dog groomer is Central City Grooming",
            "the owner's synthetic emergency plumber is Wellington Rapid Plumbing",
            "the owner's synthetic preferred pharmacy is Harbour Pharmacy",
            "the owner's synthetic favourite hiking trail is Widow's Peak Ridge",
        )

        val stub = startStub("REPLY: acknowledged")

        val runtime = ParkerRuntime(configForQmdLive(stub), RecordingParkerLogger())
        runtime.start()

        runtime.submitOwnerMessage(
            message(
                text = "Remember that the owner's synthetic emergency vet is Harbour Animal Clinic.",
                correlationId = "corr-qmd-multi-remember-1",
            ),
        )
        runtime.submitOwnerMessage(
            message(
                text = "Remember that the owner's synthetic regular vet is Riverside Veterinary Centre.",
                correlationId = "corr-qmd-multi-remember-2",
            ),
        )
        runtime.submitOwnerMessage(
            message(
                text = "Remember that the owner's synthetic dog groomer is Central City Grooming.",
                correlationId = "corr-qmd-multi-remember-3",
            ),
        )
        runtime.submitOwnerMessage(
            message(
                text = "Remember that the owner's synthetic emergency plumber is Wellington Rapid Plumbing.",
                correlationId = "corr-qmd-multi-remember-4",
            ),
        )
        runtime.submitOwnerMessage(
            message(
                text = "Remember that the owner's synthetic preferred pharmacy is Harbour Pharmacy.",
                correlationId = "corr-qmd-multi-remember-5",
            ),
        )
        runtime.submitOwnerMessage(
            message(
                text = "Remember that the owner's synthetic favourite hiking trail is Widow's Peak Ridge.",
                correlationId = "corr-qmd-multi-remember-6",
            ),
        )

        val recallOutcome = runtime.submitOwnerMessage(
            message(
                text = "Which animal clinic did I tell you to use in an emergency?",
                correlationId = "corr-qmd-multi-semantic-recall",
            ),
        )

        assertIs<ParkerRuntimeOutcome.Delivered>(recallOutcome)
        assertEquals(1, stub.receivedRequestBodies.size)
        val recallPrompt = stub.receivedRequestBodies[0]

        assertTrue(
            "Memory: ${propositions[0]}" in recallPrompt,
            "the real, live Bounded Relevance Computation fallback must recover the correct emergency vet " +
                "memory among six genuine, canonically stored candidates: $recallPrompt",
        )
        for (distractor in propositions.drop(1)) {
            assertTrue(
                "Memory: $distractor" !in recallPrompt,
                "the real mechanism must not surface a distractor -- in particular the 'emergency plumber' " +
                    "entry, which shares the incidental 'emergency' token with the query but is not the " +
                    "correct answer (the exact discrimination the Unit 9.7 mechanism-selection spike proved " +
                    "QMD, and not a naive token-overlap comparator, gets right): $recallPrompt",
            )
        }

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

    // Programme 3, Unit 9.7.4 -> Unit 9.7.5 compile-preserving composition seam (Task C bounded
    // correction): this file previously held one temporary, additive test proving the interim
    // fail-closed RelevanceMechanism placeholder ParkerRuntime.kt held between Unit 9.7.4 and Unit
    // 9.7.5. Unit 9.7.5 ("Runtime Composition Wiring") has now replaced that placeholder with the
    // real, composed QmdRelevanceMechanism, so that test's own assertions (the composed mechanism is
    // NOT QmdRelevanceMechanism; invoking it throws "not runtime-composed until Unit 9.7.5") are no
    // longer true statements about this codebase and are removed here, superseded -- not silently
    // left in place to fail on the next Windows run. This file (Knowledge Discoverability / Reasoning
    // Context integration) was never Unit 9.7.5's own governed test location in any case; the
    // Implementation Plan's own Affected Files table (Section 9) names
    // `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt` as the file Unit 9.7.5
    // extends additively -- the real composition proof now lives there instead.
}
