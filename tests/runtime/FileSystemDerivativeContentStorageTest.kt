package parker.core.runtime

import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.DerivativeContentEntry
import parker.core.interfaces.DerivativeContentStorageException
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierADerivativePayloadFixtures

/**
 * Document Ingestion — Derivative Content Persistence and Retrieval.
 * Governed by `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`
 * §4/§9/§10. Mirrors [FileSystemDerivativeGenerationStorageTest]'s own
 * established lifecycle coverage exactly for the independent content store:
 * write-once/no-overwrite, hidden-until-published staging, corruption
 * detection, unsafe-identifier rejection, and durability across restart.
 */
class FileSystemDerivativeContentStorageTest {
    @TempDir
    lateinit var directory: Path

    private fun entry(id: String = "generation-1") = DerivativeContentEntry(
        DerivativeGenerationId(id),
        EvidenceArtifactId("source-1"),
        TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()),
    )

    @Test
    fun `published content is durable and reloadable`() = runTest {
        val original = entry()
        val storage = FileSystemDerivativeContentStorage(directory)
        storage.prepare(original)
        storage.publishPrepared(original.derivativeGenerationId)
        assertEquals(original, FileSystemDerivativeContentStorage(directory).retrieve(original.derivativeGenerationId))
    }

    @Test
    fun `unknown generation retrieval returns null, never a fabricated entry`() = runTest {
        assertNull(FileSystemDerivativeContentStorage(directory).retrieve(DerivativeGenerationId("never-written")))
    }

    @Test
    fun `duplicate write for an already-published identifier is rejected without overwrite -- same content`() = runTest {
        val storage = FileSystemDerivativeContentStorage(directory)
        val original = entry()
        storage.prepare(original)
        storage.publishPrepared(original.derivativeGenerationId)
        assertFailsWith<DerivativeContentStorageException.DuplicateIdentifier> { storage.prepare(original) }
        assertEquals(original, storage.retrieve(original.derivativeGenerationId))
    }

    @Test
    fun `duplicate write for an already-published identifier is rejected without overwrite -- different content`() = runTest {
        val storage = FileSystemDerivativeContentStorage(directory)
        val original = entry()
        storage.prepare(original)
        storage.publishPrepared(original.derivativeGenerationId)
        val different = DerivativeContentEntry(
            original.derivativeGenerationId,
            original.rootSourceEvidenceArtifactId,
            TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf("a different document entirely")),
        )
        assertFailsWith<DerivativeContentStorageException.DuplicateIdentifier> { storage.prepare(different) }
        assertEquals(original, storage.retrieve(original.derivativeGenerationId))
    }

    @Test
    fun `prepared content is hidden until publication and survives restart while hidden`() = runTest {
        val original = entry()
        FileSystemDerivativeContentStorage(directory).prepare(original)
        assertNull(FileSystemDerivativeContentStorage(directory).retrieve(original.derivativeGenerationId))
        val restarted = FileSystemDerivativeContentStorage(directory)
        restarted.publishPrepared(original.derivativeGenerationId)
        assertEquals(original, FileSystemDerivativeContentStorage(directory).retrieve(original.derivativeGenerationId))
    }

    @Test
    fun `a byte-corrupted stored file is detected and never returned as valid`() = runTest {
        val id = DerivativeGenerationId("generation-corrupt")
        directory.resolve("${id.value}.content").writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        assertFailsWith<DerivativeContentStorageException.CorruptContent> {
            FileSystemDerivativeContentStorage(directory).retrieve(id)
        }
    }

    @Test
    fun `truncated stored file is detected and never partially returned`() = runTest {
        val original = entry("generation-truncate")
        val storage = FileSystemDerivativeContentStorage(directory)
        storage.prepare(original)
        storage.publishPrepared(original.derivativeGenerationId)
        val target = directory.resolve("generation-truncate.content")
        val bytes = Files.readAllBytes(target)
        Files.write(target, bytes.copyOfRange(0, bytes.size - 10))
        assertFailsWith<DerivativeContentStorageException.CorruptContent> {
            FileSystemDerivativeContentStorage(directory).retrieve(original.derivativeGenerationId)
        }
    }

    @Test
    fun `path-significant opaque identity cannot escape the storage root`() = runTest {
        val hostile = DerivativeContentEntry(
            DerivativeGenerationId("../escape"),
            EvidenceArtifactId("source-1"),
            TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()),
        )
        assertFailsWith<DerivativeContentStorageException.UnsafeIdentifier> {
            FileSystemDerivativeContentStorage(directory).prepare(hostile)
        }
        assertEquals(false, Files.exists(directory.parent.resolve("escape.content")))
    }

    @Test
    fun `a reserved device-style name is rejected as unsafe rather than reaching the filesystem`() = runTest {
        val hostile = DerivativeContentEntry(
            DerivativeGenerationId("con"),
            EvidenceArtifactId("source-1"),
            TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()),
        )
        assertFailsWith<DerivativeContentStorageException.UnsafeIdentifier> {
            FileSystemDerivativeContentStorage(directory).prepare(hostile)
        }
    }

    @Test
    fun `nonexistent storage root is rejected rather than silently created deep`() = runTest {
        val missing = directory.resolve("does-not-exist")
        assertFailsWith<DerivativeContentStorageException.InvalidStorageRoot> { FileSystemDerivativeContentStorage(missing) }
        assertEquals(false, Files.exists(missing))
    }

    @Test
    fun `concurrent duplicate publication yields exactly one durable entry without overwrite`() = runTest {
        val storage = FileSystemDerivativeContentStorage(directory)
        val id = DerivativeGenerationId("generation-concurrent")
        val a = DerivativeContentEntry(id, EvidenceArtifactId("source-1"), TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()))
        val b = DerivativeContentEntry(id, EvidenceArtifactId("source-1"), TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf()))
        val outcomes = listOf(a, b).map { candidate -> async { runCatching { storage.prepare(candidate) } } }.map { it.await() }
        assertEquals(1, outcomes.count { it.isSuccess })
        assertEquals(1, outcomes.count { it.exceptionOrNull() is DerivativeContentStorageException.DuplicateIdentifier })
        storage.publishPrepared(id)
        assertNotNull(storage.retrieve(id))
    }

    @Test
    fun `content stored under one generation is independently retrievable from content stored under a different generation`() = runTest {
        val storage = FileSystemDerivativeContentStorage(directory)
        val first = entry("generation-first")
        val second = DerivativeContentEntry(
            DerivativeGenerationId("generation-second"),
            EvidenceArtifactId("source-1"),
            TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf("second, distinct document")),
        )
        storage.prepare(first); storage.publishPrepared(first.derivativeGenerationId)
        storage.prepare(second); storage.publishPrepared(second.derivativeGenerationId)
        assertEquals(first, storage.retrieve(first.derivativeGenerationId))
        assertEquals(second, storage.retrieve(second.derivativeGenerationId))
    }
}
