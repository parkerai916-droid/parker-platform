package parker.core.interfaces

import java.time.Instant

/**
 * Permission Contract (Volume 1) and the concrete PermissionDecision
 * schema (docs/schemas/PermissionDecision.schema.json). Volume 1 describes
 * "Permission" as a concept (the question, the outcomes, the levels)
 * without its own separate required-fields list; the JSON Schema gives the
 * concrete shape for the same concept under the name "PermissionDecision".
 * This file treats them as one type -- see IMPLEMENTATION_GAPS.md if that
 * reading turns out to be wrong.
 *
 * Core question (Chapter 10 / Permission.md): "May this Principal perform
 * this Action on this Resource under these circumstances?" -- deliberately
 * NOT "may this tool run" (see the reconciliation report for why the v0.1
 * prototype's tool-scoped model doesn't match this).
 */
enum class PermissionAction {
    READ,
    WRITE,
    DELETE,
    EXECUTE,
    EXPORT,
    SHARE,
    CONTROL,
    NOTIFY,
    SCHEDULE,
    SEND_EXTERNAL,
}

enum class PermissionLevel {
    AUTOMATIC,
    USER_AWARE,
    CONFIRMATION_REQUIRED,
    HIGH_ASSURANCE,
    ADMINISTRATIVE,
}

enum class PermissionDecisionOutcome {
    APPROVED,
    APPROVED_WITH_CONFIRMATION,
    DEFERRED,
    DENIED,
}

data class PermissionDecision(
    val decisionId: DecisionId,
    val principalId: PrincipalId,
    val resourceId: ResourceId,
    val action: PermissionAction,
    val decision: PermissionDecisionOutcome,
    val level: PermissionLevel,
    val timestamp: Instant,
)

/**
 * PROVISIONAL. `PermissionEngine.explain(decisionId): PermissionExplanation`
 * is named in Volume 3's interface stub, but no specification anywhere
 * defines what an explanation actually contains. This is the minimal shape
 * needed for the interface to compile -- see IMPLEMENTATION_GAPS.md. Do
 * not build further behaviour on this until it's actually specified.
 */
data class PermissionExplanation(
    val decisionId: DecisionId,
    val reason: String,
)
