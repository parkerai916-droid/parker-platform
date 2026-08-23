package parker.ui

import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import parker.core.interfaces.EvidenceArtifactId

/**
 * One file the owner's own client (a native file-picker dialog, never this
 * interface's own caller constructing a path from untrusted input) resolved
 * to a real, existing, absolute local path. [absolutePath] is used exactly
 * once, transiently, to call [OwnerEvidenceOperations.importFile] -- it is
 * never stored in [OwnerEvidenceUiState] and never displayed (Owner-
 * Authorized Local File Ingress Scope Lock §18's "never persisted" rule,
 * applied identically to the client-side selection step this unit adds).
 * [declaredMediaType] is a declared, unauthoritative value only (typically
 * the client OS's own extension-based guess, the same epistemic status a
 * browser's Content-Type header already has) -- `null` means genuinely
 * undeclared, never a guess this controller itself fabricates.
 */
data class OwnerEvidenceFileSelection(
    val absolutePath: String,
    val originalFileName: String,
    val byteLength: Long,
    val declaredMediaType: String? = null,
)

/**
 * Owner Evidence Upload & Processing (first version). Plain-Kotlin
 * presentation controller, mirroring [OwnerUiController]'s own "owns no
 * runtime authority and knows only [OwnerEvidenceOperations]" discipline
 * exactly. Every row is independent: one file's own import/processing
 * failure never touches, blocks, or is visible in any other row's own state
 * (Phase 12's "one failed file must not invalidate already-successful
 * independent files").
 */
class OwnerEvidenceUiController(
    private val operations: OwnerEvidenceOperations,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    private val rowIdSource: () -> String = { UUID.randomUUID().toString() },
) {
    private val controllerJob = SupervisorJob()
    private val scope = CoroutineScope(coroutineContext.minusKey(Job) + controllerJob)
    private val mutableState = MutableStateFlow(OwnerEvidenceUiState())

    val state: StateFlow<OwnerEvidenceUiState> = mutableState.asStateFlow()

    /**
     * Begins independent import for every selection -- a UI-level
     * convenience loop, never a new batch/bulk evidence authority (each
     * [OwnerEvidenceOperations.importFile] call below is its own, separate,
     * independently outcome-tracked operation).
     */
    fun selectFiles(selections: List<OwnerEvidenceFileSelection>) {
        selections.forEach(::beginImport)
    }

    private fun beginImport(selection: OwnerEvidenceFileSelection) {
        val rowId = rowIdSource()
        mutableState.update {
            it.copy(
                files = it.files + OwnerEvidenceFileRow(
                    rowId = rowId,
                    originalFileName = selection.originalFileName,
                    byteLength = selection.byteLength,
                    status = OwnerEvidenceFileStatus.UPLOADING,
                ),
            )
        }
        scope.launch {
            val outcome = try {
                operations.importFile(selection.absolutePath, selection.declaredMediaType)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                EvidenceImportOutcome.Failed("Import failed unexpectedly")
            }
            updateRow(rowId) { row ->
                when (outcome) {
                    is EvidenceImportOutcome.Imported -> row.copy(
                        status = OwnerEvidenceFileStatus.READY_TO_PROCESS,
                        evidenceArtifactId = outcome.evidenceArtifactId,
                    )
                    is EvidenceImportOutcome.Rejected -> row.copy(
                        status = OwnerEvidenceFileStatus.IMPORT_FAILED,
                        message = outcome.reason,
                    )
                    is EvidenceImportOutcome.Failed -> row.copy(
                        status = OwnerEvidenceFileStatus.IMPORT_FAILED,
                        message = outcome.safeMessage,
                    )
                }
            }
        }
    }

    /** Explicit owner action -- never invoked automatically after import. */
    fun processTierA(rowId: String) {
        val evidenceArtifactId = currentRow(rowId)?.takeIf {
            it.status == OwnerEvidenceFileStatus.READY_TO_PROCESS
        }?.evidenceArtifactId ?: return

        updateRow(rowId) { it.copy(status = OwnerEvidenceFileStatus.PROCESSING) }
        scope.launch {
            val outcome = try {
                operations.processTierA(evidenceArtifactId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                TierAProcessingOutcome.Failed("UNKNOWN", "Tier A processing failed unexpectedly")
            }
            updateRow(rowId) { row ->
                when (outcome) {
                    is TierAProcessingOutcome.Admitted -> row.copy(
                        status = OwnerEvidenceFileStatus.TIER_A_COMPLETE,
                        tierAFormat = outcome.format,
                        message = null,
                    )
                    TierAProcessingOutcome.RequiresTierB -> row.copy(
                        status = OwnerEvidenceFileStatus.REQUIRES_OCR,
                        message = null,
                    )
                    is TierAProcessingOutcome.Unsupported -> row.copy(
                        status = OwnerEvidenceFileStatus.FAILED,
                        message = "Unsupported: ${outcome.reason}",
                    )
                    is TierAProcessingOutcome.IntegrityFailure -> row.copy(
                        status = OwnerEvidenceFileStatus.FAILED,
                        message = "Integrity check failed: ${outcome.reason}",
                    )
                    is TierAProcessingOutcome.Failed -> row.copy(
                        status = OwnerEvidenceFileStatus.FAILED,
                        message = "Failed (${outcome.stage}): ${outcome.safeMessage}",
                    )
                }
            }
        }
    }

    /** Explicit owner action -- only reachable once Tier A has already returned RequiresTierB. */
    fun processTierB(rowId: String) {
        val evidenceArtifactId = currentRow(rowId)?.takeIf {
            it.status == OwnerEvidenceFileStatus.REQUIRES_OCR
        }?.evidenceArtifactId ?: return

        updateRow(rowId) { it.copy(status = OwnerEvidenceFileStatus.OCR_PROCESSING) }
        scope.launch {
            val outcome = try {
                operations.processTierB(evidenceArtifactId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                TierBProcessingOutcome.Failed("OCR processing failed unexpectedly")
            }
            updateRow(rowId) { row ->
                when (outcome) {
                    is TierBProcessingOutcome.Completed -> row.copy(
                        status = OwnerEvidenceFileStatus.COMPLETE,
                        message = if (outcome.resultCount > 0) {
                            "${outcome.resultCount} result(s) produced"
                        } else {
                            "No recognisable content"
                        },
                    )
                    is TierBProcessingOutcome.NotAuthorised -> row.copy(
                        status = OwnerEvidenceFileStatus.FAILED,
                        message = "Not authorised: ${outcome.reason}",
                    )
                    is TierBProcessingOutcome.Failed -> row.copy(
                        status = OwnerEvidenceFileStatus.FAILED,
                        message = outcome.safeMessage,
                    )
                }
            }
        }
    }

    fun shutdown() {
        scope.cancel()
    }

    private fun currentRow(rowId: String): OwnerEvidenceFileRow? =
        mutableState.value.files.firstOrNull { it.rowId == rowId }

    private fun updateRow(rowId: String, transform: (OwnerEvidenceFileRow) -> OwnerEvidenceFileRow) {
        mutableState.update { state ->
            state.copy(files = state.files.map { row -> if (row.rowId == rowId) transform(row) else row })
        }
    }
}
