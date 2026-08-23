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
 * inferred fifth value.
 */
sealed interface TierAProcessingOutcome {
    data class Admitted(val format: String) : TierAProcessingOutcome
    data object RequiresTierB : TierAProcessingOutcome
    data class Unsupported(val reason: String) : TierAProcessingOutcome
    data class IntegrityFailure(val reason: String) : TierAProcessingOutcome
    data class Failed(val stage: String, val safeMessage: String) : TierAProcessingOutcome
}

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
