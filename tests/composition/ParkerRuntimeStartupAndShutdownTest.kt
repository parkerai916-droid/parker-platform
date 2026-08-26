package parker.composition

import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.runtime.ExternalTranscriptionOwnerInvocationCoordinator
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.reflect.KParameter

/**
 * Sprint 10, Unit 4 acceptance test: [ParkerRuntime]'s own lifecycle
 * (startup sequence, shutdown sequence, and the production failure
 * behaviours the task instructions name -- "startup failure," "dependency
 * construction failures," "graceful shutdown"). No live model server is
 * used by this file -- every test here either never reaches the reasoning
 * step, or is scoped to lifecycle state alone.
 */
class ParkerRuntimeStartupAndShutdownTest {

    // This file exercises lifecycle state only -- it never calls submitEvidence/retrieveEvidence/
    // deleteEvidenceAsOwner, so these two paths are real, writable, unused locations, not exercised
    // by any test below.
    private fun config(localTextChannelModuleId: String = "channel.local-text-lifecycle-test") = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted by these tests
        modelName = "test-model",
        ownerPrincipalId = "user.owner-lifecycle-test",
        ownerDisplayName = "Test Owner",
        localTextChannelModuleId = localTextChannelModuleId,
        evidenceStorageRootPath = Files.createTempDirectory("unused-evidence-storage").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("unused-evidence-storage-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("unused-evidence-storage-manifest-derivative-generation").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("unused-evidence-storage-manifest-derivative-generation-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("unused-evidence-storage-manifest-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("unused-evidence-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("unused-memory-core").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("knowledge-items-test").resolve("items.log").toString(),
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

    /**
     * Controlled Agent Run Submission (`docs/implementation/CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md`
     * Section 11, item 5): `ParkerRuntime` exposes none of `buildAndRegisterRuntimeGraph`'s local
     * construction variables (`agentRuntime`, `resourceRegistry`, the vocabulary/policy-rule
     * entries) as fields, so this test cannot assert on them directly. What it can, and does,
     * prove: `start()` -- which the pre-existing tests above already confirm exercises the
     * complete construction graph, throwing `DependencyConstructionFailed` (naming the failing
     * step) on any failure -- succeeds cleanly with the Agent Runtime Execution Boundary
     * Resource registration, the `start agent run` ActionVocabulary entry, the `EXECUTE`/`AGENT`
     * PermissionPolicyRule, `DeterministicAgentStepSource`, and `InMemoryAgentRuntime` (now
     * constructed ahead of `InMemoryTaskManagerRuntime`) all wired in. A regression in any of
     * these -- a duplicate Resource registration, a malformed vocabulary entry, or a
     * constructor-argument mismatch -- would surface here as a thrown
     * `ParkerRuntimeException.DependencyConstructionFailed`, exactly as it already would for any
     * other construction step this file's existing tests cover.
     */
    @Test
    fun `start() succeeds with Controlled Agent Run Submission wiring in place`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        runtime.start()

        assertEquals(RuntimeLifecycleState.RUNNING, runtime.state)
    }

    @Test
    fun `external transcription backend entry is owner-only and composed with a disabled mechanism`() = runTest {
        val method = ParkerRuntime::class.members.single { it.name == "invokeExternalTranscriptionAsOwner" }
        val valueParameters = method.parameters.filter { it.kind == KParameter.Kind.VALUE }
        assertEquals(listOf(EvidenceArtifactId::class), valueParameters.map { it.type.classifier })

        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val coordinatorField = ParkerRuntime::class.java.declaredFields.single {
            it.name == "externalTranscriptionOwnerInvocationCoordinator"
        }.apply { isAccessible = true }
        val coordinator = coordinatorField.get(runtime) as ExternalTranscriptionOwnerInvocationCoordinator
        val mechanismField = ExternalTranscriptionOwnerInvocationCoordinator::class.java.declaredFields.single {
            it.name == "externalMechanism"
        }.apply { isAccessible = true }
        val mechanismType = mechanismField.get(coordinator)::class.java.name

        assertTrue(mechanismType.contains("DisabledExternalTranscriptionMechanism"))
        assertTrue(!mechanismType.contains("Http") && !mechanismType.contains("OpenAI"))
        runtime.shutdown()
    }

    private fun sampleMessage(channelId: String) = InboundOwnerMessage(
        channelId = ModuleId(channelId),
        senderPrincipalId = PrincipalId("user.owner-lifecycle-test"),
        text = "hello",
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId("corr-lifecycle-1"),
    )
}
