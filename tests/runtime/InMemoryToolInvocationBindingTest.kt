package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceType
import parker.core.interfaces.Tool
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolResult
import parker.core.interfaces.ValidationResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Sprint 1, Unit 3 acceptance test.
 *
 * Provenance: docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md, Section
 * 6, Unit 3 ("Tool-binding mechanism (resolve -> invocable Tool)"):
 * "register a Mock Tool, resolve it, confirm the Execution Pipeline (and
 * only the Execution Pipeline) can obtain an invocable reference."
 *
 * This test proves the binding mechanism itself -- [InMemoryToolRegistry]
 * (the "resolve" half of that acceptance test) is unchanged and already
 * covered by tests/runtime/InMemoryToolRegistryTest.kt. A minimal,
 * private, test-local [Tool] stub is used here on purpose: the reusable
 * `tests/runtime/MockTool.kt` fixture is Sprint 1's Unit 4, a separate,
 * not-yet-implemented unit that depends on this one -- this file must not
 * anticipate it.
 *
 * The "Execution Pipeline (and only the Execution Pipeline) can obtain an
 * invocable reference" half of the acceptance test is proven only to the
 * extent the existing repository already proves the identical claim for
 * [ToolRegistry.resolve]: by documentation, not by a code-level
 * caller-identity check. See [InMemoryToolInvocationBinding]'s class-level
 * doc comment for why this test does not go further than that -- doing so
 * would introduce a new access-control mechanism not present anywhere else
 * in this repository, which is outside this unit's scope.
 */
class InMemoryToolInvocationBindingTest {

    private class StubTool(override val descriptor: ToolDescriptor) : Tool {
        override suspend fun validate(request: ExecutionRequest): ValidationResult = ValidationResult.Valid
        override suspend fun execute(request: ExecutionRequest): ToolResult =
            ToolResult(toolId = descriptor.toolId, success = true)
    }

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

    // --- bind / invocableFor round-trip ---

    @Test
    fun `a Tool bound to a descriptor is invocable for that same descriptor`() = runTest {
        val binding = InMemoryToolInvocationBinding()
        val d = descriptor()
        val tool = StubTool(d)

        binding.bind(d, tool)
        val invocable = binding.invocableFor(d)

        assertEquals(tool, invocable)
    }

    @Test
    fun `a resolved descriptor with no bound Tool yields null, not an exception`() = runTest {
        val binding = InMemoryToolInvocationBinding()

        assertNull(binding.invocableFor(descriptor()))
    }

    @Test
    fun `the bound Tool is actually invocable -- execute can be called and returns a real ToolResult`() = runTest {
        val binding = InMemoryToolInvocationBinding()
        val d = descriptor()
        binding.bind(d, StubTool(d))

        val tool = binding.invocableFor(d)
        requireNotNull(tool)
        val result = tool.execute(executionRequest())

        assertEquals(d.toolId, result.toolId)
        assertEquals(true, result.success)
    }

    // --- descriptor/version identity ---

    @Test
    fun `bind rejects a Tool whose own descriptor does not match the descriptor it is bound to`() = runTest {
        val binding = InMemoryToolInvocationBinding()
        val d = descriptor()
        val mismatchedTool = StubTool(d.copy(toolId = "tool.other"))

        assertFailsWith<IllegalArgumentException> {
            binding.bind(d, mismatchedTool)
        }
    }

    @Test
    fun `different versions of the same toolId are bound and resolved independently`() = runTest {
        val binding = InMemoryToolInvocationBinding()
        val v1 = descriptor(version = "1.0.0")
        val v2 = descriptor(version = "2.0.0")

        binding.bind(v1, StubTool(v1))
        binding.bind(v2, StubTool(v2))

        assertEquals(v1, binding.invocableFor(v1)?.descriptor)
        assertEquals(v2, binding.invocableFor(v2)?.descriptor)
    }

    @Test
    fun `re-binding the same descriptor replaces the previously bound Tool`() = runTest {
        val binding = InMemoryToolInvocationBinding()
        val d = descriptor()
        val first = StubTool(d)
        val second = StubTool(d)

        binding.bind(d, first)
        binding.bind(d, second)

        assertEquals(second, binding.invocableFor(d))
    }
}
