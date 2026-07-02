package parker.core.interfaces

import java.time.Instant

/**
 * Principal Contract (Volume 1). See docs/specifications/volume-01-core-contracts/Principal.md
 * and Chapter 41 - Identity Service.
 *
 * "Identity is not permission": this type answers *who* is requesting
 * something. Whether they may do it is the PermissionEngine's job, not
 * this type's.
 */
enum class PrincipalType {
    USER,
    SYSTEM,
    INTERNAL_AGENT,
    PLUGIN,
    TOOL,
    SCHEDULED_TASK,
    DEVELOPER_SESSION,
    FUTURE_REMOTE_DEVICE,
}

/**
 * Principal.md states the lifecycle as a linear chain (Created -> Active ->
 * Suspended -> Revoked -> Archived) with no accompanying state-machine
 * diagram and no stated branching rules (e.g. can a Suspended principal
 * return to Active?). Unlike ExecutionLifecycleState, there is deliberately
 * no transition validator for this enum -- see IMPLEMENTATION_GAPS.md.
 */
enum class PrincipalStatus {
    CREATED,
    ACTIVE,
    SUSPENDED,
    REVOKED,
    ARCHIVED,
}

/**
 * @param owner the Principal that owns/is responsible for this one (e.g. the
 *   user who installed a Plugin). Nullable because the spec doesn't say
 *   what, if anything, owns a root User or System principal.
 */
data class Principal(
    val principalId: PrincipalId,
    val principalType: PrincipalType,
    val displayName: String,
    val owner: PrincipalId?,
    val status: PrincipalStatus,
    val createdAt: Instant,
    val lastSeenAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(displayName.isNotBlank()) { "Principal.displayName must not be blank" }
    }
}
