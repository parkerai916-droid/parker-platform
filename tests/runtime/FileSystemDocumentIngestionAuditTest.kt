package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DocumentIngestionAudit
import parker.core.interfaces.DocumentIngestionAuditException
import parker.core.interfaces.DocumentIngestionAuditRecord
import parker.core.interfaces.DocumentIngestionAuditStage
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId

class FileSystemDocumentIngestionAuditTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `audit append is durable and preserves correlation and recording time`() = runTest {
        val log = directory.resolve("ingestion-audit.log")
        val audit = FileSystemDocumentIngestionAudit(log)
        audit.record(record("attempt-1", "2026-08-23T01:02:03Z"))
        val text = Files.readString(log)
        assertEquals("attempt-1", decodedField(text.trim(), "correlationValue"))
        assertTrue(text.contains("recordedAt=2026-08-23T01:02:03Z"))
        assertEquals("generation-1", decodedField(text.trim(), "derivativeGenerationId"))
    }

    @Test
    fun `records append and never overwrite`() = runTest {
        val log = directory.resolve("ingestion-audit.log")
        val audit = FileSystemDocumentIngestionAudit(log)
        audit.record(record("attempt-1", "2026-08-23T01:02:03Z"))
        audit.record(record("attempt-2", "2026-08-23T01:03:03Z"))
        val lines = Files.readAllLines(log)
        assertEquals(2, lines.size)
        assertEquals("attempt-1", decodedField(lines[0], "correlationValue"))
        assertEquals("attempt-2", decodedField(lines[1], "correlationValue"))
    }

    @Test
    fun `audit persistence failure is surfaced and no success is returned`() = runTest {
        val audit: DocumentIngestionAudit = DocumentIngestionAudit {
            throw DocumentIngestionAuditException.PersistenceFailure("injected", java.io.IOException("disk"))
        }
        assertFailsWith<DocumentIngestionAuditException.PersistenceFailure> { audit.record(record()) }
    }

    @Test
    fun `audit port has no query operation`() {
        assertEquals(listOf("record"), DocumentIngestionAudit::class.members.filter { it.isAbstract }.map { it.name })
    }

    @Test
    fun `opaque string fields are reversibly encoded without record injection`() = runTest {
        val log = directory.resolve("ingestion-audit.log")
        val audit = FileSystemDocumentIngestionAudit(log)
        val correlation = "opaque\\value\twith\rline\nUnicode-Ω"
        val record = DocumentIngestionAuditRecord(
            correlationValue = correlation,
            sourceEvidenceArtifactId = EvidenceArtifactId("source\tΩ"),
            requestingPrincipalId = PrincipalId("principal\nΩ"),
            operationalOutcome = "ADMITTED\tWITH_WARNING",
            recordedAt = Instant.parse("2026-08-23T01:02:03Z"),
            derivativeGenerationId = DerivativeGenerationId("generation\rΩ"),
            stage = DocumentIngestionAuditStage.ADMISSION_AUTHORISED,
        )
        audit.record(record)
        val lines = Files.readAllLines(log)
        assertEquals(1, lines.size)
        assertEquals(correlation, decodedField(lines.single(), "correlationValue"))
        assertEquals("source\tΩ", decodedField(lines.single(), "sourceEvidenceArtifactId"))
        assertEquals("principal\nΩ", decodedField(lines.single(), "requestingPrincipalId"))
        assertEquals("ADMITTED\tWITH_WARNING", decodedField(lines.single(), "operationalOutcome"))
        assertEquals("generation\rΩ", decodedField(lines.single(), "derivativeGenerationId"))
    }

    @Test
    fun `pre-existing audit bytes remain unchanged when a record is appended`() = runTest {
        val log = directory.resolve("ingestion-audit.log")
        Files.writeString(log, "pre-existing\n")
        FileSystemDocumentIngestionAudit(log).record(record())
        assertTrue(Files.readString(log).startsWith("pre-existing\n"))
    }

    private fun record(correlation: String = "attempt-1", time: String = "2026-08-23T01:02:03Z") =
        DocumentIngestionAuditRecord(
            correlationValue = correlation,
            sourceEvidenceArtifactId = EvidenceArtifactId("source-1"),
            requestingPrincipalId = PrincipalId("principal-1"),
            operationalOutcome = "ADMITTED",
            recordedAt = Instant.parse(time),
            derivativeGenerationId = DerivativeGenerationId("generation-1"),
            stage = DocumentIngestionAuditStage.ADMITTED,
        )

    private fun decodedField(line: String, name: String): String {
        val encoded = line.split('\t').single { it.startsWith("$name=") }.substringAfter('=')
        return String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
    }
}
