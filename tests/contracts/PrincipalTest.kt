package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class PrincipalTest {

    private fun principal(displayName: String = "Steven") = Principal(
        principalId = PrincipalId("user-1"),
        principalType = PrincipalType.USER,
        displayName = displayName,
        owner = null,
        status = PrincipalStatus.ACTIVE,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-02T00:00:00Z"),
    )

    @Test
    fun `a principal can be constructed with all required fields`() {
        val p = principal()

        assertEquals(PrincipalId("user-1"), p.principalId)
        assertEquals(PrincipalType.USER, p.principalType)
        assertEquals(PrincipalStatus.ACTIVE, p.status)
    }

    @Test
    fun `two principals with identical fields are equal`() {
        assertEquals(principal(), principal())
    }

    @Test
    fun `principals with different display names are not equal`() {
        assertNotEquals(principal(displayName = "Steven"), principal(displayName = "Other"))
    }

    @Test
    fun `a blank display name is rejected`() {
        assertFailsWith<IllegalArgumentException> { principal(displayName = "") }
    }

    @Test
    fun `an internal agent is still representable as a principal, per ADR-013`() {
        val agent = principal().copy(
            principalId = PrincipalId("agent-1"),
            principalType = PrincipalType.INTERNAL_AGENT,
            owner = PrincipalId("user-1"),
        )

        assertEquals(PrincipalType.INTERNAL_AGENT, agent.principalType)
        assertEquals(PrincipalId("user-1"), agent.owner)
    }
}
