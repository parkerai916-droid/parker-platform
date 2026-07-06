package parker.core.interfaces

/**
 * Module Registry (`docs/architecture/MODULE_CONTRACT_DESIGN.md` Section
 * 5). The single public interface for the Module Framework -- no separate
 * `ModuleRuntime` exists, mirroring `MemoryRuntime`'s identical exclusion
 * from Memory's own contract set (Contract Design Section 5, Section 11).
 *
 * Scope boundary, restated from Contract Design: [ModuleRegistry] manages
 * lifecycle and lookup only. It never invokes a module's capability
 * itself -- reaching a module's Tool is `ExecutionPipeline`/`ToolRegistry`'s
 * job (Contract Design Section 7). This interface is not a second
 * execution path.
 *
 * Error handling mirrors this codebase's two closest precedents exactly,
 * per Contract Design's own citation (Section 1): [register] throws on a
 * duplicate [ModuleId], the same way [IdentityService.register] throws on
 * a duplicate [PrincipalId]; [enable]/[disable]/[remove] throw
 * [NoSuchElementException] for an unknown [ModuleId] and
 * [IllegalArgumentException] for an illegal [ModuleLifecycleTransitions]
 * edge, the same way [IdentityService.updateStatus] and
 * [ToolRegistry.setLifecycleState] already do. No new sealed
 * Accepted/Rejected outcome type is introduced -- Contract Design
 * deliberately left this exact Kotlin shape undecided, and this is the
 * minimal, precedent-consistent choice.
 */
interface ModuleRegistry {

    /**
     * Registers [descriptor], returning its [ModuleId]. Fails
     * deterministically (throws [IllegalArgumentException]) on a
     * duplicate `moduleId`, mirroring [IdentityService.register]'s
     * existing "already registered" precedent (Contract Design Section 1).
     *
     * Internally, this is the point at which each of [descriptor]'s
     * declared `toolsExposed` entries is registered with `ToolRegistry`
     * (Contract Design Section 7) -- [ModuleRegistry] is a caller of
     * `ToolRegistry`'s own existing registration operation, not a
     * replacement for it.
     */
    suspend fun register(descriptor: ModuleDescriptor): ModuleId

    /**
     * Transitions a `REGISTERED` or `DISABLED` module to `ENABLED`,
     * making its registered Tools reachable through `ToolRegistry`'s own
     * discovery surface. Does not itself evaluate or pre-approve any
     * individual future invocation of those Tools (Contract Design
     * Section 6) -- every invocation remains independently evaluated by
     * `PermissionEngine.evaluate`.
     *
     * [requestingPrincipalId] identifies the Principal making this
     * attributable decision (Contract Design Section 5; never the
     * module's own identity -- a module never self-enables).
     *
     * Throws [NoSuchElementException] if [moduleId] is not registered, and
     * [IllegalArgumentException] if the module's current status cannot
     * legally transition to `ENABLED` (see [ModuleLifecycleTransitions]).
     */
    suspend fun enable(moduleId: ModuleId, requestingPrincipalId: PrincipalId): ModuleStatus

    /**
     * Transitions an `ENABLED` module to `DISABLED`, making its registered
     * Tools unreachable again without removing its registration.
     *
     * Throws [NoSuchElementException] if [moduleId] is not registered, and
     * [IllegalArgumentException] if the module's current status cannot
     * legally transition to `DISABLED`.
     */
    suspend fun disable(moduleId: ModuleId, requestingPrincipalId: PrincipalId): ModuleStatus

    /**
     * Transitions a `REGISTERED` or `DISABLED` module to `REMOVED`,
     * withdrawing its registration and its `toolsExposed` entries from
     * `ToolRegistry` entirely (Contract Design Section 5).
     *
     * Throws [NoSuchElementException] if [moduleId] is not registered, and
     * [IllegalArgumentException] if the module's current status cannot
     * legally transition to `REMOVED`.
     */
    suspend fun remove(moduleId: ModuleId, requestingPrincipalId: PrincipalId): ModuleStatus

    /** Returns the registered [ModuleDescriptor] for [moduleId], or `null` if none is registered. */
    suspend fun getModuleDescriptor(moduleId: ModuleId): ModuleDescriptor?

    /** Returns the current [ModuleStatus] for [moduleId], or `null` if none is registered. */
    suspend fun getModuleStatus(moduleId: ModuleId): ModuleStatus?

    /** Every registered module's [ModuleId], per Contract Design Section 5's "lookup is part of the core interface." */
    suspend fun listModules(): List<ModuleId>
}
