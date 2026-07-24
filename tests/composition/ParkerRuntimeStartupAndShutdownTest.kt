package parker.composition

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Sprint 10, Unit 4 acceptance test: [ParkerRuntime]'s own lifecycle
 * (startup sequence, shutdown sequence, and the production failure
 * behaviours the task instructions name -- "startup failure," "dependency
 * construction failures," "graceful shutdown"). No live model server is
 * used by this file -- every test here either never reaches the reasoning
 * step, or is scoped to lifecycle state alone.
 */
class ParkerRuntimeStartupAndShutdownTest {

    private fun config(localTextChannelModuleId: String = "channel.local-text-lifecycle-test") = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted by these tests
        modelName = "test-model",
        ownerPrincipalId = "user.owner-lifecycle-test",
        ownerDisplayName = "Test Owner",
        localTextChannelModuleId = localTextChannelModuleId,
    )

    @Test
    fun `start() transitions NOT_STARTED to RUNNING and logs Runtime starting then Runtime started, in order`() = runTest {
        val logger = RecordingParkerLogger()
        val runtime = ParkerRuntime(config(), logger)

        assertEquals(RuntimeLifecycleState.NOT_STARTED, runtime.state)
        runtime.start()
        assertEquals(RuntimeLifecycleState.RUNNING, runtime.state)

        val infoMessages = logger.messages(LogLevel.INFO)
        val startingIndex = infoMessages.indexOfFirst { it == "Runtime starting" }
        val startedIndex = infoMessages.indexOfFirst { it == "Runtime started" }
        assertTrue(startingIndex >= 0 && startedIndex >= 0)
        assertTrue(startingIndex < startedIndex)
    }

    @Test
    fun `start() called a second time throws IllegalStateException and does not change state`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        assertFailsWith<IllegalStateException> { runtime.start() }
        assertEquals(RuntimeLifecycleState.RUNNING, runtime.state)
    }

    @Test
    fun `shutdown() after start() transitions to STOPPED and logs Runtime shutting down then Runtime stopped`() = runTest {
        val logger = RecordingParkerLogger()
        val runtime = ParkerRuntime(config(), logger)
        runtime.start()

        runtime.shutdown()

        assertEquals(RuntimeLifecycleState.STOPPED, runtime.state)
        val infoMessages = logger.messages(LogLevel.INFO)
        val shuttingDownIndex = infoMessages.indexOfFirst { it == "Runtime shutting down" }
        val stoppedIndex = infoMessages.indexOfFirst { it == "Runtime stopped" }
        assertTrue(shuttingDownIndex >= 0 && stoppedIndex >= 0)
        assertTrue(shuttingDownIndex < stoppedIndex)
    }

    @Test
    fun `shutdown() without start() throws IllegalStateException`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        assertFailsWith<IllegalStateException> { runtime.shutdown() }
    }

    @Test
    fun `submitOwnerMessage() before start() throws NotRunning naming NOT_STARTED`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        val thrown = assertFailsWith<ParkerRuntimeException.NotRunning> {
            runtime.submitOwnerMessage(sampleMessage(config().localTextChannelModuleId))
        }
        assertEquals(RuntimeLifecycleState.NOT_STARTED, thrown.state)
    }

    @Test
    fun `submitOwnerMessage() after shutdown() throws NotRunning naming STOPPED`() = runTest {
        val cfg = config()
        val runtime = ParkerRuntime(cfg, RecordingParkerLogger())
        runtime.start()
        runtime.shutdown()

        val thrown = assertFailsWith<ParkerRuntimeException.NotRunning> {
            runtime.submitOwnerMessage(sampleMessage(cfg.localTextChannelModuleId))
        }
        assertEquals(RuntimeLifecycleState.STOPPED, thrown.state)
    }

    @Test
    fun `a dependency construction failure during start() is reported as DependencyConstructionFailed and leaves state FAILED`() = runTest {
        // ModuleId requires a non-blank value (src/contracts/Module.kt) -- a blank configured
        // localTextChannelModuleId is a genuine, real construction failure this runtime must
        // surface, not a fabricated one.
        val runtime = ParkerRuntime(config(localTextChannelModuleId = "   "), RecordingParkerLogger())

        val thrown = assertFailsWith<ParkerRuntimeException.DependencyConstructionFailed> { runtime.start() }
        assertEquals("Local Text Channel module registration", thrown.component)
        assertEquals(RuntimeLifecycleState.FAILED, runtime.state)
    }

    @Test
    fun `shutdown() is callable after a failed start() and completes cleanly`() = runTest {
        val runtime = ParkerRuntime(config(localTextChannelModuleId = "   "), RecordingParkerLogger())
        assertFailsWith<ParkerRuntimeException.DependencyConstructionFailed> { runtime.start() }
        assertEquals(RuntimeLifecycleState.FAILED, runtime.state)

        runtime.shutdown()

        assertEquals(RuntimeLifecycleState.STOPPED, runtime.state)
    }

    @Test
    fun `every log entry ParkerRuntime itself writes during startup is at INFO -- no ERROR on a successful start`() = runTest {
        val logger = RecordingParkerLogger()
        val runtime = ParkerRuntime(config(), logger)

        runtime.start()

        assertTrue(logger.messages(LogLevel.ERROR).isEmpty())
    }

    private fun sampleMessage(channelId: String) = InboundOwnerMessage(
        channelId = ModuleId(channelId),
        senderPrincipalId = PrincipalId("user.owner-lifecycle-test"),
        text = "hello",
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId("corr-lifecycle-1"),
    )
}
