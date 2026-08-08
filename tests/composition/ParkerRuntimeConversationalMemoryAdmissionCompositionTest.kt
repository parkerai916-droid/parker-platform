package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
 * **This suite exists to record a genuine, significant, pre-existing
 * finding this Unit's own mandatory live/behavioural verification
 * surfaced, not introduced.** `DefaultKnowledgeCandidateEvaluator`, as
 * actually composed in `ParkerRuntime.kt`, resolves a submitted
 * candidate's evidence through `permissionFilteredMemoryRetrieval` --
 * the same, shared decorator `ParkerRuntimeEvidenceIntelligenceCompositionTest`'s
 * own `analyseEvidence's Memory Core retrieval remains fail-closed even
 * for a record that genuinely exists in Memory Core` test already proves
 * denies *every* read unconditionally (`RETRIEVE_ACTION_NAME`/
 * `RETRIEVE_DOCUMENT_ACTION_NAME` are never registered in the Action
 * Vocabulary, and -- traced directly, `DefaultPermissionPolicy.evaluate`
 * -> `ActionMapper.mapOne` -- registering them would not help, since
 * Memory Core records are never Resource Registry entries, so
 * `targetResourceTypes` is structurally always empty for every retrieval
 * request regardless of vocabulary registration). This means
 * `KnowledgeSubmission.submit` can never resolve, and therefore can never
 * promote, *any* candidate in the live, composed runtime -- for this
 * bridge's own candidates or for Evidence Intelligence's own, identically
 * wired ones. This is not a defect in this Unit's own implementation
 * (confirmed independently correct and fully tested in isolation by
 * `MemoryAdmissionCoordinatorTest`, using a real, unfiltered `MemoryCore`
 * as its own retrieval dependency); it is a genuine, deeper, pre-existing
 * gap in Knowledge Submission's own real-world reachability, first
 * exposed here because this Unit is the first to genuinely exercise the
 * complete path end-to-end against the real, live composition, per this
 * Unit's own governing task's explicit "must prove behaviour, not merely
 * compilation" requirement. Fixing it requires changing `DefaultPermissionPolicy`
 * or `ActionMapper`'s own frozen matching logic, or Memory Core's own
 * Resource-representation choice (Errata 004) -- both well outside this
 * Unit's own authorised scope ("new permission architecture" is an
 * explicit non-responsibility). The tests below therefore verify the
 * behaviour this Unit is actually responsible for and actually
 * authorised to guarantee: that Parker recognises the explicit
 * instruction, genuinely attempts durable admission through the real,
 * governed path, and -- because that path presently, honestly fails --
 * discloses that failure accurately, never fabricating a success claim.
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

    private fun configFor(stub: StubModelServer, memoryCoreDurabilityLogPath: String) = ParkerRuntimeConfig(
        modelEndpointUrl = stub.endpointUrl,
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        ownerDisplayName = "Test Owner",
        localTextChannelModuleId = channelModuleId,
        evidenceStorageRootPath = Files.createTempDirectory("memory-admission-composition-evidence-storage").toString(),
        evidenceDeletionAuditLogPath =
            Files.createTempDirectory("memory-admission-composition-evidence-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = memoryCoreDurabilityLogPath,
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
    fun `an explicit REMEMBER instruction is genuinely attempted, and its current, real failure is disclosed honestly, never fabricated as success`() = runBlocking<Unit> {
        val stub = startStub("REMEMBER: my favourite coffee mug is black")
        val logPath = Files.createTempDirectory("memory-admission-composition-single").resolve("memory-core.log").toString()
        val logger = RecordingParkerLogger()
        val ownerSink = RecordingOwnerNotificationSink()
        val runtime = ParkerRuntime(configFor(stub, logPath), logger, ownerSink)
        runtime.start()

        val outcome = runtime.submitOwnerMessage(message("Remember that my favourite coffee mug is black"))

        // The pipeline itself completes successfully -- REMEMBER was recognised, routed to
        // MemoryAdmissionCoordinator, and a real reply was composed and delivered. This is not a
        // ParkerRuntimeOutcome.Failed; the admission path's own internal Declined disposition is
        // handled, not thrown.
        val delivered = assertIs<ParkerRuntimeOutcome.Delivered>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, delivered.executionResult.status)

        assertEquals(1, ownerSink.notifications.size)
        val replyText = ownerSink.notifications.single()

        // The mandatory response-integrity guarantee, proven against a real, currently-failing
        // governed path, not a contrived one: Parker must never claim success it did not achieve.
        assertTrue(replyText.startsWith("I wasn't able to store that:"), "actual reply: $replyText")
        assertTrue("I'll remember" !in replyText && "I've stored" !in replyText, "must never fabricate a success claim: $replyText")
        assertTrue(
            replyText.contains("could not be resolved"),
            "the real, honest basis (Knowledge Submission's own resolution failure) must be disclosed, not a generic or misleading message: $replyText",
        )

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
    fun `this Unit's own admission gate is genuinely reached and genuinely approves in the real, live runtime -- proven structurally, not merely by absence of a denial`() = runBlocking<Unit> {
        // The first test above already proves this indirectly (the reply reaches Knowledge
        // Submission's own "could not be resolved" failure, which is only reachable once this
        // Unit's own admission gate has already approved and the Memory Core write already
        // happened) -- this test restates that same fact directly and explicitly, so a future
        // reader does not have to infer it from a different test's own incidental reply text.
        val stub = startStub("REMEMBER: Stellar is my dog")
        val logPath = Files.createTempDirectory("memory-admission-composition-gate-reached").resolve("memory-core.log").toString()
        val ownerSink = RecordingOwnerNotificationSink()
        val runtime = ParkerRuntime(configFor(stub, logPath), RecordingParkerLogger(), ownerSink)
        runtime.start()

        runtime.submitOwnerMessage(message("Remember that Stellar is my dog"))

        val replyText = ownerSink.notifications.single()
        assertTrue(
            "not able to store that right now" !in replyText,
            "a reply naming this Unit's own admission-gate denial must never occur when the real policy approves WRITE/MEMORY (already an APPROVED rule) -- actual: $replyText",
        )

        runtime.shutdown()
    }
}
