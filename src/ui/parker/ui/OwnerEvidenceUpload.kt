package parker.ui

import java.time.Instant
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceGenerationSelection
import parker.core.interfaces.PendingAnalysisId
import parker.core.interfaces.SavedAnalysisId
import parker.core.interfaces.AnalysisAcquisitionAssurance

/**
 * Owner Evidence Upload & Processing (first version). The owner UI's
 * complete evidence capability boundary -- mirroring [OwnerInteraction]'s
 * own "not a generic command or runtime facade" discipline exactly: exactly
 * three narrow operations, each a thin, presentation-safe wrapper around one
 * already-governed `ParkerRuntime` entry point
 * (`importEvidenceFileAsOwner`/`invokeTierAIngestionAsOwner`/`analyseEvidence`),
 * never the runtime itself. Each file the owner selects is an independent
 * operation from the caller's own perspective -- this interface has no
 * "many files" method of any kind; a UI-level multi-select is exactly a
 * convenience loop calling [importFile] once per file, never a new evidence
 * authority (Document Ingestion Programme Implementation Closure §18: no
 * bulk evidence authority is created by this unit).
 */
interface OwnerEvidenceOperations {
    /** Read-only governed acquisition decision for one exact custodied source; never executes acquisition. */
    suspend fun governedAcquisitionDecision(evidenceArtifactId: EvidenceArtifactId): OwnerAcquisitionDecisionView =
        OwnerAcquisitionDecisionView.Indeterminate(
            OwnerAcquisitionSourceFacts(evidenceArtifactId.value, null, null, null, "UNKNOWN", "UNKNOWN", null, "UNKNOWN", "UNKNOWN", "UNKNOWN"),
            listOf("Required technical source characteristics are not established."),
        )

    /** Explicit owner action; the expected capability is revalidated server-side before exact execution. */
    suspend fun executeGovernedAcquisition(
        evidenceArtifactId: EvidenceArtifactId,
        expectedCapabilityId: String,
    ): OwnerAcquisitionExecutionView = OwnerAcquisitionExecutionView.Failed("SELECTED_CAPABILITY_UNAVAILABLE")

    /** Read-only executable readiness projection; never invokes a provider. */
    fun enhancedTranscriptionReadiness(): EnhancedTranscriptionReadiness = EnhancedTranscriptionReadiness.Disabled

    /** One explicit owner action for one route-bound evidence identity. */
    suspend fun transcribeExternal(evidenceArtifactId: EvidenceArtifactId): EnhancedTranscriptionOutcome =
        EnhancedTranscriptionOutcome.NotReady(EnhancedTranscriptionReadiness.Disabled)

    /**
     * Imports one already-local file (an absolute path the owner's own
     * client resolved -- a native file-picker dialog result, never a string
     * this interface's own caller constructs from untrusted input) into
     * Evidence Custody. [declaredMediaType] is a declared, unauthoritative
     * value only (mirroring a browser's own Content-Type header's identical
     * epistemic status) -- never Parker's own later, authoritative Tier A
     * detection, which this call does not perform or wait for.
     */
    suspend fun importFile(absolutePath: String, declaredMediaType: String?): EvidenceImportOutcome

    /** Explicit, owner-triggered Tier A routing for one already-custodied artefact. */
    suspend fun processTierA(evidenceArtifactId: EvidenceArtifactId): TierAProcessingOutcome

    /**
     * Explicit, owner-triggered Tier B/OCR invocation for one already-custodied
     * artefact -- never invoked automatically, and never invoked by this
     * interface's own [processTierA]. Structurally owner-only from the UI's
     * own vantage point: this interface accepts no `requestingPrincipalId`
     * parameter of any kind, closing -- for UI-originated calls specifically,
     * without touching `ParkerRuntime.analyseEvidence` itself -- the
     * caller-supplied-principal gap OCR Mechanism Unit 12 Runtime Invocation
     * Scope Lock §6 discloses and explicitly leaves open for "a future...
     * helper" to close.
     */
    suspend fun processTierB(evidenceArtifactId: EvidenceArtifactId): TierBProcessingOutcome

    /**
     * Document Ingestion — Derivative Content Persistence and Retrieval
     * (`DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`).
     * Retrieves an already-persisted Tier A derivative's durable content by
     * known identity -- never re-runs extraction. [derivativeGenerationId]
     * must be one the caller already possesses (from a prior
     * [processTierA] response); this interface offers no enumeration of
     * generations for an [evidenceArtifactId], mirroring the scope lock's
     * own deliberately narrow retrieval boundary. Structurally owner-only,
     * exactly like [processTierA]/[processTierB]: no `requestingPrincipalId`
     * parameter of any kind.
     */
    suspend fun retrieveTierAExtractedContent(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): TierAContentRetrievalResult

    /**
     * Document Ingestion — Tier B Durable OCR Derivative Content
     * (`DOCUMENT_INGESTION_TIER_B_DURABLE_OCR_DERIVATIVE_CONTENT_SCOPE_LOCK.md`).
     * Explicit, owner-triggered durable Tier B OCR for one already-custodied
     * artefact -- distinct from [processTierB] (the existing, unchanged
     * transient-only Evidence Intelligence path): on success, this durably
     * admits a new `DerivativeGenerationRecord` and subordinate content
     * entry, retrievable after restart without rerunning OCR. Structurally
     * owner-only, exactly like every other operation on this interface: no
     * `requestingPrincipalId` parameter of any kind.
     */
    suspend fun processTierBDurable(evidenceArtifactId: EvidenceArtifactId): TierBDurableProcessingOutcome

    /**
     * Retrieves an already-persisted Tier B durable OCR generation's
     * content by known identity -- never re-runs OCR. [derivativeGenerationId]
     * must be one the caller already possesses (from a prior
     * [processTierBDurable] response). Structurally owner-only.
     */
    suspend fun retrieveTierBOcrContent(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): TierBOcrContentRetrievalResult

    /**
     * Minimum Production Document Pipeline — Local Reasoning Implementation.
     * Submits one or more already-admitted evidence derivative generations
     * ([selections] -- identities the caller already possesses from a prior
     * [processTierA]/[processTierBDurable] response, never a filesystem path
     * or enumeration) plus the owner's own [instruction] to Parker's
     * currently configured LOCAL model-inference seam for a
     * human-reviewable analysis. Structurally owner-only, exactly like
     * every other operation on this interface: no `requestingPrincipalId`
     * parameter of any kind. Never persists the returned analysis on its
     * own, never writes to Memory/Knowledge/QMD/RKS, never re-runs
     * extraction or OCR. On [OwnerDocumentAnalysisOutcome.Completed], the
     * returned [OwnerDocumentAnalysisInvocationOutcome.pendingAnalysisId]
     * is the only identity a later, separate, explicit [saveAnalysis] call
     * may use to durably preserve exactly this result -- this interface
     * never accepts resubmitted analysis text as something to save.
     */
    suspend fun analyseDocuments(selections: List<EvidenceGenerationSelection>, instruction: String): OwnerDocumentAnalysisInvocationOutcome

    /**
     * Reviewed Analysis Result — Explicit Owner Save. Durably preserves the
     * exact, already-completed analysis registered under [pendingAnalysisId]
     * by a prior [analyseDocuments] call -- never accepts, and never
     * trusts, caller-resubmitted analysis text. Structurally owner-only.
     * Never automatic: reachable only via this explicit, separate call, and
     * only after the owner has already seen the transient result.
     */
    suspend fun saveAnalysis(pendingAnalysisId: PendingAnalysisId): OwnerSaveAnalysisOutcome

    /**
     * Reviewed Analysis Result — Explicit Owner Save. Retrieves an
     * already-saved analysis by known [savedAnalysisId] -- never re-runs
     * analysis, never invokes the model, never re-runs OCR/extraction.
     * Structurally owner-only.
     */
    suspend fun retrieveSavedAnalysis(savedAnalysisId: SavedAnalysisId): OwnerRetrieveSavedAnalysisOutcome

    /**
     * Reviewed Analysis Result — Explicit Owner Save. The bounded, most
     * recently saved analyses, newest first -- metadata only (see
     * [OwnerSavedAnalysisSummary]), never full analysis text or evidence
     * references. No search, filter, or pagination beyond this fixed cap.
     * Structurally owner-only.
     */
    suspend fun listSavedAnalyses(): List<OwnerSavedAnalysisSummary>
}

/** The truthful result of one [OwnerEvidenceOperations.importFile] call. */
sealed interface EvidenceImportOutcome {
    data class Imported(val evidenceArtifactId: EvidenceArtifactId) : EvidenceImportOutcome
    data class Rejected(val reason: String) : EvidenceImportOutcome
    data class Failed(val safeMessage: String) : EvidenceImportOutcome
}

/**
 * The truthful result of one [OwnerEvidenceOperations.processTierA] call.
 * [format] is always one of Document Ingestion's own four governed format
 * labels ("CSV"/"EML"/"DOCX"/"PDF") for [Admitted] -- never a fabricated or
 * inferred fifth value. [content] is the safe owner-facing projection of
 * the already-produced admitted Tier A payload -- never a re-extraction.
 * `null` only when a caller genuinely has no content to report (a fake
 * test double, for instance); the real production adapter
 * ([parker.composition.OwnerUiEvidenceRuntimeAdapter]) always supplies it.
 * [derivativeGenerationId] is the durable identity the caller must retain to
 * later call [OwnerEvidenceOperations.retrieveTierAExtractedContent] without
 * re-processing -- `null` only for the same fake-caller reason as [content].
 */
sealed interface TierAProcessingOutcome {
    data class Admitted(
        val format: String,
        val content: OwnerTierAContent? = null,
        val derivativeGenerationId: DerivativeGenerationId? = null,
    ) : TierAProcessingOutcome
    data object RequiresTierB : TierAProcessingOutcome
    data class Unsupported(val reason: String) : TierAProcessingOutcome
    data class IntegrityFailure(val reason: String) : TierAProcessingOutcome
    data class Failed(val stage: String, val safeMessage: String) : TierAProcessingOutcome
}

/**
 * Owner Tier A Extracted Content Presentation. The smallest safe,
 * owner-facing projection of an already-produced admitted Tier A payload
 * (`TierADerivativePayload`) -- built once, from the specialist's own
 * already-computed result, never by re-running extraction. No field here
 * ever carries a server filesystem path, a temp path, a stack trace, or a
 * secret/config value; every text field is exactly what the specialist
 * itself claims, never invented or embellished.
 *
 * PDF is presented at full fidelity ([Pdf.documentText] is the specialist's
 * own [parker.core.interfaces.PdfStructuralResult.documentText] verbatim,
 * already bounded by [parker.core.runtime.TikaEvidenceExtractor]'s own
 * governed 8 Mi-character extraction cap -- no further truncation is
 * applied here). CSV/EML/DOCX are truthful, bounded summaries rather than
 * an exhaustive dump of every structural field -- [Csv] discloses exactly
 * how many rows exist and how many are actually shown
 * ([Csv.rowsTruncatedForDisplay]), never silently truncating and then
 * calling the result complete.
 */
sealed interface OwnerTierAContent {
    data class Pdf(
        val documentText: String,
        val pageCount: Int?,
        val pageTextAssociationAvailable: Boolean,
        val producer: OwnerDerivativeProducerSummary,
        val transformationHistory: List<String>,
        val completenessState: String,
        val warnings: List<String>,
        val metadata: List<OwnerPdfMetadataValue>,
    ) : OwnerTierAContent

    data class Csv(
        val headers: List<String>,
        val previewRows: List<List<String>>,
        val totalRowCount: Int,
        val rowsTruncatedForDisplay: Boolean,
        val producer: OwnerDerivativeProducerSummary,
        val completenessState: String,
        val warnings: List<String>,
    ) : OwnerTierAContent

    data class Eml(
        val from: String?,
        val to: String?,
        val cc: String?,
        val subject: String?,
        val rawDate: String?,
        val messageId: String?,
        val bodyAlternatives: List<OwnerEmlBodySummary>,
        val attachmentCandidateCount: Int,
        val attachmentCandidates: List<OwnerEmlAttachmentSummary>,
        val producer: OwnerDerivativeProducerSummary,
        val completenessState: String,
        val warnings: List<String>,
    ) : OwnerTierAContent

    data class Docx(
        val paragraphs: List<String>,
        val tables: List<OwnerDocxTableSummary>,
        val headers: List<String>,
        val footers: List<String>,
        val producer: OwnerDerivativeProducerSummary,
        val completenessState: String,
        val warnings: List<String>,
    ) : OwnerTierAContent

    companion object {
        /** Owner-facing CSV row display bound only -- never applied to extraction; [Csv.totalRowCount] always discloses the real count. */
        const val CSV_PREVIEW_ROW_LIMIT: Int = 500
    }
}

/** Producer/model identity fields only -- no configuration values, paths, or secrets. */
data class OwnerDerivativeProducerSummary(
    val pluginIdentity: String,
    val pluginVersion: String,
    val configurationIdentity: String,
    val adapterIdentity: String?,
    val adapterVersion: String?,
    val modelIdentity: String?,
    val modelVersion: String?,
)

data class OwnerPdfMetadataValue(val name: String, val value: String)

/** [text] is the specialist's own decoded body text for one MIME alternative -- never attachment binary content. */
data class OwnerEmlBodySummary(val mediaType: String, val charset: String?, val text: String)

/** Metadata only -- never [parker.core.interfaces.EmlAttachmentCandidate.decodedBytes]. */
data class OwnerEmlAttachmentSummary(val filename: String?, val declaredMimeType: String, val byteLength: Long)

data class OwnerDocxTableSummary(val rows: List<List<String>>)

/**
 * The truthful result of one [OwnerEvidenceOperations.processTierB] call.
 * [resultCount] is the exact number of analytical results Evidence
 * Intelligence produced (0 is a genuine, honest outcome -- a timeout,
 * missing model, unsupported/corrupt input, or no-recognisable-content
 * disclosure -- never collapsed into [Failed]).
 */
sealed interface TierBProcessingOutcome {
    data class Completed(val resultCount: Int) : TierBProcessingOutcome
    data class NotAuthorised(val reason: String) : TierBProcessingOutcome
    data class Failed(val safeMessage: String) : TierBProcessingOutcome
}

/**
 * The truthful result of one [OwnerEvidenceOperations.retrieveTierAExtractedContent]
 * call. Every failure variant is distinct and non-fabricated -- mirroring
 * [parker.core.interfaces.TierAContentRetrievalOutcome]'s own identical
 * distinctions at the runtime layer, never collapsed into a single generic
 * "not found."
 */
sealed interface TierAContentRetrievalResult {
    data class Retrieved(val content: OwnerTierAContent) : TierAContentRetrievalResult
    data object UnknownGeneration : TierAContentRetrievalResult
    data object SourceMismatch : TierAContentRetrievalResult
    data object ContentMissing : TierAContentRetrievalResult
    data class ContentCorrupt(val safeMessage: String) : TierAContentRetrievalResult
    data class UnsupportedRepresentationVersion(val version: Int) : TierAContentRetrievalResult
    data class Failed(val safeMessage: String) : TierAContentRetrievalResult
}

/**
 * Owner Tier B Durable OCR Content Presentation. The smallest safe,
 * owner-facing projection of an already-produced, already-admitted
 * durable Tier B OCR generation -- built once, from the OCR mechanism's
 * own already-computed result, never by rerunning OCR. No field here ever
 * carries a server filesystem path, a model path, a temp path, a stack
 * trace, or a secret/config value.
 */
data class OwnerTierBOcrContent(
    val recognisedText: String,
    val fidelity: String,
    val outcomeKind: String,
    val degradationReason: String?,
    val warnings: List<String>,
    val segments: List<OwnerOcrSegmentSummary>,
    val producer: OwnerDerivativeProducerSummary,
    val completenessState: String,
    val sourceEvidenceArtifactId: String? = null,
    val providerIdentity: String? = null,
    val returnedModelIdentifier: String? = null,
    val transcriptionProfileIdentity: String? = null,
    val humanReviewStates: List<String> = emptyList(),
    val modelSnapshot: String? = null,
    val requestedPages: List<Int>? = null,
    val submittedPages: List<Int>? = null,
    val returnedPages: List<Int>? = null,
    val pageOutcomes: List<OwnerOcrPageOutcomeSummary> = emptyList(),
    val containsUncertaintyOrIllegibility: Boolean = false,
    val externalTranscription: Boolean = false,
)

data class OwnerOcrSegmentSummary(val text: String, val fidelity: String, val pageNumber: Int?)
data class OwnerOcrPageOutcomeSummary(val pageNumber: Int, val outcome: String, val reason: String?, val warnings: List<String>)

sealed interface EnhancedTranscriptionReadiness {
    data object Disabled : EnhancedTranscriptionReadiness
    data class ProfileNotReady(val safeReason: String) : EnhancedTranscriptionReadiness
    data object MissingCredential : EnhancedTranscriptionReadiness
    data object Ready : EnhancedTranscriptionReadiness
}

sealed interface EnhancedTranscriptionOutcome {
    data class Admitted(val content: OwnerTierBOcrContent, val derivativeGenerationId: DerivativeGenerationId) : EnhancedTranscriptionOutcome
    data class NotReady(val readiness: EnhancedTranscriptionReadiness) : EnhancedTranscriptionOutcome
    data class Failed(val safeMessage: String) : EnhancedTranscriptionOutcome
    data class ReconciliationRequired(val content: OwnerTierBOcrContent, val derivativeGenerationId: DerivativeGenerationId, val safeMessage: String) : EnhancedTranscriptionOutcome
}

/**
 * The truthful result of one [OwnerEvidenceOperations.processTierBDurable]
 * call. Distinct, non-fabricated outcomes mirroring
 * [parker.core.interfaces.TierBOcrOwnerInvocationOutcome]'s own
 * distinctions -- never collapsed into a single generic failure.
 */
sealed interface TierBDurableProcessingOutcome {
    data class Admitted(val content: OwnerTierBOcrContent, val derivativeGenerationId: DerivativeGenerationId) : TierBDurableProcessingOutcome
    data class NotAuthorised(val reason: String) : TierBDurableProcessingOutcome
    data class MandatoryProvenanceUnavailable(val reason: String) : TierBDurableProcessingOutcome
    data class OcrNotAdmissible(val reason: String) : TierBDurableProcessingOutcome
    data class IntegrityFailure(val reason: String) : TierBDurableProcessingOutcome
    data class Failed(val stage: String, val safeMessage: String) : TierBDurableProcessingOutcome
}

/**
 * The truthful result of one [OwnerEvidenceOperations.retrieveTierBOcrContent]
 * call. Mirrors [TierAContentRetrievalResult]'s own identical distinctions,
 * plus [WrongDerivativeKind] for the Tier A/Tier B kind-discrimination
 * requirement.
 */
sealed interface TierBOcrContentRetrievalResult {
    data class Retrieved(val content: OwnerTierBOcrContent) : TierBOcrContentRetrievalResult
    data object UnknownGeneration : TierBOcrContentRetrievalResult
    data object SourceMismatch : TierBOcrContentRetrievalResult
    data object WrongDerivativeKind : TierBOcrContentRetrievalResult
    data object ContentMissing : TierBOcrContentRetrievalResult
    data class ContentCorrupt(val safeMessage: String) : TierBOcrContentRetrievalResult
    data class UnsupportedRepresentationVersion(val version: Int) : TierBOcrContentRetrievalResult
    data class Failed(val safeMessage: String) : TierBOcrContentRetrievalResult
}

/**
 * One evidence derivative generation actually submitted for one
 * [OwnerEvidenceOperations.analyseDocuments] call, presented back to the
 * owner alongside the analysis so it is clear exactly what material was
 * supplied. No content, no producer/model detail beyond a bare kind label
 * -- the owner already saw the full content via [OwnerTierAContent]/
 * [OwnerTierBOcrContent] when they selected it.
 */
data class OwnerDocumentEvidenceReference(
    val evidenceArtifactId: EvidenceArtifactId,
    val derivativeGenerationId: DerivativeGenerationId,
    val derivativeKind: String,
    val assurance: AnalysisAcquisitionAssurance? = null,
)

/**
 * The truthful, owner-facing presentation of one completed
 * [OwnerEvidenceOperations.analyseDocuments] invocation. [analysisText] is
 * the local model's own raw response -- provider-generated material for
 * human review, never Evidence, Memory, Knowledge, or canonical truth.
 * [mechanismIdentity]/[mechanismVersion] are `null` when Parker cannot
 * truthfully determine them -- never fabricated.
 */
data class OwnerDocumentAnalysisPresentation(
    val analysisText: String,
    val evidenceReferences: List<OwnerDocumentEvidenceReference>,
    val mechanismIdentity: String?,
    val mechanismVersion: String?,
    val instruction: String,
    val warnings: List<String>,
)

/**
 * The truthful result of one [OwnerEvidenceOperations.analyseDocuments]
 * call. Mirrors [parker.core.interfaces.DocumentAnalysisOutcome]'s own
 * distinctions exactly -- every failure distinct and honest, never
 * collapsed into a single generic failure.
 */
sealed interface OwnerDocumentAnalysisOutcome {
    data class Completed(val result: OwnerDocumentAnalysisPresentation) : OwnerDocumentAnalysisOutcome
    data class NotAuthorised(val reason: String) : OwnerDocumentAnalysisOutcome
    data class TooManySelections(val requested: Int, val max: Int) : OwnerDocumentAnalysisOutcome
    data class InstructionTooLarge(val actualCharacters: Int, val max: Int) : OwnerDocumentAnalysisOutcome
    data class PromptTooLarge(val actualCharacters: Int, val max: Int) : OwnerDocumentAnalysisOutcome
    data class UnknownGeneration(val derivativeGenerationId: DerivativeGenerationId) : OwnerDocumentAnalysisOutcome
    data class SourceMismatch(val evidenceArtifactId: EvidenceArtifactId, val derivativeGenerationId: DerivativeGenerationId) : OwnerDocumentAnalysisOutcome
    data class ContentMissing(val derivativeGenerationId: DerivativeGenerationId) : OwnerDocumentAnalysisOutcome
    data class ContentCorrupt(val derivativeGenerationId: DerivativeGenerationId, val safeMessage: String) : OwnerDocumentAnalysisOutcome
    data class UnsupportedRepresentationVersion(val derivativeGenerationId: DerivativeGenerationId, val version: Int) : OwnerDocumentAnalysisOutcome
    data class UnverifiedExternalAcknowledgementRequired(
        val evidenceArtifactId: EvidenceArtifactId,
        val derivativeGenerationId: DerivativeGenerationId,
    ) : OwnerDocumentAnalysisOutcome
    data class UnsupportedDerivativeKind(val derivativeGenerationId: DerivativeGenerationId, val derivativeKind: String) : OwnerDocumentAnalysisOutcome
    data class ContentTooLarge(val actualCharacters: Int, val max: Int) : OwnerDocumentAnalysisOutcome
    data class ResponseTooLarge(val actualCharacters: Int, val max: Int) : OwnerDocumentAnalysisOutcome
    data class ModelInvocationFailed(val safeMessage: String) : OwnerDocumentAnalysisOutcome
}

/**
 * The truthful result of one [OwnerEvidenceOperations.analyseDocuments]
 * invocation, wrapping the existing [OwnerDocumentAnalysisOutcome]
 * unchanged with the opaque [pendingAnalysisId] a `Completed` result was
 * registered under -- present if and only if [outcome] is `Completed`.
 * Deliberately not a field of [OwnerDocumentAnalysisPresentation] itself,
 * the same reasoning [parker.core.interfaces.DocumentAnalysisInvocationResult]'s
 * own KDoc gives: a housekeeping token, not a truthful fact about the
 * analysis.
 */
data class OwnerDocumentAnalysisInvocationOutcome(
    val outcome: OwnerDocumentAnalysisOutcome,
    val pendingAnalysisId: PendingAnalysisId?,
)

/**
 * The truthful result of one [OwnerEvidenceOperations.saveAnalysis] call.
 * Mirrors [parker.core.interfaces.SaveAnalysisOutcome]'s own distinctions
 * -- every failure distinct and honest, never collapsed into a single
 * generic failure.
 */
sealed interface OwnerSaveAnalysisOutcome {
    data class Saved(val savedAnalysisId: SavedAnalysisId) : OwnerSaveAnalysisOutcome
    data object UnknownOrExpiredPendingAnalysis : OwnerSaveAnalysisOutcome
    data object SaveAlreadyInProgress : OwnerSaveAnalysisOutcome
    data class Failed(val safeMessage: String) : OwnerSaveAnalysisOutcome
}

/**
 * The truthful, owner-facing presentation of one saved analysis --
 * everything the browser needs to display it, reusing
 * [OwnerDocumentEvidenceReference] directly for evidence references
 * (identical shape, never a parallel provenance concept).
 */
data class OwnerSavedAnalysisPresentation(
    val savedAnalysisId: SavedAnalysisId,
    val savedAt: Instant,
    val analysedAt: Instant,
    val instruction: String,
    val analysisText: String,
    val evidenceReferences: List<OwnerDocumentEvidenceReference>,
    val mechanismIdentity: String?,
    val mechanismVersion: String?,
)

/** The truthful result of one [OwnerEvidenceOperations.retrieveSavedAnalysis] call. */
sealed interface OwnerRetrieveSavedAnalysisOutcome {
    data class Retrieved(val presentation: OwnerSavedAnalysisPresentation) : OwnerRetrieveSavedAnalysisOutcome
    data object UnknownSavedAnalysis : OwnerRetrieveSavedAnalysisOutcome
    data class Failed(val safeMessage: String) : OwnerRetrieveSavedAnalysisOutcome
}

/**
 * The smallest metadata-only projection of a saved analysis needed to
 * choose one from a bounded listing -- never the full analysis text, never
 * full evidence references. [instructionPreview] is a bounded,
 * presentation-only excerpt; the full, stored instruction is never
 * truncated -- only visible via [OwnerRetrieveSavedAnalysisOutcome].
 */
data class OwnerSavedAnalysisSummary(
    val savedAnalysisId: SavedAnalysisId,
    val savedAt: Instant,
    val instructionPreview: String,
)
