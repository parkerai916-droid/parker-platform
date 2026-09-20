package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HermesProcessingServiceV1ProtocolTest {

    private val sha = HermesV1Sha256("a".repeat(64))
    private val source = HermesProcessingServiceV1Source(
        reference = HermesV1SourceReference("parker-source-v1-source-1"),
        sourceSha256 = sha,
        sizeBytes = HermesV1SourceSize(12_345),
        originalFilename = HermesV1OriginalFilename("record.txt"),
        mediaType = HermesV1MediaType("text/plain"),
    )

    private fun request(
        protocolVersion: HermesV1ProtocolVersion = HermesV1ProtocolVersion.CURRENT,
        source: HermesProcessingServiceV1Source = this.source,
    ) = HermesProcessingServiceV1Request(
        protocolVersion = protocolVersion,
        requestId = HermesV1RequestId("request-1"),
        jobId = HermesV1JobId("job-1"),
        occurrenceId = HermesV1OccurrenceId("occurrence-1"),
        batchId = HermesV1BatchId("batch-1"),
        source = source,
    )

    @Test
    fun `protocol version accepts v1`() {
        assertEquals("1", HermesV1ProtocolVersion.CURRENT.value)
        assertEquals("1", request().protocolVersion.value)
    }

    @Test
    fun `unsupported protocol version is rejected`() {
        assertFailsWith<IllegalArgumentException> { HermesV1ProtocolVersion("2") }
    }

    @Test
    fun `required identifiers reject blank values`() {
        assertFailsWith<IllegalArgumentException> { HermesV1RequestId("") }
        assertFailsWith<IllegalArgumentException> { HermesV1JobId(" ") }
        assertFailsWith<IllegalArgumentException> { HermesV1OccurrenceId("\n") }
        assertFailsWith<IllegalArgumentException> { HermesV1BatchId("") }
    }

    @Test
    fun `request has no case id or arbitrary source path`() {
        assertEquals(false, HermesProcessingServiceV1Request::class.members.any { it.name == "caseId" })
        assertFailsWith<IllegalArgumentException> { HermesV1SourceReference("/var/parker/source.txt") }
        assertFailsWith<IllegalArgumentException> { HermesV1SourceReference("C:\\source\\file.txt") }
    }

    @Test
    fun `sha256 requires exactly lowercase hexadecimal`() {
        HermesV1Sha256("a".repeat(64))
        assertFailsWith<IllegalArgumentException> { HermesV1Sha256("A".repeat(64)) }
        assertFailsWith<IllegalArgumentException> { HermesV1Sha256("a".repeat(63)) }
        assertFailsWith<IllegalArgumentException> { HermesV1Sha256("g".repeat(64)) }
    }

    @Test
    fun `source size rejects negative and above frozen maximum`() {
        assertFailsWith<IllegalArgumentException> { HermesV1SourceSize(-1) }
        assertFailsWith<IllegalArgumentException> {
            HermesV1SourceSize(HermesProcessingServiceV1Limits.MAX_SOURCE_BYTES + 1)
        }
        HermesV1SourceSize(HermesProcessingServiceV1Limits.MAX_SOURCE_BYTES)
    }

    @Test
    fun `filename validates UTF8 byte boundary and path safety`() {
        HermesV1OriginalFilename("é".repeat(127) + "x")
        assertFailsWith<IllegalArgumentException> { HermesV1OriginalFilename("é".repeat(128)) }
        assertFailsWith<IllegalArgumentException> { HermesV1OriginalFilename("../record.txt") }
        assertFailsWith<IllegalArgumentException> { HermesV1OriginalFilename("record\u0000.txt") }
    }

    @Test
    fun `media type is a bounded type and subtype`() {
        assertEquals("text/plain", HermesV1MediaType("text/plain").value)
        assertFailsWith<IllegalArgumentException> { HermesV1MediaType("text") }
        assertFailsWith<IllegalArgumentException> { HermesV1MediaType("text/plain; charset=utf-8") }
    }

    @Test
    fun `initial method registry is closed and excludes disabled methods`() {
        assertEquals(
            setOf(
                "DIRECT_TEXT_EXTRACTION",
                "STRUCTURED_DOCUMENT_EXTRACTION",
                "STRUCTURED_SPREADSHEET_EXTRACTION",
                "STRUCTURED_EMAIL_EXTRACTION",
            ),
            HermesV1ProcessingMethod.entries.map { it.wireValue }.toSet(),
        )
        assertFailsWith<IllegalArgumentException> { HermesV1ProcessingMethod.fromWireValue("OCR") }
        assertFailsWith<IllegalArgumentException> { HermesV1ProcessingMethod.fromWireValue("custom-method") }
    }

    @Test
    fun `initial representation registry is closed`() {
        assertEquals(setOf("TEXT", "STRUCTURED_DOCUMENT"), HermesV1RepresentationType.entries.map { it.wireValue }.toSet())
        assertFailsWith<IllegalArgumentException> { HermesV1RepresentationType.fromWireValue("TRANSCRIPT") }
        assertFailsWith<IllegalArgumentException> { HermesV1RepresentationType.fromWireValue("arbitrary") }
    }

    @Test
    fun `status registry is closed and reuses established status semantics`() {
        assertEquals(setOf("PASS", "REVIEW_REQUIRED", "FAILED"), HermesV1StatusRegistry.values)
        assertEquals(HermesProcessingStatus.PASS, HermesV1StatusRegistry.fromWireValue("PASS"))
        assertFailsWith<IllegalArgumentException> { HermesV1StatusRegistry.fromWireValue("ADMITTED") }
    }

    @Test
    fun `issue registry is closed and explanations are separate`() {
        val issue = HermesV1Issue(HermesV1IssueCode.STRUCTURE_AMBIGUITY, "column boundary is unclear")
        assertEquals(HermesV1IssueCode.STRUCTURE_AMBIGUITY, issue.code)
        assertFailsWith<IllegalArgumentException> { HermesV1IssueCode.fromWireValue("free-form") }
        assertFailsWith<IllegalArgumentException> { HermesV1Issue(HermesV1IssueCode.MISSING_CONTENT, "") }
    }

    @Test
    fun `failure requires category-aligned stable code retryability and detail`() {
        val failure = HermesV1Failure(
            category = HermesV1FailureCategory.INTEGRITY,
            detailCode = HermesV1FailureDetailCode.SOURCE_HASH_MISMATCH,
            retryable = false,
            detail = "received source hash does not match the request",
        )
        assertEquals(false, failure.retryable)
        assertFailsWith<IllegalArgumentException> {
            HermesV1Failure(
                HermesV1FailureCategory.INTEGRITY,
                HermesV1FailureDetailCode.PROCESSOR_FAILED,
                retryable = true,
                detail = "wrong category",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            HermesV1Failure(HermesV1FailureCategory.INTERNAL, HermesV1FailureDetailCode.INTERNAL_STORAGE_FAILURE, true, "")
        }
        assertFailsWith<IllegalArgumentException> { HermesV1FailureCategory.fromWireValue("RETRYABLE") }
    }

    @Test
    fun `issue and representation collection limits are enforced`() {
        val issue = HermesV1Issue(HermesV1IssueCode.PROCESSING_QUALIFICATION, "qualified result")
        val operation = HermesV1MethodProvenance(
            method = HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION,
            startedAt = java.time.Instant.parse("2026-01-01T00:00:00Z"),
            completedAt = java.time.Instant.parse("2026-01-01T00:00:01Z"),
        )
        val result = HermesProcessingResult(
            sourceSha256 = sha.value,
            batchId = "batch-1",
            status = HermesProcessingStatus.PASS,
            methods = setOf(HermesProcessingMethod.DIRECT_TEXT_EXTRACTION),
        )
        fun envelope(issueCount: Int, representationCount: Int) = HermesProcessingServiceV1Response(
            protocolVersion = HermesV1ProtocolVersion.CURRENT,
            requestId = HermesV1RequestId("request-1"),
            jobId = HermesV1JobId("job-1"),
            occurrenceId = HermesV1OccurrenceId("occurrence-1"),
            batchId = HermesV1BatchId("batch-1"),
            sourceSha256 = sha,
            establishedResult = result,
            representations = (0 until representationCount).map {
                HermesV1RepresentationDescriptor(
                    HermesV1RepresentationId("representation-$it"),
                    HermesV1RepresentationType.TEXT,
                    HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION,
                )
            },
            issues = (0 until issueCount).map { issue },
            failure = null,
            provenance = HermesV1Provenance(
                sourceSha256 = sha,
                processor = HermesV1ProcessorIdentity("hermes-processing", "test"),
                operations = listOf(operation),
            ),
        )

        envelope(HermesProcessingServiceV1Limits.MAX_ISSUES, HermesProcessingServiceV1Limits.MAX_REPRESENTATIONS)
        assertFailsWith<IllegalArgumentException> { envelope(HermesProcessingServiceV1Limits.MAX_ISSUES + 1, 0) }
        assertFailsWith<IllegalArgumentException> { envelope(0, HermesProcessingServiceV1Limits.MAX_REPRESENTATIONS + 1) }
    }

    @Test
    fun `value models have deterministic equality and provenance validates time order`() {
        assertEquals(request(), request())
        assertFailsWith<IllegalArgumentException> {
            HermesV1MethodProvenance(
                HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION,
                Instant.parse("2026-01-01T00:00:01Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
            )
        }
    }
}
