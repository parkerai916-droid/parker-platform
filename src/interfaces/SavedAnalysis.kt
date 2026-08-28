package parker.core.interfaces

import java.time.Instant

/**
 * Reviewed Analysis Result — Explicit Owner Save. The owner reviews a
 * completed [DocumentAnalysisOutcome.Completed] and deliberately chooses to
 * durably preserve it. A saved analysis is NEVER automatically Evidence, a
 * [DerivativeGenerationRecord], Derivative Content, Memory, Knowledge,
 * QMD/RKS material, or canonical Parker truth -- it proves only that the
 * owner explicitly chose to keep one specific, already-produced,
 * provider-generated analysis for later human reference. It does not prove
 * the analysis is factually correct, that the owner agrees with every
 * proposition in it, or that it has been promoted anywhere. This authority
 * meaning is preserved by this type's own scope and documentation -- the
 * same discipline [OwnerDocumentAnalysisResult] already relies on without a
 * runtime "non-canonical" flag -- never by a mutable status field a future
 * change could silently flip.
 */

/** Opaque identity of one completed analysis awaiting an explicit owner Save decision -- never durable on its own. */
@JvmInline
value class PendingAnalysisId(val value: String) {
    init {
        require(value.isNotBlank()) { "PendingAnalysisId must not be blank" }
    }
}

/** Opaque identity of one durably saved analysis record. */
@JvmInline
value class SavedAnalysisId(val value: String) {
    init {
        require(value.isNotBlank()) { "SavedAnalysisId must not be blank" }
    }
}

/**
 * One evidence-generation reference carried by a saved analysis --
 * deliberately the same three facts [parker.ui.OwnerDocumentEvidenceReference]
 * already discloses to the owner, never a new provenance concept.
 */
data class SavedAnalysisEvidenceReference(
    val evidenceArtifactId: EvidenceArtifactId,
    val derivativeGenerationId: DerivativeGenerationId,
    val derivativeKind: String,
    /** Exact server-projected assurance snapshot used for this analysis; absent for version-1 history. */
    val assurance: AnalysisAcquisitionAssurance? = null,
)

/**
 * The durable representation of one explicitly owner-saved analysis.
 * [analysisText]/[instruction]/[evidenceReferences]/[mechanismIdentity]/
 * [mechanismVersion]/[analysedAt] are copied verbatim from the exact
 * [OwnerDocumentAnalysisResult] the owner reviewed -- never re-derived,
 * never re-generated. [mechanismIdentity]/[mechanismVersion] preserve the
 * same truthful nullability the transient result already carries: `null`
 * when genuinely not knowable, never fabricated as `"unknown"`.
 */
data class SavedAnalysisRecord(
    val savedAnalysisId: SavedAnalysisId,
    val savedAt: Instant,
    val analysedAt: Instant,
    val instruction: String,
    val analysisText: String,
    val evidenceReferences: List<SavedAnalysisEvidenceReference>,
    val mechanismIdentity: String?,
    val mechanismVersion: String?,
) {
    init {
        require(instruction.isNotBlank()) { "SavedAnalysisRecord.instruction must not be blank" }
        require(analysisText.isNotBlank()) { "SavedAnalysisRecord.analysisText must not be blank" }
        require(evidenceReferences.isNotEmpty()) { "SavedAnalysisRecord.evidenceReferences must not be empty" }
        require((mechanismIdentity == null) == (mechanismVersion == null)) {
            "SavedAnalysisRecord mechanism identity and version must either both be present or both be absent"
        }
    }
}

/**
 * The truthful result of one [parker.core.runtime.DocumentAnalysisCoordinator.analyse]
 * invocation as returned to the owner-facing layer -- wraps the existing,
 * unmodified [DocumentAnalysisOutcome] with the opaque [PendingAnalysisId]
 * a [DocumentAnalysisOutcome.Completed] result was registered under (see
 * `parker.core.runtime.PendingAnalysisCache`), so a later, separate,
 * explicit Save can bind to exactly that server-held result without the
 * caller ever resubmitting its content. Deliberately not a field of
 * [OwnerDocumentAnalysisResult] itself -- the pending id is a
 * request/response-cycle housekeeping token, not a truthful fact about the
 * analysis. `null` for every outcome other than `Completed`, since there is
 * no completed result to later save.
 */
data class DocumentAnalysisInvocationResult(
    val outcome: DocumentAnalysisOutcome,
    val pendingAnalysisId: PendingAnalysisId?,
) {
    init {
        require((outcome is DocumentAnalysisOutcome.Completed) == (pendingAnalysisId != null)) {
            "DocumentAnalysisInvocationResult.pendingAnalysisId must be present exactly when outcome is Completed"
        }
    }
}

/** The truthful result of one explicit owner Save attempt. Every failure distinct and honest, never collapsed into a single generic failure. */
sealed class SaveAnalysisOutcome {
    data class Saved(val savedAnalysisId: SavedAnalysisId) : SaveAnalysisOutcome()

    /** [PendingAnalysisId] is unknown, expired, or already consumed by an earlier successful Save -- fails closed, never silently re-saves or fabricates a second record. */
    data object UnknownOrExpiredPendingAnalysis : SaveAnalysisOutcome()

    /** [PendingAnalysisId] is currently being saved by a concurrent, still-in-flight Save request -- fails closed rather than racing to create two saved records for one reviewed result. */
    data object SaveAlreadyInProgress : SaveAnalysisOutcome()

    /** Durable publication failed (storage fault). The pending analysis is NOT consumed -- a retry with the same [PendingAnalysisId] remains possible. */
    data class PersistenceFailed(val safeMessage: String) : SaveAnalysisOutcome()

    /** The reviewed result's own instruction/analysis-text/evidence-reference-count exceeded the frozen bound reused from [parker.core.runtime.DocumentAnalysisCoordinator] -- defensive-in-depth (unreachable via the normal analyse-then-save flow, since those bounds are already enforced before a `Completed` outcome can exist). The pending analysis is NOT consumed. */
    data class SavedRecordTooLarge(val field: String, val actualCharacters: Int, val max: Int) : SaveAnalysisOutcome()
}

/** The truthful result of one retrieve-by-known-[SavedAnalysisId] attempt. Never re-runs analysis, never invokes the model. */
sealed class RetrieveSavedAnalysisOutcome {
    data class Retrieved(val record: SavedAnalysisRecord) : RetrieveSavedAnalysisOutcome()
    data object UnknownSavedAnalysis : RetrieveSavedAnalysisOutcome()
    data class CorruptRecord(val reason: String) : RetrieveSavedAnalysisOutcome()
    data class UnsupportedRepresentationVersion(val version: Int) : RetrieveSavedAnalysisOutcome()
}

/**
 * The smallest metadata-only projection of a [SavedAnalysisRecord] needed
 * to choose one from a bounded listing -- never the full analysis text,
 * never full evidence references. [instructionPreview] is a bounded,
 * presentation-only excerpt (see
 * `parker.core.runtime.SavedAnalysisCoordinator.INSTRUCTION_PREVIEW_MAX_CHARACTERS`)
 * -- the stored [SavedAnalysisRecord.instruction] itself is never truncated.
 */
data class SavedAnalysisSummary(
    val savedAnalysisId: SavedAnalysisId,
    val savedAt: Instant,
    val instructionPreview: String,
)

sealed class SavedAnalysisStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class DuplicateIdentifier(val savedAnalysisId: SavedAnalysisId) :
        SavedAnalysisStorageException("Saved analysis '${savedAnalysisId.value}' already exists")
    class UnsafeIdentifier(val savedAnalysisId: SavedAnalysisId) :
        SavedAnalysisStorageException("Saved analysis identifier '${savedAnalysisId.value}' is unsafe for storage")
    class InvalidStorageRoot(path: String, reason: String) :
        SavedAnalysisStorageException("Saved analysis storage root '$path' is invalid: $reason")
    class PersistenceFailure(message: String, cause: Throwable) : SavedAnalysisStorageException(message, cause)
    class CorruptRecord(val savedAnalysisId: SavedAnalysisId, message: String, cause: Throwable? = null) :
        SavedAnalysisStorageException("Saved analysis '${savedAnalysisId.value}' is corrupt: $message", cause)
    class UnsupportedRepresentationVersion(val savedAnalysisId: SavedAnalysisId, val version: Int) :
        SavedAnalysisStorageException("Saved analysis '${savedAnalysisId.value}' representation version $version is not supported")
}

/**
 * Write-once, prepare/publish durable storage for [SavedAnalysisRecord],
 * mirroring [DerivativeGenerationStorage]'s own established shape exactly
 * -- a wholly separate store from Evidence/Derivative Generation/Derivative
 * Content/Memory/Knowledge, never nested inside or sharing an identifier
 * namespace with any of them.
 */
interface SavedAnalysisStorage {
    suspend fun prepare(record: SavedAnalysisRecord)
    suspend fun publishPrepared(savedAnalysisId: SavedAnalysisId)
    suspend fun retrieve(savedAnalysisId: SavedAnalysisId): SavedAnalysisRecord?

    /**
     * Identities only, newest-saved first, capped at [maxCount] -- no decoding of any record's own
     * content (avoids reading potentially-large `analysisText` for every listed entry). The caller
     * (`SavedAnalysisCoordinator.listRecent`) resolves each identity via [retrieve] to build the
     * bounded, metadata-only [SavedAnalysisSummary] projection the owner UI actually needs.
     */
    suspend fun listRecentIds(maxCount: Int): List<SavedAnalysisId>
}
