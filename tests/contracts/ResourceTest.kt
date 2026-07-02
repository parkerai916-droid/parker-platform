package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResourceTest {

    private fun resource(sensitivity: String = "high") = Resource(
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
    fun `every resource must declare a sensitivity classification`() {
        assertFailsWith<IllegalArgumentException> { resource(sensitivity = "") }
    }
}
