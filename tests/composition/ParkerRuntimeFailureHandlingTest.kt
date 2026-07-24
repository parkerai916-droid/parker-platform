package parker.composition

import java.time.Instant
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 10, Unit 4 acceptance test for the task's own named production
 * failure categories: "model unavailable" and "tool failure," both
 * exercised against the real production runtime, and both reported as
 * [ParkerRuntimeOutcome.Failed] rather than propagating an uncaught
 * exception or being silently swallowed. "Coordinator failure" is not
 * separately exercised here beyond what the model-unavailable and
 * tool-failure tests already prove: no coordinator between
 * `ConversationReplyCoordinator` and either fault's own origin catches
 * anything (confirmed by direct reading, cited in
 * `ParkerRuntime.submitOwnerMessage`'s own KDoc), so a genuine fault
 * anywhere on that chain reaches this same boundary the same way.
 *
 * These tests use `runBlocking`, not `kotlinx.coroutines.test.runTest`.
 * `runTest`'s virtual-time scheduler auto-advances to the next scheduled
 * delayed task (here, `ModelReasoningProvider.reason()`'s own
 * `withTimeout`) whenever the coroutine is suspended waiting on real,
 * foreign-thread I/O (the JDK `java.net.http.HttpClient` calls below,
 * including the connection-refused and slow-response cases) -- racing
 * ahead of the real result and firing a spurious
 * `TimeoutCancellationException` in its place. `runBlocking` uses real
 * wall-clock time, so each test's real fault (a refused connection, a
 * genuine 100ms timeout against a 2000ms-delayed response, a thrown
 * exception from the owner-notification sink) is what actually produces
 * the observed [ParkerRuntimeOutcome.Failed], matching what a real
 * production runtime would see.
 *
 * Each call below is `runBlocking<Unit>`, with an explicit type argument.
 * Every test ends with `runtime.shutdown()` as its last statement, and
 * `ParkerRuntime.shutdown()`'s own expression body infers a return type
 * of `Nothing?` (from `firstFailure?.let { throw ... }`), not `Unit`. Left
 * as bare `runBlocking { ... }`, each test function's own inferred return
 * type followed suit, compiling to a JVM method returning `java.lang.Void`
 * rather than `void` -- which JUnit Jupiter's own `@Test` discovery
 * predicate silently excludes (not a failure, simply never discovered).
 * This is why these three tests were silently absent, not failing, from
 * Steven's first `runBlocking` verification run. The explicit `<Unit>`
 * type argument fixes this without touching `ParkerRuntime.shutdown()`
 * itself, using the same "expected type Unit" discard convention
 * `kotlinx.coroutines.test.runTest` already relied on for these same
 * tests before this file used `runBlocking` at all.
 */
class ParkerRuntimeFailureHandlingTest {

    private val ownerPrincipalId = "user.owner-failure-test"
    private val channelModuleId = "channel.local-text-failure-test"
    private var server: StubModelServer? = null

    @AfterTest
    fun tearDown() {
        server?.close()
    }

    private fun message() = InboundOwnerMessage(
        channelId = ModuleId(channelModuleId),
        senderPrincipalId = PrincipalId(ownerPrincipalId),
        text = "hello",
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId("corr-failure-${System.nanoTime()}"),
    )

    @Test
    fun `an unreachable model endpoint is reported as Failed with stage UNKNOWN, not thrown, not swallowed`() = runBlocking<Unit> {
        val config = ParkerRuntimeConfig(
            // Nothing is listening on this loopback port -- a real, structural "model unavailable"
            // condition, not a fabricated one.
            modelEndpointUrl = "http://127.0.0.1:1/api/generate",
            modelName = "test-model",
            ownerPrincipalId = ownerPrincipalId,
            localTextChannelModuleId = channelModuleId,
        )
        val logger = RecordingParkerLogger()
        val runtime = ParkerRuntime(config, logger)
        runtime.start()

        val outcome = runtime.submitOwnerMessage(message())

        val failed = assertIs<ParkerRuntimeOutcome.Failed>(outcome)
        assertEquals(PipelineStage.UNKNOWN, failed.stage) // a connection refusal is an IOException, not a timeout -- see PipelineStage's own KDoc
        assertTrue(logger.messages(LogLevel.ERROR).isNotEmpty())
        // The runtime itself is still RUNNING -- one failed conversation does not crash the runtime.
        assertEquals(RuntimeLifecycleState.RUNNING, runtime.state)

        runtime.shutdown()
    }

    @Test
    fun `a model response slower than modelTimeoutMs is reported as Failed with stage REASONING`() = runBlocking<Unit> {
        val stub = StubModelServer.start(responseFieldValue = "REPLY: too slow", delayMillis = 2_000L).also { server = it }
        val config = ParkerRuntimeConfig(
            modelEndpointUrl = stub.endpointUrl,
            modelName = "test-model",
            modelTimeoutMs = 100L,
            ownerPrincipalId = ownerPrincipalId,
            localTextChannelModuleId = channelModuleId,
        )
        val logger = RecordingParkerLogger()
        val runtime = ParkerRuntime(config, logger)
        runtime.start()

        val outcome = runtime.submitOwnerMessage(message())

        val failed = assertIs<ParkerRuntimeOutcome.Failed>(outcome)
        assertEquals(PipelineStage.REASONING, failed.stage)
        assertEquals(RuntimeLifecycleState.RUNNING, runtime.state)

        runtime.shutdown()
    }

    @Test
    fun `a Tool-level failure (the owner-notification sink itself throwing) is reported as Failed, not silently swallowed`() = runBlocking<Unit> {
        val stub = StubModelServer.start("REPLY: this delivery will fail").also { server = it }
        val throwingSink = RecordingOwnerNotificationSink { throw java.io.IOException("simulated delivery failure") }
        val config = ParkerRuntimeConfig(
            modelEndpointUrl = stub.endpointUrl,
            modelName = "test-model",
            ownerPrincipalId = ownerPrincipalId,
            localTextChannelModuleId = channelModuleId,
        )
        val logger = RecordingParkerLogger()
        val runtime = ParkerRuntime(config, logger, throwingSink)
        runtime.start()

        val outcome = runtime.submitOwnerMessage(message())

        val failed = assertIs<ParkerRuntimeOutcome.Failed>(outcome)
        assertEquals(PipelineStage.UNKNOWN, failed.stage)
        assertIs<java.io.IOException>(failed.cause)
        assertTrue(logger.messages(LogLevel.ERROR).isNotEmpty())

        runtime.shutdown()
    }
}
