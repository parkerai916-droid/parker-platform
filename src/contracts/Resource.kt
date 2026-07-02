package parker.core.interfaces

import java.time.Instant

/**
 * Resource Contract (Volume 1). See docs/specifications/volume-01-core-contracts/Resource.md
 * and Chapter 8 - Resource Registry.
 *
 * Core invariant (Chapter 4): "If something is not represented within the
 * Resource Registry, Parker assumes it is inaccessible."
 */
enum class ResourceType {
    MEMORY,
    WORLD_MODEL,
    DOCUMENT,
    EMAIL,
    CALENDAR,
    CONTACT,
    HOME_ASSISTANT_ENTITY,
    ANDROID_CAPABILITY,
    TOOL,
    PLUGIN,
    AGENT,
    SECRET,
    CONFIGURATION,
    AUDIT_LOG,
}

/**
 * Resource.md states this lifecycle as a linear chain (Created -> Registered
 * -> Available -> Updated -> Archived -> Deleted) with no diagram. Unlike
 * ExecutionLifecycleState, there is deliberately no transition validator --
 * see IMPLEMENTATION_GAPS.md for why (in particular, whether Available and
 * Updated cycle back and forth is not specified).
 */
enum class ResourceLifecycleState {
    CREATED,
    REGISTERED,
    AVAILABLE,
    UPDATED,
    ARCHIVED,
    DELETED,
}

/**
 * @param sensitivity Resource.md requires "a sensitivity classification"
 *   but never enumerates the value set anywhere I could find (unlike
 *   ExecutionRequest's RiskEstimate, which is a concrete enum). Typed as a
 *   free-form String rather than guessing at enum members that aren't
 *   specified -- see IMPLEMENTATION_GAPS.md.
 */
data class Resource(
    val resourceId: ResourceId,
    val resourceType: ResourceType,
    val displayName: String,
    val ownerPrincipalId: PrincipalId,
    val sensitivity: String,
    val lifecycleState: ResourceLifecycleState,
    val createdAt: Instant,
    val updatedAt: Instant,
    val source: String,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(displayName.isNotBlank()) { "Resource.displayName must not be blank" }
        require(sensitivity.isNotBlank()) { "Resource.sensitivity must not be blank" }
    }
}
