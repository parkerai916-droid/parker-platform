package parker.core.interfaces

import java.time.Instant
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Programme 2, Memory Core, Implementation Unit 7. Construction-time and
 * structural validation tests for the five candidate types
 * ([CandidateProvenance], [CandidateEntity], [CandidateDocument],
 * [CandidateAssertion], [CandidateRelationship]) `MEMORY_CORE_CONTRACT_
 * DESIGN_ERRATA_002.md` introduced to resolve the internal-identity-
 * assignment contradiction Unit 7 exposed. Each candidate type's own
 * structural test asserts the *exact* field set Errata 002's field-by-
 * field tables approved -- proving no Memory-Core-owned field (an
 * identifier, a Memory-Core-assigned timestamp, an initial lifecycle
 * status) crept in, and no convenience field was added beyond what the
 * errata approved. `MemoryCore`, `MemoryRetrieval`, and their own
 * supporting query/result types are covered separately, in
 * `MemoryCoreInterfacesTest.kt`.
 */
class MemoryCoreCandidatesTest {

    // ================= CandidateProvenance =================

    private fun minimalCandidateProvenance(
        sourceIdentifier: String = "conversation-turn-1",
        sourceType: String = "conversation",
        acquisitionTime: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        contentNature: ContentNature = ContentNature.ORIGINAL,
    ) = CandidateProvenance(
        sourceIdentifier = sourceIdentifier,
        sourceType = sourceType,
        acquisitionTime = acquisitionTime,
        contentNature = contentNature,
    )

    @Test
    fun `a CandidateProvenance can be constructed with only mandatory fields`() {
        val candidate = minimalCandidateProvenance()

        assertEquals("conversation-turn-1", candidate.sourceIdentifier)
        assertEquals("conversation", candidate.sourceType)
        assertEquals(ContentNature.ORIGINAL, candidate.contentNature)
    }

    @Test
    fun `a CandidateProvenance rejects blank sourceIdentifier, sourceType, creator, and integrityInformation`() {
        assertFailsWith<IllegalArgumentException> { minimalCandidateProvenance(sourceIdentifier = "") }
        assertFailsWith<IllegalArgumentException> { minimalCandidateProvenance(sourceType = "") }
        assertFailsWith<IllegalArgumentException> { minimalCandidateProvenance().copy(creator = "") }
        assertFailsWith<IllegalArgumentException> { minimalCandidateProvenance().copy(integrityInformation = "") }
    }

    @Test
    fun `a CandidateProvenance rejects confidence outside 0-0 to 1-0`() {
        assertFailsWith<IllegalArgumentException> { minimalCandidateProvenance().copy(confidence = 1.5) }
        assertFailsWith<IllegalArgumentException> { minimalCandidateProvenance().copy(confidence = -0.1) }
    }

    @Test
    fun `CandidateProvenance exposes exactly the thirteen caller-supplied fields -- no provenanceId, no ingestionTime`() {
        val expectedFields = setOf(
            "sourceIdentifier",
            "sourceType",
            "acquisitionTime",
            "contentNature",
            "creator",
            "creatorPrincipalId",
            "claimedCreationTime",
            "derivedFrom",
            "extractedFrom",
            "processingHistory",
            "integrityInformation",
            "confidence",
            "sensitivity",
        )

        assertEquals(expectedFields, CandidateProvenance::class.memberProperties.map { it.name }.toSet())
    }

    // ================= CandidateEntity =================

    private fun minimalCandidateEntity(
        entityType: String = "person",
        primaryLabel: String = "Jane Doe",
        provenanceId: ProvenanceId = ProvenanceId("provenance-1"),
    ) = CandidateEntity(
        entityType = entityType,
        primaryLabel = primaryLabel,
        provenanceId = provenanceId,
    )

    @Test
    fun `a CandidateEntity can be constructed with only mandatory fields`() {
        val candidate = minimalCandidateEntity()

        assertEquals("person", candidate.entityType)
        assertEquals("Jane Doe", candidate.primaryLabel)
        assertEquals(ProvenanceId("provenance-1"), candidate.provenanceId)
        assertEquals(emptyList(), candidate.aliases)
    }

    @Test
    fun `a CandidateEntity rejects blank entityType, primaryLabel, or an alias`() {
        assertFailsWith<IllegalArgumentException> { minimalCandidateEntity(entityType = "") }
        assertFailsWith<IllegalArgumentException> { minimalCandidateEntity(primaryLabel = "") }
        assertFailsWith<IllegalArgumentException> { minimalCandidateEntity().copy(aliases = listOf("")) }
    }

    @Test
    fun `CandidateEntity exposes exactly the six caller-supplied fields -- no entityId, no createdAt, no status`() {
        val expectedFields = setOf(
            "entityType",
            "primaryLabel",
            "provenanceId",
            "aliases",
            "relatedPrincipalId",
            "metadata",
        )

        assertEquals(expectedFields, CandidateEntity::class.memberProperties.map { it.name }.toSet())
    }

    // ================= CandidateDocument =================

    private fun minimalCandidateDocument(
        documentType: String = "email",
        locationReference: String = "mailbox://inbox/message-1",
        provenanceId: ProvenanceId = ProvenanceId("provenance-1"),
    ) = CandidateDocument(
        documentType = documentType,
        locationReference = locationReference,
        provenanceId = provenanceId,
    )

    @Test
    fun `a CandidateDocument can be constructed with only mandatory fields, defaulting processingStatus to REGISTERED`() {
        val candidate = minimalCandidateDocument()

        assertEquals("email", candidate.documentType)
        assertEquals("mailbox://inbox/message-1", candidate.locationReference)
        assertEquals(DocumentProcessingStatus.REGISTERED, candidate.processingStatus)
        assertNull(candidate.integrityHash)
    }

    @Test
    fun `a CandidateDocument accepts a caller-supplied processingStatus other than REGISTERED`() {
        val candidate = minimalCandidateDocument().copy(processingStatus = DocumentProcessingStatus.NOT_APPLICABLE)

        assertEquals(DocumentProcessingStatus.NOT_APPLICABLE, candidate.processingStatus)
    }

    @Test
    fun `a CandidateDocument rejects blank documentType, locationReference, or a blank integrityHash`() {
        assertFailsWith<IllegalArgumentException> { minimalCandidateDocument(documentType = "") }
        assertFailsWith<IllegalArgumentException> { minimalCandidateDocument(locationReference = "") }
        assertFailsWith<IllegalArgumentException> { minimalCandidateDocument().copy(integrityHash = "") }
    }

    @Test
    fun `CandidateDocument exposes exactly the six caller-supplied fields -- no documentId, no registeredAt, no status`() {
        val expectedFields = setOf(
            "documentType",
            "locationReference",
            "provenanceId",
            "integrityHash",
            "processingStatus",
            "metadata",
        )

        assertEquals(expectedFields, CandidateDocument::class.memberProperties.map { it.name }.toSet())
    }

    // ================= CandidateAssertion =================

    private fun minimalCandidateAssertion(
        statement: String = "the user prefers window seats",
        provenanceId: ProvenanceId = ProvenanceId("provenance-1"),
    ) = CandidateAssertion(
        statement = statement,
        provenanceId = provenanceId,
    )

    @Test
    fun `a CandidateAssertion can be constructed with only mandatory fields`() {
        val candidate = minimalCandidateAssertion()

        assertEquals("the user prefers window seats", candidate.statement)
        assertNull(candidate.confidence)
    }

    @Test
    fun `a CandidateAssertion rejects a blank statement or confidence outside 0-0 to 1-0`() {
        assertFailsWith<IllegalArgumentException> { minimalCandidateAssertion(statement = "") }
        assertFailsWith<IllegalArgumentException> { minimalCandidateAssertion().copy(confidence = 1.5) }
        assertFailsWith<IllegalArgumentException> { minimalCandidateAssertion().copy(confidence = -0.1) }
    }

    @Test
    fun `CandidateAssertion exposes exactly the four caller-supplied fields -- no assertionId, no status`() {
        val expectedFields = setOf("statement", "provenanceId", "confidence", "metadata")

        assertEquals(expectedFields, CandidateAssertion::class.memberProperties.map { it.name }.toSet())
    }

    // ================= CandidateRelationship =================

    private fun minimalCandidateRelationship(
        relationshipType: String = Relationship.SUPPORTS,
        fromEndpoint: RelationshipEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1"),
        toEndpoint: RelationshipEndpoint = RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, "document-1"),
        directional: Boolean = true,
        provenanceId: ProvenanceId = ProvenanceId("provenance-1"),
    ) = CandidateRelationship(
        relationshipType = relationshipType,
        fromEndpoint = fromEndpoint,
        toEndpoint = toEndpoint,
        directional = directional,
        provenanceId = provenanceId,
    )

    @Test
    fun `a CandidateRelationship can be constructed with all its fields, none of which are optional`() {
        val candidate = minimalCandidateRelationship()

        assertEquals(Relationship.SUPPORTS, candidate.relationshipType)
        assertTrue(candidate.directional)
    }

    @Test
    fun `a CandidateRelationship rejects a blank relationshipType`() {
        assertFailsWith<IllegalArgumentException> { minimalCandidateRelationship(relationshipType = "") }
    }

    @Test
    fun `a CandidateRelationship connecting an endpoint to itself is rejected at submission time, before reaching MemoryCore`() {
        val sameEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1")

        assertFailsWith<IllegalArgumentException> {
            minimalCandidateRelationship(fromEndpoint = sameEndpoint, toEndpoint = sameEndpoint)
        }
    }

    @Test
    fun `CandidateRelationship exposes exactly the five caller-supplied fields -- no relationshipId, no createdAt, no status, no metadata`() {
        val expectedFields = setOf("relationshipType", "fromEndpoint", "toEndpoint", "directional", "provenanceId")

        assertEquals(expectedFields, CandidateRelationship::class.memberProperties.map { it.name }.toSet())
    }

    // ================= Immutability, shared across all five candidate types =================

    @Test
    fun `every candidate type exposes no mutable (var) property`() {
        val candidateClasses = listOf(
            CandidateProvenance::class,
            CandidateEntity::class,
            CandidateDocument::class,
            CandidateAssertion::class,
            CandidateRelationship::class,
        )

        candidateClasses.forEach { candidateClass ->
            val mutableProperties = candidateClass.memberProperties.filterIsInstance<KMutableProperty1<*, *>>()
            assertTrue(
                mutableProperties.isEmpty(),
                "${candidateClass.simpleName} must expose only immutable (val) properties, found: ${mutableProperties.map { it.name }}",
            )
        }
    }

    @Test
    fun `copy() produces a distinct CandidateEntity instance without mutating the original`() {
        val original = minimalCandidateEntity()
        val copy = original.copy(primaryLabel = "John Smith")

        assertEquals("Jane Doe", original.primaryLabel)
        assertEquals("John Smith", copy.primaryLabel)
        assertNotEquals(original, copy)
    }

    // ================= Equality =================

    @Test
    fun `two candidate values with identical fields are equal, and differ when one field differs`() {
        assertEquals(minimalCandidateAssertion(), minimalCandidateAssertion())
        assertNotEquals(minimalCandidateAssertion(), minimalCandidateAssertion(statement = "a different claim"))
    }
}
