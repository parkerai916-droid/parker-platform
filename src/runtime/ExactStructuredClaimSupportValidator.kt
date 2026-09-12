package parker.core.runtime

import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ExactGovernedIdentity
import parker.core.interfaces.ExactStructuredClaimField
import parker.core.interfaces.GroundedPropositionClassification
import parker.core.interfaces.GroundedReply
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry
import parker.core.interfaces.StructuredClaimSupport
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.MemoryCoreRecord

/** Deterministic outcomes for the deliberately narrow semantic-support check. */
enum class ExactStructuredClaimSupportStatus {
    EXACTLY_SUPPORTED,
    HUMAN_REVIEW_REQUIRED,
    INVALID,
    NOT_APPLICABLE,
}

enum class ExactStructuredClaimSupportFailure {
    STRUCTURAL_GROUNDING_INVALID,
    MISSING_EXPLICIT_STRUCTURED_SUPPORT,
    SUPPORT_NOT_BOUND_TO_PROPOSITION,
    UNSUPPORTED_SOURCE_ENTRY,
    SOURCE_VALUE_MISMATCH,
    STRUCTURED_VALUE_UNAVAILABLE,
    STRUCTURED_VALUE_MISMATCH,
    IDENTITY_UNAVAILABLE,
    IDENTITY_MISMATCH,
}

internal data class ExactStructuredClaimSupportCheck(
    val propositionIndex: Int,
    val status: ExactStructuredClaimSupportStatus,
    val supportClass: String? = null,
    val checkedValue: String? = null,
    val failure: ExactStructuredClaimSupportFailure? = null,
)

internal sealed interface ExactStructuredClaimSupportValidationOutcome {
    val checks: List<ExactStructuredClaimSupportCheck>

    data class ExactlySupported(override val checks: List<ExactStructuredClaimSupportCheck>) : ExactStructuredClaimSupportValidationOutcome
    data class HumanReviewRequired(
        val reason: ExactStructuredClaimSupportFailure,
        override val checks: List<ExactStructuredClaimSupportCheck>,
    ) : ExactStructuredClaimSupportValidationOutcome
    data class Invalid(
        val reason: ExactStructuredClaimSupportFailure,
        override val checks: List<ExactStructuredClaimSupportCheck>,
    ) : ExactStructuredClaimSupportValidationOutcome
    data class NotApplicable(override val checks: List<ExactStructuredClaimSupportCheck>) : ExactStructuredClaimSupportValidationOutcome
}

/**
 * R0 semantic-support boundary. It accepts only explicit canonical
 * structured assertions; it never parses, normalises, fuzzily matches, or
 * asks a model to judge proposition meaning.
 */
internal object ExactStructuredClaimSupportValidator {

    fun validate(
        reply: GroundedReply,
        context: ReasoningContext,
        evidenceResults: List<EvidenceRetrievalResult>,
        memoryCoreResults: List<Pair<RelationshipEndpoint, MemoryCoreRecord?>>,
        structuralValidation: GroundedReplyValidationOutcome? = null,
    ): ExactStructuredClaimSupportValidationOutcome {
        val structural = structuralValidation ?: GroundedReplyValidator.validate(reply, context, evidenceResults, memoryCoreResults)
        if (structural is GroundedReplyValidationOutcome.Invalid) {
            return ExactStructuredClaimSupportValidationOutcome.Invalid(
                ExactStructuredClaimSupportFailure.STRUCTURAL_GROUNDING_INVALID,
                emptyList(),
            )
        }

        val foundEvidence = evidenceResults.filterIsInstance<EvidenceRetrievalResult.Found>()
            .associateBy { it.evidenceArtifactId }
        val checks = mutableListOf<ExactStructuredClaimSupportCheck>()

        reply.propositions.forEachIndexed { index, proposition ->
            if (proposition.classification != GroundedPropositionClassification.SUPPORTED_FACT) {
                checks += ExactStructuredClaimSupportCheck(index, ExactStructuredClaimSupportStatus.NOT_APPLICABLE)
                return@forEachIndexed
            }
            val support = proposition.structuredSupports
            if (support.isEmpty()) {
                checks += ExactStructuredClaimSupportCheck(
                    index,
                    ExactStructuredClaimSupportStatus.HUMAN_REVIEW_REQUIRED,
                    failure = ExactStructuredClaimSupportFailure.MISSING_EXPLICIT_STRUCTURED_SUPPORT,
                )
                return@forEachIndexed
            }
            if (support.any { it.contextEntry !in proposition.supportReferences }) {
                checks += ExactStructuredClaimSupportCheck(
                    index,
                    ExactStructuredClaimSupportStatus.INVALID,
                    failure = ExactStructuredClaimSupportFailure.SUPPORT_NOT_BOUND_TO_PROPOSITION,
                )
                return@forEachIndexed
            }

            val result = support.map { checkSupport(it, foundEvidence) }
            val firstFailure = result.firstOrNull { it.status != ExactStructuredClaimSupportStatus.EXACTLY_SUPPORTED }
            if (firstFailure != null) checks += firstFailure.copy(propositionIndex = index)
            else checks += ExactStructuredClaimSupportCheck(
                index,
                ExactStructuredClaimSupportStatus.EXACTLY_SUPPORTED,
                supportClass = result.first().supportClass,
                checkedValue = result.first().checkedValue,
            )
        }

        val invalid = checks.firstOrNull { it.status == ExactStructuredClaimSupportStatus.INVALID }
        if (invalid != null) return ExactStructuredClaimSupportValidationOutcome.Invalid(requireNotNull(invalid.failure), checks)
        val review = checks.firstOrNull { it.status == ExactStructuredClaimSupportStatus.HUMAN_REVIEW_REQUIRED }
        if (review != null) return ExactStructuredClaimSupportValidationOutcome.HumanReviewRequired(requireNotNull(review.failure), checks)
        return if (checks.any { it.status == ExactStructuredClaimSupportStatus.EXACTLY_SUPPORTED }) {
            ExactStructuredClaimSupportValidationOutcome.ExactlySupported(checks)
        } else {
            ExactStructuredClaimSupportValidationOutcome.NotApplicable(checks)
        }
    }

    private fun checkSupport(
        support: StructuredClaimSupport,
        foundEvidence: Map<parker.core.interfaces.EvidenceArtifactId, EvidenceRetrievalResult.Found>,
    ): ExactStructuredClaimSupportCheck {
        val evidence = support.contextEntry as? ReasoningContextEntry.GovernedEvidence
            ?: return check(ExactStructuredClaimSupportStatus.HUMAN_REVIEW_REQUIRED, support, failure = ExactStructuredClaimSupportFailure.UNSUPPORTED_SOURCE_ENTRY)
        val found = foundEvidence[evidence.evidenceArtifactId]
            ?: return check(ExactStructuredClaimSupportStatus.INVALID, support, failure = ExactStructuredClaimSupportFailure.STRUCTURAL_GROUNDING_INVALID)

        return when (support) {
            is StructuredClaimSupport.ExactSourceValue -> {
                val exactBytes = support.value.toByteArray(Charsets.UTF_8)
                check(
                    if (exactBytes.contentEquals(found.content)) ExactStructuredClaimSupportStatus.EXACTLY_SUPPORTED
                    else ExactStructuredClaimSupportStatus.INVALID,
                    support,
                    checkedValue = support.value,
                    failure = ExactStructuredClaimSupportFailure.SOURCE_VALUE_MISMATCH.takeUnless { exactBytes.contentEquals(found.content) },
                )
            }
            is StructuredClaimSupport.ExactStructuredValue -> {
                val actual = when (support.field) {
                    ExactStructuredClaimField.PAGE_NUMBER -> evidence.pageNumber?.toString()
                    ExactStructuredClaimField.SOURCE_SHA256 -> evidence.sourceSha256 ?: evidence.assurance?.sourceSha256
                }
                if (actual == null) check(ExactStructuredClaimSupportStatus.HUMAN_REVIEW_REQUIRED, support, failure = ExactStructuredClaimSupportFailure.STRUCTURED_VALUE_UNAVAILABLE)
                else check(
                    if (actual == support.value) ExactStructuredClaimSupportStatus.EXACTLY_SUPPORTED else ExactStructuredClaimSupportStatus.INVALID,
                    support,
                    checkedValue = actual,
                    failure = ExactStructuredClaimSupportFailure.STRUCTURED_VALUE_MISMATCH.takeUnless { actual == support.value },
                )
            }
            is StructuredClaimSupport.ExactIdentity -> {
                val actual = when (support.identity) {
                    ExactGovernedIdentity.EVIDENCE_ARTIFACT_ID -> evidence.evidenceArtifactId.value
                    ExactGovernedIdentity.DERIVATIVE_GENERATION_ID -> evidence.derivativeGenerationId?.value
                    ExactGovernedIdentity.SOURCE_REGION_ID -> evidence.sourceRegionId?.value
                }
                if (actual == null) check(ExactStructuredClaimSupportStatus.HUMAN_REVIEW_REQUIRED, support, failure = ExactStructuredClaimSupportFailure.IDENTITY_UNAVAILABLE)
                else check(
                    if (actual == support.value) ExactStructuredClaimSupportStatus.EXACTLY_SUPPORTED else ExactStructuredClaimSupportStatus.INVALID,
                    support,
                    checkedValue = actual,
                    failure = ExactStructuredClaimSupportFailure.IDENTITY_MISMATCH.takeUnless { actual == support.value },
                )
            }
        }
    }

    private fun check(
        status: ExactStructuredClaimSupportStatus,
        support: StructuredClaimSupport,
        checkedValue: String? = null,
        failure: ExactStructuredClaimSupportFailure? = null,
    ) = ExactStructuredClaimSupportCheck(
        propositionIndex = -1,
        status = status,
        supportClass = when (support) {
            is StructuredClaimSupport.ExactSourceValue -> "EXACT_SOURCE_VALUE"
            is StructuredClaimSupport.ExactStructuredValue -> "EXACT_STRUCTURED_VALUE"
            is StructuredClaimSupport.ExactIdentity -> "EXACT_IDENTITY"
        },
        checkedValue = checkedValue,
        failure = failure,
    )
}
