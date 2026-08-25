package parker.ui

import parker.core.interfaces.TranscriptionFidelity

/** Small pure presentation decisions kept independent of Compose rendering. */

fun canProcessTierA(row: OwnerEvidenceFileRow): Boolean =
    row.status == OwnerEvidenceFileStatus.READY_TO_PROCESS

fun canProcessTierB(row: OwnerEvidenceFileRow): Boolean =
    row.status == OwnerEvidenceFileStatus.REQUIRES_OCR

fun isRowBusy(row: OwnerEvidenceFileRow): Boolean = when (row.status) {
    OwnerEvidenceFileStatus.UPLOADING,
    OwnerEvidenceFileStatus.PROCESSING,
    OwnerEvidenceFileStatus.OCR_PROCESSING,
    -> true
    else -> false
}

fun evidenceStatusLabel(status: OwnerEvidenceFileStatus): String = when (status) {
    OwnerEvidenceFileStatus.SELECTED -> "Selected"
    OwnerEvidenceFileStatus.UPLOADING -> "Importing…"
    OwnerEvidenceFileStatus.IMPORTED -> "Imported"
    OwnerEvidenceFileStatus.IMPORT_FAILED -> "Import failed"
    OwnerEvidenceFileStatus.READY_TO_PROCESS -> "Ready to process"
    OwnerEvidenceFileStatus.PROCESSING -> "Processing…"
    OwnerEvidenceFileStatus.TIER_A_COMPLETE -> "Complete"
    OwnerEvidenceFileStatus.REQUIRES_OCR -> "Requires OCR"
    OwnerEvidenceFileStatus.OCR_PROCESSING -> "Running OCR…"
    OwnerEvidenceFileStatus.COMPLETE -> "Complete"
    OwnerEvidenceFileStatus.FAILED -> "Failed"
}

/** Owner-facing wording that does not turn a stored classification into a verification claim. */
fun transcriptionFidelityLabel(fidelity: TranscriptionFidelity): String = when (fidelity) {
    TranscriptionFidelity.VERBATIM -> "Verbatim classification — verification basis not shown"
    TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION -> "Machine transcription — unverified"
    TranscriptionFidelity.NORMALISED -> "Normalised"
    TranscriptionFidelity.INFERRED_RECONSTRUCTION -> "Inferred reconstruction"
}

/** Human-readable byte count, smallest unit that keeps at most one decimal place. */
fun formatByteLength(byteLength: Long): String = when {
    byteLength < 1024 -> "$byteLength B"
    byteLength < 1024 * 1024 -> "%.1f KiB".format(byteLength / 1024.0)
    else -> "%.1f MiB".format(byteLength / (1024.0 * 1024.0))
}
