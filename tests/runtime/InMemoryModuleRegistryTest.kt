package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ModuleConnectivityDeclaration
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.ModulePermissionRequirement
import parker.core.interfaces.ModuleStatus
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ResourceType
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolResolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Proves the behaviours `docs/architecture/MODULE_CONTRACT_DESIGN.md`
 * requires of the Module Registry: registration (incl. its Tool
 * Registry/Resource Registry wiring), duplicate `moduleId` rejection,
 * lifecycle enforcement, lookup, and the constitutional boundary that
 * enabling a module never itself grants a permission or bypasses
 * `ToolRegistry`/`PermissionEngine` as the sole invocation path.
 */
class InMemoryModuleRegistryTest {

    private fun tool(
        toolId: String = "tool.demo.read",
        version: String = "1.0.0",
        actions: Set<PermissionAction> = setOf(PermissionAction.READ),
        resourceTypes: Set<ResourceType> = setOf(ResourceType.DOCUMENT),
    ) = ToolDescriptor(
        toolId = toolId,
        displayName = "Demo Tool",
        description = "A tool exposed by a test module",
        version = version,
        supportedActions = actions,
        supportedResourceTypes = resourceTypes,
    )

    private fun descriptor(
        moduleId: String = "module.demo",
        toolsExposed: List<ToolDescriptor> = listOf(tool()),
        requiredPermissions: List<ModulePermissionRequirement> = listOf(
            ModulePermissionRequirement(PermissionAction.READ, ResourceType.DOCUMENT),
        ),
        connectivity: ModuleConnectivityDeclaration = ModuleConnectivityDeclaration.LOCAL_ONLY,
    ) = ModuleDescriptor(
        moduleId = ModuleId(moduleId),
        name = "Demo Module",
        version = "1.0.0",
        toolsExposed = toolsExposed,
        requiredPermissions = requiredPermissions,
        connectivityDeclaration = connectivity,
    )

    private fun registry(): InMemoryModuleRegistry {
        val resources = InMemoryResourceRegistry()
        val tools = InMemoryToolRegistry(resources)
        return InMemoryModuleRegistry(tools, resources)
    }

    private fun registryWithTools(): Pair<InMemoryModuleRegistry, InMemoryToolRegistry> {
        val resources = InMemoryResourceRegistry()
        val tools = InMemoryToolRegistry(resources)
        return InMemoryModuleRegistry(tools, resources) to tools
    }

    private val requester = PrincipalId("user.owner")

    // --- Registration ---

    @Test
    fun `register returns the ModuleId and records it at REGISTERED status`() = runTest {
        val registry = registry()
        val id = registry.register(descriptor())

        assertEquals(ModuleId("module.demo"), id)
        assertEquals(ModuleStatus.REGISTERED, registry.getModuleStatus(id))
        assertEquals(descriptor(), registry.getModuleDescriptor(id))
    }

    @Test
    fun `register rejects a duplicate moduleId`() = runTest {
        val registry = registry()
        registry.register(descriptor())

        assertFailsWith<IllegalArgumentException> {
            registry.register(descriptor())
        }
    }

    @Test
    fun `register succeeds for a module declaring no tools`() = runTest {
        val registry = registry()
        val id = registry.register(descriptor(moduleId = "module.no-tools", toolsExposed = emptyList()))

        assertEquals(ModuleStatus.REGISTERED, registry.getModuleStatus(id))
    }

    @Test
    fun `register wires each declared Tool into Tool Registry at REGISTERED state`() = runTest {
        val (registry, tools) = registryWithTools()
        registry.register(descriptor())

        assertEquals(1, tools.listAll().size)
        // Not yet enabled -- must not be resolvable via Tool Registry's own lookup.
        val result = tools.resolve(PermissionAction.READ, setOf(ResourceType.DOCUMENT))
        assertIs<ToolResolution.Failed>(result)
    }

    @Test
    fun `register rejects a Tool whose toolId+version already exists with a different descriptor`() = runTest {
        val (registry, _) = registryWithTools()
        registry.register(descriptor(moduleId = "module.one"))

        val conflicting = tool(actions = setOf(PermissionAction.WRITE))
        assertFailsWith<IllegalStateException> {
            registry.register(descriptor(moduleId = "module.two", toolsExposed = listOf(conflicting)))
        }
        // The first module's own registration is unaffected by the second's failure.
        assertEquals(ModuleStatus.REGISTERED, registry.getModuleStatus(ModuleId("module.one")))
    }

    @Test
    fun `register rejects an exact duplicate Tool registration (AlreadyRegistered) as a conflict`() = runTest {
        val (registry, _) = registryWithTools()
        registry.register(descriptor(moduleId = "module.one"))

        assertFailsWith<IllegalStateException> {
            registry.register(descriptor(moduleId = "module.two"))
        }
    }

    @Test
    fun `a failed multi-tool registration is not atomic -- earlier Tools remain registered with Tool Registry`() = runTest {
        val (registry, tools) = registryWithTools()
        val firstTool = tool(toolId = "tool.first")
        val conflictingSecondTool = tool(toolId = "tool.demo.read") // collides with the default fixture below
        registry.register(descriptor(moduleId = "module.other", toolsExposed = listOf(tool())))

        assertFailsWith<IllegalStateException> {
            registry.register(
                descriptor(moduleId = "module.multi", toolsExposed = listOf(firstTool, conflictingSecondTool)),
            )
        }

        // Disclosed, known limitation: "tool.first" was already registered with Tool Registry
        // before the second tool's conflict was discovered, and Module Registry has no way to
        // undo it (no legal REGISTERED -> REMOVED edge for a never-enabled Tool).
        assertTrue(tools.listAll().any { it.toolId == "tool.first" })
        // The module itself was never recorded, since registration overall failed.
        assertNull(registry.getModuleDescriptor(ModuleId("module.multi")))
        assertTrue(ModuleId("module.multi") !in registry.listModules())
    }

    // --- Lifecycle: enable ---

    @Test
    fun `enable transitions REGISTERED to ENABLED and makes Tools resolvable`() = runTest {
        val (registry, tools) = registryWithTools()
        val id = registry.register(descriptor())

        val status = registry.enable(id, requester)

        assertEquals(ModuleStatus.ENABLED, status)
        assertEquals(ModuleStatus.ENABLED, registry.getModuleStatus(id))
        val result = tools.resolve(PermissionAction.READ, setOf(ResourceType.DOCUMENT))
        assertIs<ToolResolution.Resolved>(result)
    }

    @Test
    fun `enable throws for an unregistered moduleId`() = runTest {
        val registry = registry()
        assertFailsWith<NoSuchElementException> {
            registry.enable(ModuleId("nonexistent"), requester)
        }
    }

    @Test
    fun `enabling an already-ENABLED module is an illegal transition`() = runTest {
        val registry = registry()
        val id = registry.register(descriptor())
        registry.enable(id, requester)

        assertFailsWith<IllegalArgumentException> {
            registry.enable(id, requester)
        }
    }

    @Test
    fun `enable does not validate requestingPrincipalId against any Identity or Permission check`() = runTest {
        // Disclosed scope reduction (IMPLEMENTATION_GAPS.md #52, mirroring gap #24 for Tool
        // Registry): this Unit does not wire live Permission Engine gating of lifecycle
        // operations, so an arbitrary, never-registered Principal is accepted as-is.
        val registry = registry()
        val id = registry.register(descriptor())

        val status = registry.enable(id, PrincipalId("nobody-in-particular"))
        assertEquals(ModuleStatus.ENABLED, status)
    }

    // --- Lifecycle: disable ---

    @Test
    fun `disable transitions ENABLED to DISABLED and makes Tools unresolvable again`() = runTest {
        val (registry, tools) = registryWithTools()
        val id = registry.register(descriptor())
        registry.enable(id, requester)

        val status = registry.disable(id, requester)

        assertEquals(ModuleStatus.DISABLED, status)
        val result = tools.resolve(PermissionAction.READ, setOf(ResourceType.DOCUMENT))
        assertIs<ToolResolution.Failed>(result)
    }

    @Test
    fun `disabling a never-enabled (REGISTERED) module is an illegal transition`() = runTest {
        val registry = registry()
        val id = registry.register(descriptor())

        assertFailsWith<IllegalArgumentException> {
            registry.disable(id, requester)
        }
    }

    @Test
    fun `a module can be re-enabled after being disabled`() = runTest {
        val (registry, tools) = registryWithTools()
        val id = registry.register(descriptor())
        registry.enable(id, requester)
        registry.disable(id, requester)

        val status = registry.enable(id, requester)

        assertEquals(ModuleStatus.ENABLED, status)
        val result = tools.resolve(PermissionAction.READ, setOf(ResourceType.DOCUMENT))
        assertIs<ToolResolution.Resolved>(result)
    }

    // --- Lifecycle: remove ---

    @Test
    fun `remove from REGISTERED drives its Tools all the way to REMOVED`() = runTest {
        val (registry, tools) = registryWithTools()
        val id = registry.register(descriptor())

        val status = registry.remove(id, requester)

        assertEquals(ModuleStatus.REMOVED, status)
        assertEquals(ModuleStatus.REMOVED, registry.getModuleStatus(id))
        val result = tools.resolve(PermissionAction.READ, setOf(ResourceType.DOCUMENT))
        assertIs<ToolResolution.Failed>(result)
    }

    @Test
    fun `remove from DISABLED drives its Tools to REMOVED`() = runTest {
        val (registry, tools) = registryWithTools()
        val id = registry.register(descriptor())
        registry.enable(id, requester)
        registry.disable(id, requester)

        val status = registry.remove(id, requester)

        assertEquals(ModuleStatus.REMOVED, status)
        assertIs<ToolResolution.Failed>(tools.resolve(PermissionAction.READ, setOf(ResourceType.DOCUMENT)))
    }

    @Test
    fun `remove cannot be called directly from ENABLED`() = runTest {
        val registry = registry()
        val id = registry.register(descriptor())
        registry.enable(id, requester)

        assertFailsWith<IllegalArgumentException> {
            registry.remove(id, requester)
        }
    }

    @Test
    fun `removing an already-REMOVED module is an illegal transition`() = runTest {
        val registry = registry()
        val id = registry.register(descriptor())
        registry.remove(id, requester)

        assertFailsWith<IllegalArgumentException> {
            registry.remove(id, requester)
        }
    }

    @Test
    fun `remove throws for an unregistered moduleId`() = runTest {
        val registry = registry()
        assertFailsWith<NoSuchElementException> {
            registry.remove(ModuleId("nonexistent"), requester)
        }
    }

    // --- Lookup ---

    @Test
    fun `lookup returns null descriptor and status for an unregistered moduleId`() = runTest {
        val registry = registry()
        assertNull(registry.getModuleDescriptor(ModuleId("nonexistent")))
        assertNull(registry.getModuleStatus(ModuleId("nonexistent")))
    }

    @Test
    fun `listModules includes every registered module regardless of status, including REMOVED`() = runTest {
        val registry = registry()
        val a = registry.register(descriptor(moduleId = "module.a"))
        val b = registry.register(descriptor(moduleId = "module.b", toolsExposed = emptyList()))
        registry.remove(a, requester)

        val ids = registry.listModules()
        assertEquals(setOf(a, b), ids.toSet())
        assertEquals(ModuleStatus.REMOVED, registry.getModuleStatus(a))
    }

    // --- Constitutional boundaries ---

    @Test
    fun `a module's declared requiredPermissions are stored unchanged -- enabling never grants or mutates them`() = runTest {
        val registry = registry()
        val requirement = ModulePermissionRequirement(PermissionAction.READ, ResourceType.DOCUMENT)
        val id = registry.register(descriptor(requiredPermissions = listOf(requirement)))

        registry.enable(id, requester)

        assertEquals(listOf(requirement), registry.getModuleDescriptor(id)?.requiredPermissions)
    }

    @Test
    fun `enabling a module makes its Tools reachable only through Tool Registry, never through Module Registry itself`() = runTest {
        // Module Registry exposes no execute/invoke operation at all (ModuleRegistry interface
        // has register/enable/disable/remove/lookup only) -- this test proves the practical
        // consequence: the only way to observe a module's Tool becoming usable is via
        // ToolRegistry.resolve(), never via any InMemoryModuleRegistry method.
        val (registry, tools) = registryWithTools()
        val id = registry.register(descriptor())

        assertIs<ToolResolution.Failed>(tools.resolve(PermissionAction.READ, setOf(ResourceType.DOCUMENT)))
        registry.enable(id, requester)
        assertIs<ToolResolution.Resolved>(tools.resolve(PermissionAction.READ, setOf(ResourceType.DOCUMENT)))
    }
}
