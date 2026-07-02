package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceRegistry
import parker.core.interfaces.ResourceType
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolLifecycleState
import parker.core.interfaces.ToolLifecycleTransitions
import parker.core.interfaces.ToolRegistrationOutcome
import parker.core.interfaces.ToolRegistry
import parker.core.interfaces.ToolResolution
import parker.core.interfaces.ToolResolutionFailureReason

/**
 * In-memory implementation of [ToolRegistry], per
 * `docs/architecture/tool-registry.md`. Implements exactly what that
 * document specifies:
 *
 * - Registration Model (idempotent re-registration, version supersession,
 *   rejection of a resourceId that doesn't resolve to a `TOOL`-type
 *   Resource, rejection of a changed descriptor for an unchanged
 *   toolId+version pair).
 * - Version Handling (single active version per toolId; new version of
 *   an already-Enabled toolId supersedes it -- old goes Deprecated, new
 *   goes straight to Enabled; a version with no prior Enabled sibling
 *   starts at Registered like any first registration).
 * - Lookup Process (capability-matching resolve, Enabled-only, typed
 *   zero/one/many-candidate failures).
 * - Runtime Lifecycle ([ToolLifecycleTransitions] enforced on every state
 *   change).
 *
 * NOT implemented (see the interface doc and IMPLEMENTATION_GAPS.md):
 * Principal-scoped discovery visibility, and live Permission Engine
 * gating of registration/lifecycle changes -- both require components
 * (IdentityService, a policy-bearing PermissionEngine) that don't exist
 * as concrete implementations yet.
 */
class InMemoryToolRegistry(
    private val resourceRegistry: ResourceRegistry,
) : ToolRegistry {

    private data class Entry(val descriptor: ToolDescriptor, val resourceId: ResourceId, var state: ToolLifecycleState)

    private val mutex = Mutex()

    /** toolId -> version -> Entry. */
    private val tools = mutableMapOf<String, MutableMap<String, Entry>>()

    override suspend fun register(descriptor: ToolDescriptor, resourceId: ResourceId): ToolRegistrationOutcome = mutex.withLock {
        val backingResource = resourceRegistry.resolve(resourceId)
            ?: return@withLock ToolRegistrationOutcome.Rejected(
                "resourceId '${resourceId.value}' does not resolve to a registered Resource",
            )
        if (backingResource.resourceType != ResourceType.TOOL) {
            return@withLock ToolRegistrationOutcome.Rejected(
                "resourceId '${resourceId.value}' resolves to a Resource of type " +
                    "${backingResource.resourceType}, not TOOL",
            )
        }

        val versions = tools.getOrPut(descriptor.toolId) { mutableMapOf() }
        val existing = versions[descriptor.version]

        if (existing != null) {
            return@withLock if (existing.descriptor == descriptor) {
                // Same toolId + version + descriptor: idempotent no-op.
                ToolRegistrationOutcome.AlreadyRegistered(descriptor.toolId, descriptor.version)
            } else {
                // Same toolId + version, different descriptor: inconsistent data, not supersession.
                ToolRegistrationOutcome.Rejected(
                    "toolId '${descriptor.toolId}' version '${descriptor.version}' is already registered " +
                        "with a different descriptor; re-register under a new version instead",
                )
            }
        }

        val currentlyEnabled = versions.values.singleOrNull { it.state == ToolLifecycleState.ENABLED }

        return@withLock if (currentlyEnabled != null) {
            // Version supersession: prior Enabled version -> Deprecated; new version starts Enabled directly.
            ToolLifecycleTransitions.requireValidTransition(ToolLifecycleState.ENABLED, ToolLifecycleState.DEPRECATED)
            currentlyEnabled.state = ToolLifecycleState.DEPRECATED
            versions[descriptor.version] = Entry(descriptor, resourceId, ToolLifecycleState.ENABLED)
            ToolRegistrationOutcome.Superseded(descriptor.toolId, currentlyEnabled.descriptor.version, descriptor.version)
        } else {
            // First registration of this toolId, or a new version with no currently-Enabled sibling.
            versions[descriptor.version] = Entry(descriptor, resourceId, ToolLifecycleState.REGISTERED)
            ToolRegistrationOutcome.Registered(descriptor.toolId, descriptor.version, ToolLifecycleState.REGISTERED)
        }
    }

    override suspend fun resolve(action: PermissionAction, resourceTypes: Set<ResourceType>): ToolResolution = mutex.withLock {
        val allEntries = tools.values.flatMap { it.values }
        val capabilityMatches = allEntries.filter { entry ->
            action in entry.descriptor.supportedActions &&
                entry.descriptor.supportedResourceTypes.intersect(resourceTypes).isNotEmpty()
        }

        val enabledMatches = capabilityMatches.filter { it.state == ToolLifecycleState.ENABLED }

        return@withLock when {
            enabledMatches.size == 1 -> ToolResolution.Resolved(enabledMatches.single().descriptor)
            enabledMatches.size > 1 -> ToolResolution.Failed(ToolResolutionFailureReason.TOOL_AMBIGUOUS)
            capabilityMatches.any { it.state != ToolLifecycleState.REMOVED } ->
                // A matching Tool exists (Registered/Disabled/Deprecated) but nothing Enabled.
                ToolResolution.Failed(ToolResolutionFailureReason.TOOL_DISABLED)
            else -> ToolResolution.Failed(ToolResolutionFailureReason.TOOL_NOT_FOUND)
        }
    }

    override suspend fun findCandidates(actions: Set<PermissionAction>, resourceTypes: Set<ResourceType>): List<ToolDescriptor> =
        mutex.withLock {
            tools.values.flatMap { it.values }
                .filter { it.state == ToolLifecycleState.ENABLED }
                .filter { entry ->
                    entry.descriptor.supportedActions.intersect(actions).isNotEmpty() &&
                        entry.descriptor.supportedResourceTypes.intersect(resourceTypes).isNotEmpty()
                }
                .map { it.descriptor }
        }

    override suspend fun listAll(): List<ToolDescriptor> = mutex.withLock {
        tools.values.flatMap { it.values }.map { it.descriptor }
    }

    override suspend fun setLifecycleState(toolId: String, version: String, newState: ToolLifecycleState): ToolDescriptor =
        mutex.withLock {
            val versions = tools[toolId] ?: throw NoSuchElementException("No Tool registered with toolId '$toolId'")
            val entry = versions[version]
                ?: throw NoSuchElementException("Tool '$toolId' has no registered version '$version'")
            ToolLifecycleTransitions.requireValidTransition(entry.state, newState)
            entry.state = newState
            entry.descriptor
        }
}
