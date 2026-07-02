package parker.core.interfaces

import kotlin.reflect.KFunction
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves, by reflection, that the four in-scope src/interfaces stubs still
 * match Volume 3's documented required operations exactly: same function
 * names, same suspend-ness, same parameter and return types. If any of
 * these interfaces silently drifts from the specification (or from the
 * contracts added in this branch), this test fails instead of the drift
 * going unnoticed.
 */
class InterfaceContractShapeTest {

    private fun KFunction<*>.isSuspend(): Boolean = this.isSuspend

    @Test
    fun `ExecutionPipeline matches its Volume 3 specification`() {
        val functions = ExecutionPipeline::class.functions.associateBy { it.name }

        val submit = functions.getValue("submit")
        assertTrue(submit.isSuspend())
        assertEquals(ExecutionResult::class, submit.returnType.classifier)

        val cancel = functions.getValue("cancel")
        assertTrue(cancel.isSuspend())
        assertEquals(CancellationResult::class, cancel.returnType.classifier)

        val status = functions.getValue("status")
        assertTrue(status.isSuspend())
        assertEquals(ExecutionStatus::class, status.returnType.classifier)
    }

    @Test
    fun `PermissionEngine matches its Volume 3 specification`() {
        val functions = PermissionEngine::class.functions.associateBy { it.name }

        val evaluate = functions.getValue("evaluate")
        assertTrue(evaluate.isSuspend())
        assertEquals(PermissionDecision::class, evaluate.returnType.classifier)

        val explain = functions.getValue("explain")
        assertTrue(explain.isSuspend())
        assertEquals(PermissionExplanation::class, explain.returnType.classifier)
    }

    @Test
    fun `ResourceRegistry matches its Volume 3 specification`() {
        val functions = ResourceRegistry::class.functions.associateBy { it.name }

        assertTrue(functions.getValue("register").isSuspend())
        assertTrue(functions.getValue("resolve").isSuspend())
        assertTrue(functions.getValue("update").isSuspend())
        assertTrue(functions.getValue("listByOwner").isSuspend())
    }

    /**
     * Partial proof of Phase 6 requirement #6 ("no tool execution path
     * bypasses the Execution Pipeline and Permission Engine"): Phase 1 adds
     * no concrete Tool/ExecutionPipeline implementation, so there is no
     * running path to bypass yet. What *can* be proven now is structural:
     * a Tool cannot be invoked with anything less than a full, canonical
     * ExecutionRequest -- there is no narrower "just run this input"
     * overload for feature code to reach for instead. Once a concrete
     * implementation exists (Phase 2), carry forward the v0.1 prototype's
     * `@RequiresOptIn` gate pattern (see the reconciliation report, S5) to
     * close the bypass at compile time the same way.
     */
    @Test
    fun `Tool execute only accepts the canonical ExecutionRequest, nothing narrower`() {
        val execute = Tool::class.functions.single { it.name == "execute" }
        val nonReceiverParams = execute.parameters.filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }

        assertEquals(1, nonReceiverParams.size, "Tool.execute must take exactly one value parameter")
        assertEquals(ExecutionRequest::class, nonReceiverParams.single().type.classifier)
        assertEquals(ToolResult::class, execute.returnType.classifier)
        assertTrue(execute.isSuspend())

        val descriptor = Tool::class.memberProperties.single { it.name == "descriptor" }
        assertEquals(ToolDescriptor::class, descriptor.returnType.classifier)
    }
}
