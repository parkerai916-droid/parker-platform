package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.HermesPreIngestionCorrectedRepresentation
import parker.core.interfaces.HermesPreIngestionCorrectionPublication
import parker.core.interfaces.HermesPreIngestionCorrectionBindingResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.HermesProcessingCorrection
import parker.core.interfaces.HermesProcessingHumanDecision
import parker.core.interfaces.HermesProcessingHumanDecisionType
import parker.core.interfaces.HermesProcessingIssue
import parker.core.interfaces.HermesProcessingIssueKind
import parker.core.interfaces.HermesProcessingMethod
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingStatus
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

class HermesPreIngestionCorrectionRegistryTest {
    private val owner = PrincipalId("owner-${"1".repeat(64)}")

    private fun result() = HermesProcessingResult(
        sourceSha256 = "a".repeat(64), batchId = "bulk-correction-test", status = HermesProcessingStatus.REVIEW_REQUIRED,
        methods = setOf(HermesProcessingMethod.OCR), issues = listOf(
            HermesProcessingIssue(HermesProcessingIssueKind.MISSING_CONTENT, "machine could not read the amount", hermesInterpretation = "1,000"),
        ),
    )

    private fun decision(result: HermesProcessingResult, at: Instant = Instant.parse("2026-09-12T00:00:00Z")) =
        HermesProcessingHumanDecision(
            result.batchId, result.sourceSha256, HermesProcessingHumanDecisionType.CORRECT, owner, at,
            reason = "Owner checked the source", correction = HermesProcessingCorrection(0, "1,000.00", "verified"),
        )

    private fun representation(result: HermesProcessingResult, d: HermesProcessingHumanDecision) =
        HermesPreIngestionCorrectedRepresentation(
            representationId = HermesPreIngestionCorrectedRepresentation.deriveId(
                result.batchId, result.sourceSha256, 0, result.issues[0].kind, result.issues[0].explanation,
                result.issues[0].hermesInterpretation, "1,000.00", "Owner checked the source", owner,
            ),
            batchId = result.batchId, sourceSha256 = result.sourceSha256, machineResultStatus = result.status,
            issueIndex = 0, machineIssueKind = result.issues[0].kind, machineIssueExplanation = result.issues[0].explanation,
            machineInterpretation = result.issues[0].hermesInterpretation, correctedInterpretation = "1,000.00",
            ownerExplanation = "Owner checked the source", ownerPrincipalId = owner, decisionAt = d.decidedAt,
        )

    @Test
    fun `publication is idempotent and matching replay with a later decision time does not duplicate`() = runTest {
        val result = result(); val firstDecision = decision(result); val registry = InMemoryHermesPreIngestionCorrectionRegistry()
        val first = assertIs<HermesPreIngestionCorrectionPublication.Created>(registry.publish(representation(result, firstDecision)))
        val second = assertIs<HermesPreIngestionCorrectionPublication.AlreadyPublished>(registry.publish(representation(result, decision(result, Instant.parse("2026-09-12T00:01:00Z")))))
        assertEquals(first.representation.representationId, second.representation.representationId)
        assertNotNull(registry.findForDecision(result, firstDecision))
    }

    @Test
    fun `filesystem publication survives reconstruction and retains provenance`() = runTest {
        val root = Files.createTempDirectory("hermes-pre-ingestion-correction")
        val result = result(); val d = decision(result)
        val first = FileSystemHermesPreIngestionCorrectionRegistry(root)
        val representation = representation(result, d)
        assertIs<HermesPreIngestionCorrectionPublication.Created>(first.publish(representation))
        val restarted = FileSystemHermesPreIngestionCorrectionRegistry(root)
        val recovered = assertNotNull(restarted.findForDecision(result, d))
        assertEquals(representation.representationId, recovered.representationId)
        assertEquals(result.issues[0].hermesInterpretation, recovered.machineInterpretation)
        assertEquals(d.reason, recovered.ownerExplanation)
    }

    @Test
    fun `binding is idempotent and conflicting evidence rebinding fails closed`() = runTest {
        val result = result(); val d = decision(result); val registry = InMemoryHermesPreIngestionCorrectionRegistry()
        val representation = representation(result, d)
        registry.publish(representation)
        val evidence = EvidenceArtifactId("evidence-corrected-1")
        assertIs<HermesPreIngestionCorrectionBindingResult.Bound>(registry.bindToEvidence(representation, evidence, result.sourceSha256, d.decidedAt))
        assertIs<HermesPreIngestionCorrectionBindingResult.AlreadyBound>(registry.bindToEvidence(representation, evidence, result.sourceSha256, d.decidedAt.plusSeconds(1)))
        assertIs<HermesPreIngestionCorrectionBindingResult.Conflict>(registry.bindToEvidence(representation, EvidenceArtifactId("evidence-corrected-2"), result.sourceSha256, d.decidedAt))
        assertIs<HermesPreIngestionCorrectionBindingResult.Conflict>(registry.bindToEvidence(representation, EvidenceArtifactId("evidence-corrected-3"), "b".repeat(64), d.decidedAt))
        val lineage = assertNotNull(registry.findLineageForEvidence(evidence))
        assertEquals(representation.representationId, lineage.correction.representationId)
        assertEquals(evidence, lineage.binding.evidenceArtifactId)
        val content = lineage.correctedContent(evidence)
        assertEquals("OWNER_AUTHORIZED_CORRECTION", content.authority)
        assertEquals("ISSUE", content.scope)
        assertEquals("1,000", content.machineInterpretation)
        assertEquals("1,000.00", content.correctedInterpretation)
    }

    @Test
    fun `filesystem evidence binding survives restart`() = runTest {
        val root = Files.createTempDirectory("hermes-pre-ingestion-binding")
        val result = result(); val d = decision(result); val representation = representation(result, d)
        val first = FileSystemHermesPreIngestionCorrectionRegistry(root)
        first.publish(representation)
        val evidence = EvidenceArtifactId("evidence-corrected-restart")
        assertIs<HermesPreIngestionCorrectionBindingResult.Bound>(first.bindToEvidence(representation, evidence, result.sourceSha256, d.decidedAt))
        val restarted = FileSystemHermesPreIngestionCorrectionRegistry(root)
        val lineage = assertNotNull(restarted.findLineageForEvidence(evidence))
        assertEquals(representation.representationId, lineage.correction.representationId)
        assertEquals(result.sourceSha256, lineage.binding.sourceSha256)
    }

    @Test
    fun `filesystem lineage lookup fails closed when correction content is missing`() = runTest {
        val root = Files.createTempDirectory("hermes-pre-ingestion-missing-content")
        val result = result(); val d = decision(result); val representation = representation(result, d)
        val registry = FileSystemHermesPreIngestionCorrectionRegistry(root)
        registry.publish(representation)
        registry.bindToEvidence(representation, EvidenceArtifactId("evidence-missing-content"), result.sourceSha256, d.decidedAt)
        Files.delete(root.resolve("pre-ingestion-corrections").resolve(representation.representationId.value + ".hpc"))
        assertFailsWith<HermesPreIngestionCorrectionContentUnavailableException> {
            registry.findLineageForEvidence(EvidenceArtifactId("evidence-missing-content"))
        }
    }
}
