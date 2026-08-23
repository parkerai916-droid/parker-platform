package parker.composition

import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierADocumentFormat
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome
import parker.ui.EvidenceImportOutcome
import parker.ui.OwnerEvidenceOperations
import parker.ui.TierAProcessingOutcome
import parker.ui.TierBProcessingOutcome

/** OCR Mechanism Unit 12's own, unmodified, already-governed analysisKind convention. */
private const val OCR_ANALYSIS_KIND = "ocr-transcription"

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
    private val importEvidenceFileAsOwner: suspend (String, String?) -> OwnerLocalFileIngressOutcome,
    private val invokeTierAIngestionAsOwner: suspend (EvidenceArtifactId) -> TierAOwnerInvocationOutcome,
    private val analyseEvidence: suspend (PrincipalId, EvidenceAnalysisRequest) -> EvidenceIntelligenceInvocationOutcome,
) : OwnerEvidenceOperations {

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
        is TierADocumentRoutingResult.Admitted -> TierAProcessingOutcome.Admitted(formatLabel(result.format))
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
}
