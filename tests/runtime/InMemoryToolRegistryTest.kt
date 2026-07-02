package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceType
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolLifecycleState
import parker.core.interfaces.ToolRegistrationOutcome
import parker.core.interfaces.ToolResolution
import parker.core.interfaces.ToolResolutionFailureReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Proves the behaviours docs/architecture/tool-registry.md requires:
 * registration (incl. its Resource Registry invariant), duplicate/version
 * handling, deterministic capability-based lookup, and disabled/
 * unavailable tool handling.
 */
class InMemoryToolRegistryTest {

    private fun toolResource(id: String) = Resource(
        resourceId = ResourceId(id),
        resourceType = ResourceType.TOOL,
        displayName = "Tool Resource $id",
        ownerPrincipalId = PrincipalId("system"),
        sensitivity = "PUBLIC",
        lifecycleState = ResourceLifecycleState.AVAILABLE,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        source = "test",
    )

    private fun nonToolResource(id: String) = toolResource(id).copy(resourceType = ResourceType.DOCUMENT)

    private fun descriptor(
        toolId: String = "tool.calendar.read",
        version: String = "1.0.0",
        actions: Set<PermissionAction> = setOf(PermissionAction.READ),
        resourceTypes: Set<ResourceType> = setOf(ResourceType.CALENDAR),
    ) = ToolDescriptor(
        toolId = toolId,
        displayName = "Calendar Reader",
        description = "Reads calendar entries",
        version = version,
        supportedActions = actions,
        supportedResourceTypes = resourceTypes,
    )

    private suspend fun registryWithResource(resourceId: String = "res.tool.1"): Pair<InMemoryToolRegistry, ResourceId> {
        val resources = InMemoryResourceRegistry()
        val id = resources.register(toolResource(resourceId))
        return InMemoryToolRegistry(resources) to id
    }

    // --- Registration ---

    @Test
    fun `registration fails if resourceId does not resolve to a Resource`() = runTest {
        val registry = InMemoryToolRegistry(InMemoryResourceRegistry())
        val outcome = registry.register(descriptor(), ResourceId("nonexistent"))
        assertIs<ToolRegistrationOutcome.Rejected>(outcome)
    }

    @Test
    fun `registration fails if resourceId resolves to a non-TOOL Resource`() = runTest {
        val resources = InMemoryResourceRegistry()
        val id = resources.register(nonToolResource("res.doc.1"))
        val registry = InMemoryToolRegistry(resources)

        val outcome = registry.register(descriptor(), id)
        assertIs<ToolRegistrationOutcome.Rejected>(outcome)
    }

    @Test
    fun `first registration of a toolId starts at REGISTERED, not ENABLED`() = runTest {
        val (registry, resourceId) = registryWithResource()
        val outcome = registry.register(descriptor(), resourceId)

        assertIs<ToolRegistrationOutcome.Registered>(outcome)
        assertEquals(ToolLifecycleState.REGISTERED, (outcome as ToolRegistrationOutcome.Registered).state)
    }

    // --- Duplicate handling ---

    @Test
    fun `re-registering an identical descriptor and version is an idempotent no-op`() = runTest {
        val (registry, resourceId) = registryWithResource()
        registry.register(descriptor(), resourceId)
        val second = registry.register(descriptor(), resourceId)

        assertIs<ToolRegistrationOutcome.AlreadyRegistered>(second)
    }

    @Test
    fun `re-registering the same toolId and version with a different descriptor is rejected`() = runTest {
        val (registry, resourceId) = registryWithResource()
        registry.register(descriptor(), resourceId)
        val conflicting = registry.register(descriptor(actions = setOf(PermissionAction.WRITE)), resourceId)

        assertIs<ToolRegistrationOutcome.Rejected>(conflicting)
    }

    // --- Version handling ---

    @Test
    fun `registering a new version of an Enabled tool supersedes it -- old goes Deprecated, new goes Enabled`() = runTest {
        val (registry, resourceId) = registryWithResource()
        registry.register(descriptor(version = "1.0.0"), resourceId)
        registry.setLifecycleState("tool.calendar.read", "1.0.0", ToolLifecycleState.ENABLED)

        val outcome = registry.register(descriptor(version = "2.0.0"), resourceId)
        assertIs<ToolRegistrationOutcome.Superseded>(outcome)
        outcome as ToolRegistrationOutcome.Superseded
        assertEquals("1.0.0", outcome.previousVersion)
        assertEquals("2.0.0", outcome.newVersion)

        // Old version is now Deprecated -- attempting to re-enable it directly should fail (Deprecated -> Removed only).
        assertFailsWith<IllegalArgumentException> {
            registry.setLifecycleState("tool.calendar.read", "1.0.0", ToolLifecycleState.ENABLED)
        }
    }

    // --- Deterministic lookup ---

    @Test
    fun `resolve finds the single Enabled tool matching action and resource type`() = runTest {
        val (registry, resourceId) = registryWithResource()
        registry.register(descriptor(), resourceId)
        registry.setLifecycleState("tool.calendar.read", "1.0.0", ToolLifecycleState.ENABLED)

        val result = registry.resolve(PermissionAction.READ, setOf(ResourceType.CALENDAR))
        assertIs<ToolResolution.Resolved>(result)
        assertEquals("tool.calendar.read", (result as ToolResolution.Resolved).descriptor.toolId)
    }

    @Test
    fun `resolve reports TOOL_NOT_FOUND when nothing matches`() = runTest {
        val (registry, _) = registryWithResource()
        val result = registry.resolve(PermissionAction.DELETE, setOf(ResourceType.SECRET))
        assertEquals(ToolResolution.Failed(ToolResolutionFailureReason.TOOL_NOT_FOUND), result)
    }

    @Test
    fun `resolve reports TOOL_DISABLED when a matching tool exists but is not Enabled`() = runTest {
        val (registry, resourceId) = registryWithResource()
        registry.register(descriptor(), resourceId) // stays REGISTERED, never enabled

        val result = registry.resolve(PermissionAction.READ, setOf(ResourceType.CALENDAR))
        assertEquals(ToolResolution.Failed(ToolResolutionFailureReason.TOOL_DISABLED), result)
    }

    @Test
    fun `resolve reports TOOL_AMBIGUOUS when two Enabled tools match the same capability`() = runTest {
        val resources = InMemoryResourceRegistry()
        val id1 = resources.register(toolResource("res.1"))
        val id2 = resources.register(toolResource("res.2"))
        val registry = InMemoryToolRegistry(resources)

        registry.register(descriptor(toolId = "tool.a"), id1)
        registry.setLifecycleState("tool.a", "1.0.0", ToolLifecycleState.ENABLED)
        registry.register(descriptor(toolId = "tool.b"), id2)
        registry.setLifecycleState("tool.b", "1.0.0", ToolLifecycleState.ENABLED)

        val result = registry.resolve(PermissionAction.READ, setOf(ResourceType.CALENDAR))
        assertEquals(ToolResolution.Failed(ToolResolutionFailureReason.TOOL_AMBIGUOUS), result)
    }

    @Test
    fun `findCandidates returns only Enabled capability-matching descriptors`() = runTest {
        val (registry, resourceId) = registryWithResource()
        registry.register(descriptor(), resourceId)
        // Not yet enabled -> should not appear as a candidate.
        assertEquals(0, registry.findCandidates(setOf(PermissionAction.READ), setOf(ResourceType.CALENDAR)).size)

        registry.setLifecycleState("tool.calendar.read", "1.0.0", ToolLifecycleState.ENABLED)
        assertEquals(1, registry.findCandidates(setOf(PermissionAction.READ), setOf(ResourceType.CALENDAR)).size)
    }

    @Test
    fun `listAll includes every registered version regardless of lifecycle state`() = runTest {
        val (registry, resourceId) = registryWithResource()
        registry.register(descriptor(version = "1.0.0"), resourceId)
        registry.setLifecycleState("tool.calendar.read", "1.0.0", ToolLifecycleState.ENABLED)
        registry.register(descriptor(version = "2.0.0"), resourceId)

        assertEquals(2, registry.listAll().size)
    }

    // --- Lifecycle transitions ---

    @Test
    fun `setLifecycleState rejects an invalid transition`() = runTest {
        val (registry, resourceId) = registryWithResource()
        registry.register(descriptor(), resourceId)

        assertFailsWith<IllegalArgumentException> {
            registry.setLifecycleState("tool.calendar.read", "1.0.0", ToolLifecycleState.REMOVED)
        }
    }

    @Test
    fun `setLifecycleState throws for an unknown tool`() = runTest {
        val registry = InMemoryToolRegistry(InMemoryResourceRegistry())
        assertFailsWith<NoSuchElementException> {
            registry.setLifecycleState("nonexistent", "1.0.0", ToolLifecycleState.ENABLED)
        }
    }
}
