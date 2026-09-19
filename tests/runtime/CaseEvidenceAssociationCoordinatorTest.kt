package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.CaseEvidenceAssociationMigrationReadiness
import parker.core.interfaces.CaseEvidenceAssociationMigrationReadinessProvider
import parker.core.interfaces.CaseEvidenceAssociationMigrationStatus
import parker.core.interfaces.CaseEvidenceAssociationStorage
import parker.core.interfaces.CaseGovernanceAuditEventType
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseRecord
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceOccurrence
import parker.core.interfaces.EvidenceSourceManifest
import parker.core.interfaces.PrincipalId

class CaseEvidenceAssociationCoordinatorTest {
    private val at = Instant.parse("2026-09-19T12:00:00Z")
    private val actor = PrincipalId("owner-actor")
    private val artifact = EvidenceArtifactId("evidence-a")
    private val artifactB = EvidenceArtifactId("evidence-b")
    private val caseA = CaseId("case-a")
    private val caseB = CaseId("case-b")

    @Test
    fun `association creation is idempotent and supports simultaneous cases`() = runBlocking {
        val fixture = fixture()
        fixture.seed(artifact, caseA, "content-a")
        fixture.seed(artifact, caseB, "content-a")
        val coordinator = fixture.coordinator()

        val createdA = assertIs<CaseEvidenceAssociationCoordinatorOutcome.Created>(coordinator.createOrGet(actor, caseA, artifact))
        val repeatedA = assertIs<CaseEvidenceAssociationCoordinatorOutcome.AlreadyPresent>(coordinator.createOrGet(actor, caseA, artifact))
        val createdB = assertIs<CaseEvidenceAssociationCoordinatorOutcome.Created>(coordinator.createOrGet(actor, caseB, artifact))
        assertEquals(createdA.association, repeatedA.association)
        assertTrue(createdA.association.associationId != createdB.association.associationId)
        assertEquals(1, fixture.associations.listForCase(caseA).size)
        assertEquals(1, fixture.associations.listForCase(caseB).size)
        assertEquals(2, fixture.associations.listForEvidence(artifact).size)
        assertEquals(2, Files.readAllLines(fixture.auditFile).count { it.contains("eventType=CASE_EVIDENCE_ASSOCIATION_CREATED") })
    }

    @Test
    fun `unknown identities and incomplete migration fail closed`() = runBlocking {
        val fixture = fixture(readiness = CaseEvidenceAssociationMigrationReadiness(CaseEvidenceAssociationMigrationStatus.INCOMPLETE, false, listOf("MIGRATION_INCOMPLETE")))
        fixture.seed(artifact, caseA, "content-a")
        val blocked = fixture.coordinator().createOrGet(actor, caseA, artifact)
        assertIs<CaseEvidenceAssociationCoordinatorOutcome.MigrationNotReady>(blocked)

        val ready = fixture.copy(readiness = CaseEvidenceAssociationMigrationReadiness(CaseEvidenceAssociationMigrationStatus.COMPLETE, true, emptyList())).coordinator()
        assertIs<CaseEvidenceAssociationCoordinatorOutcome.UnknownCase>(ready.createOrGet(actor, caseB, artifactB))
        assertIs<CaseEvidenceAssociationCoordinatorOutcome.UnknownEvidence>(ready.createOrGet(actor, caseA, artifactB))
    }

    @Test
    fun `association corruption fails closed`() = runBlocking {
        val fixture = fixture()
        fixture.seed(artifact, caseA, "content-a")
        val created = assertIs<CaseEvidenceAssociationCoordinatorOutcome.Created>(fixture.coordinator().createOrGet(actor, caseA, artifact)).association
        fixture.associationPath(created.associationId).writeBytes(byteArrayOf(1, 2, 3))

        val outcome = fixture.coordinator().createOrGet(actor, caseA, artifact)
        val failure = assertIs<CaseEvidenceAssociationCoordinatorOutcome.Failure>(outcome)
        assertEquals(CoordinatorFailureKind.CORRUPT_ASSOCIATION, failure.kind)
    }

    @Test
    fun `valid occurrences are idempotent, case-bound, sha-bound, and auditable`() = runBlocking {
        val fixture = fixture()
        val content = "content-a"
        fixture.seed(artifact, caseA, content)
        fixture.seed(artifact, caseB, content)
        val coordinator = fixture.coordinator()
        val associationA = assertIs<CaseEvidenceAssociationCoordinatorOutcome.Created>(coordinator.createOrGet(actor, caseA, artifact)).association
        val associationB = assertIs<CaseEvidenceAssociationCoordinatorOutcome.Created>(coordinator.createOrGet(actor, caseB, artifact)).association
        val first = occurrence(associationA.associationId, caseA, artifact, sha256Text(content), "occ-1", "one.txt")

        val created = assertIs<EvidenceOccurrenceCoordinatorOutcome.Created>(coordinator.registerOccurrence(actor, first))
        val repeated = assertIs<EvidenceOccurrenceCoordinatorOutcome.AlreadyPresent>(coordinator.registerOccurrence(actor, first.copy(createdAt = at.plusSeconds(1))))
        assertEquals(created.occurrence, repeated.occurrence)
        assertEquals(2, fixture.associations.listForEvidence(artifact).size)
        assertEquals(1, fixture.occurrences.listForAssociation(associationA.associationId).size)

        val second = occurrence(associationA.associationId, caseA, artifact, sha256Text(content), "occ-2", "two.txt")
        assertIs<EvidenceOccurrenceCoordinatorOutcome.Created>(coordinator.registerOccurrence(actor, second))
        val caseBOccurrence = occurrence(associationB.associationId, caseB, artifact, sha256Text(content), "occ-b", "case-b.txt")
        assertIs<EvidenceOccurrenceCoordinatorOutcome.Created>(coordinator.registerOccurrence(actor, caseBOccurrence))
        assertEquals(2, fixture.occurrences.listForAssociation(associationA.associationId).size)
        assertEquals(1, fixture.occurrences.listForAssociation(associationB.associationId).size)
        assertEquals(3, Files.readAllLines(fixture.auditFile).count { it.contains("eventType=EVIDENCE_OCCURRENCE_ADDED") })
    }

    @Test
    fun `occurrence rejects wrong association, wrong sha, and missing association`() = runBlocking {
        val fixture = fixture()
        fixture.seed(artifact, caseA, "content-a")
        fixture.seed(artifactB, caseA, "content-b")
        val coordinator = fixture.coordinator()
        val association = assertIs<CaseEvidenceAssociationCoordinatorOutcome.Created>(coordinator.createOrGet(actor, caseA, artifact)).association
        val wrongSha = occurrence(association.associationId, caseA, artifact, "f".repeat(64), "wrong-sha", "bad.txt")
        assertIs<EvidenceOccurrenceCoordinatorOutcome.SourceSha256Mismatch>(coordinator.registerOccurrence(actor, wrongSha))

        val otherAssociationId = deterministicAssociationId(caseA, artifactB)
        val wrongAssociation = occurrence(otherAssociationId, caseA, artifact, sha256Text("content-a"), "wrong-association", "bad.txt")
        assertIs<EvidenceOccurrenceCoordinatorOutcome.AssociationMismatch>(coordinator.registerOccurrence(actor, wrongAssociation))

        val missingAssociation = occurrence(deterministicAssociationId(caseB, artifact), caseB, artifact, sha256Text("content-a"), "missing", "bad.txt")
        assertIs<EvidenceOccurrenceCoordinatorOutcome.UnknownCase>(coordinator.registerOccurrence(actor, missingAssociation))
    }

    @Test
    fun `occurrence survives coordinator restart without duplicate audit`() = runBlocking {
        val fixture = fixture()
        fixture.seed(artifact, caseA, "content-a")
        val firstCoordinator = fixture.coordinator()
        val association = assertIs<CaseEvidenceAssociationCoordinatorOutcome.Created>(firstCoordinator.createOrGet(actor, caseA, artifact)).association
        val occurrence = occurrence(association.associationId, caseA, artifact, sha256Text("content-a"), "restart", "restart.txt")
        assertIs<EvidenceOccurrenceCoordinatorOutcome.Created>(firstCoordinator.registerOccurrence(actor, occurrence))

        val restarted = fixture.coordinator()
        assertIs<EvidenceOccurrenceCoordinatorOutcome.AlreadyPresent>(restarted.registerOccurrence(actor, occurrence))
        assertEquals(1, Files.readAllLines(fixture.auditFile).count { it.contains("eventType=EVIDENCE_OCCURRENCE_ADDED") })
        assertEquals(1, fixture.occurrences.listForAssociation(association.associationId).size)
    }

    private fun occurrence(
        associationId: parker.core.interfaces.CaseEvidenceAssociationId,
        caseId: CaseId,
        artifactId: EvidenceArtifactId,
        sha: String,
        prepId: String,
        path: String,
    ) = EvidenceOccurrence(
        occurrenceId = deterministicPrepOccurrenceId("job-1", prepId),
        associationId = associationId,
        evidenceArtifactId = artifactId,
        caseId = caseId,
        sourceSha256 = sha,
        prepJobId = "job-1",
        prepOccurrenceId = prepId,
        relativePath = path,
        createdAt = at,
    )

    private fun fixture(readiness: CaseEvidenceAssociationMigrationReadiness = CaseEvidenceAssociationMigrationReadiness(CaseEvidenceAssociationMigrationStatus.COMPLETE, true, emptyList())): Fixture {
        val root = Files.createTempDirectory("case-association-coordinator-")
        return Fixture(
            FileSystemCaseStorage(Files.createDirectories(root.resolve("cases"))),
            FileSystemEvidenceArtifactStorage(Files.createDirectories(root.resolve("evidence"))),
            FileSystemEvidenceSourceManifestStorage(Files.createDirectories(root.resolve("manifests"))),
            FileSystemCaseEvidenceAssociationStorage(Files.createDirectories(root.resolve("associations"))),
            FileSystemEvidenceOccurrenceStorage(Files.createDirectories(root.resolve("occurrences"))),
            FileSystemCaseGovernanceAudit(root.resolve("audit.log")),
            root.resolve("audit.log"),
            readiness,
            root.resolve("associations"),
        )
    }

    private class Fixture(
        val cases: FileSystemCaseStorage,
        val evidence: FileSystemEvidenceArtifactStorage,
        val manifests: FileSystemEvidenceSourceManifestStorage,
        val associations: FileSystemCaseEvidenceAssociationStorage,
        val occurrences: FileSystemEvidenceOccurrenceStorage,
        val audit: FileSystemCaseGovernanceAudit,
        val auditFile: Path,
        val readiness: CaseEvidenceAssociationMigrationReadiness,
        private val associationRoot: Path,
    ) {
        fun seed(id: EvidenceArtifactId, caseId: CaseId, content: String) {
            runBlocking {
                val bytes = content.toByteArray()
                if (evidence.read(id) == null) evidence.write(id, bytes)
                if (cases.read(caseId) == null) cases.create(CaseRecord(caseId, caseId.value, Instant.parse("2026-09-19T12:00:00Z")))
                if (manifests.read(id) == null) manifests.write(EvidenceSourceManifest(id, sha256Text(content), bytes.size.toLong(), "text/plain"))
            }
        }

        fun coordinator() = CaseEvidenceAssociationCoordinator(
            cases,
            manifests,
            associations,
            occurrences,
            CaseEvidenceAssociationMigrationReadinessProvider { readiness },
            audit,
            audit,
            { Instant.parse("2026-09-19T13:00:00Z") },
        )

        fun copy(readiness: CaseEvidenceAssociationMigrationReadiness) = Fixture(cases, evidence, manifests, associations, occurrences, audit, auditFile, readiness, associationRoot)

        fun associationPath(id: parker.core.interfaces.CaseEvidenceAssociationId): Path = associationRoot.resolve("${id.value}.association-v1")
    }
}

private fun sha256Text(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
