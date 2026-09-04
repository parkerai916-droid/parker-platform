package parker.composition

import java.util.concurrent.atomic.AtomicBoolean
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.ui.OwnerEvidenceOperations
import parker.ui.OwnerEvidenceUiController
import parker.ui.OwnerInteraction
import parker.ui.OwnerUiController

/** Result of the real graphical launcher's one runtime-start attempt. */
sealed interface OwnerUiRuntimeStartResult {
    data class Ready(val interaction: OwnerInteraction, val evidenceOperations: OwnerEvidenceOperations) : OwnerUiRuntimeStartResult
    data class Failed(val safeMessage: String) : OwnerUiRuntimeStartResult
}

/**
 * Launcher-owned lifecycle boundary. It exposes [OwnerInteraction]/
 * [OwnerEvidenceOperations] only after startup succeeds and makes runtime
 * shutdown idempotent.
 */
class OwnerUiRuntimeSession internal constructor(
    private val startRuntime: suspend () -> Unit,
    private val shutdownRuntime: suspend () -> Unit,
    private val interaction: OwnerInteraction,
    private val evidenceOperations: OwnerEvidenceOperations,
) {
    private val startAttempted = AtomicBoolean(false)
    private val shutdownAttempted = AtomicBoolean(false)

    suspend fun start(): OwnerUiRuntimeStartResult {
        check(startAttempted.compareAndSet(false, true)) { "Owner UI runtime startup may be attempted only once" }
        return try {
            startRuntime()
            OwnerUiRuntimeStartResult.Ready(interaction, evidenceOperations)
        } catch (failure: ParkerRuntimeException) {
            runCatching { shutdown() }
            OwnerUiRuntimeStartResult.Failed("Parker Runtime could not start")
        }
    }

    suspend fun shutdown() {
        if (shutdownAttempted.compareAndSet(false, true)) {
            shutdownRuntime()
        }
    }
}

/** Fixed close ordering: stop UI work before shutting down its runtime. */
suspend fun shutdownOwnerUiSession(
    controller: OwnerUiController,
    evidenceController: OwnerEvidenceUiController,
    session: OwnerUiRuntimeSession,
) {
    controller.shutdown("Window closed")
    evidenceController.shutdown()
    session.shutdown()
}

/**
 * Real graphical composition factory. Construction is side-effect-free;
 * [OwnerUiRuntimeSession.start] remains the explicit runtime-start boundary.
 */
fun createOwnerUiRuntimeSession(environment: Map<String, String>): OwnerUiRuntimeSession {
    val config = ParkerRuntimeConfigLoader.load(environment)
    val notificationBridge = OwnerUiNotificationBridge()
    val logger = ConsoleParkerLogger("owner-ui", minLevel = config.logLevel)
    val runtime = ParkerRuntime(config, logger, notificationBridge)
    val interaction = OwnerUiRuntimeAdapter(
        ownerPrincipalId = PrincipalId(config.ownerPrincipalId),
        localTextChannelModuleId = ModuleId(config.localTextChannelModuleId),
        submitOwnerMessage = runtime::submitOwnerMessage,
        notificationBridge = notificationBridge,
    )
    val evidenceOperations = OwnerUiEvidenceRuntimeAdapter(
        ownerPrincipalId = PrincipalId(config.ownerPrincipalId),
        listRegisteredEvidenceAsOwner = runtime::listRegisteredEvidenceAsOwner,
        importEvidenceFileAsOwner = runtime::importEvidenceFileAsOwner,
        importUploadedEvidenceFileAsOwner = runtime::importUploadedEvidenceFileAsOwner,
        invokeTierAIngestionAsOwner = runtime::invokeTierAIngestionAsOwner,
        analyseEvidence = runtime::analyseEvidence,
        retrieveTierAExtractedContentAsOwner = runtime::retrieveTierAExtractedContentAsOwner,
        invokeTierBOcrDurableGenerationAsOwner = runtime::invokeTierBOcrDurableGenerationAsOwner,
        retrieveTierBOcrContentAsOwner = runtime::retrieveTierBOcrContentAsOwner,
        analyseDocumentsAsOwner = runtime::analyseDocumentsAsOwner,
        saveAnalysisAsOwner = runtime::saveAnalysisAsOwner,
        retrieveSavedAnalysisAsOwner = runtime::retrieveSavedAnalysisAsOwner,
        listSavedAnalysesAsOwner = runtime::listSavedAnalysesAsOwner,
        externalReadiness = runtime::ownerEnhancedTranscriptionReadiness,
        invokeExternalTranscriptionAsOwner = runtime::invokeExternalTranscriptionAsOwner,
        listHumanVerificationRecordsAsOwner = runtime::listHumanVerificationRecordsAsOwner,
        governedDecisionAsOwner = { projectGovernedDecision(runtime.evaluateGovernedAcquisitionAsOwner(it)) },
        ordinaryRegionProposalAsOwner = runtime::proposeOrdinaryRegionIngestionAsOwner,
        ordinaryRegionAuthorizationStatusAsOwner = runtime::ordinaryRegionAuthorizationStatusAsOwner,
        authorizeOrdinaryRegionAsOwner = runtime::authorizeOrdinaryRegionIngestionAsOwner,
        executeOrdinaryRegionAsOwner = runtime::executeOrdinaryRegionIngestionAsOwner,
        executeGovernedAsOwner = { id, expected ->
            projectGovernedExecution(runtime.executeGovernedAcquisitionAsOwner(id, expected))
        },
    )
    return OwnerUiRuntimeSession(
        startRuntime = runtime::start,
        shutdownRuntime = runtime::shutdown,
        interaction = interaction,
        evidenceOperations = evidenceOperations,
    )
}
