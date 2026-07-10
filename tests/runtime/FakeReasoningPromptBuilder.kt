package parker.core.runtime

import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.Turn

/**
 * Test-only fake, mirroring [FakeReasoningProvider]'s lambda-based fake
 * precedent (Plan Decision G). Exists so [ModelReasoningProviderTest] can
 * exercise [ModelReasoningProvider]'s own orchestration independently of
 * [DefaultReasoningPromptBuilder]'s real template.
 */
class FakeReasoningPromptBuilder(
    private val promptFor: (Turn, ReasoningContext) -> String,
) : ReasoningPromptBuilder {

    var buildPromptCallCount: Int = 0
        private set

    var lastTurn: Turn? = null
        private set

    var lastReasoningContext: ReasoningContext? = null
        private set

    override fun buildPrompt(turn: Turn, reasoningContext: ReasoningContext): String {
        buildPromptCallCount++
        lastTurn = turn
        lastReasoningContext = reasoningContext
        return promptFor(turn, reasoningContext)
    }
}
