package parker.core.runtime

import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionExplanation

/**
 * Test-only fake. Deliberately does NOT live in src/runtime -- a real
 * PermissionEngine requires authorisation policy that is not specified
 * anywhere in the architecture yet (IMPLEMENTATION_GAPS.md #25/#30).
 * This fake exists solely so DefaultExecutionPipelineTest can prove the
 * *orchestration* (does the pipeline call evaluate, and correctly branch
 * on APPROVED/DENIED/DEFERRED) without this test suite inventing policy
 * either.
 */
class FakePermissionEngine(
    private val decisionFor: (ExecutionRequest) -> PermissionDecision,
) : PermissionEngine {

    var evaluateCallCount: Int = 0
        private set

    override suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
        evaluateCallCount++
        return decisionFor(request)
    }

    override suspend fun explain(decisionId: DecisionId): PermissionExplanation =
        PermissionExplanation(decisionId, "fake explanation for test")
}
