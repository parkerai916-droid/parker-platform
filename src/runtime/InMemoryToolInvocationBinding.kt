package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.Tool
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolInvocationBinding

/**
 * In-memory implementation of [ToolInvocationBinding], closing
 * `IMPLEMENTATION_GAPS.md` #32 per
 * `docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` Unit 3
 * ("Tool-binding mechanism (resolve -> invocable Tool)").
 *
 * Mirrors [InMemoryToolRegistry]'s own conventions exactly: a single
 * [Mutex]-guarded map, keyed by `(toolId, version)` -- the same identity a
 * [ToolDescriptor] already carries and [InMemoryToolRegistry] already keys
 * its own entries by. This is deliberately the smallest possible addition:
 * it does not change how [ToolRegistry.resolve] decides which descriptor
 * matches a request, and it does not touch [InMemoryToolRegistry] at all.
 * It only adds the one missing step *after* a descriptor is already
 * resolved -- binding it to something callable -- exactly as
 * [ToolInvocationBinding]'s own doc comment describes.
 *
 * [bind] requires that [Tool.descriptor] equal the [ToolDescriptor] it is
 * being bound to, mirroring [InMemoryToolRegistry.register]'s existing
 * descriptor-consistency check for the same reason: a Tool must not be
 * discoverable under a descriptor that does not describe it.
 *
 * ACCESS RESTRICTION -- reported, not silently strengthened: this class
 * enforces the "Execution-Pipeline-only" restriction [ToolInvocationBinding]
 * documents (mirroring [ToolRegistry.resolve]'s identical, already-existing
 * restriction) by documentation only, exactly matching the precedent
 * [InMemoryToolRegistry] itself already sets -- no caller-identity check,
 * no reduced visibility. `SPRINT_1_VERTICAL_SLICE_PLAN.md`'s Unit 3 row
 * asks that this be "true by construction... not merely true by
 * convention." Enforcing that would mean introducing a caller-identity or
 * visibility mechanism that does not exist anywhere else in this
 * repository today (including on [ToolRegistry.resolve] itself, which this
 * type is deliberately built to match). That is a real gap between the
 * plan's stated acceptance criterion and this repository's established
 * pattern, reported separately alongside this change rather than resolved
 * unilaterally in either direction.
 */
class InMemoryToolInvocationBinding : ToolInvocationBinding {

    private val mutex = Mutex()

    /** (toolId, version) -> the invocable Tool bound to that identity. */
    private val bindings = mutableMapOf<Pair<String, String>, Tool>()

    override suspend fun bind(descriptor: ToolDescriptor, tool: Tool): Unit = mutex.withLock {
        require(tool.descriptor == descriptor) {
            "Cannot bind a Tool whose own descriptor (${tool.descriptor}) does not match " +
                "the descriptor it is being bound to ($descriptor)"
        }
        bindings[descriptor.toolId to descriptor.version] = tool
    }

    override suspend fun invocableFor(descriptor: ToolDescriptor): Tool? = mutex.withLock {
        bindings[descriptor.toolId to descriptor.version]
    }
}
