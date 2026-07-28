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
 * Programme 2, Memory Core, Implementation Unit 5. Construction-time and
 * structural validation tests for [Assertion]
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` Section 6,
 * `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`). Pure data-shape
 * validation only, mirroring `DocumentTest.kt`'s own established scope
 * -- no `Relationship`, `MemoryCore`, or `MemoryRetrieval` exists yet;
 * this file tests [Assertion] alone, composed only with the
 * already-implemented [Provenance] and [Entity] it can reference or sit
 * alongside.
 */
class AssertionTest {

    private fun minimalAssertion(
        assertionId: AssertionId = AssertionId("assertion-1"),
        statement: String = "the user prefers window seats",
        provenanceId: ProvenanceId = ProvenanceId("provenance-1"),
    ) = Assertion(
        assertionId = assertionId,
        statement = statement,
        provenanceId = provenanceId,
    )

    // --- Successful construction ---

    @Test
    fun `an Assertion can be constructed with only mandatory fields`() {
        val assertion = minimalAssertion()

        assertEquals(AssertionId("assertion-1"), assertion.assertionId)
        assertEquals("the user prefers window seats", assertion.statement)
        assertEquals(ProvenanceId("provenance-1"), assertion.provenanceId)
    }

    @Test
    fun `an Assertion can be constructed with every optional field supplied`() {
        val assertion = Assertion(
            assertionId = AssertionId("assertion-2"),
            statement = "the invoice was paid on time",
            provenanceId = ProvenanceId("provenance-2"),
            confidence = 0.9,
            status = MemoryCoreRecordStatus.DISPUTED,
            metadata = mapOf("topic" to "billing"),
        )

        assertEquals(0.9, assertion.confidence)
        assertEquals(MemoryCoreRecordStatus.DISPUTED, assertion.status)
        assertEquals(mapOf("topic" to "billing"), assertion.metadata)
    }

    // --- Required-field validation ---

    @Test
    fun `a blank statement is rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalAssertion(statement = "") }
        assertFailsWith<IllegalArgumentException> { minimalAssertion(statement = "   ") }
    }

    @Test
    fun `confidence outside 0-0 to 1-0 is rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalAssertion().copy(confidence = 1.5) }
        assertFailsWith<IllegalArgumentException> { minimalAssertion().copy(confidence = -0.1) }
    }

    @Test
    fun `confidence at each valid boundary is accepted and preserved unchanged`() {
        assertEquals(0.0, minimalAssertion().copy(confidence = 0.0).confidence)
        assertEquals(1.0, minimalAssertion().copy(confidence = 1.0).confidence)
    }

    // --- Unknown-value / default handling ---

    @Test
    fun `confidence genuinely accepts absence, and status defaults to ACTIVE`() {
        val assertion = minimalAssertion()

        assertNull(assertion.confidence)
        assertEquals(emptyMap(), assertion.metadata)
        assertEquals(MemoryCoreRecordStatus.ACTIVE, assertion.status)
    }

    @Test
    fun `an Assertion can be constructed directly in every MemoryCoreRecordStatus, not only ACTIVE`() {
        MemoryCoreRecordStatus.entries.forEach { status ->
            val assertion = minimalAssertion().copy(status = status)
            assertEquals(status, assertion.status)
        }
    }

    // --- No source field, and no embedded supporting/contradicting reference fields ---

    @Test
    fun `Assertion exposes exactly the six frozen fields -- no source, no supportingReferences, no contradictingReferences`() {
        val expectedFields = setOf(
            "assertionId",
            "statement",
            "provenanceId",
            "confidence",
            "status",
            "metadata",
        )

        assertEquals(expectedFields, Assertion::class.memberProperties.map { it.name }.toSet())
    }

    // --- The central guarantee: creating an Assertion has no side effect on anything else ---

    @Test
    fun `constructing an Assertion has no observable effect on an unrelated, already-constructed Entity`() {
        val provenance = Provenance(
            provenanceId = ProvenanceId("provenance-3"),
            sourceIdentifier = "conversation-turn-1",
            sourceType = "conversation",
            acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
            ingestionTime = Instant.parse("2026-01-01T00:00:00Z"),
            contentNature = ContentNature.ORIGINAL,
        )
        val entity = Entity(
            entityId = EntityId("entity-1"),
            entityType = "person",
            primaryLabel = "Jane Doe",
            provenanceId = provenance.provenanceId,
            createdAt = provenance.ingestionTime,
        )
        val entityBeforeAssertion = entity.copy()

        // Constructing an Assertion that plausibly "concerns" the same Entity -- Assertion has no
        // constructor parameter capable of referencing, let alone mutating, an Entity at all.
        minimalAssertion(statement = "Jane Doe's preferences have changed")

        assertEquals(entityBeforeAssertion, entity, "Entity must be entirely unchanged by an unrelated Assertion's construction")
    }

    // --- Equality ---

    @Test
    fun `two Assertion records with identical fields are equal`() {
        assertEquals(minimalAssertion(), minimalAssertion())
    }

    @Test
    fun `two Assertion records differing in one field are not equal`() {
        assertNotEquals(minimalAssertion(), minimalAssertion(statement = "a different claim"))
        assertNotEquals(minimalAssertion(), minimalAssertion().copy(status = MemoryCoreRecordStatus.SUPERSEDED))
    }

    // --- Immutability expectations ---

    @Test
    fun `Assertion exposes no mutable (var) property`() {
        val mutableProperties = Assertion::class.memberProperties.filterIsInstance<KMutableProperty1<*, *>>()
        assertTrue(
            mutableProperties.isEmpty(),
            "Assertion must expose only immutable (val) properties, found: ${mutableProperties.map { it.name }}",
        )
    }

    @Test
    fun `copy() produces a distinct instance without mutating the original`() {
        val original = minimalAssertion()
        val copy = original.copy(statement = "a different claim")

        assertEquals("the user prefers window seats", original.statement)
        assertEquals("a different claim", copy.statement)
        assertNotEquals(original, copy)
    }
}
