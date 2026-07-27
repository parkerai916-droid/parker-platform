package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AgentRunId
import parker.core.interfaces.AgentStepContext
import parker.core.interfaces.AgentStepDecision
import parker.core.interfaces.ResourceId
import parker.core.interfaces.TaskId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Controlled Agent Run Submission
 * (`docs/implementation/CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md`
 * Section 11, item 3): direct unit coverage of [DeterministicAgentStepSource]'s
 * `Propose`-then-`Complete` contract (Section 10), mirroring
 * `tests/runtime/SingleStepAgentStepSource.kt`'s own tested shape -- the two
 * classes are deliberately behaviourally identical (Section 10's own text),
 * so this test exists to prove the *new, separate production class* honours
 * that same contract independently, not to duplicate a different fixture's
 * coverage under a different name.
 */
class DeterministicAgentStepSourceTest {

    private fun context(stepNumber: Int, resourceReferences: List<ResourceId> = emptyList()) = AgentStepContext(
        agentRunId = AgentRunId("run-for-task-1"),
        taskId = TaskId("task-1"),
        goal = "read today's calendar",
        stepNumber = stepNumber,
        priorResult = null,
        resourceReferences = resourceReferences,
        deniedActions = emptyList(),
    )

    @Test
    fun `step 1 proposes the goal as the action, carrying forward the context's own resource references`() = runTest {
        val source = DeterministicAgentStepSource()
        val resourceReferences = listOf(ResourceId("res.calendar.1"))

        val decision = source.nextStep(context(stepNumber = 1, resourceReferences = resourceReferences))

        val propose = assertIs<AgentStepDecision.Propose>(decision)
        assertEquals("read today's calendar", propose.proposedAction)
        assertEquals(resourceReferences, propose.targetResources)
    }

    @Test
    fun `step 2 and beyond always decide Complete`() = runTest {
        val source = DeterministicAgentStepSource()

        assertEquals(AgentStepDecision.Complete, source.nextStep(context(stepNumber = 2)))
        assertEquals(AgentStepDecision.Complete, source.nextStep(context(stepNumber = 3)))
    }
}
