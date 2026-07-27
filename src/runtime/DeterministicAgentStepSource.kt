package parker.core.runtime

import parker.core.interfaces.AgentStepContext
import parker.core.interfaces.AgentStepDecision
import parker.core.interfaces.AgentStepSource

/**
 * Controlled Agent Run Submission (`docs/implementation/CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md`
 * Section 10): the fixed, deterministic, non-Planner [AgentStepSource]
 * `src/contracts/AgentStep.kt`'s own KDoc and
 * `docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md` Section 11 already
 * anticipate as the correct stand-in "for any production wiring that
 * exists before a real Planner does."
 *
 * Behaviourally identical to `tests/runtime/SingleStepAgentStepSource.kt`
 * -- deliberately so, since both implement the one fixed shape the design
 * document names -- but this is a **new, separate production
 * implementation, not a relocation or reuse of that test fixture**
 * (Scope Lock decisions 8-9), mirroring `DefaultPlanCandidateGenerator`'s
 * own precedent from the preceding milestone exactly: a production class
 * must live in `src/`, be independently documented as a deliberate
 * stand-in, and never be mistaken for the real thing (a future
 * Planner-backed [AgentStepSource], Chapter 20) it reserves the seam for.
 *
 * No test fixture is deleted, moved, or modified by this class's
 * existence.
 */
class DeterministicAgentStepSource : AgentStepSource {
    override suspend fun nextStep(context: AgentStepContext): AgentStepDecision =
        if (context.stepNumber == 1) {
            AgentStepDecision.Propose(context.goal, context.resourceReferences)
        } else {
            AgentStepDecision.Complete
        }
}
