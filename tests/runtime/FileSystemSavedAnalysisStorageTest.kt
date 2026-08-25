package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.SavedAnalysisEvidenceReference
import parker.core.interfaces.SavedAnalysisId
import parker.core.interfaces.SavedAnalysisRecord
import parker.core.interfaces.SavedAnalysisStorageException

/**
 * Reviewed Analysis Result — Explicit Owner Save. Behavioural tests for
 * [FileSystemSavedAnalysisStorage], mirroring
 * [FileSystemDerivativeGenerationStorageTest]'s own established style:
 * durability/reload, write-once (no overwrite), corrupt/unsupported-version
 * fail-closed handling, unsafe-identifier rejection, and bounded,
 * newest-first listing.
 */
class FileSystemSavedAnalysisStorageTest {

    private fun storage() = FileSystemSavedAnalysisStorage(Files.createTempDirectory("saved-analysis-storage-test"))

    private fun record(id: String = "record-1", savedAt: Instant = Instant.parse("2026-01-01T00:00:00Z")) = SavedAnalysisRecord(
        savedAnalysisId = SavedAnalysisId(id),
        savedAt = savedAt,
        analysedAt = Instant.parse("2025-12-31T23:00:00Z"),
        instruction = "Summarise the document",
        analysisText = "The document discusses X.",
        evidenceReferences = listOf(SavedAnalysisEvidenceReference(EvidenceArtifactId("evidence-1"), DerivativeGenerationId("gen-1"), "Searchable PDF literal text")),
        mechanismIdentity = null,
        mechanismVersion = null,
    )

    private suspend fun admit(storage: FileSystemSavedAnalysisStorage, saved: SavedAnalysisRecord) {
        storage.prepare(saved)
        storage.publishPrepared(saved.savedAnalysisId)
    }

    @Test
    fun `A a saved record is durable and reloadable with exact field equality`() = runTest {
        val storage = storage()
        val saved = record()
        admit(storage, saved)

        val retrieved = storage.retrieve(saved.savedAnalysisId)

        assertEquals(saved, retrieved)
    }

    @Test
    fun `B a duplicate identity is rejected, never overwriting the existing record`() = runTest {
        val storage = storage()
        val saved = record()
        admit(storage, saved)

        assertFailsWith<SavedAnalysisStorageException.DuplicateIdentifier> { storage.prepare(saved) }
    }

    @Test
    fun `retrieving an unknown id returns null, never a fabricated record`() = runTest {
        val storage = storage()
        assertNull(storage.retrieve(SavedAnalysisId("never-admitted")))
    }

    @Test
    fun `corrupt stored bytes fail closed with CorruptRecord, never a partially-decoded result`() = runTest {
        val root = Files.createTempDirectory("saved-analysis-storage-corrupt")
        val storage = FileSystemSavedAnalysisStorage(root)
        val saved = record()
        admit(storage, saved)
        Files.write(root.resolve("${saved.savedAnalysisId.value}.saved-analysis"), byteArrayOf(1, 2, 3, 4))

        assertFailsWith<SavedAnalysisStorageException.CorruptRecord> { storage.retrieve(saved.savedAnalysisId) }
    }

    @Test
    fun `an unsupported representation version fails closed distinctly, never treated as ordinary corruption`() = runTest {
        val root = Files.createTempDirectory("saved-analysis-storage-version")
        val storage = FileSystemSavedAnalysisStorage(root)
        val saved = record()
        admit(storage, saved)
        val validBytes = SavedAnalysisRecordCodec.encode(saved)
        // Byte 4 (after the 4-byte magic) is the version int's high byte -- corrupt just the
        // version field to an unsupported value, leaving everything else byte-identical.
        val tampered = validBytes.copyOf()
        tampered[7] = 99
        Files.write(root.resolve("${saved.savedAnalysisId.value}.saved-analysis"), tampered)

        assertFailsWith<SavedAnalysisStorageException.UnsupportedRepresentationVersion> { storage.retrieve(saved.savedAnalysisId) }
    }

    @Test
    fun `path-traversal-shaped and reserved-device-name-shaped identifiers are rejected, never touching the filesystem outside storage root`() = runTest {
        val storage = storage()
        assertFailsWith<SavedAnalysisStorageException.UnsafeIdentifier> { storage.retrieve(SavedAnalysisId("../../../etc/passwd")) }
        assertFailsWith<SavedAnalysisStorageException.UnsafeIdentifier> { storage.retrieve(SavedAnalysisId("con")) }
    }

    @Test
    fun `listRecentIds returns newest-saved-first, capped at maxCount`() = runTest {
        val storage = storage()
        admit(storage, record("record-a"))
        Thread.sleep(20) // ensure distinct filesystem mtimes for a deterministic newest-first order
        admit(storage, record("record-b"))
        Thread.sleep(20)
        admit(storage, record("record-c"))

        val ids = storage.listRecentIds(2)

        assertEquals(listOf(SavedAnalysisId("record-c"), SavedAnalysisId("record-b")), ids)
    }

    @Test
    fun `listRecentIds on an empty store returns an empty list`() = runTest {
        val storage = storage()
        assertEquals(emptyList(), storage.listRecentIds(10))
    }
}
