package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CaseEvidenceAssociationTest {

    @Test
    fun `association and occurrence identifiers are opaque value objects`() {
        assertEquals(CaseEvidenceAssociationId("association-1"), CaseEvidenceAssociationId("association-1"))
        assertEquals(EvidenceOccurrenceId("occurrence-1"), EvidenceOccurrenceId("occurrence-1"))
        assertEquals(CaseEvidenceAssociationId("association-1").hashCode(), CaseEvidenceAssociationId("association-1").hashCode())
    }

    @Test
    fun `blank identifiers are rejected`() {
        assertFailsWith<IllegalArgumentException> { CaseEvidenceAssociationId("") }
        assertFailsWith<IllegalArgumentException> { CaseEvidenceAssociationId(" ") }
        assertFailsWith<IllegalArgumentException> { EvidenceOccurrenceId("") }
        assertFailsWith<IllegalArgumentException> { EvidenceOccurrenceId(" ") }
    }

    @Test
    fun `association carries only canonical relationship facts`() {
        val association = association()
        assertEquals(CaseEvidenceAssociationId("association-1"), association.associationId)
        assertEquals(EvidenceArtifactId("evidence-1"), association.evidenceArtifactId)
        assertEquals(CaseId("case-a"), association.caseId)
        assertEquals(Instant.parse("2026-09-19T00:00:00Z"), association.associatedAt)
    }

    @Test
    fun `association outcome distinguishes creation from idempotent repetition`() {
        val created = CaseEvidenceAssociationCreationOutcome.Created(association())
        val already = CaseEvidenceAssociationCreationOutcome.AlreadyPresent(association())
        assertIs<CaseEvidenceAssociationCreationOutcome.Created>(created)
        assertIs<CaseEvidenceAssociationCreationOutcome.AlreadyPresent>(already)
        assertEquals(created.association, already.association)
    }

    @Test
    fun `prep occurrence preserves source identity and prep provenance`() {
        val occurrence = occurrence()
        assertEquals("a".repeat(64), occurrence.sourceSha256)
        assertEquals("JOB-1", occurrence.prepJobId)
        assertEquals("prep-occ-1", occurrence.prepOccurrenceId)
        assertEquals("folder/document.pdf", occurrence.relativePath)
        assertEquals("prep-occ-archive", occurrence.archiveParentOccurrenceId)
        assertEquals("documents/document.pdf", occurrence.archiveMemberPath)
        assertEquals(association().associationId, occurrence.associationId)
        assertEquals(association().caseId, occurrence.caseId)
        assertEquals(association().evidenceArtifactId, occurrence.evidenceArtifactId)
    }

    @Test
    fun `occurrence outcome distinguishes creation from idempotent repetition`() {
        val created = EvidenceOccurrenceRegistrationOutcome.Created(occurrence())
        val already = EvidenceOccurrenceRegistrationOutcome.AlreadyPresent(occurrence())
        assertIs<EvidenceOccurrenceRegistrationOutcome.Created>(created)
        assertIs<EvidenceOccurrenceRegistrationOutcome.AlreadyPresent>(already)
        assertEquals(created.occurrence, already.occurrence)
    }

    @Test
    fun `occurrence validates digest and prep identity shape`() {
        assertFailsWith<IllegalArgumentException> { occurrence(sourceSha256 = "not-a-sha") }
        assertFailsWith<IllegalArgumentException> { occurrence(prepJobId = "JOB-1", prepOccurrenceId = null) }
        assertFailsWith<IllegalArgumentException> { occurrence(prepJobId = "JOB-1", prepOccurrenceId = "prep-occ-1", relativePath = null) }
        assertFailsWith<IllegalArgumentException> { occurrence(archiveParentOccurrenceId = "archive", archiveMemberPath = null) }
    }

    @Test
    fun `storage contracts expose association and occurrence lookup shapes`() {
        val associationMethods = CaseEvidenceAssociationStorage::class.members.map { it.name }.toSet()
        assertEquals(setOf("createOrGet", "find", "listForCase", "listForEvidence"), associationMethods.intersect(setOf("createOrGet", "find", "listForCase", "listForEvidence")))
        val occurrenceMethods = EvidenceOccurrenceStorage::class.members.map { it.name }.toSet()
        assertEquals(setOf("createOrGet", "findByPrepOccurrence", "listForAssociation"), occurrenceMethods.intersect(setOf("createOrGet", "findByPrepOccurrence", "listForAssociation")))
    }

    private fun association() = CaseEvidenceAssociation(
        associationId = CaseEvidenceAssociationId("association-1"),
        evidenceArtifactId = EvidenceArtifactId("evidence-1"),
        caseId = CaseId("case-a"),
        associatedAt = Instant.parse("2026-09-19T00:00:00Z"),
    )

    private fun occurrence(
        sourceSha256: String = "a".repeat(64),
        prepJobId: String? = "JOB-1",
        prepOccurrenceId: String? = "prep-occ-1",
        relativePath: String? = "folder/document.pdf",
        archiveParentOccurrenceId: String? = "prep-occ-archive",
        archiveMemberPath: String? = "documents/document.pdf",
    ) = EvidenceOccurrence(
        occurrenceId = EvidenceOccurrenceId("occurrence-1"),
        associationId = association().associationId,
        evidenceArtifactId = association().evidenceArtifactId,
        caseId = association().caseId,
        sourceSha256 = sourceSha256,
        prepJobId = prepJobId,
        prepOccurrenceId = prepOccurrenceId,
        relativePath = relativePath,
        archiveParentOccurrenceId = archiveParentOccurrenceId,
        archiveMemberPath = archiveMemberPath,
        createdAt = Instant.parse("2026-09-19T00:00:00Z"),
    )
}
