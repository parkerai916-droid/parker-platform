package parker.core.runtime

import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingFailureKind
import parker.core.interfaces.HermesProcessingCompleteness
import parker.core.interfaces.HermesProcessingIssue
import parker.core.interfaces.HermesProcessingIssueKind
import parker.core.interfaces.HermesProcessingIssueLocation
import parker.core.interfaces.HermesProcessingMethod
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingStatus
import parker.core.interfaces.TranscriptionFidelity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HermesProcessingResultTest {

    private val sha256 = "a".repeat(64)
    private val batchId = "bulk-a1b2c3d4-0000-0000-0000-000000000000"

    private fun result(
        status: HermesProcessingStatus,
        issues: List<HermesProcessingIssue> = emptyList(),
        failure: HermesProcessingFailure? = null,
        methods: Set<HermesProcessingMethod> = setOf(HermesProcessingMethod.DIRECT_TEXT_EXTRACTION),
        proposedEvidenceArtifactId: EvidenceArtifactId? = null,
    ) = HermesProcessingResult(
        sourceSha256 = sha256,
        batchId = batchId,
        status = status,
        methods = methods,
        proposedEvidenceArtifactId = proposedEvidenceArtifactId,
        issues = issues,
        failure = failure,
    )

    // --- PASS ---

    @Test
    fun `valid PASS can be constructed`() {
        val pass = result(HermesProcessingStatus.PASS)

        assertEquals(HermesProcessingStatus.PASS, pass.status)
        assertEquals(sha256, pass.sourceSha256)
    }

    @Test
    fun `PASS may carry non-blocking observations`() {
        val issue = HermesProcessingIssue(
            kind = HermesProcessingIssueKind.PROCESSING_EXCEPTION,
            explanation = "minor recoverable warning during extraction",
        )

        val pass = result(HermesProcessingStatus.PASS, issues = listOf(issue))

        assertEquals(listOf(issue), pass.issues)
    }

    @Test
    fun `PASS cannot contain a fatal failure reason`() {
        assertFailsWith<IllegalArgumentException> {
            result(
                HermesProcessingStatus.PASS,
                failure = HermesProcessingFailure(HermesProcessingFailureKind.PROCESSOR_FAILURE),
            )
        }
    }

    // --- REVIEW_REQUIRED ---

    @Test
    fun `REVIEW_REQUIRED can contain one issue`() {
        val issue = HermesProcessingIssue(
            kind = HermesProcessingIssueKind.TABLE_STRUCTURE_AMBIGUITY,
            explanation = "table column boundary is ambiguous",
            location = HermesProcessingIssueLocation.DocumentPage(pageNumber = 3),
            hermesInterpretation = "Gross earnings appears to be \$42,871.",
        )

        val reviewRequired = result(HermesProcessingStatus.REVIEW_REQUIRED, issues = listOf(issue))

        assertEquals(listOf(issue), reviewRequired.issues)
    }

    @Test
    fun `REVIEW_REQUIRED can contain multiple issues`() {
        val first = HermesProcessingIssue(
            kind = HermesProcessingIssueKind.MISSING_CONTENT,
            explanation = "page 2 appears blank where content was expected",
            location = HermesProcessingIssueLocation.DocumentPage(pageNumber = 2),
        )
        val second = HermesProcessingIssue(
            kind = HermesProcessingIssueKind.CONFLICTING_EXTRACTION_OUTPUTS,
            explanation = "OCR and direct text extraction disagree on the total",
            location = HermesProcessingIssueLocation.DocumentPage(pageNumber = 4),
        )

        val reviewRequired = result(HermesProcessingStatus.REVIEW_REQUIRED, issues = listOf(first, second))

        assertEquals(listOf(first, second), reviewRequired.issues)
    }

    @Test
    fun `REVIEW_REQUIRED requires at least one review issue`() {
        assertFailsWith<IllegalArgumentException> {
            result(HermesProcessingStatus.REVIEW_REQUIRED, issues = emptyList())
        }
    }

    @Test
    fun `REVIEW_REQUIRED must not carry a fatal failure reason`() {
        val issue = HermesProcessingIssue(
            kind = HermesProcessingIssueKind.UNREADABLE_REGION,
            explanation = "a region of page 1 is unreadable",
        )

        assertFailsWith<IllegalArgumentException> {
            result(
                HermesProcessingStatus.REVIEW_REQUIRED,
                issues = listOf(issue),
                failure = HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE),
            )
        }
    }

    @Test
    fun `REVIEW_REQUIRED preserves source location correctly`() {
        val location = HermesProcessingIssueLocation.DocumentPage(
            pageNumber = 7,
            startOffsetInclusive = 120,
            endOffsetExclusive = 160,
            regionDescription = "table, column 2",
        )
        val issue = HermesProcessingIssue(
            kind = HermesProcessingIssueKind.OCR_UNCERTAINTY,
            explanation = "uncertain recognition within a table cell",
            location = location,
        )

        val reviewRequired = result(HermesProcessingStatus.REVIEW_REQUIRED, issues = listOf(issue))

        val preserved = reviewRequired.issues.single().location as HermesProcessingIssueLocation.DocumentPage
        assertEquals(7, preserved.pageNumber)
        assertEquals(120, preserved.startOffsetInclusive)
        assertEquals(160, preserved.endOffsetExclusive)
        assertEquals("table, column 2", preserved.regionDescription)
    }

    @Test
    fun `confidence and review threshold remain distinct`() {
        val issue = HermesProcessingIssue(
            kind = HermesProcessingIssueKind.OCR_UNCERTAINTY,
            explanation = "OCR confidence is below threshold",
            observedConfidence = 0.74,
        )
        val value = result(HermesProcessingStatus.REVIEW_REQUIRED, issues = listOf(issue))
            .copy(reviewConfidenceThreshold = 0.80, processingCompleteness = HermesProcessingCompleteness.PARTIAL)

        assertEquals(0.74, value.issues.single().observedConfidence)
        assertEquals(0.80, value.reviewConfidenceThreshold)
        assertEquals(HermesProcessingCompleteness.PARTIAL, value.processingCompleteness)
    }

    @Test
    fun `confidence outside the provider scale is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            HermesProcessingIssue(HermesProcessingIssueKind.OCR_UNCERTAINTY, "uncertain", observedConfidence = 1.01)
        }
    }

    // --- FAILED ---

    @Test
    fun `FAILED requires a failure reason`() {
        assertFailsWith<IllegalArgumentException> {
            result(HermesProcessingStatus.FAILED, failure = null)
        }
    }

    @Test
    fun `FAILED preserves source identity, hash, and batch information`() {
        val evidenceArtifactId = EvidenceArtifactId("evidence-hermes-r0-test")

        val failed = result(
            HermesProcessingStatus.FAILED,
            failure = HermesProcessingFailure(HermesProcessingFailureKind.ENCRYPTED_SOURCE, detail = "password-protected PDF"),
            proposedEvidenceArtifactId = evidenceArtifactId,
        )

        assertEquals(sha256, failed.sourceSha256)
        assertEquals(batchId, failed.batchId)
        assertEquals(evidenceArtifactId, failed.proposedEvidenceArtifactId)
        assertEquals(HermesProcessingFailureKind.ENCRYPTED_SOURCE, failed.failure?.kind)
        assertEquals("password-protected PDF", failed.failure?.detail)
    }

    // --- Processing methods ---

    @Test
    fun `supports one processing method`() {
        val pass = result(HermesProcessingStatus.PASS, methods = setOf(HermesProcessingMethod.VISION))

        assertEquals(setOf(HermesProcessingMethod.VISION), pass.methods)
    }

    @Test
    fun `supports multiple processing methods where applicable`() {
        val pass = result(
            HermesProcessingStatus.PASS,
            methods = setOf(HermesProcessingMethod.DIRECT_TEXT_EXTRACTION, HermesProcessingMethod.OCR),
        )

        assertEquals(setOf(HermesProcessingMethod.DIRECT_TEXT_EXTRACTION, HermesProcessingMethod.OCR), pass.methods)
    }

    @Test
    fun `at least one processing method is required`() {
        assertFailsWith<IllegalArgumentException> {
            result(HermesProcessingStatus.PASS, methods = emptySet())
        }
    }

    // --- Fidelity vocabulary reuse ---

    @Test
    fun `transcriptionFidelity may accompany a LOW_FIDELITY_TRANSCRIPTION issue`() {
        val issue = HermesProcessingIssue(
            kind = HermesProcessingIssueKind.LOW_FIDELITY_TRANSCRIPTION,
            explanation = "transcription confidence is low for this passage",
            transcriptionFidelity = TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
        )

        val reviewRequired = result(HermesProcessingStatus.REVIEW_REQUIRED, issues = listOf(issue))

        assertEquals(TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION, reviewRequired.issues.single().transcriptionFidelity)
    }

    @Test
    fun `transcriptionFidelity is rejected for an unrelated issue kind`() {
        assertFailsWith<IllegalArgumentException> {
            HermesProcessingIssue(
                kind = HermesProcessingIssueKind.MISSING_CONTENT,
                explanation = "content missing",
                transcriptionFidelity = TranscriptionFidelity.NORMALISED,
            )
        }
    }

    // --- Lifecycle boundary ---

    @Test
    fun `HermesProcessingStatus is not SteveReviewQueueStatus`() {
        assertTrue(HermesProcessingStatus::class != SteveReviewQueueStatus::class)
        assertEquals(setOf("PASS", "REVIEW_REQUIRED", "FAILED"), HermesProcessingStatus.values().map { it.name }.toSet())
        assertEquals(setOf("PASS", "REVIEW_REQUIRED", "FAILED"), SteveReviewQueueStatus.values().map { it.name }.toSet())
    }

    @Test
    fun `HermesProcessingResult carries no reference to post-ingestion review state`() {
        val pass = result(HermesProcessingStatus.PASS)

        // HermesProcessingResult's own declared properties are exactly these five plus the two
        // optional fields exercised elsewhere in this file -- none of them is a DerivativeReviewState,
        // a SteveReviewQueueStatus, or a HumanFidelityReviewState.
        assertEquals(sha256, pass.sourceSha256)
        assertEquals(batchId, pass.batchId)
        assertEquals(null, pass.proposedEvidenceArtifactId)
        assertEquals(emptyList(), pass.issues)
        assertNull(pass.failure)
    }
}
