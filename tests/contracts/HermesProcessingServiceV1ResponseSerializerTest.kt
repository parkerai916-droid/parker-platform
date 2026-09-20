package parker.core.interfaces

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HermesProcessingServiceV1ResponseSerializerTest {
    private val sha = HermesV1Sha256("a".repeat(64))

    private fun response(content: HermesStructuredRepresentation? = HermesStructuredRepresentation.Text("hello π")): HermesProcessingServiceV1Response {
        val method = HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION
        val result = HermesProcessingResult(
            sourceSha256 = sha.value,
            batchId = "batch-1",
            status = HermesProcessingStatus.PASS,
            methods = setOf(method.establishedMethod),
            structuredRepresentation = content,
        )
        val operation = HermesV1MethodProvenance(method, Instant.parse("2026-01-01T00:00:00.000Z"), Instant.parse("2026-01-01T00:00:01.000Z"))
        return HermesProcessingServiceV1Response(
            HermesV1ProtocolVersion.CURRENT,
            HermesV1RequestId("request-1"), HermesV1JobId("job-1"), HermesV1OccurrenceId("occurrence-1"), HermesV1BatchId("batch-1"), sha,
            result,
            listOf(HermesV1RepresentationDescriptor(HermesV1RepresentationId("rep-1"), HermesV1RepresentationType.TEXT, method, content)),
            emptyList(), null,
            HermesV1Provenance(sha, HermesV1ProcessorIdentity("hermes-processing", "1"), listOf(operation)),
        )
    }

    @Test
    fun `same logical response produces identical canonical UTF8 bytes and typed content`() {
        val first = HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(response())
        val second = HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(response())
        assertContentEquals(first, second)
        val json = first.toString(StandardCharsets.UTF_8)
        assertTrue(json.startsWith("{\"protocolVersion\":\"1\""))
        assertTrue(json.contains("\"content\":{\"text\":\"hello π\"}"))
        assertFalse(json.contains("caseId"))
        assertFalse(json.contains("analysis-ready"))
    }

    @Test
    fun `configuration digest is canonical and excludes timestamps`() {
        val fields = linkedMapOf("mediaType" to "text/plain", "method" to "DIRECT_TEXT_EXTRACTION")
        assertEquals(
            HermesProcessingServiceV1ResponseSerializer.configurationDigest(fields),
            HermesProcessingServiceV1ResponseSerializer.configurationDigest(fields.toSortedMap()),
        )
        assertTrue(
            HermesProcessingServiceV1ResponseSerializer.configurationDigest(fields) !=
                HermesProcessingServiceV1ResponseSerializer.configurationDigest(fields + ("provider" to "tika")),
        )
    }

    @Test
    fun `timestamps are UTC with exactly millisecond precision`() {
        val json = HermesProcessingServiceV1ResponseSerializer.canonicalJson(response())
        assertTrue(json.contains("2026-01-01T00:00:00.000Z"))
        assertTrue(json.contains("2026-01-01T00:00:01.000Z"))
        val invalid = response().copy(
            provenance = response().provenance.copy(
                operations = listOf(HermesV1MethodProvenance(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION, Instant.parse("2026-01-01T00:00:00.000001Z"), Instant.parse("2026-01-01T00:00:01.000Z"))),
            ),
        )
        assertFailsWith<FramingException> { HermesProcessingServiceV1ResponseSerializer.canonicalJson(invalid) }
    }

    @Test
    fun `response framing preserves exact canonical bytes`() {
        val body = HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(response())
        val framed = HermesProcessingServiceV1Framing.frameResponseBody(body)
        val declared = ByteBuffer.wrap(framed, 0, 4).order(ByteOrder.BIG_ENDIAN).int
        assertEquals(body.size, declared)
        assertContentEquals(body, framed.copyOfRange(4, framed.size))
    }

    @Test
    fun `oversized UTF8 response fails without truncation`() {
        val oversized = response(HermesStructuredRepresentation.Text("x".repeat(8 * 1024 * 1024)))
        val fallback = HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(oversized).toString(StandardCharsets.UTF_8)
        assertTrue(fallback.contains("\"status\":\"FAILED\""))
        assertTrue(fallback.contains("\"detailCode\":\"TEXT_TOO_LARGE\""))
        assertTrue(fallback.toByteArray(StandardCharsets.UTF_8).size <= HermesProcessingServiceV1Limits.MAX_INLINE_RESPONSE_BYTES)
    }

    @Test
    fun `failure serialization is stable and contains no stack trace`() {
        val failed = response(null).copy(
            establishedResult = response(null).establishedResult.copy(
                status = HermesProcessingStatus.FAILED,
                failure = HermesProcessingFailure(HermesProcessingFailureKind.PROCESSOR_FAILURE, "bounded processor failure"),
            ),
            failure = HermesV1Failure(HermesV1FailureCategory.PROCESSOR, HermesV1FailureDetailCode.PROCESSOR_FAILED, false, "bounded processor failure"),
        )
        val first = HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(failed)
        assertContentEquals(first, HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(failed))
        val json = first.toString(StandardCharsets.UTF_8)
        assertTrue(json.contains("\"retryable\":false"))
        assertFalse(json.contains("Exception"))
        assertFalse(json.contains("stackTrace"))
    }
}
