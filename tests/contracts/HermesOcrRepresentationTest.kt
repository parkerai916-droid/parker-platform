package parker.core.interfaces

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HermesOcrRepresentationTest {
    private val sourceSha = "a".repeat(64)
    private val text = "preliminary scanned PDF text"

    private fun representation(
        mediaType: String = "application/pdf",
        completeness: HermesProcessingCompleteness = HermesProcessingCompleteness.COMPLETE,
        status: HermesProcessingStatus = HermesProcessingStatus.PASS,
        source: String = sourceSha,
    ) = HermesOcrRepresentation(
        evidenceArtifactId = EvidenceArtifactId("evidence-pdf"),
        sourceSha256 = source,
        originalFilename = "scan.pdf",
        originalMediaType = mediaType,
        processingMethod = HermesProcessingMethod.OCR,
        status = status,
        recognisedText = text,
        derivativeContentSha256 = CanonicalPagePixelDigests.sha256(text.toByteArray()),
        confidence = 0.62,
        completeness = completeness,
        warnings = listOf("preliminary OCR"),
        issues = listOf(HermesProcessingIssue(HermesProcessingIssueKind.OCR_UNCERTAINTY, "low confidence", observedConfidence = 0.62)),
        mechanismVersion = "docling-test",
        modelIdentity = "rapidocr-test",
        modelVersion = "model-test",
    )

    @Test
    fun `PDF complete preliminary OCR representation is valid`() {
        assertEquals("application/pdf", representation().originalMediaType)
    }

    @Test
    fun `PDF partial preliminary OCR is valid while uncertainty remains represented`() {
        val value = representation(completeness = HermesProcessingCompleteness.PARTIAL)
        assertEquals(HermesProcessingCompleteness.PARTIAL, value.completeness)
        assertEquals(0.62, value.confidence)
        assertEquals(HermesProcessingIssueKind.OCR_UNCERTAINTY, value.issues.single().kind)
    }

    @Test
    fun `image PASS partial remains rejected`() {
        assertFailsWith<IllegalArgumentException> {
            representation(mediaType = "image/png", completeness = HermesProcessingCompleteness.PARTIAL)
        }
    }

    @Test
    fun `source digest remains strictly validated`() {
        assertFailsWith<IllegalArgumentException> { representation(source = "b".repeat(63)) }
    }
}
