package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import parker.core.interfaces.*

class GovernedAnalysisResultStorageTest {
    @Test
    fun `governed result survives reopen and identical retry is idempotent`() = runTest {
        val root = Files.createTempDirectory("governed-analysis-store")
        val request = AnalysisRequestId.new()
        val result = result(request)
        val first = FileSystemGovernedAnalysisResultStorage(root).createOrGet(result)
        assertIs<GovernedAnalysisResultCreationOutcome.Created>(first)
        val reopened = FileSystemGovernedAnalysisResultStorage(root)
        assertEquals(result, reopened.findByAnalysisRequestId(request))
        val repeat = reopened.createOrGet(result)
        assertIs<GovernedAnalysisResultCreationOutcome.AlreadyPresent>(repeat)
        assertEquals(result, (repeat as GovernedAnalysisResultCreationOutcome.AlreadyPresent).result)
    }

    @Test
    fun `conflicting result for same request fails closed and first record remains`() = runTest {
        val root = Files.createTempDirectory("governed-analysis-conflict")
        val request = AnalysisRequestId.new()
        val storage = FileSystemGovernedAnalysisResultStorage(root)
        val first = result(request)
        storage.createOrGet(first)
        val conflict = first.copy(analysisText = "different")
        assertIs<GovernedAnalysisResultCreationOutcome.ConflictingResult>(storage.createOrGet(conflict))
        assertEquals(first, storage.findByAnalysisRequestId(request))
    }

    @Test
    fun `corrupt record fails closed`() = runTest {
        val root = Files.createTempDirectory("governed-analysis-corrupt")
        val request = AnalysisRequestId.new()
        val result = result(request)
        FileSystemGovernedAnalysisResultStorage(root).createOrGet(result)
        Files.write(root.resolve("${result.resultId.value}.analysis"), byteArrayOf(1, 2, 3))
        assertFailsWith<GovernedAnalysisResultStorageException.CorruptRecord> {
            FileSystemGovernedAnalysisResultStorage(root).findByAnalysisRequestId(request)
        }
    }

    private fun result(request: AnalysisRequestId): GovernedAnalysisResult {
        val evidence = EvidenceArtifactId("evidence-analysis")
        return GovernedAnalysisResult(
            1, GovernedAnalysisResultId.forRequest(request), request, CaseId("case-analysis"), "Case", "What happened?",
            AnalysisType.ISSUE_ANALYSIS, Instant.parse("2026-01-01T00:00:00Z"), "Hermes", null, "session-1", "parker-analysis-agent",
            listOf(GovernedAnalysisEvidenceScopeEntry(evidence, null, emptyList(), DerivativeGenerationId("generation-analysis"), "a".repeat(64), "source.txt")),
            "The answer", StructuredAnalysisResult("The answer", emptyList(), emptyList(), emptyList(), emptyList(), "The conclusion"), listOf("bounded"),
        )
    }
}
