package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*
import kotlin.test.*

class OwnerEvidenceListingTest {
    @TempDir lateinit var root: Path
    private val owner = PrincipalId("owner-list-test")

    @Test fun `durable registrations survive listing composition recreation with exact identities and hashes`() = runTest {
        val bytes = linkedMapOf(
            EvidenceArtifactId("evidence-before-process") to "first durable PDF".toByteArray(),
            EvidenceArtifactId("evidence-second") to "second durable PDF".toByteArray(),
        )
        val storage = FileSystemEvidenceSourceManifestStorage(root)
        bytes.forEach { (id, content) -> storage.write(EvidenceSourceManifest(
            id, sha(content), content.size.toLong(), "application/pdf", "$id.pdf")) }
        val custodian = FakeCustodian(bytes)

        val firstComposition = FileSystemOwnerEvidenceListing(root, owner, custodian).listRegistered()
        val recreatedComposition = FileSystemOwnerEvidenceListing(root, owner, custodian).listRegistered()

        assertEquals(2, firstComposition.size)
        assertEquals(firstComposition, recreatedComposition)
        assertEquals(bytes.keys.toList().sortedBy { it.value }, recreatedComposition.map { it.evidenceArtifactId })
        recreatedComposition.forEach { assertEquals(sha(bytes.getValue(it.evidenceArtifactId)), it.sha256) }
        assertEquals(4, custodian.retrievals) // two fresh governed reads per composition
    }

    @Test fun `empty durable registration store lists empty without side effects`() = runTest {
        val custodian = FakeCustodian(emptyMap())
        assertTrue(FileSystemOwnerEvidenceListing(root, owner, custodian).listRegistered().isEmpty())
        assertEquals(0, custodian.retrievals)
        assertEquals(0L, Files.list(root).use { it.filter { p -> p.fileName.toString().endsWith(".manifest") }.count() })
    }

    @Test fun `registered missing or changed bytes fail the whole list closed`() = runTest {
        val id = EvidenceArtifactId("evidence-integrity")
        val original = "original".toByteArray()
        FileSystemEvidenceSourceManifestStorage(root).write(EvidenceSourceManifest(
            id, sha(original), original.size.toLong(), "application/pdf", "original.pdf"))
        assertFailsWith<IllegalStateException> {
            FileSystemOwnerEvidenceListing(root, owner, FakeCustodian(emptyMap())).listRegistered()
        }
        assertFailsWith<IllegalArgumentException> {
            FileSystemOwnerEvidenceListing(root, owner, FakeCustodian(mapOf(id to "modified".toByteArray()))).listRegistered()
        }
    }

    private class FakeCustodian(private val bytes: Map<EvidenceArtifactId, ByteArray>) : EvidenceCustodian {
        var retrievals = 0
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult {
            retrievals++
            return bytes[evidenceArtifactId]?.let { EvidenceRetrievalResult.Found(evidenceArtifactId, it.copyOf()) }
                ?: EvidenceRetrievalResult.NotFound(evidenceArtifactId)
        }
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) =
            error("listing never accepts evidence")
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
            error("listing reads canonical registration storage directly")
    }

    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
