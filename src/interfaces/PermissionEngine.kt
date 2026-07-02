package parker.core.interfaces

interface PermissionEngine {
    suspend fun evaluate(request: ExecutionRequest): PermissionDecision
    suspend fun explain(decisionId: DecisionId): PermissionExplanation
}
