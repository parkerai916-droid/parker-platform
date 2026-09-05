package parker.core.runtime

import java.nio.file.Files
import java.nio.ByteBuffer
import java.time.Instant
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationStorageException
import parker.core.interfaces.DerivativeGenerationTest
import parker.core.interfaces.DerivativeParentReference
import parker.core.interfaces.DerivativeProducerIdentity
import parker.core.interfaces.DerivativeTransformation
import parker.core.interfaces.EvidenceArtifactId
import java.nio.file.Path

class FileSystemDerivativeGenerationStorageTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `admitted record is durable and reloadable`() = runTest {
        val record = DerivativeGenerationTest.record()
        FileSystemDerivativeGenerationStorage(directory).prepare(record)
        FileSystemDerivativeGenerationStorage(directory).publishPrepared(record.derivativeGenerationId)
        val reloaded = FileSystemDerivativeGenerationStorage(directory).retrieve(record.derivativeGenerationId)
        assertEquals(record, reloaded)
        assertNotNull(reloaded)
    }

    @Test
    fun `duplicate identity is rejected without overwrite`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val original = DerivativeGenerationTest.record()
        storage.prepare(original)
        storage.publishPrepared(original.derivativeGenerationId)
        assertFailsWith<DerivativeGenerationStorageException.DuplicateIdentifier> {
            storage.prepare(original.copy(derivativeKind = "replacement"))
        }
        assertEquals(original, storage.retrieve(original.derivativeGenerationId))
    }

    @Test
    fun `partial or corrupt final file is never returned as a valid record`() = runTest {
        val id = DerivativeGenerationId("generation-corrupt")
        directory.resolve("${id.value}.derivative").writeBytes(byteArrayOf(1, 2, 3))
        assertFailsWith<DerivativeGenerationStorageException.CorruptRecord> {
            FileSystemDerivativeGenerationStorage(directory).retrieve(id)
        }
    }

    @Test
    fun `storage failure is surfaced and cannot return success`() = runTest {
        val missing = directory.resolve("missing")
        assertFailsWith<DerivativeGenerationStorageException.InvalidStorageRoot> {
            FileSystemDerivativeGenerationStorage(missing)
        }
        assertEquals(false, Files.exists(missing))
    }

    @Test
    fun `path-significant opaque identity cannot escape storage root`() = runTest {
        val id = DerivativeGenerationId("../escape")
        val record = DerivativeGenerationTest.record().copy(derivativeGenerationId = id)
        assertFailsWith<DerivativeGenerationStorageException.UnsafeIdentifier> {
            FileSystemDerivativeGenerationStorage(directory).prepare(record)
        }
        assertEquals(false, Files.exists(directory.parent.resolve("escape.derivative")))
    }

    @Test
    fun `temporary artifact is never treated as admitted`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        Files.write(directory.resolve(".tmp").resolve("generation-temp.derivative"), byteArrayOf(1, 2, 3))
        assertEquals(null, storage.retrieve(DerivativeGenerationId("generation-temp")))
    }

    @Test
    fun `concurrent duplicate admission yields one durable record without overwrite`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val original = DerivativeGenerationTest.record()
        val replacement = original.copy(derivativeKind = "replacement")
        val outcomes = listOf(original, replacement).map { candidate ->
            async { runCatching { storage.prepare(candidate) } }
        }.map { it.await() }
        assertEquals(1, outcomes.count { it.isSuccess })
        assertEquals(1, outcomes.count { it.exceptionOrNull() is DerivativeGenerationStorageException.DuplicateIdentifier })
        storage.publishPrepared(original.derivativeGenerationId)
        val stored = storage.retrieve(original.derivativeGenerationId)
        assertEquals(true, stored == original || stored == replacement)
    }

    @Test
    fun `durable prepared record is hidden until atomic publication`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val record = DerivativeGenerationTest.record()
        storage.prepare(record)
        assertEquals(null, storage.retrieve(record.derivativeGenerationId))
        assertEquals(true, Files.exists(directory.resolve(".prepared").resolve("generation-1.derivative")))
        storage.publishPrepared(record.derivativeGenerationId)
        assertEquals(record, storage.retrieve(record.derivativeGenerationId))
        assertEquals(false, Files.exists(directory.resolve(".prepared").resolve("generation-1.derivative")))
    }

    @Test
    fun `prepared visibility remains hidden across restart until publication`() = runTest {
        val record = DerivativeGenerationTest.record()
        FileSystemDerivativeGenerationStorage(directory).prepare(record)
        val restarted = FileSystemDerivativeGenerationStorage(directory)
        assertEquals(null, restarted.retrieve(record.derivativeGenerationId))
        restarted.publishPrepared(record.derivativeGenerationId)
        assertEquals(record, FileSystemDerivativeGenerationStorage(directory).retrieve(record.derivativeGenerationId))
    }

    @Test
    fun `corrupt prepared bytes cannot be published into admitted namespace`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val record = DerivativeGenerationTest.record()
        storage.prepare(record)
        directory.resolve(".prepared").resolve("generation-1.derivative").writeBytes(byteArrayOf(1, 2, 3))
        assertFailsWith<DerivativeGenerationStorageException.CorruptRecord> {
            storage.publishPrepared(record.derivativeGenerationId)
        }
        assertEquals(null, storage.retrieve(record.derivativeGenerationId))
    }

    @Test
    fun `codec valid record round trips deterministically`() {
        val record = DerivativeGenerationTest.record()
        val first = DerivativeGenerationRecordCodec.encode(record)
        assertEquals(record, DerivativeGenerationRecordCodec.decode(first))
        assertContentEquals(first, DerivativeGenerationRecordCodec.encode(record))
    }

    @Test
    fun `codec rejects truncation trailing data and invalid version`() {
        val original = DerivativeGenerationRecordCodec.encode(DerivativeGenerationTest.record())
        assertFailsWith<Exception> { DerivativeGenerationRecordCodec.decode(original.dropLast(1).toByteArray()) }
        assertFailsWith<IllegalArgumentException> { DerivativeGenerationRecordCodec.decode(original + byteArrayOf(1)) }
        val invalidVersion = original.copyOf()
        ByteBuffer.wrap(invalidVersion, 4, 4).putInt(99)
        assertFailsWith<IllegalArgumentException> { DerivativeGenerationRecordCodec.decode(invalidVersion) }
    }

    @Test
    fun `codec rejects hostile lengths malformed UTF-8 and invalid tag`() {
        val record = DerivativeGenerationTest.record()
        listOf(-1, 1024 * 1024 + 1).forEach { length ->
            val encoded = DerivativeGenerationRecordCodec.encode(record)
            ByteBuffer.wrap(encoded, 8, 4).putInt(length)
            assertFailsWith<IllegalArgumentException> { DerivativeGenerationRecordCodec.decode(encoded) }
        }
        val malformedUtf8 = DerivativeGenerationRecordCodec.encode(record)
        ByteBuffer.wrap(malformedUtf8, 8, 4).putInt(1)
        malformedUtf8[12] = 0x80.toByte()
        assertFailsWith<Exception> { DerivativeGenerationRecordCodec.decode(malformedUtf8) }

        val invalidTag = DerivativeGenerationRecordCodec.encode(record)
        val idSize = record.derivativeGenerationId.value.toByteArray().size
        val rootSize = record.rootSourceEvidenceArtifactId.value.toByteArray().size
        val tagOffset = 8 + 4 + idSize + 4 + rootSize + 4
        invalidTag[tagOffset] = 99
        assertFailsWith<IllegalStateException> { DerivativeGenerationRecordCodec.decode(invalidTag) }
    }

    // ================= UI-INGESTION-8B — Exact-Evidence OCR Derivative Generation Discovery =================
    // Governed by DOCUMENT_INGESTION_TIER_B_OCR_EXACT_EVIDENCE_DERIVATIVE_GENERATION_DISCOVERY_SCOPE_LOCK_AMENDMENT.md.

    private fun ocrRecord(
        id: String,
        evidenceId: String = "source-1",
        generatedAt: Instant = Instant.parse("2026-08-23T00:00:00Z"),
    ) = DerivativeGenerationTest.record(id).copy(
        rootSourceEvidenceArtifactId = EvidenceArtifactId(evidenceId),
        parents = listOf(DerivativeParentReference.RootEvidenceArtifact(EvidenceArtifactId(evidenceId))),
        derivativeKind = "OCR recognised text",
        producerIdentity = DerivativeProducerIdentity(
            pluginIdentity = "test-parser", pluginVersion = "1.0", configurationIdentity = "test-config-v1",
            modelIdentity = "test-model", modelVersion = "1.0",
        ),
        transformationHistory = listOf(DerivativeTransformation.OCR, DerivativeTransformation.MODEL_INFERENCE),
        generatedAt = generatedAt,
    )

    private suspend fun admit(storage: FileSystemDerivativeGenerationStorage, record: parker.core.interfaces.DerivativeGenerationRecord) {
        storage.prepare(record)
        storage.publishPrepared(record.derivativeGenerationId)
    }

    @Test
    fun `known evidence with zero admitted OCR derivatives discovers an empty list`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        assertEquals(emptyList(), storage.findOcrGenerationsForEvidence(EvidenceArtifactId("source-1")))
    }

    @Test
    fun `one admitted OCR derivative is discovered`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val record = ocrRecord("generation-a")
        admit(storage, record)
        val discovered = storage.findOcrGenerationsForEvidence(EvidenceArtifactId("source-1"))
        assertEquals(listOf(record), discovered)
    }

    @Test
    fun `multiple admitted OCR derivatives for the same evidence are all discovered -- neither overwrites nor hides the other`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val first = ocrRecord("6d8d9307-8281-4574-a050-f9fec1c916f1", generatedAt = Instant.parse("2026-09-05T03:48:00Z"))
        val second = ocrRecord("4c8ed1e2-7524-467c-b4b3-32e8293c7854", generatedAt = Instant.parse("2026-09-05T03:50:00Z"))
        admit(storage, first)
        admit(storage, second)
        val discovered = storage.findOcrGenerationsForEvidence(EvidenceArtifactId("source-1"))
        assertEquals(2, discovered.size)
        assertTrue(first in discovered)
        assertTrue(second in discovered)
    }

    @Test
    fun `discovery orders by generatedAt descending then derivativeGenerationId descending`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val older = ocrRecord("generation-b", generatedAt = Instant.parse("2026-09-05T03:48:00Z"))
        val newer = ocrRecord("generation-a", generatedAt = Instant.parse("2026-09-05T03:50:00Z"))
        val sameInstantLower = ocrRecord("generation-c", generatedAt = Instant.parse("2026-09-05T03:50:00Z"))
        admit(storage, older)
        admit(storage, newer)
        admit(storage, sameInstantLower)
        val discovered = storage.findOcrGenerationsForEvidence(EvidenceArtifactId("source-1"))
        // Same instant as `newer` -- tie-broken by derivativeGenerationId descending ("generation-c" > "generation-a").
        assertEquals(
            listOf(sameInstantLower.derivativeGenerationId, newer.derivativeGenerationId, older.derivativeGenerationId),
            discovered.map { it.derivativeGenerationId },
        )
    }

    @Test
    fun `a derivative belonging to another evidence artifact never appears in this evidence artifact's discovery result`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val mine = ocrRecord("generation-mine", evidenceId = "source-1")
        val theirs = ocrRecord("generation-theirs", evidenceId = "source-2")
        admit(storage, mine)
        admit(storage, theirs)
        val discovered = storage.findOcrGenerationsForEvidence(EvidenceArtifactId("source-1"))
        assertEquals(listOf(mine), discovered)
        val theirDiscovered = storage.findOcrGenerationsForEvidence(EvidenceArtifactId("source-2"))
        assertEquals(listOf(theirs), theirDiscovered)
    }

    @Test
    fun `a non-OCR derivative for the same evidence artifact never appears in OCR discovery`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val ocr = ocrRecord("generation-ocr")
        val nonOcr = DerivativeGenerationTest.record("generation-csv")
        admit(storage, ocr)
        admit(storage, nonOcr)
        val discovered = storage.findOcrGenerationsForEvidence(EvidenceArtifactId("source-1"))
        assertEquals(listOf(ocr), discovered)
    }

    @Test
    fun `discovery exposes no arbitrary or global enumeration -- only records rooted at the exact supplied artifact are ever returned`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        repeat(5) { i -> admit(storage, ocrRecord("generation-other-$i", evidenceId = "source-other-$i")) }
        val target = ocrRecord("generation-target", evidenceId = "source-target")
        admit(storage, target)
        val discovered = storage.findOcrGenerationsForEvidence(EvidenceArtifactId("source-target"))
        assertEquals(listOf(target), discovered)
    }

    @Test
    fun `an unrelated corrupt record does not abort discovery for the evidence artifact actually requested`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val valid = ocrRecord("generation-valid")
        admit(storage, valid)
        directory.resolve("generation-corrupt-unrelated.derivative").writeBytes(byteArrayOf(1, 2, 3))
        val discovered = storage.findOcrGenerationsForEvidence(EvidenceArtifactId("source-1"))
        assertEquals(listOf(valid), discovered)
    }

    @Test
    fun `a pre-existing generation admitted before this capability existed is discoverable using only durable state already present`() = runTest {
        // No ExternalTranscriptionOwnerAuthorization / authorization-store interaction of any kind
        // anywhere in this test -- discovery depends solely on the derivative-generation domain.
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val preExisting = ocrRecord("4c8ed1e2-7524-467c-b4b3-32e8293c7854")
        admit(storage, preExisting)
        val reopened = FileSystemDerivativeGenerationStorage(directory)
        val discovered = reopened.findOcrGenerationsForEvidence(EvidenceArtifactId("source-1"))
        assertEquals(listOf(preExisting), discovered)
    }
}
