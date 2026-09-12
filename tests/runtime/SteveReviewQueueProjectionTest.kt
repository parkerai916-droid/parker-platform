package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import parker.core.interfaces.*

class SteveReviewQueueProjectionTest {
    @Test
    fun `enumeration derives pass review required and failed without writes`() = runTest {
        val evidenceId = EvidenceArtifactId("evidence-queue")
        val source = OwnerRegisteredEvidence(evidenceId, "a".repeat(64), 3, "image/png", "page.png")
        val pass = generation("generation-pass", evidenceId)
        val pending = generation("generation-pending", evidenceId)
        val failed = generation("generation-failed", evidenceId)
        val contents = ReadOnlyContents(mapOf(pass.derivativeGenerationId to content(pass.derivativeGenerationId, evidenceId), pending.derivativeGenerationId to content(pending.derivativeGenerationId, evidenceId)))
        val projection = SteveReviewQueueProjection(
            listEvidence = { listOf(source) }, assignments = null, cases = null, batches = null,
            generations = OcrDiscovery(listOf(pass, pending, failed)), contents = contents,
            derivativeReviews = null, fidelityReviews = null,
            fidelityProjector = EffectiveProjector { target, _ ->
                val state = when (target.derivativeGenerationId) {
                    pass.derivativeGenerationId -> HumanFidelityReviewState.HUMAN_REVIEWED_PASS
                    else -> HumanFidelityReviewState.UNREVIEWED
                }
                if (state == HumanFidelityReviewState.UNREVIEWED) {
                    EffectiveHumanFidelityReviewProjectionOutcome.Projected(
                        EffectiveHumanFidelityReviewSummary(
                            HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION,
                            EffectiveHumanFidelityReviewProjection(target, state, null, emptySet(), emptySet(),
                                SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED, SourceConfirmedDenialReason.UNREVIEWED)),
                            0, 0, false,
                        ),
                    )
                } else {
                    val coverage = HumanFidelityReviewCoverage(
                        HumanFidelityCoverageKind.FULL_GENERATION, listOf(1), emptyList(),
                    )
                    EffectiveHumanFidelityReviewProjectionOutcome.Projected(
                        EffectiveHumanFidelityReviewSummary(
                            HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION,
                            EffectiveHumanFidelityReviewProjection(target, state, coverage, setOf(HumanFidelityReviewId("review-${"a".repeat(64)}")), emptySet(),
                                SourceConfirmedEligibility(SourceConfirmedEligibilityState.ALLOWED)),
                            0, 0, false,
                        ),
                    )
                }
            }, corrections = null,
        )

        val items = projection.enumerate()
        assertEquals(setOf(SteveReviewQueueStatus.PASS, SteveReviewQueueStatus.REVIEW_REQUIRED, SteveReviewQueueStatus.FAILED), items.map { it.status }.toSet(), items.map { it.generation.derivativeGenerationId.value to (it.status to it.failureReason) }.toString())
        assertEquals(0, contents.writeCount)
    }

    private fun generation(id: String, evidence: EvidenceArtifactId) = DerivativeGenerationRecord(
        DerivativeGenerationId(id), evidence, listOf(DerivativeParentReference.RootEvidenceArtifact(evidence)), "ocr",
        DerivativeProducerIdentity("test", "1", "test", modelIdentity = "model", modelVersion = "1"), listOf(DerivativeTransformation.OCR), Instant.EPOCH,
        DerivativeContentIdentity.Digest("SHA-256", "b".repeat(64)), DerivativeCompletenessState.ACCOUNTED_FOR,
        DerivativeOperationalOutcome.USABLE,
    )

    private fun content(generation: DerivativeGenerationId, evidence: EvidenceArtifactId) = DerivativeContentEntry(
        generation, evidence, TierADerivativePayload.Ocr(
            OcrDerivativeExtractedResult(
                "text", TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION, OcrDerivativeOutcomeKind.RECOGNISED,
                null, emptyList(), emptyList(), DerivativeProducerIdentity("test", "1", "test", modelIdentity = "model", modelVersion = "1"),
                listOf(DerivativeTransformation.OCR), DerivativeCompletenessState.ACCOUNTED_FOR,
                OcrPageAccounting(OcrPageScope(listOf(1)), OcrPageScope(listOf(1)), OcrPageScope(listOf(1)),
                    listOf(OcrPageOutcome(1, OcrPageOutcomeKind.TRANSCRIBED))),
                OcrProcessingProvenance(evidence, OcrSha256Digest("a".repeat(64)), "image/png", 3,
                    OcrPageScope(listOf(1)), OcrPageScope(listOf(1)), "image/png", 3, OcrSha256Digest("c".repeat(64)), true, "test", Instant.EPOCH),
            ),
        ),
    )

    private class OcrDiscovery(private val values: List<DerivativeGenerationRecord>) : OcrDerivativeGenerationDiscovery {
        override suspend fun findOcrGenerationsForEvidence(evidenceArtifactId: EvidenceArtifactId) = values
    }

    private class ReadOnlyContents(private val values: Map<DerivativeGenerationId, DerivativeContentEntry>) : DerivativeContentStorage {
        var writeCount = 0
        override suspend fun prepare(entry: DerivativeContentEntry) { writeCount++ }
        override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) { writeCount++ }
        override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId) = values[derivativeGenerationId]
    }

    private fun interface EffectiveProjector : EffectiveHumanFidelityReviewProjector
}
