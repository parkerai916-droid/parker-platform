package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.CaseGovernanceAuditEventType
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseLifecycleStatus
import parker.core.interfaces.CaseRecord
import parker.core.interfaces.CaseStorageException
import parker.core.interfaces.PrincipalId

class CaseLifecycleCoordinatorTest {
    private val clock = { Instant.parse("2026-09-19T00:00:00Z") }
    private val actor = PrincipalId("owner.case-lifecycle-test")

    @Test
    fun `legacy v1 case records default to active`(@TempDir directory: Path) = runTest {
        val caseId = CaseId("case-legacy-v1")
        Files.write(directory.resolve("${caseId.value}.case"), legacyRecord(caseId, "Legacy case"))

        val record = FileSystemCaseStorage(directory).read(caseId)

        assertEquals(CaseLifecycleStatus.ACTIVE, record?.lifecycleStatus)
    }

    @Test
    fun `archive and restore are durable idempotent and audited`(@TempDir directory: Path) = runTest {
        val storage = FileSystemCaseStorage(directory.resolve("cases").also(Files::createDirectories))
        val auditPath = directory.resolve("audit").also(Files::createDirectories).resolve("case-audit.log")
        val audit = FileSystemCaseGovernanceAudit(auditPath)
        val case = CaseRecord(CaseId("case-lifecycle"), "Lifecycle case", clock())
        storage.create(case)
        val coordinator = CaseLifecycleCoordinator(storage, audit, actor, clock)

        val archived = assertIs<CaseLifecycleOutcome.Changed>(coordinator.archive(case.caseId))
        assertEquals(CaseLifecycleStatus.ARCHIVED, archived.case.lifecycleStatus)
        assertIs<CaseLifecycleOutcome.AlreadyArchived>(coordinator.archive(case.caseId))
        assertEquals(CaseLifecycleStatus.ARCHIVED, storage.read(case.caseId)?.lifecycleStatus)

        val restored = assertIs<CaseLifecycleOutcome.Changed>(coordinator.restore(case.caseId))
        assertEquals(CaseLifecycleStatus.ACTIVE, restored.case.lifecycleStatus)
        assertIs<CaseLifecycleOutcome.AlreadyActive>(coordinator.restore(case.caseId))
        assertEquals(CaseLifecycleStatus.ACTIVE, FileSystemCaseStorage(storageRoot = directory.resolve("cases")).read(case.caseId)?.lifecycleStatus)

        val lines = Files.readAllLines(auditPath)
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("eventType=${CaseGovernanceAuditEventType.CASE_ARCHIVED}"))
        assertTrue(lines[1].contains("eventType=${CaseGovernanceAuditEventType.CASE_RESTORED}"))
    }

    @Test
    fun `unknown lifecycle value fails closed`(@TempDir directory: Path) = runTest {
        val caseId = CaseId("case-corrupt-lifecycle")
        val bytes = ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { output ->
                output.writeInt(0x43415345)
                output.writeInt(2)
                output.writeStringForTest(caseId.value)
                output.writeStringForTest("Corrupt case")
                output.writeStringForTest(clock().toString())
                output.writeStringForTest("RETIRED")
            }
            buffer.toByteArray()
        }
        Files.write(directory.resolve("${caseId.value}.case"), bytes)

        assertFailsWith<CaseStorageException.CorruptRecord> {
            FileSystemCaseStorage(directory).read(caseId)
        }
    }

    @Test
    fun `listing retains archived records for explicit historical projections`(@TempDir directory: Path) = runTest {
        val storage = FileSystemCaseStorage(directory)
        val active = CaseRecord(CaseId("case-active"), "Active", clock())
        val archived = CaseRecord(CaseId("case-archived"), "Archived", clock(), CaseLifecycleStatus.ARCHIVED)
        storage.create(active)
        storage.create(archived)

        val records = storage.list().sortedBy { it.caseId.value }
        assertEquals(listOf(CaseLifecycleStatus.ACTIVE, CaseLifecycleStatus.ARCHIVED), records.map { it.lifecycleStatus })
    }

    private fun legacyRecord(caseId: CaseId, name: String): ByteArray = ByteArrayOutputStream().use { buffer ->
        DataOutputStream(buffer).use { output ->
            output.writeInt(0x43415345)
            output.writeInt(1)
            output.writeStringForTest(caseId.value)
            output.writeStringForTest(name)
            output.writeStringForTest(clock().toString())
        }
        buffer.toByteArray()
    }

    private fun DataOutputStream.writeStringForTest(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }
}
