package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceDeletionAuditException
import parker.core.interfaces.EvidenceDeletionAuditRecord
import parker.core.interfaces.EvidenceDeletionAuditStage
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Evidence Custodian, Implementation Plan Phase 7 ("Deletion workflow").
 * Behavioural tests of [FileSystemEvidenceDeletionAudit] against a real
 * temporary directory -- durability and append-only behaviour, mirroring
 * [FileSystemEvidenceArtifactStorageTest]'s own precedent for touching a
 * real filesystem, applied here to the audit log rather than evidence
 * content. This suite reads the raw log file directly via [Files.readAllLines]
 * -- deliberately not through [EvidenceDeletionAudit] itself, which
 * declares no read/query operation (Boundary Clarification Section 2).
 */
class FileSystemEvidenceDeletionAuditTest {

    private fun record(
        deletionRequestId: String = "request-1",
        evidenceArtifactId: EvidenceArtifactId = EvidenceArtifactId("artifact-1"),
        stage: EvidenceDeletionAuditStage = EvidenceDeletionAuditStage.AUTHORISED,
    ) = EvidenceDeletionAuditRecord(
        deletionRequestId = deletionRequestId,
        evidenceArtifactId = evidenceArtifactId,
        requestingPrincipalId = PrincipalId("principal-1"),
        recordedAt = Instant.now(),
        stage = stage,
    )

    @Test
    fun `a single record is durably appended and readable back from disk`(@TempDir tempDir: Path) = runTest {
        val logFile = tempDir.resolve("evidence-deletion-audit.log")
        val audit = FileSystemEvidenceDeletionAudit(logFile)

        audit.record(record())

        val lines = Files.readAllLines(logFile)
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("deletionRequestId=request-1"))
        assertTrue(lines[0].contains("stage=AUTHORISED"))
    }

    @Test
    fun `two records for one attempt are both appended, never overwritten`(@TempDir tempDir: Path) = runTest {
        val logFile = tempDir.resolve("evidence-deletion-audit.log")
        val audit = FileSystemEvidenceDeletionAudit(logFile)

        audit.record(record(stage = EvidenceDeletionAuditStage.AUTHORISED))
        audit.record(record(stage = EvidenceDeletionAuditStage.COMPLETED))

        val lines = Files.readAllLines(logFile)
        assertEquals(2, lines.size, "both stages must be appended as separate lines, never merged or overwritten")
        assertTrue(lines[0].contains("stage=AUTHORISED"))
        assertTrue(lines[1].contains("stage=COMPLETED"))
    }

    @Test
    fun `records from separate deletion attempts remain correlated by their own deletionRequestId`(
        @TempDir tempDir: Path,
    ) = runTest {
        val logFile = tempDir.resolve("evidence-deletion-audit.log")
        val audit = FileSystemEvidenceDeletionAudit(logFile)

        audit.record(record(deletionRequestId = "request-1", stage = EvidenceDeletionAuditStage.AUTHORISED))
        audit.record(record(deletionRequestId = "request-2", stage = EvidenceDeletionAuditStage.AUTHORISED))
        audit.record(record(deletionRequestId = "request-1", stage = EvidenceDeletionAuditStage.COMPLETED))

        val lines = Files.readAllLines(logFile)
        assertEquals(3, lines.size)
        assertTrue(lines[0].contains("deletionRequestId=request-1") && lines[0].contains("stage=AUTHORISED"))
        assertTrue(lines[1].contains("deletionRequestId=request-2") && lines[1].contains("stage=AUTHORISED"))
        assertTrue(lines[2].contains("deletionRequestId=request-1") && lines[2].contains("stage=COMPLETED"))
    }

    @Test
    fun `a record survives being read by a fresh instance over the same log file`(@TempDir tempDir: Path) = runTest {
        val logFile = tempDir.resolve("evidence-deletion-audit.log")
        FileSystemEvidenceDeletionAudit(logFile).record(record())

        // A fresh instance, as a new process would construct after a restart, over the same file --
        // construction must never truncate a prior process's own already-durable records.
        FileSystemEvidenceDeletionAudit(logFile)

        val lines = Files.readAllLines(logFile)
        assertEquals(1, lines.size, "a prior record must survive across instances of this class")
    }

    @Test
    fun `constructing over a parent directory that does not exist fails at construction`(@TempDir tempDir: Path) {
        val logFile = tempDir.resolve("missing-subdirectory").resolve("audit.log")

        assertFailsWith<EvidenceDeletionAuditException.PersistenceFailure> {
            FileSystemEvidenceDeletionAudit(logFile)
        }
    }
}
