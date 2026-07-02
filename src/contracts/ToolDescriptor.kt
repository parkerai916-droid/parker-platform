package parker.core.interfaces

/**
 * Needed by the existing Tool.kt stub (`val descriptor: ToolDescriptor`).
 * Tool.md describes Tool's responsibilities in prose but never gives
 * ToolDescriptor a field list -- this is inferred from what a tool
 * evidently needs to describe itself (id, name, description) plus a
 * version for the "auditable, predictable" requirement. Flagged in
 * IMPLEMENTATION_GAPS.md as an inferred, not specified, shape.
 *
 * @param supportedActions @param supportedResourceTypes Added in Phase 2
 *   (v0.7 Architecture Completion Phase, `docs/architecture/tool-registry.md`
 *   "Capability Declaration"): what this Tool declares itself capable of
 *   performing, once a request naming one of these (action, resourceType)
 *   pairs is already Permission-approved. This is what makes
 *   `ToolRegistry.resolve` deterministic instead of needing a `toolId` on
 *   `ExecutionRequest` (which the canonical schema deliberately does not
 *   have -- see tool-registry.md "Lookup Process"). Both default to empty
 *   so existing callers of this data class (Phase 1) are unaffected.
 */
data class ToolDescriptor(
    val toolId: String,
    val displayName: String,
    val description: String,
    val version: String = "0.1.0",
    val supportedActions: Set<PermissionAction> = emptySet(),
    val supportedResourceTypes: Set<ResourceType> = emptySet(),
) {
    init {
        require(toolId.isNotBlank()) { "ToolDescriptor.toolId must not be blank" }
        require(displayName.isNotBlank()) { "ToolDescriptor.displayName must not be blank" }
    }
}

/**
 * Needed by the existing Tool.kt stub (`suspend fun validate(request): ValidationResult`).
 * Shape inferred from Tool.md's "Tools MUST validate before execution"
 * requirement -- a validation either passes or fails with reasons. Not
 * independently specified; see IMPLEMENTATION_GAPS.md.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reasons: List<String>) : ValidationResult() {
        init {
            require(reasons.isNotEmpty()) { "Invalid must carry at least one reason" }
        }
    }
}
