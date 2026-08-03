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
 *
 * [SELECTION_GUIDANCE] additionally states *when* each prefix applies --
 * added after live testing showed the model defaulting ordinary
 * conversational input (a plain greeting) to `NOACTION`, which
 * `TaggedReasoningResponseParser` then classified correctly as `NoAction`:
 * the parser was never at fault, the prompt simply never told the model
 * how to choose between the three prefixes, only that it had to choose
 * one. This addition changes only that selection guidance -- the output
 * protocol itself ([INSTRUCTION]'s own `GOAL:`/`REPLY:`/`NOACTION`
 * prefixes and "NOACTION alone, no trailing text" rule) is unchanged, and
 * still contains no persona instruction.
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
            INSTRUCTION
    }

    private companion object {
        const val SELECTION_GUIDANCE =
            "Use REPLY: for greetings; questions; conversational statements that reasonably " +
                "invite a response; requests for information, explanation, clarification, or " +
                "discussion; and acknowledgements where a useful direct response is " +
                "appropriate.\n\n" +
                "Use GOAL: only when the owner is asking you to carry out work that requires " +
                "planning, execution, tools, later action, or multiple coordinated steps.\n\n" +
                "Use NOACTION only when no response and no action is appropriate. Do not use " +
                "NOACTION merely because the message is short, casual, or lacks an explicit " +
                "question."

        const val INSTRUCTION =
            "Respond with exactly one of the following prefixes: GOAL:, REPLY:, or " +
                "NOACTION, followed by your response text.\n\n" +
                SELECTION_GUIDANCE +
                "\n\n" +
                "Output only one tagged result and no other text. Use NOACTION alone, with no " +
                "text after it."
    }
}
