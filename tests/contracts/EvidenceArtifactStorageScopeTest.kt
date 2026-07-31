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
 *
 * ## Revision history (Unit 2)
 *
 * This file originally also asserted that no `EvidenceCustodian` interface
 * and no `CandidateEvidenceArtifact` type existed anywhere in the
 * repository -- both true statements about Unit 1's own scope at the time
 * they were written. Implementation Plan Phase 3, Unit 2 has since
 * introduced both, deliberately and within its own explicitly authorised
 * scope (`src/interfaces/EvidenceCustodian.kt`). Those two assertions are
 * therefore removed here, rather than left in place to fail -- a scope
 * guard that starts asserting something false is not "extra safety," it is
 * a stale test that would have blocked correctly-scoped, correctly-authorised
 * work. The one assertion below that remains true -- no `EvidenceArtifact`
 * record type -- is kept, since Unit 1's "Correction history" removal of
 * that type has not been, and is not, revisited by Unit 2 or any later
 * Unit. See `tests/contracts/EvidenceCustodianScopeTest.kt` for Unit 2's own
 * equivalent scope-discipline tests.
 */
class EvidenceArtifactStorageScopeTest {

    @Test
    fun `no EvidenceArtifact record type exists anywhere in the repository`() {
        assertFailsWith<ClassNotFoundException> { Class.forName("parker.core.interfaces.EvidenceArtifact") }
    }
}
