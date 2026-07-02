package parker.core.interfaces

import java.time.Instant

/**
 * ExecutionResult Contract (Volume 1). See
 * docs/specifications/volume-01-core-contracts/ExecutionResult.md and
 * docs/schemas/ExecutionResult.schema.json.
 */
enum class ExecutionResultStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    CANCELLED,
    DENIED,
    EXPIRED,
    DEFERRED,
}

/**
 * PROVISIONAL. Tool.md ("Tools MUST return structured results") never
 * defines ToolResult's shape. Minimal placeholder needed for the Tool
 * interface to compile -- see IMPLEMENTATION_GAPS.md.
 */
data class ToolResult(
    val toolId: String,
    val success: Boolean,
    val output: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
) {
    init {
        require(toolId.isNotBlank()) { "ToolResult.toolId must not be blank" }
    }
}

/**
 * @param auditRecordId plain String rather than a typed AuditRecordId --
 *   the Audit Record system is out of Phase 1 scope, so no such type
 *   exists yet.
 * @param reflectionCandidate ExecutionResult.md lists "reflectionCandidate"
 *   as a required field but never states its type. Inferred as Boolean
 *   (is this result a candidate for the Reflection Engine to examine) as
 *   the most direct reading -- flagged in IMPLEMENTATION_GAPS.md.
 */
data class ExecutionResult(
    val resultId: ResultId,
    val requestId: RequestId,
    val status: ExecutionResultStatus,
    val startedAt: Instant,
    val completedAt: Instant?,
    val affectedResources: List<ResourceId> = emptyList(),
    val toolResults: List<ToolResult> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val auditRecordId: String? = null,
    val reflectionCandidate: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        if (completedAt != null) {
            require(!completedAt.isBefore(startedAt)) { "ExecutionResult.completedAt must not precede startedAt" }
        }
        if (status == ExecutionResultStatus.FAILED) {
            require(errors.isNotEmpty()) { "A FAILED ExecutionResult must include at least one machine-readable error" }
        }
    }
}
