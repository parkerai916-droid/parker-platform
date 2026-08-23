package parker.ui

import parker.core.interfaces.EvidenceArtifactId

/**
 * Owner Evidence Upload & Processing (first version). Per-file lifecycle --
 * every state below is reachable only in this order (never skipped, never
 * reversed): SELECTED -> UPLOADING -> (IMPORT_FAILED | IMPORTED) ->
 * READY_TO_PROCESS -> PROCESSING -> (FAILED | TIER_A_COMPLETE | REQUIRES_OCR)
 * -> [only from REQUIRES_OCR] OCR_PROCESSING -> (FAILED | COMPLETE).
 * "UPLOADING" names the transient custody-import step even though this
 * unit's own transport is a direct, in-process local-file read (§C of the
 * human review report) -- kept as the requested vocabulary rather than
 * introducing a second, competing state name for the identical step.
 */
enum class OwnerEvidenceFileStatus {
    SELECTED,
    UPLOADING,
    IMPORTED,
    IMPORT_FAILED,
    READY_TO_PROCESS,
    PROCESSING,
    TIER_A_COMPLETE,
    REQUIRES_OCR,
    OCR_PROCESSING,
    COMPLETE,
    FAILED,
}

/**
 * One independent file's own complete, truthful state -- never merged with
 * any sibling's. [originalFileName] is a basename only (this unit's own
 * client-side selection never derives, stores, or displays a full local
 * path -- Owner-Authorized Local File Ingress Scope Lock §18's own
 * minimum-data-necessary rule, applied identically here). [message] carries
 * only an already-safe, non-sensitive summary -- never a raw exception, a
 * stack trace, or a server-side filesystem path.
 */
data class OwnerEvidenceFileRow(
    val rowId: String,
    val originalFileName: String,
    val byteLength: Long,
    val status: OwnerEvidenceFileStatus,
    val evidenceArtifactId: EvidenceArtifactId? = null,
    val tierAFormat: String? = null,
    val message: String? = null,
)

data class OwnerEvidenceUiState(
    val files: List<OwnerEvidenceFileRow> = emptyList(),
)
