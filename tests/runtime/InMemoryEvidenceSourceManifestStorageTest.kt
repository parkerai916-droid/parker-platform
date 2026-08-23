package parker.core.runtime

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceSourceManifest
import parker.core.interfaces.EvidenceSourceManifestStorageException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class InMemoryEvidenceSourceManifestStorageTest {

    private val id = EvidenceArtifactId("evidence-1")
    private val validSha256 = "b".repeat(64)

    @Test
    fun `a written manifest is readable back unchanged`() = runTest {
        val storage = InMemoryEvidenceSourceManifestStorage()
        val manifest = EvidenceSourceManifest(id, validSha256, 42L, "text/csv", "ledger.csv")

        storage.write(manifest)

        assertEquals(manifest, storage.read(id))
    }

    @Test
    fun `reading an identity nothing was written under returns null`() = runTest {
        val storage = InMemoryEvidenceSourceManifestStorage()

        assertNull(storage.read(EvidenceArtifactId("never-written")))
    }

    @Test
    fun `a duplicate write is refused, never overwritten`() = runTest {
        val storage = InMemoryEvidenceSourceManifestStorage()
        val original = EvidenceSourceManifest(id, validSha256, 1L)
        storage.write(original)

        assertFailsWith<EvidenceSourceManifestStorageException.DuplicateIdentifier> {
            storage.write(original.copy(byteLength = 2L))
        }
        assertEquals(original, storage.read(id))
    }

    @Test
    fun `deleting an existing manifest removes it and returns true`() = runTest {
        val storage = InMemoryEvidenceSourceManifestStorage()
        storage.write(EvidenceSourceManifest(id, validSha256, 1L))

        val deleted = storage.delete(id)

        assertEquals(true, deleted)
        assertNull(storage.read(id))
    }

    @Test
    fun `deleting an absent manifest is an ordinary, non-exceptional false, not an error`() = runTest {
        val storage = InMemoryEvidenceSourceManifestStorage()

        assertEquals(false, storage.delete(EvidenceArtifactId("never-written")))
    }

    @Test
    fun `concurrent duplicate writes yield exactly one durable manifest without overwrite`() = runTest {
        val storage = InMemoryEvidenceSourceManifestStorage()
        val original = EvidenceSourceManifest(id, validSha256, 1L)
        val replacement = original.copy(byteLength = 2L)

        val outcomes = listOf(original, replacement).map { candidate ->
            async { runCatching { storage.write(candidate) } }
        }.map { it.await() }

        assertEquals(1, outcomes.count { it.isSuccess })
        assertEquals(1, outcomes.count { it.exceptionOrNull() is EvidenceSourceManifestStorageException.DuplicateIdentifier })
        val stored = storage.read(id)
        assertEquals(true, stored == original || stored == replacement)
    }
}
