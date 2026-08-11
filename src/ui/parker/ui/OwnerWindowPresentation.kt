package parker.ui

/** Small pure presentation decisions kept independent of Compose rendering. */
enum class TranscriptPresentationRole {
    OWNER,
    PARKER,
    SYSTEM,
}

fun presentationRole(entry: OwnerTranscriptEntry): TranscriptPresentationRole = when (entry) {
    is OwnerTranscriptEntry.Owner -> TranscriptPresentationRole.OWNER
    is OwnerTranscriptEntry.Parker -> TranscriptPresentationRole.PARKER
    is OwnerTranscriptEntry.System -> TranscriptPresentationRole.SYSTEM
}

fun canSubmitOwnerText(state: OwnerUiState, draft: String): Boolean =
    draft.isNotBlank() &&
        !state.submissionActive &&
        state.status != OwnerUiStatus.PROCESSING &&
        state.status != OwnerUiStatus.STOPPED

enum class OwnerInputKeyAction {
    SUBMIT,
    INSERT_NEWLINE,
    NONE,
}

fun ownerInputKeyAction(
    enterPressed: Boolean,
    shiftPressed: Boolean,
    state: OwnerUiState,
    draft: String,
): OwnerInputKeyAction = when {
    !enterPressed -> OwnerInputKeyAction.NONE
    shiftPressed -> OwnerInputKeyAction.INSERT_NEWLINE
    canSubmitOwnerText(state, draft) -> OwnerInputKeyAction.SUBMIT
    else -> OwnerInputKeyAction.NONE
}

fun shouldScrollToTranscriptEnd(previousCount: Int, currentCount: Int): Boolean =
    currentCount > 0 && currentCount > previousCount
