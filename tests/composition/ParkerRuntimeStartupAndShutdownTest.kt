package parker.composition

import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.runtime.ExternalTranscriptionOwnerInvocationCoordinator
import parker.core.runtime.FileSystemFidelityFirstAttemptLedger
import parker.core.runtime.FileSystemRegionProviderStateStore
import parker.core.runtime.GovernedRegionTranscriptionExecutionCoordinator
import parker.core.runtime.OpenAiRegionTranscriptionAdapter
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertIs
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
    private fun config(
        localTextChannelModuleId: String = "channel.local-text-lifecycle-test",
        openAiExternalTranscriptionEnabled: Boolean = false,
        openAiExternalTranscriptionProviderProfilePath: String? = null,
        openAiApiCredential: OpenAiApiCredential? = null,
        fidelityFirstAcceptanceAuthorityStorageRootPath: String? = null,
        fidelityFirstAttemptStorageRootPath: String? = null,
        regionProviderStateStorageRootPath: String? = null,
        productionCommit: String? = null,
        testRoot: java.nio.file.Path? = null,
    ) = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted by these tests
        modelName = "test-model",
        ownerPrincipalId = "user.owner-lifecycle-test",
        ownerDisplayName = "Test Owner",
        localTextChannelModuleId = localTextChannelModuleId,
        evidenceStorageRootPath = testDirectory(testRoot, "evidence-storage").toString(),
        evidenceSourceManifestStorageRootPath = testDirectory(testRoot, "evidence-source-manifest-storage").toString(),
        derivativeGenerationStorageRootPath = testDirectory(testRoot, "derivative-generation-storage").toString(),
        derivativeContentStorageRootPath = testDirectory(testRoot, "derivative-content-storage").toString(),
        savedAnalysisStorageRootPath = testDirectory(testRoot, "saved-analysis-storage").toString(),
        documentIngestionAuditLogPath = testDirectory(testRoot, "document-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = testDirectory(testRoot, "evidence-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = testDirectory(testRoot, "memory-core").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = testDirectory(testRoot, "knowledge-items").resolve("items.log").toString(),
        openAiExternalTranscriptionEnabled = openAiExternalTranscriptionEnabled,
        openAiExternalTranscriptionProviderProfilePath = openAiExternalTranscriptionProviderProfilePath,
        openAiApiCredential = openAiApiCredential,
        fidelityFirstAcceptanceAuthorityStorageRootPath = fidelityFirstAcceptanceAuthorityStorageRootPath,
        fidelityFirstAttemptStorageRootPath = fidelityFirstAttemptStorageRootPath,
        regionProviderStateStorageRootPath = regionProviderStateStorageRootPath,
        productionCommit = productionCommit,
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
    fun `external transcription backend entry is owner-only and disabled when enablement is false`() = runTest {
        val method = ParkerRuntime::class.members.single { it.name == "invokeExternalTranscriptionAsOwner" }
        val valueParameters = method.parameters.filter { it.kind == KParameter.Kind.VALUE }
        assertEquals(listOf(EvidenceArtifactId::class), valueParameters.map { it.type.classifier })

        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        assertIs<OpenAiExternalTranscriptionReadiness.Disabled>(runtime.openAiExternalTranscriptionReadiness)
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

    @Test
    fun `external transcription composition follows every fail-closed readiness state without startup egress`() = runTest {
        val validProfile = profileFile()
        val staleProfile = profileFile(nextReviewDate = "2026-08-25")
        val validCredential = OpenAiApiCredential.fromEnvironment("unit-n-valid-fake-credential")!!
        val unsafeCredential = OpenAiApiCredential.fromEnvironment("unit-n${0x1b.toChar()}unsafe")

        suspend fun assertComposition(
            cfg: ParkerRuntimeConfig,
            backendType: kotlin.reflect.KClass<*>,
            uiType: kotlin.reflect.KClass<*>,
            realAdapter: Boolean,
        ) {
            val runtime = ParkerRuntime(cfg, RecordingParkerLogger(), clock = { Instant.parse("2026-08-26T00:00:00Z") })
            runtime.start()
            assertEquals(RuntimeLifecycleState.RUNNING, runtime.state)
            assertTrue(backendType.isInstance(runtime.openAiExternalTranscriptionBackendReadiness))
            assertTrue(uiType.isInstance(runtime.ownerEnhancedTranscriptionReadiness()))
            val coordinatorField = ParkerRuntime::class.java.getDeclaredField("externalTranscriptionOwnerInvocationCoordinator").apply { isAccessible = true }
            val coordinator = coordinatorField.get(runtime) as ExternalTranscriptionOwnerInvocationCoordinator
            val mechanismField = ExternalTranscriptionOwnerInvocationCoordinator::class.java.getDeclaredField("externalMechanism").apply { isAccessible = true }
            val mechanismName = mechanismField.get(coordinator)::class.java.name
            assertEquals(realAdapter, mechanismName.contains("OpenAiResponsesExternalTranscriptionAdapter"))
            assertEquals(!realAdapter, mechanismName.contains("DisabledExternalTranscriptionMechanism"))
            runtime.shutdown()
        }

        assertComposition(config(), OpenAiExternalTranscriptionBackendReadiness.Disabled::class, parker.ui.EnhancedTranscriptionReadiness.Disabled::class, false)
        assertComposition(config(openAiExternalTranscriptionEnabled = true, openAiExternalTranscriptionProviderProfilePath = "missing-profile.properties"), OpenAiExternalTranscriptionBackendReadiness.ProfileNotReady::class, parker.ui.EnhancedTranscriptionReadiness.ProfileNotReady::class, false)
        assertComposition(config(openAiExternalTranscriptionEnabled = true, openAiExternalTranscriptionProviderProfilePath = staleProfile.toString(), openAiApiCredential = validCredential), OpenAiExternalTranscriptionBackendReadiness.ProfileNotReady::class, parker.ui.EnhancedTranscriptionReadiness.ProfileNotReady::class, false)
        assertComposition(config(openAiExternalTranscriptionEnabled = true, openAiExternalTranscriptionProviderProfilePath = validProfile.toString()), OpenAiExternalTranscriptionBackendReadiness.ConfigurationNotAccepted::class, parker.ui.EnhancedTranscriptionReadiness.ProfileNotReady::class, false)
        assertEquals(null, unsafeCredential)
        assertComposition(config(openAiExternalTranscriptionEnabled = true, openAiExternalTranscriptionProviderProfilePath = validProfile.toString(), openAiApiCredential = unsafeCredential), OpenAiExternalTranscriptionBackendReadiness.ConfigurationNotAccepted::class, parker.ui.EnhancedTranscriptionReadiness.ProfileNotReady::class, false)
        assertComposition(config(openAiExternalTranscriptionEnabled = true, openAiExternalTranscriptionProviderProfilePath = validProfile.toString(), openAiApiCredential = validCredential), OpenAiExternalTranscriptionBackendReadiness.ConfigurationNotAccepted::class, parker.ui.EnhancedTranscriptionReadiness.ProfileNotReady::class, false)
    }

    @Test
    fun `pending lifecycle composes acceptance lane while ordinary mechanism remains disabled`() = runTest {
        val authorityRoot = Files.createTempDirectory("synthetic-acceptance-authorities")
        val attemptRoot = Files.createTempDirectory("synthetic-acceptance-attempts")
        val runtime = ParkerRuntime(
            config(
                openAiExternalTranscriptionEnabled = true,
                openAiExternalTranscriptionProviderProfilePath = profileFile().toString(),
                openAiApiCredential = OpenAiApiCredential.fromEnvironment("synthetic-composition-credential")!!,
                fidelityFirstAcceptanceAuthorityStorageRootPath = authorityRoot.toString(),
                fidelityFirstAttemptStorageRootPath = attemptRoot.toString(),
                productionCommit = "a".repeat(40),
            ), RecordingParkerLogger(), clock = { Instant.parse("2026-08-26T00:00:00Z") }, buildIdentity = { "a".repeat(40) },
        )
        runtime.start()
        val ordinary = ParkerRuntime::class.java.getDeclaredField("externalTranscriptionOwnerInvocationCoordinator").apply { isAccessible = true }
            .get(runtime) as ExternalTranscriptionOwnerInvocationCoordinator
        val mechanism = ExternalTranscriptionOwnerInvocationCoordinator::class.java.getDeclaredField("externalMechanism").apply { isAccessible = true }.get(ordinary)
        assertTrue(mechanism::class.java.name.contains("DisabledExternalTranscriptionMechanism"))
        val acceptance = ParkerRuntime::class.java.getDeclaredField("fidelityFirstAcceptanceCoordinator").apply { isAccessible = true }.get(runtime)
        assertTrue(acceptance != null)
        assertEquals(
            "AUTHORITY_MISSING",
            (runtime.invokeFidelityFirstAcceptanceAsOwner("synthetic-missing-authority") as parker.core.runtime.FidelityFirstAcceptanceOutcome.Blocked).reason,
        )
        runtime.shutdown()
    }

    @Test
    fun `configured region execution composes accepted store ledger and adapter without records or a public execution bypass`() = runTest {
        val testRoot = Files.createTempDirectory("region-composition-test")
        try {
            val authorityRoot = testDirectory(testRoot, "authorities")
            val attemptRoot = testDirectory(testRoot, "attempts")
            val providerRoot = testDirectory(testRoot, "provider-state")
            val profile = profileFile(parent = testRoot)
            val cfg = config(
                openAiExternalTranscriptionEnabled = true,
                openAiExternalTranscriptionProviderProfilePath = profile.toString(),
                openAiApiCredential = OpenAiApiCredential.fromEnvironment("synthetic-region-composition-credential")!!,
                fidelityFirstAcceptanceAuthorityStorageRootPath = authorityRoot.toString(),
                fidelityFirstAttemptStorageRootPath = attemptRoot.toString(),
                regionProviderStateStorageRootPath = providerRoot.toString(),
                productionCommit = "a".repeat(40),
                testRoot = testRoot,
            )

            repeat(2) {
                val runtime = ParkerRuntime(
                    cfg, RecordingParkerLogger(), clock = { Instant.parse("2026-08-26T00:00:00Z") },
                    buildIdentity = { "a".repeat(40) },
                )
                runtime.start()
                val regionCoordinator = ParkerRuntime::class.java
                    .getDeclaredField("governedRegionTranscriptionExecutionCoordinator")
                    .apply { isAccessible = true }
                    .get(runtime) as GovernedRegionTranscriptionExecutionCoordinator
                val ledger = GovernedRegionTranscriptionExecutionCoordinator::class.java.getDeclaredField("ledger")
                    .apply { isAccessible = true }.get(regionCoordinator) as FileSystemFidelityFirstAttemptLedger
                val store = GovernedRegionTranscriptionExecutionCoordinator::class.java.getDeclaredField("providerStateStore")
                    .apply { isAccessible = true }.get(regionCoordinator) as FileSystemRegionProviderStateStore
                val mechanism = GovernedRegionTranscriptionExecutionCoordinator::class.java.getDeclaredField("mechanism")
                    .apply { isAccessible = true }.get(regionCoordinator)
                val ledgerRoot = FileSystemFidelityFirstAttemptLedger::class.java.getDeclaredField("root")
                    .apply { isAccessible = true }.get(ledger) as java.nio.file.Path
                val storeRoot = FileSystemRegionProviderStateStore::class.java.getDeclaredField("root")
                    .apply { isAccessible = true }.get(store) as java.nio.file.Path

                assertEquals(attemptRoot.toAbsolutePath().normalize(), ledgerRoot)
                assertEquals(providerRoot.toAbsolutePath().normalize(), storeRoot)
                assertIs<OpenAiRegionTranscriptionAdapter>(mechanism)
                assertTrue(Files.list(attemptRoot).use { paths -> paths.count() == 0L })
                assertTrue(store.enumerate().isEmpty())
                assertTrue(ParkerRuntime::class.members.none { member ->
                    member.name.contains("RegionTranscription") && member.name.startsWith("invoke")
                })
                runtime.shutdown()
            }
        } finally {
            deleteTree(testRoot)
        }
    }

    @Test
    fun `configured region execution fails startup when its explicit root is unusable`() = runTest {
        val testRoot = Files.createTempDirectory("region-invalid-composition-test")
        try {
            val authorityRoot = testDirectory(testRoot, "authorities")
            val attemptRoot = testDirectory(testRoot, "attempts")
            val missingProviderRoot = testRoot.resolve("missing-provider-state")
            val runtime = ParkerRuntime(
                config(
                openAiExternalTranscriptionEnabled = true,
                openAiExternalTranscriptionProviderProfilePath = profileFile(parent = testRoot).toString(),
                openAiApiCredential = OpenAiApiCredential.fromEnvironment("synthetic-region-invalid-credential")!!,
                fidelityFirstAcceptanceAuthorityStorageRootPath = authorityRoot.toString(),
                fidelityFirstAttemptStorageRootPath = attemptRoot.toString(),
                regionProviderStateStorageRootPath = missingProviderRoot.toString(),
                productionCommit = "a".repeat(40),
                    testRoot = testRoot,
                ), RecordingParkerLogger(), clock = { Instant.parse("2026-08-26T00:00:00Z") }, buildIdentity = { "a".repeat(40) },
            )

            assertFailsWith<ParkerRuntimeException.DependencyConstructionFailed> { runtime.start() }
            assertEquals(RuntimeLifecycleState.FAILED, runtime.state)
            assertTrue(!Files.exists(missingProviderRoot))
        } finally {
            deleteTree(testRoot)
        }
    }

    @Test
    fun `disabled startup never logs a supplied fake OpenAI credential`() = runTest {
        val sentinel = "unit-g-fake-secret-sentinel"
        val logger = RecordingParkerLogger()
        val runtime = ParkerRuntime(config(openAiApiCredential = OpenAiApiCredential.fromEnvironment(sentinel)), logger)

        runtime.start()

        assertIs<OpenAiExternalTranscriptionBackendReadiness.Disabled>(runtime.openAiExternalTranscriptionBackendReadiness)
        assertTrue(logger.messages(LogLevel.DEBUG).none { it.contains(sentinel) })
        assertTrue(logger.messages(LogLevel.INFO).none { it.contains(sentinel) })
        assertTrue(logger.messages(LogLevel.WARN).none { it.contains(sentinel) })
        assertTrue(logger.messages(LogLevel.ERROR).none { it.contains(sentinel) })
        runtime.shutdown()
    }

    private fun sampleMessage(channelId: String) = InboundOwnerMessage(
        channelId = ModuleId(channelId),
        senderPrincipalId = PrincipalId("user.owner-lifecycle-test"),
        text = "hello",
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = CorrelationId("corr-lifecycle-1"),
    )

    private fun profileFile(nextReviewDate: String = "2026-09-01", parent: java.nio.file.Path? = null) =
        (parent?.resolve("openai-composition-profile.properties")
            ?: Files.createTempFile("openai-composition-profile", ".properties")).also { path ->
            Files.writeString(path, """schemaVersion=1
providerIdentity=OpenAI
apiProductPath=/v1/responses
store=false
modelSelectionRule=gpt-4.1-mini
modelSnapshotPolicy=RECORD_PRESENT_OR_NOT_EXPOSED
maximumPdfBytes=67108864
maximumImageBytes=16777216
maximumOutputBytes=20971520
timeoutMillis=120000
allowedNetworkDestination=https://api.openai.com
retentionTreatment=reviewed
dataUseTrainingTreatment=reviewed
zdrMamStatus=NOT_AVAILABLE_OR_ENABLED
projectAccountStatus=reviewed
projectAccountControls=reviewed
authenticationMechanism=BEARER_API_CREDENTIAL
requestLoggingConsiderations=reviewed
regionalStorageConsiderations=reviewed
verifiedOn=2026-08-01
approvingOwnerReference=owner-review
nextReviewDate=$nextReviewDate
verificationReferences=provider-review
reverificationTriggers=provider terms change
""")
        }

    private fun testDirectory(parent: java.nio.file.Path?, name: String): java.nio.file.Path =
        parent?.resolve(name)?.also { Files.createDirectories(it) } ?: Files.createTempDirectory("unused-$name")

    private fun deleteTree(root: java.nio.file.Path) {
        if (Files.exists(root)) {
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }
}
