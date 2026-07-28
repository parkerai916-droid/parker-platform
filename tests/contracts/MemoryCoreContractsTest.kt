package parker.core.interfaces

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * Programme 2, Memory Core, Implementation Unit 1. Construction-time
 * validation tests for the five identifier types and two enumerations
 * this Unit adds to `src/interfaces/MemoryCore.kt`
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` Section 4/7/11,
 * `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` Section 7/8). Pure
 * data-shape validation only, mirroring `IdentifiersTest.kt`'s and
 * `MemoryContractsTest.kt`'s own established scope -- no record type,
 * no interface, and no behavioural (store/runtime) test exists yet;
 * those begin with later Units.
 */
class MemoryCoreContractsTest {

    // --- Identifiers ---

    @Test
    fun `Memory Core identifiers with equal values are equal`() {
        assertEquals(EntityId("entity-1"), EntityId("entity-1"))
        assertEquals(DocumentId("document-1"), DocumentId("document-1"))
        assertEquals(AssertionId("assertion-1"), AssertionId("assertion-1"))
        assertEquals(ProvenanceId("provenance-1"), ProvenanceId("provenance-1"))
        assertEquals(RelationshipId("relationship-1"), RelationshipId("relationship-1"))
    }

    @Test
    fun `Memory Core identifiers with different values are not equal`() {
        assertNotEquals(EntityId("entity-1"), EntityId("entity-2"))
        assertNotEquals(DocumentId("document-1"), DocumentId("document-2"))
        assertNotEquals(AssertionId("assertion-1"), AssertionId("assertion-2"))
        assertNotEquals(ProvenanceId("provenance-1"), ProvenanceId("provenance-2"))
        assertNotEquals(RelationshipId("relationship-1"), RelationshipId("relationship-2"))
    }

    @Test
    fun `blank Memory Core identifiers are rejected at construction`() {
        assertFailsWith<IllegalArgumentException> { EntityId("") }
        assertFailsWith<IllegalArgumentException> { EntityId("   ") }
        assertFailsWith<IllegalArgumentException> { DocumentId("") }
        assertFailsWith<IllegalArgumentException> { DocumentId("   ") }
        assertFailsWith<IllegalArgumentException> { AssertionId("") }
        assertFailsWith<IllegalArgumentException> { AssertionId("   ") }
        assertFailsWith<IllegalArgumentException> { ProvenanceId("") }
        assertFailsWith<IllegalArgumentException> { ProvenanceId("   ") }
        assertFailsWith<IllegalArgumentException> { RelationshipId("") }
        assertFailsWith<IllegalArgumentException> { RelationshipId("   ") }
    }

    // --- MemoryCoreRecordStatus ---

    @Test
    fun `MemoryCoreRecordStatus has exactly the five frozen values`() {
        assertEquals(
            listOf("ACTIVE", "DISPUTED", "SUPERSEDED", "ARCHIVED", "DELETED"),
            MemoryCoreRecordStatus.entries.map { it.name },
        )
    }

    // --- ContentNature ---

    @Test
    fun `ContentNature has exactly the five frozen values, including UNKNOWN`() {
        assertEquals(
            listOf("ORIGINAL", "EXTRACTED", "SUMMARISED", "INFERRED", "UNKNOWN"),
            ContentNature.entries.map { it.name },
        )
    }
}
