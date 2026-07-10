package parker.core.runtime

import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.Turn

/**
 * Model-Backed ReasoningProvider (Sprint 9), implementing exactly what
 * `docs/implementation/reviews/ModelReasoningProvider_Implementation_Review.md`
 * ("the Review") Section 4/12 and
 * `docs/implementation/MODEL_REASONING_PROVIDER_IMPLEMENTATION_PLAN.md`
 * ("the Plan") Section 6 Decision A authorise.
 *
 * Pure, synchronous seam -- no I/O, no `suspend`. [ModelReasoningProvider]
 * never constructs a prompt itself; it only calls this collaborator
 * (Review Section 4).
 */
fun interface ReasoningPromptBuilder {
    fun buildPrompt(turn: Turn, reasoningContext: ReasoningContext): String
}

/**
 * The Plan's Decision A: a simple, deterministic template -- the already-
 * assembled context entries (one per line, in order), then the owner's
 * own message, then a fixed instruction requiring the model to prefix its
 * reply with exactly one of `GOAL:`, `REPLY:`, or `NOACTION`. No further
 * prompt engineering, few-shot examples, or persona instructions (Review
 * Section 12, Decision 2).
 */
class DefaultReasoningPromptBuilder : ReasoningPromptBuilder {

    override fun buildPrompt(turn: Turn, reasoningContext: ReasoningContext): String {
        val contextBlock = if (reasoningContext.entries.isEmpty()) {
            ""
        } else {
            reasoningContext.entries.joinToString("\n") + "\n"
        }

        return contextBlock +
            turn.message.text +
            "\n\n" +
            "Respond with exactly one of the following prefixes: GOAL:, REPLY:, or " +
            "NOACTION, followed by your response text. Use NOACTION alone, with no " +
            "text after it."
    }
}
