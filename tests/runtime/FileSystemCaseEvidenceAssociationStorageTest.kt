package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.CaseEvidenceAssociationCreationOutcome
import parker.core.interfaces.CaseEvidenceAssociationId
import parker.core.interfaces.CaseId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceOccurrence
import parker.core.interfaces.EvidenceOccurrenceId
import parker.core.interfaces.EvidenceOccurrenceRegistrationOutcome

class FileSystemCaseEvidenceAssociationStorageTest {
    private val at = Instant.parse("2026-09-19T12:00:00Z")
    private val artifactA = EvidenceArtifactId("evidence-a")
    private val artifactB = EvidenceArtifactId("evidence-b")

    @Test
    fun `association create get and list are idempotent and case scoped`() = runBlocking {
        val root = Files.createTempDirectory("association-storage-")
        val storage = FileSystemCaseEvidenceAssociationStorage(root)

        val created = assertIs<CaseEvidenceAssociationCreationOutcome.Created>(storage.createOrGet(CaseId("case-a"), artifactA, at))
        val repeated = assertIs<CaseEvidenceAssociationCreationOutcome.AlreadyPresent>(storage.createOrGet(CaseId("case-a"), artifactA, at.plusSeconds(60)))
        assertEquals(created.association, repeated.association)
        assertEquals(created.association, storage.find(CaseId("case-a"), artifactA))

        val caseB = assertIs<CaseEvidenceAssociationCreationOutcome.Created>(storage.createOrGet(CaseId("case-b"), artifactA, at))
        val otherArtifact = assertIs<CaseEvidenceAssociationCreationOutcome.Created>(storage.createOrGet(CaseId("case-a"), artifactB, at))
        assertTrue(caseB.association.associationId != created.association.associationId)
        assertEquals(listOf(created.association, otherArtifact.association).map { it.associationId }, storage.listForCase(CaseId("case-a")).map { it.associationId })
        assertEquals(listOf(created.association, caseB.association).map { it.associationId }, storage.listForEvidence(artifactA).map { it.associationId })
    }

    @Test
    fun `association identity is deterministic and survives reopen`() = runBlocking {
        val root = Files.createTempDirectory("association-restart-")
        val first = FileSystemCaseEvidenceAssociationStorage(root)
        val created = assertIs<CaseEvidenceAssociationCreationOutcome.Created>(first.createOrGet(CaseId("case-a"), artifactA, at))

        val reopened = FileSystemCaseEvidenceAssociationStorage(root)
        val repeated = assertIs<CaseEvidenceAssociationCreationOutcome.AlreadyPresent>(reopened.createOrGet(CaseId("case-a"), artifactA, at.plusSeconds(1)))
        assertEquals(created.association.associationId, repeated.association.associationId)
        assertEquals(created.association, reopened.find(CaseId("case-a"), artifactA))
    }

    @Test
    fun `independent storage instances have one winner for concurrent association and occurrence creation`() = runBlocking {
        val associationRoot = Files.createTempDirectory("association-race-")
        val associationAttempts = (1..16).map {
            async(Dispatchers.Default) {
                FileSystemCaseEvidenceAssociationStorage(associationRoot)
                    .createOrGet(CaseId("case-a"), artifactA, at.plusSeconds(it.toLong()))
            }
        }.awaitAll()
        assertEquals(1, associationAttempts.count { it is CaseEvidenceAssociationCreationOutcome.Created })
        assertEquals(15, associationAttempts.count { it is CaseEvidenceAssociationCreationOutcome.AlreadyPresent })
        val association = FileSystemCaseEvidenceAssociationStorage(associationRoot)
            .find(CaseId("case-a"), artifactA)!!
        assertEquals(association, (associationAttempts.first { it is CaseEvidenceAssociationCreationOutcome.Created } as CaseEvidenceAssociationCreationOutcome.Created).association)
        assertTrue(associationAttempts.all { outcome ->
            (outcome as? CaseEvidenceAssociationCreationOutcome.Created)?.association == association ||
                (outcome as CaseEvidenceAssociationCreationOutcome.AlreadyPresent).association == association
        })

        val occurrenceRoot = Files.createTempDirectory("occurrence-race-")
        val occurrence = occurrence(CaseEvidenceAssociationId("association-1"), "job-race", "occ-race", "one.txt")
        val occurrenceAttempts = (1..16).map {
            async(Dispatchers.Default) {
                FileSystemEvidenceOccurrenceStorage(occurrenceRoot).createOrGet(
                    occurrence.copy(createdAt = at.plusSeconds(it.toLong())),
                )
            }
        }.awaitAll()
        assertEquals(1, occurrenceAttempts.count { it is EvidenceOccurrenceRegistrationOutcome.Created })
        assertEquals(15, occurrenceAttempts.count { it is EvidenceOccurrenceRegistrationOutcome.AlreadyPresent })
        val storedOccurrence = FileSystemEvidenceOccurrenceStorage(occurrenceRoot).findByPrepOccurrence("job-race", "occ-race")!!
        assertTrue(occurrenceAttempts.all { outcome ->
            (outcome as? EvidenceOccurrenceRegistrationOutcome.Created)?.occurrence == storedOccurrence ||
                (outcome as EvidenceOccurrenceRegistrationOutcome.AlreadyPresent).occurrence == storedOccurrence
        })
    }

    @Test
    fun `corrupt association record fails closed`() = runBlocking {
        val root = Files.createTempDirectory("association-corrupt-")
        val storage = FileSystemCaseEvidenceAssociationStorage(root)
        val created = assertIs<CaseEvidenceAssociationCreationOutcome.Created>(storage.createOrGet(CaseId("case-a"), artifactA, at))
        val path = root.resolve("${created.association.associationId.value}.association-v1")
        path.writeBytes(byteArrayOf(1, 2, 3))

        assertFailsWith<CaseEvidenceAssociationStorageException.CorruptRecord> {
            runBlocking { storage.find(CaseId("case-a"), artifactA) }
        }
        assertFailsWith<CaseEvidenceAssociationStorageException.CorruptRecord> {
            runBlocking { storage.listForCase(CaseId("case-a")) }
        }
    }

    @Test
    fun `occurrence create get and listing use prep identity`() = runBlocking {
        val root = Files.createTempDirectory("occurrence-storage-")
        val storage = FileSystemEvidenceOccurrenceStorage(root)
        val association = CaseEvidenceAssociationId("case-evidence-association-" + "a".repeat(64))
        val occurrence = occurrence(association, "job-1", "occ-1", "docs/a.txt")

        val created = assertIs<EvidenceOccurrenceRegistrationOutcome.Created>(storage.createOrGet(occurrence))
        val repeated = assertIs<EvidenceOccurrenceRegistrationOutcome.AlreadyPresent>(storage.createOrGet(occurrence.copy(createdAt = at.plusSeconds(100))))
        assertEquals(created.occurrence, repeated.occurrence)
        assertEquals(created.occurrence, storage.findByPrepOccurrence("job-1", "occ-1"))
        assertEquals(listOf(occurrence.occurrenceId), storage.listForAssociation(association).map { it.occurrenceId })
    }

    @Test
    fun `occurrence supports multiple provenance occurrences for one association`() = runBlocking {
        val root = Files.createTempDirectory("occurrence-multiple-")
        val storage = FileSystemEvidenceOccurrenceStorage(root)
        val association = CaseEvidenceAssociationId("association-1")
        val first = occurrence(association, "job-1", "occ-1", "one.txt")
        val second = occurrence(association, "job-1", "occ-2", "two.txt")

        storage.createOrGet(first)
        storage.createOrGet(second)
        assertEquals(listOf(first.occurrenceId, second.occurrenceId).sortedBy { it.value }, storage.listForAssociation(association).map { it.occurrenceId })
    }

    @Test
    fun `occurrence storage persists across reopen and rejects prep identity mismatch`() = runBlocking {
        val root = Files.createTempDirectory("occurrence-restart-")
        val occurrence = occurrence(CaseEvidenceAssociationId("association-1"), "job-1", "occ-1", "one.txt")
        val first = FileSystemEvidenceOccurrenceStorage(root)
        first.createOrGet(occurrence)

        val reopened = FileSystemEvidenceOccurrenceStorage(root)
        assertEquals(occurrence, reopened.findByPrepOccurrence("job-1", "occ-1"))
        val wrongId = occurrence.copy(occurrenceId = EvidenceOccurrenceId("caller-supplied-id"))
        assertFailsWith<EvidenceOccurrenceStorageException.CorruptRecord> { reopened.createOrGet(wrongId) }
    }

    @Test
    fun `corrupt occurrence record fails closed`() = runBlocking {
        val root = Files.createTempDirectory("occurrence-corrupt-")
        val storage = FileSystemEvidenceOccurrenceStorage(root)
        val occurrence = occurrence(CaseEvidenceAssociationId("association-1"), "job-1", "occ-1", "one.txt")
        storage.createOrGet(occurrence)
        val path = root.resolve("${occurrence.occurrenceId.value}.occurrence-v1")
        path.writeBytes(byteArrayOf(9, 8, 7))

        assertFailsWith<EvidenceOccurrenceStorageException.CorruptRecord> {
            runBlocking { storage.findByPrepOccurrence("job-1", "occ-1") }
        }
        assertFailsWith<EvidenceOccurrenceStorageException.CorruptRecord> {
            runBlocking { storage.listForAssociation(occurrence.associationId) }
        }
    }

    @Test
    fun `partial temporary file is ignored and cannot become a record`() = runBlocking {
        val root = Files.createTempDirectory("storage-partial-")
        val temp = root.resolve(".tmp/association-interrupted.tmp")
        Files.createDirectories(temp.parent)
        temp.writeBytes(byteArrayOf(1, 2, 3))
        val storage = FileSystemCaseEvidenceAssociationStorage(root)
        assertNull(storage.find(CaseId("case-a"), artifactA))
        assertEquals(emptyList(), storage.listForCase(CaseId("case-a")))
    }

    private fun occurrence(
        association: CaseEvidenceAssociationId,
        job: String,
        prepOccurrence: String,
        path: String,
    ): EvidenceOccurrence = EvidenceOccurrence(
        occurrenceId = deterministicPrepOccurrenceId(job, prepOccurrence),
        associationId = association,
        evidenceArtifactId = artifactA,
        caseId = CaseId("case-a"),
        sourceSha256 = "a".repeat(64),
        prepJobId = job,
        prepOccurrenceId = prepOccurrence,
        relativePath = path,
        createdAt = at,
    )
}
