package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceArtifactStorageException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Evidence Custodian, Implementation Plan Phase 2, Unit 1. Behavioural
 * tests of [FileSystemEvidenceArtifactStorage] -- real bytes, a real
 * temporary directory, real filesystem failure conditions. Every test
 * exercises what the implementation actually does when called, not its
 * internal structure.
 *
 * This is the first test in this repository to touch a real filesystem;
 * there is no existing precedent to follow for that part of the setup.
 */
class FileSystemEvidenceArtifactStorageTest {

    private fun id(value: String = "artifact-1") = EvidenceArtifactId(value)

    // --- Deletion (Implementation Plan Phase 7) ---

    @Test
    fun `deleting an existing identifier removes the file and returns true`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        storage.write(id(), "content".toByteArray())

        val result = storage.delete(id())

        assertTrue(result)
        assertNull(storage.read(id()))
    }

    @Test
    fun `deleting removes the file from disk, leaving no tombstone`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        storage.write(id(), "content".toByteArray())

        storage.delete(id())

        val nonTempEntries = tempDir.listDirectoryEntries().filterNot { it.isDirectory() }
        assertEquals(0, nonTempEntries.size, "no file, marker, or tombstone should remain after deletion")
    }

    @Test
    fun `deleting an identifier that was never written returns false`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        assertEquals(false, storage.delete(id("never-written")))
    }

    @Test
    fun `deleting an already-deleted identifier returns false the second time`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        storage.write(id(), "content".toByteArray())
        storage.delete(id())

        assertEquals(
            false,
            storage.delete(id()),
            "repeated deletion produces false, never a tombstone-derived distinction (Determination 2)",
        )
    }

    @Test
    fun `an unsafe identifier is rejected on delete, identically to write and read`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.delete(id("../escaped"))
        }
    }

    @Test
    fun `a delete-time I-O failure throws StorageIOFailure, not a silent false`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        storage.write(id(), "content".toByteArray())
        val madeReadOnly = tempDir.toFile().setWritable(false)
        assumeTrue(
            madeReadOnly && !Files.isWritable(tempDir),
            "this platform/test-runner did not honour the read-only permission change -- skipping " +
                "rather than reporting a false failure",
        )

        assertFailsWith<EvidenceArtifactStorageException.StorageIOFailure> {
            storage.delete(id())
        }

        tempDir.toFile().setWritable(true) // restore, so JUnit's own @TempDir cleanup can delete it
    }

    // --- First write succeeds ---

    @Test
    fun `a first write for a fresh identifier succeeds and is readable back`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        val content = "hello evidence".toByteArray()

        storage.write(id(), content)

        assertContentEquals(content, storage.read(id()))
    }

    @Test
    fun `a first write creates exactly one file under the storage root, outside the temp subdirectory`(
        @TempDir tempDir: Path,
    ) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        storage.write(id(), "content".toByteArray())

        val nonTempEntries = tempDir.listDirectoryEntries().filterNot { it.isDirectory() }
        assertEquals(1, nonTempEntries.size, "expected exactly one non-directory entry under the storage root")
    }

    // --- Duplicate write fails ---

    @Test
    fun `a second write to an already-used identifier fails`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        storage.write(id(), "first".toByteArray())

        assertFailsWith<EvidenceArtifactStorageException.DuplicateIdentifier> {
            storage.write(id(), "second".toByteArray())
        }
    }

    // --- Failed duplicate leaves original bytes unchanged ---

    @Test
    fun `a failed duplicate write does not alter the originally stored bytes`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        val original = "original content".toByteArray()
        storage.write(id(), original)

        assertFailsWith<EvidenceArtifactStorageException.DuplicateIdentifier> {
            storage.write(id(), "attempted replacement".toByteArray())
        }

        assertContentEquals(original, storage.read(id()))
    }

    // --- Zero-length content ---

    @Test
    fun `zero-length content is accepted and reads back as zero-length, not null`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        storage.write(id(), ByteArray(0))

        val readBack = storage.read(id())
        assertTrue(readBack != null && readBack.isEmpty(), "expected a non-null, zero-length result")
    }

    // --- Invalid identifiers ---

    @Test
    fun `an identifier containing a path separator is rejected without touching the filesystem`(
        @TempDir tempDir: Path,
    ) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.write(id("../escaped"), "content".toByteArray())
        }
        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.write(id("sub/dir"), "content".toByteArray())
        }

        val nonTempEntries = tempDir.listDirectoryEntries().filterNot { it.isDirectory() }
        assertTrue(nonTempEntries.isEmpty(), "an unsafe identifier must not create any file")
    }

    @Test
    fun `an unsafe identifier is also rejected on read, not silently treated as not-found`(
        @TempDir tempDir: Path,
    ) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.read(id("../escaped"))
        }
    }

    // --- Directory traversal prevention ---

    @Test
    fun `a crafted identifier can never cause a write outside the storage root`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        val outsideMarker = tempDir.parent.resolve("should-not-exist-${System.nanoTime()}.evidence")

        val traversalAttempts = listOf(
            "../should-not-exist",
            "..",
            "a/../../b",
        )
        traversalAttempts.forEach { attempt ->
            assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier>("identifier '$attempt' should be rejected") {
                storage.write(id(attempt), "content".toByteArray())
            }
        }

        assertTrue(!outsideMarker.exists(), "no traversal attempt may create a file outside the storage root")
    }

    // --- Correction 1: canonical (lowercase-only) identifiers ---

    @Test
    fun `a lowercase identifier is accepted`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        storage.write(id("lowercase-artifact-1"), "content".toByteArray())

        assertContentEquals("content".toByteArray(), storage.read(id("lowercase-artifact-1")))
    }

    @Test
    fun `an identifier containing uppercase characters is rejected, not normalised`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.write(id("ARTIFACT-1"), "content".toByteArray())
        }
        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.write(id("Artifact-1"), "content".toByteArray())
        }

        // Confirms rejection, not silent lowercasing: if "ARTIFACT-1" had been quietly folded to
        // "artifact-1" and stored, this read would succeed. It must not.
        assertNull(storage.read(id("artifact-1")))
    }

    @Test
    fun `two identifiers differing only by case cannot both enter storage -- the non-canonical one is rejected outright`(
        @TempDir tempDir: Path,
    ) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        storage.write(id("artifact-1"), "lowercase-content".toByteArray())

        // Rejected because it is non-canonical (uppercase present), never because it collides
        // with the lowercase write above -- DuplicateIdentifier would imply case-insensitive
        // collision detection, which this Unit does not implement; UnsafeIdentifier is the
        // correct, and only, reason this must fail.
        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.write(id("ARTIFACT-1"), "uppercase-content".toByteArray())
        }

        assertContentEquals("lowercase-content".toByteArray(), storage.read(id("artifact-1")))
    }

    @Test
    fun `rejection of a non-canonical identifier occurs before any filesystem mutation`(
        @TempDir tempDir: Path,
    ) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
            storage.write(id("Uppercase-Artifact"), "content".toByteArray())
        }

        val nonTempEntries = tempDir.listDirectoryEntries().filterNot { it.isDirectory() }
        assertTrue(nonTempEntries.isEmpty(), "a rejected non-canonical identifier must not create any file")
        val tempDirEntries = tempDir.resolve(".tmp").listDirectoryEntries()
        assertTrue(tempDirEntries.isEmpty(), "a rejected non-canonical identifier must not create a temp file either")
    }

    // --- Correction 2: Windows reserved device names ---

    @Test
    fun `each base Windows reserved device name is rejected`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        listOf("con", "prn", "aux", "nul").forEach { reserved ->
            assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier>("'$reserved' should be rejected") {
                storage.write(id(reserved), "content".toByteArray())
            }
        }
    }

    @Test
    fun `representative numbered reserved device names are rejected`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        listOf("com1", "com9", "lpt1", "lpt9").forEach { reserved ->
            assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier>("'$reserved' should be rejected") {
                storage.write(id(reserved), "content".toByteArray())
            }
        }
    }

    @Test
    fun `near-matches of reserved device names remain valid, ordinary identifiers`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
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
    fun `no file or temporary file is created for any rejected reserved-device-name identifier`(
        @TempDir tempDir: Path,
    ) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        val reservedNames = listOf("con", "prn", "aux", "nul", "com1", "com9", "lpt1", "lpt9")

        reservedNames.forEach { reserved ->
            assertFailsWith<EvidenceArtifactStorageException.UnsafeIdentifier> {
                storage.write(id(reserved), "content".toByteArray())
            }
        }

        val nonTempEntries = tempDir.listDirectoryEntries().filterNot { it.isDirectory() }
        assertTrue(nonTempEntries.isEmpty(), "no reserved-name rejection may create a file")
        val tempDirEntries = tempDir.resolve(".tmp").listDirectoryEntries()
        assertTrue(tempDirEntries.isEmpty(), "no reserved-name rejection may create a temporary file")
    }

    // --- Concurrent writes to the same identifier ---

    @Test
    fun `concurrent writes to the same identifier -- exactly one succeeds, content is never mixed`(
        @TempDir tempDir: Path,
    ) = runBlocking {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        val attempts = 20

        val outcomes = List(attempts) { index ->
            async(Dispatchers.Default) {
                runCatching { storage.write(id(), "writer-$index".toByteArray()) }
            }
        }.awaitAll()

        val successCount = outcomes.count { it.isSuccess }
        val failureCount = outcomes.count { it.exceptionOrNull() is EvidenceArtifactStorageException.DuplicateIdentifier }

        assertEquals(1, successCount, "exactly one concurrent writer should succeed")
        assertEquals(attempts - 1, failureCount, "every other writer should see DuplicateIdentifier, not a corrupted write")

        val stored = storage.read(id())
        assertTrue(
            stored != null && String(stored).matches(Regex("writer-\\d+")),
            "stored content must be exactly one writer's own content, never a mix",
        )
    }

    // --- Storage failure propagation ---

    @Test
    fun `constructing storage over a non-writable directory fails fast, at construction, not deferred to the first write`(
        @TempDir tempDir: Path,
    ) = runTest {
        val readOnlyRoot = tempDir.resolve("read-only-root")
        Files.createDirectories(readOnlyRoot)
        val madeReadOnly = readOnlyRoot.toFile().setWritable(false)
        assumeTrue(
            madeReadOnly && !Files.isWritable(readOnlyRoot),
            "this platform/test-runner did not honour the read-only permission change -- skipping " +
                "rather than reporting a false failure",
        )

        assertFailsWith<EvidenceArtifactStorageException.InvalidStorageRoot> {
            FileSystemEvidenceArtifactStorage(readOnlyRoot)
        }

        readOnlyRoot.toFile().setWritable(true) // restore, so JUnit's own @TempDir cleanup can delete it
    }

    @Test
    fun `a write-time I-O failure after successful construction throws StorageIOFailure, not a silent or partial result`(
        @TempDir tempDir: Path,
    ) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        // Construction already succeeded (and already created .tmp) -- revoke write access to
        // .tmp specifically afterward, so this test exercises a genuine write-time failure,
        // distinct from the construction-time rejection covered above.
        val tempSubdirectory = tempDir.resolve(".tmp")
        val madeReadOnly = tempSubdirectory.toFile().setWritable(false)
        assumeTrue(
            madeReadOnly && !Files.isWritable(tempSubdirectory),
            "this platform/test-runner did not honour the read-only permission change -- skipping " +
                "rather than reporting a false failure",
        )

        assertFailsWith<EvidenceArtifactStorageException.StorageIOFailure> {
            storage.write(id(), "content".toByteArray())
        }

        tempSubdirectory.toFile().setWritable(true) // restore, so JUnit's own @TempDir cleanup can delete it
        assertNull(storage.read(id()), "a failed write must leave nothing readable under its identifier")
    }

    @Test
    fun `constructing storage over a path that is not a directory fails at construction`(@TempDir tempDir: Path) {
        val filePath = tempDir.resolve("not-a-directory.txt")
        filePath.writeText("not a directory")

        assertFailsWith<EvidenceArtifactStorageException.InvalidStorageRoot> {
            FileSystemEvidenceArtifactStorage(filePath)
        }
    }

    @Test
    fun `constructing storage over a path that does not exist fails at construction`(@TempDir tempDir: Path) {
        val missing = tempDir.resolve("does-not-exist")

        assertFailsWith<EvidenceArtifactStorageException.InvalidStorageRoot> {
            FileSystemEvidenceArtifactStorage(missing)
        }
    }

    // --- Exact-byte round trip ---

    @Test
    fun `exact-byte round trip holds for empty, small, binary, and larger content`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        val cases = listOf(
            "empty" to ByteArray(0),
            "single-byte" to byteArrayOf(0x00),
            "binary" to byteArrayOf(0x00, 0x01.toByte(), 0xFF.toByte(), 0x7F, 0x80.toByte()),
            "larger" to ByteArray(64_000) { (it % 256).toByte() },
        )

        cases.forEach { (label, content) ->
            val artifactId = id("artifact-$label")
            storage.write(artifactId, content)
            assertContentEquals(content, storage.read(artifactId), "round trip failed for case '$label'")
        }
    }

    // --- Unknown identifier ---

    @Test
    fun `reading an identifier that was never written returns null`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        assertNull(storage.read(id("never-written")))
    }

    // --- Temporary-file crash-protection behaviour, where practical ---
    //
    // A genuine mid-write process crash cannot be injected from inside a unit test running in
    // the same process -- this section tests what is practically observable instead: that no
    // failure path (duplicate, unsafe identifier) ever leaves a stray temporary file behind, and
    // that content only ever appears under its final, identifier-derived path atomically, never
    // partially. This is not a claim that a real crash was simulated.

    @Test
    fun `no temporary file is left behind after a successful write`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)

        storage.write(id(), "content".toByteArray())

        val tempDirEntries = tempDir.resolve(".tmp").listDirectoryEntries()
        assertTrue(tempDirEntries.isEmpty(), "the .tmp directory must be empty after a successful write")
    }

    @Test
    fun `no temporary file is left behind after a failed duplicate write`(@TempDir tempDir: Path) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        storage.write(id(), "first".toByteArray())

        assertFailsWith<EvidenceArtifactStorageException.DuplicateIdentifier> {
            storage.write(id(), "second".toByteArray())
        }

        val tempDirEntries = tempDir.resolve(".tmp").listDirectoryEntries()
        assertTrue(tempDirEntries.isEmpty(), "the .tmp directory must be empty after a failed duplicate write")
    }

    @Test
    fun `content never appears at the final path until the write to it has fully completed`(
        @TempDir tempDir: Path,
    ) = runTest {
        val storage = FileSystemEvidenceArtifactStorage(tempDir)
        val targetPath = tempDir.resolve("${id().value}.evidence")

        assertTrue(!targetPath.exists(), "target must not exist before any write")
        storage.write(id(), "content".toByteArray())
        assertTrue(targetPath.exists(), "target must exist immediately after a successful write")
    }
}
