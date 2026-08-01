package parker.core.interfaces

import kotlin.reflect.full.declaredFunctions
import kotlin.test.Test
import kotlin.test.assertEquals
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

    // --- Implementation Plan Phase 8 ("Optimisation Safeguard Enforcement") ---

    @Test
    fun `EvidenceArtifactStorage declares exactly write, read, and delete -- nothing more`() {
        val declared = EvidenceArtifactStorage::class.declaredFunctions

        assertEquals(
            setOf("write", "read", "delete"),
            declared.map { it.name }.toSet(),
            "EvidenceArtifactStorage must declare exactly write, read, and delete -- no compact, " +
                "optimise, prune, replace, or discard operation may exist at this layer " +
                "(Constitutional Optimisation Safeguard) -- found: ${declared.map { it.name }}",
        )
        assertEquals(3, declared.size, "no operation name may be declared more than once")
    }
}
