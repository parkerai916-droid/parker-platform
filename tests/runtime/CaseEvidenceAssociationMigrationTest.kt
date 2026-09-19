package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.security.MessageDigest
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.CaseEvidenceAssociationCreationOutcome
import parker.core.interfaces.CaseEvidenceAssociationMigrationStatus
import parker.core.interfaces.CaseEvidenceAssociationStorage
import parker.core.interfaces.CaseGovernanceAuditEventType
import parker.core.interfaces.CaseGovernanceAuditQuery
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseRecord
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceSourceManifest
import parker.core.interfaces.PrincipalId

class CaseEvidenceAssociationMigrationTest {
    private val at = Instant.parse("2026-09-19T12:00:00Z")
    private val artifact = EvidenceArtifactId("evidence-a")
    private val artifactB = EvidenceArtifactId("evidence-b")
    private val caseA = CaseId("case-a")

    @Test
    fun `current assignment migrates with assignedAt and leaves legacy state intact`() = runBlocking {
        val fixture = fixture()
        fixture.writeEvidence(artifact, "content".toByteArray())
        fixture.cases.create(CaseRecord(caseA, "Case A", at))
        val assignment = parker.core.interfaces.CaseAssignmentRecord(artifact, caseA, at.minusSeconds(30))
        fixture.assignments.writeAssignment(assignment)

        val state = fixture.runner().run()
        assertEquals(CaseEvidenceAssociationMigrationStatus.COMPLETE, state.status)
        val association = fixture.associations.find(caseA, artifact)!!
        assertEquals(assignment.assignedAt, association.associatedAt)
        assertEquals(assignment, fixture.assignments.readAssignment(artifact))
        assertTrue(fixture.runner().readiness().ready)
        assertTrue(fixture.audit.has(fixture.migrationQuery(association.associationId)))
    }

    @Test
    fun `rerun is idempotent and does not duplicate association or migration audit`() = runBlocking {
        val fixture = fixture()
        fixture.seed(artifact, caseA)
        val first = fixture.runner().run()
        val second = fixture.runner().run()
        assertEquals(CaseEvidenceAssociationMigrationStatus.COMPLETE, first.status)
        assertEquals(first, second)
        assertEquals(1, fixture.associations.listForCase(caseA).size)
        val lines = Files.readAllLines(fixture.auditFile).count { it.contains("eventType=CASE_EVIDENCE_ASSOCIATION_MIGRATED") }
        assertEquals(1, lines)
    }

    @Test
    fun `all current assignments are migrated independently`() = runBlocking {
        val fixture = fixture()
        fixture.seed(artifact, caseA)
        fixture.seed(artifactB, CaseId("case-b"))

        val state = fixture.runner().run()
        assertEquals(CaseEvidenceAssociationMigrationStatus.COMPLETE, state.status)
        assertEquals(2, state.processedAssignments.size)
        assertEquals(1, fixture.associations.listForEvidence(artifact).size)
        assertEquals(1, fixture.associations.listForEvidence(artifactB).size)
    }

    @Test
    fun `missing case or evidence leaves migration incomplete`() = runBlocking {
        val missingCase = fixture()
        missingCase.writeEvidence(artifact, byteArrayOf(1))
        missingCase.assignments.writeAssignment(parker.core.interfaces.CaseAssignmentRecord(artifact, caseA, at))
        val missingCaseState = missingCase.runner().run()
        assertEquals(CaseEvidenceAssociationMigrationStatus.INCOMPLETE, missingCaseState.status)
        assertFalse(missingCase.runner().readiness().ready)

        val missingEvidence = fixture()
        missingEvidence.cases.create(CaseRecord(caseA, "Case A", at))
        missingEvidence.assignments.writeAssignment(parker.core.interfaces.CaseAssignmentRecord(artifact, caseA, at))
        val missingEvidenceState = missingEvidence.runner().run()
        assertEquals(CaseEvidenceAssociationMigrationStatus.INCOMPLETE, missingEvidenceState.status)
        assertTrue(missingEvidenceState.failures.any { it.reason.contains("evidence artifact") })
    }

    @Test
    fun `malformed legacy assignment is not silently skipped`() = runBlocking {
        val fixture = fixture()
        fixture.writeEvidence(artifact, byteArrayOf(3))
        fixture.cases.create(CaseRecord(caseA, "Case A", at))
        fixture.assignments.writeAssignment(parker.core.interfaces.CaseAssignmentRecord(artifact, caseA, at))
        fixture.assignmentPath(artifact).writeBytes(byteArrayOf(1, 2, 3))

        val state = fixture.runner().run()
        assertEquals(CaseEvidenceAssociationMigrationStatus.INCOMPLETE, state.status)
        assertTrue(state.failures.any { it.evidenceArtifactId == artifact })
        assertTrue(fixture.associations.listForCase(caseA).isEmpty())
    }

    @Test
    fun `incomplete migration resumes after referenced case is repaired`() = runBlocking {
        val fixture = fixture()
        fixture.writeEvidence(artifact, byteArrayOf(2))
        fixture.assignments.writeAssignment(parker.core.interfaces.CaseAssignmentRecord(artifact, caseA, at))
        assertEquals(CaseEvidenceAssociationMigrationStatus.INCOMPLETE, fixture.runner().run().status)
        fixture.cases.create(CaseRecord(caseA, "Case A", at))

        val completed = fixture.runner().run()
        assertEquals(CaseEvidenceAssociationMigrationStatus.COMPLETE, completed.status)
        assertEquals(1, fixture.associations.listForEvidence(artifact).size)
        assertEquals(1, Files.readAllLines(fixture.auditFile).count { it.contains("CASE_EVIDENCE_ASSOCIATION_MIGRATED") })
    }

    @Test
    fun `existing matching association is preserved and corrupt association fails closed`() = runBlocking {
        val fixture = fixture()
        fixture.seed(artifact, caseA)
        val originalTime = at.minusSeconds(500)
        val existing = assertIs<CaseEvidenceAssociationCreationOutcome.Created>(fixture.associations.createOrGet(caseA, artifact, originalTime)).association
        val state = fixture.runner().run()
        assertEquals(CaseEvidenceAssociationMigrationStatus.COMPLETE, state.status)
        assertEquals(originalTime, fixture.associations.find(caseA, artifact)!!.associatedAt)

        val corruptFixture = fixture()
        corruptFixture.seed(artifact, caseA)
        val corruptId = deterministicAssociationId(caseA, artifact)
        corruptFixture.associationsPath(corruptId).writeBytes(byteArrayOf(9, 8, 7))
        val corruptState = corruptFixture.runner().run()
        assertEquals(CaseEvidenceAssociationMigrationStatus.INCOMPLETE, corruptState.status)
        assertTrue(corruptState.failures.any { it.reason.contains("corrupt") || it.reason.contains("MIGRATION_VALIDATION_FAILED") })
        assertNotNull(existing)
    }

    @Test
    fun `legacy reassignment after completion invalidates migration readiness`() = runBlocking {
        val fixture = fixture()
        fixture.seed(artifact, caseA)
        assertEquals(CaseEvidenceAssociationMigrationStatus.COMPLETE, fixture.runner().run().status)

        val caseB = CaseId("case-b")
        fixture.cases.create(CaseRecord(caseB, "Case B", at))
        fixture.assignments.writeAssignment(parker.core.interfaces.CaseAssignmentRecord(artifact, caseB, at.plusSeconds(1)))

        val readiness = fixture.runner().readiness()
        assertFalse(readiness.ready)
        assertEquals(CaseEvidenceAssociationMigrationStatus.INCOMPLETE, readiness.status)
        assertEquals(CaseEvidenceAssociationMigrationStatus.INCOMPLETE, fixture.state.read().status)
        assertEquals(1, fixture.associations.listForCase(caseA).size)
        assertTrue(fixture.associations.listForCase(caseB).isEmpty())
    }

    private fun fixture(): Fixture {
        val root = Files.createTempDirectory("case-association-migration-")
        val assignmentRoot = Files.createDirectories(root.resolve("assignments"))
        val caseRoot = Files.createDirectories(root.resolve("cases"))
        val evidenceRoot = Files.createDirectories(root.resolve("evidence"))
        val associationRoot = Files.createDirectories(root.resolve("associations"))
        val manifestRoot = Files.createDirectories(root.resolve("manifests"))
        val stateRoot = Files.createDirectories(root.resolve("migration"))
        val auditFile = root.resolve("audit.log")
        return Fixture(
            FileSystemCaseStorage(caseRoot),
            FileSystemCaseAssignmentStorage(assignmentRoot),
            FileSystemEvidenceArtifactStorage(evidenceRoot),
            FileSystemEvidenceSourceManifestStorage(manifestRoot),
            FileSystemCaseEvidenceAssociationStorage(associationRoot),
            FileSystemCaseEvidenceAssociationMigrationStateStorage(stateRoot),
            FileSystemCaseGovernanceAudit(auditFile),
            auditFile,
            associationRoot,
            assignmentRoot,
        )
    }

    private class Fixture(
        val cases: FileSystemCaseStorage,
        val assignments: FileSystemCaseAssignmentStorage,
        val evidence: FileSystemEvidenceArtifactStorage,
        val manifests: FileSystemEvidenceSourceManifestStorage,
        val associations: CaseEvidenceAssociationStorage,
        val state: FileSystemCaseEvidenceAssociationMigrationStateStorage,
        val audit: FileSystemCaseGovernanceAudit,
        val auditFile: Path,
        private val associationRoot: Path,
        private val assignmentRoot: Path,
    ) {
        fun seed(artifact: EvidenceArtifactId, caseId: CaseId) {
            runBlocking {
                writeEvidence(artifact, "seed".toByteArray())
                cases.create(CaseRecord(caseId, caseId.value, Instant.parse("2026-09-19T12:00:00Z")))
                assignments.writeAssignment(parker.core.interfaces.CaseAssignmentRecord(artifact, caseId, Instant.parse("2026-09-19T12:00:00Z")))
            }
        }

        fun writeEvidence(artifact: EvidenceArtifactId, bytes: ByteArray) {
            runBlocking {
                if (evidence.read(artifact) == null) evidence.write(artifact, bytes)
                if (manifests.read(artifact) == null) manifests.write(EvidenceSourceManifest(artifact, sha256(bytes), bytes.size.toLong(), "application/octet-stream"))
            }
        }

        fun runner() = CaseEvidenceAssociationMigrationRunner(
            assignments,
            assignments,
            cases,
            manifests,
            associations,
            state,
            audit,
            audit,
            PrincipalId("migration-test"),
            { Instant.parse("2026-09-19T13:00:00Z") },
        )

        fun migrationQuery(associationId: parker.core.interfaces.CaseEvidenceAssociationId) = CaseGovernanceAuditQuery(
            eventType = CaseGovernanceAuditEventType.CASE_EVIDENCE_ASSOCIATION_MIGRATED,
            caseId = CaseId("case-a"),
            evidenceArtifactId = EvidenceArtifactId("evidence-a"),
            caseEvidenceAssociationId = associationId,
        )

        fun associationsPath(id: parker.core.interfaces.CaseEvidenceAssociationId): Path = associationRoot.resolve("${id.value}.association-v1")

        fun assignmentPath(id: EvidenceArtifactId): Path = assignmentRoot.resolve("${id.value}.assignment")
    }
}

private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
