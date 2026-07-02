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
 * Resource.schema.json defines the value set IMPLEMENTATION_GAPS.md #10/#29
 * originally said was missing -- that finding was a correction: the enum was
 * only absent from the prose Resource.md, not from the schema. Sourced
 * directly from Resource.schema.json's `sensitivity` property (targeted
 * refinement pass, closes #10/#29). No values were invented here.
 */
enum class ResourceSensitivity {
    PUBLIC,
    PERSONAL,
    HOUSEHOLD,
    FINANCIAL,
    MEDICAL,
    LEGAL,
    SECURITY_SENSITIVE,
    CREDENTIALS_SECRETS,
    THIRD_PARTY_PERSONAL_DATA,
}

/**
 * @param sensitivity Resource.md requires "a sensitivity classification";
 *   the value set is defined by Resource.schema.json and represented here
 *   by [ResourceSensitivity] (targeted refinement pass, closes
 *   IMPLEMENTATION_GAPS.md #10/#29 -- previously a free-form String).
 */
data class Resource(
    val resourceId: ResourceId,
    val resourceType: ResourceType,
    val displayName: String,
    val ownerPrincipalId: PrincipalId,
    val sensitivity: ResourceSensitivity,
    val lifecycleState: ResourceLifecycleState,
    val createdAt: Instant,
    val updatedAt: Instant,
    val source: String,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(displayName.isNotBlank()) { "Resource.displayName must not be blank" }
    }
}
