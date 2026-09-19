package parker.core.runtime

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.*

class GovernedAnalysisResultExporterTest {
    @Test
    fun `markdown and json are deterministic and preserve governed distinctions`() = runTest {
        val expected = result()
        val storage = object : GovernedAnalysisResultStorage {
            override suspend fun createOrGet(result: GovernedAnalysisResult) = GovernedAnalysisResultCreationOutcome.Created(result)
            override suspend fun findByAnalysisRequestId(analysisRequestId: AnalysisRequestId) = expected.takeIf { expected.analysisRequestId == analysisRequestId }
        }
        val exporter = GovernedAnalysisResultExporter(storage)
        val markdown = requireNotNull(exporter.export(expected.analysisRequestId, GovernedAnalysisExportFormat.MARKDOWN))
        val repeatMarkdown = requireNotNull(exporter.export(expected.analysisRequestId, GovernedAnalysisExportFormat.MARKDOWN))
        val json = requireNotNull(exporter.export(expected.analysisRequestId, GovernedAnalysisExportFormat.JSON))
        val repeatJson = requireNotNull(exporter.export(expected.analysisRequestId, GovernedAnalysisExportFormat.JSON))

        assertEquals(markdown.body, repeatMarkdown.body)
        assertEquals(json.body, repeatJson.body)
        assertContains(markdown.body, "# Parker Case Analysis Export")
        assertContains(markdown.body, "Case ID: `case-export`")
        assertContains(markdown.body, "[SUPPORTED]")
        assertContains(markdown.body, "[INFERENCE]")
        assertContains(markdown.body, "[CONFLICT]")
        assertContains(markdown.body, "[NOT_FOUND]")
        assertContains(markdown.body, "[REVIEW_REQUIRED]")
        assertContains(markdown.body, "[EVIDENCE: artifact=evidence-export; derivative=derivative-export")
        assertContains(markdown.body, "## Source Index")
        assert(!markdown.body.contains("/home/") && !markdown.body.contains("C:\\"))
        assertContains(json.body, "\"schemaVersion\":1")
        assertContains(json.body, "\"analysisRequestId\":\"analysis-11111111-1111-1111-1111-111111111111\"")
        assertContains(json.body, "\"status\":\"SUPPORTED\"")
        assertContains(json.body, "\"status\":\"INFERENCE\"")
        assertContains(json.body, "\"status\":\"CONFLICT\"")
        assertContains(json.body, "\"status\":\"NOT_FOUND\"")
        assertContains(json.body, "\"status\":\"REVIEW_REQUIRED\"")
        assertContains(json.body, "\"associationId\":\"association-export\"")
        assertEquals("text/markdown; charset=utf-8", markdown.contentType)
        assertEquals("application/json; charset=utf-8", json.contentType)
    }

    @Test
    fun `unknown durable result is not regenerated`() = runTest {
        var reads = 0
        val storage = object : GovernedAnalysisResultStorage {
            override suspend fun createOrGet(result: GovernedAnalysisResult) = error("must not write")
            override suspend fun findByAnalysisRequestId(analysisRequestId: AnalysisRequestId): GovernedAnalysisResult? { reads++; return null }
        }
        val request = AnalysisRequestId.new()
        assertNull(GovernedAnalysisResultExporter(storage).export(request, GovernedAnalysisExportFormat.JSON))
        assertEquals(1, reads)
    }

    private fun result(): GovernedAnalysisResult {
        val request = AnalysisRequestId("analysis-11111111-1111-1111-1111-111111111111")
        val artifact = EvidenceArtifactId("evidence-export")
        val derivative = DerivativeGenerationId("derivative-export")
        val reference = AnalysisEvidenceReference(artifact, derivative, "a".repeat(64), "source.txt", AnalysisReferencePrecision.DOCUMENT, sectionHeading = "Summary")
        return GovernedAnalysisResult(
            1, GovernedAnalysisResultId.forRequest(request), request, CaseId("case-export"), "Export Case", "What happened?", AnalysisType.ISSUE_ANALYSIS,
            Instant.parse("2026-01-01T00:00:00Z"), "Hermes", "model-1", "session-1", "profile-1",
            listOf(GovernedAnalysisEvidenceScopeEntry(artifact, CaseEvidenceAssociationId("association-export"), listOf(EvidenceOccurrenceId("occurrence-export")), derivative, "a".repeat(64), "source.txt")),
            "Final narrative.",
            StructuredAnalysisResult(
                "Answer", listOf(StructuredAnalysisFinding("supported fact", listOf(reference)), StructuredAnalysisFinding("inferred conclusion", emptyList())),
                listOf(StructuredAnalysisContraryEvidence("competing account", listOf(reference))),
                listOf(StructuredAnalysisUncertainty("needs review", listOf(reference))),
                listOf(StructuredAnalysisEvidenceGap("missing record")), "Conclusion",
            ),
        )
    }
}
