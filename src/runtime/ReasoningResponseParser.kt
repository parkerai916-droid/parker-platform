package parker.core.runtime

import parker.core.interfaces.ReasoningProviderResponse

/**
 * Model-Backed ReasoningProvider (Sprint 9). See
 * `docs/implementation/reviews/ModelReasoningProvider_Implementation_Review.md`
 * ("the Review") Section 4/12 and
 * `docs/implementation/MODEL_REASONING_PROVIDER_IMPLEMENTATION_PLAN.md`
 * ("the Plan") Section 6 Decisions B/C.
 *
 * **The frozen architectural component.** Any implementation satisfying
 * this one-method shape is a legitimate parsing strategy -- structured
 * JSON, grammar-constrained output, function-calling, or schema-based
 * responses -- none of which would require a change to
 * [ModelReasoningProvider], to this interface, or to any architecture
 * document (Review Section 4). [ModelReasoningProvider] never inspects
 * or branches on a raw model string itself; it only calls this
 * collaborator and returns its result unchanged.
 */
fun interface ReasoningResponseParser {
    fun parse(raw: String): ReasoningProviderResponse
}

/**
 * Thrown when [TaggedReasoningResponseParser] (or any other
 * [ReasoningResponseParser] implementation) cannot classify a raw model
 * response. Plain `RuntimeException` subtype, no new Parker contract
 * (Review Section 12, Decision 3; Plan Decision C). Carries the original,
 * untrimmed `raw` text for diagnostic purposes.
 */
class UnclassifiableModelResponseException(val raw: String) :
    RuntimeException("Unclassifiable model response: $raw")

/**
 * The proposed default, first production implementation of
 * [ReasoningResponseParser] -- **a starting-point default, not the
 * permanent parsing protocol** (Review Section 4, Section 12 Decision 3).
 * A future implementation may legitimately replace it with structured
 * JSON, grammar-constrained output, function-calling, or schema-based
 * responses, so long as it satisfies [ReasoningResponseParser]'s
 * one-method shape.
 *
 * Matching is case-sensitive, against the raw string trimmed exactly
 * once (Plan Decision B):
 * - `GOAL:<text>` -> [ReasoningProviderResponse.Goal] with `<text>`
 *   trimmed. A blank `<text>` surfaces as `IllegalArgumentException` from
 *   `Goal`'s own constructor validation, not caught here.
 * - `REPLY:<text>` -> [ReasoningProviderResponse.Reply], same handling.
 * - Exactly `NOACTION`, with nothing else present after the one outer
 *   trim -> [ReasoningProviderResponse.NoAction]. Trailing text after
 *   `NOACTION` does not match this branch.
 * - Anything else -> throws [UnclassifiableModelResponseException]
 *   carrying the original, untrimmed `raw` string.
 */
class TaggedReasoningResponseParser : ReasoningResponseParser {

    override fun parse(raw: String): ReasoningProviderResponse {
        val trimmed = raw.trim()

        return when {
            trimmed.startsWith(GOAL_TAG) ->
                ReasoningProviderResponse.Goal(trimmed.removePrefix(GOAL_TAG).trim())

            trimmed.startsWith(REPLY_TAG) ->
                ReasoningProviderResponse.Reply(trimmed.removePrefix(REPLY_TAG).trim())

            trimmed == NOACTION_TAG ->
                ReasoningProviderResponse.NoAction

            else -> throw UnclassifiableModelResponseException(raw)
        }
    }

    private companion object {
        const val GOAL_TAG = "GOAL:"
        const val REPLY_TAG = "REPLY:"
        const val NOACTION_TAG = "NOACTION"
    }
}
