package parker.core.runtime

import java.nio.file.Files
import java.nio.ByteBuffer
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationStorageException
import parker.core.interfaces.DerivativeGenerationTest
import java.nio.file.Path

class FileSystemDerivativeGenerationStorageTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `admitted record is durable and reloadable`() = runTest {
        val record = DerivativeGenerationTest.record()
        FileSystemDerivativeGenerationStorage(directory).admit(record)
        val reloaded = FileSystemDerivativeGenerationStorage(directory).retrieve(record.derivativeGenerationId)
        assertEquals(record, reloaded)
        assertNotNull(reloaded)
    }

    @Test
    fun `duplicate identity is rejected without overwrite`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory)
        val original = DerivativeGenerationTest.record()
        storage.admit(original)
        assertFailsWith<DerivativeGenerationStorageException.DuplicateIdentifier> {
            storage.admit(original.copy(derivativeKind = "replacement"))
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
            FileSystemDerivativeGenerationStorage(directory).admit(record)
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
            async { runCatching { storage.admit(candidate) } }
        }.map { it.await() }
        assertEquals(1, outcomes.count { it.isSuccess })
        assertEquals(1, outcomes.count { it.exceptionOrNull() is DerivativeGenerationStorageException.DuplicateIdentifier })
        val stored = storage.retrieve(original.derivativeGenerationId)
        assertEquals(true, stored == original || stored == replacement)
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
}
