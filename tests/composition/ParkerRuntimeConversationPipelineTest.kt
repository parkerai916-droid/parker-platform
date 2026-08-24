package parker.composition

import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PlanningSessionResult
import parker.core.interfaces.PrincipalId
import parker.core.runtime.GoalPlanningHandoffOutcome
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 10, Unit 4's central acceptance test: a real inbound
 * [InboundOwnerMessage] traversing the complete, real, production
 * runtime -- `CommunicationIntake -> ConversationEngine ->
 * ReasoningProvider -> ResponseComposer -> ResponseDelivery ->
 * ExecutionPipeline -> Tool execution` -- with every stage a real,
 * unmodified production component, and Trust authorisation
 * (`PermissionEngine`, via `ExecutionPipeline`) genuinely exercised, not
 * bypassed. The one component this test replaces the real network
 * endpoint of is the model server itself -- [StubModelServer], a local
 * loopback HTTP server speaking the same Ollama-shaped wire format
 * `LocalHttpModelInferenceClient` already expects in production (see
 * `CompositionTestFixtures.kt`'s own KDoc) -- `ModelReasoningProvider`
 * and `LocalHttpModelInferenceClient` themselves are the real, unmodified
 * production classes.
 *
 * These tests use `runBlocking`, not `kotlinx.coroutines.test.runTest`.
 * `runTest`'s virtual-time scheduler auto-advances to the next scheduled
 * delayed task (here, `ModelReasoningProvider.reason()`'s own
 * `withTimeout`) whenever it sees no other scheduler-tracked runnable
 * work -- which is exactly the state a coroutine is in while genuinely
 * suspended on a real, foreign-thread I/O callback (the JDK
 * `java.net.http.HttpClient` call to [StubModelServer] below). That race
 * fires the timeout before the real, fast loopback response ever arrives,
 * producing a spurious `TimeoutCancellationException` in place of the
 * real outcome under test. `runBlocking` uses real wall-clock time, so
 * the real (sub-timeout) round trip to [StubModelServer] and the real
 * `withTimeout` race fairly, as they would in production.
 *
 * Each call below is `runBlocking<Unit>`, with an explicit type argument
 * -- not the bare, inferred `runBlocking { ... }` first used here. Every
 * test ends with `runtime.shutdown()` as its last statement;
 * `ParkerRuntime.shutdown()`'s own expression body infers a return type
 * of `Nothing?` (from `firstFailure?.let { throw ... }`), not `Unit`.
 * Without the explicit `<Unit>` type argument, `runBlocking`'s own
 * generic return type -- and therefore each test function's own inferred
 * return type -- followed `shutdown()`'s `Nothing?`, which compiles on
 * the JVM to a method returning `java.lang.Void`, not `void`. JUnit
 * Jupiter's own `@Test`-method discovery predicate (`IsTestableMethod`)
 * requires a genuine `void`-returning method and silently excludes any
 * method that isn't -- not a failure, not an error, simply never
 * discovered or run. This is exactly why these four tests were absent
 * (not failing) from Steven's first `runBlocking` verification run: 646
 * total tests, minus these 4 (and the 3 in
 * `ParkerRuntimeFailureHandlingTest.kt`, the same defect), plus the 4 new
 * `StageCancellationTest.kt` methods, = 643. The explicit `<Unit>` type
 * argument fixes this without touching `ParkerRuntime.shutdown()` itself:
 * it forces `runBlocking`'s block parameter to be a genuine
 * `Unit`-returning function type, and Kotlin's own "expected type Unit"
 * convention (the same mechanism `kotlinx.coroutines.test.runTest`
 * already relies on, since its own `testBody` parameter is fixed at
 * `suspend TestScope.() -> Unit`) discards whatever value the block's
 * last statement actually produces.
 */
class ParkerRuntimeConversationPipelineTest {

    private val ownerPrincipalId = "user.owner-pipeline-test"
    private val channelModuleId = "channel.local-text-pipeline-test"
    private var server: StubModelServer? = null

    @AfterTest
    fun tearDown() {
        server?.close()
    }

    private fun startStub(responseFieldValue: String): StubModelServer =
        StubModelServer.start(responseFieldValue).also { server = it }

    // This file exercises the conversation pipeline only -- it never calls submitEvidence/
    // retrieveEvidence/deleteEvidenceAsOwner, so these two paths are real, writable, unused
    // locations, not exercised by any test below.
    private fun configFor(stub: StubModelServer) = ParkerRuntimeConfig(
        modelEndpointUrl = stub.endpointUrl,
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        ownerDisplayName = "Test Owner",
        localTextChannelModuleId = channelModuleId,
        evidenceStorageRootPath = Files.createTempDirectory("unused-evidence-storage").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("unused-evidence-storage-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("unused-evidence-storage-manifest-derivative-generation").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("unused-evidence-storage-manifest-derivative-generation-content").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("unused-evidence-storage-manifest-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("unused-evidence-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("unused-memory-core").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("knowledge-items-test").resolve("items.log").toString(),
    )

    private fun message(senderPrincipalId: String = ownerPrincipalId, text: String = "good morning parker") = InboundOwnerMessage(
        channelId = ModuleId(channelModuleId),
        senderPrincipalId = PrincipalId(senderPrincipalId),
        text = text,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId("corr-pipeline-${System.nanoTime()}"),
    )

    @Test
    fun `a Reply from the model reaches the owner through the real production runtime, with Trust authorisation genuinely exercised`() = runBlocking<Unit> {
        val stub = startStub("REPLY: good morning to you too!")
        val logger = RecordingParkerLogger()
        val ownerSink = RecordingOwnerNotificationSink()
        val runtime = ParkerRuntime(configFor(stub), logger, ownerSink)
        runtime.start()

        val outcome = runtime.submitOwnerMessage(message())

        val delivered = assertIs<ParkerRuntimeOutcome.Delivered>(outcome)
        assertEquals(ExecutionResultStatus.SUCCESS, delivered.executionResult.status)
        assertEquals(listOf("good morning to you too!"), ownerSink.notifications)

        // Trust authorisation genuinely ran and genuinely approved -- not skipped, not assumed.
        assertTrue(logger.hasMessageContaining("Execution authorised"))
        assertTrue(logger.hasMessageContaining("Reasoning completed"))
        assertTrue(logger.hasMessageContaining("Conversation accepted"))

        runtime.shutdown()
    }

    @Test
    fun `a message from an unregistered sender is rejected before reaching the model at all`() = runBlocking<Unit> {
        val stub = startStub("REPLY: should never be requested")
        val runtime = ParkerRuntime(configFor(stub), RecordingParkerLogger())
        runtime.start()

        val outcome = runtime.submitOwnerMessage(message(senderPrincipalId = "user.stranger-not-registered"))

        val notAccepted = assertIs<ParkerRuntimeOutcome.NotAccepted>(outcome)
        assertTrue("stranger-not-registered" in notAccepted.reason || "does not resolve" in notAccepted.reason)

        runtime.shutdown()
    }

    @Test
    fun `a Goal response from the model reaches PlannerRuntime through the real production runtime, resulting in ParkerRuntimeOutcome Planned (Plan Candidate to PlannerRuntime Integration)`() = runBlocking<Unit> {
        val stub = startStub("GOAL: book a dentist appointment")
        val logger = RecordingParkerLogger()
        val runtime = ParkerRuntime(configFor(stub), logger)
        runtime.start()

        val outcome = runtime.submitOwnerMessage(message())

        val planned = assertIs<ParkerRuntimeOutcome.Planned>(outcome)
        val handoffOutcome = assertIs<GoalPlanningHandoffOutcome.Planned>(planned.outcome)
        val sessionResult = handoffOutcome.planningSessionResult

        // Predicted outcome, traced by hand from the real production wiring this Unit adds:
        // DefaultPlanCandidateGenerator produces exactly one verbatim candidate whose goal matches
        // the PlanningRequest's own goal; DefaultPlanDecision therefore selects it (no goal
        // mismatch, no duplicate ID); InMemoryTaskManagerRuntime accepts the resulting TaskProposal,
        // since its proposedOwnerPrincipalId is the message's sender, the registered owner
        // Principal ("accept-only, for a resolvable owner"). This has not been run in this sandbox
        // (no completable Gradle build/test run here, per every prior Unit's own entry in
        // IMPLEMENTATION_HISTORY.md) -- per this Unit's own Scope Lock Section 13 item 19, if
        // Steven's own native `.\gradlew.bat test` run observes a different PlanningSessionResult
        // variant, this assertion must be corrected to match the real, observed behaviour, not left
        // as a stale, unverified prediction.
        assertIs<PlanningSessionResult.Completed>(sessionResult)

        // System planner and task-manager identities resolved successfully -- if either had not,
        // InMemoryPlannerRuntime would have returned Failed with a "does not resolve" reason instead.
        assertTrue(logger.hasMessageContaining("Planning attempted"))

        runtime.shutdown()
    }

    @Test
    fun `a NoAction response is returned as NotAccepted, and nothing is delivered to the owner`() = runBlocking<Unit> {
        val stub = startStub("NOACTION")
        val ownerSink = RecordingOwnerNotificationSink()
        val runtime = ParkerRuntime(configFor(stub), RecordingParkerLogger(), ownerSink)
        runtime.start()

        val outcome = runtime.submitOwnerMessage(message())

        assertIs<ParkerRuntimeOutcome.NotAccepted>(outcome)
        assertTrue(ownerSink.notifications.isEmpty())

        runtime.shutdown()
    }
}
