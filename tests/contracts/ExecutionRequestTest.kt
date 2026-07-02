package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExecutionRequestTest {

    private fun request(
        intent: String = "send an email",
        correlationId: String = "corr-1",
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        expiresAt: Instant? = Instant.parse("2026-01-01T00:05:00Z"),
    ) = ExecutionRequest(
        requestId = RequestId("req-1"),
        principalId = PrincipalId("user-1"),
        origin = RequestOrigin.TEXT,
        intent = intent,
        targetResources = listOf(ResourceId("email-draft-1")),
        proposedActions = listOf("SEND_EXTERNAL"),
        priority = RequestPriority.NORMAL,
        createdAt = createdAt,
        correlationId = correlationId,
        expiresAt = expiresAt,
    )

    @Test
    fun `a request can be constructed with all required fields plus optional expiry`() {
        val r = request()

        assertEquals(RequestOrigin.TEXT, r.origin)
        assertEquals(listOf(ResourceId("email-draft-1")), r.targetResources)
    }

    @Test
    fun `two requests with identical fields are equal`() {
        assertEquals(request(), request())
    }

    @Test
    fun `a blank intent is rejected`() {
        assertFailsWith<IllegalArgumentException> { request(intent = "") }
    }

    @Test
    fun `a blank correlationId is rejected`() {
        assertFailsWith<IllegalArgumentException> { request(correlationId = "") }
    }

    @Test
    fun `an expiresAt at or before createdAt is rejected`() {
        val createdAt = Instant.parse("2026-01-01T00:00:00Z")

        assertFailsWith<IllegalArgumentException> {
            request(createdAt = createdAt, expiresAt = createdAt)
        }
    }

    @Test
    fun `expiresAt is optional`() {
        val r = request(expiresAt = null)

        assertEquals(null, r.expiresAt)
    }
}
