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
 * Programme 2, Memory Core, Implementation Unit 2. Construction-time and
 * structural validation tests for [Provenance]
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` Section 7,
 * `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` Section 7). Pure
 * data-shape validation only, mirroring `MemoryContractsTest.kt`'s and
 * `MemoryCoreContractsTest.kt`'s own established scope -- no `Entity`,
 * `Document`, `Assertion`, `Relationship`, `MemoryCore`, or
 * `MemoryRetrieval` exists yet; this file tests [Provenance] alone.
 */
class ProvenanceTest {

    private val acquiredAt = Instant.parse("2026-01-01T00:00:00Z")
    private val ingestedAt = Instant.parse("2026-01-01T00:00:05Z")

    private fun minimalProvenance(
        provenanceId: ProvenanceId = ProvenanceId("provenance-1"),
        sourceIdentifier: String = "conversation-turn-1",
        sourceType: String = "conversation",
        acquisitionTime: Instant = acquiredAt,
        ingestionTime: Instant = ingestedAt,
        contentNature: ContentNature = ContentNature.ORIGINAL,
    ) = Provenance(
        provenanceId = provenanceId,
        sourceIdentifier = sourceIdentifier,
        sourceType = sourceType,
        acquisitionTime = acquisitionTime,
        ingestionTime = ingestionTime,
        contentNature = contentNature,
    )

    // --- Successful construction ---

    @Test
    fun `a Provenance can be constructed with only mandatory fields`() {
        val provenance = minimalProvenance()

        assertEquals(ProvenanceId("provenance-1"), provenance.provenanceId)
        assertEquals("conversation-turn-1", provenance.sourceIdentifier)
        assertEquals("conversation", provenance.sourceType)
        assertEquals(acquiredAt, provenance.acquisitionTime)
        assertEquals(ingestedAt, provenance.ingestionTime)
        assertEquals(ContentNature.ORIGINAL, provenance.contentNature)
    }

    @Test
    fun `a Provenance can be constructed with every optional field supplied`() {
        val claimedAt = Instant.parse("2025-12-25T00:00:00Z")
        val provenance = Provenance(
            provenanceId = ProvenanceId("provenance-2"),
            sourceIdentifier = "document-9",
            sourceType = "document",
            acquisitionTime = acquiredAt,
            ingestionTime = ingestedAt,
            contentNature = ContentNature.EXTRACTED,
            creator = "Jane Doe",
            creatorPrincipalId = PrincipalId("user-1"),
            claimedCreationTime = claimedAt,
            derivedFrom = listOf(ProvenanceId("provenance-0")),
            extractedFrom = DocumentId("document-9"),
            processingHistory = listOf("extracted at $ingestedAt"),
            integrityInformation = "sha256:abc123",
            confidence = 0.8,
            sensitivity = ResourceSensitivity.PERSONAL,
        )

        assertEquals("Jane Doe", provenance.creator)
        assertEquals(PrincipalId("user-1"), provenance.creatorPrincipalId)
        assertEquals(claimedAt, provenance.claimedCreationTime)
        assertEquals(listOf(ProvenanceId("provenance-0")), provenance.derivedFrom)
        assertEquals(DocumentId("document-9"), provenance.extractedFrom)
        assertEquals(listOf("extracted at $ingestedAt"), provenance.processingHistory)
        assertEquals("sha256:abc123", provenance.integrityInformation)
        assertEquals(0.8, provenance.confidence)
        assertEquals(ResourceSensitivity.PERSONAL, provenance.sensitivity)
    }

    // --- Required-field validation ---

    @Test
    fun `a blank sourceIdentifier is rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalProvenance(sourceIdentifier = "") }
        assertFailsWith<IllegalArgumentException> { minimalProvenance(sourceIdentifier = "   ") }
    }

    @Test
    fun `a blank sourceType is rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalProvenance(sourceType = "") }
        assertFailsWith<IllegalArgumentException> { minimalProvenance(sourceType = "   ") }
    }

    // --- Unknown-value handling ---

    @Test
    fun `every optional field genuinely accepts null, expressing unknown rather than a fabricated value`() {
        val provenance = minimalProvenance()

        assertNull(provenance.creator)
        assertNull(provenance.creatorPrincipalId)
        assertNull(provenance.claimedCreationTime)
        assertNull(provenance.extractedFrom)
        assertNull(provenance.integrityInformation)
        assertNull(provenance.confidence)
        assertNull(provenance.sensitivity)
        assertEquals(emptyList(), provenance.derivedFrom)
        assertEquals(emptyList(), provenance.processingHistory)
    }

    @Test
    fun `contentNature UNKNOWN is a fully valid, non-erroring construction`() {
        val provenance = minimalProvenance(contentNature = ContentNature.UNKNOWN)

        assertEquals(ContentNature.UNKNOWN, provenance.contentNature)
    }

    // --- Equality ---

    @Test
    fun `two Provenance records with identical fields are equal`() {
        assertEquals(minimalProvenance(), minimalProvenance())
    }

    @Test
    fun `two Provenance records differing in one field are not equal`() {
        assertNotEquals(minimalProvenance(), minimalProvenance(sourceIdentifier = "conversation-turn-2"))
        assertNotEquals(minimalProvenance(), minimalProvenance(contentNature = ContentNature.INFERRED))
    }

    // --- Immutability expectations ---

    @Test
    fun `Provenance exposes no mutable (var) property`() {
        val mutableProperties = Provenance::class.memberProperties.filterIsInstance<KMutableProperty1<*, *>>()
        assertTrue(
            mutableProperties.isEmpty(),
            "Provenance must expose only immutable (val) properties, found: ${mutableProperties.map { it.name }}",
        )
    }

    @Test
    fun `copy() produces a distinct instance without mutating the original`() {
        val original = minimalProvenance()
        val copy = original.copy(sourceIdentifier = "conversation-turn-2")

        assertEquals("conversation-turn-1", original.sourceIdentifier)
        assertEquals("conversation-turn-2", copy.sourceIdentifier)
        assertNotEquals(original, copy)
    }

    // --- Invalid construction ---

    @Test
    fun `a blank creator is rejected when present, rather than treated as an empty-but-known value`() {
        assertFailsWith<IllegalArgumentException> { minimalProvenance().copy(creator = "") }
    }

    @Test
    fun `a blank integrityInformation is rejected when present`() {
        assertFailsWith<IllegalArgumentException> { minimalProvenance().copy(integrityInformation = "") }
    }

    @Test
    fun `confidence outside 0-0 to 1-0 is rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalProvenance().copy(confidence = 1.5) }
        assertFailsWith<IllegalArgumentException> { minimalProvenance().copy(confidence = -0.1) }
    }

    // --- Confidence preservation ---

    @Test
    fun `confidence at each valid boundary is accepted and preserved unchanged`() {
        assertEquals(0.0, minimalProvenance().copy(confidence = 0.0).confidence)
        assertEquals(1.0, minimalProvenance().copy(confidence = 1.0).confidence)
        assertEquals(0.42, minimalProvenance().copy(confidence = 0.42).confidence)
    }

    // --- ContentNature behaviour ---

    @Test
    fun `a Provenance can be constructed with every ContentNature value without error`() {
        ContentNature.entries.forEach { nature ->
            val provenance = minimalProvenance(contentNature = nature)
            assertEquals(nature, provenance.contentNature)
        }
    }
}
