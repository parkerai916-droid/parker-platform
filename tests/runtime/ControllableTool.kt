package parker.core.runtime

import kotlinx.coroutines.CompletableDeferred
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.Tool
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolResult
import parker.core.interfaces.ValidationResult

/**
 * Test-only fixture for Sprint 3, Track C, Unit C2's concurrency
 * requirements: that [InMemoryAgentRuntime]'s own Mutex is never held
 * while `ExecutionPipeline.submit` is in flight, and that `SUSPEND`/
 * `CANCEL` received while a step is executing are honoured immediately
 * (`CANCEL`) or at the next step boundary (`SUSPEND`), not by
 * interrupting the in-flight step. [MockTool]'s immediately-returning
 * `execute()` cannot exercise either property -- this fixture can, by
 * genuinely suspending mid-`execute()` until a test explicitly releases
 * it. Mirrors [MockTool]'s own "test-only fixture, not `src/runtime`"
 * precedent exactly.
 *
 * [executeStarted] completes the moment [execute] is entered -- a test
 * `await`s this to know the step is genuinely in flight before issuing a
 * concurrent command. [execute] then suspends until the test calls
 * [complete] with whatever [ToolResult] it wants that step to produce.
 */
class ControllableTool(
    override val descriptor: ToolDescriptor,
) : Tool {

    val executeStarted = CompletableDeferred<Unit>()
    private val release = CompletableDeferred<ToolResult>()

    var validateCallCount: Int = 0
        private set

    var executeCallCount: Int = 0
        private set

    override suspend fun validate(request: ExecutionRequest): ValidationResult {
        validateCallCount++
        return ValidationResult.Valid
    }

    override suspend fun execute(request: ExecutionRequest): ToolResult {
        executeCallCount++
        executeStarted.complete(Unit)
        return release.await()
    }

    /** Lets a paused [execute] call return [result]. */
    fun complete(result: ToolResult) {
        release.complete(result)
    }
}
