package parker.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OwnerWindowPresentationTest {

    @Test
    fun `typed transcript authorship maps directly to three distinct presentation roles`() {
        assertEquals(TranscriptPresentationRole.OWNER, presentationRole(OwnerTranscriptEntry.Owner("owner")))
        assertEquals(TranscriptPresentationRole.PARKER, presentationRole(OwnerTranscriptEntry.Parker("parker")))
        assertEquals(TranscriptPresentationRole.SYSTEM, presentationRole(OwnerTranscriptEntry.System("system")))
    }

    @Test
    fun `Ready state enables non-blank submission and rejects blank input`() {
        val ready = OwnerUiState(status = OwnerUiStatus.READY)

        assertTrue(canSubmitOwnerText(ready, "hello"))
        assertFalse(canSubmitOwnerText(ready, " \t"))
    }

    @Test
    fun `Processing disables Send regardless of draft content`() {
        val processing = OwnerUiState(status = OwnerUiStatus.PROCESSING, submissionActive = true)

        assertFalse(canSubmitOwnerText(processing, "second message"))
    }

    @Test
    fun `Enter cannot bypass Processing`() {
        val processing = OwnerUiState(status = OwnerUiStatus.PROCESSING, submissionActive = true)

        assertEquals(
            OwnerInputKeyAction.NONE,
            ownerInputKeyAction(enterPressed = true, shiftPressed = false, processing, "second message"),
        )
    }

    @Test
    fun `Enter submits when enabled and Shift Enter remains newline`() {
        val ready = OwnerUiState(status = OwnerUiStatus.READY)

        assertEquals(
            OwnerInputKeyAction.SUBMIT,
            ownerInputKeyAction(enterPressed = true, shiftPressed = false, ready, "hello"),
        )
        assertEquals(
            OwnerInputKeyAction.INSERT_NEWLINE,
            ownerInputKeyAction(enterPressed = true, shiftPressed = true, ready, "hello"),
        )
    }

    @Test
    fun `transcript scrolling reacts only to appended entries`() {
        assertTrue(shouldScrollToTranscriptEnd(previousCount = 2, currentCount = 3))
        assertFalse(shouldScrollToTranscriptEnd(previousCount = 3, currentCount = 3))
        assertFalse(shouldScrollToTranscriptEnd(previousCount = 1, currentCount = 0))
    }
}
