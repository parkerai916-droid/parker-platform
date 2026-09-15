package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId

class EvidenceProcessingStateTest {
    @Test
    fun `evidence processing state is durable and reloadable by evidence identity`() = runTest {
        val root = Files.createTempDirectory("evidence-processing-state")
        val id = EvidenceArtifactId("evidence-state-1")
        val record = EvidenceProcessingStateRecord(
            id,
            EvidenceProcessingState.ANALYSIS_READY,
            derivativeGenerationId = DerivativeGenerationId("generation-1"),
            updatedAt = Instant.parse("2026-09-14T10:00:00Z"),
        )

        FileSystemEvidenceProcessingStateStore(root).record(record)

        assertEquals(record, FileSystemEvidenceProcessingStateStore(root).find(id))
    }

    @Test
    fun `missing state is conservative registered and never ready`() = runTest {
        val store = FileSystemEvidenceProcessingStateStore(Files.createTempDirectory("evidence-processing-state-empty"))
        assertEquals(null, store.find(EvidenceArtifactId("not-yet-processed")))
        assertEquals(EvidenceProcessingState.REGISTERED, EvidenceProcessingState.values().first())
    }
}
