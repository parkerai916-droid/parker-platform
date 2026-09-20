package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import parker.core.interfaces.HermesProcessingServiceV1Limits
import parker.core.interfaces.HermesV1FailureCategory
import parker.core.interfaces.HermesV1FailureDetailCode

class HermesProcessingServiceV1SourceReceiptTest {
    @Test
    fun `matching multi-chunk source is verified and stored in a service-controlled handle`() {
        val bytes = ByteArray(100_000) { index -> (index * 31).toByte() }
        val workspace = Files.createTempDirectory("hermes-unit4-workspace-")
        val receiver = HermesProcessingServiceV1SourceReceiptReceiver(workspace)
        val outcome = receiver.receive(ByteArrayInputStream(frame(bytes)))
        val verified = assertIs<HermesV1SourceReceiptOutcome.Verified>(outcome).source
        try {
            assertEquals(bytes.size.toLong(), verified.verifiedSizeBytes)
            assertEquals(sha256(bytes), verified.calculatedSha256.value)
            assertEquals(verified.expectedSha256, verified.calculatedSha256)
            assertTrue(verified.handle.path.startsWith(workspace.toAbsolutePath().normalize()))
            assertFalse(verified.handle.path.fileName.toString().contains("record.txt"))
            verified.handle.open().use { assertContentEquals(bytes, it.readBytes()) }
        } finally {
            verified.handle.delete()
        }
        assertWorkspaceEmpty(workspace)
    }

    @Test
    fun `digest changes fail closed with non-retryable integrity failure`() {
        val original = "source bytes".toByteArray()
        val changed = "source bytez".toByteArray()
        val workspace = Files.createTempDirectory("hermes-unit4-mismatch-")
        val outcome = HermesProcessingServiceV1SourceReceiptReceiver(workspace).receive(
            ByteArrayInputStream(frame(changed, expectedSha256 = sha256(original))),
        )
        val failed = assertIs<HermesV1SourceReceiptOutcome.Failed>(outcome).failure
        assertEquals(HermesV1FailureCategory.INTEGRITY, failed.category)
        assertEquals(HermesV1FailureDetailCode.SOURCE_HASH_MISMATCH, failed.detailCode)
        assertEquals(false, failed.retryable)
        assertWorkspaceEmpty(workspace)
    }

    @Test
    fun `zero-byte source is verified with canonical empty digest`() {
        val workspace = Files.createTempDirectory("hermes-unit4-empty-")
        val outcome = HermesProcessingServiceV1SourceReceiptReceiver(workspace).receive(
            ByteArrayInputStream(frame(ByteArray(0))),
        )
        val verified = assertIs<HermesV1SourceReceiptOutcome.Verified>(outcome).source
        try {
            assertEquals(0L, verified.verifiedSizeBytes)
            assertEquals(sha256(ByteArray(0)), verified.calculatedSha256.value)
            assertEquals(64, verified.calculatedSha256.value.length)
            assertTrue(verified.calculatedSha256.value == verified.calculatedSha256.value.lowercase())
        } finally {
            verified.handle.delete()
        }
        assertWorkspaceEmpty(workspace)
    }

    @Test
    fun `chunk boundaries do not alter digest and receipt uses bounded reads`() {
        val bytes = ByteArray(70_000) { it.toByte() }
        val input = ChunkedInputStream(frame(bytes), maximumRead = 7)
        val workspace = Files.createTempDirectory("hermes-unit4-chunks-")
        val outcome = HermesProcessingServiceV1SourceReceiptReceiver(workspace).receive(input)
        val verified = assertIs<HermesV1SourceReceiptOutcome.Verified>(outcome).source
        try {
            assertEquals(sha256(bytes), verified.calculatedSha256.value)
            assertTrue(input.maximumRequestedRead <= 16 * 1024)
        } finally {
            verified.handle.delete()
        }
        assertWorkspaceEmpty(workspace)
    }

    @Test
    fun `short source and trailing data preserve framing failures`() {
        val bytes = "short source".toByteArray()
        val shortWorkspace = Files.createTempDirectory("hermes-unit4-short-")
        val short = HermesProcessingServiceV1SourceReceiptReceiver(shortWorkspace).receive(
            ByteArrayInputStream(frame(bytes.copyOf(bytes.size - 1), declaredSize = bytes.size.toLong(), expectedSha256 = sha256(bytes))),
        )
        val shortFailure = assertIs<HermesV1SourceReceiptOutcome.Failed>(short).failure
        assertEquals(HermesV1FailureDetailCode.SHORT_SOURCE_READ, shortFailure.detailCode)
        assertEquals(true, shortFailure.retryable)
        assertWorkspaceEmpty(shortWorkspace)

        val trailingWorkspace = Files.createTempDirectory("hermes-unit4-trailing-")
        val trailing = HermesProcessingServiceV1SourceReceiptReceiver(trailingWorkspace).receive(
            ByteArrayInputStream(frame(bytes) + byteArrayOf(0x7f)),
        )
        val trailingFailure = assertIs<HermesV1SourceReceiptOutcome.Failed>(trailing).failure
        assertEquals(HermesV1FailureDetailCode.TRAILING_DATA, trailingFailure.detailCode)
        assertEquals(HermesV1FailureCategory.MALFORMED, trailingFailure.category)
        assertWorkspaceEmpty(trailingWorkspace)
    }

    @Test
    fun `oversized source is rejected before source receipt work`() {
        val workspace = Files.createTempDirectory("hermes-unit4-oversized-")
        val metadata = metadata(
            declaredSize = HermesProcessingServiceV1Limits.MAX_SOURCE_BYTES + 1,
            expectedSha256 = "a".repeat(64),
        )
        val bytes = metadata.toByteArray(StandardCharsets.UTF_8)
        val frame = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bytes.size).array() + bytes
        val outcome = HermesProcessingServiceV1SourceReceiptReceiver(workspace).receive(ByteArrayInputStream(frame))
        val failed = assertIs<HermesV1SourceReceiptOutcome.Failed>(outcome).failure
        assertEquals(HermesV1FailureDetailCode.SOURCE_TOO_LARGE, failed.detailCode)
        assertWorkspaceEmpty(workspace)
    }

    @Test
    fun `metadata filename remains metadata and cannot control workspace path`() {
        val filenames = listOf("../../etc/passwd", "..\\..\\file", "/absolute/path")
        filenames.forEachIndexed { index, filename ->
            val workspace = Files.createTempDirectory("hermes-unit4-path-$index-")
            val metadata = metadata(
                filename = filename,
                declaredSize = 0,
                expectedSha256 = sha256(ByteArray(0)),
            )
            val metadataBytes = metadata.toByteArray(StandardCharsets.UTF_8)
            val framed = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(metadataBytes.size).array() + metadataBytes
            val outcome = HermesProcessingServiceV1SourceReceiptReceiver(workspace).receive(ByteArrayInputStream(framed))
            val failed = assertIs<HermesV1SourceReceiptOutcome.Failed>(outcome).failure
            assertEquals(HermesV1FailureDetailCode.INVALID_FIELD, failed.detailCode)
            assertWorkspaceEmpty(workspace)
        }
    }

    private fun frame(
        source: ByteArray,
        declaredSize: Long = source.size.toLong(),
        expectedSha256: String = sha256(source),
    ): ByteArray {
        val metadataBytes = metadata(declaredSize = declaredSize, expectedSha256 = expectedSha256)
            .toByteArray(StandardCharsets.UTF_8)
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(metadataBytes.size).array() + metadataBytes + source
    }

    private fun metadata(
        filename: String = "record.txt",
        declaredSize: Long,
        expectedSha256: String,
    ): String = """
        {"protocolVersion":"1","requestId":"request-1","jobId":"job-1","occurrenceId":"occurrence-1","batchId":"batch-1","source":{"reference":"source-1","sha256":"$expectedSha256","sizeBytes":$declaredSize,"originalFilename":"${filename.replace("\\", "\\\\").replace("\"", "\\\"")}","mediaType":"text/plain"}}
    """.trimIndent()

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
        "%02x".format(it.toInt() and 0xff)
    }

    private fun assertWorkspaceEmpty(workspace: Path) {
        Files.list(workspace).use { assertEquals(0L, it.count()) }
    }

    private class ChunkedInputStream(bytes: ByteArray, private val maximumRead: Int) : ByteArrayInputStream(bytes) {
        var maximumRequestedRead: Int = 0

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            maximumRequestedRead = maxOf(maximumRequestedRead, length)
            return super.read(buffer, offset, minOf(length, maximumRead))
        }
    }
}
