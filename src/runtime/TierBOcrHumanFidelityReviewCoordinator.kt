package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import parker.composition.HumanFidelityReviewExactTargetRegistrar
import parker.core.interfaces.*

/**
 * UI-INGESTION / HFR-2..HFR-5 (collapsed): the exact-target construction and recording path
 * `docs/architecture/HUMAN_FIDELITY_REVIEW_OWNER_UI_EXPOSURE_SCOPE_LOCK_AMENDMENT.md` authorised --
 * the Tier B OCR / External Transcription analogue of what [TierAContentRetrievalCoordinator]
 * already does for the Ordinary Region pipeline. Reuses every existing Human Fidelity Review
 * domain type, storage, audit, permission policy, Authorization Purpose, and recording/projection
 * service unchanged; this class introduces no parallel review model. It mirrors
 * [TierAContentRetrievalCoordinator]'s own independent-retrieval shape rather than delegating to
 * [TierBOcrContentRetrievalCoordinator], because a [HumanFidelityReviewTarget] requires the raw
 * [DerivativeContentEntry] (for [derivativeContentSha256]), which that coordinator does not expose.
 *
 * **Disclosed scope narrowing, not a fabrication:** [FidelityDiscrepancyLocation] requires a
 * [SourceRegionId] for both `preparationRegionId`/`derivativeRegionId`. That type's own governed
 * meaning (`SourceRegionGeometry.kt`) is a pixel-derived sub-page region from the Ordinary Region
 * pipeline's own region-derivation subsystem, which the Tier B OCR / External Transcription
 * pipeline has no equivalent of -- it submits whole pages, never sub-page regions. This class
 * supplies a deterministic, disclosed "whole page is the one and only region" identity
 * ([wholePageRegionId]) as an opaque exact-binding key only -- it asserts no pixel geometry, crop
 * digest, or structural class, and is never presented as pixel-derived provenance. Similarly,
 * [HumanSourceResolution.ResolvedAgainstSource] requires a [PageRepresentationId], itself tied to
 * a specific rendered-page-image profile Tier B OCR never produces; fabricating one there would
 * assert a specific canonical rendering was consulted when none was. This class therefore does not
 * support [HumanSourceResolution.ResolvedAgainstSource] for Tier B discrepancies -- every
 * discrepancy this class records carries [HumanSourceResolution.Unresolved], a fully valid,
 * first-class state under the existing frozen model (R6T §9: "a discrepancy is either detected but
 * source-unresolved, or resolved against an exact reviewed source location").
 */
internal class TierBOcrHumanFidelityReviewCoordinator(
    private val generationStorage: DerivativeGenerationStorage,
    private val contentStorage: DerivativeContentStorage,
    private val recordingService: GovernedHumanFidelityReviewRecordingService,
    private val exactTargetRegistrar: HumanFidelityReviewExactTargetRegistrar,
    private val projector: EffectiveHumanFidelityReviewProjector,
    private val ownerPrincipalId: PrincipalId,
    private val clock: () -> Instant = Instant::now,
) {
    private suspend fun resolveTarget(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): TierBHumanFidelityReviewTargetResolution {
        val record = generationStorage.retrieve(derivativeGenerationId)
            ?: return TierBHumanFidelityReviewTargetResolution.UnknownGeneration
        if (record.rootSourceEvidenceArtifactId != evidenceArtifactId) {
            return TierBHumanFidelityReviewTargetResolution.SourceMismatch
        }
        if (DerivativeTransformation.OCR !in record.transformationHistory) {
            return TierBHumanFidelityReviewTargetResolution.WrongDerivativeKind
        }
        val entry = try {
            contentStorage.retrieve(derivativeGenerationId)
        } catch (e: Exception) {
            return TierBHumanFidelityReviewTargetResolution.ContentCorrupt(e.message ?: "corrupt")
        } ?: return TierBHumanFidelityReviewTargetResolution.ContentMissing
        if (entry.rootSourceEvidenceArtifactId != evidenceArtifactId) {
            return TierBHumanFidelityReviewTargetResolution.SourceMismatch
        }
        val extracted = when (val payload = entry.payload) {
            is TierADerivativePayload.Ocr -> payload.value
            else -> return TierBHumanFidelityReviewTargetResolution.WrongDerivativeKind
        }
        val processing = extracted.processingProvenance
            ?: return TierBHumanFidelityReviewTargetResolution.MissingProcessingProvenance
        val target = HumanFidelityReviewTarget(
            evidenceArtifactId = evidenceArtifactId,
            sourceSha256 = processing.sourceManifestSha256,
            preparationIdentity = processing.representationSha256,
            derivativeGenerationId = derivativeGenerationId,
            derivativeGenerationSha256 = OcrSha256Digest(sha256(DerivativeGenerationRecordCodec.encode(record))),
            derivativeContentSha256 = OcrSha256Digest(sha256(DerivativeContentCodec.encode(entry))),
        )
        return TierBHumanFidelityReviewTargetResolution.Resolved(target, extracted.segments)
    }

    /** Read-only, exact-target. Never invokes a provider; never mutates the derivative or evidence. */
    suspend fun recordReview(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
        submission: TierBHumanFidelityReviewSubmission,
    ): TierBHumanFidelityReviewRecordingOutcome {
        val resolution = resolveTarget(evidenceArtifactId, derivativeGenerationId)
        val resolved = resolution as? TierBHumanFidelityReviewTargetResolution.Resolved
            ?: return TierBHumanFidelityReviewRecordingOutcome.TargetResolutionFailed(resolution)
        val target = resolved.target
        val segments = resolved.segments

        if (submission.reviewOutcome != HumanFidelityReviewState.HUMAN_REVIEWED_PASS &&
            submission.reviewOutcome != HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY
        ) {
            return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission(
                "reviewOutcome must be HUMAN_REVIEWED_PASS or HUMAN_REVIEWED_WITH_DISCREPANCY",
            )
        }
        if ((submission.reviewOutcome == HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY) != submission.discrepancies.isNotEmpty()) {
            return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission(
                "HUMAN_REVIEWED_PASS must carry no discrepancies; HUMAN_REVIEWED_WITH_DISCREPANCY requires at least one",
            )
        }
        val coverage = try {
            HumanFidelityReviewCoverage(HumanFidelityCoverageKind.FULL_GENERATION, submission.reviewedPages)
        } catch (e: IllegalArgumentException) {
            return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission(e.message ?: "invalid review coverage")
        }

        val reviewedAt = clock()
        val canonicalSubmission = canonicalSubmissionString(target, submission, reviewedAt)
        val submissionDigest = OcrSha256Digest(sha256(canonicalSubmission))
        // No separate offline worksheet exists for this Owner UI submission path -- the exact,
        // canonical digest of the owner's own submitted review facts serves as both required
        // artifact digests, honestly representing "this is the one record of what was submitted",
        // never a fabricated second document.
        val artifacts = HumanFidelityReviewArtifacts(worksheetSha256 = submissionDigest, ownerReviewRecordSha256 = submissionDigest)

        val reviewId = try {
            HumanFidelityReviewRecord.deriveId(
                target, ownerPrincipalId, reviewedAt, artifacts, coverage, submission.reviewOutcome, submission.descriptiveFidelity,
            )
        } catch (e: IllegalArgumentException) {
            return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission(e.message ?: "invalid review facts")
        }

        val discrepancyOccurrences = mutableListOf<FidelityDiscrepancyOccurrence>()
        for (spec in submission.discrepancies) {
            val segmentText = segments.firstOrNull { it.pageNumber == spec.pageNumber }?.text
                ?: return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission("no admitted segment for page ${spec.pageNumber}")
            val isMissingSourceText = spec.classification == FidelityDiscrepancyClassification.MISSING_SOURCE_TEXT
            // Owner-facing input is the exact text itself, never a code-point range: this coordinator
            // locates it within the exact admitted page text server-side, and fails closed (never
            // guesses) if it isn't found or isn't unique -- the owner never needs to understand or
            // construct Parker's internal Unicode code-point encoding to record a discrepancy.
            if (!isMissingSourceText && spec.exactText.isBlank()) {
                return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission(
                    "the exact incorrect text is required for page ${spec.pageNumber}",
                )
            }
            // MISSING_SOURCE_TEXT alone may target a zero-width insertion point (R6T's own factory
            // permits start == end): an empty exactText means "missing at the very start of the page";
            // otherwise exactText is read as the text immediately before the missing content.
            val charIndex = if (isMissingSourceText && spec.exactText.isEmpty()) 0 else segmentText.indexOf(spec.exactText)
            if (charIndex < 0) {
                return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission(
                    "the text \"${spec.exactText}\" was not found on page ${spec.pageNumber}; it must match the transcription exactly",
                )
            }
            if (spec.exactText.isNotEmpty() && segmentText.lastIndexOf(spec.exactText) != charIndex) {
                return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission(
                    "the text \"${spec.exactText}\" appears more than once on page ${spec.pageNumber}; " +
                        "include more surrounding text to identify the exact location",
                )
            }
            val startChar = if (isMissingSourceText) charIndex + spec.exactText.length else charIndex
            val endChar = charIndex + spec.exactText.length
            val startCodePoint = segmentText.codePointCount(0, startChar)
            val endCodePoint = if (isMissingSourceText) startCodePoint else segmentText.codePointCount(0, endChar)
            val exactSubstring = if (isMissingSourceText) "" else spec.exactText
            val regionId = wholePageRegionId(target, spec.pageNumber)
            val location = try {
                FidelityDiscrepancyLocation.fromProviderBlock(
                    evidenceArtifactId = target.evidenceArtifactId,
                    sourceSha256 = target.sourceSha256,
                    pageNumber = spec.pageNumber,
                    preparationIdentity = target.preparationIdentity,
                    preparationRegionId = regionId,
                    derivativeGenerationId = target.derivativeGenerationId,
                    derivativeGenerationSha256 = target.derivativeGenerationSha256,
                    derivativeContentSha256 = target.derivativeContentSha256,
                    derivativeRegionId = regionId,
                    transcriptionBlockIndex = 0,
                    providerBlock = segmentText,
                    startCodePointInclusive = startCodePoint,
                    endCodePointExclusive = endCodePoint,
                    expectedOriginalProviderSubstring = exactSubstring,
                )
            } catch (e: IllegalArgumentException) {
                return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission(e.message ?: "invalid discrepancy location")
            }
            val causeAssessment = FidelityCauseAssessment(FidelityCauseState.UNKNOWN)
            val discrepancyId = try {
                FidelityDiscrepancyOccurrence.deriveId(
                    reviewId, location, spec.classification, spec.severity, spec.reason,
                    spec.explicitClassificationDetail, HumanSourceResolution.Unresolved, causeAssessment,
                )
            } catch (e: IllegalArgumentException) {
                return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission(e.message ?: "invalid discrepancy facts")
            }
            try {
                discrepancyOccurrences.add(
                    FidelityDiscrepancyOccurrence(
                        discrepancyId, reviewId, location, spec.classification, spec.severity, spec.reason,
                        spec.explicitClassificationDetail, HumanSourceResolution.Unresolved, causeAssessment,
                    ),
                )
            } catch (e: IllegalArgumentException) {
                return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission(e.message ?: "invalid discrepancy occurrence")
            }
        }

        val review = try {
            HumanFidelityReviewRecord(
                reviewId, target, ownerPrincipalId, reviewedAt, artifacts, coverage,
                submission.reviewOutcome, submission.descriptiveFidelity, discrepancyOccurrences,
            )
        } catch (e: IllegalArgumentException) {
            return TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission(e.message ?: "invalid review record")
        }

        try {
            exactTargetRegistrar.register(target)
        } catch (e: Exception) {
            return TierBHumanFidelityReviewRecordingOutcome.Failure(GovernedHumanFidelityReviewRecordingFailureReason.AUTHORITY_EVALUATION_FAILED)
        }
        val authority = HumanFidelityReviewRecordingAuthorityScope(ownerPrincipalId, HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE, target)
        return when (val result = recordingService.record(GovernedHumanFidelityReviewRecordingRequest(review, authority))) {
            is GovernedHumanFidelityReviewRecordingResult.Recorded -> TierBHumanFidelityReviewRecordingOutcome.Recorded(result.reviewId)
            is GovernedHumanFidelityReviewRecordingResult.AlreadyRecorded -> TierBHumanFidelityReviewRecordingOutcome.AlreadyRecorded(result.reviewId)
            is GovernedHumanFidelityReviewRecordingResult.AuthorizationDenied -> TierBHumanFidelityReviewRecordingOutcome.AuthorizationDenied(result.reason)
            is GovernedHumanFidelityReviewRecordingResult.Failure -> TierBHumanFidelityReviewRecordingOutcome.Failure(result.reason)
        }
    }

    /** Read-only. Never invokes a provider; never mutates anything. */
    suspend fun projectEffectiveReview(
        evidenceArtifactId: EvidenceArtifactId,
        derivativeGenerationId: DerivativeGenerationId,
    ): TierBEffectiveHumanFidelityReviewOutcome {
        val resolution = resolveTarget(evidenceArtifactId, derivativeGenerationId)
        val resolved = resolution as? TierBHumanFidelityReviewTargetResolution.Resolved
            ?: return TierBEffectiveHumanFidelityReviewOutcome.TargetResolutionFailed(resolution)
        return when (val outcome = projector.project(resolved.target, HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION)) {
            is EffectiveHumanFidelityReviewProjectionOutcome.Projected -> TierBEffectiveHumanFidelityReviewOutcome.Projected(outcome.summary)
            is EffectiveHumanFidelityReviewProjectionOutcome.FailedClosed -> TierBEffectiveHumanFidelityReviewOutcome.FailedClosed
        }
    }

    /** Deterministic, disclosed "whole page is the one region" identity -- see class KDoc. */
    private fun wholePageRegionId(target: HumanFidelityReviewTarget, pageNumber: Int): SourceRegionId =
        SourceRegionId(sha256("whole-page-region-v1 ${target.sourceSha256.value} $pageNumber"))

    private fun canonicalSubmissionString(
        target: HumanFidelityReviewTarget,
        submission: TierBHumanFidelityReviewSubmission,
        reviewedAt: Instant,
    ): String = listOf(
        target.evidenceArtifactId.value, target.sourceSha256.value, target.preparationIdentity.value,
        target.derivativeGenerationId.value, target.derivativeGenerationSha256.value, target.derivativeContentSha256.value,
        submission.reviewOutcome.name, submission.reviewedPages.sorted().joinToString(","), submission.descriptiveFidelity,
        reviewedAt.toString(),
        submission.discrepancies.joinToString(";") { d ->
            listOf(
                d.pageNumber, d.exactText, d.classification.name,
                d.severity.name, d.reason, d.explicitClassificationDetail ?: "",
            ).joinToString(",")
        },
    ).joinToString(" ")

    private fun sha256(value: String): String = sha256(value.toByteArray(Charsets.UTF_8))
    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

sealed interface TierBHumanFidelityReviewTargetResolution {
    data class Resolved(val target: HumanFidelityReviewTarget, val segments: List<OcrRecognitionSegment>) : TierBHumanFidelityReviewTargetResolution
    data object UnknownGeneration : TierBHumanFidelityReviewTargetResolution
    data object SourceMismatch : TierBHumanFidelityReviewTargetResolution
    data object WrongDerivativeKind : TierBHumanFidelityReviewTargetResolution
    data object ContentMissing : TierBHumanFidelityReviewTargetResolution
    data object MissingProcessingProvenance : TierBHumanFidelityReviewTargetResolution
    data class ContentCorrupt(val reason: String) : TierBHumanFidelityReviewTargetResolution
}

/**
 * Owner-facing submission shape only -- never a parallel review model. Every field maps directly
 * onto the existing, frozen [HumanFidelityReviewRecord]/[HumanFidelityReviewCoverage]/
 * [FidelityDiscrepancyOccurrence] contracts; this type exists solely to cross the HTTP boundary
 * before those exact governed types are constructed.
 */
data class TierBHumanFidelityReviewSubmission(
    val reviewOutcome: HumanFidelityReviewState,
    val reviewedPages: List<Int>,
    val descriptiveFidelity: String,
    val discrepancies: List<TierBFidelityDiscrepancySubmission> = emptyList(),
)

/**
 * HFR Owner UI acceptance defect fix: [exactText] is the owner-facing identification of the
 * discrepancy location -- never a Unicode code-point range, which [recordReview] computes
 * internally by locating [exactText] within the exact admitted page text (failing closed if it is
 * absent or not unique on that page). For every classification except [FidelityDiscrepancyClassification.MISSING_SOURCE_TEXT],
 * [exactText] is the exact wrong/hallucinated/uncertain text as it appears in the transcription.
 * For [FidelityDiscrepancyClassification.MISSING_SOURCE_TEXT] alone, [exactText] is the text
 * immediately *before* the missing content (or blank, meaning "missing at the very start of the
 * page") -- the existing frozen [FidelityDiscrepancyLocation.fromProviderBlock] factory's own
 * zero-width-insertion-point allowance, reused unchanged.
 *
 * See [TierBOcrHumanFidelityReviewCoordinator]'s class KDoc for why source resolution is not
 * offered here.
 */
data class TierBFidelityDiscrepancySubmission(
    val pageNumber: Int,
    val exactText: String,
    val classification: FidelityDiscrepancyClassification,
    val severity: FidelityDiscrepancySeverity,
    val reason: String,
    val explicitClassificationDetail: String? = null,
)

sealed interface TierBHumanFidelityReviewRecordingOutcome {
    data class Recorded(val reviewId: HumanFidelityReviewId) : TierBHumanFidelityReviewRecordingOutcome
    data class AlreadyRecorded(val reviewId: HumanFidelityReviewId) : TierBHumanFidelityReviewRecordingOutcome
    data class TargetResolutionFailed(val resolution: TierBHumanFidelityReviewTargetResolution) : TierBHumanFidelityReviewRecordingOutcome
    data class InvalidSubmission(val reason: String) : TierBHumanFidelityReviewRecordingOutcome
    data class AuthorizationDenied(val reason: HumanFidelityReviewRecordingDenialReason) : TierBHumanFidelityReviewRecordingOutcome
    data class Failure(val reason: GovernedHumanFidelityReviewRecordingFailureReason) : TierBHumanFidelityReviewRecordingOutcome
}

sealed interface TierBEffectiveHumanFidelityReviewOutcome {
    data class Projected(val summary: EffectiveHumanFidelityReviewSummary) : TierBEffectiveHumanFidelityReviewOutcome
    data class TargetResolutionFailed(val resolution: TierBHumanFidelityReviewTargetResolution) : TierBEffectiveHumanFidelityReviewOutcome
    data object FailedClosed : TierBEffectiveHumanFidelityReviewOutcome
}
