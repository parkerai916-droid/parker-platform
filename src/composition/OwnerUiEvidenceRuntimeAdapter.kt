package parker.composition

import parker.core.interfaces.DerivativeProducerIdentity
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierADocumentFormat
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome
import parker.ui.EvidenceImportOutcome
import parker.ui.OwnerDerivativeProducerSummary
import parker.ui.OwnerDocxTableSummary
import parker.ui.OwnerEmlAttachmentSummary
import parker.ui.OwnerEmlBodySummary
import parker.ui.OwnerEvidenceOperations
import parker.ui.OwnerPdfMetadataValue
import parker.ui.OwnerTierAContent
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
        is TierADocumentRoutingResult.Admitted ->
            TierAProcessingOutcome.Admitted(formatLabel(result.format), toOwnerContent(result.payload))
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
    private fun toOwnerContent(payload: TierADerivativePayload): OwnerTierAContent = when (payload) {
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
