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
 * Programme 2, Memory Core, Implementation Unit 3. Construction-time and
 * structural validation tests for [Entity]
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` Section 4,
 * `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`). Pure data-shape
 * validation only, mirroring `ProvenanceTest.kt`'s own established
 * scope -- no `Document`, `Assertion`, `Relationship`, `MemoryCore`, or
 * `MemoryRetrieval` exists yet; this file tests [Entity] alone, composed
 * only with the already-implemented [Provenance] it references.
 */
class EntityTest {

    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")

    private fun provenance() = Provenance(
        provenanceId = ProvenanceId("provenance-1"),
        sourceIdentifier = "conversation-turn-1",
        sourceType = "conversation",
        acquisitionTime = createdAt,
        ingestionTime = createdAt,
        contentNature = ContentNature.ORIGINAL,
    )

    private fun minimalEntity(
        entityId: EntityId = EntityId("entity-1"),
        entityType: String = "person",
        primaryLabel: String = "Jane Doe",
        provenanceId: ProvenanceId = provenance().provenanceId,
        createdAt: Instant = this.createdAt,
    ) = Entity(
        entityId = entityId,
        entityType = entityType,
        primaryLabel = primaryLabel,
        provenanceId = provenanceId,
        createdAt = createdAt,
    )

    // --- Successful construction ---

    @Test
    fun `an Entity can be constructed with only mandatory fields`() {
        val entity = minimalEntity()

        assertEquals(EntityId("entity-1"), entity.entityId)
        assertEquals("person", entity.entityType)
        assertEquals("Jane Doe", entity.primaryLabel)
        assertEquals(ProvenanceId("provenance-1"), entity.provenanceId)
        assertEquals(createdAt, entity.createdAt)
    }

    @Test
    fun `an Entity can be constructed with every optional field supplied`() {
        val entity = Entity(
            entityId = EntityId("entity-2"),
            entityType = "organisation",
            primaryLabel = "Acme Corp",
            provenanceId = ProvenanceId("provenance-2"),
            createdAt = createdAt,
            aliases = listOf("Acme", "Acme Corporation"),
            relatedPrincipalId = PrincipalId("user-1"),
            status = MemoryCoreRecordStatus.DISPUTED,
            metadata = mapOf("region" to "EMEA"),
        )

        assertEquals(listOf("Acme", "Acme Corporation"), entity.aliases)
        assertEquals(PrincipalId("user-1"), entity.relatedPrincipalId)
        assertEquals(MemoryCoreRecordStatus.DISPUTED, entity.status)
        assertEquals(mapOf("region" to "EMEA"), entity.metadata)
    }

    // --- Required-field validation ---

    @Test
    fun `a blank entityType is rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalEntity(entityType = "") }
        assertFailsWith<IllegalArgumentException> { minimalEntity(entityType = "   ") }
    }

    @Test
    fun `a blank primaryLabel is rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalEntity(primaryLabel = "") }
        assertFailsWith<IllegalArgumentException> { minimalEntity(primaryLabel = "   ") }
    }

    @Test
    fun `a blank alias in the aliases list is rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalEntity().copy(aliases = listOf("Acme", "")) }
    }

    // --- Unknown-value / default handling ---

    @Test
    fun `optional fields genuinely accept absence, and status defaults to ACTIVE`() {
        val entity = minimalEntity()

        assertNull(entity.relatedPrincipalId)
        assertEquals(emptyList(), entity.aliases)
        assertEquals(emptyMap(), entity.metadata)
        assertEquals(MemoryCoreRecordStatus.ACTIVE, entity.status)
    }

    @Test
    fun `an Entity can be constructed directly in every MemoryCoreRecordStatus, not only ACTIVE`() {
        MemoryCoreRecordStatus.entries.forEach { status ->
            val entity = minimalEntity().copy(status = status)
            assertEquals(status, entity.status)
        }
    }

    // --- Equality ---

    @Test
    fun `two Entity records with identical fields are equal`() {
        assertEquals(minimalEntity(), minimalEntity())
    }

    @Test
    fun `two Entity records differing in one field are not equal`() {
        assertNotEquals(minimalEntity(), minimalEntity(primaryLabel = "John Doe"))
        assertNotEquals(minimalEntity(), minimalEntity().copy(status = MemoryCoreRecordStatus.ARCHIVED))
    }

    // --- Immutability expectations ---

    @Test
    fun `Entity exposes no mutable (var) property`() {
        val mutableProperties = Entity::class.memberProperties.filterIsInstance<KMutableProperty1<*, *>>()
        assertTrue(
            mutableProperties.isEmpty(),
            "Entity must expose only immutable (val) properties, found: ${mutableProperties.map { it.name }}",
        )
    }

    @Test
    fun `copy() produces a distinct instance without mutating the original`() {
        val original = minimalEntity()
        val copy = original.copy(primaryLabel = "John Doe")

        assertEquals("Jane Doe", original.primaryLabel)
        assertEquals("John Doe", copy.primaryLabel)
        assertNotEquals(original, copy)
    }

    // --- Composition with Provenance ---

    @Test
    fun `an Entity carries a real Provenance reference by identifier, not an embedded Provenance value`() {
        val realProvenance = provenance()
        val entity = minimalEntity(provenanceId = realProvenance.provenanceId)

        assertEquals(realProvenance.provenanceId, entity.provenanceId)
    }
}
