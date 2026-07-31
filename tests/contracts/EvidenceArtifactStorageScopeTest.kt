package parker.core.interfaces

import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Evidence Custodian, Implementation Plan Phase 2, Unit 1. Structural
 * scope-discipline tests, mirroring `InMemoryWorldModelTest.kt`'s own
 * established "no excluded type exists anywhere in the repository"
 * pattern. Confirms this Unit introduced a storage primitive only --
 * none of the types explicitly excluded from this Unit exist anywhere in
 * the compiled repository, not merely "unused" in this Unit's own files.
 */
class EvidenceArtifactStorageScopeTest {

    @Test
    fun `no EvidenceCustodian interface exists anywhere in the repository`() {
        assertFailsWith<ClassNotFoundException> { Class.forName("parker.core.interfaces.EvidenceCustodian") }
    }

    @Test
    fun `no EvidenceArtifact record type exists anywhere in the repository`() {
        assertFailsWith<ClassNotFoundException> { Class.forName("parker.core.interfaces.EvidenceArtifact") }
    }

    @Test
    fun `no CandidateEvidenceArtifact type exists anywhere in the repository`() {
        assertFailsWith<ClassNotFoundException> { Class.forName("parker.core.interfaces.CandidateEvidenceArtifact") }
    }
}
