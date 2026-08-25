package parker.composition

import java.io.File
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceArtifactId
import parker.ui.EvidenceImportOutcome
import parker.ui.OfflineOwnerInteraction
import parker.ui.OfflineOwnerScenario
import parker.ui.OwnerEvidenceOperations
import parker.ui.OwnerEvidenceUiController
import parker.ui.OwnerInteraction
import parker.ui.OwnerInteractionAvailability
import parker.ui.OwnerReply
import parker.ui.OwnerSubmissionAttempt
import parker.ui.OwnerSubmissionDisposition
import parker.ui.OwnerUiController
import parker.ui.OwnerUiStatus
import parker.ui.TierAContentRetrievalResult
import parker.ui.TierAProcessingOutcome
import parker.ui.TierBProcessingOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OwnerUiRuntimeCompositionTest {

    @Test
    fun `successful startup exposes only the configured OwnerInteraction and OwnerEvidenceOperations`() = runTest {
        val interaction = RecordingInteraction()
        val evidenceOperations = RecordingEvidenceOperations()
        var starts = 0
        val session = session(interaction, evidenceOperations, start = { starts++ })

        val result = assertIs<OwnerUiRuntimeStartResult.Ready>(session.start())

        assertEquals(1, starts)
        assertSame(interaction, result.interaction)
        assertSame(evidenceOperations, result.evidenceOperations)
        assertFailsWith<IllegalStateException> { session.start() }
    }

    @Test
    fun `startup failure is safe and shuts runtime down exactly once`() = runTest {
        var shutdowns = 0
        val session = session(
            start = { throw ParkerRuntimeException.StartupFailed(IllegalStateException("sensitive endpoint detail")) },
            shutdown = { shutdowns++ },
        )

        val result = assertIs<OwnerUiRuntimeStartResult.Failed>(session.start())
        session.shutdown()

        assertEquals("Parker Runtime could not start", result.safeMessage)
        assertTrue("sensitive" !in result.safeMessage)
        assertEquals(1, shutdowns)
    }

    @Test
    fun `runtime shutdown is idempotent`() = runTest {
        var shutdowns = 0
        val session = session(shutdown = { shutdowns++ })

        session.shutdown()
        session.shutdown()

        assertEquals(1, shutdowns)
    }

    @Test
    fun `close ordering stops controller before runtime shutdown`() = runTest {
        val interaction = OfflineOwnerInteraction(listOf(OfflineOwnerScenario.Delivered()))
        val controller = OwnerUiController(interaction, coroutineContext)
        val evidenceController = OwnerEvidenceUiController(RecordingEvidenceOperations(), coroutineContext)
        var stateObservedDuringRuntimeShutdown: OwnerUiStatus? = null
        val session = session(
            interaction = interaction,
            shutdown = { stateObservedDuringRuntimeShutdown = controller.state.value.status },
        )

        shutdownOwnerUiSession(controller, evidenceController, session)

        assertEquals(OwnerUiStatus.STOPPED, stateObservedDuringRuntimeShutdown)
        assertEquals(OwnerSubmissionAttempt.UNAVAILABLE, controller.submit("after close"))
    }

    @Test
    fun `real launcher remains explicit while offline and CLI launchers remain separate`() {
        val desktopBuild = File("ui-desktop/build.gradle.kts").readText()
        val realLauncher = File("ui-desktop/src/main/kotlin/parker/ui/OwnerUiMain.kt").readText()
        val offlineLauncher = File("ui-desktop/src/main/kotlin/parker/ui/OfflineOwnerUiMain.kt").readText()
        val main = File("src/composition/Main.kt").readText()

        assertTrue("runOwnerUi" in desktopBuild)
        assertTrue("parker.ui.OwnerUiMainKt" in desktopBuild)
        assertTrue("runOfflineOwnerUi" in desktopBuild)
        assertTrue("parker.ui.OfflineOwnerUiMainKt" in desktopBuild)
        assertTrue("createOwnerUiRuntimeSession(System.getenv())" in realLauncher)
        assertTrue(
            "ParkerOwnerWindow(state.controller, OwnerWindowPresentationMode.PARKER_RUNTIME, state.evidenceController)" in realLauncher,
        )
        assertTrue("Starting Parker Runtime..." in realLauncher)
        assertTrue("Parker could not start" in realLauncher)
        assertTrue("ParkerRuntime" !in offlineLauncher)
        assertTrue("OwnerUiMain" !in main)
        assertTrue("mainClass.set(\"parker.composition.MainKt\")" in File("build.gradle.kts").readText())
    }

    @Test
    fun `real graphical composition preserves capability and isolation boundaries`() {
        val composition = File("src/composition/OwnerUiRuntimeComposition.kt").readText()
        val launcher = File("ui-desktop/src/main/kotlin/parker/ui/OwnerUiMain.kt").readText()
        val adapter = File("src/composition/OwnerUiRuntimeAdapter.kt").readText()
        val evidenceAdapter = File("src/composition/OwnerUiEvidenceRuntimeAdapter.kt").readText()
        val forbiddenLauncherTerms = listOf(
            "PermissionEngine",
            "ReasoningProvider",
            "LocalHttpModelInferenceClient",
            "submitEvidence(",
            "retrieveEvidence(",
            "deleteEvidenceAsOwner(",
            "analyseEvidence(",
            "importEvidenceFileAsOwner(",
            "invokeTierAIngestionAsOwner(",
            "retrieveTierAExtractedContentAsOwner(",
            "http://",
            "https://",
        )

        assertTrue("ParkerRuntimeConfigLoader.load(environment)" in composition)
        assertTrue("PrincipalId(config.ownerPrincipalId)" in composition)
        assertTrue("ModuleId(config.localTextChannelModuleId)" in composition)
        assertTrue("submitOwnerMessage = runtime::submitOwnerMessage" in composition)
        assertTrue("importEvidenceFileAsOwner = runtime::importEvidenceFileAsOwner" in composition)
        assertTrue("invokeTierAIngestionAsOwner = runtime::invokeTierAIngestionAsOwner" in composition)
        assertTrue("analyseEvidence = runtime::analyseEvidence" in composition)
        assertTrue("retrieveTierAExtractedContentAsOwner = runtime::retrieveTierAExtractedContentAsOwner" in composition)
        assertTrue("OwnerUiController(result.interaction)" in launcher)
        assertTrue("OwnerEvidenceUiController(result.evidenceOperations)" in launcher)
        assertTrue("OwnerWindowPresentationMode.PARKER_RUNTIME" in launcher)
        assertTrue(forbiddenLauncherTerms.none { it in launcher })
        assertTrue("runtime.start()" !in adapter && "runtime.shutdown()" !in adapter)
        assertTrue("runtime.start()" !in evidenceAdapter && "runtime.shutdown()" !in evidenceAdapter)
        assertTrue("androidx.compose" !in composition && "androidx.compose" !in adapter && "androidx.compose" !in evidenceAdapter)
        // The evidence adapter must receive only the named method references, never
        // ParkerRuntime itself -- mirroring OwnerUiRuntimeAdapter's own identical discipline.
        assertTrue("ParkerRuntime," !in evidenceAdapter && "runtime: ParkerRuntime" !in evidenceAdapter)
    }

    private fun session(
        interaction: OwnerInteraction = RecordingInteraction(),
        evidenceOperations: OwnerEvidenceOperations = RecordingEvidenceOperations(),
        start: suspend () -> Unit = {},
        shutdown: suspend () -> Unit = {},
    ) = OwnerUiRuntimeSession(start, shutdown, interaction, evidenceOperations)

    private class RecordingInteraction : OwnerInteraction {
        override val availability = OwnerInteractionAvailability.AVAILABLE

        override suspend fun submit(
            ownerText: String,
            onReply: suspend (OwnerReply) -> Unit,
        ): OwnerSubmissionDisposition = OwnerSubmissionDisposition.Delivered("SUCCESS")
    }

    private class RecordingEvidenceOperations : OwnerEvidenceOperations {
        override suspend fun importFile(absolutePath: String, declaredMediaType: String?): EvidenceImportOutcome =
            EvidenceImportOutcome.Imported(EvidenceArtifactId("recording-evidence-1"))

        override suspend fun processTierA(evidenceArtifactId: EvidenceArtifactId): TierAProcessingOutcome =
            TierAProcessingOutcome.Admitted("PDF")

        override suspend fun processTierB(evidenceArtifactId: EvidenceArtifactId): TierBProcessingOutcome =
            TierBProcessingOutcome.Completed(1)

        override suspend fun retrieveTierAExtractedContent(
            evidenceArtifactId: EvidenceArtifactId,
            derivativeGenerationId: parker.core.interfaces.DerivativeGenerationId,
        ): TierAContentRetrievalResult = TierAContentRetrievalResult.UnknownGeneration

        override suspend fun processTierBDurable(evidenceArtifactId: EvidenceArtifactId): parker.ui.TierBDurableProcessingOutcome =
            parker.ui.TierBDurableProcessingOutcome.NotAuthorised("not authorised")

        override suspend fun retrieveTierBOcrContent(
            evidenceArtifactId: EvidenceArtifactId,
            derivativeGenerationId: parker.core.interfaces.DerivativeGenerationId,
        ): parker.ui.TierBOcrContentRetrievalResult = parker.ui.TierBOcrContentRetrievalResult.UnknownGeneration
    }
}
