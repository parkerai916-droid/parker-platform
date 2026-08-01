package parker.core.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceArtifactStorageException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/**
 * Evidence Custodian, Implementation Plan Phase 2, Unit 1. Behavioural
 * tests of [InMemoryEvidenceArtifactStorage] -- the same
 * [parker.core.interfaces.EvidenceArtifactStorage] behavioural contract
 * [FileSystemEvidenceArtifactStorageTest] exercises against the real
 * filesystem, minus the filesystem-specific concerns (storage-root
 * construction, temporary-file cleanup) that don't apply to an in-memory
 * implementation. Confirms this implementation, though test-only, is not
 * behaviourally looser than the production one for anything the interface
 * itself promises.
 */
class InMemoryEvidenceArtifactStorageTest {

    private fun id(value: String = "artifact-1") = EvidenceArtifactId(value)

    // --- Deletion (Implementation Plan Phase 7) ---

    @Test
    fun `deleting an existing identifier removes it and returns true`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        storage.write(id(), "content".toByteArray())

        val result = storage.delete(id())

        assertTrue(result)
        assertNull(storage.read(id()))
    }

    @Test
    fun `deleting an identifier that was never written returns false`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()

        assertEquals(false, storage.delete(id("never-written")))
    }

    @Test
    fun `deleting an already-deleted identifier returns false the second time`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        storage.write(id(), "content".toByteArray())
        storage.delete(id())

        assertEquals(
            false,
            storage.delete(id()),
            "repeated deletion produces false, never a tombstone-derived distinction (Determination 2)",
        )
    }

    @Test
    fun `an unsafe identifier is rejected on delete, identically to write and read`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()

        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.delete(id("../escaped"))
        }
    }

    @Test
    fun `a first write for a fresh identifier succeeds and is readable back`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val content = "hello evidence".toByteArray()

        storage.write(id(), content)

        assertContentEquals(content, storage.read(id()))
    }

    @Test
    fun `a second write to an already-used identifier fails`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        storage.write(id(), "first".toByteArray())

        assertFailsWith<EvidenceArtifactStorageException.DuplicateIdentifier> {
            storage.write(id(), "second".toByteArray())
        }
    }

    @Test
    fun `a failed duplicate write does not alter the originally stored bytes`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val original = "original content".toByteArray()
        storage.write(id(), original)

        assertFailsWith<EvidenceArtifactStorageException.DuplicateIdentifier> {
            storage.write(id(), "attempted replacement".toByteArray())
        }

        assertContentEquals(original, storage.read(id()))
    }

    @Test
    fun `zero-length content is accepted and reads back as zero-length, not null`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()

        storage.write(id(), ByteArray(0))

        val readBack = storage.read(id())
        assertTrue(readBack != null && readBack.isEmpty(), "expected a non-null, zero-length result")
    }

    @Test
    fun `an unsafe identifier is rejected on write, identically to the filesystem implementation`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()

        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.write(id("../escaped"), "content".toByteArray())
        }
        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.write(id("sub/dir"), "content".toByteArray())
        }
    }

    @Test
    fun `an unsafe identifier is rejected on read, not silently treated as not-found`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()

        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.read(id("../escaped"))
        }
    }

    @Test
    fun `reading an identifier that was never written returns null`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()

        assertNull(storage.read(id("never-written")))
    }

    // --- Correction 1: canonical (lowercase-only) identifiers ---

    @Test
    fun `a lowercase identifier is accepted`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()

        storage.write(id("lowercase-artifact-1"), "content".toByteArray())

        assertContentEquals("content".toByteArray(), storage.read(id("lowercase-artifact-1")))
    }

    @Test
    fun `an identifier containing uppercase characters is rejected, not normalised`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()

        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.write(id("ARTIFACT-1"), "content".toByteArray())
        }

        // Confirms rejection, not silent lowercasing: if "ARTIFACT-1" had been quietly folded to
        // "artifact-1" and stored, this read would succeed. It must not.
        assertNull(storage.read(id("artifact-1")))
    }

    @Test
    fun `two identifiers differing only by case cannot both enter storage -- the non-canonical one is rejected outright`() =
        runTest {
            val storage = InMemoryEvidenceArtifactStorage()

            storage.write(id("artifact-1"), "lowercase-content".toByteArray())

            assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
                storage.write(id("ARTIFACT-1"), "uppercase-content".toByteArray())
            }

            assertContentEquals("lowercase-content".toByteArray(), storage.read(id("artifact-1")))
        }

    @Test
    fun `rejection of a non-canonical identifier leaves nothing stored under any form of it`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()

        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.write(id("Uppercase-Artifact"), "content".toByteArray())
        }

        assertNull(storage.read(id("uppercase-artifact")))
    }

    // --- Correction 2: Windows reserved device names ---

    @Test
    fun `each base Windows reserved device name is rejected`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()

        listOf("con", "prn", "aux", "nul").forEach { reserved ->
            assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier>("'$reserved' should be rejected") {
                storage.write(id(reserved), "content".toByteArray())
            }
        }
    }

    @Test
    fun `representative numbered reserved device names are rejected`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()

        listOf("com1", "com9", "lpt1", "lpt9").forEach { reserved ->
            assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier>("'$reserved' should be rejected") {
                storage.write(id(reserved), "content".toByteArray())
            }
        }
    }

    @Test
    fun `near-matches of reserved device names remain valid, ordinary identifiers`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val nearMatches = listOf("console", "auxiliary", "com10", "lpt10", "my_con_file")

        nearMatches.forEach { value ->
            storage.write(id(value), "content-$value".toByteArray())
            assertContentEquals(
                "content-$value".toByteArray(),
                storage.read(id(value)),
                "near-match identifier '$value' should have been accepted as an ordinary identifier",
            )
        }
    }

    @Test
    fun `concurrent writes to the same identifier -- exactly one succeeds, content is never mixed`() = runBlocking {
        val storage = InMemoryEvidenceArtifactStorage()
        val attempts = 20

        val outcomes = List(attempts) { index ->
            async(Dispatchers.Default) {
                runCatching { storage.write(id(), "writer-$index".toByteArray()) }
            }
        }.awaitAll()

        val successCount = outcomes.count { it.isSuccess }
        assertEquals(1, successCount, "exactly one concurrent writer should succeed")

        val stored = storage.read(id())
        assertTrue(
            stored != null && String(stored).matches(Regex("writer-\\d+")),
            "stored content must be exactly one writer's own content, never a mix",
        )
    }

    @Test
    fun `exact-byte round trip holds for empty, small, and binary content`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val cases = listOf(
            "empty" to ByteArray(0),
            "single-byte" to byteArrayOf(0x00),
            "binary" to byteArrayOf(0x00, 0x01.toByte(), 0xFF.toByte(), 0x7F, 0x80.toByte()),
        )

        cases.forEach { (label, content) ->
            val artifactId = id("artifact-$label")
            storage.write(artifactId, content)
            assertContentEquals(content, storage.read(artifactId), "round trip failed for case '$label'")
        }
    }

    // --- Byte ownership: no shared in-memory reference ---

    @Test
    fun `mutating the caller's array after write does not affect what is stored`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val content = "original".toByteArray()

        storage.write(id(), content)
        content[0] = 'X'.code.toByte()

        assertContentEquals("original".toByteArray(), storage.read(id()))
    }

    @Test
    fun `mutating a previously returned array does not affect a later read`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        storage.write(id(), "original".toByteArray())

        val firstRead = storage.read(id())!!
        firstRead[0] = 'X'.code.toByte()

        val secondRead = storage.read(id())
        assertContentEquals("original".toByteArray(), secondRead)
        assertNotSame(firstRead, secondRead)
    }
}
