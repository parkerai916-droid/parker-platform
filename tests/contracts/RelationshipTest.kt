package parker.core.interfaces

import java.time.Instant
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Programme 2, Memory Core, Implementation Unit 6. Construction-time and
 * structural validation tests for [Relationship] and
 * [RelationshipEndpoint]
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` Section 8,
 * `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`). Pure data-shape
 * validation only, mirroring `AssertionTest.kt`'s own established scope
 * -- no `MemoryCore`, `MemoryRetrieval`, `InMemoryMemoryCore`, lifecycle
 * enforcement, runtime composition, or event publication exists yet.
 * This file does not, and cannot, test referential integrity against a
 * store -- that remains a future Unit's own responsibility, per
 * [Relationship]'s own KDoc.
 */
class RelationshipTest {

    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")

    private fun entityEndpoint(id: String = "entity-1") =
        RelationshipEndpoint(recordKind = RelationshipEndpoint.ENTITY, recordId = id)

    private fun documentEndpoint(id: String = "document-1") =
        RelationshipEndpoint(recordKind = RelationshipEndpoint.DOCUMENT, recordId = id)

    private fun minimalRelationship(
        relationshipId: RelationshipId = RelationshipId("relationship-1"),
        relationshipType: String = Relationship.SUPPORTS,
        fromEndpoint: RelationshipEndpoint = entityEndpoint("entity-1"),
        toEndpoint: RelationshipEndpoint = documentEndpoint("document-1"),
        directional: Boolean = true,
        provenanceId: ProvenanceId = ProvenanceId("provenance-1"),
        createdAt: Instant = this.createdAt,
    ) = Relationship(
        relationshipId = relationshipId,
        relationshipType = relationshipType,
        fromEndpoint = fromEndpoint,
        toEndpoint = toEndpoint,
        directional = directional,
        provenanceId = provenanceId,
        createdAt = createdAt,
    )

    // --- RelationshipEndpoint: successful construction and validation ---

    @Test
    fun `a RelationshipEndpoint can be constructed with a recognised record kind`() {
        val endpoint = entityEndpoint("entity-42")

        assertEquals("entity", endpoint.recordKind)
        assertEquals("entity-42", endpoint.recordId)
    }

    @Test
    fun `a RelationshipEndpoint accepts a caller-defined record kind not among the recognised constants`() {
        val endpoint = RelationshipEndpoint(recordKind = "future-record-kind", recordId = "id-1")

        assertEquals("future-record-kind", endpoint.recordKind)
    }

    @Test
    fun `a blank RelationshipEndpoint recordKind or recordId is rejected`() {
        assertFailsWith<IllegalArgumentException> { RelationshipEndpoint(recordKind = "", recordId = "id-1") }
        assertFailsWith<IllegalArgumentException> { RelationshipEndpoint(recordKind = "entity", recordId = "") }
    }

    @Test
    fun `RelationshipEndpoint recognised record kind constants have the expected six values`() {
        assertEquals(
            setOf("entity", "document", "assertion", "relationship", "conversation-turn", "knowledge-record"),
            setOf(
                RelationshipEndpoint.ENTITY,
                RelationshipEndpoint.DOCUMENT,
                RelationshipEndpoint.ASSERTION,
                RelationshipEndpoint.RELATIONSHIP,
                RelationshipEndpoint.CONVERSATION_TURN,
                RelationshipEndpoint.KNOWLEDGE_RECORD,
            ),
        )
    }

    // --- Relationship: successful construction ---

    @Test
    fun `a Relationship can be constructed with only mandatory fields`() {
        val relationship = minimalRelationship()

        assertEquals(RelationshipId("relationship-1"), relationship.relationshipId)
        assertEquals(Relationship.SUPPORTS, relationship.relationshipType)
        assertEquals(entityEndpoint("entity-1"), relationship.fromEndpoint)
        assertEquals(documentEndpoint("document-1"), relationship.toEndpoint)
        assertTrue(relationship.directional)
        assertEquals(ProvenanceId("provenance-1"), relationship.provenanceId)
        assertEquals(createdAt, relationship.createdAt)
        assertEquals(MemoryCoreRecordStatus.ACTIVE, relationship.status)
    }

    @Test
    fun `a Relationship preserves directional = false when supplied`() {
        val relationship = minimalRelationship(
            relationshipType = Relationship.SAME_AS,
            directional = false,
        )

        assertEquals(false, relationship.directional)
    }

    @Test
    fun `a Relationship accepts endpoints of kinds Memory Core does not own, unverified`() {
        val relationship = minimalRelationship(
            fromEndpoint = RelationshipEndpoint(recordKind = RelationshipEndpoint.CONVERSATION_TURN, recordId = "turn-1"),
            toEndpoint = RelationshipEndpoint(recordKind = RelationshipEndpoint.KNOWLEDGE_RECORD, recordId = "knowledge-record-that-does-not-exist"),
        )

        assertEquals(RelationshipEndpoint.CONVERSATION_TURN, relationship.fromEndpoint.recordKind)
        assertEquals(RelationshipEndpoint.KNOWLEDGE_RECORD, relationship.toEndpoint.recordKind)
    }

    @Test
    fun `a Relationship accepts a caller-defined relationshipType not among the recognised constants`() {
        val relationship = minimalRelationship(relationshipType = "a-future-relationship-type")

        assertEquals("a-future-relationship-type", relationship.relationshipType)
    }

    @Test
    fun `Relationship recognised relationshipType constants have the expected eight values`() {
        assertEquals(
            setOf("supports", "contradicts", "amends", "supersedes", "disputes", "same_as", "extracted_from", "references"),
            setOf(
                Relationship.SUPPORTS,
                Relationship.CONTRADICTS,
                Relationship.AMENDS,
                Relationship.SUPERSEDES,
                Relationship.DISPUTES,
                Relationship.SAME_AS,
                Relationship.EXTRACTED_FROM,
                Relationship.REFERENCES,
            ),
        )
    }

    // --- Required-field / invalid-construction validation ---

    @Test
    fun `a blank relationshipType is rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalRelationship(relationshipType = "") }
        assertFailsWith<IllegalArgumentException> { minimalRelationship(relationshipType = "   ") }
    }

    @Test
    fun `a Relationship connecting an endpoint to itself is rejected`() {
        val sameEndpoint = entityEndpoint("entity-1")

        assertFailsWith<IllegalArgumentException> {
            minimalRelationship(fromEndpoint = sameEndpoint, toEndpoint = sameEndpoint)
        }
    }

    // --- Status ---

    @Test
    fun `a Relationship can be constructed directly in every MemoryCoreRecordStatus, not only ACTIVE`() {
        MemoryCoreRecordStatus.entries.forEach { status ->
            val relationship = minimalRelationship().copy(status = status)
            assertEquals(status, relationship.status)
        }
    }

    // --- Structural: no metadata field, exactly the eight frozen fields ---

    @Test
    fun `Relationship exposes exactly the eight frozen fields`() {
        val expectedFields = setOf(
            "relationshipId",
            "relationshipType",
            "fromEndpoint",
            "toEndpoint",
            "directional",
            "provenanceId",
            "createdAt",
            "status",
        )

        assertEquals(expectedFields, Relationship::class.memberProperties.map { it.name }.toSet())
    }

    // --- Equality ---

    @Test
    fun `two Relationship records with identical fields are equal`() {
        assertEquals(minimalRelationship(), minimalRelationship())
    }

    @Test
    fun `two Relationship records differing in one field are not equal`() {
        assertNotEquals(minimalRelationship(), minimalRelationship(relationshipType = Relationship.CONTRADICTS))
        assertNotEquals(minimalRelationship(), minimalRelationship().copy(directional = false))
    }

    // --- Immutability expectations ---

    @Test
    fun `Relationship exposes no mutable (var) property`() {
        val mutableProperties = Relationship::class.memberProperties.filterIsInstance<KMutableProperty1<*, *>>()
        assertTrue(
            mutableProperties.isEmpty(),
            "Relationship must expose only immutable (val) properties, found: ${mutableProperties.map { it.name }}",
        )
    }

    @Test
    fun `copy() produces a distinct instance without mutating the original`() {
        val original = minimalRelationship()
        val copy = original.copy(relationshipType = Relationship.CONTRADICTS)

        assertEquals(Relationship.SUPPORTS, original.relationshipType)
        assertEquals(Relationship.CONTRADICTS, copy.relationshipType)
        assertNotEquals(original, copy)
    }
}
