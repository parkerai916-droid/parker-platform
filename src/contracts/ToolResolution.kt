package parker.core.interfaces

/**
 * Registry-level failure reasons, per tool-registry.md "Failure
 * Behaviour": these are distinguished from Tool-level failures (which are
 * the executing Tool's own [ToolResult]/[ExecutionResult] concern, not
 * the registry's). tool-registry.md deliberately proposes no new
 * `ExecutionResultStatus` value for these -- they are meant to surface as
 * `ExecutionResultStatus.FAILED` with one of these as a machine-readable
 * error code. That mapping is the Execution Pipeline's job (out of scope
 * for this phase); this enum is the typed vocabulary the Tool Registry
 * itself returns.
 */
enum class ToolResolutionFailureReason {
    TOOL_NOT_FOUND,
    TOOL_AMBIGUOUS,
    TOOL_DISABLED,
    TOOL_REGISTRY_UNAVAILABLE,
}

/**
 * Result of [ToolRegistry.resolve] -- the Execution-Pipeline-only lookup
 * that yields something invocable, or a typed reason it could not.
 * Deliberately does not carry a live `Tool` instance in this phase (no
 * concrete `Tool` implementations exist yet to resolve to); it resolves
 * to the matching [ToolDescriptor] instead, which is enough to prove the
 * lookup mechanics (registration, capability matching, lifecycle
 * filtering) without inventing a Tool execution runtime this phase was
 * not asked to build.
 */
sealed class ToolResolution {
    data class Resolved(val descriptor: ToolDescriptor) : ToolResolution()
    data class Failed(val reason: ToolResolutionFailureReason) : ToolResolution()
}

/**
 * Result of [ToolRegistry.register]. tool-registry.md "Registration
 * Model" point 5: re-registering an unchanged `toolId`/`version` pair is
 * idempotent (not an error); registering a new `version` for an
 * already-known `toolId` is version supersession (also not an error --
 * see [ToolRegistry.register] doc). [Rejected] covers every other failure
 * mode: a `toolId`+`version` pair that already exists with a
 * *different* descriptor (inconsistent data, not supersession or a
 * no-op), or a `resourceId` that does not resolve to a registered
 * `Resource` (tool-registry.md: "Registration MUST fail if this Resource
 * entry cannot be created or resolved").
 */
sealed class ToolRegistrationOutcome {
    data class Registered(val toolId: String, val version: String, val state: ToolLifecycleState) : ToolRegistrationOutcome()
    data class Superseded(val toolId: String, val previousVersion: String, val newVersion: String) : ToolRegistrationOutcome()
    data class AlreadyRegistered(val toolId: String, val version: String) : ToolRegistrationOutcome()
    data class Rejected(val reason: String) : ToolRegistrationOutcome()
}
