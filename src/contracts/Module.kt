package parker.core.interfaces

/**
 * Module Framework contracts (Sprint 6, Unit M1), implementing exactly the
 * shapes approved by `docs/architecture/MODULE_CONTRACT_DESIGN.md`
 * ("Module Contract Design"), itself built on
 * `docs/architecture/MODULE_FRAMEWORK_ARCHITECTURE.md`. No concept here is
 * new: every type below is cited, section by section, in Contract
 * Design's own Section 12 (Self-Traceability Review). This file does not
 * redesign, extend, or reinterpret that document -- it is its literal
 * Kotlin realisation.
 */

/**
 * Contract Design Section 1: a stable, caller-declared identity (declared
 * in a module's own [ModuleDescriptor], validated for uniqueness by
 * [ModuleRegistry] at Registration -- never minted by Parker itself),
 * matching [PrincipalId]/[ResourceId]/[MemoryId]'s identical established
 * shape.
 */
@JvmInline
value class ModuleId(val value: String) {
    init {
        require(value.isNotBlank()) { "ModuleId must not be blank" }
    }
}

/**
 * Contract Design Section 9: a module's declared network-dependency
 * posture. `LOCAL_ONLY` makes no network dependency claim; `CLOUD_CAPABLE`
 * may use one but can operate, at reduced capability, without it;
 * `CLOUD_REQUIRED` cannot function at all without one. Declaring
 * `CLOUD_CAPABLE`/`CLOUD_REQUIRED` is not itself an authorisation -- see
 * [ModuleRegistry.enable].
 */
enum class ModuleConnectivityDeclaration {
    LOCAL_ONLY,
    CLOUD_CAPABLE,
    CLOUD_REQUIRED,
}

/**
 * Contract Design Section 6: one permission a module's capabilities will
 * need evaluated at invocation time. Declaring one grants nothing -- it
 * only informs whatever Principal decides to [ModuleRegistry.enable] the
 * module what that decision is agreeing to expose the possibility of.
 * Every actual invocation is still independently evaluated by
 * `PermissionEngine.evaluate`, exactly as any other `ExecutionRequest` is.
 */
data class ModulePermissionRequirement(
    val action: PermissionAction,
    val resourceType: ResourceType,
)

/**
 * Contract Design Section 2: the manifest, field-shaped. A
 * [ModuleDescriptor] is a declaration, read by [ModuleRegistry] at
 * Registration time -- it never itself performs an action, grants a
 * permission, or activates an event subscription.
 *
 * @param toolsExposed The module's declared Tools, reusing the existing
 *   [ToolDescriptor] type directly. Per Contract Design Section 3, this
 *   single field also satisfies "provided capabilities" -- no separate
 *   `capabilities` field exists, since every capability a module offers is
 *   discoverable precisely because it is registered as a [ToolDescriptor].
 * @param requiredPermissions The module's declared [ModulePermissionRequirement]s.
 * @param eventSubscriptions Declared, not activated (Contract Design
 *   Section 8): a module may state its intended subscriptions from the
 *   first version of this contract, but a live `EventBus.subscribe` call
 *   on its behalf is deferred until ADR-024's gap #50 precondition
 *   (per-subscriber delivery isolation) is satisfied. Defaults to empty.
 * @param minimumPlatformVersion The one, minimal piece of "compatibility
 *   requirements" this contract commits to (Contract Design Section 2) --
 *   a richer dependency or feature-flag model is explicitly not designed
 *   here. Optional.
 */
data class ModuleDescriptor(
    val moduleId: ModuleId,
    val name: String,
    val version: String,
    val toolsExposed: List<ToolDescriptor>,
    val requiredPermissions: List<ModulePermissionRequirement>,
    val connectivityDeclaration: ModuleConnectivityDeclaration,
    val eventSubscriptions: List<EventType> = emptyList(),
    val minimumPlatformVersion: String? = null,
) {
    init {
        require(name.isNotBlank()) { "ModuleDescriptor.name must not be blank" }
        require(version.isNotBlank()) { "ModuleDescriptor.version must not be blank" }
    }
}

/**
 * Contract Design Section 4: the four states [ModuleRegistry] itself
 * actually assigns, stores, and transitions between. `Discovered`,
 * `Described`, and `Invoked` -- three of `MODULE_FRAMEWORK_ARCHITECTURE.md`
 * Section 4's seven conceptual steps -- are deliberately not tracked enum
 * values here: Discovered/Described are pre-registry concepts
 * `ModuleRegistry` never observes (dynamic discovery is out of scope), and
 * Invoked is a transient action performed while `ENABLED`, not a status a
 * module persists in.
 */
enum class ModuleStatus {
    REGISTERED,
    ENABLED,
    DISABLED,
    REMOVED,
}

/**
 * Transition validator for [ModuleStatus], following the same pattern as
 * [PrincipalLifecycleTransitions] and [ToolLifecycleTransitions]: a fixed
 * adjacency map, `isValidTransition`, and `requireValidTransition`.
 *
 * Edges match Contract Design Section 4 exactly:
 *
 *   REGISTERED -> ENABLED, REMOVED
 *   ENABLED    -> DISABLED
 *   DISABLED   -> ENABLED, REMOVED
 *   REMOVED    -> (terminal)
 *
 * `DISABLED -> ENABLED` (re-enabling) is a legal edge: Disable exists
 * precisely to be a reversible, non-destructive step distinct from
 * Remove. `REMOVED` has no outgoing edge -- re-introducing a removed
 * module requires a new Registration, per
 * `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 4's "must be re-discovered,
 * re-described, and re-registered."
 */
object ModuleLifecycleTransitions {

    private val allowed: Map<ModuleStatus, Set<ModuleStatus>> = mapOf(
        ModuleStatus.REGISTERED to setOf(ModuleStatus.ENABLED, ModuleStatus.REMOVED),
        ModuleStatus.ENABLED to setOf(ModuleStatus.DISABLED),
        ModuleStatus.DISABLED to setOf(ModuleStatus.ENABLED, ModuleStatus.REMOVED),
        ModuleStatus.REMOVED to emptySet(),
    )

    fun isTerminal(status: ModuleStatus): Boolean = allowed.getValue(status).isEmpty()

    fun isValidTransition(from: ModuleStatus, to: ModuleStatus): Boolean =
        to in allowed.getValue(from)

    /** Throws [IllegalArgumentException] if `from -> to` is not a permitted edge. */
    fun requireValidTransition(from: ModuleStatus, to: ModuleStatus) {
        require(isValidTransition(from, to)) {
            "Illegal Module lifecycle transition: $from -> $to"
        }
    }
}
