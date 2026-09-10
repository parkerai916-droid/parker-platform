package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.HermesProcessingCorrection
import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingFailureKind
import parker.core.interfaces.HermesProcessingHumanDecision
import parker.core.interfaces.HermesProcessingHumanDecisionType
import parker.core.interfaces.HermesProcessingIssue
import parker.core.interfaces.HermesProcessingIssueKind
import parker.core.interfaces.HermesProcessingIssueLocation
import parker.core.interfaces.HermesProcessingMethod
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingResultRecordOutcome
import parker.core.interfaces.HermesProcessingStatus
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DurableHermesProcessingRegistriesTest {
    private val sha = "a".repeat(64)

    private fun result(status: HermesProcessingStatus = HermesProcessingStatus.REVIEW_REQUIRED) = HermesProcessingResult(
        sourceSha256 = sha,
        batchId = "batch-durable",
        status = status,
        methods = setOf(HermesProcessingMethod.OCR, HermesProcessingMethod.DIRECT_TEXT_EXTRACTION),
        proposedEvidenceArtifactId = parker.core.interfaces.EvidenceArtifactId("proposed-1"),
        issues = if (status == HermesProcessingStatus.REVIEW_REQUIRED) listOf(
            HermesProcessingIssue(
                HermesProcessingIssueKind.TABLE_STRUCTURE_AMBIGUITY,
                "table needs review",
                HermesProcessingIssueLocation.DocumentPage(2, 4, 9, "table"),
                "42?",
            ),
        ) else emptyList(),
        failure = if (status == HermesProcessingStatus.FAILED) HermesProcessingFailure(HermesProcessingFailureKind.PROCESSOR_FAILURE, "stopped") else null,
    )

    private fun decision(type: HermesProcessingHumanDecisionType, at: String) = HermesProcessingHumanDecision(
        "batch-durable", sha, type, PrincipalId("owner-steve"), Instant.parse(at), "recorded",
        if (type == HermesProcessingHumanDecisionType.CORRECT) HermesProcessingCorrection(0, "42", "verified") else null,
    )

    @Test
    fun `result survives registry reconstruction and conflicting writes do not overwrite`() = runTest {
        val root = Files.createTempDirectory("hermes-result-")
        val first = FileSystemHermesProcessingResultRegistry(root)
        val stored = result()
        assertIs<HermesProcessingResultRecordOutcome.Recorded>(first.record(stored))

        val recreated = FileSystemHermesProcessingResultRegistry(root)
        assertEquals(stored, recreated.find("batch-durable", sha))
        assertIs<HermesProcessingResultRecordOutcome.AlreadyRecorded>(recreated.record(stored))
        val conflict = result(HermesProcessingStatus.FAILED)
        assertIs<HermesProcessingResultRecordOutcome.Conflict>(recreated.record(conflict))
        assertEquals(stored, recreated.find("batch-durable", sha))
    }

    @Test
    fun `decision history and latest survive reconstruction without changing result`() = runTest {
        val root = Files.createTempDirectory("hermes-decision-")
        val first = FileSystemHermesProcessingDecisionRegistry(root)
        val reject = decision(HermesProcessingHumanDecisionType.REJECT, "2026-01-01T00:00:00Z")
        val accept = decision(HermesProcessingHumanDecisionType.ACCEPT, "2026-01-02T00:00:00Z")
        first.record(reject); first.record(accept)

        val recreated = FileSystemHermesProcessingDecisionRegistry(root)
        assertEquals(listOf(reject, accept), recreated.history("batch-durable", sha))
        assertEquals(accept, recreated.latest("batch-durable", sha))
    }

    @Test
    fun `corrupt persisted result fails closed`() = runTest {
        val root = Files.createTempDirectory("hermes-corrupt-").resolve("processing-results")
        Files.createDirectories(root)
        Files.write(root.resolve("corrupt.hpr"), byteArrayOf(1, 2, 3))
        val registry = FileSystemHermesProcessingResultRegistry(root.parent)
        assertFailsWith<HermesProcessingStorageException.CorruptRecord> { registry.listAll() }
    }
}
