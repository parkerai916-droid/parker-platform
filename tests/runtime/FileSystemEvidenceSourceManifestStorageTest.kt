package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceSourceManifest
import parker.core.interfaces.EvidenceSourceManifestStorageException

/**
 * Document Ingestion, Authoritative Source Manifest Foundation
 * Implementation. Mirrors [FileSystemDerivativeGenerationStorageTest]'s
 * own coverage shape, adapted to this storage's single-step (no
 * prepare/publish) write, since a manifest carries no analogous
 * atomic-visibility requirement the Derivative Generation admission/audit
 * boundary has (Scope Lock Section 15 -- bytes-first sequencing is
 * satisfied by [DefaultEvidenceCustodian] itself calling this storage
 * only after `EvidenceArtifactStorage.write` already succeeded).
 */
class FileSystemEvidenceSourceManifestStorageTest {
    @TempDir
    lateinit var directory: Path

    private val id = EvidenceArtifactId("evidence-1")
    private val validSha256 = "c".repeat(64)

    private fun manifest(id: EvidenceArtifactId = this.id) =
        EvidenceSourceManifest(id, validSha256, 128L, "application/pdf", "report.pdf")

    @Test
    fun `a written manifest is durable and reloadable after storage re-instantiation`() = runTest {
        FileSystemEvidenceSourceManifestStorage(directory).write(manifest())

        val reloaded = FileSystemEvidenceSourceManifestStorage(directory).read(id)

        assertEquals(manifest(), reloaded)
        assertNotNull(reloaded)
    }

    @Test
    fun `duplicate identity is rejected without overwrite`() = runTest {
        val storage = FileSystemEvidenceSourceManifestStorage(directory)
        storage.write(manifest())

        assertFailsWith<EvidenceSourceManifestStorageException.DuplicateIdentifier> {
            storage.write(manifest().copy(byteLength = 999L))
        }
        assertEquals(manifest(), storage.read(id))
    }

    @Test
    fun `a genuinely absent manifest reads back as null`() = runTest {
        val storage = FileSystemEvidenceSourceManifestStorage(directory)

        assertNull(storage.read(EvidenceArtifactId("never-written")))
    }

    @Test
    fun `truncated file content fails explicitly, never repaired into an apparent valid manifest`() = runTest {
        directory.resolve("${id.value}.manifest").writeBytes(byteArrayOf(1, 2, 3))

        assertFailsWith<EvidenceSourceManifestStorageException.CorruptRecord> {
            FileSystemEvidenceSourceManifestStorage(directory).read(id)
        }
    }

    @Test
    fun `a manifest file whose stored identity does not match its filename identity fails explicitly`() = runTest {
        val storage = FileSystemEvidenceSourceManifestStorage(directory)
        storage.write(manifest(EvidenceArtifactId("actual-identity")))
        Files.move(
            directory.resolve("actual-identity.manifest"),
            directory.resolve("${id.value}.manifest"),
        )

        assertFailsWith<EvidenceSourceManifestStorageException.CorruptRecord> {
            storage.read(id)
        }
    }

    @Test
    fun `an invalid storage root fails construction, never silently creating one`() {
        val missing = directory.resolve("missing")

        assertFailsWith<EvidenceSourceManifestStorageException.InvalidStorageRoot> {
            FileSystemEvidenceSourceManifestStorage(missing)
        }
        assertEquals(false, Files.exists(missing))
    }

    @Test
    fun `a path-traversal-shaped identity is rejected, never escaping the storage root`() = runTest {
        val hostileId = EvidenceArtifactId("../escape")

        assertFailsWith<EvidenceSourceManifestStorageException.UnsafeIdentifier> {
            FileSystemEvidenceSourceManifestStorage(directory).write(manifest(hostileId))
        }
        assertEquals(false, Files.exists(directory.parent.resolve("escape.manifest")))
    }

    @Test
    fun `a leftover temporary file is never treated as a valid manifest`() = runTest {
        val storage = FileSystemEvidenceSourceManifestStorage(directory)
        Files.write(directory.resolve(".tmp").resolve("evidence-source-manifest-orphan.tmp"), byteArrayOf(1, 2, 3))

        assertNull(storage.read(EvidenceArtifactId("evidence-source-manifest-orphan")))
    }

    @Test
    fun `concurrent duplicate writes yield exactly one durable manifest without overwrite`() = runTest {
        val storage = FileSystemEvidenceSourceManifestStorage(directory)
        val original = manifest()
        val replacement = original.copy(byteLength = 999L)

        val outcomes = listOf(original, replacement).map { candidate ->
            async { runCatching { storage.write(candidate) } }
        }.map { it.await() }

        assertEquals(1, outcomes.count { it.isSuccess })
        assertEquals(1, outcomes.count { it.exceptionOrNull() is EvidenceSourceManifestStorageException.DuplicateIdentifier })
        val stored = storage.read(id)
        assertEquals(true, stored == original || stored == replacement)
    }

    @Test
    fun `deleting an existing manifest removes it with no tombstone`() = runTest {
        val storage = FileSystemEvidenceSourceManifestStorage(directory)
        storage.write(manifest())

        val deleted = storage.delete(id)

        assertEquals(true, deleted)
        assertNull(storage.read(id))
        assertEquals(false, Files.exists(directory.resolve("${id.value}.manifest")))
    }

    @Test
    fun `deleting an absent manifest is an ordinary false, not an error`() = runTest {
        val storage = FileSystemEvidenceSourceManifestStorage(directory)

        assertEquals(false, storage.delete(EvidenceArtifactId("never-written")))
    }

    // --- Codec ---

    @Test
    fun `codec valid manifest round trips deterministically`() {
        val encoded = EvidenceSourceManifestCodec.encode(manifest())
        assertEquals(manifest(), EvidenceSourceManifestCodec.decode(encoded))
        assertContentEquals(encoded, EvidenceSourceManifestCodec.encode(manifest()))
    }

    @Test
    fun `codec round trips a manifest with both optional fields absent`() {
        val minimal = EvidenceSourceManifest(id, validSha256, 0L)
        assertEquals(minimal, EvidenceSourceManifestCodec.decode(EvidenceSourceManifestCodec.encode(minimal)))
    }

    @Test
    fun `codec rejects truncation, trailing data, and invalid version`() {
        val original = EvidenceSourceManifestCodec.encode(manifest())
        assertFailsWith<Exception> { EvidenceSourceManifestCodec.decode(original.dropLast(1).toByteArray()) }
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifestCodec.decode(original + byteArrayOf(1)) }

        val invalidVersion = original.copyOf()
        ByteBuffer.wrap(invalidVersion, 4, 4).putInt(99)
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifestCodec.decode(invalidVersion) }

        val invalidMagic = original.copyOf()
        ByteBuffer.wrap(invalidMagic, 0, 4).putInt(0)
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifestCodec.decode(invalidMagic) }
    }

    @Test
    fun `codec rejects hostile string lengths and malformed UTF-8`() {
        listOf(-1, 1024 * 1024 + 1).forEach { hostileLength ->
            val encoded = EvidenceSourceManifestCodec.encode(manifest())
            ByteBuffer.wrap(encoded, 8, 4).putInt(hostileLength)
            assertFailsWith<IllegalArgumentException> { EvidenceSourceManifestCodec.decode(encoded) }
        }

        val malformedUtf8 = EvidenceSourceManifestCodec.encode(manifest())
        ByteBuffer.wrap(malformedUtf8, 8, 4).putInt(1)
        malformedUtf8[12] = 0x80.toByte()
        assertFailsWith<Exception> { EvidenceSourceManifestCodec.decode(malformedUtf8) }
    }

    @Test
    fun `codec preserves Unicode filename metadata exactly`() {
        val withUnicode = manifest().copy(originalFileName = "légal-évidence-🔎.pdf")
        assertEquals(withUnicode, EvidenceSourceManifestCodec.decode(EvidenceSourceManifestCodec.encode(withUnicode)))
    }
}
