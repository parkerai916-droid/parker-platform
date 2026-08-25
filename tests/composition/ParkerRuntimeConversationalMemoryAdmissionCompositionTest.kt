package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RelationshipEndpoint
import parker.core.runtime.ConversationReplyCoordinator
import parker.core.runtime.DurableMemoryCore
import parker.core.runtime.DurableKnowledgeItemPersistence
import parker.core.runtime.MemoryAdmissionCoordinator
import parker.core.runtime.MemoryAdmissionOutcome
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Parker Conversational Memory Bridge, Admission Unit. End-to-end
 * composition test against the real, fully-wired production runtime --
 * `ParkerRuntime.submitOwnerMessage` -> real `ConversationTurnReasoningCoordinator`
 * -> real `ModelReasoningProvider` against [StubModelServer] -> real
 * `TaggedReasoningResponseParser` -> real `ConversationReplyCoordinator`
 * -> real `MemoryAdmissionCoordinator` -> real `DurableMemoryCore` ->
 * real `KnowledgeSubmission` -- not fakes, mirroring
 * [ParkerRuntimeConversationPipelineTest]'s own established style exactly.
 *
 * Gap #54 Memory Retrieval Operationalisation Unit 5 replaces this
 * suite's historical continued-failure expectation after accepted Units
 * 1–4 lawfully operationalised candidate evidence retrieval. Success is
 * proved from the authoritative composed Knowledge persistence and the
 * genuine durable Memory Core assertion, never inferred from reply text.
 * This remains admission/promotion verification only: it makes no claim
 * that the proposition can later be discovered or recalled in conversation.
 */
class ParkerRuntimeConversationalMemoryAdmissionCompositionTest {

    private val ownerPrincipalId = "user.owner-memory-admission-composition-test"
    private val channelModuleId = "channel.local-text-memory-admission-composition-test"
    private var server: StubModelServer? = null

    @AfterTest
    fun tearDown() {
        server?.close()
    }

    private fun startStub(responseFieldValue: String): StubModelServer =
        StubModelServer.start(responseFieldValue).also { server = it }

    private fun configFor(
        stub: StubModelServer,
        memoryCoreDurabilityLogPath: String,
        knowledgeItemDurabilityLogPath: String = Files.createTempDirectory("knowledge-items-test").resolve("items.log").toString(),
    ) = ParkerRuntimeConfig(
        modelEndpointUrl = stub.endpointUrl,
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        ownerDisplayName = "Test Owner",
        localTextChannelModuleId = channelModuleId,
        evidenceStorageRootPath = Files.createTempDirectory("memory-admission-composition-evidence-storage").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("memory-admission-composition-evidence-storage-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("memory-admission-composition-evidence-storage-manifest-derivative-generation").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("memory-admission-composition-evidence-storage-manifest-derivative-generation-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("memory-admission-composition-evidence-storage-manifest-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath =
            Files.createTempDirectory("memory-admission-composition-evidence-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = memoryCoreDurabilityLogPath,
        knowledgeItemDurabilityLogPath = knowledgeItemDurabilityLogPath,
    )

    private fun message(text: String) = InboundOwnerMessage(
        channelId = ModuleId(channelModuleId),
        senderPrincipalId = PrincipalId(ownerPrincipalId),
        text = text,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId("corr-memory-admission-${System.nanoTime()}"),
    )

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    @Test
    fun `an explicit owner REMEMBER instruction creates durable evidence and a persisted promoted KnowledgeItem before success is reported`() = runBlocking<Unit> {
        val proposition = "the test lighthouse is painted orange"
        val stub = startStub("REPLY: model downgrade must not be reached")
        val logPath = Files.createTempDirectory("memory-admission-composition-single").resolve("memory-core.log").toString()
        val logger = RecordingParkerLogger()
        val ownerSink = RecordingOwnerNotificationSink()
        val runtime = ParkerRuntime(configFor(stub, logPath), logger, ownerSink)
        runtime.start()

        val ownerMessage = message("Remember the test lighthouse is painted orange.")
        val outcome = runtime.submitOwnerMessage(ownerMessage)

        val delivered = assertIs<ParkerRuntimeOutcome.Delivered>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, delivered.executionResult.status)
        assertEquals(listOf("I'll remember that."), ownerSink.notifications)
        assertTrue(stub.receivedRequestBodies.isEmpty(), "deterministic owner intent must bypass the model")

        val knowledgeRetrieval = runtime.privateField<Any>("knowledgeRetrieval")
        val persistence = knowledgeRetrieval.privateField<DurableKnowledgeItemPersistence>("persistence")
        val persistedItems = persistence.findAll()
        assertEquals(1, persistedItems.size, "success must correspond to one item in the authoritative composed persistence")

        val item = persistedItems.single()
        val assertionReference = assertIs<MemoryCoreRecordReference.ToAssertion>(item.evidenceReference)
        val memoryCore = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
            .privateField<DurableMemoryCore>("memoryCore")
        val assertion = assertNotNull(
            memoryCore.getAssertion(PrincipalId(ownerPrincipalId), assertionReference.assertionId),
            "the persisted item must reference an assertion that genuinely exists in the composed durable Memory Core",
        )
        assertEquals(proposition, assertion.statement)
        assertEquals(assertion.provenanceId, item.provenanceReference.provenanceId)

        val promotion = assertIs<KnowledgePromotion>(item.history.single())
        assertEquals(item.knowledgeId, promotion.knowledgeId)
        assertEquals(assertionReference, promotion.evidenceReference)
        assertEquals(item.evidentialState, promotion.resultingState)
        assertTrue(
            promotion.basis.contains("explicit, deterministic owner instruction"),
            "promotion must disclose the existing explicit-owner-instruction exception: ${promotion.basis}",
        )

        // Re-read after the complete conversation call and evidence inspection: this is repository
        // state owned by the running production composition, not an evaluator-local return value.
        assertEquals(item, persistence.findAll().single())

        val evidenceIntelligenceOutcome = runtime.analyseEvidence(
            PrincipalId(ownerPrincipalId),
            EvidenceAnalysisRequest(
                analysisKind = "unit-5-same-runtime-denial",
                requestingPrincipalId = PrincipalId(ownerPrincipalId),
                memoryCoreReferences = listOf(
                    RelationshipEndpoint(RelationshipEndpoint.ASSERTION, assertionReference.assertionId.value),
                ),
            ),
        )
        val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(evidenceIntelligenceOutcome)
        assertTrue(
            completed.acceptanceOutcomes.isEmpty(),
            "Evidence Intelligence must remain unable to resolve the same genuine assertion candidate evaluation used",
        )

        runtime.shutdown()
    }

    @Test
    fun `public REMEMBER promotion is recovered by a new complete runtime sharing the durability files`() = runBlocking<Unit> {
        val proposition = "My restart-proof notebook is green."
        val stub = startStub("REMEMBER: $proposition")
        val directory = Files.createTempDirectory("knowledge-item-runtime-restart")
        val memoryLog = directory.resolve("memory-core.log").toString()
        val knowledgeLog = directory.resolve("knowledge-items.log").toString()

        val runtimeA = ParkerRuntime(
            configFor(stub, memoryLog, knowledgeLog),
            RecordingParkerLogger(),
            RecordingOwnerNotificationSink(),
        )
        runtimeA.start()
        val delivered = assertIs<ParkerRuntimeOutcome.Delivered>(
            runtimeA.submitOwnerMessage(message("Remember that my restart-proof notebook is green.")),
        )
        assertEquals(ExecutionResultStatus.SUCCESS, delivered.executionResult.status)
        val itemBeforeRestart = runtimeA.privateField<Any>("knowledgeRetrieval")
            .privateField<DurableKnowledgeItemPersistence>("persistence").findAll().single()
        runtimeA.shutdown()

        val runtimeB = ParkerRuntime(
            configFor(stub, memoryLog, knowledgeLog),
            RecordingParkerLogger(),
            RecordingOwnerNotificationSink(),
        )
        runtimeB.start()

        val retrieval = runtimeB.privateField<Any>("knowledgeRetrieval")
        val recoveredPersistence = retrieval.privateField<DurableKnowledgeItemPersistence>("persistence")
        assertEquals(itemBeforeRestart, recoveredPersistence.findAll().single())

        val acceptanceCoordinator = runtimeB.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val submissionPersistence = acceptanceCoordinator.privateField<Any>("knowledgeSubmission")
            .privateField<Any>("persistence")
        val reasoningPersistence = runtimeB.privateField<Any>("reasoningContextAssembler")
            .privateField<Any>("knowledgeSource")
            .privateField<Any>("persistence")
        assertSame(recoveredPersistence, submissionPersistence)
        assertSame(recoveredPersistence, reasoningPersistence)

        val assertionReference = assertIs<MemoryCoreRecordReference.ToAssertion>(itemBeforeRestart.evidenceReference)
        val recoveredMemoryCore = acceptanceCoordinator.privateField<DurableMemoryCore>("memoryCore")
        assertNotNull(recoveredMemoryCore.getAssertion(PrincipalId(ownerPrincipalId), assertionReference.assertionId))
        runtimeB.shutdown()
    }

    @Test
    fun `an unregistered Principal cannot admit or persist Knowledge through the real composed coordinator`() = runBlocking<Unit> {
        val stub = startStub("REPLY: unused")
        val logPath = Files.createTempDirectory("memory-admission-composition-unregistered").resolve("memory-core.log").toString()
        val runtime = ParkerRuntime(configFor(stub, logPath), RecordingParkerLogger(), RecordingOwnerNotificationSink())
        runtime.start()

        val knowledgeRetrieval = runtime.privateField<Any>("knowledgeRetrieval")
        val persistence = knowledgeRetrieval.privateField<DurableKnowledgeItemPersistence>("persistence")
        assertTrue(persistence.findAll().isEmpty())

        val replyCoordinator = runtime.privateField<ConversationReplyCoordinator>("conversationReplyCoordinator")
        val admissionCoordinator = replyCoordinator.privateField<MemoryAdmissionCoordinator>("memoryAdmissionCoordinator")
        val denied = admissionCoordinator.admit(
            requestingPrincipalId = PrincipalId("user.unregistered-unit-5"),
            correlationId = "corr-unregistered-unit-5",
            instructionText = "This must not become persisted knowledge.",
        )

        assertIs<MemoryAdmissionOutcome.NotAuthorised>(denied)
        assertTrue(persistence.findAll().isEmpty(), "a denied accountable Principal must not persist Knowledge")

        runtime.shutdown()
    }

    @Test
    fun `an ordinary REPLY is unaffected by this Unit's own new Remember branch -- no regression`() = runBlocking<Unit> {
        val stub = startStub("REPLY: good morning to you too!")
        val logPath = Files.createTempDirectory("memory-admission-composition-reply-regression").resolve("memory-core.log").toString()
        val ownerSink = RecordingOwnerNotificationSink()
        val runtime = ParkerRuntime(configFor(stub, logPath), RecordingParkerLogger(), ownerSink)
        runtime.start()

        val outcome = runtime.submitOwnerMessage(message("good morning parker"))

        val delivered = assertIs<ParkerRuntimeOutcome.Delivered>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, delivered.executionResult.status)
        assertEquals(listOf("good morning to you too!"), ownerSink.notifications)

        runtime.shutdown()
    }

    @Test
    fun `ordinary model-produced Remember still reaches the existing admission gate unchanged`() = runBlocking<Unit> {
        // The first test proves full promotion and persistence; this test retains the earlier
        // direct regression that the admission gate itself remains reachable and approving.
        val stub = startStub("REMEMBER: Stellar is my dog")
        val logPath = Files.createTempDirectory("memory-admission-composition-gate-reached").resolve("memory-core.log").toString()
        val ownerSink = RecordingOwnerNotificationSink()
        val runtime = ParkerRuntime(configFor(stub, logPath), RecordingParkerLogger(), ownerSink)
        runtime.start()

        runtime.submitOwnerMessage(message("This is an ordinary non-directive turn"))

        val replyText = ownerSink.notifications.single()
        assertEquals(1, stub.receivedRequestBodies.size, "ordinary input must reach the model exactly once")
        assertTrue(
            "not able to store that right now" !in replyText,
            "a reply naming this Unit's own admission-gate denial must never occur when the real policy approves WRITE/MEMORY (already an APPROVED rule) -- actual: $replyText",
        )

        runtime.shutdown()
    }
}
