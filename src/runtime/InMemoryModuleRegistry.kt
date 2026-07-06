package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.ModuleLifecycleTransitions
import parker.core.interfaces.ModuleRegistry
import parker.core.interfaces.ModuleStatus
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceRegistry
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolLifecycleState
import parker.core.interfaces.ToolRegistrationOutcome
import parker.core.interfaces.ToolRegistry

/**
 * In-memory implementation of [ModuleRegistry], per
 * `docs/architecture/MODULE_CONTRACT_DESIGN.md`. Sprint 6, Track A, Unit
 * M1. Implements exactly what Contract Design specifies for the Module
 * Registry itself:
 *
 * - Registration: unique `moduleId`, each declared `toolsExposed` entry
 *   registered with [ToolRegistry] (Contract Design Section 7).
 * - Lifecycle: [ModuleLifecycleTransitions] is the sole enforcement point
 *   for status changes (Contract Design Section 4).
 * - Lookup: descriptor, status, and full listing (Contract Design Section
 *   5 -- part of the registry's own core interface, not an
 *   implementation-specific inspection method).
 *
 * NOT implemented, per this Unit's own instructions and Contract Design's
 * own explicitly-deferred items (recorded in
 * `docs/architecture/IMPLEMENTATION_GAPS.md` #52, not invented around):
 * live `PermissionEngine` gating of `enable`/`disable`/`remove` (mirrors
 * `InMemoryToolRegistry`'s identical, pre-existing gap #24 scope
 * reduction); registering the module itself as an `IdentityService`
 * `Principal`; module discovery, dependency injection, or loading; and any
 * plugin, Home Assistant, Weather, or Gmail integration.
 *
 * **Interpretive decisions disclosed here, not left implicit** (see gap
 * #52 for the full reasoning):
 * - Each Tool a module exposes needs a backing `Resource`
 *   (`tool-registry.md`'s own registration invariant, gap #22's
 *   precedent). This implementation mints a deterministic
 *   `ResourceId("module-tool-<moduleId>-<toolId>")`, owned by
 *   `PrincipalId(moduleId.value)` -- the module is treated as the nominal
 *   owner of its own exposed Tools' Resources, without that module being a
 *   verified, `IdentityService`-registered Principal. Every such Resource
 *   defaults to [ResourceSensitivity.PUBLIC], since neither
 *   `ModuleDescriptor` nor `ToolDescriptor` carries a sensitivity
 *   classification.
 * - Module registration is **not atomic** across multiple declared Tools:
 *   if the Nth tool fails to register with [ToolRegistry], the first
 *   `N-1` tools remain registered there (and in [ResourceRegistry]) with
 *   no corresponding Module Registry entry, since [ToolRegistry] has no
 *   legal `REGISTERED -> REMOVED` edge to undo a freshly-registered,
 *   never-enabled Tool.
 * - [ToolRegistry] exposes no way to read a specific `toolId`+`version`'s
 *   current [ToolLifecycleState] from outside. This implementation tracks
 *   each of its own exposed Tools' state locally, mirroring every
 *   transition it itself drives. If a different module later supersedes
 *   the same `toolId` (a cross-module version collision), this module's
 *   locally-tracked state becomes stale relative to [ToolRegistry]'s real
 *   state, and a subsequent `enable`/`disable`/`remove` call could throw
 *   unexpectedly. This is a disclosed limitation, not solved by this Unit.
 */
class InMemoryModuleRegistry(
    private val toolRegistry: ToolRegistry,
    private val resourceRegistry: ResourceRegistry,
) : ModuleRegistry {

    private data class ToolEntry(val descriptor: ToolDescriptor, var state: ToolLifecycleState)

    private data class ModuleEntry(
        val descriptor: ModuleDescriptor,
        var status: ModuleStatus,
        val tools: MutableList<ToolEntry>,
    )

    private val mutex = Mutex()
    private val modules = mutableMapOf<ModuleId, ModuleEntry>()

    override suspend fun register(descriptor: ModuleDescriptor): ModuleId = mutex.withLock {
        require(descriptor.moduleId !in modules) {
            "Module '${descriptor.moduleId.value}' is already registered"
        }

        val toolEntries = mutableListOf<ToolEntry>()
        for (tool in descriptor.toolsExposed) {
            val resourceId = ResourceId("module-tool-${descriptor.moduleId.value}-${tool.toolId}")
            resourceRegistry.register(moduleToolResource(descriptor.moduleId, tool, resourceId))

            val outcome = toolRegistry.register(tool, resourceId)
            val state = when (outcome) {
                is ToolRegistrationOutcome.Registered -> ToolLifecycleState.REGISTERED
                is ToolRegistrationOutcome.Superseded -> ToolLifecycleState.ENABLED
                is ToolRegistrationOutcome.AlreadyRegistered -> throw IllegalStateException(
                    "Module '${descriptor.moduleId.value}' cannot register Tool '${tool.toolId}' " +
                        "version '${tool.version}': an identical toolId+version is already registered " +
                        "with Tool Registry, and Module Registry cannot safely determine that Tool's " +
                        "actual current lifecycle state from Tool Registry's read surface -- see " +
                        "IMPLEMENTATION_GAPS.md #52",
                )
                is ToolRegistrationOutcome.Rejected -> throw IllegalStateException(
                    "Module '${descriptor.moduleId.value}' cannot register Tool '${tool.toolId}' " +
                        "version '${tool.version}': ${outcome.reason}",
                )
            }
            toolEntries.add(ToolEntry(tool, state))
        }

        modules[descriptor.moduleId] = ModuleEntry(descriptor, ModuleStatus.REGISTERED, toolEntries)
        descriptor.moduleId
    }

    override suspend fun enable(moduleId: ModuleId, requestingPrincipalId: PrincipalId): ModuleStatus = mutex.withLock {
        val entry = requireEntry(moduleId)
        ModuleLifecycleTransitions.requireValidTransition(entry.status, ModuleStatus.ENABLED)

        entry.tools.forEach { toolEntry ->
            if (toolEntry.state != ToolLifecycleState.ENABLED) {
                toolRegistry.setLifecycleState(toolEntry.descriptor.toolId, toolEntry.descriptor.version, ToolLifecycleState.ENABLED)
                toolEntry.state = ToolLifecycleState.ENABLED
            }
        }
        entry.status = ModuleStatus.ENABLED
        entry.status
    }

    override suspend fun disable(moduleId: ModuleId, requestingPrincipalId: PrincipalId): ModuleStatus = mutex.withLock {
        val entry = requireEntry(moduleId)
        ModuleLifecycleTransitions.requireValidTransition(entry.status, ModuleStatus.DISABLED)

        entry.tools.forEach { toolEntry ->
            if (toolEntry.state == ToolLifecycleState.ENABLED) {
                toolRegistry.setLifecycleState(toolEntry.descriptor.toolId, toolEntry.descriptor.version, ToolLifecycleState.DISABLED)
                toolEntry.state = ToolLifecycleState.DISABLED
            }
        }
        entry.status = ModuleStatus.DISABLED
        entry.status
    }

    override suspend fun remove(moduleId: ModuleId, requestingPrincipalId: PrincipalId): ModuleStatus = mutex.withLock {
        val entry = requireEntry(moduleId)
        ModuleLifecycleTransitions.requireValidTransition(entry.status, ModuleStatus.REMOVED)

        entry.tools.forEach { toolEntry -> driveToolToRemoved(toolEntry) }
        entry.status = ModuleStatus.REMOVED
        entry.status
    }

    override suspend fun getModuleDescriptor(moduleId: ModuleId): ModuleDescriptor? = mutex.withLock {
        modules[moduleId]?.descriptor
    }

    override suspend fun getModuleStatus(moduleId: ModuleId): ModuleStatus? = mutex.withLock {
        modules[moduleId]?.status
    }

    override suspend fun listModules(): List<ModuleId> = mutex.withLock {
        modules.keys.toList()
    }

    private fun requireEntry(moduleId: ModuleId): ModuleEntry =
        modules[moduleId] ?: throw NoSuchElementException("No registered Module for moduleId '${moduleId.value}'")

    /**
     * Drives [toolEntry] from its currently-tracked [ToolLifecycleState] to
     * `REMOVED` via the shortest legal path in `ToolLifecycleTransitions`.
     * A module can only reach `remove()` from `REGISTERED` or `DISABLED`
     * (per [ModuleLifecycleTransitions]), so [toolEntry] is only ever
     * tracked at `REGISTERED`, `ENABLED`, or `DISABLED` when this runs --
     * `DEPRECATED` is included defensively for the cross-module
     * version-collision case disclosed in this class's own KDoc.
     */
    private suspend fun driveToolToRemoved(toolEntry: ToolEntry) {
        val path: List<ToolLifecycleState> = when (toolEntry.state) {
            ToolLifecycleState.REGISTERED -> listOf(
                ToolLifecycleState.ENABLED,
                ToolLifecycleState.DISABLED,
                ToolLifecycleState.REMOVED,
            )
            ToolLifecycleState.ENABLED -> listOf(ToolLifecycleState.DISABLED, ToolLifecycleState.REMOVED)
            ToolLifecycleState.DISABLED -> listOf(ToolLifecycleState.REMOVED)
            ToolLifecycleState.DEPRECATED -> listOf(ToolLifecycleState.REMOVED)
            ToolLifecycleState.REMOVED -> emptyList()
        }
        for (next in path) {
            toolRegistry.setLifecycleState(toolEntry.descriptor.toolId, toolEntry.descriptor.version, next)
            toolEntry.state = next
        }
    }

    private fun moduleToolResource(moduleId: ModuleId, tool: ToolDescriptor, resourceId: ResourceId): Resource {
        val now = Instant.now()
        return Resource(
            resourceId = resourceId,
            resourceType = ResourceType.TOOL,
            displayName = tool.displayName,
            ownerPrincipalId = PrincipalId(moduleId.value),
            sensitivity = ResourceSensitivity.PUBLIC,
            lifecycleState = ResourceLifecycleState.REGISTERED,
            createdAt = now,
            updatedAt = now,
            source = "module:${moduleId.value}",
        )
    }
}
