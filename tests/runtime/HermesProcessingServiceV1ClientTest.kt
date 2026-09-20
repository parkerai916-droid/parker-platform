package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingFailureKind
import parker.core.interfaces.HermesProcessingMethod
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingServiceV1Framing
import parker.core.interfaces.HermesProcessingServiceV1Request
import parker.core.interfaces.HermesProcessingServiceV1Response
import parker.core.interfaces.HermesProcessingServiceV1ResponseSerializer
import parker.core.interfaces.HermesProcessingStatus
import parker.core.interfaces.HermesStructuredRepresentation
import parker.core.interfaces.HermesV1BatchId
import parker.core.interfaces.HermesV1Failure
import parker.core.interfaces.HermesV1FailureCategory
import parker.core.interfaces.HermesV1FailureDetailCode
import parker.core.interfaces.HermesV1JobId
import parker.core.interfaces.HermesV1MethodProvenance
import parker.core.interfaces.HermesV1OccurrenceId
import parker.core.interfaces.HermesV1OriginalFilename
import parker.core.interfaces.HermesV1ProcessingMethod
import parker.core.interfaces.HermesV1ProcessorIdentity
import parker.core.interfaces.HermesV1Provenance
import parker.core.interfaces.HermesV1RepresentationDescriptor
import parker.core.interfaces.HermesV1RepresentationId
import parker.core.interfaces.HermesV1RepresentationType
import parker.core.interfaces.HermesV1RequestId
import parker.core.interfaces.HermesV1Sha256
import parker.core.interfaces.HermesV1MediaType
import parker.core.interfaces.HermesV1SourceReference
import parker.core.interfaces.HermesV1SourceSize

class HermesProcessingServiceV1ClientTest {
    private val sha = HermesV1Sha256("a".repeat(64))
    private val sourceBytes = "governed source".toByteArray()

    private fun prepared(path: Path, digest: HermesV1Sha256 = HermesV1Sha256(sha256(sourceBytes)), method: HermesV1ProcessingMethod = HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION) = HermesV1PreparedSource(
        requestId = HermesV1RequestId("request-stable-1"), jobId = HermesV1JobId("job-1"), occurrenceId = HermesV1OccurrenceId("occurrence-1"), batchId = HermesV1BatchId("batch-1"),
        sourceReference = HermesV1SourceReference("source-correlation-1"), sourcePath = path, sourceSha256 = digest, sizeBytes = HermesV1SourceSize(sourceBytes.size.toLong()),
        originalFilename = HermesV1OriginalFilename("record.txt"), mediaType = HermesV1MediaType("text/plain"), requestedMethods = listOf(method),
    )

    @Test
    fun `valid request is preverified and emitted with exact Unit 2 framing`() {
        val path = source(sourceBytes)
        var capturedRequest: HermesProcessingServiceV1Request? = null
        var capturedFrame = ByteArray(0)
        val transport = HermesV1RequestTransport { request, input ->
            capturedRequest = request
            val source = ByteArrayOutputStream()
            input.use { it.copyTo(source, HermesProcessingServiceV1Framing.SOURCE_COPY_BUFFER_BYTES) }
            val frame = ByteArrayOutputStream()
            HermesProcessingServiceV1Framing.writeRequestFrame(frame, request, ByteArrayInputStream(source.toByteArray()))
            capturedFrame = frame.toByteArray()
            HermesV1TransportOutcome.Response(responseFrame(request))
        }
        val outcome = HermesProcessingServiceV1Client(transport).process(prepared(path))
        assertIs<HermesV1ClientOutcome.Accepted>(outcome)
        assertEquals("request-stable-1", capturedRequest?.requestId?.value)
        assertFalse(capturedFrame.toString(StandardCharsets.UTF_8).contains(path.toString()))
        assertFalse(capturedFrame.toString(StandardCharsets.UTF_8).contains("caseId"))
        assertTrue(capturedFrame.size > sourceBytes.size + 4)
        Files.delete(path)
    }

    @Test
    fun `changed source fails before transport`() {
        val changed = sourceBytes.copyOf().also { it[0] = 'X'.code.toByte() }
        val path = source(changed)
        var invoked = false
        val transport = HermesV1RequestTransport { _, _ -> invoked = true; HermesV1TransportOutcome.Response(ByteArray(0)) }
        val outcome = HermesProcessingServiceV1Client(transport).process(prepared(path))
        val rejected = assertIs<HermesV1ClientOutcome.Rejected>(outcome)
        assertEquals(HermesV1FailureDetailCode.SOURCE_HASH_MISMATCH, rejected.failure.detailCode)
        assertFalse(invoked)
        Files.delete(path)
    }

    @Test
    fun `unsupported audio cannot be routed through native processing`() {
        val path = source(sourceBytes)
        var invoked = false
        val transport = HermesV1RequestTransport { _, _ -> invoked = true; HermesV1TransportOutcome.Response(ByteArray(0)) }
        val audio = prepared(path).copy(mediaType = HermesV1MediaType("audio/wav"))
        val rejected = assertIs<HermesV1ClientOutcome.Rejected>(HermesProcessingServiceV1Client(transport).process(audio))
        assertEquals(HermesV1FailureDetailCode.UNSUPPORTED_MEDIA_TYPE, rejected.failure.detailCode)
        assertFalse(invoked)
        Files.delete(path)
    }

    @Test
    fun `stable request identity is retained across repeated attempts`() {
        val path = source(sourceBytes)
        val requests = mutableListOf<HermesProcessingServiceV1Request>()
        val transport = HermesV1RequestTransport { request, _ -> requests += request; HermesV1TransportOutcome.Response(responseFrame(request)) }
        val client = HermesProcessingServiceV1Client(transport)
        client.process(prepared(path)); client.process(prepared(path))
        assertEquals(listOf("request-stable-1", "request-stable-1"), requests.map { it.requestId.value })
        assertEquals(requests[0], requests[1])
        Files.delete(path)
    }

    @Test
    fun `valid PASS and valid FAILED Hermes responses are accepted as application results`() {
        val path = source(sourceBytes)
        val responses = listOf(false, true).map { failed -> responseFrame(requestFor(prepared(path)), failed) }
        var index = 0
        val transport = HermesV1RequestTransport { _, _ -> HermesV1TransportOutcome.Response(responses[index++]) }
        val client = HermesProcessingServiceV1Client(transport)
        assertEquals(HermesProcessingStatus.PASS, assertIs<HermesV1ClientOutcome.Accepted>(client.process(prepared(path))).response.establishedResult.status)
        assertEquals(HermesProcessingStatus.FAILED, assertIs<HermesV1ClientOutcome.Accepted>(client.process(prepared(path))).response.establishedResult.status)
        Files.delete(path)
    }

    @Test
    fun `response identity mismatch and malformed framing fail closed`() {
        val path = source(sourceBytes)
        val transport = HermesV1RequestTransport { request, _ ->
            val mismatched = response(request, false).copy(requestId = HermesV1RequestId("other-request"))
            HermesV1TransportOutcome.Response(HermesProcessingServiceV1Framing.frameResponseBody(HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(mismatched)))
        }
        val mismatch = assertIs<HermesV1ClientOutcome.Rejected>(HermesProcessingServiceV1Client(transport).process(prepared(path)))
        assertEquals(HermesV1FailureDetailCode.INVALID_FIELD, mismatch.failure.detailCode)
        val malformed = HermesProcessingServiceV1Client(HermesV1RequestTransport { _, _ -> HermesV1TransportOutcome.Response(byteArrayOf(0, 0, 0, 8, '{'.code.toByte())) }).process(prepared(path))
        assertIs<HermesV1ClientOutcome.Rejected>(malformed)
        Files.delete(path)
    }

    @Test
    fun `SSH command requires dedicated key seam and strict host verification`() {
        var command: List<String>? = null
        val transport = HermesV1SshRequestTransport(Path.of("/service/processing-key"), Path.of("/service/known_hosts")) { args ->
            command = args
            throw IllegalStateException("test launch stop")
        }
        val result = transport.invoke(requestFor(prepared(source(sourceBytes))), ByteArrayInputStream(sourceBytes))
        assertIs<HermesV1TransportOutcome.Failed>(result)
        assertNotNull(command)
        assertTrue(command!!.contains("-o"))
        assertTrue(command!!.contains("IdentitiesOnly=yes"))
        assertTrue(command!!.contains("BatchMode=yes"))
        assertTrue(command!!.contains("StrictHostKeyChecking=yes"))
        assertTrue(command!!.contains("UserKnownHostsFile=/service/known_hosts"))
        assertTrue(command!!.contains("-i"))
        assertTrue(command!!.contains("/service/processing-key"))
        assertFalse(command!!.any { it.contains("caseId") || it.contains("record.txt") })
    }

    private fun requestFor(prepared: HermesV1PreparedSource) = HermesProcessingServiceV1Request(
        parker.core.interfaces.HermesV1ProtocolVersion.CURRENT, prepared.requestId, prepared.jobId, prepared.occurrenceId, prepared.batchId,
        parker.core.interfaces.HermesProcessingServiceV1Source(prepared.sourceReference, prepared.sourceSha256, prepared.sizeBytes, prepared.originalFilename, prepared.mediaType), prepared.requestedMethods,
    )

    private fun responseFrame(request: HermesProcessingServiceV1Request, failed: Boolean = false) =
        HermesProcessingServiceV1Framing.frameResponseBody(HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(response(request, failed)))

    private fun response(request: HermesProcessingServiceV1Request, failed: Boolean): HermesProcessingServiceV1Response {
        val method = request.requestedMethods.single()
        val result = HermesProcessingResult(request.source.sourceSha256.value, request.batchId.value, if (failed) HermesProcessingStatus.FAILED else HermesProcessingStatus.PASS, setOf(method.establishedMethod), failure = if (failed) HermesProcessingFailure(HermesProcessingFailureKind.PROCESSOR_FAILURE, "processor failed") else null, structuredRepresentation = if (failed) null else HermesStructuredRepresentation.Text("processed"))
        val failure = if (failed) HermesV1Failure(HermesV1FailureCategory.PROCESSOR, HermesV1FailureDetailCode.PROCESSOR_FAILED, false, "processor failed") else null
        val start = Instant.parse("2026-01-01T00:00:00.000Z")
        val end = Instant.parse("2026-01-01T00:00:01.000Z")
        return HermesProcessingServiceV1Response(request.protocolVersion, request.requestId, request.jobId, request.occurrenceId, request.batchId, request.source.sourceSha256, result, listOf(HermesV1RepresentationDescriptor(HermesV1RepresentationId("rep-1"), HermesV1RepresentationType.TEXT, method, result.structuredRepresentation)), emptyList(), failure, HermesV1Provenance(request.source.sourceSha256, HermesV1ProcessorIdentity("hermes-test", "1"), listOf(HermesV1MethodProvenance(method, start, end))))
    }

    private fun source(bytes: ByteArray): Path = Files.createTempFile("hermes-v1-client", ".source").also { Files.write(it, bytes) }
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
