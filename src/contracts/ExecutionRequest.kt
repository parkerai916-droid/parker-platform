package parker.core.interfaces

import java.time.Instant

/**
 * ExecutionRequest Contract (Volume 1). See
 * docs/specifications/volume-01-core-contracts/ExecutionRequest.md and
 * docs/schemas/ExecutionRequest.schema.json.
 *
 * ADR-017: "Any proposed work that may cause execution, resource access,
 * state mutation, or external side effects MUST become an
 * ExecutionRequest." ADR-018: immutable after validation (enforced here by
 * simply being a `val`-only data class with no mutator).
 *
 * NOTE: `expiresAt` and `correlationId` are required fields in the prose
 * spec but are absent from ExecutionRequest.schema.json. Included here
 * per the prose (both are load-bearing: the `Expired` lifecycle state and
 * `ExecutionResult`'s `Expired` status only make sense if something
 * tracks an expiry, and every request needs a correlation id for the
 * audit trail ADR-020 requires). See IMPLEMENTATION_GAPS.md.
 */
enum class RequestOrigin {
    VOICE,
    TEXT,
    SCHEDULED_TASK,
    AGENT,
    PLUGIN,
    HOME_ASSISTANT_EVENT,
    ANDROID_EVENT,
    REMOTE_INTERFACE,
}

enum class RequestPriority {
    IMMEDIATE,
    HIGH,
    NORMAL,
    BACKGROUND,
    DEFERRED,
    MAINTENANCE,
}

enum class RiskEstimate {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

data class ExecutionRequest(
    val requestId: RequestId,
    val principalId: PrincipalId,
    val origin: RequestOrigin,
    val intent: String,
    val targetResources: List<ResourceId>,
    val proposedActions: List<String>,
    val priority: RequestPriority,
    val createdAt: Instant,
    val correlationId: String,
    val sessionId: String? = null,
    val riskEstimate: RiskEstimate? = null,
    val expiresAt: Instant? = null,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(intent.isNotBlank()) { "ExecutionRequest.intent must not be blank" }
        require(correlationId.isNotBlank()) { "ExecutionRequest.correlationId must not be blank" }
        if (expiresAt != null) {
            require(expiresAt.isAfter(createdAt)) { "ExecutionRequest.expiresAt must be after createdAt" }
        }
    }
}
