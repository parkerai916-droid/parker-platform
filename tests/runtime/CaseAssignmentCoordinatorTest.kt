package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CaseAssignmentStorage
import parker.core.interfaces.CaseGovernanceAuditEventType
import parker.core.interfaces.CaseGovernanceAuditRecord
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseRecord
import parker.core.interfaces.CaseStorage
import parker.core.interfaces.CaseStorageException
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId

/**
 * CASE-1 — Owner Case / Matter Classification for Evidence. Behavioural tests for
 * [CaseAssignmentCoordinator] against a real, filesystem-backed [DefaultEvidenceCustodian] and real
 * filesystem-backed [CaseStorage]/[CaseAssignmentStorage]/[CaseGovernanceAudit] -- never mocked,
 * mirroring every other coordinator test in this file's own established style.
 */
class CaseAssignmentCoordinatorTest {
    private val owner = PrincipalId("owner.case-1-test")
    private val clock = { Instant.parse("2026-09-06T00:00:00Z") }

    private fun approvingEngine() = FakePermissionEngine { request ->
        PermissionDecision(
            decisionId = DecisionId("decision-1"),
            principalId = request.principalId,
            resourceId = request.targetResources.first(),
            action = PermissionAction.WRITE,
            decision = PermissionDecisionOutcome.APPROVED,
            level = PermissionLevel.AUTOMATIC,
            timestamp = Instant.now(),
        )
    }

    private class Fixture(
        val coordinator: CaseAssignmentCoordinator,
        val custodian: DefaultEvidenceCustodian,
        val caseStorage: FileSystemCaseStorage,
        val assignmentStorage: FileSystemCaseAssignmentStorage,
        val auditLogFile: Path,
    )

    private fun fixture(directory: Path, name: String, caseIdSequence: Iterator<CaseId>? = null): Fixture {
        val custodian = DefaultEvidenceCustodian(
            FileSystemEvidenceArtifactStorage(Files.createDirectories(directory.resolve("$name/evidence"))),
            approvingEngine(),
        )
        val caseStorage = FileSystemCaseStorage(Files.createDirectories(directory.resolve("$name/cases")))
        val assignmentStorage = FileSystemCaseAssignmentStorage(Files.createDirectories(directory.resolve("$name/assignments")))
        val auditLogFile = Files.createDirectories(directory.resolve("$name/audit")).resolve("case-audit.log")
        val audit = FileSystemCaseGovernanceAudit(auditLogFile)
        val coordinator = if (caseIdSequence != null) {
            CaseAssignmentCoordinator(caseStorage, assignmentStorage, audit, custodian, owner, clock) { caseIdSequence.next() }
        } else {
            CaseAssignmentCoordinator(caseStorage, assignmentStorage, audit, custodian, owner, clock)
        }
        return Fixture(coordinator, custodian, caseStorage, assignmentStorage, auditLogFile)
    }

    private suspend fun acceptEvidence(custodian: DefaultEvidenceCustodian, content: String = "evidence bytes"): EvidenceArtifactId {
        val accepted = assertIs<EvidenceAcceptanceResult.Accepted>(
            custodian.accept(owner, CandidateEvidenceArtifact(content.toByteArray())),
        )
        return accepted.acceptedEvidenceArtifact.evidenceArtifactId
    }

    private fun auditLines(auditLogFile: Path): List<String> = Files.readAllLines(auditLogFile)

    @Test
    fun `a case can be created with a server-minted CaseId`(@TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "create")
        val outcome = assertIs<CaseCreationOutcome.Created>(fixture.coordinator.createCase("Michael — O'RANGE ERA"))
        assertEquals("Michael — O'RANGE ERA", outcome.case.caseName)
        assertTrue(outcome.case.caseId.value.isNotBlank())
        assertEquals(outcome.case, fixture.caseStorage.read(outcome.case.caseId))
        assertEquals(1, auditLines(fixture.auditLogFile).size)
        assertTrue(auditLines(fixture.auditLogFile).single().contains("eventType=${CaseGovernanceAuditEventType.CASE_CREATED}"))
    }

    @Test
    fun `blank case name is rejected without creating a record or an audit entry`(@TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "blank-name")
        val outcome = assertIs<CaseCreationOutcome.InvalidCaseName>(fixture.coordinator.createCase("   "))
        assertTrue(outcome.reason.isNotBlank())
        assertEquals(emptyList(), fixture.caseStorage.list())
        assertTrue(auditLines(fixture.auditLogFile).isEmpty())
    }

    @Test
    fun `an invalid blank CaseId is rejected by the domain type itself`() {
        assertFailsWith<IllegalArgumentException> { CaseId("") }
        assertFailsWith<IllegalArgumentException> { CaseId("   ") }
    }

    @Test
    fun `duplicate CaseId fails appropriately and never overwrites the original`(@TempDir directory: Path) = runTest {
        val fixedId = CaseId("case-fixed-for-test")
        val sequence = generateSequence { fixedId }.iterator()
        val fixture = fixture(directory, "duplicate", sequence)

        val first = assertIs<CaseCreationOutcome.Created>(fixture.coordinator.createCase("First case"))
        assertEquals(fixedId, first.case.caseId)

        val second = assertIs<CaseCreationOutcome.Failure>(fixture.coordinator.createCase("Second case, same minted id"))
        assertTrue(second.reason.isNotBlank())
        // The original record is untouched -- never merged, never overwritten.
        assertEquals("First case", fixture.caseStorage.read(fixedId)?.caseName)
    }

    @Test
    fun `FileSystemCaseStorage itself refuses a direct duplicate create`(@TempDir directory: Path) = runTest {
        val storage = FileSystemCaseStorage(directory)
        val caseId = CaseId("case-direct-duplicate")
        storage.create(CaseRecord(caseId, "Original", clock()))
        assertFailsWith<CaseStorageException.DuplicateIdentifier> {
            storage.create(CaseRecord(caseId, "Replacement", clock()))
        }
        assertEquals("Original", storage.read(caseId)?.caseName)
    }

    @Test
    fun `existing evidence can be assigned to an existing case`(@TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "assign")
        val evidenceArtifactId = acceptEvidence(fixture.custodian)
        val case = assertIs<CaseCreationOutcome.Created>(fixture.coordinator.createCase("Steve — Uber ERA")).case

        val outcome = assertIs<CaseAssignmentOutcome.Assigned>(fixture.coordinator.assign(evidenceArtifactId, case.caseId))
        assertEquals(case.caseId, outcome.record.caseId)
        assertEquals(case.caseId, fixture.coordinator.currentAssignment(evidenceArtifactId))
    }

    @Test
    fun `nonexistent evidence cannot be assigned`(@TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "unknown-evidence")
        val case = assertIs<CaseCreationOutcome.Created>(fixture.coordinator.createCase("A real case")).case

        val outcome = fixture.coordinator.assign(EvidenceArtifactId("evidence-never-registered"), case.caseId)
        assertEquals(CaseAssignmentOutcome.UnknownEvidence, outcome)
        assertNull(fixture.assignmentStorage.readAssignment(EvidenceArtifactId("evidence-never-registered")))
        assertTrue(auditLines(fixture.auditLogFile).none { it.contains("EVIDENCE_ASSIGNED") || it.contains("EVIDENCE_REASSIGNED") })
    }

    @Test
    fun `nonexistent case cannot be assigned`(@TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "unknown-case")
        val evidenceArtifactId = acceptEvidence(fixture.custodian)

        val outcome = fixture.coordinator.assign(evidenceArtifactId, CaseId("case-never-created"))
        assertEquals(CaseAssignmentOutcome.UnknownCase, outcome)
        assertNull(fixture.assignmentStorage.readAssignment(evidenceArtifactId))
    }

    @Test
    fun `reassignment is explicit and yields a distinct Reassigned outcome with the correct previous case`(@TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "reassign")
        val evidenceArtifactId = acceptEvidence(fixture.custodian)
        val caseA = assertIs<CaseCreationOutcome.Created>(fixture.coordinator.createCase("Case A")).case
        val caseB = assertIs<CaseCreationOutcome.Created>(fixture.coordinator.createCase("Case B")).case

        assertIs<CaseAssignmentOutcome.Assigned>(fixture.coordinator.assign(evidenceArtifactId, caseA.caseId))
        val reassignment = assertIs<CaseAssignmentOutcome.Reassigned>(fixture.coordinator.assign(evidenceArtifactId, caseB.caseId))
        assertEquals(caseA.caseId, reassignment.previousCaseId)
        assertEquals(caseB.caseId, reassignment.record.caseId)
        assertEquals(caseB.caseId, fixture.coordinator.currentAssignment(evidenceArtifactId))
    }

    @Test
    fun `reassignment produces an EVIDENCE_REASSIGNED audit entry distinct from the original EVIDENCE_ASSIGNED entry`(@TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "reassign-audit")
        val evidenceArtifactId = acceptEvidence(fixture.custodian)
        val caseA = assertIs<CaseCreationOutcome.Created>(fixture.coordinator.createCase("Case A")).case
        val caseB = assertIs<CaseCreationOutcome.Created>(fixture.coordinator.createCase("Case B")).case

        fixture.coordinator.assign(evidenceArtifactId, caseA.caseId)
        fixture.coordinator.assign(evidenceArtifactId, caseB.caseId)

        val lines = auditLines(fixture.auditLogFile)
        // 2 CASE_CREATED + 1 EVIDENCE_ASSIGNED + 1 EVIDENCE_REASSIGNED.
        assertEquals(4, lines.size)
        assertEquals(1, lines.count { it.contains("eventType=${CaseGovernanceAuditEventType.EVIDENCE_ASSIGNED}") })
        val reassignedLine = lines.single { it.contains("eventType=${CaseGovernanceAuditEventType.EVIDENCE_REASSIGNED}") }
        assertTrue(reassignedLine.contains("actorPrincipalId="))
        assertTrue(reassignedLine.contains("recordedAt="))
    }

    @Test
    fun `deliberately returning evidence to Unassigned is itself an audited reassignment`(@TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "unassign")
        val evidenceArtifactId = acceptEvidence(fixture.custodian)
        val case = assertIs<CaseCreationOutcome.Created>(fixture.coordinator.createCase("A case")).case
        fixture.coordinator.assign(evidenceArtifactId, case.caseId)

        val outcome = assertIs<CaseAssignmentOutcome.Reassigned>(fixture.coordinator.assign(evidenceArtifactId, null))
        assertEquals(case.caseId, outcome.previousCaseId)
        assertNull(outcome.record.caseId)
        assertNull(fixture.coordinator.currentAssignment(evidenceArtifactId))
        assertTrue(auditLines(fixture.auditLogFile).any { it.contains("eventType=${CaseGovernanceAuditEventType.EVIDENCE_REASSIGNED}") })
    }

    @Test
    fun `reselecting the current assignment is a harmless unaudited no-op`(@TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "no-change")
        val evidenceArtifactId = acceptEvidence(fixture.custodian)
        val case = assertIs<CaseCreationOutcome.Created>(fixture.coordinator.createCase("A case")).case
        fixture.coordinator.assign(evidenceArtifactId, case.caseId)
        val linesBefore = auditLines(fixture.auditLogFile).size

        val outcome = fixture.coordinator.assign(evidenceArtifactId, case.caseId)
        assertEquals(CaseAssignmentOutcome.NoChange, outcome)
        assertEquals(linesBefore, auditLines(fixture.auditLogFile).size)
    }

    @Test
    fun `never-assigned evidence remains Unassigned by default, with no record required`(@TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "default-unassigned")
        val evidenceArtifactId = acceptEvidence(fixture.custodian)
        assertNull(fixture.coordinator.currentAssignment(evidenceArtifactId))
        assertNull(fixture.assignmentStorage.readAssignment(evidenceArtifactId))
    }

    @Test
    fun `coordinator holds no provider or derivative or HFR dependency of any kind`() {
        val fieldTypes = CaseAssignmentCoordinator::class.java.declaredFields.map { it.type.name }
        assertTrue(fieldTypes.none { "Provider" in it || "Transport" in it || "OpenAi" in it || "Derivative" in it || "HumanFidelity" in it })
    }

    @Test
    fun `case operations never mutate evidence bytes or identity`(@TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "no-evidence-mutation")
        val evidenceArtifactId = acceptEvidence(fixture.custodian, "the exact original bytes")
        val before = assertIs<parker.core.interfaces.EvidenceRetrievalResult.Found>(fixture.custodian.retrieve(owner, evidenceArtifactId))
        val case = assertIs<CaseCreationOutcome.Created>(fixture.coordinator.createCase("A case")).case

        fixture.coordinator.assign(evidenceArtifactId, case.caseId)
        fixture.coordinator.assign(evidenceArtifactId, null)

        val after = assertIs<parker.core.interfaces.EvidenceRetrievalResult.Found>(fixture.custodian.retrieve(owner, evidenceArtifactId))
        assertEquals(evidenceArtifactId, after.evidenceArtifactId)
        assertContentEquals(before.content, after.content)
    }
}
