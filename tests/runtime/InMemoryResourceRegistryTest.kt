package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class InMemoryResourceRegistryTest {

    private fun resource(id: String = "res-1", owner: String = "user-1") = Resource(
        resourceId = ResourceId(id),
        resourceType = ResourceType.TOOL,
        displayName = "Test Resource",
        ownerPrincipalId = PrincipalId(owner),
        sensitivity = "HOUSEHOLD",
        lifecycleState = ResourceLifecycleState.AVAILABLE,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        source = "test",
    )

    @Test
    fun `register then resolve returns the same Resource`() = runTest {
        val registry = InMemoryResourceRegistry()
        val id = registry.register(resource())
        assertEquals(resource(), registry.resolve(id))
    }

    @Test
    fun `resolve of an unregistered id returns null, not an exception`() = runTest {
        val registry = InMemoryResourceRegistry()
        assertNull(registry.resolve(ResourceId("nonexistent")))
    }

    @Test
    fun `registering the same resourceId twice fails`() = runTest {
        val registry = InMemoryResourceRegistry()
        registry.register(resource())
        assertFailsWith<IllegalArgumentException> { registry.register(resource()) }
    }

    @Test
    fun `update requires prior registration`() = runTest {
        val registry = InMemoryResourceRegistry()
        assertFailsWith<IllegalArgumentException> { registry.update(resource()) }
    }

    @Test
    fun `update replaces the stored Resource`() = runTest {
        val registry = InMemoryResourceRegistry()
        val id = registry.register(resource())
        val updated = resource().copy(displayName = "Renamed")
        registry.update(updated)
        assertEquals("Renamed", registry.resolve(id)?.displayName)
    }

    @Test
    fun `listByOwner filters by ownerPrincipalId`() = runTest {
        val registry = InMemoryResourceRegistry()
        registry.register(resource(id = "res-1", owner = "user-1"))
        registry.register(resource(id = "res-2", owner = "user-2"))

        val owned = registry.listByOwner(PrincipalId("user-1"))
        assertEquals(1, owned.size)
        assertEquals(ResourceId("res-1"), owned.single().resourceId)
    }
}
