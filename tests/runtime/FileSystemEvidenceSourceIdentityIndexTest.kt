package parker.core.runtime

import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.SourceIdentityReservation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class FileSystemEvidenceSourceIdentityIndexTest {

    private val sha256 = "c".repeat(64)
    private val id = EvidenceArtifactId("evidence-1")

    private fun newIndex() = FileSystemEvidenceSourceIdentityIndex(Files.createTempDirectory("source-identity-index-test"))

    @Test
    fun `the first createOrGet for a hash creates a durable reservation for the proposed identity`() = runTest {
        val index = newIndex()

        val reservation = assertIs<SourceIdentityReservation.Created>(index.createOrGet(sha256, id))

        assertEquals(id, reservation.evidenceArtifactId)
    }

    @Test
    fun `a second createOrGet for the same hash returns the first reservation's identity, never the second proposal`() = runTest {
        val index = newIndex()
        index.createOrGet(sha256, id)

        val reservation = assertIs<SourceIdentityReservation.Existing>(
            index.createOrGet(sha256, EvidenceArtifactId("evidence-2")),
        )

        assertEquals(id, reservation.evidenceArtifactId, "the original reservation must never be replaced by a later proposal")
    }

    @Test
    fun `a reservation survives across separate index instances backed by the same storage root`() = runTest {
        val root = Files.createTempDirectory("source-identity-index-test-persist")
        FileSystemEvidenceSourceIdentityIndex(root).createOrGet(sha256, id)

        val reopened = FileSystemEvidenceSourceIdentityIndex(root)

        val reservation = assertIs<SourceIdentityReservation.Existing>(
            reopened.createOrGet(sha256, EvidenceArtifactId("evidence-2")),
        )
        assertEquals(id, reservation.evidenceArtifactId)
    }

    @Test
    fun `proposing the identical identity twice for the same hash is tolerated -- reported as Existing, not re-created`() = runTest {
        val index = newIndex()
        index.createOrGet(sha256, id)

        val reservation = assertIs<SourceIdentityReservation.Existing>(index.createOrGet(sha256, id))

        assertEquals(id, reservation.evidenceArtifactId)
    }

    @Test
    fun `a malformed key is rejected before touching the filesystem`() = runTest {
        val index = newIndex()

        assertFailsWith<IllegalArgumentException> { index.createOrGet("not-a-valid-sha256", id) }
    }
}
