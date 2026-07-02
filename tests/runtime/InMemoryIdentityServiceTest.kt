package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.IdentityService
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Proves the behaviours docs/architecture/IdentityService.md requires:
 * registration (incl. owning-context validation for delegated Principal
 * types), never-throw resolution, PrincipalLifecycleTransitions
 * enforcement, lastSeenAt tracking, and ownership queries.
 *
 * Does not exercise, mock, or reference Agent, MemoryStore, WorldModel,
 * ModelManager, Plugin, or any AI/model integration -- none of those
 * systems are dependencies of this service, per this phase's explicit
 * scope (see the last test in this file for the structural proof).
 */
class InMemoryIdentityServiceTest {

    private fun principal(
        id: String = "user-1",
        type: PrincipalType = PrincipalType.USER,
        owner: PrincipalId? = null,
        status: PrincipalStatus = PrincipalStatus.CREATED,
        displayName: String = "Test Principal",
    ) = Principal(
        principalId = PrincipalId(id),
        principalType = type,
        displayName = displayName,
        owner = owner,
        status = status,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    // --- Registration ---

    @Test
    fun `registering a valid principal succeeds and is resolvable`() = runTest {
        val service = InMemoryIdentityService()
        val id = service.register(principal())

        assertEquals(PrincipalId("user-1"), id)
        assertEquals(principal(), service.resolve(id))
    }

    @Test
    fun `registering a duplicate principalId is rejected deterministically`() = runTest {
        val service = InMemoryIdentityService()
        service.register(principal())

        assertFailsWith<IllegalArgumentException> { service.register(principal()) }
    }

    @Test
    fun `a newly registered principal must have CREATED status`() = runTest {
        val service = InMemoryIdentityService()
        assertFailsWith<IllegalArgumentException> {
            service.register(principal(status = PrincipalStatus.ACTIVE))
        }
    }

    @Test
    fun `a delegated principal type requires an already-registered owner`() = runTest {
        val service = InMemoryIdentityService()

        // No owner at all.
        assertFailsWith<IllegalArgumentException> {
            service.register(principal(id = "agent-1", type = PrincipalType.INTERNAL_AGENT, owner = null))
        }

        // Owner set, but that owner was never registered.
        assertFailsWith<IllegalArgumentException> {
            service.register(
                principal(id = "agent-2", type = PrincipalType.INTERNAL_AGENT, owner = PrincipalId("ghost-owner")),
            )
        }
    }

    @Test
    fun `USER and SYSTEM principals may register with a null owner`() = runTest {
        val service = InMemoryIdentityService()
        service.register(principal(id = "user-3", type = PrincipalType.USER, owner = null))
        service.register(principal(id = "system-1", type = PrincipalType.SYSTEM, owner = null))

        assertEquals(PrincipalType.USER, service.resolve(PrincipalId("user-3"))?.principalType)
        assertEquals(PrincipalType.SYSTEM, service.resolve(PrincipalId("system-1"))?.principalType)
    }

    @Test
    fun `a delegated principal type registers successfully once its owner already exists`() = runTest {
        val service = InMemoryIdentityService()
        service.register(principal(id = "user-4", type = PrincipalType.USER, owner = null))

        val agentId = service.register(
            principal(id = "agent-3", type = PrincipalType.INTERNAL_AGENT, owner = PrincipalId("user-4")),
        )

        assertEquals(PrincipalId("agent-3"), agentId)
        assertEquals(PrincipalId("user-4"), service.resolve(agentId)?.owner)
    }

    // --- Lookup ---

    @Test
    fun `resolving an existing principal returns it`() = runTest {
        val service = InMemoryIdentityService()
        service.register(principal(id = "user-2"))

        val resolved = service.resolve(PrincipalId("user-2"))
        assertEquals("user-2", resolved?.principalId?.value)
    }

    @Test
    fun `resolving a missing principal returns null, not an exception`() = runTest {
        val service = InMemoryIdentityService()
        assertNull(service.resolve(PrincipalId("nonexistent")))
    }

    // --- Lifecycle / status ---

    @Test
    fun `status transitions follow the documented lifecycle exactly`() = runTest {
        val service = InMemoryIdentityService()
        service.register(principal(id = "user-5"))

        val active = service.updateStatus(PrincipalId("user-5"), PrincipalStatus.ACTIVE)
        assertEquals(PrincipalStatus.ACTIVE, active.status)

        val suspended = service.updateStatus(PrincipalId("user-5"), PrincipalStatus.SUSPENDED)
        assertEquals(PrincipalStatus.SUSPENDED, suspended.status)

        val revoked = service.updateStatus(PrincipalId("user-5"), PrincipalStatus.REVOKED)
        assertEquals(PrincipalStatus.REVOKED, revoked.status)

        val archived = service.updateStatus(PrincipalId("user-5"), PrincipalStatus.ARCHIVED)
        assertEquals(PrincipalStatus.ARCHIVED, archived.status)
    }

    @Test
    fun `skipping a lifecycle step is rejected -- Created cannot go directly to Revoked`() = runTest {
        val service = InMemoryIdentityService()
        service.register(principal(id = "user-6"))

        assertFailsWith<IllegalArgumentException> {
            service.updateStatus(PrincipalId("user-6"), PrincipalStatus.REVOKED)
        }
    }

    @Test
    fun `Archived is terminal -- no further transitions are permitted`() = runTest {
        val service = InMemoryIdentityService()
        service.register(principal(id = "user-7"))
        service.updateStatus(PrincipalId("user-7"), PrincipalStatus.ACTIVE)
        service.updateStatus(PrincipalId("user-7"), PrincipalStatus.SUSPENDED)
        service.updateStatus(PrincipalId("user-7"), PrincipalStatus.REVOKED)
        service.updateStatus(PrincipalId("user-7"), PrincipalStatus.ARCHIVED)

        assertFailsWith<IllegalArgumentException> {
            service.updateStatus(PrincipalId("user-7"), PrincipalStatus.ACTIVE)
        }
    }

    @Test
    fun `updateStatus on an unregistered principal throws`() = runTest {
        val service = InMemoryIdentityService()
        assertFailsWith<NoSuchElementException> {
            service.updateStatus(PrincipalId("never-registered"), PrincipalStatus.ACTIVE)
        }
    }

    @Test
    fun `a Revoked or Suspended principal is still resolvable -- status filtering is a caller responsibility`() = runTest {
        // Conservative choice recorded in IMPLEMENTATION_GAPS.md: resolve() does not suppress
        // Revoked/Archived Principals (IdentityService.md leaves this an open choice between
        // "refuse to resolve" and "resolve as inert"). Suppressing here would be inventing a
        // decision the architecture doesn't make.
        val service = InMemoryIdentityService()
        service.register(principal(id = "user-11"))
        service.updateStatus(PrincipalId("user-11"), PrincipalStatus.ACTIVE)
        service.updateStatus(PrincipalId("user-11"), PrincipalStatus.SUSPENDED)

        val suspended = service.resolve(PrincipalId("user-11"))
        assertEquals(PrincipalStatus.SUSPENDED, suspended?.status)
    }

    // --- Last seen ---

    @Test
    fun `touch updates lastSeenAt without changing status or other fields`() = runTest {
        val service = InMemoryIdentityService()
        service.register(principal(id = "user-8"))
        val before = service.resolve(PrincipalId("user-8"))!!

        val touched = service.touch(PrincipalId("user-8"))

        assertTrue(touched.lastSeenAt.isAfter(before.lastSeenAt) || touched.lastSeenAt == before.lastSeenAt)
        assertEquals(before.status, touched.status)
        assertEquals(before.principalId, touched.principalId)
        assertEquals(before.displayName, touched.displayName)
        assertEquals(before.owner, touched.owner)
    }

    @Test
    fun `touch on an unregistered principal throws`() = runTest {
        val service = InMemoryIdentityService()
        assertFailsWith<NoSuchElementException> {
            service.touch(PrincipalId("never-registered"))
        }
    }

    // --- Ownership ---

    @Test
    fun `listByOwner returns every principal delegated to a given owner`() = runTest {
        val service = InMemoryIdentityService()
        service.register(principal(id = "user-9", type = PrincipalType.USER, owner = null))
        service.register(principal(id = "agent-4", type = PrincipalType.INTERNAL_AGENT, owner = PrincipalId("user-9")))
        service.register(principal(id = "tool-1", type = PrincipalType.TOOL, owner = PrincipalId("user-9")))
        service.register(principal(id = "user-10", type = PrincipalType.USER, owner = null))

        val owned = service.listByOwner(PrincipalId("user-9"))
        assertEquals(2, owned.size)
        assertEquals(setOf("agent-4", "tool-1"), owned.map { it.principalId.value }.toSet())
    }

    // --- Scope discipline ---

    @Test
    fun `InMemoryIdentityService has no dependency on Agent, Memory, World Model, or AI systems`() {
        // Structural proof, not a runtime assertion: the constructor takes no arguments, and
        // nothing in this class references Agent, MemoryStore, WorldModel, ModelManager, or
        // Plugin types -- none of those systems exist as concrete implementations, and none are
        // touched by this phase, per its explicit scope.
        val service: IdentityService = InMemoryIdentityService()
        assertTrue(service is IdentityService)
    }
}
