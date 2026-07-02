package parker.core.interfaces

/**
 * Tool Registry (`docs/architecture/tool-registry.md`). Not yet a Volume 3
 * document -- that architecture document is the specification this
 * interface implements directly; promoting a formal `ToolRegistry.md` to
 * Volume 3 alongside it is recorded as a follow-up in
 * `docs/architecture/IMPLEMENTATION_GAPS.md`, not done silently here.
 *
 * Per tool-registry.md, nothing except the Execution Pipeline may call
 * [resolve] (the only operation that yields something invocable);
 * [listAll]/[findCandidates] are the read-only discovery surface other
 * components (Planner, Conversation Engine) may use.
 *
 * KNOWN SCOPE REDUCTION (recorded in IMPLEMENTATION_GAPS.md, not
 * invented around): tool-registry.md's Discovery Model specifies
 * Principal-scoped visibility ("a Tool descriptor is only returned to a
 * caller whose Principal has *some* plausible Permission path to it").
 * That requires resolving a caller's Principal (IdentityService) and
 * evaluating plausible permission paths (Permission Engine policy),
 * neither of which exists as a concrete implementation yet. [listAll] and
 * [findCandidates] below have no caller-scoping parameter as a result --
 * they return the full visible-to-anyone catalogue. This is a deliberate,
 * documented gap, not a silent narrowing of the architecture.
 */
interface ToolRegistry {

    /**
     * Registers [descriptor] against an already-registered Resource
     * ([resourceId], expected `resourceType == ResourceType.TOOL`) per
     * tool-registry.md "Registration Model". See [ToolRegistrationOutcome]
     * for the exact idempotent/supersession/rejection rules.
     */
    suspend fun register(descriptor: ToolDescriptor, resourceId: ResourceId): ToolRegistrationOutcome

    /**
     * The Execution-Pipeline-only lookup: given an already-approved
     * [action] and the [resourceTypes] of an ExecutionRequest's target
     * Resources, finds the single `ENABLED` Tool declaring that
     * capability. See tool-registry.md "Lookup Process" for the
     * zero/one/many-candidate rules.
     */
    suspend fun resolve(action: PermissionAction, resourceTypes: Set<ResourceType>): ToolResolution

    /**
     * Capability-filtered candidates for planning purposes -- returns
     * descriptors only, never a live Tool (tool-registry.md "Discovery
     * Model": models/planners never hold executable references, only the
     * Execution Pipeline does, via [resolve]).
     */
    suspend fun findCandidates(actions: Set<PermissionAction>, resourceTypes: Set<ResourceType>): List<ToolDescriptor>

    /** Full visible catalogue, for administrative/diagnostic use. */
    suspend fun listAll(): List<ToolDescriptor>

    /**
     * The only sanctioned way to move a registered Tool through
     * [ToolLifecycleState] (tool-registry.md "Runtime Lifecycle").
     * Throws if `from -> to` is not a valid edge in
     * [ToolLifecycleTransitions].
     */
    suspend fun setLifecycleState(toolId: String, version: String, newState: ToolLifecycleState): ToolDescriptor
}
