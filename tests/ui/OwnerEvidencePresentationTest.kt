package parker.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import parker.core.interfaces.TranscriptionFidelity

class OwnerEvidencePresentationTest {
    @Test
    fun `unverified literal transcription has an explicit unverified machine label`() {
        val label = transcriptionFidelityLabel(TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION)

        assertEquals("Machine transcription — unverified", label)
        assertFalse(label.contains("verbatim", ignoreCase = true))
        assertFalse(label.contains("verified", ignoreCase = true) && !label.contains("unverified", ignoreCase = true))
    }

    @Test
    fun `historical VERBATIM classification does not itself claim a displayed verification basis`() {
        assertEquals(
            "Verbatim classification — verification basis not shown",
            transcriptionFidelityLabel(TranscriptionFidelity.VERBATIM),
        )
    }

    @Test
    fun `normalised and inferred reconstruction labels retain their meanings`() {
        assertEquals("Normalised", transcriptionFidelityLabel(TranscriptionFidelity.NORMALISED))
        assertEquals("Inferred reconstruction", transcriptionFidelityLabel(TranscriptionFidelity.INFERRED_RECONSTRUCTION))
    }
}
