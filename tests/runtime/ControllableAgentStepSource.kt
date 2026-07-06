package parker.core.runtime

import kotlinx.coroutines.CompletableDeferred
import parker.core.interfaces.AgentStepContext
import parker.core.interfaces.AgentStepDecision
import parker.core.interfaces.AgentStepSource

/**
 * Test-only fixture, mirroring [ControllableTool]'s own "genuinely
 * pausable, not a plain lambda fake" precedent exactly, for tests that
 * need to submit a command (e.g. `SUSPEND`) while an Agent Run is
 * genuinely `RUNNING` and mid-`nextStep`, before that step's own decision
 * has even been returned -- a moment [FakeAgentStepSource]'s plain,
 * non-`suspend` `decisionFor: (AgentStepContext) -> AgentStepDecision`
 * lambda type cannot reach into, since calling a `suspend` function (such
 * as [InMemoryAgentRuntime.submit]) requires a real `suspend` function
 * body, not a plain lambda. [nextStep] itself is declared `suspend`,
 * matching [AgentStepSource]'s own interface, so a test can call another
 * `suspend` function from a coroutine that awaits [enteredStep1] before
 * this class's own first call to it returns.
 *
 * The first (`stepNumber == 1`) call to [nextStep] completes
 * [enteredStep1] and then suspends until a test calls [releaseStep1] with
 * the [AgentStepDecision] that call should return. Every later call is
 * answered immediately by [afterFirstStep] (`Complete` by default, since
 * neither test using this fixture needs a second real step to do
 * anything other than end the Agent Run).
 */
class ControllableAgentStepSource(
    private val afterFirstStep: (AgentStepContext) -> AgentStepDecision = { AgentStepDecision.Complete },
) : AgentStepSource {

    val enteredStep1 = CompletableDeferred<Unit>()
    private val step1Decision = CompletableDeferred<AgentStepDecision>()

    var nextStepCallCount: Int = 0
        private set

    /** Every `AgentStepContext.stepNumber` this source has been consulted with, in call order. */
    val stepNumbersSeen: MutableList<Int> = mutableListOf()

    override suspend fun nextStep(context: AgentStepContext): AgentStepDecision {
        nextStepCallCount++
        stepNumbersSeen += context.stepNumber
        return if (context.stepNumber == 1) {
            enteredStep1.complete(Unit)
            step1Decision.await()
        } else {
            afterFirstStep(context)
        }
    }

    /** Lets the paused first [nextStep] call return [decision]. */
    fun releaseStep1(decision: AgentStepDecision) {
        step1Decision.complete(decision)
    }
}
