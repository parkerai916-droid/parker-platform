package parker.core.interfaces

import java.time.Instant

/**
 * Needed by the existing ExecutionPipeline.kt stub
 * (`suspend fun cancel(requestId): CancellationResult`,
 * `suspend fun status(requestId): ExecutionStatus`). Neither type is given
 * a shape anywhere in the specification volumes I read -- both are
 * inferred from how the interface uses them. See IMPLEMENTATION_GAPS.md.
 */
data class CancellationResult(
    val requestId: RequestId,
    val cancelled: Boolean,
    val reason: String? = null,
)

data class ExecutionStatus(
    val requestId: RequestId,
    val state: ExecutionLifecycleState,
    val lastUpdatedAt: Instant,
)
