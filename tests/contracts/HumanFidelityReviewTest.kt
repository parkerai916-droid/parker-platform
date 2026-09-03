package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class HumanFidelityReviewTest {
    @Test
    fun `identities are canonical deterministic and malformed values fail closed`() {
        val id = reviewId()
        assertEquals(id, reviewId())
        assertEquals(id.hashCode(), reviewId().hashCode())
        assertFailsWith<IllegalArgumentException> { HumanFidelityReviewId("") }
        assertFailsWith<IllegalArgumentException> { HumanFidelityReviewId("review-${"G".repeat(64)}") }
        assertFailsWith<IllegalArgumentException> { FidelityDiscrepancyId(" ") }
        assertFailsWith<IllegalArgumentException> { SystematicDiscrepancyPatternId("pattern-short") }
        assertNotEquals(reviewId(descriptive = "different"), id)
        assertFailsWith<IllegalArgumentException> { PrincipalId(" ") }
        assertFailsWith<IllegalArgumentException> { OcrSha256Digest("not-a-digest") }
    }

    @Test
    fun `coverage is immutable deterministic and rejects ambiguous scopes`() {
        val sourcePages = mutableListOf(2, 1)
        val coverage = HumanFidelityReviewCoverage(HumanFidelityCoverageKind.FULL_GENERATION, sourcePages)
        sourcePages.clear()
        assertEquals(listOf(1, 2), coverage.reviewedPages)

        assertFailsWith<IllegalArgumentException> {
            HumanFidelityReviewCoverage(HumanFidelityCoverageKind.FULL_GENERATION, listOf(0))
        }
        assertFailsWith<IllegalArgumentException> {
            HumanFidelityReviewCoverage(HumanFidelityCoverageKind.FULL_GENERATION, listOf(1, 1))
        }
        assertFailsWith<IllegalArgumentException> {
            HumanFidelityReviewCoverage(HumanFidelityCoverageKind.PARTIAL, listOf(1))
        }
        assertFailsWith<IllegalArgumentException> {
            HumanFidelityReviewCoverage(
                HumanFidelityCoverageKind.PARTIAL,
                listOf(1),
                listOf(HumanFidelityCharacterScope(1, 0, 0, 3), HumanFidelityCharacterScope(1, 0, 2, 4)),
            )
        }
    }

    @Test
    fun `exact location validates Unicode code-point offsets including non-BMP prefix`() {
        val block = "😀Grantor Michael Gary Kellee"
        val start = block.codePointIndexOf("Kellee")
        val location = location(1, block, start, start + "Kellee".codePointCount(0, 6), "Kellee")

        assertEquals("Kellee", location.originalProviderSubstring)
        assertEquals(6, location.codePointLength)
        assertEquals(start, location.startCodePointInclusive)
        assertNotEquals(block.indexOf("Kellee"), start, "UTF-16 and code-point offsets must differ after a non-BMP character")

        assertFailsWith<IllegalArgumentException> { location(1, block, -1, 2, "😀") }
        assertFailsWith<IllegalArgumentException> { location(0, block, start, start + 6, "Kellee") }
        assertFailsWith<IllegalArgumentException> { location(1, block, start + 6, start, "") }
        assertFailsWith<IllegalArgumentException> { location(1, block, start, start + 6, "Kellec") }
        assertFailsWith<IllegalArgumentException> {
            location(1, block, start, start + 6, "Kellee", fidelityDigest("wrong"))
        }
    }

    @Test
    fun `cause and source resolution remain independent`() {
        val resolved = resolution("Kellec")
        val unknown = FidelityCauseAssessment(FidelityCauseState.UNKNOWN)
        val occurrence = occurrence(location = locationForKellee(1), resolution = resolved, cause = unknown)
        assertEquals(FidelityCauseState.UNKNOWN, occurrence.causeAssessment.state)
        assertEquals("Kellec", (occurrence.sourceResolution as HumanSourceResolution.ResolvedAgainstSource).assertedSourceValue)

        assertFailsWith<IllegalArgumentException> { FidelityCauseAssessment(FidelityCauseState.UNKNOWN, "font") }
        assertFailsWith<IllegalArgumentException> { FidelityCauseAssessment(FidelityCauseState.HYPOTHESISED) }
        assertFailsWith<IllegalArgumentException> { FidelityCauseAssessment(FidelityCauseState.ESTABLISHED, "glyph") }
        assertEquals("visual ambiguity", FidelityCauseAssessment(FidelityCauseState.HYPOTHESISED, "visual ambiguity").mechanism)
        assertEquals(
            "source inspection",
            FidelityCauseAssessment(FidelityCauseState.ESTABLISHED, "mechanism", "source inspection").supportingBasis,
        )
        assertFailsWith<IllegalArgumentException> {
            HumanSourceResolution.ResolvedAgainstSource("Kellec", fidelityDigest("Kellee"), pageRepresentation, reviewer)
        }
    }

    @Test
    fun `classification and severity combinations fail closed`() {
        occurrence(location = locationForKellee(1))
        val uncertainty = occurrence(
            location = locationForKellee(1),
            classification = FidelityDiscrepancyClassification.APPROPRIATE_UNCERTAINTY,
            severity = FidelityDiscrepancySeverity.NON_ERROR_OBSERVATION,
        )
        assertEquals(FidelityDiscrepancySeverity.NON_ERROR_OBSERVATION, uncertainty.severity)

        assertFailsWith<IllegalArgumentException> {
            occurrence(
                location = locationForKellee(1),
                classification = FidelityDiscrepancyClassification.APPROPRIATE_UNCERTAINTY,
                severity = FidelityDiscrepancySeverity.MINOR,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            occurrence(location = locationForKellee(1), severity = FidelityDiscrepancySeverity.NON_ERROR_OBSERVATION)
        }
        assertFailsWith<IllegalArgumentException> {
            occurrence(
                location = locationForKellee(1),
                classification = FidelityDiscrepancyClassification.OTHER_EXPLICITLY_CLASSIFIED,
            )
        }
    }

    @Test
    fun `review verdict coverage and discrepancy facts are exhaustively consistent`() {
        val full = coverage()
        val passId = reviewId(state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS)
        val pass = HumanFidelityReviewRecord(
            passId, target, reviewer, reviewedAt, artifacts, full,
            HumanFidelityReviewState.HUMAN_REVIEWED_PASS, HIGH_FIDELITY, emptyList(),
        )
        assertEquals(HumanFidelityReviewState.HUMAN_REVIEWED_PASS, pass.reviewState)

        assertFailsWith<IllegalArgumentException> {
            HumanFidelityReviewRecord(
                reviewId(state = HumanFidelityReviewState.UNREVIEWED), target, reviewer, reviewedAt, artifacts, full,
                HumanFidelityReviewState.UNREVIEWED, HIGH_FIDELITY, emptyList(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            HumanFidelityReviewRecord(
                passId, target, reviewer, reviewedAt, artifacts, full,
                HumanFidelityReviewState.HUMAN_REVIEWED_PASS, HIGH_FIDELITY, listOf(occurrence(locationForKellee(1), review = passId)),
            )
        }
        val partial = HumanFidelityReviewCoverage(
            HumanFidelityCoverageKind.PARTIAL, listOf(1), listOf(HumanFidelityCharacterScope(1, 0, 0, 1)),
        )
        assertFailsWith<IllegalArgumentException> {
            HumanFidelityReviewRecord(
                reviewId(coverage = partial, state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS),
                target, reviewer, reviewedAt, artifacts, partial,
                HumanFidelityReviewState.HUMAN_REVIEWED_PASS, HIGH_FIDELITY, emptyList(),
            )
        }
    }

    @Test
    fun `R6 fixture represents two material resolved occurrences and one non-authoritative pattern`() {
        val id = reviewId()
        val page1Bare = occurrence(locationForKellee(1), review = id)
        val page5Bare = occurrence(locationForKellee(5), review = id)
        val patternId = SystematicDiscrepancyPattern.deriveId(
            id, listOf(page1Bare.discrepancyId, page5Bare.discrepancyId), PATTERN, reviewer,
        )
        val page1 = occurrence(locationForKellee(1), review = id, patternId = patternId)
        val page5 = occurrence(locationForKellee(5), review = id, patternId = patternId)
        val pattern = SystematicDiscrepancyPattern(
            patternId, id, listOf(page1.discrepancyId, page5.discrepancyId), PATTERN, 2, reviewer,
        )
        val review = HumanFidelityReviewRecord(
            id, target, reviewer, reviewedAt, artifacts, coverage(),
            HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY,
            HIGH_FIDELITY, listOf(page5, page1), listOf(pattern),
        )

        assertEquals(2, review.discrepancyOccurrences.size)
        assertEquals(1, review.systematicPatterns.size)
        assertEquals(listOf(1, 5), review.discrepancyOccurrences.map { it.location.pageNumber }.sorted())
        review.discrepancyOccurrences.forEach {
            assertEquals(FidelityDiscrepancySeverity.MATERIAL, it.severity)
            assertEquals(FidelityCauseState.UNKNOWN, it.causeAssessment.state)
            assertEquals("Kellec", (it.sourceResolution as HumanSourceResolution.ResolvedAgainstSource).assertedSourceValue)
        }
        assertEquals(HIGH_FIDELITY, review.descriptiveFidelity)
        assertEquals(HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY, review.reviewState)
        assertFalse(SystematicDiscrepancyPattern::class.members.any {
            it.name.contains("replacement", ignoreCase = true) || it.name.contains("correction", ignoreCase = true)
        })
    }

    @Test
    fun `patterns reject singleton duplicate and incorrect occurrence counts`() {
        val id = reviewId()
        val first = occurrence(locationForKellee(1), review = id)
        val second = occurrence(locationForKellee(5), review = id)
        val patternId = SystematicDiscrepancyPattern.deriveId(id, listOf(first.discrepancyId, second.discrepancyId), PATTERN, reviewer)
        assertFailsWith<IllegalArgumentException> {
            SystematicDiscrepancyPattern(patternId, id, listOf(first.discrepancyId), PATTERN, 1, reviewer)
        }
        assertFailsWith<IllegalArgumentException> {
            SystematicDiscrepancyPattern(patternId, id, listOf(first.discrepancyId, first.discrepancyId), PATTERN, 2, reviewer)
        }
        assertFailsWith<IllegalArgumentException> {
            SystematicDiscrepancyPattern(patternId, id, listOf(first.discrepancyId, second.discrepancyId), PATTERN, 3, reviewer)
        }
    }

    @Test
    fun `supersession and adjudication values encode no timestamp precedence`() {
        val current = reviewId()
        val predecessor = HumanFidelityReviewId("review-${"a".repeat(64)}")
        assertEquals(predecessor, HumanFidelityReviewSupersession(predecessor, target).predecessorReviewId)
        assertFailsWith<IllegalArgumentException> {
            HumanFidelityReviewRecord(
                current, target, reviewer, reviewedAt, artifacts, coverage(),
                HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY, HIGH_FIDELITY,
                listOf(occurrence(locationForKellee(1), review = current)),
                supersession = HumanFidelityReviewSupersession(current, target),
            )
        }
        val other = HumanFidelityReviewId("review-${"b".repeat(64)}")
        assertFailsWith<IllegalArgumentException> {
            HumanFidelityReviewAdjudicationReference(setOf(predecessor, other), current, target)
        }
        assertEquals(false, HumanFidelityReviewSupersession::class.members.any { it.name.contains("time", ignoreCase = true) })
    }

    @Test
    fun `projection contracts require explicit fail-closed eligibility`() {
        val unreviewed = EffectiveHumanFidelityReviewProjection(
            target, HumanFidelityReviewState.UNREVIEWED, null, emptySet(), emptySet(),
            SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED, SourceConfirmedDenialReason.UNREVIEWED),
        )
        assertEquals(SourceConfirmedEligibilityState.DENIED, unreviewed.eligibility.state)
        assertFailsWith<IllegalArgumentException> { SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED) }
        assertFailsWith<IllegalArgumentException> {
            SourceConfirmedEligibility(SourceConfirmedEligibilityState.ALLOWED, SourceConfirmedDenialReason.MATERIAL_DISCREPANCY)
        }
        val conflictIds = setOf(reviewId(), HumanFidelityReviewId("review-${"c".repeat(64)}"))
        val conflict = EffectiveHumanFidelityReviewProjection(
            target, HumanFidelityReviewState.HUMAN_REVIEW_CONFLICT, coverage(), conflictIds, emptySet(),
            SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED, SourceConfirmedDenialReason.UNRESOLVED_CONFLICT),
        )
        assertEquals(HumanFidelityReviewState.HUMAN_REVIEW_CONFLICT, conflict.effectiveState)
    }

    @Test
    fun `review rejects target reviewer and pattern binding contradictions`() {
        val id = reviewId()
        val wrongTarget = target.copy(sourceSha256 = OcrSha256Digest("f".repeat(64)))
        val wrongLocationOccurrence = occurrence(locationForKellee(1), review = id).let {
            val location = FidelityDiscrepancyLocation.fromProviderBlock(
                wrongTarget.evidenceArtifactId, wrongTarget.sourceSha256, 1, wrongTarget.preparationIdentity,
                SourceRegionId("1".padStart(64, '0')), wrongTarget.derivativeGenerationId,
                wrongTarget.derivativeGenerationSha256, wrongTarget.derivativeContentSha256,
                SourceRegionId("11".padStart(64, '0')), 0, "Kellee", 0, 6, "Kellee",
            )
            occurrence(location, review = id)
        }
        assertFailsWith<IllegalArgumentException> {
            HumanFidelityReviewRecord(
                id, target, reviewer, reviewedAt, artifacts, coverage(),
                HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY,
                HIGH_FIDELITY, listOf(wrongLocationOccurrence),
            )
        }

        val anotherReviewer = PrincipalId("owner.other")
        val wronglyResolved = occurrence(
            locationForKellee(1), review = id,
            resolution = HumanSourceResolution.ResolvedAgainstSource(
                "Kellec", fidelityDigest("Kellec"), pageRepresentation, anotherReviewer,
            ),
        )
        assertFailsWith<IllegalArgumentException> {
            HumanFidelityReviewRecord(
                id, target, reviewer, reviewedAt, artifacts, coverage(),
                HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY,
                HIGH_FIDELITY, listOf(wronglyResolved),
            )
        }
    }

    @Test
    fun `record and projection defensively copy mutable collections`() {
        val id = reviewId()
        val occurrences = mutableListOf(occurrence(locationForKellee(1), review = id))
        val record = HumanFidelityReviewRecord(
            id, target, reviewer, reviewedAt, artifacts, coverage(),
            HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY,
            HIGH_FIDELITY, occurrences,
        )
        occurrences.clear()
        assertEquals(1, record.discrepancyOccurrences.size)

        val reviewIds = mutableListOf(id)
        val discrepancyIds = mutableListOf(record.discrepancyOccurrences.single().discrepancyId)
        val projection = EffectiveHumanFidelityReviewProjection(
            target, HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY, coverage(),
            reviewIds, discrepancyIds,
            SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED, SourceConfirmedDenialReason.MATERIAL_DISCREPANCY),
        )
        reviewIds.clear()
        discrepancyIds.clear()
        assertEquals(1, projection.applicableReviewIds.size)
        assertEquals(1, projection.discrepancyIds.size)
    }

    private fun reviewId(
        coverage: HumanFidelityReviewCoverage = coverage(),
        state: HumanFidelityReviewState = HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY,
        descriptive: String = HIGH_FIDELITY,
    ) = HumanFidelityReviewRecord.deriveId(target, reviewer, reviewedAt, artifacts, coverage, state, descriptive)

    private fun occurrence(
        location: FidelityDiscrepancyLocation,
        review: HumanFidelityReviewId = reviewId(),
        classification: FidelityDiscrepancyClassification = FidelityDiscrepancyClassification.TRANSCRIPTION_DIFFERENCE,
        severity: FidelityDiscrepancySeverity = FidelityDiscrepancySeverity.MATERIAL,
        resolution: HumanSourceResolution = resolution("Kellec"),
        cause: FidelityCauseAssessment = FidelityCauseAssessment(FidelityCauseState.UNKNOWN),
        patternId: SystematicDiscrepancyPatternId? = null,
    ): FidelityDiscrepancyOccurrence {
        val reason = "Identity-bearing proper name differs from the exact source"
        val detail: String? = null
        val discrepancyId = FidelityDiscrepancyOccurrence.deriveId(
            review, location, classification, severity, reason, detail, resolution, cause,
        )
        return FidelityDiscrepancyOccurrence(
            discrepancyId, review, location, classification, severity, reason, detail,
            resolution, cause, patternId,
        )
    }

    private fun locationForKellee(page: Int): FidelityDiscrepancyLocation {
        val block = "Execution page $page — Michael Gary Kellee"
        val start = block.codePointIndexOf("Kellee")
        return location(page, block, start, start + 6, "Kellee")
    }

    private fun location(
        page: Int,
        block: String,
        start: Int,
        end: Int,
        expected: String,
        expectedDigest: OcrSha256Digest = fidelityDigest(expected),
    ) = FidelityDiscrepancyLocation.fromProviderBlock(
        target.evidenceArtifactId, target.sourceSha256, page, target.preparationIdentity,
        SourceRegionId(page.toString().padStart(64, '0')), target.derivativeGenerationId,
        target.derivativeGenerationSha256, target.derivativeContentSha256,
        SourceRegionId((page + 10).toString().padStart(64, '0')), 0, block, start, end, expected, expectedDigest,
    )

    private fun resolution(value: String) = HumanSourceResolution.ResolvedAgainstSource(
        value, fidelityDigest(value), pageRepresentation, reviewer,
    )

    companion object {
        private const val HIGH_FIDELITY = "high fidelity overall with one systematic material proper-name discrepancy pattern"
        private const val PATTERN = "Kellec in the source is represented as Kellee in two explicit locations"
        private val reviewer = PrincipalId("owner.steven-francis-mctague")
        private val reviewedAt = Instant.parse("2026-09-03T00:00:00Z")
        private val pageRepresentation = PageRepresentationId("4".repeat(64))
        private val artifacts = HumanFidelityReviewArtifacts(
            OcrSha256Digest("8e7928c671cd36c7a4517dc5d9429706c46efb65c565e948684d6c3e7c8773a4"),
            OcrSha256Digest("2d47f50e0f2915bd0e18e914eac4bd5abc879cf5419969d482b2b7f6ff6b1293"),
        )
        private val target = HumanFidelityReviewTarget(
            EvidenceArtifactId("evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9"),
            OcrSha256Digest("5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e"),
            OcrSha256Digest("85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f"),
            DerivativeGenerationId("region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6"),
            OcrSha256Digest("9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14"),
            OcrSha256Digest("18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb"),
        )

        private fun coverage() = HumanFidelityReviewCoverage(HumanFidelityCoverageKind.FULL_GENERATION, (1..5).toList())
    }
}

private fun String.codePointIndexOf(needle: String): Int {
    val charIndex = indexOf(needle)
    require(charIndex >= 0)
    return codePointCount(0, charIndex)
}
