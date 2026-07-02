package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceTest {

    private fun resource(sensitivity: ResourceSensitivity = ResourceSensitivity.PERSONAL) = Resource(
        resourceId = ResourceId("doc-1"),
        resourceType = ResourceType.DOCUMENT,
        displayName = "Passport scan",
        ownerPrincipalId = PrincipalId("user-1"),
        sensitivity = sensitivity,
        lifecycleState = ResourceLifecycleState.AVAILABLE,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        source = "document-intelligence",
    )

    @Test
    fun `a resource can be constructed with all required fields`() {
        val r = resource()

        assertEquals(ResourceType.DOCUMENT, r.resourceType)
        assertEquals(ResourceLifecycleState.AVAILABLE, r.lifecycleState)
    }

    @Test
    fun `two resources with identical fields are equal`() {
        assertEquals(resource(), resource())
    }

    @Test
    fun `every resource must declare a sensitivity classification, and all nine schema values are representable`() {
        // Resource.schema.json's sensitivity enum, exactly (IMPLEMENTATION_GAPS.md #10/#29) --
        // a completeness check that ResourceSensitivity hasn't drifted from the schema, in
        // either direction, now that sensitivity is a real enum rather than a free-form String.
        val expected = setOf(
            ResourceSensitivity.PUBLIC,
            ResourceSensitivity.PERSONAL,
            ResourceSensitivity.HOUSEHOLD,
            ResourceSensitivity.FINANCIAL,
            ResourceSensitivity.MEDICAL,
            ResourceSensitivity.LEGAL,
            ResourceSensitivity.SECURITY_SENSITIVE,
            ResourceSensitivity.CREDENTIALS_SECRETS,
            ResourceSensitivity.THIRD_PARTY_PERSONAL_DATA,
        )
        assertEquals(expected, ResourceSensitivity.values().toSet())

        // Every value must be usable to construct a Resource -- sensitivity is a required,
        // non-optional field regardless of which classification is chosen.
        for (value in ResourceSensitivity.values()) {
            assertEquals(value, resource(sensitivity = value).sensitivity)
        }
    }
}
