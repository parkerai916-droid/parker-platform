package parker.ui

import parker.core.interfaces.EvidenceArtifactId

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
 */
sealed interface TierAProcessingOutcome {
    data class Admitted(val format: String, val content: OwnerTierAContent? = null) : TierAProcessingOutcome
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
