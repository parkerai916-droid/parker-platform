package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import parker.core.interfaces.*

class DefaultEffectiveHumanFidelityReviewProjectorTest {
    @Test
    fun `zero reviews project unreviewed and denied`() = runTest {
        val result = projector().project(target, purpose).projected()
        assertEquals(HumanFidelityReviewState.UNREVIEWED, result.projection.effectiveState)
        assertNull(result.projection.coverage)
        assertEquals(denied(SourceConfirmedDenialReason.UNREVIEWED), result.projection.eligibility)
        assertEquals(0, result.materialDiscrepancyCount)
    }

    @Test
    fun `full pass is eligible only for explicit source confirmed whole generation use`() = runTest {
        val pass = review(state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS)
        val result = projector(pass).project(target, purpose).projected()
        assertEquals(HumanFidelityReviewState.HUMAN_REVIEWED_PASS, result.projection.effectiveState)
        assertEquals(SourceConfirmedEligibility(SourceConfirmedEligibilityState.ALLOWED), result.projection.eligibility)
        assertEquals(purpose, result.purpose)
    }

    @Test
    fun `exact canonical R6 review projects material denial without changing raw provider bytes`() = runTest {
        val providerBytes = "Michael Gary Kellee".encodeToByteArray()
        val before = providerBytes.copyOf()
        val r6 = review(materialPages = listOf(1, 5), patterns = true)
        assertEquals("review-3cf3186ca166acb0f4b6331ca574926dc874225247b296fb972666504992ea6e", r6.reviewId.value)

        val result = projector(r6).project(target, purpose).projected()

        assertEquals(HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY, result.projection.effectiveState)
        assertEquals(HumanFidelityCoverageKind.FULL_GENERATION, result.projection.coverage?.kind)
        assertEquals(2, result.materialDiscrepancyCount)
        assertEquals(1, result.systematicPatternCount)
        assertFalse(result.unresolvedConflict)
        assertEquals(denied(SourceConfirmedDenialReason.MATERIAL_DISCREPANCY), result.projection.eligibility)
        assertTrue(r6.discrepancyOccurrences.all { it.causeAssessment.state == FidelityCauseState.UNKNOWN })
        assertEquals(setOf("Kellec"), r6.discrepancyOccurrences.map {
            (it.sourceResolution as HumanSourceResolution.ResolvedAgainstSource).assertedSourceValue
        }.toSet())
        assertContentEquals(before, providerBytes, "projection must not alter independently retrievable provider bytes")
    }

    @Test
    fun `partial review denies whole generation use before discrepancy severity`() = runTest {
        val partial = HumanFidelityReviewCoverage(
            HumanFidelityCoverageKind.PARTIAL,
            listOf(1),
            listOf(HumanFidelityCharacterScope(1, 0, 0, 6)),
        )
        val result = projector(review(coverage = partial, materialPages = listOf(1)))
            .project(target, purpose).projected()
        assertEquals(HumanFidelityCoverageKind.PARTIAL, result.projection.coverage?.kind)
        assertEquals(denied(SourceConfirmedDenialReason.PARTIAL_COVERAGE), result.projection.eligibility)
    }

    @Test
    fun `minor discrepancy has no implicit eligibility rule and fails closed`() = runTest {
        val result = projector(review(materialPages = listOf(1), severity = FidelityDiscrepancySeverity.MINOR))
            .project(target, purpose).projected()
        assertEquals(denied(SourceConfirmedDenialReason.MALFORMED_OR_UNSUPPORTED_STATE), result.projection.eligibility)
    }

    @Test
    fun `multiple unsuperseded reviews conflict regardless of timestamp or storage order`() = runTest {
        val first = review(state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS, reviewedAt = Instant.parse("2026-09-01T00:00:00Z"))
        val second = review(state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS, reviewedAt = Instant.parse("2026-09-02T00:00:00Z"))
        val forward = projector(first, second).project(target, purpose).projected()
        val reverse = projector(second, first).project(target, purpose).projected()
        for (result in listOf(forward, reverse)) {
            assertEquals(HumanFidelityReviewState.HUMAN_REVIEW_CONFLICT, result.projection.effectiveState)
            assertTrue(result.unresolvedConflict)
            assertEquals(denied(SourceConfirmedDenialReason.UNRESOLVED_CONFLICT), result.projection.eligibility)
            assertEquals(setOf(first.reviewId, second.reviewId), result.projection.applicableReviewIds)
        }
    }

    @Test
    fun `one explicit same-target successor deterministically supersedes its predecessor`() = runTest {
        val predecessor = review(state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS, reviewedAt = Instant.parse("2026-09-01T00:00:00Z"))
        val successor = review(
            state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS,
            reviewedAt = Instant.parse("2026-09-02T00:00:00Z"),
            supersession = HumanFidelityReviewSupersession(predecessor.reviewId, target),
        )
        val result = projector(successor, predecessor).project(target, purpose).projected()
        assertEquals(HumanFidelityReviewState.HUMAN_REVIEWED_PASS, result.projection.effectiveState)
        assertEquals(setOf(successor.reviewId), result.projection.applicableReviewIds)
    }

    @Test
    fun `supersession cycle projects conflict and fails closed`() = runTest {
        val firstBase = review(state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS, reviewedAt = Instant.parse("2026-09-01T00:00:00Z"))
        val secondBase = review(state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS, reviewedAt = Instant.parse("2026-09-02T00:00:00Z"))
        val first = review(
            state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS,
            reviewedAt = Instant.parse("2026-09-01T00:00:00Z"),
            supersession = HumanFidelityReviewSupersession(secondBase.reviewId, target),
        )
        val second = review(
            state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS,
            reviewedAt = Instant.parse("2026-09-02T00:00:00Z"),
            supersession = HumanFidelityReviewSupersession(firstBase.reviewId, target),
        )
        val cycle = projector(first, second).project(target, purpose).projected()
        assertEquals(HumanFidelityReviewState.HUMAN_REVIEW_CONFLICT, cycle.projection.effectiveState)
        assertEquals(denied(SourceConfirmedDenialReason.UNRESOLVED_CONFLICT), cycle.projection.eligibility)
    }

    @Test
    fun `dangling relationship and corrupt storage fail closed`() = runTest {
        val missing = HumanFidelityReviewId("review-${"f".repeat(64)}")
        val dangling = review(
            state = HumanFidelityReviewState.HUMAN_REVIEWED_PASS,
            supersession = HumanFidelityReviewSupersession(missing, target),
        )
        assertIs<EffectiveHumanFidelityReviewProjectionOutcome.FailedClosed>(
            projector(dangling).project(target, purpose),
        )
        val corrupt = DefaultEffectiveHumanFidelityReviewProjector(object : HumanFidelityReviewStorage by ReadOnlyStorage(emptyList()) {
            override suspend fun listForExactTarget(target: HumanFidelityReviewTarget): List<HumanFidelityReviewRecord> =
                throw HumanFidelityReviewStorageException.CorruptRecord(dangling.reviewId, "test corruption")
        })
        val failed = assertIs<EffectiveHumanFidelityReviewProjectionOutcome.FailedClosed>(corrupt.project(target, purpose))
        assertEquals(denied(SourceConfirmedDenialReason.MALFORMED_OR_UNSUPPORTED_STATE), failed.eligibility)
    }

    @Test
    fun `projector is structurally read only and provider independent`() {
        assertEquals(
            setOf("HumanFidelityReviewStorage"),
            DefaultEffectiveHumanFidelityReviewProjector::class.java.declaredFields.map { it.type.simpleName }.toSet(),
        )
        assertEquals(setOf("project"), EffectiveHumanFidelityReviewProjector::class.members
            .filter { it.isAbstract }.map { it.name }.toSet())
    }

    private fun projector(vararg records: HumanFidelityReviewRecord) =
        DefaultEffectiveHumanFidelityReviewProjector(ReadOnlyStorage(records.toList()))

    private fun review(
        state: HumanFidelityReviewState = HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY,
        coverage: HumanFidelityReviewCoverage = fullCoverage,
        materialPages: List<Int> = if (state == HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY) listOf(1, 5) else emptyList(),
        patterns: Boolean = false,
        severity: FidelityDiscrepancySeverity = FidelityDiscrepancySeverity.MATERIAL,
        reviewedAt: Instant = Instant.parse("2026-09-03T00:00:00Z"),
        supersession: HumanFidelityReviewSupersession? = null,
    ): HumanFidelityReviewRecord {
        val reviewId = HumanFidelityReviewRecord.deriveId(target, reviewer, reviewedAt, artifacts, coverage, state, fidelity)
        val bare = materialPages.map { occurrence(reviewId, it, severity, null) }
        val patternId = if (patterns) SystematicDiscrepancyPattern.deriveId(reviewId, bare.map { it.discrepancyId }, patternText, reviewer) else null
        val occurrences = materialPages.map { occurrence(reviewId, it, severity, patternId) }
        val pattern = patternId?.let {
            listOf(SystematicDiscrepancyPattern(it, reviewId, occurrences.map { occurrence -> occurrence.discrepancyId }, patternText, 2, reviewer))
        }.orEmpty()
        return HumanFidelityReviewRecord(
            reviewId, target, reviewer, reviewedAt, artifacts, coverage, state, fidelity,
            occurrences, pattern, supersession, null,
        )
    }

    private fun occurrence(
        reviewId: HumanFidelityReviewId,
        page: Int,
        severity: FidelityDiscrepancySeverity,
        patternId: SystematicDiscrepancyPatternId?,
    ): FidelityDiscrepancyOccurrence {
        val block = "Michael Gary Kellee"
        val start = block.codePointCount(0, block.indexOf("Kellee"))
        val location = FidelityDiscrepancyLocation.fromProviderBlock(
            target.evidenceArtifactId, target.sourceSha256, page, target.preparationIdentity,
            SourceRegionId((page + 100).toString().padStart(64, '0')), target.derivativeGenerationId,
            target.derivativeGenerationSha256, target.derivativeContentSha256,
            SourceRegionId((page + 200).toString().padStart(64, '0')), 0, block, start, start + 6, "Kellee",
        )
        val resolution = HumanSourceResolution.ResolvedAgainstSource(
            "Kellec", fidelityDigest("Kellec"), PageRepresentationId((page + 300).toString().padStart(64, '0')), reviewer,
        )
        val cause = FidelityCauseAssessment(FidelityCauseState.UNKNOWN)
        val reason = "Identity-bearing proper name differs from the exact source"
        val id = FidelityDiscrepancyOccurrence.deriveId(
            reviewId, location, FidelityDiscrepancyClassification.TRANSCRIPTION_DIFFERENCE,
            severity, reason, null, resolution, cause,
        )
        return FidelityDiscrepancyOccurrence(
            id, reviewId, location, FidelityDiscrepancyClassification.TRANSCRIPTION_DIFFERENCE,
            severity, reason, null, resolution, cause, patternId,
        )
    }

    private class ReadOnlyStorage(private val records: List<HumanFidelityReviewRecord>) : HumanFidelityReviewStorage {
        override suspend fun listForExactTarget(target: HumanFidelityReviewTarget) = records
        override suspend fun retrieve(reviewId: HumanFidelityReviewId) = records.singleOrNull { it.reviewId == reviewId }
        override suspend fun prepare(record: HumanFidelityReviewRecord): HumanFidelityReviewPreparationResult = error("read-only")
        override suspend fun publishPrepared(reviewId: HumanFidelityReviewId): HumanFidelityReviewPublicationResult = error("read-only")
    }

    private fun EffectiveHumanFidelityReviewProjectionOutcome.projected() =
        assertIs<EffectiveHumanFidelityReviewProjectionOutcome.Projected>(this).summary

    private fun denied(reason: SourceConfirmedDenialReason) =
        SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED, reason)

    private companion object {
        val purpose = HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION
        val reviewer = PrincipalId("user.steve")
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
        val fullCoverage = HumanFidelityReviewCoverage(HumanFidelityCoverageKind.FULL_GENERATION, (1..5).toList())
        const val fidelity = "high overall fidelity"
        const val patternText = "Kellec in the source is represented as Kellee at the two exact reviewed locations"
    }
}
