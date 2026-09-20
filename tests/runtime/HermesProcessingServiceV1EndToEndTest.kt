package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
import parker.core.interfaces.HermesV1ProcessingPrincipal
import parker.core.interfaces.HermesV1ProcessorIdentity
import parker.core.interfaces.HermesV1Provenance
import parker.core.interfaces.HermesV1ProtocolVersion
import parker.core.interfaces.HermesV1RequestId
import parker.core.interfaces.HermesV1RepresentationDescriptor
import parker.core.interfaces.HermesV1RepresentationId
import parker.core.interfaces.HermesV1RepresentationType
import parker.core.interfaces.HermesV1Sha256
import parker.core.interfaces.HermesV1SourceReference
import parker.core.interfaces.HermesV1SourceSize
import parker.core.interfaces.HermesV1MediaType

class HermesProcessingServiceV1EndToEndTest {
    private val principal = HermesV1ProcessingPrincipal.PARKER_PROCESSING
    private val sourceBytes = "Hermes v1 end-to-end text".toByteArray()

    @Test
    fun `client server round trip authorizes verifies claims processes frames and replays byte identically`() {
        val root = Files.createTempDirectory("hermes-v1-e2e")
        val sourcePath = Files.createTempFile(root, "source-", ".txt").also { Files.write(it, sourceBytes) }
        val workspace = root.resolve("workspace")
        val ledgerPath = root.resolve("ledger")
        var executions = 0
        val processor = HermesV1VerifiedProcessor { _, verified ->
            executions++
            assertEquals(sha256(sourceBytes), verified.calculatedSha256.value)
            HermesV1NativeProcessingOutcome.Produced(response(verified.request))
        }
        fun service() = HermesProcessingServiceV1EndToEndService(
            parker.core.interfaces.HermesV1CapabilityPolicy.initial(),
            HermesProcessingServiceV1SourceReceiptReceiver(workspace),
            HermesProcessingServiceV1IdempotencyLedger(ledgerPath),
            processor,
        )
        fun client(service: HermesProcessingServiceV1EndToEndService) = HermesProcessingServiceV1Client(HermesV1RequestTransport { request, source ->
            val frame = ByteArrayOutputStream()
            HermesProcessingServiceV1Framing.writeRequestFrame(frame, request, source)
            runBlocking { service.handle(principal, ByteArrayInputStream(frame.toByteArray())) }
        })
        val prepared = prepared(sourcePath)
        val first = assertIs<HermesV1ClientOutcome.Accepted>(client(service()).process(prepared))
        assertEquals(1, executions)
        assertTrue(first.canonicalResponseBytes.isNotEmpty())
        assertEquals(0, Files.list(workspace).use { it.count() })

        val replay = assertIs<HermesV1ClientOutcome.Accepted>(client(service()).process(prepared))
        assertEquals(1, executions)
        assertContentEquals(first.canonicalResponseBytes, replay.canonicalResponseBytes)
        assertEquals(0, Files.list(workspace).use { it.count() })
        assertFalse(first.canonicalResponseBytes.toString(Charsets.UTF_8).contains("caseId"))
        assertFalse(first.canonicalResponseBytes.toString(Charsets.UTF_8).contains(sourcePath.toString()))
        deleteTree(root)
    }

    @Test
    fun `authorization denial precedes source receipt and processor execution`() {
        val root = Files.createTempDirectory("hermes-v1-auth")
        val path = Files.createTempFile(root, "source-", ".txt").also { Files.write(it, sourceBytes) }
        var executions = 0
        val service = HermesProcessingServiceV1EndToEndService(
            parker.core.interfaces.HermesV1CapabilityPolicy.empty(), HermesProcessingServiceV1SourceReceiptReceiver(root.resolve("workspace")), HermesProcessingServiceV1IdempotencyLedger(root.resolve("ledger")),
            HermesV1VerifiedProcessor { _, _ -> executions++; error("processor must not execute") },
        )
        val outcome = invoke(service, prepared(path), HermesV1ProcessingPrincipal("unknown-principal"))
        val accepted = assertIs<HermesV1ClientOutcome.Accepted>(outcome)
        assertEquals(HermesProcessingStatus.FAILED, accepted.response.establishedResult.status)
        assertEquals(HermesV1FailureDetailCode.POLICY_EMPTY, accepted.response.failure?.detailCode)
        assertEquals(0, executions)
        deleteTree(root)
    }

    @Test
    fun `server integrity failure does not invoke processor or claim success`() {
        val root = Files.createTempDirectory("hermes-v1-integrity")
        val path = Files.createTempFile(root, "source-", ".txt").also { Files.write(it, sourceBytes) }
        var executions = 0
        val service = HermesProcessingServiceV1EndToEndService(
            parker.core.interfaces.HermesV1CapabilityPolicy.initial(), HermesProcessingServiceV1SourceReceiptReceiver(root.resolve("workspace")), HermesProcessingServiceV1IdempotencyLedger(root.resolve("ledger")),
            HermesV1VerifiedProcessor { _, _ -> executions++; error("processor must not execute") },
        )
        val wrong = prepared(path).copy(sourceSha256 = HermesV1Sha256("b".repeat(64)))
        val request = request(wrong)
        val input = ByteArrayOutputStream().also { HermesProcessingServiceV1Framing.writeRequestFrame(it, request, ByteArrayInputStream(sourceBytes)) }
        val transport = runBlocking { service.handle(principal, ByteArrayInputStream(input.toByteArray())) }
        assertIs<HermesV1TransportOutcome.Response>(transport)
        assertEquals(0, executions)
        assertTrue((transport as HermesV1TransportOutcome.Response).framedBytes.toString(StandardCharsets.UTF_8).contains("SOURCE_HASH_MISMATCH"))
        deleteTree(root)
    }

    @Test
    fun `request identity conflict does not reprocess`() {
        val root = Files.createTempDirectory("hermes-v1-conflict")
        val path = Files.createTempFile(root, "source-", ".txt").also { Files.write(it, sourceBytes) }
        var executions = 0
        val service = HermesProcessingServiceV1EndToEndService(
            parker.core.interfaces.HermesV1CapabilityPolicy.initial(), HermesProcessingServiceV1SourceReceiptReceiver(root.resolve("workspace")), HermesProcessingServiceV1IdempotencyLedger(root.resolve("ledger")),
            HermesV1VerifiedProcessor { _, verified -> executions++; HermesV1NativeProcessingOutcome.Produced(response(verified.request)) },
        )
        val transport = HermesV1RequestTransport { request, source ->
            val frame = ByteArrayOutputStream(); HermesProcessingServiceV1Framing.writeRequestFrame(frame, request, source)
            runBlocking { service.handle(principal, ByteArrayInputStream(frame.toByteArray())) }
        }
        val client = HermesProcessingServiceV1Client(transport)
        assertIs<HermesV1ClientOutcome.Accepted>(client.process(prepared(path)))
        val conflict = assertIs<HermesV1ClientOutcome.Accepted>(client.process(prepared(path).copy(occurrenceId = HermesV1OccurrenceId("different-occurrence"))))
        assertEquals(HermesV1FailureDetailCode.REQUEST_IDENTITY_CONFLICT, conflict.response.failure?.detailCode)
        assertEquals(1, executions)
        deleteTree(root)
    }

    @Test
    fun `closed routing matrix rejects OCR transcription audio and image paths`() {
        assertEquals(setOf(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION), HermesV1ClientMethodSelection.forMediaType("text/plain"))
        assertEquals(setOf(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION), HermesV1ClientMethodSelection.forMediaType("application/pdf"))
        assertEquals(null, HermesV1ClientMethodSelection.forMediaType("audio/wav"))
        assertEquals(null, HermesV1ClientMethodSelection.forMediaType("audio/mp4"))
        assertEquals(null, HermesV1ClientMethodSelection.forMediaType("image/png"))
        assertEquals(null, HermesV1ClientMethodSelection.forMediaType("image/tiff"))
    }

    private fun invoke(service: HermesProcessingServiceV1EndToEndService, prepared: HermesV1PreparedSource, principal: HermesV1ProcessingPrincipal): HermesV1ClientOutcome {
        val transport = HermesV1RequestTransport { request, source ->
            val frame = ByteArrayOutputStream(); HermesProcessingServiceV1Framing.writeRequestFrame(frame, request, source)
            runBlocking { service.handle(principal, ByteArrayInputStream(frame.toByteArray())) }
        }
        return HermesProcessingServiceV1Client(transport).process(prepared)
    }

    private fun prepared(path: Path) = HermesV1PreparedSource(
        HermesV1RequestId("e2e-request-1"), HermesV1JobId("e2e-job-1"), HermesV1OccurrenceId("e2e-occurrence-1"), HermesV1BatchId("e2e-batch-1"), HermesV1SourceReference("e2e-source-1"), path,
        HermesV1Sha256(sha256(sourceBytes)), HermesV1SourceSize(sourceBytes.size.toLong()), HermesV1OriginalFilename("record.txt"), HermesV1MediaType("text/plain"), listOf(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION),
    )

    private fun request(prepared: HermesV1PreparedSource) = HermesProcessingServiceV1Request(HermesV1ProtocolVersion.CURRENT, prepared.requestId, prepared.jobId, prepared.occurrenceId, prepared.batchId, parker.core.interfaces.HermesProcessingServiceV1Source(prepared.sourceReference, prepared.sourceSha256, prepared.sizeBytes, prepared.originalFilename, prepared.mediaType), prepared.requestedMethods)

    private fun response(request: HermesProcessingServiceV1Request): HermesProcessingServiceV1Response {
        val method = request.requestedMethods.single(); val start = Instant.parse("2026-01-01T00:00:00.000Z"); val end = Instant.parse("2026-01-01T00:00:01.000Z")
        val result = HermesProcessingResult(request.source.sourceSha256.value, request.batchId.value, HermesProcessingStatus.PASS, setOf(method.establishedMethod), structuredRepresentation = HermesStructuredRepresentation.Text("verified"))
        return HermesProcessingServiceV1Response(request.protocolVersion, request.requestId, request.jobId, request.occurrenceId, request.batchId, request.source.sourceSha256, result, listOf(HermesV1RepresentationDescriptor(HermesV1RepresentationId("e2e-representation"), HermesV1RepresentationType.TEXT, method, result.structuredRepresentation)), emptyList(), null, HermesV1Provenance(request.source.sourceSha256, HermesV1ProcessorIdentity("e2e-test-processor", "1"), listOf(HermesV1MethodProvenance(method, start, end))))
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private fun deleteTree(root: Path) { Files.walk(root).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
}
