package parker.core.interfaces

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Collections

private const val MAX_FIDELITY_ID_CHARACTERS = 80
private const val MAX_FIDELITY_TEXT_CHARACTERS = 4_096
private const val MAX_FIDELITY_ITEMS = 1_000
private val FIDELITY_ID = Regex("^[a-z][a-z0-9-]{0,14}-[0-9a-f]{64}$")

/** Immutable, content-derived identity of one completed human fidelity review act. */
@JvmInline
value class HumanFidelityReviewId(val value: String) {
    init {
        require(value.length <= MAX_FIDELITY_ID_CHARACTERS && FIDELITY_ID.matches(value) && value.startsWith("review-")) {
            "HumanFidelityReviewId must be review- followed by a 64-character lowercase SHA-256"
        }
    }
}

/** Immutable, content-derived identity of one exact location-bound discrepancy occurrence. */
@JvmInline
value class FidelityDiscrepancyId(val value: String) {
    init {
        require(value.length <= MAX_FIDELITY_ID_CHARACTERS && FIDELITY_ID.matches(value) && value.startsWith("discrepancy-")) {
            "FidelityDiscrepancyId must be discrepancy- followed by a 64-character lowercase SHA-256"
        }
    }
}

/** Immutable identity of a descriptive association among explicit discrepancy occurrences. */
@JvmInline
value class SystematicDiscrepancyPatternId(val value: String) {
    init {
        require(value.length <= MAX_FIDELITY_ID_CHARACTERS && FIDELITY_ID.matches(value) && value.startsWith("pattern-")) {
            "SystematicDiscrepancyPatternId must be pattern- followed by a 64-character lowercase SHA-256"
        }
    }
}

enum class HumanFidelityReviewState {
    UNREVIEWED,
    HUMAN_REVIEWED_PASS,
    HUMAN_REVIEWED_WITH_DISCREPANCY,
    HUMAN_REVIEW_CONFLICT,
}

enum class HumanFidelityCoverageKind { FULL_GENERATION, PARTIAL }

data class HumanFidelityCharacterScope(
    val pageNumber: Int,
    val transcriptionBlockIndex: Int,
    val startCodePointInclusive: Int,
    val endCodePointExclusive: Int,
) {
    init {
        require(pageNumber >= 1) { "HumanFidelityCharacterScope.pageNumber must be one-based and positive" }
        require(transcriptionBlockIndex >= 0) { "HumanFidelityCharacterScope.transcriptionBlockIndex must not be negative" }
        require(startCodePointInclusive >= 0 && endCodePointExclusive > startCodePointInclusive) {
            "HumanFidelityCharacterScope must be a non-empty half-open Unicode code-point range"
        }
    }
}

/** Coverage never implies fidelity. FULL_GENERATION and PARTIAL are explicit, independent facts. */
class HumanFidelityReviewCoverage(
    val kind: HumanFidelityCoverageKind,
    reviewedPages: Collection<Int>,
    reviewedCharacterScopes: Collection<HumanFidelityCharacterScope> = emptyList(),
) {
    val reviewedPages: List<Int>
    val reviewedCharacterScopes: List<HumanFidelityCharacterScope>

    init {
        require(reviewedPages.isNotEmpty()) { "Human fidelity review coverage must contain at least one page" }
        require(reviewedPages.all { it >= 1 }) { "Reviewed pages must be one-based and positive" }
        require(reviewedPages.size == reviewedPages.toSet().size) { "Reviewed pages must not contain duplicates" }
        require(reviewedPages.size <= MAX_FIDELITY_ITEMS) { "Review coverage contains too many pages" }
        require(reviewedCharacterScopes.size <= MAX_FIDELITY_ITEMS) { "Review coverage contains too many character scopes" }
        require(reviewedCharacterScopes.all { it.pageNumber in reviewedPages }) {
            "Every reviewed character scope must belong to a reviewed page"
        }
        val orderedScopes = reviewedCharacterScopes.sortedWith(
            compareBy(HumanFidelityCharacterScope::pageNumber)
                .thenBy(HumanFidelityCharacterScope::transcriptionBlockIndex)
                .thenBy(HumanFidelityCharacterScope::startCodePointInclusive),
        )
        orderedScopes.zipWithNext().forEach { (left, right) ->
            if (left.pageNumber == right.pageNumber && left.transcriptionBlockIndex == right.transcriptionBlockIndex) {
                require(left.endCodePointExclusive <= right.startCodePointInclusive) {
                    "Reviewed character scopes must not overlap within a transcription block"
                }
            }
        }
        require((kind == HumanFidelityCoverageKind.FULL_GENERATION) == reviewedCharacterScopes.isEmpty()) {
            "FULL_GENERATION coverage uses page scope only; PARTIAL coverage requires exact character scopes"
        }
        this.reviewedPages = Collections.unmodifiableList(reviewedPages.sorted())
        this.reviewedCharacterScopes = Collections.unmodifiableList(orderedScopes)
    }

    override fun equals(other: Any?): Boolean = other is HumanFidelityReviewCoverage &&
        kind == other.kind && reviewedPages == other.reviewedPages && reviewedCharacterScopes == other.reviewedCharacterScopes

    override fun hashCode(): Int = 31 * (31 * kind.hashCode() + reviewedPages.hashCode()) + reviewedCharacterScopes.hashCode()
}

enum class FidelityDiscrepancyClassification {
    TRANSCRIPTION_DIFFERENCE,
    MISSING_SOURCE_TEXT,
    ADDED_OR_HALLUCINATED_TEXT,
    INAPPROPRIATE_CERTAINTY,
    APPROPRIATE_UNCERTAINTY,
    OTHER_EXPLICITLY_CLASSIFIED,
}

enum class FidelityDiscrepancySeverity { MINOR, MATERIAL, NON_ERROR_OBSERVATION }

enum class FidelityCauseState { ESTABLISHED, HYPOTHESISED, UNKNOWN }

/** Cause is actor-neutral and never inferred from characters, severity, repetition, or producer identity. */
data class FidelityCauseAssessment(
    val state: FidelityCauseState,
    val mechanism: String? = null,
    val supportingBasis: String? = null,
) {
    init {
        requireBoundedOptional(mechanism, "FidelityCauseAssessment.mechanism")
        requireBoundedOptional(supportingBasis, "FidelityCauseAssessment.supportingBasis")
        when (state) {
            FidelityCauseState.UNKNOWN -> require(mechanism == null && supportingBasis == null) {
                "UNKNOWN cause must not carry a fabricated mechanism or basis"
            }
            FidelityCauseState.HYPOTHESISED -> require(mechanism != null) {
                "HYPOTHESISED cause requires an explicit hypothesis"
            }
            FidelityCauseState.ESTABLISHED -> require(mechanism != null && supportingBasis != null) {
                "ESTABLISHED cause requires an explicit mechanism and supporting basis"
            }
        }
    }
}

sealed interface HumanSourceResolution {
    data object Unresolved : HumanSourceResolution

    data class ResolvedAgainstSource(
        val assertedSourceValue: String,
        val assertedSourceValueSha256: OcrSha256Digest,
        val sourcePageRepresentationId: PageRepresentationId,
        val resolvingReviewerPrincipalId: PrincipalId,
    ) : HumanSourceResolution {
        init {
            require(assertedSourceValue.length <= MAX_FIDELITY_TEXT_CHARACTERS) {
                "Resolved source value exceeds the bounded review size"
            }
            require(assertedSourceValueSha256 == fidelityDigest(assertedSourceValue)) {
                "Resolved source value SHA-256 must match the exact asserted source value"
            }
        }
    }
}

/**
 * Exact target in the unnormalised provider block. Creation validates Unicode code-point offsets
 * against that block but retains only the exact substring and governed identities.
 */
class FidelityDiscrepancyLocation private constructor(
    val evidenceArtifactId: EvidenceArtifactId,
    val sourceSha256: OcrSha256Digest,
    val pageNumber: Int,
    val preparationIdentity: OcrSha256Digest,
    val preparationRegionId: SourceRegionId,
    val derivativeGenerationId: DerivativeGenerationId,
    val derivativeGenerationSha256: OcrSha256Digest,
    val derivativeContentSha256: OcrSha256Digest,
    val derivativeRegionId: SourceRegionId,
    val transcriptionBlockIndex: Int,
    val startCodePointInclusive: Int,
    val endCodePointExclusive: Int,
    val originalProviderSubstring: String,
    val originalProviderSubstringSha256: OcrSha256Digest,
) {
    val codePointLength: Int get() = endCodePointExclusive - startCodePointInclusive

    init {
        require(pageNumber >= 1) { "Fidelity discrepancy page number must be one-based and positive" }
        require(transcriptionBlockIndex >= 0) { "Fidelity discrepancy block index must not be negative" }
        require(startCodePointInclusive >= 0 && endCodePointExclusive >= startCodePointInclusive) {
            "Fidelity discrepancy span must be a valid half-open Unicode code-point range"
        }
        require(originalProviderSubstring.length <= MAX_FIDELITY_TEXT_CHARACTERS) { "Original provider substring is too large" }
        require(originalProviderSubstring.codePointCount(0, originalProviderSubstring.length) == codePointLength) {
            "Original provider substring code-point length must equal its exact span"
        }
        require(originalProviderSubstringSha256 == fidelityDigest(originalProviderSubstring)) {
            "Original provider substring SHA-256 must match the exact substring"
        }
    }

    companion object {
        fun fromProviderBlock(
            evidenceArtifactId: EvidenceArtifactId,
            sourceSha256: OcrSha256Digest,
            pageNumber: Int,
            preparationIdentity: OcrSha256Digest,
            preparationRegionId: SourceRegionId,
            derivativeGenerationId: DerivativeGenerationId,
            derivativeGenerationSha256: OcrSha256Digest,
            derivativeContentSha256: OcrSha256Digest,
            derivativeRegionId: SourceRegionId,
            transcriptionBlockIndex: Int,
            providerBlock: String,
            startCodePointInclusive: Int,
            endCodePointExclusive: Int,
            expectedOriginalProviderSubstring: String,
            expectedOriginalProviderSubstringSha256: OcrSha256Digest = fidelityDigest(expectedOriginalProviderSubstring),
        ): FidelityDiscrepancyLocation {
            require(providerBlock.length <= MAX_FIDELITY_TEXT_CHARACTERS * 100) { "Provider block exceeds validation bound" }
            val blockCodePoints = providerBlock.codePointCount(0, providerBlock.length)
            require(startCodePointInclusive >= 0 && endCodePointExclusive >= startCodePointInclusive && endCodePointExclusive <= blockCodePoints) {
                "Fidelity discrepancy span falls outside the provider block's Unicode code points"
            }
            val startChar = providerBlock.offsetByCodePoints(0, startCodePointInclusive)
            val endChar = providerBlock.offsetByCodePoints(0, endCodePointExclusive)
            require(providerBlock.substring(startChar, endChar) == expectedOriginalProviderSubstring) {
                "Exact provider substring does not match the Unicode code-point span"
            }
            return FidelityDiscrepancyLocation(
                evidenceArtifactId, sourceSha256, pageNumber, preparationIdentity, preparationRegionId,
                derivativeGenerationId, derivativeGenerationSha256, derivativeContentSha256, derivativeRegionId,
                transcriptionBlockIndex, startCodePointInclusive, endCodePointExclusive,
                expectedOriginalProviderSubstring, expectedOriginalProviderSubstringSha256,
            )
        }
    }
}

data class FidelityDiscrepancyOccurrence(
    val discrepancyId: FidelityDiscrepancyId,
    val reviewId: HumanFidelityReviewId,
    val location: FidelityDiscrepancyLocation,
    val classification: FidelityDiscrepancyClassification,
    val severity: FidelityDiscrepancySeverity,
    val reason: String,
    val explicitClassificationDetail: String? = null,
    val sourceResolution: HumanSourceResolution,
    val causeAssessment: FidelityCauseAssessment,
    val systematicPatternId: SystematicDiscrepancyPatternId? = null,
) {
    init {
        requireBoundedRequired(reason, "Fidelity discrepancy reason")
        requireBoundedOptional(explicitClassificationDetail, "Explicit discrepancy classification detail")
        require((classification == FidelityDiscrepancyClassification.OTHER_EXPLICITLY_CLASSIFIED) == (explicitClassificationDetail != null)) {
            "OTHER_EXPLICITLY_CLASSIFIED alone requires explicit bounded detail"
        }
        require((classification == FidelityDiscrepancyClassification.APPROPRIATE_UNCERTAINTY) ==
            (severity == FidelityDiscrepancySeverity.NON_ERROR_OBSERVATION)) {
            "APPROPRIATE_UNCERTAINTY and NON_ERROR_OBSERVATION must occur together"
        }
        require(classification == FidelityDiscrepancyClassification.MISSING_SOURCE_TEXT || location.codePointLength > 0) {
            "Only missing-source-text discrepancies may target an empty insertion point"
        }
        require(discrepancyId == deriveId(
            reviewId, location, classification, severity, reason, explicitClassificationDetail,
            sourceResolution, causeAssessment,
        )) { "FidelityDiscrepancyId must be the deterministic identity of the exact occurrence" }
    }

    companion object {
        fun deriveId(
            reviewId: HumanFidelityReviewId,
            location: FidelityDiscrepancyLocation,
            classification: FidelityDiscrepancyClassification,
            severity: FidelityDiscrepancySeverity,
            reason: String,
            explicitClassificationDetail: String?,
            sourceResolution: HumanSourceResolution,
            causeAssessment: FidelityCauseAssessment,
        ): FidelityDiscrepancyId = FidelityDiscrepancyId("discrepancy-" + canonicalSha256(
            reviewId.value, location.canonicalIdentity(), classification.name, severity.name, reason,
            explicitClassificationDetail ?: "", sourceResolution.canonicalIdentity(), causeAssessment.canonicalIdentity(),
        ))
    }
}

class SystematicDiscrepancyPattern(
    val patternId: SystematicDiscrepancyPatternId,
    val reviewId: HumanFidelityReviewId,
    memberDiscrepancyIds: Collection<FidelityDiscrepancyId>,
    val observedPatternDescription: String,
    val occurrenceCount: Int,
    val reviewerPrincipalId: PrincipalId,
) {
    val memberDiscrepancyIds: List<FidelityDiscrepancyId>

    init {
        requireBoundedRequired(observedPatternDescription, "Observed systematic pattern description")
        require(memberDiscrepancyIds.size in 2..MAX_FIDELITY_ITEMS) { "A repeated systematic pattern requires at least two members" }
        require(memberDiscrepancyIds.size == memberDiscrepancyIds.toSet().size) { "Systematic pattern members must be unique" }
        require(occurrenceCount == memberDiscrepancyIds.size) { "Systematic pattern occurrence count must equal explicit member count" }
        this.memberDiscrepancyIds = Collections.unmodifiableList(memberDiscrepancyIds.sortedBy { it.value })
        require(patternId == deriveId(reviewId, this.memberDiscrepancyIds, observedPatternDescription, reviewerPrincipalId)) {
            "SystematicDiscrepancyPatternId must be the deterministic identity of its explicit association"
        }
    }

    companion object {
        fun deriveId(
            reviewId: HumanFidelityReviewId,
            memberDiscrepancyIds: Collection<FidelityDiscrepancyId>,
            observedPatternDescription: String,
            reviewerPrincipalId: PrincipalId,
        ) = SystematicDiscrepancyPatternId("pattern-" + canonicalSha256(
            reviewId.value,
            memberDiscrepancyIds.map { it.value }.sorted().joinToString("\u0000"),
            observedPatternDescription,
            reviewerPrincipalId.value,
        ))
    }
}

data class HumanFidelityReviewArtifacts(
    val worksheetSha256: OcrSha256Digest,
    val ownerReviewRecordSha256: OcrSha256Digest,
)

data class HumanFidelityReviewTarget(
    val evidenceArtifactId: EvidenceArtifactId,
    val sourceSha256: OcrSha256Digest,
    val preparationIdentity: OcrSha256Digest,
    val derivativeGenerationId: DerivativeGenerationId,
    val derivativeGenerationSha256: OcrSha256Digest,
    val derivativeContentSha256: OcrSha256Digest,
)

data class HumanFidelityReviewSupersession(
    val predecessorReviewId: HumanFidelityReviewId,
    val target: HumanFidelityReviewTarget,
)

class HumanFidelityReviewAdjudicationReference(
    conflictingReviewIds: Collection<HumanFidelityReviewId>,
    val selectedReviewId: HumanFidelityReviewId,
    val target: HumanFidelityReviewTarget,
) {
    val conflictingReviewIds: Set<HumanFidelityReviewId>

    init {
        require(conflictingReviewIds.size >= 2) { "Review adjudication requires at least two conflicting review identities" }
        require(conflictingReviewIds.size == conflictingReviewIds.toSet().size) { "Conflicting review identities must be unique" }
        this.conflictingReviewIds = Collections.unmodifiableSet(conflictingReviewIds.toSet())
        require(selectedReviewId in this.conflictingReviewIds) { "Selected review must be one of the explicitly conflicting reviews" }
    }
}

/** One immutable completed review act. It contains observations, never correction authority or corrected content. */
class HumanFidelityReviewRecord(
    val reviewId: HumanFidelityReviewId,
    val target: HumanFidelityReviewTarget,
    val reviewerPrincipalId: PrincipalId,
    val reviewedAt: Instant,
    val artifacts: HumanFidelityReviewArtifacts,
    val coverage: HumanFidelityReviewCoverage,
    val reviewState: HumanFidelityReviewState,
    val descriptiveFidelity: String,
    discrepancyOccurrences: Collection<FidelityDiscrepancyOccurrence>,
    systematicPatterns: Collection<SystematicDiscrepancyPattern> = emptyList(),
    val supersession: HumanFidelityReviewSupersession? = null,
    val adjudication: HumanFidelityReviewAdjudicationReference? = null,
) {
    val discrepancyOccurrences: List<FidelityDiscrepancyOccurrence>
    val systematicPatterns: List<SystematicDiscrepancyPattern>

    init {
        requireBoundedRequired(descriptiveFidelity, "Descriptive fidelity")
        require(reviewState != HumanFidelityReviewState.UNREVIEWED && reviewState != HumanFidelityReviewState.HUMAN_REVIEW_CONFLICT) {
            "A completed review record may contain only PASS or WITH_DISCREPANCY; absence/conflict are projected states"
        }
        require(coverage.kind == HumanFidelityCoverageKind.FULL_GENERATION || reviewState != HumanFidelityReviewState.HUMAN_REVIEWED_PASS) {
            "Partial coverage cannot claim whole-generation HUMAN_REVIEWED_PASS"
        }
        require(discrepancyOccurrences.size <= MAX_FIDELITY_ITEMS && systematicPatterns.size <= MAX_FIDELITY_ITEMS)
        require(discrepancyOccurrences.map { it.discrepancyId }.distinct().size == discrepancyOccurrences.size) {
            "Review discrepancy identities must be unique"
        }
        require((reviewState == HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY) == discrepancyOccurrences.isNotEmpty()) {
            "PASS contains no discrepancy facts; WITH_DISCREPANCY requires at least one"
        }
        require(discrepancyOccurrences.all { occurrence ->
            occurrence.reviewId == reviewId && occurrence.location.matches(target) &&
                occurrence.location.pageNumber in coverage.reviewedPages &&
                (occurrence.sourceResolution !is HumanSourceResolution.ResolvedAgainstSource ||
                    occurrence.sourceResolution.resolvingReviewerPrincipalId == reviewerPrincipalId)
        }) { "Every discrepancy must bind the exact review target and reviewed page scope" }
        val occurrencesById = discrepancyOccurrences.associateBy { it.discrepancyId }
        require(systematicPatterns.map { it.patternId }.distinct().size == systematicPatterns.size)
        require(systematicPatterns.all { pattern ->
            pattern.reviewId == reviewId && pattern.reviewerPrincipalId == reviewerPrincipalId &&
                pattern.memberDiscrepancyIds.all(occurrencesById::containsKey)
        }) { "Every systematic pattern must bind this review, reviewer, and explicit occurrence members" }
        require(discrepancyOccurrences.all { occurrence ->
            val patternId = occurrence.systematicPatternId
            patternId == null || systematicPatterns.any { it.patternId == patternId && occurrence.discrepancyId in it.memberDiscrepancyIds }
        }) { "Every occurrence pattern association must be explicit and reciprocal" }
        require(systematicPatterns.all { pattern ->
            pattern.memberDiscrepancyIds.all { occurrencesById.getValue(it).systematicPatternId == pattern.patternId }
        }) { "Pattern membership must not imply unbound occurrences" }
        supersession?.let {
            require(it.predecessorReviewId != reviewId) { "A review cannot supersede itself" }
            require(it.target == target) { "Review supersession must bind the exact target" }
        }
        adjudication?.let {
            require(it.target == target) { "Review adjudication must bind the exact target" }
            require(reviewId !in it.conflictingReviewIds) { "An adjudicating review cannot list itself as an antecedent conflict" }
        }
        require(reviewId == deriveId(target, reviewerPrincipalId, reviewedAt, artifacts, coverage, reviewState, descriptiveFidelity)) {
            "HumanFidelityReviewId must be the deterministic identity of the completed review act"
        }
        this.discrepancyOccurrences = Collections.unmodifiableList(discrepancyOccurrences.sortedBy { it.discrepancyId.value })
        this.systematicPatterns = Collections.unmodifiableList(systematicPatterns.sortedBy { it.patternId.value })
    }

    companion object {
        fun deriveId(
            target: HumanFidelityReviewTarget,
            reviewerPrincipalId: PrincipalId,
            reviewedAt: Instant,
            artifacts: HumanFidelityReviewArtifacts,
            coverage: HumanFidelityReviewCoverage,
            reviewState: HumanFidelityReviewState,
            descriptiveFidelity: String,
        ): HumanFidelityReviewId = HumanFidelityReviewId("review-" + canonicalSha256(
            target.canonicalIdentity(), reviewerPrincipalId.value, reviewedAt.toString(),
            artifacts.worksheetSha256.value, artifacts.ownerReviewRecordSha256.value,
            coverage.canonicalIdentity(), reviewState.name, descriptiveFidelity,
        ))
    }
}

enum class SourceConfirmedEligibilityState { ALLOWED, DENIED }

enum class SourceConfirmedDenialReason {
    UNREVIEWED,
    MATERIAL_DISCREPANCY,
    UNRESOLVED_CONFLICT,
    MALFORMED_OR_UNSUPPORTED_STATE,
    PARTIAL_COVERAGE,
}

data class SourceConfirmedEligibility(
    val state: SourceConfirmedEligibilityState,
    val denialReason: SourceConfirmedDenialReason? = null,
) {
    init {
        require((state == SourceConfirmedEligibilityState.DENIED) == (denialReason != null)) {
            "Denied source-confirmed eligibility requires an explicit reason; allowed eligibility must not carry one"
        }
    }
}

/** Value-only output contract for the later A4 projector; this type performs no projection. */
class EffectiveHumanFidelityReviewProjection(
    val target: HumanFidelityReviewTarget,
    val effectiveState: HumanFidelityReviewState,
    val coverage: HumanFidelityReviewCoverage?,
    applicableReviewIds: Collection<HumanFidelityReviewId>,
    discrepancyIds: Collection<FidelityDiscrepancyId>,
    val eligibility: SourceConfirmedEligibility,
) {
    val applicableReviewIds: Set<HumanFidelityReviewId>
    val discrepancyIds: Set<FidelityDiscrepancyId>

    init {
        require(applicableReviewIds.size <= MAX_FIDELITY_ITEMS && discrepancyIds.size <= MAX_FIDELITY_ITEMS)
        require(applicableReviewIds.size == applicableReviewIds.toSet().size && discrepancyIds.size == discrepancyIds.toSet().size)
        this.applicableReviewIds = Collections.unmodifiableSet(applicableReviewIds.toSet())
        this.discrepancyIds = Collections.unmodifiableSet(discrepancyIds.toSet())
        when (effectiveState) {
            HumanFidelityReviewState.UNREVIEWED -> {
                require(coverage == null && this.applicableReviewIds.isEmpty() && this.discrepancyIds.isEmpty())
                require(eligibility == SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED, SourceConfirmedDenialReason.UNREVIEWED))
            }
            HumanFidelityReviewState.HUMAN_REVIEWED_PASS -> {
                require(coverage?.kind == HumanFidelityCoverageKind.FULL_GENERATION && this.applicableReviewIds.isNotEmpty() && this.discrepancyIds.isEmpty())
            }
            HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY -> {
                require(coverage != null && this.applicableReviewIds.isNotEmpty() && this.discrepancyIds.isNotEmpty())
            }
            HumanFidelityReviewState.HUMAN_REVIEW_CONFLICT -> {
                require(this.applicableReviewIds.size >= 2)
                require(eligibility == SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED, SourceConfirmedDenialReason.UNRESOLVED_CONFLICT))
            }
        }
    }
}

fun fidelityDigest(value: String): OcrSha256Digest = OcrSha256Digest(canonicalSha256(value))

private fun requireBoundedRequired(value: String, field: String) {
    require(value.isNotBlank() && value.length <= MAX_FIDELITY_TEXT_CHARACTERS) {
        "$field must contain 1..$MAX_FIDELITY_TEXT_CHARACTERS characters"
    }
}

private fun requireBoundedOptional(value: String?, field: String) {
    require(value == null || (value.isNotBlank() && value.length <= MAX_FIDELITY_TEXT_CHARACTERS)) {
        "$field must be absent or contain 1..$MAX_FIDELITY_TEXT_CHARACTERS characters"
    }
}

private fun canonicalSha256(vararg values: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    values.forEach { value ->
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        digest.update(byteArrayOf(
            (bytes.size ushr 24).toByte(), (bytes.size ushr 16).toByte(),
            (bytes.size ushr 8).toByte(), bytes.size.toByte(),
        ))
        digest.update(bytes)
    }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

private fun HumanFidelityReviewTarget.canonicalIdentity() = listOf(
    evidenceArtifactId.value, sourceSha256.value, preparationIdentity.value, derivativeGenerationId.value,
    derivativeGenerationSha256.value, derivativeContentSha256.value,
).joinToString("\u0000")

private fun HumanFidelityReviewCoverage.canonicalIdentity() = listOf(
    kind.name,
    reviewedPages.joinToString(","),
    reviewedCharacterScopes.joinToString(";") {
        "${it.pageNumber},${it.transcriptionBlockIndex},${it.startCodePointInclusive},${it.endCodePointExclusive}"
    },
).joinToString("\u0000")

private fun FidelityDiscrepancyLocation.canonicalIdentity() = listOf(
    evidenceArtifactId.value, sourceSha256.value, pageNumber.toString(), preparationIdentity.value,
    preparationRegionId.value, derivativeGenerationId.value, derivativeGenerationSha256.value,
    derivativeContentSha256.value, derivativeRegionId.value, transcriptionBlockIndex.toString(),
    startCodePointInclusive.toString(), endCodePointExclusive.toString(), originalProviderSubstring,
    originalProviderSubstringSha256.value,
).joinToString("\u0000")

private fun FidelityDiscrepancyLocation.matches(target: HumanFidelityReviewTarget) =
    evidenceArtifactId == target.evidenceArtifactId && sourceSha256 == target.sourceSha256 &&
        preparationIdentity == target.preparationIdentity && derivativeGenerationId == target.derivativeGenerationId &&
        derivativeGenerationSha256 == target.derivativeGenerationSha256 && derivativeContentSha256 == target.derivativeContentSha256

private fun HumanSourceResolution.canonicalIdentity() = when (this) {
    HumanSourceResolution.Unresolved -> "UNRESOLVED"
    is HumanSourceResolution.ResolvedAgainstSource -> listOf(
        "RESOLVED", assertedSourceValue, assertedSourceValueSha256.value,
        sourcePageRepresentationId.value, resolvingReviewerPrincipalId.value,
    ).joinToString("\u0000")
}

private fun FidelityCauseAssessment.canonicalIdentity() =
    listOf(state.name, mechanism ?: "", supportingBasis ?: "").joinToString("\u0000")
