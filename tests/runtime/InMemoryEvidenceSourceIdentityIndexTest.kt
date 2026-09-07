package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.SourceIdentityReservation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InMemoryEvidenceSourceIdentityIndexTest {

    private val sha256 = "a".repeat(64)
    private val id = EvidenceArtifactId("evidence-1")

    @Test
    fun `the first createOrGet for a hash creates a reservation for the proposed identity`() = runTest {
        val index = InMemoryEvidenceSourceIdentityIndex()

        val reservation = assertIs<SourceIdentityReservation.Created>(index.createOrGet(sha256, id))

        assertEquals(id, reservation.evidenceArtifactId)
    }

    @Test
    fun `a second createOrGet for the same hash returns the first reservation's identity, never the second proposal`() = runTest {
        val index = InMemoryEvidenceSourceIdentityIndex()
        index.createOrGet(sha256, id)

        val reservation = assertIs<SourceIdentityReservation.Existing>(
            index.createOrGet(sha256, EvidenceArtifactId("evidence-2")),
        )

        assertEquals(id, reservation.evidenceArtifactId, "the original reservation must never be replaced by a later proposal")
    }

    @Test
    fun `different hashes each get their own independent reservation`() = runTest {
        val index = InMemoryEvidenceSourceIdentityIndex()

        val first = assertIs<SourceIdentityReservation.Created>(index.createOrGet(sha256, id))
        val second = assertIs<SourceIdentityReservation.Created>(
            index.createOrGet("b".repeat(64), EvidenceArtifactId("evidence-2")),
        )

        assertEquals(id, first.evidenceArtifactId)
        assertEquals(EvidenceArtifactId("evidence-2"), second.evidenceArtifactId)
    }
}
