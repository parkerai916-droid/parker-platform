package parker.core.runtime

import java.time.Instant
import parker.core.interfaces.*

internal object HumanFidelityReviewFixture {
    const val HIGH_FIDELITY = "high fidelity overall with one systematic material proper-name discrepancy pattern"
    const val PATTERN = "Kellec in the source is represented as Kellee in two explicit locations"
    val reviewer = PrincipalId("owner.steven-francis-mctague")
    val target = HumanFidelityReviewTarget(
        EvidenceArtifactId("evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9"),
        OcrSha256Digest("5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e"),
        OcrSha256Digest("85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f"),
        DerivativeGenerationId("region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6"),
        OcrSha256Digest("9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14"),
        OcrSha256Digest("18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb"),
    )
    val artifacts = HumanFidelityReviewArtifacts(
        OcrSha256Digest("8e7928c671cd36c7a4517dc5d9429706c46efb65c565e948684d6c3e7c8773a4"),
        OcrSha256Digest("2d47f50e0f2915bd0e18e914eac4bd5abc879cf5419969d482b2b7f6ff6b1293"),
    )

    fun review(
        reviewedAt: Instant = Instant.parse("2026-09-03T00:00:00Z"),
        reason: String = "Identity-bearing proper name differs from the exact source",
        reverseInput: Boolean = false,
        supersession: HumanFidelityReviewSupersession? = null,
        adjudication: HumanFidelityReviewAdjudicationReference? = null,
    ): HumanFidelityReviewRecord {
        val coverage = HumanFidelityReviewCoverage(HumanFidelityCoverageKind.FULL_GENERATION, (1..5).toList())
        val reviewId = HumanFidelityReviewRecord.deriveId(
            target, reviewer, reviewedAt, artifacts, coverage,
            HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY, HIGH_FIDELITY,
        )
        val bare = listOf(occurrence(reviewId, 1, reason), occurrence(reviewId, 5, reason))
        val patternId = SystematicDiscrepancyPattern.deriveId(reviewId, bare.map { it.discrepancyId }, PATTERN, reviewer)
        val occurrences = listOf(occurrence(reviewId, 1, reason, patternId), occurrence(reviewId, 5, reason, patternId))
        val pattern = SystematicDiscrepancyPattern(patternId, reviewId, occurrences.map { it.discrepancyId }, PATTERN, 2, reviewer)
        return HumanFidelityReviewRecord(
            reviewId, target, reviewer, reviewedAt, artifacts, coverage,
            HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY, HIGH_FIDELITY,
            if (reverseInput) occurrences.reversed() else occurrences,
            listOf(pattern), supersession, adjudication,
        )
    }

    fun auditRecord(
        review: HumanFidelityReviewRecord = review(),
        type: HumanFidelityGovernanceAuditEventType = HumanFidelityGovernanceAuditEventType.REVIEW_PREPARED,
        outcome: HumanFidelityGovernanceAuditOutcome = HumanFidelityGovernanceAuditOutcome.SUCCEEDED,
        recordedAt: Instant = Instant.parse("2026-09-03T01:00:00Z"),
        detail: String? = null,
    ): HumanFidelityGovernanceAuditRecord {
        val payload = HumanFidelityReviewRecordCodec.payloadSha256(review)
        return HumanFidelityGovernanceAuditRecord(
            HumanFidelityGovernanceAuditIdentity.derive(type, review.reviewerPrincipalId, review.reviewId, review.target, payload, outcome),
            type, recordedAt, review.reviewerPrincipalId, review.reviewId, review.target, payload, outcome, detail,
        )
    }

    private fun occurrence(
        reviewId: HumanFidelityReviewId,
        page: Int,
        reason: String,
        patternId: SystematicDiscrepancyPatternId? = null,
    ): FidelityDiscrepancyOccurrence {
        val block = "Execution page $page — Michael Gary Kellee"
        val charIndex = block.indexOf("Kellee")
        val start = block.codePointCount(0, charIndex)
        val location = FidelityDiscrepancyLocation.fromProviderBlock(
            target.evidenceArtifactId, target.sourceSha256, page, target.preparationIdentity,
            SourceRegionId(page.toString().padStart(64, '0')), target.derivativeGenerationId,
            target.derivativeGenerationSha256, target.derivativeContentSha256,
            SourceRegionId((page + 10).toString().padStart(64, '0')), 0, block, start, start + 6, "Kellee",
        )
        val sourceResolution = HumanSourceResolution.ResolvedAgainstSource(
            "Kellec", fidelityDigest("Kellec"), PageRepresentationId((page + 20).toString().padStart(64, '0')), reviewer,
        )
        val cause = FidelityCauseAssessment(FidelityCauseState.UNKNOWN)
        val id = FidelityDiscrepancyOccurrence.deriveId(
            reviewId, location, FidelityDiscrepancyClassification.TRANSCRIPTION_DIFFERENCE,
            FidelityDiscrepancySeverity.MATERIAL, reason, null, sourceResolution, cause,
        )
        return FidelityDiscrepancyOccurrence(
            id, reviewId, location, FidelityDiscrepancyClassification.TRANSCRIPTION_DIFFERENCE,
            FidelityDiscrepancySeverity.MATERIAL, reason, null, sourceResolution, cause, patternId,
        )
    }
}
