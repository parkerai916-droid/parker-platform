package parker.core.runtime

import parker.core.interfaces.PermissionAction
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceType
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolLifecycleState
import parker.core.interfaces.ToolRegistrationOutcome
import parker.core.interfaces.ToolRegistry
import parker.core.interfaces.ToolResolution

/**
 * Test-only fake, mirroring [FakeIdentityService]'s lambda-based fake
 * precedent (Sprint 11, Unit 3). Exists so
 * [DefaultReasoningContextAssemblerTest] can prove
 * [DefaultReasoningContextAssembler]'s own read-only-only dependency
 * discipline (`PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section
 * 4.1: "the Assembler never calls `register`, `setLifecycleState`, or
 * `resolve`") independently of any real [ToolRegistry] implementation.
 *
 * Only [listAll] is exercised by [DefaultReasoningContextAssembler];
 * [register]/[resolve]/[findCandidates]/[setLifecycleState] are never
 * called by it, so they throw if reached -- a structural guard against
 * this fake silently masking an unexpected dependency on a method the
 * Assembler must not call.
 */
class FakeToolRegistry(
    private val allTools: () -> List<ToolDescriptor>,
) : ToolRegistry {

    var listAllCallCount: Int = 0
        private set

    override suspend fun listAll(): List<ToolDescriptor> {
        listAllCallCount++
        return allTools()
    }

    override suspend fun register(descriptor: ToolDescriptor, resourceId: ResourceId): ToolRegistrationOutcome {
        throw UnsupportedOperationException("FakeToolRegistry.register must not be called by DefaultReasoningContextAssembler")
    }

    override suspend fun resolve(action: PermissionAction, resourceTypes: Set<ResourceType>): ToolResolution {
        throw UnsupportedOperationException("FakeToolRegistry.resolve must not be called by DefaultReasoningContextAssembler")
    }

    override suspend fun findCandidates(actions: Set<PermissionAction>, resourceTypes: Set<ResourceType>): List<ToolDescriptor> {
        throw UnsupportedOperationException("FakeToolRegistry.findCandidates must not be called by DefaultReasoningContextAssembler")
    }

    override suspend fun setLifecycleState(toolId: String, version: String, newState: ToolLifecycleState): ToolDescriptor {
        throw UnsupportedOperationException("FakeToolRegistry.setLifecycleState must not be called by DefaultReasoningContextAssembler")
    }
}
