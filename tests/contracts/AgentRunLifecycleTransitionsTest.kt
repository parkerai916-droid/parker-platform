package parker.core.interfaces

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves the state machine matches `AgentRuntimeSpecification.md` Section
 * 5's diagram exactly: every edge in the diagram is accepted, and a
 * representative set of non-edges (including "skip a step," "go
 * backwards," and every terminal state) are rejected. Mirrors
 * `ExecutionLifecycleTransitionsTest.kt` / `TaskLifecycleTransitionsTest.kt`'s
 * existing style exactly.
 *
 * Sprint 1, Unit 7: this file is the negative test at the Agent Run
 * lifecycle the Test Plan requires ("Terminal states remain final"),
 * independent of [InMemoryAgentRuntimeTest], which only ever drives
 * `CREATED -> INITIALISED -> READY -> RUNNING -> {COMPLETED, FAILED}`.
 */
class AgentRunLifecycleTransitionsTest {

    @Test
    fun `every edge in the state diagram is a valid transition`() {
        val edges = listOf(
            AgentRunStatus.CREATED to AgentRunStatus.INITIALISED,
            AgentRunStatus.CREATED to AgentRunStatus.CANCELLED,
            AgentRunStatus.INITIALISED to AgentRunStatus.READY,
            AgentRunStatus.INITIALISED to AgentRunStatus.CANCELLED,
            AgentRunStatus.INITIALISED to AgentRunStatus.FAILED,
            AgentRunStatus.READY to AgentRunStatus.RUNNING,
            AgentRunStatus.READY to AgentRunStatus.CANCELLED,
            AgentRunStatus.READY to AgentRunStatus.FAILED,
            AgentRunStatus.RUNNING to AgentRunStatus.WAITING_FOR_PERMISSION,
            AgentRunStatus.RUNNING to AgentRunStatus.WAITING_FOR_INPUT,
            AgentRunStatus.RUNNING to AgentRunStatus.SUSPENDED,
            AgentRunStatus.RUNNING to AgentRunStatus.COMPLETED,
            AgentRunStatus.RUNNING to AgentRunStatus.FAILED,
            AgentRunStatus.RUNNING to AgentRunStatus.CANCELLED,
            AgentRunStatus.WAITING_FOR_PERMISSION to AgentRunStatus.RUNNING,
            AgentRunStatus.WAITING_FOR_PERMISSION to AgentRunStatus.SUSPENDED,
            AgentRunStatus.WAITING_FOR_PERMISSION to AgentRunStatus.FAILED,
            AgentRunStatus.WAITING_FOR_PERMISSION to AgentRunStatus.CANCELLED,
            AgentRunStatus.WAITING_FOR_INPUT to AgentRunStatus.RUNNING,
            AgentRunStatus.WAITING_FOR_INPUT to AgentRunStatus.SUSPENDED,
            AgentRunStatus.WAITING_FOR_INPUT to AgentRunStatus.CANCELLED,
            AgentRunStatus.SUSPENDED to AgentRunStatus.RUNNING,
            AgentRunStatus.SUSPENDED to AgentRunStatus.CANCELLED,
            AgentRunStatus.SUSPENDED to AgentRunStatus.FAILED,
        )

        edges.forEach { (from, to) ->
            assertTrue(AgentRunLifecycleTransitions.isValidTransition(from, to), "$from -> $to should be valid")
            AgentRunLifecycleTransitions.requireValidTransition(from, to) // must not throw
        }
    }

    @Test
    fun `there is no Created to Failed edge -- an unresolvable identity leaves the run stuck at Created`() {
        // AgentRuntimeSpecification.md Section 7: "an Agent Instance cannot proceed past CREATED"
        // -- it stays there, it does not fail outright. Unlike TaskStatus, which does have a
        // Created -> Failed edge, this state machine deliberately has none.
        assertFalse(AgentRunLifecycleTransitions.isValidTransition(AgentRunStatus.CREATED, AgentRunStatus.FAILED))
        assertFailsWith<IllegalArgumentException> {
            AgentRunLifecycleTransitions.requireValidTransition(AgentRunStatus.CREATED, AgentRunStatus.FAILED)
        }
    }

    @Test
    fun `skipping straight from Created to Running is rejected -- identity resolution cannot be skipped`() {
        assertFalse(AgentRunLifecycleTransitions.isValidTransition(AgentRunStatus.CREATED, AgentRunStatus.RUNNING))
        assertFailsWith<IllegalArgumentException> {
            AgentRunLifecycleTransitions.requireValidTransition(AgentRunStatus.CREATED, AgentRunStatus.RUNNING)
        }
    }

    @Test
    fun `Suspended cannot go directly to Completed -- it must resume to Running first`() {
        assertFalse(AgentRunLifecycleTransitions.isValidTransition(AgentRunStatus.SUSPENDED, AgentRunStatus.COMPLETED))
        assertFailsWith<IllegalArgumentException> {
            AgentRunLifecycleTransitions.requireValidTransition(AgentRunStatus.SUSPENDED, AgentRunStatus.COMPLETED)
        }
    }

    @Test
    fun `WaitingForInput cannot go directly to WaitingForPermission, or vice versa`() {
        assertFalse(
            AgentRunLifecycleTransitions.isValidTransition(
                AgentRunStatus.WAITING_FOR_INPUT,
                AgentRunStatus.WAITING_FOR_PERMISSION,
            ),
        )
        assertFalse(
            AgentRunLifecycleTransitions.isValidTransition(
                AgentRunStatus.WAITING_FOR_PERMISSION,
                AgentRunStatus.WAITING_FOR_INPUT,
            ),
        )
    }

    @Test
    fun `a Completed Agent Run cannot transition anywhere -- it is terminal`() {
        assertTrue(AgentRunLifecycleTransitions.isTerminal(AgentRunStatus.COMPLETED))
        AgentRunStatus.entries
            .filter { it != AgentRunStatus.COMPLETED }
            .forEach { candidate ->
                assertFalse(AgentRunLifecycleTransitions.isValidTransition(AgentRunStatus.COMPLETED, candidate))
            }
    }

    @Test
    fun `every terminal state in the diagram is recognised as terminal`() {
        listOf(
            AgentRunStatus.COMPLETED,
            AgentRunStatus.FAILED,
            AgentRunStatus.CANCELLED,
        ).forEach { assertTrue(AgentRunLifecycleTransitions.isTerminal(it), "$it should be terminal") }
    }

    @Test
    fun `no non-terminal state is misreported as terminal`() {
        listOf(
            AgentRunStatus.CREATED,
            AgentRunStatus.INITIALISED,
            AgentRunStatus.READY,
            AgentRunStatus.RUNNING,
            AgentRunStatus.WAITING_FOR_PERMISSION,
            AgentRunStatus.WAITING_FOR_INPUT,
            AgentRunStatus.SUSPENDED,
        ).forEach { assertFalse(AgentRunLifecycleTransitions.isTerminal(it), "$it should not be terminal") }
    }
}
