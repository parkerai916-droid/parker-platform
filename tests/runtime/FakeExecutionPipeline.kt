package parker.core.runtime

import parker.core.interfaces.CancellationResult
import parker.core.interfaces.ExecutionPipeline
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.ExecutionStatus
import parker.core.interfaces.RequestId

/**
 * Test-only fake, mirroring [FakeCommunicationIntake]/[FakePermissionEngine]'s
 * lambda-based fake precedent. Exists so [ResponseDeliveryTest] can prove
 * [ResponseDelivery]'s own orchestration -- does it call `submit` exactly
 * once, with a correctly constructed [ExecutionRequest], and return
 * whatever [ExecutionResult] comes back unchanged -- independently of
 * [DefaultExecutionPipeline]'s own already-tested orchestration logic,
 * which [DefaultExecutionPipelineTest] already covers on its own.
 *
 * Only [submit] is exercised by [ResponseDelivery] (Plan Section 4);
 * [cancel]/[status] are never called by it, so they throw if reached -- a
 * structural guard against this fake silently masking an unexpected
 * dependency on a method [ResponseDelivery] must not call.
 */
class FakeExecutionPipeline(
    private val resultFor: (ExecutionRequest) -> ExecutionResult,
) : ExecutionPipeline {

    var submitCallCount: Int = 0
        private set

    var lastSubmittedRequest: ExecutionRequest? = null
        private set

    override suspend fun submit(request: ExecutionRequest): ExecutionResult {
        submitCallCount++
        lastSubmittedRequest = request
        return resultFor(request)
    }

    override suspend fun cancel(requestId: RequestId): CancellationResult {
        throw UnsupportedOperationException("FakeExecutionPipeline.cancel must not be called by ResponseDelivery")
    }

    override suspend fun status(requestId: RequestId): ExecutionStatus {
        throw UnsupportedOperationException("FakeExecutionPipeline.status must not be called by ResponseDelivery")
    }
}
