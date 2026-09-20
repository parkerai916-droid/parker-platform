package parker.core.interfaces

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HermesProcessingServiceV1FramingTest {
    private val sourceBytes = "hello Hermes".toByteArray()
    private val metadata = """
        {"protocolVersion":"1","requestId":"request-1","jobId":"job-1","occurrenceId":"occurrence-1","batchId":"batch-1","source":{"reference":"source-1","sha256":"${"a".repeat(64)}","sizeBytes":${sourceBytes.size},"originalFilename":"record.txt","mediaType":"text/plain"}}
    """.trimIndent()

    private fun frame(metadataText: String = metadata, source: ByteArray = sourceBytes): ByteArray {
        val bytes = metadataText.toByteArray(StandardCharsets.UTF_8)
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bytes.size).array() + bytes + source
    }

    private fun parse(frame: ByteArray, sink: ByteArrayOutputStream = ByteArrayOutputStream()): HermesProcessingServiceV1Request {
        return HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(frame), sink)
    }

    @Test
    fun `valid v1 frame accepts metadata and exact source`() {
        val sink = ByteArrayOutputStream()
        val request = HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(frame()), sink)
        assertEquals("1", request.protocolVersion.value)
        assertContentEquals(sourceBytes, sink.toByteArray())
        val encoded = ByteArrayOutputStream()
        HermesProcessingServiceV1Framing.writeRequestFrame(encoded, request, ByteArrayInputStream(sourceBytes))
        val roundTripSink = ByteArrayOutputStream()
        HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(encoded.toByteArray()), roundTripSink)
        assertContentEquals(sourceBytes, roundTripSink.toByteArray())
    }

    @Test
    fun `metadata length framing rejects zero, short, and over-limit lengths`() {
        assertFailsWith<FramingException> {
            HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(ByteArray(4)), ByteArrayOutputStream())
        }.assertCode(HermesV1FailureDetailCode.MALFORMED_FRAME)
        assertFailsWith<FramingException> {
            HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(byteArrayOf(0, 0)), ByteArrayOutputStream())
        }.assertCode(HermesV1FailureDetailCode.SHORT_METADATA_LENGTH_READ)
        val tooLarge = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
            .putInt((HermesProcessingServiceV1Limits.MAX_METADATA_ENVELOPE_BYTES + 1).toInt()).array()
        assertFailsWith<FramingException> {
            HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(tooLarge), ByteArrayOutputStream())
        }.assertCode(HermesV1FailureDetailCode.ENVELOPE_TOO_LARGE)
    }

    @Test
    fun `exact metadata envelope boundary is accepted and short metadata is rejected`() {
        val padding = " ".repeat(HermesProcessingServiceV1Limits.MAX_METADATA_ENVELOPE_BYTES.toInt() - metadata.toByteArray().size)
        val boundary = frame(metadata + padding)
        HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(boundary), ByteArrayOutputStream())
        val bytes = metadata.toByteArray()
        val short = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bytes.size + 1).array() + bytes
        assertFailsWith<FramingException> {
            HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(short), ByteArrayOutputStream())
        }.assertCode(HermesV1FailureDetailCode.SHORT_METADATA_READ)
    }

    @Test
    fun `invalid utf8 and malformed json are rejected`() {
        val invalidUtf8 = byteArrayOf(0xC3.toByte(), 0x28)
        val invalidFrame = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(invalidUtf8.size).array() + invalidUtf8
        assertFailsWith<FramingException> {
            HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(invalidFrame), ByteArrayOutputStream())
        }.assertCode(HermesV1FailureDetailCode.INVALID_UTF8)
        assertFailsWith<FramingException> {
            HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(frame("{")), ByteArrayOutputStream())
        }.assertCode(HermesV1FailureDetailCode.MALFORMED_JSON)
        assertFailsWith<FramingException> {
            HermesProcessingServiceV1Framing.decodeMetadata("[]".toByteArray())
        }.assertCode(HermesV1FailureDetailCode.INVALID_FIELD)
    }

    @Test
    fun `metadata schema rejects missing, duplicate, case, unsupported, and invalid fields`() {
        assertFailsWith<FramingException> { parse(frame(metadata.replace("\"jobId\":\"job-1\",", ""))) }
            .assertCode(HermesV1FailureDetailCode.INVALID_FIELD)
        assertFailsWith<FramingException> { parse(frame(metadata.replace("{\"protocolVersion\":", "{\"caseId\":\"case-1\",\"protocolVersion\":"))) }
            .assertCode(HermesV1FailureDetailCode.UNKNOWN_FIELD)
        assertFailsWith<FramingException> { parse(frame(metadata.replace("\"protocolVersion\":\"1\"", "\"protocolVersion\":\"2\""))) }
            .assertCode(HermesV1FailureDetailCode.UNSUPPORTED_PROTOCOL_VERSION)
        assertFailsWith<FramingException> { parse(frame(metadata.replace("\"mediaType\":\"text/plain\"", "\"mediaType\":\"text\""))) }
            .assertCode(HermesV1FailureDetailCode.INVALID_FIELD)
        val duplicate = metadata.replace("\"jobId\":\"job-1\"", "\"jobId\":\"job-1\",\"jobId\":\"job-2\"")
        assertFailsWith<FramingException> { parse(frame(duplicate)) }.assertCode(HermesV1FailureDetailCode.DUPLICATE_FIELD)
    }

    @Test
    fun `source stream requires exact declared count and rejects trailing data`() {
        val sink = ByteArrayOutputStream()
        HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(frame(source = sourceBytes)), sink)
        val short = frame(source = sourceBytes.copyOf(sourceBytes.size - 1))
        assertFailsWith<FramingException> {
            HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(short), ByteArrayOutputStream())
        }.assertCode(HermesV1FailureDetailCode.SHORT_SOURCE_READ)
        assertFailsWith<FramingException> {
            HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(frame() + byteArrayOf(1)), ByteArrayOutputStream())
        }.assertCode(HermesV1FailureDetailCode.TRAILING_DATA)
        val zeroSource = metadata.replace("\"sizeBytes\":${sourceBytes.size}", "\"sizeBytes\":0")
        HermesProcessingServiceV1Framing.readRequestFrame(ByteArrayInputStream(frame(zeroSource, byteArrayOf())), ByteArrayOutputStream())
    }

    @Test
    fun `source limit and bounded chunk copy are enforced`() {
        val oversized = metadata.replace("\"sizeBytes\":${sourceBytes.size}", "\"sizeBytes\":${HermesProcessingServiceV1Limits.MAX_SOURCE_BYTES + 1}")
        assertFailsWith<FramingException> { parse(frame(oversized)) }.assertCode(HermesV1FailureDetailCode.SOURCE_TOO_LARGE)
        val negative = metadata.replace("\"sizeBytes\":${sourceBytes.size}", "\"sizeBytes\":-1")
        assertFailsWith<FramingException> { parse(frame(negative)) }.assertCode(HermesV1FailureDetailCode.INVALID_SOURCE_LENGTH)
        val tracking = TrackingInputStream(frame())
        HermesProcessingServiceV1Framing.readRequestFrame(tracking, ByteArrayOutputStream())
        assertEquals(true, tracking.maxRequestedSourceRead <= HermesProcessingServiceV1Framing.SOURCE_COPY_BUFFER_BYTES)
    }

    @Test
    fun `request serialization is deterministic and response limit is fail closed`() {
        val request = HermesProcessingServiceV1Framing.decodeMetadata(metadata.toByteArray())
        val canonical = HermesProcessingServiceV1Framing.encodeMetadata(request)
        assertContentEquals(canonical, HermesProcessingServiceV1Framing.encodeMetadata(request))
        assertEquals(request, HermesProcessingServiceV1Framing.decodeMetadata(canonical))
        val response = response()
        assertContentEquals(
            HermesProcessingServiceV1Framing.encodeResponseFrame(response),
            HermesProcessingServiceV1Framing.encodeResponseFrame(response),
        )
        assertFailsWith<FramingException> {
            HermesProcessingServiceV1Framing.frameResponseBody(ByteArray(HermesProcessingServiceV1Limits.MAX_INLINE_RESPONSE_BYTES.toInt() + 1))
        }.assertCode(HermesV1FailureDetailCode.RESPONSE_TOO_LARGE)
    }

    private fun response(): HermesProcessingServiceV1Response {
        val sha = "a".repeat(64)
        val operation = HermesV1MethodProvenance(
            HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:01Z"),
        )
        return HermesProcessingServiceV1Response(
            HermesV1ProtocolVersion.CURRENT,
            HermesV1RequestId("request-1"),
            HermesV1JobId("job-1"),
            HermesV1OccurrenceId("occurrence-1"),
            HermesV1BatchId("batch-1"),
            HermesV1Sha256(sha),
            HermesProcessingResult(sha, "batch-1", HermesProcessingStatus.PASS, setOf(HermesProcessingMethod.DIRECT_TEXT_EXTRACTION)),
            listOf(HermesV1RepresentationDescriptor(HermesV1RepresentationId("rep-1"), HermesV1RepresentationType.TEXT, HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION)),
            emptyList(),
            null,
            HermesV1Provenance(HermesV1Sha256(sha), HermesV1ProcessorIdentity("hermes-processing", "test"), listOf(operation)),
        )
    }

    private fun FramingException.assertCode(expected: HermesV1FailureDetailCode) = assertEquals(expected, failure.detailCode)

    private class TrackingInputStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var maxRequestedSourceRead = 0
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            maxRequestedSourceRead = maxOf(maxRequestedSourceRead, length)
            return super.read(buffer, offset, length)
        }
    }
}
