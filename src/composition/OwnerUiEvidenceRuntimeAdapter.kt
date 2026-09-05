package parker.composition

import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeProducerIdentity
import parker.core.interfaces.DocumentAnalysisInvocationResult
import parker.core.interfaces.DocumentAnalysisOutcome
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceGenerationSelection
import parker.core.interfaces.OcrDerivativeExtractedResult
import parker.core.interfaces.OwnerDocumentAnalysisRequest
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.PendingAnalysisId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RetrieveSavedAnalysisOutcome
import parker.core.interfaces.SaveAnalysisOutcome
import parker.core.interfaces.SavedAnalysisId
import parker.core.interfaces.SavedAnalysisSummary
import parker.core.interfaces.TierAContentRetrievalOutcome
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierADocumentFormat
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome
import parker.core.interfaces.TierBOcrContentRetrievalOutcome
import parker.core.interfaces.TierBOcrOwnerInvocationOutcome
import parker.core.interfaces.ExternalTranscriptionOwnerInvocationOutcome
import parker.core.interfaces.OcrModelSnapshot
import parker.core.interfaces.HumanVerificationRecord
import parker.core.interfaces.EffectiveHumanFidelityReviewProjectionOutcome
import parker.core.runtime.ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID
import parker.ui.EvidenceImportOutcome
import parker.ui.OwnerDerivativeProducerSummary
import parker.ui.OwnerDocumentAnalysisInvocationOutcome
import parker.ui.OwnerDocumentAnalysisOutcome
import parker.ui.OwnerDocumentAnalysisPresentation
import parker.ui.OwnerDocumentEvidenceReference
import parker.ui.OwnerDocxTableSummary
import parker.ui.OwnerEmlAttachmentSummary
import parker.ui.OwnerEmlBodySummary
import parker.ui.OwnerEvidenceOperations
import parker.ui.OwnerOcrSegmentSummary
import parker.ui.OwnerPdfMetadataValue
import parker.ui.OwnerRetrieveSavedAnalysisOutcome
import parker.ui.OwnerSaveAnalysisOutcome
import parker.ui.OwnerSavedAnalysisPresentation
import parker.ui.OwnerSavedAnalysisSummary
import parker.ui.OwnerTierAContent
import parker.ui.OwnerHumanFidelityStatus
import parker.ui.OwnerHumanCorrectedRepresentation
import parker.ui.OwnerTierBOcrContent
import parker.ui.transcriptionFidelityLabel
import parker.ui.TierAContentRetrievalResult
import parker.ui.TierAProcessingOutcome
import parker.ui.TierBDurableProcessingOutcome
import parker.ui.TierBOcrContentRetrievalResult
import parker.ui.TierBProcessingOutcome
import parker.ui.EnhancedTranscriptionOutcome
import parker.ui.EnhancedTranscriptionReadiness
import parker.ui.OwnerOcrDerivativeGenerationSummary
import parker.ui.OwnerOcrPageOutcomeSummary
import parker.ui.OwnerOcrUncertaintySpanSummary
import parker.ui.OwnerAcquisitionDecisionView
import parker.ui.OwnerAcquisitionExecutionView
import parker.ui.OwnerAcquisitionSourceFacts
import parker.ui.OwnerAcquisitionCapabilityView
import parker.core.runtime.GovernedAcquisitionOwnerEvaluation
import parker.core.runtime.GovernedAcquisitionOwnerExecution
import parker.core.runtime.GovernedAcquisitionExecutionResult
import parker.core.runtime.OrdinaryRegionCapabilityDisposition
import parker.core.runtime.OrdinaryRegionProposal
import parker.core.runtime.OrdinaryRegionOwnerAuthorizationDisposition
import parker.core.runtime.OrdinaryRegionOwnerAuthorizationOutcome
import parker.core.runtime.OrdinaryRegionOwnerAuthorizationView
import parker.core.runtime.OrdinaryRegionOwnerResult
import parker.core.runtime.OrdinaryRegionDisposition
import parker.ui.OwnerOrdinaryRegionAuthorizationView
import parker.ui.OwnerOrdinaryRegionExecutionView
import parker.ui.OwnerExternalTranscriptionAuthorizationView
import parker.ui.OwnerRegisteredEvidenceView
import parker.core.runtime.OwnerRegisteredEvidence
import parker.core.runtime.ORDINARY_REGION_CAPABILITY_ID
import parker.core.interfaces.*

/** OCR Mechanism Unit 12's own, unmodified, already-governed analysisKind convention. */
private const val OCR_ANALYSIS_KIND = "ocr-transcription"

internal fun projectGovernedDecision(evaluation: GovernedAcquisitionOwnerEvaluation): OwnerAcquisitionDecisionView = when (evaluation) {
    is GovernedAcquisitionOwnerEvaluation.SourceUnavailable -> OwnerAcquisitionDecisionView.NoEligible(
        OwnerAcquisitionSourceFacts(evaluation.evidenceArtifactId.value, null, null, null, "UNKNOWN", "UNKNOWN", null, "UNKNOWN", "UNKNOWN", "UNKNOWN"),
        listOf(evaluation.reason),
    )
    is GovernedAcquisitionOwnerEvaluation.Evaluated -> when (val routing = evaluation.routing) {
        is EvidenceAcquisitionRoutingOutcome.Selected -> OwnerAcquisitionDecisionView.Selected(
            sourceView(evaluation.source), capabilityView(routing.decision.capability, routing.decision),
            "Parker selected ${mechanismLabel(routing.decision.capability.mechanism)} from established technical source characteristics.",
        )
        is EvidenceAcquisitionRoutingOutcome.NoEligibleCapability -> OwnerAcquisitionDecisionView.NoEligible(
            sourceView(evaluation.source), routing.reasons.map { it.name }.sorted(),
        )
        is EvidenceAcquisitionRoutingOutcome.Indeterminate -> OwnerAcquisitionDecisionView.Indeterminate(
            sourceView(evaluation.source), routing.reasons.map { it.name }.sorted(),
        )
        is EvidenceAcquisitionRoutingOutcome.Ambiguous -> OwnerAcquisitionDecisionView.Ambiguous(
            sourceView(evaluation.source), routing.capabilityIds.sorted(), routing.reasons.map { it.name }.sorted(),
        )
    }
}

internal fun projectGovernedExecution(execution: GovernedAcquisitionOwnerExecution): OwnerAcquisitionExecutionView = when (execution) {
    is GovernedAcquisitionOwnerExecution.StaleOrUnavailable ->
        OwnerAcquisitionExecutionView.StaleDecision(projectGovernedDecision(execution.current))
    is GovernedAcquisitionOwnerExecution.Executed -> when (val result = execution.result) {
        is GovernedAcquisitionExecutionResult.Admitted -> OwnerAcquisitionExecutionView.Admitted(
            result.derivativeGenerationId, execution.source.evidenceArtifactId.value,
            capabilityView(execution.decision.capability, execution.decision),
            result.fidelity?.name ?: "NOT_REPORTED",
            result.completeness?.name ?: "NOT_REPORTED",
        )
        is GovernedAcquisitionExecutionResult.Failed -> OwnerAcquisitionExecutionView.Failed(
            result.reason.name, capabilityView(execution.decision.capability, execution.decision),
        )
    }
}

private fun sourceView(source: AcquisitionSource) = OwnerAcquisitionSourceFacts(
    source.evidenceArtifactId.value, source.mediaType, source.byteLength,
    (source.pageCount as? AcquisitionPageCount.Known)?.value,
    source.characteristics.nativeSearchableText.name, source.characteristics.imageOnlyOrScanned.name,
    source.characteristics.mixedTextAndImage.name, source.characteristics.handwriting.name,
    source.characteristics.complexLayout.name, source.characteristics.tables.name,
)

private fun capabilityView(capability: EvidenceAcquisitionCapability, decision: EvidenceAcquisitionRoutingDecision) =
    OwnerAcquisitionCapabilityView(
        capability.capabilityId, mechanismLabel(capability.mechanism),
        if (capability.egress == AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED) "EXTERNAL" else "LOCAL",
        capability.egress == AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED,
        capability.providerConfiguration?.providerIdentity, capability.providerConfiguration?.modelSelectionRule,
        capability.providerConfiguration?.profileIdentity,
        when (decision.selectedRepresentation) {
            AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY -> "Authoritative source or byte-exact copy"
            AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION -> "Directly derived processing representation"
        },
        if (capability.availability is AcquisitionAvailability.Available) "READY" else "BLOCKED",
        decision.selectionReasons.map { it.name }.sorted(),
        capability.providerConfiguration?.configurationIdentity,
        capability.providerConfiguration?.processingProfileIdentity,
        capability.providerConfiguration?.instructionSha256,
        capability.providerConfiguration?.schemaSha256,
        capability.providerConfiguration?.reasoningEffort,
        capability.providerConfiguration?.store,
        capability.providerConfiguration?.pdfDetail,
        capability.providerConfiguration?.imageDetail,
    )

private fun mechanismLabel(mechanism: EvidenceAcquisitionMechanism) = when (mechanism) {
    EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION -> "Native text extraction"
    EvidenceAcquisitionMechanism.LOCAL_OCR -> "Local OCR"
    EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION,
    EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION,
    -> "External transcription"
}

/**
 * Owner Evidence Upload & Processing (first version). Narrow composition
 * adapter, mirroring [OwnerUiRuntimeAdapter]'s own "receives only
 * ParkerRuntime's own method capability, never ParkerRuntime itself or any
 * other runtime method" discipline exactly: three function-reference
 * parameters, each already-governed, already-implemented, already-accepted
 * (`importEvidenceFileAsOwner`/`invokeTierAIngestionAsOwner`/`analyseEvidence`),
 * and nothing else. This class implements no OCR, no Tier A parsing, no
 * source retrieval, no permission evaluation of its own -- it only calls
 * through and maps outcomes truthfully.
 *
 * [ownerPrincipalId] is used only to construct the `requestingPrincipalId`
 * [analyseEvidence] itself already requires -- this adapter never accepts a
 * caller-supplied principal (its own [OwnerEvidenceOperations.processTierB]
 * declares none), so every UI-originated Tier B call is, from this seam
 * onward, structurally forced to the configured owner, exactly mirroring
 * [invokeTierAIngestionAsOwner]'s own already-accepted "no principal
 * parameter" shape -- the narrow closing OCR Mechanism Unit 12 Runtime
 * Invocation Scope Lock §6 names as available to "a future... helper"
 * without requiring it or touching `analyseEvidence` itself.
 */
class OwnerUiEvidenceRuntimeAdapter(
    private val ownerPrincipalId: PrincipalId,
    private val listRegisteredEvidenceAsOwner: suspend () -> List<OwnerRegisteredEvidence> = { emptyList() },
    private val importEvidenceFileAsOwner: suspend (String, String?) -> OwnerLocalFileIngressOutcome,
    private val importUploadedEvidenceFileAsOwner: (suspend (String, String?, String) -> OwnerLocalFileIngressOutcome)? = null,
    private val invokeTierAIngestionAsOwner: suspend (EvidenceArtifactId) -> TierAOwnerInvocationOutcome,
    private val analyseEvidence: suspend (PrincipalId, EvidenceAnalysisRequest) -> EvidenceIntelligenceInvocationOutcome,
    private val retrieveTierAExtractedContentAsOwner: suspend (EvidenceArtifactId, DerivativeGenerationId) -> TierAContentRetrievalOutcome,
    private val invokeTierBOcrDurableGenerationAsOwner: suspend (EvidenceArtifactId) -> TierBOcrOwnerInvocationOutcome,
    private val retrieveTierBOcrContentAsOwner: suspend (EvidenceArtifactId, DerivativeGenerationId) -> TierBOcrContentRetrievalOutcome,
    private val discoverOcrDerivativeGenerationsAsOwner: suspend (EvidenceArtifactId) -> List<DerivativeGenerationRecord> = { emptyList() },
    private val analyseDocumentsAsOwner: suspend (OwnerDocumentAnalysisRequest) -> DocumentAnalysisInvocationResult,
    private val saveAnalysisAsOwner: suspend (PendingAnalysisId) -> SaveAnalysisOutcome,
    private val retrieveSavedAnalysisAsOwner: suspend (SavedAnalysisId) -> RetrieveSavedAnalysisOutcome,
    private val listSavedAnalysesAsOwner: suspend () -> List<SavedAnalysisSummary>,
    private val externalReadiness: () -> EnhancedTranscriptionReadiness = { EnhancedTranscriptionReadiness.Disabled },
    private val invokeExternalTranscriptionAsOwner: suspend (EvidenceArtifactId) -> ExternalTranscriptionOwnerInvocationOutcome = {
        ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed("Enhanced transcription is not enabled in this runtime")
    },
    private val listHumanVerificationRecordsAsOwner: suspend (EvidenceArtifactId, DerivativeGenerationId) -> List<HumanVerificationRecord> = { _, _ -> emptyList() },
    private val governedDecisionAsOwner: suspend (EvidenceArtifactId) -> OwnerAcquisitionDecisionView = {
        OwnerAcquisitionDecisionView.Indeterminate(
            OwnerAcquisitionSourceFacts(it.value, null, null, null, "UNKNOWN", "UNKNOWN", null, "UNKNOWN", "UNKNOWN", "UNKNOWN"),
            listOf("Required technical source characteristics are not established."),
        )
    },
    private val ordinaryRegionProposalAsOwner: suspend (EvidenceArtifactId) -> OrdinaryRegionProposal? = { null },
    private val ordinaryRegionAuthorizationStatusAsOwner: suspend (EvidenceArtifactId) -> OrdinaryRegionOwnerAuthorizationView = {
        OrdinaryRegionOwnerAuthorizationView(OrdinaryRegionOwnerAuthorizationDisposition.UNAVAILABLE, it.value, detail = "AUTHORIZATION_LANE_NOT_CONFIGURED")
    },
    private val authorizeOrdinaryRegionAsOwner: suspend (EvidenceArtifactId) -> OrdinaryRegionOwnerAuthorizationOutcome = {
        OrdinaryRegionOwnerAuthorizationOutcome.Blocked(OrdinaryRegionOwnerAuthorizationView(
            OrdinaryRegionOwnerAuthorizationDisposition.UNAVAILABLE, it.value, detail = "AUTHORIZATION_LANE_NOT_CONFIGURED"))
    },
    private val executeOrdinaryRegionAsOwner: suspend (EvidenceArtifactId, String, String, String) -> OrdinaryRegionOwnerResult =
        { _, _, _, _ -> OrdinaryRegionOwnerResult(OrdinaryRegionDisposition.CAPABILITY_NOT_ACCEPTED, "EXECUTION_LANE_NOT_CONFIGURED") },
    private val executionIdentity: () -> String = { java.util.UUID.randomUUID().toString() },
    private val executeGovernedAsOwner: suspend (EvidenceArtifactId, String) -> OwnerAcquisitionExecutionView = { _, _ ->
        OwnerAcquisitionExecutionView.Failed("SELECTED_CAPABILITY_UNAVAILABLE")
    },
    // UI-INGESTION-5: exact-target enhanced-transcription owner authorization. Defaults preserve
    // "lane not configured" for every existing caller unless explicitly wired.
    private val externalTranscriptionAuthorizationStatusAsOwner: suspend (EvidenceArtifactId) -> parker.core.runtime.ExternalTranscriptionAuthorizationView = {
        parker.core.runtime.ExternalTranscriptionAuthorizationView(
            parker.core.runtime.ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, it.value,
            detail = "AUTHORIZATION_LANE_NOT_CONFIGURED",
        )
    },
    private val authorizeExternalTranscriptionAsOwner: suspend (EvidenceArtifactId, String?) -> parker.core.runtime.ExternalTranscriptionAuthorizationView = { id, _ ->
        parker.core.runtime.ExternalTranscriptionAuthorizationView(
            parker.core.runtime.ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, id.value,
            detail = "AUTHORIZATION_LANE_NOT_CONFIGURED",
        )
    },
    // HFR Owner UI exposure scope lock amendment: null default preserves "not configured" for
    // every existing caller unless explicitly wired -- mirroring every other optional collaborator
    // in this constructor.
    private val recordHumanFidelityReviewAsOwner: (suspend (
        EvidenceArtifactId,
        DerivativeGenerationId,
        parker.core.runtime.TierBHumanFidelityReviewSubmission,
    ) -> parker.core.runtime.TierBHumanFidelityReviewRecordingOutcome)? = null,
    private val projectEffectiveHumanFidelityReviewAsOwner: (suspend (
        EvidenceArtifactId,
        DerivativeGenerationId,
    ) -> parker.core.runtime.TierBEffectiveHumanFidelityReviewOutcome)? = null,
) : OwnerEvidenceOperations {

    override suspend fun listRegisteredEvidence(): List<OwnerRegisteredEvidenceView> =
        listRegisteredEvidenceAsOwner().map {
            OwnerRegisteredEvidenceView(it.evidenceArtifactId.value, it.sha256, it.byteLength,
                it.mediaType, it.originalFileName, it.registeredAt.toString())
        }

    override suspend fun governedAcquisitionDecision(evidenceArtifactId: EvidenceArtifactId): OwnerAcquisitionDecisionView {
        val governed = governedDecisionAsOwner(evidenceArtifactId)
        val proposal = ordinaryRegionProposalAsOwner(evidenceArtifactId)
        if (proposal == null || proposal.capabilityStatus.disposition != OrdinaryRegionCapabilityDisposition.ACCEPTED) return governed
        val status = proposal.capabilityStatus
        val isV8 = proposal.capabilityId == ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID
        val authorization = ordinaryRegionAuthorizationStatusAsOwner(evidenceArtifactId)
        return OwnerAcquisitionDecisionView.Proposed(
            source = governed.source,
            capability = OwnerAcquisitionCapabilityView(
                capabilityId = status.capabilityId,
                mechanismLabel = "External region transcription",
                executionLocation = "EXTERNAL",
                externalEgressRequired = true,
                provider = status.provider,
                modelRule = status.model,
                profile = status.providerProfile,
                representationLabel = "Selected authoritative PDF evidence crops",
                availability = "READY",
                selectionReasons = listOf(if (isV8) "ACCEPTED_REQUEST_REGION_V8_CAPABILITY" else "ACCEPTED_ORDINARY_REGION_CAPABILITY", "PDF_SOURCE"),
                configurationIdentity = "${status.adapterId}:${status.adapterVersion}:wire-${status.wireVersion}",
                processingProfile = status.providerProfile,
                store = false,
                imageDetail = "original",
            ),
            explanation = "The governed ordinary ${if (isV8) "request-region-v8" else "region"} capability is accepted for this PDF. Owner review and separate evidence-specific authorization are required before any external processing.",
            disclosure = proposal.disclosure,
            egressAuthorization = authorization.disposition.name,
            nextStep = if (authorization.disposition == OrdinaryRegionOwnerAuthorizationDisposition.AUTHORISED)
                "SEPARATE_EXECUTION_ACTION_REQUIRED" else "OWNER_AUTHORIZATION_REQUIRED",
            authorizationAvailable = authorization.disposition == OrdinaryRegionOwnerAuthorizationDisposition.NOT_AUTHORISED,
            authorizationId = authorization.authorizationId,
            authorizationExpiresAt = authorization.expiresAt?.toString(),
            executeAvailable = authorization.disposition == OrdinaryRegionOwnerAuthorizationDisposition.AUTHORISED &&
                authorization.executionState == "NOT_STARTED",
            executionState = authorization.executionState,
        )
    }

    override suspend fun authorizeOrdinaryRegionTranscription(evidenceArtifactId: EvidenceArtifactId): OwnerOrdinaryRegionAuthorizationView {
        val view = when (val outcome = authorizeOrdinaryRegionAsOwner(evidenceArtifactId)) {
            is OrdinaryRegionOwnerAuthorizationOutcome.Created -> outcome.view
            is OrdinaryRegionOwnerAuthorizationOutcome.Existing -> outcome.view
            is OrdinaryRegionOwnerAuthorizationOutcome.Blocked -> outcome.view
        }
        return OwnerOrdinaryRegionAuthorizationView(view.disposition.name, view.evidenceArtifactId, view.provider,
            view.disclosure, view.detail, view.authorizationId, view.expiresAt?.toString())
    }

    override suspend fun executeOrdinaryRegionTranscription(evidenceArtifactId: EvidenceArtifactId): OwnerOrdinaryRegionExecutionView {
        val proposal = ordinaryRegionProposalAsOwner(evidenceArtifactId)
            ?: return OwnerOrdinaryRegionExecutionView("UNAVAILABLE", "SOURCE_UNAVAILABLE_OR_UNSUPPORTED", evidenceArtifactId.value)
        if (proposal.capabilityStatus.disposition != OrdinaryRegionCapabilityDisposition.ACCEPTED)
            return OwnerOrdinaryRegionExecutionView("CAPABILITY_NOT_ACCEPTED", "CAPABILITY_NOT_ACCEPTED", evidenceArtifactId.value)
        val authorization = ordinaryRegionAuthorizationStatusAsOwner(evidenceArtifactId)
        val authorizationId = authorization.authorizationId
        if (authorization.disposition != OrdinaryRegionOwnerAuthorizationDisposition.AUTHORISED ||
            authorization.executionState != "NOT_STARTED" || authorizationId == null)
            return OwnerOrdinaryRegionExecutionView("UNAVAILABLE", authorization.detail ?: authorization.executionState, evidenceArtifactId.value)
        val nonce = executionIdentity()
        val result = executeOrdinaryRegionAsOwner(evidenceArtifactId, authorizationId, "ordinary-exec-$nonce", "ordinary-attempt-$nonce")
        return OwnerOrdinaryRegionExecutionView(result.disposition.name, result.detail, evidenceArtifactId.value,
            result.derivativeGenerationId)
    }

    override suspend fun executeGovernedAcquisition(
        evidenceArtifactId: EvidenceArtifactId,
        expectedCapabilityId: String,
    ): OwnerAcquisitionExecutionView = executeGovernedAsOwner(evidenceArtifactId, expectedCapabilityId)

    override fun enhancedTranscriptionReadiness(): EnhancedTranscriptionReadiness = externalReadiness()

    override suspend fun transcribeExternal(evidenceArtifactId: EvidenceArtifactId): EnhancedTranscriptionOutcome {
        val readiness = externalReadiness()
        if (readiness !is EnhancedTranscriptionReadiness.Ready) return EnhancedTranscriptionOutcome.NotReady(readiness)
        return when (val outcome = invokeExternalTranscriptionAsOwner(evidenceArtifactId)) {
            is ExternalTranscriptionOwnerInvocationOutcome.Admitted ->
                EnhancedTranscriptionOutcome.Admitted(toOwnerOcrContent(outcome.extracted), outcome.record.derivativeGenerationId)
            is ExternalTranscriptionOwnerInvocationOutcome.ReconciliationRequired ->
                EnhancedTranscriptionOutcome.ReconciliationRequired(toOwnerOcrContent(outcome.extracted), outcome.record.derivativeGenerationId, "The transcription was admitted, but its final audit entry requires reconciliation.")
            ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised -> EnhancedTranscriptionOutcome.Failed("Enhanced transcription was not authorised.")
            is ExternalTranscriptionOwnerInvocationOutcome.SourceNotFound -> EnhancedTranscriptionOutcome.Failed("The evidence source was not found.")
            is ExternalTranscriptionOwnerInvocationOutcome.SourceRetrievalRejected -> EnhancedTranscriptionOutcome.Failed("The evidence source could not be retrieved.")
            is ExternalTranscriptionOwnerInvocationOutcome.ManifestNotFound -> EnhancedTranscriptionOutcome.Failed("No evidence manifest was found.")
            is ExternalTranscriptionOwnerInvocationOutcome.ManifestRejected -> EnhancedTranscriptionOutcome.Failed("The evidence manifest could not be verified.")
            is ExternalTranscriptionOwnerInvocationOutcome.ByteLengthMismatch,
            is ExternalTranscriptionOwnerInvocationOutcome.DigestMismatch -> EnhancedTranscriptionOutcome.Failed("Evidence integrity verification failed.")
            is ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds -> EnhancedTranscriptionOutcome.Failed("This evidence type or size is not supported for enhanced transcription.")
            is ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure -> EnhancedTranscriptionOutcome.Failed(safeExternalFailure(outcome.reason))
            is ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected -> EnhancedTranscriptionOutcome.Failed("The transcription result did not pass Parker validation.")
            is ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed -> EnhancedTranscriptionOutcome.Failed("The validated transcription could not be durably admitted.")
        }
    }

    override suspend fun externalTranscriptionAuthorizationStatus(evidenceArtifactId: EvidenceArtifactId): OwnerExternalTranscriptionAuthorizationView =
        toOwnerExternalTranscriptionAuthorizationView(externalTranscriptionAuthorizationStatusAsOwner(evidenceArtifactId))

    override suspend fun authorizeExternalTranscription(
        evidenceArtifactId: EvidenceArtifactId,
        verificationCredential: String?,
    ): OwnerExternalTranscriptionAuthorizationView =
        toOwnerExternalTranscriptionAuthorizationView(authorizeExternalTranscriptionAsOwner(evidenceArtifactId, verificationCredential))

    private fun toOwnerExternalTranscriptionAuthorizationView(
        view: parker.core.runtime.ExternalTranscriptionAuthorizationView,
    ) = OwnerExternalTranscriptionAuthorizationView(
        view.disposition.name, view.evidenceArtifactId, view.provider, view.purpose, view.disclosure,
        view.approvedAt?.toString(), view.detail,
    )

    private fun safeExternalFailure(reason: String): String = when (reason) {
        "PROVIDER_AUTHENTICATION_FAILURE" -> "The transcription provider rejected authentication."
        "PROVIDER_RATE_LIMITED" -> "The transcription provider is temporarily rate limited."
        "PROVIDER_UNAVAILABLE" -> "The transcription provider is temporarily unavailable."
        "PROVIDER_TIMEOUT" -> "Enhanced transcription timed out."
        "PROVIDER_NETWORK_FAILURE" -> "Enhanced transcription could not reach the provider."
        "MALFORMED_PROVIDER_RESPONSE" -> "The transcription provider returned an invalid result."
        "INPUT_TOO_LARGE", "ENCODED_INPUT_TOO_LARGE" -> "This evidence exceeds the enhanced transcription size limit."
        else -> "Enhanced transcription failed safely."
    }

    override suspend fun importFile(absolutePath: String, declaredMediaType: String?): EvidenceImportOutcome =
        when (val outcome = importEvidenceFileAsOwner(absolutePath, declaredMediaType)) {
            is OwnerLocalFileIngressOutcome.Accepted ->
                EvidenceImportOutcome.Imported(outcome.acceptedEvidenceArtifact.evidenceArtifactId)
            is OwnerLocalFileIngressOutcome.AuthorizationRejected ->
                EvidenceImportOutcome.Rejected("Import was not authorised.")
            OwnerLocalFileIngressOutcome.InvalidPath ->
                EvidenceImportOutcome.Rejected("The selected file could not be read.")
            OwnerLocalFileIngressOutcome.PathNotFound ->
                EvidenceImportOutcome.Rejected("The selected file no longer exists.")
            OwnerLocalFileIngressOutcome.SymlinkProhibited ->
                EvidenceImportOutcome.Rejected("Symlinked files are not accepted.")
            OwnerLocalFileIngressOutcome.NotARegularFile ->
                EvidenceImportOutcome.Rejected("Only regular files are accepted.")
            is OwnerLocalFileIngressOutcome.SourceTooLarge ->
                EvidenceImportOutcome.Rejected("The selected file exceeds the maximum accepted size.")
            is OwnerLocalFileIngressOutcome.SourceReadFailure ->
                EvidenceImportOutcome.Failed("The selected file could not be read.")
            is OwnerLocalFileIngressOutcome.EvidenceCustodianRejected ->
                EvidenceImportOutcome.Rejected("Evidence custody did not accept this file.")
        }

    override suspend fun importUploadedFile(
        absolutePath: String,
        declaredMediaType: String?,
        originalFileName: String,
    ): EvidenceImportOutcome = mapImportOutcome(
        importUploadedEvidenceFileAsOwner?.invoke(absolutePath, declaredMediaType, originalFileName)
            ?: importEvidenceFileAsOwner(absolutePath, declaredMediaType),
    )

    private fun mapImportOutcome(outcome: OwnerLocalFileIngressOutcome): EvidenceImportOutcome = when (outcome) {
        is OwnerLocalFileIngressOutcome.Accepted -> EvidenceImportOutcome.Imported(outcome.acceptedEvidenceArtifact.evidenceArtifactId)
        is OwnerLocalFileIngressOutcome.AuthorizationRejected -> EvidenceImportOutcome.Rejected("Import was not authorised.")
        OwnerLocalFileIngressOutcome.InvalidPath -> EvidenceImportOutcome.Rejected("The selected file could not be read.")
        OwnerLocalFileIngressOutcome.PathNotFound -> EvidenceImportOutcome.Rejected("The selected file no longer exists.")
        OwnerLocalFileIngressOutcome.SymlinkProhibited -> EvidenceImportOutcome.Rejected("Symlinked files are not accepted.")
        OwnerLocalFileIngressOutcome.NotARegularFile -> EvidenceImportOutcome.Rejected("Only regular files are accepted.")
        is OwnerLocalFileIngressOutcome.SourceTooLarge -> EvidenceImportOutcome.Rejected("The selected file exceeds the maximum accepted size.")
        is OwnerLocalFileIngressOutcome.SourceReadFailure -> EvidenceImportOutcome.Failed("The selected file could not be read.")
        is OwnerLocalFileIngressOutcome.EvidenceCustodianRejected -> EvidenceImportOutcome.Rejected("Evidence custody did not accept this file.")
    }

    override suspend fun processTierA(evidenceArtifactId: EvidenceArtifactId): TierAProcessingOutcome =
        when (val outcome = invokeTierAIngestionAsOwner(evidenceArtifactId)) {
            is TierAOwnerInvocationOutcome.Routed -> mapRoutingResult(outcome.result)
            is TierAOwnerInvocationOutcome.ManifestRetrievalRejected ->
                TierAProcessingOutcome.Failed("MANIFEST", "The evidence manifest could not be retrieved.")
            is TierAOwnerInvocationOutcome.ManifestNotFound ->
                TierAProcessingOutcome.Failed("MANIFEST", "No evidence manifest was found for this artefact.")
            is TierAOwnerInvocationOutcome.SourceRetrievalRejected ->
                TierAProcessingOutcome.Failed("SOURCE", "The evidence source could not be retrieved.")
            is TierAOwnerInvocationOutcome.SourceNotFound ->
                TierAProcessingOutcome.Failed("SOURCE", "The evidence source was not found.")
            is TierAOwnerInvocationOutcome.ByteLengthMismatch ->
                TierAProcessingOutcome.IntegrityFailure("stored byte length does not match the evidence manifest")
            is TierAOwnerInvocationOutcome.DigestMismatch ->
                TierAProcessingOutcome.IntegrityFailure("stored content does not match the evidence manifest")
        }

    private fun mapRoutingResult(result: TierADocumentRoutingResult): TierAProcessingOutcome = when (result) {
        is TierADocumentRoutingResult.Admitted ->
            TierAProcessingOutcome.Admitted(
                formatLabel(result.format),
                toOwnerContent(result.payload, result.record),
                result.record.derivativeGenerationId,
            )
        is TierADocumentRoutingResult.RequiresTierB -> TierAProcessingOutcome.RequiresTierB
        is TierADocumentRoutingResult.Unsupported -> TierAProcessingOutcome.Unsupported(result.reason)
        is TierADocumentRoutingResult.ExtractionFailed -> TierAProcessingOutcome.Failed("EXTRACTION", result.reason)
        is TierADocumentRoutingResult.SourceIntegrityFailed -> TierAProcessingOutcome.IntegrityFailure(result.reason)
        is TierADocumentRoutingResult.AdmissionFailed -> TierAProcessingOutcome.Failed(result.stage, result.reason)
        is TierADocumentRoutingResult.ReconciliationRequired -> TierAProcessingOutcome.Failed("RECONCILIATION_REQUIRED", result.reason)
    }

    private fun formatLabel(format: TierADocumentFormat): String = when (format) {
        TierADocumentFormat.CSV -> "CSV"
        TierADocumentFormat.EML -> "EML"
        TierADocumentFormat.DOCX -> "DOCX"
        TierADocumentFormat.PDF -> "PDF"
    }

    /**
     * Owner Tier A Extracted Content Presentation. Projects the specialist's
     * own already-computed [TierADerivativePayload] into the safe,
     * owner-facing [OwnerTierAContent] shape -- never a re-extraction, never
     * raw internal object serialization. PDF is presented at full fidelity
     * ([parker.core.interfaces.PdfStructuralResult.documentText] verbatim).
     * CSV rows are bounded for display only ([OwnerTierAContent.CSV_PREVIEW_ROW_LIMIT])
     * with the real total count always disclosed, never silently truncated.
     */
    private fun toOwnerContent(
        payload: TierADerivativePayload,
        record: DerivativeGenerationRecord,
        humanFidelityProjection: EffectiveHumanFidelityReviewProjectionOutcome? = null,
        corrected: parker.core.interfaces.HumanCorrectedRepresentationPresentation? = null,
    ): OwnerTierAContent = when (payload) {
        is TierADerivativePayload.Pdf -> payload.value.let { r ->
            OwnerTierAContent.Pdf(
                documentText = r.documentText,
                pageCount = r.pageCount,
                pageTextAssociationAvailable = r.pageTextAssociationAvailable,
                producer = r.producerIdentity.toSummary(),
                transformationHistory = r.transformationHistory.map { it.name },
                completenessState = r.completenessState.name,
                warnings = r.warnings,
                metadata = r.metadata.map { OwnerPdfMetadataValue(it.name, it.value) },
            )
        }
        is TierADerivativePayload.Csv -> payload.value.let { r ->
            val preview = r.rows.take(OwnerTierAContent.CSV_PREVIEW_ROW_LIMIT)
            OwnerTierAContent.Csv(
                headers = r.headers,
                previewRows = preview,
                totalRowCount = r.rows.size,
                rowsTruncatedForDisplay = preview.size < r.rows.size,
                producer = r.producerIdentity.toSummary(),
                completenessState = r.completenessState.name,
                warnings = r.warnings,
            )
        }
        is TierADerivativePayload.Eml -> payload.value.let { r ->
            OwnerTierAContent.Eml(
                from = r.from,
                to = r.to,
                cc = r.cc,
                subject = r.subject,
                rawDate = r.rawDate,
                messageId = r.messageId,
                bodyAlternatives = r.bodyAlternatives.map { OwnerEmlBodySummary(it.mediaType, it.charset, it.decodedText) },
                attachmentCandidateCount = r.attachmentCandidates.size,
                attachmentCandidates = r.attachmentCandidates.map {
                    OwnerEmlAttachmentSummary(it.filename, it.declaredMimeType, it.byteLength)
                },
                producer = r.producerIdentity.toSummary(),
                completenessState = r.completenessState.name,
                warnings = r.warnings,
            )
        }
        is TierADerivativePayload.Docx -> payload.value.let { r ->
            OwnerTierAContent.Docx(
                paragraphs = r.paragraphs.sortedBy { it.order }.map { it.text },
                tables = r.tables.sortedBy { it.order }.map { table ->
                    OwnerDocxTableSummary(
                        table.rows.sortedBy { it.order }.map { row -> row.cells.sortedBy { it.order }.map { it.text } },
                    )
                },
                headers = r.headers.flatMap { headerFooter -> headerFooter.paragraphs.sortedBy { it.order }.map { it.text } },
                footers = r.footers.flatMap { headerFooter -> headerFooter.paragraphs.sortedBy { it.order }.map { it.text } },
                producer = r.producerIdentity.toSummary(),
                completenessState = r.completenessState.name,
                warnings = r.warnings,
            )
        }
        is TierADerivativePayload.Ocr -> error(
            "TierADerivativePayload.Ocr is never routed through Tier A content presentation -- " +
                "it is retrieved exclusively via retrieveTierBOcrContent/toOwnerOcrContent",
        )
        is TierADerivativePayload.RegionTranscription -> payload.value.let { value ->
            val digest = record.contentIdentity as? DerivativeContentIdentity.Digest
            OwnerTierAContent.RegionTranscription(
                derivativeGenerationId = record.derivativeGenerationId.value,
                derivativeKind = record.derivativeKind,
                contentIdentityAlgorithm = digest?.algorithm,
                contentIdentityDigest = digest?.digest,
                evidenceArtifactId = value.evidenceArtifactId,
                sourceSha256 = value.sourceSha256,
                representationVersion = value.representationVersion,
                capabilityId = value.capabilityId,
                capabilityDigest = value.capabilityDigest,
                pageBindings = value.pageBindings,
                regionBindings = value.regionBindings,
                transcriptionBlocks = value.transcriptionBlocks,
                providerReturnedOrder = value.providerReturnedOrder,
                parkerSourceOrder = value.parkerSourceOrder,
                provider = value.provider,
                providerProfile = value.providerProfile,
                model = value.model,
                responseIdentity = value.responseIdentity,
                providerStateRecordIdentity = value.providerStateRecordIdentity,
                ownerAuthorizationIdentity = value.ownerAuthorizationIdentity,
                executionIdentity = value.executionIdentity,
                attemptIdentity = value.attemptIdentity,
                requestIdentity = value.requestIdentity,
                requestDigest = value.requestDigest,
                preparationIdentity = value.preparationIdentity,
                preparationProfile = value.preparationProfile,
                preparationProfileVersion = value.preparationProfileVersion,
                authorizationPurpose = value.authorizationPurpose,
                processingProfile = value.processingProfile,
                producer = record.producerIdentity.toSummary(),
                transformationHistory = record.transformationHistory.map { it.name },
                completenessState = record.completenessState.name,
                warnings = record.warnings,
                humanFidelityStatus = humanFidelityStatus(humanFidelityProjection),
                humanCorrectedRepresentation = corrected?.let { presentation ->
                    val representation = presentation.representation
                    OwnerHumanCorrectedRepresentation(
                        representation.derivativeGenerationId.value, representation.representationKind, representation.reviewId.value,
                        representation.correctedTranscriptionBlocks, representation.correctedContentSha256.value,
                        representation.proposals.size, presentation.sourceConfirmedEligibility.state.name,
                        presentation.sourceConfirmedEligibility.denialReason?.name,
                    )
                },
            )
        }
    }

    private fun humanFidelityStatus(outcome: EffectiveHumanFidelityReviewProjectionOutcome?): OwnerHumanFidelityStatus =
        when (outcome) {
            is EffectiveHumanFidelityReviewProjectionOutcome.Projected -> outcome.summary.let { summary ->
                OwnerHumanFidelityStatus(
                    summary.projection.effectiveState.name,
                    summary.projection.coverage?.kind?.name,
                    summary.materialDiscrepancyCount,
                    summary.systematicPatternCount,
                    summary.unresolvedConflict,
                    summary.projection.eligibility.state.name,
                    summary.projection.eligibility.denialReason?.name,
                )
            }
            is EffectiveHumanFidelityReviewProjectionOutcome.FailedClosed, null -> OwnerHumanFidelityStatus(
                null, null, 0, 0, false, "DENIED", "MALFORMED_OR_UNSUPPORTED_STATE",
            )
        }

    private fun DerivativeProducerIdentity.toSummary(): OwnerDerivativeProducerSummary = OwnerDerivativeProducerSummary(
        pluginIdentity = pluginIdentity,
        pluginVersion = pluginVersion,
        configurationIdentity = configurationIdentity,
        adapterIdentity = adapterIdentity,
        adapterVersion = adapterVersion,
        modelIdentity = modelIdentity,
        modelVersion = modelVersion,
    )

    override suspend fun retrieveTierAExtractedContent(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): TierAContentRetrievalResult =
        when (val outcome = retrieveTierAExtractedContentAsOwner(evidenceArtifactId, derivativeGenerationId)) {
            is TierAContentRetrievalOutcome.Retrieved ->
                TierAContentRetrievalResult.Retrieved(
                    toOwnerContent(outcome.payload, outcome.record, outcome.humanFidelityProjection, outcome.humanCorrectedRepresentation),
                )
            is TierAContentRetrievalOutcome.UnknownGeneration -> TierAContentRetrievalResult.UnknownGeneration
            is TierAContentRetrievalOutcome.SourceMismatch -> TierAContentRetrievalResult.SourceMismatch
            is TierAContentRetrievalOutcome.ContentMissing -> TierAContentRetrievalResult.ContentMissing
            is TierAContentRetrievalOutcome.ContentCorrupt -> TierAContentRetrievalResult.ContentCorrupt(outcome.reason)
            is TierAContentRetrievalOutcome.UnsupportedRepresentationVersion ->
                TierAContentRetrievalResult.UnsupportedRepresentationVersion(outcome.version)
        }

    override suspend fun processTierB(evidenceArtifactId: EvidenceArtifactId): TierBProcessingOutcome {
        val outcome = analyseEvidence(
            ownerPrincipalId,
            EvidenceAnalysisRequest(
                analysisKind = OCR_ANALYSIS_KIND,
                requestingPrincipalId = ownerPrincipalId,
                evidenceArtifactIds = listOf(evidenceArtifactId),
            ),
        )
        return when (outcome) {
            is EvidenceIntelligenceInvocationOutcome.NotAuthorised ->
                TierBProcessingOutcome.NotAuthorised(outcome.reason)
            is EvidenceIntelligenceInvocationOutcome.Completed ->
                TierBProcessingOutcome.Completed(outcome.acceptanceOutcomes.size)
        }
    }

    override suspend fun processTierBDurable(evidenceArtifactId: EvidenceArtifactId): TierBDurableProcessingOutcome =
        when (val outcome = invokeTierBOcrDurableGenerationAsOwner(evidenceArtifactId)) {
            is TierBOcrOwnerInvocationOutcome.Admitted ->
                TierBDurableProcessingOutcome.Admitted(toOwnerOcrContent(outcome.extracted), outcome.record.derivativeGenerationId)
            is TierBOcrOwnerInvocationOutcome.NotAuthorised -> TierBDurableProcessingOutcome.NotAuthorised(outcome.reason)
            is TierBOcrOwnerInvocationOutcome.MandatoryProvenanceUnavailable ->
                TierBDurableProcessingOutcome.MandatoryProvenanceUnavailable(outcome.reason)
            is TierBOcrOwnerInvocationOutcome.OcrNotAdmissible -> TierBDurableProcessingOutcome.OcrNotAdmissible(outcome.reason)
            is TierBOcrOwnerInvocationOutcome.ManifestNotFound ->
                TierBDurableProcessingOutcome.Failed("MANIFEST", "No evidence manifest was found for this artefact.")
            is TierBOcrOwnerInvocationOutcome.SourceRetrievalRejected ->
                TierBDurableProcessingOutcome.Failed("SOURCE", "The evidence source could not be retrieved.")
            is TierBOcrOwnerInvocationOutcome.SourceNotFound ->
                TierBDurableProcessingOutcome.Failed("SOURCE", "The evidence source was not found.")
            is TierBOcrOwnerInvocationOutcome.ByteLengthMismatch ->
                TierBDurableProcessingOutcome.IntegrityFailure("stored byte length does not match the evidence manifest")
            is TierBOcrOwnerInvocationOutcome.DigestMismatch ->
                TierBDurableProcessingOutcome.IntegrityFailure("stored content does not match the evidence manifest")
            is TierBOcrOwnerInvocationOutcome.NotOcrEligible ->
                TierBDurableProcessingOutcome.Failed("MEDIA_TYPE", "This evidence artefact is not OCR-eligible.")
            is TierBOcrOwnerInvocationOutcome.PreparationFailed ->
                TierBDurableProcessingOutcome.Failed("PREPARATION", "Durable OCR content or record preparation failed.")
            is TierBOcrOwnerInvocationOutcome.AuthorisationAuditFailed ->
                TierBDurableProcessingOutcome.Failed("AUDIT", "The durable generation's authorisation audit entry could not be recorded.")
            is TierBOcrOwnerInvocationOutcome.PublicationFailed ->
                TierBDurableProcessingOutcome.Failed("PUBLICATION", "The durable generation record could not be published.")
            is TierBOcrOwnerInvocationOutcome.AdmittedAuditFailed ->
                // Genuinely admitted (record and content both durably published) but the final
                // ADMITTED audit entry is missing -- reconciliation-required, never presented as
                // an unqualified success (Tier B scope lock §19/§21).
                TierBDurableProcessingOutcome.Failed("RECONCILIATION_REQUIRED", "Admitted, but the audit trail's own final entry is missing.")
        }

    override suspend fun retrieveTierBOcrContent(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): TierBOcrContentRetrievalResult =
        when (val outcome = retrieveTierBOcrContentAsOwner(evidenceArtifactId, derivativeGenerationId)) {
            is TierBOcrContentRetrievalOutcome.Retrieved -> {
                val reviews = listHumanVerificationRecordsAsOwner(evidenceArtifactId, derivativeGenerationId)
                TierBOcrContentRetrievalResult.Retrieved(toOwnerOcrContent(outcome.extracted, reviews.map { it.outcome.name }))
            }
            is TierBOcrContentRetrievalOutcome.UnknownGeneration -> TierBOcrContentRetrievalResult.UnknownGeneration
            is TierBOcrContentRetrievalOutcome.SourceMismatch -> TierBOcrContentRetrievalResult.SourceMismatch
            is TierBOcrContentRetrievalOutcome.WrongDerivativeKind -> TierBOcrContentRetrievalResult.WrongDerivativeKind
            is TierBOcrContentRetrievalOutcome.ContentMissing -> TierBOcrContentRetrievalResult.ContentMissing
            is TierBOcrContentRetrievalOutcome.ContentCorrupt -> TierBOcrContentRetrievalResult.ContentCorrupt(outcome.reason)
            is TierBOcrContentRetrievalOutcome.UnsupportedRepresentationVersion ->
                TierBOcrContentRetrievalResult.UnsupportedRepresentationVersion(outcome.version)
        }

    override suspend fun discoverOcrDerivativeGenerations(evidenceArtifactId: EvidenceArtifactId): List<OwnerOcrDerivativeGenerationSummary> =
        discoverOcrDerivativeGenerationsAsOwner(evidenceArtifactId).map { record ->
            val reviewStates = listHumanVerificationRecordsAsOwner(evidenceArtifactId, record.derivativeGenerationId).map { it.outcome.name }
            OwnerOcrDerivativeGenerationSummary(
                record.derivativeGenerationId.value, record.rootSourceEvidenceArtifactId.value,
                record.generatedAt.toString(), record.operationalOutcome.name,
                humanReviewState = reviewStates.ifEmpty { listOf("UNREVIEWED") }.joinToString(", "),
            )
        }

    override suspend fun recordHumanFidelityReview(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
        submission: parker.ui.OwnerHumanFidelityReviewSubmission,
    ): parker.ui.OwnerHumanFidelityReviewRecordingOutcome {
        val record = recordHumanFidelityReviewAsOwner
            ?: return parker.ui.OwnerHumanFidelityReviewRecordingOutcome.NotConfigured
        val reviewOutcome = runCatching { parker.core.interfaces.HumanFidelityReviewState.valueOf(submission.reviewOutcome) }
            .getOrElse { return parker.ui.OwnerHumanFidelityReviewRecordingOutcome.InvalidSubmission("Unknown reviewOutcome '${submission.reviewOutcome}'") }
        if (reviewOutcome != parker.core.interfaces.HumanFidelityReviewState.HUMAN_REVIEWED_PASS &&
            reviewOutcome != parker.core.interfaces.HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY
        ) {
            return parker.ui.OwnerHumanFidelityReviewRecordingOutcome.InvalidSubmission(
                "reviewOutcome must be HUMAN_REVIEWED_PASS or HUMAN_REVIEWED_WITH_DISCREPANCY",
            )
        }
        val discrepancies = submission.discrepancies.map { d ->
            val classification = runCatching { parker.core.interfaces.FidelityDiscrepancyClassification.valueOf(d.classification) }
                .getOrElse { return parker.ui.OwnerHumanFidelityReviewRecordingOutcome.InvalidSubmission("Unknown discrepancy classification '${d.classification}'") }
            val severity = runCatching { parker.core.interfaces.FidelityDiscrepancySeverity.valueOf(d.severity) }
                .getOrElse { return parker.ui.OwnerHumanFidelityReviewRecordingOutcome.InvalidSubmission("Unknown discrepancy severity '${d.severity}'") }
            parker.core.runtime.TierBFidelityDiscrepancySubmission(
                d.pageNumber, d.exactText,
                classification, severity, d.reason, d.explicitClassificationDetail,
            )
        }
        val outcome = record(
            evidenceArtifactId,
            derivativeGenerationId,
            parker.core.runtime.TierBHumanFidelityReviewSubmission(
                reviewOutcome, submission.reviewedPages, submission.descriptiveFidelity, discrepancies,
            ),
        )
        return when (outcome) {
            is parker.core.runtime.TierBHumanFidelityReviewRecordingOutcome.Recorded ->
                parker.ui.OwnerHumanFidelityReviewRecordingOutcome.Recorded(outcome.reviewId.value)
            is parker.core.runtime.TierBHumanFidelityReviewRecordingOutcome.AlreadyRecorded ->
                parker.ui.OwnerHumanFidelityReviewRecordingOutcome.AlreadyRecorded(outcome.reviewId.value)
            is parker.core.runtime.TierBHumanFidelityReviewRecordingOutcome.TargetResolutionFailed ->
                parker.ui.OwnerHumanFidelityReviewRecordingOutcome.TargetResolutionFailed(targetResolutionReason(outcome.resolution))
            is parker.core.runtime.TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission ->
                parker.ui.OwnerHumanFidelityReviewRecordingOutcome.InvalidSubmission(outcome.reason)
            is parker.core.runtime.TierBHumanFidelityReviewRecordingOutcome.AuthorizationDenied ->
                parker.ui.OwnerHumanFidelityReviewRecordingOutcome.AuthorizationDenied(outcome.reason.name)
            is parker.core.runtime.TierBHumanFidelityReviewRecordingOutcome.Failure ->
                parker.ui.OwnerHumanFidelityReviewRecordingOutcome.Failed(outcome.reason.name)
        }
    }

    override suspend fun effectiveHumanFidelityReview(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): OwnerHumanFidelityStatus {
        val project = projectEffectiveHumanFidelityReviewAsOwner ?: return humanFidelityStatus(null)
        return when (val outcome = project(evidenceArtifactId, derivativeGenerationId)) {
            is parker.core.runtime.TierBEffectiveHumanFidelityReviewOutcome.Projected ->
                humanFidelityStatus(EffectiveHumanFidelityReviewProjectionOutcome.Projected(outcome.summary))
            is parker.core.runtime.TierBEffectiveHumanFidelityReviewOutcome.TargetResolutionFailed -> humanFidelityStatus(null)
            parker.core.runtime.TierBEffectiveHumanFidelityReviewOutcome.FailedClosed -> humanFidelityStatus(null)
        }
    }

    private fun targetResolutionReason(resolution: parker.core.runtime.TierBHumanFidelityReviewTargetResolution): String =
        when (resolution) {
            is parker.core.runtime.TierBHumanFidelityReviewTargetResolution.Resolved -> "RESOLVED"
            parker.core.runtime.TierBHumanFidelityReviewTargetResolution.UnknownGeneration -> "UNKNOWN_GENERATION"
            parker.core.runtime.TierBHumanFidelityReviewTargetResolution.SourceMismatch -> "SOURCE_MISMATCH"
            parker.core.runtime.TierBHumanFidelityReviewTargetResolution.WrongDerivativeKind -> "WRONG_DERIVATIVE_KIND"
            parker.core.runtime.TierBHumanFidelityReviewTargetResolution.ContentMissing -> "CONTENT_MISSING"
            parker.core.runtime.TierBHumanFidelityReviewTargetResolution.MissingProcessingProvenance -> "MISSING_PROCESSING_PROVENANCE"
            is parker.core.runtime.TierBHumanFidelityReviewTargetResolution.ContentCorrupt -> "CONTENT_CORRUPT: ${resolution.reason}"
        }

    /**
     * Owner Tier B Durable OCR Content Presentation. Projects the OCR
     * mechanism's own already-produced, already-admitted
     * [OcrDerivativeExtractedResult] into the safe, owner-facing
     * [OwnerTierBOcrContent] shape -- never a re-recognition.
     */
    private fun toOwnerOcrContent(extracted: OcrDerivativeExtractedResult, humanReviewStates: List<String> = emptyList()): OwnerTierBOcrContent = OwnerTierBOcrContent(
        recognisedText = extracted.recognisedText,
        fidelity = transcriptionFidelityLabel(extracted.fidelity),
        outcomeKind = extracted.outcomeKind.name,
        degradationReason = extracted.degradationReason,
        warnings = extracted.warnings,
        segments = extracted.segments.map { OwnerOcrSegmentSummary(it.text, it.fidelity.name, it.pageNumber) },
        producer = extracted.producerIdentity.toSummary(),
        completenessState = extracted.completenessState.name,
        sourceEvidenceArtifactId = extracted.processingProvenance?.sourceEvidenceArtifactId?.value,
        providerIdentity = extracted.providerProvenance?.providerIdentity,
        returnedModelIdentifier = extracted.providerProvenance?.providerReportedModelIdentifier,
        transcriptionProfileIdentity = extracted.providerProvenance?.transcriptionConfigurationProfile,
        humanReviewStates = humanReviewStates,
        modelSnapshot = when (val snapshot = extracted.providerProvenance?.modelSnapshot) {
            is OcrModelSnapshot.Present -> snapshot.value
            OcrModelSnapshot.NotExposed -> "Not separately exposed"
            null -> null
        },
        requestedPages = extracted.pageAccounting?.requestedScope?.pageNumbers,
        submittedPages = extracted.pageAccounting?.submittedScope?.pageNumbers,
        returnedPages = extracted.pageAccounting?.returnedScope?.pageNumbers,
        pageOutcomes = extracted.pageAccounting?.pageOutcomes?.map { page ->
            OwnerOcrPageOutcomeSummary(
                page.pageNumber, page.outcome.name, page.reason?.classification, page.warnings,
                reasonDetail = page.reason?.detail,
                uncertaintySpans = page.uncertaintySpans.map { span -> OwnerOcrUncertaintySpanSummary(span.kind.name, span.disclosure) },
            )
        }.orEmpty(),
        containsUncertaintyOrIllegibility = extracted.pageAccounting?.pageOutcomes?.any { page ->
            page.uncertaintySpans.isNotEmpty() || page.outcome.name.contains("ILLEGIBLE")
        } == true,
        externalTranscription = extracted.providerProvenance != null,
        adapterIdentity = extracted.providerProvenance?.adapterIdentity,
        adapterVersion = extracted.providerProvenance?.adapterVersion,
        recognisedAt = extracted.recognisedAt?.toString(),
    )

    override suspend fun analyseDocuments(
        selections: List<EvidenceGenerationSelection>,
        instruction: String,
    ): OwnerDocumentAnalysisInvocationOutcome {
        val invocation = analyseDocumentsAsOwner(OwnerDocumentAnalysisRequest(selections, instruction))
        val mapped = mapAnalysisOutcome(invocation.outcome)
        return OwnerDocumentAnalysisInvocationOutcome(mapped, invocation.pendingAnalysisId)
    }

    private fun mapAnalysisOutcome(outcome: DocumentAnalysisOutcome): OwnerDocumentAnalysisOutcome =
        when (outcome) {
            is DocumentAnalysisOutcome.Completed -> OwnerDocumentAnalysisOutcome.Completed(
                OwnerDocumentAnalysisPresentation(
                    analysisText = outcome.result.analysisText,
                    evidenceReferences = outcome.result.evidenceItems.map {
                        OwnerDocumentEvidenceReference(it.evidenceArtifactId, it.derivativeGenerationId, it.derivativeKind, it.assurance)
                    },
                    mechanismIdentity = outcome.result.mechanismIdentity,
                    mechanismVersion = outcome.result.mechanismVersion,
                    instruction = outcome.result.instruction,
                    warnings = outcome.result.warnings,
                ),
            )
            is DocumentAnalysisOutcome.NotAuthorised -> OwnerDocumentAnalysisOutcome.NotAuthorised(outcome.reason)
            is DocumentAnalysisOutcome.TooManySelections -> OwnerDocumentAnalysisOutcome.TooManySelections(outcome.requested, outcome.max)
            is DocumentAnalysisOutcome.InstructionTooLarge -> OwnerDocumentAnalysisOutcome.InstructionTooLarge(outcome.actualCharacters, outcome.max)
            is DocumentAnalysisOutcome.PromptTooLarge -> OwnerDocumentAnalysisOutcome.PromptTooLarge(outcome.actualCharacters, outcome.max)
            is DocumentAnalysisOutcome.UnknownGeneration -> OwnerDocumentAnalysisOutcome.UnknownGeneration(outcome.derivativeGenerationId)
            is DocumentAnalysisOutcome.SourceMismatch ->
                OwnerDocumentAnalysisOutcome.SourceMismatch(outcome.evidenceArtifactId, outcome.derivativeGenerationId)
            is DocumentAnalysisOutcome.ContentMissing -> OwnerDocumentAnalysisOutcome.ContentMissing(outcome.derivativeGenerationId)
            is DocumentAnalysisOutcome.ContentCorrupt ->
                OwnerDocumentAnalysisOutcome.ContentCorrupt(outcome.derivativeGenerationId, outcome.reason)
            is DocumentAnalysisOutcome.UnsupportedRepresentationVersion ->
                OwnerDocumentAnalysisOutcome.UnsupportedRepresentationVersion(outcome.derivativeGenerationId, outcome.version)
            is DocumentAnalysisOutcome.UnverifiedExternalAcknowledgementRequired ->
                OwnerDocumentAnalysisOutcome.UnverifiedExternalAcknowledgementRequired(
                    outcome.evidenceArtifactId,
                    outcome.derivativeGenerationId,
                )
            is DocumentAnalysisOutcome.UnsupportedDerivativeKind ->
                OwnerDocumentAnalysisOutcome.UnsupportedDerivativeKind(outcome.derivativeGenerationId, outcome.derivativeKind)
            is DocumentAnalysisOutcome.ContentTooLarge -> OwnerDocumentAnalysisOutcome.ContentTooLarge(outcome.actualCharacters, outcome.max)
            is DocumentAnalysisOutcome.ResponseTooLarge -> OwnerDocumentAnalysisOutcome.ResponseTooLarge(outcome.actualCharacters, outcome.max)
            is DocumentAnalysisOutcome.ModelInvocationFailed -> OwnerDocumentAnalysisOutcome.ModelInvocationFailed(outcome.safeMessage)
        }

    override suspend fun saveAnalysis(pendingAnalysisId: PendingAnalysisId): OwnerSaveAnalysisOutcome =
        when (val outcome = saveAnalysisAsOwner(pendingAnalysisId)) {
            is SaveAnalysisOutcome.Saved -> OwnerSaveAnalysisOutcome.Saved(outcome.savedAnalysisId)
            SaveAnalysisOutcome.UnknownOrExpiredPendingAnalysis -> OwnerSaveAnalysisOutcome.UnknownOrExpiredPendingAnalysis
            SaveAnalysisOutcome.SaveAlreadyInProgress -> OwnerSaveAnalysisOutcome.SaveAlreadyInProgress
            is SaveAnalysisOutcome.PersistenceFailed -> OwnerSaveAnalysisOutcome.Failed(outcome.safeMessage)
            is SaveAnalysisOutcome.SavedRecordTooLarge ->
                OwnerSaveAnalysisOutcome.Failed("The reviewed analysis exceeds the maximum accepted size (${outcome.field}).")
        }

    override suspend fun retrieveSavedAnalysis(savedAnalysisId: SavedAnalysisId): OwnerRetrieveSavedAnalysisOutcome =
        when (val outcome = retrieveSavedAnalysisAsOwner(savedAnalysisId)) {
            is RetrieveSavedAnalysisOutcome.Retrieved -> OwnerRetrieveSavedAnalysisOutcome.Retrieved(
                OwnerSavedAnalysisPresentation(
                    savedAnalysisId = outcome.record.savedAnalysisId,
                    savedAt = outcome.record.savedAt,
                    analysedAt = outcome.record.analysedAt,
                    instruction = outcome.record.instruction,
                    analysisText = outcome.record.analysisText,
                    evidenceReferences = outcome.record.evidenceReferences.map {
                        OwnerDocumentEvidenceReference(it.evidenceArtifactId, it.derivativeGenerationId, it.derivativeKind, it.assurance)
                    },
                    mechanismIdentity = outcome.record.mechanismIdentity,
                    mechanismVersion = outcome.record.mechanismVersion,
                ),
            )
            RetrieveSavedAnalysisOutcome.UnknownSavedAnalysis -> OwnerRetrieveSavedAnalysisOutcome.UnknownSavedAnalysis
            is RetrieveSavedAnalysisOutcome.CorruptRecord -> OwnerRetrieveSavedAnalysisOutcome.Failed("The saved analysis is corrupt.")
            is RetrieveSavedAnalysisOutcome.UnsupportedRepresentationVersion ->
                OwnerRetrieveSavedAnalysisOutcome.Failed("The saved analysis representation is not supported.")
        }

    override suspend fun listSavedAnalyses(): List<OwnerSavedAnalysisSummary> =
        listSavedAnalysesAsOwner().map { OwnerSavedAnalysisSummary(it.savedAnalysisId, it.savedAt, it.instructionPreview) }
}
