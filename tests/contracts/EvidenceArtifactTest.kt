package parker.core.interfaces

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * Evidence Custodian, Implementation Unit 1 (Foundational Identity),
 * corrected. Construction-time and value-equality tests for
 * [EvidenceArtifactId] only.
 *
 * This file previously also tested an `EvidenceArtifact` data class
 * (construction, field-set shape, equality, and `copy()`/no-`var`
 * immutability checks). That type was removed following architectural
 * review -- its `acceptedAt` and `artifactType` fields exceeded what Phase
 * 1 may truthfully represent (see `EvidenceCustodian.kt`'s own "Correction
 * history" note) -- and its tests are removed with it, not retained
 * against a now-nonexistent type.
 *
 * These tests verify only what [EvidenceArtifactId] itself is: a
 * non-vacuous, value-comparable identity. They do not verify, and must
 * not be read as verifying, issuance, uniqueness, acceptance, custody, or
 * any constitutional immutability guarantee -- none of those exist yet
 * for this type to be tested against.
 */
class EvidenceArtifactTest {

    @Test
    fun `a valid EvidenceArtifactId can be constructed`() {
        val id = EvidenceArtifactId("artifact-1")

        assertEquals("artifact-1", id.value)
    }

    @Test
    fun `a blank EvidenceArtifactId is rejected at construction`() {
        assertFailsWith<IllegalArgumentException> { EvidenceArtifactId("") }
    }

    @Test
    fun `a whitespace-only EvidenceArtifactId is rejected at construction`() {
        assertFailsWith<IllegalArgumentException> { EvidenceArtifactId("   ") }
    }

    @Test
    fun `EvidenceArtifactId values with equal contents compare equally`() {
        assertEquals(EvidenceArtifactId("artifact-1"), EvidenceArtifactId("artifact-1"))
    }

    @Test
    fun `EvidenceArtifactId values with different contents compare differently`() {
        assertNotEquals(EvidenceArtifactId("artifact-1"), EvidenceArtifactId("artifact-2"))
    }
}
