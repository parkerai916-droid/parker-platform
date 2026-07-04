package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceType
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolResult
import parker.core.interfaces.ValidationResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Sprint 1, Unit 4 acceptance test
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6, Unit 4):
 * "MockTool.execute(request) returns a deterministic ToolResult for a
 * fixed input."
 *
 * Also proves [MockTool] is a drop-in replacement for the private
 * `StubTool` `InMemoryToolInvocationBindingTest.kt` used before this unit
 * existed: it binds, resolves, and invokes through
 * [InMemoryToolInvocationBinding] exactly the same way. This file does not
 * touch that existing test or replace its stub -- both are intentionally
 * left as-is per the "do not alter existing tests unless strictly
 * required" constraint on this unit.
 *
 * Scope note: this file proves Unit 4 (the fixture itself, and that it
 * composes with Unit 3's binding). It does not construct a
 * DefaultExecutionPipeline, ToolRegistry.resolve call, Planner, Task
 * Manager, or Agent Runtime -- those are Units 5 onward and are
 * deliberately absent here.
 */
class MockToolTest {

    private fun descriptor(toolId: String = "tool.calendar.read", version: String = "1.0.0") = ToolDescriptor(
        toolId = toolId,
        displayName = "Calendar Reader",
        description = "Reads calendar entries",
        version = version,
        supportedActions = setOf(PermissionAction.READ),
        supportedResourceTypes = setOf(ResourceType.CALENDAR),
    )

    private fun executionRequest(intent: String = "read today's calendar") = ExecutionRequest(
        requestId = RequestId("req-1"),
        principalId = PrincipalId("user-1"),
        origin = RequestOrigin.AGENT,
        intent = intent,
        targetResources = emptyList(),
        proposedActions = listOf("READ"),
        priority = RequestPriority.NORMAL,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = "corr-1",
    )

    // --- 1. valid descriptor ---

    @Test
    fun `MockTool exposes the exact descriptor it was constructed with`() {
        val d = descriptor()
        val tool = MockTool(d)

        assertEquals(d, tool.descriptor)
    }

    // --- 2 & 3. bindable through InMemoryToolInvocationBinding, retrievable by descriptor ---

    @Test
    fun `MockTool can be bound and retrieved through InMemoryToolInvocationBinding`() = runTest {
        val binding = InMemoryToolInvocationBinding()
        val d = descriptor()
        val tool = MockTool(d)

        binding.bind(d, tool)
        val retrieved = binding.invocableFor(d)

        assertSame(tool, retrieved)
    }

    // --- 4. deterministic invocation ---

    @Test
    fun `execute returns a deterministic ToolResult for a fixed input`() = runTest {
        val d = descriptor()
        val tool = MockTool(d)
        val request = executionRequest()

        val first = tool.execute(request)
        val second = tool.execute(request)

        assertEquals(first, second)
        assertEquals(d.toolId, first.toolId)
        assertTrue(first.success)
    }

    @Test
    fun `execute invoked through a resolved binding still returns the deterministic result`() = runTest {
        val binding = InMemoryToolInvocationBinding()
        val d = descriptor()
        binding.bind(d, MockTool(d))

        val invocable = binding.invocableFor(d)
        assertNotNull(invocable)
        val result = invocable.execute(executionRequest())

        assertEquals(d.toolId, result.toolId)
        assertTrue(result.success)
    }

    @Test
    fun `validate and execute call counts are tracked independently and accurately`() = runTest {
        val tool = MockTool(descriptor())
        val request = executionRequest()

        tool.validate(request)
        tool.validate(request)
        tool.execute(request)

        assertEquals(2, tool.validateCallCount)
        assertEquals(1, tool.executeCallCount)
    }

    @Test
    fun `failure can be simulated deterministically via the existing ToolResult contract`() = runTest {
        val d = descriptor()
        val tool = MockTool(
            descriptor = d,
            resultFor = { ToolResult(toolId = d.toolId, success = false, errorMessage = "simulated failure") },
        )

        val result = tool.execute(executionRequest())

        assertFalse(result.success)
        assertEquals("simulated failure", result.errorMessage)
    }

    @Test
    fun `an Invalid validation result can be simulated deterministically`() = runTest {
        val d = descriptor()
        val tool = MockTool(
            descriptor = d,
            validation = ValidationResult.Invalid(listOf("simulated validation failure")),
        )

        val result = tool.validate(executionRequest())

        val invalid = assertIs<ValidationResult.Invalid>(result)
        assertEquals(listOf("simulated validation failure"), invalid.reasons)
    }

    // --- 5. descriptor mismatch protection still works (no regression from Unit 3) ---

    @Test
    fun `binding still rejects a MockTool whose descriptor does not match the bind target`() = runTest {
        val binding = InMemoryToolInvocationBinding()
        val d = descriptor()
        val mismatched = MockTool(d.copy(toolId = "tool.other"))

        assertFailsWith<IllegalArgumentException> {
            binding.bind(d, mismatched)
        }
    }
}
