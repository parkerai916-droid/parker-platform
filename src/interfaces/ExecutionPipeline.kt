package parker.core.interfaces

interface ExecutionPipeline {
    suspend fun submit(request: ExecutionRequest): ExecutionResult
    suspend fun cancel(requestId: RequestId): CancellationResult
    suspend fun status(requestId: RequestId): ExecutionStatus
}
