package parker.core.interfaces

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves the state machine matches docs/diagrams/execution-state-machine.mmd
 * exactly: every edge in the diagram is accepted, and a representative set
 * of non-edges (including "skip a step" and "go backwards") are rejected.
 */
class ExecutionLifecycleTransitionsTest {

    @Test
    fun `every edge in the state diagram is a valid transition`() {
        val edges = listOf(
            ExecutionLifecycleState.CREATED to ExecutionLifecycleState.VALIDATED,
            ExecutionLifecycleState.CREATED to ExecutionLifecycleState.EXPIRED,
            ExecutionLifecycleState.CREATED to ExecutionLifecycleState.FAILED,
            ExecutionLifecycleState.VALIDATED to ExecutionLifecycleState.PERMISSION_PENDING,
            ExecutionLifecycleState.PERMISSION_PENDING to ExecutionLifecycleState.APPROVED,
            ExecutionLifecycleState.PERMISSION_PENDING to ExecutionLifecycleState.DENIED,
            ExecutionLifecycleState.PERMISSION_PENDING to ExecutionLifecycleState.DEFERRED,
            ExecutionLifecycleState.APPROVED to ExecutionLifecycleState.QUEUED,
            ExecutionLifecycleState.QUEUED to ExecutionLifecycleState.EXECUTING,
            ExecutionLifecycleState.QUEUED to ExecutionLifecycleState.CANCELLED,
            ExecutionLifecycleState.EXECUTING to ExecutionLifecycleState.COMPLETED,
            ExecutionLifecycleState.EXECUTING to ExecutionLifecycleState.FAILED,
        )

        edges.forEach { (from, to) ->
            assertTrue(ExecutionLifecycleTransitions.isValidTransition(from, to), "$from -> $to should be valid")
            ExecutionLifecycleTransitions.requireValidTransition(from, to) // must not throw
        }
    }

    @Test
    fun `Created can go directly to Failed -- a validation failure, not a Denied decision`() {
        // Added by the targeted refinement pass (IMPLEMENTATION_GAPS.md #31): a request that fails
        // validation (unresolvable target Resource, or an action-mapping failure -- see
        // action-mapping.md's "Invalid, not Denied") never reaches PermissionPending at all.
        assertTrue(
            ExecutionLifecycleTransitions.isValidTransition(
                ExecutionLifecycleState.CREATED,
                ExecutionLifecycleState.FAILED,
            ),
        )
        ExecutionLifecycleTransitions.requireValidTransition(ExecutionLifecycleState.CREATED, ExecutionLifecycleState.FAILED) // must not throw
    }

    @Test
    fun `skipping straight from Created to Executing is rejected`() {
        assertFalse(
            ExecutionLifecycleTransitions.isValidTransition(
                ExecutionLifecycleState.CREATED,
                ExecutionLifecycleState.EXECUTING,
            ),
        )
        assertFailsWith<IllegalArgumentException> {
            ExecutionLifecycleTransitions.requireValidTransition(
                ExecutionLifecycleState.CREATED,
                ExecutionLifecycleState.EXECUTING,
            )
        }
    }

    @Test
    fun `moving backwards from Completed to Executing is rejected`() {
        assertFalse(
            ExecutionLifecycleTransitions.isValidTransition(
                ExecutionLifecycleState.COMPLETED,
                ExecutionLifecycleState.EXECUTING,
            ),
        )
    }

    @Test
    fun `a Denied request cannot transition anywhere -- it is terminal`() {
        assertTrue(ExecutionLifecycleTransitions.isTerminal(ExecutionLifecycleState.DENIED))
        ExecutionLifecycleState.entries
            .filter { it != ExecutionLifecycleState.DENIED }
            .forEach { candidate ->
                assertFalse(ExecutionLifecycleTransitions.isValidTransition(ExecutionLifecycleState.DENIED, candidate))
            }
    }

    @Test
    fun `every terminal state in the diagram is recognised as terminal`() {
        listOf(
            ExecutionLifecycleState.COMPLETED,
            ExecutionLifecycleState.FAILED,
            ExecutionLifecycleState.DENIED,
            ExecutionLifecycleState.DEFERRED,
            ExecutionLifecycleState.CANCELLED,
            ExecutionLifecycleState.EXPIRED,
        ).forEach { assertTrue(ExecutionLifecycleTransitions.isTerminal(it), "$it should be terminal") }
    }
}
