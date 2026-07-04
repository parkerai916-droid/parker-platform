package parker.core.interfaces

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves the state machine matches
 * `docs/diagrams/task-lifecycle-state-machine.mmd` exactly: every edge in
 * the diagram is accepted, and a representative set of non-edges
 * (including "skip a step," "go backwards," and every terminal state) are
 * rejected. Mirrors `ExecutionLifecycleTransitionsTest.kt`'s existing
 * style exactly.
 *
 * Sprint 1, Unit 6
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §7, "Terminal
 * states remain final"): this file is the negative test at the Task
 * Manager Task lifecycle the Test Plan requires, independent of
 * [InMemoryTaskManagerRuntimeTest], which only ever drives
 * `CREATED -> QUEUED` and does not itself exercise a terminal state.
 */
class TaskLifecycleTransitionsTest {

    @Test
    fun `every edge in the state diagram is a valid transition`() {
        val edges = listOf(
            TaskStatus.CREATED to TaskStatus.QUEUED,
            TaskStatus.CREATED to TaskStatus.CANCELLED,
            TaskStatus.CREATED to TaskStatus.SUPERSEDED,
            TaskStatus.QUEUED to TaskStatus.RUNNING,
            TaskStatus.QUEUED to TaskStatus.CANCELLED,
            TaskStatus.QUEUED to TaskStatus.EXPIRED,
            TaskStatus.QUEUED to TaskStatus.SUPERSEDED,
            TaskStatus.RUNNING to TaskStatus.PAUSED,
            TaskStatus.RUNNING to TaskStatus.COMPLETED,
            TaskStatus.RUNNING to TaskStatus.FAILED,
            TaskStatus.RUNNING to TaskStatus.CANCELLED,
            TaskStatus.PAUSED to TaskStatus.RUNNING,
            TaskStatus.PAUSED to TaskStatus.CANCELLED,
            TaskStatus.PAUSED to TaskStatus.EXPIRED,
            TaskStatus.PAUSED to TaskStatus.SUPERSEDED,
        )

        edges.forEach { (from, to) ->
            assertTrue(TaskLifecycleTransitions.isValidTransition(from, to), "$from -> $to should be valid")
            TaskLifecycleTransitions.requireValidTransition(from, to) // must not throw
        }
    }

    @Test
    fun `Superseded is not reachable from Running -- a deliberate, already-specified design choice`() {
        assertFalse(TaskLifecycleTransitions.isValidTransition(TaskStatus.RUNNING, TaskStatus.SUPERSEDED))
        assertFailsWith<IllegalArgumentException> {
            TaskLifecycleTransitions.requireValidTransition(TaskStatus.RUNNING, TaskStatus.SUPERSEDED)
        }
    }

    @Test
    fun `Failed is reachable only from Running`() {
        assertTrue(TaskLifecycleTransitions.isValidTransition(TaskStatus.RUNNING, TaskStatus.FAILED))
        listOf(TaskStatus.CREATED, TaskStatus.QUEUED, TaskStatus.PAUSED).forEach { from ->
            assertFalse(
                TaskLifecycleTransitions.isValidTransition(from, TaskStatus.FAILED),
                "$from -> FAILED should be rejected",
            )
        }
    }

    @Test
    fun `skipping straight from Created to Running is rejected`() {
        assertFalse(TaskLifecycleTransitions.isValidTransition(TaskStatus.CREATED, TaskStatus.RUNNING))
        assertFailsWith<IllegalArgumentException> {
            TaskLifecycleTransitions.requireValidTransition(TaskStatus.CREATED, TaskStatus.RUNNING)
        }
    }

    @Test
    fun `moving backwards from Completed to Running is rejected`() {
        assertFalse(TaskLifecycleTransitions.isValidTransition(TaskStatus.COMPLETED, TaskStatus.RUNNING))
        assertFailsWith<IllegalArgumentException> {
            TaskLifecycleTransitions.requireValidTransition(TaskStatus.COMPLETED, TaskStatus.RUNNING)
        }
    }

    @Test
    fun `a Completed Task cannot transition anywhere -- it is terminal`() {
        assertTrue(TaskLifecycleTransitions.isTerminal(TaskStatus.COMPLETED))
        TaskStatus.entries
            .filter { it != TaskStatus.COMPLETED }
            .forEach { candidate ->
                assertFalse(TaskLifecycleTransitions.isValidTransition(TaskStatus.COMPLETED, candidate))
            }
    }

    @Test
    fun `every terminal state in the diagram is recognised as terminal`() {
        listOf(
            TaskStatus.COMPLETED,
            TaskStatus.FAILED,
            TaskStatus.CANCELLED,
            TaskStatus.EXPIRED,
            TaskStatus.SUPERSEDED,
        ).forEach { assertTrue(TaskLifecycleTransitions.isTerminal(it), "$it should be terminal") }
    }

    @Test
    fun `no non-terminal state is misreported as terminal`() {
        listOf(
            TaskStatus.CREATED,
            TaskStatus.QUEUED,
            TaskStatus.RUNNING,
            TaskStatus.PAUSED,
        ).forEach { assertFalse(TaskLifecycleTransitions.isTerminal(it), "$it should not be terminal") }
    }
}
