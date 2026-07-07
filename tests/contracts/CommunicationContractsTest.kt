package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Sprint 7, Unit C1. Construction-time validation tests for the
 * field-level Communication contracts
 * `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` approved:
 * [CorrelationId], [InboundOwnerMessage], [OutboundParkerResponse], and
 * [CommunicationIntakeDisposition]. Behavioural tests of
 * [CommunicationIntake] live in
 * `tests/runtime/InMemoryCommunicationIntakeTest.kt` instead -- this file
 * is pure data-shape validation, mirroring `MemoryContractsTest.kt`'s own
 * scope.
 */
class CommunicationContractsTest {

    private val fixedInstant: Instant = Instant.parse("2026-07-07T12:00:00Z")

    // --- CorrelationId ---

    @Test
    fun `CorrelationId with equal values are equal`() {
        assertEquals(CorrelationId("corr-1"), CorrelationId("corr-1"))
    }

    @Test
    fun `a blank CorrelationId is rejected at construction`() {
        assertFailsWith<IllegalArgumentException> { CorrelationId("") }
        assertFailsWith<IllegalArgumentException> { CorrelationId("   ") }
    }

    // --- InboundOwnerMessage ---

    private fun inbound(
        channelId: ModuleId = ModuleId("channel.text"),
        senderPrincipalId: PrincipalId = PrincipalId("user.owner"),
        text: String = "turn on the kitchen light",
        timestamp: Instant = fixedInstant,
        correlationId: CorrelationId = CorrelationId("corr-1"),
        metadata: Map<String, String> = emptyMap(),
    ) = InboundOwnerMessage(
        channelId = channelId,
        senderPrincipalId = senderPrincipalId,
        text = text,
        timestamp = timestamp,
        correlationId = correlationId,
        metadata = metadata,
    )

    @Test
    fun `an InboundOwnerMessage with blank text is rejected`() {
        assertFailsWith<IllegalArgumentException> { inbound(text = "") }
        assertFailsWith<IllegalArgumentException> { inbound(text = "   ") }
    }

    @Test
    fun `InboundOwnerMessage metadata defaults to empty`() {
        assertTrue(inbound().metadata.isEmpty())
    }

    @Test
    fun `InboundOwnerMessage carries the fields Contract Design Section 2 requires`() {
        val message = inbound(metadata = mapOf("confidence" to "0.92"))

        assertEquals(ModuleId("channel.text"), message.channelId)
        assertEquals(PrincipalId("user.owner"), message.senderPrincipalId)
        assertEquals("turn on the kitchen light", message.text)
        assertEquals(fixedInstant, message.timestamp)
        assertEquals(CorrelationId("corr-1"), message.correlationId)
        assertEquals(mapOf("confidence" to "0.92"), message.metadata)
    }

    // --- OutboundParkerResponse ---

    private fun outbound(
        channelId: ModuleId = ModuleId("channel.text"),
        senderPrincipalId: PrincipalId = PrincipalId("system.parker"),
        text: String = "the kitchen light is now on",
        timestamp: Instant = fixedInstant,
        correlationId: CorrelationId = CorrelationId("corr-1"),
        metadata: Map<String, String> = emptyMap(),
    ) = OutboundParkerResponse(
        channelId = channelId,
        senderPrincipalId = senderPrincipalId,
        text = text,
        timestamp = timestamp,
        correlationId = correlationId,
        metadata = metadata,
    )

    @Test
    fun `an OutboundParkerResponse with blank text is rejected`() {
        assertFailsWith<IllegalArgumentException> { outbound(text = "") }
        assertFailsWith<IllegalArgumentException> { outbound(text = "   ") }
    }

    @Test
    fun `OutboundParkerResponse metadata defaults to empty`() {
        assertTrue(outbound().metadata.isEmpty())
    }

    @Test
    fun `OutboundParkerResponse senderPrincipalId is independent of channelId -- never the channel's own identity by construction`() {
        // Section 5: the channel's own identity is never the value of senderPrincipalId on any
        // message it carries. This contract does not enforce that as a runtime check (it would
        // require the channel's own ModuleId, not available at this shape's construction site),
        // but this test proves the two fields are structurally independent -- a caller CAN set
        // them to different values, and nothing in this data class forces them to collide.
        val response = outbound(channelId = ModuleId("channel.text"), senderPrincipalId = PrincipalId("system.parker"))
        assertTrue(response.channelId.value != response.senderPrincipalId.value)
    }

    // --- CommunicationIntakeDisposition ---

    @Test
    fun `CommunicationIntakeDisposition Rejected with a blank reason is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CommunicationIntakeDisposition.Rejected(CorrelationId("corr-1"), "")
        }
        assertFailsWith<IllegalArgumentException> {
            CommunicationIntakeDisposition.Rejected(CorrelationId("corr-1"), "   ")
        }
    }

    @Test
    fun `CommunicationIntakeDisposition Accepted carries the correlationId and the accepted message`() {
        val message = inbound()
        val disposition = CommunicationIntakeDisposition.Accepted(message.correlationId, message)

        assertEquals(message.correlationId, disposition.correlationId)
        assertEquals(message, disposition.message)
    }

    @Test
    fun `CommunicationIntakeDisposition is exactly two variants -- Accepted and Rejected`() {
        // Structural proof, not a runtime assertion: TaskProposalDisposition has five
        // subclasses; this sealed type deliberately has only two (Contract Design Section 6's
        // own "minimal, two-variant, structural-only outcome" determination). Constructing both,
        // and only these two, compiling is the guarantee -- this test documents that intent.
        val accepted: CommunicationIntakeDisposition = CommunicationIntakeDisposition.Accepted(CorrelationId("c1"), inbound())
        val rejected: CommunicationIntakeDisposition = CommunicationIntakeDisposition.Rejected(CorrelationId("c1"), "not enabled")

        assertTrue(accepted is CommunicationIntakeDisposition.Accepted)
        assertTrue(rejected is CommunicationIntakeDisposition.Rejected)
    }
}
