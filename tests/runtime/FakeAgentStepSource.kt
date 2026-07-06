package parker.core.runtime

import parker.core.interfaces.AgentStepContext
import parker.core.interfaces.AgentStepDecision
import parker.core.interfaces.AgentStepSource

/**
 * Test-only fake for Sprint 3, Track C, Unit C2. Mirrors
 * [FakePermissionEngine]'s exact "lambda-based decision provider + call
 * count" precedent. Deliberately does NOT live in `src/runtime` -- a real
 * [AgentStepSource] requires a Planner or other real reasoning component
 * this repository does not specify or implement anywhere yet
 * (`docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md` Section 11).
 *
 * [decisionFor] is given the full [AgentStepContext] for each call, so
 * tests can route on `context.taskId` (distinguishing independent Agent
 * Runs sharing one fake) or `context.stepNumber` (scripting a fixed
 * sequence of decisions), exactly as [FakePermissionEngine] is given the
 * full `ExecutionRequest`.
 */
class FakeAgentStepSource(
    private val decisionFor: (AgentStepContext) -> AgentStepDecision,
) : AgentStepSource {

    var nextStepCallCount: Int = 0
        private set

    override suspend fun nextStep(context: AgentStepContext): AgentStepDecision {
        nextStepCallCount++
        return decisionFor(context)
    }
}
