package parker.core.runtime

import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.Tool
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolResult
import parker.core.interfaces.ValidationResult

/**
 * Sprint 1, Unit 4 (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md`
 * §6): a deterministic, in-memory [Tool] with no real external effect.
 *
 * Test-only fixture. Deliberately does NOT live in `src/runtime` --
 * mirroring [FakePermissionEngine]'s existing "test fixture, not
 * `src/runtime`" precedent exactly: this Tool exists only to prove the
 * resolve -> bind -> invoke path Units 1-3 built, not as a real platform
 * Tool. It supersedes the private `StubTool` used inline in
 * `InMemoryToolInvocationBindingTest.kt` (that file predates this unit and
 * was deliberately not written to anticipate it) by being reusable across
 * Unit 4 and later units (Unit 5 onward) that need a real, bindable,
 * invocable Tool without inventing a new one each time.
 *
 * Deterministic by construction: [validate] and [execute] each return a
 * fixed, caller-supplied result for every call -- no internal state
 * affects the outcome. [validateCallCount] and [executeCallCount] let
 * tests assert exactly how many times each was invoked, the same
 * assertion style [FakePermissionEngine.evaluateCallCount] already
 * establishes.
 *
 * Failure/error simulation is supported the same way success is: by
 * supplying a [resultFor] that returns a [ToolResult] with `success =
 * false` and a non-null `errorMessage` -- both already part of the
 * existing [ToolResult] contract (`src/contracts/ExecutionResult.kt`).
 * No new result shape or failure mechanism is introduced.
 */
class MockTool(
    override val descriptor: ToolDescriptor,
    private val validation: ValidationResult = ValidationResult.Valid,
    private val resultFor: (ExecutionRequest) -> ToolResult = { request ->
        ToolResult(
            toolId = descriptor.toolId,
            success = true,
            output = mapOf("intent" to request.intent),
        )
    },
) : Tool {

    var validateCallCount: Int = 0
        private set

    var executeCallCount: Int = 0
        private set

    override suspend fun validate(request: ExecutionRequest): ValidationResult {
        validateCallCount++
        return validation
    }

    override suspend fun execute(request: ExecutionRequest): ToolResult {
        executeCallCount++
        return resultFor(request)
    }
}
